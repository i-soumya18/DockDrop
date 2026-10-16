package `in`.soumyaranjansahoo.dockdrop.core.trigger

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbManager
import `in`.soumyaranjansahoo.dockdrop.DockDropApp

class UsbTriggerReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        val app = context.applicationContext as? DockDropApp ?: return
        val dockMonitor = app.container.dockMonitor

        when (action) {
            UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                dockMonitor.onUsbAttached()
            }

            UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                dockMonitor.onUsbDetached()
            }

            Intent.ACTION_BOOT_COMPLETED -> {
                dockMonitor.startMonitoring()
            }
        }
    }
}
