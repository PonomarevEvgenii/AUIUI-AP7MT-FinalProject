package com.example.finalproject.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.finalproject.viewmodel.WeatherViewModel

data class Location(val latitude: Double, val longitude: Double)

@Composable
fun SearchScreen(viewModel: WeatherViewModel, onSelect: (Location) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp)
    ) {
        Text("SearcScreen (Not for long)")
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = {
            onSelect(Location(55.75, 37.61)) // Moscow for first example
        }) {
            Text("Choose city")
        }
    }
}
