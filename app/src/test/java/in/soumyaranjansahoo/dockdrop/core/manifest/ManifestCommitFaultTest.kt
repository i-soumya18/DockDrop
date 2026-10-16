package `in`.soumyaranjansahoo.dockdrop.core.manifest

import android.net.Uri
import `in`.soumyaranjansahoo.dockdrop.core.storage.SourceFileInfo
import `in`.soumyaranjansahoo.dockdrop.core.storage.StorageGateway
import `in`.soumyaranjansahoo.dockdrop.core.storage.VolumeInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.ConcurrentHashMap

/**
 * Fault-injection test suite for AGENTS.md A4 completion bar:
 * "Kill the process between every step of the commit protocol (fault injection): the app always starts with a valid generation."
 */
class ManifestCommitFaultTest {

    private class FaultInjectableStorageGateway : StorageGateway {
        val files = ConcurrentHashMap<String, ByteArray>()
        var failOnPartWrite: String? = null
        var failOnRename: String? = null
        var failOnHeadWrite: Boolean = false

        override fun listSourceFiles(treeUri: Uri): Flow<SourceFileInfo> = emptyFlow()
        override suspend fun listVaultFiles(vaultTreeUri: Uri, rootSubdir: String): Map<String, Long> = emptyMap()
        override suspend fun openSourceInputStream(fileUri: Uri): InputStream = ByteArrayInputStream(ByteArray(0))

        override suspend fun openDestinationInputStream(vaultTreeUri: Uri, relativePath: String): InputStream {
            val bytes = files[relativePath] ?: throw IllegalArgumentException("Not found: $relativePath")
            return ByteArrayInputStream(bytes)
        }

        override suspend fun openDestinationPartOutputStream(vaultTreeUri: Uri, relativePath: String): OutputStream {
            if (failOnPartWrite != null && relativePath.contains(failOnPartWrite!!)) {
                throw RuntimeException("Simulated crash writing $relativePath")
            }
            return object : ByteArrayOutputStream() {
                override fun close() {
                    super.close()
                    files["$relativePath.part"] = toByteArray()
                }
            }
        }

        override suspend fun renameAtomic(vaultTreeUri: Uri, relativePath: String): Boolean {
            if (failOnRename != null && relativePath.contains(failOnRename!!)) {
                throw RuntimeException("Simulated crash renaming $relativePath")
            }
            val part = files.remove("$relativePath.part") ?: throw IllegalArgumentException("Part not found: $relativePath.part")
            files[relativePath] = part
            return true
        }

        override suspend fun move(vaultTreeUri: Uri, srcRelativePath: String, dstRelativePath: String): Boolean {
            val bytes = files.remove(srcRelativePath) ?: return false
            files[dstRelativePath] = bytes
            return true
        }

        override suspend fun delete(vaultTreeUri: Uri, relativePath: String): Boolean {
            files.remove(relativePath)
            return true
        }

        override suspend fun exists(vaultTreeUri: Uri, relativePath: String): Boolean = files.containsKey(relativePath)

        override suspend fun readText(vaultTreeUri: Uri, relativePath: String): String? {
            val bytes = files[relativePath] ?: return null
            return String(bytes, Charsets.UTF_8)
        }

        override suspend fun writeTextAtomic(vaultTreeUri: Uri, relativePath: String, content: String): Boolean {
            if (failOnHeadWrite && relativePath.contains("HEAD")) {
                throw RuntimeException("Simulated crash writing HEAD")
            }
            files[relativePath] = content.toByteArray(Charsets.UTF_8)
            return true
        }

        override suspend fun freeBytes(vaultTreeUri: Uri): Long = 100L * 1024 * 1024 * 1024
        override suspend fun volumeInfo(vaultTreeUri: Uri): VolumeInfo =
            VolumeInfo("4E21-0000", "SSD", 128L * 1024 * 1024 * 1024, 100L * 1024 * 1024 * 1024, "exFAT", true)
        override fun hasPersistedPermission(treeUri: Uri): Boolean = true
        override suspend fun runWriteSpeedTest(vaultTreeUri: Uri, testSizeBytes: Long): Double = 35.9
        override suspend fun listChildDirectories(treeUri: Uri, relativePath: String): List<String> = emptyList()
        override suspend fun phoneFreeBytes(): Long = 50L * 1024 * 1024 * 1024
        override suspend fun setLastModified(treeUri: Uri, relativePath: String, lastModifiedMs: Long): Boolean = true
        override suspend fun triggerMediaScan(treeUri: Uri, relativePaths: List<String>) {}
    }

    private val fakeUri: Uri = android.net.TestUri()
    private val setId = "test_set_1"

    @Test
    fun testSuccessfulCommitAndLoad() = runBlocking {
        val storage = FaultInjectableStorageGateway()
        val repo = ManifestRepository(storage)

        val entries = listOf(
            ManifestEntry("Camera/IMG_001.jpg", 1024, 1000L, "aabbcc", "present", 1),
            ManifestEntry("Camera/IMG_002.jpg", 2048, 2000L, "ddeeff", "present", 1)
        )

        val committed = repo.commitGeneration(fakeUri, setId, entries)
        assertEquals(1L, committed.generationNumber)
        assertEquals(2, committed.entries.size)

        val loaded = repo.loadCurrentGeneration(fakeUri, setId)
        assertEquals(1L, loaded.generationNumber)
        assertEquals(2, loaded.entries.size)
        assertEquals("aabbcc", loaded.entries["Camera/IMG_001.jpg"]?.sha256)
    }

    @Test
    fun testCrashDuringHeadRewrite_RecoversValidGeneration() = runBlocking {
        val storage = FaultInjectableStorageGateway()
        val repo = ManifestRepository(storage)

        // Generation 1 succeeds
        val gen1Entries = listOf(
            ManifestEntry("Camera/IMG_001.jpg", 1024, 1000L, "aabbcc", "present", 1)
        )
        repo.commitGeneration(fakeUri, setId, gen1Entries)

        // Inject simulated process death right before or during HEAD rewrite of Gen 2
        storage.failOnHeadWrite = true
        val gen2Entries = listOf(
            ManifestEntry("Camera/IMG_001.jpg", 1024, 1000L, "aabbcc", "present", 1),
            ManifestEntry("Camera/IMG_002.jpg", 2048, 2000L, "ddeeff", "present", 2)
        )

        try {
            repo.commitGeneration(fakeUri, setId, gen2Entries)
        } catch (e: Exception) {
            // Simulated crash happened
        }

        // On next start, the repository MUST recover and load valid Generation 1
        val loaded = repo.loadCurrentGeneration(fakeUri, setId)
        assertEquals(1L, loaded.generationNumber)
        assertEquals(1, loaded.entries.size)
        assertEquals("aabbcc", loaded.entries["Camera/IMG_001.jpg"]?.sha256)
    }

    @Test
    fun testCrashDuringNdjsonRename_PreservesPreviousGeneration() = runBlocking {
        val storage = FaultInjectableStorageGateway()
        val repo = ManifestRepository(storage)

        // Generation 1 succeeds
        repo.commitGeneration(
            fakeUri,
            setId,
            listOf(ManifestEntry("Camera/IMG_001.jpg", 1024, 1000L, "aabbcc", "present", 1))
        )

        // Crash during rename of manifest.2.ndjson
        storage.failOnRename = "manifest.2.ndjson"
        try {
            repo.commitGeneration(
                fakeUri,
                setId,
                listOf(ManifestEntry("Camera/IMG_002.jpg", 2048, 2000L, "112233", "present", 2))
            )
        } catch (e: Exception) {
            // expected
        }

        // App starts: must still be valid Gen 1
        val loaded = repo.loadCurrentGeneration(fakeUri, setId)
        assertEquals(1L, loaded.generationNumber)
        assertEquals(1, loaded.entries.size)
    }

    @Test
    fun testCorruptHeadFile_FallsBackToHighestValidGeneration() = runBlocking {
        val storage = FaultInjectableStorageGateway()
        val repo = ManifestRepository(storage)

        // Commit Gen 1 and Gen 2
        repo.commitGeneration(
            fakeUri,
            setId,
            listOf(ManifestEntry("Camera/IMG_001.jpg", 1024, 1000L, "aabbcc", "present", 1))
        )
        repo.commitGeneration(
            fakeUri,
            setId,
            listOf(
                ManifestEntry("Camera/IMG_001.jpg", 1024, 1000L, "aabbcc", "present", 1),
                ManifestEntry("Camera/IMG_002.jpg", 2048, 2000L, "ddeeff", "present", 2)
            )
        )

        // Corrupt HEAD with garbage text
        storage.files[".dockdrop/sets/$setId/HEAD"] = "GARBAGE_CRASH_DATA".toByteArray(Charsets.UTF_8)

        // loadCurrentGeneration must detect corrupt HEAD and fall back to Gen 2
        val loaded = repo.loadCurrentGeneration(fakeUri, setId)
        assertEquals(2L, loaded.generationNumber)
        assertEquals(2, loaded.entries.size)
    }
}
