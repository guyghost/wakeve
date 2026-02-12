package com.guyghost.wakeve.meeting

import com.guyghost.wakeve.DefaultNotificationService
import com.guyghost.wakeve.createFreshTestDatabase
import com.guyghost.wakeve.calendar.CalendarService
import com.guyghost.wakeve.calendar.PlatformCalendarService
import com.guyghost.wakeve.database.WakeveDb
import com.guyghost.wakeve.models.EnhancedCalendarEvent
import com.guyghost.wakeve.models.InvitationStatus
import com.guyghost.wakeve.models.MeetingPlatform
import com.guyghost.wakeve.models.MeetingStatus
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours

class MeetingServiceTest {

    private lateinit var database: WakeveDb
    private lateinit var service: MeetingService

    @BeforeTest
    fun setUp() {
        database = createFreshTestDatabase()
        val calendarService = CalendarService(database, NoopPlatformCalendarService())
        service = MeetingService(database, calendarService, DefaultNotificationService())
    }

    @Test
    fun createMeetingSucceedsForConfirmedEvent() = runTest {
        seedEvent(eventId = "event-1", status = "CONFIRMED")
        seedParticipant(eventId = "event-1", participantId = "participant-1", userId = "participant-1", validated = true)

        val result = service.createMeeting(
            eventId = "event-1",
            organizerId = "organizer-1",
            platform = MeetingPlatform.ZOOM,
            title = "Planning meeting",
            description = "Kickoff",
            scheduledFor = Clock.System.now(),
            duration = 1.hours,
            timezone = "Europe/Paris"
        )

        assertTrue(result.isSuccess)
        val meeting = result.getOrThrow()
        assertEquals(MeetingPlatform.ZOOM, meeting.platform)
        assertEquals(MeetingStatus.SCHEDULED, meeting.status)

        val pendingReminders = database.meetingReminderQueries.countPendingByMeetingId(meeting.id).executeAsOne()
        assertEquals(4L, pendingReminders)
    }

    @Test
    fun createMeetingFailsForDraftEvent() = runTest {
        seedEvent(eventId = "event-1", status = "DRAFT")
        seedParticipant(eventId = "event-1", participantId = "participant-1", userId = "participant-1", validated = true)

        val result = service.createMeeting(
            eventId = "event-1",
            organizerId = "organizer-1",
            platform = MeetingPlatform.ZOOM,
            title = "Planning meeting",
            description = null,
            scheduledFor = Clock.System.now(),
            duration = 1.hours,
            timezone = "Europe/Paris"
        )

        assertTrue(result.isFailure)
    }

    @Test
    fun sendInvitationsInvitesOnlyValidatedParticipants() = runTest {
        seedEvent(eventId = "event-1", status = "CONFIRMED")
        seedParticipant(eventId = "event-1", participantId = "validated-user", userId = "validated-user", validated = true)
        seedParticipant(eventId = "event-1", participantId = "unvalidated-user", userId = "unvalidated-user", validated = false)

        val meetingId = service.createMeeting(
            eventId = "event-1",
            organizerId = "organizer-1",
            platform = MeetingPlatform.GOOGLE_MEET,
            title = "Invites",
            description = null,
            scheduledFor = Clock.System.now(),
            duration = 1.hours,
            timezone = "Europe/Paris"
        ).getOrThrow().id

        val result = service.sendInvitations(meetingId)

        assertTrue(result.isSuccess)

        val invitations = database.meetingInvitationQueries.selectByMeetingId(meetingId).executeAsList()
        assertTrue(invitations.any { it.participant_id == "validated-user" })
        assertFalse(invitations.any { it.participant_id == "unvalidated-user" })
    }

    @Test
    fun respondToInvitationUpdatesStatusAndTimestamps() = runTest {
        seedEvent(eventId = "event-1", status = "CONFIRMED")
        seedParticipant(eventId = "event-1", participantId = "participant-1", userId = "participant-1", validated = true)

        val meetingId = service.createMeeting(
            eventId = "event-1",
            organizerId = "organizer-1",
            platform = MeetingPlatform.FACETIME,
            title = "Respond",
            description = null,
            scheduledFor = Clock.System.now(),
            duration = 1.hours,
            timezone = "Europe/Paris"
        ).getOrThrow().id

        service.sendInvitations(meetingId).getOrThrow()
        val invitation = database.meetingInvitationQueries.selectByMeetingId(meetingId).executeAsList().firstOrNull()
        assertNotNull(invitation)

        val result = service.respondToInvitation(invitation.id, InvitationStatus.ACCEPTED)

        assertTrue(result.isSuccess)

        val updated = database.meetingInvitationQueries.selectById(invitation.id).executeAsOne()
        assertEquals(InvitationStatus.ACCEPTED.name, updated.status)
        assertNotNull(updated.responded_at)
        assertNotNull(updated.accepted_at)
    }

    @Test
    fun createMeetingGeneratesGoogleMeetLink() = runTest {
        seedEvent(eventId = "event-1", status = "CONFIRMED")
        seedParticipant(eventId = "event-1", participantId = "participant-1", userId = "participant-1", validated = true)

        val result = service.createMeeting(
            eventId = "event-1",
            organizerId = "organizer-1",
            platform = MeetingPlatform.GOOGLE_MEET,
            title = "Google Meet Test",
            description = null,
            scheduledFor = Clock.System.now(),
            duration = 1.hours,
            timezone = "Europe/Paris"
        )

        assertTrue(result.isSuccess)
        val meeting = result.getOrThrow()
        assertEquals(MeetingPlatform.GOOGLE_MEET, meeting.platform)
        assertTrue(meeting.meetingUrl.contains("meet.google.com"))
        assertEquals(MeetingStatus.SCHEDULED, meeting.status)
    }

    @Test
    fun createMeetingGeneratesFaceTimeURL() = runTest {
        seedEvent(eventId = "event-1", status = "CONFIRMED")
        seedParticipant(eventId = "event-1", participantId = "participant-1", userId = "participant-1", validated = true)

        val result = service.createMeeting(
            eventId = "event-1",
            organizerId = "organizer-1",
            platform = MeetingPlatform.FACETIME,
            title = "FaceTime Test",
            description = null,
            scheduledFor = Clock.System.now(),
            duration = 1.hours,
            timezone = "Europe/Paris"
        )

        assertTrue(result.isSuccess)
        val meeting = result.getOrThrow()
        assertEquals(MeetingPlatform.FACETIME, meeting.platform)
        assertTrue(meeting.meetingUrl.contains("facetime://"))
        assertEquals(MeetingStatus.SCHEDULED, meeting.status)
    }

    @Test
    fun cancelMeetingUpdatesStatusToCancelled() = runTest {
        seedEvent(eventId = "event-1", status = "CONFIRMED")
        seedParticipant(eventId = "event-1", participantId = "participant-1", userId = "participant-1", validated = true)

        val meeting = service.createMeeting(
            eventId = "event-1",
            organizerId = "organizer-1",
            platform = MeetingPlatform.ZOOM,
            title = "To Cancel",
            description = null,
            scheduledFor = Clock.System.now(),
            duration = 1.hours,
            timezone = "Europe/Paris"
        ).getOrThrow()

        val result = service.cancelMeeting(meeting.id)

        assertTrue(result.isSuccess)

        val cancelledMeeting = service.getMeeting(meeting.id)
        assertNotNull(cancelledMeeting)
        assertEquals(MeetingStatus.CANCELLED, cancelledMeeting?.status)
    }

    @Test
    fun generateMeetingLinkReturnsValidLinkForZoom() = runTest {
        val result = service.generateMeetingLink(
            platform = MeetingPlatform.ZOOM,
            title = "Test Meeting",
            description = "Test Description",
            scheduledFor = Clock.System.now(),
            duration = 1.hours
        )

        assertTrue(result.isSuccess)
        val response = result.getOrThrow()
        assertNotNull(response.meetingId)
        assertNotNull(response.meetingUrl)
        assertTrue(response.meetingUrl.contains("zoom.us"))
        assertNotNull(response.password)
    }

    @Test
    fun generateMeetingLinkReturnsValidLinkForGoogleMeet() = runTest {
        val result = service.generateMeetingLink(
            platform = MeetingPlatform.GOOGLE_MEET,
            title = "Test Meeting",
            description = "Test Description",
            scheduledFor = Clock.System.now(),
            duration = 1.hours
        )

        assertTrue(result.isSuccess)
        val response = result.getOrThrow()
        assertNotNull(response.meetingId)
        assertNotNull(response.meetingUrl)
        assertTrue(response.meetingUrl.contains("meet.google.com"))
    }

    @Test
    fun createMeetingFailsWhenEventNotFound() = runTest {
        val result = service.createMeeting(
            eventId = "non-existent-event",
            organizerId = "organizer-1",
            platform = MeetingPlatform.ZOOM,
            title = "Test",
            description = null,
            scheduledFor = Clock.System.now(),
            duration = 1.hours,
            timezone = "Europe/Paris"
        )

        assertTrue(result.isFailure)
    }

    @Test
    fun sendInvitationsCreatesNotificationsForValidatedParticipants() = runTest {
        seedEvent(eventId = "event-1", status = "CONFIRMED")
        seedParticipant(eventId = "event-1", participantId = "user-1", userId = "user-1", validated = true)
        seedParticipant(eventId = "event-1", participantId = "user-2", userId = "user-2", validated = true)
        seedParticipant(eventId = "event-1", participantId = "user-3", userId = "user-3", validated = false)

        val meetingId = service.createMeeting(
            eventId = "event-1",
            organizerId = "organizer-1",
            platform = MeetingPlatform.ZOOM,
            title = "Meeting",
            description = null,
            scheduledFor = Clock.System.now(),
            duration = 1.hours,
            timezone = "Europe/Paris"
        ).getOrThrow().id

        val result = service.sendInvitations(meetingId)

        assertTrue(result.isSuccess)

        val invitations = database.meetingInvitationQueries.selectByMeetingId(meetingId).executeAsList()
        assertEquals(2, invitations.size) // Only validated participants
        assertTrue(invitations.any { it.participant_id == "user-1" })
        assertTrue(invitations.any { it.participant_id == "user-2" })
        assertFalse(invitations.any { it.participant_id == "user-3" })
    }

    private fun seedEvent(eventId: String, status: String) {
        database.eventQueries.insertEvent(
            id = eventId,
            organizerId = "organizer-1",
            title = "Event $eventId",
            description = "Description",
            status = status,
            deadline = "2026-01-01T00:00:00Z",
            createdAt = "2026-01-01T00:00:00Z",
            updatedAt = "2026-01-01T00:00:00Z",
            version = 1,
            eventType = "OTHER",
            eventTypeCustom = null,
            minParticipants = null,
            maxParticipants = null,
            expectedParticipants = null
        )
    }

    private fun seedParticipant(eventId: String, participantId: String, userId: String, validated: Boolean) {
        database.participantQueries.insertParticipant(
            id = participantId,
            eventId = eventId,
            userId = userId,
            role = "PARTICIPANT",
            hasValidatedDate = if (validated) 1L else 0L,
            joinedAt = "2026-01-01T00:00:00Z",
            updatedAt = "2026-01-01T00:00:00Z"
        )
    }
}

private class NoopPlatformCalendarService : PlatformCalendarService {
    override fun addEvent(event: EnhancedCalendarEvent): Result<Unit> = Result.success(Unit)
    override fun updateEvent(event: EnhancedCalendarEvent): Result<Unit> = Result.success(Unit)
    override fun deleteEvent(eventId: String): Result<Unit> = Result.success(Unit)
}
