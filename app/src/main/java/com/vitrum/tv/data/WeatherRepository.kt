package com.vitrum.tv.data

import com.vitrum.tv.core.Http
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.double
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Request
import java.net.URLEncoder

data class GeoResult(
    val name: String,
    val country: String?,
    val latitude: Double,
    val longitude: Double,
)

data class WeatherNow(
    val temperature: Double,
    val code: Int,
)

/** Weather is optional; the only request goes to Open-Meteo over HTTPS. */
class WeatherRepository {

    suspend fun geocode(query: String, language: String = "en"): GeoResult? = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext null
        val encoded = URLEncoder.encode(query.trim(), "UTF-8")
        val url = "https://geocoding-api.open-meteo.com/v1/search?name=$encoded&count=1&language=$language&format=json"
        try {
            Http.client.newCall(Request.Builder().url(url).header("User-Agent", Http.USER_AGENT).build())
                .execute().use { response ->
                    if (!response.isSuccessful) return@withContext null
                    val root = Json.parseToJsonElement(response.body?.string().orEmpty()).jsonObject
                    val results = root["results"]?.jsonArray ?: return@withContext null
                    val first = results.firstOrNull()?.jsonObject ?: return@withContext null
                    val name = first["name"]?.jsonPrimitive?.content ?: return@withContext null
                    val country = first["country"]?.jsonPrimitive?.content
                    val latitude = first["latitude"]?.jsonPrimitive?.double ?: return@withContext null
                    val longitude = first["longitude"]?.jsonPrimitive?.double ?: return@withContext null
                    GeoResult(name, country, latitude, longitude)
                }
        } catch (_: Exception) {
            null
        }
    }

    suspend fun current(
        latitude: Double,
        longitude: Double,
        celsius: Boolean,
    ): WeatherNow? = withContext(Dispatchers.IO) {
        val unit = if (celsius) "celsius" else "fahrenheit"
        val url = "https://api.open-meteo.com/v1/forecast?latitude=$latitude&longitude=$longitude" +
            "&current=temperature_2m,weather_code&temperature_unit=$unit&timezone=auto"
        try {
            Http.client.newCall(Request.Builder().url(url).header("User-Agent", Http.USER_AGENT).build())
                .execute().use { response ->
                    if (!response.isSuccessful) return@withContext null
                    val root = Json.parseToJsonElement(response.body?.string().orEmpty()).jsonObject
                    val current = root["current"]?.jsonObject ?: return@withContext null
                    val temp = current["temperature_2m"]?.jsonPrimitive?.double ?: return@withContext null
                    val code = current["weather_code"]?.jsonPrimitive?.int ?: 0
                    WeatherNow(temp, code)
                }
        } catch (_: Exception) {
            null
        }
    }
}

/** WMO weather code to a short label key understood by the UI. */
fun weatherDescriptionKey(code: Int): String = when (code) {
    0 -> "clear"
    1, 2 -> "partly"
    3 -> "cloudy"
    45, 48 -> "fog"
    51, 53, 55, 56, 57 -> "drizzle"
    61, 63, 65, 66, 67, 80, 81, 82 -> "rain"
    71, 73, 75, 77, 85, 86 -> "snow"
    95, 96, 99 -> "storm"
    else -> "cloudy"
}
