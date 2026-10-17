package `in`.soumyaranjansahoo.dockdrop.ui.screens.crypto

import android.net.Uri
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.soumyaranjansahoo.dockdrop.core.crypto.EncryptedSetManager
import `in`.soumyaranjansahoo.dockdrop.ui.components.SecureWindowEffect
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockBody
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockBodySmall
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockHeadline
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockMono
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockTheme
import `in`.soumyaranjansahoo.dockdrop.ui.theme.HapticFeedbackController
import `in`.soumyaranjansahoo.dockdrop.ui.theme.LocalReducedMotion
import `in`.soumyaranjansahoo.dockdrop.ui.theme.RadiusMd
import `in`.soumyaranjansahoo.dockdrop.ui.theme.RadiusSheet
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Hardware-grade Canvas keyhole breathing glyph adhering to Rule 8 & DESIGN.md §5.2.8.
 * Replaces generic CircularProgressIndicator during passphrase verification.
 */
@Composable
fun KeyholeBreathingGlyph(
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 22.dp
) {
    val isReducedMotion = LocalReducedMotion.current
    val infiniteTransition = rememberInfiniteTransition(label = "KeyholeBreathingTransition")

    val animatedPulseProgress by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseProgress"
    )

    val animatedBreatheAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.90f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breatheAlpha"
    )

    val pulseProgress = if (isReducedMotion) 0.5f else animatedPulseProgress
    val breatheAlpha = if (isReducedMotion) 0.85f else animatedBreatheAlpha

    Canvas(
        modifier = modifier
            .size(size)
            .semantics {
                contentDescription = "Verifying passphrase"
                liveRegion = LiveRegionMode.Polite
            }
    ) {
        val center = Offset(this.size.width / 2f, this.size.height / 2f)
        val maxRadius = this.size.minDimension / 2f

        // 1. Outer precision reticle ring
        drawCircle(
            color = tint.copy(alpha = 0.35f * breatheAlpha),
            radius = maxRadius * 0.92f,
            center = center,
            style = Stroke(width = 1.2.dp.toPx())
        )

        // 2. Inner reticle ring
        drawCircle(
            color = tint.copy(alpha = 0.50f * breatheAlpha),
            radius = maxRadius * 0.62f,
            center = center,
            style = Stroke(width = 1.2.dp.toPx())
        )

        if (!isReducedMotion) {
            val pulseRadius = (maxRadius * 0.62f) + ((maxRadius * 0.92f - maxRadius * 0.62f) * pulseProgress)
            val pulseAlpha = ((1f - pulseProgress) * 0.55f).coerceIn(0f, 1f)
            drawCircle(
                color = tint.copy(alpha = pulseAlpha),
                radius = pulseRadius,
                center = center,
                style = Stroke(width = 1.5.dp.toPx())
            )
        } else {
            val tickLen = 3.dp.toPx()
            drawLine(
                color = tint.copy(alpha = 0.6f),
                start = Offset(center.x, center.y - maxRadius * 0.92f),
                end = Offset(center.x, center.y - maxRadius * 0.92f + tickLen),
                strokeWidth = 1.2.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawLine(
                color = tint.copy(alpha = 0.6f),
                start = Offset(center.x, center.y + maxRadius * 0.92f),
                end = Offset(center.x, center.y + maxRadius * 0.92f - tickLen),
                strokeWidth = 1.2.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawLine(
                color = tint.copy(alpha = 0.6f),
                start = Offset(center.x - maxRadius * 0.92f, center.y),
                end = Offset(center.x - maxRadius * 0.92f + tickLen, center.y),
                strokeWidth = 1.2.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawLine(
                color = tint.copy(alpha = 0.6f),
                start = Offset(center.x + maxRadius * 0.92f, center.y),
                end = Offset(center.x + maxRadius * 0.92f - tickLen, center.y),
                strokeWidth = 1.2.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        // Center Keyhole Silhouette
        val headRadius = maxRadius * 0.32f
        val headCenter = Offset(center.x, center.y - headRadius * 0.35f)
        val slotTopWidth = headRadius * 0.75f
        val slotBotWidth = headRadius * 1.15f
        val slotBottomY = center.y + maxRadius * 0.52f

        val keyholePath = Path().apply {
            addOval(Rect(headCenter, headRadius))
            moveTo(center.x - slotTopWidth / 2f, headCenter.y + headRadius * 0.5f)
            lineTo(center.x + slotTopWidth / 2f, headCenter.y + headRadius * 0.5f)
            lineTo(center.x + slotBotWidth / 2f, slotBottomY)
            lineTo(center.x - slotBotWidth / 2f, slotBottomY)
            close()
        }

        drawPath(
            path = keyholePath,
            color = tint.copy(alpha = breatheAlpha)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnlockSheet(
    vaultTreeUri: Uri,
    setId: String,
    encryptedSetManager: EncryptedSetManager,
    onUnlockSuccess: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Enforce ADR-005 FLAG_SECURE on S19
    SecureWindowEffect()

    val colors = DockTheme.colors
    val context = LocalContext.current
    val haptics = remember(context) { HapticFeedbackController(context) }
    val isReducedMotion = LocalReducedMotion.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var isRecoveryKeyMode by remember { mutableStateOf(false) }
    var input by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isChecking by remember { mutableStateOf(false) }

    val shakeOffset = remember { Animatable(0f) }

    fun triggerShake() {
        if (!isReducedMotion) {
            scope.launch {
                // 2-cycle 4dp shake per DESIGN.md §5.2.1
                for (i in 0 until 2) {
                    shakeOffset.animateTo(-4f, tween(50))
                    shakeOffset.animateTo(4f, tween(50))
                }
                shakeOffset.animateTo(0f, tween(50))
            }
        }
    }

    fun submitUnlock() {
        if (input.isBlank() || isChecking) return
        isChecking = true
        errorMessage = null

        scope.launch {
            delay(150)

            val success = if (isRecoveryKeyMode) {
                encryptedSetManager.unlockWithRecoveryKey(vaultTreeUri, setId, input)
            } else {
                encryptedSetManager.unlockWithPassphrase(vaultTreeUri, setId, input)
            }

            isChecking = false
            if (success) {
                onUnlockSuccess()
            } else {
                errorMessage = "That doesn't match. Nothing was changed."
                haptics.onAttention()
                triggerShake()
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = RadiusSheet, topEnd = RadiusSheet),
        containerColor = colors.surface,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(colors.dock.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Lock,
                    contentDescription = null,
                    tint = colors.dock,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "This backup is encrypted",
                style = DockHeadline,
                color = colors.textPrimary
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = if (isRecoveryKeyMode) {
                    "Enter your 32-character recovery key to unlock."
                } else {
                    "Enter your passphrase to decrypt and restore this backup."
                },
                style = DockBodySmall,
                color = colors.textSecondary
            )

            Spacer(modifier = Modifier.height(20.dp))

            OutlinedTextField(
                value = input,
                onValueChange = {
                    input = it
                    errorMessage = null
                },
                isError = errorMessage != null,
                label = { Text(if (isRecoveryKeyMode) "Recovery key" else "Passphrase") },
                placeholder = {
                    if (isRecoveryKeyMode) Text("XXXX-XXXX-XXXX-XXXX-...")
                },
                trailingIcon = {
                    if (!isRecoveryKeyMode) {
                        IconButton(
                            onClick = { isPasswordVisible = !isPasswordVisible },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = if (isPasswordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                contentDescription = if (isPasswordVisible) "Hide passphrase" else "Show passphrase",
                                tint = colors.textSecondary
                            )
                        }
                    }
                },
                visualTransformation = if (isRecoveryKeyMode || isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = if (isRecoveryKeyMode) KeyboardType.Ascii else KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { submitUnlock() }
                ),
                textStyle = if (isRecoveryKeyMode) DockMono else DockBody,
                modifier = Modifier
                    .fillMaxWidth()
                    .offset { IntOffset(shakeOffset.value.roundToInt(), 0) },
                shape = RoundedCornerShape(RadiusMd),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colors.dock,
                    focusedLabelColor = colors.dock,
                    cursorColor = colors.dock,
                    errorBorderColor = colors.danger,
                    errorLabelColor = colors.danger
                )
            )

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ErrorOutline,
                        contentDescription = null,
                        tint = colors.danger,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = errorMessage!!,
                        style = DockBodySmall.copy(fontWeight = FontWeight.Medium),
                        color = colors.danger
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                TextButton(
                    onClick = {
                        isRecoveryKeyMode = !isRecoveryKeyMode
                        input = ""
                        errorMessage = null
                    },
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text(
                        text = if (isRecoveryKeyMode) "Use passphrase instead" else "Use recovery key instead",
                        style = DockBodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = colors.dock
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { submitUnlock() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(RadiusMd),
                enabled = input.isNotBlank() && !isChecking,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.dock,
                    contentColor = colors.onDock
                )
            ) {
                if (isChecking) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        KeyholeBreathingGlyph(
                            tint = colors.onDock,
                            size = 22.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            "Verifying…",
                            style = DockBody.copy(fontWeight = FontWeight.Bold, fontSize = 16.sp),
                            color = colors.onDock
                        )
                    }
                } else {
                    Text(
                        "Unlock",
                        style = DockBody.copy(fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
            ) {
                Text(
                    "Cancel",
                    style = DockBodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.textTertiary
                )
            }
        }
    }
}
