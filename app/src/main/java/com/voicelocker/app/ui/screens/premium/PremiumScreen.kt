package com.voicelocker.app.ui.screens.premium

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.voicelocker.ads.AdLoadingDialog
import com.voicelocker.ads.AdManager
import com.voicelocker.ads.InterstitialAdManager
import com.voicelocker.app.R
import com.voicelocker.app.data.local.Store
import com.voicelocker.app.ui.navigation.NavRoutes
import com.voicelocker.app.ui.theme.Blue
import com.voicelocker.app.ui.theme.InterFontFamily
import com.voicelocker.app.util.toast
import kotlinx.coroutines.delay

private data class PremiumPlan(
    val title: String,
    val price: String
)

@Composable
fun PremiumScreen(
    nav: NavHostController,
    from: String? = null
) {
    val ctx = LocalContext.current
    val activity = ctx as? Activity
    val c = MaterialTheme.colorScheme
    val isDark = c.background == Color(0xFF0E1320)

    var selectedPlanIndex by remember { mutableIntStateOf(1) } // Default to Monthly (1)
    var loadingAd by remember { mutableStateOf(false) }

    var startAnimation by remember { mutableStateOf(false) }
    var isTimerComplete by remember { mutableStateOf(false) }
    var remainingSeconds by remember { mutableIntStateOf(3) }

    val progressAnim by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 3000, easing = LinearEasing),
        label = "circular_timer_progress"
    )

    LaunchedEffect(Unit) {
        startAnimation = true
        for (sec in 3 downTo 1) {
            remainingSeconds = sec
            delay(1000L)
        }
        isTimerComplete = true
    }

    val plans = listOf(
        PremiumPlan("Weekly", "Rs 500/wk"),
        PremiumPlan("Monthly", "Rs 1000/mo"),
        PremiumPlan("Yearly", "Rs 1500/yr")
    )

    val features = listOf(
        "Ai Voice Lock",
        "Unlock All Feature",
        "Themes",
        "Remove Ads"
    )

    LaunchedEffect(Unit) {
        InterstitialAdManager.preload("in_app_screen", "interstitial")
    }

    val proceedToDestination: () -> Unit = {
        if (from == "onboarding" || from == "splash") {
            Store.onboarded = true
            nav.navigate(NavRoutes.MAIN) {
                popUpTo(0) { inclusive = true }
            }
        } else {
            nav.popBackStack()
        }
    }

    val handleDismiss: () -> Unit = {
        if (activity != null) {
            InterstitialAdManager.show(
                activity = activity,
                screenKey = "in_app_screen",
                adKey = "interstitial",
                onLoading = { loadingAd = it }
            ) {
                proceedToDestination()
            }
        } else {
            proceedToDestination()
        }
    }

    BackHandler {
        if (isTimerComplete) {
            handleDismiss()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(c.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Top Bar with right corner 3s circular timer & close cross
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                if (isTimerComplete) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0))
                            .clickable { handleDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close",
                            tint = c.onBackground,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier.size(34.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            progress = { progressAnim },
                            modifier = Modifier.size(34.dp),
                            color = Blue,
                            trackColor = if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0),
                            strokeWidth = 2.5.dp
                        )
                        Text(
                            text = "$remainingSeconds",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = InterFontFamily,
                            color = c.onBackground
                        )
                    }
                }
            }

            // Scrollable Content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Crown Illustration
                Image(
                    painter = painterResource(R.drawable.ic_premium),
                    contentDescription = "Premium",
                    modifier = Modifier.size(72.dp)
                )

                Spacer(Modifier.height(10.dp))

                // Title & Subtitle
                Text(
                    text = "Go Premium",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = InterFontFamily,
                    color = Blue
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = "Unlock All Feature",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = InterFontFamily,
                    color = c.onBackground
                )

                Spacer(Modifier.height(24.dp))

                // Feature List with Stars (same as rating dialog)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    features.forEach { feature ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Image(
                                painter = painterResource(R.drawable.ic_star),
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = feature,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                fontFamily = InterFontFamily,
                                color = c.onSurface
                            )
                        }
                    }
                }

                Spacer(Modifier.height(28.dp))

                // Plan Cards
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    plans.forEachIndexed { index, plan ->
                        val isSelected = selectedPlanIndex == index
                        val cardBg = if (isSelected) {
                            if (isDark) Color(0xFF1D3A60) else Color(0xFFE2F0FD)
                        } else {
                            c.surface
                        }
                        val cardBorder = if (isSelected) {
                            BorderStroke(1.5.dp, Blue)
                        } else {
                            BorderStroke(1.dp, c.outline)
                        }

                        Surface(
                            onClick = { selectedPlanIndex = index },
                            shape = RoundedCornerShape(16.dp),
                            color = cardBg,
                            border = cardBorder,
                            shadowElevation = if (isSelected) 0.dp else 1.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 68.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 18.dp, vertical = 22.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Radio circle
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) Blue else Color(0xFFD1D5DB))
                                )

                                Spacer(Modifier.width(14.dp))

                                Text(
                                    text = plan.title,
                                    fontSize = 16.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    fontFamily = InterFontFamily,
                                    color = c.onSurface,
                                    modifier = Modifier.weight(1f)
                                )

                                Text(
                                    text = plan.price,
                                    fontSize = 16.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    fontFamily = InterFontFamily,
                                    color = if (isDark) Color(0xFF8CC4FF) else Color(0xFF334155)
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                // Action Button: Start 3 days free trial
                Button(
                    onClick = {
                        ctx.toast("Connect Google Play Billing to enable purchases")
                        proceedToDestination()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Blue),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                ) {
                    Text(
                        text = "Start 3 days free trial",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = InterFontFamily,
                        color = Color.White
                    )
                }

                Spacer(Modifier.height(16.dp))

                // Centered Footer Links: Privacy Policy & Cancel Subscription (Terms of use removed)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Privacy Policy",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8),
                        fontFamily = InterFontFamily,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.clickable {
                            val i = Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com/privacy"))
                            runCatching { ctx.startActivity(i) }
                        }
                    )
                    Spacer(Modifier.width(16.dp))
                    Text(
                        text = "•",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8)
                    )
                    Spacer(Modifier.width(16.dp))
                    Text(
                        text = "Cancel Subscription",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8),
                        fontFamily = InterFontFamily,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.clickable { handleDismiss() }
                    )
                }
            }
        }

        AdLoadingDialog(isVisible = loadingAd)
    }
}

