@file:Suppress("DEPRECATION")

package com.example.finalproject.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.location.Geocoder
import android.location.LocationManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.finalproject.network.Location
import com.example.finalproject.viewmodel.WeatherViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

private const val PREFS_NAME = "favorites_prefs"
private const val PREFS_KEY_FAVS = "favorite_places_v1"
private fun decodeFavEntry(entry: String): Location? {
    val parts = entry.split("|")
    if (parts.size < 4) return null
    val name = parts[0].ifBlank { null }
    val country = parts[1].ifBlank { null }
    val lat = parts[2].toDoubleOrNull()
    val lon = parts[3].toDoubleOrNull()
    if (name == null && country == null && lat == null && lon == null) return null
    return Location(name = name, country = country, latitude = lat, longitude = lon)
}
private fun getFavoritesFromPrefs(context: Context): List<Location> {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val raw = prefs.getString(PREFS_KEY_FAVS, "") ?: ""
    if (raw.isBlank()) return emptyList()
    return raw.lines().map { it.trim() }.filter { it.isNotEmpty() }.mapNotNull { decodeFavEntry(it) }
}
private fun weatherEmoji(code: Int?): String {
    return when (code) {
        null -> "❔"
        0 -> "☀️"
        1, 2 -> "🌤️"
        3 -> "\uD83C\uDF25\uFE0F"
        45, 48 -> "🌫️"
        51, 53, 55 -> "🌦️"
        61, 63, 65 -> "🌧️"
        71, 73, 75, 77 -> "❄️"
        80, 81, 82 -> "🌧️"
        95, 96, 99 -> "⛈️"
        else -> "☁️"
    }
}
@SuppressLint("QueryPermissionsNeeded")
@Composable
fun SearchScreen(
    viewModel: WeatherViewModel,
    onSelect: (Location) -> Unit,
    modifier: Modifier = Modifier,
    previewCurrentTemp: String? = null
) {
    val scope = rememberCoroutineScope()
    val locationsState by viewModel.locations.collectAsState()
    val searchTemps: Map<String, String> by viewModel.searchTemps.collectAsState(initial = emptyMap())
    val searchWeatherCodes: Map<String, Int?> by viewModel.searchWeatherCodes.collectAsState(initial = emptyMap())
    val loading by viewModel.loading.collectAsState()
    val error by viewModel.error.collectAsState()
    val weatherState by viewModel.weather.collectAsState()
    val searchResults = locationsState
    val context = LocalContext.current
    val savedPlaces = remember { mutableStateListOf<Location>() }
    var currentPlaceName by remember { mutableStateOf<String?>(null) }
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            fetchAndApplyDeviceLocation(context, scope, viewModel) { name ->
                currentPlaceName = name
            }
        }
    }
    LaunchedEffect(Unit) {
        val pm = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        if (pm == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            fetchAndApplyDeviceLocation(context, scope, viewModel) { name ->
                currentPlaceName = name
            }
        } else {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }
    LaunchedEffect(Unit) {
        val favs = getFavoritesFromPrefs(context)
        savedPlaces.clear()
        savedPlaces.addAll(favs)
        favs.forEach { loc ->
            loc.latitude?.let { lat ->
                loc.longitude?.let { lon ->
                    viewModel.fetchWeatherForLocation(lat, lon)
                }
            }
        }
    }
    DisposableEffect(context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val listener =
            SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                if (key == PREFS_KEY_FAVS) {
                    val favs = getFavoritesFromPrefs(context)
                    savedPlaces.clear()
                    savedPlaces.addAll(favs)
                    favs.forEach { loc ->
                        loc.latitude?.let { lat ->
                            loc.longitude?.let { lon ->
                                viewModel.fetchWeatherForLocation(lat, lon)
                            }
                        }
                    }
                }
            }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose {
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }
    val currentTempString: String = previewCurrentTemp
        ?: weatherState?.current_weather?.temperature?.let { "${it.toInt()}°" } ?: "--°"
    var query by remember { mutableStateOf("") }
    var searchSubmitted by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 18.dp)
    ) {
        Text(
            text = "Weather",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = query,
                onValueChange = {
                    query = it
                    if (it.isEmpty()) searchSubmitted = false
                },
                placeholder = { Text("City") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "search") },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = {
                            query = ""
                            searchSubmitted = false
                            focusManager.clearFocus()
                        }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Clear search")
                        }
                    } else {
                        Spacer(modifier = Modifier.size(0.dp))
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .focusRequester(focusRequester),
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        val trimmed = query.trim()
                        if (trimmed.isNotEmpty() && !loading) {
                            searchSubmitted = true
                            scope.launch { viewModel.search(trimmed) }
                            focusManager.clearFocus()
                        }
                    }
                )
            )
            Spacer(modifier = Modifier.width(12.dp))
            Button(
                onClick = {
                    val trimmed = query.trim()
                    if (trimmed.isNotEmpty()) {
                        searchSubmitted = true
                        scope.launch { viewModel.search(trimmed) }
                        focusManager.clearFocus()
                    }
                },
                modifier = Modifier.height(56.dp),
                enabled = query.trim().isNotEmpty() && !loading
            ) {
                Text("Search")
            }
        }
        Spacer(modifier = Modifier.height(18.dp))
        val isSearching = query.trim().isNotEmpty()
        if (isSearching) {
            Text(text = "Search results", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
            LaunchedEffect(searchResults, searchSubmitted) {
                if (searchSubmitted && searchResults.isNotEmpty()) {
                    viewModel.clearSearchTemps()
                    searchResults.forEach { loc ->
                        loc.latitude?.let { lat ->
                            loc.longitude?.let { lon ->
                                viewModel.fetchWeatherForLocation(lat, lon)
                            }
                        }
                    }
                }
            }
            if (loading) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                if (!searchSubmitted) {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { }
                } else {
                    if (searchResults.isEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { Text("No results") }
                    } else {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            searchResults.forEach { loc ->
                                val key = if (loc.latitude != null && loc.longitude != null) {
                                    String.format(Locale.US, "%.5f_%.5f", loc.latitude, loc.longitude)
                                } else ""
                                val tempForItem = if (key.isNotEmpty()) searchTemps[key] ?: "--°" else "--°"
                                val codeForItem = if (key.isNotEmpty()) searchWeatherCodes[key] else null
                                LocationRowWithTemp(
                                    location = loc,
                                    tempString = tempForItem,
                                    weatherCode = codeForItem,
                                    onClick = {
                                        query = ""
                                        searchSubmitted = false
                                        onSelect(loc)
                                    }
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                }
            }
            error?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = it, color = MaterialTheme.colorScheme.error)
            }
            return@Column
        }
        CurrentLocationCard(
            tempString = currentTempString,
            currentName = currentPlaceName ?: (weatherState?.timezone?.let { formatCityNameFromTimezone(it) } ?: "Current location"),
            onClick = {
                onSelect(
                    Location(
                        name = "Current location",
                        country = null,
                        latitude = null,
                        longitude = null
                    )
                )
            },
            onLocationIconClick = {
                try {
                    val pkg = context.packageName
                    val managePermsIntent = Intent().apply {
                        action = "android.settings.MANAGE_APP_PERMISSIONS"
                        putExtra("android.provider.extra.APP_PACKAGE", pkg)
                    }
                    if (managePermsIntent.resolveActivity(context.packageManager) != null) {
                        context.startActivity(managePermsIntent)
                        return@CurrentLocationCard
                    }
                    val appDetails = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", pkg, null)
                    }
                    if (appDetails.resolveActivity(context.packageManager) != null) {
                        context.startActivity(appDetails)
                        return@CurrentLocationCard
                    }
                    val locIntent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                    if (locIntent.resolveActivity(context.packageManager) != null) {
                        context.startActivity(locIntent)
                    }
                } catch (_: Exception) {
                }
            }
        )
        Spacer(modifier = Modifier.height(20.dp))
        Text(text = "Saved places", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
        if (loading) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            if (savedPlaces.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("No places found")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(items = savedPlaces, key = { loc ->
                        if (loc.latitude != null && loc.longitude != null) {
                            String.format(Locale.US, "%.6f_%.6f", loc.latitude, loc.longitude)
                        } else {
                            "${loc.name ?: "unknown"}|${loc.country ?: ""}"
                        }
                    }) { loc ->
                        val key = if (loc.latitude != null && loc.longitude != null) {
                            String.format(Locale.US, "%.5f_%.5f", loc.latitude, loc.longitude)
                        } else ""
                        val tempForSaved = if (key.isNotEmpty()) searchTemps[key] ?: "--°" else "--°"
                        val codeForSaved = if (key.isNotEmpty()) searchWeatherCodes[key] else null

                        SavedPlaceRow(
                            location = loc,
                            tempString = tempForSaved,
                            weatherCode = codeForSaved,
                            onClick = { onSelect(loc) }
                        )
                    }
                }
            }
        }
        error?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = it, color = MaterialTheme.colorScheme.error)
        }
    }
}
private fun formatCityNameFromTimezone(timezone: String?): String {
    if (timezone.isNullOrBlank()) return "Current location"
    val raw = timezone.substringAfterLast('/')
    return raw.replace('_', ' ')
}
@Composable
private fun CurrentLocationCard(
    tempString: String,
    currentName: String,
    onClick: () -> Unit,
    onLocationIconClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(84.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { onLocationIconClick() }, modifier = Modifier.size(40.dp)) {
                Icon(imageVector = Icons.Default.Place, contentDescription = "place", modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                val cityName = currentName
                Text(text = cityName, style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.height(6.dp))
                Text(text = "Tap to view details", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Box(modifier = Modifier
                .clickable { onClick() }
                .padding(start = 8.dp)
            ) {
                Text(text = tempString, style = MaterialTheme.typography.displaySmall)
            }
        }
    }
}
@Composable
private fun SavedPlaceRow(
    location: Location,
    tempString: String,
    weatherCode: Int?,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onClick() }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = location.name ?: "Unknown", style = MaterialTheme.typography.bodyLarge)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = weatherEmoji(weatherCode), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(end = 8.dp))
            Text(text = tempString, style = MaterialTheme.typography.headlineSmall)
        }
    }
}
@Composable
private fun LocationRowWithTemp(location: Location, tempString: String, weatherCode: Int?, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onClick() }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = location.name ?: "Unknown", style = MaterialTheme.typography.bodyLarge)
            Text(text = location.country ?: "", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = weatherEmoji(weatherCode), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(end = 8.dp))
            Text(text = tempString, style = MaterialTheme.typography.headlineSmall)
        }
    }
}
private fun fetchAndApplyDeviceLocation(
    context: Context,
    scope: kotlinx.coroutines.CoroutineScope,
    viewModel: WeatherViewModel,
    onNameResolved: (String) -> Unit
) {
    try {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return
        val providers = listOf(LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER)
        var best: android.location.Location? = null
        for (p in providers) {
            try {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
                ) {
                    val l = lm.getLastKnownLocation(p)
                    if (l != null) {
                        if (best == null || l.time > best.time) best = l
                    }
                }
            } catch (_: Exception) {
            }
        }
        if (best != null) {
            val lat = best.latitude
            val lon = best.longitude
            scope.launch {
                val name = withContext(Dispatchers.IO) {
                    try {
                        val geocoder = Geocoder(context, Locale.getDefault())
                        val list = geocoder.getFromLocation(lat, lon, 1)
                        if (!list.isNullOrEmpty()) {
                            val placemark = list[0]
                            placemark.locality ?: placemark.subAdminArea ?: placemark.adminArea ?: "Current location"
                        } else {
                            "Current location"
                        }
                    } catch (_: Exception) {
                        "Current location"
                    }
                }
                onNameResolved(name)
                viewModel.loadWeather(lat, lon)
            }
        }
    } catch (_: Exception) {
    }
}
