package `in`.soumyaranjansahoo.dockdrop.ui.screens.crypto

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.PersistableBundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.soumyaranjansahoo.dockdrop.core.crypto.RecoveryKeyCodec
import `in`.soumyaranjansahoo.dockdrop.ui.components.SecureWindowEffect
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockBody
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockBodySmall
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockHeadline
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockMono
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockTheme
import `in`.soumyaranjansahoo.dockdrop.ui.theme.RadiusMd

@Composable
fun EncryptionSetupScreen(
    setName: String,
    onComplete: (passphrase: String, recoveryKey: String) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Enforce ADR-005 FLAG_SECURE on S18
    SecureWindowEffect()

    val colors = DockTheme.colors
    val context = LocalContext.current
    var currentStep by remember { mutableIntStateOf(1) } // 1: Passphrase, 2: Recovery Key, 3: Confirm

    var passphrase by remember { mutableStateOf("") }
    var confirmPassphrase by remember { mutableStateOf("") }
    var passphraseError by remember { mutableStateOf<String?>(null) }

    val recoveryKey = remember { RecoveryKeyCodec.generate() }
    var userLastFour by remember { mutableStateOf("") }
    var gateError by remember { mutableStateOf<String?>(null) }
    var keyCopied by remember { mutableStateOf(false) }
    var keySavedToFile by remember { mutableStateOf(false) }

    val fileExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    val kit = buildString {
                        appendLine("=== DockDrop Recovery Key Kit ===")
                        appendLine("Set Name: $setName")
                        appendLine("Date: ${java.time.LocalDate.now()}")
                        appendLine()
                        appendLine("Recovery Key:")
                        appendLine(recoveryKey)
                        appendLine()
                        appendLine("Important: If you lose both your passphrase and this recovery key,")
                        appendLine("your encrypted backup CANNOT be restored by anyone.")
                    }
                    out.write(kit.toByteArray(Charsets.UTF_8))
                }
                keySavedToFile = true
            } catch (e: Exception) {
                // non-fatal
            }
        }
    }

    Scaffold(
        containerColor = colors.bg,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = {
                    if (currentStep > 1) currentStep-- else onCancel()
                }) {
                    Text("Back", color = colors.dock, style = DockBody)
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "Step $currentStep of 3",
                    style = DockMono.copy(color = colors.textTertiary, fontSize = 12.sp)
                )
            }
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Button(
                    onClick = {
                        when (currentStep) {
                            1 -> {
                                if (passphrase.length < 8) {
                                    passphraseError = "Passphrase must be at least 8 characters"
                                } else if (passphrase != confirmPassphrase) {
                                    passphraseError = "Passphrases do not match"
                                } else {
                                    passphraseError = null
                                    currentStep = 2
                                }
                            }
                            2 -> {
                                if (!RecoveryKeyCodec.verifyLastFourChars(recoveryKey, userLastFour)) {
                                    gateError = "Those 4 characters don't match the end of your key"
                                } else {
                                    gateError = null
                                    currentStep = 3
                                }
                            }
                            3 -> {
                                onComplete(passphrase, recoveryKey)
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
                        text = when (currentStep) {
                            1 -> "Next · Recovery key"
                            2 -> "Next · Confirm"
                            else -> "Enable encryption"
                        },
                        style = DockBody.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            when (currentStep) {
                1 -> {
                    // (a) Passphrase
                    Text("Protect with a passphrase", style = DockHeadline, color = colors.textPrimary)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "We can't recover this for you. Write it down or save it in a password manager.",
                        style = DockBodySmall,
                        color = colors.textSecondary
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    OutlinedTextField(
                        value = passphrase,
                        onValueChange = {
                            passphrase = it
                            passphraseError = null
                        },
                        label = { Text("Passphrase") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.dock,
                            focusedLabelColor = colors.dock,
                            cursorColor = colors.dock
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Strength meter
                    val strength = calculateStrength(passphrase)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Strength: ", style = DockBodySmall, color = colors.textTertiary)
                        Text(
                            text = strength.label,
                            style = DockBodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = when (strength) {
                                Strength.WEAK -> colors.danger
                                Strength.OK -> colors.attention
                                Strength.STRONG -> colors.seal
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = confirmPassphrase,
                        onValueChange = {
                            confirmPassphrase = it
                            passphraseError = null
                        },
                        label = { Text("Confirm Passphrase") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.dock,
                            focusedLabelColor = colors.dock,
                            cursorColor = colors.dock
                        )
                    )

                    if (passphraseError != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(passphraseError!!, style = DockBodySmall, color = colors.danger)
                    }
                }

                2 -> {
                    // (b) Recovery Key
                    Text("Save your recovery key", style = DockHeadline, color = colors.textPrimary)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "If you forget your passphrase, this is the only way to restore your backup.",
                        style = DockBodySmall,
                        color = colors.textSecondary
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(RadiusMd))
                            .border(1.dp, colors.outline, RoundedCornerShape(RadiusMd)),
                        colors = CardDefaults.cardColors(containerColor = colors.surface)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Text(
                                "RECOVERY KEY (32 CHARACTERS)",
                                style = DockMono.copy(fontSize = 11.sp),
                                color = colors.textTertiary
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = recoveryKey,
                                style = DockMono.copy(
                                    fontSize = 15.sp,
                                    lineHeight = 24.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                color = colors.textPrimary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("DockDrop Recovery Key", recoveryKey).apply {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        description.extras = PersistableBundle().apply {
                                            putBoolean("android.content.extra.IS_SENSITIVE", true)
                                        }
                                    }
                                }
                                clipboard.setPrimaryClip(clip)
                                keyCopied = true
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(if (keyCopied) "Copied ✓" else "Copy", style = DockBodySmall)
                        }

                        OutlinedButton(
                            onClick = {
                                fileExportLauncher.launch("DockDrop-Recovery-Key-$setName.txt")
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(if (keySavedToFile) "Saved ✓" else "Save as file", style = DockBodySmall)
                        }
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    // Verification gate
                    Text("Let's check you saved it", style = DockBody.copy(fontWeight = FontWeight.SemiBold), color = colors.textPrimary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Enter the last 4 characters of your recovery key:",
                        style = DockBodySmall,
                        color = colors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = userLastFour,
                        onValueChange = {
                            if (it.length <= 4) {
                                userLastFour = it.uppercase()
                                gateError = null
                            }
                        },
                        placeholder = { Text("e.g. ${recoveryKey.takeLast(4)}") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        textStyle = DockMono.copy(fontSize = 18.sp, color = colors.textPrimary),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.dock,
                            focusedLabelColor = colors.dock,
                            cursorColor = colors.dock
                        )
                    )

                    if (gateError != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(gateError!!, style = DockBodySmall, color = colors.danger)
                    }
                }

                3 -> {
                    // (c) Confirmation
                    Text("You're all set", style = DockHeadline, color = colors.textPrimary)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Encrypted backups can only be restored with your passphrase or your recovery key.",
                        style = DockBodySmall,
                        color = colors.textSecondary
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(RadiusMd))
                            .border(1.dp, colors.outline, RoundedCornerShape(RadiusMd)),
                        colors = CardDefaults.cardColors(containerColor = colors.surface)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Text("ENCRYPTION SUMMARY", style = DockMono.copy(fontSize = 11.sp), color = colors.textTertiary)
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("• Format: AES-256-GCM chunked streams (DDC1)", style = DockBodySmall, color = colors.textPrimary)
                            Text("• Filenames: Obfuscated with HMAC-SHA256", style = DockBodySmall, color = colors.textPrimary)
                            Text("• Offline unlock: Verified before any file is read", style = DockBodySmall, color = colors.textPrimary)
                            Text("• Storage: Passphrase is never stored anywhere", style = DockBodySmall, color = colors.seal)
                        }
                    }
                }
            }
        }
    }
}

private enum class Strength(val label: String) {
    WEAK("Weak"),
    OK("OK"),
    STRONG("Strong")
}

private fun calculateStrength(p: String): Strength {
    if (p.length < 8) return Strength.WEAK
    var score = 0
    if (p.length >= 12) score++
    if (p.any { it.isUpperCase() } && p.any { it.isLowerCase() }) score++
    if (p.any { it.isDigit() }) score++
    if (p.any { !it.isLetterOrDigit() }) score++
    return when {
        score >= 3 -> Strength.STRONG
        score >= 1 -> Strength.OK
        else -> Strength.WEAK
    }
}
