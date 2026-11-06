package com.voicelocker.ads.native_manager

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.libraries.ads.mobile.sdk.MobileAds
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.nativead.MediaView
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdLoader
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdLoaderCallback
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdRequest
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdView
import com.voicelocker.app.R
import com.voicelocker.ads.AdsInitializer
import com.voicelocker.ads.ThemeManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object NativeAdsManager {
    private const val TAG = "NativeAdsManager"

    fun isInternetAvailable(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    interface NativeShowListener {
        fun onLoaded()
        fun onFailed()
    }

    fun getTemplateLayout(template: Int): Int {
        return when (template) {
            1 -> R.layout.ads_google_small_native
            2 -> R.layout.ads_google_medium_native
            3 -> R.layout.ads_google_large_native
            4 -> R.layout.layout_native_full_screen
            else -> R.layout.ads_google_small_native
        }
    }

    fun getTemplateShimmer(template: Int): Int {
        return when (template) {
            1 -> R.layout.layout_shimmer_native_small
            2 -> R.layout.layout_shimmer_native_medium
            3 -> R.layout.layout_shimmer_native_large
            4 -> R.layout.layout_shimmer_native_full_screen
            else -> R.layout.layout_shimmer_native_small
        }
    }

    private fun findShimmer(root: View?): ShimmerFrameLayout? {
        if (root == null) return null
        return (root as? ShimmerFrameLayout)
            ?: root.findViewById(R.id.shimmerNativeSmall)
            ?: root.findViewById(R.id.shimmerNativeMedium)
            ?: root.findViewById(R.id.shimmerNativeLarge)
            ?: root.findViewById(R.id.shimmerNativeFullScreen)
            ?: ((root as? ViewGroup)?.getChildAt(0) as? ShimmerFrameLayout)
    }

    fun showDynamicShimmer(
        context: Context,
        template: Int,
        shimmerContainer: ViewGroup?,
        shimmerView: View? = null
    ) {
        val shimmerResId = getTemplateShimmer(template)
        val target: ViewGroup? = shimmerContainer ?: (shimmerView as? ViewGroup)

        if (target != null) {
            val currentTag = target.tag as? Int
            if (currentTag == shimmerResId && target.childCount > 0 && target.visibility == View.VISIBLE) {
                findShimmer(target)?.startShimmer()
                return
            }
            target.removeAllViews()
            LayoutInflater.from(context).inflate(shimmerResId, target, true)
            target.tag = shimmerResId
            target.visibility = View.VISIBLE

            val isDark = ThemeManager.isDark(context)
            updateShimmerTheme(target, template, isDark)

            findShimmer(target)?.startShimmer()
        } else if (shimmerView != null) {
            shimmerView.visibility = View.VISIBLE
            (shimmerView as? ShimmerFrameLayout)?.startShimmer()
        }
    }

    fun hideDynamicShimmer(
        shimmerContainer: ViewGroup?,
        shimmerView: View? = null
    ) {
        if (shimmerContainer != null) {
            findShimmer(shimmerContainer)?.stopShimmer()
            shimmerContainer.visibility = View.GONE
            shimmerContainer.removeAllViews()
            shimmerContainer.tag = null
        }
        if (shimmerView != null) {
            findShimmer(shimmerView)?.stopShimmer()
            shimmerView.visibility = View.GONE
        }
    }

    fun loadNativeAdObject(
        context: Context,
        adUnitId: String,
        onSuccess: (NativeAd) -> Unit,
        onError: (LoadAdError?) -> Unit
    ) {
        if (adUnitId.isBlank() || !isInternetAvailable(context)) {
            Log.w(TAG, "[Native] CANCELLED loadNativeAdObject: adUnitId is blank or no internet")
            onError(null)
            return
        }

        CoroutineScope(Dispatchers.Main).launch {
            try {
                AdsInitializer.awaitReady(context)
                if (!MobileAds.isInitialized) {
                    Log.w(TAG, "[Native] MobileAds not initialized for loadNativeAdObject")
                    onError(null)
                    return@launch
                }

                Log.d(TAG, "📡 [Native] Request sent (Object): adUnitId=$adUnitId")
                val request = NativeAdRequest.Builder(
                    adUnitId,
                    listOf(NativeAd.NativeAdType.NATIVE)
                ).build()

                NativeAdLoader.load(
                    request,
                    object : NativeAdLoaderCallback {
                        override fun onNativeAdLoaded(nativeAd: NativeAd) {
                            Log.d(TAG, "✅ [Native] Ad loaded successfully (Object): adUnitId=$adUnitId, headline='${nativeAd.headline}'")
                            Handler(Looper.getMainLooper()).post {
                                onSuccess(nativeAd)
                            }
                        }

                        override fun onAdFailedToLoad(error: LoadAdError) {
                            Log.e(TAG, "❌ [Native] Failed to load (Object): adUnitId=$adUnitId, code=${error.code}, message='${error.message}'")
                            Handler(Looper.getMainLooper()).post {
                                onError(error)
                            }
                        }
                    }
                )
            } catch (e: Throwable) {
                Log.e(TAG, "❌ [Native] Exception loading native ad object: ${e.message}", e)
                onError(null)
            }
        }
    }

    fun loadNativeAd(
        context: Context,
        adUnitId: String,
        template: Int,
        container: FrameLayout,
        shimmerContainer: ViewGroup? = null,
        shimmerView: View? = null,
        onClose: (() -> Unit)? = null,
        listener: NativeShowListener? = null
    ) {
        if (adUnitId.isBlank() || !isInternetAvailable(context)) {
            Log.w(TAG, "⚠️ [Native] CANCELLED: adUnitId is blank or no internet")
            container.visibility = View.GONE
            hideDynamicShimmer(shimmerContainer, shimmerView)
            listener?.onFailed()
            return
        }

        val layoutResId = getTemplateLayout(template)
        Log.d(TAG, "📡 [Native] Request sent: adUnitId=$adUnitId, template=$template, layoutResId=$layoutResId")
        showDynamicShimmer(context, template, shimmerContainer, shimmerView)

        CoroutineScope(Dispatchers.Main).launch {
            try {
                // Ensure MobileAds is initialized before calling NativeAdLoader.load
                AdsInitializer.awaitReady(context)

                if (!MobileAds.isInitialized) {
                    Log.w(TAG, "❌ [Native] MobileAds not initialized yet, skipping native load and falling back")
                    hideDynamicShimmer(shimmerContainer, shimmerView)
                    container.visibility = View.GONE
                    listener?.onFailed()
                    return@launch
                }

                val request = NativeAdRequest.Builder(
                    adUnitId,
                    listOf(NativeAd.NativeAdType.NATIVE)
                ).build()

                NativeAdLoader.load(
                    request,
                    object : NativeAdLoaderCallback {
                        override fun onNativeAdLoaded(nativeAd: NativeAd) {
                            Log.d(TAG, "✅ [Native] Ad loaded successfully: adUnitId=$adUnitId, headline='${nativeAd.headline}'")
                            Handler(Looper.getMainLooper()).post {
                                try {
                                    Log.d(TAG, "▶️ [Native] Show requested (rendering into container)")
                                    renderIntoContainer(context, nativeAd, container, layoutResId, onClose)
                                    hideDynamicShimmer(shimmerContainer, shimmerView)
                                    container.visibility = View.VISIBLE
                                    Log.d(TAG, "🎉 [Native] Show success (view rendered and visible)")
                                    listener?.onLoaded()
                                } catch (e: Throwable) {
                                    Log.e(TAG, "❌ [Native] Failed to show: error rendering ad: ${e.message}", e)
                                    hideDynamicShimmer(shimmerContainer, shimmerView)
                                    container.visibility = View.GONE
                                    listener?.onFailed()
                                }
                            }
                        }

                        override fun onAdFailedToLoad(error: LoadAdError) {
                            Log.e(TAG, "❌ [Native] Failed to load: adUnitId=$adUnitId, template=$template, code=${error.code}, message='${error.message}'")
                            Handler(Looper.getMainLooper()).post {
                                hideDynamicShimmer(shimmerContainer, shimmerView)
                                container.visibility = View.GONE
                                listener?.onFailed()
                            }
                        }
                    }
                )
            } catch (e: Throwable) {
                Log.e(TAG, "❌ [Native] Exception loading native ad: ${e.message}", e)
                hideDynamicShimmer(shimmerContainer, shimmerView)
                container.visibility = View.GONE
                listener?.onFailed()
            }
        }
    }

    fun renderIntoContainer(
        context: Context,
        nativeAd: NativeAd,
        container: FrameLayout,
        layoutResId: Int,
        onClose: (() -> Unit)? = null
    ) {
        val inflatedView = LayoutInflater.from(context).inflate(layoutResId, container, false)
        val adView = (inflatedView as? NativeAdView)
            ?: inflatedView.findViewById<NativeAdView>(R.id.native_ad_view)
            ?: inflatedView.findViewById<NativeAdView>(R.id.native_ad_fullscreen_view)
            ?: run {
                (inflatedView as? FrameLayout)?.let { fl ->
                    for (i in 0 until fl.childCount) {
                        val child = fl.getChildAt(i)
                        if (child is NativeAdView) return@run child
                    }
                }
                null
            }

        if (adView != null) {
            populateNativeAdView(nativeAd, adView, onClose)
        }
        container.removeAllViews()
        container.addView(inflatedView)
    }

    fun populateNativeAdView(
        nativeAd: NativeAd,
        nativeAdView: NativeAdView,
        onClose: (() -> Unit)? = null
    ) {
        val closeBtn = nativeAdView.findViewById<View>(R.id.btn_close_native_ad)
        if (closeBtn != null) {
            closeBtn.visibility = View.VISIBLE
            closeBtn.setOnClickListener {
                onClose?.invoke()
            }
        }

        val mediaView = (nativeAdView.findViewById<View>(R.id.media_view) as? MediaView)
            ?: (nativeAdView.findViewById<View>(R.id.ad_media) as? MediaView)

        val headlineView = (nativeAdView.findViewById<View>(R.id.ad_headline) as? TextView)
            ?: (nativeAdView.findViewById<View>(R.id.primary) as? TextView)
        if (headlineView != null) {
            headlineView.text = nativeAd.headline ?: ""
            nativeAdView.headlineView = headlineView
        }

        val advertiserView = (nativeAdView.findViewById<View>(R.id.ad_advertiser) as? TextView)
        val advertiserText = nativeAd.advertiser ?: nativeAd.store
        if (advertiserView != null) {
            if (!advertiserText.isNullOrBlank()) {
                advertiserView.text = advertiserText
                advertiserView.visibility = View.VISIBLE
                nativeAdView.advertiserView = advertiserView
            } else {
                advertiserView.visibility = View.GONE
            }
        }

        val bodyView = (nativeAdView.findViewById<View>(R.id.ad_body) as? TextView)
            ?: (nativeAdView.findViewById<View>(R.id.secondary) as? TextView)
        if (bodyView != null && !nativeAd.body.isNullOrBlank()) {
            bodyView.text = nativeAd.body
            bodyView.visibility = View.VISIBLE
            nativeAdView.bodyView = bodyView
        } else {
            bodyView?.visibility = View.GONE
        }

        val ctaView = (nativeAdView.findViewById<View>(R.id.ad_call_to_action) as? TextView)
            ?: (nativeAdView.findViewById<View>(R.id.cta) as? TextView)
        if (ctaView != null) {
            ctaView.text = nativeAd.callToAction ?: ctaView.context.getString(R.string.install)
            ctaView.visibility = View.VISIBLE
            nativeAdView.callToActionView = ctaView
        }

        val iconView = (nativeAdView.findViewById<View>(R.id.icon) as? ImageView)
            ?: (nativeAdView.findViewById<View>(R.id.ad_app_icon) as? ImageView)
        val iconDrawable = nativeAd.icon?.drawable
        if (iconView != null && iconDrawable != null) {
            iconView.setImageDrawable(iconDrawable)
            iconView.visibility = View.VISIBLE
            nativeAdView.iconView = iconView
        } else {
            iconView?.visibility = View.GONE
        }

        val ratingView = nativeAdView.findViewById<View>(R.id.rating_bar) as? RatingBar
        if (ratingView != null && nativeAd.starRating != null && nativeAd.starRating!! > 0) {
            ratingView.rating = nativeAd.starRating!!.toFloat()
            ratingView.visibility = View.VISIBLE
            nativeAdView.starRatingView = ratingView
        } else {
            ratingView?.visibility = View.GONE
        }

        if (mediaView != null) {
            val mediaContent = nativeAd.mediaContent
            if (mediaContent != null) {
                mediaView.mediaContent = mediaContent
            }
            mediaView.visibility = View.VISIBLE
        }

        applyThemeStyles(nativeAdView)

        nativeAd.adEventCallback = object : NativeAdEventCallback {
            override fun onAdImpression() {
                Log.d(TAG, "👁️ [Native] Show impression recorded: headline='${nativeAd.headline}'")
            }

            override fun onAdClicked() {
                Log.d(TAG, "👆 [Native] Ad clicked: headline='${nativeAd.headline}'")
            }

            override fun onAdPaid(value: com.google.android.libraries.ads.mobile.sdk.common.AdValue) {
                Log.d(TAG, "💰 [Native] Ad paid: ${value.valueMicros} ${value.currencyCode}")
            }
        }

        nativeAdView.registerNativeAd(nativeAd, mediaView)
    }

    fun updateShimmerTheme(root: View, template: Int, isDark: Boolean) {
        val density = root.resources.displayMetrics.density
        val bgContainer = root.findViewById<View>(R.id.background)
            ?: root.findViewById<View>(R.id.shimmer_background)
            ?: (findShimmer(root)?.getChildAt(0))

        if (bgContainer != null) {
            if (template == 4) {
                val bgColor = if (isDark) Color.parseColor("#0E1320") else Color.WHITE
                bgContainer.setBackgroundColor(bgColor)
            } else {
                val bgColor = if (isDark) Color.parseColor("#121826") else Color.WHITE
                val borderColor = if (isDark) Color.parseColor("#2A3550") else Color.parseColor("#E2E8F0")
                val bgDrawable = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    setColor(bgColor)
                    setStroke((1 * density).toInt(), borderColor)
                    cornerRadius = 12f * density
                }
                bgContainer.background = bgDrawable
            }
        }
        tintShimmerPlaceholders(bgContainer ?: root, isDark)
    }

    fun tintShimmerPlaceholders(root: View, isDark: Boolean) {
        val darkPlaceholderColor = Color.parseColor("#232C44")
        val darkCtaPlaceholderColor = Color.parseColor("#2A3550")

        val lightPlaceholderColor = Color.parseColor("#D1D5DB")
        val lightCtaPlaceholderColor = Color.parseColor("#CBD5E1")

        fun setViewColor(view: View, color: Int) {
            val bg = view.background
            if (bg is GradientDrawable) {
                bg.setColor(color)
            } else {
                view.setBackgroundColor(color)
            }
        }

        fun applyTint(view: View) {
            if (view is ViewGroup) {
                for (i in 0 until view.childCount) {
                    applyTint(view.getChildAt(i))
                }
            } else {
                if (view.id == R.id.shimmer_close || view.id == R.id.btn_close_native_ad) {
                    return
                }
                when (view.id) {
                    R.id.shimmerCta -> {
                        val color = if (isDark) darkCtaPlaceholderColor else lightCtaPlaceholderColor
                        setViewColor(view, color)
                    }
                    R.id.shimmerMedia, R.id.shimmerIcon -> {
                        val color = if (isDark) darkPlaceholderColor else lightPlaceholderColor
                        setViewColor(view, color)
                    }
                    else -> {
                        val color = if (isDark) darkPlaceholderColor else lightPlaceholderColor
                        setViewColor(view, color)
                    }
                }
            }
        }

        if (root is ViewGroup) {
            for (i in 0 until root.childCount) {
                applyTint(root.getChildAt(i))
            }
        }
    }

    fun applyThemeStyles(nativeAdView: NativeAdView) {
        val isDark = ThemeManager.isDark(nativeAdView.context)
        val density = nativeAdView.resources.displayMetrics.density

        val isFullScreen = nativeAdView.id == R.id.native_ad_fullscreen_view ||
                nativeAdView.findViewById<View>(R.id.btn_close_native_ad) != null

        if (isFullScreen) {
            val bgColor = if (isDark) Color.parseColor("#0E1320") else Color.WHITE
            nativeAdView.setBackgroundColor(bgColor)
            nativeAdView.findViewById<View>(R.id.background)?.setBackgroundColor(bgColor)

            val closeBtn = nativeAdView.findViewById<ImageView>(R.id.btn_close_native_ad)
            closeBtn?.setColorFilter(if (isDark) Color.WHITE else Color.parseColor("#334155"))

            val headlineView = (nativeAdView.findViewById<View>(R.id.ad_headline) as? TextView)
                ?: (nativeAdView.findViewById<View>(R.id.primary) as? TextView)
            headlineView?.setTextColor(if (isDark) Color.parseColor("#EEF2F8") else Color.parseColor("#0F172A"))

            val advertiserView = (nativeAdView.findViewById<View>(R.id.ad_advertiser) as? TextView)
            advertiserView?.setTextColor(if (isDark) Color.parseColor("#8A94AA") else Color.parseColor("#64748B"))

            val bodyView = (nativeAdView.findViewById<View>(R.id.ad_body) as? TextView)
                ?: (nativeAdView.findViewById<View>(R.id.secondary) as? TextView)
            bodyView?.setTextColor(if (isDark) Color.parseColor("#8A94AA") else Color.parseColor("#475569"))
            return
        }

        val bgContainer = nativeAdView.findViewById<View>(R.id.background)
            ?: if (nativeAdView.childCount > 0) nativeAdView.getChildAt(0) else nativeAdView

        val bgColor = if (isDark) Color.parseColor("#121826") else Color.WHITE
        val borderColor = if (isDark) Color.parseColor("#2A3550") else Color.parseColor("#E2E8F0")

        val bgDrawable = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(bgColor)
            setStroke((1 * density).toInt(), borderColor)
            cornerRadius = 12f * density
        }
        bgContainer.background = bgDrawable

        val headlineView = (nativeAdView.findViewById<View>(R.id.ad_headline) as? TextView)
            ?: (nativeAdView.findViewById<View>(R.id.primary) as? TextView)
        headlineView?.setTextColor(if (isDark) Color.parseColor("#EEF2F8") else Color.parseColor("#111827"))

        val advertiserView = (nativeAdView.findViewById<View>(R.id.ad_advertiser) as? TextView)
        advertiserView?.setTextColor(if (isDark) Color.parseColor("#8A94AA") else Color.parseColor("#64748B"))

        val bodyView = (nativeAdView.findViewById<View>(R.id.ad_body) as? TextView)
            ?: (nativeAdView.findViewById<View>(R.id.secondary) as? TextView)
        bodyView?.setTextColor(if (isDark) Color.parseColor("#8A94AA") else Color.parseColor("#64748B"))
    }
}
