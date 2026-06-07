package com.guyghost.wakeve.scenario

import com.guyghost.wakeve.models.PotentialLocation
import com.guyghost.wakeve.models.Scenario
import com.guyghost.wakeve.models.ScenarioGenerationType
import com.guyghost.wakeve.models.ScenarioStatus
import com.guyghost.wakeve.models.TimeSlot

/**
 * Pure generator for date-and-destination scenario matrices.
 */
object ScenarioMatrixGenerationService {
    fun generateDraftScenarios(
        eventId: String,
        timeSlots: List<TimeSlot>,
        potentialLocations: List<PotentialLocation>,
        existingScenarios: List<Scenario> = emptyList(),
        estimatedParticipants: Int,
        estimatedBudgetPerPerson: Double = 0.0,
        now: String
    ): List<Scenario> {
        require(eventId.isNotBlank()) { "Event ID is required" }
        require(timeSlots.isNotEmpty()) { "At least one time slot is required" }
        require(potentialLocations.isNotEmpty()) { "At least one destination is required" }
        require(estimatedParticipants > 0) { "Estimated participants must be positive" }
        require(estimatedBudgetPerPerson >= 0.0) { "Budget cannot be negative" }

        val existingKeys = existingScenarios
            .filter { it.generationType == ScenarioGenerationType.MATRIX }
            .mapNotNull { scenario ->
                val slotId = scenario.sourceTimeSlotId ?: return@mapNotNull null
                val locationId = scenario.sourcePotentialLocationId ?: return@mapNotNull null
                MatrixKey(slotId, locationId)
            }
            .toSet()

        return timeSlots
            .sortedBy { it.id }
            .flatMap { slot ->
                potentialLocations.sortedBy { it.id }.mapNotNull { location ->
                    val key = MatrixKey(slot.id, location.id)
                    if (key in existingKeys) {
                        null
                    } else {
                        Scenario(
                            id = deterministicScenarioId(eventId, slot.id, location.id),
                            eventId = eventId,
                            name = "${formatSlotLabel(slot)} - ${location.name}",
                            dateOrPeriod = formatSlotLabel(slot),
                            location = location.name,
                            duration = 1,
                            estimatedParticipants = estimatedParticipants,
                            estimatedBudgetPerPerson = estimatedBudgetPerPerson,
                            description = "Generated from ${formatSlotLabel(slot)} and ${location.name}",
                            status = ScenarioStatus.DRAFT,
                            createdAt = now,
                            updatedAt = now,
                            sourceTimeSlotId = slot.id,
                            sourcePotentialLocationId = location.id,
                            generationType = ScenarioGenerationType.MATRIX
                        )
                    }
                }
            }
    }

    fun deterministicScenarioId(eventId: String, timeSlotId: String, potentialLocationId: String): String {
        return "scenario_matrix_${sanitize(eventId)}_${sanitize(timeSlotId)}_${sanitize(potentialLocationId)}"
    }

    private fun formatSlotLabel(slot: TimeSlot): String {
        val start = slot.start ?: "Flexible date"
        val end = slot.end
        val period = if (end == null || end == start) start else "$start/$end"
        return if (slot.timeOfDay.name == "SPECIFIC") {
            period
        } else {
            "$period ${slot.timeOfDay.displayName}"
        }
    }

    private fun sanitize(value: String): String {
        return value.map { char ->
            when {
                char.isLetterOrDigit() -> char.lowercaseChar()
                else -> '_'
            }
        }.joinToString("").trim('_').ifBlank { "unknown" }
    }

    private data class MatrixKey(
        val timeSlotId: String,
        val potentialLocationId: String
    )
}
