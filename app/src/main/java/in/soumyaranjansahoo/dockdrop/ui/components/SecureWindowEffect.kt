package `in`.soumyaranjansahoo.dockdrop.ui.components

import android.app.Activity
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext

/**
 * Applies WindowManager.LayoutParams.FLAG_SECURE while in composition and clears it on dispose.
 * Enforces ADR-005: scoped strictly to S18 (Encryption Setup) and S19 (Unlock Sheet).
 */
@Composable
fun SecureWindowEffect() {
    val context = LocalContext.current
    DisposableEffect(Unit) {
        val window = (context as? Activity)?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }
}
