package com.voicelocker.ads

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Marks periods where WE are intentionally showing a full-screen ad
 * (interstitial or app open). Showing any full-screen ad pauses/resumes
 * the host Activity — this guard lets AppOpenLifecycleTracker tell that
 * apart from a genuine user-initiated background/foreground transition.
 */
object AdActivityGuard {
    @Volatile private var isShowingAd = false
    private val scope = CoroutineScope(Dispatchers.Main)
    private var resetJob: Job? = null

    fun beginShowingAd() {
        isShowingAd = true
        resetJob?.cancel()
        // Safety net: if dismiss/fail callback never fires cleanly, self-clear
        resetJob = scope.launch {
            delay(60_000L)
            isShowingAd = false
        }
    }

    fun endShowingAd() {
        resetJob?.cancel()
        isShowingAd = false
    }

    /** Returns true if we were mid-ad, and clears the flag either way. */
    fun consumeIfShowingAd(): Boolean {
        val was = isShowingAd
        isShowingAd = false
        resetJob?.cancel()
        return was
    }
}