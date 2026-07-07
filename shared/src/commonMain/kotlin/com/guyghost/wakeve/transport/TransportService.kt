package com.guyghost.wakeve.transport

import com.guyghost.wakeve.models.OptimizationType
import com.guyghost.wakeve.models.Route
import com.guyghost.wakeve.models.TransportLocation
import com.guyghost.wakeve.models.TransportMode
import com.guyghost.wakeve.models.TransportOption
import com.guyghost.wakeve.models.TransportPlan
import kotlinx.datetime.Instant
import kotlin.math.abs

/**
 * Production transport service used when no real transport provider has been configured.
 *
 * It deliberately does not fabricate carrier names, prices, durations, or booking URLs.
 * Tests and future integrations should inject an explicit provider into TransportRepository.
 */
class TransportService : com.guyghost.wakeve.models.TransportService {

    override suspend fun getTransportOptions(
        from: TransportLocation,
        to: TransportLocation,
        departureTime: String,
        mode: TransportMode?
    ): List<TransportOption> {
        return emptyList()
    }

    override suspend fun optimizeRoutes(
        participants: Map<String, TransportLocation>,
        destination: TransportLocation,
        eventTime: String,
        optimizationType: OptimizationType
    ): TransportPlan {
        error("Transport option provider is not configured")
    }

    override suspend fun findGroupMeetingPoints(
        routes: Map<String, Route>,
        maxWaitTimeMinutes: Int
    ): List<String> {
        if (routes.isEmpty()) return emptyList()

        val arrivalTimes = routes.values
            .flatMap { route -> route.segments.map { segment -> segment.arrivalTime } }
            .sorted()

        val meetingPoints = mutableListOf<String>()
        var currentGroup = mutableListOf<String>()

        arrivalTimes.forEach { time ->
            if (currentGroup.isEmpty()) {
                currentGroup.add(time)
            } else {
                val lastTime = Instant.parse(currentGroup.last())
                val currentTime = Instant.parse(time)
                val diffMinutes = abs((currentTime - lastTime).inWholeMinutes)

                if (diffMinutes <= maxWaitTimeMinutes) {
                    currentGroup.add(time)
                } else {
                    meetingPoints.add(calculateGroupMeetingTime(currentGroup))
                    currentGroup = mutableListOf(time)
                }
            }
        }

        if (currentGroup.isNotEmpty()) {
            meetingPoints.add(calculateGroupMeetingTime(currentGroup))
        }

        return meetingPoints
    }

    private fun calculateGroupMeetingTime(times: List<String>): String {
        val instants = times.map { Instant.parse(it) }
        val averageEpoch = instants.sumOf { it.toEpochMilliseconds() } / instants.size
        return Instant.fromEpochMilliseconds(averageEpoch).toString()
    }
}

object NoConfiguredTransportOptionProvider {
    suspend fun optionsFor(
        participantId: String,
        departure: TransportLocation,
        destination: TransportLocation,
        eventTime: String
    ): List<TransportOption> {
        error("Transport option provider is not configured")
    }
}
