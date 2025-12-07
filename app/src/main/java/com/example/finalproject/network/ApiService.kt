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
    val generationtime_ms: Double?,
    val utc_offset_seconds: Int?,
    val timezone: String?,
    val current_weather: CurrentWeather?
)
data class CurrentWeather(
    val temperature: Double,
    val windspeed: Double,
    val winddirection: Double,
    val weathercode: Int,
    val time: String
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
        @Query("current_weather") currentWeather: Boolean = true
    ): WeatherResponse
}
