package com.guyghost.wakeve.weather

import com.guyghost.wakeve.database.WakeveDb
import com.guyghost.wakeve.models.Coordinates
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class SqlDelightEventWeatherCache(
    private val db: WakeveDb,
    private val json: Json = Json { ignoreUnknownKeys = true }
) : EventWeatherCache {
    override fun getSnapshot(
        eventId: String,
        locationId: String,
        startDate: String,
        endDate: String,
        providerName: String
    ): WeatherSnapshot? {
        return db.eventWeatherQueries
            .selectWeatherSnapshot(eventId, locationId, startDate, endDate, providerName)
            .executeAsOneOrNull()
            ?.toSnapshot(json)
    }

    override fun getLatestSnapshot(eventId: String): WeatherSnapshot? {
        return db.eventWeatherQueries
            .selectLatestWeatherSnapshotForEvent(eventId)
            .executeAsOneOrNull()
            ?.toSnapshot(json)
    }

    override fun saveSnapshot(snapshot: WeatherSnapshot) {
        db.eventWeatherQueries.upsertWeatherSnapshot(
            id = snapshot.stableId(),
            eventId = snapshot.eventId,
            locationId = snapshot.locationId,
            locationLabel = snapshot.locationLabel,
            latitude = snapshot.coordinates.latitude,
            longitude = snapshot.coordinates.longitude,
            startDate = snapshot.startDate,
            endDate = snapshot.endDate,
            providerName = snapshot.providerName,
            fetchedAt = snapshot.fetchedAt,
            expiresAt = snapshot.expiresAt,
            dailyForecastsJson = json.encodeToString(snapshot.dailyForecasts)
        )
    }

    private fun WeatherSnapshot.stableId(): String =
        listOf(eventId, locationId, startDate, endDate, providerName)
            .joinToString("_") { it.replace(Regex("[^A-Za-z0-9]"), "_") }
}

private fun com.guyghost.wakeve.EventWeatherSnapshot.toSnapshot(json: Json): WeatherSnapshot =
    WeatherSnapshot(
        eventId = eventId,
        locationId = locationId,
        locationLabel = locationLabel,
        coordinates = Coordinates(latitude, longitude),
        startDate = startDate,
        endDate = endDate,
        providerName = providerName,
        fetchedAt = fetchedAt,
        expiresAt = expiresAt,
        dailyForecasts = json.decodeFromString(dailyForecastsJson)
    )
