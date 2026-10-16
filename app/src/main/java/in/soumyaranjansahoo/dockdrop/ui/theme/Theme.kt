package `in`.soumyaranjansahoo.dockdrop.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

private val DarkColorScheme = darkColorScheme(
    primary = DarkDock,
    onPrimary = DarkOnDock,
    background = DarkBg,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceRaised,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkOutline
)

private val LightColorScheme = lightColorScheme(
    primary = LightDock,
    onPrimary = LightOnDock,
    background = LightBg,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceRaised,
    onSurfaceVariant = LightTextSecondary,
    outline = LightOutline
)

@Composable
fun DockDropTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val dockColors = if (darkTheme) {
        DockColors(
            bg = DarkBg,
            surface = DarkSurface,
            surfaceRaised = DarkSurfaceRaised,
            outline = DarkOutline,
            textPrimary = DarkTextPrimary,
            textSecondary = DarkTextSecondary,
            textTertiary = DarkTextTertiary,
            dock = DarkDock,
            onDock = DarkOnDock,
            seal = DarkSeal,
            attention = DarkAttention,
            danger = DarkDanger
        )
    } else {
        DockColors(
            bg = LightBg,
            surface = LightSurface,
            surfaceRaised = LightSurfaceRaised,
            outline = LightOutline,
            textPrimary = LightTextPrimary,
            textSecondary = LightTextSecondary,
            textTertiary = LightTextTertiary,
            dock = LightDock,
            onDock = LightOnDock,
            seal = LightSeal,
            attention = LightAttention,
            danger = LightDanger
        )
    }

    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    CompositionLocalProvider(LocalDockColors provides dockColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = Shapes,
            content = content
        )
    }
}

object DockTheme {
    val colors: DockColors
        @Composable
        get() = LocalDockColors.current
}