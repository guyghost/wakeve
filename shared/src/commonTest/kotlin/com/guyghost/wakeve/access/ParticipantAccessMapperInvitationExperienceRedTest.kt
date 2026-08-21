package com.guyghost.wakeve.access

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ParticipantAccessMapperInvitationExperienceRedTest {
    @Test
    fun `shared access vocabulary represents unavailable and not-applicable explicitly`() {
        val rsvpCases = ParticipantRsvp.entries.map { it.name }.toSet()
        val dateCases = DateValidationState.entries.map { it.name }.toSet()

        assertTrue("UNAVAILABLE" in rsvpCases)
        assertTrue("NOT_APPLICABLE" in rsvpCases)
        assertTrue("UNAVAILABLE" in dateCases)
        assertTrue("NOT_APPLICABLE" in dateCases)
    }

    @Test
    fun `non-member axes are not inferred as pending or not-validated`() {
        val mapped = ParticipantAccessMapper.fromRepositoryRecord(
            ParticipantRepositoryRecord(
                id = "invite-only",
                eventId = "event-1",
                userId = "viewer-1",
                role = "NON_MEMBER",
                rsvp = "ACCEPTED",
                hasValidatedDate = 1
            )
        )

        assertEquals(ParticipantAccessState.Role.NON_MEMBER, mapped.role)
        assertEquals("NOT_APPLICABLE", mapped.rsvp.name)
        assertEquals("NOT_APPLICABLE", mapped.dateValidation.name)
    }

    @Test
    fun `active member missing applicable records maps unavailable instead of permissive defaults`() {
        val mapped = ParticipantAccessMapper.fromRepositoryRecord(
            ParticipantRepositoryRecord(
                id = "member-1",
                eventId = "event-1",
                userId = "viewer-1",
                role = "MEMBER",
                rsvp = "",
                hasValidatedDate = -1
            )
        )

        assertEquals(ParticipantAccessState.Role.MEMBER, mapped.role)
        assertEquals("UNAVAILABLE", mapped.rsvp.name)
        assertEquals("UNAVAILABLE", mapped.dateValidation.name)
    }
}
