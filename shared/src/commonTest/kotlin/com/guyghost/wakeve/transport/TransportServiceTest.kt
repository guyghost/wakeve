package com.guyghost.wakeve.transport

import com.guyghost.wakeve.models.OptimizationType
import com.guyghost.wakeve.models.Route
import com.guyghost.wakeve.models.TransportLocation
import com.guyghost.wakeve.models.TransportMode
import com.guyghost.wakeve.models.TransportOption
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TransportServiceTest {

    private val transportService = TransportService()

    @Test
    fun `getTransportOptions returns options for flight mode`() = runBlocking {
        // Given
        val from = TransportLocation("Paris", "Paris CDG Airport", 49.0, 2.5, "CDG")
        val to = TransportLocation("London", "London Heathrow", 51.5, -0.5, "LHR")
        val departureTime = "2025-12-25T10:00:00Z"

        // When
        val options = transportService.getTransportOptions(from, to, departureTime, TransportMode.FLIGHT)

        // Then
        assertTrue(options.isNotEmpty(), "Should return flight options")
        options.forEach { option ->
            assertEquals(TransportMode.FLIGHT, option.mode, "All options should be flights")
            assertEquals(from, option.departure, "Departure should match")
            assertEquals(to, option.arrival, "Arrival should match")
            assertTrue(option.cost > 0, "Cost should be positive")
            assertTrue(option.durationMinutes > 0, "Duration should be positive")
        }
    }

    @Test
    fun `getTransportOptions returns multiple modes when no mode specified`() = runBlocking {
        // Given
        val from = TransportLocation("Paris", null, null, null, null)
        val to = TransportLocation("London", null, null, null, null)
        val departureTime = "2025-12-25T10:00:00Z"

        // When
        val options = transportService.getTransportOptions(from, to, departureTime)

        // Then
        assertTrue(options.isNotEmpty(), "Should return transport options")
        val modes = options.map { it.mode }.distinct()
        assertTrue(modes.size > 1, "Should include multiple transport modes")
    }

    @Test
    fun `optimizeRoutes returns plan with cost minimization`() = runBlocking {
        // Given
        val participants = mapOf(
            "user1" to TransportLocation("Paris", null, null, null, null),
            "user2" to TransportLocation("Lyon", null, null, null, null)
        )
        val destination = TransportLocation("Barcelona", null, null, null, null)
        val eventTime = "2025-12-25T14:00:00Z"

        // When
        val plan = transportService.optimizeRoutes(
            participants = participants,
            destination = destination,
            eventTime = eventTime,
            optimizationType = OptimizationType.COST_MINIMIZE
        )

        // Then
        assertNotNull(plan, "Should return a transport plan")
        assertEquals(participants.size, plan.participantRoutes.size, "Should have routes for all participants")
        assertTrue(plan.totalGroupCost > 0, "Total cost should be positive")
        assertEquals(OptimizationType.COST_MINIMIZE, plan.optimizationType, "Should use cost minimization")
    }

    @Test
    fun `optimizeRoutes returns plan with time minimization`() = runBlocking {
        // Given
        val participants = mapOf(
            "user1" to TransportLocation("Paris", null, null, null, null)
        )
        val destination = TransportLocation("London", null, null, null, null)
        val eventTime = "2025-12-25T14:00:00Z"

        // When
        val plan = transportService.optimizeRoutes(
            participants = participants,
            destination = destination,
            eventTime = eventTime,
            optimizationType = OptimizationType.TIME_MINIMIZE
        )

        // Then
        assertNotNull(plan, "Should return a transport plan")
        assertEquals(OptimizationType.TIME_MINIMIZE, plan.optimizationType, "Should use time minimization")
    }

    @Test
    fun `optimizeRoutes returns plan with balanced optimization`() = runBlocking {
        // Given
        val participants = mapOf(
            "user1" to TransportLocation("Paris", null, null, null, null)
        )
        val destination = TransportLocation("Rome", null, null, null, null)
        val eventTime = "2025-12-25T14:00:00Z"

        // When
        val plan = transportService.optimizeRoutes(
            participants = participants,
            destination = destination,
            eventTime = eventTime,
            optimizationType = OptimizationType.BALANCED
        )

        // Then
        assertNotNull(plan, "Should return a transport plan")
        assertEquals(OptimizationType.BALANCED, plan.optimizationType, "Should use balanced optimization")
    }

    @Test
    fun `findGroupMeetingPoints groups close arrival times`() = runBlocking {
        // Given
        val route1 = Route(
            id = "route1",
            segments = listOf(
                TransportOption(
                    id = "opt1",
                    mode = TransportMode.TRAIN,
                    provider = "SNCF",
                    departure = TransportLocation("Paris", null, null, null, null),
                    arrival = TransportLocation("Barcelona", null, null, null, null),
                    departureTime = "2025-12-25T08:00:00Z",
                    arrivalTime = "2025-12-25T12:00:00Z", // Arrive at noon
                    durationMinutes = 240,
                    cost = 50.0
                )
            ),
            totalDurationMinutes = 240,
            totalCost = 50.0,
            currency = "EUR",
            score = 0.8
        )

        val route2 = Route(
            id = "route2",
            segments = listOf(
                TransportOption(
                    id = "opt2",
                    mode = TransportMode.FLIGHT,
                    provider = "Air France",
                    departure = TransportLocation("Lyon", null, null, null, null),
                    arrival = TransportLocation("Barcelona", null, null, null, null),
                    departureTime = "2025-12-25T09:00:00Z",
                    arrivalTime = "2025-12-25T12:30:00Z", // Arrive 30 min later
                    durationMinutes = 210,
                    cost = 120.0
                )
            ),
            totalDurationMinutes = 210,
            totalCost = 120.0,
            currency = "EUR",
            score = 0.7
        )

        val routes = mapOf("user1" to route1, "user2" to route2)

        // When
        val meetingPoints = transportService.findGroupMeetingPoints(routes, maxWaitTimeMinutes = 60)

        // Then
        assertTrue(meetingPoints.isNotEmpty(), "Should find meeting points")
        // The two arrivals are within 30 minutes, so they should be grouped into one meeting point
        assertEquals(1, meetingPoints.size, "Should group close arrivals into one meeting point")
    }

    @Test
    fun `findGroupMeetingPoints separates far arrival times`() = runBlocking {
        // Given
        val route1 = Route(
            id = "route1",
            segments = listOf(
                TransportOption(
                    id = "opt1",
                    mode = TransportMode.TRAIN,
                    provider = "SNCF",
                    departure = TransportLocation("Paris", null, null, null, null),
                    arrival = TransportLocation("Barcelona", null, null, null, null),
                    departureTime = "2025-12-25T08:00:00Z",
                    arrivalTime = "2025-12-25T12:00:00Z",
                    durationMinutes = 240,
                    cost = 50.0
                )
            ),
            totalDurationMinutes = 240,
            totalCost = 50.0,
            currency = "EUR",
            score = 0.8
        )

        val route2 = Route(
            id = "route2",
            segments = listOf(
                TransportOption(
                    id = "opt2",
                    mode = TransportMode.BUS,
                    provider = "FlixBus",
                    departure = TransportLocation("Marseille", null, null, null, null),
                    arrival = TransportLocation("Barcelona", null, null, null, null),
                    departureTime = "2025-12-25T06:00:00Z",
                    arrivalTime = "2025-12-25T14:00:00Z", // Arrive 2 hours later
                    durationMinutes = 480,
                    cost = 30.0
                )
            ),
            totalDurationMinutes = 480,
            totalCost = 30.0,
            currency = "EUR",
            score = 0.6
        )

        val routes = mapOf("user1" to route1, "user2" to route2)

        // When
        val meetingPoints = transportService.findGroupMeetingPoints(routes, maxWaitTimeMinutes = 60)

        // Then
        assertEquals(2, meetingPoints.size, "Should separate far arrivals into different meeting points")
    }

    @Test
    fun `walking options only generated for same location`() = runBlocking {
        // Given
        val sameLocation = TransportLocation("Barcelona", "City Center", 41.4, 2.2, null)
        val departureTime = "2025-12-25T10:00:00Z"

        // When
        val options = transportService.getTransportOptions(sameLocation, sameLocation, departureTime, TransportMode.WALKING)

        // Then
        assertTrue(options.isNotEmpty(), "Should return walking option for same location")
        assertEquals(TransportMode.WALKING, options.first().mode)
        assertEquals(0.0, options.first().cost, "Walking should be free")
    }

    @Test
    fun `options are sorted by cost ascending`() = runBlocking {
        // Given
        val from = TransportLocation("Paris", null, null, null, null)
        val to = TransportLocation("London", null, null, null, null)
        val departureTime = "2025-12-25T10:00:00Z"

        // When
        val options = transportService.getTransportOptions(from, to, departureTime)

        // Then
        val costs = options.map { it.cost }
        val sortedCosts = costs.sorted()
        assertEquals(sortedCosts, costs, "Options should be sorted by cost ascending")
    }
}