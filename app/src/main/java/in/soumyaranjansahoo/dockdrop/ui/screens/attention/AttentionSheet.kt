package `in`.soumyaranjansahoo.dockdrop.ui.screens.attention

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import `in`.soumyaranjansahoo.dockdrop.core.model.AttentionReason
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockBody
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockBodySmall
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockHeadline
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockTheme
import `in`.soumyaranjansahoo.dockdrop.ui.theme.RadiusSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttentionSheet(
    reason: AttentionReason,
    onPrimaryAction: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = DockTheme.colors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

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
                color = colors.attention
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = reason.message,
                style = DockBody,
                color = colors.textPrimary
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Nothing was lost. Files copied so far are safe on your SSD.",
                style = DockBodySmall,
                color = colors.textSecondary
            )

            Spacer(modifier = Modifier.height(28.dp))

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
                        is AttentionReason.MassDelete -> "Review files"
                        is AttentionReason.ReadErrors -> "Retry"
                    },
                    style = DockBody.copy(fontWeight = FontWeight.SemiBold)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(onClick = onDismiss) {
                Text("Dismiss", style = DockBodySmall, color = colors.textTertiary)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
