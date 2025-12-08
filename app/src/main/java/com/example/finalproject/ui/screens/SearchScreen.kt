package com.example.finalproject.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.finalproject.network.Location
import com.example.finalproject.viewmodel.WeatherViewModel
import kotlinx.coroutines.launch

@Composable
fun SearchScreen(
    viewModel: WeatherViewModel,
    onSelect: (Location) -> Unit,
    modifier: Modifier = Modifier,
    previewLocations: List<Location>? = null,
    previewCurrentTemp: String? = null
) {
    val scope = rememberCoroutineScope()
    val locationsState by viewModel.locations.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val error by viewModel.error.collectAsState()
    val weatherState by viewModel.weather.collectAsState() // WeatherResponse? or null
    val shownLocations = previewLocations ?: locationsState
    val currentTempString: String = previewCurrentTemp
        ?: weatherState?.current_weather?.temperature?.let { "${it.toInt()}°" } ?: "--°"

    var query by remember { mutableStateOf("") }

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
                onValueChange = { query = it },
                placeholder = { Text("City") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "search") },
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Button(
                onClick = { scope.launch { viewModel.search(query) } },
                modifier = Modifier.height(56.dp)
            ) {
                Text("Search")
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        CurrentLocationCard(
            tempString = currentTempString,
            onClick = {
                weatherState?.let {
                    onSelect(Location(name = "Current location", country = null, latitude = it.latitude, longitude = it.longitude))
                }
            }
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Saved places",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        if (loading) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            if (shownLocations.isNullOrEmpty()) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("No places found")
                }
            } else {
                Column(modifier = Modifier.fillMaxWidth()) {
                    shownLocations.forEach { loc ->
                        val tempForItem = weatherState?.let { w ->
                            if (w.latitude == loc.latitude && w.longitude == loc.longitude) {
                                w.current_weather?.temperature?.let { "${it.toInt()}°" } ?: "--°"
                            } else {
                                "--°"
                            }
                        } ?: "--°"

                        LocationRowWithTemp(
                            location = loc,
                            tempString = tempForItem,
                            onClick = { onSelect(loc) }
                        )

                        Spacer(modifier = Modifier.height(8.dp))
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

@Composable
private fun CurrentLocationCard(tempString: String, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(84.dp)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Place, contentDescription = "place", modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Current location", style = MaterialTheme.typography.bodyLarge)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(text = "Tap to view details", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Text(text = tempString, style = MaterialTheme.typography.displaySmall)
        }
    }
}

@Composable
fun LocationRowWithTemp(location: Location, tempString: String, onClick: () -> Unit) {
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

        Text(text = tempString, style = MaterialTheme.typography.headlineSmall)
    }
}



