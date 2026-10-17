package `in`.soumyaranjansahoo.dockdrop.ui.screens.crypto

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.PersistableBundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.heightIn
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
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockLabel
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockMono
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockTheme
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockTitle
import `in`.soumyaranjansahoo.dockdrop.ui.theme.RadiusMd
import `in`.soumyaranjansahoo.dockdrop.ui.theme.RadiusSm
import java.util.Locale

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
            } catch (_: Exception) {
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
                    TextButton(
                        onClick = onCancel,
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Text(
                            text = "Cancel",
                            color = colors.textSecondary,
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
                    .background(colors.bg)
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
                        .heightIn(min = 56.dp),
                    shape = RoundedCornerShape(RadiusMd),
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
                        style = DockBody.copy(fontWeight = FontWeight.Bold)
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
                        targetValue = if (isFinished) colors.dock else colors.outline.copy(alpha = 0.45f),
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
            Spacer(modifier = Modifier.height(24.dp))

            when (currentStep) {
                1 -> {
                    // (a) Passphrase
                    Text(
                        text = "Protect with a passphrase",
                        style = DockHeadline,
                        color = colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Your backup will be encrypted with AES-256-GCM. Because keys never leave your device, we cannot recover your passphrase if lost.",
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
                        label = { Text("Vault passphrase", style = DockBodySmall) },
                        trailingIcon = {
                            IconButton(
                                onClick = { passphraseVisible = !passphraseVisible },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = if (passphraseVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                    contentDescription = if (passphraseVisible) "Hide passphrase" else "Show passphrase",
                                    tint = colors.textSecondary
                                )
                            }
                        },
                        visualTransformation = if (passphraseVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(RadiusSm),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.dock,
                            unfocusedBorderColor = colors.outline,
                            focusedLabelColor = colors.dock,
                            unfocusedLabelColor = colors.textSecondary,
                            cursorColor = colors.dock
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // 4-Bar Strength Meter using semantic strength tokens
                    val score = calculateStrengthScore(passphrase)
                    val activeStrengthColor = when (score) {
                        1 -> colors.strengthWeak
                        2 -> colors.strengthMedium
                        3 -> colors.dock
                        4 -> colors.strengthStrong
                        else -> colors.textTertiary
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (i in 1..4) {
                            val active = score >= i
                            val barColor = if (active) activeStrengthColor else colors.outline.copy(alpha = 0.35f)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(5.dp)
                                    .clip(RoundedCornerShape(2.5.dp))
                                    .background(barColor)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = getStrengthLabel(score),
                            style = DockMono.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = activeStrengthColor
                            )
                        )
                        Text(
                            text = "Min 8 chars",
                            style = DockMono.copy(fontSize = 11.sp),
                            color = colors.textTertiary
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = confirmPassphrase,
                        onValueChange = {
                            confirmPassphrase = it
                            passphraseError = null
                        },
                        label = { Text("Confirm vault passphrase", style = DockBodySmall) },
                        visualTransformation = if (passphraseVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(RadiusSm),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.dock,
                            unfocusedBorderColor = colors.outline,
                            focusedLabelColor = colors.dock,
                            unfocusedLabelColor = colors.textSecondary,
                            cursorColor = colors.dock
                        )
                    )

                    if (passphraseError != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = passphraseError!!,
                            style = DockBodySmall.copy(fontWeight = FontWeight.Medium),
                            color = colors.danger
                        )
                    }
                }

                2 -> {
                    // (b) Recovery Key
                    Text(
                        text = "Save your recovery key",
                        style = DockHeadline,
                        color = colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "If you forget your passphrase, this 32-character key is the only way to unlock your backup.",
                        style = DockBodySmall,
                        color = colors.textSecondary
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(RadiusMd))
                            .border(1.dp, colors.outline, RoundedCornerShape(RadiusMd)),
                        colors = CardDefaults.cardColors(containerColor = colors.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
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
                                    text = "RECOVERY KEY · 32 CHARACTERS",
                                    style = DockLabel,
                                    color = colors.textTertiary
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(RadiusSm))
                                    .background(colors.surfaceRaised)
                                    .border(1.dp, colors.outline.copy(alpha = 0.6f), RoundedCornerShape(RadiusSm))
                                    .padding(16.dp)
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
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp),
                            shape = RoundedCornerShape(RadiusSm),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (keyCopied) colors.dock else colors.outline
                            )
                        ) {
                            Icon(
                                imageVector = if (keyCopied) Icons.Outlined.Check else Icons.Outlined.ContentCopy,
                                contentDescription = null,
                                tint = if (keyCopied) colors.dock else colors.textPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (keyCopied) "Copied" else "Copy",
                                style = DockBodySmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (keyCopied) colors.dock else colors.textPrimary
                                )
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                fileExportLauncher.launch("DockDrop-Recovery-Key-$setName.txt")
                            },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp),
                            shape = RoundedCornerShape(RadiusSm),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (keySavedToFile) colors.dock else colors.outline
                            )
                        ) {
                            Icon(
                                imageVector = if (keySavedToFile) Icons.Outlined.Check else Icons.Outlined.Download,
                                contentDescription = null,
                                tint = if (keySavedToFile) colors.dock else colors.textPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (keySavedToFile) "Saved" else "Save File",
                                style = DockBodySmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (keySavedToFile) colors.dock else colors.textPrimary
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    // Verification gate
                    Text(
                        text = "Confirm recovery key",
                        style = DockTitle.copy(fontSize = 17.sp),
                        color = colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Enter the last 4 characters of your recovery key to verify you saved it:",
                        style = DockBodySmall,
                        color = colors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = userLastFour,
                        onValueChange = {
                            if (it.length <= 4) {
                                userLastFour = it.uppercase(Locale.US)
                                gateError = null
                            }
                        },
                        placeholder = {
                            Text(
                                text = "Last 4 characters (e.g. ${recoveryKey.takeLast(4)})",
                                style = DockMono.copy(color = colors.textTertiary)
                            )
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(RadiusSm),
                        textStyle = DockMono.copy(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.dock,
                            unfocusedBorderColor = colors.outline,
                            focusedLabelColor = colors.dock,
                            unfocusedLabelColor = colors.textSecondary,
                            cursorColor = colors.dock
                        )
                    )

                    if (gateError != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = gateError!!,
                            style = DockBodySmall.copy(fontWeight = FontWeight.Medium),
                            color = colors.danger
                        )
                    }
                }

                3 -> {
                    // (c) Confirmation (Uses colors.dock rather than premature seal green per Rule 2)
                    Text(
                        text = "Encryption ready",
                        style = DockHeadline,
                        color = colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Your Set will be encrypted before bytes touch your SSD. Keys never leave your phone.",
                        style = DockBodySmall,
                        color = colors.textSecondary
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(RadiusMd))
                            .border(1.dp, colors.dock.copy(alpha = 0.45f), RoundedCornerShape(RadiusMd)),
                        colors = CardDefaults.cardColors(containerColor = colors.dock.copy(alpha = 0.08f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Outlined.CheckCircle,
                                    contentDescription = null,
                                    tint = colors.dock,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "VAULT SPECIFICATIONS",
                                    style = DockLabel,
                                    color = colors.dock
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "• Cipher: AES-256-GCM chunked streaming (DDC1)",
                                style = DockBodySmall,
                                color = colors.textPrimary
                            )
                            Text(
                                text = "• Key derivation: Scrypt 32 MB (N=32768) memory hardness",
                                style = DockBodySmall,
                                color = colors.textPrimary
                            )
                            Text(
                                text = "• Filenames: Obfuscated with HMAC-SHA256",
                                style = DockBodySmall,
                                color = colors.textPrimary
                            )
                            Text(
                                text = "• Offline unlock: Verified locally before any file is read",
                                style = DockBodySmall,
                                color = colors.textPrimary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
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
        2 -> "Fair · Mix letters and numbers"
        3 -> "Good · Strong passphrase"
        4 -> "Excellent · High entropy"
        else -> "Enter passphrase"
    }
}
