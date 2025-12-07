package com.example.finalproject.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.finalproject.network.WeatherResponse

@Composable
fun WeatherScreen(weather: WeatherResponse?) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        if (weather == null) {
            Text("No data")
            return
        }

        weather.current_weather?.let { cw ->
            Text("Temperature: ${cw.temperature} °C")
            Spacer(modifier = Modifier.height(8.dp))
            Text("Wind: ${cw.windspeed} m/s")
            Spacer(modifier = Modifier.height(8.dp))
            Text("Wind dir: ${cw.winddirection}°")
            Spacer(modifier = Modifier.height(8.dp))
            Text("Weather code: ${cw.weathercode}")
            Spacer(modifier = Modifier.height(8.dp))
            Text("Time: ${cw.time}")
        } ?: Text("No current weather available")
    }
}

