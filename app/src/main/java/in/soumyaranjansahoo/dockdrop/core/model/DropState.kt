package `in`.soumyaranjansahoo.dockdrop.core.model

data class DockInfo(
    val volumeUuid: String,
    val userLabel: String,
    val totalBytes: Long,
    val freeBytes: Long,
    val filesystem: String,
    val isMounted: Boolean
)

data class DropProgress(
    val setId: String,
    val currentFolder: String,
    val folderIndex: Int,
    val totalFolders: Int,
    val copiedFiles: Int,
    val totalFiles: Int,
    val copiedBytes: Long,
    val totalBytes: Long,
    val speedMbS: Double,
    val filesPerSec: Double,
    val estMinutesRemaining: Double,
    val currentFilePath: String
) {
    val percentage: Int get() = if (totalBytes > 0) ((copiedBytes * 100) / totalBytes).toInt().coerceIn(0, 100) else 0
}

enum class PauseReason(val message: String) {
    USER_REQUESTED("You paused the backup"),
    BATTERY_LOW("Battery dropped below threshold"),
    CHARGING_REQUIRED("Waiting for phone to be charging"),
    THERMAL_THROTTLE("Device is cooling down"),
    SYSTEM_TIMEOUT("Paused by system time limit — tap Resume to continue")
}

sealed class AttentionReason(val title: String, val message: String) {
    data class NoSpace(val neededBytes: Long, val freeBytes: Long) :
        AttentionReason("SSD is full", "Needs ${formatBytes(neededBytes)}, ${formatBytes(freeBytes)} free.")

    data class PermissionLost(val folderLabel: String, val folderUri: String) :
        AttentionReason("Access lost to $folderLabel", "Grant access again to continue.")

    data class MassDelete(val count: Int, val paths: List<String> = emptyList()) :
        AttentionReason("Mirror mode wants to remove $count files", "Nothing has been removed.")

    data class ReadErrors(val count: Int) :
        AttentionReason("Read errors encountered", "$count files could not be read.")

    companion object {
        private fun formatBytes(bytes: Long): String {
            val gb = bytes.toDouble() / (1024 * 1024 * 1024)
            return if (gb >= 1.0) String.format("%.1f GB", gb) else "${bytes / (1024 * 1024)} MB"
        }
    }
}

sealed class DropState {
    object NoDock : DropState()
    data class DockDetected(val dock: DockInfo) : DropState()
    data class AwaitingConfirm(
        val set: SetDefinition,
        val dock: DockInfo,
        val plannedFiles: Int,
        val plannedBytes: Long,
        val estMinutes: Double
    ) : DropState()
    object Preflight : DropState()
    data class Dropping(val progress: DropProgress) : DropState()
    data class Verifying(
        val currentFolder: String,
        val folderIndex: Int,
        val totalFolders: Int,
        val verifiedFiles: Int,
        val totalFiles: Int
    ) : DropState()
    data class Sealed(val receipt: Receipt) : DropState()
    data class Partial(val receipt: Receipt, val skippedFiles: List<SkippedFile>) : DropState()
    data class Paused(val reason: PauseReason, val progress: DropProgress) : DropState()
    data class Interrupted(val progress: DropProgress) : DropState()
    data class Resumable(val progress: DropProgress) : DropState()
    data class Attention(val reason: AttentionReason) : DropState()
}
