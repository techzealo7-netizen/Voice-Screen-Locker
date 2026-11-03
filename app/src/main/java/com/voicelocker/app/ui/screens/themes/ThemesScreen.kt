package com.voicelocker.app.ui.screens.themes

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.voicelocker.app.data.model.lockThemes
import com.voicelocker.app.ui.components.Page
import com.voicelocker.app.ui.components.ThemeGrid

@Composable
fun ThemesScreen(nav: NavHostController) = Page("Themes", onBack = { nav.popBackStack() }) {
    Spacer(Modifier.height(8.dp))
    ThemeGrid(lockThemes.indices.toList()) { nav.navigate("themePreview/$it") }
}

