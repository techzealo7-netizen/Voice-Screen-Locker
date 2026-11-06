package com.voicelocker.app.ui.screens.security

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.voicelocker.app.data.local.Store
import com.voicelocker.app.data.model.securityQuestions
import com.voicelocker.app.ui.components.AppButton
import com.voicelocker.app.ui.components.Muted
import com.voicelocker.app.ui.components.Page
import com.voicelocker.app.ui.components.StepDots
import com.voicelocker.app.ui.navigation.go
import com.voicelocker.app.ui.theme.InterFontFamily
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
        adScreenKey = "set_pin_lock_screen",
        nativeTemplate = 1,
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
                enabled = if (answering) a.trim().isNotBlank() else q != null,
                height = 52.dp
            )
        }
    ) {
        Spacer(Modifier.height(4.dp))
        StepDots(2)
        Spacer(Modifier.height(8.dp))

        Text(
            text = "Create your security question",
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp,
            color = c.onBackground
        )
        Muted(
            text = "Create a security question to help recover your voice password if needed.",
            size = 11,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, bottom = 14.dp),
            align = TextAlign.Center
        )

        if (!answering) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                securityQuestions.forEach { question ->
                    val isSelected = q == question
                    Surface(
                        onClick = { q = question },
                        shape = RoundedCornerShape(14.dp),
                        color = c.surface,
                        border = if (isSelected) BorderStroke(1.5.dp, c.primary) else BorderStroke(1.dp, c.outline),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = question,
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Normal,
                                fontSize = 14.sp,
                                color = c.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            Box(
                                modifier = Modifier.size(20.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clip(CircleShape)
                                            .background(c.primary)
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(18.dp)
                                            .clip(CircleShape)
                                            .border(1.5.dp, c.outline, CircleShape)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Card 1: Choose another question
            Surface(
                onClick = { answering = false },
                shape = RoundedCornerShape(14.dp),
                color = c.surface,
                border = BorderStroke(1.dp, c.outline),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Choose another question",
                        fontFamily = InterFontFamily,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal,
                        color = c.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.Filled.ChevronRight,
                        contentDescription = null,
                        tint = c.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // Card 2: Selected question with blue stroke & solid blue dot
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = c.surface,
                border = BorderStroke(1.5.dp, c.primary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = q ?: "",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 14.sp,
                        color = c.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Box(
                        modifier = Modifier.size(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(c.primary)
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // Card 3: Answer input area
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = c.surface,
                border = BorderStroke(1.dp, c.outline),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            ) {
                TextField(
                    value = a,
                    onValueChange = { a = it },
                    placeholder = {
                        Text(
                            text = "Enter your answer",
                            fontFamily = InterFontFamily,
                            fontSize = 14.sp,
                            color = Color(0xFF9DA3B4)
                        )
                    },
                    modifier = Modifier.fillMaxSize(),
                    textStyle = TextStyle(
                        fontFamily = InterFontFamily,
                        fontSize = 14.sp,
                        color = c.onSurface
                    ),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = c.surface,
                        unfocusedContainerColor = c.surface,
                        focusedTextColor = c.onSurface,
                        unfocusedTextColor = c.onSurface,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = c.primary
                    )
                )
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}
