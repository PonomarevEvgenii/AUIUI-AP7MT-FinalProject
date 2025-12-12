package com.example.finalproject.ui.navigation

import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.finalproject.ui.screens.SearchScreen
import com.example.finalproject.ui.screens.WeatherScreen
import com.example.finalproject.viewmodel.WeatherViewModel

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    val vm: WeatherViewModel = viewModel()
    NavHost(navController = navController, startDestination = "search") {
        composable("search") {
            SearchScreen(
                viewModel = vm,
                onSelect = { location ->
                    val lat = location.latitude
                    val lon = location.longitude
                    if (lat != null && lon != null) {
                        vm.loadWeather(lat, lon)
                    } else {
                    }
                    val cityNameEncoded = Uri.encode(location.name ?: "")
                    navController.navigate("weather/$cityNameEncoded")
                }
            )
        }
        composable(
            route = "weather/{city}",
            arguments = listOf(
                navArgument("city") { type = NavType.StringType; defaultValue = "" }
            )
        ) { backStackEntry ->
            val weatherState by vm.weather.collectAsState()
            val cityArg = backStackEntry.arguments?.getString("city") ?: ""
            val cityTitle: String? = cityArg.takeIf { it.isNotBlank() }?.let { Uri.decode(it) }
            val headerText = cityTitle ?: run {
                val lat = weatherState?.latitude
                val lon = weatherState?.longitude
                val tz = weatherState?.timezone
                when {
                    !tz.isNullOrBlank() -> tz
                    lat != null && lon != null -> String.format("%.2f, %.2f", lat, lon)
                    else -> "Unknown"
                }
            }
            WeatherScreen(
                cityName = headerText,
                weather = weatherState
            )
        }
    }
}
