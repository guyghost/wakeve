package com.guyghost.wakeve.weather

import com.guyghost.wakeve.createFreshTestDatabase
import com.guyghost.wakeve.models.Coordinates
import com.guyghost.wakeve.models.Event
import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.models.LocationType
import com.guyghost.wakeve.models.Scenario
import com.guyghost.wakeve.models.ScenarioStatus
import com.guyghost.wakeve.models.TimeSlot
import com.guyghost.wakeve.models.Vote
import com.guyghost.wakeve.repository.DatabaseEventRepository
import com.guyghost.wakeve.repository.ScenarioRepository
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class EventWeatherServiceTest {
    @Test
    fun confirmedEventUsesStoredLocationAndCachesAvailableForecast() {
        runBlocking {
            val db = createFreshTestDatabase()
            val eventRepository = DatabaseEventRepository(db)
            val cache = SqlDelightEventWeatherCache(db)
            val provider = FakeWeatherProvider(
                result = WeatherProviderResult.Available(
                    dailyForecasts = listOf(
                        WeatherDailyForecast(
                            date = "2026-06-22",
                            condition = WeatherCondition.SUNNY,
                            temperatureLowCelsius = 18.0,
                            temperatureHighCelsius = 27.0,
                            precipitationProbability = 0.12,
                            windSpeedKph = 14.0,
                            summary = "Clear and warm"
                        )
                    ),
                    fetchedAt = "2026-06-18T08:00:00Z",
                    expiresAt = "2026-06-18T14:00:00Z",
                    providerName = "FakeWeather"
                )
            )
            val service = EventWeatherService(
                eventRepository = eventRepository,
                locationRepository = DatabaseEventWeatherLocationRepository(db),
                weatherCache = cache,
                weatherProvider = provider,
                nowProvider = { "2026-06-18T09:00:00Z" }
            )

            createConfirmedEvent(eventRepository)
            insertLocation(db, coordinates = Coordinates(43.2965, 5.3698))

            val context = service.loadWeatherContext("event-weather-1")

            assertEquals(WeatherAvailability.AVAILABLE, context.availability)
            assertEquals("Marseille", context.location?.label)
            assertEquals(Coordinates(43.2965, 5.3698), provider.lastRequest?.coordinates)
            assertEquals("2026-06-22", context.dailyForecasts.single().date)
            assertNotNull(cache.getSnapshot("event-weather-1", "loc-1", "2026-06-22", "2026-06-22", "FakeWeather"))
        }
    }

    @Test
    fun futureEventOutsideForecastWindowDoesNotCallProvider() {
        runBlocking {
            val db = createFreshTestDatabase()
            val eventRepository = DatabaseEventRepository(db)
            val provider = FakeWeatherProvider()
            val service = EventWeatherService(
                eventRepository = eventRepository,
                locationRepository = DatabaseEventWeatherLocationRepository(db),
                weatherCache = SqlDelightEventWeatherCache(db),
                weatherProvider = provider,
                nowProvider = { "2026-06-18T09:00:00Z" }
            )

            createConfirmedEvent(
                eventRepository = eventRepository,
                start = "2026-07-20T09:00:00Z",
                end = "2026-07-20T17:00:00Z"
            )
            insertLocation(db, coordinates = Coordinates(43.2965, 5.3698))

            val context = service.loadWeatherContext("event-weather-1")

            assertEquals(WeatherAvailability.PENDING_FORECAST_WINDOW, context.availability)
            assertEquals("2026-07-11", context.earliestRefreshDate)
            assertEquals(null, provider.lastRequest)
        }
    }

    @Test
    fun selectedScenarioDateRangeFeedsForecastRequestWhenEventDateIsNotConfirmed() {
        runBlocking {
            val db = createFreshTestDatabase()
            val eventRepository = DatabaseEventRepository(db)
            val scenarioRepository = ScenarioRepository(db)
            val provider = FakeWeatherProvider(
                result = WeatherProviderResult.Available(
                    dailyForecasts = listOf(
                        WeatherDailyForecast(
                            date = "2026-06-22",
                            condition = WeatherCondition.PARTLY_CLOUDY,
                            temperatureLowCelsius = 17.0,
                            temperatureHighCelsius = 25.0,
                            precipitationProbability = 0.2,
                            windSpeedKph = 11.0,
                            summary = "Good for a weekend plan"
                        )
                    ),
                    fetchedAt = "2026-06-18T08:00:00Z",
                    expiresAt = "2026-06-18T14:00:00Z",
                    providerName = "FakeWeather"
                )
            )
            val service = EventWeatherService(
                eventRepository = eventRepository,
                locationRepository = DatabaseEventWeatherLocationRepository(db),
                weatherCache = SqlDelightEventWeatherCache(db),
                weatherProvider = provider,
                nowProvider = { "2026-06-18T09:00:00Z" },
                scenarioRepository = ScenarioEventWeatherRepository(scenarioRepository)
            )

            createUnconfirmedScenarioEvent(eventRepository)
            scenarioRepository.createScenario(
                testScenario(
                    dateOrPeriod = "2026-06-22/2026-06-24",
                    duration = 3,
                    status = ScenarioStatus.SELECTED
                )
            ).getOrThrow()
            insertLocation(db, coordinates = Coordinates(43.2965, 5.3698))

            val context = service.loadWeatherContext("event-weather-1")

            assertEquals(WeatherAvailability.AVAILABLE, context.availability)
            assertEquals("2026-06-22", provider.lastRequest?.startDate)
            assertEquals("2026-06-24", provider.lastRequest?.endDate)
            assertEquals("UTC", provider.lastRequest?.timezone)
        }
    }

    @Test
    fun confirmedDateTakesPriorityOverSelectedScenarioDate() {
        runBlocking {
            val db = createFreshTestDatabase()
            val eventRepository = DatabaseEventRepository(db)
            val scenarioRepository = ScenarioRepository(db)
            val provider = FakeWeatherProvider(
                result = WeatherProviderResult.Available(
                    dailyForecasts = listOf(
                        WeatherDailyForecast(
                            date = "2026-06-22",
                            condition = WeatherCondition.SUNNY,
                            temperatureLowCelsius = 18.0,
                            temperatureHighCelsius = 27.0,
                            precipitationProbability = 0.12,
                            windSpeedKph = 14.0,
                            summary = "Confirmed date wins"
                        )
                    ),
                    fetchedAt = "2026-06-18T08:00:00Z",
                    expiresAt = "2026-06-18T14:00:00Z",
                    providerName = "FakeWeather"
                )
            )
            val service = EventWeatherService(
                eventRepository = eventRepository,
                locationRepository = DatabaseEventWeatherLocationRepository(db),
                weatherCache = SqlDelightEventWeatherCache(db),
                weatherProvider = provider,
                nowProvider = { "2026-06-18T09:00:00Z" },
                scenarioRepository = ScenarioEventWeatherRepository(scenarioRepository)
            )

            createConfirmedEvent(eventRepository)
            scenarioRepository.createScenario(
                testScenario(
                    dateOrPeriod = "2026-06-25/2026-06-26",
                    duration = 2,
                    status = ScenarioStatus.SELECTED
                )
            ).getOrThrow()
            insertLocation(db, coordinates = Coordinates(43.2965, 5.3698))

            service.loadWeatherContext("event-weather-1")

            assertEquals("2026-06-22", provider.lastRequest?.startDate)
            assertEquals("2026-06-22", provider.lastRequest?.endDate)
            assertEquals("Europe/Paris", provider.lastRequest?.timezone)
        }
    }

    @Test
    fun offlineModeReturnsStaleCachedWeatherWhenAvailable() {
        runBlocking {
            val db = createFreshTestDatabase()
            val eventRepository = DatabaseEventRepository(db)
            val cache = SqlDelightEventWeatherCache(db)
            val service = EventWeatherService(
                eventRepository = eventRepository,
                locationRepository = DatabaseEventWeatherLocationRepository(db),
                weatherCache = cache,
                weatherProvider = FakeWeatherProvider(),
                nowProvider = { "2026-06-18T09:00:00Z" }
            )

            createConfirmedEvent(eventRepository)
            insertLocation(db, coordinates = Coordinates(43.2965, 5.3698))
            cache.saveSnapshot(
                WeatherSnapshot(
                    eventId = "event-weather-1",
                    locationId = "loc-1",
                    locationLabel = "Marseille",
                    coordinates = Coordinates(43.2965, 5.3698),
                    startDate = "2026-06-22",
                    endDate = "2026-06-22",
                    providerName = "FakeWeather",
                    fetchedAt = "2026-06-17T08:00:00Z",
                    expiresAt = "2026-06-17T14:00:00Z",
                    dailyForecasts = listOf(
                        WeatherDailyForecast(
                            date = "2026-06-22",
                            condition = WeatherCondition.CLOUDY,
                            temperatureLowCelsius = 16.0,
                            temperatureHighCelsius = 24.0,
                            precipitationProbability = 0.35,
                            windSpeedKph = 9.0,
                            summary = "Cached clouds"
                        )
                    )
                )
            )

            val context = service.loadWeatherContext("event-weather-1", networkAvailable = false)

            assertEquals(WeatherAvailability.STALE, context.availability)
            assertTrue(context.isStale)
            assertEquals("Cached clouds", context.dailyForecasts.single().summary)
        }
    }

    @Test
    fun missingCoordinatesReturnMissingLocation() {
        runBlocking {
            val db = createFreshTestDatabase()
            val eventRepository = DatabaseEventRepository(db)
            val provider = FakeWeatherProvider()
            val service = EventWeatherService(
                eventRepository = eventRepository,
                locationRepository = DatabaseEventWeatherLocationRepository(db),
                weatherCache = SqlDelightEventWeatherCache(db),
                weatherProvider = provider,
                nowProvider = { "2026-06-18T09:00:00Z" }
            )

            createConfirmedEvent(eventRepository)
            insertLocation(db, coordinates = null)

            val context = service.loadWeatherContext("event-weather-1")

            assertEquals(WeatherAvailability.MISSING_LOCATION, context.availability)
            assertEquals(null, provider.lastRequest)
        }
    }

    @Test
    fun androidFallbackProviderMapsToProviderUnavailable() {
        runBlocking {
            val db = createFreshTestDatabase()
            val eventRepository = DatabaseEventRepository(db)
            val service = EventWeatherService(
                eventRepository = eventRepository,
                locationRepository = DatabaseEventWeatherLocationRepository(db),
                weatherCache = SqlDelightEventWeatherCache(db),
                weatherProvider = AndroidEventWeatherProvider(),
                nowProvider = { "2026-06-18T09:00:00Z" }
            )

            createConfirmedEvent(eventRepository)
            insertLocation(db, coordinates = Coordinates(43.2965, 5.3698))

            val context = service.loadWeatherContext("event-weather-1")

            assertEquals(WeatherAvailability.PROVIDER_UNAVAILABLE, context.availability)
        }
    }

    private suspend fun createConfirmedEvent(
        eventRepository: DatabaseEventRepository,
        start: String = "2026-06-22T09:00:00Z",
        end: String = "2026-06-22T17:00:00Z"
    ) {
        val slot = TimeSlot(
            id = "slot-weather-1",
            start = start,
            end = end,
            timezone = "Europe/Paris"
        )
        eventRepository.createEvent(
            Event(
                id = "event-weather-1",
                title = "Outdoor event",
                description = "Weather-aware event",
                organizerId = "organizer",
                participants = listOf("organizer", "alice"),
                proposedSlots = listOf(slot),
                deadline = "9999-12-31T23:59:59Z",
                status = EventStatus.POLLING,
                createdAt = "2026-06-01T08:00:00Z",
                updatedAt = "2026-06-01T08:00:00Z"
            )
        ).getOrThrow()
        eventRepository.addVote("event-weather-1", "organizer", "slot-weather-1", Vote.YES).getOrThrow()
        eventRepository.confirmEventDate("event-weather-1", "slot-weather-1", "organizer").getOrThrow()
    }

    private suspend fun createUnconfirmedScenarioEvent(
        eventRepository: DatabaseEventRepository
    ) {
        eventRepository.createEvent(
            Event(
                id = "event-weather-1",
                title = "Scenario event",
                description = "Weather-aware scenario planning",
                organizerId = "organizer",
                participants = listOf("organizer", "alice"),
                proposedSlots = emptyList(),
                deadline = "2026-06-20T12:00:00Z",
                status = EventStatus.COMPARING,
                createdAt = "2026-06-01T08:00:00Z",
                updatedAt = "2026-06-01T08:00:00Z"
            )
        ).getOrThrow()
    }

    private fun testScenario(
        dateOrPeriod: String,
        duration: Int,
        status: ScenarioStatus
    ): Scenario {
        return Scenario(
            id = "scenario-weather-1",
            eventId = "event-weather-1",
            name = "Marseille weekend",
            dateOrPeriod = dateOrPeriod,
            location = "Marseille, France",
            duration = duration,
            estimatedParticipants = 4,
            estimatedBudgetPerPerson = 180.0,
            description = "Scenario with an explicit weather date range",
            status = status,
            createdAt = "2026-06-01T08:00:00Z",
            updatedAt = "2026-06-01T08:00:00Z"
        )
    }

    private fun insertLocation(
        db: com.guyghost.wakeve.database.WakeveDb,
        coordinates: Coordinates?
    ) {
        db.potentialLocationQueries.insertLocation(
            id = "loc-1",
            eventId = "event-weather-1",
            name = "Marseille",
            locationType = LocationType.CITY.name,
            address = "Marseille, France",
            coordinates = coordinates?.toJson(),
            createdAt = "2026-06-01T08:00:00Z"
        )
    }

    private class FakeWeatherProvider(
        private val result: WeatherProviderResult = WeatherProviderResult.Unavailable(
            WeatherAvailability.PROVIDER_UNAVAILABLE,
            "No fake weather configured"
        )
    ) : EventWeatherProvider {
        var lastRequest: WeatherForecastRequest? = null

        override val providerName: String = "FakeWeather"
        override val forecastWindowDays: Int = 10

        override suspend fun fetchDailyForecast(request: WeatherForecastRequest): WeatherProviderResult {
            lastRequest = request
            return result
        }
    }
}
