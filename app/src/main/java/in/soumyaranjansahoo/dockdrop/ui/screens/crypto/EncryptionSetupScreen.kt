package `in`.soumyaranjansahoo.dockdrop.ui.screens.crypto

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.PersistableBundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.soumyaranjansahoo.dockdrop.core.crypto.RecoveryKeyCodec
import `in`.soumyaranjansahoo.dockdrop.ui.components.DockTopAppBar
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
    var passphraseVisible by remember { mutableStateOf(false) }
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
            DockTopAppBar(
                title = "Hardware Vault Security",
                subtitle = "Step $currentStep of 3",
                onBackClick = {
                    if (currentStep > 1) currentStep-- else onCancel()
                },
                actions = {
                    TextButton(onClick = onCancel) {
                        Text(
                            "Cancel",
                            color = colors.textTertiary,
                            style = DockBodySmall.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }
                }
            )
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
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
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.dock,
                        contentColor = colors.onDock
                    )
                ) {
                    Text(
                        text = when (currentStep) {
                            1 -> "Next: Generate Recovery Key"
                            2 -> "Next: Review & Confirm"
                            else -> "Enable Hardware Encryption"
                        },
                        style = DockBody.copy(fontWeight = FontWeight.Bold, fontSize = 16.sp)
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
            // Step Progress Track
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (i in 1..3) {
                    val isFinished = i <= currentStep
                    val barColor by animateColorAsState(
                        targetValue = if (isFinished) colors.dock else colors.outline.copy(alpha = 0.35f),
                        label = "stepProgress"
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(barColor)
                    )
                }
            }
            Spacer(modifier = Modifier.height(20.dp))

            when (currentStep) {
                1 -> {
                    // (a) Passphrase
                    Text("Protect with a passphrase", style = DockHeadline, color = colors.textPrimary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Your backup will be encrypted with AES-256-GCM. We cannot recover it for you if lost.",
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
                        label = { Text("Vault Passphrase") },
                        trailingIcon = {
                            IconButton(onClick = { passphraseVisible = !passphraseVisible }) {
                                Icon(
                                    imageVector = if (passphraseVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                    contentDescription = "Toggle visibility",
                                    tint = colors.textSecondary
                                )
                            }
                        },
                        visualTransformation = if (passphraseVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.dock,
                            focusedLabelColor = colors.dock,
                            cursorColor = colors.dock
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // 4-Bar Strength Meter
                    val score = calculateStrengthScore(passphrase)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (i in 1..4) {
                            val active = score >= i
                            val barColor = when {
                                !active -> colors.outline.copy(alpha = 0.3f)
                                score == 1 -> colors.danger
                                score == 2 -> colors.attention
                                score == 3 -> Color(0xFF6366F1)
                                else -> colors.seal
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(5.dp)
                                    .clip(RoundedCornerShape(2.5.dp))
                                    .background(barColor)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = getStrengthLabel(score),
                            style = DockMono.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = when (score) {
                                    1 -> colors.danger
                                    2 -> colors.attention
                                    3 -> Color(0xFF6366F1)
                                    4 -> colors.seal
                                    else -> colors.textTertiary
                                }
                            )
                        )
                        Text("Min 8 characters", style = DockMono.copy(fontSize = 11.sp), color = colors.textTertiary)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = confirmPassphrase,
                        onValueChange = {
                            confirmPassphrase = it
                            passphraseError = null
                        },
                        label = { Text("Confirm Vault Passphrase") },
                        visualTransformation = if (passphraseVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
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
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "If you forget your passphrase, this 32-character key is the ONLY way to restore your backup.",
                        style = DockBodySmall,
                        color = colors.textSecondary
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .border(1.dp, colors.outline.copy(alpha = 0.5f), RoundedCornerShape(18.dp)),
                        colors = CardDefaults.cardColors(containerColor = colors.surface)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Outlined.Shield,
                                    contentDescription = null,
                                    tint = colors.dock,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "RECOVERY KEY (32 CHARACTERS)",
                                    style = DockMono.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                                    color = colors.textTertiary
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(colors.surfaceRaised)
                                    .padding(14.dp)
                            ) {
                                Text(
                                    text = formatChunkedKey(recoveryKey),
                                    style = DockMono.copy(
                                        fontSize = 16.sp,
                                        lineHeight = 24.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = colors.textPrimary
                                )
                            }
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
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(
                                imageVector = if (keyCopied) Icons.Outlined.Check else Icons.Outlined.ContentCopy,
                                contentDescription = null,
                                tint = if (keyCopied) colors.seal else colors.textPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                if (keyCopied) "Copied ✓" else "Copy",
                                style = DockBodySmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (keyCopied) colors.seal else colors.textPrimary
                                )
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                fileExportLauncher.launch("DockDrop-Recovery-Key-$setName.txt")
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(
                                imageVector = if (keySavedToFile) Icons.Outlined.Check else Icons.Outlined.Download,
                                contentDescription = null,
                                tint = if (keySavedToFile) colors.seal else colors.textPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                if (keySavedToFile) "Saved ✓" else "Save File",
                                style = DockBodySmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (keySavedToFile) colors.seal else colors.textPrimary
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    // Verification gate
                    Text("Verification check", style = DockBody.copy(fontWeight = FontWeight.Bold), color = colors.textPrimary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Enter the last 4 characters of your recovery key to confirm you saved it:",
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
                        textStyle = DockMono.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary),
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
                    Text("Encryption configured", style = DockHeadline, color = colors.textPrimary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Your backup will be cryptographically sealed. Passphrases are never sent over any network.",
                        style = DockBodySmall,
                        color = colors.textSecondary
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .border(1.dp, colors.seal.copy(alpha = 0.4f), RoundedCornerShape(18.dp)),
                        colors = CardDefaults.cardColors(containerColor = colors.seal.copy(alpha = 0.05f))
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Outlined.CheckCircle,
                                    contentDescription = null,
                                    tint = colors.seal,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("VAULT SPECIFICATIONS", style = DockMono.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold), color = colors.seal)
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("• Cipher: AES-256-GCM chunked streaming (DDC1)", style = DockBodySmall, color = colors.textPrimary)
                            Text("• Key derivation: Scrypt 32MB / N=32768 memory hardness", style = DockBodySmall, color = colors.textPrimary)
                            Text("• Filenames: Obfuscated with HMAC-SHA256", style = DockBodySmall, color = colors.textPrimary)
                            Text("• Offline unlock: Verified locally before any file is read", style = DockBodySmall, color = colors.textPrimary)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

private fun formatChunkedKey(key: String): String {
    return key.chunked(4).joinToString(" ")
}

private fun calculateStrengthScore(p: String): Int {
    if (p.isEmpty()) return 0
    if (p.length < 8) return 1
    var score = 1
    if (p.length >= 12) score++
    if (p.any { it.isUpperCase() } && p.any { it.isLowerCase() }) score++
    if (p.any { it.isDigit() } && p.any { !it.isLetterOrDigit() }) score++
    return score.coerceIn(1, 4)
}

private fun getStrengthLabel(score: Int): String {
    return when (score) {
        1 -> "Weak · Add more characters"
        2 -> "Fair · Use letters and numbers"
        3 -> "Good · Strong password"
        4 -> "Excellent · Highly secure"
        else -> "Enter passphrase"
    }
}
