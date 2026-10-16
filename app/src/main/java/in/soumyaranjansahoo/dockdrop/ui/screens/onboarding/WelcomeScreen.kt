package `in`.soumyaranjansahoo.dockdrop.ui.screens.onboarding

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockBody
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockBodySmall
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockHeadline
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockMono
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockTheme
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockTitle
import `in`.soumyaranjansahoo.dockdrop.ui.theme.RadiusMd
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WelcomeScreen(
    onFinishOnboarding: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = DockTheme.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { 3 })
    var showHowToCheckSheet by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = colors.bg,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "DockDrop",
                    style = DockTitle.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
                    color = colors.textPrimary
                )

                TextButton(
                    onClick = onFinishOnboarding,
                    modifier = Modifier.semantics {
                        contentDescription = "Skip onboarding and go to setup"
                    }
                ) {
                    Text(
                        text = "Skip",
                        style = DockBody.copy(color = colors.textSecondary)
                    )
                }
            }
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Page Indicator Dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(3) { index ->
                        val isSelected = pagerState.currentPage == index
                        Box(
                            modifier = Modifier
                                .size(if (isSelected) 10.dp else 8.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) colors.dock else colors.outline)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                val isLastPage = pagerState.currentPage == 2
                Button(
                    onClick = {
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
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.dock,
                        contentColor = colors.onDock
                    )
                ) {
                    Text(
                        text = if (isLastPage) "Get started" else "Continue",
                        style = DockBody.copy(fontWeight = FontWeight.SemiBold)
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
                    .padding(horizontal = 24.dp)
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
            shape = RoundedCornerShape(topStart = RadiusMd, topEnd = RadiusMd)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 36.dp, top = 8.dp)
            ) {
                Text(
                    text = "How to verify zero network access",
                    style = DockHeadline,
                    color = colors.textPrimary
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Android strictly enforces app permissions. You can verify that DockDrop has no internet permission directly in system settings:",
                    style = DockBodySmall,
                    color = colors.textSecondary
                )

                Spacer(modifier = Modifier.height(16.dp))

                InstructionStep(num = "1", text = "Open Android Settings → Apps → DockDrop")
                InstructionStep(num = "2", text = "Tap Permissions")
                InstructionStep(num = "3", text = "Notice that 'Internet' or 'Network access' is not even listed — DockDrop does not declare it.")

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        openAppSettings(context)
                        showHowToCheckSheet = false
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.surfaceRaised,
                        contentColor = colors.dock
                    )
                ) {
                    Text("Open App Info in Settings", style = DockBody.copy(fontWeight = FontWeight.SemiBold))
                }
            }
        }
    }
}

@Composable
private fun WelcomePageOne(accentColor: Color, outlineColor: Color) {
    val colors = DockTheme.colors

    Box(
        modifier = Modifier.size(180.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)

            // Phone glyph (left)
            drawRoundRect(
                color = outlineColor,
                topLeft = Offset(center.x - 70.dp.toPx(), center.y - 45.dp.toPx()),
                size = Size(46.dp.toPx(), 78.dp.toPx()),
                cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx()),
                style = Stroke(width = 2.5.dp.toPx())
            )

            // Cable (connecting line)
            drawLine(
                color = accentColor,
                start = Offset(center.x - 24.dp.toPx(), center.y),
                end = Offset(center.x + 24.dp.toPx(), center.y),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round
            )

            // SSD glyph (right)
            drawRoundRect(
                color = accentColor,
                topLeft = Offset(center.x + 24.dp.toPx(), center.y - 36.dp.toPx()),
                size = Size(50.dp.toPx(), 68.dp.toPx()),
                cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx()),
                style = Stroke(width = 3.dp.toPx())
            )
        }
    }

    Spacer(modifier = Modifier.height(32.dp))

    Text(
        text = "Your phone. Your SSD.\nNothing in between.",
        style = DockHeadline.copy(fontSize = 24.sp, lineHeight = 32.sp),
        color = colors.textPrimary,
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(12.dp))

    Text(
        text = "Plug in a drive you own, tap once, and your photos, videos and documents are copied and verified.",
        style = DockBody,
        color = colors.textSecondary,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun WelcomePageTwo(sealColor: Color, outlineColor: Color) {
    val colors = DockTheme.colors

    Box(
        modifier = Modifier.size(180.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)

            // Receipt card outline
            drawRoundRect(
                color = outlineColor,
                topLeft = Offset(center.x - 48.dp.toPx(), center.y - 64.dp.toPx()),
                size = Size(96.dp.toPx(), 128.dp.toPx()),
                cornerRadius = CornerRadius(14.dp.toPx(), 14.dp.toPx()),
                style = Stroke(width = 2.5.dp.toPx())
            )

            // Verification seal circle in center
            drawCircle(
                color = sealColor,
                radius = 24.dp.toPx(),
                center = center,
                style = Stroke(width = 3.dp.toPx())
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

    Spacer(modifier = Modifier.height(32.dp))

    Text(
        text = "Verified, not just copied.",
        style = DockHeadline.copy(fontSize = 24.sp),
        color = colors.textPrimary,
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(12.dp))

    Text(
        text = "Every backup ends with a cryptographic Receipt proving every single file matches byte-for-byte on your drive.",
        style = DockBody,
        color = colors.textSecondary,
        textAlign = TextAlign.Center
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
        modifier = Modifier.size(180.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)

            // Shield outline
            val shield = Path().apply {
                moveTo(center.x, center.y - 50.dp.toPx())
                lineTo(center.x + 44.dp.toPx(), center.y - 30.dp.toPx())
                lineTo(center.x + 44.dp.toPx(), center.y + 10.dp.toPx())
                cubicTo(
                    center.x + 44.dp.toPx(), center.y + 40.dp.toPx(),
                    center.x, center.y + 55.dp.toPx(),
                    center.x, center.y + 55.dp.toPx()
                )
                cubicTo(
                    center.x, center.y + 55.dp.toPx(),
                    center.x - 44.dp.toPx(), center.y + 40.dp.toPx(),
                    center.x - 44.dp.toPx(), center.y + 10.dp.toPx()
                )
                lineTo(center.x - 44.dp.toPx(), center.y - 30.dp.toPx())
                close()
            }
            drawPath(path = shield, color = sealColor, style = Stroke(width = 3.dp.toPx()))

            // Keyhole / lock center
            drawCircle(
                color = sealColor,
                radius = 10.dp.toPx(),
                center = Offset(center.x, center.y - 8.dp.toPx())
            )
            drawRoundRect(
                color = sealColor,
                topLeft = Offset(center.x - 5.dp.toPx(), center.y - 8.dp.toPx()),
                size = Size(10.dp.toPx(), 18.dp.toPx()),
                cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
            )
        }
    }

    Spacer(modifier = Modifier.height(24.dp))

    // Offline chip
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(colors.surfaceRaised)
            .border(1.dp, colors.outline, RoundedCornerShape(20.dp))
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Text(
            text = "🔒 No network access",
            style = DockMono.copy(fontSize = 12.sp, color = colors.seal)
        )
    }

    Spacer(modifier = Modifier.height(16.dp))

    Text(
        text = "Fully offline.",
        style = DockHeadline.copy(fontSize = 24.sp),
        color = colors.textPrimary,
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(12.dp))

    Text(
        text = "DockDrop has no internet permission. Your files never leave your phone and your drive.",
        style = DockBody,
        color = colors.textSecondary,
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(16.dp))

    Text(
        text = "How to check →",
        style = DockBody.copy(color = colors.dock, fontWeight = FontWeight.SemiBold),
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onHowToCheckClick() }
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}

@Composable
private fun InstructionStep(num: String, text: String) {
    val colors = DockTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(colors.surfaceRaised),
            contentAlignment = Alignment.Center
        ) {
            Text(text = num, style = DockMono.copy(fontSize = 12.sp), color = colors.dock)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = text, style = DockBodySmall, color = colors.textPrimary)
    }
}

private fun openAppSettings(context: Context) {
    try {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
        }
        context.startActivity(intent)
    } catch (_: Exception) {}
}
