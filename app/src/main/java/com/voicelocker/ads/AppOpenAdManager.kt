package com.voicelocker.ads

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.android.libraries.ads.mobile.sdk.appopen.AppOpenAd
import com.google.android.libraries.ads.mobile.sdk.appopen.AppOpenAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

object AppOpenAdManager {
    private const val TAG = "ADSMANAGER"
    private const val AD_EXPIRE_TIME = 4 * 60 * 60 * 1000L
    private var appOpenAd: AppOpenAd? = null
    @Volatile
    private var suppressNextResume = false
    @Volatile
    private var suppressUntilTimestamp: Long = 0L
    @Volatile
    private var isSettingsScreenActive = false
    @Volatile
    private var isPermissionRequestActive = false

    fun suppressAppOpenFor(millis: Long = 5000L) {
        val target = System.currentTimeMillis() + millis
        if (target > suppressUntilTimestamp) {
            suppressUntilTimestamp = target
        }
        suppressNextResume = true
        Log.d(TAG, "🔇 App Open suppressed for ${millis}ms")
    }

    fun suppressNextAppOpen() {
        suppressNextResume = true
    }

    fun setSettingsScreenActive(active: Boolean) {
        isSettingsScreenActive = active
        Log.d(TAG, "Settings screen active state set to: $active")
        if (active) {
            suppressAppOpenFor(10000L)
        }
    }

    fun isSettingsScreenActive(): Boolean = isSettingsScreenActive

    fun setPermissionRequestActive(active: Boolean) {
        isPermissionRequestActive = active
        Log.d(TAG, "Permission request active state set to: $active")
        if (active) {
            suppressAppOpenFor(60000L)
        }
    }

    fun isPermissionRequestActive(): Boolean = isPermissionRequestActive

    private var loadTime: Long = 0L

    @Volatile
    private var isLoading = false

    @Volatile
    private var isShowing = false

    private val _isOverlayVisible = MutableStateFlow(false)
    val isOverlayVisible: StateFlow<Boolean> = _isOverlayVisible
    val isBlackOverlayVisible: StateFlow<Boolean> get() = _isOverlayVisible

    private var overlaySafetyJob: Job? = null

    fun showOverlay() {
        overlaySafetyJob?.cancel()
        _isOverlayVisible.value = true
        overlaySafetyJob = CoroutineScope(Dispatchers.Main).launch {
            delay(15000L)
            _isOverlayVisible.value = false
        }
    }

    fun hideOverlay() {
        overlaySafetyJob?.cancel()
        _isOverlayVisible.value = false
    }

    private val _isAppOpenActive = MutableStateFlow(false)
    val isAppOpenActive: StateFlow<Boolean> = _isAppOpenActive

    // ✅ NEW — guards against the flag getting stuck forever if a caller
    // never follows through with showAdIfAvailable (e.g. Splash already
    // navigated away by the time the ad finishes loading).
    private var activeResetJob: Job? = null

    private fun markActive() {
        activeResetJob?.cancel()
        _isAppOpenActive.value = true
        activeResetJob = CoroutineScope(Dispatchers.Main).launch {
            delay(8000)
            Log.d(TAG, "⏰ App Open active flag safety reset — show never happened")
            _isAppOpenActive.value = false
        }
    }

    private fun markInactive() {
        activeResetJob?.cancel()
        _isAppOpenActive.value = false
    }

    /** Call explicitly if you decide NOT to show a loaded ad after all (e.g. already navigated away). */
    fun cancelPendingShow() {
        markInactive()
    }

    fun shouldShowOnResume(): Boolean {
        if (System.currentTimeMillis() < suppressUntilTimestamp) {
            Log.d(TAG, "🔇 Resume ad suppressed — within suppression time window")
            return false
        }
        if (isSettingsScreenActive) {
            Log.d(TAG, "🔇 Resume ad suppressed — user is on Settings screen")
            suppressNextResume = false
            return false
        }
        if (isPermissionRequestActive) {
            Log.d(TAG, "🔇 Resume ad suppressed — permission request active")
            suppressNextResume = false
            return false
        }
        if (suppressNextResume) {
            suppressNextResume = false
            return false
        }
        return true
    }

    fun loadAd(
        context: Context,
        adUnitId: String,
        onLoaded: (() -> Unit)? = null,
        onFailed: (() -> Unit)? = null
    ) {
        if (BillingManager.getInstance(context).isPremiumUser()) {
            Log.d(TAG, "👑 Premium user — skipping App Open ad load")
            onFailed?.invoke()
            return
        }
        if (isLoading) return
        if (isAdAvailable()) {
            markActive()   // ✅ FIX — was missing on this cached-ad fast path
            onLoaded?.invoke()
            return
        }

        isLoading = true
        markActive()   // ✅ true + safety timer armed together

        CoroutineScope(Dispatchers.Main).launch {
            AdsInitializer.awaitReady()

            Log.d(TAG, "📡 [AppOpen] Request sent: adUnitId=$adUnitId")
            val request = AdRequest.Builder(adUnitId).build()

            AppOpenAd.load(
                request,
                object : AdLoadCallback<AppOpenAd> {
                    override fun onAdLoaded(ad: AppOpenAd) {
                        Log.d(TAG, "✅ [AppOpen] Ad loaded successfully: adUnitId=$adUnitId")
                        appOpenAd = ad
                        loadTime = System.currentTimeMillis()
                        isLoading = false
                        Handler(Looper.getMainLooper()).post { onLoaded?.invoke() }
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        Log.e(TAG, "❌ [AppOpen] Failed to load: adUnitId=$adUnitId, code=${error.code}, message='${error.message}'")
                        appOpenAd = null
                        isLoading = false
                        markInactive()   // ✅ never going to show — release immediately
                        Handler(Looper.getMainLooper()).post { onFailed?.invoke() }
                    }
                }
            )
        }
    }

    fun showAdIfAvailable(
        activity: Activity,
        onShow: (() -> Unit)? = null,
        onComplete: () -> Unit
    ) {
        if (BillingManager.getInstance(activity).isPremiumUser()) {
            Log.d(TAG, "👑 [app_open_resume] Premium user — skipping App Open ad")
            markInactive()
            onComplete()
            return
        }
        if (isShowing) {
            Log.w(TAG, "⚠️ [app_open_resume] Already showing — skipping show")
            return
        }

        val ad = appOpenAd
        if (ad == null || !isAdAvailable()) {
            Log.d(TAG, "⚠️ [app_open_resume] No App Open Ad available")
            markInactive()
            onComplete()
            return
        }

        activeResetJob?.cancel()   // ✅ genuinely showing now — safety timer no longer needed
        isShowing = true
        AdActivityGuard.beginShowingAd()

        ad.adEventCallback = object : AppOpenAdEventCallback {
            override fun onAdShowedFullScreenContent() {
                Log.d(TAG, "🎉 [app_open_resume] Show success (fullscreen displayed)")
                Handler(Looper.getMainLooper()).post { onShow?.invoke() }
            }

            override fun onAdDismissedFullScreenContent() {
                Handler(Looper.getMainLooper()).post {
                    Log.d(TAG, "👋 [app_open_resume] Ad dismissed")
                    appOpenAd = null
                    isShowing = false
                    AdActivityGuard.endShowingAd()
                    hideOverlay()
                    markInactive()
                    onComplete()
                }
            }

            override fun onAdFailedToShowFullScreenContent(
                error: com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
            ) {
                Log.e(TAG, "❌ [app_open_resume] Failed to show: code=${error.code}, message='${error.message}'")
                appOpenAd = null
                isShowing = false
                AdActivityGuard.endShowingAd()
                hideOverlay()
                markInactive()
                onComplete()
            }

            override fun onAdClicked() { Log.d(TAG, "👆 [app_open_resume] Ad clicked") }
            override fun onAdImpression() { Log.d(TAG, "👁️ [app_open_resume] Show impression recorded") }
            override fun onAdPaid(value: com.google.android.libraries.ads.mobile.sdk.common.AdValue) {
                Log.d(TAG, "💰 [app_open_resume] Ad paid: ${value.valueMicros} ${value.currencyCode}")
            }
        }

        showOverlay()

        CoroutineScope(Dispatchers.Main).launch {
            delay(150)
            Log.d(TAG, "▶️ [AppOpen] Show requested: activity=${activity.localClassName}")
            ad.show(activity)
            activity.overridePendingTransition(0, 0)
        }
    }

    private fun isAdExpired(): Boolean =
        System.currentTimeMillis() - loadTime > AD_EXPIRE_TIME

    private fun isAdAvailable(): Boolean {
        if (appOpenAd == null) return false
        if (isAdExpired()) {
            Log.d(TAG, "⏰ App Open Ad expired")
            appOpenAd = null
            return false
        }
        return true
    }
}