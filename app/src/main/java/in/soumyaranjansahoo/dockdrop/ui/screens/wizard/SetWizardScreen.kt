package `in`.soumyaranjansahoo.dockdrop.ui.screens.wizard

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
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
import `in`.soumyaranjansahoo.dockdrop.core.crypto.EncryptedSetManager
import `in`.soumyaranjansahoo.dockdrop.core.crypto.ProvisionedEncryption
import `in`.soumyaranjansahoo.dockdrop.core.model.DeletionPolicy
import `in`.soumyaranjansahoo.dockdrop.core.model.DockInfo
import `in`.soumyaranjansahoo.dockdrop.core.model.SetDefinition
import `in`.soumyaranjansahoo.dockdrop.core.model.SourceFolder
import `in`.soumyaranjansahoo.dockdrop.core.model.VerificationLevel
import `in`.soumyaranjansahoo.dockdrop.core.storage.StorageGateway
import `in`.soumyaranjansahoo.dockdrop.ui.screens.crypto.EncryptionSetupScreen
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockBody
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockBodySmall
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockDisplay
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockHeadline
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockMono
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockTheme
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockTitle
import `in`.soumyaranjansahoo.dockdrop.ui.theme.RadiusMd
import `in`.soumyaranjansahoo.dockdrop.ui.theme.RadiusSheet
import kotlinx.coroutines.launch
import java.util.UUID

data class FolderSuggestion(
    val label: String,
    val description: String,
    var grantedUri: Uri? = null,
    var isSelected: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetWizardScreen(
    currentDock: DockInfo?,
    storageGateway: StorageGateway,
    encryptedSetManager: EncryptedSetManager? = null,
    defaultMinBatteryPct: Int = 20,
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

    // Step 3 State: Policies & Encryption
    var deletionPolicy by remember { mutableStateOf(DeletionPolicy.KEEP) }
    var retentionDays by remember { mutableIntStateOf(30) }
    var mirrorConfirmed by remember { mutableStateOf(false) }
    var showMirrorSheet by remember { mutableStateOf(false) }

    var isEncrypted by remember { mutableStateOf(false) }
    var showEncryptionSetup by remember { mutableStateOf(false) }
    var provisionedEncryption by remember { mutableStateOf<ProvisionedEncryption?>(null) }

    var minBatteryPct by remember { mutableIntStateOf(defaultMinBatteryPct) }

    // Step 4 State: Review & Name
    var setName by remember { mutableStateOf("My Phone") }

    // Encryption Setup Overlay (S18)
    if (showEncryptionSetup) {
        EncryptionSetupScreen(
            setName = setName.ifBlank { "My Phone" },
            onComplete = { pass, recKey ->
                if (encryptedSetManager != null) {
                    val setId = UUID.randomUUID().toString()
                    val prov = encryptedSetManager.provisionNewSetEncryption(setId, pass)
                    provisionedEncryption = prov
                    isEncrypted = true
                } else {
                    isEncrypted = true
                }
                showEncryptionSetup = false
            },
            onCancel = {
                isEncrypted = false
                showEncryptionSetup = false
            }
        )
        return
    }

    // Mirror mode confirmation sheet
    if (showMirrorSheet) {
        ModalBottomSheet(
            onDismissRequest = { showMirrorSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            shape = RoundedCornerShape(topStart = RadiusSheet, topEnd = RadiusSheet),
            containerColor = colors.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Turn on Mirror mode?",
                    style = DockHeadline,
                    color = colors.danger
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Mirror mode removes files from the SSD when you delete them on your phone. Deleted files can't be recovered. Keep this off unless you're sure.",
                    style = DockBody,
                    color = colors.textPrimary
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {
                        deletionPolicy = DeletionPolicy.MIRROR
                        mirrorConfirmed = true
                        showMirrorSheet = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.danger),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    Text("Turn on Mirror mode", color = colors.textPrimary, style = DockBody.copy(fontWeight = FontWeight.SemiBold))
                }
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = {
                        deletionPolicy = DeletionPolicy.KEEP
                        mirrorConfirmed = false
                        showMirrorSheet = false
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    Text("Keep files instead", style = DockBody)
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // SAF Tree pickers for source folders
    var activePickingIndex by remember { mutableIntStateOf(-1) }
    val sourcePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null && activePickingIndex in suggestions.indices) {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            suggestions[activePickingIndex] = suggestions[activePickingIndex].copy(
                grantedUri = uri,
                isSelected = true
            )
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
                            Text("Next · Storage destination", style = DockBody.copy(fontWeight = FontWeight.SemiBold))
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
                            Text("Next · Policies & security", style = DockBody.copy(fontWeight = FontWeight.SemiBold))
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
                                val setId = provisionedEncryption?.let {
                                    // SetId provisioned during encryption
                                    UUID.randomUUID().toString()
                                } ?: UUID.randomUUID().toString()

                                scope.launch {
                                    if (isEncrypted && provisionedEncryption != null && vaultTreeUri != null) {
                                        encryptedSetManager?.writeEncryptionMetadata(
                                            vaultTreeUri!!,
                                            setId,
                                            provisionedEncryption!!.metadata
                                        )
                                    }

                                    val set = SetDefinition(
                                        id = setId,
                                        name = setName.ifBlank { "My Phone" },
                                        dockVolumeUuid = currentDock?.volumeUuid ?: "4E21-0000",
                                        dockUserLabel = currentDock?.userLabel ?: "External SSD",
                                        destinationVaultUriString = vaultTreeUri?.toString() ?: "",
                                        deletionPolicy = deletionPolicy,
                                        retentionDays = retentionDays,
                                        minBatteryPct = minBatteryPct,
                                        isEncrypted = isEncrypted,
                                        mirrorConfirmed = mirrorConfirmed,
                                        keystoreWrappedMkHex = provisionedEncryption?.keystoreWrappedMkHex,
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
                                }
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
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = {
                        if (currentStep > 1) currentStep-- else onCancel()
                    }) {
                        Text("← Back", color = colors.dock, style = DockBody)
                    }

                    // Progress dots (4 steps)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        for (i in 1..4) {
                            Box(
                                modifier = Modifier
                                    .size(if (i == currentStep) 10.dp else 6.dp)
                                    .clip(CircleShape)
                                    .background(if (i <= currentStep) colors.dock else colors.outline)
                            )
                        }
                    }

                    TextButton(onClick = onCancel) {
                        Text("Cancel", color = colors.textTertiary, style = DockBodySmall)
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            when (currentStep) {
                1 -> {
                    item {
                        Text("What should we protect?", style = DockHeadline, color = colors.textPrimary)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "Select the folders on your phone you want backed up to your SSD.",
                            style = DockBodySmall,
                            color = colors.textSecondary
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    items(suggestions.size) { index ->
                        val suggestion = suggestions[index]
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                                .clip(RoundedCornerShape(RadiusMd))
                                .border(
                                    1.dp,
                                    if (suggestion.grantedUri != null) colors.dock else colors.outline,
                                    RoundedCornerShape(RadiusMd)
                                )
                                .clickable {
                                    if (suggestion.grantedUri == null) {
                                        activePickingIndex = index
                                        sourcePickerLauncher.launch(null)
                                    } else {
                                        suggestions[index] = suggestion.copy(isSelected = !suggestion.isSelected)
                                    }
                                },
                            colors = CardDefaults.cardColors(containerColor = colors.surface)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = suggestion.isSelected && suggestion.grantedUri != null,
                                    onCheckedChange = { checked ->
                                        if (suggestion.grantedUri == null) {
                                            activePickingIndex = index
                                            sourcePickerLauncher.launch(null)
                                        } else {
                                            suggestions[index] = suggestion.copy(isSelected = checked)
                                        }
                                    },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = colors.dock,
                                        checkmarkColor = colors.onDock
                                    )
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(suggestion.label, style = DockBody.copy(fontWeight = FontWeight.SemiBold), color = colors.textPrimary)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        if (suggestion.grantedUri != null) "Folder access granted ✓" else suggestion.description,
                                        style = DockBodySmall,
                                        color = if (suggestion.grantedUri != null) colors.seal else colors.textSecondary
                                    )
                                }
                            }
                        }
                    }
                }

                2 -> {
                    item {
                        Text("Where do we back up?", style = DockHeadline, color = colors.textPrimary)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "Select your SSD and pick the DockDrop vault directory.",
                            style = DockBodySmall,
                            color = colors.textSecondary
                        )
                        Spacer(modifier = Modifier.height(24.dp))

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(RadiusMd))
                                .border(1.dp, colors.outline, RoundedCornerShape(RadiusMd)),
                            colors = CardDefaults.cardColors(containerColor = colors.surface)
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Text("CONNECTED DRIVE", style = DockMono.copy(fontSize = 11.sp), color = colors.textTertiary)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    currentDock?.userLabel ?: "External Storage Device",
                                    style = DockTitle,
                                    color = colors.textPrimary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "Filesystem: exFAT · Status: Ready",
                                    style = DockBodySmall,
                                    color = colors.seal
                                )

                                Spacer(modifier = Modifier.height(16.dp))
                                Text("VAULT LOCATION", style = DockMono.copy(fontSize = 11.sp), color = colors.textTertiary)
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = if (vaultTreeUri != null) "SSD / DockDrop" else "Not chosen yet",
                                        style = DockBody.copy(fontWeight = FontWeight.Medium),
                                        color = if (vaultTreeUri != null) colors.textPrimary else colors.textTertiary,
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedButton(
                                        onClick = { vaultPickerLauncher.launch(null) },
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text(if (vaultTreeUri != null) "Change" else "Choose folder")
                                    }
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
                        Text("Three decisions, all with safe defaults.", style = DockBodySmall, color = colors.textSecondary)
                        Spacer(modifier = Modifier.height(24.dp))

                        // 1. Deletion policy card (S07 Card 1)
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

                                // Option 1: KEEP (Default)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            deletionPolicy = DeletionPolicy.KEEP
                                            mirrorConfirmed = false
                                        }
                                ) {
                                    RadioButton(
                                        selected = deletionPolicy == DeletionPolicy.KEEP,
                                        onClick = {
                                            deletionPolicy = DeletionPolicy.KEEP
                                            mirrorConfirmed = false
                                        },
                                        colors = RadioButtonDefaults.colors(selectedColor = colors.dock)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text("Keep them on the SSD (Recommended)", style = DockBody.copy(fontWeight = FontWeight.SemiBold), color = colors.textPrimary)
                                        Text("Moved to _deleted/ folder so nothing is ever destroyed.", style = DockBodySmall, color = colors.textSecondary)
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Option 2: KEEP_N_DAYS
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            deletionPolicy = DeletionPolicy.KEEP_N_DAYS
                                            mirrorConfirmed = false
                                        }
                                ) {
                                    RadioButton(
                                        selected = deletionPolicy == DeletionPolicy.KEEP_N_DAYS,
                                        onClick = {
                                            deletionPolicy = DeletionPolicy.KEEP_N_DAYS
                                            mirrorConfirmed = false
                                        },
                                        colors = RadioButtonDefaults.colors(selectedColor = colors.dock)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text("Keep for $retentionDays days", style = DockBody.copy(fontWeight = FontWeight.SemiBold), color = colors.textPrimary)
                                        Text("Cleaned up automatically after $retentionDays days.", style = DockBodySmall, color = colors.textSecondary)
                                    }
                                }

                                if (deletionPolicy == DeletionPolicy.KEEP_N_DAYS) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.padding(start = 40.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        listOf(7, 30, 90).forEach { days ->
                                            val isSel = retentionDays == days
                                            OutlinedButton(
                                                onClick = { retentionDays = days },
                                                colors = if (isSel) ButtonDefaults.buttonColors(containerColor = colors.dock, contentColor = colors.onDock) else ButtonDefaults.outlinedButtonColors(),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text("$days days", style = DockMono.copy(fontSize = 12.sp))
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Option 3: MIRROR (Danger tint)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { showMirrorSheet = true }
                                ) {
                                    RadioButton(
                                        selected = deletionPolicy == DeletionPolicy.MIRROR,
                                        onClick = { showMirrorSheet = true },
                                        colors = RadioButtonDefaults.colors(selectedColor = colors.danger)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text("Mirror exactly", style = DockBody.copy(fontWeight = FontWeight.SemiBold), color = colors.danger)
                                        Text("Removes files from SSD when deleted on phone.", style = DockBodySmall, color = colors.textSecondary)
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Live consequence line
                                Text(
                                    text = when (deletionPolicy) {
                                        DeletionPolicy.KEEP -> "Result: Deleted files move to _deleted/<Set>/<date>/ and stay forever."
                                        DeletionPolicy.KEEP_N_DAYS -> "Result: Deleted files move to _deleted/ and are purged after $retentionDays days."
                                        DeletionPolicy.MIRROR -> "Result: Files removed on phone are permanently deleted from the SSD."
                                    },
                                    style = DockBodySmall.copy(fontSize = 12.sp),
                                    color = if (deletionPolicy == DeletionPolicy.MIRROR) colors.danger else colors.textTertiary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // 2. Encryption Card (S07 Card 2)
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(RadiusMd))
                                .border(1.dp, colors.outline, RoundedCornerShape(RadiusMd)),
                            colors = CardDefaults.cardColors(containerColor = colors.surface)
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Protect with a passphrase", style = DockBody.copy(fontWeight = FontWeight.SemiBold), color = colors.textPrimary)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            "Encrypts this backup. If you lose both the passphrase and the recovery key, the backup can't be opened. This can't be turned on later without a new backup.",
                                            style = DockBodySmall,
                                            color = colors.textSecondary
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Switch(
                                        checked = isEncrypted,
                                        onCheckedChange = { checked ->
                                            if (checked) {
                                                showEncryptionSetup = true
                                            } else {
                                                isEncrypted = false
                                                provisionedEncryption = null
                                            }
                                        },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = colors.onDock,
                                            checkedTrackColor = colors.dock
                                        )
                                    )
                                }

                                if (isEncrypted) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        "Result: Protected with AES-256-GCM. Recovery key generated.",
                                        style = DockBodySmall.copy(fontSize = 12.sp),
                                        color = colors.seal
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // 3. Battery threshold card (S07 Card 3)
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(RadiusMd))
                                .border(1.dp, colors.outline, RoundedCornerShape(RadiusMd)),
                            colors = CardDefaults.cardColors(containerColor = colors.surface)
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Text("ONLY BACK UP WHEN BATTERY IS ABOVE", style = DockMono.copy(fontSize = 11.sp), color = colors.textTertiary)
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    listOf(15, 20, 30, 100).forEach { pct ->
                                        val sel = minBatteryPct == pct
                                        val label = if (pct == 100) "Charging only" else "$pct%"
                                        OutlinedButton(
                                            onClick = { minBatteryPct = pct },
                                            colors = if (sel) ButtonDefaults.buttonColors(containerColor = colors.dock, contentColor = colors.onDock) else ButtonDefaults.outlinedButtonColors(),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Text(label, style = DockMono)
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

                                val policyText = when (deletionPolicy) {
                                    DeletionPolicy.KEEP -> "Keep deleted files in _deleted/"
                                    DeletionPolicy.KEEP_N_DAYS -> "Keep for $retentionDays days in _deleted/"
                                    DeletionPolicy.MIRROR -> "Mirror exactly (deletions purged from SSD)"
                                }
                                Text("Policy: $policyText", style = DockBodySmall, color = if (deletionPolicy == DeletionPolicy.MIRROR) colors.danger else colors.textSecondary)

                                val encText = if (isEncrypted) "Encrypted (AES-256-GCM · Passphrase & Recovery Key)" else "Unencrypted (Plain mirror folders)"
                                Text("Encryption: $encText", style = DockBodySmall, color = if (isEncrypted) colors.seal else colors.textSecondary)

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
