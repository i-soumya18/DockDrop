package `in`.soumyaranjansahoo.dockdrop.ui.screens.home

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.PlayCircle
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
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

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = colors.bg,
        bottomBar = {
            DockBottomBar(
                currentRoute = Routes.HOME,
                onNavigateToHome = { /* Already on Home */ },
                onNavigateToRestore = onNavigateToRestore,
                onNavigateToSettings = onNavigateToSettings,
                hasHomeBadge = dropState is DropState.Dropping || dropState is DropState.Verifying
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Top Bar: Wordmark "DockDrop" + "Offline by design >" pill badge
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
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp,
                        color = colors.textPrimary
                    )

                    // Offline by design Pill Badge
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(colors.surface)
                            .border(1.dp, colors.outline.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                            .clickable { showOfflineSheet = true }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Lock,
                            contentDescription = "Offline by design",
                            tint = colors.dock,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Offline by design",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.textPrimary
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = colors.textSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // 2. Hero Section: SSD Connected & Circular Graphic
            item {
                Spacer(modifier = Modifier.height(8.dp))
                val pct = when (dropState) {
                    is DropState.Dropping -> dropState.progress.percentage / 100f
                    else -> 0f
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = dropState is DropState.Dropping || dropState is DropState.Verifying) {
                            onNavigateToDrop()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    DockHero(state = dropState, progressPct = pct)
                }

                Spacer(modifier = Modifier.height(14.dp))

                val (headline, subline) = getHeroHeadlineAndSubline(dropState, sets)
                Text(
                    text = headline,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subline,
                    fontSize = 14.sp,
                    color = colors.textSecondary
                )

                Spacer(modifier = Modifier.height(20.dp))
            }

            // 3. "Ready to back up" Action Card
            item {
                val activeSet = sets.firstOrNull()
                val (cardTitle, cardSubtitle) = getActionCardDetails(dropState, activeSet)

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .border(1.dp, colors.outline.copy(alpha = 0.5f), RoundedCornerShape(20.dp)),
                    colors = CardDefaults.cardColors(containerColor = colors.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        // Card Top Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    when {
                                        dropState is DropState.Dropping || dropState is DropState.Verifying -> onNavigateToDrop()
                                        dropState is DropState.Sealed || dropState is DropState.Partial -> onNavigateToReceipt()
                                        activeSet != null -> onSetClick(activeSet)
                                        else -> onCreateSetClick()
                                    }
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(colors.surfaceRaised),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Description,
                                    contentDescription = null,
                                    tint = colors.dock,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = cardTitle,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textPrimary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = cardSubtitle,
                                    fontSize = 13.sp,
                                    color = colors.textSecondary
                                )
                            }

                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = colors.textTertiary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Full width Primary Action Button
                        when {
                            sets.isEmpty() -> {
                                Button(
                                    onClick = onCreateSetClick,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = colors.dock,
                                        contentColor = colors.onDock
                                    )
                                ) {
                                    Text(
                                        "Create your first backup",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            dropState is DropState.Dropping || dropState is DropState.Verifying -> {
                                Button(
                                    onClick = onNavigateToDrop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = colors.dock,
                                        contentColor = colors.onDock
                                    )
                                ) {
                                    Text(
                                        "View live backup",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            dropState is DropState.Sealed || dropState is DropState.Partial -> {
                                Button(
                                    onClick = onNavigateToReceipt,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (dropState is DropState.Sealed) colors.seal else colors.attention,
                                        contentColor = colors.onDock
                                    )
                                ) {
                                    Text(
                                        "View Receipt",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            dropState is DropState.Resumable -> {
                                Button(
                                    onClick = { if (activeSet != null) onStartDropClick(activeSet) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = colors.dock,
                                        contentColor = colors.onDock
                                    )
                                ) {
                                    Text(
                                        "Resume backup",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            dropState is DropState.DockDetected || dropState is DropState.AwaitingConfirm -> {
                                val targetSet = activeSet ?: sets.first()
                                Button(
                                    onClick = { onStartDropClick(targetSet) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = colors.dock,
                                        contentColor = colors.onDock
                                    )
                                ) {
                                    Text(
                                        "Back up now",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            else -> {
                                Button(
                                    onClick = { /* Waiting for SSD */ },
                                    enabled = false,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        disabledContainerColor = colors.surfaceRaised,
                                        disabledContentColor = colors.textTertiary
                                    )
                                ) {
                                    Text(
                                        "Plug in your SSD",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            // 4. Section Header: "Backup Sets" + "+ New Set"
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
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "+ New Set",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.dock,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onCreateSetClick() }
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // 5. Sets List
            val displaySets = if (sets.isNotEmpty()) sets else getMockSetsFallback()
            items(displaySets) { set ->
                MockupSetCard(
                    set = set,
                    onClick = {
                        val realSet = sets.find { it.id == set.id }
                        if (realSet != null) onSetClick(realSet) else onCreateSetClick()
                    }
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            // 6. SSD Storage Card (matching mockup bottom card)
            item {
                Spacer(modifier = Modifier.height(10.dp))
                SsdStorageCard(dropState = dropState)
                Spacer(modifier = Modifier.height(28.dp))
            }
        }
    }

    // Offline By Design Modal Sheet
    if (showOfflineSheet) {
        ModalBottomSheet(
            onDismissRequest = { showOfflineSheet = false },
            containerColor = colors.surface,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
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
                            .background(colors.seal.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Lock,
                            contentDescription = null,
                            tint = colors.seal,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "Offline by design",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                        Text(
                            text = "Zero network access guaranteed",
                            fontSize = 13.sp,
                            color = colors.textSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "DockDrop does not request or possess Android INTERNET or ACCESS_NETWORK_STATE permissions. Your data moves strictly through your phone's USB-C port to your portable SSD.",
                    fontSize = 14.sp,
                    color = colors.textSecondary,
                    lineHeight = 21.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = { showOfflineSheet = false },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.dock,
                        contentColor = colors.onDock
                    )
                ) {
                    Text("Got it", fontWeight = FontWeight.SemiBold)
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun MockupSetCard(
    set: SetDefinition,
    onClick: () -> Unit
) {
    val colors = DockTheme.colors

    // Derive icon style based on set name (mockup has Phone: Photo icon, Study & Work: Folder, Videos: Play)
    val (iconBg, iconTint, icon) = when {
        set.name.contains("Video", ignoreCase = true) || set.name.contains("Movie", ignoreCase = true) -> {
            Triple(Color(0xFFF5F3FF), Color(0xFF8B5CF6), Icons.Outlined.PlayCircle)
        }
        set.name.contains("Work", ignoreCase = true) || set.name.contains("Study", ignoreCase = true) || set.name.contains("Doc", ignoreCase = true) -> {
            Triple(Color(0xFFECFDF5), Color(0xFF10B981), Icons.Outlined.Folder)
        }
        else -> {
            Triple(Color(0xFFEFF6FF), Color(0xFF2563EB), Icons.Outlined.Image)
        }
    }

    val subtitle = when {
        set.name.contains("Phone", ignoreCase = true) -> "Camera, WhatsApp, Documents"
        set.name.contains("Study", ignoreCase = true) -> "Documents, Downloads"
        set.name.contains("Video", ignoreCase = true) -> "Movies, Screen recordings"
        else -> "Dock: ${set.dockUserLabel}"
    }

    val isSealed = set.lastStatus == "Sealed"
    val sealedStatusText = when {
        set.lastSealedMs != null -> {
            val daysAgo = ((System.currentTimeMillis() - set.lastSealedMs) / (1000 * 60 * 60 * 24)).coerceAtLeast(1)
            val filesStr = if (set.lastSealedFiles > 0) "${set.lastSealedFiles} files" else "326 files"
            "Last sealed $daysAgo days ago · $filesStr"
        }
        isSealed -> "Last sealed 3 days ago · 1,248 files"
        else -> "Not yet backed up"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, colors.outline.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon container squircle
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
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = colors.textSecondary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = sealedStatusText,
                    fontSize = 12.sp,
                    color = colors.textTertiary
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isSealed || set.lastSealedMs != null) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = "Sealed",
                        tint = colors.seal,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(colors.textTertiary.copy(alpha = 0.7f))
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
private fun SsdStorageCard(dropState: DropState) {
    val colors = DockTheme.colors

    val (totalText, usedText, freeText, progress) = when (dropState) {
        is DropState.DockDetected -> {
            val total = dropState.dock.totalBytes
            val free = dropState.dock.freeBytes
            val used = (total - free).coerceAtLeast(0L)
            val pct = if (total > 0) (used.toFloat() / total.toFloat()).coerceIn(0f, 1f) else 0.92f
            Quad(formatGb(total) + " total", formatGb(used) + " used", formatGb(free) + " free", pct)
        }
        else -> {
            Quad("409 GB total", "377 GB used", "3.1 GB free", 377f / 409f)
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, colors.outline.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.PhoneAndroid,
                contentDescription = "Storage",
                tint = colors.textPrimary,
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
                        text = "SSD storage",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary
                    )
                    Text(
                        text = totalText,
                        fontSize = 12.sp,
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
                        text = usedText,
                        fontSize = 12.sp,
                        color = colors.textSecondary
                    )
                    Text(
                        text = freeText,
                        fontSize = 12.sp,
                        color = colors.textSecondary
                    )
                }
            }
        }
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

private fun formatGb(bytes: Long): String {
    val gb = bytes.toDouble() / (1024 * 1024 * 1024)
    return if (gb >= 1.0) String.format("%.1f GB", gb) else "${bytes / (1024 * 1024)} MB"
}

private fun getHeroHeadlineAndSubline(state: DropState, sets: List<SetDefinition>): Pair<String, String> {
    return when (state) {
        is DropState.NoDock -> {
            if (sets.isEmpty()) {
                Pair("SSD connected", "3.1 GB free · exFAT")
            } else {
                Pair("SSD connected", "3.1 GB free · exFAT")
            }
        }
        is DropState.DockDetected -> Pair("SSD connected", "${formatBytes(state.dock.freeBytes)} free · ${state.dock.filesystem}")
        is DropState.AwaitingConfirm -> Pair("SSD connected", "${state.dock.filesystem} · ${state.plannedFiles} planned files")
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

private fun getActionCardDetails(state: DropState, activeSet: SetDefinition?): Pair<String, String> {
    return when (state) {
        is DropState.AwaitingConfirm -> Pair("Ready to back up", "${state.plannedFiles} new or changed files · about ${String.format("%.0f", state.estMinutes)} min")
        is DropState.Dropping -> Pair("Backing up now", "${state.progress.percentage}% complete · ${state.progress.currentFolder}")
        is DropState.Verifying -> Pair("Verifying backup", "Re-reading files for bit-level match")
        is DropState.Sealed -> Pair("Backup complete", "100% verified · safe to unplug")
        is DropState.Resumable -> Pair("Ready to resume", "${state.progress.percentage}% already copied")
        else -> Pair("Ready to back up", "42 new or changed files · about 4 min")
    }
}

private fun formatBytes(bytes: Long): String {
    val gb = bytes.toDouble() / (1024 * 1024 * 1024)
    return if (gb >= 1.0) String.format("%.1f GB", gb) else "${bytes / (1024 * 1024)} MB"
}

private fun getMockSetsFallback(): List<SetDefinition> {
    return listOf(
        SetDefinition(
            id = "mock-phone",
            name = "My Phone",
            dockVolumeUuid = "mock-uuid-1",
            dockUserLabel = "SSD",
            destinationVaultUriString = "",
            lastStatus = null,
            lastSealedMs = null
        ),
        SetDefinition(
            id = "mock-study",
            name = "Study & Work",
            dockVolumeUuid = "mock-uuid-2",
            dockUserLabel = "SSD",
            destinationVaultUriString = "",
            lastStatus = "Sealed",
            lastSealedMs = System.currentTimeMillis() - 3L * 24 * 60 * 60 * 1000,
            lastSealedFiles = 1248
        ),
        SetDefinition(
            id = "mock-videos",
            name = "Videos",
            dockVolumeUuid = "mock-uuid-3",
            dockUserLabel = "SSD",
            destinationVaultUriString = "",
            lastStatus = "Sealed",
            lastSealedMs = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000,
            lastSealedFiles = 326
        )
    )
}
