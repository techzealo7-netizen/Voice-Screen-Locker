package com.voicelocker.ads

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Dim black overlay rendered while full-screen App Open ads (start or resume)
 * are being presented, to hide underlying screen content and prevent accidental taps.
 */
@Composable
fun AppOpenBlackOverlay() {
    val isVisible by AppOpenAdManager.isOverlayVisible.collectAsStateWithLifecycle()

    if (isVisible) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xF0000000))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                )
        )
    }
}