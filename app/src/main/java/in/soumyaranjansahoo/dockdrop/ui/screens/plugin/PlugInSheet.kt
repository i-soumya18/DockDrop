package `in`.soumyaranjansahoo.dockdrop.ui.screens.plugin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.soumyaranjansahoo.dockdrop.core.model.DockInfo
import `in`.soumyaranjansahoo.dockdrop.core.model.DropState
import `in`.soumyaranjansahoo.dockdrop.core.model.SetDefinition
import `in`.soumyaranjansahoo.dockdrop.ui.components.DockHero
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockBody
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockBodySmall
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockHeadline
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockMono
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockTheme
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockTitle
import `in`.soumyaranjansahoo.dockdrop.ui.theme.RadiusSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlugInSheet(
    dockInfo: DockInfo?,
    isUsbConnecting: Boolean,
    matchingSet: SetDefinition?,
    dropState: DropState,
    onStartBackup: (SetDefinition) -> Unit,
    onResumeBackup: (SetDefinition) -> Unit,
    onSetupNewDock: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = DockTheme.colors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.surface,
        shape = RoundedCornerShape(topStart = RadiusSheet, topEnd = RadiusSheet),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp, top = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Hero Dock with latching cable
            DockHero(
                state = when {
                    isUsbConnecting && dockInfo == null -> DropState.NoDock
                    dockInfo != null -> DropState.DockDetected(dockInfo)
                    else -> dropState
                },
                modifier = Modifier.size(160.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            when {
                // Stage A: Connecting to USB
                isUsbConnecting && dockInfo == null -> {
                    Text(
                        text = "Connecting to your SSD…",
                        style = DockHeadline,
                        color = colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Checking filesystem and mounting volume",
                        style = DockBodySmall,
                        color = colors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    TextButton(onClick = onDismiss) {
                        Text("Not now", style = DockBody.copy(color = colors.textSecondary))
                    }
                }

                // Stage B: Unknown Dock
                dockInfo != null && matchingSet == null -> {
                    Text(
                        text = "New drive detected",
                        style = DockHeadline,
                        color = colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "${dockInfo.userLabel} · ${formatBytes(dockInfo.freeBytes)} free · ${dockInfo.filesystem}",
                        style = DockBodySmall,
                        color = colors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = onSetupNewDock,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.dock,
                            contentColor = colors.onDock
                        )
                    ) {
                        Text("Set up this SSD", style = DockBody.copy(fontWeight = FontWeight.SemiBold))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = onDismiss) {
                        Text("Not now", style = DockBody.copy(color = colors.textSecondary))
                    }
                }

                // Stage B: Registered Dock & Resumable
                dockInfo != null && matchingSet != null && dropState is DropState.Resumable -> {
                    Text(
                        text = "Resume backup",
                        style = DockHeadline,
                        color = colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "${matchingSet.name} · ${dropState.progress.percentage}% done · ${dropState.progress.totalFiles - dropState.progress.copiedFiles} files left",
                        style = DockBodySmall,
                        color = colors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { onResumeBackup(matchingSet) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.dock,
                            contentColor = colors.onDock
                        )
                    ) {
                        Text("Resume backup", style = DockBody.copy(fontWeight = FontWeight.SemiBold))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = onDismiss) {
                        Text("Not now", style = DockBody.copy(color = colors.textSecondary))
                    }
                }

                // Stage B: Registered Dock & Ready to backup
                dockInfo != null && matchingSet != null -> {
                    Text(
                        text = "${dockInfo.userLabel} · ${formatBytes(dockInfo.freeBytes)} free · ${dockInfo.filesystem}",
                        style = DockTitle.copy(fontWeight = FontWeight.Bold),
                        color = colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    val statusText = if (matchingSet.lastSealedMs != null) {
                        "${matchingSet.name} · tap to check and back up"
                    } else {
                        "${matchingSet.name} · first backup ready"
                    }
                    Text(
                        text = statusText,
                        style = DockBodySmall,
                        color = colors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { onStartBackup(matchingSet) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.dock,
                            contentColor = colors.onDock
                        )
                    ) {
                        Text("Start backup", style = DockBody.copy(fontWeight = FontWeight.SemiBold))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = onDismiss) {
                        Text("Not now", style = DockBody.copy(color = colors.textSecondary))
                    }
                }

                else -> {
                    Text(
                        text = "Plug in your SSD",
                        style = DockHeadline,
                        color = colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    TextButton(onClick = onDismiss) {
                        Text("Close", style = DockBody.copy(color = colors.textSecondary))
                    }
                }
            }
        }
    }
}

private fun formatBytes(bytes: Long): String {
    val gb = bytes.toDouble() / (1024 * 1024 * 1024)
    return if (gb >= 1.0) String.format("%.1f GB", gb) else "${bytes / (1024 * 1024)} MB"
}
