package com.voicelocker.ads

import android.app.Activity
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

object AppOpenLifecycleTracker {

    private const val TAG = "AppOpenLifecycle"
    private const val SAFETY_TIMEOUT_MS = 8000L

    private var startedActivityCount = 0
    private var isFirstLaunch = true
    private var isHandlingResume = false

    // ✅ Stays false for the ENTIRE first-launch journey (splash/onboarding/
    // premium/etc). Nothing in that chain — including its own ads — can
    // trigger a resume ad until this is explicitly flipped on.
    @Volatile
    private var isJourneyReady = false

    private val _showWelcomeBack = MutableStateFlow(false)
    val showWelcomeBack: StateFlow<Boolean> = _showWelcomeBack

    private val scope = CoroutineScope(Dispatchers.Main)

    /** Call once, from your home/main screen's first composition. */
    fun markJourneyReady() {
        if (!isJourneyReady) {
            isJourneyReady = true
            Log.d(TAG, "✅ Journey reached main — resume ads now enabled")
        }
    }

    fun onActivityStarted() {
        startedActivityCount++
    }

    fun onActivityStopped() {
        startedActivityCount = (startedActivityCount - 1).coerceAtLeast(0)
    }

    fun onActivityResumed(activity: Activity) {
        // ✅ Ignore resumes caused by our own ad closing (interstitial, app open)
        if (AdActivityGuard.consumeIfShowingAd()) {
            Log.d(TAG, "🎯 Resume caused by our own ad closing — ignoring")
            return
        }
        if (isFirstLaunch) {
            isFirstLaunch = false
            Log.d(TAG, "🚀 Cold launch — Splash owns App Open, skipping")
            return
        }
        // ✅ Whole first-launch journey (splash → onboarding → premium → main)
        // is off-limits until markJourneyReady() has been called once
        if (!isJourneyReady) {
            Log.d(TAG, "⏳ Journey not yet complete — skipping resume ad")
            return
        }
        if (isHandlingResume) return
        if (startedActivityCount != 1) return

        maybeShowResumeAd(activity)
    }

    private fun maybeShowResumeAd(activity: Activity) {
        if (!AppOpenAdManager.shouldShowOnResume()) {
            Log.d(TAG, "🔇 Resume ad suppressed (e.g. locale/recreate-triggered resume)")
            return
        }
        if (BillingManager.getInstance(activity).isPremiumUser()) {
            Log.d(TAG, "👑 Premium user — skipping resume ad")
            return
        }
        if (!AdManager.isAppOpenResumeEnabled()) {          // ✅ was isAppOpenEnabled()
            Log.d(TAG, "⚠️ App Open Resume disabled in Remote Config — skipping")
            return
        }

        val cooldownHours = RemoteConfigManager.getAppOpenResumeCooldownHours()
        val cooldownMs = cooldownHours * 60 * 60 * 1000L
        val elapsed = System.currentTimeMillis() - com.voicelocker.app.data.local.Store.lastResumeAdTime
        if (com.voicelocker.app.data.local.Store.lastResumeAdTime > 0L && elapsed < cooldownMs) {
            val remainingMins = (cooldownMs - elapsed) / 60000L
            Log.d(TAG, "⏳ App Open Resume in cooldown: ${remainingMins}m remaining (cooldown=${cooldownHours}h)")
            return
        }

        val adUnitId = AdManager.getAppOpenResumeAdId()      // ✅ new — fetch resume-specific ID
        if (adUnitId == null) {
            Log.d(TAG, "⚠️ No valid App Open Resume ID — skipping")
            return
        }

        Log.d(TAG, "🔄 Starting welcome-back flow")
        isHandlingResume = true
        _showWelcomeBack.value = true

        var responded = false

        AppOpenAdManager.loadAd(
            context = activity,
            adUnitId = adUnitId,
            onLoaded = {
                responded = true
                Log.d(TAG, "✅ Ad loaded — presenting ad while keeping loading overlay visible")

                AppOpenAdManager.showAdIfAvailable(activity) {
                    Log.d(TAG, "✅ Ad dismissed or completed — resume flow complete")
                    com.voicelocker.app.data.local.Store.lastResumeAdTime = System.currentTimeMillis()
                    _showWelcomeBack.value = false
                    isHandlingResume = false
                }
            },
            onFailed = {
                responded = true
                Log.d(TAG, "❌ Ad failed to load — hiding welcome back")
                _showWelcomeBack.value = false
                isHandlingResume = false
            }
        )

        scope.launch {
            delay(SAFETY_TIMEOUT_MS)
            if (!responded) {
                Log.d(TAG, "⏰ Resume ad load timed out — force hiding welcome back")
                _showWelcomeBack.value = false
                isHandlingResume = false
            }
        }
    }
}