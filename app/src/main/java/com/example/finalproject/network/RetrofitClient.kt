package com.example.finalproject.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(logging)
        .build()

    private fun retrofit(baseUrl: String): Retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val geocodingApi: GeocodingApi by lazy {
        retrofit("https://geocoding-api.open-meteo.com/").create(GeocodingApi::class.java)
    }

    val weatherApi: WeatherApi by lazy {
        retrofit("https://api.open-meteo.com/").create(WeatherApi::class.java)
    }
}
