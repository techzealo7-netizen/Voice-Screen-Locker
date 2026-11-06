package com.voicelocker.app.ui.screens.themes

import android.app.Activity
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.voicelocker.ads.AdLoadingDialog
import com.voicelocker.ads.AdManager
import com.voicelocker.ads.BillingManager
import com.voicelocker.ads.InterstitialAdManager
import com.voicelocker.app.R
import com.voicelocker.app.data.local.Store
import com.voicelocker.app.data.model.lockThemes
import com.voicelocker.app.ui.components.Page
import com.voicelocker.app.ui.components.SuccessDialog
import com.voicelocker.app.ui.components.ThemeBg
import com.voicelocker.app.ui.components.ThemeUnlockDialog
import com.voicelocker.app.ui.navigation.NavRoutes
import com.voicelocker.app.ui.theme.Blue
import com.voicelocker.app.ui.theme.InterFontFamily
import com.voicelocker.app.util.toast

@Composable
fun ThemePreviewScreen(nav: NavHostController, i: Int) {
    val ctx = LocalContext.current
    val act = ctx as? Activity
    var loadingAd by remember { mutableStateOf(false) }
    val isPremium = BillingManager.getInstance(ctx).isPremiumUser()
    val isUnlocked = Store.isThemeUnlocked(i, isPremium)
    var showUnlockDialog by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        InterstitialAdManager.preload("theme_screen", "inter_apply")
    }

    val navigateToHome: () -> Unit = {
        val popped = nav.popBackStack(NavRoutes.MAIN, false)
        if (!popped) {
            nav.navigate(NavRoutes.MAIN) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    val applyThemeAndShowSuccess: () -> Unit = {
        Store.themeIndex = i
        showSuccessDialog = true
    }

    Page(
        title = "Themes",
        onBack = { nav.popBackStack() },
        scroll = false,
        adScreenKey = "theme_screen",
        nativeTemplate = 1,
        bottom = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Preview Button with ic_preview icon and blue border/text
                Button(
                    onClick = { nav.navigate("lockPreview/$i") },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.2.dp, Blue),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Blue
                    )
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_preview),
                        contentDescription = "Preview",
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Preview",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = InterFontFamily,
                        color = Blue
                    )
                }

                // Apply Button with solid blue fill and white text
                Button(
                    onClick = {
                        if (!isUnlocked) {
                            showUnlockDialog = true
                        } else {
                            Log.d("ADSMANAGER", "[theme_screen][inter_apply] Interstitial show triggered on Apply click")
                            if (act != null) {
                                InterstitialAdManager.show(
                                    activity = act,
                                    screenKey = "theme_screen",
                                    adKey = "inter_apply",
                                    onLoading = { loadingAd = it }
                                ) {
                                    Log.d("ADSMANAGER", "[theme_screen][inter_apply] Interstitial flow completed -> applying theme")
                                    applyThemeAndShowSuccess()
                                }
                            } else {
                                applyThemeAndShowSuccess()
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Blue,
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = if (isUnlocked) "Apply" else "Unlock & Apply",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = InterFontFamily,
                        color = Color.White
                    )
                }
            }
        }
    ) {
        ThemeBg(
            i = i,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(bottom = 4.dp),
            shape = RoundedCornerShape(16.dp)
        )
    }

    if (showUnlockDialog) {
        ThemeUnlockDialog(
            themeIndex = i,
            onDismiss = { showUnlockDialog = false },
            onUnlocked = {
                showUnlockDialog = false
                applyThemeAndShowSuccess()
            },
            onBuyPremium = {
                nav.navigate("premium")
            }
        )
    }

    if (showSuccessDialog) {
        SuccessDialog(
            msg = "${lockThemes[i].name} applied successfully.",
            onDone = {
                showSuccessDialog = false
                navigateToHome()
            }
        )
    }

    AdLoadingDialog(isVisible = loadingAd)
}
