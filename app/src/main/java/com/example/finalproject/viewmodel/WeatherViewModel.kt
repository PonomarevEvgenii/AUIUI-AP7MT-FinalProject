package com.example.finalproject.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class WeatherViewModel : ViewModel() {

    private val _weather = MutableStateFlow("Weather")
    val weather: StateFlow<String> = _weather

    fun loadWeather(lat: Double, lon: Double) {
        _weather.value = "Pls wait ($lat, $lon)"
    }
}
