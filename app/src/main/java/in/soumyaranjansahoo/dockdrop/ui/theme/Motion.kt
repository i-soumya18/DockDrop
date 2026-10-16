package `in`.soumyaranjansahoo.dockdrop.ui.theme

import android.content.Context
import android.provider.Settings
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.platform.LocalContext

val LocalReducedMotion = compositionLocalOf { false }

@Composable
fun isSystemReducedMotion(): Boolean {
    val context = LocalContext.current
    return try {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1.0f
        ) == 0f
    } catch (_: Exception) {
        false
    }
}

object DockMotion {
    val Snap = spring<Float>(dampingRatio = 0.72f, stiffness = 700f)
    val Settle = spring<Float>(dampingRatio = 0.85f, stiffness = 380f)
    val Flow = tween<Float>(durationMillis = 320, easing = CubicBezierEasing(0.2f, 0f, 0f, 1f))
    val Exit = tween<Float>(durationMillis = 200, easing = CubicBezierEasing(0.3f, 0f, 1f, 1f))

    const val DurationMicro = 120
    const val DurationSmall = 200
    const val DurationMedium = 320
    const val DurationLarge = 500
    const val DurationHero = 900
}
