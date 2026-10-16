package `in`.soumyaranjansahoo.dockdrop.ui.screens.crypto

import android.net.Uri
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
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
import `in`.soumyaranjansahoo.dockdrop.ui.theme.RadiusSheet
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

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
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var isRecoveryKeyMode by remember { mutableStateOf(false) }
    var input by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isChecking by remember { mutableStateOf(false) }

    val shakeOffset = remember { Animatable(0f) }

    fun triggerShake() {
        scope.launch {
            // 2-cycle 4dp shake per DESIGN.md 5.2.1
            for (i in 0 until 2) {
                shakeOffset.animateTo(-4f, tween(50))
                shakeOffset.animateTo(4f, tween(50))
            }
            shakeOffset.animateTo(0f, tween(50))
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
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
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(colors.dock.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Lock,
                    contentDescription = null,
                    tint = colors.dock,
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Encrypted Hardware Vault",
                style = DockHeadline,
                color = colors.textPrimary
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = if (isRecoveryKeyMode) {
                    "Enter your 32-character recovery key to unlock."
                } else {
                    "Enter your passphrase to decrypt and browse this backup."
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
                label = { Text(if (isRecoveryKeyMode) "Recovery Key" else "Passphrase") },
                placeholder = {
                    if (isRecoveryKeyMode) Text("XXXX-XXXX-XXXX-XXXX-...")
                },
                trailingIcon = {
                    if (!isRecoveryKeyMode) {
                        IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                            Icon(
                                imageVector = if (isPasswordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                contentDescription = "Toggle password visibility",
                                tint = colors.textSecondary
                            )
                        }
                    }
                },
                visualTransformation = if (isRecoveryKeyMode || isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = if (isRecoveryKeyMode) KeyboardType.Ascii else KeyboardType.Password
                ),
                textStyle = if (isRecoveryKeyMode) DockMono else DockBody,
                modifier = Modifier
                    .fillMaxWidth()
                    .offset { IntOffset(shakeOffset.value.roundToInt(), 0) },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colors.dock,
                    focusedLabelColor = colors.dock,
                    cursorColor = colors.dock
                )
            )

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = errorMessage!!,
                    style = DockBodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.danger
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = {
                    isRecoveryKeyMode = !isRecoveryKeyMode
                    input = ""
                    errorMessage = null
                }) {
                    Text(
                        text = if (isRecoveryKeyMode) "Use passphrase instead" else "Use recovery key instead",
                        style = DockBodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = colors.dock
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (input.isBlank() || isChecking) return@Button
                    isChecking = true
                    errorMessage = null

                    scope.launch {
                        delay(300)

                        val success = if (isRecoveryKeyMode) {
                            encryptedSetManager.unlockWithRecoveryKey(vaultTreeUri, setId, input)
                        } else {
                            encryptedSetManager.unlockWithPassphrase(vaultTreeUri, setId, input)
                        }

                        isChecking = false
                        if (success) {
                            onUnlockSuccess()
                        } else {
                            errorMessage = "Passphrase or key doesn't match. Nothing was modified."
                            triggerShake()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(18.dp),
                enabled = input.isNotBlank() && !isChecking,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.dock,
                    contentColor = colors.onDock
                )
            ) {
                if (isChecking) {
                    CircularProgressIndicator(color = colors.onDock, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                } else {
                    Text("Unlock Vault", style = DockBody.copy(fontWeight = FontWeight.Bold, fontSize = 16.sp))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(onClick = onDismiss) {
                Text("Cancel", style = DockBodySmall.copy(fontWeight = FontWeight.SemiBold), color = colors.textTertiary)
            }
        }
    }
}
