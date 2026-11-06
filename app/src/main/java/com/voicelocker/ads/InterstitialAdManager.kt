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
    private const val TAG = "InterstitialAdManager"

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
            Log.d(TAG, "ℹ️ [Preload] Interstitial disabled for [$screenKey][$adKey] — skipping request")
            return
        }

        val key = cacheKey(screenKey, adKey)
        if (hasPreloadedAd(screenKey, adKey)) {
            Log.d(TAG, "🔄 [Preload] Interstitial already preloaded and ready for [$key]")
            return
        }

        if (isPreloading(screenKey, adKey)) {
            Log.d(TAG, "🔄 [Preload] Interstitial preload already in flight for [$key]")
            return
        }

        val adUnitIds = AdManager.getInterstitialIds(screenKey, adKey)
        if (adUnitIds.isEmpty()) {
            Log.d(TAG, "⚠️ [Preload] No valid interstitial IDs for [$screenKey][$adKey] — skipping request")
            return
        }

        isPreloadingMap[key] = true
        Log.d(TAG, "🚀 [Preload] Starting launch preload for [$key] (${adUnitIds.size} IDs)")

        CoroutineScope(Dispatchers.Main).launch {
            AdsInitializer.awaitReady()
            loadWaterfallForPreload(
                cacheKey = key,
                screenKey = screenKey,
                adUnitIds = adUnitIds,
                index = 0
            )
        }
    }

    private fun loadWaterfallForPreload(
        cacheKey: String,
        screenKey: String,
        adUnitIds: List<String>,
        index: Int
    ) {
        if (index >= adUnitIds.size) {
            Log.d(TAG, "❌ [Preload] All waterfall IDs exhausted for [$cacheKey]")
            isPreloadingMap[cacheKey] = false
            return
        }

        val adUnitId = adUnitIds[index]
        Log.d(TAG, "📡 [Interstitial:Preload] Request sent: cacheKey=$cacheKey, adUnitId=$adUnitId, priority=$index")

        val request = AdRequest.Builder(adUnitId).build()
        InterstitialAd.load(
            request,
            object : AdLoadCallback<InterstitialAd> {
                override fun onAdLoaded(ad: InterstitialAd) {
                    Log.d(TAG, "✅ [Interstitial:Preload] Ad preloaded fresh for [$cacheKey]: adUnitId=$adUnitId")
                    preloadedAds[cacheKey] = ad
                    if (cacheKey.endsWith(":interstitial")) {
                        preloadedAds[screenKey] = ad
                    }
                    isPreloadingMap[cacheKey] = false
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.w(TAG, "❌ [Interstitial:Preload] Failed to load for [$cacheKey]: adUnitId=$adUnitId, code=${error.code}, message='${error.message}'")
                    loadWaterfallForPreload(cacheKey, screenKey, adUnitIds, index + 1)
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
            Log.d(TAG, "ℹ️ Interstitial disabled for [$screenKey][$adKey] — skipping show")
            onComplete()
            return
        }

        if (BillingManager.getInstance(activity).isPremiumUser()) {
            Log.d(TAG, "👑 Premium user — skipping interstitial")
            onComplete()
            return
        }

        if (isShowing) {
            Log.w(TAG, "⚠️ Already showing an ad — skipping")
            onComplete()
            return
        }

        // Case 1: Fresh preloaded ad is ready
        val cachedAd = getAndRemovePreloadedAd(screenKey, adKey)
        if (cachedAd != null) {
            Log.d(TAG, "⚡ Showing fresh preloaded interstitial for [$screenKey][$adKey]")
            showLoadedAd(activity, cachedAd) {
                preload(screenKey, adKey)
                onComplete()
            }
            return
        }

        // Case 2: Preload is currently in flight — wait briefly for it
        if (isPreloading(screenKey, adKey)) {
            Log.d(TAG, "⏳ Preload in flight for [$screenKey][$adKey] — waiting briefly with loading dialog...")
            mainHandler.post { onLoading(true) }

            CoroutineScope(Dispatchers.Main).launch {
                val start = System.currentTimeMillis()
                while (isPreloading(screenKey, adKey) && System.currentTimeMillis() - start < 2000L) {
                    delay(50L)
                }
                mainHandler.post { onLoading(false) }

                val readyAd = getAndRemovePreloadedAd(screenKey, adKey)
                if (readyAd != null) {
                    Log.d(TAG, "⚡ Preload finished just in time — showing for [$screenKey][$adKey]")
                    showLoadedAd(activity, readyAd) {
                        preload(screenKey, adKey)
                        onComplete()
                    }
                } else {
                    Log.d(TAG, "⚠️ Preload timed out or failed for [$screenKey][$adKey] — proceeding")
                    onComplete()
                }
            }
            return
        }

        // Case 3: No preloaded ad and not preloading — proceed immediately (no slow runtime load)
        Log.d(TAG, "ℹ️ No preloaded ad for [$screenKey][$adKey] — proceeding immediately without runtime delay")
        preload(screenKey, adKey)
        onComplete()
    }

    /** Direct overload for callers with explicit IDs list. */
    fun show(
        activity: Activity,
        adUnitIds: List<String>,
        onLoading: (Boolean) -> Unit,
        onComplete: () -> Unit
    ) {
        if (adUnitIds.isEmpty()) {
            Log.d(TAG, "⚠️ No interstitial IDs — skipping ad")
            onComplete()
            return
        }

        if (isShowing) {
            Log.d(TAG, "⚠️ Already showing")
            onComplete()
            return
        }

        mainHandler.post {
            onLoading(true)
            Log.d(TAG, "🔄 Showing loading dialog")
        }

        // ✅ Wait for SDK readiness before touching InterstitialAd.load(...)
        CoroutineScope(Dispatchers.Main).launch {
            AdsInitializer.awaitReady()
            loadFromWaterfall(
                activity   = activity,
                adUnitIds  = adUnitIds,
                index      = 0,
                onLoading  = onLoading,
                onComplete = onComplete
            )
        }
    }

    private fun loadFromWaterfall(
        activity: Activity,
        adUnitIds: List<String>,
        index: Int,
        onLoading: (Boolean) -> Unit,
        onComplete: () -> Unit
    ) {
        if (index >= adUnitIds.size) {
            Log.d(TAG, "❌ All waterfall IDs exhausted")
            mainHandler.post {
                onLoading(false)
                onComplete()
            }
            return
        }

        val adUnitId = adUnitIds[index]
        Log.d(TAG, "📡 [Interstitial] Request sent: adUnitId=$adUnitId, priority=$index")

        val request = AdRequest.Builder(adUnitId).build()

        InterstitialAd.load(
            request,
            object : AdLoadCallback<InterstitialAd> {

                override fun onAdLoaded(ad: InterstitialAd) {
                    Log.d(TAG, "✅ [Interstitial] Ad loaded successfully: adUnitId=$adUnitId, priority=$index")
                    mainHandler.post {
                        onLoading(false)
                        mainHandler.postDelayed({
                            showLoadedAd(activity, ad, onComplete)
                        }, 300L)
                    }
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.e(TAG, "❌ [Interstitial] Failed to load: adUnitId=$adUnitId, priority=$index, code=${error.code}, message='${error.message}'")
                    loadFromWaterfall(activity, adUnitIds, index + 1, onLoading, onComplete)
                }
            }
        )
    }

    private fun showLoadedAd(
        activity: Activity,
        ad: InterstitialAd,
        onComplete: () -> Unit
    ) {
        if (isShowing) {
            Log.w(TAG, "⚠️ [Interstitial] Already showing — skipping show")
            onComplete()
            return
        }

        isShowing = true
        AdActivityGuard.beginShowingAd()
        ad.adEventCallback = object : InterstitialAdEventCallback {
            override fun onAdShowedFullScreenContent() {
                Log.d(TAG, "🎉 [Interstitial] Show success (fullscreen displayed)")
            }

            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "👋 [Interstitial] Ad dismissed")
                isShowing = false
                AdActivityGuard.endShowingAd()
                mainHandler.post { onComplete() }
            }

            override fun onAdFailedToShowFullScreenContent(
                error: com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
            ) {
                Log.e(TAG, "❌ [Interstitial] Failed to show: code=${error.code}, message='${error.message}'")
                isShowing = false
                AdActivityGuard.endShowingAd()
                mainHandler.post { onComplete() }
            }

            override fun onAdClicked() {
                Log.d(TAG, "👆 [Interstitial] Ad clicked")
            }

            override fun onAdImpression() {
                Log.d(TAG, "👁️ [Interstitial] Show impression recorded")
            }

            override fun onAdPaid(value: com.google.android.libraries.ads.mobile.sdk.common.AdValue) {
                Log.d(TAG, "💰 [Interstitial] Ad paid: ${value.valueMicros} ${value.currencyCode}")
            }
        }

        Log.d(TAG, "▶️ [Interstitial] Show requested: activity=${activity.localClassName}")
        ad.show(activity)
    }
}