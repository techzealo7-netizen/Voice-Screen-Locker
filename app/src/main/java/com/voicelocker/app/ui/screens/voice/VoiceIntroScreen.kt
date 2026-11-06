package com.voicelocker.app.ui.screens.voice

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.voicelocker.ads.AppAdaptiveBannerTop
import com.voicelocker.ads.AppNativeAdView
import com.voicelocker.app.R
import com.voicelocker.app.data.local.Store
import com.voicelocker.app.data.model.langByName
import com.voicelocker.app.ui.components.*
import com.voicelocker.app.ui.theme.InterFontFamily

@Composable
fun VoiceIntroScreen(nav: NavHostController) {
    val c = MaterialTheme.colorScheme
    var isVoiceActive by remember { mutableStateOf(Store.voiceEnabled && Store.hasVoice) }
    var langDialogVisible by remember { mutableStateOf(false) }
    val currentLang = langByName(Store.language)

    LaunchedEffect(Store.voiceEnabled, Store.hasVoice) {
        isVoiceActive = Store.voiceEnabled && Store.hasVoice
    }

    val toggleVoice = {
        val next = !isVoiceActive
        if (next && !Store.hasVoice) {
            nav.navigate("perms")
        } else {
            Store.voiceEnabled = next
            isVoiceActive = next
        }
    }

    Page(
        title = "Voice Lock",
        onBack = { nav.popBackStack() },
        adScreenKey = "voice_lock_screen",
        nativeTemplate = 1
    ) {
        StepDots(if (isVoiceActive) 2 else 1)
        Spacer(Modifier.height(14.dp))

        // Active voice lock row with sleek switch
        Surface(
            onClick = toggleVoice,
            shape = RoundedCornerShape(16.dp),
            color = c.surface,
            border = BorderStroke(1.dp, c.outline),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Active voice lock",
                    modifier = Modifier.weight(1f),
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp,
                    color = c.onSurface
                )
                SleekSwitch(
                    checked = isVoiceActive,
                    onCheckedChange = { toggleVoice() }
                )
            }
        }

        // Center illustration with ic_voice_lock inside circular container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 32.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(176.dp)
                    .clip(CircleShape)
                    .background(c.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_voice_lock),
                    contentDescription = "Voice Lock",
                    modifier = Modifier.size(135.dp),
                    contentScale = ContentScale.Fit
                )
            }
        }

        // Language selection heading
        Text(
            text = "Choose Your Voice language",
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp,
            color = c.onBackground
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Select the right language for better voice recognition",
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            color = Color(0xFF8B93A7)
        )
        Spacer(Modifier.height(20.dp))

        // Dropdown card (centered flag, language name, and chevron down)
        Surface(
            onClick = { langDialogVisible = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp),
            color = c.surface,
            border = BorderStroke(1.dp, c.outline)
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = currentLang.flag,
                    fontSize = 20.sp
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = currentLang.name,
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp,
                    color = c.onSurface
                )
                Spacer(Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    tint = c.onSurface,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // Continue button (matching card style with themed text)
        Surface(
            onClick = {
                if (Store.hasVoice) nav.popBackStack() else nav.navigate("perms")
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp),
            color = c.surface,
            border = BorderStroke(1.dp, c.outline)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Continue",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color = c.onSurface
                )
            }
        }
        Spacer(Modifier.height(16.dp))
    }

    if (langDialogVisible) {
        LanguageDialog(
            selected = Store.language,
            onDismiss = { langDialogVisible = false },
            onConfirm = {
                Store.language = it
                langDialogVisible = false
            }
        )
    }
}
