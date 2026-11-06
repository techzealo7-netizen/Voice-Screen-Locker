package com.voicelocker.app.ui.screens.onboarding

import android.app.Activity
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
import com.voicelocker.ads.AppNativeAdView
import com.voicelocker.ads.RemoteConfigManager
import com.voicelocker.ads.SplashAdPreloader
import com.voicelocker.app.R
import com.voicelocker.app.data.local.Store
import com.voicelocker.app.ui.theme.Blue
import com.voicelocker.app.ui.theme.InterFontFamily

@Composable
fun SplashScreen(onDone: () -> Unit) {
    val progress = remember { Animatable(0f) }
    val context = LocalContext.current
    val activity = context as? Activity
    val view = LocalView.current
    val continueEnabled = remember { RemoteConfigManager.isSplashContinueButtonEnabled() }
    var isProgressFinished by remember { mutableStateOf(false) }

    val dark = when (Store.themeMode) {
        1 -> false
        2 -> true
        else -> isSystemInDarkTheme()
    }

    DisposableEffect(Unit) {
        val window = (view.context as? Activity)?.window
        val insetsController = window?.let { WindowCompat.getInsetsController(it, view) }
        // Ensure status bar & navigation bar icons are white
        insetsController?.isAppearanceLightStatusBars = false
        insetsController?.isAppearanceLightNavigationBars = false
        // Ensure status bar and nav bar backgrounds are the primary blue
        window?.statusBarColor = android.graphics.Color.parseColor("#1789F4")
        window?.navigationBarColor = android.graphics.Color.parseColor("#1789F4")

        onDispose {
            // Restore status bar and navigation bar to transparent with normal theme icons
            window?.statusBarColor = android.graphics.Color.TRANSPARENT
            window?.navigationBarColor = android.graphics.Color.TRANSPARENT
            insetsController?.isAppearanceLightStatusBars = !dark
            insetsController?.isAppearanceLightNavigationBars = !dark
        }
    }

    LaunchedEffect(Unit) {
        // Preload enabled splash fullscreen ad (App Open or Interstitial) during progress
        SplashAdPreloader.startPreload(context)
        val loadSeconds = RemoteConfigManager.getSplashLoaderSec().coerceAtLeast(1)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = loadSeconds * 1000,
                easing = LinearEasing
            )
        )
        isProgressFinished = true

        // If continue button is disabled, show ad immediately at end of progress bar with no extra loading delay
        if (!continueEnabled) {
            if (activity != null) {
                SplashAdPreloader.show(
                    activity = activity,
                    waitIfPreloading = true
                ) {
                    onDone()
                }
            } else {
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
                .statusBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.weight(1f))

            // Splash Illustration
            Image(
                painter = painterResource(R.drawable.ic_splash),
                contentDescription = "Voice Screen Locker",
                modifier = Modifier
                    .fillMaxWidth(0.52f)
                    .wrapContentHeight(),
                contentScale = ContentScale.Fit
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text = "Voice Screen",
                color = Color.White,
                fontSize = 30.sp,
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
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = InterFontFamily
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Secure Your Screen, Your Way",
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 13.sp,
                fontFamily = InterFontFamily
            )

            Spacer(Modifier.weight(1f))

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

            Spacer(Modifier.height(8.dp))

            Text(
                text = "This action may contain ad",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 11.sp,
                fontFamily = InterFontFamily
            )

            if (continueEnabled && isProgressFinished) {
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = {
                        if (activity != null) {
                            SplashAdPreloader.show(
                                activity = activity,
                                waitIfPreloading = true
                            ) {
                                onDone()
                            }
                        } else {
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

            Spacer(Modifier.height(12.dp))

            // Bottom Native Ad with same-size Inline Banner fallback
            AppNativeAdView(
                screenKey = "splash",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                isBottom = true
            )
        }
    }
}
