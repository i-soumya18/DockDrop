package `in`.soumyaranjansahoo.dockdrop.core.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import `in`.soumyaranjansahoo.dockdrop.MainActivity
import `in`.soumyaranjansahoo.dockdrop.R

class DropNotificationManager(private val context: Context) {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val progressChannel = NotificationChannel(
                DropNotificationMapper.CHANNEL_PROGRESS,
                context.getString(R.string.channel_progress_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows live backup progress"
                setShowBadge(false)
            }

            val resultsChannel = NotificationChannel(
                DropNotificationMapper.CHANNEL_RESULTS,
                context.getString(R.string.channel_results_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Shows completed and verified backup receipts"
            }

            val attentionChannel = NotificationChannel(
                DropNotificationMapper.CHANNEL_ATTENTION,
                context.getString(R.string.channel_attention_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Important notifications when SSD requires attention"
            }

            notificationManager.createNotificationChannels(
                listOf(progressChannel, resultsChannel, attentionChannel)
            )
        }
    }

    fun buildNotification(spec: NotificationSpec): Notification {
        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, spec.channelId)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentTitle(spec.title)
            .setContentText(spec.text)
            .setContentIntent(contentIntent)
            .setOngoing(spec.isOngoing)
            .setAutoCancel(!spec.isOngoing)

        if (spec.maxProgress > 0 || spec.isIndeterminate) {
            builder.setProgress(spec.maxProgress, spec.progress, spec.isIndeterminate)
        }

        spec.actions.forEachIndexed { index, actionSpec ->
            val pendingIntent = createActionPendingIntent(actionSpec.actionType, index)
            builder.addAction(0, actionSpec.label, pendingIntent)
        }

        return builder.build()
    }

    fun postNotification(spec: NotificationSpec) {
        val notification = buildNotification(spec)
        notificationManager.notify(spec.notificationId, notification)
    }

    fun cancelNotification(notificationId: Int) {
        notificationManager.cancel(notificationId)
    }

    private fun createActionPendingIntent(actionType: ActionType, requestCodeOffset: Int): PendingIntent {
        return when (actionType) {
            ActionType.BACK_UP_NOW -> {
                val intent = Intent(context, DropService::class.java).apply {
                    action = DropService.ACTION_START_DROP
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    PendingIntent.getForegroundService(
                        context,
                        100 + requestCodeOffset,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                } else {
                    PendingIntent.getService(
                        context,
                        100 + requestCodeOffset,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                }
            }

            ActionType.PAUSE -> {
                val intent = Intent(context, DropService::class.java).apply {
                    action = DropService.ACTION_PAUSE_DROP
                }
                PendingIntent.getService(
                    context,
                    200 + requestCodeOffset,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            }

            ActionType.RESUME -> {
                val intent = Intent(context, DropService::class.java).apply {
                    action = DropService.ACTION_RESUME_DROP
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    PendingIntent.getForegroundService(
                        context,
                        300 + requestCodeOffset,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                } else {
                    PendingIntent.getService(
                        context,
                        300 + requestCodeOffset,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                }
            }

            ActionType.STOP -> {
                val intent = Intent(context, DropService::class.java).apply {
                    action = DropService.ACTION_STOP_DROP
                }
                PendingIntent.getService(
                    context,
                    400 + requestCodeOffset,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            }

            ActionType.REVIEW, ActionType.FIX -> {
                val intent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra("action_type", actionType.name)
                }
                PendingIntent.getActivity(
                    context,
                    500 + requestCodeOffset,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            }
        }
    }
}
