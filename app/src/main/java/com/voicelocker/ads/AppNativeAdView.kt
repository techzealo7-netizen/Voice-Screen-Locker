package com.voicelocker.ads

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdView
import com.voicelocker.app.R
import com.voicelocker.ads.native_manager.NativeAdsManager

enum class AdDisplayState {
    LOADING_NATIVE,
    NATIVE_LOADED,
    FALLBACK_BANNER,
    HIDDEN
}

/**
 * Renders a Native Ad according to journey_b specifications:
 * - Uses native_template (1 = small, 2 = medium, 3 = large)
 * - Shows corresponding shimmer while loading
 * - Falls back to inline banner if native ad fails to load
 * - Hides/collapses container completely if both fail
 */
@Composable
fun AppNativeAdView(
    screenKey: String,
    modifier: Modifier = Modifier,
    template: Int? = null,
    isCenter: Boolean = false,
    isBottom: Boolean = false,
    fallbackInline: Boolean? = null,
    forceDarkMode: Boolean? = null
) {
    val context = LocalContext.current
    val effectiveForceDark = forceDarkMode ?: if (screenKey == "language_screen") false else null
    val isDarkState by ThemeManager.isDarkMode.collectAsStateWithLifecycle()
    val isDark = effectiveForceDark ?: isDarkState
    val billingManager = remember { BillingManager.getInstance(context) }
    val isPremium by billingManager.isPremium.collectAsState()
    if (isPremium) return

    val configVersion by RemoteConfigManager.configVersion.collectAsState()

    val isNativeEnabled = remember(screenKey, isCenter, isBottom, configVersion) {
        if (screenKey == "home_screen") {
            when {
                isCenter -> AdManager.isNativeCentreEnabled("home_screen")
                isBottom -> AdManager.isNativeBottomEnabled("home_screen")
                else -> AdManager.isNativeEnabled("home_screen")
            }
        } else {
            AdManager.isNativeEnabled(screenKey)
        }
    }

    val isBannerEnabled = remember(screenKey, configVersion) {
        AdManager.isBannerEnabled(screenKey)
    }

    // ✅ If neither native ad nor fallback banner is enabled in Remote Config, collapse completely
    if (!isNativeEnabled && !isBannerEnabled) return

    val adUnitId = remember(screenKey, configVersion) { AdManager.getNativeAdId(screenKey) }
    val templateType = remember(screenKey, template, configVersion) {
        template ?: AdManager.getNativeTemplate(screenKey)
    }

    val isInlineFallback = remember(screenKey, templateType, fallbackInline) {
        fallbackInline ?: when (screenKey) {
            "set_pin_lock_screen", "set_pattern_lock_screen", "theme_screen" -> false
            "voice_lock_screen", "voice_record_screen", "onboarding_screen", "language_screen", "splash" -> true
            else -> templateType > 1
        }
    }

    var displayState by remember(screenKey, isNativeEnabled, isBannerEnabled, adUnitId, templateType, configVersion) {
        mutableStateOf(
            if (isNativeEnabled && !adUnitId.isNullOrBlank()) AdDisplayState.LOADING_NATIVE
            else AdDisplayState.FALLBACK_BANNER
        )
    }

    if (displayState == AdDisplayState.HIDDEN) {
        return
    }

    val isSplash = screenKey == "splash"
    val verticalMargin = if (isSplash) 0.dp else 8.dp

    val adModifier = if (isBottom) {
        modifier.navigationBarsPadding()
    } else {
        modifier
    }

    Box(
        modifier = adModifier
            .fillMaxWidth()
            .padding(top = verticalMargin, bottom = verticalMargin),
        contentAlignment = Alignment.Center
    ) {
        when (displayState) {
            AdDisplayState.LOADING_NATIVE, AdDisplayState.NATIVE_LOADED -> {
                key(screenKey, templateType, adUnitId) {
                    AndroidView(
                        factory = { ctx ->
                            val root = FrameLayout(ctx).apply {
                                layoutParams = FrameLayout.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.WRAP_CONTENT
                                )
                            }

                            val shimmerLayout = FrameLayout(ctx).apply {
                                layoutParams = FrameLayout.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.WRAP_CONTENT
                                )
                            }
                            root.addView(shimmerLayout)

                            val adContainer = FrameLayout(ctx).apply {
                                layoutParams = FrameLayout.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.WRAP_CONTENT
                                )
                                visibility = android.view.View.GONE
                            }
                            root.addView(adContainer)

                            NativeAdsManager.loadNativeAd(
                                context = ctx,
                                adUnitId = adUnitId ?: "",
                                template = templateType,
                                container = adContainer,
                                shimmerContainer = shimmerLayout,
                                forceDark = effectiveForceDark,
                                listener = object : NativeAdsManager.NativeShowListener {
                                    override fun onLoaded() {
                                        android.util.Log.d("AppNativeAdView", "🎉 [NativeAdView][$screenKey] Native ad show success (template=$templateType)")
                                        displayState = AdDisplayState.NATIVE_LOADED
                                    }

                                    override fun onFailed() {
                                        val bannerType = if (isInlineFallback) "inline banner" else "small adaptive banner"
                                        android.util.Log.e("AppNativeAdView", "❌ [NativeAdView][$screenKey] Native ad failed to load (template=$templateType) — falling back to $bannerType")
                                        android.util.Log.d("ADSMANAGER", "[$screenKey][native] Native ad failed to load -> showing $bannerType as fallback")
                                        displayState = AdDisplayState.FALLBACK_BANNER
                                    }
                                }
                            )

                            root
                        },
                        update = { root ->
                            val currentDark = effectiveForceDark ?: ThemeManager.isDark(root.context)
                            val shimmerLayout = root.getChildAt(0) as? ViewGroup
                            val adContainer = root.getChildAt(1) as? ViewGroup

                            if (shimmerLayout != null && shimmerLayout.visibility == android.view.View.VISIBLE) {
                                NativeAdsManager.updateShimmerTheme(shimmerLayout, templateType, currentDark)
                            }
                            if (adContainer != null && adContainer.visibility == android.view.View.VISIBLE) {
                                val nativeAdView = (adContainer.getChildAt(0) as? NativeAdView)
                                    ?: adContainer.findViewById<NativeAdView>(R.id.native_ad_view)
                                    ?: adContainer.findViewById<NativeAdView>(R.id.native_ad_fullscreen_view)
                                if (nativeAdView != null) {
                                    NativeAdsManager.applyThemeStyles(nativeAdView, forceDark = effectiveForceDark)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            AdDisplayState.FALLBACK_BANNER -> {
                val bannerId = AdManager.getFallbackBannerAdId(screenKey)
                    ?: AdManager.getBannerAdId(screenKey)
                    ?: AdManager.getBannerAdId("home_screen")
                    ?: AdManager.getBannerAdId("splash")
                if (bannerId != null) {
                    if (isInlineFallback) {
                        android.util.Log.d("ADSMANAGER", "[$screenKey][banner] Displaying ResponsiveInlineBanner fallback with template $templateType")
                        ResponsiveInlineBanner(
                            adUnitId = bannerId,
                            modifier = Modifier.fillMaxWidth(),
                            template = templateType,
                            forceDarkMode = effectiveForceDark,
                            onAdLoaded = {
                                android.util.Log.d("ADSMANAGER", "[$screenKey][banner] Inline banner fallback loaded successfully")
                            },
                            onAdFailed = {
                                android.util.Log.e("ADSMANAGER", "[$screenKey][banner] Inline banner fallback failed to load -> hiding container")
                                displayState = AdDisplayState.HIDDEN
                            }
                        )
                    } else {
                        android.util.Log.d("ADSMANAGER", "[$screenKey][banner] Displaying NextGenAdaptiveBanner (small adaptive banner) fallback")
                        NextGenAdaptiveBanner(
                            adUnitId = bannerId,
                            modifier = Modifier.fillMaxWidth(),
                            isInline = false,
                            forceDarkMode = effectiveForceDark,
                            onAdLoaded = {
                                android.util.Log.d("ADSMANAGER", "[$screenKey][banner] Small adaptive banner fallback loaded successfully")
                            },
                            onAdFailed = {
                                android.util.Log.e("ADSMANAGER", "[$screenKey][banner] Small adaptive banner fallback failed to load -> hiding container")
                                displayState = AdDisplayState.HIDDEN
                            }
                        )
                    }
                } else {
                    android.util.Log.w("ADSMANAGER", "[$screenKey][banner] No banner ID available for fallback -> hiding container")
                    displayState = AdDisplayState.HIDDEN
                }
            }

            AdDisplayState.HIDDEN -> {
                // Collapsed completely
            }
        }
    }
}

/**
 * Top Adaptive Banner:
 * Placed immediately below the screen top bar/toolbar and above other screen content.
 */
@Composable
fun AppAdaptiveBannerTop(
    screenKey: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDark by ThemeManager.isDarkMode.collectAsStateWithLifecycle()
    val billingManager = remember { BillingManager.getInstance(context) }
    val isPremium by billingManager.isPremium.collectAsState()
    if (isPremium) return

    val configVersion by RemoteConfigManager.configVersion.collectAsState()

    val isTopBannerEnabled = remember(screenKey, configVersion) {
        AdManager.isAdaptiveBannerTopEnabled(screenKey)
    }

    if (!isTopBannerEnabled) return

    val adUnitId = remember(screenKey, configVersion) {
        AdManager.getAdaptiveBannerTopId(screenKey)
    }

    if (adUnitId.isNullOrBlank()) return

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        NextGenAdaptiveBanner(adUnitId = adUnitId)
    }
}
