package com.example.finalproject.data

import com.example.finalproject.network.Location
import com.example.finalproject.network.RetrofitClient
import com.example.finalproject.network.WeatherResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WeatherRepository {
    private val geo = RetrofitClient.geocodingApi
    private val weather = RetrofitClient.weatherApi

    suspend fun searchLocations(query: String): Result<List<Location>> = withContext(Dispatchers.IO) {
        try {
            val res = geo.search(query)
            Result.success(res.results ?: emptyList())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCurrentWeather(lat: Double, lon: Double): Result<WeatherResponse> = withContext(Dispatchers.IO) {
        try {
            val res = weather.currentWeather(lat, lon)
            Result.success(res)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}


