package `in`.soumyaranjansahoo.dockdrop.ui.screens.history

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.soumyaranjansahoo.dockdrop.core.model.Receipt
import `in`.soumyaranjansahoo.dockdrop.core.repository.SetRepository
import `in`.soumyaranjansahoo.dockdrop.ui.components.DockEmptyState
import `in`.soumyaranjansahoo.dockdrop.ui.components.DockTopAppBar
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockBodySmall
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockLabel
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockMono
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockTheme
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockTitle
import `in`.soumyaranjansahoo.dockdrop.ui.theme.RadiusMd
import `in`.soumyaranjansahoo.dockdrop.ui.theme.RadiusSm
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    setRepository: SetRepository,
    onReceiptClick: (Receipt) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = DockTheme.colors
    val receipts by setRepository.allReceipts.collectAsState(initial = emptyList())

    val historySubtitle = remember(receipts) {
        if (receipts.isEmpty()) {
            null
        } else {
            val sealedCount = receipts.count { it.isSealed }
            val partialCount = receipts.size - sealedCount
            if (partialCount == 0) {
                "$sealedCount sealed ${if (sealedCount == 1) "run" else "runs"}"
            } else {
                "$sealedCount sealed · $partialCount partial"
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = colors.bg,
        topBar = {
            DockTopAppBar(
                title = "Backup History",
                subtitle = historySubtitle,
                onBackClick = onBackClick
            )
        }
    ) { innerPadding ->
        if (receipts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                DockEmptyState(
                    title = "No Backups Yet",
                    description = "When you back up your phone to an external SSD, cryptographic verification receipts with SHA-256 manifests will appear here.",
                    icon = Icons.Outlined.History
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "CRYPTOGRAPHIC RECEIPTS",
                        style = DockLabel,
                        color = colors.textTertiary
                    )
                }

                items(receipts, key = { it.id }) { receipt ->
                    HistoryReceiptItem(
                        receipt = receipt,
                        onClick = { onReceiptClick(receipt) }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun HistoryReceiptItem(
    receipt: Receipt,
    onClick: () -> Unit
) {
    val colors = DockTheme.colors
    val formattedDate = remember(receipt.timestampMs) {
        SimpleDateFormat("MMM d, yyyy · h:mm a", Locale.US).format(Date(receipt.timestampMs))
    }

    // Rule 2: Green is earned only when receipt.isSealed is true
    val isVerified = receipt.isSealed
    val statusFillColor = if (isVerified) colors.seal else colors.attention
    val statusTextColor = if (isVerified) colors.sealText else colors.attentionText
    val statusText = if (isVerified) "SEALED" else "PARTIAL"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 76.dp)
            .clip(RoundedCornerShape(RadiusMd))
            .border(1.dp, colors.outline, RoundedCornerShape(RadiusMd))
            .clickable(role = Role.Button) { onClick() },
        colors = CardDefaults.cardColors(containerColor = colors.surface),
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
                    .size(46.dp)
                    .clip(RoundedCornerShape(RadiusSm))
                    .background(statusFillColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isVerified) Icons.Outlined.Verified else Icons.Outlined.Warning,
                    contentDescription = null,
                    tint = statusTextColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = receipt.setName,
                        style = DockTitle.copy(fontSize = 16.sp),
                        color = colors.textPrimary,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(statusFillColor.copy(alpha = 0.14f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = statusText,
                            style = DockMono.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = statusTextColor
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "${String.format(Locale.US, "%,d", receipt.totalFilesVerified)} files · ${formatBytes(receipt.totalBytesVerified)}",
                    style = DockMono.copy(fontSize = 12.sp, color = colors.textSecondary)
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = formattedDate,
                    style = DockBodySmall.copy(fontSize = 11.sp),
                    color = colors.textTertiary
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowForwardIos,
                contentDescription = null,
                tint = colors.textTertiary,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

private fun formatBytes(bytes: Long): String {
    val gb = bytes.toDouble() / (1024.0 * 1024.0 * 1024.0)
    if (gb >= 1.0) return String.format(Locale.US, "%.1f GB", gb)
    val mb = bytes.toDouble() / (1024.0 * 1024.0)
    if (mb >= 1.0) return String.format(Locale.US, "%.1f MB", mb)
    val kb = bytes.toDouble() / 1024.0
    return String.format(Locale.US, "%.0f KB", kb.coerceAtLeast(1.0))
}
