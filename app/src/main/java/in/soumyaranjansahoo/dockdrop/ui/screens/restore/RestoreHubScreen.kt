package `in`.soumyaranjansahoo.dockdrop.ui.screens.restore

import android.net.Uri
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import `in`.soumyaranjansahoo.dockdrop.core.manifest.DiscoveredBackupSet
import `in`.soumyaranjansahoo.dockdrop.core.model.DockInfo
import `in`.soumyaranjansahoo.dockdrop.core.model.DropState
import `in`.soumyaranjansahoo.dockdrop.core.model.SetDefinition
import `in`.soumyaranjansahoo.dockdrop.core.model.SourceFolder
import `in`.soumyaranjansahoo.dockdrop.core.restore.ConflictPolicy
import `in`.soumyaranjansahoo.dockdrop.core.restore.RestoreDestination
import `in`.soumyaranjansahoo.dockdrop.core.restore.RestorePlan
import `in`.soumyaranjansahoo.dockdrop.core.restore.RestorePlanner
import `in`.soumyaranjansahoo.dockdrop.ui.components.DockHero
import `in`.soumyaranjansahoo.dockdrop.ui.components.HoldToRestoreButton
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockBody
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockBodySmall
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockHeadline
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockMono
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockTheme
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockTitle
import `in`.soumyaranjansahoo.dockdrop.ui.theme.HapticFeedbackController
import `in`.soumyaranjansahoo.dockdrop.ui.theme.RadiusMd
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

@Composable
fun RestoreHubScreen(
    currentDock: DockInfo?,
    sets: List<SetDefinition>,
    discoveredBackups: List<DiscoveredBackupSet>,
    restorePlanner: RestorePlanner,
    sourceFoldersForSet: suspend (String) -> List<SourceFolder>,
    onExecutePlan: (RestorePlan) -> Unit,
    onNavigateToBrowser: (SetDefinition) -> Unit,
    onNavigateToCustomFolderRestore: (SetDefinition) -> Unit,
    onNavigateToNewPhone: (DiscoveredBackupSet) -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToSettings: () -> Unit,
    encryptedSetManager: `in`.soumyaranjansahoo.dockdrop.core.crypto.EncryptedSetManager? = null,
    onPromptUnlock: (DiscoveredBackupSet) -> Unit = {},
    haptics: HapticFeedbackController? = null,
    modifier: Modifier = Modifier
) {
    val colors = DockTheme.colors

    val activeSet = sets.firstOrNull()
    val primaryBackup = discoveredBackups.firstOrNull()
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    var defaultPlan by remember { mutableStateOf<RestorePlan?>(null) }
    var isBuildingPlan by remember { mutableStateOf(false) }

    LaunchedEffect(currentDock, activeSet, primaryBackup) {
        if (currentDock != null && activeSet != null) {
            isBuildingPlan = true
            val folders = sourceFoldersForSet(activeSet.id)
            val mapping = folders.associate { it.label to it.treeUriString }
            try {
                val plan = restorePlanner.createPlan(
                    vaultTreeUri = Uri.parse(activeSet.destinationVaultUriString),
                    setId = activeSet.id,
                    setName = activeSet.name,
                    deviceLabel = primaryBackup?.deviceLabel ?: android.os.Build.MODEL,
                    destination = RestoreDestination.OriginalFolders(mapping),
                    conflictPolicy = ConflictPolicy.KEEP_BOTH
                )
                defaultPlan = plan
            } catch (e: Exception) {
                // ignore
            } finally {
                isBuildingPlan = false
            }
        } else {
            defaultPlan = null
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = colors.bg,
        bottomBar = {
            // Sleek bottom navigation bar per SCREENS.md §1 (Home · Restore · Settings)
            Column(modifier = Modifier.fillMaxWidth().background(colors.surface).border(1.dp, colors.outline)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Home",
                        style = DockBody.copy(color = colors.textSecondary, fontWeight = FontWeight.Normal),
                        modifier = Modifier.clickable { onNavigateToHome() }
                    )
                    Text(
                        text = "Restore",
                        style = DockBody.copy(color = colors.dock, fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Settings",
                        style = DockBody.copy(color = colors.textSecondary, fontWeight = FontWeight.Normal),
                        modifier = Modifier.clickable { onNavigateToSettings() }
                    )
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
            // 1. Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Restore",
                        style = DockTitle.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
                        color = colors.textPrimary
                    )
                }
            }

            // 2. States: No Dock / Dock without DockDrop data / Ready
            if (currentDock == null) {
                item {
                    Spacer(modifier = Modifier.height(32.dp))
                    DockHero(state = DropState.NoDock)
                    Spacer(modifier = Modifier.height(24.dp))
                    Text("Plug in your SSD", style = DockHeadline, color = colors.textPrimary)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Plug in the SSD that has your backup to restore files.",
                        style = DockBodySmall,
                        color = colors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                }
            } else if (primaryBackup == null && (activeSet == null || defaultPlan?.totalFilesSelected == 0)) {
                item {
                    Spacer(modifier = Modifier.height(32.dp))
                    DockHero(state = DropState.DockDetected(currentDock))
                    Spacer(modifier = Modifier.height(24.dp))
                    Text("No backup found", style = DockHeadline, color = colors.textPrimary)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "No DockDrop backup was found on ${currentDock.userLabel}.",
                        style = DockBodySmall,
                        color = colors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                }
            } else {
                // 3. Backup summary card
                item {
                    val dateStr = SimpleDateFormat("MMM d, yyyy", Locale.US).format(
                        Date(primaryBackup?.lastModifiedMs ?: activeSet?.lastSealedMs ?: System.currentTimeMillis())
                    )
                    val setName = activeSet?.name ?: primaryBackup?.setName ?: "Backup"
                    val fileCount = defaultPlan?.totalFilesSelected ?: primaryBackup?.generation?.entries?.size ?: 0
                    val totalBytes = defaultPlan?.totalBytesToCopy ?: primaryBackup?.generation?.entries?.values?.sumOf { it.sizeBytes } ?: 0L

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(RadiusMd))
                            .background(colors.surface)
                            .border(1.dp, colors.outline, RoundedCornerShape(RadiusMd))
                            .padding(18.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Latest backup · $setName",
                                    style = DockBody.copy(fontWeight = FontWeight.SemiBold),
                                    color = colors.textPrimary
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(colors.surfaceRaised)
                                        .border(1.dp, colors.seal, RoundedCornerShape(8.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text("SSD connected", style = DockMono.copy(color = colors.seal, fontSize = 11.sp))
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "$dateStr · $fileCount files · ${formatBytes(totalBytes)}",
                                style = DockBodySmall,
                                color = colors.textSecondary
                            )
                            if (primaryBackup != null && primaryBackup.deviceLabel != android.os.Build.MODEL) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Backup from ${primaryBackup.deviceLabel}",
                                    style = DockMono.copy(color = colors.dock, fontSize = 12.sp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }

                // 4. Hero Hold Button & 3 Info Lines (SCREENS.md S12 & DESIGN.md §5.2.5)
                item {
                    val plan = defaultPlan
                    val hasGaps = plan != null && plan.permissionGaps.isNotEmpty()
                    val isLockedEncrypted = primaryBackup != null && primaryBackup.isEncrypted &&
                            (encryptedSetManager?.isSetUnlocked(primaryBackup.setId) != true)

                    if (isLockedEncrypted) {
                        Button(
                            onClick = {
                                onPromptUnlock(primaryBackup!!)
                            },
                            modifier = Modifier.fillMaxWidth().height(64.dp),
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = colors.dock, contentColor = colors.onDock)
                        ) {
                            Text("Unlock to restore", style = DockBody.copy(fontWeight = FontWeight.Bold))
                        }
                    } else if (hasGaps) {
                        Button(
                            onClick = {
                                if (primaryBackup != null) onNavigateToNewPhone(primaryBackup)
                            },
                            modifier = Modifier.fillMaxWidth().height(64.dp),
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = colors.attention, contentColor = colors.onDock)
                        ) {
                            Text("Grant ${plan.permissionGaps.size} folders first", style = DockBody.copy(fontWeight = FontWeight.Bold))
                        }
                    } else {
                        HoldToRestoreButton(
                            text = "Hold to restore everything",
                            enabled = plan != null && plan.canExecute,
                            haptics = haptics,
                            onHoldComplete = {
                                if (plan != null) onExecutePlan(plan)
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Three mandatory info lines (SCREENS.md S12)
                    val filesCount = plan?.totalFilesToCopy ?: 0
                    val sizeFormatted = formatBytes(plan?.totalBytesToCopy ?: 0L)
                    Text(
                        text = "To original folders · Keep both if file exists · $filesCount files, $sizeFormatted",
                        style = DockBodySmall.copy(fontSize = 12.sp),
                        color = colors.textTertiary
                    )
                    Spacer(modifier = Modifier.height(28.dp))
                }

                // 5. "Or restore part of it" section
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(RadiusMd))
                            .background(colors.surface)
                            .border(1.dp, colors.outline, RoundedCornerShape(RadiusMd))
                    ) {
                        ActionRow(
                            title = "Choose folders or files",
                            subtitle = "Browse your backup tree or search by name",
                            onClick = {
                                if (activeSet != null) onNavigateToBrowser(activeSet)
                            }
                        )
                        HorizontalDivider(color = colors.outline)
                        ActionRow(
                            title = "Restore to a different folder",
                            subtitle = "Pick a single custom location on your phone",
                            onClick = {
                                if (activeSet != null) onNavigateToCustomFolderRestore(activeSet)
                            }
                        )
                        HorizontalDivider(color = colors.outline)
                        ActionRow(
                            title = "Restore to a new phone",
                            subtitle = "Map all folders to a clean install",
                            onClick = {
                                if (primaryBackup != null) onNavigateToNewPhone(primaryBackup)
                            }
                        )
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }

                // 6. Test restore (10 random files) drill row (SCREENS.md S12 line 166)
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(RadiusMd))
                            .background(colors.surfaceRaised)
                            .border(1.dp, colors.outline, RoundedCornerShape(RadiusMd))
                            .clickable {
                                if (activeSet != null) {
                                    val vaultUri = Uri.parse(activeSet.destinationVaultUriString)
                                    scope.launch {
                                        val drillPlan = restorePlanner.createPlan(
                                            vaultTreeUri = vaultUri,
                                            setId = activeSet.id,
                                            setName = activeSet.name,
                                            deviceLabel = android.os.Build.MODEL,
                                            destination = RestoreDestination.DrillSandbox,
                                            isDrill = true
                                        )
                                        onExecutePlan(drillPlan)
                                    }
                                }
                            }
                            .padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("⚡", fontSize = 18.sp, modifier = Modifier.padding(end = 12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Test restore (10 random files)", style = DockBody.copy(fontWeight = FontWeight.SemiBold), color = colors.textPrimary)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("A restore drill to verify physical reads and hashes with zero risk", style = DockBodySmall, color = colors.textSecondary)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
private fun ActionRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    val colors = DockTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = DockBody.copy(fontWeight = FontWeight.Medium), color = colors.textPrimary)
            Spacer(modifier = Modifier.height(2.dp))
            Text(subtitle, style = DockBodySmall, color = colors.textSecondary)
        }
        Text("→", style = DockTitle, color = colors.textTertiary)
    }
}

private fun formatBytes(bytes: Long): String {
    val gb = bytes.toDouble() / (1024 * 1024 * 1024)
    return if (gb >= 1.0) String.format("%.1f GB", gb) else "${bytes / (1024 * 1024)} MB"
}
