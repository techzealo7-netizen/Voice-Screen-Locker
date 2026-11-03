package com.voicelocker.app

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.navigation.NavHostController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Hook for AI features. summarize() is a simple local fallback; translate() needs YOUR backend (never ship API keys in the app). */
object AiService {
    suspend fun summarize(text: String): String {
        val s = text.split(Regex("(?<=[.!?])\\s+")).filter { it.isNotBlank() }
        return if (s.size <= 2) text else s.take(2).joinToString(" ")
    }
    suspend fun translate(text: String, to: String): String = throw UnsupportedOperationException("Connect a translation backend in AiService.translate()")
}

@Composable
private fun NoteTile(n: Note, modifier: Modifier, onOpen: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit, onAi: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    Surface(onOpen, modifier, shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surface) {
        Box(Modifier.padding(6.dp)) {
            Box(Modifier.align(Alignment.TopEnd)) {
                IconButton({ menu = true }, Modifier.size(24.dp)) { Icon(Icons.Filled.MoreVert, "Menu", Modifier.size(16.dp)) }
                DropdownMenu(menu, { menu = false }) {
                    DropdownMenuItem({ Text("Edit") }, { menu = false; onEdit() }, leadingIcon = { Icon(Icons.Filled.Edit, null) })
                    DropdownMenuItem({ Text("Delete", color = MaterialTheme.colorScheme.error) }, { menu = false; onDelete() }, leadingIcon = { Icon(Icons.Filled.Delete, null, tint = MaterialTheme.colorScheme.error) })
                    DropdownMenuItem({ Text("AI Summary") }, { menu = false; onAi() }, leadingIcon = { Icon(Icons.Filled.AutoAwesome, null) })
                }
            }
            Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Filled.Description, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(34.dp))
                Text(n.title, fontSize = 12.sp, maxLines = 1)
                Muted(SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(n.created)), 9)
            }
        }
    }
}

@Composable
fun NotesTab(nav: NavHostController) {
    val ctx = LocalContext.current
    val c = MaterialTheme.colorScheme
    val notes = Store.notes
    var askMic by remember { mutableStateOf(false) }
    var del by remember { mutableStateOf<Note?>(null) }
    val micL = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { if (it) nav.navigate("noteEdit") else ctx.toast("Microphone permission is required.") }
    val cols = if (LocalConfiguration.current.screenWidthDp >= 600) 5 else 3

    TabColumn {
        Text("Voice Notes", Modifier.fillMaxWidth(), textAlign = TextAlign.Center, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        if (notes.isEmpty()) {
            Column(Modifier.fillMaxWidth().padding(top = 70.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Filled.SmartToy, null, Modifier.size(120.dp), tint = c.primaryContainer)
                Spacer(Modifier.height(12.dp)); Text("No voice notes available.", fontWeight = FontWeight.SemiBold)
                Muted("Start recording to create your first voice note.")
                Spacer(Modifier.height(40.dp)); AppButton("Create Now", { askMic = true })
            }
        } else {
            Spacer(Modifier.height(14.dp))
            ColCard(pad = 14.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) { Text("Voice Note", fontWeight = FontWeight.SemiBold); Muted("Private Voice Diary, only yours", 10) }
                    Button({ askMic = true }, colors = ButtonDefaults.buttonColors(containerColor = c.primary)) { Text("Create Now") }
                }
            }
            Row(Modifier.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.FolderOpen, null, tint = c.primary, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("My Notes", fontWeight = FontWeight.SemiBold)
            }
            notes.chunked(cols).forEach { row ->
                Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { n ->
                        NoteTile(n, Modifier.weight(1f).aspectRatio(1f), { nav.navigate("note/${n.id}") }, { nav.navigate("noteEdit?id=${n.id}") }, { del = n }, { nav.navigate("note/${n.id}?tab=1") })
                    }
                    repeat(cols - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
    if (askMic) AlertDialog(onDismissRequest = { askMic = false },
        icon = { MicButton(30.dp, Color.Transparent, c.primary) }, title = { Text("Allow Microphone Access", fontSize = 16.sp) },
        text = { Text("Allow microphone access to record and manage your voice notes.", fontSize = 12.sp, textAlign = TextAlign.Center) },
        confirmButton = { TextButton({ askMic = false; micL.launch(Manifest.permission.RECORD_AUDIO) }) { Text("Ok") } },
        dismissButton = { TextButton({ askMic = false }) { Text("Cancel") } })
    del?.let { n ->
        AlertDialog(onDismissRequest = { del = null },
            icon = { Icon(Icons.Filled.Delete, null, tint = c.error, modifier = Modifier.size(56.dp)) }, title = { Text("Delete Voice Note?", fontSize = 16.sp) },
            confirmButton = { TextButton({ Store.saveNotes(Store.notes.filter { it.id != n.id }); del = null }) { Text("Delete", color = c.error) } },
            dismissButton = { TextButton({ del = null }) { Text("Cancel") } })
    }
}

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
    LaunchedEffect(phase) { if (phase == 1) while (true) { delay(1000); secs++ } }

    fun listen() {
        speech.start(langByName(lang).tag, onPartial = { text = "$base $it".trim() },
            onFinal = { if (it.isNotBlank()) { base = "$base $it".trim(); text = base }; if (phase == 1) listen() },
            onError = { e -> if (phase == 1 && (e == 6 || e == 7)) listen() else if (phase == 1) { phase = if (text.isBlank()) 0 else 2; ctx.toast("Speech recognition stopped.") } })
    }
    fun start() { base = text.trim(); phase = 1; listen() }
    fun stop() { phase = if (text.isBlank()) 0 else 2; speech.finish() }
    fun save(title: String) {
        val list = Store.notes.toMutableList()
        if (existing != null) list.firstOrNull { it.id == existing.id }?.text = text
        else list.add(0, Note(System.currentTimeMillis().toString(), title.ifBlank { "New Note" }, text))
        Store.saveNotes(list); nav.popBackStack()
    }

    Page(if (phase == 0) "Voice Note" else "New Note", onBack = { speech.cancel(); nav.popBackStack() }, scroll = phase != 1, bottom = when (phase) {
        1 -> ({ AppButton("Stop", { stop() }, kind = Btn.Danger) })
        2 -> ({ AppButton("Continue", { start() }, kind = Btn.White); Spacer(Modifier.height(8.dp)); AppButton("Save", { if (existing != null) save("") else naming = true }, enabled = text.isNotBlank()) })
        else -> if (text.isNotBlank()) ({ AppButton("Save", { naming = true }) }) else null
    }) {
        when (phase) {
            1 -> ColCard(center = true, pad = 28.dp) {
                Waveform(bars = 28, height = 56.dp); Spacer(Modifier.height(14.dp)); Text("Listening..."); Spacer(Modifier.height(10.dp))
                Muted(text, 12, align = TextAlign.Center)
                Spacer(Modifier.height(10.dp)); Text(fmtTime(secs), fontSize = 12.sp)
            }
            else -> {
                OutlinedTextField(text, { text = it }, Modifier.fillMaxWidth(), minLines = 8, maxLines = 12, shape = RoundedCornerShape(14.dp),
                    placeholder = { Text("Type text here...") },
                    colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = c.surface, unfocusedContainerColor = c.surface, unfocusedBorderColor = Color.Transparent))
                if (phase == 0) {
                    Spacer(Modifier.height(24.dp))
                    Muted("Choose your language for better voice recognition.", 10, Modifier.fillMaxWidth(), TextAlign.Center)
                    Spacer(Modifier.height(8.dp))
                    val l = langByName(lang)
                    RowCard(onClick = { pickLang = true }) { Spacer(Modifier.weight(1f)); Text(l.flag, fontSize = 20.sp); Spacer(Modifier.width(8.dp)); Text(l.name); Icon(Icons.Filled.KeyboardArrowDown, null); Spacer(Modifier.weight(1f)) }
                    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        MicButton(60.dp, c.primaryContainer, c.primary, onClick = { start() }); Spacer(Modifier.height(10.dp)); Muted("00:00", 12)
                    }
                } else Text(fmtTime(secs), Modifier.fillMaxWidth().padding(top = 8.dp), textAlign = TextAlign.Center, fontSize = 12.sp)
            }
        }
    }
    if (pickLang) LanguageDialog(lang, { pickLang = false }) { lang = it; pickLang = false }
    if (naming) AlertDialog(onDismissRequest = { naming = false }, title = { Text("Note Name", fontSize = 16.sp) },
        text = { OutlinedTextField(name, { name = it }, singleLine = true, placeholder = { Text("Type note name...") }) },
        confirmButton = { TextButton({ naming = false; save(name) }) { Text("Save") } },
        dismissButton = { TextButton({ naming = false }) { Text("Cancel") } })
}

@Composable
fun NoteDetailScreen(nav: NavHostController, id: String, initialTab: Int) {
    val c = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()
    val note = Store.notes.firstOrNull { it.id == id }
    var tab by remember { mutableIntStateOf(initialTab) }
    var busy by remember { mutableStateOf(false) }
    var pick by remember { mutableStateOf(false) }
    var tLang by remember { mutableStateOf("English") }
    var translated by remember { mutableStateOf<String?>(null) }
    var menu by remember { mutableStateOf(false) }
    if (note == null) { LaunchedEffect(Unit) { nav.popBackStack() }; return }

    Page(note.title, onBack = { nav.popBackStack() }, actions = {
        Box {
            IconButton({ menu = true }) { Icon(Icons.Filled.MoreVert, "Menu") }
            DropdownMenu(menu, { menu = false }) {
                DropdownMenuItem({ Text("Delete", color = c.error) }, { menu = false; Store.saveNotes(Store.notes.filter { it.id != id }); nav.popBackStack() })
            }
        }
    }) {
        Row(Modifier.fillMaxWidth().padding(bottom = 14.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("Transcript", "AI Summary", "Translate").forEachIndexed { i, t ->
                Box(Modifier.weight(1f).height(36.dp).clip(RoundedCornerShape(9.dp)).background(if (tab == i) c.primary else c.surface).clickable { tab = i }, Alignment.Center) {
                    Text(t, fontSize = 12.sp, color = if (tab == i) Color.White else c.onSurfaceVariant)
                }
            }
        }
        when (tab) {
            0 -> ColCard { Text(note.text) }
            1 -> if (note.summary != null) ColCard { Text(note.summary!!) } else ColCard(center = true, pad = 28.dp) {
                Icon(Icons.Filled.Summarize, null, Modifier.size(70.dp), tint = c.primary); Spacer(Modifier.height(8.dp))
                Text("Generate AI Summary", fontWeight = FontWeight.SemiBold); Spacer(Modifier.height(16.dp))
                AppButton(if (busy) "Working…" else "Summarize", {
                    busy = true
                    scope.launch {
                        val s = AiService.summarize(note.text)
                        Store.saveNotes(Store.notes.map { if (it.id == id) it.apply { summary = s } else it }); busy = false
                    }
                }, enabled = !busy)
            }
            else -> {
                val l = langByName(tLang)
                ColCard(center = true, pad = 28.dp) {
                    Icon(Icons.Filled.Translate, null, Modifier.size(60.dp), tint = c.primary); Spacer(Modifier.height(8.dp))
                    Text("Select a language to translate this note", textAlign = TextAlign.Center); Spacer(Modifier.height(14.dp))
                    AppButton("${l.flag}  ${l.name}", { pick = true }, enabled = !busy)
                }
                translated?.let { ColCard { Text(it) } }
            }
        }
    }
    if (pick) LanguageDialog(tLang, { pick = false }) { sel ->
        pick = false; tLang = sel; busy = true
        scope.launch {
            translated = try { AiService.translate(note.text, sel) } catch (e: Exception) { "Translation is not connected yet. Implement AiService.translate()." }
            busy = false
        }
    }
}
