package com.voicelocker.app.ui.screens.security

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.voicelocker.ads.AppAdaptiveBannerTop
import com.voicelocker.ads.AppNativeAdView
import com.voicelocker.app.R
import com.voicelocker.app.data.local.Store
import com.voicelocker.app.ui.components.*
import com.voicelocker.app.ui.navigation.go
import com.voicelocker.app.ui.theme.InterFontFamily
import kotlinx.coroutines.delay

@Composable
fun PinScreen(nav: NavHostController, flow: String) {
    var stage by remember { mutableIntStateOf(1) } // 1 enter pin, 2 confirm pin
    var first by remember { mutableStateOf("") }
    var cur by remember { mutableStateOf("") }
    var err by remember { mutableStateOf(false) }
    var showPin by remember { mutableStateOf(true) }
    val from = if (flow == "voice") "pin" else "setup"
    val c = MaterialTheme.colorScheme
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp
    val isCompact = screenHeight < 820
    val isSmall = screenHeight < 720
    val isVerySmall = screenHeight < 660

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

    LaunchedEffect(Unit) {
        android.util.Log.d(
            "ADSMANAGER",
            "[set_pin_lock_screen] Set PIN Screen opened. Native bottom enabled=${com.voicelocker.ads.AdManager.isNativeEnabled("set_pin_lock_screen")}, template=1, fallback=small adaptive banner"
        )
    }

    Page(
        title = "Set Pin Lock",
        onBack = {
            if (stage == 2 && !err) {
                stage = 1
                cur = ""
                first = ""
            } else {
                nav.popBackStack()
            }
        },
        scroll = false,
        adScreenKey = "set_pin_lock_screen",
        nativeTemplate = 1,
        fallbackInlineBanner = false,
        bottom = {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = c.surface,
                border = BorderStroke(1.dp, c.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            top = if (isVerySmall) 4.dp else if (isSmall) 6.dp else 8.dp,
                            bottom = if (isVerySmall) 4.dp else if (isSmall) 6.dp else 8.dp
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    NumPad(
                        onDigit = { digit ->
                            if (err) {
                                cur = digit
                                err = false
                                stage = 1
                                first = ""
                            } else if (cur.length < 4) {
                                cur += digit
                            }
                        },
                        onDelete = {
                            if (err) {
                                cur = ""
                                err = false
                                stage = 1
                                first = ""
                            } else if (cur.isNotEmpty()) {
                                cur = cur.dropLast(1)
                            }
                        }
                    )
                }
            }
        }
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = if (isVerySmall) 2.dp else if (isSmall) 4.dp else 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                StepDots(step = if (err) 2 else stage, error = err)
                Spacer(Modifier.height(if (isVerySmall) 2.dp else if (isSmall) 4.dp else 8.dp))

                Text(
                    text = if (err) "Incorrect PIN. Please try again."
                           else if (stage == 1) "Enter your pin"
                           else "Confirm your pin",
                    color = if (err) c.error else c.onBackground,
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = if (isVerySmall) 13.sp else if (isSmall) 14.sp else 16.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(if (isVerySmall) 2.dp else if (isSmall) 4.dp else 6.dp))

                // Pin dots and Preview icon row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    PinDots(
                        filled = cur.length,
                        error = err,
                        digits = cur,
                        showPin = showPin
                    )

                    Spacer(Modifier.width(if (isSmall) 8.dp else 12.dp))

                    IconButton(
                        onClick = { showPin = !showPin },
                        modifier = Modifier.size(if (isSmall) 28.dp else 32.dp)
                    ) {
                        Image(
                            painter = painterResource(R.drawable.ic_preview),
                            contentDescription = if (showPin) "Hide PIN" else "Show PIN",
                            modifier = Modifier.size(if (isSmall) 16.dp else 18.dp)
                        )
                    }
                }

                if (err) {
                    Spacer(Modifier.height(if (isVerySmall) 4.dp else if (isSmall) 6.dp else 10.dp))
                    Button(
                        onClick = {
                            cur = ""
                            err = false
                            stage = 1
                            first = ""
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = c.primary),
                        modifier = Modifier
                            .height(if (isVerySmall) 32.dp else if (isSmall) 36.dp else 40.dp)
                            .width(if (isSmall) 105.dp else 120.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                    ) {
                        Text(
                            text = "Retry",
                            color = Color.White,
                            fontSize = if (isSmall) 13.sp else 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = InterFontFamily
                        )
                    }
                }
            }
        }
    }
}
