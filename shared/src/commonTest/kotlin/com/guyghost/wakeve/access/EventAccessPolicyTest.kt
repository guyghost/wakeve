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
        assertEquals(null, preview.title)
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
        OrganizationSection.entries.forEach { section ->
            val decision = EventAccessPolicy.canAccessOrganizationSection(
                event = confirmedEvent,
                viewer = ParticipantAccessState.organizer("organizer-1"),
                section = section
            )

            assertEquals(AccessDecision(isAllowed = true), decision, "Section $section")
        }
    }

    @Test
    fun `pending declined and non member viewers are denied protected organization sections`() {
        val deniedViewers = listOf(
            ParticipantAccessState.invitedPending("pending-1") to AccessDeniedReason.ATTENDANCE_NOT_CONFIRMED,
            ParticipantAccessState.declined("declined-1") to AccessDeniedReason.PARTICIPATION_DECLINED,
            ParticipantAccessState.nonMember("stranger-1") to AccessDeniedReason.NOT_EVENT_MEMBER
        )

        deniedViewers.forEach { (viewer, expectedReason) ->
            OrganizationSection.entries.forEach { section ->
                val decision = EventAccessPolicy.canAccessOrganizationSection(
                    event = confirmedEvent,
                    viewer = viewer,
                    section = section
                )

                assertEquals(
                    AccessDecision(isAllowed = false, reason = expectedReason),
                    decision,
                    "Viewer ${viewer.userId}, section $section"
                )
            }
        }
    }

    @Test
    fun `exact organizer sees metadata but does not need to join`() {
        val preview = EventAccessPolicy.invitationPreviewFor(
            event = confirmedEvent,
            viewer = ParticipantAccessState.organizer("organizer-1")
        )

        assertEquals(InvitationPreviewVisibility.EVENT_METADATA, preview.visibility)
        assertEquals("event-access-1", preview.eventId)
        assertEquals("Summer trip", preview.title)
        assertFalse(preview.canJoin)
        assertFalse(preview.exposesOrganizationDetails)
    }

    @Test
    fun `exact organizer can submit ballot without being listed as a participant`() {
        val decision = EventAccessPolicy.canSubmitPollBallot(
            event = confirmedEvent,
            viewer = ParticipantAccessState.organizer("organizer-1")
        )

        assertEquals(AccessDecision(isAllowed = true), decision)
    }

    @Test
    fun `accepted event member can submit ballot before retained date validation`() {
        val decision = EventAccessPolicy.canSubmitPollBallot(
            event = confirmedEvent,
            viewer = ParticipantAccessState.member(
                userId = "pending-1",
                rsvp = ParticipantRsvp.ACCEPTED,
                dateValidation = DateValidationState.NOT_VALIDATED
            )
        )

        assertEquals(AccessDecision(isAllowed = true), decision)
    }

    @Test
    fun `event member with unconfirmed or declined attendance cannot submit ballot`() {
        val deniedAttendance = listOf(
            ParticipantRsvp.PENDING to AccessDeniedReason.ATTENDANCE_NOT_CONFIRMED,
            ParticipantRsvp.DECLINED to AccessDeniedReason.PARTICIPATION_DECLINED,
            ParticipantRsvp.NOT_APPLICABLE to AccessDeniedReason.ATTENDANCE_NOT_CONFIRMED,
            ParticipantRsvp.UNAVAILABLE to AccessDeniedReason.ATTENDANCE_NOT_CONFIRMED
        )

        deniedAttendance.forEach { (rsvp, expectedReason) ->
            val decision = EventAccessPolicy.canSubmitPollBallot(
                event = confirmedEvent,
                viewer = ParticipantAccessState.member(
                    userId = "pending-1",
                    rsvp = rsvp,
                    dateValidation = DateValidationState.VALIDATED_RETAINED_DATE
                )
            )

            assertEquals(
                AccessDecision(isAllowed = false, reason = expectedReason),
                decision,
                "RSVP $rsvp"
            )
        }
    }

    @Test
    fun `accepted outsider cannot vote or access organization despite favorable attendance state`() {
        val viewer = ParticipantAccessState.member(
            userId = "stranger-1",
            rsvp = ParticipantRsvp.ACCEPTED,
            dateValidation = DateValidationState.VALIDATED_RETAINED_DATE
        )
        val expected = AccessDecision(isAllowed = false, reason = AccessDeniedReason.NOT_EVENT_MEMBER)

        assertEquals(expected, EventAccessPolicy.canSubmitPollBallot(confirmedEvent, viewer))
        OrganizationSection.entries.forEach { section ->
            assertEquals(
                expected,
                EventAccessPolicy.canAccessOrganizationSection(confirmedEvent, viewer, section),
                "Section $section"
            )
        }
    }

    @Test
    fun `unavailable attendance cannot access organization even with a validated retained date`() {
        listOf(ParticipantRsvp.NOT_APPLICABLE, ParticipantRsvp.UNAVAILABLE).forEach { rsvp ->
            val viewer = ParticipantAccessState.member(
                userId = "confirmed-1",
                rsvp = rsvp,
                dateValidation = DateValidationState.VALIDATED_RETAINED_DATE
            )

            OrganizationSection.entries.forEach { section ->
                assertEquals(
                    AccessDecision(isAllowed = false, reason = AccessDeniedReason.ATTENDANCE_NOT_CONFIRMED),
                    EventAccessPolicy.canAccessOrganizationSection(confirmedEvent, viewer, section),
                    "RSVP $rsvp, section $section"
                )
            }
        }
    }

    @Test
    fun `accepted member needs an explicitly validated retained date for organization`() {
        listOf(
            DateValidationState.NOT_VALIDATED,
            DateValidationState.NOT_APPLICABLE,
            DateValidationState.UNAVAILABLE
        ).forEach { dateValidation ->
            val viewer = ParticipantAccessState.member(
                userId = "confirmed-1",
                rsvp = ParticipantRsvp.ACCEPTED,
                dateValidation = dateValidation
            )

            OrganizationSection.entries.forEach { section ->
                assertEquals(
                    AccessDecision(isAllowed = false, reason = AccessDeniedReason.RETAINED_DATE_NOT_VALIDATED),
                    EventAccessPolicy.canAccessOrganizationSection(confirmedEvent, viewer, section),
                    "Date validation $dateValidation, section $section"
                )
            }
        }
    }
}
