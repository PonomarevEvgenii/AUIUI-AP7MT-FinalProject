package com.example.finalproject

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.finalproject.ui.navigation.AppNavHost
import com.example.finalproject.ui.theme.FinalProjectTheme
class MainActivity : ComponentActivity() {
    private val TAG = "MainActivityStatusBar"
    private var keepSplashOnScreen = true
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { keepSplashOnScreen }
        super.onCreate(savedInstanceState)
        Handler(Looper.getMainLooper()).postDelayed({
            keepSplashOnScreen = false
        }, 1000)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            val isDarkSystem = isSystemInDarkTheme()
            FinalProjectTheme(darkTheme = isDarkSystem) {
                val bgColor = MaterialTheme.colorScheme.background
                SideEffect {
                    applyStatusBar(isDarkSystem, bgColor.toArgb())
                }
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(WindowInsets.statusBars.asPaddingValues()),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavHost()
                }
            }
        }
    }
    override fun onResume() {
        super.onResume()
        try {
            val isDark = resources.configuration.uiMode and
                    android.content.res.Configuration.UI_MODE_NIGHT_MASK ==
                    android.content.res.Configuration.UI_MODE_NIGHT_YES
            applyStatusBar(isDark, MaterialThemeStubBackgroundColor())
            Log.i(
                TAG,
                "onResume applied status bar. isDark=$isDark, statusBarColor=0x${window.statusBarColor.toUInt().toString(16)}"
            )
        } catch (e: Exception) {
            Log.w(TAG, "onResume applyStatusBar error: ${e.message}")
        }
    }
    private fun applyStatusBar(isDarkTheme: Boolean, colorArgb: Int) {
        try {
            window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            window.addFlags(android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.statusBarColor = colorArgb
            WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = !isDarkTheme
            Log.i(TAG, "applyStatusBar: isDarkTheme=$isDarkTheme, statusBarColor=0x${colorArgb.toUInt().toString(16)}")
        } catch (e: Exception) {
            Log.w(TAG, "applyStatusBar error: ${e.message}")
        }
    }
    private fun MaterialThemeStubBackgroundColor(): Int {
        return Color.TRANSPARENT
    }
}
