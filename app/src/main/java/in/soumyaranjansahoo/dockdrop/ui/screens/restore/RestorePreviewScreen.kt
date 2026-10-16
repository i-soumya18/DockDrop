package `in`.soumyaranjansahoo.dockdrop.ui.screens.restore

import android.app.Activity
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
import androidx.compose.foundation.lazy.LazyColumn
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

    // Folder Picker for granting missing folders
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

    // Rebuild plan whenever destination or policy changes
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "←",
                    style = DockTitle.copy(fontSize = 24.sp),
                    color = colors.textPrimary,
                    modifier = Modifier
                        .clickable { onBackClick() }
                        .padding(end = 12.dp)
                )
                Text("Restore Preview", style = DockTitle, color = colors.textPrimary)
            }
        },
        bottomBar = {
            val p = plan
            val canRestore = p != null && p.canExecute && !isRecalculating

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.surface)
                    .border(1.dp, colors.outline)
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Button(
                    onClick = {
                        if (p != null) onStartRestore(p)
                    },
                    enabled = canRestore,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.dock,
                        contentColor = colors.onDock,
                        disabledContainerColor = colors.surfaceRaised,
                        disabledContentColor = colors.textTertiary
                    )
                ) {
                    val label = when {
                        isRecalculating -> "Calculating…"
                        p == null -> "Preparing…"
                        p.permissionGaps.isNotEmpty() -> "Grant missing folders first"
                        !p.hasEnoughSpace -> "Not enough space on phone"
                        p.totalFilesToCopy == 0 -> "0 files to copy"
                        else -> "Restore ${p.totalFilesToCopy} files"
                    }
                    Text(label, style = DockBody.copy(fontWeight = FontWeight.Bold))
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
            // 1. Summary Header
            item {
                val totalSelected = selectedEntries.size
                val totalBytes = selectedEntries.sumOf { it.sizeBytes }
                Text(
                    text = "$totalSelected files · ${formatBytes(totalBytes)}",
                    style = DockHeadline,
                    color = colors.textPrimary
                )
                Spacer(modifier = Modifier.height(20.dp))
            }

            // 2. Destination Segmented Control
            item {
                Text("Destination", style = DockBody.copy(fontWeight = FontWeight.SemiBold), color = colors.textPrimary)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.surface)
                        .border(1.dp, colors.outline, RoundedCornerShape(14.dp))
                        .padding(4.dp)
                ) {
                    SegmentButton(
                        text = "Original folders",
                        isSelected = isOriginalDestination,
                        modifier = Modifier.weight(1f),
                        onClick = { isOriginalDestination = true }
                    )
                    SegmentButton(
                        text = "Choose folder",
                        isSelected = !isOriginalDestination,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            isOriginalDestination = false
                            if (customFolderUri == null) {
                                folderPickerLauncher.launch(null)
                            }
                        }
                    )
                }

                if (!isOriginalDestination) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.surfaceRaised)
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = customFolderLabel ?: "No folder selected",
                            style = DockMono.copy(fontSize = 13.sp),
                            color = colors.textPrimary
                        )
                        Text(
                            text = "Change",
                            style = DockBodySmall.copy(color = colors.dock, fontWeight = FontWeight.Bold),
                            modifier = Modifier.clickable { folderPickerLauncher.launch(null) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // 3. Conflict Policy Segmented Control (Keep both · Skip · Overwrite)
            item {
                Text("If a file already exists", style = DockBody.copy(fontWeight = FontWeight.SemiBold), color = colors.textPrimary)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.surface)
                        .border(1.dp, colors.outline, RoundedCornerShape(14.dp))
                        .padding(4.dp)
                ) {
                    ConflictSegmentButton(
                        policy = ConflictPolicy.KEEP_BOTH,
                        isSelected = conflictPolicy == ConflictPolicy.KEEP_BOTH,
                        modifier = Modifier.weight(1f),
                        onClick = { conflictPolicy = ConflictPolicy.KEEP_BOTH }
                    )
                    ConflictSegmentButton(
                        policy = ConflictPolicy.SKIP,
                        isSelected = conflictPolicy == ConflictPolicy.SKIP,
                        modifier = Modifier.weight(1f),
                        onClick = { conflictPolicy = ConflictPolicy.SKIP }
                    )
                    ConflictSegmentButton(
                        policy = ConflictPolicy.OVERWRITE,
                        isSelected = conflictPolicy == ConflictPolicy.OVERWRITE,
                        modifier = Modifier.weight(1f),
                        onClick = { conflictPolicy = ConflictPolicy.OVERWRITE }
                    )
                }

                // Conflict summary line
                val p = plan
                if (p != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (p.conflictCount > 0) {
                            "${p.conflictCount} files already exist in destination"
                        } else {
                            "No conflicts found in destination"
                        },
                        style = DockBodySmall.copy(
                            color = if (p.conflictCount > 0) colors.attention else colors.seal
                        )
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // 4. Permission Gaps (if any)
            val p = plan
            if (p != null && p.permissionGaps.isNotEmpty()) {
                item {
                    Text("Permissions needed", style = DockBody.copy(fontWeight = FontWeight.SemiBold), color = colors.textPrimary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(RadiusMd))
                            .background(colors.surface)
                            .border(1.dp, colors.attention, RoundedCornerShape(RadiusMd))
                    ) {
                        p.permissionGaps.forEachIndexed { idx, gap ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("${gap.folderLabel} folder", style = DockBody.copy(fontWeight = FontWeight.Medium), color = colors.textPrimary)
                                    Text("${gap.fileCount} files · ${formatBytes(gap.totalBytes)}", style = DockBodySmall, color = colors.textSecondary)
                                }
                                Button(
                                    onClick = {
                                        activeGrantingFolder = gap.folderLabel
                                        folderPickerLauncher.launch(null)
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = colors.dock, contentColor = colors.onDock)
                                ) {
                                    Text("Grant")
                                }
                            }
                            if (idx < p.permissionGaps.size - 1) {
                                HorizontalDivider(color = colors.outline)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            // 5. Space Check Row
            item {
                val pPlan = plan
                val phoneFree = pPlan?.phoneFreeBytes ?: 0L
                val needBytes = pPlan?.totalBytesToCopy ?: 0L
                val enough = pPlan?.hasEnoughSpace ?: true

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.surface)
                        .border(1.dp, if (enough) colors.outline else colors.attention, RoundedCornerShape(12.dp))
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (enough) "✓" else "⚠",
                        style = DockMono.copy(color = if (enough) colors.seal else colors.attention, fontSize = 16.sp),
                        modifier = Modifier.padding(end = 12.dp)
                    )
                    Column {
                        Text(
                            text = "Needs ${formatBytes(needBytes)}, ${formatBytes(phoneFree)} free on your phone",
                            style = DockBody.copy(fontWeight = FontWeight.Medium),
                            color = if (enough) colors.textPrimary else colors.attention
                        )
                        if (!enough) {
                            Text(
                                text = "Free up space on your phone to continue with this restore.",
                                style = DockBodySmall,
                                color = colors.textSecondary
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun SegmentButton(
    text: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val colors = DockTheme.colors
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) colors.surfaceRaised else colors.surface)
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = DockBodySmall.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) colors.textPrimary else colors.textSecondary
            )
        )
    }
}

@Composable
private fun ConflictSegmentButton(
    policy: ConflictPolicy,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val colors = DockTheme.colors
    val tint = when {
        !isSelected -> colors.textSecondary
        policy.isDestructive -> colors.danger
        else -> colors.textPrimary
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) colors.surfaceRaised else colors.surface)
            .border(
                if (isSelected && policy.isDestructive) 1.dp else 0.dp,
                if (isSelected && policy.isDestructive) colors.danger else colors.outline,
                RoundedCornerShape(10.dp)
            )
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = policy.displayName,
            style = DockBodySmall.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = tint
            )
        )
    }
}

private fun formatBytes(bytes: Long): String {
    val gb = bytes.toDouble() / (1024 * 1024 * 1024)
    return if (gb >= 1.0) String.format("%.1f GB", gb) else "${bytes / (1024 * 1024)} MB"
}
