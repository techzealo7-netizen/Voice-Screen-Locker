package com.voicelocker.app.ui.screens.notes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Summarize
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavHostController
import com.voicelocker.app.data.local.Store
import com.voicelocker.app.domain.service.AiService
import com.voicelocker.app.ui.components.AppButton
import com.voicelocker.app.ui.components.Page
import com.voicelocker.app.ui.navigation.NavRoutes
import com.voicelocker.app.ui.theme.InterFontFamily
import kotlinx.coroutines.launch

@Composable
fun NoteDetailScreen(nav: NavHostController, id: String, initialTab: Int) {
    val c = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()
    val liveNote = Store.notes.firstOrNull { it.id == id }
    val initialNote = remember(id) { liveNote }
    val note = liveNote ?: initialNote

    var tab by remember { mutableIntStateOf(if (initialTab == 1) 1 else 0) }
    var busy by remember { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    if (note == null) {
        LaunchedEffect(Unit) {
            nav.popBackStack()
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(c.background)
        )
        return
    }

    LaunchedEffect(Unit) {
        android.util.Log.d(
            "ADSMANAGER",
            "[voice_note_screen] Voice Note Screen (NoteDetail) opened. Native bottom enabled=${com.voicelocker.ads.AdManager.isNativeEnabled("voice_note_screen")}, template=3 (Large Native), fallback=inline banner"
        )
    }

    Page(
        title = note.title,
        onBack = { nav.popBackStack() },
        adScreenKey = "voice_note_screen",
        nativeTemplate = 3,
        fallbackInlineBanner = true,
        actions = {
            Box {
                IconButton({ menu = true }) {
                    Icon(Icons.Filled.MoreVert, "Menu", tint = Color(0xFF1E2022))
                }
                DropdownMenu(
                    expanded = menu,
                    onDismissRequest = { menu = false },
                    modifier = Modifier.background(Color.White)
                ) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "Delete",
                                color = Color(0xFFEF4444),
                                fontFamily = InterFontFamily
                            )
                        },
                        onClick = {
                            menu = false
                            confirmDelete = true
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = null,
                                tint = Color(0xFFEF4444)
                            )
                        }
                    )
                }
            }
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("Transcript", "AI Summary").forEachIndexed { i, t ->
                val selected = tab == i
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selected) c.primary else Color.White)
                        .clickable { tab = i },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = t,
                        fontSize = 13.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                        fontFamily = InterFontFamily,
                        color = if (selected) Color.White else Color(0xFF8B93A7)
                    )
                }
            }
        }

        when (tab) {
            0 -> {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = note.text,
                        fontSize = 14.sp,
                        fontFamily = InterFontFamily,
                        color = Color(0xFF1E2022),
                        lineHeight = 20.sp,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
            1 -> {
                if (note.summary != null) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = note.summary!!,
                            fontSize = 14.sp,
                            fontFamily = InterFontFamily,
                            color = Color(0xFF1E2022),
                            lineHeight = 20.sp,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp, horizontal = 20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Summarize,
                                contentDescription = null,
                                modifier = Modifier.size(60.dp),
                                tint = c.primary
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = "Generate AI Summary",
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = InterFontFamily,
                                fontSize = 16.sp,
                                color = Color(0xFF1E2022)
                            )
                            Spacer(Modifier.height(20.dp))
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
                }
            }
        }
    }

    if (confirmDelete) {
        Dialog(
            onDismissRequest = { confirmDelete = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.White,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 28.dp, bottom = 20.dp, start = 20.dp, end = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = null,
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(52.dp)
                    )

                    Spacer(Modifier.height(16.dp))

                    Text(
                        text = "Delete Voice Note?",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = InterFontFamily,
                        color = Color(0xFF1E2022),
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { confirmDelete = false },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFCBE3FB),
                                contentColor = Color(0xFF1789F4)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Text(
                                text = "Cancel",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = InterFontFamily,
                                color = Color(0xFF1789F4)
                            )
                        }

                        Button(
                            onClick = {
                                confirmDelete = false
                                nav.popBackStack()
                                Store.saveNotes(Store.notes.filter { it.id != id })
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFEF4444),
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Text(
                                text = "Delete",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = InterFontFamily,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}
