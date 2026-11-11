package com.voicelocker.ads

import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAd
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAdEventCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

object InterstitialAdManager {
    private const val TAG = "ADSMANAGER"

    @Volatile
    private var isShowing = false

    private val preloadedAds = ConcurrentHashMap<String, InterstitialAd>()
    private val isPreloadingMap = ConcurrentHashMap<String, Boolean>()

    private val mainHandler = Handler(Looper.getMainLooper())

    private fun cacheKey(screenKey: String, adKey: String): String = "$screenKey:$adKey"

    fun hasPreloadedAd(screenKey: String, adKey: String? = null): Boolean {
        if (adKey != null && preloadedAds.containsKey(cacheKey(screenKey, adKey))) return true
        if (preloadedAds.containsKey(screenKey)) return true
        return preloadedAds.keys.any { it == screenKey || it.startsWith("$screenKey:") }
    }

    fun isPreloading(screenKey: String, adKey: String? = null): Boolean {
        if (adKey != null && isPreloadingMap[cacheKey(screenKey, adKey)] == true) return true
        if (isPreloadingMap[screenKey] == true) return true
        return isPreloadingMap.entries.any { (it.key == screenKey || it.key.startsWith("$screenKey:")) && it.value == true }
    }

    private fun getAndRemovePreloadedAd(screenKey: String, adKey: String): InterstitialAd? {
        preloadedAds.remove(cacheKey(screenKey, adKey))?.let { return it }
        preloadedAds.remove(screenKey)?.let { return it }
        val matchingKey = preloadedAds.keys.firstOrNull { it.startsWith("$screenKey:") }
        if (matchingKey != null) {
            return preloadedAds.remove(matchingKey)
        }
        return null
    }

    /**
     * Preloads a fresh InterstitialAd for the given [screenKey] immediately at screen launch.
     * Verifies that interstitial is enabled for this screen before requesting.
     */
    fun preload(screenKey: String, adKey: String = "interstitial") {
        if (!AdManager.isInterstitialEnabled(screenKey, adKey)) {
            Log.d(TAG, "[$screenKey][$adKey] Interstitial disabled in config — skipping preload")
            return
        }

        val key = cacheKey(screenKey, adKey)
        if (hasPreloadedAd(screenKey, adKey)) {
            Log.d(TAG, "[$screenKey][$adKey] Interstitial already preloaded and ready")
            return
        }

        if (isPreloading(screenKey, adKey)) {
            Log.d(TAG, "[$screenKey][$adKey] Interstitial preload already in flight")
            return
        }

        val adUnitIds = AdManager.getInterstitialIds(screenKey, adKey)
        if (adUnitIds.isEmpty()) {
            Log.w(TAG, "[$screenKey][$adKey] No valid interstitial IDs found — skipping preload")
            return
        }

        isPreloadingMap[key] = true
        Log.d(TAG, "[$screenKey][$adKey] Starting preload (${adUnitIds.size} waterfall IDs)")

        CoroutineScope(Dispatchers.Main).launch {
            AdsInitializer.awaitReady()
            loadWaterfallForPreload(
                cacheKey = key,
                screenKey = screenKey,
                adKey = adKey,
                adUnitIds = adUnitIds,
                index = 0
            )
        }
    }

    private fun loadWaterfallForPreload(
        cacheKey: String,
        screenKey: String,
        adKey: String,
        adUnitIds: List<String>,
        index: Int
    ) {
        if (index >= adUnitIds.size) {
            Log.e(TAG, "[$screenKey][$adKey] All waterfall IDs exhausted, failed to load ad")
            isPreloadingMap[cacheKey] = false
            return
        }

        val adUnitId = adUnitIds[index]
        Log.d(TAG, "[$screenKey][$adKey] Interstitial ad request sent: adUnitId=$adUnitId, index=$index")

        val request = AdRequest.Builder(adUnitId).build()
        InterstitialAd.load(
            request,
            object : AdLoadCallback<InterstitialAd> {
                override fun onAdLoaded(ad: InterstitialAd) {
                    Log.d(TAG, "[$screenKey][$adKey] Interstitial ad loaded: adUnitId=$adUnitId")
                    preloadedAds[cacheKey] = ad
                    if (cacheKey.endsWith(":interstitial")) {
                        preloadedAds[screenKey] = ad
                    }
                    isPreloadingMap[cacheKey] = false
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.e(TAG, "[$screenKey][$adKey] Interstitial ad failed to load: code=${error.code}, message='${error.message}', adUnitId=$adUnitId")
                    loadWaterfallForPreload(cacheKey, screenKey, adKey, adUnitIds, index + 1)
                }
            }
        )
    }

    /**
     * Shows the preloaded ad for [screenKey]. If preloading is currently in flight,
     * shows the loading dialog briefly while waiting for it to finish.
     * If interstitial is disabled or not available, completes immediately without runtime blocking.
     */
    fun show(
        activity: Activity,
        screenKey: String,
        adKey: String = "interstitial",
        onLoading: (Boolean) -> Unit = {},
        onComplete: () -> Unit
    ) {
        if (!AdManager.isInterstitialEnabled(screenKey, adKey)) {
            Log.d(TAG, "[$screenKey][$adKey] Interstitial disabled in config — skipping show")
            onComplete()
            return
        }

        if (BillingManager.getInstance(activity).isPremiumUser()) {
            Log.d(TAG, "[$screenKey][$adKey] Premium user — skipping interstitial show")
            onComplete()
            return
        }

        if (isShowing) {
            Log.w(TAG, "[$screenKey][$adKey] Already showing an ad — skipping show")
            onComplete()
            return
        }

        // Case 1: Fresh preloaded ad is ready
        val cachedAd = getAndRemovePreloadedAd(screenKey, adKey)
        if (cachedAd != null) {
            Log.d(TAG, "[$screenKey][$adKey] Showing preloaded interstitial ad")
            showLoadedAd(activity, cachedAd, screenKey, adKey) {
                preload(screenKey, adKey)
                onComplete()
            }
            return
        }

        // Case 2: Preload is currently in flight — wait briefly for it
        if (isPreloading(screenKey, adKey)) {
            Log.d(TAG, "[$screenKey][$adKey] Preload in flight — waiting briefly with loading dialog...")
            mainHandler.post { onLoading(true) }

            CoroutineScope(Dispatchers.Main).launch {
                val start = System.currentTimeMillis()
                while (isPreloading(screenKey, adKey) && System.currentTimeMillis() - start < 2000L) {
                    delay(50L)
                }
                mainHandler.post { onLoading(false) }

                val readyAd = getAndRemovePreloadedAd(screenKey, adKey)
                if (readyAd != null) {
                    Log.d(TAG, "[$screenKey][$adKey] Preload finished just in time — showing interstitial")
                    showLoadedAd(activity, readyAd, screenKey, adKey) {
                        preload(screenKey, adKey)
                        onComplete()
                    }
                } else {
                    Log.w(TAG, "[$screenKey][$adKey] Preload timed out or failed to load — proceeding without ad")
                    onComplete()
                }
            }
            return
        }

        // Case 3: No preloaded ad and not preloading — load on demand with loading dialog
        val adUnitIds = AdManager.getInterstitialIds(screenKey, adKey)
        if (adUnitIds.isEmpty()) {
            Log.w(TAG, "[$screenKey][$adKey] No valid interstitial IDs found — skipping show")
            onComplete()
            return
        }

        Log.d(TAG, "[$screenKey][$adKey] No preloaded ad available — loading on demand with loading dialog")
        show(
            activity = activity,
            adUnitIds = adUnitIds,
            screenKey = screenKey,
            adKey = adKey,
            onLoading = onLoading,
            onComplete = onComplete
        )
    }

    /** Direct overload for callers with explicit IDs list. */
    fun show(
        activity: Activity,
        adUnitIds: List<String>,
        screenKey: String = "theme_screen",
        adKey: String = "inter_apply",
        onLoading: (Boolean) -> Unit = {},
        onComplete: () -> Unit
    ) {
        if (adUnitIds.isEmpty()) {
            Log.w(TAG, "[$screenKey][$adKey] No interstitial IDs provided — skipping show")
            onComplete()
            return
        }

        if (BillingManager.getInstance(activity).isPremiumUser()) {
            Log.d(TAG, "[$screenKey][$adKey] Premium user — skipping interstitial show")
            onComplete()
            return
        }

        if (isShowing) {
            Log.w(TAG, "[$screenKey][$adKey] Already showing an ad — skipping show")
            onComplete()
            return
        }

        mainHandler.post {
            onLoading(true)
            Log.d(TAG, "[$screenKey][$adKey] Showing loading dialog for runtime load")
        }

        // ✅ Wait for SDK readiness before touching InterstitialAd.load(...)
        CoroutineScope(Dispatchers.Main).launch {
            AdsInitializer.awaitReady()
            loadFromWaterfall(
                activity   = activity,
                adUnitIds  = adUnitIds,
                index      = 0,
                screenKey  = screenKey,
                adKey      = adKey,
                onLoading  = onLoading,
                onComplete = onComplete
            )
        }
    }

    private fun loadFromWaterfall(
        activity: Activity,
        adUnitIds: List<String>,
        index: Int,
        screenKey: String,
        adKey: String,
        onLoading: (Boolean) -> Unit,
        onComplete: () -> Unit
    ) {
        if (index >= adUnitIds.size) {
            Log.e(TAG, "[$screenKey][$adKey] All waterfall IDs exhausted, failed to load ad")
            mainHandler.post {
                onLoading(false)
                onComplete()
            }
            return
        }

        val adUnitId = adUnitIds[index]
        Log.d(TAG, "[$screenKey][$adKey] Interstitial ad request sent: adUnitId=$adUnitId, index=$index")

        val request = AdRequest.Builder(adUnitId).build()

        InterstitialAd.load(
            request,
            object : AdLoadCallback<InterstitialAd> {

                override fun onAdLoaded(ad: InterstitialAd) {
                    Log.d(TAG, "[$screenKey][$adKey] Interstitial ad loaded: adUnitId=$adUnitId")
                    mainHandler.post {
                        onLoading(false)
                        mainHandler.postDelayed({
                            showLoadedAd(activity, ad, screenKey, adKey, onComplete)
                        }, 300L)
                    }
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.e(TAG, "[$screenKey][$adKey] Interstitial ad failed to load: code=${error.code}, message='${error.message}', adUnitId=$adUnitId")
                    loadFromWaterfall(activity, adUnitIds, index + 1, screenKey, adKey, onLoading, onComplete)
                }
            }
        )
    }

    private fun showLoadedAd(
        activity: Activity,
        ad: InterstitialAd,
        screenKey: String,
        adKey: String,
        onComplete: () -> Unit
    ) {
        if (isShowing) {
            Log.w(TAG, "[$screenKey][$adKey] Interstitial ad failed to show: already showing an ad")
            onComplete()
            return
        }

        isShowing = true
        AdActivityGuard.beginShowingAd()
        ad.adEventCallback = object : InterstitialAdEventCallback {
            override fun onAdShowedFullScreenContent() {
                Log.d(TAG, "[$screenKey][$adKey] Interstitial ad show success")
                if (RemoteConfigManager.isSecondStrategyEnabled() && SecondStrategyManager.isDashboardOrAheadScreen(screenKey)) {
                    SecondStrategyManager.onInterstitialShown(screenKey, adKey)
                } else if (!RemoteConfigManager.isSecondStrategyEnabled()) {
                    CounterAdManager.onInterstitialDisplayed()
                }
            }

            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "[$screenKey][$adKey] Interstitial ad dismissed")
                isShowing = false
                AdActivityGuard.endShowingAd()
                mainHandler.post { onComplete() }
            }

            override fun onAdFailedToShowFullScreenContent(
                error: com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
            ) {
                Log.e(TAG, "[$screenKey][$adKey] Interstitial ad failed to show: code=${error.code}, message='${error.message}'")
                isShowing = false
                AdActivityGuard.endShowingAd()
                mainHandler.post { onComplete() }
            }

            override fun onAdClicked() {
                Log.d(TAG, "[$screenKey][$adKey] Interstitial ad clicked")
            }

            override fun onAdImpression() {
                Log.d(TAG, "[$screenKey][$adKey] Interstitial ad impression recorded")
            }

            override fun onAdPaid(value: com.google.android.libraries.ads.mobile.sdk.common.AdValue) {
                Log.d(TAG, "[$screenKey][$adKey] Interstitial ad paid: ${value.valueMicros} ${value.currencyCode}")
            }
        }

        Log.d(TAG, "[$screenKey][$adKey] Interstitial ad show requested: activity=${activity.localClassName}")
        ad.show(activity)
    }
}