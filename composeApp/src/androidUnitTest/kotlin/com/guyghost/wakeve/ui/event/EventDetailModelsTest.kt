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
    fun eventDetailStatusLabelsAreUserFacingFrenchCopy() {
        assertEquals("Brouillon", eventDetailStatusLabel(EventStatus.DRAFT))
        assertEquals("Sondage", eventDetailStatusLabel(EventStatus.POLLING))
        assertEquals("Comparaison", eventDetailStatusLabel(EventStatus.COMPARING))
        assertEquals("Date confirmee", eventDetailStatusLabel(EventStatus.CONFIRMED))
        assertEquals("Organisation", eventDetailStatusLabel(EventStatus.ORGANIZING))
        assertEquals("Finalise", eventDetailStatusLabel(EventStatus.FINALIZED))
    }

    @Test
    fun eventDetailNextStepsAnswerWhatToDoNowForEveryStatus() {
        val statuses = EventStatus.entries

        assertEquals(statuses.size, statuses.map(::eventDetailNextStepTitle).toSet().size)
        assertEquals(statuses.size, statuses.map(::eventDetailNextStepBody).toSet().size)

        assertEquals("Terminer la creation", eventDetailNextStepTitle(EventStatus.DRAFT))
        assertEquals("Obtenir les votes", eventDetailNextStepTitle(EventStatus.POLLING))
        assertEquals("Choisir la meilleure option", eventDetailNextStepTitle(EventStatus.COMPARING))
        assertEquals("Inviter et preparer", eventDetailNextStepTitle(EventStatus.CONFIRMED))
        assertEquals("Piloter l'evenement", eventDetailNextStepTitle(EventStatus.ORGANIZING))
        assertEquals("Consulter le recapitulatif", eventDetailNextStepTitle(EventStatus.FINALIZED))

        assertTrue(eventDetailNextStepBody(EventStatus.POLLING).contains("participants"))
        assertTrue(eventDetailNextStepBody(EventStatus.CONFIRMED).contains("invitation"))
        assertTrue(eventDetailNextStepBody(EventStatus.ORGANIZING).contains("centre de controle"))
        assertTrue(eventDetailNextStepBody(EventStatus.FINALIZED).contains("verrouille"))
    }

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

    @Test
    fun dayOfSummaryExplainsExpectedPendingAndDeclinedParticipants() {
        val uiState = EventManagementContract.State(
            selectedEvent = event(
                status = EventStatus.ORGANIZING,
                participants = listOf("organizer", "participant-1", "participant-2", "participant-3")
            ),
            participantAccessStates = listOf(
                ParticipantAccessState.organizer("organizer"),
                ParticipantAccessState.member(
                    userId = "participant-1",
                    rsvp = ParticipantRsvp.ACCEPTED,
                    dateValidation = DateValidationState.VALIDATED_RETAINED_DATE
                ),
                ParticipantAccessState.invitedPending("participant-2"),
                ParticipantAccessState.declined("participant-3")
            )
        ).toEventDetailUiState(eventId = eventId, currentUserId = "organizer")

        assertEquals("Jour J", uiState.dayOfSummary?.title)
        assertEquals("2 participants attendus", uiState.dayOfSummary?.attendanceLabel)
        assertEquals("1 sans reponse · 1 ne vient pas", uiState.dayOfSummary?.missingLabel)
        assertEquals(
            "Suivre les arrivees et traiter les absents avant la prochaine etape.",
            uiState.dayOfSummary?.nextActionLabel
        )
    }

    @Test
    fun dayOfSummaryUsesFallbackWhenRsvpDetailsAreNotLoaded() {
        val uiState = EventManagementContract.State(
            selectedEvent = event(status = EventStatus.CONFIRMED)
        ).toEventDetailUiState(eventId = eventId, currentUserId = "organizer")

        assertEquals("Participants invites : 2", uiState.dayOfSummary?.attendanceLabel)
        assertEquals("RSVP detailles non synchronises", uiState.dayOfSummary?.missingLabel)
        assertEquals(
            "Verifier le lieu de rendez-vous et envoyer le rappel de depart.",
            uiState.dayOfSummary?.nextActionLabel
        )
    }

    @Test
    fun dayOfSummaryIsHiddenBeforeDateIsConfirmed() {
        val uiState = EventManagementContract.State(
            selectedEvent = event(status = EventStatus.POLLING),
            participantAccessStates = listOf(ParticipantAccessState.organizer("organizer"))
        ).toEventDetailUiState(eventId = eventId, currentUserId = "organizer")

        assertEquals(null, uiState.dayOfSummary)
    }

    private fun event(
        id: String = eventId,
        status: EventStatus,
        participants: List<String> = listOf("organizer", "participant-1")
    ): Event =
        Event(
            id = id,
            title = "Detail event",
            description = "Detail description",
            organizerId = "organizer",
            participants = participants,
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
