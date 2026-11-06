package com.voicelocker.ads

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.voicelocker.ads.native_manager.NativeAdsManager
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd
import kotlinx.coroutines.delay

@Composable
fun FullScreenNativeAdView(
    adUnitId: String,
    preloadedAd: NativeAd? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val isDarkMode by ThemeManager.isDarkMode.collectAsState()
    val bgColor = if (isDarkMode) Color(0xFF0E1320) else Color.White

    var currentAd by remember(preloadedAd) { mutableStateOf(preloadedAd) }
    var isLoading by remember { mutableStateOf(preloadedAd == null) }

    // Intercept back button to dismiss ad cleanly
    BackHandler {
        onDismiss()
    }

    LaunchedEffect(adUnitId, preloadedAd) {
        if (preloadedAd == null && adUnitId.isNotBlank()) {
            isLoading = true
            NativeAdsManager.loadNativeAdObject(
                context = context,
                adUnitId = adUnitId,
                onSuccess = { loadedAd ->
                    android.util.Log.d("FullScreenNativeAd", "✅ [FullScreenNative] Ad loaded successfully: headline='${loadedAd.headline}'")
                    currentAd = loadedAd
                    isLoading = false
                },
                onError = {
                    android.util.Log.e("FullScreenNativeAd", "❌ [FullScreenNative] Failed to load native ad: adUnitId=$adUnitId")
                    isLoading = false
                    // If ad failed, dismiss to avoid blocking user
                    onDismiss()
                }
            )
        }
    }

    // Safety timeout: if loading takes > 5 seconds, auto-dismiss
    LaunchedEffect(isLoading) {
        if (isLoading) {
            delay(5000)
            if (isLoading && currentAd == null) {
                onDismiss()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            factory = { ctx ->
                val root = FrameLayout(ctx).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }

                val shimmerContainer = FrameLayout(ctx).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
                root.addView(shimmerContainer)

                val adContainer = FrameLayout(ctx).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
                root.addView(adContainer)

                if (currentAd != null) {
                    NativeAdsManager.hideDynamicShimmer(shimmerContainer)
                    NativeAdsManager.renderIntoContainer(
                        context = ctx,
                        nativeAd = currentAd!!,
                        container = adContainer,
                        layoutResId = NativeAdsManager.getTemplateLayout(4),
                        onClose = onDismiss
                    )
                } else {
                    NativeAdsManager.showDynamicShimmer(ctx, 4, shimmerContainer)
                }

                root
            },
            update = { root ->
                val shimmerContainer = root.getChildAt(0) as? FrameLayout
                val adContainer = root.getChildAt(1) as? FrameLayout

                if (currentAd != null && adContainer != null) {
                    NativeAdsManager.hideDynamicShimmer(shimmerContainer)
                    NativeAdsManager.renderIntoContainer(
                        context = root.context,
                        nativeAd = currentAd!!,
                        container = adContainer,
                        layoutResId = NativeAdsManager.getTemplateLayout(4),
                        onClose = onDismiss
                    )
                } else if (shimmerContainer != null && isLoading) {
                    NativeAdsManager.showDynamicShimmer(root.context, 4, shimmerContainer)
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}

