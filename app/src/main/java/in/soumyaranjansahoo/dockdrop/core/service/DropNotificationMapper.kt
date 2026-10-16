package `in`.soumyaranjansahoo.dockdrop.core.service

import `in`.soumyaranjansahoo.dockdrop.core.model.AttentionReason
import `in`.soumyaranjansahoo.dockdrop.core.model.DropState

data class NotificationSpec(
    val channelId: String,
    val notificationId: Int,
    val title: String,
    val text: String,
    val isOngoing: Boolean,
    val maxProgress: Int = 0,
    val progress: Int = 0,
    val isIndeterminate: Boolean = false,
    val actions: List<NotificationActionSpec> = emptyList()
)

data class NotificationActionSpec(
    val actionType: ActionType,
    val label: String
)

enum class ActionType {
    BACK_UP_NOW,
    PAUSE,
    RESUME,
    STOP,
    REVIEW,
    FIX
}

object DropNotificationMapper {
    const val CHANNEL_PROGRESS = "dockdrop_progress"
    const val CHANNEL_RESULTS = "dockdrop_results"
    const val CHANNEL_ATTENTION = "dockdrop_attention"

    const val NOTIF_ID_PROGRESS = 1001
    const val NOTIF_ID_RESULTS = 1002
    const val NOTIF_ID_ATTENTION = 1003
    const val NOTIF_ID_DETECTED = 1004

    fun map(state: DropState, defaultSetName: String? = null): NotificationSpec? {
        return when (state) {
            is DropState.NoDock -> null

            is DropState.DockDetected -> {
                val setName = defaultSetName ?: "files"
                NotificationSpec(
                    channelId = CHANNEL_ATTENTION,
                    notificationId = NOTIF_ID_DETECTED,
                    title = "SSD detected · ${state.dock.userLabel}",
                    text = "Tap to back up $setName",
                    isOngoing = false,
                    actions = listOf(
                        NotificationActionSpec(ActionType.BACK_UP_NOW, "Back up now")
                    )
                )
            }

            is DropState.AwaitingConfirm -> {
                NotificationSpec(
                    channelId = CHANNEL_ATTENTION,
                    notificationId = NOTIF_ID_DETECTED,
                    title = "SSD detected · ${state.dock.userLabel}",
                    text = "Tap to back up ${state.set.name}",
                    isOngoing = false,
                    actions = listOf(
                        NotificationActionSpec(ActionType.BACK_UP_NOW, "Back up now")
                    )
                )
            }

            is DropState.Preflight -> {
                NotificationSpec(
                    channelId = CHANNEL_PROGRESS,
                    notificationId = NOTIF_ID_PROGRESS,
                    title = "Checking SSD…",
                    text = "Testing write access",
                    isOngoing = true,
                    isIndeterminate = true
                )
            }

            is DropState.Dropping -> {
                val p = state.progress
                val pct = p.percentage
                val estMins = String.format("%.1f", p.estMinutesRemaining)
                NotificationSpec(
                    channelId = CHANNEL_PROGRESS,
                    notificationId = NOTIF_ID_PROGRESS,
                    title = "Backing up · $pct%",
                    text = "${p.currentFolder} · ${p.folderIndex} of ${p.totalFolders} · about $estMins min left",
                    isOngoing = true,
                    maxProgress = 100,
                    progress = pct,
                    actions = listOf(
                        NotificationActionSpec(ActionType.PAUSE, "Pause"),
                        NotificationActionSpec(ActionType.STOP, "Stop")
                    )
                )
            }

            is DropState.Verifying -> {
                NotificationSpec(
                    channelId = CHANNEL_PROGRESS,
                    notificationId = NOTIF_ID_PROGRESS,
                    title = "Verifying · ${state.folderIndex} of ${state.totalFolders}",
                    text = "Almost there",
                    isOngoing = true,
                    maxProgress = state.totalFiles.coerceAtLeast(1),
                    progress = state.verifiedFiles,
                    actions = listOf(
                        NotificationActionSpec(ActionType.STOP, "Stop")
                    )
                )
            }

            is DropState.Sealed -> {
                NotificationSpec(
                    channelId = CHANNEL_RESULTS,
                    notificationId = NOTIF_ID_RESULTS,
                    title = "Sealed · ${state.receipt.totalFilesVerified} files verified",
                    text = "All writes finished. Safe to unplug.",
                    isOngoing = false
                )
            }

            is DropState.Partial -> {
                NotificationSpec(
                    channelId = CHANNEL_RESULTS,
                    notificationId = NOTIF_ID_RESULTS,
                    title = "Finished with ${state.skippedFiles.size} skipped",
                    text = "Tap to review",
                    isOngoing = false,
                    actions = listOf(
                        NotificationActionSpec(ActionType.REVIEW, "Review")
                    )
                )
            }

            is DropState.Paused -> {
                NotificationSpec(
                    channelId = CHANNEL_ATTENTION,
                    notificationId = NOTIF_ID_ATTENTION,
                    title = "Paused",
                    text = state.reason.message,
                    isOngoing = false,
                    actions = listOf(
                        NotificationActionSpec(ActionType.RESUME, "Resume"),
                        NotificationActionSpec(ActionType.STOP, "Stop")
                    )
                )
            }

            is DropState.Interrupted -> {
                NotificationSpec(
                    channelId = CHANNEL_ATTENTION,
                    notificationId = NOTIF_ID_ATTENTION,
                    title = "Backup interrupted",
                    text = "Plug the SSD back in to resume",
                    isOngoing = false
                )
            }

            is DropState.Resumable -> {
                NotificationSpec(
                    channelId = CHANNEL_ATTENTION,
                    notificationId = NOTIF_ID_ATTENTION,
                    title = "Resume backup?",
                    text = "${state.progress.percentage}% done",
                    isOngoing = false,
                    actions = listOf(
                        NotificationActionSpec(ActionType.RESUME, "Resume")
                    )
                )
            }

            is DropState.Attention -> {
                when (val r = state.reason) {
                    is AttentionReason.NoSpace -> {
                        NotificationSpec(
                            channelId = CHANNEL_ATTENTION,
                            notificationId = NOTIF_ID_ATTENTION,
                            title = "Backup stopped",
                            text = "SSD is full. Needs ${formatBytes(r.neededBytes)}, ${formatBytes(r.freeBytes)} free.",
                            isOngoing = false
                        )
                    }
                    is AttentionReason.PermissionLost -> {
                        NotificationSpec(
                            channelId = CHANNEL_ATTENTION,
                            notificationId = NOTIF_ID_ATTENTION,
                            title = "Needs your attention",
                            text = "Access to ${r.folderLabel} was removed.",
                            isOngoing = false,
                            actions = listOf(
                                NotificationActionSpec(ActionType.FIX, "Fix")
                            )
                        )
                    }
                    is AttentionReason.MassDelete -> {
                        NotificationSpec(
                            channelId = CHANNEL_ATTENTION,
                            notificationId = NOTIF_ID_ATTENTION,
                            title = "Review needed",
                            text = "Mirror mode would remove ${r.count} files from the SSD.",
                            isOngoing = false,
                            actions = listOf(
                                NotificationActionSpec(ActionType.REVIEW, "Review")
                            )
                        )
                    }
                    is AttentionReason.ReadErrors -> {
                        NotificationSpec(
                            channelId = CHANNEL_ATTENTION,
                            notificationId = NOTIF_ID_ATTENTION,
                            title = "Read errors encountered",
                            text = "${r.count} files could not be read.",
                            isOngoing = false
                        )
                    }
                }
            }
        }
    }

    private fun formatBytes(bytes: Long): String {
        val gb = bytes.toDouble() / (1024 * 1024 * 1024)
        return if (gb >= 1.0) String.format("%.1f GB", gb) else "${bytes / (1024 * 1024)} MB"
    }
}
