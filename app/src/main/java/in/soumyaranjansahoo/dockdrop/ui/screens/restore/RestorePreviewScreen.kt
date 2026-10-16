package `in`.soumyaranjansahoo.dockdrop.ui.screens.restore

import android.app.Activity
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
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DriveFileMove
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.soumyaranjansahoo.dockdrop.core.manifest.ManifestEntry
import `in`.soumyaranjansahoo.dockdrop.core.model.SetDefinition
import `in`.soumyaranjansahoo.dockdrop.core.model.SourceFolder
import `in`.soumyaranjansahoo.dockdrop.core.restore.ConflictPolicy
import `in`.soumyaranjansahoo.dockdrop.core.restore.RestoreDestination
import `in`.soumyaranjansahoo.dockdrop.core.restore.RestorePlan
import `in`.soumyaranjansahoo.dockdrop.core.restore.RestorePlanner
import `in`.soumyaranjansahoo.dockdrop.ui.components.DockSelectableCard
import `in`.soumyaranjansahoo.dockdrop.ui.components.DockTopAppBar
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockBody
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockBodySmall
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockHeadline
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockMono
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockTheme
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockTitle
import `in`.soumyaranjansahoo.dockdrop.ui.theme.RadiusMd
import kotlinx.coroutines.launch

@Composable
fun RestorePreviewScreen(
    set: SetDefinition,
    sourceFolders: List<SourceFolder>,
    selectedEntries: List<ManifestEntry>,
    restorePlanner: RestorePlanner,
    onStartRestore: (RestorePlan) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = DockTheme.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isOriginalDestination by remember { mutableStateOf(true) }
    var customFolderUri by remember { mutableStateOf<Uri?>(null) }
    var customFolderLabel by remember { mutableStateOf<String?>(null) }
    var conflictPolicy by remember { mutableStateOf(ConflictPolicy.KEEP_BOTH) }

    // Map: folderLabel -> treeUriString (mutable so gaps can be granted live)
    val folderMappings = remember {
        mutableStateMapOf<String, String>().apply {
            sourceFolders.forEach { put(it.label, it.treeUriString) }
        }
    }

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
            } else if (!isOriginalDestination) {
                customFolderUri = uri
                customFolderLabel = uri.lastPathSegment ?: "Chosen folder"
            }
        }
    }

    var plan by remember { mutableStateOf<RestorePlan?>(null) }
    var isRecalculating by remember { mutableStateOf(true) }

    LaunchedEffect(isOriginalDestination, customFolderUri, conflictPolicy, folderMappings.toMap()) {
        isRecalculating = true
        scope.launch {
            val dest = if (isOriginalDestination) {
                RestoreDestination.OriginalFolders(folderMappings.toMap())
            } else {
                val uriStr = customFolderUri?.toString() ?: ""
                RestoreDestination.ChosenFolder(uriStr, customFolderLabel ?: "Custom folder")
            }

            try {
                val newPlan = restorePlanner.createPlan(
                    vaultTreeUri = Uri.parse(set.destinationVaultUriString),
                    setId = set.id,
                    setName = set.name,
                    deviceLabel = android.os.Build.MODEL,
                    selectedEntries = selectedEntries,
                    destination = dest,
                    conflictPolicy = conflictPolicy
                )
                plan = newPlan
            } catch (e: Exception) {
                // ignore
            } finally {
                isRecalculating = false
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = colors.bg,
        topBar = {
            DockTopAppBar(
                title = "Restore Preview",
                subtitle = "${selectedEntries.size} items selected",
                onBackClick = onBackClick
            )
        },
        bottomBar = {
            val p = plan
            val canRestore = p != null && p.canExecute && !isRecalculating

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Button(
                    onClick = {
                        if (p != null) onStartRestore(p)
                    },
                    enabled = canRestore,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.dock,
                        contentColor = colors.onDock,
                        disabledContainerColor = colors.surfaceRaised,
                        disabledContentColor = colors.textTertiary
                    )
                ) {
                    val label = when {
                        isRecalculating -> "Calculating Plan…"
                        p == null -> "Preparing…"
                        p.permissionGaps.isNotEmpty() -> "Grant Missing Folders First"
                        !p.hasEnoughSpace -> "Insufficient Storage Space"
                        p.totalFilesToCopy == 0 -> "0 Files to Copy"
                        else -> "Start Restore · ${p.totalFilesToCopy} files"
                    }
                    Text(label, style = DockBody.copy(fontWeight = FontWeight.Bold, fontSize = 16.sp))
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
            // Summary Header Card
            item {
                Spacer(modifier = Modifier.height(8.dp))
                val totalSelected = selectedEntries.size
                val totalBytes = selectedEntries.sumOf { it.sizeBytes }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .border(1.dp, colors.outline.copy(alpha = 0.5f), RoundedCornerShape(18.dp)),
                    colors = CardDefaults.cardColors(containerColor = colors.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(colors.dock.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.FolderOpen,
                                contentDescription = null,
                                tint = colors.dock,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "$totalSelected files selected",
                                style = DockTitle.copy(fontSize = 17.sp),
                                color = colors.textPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Total volume: ${formatBytes(totalBytes)}",
                                style = DockMono.copy(color = colors.dock, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            // Destination Selector
            item {
                Text(
                    "RESTORE DESTINATION",
                    style = DockMono.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                    color = colors.textTertiary
                )
                Spacer(modifier = Modifier.height(10.dp))

                DockSelectableCard(
                    title = "Original folders (Recommended)",
                    subtitle = "Puts photos back in DCIM, documents back in Documents, etc.",
                    selected = isOriginalDestination,
                    onClick = { isOriginalDestination = true },
                    icon = Icons.Outlined.Folder,
                    iconBgColor = colors.dock.copy(alpha = 0.12f),
                    iconTintColor = colors.dock,
                    isRadio = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                DockSelectableCard(
                    title = "Extract to a custom folder",
                    subtitle = if (!isOriginalDestination && customFolderLabel != null) "Restoring to: $customFolderLabel" else "Choose a single directory on your phone",
                    selected = !isOriginalDestination,
                    onClick = {
                        isOriginalDestination = false
                        if (customFolderUri == null) {
                            folderPickerLauncher.launch(null)
                        }
                    },
                    icon = Icons.Outlined.DriveFileMove,
                    iconBgColor = Color(0xFF6366F1).copy(alpha = 0.12f),
                    iconTintColor = Color(0xFF6366F1),
                    isRadio = true
                )

                if (!isOriginalDestination) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { folderPickerLauncher.launch(null) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (customFolderLabel != null) "Change Folder ($customFolderLabel)" else "Choose Target Folder")
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            // Conflict Policy Selector
            item {
                Text(
                    "IF A FILE ALREADY EXISTS ON PHONE",
                    style = DockMono.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                    color = colors.textTertiary
                )
                Spacer(modifier = Modifier.height(10.dp))

                DockSelectableCard(
                    title = "Keep both (Recommended)",
                    subtitle = "Appends (1) to restored file so nothing is overwritten.",
                    selected = conflictPolicy == ConflictPolicy.KEEP_BOTH,
                    onClick = { conflictPolicy = ConflictPolicy.KEEP_BOTH },
                    icon = Icons.Outlined.ContentCopy,
                    iconBgColor = colors.dock.copy(alpha = 0.12f),
                    iconTintColor = colors.dock,
                    trailingBadge = "Safe",
                    isRadio = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                DockSelectableCard(
                    title = "Skip existing",
                    subtitle = "Does not overwrite files that already exist on your phone.",
                    selected = conflictPolicy == ConflictPolicy.SKIP,
                    onClick = { conflictPolicy = ConflictPolicy.SKIP },
                    icon = Icons.Outlined.SkipNext,
                    iconBgColor = colors.surfaceRaised,
                    iconTintColor = colors.textSecondary,
                    isRadio = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                DockSelectableCard(
                    title = "Overwrite existing (Caution)",
                    subtitle = "Replaces local file with the version from your SSD.",
                    selected = conflictPolicy == ConflictPolicy.OVERWRITE,
                    onClick = { conflictPolicy = ConflictPolicy.OVERWRITE },
                    icon = Icons.Outlined.Warning,
                    iconBgColor = colors.danger.copy(alpha = 0.12f),
                    iconTintColor = colors.danger,
                    trailingBadge = "Destructive",
                    isRadio = true
                )

                val p = plan
                if (p != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (p.conflictCount > 0) {
                            "⚡ ${p.conflictCount} conflicts detected in target destination"
                        } else {
                            "✓ No filename conflicts found in destination"
                        },
                        style = DockMono.copy(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (p.conflictCount > 0) colors.attention else colors.seal
                        )
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            // Permission Gaps
            val p = plan
            if (p != null && p.permissionGaps.isNotEmpty()) {
                item {
                    Text(
                        "PERMISSIONS NEEDED",
                        style = DockMono.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                        color = colors.attention
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .border(1.dp, colors.attention.copy(alpha = 0.5f), RoundedCornerShape(18.dp)),
                        colors = CardDefaults.cardColors(containerColor = colors.attention.copy(alpha = 0.05f))
                    ) {
                        Column {
                            p.permissionGaps.forEachIndexed { idx, gap ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            "${gap.folderLabel} Folder",
                                            style = DockBody.copy(fontWeight = FontWeight.Bold),
                                            color = colors.textPrimary
                                        )
                                        Text(
                                            "${gap.fileCount} files · ${formatBytes(gap.totalBytes)}",
                                            style = DockBodySmall,
                                            color = colors.textSecondary
                                        )
                                    }
                                    Button(
                                        onClick = {
                                            activeGrantingFolder = gap.folderLabel
                                            folderPickerLauncher.launch(null)
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = colors.dock)
                                    ) {
                                        Text("Grant Access", style = DockBodySmall.copy(fontWeight = FontWeight.SemiBold))
                                    }
                                }
                                if (idx < p.permissionGaps.size - 1) {
                                    HorizontalDivider(color = colors.outline.copy(alpha = 0.35f))
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            // Space Preflight Row
            item {
                Text(
                    "PHONE STORAGE CHECK",
                    style = DockMono.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                    color = colors.textTertiary
                )
                Spacer(modifier = Modifier.height(8.dp))

                val pPlan = plan
                val phoneFree = pPlan?.phoneFreeBytes ?: 0L
                val needBytes = pPlan?.totalBytesToCopy ?: 0L
                val enough = pPlan?.hasEnoughSpace ?: true

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .border(
                            1.dp,
                            if (enough) colors.seal.copy(alpha = 0.5f) else colors.danger.copy(alpha = 0.5f),
                            RoundedCornerShape(18.dp)
                        ),
                    colors = CardDefaults.cardColors(
                        containerColor = if (enough) colors.seal.copy(alpha = 0.05f) else colors.danger.copy(alpha = 0.05f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (enough) Icons.Outlined.CheckCircle else Icons.Outlined.Warning,
                            contentDescription = null,
                            tint = if (enough) colors.seal else colors.danger,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Needs ${formatBytes(needBytes)} · ${formatBytes(phoneFree)} free on phone",
                                style = DockBody.copy(fontWeight = FontWeight.Bold),
                                color = if (enough) colors.textPrimary else colors.danger
                            )
                            Text(
                                text = if (enough) "Sufficient internal storage available" else "Free up space on your device before starting restore",
                                style = DockBodySmall,
                                color = colors.textSecondary
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }
}

private fun formatBytes(bytes: Long): String {
    val gb = bytes.toDouble() / (1024 * 1024 * 1024)
    return if (gb >= 1.0) String.format("%.1f GB", gb) else "${bytes / (1024 * 1024)} MB"
}
