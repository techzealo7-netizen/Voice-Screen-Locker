@file:OptIn(ExperimentalMaterial3Api::class)

package com.voicelocker.app.ui.screens.notes

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavHostController
import com.voicelocker.ads.AppAdaptiveBannerTop
import com.voicelocker.ads.AppNativeAdView
import com.voicelocker.ads.AppOpenAdManager
import com.voicelocker.ads.CounterAdManager
import com.voicelocker.app.R
import com.voicelocker.app.data.local.Store
import com.voicelocker.app.data.model.Note
import com.voicelocker.app.ui.components.*
import com.voicelocker.app.ui.theme.InterFontFamily
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
        shape = RoundedCornerShape(16.dp),
        color = Color.White
    ) {
        Box(Modifier.padding(10.dp)) {
            Box(Modifier.align(Alignment.TopEnd)) {
                IconButton({ menu = true }, Modifier.size(24.dp)) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = "Menu",
                        modifier = Modifier.size(18.dp),
                        tint = Color(0xFF8B93A7)
                    )
                }
                DropdownMenu(
                    expanded = menu,
                    onDismissRequest = { menu = false },
                    modifier = Modifier.background(Color.White)
                ) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "Edit",
                                fontFamily = InterFontFamily,
                                fontSize = 14.sp,
                                color = Color(0xFF1E2022)
                            )
                        },
                        onClick = { menu = false; onEdit() },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = null,
                                tint = Color(0xFF1E2022),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "Delete",
                                color = Color(0xFFEF4444),
                                fontFamily = InterFontFamily,
                                fontSize = 14.sp
                            )
                        },
                        onClick = { menu = false; onDelete() },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = null,
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "AI Summary",
                                fontFamily = InterFontFamily,
                                fontSize = 14.sp,
                                color = Color(0xFF1E2022)
                            )
                        },
                        onClick = { menu = false; onAi() },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.AutoAwesome,
                                contentDescription = null,
                                tint = Color(0xFF1E2022),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )
                }
            }
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(top = 10.dp, bottom = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_voice_note),
                    contentDescription = null,
                    modifier = Modifier.size(42.dp)
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = n.title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = InterFontFamily,
                    color = Color(0xFF1E2022),
                    maxLines = 1,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = SimpleDateFormat("EEEE d MMMM yyyy,\nhh:mma", Locale.getDefault()).format(Date(n.created)),
                    fontSize = 9.sp,
                    fontFamily = InterFontFamily,
                    color = Color(0xFF8B93A7),
                    textAlign = TextAlign.Center,
                    lineHeight = 12.sp
                )
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
    val micL = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        AppOpenAdManager.suppressAppOpenFor(5000L)
        if (granted) {
            nav.navigate("noteEdit")
        } else {
            askMic = true
        }
    }

    fun startCreate() {
        CounterAdManager.onAction(
            activity = ctx as? Activity,
            screenKey = "voice_note_screen",
            adKey = "interstitial"
        ) {
            val hasMic = ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
            if (hasMic) {
                nav.navigate("noteEdit")
            } else {
                AppOpenAdManager.suppressAppOpenFor(60_000L)
                micL.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }
    val cols = if (LocalConfiguration.current.screenWidthDp >= 600) 4 else 2

    LaunchedEffect(notes.size) {
        android.util.Log.d(
            "ADSMANAGER",
            "[voice_note_screen] Notes count: ${notes.size}. Native ad visible=${notes.isNotEmpty()} (template=2 Medium Native, fallback=adaptive banner)"
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Fixed Top Header & Banner
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(c.background)
        ) {
            Text(
                text = "Voice Notes",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 6.dp),
                textAlign = TextAlign.Center,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = InterFontFamily,
                color = c.onBackground
            )
            AppAdaptiveBannerTop("voice_note_screen")
        }

        // Scrollable Middle Content
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = MaxContentWidth)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                if (notes.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 60.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_voice_note),
                    contentDescription = null,
                    modifier = Modifier.size(130.dp)
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "No voice notes available.",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    fontFamily = InterFontFamily,
                    color = Color(0xFF1E2022)
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Start recording to create your first voice note.",
                    color = Color(0xFF8B93A7),
                    fontSize = 12.sp,
                    fontFamily = InterFontFamily,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(36.dp))
                AppButton("Create Now", { startCreate() })
            }
        } else {
            Spacer(Modifier.height(14.dp))
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = "Voice Note",
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = InterFontFamily,
                            fontSize = 16.sp,
                            color = Color(0xFF1E2022)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Private Voice Diary, only yours",
                            fontSize = 11.sp,
                            fontFamily = InterFontFamily,
                            color = Color(0xFF8B93A7)
                        )
                    }
                    Button(
                        onClick = { startCreate() },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = c.primary),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "Create Now",
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = Color.White
                        )
                    }
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.FolderOpen,
                    contentDescription = null,
                    tint = c.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "My Notes",
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = InterFontFamily,
                    fontSize = 16.sp,
                    color = Color(0xFF1E2022)
                )
            }
            notes.chunked(cols).forEach { row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    row.forEach { n ->
                        NoteTile(
                            n = n,
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f),
                            onOpen = {
                                CounterAdManager.onAction(
                                    activity = ctx as? Activity,
                                    screenKey = "voice_note_screen",
                                    adKey = "interstitial"
                                ) {
                                    nav.navigate("note/${n.id}")
                                }
                            },
                            onEdit = {
                                CounterAdManager.onAction(
                                    activity = ctx as? Activity,
                                    screenKey = "voice_note_screen",
                                    adKey = "interstitial"
                                ) {
                                    nav.navigate("noteEdit?id=${n.id}")
                                }
                            },
                            onDelete = { del = n },
                            onAi = {
                                CounterAdManager.onAction(
                                    activity = ctx as? Activity,
                                    screenKey = "voice_note_screen",
                                    adKey = "interstitial"
                                ) {
                                    nav.navigate("note/${n.id}?tab=1")
                                }
                            }
                        )
                    }
                    repeat(cols - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

    // Fixed Bottom Native Ad: Medium Native (template 2) with Fallback Adaptive Banner, ONLY when there is content on the screen
    if (notes.isNotEmpty()) {
        AppNativeAdView(
            screenKey = "voice_note_screen",
            template = 2,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp),
            fallbackInline = false
        )
    }
}

    if (askMic) {
        Dialog(
            onDismissRequest = { askMic = false },
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
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(c.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Mic,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(34.dp)
                        )
                    }

                    Spacer(Modifier.height(18.dp))

                    Text(
                        text = "Allow Microphone Access",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = InterFontFamily,
                        color = Color(0xFF1E2022),
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = "Allow microphone access to record and\nmanage your voice notes.",
                        fontSize = 12.sp,
                        fontFamily = InterFontFamily,
                        color = Color(0xFF8B93A7),
                        textAlign = TextAlign.Center,
                        lineHeight = 17.sp
                    )

                    Spacer(Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { askMic = false },
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
                                askMic = false
                                AppOpenAdManager.suppressAppOpenFor(60_000L)
                                runCatching {
                                    ctx.startActivity(
                                        Intent(
                                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                            Uri.parse("package:${ctx.packageName}")
                                        )
                                    )
                                }.onFailure {
                                    micL.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = c.primary,
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Text(
                                text = "Settings",
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

    del?.let { n ->
        Dialog(
            onDismissRequest = { del = null },
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
                            onClick = { del = null },
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
                                Store.saveNotes(Store.notes.filter { it.id != n.id })
                                del = null
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
