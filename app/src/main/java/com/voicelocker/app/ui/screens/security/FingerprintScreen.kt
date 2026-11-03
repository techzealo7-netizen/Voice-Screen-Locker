package com.voicelocker.app.ui.screens.security

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.voicelocker.app.data.local.Store
import com.voicelocker.app.ui.components.*
import com.voicelocker.app.ui.navigation.go
import com.voicelocker.app.ui.theme.Success
import com.voicelocker.app.util.biometricAuth
import com.voicelocker.app.util.findActivity

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
            Icon(
                Icons.Filled.Fingerprint,
                null,
                Modifier.size(110.dp),
                tint = when (st) {
                    1 -> c.error
                    2 -> Success
                    else -> c.onSurfaceVariant
                }
            )
            Spacer(Modifier.height(12.dp))
            when (st) {
                0 -> {
                    Text("Tab to Enable", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    Muted("Use your fingerprint to unlock your device")
                }
                1 -> Text("Fingerprint Not Recognized.", color = c.error)
                else -> {
                    Text("Fingerprint scanned successfully", fontWeight = FontWeight.SemiBold)
                    Muted("Your fingerprint has been securely saved.")
                }
            }
        }
        if (st == 1) AppButton("Try Again", { scan() }, kind = Btn.Danger)
        if (st == 2) Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AppButton("Done", {}, Modifier.weight(1f), enabled = false)
            AppButton("Finish", {
                Store.fingerprintEnabled = true
                nav.go("fp", "setup")
            }, Modifier.weight(1f))
        }
    }
}

