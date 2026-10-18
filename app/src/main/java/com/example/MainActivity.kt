package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.presentation.PermoraApp
import com.example.ui.theme.PermoraTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as PermoraApplication
        val settingsRepo = app.container.settingsRepository

        setContent {
            val themeMode by settingsRepo.themeMode.collectAsStateWithLifecycle(initialValue = "SYSTEM")
            PermoraTheme(themePreference = themeMode) {
                PermoraApp()
            }
        }
    }
}
