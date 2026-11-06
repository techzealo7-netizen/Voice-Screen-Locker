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
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.navigation.NavHostController
import com.voicelocker.ads.AppOpenAdManager
import com.voicelocker.app.R
import com.voicelocker.app.ui.components.AppButton
import com.voicelocker.app.ui.components.Muted
import com.voicelocker.app.ui.components.Page
import com.voicelocker.app.ui.components.RowCard
import com.voicelocker.app.ui.components.SleekSwitch
import com.voicelocker.app.ui.navigation.go
import com.voicelocker.app.ui.theme.InterFontFamily
import com.voicelocker.app.util.toast

private fun has(ctx: Context, p: String) = ContextCompat.checkSelfPermission(ctx, p) == PackageManager.PERMISSION_GRANTED

@Composable
private fun PermRow(
    iconRes: Int,
    title: String,
    sub: String,
    on: Boolean,
    onToggle: () -> Unit
) = RowCard(pad = 12.dp, onClick = onToggle) {
    Image(
        painter = painterResource(iconRes),
        contentDescription = null,
        modifier = Modifier.size(40.dp),
        contentScale = ContentScale.Fit
    )
    Spacer(Modifier.width(14.dp))
    Column(Modifier.weight(1f)) {
        Text(
            text = title,
            fontSize = 15.sp,
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF1E2022)
        )
        Spacer(Modifier.height(2.dp))
        Muted(sub, 11)
    }
    SleekSwitch(
        checked = on,
        onCheckedChange = { onToggle() }
    )
}

@Composable
fun PermissionsScreen(nav: NavHostController) {
    val ctx = LocalContext.current
    var mic by remember { mutableStateOf(has(ctx, Manifest.permission.RECORD_AUDIO)) }
    var phone by remember { mutableStateOf(has(ctx, Manifest.permission.READ_PHONE_STATE)) }
    var overlay by remember { mutableStateOf(Settings.canDrawOverlays(ctx)) }

    DisposableEffect(Unit) {
        AppOpenAdManager.setPermissionRequestActive(true)
        onDispose {
            AppOpenAdManager.setPermissionRequestActive(false)
            AppOpenAdManager.suppressAppOpenFor(5000L)
        }
    }

    fun refresh() {
        mic = has(ctx, Manifest.permission.RECORD_AUDIO)
        phone = has(ctx, Manifest.permission.READ_PHONE_STATE)
        overlay = Settings.canDrawOverlays(ctx)
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        AppOpenAdManager.suppressAppOpenFor(5000L)
        refresh()
    }
    val micL = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        AppOpenAdManager.suppressAppOpenFor(5000L)
        refresh()
    }
    val phoneL = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        AppOpenAdManager.suppressAppOpenFor(5000L)
        refresh()
    }
    fun appSettings() {
        AppOpenAdManager.suppressAppOpenFor(60_000L)
        ctx.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${ctx.packageName}")))
    }

    Page(
        title = "Permission Required",
        onBack = { nav.popBackStack() },
        adScreenKey = "voice_lock_screen",
        nativeTemplate = 1,
        bottom = {
            AppButton(
                text = "Done",
                onClick = {
                    if (!mic) ctx.toast("Microphone access is required to create a voice lock.")
                    else nav.go("voice", "perms")
                },
                height = 56.dp
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(R.drawable.ic_lock2),
                contentDescription = "Permission Required",
                modifier = Modifier.size(80.dp),
                contentScale = ContentScale.Fit
            )
            Spacer(Modifier.height(14.dp))
            Text(
                text = "Additional permission required",
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp,
                color = Color(0xFF1E2022)
            )
            Spacer(Modifier.height(4.dp))
            Muted(
                text = "Our app needs access to these permissions to work correctly.",
                size = 12,
                align = TextAlign.Center
            )
        }

        PermRow(
            iconRes = R.drawable.ic_mic,
            title = "Microphone Access",
            sub = "To record your voice and create voice lock",
            on = mic
        ) {
            AppOpenAdManager.suppressAppOpenFor(60_000L)
            if (mic) appSettings()
            else micL.launch(buildList {
                add(Manifest.permission.RECORD_AUDIO)
                if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
            }.toTypedArray())
        }

        PermRow(
            iconRes = R.drawable.ic_call,
            title = "Phone Call Access",
            sub = "To detect your voice and unlock the screen",
            on = phone
        ) {
            AppOpenAdManager.suppressAppOpenFor(60_000L)
            if (phone) appSettings() else phoneL.launch(Manifest.permission.READ_PHONE_STATE)
        }

        PermRow(
            iconRes = R.drawable.ic_overlay,
            title = "Display over other apps",
            sub = "To display lock screen when you turn on the screen",
            on = overlay
        ) {
            AppOpenAdManager.suppressAppOpenFor(60_000L)
            if (overlay) appSettings()
            else ctx.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${ctx.packageName}")))
        }

        Spacer(Modifier.height(16.dp))
    }
}
