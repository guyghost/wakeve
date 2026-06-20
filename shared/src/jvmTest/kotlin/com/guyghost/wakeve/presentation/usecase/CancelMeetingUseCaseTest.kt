package com.guyghost.wakeve.presentation.usecase

import com.guyghost.wakeve.calendar.CalendarService
import com.guyghost.wakeve.calendar.PlatformCalendarService
import com.guyghost.wakeve.createFreshTestDatabase
import com.guyghost.wakeve.database.WakeveDb
import com.guyghost.wakeve.meeting.DeterministicMeetingLinkProvider
import com.guyghost.wakeve.meeting.MeetingRepository
import com.guyghost.wakeve.meeting.MeetingService
import com.guyghost.wakeve.meeting.MeetingStatus
import com.guyghost.wakeve.models.EnhancedCalendarEvent
import com.guyghost.wakeve.models.MeetingPlatform
import com.guyghost.wakeve.models.NotificationMessage
import com.guyghost.wakeve.models.PushToken
import com.guyghost.wakeve.notification.NotificationServiceInterface
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours

/**
 * Tests for CancelMeetingUseCase.
 *
 * Verifies that:
 * - Cancelling a valid meeting succeeds
 * - Cancelling a non-existent meeting fails gracefully
 * - Result types are correct (success/failure)
 */
class CancelMeetingUseCaseTest {

    private lateinit var database: WakeveDb
    private lateinit var meetingService: MeetingService
    private lateinit var useCase: CancelMeetingUseCase

    @BeforeTest
    fun setUp() {
        database = createFreshTestDatabase()
        val calendarService = CalendarService(database, StubPlatformCalendarService())
        meetingService = MeetingService(
            database = database,
            calendarService = calendarService,
            notificationService = StubNotificationService(),
            meetingLinkProvider = DeterministicMeetingLinkProvider()
        )
        useCase = CancelMeetingUseCase(
            meetingService = meetingService,
            repository = MeetingRepository(database)
        )
        seedOrganizingEvent()
    }

    @Test
    fun `invoke cancels a scheduled meeting successfully`() = runTest {
        // Create a meeting to cancel
        val meeting = meetingService.createMeeting(
            eventId = "event-1",
            organizerId = "organizer-1",
            platform = MeetingPlatform.ZOOM,
            title = "Meeting to cancel",
            description = null,
            scheduledFor = Clock.System.now(),
            duration = 1.hours,
            timezone = "UTC"
        ).getOrThrow()

        val result = useCase(meeting.id, organizerId = "organizer-1")

        assertTrue(result.isSuccess)
    }

    @Test
    fun `invoke on non-existent meeting returns failure`() = runTest {
        val result = useCase("non-existent-id", organizerId = "organizer-1")

        assertFalse(result.isSuccess)
    }

    @Test
    fun `invoke rejects cancellation when current actor is not meeting organizer`() = runTest {
        seedParticipant("participant-1")
        val meeting = meetingService.createMeeting(
            eventId = "event-1",
            organizerId = "organizer-1",
            platform = MeetingPlatform.ZOOM,
            title = "Organizer owned meeting",
            description = null,
            scheduledFor = Clock.System.now(),
            duration = 1.hours,
            timezone = "UTC"
        ).getOrThrow()

        val result = useCase(meeting.id, organizerId = "participant-1")
        val persistedMeeting = MeetingRepository(database).getMeetingById(meeting.id)

        assertFalse(result.isSuccess, "A participant/non-organizer must not be able to cancel an organizer-owned meeting.")
        assertTrue(
            result.exceptionOrNull()?.message?.contains("organizer", ignoreCase = true) == true ||
                result.exceptionOrNull()?.message?.contains("unauthorized", ignoreCase = true) == true,
            "Cancellation failure should explain that the current actor is not authorized."
        )
        assertTrue(persistedMeeting != null, "Unauthorized cancellation must not delete the meeting.")
        assertTrue(
            persistedMeeting?.status == MeetingStatus.SCHEDULED,
            "Unauthorized cancellation must leave the meeting scheduled."
        )
    }

    @Test
    fun `invoke rejects organizer cancellation when event is finalized read only`() = runTest {
        val meeting = meetingService.createMeeting(
            eventId = "event-1",
            organizerId = "organizer-1",
            platform = MeetingPlatform.ZOOM,
            title = "Finalized event meeting",
            description = null,
            scheduledFor = Clock.System.now(),
            duration = 1.hours,
            timezone = "UTC"
        ).getOrThrow()
        database.eventQueries.updateEventStatus(
            status = "FINALIZED",
            updatedAt = "2026-01-01T01:00:00Z",
            id = "event-1"
        )

        val result = useCase(meeting.id, organizerId = "organizer-1")
        val persistedMeeting = MeetingRepository(database).getMeetingById(meeting.id)

        assertFalse(result.isSuccess, "Even the real organizer must not cancel meetings after the event is FINALIZED/read-only.")
        assertTrue(
            result.exceptionOrNull()?.message?.contains("finalized", ignoreCase = true) == true ||
                result.exceptionOrNull()?.message?.contains("read-only", ignoreCase = true) == true ||
                result.exceptionOrNull()?.message?.contains("organizing", ignoreCase = true) == true,
            "Cancellation failure should explain that the event is no longer mutable."
        )
        assertTrue(persistedMeeting != null, "Read-only cancellation must not delete the meeting.")
        assertTrue(
            persistedMeeting?.status == MeetingStatus.SCHEDULED,
            "Read-only cancellation must leave the meeting scheduled."
        )
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun seedOrganizingEvent() {
        val now = "2026-01-01T00:00:00Z"
        database.eventQueries.insertEvent(
            id = "event-1",
            organizerId = "organizer-1",
            title = "Test Event",
            description = "Description",
            status = "ORGANIZING",
            deadline = now,
            createdAt = now,
            updatedAt = now,
            version = 1,
            eventType = "OTHER",
            eventTypeCustom = null,
            minParticipants = null,
            maxParticipants = null,
            expectedParticipants = null,
            isSample = 0L
        )
        // Note: id and userId must be equal so that meeting_reminder.participant_id
        // FK (references participant.id) is satisfied when MeetingService uses participant.userId
        database.participantQueries.insertParticipant(
            id = "organizer-1",
            eventId = "event-1",
            userId = "organizer-1",
            role = "ORGANIZER",
            hasValidatedDate = 1L,
            joinedAt = now,
            updatedAt = now
        )
    }

    private fun seedParticipant(userId: String) {
        val now = "2026-01-01T00:00:00Z"
        database.participantQueries.insertParticipant(
            id = userId,
            eventId = "event-1",
            userId = userId,
            role = "PARTICIPANT",
            hasValidatedDate = 1L,
            joinedAt = now,
            updatedAt = now
        )
    }
}

private class StubPlatformCalendarService : PlatformCalendarService {
    override fun addEvent(event: EnhancedCalendarEvent): Result<Unit> = Result.success(Unit)
    override fun updateEvent(event: EnhancedCalendarEvent): Result<Unit> = Result.success(Unit)
    override fun deleteEvent(eventId: String): Result<Unit> = Result.success(Unit)
}

private class StubNotificationService : NotificationServiceInterface {
    override suspend fun sendNotification(message: NotificationMessage): Result<Unit> = Result.success(Unit)
    override suspend fun registerPushToken(token: PushToken): Result<Unit> = Result.success(Unit)
    override suspend fun unregisterPushToken(userId: String, deviceId: String): Result<Unit> = Result.success(Unit)
    override suspend fun getUnreadNotifications(userId: String): List<NotificationMessage> = emptyList()
    override suspend fun markAsRead(notificationId: String): Result<Unit> = Result.success(Unit)
}
