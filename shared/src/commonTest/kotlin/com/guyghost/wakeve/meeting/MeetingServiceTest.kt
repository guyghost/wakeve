package com.guyghost.wakeve.meeting

import com.guyghost.wakeve.NotificationService
import com.guyghost.wakeve.calendar.CalendarService
import com.guyghost.wakeve.database.WakevDb
import com.guyghost.wakeve.models.InvitationStatus
import com.guyghost.wakeve.models.MeetingPlatform
import com.guyghost.wakeve.models.MeetingStatus
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours

/**
 * Tests pour MeetingService
 */
class MeetingServiceTest {

    private lateinit var database: WakevDb
    private lateinit var meetingService: MeetingService
    private lateinit var calendarService: CalendarService
    private lateinit var notificationService: NotificationService

    @Test
    fun `createZoomMeeting generates valid meeting ID and password`() = runTest {
        // Given
        setupTestEnvironment()
        createTestEvent(eventId = "event-1", status = "CONFIRMED")
        createTestParticipant(eventId = "event-1", userId = "participant-1", hasValidatedDate = 1L)

        // When
        val result = meetingService.createMeeting(
            eventId = "event-1",
            organizerId = "organizer-1",
            platform = MeetingPlatform.ZOOM,
            title = "Test Meeting",
            description = "Test Description",
            scheduledFor = Clock.System.now(),
            duration = 1.hours,
            timezone = "Europe/Paris",
            requirePassword = true,
            waitingRoom = true
        )

        // Then
        assertTrue(result.isSuccess)
        val meeting = result.getOrThrow()
        assertEquals(MeetingPlatform.ZOOM, meeting.platform)
        assertTrue(meeting.meetingId.length == 10)
        assertTrue(meeting.meetingPassword?.length == 6)
        assertTrue(meeting.requirePassword)
        assertTrue(meeting.waitingRoom)
        assertTrue(meeting.meetingUrl.contains("zoom.us/j/"))
    }

    @Test
    fun `createGoogleMeet generates valid meet code`() = runTest {
        // Given
        setupTestEnvironment()
        createTestEvent(eventId = "event-1", status = "CONFIRMED")
        createTestParticipant(eventId = "event-1", userId = "participant-1", hasValidatedDate = 1L)

        // When
        val result = meetingService.createMeeting(
            eventId = "event-1",
            organizerId = "organizer-1",
            platform = MeetingPlatform.GOOGLE_MEET,
            title = "Test Meeting",
            description = null,
            scheduledFor = Clock.System.now(),
            duration = 1.hours,
            timezone = "Europe/Paris"
        )

        // Then
        assertTrue(result.isSuccess)
        val meeting = result.getOrThrow()
        assertEquals(MeetingPlatform.GOOGLE_MEET, meeting.platform)
        assertTrue(meeting.meetingId.contains("-"))
        assertTrue(meeting.meetingUrl.contains("meet.google.com/"))
        assertNull(meeting.meetingPassword)
        assertFalse(meeting.requirePassword)
        assertFalse(meeting.waitingRoom)
    }

    @Test
    fun `createFaceTimeMeeting uses organizer ID as meeting ID`() = runTest {
        // Given
        setupTestEnvironment()
        createTestEvent(eventId = "event-1", status = "CONFIRMED")
        createTestParticipant(eventId = "event-1", userId = "participant-1", hasValidatedDate = 1L)

        // When
        val result = meetingService.createMeeting(
            eventId = "event-1",
            organizerId = "organizer-appleid@icloud.com",
            platform = MeetingPlatform.FACETIME,
            title = "Test Meeting",
            description = null,
            scheduledFor = Clock.System.now(),
            duration = 1.hours,
            timezone = "Europe/Paris"
        )

        // Then
        assertTrue(result.isSuccess)
        val meeting = result.getOrThrow()
        assertEquals(MeetingPlatform.FACETIME, meeting.platform)
        assertEquals("organizer-appleid@icloud.com", meeting.meetingId)
        assertNull(meeting.meetingPassword)
        assertNull(meeting.dialInNumber)
        assertFalse(meeting.requirePassword)
        assertFalse(meeting.waitingRoom)
    }

    @Test
    fun `createMeeting fails when event is not confirmed`() = runTest {
        // Given
        setupTestEnvironment()
        createTestEvent(eventId = "event-1", status = "DRAFT")
        createTestParticipant(eventId = "event-1", userId = "participant-1", hasValidatedDate = 1L)

        // When
        val result = meetingService.createMeeting(
            eventId = "event-1",
            organizerId = "organizer-1",
            platform = MeetingPlatform.ZOOM,
            title = "Test Meeting",
            description = null,
            scheduledFor = Clock.System.now(),
            duration = 1.hours,
            timezone = "Europe/Paris"
        )

        // Then
        assertTrue(result.isFailure)
    }

    @Test
    fun `startMeeting updates status to STARTED`() = runTest {
        // Given
        setupTestEnvironment()
        val meetingId = createTestMeeting(status = MeetingStatus.SCHEDULED)

        // When
        val result = meetingService.startMeeting(meetingId)

        // Then
        assertTrue(result.isSuccess)
        val meeting = result.getOrThrow()
        assertEquals(MeetingStatus.STARTED, meeting.status)
    }

    @Test
    fun `startMeeting fails when already started`() = runTest {
        // Given
        setupTestEnvironment()
        val meetingId = createTestMeeting(status = MeetingStatus.STARTED)

        // When
        val result = meetingService.startMeeting(meetingId)

        // Then
        assertTrue(result.isFailure)
    }

    @Test
    fun `endMeeting updates status to ENDED`() = runTest {
        // Given
        setupTestEnvironment()
        val meetingId = createTestMeeting(status = MeetingStatus.STARTED)

        // When
        val result = meetingService.endMeeting(meetingId)

        // Then
        assertTrue(result.isSuccess)
        val meeting = result.getOrThrow()
        assertEquals(MeetingStatus.ENDED, meeting.status)
    }

    @Test
    fun `endMeeting fails when already ended`() = runTest {
        // Given
        setupTestEnvironment()
        val meetingId = createTestMeeting(status = MeetingStatus.ENDED)

        // When
        val result = meetingService.endMeeting(meetingId)

        // Then
        assertTrue(result.isFailure)
    }

    @Test
    fun `cancelMeeting updates status to CANCELLED`() = runTest {
        // Given
        setupTestEnvironment()
        val meetingId = createTestMeeting(status = MeetingStatus.SCHEDULED)

        // When
        val result = meetingService.cancelMeeting(meetingId)

        // Then
        assertTrue(result.isSuccess)
        val meeting = meetingService.getMeeting(meetingId)
        assertEquals(MeetingStatus.CANCELLED, meeting?.status)
    }

    @Test
    fun `sendInvitations only invites validated participants`() = runTest {
        // Given
        setupTestEnvironment()
        val meetingId = createTestMeeting(status = MeetingStatus.SCHEDULED)
        val validatedParticipant = createTestParticipant(
            eventId = "event-1",
            userId = "validated-user",
            hasValidatedDate = 1L
        )
        val unvalidatedParticipant = createTestParticipant(
            eventId = "event-1",
            userId = "unvalidated-user",
            hasValidatedDate = 0L
        )

        // When
        val result = meetingService.sendInvitations(meetingId)

        // Then
        assertTrue(result.isSuccess)

        val invitations = database.meetingInvitationQueries
            .selectByMeetingId(meetingId)
            .executeAsList()

        assertTrue(invitations.any { it.participant_id == validatedParticipant })
        assertFalse(invitations.any { it.participant_id == unvalidatedParticipant })
    }

    @Test
    fun `respondToInvitation updates status and timestamps`() = runTest {
        // Given
        setupTestEnvironment()
        createTestEvent(eventId = "event-1", status = "CONFIRMED")
        createTestParticipant(eventId = "event-1", userId = "participant-1", hasValidatedDate = 1L)
        val meetingId = createTestMeeting(status = MeetingStatus.SCHEDULED)

        // Create invitation
        val invitationId = createTestInvitation(
            meetingId = meetingId,
            participantId = "participant-1",
            status = InvitationStatus.PENDING
        )

        // When
        val result = meetingService.respondToInvitation(
            invitationId = invitationId,
            status = InvitationStatus.ACCEPTED
        )

        // Then
        assertTrue(result.isSuccess)

        val invitation = database.meetingInvitationQueries
            .selectById(invitationId)
            .executeAsOne()

        assertEquals(InvitationStatus.ACCEPTED.name, invitation.status)
        assertNotNull(invitation.responded_at)
        assertNotNull(invitation.accepted_at)
    }

    @Test
    fun `generateMeetingLink returns correct platform link`() = runTest {
        // Given
        setupTestEnvironment()

        // When - Zoom
        val zoomResult = meetingService.generateMeetingLink(
            platform = MeetingPlatform.ZOOM,
            title = "Test",
            description = null,
            scheduledFor = Clock.System.now(),
            duration = 1.hours
        )

        // Then
        assertTrue(zoomResult.isSuccess)
        val zoomLink = zoomResult.getOrThrow()
        assertTrue(zoomLink.meetingUrl.contains("zoom.us/j/"))
        assertNotNull(zoomLink.password)
        assertNotNull(zoomLink.dialInNumber)

        // When - Google Meet
        val googleResult = meetingService.generateMeetingLink(
            platform = MeetingPlatform.GOOGLE_MEET,
            title = "Test",
            description = null,
            scheduledFor = Clock.System.now(),
            duration = 1.hours
        )

        // Then
        assertTrue(googleResult.isSuccess)
        val googleLink = googleResult.getOrThrow()
        assertTrue(googleLink.meetingUrl.contains("meet.google.com/"))
        assertNull(googleLink.password)
        assertNull(googleLink.dialInNumber)
    }

    @Test
    fun `updateMeeting updates specified fields`() = runTest {
        // Given
        setupTestEnvironment()
        val meetingId = createTestMeeting(
            status = MeetingStatus.SCHEDULED,
            title = "Original Title"
        )

        // When
        val result = meetingService.updateMeeting(
            meetingId = meetingId,
            title = "Updated Title",
            description = "Updated Description"
        )

        // Then
        assertTrue(result.isSuccess)
        val meeting = result.getOrThrow()
        assertEquals("Updated Title", meeting.title)
        assertEquals("Updated Description", meeting.description)
    }

    // ============ Helper methods ============

    private fun setupTestEnvironment() {
        // In a real test, this would initialize the database with test data
        // For now, this is a placeholder
    }

    private fun createTestEvent(eventId: String, status: String) {
        // Placeholder for creating test event
    }

    private fun createTestParticipant(
        eventId: String,
        userId: String,
        hasValidatedDate: Long
    ): String {
        // Placeholder for creating test participant
        return userId
    }

    private fun createTestMeeting(
        status: MeetingStatus,
        title: String = "Test Meeting"
    ): String {
        // Placeholder for creating test meeting
        return "test-meeting-${Clock.System.now().toEpochMilliseconds()}"
    }

    private fun createTestInvitation(
        meetingId: String,
        participantId: String,
        status: InvitationStatus
    ): String {
        // Placeholder for creating test invitation
        return "test-invitation-${Clock.System.now().toEpochMilliseconds()}"
    }
}
