package com.guyghost.wakeve.scenario

import com.guyghost.wakeve.models.LocationType
import com.guyghost.wakeve.models.PotentialLocation
import com.guyghost.wakeve.models.ScenarioGenerationType
import com.guyghost.wakeve.models.TimeOfDay
import com.guyghost.wakeve.models.TimeSlot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ScenarioMatrixGenerationServiceTest {
    @Test
    fun generateDraftScenariosCreatesCartesianProduct() {
        val scenarios = ScenarioMatrixGenerationService.generateDraftScenarios(
            eventId = "event-1",
            timeSlots = listOf(slot("slot-1"), slot("slot-2"), slot("slot-3")),
            potentialLocations = listOf(location("loc-1", "Paris"), location("loc-2", "Lyon")),
            estimatedParticipants = 8,
            now = "2026-06-07T10:00:00Z"
        )

        assertEquals(6, scenarios.size)
        assertTrue(scenarios.all { it.generationType == ScenarioGenerationType.MATRIX })
        assertEquals(6, scenarios.map { it.id }.toSet().size)
    }

    @Test
    fun generateDraftScenariosSkipsExistingMatrixPairs() {
        val first = ScenarioMatrixGenerationService.generateDraftScenarios(
            eventId = "event-1",
            timeSlots = listOf(slot("slot-1")),
            potentialLocations = listOf(location("loc-1", "Paris")),
            estimatedParticipants = 8,
            now = "2026-06-07T10:00:00Z"
        )

        val second = ScenarioMatrixGenerationService.generateDraftScenarios(
            eventId = "event-1",
            timeSlots = listOf(slot("slot-1")),
            potentialLocations = listOf(location("loc-1", "Paris")),
            existingScenarios = first,
            estimatedParticipants = 8,
            now = "2026-06-07T11:00:00Z"
        )

        assertEquals(0, second.size)
    }

    private fun slot(id: String): TimeSlot {
        return TimeSlot(
            id = id,
            start = "2026-07-01T10:00:00Z",
            end = "2026-07-01T12:00:00Z",
            timezone = "Europe/Paris",
            timeOfDay = TimeOfDay.SPECIFIC
        )
    }

    private fun location(id: String, name: String): PotentialLocation {
        return PotentialLocation(
            id = id,
            eventId = "event-1",
            name = name,
            locationType = LocationType.CITY,
            createdAt = "2026-06-07T10:00:00Z"
        )
    }
}
