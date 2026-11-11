package com.voicelocker.app.ui.screens.home

import android.app.Activity
import android.util.Log
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.navigation.NavHostController
import com.voicelocker.ads.*
import com.voicelocker.app.R
import com.voicelocker.app.data.local.Store
import com.voicelocker.app.ui.components.ColCard
import com.voicelocker.app.ui.components.MaxContentWidth
import com.voicelocker.app.ui.components.Muted
import com.voicelocker.app.ui.components.ThemeHorizontalRow
import com.voicelocker.app.ui.components.ThemeUnlockDialog
import com.voicelocker.app.ui.theme.Blue
import com.voicelocker.app.ui.theme.InterFontFamily
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun LockTileBtn(
    iconRes: Int,
    label: String,
    onClick: () -> Unit
) = Column(
    Modifier
        .clip(RoundedCornerShape(12.dp))
        .clickable(onClick = onClick)
        .padding(10.dp),
    horizontalAlignment = Alignment.CenterHorizontally
) {
    Image(
        painter = painterResource(iconRes),
        contentDescription = label,
        modifier = Modifier.size(32.dp)
    )
    Spacer(Modifier.height(6.dp))
    Text(label, fontFamily = InterFontFamily, fontSize = 11.sp, textAlign = TextAlign.Center)
}

/**
 * Animated Voice Lock Button:
 * When inactive, animates in an organic ripple & bubble wave form with expanding
 * concentric halos and floating bubble particles, plus a subtle gentle pulse on the mic button.
 */
@Composable
fun VoiceLockMicButton(
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "mic_ripple_transition")

    val rippleProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ripple_progress"
    )

    val micScale by infiniteTransition.animateFloat(
        initialValue = if (!isActive) 0.96f else 1f,
        targetValue = if (!isActive) 1.04f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mic_scale"
    )

    val rippleColor = Blue

    Box(
        modifier = modifier.size(150.dp),
        contentAlignment = Alignment.Center
    ) {
        // Draw ripple and bubbles only when inactive
        if (!isActive) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val centerOffset = center
                val baseRadius = 48.dp.toPx() // radius of 96dp mic button
                val maxRadius = 72.dp.toPx()  // expands out to 144dp diameter

                // 1. Concentric expanding ripple waves & bubble halos
                for (i in 0 until 3) {
                    val phase = (rippleProgress + i * (1f / 3f)) % 1f
                    val currentRadius = baseRadius + phase * (maxRadius - baseRadius)
                    val alpha = ((1f - phase) * (1f - phase)).coerceIn(0f, 1f)

                    // Bubble fill halo
                    drawCircle(
                        color = rippleColor.copy(alpha = alpha * 0.16f),
                        radius = currentRadius,
                        center = centerOffset
                    )

                    // Bubble ripple stroke edge
                    drawCircle(
                        color = rippleColor.copy(alpha = alpha * 0.45f),
                        radius = currentRadius,
                        center = centerOffset,
                        style = Stroke(width = 1.8.dp.toPx())
                    )
                }

                // 2. Floating organic bubble particles around the button
                val bubbleAngles = floatArrayOf(35f, 95f, 160f, 220f, 285f, 335f)
                val bubbleBaseSizes = floatArrayOf(3.5f, 4.5f, 3.0f, 5.0f, 3.8f, 4.2f)

                for (idx in bubbleAngles.indices) {
                    val phase = (rippleProgress + idx * (1f / bubbleAngles.size)) % 1f
                    val distance = baseRadius + 3.dp.toPx() + phase * (maxRadius - baseRadius - 4.dp.toPx())
                    val angleRad = Math.toRadians(bubbleAngles[idx].toDouble())
                    val bx = centerOffset.x + (distance * cos(angleRad)).toFloat()
                    val by = centerOffset.y + (distance * sin(angleRad)).toFloat()
                    val bAlpha = ((1f - phase) * (1f - phase)).coerceIn(0f, 1f)
                    val bRadius = bubbleBaseSizes[idx].dp.toPx() * (0.8f + phase * 0.35f)

                    // Translucent bubble body
                    drawCircle(
                        color = rippleColor.copy(alpha = bAlpha * 0.35f),
                        radius = bRadius,
                        center = Offset(bx, by)
                    )
                    // White shine/reflection ring on bubble
                    drawCircle(
                        color = Color.White.copy(alpha = bAlpha * 0.65f),
                        radius = bRadius,
                        center = Offset(bx, by),
                        style = Stroke(width = 1.dp.toPx())
                    )
                }
            }
        }

        // Central Mic Button
        Image(
            painter = painterResource(R.drawable.ic_mic_home),
            contentDescription = "Voice Lock",
            modifier = Modifier
                .size(96.dp)
                .graphicsLayer {
                    scaleX = micScale
                    scaleY = micScale
                }
                .clip(CircleShape)
                .clickable(onClick = onClick)
        )
    }
}

@Composable
fun HomeTab(nav: NavHostController) {
    val c = MaterialTheme.colorScheme
    val context = LocalContext.current
    val activity = context as? Activity
    val active = Store.voiceEnabled && Store.hasVoice
    var loadingAd by remember { mutableStateOf(false) }
    var clickCount by remember { mutableIntStateOf(0) }
    val isPremium = BillingManager.getInstance(context).isPremiumUser()
    var unlockDialogThemeIndex by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(Unit) {
        AppOpenLifecycleTracker.markJourneyReady()
        // Preload fresh interstitial ad at Home screen launch if enabled
        InterstitialAdManager.preload("home_screen")
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        if (!InterstitialAdManager.hasPreloadedAd("home_screen")) {
            InterstitialAdManager.preload("home_screen")
        }
    }

    fun showInterOrNavigate(adKey: String, onNavigate: () -> Unit) {
        val isSecondStrategy = RemoteConfigManager.isSecondStrategyEnabled()
        val effectiveAdKey = if (isSecondStrategy) {
            adKey
        } else {
            clickCount++
            val threshold = RemoteConfigManager.getInterstitialClickThreshold("home_screen")
            val isNth = threshold > 0 && clickCount % threshold == 0
            if (isNth && AdManager.isInterstitialEnabled("home_screen", "interstitial_nth_click")) {
                "interstitial_nth_click"
            } else {
                adKey
            }
        }

        CounterAdManager.onAction(
            activity = activity,
            screenKey = "home_screen",
            adKey = effectiveAdKey,
            onLoading = { loadingAd = it }
        ) {
            onNavigate()
        }
    }

    Box(Modifier.fillMaxSize(), Alignment.TopCenter) {
        Column(
            Modifier
                .widthIn(max = MaxContentWidth)
                .fillMaxSize()
        ) {
            // FIXED HEADER: Toolbar and Top Banner
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 16.dp, bottom = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Voice Lock Screen",
                        fontFamily = InterFontFamily,
                        modifier = Modifier.weight(1f),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    IconButton({
                        if (RemoteConfigManager.isInAppScreenEnabled()) {
                            nav.navigate("premium")
                        }
                    }) {
                        Image(
                            painter = painterResource(R.drawable.ic_crown_home),
                            contentDescription = "Premium",
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                // Fixed Top Banner between toolbar and Voice Lock Inactive card
                val bannerId = AdManager.getBannerAdId("home_screen")
                if (AdManager.isBannerEnabled("home_screen") && !bannerId.isNullOrBlank()) {
                    Spacer(Modifier.height(8.dp))
                    NextGenAdaptiveBanner(adUnitId = bannerId, modifier = Modifier.fillMaxWidth())
                }
            }

            // SCROLLABLE BODY
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(top = 4.dp, bottom = 16.dp)
            ) {
                ColCard(center = true, pad = 14.dp) {
                    VoiceLockMicButton(
                        isActive = active,
                        onClick = { showInterOrNavigate("inter_voice_lock") { nav.navigate("voiceIntro") } }
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (active) "Voice lock Active" else "Voice lock Inactive",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Medium
                    )
                    Muted(
                        if (active) "Listening for your unique voice lock key" else "Tap the mic to set up your voice lock",
                        10
                    )
                }

                // Native Centre Ad
                AppNativeAdView(
                    screenKey = "home_screen",
                    template = 3,
                    isCenter = true,
                    modifier = Modifier.fillMaxWidth()
                )

                ColCard(pad = 8.dp) {
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceEvenly) {
                        LockTileBtn(R.drawable.ic_lock, "Pin Lock") {
                            showInterOrNavigate("inter_pin_lock") { nav.navigate("pin/pin") }
                        }
                        LockTileBtn(R.drawable.ic_fingerprint, "Fingerprint Lock") {
                            showInterOrNavigate("inter_fingerprint_lock") { nav.navigate("fingerprint") }
                        }
                        LockTileBtn(R.drawable.ic_pattern, "Pattern Lock") {
                            showInterOrNavigate("inter_pattern_lock") { nav.navigate("pattern") }
                        }
                    }
                }
                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    Text(
                        "Themes",
                        fontFamily = InterFontFamily,
                        modifier = Modifier.weight(1f),
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "See more",
                        fontFamily = InterFontFamily,
                        color = c.primary,
                        fontSize = 13.sp,
                        modifier = Modifier.clickable {
                            showInterOrNavigate("inter_see_more") { nav.navigate("themes") }
                        }
                    )
                }
                ThemeHorizontalRow { it ->
                    showInterOrNavigate("inter_see_more") {
                        if (Store.isThemeUnlocked(it, isPremium)) {
                            nav.navigate("themePreview/$it")
                        } else {
                            unlockDialogThemeIndex = it
                        }
                    }
                }

                unlockDialogThemeIndex?.let { themeIdx ->
                    ThemeUnlockDialog(
                        themeIndex = themeIdx,
                        onDismiss = { unlockDialogThemeIndex = null },
                        onUnlocked = {
                            nav.navigate("themePreview/$themeIdx")
                        },
                        onBuyPremium = {
                            if (RemoteConfigManager.isInAppScreenEnabled()) {
                                nav.navigate("premium")
                            }
                        }
                    )
                }

                if (AdManager.isNativeBottomEnabled("home_screen")) {
                    AppNativeAdView(
                        screenKey = "home_screen",
                        template = 3,
                        isBottom = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }

    AdLoadingDialog(isVisible = loadingAd)
}
