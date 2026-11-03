package com.voicelocker.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import com.voicelocker.app.data.local.Store
import com.voicelocker.app.ui.navigation.AppRoot
import com.voicelocker.app.ui.theme.VoiceLockTheme

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Store.init(this)
        setContent {
            VoiceLockTheme {
                AppRoot()
            }
        }
    }
}
