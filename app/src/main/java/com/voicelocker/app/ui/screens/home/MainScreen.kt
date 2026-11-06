package com.voicelocker.app.ui.screens.home

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.voicelocker.app.R
import com.voicelocker.app.ui.components.ExitDialog
import com.voicelocker.app.ui.screens.notes.NotesTab
import com.voicelocker.app.ui.theme.InterFontFamily

@Composable
fun MainScreen(nav: NavHostController) {
    val ctx = LocalContext.current
    val activity = ctx as? Activity
    val c = MaterialTheme.colorScheme
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var showExitDialog by rememberSaveable { mutableStateOf(false) }

    BackHandler {
        if (tab != 0) {
            tab = 0
        } else {
            showExitDialog = true
        }
    }

    val navItems = listOf(
        R.drawable.ic_nav_lock to "Ai Voice Lock",
        R.drawable.ic_ai_notes to "Ai voice Notes",
        R.drawable.ic_settings to "Setting"
    )

    Scaffold(
        containerColor = c.background,
        bottomBar = {
            NavigationBar(containerColor = c.surface) {
                navItems.forEachIndexed { i, (iconRes, label) ->
                    val isSelected = tab == i
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { tab = i },
                        icon = {
                            Icon(
                                painter = painterResource(iconRes),
                                contentDescription = label,
                                modifier = Modifier.size(24.dp),
                                tint = if (isSelected) c.primary else c.onSurfaceVariant
                            )
                        },
                        label = {
                            Text(
                                text = label,
                                fontFamily = InterFontFamily,
                                fontSize = 11.sp,
                                maxLines = 1
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = c.primary,
                            selectedTextColor = c.primary,
                            indicatorColor = Color.Transparent,
                            unselectedIconColor = c.onSurfaceVariant,
                            unselectedTextColor = c.onSurfaceVariant
                        )
                    )
                }
            }
        }
    ) { pad ->
        Box(Modifier.padding(pad)) {
            when (tab) {
                0 -> HomeTab(nav)
                1 -> NotesTab(nav)
                else -> SettingsTab(nav, onBack = { tab = 0 })
            }
        }
    }

    if (showExitDialog) {
        ExitDialog(
            onDismiss = { showExitDialog = false },
            onExit = {
                showExitDialog = false
                activity?.finish()
            }
        )
    }
}
