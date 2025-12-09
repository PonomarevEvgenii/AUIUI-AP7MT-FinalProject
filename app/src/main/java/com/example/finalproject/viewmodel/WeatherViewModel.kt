package com.example.finalproject.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.finalproject.data.WeatherRepository
import com.example.finalproject.network.Location
import com.example.finalproject.network.WeatherResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.Normalizer
import java.util.Locale
import kotlin.math.pow
import kotlin.math.round

class WeatherViewModel : ViewModel() {
    private val repo = WeatherRepository()
    private val _locations = MutableStateFlow<List<Location>>(emptyList())
    val locations: StateFlow<List<Location>> = _locations.asStateFlow()
    private val _weather = MutableStateFlow<WeatherResponse?>(null)
    val weather: StateFlow<WeatherResponse?> = _weather.asStateFlow()
    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    private val _searchTemps = MutableStateFlow<Map<String, String>>(emptyMap())
    val searchTemps: StateFlow<Map<String, String>> = _searchTemps.asStateFlow()
    private val _searchWeatherCodes = MutableStateFlow<Map<String, Int?>>(emptyMap())
    val searchWeatherCodes: StateFlow<Map<String, Int?>> = _searchWeatherCodes.asStateFlow()
    private fun keyFor(lat: Double?, lon: Double?): String {
        if (lat == null || lon == null) return ""
        return String.format(Locale.US, "%.5f_%.5f", lat, lon)
    }
    private fun normalizeName(name: String?): String {
        if (name.isNullOrBlank()) return ""
        val collapsed = name.trim().replace(Regex("\\s+"), " ")
        val normalized = Normalizer.normalize(collapsed, Normalizer.Form.NFD)
            .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
        return normalized.lowercase(Locale.US)
    }
    private fun roundCoord(coord: Double?, decimals: Int = 4): String {
        if (coord == null || coord.isNaN()) return "na"
        val factor = 10.0.pow(decimals)
        val rounded = round(coord * factor) / factor
        return String.format(Locale.US, "%.${decimals}f", rounded)
    }
    fun search(query: String) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                val res = repo.searchLocations(trimmed)
                if (res.isSuccess) {
                    val rawList = res.getOrDefault(emptyList())
                    val seen = linkedSetOf<String>()
                    val deduped = mutableListOf<Location>()
                    for (loc in rawList) {
                        val n = normalizeName(loc.name)
                        val latKey = roundCoord(loc.latitude)
                        val lonKey = roundCoord(loc.longitude)
                        val key = "${n}__${latKey}__${lonKey}"
                        if (key.isNotBlank() && !seen.contains(key)) {
                            seen.add(key)
                            deduped.add(loc)
                        } else if (key.isBlank()) {
                            val alt = normalizeName(loc.name)
                            if (alt.isNotBlank() && !seen.contains(alt)) {
                                seen.add(alt)
                                deduped.add(loc)
                            }
                        }
                    }
                    _locations.value = deduped
                } else {
                    _error.value = res.exceptionOrNull()?.localizedMessage ?: "Unknown error"
                }
            } catch (e: Exception) {
                _error.value = e.localizedMessage ?: "Unknown error"
            } finally {
                _loading.value = false
            }
        }
    }
    fun loadWeather(lat: Double, lon: Double) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                val res = repo.getCurrentWeather(lat, lon)
                if (res.isSuccess) {
                    _weather.value = res.getOrNull()
                } else {
                    _error.value = res.exceptionOrNull()?.localizedMessage ?: "Unknown error"
                }
            } catch (e: Exception) {
                _error.value = e.localizedMessage ?: "Unknown error"
            } finally {
                _loading.value = false
            }
        }
    }
    fun fetchWeatherForLocation(lat: Double?, lon: Double?) {
        if (lat == null || lon == null) return
        val key = keyFor(lat, lon)
        if (key.isBlank()) return
        val hasTemp = _searchTemps.value.containsKey(key)
        val hasCode = _searchWeatherCodes.value.containsKey(key)
        if (hasTemp && hasCode) return
        viewModelScope.launch {
            try {
                val res = repo.getCurrentWeather(lat, lon)
                val tempString = if (res.isSuccess) {
                    res.getOrNull()?.current_weather?.temperature?.let { "${it.toInt()}°" } ?: "--°"
                } else {
                    "--°"
                }
                val code: Int? = if (res.isSuccess) {
                    res.getOrNull()?.current_weather?.weathercode
                } else {
                    null
                }
                val newMap = _searchTemps.value.toMutableMap()
                newMap[key] = tempString
                _searchTemps.value = newMap
                val newCodes = _searchWeatherCodes.value.toMutableMap()
                newCodes[key] = code
                _searchWeatherCodes.value = newCodes
            } catch (_: Exception) {
                val newMap = _searchTemps.value.toMutableMap()
                newMap[key] = "--°"
                _searchTemps.value = newMap
                val newCodes = _searchWeatherCodes.value.toMutableMap()
                newCodes[key] = null
                _searchWeatherCodes.value = newCodes
            }
        }
    }
    fun clearSearchTemps() {
        _searchTemps.value = emptyMap()
        _searchWeatherCodes.value = emptyMap()
    }
}
