package `in`.soumyaranjansahoo.dockdrop.ui.screens.drop

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.soumyaranjansahoo.dockdrop.core.model.DropProgress
import `in`.soumyaranjansahoo.dockdrop.core.model.DropState
import `in`.soumyaranjansahoo.dockdrop.core.model.SetDefinition
import `in`.soumyaranjansahoo.dockdrop.core.model.SourceFolder
import `in`.soumyaranjansahoo.dockdrop.ui.components.DockHero
import `in`.soumyaranjansahoo.dockdrop.ui.components.DropParticles
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockBody
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockBodySmall
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockHeadline
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockMono
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockTheme
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockTitle
import `in`.soumyaranjansahoo.dockdrop.ui.theme.RadiusMd

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropScreen(
    set: SetDefinition,
    sourceFolders: List<SourceFolder>,
    dropState: DropState,
    onPauseClick: () -> Unit,
    onResumeClick: () -> Unit,
    onStopClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = DockTheme.colors
    var showDetails by remember { mutableStateOf(false) }
    var showStopConfirmSheet by remember { mutableStateOf(false) }

    val progress = when (dropState) {
        is DropState.Dropping -> dropState.progress
        is DropState.Paused -> dropState.progress
        is DropState.Interrupted -> dropState.progress
        is DropState.Resumable -> dropState.progress
        else -> null
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = colors.bg,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "←",
                        style = DockTitle.copy(fontSize = 24.sp),
                        color = colors.textPrimary,
                        modifier = Modifier
                            .padding(8.dp)
                            .clickable { onBackClick() }
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = set.name, style = DockTitle, color = colors.textPrimary)
                }

                Text(
                    text = if (showDetails) "Hide Details" else "Details",
                    style = DockBodySmall.copy(fontWeight = FontWeight.Medium),
                    color = colors.dock,
                    modifier = Modifier
                        .padding(8.dp)
                        .clickable { showDetails = !showDetails }
                )
            }
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "You can lock your phone. Backup keeps going.",
                    style = DockBodySmall,
                    color = colors.textTertiary
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    if (dropState is DropState.Paused) {
                        TextButton(onClick = onResumeClick) {
                            Text("Resume", style = DockBody.copy(color = colors.dock, fontWeight = FontWeight.SemiBold))
                        }
                    } else if (dropState is DropState.Dropping) {
                        TextButton(onClick = onPauseClick) {
                            Text("Pause", style = DockBody.copy(color = colors.textSecondary))
                        }
                    }

                    TextButton(onClick = { showStopConfirmSheet = true }) {
                        Text("Stop", style = DockBody.copy(color = colors.danger))
                    }
                }
            }
        }
    ) { innerPadding ->
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

            // Live Particle Data Flow Area
            val speed = progress?.filesPerSec ?: 0.0
            val curFile = progress?.currentFilePath ?: ""
            DropParticles(
                filesPerSec = speed,
                currentFileName = curFile,
                modifier = Modifier.fillMaxWidth()
            )

            // Dock Hero with Ring
            val pct = (progress?.percentage ?: 0) / 100f
            DockHero(state = dropState, progressPct = pct)

            Spacer(modifier = Modifier.height(16.dp))

            // Status Headline & Mono Counters
            when (dropState) {
                is DropState.Dropping -> {
                    Text(
                        text = "Backing up ${dropState.progress.currentFolder}",
                        style = DockHeadline,
                        color = colors.textPrimary,
                        modifier = Modifier.semantics {
                            liveRegion = LiveRegionMode.Polite
                            contentDescription = "Backing up ${dropState.progress.currentFolder}, $pctBucket percent complete"
                        }
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "${dropState.progress.copiedFiles} / ${dropState.progress.totalFiles} files · ${formatBytes(dropState.progress.copiedBytes)} / ${formatBytes(dropState.progress.totalBytes)}",
                        style = DockMono.copy(fontSize = 14.sp),
                        color = colors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "about ${String.format("%.1f", dropState.progress.estMinutesRemaining)} min left",
                        style = DockMono.copy(color = colors.dock, fontSize = 13.sp)
                    )
                }

                is DropState.Verifying -> {
                    Text("Verifying ${dropState.currentFolder}", style = DockHeadline, color = colors.seal)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "${dropState.verifiedFiles} of ${dropState.totalFiles} files verified · re-read check",
                        style = DockMono.copy(fontSize = 14.sp),
                        color = colors.textSecondary
                    )
                }

                is DropState.Paused -> {
                    Text("Paused", style = DockHeadline, color = colors.attention)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(dropState.reason.message, style = DockBodySmall, color = colors.textSecondary)
                }

                is DropState.Interrupted -> {
                    Text("Backup interrupted", style = DockHeadline, color = colors.danger)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("SSD was disconnected. Plug it back in to resume.", style = DockBodySmall, color = colors.textSecondary)
                }

                is DropState.Preflight -> {
                    Text("Checking your SSD…", style = DockHeadline, color = colors.textPrimary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Testing write access and preparing files", style = DockBodySmall, color = colors.textSecondary)
                }

                else -> {}
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Folder Segments Row (Camera ✓ · Pictures ● · Documents)
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                itemsIndexed(sourceFolders) { index, folder ->
                    val isDone = (progress?.folderIndex ?: 0) > index + 1
                    val isCurrent = (progress?.folderIndex ?: 0) == index + 1

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isCurrent) colors.surfaceRaised else colors.surface)
                            .border(
                                1.dp,
                                if (isDone) colors.seal else if (isCurrent) colors.dock else colors.outline,
                                RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isDone) {
                                Text("✓ ", style = DockMono.copy(color = colors.seal, fontSize = 12.sp))
                            } else if (isCurrent) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(colors.dock)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Text(folder.label, style = DockBodySmall, color = colors.textPrimary)
                        }
                    }
                }
            }

            // Expandable Technical Details Disclosure
            AnimatedVisibility(visible = showDetails) {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(RadiusMd))
                        .border(1.dp, colors.outline, RoundedCornerShape(RadiusMd)),
                    colors = CardDefaults.cardColors(containerColor = colors.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("CURRENT FILE", style = DockMono.copy(fontSize = 10.sp), color = colors.textTertiary)
                        Text(
                            text = progress?.currentFilePath?.ifEmpty { "Scanning…" } ?: "None",
                            style = DockMono.copy(fontSize = 11.sp),
                            color = colors.textPrimary,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Speed: ${String.format("%.1f", progress?.speedMbS ?: 0.0)} MB/s", style = DockMono.copy(fontSize = 11.sp), color = colors.textSecondary)
                            Text("Rate: ${String.format("%.1f", progress?.filesPerSec ?: 0.0)} files/s", style = DockMono.copy(fontSize = 11.sp), color = colors.textSecondary)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Verification: size + SHA-256 on copy + re-read", style = DockMono.copy(fontSize = 11.sp), color = colors.seal)
                    }
                }
            }
        }
    }

    if (showStopConfirmSheet) {
        ModalBottomSheet(
            onDismissRequest = { showStopConfirmSheet = false },
            containerColor = colors.surface,
            shape = RoundedCornerShape(topStart = RadiusMd, topEnd = RadiusMd)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp, top = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Stop backup?",
                    style = DockHeadline,
                    color = colors.textPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Files copied so far are kept. You can resume later.",
                    style = DockBodySmall,
                    color = colors.textSecondary
                )
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { showStopConfirmSheet = false }) {
                        Text("Keep going", style = DockBody.copy(color = colors.dock, fontWeight = FontWeight.SemiBold))
                    }
                    Button(
                        onClick = {
                            showStopConfirmSheet = false
                            onStopClick()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = colors.danger)
                    ) {
                        Text("Stop", style = DockBody.copy(color = colors.textPrimary))
                    }
                }
            }
        }
    }
}

private fun formatBytes(bytes: Long): String {
    val gb = bytes.toDouble() / (1024 * 1024 * 1024)
    return if (gb >= 1.0) String.format("%.1f GB", gb) else "${bytes / (1024 * 1024)} MB"
}
