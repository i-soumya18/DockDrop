package `in`.soumyaranjansahoo.dockdrop.ui.screens.restore

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.soumyaranjansahoo.dockdrop.core.manifest.ManifestEntry
import `in`.soumyaranjansahoo.dockdrop.core.manifest.ManifestRepository
import `in`.soumyaranjansahoo.dockdrop.core.model.SetDefinition
import `in`.soumyaranjansahoo.dockdrop.core.restore.RestoreFilter
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
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
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
                    Text(
                        text = "Browse ${set.name}",
                        style = DockTitle,
                        color = colors.textPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    if (selectedPaths.isNotEmpty()) {
                        Text(
                            text = "Clear",
                            style = DockBodySmall.copy(color = colors.dock, fontWeight = FontWeight.Medium),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { selectedPaths.clear() }
                                .padding(4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Search field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search files…", style = DockBodySmall, color = colors.textTertiary) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = colors.surface,
                        unfocusedContainerColor = colors.surface,
                        focusedBorderColor = colors.dock,
                        unfocusedBorderColor = colors.outline,
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Filter chips
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(RestoreFilter.values()) { filter ->
                        val isSelected = selectedFilter == filter
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) colors.surfaceRaised else colors.surface)
                                .border(
                                    1.dp,
                                    if (isSelected) colors.dock else colors.outline,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { selectedFilter = filter }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = filter.label,
                                style = DockBodySmall.copy(
                                    color = if (isSelected) colors.textPrimary else colors.textSecondary,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            // Multi-select primary action bar (SCREENS.md S13)
            if (selectedPaths.isNotEmpty()) {
                val selectedItems = allEntries.filter { selectedPaths.contains(it.path) }
                val totalBytes = selectedItems.sumOf { it.sizeBytes }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.surface)
                        .border(1.dp, colors.outline)
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    Button(
                        onClick = { onRestoreSelected(selectedItems) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.dock, contentColor = colors.onDock)
                    ) {
                        Text(
                            text = "Restore ${selectedItems.size} items · ${formatBytes(totalBytes)}",
                            style = DockBody.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Text("Reading manifest…", style = DockBodySmall, color = colors.textSecondary)
            }
        } else if (filteredEntries.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Text(
                    text = if (searchQuery.isNotBlank()) "Nothing matches '$searchQuery'" else "No files in this category",
                    style = DockBodySmall,
                    color = colors.textSecondary
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

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                previewEntry = entry
                            }
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

                        Spacer(modifier = Modifier.width(8.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = fileName,
                                style = DockBody.copy(fontWeight = FontWeight.Medium),
                                color = colors.textPrimary,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (folder.isNotEmpty()) {
                                    Text(
                                        text = "$folder • ",
                                        style = DockMono.copy(fontSize = 12.sp),
                                        color = colors.textTertiary,
                                        maxLines = 1
                                    )
                                }
                                Text(
                                    text = formatBytes(entry.sizeBytes),
                                    style = DockMono.copy(fontSize = 12.sp),
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

        // Single-file Preview Sheet (SCREENS.md S13)
        previewEntry?.let { entry ->
            val dateStr = SimpleDateFormat("MMM d, yyyy HH:mm", Locale.US).format(Date(entry.modifiedMs))
            val isChecked = selectedPaths.contains(entry.path)

            ModalBottomSheet(
                onDismissRequest = { previewEntry = null },
                containerColor = colors.surface,
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
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
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = entry.path, style = DockMono.copy(fontSize = 12.sp), color = colors.textTertiary)
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("SIZE", style = DockMono.copy(fontSize = 11.sp), color = colors.textTertiary)
                            Text(formatBytes(entry.sizeBytes), style = DockMono, color = colors.textPrimary)
                        }
                        Column {
                            Text("MODIFIED", style = DockMono.copy(fontSize = 11.sp), color = colors.textTertiary)
                            Text(dateStr, style = DockMono, color = colors.textPrimary)
                        }
                        Column {
                            Text("STATUS", style = DockMono.copy(fontSize = 11.sp), color = colors.textTertiary)
                            Text(
                                entry.status.capitalize(Locale.US),
                                style = DockMono,
                                color = if (entry.status == "present") colors.seal else colors.attention
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            previewEntry = null
                            onRestoreSelected(listOf(entry))
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
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
                            style = DockBodySmall,
                            color = colors.textSecondary
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

private fun formatBytes(bytes: Long): String {
    val gb = bytes.toDouble() / (1024 * 1024 * 1024)
    return if (gb >= 1.0) String.format("%.1f GB", gb) else "${bytes / (1024 * 1024)} MB"
}
