package `in`.soumyaranjansahoo.dockdrop.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import `in`.soumyaranjansahoo.dockdrop.core.preferences.ThemeMode

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
    themeMode: ThemeMode = ThemeMode.DARK,
    reduceMotionPref: Boolean = false,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }

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
            sealText = DarkSealText,
            attention = DarkAttention,
            attentionText = DarkAttentionText,
            danger = DarkDanger,
            categoryPhoto = DarkCategoryPhoto,
            categoryVideo = DarkCategoryVideo,
            categoryDocument = DarkCategoryDocument,
            categoryAudio = DarkCategoryAudio,
            categoryOther = DarkCategoryOther,
            badgeBg = DarkBadgeBg,
            badgeBorder = DarkBadgeBorder,
            badgeContent = DarkBadgeContent,
            strengthWeak = DarkStrengthWeak,
            strengthMedium = DarkStrengthMedium,
            strengthStrong = DarkStrengthStrong,
            isDark = true
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
            sealText = LightSealText,
            attention = LightAttention,
            attentionText = LightAttentionText,
            danger = LightDanger,
            categoryPhoto = LightCategoryPhoto,
            categoryVideo = LightCategoryVideo,
            categoryDocument = LightCategoryDocument,
            categoryAudio = LightCategoryAudio,
            categoryOther = LightCategoryOther,
            badgeBg = LightBadgeBg,
            badgeBorder = LightBadgeBorder,
            badgeContent = LightBadgeContent,
            strengthWeak = LightStrengthWeak,
            strengthMedium = LightStrengthMedium,
            strengthStrong = LightStrengthStrong,
            isDark = false
        )
    }

    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val isReducedMotion = reduceMotionPref || isSystemReducedMotion()

    CompositionLocalProvider(
        LocalDockColors provides dockColors,
        LocalReducedMotion provides isReducedMotion
    ) {
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

    val typography: DockTypography
        get() = DockTypography
}