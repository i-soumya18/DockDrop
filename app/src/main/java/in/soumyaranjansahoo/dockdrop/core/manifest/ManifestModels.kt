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
    @SerialName("g") val generation: Long          // Generation number when file was created/updated
)

data class ManifestGeneration(
    val generationNumber: Long,
    val entries: Map<String, ManifestEntry>, // path -> ManifestEntry
    val sha256Checksum: String
)
