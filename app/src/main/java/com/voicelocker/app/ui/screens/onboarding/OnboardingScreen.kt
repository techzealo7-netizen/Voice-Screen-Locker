package com.voicelocker.app.ui.screens.onboarding

import android.app.Activity
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.voicelocker.ads.AdLoadingDialog
import com.voicelocker.ads.AdManager
import com.voicelocker.ads.AppNativeAdView
import com.voicelocker.ads.InterstitialAdManager
import com.voicelocker.app.R
import com.voicelocker.app.data.local.Store
import com.voicelocker.app.ui.components.MaxContentWidth
import com.voicelocker.app.ui.theme.Blue
import com.voicelocker.app.ui.theme.InterFontFamily
import kotlinx.coroutines.launch

private data class OnboardingPageData(
    val imageRes: Int,
    val title: String,
    val description: String
)

@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val pages = listOf(
        OnboardingPageData(
            imageRes = R.drawable.ic_onb1,
            title = "Secure with Voice Notes",
            description = "Create a unique voice note to protect your device\nwith secure voice authentication."
        ),
        OnboardingPageData(
            imageRes = R.drawable.ic_onb2,
            title = "Multiple Lock\nOptions",
            description = "Add PIN, Pattern, or Fingerprint as\nbackup methods for extra security\nwhenever needed."
        ),
        OnboardingPageData(
            imageRes = R.drawable.ic_onb3,
            title = "Secure with Voice Notes",
            description = "Create a unique voice note to\nprotect your device with secure\nvoice authentication."
        ),
        OnboardingPageData(
            imageRes = R.drawable.ic_onb4,
            title = "Beautiful Lock\nScreen Themes",
            description = "Choose stylish themes and\nwallpapers to make your lock\nscreen unique."
        )
    )

    val pager = rememberPagerState { pages.size }
    val scope = rememberCoroutineScope()
    val c = MaterialTheme.colorScheme
    val config = LocalConfiguration.current
    val isCompact = config.screenHeightDp < 700
    val artHeight = if (isCompact) 180.dp else 240.dp
    val context = LocalContext.current
    val view = LocalView.current
    var loadingAd by remember { mutableStateOf(false) }

    val dark = when (Store.themeMode) {
        1 -> false
        2 -> true
        else -> isSystemInDarkTheme()
    }

    LaunchedEffect(Unit) {
        // Preload fresh interstitial ad at onboarding screen launch if enabled
        InterstitialAdManager.preload("onboarding_screen", "interstitial")
    }

    SideEffect {
        (view.context as? Activity)?.window?.let { window ->
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !dark
            insetsController.isAppearanceLightNavigationBars = !dark
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(c.background)
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Main onboarding pager
            HorizontalPager(
                state = pager,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { index ->
                val pageData = pages[index]
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Column(
                        modifier = Modifier
                            .widthIn(max = MaxContentWidth)
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(artHeight),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(pageData.imageRes),
                                contentDescription = pageData.title,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                        }

                        Spacer(Modifier.height(16.dp))

                        Text(
                            text = pageData.title,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            color = c.onBackground,
                            lineHeight = 26.sp,
                            fontFamily = InterFontFamily
                        )

                        Spacer(Modifier.height(8.dp))

                        Text(
                            text = pageData.description,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            color = c.onSurfaceVariant,
                            lineHeight = 17.sp,
                            fontFamily = InterFontFamily
                        )
                    }
                }
            }

            // Action row: Dots indicator on left, Next/Started button on right
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Circular Dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(pages.size) { idx ->
                        val selected = idx == pager.currentPage
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (selected) Blue else Color(0xFFD6DBE4))
                        )
                    }
                }

                // Capsule Action Button
                Button(
                    onClick = {
                        if (pager.currentPage == pages.size - 1) {
                            val act = context as? Activity
                            Log.d("ADSMANAGER", "[onboarding_screen][interstitial] Interstitial show triggered on Let's Start click")
                            if (act != null) {
                                InterstitialAdManager.show(
                                    activity = act,
                                    screenKey = "onboarding_screen",
                                    adKey = "interstitial",
                                    onLoading = { loadingAd = it }
                                ) {
                                    Log.d("ADSMANAGER", "[onboarding_screen][interstitial] Interstitial flow completed -> finishing onboarding")
                                    onFinish()
                                }
                            } else {
                                onFinish()
                            }
                        } else {
                            // "Next" button simply scrolls to next page without interstitial
                            scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Blue),
                    shape = RoundedCornerShape(percent = 50),
                    contentPadding = PaddingValues(horizontal = 28.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = if (pager.currentPage == pages.size - 1) "Let's Start" else "Next",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = InterFontFamily,
                        color = Color.White
                    )
                }
            }

            // Native Ad at bottom of the Onboarding screen with same-size Inline Banner fallback
            AppNativeAdView(
                screenKey = "onboarding_screen",
                template = 3,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp)
            )
        }

        AdLoadingDialog(isVisible = loadingAd)
    }
}
