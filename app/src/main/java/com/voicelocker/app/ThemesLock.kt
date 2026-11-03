package com.voicelocker.app

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.navigation.NavHostController
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ThemesScreen(nav: NavHostController) = Page("Themes", onBack = { nav.popBackStack() }) {
    Spacer(Modifier.height(8.dp))
    ThemeGrid(lockThemes.indices.toList()) { nav.navigate("themePreview/$it") }
}

@Composable
fun ThemePreviewScreen(nav: NavHostController, i: Int) {
    val ctx = LocalContext.current
    Page("Themes", onBack = { nav.popBackStack() }, scroll = false, bottom = {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AppButton("Preview", { nav.navigate("lockPreview/$i") }, Modifier.weight(1f), kind = Btn.Outline, icon = Icons.Filled.Visibility)
            AppButton("Apply", { Store.themeIndex = i; ctx.toast("${lockThemes[i].name} applied") }, Modifier.weight(1f))
        }
    }) {
        ThemeBg(i, Modifier.weight(1f).fillMaxWidth().padding(bottom = 4.dp), RoundedCornerShape(16.dp))
    }
}

private enum class LockMode { Voice, Pin, Pattern }

/**
 * The lock screen. Used by LockActivity (real), by "Test lock screen", and with preview=true by the Themes flow
 * (nothing is verified and an Apply button is shown).
 */
@Composable
fun LockScreen(themeIndex: Int, preview: Boolean = false, onUnlock: () -> Unit = {}, onApply: (() -> Unit)? = null, onBack: (() -> Unit)? = null) {
    val ctx = LocalContext.current
    val act = ctx.findActivity()
    val speech = rememberSpeech()
    val hasVoice = preview || (Store.voiceEnabled && Store.hasVoice)
    var mode by remember { mutableStateOf(if (hasVoice) LockMode.Voice else if (Store.hasPin) LockMode.Pin else if (Store.hasPattern) LockMode.Pattern else LockMode.Voice) }
    val now by produceState(Date()) { while (true) { value = Date(); delay(1000) } }
    var msg by remember { mutableStateOf("Private Voice Diary, Only yours") }
    var pin by remember { mutableStateOf("") }
    var err by remember { mutableStateOf(false) }
    var forgot by remember { mutableStateOf(false) }
    val compact = LocalConfiguration.current.screenHeightDp < 650

    fun listen() {
        if (preview) return
        msg = "Listening… say your voice password"
        speech.start(langByName(Store.language).tag,
            onPartial = { if (Store.checkVoice(it)) { speech.cancel(); onUnlock() } },
            onFinal = { if (Store.checkVoice(it)) onUnlock() else msg = "Voice not recognized. Tap the mic to try again." },
            onError = { msg = "Could not hear you. Tap the mic to try again." })
    }
    LaunchedEffect(mode) {
        if (!preview && mode == LockMode.Voice && hasVoice &&
            androidx.core.content.ContextCompat.checkSelfPermission(ctx, android.Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED) listen()
    }
    LaunchedEffect(pin) {
        if (pin.length == 4) {
            if (preview) { delay(300); pin = "" }
            else if (Store.checkPin(pin)) onUnlock()
            else { err = true; delay(600); pin = ""; err = false }
        }
    }

    ThemeBg(themeIndex, Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().background(Color.Black.copy(.3f)))
        Column(Modifier.fillMaxSize().systemBarsPadding().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                if (onBack != null) CircleBtn(Icons.Filled.ChevronLeft, onBack) else Spacer(Modifier.size(34.dp))
                Spacer(Modifier.weight(1f)); CircleBtn(Icons.Filled.Help) {}
            }
            val big = if (compact) 64.sp else 88.sp
            Text(SimpleDateFormat("HH", Locale.getDefault()).format(now), color = Color.White, fontSize = big, fontWeight = FontWeight.Bold, lineHeight = big * .92f)
            Text(SimpleDateFormat("mm", Locale.getDefault()).format(now), color = Color.White, fontSize = big, fontWeight = FontWeight.Bold, lineHeight = big * .92f)
            Text(SimpleDateFormat("EEEE, MMM d", Locale.getDefault()).format(now), color = Color.White, fontSize = 13.sp)
            Box(Modifier.weight(1f).fillMaxWidth(), Alignment.Center) {
                Column(Modifier.verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
                    when (mode) {
                        LockMode.Voice -> {
                            MicButton(if (speech.listening) 66.dp else 54.dp, Color.White.copy(.18f), if (speech.listening) Blue else Color.White.copy(.3f), onClick = { listen() })
                            Spacer(Modifier.height(12.dp)); Text(msg, color = Color.White, fontSize = 11.sp, textAlign = TextAlign.Center)
                            Spacer(Modifier.height(22.dp))
                            Row(Modifier.clip(RoundedCornerShape(16.dp)).background(Color.White.copy(.25f)).padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (preview || Store.hasPin) GlassIcon(Icons.Filled.Dialpad) { mode = LockMode.Pin }
                                if (preview || Store.fingerprintEnabled) GlassIcon(Icons.Filled.Fingerprint) { if (!preview) biometricAuth(act, "Unlock") { if (it) onUnlock() } }
                                if (preview || Store.hasPattern) GlassIcon(Icons.Filled.Apps) { mode = LockMode.Pattern }
                            }
                        }
                        LockMode.Pin -> {
                            Text(if (err) "Wrong PIN" else "Enter your pin", color = if (err) Color(0xFFFF8A8A) else Color.White, fontSize = 12.sp)
                            PinDots(pin.length, err, glass = true)
                            NumPad({ if (pin.length < 4) pin += it }, { pin = pin.dropLast(1) }, glass = true)
                        }
                        LockMode.Pattern -> {
                            Text(if (err) "Wrong pattern" else "Enter your pattern", color = if (err) Color(0xFFFF8A8A) else Color.White, fontSize = 12.sp)
                            Spacer(Modifier.height(14.dp))
                            PatternPad(glass = true, error = err, onComplete = { p ->
                                if (!preview && p.size >= 4) { if (Store.checkPattern(p)) onUnlock() else err = true }
                            })
                            LaunchedEffect(err) { if (err) { delay(800); err = false } }
                        }
                    }
                }
            }
            if (mode != LockMode.Voice && hasVoice) TextButton({ mode = LockMode.Voice; pin = "" }) {
                Icon(Icons.Filled.Mic, null, tint = Color.White, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Use voice lock", color = Color.White)
            }
            if (!preview && Store.hasSecurity) TextButton({ forgot = true }) { Text("Forgot password?", color = Color.White.copy(.85f), fontSize = 12.sp) }
            if (preview && onApply != null) AppButton("Apply", onApply)
        }
    }
    if (forgot) {
        var ans by remember { mutableStateOf("") }
        AlertDialog(onDismissRequest = { forgot = false }, title = { Text("Security question", fontSize = 16.sp) },
            text = { Column { Text(Store.securityQuestion ?: ""); Spacer(Modifier.height(8.dp)); OutlinedTextField(ans, { ans = it }, singleLine = true, placeholder = { Text("Your answer") }) } },
            confirmButton = { TextButton({ if (Store.checkAnswer(ans)) { forgot = false; onUnlock() } else ctx.toast("Incorrect answer") }) { Text("Unlock") } },
            dismissButton = { TextButton({ forgot = false }) { Text("Cancel") } })
    }
}

@Composable
private fun CircleBtn(icon: ImageVector, onClick: () -> Unit) = Box(
    Modifier.size(34.dp).clip(CircleShape).background(Color.Black.copy(.35f)).clickable(onClick = onClick), Alignment.Center) {
    Icon(icon, null, tint = Color.White, modifier = Modifier.size(20.dp))
}

@Composable
private fun GlassIcon(icon: ImageVector, onClick: () -> Unit) = IconButton(onClick, Modifier.size(52.dp)) { Icon(icon, null, tint = Color.White, modifier = Modifier.size(28.dp)) }
