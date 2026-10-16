package `in`.soumyaranjansahoo.dockdrop.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Dark Theme Tokens (Default per DESIGN.md §2)
val DarkBg = Color(0xFF0A0F1C)
val DarkSurface = Color(0xFF121A2B)
val DarkSurfaceRaised = Color(0xFF1A2338)
val DarkOutline = Color(0xFF28334D)
val DarkTextPrimary = Color(0xFFE8EEF9)
val DarkTextSecondary = Color(0xFF9AA7C0)
val DarkTextTertiary = Color(0xFF66738F)
val DarkDock = Color(0xFF4F8CFF)
val DarkOnDock = Color(0xFF06122B)
val DarkSeal = Color(0xFF34D399) // Earned only after verification passes
val DarkAttention = Color(0xFFFBBF24)
val DarkDanger = Color(0xFFFB7185)

// Light Theme Tokens
val LightBg = Color(0xFFF6F8FC)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceRaised = Color(0xFFEEF2FA)
val LightOutline = Color(0xFFD5DCEB)
val LightTextPrimary = Color(0xFF0F172A)
val LightTextSecondary = Color(0xFF475569)
val LightTextTertiary = Color(0xFF94A3B8)
val LightDock = Color(0xFF2F6BEA)
val LightOnDock = Color(0xFFFFFFFF)
val LightSeal = Color(0xFF10B981)
val LightAttention = Color(0xFFF59E0B)
val LightDanger = Color(0xFFE11D48)

// Drop Particle Tints (DESIGN.md §2)
val ParticlePhoto = Color(0xFF4F8CFF)
val ParticleVideo = Color(0xFFA78BFA)
val ParticleDocument = Color(0xFF2DD4BF)
val ParticleOther = Color(0xFF9AA7C0)

data class DockColors(
    val bg: Color,
    val surface: Color,
    val surfaceRaised: Color,
    val outline: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val dock: Color,
    val onDock: Color,
    val seal: Color,
    val attention: Color,
    val danger: Color
)

val LocalDockColors = staticCompositionLocalOf {
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
}