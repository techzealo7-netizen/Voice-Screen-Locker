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
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAd
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAdEventCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

object SplashAdPreloader {
    private const val TAG = "SplashAdPreloader"

    private var loadedInterstitial: InterstitialAd? = null
    private var loadedAppOpen: AppOpenAd? = null

    @Volatile
    var isPreloading: Boolean = false
        private set

    @Volatile
    var isAdReady: Boolean = false
        private set

    @Volatile
    private var isShowing: Boolean = false

    private val mainHandler = Handler(Looper.getMainLooper())

    fun reset() {
        loadedInterstitial = null
        loadedAppOpen = null
        isPreloading = false
        isAdReady = false
        isShowing = false
    }

    fun startPreload(context: Context) {
        if (BillingManager.getInstance(context).isPremiumUser()) {
            Log.d(TAG, "👑 Premium user — skipping splash ad preload")
            return
        }

        if (isPreloading || isAdReady) {
            Log.d(TAG, "🔄 Preload already in progress or ready")
            return
        }

        reset()
        isPreloading = true

        CoroutineScope(Dispatchers.Main).launch {
            AdsInitializer.awaitReady(context)

            val isInterEnabled = AdManager.isInterstitialEnabled("splash", "interstitial")
            val isAppOpenEnabled = AdManager.isAppOpenEnabled()

            Log.d(TAG, "🚀 Starting Splash preload: interEnabled=$isInterEnabled, appOpenEnabled=$isAppOpenEnabled")

            when {
                // Journey A / Primary: App Open is enabled (preferred on splash)
                isAppOpenEnabled -> {
                    val appOpenId = AdManager.getAppOpenAdId() ?: AdManager.getRawAppOpenAdId("splash")
                    if (!appOpenId.isNullOrBlank()) {
                        preloadAppOpen(appOpenId) { success ->
                            if (!success) {
                                Log.w(TAG, "⚠️ Splash primary App Open failed to load. Falling back to Interstitial...")
                                val interIds = AdManager.getRawInterstitialIds("splash", "interstitial")
                                if (interIds.isNotEmpty()) {
                                    preloadInterstitialWaterfall(interIds, index = 0) { interSuccess ->
                                        isPreloading = false
                                        isAdReady = interSuccess
                                        Log.d(TAG, "🏁 Splash preload finished: hasAd=$isAdReady (type=${if (isAdReady) "InterFallback" else "None"})")
                                    }
                                } else {
                                    isPreloading = false
                                    isAdReady = false
                                }
                            } else {
                                isPreloading = false
                                isAdReady = true
                                Log.d(TAG, "✅ Splash primary App Open preloaded successfully")
                            }
                        }
                    } else {
                        // App Open ID was blank -> fallback to Interstitial immediately
                        val interIds = AdManager.getRawInterstitialIds("splash", "interstitial")
                        if (interIds.isNotEmpty()) {
                            preloadInterstitialWaterfall(interIds, index = 0) { interSuccess ->
                                isPreloading = false
                                isAdReady = interSuccess
                            }
                        } else {
                            isPreloading = false
                            isAdReady = false
                        }
                    }
                }

                // Journey B: Interstitial is enabled (and App Open disabled)
                isInterEnabled -> {
                    val interIds = AdManager.getInterstitialIds("splash", "interstitial").ifEmpty {
                        AdManager.getRawInterstitialIds("splash", "interstitial")
                    }
                    if (interIds.isNotEmpty()) {
                        preloadInterstitialWaterfall(interIds, index = 0) { success ->
                            if (!success) {
                                Log.w(TAG, "⚠️ Splash primary Interstitial failed to load. Falling back to App Open...")
                                val appOpenId = AdManager.getRawAppOpenAdId("splash")
                                if (!appOpenId.isNullOrBlank()) {
                                    preloadAppOpen(appOpenId) { appOpenSuccess ->
                                        isPreloading = false
                                        isAdReady = appOpenSuccess
                                        Log.d(TAG, "🏁 Splash preload finished: hasAd=$isAdReady (type=${if (isAdReady) "AppOpenFallback" else "None"})")
                                    }
                                } else {
                                    isPreloading = false
                                    isAdReady = false
                                }
                            } else {
                                isPreloading = false
                                isAdReady = true
                                Log.d(TAG, "✅ Splash primary Interstitial preloaded successfully")
                            }
                        }
                    } else {
                        // Interstitial IDs blank -> fallback to App Open immediately
                        val appOpenId = AdManager.getRawAppOpenAdId("splash")
                        if (!appOpenId.isNullOrBlank()) {
                            preloadAppOpen(appOpenId) { appOpenSuccess ->
                                isPreloading = false
                                isAdReady = appOpenSuccess
                            }
                        } else {
                            isPreloading = false
                            isAdReady = false
                        }
                    }
                }

                else -> {
                    Log.d(TAG, "ℹ️ Neither interstitial nor app open enabled on splash")
                    isPreloading = false
                    isAdReady = false
                }
            }
        }
    }

    private fun preloadInterstitialWaterfall(
        ids: List<String>,
        index: Int,
        onFinished: (Boolean) -> Unit
    ) {
        if (index >= ids.size) {
            Log.d(TAG, "❌ All splash interstitial waterfall IDs exhausted")
            mainHandler.post { onFinished(false) }
            return
        }

        val adUnitId = ids[index]
        Log.d(TAG, "📡 [Splash:Interstitial] Request sent: adUnitId=$adUnitId, priority=$index")

        val request = AdRequest.Builder(adUnitId).build()
        InterstitialAd.load(
            request,
            object : AdLoadCallback<InterstitialAd> {
                override fun onAdLoaded(ad: InterstitialAd) {
                    Log.d(TAG, "✅ [Splash:Interstitial] Ad loaded successfully: adUnitId=$adUnitId, priority=$index")
                    loadedInterstitial = ad
                    mainHandler.post { onFinished(true) }
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.w(TAG, "❌ [Splash:Interstitial] Failed to load: adUnitId=$adUnitId, priority=$index, code=${error.code}, message='${error.message}'")
                    preloadInterstitialWaterfall(ids, index + 1, onFinished)
                }
            }
        )
    }

    private fun preloadAppOpen(adUnitId: String, onFinished: (Boolean) -> Unit) {
        Log.d(TAG, "📡 [Splash:AppOpen] Request sent: adUnitId=$adUnitId")
        val request = AdRequest.Builder(adUnitId).build()
        AppOpenAd.load(
            request,
            object : AdLoadCallback<AppOpenAd> {
                override fun onAdLoaded(ad: AppOpenAd) {
                    Log.d(TAG, "✅ [Splash:AppOpen] Ad loaded successfully: adUnitId=$adUnitId")
                    loadedAppOpen = ad
                    mainHandler.post { onFinished(true) }
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.w(TAG, "❌ [Splash:AppOpen] Failed to load: adUnitId=$adUnitId, code=${error.code}, message='${error.message}'")
                    mainHandler.post { onFinished(false) }
                }
            }
        )
    }

    fun show(
        activity: Activity,
        waitIfPreloading: Boolean = true,
        onLoading: (Boolean) -> Unit = {},
        onComplete: () -> Unit
    ) {
        if (BillingManager.getInstance(activity).isPremiumUser()) {
            onComplete()
            return
        }

        if (isShowing) {
            onComplete()
            return
        }

        // Prefer App Open if App Open is enabled in remote config
        val preferAppOpen = AdManager.isAppOpenEnabled()

        if (preferAppOpen) {
            // Case 1: App Open ready
            if (loadedAppOpen != null) {
                val ad = loadedAppOpen!!
                loadedAppOpen = null
                showAppOpen(activity, ad, isFallback = false, onComplete = onComplete)
                return
            }
            // Case 2: Interstitial ready as fallback
            if (loadedInterstitial != null) {
                val ad = loadedInterstitial!!
                loadedInterstitial = null
                showInterstitial(activity, ad, isFallback = true, onComplete = onComplete)
                return
            }
        } else {
            // Case 1: Interstitial ready
            if (loadedInterstitial != null) {
                val ad = loadedInterstitial!!
                loadedInterstitial = null
                showInterstitial(activity, ad, isFallback = false, onComplete = onComplete)
                return
            }
            // Case 2: App Open ready as fallback
            if (loadedAppOpen != null) {
                val ad = loadedAppOpen!!
                loadedAppOpen = null
                showAppOpen(activity, ad, isFallback = true, onComplete = onComplete)
                return
            }
        }

        // Case 3: Still preloading — only wait if waitIfPreloading is explicitly true
        if (isPreloading && waitIfPreloading) {
            Log.d(TAG, "⏳ Splash ad still preloading — waiting briefly...")
            onLoading(true)

            CoroutineScope(Dispatchers.Main).launch {
                val startTime = System.currentTimeMillis()
                while (isPreloading && System.currentTimeMillis() - startTime < 2500L) {
                    delay(100L)
                }

                onLoading(false)

                if (preferAppOpen) {
                    when {
                        loadedAppOpen != null -> {
                            val ad = loadedAppOpen!!
                            loadedAppOpen = null
                            showAppOpen(activity, ad, isFallback = false, onComplete = onComplete)
                        }
                        loadedInterstitial != null -> {
                            val ad = loadedInterstitial!!
                            loadedInterstitial = null
                            showInterstitial(activity, ad, isFallback = true, onComplete = onComplete)
                        }
                        else -> {
                            Log.d(TAG, "⚠️ Splash ad preload timed out or failed — proceeding")
                            onComplete()
                        }
                    }
                } else {
                    when {
                        loadedInterstitial != null -> {
                            val ad = loadedInterstitial!!
                            loadedInterstitial = null
                            showInterstitial(activity, ad, isFallback = false, onComplete = onComplete)
                        }
                        loadedAppOpen != null -> {
                            val ad = loadedAppOpen!!
                            loadedAppOpen = null
                            showAppOpen(activity, ad, isFallback = true, onComplete = onComplete)
                        }
                        else -> {
                            Log.d(TAG, "⚠️ Splash ad preload timed out or failed — proceeding")
                            onComplete()
                        }
                    }
                }
            }
            return
        }

        // Case 4: No ad ready or wait not requested — proceed immediately without extra loading time
        Log.d(TAG, "ℹ️ No splash fullscreen ad ready — proceeding immediately (no extra loading time)")
        onComplete()
    }

    private fun showInterstitial(
        activity: Activity,
        ad: InterstitialAd,
        isFallback: Boolean = false,
        onComplete: () -> Unit
    ) {
        if (activity.isFinishing || activity.isDestroyed) {
            Log.w(TAG, "⚠️ Host activity finishing or destroyed — skipping show")
            cleanupAndFinish(onComplete)
            return
        }

        isShowing = true
        AdActivityGuard.beginShowingAd()

        ad.adEventCallback = object : InterstitialAdEventCallback {
            override fun onAdShowedFullScreenContent() {
                Log.d(TAG, "🎉 [Splash:Interstitial] Show success (fullscreen displayed)")
            }

            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "👋 [Splash:Interstitial] Ad dismissed")
                cleanupAndFinish(onComplete)
            }

            override fun onAdFailedToShowFullScreenContent(error: FullScreenContentError) {
                Log.e(TAG, "❌ [Splash:Interstitial] Failed to show: code=${error.code}, message='${error.message}'")
                if (!isFallback) {
                    Log.w(TAG, "🔄 [Splash:Interstitial] Falling back to App Open...")
                    fallbackToAppOpen(activity, onComplete)
                } else {
                    cleanupAndFinish(onComplete)
                }
            }
        }

        Log.d(TAG, "▶️ [Splash:Interstitial] Show requested: activity=${activity.localClassName}")
        ad.show(activity)
    }

    private fun showAppOpen(
        activity: Activity,
        ad: AppOpenAd,
        isFallback: Boolean = false,
        onComplete: () -> Unit
    ) {
        if (activity.isFinishing || activity.isDestroyed) {
            Log.w(TAG, "⚠️ Host activity finishing or destroyed — skipping show")
            cleanupAndFinish(onComplete)
            return
        }

        isShowing = true
        AdActivityGuard.beginShowingAd()
        AppOpenAdManager.showOverlay()

        ad.adEventCallback = object : AppOpenAdEventCallback {
            override fun onAdShowedFullScreenContent() {
                Log.d(TAG, "🎉 [Splash:AppOpen] Show success (fullscreen displayed)")
            }

            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "👋 [Splash:AppOpen] Ad dismissed")
                cleanupAndFinish(onComplete)
            }

            override fun onAdFailedToShowFullScreenContent(error: FullScreenContentError) {
                Log.e(TAG, "❌ [Splash:AppOpen] Failed to show: code=${error.code}, message='${error.message}'")
                if (!isFallback) {
                    Log.w(TAG, "🔄 [Splash:AppOpen] Falling back to Interstitial...")
                    fallbackToInterstitial(activity, onComplete)
                } else {
                    cleanupAndFinish(onComplete)
                }
            }
        }

        Log.d(TAG, "▶️ [Splash:AppOpen] Show requested: activity=${activity.localClassName}")
        ad.show(activity)
    }

    private fun fallbackToInterstitial(activity: Activity, onComplete: () -> Unit) {
        val inter = loadedInterstitial
        if (inter != null) {
            loadedInterstitial = null
            showInterstitial(activity, inter, isFallback = true, onComplete = onComplete)
            return
        }

        val interIds = AdManager.getRawInterstitialIds("splash", "interstitial")
        if (interIds.isNotEmpty() && !activity.isFinishing && !activity.isDestroyed) {
            val adUnitId = interIds.first()
            val request = AdRequest.Builder(adUnitId).build()
            InterstitialAd.load(
                request,
                object : AdLoadCallback<InterstitialAd> {
                    override fun onAdLoaded(loadedAd: InterstitialAd) {
                        showInterstitial(activity, loadedAd, isFallback = true, onComplete = onComplete)
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        Log.w(TAG, "❌ [Splash:InterstitialFallback] Failed to load on-demand: code=${error.code}, message='${error.message}'")
                        cleanupAndFinish(onComplete)
                    }
                }
            )
        } else {
            cleanupAndFinish(onComplete)
        }
    }

    private fun fallbackToAppOpen(activity: Activity, onComplete: () -> Unit) {
        val appOpen = loadedAppOpen
        if (appOpen != null) {
            loadedAppOpen = null
            showAppOpen(activity, appOpen, isFallback = true, onComplete = onComplete)
            return
        }

        val appOpenId = AdManager.getRawAppOpenAdId("splash")
        if (!appOpenId.isNullOrBlank() && !activity.isFinishing && !activity.isDestroyed) {
            val request = AdRequest.Builder(appOpenId).build()
            AppOpenAd.load(
                request,
                object : AdLoadCallback<AppOpenAd> {
                    override fun onAdLoaded(loadedAd: AppOpenAd) {
                        showAppOpen(activity, loadedAd, isFallback = true, onComplete = onComplete)
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        Log.w(TAG, "❌ [Splash:AppOpenFallback] Failed to load on-demand: code=${error.code}, message='${error.message}'")
                        cleanupAndFinish(onComplete)
                    }
                }
            )
        } else {
            cleanupAndFinish(onComplete)
        }
    }

    private fun cleanupAndFinish(onComplete: () -> Unit) {
        loadedInterstitial = null
        loadedAppOpen = null
        isShowing = false
        isAdReady = false
        AdActivityGuard.endShowingAd()
        AppOpenAdManager.hideOverlay()
        mainHandler.post { onComplete() }
    }
}

