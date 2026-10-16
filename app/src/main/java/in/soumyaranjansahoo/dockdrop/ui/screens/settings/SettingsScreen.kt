package `in`.soumyaranjansahoo.dockdrop.ui.screens.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.soumyaranjansahoo.dockdrop.core.model.SetDefinition
import `in`.soumyaranjansahoo.dockdrop.core.preferences.AppPreferences
import `in`.soumyaranjansahoo.dockdrop.core.preferences.ThemeMode
import `in`.soumyaranjansahoo.dockdrop.core.repository.SetRepository
import `in`.soumyaranjansahoo.dockdrop.core.support.LogExporter
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockBody
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockBodySmall
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockHeadline
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockMono
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockTheme
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockTitle
import `in`.soumyaranjansahoo.dockdrop.ui.theme.RadiusMd
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    appPreferences: AppPreferences,
    setRepository: SetRepository,
    sets: List<SetDefinition>,
    onBackClick: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToKeepAlive: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = DockTheme.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val currentTheme by appPreferences.themeMode.collectAsState()
    val isReduceMotion by appPreferences.reduceMotion.collectAsState()
    val defaultBatteryPct by appPreferences.defaultMinBatteryPct.collectAsState()

    var showHowToCheckSheet by remember { mutableStateOf(false) }
    var showAboutSheet by remember { mutableStateOf(false) }
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
                    Toast.makeText(context, "Diagnostic log exported successfully", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Failed to export log: ${e.message}", Toast.LENGTH_LONG).show()
                }
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
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onBackClick() }
                        .padding(8.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Settings", style = DockTitle, color = colors.textPrimary)
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // 1. BACKUP SECTION
            SectionHeader(title = "BACKUP")

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(RadiusMd))
                    .border(1.dp, colors.outline, RoundedCornerShape(RadiusMd)),
                colors = CardDefaults.cardColors(containerColor = colors.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Default battery condition",
                        style = DockBody.copy(fontWeight = FontWeight.SemiBold),
                        color = colors.textPrimary
                    )
                    Text(
                        text = "New Sets require battery to be above this level before starting.",
                        style = DockBodySmall,
                        color = colors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(15 to "15%", 20 to "20%", 30 to "30%", 100 to "Charging").forEach { (pct, label) ->
                            val selected = defaultBatteryPct == pct
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (selected) colors.dock else colors.surfaceRaised)
                                    .clickable { appPreferences.setDefaultMinBatteryPct(pct) }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    style = DockBodySmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = if (selected) colors.onDock else colors.textPrimary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = colors.outline)
                    Spacer(modifier = Modifier.height(12.dp))

                    SettingRow(
                        title = "Backup history",
                        subtitle = "View Receipts from all past backups",
                        trailingText = "→",
                        onClick = onNavigateToHistory
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = colors.outline)
                    Spacer(modifier = Modifier.height(12.dp))

                    SettingRow(
                        title = "Notification settings",
                        subtitle = "Configure system alerts for progress and sealed backups",
                        trailingText = "↗",
                        onClick = { openAppNotificationSettings(context) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 2. APPEARANCE SECTION
            SectionHeader(title = "APPEARANCE")

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(RadiusMd))
                    .border(1.dp, colors.outline, RoundedCornerShape(RadiusMd)),
                colors = CardDefaults.cardColors(containerColor = colors.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Theme",
                        style = DockBody.copy(fontWeight = FontWeight.SemiBold),
                        color = colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            ThemeMode.SYSTEM to "System",
                            ThemeMode.DARK to "Dark",
                            ThemeMode.LIGHT to "Light"
                        ).forEach { (mode, label) ->
                            val selected = currentTheme == mode
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (selected) colors.dock else colors.surfaceRaised)
                                    .clickable { appPreferences.setThemeMode(mode) }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    style = DockBodySmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = if (selected) colors.onDock else colors.textPrimary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = colors.outline)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Reduce motion",
                                style = DockBody.copy(fontWeight = FontWeight.SemiBold),
                                color = colors.textPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Replaces particle streams and bounces with calm static indicators.",
                                style = DockBodySmall,
                                color = colors.textSecondary
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Switch(
                            checked = isReduceMotion,
                            onCheckedChange = { appPreferences.setReduceMotion(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = colors.onDock,
                                checkedTrackColor = colors.dock,
                                uncheckedThumbColor = colors.textTertiary,
                                uncheckedTrackColor = colors.surfaceRaised
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 3. PRIVACY SECTION
            SectionHeader(title = "PRIVACY")

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(RadiusMd))
                    .border(1.dp, colors.seal.copy(alpha = 0.5f), RoundedCornerShape(RadiusMd)),
                colors = CardDefaults.cardColors(containerColor = colors.surface)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "🔒", fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Offline by design. Network permission: none",
                            style = DockBody.copy(fontWeight = FontWeight.Bold),
                            color = colors.seal
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "DockDrop does not request internet permission and cannot transmit data off your device. Your files stay strictly between your phone and your SSD.",
                        style = DockBodySmall,
                        color = colors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "How to check in Android Settings →",
                        style = DockBodySmall.copy(color = colors.dock, fontWeight = FontWeight.SemiBold),
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { showHowToCheckSheet = true }
                            .padding(vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 4. KEEP RUNNING SECTION
            SectionHeader(title = "BACKGROUND RELIABILITY")

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(RadiusMd))
                    .border(1.dp, colors.outline, RoundedCornerShape(RadiusMd)),
                colors = CardDefaults.cardColors(containerColor = colors.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SettingRow(
                        title = "Screen-off backup setup",
                        subtitle = "Ensure Android and Samsung One UI don't interrupt screen-locked backups",
                        trailingText = "→",
                        onClick = onNavigateToKeepAlive
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 5. SUPPORT & ABOUT SECTION
            SectionHeader(title = "SUPPORT & ABOUT")

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(RadiusMd))
                    .border(1.dp, colors.outline, RoundedCornerShape(RadiusMd)),
                colors = CardDefaults.cardColors(containerColor = colors.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SettingRow(
                        title = "Export diagnostic log",
                        subtitle = "Saves a plain-text receipt and device summary via file picker",
                        trailingText = "💾",
                        onClick = { exportLogLauncher.launch("dockdrop-diagnostic-log.txt") }
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = colors.outline)
                    Spacer(modifier = Modifier.height(12.dp))

                    SettingRow(
                        title = "About DockDrop",
                        subtitle = "Version 1.0 · Open format specs & licenses",
                        trailingText = "ℹ",
                        onClick = { showAboutSheet = true }
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // 6. DANGER ZONE
            SectionHeader(title = "DANGER ZONE", color = colors.danger)

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(RadiusMd))
                    .border(1.dp, colors.danger.copy(alpha = 0.5f), RoundedCornerShape(RadiusMd)),
                colors = CardDefaults.cardColors(containerColor = colors.surface)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Forget this SSD",
                        style = DockBody.copy(fontWeight = FontWeight.Bold),
                        color = colors.danger
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Clears DockDrop's saved Sets and folder permissions on this phone. Your backup files on the SSD are never touched.",
                        style = DockBodySmall,
                        color = colors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedButton(
                        onClick = { showForgetConfirmDialog = true },
                        border = androidx.compose.foundation.BorderStroke(1.dp, colors.danger),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Forget SSD", color = colors.danger, style = DockBodySmall.copy(fontWeight = FontWeight.SemiBold))
                    }
                }
            }

            Spacer(modifier = Modifier.height(36.dp))
        }
    }

    // Modal: How to check zero network access
    if (showHowToCheckSheet) {
        ModalBottomSheet(
            onDismissRequest = { showHowToCheckSheet = false },
            containerColor = colors.surface,
            shape = RoundedCornerShape(topStart = RadiusMd, topEnd = RadiusMd)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 36.dp, top = 8.dp)
            ) {
                Text(
                    text = "Verify Zero Network Access",
                    style = DockHeadline,
                    color = colors.textPrimary
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Android strictly enforces manifest permissions. DockDrop cannot connect to the internet because it never requested the permission:",
                    style = DockBodySmall,
                    color = colors.textSecondary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("1. Open Android Settings → Apps → DockDrop", style = DockBodySmall, color = colors.textPrimary)
                Spacer(modifier = Modifier.height(6.dp))
                Text("2. Tap Permissions", style = DockBodySmall, color = colors.textPrimary)
                Spacer(modifier = Modifier.height(6.dp))
                Text("3. Observe that 'Network' or 'Internet' is not present.", style = DockBodySmall, color = colors.seal)

                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {
                        openAppSettings(context)
                        showHowToCheckSheet = false
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.dock, contentColor = colors.onDock)
                ) {
                    Text("Open App Info", style = DockBody.copy(fontWeight = FontWeight.SemiBold))
                }
            }
        }
    }

    // Modal: About Sheet
    if (showAboutSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAboutSheet = false },
            containerColor = colors.surface,
            shape = RoundedCornerShape(topStart = RadiusMd, topEnd = RadiusMd)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 36.dp, top = 8.dp)
            ) {
                Text(text = "About DockDrop", style = DockHeadline, color = colors.textPrimary)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Version 1.0 (versionCode 1)", style = DockMono.copy(fontSize = 13.sp), color = colors.dock)
                Spacer(modifier = Modifier.height(16.dp))

                Text(text = "OPEN SPECIFICATIONS", style = DockMono.copy(fontSize = 11.sp), color = colors.textTertiary)
                Spacer(modifier = Modifier.height(6.dp))
                Text("• Plain Mirror v1: standard directories readable on any OS", style = DockBodySmall, color = colors.textSecondary)
                Text("• Encrypted Set v1: AES-256-GCM chunked stream (docs/format-encrypted-v1.md)", style = DockBodySmall, color = colors.textSecondary)

                Spacer(modifier = Modifier.height(16.dp))
                Text(text = "LICENSES", style = DockMono.copy(fontSize = 11.sp), color = colors.textTertiary)
                Spacer(modifier = Modifier.height(6.dp))
                Text("DockDrop and its open components are licensed under Apache 2.0. Uses Jetpack Compose, AndroidX, and Kotlinx Serialization.", style = DockBodySmall, color = colors.textSecondary)

                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { showAboutSheet = false },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.surfaceRaised, contentColor = colors.textPrimary)
                ) {
                    Text("Close", style = DockBody.copy(fontWeight = FontWeight.SemiBold))
                }
            }
        }
    }

    // Modal: Forget SSD Confirmation Dialog
    if (showForgetConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showForgetConfirmDialog = false },
            title = { Text("Forget this SSD?", color = colors.textPrimary) },
            text = {
                Text(
                    text = "This will delete all saved Backup Sets, folder configurations, and persisted permissions from this phone. Your backup files on the external drive are never touched.",
                    color = colors.textSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showForgetConfirmDialog = false
                        scope.launch {
                            // Clear persisted URI permissions
                            try {
                                context.contentResolver.persistedUriPermissions.forEach { perm ->
                                    context.contentResolver.releasePersistableUriPermission(
                                        perm.uri,
                                        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                                    )
                                }
                            } catch (_: Exception) {}

                            // Clear DB sets
                            setRepository.deleteAllSets()
                            Toast.makeText(context, "SSD and sets forgotten", Toast.LENGTH_SHORT).show()
                            onBackClick()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.danger)
                ) {
                    Text("Forget", color = colors.textPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showForgetConfirmDialog = false }) {
                    Text("Cancel", color = colors.textTertiary)
                }
            },
            containerColor = colors.surface,
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
private fun SectionHeader(title: String, color: androidx.compose.ui.graphics.Color = DockTheme.colors.textTertiary) {
    Text(
        text = title,
        style = DockMono.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
        color = color,
        modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
    )
}

@Composable
private fun SettingRow(
    title: String,
    subtitle: String,
    trailingText: String,
    onClick: () -> Unit
) {
    val colors = DockTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = DockBody.copy(fontWeight = FontWeight.SemiBold), color = colors.textPrimary)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = subtitle, style = DockBodySmall, color = colors.textSecondary)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = trailingText, style = DockTitle.copy(fontSize = 18.sp), color = colors.dock)
    }
}

private fun openAppSettings(context: Context) {
    try {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
        }
        context.startActivity(intent)
    } catch (_: Exception) {}
}

private fun openAppNotificationSettings(context: Context) {
    try {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        openAppSettings(context)
    }
}
