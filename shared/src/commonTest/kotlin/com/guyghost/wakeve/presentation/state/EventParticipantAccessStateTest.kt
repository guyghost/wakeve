package com.guyghost.wakeve.presentation.state

import com.guyghost.wakeve.access.DateValidationState
import com.guyghost.wakeve.access.ParticipantAccessState
import com.guyghost.wakeve.access.ParticipantRsvp
import kotlin.test.Test
import kotlin.test.assertEquals

class EventParticipantAccessStateTest {

    @Test
    fun `event management state exposes participant access states for confirmed and pending attendees`() {
        val confirmed = ParticipantAccessState.member(
            userId = "confirmed-1",
            rsvp = ParticipantRsvp.ACCEPTED,
            dateValidation = DateValidationState.VALIDATED_RETAINED_DATE
        )
        val pending = ParticipantAccessState.invitedPending("pending-1")

        val state = EventManagementContract.State(
            participantIds = listOf("confirmed-1", "pending-1"),
            participantAccessStates = listOf(confirmed, pending)
        )

        assertEquals(listOf("confirmed-1", "pending-1"), state.participantIds)
        assertEquals(
            listOf("confirmed-1"),
            state.participantAccessStates
                .filter { it.rsvp == ParticipantRsvp.ACCEPTED }
                .filter { it.dateValidation == DateValidationState.VALIDATED_RETAINED_DATE }
                .map { it.userId }
        )
        assertEquals(
            listOf("pending-1"),
            state.participantAccessStates
                .filter { it.rsvp == ParticipantRsvp.PENDING }
                .map { it.userId }
        )
    }
}
