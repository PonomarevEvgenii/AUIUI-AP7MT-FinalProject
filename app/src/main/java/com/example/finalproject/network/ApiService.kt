package com.example.finalproject.network

import retrofit2.http.GET
import retrofit2.http.Query
data class GeocodingResponse(
    val results: List<Location>? = null
)
data class Location(
    val id: Long? = null,
    val name: String? = null,
    val country: String? = null,
    val latitude: Double? = 0.0,
    val longitude: Double? = 0.0
)
data class WeatherResponse(
    val latitude: Double,
    val longitude: Double,
    val generationtime_ms: Double? = null,
    val utc_offset_seconds: Int? = null,
    val timezone: String? = null,
    val current_weather: CurrentWeather? = null,
    val hourly: Hourly? = null,
    val daily: Daily? = null
)
data class CurrentWeather(
    val temperature: Double,
    val windspeed: Double,
    val winddirection: Double,
    val weathercode: Int,
    val time: String
)
data class Hourly(
    val time: List<String> = emptyList(),
    val temperature_2m: List<Double> = emptyList(),
    val relativehumidity_2m: List<Double>? = null,
    val pressure_msl: List<Double>? = null,
    val visibility: List<Double>? = null,
    val weathercode: List<Int>? = null,
    val precipitation: List<Double>? = null
)
data class Daily(
    val time: List<String> = emptyList(),
    val sunrise: List<String>? = null,
    val sunset: List<String>? = null,
    val precipitation_sum: List<Double>? = null,
    val temperature_2m_max: List<Double>? = null,
    val temperature_2m_min: List<Double>? = null
)
interface GeocodingApi {
    @GET("v1/search")
    suspend fun search(
        @Query("name") name: String,
        @Query("count") count: Int = 10
    ): GeocodingResponse
}

interface WeatherApi {
    @GET("v1/forecast")
    suspend fun currentWeather(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current_weather") currentWeather: Boolean = true,
        @Query("hourly") hourly: String = "temperature_2m,relativehumidity_2m,pressure_msl,visibility,weathercode,precipitation",
        @Query("daily") daily: String = "sunrise,sunset,precipitation_sum",
        @Query("timezone") timezone: String = "auto"
    ): WeatherResponse
}
