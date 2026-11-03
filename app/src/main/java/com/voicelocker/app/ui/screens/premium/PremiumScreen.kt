package com.voicelocker.app.ui.screens.premium

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.voicelocker.app.R
import com.voicelocker.app.ui.components.AppButton
import com.voicelocker.app.ui.components.Muted
import com.voicelocker.app.ui.components.Page
import com.voicelocker.app.ui.theme.CrownGold
import com.voicelocker.app.util.toast

@Composable
fun PremiumScreen(nav: NavHostController) {
    val c = MaterialTheme.colorScheme
    val ctx = LocalContext.current
    val config = LocalConfiguration.current
    val isCompact = config.screenHeightDp < 700

    var plan by remember { mutableIntStateOf(1) }
    val plans = listOf("Weekly" to "Rs 500/wk", "Monthly" to "Rs 1000/mo", "Yearly" to "Rs 1500/yr")

    Page(
        title = "",
        onBack = { nav.popBackStack() },
        scroll = false,
        bottom = {
            AppButton(
                "Start 3 days free trial",
                { ctx.toast("Connect Google Play Billing to enable purchases") },
                modifier = Modifier.height(if (isCompact) 44.dp else 48.dp)
            )
            Row(
                Modifier.fillMaxWidth().padding(top = if (isCompact) 4.dp else 8.dp),
                Arrangement.SpaceBetween
            ) {
                listOf("Privacy Policy", "Cancel", "Terms of use").forEach { Muted(it, if (isCompact) 9 else 10) }
            }
        }
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(bottom = 4.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_crown_home),
                    contentDescription = "Premium Crown",
                    modifier = Modifier
                        .width(if (isCompact) 56.dp else 68.dp)
                        .height(if (isCompact) 36.dp else 44.dp)
                )
                Spacer(Modifier.height(if (isCompact) 4.dp else 8.dp))
                Text(
                    "Go Premium",
                    color = Color(0xFF2979FF),
                    fontSize = if (isCompact) 20.sp else 23.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.2).sp
                )
                Text(
                    "Unlock All Feature",
                    color = Color(0xFF616161),
                    fontSize = if (isCompact) 11.sp else 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Column(Modifier.fillMaxWidth()) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = if (isCompact) 2.dp else 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(Modifier.weight(1f))
                    Text(
                        "PREMIUM",
                        fontSize = if (isCompact) 9.sp else 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF757575),
                        modifier = Modifier.width(if (isCompact) 52.dp else 58.dp),
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "BASIC",
                        fontSize = if (isCompact) 9.sp else 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF757575),
                        modifier = Modifier.width(if (isCompact) 44.dp else 48.dp),
                        textAlign = TextAlign.Center
                    )
                }

                val features = listOf(
                    Triple(Icons.Filled.Mic, "Voice Lock Screen", Pair(true, true)),
                    Triple(Icons.Filled.Lock, "Pin & Finger Lock options", Pair(true, true)),
                    Triple(Icons.Filled.Style, "Unlimited Themes", Pair(true, true)),
                    Triple(Icons.Filled.HeadsetMic, "VIP Customer Support", Pair(true, false)),
                    Triple(Icons.AutoMirrored.Filled.VolumeOff, "Remove Ads", Pair(true, false))
                )

                features.forEach { (icon, title, status) ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = if (isCompact) 3.dp else 5.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            icon,
                            contentDescription = null,
                            tint = CrownGold,
                            modifier = Modifier.size(if (isCompact) 16.dp else 18.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            title,
                            fontSize = if (isCompact) 11.sp else 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = c.onSurface,
                            modifier = Modifier.weight(1f)
                        )

                        Box(Modifier.width(if (isCompact) 52.dp else 58.dp), Alignment.Center) {
                            if (status.first) {
                                Icon(
                                    Icons.Filled.Check,
                                    contentDescription = "Included in Premium",
                                    tint = CrownGold,
                                    modifier = Modifier.size(if (isCompact) 16.dp else 18.dp)
                                )
                            } else {
                                Text("—", color = CrownGold, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }

                        Spacer(Modifier.width(6.dp))

                        Box(Modifier.width(if (isCompact) 44.dp else 48.dp), Alignment.Center) {
                            if (status.second) {
                                Icon(
                                    Icons.Filled.Check,
                                    contentDescription = "Included in Basic",
                                    tint = CrownGold,
                                    modifier = Modifier.size(if (isCompact) 16.dp else 18.dp)
                                )
                            } else {
                                Text("—", color = CrownGold, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }

            Column(Modifier.fillMaxWidth()) {
                plans.forEachIndexed { i, (n, p) ->
                    val isSelected = plan == i
                    Surface(
                        onClick = { plan = i },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = if (isCompact) 5.dp else 8.dp),
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) c.primaryContainer else c.surface,
                        border = if (isSelected) BorderStroke(1.2.dp, c.primary) else BorderStroke(0.8.dp, c.outlineVariant.copy(alpha = 0.5f))
                    ) {
                        Row(
                            Modifier.padding(horizontal = 12.dp, vertical = if (isCompact) 7.dp else 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                n,
                                Modifier.weight(1f),
                                fontSize = if (isCompact) 13.sp else 14.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                            )
                            Text(
                                p,
                                fontSize = if (isCompact) 13.sp else 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

