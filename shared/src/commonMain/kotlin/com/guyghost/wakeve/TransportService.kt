package com.guyghost.wakeve

import com.guyghost.wakeve.models.Location
import com.guyghost.wakeve.models.OptimizationType
import com.guyghost.wakeve.models.Route
import com.guyghost.wakeve.models.TransportMode
import com.guyghost.wakeve.models.TransportOption
import com.guyghost.wakeve.models.TransportPlan
import com.guyghost.wakeve.models.TransportService

class DefaultTransportService : TransportService {

    override suspend fun getTransportOptions(
        from: Location,
        to: Location,
        departureTime: String,
        mode: TransportMode?
    ): List<TransportOption> {
        // Mock transport options
        return when (mode) {
            TransportMode.FLIGHT -> listOf(
                TransportOption(
                    id = "flight-1",
                    mode = TransportMode.FLIGHT,
                    provider = "Air France",
                    departure = from,
                    arrival = to,
                    departureTime = departureTime,
                    arrivalTime = addHours(departureTime, 2),
                    durationMinutes = 120,
                    cost = 150.0
                ),
                TransportOption(
                    id = "flight-2",
                    mode = TransportMode.FLIGHT,
                    provider = "Ryanair",
                    departure = from,
                    arrival = to,
                    departureTime = addHours(departureTime, 1),
                    arrivalTime = addHours(departureTime, 3),
                    durationMinutes = 120,
                    cost = 80.0
                )
            )
            TransportMode.TRAIN -> listOf(
                TransportOption(
                    id = "train-1",
                    mode = TransportMode.TRAIN,
                    provider = "SNCF",
                    departure = from,
                    arrival = to,
                    departureTime = departureTime,
                    arrivalTime = addHours(departureTime, 3),
                    durationMinutes = 180,
                    cost = 60.0
                )
            )
            else -> listOf(
                TransportOption(
                    id = "car-1",
                    mode = TransportMode.CAR,
                    provider = "Rental",
                    departure = from,
                    arrival = to,
                    departureTime = departureTime,
                    arrivalTime = addHours(departureTime, 4),
                    durationMinutes = 240,
                    cost = 40.0
                )
            )
        }
    }

    override suspend fun optimizeRoutes(
        participants: Map<String, Location>,
        destination: Location,
        eventTime: String,
        optimizationType: OptimizationType
    ): TransportPlan {
        val routes = mutableMapOf<String, Route>()

        participants.forEach { (participantId, homeLocation) ->
            val options = getTransportOptions(homeLocation, destination, eventTime)
            val bestOption = when (optimizationType) {
                OptimizationType.COST_MINIMIZE -> options.minByOrNull { it.cost }
                OptimizationType.TIME_MINIMIZE -> options.minByOrNull { it.durationMinutes }
                OptimizationType.BALANCED -> options.minByOrNull { it.cost * 0.6 + it.durationMinutes * 0.4 }
            } ?: options.first()

            val route = Route(
                id = "route-$participantId",
                segments = listOf(bestOption),
                totalDurationMinutes = bestOption.durationMinutes,
                totalCost = bestOption.cost,
                score = calculateRouteScore(bestOption, optimizationType)
            )
            routes[participantId] = route
        }

        val totalCost = routes.values.sumOf { it.totalCost }
        val groupArrivals = findGroupMeetingPoints(routes, 60)

        return TransportPlan(
            eventId = "event-test",
            participantRoutes = routes,
            groupArrivals = groupArrivals,
            totalGroupCost = totalCost,
            optimizationType = optimizationType,
            createdAt = "2025-11-20T10:00:00Z"
        )
    }

    override suspend fun findGroupMeetingPoints(
        routes: Map<String, Route>,
        maxWaitTimeMinutes: Int
    ): List<String> {
        val arrivalTimes = routes.values.map { route ->
            route.segments.last().arrivalTime
        }.sorted()

        if (arrivalTimes.isEmpty()) return emptyList()

        val meetingPoints = mutableListOf<String>()
        var currentGroup = mutableListOf<String>()

        for (time in arrivalTimes) {
            if (currentGroup.isEmpty() || timeDiffMinutes(currentGroup.last(), time) <= maxWaitTimeMinutes) {
                currentGroup.add(time)
            } else {
                if (currentGroup.size > 1) {
                    meetingPoints.add(currentGroup.last())
                }
                currentGroup = mutableListOf(time)
            }
        }

        if (currentGroup.size > 1) {
            meetingPoints.add(currentGroup.last())
        }

        return meetingPoints
    }

    private fun calculateRouteScore(option: TransportOption, type: OptimizationType): Double {
        return when (type) {
            OptimizationType.COST_MINIMIZE -> 1.0 / (option.cost + 1.0)
            OptimizationType.TIME_MINIMIZE -> 1.0 / (option.durationMinutes + 1.0)
            OptimizationType.BALANCED -> 1.0 / (option.cost * 0.5 + option.durationMinutes * 0.5 + 1.0)
        }
    }

    private fun addHours(time: String, hours: Int): String {
        val hour = time.substring(11, 13).toInt()
        val newHour = (hour + hours) % 24
        val dayOffset = (hour + hours) / 24
        val day = time.substring(8, 10).toInt() + dayOffset
        val dayStr = if (day < 10) "0$day" else day.toString()
        return time.substring(0, 8) + dayStr + "T" + (if (newHour < 10) "0$newHour" else newHour.toString()) + time.substring(13)
    }

    private fun timeDiffMinutes(time1: String, time2: String): Int {
        // Simple mock diff
        val hour1 = time1.substring(11, 13).toInt()
        val hour2 = time2.substring(11, 13).toInt()
        return (hour2 - hour1) * 60
    }
}