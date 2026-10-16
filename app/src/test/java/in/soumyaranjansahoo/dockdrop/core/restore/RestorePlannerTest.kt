package `in`.soumyaranjansahoo.dockdrop.core.restore

import android.net.TestUri
import android.net.Uri
import `in`.soumyaranjansahoo.dockdrop.core.manifest.ManifestEntry
import `in`.soumyaranjansahoo.dockdrop.core.manifest.ManifestRepository
import `in`.soumyaranjansahoo.dockdrop.core.storage.SourceFileInfo
import `in`.soumyaranjansahoo.dockdrop.core.storage.StorageGateway
import `in`.soumyaranjansahoo.dockdrop.core.storage.VolumeInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.ConcurrentHashMap

class RestorePlannerTest {

    private class TestStorageGateway : StorageGateway {
        val vaultFiles = ConcurrentHashMap<String, Long>()
        val phoneFiles = ConcurrentHashMap<String, Long>()
        var phoneFreeSpace: Long = 100L * 1024 * 1024 * 1024
        var permittedUris = mutableSetOf<String>()

        override fun listSourceFiles(treeUri: Uri): Flow<SourceFileInfo> = kotlinx.coroutines.flow.flow {
            for ((path, size) in phoneFiles) {
                emit(SourceFileInfo(path, size, 1000L, treeUri))
            }
        }
        override suspend fun listVaultFiles(vaultTreeUri: Uri, rootSubdir: String): Map<String, Long> = vaultFiles

        override suspend fun openSourceInputStream(fileUri: Uri): InputStream =
            ByteArrayInputStream(ByteArray(0))

        override suspend fun openDestinationInputStream(vaultTreeUri: Uri, relativePath: String): InputStream =
            ByteArrayInputStream(ByteArray(0))

        override suspend fun openDestinationPartOutputStream(vaultTreeUri: Uri, relativePath: String): OutputStream =
            throw UnsupportedOperationException()

        override suspend fun renameAtomic(vaultTreeUri: Uri, relativePath: String): Boolean = true
        override suspend fun move(vaultTreeUri: Uri, srcRelativePath: String, dstRelativePath: String): Boolean = true
        override suspend fun delete(vaultTreeUri: Uri, relativePath: String): Boolean = true

        override suspend fun exists(vaultTreeUri: Uri, relativePath: String): Boolean {
            return phoneFiles.containsKey(relativePath) || vaultFiles.containsKey(relativePath)
        }

        override suspend fun readText(vaultTreeUri: Uri, relativePath: String): String? = null
        override suspend fun writeTextAtomic(vaultTreeUri: Uri, relativePath: String, content: String): Boolean = true
        override suspend fun freeBytes(vaultTreeUri: Uri): Long = 100L * 1024 * 1024 * 1024
        override suspend fun volumeInfo(vaultTreeUri: Uri): VolumeInfo =
            VolumeInfo("4E21-0000", "SSD", 128L * 1024 * 1024 * 1024, 100L * 1024 * 1024 * 1024, "exFAT", true)

        override fun hasPersistedPermission(treeUri: Uri): Boolean =
            permittedUris.contains(treeUri.toString())

        override suspend fun runWriteSpeedTest(vaultTreeUri: Uri, testSizeBytes: Long): Double = 35.0
        override suspend fun listChildDirectories(treeUri: Uri, relativePath: String): List<String> = emptyList()
        override suspend fun phoneFreeBytes(): Long = phoneFreeSpace
        override suspend fun setLastModified(treeUri: Uri, relativePath: String, lastModifiedMs: Long): Boolean = true
        override suspend fun triggerMediaScan(treeUri: Uri, relativePaths: List<String>) {}
    }

    private lateinit var storage: TestStorageGateway
    private lateinit var manifestRepo: ManifestRepository
    private lateinit var planner: RestorePlanner
    private val fakeVaultUri = TestUri("content://fake/vault")
    private val cameraUri = TestUri("content://fake/camera")
    private val docsUri = TestUri("content://fake/docs")

    @Before
    fun setup() {
        storage = TestStorageGateway()
        storage.permittedUris.add(cameraUri.toString())
        storage.permittedUris.add(docsUri.toString())
        manifestRepo = ManifestRepository(storage)
        planner = RestorePlanner(storage, manifestRepo)
    }

    @Test
    fun testPlanCountAndSizeEquality() = runBlocking {
        // AGENTS.md A5: "The preview count/size equals the actual result in tests."
        val entries = listOf(
            ManifestEntry("Camera/IMG_001.jpg", 4000L, 1000L, "hash1", "present", 1L),
            ManifestEntry("Camera/IMG_002.jpg", 6000L, 1001L, "hash2", "present", 1L),
            ManifestEntry("Documents/memo.pdf", 10000L, 1002L, "hash3", "present", 1L)
        )

        val folderMappings = mapOf(
            "Camera" to cameraUri.toString(),
            "Documents" to docsUri.toString()
        )

        val plan = planner.createPlan(
            vaultTreeUri = fakeVaultUri,
            setId = "set_1",
            setName = "Pixel Backup",
            deviceLabel = "Pixel 8",
            selectedEntries = entries,
            destination = RestoreDestination.OriginalFolders(folderMappings),
            conflictPolicy = ConflictPolicy.KEEP_BOTH
        )

        assertEquals(3, plan.totalFilesToCopy)
        assertEquals(20000L, plan.totalBytesToCopy)
        assertEquals(3, plan.itemsToTransfer.size)
        assertEquals(20000L, plan.itemsToTransfer.sumOf { it.manifestEntry.sizeBytes })
        assertTrue(plan.hasEnoughSpace)
        assertTrue(plan.permissionGaps.isEmpty())
    }

    @Test
    fun testConflictPolicyKeepBoth_appendsUniqueNumericSuffix() = runBlocking {
        // Destination already has IMG_001.jpg
        storage.phoneFiles["IMG_001.jpg"] = 4000L

        val entries = listOf(
            ManifestEntry("Camera/IMG_001.jpg", 4000L, 1000L, "hash1", "present", 1L)
        )

        val folderMappings = mapOf("Camera" to cameraUri.toString())

        val plan = planner.createPlan(
            vaultTreeUri = fakeVaultUri,
            setId = "set_1",
            setName = "Pixel Backup",
            deviceLabel = "Pixel 8",
            selectedEntries = entries,
            destination = RestoreDestination.OriginalFolders(folderMappings),
            conflictPolicy = ConflictPolicy.KEEP_BOTH
        )

        assertEquals(1, plan.conflictCount)
        val item = plan.itemsToTransfer.first()
        assertTrue(item.hasConflict)
        assertEquals("IMG_001 (1).jpg", item.resolvedTargetRelPath)
    }

    @Test
    fun testConflictPolicySkip_doesNotOverwrite() = runBlocking {
        storage.phoneFiles["IMG_001.jpg"] = 4000L

        val entries = listOf(
            ManifestEntry("Camera/IMG_001.jpg", 4000L, 1000L, "hash1", "present", 1L)
        )

        val folderMappings = mapOf("Camera" to cameraUri.toString())

        val plan = planner.createPlan(
            vaultTreeUri = fakeVaultUri,
            setId = "set_1",
            setName = "Pixel Backup",
            deviceLabel = "Pixel 8",
            selectedEntries = entries,
            destination = RestoreDestination.OriginalFolders(folderMappings),
            conflictPolicy = ConflictPolicy.SKIP
        )

        assertEquals(1, plan.conflictCount)
        assertEquals(0, plan.itemsToTransfer.size)
        val item = plan.allCandidateItems.first()
        assertTrue(item.hasConflict)
    }

    @Test
    fun testConflictPolicyOverwrite_marksWillOverwrite() = runBlocking {
        storage.phoneFiles["IMG_001.jpg"] = 4000L

        val entries = listOf(
            ManifestEntry("Camera/IMG_001.jpg", 4000L, 1000L, "hash1", "present", 1L)
        )

        val folderMappings = mapOf("Camera" to cameraUri.toString())

        val plan = planner.createPlan(
            vaultTreeUri = fakeVaultUri,
            setId = "set_1",
            setName = "Pixel Backup",
            deviceLabel = "Pixel 8",
            selectedEntries = entries,
            destination = RestoreDestination.OriginalFolders(folderMappings),
            conflictPolicy = ConflictPolicy.OVERWRITE
        )

        assertEquals(1, plan.conflictCount)
        assertEquals(1, plan.itemsToTransfer.size)
        val item = plan.itemsToTransfer.first()
        assertTrue(item.hasConflict)
        assertEquals("IMG_001.jpg", item.resolvedTargetRelPath)
    }

    @Test
    fun testPermissionGapDetection() = runBlocking {
        // Remove permission for Documents folder
        storage.permittedUris.remove(docsUri.toString())

        val entries = listOf(
            ManifestEntry("Camera/IMG_001.jpg", 4000L, 1000L, "hash1", "present", 1L),
            ManifestEntry("Documents/doc.pdf", 5000L, 1001L, "hash2", "present", 1L)
        )

        val folderMappings = mapOf(
            "Camera" to cameraUri.toString(),
            "Documents" to docsUri.toString()
        )

        val plan = planner.createPlan(
            vaultTreeUri = fakeVaultUri,
            setId = "set_1",
            setName = "Pixel Backup",
            deviceLabel = "Pixel 8",
            selectedEntries = entries,
            destination = RestoreDestination.OriginalFolders(folderMappings),
            conflictPolicy = ConflictPolicy.KEEP_BOTH
        )

        assertEquals(1, plan.permissionGaps.size)
        assertEquals("Documents", plan.permissionGaps.first().folderLabel)
    }

    @Test
    fun testSpaceCheckDetection() = runBlocking {
        // Set phone free space to only 1 KB
        storage.phoneFreeSpace = 1024L

        val entries = listOf(
            ManifestEntry("Camera/IMG_001.jpg", 5000L, 1000L, "hash1", "present", 1L)
        )

        val folderMappings = mapOf("Camera" to cameraUri.toString())

        val plan = planner.createPlan(
            vaultTreeUri = fakeVaultUri,
            setId = "set_1",
            setName = "Pixel Backup",
            deviceLabel = "Pixel 8",
            selectedEntries = entries,
            destination = RestoreDestination.OriginalFolders(folderMappings),
            conflictPolicy = ConflictPolicy.KEEP_BOTH
        )

        assertFalse(plan.hasEnoughSpace)
        assertEquals(5000L, plan.totalBytesToCopy)
        assertEquals(1024L, plan.phoneFreeBytes)
    }

    @Test
    fun testFilterSubsets() {
        val entries = listOf(
            ManifestEntry("Camera/IMG_001.jpg", 1000L, 1L, "h1", "present", 1L),
            ManifestEntry("Camera/VID_001.mp4", 5000L, 2L, "h2", "present", 1L),
            ManifestEntry("Documents/file.pdf", 2000L, 3L, "h3", "present", 1L),
            ManifestEntry("Audio/song.mp3", 3000L, 4L, "h4", "present", 1L),
            ManifestEntry("Camera/old.jpg", 1000L, 5L, "h5", "deleted", 1L)
        )

        val photos = planner.filterEntries(entries, RestoreFilter.PHOTOS)
        assertEquals(1, photos.size)
        assertEquals("Camera/IMG_001.jpg", photos.first().path)

        val videos = planner.filterEntries(entries, RestoreFilter.VIDEOS)
        assertEquals(1, videos.size)
        assertEquals("Camera/VID_001.mp4", videos.first().path)

        val docs = planner.filterEntries(entries, RestoreFilter.DOCUMENTS)
        assertEquals(1, docs.size)
        assertEquals("Documents/file.pdf", docs.first().path)

        val deleted = planner.filterEntries(entries, RestoreFilter.DELETED)
        assertEquals(1, deleted.size)
        assertEquals("Camera/old.jpg", deleted.first().path)
    }
}
