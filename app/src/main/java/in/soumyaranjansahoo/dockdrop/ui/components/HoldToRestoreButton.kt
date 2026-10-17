package `in`.soumyaranjansahoo.dockdrop.ui.components

import android.view.accessibility.AccessibilityManager
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockBody
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockMotion
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockTheme
import `in`.soumyaranjansahoo.dockdrop.ui.theme.HapticFeedbackController
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import `in`.soumyaranjansahoo.dockdrop.ui.theme.LocalReducedMotion

@Composable
fun HoldToRestoreButton(
    text: String = "Hold to restore everything",
    enabled: Boolean = true,
    isReducedMotion: Boolean = LocalReducedMotion.current,
    haptics: HapticFeedbackController? = null,
    onHoldComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = DockTheme.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val a11yManager = remember {
        context.getSystemService(android.content.Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
    }
    val isTouchExploration = a11yManager?.isTouchExplorationEnabled == true

    var showA11yConfirmDialog by remember { mutableStateOf(false) }
    var isHolding by remember { mutableStateOf(false) }

    val progressAnim = remember { Animatable(0f) }
    val buttonScale = remember { Animatable(1f) }

    // TalkBack Confirmation Dialog (DESIGN.md §5.2.5)
    if (showA11yConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showA11yConfirmDialog = false },
            title = { Text("Restore backup?", color = colors.textPrimary) },
            text = { Text("Are you sure you want to restore your files to their folders?", color = colors.textSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        showA11yConfirmDialog = false
                        haptics?.onHoldComplete()
                        onHoldComplete()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.dock, contentColor = colors.onDock)
                ) {
                    Text("Restore")
                }
            },
            dismissButton = {
                TextButton(onClick = { showA11yConfirmDialog = false }) {
                    Text("Cancel", color = colors.textSecondary)
                }
            },
            containerColor = colors.surface,
            shape = RoundedCornerShape(20.dp)
        )
    }

    val buttonLabel = when {
        !enabled -> text
        progressAnim.value > 0.2f -> "Keep holding…"
        else -> text
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .scale(if (isReducedMotion) 1f else buttonScale.value)
            .clip(RoundedCornerShape(20.dp))
            .background(if (enabled) colors.surfaceRaised else colors.surface)
            .border(1.dp, if (enabled) colors.outline else colors.outline.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
            .semantics {
                if (enabled) {
                    onClick(label = "Restore") {
                        showA11yConfirmDialog = true
                        true
                    }
                }
            }
            .pointerInput(enabled, isTouchExploration) {
                if (!enabled) return@pointerInput

                if (isTouchExploration) {
                    detectTapGestures(
                        onTap = { showA11yConfirmDialog = true }
                    )
                } else {
                    detectTapGestures(
                        onPress = {
                            isHolding = true
                            var completed = false
                            var hapticJob: Job? = null

                            val holdJob = scope.launch {
                                hapticJob = scope.launch {
                                    var elapsed = 0L
                                    while (elapsed < 1200L) {
                                        delay(200L)
                                        elapsed += 200L
                                        haptics?.onHoldTick(elapsed / 1200f)
                                    }
                                }

                                if (!isReducedMotion) {
                                    buttonScale.animateTo(0.97f, animationSpec = DockMotion.Snap)
                                }

                                progressAnim.animateTo(
                                    targetValue = 1f,
                                    animationSpec = tween(durationMillis = 1200, easing = LinearEasing)
                                )

                                completed = true
                                haptics?.onHoldComplete()
                                if (!isReducedMotion) {
                                    buttonScale.animateTo(1.03f, animationSpec = DockMotion.Snap)
                                    buttonScale.animateTo(1f, animationSpec = DockMotion.Settle)
                                }
                                onHoldComplete()
                            }

                            tryAwaitRelease()

                            isHolding = false
                            hapticJob?.cancel()
                            if (!completed) {
                                holdJob.cancel()
                                haptics?.onHoldCancelled()
                                scope.launch {
                                    buttonScale.animateTo(1f, animationSpec = DockMotion.Settle)
                                }
                                scope.launch {
                                    // Settle unwind (DESIGN.md §5.2.5)
                                    progressAnim.animateTo(0f, animationSpec = DockMotion.Settle)
                                }
                            }
                        }
                    )
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // Perimeter ring / fill Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val prog = progressAnim.value
            val strokeW = 3.5.dp.toPx()
            val inset = strokeW / 2f
            val cornerRadiusPx = 20.dp.toPx()
            val r = (cornerRadiusPx - inset).coerceAtLeast(0f)
            val left = inset
            val top = inset
            val right = size.width - inset
            val bottom = size.height - inset
            val midX = size.width / 2f

            // 1. Background subtle fill proportional to hold progress
            if (prog > 0f) {
                drawRoundRect(
                    color = colors.dock.copy(alpha = 0.12f * prog),
                    cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx)
                )
            }

            // 2. Build rounded rectangle perimeter path starting at top center
            val fullPath = Path().apply {
                moveTo(midX, top)
                lineTo(right - r, top)
                if (r > 0f) {
                    arcTo(
                        rect = Rect(right - 2 * r, top, right, top + 2 * r),
                        startAngleDegrees = -90f,
                        sweepAngleDegrees = 90f,
                        forceMoveTo = false
                    )
                }
                lineTo(right, bottom - r)
                if (r > 0f) {
                    arcTo(
                        rect = Rect(right - 2 * r, bottom - 2 * r, right, bottom),
                        startAngleDegrees = 0f,
                        sweepAngleDegrees = 90f,
                        forceMoveTo = false
                    )
                }
                lineTo(left + r, bottom)
                if (r > 0f) {
                    arcTo(
                        rect = Rect(left, bottom - 2 * r, left + 2 * r, bottom),
                        startAngleDegrees = 90f,
                        sweepAngleDegrees = 90f,
                        forceMoveTo = false
                    )
                }
                lineTo(left, top + r)
                if (r > 0f) {
                    arcTo(
                        rect = Rect(left, top, left + 2 * r, top + 2 * r),
                        startAngleDegrees = 180f,
                        sweepAngleDegrees = 90f,
                        forceMoveTo = false
                    )
                }
                lineTo(midX, top)
                close()
            }

            // 3. Progressive perimeter sweep
            if (prog > 0f) {
                // Calm perimeter guide track
                drawPath(
                    path = fullPath,
                    color = colors.outline.copy(alpha = 0.35f),
                    style = Stroke(width = strokeW, cap = StrokeCap.Round)
                )

                // Progressive sweep fill
                if (prog >= 1f) {
                    // Full closed ring in earned seal green
                    drawPath(
                        path = fullPath,
                        color = colors.seal,
                        style = Stroke(width = strokeW, cap = StrokeCap.Round)
                    )
                } else {
                    val pathMeasure = PathMeasure()
                    pathMeasure.setPath(fullPath, forceClosed = true)
                    val totalLength = pathMeasure.length
                    val segmentPath = Path()
                    pathMeasure.getSegment(
                        startDistance = 0f,
                        stopDistance = totalLength * prog.coerceIn(0f, 1f),
                        destination = segmentPath,
                        startWithMoveTo = true
                    )
                    drawPath(
                        path = segmentPath,
                        color = colors.dock,
                        style = Stroke(width = strokeW, cap = StrokeCap.Round)
                    )
                }
            }
        }

        val completionTextColor = if (colors.isDark) colors.seal else colors.sealText
        Text(
            text = buttonLabel,
            style = DockBody.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = if (enabled) (if (progressAnim.value >= 1f) completionTextColor else colors.textPrimary) else colors.textTertiary
            )
        )
    }
}
