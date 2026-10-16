package `in`.soumyaranjansahoo.dockdrop.spikelab

import android.app.ActivityManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.provider.DocumentsContract
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

data class HardwareInfo(
    val modelCode: String = Build.MODEL ?: "SM-A146B",
    val oneUiVersion: String = readSystemProperty("ro.build.version.oneui").let { raw ->
        if (raw.length >= 5) {
            val major = raw.dropLast(4).toIntOrNull() ?: 7
            val minor = raw.takeLast(4).take(2).toIntOrNull() ?: 0
            "$major.$minor (raw=$raw)"
        } else {
            raw.ifBlank { "One UI 7.0" }
        }
    },
    val androidRelease: String = "Android ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})",
    val buildNumber: String = Build.DISPLAY ?: Build.ID ?: "",
    val ssdBrandModel: String = "External USB-C SSD",
    val ssdCapacity: String = "500 GB / 1 TB",
    val filesystem: String = "exFAT",
    val cableAdapterHub: String = "Direct USB-C to USB-C cable",
    val activeTreeUri: String = ""
)

data class TriggerEventRecord(
    val id: Long = System.currentTimeMillis(),
    val isoTime: String,
    val scenarioTag: String, // T1..T8
    val eventType: String,   // USB_DEVICE_ATTACHED, STORAGE_VOLUME_MOUNTED, ACTION_MEDIA_MOUNTED, etc.
    val latencyMs: Long,     // -1 if no manual plug mark
    val processState: String,
    val launchMethod: String,
    val details: String,
    val systemDialogInterfered: String
)

data class PersistenceProbeRecord(
    val id: Long = System.currentTimeMillis(),
    val isoTime: String,
    val scenarioTag: String, // P1-Cycle1, P1-Cycle2, P1-Cycle3, P2-Reboot, P3-ForceStop, P4-HubPort, P5-LabelChange
    val uriString: String,
    val inPersistedList: Boolean,
    val listSucceeded: Boolean,
    val childCount: Int,
    val writeReadDeleteSucceeded: Boolean,
    val volumeUuid: String,
    val volumeDescription: String,
    val elapsedMs: Long,
    val notes: String
)

data class RenameTestRecord(
    val testId: String, // R1, R2, R3, R4a..R4g
    val description: String,
    val inputName: String,
    val accepted: Boolean,
    val actualDisplayName: String,
    val behaviorSummary: String,
    val elapsedMs: Long,
    val details: String
)

data class ThroughputRunRecord(
    val id: Long = System.currentTimeMillis(),
    val isoTime: String,
    val testId: String, // B1, B2, B3, B4-B1, B4-B2, B4-B3
    val runNumber: Int,
    val workloadLabel: String,
    val bufferSizeKb: Int,
    val isCharging: Boolean,
    val totalBytes: Long,
    val fileCount: Int,
    val elapsedMs: Long,
    val mbPerSec: Double,
    val filesPerSec: Double,
    val sha256Verified: Boolean,
    val batteryStartPct: Int,
    val batteryEndPct: Int,
    val tempStartC: Double,
    val tempEndC: Double,
    val heapDeltaMb: Double
)

data class ListingRunRecord(
    val id: Long = System.currentTimeMillis(),
    val isoTime: String,
    val testId: String = "B5",
    val method: String, // "DocumentsContract Batch Cursor" vs "Per-file DocumentFile"
    val runNumber: Int,
    val fileCount: Int,
    val elapsedMs: Long,
    val msPer1kFiles: Double,
    val heapDeltaMb: Double
)

data class GrantabilityRecord(
    val id: String, // G1..G7
    val label: String,
    val initialPathHint: String,
    val status: String, // UNTESTED, GRANTED, BLOCKED_BY_PICKER, ERROR
    val grantedUri: String = "",
    val canList: Boolean = false,
    val canReadSample: Boolean = false,
    val childCount: Int = 0,
    val notes: String = ""
)

data class FgsTestRecord(
    val id: Long = System.currentTimeMillis(),
    val isoTime: String,
    val testId: String, // F1, F2, F3, F4
    val serviceType: String, // connectedDevice / dataSync
    val usbPermGranted: String,
    val startedOk: Boolean,
    val durationSec: Long,
    val timeoutCallbackFired: Boolean,
    val checkpointSavedMs: Long,
    val batteryExempt: Boolean,
    val survivedScreenOff: String,
    val exitReason: String,
    val notes: String
)

data class SpikeLabState(
    val hardware: HardwareInfo = HardwareInfo(),
    val activeTriggerScenario: String = "T1",
    val lastPlugMarkRealtimeMs: Long = 0L,
    val systemDialogNote: String = "None observed",
    val triggerEvents: List<TriggerEventRecord> = emptyList(),
    val persistenceProbes: List<PersistenceProbeRecord> = emptyList(),
    val renameResults: List<RenameTestRecord> = emptyList(),
    val throughputRuns: List<ThroughputRunRecord> = emptyList(),
    val listingRuns: List<ListingRunRecord> = emptyList(),
    val grantability: List<GrantabilityRecord> = defaultGrantabilityList(),
    val fgsRecords: List<FgsTestRecord> = emptyList(),
    val statusBanner: String = "Ready. Grant SSD root/folder in G7 first to unlock P/R/B suites."
)

fun defaultGrantabilityList(): List<GrantabilityRecord> = listOf(
    GrantabilityRecord("G1", "DCIM (or DCIM/Camera)", "DCIM", "UNTESTED"),
    GrantabilityRecord("G2", "Pictures", "Pictures", "UNTESTED"),
    GrantabilityRecord("G3", "Documents", "Documents", "UNTESTED"),
    GrantabilityRecord("G4", "Download (root)", "Download", "UNTESTED"),
    GrantabilityRecord("G5", "Download sub-folder", "Download/DockDropTest", "UNTESTED"),
    GrantabilityRecord("G6", "WhatsApp Media", "Android/media/com.whatsapp/WhatsApp/Media", "UNTESTED"),
    GrantabilityRecord("G7", "SSD Root / DockDrop Vault", "SSD_ROOT", "UNTESTED")
)

fun readSystemProperty(key: String): String {
    return try {
        val proc = Runtime.getRuntime().exec(arrayOf("getprop", key))
        proc.inputStream.bufferedReader().use { it.readText().trim() }
    } catch (_: Exception) {
        ""
    }
}

data class BatteryTelemetry(
    val percent: Int,
    val tempCelsius: Double,
    val isCharging: Boolean
)

object SpikeStore {
    private const val STATE_FILE_NAME = "spikelab_state.json"
    private val _state = MutableStateFlow(SpikeLabState())
    val state: StateFlow<SpikeLabState> = _state.asStateFlow()

    @Volatile
    private var initialized = false
    private val lock = Any()

    fun ensureLoaded(context: Context) {
        if (initialized) return
        synchronized(lock) {
            if (initialized) return
            val file = File(context.applicationContext.filesDir, STATE_FILE_NAME)
            if (file.exists()) {
                try {
                    val json = JSONObject(file.readText())
                    _state.value = parseState(json)
                } catch (_: Exception) {
                    // Keep default state if corrupted
                }
            }
            initialized = true
        }
    }

    fun updateStatus(context: Context, message: String) {
        ensureLoaded(context)
        synchronized(lock) {
            _state.value = _state.value.copy(statusBanner = message)
            saveLocked(context)
        }
    }

    fun updateHardware(context: Context, transform: (HardwareInfo) -> HardwareInfo) {
        ensureLoaded(context)
        synchronized(lock) {
            val next = _state.value.copy(hardware = transform(_state.value.hardware))
            _state.value = next
            saveLocked(context)
        }
    }

    fun setTriggerScenario(context: Context, tag: String, dialogNote: String = _state.value.systemDialogNote) {
        ensureLoaded(context)
        synchronized(lock) {
            _state.value = _state.value.copy(
                activeTriggerScenario = tag,
                systemDialogNote = dialogNote
            )
            saveLocked(context)
        }
    }

    fun markPlugTimestamp(context: Context, elapsedRealtimeMs: Long) {
        ensureLoaded(context)
        synchronized(lock) {
            _state.value = _state.value.copy(
                lastPlugMarkRealtimeMs = elapsedRealtimeMs,
                statusBanner = "Marked t=0 plug-in timestamp for ${_state.value.activeTriggerScenario}"
            )
            saveLocked(context)
        }
    }

    fun appendTriggerEvent(
        context: Context,
        eventType: String,
        launchMethod: String,
        details: String,
        eventRealtimeMs: Long = android.os.SystemClock.elapsedRealtime()
    ) {
        ensureLoaded(context)
        synchronized(lock) {
            val cur = _state.value
            val latency = if (cur.lastPlugMarkRealtimeMs > 0L && eventRealtimeMs >= cur.lastPlugMarkRealtimeMs) {
                eventRealtimeMs - cur.lastPlugMarkRealtimeMs
            } else {
                -1L
            }
            val record = TriggerEventRecord(
                isoTime = nowIso(),
                scenarioTag = cur.activeTriggerScenario,
                eventType = eventType,
                latencyMs = latency,
                processState = currentProcessStateDescription(context),
                launchMethod = launchMethod,
                details = details,
                systemDialogInterfered = cur.systemDialogNote
            )
            _state.value = cur.copy(
                triggerEvents = listOf(record) + cur.triggerEvents,
                statusBanner = "Logged ${record.scenarioTag}: $eventType"
            )
            saveLocked(context)
        }
    }

    fun clearTriggerEvents(context: Context) {
        ensureLoaded(context)
        synchronized(lock) {
            _state.value = _state.value.copy(triggerEvents = emptyList())
            saveLocked(context)
        }
    }

    fun appendPersistenceProbe(context: Context, record: PersistenceProbeRecord) {
        ensureLoaded(context)
        synchronized(lock) {
            val cur = _state.value
            _state.value = cur.copy(
                persistenceProbes = listOf(record) + cur.persistenceProbes,
                statusBanner = "Probe ${record.scenarioTag}: persisted=${record.inPersistedList}, writeOk=${record.writeReadDeleteSucceeded}"
            )
            saveLocked(context)
        }
    }

    fun replaceRenameResults(context: Context, records: List<RenameTestRecord>) {
        ensureLoaded(context)
        synchronized(lock) {
            _state.value = _state.value.copy(
                renameResults = records,
                statusBanner = "Completed R1–R4 rename & filename semantics suite (${records.size} checks)"
            )
            saveLocked(context)
        }
    }

    fun appendThroughputRun(context: Context, record: ThroughputRunRecord) {
        ensureLoaded(context)
        synchronized(lock) {
            val cur = _state.value
            _state.value = cur.copy(
                throughputRuns = cur.throughputRuns + record,
                statusBanner = "${record.testId} #${record.runNumber}: ${"%.2f".format(record.mbPerSec)} MB/s (${"%.1f".format(record.filesPerSec)} files/s)"
            )
            saveLocked(context)
        }
    }

    fun clearThroughputRuns(context: Context) {
        ensureLoaded(context)
        synchronized(lock) {
            _state.value = _state.value.copy(throughputRuns = emptyList())
            saveLocked(context)
        }
    }

    fun appendListingRun(context: Context, record: ListingRunRecord) {
        ensureLoaded(context)
        synchronized(lock) {
            val cur = _state.value
            _state.value = cur.copy(
                listingRuns = cur.listingRuns + record,
                statusBanner = "B5 (${record.method}) #${record.runNumber}: ${record.fileCount} files in ${record.elapsedMs} ms"
            )
            saveLocked(context)
        }
    }

    fun updateGrantability(context: Context, updated: GrantabilityRecord) {
        ensureLoaded(context)
        synchronized(lock) {
            val cur = _state.value
            val list = cur.grantability.map { if (it.id == updated.id) updated else it }
            val newHw = if (updated.id == "G7" && updated.grantedUri.isNotBlank()) {
                cur.hardware.copy(activeTreeUri = updated.grantedUri)
            } else {
                cur.hardware
            }
            _state.value = cur.copy(
                grantability = list,
                hardware = newHw,
                statusBanner = "Updated ${updated.id} (${updated.label}): ${updated.status}"
            )
            saveLocked(context)
        }
    }

    fun appendFgsRecord(context: Context, record: FgsTestRecord) {
        ensureLoaded(context)
        synchronized(lock) {
            val cur = _state.value
            _state.value = cur.copy(
                fgsRecords = listOf(record) + cur.fgsRecords,
                statusBanner = "FGS ${record.testId} (${record.serviceType}): started=${record.startedOk}, dur=${record.durationSec}s"
            )
            saveLocked(context)
        }
    }

    fun readBatteryTelemetry(context: Context): BatteryTelemetry {
        val intent = context.applicationContext.registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )
        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val pct = if (level >= 0 && scale > 0) ((level * 100f) / scale).roundToInt() else -1
        val tempTenths = intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
        val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val plugged = intent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL ||
            plugged != 0
        return BatteryTelemetry(
            percent = pct,
            tempCelsius = tempTenths / 10.0,
            isCharging = charging
        )
    }

    fun currentProcessStateDescription(context: Context): String {
        return try {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val myPid = android.os.Process.myPid()
            val proc = am.runningAppProcesses?.firstOrNull { it.pid == myPid }
            when (proc?.importance) {
                ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND -> "FOREGROUND (100)"
                ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND_SERVICE -> "FOREGROUND_SERVICE (125)"
                ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE -> "VISIBLE (200)"
                ActivityManager.RunningAppProcessInfo.IMPORTANCE_SERVICE -> "SERVICE (300)"
                ActivityManager.RunningAppProcessInfo.IMPORTANCE_CACHED -> "CACHED/RECENTS (400)"
                null -> "COLD_START_OR_UNKNOWN"
                else -> "IMPORTANCE_${proc.importance}"
            }
        } catch (e: Exception) {
            "UNKNOWN (${e.javaClass.simpleName})"
        }
    }

    fun nowIso(): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date())
    }

    fun medianDouble(values: List<Double>): Double {
        if (values.isEmpty()) return 0.0
        val sorted = values.sorted()
        val mid = sorted.size / 2
        return if (sorted.size % 2 == 1) sorted[mid] else (sorted[mid - 1] + sorted[mid]) / 2.0
    }

    fun medianLong(values: List<Long>): Long {
        if (values.isEmpty()) return 0L
        val sorted = values.sorted()
        val mid = sorted.size / 2
        return if (sorted.size % 2 == 1) sorted[mid] else (sorted[mid - 1] + sorted[mid]) / 2L
    }

    fun buildMarkdownReport(context: Context): String {
        ensureLoaded(context)
        val s = _state.value
        val hw = s.hardware
        val bat = readBatteryTelemetry(context)
        val sb = StringBuilder()

        sb.appendLine("# Phase 0 Spike Results — Samsung Galaxy A14 (`${hw.modelCode}`)")
        sb.appendLine()
        sb.appendLine("> Generated by DockDrop S22 Spike Lab on ${nowIso()}")
        sb.appendLine()
        sb.appendLine("## 1. Device & Test Hardware Facts")
        sb.appendLine()
        sb.appendLine("| Field | Value |")
        sb.appendLine("|---|---|")
        sb.appendLine("| **Phone Model Code** | `${hw.modelCode}` |")
        sb.appendLine("| **Android Version** | `${hw.androidRelease}` |")
        sb.appendLine("| **One UI Version** | `${hw.oneUiVersion}` |")
        sb.appendLine("| **Build Number** | `${hw.buildNumber}` |")
        sb.appendLine("| **SSD Brand / Model** | ${hw.ssdBrandModel} |")
        sb.appendLine("| **SSD Capacity** | ${hw.ssdCapacity} |")
        sb.appendLine("| **Filesystem** | `${hw.filesystem}` |")
        sb.appendLine("| **Cable / Adapter / Hub** | ${hw.cableAdapterHub} |")
        sb.appendLine("| **Current Battery / Temp** | ${bat.percent}% (${bat.tempCelsius} °C, charging=${bat.isCharging}) |")
        sb.appendLine("| **Granted SSD Tree URI** | `${hw.activeTreeUri.ifBlank { "none" }}` |")
        sb.appendLine()

        // T: Trigger Matrix
        sb.appendLine("## 2. Trigger Matrix (`T1`–`T8`)")
        sb.appendLine()
        sb.appendLine("| Scenario | Timestamp | Event Fired | Latency (ms) | Process State | Launch Method | System Dialog / Notes |")
        sb.appendLine("|---|---|---|---|---|---|---|")
        if (s.triggerEvents.isEmpty()) {
            sb.appendLine("| *(no events recorded yet)* | — | — | — | — | — | — |")
        } else {
            s.triggerEvents.reversed().forEach { ev ->
                val lat = if (ev.latencyMs >= 0) "${ev.latencyMs} ms" else "n/a"
                sb.appendLine("| **${ev.scenarioTag}** | `${ev.isoTime}` | `${ev.eventType}` | $lat | `${ev.processState}` | ${ev.launchMethod} | ${ev.systemDialogInterfered} (${ev.details}) |")
            }
        }
        sb.appendLine()

        // P: SAF Permission Persistence
        sb.appendLine("## 3. SAF Permission Persistence (`P1`–`P5`)")
        sb.appendLine()
        sb.appendLine("| Scenario | Timestamp | In `persistedUriPermissions`? | List Folder OK? | Write+Read+Delete Probe OK? | Volume UUID / Label | Elapsed (ms) | Notes |")
        sb.appendLine("|---|---|---|---|---|---|---|---|")
        if (s.persistenceProbes.isEmpty()) {
            sb.appendLine("| *(no probes recorded yet)* | — | — | — | — | — | — | — |")
        } else {
            s.persistenceProbes.reversed().forEach { p ->
                sb.appendLine("| **${p.scenarioTag}** | `${p.isoTime}` | `${p.inPersistedList}` | `${p.listSucceeded}` (${p.childCount} items) | `${p.writeReadDeleteSucceeded}` | `${p.volumeUuid}` (${p.volumeDescription}) | ${p.elapsedMs} ms | ${p.notes} |")
            }
        }
        sb.appendLine()

        // R: Rename & Name Semantics
        sb.appendLine("## 4. Rename & Filename Semantics (`R1`–`R4`)")
        sb.appendLine()
        sb.appendLine("| Test ID | Scenario | Input Name | Accepted? | Actual Display Name | Provider Behavior | Elapsed (ms) | Details |")
        sb.appendLine("|---|---|---|---|---|---|---|---|")
        if (s.renameResults.isEmpty()) {
            sb.appendLine("| *(not run yet)* | — | — | — | — | — | — | — |")
        } else {
            s.renameResults.forEach { r ->
                sb.appendLine("| **${r.testId}** | ${r.description} | `${r.inputName.take(48)}` | `${r.accepted}` | `${r.actualDisplayName.take(48)}` | **${r.behaviorSummary}** | ${r.elapsedMs} ms | ${r.details} |")
            }
        }
        sb.appendLine()

        // B: Throughput Medians & Raw Runs
        sb.appendLine("## 5. Throughput & Listing Benchmarks (`B1`–`B5`)")
        sb.appendLine()
        sb.appendLine("### 5.1 Throughput Summary (Medians Across Runs)")
        sb.appendLine()
        sb.appendLine("| Group (Test · Buffer · Charging) | Runs | Median MB/s | Median files/s | Median Elapsed (s) | Temp Range (°C) | SHA-256 Verified |")
        sb.appendLine("|---|---|---|---|---|---|---|")
        val grouped = s.throughputRuns.groupBy { "${it.testId}|${it.workloadLabel}|${it.bufferSizeKb}KB|chg=${it.isCharging}" }
        if (grouped.isEmpty()) {
            sb.appendLine("| *(no throughput runs yet)* | 0 | — | — | — | — | — |")
        } else {
            grouped.forEach { (_, runs) ->
                val sample = runs.first()
                val medMbS = medianDouble(runs.map { it.mbPerSec })
                val medFilesS = medianDouble(runs.map { it.filesPerSec })
                val medElapsedS = medianLong(runs.map { it.elapsedMs }) / 1000.0
                val minTemp = runs.minOf { minOf(it.tempStartC, it.tempEndC) }
                val maxTemp = runs.maxOf { maxOf(it.tempStartC, it.tempEndC) }
                val allVerified = runs.all { it.sha256Verified }
                sb.appendLine(
                    "| **${sample.testId}** (${sample.workloadLabel}, ${sample.bufferSizeKb} KB, charging=${sample.isCharging}) | ${runs.size} | **${"%.2f".format(medMbS)} MB/s** | ${"%.1f".format(medFilesS)} /s | ${"%.2f".format(medElapsedS)} s | ${"%.1f".format(minTemp)}–${"%.1f".format(maxTemp)} °C | `$allVerified` |"
                )
            }
        }
        sb.appendLine()

        // Extrapolation for 20 GB First Drop
        val writeRuns = s.throughputRuns.filter { it.testId == "B3" }.ifEmpty { s.throughputRuns.filter { it.testId == "B1" } }
        val readRuns = s.throughputRuns.filter { it.testId.startsWith("B4") }
        val medWriteMbS = medianDouble(writeRuns.map { it.mbPerSec })
        val medReadMbS = medianDouble(readRuns.map { it.mbPerSec })
        sb.appendLine("### 5.2 20 GB First-Drop Extrapolation (ADR-002 Input)")
        sb.appendLine()
        if (medWriteMbS > 0.0) {
            val twentyGbMb = 20.0 * 1024.0
            val copyMinutes = (twentyGbMb / medWriteMbS) / 60.0
            val rereadMinutes = if (medReadMbS > 0.0) (twentyGbMb / medReadMbS) / 60.0 else 0.0
            val totalWithRereadMinutes = copyMinutes + rereadMinutes
            sb.appendLine("- **Median Write Throughput Used:** `${"%.2f".format(medWriteMbS)} MB/s`")
            sb.appendLine("- **Median Read-Back Throughput Used:** `${if (medReadMbS > 0.0) "%.2f MB/s".format(medReadMbS) else "pending B4"}`")
            sb.appendLine("- **Estimated 20 GB Copy + SHA-256 on Copy:** **${"%.1f".format(copyMinutes)} minutes**")
            if (medReadMbS > 0.0) {
                sb.appendLine("- **Estimated 20 GB Copy + SHA-256 + Full Destination Re-Read:** **${"%.1f".format(totalWithRereadMinutes)} minutes** (Threshold: ≤ 45.0 minutes → **${if (totalWithRereadMinutes <= 45.0) "PASSES full re-read default" else "EXCEEDS 45m; default to on-copy SHA-256"}**)")
            }
        } else {
            sb.appendLine("- *(Run B1/B3 and B4 to compute 20 GB extrapolation)*")
        }
        sb.appendLine()

        // B5 Listing Benchmark
        sb.appendLine("### 5.3 Directory Listing Benchmark (`B5`: 5,000 Files)")
        sb.appendLine()
        sb.appendLine("| Method | Run # | File Count | Elapsed (ms) | ms per 1,000 files | Heap Delta (MB) |")
        sb.appendLine("|---|---|---|---|---|---|")
        if (s.listingRuns.isEmpty()) {
            sb.appendLine("| *(not run yet)* | — | — | — | — | — |")
        } else {
            s.listingRuns.forEach { l ->
                sb.appendLine("| **${l.method}** | #${l.runNumber} | ${l.fileCount} | **${l.elapsedMs} ms** | ${"%.1f".format(l.msPer1kFiles)} ms | ${"%.2f".format(l.heapDeltaMb)} MB |")
            }
        }
        sb.appendLine()

        // G: Grantability Matrix
        sb.appendLine("## 6. Grantability Matrix (`G1`–`G7`)")
        sb.appendLine()
        sb.appendLine("| ID | Target Folder | Status | Can List? | Can Read Sample? | Persisted Tree URI | Notes |")
        sb.appendLine("|---|---|---|---|---|---|---|")
        s.grantability.forEach { g ->
            sb.appendLine("| **${g.id}** | ${g.label} (`${g.initialPathHint}`) | **${g.status}** | `${g.canList}` (${g.childCount}) | `${g.canReadSample}` | `${g.grantedUri.ifBlank { "—" }}` | ${g.notes} |")
        }
        sb.appendLine()

        // F: Foreground Service Lab
        sb.appendLine("## 7. Foreground Service Lab (`F1`–`F4`)")
        sb.appendLine()
        sb.appendLine("| Test ID | Timestamp | Service Type | USB Perm | Started OK? | Duration (s) | `onTimeout()` Fired? | Checkpoint Save (ms) | Battery Exempt? | Screen-Off Survival | Notes / Exit Reason |")
        sb.appendLine("|---|---|---|---|---|---|---|---|---|---|---|")
        if (s.fgsRecords.isEmpty()) {
            sb.appendLine("| *(not run yet)* | — | — | — | — | — | — | — | — | — | — |")
        } else {
            s.fgsRecords.reversed().forEach { f ->
                sb.appendLine("| **${f.testId}** | `${f.isoTime}` | `${f.serviceType}` | ${f.usbPermGranted} | `${f.startedOk}` | ${f.durationSec}s | `${f.timeoutCallbackFired}` | ${f.checkpointSavedMs} ms | `${f.batteryExempt}` | ${f.survivedScreenOff} | ${f.notes} (${f.exitReason}) |")
            }
        }

        return sb.toString()
    }

    fun buildThroughputCsv(context: Context): String {
        ensureLoaded(context)
        val sb = StringBuilder()
        sb.appendLine("test_id,run_number,iso_time,workload_label,buffer_kb,is_charging,total_bytes,file_count,elapsed_ms,mb_per_sec,files_per_sec,sha256_verified,battery_start_pct,battery_end_pct,temp_start_c,temp_end_c,heap_delta_mb")
        _state.value.throughputRuns.forEach { r ->
            sb.appendLine("${r.testId},${r.runNumber},${r.isoTime},\"${r.workloadLabel}\",${r.bufferSizeKb},${r.isCharging},${r.totalBytes},${r.fileCount},${r.elapsedMs},${"%.3f".format(Locale.US, r.mbPerSec)},${"%.2f".format(Locale.US, r.filesPerSec)},${r.sha256Verified},${r.batteryStartPct},${r.batteryEndPct},${r.tempStartC},${r.tempEndC},${"%.2f".format(Locale.US, r.heapDeltaMb)}")
        }
        return sb.toString()
    }

    fun exportAllReports(context: Context): String {
        ensureLoaded(context)
        val md = buildMarkdownReport(context)
        val csv = buildThroughputCsv(context)
        val outDir = context.getExternalFilesDir(null) ?: context.filesDir
        val mdFile = File(outDir, "PHASE0_RESULTS.md")
        val csvFile = File(outDir, "phase0_raw_runs.csv")
        mdFile.writeText(md)
        csvFile.writeText(csv)

        // Also attempt writing to SSD tree if granted
        var ssdSaved = false
        val treeUriStr = _state.value.hardware.activeTreeUri
        if (treeUriStr.isNotBlank()) {
            try {
                val treeUri = Uri.parse(treeUriStr)
                val docId = DocumentsContract.getTreeDocumentId(treeUri)
                val parentDocUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
                val cr = context.contentResolver
                val mdUri = DocumentsContract.createDocument(cr, parentDocUri, "text/markdown", "PHASE0_RESULTS.md")
                if (mdUri != null) {
                    cr.openOutputStream(mdUri)?.use { it.write(md.toByteArray()) }
                    ssdSaved = true
                }
                val csvUri = DocumentsContract.createDocument(cr, parentDocUri, "text/csv", "phase0_raw_runs.csv")
                if (csvUri != null) {
                    cr.openOutputStream(csvUri)?.use { it.write(csv.toByteArray()) }
                }
            } catch (_: Exception) {
                // Ignore SSD export error if unplugged
            }
        }

        // Copy Markdown to clipboard for convenience
        try {
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("PHASE0_RESULTS.md", md))
        } catch (_: Exception) {
        }

        val msg = "Exported to ${mdFile.absolutePath} (and clipboard" + (if (ssdSaved) " + SSD" else "") + ")"
        updateStatus(context, msg)
        return mdFile.absolutePath
    }

    private fun saveLocked(context: Context) {
        try {
            val file = File(context.applicationContext.filesDir, STATE_FILE_NAME)
            val json = serializeState(_state.value)
            file.writeText(json.toString(2))
        } catch (_: Exception) {
        }
    }

    private fun serializeState(s: SpikeLabState): JSONObject {
        val root = JSONObject()
        val hw = JSONObject().apply {
            put("modelCode", s.hardware.modelCode)
            put("oneUiVersion", s.hardware.oneUiVersion)
            put("androidRelease", s.hardware.androidRelease)
            put("buildNumber", s.hardware.buildNumber)
            put("ssdBrandModel", s.hardware.ssdBrandModel)
            put("ssdCapacity", s.hardware.ssdCapacity)
            put("filesystem", s.hardware.filesystem)
            put("cableAdapterHub", s.hardware.cableAdapterHub)
            put("activeTreeUri", s.hardware.activeTreeUri)
        }
        root.put("hardware", hw)
        root.put("activeTriggerScenario", s.activeTriggerScenario)
        root.put("lastPlugMarkRealtimeMs", s.lastPlugMarkRealtimeMs)
        root.put("systemDialogNote", s.systemDialogNote)

        val triggers = JSONArray()
        s.triggerEvents.forEach { e ->
            triggers.put(JSONObject().apply {
                put("id", e.id)
                put("isoTime", e.isoTime)
                put("scenarioTag", e.scenarioTag)
                put("eventType", e.eventType)
                put("latencyMs", e.latencyMs)
                put("processState", e.processState)
                put("launchMethod", e.launchMethod)
                put("details", e.details)
                put("systemDialogInterfered", e.systemDialogInterfered)
            })
        }
        root.put("triggerEvents", triggers)

        val probes = JSONArray()
        s.persistenceProbes.forEach { p ->
            probes.put(JSONObject().apply {
                put("id", p.id)
                put("isoTime", p.isoTime)
                put("scenarioTag", p.scenarioTag)
                put("uriString", p.uriString)
                put("inPersistedList", p.inPersistedList)
                put("listSucceeded", p.listSucceeded)
                put("childCount", p.childCount)
                put("writeReadDeleteSucceeded", p.writeReadDeleteSucceeded)
                put("volumeUuid", p.volumeUuid)
                put("volumeDescription", p.volumeDescription)
                put("elapsedMs", p.elapsedMs)
                put("notes", p.notes)
            })
        }
        root.put("persistenceProbes", probes)

        val renames = JSONArray()
        s.renameResults.forEach { r ->
            renames.put(JSONObject().apply {
                put("testId", r.testId)
                put("description", r.description)
                put("inputName", r.inputName)
                put("accepted", r.accepted)
                put("actualDisplayName", r.actualDisplayName)
                put("behaviorSummary", r.behaviorSummary)
                put("elapsedMs", r.elapsedMs)
                put("details", r.details)
            })
        }
        root.put("renameResults", renames)

        val tRuns = JSONArray()
        s.throughputRuns.forEach { r ->
            tRuns.put(JSONObject().apply {
                put("id", r.id)
                put("isoTime", r.isoTime)
                put("testId", r.testId)
                put("runNumber", r.runNumber)
                put("workloadLabel", r.workloadLabel)
                put("bufferSizeKb", r.bufferSizeKb)
                put("isCharging", r.isCharging)
                put("totalBytes", r.totalBytes)
                put("fileCount", r.fileCount)
                put("elapsedMs", r.elapsedMs)
                put("mbPerSec", r.mbPerSec)
                put("filesPerSec", r.filesPerSec)
                put("sha256Verified", r.sha256Verified)
                put("batteryStartPct", r.batteryStartPct)
                put("batteryEndPct", r.batteryEndPct)
                put("tempStartC", r.tempStartC)
                put("tempEndC", r.tempEndC)
                put("heapDeltaMb", r.heapDeltaMb)
            })
        }
        root.put("throughputRuns", tRuns)

        val lRuns = JSONArray()
        s.listingRuns.forEach { l ->
            lRuns.put(JSONObject().apply {
                put("id", l.id)
                put("isoTime", l.isoTime)
                put("testId", l.testId)
                put("method", l.method)
                put("runNumber", l.runNumber)
                put("fileCount", l.fileCount)
                put("elapsedMs", l.elapsedMs)
                put("msPer1kFiles", l.msPer1kFiles)
                put("heapDeltaMb", l.heapDeltaMb)
            })
        }
        root.put("listingRuns", lRuns)

        val grants = JSONArray()
        s.grantability.forEach { g ->
            grants.put(JSONObject().apply {
                put("id", g.id)
                put("label", g.label)
                put("initialPathHint", g.initialPathHint)
                put("status", g.status)
                put("grantedUri", g.grantedUri)
                put("canList", g.canList)
                put("canReadSample", g.canReadSample)
                put("childCount", g.childCount)
                put("notes", g.notes)
            })
        }
        root.put("grantability", grants)

        val fgs = JSONArray()
        s.fgsRecords.forEach { f ->
            fgs.put(JSONObject().apply {
                put("id", f.id)
                put("isoTime", f.isoTime)
                put("testId", f.testId)
                put("serviceType", f.serviceType)
                put("usbPermGranted", f.usbPermGranted)
                put("startedOk", f.startedOk)
                put("durationSec", f.durationSec)
                put("timeoutCallbackFired", f.timeoutCallbackFired)
                put("checkpointSavedMs", f.checkpointSavedMs)
                put("batteryExempt", f.batteryExempt)
                put("survivedScreenOff", f.survivedScreenOff)
                put("exitReason", f.exitReason)
                put("notes", f.notes)
            })
        }
        root.put("fgsRecords", fgs)
        return root
    }

    private fun parseState(root: JSONObject): SpikeLabState {
        val hwObj = root.optJSONObject("hardware")
        val hw = if (hwObj != null) {
            HardwareInfo(
                modelCode = hwObj.optString("modelCode", Build.MODEL ?: "SM-A146B"),
                oneUiVersion = hwObj.optString("oneUiVersion", HardwareInfo().oneUiVersion),
                androidRelease = hwObj.optString("androidRelease", HardwareInfo().androidRelease),
                buildNumber = hwObj.optString("buildNumber", Build.DISPLAY ?: ""),
                ssdBrandModel = hwObj.optString("ssdBrandModel", "External USB-C SSD"),
                ssdCapacity = hwObj.optString("ssdCapacity", "500 GB / 1 TB"),
                filesystem = hwObj.optString("filesystem", "exFAT"),
                cableAdapterHub = hwObj.optString("cableAdapterHub", "Direct USB-C to USB-C cable"),
                activeTreeUri = hwObj.optString("activeTreeUri", "")
            )
        } else {
            HardwareInfo()
        }

        val triggers = mutableListOf<TriggerEventRecord>()
        root.optJSONArray("triggerEvents")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                triggers.add(
                    TriggerEventRecord(
                        id = o.optLong("id"),
                        isoTime = o.optString("isoTime"),
                        scenarioTag = o.optString("scenarioTag", "T1"),
                        eventType = o.optString("eventType"),
                        latencyMs = o.optLong("latencyMs", -1L),
                        processState = o.optString("processState"),
                        launchMethod = o.optString("launchMethod"),
                        details = o.optString("details"),
                        systemDialogInterfered = o.optString("systemDialogInterfered")
                    )
                )
            }
        }

        val probes = mutableListOf<PersistenceProbeRecord>()
        root.optJSONArray("persistenceProbes")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                probes.add(
                    PersistenceProbeRecord(
                        id = o.optLong("id"),
                        isoTime = o.optString("isoTime"),
                        scenarioTag = o.optString("scenarioTag"),
                        uriString = o.optString("uriString"),
                        inPersistedList = o.optBoolean("inPersistedList"),
                        listSucceeded = o.optBoolean("listSucceeded"),
                        childCount = o.optInt("childCount"),
                        writeReadDeleteSucceeded = o.optBoolean("writeReadDeleteSucceeded"),
                        volumeUuid = o.optString("volumeUuid"),
                        volumeDescription = o.optString("volumeDescription"),
                        elapsedMs = o.optLong("elapsedMs"),
                        notes = o.optString("notes")
                    )
                )
            }
        }

        val renames = mutableListOf<RenameTestRecord>()
        root.optJSONArray("renameResults")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                renames.add(
                    RenameTestRecord(
                        testId = o.optString("testId"),
                        description = o.optString("description"),
                        inputName = o.optString("inputName"),
                        accepted = o.optBoolean("accepted"),
                        actualDisplayName = o.optString("actualDisplayName"),
                        behaviorSummary = o.optString("behaviorSummary"),
                        elapsedMs = o.optLong("elapsedMs"),
                        details = o.optString("details")
                    )
                )
            }
        }

        val tRuns = mutableListOf<ThroughputRunRecord>()
        root.optJSONArray("throughputRuns")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                tRuns.add(
                    ThroughputRunRecord(
                        id = o.optLong("id"),
                        isoTime = o.optString("isoTime"),
                        testId = o.optString("testId"),
                        runNumber = o.optInt("runNumber"),
                        workloadLabel = o.optString("workloadLabel"),
                        bufferSizeKb = o.optInt("bufferSizeKb"),
                        isCharging = o.optBoolean("isCharging"),
                        totalBytes = o.optLong("totalBytes"),
                        fileCount = o.optInt("fileCount"),
                        elapsedMs = o.optLong("elapsedMs"),
                        mbPerSec = o.optDouble("mbPerSec"),
                        filesPerSec = o.optDouble("filesPerSec"),
                        sha256Verified = o.optBoolean("sha256Verified"),
                        batteryStartPct = o.optInt("batteryStartPct"),
                        batteryEndPct = o.optInt("batteryEndPct"),
                        tempStartC = o.optDouble("tempStartC"),
                        tempEndC = o.optDouble("tempEndC"),
                        heapDeltaMb = o.optDouble("heapDeltaMb")
                    )
                )
            }
        }

        val lRuns = mutableListOf<ListingRunRecord>()
        root.optJSONArray("listingRuns")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                lRuns.add(
                    ListingRunRecord(
                        id = o.optLong("id"),
                        isoTime = o.optString("isoTime"),
                        testId = o.optString("testId", "B5"),
                        method = o.optString("method"),
                        runNumber = o.optInt("runNumber"),
                        fileCount = o.optInt("fileCount"),
                        elapsedMs = o.optLong("elapsedMs"),
                        msPer1kFiles = o.optDouble("msPer1kFiles"),
                        heapDeltaMb = o.optDouble("heapDeltaMb")
                    )
                )
            }
        }

        val grants = mutableListOf<GrantabilityRecord>()
        root.optJSONArray("grantability")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                grants.add(
                    GrantabilityRecord(
                        id = o.optString("id"),
                        label = o.optString("label"),
                        initialPathHint = o.optString("initialPathHint"),
                        status = o.optString("status", "UNTESTED"),
                        grantedUri = o.optString("grantedUri"),
                        canList = o.optBoolean("canList"),
                        canReadSample = o.optBoolean("canReadSample"),
                        childCount = o.optInt("childCount"),
                        notes = o.optString("notes")
                    )
                )
            }
        }

        val fgs = mutableListOf<FgsTestRecord>()
        root.optJSONArray("fgsRecords")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                fgs.add(
                    FgsTestRecord(
                        id = o.optLong("id"),
                        isoTime = o.optString("isoTime"),
                        testId = o.optString("testId"),
                        serviceType = o.optString("serviceType"),
                        usbPermGranted = o.optString("usbPermGranted"),
                        startedOk = o.optBoolean("startedOk"),
                        durationSec = o.optLong("durationSec"),
                        timeoutCallbackFired = o.optBoolean("timeoutCallbackFired"),
                        checkpointSavedMs = o.optLong("checkpointSavedMs"),
                        batteryExempt = o.optBoolean("batteryExempt"),
                        survivedScreenOff = o.optString("survivedScreenOff"),
                        exitReason = o.optString("exitReason"),
                        notes = o.optString("notes")
                    )
                )
            }
        }

        return SpikeLabState(
            hardware = hw,
            activeTriggerScenario = root.optString("activeTriggerScenario", "T1"),
            lastPlugMarkRealtimeMs = root.optLong("lastPlugMarkRealtimeMs", 0L),
            systemDialogNote = root.optString("systemDialogNote", "None observed"),
            triggerEvents = triggers,
            persistenceProbes = probes,
            renameResults = renames,
            throughputRuns = tRuns,
            listingRuns = lRuns,
            grantability = if (grants.size == 7) grants else defaultGrantabilityList(),
            fgsRecords = fgs
        )
    }
}
