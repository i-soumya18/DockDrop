package `in`.soumyaranjansahoo.dockdrop.core.model

data class SkippedFile(
    val relativePath: String,
    val sourceFolderLabel: String,
    val sizeBytes: Long,
    val reason: String
)

data class Receipt(
    val id: String,
    val setId: String,
    val setName: String,
    val isSealed: Boolean,
    val totalFilesVerified: Int,
    val totalBytesVerified: Long,
    val durationSeconds: Long,
    val averageSpeedMbS: Double,
    val verificationLevel: VerificationLevel,
    val generationNumber: Long,
    val manifestSha256: String,
    val keptDeletedFilesCount: Int = 0,
    val skippedFiles: List<SkippedFile> = emptyList(),
    val timestampMs: Long = System.currentTimeMillis()
) {
    val isPartial: Boolean get() = !isSealed || skippedFiles.isNotEmpty()
}
