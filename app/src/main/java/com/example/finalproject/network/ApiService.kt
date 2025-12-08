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
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
)
data class WeatherResponse(
    val latitude: Double,
    val longitude: Double,
    val generationtime_ms: Double? = null,
    val utc_offset_seconds: Int? = null,
    val timezone: String? = null,
    val current_weather: CurrentWeather? = null,
    val hourly: Hourly? = null
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
    val pressure_msl: List<Double>? = null,
    val relativehumidity_2m: List<Double>? = null,
    val visibility: List<Double>? = null
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
        @Query("hourly") hourly: String = "pressure_msl,relativehumidity_2m,visibility"
    ): WeatherResponse
}
