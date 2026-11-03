package com.voicelocker.app.ui.screens.notes

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.voicelocker.app.data.local.Store
import com.voicelocker.app.data.model.Note
import com.voicelocker.app.ui.components.*
import com.voicelocker.app.util.toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
private fun NoteTile(
    n: Note,
    modifier: Modifier,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onAi: () -> Unit
) {
    var menu by remember { mutableStateOf(false) }
    Surface(
        onClick = onOpen,
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Box(Modifier.padding(6.dp)) {
            Box(Modifier.align(Alignment.TopEnd)) {
                IconButton({ menu = true }, Modifier.size(24.dp)) {
                    Icon(Icons.Filled.MoreVert, "Menu", Modifier.size(16.dp))
                }
                DropdownMenu(menu, { menu = false }) {
                    DropdownMenuItem(
                        text = { Text("Edit") },
                        onClick = { menu = false; onEdit() },
                        leadingIcon = { Icon(Icons.Filled.Edit, null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                        onClick = { menu = false; onDelete() },
                        leadingIcon = { Icon(Icons.Filled.Delete, null, tint = MaterialTheme.colorScheme.error) }
                    )
                    DropdownMenuItem(
                        text = { Text("AI Summary") },
                        onClick = { menu = false; onAi() },
                        leadingIcon = { Icon(Icons.Filled.AutoAwesome, null) }
                    )
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
    val micL = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        if (it) nav.navigate("noteEdit") else ctx.toast("Microphone permission is required.")
    }
    val cols = if (LocalConfiguration.current.screenWidthDp >= 600) 5 else 3

    TabColumn {
        Text("Voice Notes", Modifier.fillMaxWidth(), textAlign = TextAlign.Center, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        if (notes.isEmpty()) {
            Column(Modifier.fillMaxWidth().padding(top = 70.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Filled.SmartToy, null, Modifier.size(120.dp), tint = c.primaryContainer)
                Spacer(Modifier.height(12.dp))
                Text("No voice notes available.", fontWeight = FontWeight.SemiBold)
                Muted("Start recording to create your first voice note.")
                Spacer(Modifier.height(40.dp))
                AppButton("Create Now", { askMic = true })
            }
        } else {
            Spacer(Modifier.height(14.dp))
            ColCard(pad = 14.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Voice Note", fontWeight = FontWeight.SemiBold)
                        Muted("Private Voice Diary, only yours", 10)
                    }
                    Button({ askMic = true }, colors = ButtonDefaults.buttonColors(containerColor = c.primary)) {
                        Text("Create Now")
                    }
                }
            }
            Row(Modifier.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.FolderOpen, null, tint = c.primary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("My Notes", fontWeight = FontWeight.SemiBold)
            }
            notes.chunked(cols).forEach { row ->
                Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { n ->
                        NoteTile(
                            n,
                            Modifier.weight(1f).aspectRatio(1f),
                            onOpen = { nav.navigate("note/${n.id}") },
                            onEdit = { nav.navigate("noteEdit?id=${n.id}") },
                            onDelete = { del = n },
                            onAi = { nav.navigate("note/${n.id}?tab=1") }
                        )
                    }
                    repeat(cols - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
    if (askMic) AlertDialog(
        onDismissRequest = { askMic = false },
        icon = { MicButton(30.dp, Color.Transparent, c.primary) },
        title = { Text("Allow Microphone Access", fontSize = 16.sp) },
        text = { Text("Allow microphone access to record and manage your voice notes.", fontSize = 12.sp, textAlign = TextAlign.Center) },
        confirmButton = { TextButton({ askMic = false; micL.launch(Manifest.permission.RECORD_AUDIO) }) { Text("Ok") } },
        dismissButton = { TextButton({ askMic = false }) { Text("Cancel") } }
    )
    del?.let { n ->
        AlertDialog(
            onDismissRequest = { del = null },
            icon = { Icon(Icons.Filled.Delete, null, tint = c.error, modifier = Modifier.size(56.dp)) },
            title = { Text("Delete Voice Note?", fontSize = 16.sp) },
            confirmButton = {
                TextButton({
                    Store.saveNotes(Store.notes.filter { it.id != n.id })
                    del = null
                }) {
                    Text("Delete", color = c.error)
                }
            },
            dismissButton = { TextButton({ del = null }) { Text("Cancel") } }
        )
    }
}

