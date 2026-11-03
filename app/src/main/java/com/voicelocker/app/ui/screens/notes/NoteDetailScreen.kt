package com.voicelocker.app.ui.screens.notes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Summarize
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.*
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
import com.voicelocker.app.domain.service.AiService
import com.voicelocker.app.ui.components.AppButton
import com.voicelocker.app.ui.components.ColCard
import com.voicelocker.app.ui.components.LanguageDialog
import com.voicelocker.app.ui.components.Page
import kotlinx.coroutines.launch

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
    if (note == null) {
        LaunchedEffect(Unit) { nav.popBackStack() }
        return
    }

    Page(
        title = note.title,
        onBack = { nav.popBackStack() },
        actions = {
            Box {
                IconButton({ menu = true }) { Icon(Icons.Filled.MoreVert, "Menu") }
                DropdownMenu(menu, { menu = false }) {
                    DropdownMenuItem(
                        text = { Text("Delete", color = c.error) },
                        onClick = {
                            menu = false
                            Store.saveNotes(Store.notes.filter { it.id != id })
                            nav.popBackStack()
                        }
                    )
                }
            }
        }
    ) {
        Row(Modifier.fillMaxWidth().padding(bottom = 14.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("Transcript", "AI Summary", "Translate").forEachIndexed { i, t ->
                Box(
                    Modifier
                        .weight(1f)
                        .height(36.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(if (tab == i) c.primary else c.surface)
                        .clickable { tab = i },
                    Alignment.Center
                ) {
                    Text(t, fontSize = 12.sp, color = if (tab == i) Color.White else c.onSurfaceVariant)
                }
            }
        }
        when (tab) {
            0 -> ColCard { Text(note.text) }
            1 -> if (note.summary != null) {
                ColCard { Text(note.summary!!) }
            } else {
                ColCard(center = true, pad = 28.dp) {
                    Icon(Icons.Filled.Summarize, null, Modifier.size(70.dp), tint = c.primary)
                    Spacer(Modifier.height(8.dp))
                    Text("Generate AI Summary", fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(16.dp))
                    AppButton(
                        text = if (busy) "Working…" else "Summarize",
                        onClick = {
                            busy = true
                            scope.launch {
                                val s = AiService.summarize(note.text)
                                Store.saveNotes(Store.notes.map { if (it.id == id) it.apply { summary = s } else it })
                                busy = false
                            }
                        },
                        enabled = !busy
                    )
                }
            }
            else -> {
                val l = langByName(tLang)
                ColCard(center = true, pad = 28.dp) {
                    Icon(Icons.Filled.Translate, null, Modifier.size(60.dp), tint = c.primary)
                    Spacer(Modifier.height(8.dp))
                    Text("Select a language to translate this note", textAlign = TextAlign.Center)
                    Spacer(Modifier.height(14.dp))
                    AppButton("${l.flag}  ${l.name}", { pick = true }, enabled = !busy)
                }
                translated?.let { ColCard { Text(it) } }
            }
        }
    }
    if (pick) LanguageDialog(tLang, { pick = false }) { sel ->
        pick = false
        tLang = sel
        busy = true
        scope.launch {
            translated = try {
                AiService.translate(note.text, sel)
            } catch (e: Exception) {
                "Translation is not connected yet. Implement AiService.translate()."
            }
            busy = false
        }
    }
}

