package com.guyghost.wakeve.presentation.participants

import com.guyghost.wakeve.access.DateValidationState
import com.guyghost.wakeve.access.ParticipantAccessState
import com.guyghost.wakeve.access.ParticipantRsvp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ParticipantManagementPresentationMapperTest {

    @Test
    fun `organizer row is confirmed and can access organization details`() {
        val rows = ParticipantManagementPresentationMapper.map(
            listOf(ParticipantAccessState.organizer("organizer@example.com"))
        )

        assertEquals(1, rows.size)
        val row = rows.single()
        assertEquals("organizer@example.com", row.userIdOrEmail)
        assertEquals("Organizer", row.roleLabel)
        assertEquals("Confirmed", row.statusLabel)
        assertTrue(row.canAccessOrganizationDetails)
    }

    @Test
    fun `accepted member who validated retained date is confirmed and can access organization details`() {
        val rows = ParticipantManagementPresentationMapper.map(
            listOf(
                ParticipantAccessState.member(
                    userId = "validated-member@example.com",
                    rsvp = ParticipantRsvp.ACCEPTED,
                    dateValidation = DateValidationState.VALIDATED_RETAINED_DATE
                )
            )
        )

        assertEquals(1, rows.size)
        val row = rows.single()
        assertEquals("validated-member@example.com", row.userIdOrEmail)
        assertEquals("Member", row.roleLabel)
        assertEquals("Confirmed", row.statusLabel)
        assertTrue(row.canAccessOrganizationDetails)
    }

    @Test
    fun `pending member row stays pending and cannot access organization details`() {
        val rows = ParticipantManagementPresentationMapper.map(
            listOf(ParticipantAccessState.invitedPending("pending-member@example.com"))
        )

        assertEquals(1, rows.size)
        val row = rows.single()
        assertEquals("pending-member@example.com", row.userIdOrEmail)
        assertEquals("Member", row.roleLabel)
        assertEquals("Pending", row.statusLabel)
        assertFalse(row.canAccessOrganizationDetails)
    }

    @Test
    fun `accepted member without retained date validation stays pending and cannot access organization details`() {
        val rows = ParticipantManagementPresentationMapper.map(
            listOf(
                ParticipantAccessState.member(
                    userId = "unvalidated-member@example.com",
                    rsvp = ParticipantRsvp.ACCEPTED,
                    dateValidation = DateValidationState.NOT_VALIDATED
                )
            )
        )

        assertEquals(1, rows.size)
        val row = rows.single()
        assertEquals("unvalidated-member@example.com", row.userIdOrEmail)
        assertEquals("Member", row.roleLabel)
        assertEquals("Pending", row.statusLabel)
        assertFalse(row.canAccessOrganizationDetails)
    }

    @Test
    fun `declined member row is declined and cannot access organization details`() {
        val rows = ParticipantManagementPresentationMapper.map(
            listOf(ParticipantAccessState.declined("declined-member@example.com"))
        )

        assertEquals(1, rows.size)
        val row = rows.single()
        assertEquals("declined-member@example.com", row.userIdOrEmail)
        assertEquals("Member", row.roleLabel)
        assertEquals("Declined", row.statusLabel)
        assertFalse(row.canAccessOrganizationDetails)
    }
}
