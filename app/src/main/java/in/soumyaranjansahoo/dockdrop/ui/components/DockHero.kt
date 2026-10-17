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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import `in`.soumyaranjansahoo.dockdrop.core.model.DropState
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockTheme
import `in`.soumyaranjansahoo.dockdrop.ui.theme.LocalReducedMotion

@Composable
fun DockHero(
    state: DropState,
    modifier: Modifier = Modifier,
    progressPct: Float = 0f
) {
    val colors = DockTheme.colors
    val isReducedMotion = LocalReducedMotion.current

    // Breathe transition for absent/waiting state
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

    // Sweep scan line for verifying state
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
            .size(210.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = "Dock SSD status"
                stateDescription = a11yStateDescription
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f + shakeOffset.value, size.height / 2f)
            val ringRadius = size.minDimension * 0.44f

            when (state) {
                is DropState.NoDock -> {
                    // Soft background glow
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(colors.surfaceRaised.copy(alpha = 0.5f), Color.Transparent),
                            center = center,
                            radius = ringRadius
                        ),
                        radius = ringRadius,
                        center = center
                    )
                    // Dashed ring track
                    drawCircle(
                        color = colors.outline.copy(alpha = breatheAlpha * 0.6f),
                        radius = ringRadius,
                        center = center,
                        style = Stroke(
                            width = 3.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
                        )
                    )
                    drawSolidSsd(
                        center = center,
                        isGhost = true,
                        alpha = breatheAlpha,
                        bodyColor = colors.surface,
                        borderColor = colors.outline,
                        ledColor = colors.textTertiary,
                        cableColor = colors.dock
                    )
                }

                is DropState.DockDetected, is DropState.AwaitingConfirm, is DropState.Preflight -> {
                    // Radial gradient background
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(colors.surfaceRaised.copy(alpha = 0.85f), colors.surfaceRaised.copy(alpha = 0.2f)),
                            center = center,
                            radius = ringRadius
                        ),
                        radius = ringRadius,
                        center = center
                    )
                    // Outer ring base
                    drawCircle(
                        color = colors.outline.copy(alpha = 0.6f),
                        radius = ringRadius,
                        center = center,
                        style = Stroke(width = 3.5.dp.toPx())
                    )
                    // Mockup prominent blue accent arc on top right (approx -60° to 15°)
                    drawArc(
                        color = colors.dock,
                        startAngle = -65f,
                        sweepAngle = 75f,
                        useCenter = false,
                        topLeft = Offset(center.x - ringRadius, center.y - ringRadius),
                        size = Size(ringRadius * 2, ringRadius * 2),
                        style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                    )

                    drawSolidSsd(
                        center = center,
                        isGhost = false,
                        alpha = 1f,
                        bodyColor = colors.surface,
                        borderColor = colors.outline,
                        ledColor = colors.dock,
                        cableColor = colors.dock
                    )
                }

                is DropState.Dropping -> {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(colors.surfaceRaised.copy(alpha = 0.85f), colors.surfaceRaised.copy(alpha = 0.2f)),
                            center = center,
                            radius = ringRadius
                        ),
                        radius = ringRadius,
                        center = center
                    )
                    drawCircle(
                        color = colors.outline.copy(alpha = 0.6f),
                        radius = ringRadius,
                        center = center,
                        style = Stroke(width = 3.5.dp.toPx())
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

                    drawSolidSsd(
                        center = center,
                        isGhost = false,
                        alpha = 1f,
                        bodyColor = colors.surface,
                        borderColor = colors.outline,
                        ledColor = colors.dock,
                        cableColor = colors.dock
                    )
                }

                is DropState.Verifying -> {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(colors.surfaceRaised.copy(alpha = 0.85f), colors.surfaceRaised.copy(alpha = 0.2f)),
                            center = center,
                            radius = ringRadius
                        ),
                        radius = ringRadius,
                        center = center
                    )
                    drawCircle(
                        color = colors.outline.copy(alpha = 0.6f),
                        radius = ringRadius,
                        center = center,
                        style = Stroke(width = 3.5.dp.toPx())
                    )
                    drawArc(
                        color = colors.dock,
                        startAngle = scanAngle,
                        sweepAngle = 70f,
                        useCenter = false,
                        topLeft = Offset(center.x - ringRadius, center.y - ringRadius),
                        size = Size(ringRadius * 2, ringRadius * 2),
                        style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round)
                    )

                    drawSolidSsd(
                        center = center,
                        isGhost = false,
                        alpha = 1f,
                        bodyColor = colors.surface,
                        borderColor = colors.outline,
                        ledColor = colors.dock,
                        cableColor = colors.dock
                    )
                }

                is DropState.Sealed -> {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(colors.seal.copy(alpha = 0.12f), Color.Transparent),
                            center = center,
                            radius = ringRadius
                        ),
                        radius = ringRadius,
                        center = center
                    )
                    drawCircle(
                        color = colors.seal,
                        radius = ringRadius,
                        center = center,
                        style = Stroke(width = 4.dp.toPx())
                    )

                    drawSolidSsd(
                        center = center,
                        isGhost = false,
                        alpha = 1f,
                        bodyColor = colors.surface,
                        borderColor = colors.seal.copy(alpha = 0.4f),
                        ledColor = colors.seal,
                        cableColor = colors.seal
                    )
                    drawCheckmark(center, checkmarkTrim.value, colors.seal)
                }

                is DropState.Partial -> {
                    drawCircle(
                        color = colors.attention,
                        radius = ringRadius,
                        center = center,
                        style = Stroke(width = 3.5.dp.toPx())
                    )
                    drawSolidSsd(
                        center = center,
                        isGhost = false,
                        alpha = 1f,
                        bodyColor = colors.surface,
                        borderColor = colors.attention.copy(alpha = 0.5f),
                        ledColor = colors.attention,
                        cableColor = colors.attention
                    )
                }

                is DropState.Attention, is DropState.Interrupted, is DropState.Paused, is DropState.Resumable -> {
                    val tint = if (state is DropState.Attention) colors.attention else colors.dock
                    drawCircle(
                        color = tint,
                        radius = ringRadius,
                        center = center,
                        style = Stroke(
                            width = 3.5.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 12f))
                        )
                    )
                    drawSolidSsd(
                        center = center,
                        isGhost = false,
                        alpha = 0.9f,
                        bodyColor = colors.surface,
                        borderColor = tint.copy(alpha = 0.5f),
                        ledColor = tint,
                        cableColor = tint
                    )
                }
            }
        }
    }
}

/**
 * Draws the high-fidelity tactile SSD body with top cable and bottom LED light
 * matching the mockup illustration.
 */
private fun DrawScope.drawSolidSsd(
    center: Offset,
    isGhost: Boolean,
    alpha: Float,
    bodyColor: Color,
    borderColor: Color,
    ledColor: Color,
    cableColor: Color,
    shadowColor: Color = borderColor.copy(alpha = 0.35f)
) {
    val driveWidth = 74.dp.toPx()
    val driveHeight = 98.dp.toPx()
    val driveTopLeft = Offset(center.x - driveWidth / 2f, center.y - driveHeight / 2f + 4.dp.toPx())
    val cornerRadius = CornerRadius(14.dp.toPx(), 14.dp.toPx())

    // 1. Cable & Top Plug Latch
    val plugWidth = 16.dp.toPx()
    val plugHeight = 8.dp.toPx()
    val plugTop = driveTopLeft.y - plugHeight + 2.dp.toPx()
    val cableLength = 26.dp.toPx()
    val cableTop = plugTop - cableLength

    if (isGhost) {
        // Ghost cable
        drawLine(
            color = cableColor.copy(alpha = alpha * 0.5f),
            start = Offset(center.x, plugTop),
            end = Offset(center.x, cableTop),
            strokeWidth = 3.5.dp.toPx(),
            cap = StrokeCap.Round,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
        )
    } else {
        // Solid latching cable
        drawLine(
            color = cableColor,
            start = Offset(center.x, plugTop + 2.dp.toPx()),
            end = Offset(center.x, cableTop),
            strokeWidth = 4.dp.toPx(),
            cap = StrokeCap.Round
        )
        // Blue plug connector
        drawRoundRect(
            color = cableColor,
            topLeft = Offset(center.x - plugWidth / 2f, plugTop),
            size = Size(plugWidth, plugHeight),
            cornerRadius = CornerRadius(2.5.dp.toPx(), 2.5.dp.toPx())
        )
    }

    // 2. Drive Solid Body Fill
    if (!isGhost) {
        // Subtle tonal depth underlayer adapting to theme
        drawRoundRect(
            color = shadowColor,
            topLeft = Offset(driveTopLeft.x, driveTopLeft.y + 2.dp.toPx()),
            size = Size(driveWidth, driveHeight),
            cornerRadius = cornerRadius
        )
    }

    drawRoundRect(
        color = if (isGhost) bodyColor.copy(alpha = alpha * 0.4f) else bodyColor,
        topLeft = driveTopLeft,
        size = Size(driveWidth, driveHeight),
        cornerRadius = cornerRadius,
        style = Fill
    )

    // 3. Drive Border Outline
    val borderEffect = if (isGhost) PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f) else null
    drawRoundRect(
        color = borderColor.copy(alpha = if (isGhost) alpha else 0.9f),
        topLeft = driveTopLeft,
        size = Size(driveWidth, driveHeight),
        cornerRadius = cornerRadius,
        style = Stroke(width = 1.6.dp.toPx(), pathEffect = borderEffect)
    )

    // 4. Subtle Top Notch/Port Detail
    val notchWidth = 20.dp.toPx()
    val notchHeight = 4.dp.toPx()
    drawRoundRect(
        color = borderColor.copy(alpha = if (isGhost) alpha * 0.4f else 0.7f),
        topLeft = Offset(center.x - notchWidth / 2f, driveTopLeft.y + 14.dp.toPx()),
        size = Size(notchWidth, notchHeight),
        cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
    )

    // 5. Bottom Glowing LED Light Strip
    val ledWidth = 22.dp.toPx()
    val ledHeight = 3.5.dp.toPx()
    val ledTop = driveTopLeft.y + driveHeight - 16.dp.toPx()
    drawRoundRect(
        color = ledColor.copy(alpha = if (isGhost) alpha * 0.3f else 1f),
        topLeft = Offset(center.x - ledWidth / 2f, ledTop),
        size = Size(ledWidth, ledHeight),
        cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
    )
}

private fun DrawScope.drawCheckmark(center: Offset, trim: Float, tint: Color) {
    if (trim <= 0f) return
    val checkPath = Path().apply {
        moveTo(center.x - 14.dp.toPx(), center.y + 2.dp.toPx())
        lineTo(center.x - 3.dp.toPx(), center.y + 13.dp.toPx())
        lineTo(center.x + 16.dp.toPx(), center.y - 10.dp.toPx())
    }
    if (trim >= 1f) {
        drawPath(
            path = checkPath,
            color = tint,
            style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round)
        )
    } else {
        val pathMeasure = PathMeasure()
        pathMeasure.setPath(checkPath, forceClosed = false)
        val segment = Path()
        pathMeasure.getSegment(0f, pathMeasure.length * trim, segment, startWithMoveTo = true)
        drawPath(
            path = segment,
            color = tint,
            style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}
