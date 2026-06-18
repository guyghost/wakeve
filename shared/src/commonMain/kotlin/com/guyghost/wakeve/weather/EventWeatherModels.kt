package com.guyghost.wakeve.weather

import com.guyghost.wakeve.models.Coordinates
import kotlinx.serialization.Serializable

@Serializable
enum class WeatherAvailability {
    AVAILABLE,
    STALE,
    PENDING_FORECAST_WINDOW,
    MISSING_LOCATION,
    PERMISSION_OR_ENTITLEMENT_REQUIRED,
    PROVIDER_UNAVAILABLE,
    OFFLINE_UNAVAILABLE
}

@Serializable
enum class WeatherCondition {
    SUNNY,
    PARTLY_CLOUDY,
    CLOUDY,
    RAIN,
    SNOW,
    STORM,
    WINDY,
    FOG,
    UNKNOWN
}

@Serializable
data class EventWeatherLocation(
    val id: String,
    val eventId: String,
    val label: String,
    val address: String?,
    val coordinates: Coordinates,
    val source: EventWeatherLocationSource,
    val providerName: String? = null,
    val providerPlaceId: String? = null,
    val resolvedAt: String? = null
)

@Serializable
enum class EventWeatherLocationSource {
    POTENTIAL_LOCATION,
    RESOLVED_MAP_LOCATION,
    MANUAL
}

@Serializable
data class WeatherDailyForecast(
    val date: String,
    val condition: WeatherCondition,
    val temperatureLowCelsius: Double?,
    val temperatureHighCelsius: Double?,
    val precipitationProbability: Double?,
    val windSpeedKph: Double?,
    val summary: String?
)

@Serializable
data class WeatherSnapshot(
    val eventId: String,
    val locationId: String,
    val locationLabel: String,
    val coordinates: Coordinates,
    val startDate: String,
    val endDate: String,
    val providerName: String,
    val fetchedAt: String,
    val expiresAt: String,
    val dailyForecasts: List<WeatherDailyForecast>
)

@Serializable
data class EventWeatherContext(
    val eventId: String,
    val availability: WeatherAvailability,
    val location: EventWeatherLocation? = null,
    val startDate: String? = null,
    val endDate: String? = null,
    val dailyForecasts: List<WeatherDailyForecast> = emptyList(),
    val providerName: String? = null,
    val fetchedAt: String? = null,
    val expiresAt: String? = null,
    val isStale: Boolean = false,
    val earliestRefreshDate: String? = null,
    val message: String? = null
)

data class WeatherForecastRequest(
    val eventId: String,
    val coordinates: Coordinates,
    val startDate: String,
    val endDate: String,
    val timezone: String
)

sealed class WeatherProviderResult {
    data class Available(
        val dailyForecasts: List<WeatherDailyForecast>,
        val fetchedAt: String,
        val expiresAt: String,
        val providerName: String
    ) : WeatherProviderResult()

    data class Unavailable(
        val availability: WeatherAvailability,
        val message: String? = null,
        val earliestRefreshDate: String? = null
    ) : WeatherProviderResult()
}

interface EventWeatherProvider {
    val providerName: String
    val forecastWindowDays: Int

    suspend fun fetchDailyForecast(request: WeatherForecastRequest): WeatherProviderResult
}

interface EventWeatherCache {
    fun getSnapshot(
        eventId: String,
        locationId: String,
        startDate: String,
        endDate: String,
        providerName: String
    ): WeatherSnapshot?

    fun getLatestSnapshot(eventId: String): WeatherSnapshot?

    fun saveSnapshot(snapshot: WeatherSnapshot)
}

interface EventWeatherLocationRepository {
    fun getWeatherLocation(eventId: String): EventWeatherLocation?
    fun saveResolvedLocation(location: EventWeatherLocation)
}
