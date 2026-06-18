package com.guyghost.wakeve.weather

import com.guyghost.wakeve.models.Event
import com.guyghost.wakeve.repository.EventRepositoryInterface
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

class EventWeatherService(
    private val eventRepository: EventRepositoryInterface,
    private val locationRepository: EventWeatherLocationRepository,
    private val weatherCache: EventWeatherCache,
    private val weatherProvider: EventWeatherProvider,
    private val nowProvider: () -> String
) {
    suspend fun loadWeatherContext(
        eventId: String,
        networkAvailable: Boolean = true
    ): EventWeatherContext {
        val event = eventRepository.getEvent(eventId)
            ?: return unavailable(eventId, WeatherAvailability.PROVIDER_UNAVAILABLE, "Event not found")

        val dateRange = event.weatherDateRange()
            ?: return unavailable(eventId, WeatherAvailability.PENDING_FORECAST_WINDOW, "Event date is not confirmed")

        val location = locationRepository.getWeatherLocation(eventId)
            ?: return cachedOrUnavailable(
                eventId = eventId,
                availability = WeatherAvailability.MISSING_LOCATION,
                now = nowProvider()
            )

        val now = nowProvider()
        val cached = weatherCache.getSnapshot(
            eventId = eventId,
            locationId = location.id,
            startDate = dateRange.startDate,
            endDate = dateRange.endDate,
            providerName = weatherProvider.providerName
        )

        if (!networkAvailable) {
            return cached?.toContext(eventId, location, now)
                ?: unavailable(eventId, WeatherAvailability.OFFLINE_UNAVAILABLE, "Weather is unavailable offline until it has been fetched once")
        }

        val daysUntilStart = daysBetween(now, dateRange.startDate, dateRange.timezone)
        if (daysUntilStart > weatherProvider.forecastWindowDays) {
            return cached?.toContext(eventId, location, now)
                ?: EventWeatherContext(
                    eventId = eventId,
                    availability = WeatherAvailability.PENDING_FORECAST_WINDOW,
                    location = location,
                    startDate = dateRange.startDate,
                    endDate = dateRange.endDate,
                    earliestRefreshDate = refreshDateFor(dateRange.startDate, weatherProvider.forecastWindowDays),
                    message = "Weather will be available closer to the event date"
                )
        }

        return when (
            val result = weatherProvider.fetchDailyForecast(
                WeatherForecastRequest(
                    eventId = eventId,
                    coordinates = location.coordinates,
                    startDate = dateRange.startDate,
                    endDate = dateRange.endDate,
                    timezone = dateRange.timezone
                )
            )
        ) {
            is WeatherProviderResult.Available -> {
                val snapshot = WeatherSnapshot(
                    eventId = eventId,
                    locationId = location.id,
                    locationLabel = location.label,
                    coordinates = location.coordinates,
                    startDate = dateRange.startDate,
                    endDate = dateRange.endDate,
                    providerName = result.providerName,
                    fetchedAt = result.fetchedAt,
                    expiresAt = result.expiresAt,
                    dailyForecasts = result.dailyForecasts
                )
                weatherCache.saveSnapshot(snapshot)
                snapshot.toContext(eventId, location, now)
            }
            is WeatherProviderResult.Unavailable -> {
                cached?.toContext(eventId, location, now)
                    ?: EventWeatherContext(
                        eventId = eventId,
                        availability = result.availability,
                        location = location,
                        startDate = dateRange.startDate,
                        endDate = dateRange.endDate,
                        providerName = weatherProvider.providerName,
                        earliestRefreshDate = result.earliestRefreshDate,
                        message = result.message
                    )
            }
        }
    }

    private fun cachedOrUnavailable(
        eventId: String,
        availability: WeatherAvailability,
        now: String
    ): EventWeatherContext {
        val cached = weatherCache.getLatestSnapshot(eventId)
        return cached?.toContext(
            eventId = eventId,
            location = EventWeatherLocation(
                id = cached.locationId,
                eventId = eventId,
                label = cached.locationLabel,
                address = null,
                coordinates = cached.coordinates,
                source = EventWeatherLocationSource.MANUAL
            ),
            now = now
        ) ?: unavailable(eventId, availability)
    }

    private fun unavailable(
        eventId: String,
        availability: WeatherAvailability,
        message: String? = null
    ): EventWeatherContext = EventWeatherContext(
        eventId = eventId,
        availability = availability,
        message = message
    )

    private data class WeatherDateRange(
        val startDate: String,
        val endDate: String,
        val timezone: String
    )

    private fun Event.weatherDateRange(): WeatherDateRange? {
        val confirmedSlot = finalDate?.let { confirmedDate ->
            proposedSlots.firstOrNull { it.start == confirmedDate } ?: proposedSlots.firstOrNull { it.id == confirmedDate }
        } ?: proposedSlots.firstOrNull { it.start != null }

        val start = finalDate?.takeIf { it.contains("T") } ?: confirmedSlot?.start ?: return null
        val end = confirmedSlot?.end ?: start
        val timezone = confirmedSlot?.timezone ?: "UTC"
        return WeatherDateRange(
            startDate = start.toIsoDate(timezone),
            endDate = end.toIsoDate(timezone),
            timezone = timezone
        )
    }

    private fun WeatherSnapshot.toContext(
        eventId: String,
        location: EventWeatherLocation,
        now: String
    ): EventWeatherContext {
        val stale = expiresAt <= now
        return EventWeatherContext(
            eventId = eventId,
            availability = if (stale) WeatherAvailability.STALE else WeatherAvailability.AVAILABLE,
            location = location,
            startDate = startDate,
            endDate = endDate,
            dailyForecasts = dailyForecasts,
            providerName = providerName,
            fetchedAt = fetchedAt,
            expiresAt = expiresAt,
            isStale = stale
        )
    }
}

private fun String.toIsoDate(timezoneId: String): String {
    return try {
        val timeZone = TimeZone.of(timezoneId)
        Instant.parse(this).toLocalDateTime(timeZone).date.toString()
    } catch (_: Exception) {
        substringBefore("T")
    }
}

private fun daysBetween(nowIso: String, targetDateIso: String, timezoneId: String): Int {
    return try {
        val timeZone = TimeZone.of(timezoneId)
        val nowDate = Instant.parse(nowIso).toLocalDateTime(timeZone).date
        val targetDate = LocalDate.parse(targetDateIso)
        nowDate.daysUntil(targetDate)
    } catch (_: Exception) {
        0
    }
}

private fun refreshDateFor(targetDateIso: String, forecastWindowDays: Int): String {
    return try {
        val target = LocalDate.parse(targetDateIso)
        target.plus(DatePeriod(days = -forecastWindowDays + 1)).toString()
    } catch (_: Exception) {
        targetDateIso
    }
}
