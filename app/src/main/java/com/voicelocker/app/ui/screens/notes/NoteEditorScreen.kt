package com.voicelocker.app.ui.screens.notes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.voicelocker.app.data.local.Store
import com.voicelocker.app.data.model.Note
import com.voicelocker.app.data.model.langByName
import com.voicelocker.app.ui.components.*
import com.voicelocker.app.util.fmtTime
import com.voicelocker.app.util.rememberSpeech
import com.voicelocker.app.util.toast
import kotlinx.coroutines.delay

@Composable
fun NoteEditorScreen(nav: NavHostController, id: String?) {
    val ctx = LocalContext.current
    val c = MaterialTheme.colorScheme
    val speech = rememberSpeech()
    val existing = remember { id?.let { i -> Store.notes.firstOrNull { it.id == i } } }
    var phase by remember { mutableIntStateOf(if (existing != null) 2 else 0) } // 0 idle, 1 listening, 2 edit
    var text by remember { mutableStateOf(existing?.text ?: "") }
    var base by remember { mutableStateOf("") }
    var secs by remember { mutableIntStateOf(0) }
    var lang by remember { mutableStateOf(Store.language) }
    var pickLang by remember { mutableStateOf(false) }
    var naming by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    LaunchedEffect(phase) {
        if (phase == 1) while (true) {
            delay(1000)
            secs++
        }
    }

    fun listen() {
        speech.start(
            langByName(lang).tag,
            onPartial = { text = "$base $it".trim() },
            onFinal = {
                if (it.isNotBlank()) {
                    base = "$base $it".trim()
                    text = base
                }
                if (phase == 1) listen()
            },
            onError = { e ->
                if (phase == 1 && (e == 6 || e == 7)) listen()
                else if (phase == 1) {
                    phase = if (text.isBlank()) 0 else 2
                    ctx.toast("Speech recognition stopped.")
                }
            }
        )
    }

    fun start() {
        base = text.trim()
        phase = 1
        listen()
    }

    fun stop() {
        phase = if (text.isBlank()) 0 else 2
        speech.finish()
    }

    fun save(title: String) {
        val list = Store.notes.toMutableList()
        if (existing != null) {
            list.firstOrNull { it.id == existing.id }?.text = text
        } else {
            list.add(0, Note(System.currentTimeMillis().toString(), title.ifBlank { "New Note" }, text))
        }
        Store.saveNotes(list)
        nav.popBackStack()
    }

    Page(
        if (phase == 0) "Voice Note" else "New Note",
        onBack = {
            speech.cancel()
            nav.popBackStack()
        },
        scroll = phase != 1,
        bottom = when (phase) {
            1 -> ({ AppButton("Stop", { stop() }, kind = Btn.Danger) })
            2 -> ({
                AppButton("Continue", { start() }, kind = Btn.White)
                Spacer(Modifier.height(8.dp))
                AppButton("Save", { if (existing != null) save("") else naming = true }, enabled = text.isNotBlank())
            })
            else -> if (text.isNotBlank()) ({ AppButton("Save", { naming = true }) }) else null
        }
    ) {
        when (phase) {
            1 -> ColCard(center = true, pad = 28.dp) {
                Waveform(bars = 28, height = 56.dp)
                Spacer(Modifier.height(14.dp))
                Text("Listening...")
                Spacer(Modifier.height(10.dp))
                Muted(text, 12, align = TextAlign.Center)
                Spacer(Modifier.height(10.dp))
                Text(fmtTime(secs), fontSize = 12.sp)
            }
            else -> {
                OutlinedTextField(
                    text, { text = it }, Modifier.fillMaxWidth(), minLines = 8, maxLines = 12,
                    shape = RoundedCornerShape(14.dp),
                    placeholder = { Text("Type text here...") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = c.surface,
                        unfocusedContainerColor = c.surface,
                        unfocusedBorderColor = Color.Transparent
                    )
                )
                if (phase == 0) {
                    Spacer(Modifier.height(24.dp))
                    Muted("Choose your language for better voice recognition.", 10, Modifier.fillMaxWidth(), TextAlign.Center)
                    Spacer(Modifier.height(8.dp))
                    val l = langByName(lang)
                    RowCard(onClick = { pickLang = true }) {
                        Spacer(Modifier.weight(1f))
                        Text(l.flag, fontSize = 20.sp)
                        Spacer(Modifier.width(8.dp))
                        Text(l.name)
                        Icon(Icons.Filled.KeyboardArrowDown, null)
                        Spacer(Modifier.weight(1f))
                    }
                    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        MicButton(60.dp, c.primaryContainer, c.primary, onClick = { start() })
                        Spacer(Modifier.height(10.dp))
                        Muted("00:00", 12)
                    }
                } else {
                    Text(fmtTime(secs), Modifier.fillMaxWidth().padding(top = 8.dp), textAlign = TextAlign.Center, fontSize = 12.sp)
                }
            }
        }
    }
    if (pickLang) LanguageDialog(lang, { pickLang = false }) {
        lang = it
        pickLang = false
    }
    if (naming) AlertDialog(
        onDismissRequest = { naming = false },
        title = { Text("Note Name", fontSize = 16.sp) },
        text = { OutlinedTextField(name, { name = it }, singleLine = true, placeholder = { Text("Type note name...") }) },
        confirmButton = { TextButton({ naming = false; save(name) }) { Text("Save") } },
        dismissButton = { TextButton({ naming = false }) { Text("Cancel") } }
    )
}

