package `in`.soumyaranjansahoo.dockdrop.ui.screens.restore

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Storage
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.soumyaranjansahoo.dockdrop.core.crypto.EncryptedSetManager
import `in`.soumyaranjansahoo.dockdrop.core.manifest.DiscoveredBackupSet
import `in`.soumyaranjansahoo.dockdrop.core.model.SetDefinition
import `in`.soumyaranjansahoo.dockdrop.core.model.SourceFolder
import `in`.soumyaranjansahoo.dockdrop.core.repository.SetRepository
import `in`.soumyaranjansahoo.dockdrop.core.restore.ConflictPolicy
import `in`.soumyaranjansahoo.dockdrop.core.restore.RestoreDestination
import `in`.soumyaranjansahoo.dockdrop.core.restore.RestorePlan
import `in`.soumyaranjansahoo.dockdrop.core.restore.RestorePlanner
import `in`.soumyaranjansahoo.dockdrop.ui.components.DockTopAppBar
import `in`.soumyaranjansahoo.dockdrop.ui.components.HoldToRestoreButton
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockBody
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockBodySmall
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockHeadline
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockMono
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockTheme
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockTitle
import `in`.soumyaranjansahoo.dockdrop.ui.theme.HapticFeedbackController
import `in`.soumyaranjansahoo.dockdrop.ui.theme.RadiusMd
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

@Composable
fun NewPhoneRestoreScreen(
    backup: DiscoveredBackupSet,
    vaultTreeUri: Uri,
    restorePlanner: RestorePlanner,
    setRepository: SetRepository,
    onExecutePlan: (RestorePlan) -> Unit,
    onCancel: () -> Unit,
    encryptedSetManager: EncryptedSetManager? = null,
    onPromptUnlock: (DiscoveredBackupSet) -> Unit = {},
    haptics: HapticFeedbackController? = null,
    modifier: Modifier = Modifier
) {
    val colors = DockTheme.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var step by remember { mutableIntStateOf(1) } // 1: Found backup, 2: Map folders, 3: Review, 4: Hold to restore
    val folderMappings = remember { mutableStateMapOf<String, String>() }
    var activeGrantingFolder by remember { mutableStateOf<String?>(null) }

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            try {
                context.contentResolver.takePersistableUriPermission(uri, flags)
            } catch (e: Exception) {}

            val folder = activeGrantingFolder
            if (folder != null) {
                folderMappings[folder] = uri.toString()
                activeGrantingFolder = null
            }
        }
    }

    var plan by remember { mutableStateOf<RestorePlan?>(null) }
    var isBuildingPlan by remember { mutableStateOf(false) }

    fun refreshPlan() {
        scope.launch {
            isBuildingPlan = true
            try {
                val p = restorePlanner.createPlan(
                    vaultTreeUri = vaultTreeUri,
                    setId = backup.setId,
                    setName = backup.setName,
                    deviceLabel = backup.deviceLabel,
                    destination = RestoreDestination.OriginalFolders(folderMappings.toMap()),
                    conflictPolicy = ConflictPolicy.KEEP_BOTH
                )
                plan = p
            } catch (e: Exception) {
                // ignore
            } finally {
                isBuildingPlan = false
            }
        }
    }

    LaunchedEffect(folderMappings.toMap()) {
        refreshPlan()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = colors.bg,
        topBar = {
            DockTopAppBar(
                title = "New Phone Migration",
                subtitle = "Step $step of 4",
                onBackClick = {
                    if (step > 1) step-- else onCancel()
                },
                actions = {
                    TextButton(onClick = onCancel) {
                        Text(
                            "Cancel",
                            color = colors.textTertiary,
                            style = DockBodySmall.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
        ) {
            // Step Capsule Progress Track
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (i in 1..4) {
                        val isFinished = i <= step
                        val barColor by animateColorAsState(
                            targetValue = if (isFinished) colors.dock else colors.outline.copy(alpha = 0.35f),
                            label = "stepProgress"
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(barColor)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            when (step) {
                1 -> {
                    // Step 1: Found backup
                    item {
                        Text("We found a backup", style = DockHeadline, color = colors.textPrimary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "This SSD contains files backed up from another device. You can safely migrate them to this phone.",
                            style = DockBodySmall,
                            color = colors.textSecondary
                        )
                        Spacer(modifier = Modifier.height(20.dp))

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .border(1.dp, colors.outline.copy(alpha = 0.5f), RoundedCornerShape(20.dp)),
                            colors = CardDefaults.cardColors(containerColor = colors.surface)
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(46.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(colors.dock.copy(alpha = 0.12f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.PhoneAndroid,
                                            contentDescription = null,
                                            tint = colors.dock,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column {
                                        Text(
                                            backup.setName,
                                            style = DockTitle.copy(fontSize = 18.sp),
                                            color = colors.textPrimary
                                        )
                                        Text(
                                            "Source: ${backup.deviceLabel}",
                                            style = DockBodySmall,
                                            color = colors.dock
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(1.dp)
                                        .background(colors.outline.copy(alpha = 0.35f))
                                )
                                Spacer(modifier = Modifier.height(14.dp))

                                val totalBytes = backup.generation.entries.values.sumOf { it.sizeBytes }
                                val dateStr = SimpleDateFormat("MMM d, yyyy", Locale.US).format(Date(backup.lastModifiedMs))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Archive Volume", style = DockBodySmall, color = colors.textSecondary)
                                    Text(
                                        "${backup.generation.entries.size} files · ${formatBytes(totalBytes)}",
                                        style = DockMono.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp),
                                        color = colors.textPrimary
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Last Sealed", style = DockBodySmall, color = colors.textSecondary)
                                    Text(dateStr, style = DockMono.copy(fontSize = 12.sp), color = colors.textSecondary)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(30.dp))

                        Button(
                            onClick = {
                                if (backup.isEncrypted && encryptedSetManager?.isSetUnlocked(backup.setId) != true) {
                                    onPromptUnlock(backup)
                                } else {
                                    step = 2
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(18.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = colors.dock, contentColor = colors.onDock)
                        ) {
                            if (backup.isEncrypted && encryptedSetManager?.isSetUnlocked(backup.setId) != true) {
                                Icon(imageVector = Icons.Outlined.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text(
                                if (backup.isEncrypted && encryptedSetManager?.isSetUnlocked(backup.setId) != true) "Unlock to restore" else "Migrate from this backup",
                                style = DockBody.copy(fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            )
                        }
                    }
                }

                2 -> {
                    // Step 2: Map your folders
                    item {
                        Text("Map target folders", style = DockHeadline, color = colors.textPrimary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Grant access to where each backed-up folder should be placed on this phone.",
                            style = DockBodySmall,
                            color = colors.textSecondary
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                    }

                    val sourceLabels = backup.sourceLabels.ifEmpty { listOf("Camera", "Pictures", "Documents") }
                    items(sourceLabels.size) { index ->
                        val label = sourceLabels[index]
                        val isGranted = folderMappings.containsKey(label)

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 5.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .border(
                                    1.dp,
                                    if (isGranted) colors.seal else colors.outline.copy(alpha = 0.5f),
                                    RoundedCornerShape(16.dp)
                                ),
                            colors = CardDefaults.cardColors(containerColor = colors.surface)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isGranted) colors.seal.copy(alpha = 0.12f) else colors.surfaceRaised),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isGranted) Icons.Outlined.CheckCircle else Icons.Outlined.Folder,
                                        contentDescription = null,
                                        tint = if (isGranted) colors.seal else colors.textSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(label, style = DockBody.copy(fontWeight = FontWeight.Bold), color = colors.textPrimary)
                                    Text(
                                        text = if (isGranted) "Access granted ✓" else "Needs folder permission",
                                        style = DockBodySmall.copy(
                                            color = if (isGranted) colors.seal else colors.textTertiary,
                                            fontSize = 12.sp
                                        )
                                    )
                                }

                                OutlinedButton(
                                    onClick = {
                                        activeGrantingFolder = label
                                        folderPickerLauncher.launch(null)
                                    },
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(if (isGranted) "Change" else "Grant")
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                        val allMapped = sourceLabels.all { folderMappings.containsKey(it) }
                        Button(
                            onClick = {
                                scope.launch {
                                    val newSet = SetDefinition(
                                        id = backup.setId,
                                        name = backup.setName,
                                        dockVolumeUuid = backup.driveId,
                                        dockUserLabel = "SSD",
                                        destinationVaultUriString = vaultTreeUri.toString(),
                                        isEncrypted = backup.isEncrypted
                                    )
                                    val newFolders = folderMappings.map { (fLabel, uriStr) ->
                                        SourceFolder(
                                            id = UUID.randomUUID().toString(),
                                            setId = backup.setId,
                                            label = fLabel,
                                            treeUriString = uriStr
                                        )
                                    }
                                    setRepository.saveSet(newSet, newFolders)
                                    refreshPlan()
                                    step = 3
                                }
                            },
                            enabled = allMapped,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(18.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = colors.dock, contentColor = colors.onDock)
                        ) {
                            Text(
                                if (allMapped) "Continue to Review" else "Grant All Folders to Continue",
                                style = DockBody.copy(fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            )
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }

                3 -> {
                    // Step 3: Review
                    item {
                        val p = plan
                        Text("Review Migration", style = DockHeadline, color = colors.textPrimary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Check destination and phone storage capacity before copying.",
                            style = DockBodySmall,
                            color = colors.textSecondary
                        )
                        Spacer(modifier = Modifier.height(20.dp))

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .border(1.dp, colors.outline.copy(alpha = 0.5f), RoundedCornerShape(18.dp)),
                            colors = CardDefaults.cardColors(containerColor = colors.surface)
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Text(
                                    "PLAN SUMMARY",
                                    style = DockMono.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                                    color = colors.textTertiary
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text("Destination: Original mapped directories", style = DockBody.copy(fontWeight = FontWeight.Medium), color = colors.textPrimary)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Conflict Rule: Keep both files (non-destructive)", style = DockBodySmall, color = colors.textSecondary)

                                Spacer(modifier = Modifier.height(14.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(1.dp)
                                        .background(colors.outline.copy(alpha = 0.35f))
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                val totalBytes = p?.totalBytesToCopy ?: 0L
                                val phoneFree = p?.phoneFreeBytes ?: 0L
                                val enough = p?.hasEnoughSpace ?: true

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Files to Restore", style = DockBodySmall, color = colors.textSecondary)
                                    Text("${p?.totalFilesToCopy ?: 0}", style = DockMono.copy(fontWeight = FontWeight.Bold), color = colors.textPrimary)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Storage Space", style = DockBodySmall, color = colors.textSecondary)
                                    Text(
                                        "Needs ${formatBytes(totalBytes)} · ${formatBytes(phoneFree)} free",
                                        style = DockMono.copy(fontWeight = FontWeight.Bold, color = if (enough) colors.seal else colors.attention)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(30.dp))

                        Button(
                            onClick = { step = 4 },
                            enabled = plan?.canExecute == true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(18.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = colors.dock, contentColor = colors.onDock)
                        ) {
                            Text("Ready to restore", style = DockBody.copy(fontWeight = FontWeight.Bold, fontSize = 16.sp))
                        }
                    }
                }

                4 -> {
                    // Step 4: Hold to restore
                    item {
                        val p = plan
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Hold to restore", style = DockHeadline, color = colors.textPrimary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Press and hold the button below to stream your files to this phone.",
                            style = DockBodySmall,
                            color = colors.textSecondary
                        )
                        Spacer(modifier = Modifier.height(32.dp))

                        HoldToRestoreButton(
                            text = "Hold to restore everything",
                            enabled = p != null && p.canExecute,
                            haptics = haptics,
                            onHoldComplete = {
                                if (p != null) onExecutePlan(p)
                            }
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "To original folders · Keep both if file exists · ${p?.totalFilesToCopy ?: 0} files",
                            style = DockBodySmall.copy(fontSize = 12.sp),
                            color = colors.textTertiary
                        )
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
