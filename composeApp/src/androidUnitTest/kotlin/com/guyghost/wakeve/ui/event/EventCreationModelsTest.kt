package com.guyghost.wakeve.ui.event

import com.guyghost.wakeve.models.Event
import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.models.EventType
import com.guyghost.wakeve.models.TimeSlot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class EventCreationModelsTest {
    @Test
    fun `null event maps to empty draft wizard state`() {
        val state = null.toDraftEventWizardUiState()

        assertEquals("", state.title)
        assertEquals("", state.description)
        assertEquals(EventType.OTHER, state.eventType)
        assertEquals(emptyList(), state.timeSlots)
        assertFalse(state.showLocationDialog)
        assertFalse(state.showTimeSlotInput)
    }

    @Test
    fun `event maps to immutable draft wizard state`() {
        val slot = TimeSlot(
            id = "slot-1",
            start = "2026-07-14T09:00:00Z",
            end = "2026-07-14T18:00:00Z",
            timezone = "Europe/Paris"
        )
        val event = Event(
            id = "event-1",
            title = "Retreat",
            description = "Plan team retreat",
            organizerId = "user-1",
            participants = listOf("user-1"),
            proposedSlots = listOf(slot),
            deadline = "2026-07-01T12:00:00Z",
            status = EventStatus.DRAFT,
            createdAt = "2026-06-01T08:00:00Z",
            updatedAt = "2026-06-01T08:00:00Z",
            eventType = EventType.TEAM_BUILDING,
            expectedParticipants = 12
        )

        val state = event.toDraftEventWizardUiState()

        assertEquals("Retreat", state.title)
        assertEquals("Plan team retreat", state.description)
        assertEquals(EventType.TEAM_BUILDING, state.eventType)
        assertEquals("12", state.participantCount)
        assertEquals(listOf(slot), state.timeSlots)
    }

    @Test
    fun `creation friction summary keeps simple event lightweight`() {
        val summary = DraftEventWizardUiState(
            eventType = EventType.PARTY,
            participantCount = "4"
        ).toCreationFrictionSummary()

        assertEquals("Friction de creation", summary.title)
        assertEquals("Parcours simple", summary.complexityLabel)
        assertEquals("Temps estime : 2 a 4 min", summary.estimatedTimeLabel)
        assertEquals("Actions : 6 a 8", summary.actionCountLabel)
        assertEquals("Friction : faible", summary.frictionLabel)
        assertEquals("Risque d'abandon : faible", summary.abandonmentRiskLabel)
        assertEquals("Note attendue : 8/10", summary.userScoreLabel)
        assertEquals("Gardez le flux court : titre, date, invites.", summary.nextStepLabel)
    }

    @Test
    fun `creation friction summary escalates intermediate event with many participants`() {
        val summary = DraftEventWizardUiState(
            eventType = EventType.BIRTHDAY,
            participantCount = "18"
        ).toCreationFrictionSummary()

        assertEquals("Parcours complexe", summary.complexityLabel)
        assertEquals("Temps estime : 8 a 12 min", summary.estimatedTimeLabel)
        assertEquals("Actions : 12 a 18", summary.actionCountLabel)
        assertEquals("Friction : elevee", summary.frictionLabel)
        assertEquals("Risque d'abandon : eleve", summary.abandonmentRiskLabel)
        assertEquals("Note attendue : 6/10", summary.userScoreLabel)
        assertEquals("Cadrez transport, budget et programme des la creation.", summary.nextStepLabel)
    }

    @Test
    fun `creation friction summary treats weddings as very complex`() {
        val summary = DraftEventWizardUiState(
            eventType = EventType.WEDDING,
            participantCount = "12"
        ).toCreationFrictionSummary()

        assertEquals("Parcours tres complexe", summary.complexityLabel)
        assertEquals("Temps estime : 12 a 20 min", summary.estimatedTimeLabel)
        assertEquals("Actions : 18+", summary.actionCountLabel)
        assertEquals("Friction : tres elevee", summary.frictionLabel)
        assertEquals("Risque d'abandon : critique sans guidage", summary.abandonmentRiskLabel)
        assertEquals("Note attendue : 5/10", summary.userScoreLabel)
        assertEquals(
            "Decoupez en decisions : dates, destination, budget, logement, roles.",
            summary.nextStepLabel
        )
    }
}
