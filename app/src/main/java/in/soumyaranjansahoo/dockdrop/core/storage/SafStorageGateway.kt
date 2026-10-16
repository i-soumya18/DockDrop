package `in`.soumyaranjansahoo.dockdrop.core.storage

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.storage.StorageManager
import android.provider.DocumentsContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileNotFoundException
import java.io.InputStream
import java.io.OutputStream
import java.util.ArrayDeque
import java.util.concurrent.ConcurrentHashMap

class SafStorageGateway(
    private val context: Context
) : StorageGateway {

    private val contentResolver: ContentResolver get() = context.contentResolver
    private val storageManager: StorageManager get() = context.getSystemService(Context.STORAGE_SERVICE) as StorageManager

    // Cache of (parentDocId, dirName) -> dirDocId to avoid redundant queries during streaming writes
    private val dirCache = ConcurrentHashMap<String, String>()

    override fun listSourceFiles(treeUri: Uri): Flow<SourceFileInfo> = flow {
        if (!hasPersistedPermission(treeUri)) {
            throw StorageError.PermissionLost(treeUri, "Persisted permission lost for $treeUri")
        }

        val rootDocId = DocumentsContract.getTreeDocumentId(treeUri)
        val stack = ArrayDeque<Pair<String, String>>() // (docId, relativeDirPath)
        stack.push(Pair(rootDocId, ""))

        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_SIZE,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED
        )

        while (stack.isNotEmpty()) {
            val (currentDocId, currentRelPath) = stack.pop()
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, currentDocId)

            var cursor: Cursor? = null
            try {
                cursor = contentResolver.query(childrenUri, projection, null, null, null)
                if (cursor != null) {
                    val idCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                    val nameCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                    val mimeCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
                    val sizeCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_SIZE)
                    val modifiedCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_LAST_MODIFIED)

                    while (cursor.moveToNext()) {
                        val docId = cursor.getString(idCol)
                        val name = cursor.getString(nameCol) ?: continue
                        val mime = cursor.getString(mimeCol) ?: ""
                        val size = if (cursor.isNull(sizeCol)) 0L else cursor.getLong(sizeCol)
                        val modified = if (cursor.isNull(modifiedCol)) 0L else cursor.getLong(modifiedCol)

                        val relPath = if (currentRelPath.isEmpty()) name else "$currentRelPath/$name"

                        if (mime == DocumentsContract.Document.MIME_TYPE_DIR) {
                            stack.push(Pair(docId, relPath))
                        } else {
                            val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
                            emit(
                                SourceFileInfo(
                                    relativePath = relPath,
                                    sizeBytes = size,
                                    lastModifiedMs = modified,
                                    uri = docUri
                                )
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                if (e is SecurityException) {
                    throw StorageError.PermissionLost(treeUri, "Permission revoked while listing: ${e.message}")
                }
                throw StorageError.Io("Error listing source directory: ${e.message}", e)
            } finally {
                cursor?.close()
            }
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun listVaultFiles(vaultTreeUri: Uri, rootSubdir: String): Map<String, Long> = withContext(Dispatchers.IO) {
        val result = mutableMapOf<String, Long>()
        val rootDocId = DocumentsContract.getTreeDocumentId(vaultTreeUri)

        val startDocId = if (rootSubdir.isEmpty()) {
            rootDocId
        } else {
            findSubdirectory(vaultTreeUri, rootDocId, rootSubdir) ?: return@withContext emptyMap()
        }

        val stack = ArrayDeque<Pair<String, String>>()
        stack.push(Pair(startDocId, ""))

        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_SIZE
        )

        while (stack.isNotEmpty()) {
            val (currentDocId, currentRelPath) = stack.pop()
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(vaultTreeUri, currentDocId)

            var cursor: Cursor? = null
            try {
                cursor = contentResolver.query(childrenUri, projection, null, null, null)
                if (cursor != null) {
                    val idCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                    val nameCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                    val mimeCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
                    val sizeCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_SIZE)

                    while (cursor.moveToNext()) {
                        val docId = cursor.getString(idCol)
                        val name = cursor.getString(nameCol) ?: continue
                        val mime = cursor.getString(mimeCol) ?: ""
                        val size = if (cursor.isNull(sizeCol)) 0L else cursor.getLong(sizeCol)

                        val relPath = if (currentRelPath.isEmpty()) name else "$currentRelPath/$name"

                        if (mime == DocumentsContract.Document.MIME_TYPE_DIR) {
                            stack.push(Pair(docId, relPath))
                        } else {
                            val fullPath = if (rootSubdir.isEmpty()) relPath else "$rootSubdir/$relPath"
                            result[fullPath] = size
                        }
                    }
                }
            } catch (e: Exception) {
                // Ignore or return partial
            } finally {
                cursor?.close()
            }
        }

        result
    }

    override suspend fun openSourceInputStream(fileUri: Uri): InputStream = withContext(Dispatchers.IO) {
        try {
            contentResolver.openInputStream(fileUri)
                ?: throw StorageError.Io("Could not open input stream for $fileUri")
        } catch (e: SecurityException) {
            throw StorageError.PermissionLost(fileUri, "Permission lost reading $fileUri")
        } catch (e: Exception) {
            throw StorageError.Io("Error reading source file: ${e.message}", e)
        }
    }

    override suspend fun openDestinationInputStream(vaultTreeUri: Uri, relativePath: String): InputStream = withContext(Dispatchers.IO) {
        val docUri = findDocumentUri(vaultTreeUri, relativePath)
            ?: throw StorageError.Io("File not found on destination: $relativePath")
        try {
            contentResolver.openInputStream(docUri)
                ?: throw StorageError.Io("Could not open destination input stream for $relativePath")
        } catch (e: Exception) {
            throw StorageError.Io("Failed to read destination file for verification: ${e.message}", e)
        }
    }

    override suspend fun openDestinationPartOutputStream(vaultTreeUri: Uri, relativePath: String): OutputStream = withContext(Dispatchers.IO) {
        val partRelativePath = "$relativePath.part"
        val segments = partRelativePath.split("/")
        val parentPathSegments = segments.dropLast(1)
        val partFileName = segments.last()

        val rootDocId = DocumentsContract.getTreeDocumentId(vaultTreeUri)
        val parentDocId = ensureDirectoryHierarchy(vaultTreeUri, rootDocId, parentPathSegments)

        val parentUri = DocumentsContract.buildDocumentUriUsingTree(vaultTreeUri, parentDocId)

        // Delete any existing .part file if present (e.g. from a prior killed attempt)
        val existingPartDocId = findChildDocumentId(vaultTreeUri, parentDocId, partFileName)
        if (existingPartDocId != null) {
            try {
                val existingPartUri = DocumentsContract.buildDocumentUriUsingTree(vaultTreeUri, existingPartDocId)
                DocumentsContract.deleteDocument(contentResolver, existingPartUri)
            } catch (e: Exception) {
                // continue to create or overwrite
            }
        }

        val partDocUri = try {
            DocumentsContract.createDocument(contentResolver, parentUri, "application/octet-stream", partFileName)
                ?: throw StorageError.Io("createDocument returned null for $partRelativePath")
        } catch (e: Exception) {
            throw StorageError.Io("Failed to create part file $partRelativePath: ${e.message}", e)
        }

        try {
            contentResolver.openOutputStream(partDocUri, "wt")
                ?: throw StorageError.Io("openOutputStream returned null for $partDocUri")
        } catch (e: Exception) {
            throw StorageError.Io("Failed to open part output stream: ${e.message}", e)
        }
    }

    /**
     * Atomically renames <relativePath>.part to <relativePath>.
     * Applies Phase 0 R2 rule: deletes any existing target file first, calls renameDocument,
     * and asserts COLUMN_DISPLAY_NAME matches finalFileName.
     */
    override suspend fun renameAtomic(vaultTreeUri: Uri, relativePath: String): Boolean = withContext(Dispatchers.IO) {
        val partRelativePath = "$relativePath.part"
        val segments = relativePath.split("/")
        val parentPathSegments = segments.dropLast(1)
        val finalFileName = segments.last()
        val partFileName = "$finalFileName.part"

        val rootDocId = DocumentsContract.getTreeDocumentId(vaultTreeUri)
        val parentDocId = findDirectoryHierarchy(vaultTreeUri, rootDocId, parentPathSegments)
            ?: throw StorageError.Io("Parent directory not found for rename: $relativePath")

        val partDocId = findChildDocumentId(vaultTreeUri, parentDocId, partFileName)
            ?: throw StorageError.Io("Source .part file not found: $partRelativePath")

        val partDocUri = DocumentsContract.buildDocumentUriUsingTree(vaultTreeUri, partDocId)

        // LOAD-BEARING PHASE 0 R2 RULE:
        // ExternalStorageProvider auto-suffixes to 'name (1).bin' if target already exists.
        // We MUST delete any existing target file first.
        val existingTargetDocId = findChildDocumentId(vaultTreeUri, parentDocId, finalFileName)
        if (existingTargetDocId != null) {
            val existingTargetUri = DocumentsContract.buildDocumentUriUsingTree(vaultTreeUri, existingTargetDocId)
            val deleted = try {
                DocumentsContract.deleteDocument(contentResolver, existingTargetUri)
            } catch (e: Exception) {
                false
            }
            if (!deleted) {
                throw StorageError.Io("Could not delete existing target file before rename: $relativePath")
            }
        }

        val renamedUri = try {
            DocumentsContract.renameDocument(contentResolver, partDocUri, finalFileName)
                ?: throw StorageError.Io("renameDocument returned null for $finalFileName")
        } catch (e: Exception) {
            throw StorageError.Io("renameDocument failed: ${e.message}", e)
        }

        // Verify that the resulting file name has exact expected name without (1) suffix
        var verifiedName: String? = null
        contentResolver.query(
            renamedUri,
            arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME),
            null,
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                verifiedName = cursor.getString(0)
            }
        }

        if (verifiedName != finalFileName) {
            throw StorageError.Io("Atomic rename verification failed: expected '$finalFileName', got '$verifiedName'")
        }

        true
    }

    override suspend fun move(vaultTreeUri: Uri, srcRelativePath: String, dstRelativePath: String): Boolean = withContext(Dispatchers.IO) {
        val rootDocId = DocumentsContract.getTreeDocumentId(vaultTreeUri)

        val srcSegments = srcRelativePath.split("/")
        val srcParentSegments = srcSegments.dropLast(1)
        val srcFileName = srcSegments.last()

        val dstSegments = dstRelativePath.split("/")
        val dstParentSegments = dstSegments.dropLast(1)
        val dstFileName = dstSegments.last()

        val srcParentDocId = findDirectoryHierarchy(vaultTreeUri, rootDocId, srcParentSegments)
            ?: throw StorageError.Io("Source parent directory not found: $srcRelativePath")
        val srcDocId = findChildDocumentId(vaultTreeUri, srcParentDocId, srcFileName)
            ?: throw StorageError.Io("Source file not found to move: $srcRelativePath")

        val dstParentDocId = ensureDirectoryHierarchy(vaultTreeUri, rootDocId, dstParentSegments)

        val srcDocUri = DocumentsContract.buildDocumentUriUsingTree(vaultTreeUri, srcDocId)
        val srcParentUri = DocumentsContract.buildDocumentUriUsingTree(vaultTreeUri, srcParentDocId)
        val dstParentUri = DocumentsContract.buildDocumentUriUsingTree(vaultTreeUri, dstParentDocId)

        val movedUri = try {
            DocumentsContract.moveDocument(contentResolver, srcDocUri, srcParentUri, dstParentUri)
                ?: throw StorageError.Io("moveDocument returned null for $srcRelativePath -> $dstRelativePath")
        } catch (e: Exception) {
            throw StorageError.Io("moveDocument failed: ${e.message}", e)
        }

        if (srcFileName != dstFileName) {
            DocumentsContract.renameDocument(contentResolver, movedUri, dstFileName)
        }

        true
    }

    override suspend fun delete(vaultTreeUri: Uri, relativePath: String): Boolean = withContext(Dispatchers.IO) {
        val docUri = findDocumentUri(vaultTreeUri, relativePath) ?: return@withContext true
        try {
            DocumentsContract.deleteDocument(contentResolver, docUri)
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun exists(vaultTreeUri: Uri, relativePath: String): Boolean = withContext(Dispatchers.IO) {
        findDocumentUri(vaultTreeUri, relativePath) != null
    }

    override suspend fun readText(vaultTreeUri: Uri, relativePath: String): String? = withContext(Dispatchers.IO) {
        val docUri = findDocumentUri(vaultTreeUri, relativePath) ?: return@withContext null
        try {
            contentResolver.openInputStream(docUri)?.bufferedReader()?.use { it.readText() }
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun writeTextAtomic(vaultTreeUri: Uri, relativePath: String, content: String): Boolean = withContext(Dispatchers.IO) {
        openDestinationPartOutputStream(vaultTreeUri, relativePath).use { out ->
            out.write(content.toByteArray(Charsets.UTF_8))
            out.flush()
        }
        renameAtomic(vaultTreeUri, relativePath)
    }

    override suspend fun freeBytes(vaultTreeUri: Uri): Long = withContext(Dispatchers.IO) {
        try {
            val vInfo = volumeInfo(vaultTreeUri)
            vInfo.freeBytes
        } catch (e: Exception) {
            0L
        }
    }

    override suspend fun volumeInfo(vaultTreeUri: Uri): VolumeInfo = withContext(Dispatchers.IO) {
        val docId = DocumentsContract.getTreeDocumentId(vaultTreeUri)
        val volumeUuid = docId.substringBefore(":", missingDelimiterValue = "")

        val volumes = storageManager.storageVolumes
        val matchingVol = volumes.firstOrNull { it.uuid.equals(volumeUuid, ignoreCase = true) }

        val label = matchingVol?.getDescription(context) ?: "External SSD"
        val isMounted = matchingVol?.state == Environment.MEDIA_MOUNTED

        var totalBytes = 0L
        var freeBytes = 0L

        if (matchingVol != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val dir = matchingVol.directory
                if (dir != null && dir.exists()) {
                    totalBytes = dir.totalSpace
                    freeBytes = dir.freeSpace
                }
            }
        }

        // Fallback if directory reflection/access was blocked
        if (totalBytes <= 0L) {
            totalBytes = 128L * 1024 * 1024 * 1024 // reasonable estimate if inaccessible
            freeBytes = 100L * 1024 * 1024 * 1024
        }

        VolumeInfo(
            uuid = volumeUuid,
            userLabel = label,
            totalBytes = totalBytes,
            freeBytes = freeBytes,
            filesystem = "exFAT",
            isMounted = isMounted
        )
    }

    override fun hasPersistedPermission(treeUri: Uri): Boolean {
        return context.contentResolver.persistedUriPermissions.any {
            it.uri == treeUri && it.isWritePermission
        }
    }

    override suspend fun runWriteSpeedTest(vaultTreeUri: Uri, testSizeBytes: Long): Double = withContext(Dispatchers.IO) {
        val testFileName = ".dockdrop_speed_test.bin"
        val buffer = ByteArray(1024 * 1024) { 0x5A } // 1 MB buffer
        var bytesWritten = 0L
        val startTime = System.nanoTime()

        try {
            openDestinationPartOutputStream(vaultTreeUri, testFileName).use { out ->
                while (bytesWritten < testSizeBytes) {
                    val toWrite = minOf(buffer.size.toLong(), testSizeBytes - bytesWritten).toInt()
                    out.write(buffer, 0, toWrite)
                    bytesWritten += toWrite
                }
                out.flush()
            }
            val elapsedSec = (System.nanoTime() - startTime) / 1_000_000_000.0
            delete(vaultTreeUri, "$testFileName.part")
            if (elapsedSec > 0) (bytesWritten / (1024.0 * 1024.0)) / elapsedSec else 30.0
        } catch (e: Exception) {
            delete(vaultTreeUri, "$testFileName.part")
            30.0 // reasonable default
        }
    }

    // --- Helper directory navigation and caching methods ---

    private fun findDocumentUri(treeUri: Uri, relativePath: String): Uri? {
        val segments = relativePath.split("/")
        val parentSegments = segments.dropLast(1)
        val fileName = segments.last()

        val rootDocId = DocumentsContract.getTreeDocumentId(treeUri)
        val parentDocId = findDirectoryHierarchy(treeUri, rootDocId, parentSegments) ?: return null
        val docId = findChildDocumentId(treeUri, parentDocId, fileName) ?: return null
        return DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
    }

    private fun findDirectoryHierarchy(treeUri: Uri, rootDocId: String, segments: List<String>): String? {
        var currentDocId = rootDocId
        for (segment in segments) {
            if (segment.isEmpty()) continue
            val cacheKey = "$currentDocId/$segment"
            val cached = dirCache[cacheKey]
            if (cached != null) {
                currentDocId = cached
            } else {
                val childId = findChildDocumentId(treeUri, currentDocId, segment) ?: return null
                dirCache[cacheKey] = childId
                currentDocId = childId
            }
        }
        return currentDocId
    }

    private fun ensureDirectoryHierarchy(treeUri: Uri, rootDocId: String, segments: List<String>): String {
        var currentDocId = rootDocId
        for (segment in segments) {
            if (segment.isEmpty()) continue
            val cacheKey = "$currentDocId/$segment"
            val cached = dirCache[cacheKey]
            if (cached != null) {
                currentDocId = cached
            } else {
                var childId = findChildDocumentId(treeUri, currentDocId, segment)
                if (childId == null) {
                    val parentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, currentDocId)
                    val newDirUri = DocumentsContract.createDocument(
                        contentResolver,
                        parentUri,
                        DocumentsContract.Document.MIME_TYPE_DIR,
                        segment
                    ) ?: throw StorageError.Io("Failed to create directory segment '$segment'")
                    childId = DocumentsContract.getDocumentId(newDirUri)
                }
                dirCache[cacheKey] = childId
                currentDocId = childId
            }
        }
        return currentDocId
    }

    private fun findChildDocumentId(treeUri: Uri, parentDocId: String, nameToFind: String): String? {
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentDocId)
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME
        )
        contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val nameCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            while (cursor.moveToNext()) {
                val name = cursor.getString(nameCol)
                if (name == nameToFind) {
                    return cursor.getString(idCol)
                }
            }
        }
        return null
    }

    private fun findSubdirectory(treeUri: Uri, rootDocId: String, path: String): String? {
        val segments = path.split("/").filter { it.isNotEmpty() }
        return findDirectoryHierarchy(treeUri, rootDocId, segments)
    }
}
