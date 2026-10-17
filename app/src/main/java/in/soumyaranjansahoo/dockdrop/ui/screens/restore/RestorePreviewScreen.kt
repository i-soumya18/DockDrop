package `in`.soumyaranjansahoo.dockdrop.ui.screens.restore

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DriveFileMove
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.SkipNext
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
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockMono
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockTheme
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockTitle
import `in`.soumyaranjansahoo.dockdrop.ui.theme.RadiusMd
import `in`.soumyaranjansahoo.dockdrop.ui.theme.RadiusSm
import kotlinx.coroutines.launch
import java.util.Locale

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
                subtitle = if (selectedEntries.isNotEmpty()) {
                    String.format(Locale.US, "%,d files selected", selectedEntries.size)
                } else {
                    set.name
                },
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
                    shape = RoundedCornerShape(RadiusMd),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.dock,
                        contentColor = colors.onDock,
                        disabledContainerColor = colors.surfaceRaised,
                        disabledContentColor = colors.textTertiary
                    )
                ) {
                    val label = when {
                        isRecalculating -> "Calculating restore plan…"
                        p == null -> "Preparing…"
                        p.permissionGaps.isNotEmpty() -> "Grant missing folders first"
                        !p.hasEnoughSpace -> "Not enough space on phone"
                        p.totalFilesToCopy == 0 -> "0 files to restore"
                        else -> String.format(Locale.US, "Restore %,d files", p.totalFilesToCopy)
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
            // Summary Header Card (SCREENS.md S14: "{n} files · {size}")
            item {
                Spacer(modifier = Modifier.height(8.dp))
                val p = plan
                val totalSelected = if (selectedEntries.isNotEmpty()) selectedEntries.size else (p?.totalFilesSelected ?: 0)
                val totalBytes = if (selectedEntries.isNotEmpty()) {
                    selectedEntries.sumOf { it.sizeBytes }
                } else {
                    p?.totalBytesToCopy ?: 0L
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(RadiusMd))
                        .border(1.dp, colors.outline.copy(alpha = 0.5f), RoundedCornerShape(RadiusMd)),
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
                                .clip(RoundedCornerShape(RadiusSm))
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
                                text = String.format(Locale.US, "%,d files · %s", totalSelected, formatBytes(totalBytes)),
                                style = DockTitle.copy(fontSize = 18.sp),
                                color = colors.textPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Verified with SHA-256 on restore",
                                style = DockBodySmall,
                                color = colors.textSecondary
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            // Destination Selector (SCREENS.md S14)
            item {
                Text(
                    "DESTINATION",
                    style = DockMono.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                    color = colors.textTertiary
                )
                Spacer(modifier = Modifier.height(10.dp))

                DockSelectableCard(
                    title = "Original folders",
                    subtitle = "Restores Camera to DCIM, Documents to Documents, etc.",
                    selected = isOriginalDestination,
                    onClick = { isOriginalDestination = true },
                    icon = Icons.Outlined.Folder,
                    iconBgColor = colors.dock.copy(alpha = 0.12f),
                    iconTintColor = colors.dock,
                    trailingBadge = "Default",
                    isRadio = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                DockSelectableCard(
                    title = "Choose folder",
                    subtitle = if (!isOriginalDestination && customFolderLabel != null) {
                        "Destination: $customFolderLabel"
                    } else {
                        "Restore everything into a single folder you pick"
                    },
                    selected = !isOriginalDestination,
                    onClick = {
                        isOriginalDestination = false
                        if (customFolderUri == null) {
                            folderPickerLauncher.launch(null)
                        }
                    },
                    icon = Icons.AutoMirrored.Outlined.DriveFileMove,
                    iconBgColor = colors.categoryVideo.copy(alpha = 0.12f),
                    iconTintColor = colors.categoryVideo,
                    isRadio = true
                )

                if (!isOriginalDestination) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { folderPickerLauncher.launch(null) },
                        shape = RoundedCornerShape(RadiusSm),
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 48.dp)
                    ) {
                        Text(
                            text = if (customFolderLabel != null) "Change folder ($customFolderLabel)" else "Choose target folder",
                            style = DockBodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = colors.dock
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            // Conflict Policy Selector (SCREENS.md S14: Keep both · Skip · Overwrite)
            item {
                Text(
                    "IF A FILE ALREADY EXISTS",
                    style = DockMono.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                    color = colors.textTertiary
                )
                Spacer(modifier = Modifier.height(10.dp))

                DockSelectableCard(
                    title = "Keep both",
                    subtitle = "Adds (1) to the restored filename so nothing is lost.",
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
                    title = "Skip",
                    subtitle = "Leaves existing files on your phone untouched.",
                    selected = conflictPolicy == ConflictPolicy.SKIP,
                    onClick = { conflictPolicy = ConflictPolicy.SKIP },
                    icon = Icons.Outlined.SkipNext,
                    iconBgColor = colors.surfaceRaised,
                    iconTintColor = colors.textSecondary,
                    isRadio = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                DockSelectableCard(
                    title = "Overwrite",
                    subtitle = "Replaces the file on your phone with the backup copy.",
                    selected = conflictPolicy == ConflictPolicy.OVERWRITE,
                    onClick = { conflictPolicy = ConflictPolicy.OVERWRITE },
                    icon = Icons.Outlined.Warning,
                    iconBgColor = colors.danger.copy(alpha = 0.12f),
                    iconTintColor = colors.danger,
                    trailingBadge = "Replaces files",
                    isRadio = true
                )

                val p = plan
                if (p != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (p.conflictCount > 0) {
                            String.format(Locale.US, "%,d files already exist in destination", p.conflictCount)
                        } else {
                            "No conflicting files found in destination"
                        },
                        style = DockMono.copy(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (p.conflictCount > 0) colors.attentionText else colors.textSecondary
                        )
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            // Permissions Needed (SCREENS.md S14)
            val p = plan
            if (p != null && p.permissionGaps.isNotEmpty()) {
                item {
                    Text(
                        "PERMISSIONS NEEDED",
                        style = DockMono.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                        color = colors.attentionText
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(RadiusMd))
                            .border(1.dp, colors.attention.copy(alpha = 0.5f), RoundedCornerShape(RadiusMd)),
                        colors = CardDefaults.cardColors(containerColor = colors.attention.copy(alpha = 0.08f))
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
                                            "${gap.folderLabel} folder",
                                            style = DockBody.copy(fontWeight = FontWeight.Bold),
                                            color = colors.textPrimary
                                        )
                                        Text(
                                            String.format(Locale.US, "%,d files · %s", gap.fileCount, formatBytes(gap.totalBytes)),
                                            style = DockBodySmall,
                                            color = colors.textSecondary
                                        )
                                    }
                                    Button(
                                        onClick = {
                                            activeGrantingFolder = gap.folderLabel
                                            folderPickerLauncher.launch(null)
                                        },
                                        modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                                        shape = RoundedCornerShape(RadiusSm),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = colors.dock,
                                            contentColor = colors.onDock
                                        )
                                    ) {
                                        Text("Grant", style = DockBodySmall.copy(fontWeight = FontWeight.Bold))
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

            // Space Check Row (SCREENS.md S14: "Needs {size}, {free} free on your phone ✓" — amber if not)
            item {
                Text(
                    "SPACE CHECK",
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
                        .clip(RoundedCornerShape(RadiusMd))
                        .border(
                            1.dp,
                            if (enough) colors.outline.copy(alpha = 0.6f) else colors.attention.copy(alpha = 0.6f),
                            RoundedCornerShape(RadiusMd)
                        ),
                    colors = CardDefaults.cardColors(
                        containerColor = if (enough) colors.surface else colors.attention.copy(alpha = 0.08f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (enough) Icons.Outlined.CheckCircle else Icons.Outlined.Warning,
                            contentDescription = null,
                            tint = if (enough) colors.dock else colors.attentionText,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (enough) {
                                    "Needs ${formatBytes(needBytes)}, ${formatBytes(phoneFree)} free on your phone ✓"
                                } else {
                                    "Needs ${formatBytes(needBytes)}, ${formatBytes(phoneFree)} free on your phone"
                                },
                                style = DockBody.copy(fontWeight = FontWeight.Bold),
                                color = if (enough) colors.textPrimary else colors.attentionText
                            )
                            Text(
                                text = if (enough) {
                                    "Enough free space on your phone"
                                } else {
                                    "Free up space on your phone or select fewer files"
                                },
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
    return if (gb >= 1.0) String.format(Locale.US, "%.1f GB", gb) else "${bytes / (1024 * 1024)} MB"
}
