package `in`.soumyaranjansahoo.dockdrop.core.engine

import android.net.Uri
import `in`.soumyaranjansahoo.dockdrop.core.crypto.EncryptedSetManager
import `in`.soumyaranjansahoo.dockdrop.core.crypto.PlainSetStreamCodec
import `in`.soumyaranjansahoo.dockdrop.core.crypto.SetStreamCodec
import `in`.soumyaranjansahoo.dockdrop.core.database.DropDatabase
import `in`.soumyaranjansahoo.dockdrop.core.database.JournalDao
import `in`.soumyaranjansahoo.dockdrop.core.database.JournalEntry
import `in`.soumyaranjansahoo.dockdrop.core.database.JournalStatus
import `in`.soumyaranjansahoo.dockdrop.core.database.updateStatus
import `in`.soumyaranjansahoo.dockdrop.core.manifest.ManifestEntry
import `in`.soumyaranjansahoo.dockdrop.core.manifest.ManifestRepository
import `in`.soumyaranjansahoo.dockdrop.core.manifest.SetMetadata
import `in`.soumyaranjansahoo.dockdrop.core.model.AttentionReason
import `in`.soumyaranjansahoo.dockdrop.core.model.DeletionPolicy
import `in`.soumyaranjansahoo.dockdrop.core.model.DockInfo
import `in`.soumyaranjansahoo.dockdrop.core.model.DropProgress
import `in`.soumyaranjansahoo.dockdrop.core.model.DropState
import `in`.soumyaranjansahoo.dockdrop.core.model.PauseReason
import `in`.soumyaranjansahoo.dockdrop.core.model.Receipt
import `in`.soumyaranjansahoo.dockdrop.core.model.SetDefinition
import `in`.soumyaranjansahoo.dockdrop.core.model.SkippedFile
import `in`.soumyaranjansahoo.dockdrop.core.model.SourceFolder
import `in`.soumyaranjansahoo.dockdrop.core.model.VerificationLevel
import `in`.soumyaranjansahoo.dockdrop.core.policy.DeletionPolicyPlanner
import `in`.soumyaranjansahoo.dockdrop.core.policy.PolicyCheckResult
import `in`.soumyaranjansahoo.dockdrop.core.repository.SetRepository
import `in`.soumyaranjansahoo.dockdrop.core.storage.SourceFileInfo
import `in`.soumyaranjansahoo.dockdrop.core.storage.StorageError
import `in`.soumyaranjansahoo.dockdrop.core.storage.StorageGateway
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class DropEngine(
    private val storageGateway: StorageGateway,
    private val manifestRepository: ManifestRepository,
    private val journalDao: JournalDao,
    private val setRepository: SetRepository,
    private val scope: CoroutineScope,
    private val encryptedSetManager: EncryptedSetManager? = null
) {
    constructor(
        storageGateway: StorageGateway,
        manifestRepository: ManifestRepository,
        database: DropDatabase,
        setRepository: SetRepository,
        scope: CoroutineScope,
        encryptedSetManager: EncryptedSetManager? = null
    ) : this(
        storageGateway = storageGateway,
        manifestRepository = manifestRepository,
        journalDao = database.journalDao(),
        setRepository = setRepository,
        scope = scope,
        encryptedSetManager = encryptedSetManager
    )

    private val _state = MutableStateFlow<DropState>(DropState.NoDock)
    val state: StateFlow<DropState> = _state.asStateFlow()

    private var activeJob: Job? = null
    @Volatile
    private var isPaused: Boolean = false

    @Volatile
    private var allowMassDeleteOverride: Boolean = false

    private var currentDock: DockInfo? = null

    fun updateDockState(dockInfo: DockInfo?) {
        currentDock = dockInfo
        if (dockInfo == null) {
            val currentState = _state.value
            if (currentState is DropState.Dropping || currentState is DropState.Verifying) {
                _state.value = DropState.Interrupted(
                    DropProgress(
                        setId = "",
                        currentFolder = "",
                        folderIndex = 0,
                        totalFolders = 0,
                        copiedFiles = 0,
                        totalFiles = 0,
                        copiedBytes = 0L,
                        totalBytes = 0L,
                        speedMbS = 0.0,
                        filesPerSec = 0.0,
                        estMinutesRemaining = 0.0,
                        currentFilePath = ""
                    )
                )
            } else if (currentState !is DropState.Resumable) {
                _state.value = DropState.NoDock
            }
        } else {
            val currentState = _state.value
            if (currentState is DropState.NoDock) {
                _state.value = DropState.DockDetected(dockInfo)
            }
        }
    }

    var lastActiveSet: SetDefinition? = null
        private set
    var lastActiveFolders: List<SourceFolder> = emptyList()
        private set

    fun startDrop(set: SetDefinition, sourceFolders: List<SourceFolder>) {
        if (activeJob?.isActive == true) return
        lastActiveSet = set
        lastActiveFolders = sourceFolders
        isPaused = false
        activeJob = scope.launch(Dispatchers.IO) {
            executePipeline(set, sourceFolders)
        }
    }

    fun confirmMassDeleteAndResume(set: SetDefinition? = null, sourceFolders: List<SourceFolder>? = null) {
        val targetSet = set ?: lastActiveSet
        val targetFolders = sourceFolders ?: lastActiveFolders
        if (targetSet != null) {
            allowMassDeleteOverride = true
            startDrop(targetSet, targetFolders)
        }
    }

    fun switchToKeepAndResume(set: SetDefinition? = null, sourceFolders: List<SourceFolder>? = null) {
        val targetSet = set ?: lastActiveSet
        val targetFolders = sourceFolders ?: lastActiveFolders
        if (targetSet != null) {
            scope.launch {
                val updated = targetSet.copy(deletionPolicy = DeletionPolicy.KEEP, mirrorConfirmed = false)
                setRepository.updateSet(updated)
                allowMassDeleteOverride = false
                startDrop(updated, targetFolders)
            }
        }
    }

    fun pause(reason: PauseReason = PauseReason.USER_REQUESTED) {
        if (isPaused) return
        if (reason == PauseReason.SYSTEM_TIMEOUT) {
            val current = _state.value
            val prog = when (current) {
                is DropState.Dropping -> current.progress
                is DropState.Paused -> current.progress
                else -> null
            }
            activeJob?.cancel()
            isPaused = false
            if (prog != null) {
                _state.value = DropState.Resumable(prog)
            }
            return
        }

        if (_state.value is DropState.Dropping) {
            isPaused = true
            val currentProg = (_state.value as DropState.Dropping).progress
            _state.value = DropState.Paused(reason, currentProg)
        }
    }

    fun resume(set: SetDefinition, sourceFolders: List<SourceFolder>) {
        if (isPaused) {
            isPaused = false
            // resume loop
        } else if (_state.value is DropState.Resumable || _state.value is DropState.Interrupted) {
            startDrop(set, sourceFolders)
        }
    }

    fun stop() {
        activeJob?.cancel()
        val current = _state.value
        val prog = when (current) {
            is DropState.Dropping -> current.progress
            is DropState.Paused -> current.progress
            else -> null
        }
        if (prog != null) {
            _state.value = DropState.Resumable(prog)
        } else {
            _state.value = DropState.NoDock
        }
    }

    private suspend fun executePipeline(
        set: SetDefinition,
        sourceFolders: List<SourceFolder>
    ) {
        val startTimeMs = System.currentTimeMillis()
        val vaultUri = Uri.parse(set.destinationVaultUriString)
        val skippedFiles = mutableListOf<SkippedFile>()

        try {
            // Stage 0: Preflight
            _state.value = DropState.Preflight

            if (!storageGateway.hasPersistedPermission(vaultUri)) {
                _state.value = DropState.Attention(
                    AttentionReason.PermissionLost(set.dockUserLabel, vaultUri.toString())
                )
                return
            }

            for (folder in sourceFolders) {
                val fUri = Uri.parse(folder.treeUriString)
                if (!storageGateway.hasPersistedPermission(fUri)) {
                    _state.value = DropState.Attention(
                        AttentionReason.PermissionLost(folder.label, folder.treeUriString)
                    )
                    return
                }
            }

            val volume = storageGateway.volumeInfo(vaultUri)
            if (!volume.isMounted) {
                _state.value = DropState.NoDock
                return
            }

            // Ensure drive.json exists on SSD
            manifestRepository.ensureDriveJson(
                vaultTreeUri = vaultUri,
                driveId = volume.uuid,
                deviceLabel = android.os.Build.MODEL
            )

            // Resolve codec per AGENTS.md Handoff Rule 5 (Engine never branches on isEncrypted)
            val codec: SetStreamCodec = encryptedSetManager?.resolveCodecForSet(
                vaultTreeUri = vaultUri,
                setId = set.id,
                isEncrypted = set.isEncrypted,
                keystoreWrappedMkHex = set.keystoreWrappedMkHex
            ) ?: PlainSetStreamCodec

            // Stage 1 & 2: Scan & Diff
            val activeManifest = manifestRepository.loadCurrentGeneration(vaultUri, set.id, codec)
            val manifestEntries = activeManifest.entries.toMutableMap()

            // Scan all source folders using batch cursor queries
            data class SourceItem(val folderLabel: String, val info: SourceFileInfo, val pathInSet: String)
            val allSourceItems = mutableListOf<SourceItem>()

            for (folder in sourceFolders) {
                val treeUri = Uri.parse(folder.treeUriString)
                storageGateway.listSourceFiles(treeUri).collect { fileInfo ->
                    val pathInSet = "${folder.label}/${fileInfo.relativePath}"
                    allSourceItems.add(SourceItem(folder.label, fileInfo, pathInSet))
                }
            }

            // A4 Deletion & Retention Policy Evaluation (CLAUDE.md Rule 6)
            val currentSourcePaths = allSourceItems.map { it.pathInSet }.toSet()
            val policyResult = DeletionPolicyPlanner.evaluate(
                setId = set.id,
                policy = set.deletionPolicy,
                retentionDays = set.retentionDays,
                mirrorConfirmed = set.mirrorConfirmed,
                allowMassDeleteOverride = allowMassDeleteOverride,
                activeManifestEntries = manifestEntries,
                currentSourcePaths = currentSourcePaths
            )

            when (policyResult) {
                is PolicyCheckResult.RequireMirrorConfirmation -> {
                    _state.value = DropState.Attention(
                        AttentionReason.PermissionLost(
                            folderLabel = "Mirror mode confirmation",
                            folderUri = "Mirror mode requires explicit confirmation before execution."
                        )
                    )
                    return
                }
                is PolicyCheckResult.TriggerMassDeleteGuard -> {
                    _state.value = DropState.Attention(
                        AttentionReason.MassDelete(
                            count = policyResult.candidateCount,
                            paths = policyResult.candidatePaths
                        )
                    )
                    return
                }
                is PolicyCheckResult.Proceed -> {
                    // Passed policy gate!
                }
            }

            val retentionPlan = (policyResult as PolicyCheckResult.Proceed).plan

            // Load existing journal entries for resume support
            val existingJournal = journalDao.getJournalForSet(set.id).associateBy { it.relativePath }

            // Determine what files need copying
            val filesToCopy = mutableListOf<SourceItem>()
            val filesToVerify = mutableListOf<SourceItem>()
            var plannedBytes = 0L

            for (item in allSourceItems) {
                val pathInSet = item.pathInSet
                val manifestEntry = manifestEntries[pathInSet]
                val journalEntry = existingJournal[pathInSet]

                val isUnchanged = manifestEntry != null &&
                        manifestEntry.sizeBytes == item.info.sizeBytes &&
                        manifestEntry.modifiedMs == item.info.lastModifiedMs

                if (journalEntry != null && journalEntry.status == JournalStatus.VERIFIED.name) {
                    continue
                }

                if (journalEntry != null && journalEntry.status == JournalStatus.COPIED.name) {
                    filesToVerify.add(item)
                    continue
                }

                if (isUnchanged) {
                    continue
                }

                filesToCopy.add(item)
                plannedBytes += item.info.sizeBytes
            }

            // Check SSD free space
            val freeBytes = storageGateway.freeBytes(vaultUri)
            if (plannedBytes > freeBytes) {
                _state.value = DropState.Attention(
                    AttentionReason.NoSpace(neededBytes = plannedBytes, freeBytes = freeBytes)
                )
                return
            }

            // Populate Journal for newly planned files
            val newJournalEntries = filesToCopy.map { item ->
                JournalEntry(
                    setId = set.id,
                    relativePath = item.pathInSet,
                    sourceFolderLabel = item.folderLabel,
                    sourceUriString = item.info.uri.toString(),
                    sizeBytes = item.info.sizeBytes,
                    sourceModifiedMs = item.info.lastModifiedMs,
                    status = JournalStatus.PLANNED.name
                )
            }
            journalDao.insertOrReplaceAll(newJournalEntries)

            // Stage 3: Copy Pass (1 MB reusable buffer + on-the-fly SHA-256 to .part)
            val buffer = ByteArray(1024 * 1024) // 1 MB reusable buffer per ADR-002
            var copiedFiles = 0
            var copiedBytes = 0L
            val totalFilesToCopy = filesToCopy.size
            val copyStartTime = System.currentTimeMillis()
            var lastUiEmissionTime = 0L

            for ((index, item) in filesToCopy.withIndex()) {
                while (isPaused) {
                    kotlinx.coroutines.delay(200)
                }

                val pathInSet = item.pathInSet
                val relVaultPath = codec.activeVaultPath(set.name, pathInSet)

                journalDao.updateStatus(set.id, pathInSet, JournalStatus.COPYING.name)

                var sha256Digest: String? = null
                try {
                    val digest = MessageDigest.getInstance("SHA-256")
                    var fileBytesWritten = 0L

                    storageGateway.openSourceInputStream(item.info.uri).use { inputStream ->
                        val rawPartOut = storageGateway.openDestinationPartOutputStream(vaultUri, relVaultPath)
                        codec.wrapOutputStream(rawPartOut).use { outputStream ->
                            var bytesRead: Int
                            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                                while (isPaused) {
                                    kotlinx.coroutines.delay(200)
                                }
                                digest.update(buffer, 0, bytesRead)
                                outputStream.write(buffer, 0, bytesRead)
                                fileBytesWritten += bytesRead
                                copiedBytes += bytesRead

                                // Throttle UI state emissions to 10 Hz (100 ms)
                                val now = System.currentTimeMillis()
                                if (now - lastUiEmissionTime >= 100) {
                                    lastUiEmissionTime = now
                                    val elapsedSec = maxOf(0.1, (now - copyStartTime) / 1000.0)
                                    val speedMbS = (copiedBytes / (1024.0 * 1024.0)) / elapsedSec
                                    val filesPerSec = copiedFiles / elapsedSec
                                    val remainingBytes = maxOf(0L, plannedBytes - copiedBytes)
                                    val estMinutesRemaining = if (speedMbS > 0) (remainingBytes / (1024.0 * 1024.0)) / (speedMbS * 60.0) else 0.0

                                    _state.value = DropState.Dropping(
                                        DropProgress(
                                            setId = set.id,
                                            currentFolder = item.folderLabel,
                                            folderIndex = sourceFolders.indexOfFirst { it.label == item.folderLabel } + 1,
                                            totalFolders = sourceFolders.size,
                                            copiedFiles = copiedFiles,
                                            totalFiles = totalFilesToCopy,
                                            copiedBytes = copiedBytes,
                                            totalBytes = plannedBytes,
                                            speedMbS = speedMbS,
                                            filesPerSec = filesPerSec,
                                            estMinutesRemaining = estMinutesRemaining,
                                            currentFilePath = item.info.relativePath
                                        )
                                    )
                                }
                            }
                            outputStream.flush()
                        }
                    }

                    sha256Digest = digest.digest().joinToString("") { "%02x".format(it) }

                    // LOAD-BEARING PHASE 0 R2 RULE:
                    // Atomic rename deletes any target first and renames .part -> final
                    storageGateway.renameAtomic(vaultUri, relVaultPath)

                    copiedFiles++
                    journalDao.updateStatus(
                        setId = set.id,
                        relativePath = pathInSet,
                        status = JournalStatus.COPIED.name,
                        expectedSha = sha256Digest
                    )
                    filesToVerify.add(item)
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    skippedFiles.add(
                        SkippedFile(
                            relativePath = pathInSet,
                            sourceFolderLabel = item.folderLabel,
                            sizeBytes = item.info.sizeBytes,
                            reason = e.message ?: "Write failure"
                        )
                    )
                    journalDao.updateStatus(
                        setId = set.id,
                        relativePath = pathInSet,
                        status = JournalStatus.FAILED.name,
                        errorReason = e.message
                    )
                }
            }

            // Stage 4: Verify Pass (Deferred FIFO Destination Re-read per ADR-002)
            val verifiedEntries = mutableListOf<ManifestEntry>()
            var verifiedCount = 0

            for ((index, item) in filesToVerify.withIndex()) {
                while (isPaused) {
                    kotlinx.coroutines.delay(200)
                }

                val pathInSet = item.pathInSet
                val relVaultPath = codec.activeVaultPath(set.name, pathInSet)

                _state.value = DropState.Verifying(
                    currentFolder = item.folderLabel,
                    folderIndex = sourceFolders.indexOfFirst { it.label == item.folderLabel } + 1,
                    totalFolders = sourceFolders.size,
                    verifiedFiles = verifiedCount,
                    totalFiles = filesToVerify.size
                )

                val journalEntry = journalDao.getJournalForSet(set.id).firstOrNull { it.relativePath == pathInSet }
                val expectedSha = journalEntry?.expectedSha256

                var destinationSha: String? = null
                try {
                    val digest = MessageDigest.getInstance("SHA-256")
                    val rawIn = storageGateway.openDestinationInputStream(vaultUri, relVaultPath)
                    codec.wrapInputStream(rawIn).use { inStream ->
                        var r: Int
                        while (inStream.read(buffer).also { r = it } != -1) {
                            digest.update(buffer, 0, r)
                        }
                    }
                    destinationSha = digest.digest().joinToString("") { "%02x".format(it) }

                    if (expectedSha != null && !destinationSha.equals(expectedSha, ignoreCase = true)) {
                        throw IllegalStateException("Hash mismatch on verification re-read: expected $expectedSha, got $destinationSha")
                    }

                    journalDao.updateStatus(
                        setId = set.id,
                        relativePath = pathInSet,
                        status = JournalStatus.VERIFIED.name,
                        destSha = destinationSha
                    )

                    verifiedCount++
                    val activeGen = activeManifest.generationNumber + 1
                    verifiedEntries.add(
                        ManifestEntry(
                            path = pathInSet,
                            sizeBytes = item.info.sizeBytes,
                            modifiedMs = item.info.lastModifiedMs,
                            sha256 = destinationSha,
                            status = "present",
                            generation = activeGen
                        )
                    )
                } catch (e: Exception) {
                    skippedFiles.add(
                        SkippedFile(
                            relativePath = pathInSet,
                            sourceFolderLabel = item.folderLabel,
                            sizeBytes = item.info.sizeBytes,
                            reason = e.message ?: "Verification failed"
                        )
                    )
                    journalDao.updateStatus(
                        setId = set.id,
                        relativePath = pathInSet,
                        status = JournalStatus.FAILED.name,
                        errorReason = e.message
                    )
                }
            }

            // Stage 5: Commit Protocol & Deletion Plan Execution (A4)
            var keptDeletionsCount = 0
            val dateFolder = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            val deletedEntryUpdates = mutableMapOf<String, ManifestEntry>()

            // 1. Move newly deleted files to _deleted/ (KEEP / KEEP_N_DAYS)
            for (del in retentionPlan.filesToMoveToDeleted) {
                val srcRelVaultPath = codec.activeVaultPath(set.name, del.path)
                val dstRelVaultPath = codec.deletedVaultPath(set.name, dateFolder, del.path)
                try {
                    storageGateway.move(vaultUri, srcRelVaultPath, dstRelVaultPath)
                    keptDeletionsCount++
                    deletedEntryUpdates[del.path] = del.copy(
                        status = "deleted",
                        deletedDate = dateFolder,
                        deletedPath = dstRelVaultPath
                    )
                } catch (e: Exception) {
                    deletedEntryUpdates[del.path] = del.copy(status = "deleted")
                }
            }

            // 2. Delete files from active location (MIRROR)
            for (del in retentionPlan.filesToDeleteFromActive) {
                val srcRelVaultPath = codec.activeVaultPath(set.name, del.path)
                try {
                    storageGateway.delete(vaultUri, srcRelVaultPath)
                } catch (e: Exception) {
                    // non-fatal if already removed
                }
            }

            // 3. Purge expired deleted entries from SSD (KEEP_N_DAYS retention job or MIRROR cleanup)
            for (del in retentionPlan.expiredDeletedEntriesToPurge) {
                if (!del.deletedPath.isNullOrEmpty()) {
                    try {
                        storageGateway.delete(vaultUri, del.deletedPath)
                    } catch (e: Exception) {
                        // non-fatal
                    }
                }
            }

            // Assemble new manifest generation
            val purgedPaths = retentionPlan.expiredDeletedEntriesToPurge.map { it.path }.toSet()
            val mirrorDeletedPaths = retentionPlan.filesToDeleteFromActive.map { it.path }.toSet()

            val updatedManifestEntries = mutableMapOf<String, ManifestEntry>()
            for ((p, e) in manifestEntries) {
                if (purgedPaths.contains(p) || mirrorDeletedPaths.contains(p)) {
                    continue
                }
                if (!currentSourcePaths.contains(p)) {
                    updatedManifestEntries[p] = deletedEntryUpdates[p] ?: e.copy(status = "deleted")
                } else {
                    updatedManifestEntries[p] = e
                }
            }

            for (v in verifiedEntries) {
                updatedManifestEntries[v.path] = v
            }

            val committedGeneration = manifestRepository.commitGeneration(
                vaultTreeUri = vaultUri,
                setId = set.id,
                allEntries = updatedManifestEntries.values.toList(),
                codec = codec
            )

            // Write Set metadata for cross-device & new-phone discovery
            try {
                manifestRepository.writeSetMetadata(
                    vaultTreeUri = vaultUri,
                    metadata = SetMetadata(
                        setId = set.id,
                        setName = set.name,
                        sourceLabels = sourceFolders.map { it.label },
                        updatedAtMs = System.currentTimeMillis(),
                        isEncrypted = set.isEncrypted
                    )
                )
            } catch (e: Exception) {
                // non-fatal
            }

            // Reset mass-delete override for future drops
            allowMassDeleteOverride = false

            // Stage 6: Sealed Receipt (S10)
            val elapsedTotalSec = maxOf(1L, (System.currentTimeMillis() - startTimeMs) / 1000L)
            val avgSpeedMbS = if (copiedBytes > 0) (copiedBytes / (1024.0 * 1024.0)) / elapsedTotalSec.toDouble() else 0.0
            val isSealed = skippedFiles.isEmpty()

            val receipt = Receipt(
                id = UUID.randomUUID().toString(),
                setId = set.id,
                setName = set.name,
                isSealed = isSealed,
                totalFilesVerified = verifiedEntries.size + (manifestEntries.size - filesToCopy.size),
                totalBytesVerified = copiedBytes + (activeManifest.entries.values.sumOf { it.sizeBytes } - plannedBytes),
                durationSeconds = elapsedTotalSec,
                averageSpeedMbS = avgSpeedMbS,
                verificationLevel = set.verificationLevel,
                generationNumber = committedGeneration.generationNumber,
                manifestSha256 = committedGeneration.sha256Checksum,
                keptDeletedFilesCount = keptDeletionsCount,
                skippedFiles = skippedFiles,
                timestampMs = System.currentTimeMillis()
            )

            setRepository.saveReceipt(receipt)
            setRepository.updateSetLastStatus(
                setId = set.id,
                status = if (isSealed) "Sealed" else "Partial",
                sealedMs = if (isSealed) receipt.timestampMs else null,
                files = receipt.totalFilesVerified,
                bytes = receipt.totalBytesVerified
            )

            // Clean up journal on success
            journalDao.clearJournalForSet(set.id)

            if (isSealed) {
                _state.value = DropState.Sealed(receipt)
            } else {
                _state.value = DropState.Partial(receipt, skippedFiles)
            }

        } catch (e: Exception) {
            if (e is CancellationException) {
                return
            }
            if (e is StorageError.VolumeGone) {
                _state.value = DropState.Interrupted(
                    DropProgress(
                        setId = set.id,
                        currentFolder = "",
                        folderIndex = 0,
                        totalFolders = sourceFolders.size,
                        copiedFiles = 0,
                        totalFiles = 0,
                        copiedBytes = 0L,
                        totalBytes = 0L,
                        speedMbS = 0.0,
                        filesPerSec = 0.0,
                        estMinutesRemaining = 0.0,
                        currentFilePath = ""
                    )
                )
            } else {
                _state.value = DropState.Attention(
                    AttentionReason.ReadErrors(skippedFiles.size.coerceAtLeast(1))
                )
            }
        }
    }
}
