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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
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
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockDisplay
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockHeadline
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockMono
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockMotion
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockTheme
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockTitle
import `in`.soumyaranjansahoo.dockdrop.ui.theme.RadiusLg

@Composable
fun PerforatedReceiptCard(
    receipt: Receipt,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = DockTheme.colors
    val context = LocalContext.current

    val stampScale = remember { Animatable(1.18f) }
    LaunchedEffect(Unit) {
        stampScale.animateTo(1f, animationSpec = DockMotion.Snap)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(RadiusLg))
            .background(colors.surface)
            .border(1.dp, colors.outline, RoundedCornerShape(RadiusLg))
    ) {
        // Perforated top border canvas
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(16.dp)
        ) {
            val holeRadius = 3.dp.toPx()
            val spacing = 14.dp.toPx()
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
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Receipt Stamp
            val stampColor = if (receipt.isSealed) colors.seal else colors.attention
            Box(
                modifier = Modifier
                    .scale(stampScale.value)
                    .border(2.dp, stampColor, RoundedCornerShape(8.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = if (receipt.isSealed) "SEALED" else "PARTIAL",
                    style = DockMono.copy(fontWeight = FontWeight.Bold, letterSpacing = 2.sp),
                    color = stampColor
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (receipt.isSealed) "Sealed" else "Finished with ${receipt.skippedFiles.size} skipped",
                style = DockHeadline,
                color = colors.textPrimary
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Primary Stats Row (Tabular Figures)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatColumn(label = "FILES", value = "${receipt.totalFilesVerified}")
                StatColumn(label = "SIZE", value = formatBytes(receipt.totalBytesVerified))
                StatColumn(label = "DURATION", value = formatDuration(receipt.durationSeconds))
            }

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(color = colors.outline)
            Spacer(modifier = Modifier.height(16.dp))

            // Plain truth statements (SCREENS.md S10)
            FactRow(text = "Verified: ${receipt.verificationLevel.displayName}")
            FactRow(text = "Nothing was deleted from your phone.")
            if (receipt.keptDeletedFilesCount > 0) {
                FactRow(text = "Kept from earlier: ${receipt.keptDeletedFilesCount} files in _deleted/")
            }
            FactRow(text = "All writes finished. Safe to unplug.")

            Spacer(modifier = Modifier.height(24.dp))

            // Action Buttons
            Button(
                onClick = onDone,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.dock,
                    contentColor = colors.onDock
                )
            ) {
                Text("Done", style = DockBody.copy(fontWeight = FontWeight.SemiBold))
            }

            Spacer(modifier = Modifier.height(12.dp))

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
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Share receipt", style = DockBodySmall, color = colors.textSecondary)
            }
        }
    }
}

@Composable
private fun StatColumn(label: String, value: String) {
    val colors = DockTheme.colors
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, style = DockMono.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold), color = colors.textPrimary)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = label, style = DockMono.copy(fontSize = 11.sp, letterSpacing = 1.sp), color = colors.textTertiary)
    }
}

@Composable
private fun FactRow(text: String) {
    val colors = DockTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = "•", color = colors.seal, modifier = Modifier.padding(end = 8.dp))
        Text(text = text, style = DockBodySmall, color = colors.textSecondary)
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
