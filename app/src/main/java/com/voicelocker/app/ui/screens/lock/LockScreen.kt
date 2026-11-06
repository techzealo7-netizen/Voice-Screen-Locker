package com.voicelocker.app.ui.screens.lock

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.voicelocker.app.R
import com.voicelocker.app.data.local.Store
import com.voicelocker.app.data.model.langByName
import com.voicelocker.app.ui.components.NumPad
import com.voicelocker.app.ui.components.PatternPad
import com.voicelocker.app.ui.components.PinDots
import com.voicelocker.app.ui.components.ThemeBg
import com.voicelocker.app.ui.theme.Blue
import com.voicelocker.app.ui.theme.InterFontFamily
import com.voicelocker.app.util.biometricAuth
import com.voicelocker.app.util.findActivity
import com.voicelocker.app.util.rememberSpeech
import com.voicelocker.app.util.toast
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class LockMode { Voice, Pin, Pattern }

/**
 * The lock screen. Used by LockActivity (real), by "Test lock screen", and with preview=true by the Themes flow
 * (matches the design mockup with white home icons inside a frosted glass bar and an Apply button).
 */
@Composable
fun LockScreen(
    themeIndex: Int,
    preview: Boolean = false,
    onUnlock: () -> Unit = {},
    onApply: (() -> Unit)? = null,
    onBack: (() -> Unit)? = null
) {
    val ctx = LocalContext.current
    val act = ctx.findActivity()
    val speech = rememberSpeech()
    val hasVoice = preview || (Store.voiceEnabled && Store.hasVoice)
    var mode by remember {
        mutableStateOf(
            if (hasVoice) LockMode.Voice
            else if (Store.hasPin) LockMode.Pin
            else if (Store.hasPattern) LockMode.Pattern
            else LockMode.Voice
        )
    }
    val now by produceState(Date()) {
        while (true) {
            value = Date()
            delay(1000)
        }
    }
    var msg by remember { mutableStateOf("Private Voice Diary, Only yours") }
    var pin by remember { mutableStateOf("") }
    var err by remember { mutableStateOf(false) }
    var forgot by remember { mutableStateOf(false) }
    val compact = LocalConfiguration.current.screenHeightDp < 680

    fun listen() {
        if (preview) return
        msg = "Listening… say your voice password"
        speech.start(
            langByName(Store.language).tag,
            onPartial = {
                if (Store.checkVoice(it)) {
                    speech.cancel()
                    onUnlock()
                }
            },
            onFinal = {
                if (Store.checkVoice(it)) onUnlock()
                else msg = "Voice not recognized. Tap the mic to try again."
            },
            onError = { msg = "Could not hear you. Tap the mic to try again." }
        )
    }

    LaunchedEffect(mode) {
        if (!preview && mode == LockMode.Voice && hasVoice &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        ) {
            listen()
        }
    }

    LaunchedEffect(pin) {
        if (pin.length == 4) {
            if (preview) {
                delay(300)
                pin = ""
            } else if (Store.checkPin(pin)) {
                onUnlock()
            } else {
                err = true
                delay(600)
                pin = ""
                err = false
            }
        }
    }

    ThemeBg(themeIndex, Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().background(Color.Black.copy(0.25f)))
        Column(
            Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Bar: Back & Help
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onBack != null) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            Icons.Filled.ChevronLeft,
                            contentDescription = "Back",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                } else {
                    Spacer(Modifier.size(38.dp))
                }
                Spacer(Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Help,
                        contentDescription = "Help",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Clock Area (Tall bold condensed numbers + date)
            val big = if (compact) 60.sp else 74.sp
            Text(
                text = SimpleDateFormat("HH", Locale.getDefault()).format(now),
                color = Color.White,
                fontSize = big,
                fontWeight = FontWeight.Bold,
                fontFamily = InterFontFamily,
                lineHeight = big * 0.9f
            )
            Text(
                text = SimpleDateFormat("mm", Locale.getDefault()).format(now),
                color = Color.White,
                fontSize = big,
                fontWeight = FontWeight.Bold,
                fontFamily = InterFontFamily,
                lineHeight = big * 0.9f
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = SimpleDateFormat("EEEE, MMM d", Locale.getDefault()).format(now),
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 14.sp,
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Normal
            )

            // Middle Content: Mic & Shortcuts, or PIN / Pattern pad
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    Modifier.verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    when (mode) {
                        LockMode.Voice -> {
                            // Circular Frosted Mic
                            Box(
                                modifier = Modifier
                                    .size(if (compact) 82.dp else 96.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.28f))
                                    .clickable { listen() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Mic,
                                    contentDescription = "Microphone",
                                    tint = Color.White,
                                    modifier = Modifier.size(if (compact) 40.dp else 48.dp)
                                )
                            }

                            Spacer(Modifier.height(14.dp))
                            Text(
                                text = if (speech.listening) msg else "Private Voice Diary, Only yours",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontFamily = InterFontFamily,
                                textAlign = TextAlign.Center
                            )

                            Spacer(Modifier.height(28.dp))

                            // Frosted glass card with the home screen icons in solid white
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Color.White.copy(alpha = 0.18f))
                                    .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
                                    .padding(horizontal = 24.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(28.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Pin Lock Icon (White)
                                IconButton(
                                    onClick = { mode = LockMode.Pin },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_lock),
                                        contentDescription = "Pin Lock",
                                        tint = Color.White,
                                        modifier = Modifier.size(34.dp)
                                    )
                                }

                                // Fingerprint Lock Icon (White)
                                IconButton(
                                    onClick = {
                                        if (!preview) biometricAuth(act, "Unlock") { if (it) onUnlock() }
                                    },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_fingerprint),
                                        contentDescription = "Fingerprint Lock",
                                        tint = Color.White,
                                        modifier = Modifier.size(34.dp)
                                    )
                                }

                                // Pattern Lock Icon (White)
                                IconButton(
                                    onClick = { mode = LockMode.Pattern },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_pattern),
                                        contentDescription = "Pattern Lock",
                                        tint = Color.White,
                                        modifier = Modifier.size(34.dp)
                                    )
                                }
                            }
                        }

                        LockMode.Pin -> {
                            Text(
                                if (err) "Wrong PIN" else "Enter your pin",
                                color = if (err) Color(0xFFFF8A8A) else Color.White,
                                fontSize = 13.sp,
                                fontFamily = InterFontFamily
                            )
                            PinDots(pin.length, err, glass = true)
                            NumPad({ if (pin.length < 4) pin += it }, { pin = pin.dropLast(1) }, glass = true)
                        }

                        LockMode.Pattern -> {
                            Text(
                                if (err) "Wrong pattern" else "Enter your pattern",
                                color = if (err) Color(0xFFFF8A8A) else Color.White,
                                fontSize = 13.sp,
                                fontFamily = InterFontFamily
                            )
                            Spacer(Modifier.height(14.dp))
                            PatternPad(glass = true, error = err, onComplete = { p ->
                                if (!preview && p.size >= 4) {
                                    if (Store.checkPattern(p)) onUnlock() else err = true
                                }
                            })
                            LaunchedEffect(err) {
                                if (err) {
                                    delay(800)
                                    err = false
                                }
                            }
                        }
                    }
                }
            }

            // Return to voice mode if currently in Pin / Pattern
            if (mode != LockMode.Voice && hasVoice) {
                TextButton(onClick = { mode = LockMode.Voice; pin = "" }) {
                    Icon(Icons.Filled.Mic, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Use voice lock", color = Color.White, fontFamily = InterFontFamily)
                }
            }

            // Forgot password
            if (!preview && Store.hasSecurity) {
                TextButton(onClick = { forgot = true }) {
                    Text("Forgot password?", color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp, fontFamily = InterFontFamily)
                }
            }

            // Preview Mode Apply Button (Matches Screenshot 3)
            if (preview && onApply != null) {
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = onApply,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Blue,
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = "Apply",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = InterFontFamily,
                        color = Color.White
                    )
                }
            }
        }
    }

    // Security question recovery dialog
    if (forgot) {
        var ans by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { forgot = false },
            title = { Text("Security question", fontSize = 16.sp, fontFamily = InterFontFamily) },
            text = {
                Column {
                    Text(Store.securityQuestion ?: "", fontFamily = InterFontFamily)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        ans, { ans = it }, singleLine = true,
                        placeholder = { Text("Your answer", fontFamily = InterFontFamily) }
                    )
                }
            },
            confirmButton = {
                TextButton({
                    if (Store.checkAnswer(ans)) {
                        forgot = false
                        onUnlock()
                    } else {
                        ctx.toast("Incorrect answer")
                    }
                }) {
                    Text("Unlock", fontFamily = InterFontFamily)
                }
            },
            dismissButton = {
                TextButton({ forgot = false }) {
                    Text("Cancel", fontFamily = InterFontFamily)
                }
            }
        )
    }
}
