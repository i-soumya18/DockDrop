package `in`.soumyaranjansahoo.dockdrop.core.crypto

import android.net.Uri
import `in`.soumyaranjansahoo.dockdrop.core.storage.SourceFileInfo
import `in`.soumyaranjansahoo.dockdrop.core.storage.StorageGateway
import `in`.soumyaranjansahoo.dockdrop.core.storage.VolumeInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.ConcurrentHashMap

class EncryptedDropAndRestoreTest {

    private class InMemoryStorageGateway : StorageGateway {
        val files = ConcurrentHashMap<String, ByteArray>()

        override fun listSourceFiles(treeUri: Uri): Flow<SourceFileInfo> = emptyFlow()
        override suspend fun listVaultFiles(vaultTreeUri: Uri, rootSubdir: String): Map<String, Long> = emptyMap()
        override suspend fun openSourceInputStream(fileUri: Uri): InputStream = ByteArrayInputStream(ByteArray(0))

        override suspend fun openDestinationInputStream(vaultTreeUri: Uri, relativePath: String): InputStream {
            val bytes = files[relativePath] ?: throw IllegalArgumentException("Not found: $relativePath")
            return ByteArrayInputStream(bytes)
        }

        override suspend fun openDestinationPartOutputStream(vaultTreeUri: Uri, relativePath: String): OutputStream {
            return object : ByteArrayOutputStream() {
                override fun close() {
                    super.close()
                    files["$relativePath.part"] = toByteArray()
                }
            }
        }

        override suspend fun renameAtomic(vaultTreeUri: Uri, relativePath: String): Boolean {
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

    private val fakeVaultUri: Uri = android.net.TestUri()
    private val setId = "set_family_photos"
    private val setName = "FamilyPhotos"
    private val passphrase = "CorrectHorseBatteryStaple2026!"

    @Test
    fun testEncryptedSetCreation_backup_andRestoreOnCleanInstallWithPassphraseAndRecoveryKey() = runBlocking {
        val storageGateway = InMemoryStorageGateway()
        val originalManager = EncryptedSetManager(storageGateway)

        // 1. Setup encrypted set
        val setupResult = originalManager.provisionNewSetEncryption(
            setId = setId,
            passphrase = passphrase
        )
        val recoveryKey = setupResult.recoveryKeyFormatted
        assertNotNull(recoveryKey)
        assertTrue(recoveryKey.length >= 32)

        originalManager.writeEncryptionMetadata(fakeVaultUri, setId, setupResult.metadata)

        // Check encryption.json was created on drive
        val configJson = storageGateway.readText(fakeVaultUri, ".dockdrop/sets/$setId/encryption.json")
        assertNotNull(configJson)
        assertTrue(configJson!!.contains("DDC1"))
        assertTrue(configJson.contains("scrypt"))
        assertTrue(configJson.contains("passphraseSlot"))
        assertTrue(configJson.contains("recoverySlot"))

        // 2. Perform encrypted backup using the active codec
        val activeCodec = originalManager.resolveCodecForSet(fakeVaultUri, setId, isEncrypted = true)
        assertTrue(activeCodec is EncryptedSetStreamCodec)

        val relativeFilePath = "DCIM/Camera/vacation.jpg"
        val originalPayload = "JPEG_SIMULATED_CONTENT_BYTES_DOCKDROP_PHASE_4".toByteArray(Charsets.UTF_8)

        val vaultEncryptedPath = activeCodec.activeVaultPath(setName, relativeFilePath)
        assertTrue("Vault path must follow <setName>.enc/blobs/xx/<blobId>.enc",
            vaultEncryptedPath.startsWith("$setName.enc/blobs/"))
        assertTrue(vaultEncryptedPath.endsWith(".enc"))

        // Stream write encrypted file
        val partOut = storageGateway.openDestinationPartOutputStream(fakeVaultUri, vaultEncryptedPath)
        activeCodec.wrapOutputStream(partOut).use { encOut ->
            encOut.write(originalPayload)
        }
        storageGateway.renameAtomic(fakeVaultUri, vaultEncryptedPath)
        assertTrue(storageGateway.exists(fakeVaultUri, vaultEncryptedPath))

        // 3. Clean Install Simulation 1 (New device or app reinstall)
        val cleanInstallManager1 = EncryptedSetManager(storageGateway)
        assertFalse("Set must start locked on clean install",
            cleanInstallManager1.isSetUnlocked(setId))

        // Wrong passphrase fails fast
        val wrongPassResult = cleanInstallManager1.unlockWithPassphrase(fakeVaultUri, setId, "WrongPassword123!")
        assertFalse("Wrong passphrase must fail fast and return false", wrongPassResult)
        assertFalse("Set must remain locked after failed passphrase",
            cleanInstallManager1.isSetUnlocked(setId))

        // Correct passphrase succeeds
        val rightPassResult = cleanInstallManager1.unlockWithPassphrase(fakeVaultUri, setId, passphrase)
        assertTrue("Correct passphrase must unlock set", rightPassResult)
        assertTrue(cleanInstallManager1.isSetUnlocked(setId))

        // Restore file using newly unlocked codec
        val restoredCodec1 = cleanInstallManager1.resolveCodecForSet(fakeVaultUri, setId, isEncrypted = true)
        val encIn1 = storageGateway.openDestinationInputStream(fakeVaultUri, vaultEncryptedPath)
        val restoredOut1 = ByteArrayOutputStream()
        restoredCodec1.wrapInputStream(encIn1).use { decIn ->
            decIn.copyTo(restoredOut1)
        }
        assertArrayEquals("Restored bytes with passphrase must be 100% identical",
            originalPayload, restoredOut1.toByteArray())

        // 4. Clean Install Simulation 2 (Restore using Recovery Key)
        val cleanInstallManager2 = EncryptedSetManager(storageGateway)
        assertFalse(cleanInstallManager2.isSetUnlocked(setId))

        // Test with lowercased recovery key (testing case/confusable tolerance)
        val recoveryKeyInput = recoveryKey.lowercase()
        val unlockWithRecoveryResult = cleanInstallManager2.unlockWithRecoveryKey(fakeVaultUri, setId, recoveryKeyInput)
        assertTrue("Recovery key must unlock set on clean install", unlockWithRecoveryResult)
        assertTrue(cleanInstallManager2.isSetUnlocked(setId))

        val restoredCodec2 = cleanInstallManager2.resolveCodecForSet(fakeVaultUri, setId, isEncrypted = true)
        val encIn2 = storageGateway.openDestinationInputStream(fakeVaultUri, vaultEncryptedPath)
        val restoredOut2 = ByteArrayOutputStream()
        restoredCodec2.wrapInputStream(encIn2).use { decIn ->
            decIn.copyTo(restoredOut2)
        }
        assertArrayEquals("Restored bytes with recovery key must be 100% identical",
            originalPayload, restoredOut2.toByteArray())
    }
}
