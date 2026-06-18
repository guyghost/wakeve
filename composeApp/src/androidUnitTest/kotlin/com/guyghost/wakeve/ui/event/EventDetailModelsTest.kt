package com.guyghost.wakeve.ui.event

import com.guyghost.wakeve.access.DateValidationState
import com.guyghost.wakeve.access.ParticipantAccessState
import com.guyghost.wakeve.access.ParticipantRsvp
import com.guyghost.wakeve.models.Event
import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.models.Vote
import com.guyghost.wakeve.presentation.state.EventManagementContract
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EventDetailModelsTest {
    @Test
    fun detailStateFallsBackToRouteEventWhenSelectedEventBelongsToAnotherRoute() {
        val routeEvent = event(id = "route-event", status = EventStatus.POLLING)
        val otherSelectedEvent = event(id = "other-event", status = EventStatus.CONFIRMED)

        val uiState = EventManagementContract.State(
            events = listOf(routeEvent, otherSelectedEvent),
            selectedEvent = otherSelectedEvent,
            participantIds = listOf("other-participant"),
            pollVotes = mapOf("route-event" to mapOf("alice" to Vote.YES))
        ).toEventDetailUiState(eventId = "route-event", currentUserId = "organizer")

        assertEquals("route-event", uiState.event?.id)
        assertEquals(routeEvent.participants, uiState.participants)
        assertEquals(mapOf("alice" to Vote.YES), uiState.pollVotes)
    }

    @Test
    fun organizerCanEditButCannotDeleteFinalizedEvent() {
        val uiState = EventManagementContract.State(
            selectedEvent = event(status = EventStatus.FINALIZED)
        ).toEventDetailUiState(eventId = eventId, currentUserId = "organizer")

        assertTrue(uiState.isOrganizer)
        assertFalse(uiState.canDelete)
        assertTrue(uiState.canAccessOrganizationDetails)
        assertTrue(uiState.showOrganizationTools)
    }

    @Test
    fun confirmedParticipantCanAccessTransportButNotOrganizationToolsBeforeOrganizing() {
        val uiState = EventManagementContract.State(
            selectedEvent = event(status = EventStatus.CONFIRMED),
            participantAccessStates = listOf(
                ParticipantAccessState.member(
                    userId = "participant-1",
                    rsvp = ParticipantRsvp.ACCEPTED,
                    dateValidation = DateValidationState.VALIDATED_RETAINED_DATE
                )
            )
        ).toEventDetailUiState(eventId = eventId, currentUserId = "participant-1")

        assertFalse(uiState.isOrganizer)
        assertFalse(uiState.canDelete)
        assertTrue(uiState.canAccessOrganizationDetails)
        assertTrue(uiState.showTransportPlanning)
        assertFalse(uiState.showOrganizationTools)
        assertEquals(ParticipantRsvp.ACCEPTED, uiState.rsvp?.selectedResponse)
        assertTrue(uiState.rsvp?.isEnabled == true)
        assertEquals("Participation confirmée", uiState.rsvp?.statusLabel)
    }

    @Test
    fun organizerRsvpStateIsLockedAccepted() {
        val uiState = EventManagementContract.State(
            selectedEvent = event(status = EventStatus.CONFIRMED),
            participantAccessStates = listOf(ParticipantAccessState.organizer("organizer"))
        ).toEventDetailUiState(eventId = eventId, currentUserId = "organizer")

        assertEquals(ParticipantRsvp.ACCEPTED, uiState.rsvp?.selectedResponse)
        assertTrue(uiState.rsvp?.isOrganizer == true)
        assertFalse(uiState.rsvp?.isEnabled ?: true)
    }

    private fun event(
        id: String = eventId,
        status: EventStatus
    ): Event =
        Event(
            id = id,
            title = "Detail event",
            description = "Detail description",
            organizerId = "organizer",
            participants = listOf("organizer", "participant-1"),
            proposedSlots = emptyList(),
            deadline = "2026-07-01T12:00:00Z",
            status = status,
            createdAt = "2026-06-01T08:00:00Z",
            updatedAt = "2026-06-01T08:00:00Z"
        )

    private companion object {
        const val eventId = "event-1"
    }
}
