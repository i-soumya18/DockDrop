package `in`.soumyaranjansahoo.dockdrop.core.support

import android.content.Context
import android.os.Build
import `in`.soumyaranjansahoo.dockdrop.core.model.Receipt
import `in`.soumyaranjansahoo.dockdrop.core.model.SetDefinition
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class PersistedPermissionInfo(
    val isRead: Boolean,
    val isWrite: Boolean,
    val persistedTimeMs: Long
)

object LogExporter {

    fun generateDiagnosticLog(
        context: Context,
        sets: List<SetDefinition>,
        recentReceipts: List<Receipt>
    ): String {
        val permissions = try {
            context.contentResolver.persistedUriPermissions.map {
                PersistedPermissionInfo(
                    isRead = it.isReadPermission,
                    isWrite = it.isWritePermission,
                    persistedTimeMs = it.persistedTime
                )
            }
        } catch (_: Throwable) {
            emptyList()
        }
        val pkg = try { context.packageName } catch (_: Throwable) { "in.soumyaranjansahoo.dockdrop" }
        return generateDiagnosticLog(
            packageName = pkg,
            persistedPermissions = permissions,
            sets = sets,
            recentReceipts = recentReceipts
        )
    }

    fun generateDiagnosticLog(
        packageName: String,
        persistedPermissions: List<PersistedPermissionInfo>,
        sets: List<SetDefinition>,
        recentReceipts: List<Receipt>,
        deviceInfo: String = "${Build.MANUFACTURER} ${Build.MODEL} (${Build.DEVICE})",
        osInfo: String = "${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT}, Build ${Build.DISPLAY})"
    ): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss z", Locale.US)
        val nowFormatted = dateFormat.format(Date())

        val sb = StringBuilder()
        sb.appendLine("==================================================")
        sb.appendLine("               DOCKDROP DIAGNOSTIC LOG             ")
        sb.appendLine("==================================================")
        sb.appendLine("Generated: $nowFormatted")
        sb.appendLine("Package: $packageName")
        sb.appendLine("App Version: 1.0 (versionCode 1)")
        sb.appendLine("Device: $deviceInfo")
        sb.appendLine("Android OS: $osInfo")
        sb.appendLine("Network Status: Strictly Offline (zero network permissions declared)")
        sb.appendLine()

        sb.appendLine("--------------------------------------------------")
        sb.appendLine("1. PERSISTED STORAGE ACCESS (SAF)")
        sb.appendLine("--------------------------------------------------")
        sb.appendLine("Active persisted URI grants: ${persistedPermissions.size}")
        persistedPermissions.forEachIndexed { idx, perm ->
            val readWrite = when {
                perm.isRead && perm.isWrite -> "Read+Write"
                perm.isRead -> "Read"
                perm.isWrite -> "Write"
                else -> "None"
            }
            sb.appendLine("  [${idx + 1}] $readWrite (persisted: ${dateFormat.format(Date(perm.persistedTimeMs))})")
        }
        sb.appendLine()

        sb.appendLine("--------------------------------------------------")
        sb.appendLine("2. CONFIGURED BACKUP SETS (${sets.size})")
        sb.appendLine("--------------------------------------------------")
        if (sets.isEmpty()) {
            sb.appendLine("No Backup Sets configured.")
        } else {
            sets.forEachIndexed { idx, set ->
                sb.appendLine("Set #${idx + 1}: ${set.name} (ID: ${set.id})")
                sb.appendLine("  Dock Label: ${set.dockUserLabel}")
                sb.appendLine("  Dock Volume UUID: ${set.dockVolumeUuid}")
                sb.appendLine("  Deletion Policy: ${set.deletionPolicy.name} (Retention: ${set.retentionDays} days)")
                sb.appendLine("  Encrypted: ${set.isEncrypted} (DDC1 AES-256-GCM)")
                sb.appendLine("  Verification Level: ${set.verificationLevel.name}")
                sb.appendLine("  Min Battery Condition: ${if (set.minBatteryPct >= 100) "Charging only" else "${set.minBatteryPct}%"}")
                val lastSealedStr = set.lastSealedMs?.let { dateFormat.format(Date(it)) } ?: "Never"
                sb.appendLine("  Last Status: ${set.lastStatus ?: "None"} (Sealed at: $lastSealedStr, Files: ${set.lastSealedFiles})")
                sb.appendLine()
            }
        }

        sb.appendLine("--------------------------------------------------")
        sb.appendLine("3. RECENT BACKUP RECEIPTS (${recentReceipts.size})")
        sb.appendLine("--------------------------------------------------")
        if (recentReceipts.isEmpty()) {
            sb.appendLine("No backup receipts recorded.")
        } else {
            recentReceipts.forEachIndexed { idx, r ->
                val dateStr = dateFormat.format(Date(r.timestampMs))
                val statusStr = if (r.isSealed) "SEALED [100% Verified]" else "PARTIAL (${r.skippedFiles.size} skipped)"
                sb.appendLine("Receipt #${idx + 1}: Set '${r.setName}' · $statusStr")
                sb.appendLine("  Timestamp: $dateStr (Generation #${r.generationNumber})")
                sb.appendLine("  Verified: ${r.totalFilesVerified} files (${formatBytes(r.totalBytesVerified)})")
                sb.appendLine("  Speed: ${String.format(Locale.US, "%.1f", r.averageSpeedMbS)} MB/s · Duration: ${r.durationSeconds}s")
                sb.appendLine("  Manifest Hash: ${r.manifestSha256.take(16)}…")
                if (r.skippedFiles.isNotEmpty()) {
                    sb.appendLine("  Skipped files (${r.skippedFiles.size}):")
                    r.skippedFiles.take(5).forEach { sf ->
                        sb.appendLine("    - ${sf.relativePath}: ${sf.reason}")
                    }
                    if (r.skippedFiles.size > 5) {
                        sb.appendLine("    - ... and ${r.skippedFiles.size - 5} more")
                    }
                }
                sb.appendLine()
            }
        }

        sb.appendLine("==================================================")
        sb.appendLine("                   END OF LOG                     ")
        sb.appendLine("==================================================")
        return sb.toString()
    }

    private fun formatBytes(bytes: Long): String {
        val gb = bytes.toDouble() / (1024 * 1024 * 1024)
        return if (gb >= 1.0) String.format(Locale.US, "%.2f GB", gb) else "${bytes / (1024 * 1024)} MB"
    }
}
