package com.example.finalproject.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
@Composable
fun FinalProjectTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val LightColors = lightColorScheme(
        primary = Color(0xFF6D5DFB),
        onPrimary = Color.White,
        secondary = Color(0xFF958CF6),
        onSecondary = Color.White,
        background = Color(0xFFF5F3FF),
        onBackground = Color(0xFF1C1B1F),
        surface = Color.White,
        onSurface = Color(0xFF1C1B1F),
    )
    val DarkColors = darkColorScheme(
        primary = Color(0xFF9AA1FF),
        onPrimary = Color.Black,
        secondary = Color(0xFFC1BFFF),
        onSecondary = Color.Black,
        background = Color(0xFF141427),
        onBackground = Color(0xFFEAE6FF),
        surface = Color(0xFF1D1B33),
        onSurface = Color(0xFFEAE6FF),
    )
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography,
        content = content
    )
}
