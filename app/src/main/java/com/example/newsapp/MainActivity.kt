package com.example.newsapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.newsapp.Navigation.App
import com.example.newsapp.ui.theme.NewsAppTheme
import dagger.hilt.android.AndroidEntryPoint

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.newsapp.domain.util.SettingsManager
import com.example.newsapp.domain.util.ThemePreference

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    @javax.inject.Inject
    lateinit var settingsManager: SettingsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        installSplashScreen()
        setContent {
            val highContrastEnabled by settingsManager.highContrastEnabled.collectAsState(initial = false)
            val themePreference by settingsManager.themePreference.collectAsState(initial = ThemePreference.SYSTEM)
            NewsAppTheme(
                highContrast = highContrastEnabled,
                themePreference = themePreference
            ) {
                App()
            }
        }
    }
}
