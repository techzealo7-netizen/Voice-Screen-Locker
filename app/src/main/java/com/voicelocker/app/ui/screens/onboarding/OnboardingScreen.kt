package com.voicelocker.app.ui.screens.onboarding

import android.app.Activity
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.voicelocker.app.R
import com.voicelocker.app.data.local.Store
import com.voicelocker.app.ui.components.MaxContentWidth
import com.voicelocker.app.ui.theme.Blue
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
    val isCompact = config.screenHeightDp < 680
    val artHeight = if (isCompact) 250.dp else 320.dp
    val view = LocalView.current

    val dark = when (Store.themeMode) {
        1 -> false
        2 -> true
        else -> isSystemInDarkTheme()
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

    Column(
        Modifier
            .fillMaxSize()
            .background(c.background)
            .systemBarsPadding()
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

                    Spacer(Modifier.height(24.dp))

                    Text(
                        text = pageData.title,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = c.onBackground,
                        lineHeight = 28.sp
                    )

                    Spacer(Modifier.height(10.dp))

                    Text(
                        text = pageData.description,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        color = c.onSurfaceVariant,
                        lineHeight = 17.sp
                    )
                }
            }
        }

        // Bottom row: Dots indicator on left, Next/Started button on right
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 24.dp),
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
                    if (pager.currentPage == pages.size - 1) onFinish()
                    else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Blue),
                shape = RoundedCornerShape(percent = 50),
                contentPadding = PaddingValues(horizontal = 28.dp, vertical = 10.dp)
            ) {
                Text(
                    text = if (pager.currentPage == pages.size - 1) "Started" else "Next",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
            }
        }
    }
}
