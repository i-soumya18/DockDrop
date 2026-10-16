package `in`.soumyaranjansahoo.dockdrop

import android.content.Intent
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import `in`.soumyaranjansahoo.dockdrop.core.model.DropState
import `in`.soumyaranjansahoo.dockdrop.core.service.DropService
import `in`.soumyaranjansahoo.dockdrop.ui.navigation.AppNavGraph
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockDropTheme
import `in`.soumyaranjansahoo.dockdrop.ui.theme.HapticFeedbackController

class MainActivity : ComponentActivity() {

    companion object {
        const val EXTRA_OPEN_PLUGIN_SHEET = "extra_open_plugin_sheet"
    }

    private var showPlugInSheet by mutableStateOf(false)
    private lateinit var haptics: HapticFeedbackController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (DebugEntryPoint.launchSpikeLabIfDebug(this)) {
            return
        }

        enableEdgeToEdge()
        haptics = HapticFeedbackController(this)

        val app = application as DockDropApp
        val container = app.container
        container.dockMonitor.startMonitoring()

        handleIntent(intent)

        setContent {
            DockDropTheme {
                val currentDock by container.dockMonitor.currentDock.collectAsState()
                val isUsbConnecting by container.dockMonitor.isUsbConnecting.collectAsState()
                val dropState by container.engine.state.collectAsState()

                // Haptics reaction to state machine changes (DESIGN.md §6)
                LaunchedEffect(currentDock) {
                    if (currentDock != null) {
                        haptics.onSsdDetected()
                    }
                }

                LaunchedEffect(dropState) {
                    when (dropState) {
                        is DropState.Sealed -> haptics.onSealed()
                        is DropState.Attention -> haptics.onAttention()
                        else -> {}
                    }
                }

                AppNavGraph(
                    container = container,
                    currentDock = currentDock,
                    isUsbConnecting = isUsbConnecting,
                    showPlugInSheet = showPlugInSheet,
                    onDismissPlugInSheet = { showPlugInSheet = false },
                    onStartDropInService = { setId ->
                        val serviceIntent = Intent(this@MainActivity, DropService::class.java).apply {
                            action = DropService.ACTION_START_DROP
                            putExtra(DropService.EXTRA_SET_ID, setId)
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            startForegroundService(serviceIntent)
                        } else {
                            startService(serviceIntent)
                        }
                        haptics.onDropStarted()
                    },
                    onResumeDropInService = { setId ->
                        val serviceIntent = Intent(this@MainActivity, DropService::class.java).apply {
                            action = DropService.ACTION_RESUME_DROP
                            putExtra(DropService.EXTRA_SET_ID, setId)
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            startForegroundService(serviceIntent)
                        } else {
                            startService(serviceIntent)
                        }
                    },
                    onPauseDropInService = {
                        val serviceIntent = Intent(this@MainActivity, DropService::class.java).apply {
                            action = DropService.ACTION_PAUSE_DROP
                        }
                        startService(serviceIntent)
                    },
                    onStopDropInService = {
                        val serviceIntent = Intent(this@MainActivity, DropService::class.java).apply {
                            action = DropService.ACTION_STOP_DROP
                        }
                        startService(serviceIntent)
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val app = application as? DockDropApp ?: return
        if (intent == null) return

        if (intent.action == UsbManager.ACTION_USB_DEVICE_ATTACHED) {
            app.container.dockMonitor.onUsbAttached()
            showPlugInSheet = true
        } else if (intent.getBooleanExtra(EXTRA_OPEN_PLUGIN_SHEET, false)) {
            showPlugInSheet = true
        }
    }

    override fun onDestroy() {
        val app = application as? DockDropApp
        app?.container?.dockMonitor?.stopMonitoring()
        super.onDestroy()
    }
}