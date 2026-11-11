package com.voicelocker.ads

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun AdLoadingDialog(
    isVisible: Boolean,
    message: String = "Loading Ad...",
    forceDarkMode: Boolean? = null
) {
    if (!isVisible) return

    val isDarkModeState by ThemeManager.isDarkMode.collectAsStateWithLifecycle()
    val isDarkMode = forceDarkMode ?: isDarkModeState
    val cardBgColor = if (isDarkMode) Color(0xFF121826) else Color.White
    val textColor = if (isDarkMode) Color(0xFFF8FAFC) else Color(0xFF333333)
    val progressColor = if (isDarkMode) Color(0xFF60A5FA) else Color(0xFF1A73E8)
    val progressTrackColor = if (isDarkMode) Color(0xFF334155) else Color(0xFFE0E0E0)

    Dialog(
        onDismissRequest = { /* Non-dismissable */ },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .size(130.dp) // Square shape
                    .background(
                        color = cardBgColor,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(42.dp),
                    color = progressColor,
                    strokeWidth = 4.dp,
                    trackColor = progressTrackColor
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = message,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = textColor,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
