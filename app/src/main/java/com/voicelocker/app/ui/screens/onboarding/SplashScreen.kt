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
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.voicelocker.app.R
import com.voicelocker.app.data.local.Store
import com.voicelocker.app.ui.theme.Blue

@Composable
fun SplashScreen(onDone: () -> Unit) {
    val progress = remember { Animatable(0f) }
    val view = LocalView.current

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
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = 3000,
                easing = LinearEasing
            )
        )
        onDone()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Blue)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.weight(3f))

            // Splash Illustration
            Image(
                painter = painterResource(R.drawable.ic_splash),
                contentDescription = "Voice Screen Locker",
                modifier = Modifier
                    .fillMaxWidth(0.68f)
                    .wrapContentHeight(),
                contentScale = ContentScale.Fit
            )

            Spacer(Modifier.height(28.dp))

            Text(
                text = "Voice Screen",
                color = Color.White,
                fontSize = 34.sp,
                fontWeight = FontWeight.Light
            )
            Box(
                Modifier
                    .background(Color.White)
                    .padding(horizontal = 10.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "Locker",
                    color = Blue,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Secure Your Screen, Your Way",
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 14.sp
            )

            Spacer(Modifier.weight(3f))

            // Smooth 3-Second Horizontal Loading Bar (White stroke, inside Blue filling)
            Box(
                modifier = Modifier
                    .width(240.dp)
                    .height(14.dp)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(Color.White)
                    .border(1.5.dp, Color.White, RoundedCornerShape(percent = 50))
                    .padding(2.5.dp),
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

            Spacer(Modifier.height(36.dp))
        }
    }
}
