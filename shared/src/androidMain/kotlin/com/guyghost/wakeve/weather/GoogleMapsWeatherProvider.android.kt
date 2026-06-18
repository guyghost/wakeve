package com.guyghost.wakeve.weather

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class GoogleMapsWeatherProvider(
    private val apiKey: String,
    private val client: HttpClient = HttpClient(CIO),
    private val json: Json = Json { ignoreUnknownKeys = true }
) : EventWeatherProvider {
    override val providerName: String = "GoogleMapsWeather"
    override val forecastWindowDays: Int = 10

    override suspend fun fetchDailyForecast(request: WeatherForecastRequest): WeatherProviderResult {
        if (apiKey.isBlank()) {
            return WeatherProviderResult.Unavailable(
                availability = WeatherAvailability.PROVIDER_UNAVAILABLE,
                message = "Google Maps Weather API key is not configured"
            )
        }

        return try {
            val days = maxOf(1, minOf(forecastWindowDays, request.startDate.daysFromToday(request.timezone) + 1))
            val responseText = client.get("https://weather.googleapis.com/v1/forecast/days:lookup") {
                parameter("key", apiKey)
                parameter("location.latitude", request.coordinates.latitude)
                parameter("location.longitude", request.coordinates.longitude)
                parameter("days", days)
                parameter("unitsSystem", "METRIC")
            }.body<String>()

            val root = json.parseToJsonElement(responseText).jsonObject
            val daysArray = root["forecastDays"]?.jsonArray ?: root["days"]?.jsonArray.orEmpty()
            val forecasts = daysArray.mapNotNull { it.toDailyForecast() }
                .filter { it.date in request.startDate..request.endDate }

            if (forecasts.isEmpty()) {
                WeatherProviderResult.Unavailable(
                    availability = WeatherAvailability.PENDING_FORECAST_WINDOW,
                    message = "No Google weather forecast is available for this event date"
                )
            } else {
                val fetchedAt = Clock.System.now()
                WeatherProviderResult.Available(
                    dailyForecasts = forecasts,
                    fetchedAt = fetchedAt.toString(),
                    expiresAt = fetchedAt.plus(6, DateTimeUnit.HOUR).toString(),
                    providerName = providerName
                )
            }
        } catch (e: Exception) {
            WeatherProviderResult.Unavailable(
                availability = WeatherAvailability.PROVIDER_UNAVAILABLE,
                message = e.message ?: "Google Maps Weather API request failed"
            )
        }
    }
}

private fun JsonElement.toDailyForecast(): WeatherDailyForecast? {
    val obj = jsonObject
    val date = obj.stringAt("displayDate", "date", "interval", "startTime")
        ?.substringBefore("T")
        ?: return null
    val conditionText = obj.stringAt("weatherCondition", "condition", "daytimeForecast", "weatherCondition")
    val temperature = obj["temperature"]?.jsonObject
    val high = obj.doubleAt("maxTemperature", "highTemperature", "temperatureMax") ?: temperature?.doubleAt("max", "high")
    val low = obj.doubleAt("minTemperature", "lowTemperature", "temperatureMin") ?: temperature?.doubleAt("min", "low")
    val precipitation = obj.doubleAt("precipitationProbability", "precipitationChance")
    val wind = obj["wind"]?.jsonObject

    return WeatherDailyForecast(
        date = date,
        condition = conditionText.toWeatherCondition(),
        temperatureLowCelsius = low,
        temperatureHighCelsius = high,
        precipitationProbability = precipitation?.let { if (it > 1.0) it / 100.0 else it },
        windSpeedKph = obj.doubleAt("windSpeed", "maxWindSpeed") ?: wind?.doubleAt("speed", "maxSpeed"),
        summary = conditionText
    )
}

private fun JsonObject.stringAt(vararg path: String): String? {
    if (path.size == 1) {
        return this[path[0]]?.jsonPrimitive?.content
    }
    val head = this[path[0]]?.jsonObject ?: return null
    return head.stringAt(*path.drop(1).toTypedArray())
}

private fun JsonObject.doubleAt(vararg names: String): Double? =
    names.firstNotNullOfOrNull { name ->
        val value = this[name]
        when (value) {
            is JsonObject -> value["value"]?.jsonPrimitive?.doubleOrNull
            else -> value?.jsonPrimitive?.doubleOrNull
        }
    }

private fun String?.toWeatherCondition(): WeatherCondition {
    val value = this?.lowercase().orEmpty()
    return when {
        "storm" in value || "thunder" in value -> WeatherCondition.STORM
        "snow" in value -> WeatherCondition.SNOW
        "rain" in value || "shower" in value -> WeatherCondition.RAIN
        "wind" in value -> WeatherCondition.WINDY
        "fog" in value || "mist" in value -> WeatherCondition.FOG
        "part" in value && "cloud" in value -> WeatherCondition.PARTLY_CLOUDY
        "cloud" in value || "overcast" in value -> WeatherCondition.CLOUDY
        "sun" in value || "clear" in value -> WeatherCondition.SUNNY
        else -> WeatherCondition.UNKNOWN
    }
}

private fun String.daysFromToday(timezoneId: String): Int {
    return try {
        val today = Clock.System.now().toLocalDateTime(TimeZone.of(timezoneId)).date
        today.daysUntil(kotlinx.datetime.LocalDate.parse(this))
    } catch (_: Exception) {
        0
    }
}
