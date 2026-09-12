package com.safespend.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.safespend.app.data.prefs.Settings
import com.safespend.app.data.prefs.ThemeMode
import com.safespend.app.ui.SafeSpendRoot
import com.safespend.app.ui.theme.SafeSpendTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val preferences = (application as SafeSpendApp).container.preferences

        setContent {
            val settings by preferences.settings.collectAsState(initial = Settings())
            val darkTheme = when (settings.themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            SafeSpendTheme(darkTheme = darkTheme) {
                SafeSpendRoot()
            }
        }
    }
}
