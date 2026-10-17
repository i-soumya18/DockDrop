package `in`.soumyaranjansahoo.dockdrop.ui.screens.onboarding

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockBody
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockBodySmall
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockHeadline
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockTheme
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockTitle
import `in`.soumyaranjansahoo.dockdrop.ui.theme.LocalReducedMotion
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WelcomeScreen(
    onFinishOnboarding: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = DockTheme.colors
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { 3 })
    val isReducedMotion = LocalReducedMotion.current
    var showHowToCheckSheet by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = colors.bg,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "DockDrop",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp,
                    color = colors.textPrimary
                )

                TextButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onFinishOnboarding()
                    }
                ) {
                    Text(
                        text = "Skip",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.textSecondary
                    )
                }
            }
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Expanding Capsule Page Dots (Google M3 Expressive)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(3) { index ->
                        val isSelected = pagerState.currentPage == index
                        val dotWidth by animateDpAsState(
                            targetValue = if (isSelected) 26.dp else 8.dp,
                            animationSpec = if (isReducedMotion) spring() else spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            ),
                            label = "dotWidth"
                        )
                        Box(
                            modifier = Modifier
                                .height(8.dp)
                                .width(dotWidth)
                                .clip(CircleShape)
                                .background(if (isSelected) colors.dock else colors.outline.copy(alpha = 0.6f))
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                val isLastPage = pagerState.currentPage == 2
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        if (isLastPage) {
                            onFinishOnboarding()
                        } else {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.dock,
                        contentColor = colors.onDock
                    )
                ) {
                    Text(
                        text = if (isLastPage) "Get started" else "Continue",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    ) { innerPadding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) { page ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 28.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                when (page) {
                    0 -> WelcomePageOne(colors.dock, colors.outline)
                    1 -> WelcomePageTwo(colors.seal, colors.outline)
                    2 -> WelcomePageThree(
                        dockColor = colors.dock,
                        sealColor = colors.seal,
                        outlineColor = colors.outline,
                        onHowToCheckClick = { showHowToCheckSheet = true }
                    )
                }
            }
        }
    }

    if (showHowToCheckSheet) {
        ModalBottomSheet(
            onDismissRequest = { showHowToCheckSheet = false },
            containerColor = colors.surface,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 36.dp, top = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(if (colors.isDark) Color(0xFF0D4327) else Color(0xFFDCFCE7)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Lock,
                            contentDescription = null,
                            tint = if (colors.isDark) Color(0xFF86EFAC) else Color(0xFF15803D),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(
                        text = "Verify zero network access",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Android strictly enforces manifest permissions. You can verify that DockDrop has zero network access directly in system settings:",
                    fontSize = 13.sp,
                    color = colors.textSecondary,
                    lineHeight = 19.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                InstructionStep(num = "1", text = "Open Android Settings → Apps → DockDrop")
                InstructionStep(num = "2", text = "Tap Permissions")
                InstructionStep(num = "3", text = "Notice that 'Internet' or 'Network' is completely absent — DockDrop has zero network access.")

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        openAppSettings(context)
                        showHowToCheckSheet = false
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.dock,
                        contentColor = colors.onDock
                    )
                ) {
                    Text("Open App Info in Settings", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun WelcomePageOne(accentColor: Color, outlineColor: Color) {
    val colors = DockTheme.colors

    Box(
        modifier = Modifier.size(200.dp),
        contentAlignment = Alignment.Center
    ) {
        // Radial ambient backdrop
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(colors.surfaceRaised.copy(alpha = 0.8f), Color.Transparent),
                    center = center,
                    radius = size.width * 0.48f
                ),
                radius = size.width * 0.48f,
                center = center
            )

            // Phone glyph (left)
            drawRoundRect(
                color = colors.surface,
                topLeft = Offset(center.x - 72.dp.toPx(), center.y - 46.dp.toPx()),
                size = Size(48.dp.toPx(), 84.dp.toPx()),
                cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx())
            )
            drawRoundRect(
                color = outlineColor,
                topLeft = Offset(center.x - 72.dp.toPx(), center.y - 46.dp.toPx()),
                size = Size(48.dp.toPx(), 84.dp.toPx()),
                cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx()),
                style = Stroke(width = 2.dp.toPx())
            )

            // Cable (connecting latch)
            drawLine(
                color = accentColor,
                start = Offset(center.x - 24.dp.toPx(), center.y),
                end = Offset(center.x + 22.dp.toPx(), center.y),
                strokeWidth = 3.5.dp.toPx(),
                cap = StrokeCap.Round
            )

            // SSD glyph (right)
            drawRoundRect(
                color = colors.surface,
                topLeft = Offset(center.x + 22.dp.toPx(), center.y - 38.dp.toPx()),
                size = Size(54.dp.toPx(), 74.dp.toPx()),
                cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx())
            )
            drawRoundRect(
                color = accentColor,
                topLeft = Offset(center.x + 22.dp.toPx(), center.y - 38.dp.toPx()),
                size = Size(54.dp.toPx(), 74.dp.toPx()),
                cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx()),
                style = Stroke(width = 2.5.dp.toPx())
            )
            // LED on SSD
            drawRoundRect(
                color = accentColor,
                topLeft = Offset(center.x + 36.dp.toPx(), center.y + 24.dp.toPx()),
                size = Size(26.dp.toPx(), 3.5.dp.toPx()),
                cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
            )
        }
    }

    Spacer(modifier = Modifier.height(28.dp))

    Text(
        text = "Your phone. Your SSD.\nNothing in between.",
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 32.sp,
        color = colors.textPrimary,
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(12.dp))

    Text(
        text = "Plug in a USB-C drive you own, tap once, and your photos, videos and documents are copied and verified.",
        fontSize = 14.sp,
        color = colors.textSecondary,
        textAlign = TextAlign.Center,
        lineHeight = 21.sp
    )
}

@Composable
private fun WelcomePageTwo(sealColor: Color, outlineColor: Color) {
    val colors = DockTheme.colors

    Box(
        modifier = Modifier.size(200.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)

            // Radial ambient
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(sealColor.copy(alpha = 0.12f), Color.Transparent),
                    center = center,
                    radius = size.width * 0.48f
                ),
                radius = size.width * 0.48f,
                center = center
            )

            // Receipt card body
            drawRoundRect(
                color = colors.surface,
                topLeft = Offset(center.x - 52.dp.toPx(), center.y - 68.dp.toPx()),
                size = Size(104.dp.toPx(), 136.dp.toPx()),
                cornerRadius = CornerRadius(14.dp.toPx(), 14.dp.toPx())
            )
            drawRoundRect(
                color = outlineColor,
                topLeft = Offset(center.x - 52.dp.toPx(), center.y - 68.dp.toPx()),
                size = Size(104.dp.toPx(), 136.dp.toPx()),
                cornerRadius = CornerRadius(14.dp.toPx(), 14.dp.toPx()),
                style = Stroke(width = 2.dp.toPx())
            )

            // Verification seal circle
            drawCircle(
                color = sealColor,
                radius = 24.dp.toPx(),
                center = center,
                style = Stroke(width = 2.5.dp.toPx())
            )

            // Checkmark
            val check = Path().apply {
                moveTo(center.x - 9.dp.toPx(), center.y + 1.dp.toPx())
                lineTo(center.x - 2.dp.toPx(), center.y + 8.dp.toPx())
                lineTo(center.x + 11.dp.toPx(), center.y - 6.dp.toPx())
            }
            drawPath(path = check, color = sealColor, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
        }
    }

    Spacer(modifier = Modifier.height(28.dp))

    Text(
        text = "Verified, not just copied.",
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        color = colors.textPrimary,
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(12.dp))

    Text(
        text = "Every backup ends with a cryptographic Receipt proving every single file matches byte-for-byte on your drive.",
        fontSize = 14.sp,
        color = colors.textSecondary,
        textAlign = TextAlign.Center,
        lineHeight = 21.sp
    )
}

@Composable
private fun WelcomePageThree(
    dockColor: Color,
    sealColor: Color,
    outlineColor: Color,
    onHowToCheckClick: () -> Unit
) {
    val colors = DockTheme.colors

    Box(
        modifier = Modifier.size(200.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)

            // Radial ambient
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFDCFCE7).copy(alpha = 0.5f), Color.Transparent),
                    center = center,
                    radius = size.width * 0.48f
                ),
                radius = size.width * 0.48f,
                center = center
            )

            // Shield outline
            val shield = Path().apply {
                moveTo(center.x, center.y - 48.dp.toPx())
                lineTo(center.x + 42.dp.toPx(), center.y - 28.dp.toPx())
                lineTo(center.x + 42.dp.toPx(), center.y + 12.dp.toPx())
                cubicTo(
                    center.x + 42.dp.toPx(), center.y + 40.dp.toPx(),
                    center.x, center.y + 54.dp.toPx(),
                    center.x, center.y + 54.dp.toPx()
                )
                cubicTo(
                    center.x, center.y + 54.dp.toPx(),
                    center.x - 42.dp.toPx(), center.y + 40.dp.toPx(),
                    center.x - 42.dp.toPx(), center.y + 12.dp.toPx()
                )
                lineTo(center.x - 42.dp.toPx(), center.y - 28.dp.toPx())
                close()
            }
            drawPath(path = shield, color = Color(0xFF15803D), style = Stroke(width = 3.dp.toPx()))

            // Keyhole / lock center
            drawCircle(
                color = Color(0xFF15803D),
                radius = 9.dp.toPx(),
                center = Offset(center.x, center.y - 6.dp.toPx())
            )
            drawRoundRect(
                color = Color(0xFF15803D),
                topLeft = Offset(center.x - 4.5.dp.toPx(), center.y - 6.dp.toPx()),
                size = Size(9.dp.toPx(), 18.dp.toPx()),
                cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
            )
        }
    }

    Spacer(modifier = Modifier.height(20.dp))

    // Offline Pill Chip
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFFDCFCE7))
            .border(1.dp, Color(0xFFBBF7D0), RoundedCornerShape(20.dp))
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Outlined.Lock,
                contentDescription = null,
                tint = Color(0xFF15803D),
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "No network access",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF15803D)
            )
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    Text(
        text = "Fully offline.",
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        color = colors.textPrimary,
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(10.dp))

    Text(
        text = "DockDrop does not request internet permission. Your files never leave your phone and your drive.",
        fontSize = 14.sp,
        color = colors.textSecondary,
        textAlign = TextAlign.Center,
        lineHeight = 21.sp
    )

    Spacer(modifier = Modifier.height(14.dp))

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onHowToCheckClick() }
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "How to check",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.dock
        )
        Spacer(modifier = Modifier.width(4.dp))
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = colors.dock,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun InstructionStep(num: String, text: String) {
    val colors = DockTheme.colors
    Row(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(text = "$num.", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.dock)
        Spacer(modifier = Modifier.width(10.dp))
        Text(text = text, fontSize = 14.sp, color = colors.textSecondary, lineHeight = 20.sp)
    }
}

private fun openAppSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", context.packageName, null)
    }
    context.startActivity(intent)
}
