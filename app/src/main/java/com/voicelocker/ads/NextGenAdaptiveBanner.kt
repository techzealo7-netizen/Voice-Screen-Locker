package com.voicelocker.ads

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.drawable.GradientDrawable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.voicelocker.app.BuildConfig
import com.voicelocker.app.R
import com.voicelocker.ads.native_manager.NativeAdsManager
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.libraries.ads.mobile.sdk.banner.AdSize
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAd
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdRequest
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadResult
import kotlinx.coroutines.delay

private val BannerHeight = 60.dp

private fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

/**
 * Calculates device physical screen diagonal in inches.
 * Returns true if diagonal >= 6.0 inches (considered Large device), false otherwise (Small device).
 */
fun isLargeDevice(context: Context): Boolean {
    val metrics = context.resources.displayMetrics
    val densityDpi = context.resources.configuration.densityDpi
    val xdpi = if (metrics.xdpi > 0) metrics.xdpi else densityDpi.toFloat()
    val ydpi = if (metrics.ydpi > 0) metrics.ydpi else densityDpi.toFloat()
    val widthInches = metrics.widthPixels.toDouble() / xdpi
    val heightInches = metrics.heightPixels.toDouble() / ydpi
    val diagonalInches = Math.sqrt(widthInches * widthInches + heightInches * heightInches)
    Log.d("NextGenAds", "📱 Device diagonal screen size: ${String.format("%.2f", diagonalInches)} inches (isLarge=${diagonalInches >= 6.0})")
    return diagonalInches >= 6.0
}

/**
 * Small inline banner with fixed height of 200 dp.
 */
@Composable
fun SmallInlineBanner(
    adUnitId: String,
    modifier: Modifier = Modifier,
    template: Int = 1,
    onAdLoaded: (() -> Unit)? = null,
    onAdFailed: (() -> Unit)? = null
) {
    ResponsiveInlineBanner(
        adUnitId = adUnitId,
        modifier = modifier,
        template = 1,
        onAdLoaded = onAdLoaded,
        onAdFailed = onAdFailed
    )
}

/**
 * Large inline banner with max height of 250 dp.
 */
@Composable
fun LargeInlineBanner(
    adUnitId: String,
    modifier: Modifier = Modifier,
    template: Int = 1,
    onAdLoaded: (() -> Unit)? = null,
    onAdFailed: (() -> Unit)? = null
) {
    ResponsiveInlineBanner(
        adUnitId = adUnitId,
        modifier = modifier,
        template = 2,
        onAdLoaded = onAdLoaded,
        onAdFailed = onAdFailed
    )
}

/**
 * Responsive inline banner that renders according to native ad template specifications:
 * - Template 1 (Small native): 130 dp maxHeight (min 100 dp)
 * - Template 2 (Medium native): 220 dp maxHeight (min 180 dp)
 * - Template 3 (Large native): 260 dp maxHeight (min 200 dp)
 */
@Composable
fun ResponsiveInlineBanner(
    adUnitId: String,
    modifier: Modifier = Modifier,
    template: Int = 1,
    onAdLoaded: (() -> Unit)? = null,
    onAdFailed: (() -> Unit)? = null
) {
    val exactHeight = when (template) {
        1 -> 130.dp
        2 -> 220.dp
        3 -> 260.dp
        else -> 130.dp
    }

    NextGenAdaptiveBanner(
        adUnitId = adUnitId,
        modifier = modifier
            .fillMaxWidth()
            .height(exactHeight),
        isInline = true,
        inlineMaxHeightDp = exactHeight.value.toInt(),
        template = template,
        onAdLoaded = onAdLoaded,
        onAdFailed = onAdFailed
    )
}

@Suppress("DEPRECATION")
@Composable
fun NextGenAdaptiveBanner(
    adUnitId: String,
    modifier: Modifier = Modifier,
    isInline: Boolean = false,
    inlineMaxHeightDp: Int = 150,
    template: Int = 1,
    onAdLoaded: (() -> Unit)? = null,
    onAdFailed: (() -> Unit)? = null
) {
    val context = LocalContext.current

    val billingManager = remember { BillingManager.getInstance(context) }
    val isPremium by billingManager.isPremium.collectAsStateWithLifecycle()

    if (isPremium) return

    val targetHeight = if (isInline) inlineMaxHeightDp.dp else BannerHeight

    BoxWithConstraints(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        val screenWidthDp = getScreenWidthDp(context)
        val availableWidthDp = if (maxWidth > 0.dp && maxWidth.isSpecified && maxWidth < 2000.dp) {
            maxWidth.value.toInt()
        } else {
            screenWidthDp
        }

        val widthForAd = if (isInline) {
            availableWidthDp.coerceAtLeast(320)
        } else {
            if (availableWidthDp in 280..screenWidthDp) availableWidthDp else screenWidthDp
        }

        val estimatedAdaptiveHeight = remember(widthForAd, isInline) {
            if (isInline) {
                inlineMaxHeightDp.dp
            } else {
                try {
                    val size = AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, widthForAd)
                    if (size.height > 0) size.height.dp else BannerHeight
                } catch (_: Exception) {
                    BannerHeight
                }
            }
        }

        var bannerAd by remember { mutableStateOf<BannerAd?>(null) }
        var isLoading by remember { mutableStateOf(true) }
        var isFailed by remember { mutableStateOf(false) }

        val targetHeight = if (isInline) inlineMaxHeightDp.dp else (bannerAd?.getAdSize()?.height?.dp ?: estimatedAdaptiveHeight)

        fun setLoadedAd(newAd: BannerAd) {
            val old = bannerAd
            newAd.adEventCallback = object : BannerAdEventCallback {
                override fun onAdImpression() {
                    Log.d("NextGenAds", "👁️ [Banner] Show impression recorded (inline=$isInline)")
                }

                override fun onAdClicked() {
                    Log.d("NextGenAds", "👆 [Banner] Ad clicked")
                }

                override fun onAdPaid(value: com.google.android.libraries.ads.mobile.sdk.common.AdValue) {
                    Log.d("NextGenAds", "💰 [Banner] Ad paid: ${value.valueMicros} ${value.currencyCode}")
                }

                override fun onAdFailedToShowFullScreenContent(error: com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError) {
                    Log.e("NextGenAds", "❌ [Banner] Failed to show: code=${error.code}, message='${error.message}'")
                }
            }
            bannerAd = newAd
            if (old != null && old != newAd) {
                try {
                    old.destroy()
                } catch (_: Exception) {}
            }
        }

        DisposableEffect(Unit) {
            onDispose {
                try {
                    bannerAd?.destroy()
                } catch (_: Exception) {}
                bannerAd = null
            }
        }

        LaunchedEffect(adUnitId, widthForAd, isInline, inlineMaxHeightDp) {
            isLoading = true
            isFailed = false

            // Ensure SDK is ready; auto-initialize if not yet started
            AdsInitializer.awaitReady(context)

            // In DEBUG mode, use ADAPTIVE_BANNER test ID (supports both inline and anchored adaptive)
            val effectiveAdUnitId = if (BuildConfig.DEBUG) {
                AdManager.TestIds.ADAPTIVE_BANNER
            } else {
                adUnitId.ifBlank { AdManager.TestIds.ADAPTIVE_BANNER }
            }

            val adSize = try {
                if (isInline) {
                    AdSize.getInlineAdaptiveBannerAdSize(widthForAd, inlineMaxHeightDp)
                } else {
                    AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, widthForAd)
                }
            } catch (e: Exception) {
                Log.w("NextGenAds", "Adaptive size computation failed, falling back: ${e.message}")
                if (isInline) {
                    AdSize.getInlineAdaptiveBannerAdSize(widthForAd, inlineMaxHeightDp)
                } else {
                    AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, screenWidthDp)
                }
            }

            Log.d("NextGenAds", "📡 [Banner] Request sent: id=$effectiveAdUnitId, size=${adSize.width}x${adSize.height}, inline=$isInline, containerWidthDp=$widthForAd")

            try {
                // Attempt 1: Load requested size (inline or anchored adaptive banner)
                val firstResult = BannerAd.load(BannerAdRequest.Builder(effectiveAdUnitId, adSize).build())
                if (firstResult is AdLoadResult.Success) {
                    setLoadedAd(firstResult.ad)
                    isLoading = false
                    isFailed = false
                    onAdLoaded?.invoke()
                    Log.d("NextGenAds", "✅ [Banner] Ad loaded successfully: id=$effectiveAdUnitId, size=${firstResult.ad.getAdSize().width}x${firstResult.ad.getAdSize().height}, inline=$isInline")
                    return@LaunchedEffect
                }

                val firstError = (firstResult as? AdLoadResult.Failure)?.error
                Log.w("NextGenAds", "⚠️ [Banner] Failed to load attempt 1 (code=${firstError?.code}, message='${firstError?.message}'). Requesting fallback...")

                // Attempt 2: If inline fails, try medium rectangle (if large enough) or anchored banner
                val fallbackSize = if (isInline) {
                    if (inlineMaxHeightDp >= 250) {
                        AdSize.MEDIUM_RECTANGLE
                    } else {
                        AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, widthForAd)
                    }
                } else {
                    // For top banner, must ALWAYS remain an anchored adaptive banner
                    AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, widthForAd)
                }

                val fallbackAdUnitId = if (BuildConfig.DEBUG) {
                    if (isInline && fallbackSize == AdSize.MEDIUM_RECTANGLE) AdManager.TestIds.MEDIUM_RECT else AdManager.TestIds.ADAPTIVE_BANNER
                } else {
                    effectiveAdUnitId
                }

                Log.d("NextGenAds", "📡 [Banner] Fallback request sent (Attempt 2): id=$fallbackAdUnitId, size=${fallbackSize.width}x${fallbackSize.height}")
                val fallbackResult = BannerAd.load(BannerAdRequest.Builder(fallbackAdUnitId, fallbackSize).build())
                if (fallbackResult is AdLoadResult.Success) {
                    setLoadedAd(fallbackResult.ad)
                    isLoading = false
                    isFailed = false
                    onAdLoaded?.invoke()
                    Log.d("NextGenAds", "✅ [Banner] Ad loaded successfully on fallback: size=${fallbackSize.width}x${fallbackSize.height}")
                    return@LaunchedEffect
                }

                val secondError = (fallbackResult as? AdLoadResult.Failure)?.error
                Log.w("NextGenAds", "⚠️ [Banner] Failed to load attempt 2 (code=${secondError?.code}, message='${secondError?.message}'). Requesting retry...")

                // Attempt 3: Final retry
                delay(1500)
                val retrySize = if (isInline) {
                    AdSize.BANNER
                } else {
                    // Top banner NEVER falls back to 320x50 fixed banner; remains anchored adaptive banner
                    AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, widthForAd)
                }
                val retryAdUnitId = if (BuildConfig.DEBUG) {
                    if (isInline) AdManager.TestIds.STANDARD_BANNER else AdManager.TestIds.ADAPTIVE_BANNER
                } else {
                    effectiveAdUnitId
                }

                Log.d("NextGenAds", "📡 [Banner] Retry request sent (Attempt 3): id=$retryAdUnitId, size=${retrySize.width}x${retrySize.height}")
                val retryResult = BannerAd.load(
                    BannerAdRequest.Builder(retryAdUnitId, retrySize).build()
                )
                if (retryResult is AdLoadResult.Success) {
                    setLoadedAd(retryResult.ad)
                    isLoading = false
                    isFailed = false
                    onAdLoaded?.invoke()
                    Log.d("NextGenAds", "✅ [Banner] Ad loaded successfully on retry: size=${retrySize.width}x${retrySize.height}")
                    return@LaunchedEffect
                }

                val finalError = (retryResult as? AdLoadResult.Failure)?.error
                Log.e("NextGenAds", "❌ [Banner] Failed to load: all banner attempts failed: code=${finalError?.code}, message='${finalError?.message}'")
                isLoading = false
                isFailed = true
                onAdFailed?.invoke()
            } catch (e: Throwable) {
                Log.e("NextGenAds", "❌ [Banner] Failed to load: Exception during load: ${e.message}", e)
                isLoading = false
                isFailed = true
                onAdFailed?.invoke()
            }
        }

        // Only collapse if loading is completely finished AND load has failed
        if (isFailed && !isLoading && bannerAd == null) {
            return@BoxWithConstraints
        }

        val isDark by ThemeManager.isDarkMode.collectAsStateWithLifecycle()
        val cardBg = if (isDark) Color(0xFF121826) else Color.White
        val cardBorder = if (isDark) Color(0xFF2A3550) else Color(0xFFE2E8F0)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            contentAlignment = Alignment.Center
        ) {
            when {
                bannerAd != null -> {
                    key(bannerAd) {
                        if (isInline) {
                            val exactCardHeight = when (template) {
                                1 -> 130.dp
                                2 -> 220.dp
                                3 -> 260.dp
                                else -> 130.dp
                            }
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(exactCardHeight),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = cardBg),
                                border = BorderStroke(1.dp, cardBorder)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(vertical = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    AndroidView(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .wrapContentHeight(),
                                        factory = { activityContext ->
                                            val activity = activityContext.findActivity()
                                                ?: context.findActivity()
                                                ?: (activityContext as? Activity)
                                                ?: (context as? Activity)

                                            val adView = com.google.android.libraries.ads.mobile.sdk.banner.AdView(activityContext).apply {
                                                layoutParams = android.widget.FrameLayout.LayoutParams(
                                                    android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                                                    android.widget.FrameLayout.LayoutParams.WRAP_CONTENT,
                                                    android.view.Gravity.CENTER
                                                )
                                                setBackgroundColor(if (isDark) android.graphics.Color.parseColor("#121826") else android.graphics.Color.WHITE)
                                            }

                                            if (activity != null && bannerAd != null) {
                                                try {
                                                    Log.d("NextGenAds", "▶️ [Banner] Show requested (registering banner with AdView)")
                                                    adView.registerBannerAd(bannerAd!!, activity)
                                                    adView.tag = bannerAd
                                                    Log.d("NextGenAds", "🎉 [Banner] Show success (banner registered with AdView)")
                                                } catch (e: Exception) {
                                                    Log.e("NextGenAds", "❌ [Banner] Failed to show: error registering banner with AdView: ${e.message}", e)
                                                }
                                            }
                                            adView
                                        },
                                        update = { adView ->
                                            adView.setBackgroundColor(if (isDark) android.graphics.Color.parseColor("#121826") else android.graphics.Color.WHITE)
                                            val activity = adView.context.findActivity()
                                                ?: context.findActivity()
                                                ?: (adView.context as? Activity)
                                                ?: (context as? Activity)
                                            if (activity != null && bannerAd != null && adView.tag != bannerAd) {
                                                try {
                                                    Log.d("NextGenAds", "▶️ [Banner] Show update requested")
                                                    adView.registerBannerAd(bannerAd!!, activity)
                                                    adView.tag = bannerAd
                                                    Log.d("NextGenAds", "🎉 [Banner] Show update success")
                                                } catch (e: Exception) {
                                                    Log.e("NextGenAds", "❌ [Banner] Failed to show update: ${e.message}", e)
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                        } else {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .wrapContentHeight(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = cardBg),
                                border = BorderStroke(1.dp, cardBorder)
                            ) {
                                AndroidView(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = targetHeight)
                                        .wrapContentHeight(),
                                    factory = { activityContext ->
                                        val activity = activityContext.findActivity()
                                            ?: context.findActivity()
                                            ?: (activityContext as? Activity)
                                            ?: (context as? Activity)

                                        val adView = com.google.android.libraries.ads.mobile.sdk.banner.AdView(activityContext).apply {
                                            layoutParams = android.widget.FrameLayout.LayoutParams(
                                                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                                                android.widget.FrameLayout.LayoutParams.WRAP_CONTENT,
                                                android.view.Gravity.CENTER
                                            )
                                            setBackgroundColor(if (isDark) android.graphics.Color.parseColor("#121826") else android.graphics.Color.WHITE)
                                        }

                                        if (activity != null && bannerAd != null) {
                                            try {
                                                Log.d("NextGenAds", "▶️ [Banner] Show requested (registering banner with AdView)")
                                                adView.registerBannerAd(bannerAd!!, activity)
                                                adView.tag = bannerAd
                                                Log.d("NextGenAds", "🎉 [Banner] Show success (banner registered with AdView)")
                                            } catch (e: Exception) {
                                                Log.e("NextGenAds", "❌ [Banner] Failed to show: error registering banner with AdView: ${e.message}", e)
                                            }
                                        }
                                        adView
                                    },
                                    update = { adView ->
                                        adView.setBackgroundColor(if (isDark) android.graphics.Color.parseColor("#121826") else android.graphics.Color.WHITE)
                                        val activity = adView.context.findActivity()
                                            ?: context.findActivity()
                                            ?: (adView.context as? Activity)
                                            ?: (context as? Activity)
                                        if (activity != null && bannerAd != null && adView.tag != bannerAd) {
                                            try {
                                                Log.d("NextGenAds", "▶️ [Banner] Show update requested")
                                                adView.registerBannerAd(bannerAd!!, activity)
                                                adView.tag = bannerAd
                                                Log.d("NextGenAds", "🎉 [Banner] Show update success")
                                            } catch (e: Exception) {
                                                Log.e("NextGenAds", "❌ [Banner] Failed to show update: ${e.message}", e)
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
                isLoading -> {
                    if (isInline) {
                        NativeShimmerPlaceholder(modifier = Modifier.fillMaxWidth(), template = template)
                    } else {
                        BannerShimmerPlaceholder(height = targetHeight)
                    }
                }
            }
        }
    }
}

private fun getScreenWidthDp(context: Context): Int {
    val metrics = context.resources.displayMetrics
    return (metrics.widthPixels / metrics.density).toInt()
}

/**
 * Native Ad Shimmer Placeholder:
 * Pixel-identical to the native ad shimmer layout (R.layout.layout_shimmer_native_small)
 * with a clean white/dark card, 12dp rounded corners, 1dp border, and NO background gray.
 */
@Composable
fun NativeShimmerPlaceholder(
    modifier: Modifier = Modifier,
    template: Int = 1
) {
    val isDark by ThemeManager.isDarkMode.collectAsStateWithLifecycle()

    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { ctx ->
            val root = FrameLayout(ctx).apply {
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }
            val shimmerRes = NativeAdsManager.getTemplateShimmer(template)
            LayoutInflater.from(ctx).inflate(shimmerRes, root, true)

            NativeAdsManager.updateShimmerTheme(root, template, isDark)

            val shimmer = root.findViewById<ShimmerFrameLayout>(R.id.shimmerNativeSmall)
                ?: root.findViewById<ShimmerFrameLayout>(R.id.shimmerNativeMedium)
                ?: root.findViewById<ShimmerFrameLayout>(R.id.shimmerNativeLarge)
                ?: (root.getChildAt(0) as? ShimmerFrameLayout)

            shimmer?.startShimmer()
            root
        },
        update = { root ->
            NativeAdsManager.updateShimmerTheme(root, template, isDark)
            val shimmer = root.findViewById<ShimmerFrameLayout>(R.id.shimmerNativeSmall)
                ?: root.findViewById<ShimmerFrameLayout>(R.id.shimmerNativeMedium)
                ?: root.findViewById<ShimmerFrameLayout>(R.id.shimmerNativeLarge)
                ?: (root.getChildAt(0) as? ShimmerFrameLayout)
            shimmer?.startShimmer()
        }
    )
}

/**
 * Loading shimmer placeholder for standard anchored adaptive banners (top toolbar / screen header).
 * Adapts between Light (clean white card with border) and Dark mode (clean black/dark card with border).
 */
@Composable
fun BannerShimmerPlaceholder(
    modifier: Modifier = Modifier,
    height: Dp = BannerHeight
) {
    val isDarkMode by ThemeManager.isDarkMode.collectAsStateWithLifecycle()
    val transition = rememberInfiniteTransition(label = "banner_shimmer_transition")
    val translateAnim by transition.animateFloat(
        initialValue = -300f,
        targetValue = 900f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "banner_shimmer_offset"
    )

    val shimmerColors = if (isDarkMode) {
        listOf(
            Color(0xFF121826),
            Color(0xFF1E293B),
            Color(0xFF2A3550),
            Color(0xFF1E293B),
            Color(0xFF121826)
        )
    } else {
        listOf(
            Color(0xFFE2E8F0),
            Color(0xFFF1F5F9),
            Color(0xFFCBD5E1),
            Color(0xFFF1F5F9),
            Color(0xFFE2E8F0)
        )
    }

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(translateAnim, translateAnim),
        end = Offset(translateAnim + 300f, translateAnim + 300f)
    )

    val cardBg = if (isDarkMode) Color(0xFF121826) else Color.White
    val cardBorder = if (isDarkMode) Color(0xFF2A3550) else Color(0xFFE2E8F0)
    val elementColor = if (isDarkMode) Color(0xFF232C44) else Color(0xFF94A3B8).copy(alpha = 0.25f)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(height),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = BorderStroke(1.dp, cardBorder)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(elementColor)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "AD",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDarkMode) Color(0xFF8A94AA) else Color(0xFF64748B)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Box(
                    modifier = Modifier
                        .width(120.dp)
                        .height(10.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(brush)
                )
            }
        }
    }
}
