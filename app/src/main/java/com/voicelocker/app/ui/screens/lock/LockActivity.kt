package com.voicelocker.app.ui.screens.lock

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import com.voicelocker.app.data.local.Store
import com.voicelocker.app.service.LockService
import com.voicelocker.app.ui.theme.VoiceLockTheme

/**
 * Full-screen lock shown over the lock screen / other apps. Back is disabled.
 */
class LockActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Store.init(this)
        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
        }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {}
        })
        setContent {
            VoiceLockTheme {
                LockScreen(
                    themeIndex = Store.themeIndex,
                    onUnlock = {
                        LockService.lastUnlock = System.currentTimeMillis()
                        finishAndRemoveTask()
                    }
                )
            }
        }
    }
}

