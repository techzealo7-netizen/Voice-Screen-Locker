package com.voicelocker.app.ui.screens.home

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import com.voicelocker.app.ui.components.MaxContentWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavHostController
import com.voicelocker.ads.AppAdaptiveBannerTop
import com.voicelocker.ads.AppNativeAdView
import com.voicelocker.ads.AppOpenAdManager
import com.voicelocker.app.R
import com.voicelocker.app.data.local.Store
import com.voicelocker.app.ui.components.LanguageDialog
import com.voicelocker.app.ui.components.SleekSwitch
import com.voicelocker.app.ui.components.TabColumn
import com.voicelocker.app.ui.theme.InterFontFamily
import com.voicelocker.app.util.toast

@Composable
private fun SettingItem(
    icon: ImageVector,
    title: String,
    sub: String,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    val c = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = c.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.SemiBold,
                color = c.onSurface
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = sub,
                fontSize = 11.sp,
                fontFamily = InterFontFamily,
                color = c.onSurfaceVariant
            )
        }
        if (trailing != null) {
            Spacer(Modifier.width(8.dp))
            trailing()
        }
    }
}

@Composable
fun SettingsTab(nav: NavHostController, onBack: (() -> Unit)? = null) {
    val ctx = LocalContext.current
    val c = MaterialTheme.colorScheme
    var lang by remember { mutableStateOf(false) }
    var rate by remember { mutableStateOf(false) }
    var sys by remember { mutableStateOf(false) }
    val pkg = ctx.packageName

    DisposableEffect(Unit) {
        AppOpenAdManager.setSettingsScreenActive(true)
        onDispose {
            AppOpenAdManager.setSettingsScreenActive(false)
            AppOpenAdManager.suppressAppOpenFor(5000L)
        }
    }

    fun open(i: Intent) {
        AppOpenAdManager.suppressAppOpenFor(60_000L)
        runCatching { ctx.startActivity(i) }.onFailure { ctx.toast("Nothing to open this.") }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Fixed Top Header & Banner
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(c.background)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = { onBack?.invoke() ?: nav.popBackStack() },
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = c.onBackground
                    )
                }

                Text(
                    text = "Settings",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = InterFontFamily,
                    color = c.onBackground
                )
            }
            AppAdaptiveBannerTop("settings_screen")
        }

        // Scrollable Middle Content
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = MaxContentWidth)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Section 1: General settings
                Text(
                    text = "General settings",
                    fontSize = 13.sp,
                    fontFamily = InterFontFamily,
                    color = c.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp, bottom = 10.dp)
                )

                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = c.surface,
                    border = BorderStroke(1.dp, c.outline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp, horizontal = 12.dp)
                    ) {
                        SettingItem(
                            icon = Icons.Filled.Language,
                            title = "Change language",
                            sub = "Select your preferred language",
                            onClick = {
                                AppOpenAdManager.suppressAppOpenFor(10000L)
                                lang = true
                            }
                        )
                        SettingItem(
                            icon = Icons.Filled.Shield,
                            title = "Change Security Question",
                            sub = "Incase you forget your password",
                            onClick = {
                                AppOpenAdManager.suppressAppOpenFor(10000L)
                                nav.navigate("security/edit")
                            }
                        )
                        SettingItem(
                            icon = Icons.Filled.LockOpen,
                            title = "Disable System Lock",
                            sub = "To avoid multiple lock",
                            trailing = {
                                SleekSwitch(
                                    checked = Store.systemLockOff,
                                    onCheckedChange = {
                                        Store.systemLockOff = it
                                        if (it) {
                                            AppOpenAdManager.suppressAppOpenFor(60000L)
                                            sys = true
                                        }
                                    }
                                )
                            }
                        )
                        SettingItem(
                            icon = Icons.Filled.DarkMode,
                            title = "Appearance",
                            sub = listOf("Follow system", "Light", "Dark")[Store.themeMode],
                            onClick = {
                                AppOpenAdManager.suppressAppOpenFor(10000L)
                                Store.themeMode = (Store.themeMode + 1) % 3
                            }
                        )
                        SettingItem(
                            icon = Icons.Filled.Lock,
                            title = "Test lock screen",
                            sub = "Try unlocking with your saved credentials",
                            onClick = {
                                AppOpenAdManager.suppressAppOpenFor(10000L)
                                nav.navigate("testLock")
                            }
                        )
                    }
                }

                // Section 2: Other
                Text(
                    text = "Other",
                    fontSize = 13.sp,
                    fontFamily = InterFontFamily,
                    color = c.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp, top = 22.dp, bottom = 10.dp)
                )

                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = c.surface,
                    border = BorderStroke(1.dp, c.outline),
                    modifier = Modifier.fillMaxWidth()
                ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp, horizontal = 12.dp)
            ) {
                SettingItem(
                    icon = Icons.Filled.Share,
                    title = "Share App",
                    sub = "Tap to share our app",
                    onClick = {
                        open(
                            Intent.createChooser(
                                Intent(Intent.ACTION_SEND).setType("text/plain")
                                    .putExtra(Intent.EXTRA_TEXT, "Try Voice Screen Locker: https://play.google.com/store/apps/details?id=$pkg"), null
                            )
                        )
                    }
                )
                SettingItem(
                    icon = Icons.Filled.Feedback,
                    title = "Feedback",
                    sub = "Help us improve our app",
                    onClick = {
                        open(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:videodownloader0786@gmail.com")).putExtra(Intent.EXTRA_SUBJECT, "Voice Screen Locker feedback"))
                    }
                )
                SettingItem(
                    icon = Icons.Filled.VerifiedUser,
                    title = "Privacy Policies",
                    sub = "Read our privacy policy",
                    onClick = {
                        open(Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com/privacy")))
                    }
                )
                SettingItem(
                    icon = Icons.Filled.Stars,
                    title = "Rates Us",
                    sub = "Rate the app on the store",
                    onClick = {
                        AppOpenAdManager.suppressAppOpenFor(60000L)
                        rate = true
                    }
                )
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

    // Fixed Bottom Native Ad
    AppNativeAdView(
        screenKey = "settings_screen",
        template = 1,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
    )
}

    if (lang) {
        LanguageDialog(Store.language, { lang = false }) {
            Store.language = it
            lang = false
        }
    }

    if (sys) {
        AlertDialog(
            onDismissRequest = { sys = false },
            title = { Text("Disable system lock", fontFamily = InterFontFamily) },
            text = { Text("To avoid two lock screens, set your phone's screen lock to None or Swipe in system security settings.", fontFamily = InterFontFamily) },
            confirmButton = { TextButton({ sys = false; open(Intent(Settings.ACTION_SECURITY_SETTINGS)) }) { Text("Open settings", fontFamily = InterFontFamily) } },
            dismissButton = { TextButton({ sys = false }) { Text("Later", fontFamily = InterFontFamily) } },
        )
    }

    if (rate) {
        RateDialog({ rate = false }) { selectedStars ->
            rate = false
            if (selectedStars >= 5) {
                val ok = runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$pkg"))) }.isSuccess
                if (!ok) open(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$pkg")))
            } else {
                val feedbackIntent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:videodownloader@example.com")).apply {
                    putExtra(Intent.EXTRA_SUBJECT, "Voice Screen Locker Feedback ($selectedStars stars)")
                    putExtra(Intent.EXTRA_TEXT, "Hi team,\n\nI rated $selectedStars stars. Here is how you can improve the app:\n\n")
                }
                open(feedbackIntent)
            }
        }
    }
}

@Composable
private fun RateDialog(onDismiss: () -> Unit, onRate: (Int) -> Unit) {
    var stars by remember { mutableIntStateOf(0) }
    val c = MaterialTheme.colorScheme

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = c.surface,
            border = BorderStroke(1.dp, c.outline),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp, horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "🤩",
                    fontSize = 44.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(14.dp))

                Text(
                    text = "Do you like\nVoice Screen Locker",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = InterFontFamily,
                    textAlign = TextAlign.Center,
                    color = c.onSurface,
                    lineHeight = 24.sp
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = "Give us a quick review and help us\nimprove",
                    fontSize = 12.sp,
                    fontFamily = InterFontFamily,
                    color = c.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = 17.sp
                )

                Spacer(Modifier.height(20.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    (1..5).forEach { i ->
                        val isFilled = i <= stars
                        if (isFilled) {
                            Image(
                                painter = painterResource(R.drawable.ic_star),
                                contentDescription = "$i stars",
                                modifier = Modifier
                                    .size(34.dp)
                                    .clickable { stars = i }
                            )
                        } else {
                            Image(
                                painter = painterResource(R.drawable.ic_star),
                                contentDescription = "$i stars",
                                colorFilter = ColorFilter.tint(Color(0xFFD1D5DB), BlendMode.SrcIn),
                                modifier = Modifier
                                    .size(34.dp)
                                    .clickable { stars = i }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(22.dp))

                Button(
                    onClick = { if (stars > 0) onRate(stars) },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (stars > 0) c.primary else if (c.background == Color(0xFF0E1320)) Color(0xFF1D3A60) else Color(0xFFCBE3FB),
                        contentColor = if (stars > 0) Color.White else if (c.background == Color(0xFF0E1320)) Color(0xFF8CC4FF) else Color(0xFF1789F4)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text(
                        text = "Rate Us",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = InterFontFamily,
                        color = if (stars > 0) Color.White else if (c.background == Color(0xFF0E1320)) Color(0xFF8CC4FF) else Color(0xFF1789F4)
                    )
                }
            }
        }
    }
}
