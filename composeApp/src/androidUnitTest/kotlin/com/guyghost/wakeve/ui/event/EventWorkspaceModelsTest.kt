package com.guyghost.wakeve.ui.event

import com.guyghost.wakeve.models.Event
import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.models.TimeSlot
import com.guyghost.wakeve.presentation.state.EventManagementContract
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class EventWorkspaceModelsTest {
    @Test
    fun `maps event state to filtered immutable workspace state`() {
        val state = EventManagementContract.State(
            events = listOf(
                event(id = "draft", title = "Draft dinner", status = EventStatus.DRAFT, organizerId = "me"),
                event(id = "final", title = "Finished trip", status = EventStatus.FINALIZED, organizerId = "friend")
            )
        )

        val uiState = state.toEventWorkspaceUiState(
            currentUserId = "me",
            selectedFilter = EventListFilter.Upcoming,
            searchQuery = "dinner",
            selectedEventId = null
        )

        assertFalse(uiState.isLoading)
        assertEquals(null, uiState.error)
        assertEquals(listOf("draft"), uiState.events.map { it.id })
        assertNull(uiState.selectedEvent)
        assertEquals(true, uiState.events.single().isOrganizer)
    }

    @Test
    fun `uses selected event id for expanded list detail state`() {
        val state = EventManagementContract.State(
            events = listOf(
                event(id = "first", title = "First event", status = EventStatus.POLLING),
                event(id = "second", title = "Second event", status = EventStatus.CONFIRMED)
            )
        )

        val uiState = state.toEventWorkspaceUiState(
            currentUserId = "me",
            selectedFilter = EventListFilter.Upcoming,
            searchQuery = "",
            selectedEventId = "second"
        )

        assertEquals("second", uiState.selectedEvent?.id)
    }

    private fun event(
        id: String,
        title: String,
        status: EventStatus,
        organizerId: String = "me"
    ): Event =
        Event(
            id = id,
            title = title,
            description = "Description",
            organizerId = organizerId,
            participants = listOf("me"),
            proposedSlots = listOf(
                TimeSlot(
                    id = "$id-slot",
                    start = "2026-07-14T09:00:00Z",
                    end = "2026-07-14T18:00:00Z",
                    timezone = "Europe/Paris"
                )
            ),
            deadline = "2026-07-01T12:00:00Z",
            status = status,
            createdAt = "2026-06-01T08:00:00Z",
            updatedAt = "2026-06-01T08:00:00Z"
        )
}
