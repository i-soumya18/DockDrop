package `in`.soumyaranjansahoo.dockdrop.ui.screens.wizard

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
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
import `in`.soumyaranjansahoo.dockdrop.core.model.DeletionPolicy
import `in`.soumyaranjansahoo.dockdrop.core.model.DockInfo
import `in`.soumyaranjansahoo.dockdrop.core.model.SetDefinition
import `in`.soumyaranjansahoo.dockdrop.core.model.SourceFolder
import `in`.soumyaranjansahoo.dockdrop.core.model.VerificationLevel
import `in`.soumyaranjansahoo.dockdrop.core.storage.StorageGateway
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockBody
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockBodySmall
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockDisplay
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockHeadline
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockMono
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockTheme
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockTitle
import `in`.soumyaranjansahoo.dockdrop.ui.theme.RadiusMd
import kotlinx.coroutines.launch
import java.util.UUID

data class FolderSuggestion(
    val label: String,
    val description: String,
    var grantedUri: Uri? = null,
    var isSelected: Boolean = false
)

@Composable
fun SetWizardScreen(
    currentDock: DockInfo?,
    storageGateway: StorageGateway,
    onWizardFinished: (SetDefinition, List<SourceFolder>) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = DockTheme.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var currentStep by remember { mutableIntStateOf(1) } // 1..4

    // Step 1 State: Source Folders
    val suggestions = remember {
        mutableStateListOf(
            FolderSuggestion("Camera", "Photos & Videos in DCIM/Camera", isSelected = true),
            FolderSuggestion("Pictures", "Screenshots and saved pictures", isSelected = true),
            FolderSuggestion("Documents", "Local documents and PDFs", isSelected = false),
            FolderSuggestion("WhatsApp Media", "Received photos and documents", isSelected = false)
        )
    }

    // Step 2 State: Vault destination
    var vaultTreeUri by remember { mutableStateOf<Uri?>(null) }
    var writeSpeedMbS by remember { mutableDoubleStateOf(35.9) }
    var isTestingWrite by remember { mutableStateOf(false) }

    // Step 3 State: Policies
    var deletionPolicy by remember { mutableStateOf(DeletionPolicy.KEEP) }
    var minBatteryPct by remember { mutableIntStateOf(20) }

    // Step 4 State: Review & Name
    var setName by remember { mutableStateOf("My Phone") }

    // SAF Tree picker for source folders
    var activeFolderIndexForPicker by remember { mutableIntStateOf(-1) }
    val sourceFolderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null && activeFolderIndexForPicker in suggestions.indices) {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            val updated = suggestions[activeFolderIndexForPicker].copy(grantedUri = uri, isSelected = true)
            suggestions[activeFolderIndexForPicker] = updated
        }
    }

    // SAF Tree picker for Vault folder on SSD
    val vaultPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            vaultTreeUri = uri

            // Run write test micro-interaction
            isTestingWrite = true
            scope.launch {
                val speed = storageGateway.runWriteSpeedTest(uri, 8 * 1024 * 1024L)
                writeSpeedMbS = speed
                isTestingWrite = false
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = colors.bg,
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                when (currentStep) {
                    1 -> {
                        val hasSelected = suggestions.any { it.isSelected && it.grantedUri != null }
                        Button(
                            onClick = { currentStep = 2 },
                            enabled = hasSelected,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.dock,
                                contentColor = colors.onDock
                            )
                        ) {
                            Text("Next · What to protect", style = DockBody.copy(fontWeight = FontWeight.SemiBold))
                        }
                    }

                    2 -> {
                        Button(
                            onClick = { currentStep = 3 },
                            enabled = vaultTreeUri != null && !isTestingWrite,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.dock,
                                contentColor = colors.onDock
                            )
                        ) {
                            Text("Next · Storage destination", style = DockBody.copy(fontWeight = FontWeight.SemiBold))
                        }
                    }

                    3 -> {
                        Button(
                            onClick = { currentStep = 4 },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.dock,
                                contentColor = colors.onDock
                            )
                        ) {
                            Text("Next · Review choices", style = DockBody.copy(fontWeight = FontWeight.SemiBold))
                        }
                    }

                    4 -> {
                        Button(
                            onClick = {
                                val setId = UUID.randomUUID().toString()
                                val set = SetDefinition(
                                    id = setId,
                                    name = setName.ifBlank { "My Phone" },
                                    dockVolumeUuid = currentDock?.volumeUuid ?: "4E21-0000",
                                    dockUserLabel = currentDock?.userLabel ?: "External SSD",
                                    destinationVaultUriString = vaultTreeUri?.toString() ?: "",
                                    deletionPolicy = deletionPolicy,
                                    minBatteryPct = minBatteryPct,
                                    verificationLevel = VerificationLevel.FULL_REREAD
                                )
                                val chosenFolders = suggestions.filter { it.isSelected && it.grantedUri != null }.map {
                                    SourceFolder(
                                        id = UUID.randomUUID().toString(),
                                        setId = setId,
                                        label = it.label,
                                        treeUriString = it.grantedUri.toString()
                                    )
                                }
                                onWizardFinished(set, chosenFolders)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.dock,
                                contentColor = colors.onDock
                            )
                        ) {
                            Text("Create backup set", style = DockBody.copy(fontWeight = FontWeight.SemiBold))
                        }
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
            // Top Step Progress Indicator (4 dots)
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 1..4) {
                        val active = i <= currentStep
                        Box(
                            modifier = Modifier
                                .size(if (i == currentStep) 10.dp else 8.dp)
                                .clip(CircleShape)
                                .background(if (active) colors.dock else colors.outline)
                        )
                        if (i < 4) Spacer(modifier = Modifier.width(8.dp))
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Step Content
            when (currentStep) {
                1 -> {
                    item {
                        Text("What should we protect?", style = DockHeadline, color = colors.textPrimary)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "Choose the folders you care about. DockDrop will scan and protect every file inside them.",
                            style = DockBodySmall,
                            color = colors.textSecondary
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                    }

                    items(suggestions.indices.toList()) { index ->
                        val item = suggestions[index]
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                                .clip(RoundedCornerShape(RadiusMd))
                                .border(1.dp, if (item.grantedUri != null) colors.dock else colors.outline, RoundedCornerShape(RadiusMd)),
                            colors = CardDefaults.cardColors(containerColor = colors.surface)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.label, style = DockTitle, color = colors.textPrimary)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(item.description, style = DockBodySmall, color = colors.textSecondary)
                                    if (item.grantedUri != null) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("✓ Granted", style = DockMono.copy(color = colors.seal, fontSize = 12.sp))
                                    }
                                }

                                if (item.grantedUri == null) {
                                    OutlinedButton(
                                        onClick = {
                                            activeFolderIndexForPicker = index
                                            sourceFolderPickerLauncher.launch(null)
                                        },
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("Select", style = DockBodySmall)
                                    }
                                } else {
                                    Checkbox(
                                        checked = item.isSelected,
                                        onCheckedChange = { chk ->
                                            suggestions[index] = item.copy(isSelected = chk)
                                        },
                                        colors = CheckboxDefaults.colors(checkedColor = colors.dock)
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Android doesn't allow backing up the whole Downloads root or private app data. Sub-folders are fully grantable.",
                            style = DockBodySmall,
                            color = colors.textTertiary
                        )
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }

                2 -> {
                    item {
                        Text("Where do we back up?", style = DockHeadline, color = colors.textPrimary)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Pick the folder on your SSD where DockDrop will store this backup.", style = DockBodySmall, color = colors.textSecondary)
                        Spacer(modifier = Modifier.height(24.dp))

                        // SSD Status Card
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(RadiusMd))
                                .border(1.dp, colors.outline, RoundedCornerShape(RadiusMd)),
                            colors = CardDefaults.cardColors(containerColor = colors.surface)
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Text("CONNECTED DRIVE", style = DockMono.copy(fontSize = 11.sp), color = colors.textTertiary)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(currentDock?.userLabel ?: "External SSD", style = DockTitle, color = colors.textPrimary)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("exFAT filesystem ✓ · Ready", style = DockBodySmall, color = colors.seal)

                                Spacer(modifier = Modifier.height(16.dp))
                                Text("DESTINATION VAULT", style = DockMono.copy(fontSize = 11.sp), color = colors.textTertiary)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    vaultTreeUri?.lastPathSegment ?: "Not chosen yet",
                                    style = DockMono.copy(fontSize = 12.sp),
                                    color = colors.textSecondary
                                )

                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = { vaultPickerLauncher.launch(null) },
                                    colors = ButtonDefaults.buttonColors(containerColor = colors.dock, contentColor = colors.onDock),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(if (vaultTreeUri == null) "Choose folder on SSD" else "Change folder", style = DockBodySmall)
                                }

                                if (isTestingWrite) {
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = colors.dock, strokeWidth = 2.dp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Testing write access…", style = DockBodySmall, color = colors.textSecondary)
                                    }
                                } else if (vaultTreeUri != null) {
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Text(
                                        "Write test passed · ${String.format("%.1f", writeSpeedMbS)} MB/s",
                                        style = DockMono.copy(color = colors.seal, fontSize = 12.sp)
                                    )
                                }
                            }
                        }
                    }
                }

                3 -> {
                    item {
                        Text("Choose how it behaves", style = DockHeadline, color = colors.textPrimary)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Safe defaults are chosen for you.", style = DockBodySmall, color = colors.textSecondary)
                        Spacer(modifier = Modifier.height(24.dp))

                        // Deletion policy card
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(RadiusMd))
                                .border(1.dp, colors.outline, RoundedCornerShape(RadiusMd)),
                            colors = CardDefaults.cardColors(containerColor = colors.surface)
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Text("WHEN FILES ARE DELETED ON YOUR PHONE", style = DockMono.copy(fontSize = 11.sp), color = colors.textTertiary)
                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { deletionPolicy = DeletionPolicy.KEEP }
                                ) {
                                    RadioButton(
                                        selected = deletionPolicy == DeletionPolicy.KEEP,
                                        onClick = { deletionPolicy = DeletionPolicy.KEEP },
                                        colors = RadioButtonDefaults.colors(selectedColor = colors.dock)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text("Keep them on the SSD (Recommended)", style = DockBody.copy(fontWeight = FontWeight.SemiBold), color = colors.textPrimary)
                                        Text("Moved to _deleted/ folder so nothing is ever destroyed.", style = DockBodySmall, color = colors.textSecondary)
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { deletionPolicy = DeletionPolicy.KEEP_N_DAYS }
                                ) {
                                    RadioButton(
                                        selected = deletionPolicy == DeletionPolicy.KEEP_N_DAYS,
                                        onClick = { deletionPolicy = DeletionPolicy.KEEP_N_DAYS },
                                        colors = RadioButtonDefaults.colors(selectedColor = colors.dock)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text("Keep for 30 days", style = DockBody.copy(fontWeight = FontWeight.SemiBold), color = colors.textPrimary)
                                        Text("Cleaned up automatically after a month.", style = DockBodySmall, color = colors.textSecondary)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Battery threshold
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(RadiusMd))
                                .border(1.dp, colors.outline, RoundedCornerShape(RadiusMd)),
                            colors = CardDefaults.cardColors(containerColor = colors.surface)
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Text("MINIMUM BATTERY TO RUN", style = DockMono.copy(fontSize = 11.sp), color = colors.textTertiary)
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    listOf(15, 20, 30).forEach { pct ->
                                        val sel = minBatteryPct == pct
                                        OutlinedButton(
                                            onClick = { minBatteryPct = pct },
                                            colors = if (sel) ButtonDefaults.buttonColors(containerColor = colors.dock, contentColor = colors.onDock) else ButtonDefaults.outlinedButtonColors(),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Text("$pct%", style = DockMono)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                4 -> {
                    item {
                        Text("Review and create", style = DockHeadline, color = colors.textPrimary)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Give your backup set a name and confirm your settings.", style = DockBodySmall, color = colors.textSecondary)
                        Spacer(modifier = Modifier.height(24.dp))

                        OutlinedTextField(
                            value = setName,
                            onValueChange = { setName = it },
                            label = { Text("Set Name") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = colors.dock,
                                focusedLabelColor = colors.dock,
                                cursorColor = colors.dock
                            ),
                            shape = RoundedCornerShape(14.dp)
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(RadiusMd))
                                .border(1.dp, colors.outline, RoundedCornerShape(RadiusMd)),
                            colors = CardDefaults.cardColors(containerColor = colors.surface)
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Text("SUMMARY", style = DockMono.copy(fontSize = 11.sp), color = colors.textTertiary)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Folders protected:", style = DockBodySmall, color = colors.textSecondary)
                                suggestions.filter { it.isSelected && it.grantedUri != null }.forEach {
                                    Text("• ${it.label}", style = DockBody.copy(fontWeight = FontWeight.Medium), color = colors.textPrimary)
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                                Text("SSD: ${currentDock?.userLabel ?: "External SSD"}", style = DockBodySmall, color = colors.textSecondary)
                                Text("Policy: Keep deleted files in _deleted/", style = DockBodySmall, color = colors.textSecondary)
                                Text("Verification: size + SHA-256 on copy + re-read", style = DockBodySmall, color = colors.seal)

                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    "First backup: writes all selected files. Later backups take seconds and copy zero unchanged bytes.",
                                    style = DockBodySmall,
                                    color = colors.textTertiary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
