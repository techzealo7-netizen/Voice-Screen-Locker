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

    private const val TAG = "ADSMANAGER"
    private const val SAFETY_TIMEOUT_MS = 8000L

    private var startedActivityCount = 0
    private var isFirstLaunch = true
    private var isHandlingResume = false

    // ✅ Stays false for the ENTIRE first-launch journey (splash/onboarding/
    // premium/etc). Nothing in that chain — including its own ads — can
    // trigger a resume ad until this is explicitly flipped on.
    @Volatile
    private var isJourneyReady = false

    private val _isAdLoading = MutableStateFlow(false)
    val isAdLoading: StateFlow<Boolean> = _isAdLoading
    val showWelcomeBack: StateFlow<Boolean> get() = _isAdLoading

    private val scope = CoroutineScope(Dispatchers.Main)

    @Volatile
    var isSplashScreenActive: Boolean = true

    /** Call once, from your home/main screen's first composition. */
    fun markJourneyReady() {
        isSplashScreenActive = false
        if (!isJourneyReady) {
            isJourneyReady = true
            Log.d(TAG, "✅ [app_open_resume] Journey reached main — resume ads now enabled")
        }
    }

    fun onActivityStarted() {
        startedActivityCount++
    }

    fun onActivityStopped() {
        startedActivityCount = (startedActivityCount - 1).coerceAtLeast(0)
    }

    fun onActivityResumed(activity: Activity) {
        val activityName = activity.javaClass.name
        // 1. Ignore if resumed activity is full screen AdActivity (AdMob fullscreen)
        if (activityName.contains("AdActivity", ignoreCase = true)) {
            Log.d(TAG, "🎯 [app_open_resume] Full screen AdActivity ($activityName) resumed — skipping resume ad")
            return
        }

        // 2. Ignore if lock activity
        if (activity.javaClass.simpleName == "LockActivity") {
            Log.d(TAG, "🔒 [app_open_resume] LockActivity resumed — skipping resume ad")
            return
        }

        // 3. NEVER show if splash screen is currently active
        if (isSplashScreenActive) {
            Log.d(TAG, "🚫 [app_open_resume] SplashScreen is active — skipping resume ad")
            return
        }

        // 4. Ignore resumes caused by ANY full screen ad showing or just closed (interstitial, rewarded, app open)
        if (AdActivityGuard.isAdActiveOrJustDismissed() || AdActivityGuard.consumeIfShowingAd()) {
            Log.d(TAG, "🎯 [app_open_resume] Resume caused by full screen ad — skipping resume ad")
            return
        }

        if (isFirstLaunch) {
            isFirstLaunch = false
            Log.d(TAG, "🚀 [app_open_resume] Cold launch — Splash owns App Open, skipping")
            return
        }

        // 5. Whole first-launch journey (splash → onboarding → premium → main)
        // is off-limits until markJourneyReady() has been called once
        if (!isJourneyReady) {
            Log.d(TAG, "⏳ [app_open_resume] Journey not yet complete — skipping resume ad")
            return
        }
        if (isHandlingResume) return
        if (startedActivityCount != 1) return

        maybeShowResumeAd(activity)
    }

    private fun maybeShowResumeAd(activity: Activity) {
        if (!AppOpenAdManager.shouldShowOnResume()) {
            Log.d(TAG, "🔇 [app_open_resume] Suppressed (e.g. locale/permission/settings-triggered resume)")
            return
        }
        if (BillingManager.getInstance(activity).isPremiumUser()) {
            Log.d(TAG, "👑 [app_open_resume] Premium user — skipping resume ad")
            return
        }
        if (!AdManager.isAppOpenResumeEnabled()) {
            Log.d(TAG, "⚠️ [app_open_resume] Disabled in Remote Config — skipping")
            return
        }

        val cooldownHours = RemoteConfigManager.getAppOpenResumeCooldownHours().coerceAtLeast(1)
        val cooldownMs = cooldownHours * 60 * 60 * 1000L
        val elapsed = System.currentTimeMillis() - com.voicelocker.app.data.local.Store.lastResumeAdTime
        if (com.voicelocker.app.data.local.Store.lastResumeAdTime > 0L && elapsed < cooldownMs) {
            val remainingMins = (cooldownMs - elapsed) / 60000L
            val remH = remainingMins / 60
            val remM = remainingMins % 60
            Log.d(TAG, "⏳ [app_open_resume] In cooldown: ${remH}h ${remM}m remaining (cooldown=${cooldownHours}h) -> skipping resume ad")
            return
        }

        val adUnitId = AdManager.getAppOpenResumeAdId()
        if (adUnitId == null) {
            Log.d(TAG, "⚠️ [app_open_resume] No valid resume ad ID — skipping")
            return
        }

        Log.d(TAG, "🔄 [app_open_resume] Returning from background -> showing 'Ad is loading...' loader")
        isHandlingResume = true
        _isAdLoading.value = true

        var responded = false

        AppOpenAdManager.loadAd(
            context = activity,
            adUnitId = adUnitId,
            onLoaded = {
                responded = true
                Log.d(TAG, "✅ [app_open_resume] Ad loaded successfully -> presenting ad")

                AppOpenAdManager.showAdIfAvailable(
                    activity = activity,
                    onShow = {
                        Log.d(TAG, "🎉 [app_open_resume] Ad displayed on screen -> dismissing loader")
                        _isAdLoading.value = false
                    },
                    onComplete = {
                        com.voicelocker.app.data.local.Store.lastResumeAdTime = System.currentTimeMillis()
                        Log.d(TAG, "👋 [app_open_resume] Ad dismissed -> Cooldown reset to ${cooldownHours}h")
                        _isAdLoading.value = false
                        isHandlingResume = false
                    }
                )
            },
            onFailed = {
                responded = true
                Log.d(TAG, "❌ [app_open_resume] Ad failed to load -> dismissing loader")
                _isAdLoading.value = false
                isHandlingResume = false
            }
        )

        scope.launch {
            delay(SAFETY_TIMEOUT_MS)
            if (!responded) {
                Log.d(TAG, "⏰ [app_open_resume] Load timed out after ${SAFETY_TIMEOUT_MS}ms -> force dismissing loader")
                _isAdLoading.value = false
                isHandlingResume = false
            }
        }
    }
}