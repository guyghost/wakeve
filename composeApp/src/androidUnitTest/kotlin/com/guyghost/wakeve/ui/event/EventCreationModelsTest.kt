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
}
