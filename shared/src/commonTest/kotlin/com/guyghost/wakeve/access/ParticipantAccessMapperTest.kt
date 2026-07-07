package com.guyghost.wakeve.access

import com.guyghost.wakeve.models.Event
import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.models.TimeOfDay
import com.guyghost.wakeve.models.TimeSlot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ParticipantAccessMapperTest {

    private val retainedSlot = TimeSlot(
        id = "slot-retained",
        start = "2026-09-12T09:00:00Z",
        end = "2026-09-12T18:00:00Z",
        timezone = "Europe/Paris",
        timeOfDay = TimeOfDay.SPECIFIC
    )

    private val confirmedEvent = Event(
        id = "event-participant-access-1",
        title = "Confirmed organization",
        description = "Protected organization details",
        organizerId = "organizer-1",
        participants = listOf("confirmed-1", "accepted-unvalidated-1", "pending-1"),
        proposedSlots = listOf(retainedSlot),
        deadline = "2026-09-01T22:00:00Z",
        status = EventStatus.CONFIRMED,
        finalDate = retainedSlot.start,
        createdAt = "2026-05-21T10:00:00Z",
        updatedAt = "2026-05-21T10:00:00Z"
    )

    @Test
    fun `maps repository participant role RSVP and retained date flag to access state`() {
        val accessState = ParticipantAccessMapper.fromRepositoryRecord(
            ParticipantRepositoryRecord(
                id = "participant-1",
                eventId = confirmedEvent.id,
                userId = "confirmed-1",
                role = "PARTICIPANT",
                rsvp = "ACCEPTED",
                hasValidatedDate = 1L
            )
        )

        assertEquals("confirmed-1", accessState.userId)
        assertEquals(ParticipantAccessState.Role.MEMBER, accessState.role)
        assertEquals(ParticipantRsvp.ACCEPTED, accessState.rsvp)
        assertEquals(DateValidationState.VALIDATED_RETAINED_DATE, accessState.dateValidation)
    }

    @Test
    fun `confirmed attendee from repository data can access protected organization sections`() {
        val accessState = ParticipantAccessMapper.fromRepositoryRecord(
            ParticipantRepositoryRecord(
                id = "participant-confirmed",
                eventId = confirmedEvent.id,
                userId = "confirmed-1",
                role = "PARTICIPANT",
                rsvp = "ACCEPTED",
                hasValidatedDate = 1L
            )
        )

        val protectedSections = listOf(
            OrganizationSection.TRANSPORT,
            OrganizationSection.LODGING,
            OrganizationSection.BUDGET,
            OrganizationSection.PAYMENT,
            OrganizationSection.MEETING
        )

        protectedSections.forEach { section ->
            val decision = EventAccessPolicy.canAccessOrganizationSection(
                event = confirmedEvent,
                viewer = accessState,
                section = section
            )

            assertTrue(decision.isAllowed, "Expected repository confirmed attendee to access $section")
        }
    }

    @Test
    fun `accepted participant who has not validated retained date is refused by retained date guard`() {
        val accessState = ParticipantAccessMapper.fromRepositoryRecord(
            ParticipantRepositoryRecord(
                id = "participant-unvalidated",
                eventId = confirmedEvent.id,
                userId = "accepted-unvalidated-1",
                role = "PARTICIPANT",
                rsvp = "ACCEPTED",
                hasValidatedDate = 0L
            )
        )

        val decision = EventAccessPolicy.canAccessOrganizationSection(
            event = confirmedEvent,
            viewer = accessState,
            section = OrganizationSection.BUDGET
        )

        assertFalse(decision.isAllowed)
        assertEquals(AccessDeniedReason.RETAINED_DATE_NOT_VALIDATED, decision.reason)
    }

    @Test
    fun `pending participant from repository data is refused by attendance confirmation guard`() {
        val accessState = ParticipantAccessMapper.fromRepositoryRecord(
            ParticipantRepositoryRecord(
                id = "participant-pending",
                eventId = confirmedEvent.id,
                userId = "pending-1",
                role = "PARTICIPANT",
                rsvp = "PENDING",
                hasValidatedDate = 0L
            )
        )

        val decision = EventAccessPolicy.canAccessOrganizationSection(
            event = confirmedEvent,
            viewer = accessState,
            section = OrganizationSection.TRANSPORT
        )

        assertFalse(decision.isAllowed)
        assertEquals(AccessDeniedReason.ATTENDANCE_NOT_CONFIRMED, decision.reason)
    }
}
