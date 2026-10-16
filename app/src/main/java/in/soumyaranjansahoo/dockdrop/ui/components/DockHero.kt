package `in`.soumyaranjansahoo.dockdrop.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import `in`.soumyaranjansahoo.dockdrop.core.model.DropState
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockMotion
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockTheme
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import `in`.soumyaranjansahoo.dockdrop.ui.theme.LocalReducedMotion

@Composable
fun DockHero(
    state: DropState,
    modifier: Modifier = Modifier,
    progressPct: Float = 0f
) {
    val colors = DockTheme.colors
    val isReducedMotion = LocalReducedMotion.current

    // Breathe transition for absent/waiting state (disabled if reduced motion)
    val infiniteTransition = rememberInfiniteTransition(label = "DockBreathe")
    val animatedBreatheAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breatheAlpha"
    )
    val breatheAlpha = if (isReducedMotion) 0.75f else animatedBreatheAlpha

    // Sweep scan line for verifying state (disabled if reduced motion)
    val animatedScanAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scanAngle"
    )
    val scanAngle = if (isReducedMotion) 0f else animatedScanAngle

    // Checkmark draw trim for sealed state
    val checkmarkTrim = remember { Animatable(if (state is DropState.Sealed && isReducedMotion) 1f else 0f) }
    LaunchedEffect(state, isReducedMotion) {
        if (state is DropState.Sealed) {
            if (isReducedMotion) {
                checkmarkTrim.snapTo(1f)
            } else {
                checkmarkTrim.snapTo(0f)
                checkmarkTrim.animateTo(1f, animationSpec = tween(380, easing = FastOutSlowInEasing))
            }
        } else {
            checkmarkTrim.snapTo(0f)
        }
    }

    // Shake offset for Attention state
    val shakeOffset = remember { Animatable(0f) }
    LaunchedEffect(state, isReducedMotion) {
        if (state is DropState.Attention && !isReducedMotion) {
            shakeOffset.animateTo(6f, animationSpec = tween(60))
            shakeOffset.animateTo(-6f, animationSpec = tween(60))
            shakeOffset.animateTo(4f, animationSpec = tween(60))
            shakeOffset.animateTo(0f, animationSpec = tween(60))
        } else {
            shakeOffset.snapTo(0f)
        }
    }

    val a11yStateDescription = when (state) {
        is DropState.NoDock -> "No SSD connected. Plug in a USB-C drive to begin."
        is DropState.DockDetected -> "${state.dock.userLabel} connected"
        is DropState.AwaitingConfirm -> "Ready to back up to ${state.dock.userLabel}"
        is DropState.Preflight -> "Checking SSD write access"
        is DropState.Dropping -> "Backing up ${state.progress.currentFolder}, ${state.progress.percentage}% complete"
        is DropState.Verifying -> "Verifying files in ${state.currentFolder}"
        is DropState.Sealed -> "Backup sealed and 100% verified"
        is DropState.Partial -> "Backup completed with skipped files"
        is DropState.Paused -> "Backup paused: ${state.reason.message}"
        is DropState.Interrupted -> "Backup interrupted: SSD disconnected"
        is DropState.Resumable -> "Backup resumable at ${state.progress.percentage}%"
        is DropState.Attention -> "Attention needed: ${state.reason.title}"
    }

    Box(
        modifier = modifier
            .size(220.dp)
            .semantics(mergeDescendants = true) {
                stateDescription = a11yStateDescription
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f + shakeOffset.value, size.height / 2f)
            val ringRadius = size.minDimension * 0.44f

            when (state) {
                is DropState.NoDock -> {
                    // Dashed drive outline with ghost cable
                    drawDriveBody(center, isSolid = false, alpha = breatheAlpha, colors.textTertiary)
                    drawGhostCable(center, alpha = breatheAlpha, colors.dock)
                }

                is DropState.DockDetected, is DropState.AwaitingConfirm, is DropState.Preflight -> {
                    // Solid drive glyph with latched cable
                    drawDriveBody(center, isSolid = true, alpha = 1f, colors.dock)
                    drawLatchedCable(center, colors.dock)
                    // Outer ring base
                    drawCircle(
                        color = colors.outline,
                        radius = ringRadius,
                        center = center,
                        style = Stroke(width = 3.dp.toPx())
                    )
                }

                is DropState.Dropping -> {
                    drawDriveBody(center, isSolid = true, alpha = 1f, colors.dock)
                    drawLatchedCable(center, colors.dock)
                    // Outer background ring
                    drawCircle(
                        color = colors.outline,
                        radius = ringRadius,
                        center = center,
                        style = Stroke(width = 4.dp.toPx())
                    )
                    // Progress arc
                    val sweep = (progressPct.coerceIn(0f, 1f)) * 360f
                    drawArc(
                        color = colors.dock,
                        startAngle = -90f,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = Offset(center.x - ringRadius, center.y - ringRadius),
                        size = Size(ringRadius * 2, ringRadius * 2),
                        style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                is DropState.Verifying -> {
                    drawDriveBody(center, isSolid = true, alpha = 1f, colors.dock)
                    drawLatchedCable(center, colors.dock)
                    // Thin sweeping scan line
                    drawCircle(
                        color = colors.outline,
                        radius = ringRadius,
                        center = center,
                        style = Stroke(width = 2.dp.toPx())
                    )
                    drawArc(
                        color = colors.seal,
                        startAngle = scanAngle,
                        sweepAngle = 70f,
                        useCenter = false,
                        topLeft = Offset(center.x - ringRadius, center.y - ringRadius),
                        size = Size(ringRadius * 2, ringRadius * 2),
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                is DropState.Sealed -> {
                    // Green is earned: seal checkmark and closed green ring
                    drawDriveBody(center, isSolid = true, alpha = 1f, colors.seal)
                    drawCircle(
                        color = colors.seal,
                        radius = ringRadius,
                        center = center,
                        style = Stroke(width = 4.dp.toPx())
                    )
                    drawCheckmark(center, checkmarkTrim.value, colors.seal)
                }

                is DropState.Partial -> {
                    // Amber icon and ring (never green)
                    drawDriveBody(center, isSolid = true, alpha = 1f, colors.attention)
                    drawCircle(
                        color = colors.attention,
                        radius = ringRadius,
                        center = center,
                        style = Stroke(width = 4.dp.toPx())
                    )
                }

                is DropState.Attention, is DropState.Interrupted, is DropState.Paused, is DropState.Resumable -> {
                    val tint = if (state is DropState.Attention) colors.attention else colors.dock
                    drawDriveBody(center, isSolid = true, alpha = 0.85f, tint)
                    drawCircle(
                        color = colors.attention,
                        radius = ringRadius,
                        center = center,
                        style = Stroke(
                            width = 4.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 12f))
                        )
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawDriveBody(
    center: Offset,
    isSolid: Boolean,
    alpha: Float,
    tint: Color
) {
    val driveWidth = 72.dp.toPx()
    val driveHeight = 92.dp.toPx()
    val topLeft = Offset(center.x - driveWidth / 2f, center.y - driveHeight / 2f)
    val corner = CornerRadius(16.dp.toPx(), 16.dp.toPx())

    val strokeWidth = 2.5.dp.toPx()
    val pathEffect = if (isSolid) null else PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)

    drawRoundRect(
        color = tint.copy(alpha = alpha),
        topLeft = topLeft,
        size = Size(driveWidth, driveHeight),
        cornerRadius = corner,
        style = Stroke(width = strokeWidth, pathEffect = pathEffect)
    )

    // Port notch at top
    val portWidth = 20.dp.toPx()
    val portHeight = 6.dp.toPx()
    drawRoundRect(
        color = tint.copy(alpha = alpha),
        topLeft = Offset(center.x - portWidth / 2f, topLeft.y + 8.dp.toPx()),
        size = Size(portWidth, portHeight),
        cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx()),
        style = Stroke(width = 2.dp.toPx())
    )
}

private fun DrawScope.drawGhostCable(center: Offset, alpha: Float, tint: Color) {
    val cableLength = 36.dp.toPx()
    val startY = center.y - 75.dp.toPx()
    val endY = startY + cableLength

    drawLine(
        color = tint.copy(alpha = alpha * 0.6f),
        start = Offset(center.x, startY),
        end = Offset(center.x, endY),
        strokeWidth = 3.dp.toPx(),
        cap = StrokeCap.Round,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
    )
}

private fun DrawScope.drawLatchedCable(center: Offset, tint: Color) {
    val cableLength = 28.dp.toPx()
    val startY = center.y - 65.dp.toPx()
    val endY = center.y - 37.dp.toPx()

    drawLine(
        color = tint,
        start = Offset(center.x, startY),
        end = Offset(center.x, endY),
        strokeWidth = 3.5.dp.toPx(),
        cap = StrokeCap.Round
    )
}

private fun DrawScope.drawCheckmark(center: Offset, trim: Float, tint: Color) {
    if (trim <= 0f) return
    val checkPath = Path().apply {
        moveTo(center.x - 16.dp.toPx(), center.y + 2.dp.toPx())
        lineTo(center.x - 4.dp.toPx(), center.y + 14.dp.toPx())
        lineTo(center.x + 18.dp.toPx(), center.y - 12.dp.toPx())
    }
    drawPath(
        path = checkPath,
        color = tint,
        style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
    )
}
