package com.voicelocker.ads

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Marks periods where WE are intentionally showing a full-screen ad
 * (interstitial, rewarded, or app open). Showing any full-screen ad pauses/resumes
 * the host Activity — this guard lets AppOpenLifecycleTracker tell that
 * apart from a genuine user-initiated background/foreground transition.
 */
object AdActivityGuard {
    @Volatile
    var isShowingAd = false
        private set

    @Volatile
    var lastAdDismissedTime = 0L
        private set

    private val scope = CoroutineScope(Dispatchers.Main)
    private var resetJob: Job? = null

    fun beginShowingAd() {
        isShowingAd = true
        resetJob?.cancel()
        // Safety net: if dismiss/fail callback never fires cleanly, self-clear after 60s
        resetJob = scope.launch {
            delay(60_000L)
            isShowingAd = false
        }
    }

    fun endShowingAd() {
        resetJob?.cancel()
        isShowingAd = false
        lastAdDismissedTime = System.currentTimeMillis()
    }

    /**
     * Checks if a full-screen ad is currently showing or was dismissed within the last [bufferMs].
     */
    fun isAdActiveOrJustDismissed(bufferMs: Long = 3500L): Boolean {
        if (isShowingAd) return true
        if (lastAdDismissedTime > 0L && System.currentTimeMillis() - lastAdDismissedTime < bufferMs) {
            return true
        }
        return false
    }

    /** Returns true if we were mid-ad or just dismissed an ad, and clears the flag. */
    fun consumeIfShowingAd(): Boolean {
        if (isAdActiveOrJustDismissed()) {
            isShowingAd = false
            return true
        }
        return false
    }
}