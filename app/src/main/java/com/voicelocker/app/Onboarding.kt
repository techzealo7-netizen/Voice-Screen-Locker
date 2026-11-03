package com.voicelocker.app

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(onDone: () -> Unit) {
    LaunchedEffect(Unit) { delay(2500); onDone() }
    Column(Modifier.fillMaxSize().background(Blue).systemBarsPadding(), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.weight(3f))
        Box(contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.Shield, null, Modifier.size(150.dp), tint = Color.White)
            Icon(Icons.Filled.Mic, null, Modifier.size(60.dp), tint = Color.White)
        }
        Spacer(Modifier.height(24.dp))
        Text("Voice Screen", color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Light)
        Box(Modifier.background(Color.White).padding(horizontal = 8.dp)) { Text("Locker", color = Blue, fontSize = 34.sp, fontWeight = FontWeight.Medium) }
        Spacer(Modifier.height(12.dp))
        Text("Secure Your Screen, Your Way", color = Color.White)
        Spacer(Modifier.weight(3f))
        CircularProgressIndicator(Modifier.size(28.dp), color = Color.White, strokeWidth = 2.dp)
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ObPage(title: String, body: String, art: @Composable () -> Unit) = Box(Modifier.fillMaxSize(), Alignment.TopCenter) {
    Column(Modifier.widthIn(max = MaxContentWidth).fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        art(); Spacer(Modifier.height(24.dp))
        Text(title, fontSize = 20.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp)); Muted(body, 12, align = TextAlign.Center)
    }
}

@Composable
private fun BlueCard(content: @Composable ColumnScope.() -> Unit) = Column(
    Modifier.fillMaxWidth(.7f).widthIn(max = 280.dp).aspectRatio(.8f).clip(RoundedCornerShape(22.dp)).background(Blue),
    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center, content = content,
)

@Composable
private fun WhiteMic() = Box(Modifier.size(110.dp).clip(CircleShape).background(Color.White), Alignment.Center) {
    Icon(Icons.Filled.Mic, null, Modifier.size(64.dp), tint = Blue)
}

@Composable
private fun LockTile(icon: ImageVector, title: String, sub: String) = Row(
    Modifier.fillMaxWidth().padding(bottom = 10.dp).clip(RoundedCornerShape(12.dp)).background(Blue).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
    Icon(icon, null, tint = Color.White, modifier = Modifier.size(30.dp)); Spacer(Modifier.width(16.dp))
    Column { Text(title, color = Color.White, fontWeight = FontWeight.SemiBold); Text(sub, color = Color.White.copy(.75f), fontSize = 11.sp) }
}

@Composable
private fun PhoneMockup(
    themeIdx: Int,
    isCenter: Boolean,
    modifier: Modifier = Modifier
) {
    val corner = if (isCenter) 26.dp else 20.dp
    val bezel = if (isCenter) 6.dp else 4.dp

    Box(
        modifier = modifier
            .shadow(if (isCenter) 20.dp else 10.dp, RoundedCornerShape(corner))
            .background(Color(0xFF1F1F1F), RoundedCornerShape(corner))
            .border(2.dp, Color(0xFF383838), RoundedCornerShape(corner))
            .padding(bezel)
    ) {
        ThemeBg(
            themeIdx,
            Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(corner - bezel))
        ) {
            // Subtle dark overlay to match lock screen
            Box(Modifier.fillMaxSize().background(Color.Black.copy(0.22f)))

            // Lock screen mockup contents
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = if (isCenter) 10.dp else 6.dp, vertical = if (isCenter) 8.dp else 5.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Dynamic island / notch mockup on center phone
                if (isCenter) {
                    Box(
                        Modifier
                            .width(38.dp)
                            .height(11.dp)
                            .clip(CircleShape)
                            .background(Color.Black)
                    )
                    Spacer(Modifier.height(6.dp))
                }

                // Clock
                Text("09", color = Color.White, fontSize = if (isCenter) 22.sp else 15.sp, fontWeight = FontWeight.Bold, lineHeight = if (isCenter) 20.sp else 14.sp)
                Text("05", color = Color.White, fontSize = if (isCenter) 22.sp else 15.sp, fontWeight = FontWeight.Bold, lineHeight = if (isCenter) 20.sp else 14.sp)
                Text("Monday, May 15", color = Color.White.copy(0.85f), fontSize = if (isCenter) 7.sp else 5.sp)

                Spacer(Modifier.weight(1f))

                // Mic icon
                Box(
                    Modifier
                        .size(if (isCenter) 30.dp else 22.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(0.25f)),
                    Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Mic,
                        null,
                        tint = Color.White,
                        modifier = Modifier.size(if (isCenter) 16.dp else 12.dp)
                    )
                }

                if (isCenter) {
                    Spacer(Modifier.height(3.dp))
                    Text("Private Voice Diary, Only yours", color = Color.White.copy(0.8f), fontSize = 5.sp)
                }

                Spacer(Modifier.height(if (isCenter) 6.dp else 4.dp))

                // Lock type shortcuts row
                Row(
                    Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(0.25f))
                        .padding(horizontal = 6.dp, vertical = 3.dp),
                    horizontalArrangement = Arrangement.spacedBy(if (isCenter) 6.dp else 4.dp)
                ) {
                    Icon(Icons.Filled.Dialpad, null, tint = Color.White, modifier = Modifier.size(if (isCenter) 12.dp else 9.dp))
                    Icon(Icons.Filled.Fingerprint, null, tint = Color.White, modifier = Modifier.size(if (isCenter) 12.dp else 9.dp))
                    Icon(Icons.Filled.Apps, null, tint = Color.White, modifier = Modifier.size(if (isCenter) 12.dp else 9.dp))
                }

                Spacer(Modifier.height(if (isCenter) 8.dp else 5.dp))

                // Apply button mockup
                Box(
                    Modifier
                        .fillMaxWidth(if (isCenter) 0.85f else 0.9f)
                        .height(if (isCenter) 18.dp else 12.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(Blue),
                    Alignment.Center
                ) {
                    Text("Apply", color = Color.White, fontSize = if (isCenter) 8.sp else 6.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun ThemesFannedShowcase() {
    Box(
        Modifier
            .fillMaxWidth()
            .height(290.dp),
        contentAlignment = Alignment.Center
    ) {
        // Far left (#4)
        PhoneMockup(
            themeIdx = 3,
            isCenter = false,
            modifier = Modifier
                .offset(x = (-118).dp)
                .width(108.dp)
                .height(210.dp)
                .zIndex(1f)
        )

        // Mid left (#2)
        PhoneMockup(
            themeIdx = 1,
            isCenter = false,
            modifier = Modifier
                .offset(x = (-66).dp)
                .width(118.dp)
                .height(230.dp)
                .zIndex(2f)
        )

        // Far right (#5)
        PhoneMockup(
            themeIdx = 4,
            isCenter = false,
            modifier = Modifier
                .offset(x = 118.dp)
                .width(108.dp)
                .height(210.dp)
                .zIndex(1f)
        )

        // Mid right (#3)
        PhoneMockup(
            themeIdx = 5,
            isCenter = false,
            modifier = Modifier
                .offset(x = 66.dp)
                .width(118.dp)
                .height(230.dp)
                .zIndex(2f)
        )

        // Center Phone (#1 - foreground, largest, with dynamic island & apply button)
        PhoneMockup(
            themeIdx = 2,
            isCenter = true,
            modifier = Modifier
                .width(140.dp)
                .height(272.dp)
                .zIndex(3f)
        )
    }
}

@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val pager = rememberPagerState { 4 }
    val scope = rememberCoroutineScope()
    val c = MaterialTheme.colorScheme
    Column(Modifier.fillMaxSize().background(c.background).systemBarsPadding()) {
        Row(Modifier.fillMaxWidth(), Arrangement.End) { TextButton(onFinish) { Text("Skip", color = c.onSurfaceVariant) } }
        HorizontalPager(pager, Modifier.weight(1f)) { page ->
            when (page) {
                0 -> ObPage("Secure with Voice Notes", "Create a unique voice note to protect your device with secure voice authentication.") {
                    BlueCard { WhiteMic(); Spacer(Modifier.height(24.dp)); Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        listOf(Icons.Filled.Dialpad, Icons.Filled.Apps, Icons.Filled.Fingerprint).forEach { Icon(it, null, tint = Color.White, modifier = Modifier.size(30.dp)) } } }
                }
                1 -> ObPage("Multiple Lock Options", "Add PIN, Pattern, or Fingerprint as backup methods for extra security whenever needed.") {
                    Column(Modifier.widthIn(max = 360.dp)) {
                        LockTile(Icons.Filled.Mic, "Voice Lock", "Unlock your device using your unique voice.")
                        LockTile(Icons.Filled.Fingerprint, "Fingerprint Lock", "Fast and accurate")
                        LockTile(Icons.Filled.Apps, "Pattern Lock", "Draw your unique pattern")
                        LockTile(Icons.Filled.Dialpad, "Pin Lock", "Secure with Pin Code")
                    }
                }
                2 -> ObPage("Secure with Voice Notes", "Create a unique voice note to protect your device with secure voice authentication.") {
                    BlueCard { WhiteMic(); Spacer(Modifier.height(20.dp)); Waveform(Modifier.padding(horizontal = 16.dp), Color.White, bars = 26)
                        Text("00:05", color = Color.White); Spacer(Modifier.height(6.dp))
                        Text("Say your voice password to unlock the screen", color = Color.White.copy(.8f), fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 16.dp)) }
                }
                else -> ObPage("Beautiful Lock Screen Themes", "Choose stylish themes and wallpapers to make your lock screen unique.") {
                    ThemesFannedShowcase()
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
            repeat(4) { Box(Modifier.padding(end = 6.dp).size(10.dp).clip(CircleShape).background(if (it == pager.currentPage) Blue else c.outline)) }
            Spacer(Modifier.weight(1f))
            Button({ if (pager.currentPage == 3) onFinish() else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } },
                colors = ButtonDefaults.buttonColors(containerColor = Blue)) { Text(if (pager.currentPage == 3) "Started" else "Next") }
        }
    }
}
