package `in`.soumyaranjansahoo.dockdrop.ui.screens.restore

import android.net.Uri
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.AudioFile
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material.icons.automirrored.outlined.ViewList
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.soumyaranjansahoo.dockdrop.core.manifest.ManifestEntry
import `in`.soumyaranjansahoo.dockdrop.core.manifest.ManifestRepository
import `in`.soumyaranjansahoo.dockdrop.core.model.SetDefinition
import `in`.soumyaranjansahoo.dockdrop.core.restore.RestoreFilter
import `in`.soumyaranjansahoo.dockdrop.ui.components.DockEmptyState
import `in`.soumyaranjansahoo.dockdrop.ui.components.DockTopAppBar
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockBody
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockBodySmall
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockColors
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockHeadline
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockMono
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockTheme
import `in`.soumyaranjansahoo.dockdrop.ui.theme.LocalReducedMotion
import `in`.soumyaranjansahoo.dockdrop.ui.theme.RadiusMd
import `in`.soumyaranjansahoo.dockdrop.ui.theme.RadiusSheet
import `in`.soumyaranjansahoo.dockdrop.ui.theme.RadiusSm
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestoreBrowserScreen(
    set: SetDefinition,
    manifestRepository: ManifestRepository,
    onRestoreSelected: (List<ManifestEntry>) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = DockTheme.colors

    var allEntries by remember { mutableStateOf<List<ManifestEntry>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(RestoreFilter.ALL) }
    var selectedFolderPrefix by remember { mutableStateOf<String?>(null) }
    var forceGridMode by remember { mutableStateOf<Boolean?>(null) }

    val selectedPaths = remember { mutableStateListOf<String>() }
    var previewEntry by remember { mutableStateOf<ManifestEntry?>(null) }

    LaunchedEffect(set) {
        isLoading = true
        try {
            val gen = manifestRepository.loadCurrentGeneration(
                vaultTreeUri = Uri.parse(set.destinationVaultUriString),
                setId = set.id
            )
            allEntries = gen.entries.values.toList()
        } catch (e: Exception) {
            allEntries = emptyList()
        } finally {
            isLoading = false
        }
    }

    // Top-level folder breadcrumbs derived from manifest
    val topFolders = remember(allEntries) {
        allEntries.mapNotNull { entry ->
            entry.path.substringBefore("/", missingDelimiterValue = "").takeIf { it.isNotBlank() }
        }.distinct().sorted()
    }

    // Filter items based on active filter, breadcrumb folder, and search query
    val filteredEntries = allEntries.filter { entry ->
        val matchesQuery = if (searchQuery.isBlank()) true else {
            entry.path.contains(searchQuery, ignoreCase = true)
        }
        val matchesFolder = selectedFolderPrefix == null || entry.path.startsWith("$selectedFolderPrefix/")

        val ext = entry.path.substringAfterLast(".", "").lowercase(Locale.US)
        val isPhoto = ext in listOf("jpg", "jpeg", "png", "heic", "webp", "dng")
        val isVideo = ext in listOf("mp4", "mov", "mkv", "avi")
        val isDoc = ext in listOf("pdf", "doc", "docx", "txt", "xlsx", "zip")

        val matchesFilter = when (selectedFilter) {
            RestoreFilter.ALL -> entry.status == "present"
            RestoreFilter.PHOTOS -> entry.status == "present" && isPhoto
            RestoreFilter.VIDEOS -> entry.status == "present" && isVideo
            RestoreFilter.DOCUMENTS -> entry.status == "present" && isDoc
            RestoreFilter.DELETED -> entry.status == "deleted"
        }

        matchesQuery && matchesFolder && matchesFilter
    }

    // SCREENS.md S13: image/video folders switch to a thumbnail grid automatically
    val isMediaContext = selectedFilter == RestoreFilter.PHOTOS ||
        selectedFilter == RestoreFilter.VIDEOS ||
        selectedFolderPrefix?.lowercase(Locale.US) in listOf("dcim", "camera", "pictures", "movies", "videos")
    val isGridView = forceGridMode ?: isMediaContext

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = colors.bg,
        topBar = {
            Column {
                DockTopAppBar(
                    title = "Browse ${set.name}",
                    subtitle = if (allEntries.isNotEmpty()) {
                        String.format(Locale.US, "%,d total files", allEntries.size)
                    } else null,
                    onBackClick = onBackClick,
                    actions = {
                        IconButton(
                            onClick = { forceGridMode = !isGridView },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = if (isGridView) Icons.AutoMirrored.Outlined.ViewList else Icons.Outlined.GridView,
                                contentDescription = if (isGridView) "Switch to list view" else "Switch to thumbnail grid",
                                tint = colors.textSecondary
                            )
                        }
                        if (selectedPaths.isNotEmpty()) {
                            TextButton(
                                onClick = { selectedPaths.clear() },
                                modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                            ) {
                                Text(
                                    text = "Clear (${selectedPaths.size})",
                                    style = DockBodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = colors.dock
                                )
                            }
                        }
                    }
                )

                // Search, Breadcrumbs & Filter Section
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search files in manifest…", style = DockBodySmall, color = colors.textTertiary) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Search,
                                contentDescription = null,
                                tint = colors.textSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = { searchQuery = "" },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Close,
                                        contentDescription = "Clear search",
                                        tint = colors.textSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = colors.surface,
                            unfocusedContainerColor = colors.surface,
                            focusedBorderColor = colors.dock,
                            unfocusedBorderColor = colors.outline.copy(alpha = 0.5f),
                            cursorColor = colors.dock
                        )
                    )

                    // Breadcrumb Chips (SCREENS.md S13)
                    if (topFolders.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            item {
                                BreadcrumbChip(
                                    label = "All folders",
                                    selected = selectedFolderPrefix == null,
                                    onClick = { selectedFolderPrefix = null }
                                )
                            }
                            items(topFolders) { folder ->
                                BreadcrumbChip(
                                    label = folder,
                                    selected = selectedFolderPrefix == folder,
                                    onClick = {
                                        selectedFolderPrefix = if (selectedFolderPrefix == folder) null else folder
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Filter Pills Carousel (All · Photos · Videos · Documents · Deleted)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(RestoreFilter.values()) { filter ->
                            val isSelected = selectedFilter == filter
                            val pillBg by animateColorAsState(
                                targetValue = if (isSelected) colors.dock else colors.surface,
                                label = "filterPillBg"
                            )
                            val pillContentColor by animateColorAsState(
                                targetValue = if (isSelected) colors.onDock else colors.textSecondary,
                                label = "filterPillContent"
                            )

                            Box(
                                modifier = Modifier
                                    .defaultMinSize(minHeight = 48.dp)
                                    .clip(RoundedCornerShape(RadiusSm))
                                    .background(pillBg)
                                    .border(
                                        1.dp,
                                        if (isSelected) colors.dock else colors.outline.copy(alpha = 0.5f),
                                        RoundedCornerShape(RadiusSm)
                                    )
                                    .clickable(role = Role.Tab) { selectedFilter = filter }
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = filter.label,
                                    style = DockBodySmall.copy(
                                        color = pillContentColor,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                )
                            }
                        }
                    }
                }
            }
        },
        bottomBar = {
            if (selectedPaths.isNotEmpty()) {
                val selectedItems = allEntries.filter { selectedPaths.contains(it.path) }
                val totalBytes = selectedItems.sumOf { it.sizeBytes }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Button(
                        onClick = { onRestoreSelected(selectedItems) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(RadiusMd),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.dock,
                            contentColor = colors.onDock
                        )
                    ) {
                        Text(
                            text = String.format(
                                Locale.US,
                                "Restore %,d items · %s",
                                selectedItems.size,
                                formatBytes(totalBytes)
                            ),
                            style = DockBody.copy(fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        if (isLoading) {
            // Content-shaped breathing placeholders per DESIGN.md §5.2.8 & SCREENS.md S13 (zero spinners!)
            ManifestBreathingPlaceholder(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        } else if (filteredEntries.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                DockEmptyState(
                    title = if (searchQuery.isNotBlank()) "Nothing matches '$searchQuery'" else "No files found",
                    description = if (searchQuery.isNotBlank()) {
                        "Try a different file name or clear the search filter."
                    } else {
                        "No files were found in this category."
                    },
                    icon = Icons.Outlined.Search
                )
            }
        } else if (isGridView) {
            // Thumbnail Grid Mode for Photos/Videos (SCREENS.md S13)
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 112.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredEntries, key = { it.path }) { entry ->
                    val isChecked = selectedPaths.contains(entry.path)
                    val fileName = entry.path.substringAfterLast("/")
                    val (icon, iconTint, iconBg) = getFileTypeMeta(entry.path, colors)

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(RadiusSm))
                            .background(colors.surface)
                            .border(
                                width = if (isChecked) 2.dp else 1.dp,
                                color = if (isChecked) colors.dock else colors.outline.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(RadiusSm)
                            )
                            .clickable { previewEntry = entry }
                            .padding(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(iconBg)
                                .align(Alignment.Center),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = iconTint,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = { checked ->
                                if (checked) selectedPaths.add(entry.path) else selectedPaths.remove(entry.path)
                            },
                            modifier = Modifier.align(Alignment.TopEnd),
                            colors = CheckboxDefaults.colors(
                                checkedColor = colors.dock,
                                checkmarkColor = colors.onDock,
                                uncheckedColor = colors.outline
                            )
                        )

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomStart)
                        ) {
                            Text(
                                text = middleEllipsizeFileName(fileName, 16),
                                style = DockMono.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                                color = colors.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = formatBytes(entry.sizeBytes),
                                style = DockMono.copy(fontSize = 10.sp),
                                color = colors.textTertiary,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 20.dp)
            ) {
                items(filteredEntries, key = { it.path }) { entry ->
                    val isChecked = selectedPaths.contains(entry.path)
                    val fileName = entry.path.substringAfterLast("/")
                    val folder = entry.path.substringBeforeLast("/", missingDelimiterValue = "")
                    val (icon, iconTint, iconBg) = getFileTypeMeta(entry.path, colors)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 56.dp)
                            .clip(RoundedCornerShape(RadiusSm))
                            .clickable { previewEntry = entry }
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = { checked ->
                                if (checked) selectedPaths.add(entry.path) else selectedPaths.remove(entry.path)
                            },
                            colors = CheckboxDefaults.colors(
                                checkedColor = colors.dock,
                                checkmarkColor = colors.onDock,
                                uncheckedColor = colors.outline
                            )
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(iconBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = iconTint,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = middleEllipsizeFileName(fileName, 32),
                                style = DockMono.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
                                color = colors.textPrimary,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (folder.isNotEmpty()) {
                                    Text(
                                        text = "${middleEllipsizeFileName(folder, 22)} · ",
                                        style = DockMono.copy(fontSize = 11.sp),
                                        color = colors.textTertiary,
                                        maxLines = 1
                                    )
                                }
                                Text(
                                    text = formatBytes(entry.sizeBytes),
                                    style = DockMono.copy(fontSize = 11.sp),
                                    color = colors.textSecondary
                                )
                                if (entry.status == "deleted" && !entry.deletedDate.isNullOrEmpty()) {
                                    Text(
                                        text = " · Removed ${entry.deletedDate}",
                                        style = DockBodySmall.copy(fontSize = 11.sp, color = colors.attentionText)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Single-file Preview Sheet (SCREENS.md S13)
        previewEntry?.let { entry ->
            val dateStr = SimpleDateFormat("MMM d, yyyy · HH:mm", Locale.US).format(Date(entry.modifiedMs))
            val isChecked = selectedPaths.contains(entry.path)

            ModalBottomSheet(
                onDismissRequest = { previewEntry = null },
                containerColor = colors.surface,
                shape = RoundedCornerShape(topStart = RadiusSheet, topEnd = RadiusSheet)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                ) {
                    Text(
                        text = entry.path.substringAfterLast("/"),
                        style = DockHeadline,
                        color = colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = middleEllipsizeFileName(entry.path, 42),
                        style = DockMono.copy(fontSize = 12.sp),
                        color = colors.textTertiary
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(RadiusSm))
                            .background(colors.surfaceRaised)
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("SIZE", style = DockMono.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold), color = colors.textTertiary)
                            Text(formatBytes(entry.sizeBytes), style = DockMono.copy(fontWeight = FontWeight.Bold), color = colors.textPrimary)
                        }
                        Column {
                            Text("MODIFIED", style = DockMono.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold), color = colors.textTertiary)
                            Text(dateStr, style = DockMono.copy(fontWeight = FontWeight.Bold), color = colors.textPrimary)
                        }
                        Column {
                            Text("STATUS", style = DockMono.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold), color = colors.textTertiary)
                            Text(
                                entry.status.replaceFirstChar { it.titlecase(Locale.US) },
                                style = DockMono.copy(fontWeight = FontWeight.Bold),
                                color = if (entry.status == "present") colors.dock else colors.attentionText
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            previewEntry = null
                            onRestoreSelected(listOf(entry))
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(RadiusMd),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.dock, contentColor = colors.onDock)
                    ) {
                        Text("Restore this file", style = DockBody.copy(fontWeight = FontWeight.Bold))
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    TextButton(
                        onClick = {
                            if (isChecked) selectedPaths.remove(entry.path) else selectedPaths.add(entry.path)
                            previewEntry = null
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                    ) {
                        Text(
                            text = if (isChecked) "Remove from selection" else "Add to selection",
                            style = DockBodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = colors.textSecondary
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun BreadcrumbChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val colors = DockTheme.colors
    Row(
        modifier = Modifier
            .defaultMinSize(minHeight = 40.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) colors.dock.copy(alpha = 0.14f) else colors.surfaceRaised)
            .border(
                1.dp,
                if (selected) colors.dock else colors.outline.copy(alpha = 0.4f),
                RoundedCornerShape(10.dp)
            )
            .clickable(role = Role.Button) { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Outlined.Folder,
            contentDescription = null,
            tint = if (selected) colors.dock else colors.textSecondary,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = DockMono.copy(
                fontSize = 11.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) colors.dock else colors.textSecondary
            )
        )
    }
}

/**
 * Content-shaped breathing directory placeholder per DESIGN.md §5.2.8.
 * Replaces generic CircularProgressIndicator when reading manifest entries from SSD.
 */
@Composable
private fun ManifestBreathingPlaceholder(modifier: Modifier = Modifier) {
    val colors = DockTheme.colors
    val isReducedMotion = LocalReducedMotion.current
    val infiniteTransition = rememberInfiniteTransition(label = "manifestBreathe")
    val animatedAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.90f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400),
            repeatMode = RepeatMode.Reverse
        ),
        label = "skeletonAlpha"
    )
    val alpha = if (isReducedMotion) 0.7f else animatedAlpha

    Column(
        modifier = modifier
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .semantics {
                contentDescription = "Reading files on your SSD"
                liveRegion = LiveRegionMode.Polite
            }
    ) {
        Text(
            text = "Reading files on your SSD…",
            style = DockBodySmall.copy(fontWeight = FontWeight.Medium),
            color = colors.textSecondary,
            modifier = Modifier.padding(bottom = 14.dp)
        )

        repeat(6) { index ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(colors.surfaceRaised.copy(alpha = alpha))
                )
                Spacer(modifier = Modifier.width(12.dp))
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.surfaceRaised.copy(alpha = alpha))
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(if (index % 2 == 0) 0.65f else 0.5f)
                            .height(14.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(colors.surfaceRaised.copy(alpha = alpha))
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.35f)
                            .height(10.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(colors.surfaceRaised.copy(alpha = alpha * 0.8f))
                    )
                }
            }
        }
    }
}

private fun getFileTypeMeta(path: String, colors: DockColors): Triple<ImageVector, Color, Color> {
    val ext = path.substringAfterLast(".", "").lowercase(Locale.US)
    return when (ext) {
        "jpg", "jpeg", "png", "heic", "webp", "dng" -> Triple(
            Icons.Outlined.Image,
            colors.categoryPhoto,
            colors.categoryPhoto.copy(alpha = 0.12f)
        )
        "mp4", "mov", "mkv", "avi" -> Triple(
            Icons.Outlined.Videocam,
            colors.categoryVideo,
            colors.categoryVideo.copy(alpha = 0.12f)
        )
        "pdf", "doc", "docx", "txt", "xlsx", "zip" -> Triple(
            Icons.Outlined.Description,
            colors.categoryDocument,
            colors.categoryDocument.copy(alpha = 0.12f)
        )
        "mp3", "m4a", "wav", "flac" -> Triple(
            Icons.Outlined.AudioFile,
            colors.categoryAudio,
            colors.categoryAudio.copy(alpha = 0.12f)
        )
        else -> Triple(
            Icons.AutoMirrored.Outlined.InsertDriveFile,
            colors.categoryOther,
            colors.categoryOther.copy(alpha = 0.12f)
        )
    }
}

private fun middleEllipsizeFileName(value: String, maxChars: Int = 32): String {
    if (value.length <= maxChars) return value
    val half = (maxChars - 1) / 2
    return "${value.take(half)}…${value.takeLast(half)}"
}

private fun formatBytes(bytes: Long): String {
    val gb = bytes.toDouble() / (1024 * 1024 * 1024)
    return if (gb >= 1.0) String.format(Locale.US, "%.1f GB", gb) else "${bytes / (1024 * 1024)} MB"
}
