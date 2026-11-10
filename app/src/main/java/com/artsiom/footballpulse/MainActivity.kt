package com.artsiom.footballpulse

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.artsiom.footballpulse.ui.shell.AppShell
import com.artsiom.footballpulse.ui.theme.FootballPulseTheme

class MainActivity : ComponentActivity() {

    companion object {
        private const val PREFS_NAME = "settings"
        private const val KEY_DARK_MODE = "dark_mode"
        private const val LIGHT_SCRIM = 0xE6FFFFFF.toInt()
        private const val DARK_SCRIM = 0x801B1B1B.toInt()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val repository = (application as FootballPulseApp).container.repository
        setContent {
            // The in-app toggle overrides the system setting, so it is kept as state here
            // (survives rotation via rememberSaveable) and persisted for the next launch.
            var darkTheme by rememberSaveable { mutableStateOf(prefs.getBoolean(KEY_DARK_MODE, false)) }
            DisposableEffect(darkTheme) {
                // System bar icon colors must follow the in-app theme, not the system one.
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkTheme },
                    navigationBarStyle = SystemBarStyle.auto(LIGHT_SCRIM, DARK_SCRIM) { darkTheme }
                )
                onDispose { }
            }
            FootballPulseTheme(darkTheme = darkTheme) {
                AppShell(
                    repository = repository,
                    darkTheme = darkTheme,
                    onToggleTheme = {
                        darkTheme = !darkTheme
                        prefs.edit().putBoolean(KEY_DARK_MODE, darkTheme).apply()
                    }
                )
            }
        }
    }
}
