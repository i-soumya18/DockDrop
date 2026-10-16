package `in`.soumyaranjansahoo.dockdrop.core.restore

import android.content.Context
import android.net.Uri
import `in`.soumyaranjansahoo.dockdrop.core.model.SkippedFile
import `in`.soumyaranjansahoo.dockdrop.core.storage.StorageGateway
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest
import java.util.UUID

class RestoreEngine(
    private val storageGateway: StorageGateway,
    private val context: Context? = null,
    private val scope: CoroutineScope,
    private val cacheDir: File? = context?.cacheDir
) {

    private val _state = MutableStateFlow<RestoreState>(RestoreState.Idle)
    val state: StateFlow<RestoreState> = _state.asStateFlow()

    private var activeJob: Job? = null

    fun executeRestore(plan: RestorePlan) {
        if (activeJob?.isActive == true) return

        activeJob = scope.launch(Dispatchers.IO) {
            runRestorePipeline(plan)
        }
    }

    fun stop() {
        activeJob?.cancel()
        _state.value = RestoreState.Idle
    }

    private suspend fun runRestorePipeline(plan: RestorePlan) = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val vaultUri = Uri.parse(plan.vaultTreeUriString)
        val buffer = ByteArray(1024 * 1024) // 1 MB reusable buffer per ADR-002

        var restoredFiles = 0
        var restoredBytes = 0L
        var keptBothCount = 0
        var overwrittenCount = 0
        var failedCount = 0
        var timestampsPreserved = 0
        val failedFiles = mutableListOf<SkippedFile>()
        val distinctFolders = plan.itemsToTransfer.map { it.sourceFolderLabel }.distinct()

        // Track media scan paths per folderUriString
        val pathsByFolderUri = mutableMapOf<Uri, MutableList<String>>()

        val totalFiles = plan.totalFilesToCopy
        val totalBytes = plan.totalBytesToCopy
        var lastUiEmission = 0L

        try {
            for ((index, item) in plan.itemsToTransfer.withIndex()) {
                val folderIndex = distinctFolders.indexOf(item.sourceFolderLabel).coerceAtLeast(0) + 1

                val now = System.currentTimeMillis()
                if (now - lastUiEmission >= 100) {
                    lastUiEmission = now
                    val elapsedSec = maxOf(0.1, (now - startTime) / 1000.0)
                    val speedMbS = (restoredBytes / (1024.0 * 1024.0)) / elapsedSec
                    val filesPerSec = restoredFiles / elapsedSec
                    val remainingBytes = maxOf(0L, totalBytes - restoredBytes)
                    val estMinutes = if (speedMbS > 0) (remainingBytes / (1024.0 * 1024.0)) / (speedMbS * 60.0) else 0.0

                    _state.value = RestoreState.Restoring(
                        RestoreProgress(
                            currentFolder = item.sourceFolderLabel,
                            folderIndex = folderIndex,
                            totalFolders = distinctFolders.size.coerceAtLeast(1),
                            restoredFiles = restoredFiles,
                            totalFiles = totalFiles,
                            restoredBytes = restoredBytes,
                            totalBytes = totalBytes,
                            speedMbS = speedMbS,
                            filesPerSec = filesPerSec,
                            estMinutesRemaining = estMinutes,
                            currentFilePath = item.relativePathInFolder
                        )
                    )
                }

                // Execute transfer for item
                val success = restoreSingleItem(
                    vaultUri = vaultUri,
                    plan = plan,
                    item = item,
                    buffer = buffer,
                    onHashVerified = {
                        restoredFiles++
                        restoredBytes += item.manifestEntry.sizeBytes
                        if (item.hasConflict) {
                            if (plan.conflictPolicy == ConflictPolicy.KEEP_BOTH) {
                                keptBothCount++
                            } else if (plan.conflictPolicy == ConflictPolicy.OVERWRITE) {
                                overwrittenCount++
                            }
                        }
                    },
                    onTimestampPreserved = { timestampsPreserved++ },
                    onRecordScanPath = { targetUri, relPath ->
                        pathsByFolderUri.getOrPut(targetUri) { mutableListOf() }.add(relPath)
                    },
                    onError = { reason ->
                        failedCount++
                        failedFiles.add(
                            SkippedFile(
                                relativePath = item.resolvedTargetRelPath,
                                sourceFolderLabel = item.sourceFolderLabel,
                                sizeBytes = item.manifestEntry.sizeBytes,
                                reason = reason
                            )
                        )
                    }
                )
            }

            // Media Rescan Step (S15)
            if (!plan.isDrill && pathsByFolderUri.isNotEmpty()) {
                _state.value = RestoreState.UpdatingGallery
                for ((targetUri, paths) in pathsByFolderUri) {
                    try {
                        storageGateway.triggerMediaScan(targetUri, paths)
                    } catch (e: Exception) {
                        // non-fatal
                    }
                }
            }

            val elapsedSec = maxOf(1L, (System.currentTimeMillis() - startTime) / 1000L)
            val receipt = RestoreReceipt(
                id = UUID.randomUUID().toString(),
                setId = plan.setId,
                setName = plan.setName,
                restoredCount = restoredFiles,
                keptBothCount = keptBothCount,
                skippedConflictCount = plan.conflictCount.takeIf { plan.conflictPolicy == ConflictPolicy.SKIP } ?: 0,
                overwrittenCount = overwrittenCount,
                failedCount = failedCount,
                totalBytesRestored = restoredBytes,
                durationSeconds = elapsedSec,
                verificationSummary = "SHA-256 against backup",
                timestampsPreservedCount = timestampsPreserved,
                isDrill = plan.isDrill,
                failedFiles = failedFiles,
                timestampMs = System.currentTimeMillis()
            )

            if (failedCount == 0) {
                _state.value = RestoreState.Completed(receipt)
            } else {
                _state.value = RestoreState.Partial(receipt)
            }

        } catch (e: Exception) {
            if (e is CancellationException) {
                _state.value = RestoreState.Idle
                return@withContext
            }
            val elapsedSec = maxOf(1L, (System.currentTimeMillis() - startTime) / 1000L)
            val receipt = RestoreReceipt(
                id = UUID.randomUUID().toString(),
                setId = plan.setId,
                setName = plan.setName,
                restoredCount = restoredFiles,
                keptBothCount = keptBothCount,
                skippedConflictCount = 0,
                overwrittenCount = overwrittenCount,
                failedCount = failedCount.coerceAtLeast(1),
                totalBytesRestored = restoredBytes,
                durationSeconds = elapsedSec,
                verificationSummary = "SHA-256 against backup",
                timestampsPreservedCount = timestampsPreserved,
                isDrill = plan.isDrill,
                failedFiles = failedFiles,
                timestampMs = System.currentTimeMillis()
            )
            _state.value = RestoreState.Partial(receipt)
        }
    }

    private suspend fun restoreSingleItem(
        vaultUri: Uri,
        plan: RestorePlan,
        item: RestoreItem,
        buffer: ByteArray,
        onHashVerified: () -> Unit,
        onTimestampPreserved: () -> Unit,
        onRecordScanPath: (Uri, String) -> Unit,
        onError: (String) -> Unit
    ): Boolean {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")

            if (plan.destination is RestoreDestination.DrillSandbox) {
                val baseDir = cacheDir ?: context?.cacheDir ?: File(System.getProperty("java.io.tmpdir") ?: ".")
                val drillDir = File(baseDir, "restore_drill")
                drillDir.mkdirs()
                val partFile = File(drillDir, "${item.resolvedTargetRelPath}.part")
                partFile.parentFile?.mkdirs()

                storageGateway.openDestinationInputStream(vaultUri, item.vaultRelativePath).use { inStream ->
                    partFile.outputStream().use { outStream ->
                        var r: Int
                        while (inStream.read(buffer).also { r = it } != -1) {
                            digest.update(buffer, 0, r)
                            outStream.write(buffer, 0, r)
                        }
                        outStream.flush()
                    }
                }

                val computedSha = digest.digest().joinToString("") { "%02x".format(it) }
                if (!computedSha.equals(item.manifestEntry.sha256, ignoreCase = true)) {
                    partFile.delete()
                    onError("SHA-256 hash mismatch during drill verification")
                    return false
                }

                // Hash matched 100%! Drill succeeds, clean up temp file
                partFile.delete()
                onHashVerified()
                onTimestampPreserved()
                true
            } else {
                // SAF Destination (OriginalFolders or ChosenFolder)
                val targetTreeUri = when (plan.destination) {
                    is RestoreDestination.OriginalFolders -> {
                        val uriStr = plan.destination.folderLabelToUri[item.sourceFolderLabel]
                            ?: throw IllegalStateException("No target folder mapped for ${item.sourceFolderLabel}")
                        Uri.parse(uriStr)
                    }
                    is RestoreDestination.ChosenFolder -> Uri.parse(plan.destination.treeUriString)
                    else -> throw IllegalStateException("Unexpected destination type")
                }

                // 1. Stream to .part on destination while hashing
                storageGateway.openDestinationInputStream(vaultUri, item.vaultRelativePath).use { inStream ->
                    storageGateway.openDestinationPartOutputStream(targetTreeUri, item.resolvedTargetRelPath).use { outStream ->
                        var r: Int
                        while (inStream.read(buffer).also { r = it } != -1) {
                            digest.update(buffer, 0, r)
                            outStream.write(buffer, 0, r)
                        }
                        outStream.flush()
                    }
                }

                // 2. Pre-rename verification against manifest SHA-256
                val computedSha = digest.digest().joinToString("") { "%02x".format(it) }
                if (!computedSha.equals(item.manifestEntry.sha256, ignoreCase = true)) {
                    // Abort write: delete .part file, NEVER rename over or create target file!
                    try {
                        storageGateway.delete(targetTreeUri, "${item.resolvedTargetRelPath}.part")
                    } catch (e: Exception) {
                        // ignore
                    }
                    onError("SHA-256 hash mismatch: expected ${item.manifestEntry.sha256}, computed $computedSha")
                    return false
                }

                // 3. Atomic rename to final name
                storageGateway.renameAtomic(targetTreeUri, item.resolvedTargetRelPath)

                // 4. Best-effort original timestamp preservation
                if (item.manifestEntry.modifiedMs > 0) {
                    try {
                        val preserved = storageGateway.setLastModified(
                            targetTreeUri,
                            item.resolvedTargetRelPath,
                            item.manifestEntry.modifiedMs
                        )
                        if (preserved) onTimestampPreserved()
                    } catch (e: Exception) {
                        // non-fatal
                    }
                }

                onRecordScanPath(targetTreeUri, item.resolvedTargetRelPath)
                onHashVerified()
                true
            }
        } catch (e: Exception) {
            onError(e.message ?: "Restore stream error")
            false
        }
    }
}
