package `in`.soumyaranjansahoo.dockdrop.ui.components

import android.content.Intent
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.soumyaranjansahoo.dockdrop.core.model.Receipt
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockBody
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockBodySmall
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockHeadline
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockMono
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockMotion
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockTheme
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockTitle
import `in`.soumyaranjansahoo.dockdrop.ui.theme.RadiusLg

/**
 * Premium Google-style perforated cryptographic receipt card.
 * Adheres to CLAUDE.md Rule 2: Green seal (#10B981) is earned ONLY after verification passes.
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

    val stampScale = remember { Animatable(1.22f) }
    LaunchedEffect(Unit) {
        stampScale.animateTo(1f, animationSpec = DockMotion.Snap)
    }

    val isVerified = receipt.isSealed
    val sealColor = if (isVerified) colors.seal else colors.attention

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(colors.surface)
            .border(1.dp, colors.outline.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
    ) {
        // Perforated top border canvas with smooth rhythmic punch-holes
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
                    .clip(RoundedCornerShape(12.dp))
                    .background(sealColor.copy(alpha = 0.12f))
                    .border(1.5.dp, sealColor, RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (isVerified) Icons.Outlined.Verified else Icons.Outlined.Warning,
                        contentDescription = null,
                        tint = sealColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = if (isVerified) "VERIFIED & SEALED" else "PARTIAL BACKUP",
                        style = DockMono.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp, fontSize = 12.sp),
                        color = sealColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = if (isVerified) "Backup Complete" else "Finished with ${receipt.skippedFiles.size} skipped",
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

            // Plain Truth Trust Statements
            FactRow(text = "Verified: ${receipt.verificationLevel.displayName}", isCheck = true)
            FactRow(text = "Zero bytes modified or deleted on phone", isCheck = true)
            if (receipt.keptDeletedFilesCount > 0) {
                FactRow(text = "Kept from earlier: ${receipt.keptDeletedFilesCount} files in _deleted/", isCheck = false)
            }
            FactRow(text = "All writes finalized. Safe to unplug SSD now.", isCheck = true)

            Spacer(modifier = Modifier.height(24.dp))

            // Action Buttons
            Button(
                onClick = onDone,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(18.dp),
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
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = null,
                            tint = colors.dock,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Test Restore", style = DockBodySmall.copy(fontWeight = FontWeight.SemiBold), color = colors.dock)
                    }
                }

                OutlinedButton(
                    onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(
                                Intent.EXTRA_TEXT,
                                """
                                DockDrop Backup Receipt
                                Set: ${receipt.setName}
                                Status: ${if (receipt.isSealed) "Sealed" else "Partial"}
                                Files verified: ${receipt.totalFilesVerified}
                                Size: ${formatBytes(receipt.totalBytesVerified)}
                                Duration: ${formatDuration(receipt.durationSeconds)}
                                Verification: ${receipt.verificationLevel.displayName}
                                Manifest SHA-256: ${receipt.manifestSha256}
                                """.trimIndent()
                            )
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share Receipt"))
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Share,
                        contentDescription = null,
                        tint = colors.textSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Share", style = DockBodySmall.copy(fontWeight = FontWeight.SemiBold), color = colors.textSecondary)
                }
            }
        }
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
private fun FactRow(text: String, isCheck: Boolean = true) {
    val colors = DockTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (isCheck) Icons.Outlined.CheckCircle else Icons.Outlined.Shield,
            contentDescription = null,
            tint = colors.seal,
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

private fun formatBytes(bytes: Long): String {
    val gb = bytes.toDouble() / (1024 * 1024 * 1024)
    return if (gb >= 1.0) String.format("%.1f GB", gb) else "${bytes / (1024 * 1024)} MB"
}

private fun formatDuration(seconds: Long): String {
    val m = seconds / 60
    val s = seconds % 60
    return if (m > 0) "${m}m ${s}s" else "${s}s"
}
