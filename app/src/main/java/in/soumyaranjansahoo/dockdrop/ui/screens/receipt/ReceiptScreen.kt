package `in`.soumyaranjansahoo.dockdrop.ui.screens.receipt

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import `in`.soumyaranjansahoo.dockdrop.core.model.Receipt
import `in`.soumyaranjansahoo.dockdrop.ui.components.PerforatedReceiptCard
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockTheme

@Composable
fun ReceiptScreen(
    receipt: Receipt,
    onDone: () -> Unit,
    onTestRestoreClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val colors = DockTheme.colors

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = colors.bg
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            PerforatedReceiptCard(
                receipt = receipt,
                onDone = onDone,
                onTestRestoreClick = onTestRestoreClick
            )
        }
    }
}
