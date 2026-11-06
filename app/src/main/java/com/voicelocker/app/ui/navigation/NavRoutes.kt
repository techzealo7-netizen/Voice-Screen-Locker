package com.voicelocker.app.ui.navigation

import androidx.navigation.NavHostController
import com.voicelocker.app.data.local.Store

object NavRoutes {
    const val SPLASH = "splash"
    const val LANGUAGE = "language"
    const val ONBOARDING = "onboarding"
    const val MAIN = "main"
    const val VOICE_INTRO = "voiceIntro"
    const val PERMS = "perms"
    const val RECORD = "record"
    const val SECURITY = "security/{flow}"
    const val PIN = "pin/{flow}"
    const val PATTERN = "pattern"
    const val FINGERPRINT = "fingerprint"
    const val THEMES = "themes"
    const val THEME_PREVIEW = "themePreview/{i}"
    const val LOCK_PREVIEW = "lockPreview/{i}"
    const val TEST_LOCK = "testLock"
    const val NOTE_EDIT = "noteEdit?id={id}"
    const val NOTE_DETAIL = "note/{id}?tab={tab}"
    const val PREMIUM = "premium?from={from}"

    fun premium(from: String = "inapp") = "premium?from=$from"
}

/**
 * Setup flows. voice: perms -> record -> security? -> pin? ; pin/pattern/fp: setup -> security? ; edit: security.
 * "from" is the step that just finished.
 */
fun nextRoute(flow: String, from: String): String? {
    val sec = if (!Store.hasSecurity) "security/$flow" else null
    val pin = if (flow == "voice" && !Store.hasPin) "pin/voice" else null
    return when (from) {
        "perms" -> "record"
        "record" -> sec ?: pin
        "setup" -> sec
        "security" -> pin
        else -> null
    }
}

fun NavHostController.go(flow: String, from: String) {
    val n = nextRoute(flow, from)
    if (n != null) {
        navigate(n)
        return
    }
    if (flow == "voice") Store.voiceEnabled = true
    val popped = popBackStack(NavRoutes.MAIN, false)
    if (!popped) {
        navigate(NavRoutes.MAIN) {
            popUpTo(0) { inclusive = true }
        }
    }
    UiBus.success = when (flow) {
        "voice" -> "Your Voice has been set successfully."
        "pin" -> "Your PIN has been set successfully."
        "pattern" -> "Your Pattern has been set successfully."
        "fp" -> "Your Fingerprint has been set successfully."
        else -> "Your security question has been saved."
    }
    UiBus.onDismissSuccess = {
        navigate(NavRoutes.THEMES)
    }
}

