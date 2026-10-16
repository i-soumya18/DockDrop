package `in`.soumyaranjansahoo.dockdrop.core.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import `in`.soumyaranjansahoo.dockdrop.core.model.DeletionPolicy
import `in`.soumyaranjansahoo.dockdrop.core.model.Receipt
import `in`.soumyaranjansahoo.dockdrop.core.model.SetDefinition
import `in`.soumyaranjansahoo.dockdrop.core.model.SkippedFile
import `in`.soumyaranjansahoo.dockdrop.core.model.SourceFolder
import `in`.soumyaranjansahoo.dockdrop.core.model.VerificationLevel
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Entity(tableName = "sets")
data class SetEntity(
    @PrimaryKey val id: String,
    val name: String,
    val dockVolumeUuid: String,
    val dockUserLabel: String,
    val destinationVaultUriString: String,
    val deletionPolicy: String,
    val retentionDays: Int,
    val minBatteryPct: Int,
    val isEncrypted: Boolean,
    val verificationLevel: String,
    val createdAtMs: Long,
    val lastSealedMs: Long?,
    val lastSealedFiles: Int,
    val lastSealedBytes: Long,
    val lastStatus: String?
) {
    fun toDomain(): SetDefinition = SetDefinition(
        id = id,
        name = name,
        dockVolumeUuid = dockVolumeUuid,
        dockUserLabel = dockUserLabel,
        destinationVaultUriString = destinationVaultUriString,
        deletionPolicy = runCatching { DeletionPolicy.valueOf(deletionPolicy) }.getOrDefault(DeletionPolicy.KEEP),
        retentionDays = retentionDays,
        minBatteryPct = minBatteryPct,
        isEncrypted = isEncrypted,
        verificationLevel = runCatching { VerificationLevel.valueOf(verificationLevel) }.getOrDefault(VerificationLevel.FULL_REREAD),
        createdAtMs = createdAtMs,
        lastSealedMs = lastSealedMs,
        lastSealedFiles = lastSealedFiles,
        lastSealedBytes = lastSealedBytes,
        lastStatus = lastStatus
    )

    companion object {
        fun fromDomain(d: SetDefinition): SetEntity = SetEntity(
            id = d.id,
            name = d.name,
            dockVolumeUuid = d.dockVolumeUuid,
            dockUserLabel = d.dockUserLabel,
            destinationVaultUriString = d.destinationVaultUriString,
            deletionPolicy = d.deletionPolicy.name,
            retentionDays = d.retentionDays,
            minBatteryPct = d.minBatteryPct,
            isEncrypted = d.isEncrypted,
            verificationLevel = d.verificationLevel.name,
            createdAtMs = d.createdAtMs,
            lastSealedMs = d.lastSealedMs,
            lastSealedFiles = d.lastSealedFiles,
            lastSealedBytes = d.lastSealedBytes,
            lastStatus = d.lastStatus
        )
    }
}

@Entity(
    tableName = "source_folders",
    indices = [Index(value = ["setId"])]
)
data class SourceFolderEntity(
    @PrimaryKey val id: String,
    val setId: String,
    val label: String,
    val treeUriString: String,
    val relativeSubpath: String,
    val estimatedBytes: Long,
    val fileCount: Int
) {
    fun toDomain(): SourceFolder = SourceFolder(
        id = id,
        setId = setId,
        label = label,
        treeUriString = treeUriString,
        relativeSubpath = relativeSubpath,
        estimatedBytes = estimatedBytes,
        fileCount = fileCount
    )

    companion object {
        fun fromDomain(d: SourceFolder): SourceFolderEntity = SourceFolderEntity(
            id = d.id,
            setId = d.setId,
            label = d.label,
            treeUriString = d.treeUriString,
            relativeSubpath = d.relativeSubpath,
            estimatedBytes = d.estimatedBytes,
            fileCount = d.fileCount
        )
    }
}

enum class JournalStatus {
    PLANNED,
    COPYING,
    COPIED,
    VERIFIED,
    FAILED
}

@Entity(
    tableName = "drop_journal",
    primaryKeys = ["setId", "relativePath"]
)
data class JournalEntry(
    val setId: String,
    val relativePath: String,
    val sourceFolderLabel: String,
    val sourceUriString: String,
    val sizeBytes: Long,
    val sourceModifiedMs: Long,
    val expectedSha256: String? = null,
    val destinationSha256: String? = null,
    val status: String = JournalStatus.PLANNED.name,
    val errorReason: String? = null,
    val updatedAtMs: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "receipts",
    indices = [Index(value = ["setId"])]
)
data class ReceiptEntity(
    @PrimaryKey val id: String,
    val setId: String,
    val setName: String,
    val isSealed: Boolean,
    val totalFilesVerified: Int,
    val totalBytesVerified: Long,
    val durationSeconds: Long,
    val averageSpeedMbS: Double,
    val verificationLevel: String,
    val generationNumber: Long,
    val manifestSha256: String,
    val keptDeletedFilesCount: Int,
    val skippedFilesJson: String,
    val timestampMs: Long
) {
    fun toDomain(): Receipt {
        val skipped = runCatching {
            Json.decodeFromString<List<SkippedFile>>(skippedFilesJson)
        }.getOrDefault(emptyList())

        return Receipt(
            id = id,
            setId = setId,
            setName = setName,
            isSealed = isSealed,
            totalFilesVerified = totalFilesVerified,
            totalBytesVerified = totalBytesVerified,
            durationSeconds = durationSeconds,
            averageSpeedMbS = averageSpeedMbS,
            verificationLevel = runCatching { VerificationLevel.valueOf(verificationLevel) }.getOrDefault(VerificationLevel.FULL_REREAD),
            generationNumber = generationNumber,
            manifestSha256 = manifestSha256,
            keptDeletedFilesCount = keptDeletedFilesCount,
            skippedFiles = skipped,
            timestampMs = timestampMs
        )
    }

    companion object {
        fun fromDomain(r: Receipt): ReceiptEntity = ReceiptEntity(
            id = r.id,
            setId = r.setId,
            setName = r.setName,
            isSealed = r.isSealed,
            totalFilesVerified = r.totalFilesVerified,
            totalBytesVerified = r.totalBytesVerified,
            durationSeconds = r.durationSeconds,
            averageSpeedMbS = r.averageSpeedMbS,
            verificationLevel = r.verificationLevel.name,
            generationNumber = r.generationNumber,
            manifestSha256 = r.manifestSha256,
            keptDeletedFilesCount = r.keptDeletedFilesCount,
            skippedFilesJson = Json.encodeToString(r.skippedFiles),
            timestampMs = r.timestampMs
        )
    }
}
