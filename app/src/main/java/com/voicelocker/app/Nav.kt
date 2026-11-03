package com.voicelocker.app

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

object UiBus { var success by mutableStateOf<String?>(null) }

/**
 * Setup flows. voice: perms -> record -> security? -> pin? ; pin/pattern/fp: setup -> security? ; edit: security.
 * "from" is the step that just finished.
 */
fun nextRoute(flow: String, from: String): String? {
    val sec = if (!Store.hasSecurity) "security/$flow" else null
    val pin = if (flow == "voice" && !Store.hasPin) "pin/voice" else null
    return when (from) { "perms" -> "record"; "record" -> sec ?: pin; "setup" -> sec; "security" -> pin; else -> null }
}

fun NavHostController.go(flow: String, from: String) {
    val n = nextRoute(flow, from)
    if (n != null) { navigate(n); return }
    if (flow == "voice") Store.voiceEnabled = true
    popBackStack(if (flow == "voice") "voiceIntro" else "main", false)
    UiBus.success = when (flow) {
        "voice" -> "Your Voice has been set successfully."
        "pin" -> "Your PIN has been set successfully."
        "pattern" -> "Your Pattern has been set successfully."
        "fp" -> "Your Fingerprint has been set successfully."
        else -> "Your security question has been saved."
    }
}

@Composable
fun AppRoot() {
    val nav = rememberNavController()
    val ctx = LocalContext.current
    LaunchedEffect(Store.lockEnabled) { LockService.sync(ctx) }
    NavHost(nav, startDestination = "splash") {
        composable("splash") { SplashScreen { nav.navigate(if (Store.onboarded) "main" else "onboarding") { popUpTo("splash") { inclusive = true } } } }
        composable("onboarding") { OnboardingScreen { Store.onboarded = true; nav.navigate("main") { popUpTo("onboarding") { inclusive = true } } } }
        composable("main") { MainScreen(nav) }
        composable("voiceIntro") { VoiceIntroScreen(nav) }
        composable("perms") { PermissionsScreen(nav) }
        composable("record") { RecordScreen(nav) }
        composable("security/{flow}") { SecurityScreen(nav, it.arguments?.getString("flow") ?: "edit") }
        composable("pin/{flow}") { PinScreen(nav, it.arguments?.getString("flow") ?: "pin") }
        composable("pattern") { PatternScreen(nav) }
        composable("fingerprint") { FingerprintScreen(nav) }
        composable("themes") { ThemesScreen(nav) }
        composable("themePreview/{i}", listOf(navArgument("i") { type = NavType.IntType })) { ThemePreviewScreen(nav, it.arguments?.getInt("i") ?: 0) }
        composable("lockPreview/{i}", listOf(navArgument("i") { type = NavType.IntType })) { e ->
            val i = e.arguments?.getInt("i") ?: 0
            LockScreen(i, preview = true, onBack = { nav.popBackStack() }, onApply = { Store.themeIndex = i; ctx.toast("${lockThemes[i].name} applied"); nav.popBackStack() })
        }
        composable("testLock") { LockScreen(Store.themeIndex, onBack = { nav.popBackStack() }, onUnlock = { nav.popBackStack() }) }
        composable("noteEdit?id={id}", listOf(navArgument("id") { type = NavType.StringType; nullable = true; defaultValue = null })) { NoteEditorScreen(nav, it.arguments?.getString("id")) }
        composable("note/{id}?tab={tab}", listOf(navArgument("id") { type = NavType.StringType }, navArgument("tab") { type = NavType.IntType; defaultValue = 0 })) {
            NoteDetailScreen(nav, it.arguments?.getString("id") ?: "", it.arguments?.getInt("tab") ?: 0)
        }
        composable("premium") { PremiumScreen(nav) }
    }
    UiBus.success?.let { SuccessDialog(it) { UiBus.success = null } }
}
