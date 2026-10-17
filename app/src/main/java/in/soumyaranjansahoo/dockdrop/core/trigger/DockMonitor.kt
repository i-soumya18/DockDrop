package `in`.soumyaranjansahoo.dockdrop.core.trigger

import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.storage.StorageManager
import android.os.storage.StorageVolume
import `in`.soumyaranjansahoo.dockdrop.core.engine.DropEngine
import `in`.soumyaranjansahoo.dockdrop.core.model.DockInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DockMonitor(
    private val context: Context,
    private val engine: DropEngine,
    private val scope: CoroutineScope
) {
    private val storageManager = context.getSystemService(Context.STORAGE_SERVICE) as StorageManager

    private val _currentDock = MutableStateFlow<DockInfo?>(null)
    val currentDock: StateFlow<DockInfo?> = _currentDock.asStateFlow()

    private val _isUsbConnecting = MutableStateFlow(false)
    val isUsbConnecting: StateFlow<Boolean> = _isUsbConnecting.asStateFlow()

    private var mountTimeoutJob: Job? = null
    private var isMonitoring = false

    private val volumeCallback: StorageManager.StorageVolumeCallback? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            object : StorageManager.StorageVolumeCallback() {
                override fun onStateChanged(volume: StorageVolume) {
                    checkStorageVolumes()
                }
            }
        } else {
            null
        }

    fun startMonitoring() {
        if (isMonitoring) return
        isMonitoring = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && volumeCallback != null) {
            try {
                storageManager.registerStorageVolumeCallback(context.mainExecutor, volumeCallback)
            } catch (_: Exception) {}
        }
        checkStorageVolumes()
    }

    fun stopMonitoring() {
        if (!isMonitoring) return
        isMonitoring = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && volumeCallback != null) {
            try {
                storageManager.unregisterStorageVolumeCallback(volumeCallback)
            } catch (_: Exception) {}
        }
    }

    fun onUsbAttached() {
        _isUsbConnecting.value = true
        mountTimeoutJob?.cancel()
        mountTimeoutJob = scope.launch(Dispatchers.Main) {
            // Stage A: Wait up to 25 seconds for vold exFAT mount to complete per ADR-001
            delay(25_000L)
            if (_currentDock.value == null) {
                _isUsbConnecting.value = false
            }
        }
        checkStorageVolumes()
    }

    fun onUsbDetached() {
        mountTimeoutJob?.cancel()
        _isUsbConnecting.value = false
        _currentDock.value = null
        engine.updateDockState(null)
    }

    fun checkStorageVolumes() {
        val volumes = storageManager.storageVolumes
        val removable = volumes.firstOrNull { it.isRemovable && it.state == Environment.MEDIA_MOUNTED }

        if (removable != null) {
            mountTimeoutJob?.cancel()
            _isUsbConnecting.value = false

            var freeBytes = 0L
            var totalBytes = 0L
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                removable.directory?.let { dir ->
                    if (dir.exists()) {
                        freeBytes = dir.freeSpace
                        totalBytes = dir.totalSpace
                    }
                }
            }
            if (totalBytes <= 0L) {
                totalBytes = 128L * 1024 * 1024 * 1024
                freeBytes = 100L * 1024 * 1024 * 1024
            }

            val dock = DockInfo(
                volumeUuid = removable.uuid ?: "unknown",
                userLabel = removable.getDescription(context),
                totalBytes = totalBytes,
                freeBytes = freeBytes,
                filesystem = "exFAT",
                isMounted = true
            )
            _currentDock.value = dock
            engine.updateDockState(dock)
        } else {
            if (!_isUsbConnecting.value) {
                _currentDock.value = null
                engine.updateDockState(null)
            }
        }
    }
}
