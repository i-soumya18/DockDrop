package `in`.soumyaranjansahoo.dockdrop.ui.screens.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.soumyaranjansahoo.dockdrop.core.model.SetDefinition
import `in`.soumyaranjansahoo.dockdrop.core.preferences.AppPreferences
import `in`.soumyaranjansahoo.dockdrop.core.preferences.ThemeMode
import `in`.soumyaranjansahoo.dockdrop.core.repository.SetRepository
import `in`.soumyaranjansahoo.dockdrop.core.support.LogExporter
import `in`.soumyaranjansahoo.dockdrop.ui.components.DockBottomBar
import `in`.soumyaranjansahoo.dockdrop.ui.navigation.Routes
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockBody
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockBodySmall
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockHeadline
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockLabel
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockMono
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockTheme
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockTitle
import `in`.soumyaranjansahoo.dockdrop.ui.theme.RadiusMd
import `in`.soumyaranjansahoo.dockdrop.ui.theme.RadiusSheet
import `in`.soumyaranjansahoo.dockdrop.ui.theme.RadiusSm
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    appPreferences: AppPreferences,
    setRepository: SetRepository,
    sets: List<SetDefinition>,
    onBackClick: () -> Unit = {},
    onNavigateToHome: () -> Unit = {},
    onNavigateToRestore: () -> Unit = {},
    onNavigateToHistory: () -> Unit = {},
    onNavigateToKeepAlive: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val colors = DockTheme.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val currentTheme by appPreferences.themeMode.collectAsState()
    val isReduceMotion by appPreferences.reduceMotion.collectAsState()
    val defaultBatteryPct by appPreferences.defaultMinBatteryPct.collectAsState()

    val appVersion = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0.1"
        } catch (_: Exception) {
            "1.0.1"
        }
    }

    var showHowToCheckSheet by remember { mutableStateOf(false) }
    var showAboutSheet by remember { mutableStateOf(false) }
    var showManageDocksSheet by remember { mutableStateOf(false) }
    var showForgetConfirmDialog by remember { mutableStateOf(false) }

    val exportLogLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                try {
                    val recentReceipts = withContext(Dispatchers.IO) {
                        setRepository.getRecentReceiptsSync(20)
                    }
                    val logContent = LogExporter.generateDiagnosticLog(
                        context = context,
                        sets = sets,
                        recentReceipts = recentReceipts
                    )
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri)?.use { os ->
                            os.write(logContent.toByteArray())
                        }
                    }
                    Toast.makeText(context, "Diagnostic log exported", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = colors.bg,
        bottomBar = {
            DockBottomBar(
                currentRoute = Routes.SETTINGS,
                onNavigateToHome = onNavigateToHome,
                onNavigateToRestore = onNavigateToRestore,
                onNavigateToSettings = { /* Already on settings */ }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Settings",
                style = DockHeadline,
                color = colors.textPrimary
            )
            Spacer(modifier = Modifier.height(20.dp))

            // 1. BACKUP SECTION
            SectionCaption(title = "BACKUP")

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(RadiusMd))
                    .border(1.dp, colors.outline, RoundedCornerShape(RadiusMd)),
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Default battery condition",
                        style = DockBody.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
                        color = colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "New Sets require battery to be above this level before starting.",
                        style = DockBodySmall,
                        color = colors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    // Battery selector pills (>=48dp touch targets + radio semantics)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(15 to "15%", 20 to "20%", 30 to "30%", 100 to "Charging").forEach { (pct, label) ->
                            val isSelected = defaultBatteryPct == pct
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .clip(RoundedCornerShape(RadiusSm))
                                    .background(if (isSelected) colors.dock else colors.surfaceRaised)
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) colors.dock else colors.outline.copy(alpha = 0.6f),
                                        shape = RoundedCornerShape(RadiusSm)
                                    )
                                    .semantics {
                                        role = Role.RadioButton
                                        selected = isSelected
                                    }
                                    .clickable { appPreferences.setDefaultMinBatteryPct(pct) }
                                    .padding(horizontal = 6.dp, vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    style = DockMono.copy(
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = if (isSelected) colors.onDock else colors.textPrimary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = colors.outline.copy(alpha = 0.6f), thickness = 1.dp)
                    Spacer(modifier = Modifier.height(8.dp))

                    SettingsItemRow(
                        icon = Icons.Outlined.Description,
                        title = "Backup history",
                        subtitle = "View cryptographic receipts from past Drops",
                        onClick = onNavigateToHistory
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = colors.outline.copy(alpha = 0.6f), thickness = 1.dp)
                    Spacer(modifier = Modifier.height(8.dp))

                    SettingsItemRow(
                        icon = Icons.Outlined.Notifications,
                        title = "Notification settings",
                        subtitle = "System alerts for progress and sealed backups",
                        onClick = { openAppNotificationSettings(context) }
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = colors.outline.copy(alpha = 0.6f), thickness = 1.dp)
                    Spacer(modifier = Modifier.height(8.dp))

                    SettingsItemRow(
                        icon = Icons.Outlined.BatteryChargingFull,
                        title = "Background setup",
                        subtitle = "Keep Drops alive when screen locks",
                        onClick = onNavigateToKeepAlive
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 2. APPEARANCE SECTION
            SectionCaption(title = "APPEARANCE")

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(RadiusMd))
                    .border(1.dp, colors.outline, RoundedCornerShape(RadiusMd)),
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Theme",
                        style = DockBody.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
                        color = colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Theme selector pills (>=48dp touch targets + radio semantics)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            ThemeMode.SYSTEM to "System",
                            ThemeMode.DARK to "Dark",
                            ThemeMode.LIGHT to "Light"
                        ).forEach { (mode, label) ->
                            val isSelected = currentTheme == mode
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .clip(RoundedCornerShape(RadiusSm))
                                    .background(if (isSelected) colors.dock else colors.surfaceRaised)
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) colors.dock else colors.outline.copy(alpha = 0.6f),
                                        shape = RoundedCornerShape(RadiusSm)
                                    )
                                    .semantics {
                                        role = Role.RadioButton
                                        selected = isSelected
                                    }
                                    .clickable { appPreferences.setThemeMode(mode) }
                                    .padding(horizontal = 8.dp, vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    style = DockBodySmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = if (isSelected) colors.onDock else colors.textPrimary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = colors.outline.copy(alpha = 0.6f), thickness = 1.dp)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .clip(RoundedCornerShape(RadiusSm))
                            .clickable { appPreferences.setReduceMotion(!isReduceMotion) }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Reduce motion",
                                style = DockBody.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
                                color = colors.textPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Replaces particle streams and springs with calm static indicators.",
                                style = DockBodySmall,
                                color = colors.textSecondary
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Switch(
                            checked = isReduceMotion,
                            onCheckedChange = { appPreferences.setReduceMotion(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = colors.surface,
                                checkedTrackColor = colors.dock,
                                uncheckedThumbColor = colors.textSecondary,
                                uncheckedTrackColor = colors.surfaceRaised,
                                uncheckedBorderColor = colors.outline
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 3. STORAGE SECTION
            SectionCaption(title = "STORAGE")

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(RadiusMd))
                    .border(1.dp, colors.outline, RoundedCornerShape(RadiusMd)),
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SettingsItemRow(
                        icon = Icons.Outlined.Storage,
                        title = "Manage Docks",
                        subtitle = "View configured drives and release permissions",
                        onClick = { showManageDocksSheet = true }
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = colors.outline.copy(alpha = 0.6f), thickness = 1.dp)
                    Spacer(modifier = Modifier.height(8.dp))

                    SettingsItemRow(
                        icon = Icons.Outlined.Folder,
                        title = "Default vault location",
                        subtitle = "DockDrop/ on your USB-C SSD",
                        onClick = { showManageDocksSheet = true }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 4. PRIVACY SECTION (Uses semantic badgeBg, badgeBorder, badgeContent tokens)
            SectionCaption(title = "PRIVACY")

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(RadiusMd))
                    .border(1.dp, colors.badgeBorder, RoundedCornerShape(RadiusMd))
                    .clickable(role = Role.Button) { showHowToCheckSheet = true },
                colors = CardDefaults.cardColors(containerColor = colors.badgeBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
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
                            .clip(CircleShape)
                            .background(colors.badgeBorder.copy(alpha = 0.45f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Lock,
                            contentDescription = null,
                            tint = colors.badgeContent,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Offline by design · Network permission: none",
                            style = DockBodySmall.copy(fontWeight = FontWeight.Bold),
                            color = colors.badgeContent
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "DockDrop does not request internet access. Tap to verify in Android Settings.",
                            style = DockBodySmall.copy(fontSize = 12.sp),
                            color = colors.badgeContent.copy(alpha = 0.9f)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = colors.badgeContent,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 5. ABOUT SECTION
            SectionCaption(title = "ABOUT")

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(RadiusMd))
                    .border(1.dp, colors.outline, RoundedCornerShape(RadiusMd)),
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SettingsItemRow(
                        icon = Icons.Outlined.Info,
                        title = "App version & diagnostics",
                        subtitle = "v$appVersion · Export support log",
                        onClick = { showAboutSheet = true }
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Modal Sheet: How to verify zero network access
    if (showHowToCheckSheet) {
        ModalBottomSheet(
            onDismissRequest = { showHowToCheckSheet = false },
            containerColor = colors.surface,
            shape = RoundedCornerShape(topStart = RadiusSheet, topEnd = RadiusSheet)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(colors.badgeBg)
                            .border(1.dp, colors.badgeBorder, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Lock,
                            contentDescription = null,
                            tint = colors.badgeContent,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "Verify zero network access",
                            style = DockTitle,
                            color = colors.textPrimary
                        )
                        Text(
                            text = "In Android system settings",
                            style = DockBodySmall,
                            color = colors.textSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                val steps = listOf(
                    "Open Android Settings → Apps → DockDrop.",
                    "Tap \"Permissions\". Notice \"Network\" or \"Internet\" does not appear.",
                    "Tap \"Mobile data & Wi-Fi\". Data usage shows 0 B used.",
                    "DockDrop's manifest is verified at build time to contain zero network permissions."
                )

                steps.forEachIndexed { i, step ->
                    Row(modifier = Modifier.padding(vertical = 4.dp)) {
                        Text(
                            text = "${i + 1}.",
                            style = DockMono.copy(fontWeight = FontWeight.Bold),
                            color = colors.dock
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = step,
                            style = DockBodySmall,
                            color = colors.textSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                        context.startActivity(intent)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp),
                    shape = RoundedCornerShape(RadiusSm),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.dock,
                        contentColor = colors.onDock
                    )
                ) {
                    Text(
                        text = "Open Android App Info",
                        style = DockBody.copy(fontWeight = FontWeight.SemiBold)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Modal Sheet: Manage Docks & Forget SSD
    if (showManageDocksSheet) {
        ModalBottomSheet(
            onDismissRequest = { showManageDocksSheet = false },
            containerColor = colors.surface,
            shape = RoundedCornerShape(topStart = RadiusSheet, topEnd = RadiusSheet)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 20.dp)
            ) {
                Text(
                    text = "Storage & Connected Docks",
                    style = DockTitle,
                    color = colors.textPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Default vault path: DockDrop/ on your USB-C SSD",
                    style = DockMono.copy(color = colors.textSecondary)
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (sets.isNotEmpty()) {
                    Text(
                        text = "Configured Sets (${sets.size})",
                        style = DockBodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    sets.forEach { s ->
                        Text(
                            text = "• ${s.name} (${s.dockUserLabel})",
                            style = DockBodySmall,
                            color = colors.textSecondary,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                } else {
                    Text(
                        text = "No Sets configured yet.",
                        style = DockBodySmall,
                        color = colors.textSecondary
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                OutlinedButton(
                    onClick = {
                        showManageDocksSheet = false
                        showForgetConfirmDialog = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp),
                    shape = RoundedCornerShape(RadiusSm),
                    border = BorderStroke(1.dp, colors.danger)
                ) {
                    Text(
                        text = "Forget this SSD",
                        color = colors.danger,
                        style = DockBody.copy(fontWeight = FontWeight.SemiBold)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Modal Sheet: About DockDrop & Diagnostics
    if (showAboutSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAboutSheet = false },
            containerColor = colors.surface,
            shape = RoundedCornerShape(topStart = RadiusSheet, topEnd = RadiusSheet)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 20.dp)
            ) {
                Text(
                    text = "About DockDrop",
                    style = DockTitle,
                    color = colors.textPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Version $appVersion · Offline by design",
                    style = DockMono.copy(color = colors.textSecondary)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "DockDrop backs up your phone directly to an external USB-C drive you own with zero network permissions, SHA-256 verification, and open formats.",
                    style = DockBodySmall,
                    color = colors.textSecondary
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        showAboutSheet = false
                        exportLogLauncher.launch("dockdrop-diagnostic-log.txt")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp),
                    shape = RoundedCornerShape(RadiusSm),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.dock,
                        contentColor = colors.onDock
                    )
                ) {
                    Text(
                        text = "Export diagnostic log",
                        style = DockBody.copy(fontWeight = FontWeight.SemiBold)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = {
                        showAboutSheet = false
                        onNavigateToKeepAlive()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp),
                    shape = RoundedCornerShape(RadiusSm),
                    border = BorderStroke(1.dp, colors.outline)
                ) {
                    Text(
                        text = "Screen-off setup guide",
                        style = DockBody.copy(fontWeight = FontWeight.SemiBold),
                        color = colors.textPrimary
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Confirmation dialog: Forget SSD
    if (showForgetConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showForgetConfirmDialog = false },
            containerColor = colors.surface,
            title = {
                Text(
                    text = "Forget this SSD?",
                    style = DockTitle,
                    color = colors.textPrimary
                )
            },
            text = {
                Text(
                    text = "This removes all saved Sets and folder permissions on this phone. Files on your SSD are never deleted.",
                    style = DockBodySmall,
                    color = colors.textSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showForgetConfirmDialog = false
                        scope.launch {
                            try {
                                sets.forEach { s ->
                                    setRepository.deleteSet(s.id)
                                }
                                for (perm in context.contentResolver.persistedUriPermissions) {
                                    try {
                                        context.contentResolver.releasePersistableUriPermission(
                                            perm.uri,
                                            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                                        )
                                    } catch (_: Exception) {}
                                }
                                Toast.makeText(context, "SSD forgotten", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    modifier = Modifier.heightIn(min = 48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.danger,
                        contentColor = colors.surface
                    )
                ) {
                    Text(
                        text = "Forget",
                        style = DockBodySmall.copy(fontWeight = FontWeight.Bold)
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showForgetConfirmDialog = false },
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text(
                        text = "Cancel",
                        style = DockBodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = colors.textSecondary
                    )
                }
            }
        )
    }
}

@Composable
private fun SectionCaption(title: String) {
    val colors = DockTheme.colors
    Text(
        text = title,
        style = DockLabel,
        color = colors.textSecondary,
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
    )
}

@Composable
private fun SettingsItemRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    val colors = DockTheme.colors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clip(RoundedCornerShape(RadiusSm))
            .clickable(role = Role.Button) { onClick() }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = colors.dock,
            modifier = Modifier.size(22.dp)
        )

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = DockBody.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
                color = colors.textPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = DockBodySmall,
                color = colors.textSecondary
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = colors.textTertiary,
            modifier = Modifier.size(20.dp)
        )
    }
}

private fun openAppNotificationSettings(context: Context) {
    try {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
        }
        context.startActivity(intent)
    }
}
