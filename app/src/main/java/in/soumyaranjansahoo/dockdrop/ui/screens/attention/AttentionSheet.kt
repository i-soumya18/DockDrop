package `in`.soumyaranjansahoo.dockdrop.ui.screens.attention

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.soumyaranjansahoo.dockdrop.core.model.AttentionReason
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockBody
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockBodySmall
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockHeadline
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockMono
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockTheme
import `in`.soumyaranjansahoo.dockdrop.ui.theme.RadiusMd
import `in`.soumyaranjansahoo.dockdrop.ui.theme.RadiusSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttentionSheet(
    reason: AttentionReason,
    onPrimaryAction: () -> Unit,
    onSecondaryAction: (() -> Unit)? = null,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = DockTheme.colors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var isReviewingFiles by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = RadiusSheet, topEnd = RadiusSheet),
        containerColor = colors.surface,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = reason.title,
                style = DockHeadline,
                color = if (reason is AttentionReason.MassDelete) colors.danger else colors.attention
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = reason.message,
                style = DockBody,
                color = colors.textPrimary
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = if (reason is AttentionReason.MassDelete) {
                    "Nothing has been removed from the SSD yet."
                } else {
                    "Nothing was lost. Files copied so far are safe on your SSD."
                },
                style = DockBodySmall,
                color = colors.textSecondary
            )

            // If MassDelete and reviewing files, show candidate list
            if (reason is AttentionReason.MassDelete && reason.paths.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))

                AnimatedVisibility(visible = isReviewingFiles) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(RadiusMd))
                            .border(1.dp, colors.outline, RoundedCornerShape(RadiusMd)),
                        colors = CardDefaults.cardColors(containerColor = colors.bg)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                "FILES MARKED FOR REMOVAL (${reason.paths.size})",
                                style = DockMono.copy(fontSize = 11.sp),
                                color = colors.textTertiary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            LazyColumn(modifier = Modifier.heightIn(max = 180.dp)) {
                                items(reason.paths) { path ->
                                    Text(
                                        text = path,
                                        style = DockMono.copy(fontSize = 12.sp),
                                        color = colors.textPrimary,
                                        modifier = Modifier.padding(vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (reason is AttentionReason.MassDelete) {
                if (!isReviewingFiles) {
                    Button(
                        onClick = { isReviewingFiles = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.dock,
                            contentColor = colors.onDock
                        )
                    ) {
                        Text(
                            "Review ${reason.count} files",
                            style = DockBody.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }
                } else {
                    Button(
                        onClick = onPrimaryAction,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.danger,
                            contentColor = colors.textPrimary
                        )
                    ) {
                        Text(
                            "Remove ${reason.count} files and continue",
                            style = DockBody.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }
                }

                if (onSecondaryAction != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = onSecondaryAction,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Keep all files (switch to Keep mode)", style = DockBody)
                    }
                }
            } else {
                Button(
                    onClick = onPrimaryAction,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.dock,
                        contentColor = colors.onDock
                    )
                ) {
                    Text(
                        text = when (reason) {
                            is AttentionReason.NoSpace -> "Choose what to back up"
                            is AttentionReason.PermissionLost -> "Grant access again"
                            is AttentionReason.ReadErrors -> "Retry"
                            else -> "Continue"
                        },
                        style = DockBody.copy(fontWeight = FontWeight.SemiBold)
                    )
                }

                if (onSecondaryAction != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    TextButton(onClick = onSecondaryAction) {
                        Text("Secondary action", style = DockBodySmall, color = colors.dock)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(onClick = onDismiss) {
                Text("Dismiss", style = DockBodySmall, color = colors.textTertiary)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
