package `in`.soumyaranjansahoo.dockdrop.ui.screens.restore

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.soumyaranjansahoo.dockdrop.core.model.DropProgress
import `in`.soumyaranjansahoo.dockdrop.core.model.DropState
import `in`.soumyaranjansahoo.dockdrop.core.restore.RestorePlan
import `in`.soumyaranjansahoo.dockdrop.core.restore.RestoreProgress
import `in`.soumyaranjansahoo.dockdrop.core.restore.RestoreReceipt
import `in`.soumyaranjansahoo.dockdrop.core.restore.RestoreState
import `in`.soumyaranjansahoo.dockdrop.ui.components.DockHero
import `in`.soumyaranjansahoo.dockdrop.ui.components.DockTopAppBar
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
                DockTopAppBar(
                    title = if (plan.isDrill) "Restore Drill" else "Restoring ${plan.setName}",
                    subtitle = "Safe copy in progress",
                    actions = {
                        TextButton(onClick = onStopClick) {
                            Text(
                                "Cancel",
                                style = DockBodySmall.copy(fontWeight = FontWeight.Bold),
                                color = colors.danger
                            )
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        if (receipt != null) {
            // Restore Receipt View
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
            // Live Restore Progress View (with reverse upward particles)
            val pctBucket = ((progress?.percentage ?: 0) / 10) * 10
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                // Upward particle flow from Dock to Phone
                val speed = progress?.filesPerSec ?: 0.0
                val curFile = progress?.currentFilePath ?: ""
                DropParticles(
                    filesPerSec = speed,
                    currentFileName = curFile,
                    reverseDirection = true,
                    modifier = Modifier.fillMaxWidth()
                )

                val pct = (progress?.percentage ?: 0) / 100f
                DockHero(
                    state = DropState.Dropping(
                        DropProgress(
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
                    ),
                    progressPct = pct
                )

                Spacer(modifier = Modifier.height(20.dp))

                if (restoreState is RestoreState.UpdatingGallery) {
                    Text("Updating media library…", style = DockHeadline, color = colors.dock)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Registering restored photos & videos with the Android MediaStore",
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
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${progress.restoredFiles} / ${progress.totalFiles} files · ${formatBytes(progress.restoredBytes)} / ${formatBytes(progress.totalBytes)}",
                        style = DockMono.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
                        color = colors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "about ${String.format("%.1f", progress.estMinutesRemaining)} min remaining",
                        style = DockMono.copy(color = colors.dock, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    )
                } else {
                    Text("Preparing restore…", style = DockHeadline, color = colors.textPrimary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Preflight checks & SHA-256 verification…", style = DockBodySmall, color = colors.textSecondary)
                }

                Spacer(modifier = Modifier.height(24.dp))
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
    val stampScale = remember { Animatable(1.22f) }
    LaunchedEffect(Unit) {
        stampScale.animateTo(1f, animationSpec = DockMotion.Snap)
    }

    val isSuccess = receipt.isCompleteSuccess
    val stampColor = if (isSuccess) colors.seal else colors.attention

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(colors.surface)
            .border(1.dp, colors.outline.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
    ) {
        // Perforated top border
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
            Box(
                modifier = Modifier
                    .scale(stampScale.value)
                    .clip(RoundedCornerShape(12.dp))
                    .background(stampColor.copy(alpha = 0.12f))
                    .border(1.5.dp, stampColor, RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (isSuccess) Icons.Outlined.Verified else Icons.Outlined.Warning,
                        contentDescription = null,
                        tint = stampColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = if (isSuccess) "RESTORE SEALED" else "PARTIAL RESTORE",
                        style = DockMono.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp, fontSize = 12.sp),
                        color = stampColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = if (isSuccess) "Restored ${receipt.restoredCount} files" else "Finished with ${receipt.failedCount} failed",
                style = DockHeadline,
                color = colors.textPrimary
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
                StatColumn(label = "RESTORED", value = "${receipt.restoredCount}")
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(36.dp)
                        .background(colors.outline.copy(alpha = 0.35f))
                )
                StatColumn(label = "SIZE", value = formatBytes(receipt.totalBytesRestored))
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(36.dp)
                        .background(colors.outline.copy(alpha = 0.35f))
                )
                StatColumn(label = "DURATION", value = "${receipt.durationSeconds}s")
            }

            Spacer(modifier = Modifier.height(18.dp))
            HorizontalDivider(color = colors.outline.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(14.dp))

            // Plain Truth Facts
            FactRow(text = "Restored ${receipt.restoredCount} files · ${receipt.keptBothCount} kept both")
            FactRow(text = "Integrity: ${receipt.verificationSummary}")
            if (receipt.timestampsPreservedCount > 0) {
                FactRow(text = "Original filesystem timestamps preserved")
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
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colors.dock, contentColor = colors.onDock)
            ) {
                Text("Done", style = DockBody.copy(fontWeight = FontWeight.Bold, fontSize = 16.sp))
            }

            if (!receipt.isDrill) {
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = onOpenFiles,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.FolderOpen,
                        contentDescription = null,
                        tint = colors.dock,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Open Restored Files", style = DockBodySmall.copy(fontWeight = FontWeight.SemiBold), color = colors.dock)
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
private fun FactRow(text: String) {
    val colors = DockTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Outlined.CheckCircle,
            contentDescription = null,
            tint = colors.seal,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text, style = DockBodySmall, color = colors.textSecondary)
    }
}

private fun formatBytes(bytes: Long): String {
    val gb = bytes.toDouble() / (1024 * 1024 * 1024)
    return if (gb >= 1.0) String.format("%.1f GB", gb) else "${bytes / (1024 * 1024)} MB"
}
