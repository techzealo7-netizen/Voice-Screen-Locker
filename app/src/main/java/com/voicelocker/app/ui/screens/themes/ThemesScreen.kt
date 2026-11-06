package com.voicelocker.app.ui.screens.themes

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.voicelocker.ads.AppAdaptiveBannerTop
import com.voicelocker.ads.AppNativeAdView
import com.voicelocker.ads.BillingManager
import com.voicelocker.ads.RewardedAdManager
import com.voicelocker.app.data.local.Store
import com.voicelocker.app.data.model.lockThemes
import com.voicelocker.app.ui.components.Page
import com.voicelocker.app.ui.components.ThemeGrid
import com.voicelocker.app.ui.components.ThemeUnlockDialog

@Composable
fun ThemesScreen(nav: NavHostController) {
    val ctx = LocalContext.current
    val isPremium = BillingManager.getInstance(ctx).isPremiumUser()
    var unlockDialogThemeIndex by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(Unit) {
        RewardedAdManager.preload()
    }

    Page(
        title = "Themes",
        onBack = { nav.popBackStack() },
        adScreenKey = "theme_screen",
        nativeTemplate = 1
    ) {
        ThemeGrid(lockThemes.indices.toList()) { it ->
            if (Store.isThemeUnlocked(it, isPremium)) {
                nav.navigate("themePreview/$it")
            } else {
                unlockDialogThemeIndex = it
            }
        }
        Spacer(Modifier.height(16.dp))

        unlockDialogThemeIndex?.let { themeIdx ->
            ThemeUnlockDialog(
                themeIndex = themeIdx,
                onDismiss = { unlockDialogThemeIndex = null },
                onUnlocked = {
                    nav.navigate("themePreview/$themeIdx")
                },
                onBuyPremium = {
                    nav.navigate("premium")
                }
            )
        }
    }
}


