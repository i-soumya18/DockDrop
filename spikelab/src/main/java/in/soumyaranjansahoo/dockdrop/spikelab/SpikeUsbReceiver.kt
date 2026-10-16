package `in`.soumyaranjansahoo.dockdrop.spikelab

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.SystemClock
import androidx.core.app.NotificationCompat

class SpikeUsbReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        val eventTime = SystemClock.elapsedRealtime()
        SpikeStore.ensureLoaded(context)

        val device: UsbDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
        }

        val devSummary = if (device != null) {
            val classes = (0 until device.interfaceCount).joinToString(",") { idx ->
                val intf = device.getInterface(idx)
                "c=${intf.interfaceClass}/s=${intf.interfaceSubclass}/p=${intf.interfaceProtocol}"
            }
            "vid=0x${device.vendorId.toString(16)} pid=0x${device.productId.toString(16)} name=${device.productName ?: device.deviceName} if=[$classes]"
        } else {
            "no UsbDevice extra"
        }

        SpikeStore.appendTriggerEvent(
            context = context,
            eventType = "MANIFEST_RECEIVER:$action",
            launchMethod = "Manifest BroadcastReceiver",
            details = devSummary,
            eventRealtimeMs = eventTime
        )

        if (action == UsbManager.ACTION_USB_DEVICE_ATTACHED || action == Intent.ACTION_BOOT_COMPLETED) {
            postTriggerNotification(context, action, devSummary)
        }
    }

    private fun postTriggerNotification(context: Context, action: String, details: String) {
        try {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channelId = "spike_trigger_channel"
            val channel = NotificationChannel(
                channelId,
                "Spike Trigger Events",
                NotificationManager.IMPORTANCE_HIGH
            )
            nm.createNotificationChannel(channel)

            val launchIntent = Intent(context, SpikeLabActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra("launch_source", "TriggerNotification:$action")
            }
            val pi = PendingIntent.getActivity(
                context,
                1001,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notif = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.stat_notify_sync)
                .setContentTitle("SSD Trigger: ${action.substringAfterLast('.')}")
                .setContentText(details)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pi)
                .setAutoCancel(true)
                .build()

            nm.notify(2001, notif)
        } catch (_: Exception) {
        }
    }
}
