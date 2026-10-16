package `in`.soumyaranjansahoo.dockdrop.core.storage

import android.net.Uri
import `in`.soumyaranjansahoo.dockdrop.core.model.DockInfo
import kotlinx.coroutines.flow.Flow
import java.io.InputStream
import java.io.OutputStream

sealed class StorageError(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class PermissionLost(val uri: Uri, message: String) : StorageError(message)
    class VolumeGone(val volumeUuid: String, message: String) : StorageError(message)
    class NoSpace(val neededBytes: Long, val freeBytes: Long) :
        StorageError("SSD is full. Needs $neededBytes bytes, $freeBytes free.")
    class Io(message: String, cause: Throwable? = null) : StorageError(message, cause)
}

data class SourceFileInfo(
    val relativePath: String,
    val sizeBytes: Long,
    val lastModifiedMs: Long,
    val uri: Uri
)

data class VolumeInfo(
    val uuid: String,
    val userLabel: String,
    val totalBytes: Long,
    val freeBytes: Long,
    val filesystem: String,
    val isMounted: Boolean
) {
    fun toDockInfo() = DockInfo(
        volumeUuid = uuid,
        userLabel = userLabel,
        totalBytes = totalBytes,
        freeBytes = freeBytes,
        filesystem = filesystem,
        isMounted = isMounted
    )
}

interface StorageGateway {
    /**
     * Lists files recursively under a source folder tree URI using DocumentsContract batch cursor queries.
     * Must meet the Phase 0 A1 budget: ≤ 1,500 ms for 5,000 files (Phase 0 median: 285 ms).
     */
    fun listSourceFiles(treeUri: Uri): Flow<SourceFileInfo>

    /**
     * Recursively lists relative paths and sizes of existing files in the vault.
     */
    suspend fun listVaultFiles(vaultTreeUri: Uri, rootSubdir: String = ""): Map<String, Long>

    /**
     * Opens an input stream to read a source file.
     */
    suspend fun openSourceInputStream(fileUri: Uri): InputStream

    /**
     * Opens an input stream to re-read a destination file from the vault (used in ADR-002 verification).
     */
    suspend fun openDestinationInputStream(vaultTreeUri: Uri, relativePath: String): InputStream

    /**
     * Opens an output stream to write a temporary `.part` file at <relativePath>.part in the destination vault.
     * Automatically creates parent directories if needed.
     */
    suspend fun openDestinationPartOutputStream(vaultTreeUri: Uri, relativePath: String): OutputStream

    /**
     * Atomically renames <relativePath>.part to <relativePath>.
     * Phase 0 R2 discovery: ExternalStorageProvider silently auto-suffixes ('(1)') if the target exists!
     * Must delete any existing target file first, call renameDocument(), and verify COLUMN_DISPLAY_NAME.
     */
    suspend fun renameAtomic(vaultTreeUri: Uri, relativePath: String): Boolean

    /**
     * Moves a file natively using DocumentsContract.moveDocument (Phase 0 R3: 52 ms).
     * Used for moving deleted files to `_deleted/` per Rule 6.
     */
    suspend fun move(vaultTreeUri: Uri, srcRelativePath: String, dstRelativePath: String): Boolean

    /**
     * Deletes a file or directory at relative path.
     */
    suspend fun delete(vaultTreeUri: Uri, relativePath: String): Boolean

    /**
     * Checks if a document exists at relative path.
     */
    suspend fun exists(vaultTreeUri: Uri, relativePath: String): Boolean

    /**
     * Reads text content of a small file (e.g. HEAD, drive.json, manifest.sha256).
     */
    suspend fun readText(vaultTreeUri: Uri, relativePath: String): String?

    /**
     * Writes text content to a small file atomically (write to .part, delete existing, rename).
     */
    suspend fun writeTextAtomic(vaultTreeUri: Uri, relativePath: String, content: String): Boolean

    /**
     * Queries free bytes on the destination volume.
     */
    suspend fun freeBytes(vaultTreeUri: Uri): Long

    /**
     * Queries volume metadata (UUID, label, filesystem, mount state).
     */
    suspend fun volumeInfo(vaultTreeUri: Uri): VolumeInfo

    /**
     * Checks if persisted SAF permission is held for a tree URI.
     */
    fun hasPersistedPermission(treeUri: Uri): Boolean

    /**
     * Runs a quick write test (e.g. 16 MB or 4 MB temp file) and returns throughput in MB/s.
     */
    suspend fun runWriteSpeedTest(vaultTreeUri: Uri, testSizeBytes: Long = 8 * 1024 * 1024L): Double
}
