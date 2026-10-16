package `in`.soumyaranjansahoo.dockdrop.core.manifest

import android.net.Uri
import `in`.soumyaranjansahoo.dockdrop.core.storage.StorageGateway
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.security.MessageDigest

open class ManifestRepository(
    private val storageGateway: StorageGateway
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
            val generation = loadCurrentGeneration(vaultTreeUri, setId)
            if (generation.generationNumber <= 0L && generation.entries.isEmpty()) {
                continue
            }
            val metadata = readSetMetadata(vaultTreeUri, setId)

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
                    isEncrypted = metadata?.isEncrypted ?: false
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
        setId: String
    ): ManifestGeneration = withContext(Dispatchers.IO) {
        val setDir = ".dockdrop/sets/$setId"
        val headPath = "$setDir/HEAD"
        val headText = storageGateway.readText(vaultTreeUri, headPath)?.trim()

        val headGen = headText?.toLongOrNull()
        if (headGen != null && headGen > 0) {
            val gen = tryLoadGeneration(vaultTreeUri, setId, headGen)
            if (gen != null) return@withContext gen
        }

        // Fallback: If HEAD is missing or points to invalid generation, probe backwards from 100
        for (candidateGen in 50L downTo 1L) {
            val gen = tryLoadGeneration(vaultTreeUri, setId, candidateGen)
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
        allEntries: List<ManifestEntry>
    ): ManifestGeneration = withContext(Dispatchers.IO) {
        val currentGen = loadCurrentGeneration(vaultTreeUri, setId)
        val newGenNumber = currentGen.generationNumber + 1
        val setDir = ".dockdrop/sets/$setId"

        val ndjsonRelPath = "$setDir/manifest.$newGenNumber.ndjson"
        val shaRelPath = "$setDir/manifest.$newGenNumber.sha256"
        val headRelPath = "$setDir/HEAD"

        // 1. Write manifest.<newGen>.ndjson.part while hashing
        val digest = MessageDigest.getInstance("SHA-256")
        storageGateway.openDestinationPartOutputStream(vaultTreeUri, ndjsonRelPath).use { outStream ->
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
        genNumber: Long
    ): ManifestGeneration? {
        val setDir = ".dockdrop/sets/$setId"
        val ndjsonPath = "$setDir/manifest.$genNumber.ndjson"
        val shaPath = "$setDir/manifest.$genNumber.sha256"

        val expectedShaLine = storageGateway.readText(vaultTreeUri, shaPath)?.trim() ?: return null
        val expectedSha = expectedShaLine.substringBefore(" ").trim()
        if (expectedSha.length != 64) return null

        val ndjsonContent = storageGateway.readText(vaultTreeUri, ndjsonPath) ?: return null

        // Verify SHA-256 of the NDJSON text
        val digest = MessageDigest.getInstance("SHA-256")
        val computedSha = digest.digest(ndjsonContent.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }

        if (!computedSha.equals(expectedSha, ignoreCase = true)) {
            return null // Checksum mismatch, invalid generation
        }

        val entries = mutableMapOf<String, ManifestEntry>()
        ndjsonContent.lineSequence().forEach { line ->
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

        return ManifestGeneration(
            generationNumber = genNumber,
            entries = entries,
            sha256Checksum = computedSha
        )
    }
}
