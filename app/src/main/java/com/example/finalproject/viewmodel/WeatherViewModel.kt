package com.example.finalproject.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.finalproject.data.WeatherRepository
import com.example.finalproject.network.Location
import com.example.finalproject.network.WeatherResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class WeatherViewModel : ViewModel() {
    private val repo = WeatherRepository()

    private val _locations = MutableStateFlow<List<Location>>(emptyList())
    val locations: StateFlow<List<Location>> = _locations

    private val _weather = MutableStateFlow<WeatherResponse?>(null)
    val weather: StateFlow<WeatherResponse?> = _weather

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun search(query: String) {
        if (query.isBlank()) return
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            val res = repo.searchLocations(query)
            if (res.isSuccess) {
                _locations.value = res.getOrDefault(emptyList())
            } else {
                _error.value = res.exceptionOrNull()?.localizedMessage ?: "Unknown error"
            }
            _loading.value = false
        }
    }

    fun loadWeather(lat: Double, lon: Double) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            val res = repo.getCurrentWeather(lat, lon)
            if (res.isSuccess) {
                _weather.value = res.getOrNull()
            } else {
                _error.value = res.exceptionOrNull()?.localizedMessage ?: "Unknown error"
            }
            _loading.value = false
        }
    }
}

