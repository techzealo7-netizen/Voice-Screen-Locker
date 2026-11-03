package com.voicelocker.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.navigation.NavHostController
import kotlinx.coroutines.delay

@Composable
fun VoiceIntroScreen(nav: NavHostController) {
    val c = MaterialTheme.colorScheme
    val on = Store.voiceEnabled && Store.hasVoice
    var lang by remember { mutableStateOf(false) }
    val l = langByName(Store.language)
    Page("Voice Lock", onBack = { nav.popBackStack() }) {
        StepDots(if (on) 2 else 1)
        RowCard(pad = 10.dp) {
            Text("Active voice lock", Modifier.weight(1f).padding(start = 6.dp))
            Switch(on, { v -> if (v && !Store.hasVoice) nav.navigate("perms") else Store.voiceEnabled = v })
        }
        Box(Modifier.fillMaxWidth().padding(vertical = 28.dp), Alignment.Center) {
            Box(Modifier.size(170.dp).clip(CircleShape).background(c.primaryContainer), Alignment.Center) {
                Icon(Icons.Filled.PhoneAndroid, null, Modifier.size(110.dp), tint = c.primary)
                Icon(Icons.Filled.Mic, null, Modifier.size(34.dp), tint = Color.White)
            }
        }
        Text("Choose Your Voice language", Modifier.fillMaxWidth(), textAlign = TextAlign.Center, fontWeight = FontWeight.SemiBold)
        Muted("Select the right language for better voice recognition", 11, Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 14.dp), TextAlign.Center)
        RowCard(onClick = { lang = true }) {
            Spacer(Modifier.weight(1f)); Text(l.flag, fontSize = 22.sp); Spacer(Modifier.width(10.dp)); Text(l.name)
            Icon(Icons.Filled.KeyboardArrowDown, null); Spacer(Modifier.weight(1f))
        }
        AppButton("Continue", { if (Store.hasVoice) nav.popBackStack() else nav.navigate("perms") }, kind = Btn.White)
        Spacer(Modifier.height(16.dp))
    }
    if (lang) LanguageDialog(Store.language, { lang = false }) { Store.language = it; lang = false }
}

private fun has(ctx: android.content.Context, p: String) = ContextCompat.checkSelfPermission(ctx, p) == PackageManager.PERMISSION_GRANTED

@Composable
private fun PermRow(icon: ImageVector, title: String, sub: String, on: Boolean, onToggle: () -> Unit) = RowCard(pad = 12.dp) {
    Box(Modifier.size(36.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary), Alignment.Center) { Icon(icon, null, tint = Color.White, modifier = Modifier.size(19.dp)) }
    Spacer(Modifier.width(14.dp))
    Column(Modifier.weight(1f)) { Text(title, fontSize = 14.sp); Muted(sub, 10) }
    Switch(on, { onToggle() })
}

@Composable
fun PermissionsScreen(nav: NavHostController) {
    val ctx = LocalContext.current
    var mic by remember { mutableStateOf(has(ctx, Manifest.permission.RECORD_AUDIO)) }
    var phone by remember { mutableStateOf(has(ctx, Manifest.permission.READ_PHONE_STATE)) }
    var overlay by remember { mutableStateOf(Settings.canDrawOverlays(ctx)) }
    fun refresh() { mic = has(ctx, Manifest.permission.RECORD_AUDIO); phone = has(ctx, Manifest.permission.READ_PHONE_STATE); overlay = Settings.canDrawOverlays(ctx) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { refresh() }
    val micL = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { refresh() }
    val phoneL = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { refresh() }
    fun appSettings() = ctx.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${ctx.packageName}")))

    Page("Permission Required", onBack = { nav.popBackStack() }, bottom = {
        AppButton("Done", { if (!mic) ctx.toast("Microphone access is required to create a voice lock.") else nav.go("voice", "perms") })
    }) {
        Column(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(76.dp).clip(CircleShape).background(Blue), Alignment.Center) { Icon(Icons.Filled.Lock, null, tint = Color.White, modifier = Modifier.size(38.dp)) }
            Spacer(Modifier.height(14.dp)); Text("Additional permission required", fontWeight = FontWeight.SemiBold)
            Muted("Our app needs access to these permissions to work correctly.", 11, align = TextAlign.Center)
        }
        PermRow(Icons.Filled.Mic, "Microphone Access", "To record your voice and create voice lock", mic) {
            if (mic) appSettings() else micL.launch(buildList { add(Manifest.permission.RECORD_AUDIO); if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS) }.toTypedArray())
        }
        PermRow(Icons.Filled.Call, "Phone Call Access", "To detect your voice and unlock the screen", phone) {
            if (phone) appSettings() else phoneL.launch(Manifest.permission.READ_PHONE_STATE)
        }
        PermRow(Icons.Filled.Layers, "Display Over Other Apps", "To show the lock screen over other apps", overlay) {
            ctx.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${ctx.packageName}")))
        }
    }
}

@Composable
fun RecordScreen(nav: NavHostController) {
    val ctx = LocalContext.current
    val c = MaterialTheme.colorScheme
    val speech = rememberSpeech()
    var phase by remember { mutableIntStateOf(0) } // 0 idle, 1 listening, 2 done
    var text by remember { mutableStateOf("") }
    var left by remember { mutableIntStateOf(5) }
    LaunchedEffect(phase) { if (phase == 1) { left = 5; while (left > 0) { delay(1000); left-- }; speech.finish() } }

    fun record() {
        text = ""; phase = 1
        speech.start(langByName(Store.language).tag, onPartial = { text = it },
            onFinal = { if (it.isNotBlank()) text = it; phase = if (text.isBlank()) 0 else 2; if (text.isBlank()) ctx.toast("We could not hear you. Please try again.") },
            onError = { phase = if (text.isBlank()) 0 else 2; ctx.toast("Could not recognise speech. Please try again.") })
    }

    Page("Voice Lock", onBack = { nav.popBackStack() }, bottom = if (phase == 2) ({
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AppButton("Continue", { Store.setVoicePhrase(text); nav.go("voice", "record") }, Modifier.weight(3f))
            AppButton("Record Again", { text = ""; phase = 0 }, Modifier.weight(2f), kind = Btn.White)
        }
    }) else null) {
        StepDots(1)
        Text(if (phase == 2) "Your Voice Key" else "Create Your Voice Lock", Modifier.fillMaxWidth().padding(vertical = 8.dp),
            textAlign = TextAlign.Center, color = if (phase == 2) c.primary else c.onSurface, fontWeight = FontWeight.Medium)
        ColCard(center = true, pad = 12.dp) {
            Box(Modifier.fillMaxWidth().heightIn(min = if (phase == 2) 220.dp else 120.dp).clip(RoundedCornerShape(12.dp)).background(c.surfaceVariant).padding(10.dp)) {
                Text(text.ifEmpty { "Recorded text will appear here" }, fontSize = 13.sp, color = if (text.isEmpty()) c.onSurfaceVariant else c.onSurface)
            }
            if (phase != 2) {
                Spacer(Modifier.height(22.dp))
                MicButton(56.dp, c.primaryContainer, c.primary, onClick = if (phase == 0) ({ record() }) else null, icon = if (phase == 1) Icons.Filled.GraphicEq else Icons.Filled.Mic)
                Spacer(Modifier.height(10.dp))
                Text(if (phase == 1) "Listening… ${left}s" else "Tap to Record", fontWeight = FontWeight.SemiBold)
                Muted("Record your password within 5 seconds.", 10)
                Spacer(Modifier.height(6.dp))
            }
        }
    }
}
