@file:OptIn(ExperimentalMaterial3Api::class)

package com.voicelocker.app.ui.screens.notes

import android.app.Activity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavHostController
import com.voicelocker.ads.CounterAdManager
import com.voicelocker.app.data.local.Store
import com.voicelocker.app.data.model.Note
import com.voicelocker.app.data.model.langByName
import com.voicelocker.app.ui.components.*
import com.voicelocker.app.ui.navigation.NavRoutes
import com.voicelocker.app.ui.theme.InterFontFamily
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
        val act = ctx as? Activity
        CounterAdManager.onAction(
            activity = act,
            screenKey = "voice_note_screen",
            adKey = "interstitial"
        ) {
            val list = Store.notes.toMutableList()
            if (existing != null) {
                list.firstOrNull { it.id == existing.id }?.text = text
            } else {
                list.add(0, Note(System.currentTimeMillis().toString(), title.ifBlank { "New Note" }, text))
            }
            nav.popBackStack()
            Store.saveNotes(list)
        }
    }

    LaunchedEffect(Unit) {
        android.util.Log.d(
            "ADSMANAGER",
            "[voice_note_screen] New Note Screen (NoteEditor) opened. Native top enabled=${com.voicelocker.ads.AdManager.isNativeEnabled("voice_note_screen")}, template=2 (Medium Native), fallback=Medium Banner on top"
        )
    }

    Page(
        title = if (phase == 0) "Voice Note" else "New Note",
        onBack = {
            speech.cancel()
            nav.popBackStack()
        },
        adScreenKey = "voice_note_screen",
        nativeTemplate = 2,
        nativeAdOnTop = true,
        fallbackInlineBanner = true,
        actions = {},
        scroll = phase != 1,
        bottom = when (phase) {
            1 -> ({
                Button(
                    onClick = { stop() },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFEF4444),
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Text(
                        text = "Stop",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = InterFontFamily
                    )
                }
            })
            2 -> ({
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            CounterAdManager.onAction(
                                activity = ctx as? Activity,
                                screenKey = "voice_note_screen",
                                adKey = "interstitial"
                            ) {
                                start()
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = c.primary
                        ),
                        border = BorderStroke(1.dp, c.primary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Text(
                            text = "Continue",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = InterFontFamily,
                            color = c.primary
                        )
                    }

                    Button(
                        onClick = { if (existing != null) save("") else naming = true },
                        enabled = text.isNotBlank(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = c.primary,
                            contentColor = Color.White,
                            disabledContainerColor = Color(0xFFBFE0FE),
                            disabledContentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Text(
                            text = "Save",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = InterFontFamily,
                            color = Color.White
                        )
                    }
                }
            })
            else -> null
        }
    ) {
        when (phase) {
            1 -> {
                // Screen 2: Recording / Listening Card
                Spacer(Modifier.height(40.dp))
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 180.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp, horizontal = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Waveform(bars = 28, height = 56.dp, color = c.primary)
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "Listening...",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = InterFontFamily,
                            color = Color(0xFF1E2022)
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = fmtTime(secs),
                            fontSize = 12.sp,
                            fontFamily = InterFontFamily,
                            color = Color(0xFF8B93A7)
                        )
                    }
                }
            }
            else -> {
                // Screen 1 (idle) & Screen 3 (stopped text preview)
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 160.dp)
                ) {
                    Box(modifier = Modifier.padding(16.dp)) {
                        BasicTextField(
                            value = text,
                            onValueChange = { text = it },
                            modifier = Modifier.fillMaxWidth(),
                            cursorBrush = SolidColor(c.primary),
                            textStyle = TextStyle(
                                fontSize = 14.sp,
                                fontFamily = InterFontFamily,
                                color = Color(0xFF1E2022),
                                lineHeight = 20.sp
                            ),
                            decorationBox = { innerTextField ->
                                if (text.isEmpty()) {
                                    Text(
                                        text = "Type text here...",
                                        fontSize = 14.sp,
                                        fontFamily = InterFontFamily,
                                        color = Color(0xFF8B93A7)
                                    )
                                }
                                innerTextField()
                            }
                        )
                    }
                }

                if (phase == 0) {
                    Spacer(Modifier.height(28.dp))
                    Text(
                        text = "Choose your language for better voice\nrecognition.",
                        fontSize = 11.sp,
                        fontFamily = InterFontFamily,
                        color = Color(0xFF8B93A7),
                        textAlign = TextAlign.Center,
                        lineHeight = 16.sp,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(12.dp))
                    val l = langByName(lang)
                    Surface(
                        onClick = { pickLang = true },
                        shape = RoundedCornerShape(14.dp),
                        color = Color.White,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(l.flag, fontSize = 20.sp)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = l.name,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                fontFamily = InterFontFamily,
                                color = Color(0xFF1E2022)
                            )
                            Spacer(Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Filled.KeyboardArrowDown,
                                contentDescription = null,
                                tint = Color(0xFF1E2022)
                            )
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(68.dp)
                                .clip(CircleShape)
                                .background(c.primary)
                                .clickable { start() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Mic,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(34.dp)
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = "00:00",
                            fontSize = 12.sp,
                            fontFamily = InterFontFamily,
                            color = Color(0xFF8B93A7)
                        )
                    }
                } else {
                    // Phase 2: timer below text box
                    Text(
                        text = fmtTime(secs),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        textAlign = TextAlign.Center,
                        fontSize = 12.sp,
                        fontFamily = InterFontFamily,
                        color = Color(0xFF8B93A7)
                    )
                }
            }
        }
    }

    if (pickLang) {
        LanguageDialog(lang, { pickLang = false }) {
            lang = it
            pickLang = false
        }
    }

    if (naming) {
        Dialog(
            onDismissRequest = { naming = false },
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
                        .padding(horizontal = 20.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Note Name",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = InterFontFamily,
                        color = Color(0xFF1E2022),
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(16.dp))

                    TextField(
                        value = name,
                        onValueChange = { name = it },
                        singleLine = true,
                        placeholder = {
                            Text(
                                text = "Type note name...",
                                fontSize = 14.sp,
                                fontFamily = InterFontFamily,
                                color = Color(0xFF8B93A7)
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFFF0F6FE),
                            unfocusedContainerColor = Color(0xFFF0F6FE),
                            disabledContainerColor = Color(0xFFF0F6FE),
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        textStyle = TextStyle(
                            fontSize = 14.sp,
                            fontFamily = InterFontFamily,
                            color = Color(0xFF1E2022)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    )

                    Spacer(Modifier.height(20.dp))

                    Button(
                        onClick = {
                            naming = false
                            save(name)
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = c.primary,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text(
                            text = "Save",
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
