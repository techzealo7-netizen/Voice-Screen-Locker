package com.voicelocker.app.ui.screens.voice

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.navigation.NavHostController
import com.voicelocker.app.ui.components.AppButton
import com.voicelocker.app.ui.components.Muted
import com.voicelocker.app.ui.components.Page
import com.voicelocker.app.ui.components.RowCard
import com.voicelocker.app.ui.navigation.go
import com.voicelocker.app.ui.theme.Blue
import com.voicelocker.app.util.toast

private fun has(ctx: Context, p: String) = ContextCompat.checkSelfPermission(ctx, p) == PackageManager.PERMISSION_GRANTED

@Composable
private fun PermRow(icon: ImageVector, title: String, sub: String, on: Boolean, onToggle: () -> Unit) = RowCard(pad = 12.dp) {
    Box(Modifier.size(36.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary), Alignment.Center) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(19.dp))
    }
    Spacer(Modifier.width(14.dp))
    Column(Modifier.weight(1f)) {
        Text(title, fontSize = 14.sp)
        Muted(sub, 10)
    }
    Switch(on, { onToggle() })
}

@Composable
fun PermissionsScreen(nav: NavHostController) {
    val ctx = LocalContext.current
    var mic by remember { mutableStateOf(has(ctx, Manifest.permission.RECORD_AUDIO)) }
    var phone by remember { mutableStateOf(has(ctx, Manifest.permission.READ_PHONE_STATE)) }
    var overlay by remember { mutableStateOf(Settings.canDrawOverlays(ctx)) }
    fun refresh() {
        mic = has(ctx, Manifest.permission.RECORD_AUDIO)
        phone = has(ctx, Manifest.permission.READ_PHONE_STATE)
        overlay = Settings.canDrawOverlays(ctx)
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { refresh() }
    val micL = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { refresh() }
    val phoneL = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { refresh() }
    fun appSettings() = ctx.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${ctx.packageName}")))

    Page("Permission Required", onBack = { nav.popBackStack() }, bottom = {
        AppButton("Done", {
            if (!mic) ctx.toast("Microphone access is required to create a voice lock.")
            else nav.go("voice", "perms")
        })
    }) {
        Column(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(76.dp).clip(CircleShape).background(Blue), Alignment.Center) {
                Icon(Icons.Filled.Lock, null, tint = Color.White, modifier = Modifier.size(38.dp))
            }
            Spacer(Modifier.height(14.dp))
            Text("Additional permission required", fontWeight = FontWeight.SemiBold)
            Muted("Our app needs access to these permissions to work correctly.", 11, align = TextAlign.Center)
        }
        PermRow(Icons.Filled.Mic, "Microphone Access", "To record your voice and create voice lock", mic) {
            if (mic) appSettings()
            else micL.launch(buildList {
                add(Manifest.permission.RECORD_AUDIO)
                if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
            }.toTypedArray())
        }
        PermRow(Icons.Filled.Call, "Phone Call Access", "To detect your voice and unlock the screen", phone) {
            if (phone) appSettings() else phoneL.launch(Manifest.permission.READ_PHONE_STATE)
        }
        PermRow(Icons.Filled.Layers, "Display Over Other Apps", "To show the lock screen over other apps", overlay) {
            ctx.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${ctx.packageName}")))
        }
    }
}

