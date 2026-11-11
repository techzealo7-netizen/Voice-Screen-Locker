package com.voicelocker.app.ui.screens.onboarding

import android.app.Activity
import android.util.Log
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.voicelocker.ads.AdManager
import com.voicelocker.ads.AdsInitializer
import com.voicelocker.ads.AppNativeAdView
import com.voicelocker.ads.AppOpenLifecycleTracker
import com.voicelocker.ads.BillingManager
import com.voicelocker.ads.ConsentManager
import com.voicelocker.ads.RemoteConfigManager
import com.voicelocker.ads.SplashAdPreloader
import com.voicelocker.app.R
import com.voicelocker.app.data.local.Store
import com.voicelocker.app.ui.theme.Blue
import com.voicelocker.app.ui.theme.InterFontFamily
import kotlin.coroutines.resume
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine

@Composable
fun SplashScreen(onDone: () -> Unit) {
    val progress = remember { Animatable(0f) }
    val context = LocalContext.current
    val activity = context as? Activity
    val view = LocalView.current
    var isContinueEnabled by remember { mutableStateOf(RemoteConfigManager.isSplashContinueButtonEnabled()) }
    var canRequestAds by remember { mutableStateOf(ConsentManager.canRequestAds(context)) }
    var isProgressFinished by remember { mutableStateOf(false) }
    val configVersion by RemoteConfigManager.configVersion.collectAsState()

    val isPremium = remember { BillingManager.getInstance(context).isPremiumUser() }
    val isNativeOrBannerEnabled = remember(configVersion) {
        !isPremium && (AdManager.isNativeEnabled("splash") || AdManager.isBannerEnabled("splash"))
    }

    val dark = when (Store.themeMode) {
        1 -> false
        2 -> true
        else -> isSystemInDarkTheme()
    }

    DisposableEffect(Unit) {
        AppOpenLifecycleTracker.isSplashScreenActive = true
        val window = (view.context as? Activity)?.window
        val insetsController = window?.let { WindowCompat.getInsetsController(it, view) }
        // Ensure status bar & navigation bar icons are white
        insetsController?.isAppearanceLightStatusBars = false
        insetsController?.isAppearanceLightNavigationBars = false
        // Ensure status bar and nav bar backgrounds are the primary blue
        window?.statusBarColor = android.graphics.Color.parseColor("#1789F4")
        window?.navigationBarColor = android.graphics.Color.parseColor("#1789F4")

        onDispose {
            AppOpenLifecycleTracker.isSplashScreenActive = false
            // Restore status bar and navigation bar to transparent with normal theme icons
            window?.statusBarColor = android.graphics.Color.TRANSPARENT
            window?.navigationBarColor = android.graphics.Color.TRANSPARENT
            insetsController?.isAppearanceLightStatusBars = !dark
            insetsController?.isAppearanceLightNavigationBars = !dark
        }
    }

    LaunchedEffect(Unit) {
        // Step 1: Concurrently fetch fresh Remote Config & gather UMP consent
        val fetchJob = launch {
            RemoteConfigManager.fetchFreshConfig(2500L)
        }

        val consentJob = launch {
            if (canRequestAds) {
                Log.d("ADSMANAGER", "[splash] Consent already available -> initializing MobileAds & preloading splash ad")
                AdsInitializer.initializeIfNeeded(context)
                SplashAdPreloader.startPreload(context)
            }
            if (activity != null) {
                Log.d("ADSMANAGER", "[splash] Gathering UMP consent in parallel...")
                val allowed = suspendCancellableCoroutine<Boolean> { cont ->
                    ConsentManager.gatherConsent(activity) { canRequest ->
                        if (cont.isActive) {
                            cont.resume(canRequest)
                        }
                    }
                }
                canRequestAds = allowed
                Log.d("ADSMANAGER", "[splash] UMP consent completed: canRequestAds=$allowed")
                if (allowed) {
                    AdsInitializer.initializeIfNeeded(context)
                    SplashAdPreloader.startPreload(context)
                }
            }
        }

        // Step 2: Ensure fresh Remote Config is fetched before starting animation
        fetchJob.join()

        // Step 3: Read resolved duration and continue button setting from Remote Config
        val loadSeconds = RemoteConfigManager.getSplashLoaderSec().coerceAtLeast(1)
        val continueEnabled = RemoteConfigManager.isSplashContinueButtonEnabled()
        isContinueEnabled = continueEnabled
        Log.d("ADSMANAGER", "[splash] Remote config ready -> animating progress for $loadSeconds seconds (continueEnabled=$continueEnabled)")

        // Step 4: Run splash progress animation for the exact configured duration
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = loadSeconds * 1000,
                easing = LinearEasing
            )
        )
        isProgressFinished = true
        Log.d("ADSMANAGER", "[splash] Progress animation finished ($loadSeconds seconds elapsed)")

        // Step 5: Ensure consent flow completed before showing ad or navigating
        consentJob.join()

        // Step 6: If continue button is disabled, show ad immediately at end of progress bar
        if (!continueEnabled) {
            if (canRequestAds && activity != null) {
                SplashAdPreloader.show(
                    activity = activity,
                    waitIfPreloading = true
                ) {
                    AppOpenLifecycleTracker.isSplashScreenActive = false
                    onDone()
                }
            } else {
                AppOpenLifecycleTracker.isSplashScreenActive = false
                onDone()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Blue)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.weight(if (isNativeOrBannerEnabled) 0.85f else 1f))

            // Splash Illustration
            Image(
                painter = painterResource(R.drawable.ic_splash),
                contentDescription = "Voice Screen Locker",
                modifier = Modifier
                    .fillMaxWidth(0.50f)
                    .wrapContentHeight(),
                contentScale = ContentScale.Fit
            )

            Spacer(Modifier.height(14.dp))

            Text(
                text = "Voice Screen",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Light,
                fontFamily = InterFontFamily
            )
            Box(
                Modifier
                    .background(Color.White, RoundedCornerShape(4.dp))
                    .padding(horizontal = 10.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "Locker",
                    color = Blue,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = InterFontFamily
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Secure Your Screen, Your Way",
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 13.sp,
                fontFamily = InterFontFamily
            )

            // Space above native ad (reduced to 0.30f to move native ad up and eliminate empty gap)
            Spacer(Modifier.weight(if (isNativeOrBannerEnabled) 0.30f else 1f))

            // ============================================
            // BOTTOM SECTION: Native Ad above, Loading Bar + Text at bottom
            // ============================================
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Native Ad (above loading bar): Stable 260dp container to prevent layout jumping/flickering
                if (isNativeOrBannerEnabled) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (canRequestAds) {
                            AppNativeAdView(
                                screenKey = "splash",
                                modifier = Modifier.fillMaxWidth(),
                                isBottom = false
                            )
                        }
                    }
                    Spacer(Modifier.height(32.dp))
                }

                // Horizontal Loading Bar
                Box(
                    modifier = Modifier
                        .width(230.dp)
                        .height(12.dp)
                        .clip(RoundedCornerShape(percent = 50))
                        .background(Color.White)
                        .border(1.5.dp, Color.White, RoundedCornerShape(percent = 50))
                        .padding(2.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progress.value)
                            .clip(RoundedCornerShape(percent = 50))
                            .background(Blue)
                    )
                }

                Spacer(Modifier.height(6.dp))

                Text(
                    text = "This action may contain ad",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 11.sp,
                    fontFamily = InterFontFamily
                )

                if (isContinueEnabled && isProgressFinished) {
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = {
                            if (canRequestAds && activity != null) {
                                SplashAdPreloader.show(
                                    activity = activity,
                                    waitIfPreloading = true
                                ) {
                                    AppOpenLifecycleTracker.isSplashScreenActive = false
                                    onDone()
                                }
                            } else {
                                AppOpenLifecycleTracker.isSplashScreenActive = false
                                onDone()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Blue
                        ),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .height(44.dp)
                            .padding(horizontal = 32.dp)
                    ) {
                        Text(
                            text = "Continue",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = InterFontFamily
                        )
                    }
                }
            }

            // Bottom breathing room lifting the bottom section up
            if (isNativeOrBannerEnabled) {
                Spacer(Modifier.weight(0.18f))
            } else {
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}
