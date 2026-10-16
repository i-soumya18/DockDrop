package `in`.soumyaranjansahoo.dockdrop.ui.screens.detail

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Policy
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.soumyaranjansahoo.dockdrop.core.model.DeletionPolicy
import `in`.soumyaranjansahoo.dockdrop.core.model.SetDefinition
import `in`.soumyaranjansahoo.dockdrop.core.repository.SetRepository
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
fun SetDetailScreen(
    set: SetDefinition,
    setRepository: SetRepository,
    onBack: () -> Unit,
    onBackUpNow: () -> Unit,
    onDeleteConfirmed: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = DockTheme.colors
    val scope = rememberCoroutineScope()
    val sourceFolders by setRepository.getSourceFolders(set.id).collectAsState(initial = emptyList())

    var isEditingName by remember { mutableStateOf(false) }
    var currentName by remember { mutableStateOf(set.name) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = colors.bg,
        topBar = {
            DockTopAppBar(
                title = if (isEditingName) "Edit Backup Set" else set.name,
                subtitle = "Configuration",
                onBackClick = onBack,
                actions = {
                    if (!isEditingName) {
                        IconButton(onClick = { isEditingName = true }) {
                            Icon(
                                imageVector = Icons.Outlined.Edit,
                                contentDescription = "Edit Name",
                                tint = colors.dock
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                Button(
                    onClick = onBackUpNow,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.dock,
                        contentColor = colors.onDock
                    )
                ) {
                    Icon(
                        imageVector = Icons.Outlined.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Back up now",
                        style = DockBody.copy(fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Inline Rename Editor
            if (isEditingName) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .border(1.dp, colors.dock.copy(alpha = 0.5f), RoundedCornerShape(18.dp)),
                    colors = CardDefaults.cardColors(containerColor = colors.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "RENAME BACKUP SET",
                            style = DockMono.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                            color = colors.dock
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = currentName,
                            onValueChange = { currentName = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = colors.dock,
                                cursorColor = colors.dock
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { isEditingName = false }) {
                                Text("Cancel", color = colors.textTertiary)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    isEditingName = false
                                    scope.launch {
                                        setRepository.updateSet(set.copy(name = currentName))
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = colors.dock),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Save", color = colors.onDock)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Protected Folders Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .border(1.dp, colors.outline.copy(alpha = 0.5f), RoundedCornerShape(18.dp)),
                colors = CardDefaults.cardColors(containerColor = colors.surface)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(colors.dock.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Folder,
                                contentDescription = null,
                                tint = colors.dock,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "PROTECTED FOLDERS (${sourceFolders.size})",
                            style = DockMono.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold),
                            color = colors.textTertiary
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (sourceFolders.isEmpty()) {
                        Text("No folders configured", style = DockBodySmall, color = colors.textSecondary)
                    } else {
                        sourceFolders.forEach { folder ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(colors.surfaceRaised)
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(colors.dock)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = folder.label,
                                    style = DockBody.copy(fontWeight = FontWeight.Medium),
                                    color = colors.textPrimary
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Policy & Retention Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .border(1.dp, colors.outline.copy(alpha = 0.5f), RoundedCornerShape(18.dp)),
                colors = CardDefaults.cardColors(containerColor = colors.surface)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(colors.surfaceRaised),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Policy,
                                contentDescription = null,
                                tint = colors.textSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "BEHAVIOR & POLICIES",
                            style = DockMono.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold),
                            color = colors.textTertiary
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Deletion Policy
                    Text("Deletion policy:", style = DockBodySmall, color = colors.textTertiary)
                    Text(
                        text = when (set.deletionPolicy) {
                            DeletionPolicy.KEEP -> "Keep deleted files on SSD in _deleted/ (Safe)"
                            DeletionPolicy.KEEP_N_DAYS -> "Auto-purge deleted files after ${set.retentionDays} days"
                            DeletionPolicy.MIRROR -> "Mirror exactly (deletions purged immediately)"
                        },
                        style = DockBody.copy(fontWeight = FontWeight.SemiBold),
                        color = if (set.deletionPolicy == DeletionPolicy.MIRROR) colors.danger else colors.textPrimary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Encryption
                    Text("Security:", style = DockBodySmall, color = colors.textTertiary)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Lock,
                            contentDescription = null,
                            tint = if (set.isEncrypted) colors.seal else colors.textSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (set.isEncrypted) "Encrypted (AES-256-GCM · Passphrase Protected)" else "Unencrypted (Direct Mirror Folders)",
                            style = DockBody.copy(fontWeight = FontWeight.SemiBold),
                            color = if (set.isEncrypted) colors.seal else colors.textPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Battery
                    Text("Battery Threshold:", style = DockBodySmall, color = colors.textTertiary)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.BatteryChargingFull,
                            contentDescription = null,
                            tint = colors.dock,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (set.minBatteryPct >= 100) "Charging only" else "Battery above ${set.minBatteryPct}%",
                            style = DockBody.copy(fontWeight = FontWeight.SemiBold),
                            color = colors.textPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Target Drive Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .border(1.dp, colors.outline.copy(alpha = 0.5f), RoundedCornerShape(18.dp)),
                colors = CardDefaults.cardColors(containerColor = colors.surface)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(colors.surfaceRaised),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Storage,
                                contentDescription = null,
                                tint = colors.dock,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = set.dockUserLabel,
                                style = DockBody.copy(fontWeight = FontWeight.Bold),
                                color = colors.textPrimary
                            )
                            Text(
                                text = "UUID: ${set.dockVolumeUuid}",
                                style = DockMono.copy(fontSize = 11.sp),
                                color = colors.textTertiary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Danger Zone Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .border(1.dp, colors.danger.copy(alpha = 0.4f), RoundedCornerShape(18.dp)),
                colors = CardDefaults.cardColors(containerColor = colors.danger.copy(alpha = 0.04f))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Warning,
                            contentDescription = null,
                            tint = colors.danger,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "DELETE THIS BACKUP SET",
                            style = DockMono.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                            color = colors.danger
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Removes this configuration from DockDrop. Your files on the SSD will NOT be deleted.",
                        style = DockBodySmall,
                        color = colors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    if (showDeleteConfirm) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = {
                                    scope.launch {
                                        setRepository.deleteSet(set.id)
                                        onDeleteConfirmed()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = colors.danger),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Confirm Delete", color = Color.White, style = DockBody.copy(fontWeight = FontWeight.SemiBold))
                            }
                            TextButton(onClick = { showDeleteConfirm = false }) {
                                Text("Cancel", color = colors.textTertiary)
                            }
                        }
                    } else {
                        OutlinedButton(
                            onClick = { showDeleteConfirm = true },
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, colors.danger.copy(alpha = 0.6f))
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = null,
                                tint = colors.danger,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Delete Set", color = colors.danger, style = DockBodySmall.copy(fontWeight = FontWeight.SemiBold))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}
