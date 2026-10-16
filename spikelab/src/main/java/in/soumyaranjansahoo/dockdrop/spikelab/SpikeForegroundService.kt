package `in`.soumyaranjansahoo.dockdrop.spikelab

import android.app.ActivityManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import android.provider.DocumentsContract
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File

data class FgsLiveStatus(
    val isRunning: Boolean = false,
    val activeTestId: String = "",
    val serviceType: String = "",
    val elapsedSec: Long = 0L,
    val screenOffSec: Long = 0L,
    val heartbeats: Int = 0,
    val bytesPaced: Long = 0L,
    val lastMessage: String = "Idle"
)

class SpikeForegroundService : Service() {

    companion object {
        const val ACTION_START_CONNECTED_DEVICE = "in.soumyaranjansahoo.dockdrop.spikelab.START_CONNECTED_DEVICE"
        const val ACTION_START_DATA_SYNC = "in.soumyaranjansahoo.dockdrop.spikelab.START_DATA_SYNC"
        const val ACTION_SIMULATE_TIMEOUT = "in.soumyaranjansahoo.dockdrop.spikelab.SIMULATE_TIMEOUT"
        const val ACTION_STOP = "in.soumyaranjansahoo.dockdrop.spikelab.STOP"

        const val EXTRA_TEST_ID = "extra_test_id"
        const val EXTRA_USB_PERM_STATE = "extra_usb_perm_state"
        const val EXTRA_SHORT_TIMEOUT_SEC = "extra_short_timeout_sec"

        private const val NOTIF_ID = 3001
        private const val TIMEOUT_NOTIF_ID = 3002
        private const val CHANNEL_ID = "spike_fgs_channel"

        private val _liveStatus = MutableStateFlow(FgsLiveStatus())
        val liveStatus: StateFlow<FgsLiveStatus> = _liveStatus.asStateFlow()

        fun inspectPreviousSessionHeartbeat(context: Context) {
            val hbFile = File(context.applicationContext.filesDir, "fgs_active_heartbeat.json")
            if (!hbFile.exists()) return
            try {
                val obj = JSONObject(hbFile.readText())
                val wasCleanStop = obj.optBoolean("cleanStop", false)
                if (!wasCleanStop) {
                    val exitInfo = queryLatestExitReason(context)
                    val record = FgsTestRecord(
                        isoTime = obj.optString("lastIsoTime", SpikeStore.nowIso()),
                        testId = obj.optString("testId", "F4"),
                        serviceType = obj.optString("serviceType", "dataSync"),
                        usbPermGranted = obj.optString("usbPerm", "n/a"),
                        startedOk = true,
                        durationSec = obj.optLong("elapsedSec", 0L),
                        timeoutCallbackFired = false,
                        checkpointSavedMs = 0L,
                        batteryExempt = obj.optBoolean("batteryExempt", false),
                        survivedScreenOff = "Terminated while running (screenOffSec=${obj.optLong("screenOffSec", 0L)}s)",
                        exitReason = exitInfo,
                        notes = "Recovered from heartbeat file after process death"
                    )
                    SpikeStore.appendFgsRecord(context, record)
                }
                hbFile.delete()
            } catch (_: Exception) {
            }
        }

        fun queryLatestExitReason(context: Context): String {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                try {
                    val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                    val reasons = am.getHistoricalProcessExitReasons(context.packageName, 0, 3)
                    val latest = reasons.firstOrNull()
                    if (latest != null) {
                        "reason=${latest.reason} status=${latest.status} imp=${latest.importance} desc=${latest.description ?: "none"}"
                    } else {
                        "no HistoricalProcessExitReasons"
                    }
                } catch (e: Exception) {
                    "error:${e.message}"
                }
            } else {
                "API<30"
            }
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var runnerJob: Job? = null
    private var startRealtimeMs: Long = 0L
    private var currentTestId: String = "F2"
    private var currentServiceType: String = "dataSync"
    private var currentUsbPerm: String = "n/a"
    private var screenOffAccumSec: Long = 0L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        ensureChannel()
        val action = intent?.action ?: return START_NOT_STICKY

        when (action) {
            ACTION_START_CONNECTED_DEVICE -> {
                currentTestId = intent.getStringExtra(EXTRA_TEST_ID) ?: "F1"
                currentServiceType = "connectedDevice"
                currentUsbPerm = intent.getStringExtra(EXTRA_USB_PERM_STATE) ?: "unknown"
                startExperiment(
                    fgsType = ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE,
                    forcedTimeoutSec = 15L
                )
            }

            ACTION_START_DATA_SYNC -> {
                currentTestId = intent.getStringExtra(EXTRA_TEST_ID) ?: "F2"
                currentServiceType = "dataSync"
                currentUsbPerm = intent.getStringExtra(EXTRA_USB_PERM_STATE) ?: "n/a"
                val forcedTimeoutSec = intent.getLongExtra(EXTRA_SHORT_TIMEOUT_SEC, 0L)
                startExperiment(
                    fgsType = ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
                    forcedTimeoutSec = forcedTimeoutSec
                )
            }

            ACTION_SIMULATE_TIMEOUT -> {
                handleServiceTimeout(startId, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC, isSimulated = true)
            }

            ACTION_STOP -> {
                stopExperimentCleanly("User tapped Stop")
            }
        }
        return START_NOT_STICKY
    }

    private fun startExperiment(fgsType: Int, forcedTimeoutSec: Long) {
        runnerJob?.cancel()
        startRealtimeMs = SystemClock.elapsedRealtime()
        screenOffAccumSec = 0L
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        val batteryExempt = pm.isIgnoringBatteryOptimizations(packageName)

        try {
            val notif = buildOngoingNotification("Running $currentTestId ($currentServiceType) — 0s")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(NOTIF_ID, notif, fgsType)
            } else {
                startForeground(NOTIF_ID, notif)
            }
        } catch (se: Exception) {
            val errMsg = "${se.javaClass.simpleName}: ${se.message}"
            _liveStatus.value = FgsLiveStatus(
                isRunning = false,
                activeTestId = currentTestId,
                serviceType = currentServiceType,
                lastMessage = "FAILED to start: $errMsg"
            )
            SpikeStore.appendFgsRecord(
                this,
                FgsTestRecord(
                    isoTime = SpikeStore.nowIso(),
                    testId = currentTestId,
                    serviceType = currentServiceType,
                    usbPermGranted = currentUsbPerm,
                    startedOk = false,
                    durationSec = 0L,
                    timeoutCallbackFired = false,
                    checkpointSavedMs = 0L,
                    batteryExempt = batteryExempt,
                    survivedScreenOff = "n/a (failed at startForeground)",
                    exitReason = errMsg,
                    notes = "startForeground threw exception"
                )
            )
            stopSelf()
            return
        }

        _liveStatus.value = FgsLiveStatus(
            isRunning = true,
            activeTestId = currentTestId,
            serviceType = currentServiceType,
            lastMessage = "Started $currentServiceType FGS (exempt=$batteryExempt)"
        )

        if (currentTestId == "F1") {
            SpikeStore.appendFgsRecord(
                this,
                FgsTestRecord(
                    isoTime = SpikeStore.nowIso(),
                    testId = "F1",
                    serviceType = currentServiceType,
                    usbPermGranted = currentUsbPerm,
                    startedOk = true,
                    durationSec = 0L,
                    timeoutCallbackFired = false,
                    checkpointSavedMs = 0L,
                    batteryExempt = batteryExempt,
                    survivedScreenOff = "n/a (F1 prerequisite check)",
                    exitReason = "startForeground(CONNECTED_DEVICE) succeeded",
                    notes = "Verified connectedDevice FGS start with USB perm state: $currentUsbPerm"
                )
            )
        }

        runnerJob = scope.launch {
            var heartbeats = 0
            var bytesPaced = 0L
            while (isActive) {
                delay(2_000L)
                val elapsedSec = (SystemClock.elapsedRealtime() - startRealtimeMs) / 1000L
                val isScreenInteractive = pm.isInteractive
                if (!isScreenInteractive) {
                    screenOffAccumSec += 2L
                }
                heartbeats++

                // If SSD tree is granted, perform a tiny paced SAF read/write heartbeat so USB stays active during F4
                val treeStr = SpikeStore.state.value.hardware.activeTreeUri
                if (treeStr.isNotBlank() && heartbeats % 5 == 0) {
                    bytesPaced += performPacedSafHeartbeat(Uri.parse(treeStr), elapsedSec)
                }

                writeHeartbeatFile(
                    elapsedSec = elapsedSec,
                    screenOffSec = screenOffAccumSec,
                    batteryExempt = batteryExempt,
                    cleanStop = false
                )

                val statusText = "$currentTestId ($currentServiceType) · ${elapsedSec}s elapsed · screenOff=${screenOffAccumSec}s"
                _liveStatus.value = FgsLiveStatus(
                    isRunning = true,
                    activeTestId = currentTestId,
                    serviceType = currentServiceType,
                    elapsedSec = elapsedSec,
                    screenOffSec = screenOffAccumSec,
                    heartbeats = heartbeats,
                    bytesPaced = bytesPaced,
                    lastMessage = statusText
                )

                val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                nm.notify(NOTIF_ID, buildOngoingNotification(statusText))

                if (forcedTimeoutSec > 0L && elapsedSec >= forcedTimeoutSec) {
                    if (currentTestId == "F3") {
                        handleServiceTimeout(0, fgsType, isSimulated = true)
                    } else {
                        stopExperimentCleanly("Completed ${elapsedSec}s target run")
                    }
                    break
                }
            }
        }
    }

    private fun performPacedSafHeartbeat(treeUri: Uri, elapsedSec: Long): Long {
        return try {
            val docId = DocumentsContract.getTreeDocumentId(treeUri)
            val parentDocUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
            val cr = contentResolver
            val payload = "FGS heartbeat at ${elapsedSec}s (${SpikeStore.nowIso()})\n".toByteArray()
            val tempUri = DocumentsContract.createDocument(cr, parentDocUri, "application/octet-stream", ".fgs_heartbeat.tmp")
            if (tempUri != null) {
                cr.openOutputStream(tempUri)?.use { out ->
                    out.write(payload)
                    out.flush()
                }
                DocumentsContract.deleteDocument(cr, tempUri)
                payload.size.toLong()
            } else {
                0L
            }
        } catch (_: Exception) {
            0L
        }
    }

    override fun onTimeout(startId: Int, fgsType: Int) {
        handleServiceTimeout(startId, fgsType, isSimulated = false)
    }

    private fun handleServiceTimeout(startId: Int, fgsType: Int, isSimulated: Boolean) {
        val t0 = SystemClock.elapsedRealtime()
        runnerJob?.cancel()
        val elapsedSec = if (startRealtimeMs > 0L) (t0 - startRealtimeMs) / 1000L else 0L
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        val batteryExempt = pm.isIgnoringBatteryOptimizations(packageName)

        // Persist checkpoint state immediately
        val checkpointFile = File(applicationContext.filesDir, "fgs_timeout_checkpoint.json")
        val cpJson = JSONObject().apply {
            put("savedAtIso", SpikeStore.nowIso())
            put("testId", currentTestId)
            put("serviceType", currentServiceType)
            put("elapsedSecAtTimeout", elapsedSec)
            put("screenOffSec", screenOffAccumSec)
            put("isSimulated", isSimulated)
            put("fgsTypeInt", fgsType)
        }
        checkpointFile.writeText(cpJson.toString(2))
        writeHeartbeatFile(elapsedSec, screenOffAccumSec, batteryExempt, cleanStop = true)

        val saveLatencyMs = (SystemClock.elapsedRealtime() - t0).coerceAtLeast(1L)

        // Post "Continue backup" notification per AGENTS.md A2 completion bar
        postContinueNotification(elapsedSec, saveLatencyMs, isSimulated)

        SpikeStore.appendFgsRecord(
            this,
            FgsTestRecord(
                isoTime = SpikeStore.nowIso(),
                testId = if (currentTestId == "F2") "F3" else currentTestId,
                serviceType = currentServiceType,
                usbPermGranted = currentUsbPerm,
                startedOk = true,
                durationSec = elapsedSec,
                timeoutCallbackFired = true,
                checkpointSavedMs = saveLatencyMs,
                batteryExempt = batteryExempt,
                survivedScreenOff = "screenOff=${screenOffAccumSec}s",
                exitReason = if (isSimulated) "Simulated forced short timeout" else "OS Service.onTimeout(startId=$startId)",
                notes = "Checkpoint persisted in ${saveLatencyMs}ms; Continue notification posted; stopped cleanly"
            )
        )

        _liveStatus.value = FgsLiveStatus(
            isRunning = false,
            activeTestId = currentTestId,
            serviceType = currentServiceType,
            elapsedSec = elapsedSec,
            screenOffSec = screenOffAccumSec,
            lastMessage = "onTimeout() handled in ${saveLatencyMs}ms; stopped cleanly."
        )

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun stopExperimentCleanly(reason: String) {
        runnerJob?.cancel()
        val elapsedSec = if (startRealtimeMs > 0L) (SystemClock.elapsedRealtime() - startRealtimeMs) / 1000L else 0L
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        val batteryExempt = pm.isIgnoringBatteryOptimizations(packageName)

        writeHeartbeatFile(elapsedSec, screenOffAccumSec, batteryExempt, cleanStop = true)

        SpikeStore.appendFgsRecord(
            this,
            FgsTestRecord(
                isoTime = SpikeStore.nowIso(),
                testId = currentTestId,
                serviceType = currentServiceType,
                usbPermGranted = currentUsbPerm,
                startedOk = true,
                durationSec = elapsedSec,
                timeoutCallbackFired = false,
                checkpointSavedMs = 0L,
                batteryExempt = batteryExempt,
                survivedScreenOff = "Survived (screenOff=${screenOffAccumSec}s / total=${elapsedSec}s)",
                exitReason = "Clean stop",
                notes = reason
            )
        )

        _liveStatus.value = FgsLiveStatus(
            isRunning = false,
            activeTestId = currentTestId,
            serviceType = currentServiceType,
            elapsedSec = elapsedSec,
            screenOffSec = screenOffAccumSec,
            lastMessage = "Stopped cleanly after ${elapsedSec}s (screenOff=${screenOffAccumSec}s)"
        )

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun writeHeartbeatFile(
        elapsedSec: Long,
        screenOffSec: Long,
        batteryExempt: Boolean,
        cleanStop: Boolean
    ) {
        try {
            val hbFile = File(applicationContext.filesDir, "fgs_active_heartbeat.json")
            if (cleanStop) {
                hbFile.delete()
                return
            }
            val json = JSONObject().apply {
                put("lastIsoTime", SpikeStore.nowIso())
                put("testId", currentTestId)
                put("serviceType", currentServiceType)
                put("usbPerm", currentUsbPerm)
                put("elapsedSec", elapsedSec)
                put("screenOffSec", screenOffSec)
                put("batteryExempt", batteryExempt)
                put("cleanStop", false)
            }
            hbFile.writeText(json.toString())
        } catch (_: Exception) {
        }
    }

    private fun ensureChannel() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val ch = NotificationChannel(
            CHANNEL_ID,
            "Spike Foreground Service Lab",
            NotificationManager.IMPORTANCE_LOW
        )
        nm.createNotificationChannel(ch)
    }

    private fun buildOngoingNotification(text: String): Notification {
        val launchIntent = Intent(this, SpikeLabActivity::class.java)
        val pi = PendingIntent.getActivity(
            this,
            3010,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentTitle("DockDrop FGS Lab ($currentServiceType)")
            .setContentText(text)
            .setOngoing(true)
            .setContentIntent(pi)
            .build()
    }

    private fun postContinueNotification(elapsedSec: Long, saveLatencyMs: Long, isSimulated: Boolean) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val launchIntent = Intent(this, SpikeLabActivity::class.java).apply {
            putExtra("launch_source", "FgsTimeoutContinue")
        }
        val pi = PendingIntent.getActivity(
            this,
            3011,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notif = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle("Backup paused by system timeout — Tap to Continue")
            .setContentText("Saved checkpoint in ${saveLatencyMs}ms after ${elapsedSec}s (simulated=$isSimulated)")
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()
        nm.notify(TIMEOUT_NOTIF_ID, notif)
    }

    override fun onDestroy() {
        runnerJob?.cancel()
        scope.cancel()
        super.onDestroy()
    }
}
