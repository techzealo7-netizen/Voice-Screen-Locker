@file:OptIn(ExperimentalMaterial3Api::class)

package com.voicelocker.app.ui.screens.security

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.voicelocker.ads.AppAdaptiveBannerTop
import com.voicelocker.ads.AppNativeAdView
import com.voicelocker.app.data.local.Store
import com.voicelocker.app.ui.components.*
import com.voicelocker.app.ui.navigation.go
import com.voicelocker.app.ui.theme.InterFontFamily
import com.voicelocker.app.util.biometricAuth
import com.voicelocker.app.util.findActivity

@Composable
fun FingerprintScreen(nav: NavHostController) {
    val act = LocalContext.current.findActivity()
    val c = MaterialTheme.colorScheme
    var st by remember { mutableIntStateOf(0) } // 0 idle, 1 failed, 2 ok
    var lastScanTime by remember { mutableLongStateOf(0L) }

    fun scan() {
        val now = System.currentTimeMillis()
        if (now - lastScanTime < 400L) return
        lastScanTime = now
        biometricAuth(act, "Enable fingerprint lock") { success ->
            st = if (success) 2 else 1
        }
    }

    Page(
        title = "Fingerprint Lock",
        onBack = { nav.popBackStack() },
        adScreenKey = "finger_print_screen",
        nativeTemplate = 1
    ) {
        StepDots(step = if (st == 2) 2 else 1, error = (st == 1))
        Spacer(Modifier.height(18.dp))

        val cardShape = RoundedCornerShape(16.dp)
        Surface(
            shape = cardShape,
            color = c.surface,
            border = BorderStroke(1.dp, c.outline),
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 200.dp)
                .clip(cardShape)
                .clickable(enabled = st != 2) { scan() }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp, horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Fingerprint,
                    contentDescription = null,
                    modifier = Modifier.size(72.dp),
                    tint = when (st) {
                        1 -> Color(0xFFEF4444)
                        2 -> Color(0xFF22C55E)
                        else -> Color(0xFF555B68)
                    }
                )
                Spacer(Modifier.height(16.dp))
                when (st) {
                    0 -> {
                        Text(
                            text = "Tab to Enable",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = InterFontFamily,
                            color = c.onSurface
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Use your fingerprint to secure your device",
                            color = Color(0xFF8B93A7),
                            fontSize = 11.sp,
                            fontFamily = InterFontFamily,
                            textAlign = TextAlign.Center
                        )
                    }
                    1 -> {
                        Text(
                            text = "Fingerprint Not Recognized.",
                            color = Color(0xFFEF4444),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = InterFontFamily,
                            textAlign = TextAlign.Center
                        )
                    }
                    else -> {
                        Text(
                            text = "Fingerprint scanned successfully",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = InterFontFamily,
                            color = c.onSurface,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Your fingerprint has been securely saved.",
                            color = Color(0xFF8B93A7),
                            fontSize = 11.sp,
                            fontFamily = InterFontFamily,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        if (st == 1) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Button(
                    onClick = { scan() },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFEF4444),
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .width(140.dp)
                        .height(44.dp)
                ) {
                    Text(
                        text = "Try Again",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = InterFontFamily
                    )
                }
            }
        } else if (st == 2) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {},
                    enabled = false,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        disabledContainerColor = Color(0xFFC5E1FB),
                        disabledContentColor = Color.White
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Text(
                        text = "Done",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = InterFontFamily,
                        color = Color.White
                    )
                }
                Button(
                    onClick = {
                        Store.fingerprintEnabled = true
                        nav.go("fp", "setup")
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
                        text = "Finish",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = InterFontFamily
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}
