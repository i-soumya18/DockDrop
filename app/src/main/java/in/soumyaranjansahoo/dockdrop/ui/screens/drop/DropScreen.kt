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
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
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
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import `in`.soumyaranjansahoo.dockdrop.ui.theme.LocalReducedMotion
import `in`.soumyaranjansahoo.dockdrop.ui.theme.RadiusMd
import `in`.soumyaranjansahoo.dockdrop.ui.theme.RadiusSheet
import `in`.soumyaranjansahoo.dockdrop.ui.theme.RadiusSm
import java.util.Locale

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
    val isReducedMotion = LocalReducedMotion.current
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
    val animatedPulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )
    val pulseAlpha = if (isReducedMotion) 1.0f else animatedPulseAlpha

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = colors.bg,
        topBar = {
            DockTopAppBar(
                title = set.name,
                subtitle = "Live Backup",
                onBackClick = onBackClick,
                actions = {
                    Box(
                        modifier = Modifier
                            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                            .clip(RoundedCornerShape(RadiusSm))
                            .background(colors.surfaceRaised)
                            .border(1.dp, colors.outline.copy(alpha = 0.5f), RoundedCornerShape(RadiusSm))
                            .clickable(role = Role.Button) { showDetails = !showDetails }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Info,
                                contentDescription = if (showDetails) "Hide details" else "Show details",
                                tint = if (showDetails) colors.dock else colors.textSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Details",
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
                    text = "You can lock your phone. Backup keeps going.",
                    style = DockBodySmall.copy(fontSize = 12.sp),
                    color = colors.textTertiary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(10.dp))

                // Secondary Action Bar (SCREENS.md S09: Pause and Stop never styled as primary)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(RadiusMd))
                        .background(colors.surface)
                        .border(1.dp, colors.outline.copy(alpha = 0.5f), RoundedCornerShape(RadiusMd))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (dropState is DropState.Paused || dropState is DropState.Resumable) {
                        TextButton(
                            onClick = onResumeClick,
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 48.dp),
                            shape = RoundedCornerShape(RadiusSm)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.PlayArrow,
                                contentDescription = null,
                                tint = colors.dock,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Resume",
                                style = DockBody.copy(color = colors.dock, fontWeight = FontWeight.SemiBold)
                            )
                        }
                    } else if (dropState is DropState.Dropping) {
                        TextButton(
                            onClick = onPauseClick,
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 48.dp),
                            shape = RoundedCornerShape(RadiusSm)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Pause,
                                contentDescription = null,
                                tint = colors.textPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Pause",
                                style = DockBody.copy(color = colors.textPrimary, fontWeight = FontWeight.SemiBold)
                            )
                        }
                    }

                    TextButton(
                        onClick = { showStopConfirmSheet = true },
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp),
                        shape = RoundedCornerShape(RadiusSm)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Stop,
                            contentDescription = null,
                            tint = colors.danger,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "Stop",
                            style = DockBody.copy(color = colors.danger, fontWeight = FontWeight.SemiBold)
                        )
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

            // Live Particle Data Flow Area (with automatic static reduced-motion counter)
            val speed = progress?.filesPerSec ?: 0.0
            val curFile = progress?.currentFilePath ?: ""
            DropParticles(
                filesPerSec = speed,
                currentFileName = middleEllipsizePath(curFile, 32),
                modifier = Modifier.fillMaxWidth()
            )

            // Tactile Dock Hero with Arc & LED Strip
            val pct = when (dropState) {
                is DropState.Verifying -> {
                    if (dropState.totalFiles > 0) {
                        (dropState.verifiedFiles.toFloat() / dropState.totalFiles.toFloat()).coerceIn(0f, 1f)
                    } else 1f
                }
                else -> (progress?.percentage ?: 0) / 100f
            }
            DockHero(state = dropState, progressPct = pct)

            Spacer(modifier = Modifier.height(16.dp))

            // Status Headline & Live Telemetry Counters (SCREENS.md §3 & S09)
            when (dropState) {
                is DropState.Dropping -> {
                    val p = dropState.progress
                    Text(
                        text = "Backing up ${p.currentFolder}",
                        style = DockHeadline,
                        color = colors.textPrimary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.semantics {
                            liveRegion = LiveRegionMode.Polite
                            contentDescription = "Backing up ${p.currentFolder}. ${p.folderIndex} of ${p.totalFolders} folders. $pctBucket percent complete."
                        }
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = String.format(
                            Locale.US,
                            "%,d / %,d files · %s / %s",
                            p.copiedFiles,
                            p.totalFiles,
                            formatDropBytes(p.copiedBytes),
                            formatDropBytes(p.totalBytes)
                        ),
                        style = DockMono.copy(color = colors.dock, fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${p.folderIndex} of ${p.totalFolders} folders · ${formatEta(p.estMinutesRemaining)}",
                        style = DockBodySmall,
                        color = colors.textSecondary,
                        textAlign = TextAlign.Center
                    )
                }

                is DropState.Verifying -> {
                    // CLAUDE.md Rule 2: Green is earned ONLY after verification passes. Use colors.dock during Verifying!
                    Text(
                        text = "Verifying ${dropState.currentFolder}",
                        style = DockHeadline,
                        color = colors.dock,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.semantics {
                            liveRegion = LiveRegionMode.Polite
                            contentDescription = "Verifying ${dropState.currentFolder}, ${dropState.folderIndex} of ${dropState.totalFolders} folders"
                        }
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = String.format(
                            Locale.US,
                            "%d of %d folders · %,d / %,d files checked",
                            dropState.folderIndex,
                            dropState.totalFolders,
                            dropState.verifiedFiles,
                            dropState.totalFiles
                        ),
                        style = DockMono.copy(fontSize = 13.sp, color = colors.dock, fontWeight = FontWeight.SemiBold),
                        textAlign = TextAlign.Center
                    )
                }

                is DropState.Paused -> {
                    Text(
                        text = "Paused",
                        style = DockHeadline,
                        color = colors.attention,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = dropState.reason.message,
                        style = DockBodySmall,
                        color = colors.textSecondary,
                        textAlign = TextAlign.Center
                    )
                }

                is DropState.Interrupted -> {
                    // DESIGN.md §2: Amber means "you can fix this" (plug cable back in)
                    Text(
                        text = "Backup interrupted",
                        style = DockHeadline,
                        color = colors.attention,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "SSD disconnected at ${dropState.progress.percentage}%. Plug it back in to resume.",
                        style = DockBodySmall,
                        color = colors.textSecondary,
                        textAlign = TextAlign.Center
                    )
                }

                is DropState.Resumable -> {
                    val p = dropState.progress
                    val filesLeft = (p.totalFiles - p.copiedFiles).coerceAtLeast(0)
                    Text(
                        text = "Resume backup",
                        style = DockHeadline,
                        color = colors.dock,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = String.format(Locale.US, "%d%% done · %,d files left", p.percentage, filesLeft),
                        style = DockMono.copy(fontSize = 13.sp, color = colors.textSecondary),
                        textAlign = TextAlign.Center
                    )
                }

                is DropState.Preflight -> {
                    Text(
                        text = "Checking your SSD…",
                        style = DockHeadline,
                        color = colors.textPrimary.copy(alpha = pulseAlpha),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Testing write access",
                        style = DockBodySmall,
                        color = colors.textSecondary,
                        textAlign = TextAlign.Center
                    )
                }

                is DropState.Attention -> {
                    Text(
                        text = dropState.reason.title,
                        style = DockHeadline,
                        color = colors.attention,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = dropState.reason.message,
                        style = DockBodySmall,
                        color = colors.textSecondary,
                        textAlign = TextAlign.Center
                    )
                }

                is DropState.NoDock -> {
                    Text(
                        text = "Plug in your SSD",
                        style = DockHeadline,
                        color = colors.textPrimary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Connect your drive to start backing up",
                        style = DockBodySmall,
                        color = colors.textSecondary,
                        textAlign = TextAlign.Center
                    )
                }

                is DropState.DockDetected -> {
                    Text(
                        text = "${dropState.dock.userLabel} connected",
                        style = DockHeadline,
                        color = colors.textPrimary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${formatDropBytes(dropState.dock.freeBytes)} free · ${dropState.dock.filesystem}",
                        style = DockBodySmall,
                        color = colors.textSecondary,
                        textAlign = TextAlign.Center
                    )
                }

                is DropState.AwaitingConfirm -> {
                    Text(
                        text = "Ready to back up",
                        style = DockHeadline,
                        color = colors.textPrimary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = String.format(
                            Locale.US,
                            "%,d new or changed files · %s",
                            dropState.plannedFiles,
                            formatEta(dropState.estMinutes)
                        ),
                        style = DockBodySmall,
                        color = colors.textSecondary,
                        textAlign = TextAlign.Center
                    )
                }

                is DropState.Sealed -> {
                    Text(
                        text = "Sealed",
                        style = DockHeadline,
                        color = colors.sealText,
                        textAlign = TextAlign.Center
                    )
                }

                is DropState.Partial -> {
                    Text(
                        text = "Finished with ${dropState.skippedFiles.size} skipped",
                        style = DockHeadline,
                        color = colors.attentionText,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Folder Segments Carousel (Camera ✓ · Pictures ● · Documents)
            // Note: Completed copy segments use colors.dock until the entire Set is Sealed
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                itemsIndexed(sourceFolders) { index, folder ->
                    val currentFolderIdx = when (dropState) {
                        is DropState.Verifying -> dropState.folderIndex
                        else -> progress?.folderIndex ?: 0
                    }
                    val isDone = currentFolderIdx > index + 1 || dropState is DropState.Sealed
                    val isCurrent = currentFolderIdx == index + 1 && dropState !is DropState.Sealed

                    val chipBorderColor by animateColorAsState(
                        targetValue = when {
                            dropState is DropState.Sealed -> colors.seal
                            isDone || isCurrent -> colors.dock
                            else -> colors.outline.copy(alpha = 0.5f)
                        },
                        label = "chipBorder"
                    )

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                when {
                                    dropState is DropState.Sealed -> colors.seal.copy(alpha = 0.08f)
                                    isDone || isCurrent -> colors.dock.copy(alpha = 0.08f)
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
                                    contentDescription = "Completed",
                                    tint = if (dropState is DropState.Sealed) colors.sealText else colors.dock,
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
                                    color = when {
                                        dropState is DropState.Sealed -> colors.sealText
                                        isDone || isCurrent -> colors.dock
                                        else -> colors.textPrimary
                                    }
                                )
                            )
                        }
                    }
                }
            }

            // Expandable "Details" Disclosure Card (SCREENS.md S09)
            AnimatedVisibility(visible = showDetails) {
                Column {
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(RadiusMd))
                            .border(1.dp, colors.outline.copy(alpha = 0.5f), RoundedCornerShape(RadiusMd)),
                        colors = CardDefaults.cardColors(containerColor = colors.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "TECHNICAL DETAILS",
                                style = DockMono.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                                color = colors.textTertiary
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Speed", style = DockBodySmall, color = colors.textSecondary)
                                    Text(
                                        String.format(Locale.US, "%.1f MB/s", progress?.speedMbS ?: 0.0),
                                        style = DockMono.copy(fontWeight = FontWeight.Bold, color = colors.textPrimary)
                                    )
                                }
                                Column {
                                    Text("File rate", style = DockBodySmall, color = colors.textSecondary)
                                    Text(
                                        String.format(Locale.US, "%.1f files/s", progress?.filesPerSec ?: 0.0),
                                        style = DockMono.copy(fontWeight = FontWeight.Bold, color = colors.textPrimary)
                                    )
                                }
                                Column {
                                    Text("Verification", style = DockBodySmall, color = colors.textSecondary)
                                    Text(
                                        "SHA-256 + Re-read",
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

                            Text("Current file path:", style = DockBodySmall, color = colors.textTertiary)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = progress?.currentFilePath?.takeIf { it.isNotBlank() }
                                    ?.let { middleEllipsizePath(it, 42) }
                                    ?: "Scanning directory…",
                                style = DockMono.copy(fontSize = 12.sp),
                                color = colors.textPrimary,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Stop Confirmation Sheet (SCREENS.md S09)
    if (showStopConfirmSheet) {
        ModalBottomSheet(
            onDismissRequest = { showStopConfirmSheet = false },
            containerColor = colors.surface,
            shape = RoundedCornerShape(topStart = RadiusSheet, topEnd = RadiusSheet)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
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
                    text = "Files copied so far are kept. You can resume later.",
                    style = DockBodySmall,
                    color = colors.textSecondary,
                    textAlign = TextAlign.Center
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
                        shape = RoundedCornerShape(RadiusSm)
                    ) {
                        Text(
                            "Stop",
                            style = DockBody.copy(color = colors.danger, fontWeight = FontWeight.SemiBold)
                        )
                    }
                    Button(
                        onClick = { showStopConfirmSheet = false },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(RadiusSm),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.dock,
                            contentColor = colors.onDock
                        )
                    ) {
                        Text(
                            "Keep going",
                            style = DockBody.copy(color = colors.onDock, fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
    }
}

private fun middleEllipsizePath(path: String, maxChars: Int = 38): String {
    if (path.length <= maxChars) return path
    val lastSlash = path.lastIndexOf('/')
    if (lastSlash != -1 && lastSlash < path.length - 1) {
        val fileName = path.substring(lastSlash + 1)
        val dirPath = path.substring(0, lastSlash)
        if (fileName.length >= maxChars - 6) {
            val half = (maxChars - 3) / 2
            return "${fileName.take(half)}…${fileName.takeLast(half)}"
        }
        val remainingForDir = (maxChars - fileName.length - 2).coerceAtLeast(3)
        return "${dirPath.take(remainingForDir)}…/$fileName"
    }
    val half = (maxChars - 1) / 2
    return "${path.take(half)}…${path.takeLast(half)}"
}

private fun formatDropBytes(bytes: Long): String {
    val gb = bytes.toDouble() / (1024 * 1024 * 1024)
    return if (gb >= 1.0) {
        String.format(Locale.US, "%.1f GB", gb)
    } else {
        String.format(Locale.US, "%d MB", (bytes / (1024 * 1024)).coerceAtLeast(0))
    }
}

private fun formatEta(minutes: Double): String {
    if (minutes.isNaN() || minutes.isInfinite() || minutes <= 0.0) return "under a min left"
    val rounded = minutes.toInt().coerceAtLeast(1)
    return "about $rounded min left"
}
