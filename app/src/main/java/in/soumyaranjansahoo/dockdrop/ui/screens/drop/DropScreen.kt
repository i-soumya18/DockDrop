package `in`.soumyaranjansahoo.dockdrop.ui.screens.drop

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.soumyaranjansahoo.dockdrop.core.model.DropProgress
import `in`.soumyaranjansahoo.dockdrop.core.model.DropState
import `in`.soumyaranjansahoo.dockdrop.core.model.SetDefinition
import `in`.soumyaranjansahoo.dockdrop.core.model.SourceFolder
import `in`.soumyaranjansahoo.dockdrop.ui.components.DockHero
import `in`.soumyaranjansahoo.dockdrop.ui.components.DockTopAppBar
import `in`.soumyaranjansahoo.dockdrop.ui.components.DropParticles
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockBody
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockBodySmall
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockHeadline
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockMono
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockTheme
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockTitle
import `in`.soumyaranjansahoo.dockdrop.ui.theme.RadiusMd

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

    val infiniteTransition = rememberInfiniteTransition(label = "pulseTransition")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = colors.bg,
        topBar = {
            DockTopAppBar(
                title = set.name,
                subtitle = "Active Backup",
                onBackClick = onBackClick,
                actions = {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(colors.surfaceRaised)
                            .clickable { showDetails = !showDetails }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Info,
                                contentDescription = null,
                                tint = if (showDetails) colors.dock else colors.textSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (showDetails) "Hide Info" else "Telemetry",
                                style = DockBodySmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (showDetails) colors.dock else colors.textSecondary
                                )
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Screen can be locked · DockDrop runs in the background",
                    style = DockBodySmall.copy(fontSize = 12.sp),
                    color = colors.textTertiary
                )
                Spacer(modifier = Modifier.height(10.dp))

                // Floating Action Pill
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(colors.surface)
                        .border(1.dp, colors.outline.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (dropState is DropState.Paused) {
                        Button(
                            onClick = onResumeClick,
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = colors.dock)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.PlayArrow,
                                contentDescription = null,
                                tint = colors.onDock,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Resume", style = DockBody.copy(color = colors.onDock, fontWeight = FontWeight.Bold))
                        }
                    } else if (dropState is DropState.Dropping) {
                        OutlinedButton(
                            onClick = onPauseClick,
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Pause,
                                contentDescription = null,
                                tint = colors.textPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Pause", style = DockBody.copy(color = colors.textPrimary, fontWeight = FontWeight.SemiBold))
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    OutlinedButton(
                        onClick = { showStopConfirmSheet = true },
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Stop,
                            contentDescription = null,
                            tint = colors.danger,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Stop", style = DockBody.copy(color = colors.danger, fontWeight = FontWeight.SemiBold))
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
            Spacer(modifier = Modifier.height(4.dp))

            // Live Particle Data Flow Area
            val speed = progress?.filesPerSec ?: 0.0
            val curFile = progress?.currentFilePath ?: ""
            DropParticles(
                filesPerSec = speed,
                currentFileName = curFile,
                modifier = Modifier.fillMaxWidth()
            )

            // Tactile Dock Hero with Arc & LED Strip
            val pct = (progress?.percentage ?: 0) / 100f
            DockHero(state = dropState, progressPct = pct)

            Spacer(modifier = Modifier.height(16.dp))

            // Status Headline & Live Telemetry Card
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
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${dropState.progress.percentage}% completed · about ${String.format("%.1f", dropState.progress.estMinutesRemaining)} min remaining",
                        style = DockMono.copy(color = colors.dock, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    )
                }

                is DropState.Verifying -> {
                    Text("Verifying ${dropState.currentFolder}", style = DockHeadline, color = colors.seal)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${dropState.verifiedFiles} of ${dropState.totalFiles} files verified · re-read check",
                        style = DockMono.copy(fontSize = 13.sp, color = colors.seal, fontWeight = FontWeight.SemiBold)
                    )
                }

                is DropState.Paused -> {
                    Text("Backup Paused", style = DockHeadline, color = colors.attention)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(dropState.reason.message, style = DockBodySmall, color = colors.textSecondary)
                }

                is DropState.Interrupted -> {
                    Text("SSD Disconnected", style = DockHeadline, color = colors.danger)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Plug your SSD back in to resume where it left off.", style = DockBodySmall, color = colors.textSecondary)
                }

                is DropState.Preflight -> {
                    Text("Checking Storage & Permissions…", style = DockHeadline, color = colors.textPrimary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Running write verification test…", style = DockBodySmall, color = colors.textSecondary)
                }

                else -> {}
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Folder Segments Carousel (Camera ✓ · Pictures ● · Documents)
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                itemsIndexed(sourceFolders) { index, folder ->
                    val isDone = (progress?.folderIndex ?: 0) > index + 1
                    val isCurrent = (progress?.folderIndex ?: 0) == index + 1

                    val chipBorderColor by animateColorAsState(
                        targetValue = when {
                            isDone -> colors.seal
                            isCurrent -> colors.dock
                            else -> colors.outline.copy(alpha = 0.5f)
                        },
                        label = "chipBorder"
                    )

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                when {
                                    isDone -> colors.seal.copy(alpha = 0.08f)
                                    isCurrent -> colors.dock.copy(alpha = 0.08f)
                                    else -> colors.surface
                                }
                            )
                            .border(if (isCurrent) 1.5.dp else 1.dp, chipBorderColor, RoundedCornerShape(14.dp))
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isDone) {
                                Icon(
                                    imageVector = Icons.Outlined.Check,
                                    contentDescription = null,
                                    tint = colors.seal,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            } else if (isCurrent) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(colors.dock.copy(alpha = pulseAlpha))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Text(
                                text = folder.label,
                                style = DockBodySmall.copy(
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isDone) colors.seal else if (isCurrent) colors.dock else colors.textPrimary
                                )
                            )
                        }
                    }
                }
            }

            // Expandable Telemetry Card
            AnimatedVisibility(visible = showDetails) {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .border(1.dp, colors.outline.copy(alpha = 0.5f), RoundedCornerShape(18.dp)),
                    colors = CardDefaults.cardColors(containerColor = colors.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "LIVE STREAM METRICS",
                            style = DockMono.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                            color = colors.textTertiary
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Throughput", style = DockBodySmall, color = colors.textSecondary)
                                Text(
                                    "${String.format("%.1f", progress?.speedMbS ?: 0.0)} MB/s",
                                    style = DockMono.copy(fontWeight = FontWeight.Bold, color = colors.textPrimary)
                                )
                            }
                            Column {
                                Text("Rate", style = DockBodySmall, color = colors.textSecondary)
                                Text(
                                    "${String.format("%.1f", progress?.filesPerSec ?: 0.0)} files/s",
                                    style = DockMono.copy(fontWeight = FontWeight.Bold, color = colors.textPrimary)
                                )
                            }
                            Column {
                                Text("Copied Files", style = DockBodySmall, color = colors.textSecondary)
                                Text(
                                    "${progress?.copiedFiles ?: 0} / ${progress?.totalFiles ?: 0}",
                                    style = DockMono.copy(fontWeight = FontWeight.Bold, color = colors.textPrimary)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(colors.outline.copy(alpha = 0.35f))
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        Text("Current File:", style = DockBodySmall, color = colors.textTertiary)
                        Text(
                            text = progress?.currentFilePath?.ifEmpty { "Scanning directory…" } ?: "None",
                            style = DockMono.copy(fontSize = 11.sp),
                            color = colors.textPrimary,
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Stop Confirmation Sheet
    if (showStopConfirmSheet) {
        ModalBottomSheet(
            onDismissRequest = { showStopConfirmSheet = false },
            containerColor = colors.surface,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp, top = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(colors.attention.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Warning,
                        contentDescription = null,
                        tint = colors.attention,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Stop backup?",
                    style = DockHeadline,
                    color = colors.textPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Files copied so far are safely committed to your SSD. You can resume at any time.",
                    style = DockBodySmall,
                    color = colors.textSecondary
                )
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            showStopConfirmSheet = false
                            onStopClick()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Stop Backup", style = DockBody.copy(color = colors.danger, fontWeight = FontWeight.SemiBold))
                    }
                    Button(
                        onClick = { showStopConfirmSheet = false },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.dock)
                    ) {
                        Text("Keep Going", style = DockBody.copy(color = colors.onDock, fontWeight = FontWeight.Bold))
                    }
                }
            }
        }
    }
}
