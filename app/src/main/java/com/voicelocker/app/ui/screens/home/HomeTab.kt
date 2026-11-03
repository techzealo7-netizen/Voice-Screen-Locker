package com.voicelocker.app.ui.screens.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.voicelocker.app.R
import com.voicelocker.app.data.local.Store
import com.voicelocker.app.ui.components.ColCard
import com.voicelocker.app.ui.components.Muted
import com.voicelocker.app.ui.components.TabColumn
import com.voicelocker.app.ui.components.ThemeGrid

@Composable
fun LockTileBtn(
    iconRes: Int,
    label: String,
    onClick: () -> Unit
) = Column(
    Modifier
        .clip(RoundedCornerShape(12.dp))
        .clickable(onClick = onClick)
        .padding(10.dp),
    horizontalAlignment = Alignment.CenterHorizontally
) {
    Image(
        painter = painterResource(iconRes),
        contentDescription = label,
        modifier = Modifier.size(32.dp)
    )
    Spacer(Modifier.height(6.dp))
    Text(label, fontSize = 11.sp, textAlign = TextAlign.Center)
}

@Composable
fun HomeTab(nav: NavHostController) {
    val c = MaterialTheme.colorScheme
    val active = Store.voiceEnabled && Store.hasVoice
    TabColumn {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Voice Lock Screen", Modifier.weight(1f), fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            IconButton({ nav.navigate("premium") }) {
                Image(
                    painter = painterResource(R.drawable.ic_crown_home),
                    contentDescription = "Premium",
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        ColCard(center = true, pad = 22.dp) {
            Image(
                painter = painterResource(R.drawable.ic_mic_home),
                contentDescription = "Voice Lock",
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .clickable { nav.navigate("voiceIntro") }
            )
            Spacer(Modifier.height(14.dp))
            Text(if (active) "Voice lock Active" else "Voice lock Inactive", fontWeight = FontWeight.Medium)
            Muted(if (active) "Listening for your unique voice lock key" else "Tap the mic to set up your voice lock", 10)
        }
        ColCard(pad = 8.dp) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceEvenly) {
                LockTileBtn(R.drawable.ic_lock, "Pin Lock") { nav.navigate("pin/pin") }
                LockTileBtn(R.drawable.ic_fingerprint, "Fingerprint Lock") { nav.navigate("fingerprint") }
                LockTileBtn(R.drawable.ic_pattern, "Pattern Lock") { nav.navigate("pattern") }
            }
        }
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            Text("Themes", Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
            Text("See more", color = c.primary, fontSize = 13.sp, modifier = Modifier.clickable { nav.navigate("themes") })
        }
        ThemeGrid(listOf(0, 1, 2, 3)) { nav.navigate("themePreview/$it") }
    }
}
