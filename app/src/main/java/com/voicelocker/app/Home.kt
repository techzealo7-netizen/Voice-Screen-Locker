package com.voicelocker.app

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavHostController

@Composable
fun MainScreen(nav: NavHostController) {
    val c = MaterialTheme.colorScheme
    var tab by rememberSaveable { mutableIntStateOf(0) }
    Scaffold(containerColor = c.background, bottomBar = {
        NavigationBar(containerColor = c.surface) {
            listOf(Icons.Filled.Lock to "Ai Voice Lock", Icons.Filled.Forum to "Ai voice Notes", Icons.Filled.Settings to "Setting").forEachIndexed { i, (ic, l) ->
                NavigationBarItem(tab == i, { tab = i }, { Icon(ic, l) }, label = { Text(l, fontSize = 11.sp, maxLines = 1) },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = c.primary, selectedTextColor = c.primary, indicatorColor = Color.Transparent,
                        unselectedIconColor = c.onSurfaceVariant, unselectedTextColor = c.onSurfaceVariant))
            }
        }
    }) { pad ->
        Box(Modifier.padding(pad)) { when (tab) { 0 -> HomeTab(nav); 1 -> NotesTab(nav); else -> SettingsTab(nav) } }
    }
}

@Composable
private fun LockTileBtn(icon: ImageVector, label: String, onClick: () -> Unit) = Column(
    Modifier.clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
    Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp)); Spacer(Modifier.height(6.dp)); Text(label, fontSize = 11.sp, textAlign = TextAlign.Center)
}

@Composable
fun HomeTab(nav: NavHostController) {
    val c = MaterialTheme.colorScheme
    val active = Store.voiceEnabled && Store.hasVoice
    TabColumn {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Voice Lock Screen", Modifier.weight(1f), fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            IconButton({ nav.navigate("premium") }) {
                Image(
                    painter = painterResource(R.drawable.ic_crown_home),
                    contentDescription = "Premium",
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        ColCard(center = true, pad = 22.dp) {
            MicButton(72.dp, c.primaryContainer, if (active) c.primary else c.onSurfaceVariant, onClick = { nav.navigate("voiceIntro") })
            Spacer(Modifier.height(14.dp))
            Text(if (active) "Voice lock Active" else "Voice lock Inactive", fontWeight = FontWeight.Medium)
            Muted(if (active) "Listening for your unique voice lock key" else "Tap the mic to set up your voice lock", 10)
        }
        ColCard(pad = 8.dp) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceEvenly) {
                LockTileBtn(Icons.Filled.Dialpad, "Pin Lock") { nav.navigate("pin/pin") }
                LockTileBtn(Icons.Filled.Fingerprint, "Fingerprint Lock") { nav.navigate("fingerprint") }
                LockTileBtn(Icons.Filled.Apps, "Pattern Lock") { nav.navigate("pattern") }
            }
        }
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            Text("Themes", Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
            Text("See more", color = c.primary, fontSize = 13.sp, modifier = Modifier.clickable { nav.navigate("themes") })
        }
        ThemeGrid(listOf(0, 1, 2, 3)) { nav.navigate("themePreview/$it") }
    }
}

@Composable
private fun SettingRow(icon: ImageVector, title: String, sub: String, onClick: (() -> Unit)? = null, trailing: (@Composable () -> Unit)? = null) =
    RowCard(onClick = onClick) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) { Text(title, fontSize = 14.sp); Muted(sub, 10) }
        trailing?.invoke()
    }

@Composable
fun SettingsTab(nav: NavHostController) {
    val ctx = LocalContext.current
    val c = MaterialTheme.colorScheme
    var lang by remember { mutableStateOf(false) }
    var rate by remember { mutableStateOf(false) }
    var sys by remember { mutableStateOf(false) }
    val pkg = ctx.packageName
    fun open(i: Intent) = runCatching { ctx.startActivity(i) }.onFailure { ctx.toast("Nothing to open this.") }

    TabColumn {
        Text("Setting", Modifier.fillMaxWidth(), textAlign = TextAlign.Center, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(16.dp))
        Muted("General settings", 12, Modifier.padding(bottom = 6.dp))
        SettingRow(Icons.Filled.Language, "Change language", Store.language, { lang = true })
        SettingRow(Icons.Filled.Shield, "Change Security Question", "In case you forget your password", { nav.navigate("security/edit") })
        SettingRow(Icons.Filled.LockOpen, "Disable System Lock", "To avoid multiple lock", null) {
            Switch(Store.systemLockOff, { Store.systemLockOff = it; if (it) sys = true })
        }
        SettingRow(Icons.Filled.DarkMode, "Appearance", listOf("Follow system", "Light", "Dark")[Store.themeMode], { Store.themeMode = (Store.themeMode + 1) % 3 })
        SettingRow(Icons.Filled.Lock, "Test lock screen", "Try unlocking with your saved credentials", { nav.navigate("testLock") })
        Muted("Other", 12, Modifier.padding(top = 8.dp, bottom = 6.dp))
        SettingRow(Icons.Filled.Share, "Share App", "Tap to share with friends", {
            open(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain")
                .putExtra(Intent.EXTRA_TEXT, "Try Voice Screen Locker: https://play.google.com/store/apps/details?id=$pkg"), null))
        })
        SettingRow(Icons.Filled.Feedback, "Feedback", "Help us improve our app", {
            open(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:support@example.com")).putExtra(Intent.EXTRA_SUBJECT, "Voice Screen Locker feedback")) // TODO: your email
        })
        SettingRow(Icons.Filled.PrivacyTip, "Privacy Policies", "Read our privacy policy", { open(Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com/privacy"))) }) // TODO: your URL
        SettingRow(Icons.Filled.Star, "Rates Us", "Rate the app on the store", { rate = true })
    }

    if (lang) LanguageDialog(Store.language, { lang = false }) { Store.language = it; lang = false }
    if (sys) AlertDialog(
        onDismissRequest = { sys = false }, title = { Text("Disable system lock") },
        text = { Text("To avoid two lock screens, set your phone's screen lock to None or Swipe in system security settings.") },
        confirmButton = { TextButton({ sys = false; open(Intent(Settings.ACTION_SECURITY_SETTINGS)) }) { Text("Open settings") } },
        dismissButton = { TextButton({ sys = false }) { Text("Later") } },
    )
    if (rate) RateDialog({ rate = false }) { selectedStars ->
        rate = false
        if (selectedStars >= 5) {
            // 5 stars: Direct to Google Play Store
            val ok = runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$pkg"))) }.isSuccess
            if (!ok) open(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$pkg")))
        } else {
            // Less than 5 stars: Direct to Feedback email
            val feedbackIntent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:support@example.com")).apply {
                putExtra(Intent.EXTRA_SUBJECT, "Voice Screen Locker Feedback ($selectedStars stars)")
                putExtra(Intent.EXTRA_TEXT, "Hi team,\n\nI rated $selectedStars stars. Here is how you can improve the app:\n\n")
            }
            open(feedbackIntent)
        }
    }
}

@Composable
private fun RateDialog(onDismiss: () -> Unit, onRate: (Int) -> Unit) {
    var stars by remember { mutableIntStateOf(0) }
    val c = MaterialTheme.colorScheme

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
            color = c.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Close Button
                Box(Modifier.fillMaxWidth()) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .size(28.dp)
                    ) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = "Close",
                            tint = c.onSurface.copy(alpha = 0.65f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Heading
                Text(
                    text = "Do you like\nVoice Translator?",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    lineHeight = 28.sp,
                    color = c.onSurface
                )

                Spacer(Modifier.height(14.dp))

                // Subtitle 1
                Text(
                    text = "We're always working to make\nyour experience better!",
                    fontSize = 14.sp,
                    color = c.onSurface.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(Modifier.height(8.dp))

                // Subtitle 2
                Text(
                    text = "Your feedback and rating helps us grow.",
                    fontSize = 12.sp,
                    color = c.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(20.dp))

                // Stars Row
                Row(
                    Modifier.padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    (1..5).forEach { i ->
                        val isFilled = i <= stars
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = "$i stars",
                            modifier = Modifier
                                .size(42.dp)
                                .clickable { stars = i },
                            tint = if (isFilled) Color(0xFFFFB400) else Color(0xFFC4C4C4)
                        )
                    }
                }

                // Curving arrow pointing directly up to the 5th star
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp, end = 20.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    androidx.compose.foundation.Canvas(modifier = Modifier.size(width = 62.dp, height = 52.dp)) {
                        val strokeColor = Color(0xFF4A4E57) // Slate charcoal color matching the uploaded image
                        val strokeWidth = 2.4.dp.toPx()

                        // Wide sweeping upward curve
                        val tailPath = androidx.compose.ui.graphics.Path().apply {
                            moveTo(x = 2.dp.toPx(), y = 46.dp.toPx())
                            cubicTo(
                                x1 = 28.dp.toPx(), y1 = 45.dp.toPx(),
                                x2 = 47.dp.toPx(), y2 = 36.dp.toPx(),
                                x3 = 51.dp.toPx(), y3 = 14.dp.toPx()
                            )
                        }
                        drawPath(
                            path = tailPath,
                            color = strokeColor,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(
                                width = strokeWidth,
                                cap = androidx.compose.ui.graphics.StrokeCap.Round
                            )
                        )

                        // Filled solid triangle arrowhead pointing upwards/slightly tilted
                        val arrowHead = androidx.compose.ui.graphics.Path().apply {
                            moveTo(x = 52.dp.toPx(), y = 2.dp.toPx())   // sharp tip
                            lineTo(x = 42.dp.toPx(), y = 16.dp.toPx())  // left base
                            lineTo(x = 50.dp.toPx(), y = 14.dp.toPx())  // center indent
                            lineTo(x = 59.dp.toPx(), y = 14.dp.toPx())  // right base
                            close()
                        }
                        drawPath(
                            path = arrowHead,
                            color = strokeColor
                        )
                    }
                }

                Spacer(Modifier.height(22.dp))

                // Rate Button
                Button(
                    onClick = { onRate(stars) },
                    enabled = stars > 0,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = c.primary,
                        contentColor = Color.White,
                        disabledContainerColor = Color(0xFFB0B0B8),
                        disabledContentColor = Color.White.copy(alpha = 0.85f)
                    )
                ) {
                    Text(
                        "Rate",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
