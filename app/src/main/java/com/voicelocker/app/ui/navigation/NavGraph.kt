package com.voicelocker.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.voicelocker.ads.AppOpenBlackOverlay
import com.voicelocker.ads.BillingManager
import com.voicelocker.ads.RemoteConfigManager
import com.voicelocker.app.data.local.Store
import com.voicelocker.app.data.model.lockThemes
import com.voicelocker.app.service.LockService
import com.voicelocker.app.ui.components.SuccessDialog
import com.voicelocker.app.ui.screens.home.MainScreen
import com.voicelocker.app.ui.screens.language.LanguageScreen
import com.voicelocker.app.ui.screens.lock.LockScreen
import com.voicelocker.app.ui.screens.notes.NoteDetailScreen
import com.voicelocker.app.ui.screens.notes.NoteEditorScreen
import com.voicelocker.app.ui.screens.onboarding.OnboardingScreen
import com.voicelocker.app.ui.screens.onboarding.SplashScreen
import com.voicelocker.app.ui.screens.premium.PremiumScreen
import com.voicelocker.app.ui.screens.security.FingerprintScreen
import com.voicelocker.app.ui.screens.security.PatternScreen
import com.voicelocker.app.ui.screens.security.PinScreen
import com.voicelocker.app.ui.screens.security.SecurityScreen
import com.voicelocker.app.ui.screens.themes.ThemePreviewScreen
import com.voicelocker.app.ui.screens.themes.ThemesScreen
import com.voicelocker.app.ui.screens.voice.PermissionsScreen
import com.voicelocker.app.ui.screens.voice.RecordScreen
import com.voicelocker.app.ui.screens.voice.VoiceIntroScreen
import com.voicelocker.app.util.toast

@Composable
fun AppRoot() {
    val nav = rememberNavController()
    val ctx = LocalContext.current
    LaunchedEffect(Store.lockEnabled) {
        LockService.sync(ctx)
    }

    NavHost(nav, startDestination = NavRoutes.SPLASH) {
        composable(NavRoutes.SPLASH) {
            SplashScreen {
                if (!Store.onboarded) {
                    // Flow 1: splash -> language -> onb -> premium screen -> home
                    nav.navigate(NavRoutes.LANGUAGE) {
                        popUpTo(NavRoutes.SPLASH) { inclusive = true }
                    }
                } else {
                    // Flow 2: splash -> inapp screen -> Home
                    val isPremium = BillingManager.getInstance(ctx).isPremiumUser()
                    val inAppEnabled = RemoteConfigManager.isInAppScreenEnabled()
                    if (!isPremium && inAppEnabled) {
                        nav.navigate(NavRoutes.premium("splash")) {
                            popUpTo(NavRoutes.SPLASH) { inclusive = true }
                        }
                    } else {
                        nav.navigate(NavRoutes.MAIN) {
                            popUpTo(NavRoutes.SPLASH) { inclusive = true }
                        }
                    }
                }
            }
        }
        composable(NavRoutes.LANGUAGE) {
            LanguageScreen(
                onBack = null,
                onConfirm = {
                    nav.navigate(NavRoutes.ONBOARDING) {
                        popUpTo(NavRoutes.LANGUAGE) { inclusive = true }
                    }
                }
            )
        }
        composable(NavRoutes.ONBOARDING) {
            OnboardingScreen {
                nav.navigate(NavRoutes.premium("onboarding")) {
                    popUpTo(NavRoutes.ONBOARDING) { inclusive = true }
                }
            }
        }
        composable(NavRoutes.MAIN) {
            MainScreen(nav)
        }
        composable(NavRoutes.VOICE_INTRO) {
            VoiceIntroScreen(nav)
        }
        composable(NavRoutes.PERMS) {
            PermissionsScreen(nav)
        }
        composable(NavRoutes.RECORD) {
            RecordScreen(nav)
        }
        composable(NavRoutes.SECURITY) {
            SecurityScreen(nav, it.arguments?.getString("flow") ?: "edit")
        }
        composable(NavRoutes.PIN) {
            PinScreen(nav, it.arguments?.getString("flow") ?: "pin")
        }
        composable(NavRoutes.PATTERN) {
            PatternScreen(nav)
        }
        composable(NavRoutes.FINGERPRINT) {
            FingerprintScreen(nav)
        }
        composable(NavRoutes.THEMES) {
            ThemesScreen(nav)
        }
        composable(
            NavRoutes.THEME_PREVIEW,
            listOf(navArgument("i") { type = NavType.IntType })
        ) {
            ThemePreviewScreen(nav, it.arguments?.getInt("i") ?: 0)
        }
        composable(
            NavRoutes.LOCK_PREVIEW,
            listOf(navArgument("i") { type = NavType.IntType })
        ) { e ->
            val i = e.arguments?.getInt("i") ?: 0
            LockScreen(
                themeIndex = i,
                preview = true,
                onBack = { nav.popBackStack() },
                onApply = {
                    Store.themeIndex = i
                    UiBus.success = "${lockThemes[i].name} applied successfully."
                    val popped = nav.popBackStack(NavRoutes.MAIN, false)
                    if (!popped) {
                        nav.navigate(NavRoutes.MAIN) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                }
            )
        }
        composable(NavRoutes.TEST_LOCK) {
            LockScreen(
                themeIndex = Store.themeIndex,
                onBack = { nav.popBackStack() },
                onUnlock = { nav.popBackStack() }
            )
        }
        composable(
            NavRoutes.NOTE_EDIT,
            listOf(navArgument("id") {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            })
        ) {
            NoteEditorScreen(nav, it.arguments?.getString("id"))
        }
        composable(
            NavRoutes.NOTE_DETAIL,
            listOf(
                navArgument("id") { type = NavType.StringType },
                navArgument("tab") {
                    type = NavType.IntType
                    defaultValue = 0
                }
            )
        ) {
            NoteDetailScreen(
                nav = nav,
                id = it.arguments?.getString("id") ?: "",
                initialTab = it.arguments?.getInt("tab") ?: 0
            )
        }
        composable(
            route = NavRoutes.PREMIUM,
            arguments = listOf(
                navArgument("from") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = "inapp"
                }
            )
        ) { entry ->
            val from = entry.arguments?.getString("from") ?: "inapp"
            PremiumScreen(nav = nav, from = from)
        }
    }
    UiBus.success?.let {
        SuccessDialog(it) {
            UiBus.success = null
            val onDismiss = UiBus.onDismissSuccess
            UiBus.onDismissSuccess = null
            onDismiss?.invoke()
        }
    }
    AppOpenBlackOverlay()
}

