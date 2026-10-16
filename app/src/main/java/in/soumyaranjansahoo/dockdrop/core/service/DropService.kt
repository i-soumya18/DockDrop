package `in`.soumyaranjansahoo.dockdrop.core.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import `in`.soumyaranjansahoo.dockdrop.DockDropApp
import `in`.soumyaranjansahoo.dockdrop.core.model.DropProgress
import `in`.soumyaranjansahoo.dockdrop.core.model.DropState
import `in`.soumyaranjansahoo.dockdrop.core.model.PauseReason
import `in`.soumyaranjansahoo.dockdrop.core.restore.RestoreState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File

class DropService : Service() {

    companion object {
        const val ACTION_START_DROP = "in.soumyaranjansahoo.dockdrop.action.START_DROP"
        const val ACTION_RESUME_DROP = "in.soumyaranjansahoo.dockdrop.action.RESUME_DROP"
        const val ACTION_PAUSE_DROP = "in.soumyaranjansahoo.dockdrop.action.PAUSE_DROP"
        const val ACTION_STOP_DROP = "in.soumyaranjansahoo.dockdrop.action.STOP_DROP"
        const val ACTION_START_RESTORE = "in.soumyaranjansahoo.dockdrop.action.START_RESTORE"
        const val ACTION_STOP_RESTORE = "in.soumyaranjansahoo.dockdrop.action.STOP_RESTORE"
        const val ACTION_DEBUG_FORCE_TIMEOUT = "in.soumyaranjansahoo.dockdrop.action.DEBUG_FORCE_TIMEOUT"

        const val EXTRA_SET_ID = "extra_set_id"
        const val EXTRA_DEBUG_SHORT_TIMEOUT_MS = "extra_debug_short_timeout_ms"
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var stateObservationJob: Job? = null
    private var debugTimeoutJob: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var lastProgressEmissionTime = 0L

    private val app by lazy { applicationContext as DockDropApp }
    private val engine by lazy { app.container.engine }
    private val setRepository by lazy { app.container.setRepository }
    private val notificationManager by lazy { DropNotificationManager(this) }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: return START_NOT_STICKY

        when (action) {
            ACTION_START_DROP -> {
                val setId = intent.getStringExtra(EXTRA_SET_ID)
                val debugTimeoutMs = intent.getLongExtra(EXTRA_DEBUG_SHORT_TIMEOUT_MS, 0L)
                startForegroundWithProperType()
                acquireWakeLock()
                observeEngineState()

                serviceScope.launch(Dispatchers.IO) {
                    val sets = setRepository.allSets.first()
                    val targetSet = if (setId != null) sets.firstOrNull { it.id == setId } else sets.firstOrNull()
                    if (targetSet != null) {
                        val folders = setRepository.getSourceFoldersSync(targetSet.id)
                        engine.startDrop(targetSet, folders)
                    }
                }

                if (debugTimeoutMs > 0L) {
                    scheduleDebugTimeout(debugTimeoutMs, startId)
                }
            }

            ACTION_RESUME_DROP -> {
                val setId = intent.getStringExtra(EXTRA_SET_ID)
                startForegroundWithProperType()
                acquireWakeLock()
                observeEngineState()

                serviceScope.launch(Dispatchers.IO) {
                    val sets = setRepository.allSets.first()
                    val targetSet = if (setId != null) sets.firstOrNull { it.id == setId } else sets.firstOrNull()
                    if (targetSet != null) {
                        val folders = setRepository.getSourceFoldersSync(targetSet.id)
                        engine.resume(targetSet, folders)
                    }
                }
            }

            ACTION_PAUSE_DROP -> {
                engine.pause(PauseReason.USER_REQUESTED)
                releaseWakeLock()
            }

            ACTION_STOP_DROP -> {
                engine.stop()
                releaseWakeLock()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }

            ACTION_START_RESTORE -> {
                startForegroundWithProperType()
                acquireWakeLock()
                observeRestoreState()
            }

            ACTION_STOP_RESTORE -> {
                app.container.restoreEngine.stop()
                releaseWakeLock()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }

            ACTION_DEBUG_FORCE_TIMEOUT -> {
                handleTimeout(startId, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC, isSimulated = true)
            }
        }

        return START_NOT_STICKY
    }

    private fun startForegroundWithProperType() {
        val initialSpec = DropNotificationMapper.map(engine.state.value) ?: NotificationSpec(
            channelId = DropNotificationMapper.CHANNEL_PROGRESS,
            notificationId = DropNotificationMapper.NOTIF_ID_PROGRESS,
            title = "Checking SSD…",
            text = "Testing write access",
            isOngoing = true,
            isIndeterminate = true
        )
        val notification = notificationManager.buildNotification(initialSpec)

        val usbManager = getSystemService(Context.USB_SERVICE) as UsbManager
        val hasUsbPermission = try {
            usbManager.deviceList.values.any { usbManager.hasPermission(it) }
        } catch (_: Exception) {
            false
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (hasUsbPermission) {
                try {
                    startForeground(
                        DropNotificationMapper.NOTIF_ID_PROGRESS,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC or ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
                    )
                    return
                } catch (e: SecurityException) {
                    // Fall back strictly to dataSync per ADR-003
                }
            }
            startForeground(
                DropNotificationMapper.NOTIF_ID_PROGRESS,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(DropNotificationMapper.NOTIF_ID_PROGRESS, notification)
        }
    }

    private fun observeEngineState() {
        if (stateObservationJob?.isActive == true) return
        stateObservationJob = serviceScope.launch {
            engine.state.collect { state ->
                val spec = DropNotificationMapper.map(state)
                when (state) {
                    is DropState.Dropping, is DropState.Verifying, is DropState.Preflight -> {
                        val now = SystemClock.elapsedRealtime()
                        if (now - lastProgressEmissionTime >= 800L || state !is DropState.Dropping) {
                            lastProgressEmissionTime = now
                            if (spec != null) {
                                notificationManager.postNotification(spec)
                            }
                        }
                    }

                    is DropState.Sealed, is DropState.Partial -> {
                        releaseWakeLock()
                        stopForeground(STOP_FOREGROUND_REMOVE)
                        if (spec != null) {
                            notificationManager.postNotification(spec)
                        }
                        stopSelf()
                    }

                    is DropState.Paused, is DropState.Interrupted, is DropState.Resumable, is DropState.Attention -> {
                        releaseWakeLock()
                        stopForeground(STOP_FOREGROUND_REMOVE)
                        if (spec != null) {
                            notificationManager.postNotification(spec)
                        }
                        stopSelf()
                    }

                    is DropState.NoDock, is DropState.DockDetected, is DropState.AwaitingConfirm -> {
                        releaseWakeLock()
                        stopForeground(STOP_FOREGROUND_REMOVE)
                        stopSelf()
                    }
                }
            }
        }
    }

    private fun observeRestoreState() {
        val restoreEngine = app.container.restoreEngine
        stateObservationJob?.cancel()
        stateObservationJob = serviceScope.launch {
            restoreEngine.state.collect { state ->
                when (state) {
                    is RestoreState.Restoring -> {
                        val spec = NotificationSpec(
                            channelId = DropNotificationMapper.CHANNEL_PROGRESS,
                            notificationId = DropNotificationMapper.NOTIF_ID_PROGRESS,
                            title = "Restoring · ${state.progress.percentage}%",
                            text = "${state.progress.currentFolder} · ${state.progress.restoredFiles} of ${state.progress.totalFiles}",
                            isOngoing = true,
                            progress = state.progress.percentage,
                            maxProgress = 100
                        )
                        notificationManager.postNotification(spec)
                    }

                    is RestoreState.UpdatingGallery -> {
                        val spec = NotificationSpec(
                            channelId = DropNotificationMapper.CHANNEL_PROGRESS,
                            notificationId = DropNotificationMapper.NOTIF_ID_PROGRESS,
                            title = "Updating gallery…",
                            text = "Registering restored photos and videos",
                            isOngoing = true,
                            isIndeterminate = true
                        )
                        notificationManager.postNotification(spec)
                    }

                    is RestoreState.Completed, is RestoreState.Partial -> {
                        releaseWakeLock()
                        stopForeground(STOP_FOREGROUND_REMOVE)
                        stopSelf()
                    }

                    else -> {}
                }
            }
        }
    }

    private fun acquireWakeLock() {
        if (wakeLock?.isHeld == true) return
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "DockDrop:DropWakeLock").apply {
            setReferenceCounted(false)
            acquire(6 * 60 * 60 * 1000L) // 6 hours max
        }
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (_: Exception) {}
    }

    override fun onTimeout(startId: Int, fgsType: Int) {
        handleTimeout(startId, fgsType, isSimulated = false)
    }

    private fun handleTimeout(startId: Int, fgsType: Int, isSimulated: Boolean) {
        val t0 = SystemClock.elapsedRealtime()
        debugTimeoutJob?.cancel()
        stateObservationJob?.cancel()

        // 1. Immediately pause the engine with SYSTEM_TIMEOUT (<50ms)
        engine.pause(PauseReason.SYSTEM_TIMEOUT)
        releaseWakeLock()

        // 2. Persist timeout checkpoint for verification and inspection
        val checkpointFile = File(applicationContext.filesDir, "drop_timeout_checkpoint.json")
        try {
            val json = JSONObject().apply {
                put("timestampMs", System.currentTimeMillis())
                put("fgsType", fgsType)
                put("isSimulated", isSimulated)
                put("startId", startId)
            }
            checkpointFile.writeText(json.toString(2))
        } catch (_: Exception) {}

        val saveLatencyMs = (SystemClock.elapsedRealtime() - t0).coerceAtLeast(1L)

        // 3. Post "Resume backup?" notification per SCREENS.md §3 Resumable
        val resumableSpec = DropNotificationMapper.map(engine.state.value) ?: NotificationSpec(
            channelId = DropNotificationMapper.CHANNEL_ATTENTION,
            notificationId = DropNotificationMapper.NOTIF_ID_ATTENTION,
            title = "Resume backup?",
            text = "Tap to continue",
            isOngoing = false,
            actions = listOf(
                NotificationActionSpec(ActionType.RESUME, "Resume")
            )
        )
        notificationManager.postNotification(resumableSpec)

        // 4. Cleanly stop foreground and service (<500ms)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf(startId)
    }

    private fun scheduleDebugTimeout(delayMs: Long, startId: Int) {
        debugTimeoutJob?.cancel()
        debugTimeoutJob = serviceScope.launch {
            delay(delayMs)
            handleTimeout(startId, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC, isSimulated = true)
        }
    }

    override fun onDestroy() {
        releaseWakeLock()
        debugTimeoutJob?.cancel()
        stateObservationJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }
}
