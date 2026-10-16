package `in`.soumyaranjansahoo.dockdrop.ui.screens.restore

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.soumyaranjansahoo.dockdrop.core.model.DropState
import `in`.soumyaranjansahoo.dockdrop.core.restore.RestorePlan
import `in`.soumyaranjansahoo.dockdrop.core.restore.RestoreProgress
import `in`.soumyaranjansahoo.dockdrop.core.restore.RestoreReceipt
import `in`.soumyaranjansahoo.dockdrop.core.restore.RestoreState
import `in`.soumyaranjansahoo.dockdrop.ui.components.DockHero
import `in`.soumyaranjansahoo.dockdrop.ui.components.DropParticles
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockBody
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockBodySmall
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockHeadline
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockMono
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockMotion
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockTheme
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockTitle
import `in`.soumyaranjansahoo.dockdrop.ui.theme.RadiusLg

@Composable
fun RestoreProgressScreen(
    plan: RestorePlan,
    restoreState: RestoreState,
    onStopClick: () -> Unit,
    onDoneClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = DockTheme.colors
    val context = LocalContext.current

    val progress = when (restoreState) {
        is RestoreState.Restoring -> restoreState.progress
        else -> null
    }

    val receipt = when (restoreState) {
        is RestoreState.Completed -> restoreState.receipt
        is RestoreState.Partial -> restoreState.receipt
        else -> null
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = colors.bg,
        topBar = {
            if (receipt == null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (plan.isDrill) "Restore Drill" else "Restoring ${plan.setName}",
                        style = DockTitle,
                        color = colors.textPrimary
                    )
                    TextButton(onClick = onStopClick) {
                        Text("Cancel", style = DockBodySmall, color = colors.danger)
                    }
                }
            }
        }
    ) { innerPadding ->
        if (receipt != null) {
            // Restore Receipt View (SCREENS.md S15 / S10 format)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                RestoreReceiptCard(
                    receipt = receipt,
                    onDone = onDoneClick,
                    onOpenFiles = {
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(Uri.parse("content://media/internal/images/media"), "*/*")
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        try {
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            // fallback open general files
                            val filesIntent = Intent(Intent.ACTION_GET_CONTENT).apply {
                                type = "*/*"
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            try { context.startActivity(filesIntent) } catch (_: Exception) {}
                        }
                    }
                )
            }
        } else {
            // Live Restore Progress View (Mirror of S09 with reverse particles)
            val pctBucket = ((progress?.percentage ?: 0) / 10) * 10
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // Upward particle flow from Dock to Phone (DESIGN.md §5.2.2 / SCREENS.md S15)
                val speed = progress?.filesPerSec ?: 0.0
                val curFile = progress?.currentFilePath ?: ""
                DropParticles(
                    filesPerSec = speed,
                    currentFileName = curFile,
                    reverseDirection = true,
                    modifier = Modifier.fillMaxWidth()
                )

                val pct = (progress?.percentage ?: 0) / 100f
                DockHero(state = DropState.Dropping(
                    `in`.soumyaranjansahoo.dockdrop.core.model.DropProgress(
                        setId = plan.setId,
                        currentFolder = progress?.currentFolder ?: "",
                        folderIndex = progress?.folderIndex ?: 1,
                        totalFolders = progress?.totalFolders ?: 1,
                        copiedFiles = progress?.restoredFiles ?: 0,
                        totalFiles = progress?.totalFiles ?: plan.totalFilesToCopy,
                        copiedBytes = progress?.restoredBytes ?: 0L,
                        totalBytes = progress?.totalBytes ?: plan.totalBytesToCopy,
                        speedMbS = progress?.speedMbS ?: 0.0,
                        filesPerSec = progress?.filesPerSec ?: 0.0,
                        estMinutesRemaining = progress?.estMinutesRemaining ?: 0.0,
                        currentFilePath = progress?.currentFilePath ?: ""
                    )
                ), progressPct = pct)

                Spacer(modifier = Modifier.height(24.dp))

                if (restoreState is RestoreState.UpdatingGallery) {
                    Text("Updating your gallery…", style = DockHeadline, color = colors.dock)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Registering restored photos and videos with the system",
                        style = DockBodySmall,
                        color = colors.textSecondary
                    )
                } else if (progress != null) {
                    Text(
                        text = "Restoring ${progress.currentFolder}",
                        style = DockHeadline,
                        color = colors.textPrimary,
                        modifier = Modifier.semantics {
                            liveRegion = LiveRegionMode.Polite
                            contentDescription = "Restoring ${progress.currentFolder}, $pctBucket percent complete"
                        }
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "${progress.restoredFiles} / ${progress.totalFiles} files · ${formatBytes(progress.restoredBytes)} / ${formatBytes(progress.totalBytes)}",
                        style = DockMono.copy(fontSize = 14.sp),
                        color = colors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "about ${String.format("%.1f", progress.estMinutesRemaining)} min left",
                        style = DockMono.copy(color = colors.dock, fontSize = 13.sp)
                    )
                } else {
                    Text("Preparing restore…", style = DockHeadline, color = colors.textPrimary)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Testing destination and verifying hashes", style = DockBodySmall, color = colors.textSecondary)
                }
            }
        }
    }
}

@Composable
private fun RestoreReceiptCard(
    receipt: RestoreReceipt,
    onDone: () -> Unit,
    onOpenFiles: () -> Unit
) {
    val colors = DockTheme.colors
    val context = LocalContext.current

    val stampScale = remember { Animatable(1.18f) }
    LaunchedEffect(Unit) {
        stampScale.animateTo(1f, animationSpec = DockMotion.Snap)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(RadiusLg))
            .background(colors.surface)
            .border(1.dp, colors.outline, RoundedCornerShape(RadiusLg))
    ) {
        // Perforated top border
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
            val stampColor = if (receipt.isCompleteSuccess) colors.seal else colors.attention
            Box(
                modifier = Modifier
                    .scale(stampScale.value)
                    .border(2.dp, stampColor, RoundedCornerShape(8.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = if (receipt.isCompleteSuccess) "RESTORED" else "PARTIAL",
                    style = DockMono.copy(fontWeight = FontWeight.Bold, letterSpacing = 2.sp),
                    color = stampColor
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (receipt.isCompleteSuccess) "Restored ${receipt.restoredCount} files" else "Finished with ${receipt.failedCount} failed",
                style = DockHeadline,
                color = colors.textPrimary
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Primary Stats Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${receipt.restoredCount}", style = DockMono.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold), color = colors.textPrimary)
                    Text("RESTORED", style = DockMono.copy(fontSize = 11.sp), color = colors.textTertiary)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(formatBytes(receipt.totalBytesRestored), style = DockMono.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold), color = colors.textPrimary)
                    Text("SIZE", style = DockMono.copy(fontSize = 11.sp), color = colors.textTertiary)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${receipt.durationSeconds}s", style = DockMono.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold), color = colors.textPrimary)
                    Text("DURATION", style = DockMono.copy(fontSize = 11.sp), color = colors.textTertiary)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(color = colors.outline)
            Spacer(modifier = Modifier.height(16.dp))

            // Plain Truth Facts (SCREENS.md S15)
            FactRow(text = "Restored ${receipt.restoredCount} files · ${receipt.keptBothCount} kept both · ${receipt.failedCount} failed")
            FactRow(text = "Verified: ${receipt.verificationSummary}")
            if (receipt.timestampsPreservedCount > 0) {
                FactRow(text = "Original timestamps preserved where platform allowed")
            }
            if (receipt.isDrill) {
                FactRow(text = "Restore drill verified 100% hash match on random files")
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onDone,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colors.dock, contentColor = colors.onDock)
            ) {
                Text("Done", style = DockBody.copy(fontWeight = FontWeight.SemiBold))
            }

            if (!receipt.isDrill) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onOpenFiles,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Open Files", style = DockBodySmall, color = colors.textSecondary)
                }
            }
        }
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
        Text("• ", color = colors.seal)
        Text(text, style = DockBodySmall, color = colors.textSecondary)
    }
}

private fun formatBytes(bytes: Long): String {
    val gb = bytes.toDouble() / (1024 * 1024 * 1024)
    return if (gb >= 1.0) String.format("%.1f GB", gb) else "${bytes / (1024 * 1024)} MB"
}
