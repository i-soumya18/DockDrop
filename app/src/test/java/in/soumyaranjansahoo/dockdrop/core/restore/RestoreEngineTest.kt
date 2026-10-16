package `in`.soumyaranjansahoo.dockdrop.core.restore

import android.content.Context
import android.net.TestUri
import android.net.Uri
import `in`.soumyaranjansahoo.dockdrop.core.manifest.ManifestEntry
import `in`.soumyaranjansahoo.dockdrop.core.manifest.ManifestRepository
import `in`.soumyaranjansahoo.dockdrop.core.storage.SourceFileInfo
import `in`.soumyaranjansahoo.dockdrop.core.storage.StorageGateway
import `in`.soumyaranjansahoo.dockdrop.core.storage.VolumeInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Completion bar tests for AGENTS.md A5:
 * - Wipe a test folder, restore it, compare hashes: 100% match.
 * - Corrupted backup on SSD rejected before rename; .part deleted; destination untouched.
 * - Conflict resolution: Keep both preserves original destination file.
 * - Restore drill: 10 files verified into cache sandbox and cleaned up.
 * - Discovery on drive: ManifestRepository discovers backups on SSD.
 */
class RestoreEngineTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private class MemoryStorageGateway : StorageGateway {
        val vaultFiles = ConcurrentHashMap<String, ByteArray>()
        val destFiles = ConcurrentHashMap<String, ByteArray>()
        val scannedPaths = mutableListOf<String>()
        val modifiedTimestamps = ConcurrentHashMap<String, Long>()

        override fun listSourceFiles(treeUri: Uri): Flow<SourceFileInfo> = emptyFlow()
        override suspend fun listVaultFiles(vaultTreeUri: Uri, rootSubdir: String): Map<String, Long> =
            vaultFiles.mapValues { it.value.size.toLong() }

        override suspend fun openSourceInputStream(fileUri: Uri): InputStream =
            ByteArrayInputStream(ByteArray(0))

        override suspend fun openDestinationInputStream(vaultTreeUri: Uri, relativePath: String): InputStream {
            val bytes = vaultFiles[relativePath] ?: destFiles[relativePath]
                ?: throw IllegalArgumentException("Vault file not found: $relativePath")
            return ByteArrayInputStream(bytes)
        }

        override suspend fun openDestinationPartOutputStream(vaultTreeUri: Uri, relativePath: String): OutputStream {
            return object : ByteArrayOutputStream() {
                override fun close() {
                    super.close()
                    destFiles["$relativePath.part"] = toByteArray()
                }
            }
        }

        override suspend fun renameAtomic(vaultTreeUri: Uri, relativePath: String): Boolean {
            val part = destFiles.remove("$relativePath.part")
                ?: throw IllegalStateException("Part file not found: $relativePath.part")
            destFiles[relativePath] = part
            return true
        }

        override suspend fun move(vaultTreeUri: Uri, srcRelativePath: String, dstRelativePath: String): Boolean {
            val bytes = destFiles.remove(srcRelativePath) ?: return false
            destFiles[dstRelativePath] = bytes
            return true
        }

        override suspend fun delete(vaultTreeUri: Uri, relativePath: String): Boolean {
            destFiles.remove(relativePath)
            vaultFiles.remove(relativePath)
            return true
        }

        override suspend fun exists(vaultTreeUri: Uri, relativePath: String): Boolean =
            destFiles.containsKey(relativePath) || vaultFiles.containsKey(relativePath)

        override suspend fun readText(vaultTreeUri: Uri, relativePath: String): String? {
            val bytes = vaultFiles[relativePath] ?: destFiles[relativePath] ?: return null
            return String(bytes, Charsets.UTF_8)
        }

        override suspend fun writeTextAtomic(vaultTreeUri: Uri, relativePath: String, content: String): Boolean {
            vaultFiles[relativePath] = content.toByteArray(Charsets.UTF_8)
            return true
        }

        override suspend fun freeBytes(vaultTreeUri: Uri): Long = 100L * 1024 * 1024 * 1024
        override suspend fun volumeInfo(vaultTreeUri: Uri): VolumeInfo =
            VolumeInfo("4E21-0000", "SSD", 128L * 1024 * 1024 * 1024, 100L * 1024 * 1024 * 1024, "exFAT", true)

        override fun hasPersistedPermission(treeUri: Uri): Boolean = true
        override suspend fun runWriteSpeedTest(vaultTreeUri: Uri, testSizeBytes: Long): Double = 40.0

        override suspend fun listChildDirectories(treeUri: Uri, relativePath: String): List<String> {
            val prefix = if (relativePath.isEmpty()) "" else "$relativePath/"
            return vaultFiles.keys
                .filter { it.startsWith(prefix) }
                .map { it.substring(prefix.length) }
                .map { it.substringBefore('/') }
                .distinct()
        }

        override suspend fun phoneFreeBytes(): Long = 50L * 1024 * 1024 * 1024

        override suspend fun setLastModified(treeUri: Uri, relativePath: String, lastModifiedMs: Long): Boolean {
            modifiedTimestamps[relativePath] = lastModifiedMs
            return true
        }

        override suspend fun triggerMediaScan(treeUri: Uri, relativePaths: List<String>) {
            scannedPaths.addAll(relativePaths)
        }
    }

    private lateinit var storage: MemoryStorageGateway
    private lateinit var manifestRepo: ManifestRepository
    private lateinit var restoreEngine: RestoreEngine
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)

    private val fakeVaultUri = TestUri("content://fake/vault")
    private val fakeCameraUri = TestUri("content://fake/camera")

    private fun sha256(bytes: ByteArray): String {
        return MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
    }

    @Before
    fun setup() {
        storage = MemoryStorageGateway()
        manifestRepo = ManifestRepository(storage)

        restoreEngine = RestoreEngine(
            storageGateway = storage,
            scope = scope,
            cacheDir = tempFolder.root
        )
    }

    @Test
    fun testWipeAndRestore_hashesMatch100Percent() = runBlocking {
        // Setup 2 backup files in SSD vault
        val photo1 = "Photo 1 binary content".toByteArray()
        val photo2 = "Photo 2 binary content".toByteArray()
        val hash1 = sha256(photo1)
        val hash2 = sha256(photo2)

        storage.vaultFiles["MySet/Camera/IMG_001.jpg"] = photo1
        storage.vaultFiles["MySet/Camera/IMG_002.jpg"] = photo2

        val entry1 = ManifestEntry("Camera/IMG_001.jpg", photo1.size.toLong(), 1700000000000L, hash1, "present", 1L)
        val entry2 = ManifestEntry("Camera/IMG_002.jpg", photo2.size.toLong(), 1700000001000L, hash2, "present", 1L)

        val items = listOf(
            RestoreItem(
                manifestEntry = entry1,
                sourceFolderLabel = "Camera",
                relativePathInFolder = "IMG_001.jpg",
                vaultRelativePath = "MySet/Camera/IMG_001.jpg",
                hasConflict = false,
                resolvedTargetRelPath = "IMG_001.jpg"
            ),
            RestoreItem(
                manifestEntry = entry2,
                sourceFolderLabel = "Camera",
                relativePathInFolder = "IMG_002.jpg",
                vaultRelativePath = "MySet/Camera/IMG_002.jpg",
                hasConflict = false,
                resolvedTargetRelPath = "IMG_002.jpg"
            )
        )

        val plan = RestorePlan(
            id = UUID.randomUUID().toString(),
            setId = "set_1",
            setName = "MySet",
            deviceLabel = "Pixel 8",
            vaultTreeUriString = fakeVaultUri.toString(),
            destination = RestoreDestination.OriginalFolders(mapOf("Camera" to fakeCameraUri.toString())),
            conflictPolicy = ConflictPolicy.KEEP_BOTH,
            allCandidateItems = items,
            itemsToTransfer = items,
            totalFilesSelected = 2,
            totalFilesToCopy = 2,
            totalBytesToCopy = (photo1.size + photo2.size).toLong(),
            conflictCount = 0,
            permissionGaps = emptyList(),
            phoneFreeBytes = 50L * 1024 * 1024 * 1024,
            hasEnoughSpace = true,
            isDrill = false
        )

        // Ensure destination folder is wiped (empty)
        assertTrue(storage.destFiles.isEmpty())

        // Execute restore
        restoreEngine.executeRestore(plan)

        // Wait for state to finish
        val finalState = restoreEngine.state.first { it is RestoreState.Completed || it is RestoreState.Partial }
        assertTrue(finalState is RestoreState.Completed)

        val receipt = (finalState as RestoreState.Completed).receipt
        assertEquals(2, receipt.restoredCount)
        assertEquals(0, receipt.failedCount)
        assertEquals("SHA-256 against backup", receipt.verificationSummary)

        // Verify files in destination: 100% hash match!
        assertNotNull(storage.destFiles["IMG_001.jpg"])
        assertNotNull(storage.destFiles["IMG_002.jpg"])
        assertEquals(hash1, sha256(storage.destFiles["IMG_001.jpg"]!!))
        assertEquals(hash2, sha256(storage.destFiles["IMG_002.jpg"]!!))

        // Verify timestamps preserved
        assertEquals(1700000000000L, storage.modifiedTimestamps["IMG_001.jpg"])
        assertEquals(1700000001000L, storage.modifiedTimestamps["IMG_002.jpg"])

        // Verify media rescan was triggered
        assertTrue(storage.scannedPaths.contains("IMG_001.jpg"))
        assertTrue(storage.scannedPaths.contains("IMG_002.jpg"))
    }

    @Test
    fun testCorruptedBackupOnSSD_rejectedBeforeRename() = runBlocking {
        // Vault file on SSD is corrupted (bytes do not match expected manifest SHA-256)
        val originalGoodBytes = "Good original photo content".toByteArray()
        val expectedSha = sha256(originalGoodBytes)

        val corruptedBytesOnSsd = "Corrupted bit-flip bytes".toByteArray()
        storage.vaultFiles["MySet/Camera/corrupt.jpg"] = corruptedBytesOnSsd

        val entry = ManifestEntry("Camera/corrupt.jpg", corruptedBytesOnSsd.size.toLong(), 1000L, expectedSha, "present", 1L)

        val item = RestoreItem(
            manifestEntry = entry,
            sourceFolderLabel = "Camera",
            relativePathInFolder = "corrupt.jpg",
            vaultRelativePath = "MySet/Camera/corrupt.jpg",
            hasConflict = false,
            resolvedTargetRelPath = "corrupt.jpg"
        )

        val plan = RestorePlan(
            id = UUID.randomUUID().toString(),
            setId = "set_1",
            setName = "MySet",
            deviceLabel = "Pixel 8",
            vaultTreeUriString = fakeVaultUri.toString(),
            destination = RestoreDestination.OriginalFolders(mapOf("Camera" to fakeCameraUri.toString())),
            conflictPolicy = ConflictPolicy.KEEP_BOTH,
            allCandidateItems = listOf(item),
            itemsToTransfer = listOf(item),
            totalFilesSelected = 1,
            totalFilesToCopy = 1,
            totalBytesToCopy = corruptedBytesOnSsd.size.toLong(),
            conflictCount = 0,
            permissionGaps = emptyList(),
            phoneFreeBytes = 50L * 1024 * 1024 * 1024,
            hasEnoughSpace = true,
            isDrill = false
        )

        restoreEngine.executeRestore(plan)

        val finalState = restoreEngine.state.first { it is RestoreState.Completed || it is RestoreState.Partial }
        assertTrue(finalState is RestoreState.Partial)

        val receipt = (finalState as RestoreState.Partial).receipt
        assertEquals(0, receipt.restoredCount)
        assertEquals(1, receipt.failedCount)

        // Critical safety verification: corrupt file was NEVER renamed into place!
        assertFalse(storage.destFiles.containsKey("corrupt.jpg"))
        // And the .part file was deleted
        assertFalse(storage.destFiles.containsKey("corrupt.jpg.part"))
    }

    @Test
    fun testConflictResolution_keepBothPreservesExistingFile() = runBlocking {
        // Pre-existing file on phone
        val phoneExistingBytes = "Old local phone file".toByteArray()
        storage.destFiles["photo.jpg"] = phoneExistingBytes

        // Restored file from SSD
        val backupBytes = "Fresh restored backup file".toByteArray()
        val backupSha = sha256(backupBytes)
        storage.vaultFiles["MySet/Camera/photo.jpg"] = backupBytes

        val entry = ManifestEntry("Camera/photo.jpg", backupBytes.size.toLong(), 1000L, backupSha, "present", 1L)

        val item = RestoreItem(
            manifestEntry = entry,
            sourceFolderLabel = "Camera",
            relativePathInFolder = "photo.jpg",
            vaultRelativePath = "MySet/Camera/photo.jpg",
            hasConflict = true,
            resolvedTargetRelPath = "photo (1).jpg" // Keep both suffix!
        )

        val plan = RestorePlan(
            id = UUID.randomUUID().toString(),
            setId = "set_1",
            setName = "MySet",
            deviceLabel = "Pixel 8",
            vaultTreeUriString = fakeVaultUri.toString(),
            destination = RestoreDestination.OriginalFolders(mapOf("Camera" to fakeCameraUri.toString())),
            conflictPolicy = ConflictPolicy.KEEP_BOTH,
            allCandidateItems = listOf(item),
            itemsToTransfer = listOf(item),
            totalFilesSelected = 1,
            totalFilesToCopy = 1,
            totalBytesToCopy = backupBytes.size.toLong(),
            conflictCount = 1,
            permissionGaps = emptyList(),
            phoneFreeBytes = 50L * 1024 * 1024 * 1024,
            hasEnoughSpace = true,
            isDrill = false
        )

        restoreEngine.executeRestore(plan)

        val finalState = restoreEngine.state.first { it is RestoreState.Completed || it is RestoreState.Partial }
        assertTrue(finalState is RestoreState.Completed)

        // Existing local phone file is completely preserved!
        assertEquals("Old local phone file", String(storage.destFiles["photo.jpg"]!!))
        // Restored file is placed under photo (1).jpg
        assertEquals("Fresh restored backup file", String(storage.destFiles["photo (1).jpg"]!!))
    }

    @Test
    fun testRestoreDrill_verifies10FilesIntoCacheSandbox() = runBlocking {
        val items = mutableListOf<RestoreItem>()
        var totalBytes = 0L

        for (i in 1..10) {
            val content = "Drill file content $i".toByteArray()
            val hash = sha256(content)
            storage.vaultFiles["MySet/Camera/drill_$i.jpg"] = content
            totalBytes += content.size

            val entry = ManifestEntry("Camera/drill_$i.jpg", content.size.toLong(), 1000L, hash, "present", 1L)
            items.add(
                RestoreItem(
                    manifestEntry = entry,
                    sourceFolderLabel = "Camera",
                    relativePathInFolder = "drill_$i.jpg",
                    vaultRelativePath = "MySet/Camera/drill_$i.jpg",
                    hasConflict = false,
                    resolvedTargetRelPath = "drill_$i.jpg"
                )
            )
        }

        val plan = RestorePlan(
            id = UUID.randomUUID().toString(),
            setId = "set_1",
            setName = "MySet",
            deviceLabel = "Pixel 8",
            vaultTreeUriString = fakeVaultUri.toString(),
            destination = RestoreDestination.DrillSandbox,
            conflictPolicy = ConflictPolicy.KEEP_BOTH,
            allCandidateItems = items,
            itemsToTransfer = items,
            totalFilesSelected = 10,
            totalFilesToCopy = 10,
            totalBytesToCopy = totalBytes,
            conflictCount = 0,
            permissionGaps = emptyList(),
            phoneFreeBytes = 50L * 1024 * 1024 * 1024,
            hasEnoughSpace = true,
            isDrill = true
        )

        restoreEngine.executeRestore(plan)

        val finalState = restoreEngine.state.first { it is RestoreState.Completed || it is RestoreState.Partial }
        assertTrue(finalState is RestoreState.Completed)

        val receipt = (finalState as RestoreState.Completed).receipt
        assertEquals(10, receipt.restoredCount)
        assertEquals(0, receipt.failedCount)
        assertTrue(receipt.isDrill)
        assertEquals("SHA-256 against backup", receipt.verificationSummary)

        // Sandbox check: drill folder exists and temporary files were cleaned up
        val drillDir = File(tempFolder.root, "restore_drill")
        val remainingPartFiles = drillDir.listFiles { _, name -> name.endsWith(".part") }
        assertTrue(remainingPartFiles == null || remainingPartFiles.isEmpty())
    }

    @Test
    fun testManifestDiscoverBackupsOnDrive() = runBlocking {
        // 1. Ensure drive.json exists
        manifestRepo.ensureDriveJson(fakeVaultUri, "drive_123", "Galaxy S24")

        // 2. Commit a generation for set_abc
        val entry1 = ManifestEntry("Camera/IMG_1.jpg", 5000L, 1000L, "h1", "present", 1L)
        val entry2 = ManifestEntry("Documents/file.pdf", 7000L, 1001L, "h2", "present", 1L)
        manifestRepo.commitGeneration(fakeVaultUri, "set_abc", listOf(entry1, entry2))

        // 3. Write set metadata
        manifestRepo.writeSetMetadata(
            fakeVaultUri,
            `in`.soumyaranjansahoo.dockdrop.core.manifest.SetMetadata(
                setId = "set_abc",
                setName = "Photos and Docs",
                sourceLabels = listOf("Camera", "Documents")
            )
        )

        val discovered = manifestRepo.discoverBackupsOnDrive(fakeVaultUri)
        assertEquals(1, discovered.size)

        val backup = discovered.first()
        assertEquals("set_abc", backup.setId)
        assertEquals("Photos and Docs", backup.setName)
        assertEquals("Galaxy S24", backup.deviceLabel)
        assertEquals(2, backup.generation.entries.size)
        assertEquals(12000L, backup.generation.entries.values.sumOf { it.sizeBytes })
        assertEquals(listOf("Camera", "Documents"), backup.sourceLabels)
    }
}
