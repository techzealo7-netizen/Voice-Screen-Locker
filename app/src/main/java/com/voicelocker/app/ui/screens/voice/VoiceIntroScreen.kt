package com.voicelocker.app.ui.screens.voice

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.voicelocker.app.data.local.Store
import com.voicelocker.app.data.model.langByName
import com.voicelocker.app.ui.components.*

@Composable
fun VoiceIntroScreen(nav: NavHostController) {
    val c = MaterialTheme.colorScheme
    val on = Store.voiceEnabled && Store.hasVoice
    var lang by remember { mutableStateOf(false) }
    val l = langByName(Store.language)

    Page("Voice Lock", onBack = { nav.popBackStack() }) {
        StepDots(if (on) 2 else 1)
        RowCard(pad = 10.dp) {
            Text("Active voice lock", Modifier.weight(1f).padding(start = 6.dp))
            Switch(on, { v -> if (v && !Store.hasVoice) nav.navigate("perms") else Store.voiceEnabled = v })
        }
        Box(Modifier.fillMaxWidth().padding(vertical = 28.dp), Alignment.Center) {
            Box(Modifier.size(170.dp).clip(CircleShape).background(c.primaryContainer), Alignment.Center) {
                Icon(Icons.Filled.PhoneAndroid, null, Modifier.size(110.dp), tint = c.primary)
                Icon(Icons.Filled.Mic, null, Modifier.size(34.dp), tint = Color.White)
            }
        }
        Text("Choose Your Voice language", Modifier.fillMaxWidth(), textAlign = TextAlign.Center, fontWeight = FontWeight.SemiBold)
        Muted("Select the right language for better voice recognition", 11, Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 14.dp), TextAlign.Center)
        RowCard(onClick = { lang = true }) {
            Spacer(Modifier.weight(1f))
            Text(l.flag, fontSize = 22.sp)
            Spacer(Modifier.width(10.dp))
            Text(l.name)
            Icon(Icons.Filled.KeyboardArrowDown, null)
            Spacer(Modifier.weight(1f))
        }
        AppButton("Continue", { if (Store.hasVoice) nav.popBackStack() else nav.navigate("perms") }, kind = Btn.White)
        Spacer(Modifier.height(16.dp))
    }
    if (lang) LanguageDialog(Store.language, { lang = false }) { Store.language = it; lang = false }
}

