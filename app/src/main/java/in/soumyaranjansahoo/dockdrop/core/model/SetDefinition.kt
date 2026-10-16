package `in`.soumyaranjansahoo.dockdrop.core.model

data class SetDefinition(
    val id: String,
    val name: String,
    val dockVolumeUuid: String,
    val dockUserLabel: String,
    val destinationVaultUriString: String,
    val deletionPolicy: DeletionPolicy = DeletionPolicy.KEEP,
    val retentionDays: Int = 30,
    val minBatteryPct: Int = 20,
    val isEncrypted: Boolean = false,
    val verificationLevel: VerificationLevel = VerificationLevel.FULL_REREAD,
    val createdAtMs: Long = System.currentTimeMillis(),
    val lastSealedMs: Long? = null,
    val lastSealedFiles: Int = 0,
    val lastSealedBytes: Long = 0L,
    val lastStatus: String? = null
)
