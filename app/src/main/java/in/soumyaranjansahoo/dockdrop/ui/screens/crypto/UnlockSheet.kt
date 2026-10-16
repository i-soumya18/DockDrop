package `in`.soumyaranjansahoo.dockdrop.ui.screens.crypto

import android.net.Uri
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
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
        shape = RoundedCornerShape(topStart = RadiusSheet, topEnd = RadiusSheet),
        containerColor = colors.surface,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
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
                    "Enter your passphrase to unlock and browse this backup."
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
                visualTransformation = if (isRecoveryKeyMode) androidx.compose.ui.text.input.VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = if (isRecoveryKeyMode) KeyboardType.Ascii else KeyboardType.Password
                ),
                textStyle = if (isRecoveryKeyMode) DockMono else DockBody,
                modifier = Modifier
                    .fillMaxWidth()
                    .offset { IntOffset(shakeOffset.value.roundToInt(), 0) },
                shape = RoundedCornerShape(14.dp),
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
                    style = DockBodySmall,
                    color = colors.danger
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = {
                    isRecoveryKeyMode = !isRecoveryKeyMode
                    input = ""
                    errorMessage = null
                }) {
                    Text(
                        text = if (isRecoveryKeyMode) "Use passphrase instead" else "Use recovery key instead",
                        style = DockBodySmall,
                        color = colors.dock
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    if (input.isBlank() || isChecking) return@Button
                    isChecking = true
                    errorMessage = null

                    scope.launch {
                        // Brief anti-brute-force delay
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
                            errorMessage = "That doesn't match. Nothing was changed."
                            triggerShake()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = input.isNotBlank() && !isChecking,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.dock,
                    contentColor = colors.onDock
                )
            ) {
                if (isChecking) {
                    CircularProgressIndicator(color = colors.onDock, strokeWidth = 2.dp)
                } else {
                    Text("Unlock", style = DockBody.copy(fontWeight = FontWeight.SemiBold))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(onClick = onDismiss) {
                Text("Cancel", style = DockBodySmall, color = colors.textTertiary)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
