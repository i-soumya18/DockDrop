package `in`.soumyaranjansahoo.dockdrop.spikelab

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.SystemClock
import android.os.storage.StorageManager
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import kotlin.coroutines.coroutineContext
import kotlin.math.max

data class BenchProgress(
    val isRunning: Boolean = false,
    val title: String = "Idle",
    val detail: String = "",
    val fraction: Float = 0f
)

object SpikeRunners {

    private val _progress = MutableStateFlow(BenchProgress())
    val progress: StateFlow<BenchProgress> = _progress.asStateFlow()

    @Volatile
    private var cancelRequested = false

    fun requestCancel() {
        cancelRequested = true
    }

    /**
     * Evaluates a newly granted tree URI for G1..G7.
     */
    suspend fun evaluateGrantedTree(
        context: Context,
        record: GrantabilityRecord,
        treeUri: Uri
    ): GrantabilityRecord = withContext(Dispatchers.IO) {
        val cr = context.contentResolver
        try {
            val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            try {
                cr.takePersistableUriPermission(treeUri, takeFlags)
            } catch (_: SecurityException) {
                cr.takePersistableUriPermission(treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val rootDocId = DocumentsContract.getTreeDocumentId(treeUri)
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, rootDocId)
            var count = 0
            var firstFileDocId: String? = null
            cr.query(
                childrenUri,
                arrayOf(
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_MIME_TYPE
                ),
                null,
                null,
                null
            )?.use { cursor ->
                while (cursor.moveToNext()) {
                    count++
                    val docId = cursor.getString(0)
                    val mime = cursor.getString(1) ?: ""
                    if (firstFileDocId == null && mime != DocumentsContract.Document.MIME_TYPE_DIR) {
                        firstFileDocId = docId
                    }
                }
            }

            var canReadSample = false
            val sampleDocId = firstFileDocId
            if (sampleDocId != null) {
                val fileUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, sampleDocId)
                cr.openInputStream(fileUri)?.use { input ->
                    val buf = ByteArray(256)
                    input.read(buf)
                    canReadSample = true
                }
            } else {
                // Folder is empty; listing succeeded so read access is structurally valid
                canReadSample = true
            }

            record.copy(
                status = "GRANTED",
                grantedUri = treeUri.toString(),
                canList = true,
                canReadSample = canReadSample,
                childCount = count,
                notes = "DocId=$rootDocId (${count} direct children)"
            )
        } catch (e: Exception) {
            record.copy(
                status = "ERROR",
                grantedUri = treeUri.toString(),
                canList = false,
                canReadSample = false,
                notes = "${e.javaClass.simpleName}: ${e.message}"
            )
        }
    }

    /**
     * P1-P5: Runs SAF permission persistence probe on the active SSD tree URI.
     */
    suspend fun runPersistenceProbe(
        context: Context,
        treeUriString: String,
        scenarioTag: String,
        userNotes: String
    ): PersistenceProbeRecord = withContext(Dispatchers.IO) {
        val t0 = SystemClock.elapsedRealtime()
        val cr = context.contentResolver
        val persisted = cr.persistedUriPermissions.any {
            it.uri.toString() == treeUriString && it.isReadPermission && it.isWritePermission
        }

        var listOk = false
        var childCount = 0
        var writeReadDeleteOk = false
        var volUuid = "unknown"
        var volDesc = "unknown"
        var errNote = userNotes

        if (treeUriString.isNotBlank()) {
            try {
                val treeUri = Uri.parse(treeUriString)
                val rootDocId = DocumentsContract.getTreeDocumentId(treeUri)
                val volPrefix = rootDocId.substringBefore(':')
                val sm = context.getSystemService(Context.STORAGE_SERVICE) as StorageManager
                val sv = sm.storageVolumes.firstOrNull {
                    it.uuid.equals(volPrefix, ignoreCase = true) ||
                        (volPrefix.equals("primary", ignoreCase = true) && it.isPrimary)
                }
                if (sv != null) {
                    volUuid = sv.uuid ?: "primary"
                    volDesc = sv.getDescription(context)
                } else {
                    volUuid = volPrefix
                }

                val parentDocUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, rootDocId)
                val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, rootDocId)

                cr.query(
                    childrenUri,
                    arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID),
                    null,
                    null,
                    null
                )?.use { c ->
                    childCount = c.count
                    listOk = true
                }

                // Write + Read + Delete tiny 1 KB probe file
                val probeName = ".dockdrop_probe_${System.currentTimeMillis()}.tmp"
                val probeUri = DocumentsContract.createDocument(
                    cr,
                    parentDocUri,
                    "application/octet-stream",
                    probeName
                )
                if (probeUri != null) {
                    val expected = ByteArray(1024) { (it % 251).toByte() }
                    cr.openOutputStream(probeUri)?.use { out ->
                        out.write(expected)
                        out.flush()
                    }
                    val actual = cr.openInputStream(probeUri)?.use { it.readBytes() }
                    val deleted = DocumentsContract.deleteDocument(cr, probeUri)
                    writeReadDeleteOk = actual != null && actual.contentEquals(expected) && deleted
                }
            } catch (e: Exception) {
                errNote = (if (userNotes.isNotBlank()) "$userNotes | " else "") +
                    "${e.javaClass.simpleName}: ${e.message}"
            }
        } else {
            errNote = "No SSD tree URI granted yet"
        }

        val elapsed = SystemClock.elapsedRealtime() - t0
        val record = PersistenceProbeRecord(
            isoTime = SpikeStore.nowIso(),
            scenarioTag = scenarioTag,
            uriString = treeUriString,
            inPersistedList = persisted,
            listSucceeded = listOk,
            childCount = childCount,
            writeReadDeleteSucceeded = writeReadDeleteOk,
            volumeUuid = volUuid,
            volumeDescription = volDesc,
            elapsedMs = elapsed,
            notes = errNote
        )
        SpikeStore.appendPersistenceProbe(context, record)
        record
    }

    /**
     * R1-R4: Runs the rename and filename semantics test suite on the granted SSD tree.
     */
    suspend fun runRenameAndNameSuite(
        context: Context,
        treeUri: Uri
    ): List<RenameTestRecord> = withContext(Dispatchers.IO) {
        val cr = context.contentResolver
        val results = mutableListOf<RenameTestRecord>()
        _progress.value = BenchProgress(true, "Running R1–R4 Rename & Filename Suite", "Preparing test folder…", 0.05f)

        val rootDocId = DocumentsContract.getTreeDocumentId(treeUri)
        val rootDocUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, rootDocId)

        // Create clean test directory
        findChildByName(cr, treeUri, rootDocId, ".spikelab_rename_test")?.let {
            try { DocumentsContract.deleteDocument(cr, it) } catch (_: Exception) {}
        }
        val testDirUri = DocumentsContract.createDocument(
            cr,
            rootDocUri,
            DocumentsContract.Document.MIME_TYPE_DIR,
            ".spikelab_rename_test"
        ) ?: throw IllegalStateException("Could not create .spikelab_rename_test directory")
        val testDirDocId = DocumentsContract.getDocumentId(testDirUri)

        // R1: create x.part, rename to final name
        run {
            val t0 = SystemClock.elapsedRealtime()
            try {
                val partUri = DocumentsContract.createDocument(cr, testDirUri, "application/octet-stream", "x.part")!!
                val payload = ByteArray(16 * 1024) { 0x42 }
                cr.openOutputStream(partUri)?.use {
                    it.write(payload)
                    it.flush()
                }
                val renamedUri = DocumentsContract.renameDocument(cr, partUri, "x_final.bin")
                val elapsed = SystemClock.elapsedRealtime() - t0
                val displayName = renamedUri?.let { queryDisplayName(cr, it) } ?: "null"
                val partStillExists = findChildByName(cr, treeUri, testDirDocId, "x.part") != null
                results.add(
                    RenameTestRecord(
                        testId = "R1",
                        description = "Create x.part, write 16 KB, rename to x_final.bin",
                        inputName = "x.part → x_final.bin",
                        accepted = renamedUri != null && displayName == "x_final.bin" && !partStillExists,
                        actualDisplayName = displayName,
                        behaviorSummary = if (!partStillExists && displayName == "x_final.bin") "Atomic rename succeeded; .part gone" else "Unexpected state",
                        elapsedMs = elapsed,
                        details = "partStillExists=$partStillExists, finalUri=${renamedUri?.lastPathSegment}"
                    )
                )
            } catch (e: Exception) {
                results.add(
                    RenameTestRecord(
                        testId = "R1",
                        description = "Create x.part → rename to x_final.bin",
                        inputName = "x.part → x_final.bin",
                        accepted = false,
                        actualDisplayName = "ERROR",
                        behaviorSummary = "Failed with ${e.javaClass.simpleName}",
                        elapsedMs = SystemClock.elapsedRealtime() - t0,
                        details = e.message ?: ""
                    )
                )
            }
        }

        // R2: rename onto an existing name (fail, overwrite, or auto-suffix?)
        run {
            val t0 = SystemClock.elapsedRealtime()
            try {
                val existingUri = DocumentsContract.createDocument(cr, testDirUri, "application/octet-stream", "existing.bin")!!
                cr.openOutputStream(existingUri)?.use { it.write("OLD_DATA".toByteArray()) }

                val incomingPartUri = DocumentsContract.createDocument(cr, testDirUri, "application/octet-stream", "incoming.part")!!
                cr.openOutputStream(incomingPartUri)?.use { it.write("NEW_OVERWRITE_DATA".toByteArray()) }

                val renamedUri = DocumentsContract.renameDocument(cr, incomingPartUri, "existing.bin")
                val elapsed = SystemClock.elapsedRealtime() - t0
                val actualName = renamedUri?.let { queryDisplayName(cr, it) } ?: "null"
                val existingContent = cr.openInputStream(existingUri)?.use { String(it.readBytes()) } ?: ""

                val summary = when {
                    renamedUri == null -> "Returned null URI"
                    actualName != "existing.bin" -> "AUTO-SUFFIXED to '$actualName' (original untouched)"
                    existingContent == "NEW_OVERWRITE_DATA" -> "OVERWROTE existing file in place"
                    else -> "Renamed to '$actualName' (existingContent='$existingContent')"
                }

                results.add(
                    RenameTestRecord(
                        testId = "R2",
                        description = "Rename incoming.part onto existing.bin",
                        inputName = "incoming.part → existing.bin",
                        accepted = renamedUri != null,
                        actualDisplayName = actualName,
                        behaviorSummary = summary,
                        elapsedMs = elapsed,
                        details = "existingContent='$existingContent'"
                    )
                )
            } catch (e: Exception) {
                results.add(
                    RenameTestRecord(
                        testId = "R2",
                        description = "Rename incoming.part onto existing.bin",
                        inputName = "incoming.part → existing.bin",
                        accepted = false,
                        actualDisplayName = "THREW_EXCEPTION",
                        behaviorSummary = "THROWS ${e.javaClass.simpleName} (must delete target before rename)",
                        elapsedMs = SystemClock.elapsedRealtime() - t0,
                        details = "${e.javaClass.simpleName}: ${e.message}"
                    )
                )
            }
        }

        // R3: moveDocument support (between folders on SSD)
        run {
            val t0 = SystemClock.elapsedRealtime()
            try {
                val srcDir = DocumentsContract.createDocument(cr, testDirUri, DocumentsContract.Document.MIME_TYPE_DIR, "move_src")!!
                val dstDir = DocumentsContract.createDocument(cr, testDirUri, DocumentsContract.Document.MIME_TYPE_DIR, "move_dst")!!
                val fileInSrc = DocumentsContract.createDocument(cr, srcDir, "application/octet-stream", "movable.bin")!!
                cr.openOutputStream(fileInSrc)?.use { it.write("MOVE_PAYLOAD".toByteArray()) }

                val movedUri = DocumentsContract.moveDocument(cr, fileInSrc, srcDir, dstDir)
                val elapsed = SystemClock.elapsedRealtime() - t0
                val dstDocId = DocumentsContract.getDocumentId(dstDir)
                val foundInDst = findChildByName(cr, treeUri, dstDocId, "movable.bin") != null
                results.add(
                    RenameTestRecord(
                        testId = "R3",
                        description = "DocumentsContract.moveDocument(file, srcDir, dstDir)",
                        inputName = "move_src/movable.bin → move_dst/",
                        accepted = movedUri != null && foundInDst,
                        actualDisplayName = movedUri?.let { queryDisplayName(cr, it) } ?: "null",
                        behaviorSummary = if (movedUri != null && foundInDst) "SUPPORTED (moved in ${elapsed}ms)" else "Returned null or not in dst",
                        elapsedMs = elapsed,
                        details = "foundInDst=$foundInDst, movedUri=${movedUri?.lastPathSegment}"
                    )
                )
            } catch (e: Exception) {
                results.add(
                    RenameTestRecord(
                        testId = "R3",
                        description = "DocumentsContract.moveDocument(file, srcDir, dstDir)",
                        inputName = "move_src/movable.bin → move_dst/",
                        accepted = false,
                        actualDisplayName = "UNSUPPORTED",
                        behaviorSummary = "THROWS ${e.javaClass.simpleName}",
                        elapsedMs = SystemClock.elapsedRealtime() - t0,
                        details = "${e.javaClass.simpleName}: ${e.message}"
                    )
                )
            }
        }

        // R4: Edge-case filenames
        val r4Cases = listOf(
            Triple("R4a", "Spaces in filename", "file with   multiple spaces.txt"),
            Triple("R4b", "Odia & Hindi script", "ଓଡ଼ିଆ_हिन्दी_परीक्षा.txt"),
            Triple("R4c", "Emoji in filename", "📸_DockDrop_🔒_test.txt"),
            Triple("R4d", "200-character filename", "a".repeat(196) + ".txt"),
            Triple("R4e", "Trailing dots", "trailing_dots..."),
            Triple("R4f", "Colon & question mark (: and ?)", "time_12:30_what?.txt"),
            Triple("R4g", "Other exFAT reserved (* \" < > |)", "bad_*_\"_<_>_|.txt")
        )

        r4Cases.forEachIndexed { idx, (id, desc, inputName) ->
            _progress.value = BenchProgress(true, "Running $id", desc, 0.4f + (idx * 0.08f))
            val t0 = SystemClock.elapsedRealtime()
            try {
                val createdUri = DocumentsContract.createDocument(
                    cr,
                    testDirUri,
                    "application/octet-stream",
                    inputName
                )
                val elapsed = SystemClock.elapsedRealtime() - t0
                if (createdUri != null) {
                    cr.openOutputStream(createdUri)?.use { it.write("OK".toByteArray()) }
                    val actualName = queryDisplayName(cr, createdUri)
                    val exactMatch = actualName == inputName
                    val summary = if (exactMatch) {
                        "ACCEPTED AS-IS (len=${actualName.length})"
                    } else {
                        "REWRITTEN by provider to '$actualName'"
                    }
                    results.add(
                        RenameTestRecord(
                            testId = id,
                            description = desc,
                            inputName = inputName,
                            accepted = true,
                            actualDisplayName = actualName,
                            behaviorSummary = summary,
                            elapsedMs = elapsed,
                            details = "exactMatch=$exactMatch"
                        )
                    )
                } else {
                    results.add(
                        RenameTestRecord(
                            testId = id,
                            description = desc,
                            inputName = inputName,
                            accepted = false,
                            actualDisplayName = "null",
                            behaviorSummary = "REJECTED (createDocument returned null)",
                            elapsedMs = elapsed,
                            details = "Provider refused name"
                        )
                    )
                }
            } catch (e: Exception) {
                results.add(
                    RenameTestRecord(
                        testId = id,
                        description = desc,
                        inputName = inputName,
                        accepted = false,
                        actualDisplayName = "EXCEPTION",
                        behaviorSummary = "REJECTED (${e.javaClass.simpleName})",
                        elapsedMs = SystemClock.elapsedRealtime() - t0,
                        details = "${e.javaClass.simpleName}: ${e.message}"
                    )
                )
            }
        }

        // Cleanup test dir
        try {
            DocumentsContract.deleteDocument(cr, testDirUri)
        } catch (_: Exception) {
        }

        SpikeStore.replaceRenameResults(context, results)
        _progress.value = BenchProgress(false, "Completed R1–R4 Suite", "${results.size} checks recorded", 1f)
        results
    }

    /**
     * B1, B2, B3 Write & B4 Read-Back Throughput Suite.
     */
    suspend fun runThroughputWorkload(
        context: Context,
        treeUri: Uri,
        testId: String,        // "B1", "B2", "B3", "B4-B1", "B4-B2", "B4-B3"
        bufferSizeKb: Int,     // 256, 1024, 4096
        iterations: Int = 3
    ) = withContext(Dispatchers.IO) {
        cancelRequested = false
        val cr = context.contentResolver
        val rootDocId = DocumentsContract.getTreeDocumentId(treeUri)
        val rootDocUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, rootDocId)

        val benchRootUri = getOrCreateDir(cr, treeUri, rootDocId, rootDocUri, ".spikelab_bench")
        val benchRootDocId = DocumentsContract.getDocumentId(benchRootUri)

        for (runNum in 1..iterations) {
            if (!coroutineContext.isActive || cancelRequested) break
            val batStart = SpikeStore.readBatteryTelemetry(context)
            val heapBefore = usedHeapMb()

            when (testId) {
                "B1" -> {
                    val dirUri = recreateSubdir(cr, treeUri, benchRootDocId, benchRootUri, "b1_1gb")
                    val totalBytes = 1024L * 1024L * 1024L // 1 GiB
                    val t0 = SystemClock.elapsedRealtime()
                    val shaHex = writeSingleFileAtomic(
                        cr = cr,
                        parentDirUri = dirUri,
                        finalName = "large_1gb.bin",
                        sizeBytes = totalBytes,
                        bufferSizeBytes = bufferSizeKb * 1024,
                        progressTitle = "B1 (1 GB Write, ${bufferSizeKb}KB buf) Run $runNum/$iterations"
                    )
                    val elapsedMs = max(1L, SystemClock.elapsedRealtime() - t0)
                    val mbS = (totalBytes / (1024.0 * 1024.0)) / (elapsedMs / 1000.0)
                    val batEnd = SpikeStore.readBatteryTelemetry(context)
                    val heapDelta = max(0.0, usedHeapMb() - heapBefore)
                    SpikeStore.appendThroughputRun(
                        context,
                        ThroughputRunRecord(
                            isoTime = SpikeStore.nowIso(),
                            testId = "B1",
                            runNumber = runNum,
                            workloadLabel = "1 GB Single Write",
                            bufferSizeKb = bufferSizeKb,
                            isCharging = batStart.isCharging,
                            totalBytes = totalBytes,
                            fileCount = 1,
                            elapsedMs = elapsedMs,
                            mbPerSec = mbS,
                            filesPerSec = 1000.0 / elapsedMs,
                            sha256Verified = shaHex.isNotEmpty(),
                            batteryStartPct = batStart.percent,
                            batteryEndPct = batEnd.percent,
                            tempStartC = batStart.tempCelsius,
                            tempEndC = batEnd.tempCelsius,
                            heapDeltaMb = heapDelta
                        )
                    )
                }

                "B2" -> {
                    val dirUri = recreateSubdir(cr, treeUri, benchRootDocId, benchRootUri, "b2_2000_small")
                    val fileCount = 2000
                    val fileSize = 100 * 1024L // 100 KB
                    val totalBytes = fileCount * fileSize
                    val buf = ByteArray(bufferSizeKb * 1024) { (it % 251).toByte() }
                    val t0 = SystemClock.elapsedRealtime()
                    for (i in 1..fileCount) {
                        if (cancelRequested) break
                        if (i % 25 == 0 || i == 1) {
                            val elapsedSec = max(0.1, (SystemClock.elapsedRealtime() - t0) / 1000.0)
                            val curFilesS = i / elapsedSec
                            _progress.value = BenchProgress(
                                true,
                                "B2 (2,000 × 100 KB Write) Run $runNum/$iterations",
                                "File $i / $fileCount (${"%.1f".format(curFilesS)} files/s)",
                                i.toFloat() / fileCount
                            )
                        }
                        writeFileFromReusableBuffer(cr, dirUri, "small_%04d.bin".format(i), fileSize, buf)
                    }
                    if (!cancelRequested) {
                        val elapsedMs = max(1L, SystemClock.elapsedRealtime() - t0)
                        val mbS = (totalBytes / (1024.0 * 1024.0)) / (elapsedMs / 1000.0)
                        val filesS = fileCount / (elapsedMs / 1000.0)
                        val batEnd = SpikeStore.readBatteryTelemetry(context)
                        val heapDelta = max(0.0, usedHeapMb() - heapBefore)
                        SpikeStore.appendThroughputRun(
                            context,
                            ThroughputRunRecord(
                                isoTime = SpikeStore.nowIso(),
                                testId = "B2",
                                runNumber = runNum,
                                workloadLabel = "2,000 x 100 KB Write",
                                bufferSizeKb = bufferSizeKb,
                                isCharging = batStart.isCharging,
                                totalBytes = totalBytes,
                                fileCount = fileCount,
                                elapsedMs = elapsedMs,
                                mbPerSec = mbS,
                                filesPerSec = filesS,
                                sha256Verified = true,
                                batteryStartPct = batStart.percent,
                                batteryEndPct = batEnd.percent,
                                tempStartC = batStart.tempCelsius,
                                tempEndC = batEnd.tempCelsius,
                                heapDeltaMb = heapDelta
                            )
                        )
                    }
                }

                "B3" -> {
                    val dirUri = recreateSubdir(cr, treeUri, benchRootDocId, benchRootUri, "b3_mixed")
                    val buf = ByteArray(bufferSizeKb * 1024) { (it % 251).toByte() }
                    val mediumCount = 200
                    val smallCount = 2000
                    val totalFiles = mediumCount + smallCount
                    var bytesWritten = 0L
                    val t0 = SystemClock.elapsedRealtime()

                    // 200 files of 1..20 MB (deterministic step: 1 MB .. 20 MB cycling, average 10.5 MB = 2.1 GB)
                    for (i in 1..mediumCount) {
                        if (cancelRequested) break
                        val sizeMb = 1 + ((i - 1) % 20)
                        val fileBytes = sizeMb * 1024L * 1024L
                        _progress.value = BenchProgress(
                            true,
                            "B3 Mixed Write Run $runNum/$iterations",
                            "Medium file $i/$mediumCount ($sizeMb MB) · ${bytesWritten / (1024 * 1024)} MB written",
                            (i.toFloat() / totalFiles) * 0.8f
                        )
                        writeFileFromReusableBuffer(cr, dirUri, "med_%03d_%dmb.bin".format(i, sizeMb), fileBytes, buf)
                        bytesWritten += fileBytes
                    }

                    // 2,000 x 100 KB small files
                    val smallBytes = 100 * 1024L
                    for (i in 1..smallCount) {
                        if (cancelRequested) break
                        if (i % 50 == 0) {
                            _progress.value = BenchProgress(
                                true,
                                "B3 Mixed Write Run $runNum/$iterations",
                                "Small file $i/$smallCount · ${bytesWritten / (1024 * 1024)} MB written",
                                0.8f + (i.toFloat() / smallCount) * 0.2f
                            )
                        }
                        writeFileFromReusableBuffer(cr, dirUri, "sml_%04d.bin".format(i), smallBytes, buf)
                        bytesWritten += smallBytes
                    }

                    if (!cancelRequested) {
                        val elapsedMs = max(1L, SystemClock.elapsedRealtime() - t0)
                        val mbS = (bytesWritten / (1024.0 * 1024.0)) / (elapsedMs / 1000.0)
                        val filesS = totalFiles / (elapsedMs / 1000.0)
                        val batEnd = SpikeStore.readBatteryTelemetry(context)
                        val heapDelta = max(0.0, usedHeapMb() - heapBefore)
                        SpikeStore.appendThroughputRun(
                            context,
                            ThroughputRunRecord(
                                isoTime = SpikeStore.nowIso(),
                                testId = "B3",
                                runNumber = runNum,
                                workloadLabel = "Mixed (200x1-20MB + 2000x100KB)",
                                bufferSizeKb = bufferSizeKb,
                                isCharging = batStart.isCharging,
                                totalBytes = bytesWritten,
                                fileCount = totalFiles,
                                elapsedMs = elapsedMs,
                                mbPerSec = mbS,
                                filesPerSec = filesS,
                                sha256Verified = true,
                                batteryStartPct = batStart.percent,
                                batteryEndPct = batEnd.percent,
                                tempStartC = batStart.tempCelsius,
                                tempEndC = batEnd.tempCelsius,
                                heapDeltaMb = heapDelta
                            )
                        )
                    }
                }

                "B4-B1", "B4-B2", "B4-B3" -> {
                    val targetSubdir = when (testId) {
                        "B4-B1" -> "b1_1gb"
                        "B4-B2" -> "b2_2000_small"
                        else -> "b3_mixed"
                    }
                    val dirUri = findChildByName(cr, treeUri, benchRootDocId, targetSubdir)
                        ?: throw IllegalStateException("Run ${testId.removePrefix("B4-")} first so $targetSubdir exists on SSD")
                    val dirDocId = DocumentsContract.getDocumentId(dirUri)
                    val fileDocIds = listAllFileDocIds(cr, treeUri, dirDocId)
                    val buf = ByteArray(bufferSizeKb * 1024)
                    var totalBytesRead = 0L
                    val t0 = SystemClock.elapsedRealtime()

                    fileDocIds.forEachIndexed { idx, docId ->
                        if (cancelRequested) return@forEachIndexed
                        if (idx % 20 == 0 || fileDocIds.size == 1) {
                            _progress.value = BenchProgress(
                                true,
                                "$testId Read-Back + SHA-256 Run $runNum/$iterations",
                                "File ${idx + 1}/${fileDocIds.size} (${totalBytesRead / (1024 * 1024)} MB)",
                                (idx + 1).toFloat() / max(1, fileDocIds.size)
                            )
                        }
                        val fUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
                        val md = MessageDigest.getInstance("SHA-256")
                        cr.openInputStream(fUri)?.use { input ->
                            while (true) {
                                val r = input.read(buf)
                                if (r <= 0) break
                                md.update(buf, 0, r)
                                totalBytesRead += r
                            }
                        }
                        md.digest()
                    }

                    if (!cancelRequested) {
                        val elapsedMs = max(1L, SystemClock.elapsedRealtime() - t0)
                        val mbS = (totalBytesRead / (1024.0 * 1024.0)) / (elapsedMs / 1000.0)
                        val filesS = fileDocIds.size / (elapsedMs / 1000.0)
                        val batEnd = SpikeStore.readBatteryTelemetry(context)
                        val heapDelta = max(0.0, usedHeapMb() - heapBefore)
                        SpikeStore.appendThroughputRun(
                            context,
                            ThroughputRunRecord(
                                isoTime = SpikeStore.nowIso(),
                                testId = testId,
                                runNumber = runNum,
                                workloadLabel = "Read-back of ${testId.removePrefix("B4-")}",
                                bufferSizeKb = bufferSizeKb,
                                isCharging = batStart.isCharging,
                                totalBytes = totalBytesRead,
                                fileCount = fileDocIds.size,
                                elapsedMs = elapsedMs,
                                mbPerSec = mbS,
                                filesPerSec = filesS,
                                sha256Verified = true,
                                batteryStartPct = batStart.percent,
                                batteryEndPct = batEnd.percent,
                                tempStartC = batStart.tempCelsius,
                                tempEndC = batEnd.tempCelsius,
                                heapDeltaMb = heapDelta
                            )
                        )
                    }
                }
            }
        }
        _progress.value = BenchProgress(false, "Finished $testId", "", 1f)
    }

    /**
     * B5: 5,000-file listing benchmark (DocumentsContract batch cursor vs per-file DocumentFile).
     */
    suspend fun runListingBenchmark(
        context: Context,
        treeUri: Uri,
        targetCount: Int = 5000,
        iterations: Int = 3
    ) = withContext(Dispatchers.IO) {
        cancelRequested = false
        val cr = context.contentResolver
        val rootDocId = DocumentsContract.getTreeDocumentId(treeUri)
        val rootDocUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, rootDocId)

        val benchRootUri = getOrCreateDir(cr, treeUri, rootDocId, rootDocUri, ".spikelab_bench")
        val benchRootDocId = DocumentsContract.getDocumentId(benchRootUri)
        val listDirUri = getOrCreateDir(cr, treeUri, benchRootDocId, benchRootUri, "b5_list_5000")
        val listDirDocId = DocumentsContract.getDocumentId(listDirUri)

        // Check how many files already exist in b5_list_5000
        val existingIds = listAllFileDocIds(cr, treeUri, listDirDocId)
        if (existingIds.size < targetCount) {
            val tinyPayload = ByteArray(128) { 0x61 }
            for (i in (existingIds.size + 1)..targetCount) {
                if (cancelRequested) break
                if (i % 50 == 0 || i == 1) {
                    _progress.value = BenchProgress(
                        true,
                        "Preparing B5 5,000-file directory",
                        "Creating file $i / $targetCount",
                        i.toFloat() / targetCount
                    )
                }
                val u = DocumentsContract.createDocument(cr, listDirUri, "application/octet-stream", "item_%04d.dat".format(i))
                if (u != null) {
                    cr.openOutputStream(u)?.use { it.write(tinyPayload) }
                }
            }
        }

        if (cancelRequested) {
            _progress.value = BenchProgress(false, "Cancelled B5 preparation", "", 0f)
            return@withContext
        }

        // 1) 3 runs of DocumentsContract batch cursor query
        for (run in 1..iterations) {
            _progress.value = BenchProgress(true, "B5 Batch Cursor Query", "Run $run / $iterations", 0.5f)
            val heapBefore = usedHeapMb()
            val t0 = SystemClock.elapsedRealtime()
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, listDirDocId)
            var count = 0
            cr.query(
                childrenUri,
                arrayOf(
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                    DocumentsContract.Document.COLUMN_MIME_TYPE,
                    DocumentsContract.Document.COLUMN_SIZE,
                    DocumentsContract.Document.COLUMN_LAST_MODIFIED
                ),
                null,
                null,
                null
            )?.use { c ->
                while (c.moveToNext()) {
                    c.getString(0)
                    c.getString(1)
                    c.getString(2)
                    c.getLong(3)
                    c.getLong(4)
                    count++
                }
            }
            val elapsedMs = max(1L, SystemClock.elapsedRealtime() - t0)
            val msPer1k = (elapsedMs * 1000.0) / max(1, count)
            val heapDelta = max(0.0, usedHeapMb() - heapBefore)
            SpikeStore.appendListingRun(
                context,
                ListingRunRecord(
                    isoTime = SpikeStore.nowIso(),
                    method = "DocumentsContract Batch Cursor",
                    runNumber = run,
                    fileCount = count,
                    elapsedMs = elapsedMs,
                    msPer1kFiles = msPer1k,
                    heapDeltaMb = heapDelta
                )
            )
        }

        // 2) Per-file DocumentFile.listFiles() + metadata access
        for (run in 1..iterations) {
            if (cancelRequested) break
            _progress.value = BenchProgress(
                true,
                "B5 Per-File DocumentFile (Slow)",
                "Run $run / $iterations — iterating DocumentFile metadata…",
                0.8f
            )
            val heapBefore = usedHeapMb()
            val t0 = SystemClock.elapsedRealtime()
            val subTreeUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, listDirDocId)
            val docDir = DocumentFile.fromTreeUri(context, subTreeUri)
            val files = docDir?.listFiles() ?: emptyArray()
            var count = 0
            for (f in files) {
                if (cancelRequested) break
                // Each call below triggers an individual ContentResolver query in DocumentFile!
                f.name
                f.length()
                f.lastModified()
                count++
                if (count % 250 == 0) {
                    val soFarMs = SystemClock.elapsedRealtime() - t0
                    _progress.value = BenchProgress(
                        true,
                        "B5 Per-File DocumentFile Run $run/$iterations",
                        "Inspected $count / ${files.size} files (${soFarMs}ms elapsed)",
                        count.toFloat() / max(1, files.size)
                    )
                }
            }
            if (!cancelRequested) {
                val elapsedMs = max(1L, SystemClock.elapsedRealtime() - t0)
                val msPer1k = (elapsedMs * 1000.0) / max(1, count)
                val heapDelta = max(0.0, usedHeapMb() - heapBefore)
                SpikeStore.appendListingRun(
                    context,
                    ListingRunRecord(
                        isoTime = SpikeStore.nowIso(),
                        method = "Per-file DocumentFile",
                        runNumber = run,
                        fileCount = count,
                        elapsedMs = elapsedMs,
                        msPer1kFiles = msPer1k,
                        heapDeltaMb = heapDelta
                    )
                )
            }
        }

        _progress.value = BenchProgress(false, "Finished B5 Listing Benchmark", "", 1f)
    }

    private fun writeSingleFileAtomic(
        cr: ContentResolver,
        parentDirUri: Uri,
        finalName: String,
        sizeBytes: Long,
        bufferSizeBytes: Int,
        progressTitle: String
    ): String {
        val partName = "$finalName.part"
        val partUri = DocumentsContract.createDocument(cr, parentDirUri, "application/octet-stream", partName)
            ?: throw IllegalStateException("Failed to create $partName")

        val buf = ByteArray(bufferSizeBytes) { (it % 251).toByte() }
        val md = MessageDigest.getInstance("SHA-256")
        var remaining = sizeBytes
        val t0 = SystemClock.elapsedRealtime()
        var lastUiUpdate = t0

        cr.openOutputStream(partUri)?.use { out ->
            while (remaining > 0 && !cancelRequested) {
                val toWrite = minOf(remaining, buf.size.toLong()).toInt()
                out.write(buf, 0, toWrite)
                md.update(buf, 0, toWrite)
                remaining -= toWrite

                val now = SystemClock.elapsedRealtime()
                if (now - lastUiUpdate >= 250L) {
                    lastUiUpdate = now
                    val written = sizeBytes - remaining
                    val mbWritten = written / (1024.0 * 1024.0)
                    val elapsedSec = max(0.05, (now - t0) / 1000.0)
                    val mbS = mbWritten / elapsedSec
                    _progress.value = BenchProgress(
                        true,
                        progressTitle,
                        "${"%.0f".format(mbWritten)} / ${sizeBytes / (1024 * 1024)} MB (${"%.1f".format(mbS)} MB/s)",
                        written.toFloat() / sizeBytes.toFloat()
                    )
                }
            }
            out.flush()
        }

        if (cancelRequested) {
            try { DocumentsContract.deleteDocument(cr, partUri) } catch (_: Exception) {}
            return ""
        }

        DocumentsContract.renameDocument(cr, partUri, finalName)
        return md.digest().joinToString("") { "%02x".format(it) }
    }

    private fun writeFileFromReusableBuffer(
        cr: ContentResolver,
        parentDirUri: Uri,
        finalName: String,
        sizeBytes: Long,
        buf: ByteArray
    ) {
        val partUri = DocumentsContract.createDocument(cr, parentDirUri, "application/octet-stream", "$finalName.part")
            ?: throw IllegalStateException("Failed to create $finalName.part")
        val md = MessageDigest.getInstance("SHA-256")
        var remaining = sizeBytes
        cr.openOutputStream(partUri)?.use { out ->
            while (remaining > 0) {
                val n = minOf(remaining, buf.size.toLong()).toInt()
                out.write(buf, 0, n)
                md.update(buf, 0, n)
                remaining -= n
            }
            out.flush()
        }
        md.digest()
        DocumentsContract.renameDocument(cr, partUri, finalName)
    }

    private fun queryDisplayName(cr: ContentResolver, docUri: Uri): String {
        cr.query(
            docUri,
            arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME),
            null,
            null,
            null
        )?.use { c ->
            if (c.moveToFirst()) return c.getString(0) ?: ""
        }
        return ""
    }

    private fun findChildByName(
        cr: ContentResolver,
        treeUri: Uri,
        parentDocId: String,
        targetName: String
    ): Uri? {
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentDocId)
        cr.query(
            childrenUri,
            arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME
            ),
            null,
            null,
            null
        )?.use { c ->
            while (c.moveToNext()) {
                val id = c.getString(0)
                val name = c.getString(1)
                if (name == targetName) {
                    return DocumentsContract.buildDocumentUriUsingTree(treeUri, id)
                }
            }
        }
        return null
    }

    private fun getOrCreateDir(
        cr: ContentResolver,
        treeUri: Uri,
        parentDocId: String,
        parentDocUri: Uri,
        name: String
    ): Uri {
        return findChildByName(cr, treeUri, parentDocId, name)
            ?: DocumentsContract.createDocument(cr, parentDocUri, DocumentsContract.Document.MIME_TYPE_DIR, name)
            ?: throw IllegalStateException("Cannot create dir $name")
    }

    private fun recreateSubdir(
        cr: ContentResolver,
        treeUri: Uri,
        parentDocId: String,
        parentDocUri: Uri,
        name: String
    ): Uri {
        findChildByName(cr, treeUri, parentDocId, name)?.let {
            try { DocumentsContract.deleteDocument(cr, it) } catch (_: Exception) {}
        }
        return DocumentsContract.createDocument(cr, parentDocUri, DocumentsContract.Document.MIME_TYPE_DIR, name)
            ?: throw IllegalStateException("Cannot recreate dir $name")
    }

    private fun listAllFileDocIds(
        cr: ContentResolver,
        treeUri: Uri,
        parentDocId: String
    ): List<String> {
        val list = mutableListOf<String>()
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentDocId)
        cr.query(
            childrenUri,
            arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_MIME_TYPE
            ),
            null,
            null,
            null
        )?.use { c ->
            while (c.moveToNext()) {
                val id = c.getString(0)
                val mime = c.getString(1) ?: ""
                if (mime != DocumentsContract.Document.MIME_TYPE_DIR) {
                    list.add(id)
                }
            }
        }
        return list
    }

    private fun usedHeapMb(): Double {
        val rt = Runtime.getRuntime()
        return (rt.totalMemory() - rt.freeMemory()) / (1024.0 * 1024.0)
    }
}
