package com.example.finalproject.ui.screens

import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.finalproject.network.WeatherResponse
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import kotlin.math.abs

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun WeatherScreen(
    cityName: String? = null,
    weather: WeatherResponse?,
    @SuppressLint("ModifierParameter") modifier: Modifier = Modifier
) {
    val headerTitle: String = cityName?.takeIf { it.isNotBlank() }
        ?: weather?.timezone?.takeIf { it.isNotBlank() }
        ?: weather?.let { "${"%.2f".format(it.latitude)}, ${"%.2f".format(it.longitude)}" }
        ?: "Unknown"
    val description = mapWeatherCodeToText(weather?.current_weather?.weathercode)
    val temp = weather?.current_weather?.temperature
    val time = weather?.current_weather?.time
    val idx = findNearestHourlyIndex(weather)
    val pressureStr = formatPressureAtIndex(weather, idx)
    val humidityStr = formatHumidityAtIndex(weather, idx)
    val visibilityStr = formatVisibilityAtIndex(weather, idx)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Box(modifier = Modifier.fillMaxWidth()) {
                IconButton(
                    onClick = {},
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                }

                Text(
                    text = headerTitle,
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 40.dp),
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )

                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "location",
                    modifier = Modifier
                        .size(28.dp)
                        .align(Alignment.CenterEnd)
                )
            }
        }

        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(bottom = 6.dp)
                ) {
                    Text(
                        text = weatherEmoji(weather?.current_weather?.weathercode),
                        fontSize = 32.sp,
                        modifier = Modifier.padding(end = 8.dp)
                    )

                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                Text(
                    text = temp?.let { "${it.toInt()}°" } ?: "--°",
                    fontSize = 72.sp,
                    style = MaterialTheme.typography.displaySmall,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                Text(
                    text = time?.let { extractTimePart(it) } ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SummaryCard(
                    title = "Wind",
                    value = weather?.current_weather?.windspeed?.let { "${it} m/s" } ?: "--",
                    modifier = Modifier.weight(1f).height(76.dp)
                )
                SummaryCard(
                    title = "Dir",
                    value = weather?.current_weather?.winddirection?.let { "${it}°" } ?: "--",
                    modifier = Modifier.weight(1f).height(76.dp)
                )
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Details", style = MaterialTheme.typography.titleMedium)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        DetailBlock(
                            label = "Temperature",
                            value = weather?.current_weather?.temperature?.let { "${it.toInt()}°" },
                            modifier = Modifier.weight(1f)
                        )
                        DetailBlock(
                            label = "Feels like",
                            value = weather?.current_weather?.temperature?.let { "${(it - 2).toInt()}°" },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        DetailBlock(
                            label = "Pressure",
                            value = pressureStr,
                            modifier = Modifier.weight(1f)
                        )
                        DetailBlock(
                            label = "Humidity",
                            value = humidityStr,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CircularInfo(
                    title = "Wind",
                    value = weather?.current_weather?.windspeed?.let { "${it} m/s" } ?: "--",
                    modifier = Modifier.weight(1f).height(120.dp)
                )
                CircularInfo(
                    title = "Visibility",
                    value = visibilityStr ?: "-- km",
                    modifier = Modifier.weight(1f).height(120.dp)
                )
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 88.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("Sunrise / Sunset", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        Column {
                            Text("Sunrise", style = MaterialTheme.typography.bodySmall)
                            Text("07:31", style = MaterialTheme.typography.bodyLarge)
                        }
                        Column {
                            Text("Sunset", style = MaterialTheme.typography.bodySmall)
                            Text("15:51", style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}
@Composable
private fun SummaryCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleMedium)
        }
    }
}
@Composable
private fun DetailBlock(label: String, value: String?, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(8.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value ?: "--", style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun CircularInfo(title: String, value: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(160.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(6.dp))
                Text(value, style = MaterialTheme.typography.headlineSmall)
            }
        }
    }
}
private fun extractTimePart(dateTime: String): String {
    return dateTime.substringAfterLast('T').substringBefore(' ')
}
@RequiresApi(Build.VERSION_CODES.O)
private fun parseToEpochSecondsUtc(dateTime: String): Long? {
    return try {
        val odt = OffsetDateTime.parse(dateTime)
        odt.toEpochSecond()
    } catch (_: Exception) {
        try {
            val ldt = LocalDateTime.parse(dateTime)
            ldt.toEpochSecond(ZoneOffset.UTC)
        } catch (e: Exception) {
            null
        }
    }
}
@RequiresApi(Build.VERSION_CODES.O)
private fun findNearestHourlyIndex(weather: WeatherResponse?): Int {
    val curTime = weather?.current_weather?.time ?: return -1
    val times = weather.hourly?.time ?: return -1
    val curEpoch = parseToEpochSecondsUtc(curTime) ?: return -1

    var bestIdx = -1
    var bestDiff = Long.MAX_VALUE
    for (i in times.indices) {
        val tEpoch = parseToEpochSecondsUtc(times[i]) ?: continue
        val diff = abs(curEpoch - tEpoch)
        if (diff < bestDiff) {
            bestDiff = diff
            bestIdx = i
        }
    }
    return bestIdx
}
private fun formatPressureAtIndex(weather: WeatherResponse?, idx: Int): String? {
    if (idx < 0) return null
    val pressures = weather?.hourly?.pressure_msl
    return pressures?.getOrNull(idx)?.let { "${it.toInt()} hPa" }
}
private fun formatHumidityAtIndex(weather: WeatherResponse?, idx: Int): String? {
    if (idx < 0) return null
    val humid = weather?.hourly?.relativehumidity_2m
    return humid?.getOrNull(idx)?.let { "${it.toInt()} %" }
}
private fun formatVisibilityAtIndex(weather: WeatherResponse?, idx: Int): String? {
    if (idx < 0) return null
    val vis = weather?.hourly?.visibility ?: return null
    return vis.getOrNull(idx)?.let { "${(it / 1000).toInt()} km" }
}
private fun weatherEmoji(code: Int?): String {
    return when (code) {
        null -> "❔"
        0 -> "☀️"
        1, 2 -> "🌤️"
        3 -> "☁️"
        45, 48 -> "🌫️"
        51, 53, 55 -> "🌦️"
        61, 63, 65 -> "🌧️"
        71, 73, 75, 77 -> "❄️"
        80, 81, 82 -> "🌧️"
        95, 96, 99 -> "⛈️"
        else -> "☁️"
    }
}
private fun mapWeatherCodeToText(code: Int?): String {
    return when (code) {
        null -> ""
        0 -> "Clear"
        1, 2, 3 -> "Partly cloudy"
        45, 48 -> "Fog"
        51, 53, 55 -> "Drizzle"
        61, 63, 65 -> "Rain"
        71, 73, 75, 77 -> "Snow"
        else -> "Cloudy"
    }
}
