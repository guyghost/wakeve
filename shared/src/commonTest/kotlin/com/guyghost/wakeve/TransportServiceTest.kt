package com.guyghost.wakeve

import com.guyghost.wakeve.models.Location
import com.guyghost.wakeve.models.OptimizationType
import com.guyghost.wakeve.models.Route
import com.guyghost.wakeve.models.TransportMode
import com.guyghost.wakeve.models.TransportOption
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TransportServiceTest {

    private val service = DefaultTransportService()

    private val paris = Location("Paris", "Paris, France", 48.8566, 2.3522, "CDG")
    private val london = Location("London", "London, UK", 51.5074, -0.1278, "LHR")
    private val berlin = Location("Berlin", "Berlin, Germany", 52.5200, 13.4050, "BER")

    @Test
    fun getTransportOptionsReturnsValidOptions() {
        val options = service.getTransportOptions(paris, london, "2025-12-01T10:00:00Z")

        assertTrue(options.isNotEmpty())
        options.forEach { option ->
            assertEquals(paris, option.departure)
            assertEquals(london, option.arrival)
            assertTrue(option.durationMinutes > 0)
            assertTrue(option.cost >= 0.0)
        }
    }

    @Test
    fun optimizeRoutesCreatesValidPlan() {
        val participants = mapOf(
            "p1" to paris,
            "p2" to london,
            "p3" to berlin
        )

        val plan = service.optimizeRoutes(participants, paris, "2025-12-01T14:00:00Z")

        assertEquals("event-test", plan.eventId) // Mock eventId
        assertEquals(participants.size, plan.participantRoutes.size)
        assertTrue(plan.totalGroupCost >= 0.0)
        assertTrue(plan.groupArrivals.isNotEmpty())
    }

    @Test
    fun optimizeRoutesWithCostMinimization() {
        val participants = mapOf("p1" to london)

        val costPlan = service.optimizeRoutes(participants, paris, "2025-12-01T14:00:00Z", OptimizationType.COST_MINIMIZE)
        val timePlan = service.optimizeRoutes(participants, paris, "2025-12-01T14:00:00Z", OptimizationType.TIME_MINIMIZE)

        // Cost plan should have lower total cost than time plan (in mock data)
        assertTrue(costPlan.totalGroupCost <= timePlan.totalGroupCost)
    }

    @Test
    fun findGroupMeetingPointsReturnsValidTimes() {
        val routes = mapOf(
            "p1" to Route("r1", listOf(
                TransportOption("opt1", TransportMode.FLIGHT, "Airline", paris, london, "2025-12-01T10:00:00Z", "2025-12-01T12:00:00Z", 120, 100.0)
            ), 120, 100.0, "EUR", 0.8),
            "p2" to Route("r2", listOf(
                TransportOption("opt2", TransportMode.TRAIN, "Rail", london, paris, "2025-12-01T11:00:00Z", "2025-12-01T13:00:00Z", 120, 50.0)
            ), 120, 50.0, "EUR", 0.9)
        )

        val meetingPoints = service.findGroupMeetingPoints(routes, 30)

        assertTrue(meetingPoints.isNotEmpty())
        meetingPoints.forEach { time ->
            assertTrue(time.matches(Regex("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z")))
        }
    }
}