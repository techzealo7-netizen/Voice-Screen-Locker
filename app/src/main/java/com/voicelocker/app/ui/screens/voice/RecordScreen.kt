package com.voicelocker.app.ui.screens.voice

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.voicelocker.ads.AppAdaptiveBannerTop
import com.voicelocker.ads.AppNativeAdView
import com.voicelocker.ads.CounterAdManager
import com.voicelocker.app.data.local.Store
import com.voicelocker.app.data.model.langByName
import com.voicelocker.app.ui.components.*
import com.voicelocker.app.ui.navigation.go
import com.voicelocker.app.ui.theme.InterFontFamily
import com.voicelocker.app.util.rememberSpeech
import com.voicelocker.app.util.toast
import kotlinx.coroutines.delay

@Composable
fun RecordScreen(nav: NavHostController) {
    val ctx = LocalContext.current
    val act = ctx as? Activity
    val c = MaterialTheme.colorScheme
    val speech = rememberSpeech()
    var phase by remember { mutableIntStateOf(0) } // 0 idle, 1 listening, 2 done
    var text by remember { mutableStateOf("") }
    var left by remember { mutableIntStateOf(5) }

    LaunchedEffect(phase) {
        if (phase == 1) {
            left = 5
            while (left > 0) {
                delay(1000)
                left--
            }
            speech.finish()
        }
    }

    fun record() {
        text = ""
        phase = 1
        speech.start(
            langByName(Store.language).tag,
            onPartial = { text = it },
            onFinal = {
                if (it.isNotBlank()) text = it
                phase = if (text.isBlank()) 0 else 2
                if (text.isBlank()) ctx.toast("We could not hear you. Please try again.")
            },
            onError = {
                phase = if (text.isBlank()) 0 else 2
                ctx.toast("Could not recognise speech. Please try again.")
            }
        )
    }

    Page(
        title = "Voice Lock",
        onBack = { nav.popBackStack() },
        adScreenKey = "voice_record_screen",
        nativeTemplate = 1
    ) {
        StepDots(1)
        Spacer(Modifier.height(8.dp))

        Text(
            text = if (phase == 2) "Your Voice Key" else "Create Your Voice Lock",
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            textAlign = TextAlign.Center,
            color = if (phase == 2) c.primary else Color(0xFF1E2022),
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp
        )

        Spacer(Modifier.height(8.dp))

        // Main white card (consistent size across both phases)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(310.dp),
            shape = RoundedCornerShape(16.dp),
            color = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Inner gray text box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(135.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF2F2F2))
                        .padding(14.dp)
                ) {
                    Text(
                        text = text.ifEmpty { "Recorded text will appear here" },
                        fontSize = 14.sp,
                        fontFamily = InterFontFamily,
                        color = if (text.isEmpty()) Color(0xFF9DA3B4) else Color(0xFF1E2022)
                    )
                }

                if (phase != 2) {
                    Spacer(Modifier.height(18.dp))
                    MicButton(
                        size = 54.dp,
                        ring = Color(0xFFE5F1FE),
                        fill = c.primary,
                        onClick = if (phase == 0) ({ record() }) else null,
                        icon = if (phase == 1) Icons.Filled.GraphicEq else Icons.Filled.Mic
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = if (phase == 1) "Listening… ${left}s" else "Tap to Record",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        color = Color(0xFF1E2022)
                    )
                    Spacer(Modifier.height(3.dp))
                    Muted("Record your password within 5 seconds.", 11)
                }
            }
        }

        // Action buttons placed directly below the card in Phase 2
        if (phase == 2) {
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AppButton(
                    text = "Continue",
                    onClick = {
                        CounterAdManager.onAction(
                            activity = act,
                            screenKey = "voice_record_screen",
                            adKey = "interstitial"
                        ) {
                            Store.setVoicePhrase(text)
                            nav.go("voice", "record")
                        }
                    },
                    modifier = Modifier.weight(1f),
                    height = 52.dp,
                    kind = Btn.Primary
                )
                AppButton(
                    text = "Record Again",
                    onClick = {
                        CounterAdManager.onAction(
                            activity = act,
                            screenKey = "voice_record_screen",
                            adKey = "interstitial"
                        ) {
                            text = ""
                            phase = 0
                        }
                    },
                    modifier = Modifier.weight(1f),
                    height = 52.dp,
                    kind = Btn.White
                )
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}
