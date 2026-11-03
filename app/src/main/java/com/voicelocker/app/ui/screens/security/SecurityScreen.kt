package com.voicelocker.app.ui.screens.security

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.voicelocker.app.data.local.Store
import com.voicelocker.app.data.model.securityQuestions
import com.voicelocker.app.ui.components.AppButton
import com.voicelocker.app.ui.components.Muted
import com.voicelocker.app.ui.components.Page
import com.voicelocker.app.ui.components.RowCard
import com.voicelocker.app.ui.components.StepDots
import com.voicelocker.app.ui.navigation.go
import com.voicelocker.app.util.toast

@Composable
fun SecurityScreen(nav: NavHostController, flow: String) {
    val ctx = LocalContext.current
    val c = MaterialTheme.colorScheme
    var q by remember { mutableStateOf<String?>(null) }
    var answering by remember { mutableStateOf(false) }
    var a by remember { mutableStateOf("") }

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
                text = "Done",
                onClick = {
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
            RowCard(selected = q == s, onClick = { q = s }) {
                Text(s, Modifier.weight(1f))
                RadioButton(q == s, null)
            }
        } else {
            RowCard(onClick = { answering = false }) {
                Text("Choose another question", Modifier.weight(1f))
                Icon(Icons.Filled.ChevronRight, null)
            }
            RowCard(selected = true) {
                Text(q ?: "", Modifier.weight(1f))
                RadioButton(true, null)
            }
            OutlinedTextField(
                a, { a = it }, Modifier.fillMaxWidth(), placeholder = { Text("Enter your answer") }, minLines = 4,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = c.surface,
                    unfocusedContainerColor = c.surface,
                    unfocusedBorderColor = Color.Transparent
                )
            )
        }
    }
}

