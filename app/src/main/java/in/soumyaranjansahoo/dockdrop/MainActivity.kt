package `in`.soumyaranjansahoo.dockdrop

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.storage.StorageManager
import android.os.storage.StorageVolume
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import `in`.soumyaranjansahoo.dockdrop.core.model.DockInfo
import `in`.soumyaranjansahoo.dockdrop.ui.navigation.AppNavGraph
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockDropTheme

class MainActivity : ComponentActivity() {

    private lateinit var storageManager: StorageManager
    private var currentDock by mutableStateOf<DockInfo?>(null)

    private val volumeCallback = object : StorageManager.StorageVolumeCallback() {
        override fun onStateChanged(volume: StorageVolume) {
            checkStorageVolumes()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (DebugEntryPoint.launchSpikeLabIfDebug(this)) {
            return
        }

        enableEdgeToEdge()
        storageManager = getSystemService(Context.STORAGE_SERVICE) as StorageManager
        storageManager.registerStorageVolumeCallback(mainExecutor, volumeCallback)

        val app = application as DockDropApp
        val container = app.container

        setContent {
            DockDropTheme {
                AppNavGraph(
                    container = container,
                    currentDock = currentDock,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        checkStorageVolumes()
    }

    override fun onDestroy() {
        super.onDestroy()
        runCatching { storageManager.unregisterStorageVolumeCallback(volumeCallback) }
    }

    private fun checkStorageVolumes() {
        val app = application as? DockDropApp ?: return
        val volumes = storageManager.storageVolumes
        val removable = volumes.firstOrNull { it.isRemovable && it.state == Environment.MEDIA_MOUNTED }

        if (removable != null) {
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
                userLabel = removable.getDescription(this),
                totalBytes = totalBytes,
                freeBytes = freeBytes,
                filesystem = "exFAT",
                isMounted = true
            )
            currentDock = dock
            app.container.engine.updateDockState(dock)
        } else {
            currentDock = null
            app.container.engine.updateDockState(null)
        }
    }
}