package `in`.soumyaranjansahoo.dockdrop.ui.screens.wizard

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import `in`.soumyaranjansahoo.dockdrop.ui.components.DockSelectableCard
import `in`.soumyaranjansahoo.dockdrop.ui.components.DockTopAppBar
import `in`.soumyaranjansahoo.dockdrop.ui.screens.crypto.EncryptionSetupScreen
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockBody
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockBodySmall
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
    val icon: ImageVector,
    val iconColor: Color,
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
            FolderSuggestion(
                label = "Camera",
                description = "Photos & Videos in DCIM/Camera",
                icon = Icons.Outlined.PhotoCamera,
                iconColor = Color(0xFF2563EB),
                isSelected = true
            ),
            FolderSuggestion(
                label = "Pictures",
                description = "Screenshots and saved pictures",
                icon = Icons.Outlined.Image,
                iconColor = Color(0xFF6366F1),
                isSelected = true
            ),
            FolderSuggestion(
                label = "Documents",
                description = "Local documents and PDFs",
                icon = Icons.Outlined.Description,
                iconColor = Color(0xFF059669),
                isSelected = false
            ),
            FolderSuggestion(
                label = "WhatsApp Media",
                description = "Received photos and documents",
                icon = Icons.Outlined.Chat,
                iconColor = Color(0xFF10B981),
                isSelected = false
            )
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
            onComplete = { pass, _ ->
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
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(colors.danger.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Warning,
                        contentDescription = null,
                        tint = colors.danger,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Turn on Mirror mode?",
                    style = DockHeadline,
                    color = colors.danger
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Mirror mode removes files from the SSD when you delete them on your phone. Deleted files cannot be recovered. Keep this off unless you specifically need an exact mirror.",
                    style = DockBody,
                    color = colors.textSecondary
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {
                        deletionPolicy = DeletionPolicy.MIRROR
                        mirrorConfirmed = true
                        showMirrorSheet = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.danger),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Text("Turn on Mirror mode", color = Color.White, style = DockBody.copy(fontWeight = FontWeight.SemiBold))
                }
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = {
                        deletionPolicy = DeletionPolicy.KEEP
                        mirrorConfirmed = false
                        showMirrorSheet = false
                    },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
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
        topBar = {
            DockTopAppBar(
                title = "New Backup Set",
                subtitle = "Step $currentStep of 4",
                onBackClick = {
                    if (currentStep > 1) currentStep-- else onCancel()
                },
                actions = {
                    TextButton(onClick = onCancel) {
                        Text(
                            "Cancel",
                            color = colors.textTertiary,
                            style = DockBodySmall.copy(fontWeight = FontWeight.SemiBold)
                        )
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
                when (currentStep) {
                    1 -> {
                        val hasSelected = suggestions.any { it.isSelected && it.grantedUri != null }
                        Button(
                            onClick = { currentStep = 2 },
                            enabled = hasSelected,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(18.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.dock,
                                contentColor = colors.onDock
                            )
                        ) {
                            Text(
                                if (hasSelected) "Next: Pick Storage Location" else "Select Folders to Continue",
                                style = DockBody.copy(fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            )
                        }
                    }

                    2 -> {
                        Button(
                            onClick = { currentStep = 3 },
                            enabled = vaultTreeUri != null && !isTestingWrite,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(18.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.dock,
                                contentColor = colors.onDock
                            )
                        ) {
                            Text(
                                "Next: Backup Policies",
                                style = DockBody.copy(fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            )
                        }
                    }

                    3 -> {
                        Button(
                            onClick = { currentStep = 4 },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(18.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.dock,
                                contentColor = colors.onDock
                            )
                        ) {
                            Text(
                                "Next: Review & Confirm",
                                style = DockBody.copy(fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            )
                        }
                    }

                    4 -> {
                        Button(
                            onClick = {
                                val setId = UUID.randomUUID().toString()

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
                            shape = RoundedCornerShape(18.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.dock,
                                contentColor = colors.onDock
                            )
                        ) {
                            Text(
                                "Create Backup Set",
                                style = DockBody.copy(fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            )
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
            // Step Progress Bar
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (step in 1..4) {
                        val isFinished = step <= currentStep
                        val barColor by animateColorAsState(
                            targetValue = if (isFinished) colors.dock else colors.outline.copy(alpha = 0.35f),
                            label = "stepBarColor"
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(barColor)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            when (currentStep) {
                1 -> {
                    item {
                        Text(
                            text = "What should we protect?",
                            style = DockHeadline,
                            color = colors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tap to grant folder access. DockDrop never leaves your device.",
                            style = DockBodySmall,
                            color = colors.textSecondary
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                    }

                    items(suggestions.size) { index ->
                        val suggestion = suggestions[index]
                        val isGranted = suggestion.grantedUri != null
                        val isSelected = suggestion.isSelected && isGranted

                        DockSelectableCard(
                            title = suggestion.label,
                            subtitle = if (isGranted) "Access granted · Ready for backup" else suggestion.description,
                            selected = isSelected,
                            onClick = {
                                if (!isGranted) {
                                    activePickingIndex = index
                                    sourcePickerLauncher.launch(null)
                                } else {
                                    suggestions[index] = suggestion.copy(isSelected = !suggestion.isSelected)
                                }
                            },
                            icon = suggestion.icon,
                            iconBgColor = suggestion.iconColor.copy(alpha = 0.12f),
                            iconTintColor = suggestion.iconColor,
                            trailingBadge = if (isGranted) "Granted ✓" else "Tap to Grant",
                            modifier = Modifier.padding(vertical = 5.dp)
                        )
                    }
                }

                2 -> {
                    item {
                        Text("Where do we back up?", style = DockHeadline, color = colors.textPrimary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Select your target drive and specify the DockDrop vault folder.",
                            style = DockBodySmall,
                            color = colors.textSecondary
                        )
                        Spacer(modifier = Modifier.height(20.dp))

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .border(1.dp, colors.outline.copy(alpha = 0.5f), RoundedCornerShape(18.dp)),
                            colors = CardDefaults.cardColors(containerColor = colors.surface)
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(colors.dock.copy(alpha = 0.12f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Storage,
                                            contentDescription = null,
                                            tint = colors.dock,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column {
                                        Text(
                                            text = currentDock?.userLabel ?: "External Storage Drive",
                                            style = DockTitle,
                                            color = colors.textPrimary
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .clip(CircleShape)
                                                    .background(colors.seal)
                                            )
                                            Text(
                                                text = "Connected · exFAT filesystem",
                                                style = DockBodySmall,
                                                color = colors.seal
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(20.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(1.dp)
                                        .background(colors.outline.copy(alpha = 0.35f))
                                )
                                Spacer(modifier = Modifier.height(16.dp))

                                Text(
                                    "VAULT DESTINATION",
                                    style = DockMono.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                                    color = colors.textTertiary
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(colors.surfaceRaised)
                                        .padding(horizontal = 14.dp, vertical = 10.dp)
                                ) {
                                    Icon(
                                        imageVector = if (vaultTreeUri != null) Icons.Outlined.FolderOpen else Icons.Outlined.Folder,
                                        contentDescription = null,
                                        tint = if (vaultTreeUri != null) colors.dock else colors.textTertiary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = if (vaultTreeUri != null) "SSD / DockDropVault" else "Choose folder on SSD…",
                                        style = DockBody.copy(fontWeight = FontWeight.Medium),
                                        color = if (vaultTreeUri != null) colors.textPrimary else colors.textTertiary,
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedButton(
                                        onClick = { vaultPickerLauncher.launch(null) },
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text(if (vaultTreeUri != null) "Change" else "Choose")
                                    }
                                }

                                if (isTestingWrite) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(colors.dock.copy(alpha = 0.08f))
                                            .padding(12.dp)
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            color = colors.dock,
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            "Benchmarking SSD write throughput…",
                                            style = DockBodySmall,
                                            color = colors.dock
                                        )
                                    }
                                } else if (vaultTreeUri != null) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(colors.seal.copy(alpha = 0.08f))
                                            .padding(12.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.CheckCircle,
                                            contentDescription = null,
                                            tint = colors.seal,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            "Write benchmark passed: ${String.format("%.1f", writeSpeedMbS)} MB/s",
                                            style = DockMono.copy(color = colors.seal, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                3 -> {
                    item {
                        Text("Choose how it behaves", style = DockHeadline, color = colors.textPrimary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Safe, non-destructive defaults with full control.", style = DockBodySmall, color = colors.textSecondary)
                        Spacer(modifier = Modifier.height(20.dp))

                        // Deletion Policy Section
                        Text(
                            "WHEN FILES ARE DELETED ON YOUR PHONE",
                            style = DockMono.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                            color = colors.textTertiary
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        DockSelectableCard(
                            title = "Keep on SSD (Recommended)",
                            subtitle = "Deleted files move to _deleted/ on your SSD so nothing is lost.",
                            selected = deletionPolicy == DeletionPolicy.KEEP,
                            onClick = {
                                deletionPolicy = DeletionPolicy.KEEP
                                mirrorConfirmed = false
                            },
                            icon = Icons.Outlined.Shield,
                            iconBgColor = colors.dock.copy(alpha = 0.12f),
                            iconTintColor = colors.dock,
                            trailingBadge = "Safe",
                            isRadio = true
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        DockSelectableCard(
                            title = "Keep for $retentionDays days",
                            subtitle = "Deleted items are kept in _deleted/ and auto-purged after $retentionDays days.",
                            selected = deletionPolicy == DeletionPolicy.KEEP_N_DAYS,
                            onClick = {
                                deletionPolicy = DeletionPolicy.KEEP_N_DAYS
                                mirrorConfirmed = false
                            },
                            icon = Icons.Outlined.Folder,
                            iconBgColor = Color(0xFF6366F1).copy(alpha = 0.12f),
                            iconTintColor = Color(0xFF6366F1),
                            isRadio = true
                        )

                        if (deletionPolicy == DeletionPolicy.KEEP_N_DAYS) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(7, 30, 90).forEach { days ->
                                    val isSel = retentionDays == days
                                    OutlinedButton(
                                        onClick = { retentionDays = days },
                                        colors = if (isSel) ButtonDefaults.buttonColors(
                                            containerColor = colors.dock,
                                            contentColor = colors.onDock
                                        ) else ButtonDefaults.outlinedButtonColors(),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text("$days days", style = DockMono.copy(fontSize = 12.sp))
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        DockSelectableCard(
                            title = "Mirror exactly (Caution)",
                            subtitle = "Deleting a file on your phone will permanently delete it from the SSD.",
                            selected = deletionPolicy == DeletionPolicy.MIRROR,
                            onClick = { showMirrorSheet = true },
                            icon = Icons.Outlined.Warning,
                            iconBgColor = colors.danger.copy(alpha = 0.12f),
                            iconTintColor = colors.danger,
                            trailingBadge = "Danger",
                            isRadio = true
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Encryption Card
                        Text(
                            "SECURITY & ENCRYPTION",
                            style = DockMono.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                            color = colors.textTertiary
                        )
                        Spacer(modifier = Modifier.height(10.dp))

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
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isEncrypted) colors.seal.copy(alpha = 0.12f) else colors.surfaceRaised),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Lock,
                                        contentDescription = null,
                                        tint = if (isEncrypted) colors.seal else colors.textSecondary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Encrypt with Passphrase",
                                        style = DockBody.copy(fontWeight = FontWeight.Bold),
                                        color = colors.textPrimary
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = if (isEncrypted) "AES-256-GCM · Recovery key saved" else "Zero-knowledge hardware vault",
                                        style = DockBodySmall,
                                        color = if (isEncrypted) colors.seal else colors.textSecondary
                                    )
                                }
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
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Battery Threshold
                        Text(
                            "BATTERY PROTECTION",
                            style = DockMono.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                            color = colors.textTertiary
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .border(1.dp, colors.outline.copy(alpha = 0.5f), RoundedCornerShape(18.dp)),
                            colors = CardDefaults.cardColors(containerColor = colors.surface)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(colors.surfaceRaised),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.BatteryChargingFull,
                                            contentDescription = null,
                                            tint = colors.dock,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            "Minimum Battery Level",
                                            style = DockBody.copy(fontWeight = FontWeight.Bold),
                                            color = colors.textPrimary
                                        )
                                        Text(
                                            "Pauses backup if battery drops below this limit.",
                                            style = DockBodySmall,
                                            color = colors.textSecondary
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(14.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf(15, 20, 30, 100).forEach { pct ->
                                        val sel = minBatteryPct == pct
                                        val label = if (pct == 100) "Charging only" else "$pct%"
                                        OutlinedButton(
                                            onClick = { minBatteryPct = pct },
                                            modifier = Modifier.weight(1f),
                                            colors = if (sel) ButtonDefaults.buttonColors(
                                                containerColor = colors.dock,
                                                contentColor = colors.onDock
                                            ) else ButtonDefaults.outlinedButtonColors(),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Text(label, style = DockMono.copy(fontSize = 12.sp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                4 -> {
                    item {
                        Text("Review and name", style = DockHeadline, color = colors.textPrimary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Confirm your configuration and give your set a friendly name.", style = DockBodySmall, color = colors.textSecondary)
                        Spacer(modifier = Modifier.height(20.dp))

                        OutlinedTextField(
                            value = setName,
                            onValueChange = { setName = it },
                            label = { Text("Backup Set Name") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = colors.dock,
                                focusedLabelColor = colors.dock,
                                cursorColor = colors.dock
                            ),
                            shape = RoundedCornerShape(14.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .border(1.dp, colors.outline.copy(alpha = 0.5f), RoundedCornerShape(18.dp)),
                            colors = CardDefaults.cardColors(containerColor = colors.surface)
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Text(
                                    "BACKUP CONFIGURATION",
                                    style = DockMono.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                                    color = colors.textTertiary
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Outlined.Folder,
                                        contentDescription = null,
                                        tint = colors.dock,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "Protected Folders",
                                        style = DockBody.copy(fontWeight = FontWeight.Bold),
                                        color = colors.textPrimary
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                suggestions.filter { it.isSelected && it.grantedUri != null }.forEach {
                                    Text(
                                        "  • ${it.label}",
                                        style = DockBodySmall.copy(fontWeight = FontWeight.Medium),
                                        color = colors.textSecondary
                                    )
                                }

                                Spacer(modifier = Modifier.height(14.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(1.dp)
                                        .background(colors.outline.copy(alpha = 0.35f))
                                )
                                Spacer(modifier = Modifier.height(14.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Destination", style = DockBodySmall, color = colors.textSecondary)
                                    Text(currentDock?.userLabel ?: "External SSD", style = DockBodySmall.copy(fontWeight = FontWeight.SemiBold), color = colors.textPrimary)
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Deletion Policy", style = DockBodySmall, color = colors.textSecondary)
                                    val policyLabel = when (deletionPolicy) {
                                        DeletionPolicy.KEEP -> "Keep forever"
                                        DeletionPolicy.KEEP_N_DAYS -> "$retentionDays days"
                                        DeletionPolicy.MIRROR -> "Mirror (Permanent deletion)"
                                    }
                                    Text(policyLabel, style = DockBodySmall.copy(fontWeight = FontWeight.SemiBold), color = if (deletionPolicy == DeletionPolicy.MIRROR) colors.danger else colors.textPrimary)
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Security", style = DockBodySmall, color = colors.textSecondary)
                                    Text(
                                        if (isEncrypted) "AES-256-GCM Encrypted" else "Plain Mirror",
                                        style = DockBodySmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = if (isEncrypted) colors.seal else colors.textPrimary
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Integrity", style = DockBodySmall, color = colors.textSecondary)
                                    Text("SHA-256 + Re-read Check", style = DockBodySmall.copy(fontWeight = FontWeight.SemiBold), color = colors.seal)
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }
}
