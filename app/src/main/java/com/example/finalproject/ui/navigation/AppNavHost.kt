package com.example.finalproject.ui.navigation

import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.finalproject.ui.screens.SearchScreen
import com.example.finalproject.ui.screens.WelcomeScreen
import com.example.finalproject.ui.screens.WeatherScreen
import com.example.finalproject.viewmodel.WeatherViewModel

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    val vm: WeatherViewModel = viewModel()

    NavHost(navController = navController, startDestination = "welcome") {

        composable("welcome") {
            WelcomeScreen(
                onGetStarted = {
                    navController.navigate("search") {
                        popUpTo("welcome") { inclusive = true }
                    }
                }
            )
        }

        composable("search") {
            SearchScreen(
                viewModel = vm,
                onSelect = { location ->
                    vm.loadWeather(location.latitude, location.longitude)
                    navController.navigate("weather")
                }
            )
        }

        composable("weather") {
            val weatherState by vm.weather.collectAsState()
            WeatherScreen(weatherState)
        }
    }
}

