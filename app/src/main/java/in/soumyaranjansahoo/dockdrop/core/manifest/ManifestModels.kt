package `in`.soumyaranjansahoo.dockdrop.core.manifest

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DriveInfo(
    @SerialName("driveId") val driveId: String,
    @SerialName("createdAt") val createdAt: Long,
    @SerialName("formatVersion") val formatVersion: Int = 1,
    @SerialName("deviceLabel") val deviceLabel: String
)

@Serializable
data class ManifestEntry(
    @SerialName("p") val path: String,             // Relative path under set (e.g. "Camera/IMG_001.jpg")
    @SerialName("s") val sizeBytes: Long,          // Size in bytes
    @SerialName("m") val modifiedMs: Long,         // Last modified timestamp in ms
    @SerialName("h") val sha256: String,           // SHA-256 hex digest
    @SerialName("st") val status: String = "present", // "present" or "deleted"
    @SerialName("g") val generation: Long,         // Generation number when file was created/updated
    @SerialName("d") val deletedDate: String? = null, // yyyy-MM-dd when marked deleted
    @SerialName("dp") val deletedPath: String? = null // Relative path under _deleted/ (e.g. "_deleted/My phone/2026-10-16/Camera/IMG_001.jpg")
)

data class ManifestGeneration(
    val generationNumber: Long,
    val entries: Map<String, ManifestEntry>, // path -> ManifestEntry
    val sha256Checksum: String
)

@Serializable
data class SetMetadata(
    @SerialName("setId") val setId: String,
    @SerialName("setName") val setName: String,
    @SerialName("sourceLabels") val sourceLabels: List<String> = emptyList(),
    @SerialName("updatedAtMs") val updatedAtMs: Long = System.currentTimeMillis(),
    @SerialName("isEncrypted") val isEncrypted: Boolean = false
)

data class DiscoveredBackupSet(
    val setId: String,
    val setName: String,
    val deviceLabel: String,
    val driveId: String,
    val generation: ManifestGeneration,
    val sourceLabels: List<String>,
    val lastModifiedMs: Long,
    val isEncrypted: Boolean
)

