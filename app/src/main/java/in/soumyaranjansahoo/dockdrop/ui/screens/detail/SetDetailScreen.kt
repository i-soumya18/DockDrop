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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.soumyaranjansahoo.dockdrop.core.model.DeletionPolicy
import `in`.soumyaranjansahoo.dockdrop.core.model.SetDefinition
import `in`.soumyaranjansahoo.dockdrop.core.repository.SetRepository
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockBody
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockBodySmall
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockHeadline
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockMono
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockTheme
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onBack) {
                    Text("← Back", color = colors.dock, style = DockBody)
                }
                Spacer(modifier = Modifier.weight(1f))
            }
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Button(
                    onClick = onBackUpNow,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.dock,
                        contentColor = colors.onDock
                    )
                ) {
                    Text("Back up now", style = DockBody.copy(fontWeight = FontWeight.SemiBold))
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
            // Set Name
            if (isEditingName) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = currentName,
                        onValueChange = { currentName = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        textStyle = DockHeadline,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.dock,
                            cursorColor = colors.dock
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(onClick = {
                        isEditingName = false
                        scope.launch {
                            setRepository.updateSet(set.copy(name = currentName))
                        }
                    }) {
                        Text("Save", color = colors.dock)
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = set.name,
                        style = DockHeadline,
                        color = colors.textPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { isEditingName = true }) {
                        Text("Edit", color = colors.dock, style = DockBodySmall)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Folders protected Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(RadiusMd))
                    .border(1.dp, colors.outline, RoundedCornerShape(RadiusMd)),
                colors = CardDefaults.cardColors(containerColor = colors.surface)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("PROTECTED FOLDERS (${sourceFolders.size})", style = DockMono.copy(fontSize = 11.sp), color = colors.textTertiary)
                    Spacer(modifier = Modifier.height(10.dp))
                    if (sourceFolders.isEmpty()) {
                        Text("No folders configured", style = DockBodySmall, color = colors.textSecondary)
                    } else {
                        sourceFolders.forEach { folder ->
                            Text("• ${folder.label}", style = DockBody.copy(fontWeight = FontWeight.Medium), color = colors.textPrimary)
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Policy Summary Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(RadiusMd))
                    .border(1.dp, colors.outline, RoundedCornerShape(RadiusMd)),
                colors = CardDefaults.cardColors(containerColor = colors.surface)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("BEHAVIOR & POLICIES", style = DockMono.copy(fontSize = 11.sp), color = colors.textTertiary)
                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Deletion policy:", style = DockBodySmall, color = colors.textTertiary)
                    Text(
                        text = when (set.deletionPolicy) {
                            DeletionPolicy.KEEP -> "Keep deleted files on SSD (in _deleted/)"
                            DeletionPolicy.KEEP_N_DAYS -> "Keep deleted files for ${set.retentionDays} days"
                            DeletionPolicy.MIRROR -> "Mirror exactly (deletions purged from SSD)"
                        },
                        style = DockBody.copy(fontWeight = FontWeight.Medium),
                        color = if (set.deletionPolicy == DeletionPolicy.MIRROR) colors.danger else colors.textPrimary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Encryption (Read-only):", style = DockBodySmall, color = colors.textTertiary)
                    Text(
                        text = if (set.isEncrypted) "Encrypted (AES-256-GCM · Passphrase & Recovery Key)" else "Off (Plain mirror folders)",
                        style = DockBody.copy(fontWeight = FontWeight.Medium),
                        color = if (set.isEncrypted) colors.seal else colors.textPrimary
                    )
                    Text(
                        text = "Encryption is chosen when a Set is created and cannot be switched in place.",
                        style = DockBodySmall.copy(fontSize = 12.sp),
                        color = colors.textTertiary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Minimum battery condition:", style = DockBodySmall, color = colors.textTertiary)
                    Text(
                        text = if (set.minBatteryPct >= 100) "Charging only" else "Battery above ${set.minBatteryPct}%",
                        style = DockBody.copy(fontWeight = FontWeight.Medium),
                        color = colors.textPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Dock Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(RadiusMd))
                    .border(1.dp, colors.outline, RoundedCornerShape(RadiusMd)),
                colors = CardDefaults.cardColors(containerColor = colors.surface)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("TARGET SSD", style = DockMono.copy(fontSize = 11.sp), color = colors.textTertiary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(set.dockUserLabel, style = DockBody.copy(fontWeight = FontWeight.Medium), color = colors.textPrimary)
                    Text("Volume UUID: ${set.dockVolumeUuid}", style = DockMono.copy(fontSize = 12.sp), color = colors.textSecondary)
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Danger Zone
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(RadiusMd))
                    .border(1.dp, colors.danger.copy(alpha = 0.5f), RoundedCornerShape(RadiusMd)),
                colors = CardDefaults.cardColors(containerColor = colors.surface)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("DANGER ZONE", style = DockMono.copy(fontSize = 11.sp), color = colors.danger)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Delete this Set. Backup files stay on your SSD.",
                        style = DockBodySmall,
                        color = colors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (showDeleteConfirm) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = {
                                    scope.launch {
                                        setRepository.deleteSet(set.id)
                                        onDeleteConfirmed()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = colors.danger)
                            ) {
                                Text("Confirm Delete", color = colors.textPrimary)
                            }
                            TextButton(onClick = { showDeleteConfirm = false }) {
                                Text("Cancel", color = colors.textTertiary)
                            }
                        }
                    } else {
                        OutlinedButton(
                            onClick = { showDeleteConfirm = true },
                            border = androidx.compose.foundation.BorderStroke(1.dp, colors.danger)
                        ) {
                            Text("Delete Set", color = colors.danger)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
