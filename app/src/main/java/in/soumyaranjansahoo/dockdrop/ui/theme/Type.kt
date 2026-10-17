package `in`.soumyaranjansahoo.dockdrop.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val DockDisplay = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = FontWeight.Bold,
    fontSize = 34.sp,
    lineHeight = 40.sp
)

val DockHeadline = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = FontWeight.Bold,
    fontSize = 28.sp,
    lineHeight = 34.sp
)

val DockTitle = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = FontWeight.SemiBold,
    fontSize = 20.sp,
    lineHeight = 26.sp
)

val DockBody = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = FontWeight.Normal,
    fontSize = 16.sp,
    lineHeight = 24.sp
)

val DockBodySmall = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = FontWeight.Normal,
    fontSize = 14.sp,
    lineHeight = 20.sp
)

val DockLabel = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = FontWeight.SemiBold,
    fontSize = 12.sp,
    lineHeight = 16.sp,
    letterSpacing = 0.24.sp
)

// Monospace with tabular numbers (tnum) for speeds, ETAs, file sizes, and paths
val DockMono = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontWeight = FontWeight.Medium,
    fontSize = 13.sp,
    lineHeight = 18.sp,
    fontFeatureSettings = "tnum"
)

// Dedicated counter typography styles with tabular figures (prevents layout wiggles)
val DockCounterDisplay = DockDisplay.copy(fontFeatureSettings = "tnum")
val DockCounterHeadline = DockHeadline.copy(fontFeatureSettings = "tnum")
val DockCounterTitle = DockTitle.copy(fontFeatureSettings = "tnum")
val DockCounterBody = DockBody.copy(fontFeatureSettings = "tnum")

object DockTypography {
    val display: TextStyle = DockDisplay
    val headline: TextStyle = DockHeadline
    val title: TextStyle = DockTitle
    val body: TextStyle = DockBody
    val bodySmall: TextStyle = DockBodySmall
    val label: TextStyle = DockLabel
    val mono: TextStyle = DockMono
    val counterDisplay: TextStyle = DockCounterDisplay
    val counterHeadline: TextStyle = DockCounterHeadline
    val counterTitle: TextStyle = DockCounterTitle
    val counterBody: TextStyle = DockCounterBody
}

val Typography = Typography(
    displayLarge = DockDisplay,
    headlineMedium = DockHeadline,
    titleLarge = DockTitle,
    bodyLarge = DockBody,
    bodyMedium = DockBodySmall,
    labelLarge = DockLabel
)