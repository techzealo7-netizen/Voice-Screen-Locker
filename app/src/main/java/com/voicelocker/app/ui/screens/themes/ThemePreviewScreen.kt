package com.voicelocker.app.ui.screens.themes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.voicelocker.app.data.local.Store
import com.voicelocker.app.data.model.lockThemes
import com.voicelocker.app.ui.components.AppButton
import com.voicelocker.app.ui.components.Btn
import com.voicelocker.app.ui.components.Page
import com.voicelocker.app.ui.components.ThemeBg
import com.voicelocker.app.util.toast

@Composable
fun ThemePreviewScreen(nav: NavHostController, i: Int) {
    val ctx = LocalContext.current
    Page(
        title = "Themes",
        onBack = { nav.popBackStack() },
        scroll = false,
        bottom = {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AppButton("Preview", { nav.navigate("lockPreview/$i") }, Modifier.weight(1f), kind = Btn.Outline, icon = Icons.Filled.Visibility)
                AppButton("Apply", {
                    Store.themeIndex = i
                    ctx.toast("${lockThemes[i].name} applied")
                }, Modifier.weight(1f))
            }
        }
    ) {
        ThemeBg(i, Modifier.weight(1f).fillMaxWidth().padding(bottom = 4.dp), RoundedCornerShape(16.dp))
    }
}

