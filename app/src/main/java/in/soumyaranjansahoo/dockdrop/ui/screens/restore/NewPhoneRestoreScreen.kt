package `in`.soumyaranjansahoo.dockdrop.ui.screens.restore

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.soumyaranjansahoo.dockdrop.core.manifest.DiscoveredBackupSet
import `in`.soumyaranjansahoo.dockdrop.core.model.SetDefinition
import `in`.soumyaranjansahoo.dockdrop.core.model.SourceFolder
import `in`.soumyaranjansahoo.dockdrop.core.repository.SetRepository
import `in`.soumyaranjansahoo.dockdrop.core.restore.ConflictPolicy
import `in`.soumyaranjansahoo.dockdrop.core.restore.RestoreDestination
import `in`.soumyaranjansahoo.dockdrop.core.restore.RestorePlan
import `in`.soumyaranjansahoo.dockdrop.core.restore.RestorePlanner
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
    encryptedSetManager: `in`.soumyaranjansahoo.dockdrop.core.crypto.EncryptedSetManager? = null,
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "←",
                        style = DockTitle.copy(fontSize = 24.sp),
                        color = colors.textPrimary,
                        modifier = Modifier
                            .clickable {
                                if (step > 1) step-- else onCancel()
                            }
                            .padding(end = 12.dp)
                    )
                    Text(
                        text = "New Phone Setup",
                        style = DockTitle,
                        color = colors.textPrimary
                    )
                }

                // Progress dots (4 steps)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (i in 1..4) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (i == step) colors.dock else if (i < step) colors.seal else colors.outline)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
        ) {
            when (step) {
                1 -> {
                    // Step 1: We found a backup (SCREENS.md S16 step 1)
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("We found a backup", style = DockHeadline, color = colors.textPrimary)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "This SSD contains files backed up from another device. You can restore them to this phone.",
                            style = DockBodySmall,
                            color = colors.textSecondary
                        )
                        Spacer(modifier = Modifier.height(24.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(RadiusMd))
                                .background(colors.surface)
                                .border(1.dp, colors.outline, RoundedCornerShape(RadiusMd))
                                .padding(20.dp)
                        ) {
                            Column {
                                Text(backup.setName, style = DockHeadline.copy(fontSize = 22.sp), color = colors.textPrimary)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Device: ${backup.deviceLabel}", style = DockBodySmall, color = colors.dock)
                                Spacer(modifier = Modifier.height(12.dp))
                                HorizontalDivider(color = colors.outline)
                                Spacer(modifier = Modifier.height(12.dp))

                                val totalBytes = backup.generation.entries.values.sumOf { it.sizeBytes }
                                val dateStr = SimpleDateFormat("MMM d, yyyy", Locale.US).format(Date(backup.lastModifiedMs))

                                Text("${backup.generation.entries.size} files · ${formatBytes(totalBytes)}", style = DockMono, color = colors.textPrimary)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Last sealed: $dateStr", style = DockBodySmall, color = colors.textSecondary)
                            }
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        Button(
                            onClick = {
                                if (backup.isEncrypted && encryptedSetManager?.isSetUnlocked(backup.setId) != true) {
                                    onPromptUnlock(backup)
                                } else {
                                    step = 2
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = colors.dock, contentColor = colors.onDock)
                        ) {
                            Text(
                                if (backup.isEncrypted && encryptedSetManager?.isSetUnlocked(backup.setId) != true) "Unlock to restore" else "Restore from this backup",
                                style = DockBody.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }

                2 -> {
                    // Step 2: Map your folders (SCREENS.md S16 step 2)
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Map your folders", style = DockHeadline, color = colors.textPrimary)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Grant access to where each folder should live on this phone.",
                            style = DockBodySmall,
                            color = colors.textSecondary
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                    }

                    val sourceLabels = backup.sourceLabels.ifEmpty { listOf("Camera", "Pictures", "Documents") }
                    items(sourceLabels.size) { index ->
                        val label = sourceLabels[index]
                        val isGranted = folderMappings.containsKey(label)

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(colors.surface)
                                .border(1.dp, if (isGranted) colors.seal else colors.outline, RoundedCornerShape(14.dp))
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(label, style = DockBody.copy(fontWeight = FontWeight.SemiBold), color = colors.textPrimary)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = if (isGranted) "Folder granted ✓" else "Needs access on this phone",
                                        style = DockBodySmall.copy(color = if (isGranted) colors.seal else colors.textTertiary)
                                    )
                                }

                                Button(
                                    onClick = {
                                        activeGrantingFolder = label
                                        folderPickerLauncher.launch(null)
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isGranted) colors.surfaceRaised else colors.dock,
                                        contentColor = if (isGranted) colors.textPrimary else colors.onDock
                                    )
                                ) {
                                    Text(if (isGranted) "Change" else "Grant")
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    item {
                        Spacer(modifier = Modifier.height(20.dp))
                        val allMapped = sourceLabels.all { folderMappings.containsKey(it) }
                        Button(
                            onClick = {
                                // Persist newly mapped set in local Room database
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
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = colors.dock, contentColor = colors.onDock)
                        ) {
                            Text("Continue to review", style = DockBody.copy(fontWeight = FontWeight.Bold))
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }

                3 -> {
                    // Step 3: Review (SCREENS.md S16 step 3)
                    item {
                        val p = plan
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Review restore", style = DockHeadline, color = colors.textPrimary)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Check destination and space before beginning.",
                            style = DockBodySmall,
                            color = colors.textSecondary
                        )
                        Spacer(modifier = Modifier.height(20.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(RadiusMd))
                                .background(colors.surface)
                                .border(1.dp, colors.outline, RoundedCornerShape(RadiusMd))
                                .padding(18.dp)
                        ) {
                            Column {
                                Text("Destination: Original mapped folders", style = DockBody.copy(fontWeight = FontWeight.Medium), color = colors.textPrimary)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Policy: Keep both if file exists", style = DockBodySmall, color = colors.textSecondary)
                                Spacer(modifier = Modifier.height(12.dp))
                                HorizontalDivider(color = colors.outline)
                                Spacer(modifier = Modifier.height(12.dp))

                                val totalBytes = p?.totalBytesToCopy ?: 0L
                                val phoneFree = p?.phoneFreeBytes ?: 0L
                                val enough = p?.hasEnoughSpace ?: true

                                Text("Files to restore: ${p?.totalFilesToCopy ?: 0}", style = DockMono, color = colors.textPrimary)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Needs ${formatBytes(totalBytes)} · ${formatBytes(phoneFree)} free", style = DockMono, color = if (enough) colors.seal else colors.attention)
                            }
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        Button(
                            onClick = { step = 4 },
                            enabled = plan?.canExecute == true,
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = colors.dock, contentColor = colors.onDock)
                        ) {
                            Text("Ready to restore", style = DockBody.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                }

                4 -> {
                    // Step 4: Hold to restore (SCREENS.md S16 step 4)
                    item {
                        val p = plan
                        Spacer(modifier = Modifier.height(24.dp))
                        Text("Hold to restore", style = DockHeadline, color = colors.textPrimary)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Press and hold the button below to start copying files to your phone.",
                            style = DockBodySmall,
                            color = colors.textSecondary
                        )
                        Spacer(modifier = Modifier.height(36.dp))

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
