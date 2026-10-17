package `in`.soumyaranjansahoo.dockdrop.ui.screens.plugin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Usb
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
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
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp, top = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Tactile Hero Dock with latching cable
            DockHero(
                state = when {
                    isUsbConnecting && dockInfo == null -> DropState.NoDock
                    dockInfo != null -> DropState.DockDetected(dockInfo)
                    else -> dropState
                },
                modifier = Modifier.size(150.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            when {
                // Stage A: Connecting to USB
                isUsbConnecting && dockInfo == null -> {
                    Text(
                        text = "Connecting to storage…",
                        style = DockHeadline,
                        color = colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Detecting filesystem and mounting drive safely",
                        style = DockBodySmall,
                        color = colors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(28.dp))
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.defaultMinSize(minHeight = 48.dp, minWidth = 48.dp)
                    ) {
                        Text("Not now", style = DockBody.copy(color = colors.textSecondary))
                    }
                }

                // Stage B: Unknown Dock
                dockInfo != null && matchingSet == null -> {
                    Text(
                        text = "External drive connected",
                        style = DockHeadline,
                        color = colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .border(1.dp, colors.outline.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                        colors = CardDefaults.cardColors(containerColor = colors.surfaceRaised)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(colors.dock.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Storage,
                                    contentDescription = null,
                                    tint = colors.dock,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = dockInfo.userLabel,
                                    style = DockBody.copy(fontWeight = FontWeight.Bold),
                                    color = colors.textPrimary
                                )
                                Text(
                                    text = "${formatBytes(dockInfo.freeBytes)} free · ${dockInfo.filesystem}",
                                    style = DockMono.copy(fontSize = 12.sp),
                                    color = colors.textSecondary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = onSetupNewDock,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .semantics {
                                role = Role.Button
                                contentDescription = "Set up this SSD as a new backup destination"
                            },
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.dock,
                            contentColor = colors.onDock
                        )
                    ) {
                        Text("Set up this SSD", style = DockBody.copy(fontWeight = FontWeight.Bold, fontSize = 16.sp))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.defaultMinSize(minHeight = 48.dp, minWidth = 48.dp)
                    ) {
                        Text("Not now", style = DockBody.copy(color = colors.textSecondary))
                    }
                }

                // Stage C: Registered Dock & Resumable
                dockInfo != null && matchingSet != null && dropState is DropState.Resumable -> {
                    Text(
                        text = "Resume Backup?",
                        style = DockHeadline,
                        color = colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${matchingSet.name} · ${dropState.progress.percentage}% completed",
                        style = DockBodySmall.copy(fontFeatureSettings = "tnum"),
                        color = colors.dock
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = { onResumeBackup(matchingSet) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .semantics {
                                role = Role.Button
                                contentDescription = "Resume backup of ${matchingSet.name}"
                            },
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.dock,
                            contentColor = colors.onDock
                        )
                    ) {
                        Icon(imageVector = Icons.Outlined.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Resume Backup", style = DockBody.copy(fontWeight = FontWeight.Bold, fontSize = 16.sp))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.defaultMinSize(minHeight = 48.dp, minWidth = 48.dp)
                    ) {
                        Text("Not now", style = DockBody.copy(color = colors.textSecondary))
                    }
                }

                // Stage D: Registered Dock & Ready to backup (Premature Seal Green Fixed on lines 230 & 236)
                dockInfo != null && matchingSet != null -> {
                    Text(
                        text = matchingSet.name,
                        style = DockHeadline,
                        color = colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Hardware connection dot uses colors.dock (neutral active connection, never premature seal green)
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(colors.dock)
                        )
                        // Drive free space uses neutral textSecondary with tabular figures
                        Text(
                            text = "${dockInfo.userLabel} · ${formatBytes(dockInfo.freeBytes)} free",
                            style = DockBodySmall.copy(fontFeatureSettings = "tnum"),
                            color = colors.textSecondary
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = { onStartBackup(matchingSet) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .semantics {
                                role = Role.Button
                                contentDescription = "Start backup of ${matchingSet.name} to ${dockInfo.userLabel}"
                            },
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.dock,
                            contentColor = colors.onDock
                        )
                    ) {
                        Icon(imageVector = Icons.Outlined.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Start Backup Now", style = DockBody.copy(fontWeight = FontWeight.Bold, fontSize = 16.sp))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.defaultMinSize(minHeight = 48.dp, minWidth = 48.dp)
                    ) {
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
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.defaultMinSize(minHeight = 48.dp, minWidth = 48.dp)
                    ) {
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
