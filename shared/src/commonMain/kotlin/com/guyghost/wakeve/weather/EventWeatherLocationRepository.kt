package com.guyghost.wakeve.weather

import com.guyghost.wakeve.database.WakeveDb
import com.guyghost.wakeve.models.Coordinates

class DatabaseEventWeatherLocationRepository(
    private val db: WakeveDb
) : EventWeatherLocationRepository {
    override fun getWeatherLocation(eventId: String): EventWeatherLocation? {
        val resolved = db.eventWeatherQueries
            .selectResolvedLocationByEvent(eventId)
            .executeAsOneOrNull()
            ?.let {
                EventWeatherLocation(
                    id = it.id,
                    eventId = it.eventId,
                    label = it.label,
                    address = it.address,
                    coordinates = Coordinates(it.latitude, it.longitude),
                    source = EventWeatherLocationSource.RESOLVED_MAP_LOCATION,
                    providerName = it.providerName,
                    providerPlaceId = it.providerPlaceId,
                    resolvedAt = it.resolvedAt
                )
            }

        if (resolved != null) {
            return resolved
        }

        return db.potentialLocationQueries
            .selectByEventId(eventId)
            .executeAsList()
            .firstNotNullOfOrNull { row ->
                row.coordinates
                    ?.let(Coordinates::fromJson)
                    ?.let { coordinates ->
                        EventWeatherLocation(
                            id = row.id,
                            eventId = row.eventId,
                            label = row.name,
                            address = row.address,
                            coordinates = coordinates,
                            source = EventWeatherLocationSource.POTENTIAL_LOCATION
                        )
                    }
            }
    }

    override fun saveResolvedLocation(location: EventWeatherLocation) {
        db.eventWeatherQueries.upsertResolvedLocation(
            id = location.id,
            eventId = location.eventId,
            sourceLocationId = if (location.source == EventWeatherLocationSource.POTENTIAL_LOCATION) location.id else null,
            label = location.label,
            address = location.address,
            latitude = location.coordinates.latitude,
            longitude = location.coordinates.longitude,
            providerName = location.providerName ?: "manual",
            providerPlaceId = location.providerPlaceId,
            resolvedAt = location.resolvedAt ?: ""
        )
    }
}
