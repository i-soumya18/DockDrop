package `in`.soumyaranjansahoo.dockdrop.ui.screens.restore

import android.net.Uri
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos
import androidx.compose.material.icons.automirrored.outlined.DriveFileMove
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Usb
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.soumyaranjansahoo.dockdrop.core.crypto.EncryptedSetManager
import `in`.soumyaranjansahoo.dockdrop.core.manifest.DiscoveredBackupSet
import `in`.soumyaranjansahoo.dockdrop.core.model.DockInfo
import `in`.soumyaranjansahoo.dockdrop.core.model.DropState
import `in`.soumyaranjansahoo.dockdrop.core.model.SetDefinition
import `in`.soumyaranjansahoo.dockdrop.core.model.SourceFolder
import `in`.soumyaranjansahoo.dockdrop.core.restore.ConflictPolicy
import `in`.soumyaranjansahoo.dockdrop.core.restore.RestoreDestination
import `in`.soumyaranjansahoo.dockdrop.core.restore.RestorePlan
import `in`.soumyaranjansahoo.dockdrop.core.restore.RestorePlanner
import `in`.soumyaranjansahoo.dockdrop.ui.components.DockBottomBar
import `in`.soumyaranjansahoo.dockdrop.ui.components.DockEmptyState
import `in`.soumyaranjansahoo.dockdrop.ui.components.DockHero
import `in`.soumyaranjansahoo.dockdrop.ui.components.HoldToRestoreButton
import `in`.soumyaranjansahoo.dockdrop.ui.navigation.Routes
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockBody
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockBodySmall
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockHeadline
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockMono
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockTheme
import `in`.soumyaranjansahoo.dockdrop.ui.theme.HapticFeedbackController
import `in`.soumyaranjansahoo.dockdrop.ui.theme.LocalReducedMotion
import `in`.soumyaranjansahoo.dockdrop.ui.theme.RadiusMd
import `in`.soumyaranjansahoo.dockdrop.ui.theme.RadiusSm
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    encryptedSetManager: EncryptedSetManager? = null,
    onPromptUnlock: (DiscoveredBackupSet) -> Unit = {},
    haptics: HapticFeedbackController? = null,
    modifier: Modifier = Modifier
) {
    val colors = DockTheme.colors
    val isReducedMotion = LocalReducedMotion.current

    val activeSet = sets.firstOrNull()
    val primaryBackup = discoveredBackups.firstOrNull()
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    var defaultPlan by remember { mutableStateOf<RestorePlan?>(null) }
    var isBuildingPlan by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "restoreHubBreathe")
    val animatedBreatheAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breatheAlpha"
    )
    val breatheAlpha = if (isReducedMotion) 0.8f else animatedBreatheAlpha

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
            DockBottomBar(
                currentRoute = Routes.RESTORE_HUB,
                onNavigateToHome = onNavigateToHome,
                onNavigateToRestore = {},
                onNavigateToSettings = onNavigateToSettings
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
            // Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(top = 16.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Restore",
                        style = DockHeadline,
                        color = colors.textPrimary
                    )
                }
            }

            // States: No Dock / Dock without DockDrop data / Ready (SCREENS.md S12)
            if (currentDock == null) {
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    DockHero(state = DropState.NoDock)
                    Spacer(modifier = Modifier.height(20.dp))
                    DockEmptyState(
                        title = "Plug in the SSD that has your backup",
                        description = "Connect your drive to browse or restore your photos, videos, and documents.",
                        icon = Icons.Outlined.Usb
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                }
            } else if (!isBuildingPlan && primaryBackup == null && (activeSet == null || defaultPlan?.totalFilesSelected == 0)) {
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    DockHero(state = DropState.DockDetected(currentDock))
                    Spacer(modifier = Modifier.height(20.dp))
                    DockEmptyState(
                        title = "No DockDrop backup found on this drive",
                        description = "No backup manifest was found on ${currentDock.userLabel}. Run a backup from Home first.",
                        icon = Icons.Outlined.Storage
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                }
            } else {
                // Backup Summary Card (SCREENS.md S12)
                item {
                    val dateStr = SimpleDateFormat("MMM d, yyyy", Locale.US).format(
                        Date(primaryBackup?.lastModifiedMs ?: activeSet?.lastSealedMs ?: System.currentTimeMillis())
                    )
                    val setName = activeSet?.name ?: primaryBackup?.setName ?: "Backup"
                    val fileCount = defaultPlan?.totalFilesSelected ?: primaryBackup?.generation?.entries?.size ?: 0
                    val totalBytes = defaultPlan?.totalBytesToCopy
                        ?: primaryBackup?.generation?.entries?.values?.sumOf { it.sizeBytes }
                        ?: 0L
                    val isEncryptedSet = (primaryBackup?.isEncrypted == true) || (activeSet?.isEncrypted == true)

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(RadiusMd))
                            .border(1.dp, colors.outline.copy(alpha = 0.5f), RoundedCornerShape(RadiusMd)),
                        colors = CardDefaults.cardColors(containerColor = colors.surface)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(colors.dock.copy(alpha = 0.12f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (isEncryptedSet) Icons.Outlined.Lock else Icons.Outlined.Storage,
                                            contentDescription = null,
                                            tint = colors.dock,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "Latest backup · $setName",
                                            style = DockBody.copy(fontWeight = FontWeight.Bold),
                                            color = colors.textPrimary
                                        )
                                        Text(
                                            text = currentDock.userLabel,
                                            style = DockBodySmall.copy(fontSize = 12.sp),
                                            color = colors.textSecondary
                                        )
                                    }
                                }

                                // Connected Dock Chip (uses colors.dock; green is earned only after verification)
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(colors.dock.copy(alpha = 0.12f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(colors.dock)
                                        )
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(
                                            "Connected",
                                            style = DockMono.copy(
                                                color = colors.dock,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(colors.outline.copy(alpha = 0.35f))
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Last sealed", style = DockBodySmall, color = colors.textSecondary)
                                Text(
                                    dateStr,
                                    style = DockMono.copy(fontWeight = FontWeight.SemiBold, fontSize = 12.sp),
                                    color = colors.textPrimary
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Backup size", style = DockBodySmall, color = colors.textSecondary)
                                Text(
                                    text = if (isBuildingPlan) {
                                        "Reading manifest…"
                                    } else {
                                        String.format(Locale.US, "%,d files · %s", fileCount, formatBytes(totalBytes))
                                    },
                                    style = DockMono.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp,
                                        color = if (isBuildingPlan) colors.textSecondary.copy(alpha = breatheAlpha) else colors.dock
                                    )
                                )
                            }

                            if (primaryBackup != null && primaryBackup.deviceLabel != android.os.Build.MODEL) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Backup from", style = DockBodySmall, color = colors.textSecondary)
                                    Text(
                                        primaryBackup.deviceLabel,
                                        style = DockMono.copy(fontSize = 12.sp),
                                        color = colors.textSecondary
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }

                // Hero Hold Button & 3 Info Lines (SCREENS.md S12)
                item {
                    val plan = defaultPlan
                    val hasGaps = plan != null && plan.permissionGaps.isNotEmpty()
                    val isLockedEncrypted = primaryBackup != null && primaryBackup.isEncrypted &&
                        (encryptedSetManager?.isSetUnlocked(primaryBackup.setId) != true)

                    if (isLockedEncrypted) {
                        Button(
                            onClick = { onPromptUnlock(primaryBackup!!) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(72.dp),
                            shape = RoundedCornerShape(RadiusMd),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.dock,
                                contentColor = colors.onDock
                            )
                        ) {
                            Icon(imageVector = Icons.Outlined.Lock, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Unlock to restore", style = DockBody.copy(fontWeight = FontWeight.Bold, fontSize = 16.sp))
                        }
                    } else if (hasGaps) {
                        Button(
                            onClick = {
                                if (primaryBackup != null) onNavigateToNewPhone(primaryBackup)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(72.dp),
                            shape = RoundedCornerShape(RadiusMd),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.attention,
                                contentColor = colors.onDock
                            )
                        ) {
                            Text(
                                "Grant ${plan.permissionGaps.size} folders first",
                                style = DockBody.copy(fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            )
                        }
                    } else {
                        HoldToRestoreButton(
                            text = "Hold to restore everything",
                            enabled = plan != null && plan.canExecute && !isBuildingPlan,
                            haptics = haptics,
                            onHoldComplete = {
                                if (plan != null) onExecutePlan(plan)
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val filesCount = plan?.totalFilesToCopy ?: 0
                    val sizeFormatted = formatBytes(plan?.totalBytesToCopy ?: 0L)
                    Text(
                        text = String.format(
                            Locale.US,
                            "To original folders · Keep both if a file already exists · %,d files, %s",
                            filesCount,
                            sizeFormatted
                        ),
                        style = DockBodySmall.copy(fontSize = 12.sp),
                        color = colors.textTertiary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }

                // Selective Restore Options Card (SCREENS.md S12: "Or restore part of it")
                item {
                    Text(
                        text = "OR RESTORE PART OF IT",
                        style = DockMono.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                        color = colors.textTertiary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    )

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(RadiusMd))
                            .border(1.dp, colors.outline.copy(alpha = 0.5f), RoundedCornerShape(RadiusMd)),
                        colors = CardDefaults.cardColors(containerColor = colors.surface)
                    ) {
                        Column {
                            ActionRow(
                                title = "Choose folders or files",
                                subtitle = "Browse the backup tree or search by file name",
                                icon = Icons.Outlined.FolderOpen,
                                iconColor = colors.categoryPhoto,
                                onClick = {
                                    if (activeSet != null) onNavigateToBrowser(activeSet)
                                }
                            )
                            HorizontalDivider(color = colors.outline.copy(alpha = 0.35f))
                            ActionRow(
                                title = "Restore to a different folder",
                                subtitle = "Copy files into a single folder you choose",
                                icon = Icons.AutoMirrored.Outlined.DriveFileMove,
                                iconColor = colors.categoryVideo,
                                onClick = {
                                    if (activeSet != null) onNavigateToCustomFolderRestore(activeSet)
                                }
                            )
                            HorizontalDivider(color = colors.outline.copy(alpha = 0.35f))
                            ActionRow(
                                title = "Restore to a new phone",
                                subtitle = "Guided folder mapping onto a clean device",
                                icon = Icons.Outlined.PhoneAndroid,
                                iconColor = colors.categoryDocument,
                                onClick = {
                                    if (primaryBackup != null) onNavigateToNewPhone(primaryBackup)
                                }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Test Restore Drill Row (SCREENS.md S12)
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 56.dp)
                            .clip(RoundedCornerShape(RadiusSm))
                            .border(1.dp, colors.outline.copy(alpha = 0.4f), RoundedCornerShape(RadiusSm))
                            .clickable(role = Role.Button) {
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
                            },
                        colors = CardDefaults.cardColors(containerColor = colors.surfaceRaised)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(colors.dock.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Speed,
                                    contentDescription = null,
                                    tint = colors.dock,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Test restore (10 random files)",
                                    style = DockBody.copy(fontWeight = FontWeight.Bold),
                                    color = colors.textPrimary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    "Restore drill into a temporary folder with SHA-256 verification",
                                    style = DockBodySmall,
                                    color = colors.textSecondary
                                )
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowForwardIos,
                                contentDescription = null,
                                tint = colors.textTertiary,
                                modifier = Modifier.size(14.dp)
                            )
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
    icon: ImageVector,
    iconColor: Color,
    onClick: () -> Unit
) {
    val colors = DockTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 64.dp)
            .clickable(role = Role.Button) { onClick() }
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconColor.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = DockBody.copy(fontWeight = FontWeight.Bold), color = colors.textPrimary)
            Spacer(modifier = Modifier.height(2.dp))
            Text(subtitle, style = DockBodySmall, color = colors.textSecondary)
        }
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.ArrowForwardIos,
            contentDescription = null,
            tint = colors.textTertiary,
            modifier = Modifier.size(14.dp)
        )
    }
}

private fun formatBytes(bytes: Long): String {
    val gb = bytes.toDouble() / (1024 * 1024 * 1024)
    return if (gb >= 1.0) String.format(Locale.US, "%.1f GB", gb) else "${bytes / (1024 * 1024)} MB"
}
