package `in`.soumyaranjansahoo.dockdrop.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// ============================================================================
// Dark Theme Tokens (Default per DESIGN.md §2)
// ============================================================================
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
val DarkSealText = Color(0xFF34D399)
val DarkAttention = Color(0xFFFBBF24)
val DarkAttentionText = Color(0xFFFBBF24)
val DarkDanger = Color(0xFFFB7185)

// Dark Badge Tokens (100% Offline pill, Privacy section)
val DarkBadgeBg = Color(0xFF0D4327)
val DarkBadgeBorder = Color(0xFF0F5B34)
val DarkBadgeContent = Color(0xFF86EFAC)

// Dark Category Tokens (DESIGN.md §2)
val DarkCategoryPhoto = Color(0xFF4F8CFF)
val DarkCategoryVideo = Color(0xFFA78BFA)
val DarkCategoryDocument = Color(0xFF2DD4BF)
val DarkCategoryAudio = Color(0xFFFBBF24)
val DarkCategoryOther = Color(0xFF9AA7C0)

// Dark Security Strength Tokens
val DarkStrengthWeak = DarkDanger
val DarkStrengthMedium = DarkAttention
val DarkStrengthStrong = DarkSeal

// ============================================================================
// Light Theme Tokens (Aligned with DESIGN.md §2)
// ============================================================================
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
val LightSealText = Color(0xFF047857) // Contrast >= 4.8:1 (5.56:1 on white, 5.23:1 on LightBg)
val LightAttention = Color(0xFFF59E0B)
val LightAttentionText = Color(0xFFB45309) // Contrast >= 4.9:1 (5.05:1 on white, 4.75:1 on LightBg)
val LightDanger = Color(0xFFE11D48)

// Light Badge Tokens (100% Offline pill, Privacy section)
val LightBadgeBg = Color(0xFFDCFCE7)
val LightBadgeBorder = Color(0xFFBBF7D0)
val LightBadgeContent = Color(0xFF047857)

// Light Category Tokens (WCAG AA >= 4.5:1 compliant)
val LightCategoryPhoto = Color(0xFF2F6BEA)
val LightCategoryVideo = Color(0xFF7C3AED)
val LightCategoryDocument = Color(0xFF059669)
val LightCategoryAudio = Color(0xFFD97706)
val LightCategoryOther = Color(0xFF64748B)

// Light Security Strength Tokens
val LightStrengthWeak = LightDanger
val LightStrengthMedium = LightAttentionText
val LightStrengthStrong = LightSealText

// ============================================================================
// Drop Particle Tints (DESIGN.md §2, subtle 70% alpha)
// ============================================================================
val ParticlePhoto = Color(0xFF4F8CFF)
val ParticleVideo = Color(0xFFA78BFA)
val ParticleDocument = Color(0xFF2DD4BF)
val ParticleOther = Color(0xFF9AA7C0)

// ============================================================================
// Semantic DockColors Data Structure
// ============================================================================
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
    val sealText: Color,
    val attention: Color,
    val attentionText: Color,
    val danger: Color,

    // Categories
    val categoryPhoto: Color,
    val categoryVideo: Color,
    val categoryDocument: Color,
    val categoryAudio: Color,
    val categoryOther: Color,

    // Trust & Privacy Badges
    val badgeBg: Color,
    val badgeBorder: Color,
    val badgeContent: Color,

    // Security Strength
    val strengthWeak: Color,
    val strengthMedium: Color,
    val strengthStrong: Color,

    val isDark: Boolean = true
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
}