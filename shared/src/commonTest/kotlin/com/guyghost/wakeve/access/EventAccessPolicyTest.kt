package com.guyghost.wakeve.access

import com.guyghost.wakeve.models.Event
import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.models.TimeOfDay
import com.guyghost.wakeve.models.TimeSlot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EventAccessPolicyTest {

    private val retainedSlot = TimeSlot(
        id = "slot-retained",
        start = "2026-07-10T18:00:00Z",
        end = "2026-07-10T22:00:00Z",
        timezone = "Europe/Paris",
        timeOfDay = TimeOfDay.SPECIFIC
    )

    private val confirmedEvent = Event(
        id = "event-access-1",
        title = "Summer trip",
        description = "Confirmed trip details",
        organizerId = "organizer-1",
        participants = listOf("pending-1", "confirmed-1", "declined-1"),
        proposedSlots = listOf(retainedSlot),
        deadline = "2026-07-01T22:00:00Z",
        status = EventStatus.CONFIRMED,
        finalDate = retainedSlot.start,
        createdAt = "2026-05-21T10:00:00Z",
        updatedAt = "2026-05-21T10:00:00Z"
    )

    @Test
    fun `invitation preview shows joinable event metadata to invited pending participant`() {
        val preview = EventAccessPolicy.invitationPreviewFor(
            event = confirmedEvent,
            viewer = ParticipantAccessState.invitedPending("pending-1")
        )

        assertEquals(InvitationPreviewVisibility.EVENT_METADATA, preview.visibility)
        assertEquals("event-access-1", preview.eventId)
        assertEquals("Summer trip", preview.title)
        assertTrue(preview.canJoin)
        assertFalse(preview.exposesOrganizationDetails)
    }

    @Test
    fun `public invitation preview for non member hides sensitive event details`() {
        val preview = EventAccessPolicy.invitationPreviewFor(
            event = confirmedEvent,
            viewer = ParticipantAccessState.nonMember("stranger-1")
        )

        assertEquals(InvitationPreviewVisibility.PUBLIC_METADATA_ONLY, preview.visibility)
        assertEquals("event-access-1", preview.eventId)
        assertFalse(preview.canJoin)
        assertFalse(preview.exposesOrganizationDetails)
    }

    @Test
    fun `accepted RSVP without retained date validation cannot access organization sections`() {
        val viewer = ParticipantAccessState.member(
            userId = "pending-1",
            rsvp = ParticipantRsvp.ACCEPTED,
            dateValidation = DateValidationState.NOT_VALIDATED
        )

        val decision = EventAccessPolicy.canAccessOrganizationSection(
            event = confirmedEvent,
            viewer = viewer,
            section = OrganizationSection.TRANSPORT
        )

        assertFalse(decision.isAllowed)
        assertEquals(AccessDeniedReason.RETAINED_DATE_NOT_VALIDATED, decision.reason)
    }

    @Test
    fun `confirmed attendee can access full logistics budget payment and meeting sections`() {
        val viewer = ParticipantAccessState.member(
            userId = "confirmed-1",
            rsvp = ParticipantRsvp.ACCEPTED,
            dateValidation = DateValidationState.VALIDATED_RETAINED_DATE
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
                viewer = viewer,
                section = section
            )

            assertTrue(decision.isAllowed, "Expected confirmed attendee to access $section")
        }
    }

    @Test
    fun `organizer can access organization sections even without participant RSVP state`() {
        val decision = EventAccessPolicy.canAccessOrganizationSection(
            event = confirmedEvent,
            viewer = ParticipantAccessState.organizer("organizer-1"),
            section = OrganizationSection.BUDGET
        )

        assertTrue(decision.isAllowed)
    }

    @Test
    fun `pending declined and non member viewers are denied protected organization sections`() {
        val deniedViewers = listOf(
            ParticipantAccessState.invitedPending("pending-1") to AccessDeniedReason.ATTENDANCE_NOT_CONFIRMED,
            ParticipantAccessState.declined("declined-1") to AccessDeniedReason.PARTICIPATION_DECLINED,
            ParticipantAccessState.nonMember("stranger-1") to AccessDeniedReason.NOT_EVENT_MEMBER
        )

        deniedViewers.forEach { (viewer, expectedReason) ->
            val decision = EventAccessPolicy.canAccessOrganizationSection(
                event = confirmedEvent,
                viewer = viewer,
                section = OrganizationSection.PAYMENT
            )

            assertFalse(decision.isAllowed, "Expected ${viewer.userId} to be denied")
            assertEquals(expectedReason, decision.reason)
        }
    }
}
