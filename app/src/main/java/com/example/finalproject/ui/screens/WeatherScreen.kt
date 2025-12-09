package com.example.finalproject.ui.screens

import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.finalproject.network.WeatherResponse
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import kotlin.math.abs
import kotlin.math.roundToInt

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
    val hourlyTimes = weather?.hourly?.time ?: emptyList()
    val hourlyTemps = weather?.hourly?.temperature_2m ?: emptyList()
    val hourlyHumidity = weather?.hourly?.relativehumidity_2m ?: emptyList()
    val hourlyPressure = weather?.hourly?.pressure_msl ?: emptyList()
    val hourlyVisibility = weather?.hourly?.visibility ?: emptyList()
    val hourlyWeatherCodes = weather?.hourly?.weathercode ?: emptyList()
    val dailySunrise = weather?.daily?.sunrise ?: emptyList()
    val dailySunset = weather?.daily?.sunset ?: emptyList()
    val dailyPrecipitationSum = weather?.daily?.precipitation_sum ?: emptyList()
    val currentHour = hourFromIso(weather?.current_weather?.time)
    val byHourIndex = if (currentHour != null && hourlyTimes.isNotEmpty()) findHourlyIndexByHour(hourlyTimes, currentHour) else -1
    val fallbackNearest = findNearestHourlyIndex(weather).takeIf { it >= 0 } ?: 0
    val startIndex = (if (byHourIndex >= 0) byHourIndex else fallbackNearest).coerceAtLeast(0)
    val available = minOf(hourlyTimes.size, hourlyTemps.size)
    val windowSize = minOf(24, available)
    val windowStart = when {
        available == 0 -> 0
        startIndex <= available - windowSize -> startIndex
        else -> (available - windowSize).coerceAtLeast(0)
    }
    val displayHours = (windowStart until (windowStart + windowSize)).toList()
    val topTempDouble = hourlyTemps.getOrNull(startIndex) ?: weather?.current_weather?.temperature
    val topTempDisplay = topTempDouble?.roundToInt()
    val feelsLike = topTempDouble?.let { (it - 2).roundToInt() }
    val hourlyListState = rememberLazyListState()
    LaunchedEffect(windowStart, displayHours.size) {
        if (displayHours.isNotEmpty()) {
            hourlyListState.animateScrollToItem(0)
        }
    }
    val dailySummaries = computeDailySummaries(hourlyTimes, hourlyTemps, hourlyHumidity, hourlyWeatherCodes, weather?.current_weather?.weathercode).take(10)
    val nowIdx = startIndex.coerceIn(0, maxOf(0, hourlyTimes.size - 1))
    val precipNow: Double? = try {
        val fld = weather?.hourly?.javaClass?.declaredFields?.firstOrNull { it.name == "precipitation" }
        val byHourly = fld?.let { f ->
            f.isAccessible = true
            (f.get(weather.hourly) as? List<Double>)?.getOrNull(nowIdx)
        }
        byHourly
    } catch (_: Exception) {
        null
    } ?: dailyPrecipitationSum.getOrNull(0)
    val humidityNow = hourlyHumidity.getOrNull(nowIdx)
    val visibilityNowMeters = hourlyVisibility.getOrNull(nowIdx)
    val visibilityNowKm = visibilityNowMeters?.let { (it / 1000).toInt() }
    val pressureNow = hourlyPressure.getOrNull(nowIdx) ?: weather?.current_weather?.let { null }
    val windSpeedNow = weather?.current_weather?.windspeed
    val windDirNow = weather?.current_weather?.winddirection
    val todayIndexInDaily = findNearestDailyIndex(weather)
    val sunriseTodayIso = dailySunrise.getOrNull(todayIndexInDaily)
    val sunsetTodayIso = dailySunset.getOrNull(todayIndexInDaily)
    val metrics = listOf(
        MetricCardData(
            title = "Precipitation",
            emoji = "\uD83C\uDF27",
            value = precipNow?.let { "${formatDouble(it)} mm" } ?: "-- mm"
        ),
        MetricCardData(
            title = "Wind",
            emoji = "🧭",
            value = windSpeedNow?.let { "${it} m/s" } ?: "-- m/s",
            windDir = windDirNow,
            windLabel = windDirNow?.let { degToCompassLabel(it) + " • ${it.toInt()}°" }
        ),
        MetricCardData(
            title = "Humidity",
            emoji = "\uD83D\uDCA7",
            value = humidityNow?.let { "${it.toInt()}%" } ?: "--%"
        ),
        MetricCardData(
            title = "Visibility",
            emoji = "\uD83D\uDD0D",
            value = visibilityNowKm?.let { "${it} km" } ?: "-- km"
        ),
        MetricCardData(
            title = "Pressure",
            emoji = "\uD83D\uDCA8",
            value = pressureNow?.let { "${it.toInt()} hPa" } ?: "-- hPa"
        ),
        MetricCardData(
            title = "Sunrise / Sunset",
            emoji = "\u2600\uFE0F",
            value = (sunriseTodayIso?.let { extractTimePart(it) } ?: "--:--") + " / " + (sunsetTodayIso?.let { extractTimePart(it) } ?: "--:--")
        )
    )
    val metricPairs = metrics.chunked(2)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Box(modifier = Modifier.fillMaxWidth()) {
                IconButton(onClick = { }, modifier = Modifier.align(Alignment.CenterStart)) {
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
                Icon(imageVector = Icons.Default.LocationOn, contentDescription = "location", modifier = Modifier.size(28.dp).align(Alignment.CenterEnd))
            }
        }

        item {
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                val description = mapWeatherCodeToText(weather?.current_weather?.weathercode)

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.padding(bottom = 6.dp)) {
                    Text(text = weatherEmoji(weather?.current_weather?.weathercode), fontSize = 32.sp, modifier = Modifier.padding(end = 8.dp))
                    Text(text = description, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground)
                }

                Text(text = topTempDisplay?.let { "${it}°" } ?: "--°", fontSize = 72.sp, style = MaterialTheme.typography.displaySmall, modifier = Modifier.padding(bottom = 6.dp))
                Text(text = feelsLike?.let { "Feels like ${it}°" } ?: "Feels like --", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.9f), modifier = Modifier.padding(bottom = 8.dp))

                Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().heightIn(min = 160.dp)) {
                    Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                        Text(text = "Погода по часам", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(bottom = 8.dp))
                        if (displayHours.isNotEmpty()) {
                            LazyRow(state = hourlyListState, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(displayHours) { idx ->
                                    val t = hourlyTemps.getOrNull(idx)
                                    val h = hourlyHumidity.getOrNull(idx)
                                    val code = hourlyWeatherCodes.getOrNull(idx) ?: weather?.current_weather?.weathercode
                                    HourlyCard(
                                        temperature = t,
                                        weatherCode = code,
                                        humidity = h,
                                        timeIso = hourlyTimes.getOrNull(idx)
                                    )
                                }
                            }
                        } else {
                            Text(text = "Hourly forecast is not available", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
                Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().heightIn(min = 160.dp)) {
                    Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                        Text(text = "Погода по дням", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(bottom = 8.dp))
                        if (dailySummaries.isNotEmpty()) {
                            LazyRow(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(dailySummaries) { ds ->
                                    DailyCard(dateLabel = ds.dateLabel, avgTemp = ds.avgTemp, humidity = ds.avgHumidity, weatherCode = ds.code)
                                }
                            }
                        } else {
                            Text(text = "Daily forecast is not available", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
                        }
                    }
                }
            }
        }

        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(text = "Now metrics", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))

                metricPairs.forEach { pair ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        pair.forEach { metric ->
                            MetricSquareCard(metric = metric, modifier = Modifier.weight(1f))
                        }
                        if (pair.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}
private data class MetricCardData(
    val title: String,
    val emoji: String,
    val value: String,
    val windDir: Double? = null,
    val windLabel: String? = null
)
@Composable
private fun MetricSquareCard(metric: MetricCardData, modifier: Modifier = Modifier) {
    Card(
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .height(140.dp)
    ) {
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.align(Alignment.TopStart)
            ) {
                Text(text = metric.emoji, fontSize = 18.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = metric.title,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (metric.title == "Wind") {
                val deg = metric.windDir?.toFloat() ?: 0f
                Icon(
                    imageVector = Icons.Default.ArrowUpward,
                    contentDescription = "wind direction",
                    modifier = Modifier
                        .size(44.dp)
                        .rotate(deg)
                        .align(Alignment.CenterStart)
                )
                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                ) {
                    Text(
                        text = metric.value,
                        style = MaterialTheme.typography.titleMedium,
                        fontSize = 30.sp
                    )
                    metric.windLabel?.let {
                        Text(text = it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                Text(
                    text = metric.value,
                    style = MaterialTheme.typography.titleMedium,
                    fontSize = 30.sp,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                )
            }
        }
    }
}
@Composable
private fun HourlyCard(
    temperature: Double?,
    weatherCode: Int?,
    humidity: Double?,
    timeIso: String?,
    modifier: Modifier = Modifier
) {
    Card(shape = RoundedCornerShape(12.dp), modifier = modifier.width(64.dp).height(140.dp)) {
        Column(modifier = Modifier.fillMaxSize().padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceBetween) {
            Text(text = temperature?.let { "${it.toInt()}°" } ?: "--°", style = MaterialTheme.typography.titleSmall)
            Text(text = weatherEmoji(weatherCode), fontSize = 20.sp)
            Text(text = humidity?.let { "${it.toInt()}%" } ?: "--%", style = MaterialTheme.typography.bodySmall)
            Text(text = timeIso?.let { formatHourLabel(it) } ?: "--:00", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
        }
    }
}
@Composable
private fun DailyCard(dateLabel: String, avgTemp: Int?, humidity: Int?, weatherCode: Int?, modifier: Modifier = Modifier) {
    Card(shape = RoundedCornerShape(12.dp), modifier = modifier.width(80.dp).height(140.dp)) {
        Column(modifier = Modifier.fillMaxSize().padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceBetween) {
            Text(text = avgTemp?.let { "${it}°" } ?: "--°", style = MaterialTheme.typography.titleSmall)
            Text(text = weatherEmoji(weatherCode), fontSize = 20.sp)
            Text(text = humidity?.let { "${it}%" } ?: "--%", style = MaterialTheme.typography.bodySmall)
            Text(text = dateLabel, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
        }
    }
}
private data class DailySummary(
    val dateLabel: String,
    val avgTemp: Int?,
    val avgHumidity: Int?,
    val code: Int?
)
private fun computeDailySummaries(
    hourlyTimes: List<String>,
    hourlyTemps: List<Double>,
    hourlyHumidity: List<Double>,
    hourlyCodes: List<Int>,
    fallbackCode: Int?
): List<DailySummary> {
    if (hourlyTimes.isEmpty() || hourlyTemps.isEmpty()) return emptyList()
    val map = linkedMapOf<String, MutableList<Int>>()
    for (i in hourlyTimes.indices) {
        val t = hourlyTimes[i]
        val datePart = t.substringBefore('T').substringBefore(' ')
        if (datePart.isBlank()) continue
        map.getOrPut(datePart) { mutableListOf() }.add(i)
    }
    val result = mutableListOf<DailySummary>()
    for ((datePart, indices) in map) {
        val temps = indices.mapNotNull { idx -> hourlyTemps.getOrNull(idx) }
        val avgTemp = if (temps.isNotEmpty()) (temps.average().roundToInt()) else null

        val hums = indices.mapNotNull { idx -> hourlyHumidity.getOrNull(idx) }
        val avgHum = if (hums.isNotEmpty()) (hums.average().roundToInt()) else null

        val codes = indices.mapNotNull { idx -> hourlyCodes.getOrNull(idx) }
        val modeCode: Int? = if (codes.isNotEmpty()) {
            codes.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key
        } else fallbackCode
        val label = try {
            val parts = datePart.split('-')
            if (parts.size >= 3) {
                val dd = parts[2].padStart(2, '0')
                val mm = parts[1].padStart(2, '0')
                "$dd.$mm"
            } else datePart
        } catch (_: Exception) {
            datePart
        }
        result.add(DailySummary(dateLabel = label, avgTemp = avgTemp, avgHumidity = avgHum, code = modeCode))
    }
    return result
}
private fun extractTimePart(dateTime: String): String {
    return dateTime.substringAfterLast('T').substringBefore(' ')
}
private fun formatHourLabel(isoTime: String): String {
    return try {
        isoTime.substringAfter("T").substring(0, 5)
    } catch (e: Exception) {
        "--:00"
    }
}
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
private fun findNearestDailyIndex(weather: WeatherResponse?): Int {
    val curTime = weather?.current_weather?.time ?: return 0
    val curDate = curTime.substringBefore('T').substringBefore(' ')
    val times = weather?.daily?.time ?: return 0
    for (i in times.indices) {
        val d = times[i].substringBefore('T').substringBefore(' ')
        if (d == curDate) return i
    }
    return 0
}
private fun hourFromIso(dateTime: String?): Int? {
    if (dateTime == null) return null
    return try {
        val timePart = dateTime.substringAfterLast('T').substringBefore(' ')
        val hourStr = timePart.substringBefore(':')
        hourStr.toIntOrNull()
    } catch (_: Exception) {
        null
    }
}
private fun findHourlyIndexByHour(hourlyTimes: List<String>, currentHour: Int): Int {
    for (i in hourlyTimes.indices) {
        val h = try {
            val timePart = hourlyTimes[i].substringAfterLast('T').substringBefore(' ')
            timePart.substringBefore(':').toIntOrNull()
        } catch (_: Exception) {
            null
        }
        if (h != null && h == currentHour) return i
    }
    return -1
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
private fun degToCompassLabel(deg: Double): String {
    val sectors = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
    val sector = ((deg + 22.5) % 360 / 45).toInt()
    return sectors[sector]
}
@SuppressLint("DefaultLocale")
private fun formatDouble(d: Double): String {
    return if (d % 1.0 == 0.0) d.toInt().toString() else String.format("%.1f", d)
}
