package `in`.soumyaranjansahoo.dockdrop.ui.screens.restore

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.AudioFile
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.text.font.FontWeight
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
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockHeadline
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockMono
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockTheme
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockTitle
import `in`.soumyaranjansahoo.dockdrop.ui.theme.RadiusMd
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

    // Filter items based on active filter and search query
    val filteredEntries = allEntries.filter { entry ->
        val matchesQuery = if (searchQuery.isBlank()) true else {
            entry.path.contains(searchQuery, ignoreCase = true)
        }

        val ext = entry.path.substringAfterLast(".", "").lowercase()
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

        matchesQuery && matchesFilter
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = colors.bg,
        topBar = {
            Column {
                DockTopAppBar(
                    title = "Browse ${set.name}",
                    subtitle = if (allEntries.isNotEmpty()) "${allEntries.size} total files" else null,
                    onBackClick = onBackClick,
                    actions = {
                        if (selectedPaths.isNotEmpty()) {
                            TextButton(onClick = { selectedPaths.clear() }) {
                                Text(
                                    text = "Clear (${selectedPaths.size})",
                                    style = DockBodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = colors.dock
                                )
                            }
                        }
                    }
                )

                // Search & Filter Section
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    // Floating Search Bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search files…", style = DockBodySmall, color = colors.textTertiary) },
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
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(
                                        imageVector = Icons.Outlined.Close,
                                        contentDescription = "Clear",
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

                    Spacer(modifier = Modifier.height(10.dp))

                    // Filter Pills Carousel
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
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(pillBg)
                                    .border(
                                        1.dp,
                                        if (isSelected) colors.dock else colors.outline.copy(alpha = 0.5f),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clickable { selectedFilter = filter }
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
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
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.dock, contentColor = colors.onDock)
                    ) {
                        Text(
                            text = "Restore (${selectedItems.size}) items · ${formatBytes(totalBytes)}",
                            style = DockBody.copy(fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = colors.dock, modifier = Modifier.size(32.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Reading manifest index…", style = DockBodySmall, color = colors.textSecondary)
                }
            }
        } else if (filteredEntries.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                DockEmptyState(
                    title = if (searchQuery.isNotBlank()) "No Matching Files" else "No Files Found",
                    description = if (searchQuery.isNotBlank()) "No files match '$searchQuery' in this category." else "No files were found in this category.",
                    icon = Icons.Outlined.Search
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
            ) {
                items(filteredEntries) { entry ->
                    val isChecked = selectedPaths.contains(entry.path)
                    val fileName = entry.path.substringAfterLast("/")
                    val folder = entry.path.substringBeforeLast("/", missingDelimiterValue = "")
                    val (icon, iconTint, iconBg) = getFileTypeMeta(entry.path)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
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
                                .size(38.dp)
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
                                text = fileName,
                                style = DockBody.copy(fontWeight = FontWeight.SemiBold),
                                color = colors.textPrimary,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (folder.isNotEmpty()) {
                                    Text(
                                        text = "$folder • ",
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
                                        text = " • Removed ${entry.deletedDate}",
                                        style = DockBodySmall.copy(fontSize = 11.sp, color = colors.attention)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Single-file Preview Sheet
        previewEntry?.let { entry ->
            val dateStr = SimpleDateFormat("MMM d, yyyy · HH:mm", Locale.US).format(Date(entry.modifiedMs))
            val isChecked = selectedPaths.contains(entry.path)

            ModalBottomSheet(
                onDismissRequest = { previewEntry = null },
                containerColor = colors.surface,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                ) {
                    Text(
                        text = entry.path.substringAfterLast("/"),
                        style = DockHeadline,
                        color = colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = entry.path, style = DockMono.copy(fontSize = 12.sp), color = colors.textTertiary)
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
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
                                entry.status.replaceFirstChar { it.uppercase() },
                                style = DockMono.copy(fontWeight = FontWeight.Bold),
                                color = if (entry.status == "present") colors.seal else colors.attention
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
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp),
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
                        modifier = Modifier.fillMaxWidth()
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

private fun getFileTypeMeta(path: String): Triple<ImageVector, Color, Color> {
    val ext = path.substringAfterLast(".", "").lowercase()
    return when (ext) {
        "jpg", "jpeg", "png", "heic", "webp", "dng" -> Triple(
            Icons.Outlined.Image,
            Color(0xFF2563EB),
            Color(0xFF2563EB).copy(alpha = 0.12f)
        )
        "mp4", "mov", "mkv", "avi" -> Triple(
            Icons.Outlined.Videocam,
            Color(0xFF6366F1),
            Color(0xFF6366F1).copy(alpha = 0.12f)
        )
        "pdf", "doc", "docx", "txt", "xlsx", "zip" -> Triple(
            Icons.Outlined.Description,
            Color(0xFF059669),
            Color(0xFF059669).copy(alpha = 0.12f)
        )
        "mp3", "m4a", "wav", "flac" -> Triple(
            Icons.Outlined.AudioFile,
            Color(0xFFF59E0B),
            Color(0xFFF59E0B).copy(alpha = 0.12f)
        )
        else -> Triple(
            Icons.AutoMirrored.Outlined.InsertDriveFile,
            Color(0xFF64748B),
            Color(0xFF64748B).copy(alpha = 0.12f)
        )
    }
}

private fun formatBytes(bytes: Long): String {
    val gb = bytes.toDouble() / (1024 * 1024 * 1024)
    return if (gb >= 1.0) String.format("%.1f GB", gb) else "${bytes / (1024 * 1024)} MB"
}
