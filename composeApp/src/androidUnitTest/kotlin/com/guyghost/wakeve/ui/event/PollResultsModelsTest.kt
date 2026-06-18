package com.guyghost.wakeve.ui.event

import com.guyghost.wakeve.models.Event
import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.models.Poll
import com.guyghost.wakeve.models.TimeSlot
import com.guyghost.wakeve.models.Vote
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PollResultsModelsTest {
    @Test
    fun mapsPollScoresAndRecommendedSlot() {
        val event = testEvent()
        val poll = Poll(
            id = "poll-1",
            eventId = event.id,
            votes = mapOf(
                "alice" to mapOf("slot-1" to Vote.YES, "slot-2" to Vote.NO),
                "bea" to mapOf("slot-1" to Vote.MAYBE, "slot-2" to Vote.YES),
                "cam" to mapOf("slot-1" to Vote.YES, "slot-2" to Vote.NO)
            )
        )

        val uiState = event.toPollResultsUiState(
            poll = poll,
            isOrganizer = true,
            selectedSlotId = "slot-1"
        )

        assertEquals("slot-1", uiState.recommendedSlot?.slotId)
        assertEquals(2, uiState.recommendedSlot?.yesCount)
        assertEquals(1, uiState.recommendedSlot?.maybeCount)
        assertEquals(5, uiState.recommendedSlot?.totalScore)
        assertTrue(uiState.slots.first { it.slotId == "slot-1" }.isSelected)
        assertTrue(uiState.canConfirm)
    }

    @Test
    fun nonOrganizerCannotConfirmEvenWithSelectedSlot() {
        val uiState = testEvent().toPollResultsUiState(
            poll = Poll(id = "poll-1", eventId = eventId, votes = emptyMap()),
            isOrganizer = false,
            selectedSlotId = "slot-1"
        )

        assertFalse(uiState.canConfirm)
    }

    private fun testEvent(): Event =
        Event(
            id = eventId,
            title = "Poll event",
            description = "Poll description",
            organizerId = "organizer",
            participants = listOf("alice", "bea", "cam"),
            proposedSlots = listOf(
                TimeSlot(
                    id = "slot-1",
                    start = "2026-07-14T09:00:00Z",
                    end = "2026-07-14T18:00:00Z",
                    timezone = "Europe/Paris"
                ),
                TimeSlot(
                    id = "slot-2",
                    start = "2026-07-15T09:00:00Z",
                    end = "2026-07-15T18:00:00Z",
                    timezone = "Europe/Paris"
                )
            ),
            deadline = "2026-07-01T12:00:00Z",
            status = EventStatus.POLLING,
            createdAt = "2026-06-01T08:00:00Z",
            updatedAt = "2026-06-01T08:00:00Z"
        )

    private companion object {
        const val eventId = "event-1"
    }
}
