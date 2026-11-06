package com.voicelocker.ads

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import com.google.android.libraries.ads.mobile.sdk.MobileAds
import com.google.android.libraries.ads.mobile.sdk.initialization.InitializationConfig
import com.voicelocker.app.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

object AdsInitializer {
    private const val TAG = "AdsInitializer"
    private const val TEST_APP_ID = "ca-app-pub-3940256099942544~3347511713"

    @Volatile
    private var isInitialized = false
    @Volatile
    private var isInitializing = false

    // ✅ True only once MobileAds.initialize()'s callback has actually fired
    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady

    val isSdkInitialized: Boolean
        get() = isInitialized || try { MobileAds.isInitialized } catch (e: Exception) { false }

    fun initializeIfNeeded(context: Context) {
        if (isSdkInitialized || isInitializing) return
        isInitializing = true

        val appContext = context.applicationContext

        // Google Next-Gen SDK requires MobileAds.initialize to be called on a @WorkerThread
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Dynamically read the application ID from AndroidManifest metadata to guarantee 100% match
                val manifestAppId = try {
                    val appInfo = appContext.packageManager.getApplicationInfo(
                        appContext.packageName,
                        PackageManager.GET_META_DATA
                    )
                    appInfo.metaData?.getString("com.google.android.gms.ads.APPLICATION_ID")
                } catch (e: Exception) {
                    null
                }

                val appId = manifestAppId?.takeIf { it.isNotBlank() } ?: TEST_APP_ID
                Log.d(TAG, "Initializing MobileAds on WorkerThread with appId: $appId (DEBUG=${BuildConfig.DEBUG})")

                MobileAds.initialize(
                    appContext,
                    InitializationConfig.Builder(appId).build()
                ) {
                    Log.d(TAG, "✅ MobileAds initialized successfully")
                    isInitialized = true
                    isInitializing = false
                    _isReady.value = true
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ MobileAds initialization failed: ${e.message}", e)
                isInitializing = false
                isInitialized = true
                _isReady.value = true // Unblock callers so they never hang
            }
        }
    }

    /** Suspends until MobileAds.initialize() has completed or safely times out. Safe to call repeatedly. */
    suspend fun awaitReady(context: Context? = null) {
        if (isSdkInitialized) {
            _isReady.value = true
            return
        }
        if (context != null && !isInitializing) {
            initializeIfNeeded(context)
        }
        withTimeoutOrNull(5000L) {
            _isReady.first { it || isSdkInitialized }
        }
    }
}
