package `in`.soumyaranjansahoo.dockdrop.core.restore

import `in`.soumyaranjansahoo.dockdrop.core.manifest.ManifestEntry
import `in`.soumyaranjansahoo.dockdrop.core.model.AttentionReason
import `in`.soumyaranjansahoo.dockdrop.core.model.SkippedFile

enum class ConflictPolicy(val displayName: String, val isDestructive: Boolean) {
    KEEP_BOTH("Keep both", isDestructive = false),
    SKIP("Skip", isDestructive = false),
    OVERWRITE("Overwrite", isDestructive = true)
}

enum class RestoreFilter(val label: String) {
    ALL("All"),
    PHOTOS("Photos"),
    VIDEOS("Videos"),
    DOCUMENTS("Documents"),
    DELETED("Deleted")
}

sealed class RestoreDestination {
    /** Restores each file to its original SourceFolder treeUri (mapped by folderLabel). */
    data class OriginalFolders(
        val folderLabelToUri: Map<String, String> // folderLabel -> treeUriString
    ) : RestoreDestination()

    /** Restores all selected files into a single user-picked folder on the phone. */
    data class ChosenFolder(
        val treeUriString: String,
        val displayLabel: String
    ) : RestoreDestination()

    /** Internal sandbox in context.cacheDir/restore_drill for the 10-file Restore Drill. */
    object DrillSandbox : RestoreDestination()
}

data class RestoreItem(
    val manifestEntry: ManifestEntry,
    val sourceFolderLabel: String,          // e.g. "Camera"
    val relativePathInFolder: String,       // e.g. "2026/IMG_001.jpg"
    val vaultRelativePath: String,          // e.g. "My phone/Camera/2026/IMG_001.jpg" or "_deleted/..."
    val hasConflict: Boolean,
    val resolvedTargetRelPath: String       // e.g. "2026/IMG_001 (restored).jpg" when KEEP_BOTH + conflict
)

data class PermissionGap(
    val folderLabel: String,
    val previousUriString: String?,
    val fileCount: Int,
    val totalBytes: Long
)

data class RestorePlan(
    val id: String,
    val setId: String,
    val setName: String,
    val deviceLabel: String,
    val vaultTreeUriString: String,
    val destination: RestoreDestination,
    val conflictPolicy: ConflictPolicy,
    val allCandidateItems: List<RestoreItem>,
    val itemsToTransfer: List<RestoreItem>, // Excludes SKIP conflicts
    val totalFilesSelected: Int,
    val totalFilesToCopy: Int,              // Guaranteed to equal actual processed count
    val totalBytesToCopy: Long,             // Guaranteed to equal actual processed bytes
    val conflictCount: Int,
    val permissionGaps: List<PermissionGap>,
    val phoneFreeBytes: Long,
    val hasEnoughSpace: Boolean,
    val isDrill: Boolean = false
) {
    val canExecute: Boolean
        get() = itemsToTransfer.isNotEmpty() && permissionGaps.isEmpty() && hasEnoughSpace
}

data class RestoreProgress(
    val currentFolder: String,
    val folderIndex: Int,
    val totalFolders: Int,
    val restoredFiles: Int,
    val totalFiles: Int,
    val restoredBytes: Long,
    val totalBytes: Long,
    val speedMbS: Double,
    val filesPerSec: Double,
    val estMinutesRemaining: Double,
    val currentFilePath: String
) {
    val percentage: Int
        get() = if (totalBytes > 0) ((restoredBytes * 100) / totalBytes).toInt().coerceIn(0, 100) else 0
}

data class RestoreReceipt(
    val id: String,
    val setId: String,
    val setName: String,
    val restoredCount: Int,
    val keptBothCount: Int,
    val skippedConflictCount: Int,
    val overwrittenCount: Int,
    val failedCount: Int,
    val totalBytesRestored: Long,
    val durationSeconds: Long,
    val verificationSummary: String = "SHA-256 against backup",
    val timestampsPreservedCount: Int,
    val isDrill: Boolean = false,
    val failedFiles: List<SkippedFile> = emptyList(),
    val timestampMs: Long = System.currentTimeMillis()
) {
    val isCompleteSuccess: Boolean get() = failedCount == 0
}

sealed class RestoreState {
    object Idle : RestoreState()
    object PreparingPlan : RestoreState()
    data class Restoring(val progress: RestoreProgress) : RestoreState()
    object UpdatingGallery : RestoreState()
    data class Completed(val receipt: RestoreReceipt) : RestoreState()
    data class Partial(val receipt: RestoreReceipt) : RestoreState()
    data class Attention(val reason: AttentionReason) : RestoreState()
}
