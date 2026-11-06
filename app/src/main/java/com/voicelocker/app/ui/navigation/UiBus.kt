package com.voicelocker.app.ui.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object UiBus {
    var success by mutableStateOf<String?>(null)
    var onDismissSuccess: (() -> Unit)? = null
}

