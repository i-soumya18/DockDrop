package `in`.soumyaranjansahoo.dockdrop.ui.screens.home

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Usb
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.soumyaranjansahoo.dockdrop.core.model.DropState
import `in`.soumyaranjansahoo.dockdrop.core.model.SetDefinition
import `in`.soumyaranjansahoo.dockdrop.ui.components.DockBottomBar
import `in`.soumyaranjansahoo.dockdrop.ui.components.DockHero
import `in`.soumyaranjansahoo.dockdrop.ui.navigation.Routes
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockBody
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockBodySmall
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockHeadline
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockMono
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockTheme
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockTitle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    dropState: DropState,
    sets: List<SetDefinition>,
    onCreateSetClick: () -> Unit,
    onStartDropClick: (SetDefinition) -> Unit,
    onNavigateToDrop: () -> Unit,
    onNavigateToReceipt: () -> Unit,
    onNavigateToRestore: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToHistory: () -> Unit = {},
    onOpenKeepAliveClick: () -> Unit = {},
    onSetClick: (SetDefinition) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val colors = DockTheme.colors
    var showOfflineSheet by remember { mutableStateOf(false) }
    val activeSet = sets.firstOrNull()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = colors.bg,
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.bg)
            ) {
                // Bottom-anchored 56dp Primary CTA
                HomeBottomCta(
                    dropState = dropState,
                    sets = sets,
                    activeSet = activeSet,
                    onCreateSetClick = onCreateSetClick,
                    onStartDropClick = onStartDropClick,
                    onNavigateToDrop = onNavigateToDrop,
                    onNavigateToReceipt = onNavigateToReceipt,
                    onOpenKeepAliveClick = onOpenKeepAliveClick
                )

                // Expressive Navigation Bar
                DockBottomBar(
                    currentRoute = Routes.HOME,
                    onNavigateToHome = { /* Current destination */ },
                    onNavigateToRestore = onNavigateToRestore,
                    onNavigateToSettings = onNavigateToSettings,
                    hasHomeBadge = dropState is DropState.Dropping || dropState is DropState.Verifying
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Top Bar: Wordmark + Accessible 100% Offline Trust Pill
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "DockDrop",
                        style = DockHeadline.copy(fontSize = 26.sp, letterSpacing = (-0.5).sp),
                        color = colors.textPrimary
                    )

                    // 100% Offline Trust Badge with >= 48dp interactive touch target
                    Box(
                        modifier = Modifier
                            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                            .semantics {
                                role = Role.Button
                                contentDescription = "100% Offline guaranteed. Zero network permissions. Tap for verification details."
                            }
                            .clickable { showOfflineSheet = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(colors.badgeBg)
                                .border(1.dp, colors.badgeBorder, RoundedCornerShape(20.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Shield,
                                contentDescription = null,
                                tint = colors.badgeContent,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "100% Offline",
                                style = DockBodySmall.copy(fontSize = 12.sp, fontWeight = FontWeight.SemiBold),
                                color = colors.badgeContent
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = colors.badgeContent.copy(alpha = 0.7f),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            // 2. Hero Section: SSD Connected & Tactile Circular Graphic (~40% viewport)
            item {
                Spacer(modifier = Modifier.height(8.dp))
                val pct = when (dropState) {
                    is DropState.Dropping -> dropState.progress.percentage / 100f
                    else -> 0f
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .clickable(enabled = dropState is DropState.Dropping || dropState is DropState.Verifying) {
                            onNavigateToDrop()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    DockHero(state = dropState, progressPct = pct, modifier = Modifier.size(160.dp))
                }

                Spacer(modifier = Modifier.height(14.dp))

                val (headline, subline) = getHeroHeadlineAndSubline(dropState, sets)
                Text(
                    text = headline,
                    style = DockHeadline.copy(fontSize = 24.sp),
                    color = colors.textPrimary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subline,
                    style = DockBodySmall.copy(fontFeatureSettings = "tnum"),
                    color = colors.textSecondary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))
            }

            // 3. Attention / Resume Card (SCREENS.md S03 §3: ONLY shown if applicable)
            if (dropState is DropState.Resumable || dropState is DropState.Attention || dropState is DropState.Interrupted || dropState is DropState.Partial) {
                item {
                    HomeAttentionCard(
                        dropState = dropState,
                        activeSet = activeSet,
                        onStartDropClick = onStartDropClick,
                        onNavigateToReceipt = onNavigateToReceipt,
                        onOpenKeepAliveClick = onOpenKeepAliveClick
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }

            // 4. Section Header: "Backup Sets" + "+ New Set" Action
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Backup Sets",
                        style = DockTitle,
                        color = colors.textPrimary
                    )

                    Box(
                        modifier = Modifier
                            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                            .semantics {
                                role = Role.Button
                                contentDescription = "Create a new backup set"
                            }
                            .clickable { onCreateSetClick() },
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        Text(
                            text = "+ New Set",
                            style = DockBody.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
                            color = colors.dock,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // 5. Sets List
            if (sets.isEmpty()) {
                item {
                    EmptySetsCard(onCreateSetClick = onCreateSetClick)
                    Spacer(modifier = Modifier.height(14.dp))
                }
            } else {
                items(sets, key = { it.id }) { set ->
                    RealSetCard(
                        set = set,
                        onClick = { onSetClick(set) }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }

            // 6. SSD Storage Hardware Card
            item {
                Spacer(modifier = Modifier.height(8.dp))
                SsdStorageCard(dropState = dropState)
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    // Modal Offline By Design Verification Sheet
    if (showOfflineSheet) {
        OfflineByDesignModalSheet(onDismiss = { showOfflineSheet = false })
    }
}

@Composable
private fun HomeBottomCta(
    dropState: DropState,
    sets: List<SetDefinition>,
    activeSet: SetDefinition?,
    onCreateSetClick: () -> Unit,
    onStartDropClick: (SetDefinition) -> Unit,
    onNavigateToDrop: () -> Unit,
    onNavigateToReceipt: () -> Unit,
    onOpenKeepAliveClick: () -> Unit
) {
    val colors = DockTheme.colors
    val targetSet = activeSet ?: sets.firstOrNull()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when {
            // First run, no set created yet
            sets.isEmpty() -> {
                Button(
                    onClick = onCreateSetClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .semantics {
                            role = Role.Button
                            contentDescription = "Create your first backup set"
                        },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.dock,
                        contentColor = colors.onDock
                    )
                ) {
                    Text(
                        text = "Create your first backup",
                        style = DockBody.copy(fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Safeguard your photos, videos, and documents",
                    style = DockBodySmall,
                    color = colors.textTertiary
                )
            }

            // In-flight backup
            dropState is DropState.Dropping || dropState is DropState.Verifying -> {
                Button(
                    onClick = onNavigateToDrop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .semantics {
                            role = Role.Button
                            contentDescription = "View live backup progress"
                        },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.dock,
                        contentColor = colors.onDock
                    )
                ) {
                    Text(
                        text = "View live backup",
                        style = DockBody.copy(fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Backup running in background · Safe to lock phone",
                    style = DockBodySmall,
                    color = colors.textTertiary
                )
            }

            // Sealed backup
            dropState is DropState.Sealed -> {
                Button(
                    onClick = onNavigateToReceipt,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .semantics {
                            role = Role.Button
                            contentDescription = "View sealed backup receipt"
                        },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.surfaceRaised,
                        contentColor = colors.textPrimary
                    )
                ) {
                    Icon(imageVector = Icons.Filled.CheckCircle, contentDescription = null, tint = colors.seal, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "View Receipt",
                        style = DockBody.copy(fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Last backup 100% verified · Safe to unplug",
                    style = DockBodySmall,
                    color = colors.textTertiary
                )
            }

            // Partial backup (amber earned status, never green)
            dropState is DropState.Partial -> {
                Button(
                    onClick = onNavigateToReceipt,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .semantics {
                            role = Role.Button
                            contentDescription = "Review skipped files in receipt"
                        },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.attention,
                        contentColor = colors.onDock
                    )
                ) {
                    Icon(imageVector = Icons.Outlined.Warning, contentDescription = null, tint = colors.onDock, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Review skipped files",
                        style = DockBody.copy(fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Backup completed with skipped files",
                    style = DockBodySmall,
                    color = colors.textTertiary
                )
            }

            // Resumable backup
            dropState is DropState.Resumable -> {
                Button(
                    onClick = { if (targetSet != null) onStartDropClick(targetSet) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .semantics {
                            role = Role.Button
                            contentDescription = "Resume backup of ${targetSet?.name ?: "set"}"
                        },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.dock,
                        contentColor = colors.onDock
                    )
                ) {
                    Icon(imageVector = Icons.Outlined.PlayArrow, contentDescription = null, tint = colors.onDock, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Resume backup",
                        style = DockBody.copy(fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${dropState.progress.percentage}% already copied",
                    style = DockBodySmall.copy(fontFeatureSettings = "tnum"),
                    color = colors.textTertiary
                )
            }

            // SSD Connected & Ready
            dropState is DropState.DockDetected || dropState is DropState.AwaitingConfirm -> {
                Button(
                    onClick = { if (targetSet != null) onStartDropClick(targetSet) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .semantics {
                            role = Role.Button
                            contentDescription = "Start backup of ${targetSet?.name ?: "set"} now"
                        },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.dock,
                        contentColor = colors.onDock
                    )
                ) {
                    Icon(imageVector = Icons.Outlined.PlayArrow, contentDescription = null, tint = colors.onDock, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Back up now",
                        style = DockBody.copy(fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                val dockLabel = when (dropState) {
                    is DropState.DockDetected -> dropState.dock.userLabel
                    is DropState.AwaitingConfirm -> dropState.dock.userLabel
                    else -> "SSD"
                }
                Text(
                    text = "Target: $dockLabel · Ready for transfer",
                    style = DockBodySmall,
                    color = colors.textTertiary
                )
            }

            // No SSD connected (Eliminating line 374 no-op: Interactive keep-alive guidance)
            else -> {
                Button(
                    onClick = onOpenKeepAliveClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .border(1.dp, colors.outline.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                        .semantics {
                            role = Role.Button
                            contentDescription = "SSD not connected. Connect your portable SSD via USB-C to back up, or tap for connection tips and background settings."
                        },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.surfaceRaised,
                        contentColor = colors.textPrimary
                    )
                ) {
                    Icon(imageVector = Icons.Outlined.Usb, contentDescription = null, tint = colors.dock, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Plug in your SSD",
                        style = DockBody.copy(fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Connect via USB-C to back up · Tap for help",
                    style = DockBodySmall,
                    color = colors.textTertiary
                )
            }
        }
    }
}

@Composable
private fun RealSetCard(
    set: SetDefinition,
    onClick: () -> Unit
) {
    val colors = DockTheme.colors

    // Semantic category colors from DockColors (zero raw hexes)
    val (iconBg, iconTint, icon) = when {
        set.isEncrypted -> {
            Triple(
                colors.dock.copy(alpha = 0.12f),
                colors.dock,
                Icons.Outlined.Lock
            )
        }
        set.name.contains("Video", ignoreCase = true) || set.name.contains("Movie", ignoreCase = true) -> {
            Triple(
                colors.categoryVideo.copy(alpha = if (colors.isDark) 0.18f else 0.12f),
                colors.categoryVideo,
                Icons.Outlined.PlayCircle
            )
        }
        set.name.contains("Work", ignoreCase = true) || set.name.contains("Study", ignoreCase = true) || set.name.contains("Doc", ignoreCase = true) -> {
            Triple(
                colors.categoryDocument.copy(alpha = if (colors.isDark) 0.18f else 0.12f),
                colors.categoryDocument,
                Icons.Outlined.Folder
            )
        }
        set.name.contains("Audio", ignoreCase = true) || set.name.contains("Music", ignoreCase = true) -> {
            Triple(
                colors.categoryAudio.copy(alpha = if (colors.isDark) 0.18f else 0.12f),
                colors.categoryAudio,
                Icons.Outlined.PlayCircle
            )
        }
        else -> {
            Triple(
                colors.categoryPhoto.copy(alpha = if (colors.isDark) 0.18f else 0.12f),
                colors.categoryPhoto,
                Icons.Outlined.Image
            )
        }
    }

    val subtitle = if (set.dockUserLabel.isNotBlank()) "Target: ${set.dockUserLabel}" else "External SSD vault"

    // Strict status & earned seal calculation
    val isSealed = set.lastStatus == "Sealed"
    val isPartial = set.lastStatus == "Partial"
    val isInterrupted = set.lastStatus == "Interrupted"

    val (sealedStatusText, statusDotColor, isEarnedGreen) = when {
        isPartial -> {
            val fileDetail = if (set.lastSealedFiles > 0) " · ${set.lastSealedFiles} verified" else ""
            Triple("Finished with skipped files$fileDetail", colors.attention, false)
        }
        isInterrupted -> {
            Triple("Backup interrupted", colors.attention, false)
        }
        isSealed -> {
            val timeAgo = if (set.lastSealedMs != null) {
                val daysAgo = ((System.currentTimeMillis() - set.lastSealedMs) / (1000 * 60 * 60 * 24)).coerceAtLeast(0)
                if (daysAgo == 0L) "today" else "$daysAgo days ago"
            } else "recently"
            val filesStr = if (set.lastSealedFiles > 0) " · ${set.lastSealedFiles} files" else ""
            Triple("Last sealed $timeAgo$filesStr", colors.seal, true)
        }
        set.lastSealedMs != null -> {
            // Backward-compatible fallback only when lastStatus was null
            val daysAgo = ((System.currentTimeMillis() - set.lastSealedMs) / (1000 * 60 * 60 * 24)).coerceAtLeast(0)
            val timeAgo = if (daysAgo == 0L) "today" else "$daysAgo days ago"
            val filesStr = if (set.lastSealedFiles > 0) " · ${set.lastSealedFiles} files" else ""
            Triple("Last sealed $timeAgo$filesStr", colors.seal, true)
        }
        else -> {
            Triple("Not yet backed up", colors.textTertiary, false)
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, colors.outline.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .semantics(mergeDescendants = true) {
                role = Role.Button
                contentDescription = "Backup set ${set.name}, $subtitle, $sealedStatusText. Tap to view details."
            }
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = set.name,
                    style = DockBody.copy(fontWeight = FontWeight.Bold, fontSize = 15.sp),
                    color = colors.textPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = DockBodySmall.copy(fontSize = 13.sp),
                    color = colors.textSecondary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = sealedStatusText,
                    style = DockBodySmall.copy(fontSize = 12.sp, fontFeatureSettings = "tnum"),
                    color = if (isPartial || isInterrupted) colors.attention else colors.textTertiary
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isEarnedGreen) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = "Sealed and verified",
                        tint = colors.seal,
                        modifier = Modifier.size(20.dp)
                    )
                } else if (isPartial || isInterrupted) {
                    Icon(
                        imageVector = Icons.Outlined.Warning,
                        contentDescription = if (isPartial) "Finished with skipped files" else "Backup interrupted",
                        tint = colors.attention,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(statusDotColor.copy(alpha = 0.6f))
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = colors.textTertiary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun HomeAttentionCard(
    dropState: DropState,
    activeSet: SetDefinition?,
    onStartDropClick: (SetDefinition) -> Unit,
    onNavigateToReceipt: () -> Unit,
    onOpenKeepAliveClick: () -> Unit
) {
    val colors = DockTheme.colors

    val (accentColor, title, message, actionLabel, onAction) = when (dropState) {
        is DropState.Resumable -> {
            Tuple5(
                colors.dock,
                "Resume backup",
                "${dropState.progress.percentage}% completed · ${dropState.progress.totalFiles - dropState.progress.copiedFiles} files remaining",
                "Resume",
                { if (activeSet != null) onStartDropClick(activeSet) }
            )
        }
        is DropState.Interrupted -> {
            Tuple5(
                colors.attention,
                "Backup interrupted",
                "SSD disconnected at ${dropState.progress.percentage}%. Reconnect drive to continue.",
                "Check connection",
                onOpenKeepAliveClick
            )
        }
        is DropState.Partial -> {
            Tuple5(
                colors.attention,
                "Finished with ${dropState.skippedFiles.size} skipped",
                "Some files could not be read. Tap to review skipped items.",
                "Review",
                onNavigateToReceipt
            )
        }
        is DropState.Attention -> {
            Tuple5(
                colors.attention,
                dropState.reason.title,
                dropState.reason.message,
                "Review",
                onOpenKeepAliveClick
            )
        }
        else -> return
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, colors.outline.copy(alpha = 0.6f), RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Amber / Dock leading vertical bar
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .defaultMinSize(minHeight = 72.dp)
                    .background(accentColor)
            )

            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = DockBody.copy(fontWeight = FontWeight.Bold, fontSize = 15.sp),
                        color = colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = message,
                        style = DockBodySmall.copy(fontFeatureSettings = "tnum"),
                        color = colors.textSecondary
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = onAction,
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentColor,
                        contentColor = colors.onDock
                    )
                ) {
                    Text(
                        text = actionLabel,
                        style = DockBodySmall.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}

private data class Tuple5<A, B, C, D, E>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
    val fifth: E
)

@Composable
private fun SsdStorageCard(dropState: DropState) {
    val colors = DockTheme.colors

    val dockInfo = when (dropState) {
        is DropState.DockDetected -> dropState.dock
        is DropState.AwaitingConfirm -> dropState.dock
        else -> null
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, colors.outline.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        if (dockInfo != null) {
            val total = dockInfo.totalBytes
            val free = dockInfo.freeBytes
            val used = (total - free).coerceAtLeast(0L)
            val progress = if (total > 0) (used.toFloat() / total.toFloat()).coerceIn(0f, 1f) else 0f

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Storage,
                    contentDescription = "SSD Storage",
                    tint = colors.dock,
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${dockInfo.userLabel} (${dockInfo.filesystem})",
                            style = DockBody.copy(fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
                            color = colors.textPrimary
                        )
                        Text(
                            text = "${formatGb(total)} total",
                            style = DockMono.copy(fontSize = 12.sp),
                            color = colors.textSecondary
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(7.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = colors.dock,
                        trackColor = colors.surfaceRaised,
                        strokeCap = StrokeCap.Round
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${formatGb(used)} used",
                            style = DockMono.copy(fontSize = 12.sp),
                            color = colors.textSecondary
                        )
                        Text(
                            text = "${formatGb(free)} available",
                            style = DockMono.copy(fontSize = 12.sp),
                            color = colors.textSecondary
                        )
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.surfaceRaised),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Storage,
                        contentDescription = "Storage",
                        tint = colors.textSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "SSD storage",
                        style = DockBody.copy(fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
                        color = colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Plug in your USB-C portable SSD to view available capacity and start backing up.",
                        style = DockBodySmall.copy(fontSize = 12.sp),
                        color = colors.textSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptySetsCard(
    onCreateSetClick: () -> Unit
) {
    val colors = DockTheme.colors

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .border(1.dp, colors.outline.copy(alpha = 0.5f), RoundedCornerShape(18.dp)),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(colors.dock.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Folder,
                    contentDescription = null,
                    tint = colors.dock,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "No backup sets yet",
                style = DockBody.copy(fontWeight = FontWeight.Bold, fontSize = 16.sp),
                color = colors.textPrimary
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Choose folders like Camera, Downloads, or Documents to safeguard on your external SSD.",
                style = DockBodySmall,
                color = colors.textSecondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onCreateSetClick,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.dock,
                    contentColor = colors.onDock
                ),
                modifier = Modifier.defaultMinSize(minHeight = 48.dp)
            ) {
                Text(
                    text = "+ Create Backup Set",
                    style = DockBody.copy(fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OfflineByDesignModalSheet(onDismiss: () -> Unit) {
    val colors = DockTheme.colors

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(colors.badgeBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Shield,
                        contentDescription = null,
                        tint = colors.badgeContent,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = "Offline by design",
                        style = DockTitle,
                        color = colors.textPrimary
                    )
                    Text(
                        text = "Zero network access guaranteed",
                        style = DockBodySmall,
                        color = colors.badgeContent
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "DockDrop does not request or possess Android INTERNET or ACCESS_NETWORK_STATE permissions. Your data moves strictly through your phone's USB-C port to your portable SSD.",
                style = DockBody.copy(fontSize = 14.sp, lineHeight = 21.sp),
                color = colors.textSecondary
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .defaultMinSize(minHeight = 48.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.dock,
                    contentColor = colors.onDock
                )
            ) {
                Text("Got it", style = DockBody.copy(fontWeight = FontWeight.Bold))
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

private fun formatGb(bytes: Long): String {
    val gb = bytes.toDouble() / (1024 * 1024 * 1024)
    return if (gb >= 1.0) String.format("%.1f GB", gb) else "${bytes / (1024 * 1024)} MB"
}

private fun formatBytes(bytes: Long): String {
    val gb = bytes.toDouble() / (1024 * 1024 * 1024)
    return if (gb >= 1.0) String.format("%.1f GB", gb) else "${bytes / (1024 * 1024)} MB"
}

private fun getHeroHeadlineAndSubline(state: DropState, sets: List<SetDefinition>): Pair<String, String> {
    return when (state) {
        is DropState.NoDock -> {
            if (sets.isEmpty()) {
                Pair("Welcome to DockDrop", "Zero cloud, zero internet — physical SSD backup")
            } else {
                Pair("Plug in your SSD", "Connect drive via USB-C to start backup")
            }
        }
        is DropState.DockDetected -> Pair("${state.dock.userLabel} connected", "${formatBytes(state.dock.freeBytes)} available · ${state.dock.filesystem}")
        is DropState.AwaitingConfirm -> Pair("${state.dock.userLabel} connected", "${state.dock.filesystem} · ${state.plannedFiles} planned files")
        is DropState.Preflight -> Pair("Checking your SSD…", "Testing write access")
        is DropState.Dropping -> Pair("Backing up ${state.progress.currentFolder}", "${state.progress.percentage}% · ${state.progress.folderIndex} of ${state.progress.totalFolders}")
        is DropState.Verifying -> Pair("Verifying ${state.currentFolder}", "${state.folderIndex} of ${state.totalFolders} folders")
        is DropState.Sealed -> Pair("Backup sealed", "100% verified · nothing deleted from phone")
        is DropState.Partial -> Pair("Finished with ${state.skippedFiles.size} skipped", "Files couldn't be read")
        is DropState.Paused -> Pair("Paused", state.reason.message)
        is DropState.Interrupted -> Pair("Backup interrupted", "SSD disconnected at ${state.progress.percentage}%")
        is DropState.Resumable -> Pair("Resume backup", "${state.progress.percentage}% done")
        is DropState.Attention -> Pair(state.reason.title, state.reason.message)
    }
}
