package `in`.soumyaranjansahoo.dockdrop.ui.components

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.soumyaranjansahoo.dockdrop.core.model.Receipt
import `in`.soumyaranjansahoo.dockdrop.core.model.VerificationLevel
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockBody
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockBodySmall
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockHeadline
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockMono
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockMotion
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockTheme
import `in`.soumyaranjansahoo.dockdrop.ui.theme.LocalReducedMotion
import `in`.soumyaranjansahoo.dockdrop.ui.theme.RadiusLg
import `in`.soumyaranjansahoo.dockdrop.ui.theme.RadiusMd
import `in`.soumyaranjansahoo.dockdrop.ui.theme.RadiusSm
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Apple-grade perforated cryptographic receipt card.
 * Adheres to CLAUDE.md Rule 2: Green seal is earned ONLY after cryptographic verification passes
 * with zero skipped files.
 */
@Composable
fun PerforatedReceiptCard(
    receipt: Receipt,
    onDone: () -> Unit,
    onTestRestoreClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val colors = DockTheme.colors
    val context = LocalContext.current
    val isReducedMotion = LocalReducedMotion.current
    var showDetails by remember { mutableStateOf(false) }

    val stampScale = remember { Animatable(if (isReducedMotion) 1f else 1.15f) }
    LaunchedEffect(isReducedMotion) {
        if (!isReducedMotion) {
            stampScale.animateTo(1f, animationSpec = DockMotion.Snap)
        } else {
            stampScale.snapTo(1f)
        }
    }

    val isFullyVerified = receipt.isSealed &&
        !receipt.isPartial &&
        receipt.verificationLevel != VerificationLevel.SIZE_ONLY

    val sealBorderColor = if (isFullyVerified) colors.seal else colors.attention
    val sealTextColor = if (isFullyVerified) colors.sealText else colors.attentionText
    val badgeLabel = when {
        isFullyVerified -> "VERIFIED & SEALED"
        receipt.verificationLevel == VerificationLevel.SIZE_ONLY -> "UNVERIFIED (SIZE ONLY)"
        else -> "PARTIAL BACKUP"
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(RadiusLg))
            .background(colors.surface)
            .border(1.dp, colors.outline.copy(alpha = 0.6f), RoundedCornerShape(RadiusLg))
    ) {
        // Perforated top border canvas with rhythmic punch-holes
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(18.dp)
        ) {
            val holeRadius = 3.5.dp.toPx()
            val spacing = 16.dp.toPx()
            val count = (size.width / spacing).toInt()
            for (i in 0..count) {
                drawCircle(
                    color = colors.bg,
                    radius = holeRadius,
                    center = Offset(i * spacing + spacing / 2, 0f)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Certified Hardware Seal Badge
            Box(
                modifier = Modifier
                    .scale(stampScale.value)
                    .clip(RoundedCornerShape(RadiusSm))
                    .background(sealBorderColor.copy(alpha = 0.12f))
                    .border(1.5.dp, sealBorderColor, RoundedCornerShape(RadiusSm))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (isFullyVerified) Icons.Outlined.Verified else Icons.Outlined.Warning,
                        contentDescription = null,
                        tint = sealTextColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = badgeLabel,
                        style = DockMono.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp, fontSize = 12.sp),
                        color = sealTextColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = if (isFullyVerified) "Sealed" else "Finished with ${receipt.skippedFiles.size} skipped",
                style = DockHeadline,
                color = colors.textPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = receipt.setName,
                style = DockBodySmall.copy(fontWeight = FontWeight.SemiBold),
                color = colors.dock
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Primary Stats Container with Tabular Figures
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(colors.surfaceRaised)
                    .border(1.dp, colors.outline.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                    .padding(vertical = 14.dp, horizontal = 10.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatColumn(label = "FILES", value = "${receipt.totalFilesVerified}")
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(36.dp)
                        .background(colors.outline.copy(alpha = 0.35f))
                )
                StatColumn(label = "SIZE", value = formatBytes(receipt.totalBytesVerified))
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(36.dp)
                        .background(colors.outline.copy(alpha = 0.35f))
                )
                StatColumn(label = "DURATION", value = formatDuration(receipt.durationSeconds))
            }

            Spacer(modifier = Modifier.height(18.dp))
            HorizontalDivider(color = colors.outline.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(14.dp))

            // Plain Truth Trust Statements (SCREENS.md S10)
            FactRow(
                text = "Verified: ${receipt.verificationLevel.displayName}",
                isCheck = true,
                isVerified = isFullyVerified
            )
            FactRow(
                text = "Nothing was deleted from your phone.",
                isCheck = true,
                isVerified = isFullyVerified
            )
            if (receipt.keptDeletedFilesCount > 0) {
                FactRow(
                    text = "Kept from earlier: ${receipt.keptDeletedFilesCount} deleted files in _deleted/",
                    isCheck = false,
                    isVerified = isFullyVerified
                )
            }
            FactRow(
                text = "All writes finished. Safe to unplug.",
                isCheck = true,
                isVerified = isFullyVerified
            )

            // Skipped files summary when Partial
            if (receipt.skippedFiles.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(RadiusSm))
                        .background(colors.attention.copy(alpha = 0.1f))
                        .border(1.dp, colors.attention.copy(alpha = 0.4f), RoundedCornerShape(RadiusSm))
                        .padding(12.dp)
                ) {
                    Text(
                        text = "${receipt.skippedFiles.size} file(s) couldn't be read:",
                        style = DockBodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = colors.attentionText
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    receipt.skippedFiles.take(5).forEach { skipped ->
                        Text(
                            text = "• ${middleEllipsizeReceiptPath(skipped.relativePath)} (${skipped.reason})",
                            style = DockMono.copy(fontSize = 11.sp),
                            color = colors.textPrimary,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Expandable "Details" Disclosure per SCREENS.md S10 & §2
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 48.dp)
                    .clip(RoundedCornerShape(RadiusSm))
                    .clickable(role = Role.Button) { showDetails = !showDetails }
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (showDetails) "Hide technical details" else "Details",
                    style = DockBodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.textSecondary
                )
                Icon(
                    imageVector = if (showDetails) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                    contentDescription = if (showDetails) "Collapse technical details" else "Expand technical details",
                    tint = colors.textSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }

            AnimatedVisibility(visible = showDetails) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(RadiusSm))
                        .background(colors.surfaceRaised)
                        .border(1.dp, colors.outline.copy(alpha = 0.4f), RoundedCornerShape(RadiusSm))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    DetailMonoRow(label = "Generation", value = "#${receipt.generationNumber}")
                    DetailMonoRow(
                        label = "Average speed",
                        value = "${String.format(Locale.US, "%.1f", receipt.averageSpeedMbS)} MB/s"
                    )
                    DetailMonoRow(
                        label = "Skipped files",
                        value = if (receipt.skippedFiles.isEmpty()) "None (0)" else "${receipt.skippedFiles.size}"
                    )
                    DetailMonoRow(
                        label = "Manifest SHA-256",
                        value = middleEllipsizeReceiptPath(receipt.manifestSha256, 24)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Primary Action CTA (56dp)
            Button(
                onClick = onDone,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(RadiusMd),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.dock,
                    contentColor = colors.onDock
                )
            ) {
                Text(
                    "Done",
                    style = DockBody.copy(fontWeight = FontWeight.Bold, fontSize = 16.sp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Secondary Actions (>= 48dp touch targets)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (onTestRestoreClick != null) {
                    OutlinedButton(
                        onClick = onTestRestoreClick,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(RadiusSm)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = null,
                            tint = colors.dock,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "Test a restore",
                            style = DockBodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = colors.dock
                        )
                    }
                }

                OutlinedButton(
                    onClick = {
                        val timestampStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
                            .format(Date(receipt.timestampMs))
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "DockDrop Backup Receipt - ${receipt.setName}")
                            putExtra(
                                Intent.EXTRA_TEXT,
                                buildString {
                                    appendLine("DOCKDROP BACKUP RECEIPT")
                                    appendLine("======================================")
                                    appendLine("Set:                ${receipt.setName}")
                                    appendLine("Status:             ${if (isFullyVerified) "Sealed" else "Partial"}")
                                    appendLine("Date:               $timestampStr")
                                    appendLine("Files verified:     ${receipt.totalFilesVerified}")
                                    appendLine("Total size:         ${formatBytes(receipt.totalBytesVerified)}")
                                    appendLine("Duration:           ${formatDuration(receipt.durationSeconds)}")
                                    appendLine("Average speed:      ${String.format(Locale.US, "%.1f", receipt.averageSpeedMbS)} MB/s")
                                    appendLine("Verification:       ${receipt.verificationLevel.displayName}")
                                    appendLine("Generation:         #${receipt.generationNumber}")
                                    appendLine("Manifest SHA-256:   ${receipt.manifestSha256}")
                                    if (receipt.keptDeletedFilesCount > 0) {
                                        appendLine("Kept in _deleted/:  ${receipt.keptDeletedFilesCount}")
                                    }
                                    if (receipt.skippedFiles.isNotEmpty()) {
                                        appendLine("Skipped files:      ${receipt.skippedFiles.size}")
                                    }
                                    appendLine("======================================")
                                    appendLine("Offline by design · Zero network permissions")
                                }
                            )
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share receipt"))
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(RadiusSm)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Share,
                        contentDescription = null,
                        tint = colors.textSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "Share receipt",
                        style = DockBodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = colors.textSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailMonoRow(label: String, value: String) {
    val colors = DockTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = DockBodySmall,
            color = colors.textSecondary
        )
        Text(
            text = value,
            style = DockMono.copy(fontSize = 12.sp, fontWeight = FontWeight.Medium),
            color = colors.textPrimary
        )
    }
}

@Composable
private fun StatColumn(label: String, value: String) {
    val colors = DockTheme.colors
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = DockMono.copy(fontSize = 17.sp, fontWeight = FontWeight.Bold),
            color = colors.textPrimary
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = DockMono.copy(fontSize = 10.sp, letterSpacing = 1.sp, fontWeight = FontWeight.SemiBold),
            color = colors.textTertiary
        )
    }
}

@Composable
private fun FactRow(
    text: String,
    isCheck: Boolean = true,
    isVerified: Boolean = true
) {
    val colors = DockTheme.colors
    val iconColor = if (isCheck) {
        if (isVerified) colors.seal else colors.attention
    } else {
        colors.dock
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (isCheck) Icons.Outlined.CheckCircle else Icons.Outlined.Shield,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = DockBodySmall,
            color = colors.textSecondary
        )
    }
}

private fun middleEllipsizeReceiptPath(value: String, maxChars: Int = 34): String {
    if (value.length <= maxChars) return value
    val half = (maxChars - 1) / 2
    return "${value.take(half)}…${value.takeLast(half)}"
}

private fun formatBytes(bytes: Long): String {
    val gb = bytes.toDouble() / (1024 * 1024 * 1024)
    return if (gb >= 1.0) String.format(Locale.US, "%.1f GB", gb) else "${bytes / (1024 * 1024)} MB"
}

private fun formatDuration(seconds: Long): String {
    val m = seconds / 60
    val s = seconds % 60
    return if (m > 0) "${m} min ${s} s" else "${s} s"
}
