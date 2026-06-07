package com.example.newsapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.newsapp.Navigation.App
import com.example.newsapp.ui.theme.NewsAppTheme
import dagger.hilt.android.AndroidEntryPoint

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.isSystemInDarkTheme
import android.os.Build
import com.example.newsapp.domain.util.SettingsManager
import com.example.newsapp.domain.util.ThemePreference
import com.example.newsapp.data.util.TelemetryManager

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    @javax.inject.Inject
    lateinit var settingsManager: SettingsManager

    @javax.inject.Inject
    lateinit var telemetryManager: TelemetryManager

    override fun onPause() {
        super.onPause()
        telemetryManager.flush()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Initial default to edge-to-edge
        enableEdgeToEdge()
        val splashScreen = installSplashScreen()
        
        var keepSplashScreen = true
        splashScreen.setKeepOnScreenCondition { keepSplashScreen }

        setContent {
            val highContrastEnabled by settingsManager.highContrastEnabled.collectAsState(initial = null)
            val themePreference by settingsManager.themePreference.collectAsState(initial = null)
            
            LaunchedEffect(highContrastEnabled, themePreference) {
                if (highContrastEnabled != null && themePreference != null) {
                    keepSplashScreen = false
                }
            }

            if (highContrastEnabled != null && themePreference != null) {
                val isDarkTheme = when (themePreference!!) {
                    ThemePreference.SYSTEM -> isSystemInDarkTheme()
                    ThemePreference.LIGHT -> false
                    ThemePreference.DARK -> true
                }

            DisposableEffect(isDarkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = if (isDarkTheme) {
                        SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(
                            android.graphics.Color.TRANSPARENT,
                            android.graphics.Color.TRANSPARENT
                        )
                    },
                    navigationBarStyle = if (isDarkTheme) {
                        SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(
                            android.graphics.Color.TRANSPARENT,
                            android.graphics.Color.TRANSPARENT
                        )
                    }
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    window.isStatusBarContrastEnforced = false
                    window.isNavigationBarContrastEnforced = false
                }
                onDispose {}
            }

            NewsAppTheme(
                highContrast = highContrastEnabled!!,
                themePreference = themePreference!!
            ) {
                App()
            }
            }
        }
    }
}
