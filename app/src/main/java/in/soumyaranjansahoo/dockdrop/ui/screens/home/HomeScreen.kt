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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.soumyaranjansahoo.dockdrop.core.model.DropState
import `in`.soumyaranjansahoo.dockdrop.core.model.SetDefinition
import `in`.soumyaranjansahoo.dockdrop.ui.components.DockHero
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockBody
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockBodySmall
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockHeadline
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockMono
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockTheme
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockTitle
import `in`.soumyaranjansahoo.dockdrop.ui.theme.RadiusMd

@Composable
fun HomeScreen(
    dropState: DropState,
    sets: List<SetDefinition>,
    onCreateSetClick: () -> Unit,
    onStartDropClick: (SetDefinition) -> Unit,
    onNavigateToDrop: () -> Unit,
    onNavigateToReceipt: () -> Unit,
    onOpenKeepAliveClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val colors = DockTheme.colors

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = colors.bg,
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 20.dp)
            ) {
                when {
                    sets.isEmpty() -> {
                        Button(
                            onClick = onCreateSetClick,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.dock,
                                contentColor = colors.onDock
                            )
                        ) {
                            Text("Create your first backup", style = DockBody.copy(fontWeight = FontWeight.SemiBold))
                        }
                    }

                    dropState is DropState.Dropping || dropState is DropState.Verifying -> {
                        Button(
                            onClick = onNavigateToDrop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.dock,
                                contentColor = colors.onDock
                            )
                        ) {
                            Text("View live backup", style = DockBody.copy(fontWeight = FontWeight.SemiBold))
                        }
                    }

                    dropState is DropState.Sealed || dropState is DropState.Partial -> {
                        Button(
                            onClick = onNavigateToReceipt,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (dropState is DropState.Sealed) colors.seal else colors.attention,
                                contentColor = colors.onDock
                            )
                        ) {
                            Text("View Receipt", style = DockBody.copy(fontWeight = FontWeight.SemiBold))
                        }
                    }

                    dropState is DropState.DockDetected || dropState is DropState.AwaitingConfirm -> {
                        val activeSet = sets.first()
                        Button(
                            onClick = { onStartDropClick(activeSet) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.dock,
                                contentColor = colors.onDock
                            )
                        ) {
                            Text("Back up now", style = DockBody.copy(fontWeight = FontWeight.SemiBold))
                        }
                    }

                    else -> {
                        Button(
                            onClick = { /* disabled */ },
                            enabled = false,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                disabledContainerColor = colors.surfaceRaised,
                                disabledContentColor = colors.textTertiary
                            )
                        ) {
                            Text("Plug in your SSD", style = DockBody.copy(fontWeight = FontWeight.SemiBold))
                        }
                    }
                }
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
            // 1. Top bar: Wordmark "DockDrop" left
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "DockDrop",
                        style = DockTitle.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
                        color = colors.textPrimary
                    )
                    Text(
                        text = "Keep running ⚡",
                        style = DockBodySmall.copy(color = colors.dock, fontWeight = FontWeight.Medium),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onOpenKeepAliveClick() }
                            .padding(4.dp)
                    )
                }
            }

            // 2. Hero Dock (~40% height, centered)
            item {
                Spacer(modifier = Modifier.height(16.dp))
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

                Spacer(modifier = Modifier.height(16.dp))

                // State Headline and Sub-line (SCREENS.md §3)
                val (headline, subline) = getHeadlineAndSubline(dropState, sets)
                Text(text = headline, style = DockHeadline, color = colors.textPrimary)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = subline, style = DockBodySmall, color = colors.textSecondary)

                Spacer(modifier = Modifier.height(32.dp))
            }

            // 3. Resume / Attention Card if applicable
            if (dropState is DropState.Resumable) {
                item {
                    AttentionCard(
                        title = "Resume backup?",
                        subtitle = "${dropState.progress.percentage}% done · tap to continue",
                        buttonText = "Resume",
                        onClick = { if (sets.isNotEmpty()) onStartDropClick(sets.first()) }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // 4. Set cards
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Backup Sets", style = DockBody.copy(fontWeight = FontWeight.SemiBold), color = colors.textSecondary)
                    if (sets.isNotEmpty()) {
                        Text(
                            text = "+ New Set",
                            style = DockBodySmall.copy(fontWeight = FontWeight.Medium),
                            color = colors.dock,
                            modifier = Modifier.clickable { onCreateSetClick() }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            items(sets) { set ->
                SetCard(set = set, onClick = {})
                Spacer(modifier = Modifier.height(12.dp))
            }

            // 5. Footer microcopy: "Offline by design"
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "🔒 Offline by design · No network access",
                        style = DockMono.copy(fontSize = 11.sp),
                        color = colors.textTertiary
                    )
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun SetCard(set: SetDefinition, onClick: () -> Unit) {
    val colors = DockTheme.colors
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(RadiusMd))
            .border(1.dp, colors.outline, RoundedCornerShape(RadiusMd))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = colors.surface)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = set.name, style = DockTitle, color = colors.textPrimary)

                // Status dot: Green ONLY if last sealed (CLAUDE.md Rule 2)
                val dotColor = if (set.lastStatus == "Sealed") colors.seal else colors.textTertiary
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(dotColor)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Dock: ${set.dockUserLabel}",
                style = DockBodySmall,
                color = colors.textSecondary
            )

            Spacer(modifier = Modifier.height(8.dp))
            val sealedInfo = if (set.lastSealedMs != null) {
                "Last sealed · ${set.lastSealedFiles} files"
            } else {
                "Not yet backed up"
            }
            Text(text = sealedInfo, style = DockMono.copy(fontSize = 12.sp), color = colors.textTertiary)
        }
    }
}

@Composable
private fun AttentionCard(title: String, subtitle: String, buttonText: String, onClick: () -> Unit) {
    val colors = DockTheme.colors
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(RadiusMd))
            .border(1.dp, colors.attention.copy(alpha = 0.5f), RoundedCornerShape(RadiusMd)),
        colors = CardDefaults.cardColors(containerColor = colors.surfaceRaised)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = DockBody.copy(fontWeight = FontWeight.Bold), color = colors.attention)
                Text(text = subtitle, style = DockBodySmall, color = colors.textSecondary)
            }
            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(containerColor = colors.dock, contentColor = colors.onDock),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(buttonText, style = DockBodySmall.copy(fontWeight = FontWeight.SemiBold))
            }
        }
    }
}

private fun getHeadlineAndSubline(state: DropState, sets: List<SetDefinition>): Pair<String, String> {
    return when (state) {
        is DropState.NoDock -> {
            if (sets.isEmpty()) {
                Pair("Back up your phone to an SSD you own", "Plug in a USB-C drive to begin")
            } else {
                Pair("Plug in your SSD", "Last sealed ${sets.first().lastSealedFiles} files")
            }
        }
        is DropState.DockDetected -> Pair("${state.dock.userLabel} connected", "${formatBytes(state.dock.freeBytes)} free · ${state.dock.filesystem}")
        is DropState.AwaitingConfirm -> Pair("Ready to back up", "${state.plannedFiles} new or changed files · about ${String.format("%.1f", state.estMinutes)} min")
        is DropState.Preflight -> Pair("Checking your SSD…", "Testing write access")
        is DropState.Dropping -> Pair("Backing up ${state.progress.currentFolder}", "${state.progress.folderIndex} of ${state.progress.totalFolders} folders · about ${String.format("%.1f", state.progress.estMinutesRemaining)} min left")
        is DropState.Verifying -> Pair("Verifying ${state.currentFolder}", "${state.folderIndex} of ${state.totalFolders} folders")
        is DropState.Sealed -> Pair("Sealed", "${state.receipt.totalFilesVerified} files verified · nothing deleted from your phone")
        is DropState.Partial -> Pair("Finished with ${state.skippedFiles.size} skipped", "${state.skippedFiles.size} files couldn't be read")
        is DropState.Paused -> Pair("Paused", state.reason.message)
        is DropState.Interrupted -> Pair("Backup interrupted", "SSD was disconnected at ${state.progress.percentage}%")
        is DropState.Resumable -> Pair("Resume backup", "${state.progress.percentage}% done · ${state.progress.totalFiles - state.progress.copiedFiles} files left")
        is DropState.Attention -> Pair(state.reason.title, state.reason.message)
    }
}

private fun formatBytes(bytes: Long): String {
    val gb = bytes.toDouble() / (1024 * 1024 * 1024)
    return if (gb >= 1.0) String.format("%.1f GB", gb) else "${bytes / (1024 * 1024)} MB"
}
