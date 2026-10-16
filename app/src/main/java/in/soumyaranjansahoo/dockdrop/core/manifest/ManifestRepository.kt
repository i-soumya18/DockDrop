package `in`.soumyaranjansahoo.dockdrop.core.manifest

import android.net.Uri
import `in`.soumyaranjansahoo.dockdrop.core.crypto.EncryptedSetManager
import `in`.soumyaranjansahoo.dockdrop.core.crypto.PlainSetStreamCodec
import `in`.soumyaranjansahoo.dockdrop.core.crypto.SetStreamCodec
import `in`.soumyaranjansahoo.dockdrop.core.storage.StorageGateway
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.security.MessageDigest

open class ManifestRepository(
    private val storageGateway: StorageGateway,
    private val encryptedSetManager: EncryptedSetManager? = null
) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    /**
     * Ensures `<vault>/.dockdrop/drive.json` exists. If not, creates it atomically.
     */
    suspend fun ensureDriveJson(
        vaultTreeUri: Uri,
        driveId: String,
        deviceLabel: String
    ): DriveInfo = withContext(Dispatchers.IO) {
        val relPath = ".dockdrop/drive.json"
        val existingText = storageGateway.readText(vaultTreeUri, relPath)
        if (existingText != null) {
            try {
                return@withContext json.decodeFromString<DriveInfo>(existingText)
            } catch (e: Exception) {
                // If corrupted, recreate
            }
        }

        val driveInfo = DriveInfo(
            driveId = driveId,
            createdAt = System.currentTimeMillis(),
            formatVersion = 1,
            deviceLabel = deviceLabel
        )
        val text = json.encodeToString(driveInfo)
        storageGateway.writeTextAtomic(vaultTreeUri, relPath, text)
        driveInfo
    }

    /**
     * Reads `.dockdrop/drive.json` if it exists, without auto-creating one.
     */
    suspend fun readDriveInfo(vaultTreeUri: Uri): DriveInfo? = withContext(Dispatchers.IO) {
        val relPath = ".dockdrop/drive.json"
        val existingText = storageGateway.readText(vaultTreeUri, relPath) ?: return@withContext null
        try {
            json.decodeFromString<DriveInfo>(existingText)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Writes optional set metadata so other devices and clean installs can discover the set name and folders.
     */
    suspend fun writeSetMetadata(vaultTreeUri: Uri, metadata: SetMetadata): Boolean = withContext(Dispatchers.IO) {
        val relPath = ".dockdrop/sets/${metadata.setId}/set.json"
        val text = json.encodeToString(metadata)
        storageGateway.writeTextAtomic(vaultTreeUri, relPath, text)
    }

    /**
     * Reads set metadata if available.
     */
    suspend fun readSetMetadata(vaultTreeUri: Uri, setId: String): SetMetadata? = withContext(Dispatchers.IO) {
        val relPath = ".dockdrop/sets/$setId/set.json"
        val text = storageGateway.readText(vaultTreeUri, relPath) ?: return@withContext null
        try {
            json.decodeFromString<SetMetadata>(text)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Discovers all backup Sets and their active generations on the Vault drive.
     * Used by S12 Restore Hub and S16 New-Phone restore.
     */
    suspend fun discoverBackupsOnDrive(vaultTreeUri: Uri): List<DiscoveredBackupSet> = withContext(Dispatchers.IO) {
        val driveInfo = readDriveInfo(vaultTreeUri) ?: return@withContext emptyList()
        val setDirNames = storageGateway.listChildDirectories(vaultTreeUri, ".dockdrop/sets")
        val result = mutableListOf<DiscoveredBackupSet>()

        for (setId in setDirNames) {
            val metadata = readSetMetadata(vaultTreeUri, setId)
            val encMeta = encryptedSetManager?.readEncryptionMetadata(vaultTreeUri, setId)
            val isEncrypted = metadata?.isEncrypted == true || encMeta != null

            val codec = try {
                if (encryptedSetManager != null) {
                    encryptedSetManager.resolveCodecForSet(vaultTreeUri, setId, isEncrypted)
                } else {
                    PlainSetStreamCodec
                }
            } catch (e: Exception) {
                null
            }

            if (isEncrypted && codec == null) {
                // Set is encrypted and currently locked: return placeholder so UI can display S19 Unlock
                val headText = storageGateway.readText(vaultTreeUri, ".dockdrop/sets/$setId/HEAD")?.trim()
                val headGen = headText?.toLongOrNull() ?: 1L
                val setName = metadata?.setName ?: "Backup ($setId)"
                result.add(
                    DiscoveredBackupSet(
                        setId = setId,
                        setName = setName,
                        deviceLabel = driveInfo.deviceLabel,
                        driveId = driveInfo.driveId,
                        generation = ManifestGeneration(headGen, emptyMap(), ""),
                        sourceLabels = metadata?.sourceLabels ?: emptyList(),
                        lastModifiedMs = metadata?.updatedAtMs ?: driveInfo.createdAt,
                        isEncrypted = true
                    )
                )
                continue
            }

            val activeCodec = codec ?: PlainSetStreamCodec
            val generation = loadCurrentGeneration(vaultTreeUri, setId, activeCodec)
            if (generation.generationNumber <= 0L && generation.entries.isEmpty()) {
                continue
            }

            // Infer source labels if not in metadata
            val inferredSourceLabels = if (metadata != null && metadata.sourceLabels.isNotEmpty()) {
                metadata.sourceLabels
            } else {
                generation.entries.keys.map { it.substringBefore("/") }.distinct()
            }

            val setName = metadata?.setName ?: "Backup ($setId)"
            val lastModified = generation.entries.values.maxOfOrNull { it.modifiedMs } ?: driveInfo.createdAt

            result.add(
                DiscoveredBackupSet(
                    setId = setId,
                    setName = setName,
                    deviceLabel = driveInfo.deviceLabel,
                    driveId = driveInfo.driveId,
                    generation = generation,
                    sourceLabels = inferredSourceLabels,
                    lastModifiedMs = lastModified,
                    isEncrypted = isEncrypted
                )
            )
        }
        result
    }

    /**
     * Loads the active generation for a Set.
     * Follows HEAD, verifies against .sha256, and falls back to previous valid generation if HEAD is corrupt.
     */
    suspend fun loadCurrentGeneration(
        vaultTreeUri: Uri,
        setId: String,
        codec: SetStreamCodec = PlainSetStreamCodec
    ): ManifestGeneration = withContext(Dispatchers.IO) {
        val setDir = ".dockdrop/sets/$setId"
        val headPath = "$setDir/HEAD"
        val headText = storageGateway.readText(vaultTreeUri, headPath)?.trim()

        val headGen = headText?.toLongOrNull()
        if (headGen != null && headGen > 0) {
            val gen = tryLoadGeneration(vaultTreeUri, setId, headGen, codec)
            if (gen != null) return@withContext gen
        }

        // Fallback: If HEAD is missing or points to invalid generation, probe backwards from 50
        for (candidateGen in 50L downTo 1L) {
            val gen = tryLoadGeneration(vaultTreeUri, setId, candidateGen, codec)
            if (gen != null) {
                // Repair HEAD atomically
                try {
                    storageGateway.delete(vaultTreeUri, headPath)
                    storageGateway.writeTextAtomic(vaultTreeUri, headPath, "$candidateGen\n")
                } catch (e: Exception) {
                    // ignore
                }
                return@withContext gen
            }
        }

        // No existing generation found (First run)
        ManifestGeneration(generationNumber = 0L, entries = emptyMap(), sha256Checksum = "")
    }

    /**
     * Commits a new manifest generation following the fault-resilient commit protocol:
     * 1. Write `manifest.<newGen>.ndjson.part` + compute SHA-256
     * 2. Write `manifest.<newGen>.sha256.part`
     * 3. Atomically rename both `.part` files
     * 4. Verify checksum on the renamed file
     * 5. Atomically rewrite `HEAD` (delete old HEAD first per R2)
     */
    suspend fun commitGeneration(
        vaultTreeUri: Uri,
        setId: String,
        allEntries: List<ManifestEntry>,
        codec: SetStreamCodec = PlainSetStreamCodec
    ): ManifestGeneration = withContext(Dispatchers.IO) {
        val currentGen = loadCurrentGeneration(vaultTreeUri, setId, codec)
        val newGenNumber = currentGen.generationNumber + 1
        val setDir = ".dockdrop/sets/$setId"

        val ndjsonRelPath = "$setDir/manifest.$newGenNumber.ndjson"
        val shaRelPath = "$setDir/manifest.$newGenNumber.sha256"
        val headRelPath = "$setDir/HEAD"

        // 1. Write manifest.<newGen>.ndjson.part while hashing plaintext bytes
        val digest = MessageDigest.getInstance("SHA-256")
        val rawPartOut = storageGateway.openDestinationPartOutputStream(vaultTreeUri, ndjsonRelPath)
        codec.wrapOutputStream(rawPartOut).use { outStream ->
            for (entry in allEntries) {
                val line = json.encodeToString(entry) + "\n"
                val bytes = line.toByteArray(Charsets.UTF_8)
                digest.update(bytes)
                outStream.write(bytes)
            }
            outStream.flush()
        }
        val computedSha256 = digest.digest().joinToString("") { "%02x".format(it) }

        // 2. Write manifest.<newGen>.sha256.part
        storageGateway.openDestinationPartOutputStream(vaultTreeUri, shaRelPath).use { outStream ->
            val content = "$computedSha256  manifest.$newGenNumber.ndjson\n"
            outStream.write(content.toByteArray(Charsets.UTF_8))
            outStream.flush()
        }

        // 3. Atomically rename both part files
        storageGateway.renameAtomic(vaultTreeUri, ndjsonRelPath)
        storageGateway.renameAtomic(vaultTreeUri, shaRelPath)

        // 4. Verification re-read of sha256
        val readBackSha = storageGateway.readText(vaultTreeUri, shaRelPath)?.trim()?.substringBefore(" ")
        if (!readBackSha.equals(computedSha256, ignoreCase = true)) {
            throw IllegalStateException("Checksum verification failed after writing manifest $newGenNumber")
        }

        // 5. Rewrite HEAD atomically: write HEAD.part, delete old HEAD (R2), rename HEAD.part -> HEAD
        storageGateway.writeTextAtomic(vaultTreeUri, headRelPath, "$newGenNumber\n")

        val entriesMap = allEntries.associateBy { it.path }
        ManifestGeneration(
            generationNumber = newGenNumber,
            entries = entriesMap,
            sha256Checksum = computedSha256
        )
    }

    private suspend fun tryLoadGeneration(
        vaultTreeUri: Uri,
        setId: String,
        genNumber: Long,
        codec: SetStreamCodec
    ): ManifestGeneration? {
        val setDir = ".dockdrop/sets/$setId"
        val ndjsonPath = "$setDir/manifest.$genNumber.ndjson"
        val shaPath = "$setDir/manifest.$genNumber.sha256"

        val expectedShaLine = storageGateway.readText(vaultTreeUri, shaPath)?.trim() ?: return null
        val expectedSha = expectedShaLine.substringBefore(" ").trim()
        if (expectedSha.length != 64) return null

        val entries = mutableMapOf<String, ManifestEntry>()
        val digest = MessageDigest.getInstance("SHA-256")

        try {
            val rawIn = storageGateway.openDestinationInputStream(vaultTreeUri, ndjsonPath)
            codec.wrapInputStream(rawIn).bufferedReader(Charsets.UTF_8).useLines { lines ->
                for (line in lines) {
                    val lineBytes = (line + "\n").toByteArray(Charsets.UTF_8)
                    digest.update(lineBytes)
                    val trimmed = line.trim()
                    if (trimmed.isNotEmpty()) {
                        try {
                            val entry = json.decodeFromString<ManifestEntry>(trimmed)
                            entries[entry.path] = entry
                        } catch (e: Exception) {
                            // skip malformed line
                        }
                    }
                }
            }
        } catch (e: Exception) {
            return null
        }

        val computedSha = digest.digest().joinToString("") { "%02x".format(it) }
        if (!computedSha.equals(expectedSha, ignoreCase = true)) {
            return null // Checksum mismatch, invalid generation
        }

        return ManifestGeneration(
            generationNumber = genNumber,
            entries = entries,
            sha256Checksum = computedSha
        )
    }
}
