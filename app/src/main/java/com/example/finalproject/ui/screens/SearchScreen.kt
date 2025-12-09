package com.example.finalproject.ui.screens

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.provider.Settings
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
import com.example.finalproject.network.Location
import com.example.finalproject.viewmodel.WeatherViewModel
import kotlinx.coroutines.launch
import kotlin.math.abs
import java.util.Locale

private const val PREFS_NAME = "favorites_prefs"
private const val PREFS_KEY_FAVS = "favorite_places_v1"

private fun encodeFavEntry(name: String?, country: String?, lat: Double?, lon: Double?): String {
    val n = (name ?: "").replace("\n", " ").replace("|", " ")
    val c = (country ?: "").replace("\n", " ").replace("|", " ")
    val latS = lat?.toString() ?: ""
    val lonS = lon?.toString() ?: ""
    return listOf(n, c, latS, lonS).joinToString("|")
}

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
    val loading by viewModel.loading.collectAsState()
    val error by viewModel.error.collectAsState()
    val weatherState by viewModel.weather.collectAsState()
    val searchResults = locationsState
    val context = LocalContext.current
    val savedPlaces = remember { mutableStateListOf<Location>() }
    LaunchedEffect(Unit) {
        val favs = getFavoritesFromPrefs(context)
        savedPlaces.clear()
        savedPlaces.addAll(favs)
    }
    DisposableEffect(context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val listener =
            SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                if (key == PREFS_KEY_FAVS) {
                    val favs = getFavoritesFromPrefs(context)
                    savedPlaces.clear()
                    savedPlaces.addAll(favs)
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
                                val tempForItem = tempFromSearchTemps(searchTemps = searchTemps, lat = loc.latitude, lon = loc.longitude)
                                LocationRowWithTemp(location = loc, tempString = tempForItem, weatherCode = null, onClick = {
                                    query = ""
                                    searchSubmitted = false
                                    onSelect(loc)
                                })
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
            weatherState = weatherState,
            onClick = {
                weatherState?.let {
                    onSelect(
                        Location(
                            name = formatCityNameFromTimezone(it.timezone),
                            country = null,
                            latitude = it.latitude,
                            longitude = it.longitude
                        )
                    )
                }
            },
            onLocationIconClick = {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    val uri: Uri = Uri.fromParts("package", context.packageName, null)
                    data = uri
                }
                context.startActivity(intent)
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
                        SavedPlaceRow(
                            location = loc,
                            tempString = tempStringForLocation(weatherState, loc),
                            weatherCode = run {
                                val eps = 0.001
                                val w = weatherState
                                if (w == null) null
                                else {
                                    if (loc.latitude != null && loc.longitude != null &&
                                        abs(w.latitude - loc.latitude) < eps &&
                                        abs(w.longitude - loc.longitude) < eps
                                    ) {
                                        w.current_weather?.weathercode
                                    } else null
                                }
                            },
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

private fun tempFromSearchTemps(searchTemps: Map<String, String>, lat: Double?, lon: Double?): String {
    if (lat == null || lon == null) return "--°"
    val key = String.format(Locale.US, "%.5f_%.5f", lat, lon)
    return searchTemps[key] ?: "--°"
}
private fun formatCityNameFromTimezone(timezone: String?): String {
    if (timezone.isNullOrBlank()) return "Current location"
    val raw = timezone.substringAfterLast('/')
    return raw.replace('_', ' ')
}
private fun tempStringForLocation(weatherState: com.example.finalproject.network.WeatherResponse?, loc: Location): String {
    val eps = 0.001
    val w = weatherState ?: return "--°"
    return if (abs(w.latitude - (loc.latitude ?: Double.NaN)) < eps &&
        abs(w.longitude - (loc.longitude ?: Double.NaN)) < eps
    ) {
        w.current_weather?.temperature?.let { "${it.toInt()}°" } ?: "--°"
    } else {
        "--°"
    }
}
@Composable
private fun CurrentLocationCard(
    tempString: String,
    weatherState: com.example.finalproject.network.WeatherResponse?,
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
                val cityName = weatherState?.timezone?.let { formatCityNameFromTimezone(it) } ?: "Current location"
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
