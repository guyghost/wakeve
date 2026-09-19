package com.guyghost.wakeve.transport

import com.guyghost.wakeve.models.TransportLocation
import com.guyghost.wakeve.models.TransportMode
import com.guyghost.wakeve.models.TransportOption
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Deterministic local fallback provider (proposal #46): derives plausible
 * multi-mode options (flight / train / car) from the departure/destination
 * coordinates so the transport loop — generate -> compare -> select ->
 * readiness COMPLETE — works without an external provider.
 *
 * Used only when no external provider is injected into TransportRepository.
 */
object LocalTransportOptionProvider {

    suspend fun optionsFor(
        participantId: String,
        departure: TransportLocation,
        destination: TransportLocation,
        eventTime: String
    ): List<TransportOption> {
        val distanceKm = estimateDistanceKm(departure, destination)
        val modes = buildList {
            add(Triple(TransportMode.FLIGHT, 850.0, 750.0))      // km/h, EUR base
            if (distanceKm <= 1500.0) add(Triple(TransportMode.TRAIN, 220.0, 90.0))
            if (distanceKm <= 900.0) add(Triple(TransportMode.CAR, 95.0, 120.0))
        }
        return modes.mapIndexed { index, (mode, speedKmh, baseCostEur) ->
            val durationMinutes = ((distanceKm / speedKmh) * 60).roundToInt() + 60 // +1h marge
            val cost = (baseCostEur + distanceKm * costPerKm(mode)).roundToInt().toDouble()
            TransportOption(
                id = "local_${mode.name.lowercase()}_${participantId.hashCode()}_${distanceKm.roundToInt()}",
                mode = mode,
                provider = "Wakeve Local",
                departure = departure,
                arrival = destination,
                departureTime = eventTime,
                arrivalTime = eventTime, // refined by the caller if needed
                durationMinutes = durationMinutes,
                cost = cost,
                currency = "EUR"
            )
        }
    }

    private fun costPerKm(mode: TransportMode): Double = when (mode) {
        TransportMode.FLIGHT -> 0.18
        TransportMode.TRAIN -> 0.12
        TransportMode.CAR -> 0.22
        else -> 0.15
    }

    /** Haversine when coordinates exist; deterministic pseudo-distance otherwise. */
    private fun estimateDistanceKm(from: TransportLocation, to: TransportLocation): Double {
        val lat1 = from.latitude
        val lon1 = from.longitude
        val lat2 = to.latitude
        val lon2 = to.longitude
        if (lat1 != null && lon1 != null && lat2 != null && lon2 != null) {
            val r = 6371.0
            val dLat = Math.toRadians(lat2 - lat1)
            val dLon = Math.toRadians(lon2 - lon1)
            val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
            return 2 * r * atan2(sqrt(a), sqrt(1 - a)).coerceAtLeast(50.0)
        }
        // Deterministic fallback: stable pseudo-distance from the location names
        val seed = (from.name + to.name).hashCode().toUInt().toInt()
        return 200.0 + (seed % 5000).toDouble().let { if (it < 0) -it else it } / 10.0
    }
}
