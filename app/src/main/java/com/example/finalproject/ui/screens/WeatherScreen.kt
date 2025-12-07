package com.example.finalproject.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun WeatherScreen(weather: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp)
    ) {
        Text("Weather screen")
        Spacer(modifier = Modifier.height(16.dp))
        Text(weather)
    }
}
