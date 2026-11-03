package com.voicelocker.app.ui.screens.security

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.voicelocker.app.data.local.Store
import com.voicelocker.app.ui.components.*
import com.voicelocker.app.ui.navigation.go
import kotlinx.coroutines.delay

@Composable
fun PinScreen(nav: NavHostController, flow: String) {
    var stage by remember { mutableIntStateOf(1) }
    var first by remember { mutableStateOf("") }
    var cur by remember { mutableStateOf("") }
    var err by remember { mutableStateOf(false) }
    var showPin by remember { mutableStateOf(false) }
    val from = if (flow == "voice") "pin" else "setup"
    val c = MaterialTheme.colorScheme

    fun handleUndo() {
        if (err) {
            err = false
            cur = ""
        } else if (cur.isNotEmpty()) {
            cur = cur.dropLast(1)
        } else if (stage == 2) {
            stage = 1
            cur = first
            first = ""
        }
    }

    LaunchedEffect(cur) {
        if (cur.length == 4 && !err) {
            delay(200)
            if (stage == 1) {
                first = cur
                cur = ""
                stage = 2
            } else if (cur == first) {
                Store.setPin(cur)
                nav.go(flow, from)
            } else {
                err = true
            }
        }
    }

    Page(
        title = "Set Pin Lock",
        onBack = { nav.popBackStack() },
        scroll = false,
        actions = {
            IconButton(
                onClick = { handleUndo() },
                enabled = cur.isNotEmpty() || stage == 2 || err
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Undo,
                    contentDescription = "Undo",
                    tint = if (cur.isNotEmpty() || stage == 2 || err) c.primary else c.onSurfaceVariant.copy(alpha = 0.4f)
                )
            }
            IconButton(onClick = { showPin = !showPin }) {
                Icon(
                    if (showPin) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                    contentDescription = if (showPin) "Hide PIN" else "Show PIN",
                    tint = if (showPin) c.primary else c.onSurfaceVariant
                )
            }
        },
        bottom = {
            Box(Modifier.fillMaxWidth(), Alignment.Center) {
                NumPad(
                    onDigit = { if (cur.length < 4 && !err) cur += it },
                    onDelete = { if (!err && cur.isNotEmpty()) cur = cur.dropLast(1) },
                    onUndo = { handleUndo() }
                )
            }
        }
    ) {
        StepDots(stage, err)
        Column(Modifier.weight(1f).fillMaxWidth(), Arrangement.Center, Alignment.CenterHorizontally) {
            if (err) Text("Incorrect PIN. Please try again.", color = MaterialTheme.colorScheme.error)
            else Text(if (stage == 1) "Enter your pin" else "Confirm your pin")
            PinDots(cur.length, error = err, digits = cur, showPin = showPin)

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    Modifier
                        .clickable { showPin = !showPin }
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (showPin) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        null,
                        tint = if (showPin) c.primary else c.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (showPin) "Hide PIN" else "Show PIN",
                        fontSize = 12.sp,
                        color = if (showPin) c.primary else c.onSurfaceVariant
                    )
                }

                if (cur.isNotEmpty() || stage == 2 || err) {
                    Row(
                        Modifier
                            .clickable { handleUndo() }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Undo,
                            null,
                            tint = c.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Undo",
                            fontSize = 12.sp,
                            color = c.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            if (err) {
                Spacer(Modifier.height(10.dp))
                AppButton("Retry", { err = false; cur = ""; first = ""; stage = 1 }, Modifier.width(130.dp), kind = Btn.Primary)
            }
        }
    }
}

