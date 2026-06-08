package com.prowllabs.prowl.ui.internal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import com.prowllabs.prowl.ui.navigation.ProwlNavHost

internal class ProwlMainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ProwlTheme {
                ProwlNavHost(onClose = { finish() })
            }
        }
    }
}

@Composable
private fun ProwlTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    MaterialTheme(
        colorScheme = if (dark) darkColorScheme() else lightColorScheme(),
        content = content,
    )
}
