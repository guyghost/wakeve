package com.guyghost.wakeve.weather

import com.guyghost.wakeve.models.Event
import com.guyghost.wakeve.models.Scenario
import com.guyghost.wakeve.models.ScenarioStatus
import com.guyghost.wakeve.models.TimeSlot
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
    private val nowProvider: () -> String,
    private val scenarioRepository: EventWeatherScenarioRepository? = null
) {
    suspend fun loadWeatherContext(
        eventId: String,
        networkAvailable: Boolean = true
    ): EventWeatherContext {
        val event = eventRepository.getEvent(eventId)
            ?: return unavailable(eventId, WeatherAvailability.PROVIDER_UNAVAILABLE, "Event not found")

        val dateRange = event.weatherDateRange(scenarioRepository)
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

    private fun Event.weatherDateRange(
        scenarioRepository: EventWeatherScenarioRepository?
    ): WeatherDateRange? {
        return confirmedWeatherDateRange()
            ?: scenarioWeatherDateRange(scenarioRepository)
            ?: candidateTimeSlotWeatherDateRange()
    }

    private fun Event.confirmedWeatherDateRange(): WeatherDateRange? {
        val confirmedDate = finalDate ?: return null
        val confirmedSlot = proposedSlots.firstOrNull { it.start == confirmedDate }
            ?: proposedSlots.firstOrNull { it.id == confirmedDate }

        val start = confirmedDate.takeIf { it.contains("T") } ?: confirmedSlot?.start ?: return null
        val end = confirmedSlot?.end ?: start
        val timezone = confirmedSlot?.timezone ?: "UTC"
        return WeatherDateRange(
            startDate = start.toIsoDate(timezone),
            endDate = end.toIsoDate(timezone),
            timezone = timezone
        )
    }

    private fun Event.candidateTimeSlotWeatherDateRange(): WeatherDateRange? {
        return proposedSlots.firstOrNull { it.start != null }?.toWeatherDateRange()
    }

    private fun Event.scenarioWeatherDateRange(
        scenarioRepository: EventWeatherScenarioRepository?
    ): WeatherDateRange? {
        scenarioRepository ?: return null
        val selectedScenario = scenarioRepository.getSelectedScenario(id)
        val scenarios = selectedScenario?.let(::listOf)
            ?: scenarioRepository.getScenarios(id)
                .filter { it.status != ScenarioStatus.REJECTED }
                .sortedWith(compareBy<Scenario> { it.status.weatherPriority() }.thenBy { it.dateOrPeriod })

        return scenarios
            .mapNotNull { it.toWeatherDateRange(this) }
            .minByOrNull { it.startDate }
    }

    private fun Scenario.toWeatherDateRange(event: Event): WeatherDateRange? {
        val sourceSlotRange = sourceTimeSlotId
            ?.let { slotId -> event.proposedSlots.firstOrNull { it.id == slotId } }
            ?.toWeatherDateRange()
        if (sourceSlotRange != null) return sourceSlotRange

        val tokens = isoDateOrInstantPattern.findAll(dateOrPeriod)
            .map { it.value }
            .toList()
        val start = tokens.firstOrNull()?.toIsoDate("UTC") ?: return null
        val end = tokens.getOrNull(1)?.toIsoDate("UTC") ?: endDateFromDuration(start, duration)
        return WeatherDateRange(
            startDate = start,
            endDate = end,
            timezone = "UTC"
        )
    }

    private fun TimeSlot.toWeatherDateRange(): WeatherDateRange? {
        val start = start ?: return null
        val end = end ?: start
        return WeatherDateRange(
            startDate = start.toIsoDate(timezone),
            endDate = end.toIsoDate(timezone),
            timezone = timezone
        )
    }

    private fun ScenarioStatus.weatherPriority(): Int = when (this) {
        ScenarioStatus.SELECTED -> 0
        ScenarioStatus.PROPOSED -> 1
        ScenarioStatus.DRAFT -> 2
        ScenarioStatus.REJECTED -> 3
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

private val isoDateOrInstantPattern = Regex("""\d{4}-\d{2}-\d{2}(?:T[^\s/]+)?""")

private fun String.toIsoDate(timezoneId: String): String {
    return try {
        val timeZone = TimeZone.of(timezoneId)
        Instant.parse(this).toLocalDateTime(timeZone).date.toString()
    } catch (_: Exception) {
        substringBefore("T")
    }
}

private fun endDateFromDuration(startDateIso: String, durationDays: Int): String {
    return try {
        val extraDays = if (durationDays > 1) durationDays - 1 else 0
        LocalDate.parse(startDateIso).plus(DatePeriod(days = extraDays)).toString()
    } catch (_: Exception) {
        startDateIso
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
