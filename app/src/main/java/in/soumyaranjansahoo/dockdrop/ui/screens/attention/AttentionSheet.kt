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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
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

    val isDanger = reason is AttentionReason.MassDelete
    val accentColor = if (isDanger) colors.danger else colors.attention

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = colors.surface,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Warning,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = reason.title,
                style = DockHeadline,
                color = colors.textPrimary
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = reason.message,
                style = DockBody,
                color = colors.textSecondary
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Reassurance Pledge Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.dp, colors.seal.copy(alpha = 0.35f), RoundedCornerShape(14.dp)),
                colors = CardDefaults.cardColors(containerColor = colors.seal.copy(alpha = 0.06f))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Shield,
                        contentDescription = null,
                        tint = colors.seal,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (reason is AttentionReason.MassDelete) {
                            "Safe guard active. Nothing has been deleted from your SSD yet."
                        } else {
                            "Nothing was lost. Files copied so far are safely sealed on your SSD."
                        },
                        style = DockBodySmall.copy(fontWeight = FontWeight.Medium),
                        color = colors.seal
                    )
                }
            }

            // If MassDelete and reviewing files, show candidate list
            if (reason is AttentionReason.MassDelete && reason.paths.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))

                AnimatedVisibility(visible = isReviewingFiles) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .border(1.dp, colors.outline.copy(alpha = 0.5f), RoundedCornerShape(14.dp)),
                        colors = CardDefaults.cardColors(containerColor = colors.surfaceRaised)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                "FILES MARKED FOR REMOVAL (${reason.paths.size})",
                                style = DockMono.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                                color = colors.textTertiary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            LazyColumn(modifier = Modifier.heightIn(max = 160.dp)) {
                                items(reason.paths) { path ->
                                    Text(
                                        text = path,
                                        style = DockMono.copy(fontSize = 11.sp),
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
                            .height(54.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.dock, contentColor = colors.onDock)
                    ) {
                        Text(
                            "Review ${reason.count} files",
                            style = DockBody.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                } else {
                    Button(
                        onClick = onPrimaryAction,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.danger)
                    ) {
                        Text(
                            "Confirm deletion on SSD",
                            color = Color.White,
                            style = DockBody.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            } else {
                val actionLabel = when (reason) {
                    is AttentionReason.NoSpace -> "Retry after freeing space"
                    is AttentionReason.PermissionLost -> "Grant access again"
                    is AttentionReason.ReadErrors -> "Retry read"
                    is AttentionReason.MassDelete -> "Confirm deletions"
                }
                Button(
                    onClick = onPrimaryAction,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.dock, contentColor = colors.onDock)
                ) {
                    Text(
                        actionLabel,
                        style = DockBody.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    "Cancel",
                    style = DockBodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.textSecondary
                )
            }
        }
    }
}
