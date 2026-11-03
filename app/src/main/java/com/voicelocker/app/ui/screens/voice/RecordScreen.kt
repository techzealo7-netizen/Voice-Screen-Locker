package com.voicelocker.app.ui.screens.voice

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.voicelocker.app.data.local.Store
import com.voicelocker.app.data.model.langByName
import com.voicelocker.app.ui.components.*
import com.voicelocker.app.ui.navigation.go
import com.voicelocker.app.util.rememberSpeech
import com.voicelocker.app.util.toast
import kotlinx.coroutines.delay

@Composable
fun RecordScreen(nav: NavHostController) {
    val ctx = LocalContext.current
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
        bottom = if (phase == 2) ({
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AppButton("Continue", {
                    Store.setVoicePhrase(text)
                    nav.go("voice", "record")
                }, Modifier.weight(3f))
                AppButton("Record Again", { text = ""; phase = 0 }, Modifier.weight(2f), kind = Btn.White)
            }
        }) else null
    ) {
        StepDots(1)
        Text(
            if (phase == 2) "Your Voice Key" else "Create Your Voice Lock",
            Modifier.fillMaxWidth().padding(vertical = 8.dp),
            textAlign = TextAlign.Center,
            color = if (phase == 2) c.primary else c.onSurface,
            fontWeight = FontWeight.Medium
        )
        ColCard(center = true, pad = 12.dp) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = if (phase == 2) 220.dp else 120.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(c.surfaceVariant)
                    .padding(10.dp)
            ) {
                Text(text.ifEmpty { "Recorded text will appear here" }, fontSize = 13.sp, color = if (text.isEmpty()) c.onSurfaceVariant else c.onSurface)
            }
            if (phase != 2) {
                Spacer(Modifier.height(22.dp))
                MicButton(
                    56.dp,
                    c.primaryContainer,
                    c.primary,
                    onClick = if (phase == 0) ({ record() }) else null,
                    icon = if (phase == 1) Icons.Filled.GraphicEq else Icons.Filled.Mic
                )
                Spacer(Modifier.height(10.dp))
                Text(if (phase == 1) "Listening… ${left}s" else "Tap to Record", fontWeight = FontWeight.SemiBold)
                Muted("Record your password within 5 seconds.", 10)
                Spacer(Modifier.height(6.dp))
            }
        }
    }
}

