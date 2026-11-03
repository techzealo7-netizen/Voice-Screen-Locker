package com.voicelocker.app.ui.screens.security

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.voicelocker.app.data.local.Store
import com.voicelocker.app.ui.components.Muted
import com.voicelocker.app.ui.components.Page
import com.voicelocker.app.ui.components.PatternPad
import com.voicelocker.app.ui.components.StepDots
import com.voicelocker.app.ui.navigation.go
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun PatternScreen(nav: NavHostController) {
    val scope = rememberCoroutineScope()
    var stage by remember { mutableIntStateOf(1) }
    var first by remember { mutableStateOf(listOf<Int>()) }
    var err by remember { mutableStateOf(false) }
    var hint by remember { mutableStateOf<String?>(null) }

    Page("Set Pattern Lock", onBack = { nav.popBackStack() }, scroll = false) {
        StepDots(stage, err)
        Column(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            if (err) {
                Text("Incorrect Pattern. Please try again.", color = MaterialTheme.colorScheme.error)
            } else {
                Text(if (stage == 1) "Enter your pattern" else "Confirm your pattern", fontWeight = FontWeight.Medium)
                Muted(hint ?: if (stage == 1) "Draw your pattern. It will be used if voice unlock fails." else "Redraw your pattern to confirm.", 11)
            }
        }
        Box(Modifier.weight(1f).fillMaxWidth(), Alignment.Center) {
            PatternPad(error = err, onComplete = { p ->
                when {
                    p.size < 4 -> hint = "Connect at least 4 dots."
                    stage == 1 -> scope.launch { delay(750); first = p; stage = 2; hint = null }
                    p == first -> { Store.setPattern(p); nav.go("pattern", "setup") }
                    else -> scope.launch { err = true; delay(900); err = false }
                }
            })
        }
    }
}

