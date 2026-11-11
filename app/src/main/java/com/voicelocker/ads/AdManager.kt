package com.voicelocker.ads

import android.util.Log
import com.voicelocker.app.BuildConfig

object AdManager {
    private const val TAG = "AdManager"

    var isInitialized = false
        private set

    // ✅ Test IDs — only used in DEBUG mode
    object TestIds {
        const val BANNER = "ca-app-pub-3940256099942544/9214589741"
        const val ADAPTIVE_BANNER = "ca-app-pub-3940256099942544/9214589741"
        const val STANDARD_BANNER = "ca-app-pub-3940256099942544/6300978111"
        const val INTERSTITIAL = "ca-app-pub-3940256099942544/1033173712"
        const val REWARDED = "ca-app-pub-3940256099942544/5224354917"
        const val NATIVE = "ca-app-pub-3940256099942544/2247696110"
        const val APP_OPEN = "ca-app-pub-3940256099942544/9257395921"
        const val REWARDED_INTER = "ca-app-pub-3940256099942544/5354046379"
        const val MEDIUM_RECT = "ca-app-pub-3940256099942544/6300978111"
    }

    // =====================================================
    // VALIDATION
    // =====================================================

    private fun isValidId(id: String?): Boolean {
        return !id.isNullOrBlank() &&
                id.startsWith("ca-app-pub-") &&
                id.contains("/")
    }

    // =====================================================
    // BANNER ADS
    // ✅ Reads ID from Remote Config
    // =====================================================

    fun getBannerAdId(screenKey: String): String? {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "🧪 Banner[$screenKey] → TEST ID (${TestIds.ADAPTIVE_BANNER})")
            return TestIds.ADAPTIVE_BANNER
        }
        // ✅ First check if enabled in Remote Config
        if (!isBannerEnabled(screenKey)) {
            Log.d(TAG, "❌ Banner[$screenKey] disabled in Remote Config")
            return null
        }
        val id = RemoteConfigManager.getBannerAdId(screenKey)
            ?: RemoteConfigManager.getBannerAdId("home_screen")
            ?: RemoteConfigManager.getBannerAdId("splash")
        Log.d(TAG, "✅ Banner[$screenKey] → $id")
        return id
    }

    /**
     * Resolves banner ad unit ID for fallback display when native ad fails to load.
     * Always provides a valid ID (direct, cross-screen, or test) as long as ads/screens are enabled.
     */
    fun getFallbackBannerAdId(screenKey: String): String? {
        if (!RemoteConfigManager.allAdsEnabled) return null
        if (!RemoteConfigManager.isScreenEnabled(screenKey)) return null

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "🧪 FallbackBanner[$screenKey] → TEST ID (${TestIds.ADAPTIVE_BANNER})")
            return TestIds.ADAPTIVE_BANNER
        }

        // 1. Direct screen banner ID from remote config
        val directId = RemoteConfigManager.getBannerAdId(screenKey)
        if (!directId.isNullOrBlank() && isValidId(directId)) {
            Log.d(TAG, "✅ FallbackBanner[$screenKey] → direct ID: $directId")
            return directId
        }

        // 2. Candidate screens fallback
        val candidates = listOf("voice_note_screen", "theme_screen", "set_pattern_lock_screen", "set_pin_lock_screen", "voice_lock_screen", "voice_record_screen", "onboarding_screen", "language_screen", "home_screen", "splash")
        for (screen in candidates) {
            val id = RemoteConfigManager.getBannerAdId(screen)
            if (!id.isNullOrBlank() && isValidId(id)) {
                Log.d(TAG, "✅ FallbackBanner[$screenKey] → from $screen: $id")
                return id
            }
        }

        // 3. Fallback to test ID
        Log.d(TAG, "⚠️ FallbackBanner[$screenKey] → using TEST ID fallback")
        return TestIds.ADAPTIVE_BANNER
    }

    fun isBannerEnabled(screenKey: String): Boolean {
        if (!RemoteConfigManager.allAdsEnabled) return false
        if (!RemoteConfigManager.isScreenEnabled(screenKey)) return false
        val config = RemoteConfigManager.getAdConfig(screenKey, "inline_banner")
            ?: RemoteConfigManager.getAdConfig(screenKey, "adaptive_banner")
            ?: RemoteConfigManager.getAdConfig(screenKey, "adaptive_banner_top")
        if (config != null && !config.isEnabled) {
            Log.d(TAG, "❌ Banner[$screenKey] explicitly disabled in Remote Config")
            return false
        }
        val enabled = RemoteConfigManager.isBannerEnabled(screenKey)
        val fallback = if (!enabled && screenKey != "home_screen") {
            RemoteConfigManager.isBannerEnabled("home_screen")
        } else {
            enabled
        }
        Log.d(TAG, "isBannerEnabled($screenKey) = $fallback")
        return fallback
    }

    // =====================================================
    // INTERSTITIAL ADS (WATERFALL)
    // ✅ Reads IDs from Remote Config
    // =====================================================
    fun getInterstitialIds(
        screenKey: String,
        adKey: String = "interstitial"
    ): List<String> {
        // ✅ First check if enabled in Remote Config
        if (!isInterstitialEnabled(screenKey, adKey)) {
            Log.d(TAG, "❌ Interstitial[$screenKey][$adKey] disabled — returning []")
            return emptyList()
        }
        // ✅ Enabled — return test IDs in debug, real IDs in release
        return if (BuildConfig.DEBUG) {
            Log.d(TAG, "🧪 Interstitial[$screenKey][$adKey] → TEST IDs")
            listOf(TestIds.INTERSTITIAL)
        } else {
            val directIds = RemoteConfigManager.getInterstitialIds(screenKey, adKey)
                .filter { isValidId(it) }
            if (directIds.isNotEmpty()) {
                Log.d(TAG, "✅ Interstitial[$screenKey][$adKey] → $directIds")
                return directIds
            }
            if (screenKey == "home_screen") {
                val candidateKeys = listOf("inter_pin_lock", "inter_voice_lock", "inter_fingerprint_lock", "inter_pattern_lock", "inter_see_more", "interstitial_nth_click", "interstitial")
                for (k in candidateKeys) {
                    val ids = RemoteConfigManager.getInterstitialIds(screenKey, k).filter { isValidId(it) }
                    if (ids.isNotEmpty()) return ids
                }
            }
            // Fallback to home_screen candidate IDs for dashboard/ahead screens
            val homeCandidateKeys = listOf("inter_pin_lock", "inter_fingerprint_lock", "inter_pattern_lock", "inter_see_more", "interstitial_nth_click", "interstitial")
            for (k in homeCandidateKeys) {
                val ids = RemoteConfigManager.getInterstitialIds("home_screen", k).filter { isValidId(it) }
                if (ids.isNotEmpty()) return ids
            }
            val candidateScreens = listOf("theme_screen", "language_screen", "onboarding_screen", "splash")
            for (s in candidateScreens) {
                val ids = RemoteConfigManager.getInterstitialIds(s, "interstitial").filter { isValidId(it) }
                if (ids.isNotEmpty()) return ids
            }
            listOf(TestIds.INTERSTITIAL)
        }
    }

    // ✅ Get raw interstitial waterfall IDs regardless of enabled flag (for fallback)
    fun getRawInterstitialIds(screenKey: String = "splash", adKey: String = "interstitial"): List<String> {
        return if (BuildConfig.DEBUG) {
            listOf(TestIds.INTERSTITIAL)
        } else {
            val ids = RemoteConfigManager.getAdConfig(screenKey, adKey)?.ids?.toWaterfallList()
                ?.filter { isValidId(it) } ?: emptyList()
            ids.ifEmpty { listOf(TestIds.INTERSTITIAL) }
        }
    }

    fun isHomeInterstitialEnabled(): Boolean {
        if (!RemoteConfigManager.allAdsEnabled) return false
        if (!RemoteConfigManager.isScreenEnabled("home_screen")) return false
        return RemoteConfigManager.isAdEnabled("home_screen", "inter_voice_lock") ||
                RemoteConfigManager.isAdEnabled("home_screen", "inter_pin_lock") ||
                RemoteConfigManager.isAdEnabled("home_screen", "inter_fingerprint_lock") ||
                RemoteConfigManager.isAdEnabled("home_screen", "inter_pattern_lock") ||
                RemoteConfigManager.isAdEnabled("home_screen", "inter_see_more") ||
                RemoteConfigManager.isAdEnabled("home_screen", "interstitial") ||
                RemoteConfigManager.isAdEnabled("home_screen", "interstitial_nth_click")
    }

    fun isInterstitialEnabled(
        screenKey: String,
        adKey: String = "interstitial"
    ): Boolean {
        if (!RemoteConfigManager.allAdsEnabled) return false
        if (!RemoteConfigManager.isScreenEnabled(screenKey)) {
            Log.d(TAG, "❌ Screen[$screenKey] explicitly disabled in Remote Config")
            return false
        }

        // ✅ SECOND STRATEGY (Applies to dashboard and ahead screens for Interstitial ads only)
        if (RemoteConfigManager.isSecondStrategyEnabled() && SecondStrategyManager.isDashboardOrAheadScreen(screenKey)) {
            val eligible = SecondStrategyManager.canShowInterstitial()
            Log.d(TAG, "[SecondStrategy][$screenKey][$adKey] isInterstitialEnabled = $eligible")
            return eligible
        }

        val config = RemoteConfigManager.getAdConfig(screenKey, adKey)
        if (config != null && !config.isEnabled) {
            Log.d(TAG, "❌ Interstitial[$screenKey][$adKey] explicitly disabled in Remote Config")
            return false
        }

        if (screenKey == "home_screen" && (adKey == "interstitial" || adKey == "inter_voice_lock")) {
            val homeEnabled = isHomeInterstitialEnabled()
            Log.d(TAG, "isInterstitialEnabled(home_screen, $adKey) = $homeEnabled")
            return homeEnabled
        }

        val enabled = RemoteConfigManager.isAdEnabled(screenKey, adKey)
        Log.d(TAG, "isInterstitialEnabled($screenKey, $adKey) = $enabled")
        return enabled
    }
    // =====================================================
    // APP OPEN ADS
    // ✅ Reads ID from Remote Config
    // =====================================================

    fun getAppOpenAdId(): String? {
        // ✅ First check if enabled in Remote Config
        if (!isAppOpenEnabled()) {
            Log.d(TAG, "❌ App Open disabled in Remote Config")
            return null
        }
        // ✅ Enabled — return test ID in debug, real ID in release with test fallback
        return if (BuildConfig.DEBUG) {
            Log.d(TAG, "🧪 App Open → TEST ID")
            TestIds.APP_OPEN
        } else {
            val id = RemoteConfigManager.getAppOpenAdId()
            if (!id.isNullOrBlank() && isValidId(id)) {
                Log.d(TAG, "✅ App Open → $id")
                id
            } else {
                Log.w(TAG, "⚠️ App Open missing/invalid in Remote Config, fallback to test ID")
                TestIds.APP_OPEN
            }
        }
    }

    // ✅ Get raw app open ID regardless of enabled flag (for fallback)
    fun getRawAppOpenAdId(screenKey: String = "splash"): String? {
        return if (BuildConfig.DEBUG) {
            TestIds.APP_OPEN
        } else {
            val id = RemoteConfigManager.getAdConfig(screenKey, "app_open")?.id?.takeIf { isValidId(it) }
            id ?: TestIds.APP_OPEN
        }
    }

    fun isAppOpenEnabled(): Boolean {
        if (!RemoteConfigManager.allAdsEnabled) return false
        if (!RemoteConfigManager.isScreenEnabled("splash")) return false
        val enabled = RemoteConfigManager.isAdEnabled("splash", "app_open")
        Log.d(TAG, "isAppOpenEnabled() = $enabled")
        return enabled
    }
    // =====================================================
    // APP OPEN ADS — RESUME (separate from splash's app_open)
    // ✅ Reads ID from Remote Config
    // =====================================================

    fun getAppOpenResumeAdId(): String? {
        if (!isAppOpenResumeEnabled()) {
            Log.d(TAG, "❌ App Open Resume disabled in Remote Config")
            return null
        }
        return if (BuildConfig.DEBUG) {
            Log.d(TAG, "🧪 App Open Resume → TEST ID")
            TestIds.APP_OPEN
        } else {
            val id = RemoteConfigManager.getAppOpenResumeAdId()
            if (!id.isNullOrBlank() && isValidId(id)) {
                Log.d(TAG, "✅ App Open Resume → $id")
                id
            } else {
                Log.w(TAG, "⚠️ App Open Resume missing/invalid in Remote Config, fallback to test ID")
                TestIds.APP_OPEN
            }
        }
    }

    fun isAppOpenResumeEnabled(): Boolean {
        val enabled = RemoteConfigManager.isAdEnabled("app_open_resume", "app_open_resume")
        Log.d(TAG, "isAppOpenResumeEnabled() = $enabled")
        return enabled
    }
    // =====================================================
    // REWARDED ADS
    // (Add IDs to Remote Config later when needed)
    // =====================================================

    fun getRewardedAdId(screenKey: String): String = TestIds.REWARDED

    // =====================================================
    // NATIVE ADS
    // =====================================================

    fun isNativeEnabled(screenKey: String): Boolean {
        val enabled = RemoteConfigManager.isNativeEnabled(screenKey)
        Log.d(TAG, "isNativeEnabled($screenKey) = $enabled")
        return enabled
    }

    fun getNativeAdId(screenKey: String): String? {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "🧪 Native[$screenKey] → TEST ID")
            return TestIds.NATIVE
        }
        val id = RemoteConfigManager.getNativeAdId(screenKey) ?: RemoteConfigManager.getNativeAdId("home_screen")
        Log.d(TAG, "✅ Native[$screenKey] → $id")
        return id ?: TestIds.NATIVE
    }

    fun getNativeTemplate(screenKey: String): Int {
        return RemoteConfigManager.getNativeTemplate(screenKey)
    }

    fun isNativeCentreEnabled(screenKey: String = "home_screen"): Boolean {
        return RemoteConfigManager.isNativeCentreEnabled(screenKey)
    }

    fun isNativeBottomEnabled(screenKey: String = "home_screen"): Boolean {
        return RemoteConfigManager.isNativeBottomEnabled(screenKey)
    }

    // =====================================================
    // TOP ADAPTIVE BANNER ADS
    // =====================================================

    fun isAdaptiveBannerTopEnabled(screenKey: String): Boolean {
        if (!RemoteConfigManager.allAdsEnabled) return false
        if (!RemoteConfigManager.isScreenEnabled(screenKey)) return false
        val config = RemoteConfigManager.getAdConfig(screenKey, "adaptive_banner_top")
        if (config != null && !config.isEnabled) {
            Log.d(TAG, "❌ AdaptiveBannerTop[$screenKey] explicitly disabled in Remote Config")
            return false
        }
        val enabled = RemoteConfigManager.isAdaptiveBannerTopEnabled(screenKey)
        Log.d(TAG, "isAdaptiveBannerTopEnabled($screenKey) = $enabled")
        return enabled
    }

    fun getAdaptiveBannerTopId(screenKey: String): String? {
        if (!isAdaptiveBannerTopEnabled(screenKey)) {
            Log.d(TAG, "❌ AdaptiveBannerTop[$screenKey] disabled in Remote Config")
            return null
        }
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "🧪 AdaptiveBannerTop[$screenKey] → TEST ID")
            return TestIds.ADAPTIVE_BANNER
        }
        val id = RemoteConfigManager.getAdaptiveBannerTopId(screenKey)
        Log.d(TAG, "✅ AdaptiveBannerTop[$screenKey] → $id")
        return id
    }
    fun getNthClickInterstitialIds(screenKey: String): List<String> {
        return getInterstitialIds(screenKey, "interstitial_nth_click")
    }

    fun getClickThreshold(screenKey: String): Int {
        return RemoteConfigManager.getInterstitialClickThreshold(screenKey)
    }

    // =====================================================
    // PREMIUM DIALOG CROSS INTERSTITIAL AD
    // =====================================================

    fun getPremiumDialogCrossInterstitialIds(): List<String> {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "🧪 Premium Dialog Cross Interstitial → TEST ID")
            return listOf(TestIds.INTERSTITIAL)
        }
        val customId = RemoteConfigManager.getPremiumDialogCrossInterId()
        if (!customId.isNullOrBlank() && isValidId(customId)) {
            Log.d(TAG, "✅ Premium Dialog Cross Interstitial (Remote Config) → $customId")
            return listOf(customId)
        }
        val inAppIds = getRawInterstitialIds("in_app_screen", "interstitial")
        if (inAppIds.isNotEmpty()) return inAppIds
        val homeIds = getNthClickInterstitialIds("home_screen")
        if (homeIds.isNotEmpty()) return homeIds
        return getRawInterstitialIds("splash", "interstitial")
    }

    // =====================================================
    // REWARDED ADS
    // =====================================================

    fun isRewardedEnabled(): Boolean {
        if (!RemoteConfigManager.allAdsEnabled) return false
        return RemoteConfigManager.isRewardedEnabled()
    }

    fun getRewardedAdId(): String {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "🧪 Rewarded → TEST ID")
            return TestIds.REWARDED
        }
        val customId = RemoteConfigManager.getRewardedAdId()
        if (!customId.isNullOrBlank() && isValidId(customId)) {
            Log.d(TAG, "✅ Rewarded (Remote Config) → $customId")
            return customId
        }
        return TestIds.REWARDED
    }

    fun getRewardTimeHr(): Int {
        return RemoteConfigManager.getRewardTimeHr()
    }
}

enum class AdType {
    BANNER,
    MEDIUM_RECTANGLE,
    INTERSTITIAL,
    REWARDED,
    APP_OPEN,
    NATIVE,
    REWARDED_INTERSTITIAL
}