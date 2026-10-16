package `in`.soumyaranjansahoo.dockdrop.ui.screens.onboarding

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockBody
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockBodySmall
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockHeadline
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockMono
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockTheme
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockTitle
import `in`.soumyaranjansahoo.dockdrop.ui.theme.RadiusMd

@Composable
fun KeepAliveScreen(
    onDoneClick: () -> Unit,
    onRemindMeLaterClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = DockTheme.colors
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasNotificationPerm by remember {
        mutableStateOf(checkNotificationPermission(context))
    }
    var isBatteryExempt by remember {
        mutableStateOf(checkBatteryOptimization(context))
    }
    var isSleepingAppsConfirmed by remember { mutableStateOf(false) }

    // Re-check permissions when returning from system settings
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasNotificationPerm = checkNotificationPermission(context)
                isBatteryExempt = checkBatteryOptimization(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val notifPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotificationPerm = granted || checkNotificationPermission(context)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = colors.bg,
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val canFinish = hasNotificationPerm && isBatteryExempt
                Button(
                    onClick = onDoneClick,
                    enabled = canFinish,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.dock,
                        contentColor = colors.onDock,
                        disabledContainerColor = colors.surfaceRaised,
                        disabledContentColor = colors.textTertiary
                    )
                ) {
                    Text("Done", style = DockBody.copy(fontWeight = FontWeight.SemiBold))
                }

                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = onRemindMeLaterClick) {
                    Text("Remind me later", style = DockBody.copy(color = colors.textSecondary))
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Let backups finish with the screen off",
                style = DockHeadline,
                color = colors.textPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Android and Samsung One UI stop background apps to save power. Complete these steps so your backups don't get interrupted.",
                style = DockBodySmall,
                color = colors.textSecondary
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Step 1: Notifications
            StepCard(
                stepNumber = "1",
                title = "Allow notifications",
                description = "Required on Android 13+ to show progress and keep the backup alive.",
                isDone = hasNotificationPerm,
                actionLabel = if (hasNotificationPerm) "Allowed ✓" else "Allow",
                onActionClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        hasNotificationPerm = true
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Step 2: Battery optimization exemption
            StepCard(
                stepNumber = "2",
                title = "Don't restrict battery",
                description = "Allows DockDrop to copy and verify files when your screen is locked.",
                isDone = isBatteryExempt,
                actionLabel = if (isBatteryExempt) "Unrestricted ✓" else "Set unrestricted",
                onActionClick = {
                    if (!isBatteryExempt) {
                        requestBatteryOptimizationExemption(context)
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Step 3: Samsung Sleeping Apps
            SamsungStepCard(
                stepNumber = "3",
                title = "Remove from Sleeping apps (Samsung)",
                description = "Check that Samsung Device Care doesn't put DockDrop into 'Sleeping apps' or 'Deep sleeping apps'.",
                isConfirmed = isSleepingAppsConfirmed,
                onCheckedChange = { isSleepingAppsConfirmed = it },
                onOpenSettingsClick = { openAppSettings(context) }
            )
        }
    }
}

@Composable
private fun StepCard(
    stepNumber: String,
    title: String,
    description: String,
    isDone: Boolean,
    actionLabel: String,
    onActionClick: () -> Unit
) {
    val colors = DockTheme.colors
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(RadiusMd))
            .background(colors.surface)
            .border(
                1.dp,
                if (isDone) colors.seal.copy(alpha = 0.5f) else colors.outline,
                RoundedCornerShape(RadiusMd)
            )
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (isDone) colors.seal.copy(alpha = 0.2f) else colors.surfaceRaised),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isDone) "✓" else stepNumber,
                    style = DockTitle.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold),
                    color = if (isDone) colors.seal else colors.textPrimary
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = DockTitle.copy(fontSize = 16.sp), color = colors.textPrimary)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = description, style = DockBodySmall, color = colors.textSecondary)
            }

            Spacer(modifier = Modifier.width(12.dp))

            if (!isDone) {
                Button(
                    onClick = onActionClick,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.dock,
                        contentColor = colors.onDock
                    )
                ) {
                    Text(actionLabel, style = DockBodySmall.copy(fontWeight = FontWeight.SemiBold))
                }
            } else {
                Text(
                    text = "Done ✓",
                    style = DockMono.copy(color = colors.seal, fontSize = 12.sp)
                )
            }
        }
    }
}

@Composable
private fun SamsungStepCard(
    stepNumber: String,
    title: String,
    description: String,
    isConfirmed: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onOpenSettingsClick: () -> Unit
) {
    val colors = DockTheme.colors
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(RadiusMd))
            .background(colors.surface)
            .border(
                1.dp,
                if (isConfirmed) colors.seal.copy(alpha = 0.5f) else colors.outline,
                RoundedCornerShape(RadiusMd)
            )
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (isConfirmed) colors.seal.copy(alpha = 0.2f) else colors.surfaceRaised),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isConfirmed) "✓" else stepNumber,
                        style = DockTitle.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold),
                        color = if (isConfirmed) colors.seal else colors.textPrimary
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(text = title, style = DockTitle.copy(fontSize = 16.sp), color = colors.textPrimary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = description, style = DockBodySmall, color = colors.textSecondary)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onOpenSettingsClick) {
                    Text("Open App Settings", style = DockBodySmall.copy(color = colors.dock))
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onCheckedChange(!isConfirmed) }
                ) {
                    Checkbox(
                        checked = isConfirmed,
                        onCheckedChange = onCheckedChange,
                        colors = CheckboxDefaults.colors(
                            checkedColor = colors.seal,
                            uncheckedColor = colors.outline
                        )
                    )
                    Text("Confirmed", style = DockBodySmall, color = colors.textSecondary)
                }
            }
        }
    }
}

private fun checkNotificationPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        true
    }
}

private fun checkBatteryOptimization(context: Context): Boolean {
    val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    return pm.isIgnoringBatteryOptimizations(context.packageName)
}

private fun requestBatteryOptimizationExemption(context: Context) {
    try {
        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:${context.packageName}")
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        openAppSettings(context)
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
