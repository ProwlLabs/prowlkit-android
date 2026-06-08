package com.prowllabs.prowl.ui.internal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.prowllabs.prowl.ui.navigation.ProwlNavHost
import com.prowllabs.prowl.ui.theme.ProwlTheme
import com.prowllabs.prowl.ui.util.ProwlUiPreferences

internal class ProwlMainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        var themeMode by mutableStateOf(ProwlUiPreferences.themeMode(this))
        setContent {
            ProwlTheme(themeMode = themeMode) {
                ProwlNavHost(
                    onClose = { finish() },
                    onThemeChanged = { themeMode = it },
                )
            }
        }
    }
}
