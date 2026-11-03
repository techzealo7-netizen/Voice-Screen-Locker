package com.voicelocker.app

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.navigation.NavHostController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SecurityScreen(nav: NavHostController, flow: String) {
    val ctx = LocalContext.current
    val c = MaterialTheme.colorScheme
    var q by remember { mutableStateOf<String?>(null) }
    var answering by remember { mutableStateOf(false) }
    var a by remember { mutableStateOf("") }
    
    // In edit mode (from settings), user is allowed to navigate back freely.
    // In setup flows, a security question is mandatory for account recovery.
    val isMandatory = flow != "edit"

    BackHandler(enabled = true) {
        if (answering) {
            answering = false
        } else if (isMandatory) {
            ctx.toast("Security question is required to secure your lock.")
        } else {
            nav.popBackStack()
        }
    }

    Page(
        title = "Security question",
        onBack = {
            if (answering) {
                answering = false
            } else if (isMandatory) {
                ctx.toast("Security question is required to secure your lock.")
            } else {
                nav.popBackStack()
            }
        },
        bottom = {
            AppButton(
                "Done",
                {
                    if (!answering) {
                        answering = true
                    } else {
                        Store.setSecurity(q!!, a.trim())
                        nav.go(flow, "security")
                    }
                },
                enabled = if (answering) a.trim().isNotBlank() else q != null
            )
        }
    ) {
        StepDots(2)
        Text(
            if (isMandatory) "Set Required Security Question" else "Create your security question",
            Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.SemiBold
        )
        Muted(
            "Security question is required to recover your lock screen if you ever forget your password.",
            11,
            Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 14.dp),
            TextAlign.Center
        )
        if (!answering) securityQuestions.forEach { s ->
            RowCard(selected = q == s, onClick = { q = s }) { Text(s, Modifier.weight(1f)); RadioButton(q == s, null) }
        } else {
            RowCard(onClick = { answering = false }) { Text("Choose another question", Modifier.weight(1f)); Icon(Icons.Filled.ChevronRight, null) }
            RowCard(selected = true) { Text(q ?: "", Modifier.weight(1f)); RadioButton(true, null) }
            OutlinedTextField(a, { a = it }, Modifier.fillMaxWidth(), placeholder = { Text("Enter your answer") }, minLines = 4, shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = c.surface, unfocusedContainerColor = c.surface, unfocusedBorderColor = Color.Transparent))
        }
    }
}

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
            // Undo back to stage 1 to change the first PIN
            stage = 1
            cur = first
            first = ""
        }
    }

    LaunchedEffect(cur) {
        if (cur.length == 4 && !err) {
            delay(200)
            if (stage == 1) { first = cur; cur = ""; stage = 2 }
            else if (cur == first) { Store.setPin(cur); nav.go(flow, from) }
            else err = true
        }
    }
    Page(
        "Set Pin Lock",
        onBack = { nav.popBackStack() },
        scroll = false,
        actions = {
            // Top App Bar Undo Button
            IconButton(
                onClick = { handleUndo() },
                enabled = cur.isNotEmpty() || stage == 2 || err
            ) {
                Icon(
                    Icons.Filled.Undo,
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
            
            // Actions row below PIN dots: Show/Hide and Undo
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
                            Icons.Filled.Undo,
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
            if (err) Text("Incorrect Pattern. Please try again.", color = MaterialTheme.colorScheme.error)
            else {
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

@Composable
fun FingerprintScreen(nav: NavHostController) {
    val act = LocalContext.current.findActivity()
    val c = MaterialTheme.colorScheme
    var st by remember { mutableIntStateOf(0) } // 0 idle, 1 failed, 2 ok
    fun scan() = biometricAuth(act, "Enable fingerprint lock") { st = if (it) 2 else 1 }
    Page("Fingerprint Lock", onBack = { nav.popBackStack() }) {
        StepDots(if (st == 2) 2 else 1, st == 1)
        Spacer(Modifier.height(12.dp))
        ColCard(center = true, onClick = if (st == 2) null else ({ scan() }), pad = 36.dp) {
            Icon(Icons.Filled.Fingerprint, null, Modifier.size(110.dp), tint = when (st) { 1 -> c.error; 2 -> Success; else -> c.onSurfaceVariant })
            Spacer(Modifier.height(12.dp))
            when (st) {
                0 -> { Text("Tab to Enable", fontSize = 18.sp, fontWeight = FontWeight.SemiBold); Muted("Use your fingerprint to unlock your device") }
                1 -> Text("Fingerprint Not Recognized.", color = c.error)
                else -> { Text("Fingerprint scanned successfully", fontWeight = FontWeight.SemiBold); Muted("Your fingerprint has been securely saved.") }
            }
        }
        if (st == 1) AppButton("Try Again", { scan() }, kind = Btn.Danger)
        if (st == 2) Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AppButton("Done", {}, Modifier.weight(1f), enabled = false)
            AppButton("Finish", { Store.fingerprintEnabled = true; nav.go("fp", "setup") }, Modifier.weight(1f))
        }
    }
}
