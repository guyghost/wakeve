package com.guyghost.wakeve.presentation.usecase

import com.guyghost.wakeve.calendar.CalendarService
import com.guyghost.wakeve.calendar.PlatformCalendarService
import com.guyghost.wakeve.createFreshTestDatabase
import com.guyghost.wakeve.database.WakeveDb
import com.guyghost.wakeve.meeting.MeetingRepository
import com.guyghost.wakeve.meeting.MeetingService
import com.guyghost.wakeve.models.EnhancedCalendarEvent
import com.guyghost.wakeve.models.MeetingPlatform
import com.guyghost.wakeve.notification.DefaultNotificationService
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
        meetingService = MeetingService(database, calendarService, DefaultNotificationService())
        useCase = CancelMeetingUseCase(
            meetingService = meetingService,
            repository = MeetingRepository(database)
        )
        seedConfirmedEvent()
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

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun seedConfirmedEvent() {
        val now = "2026-01-01T00:00:00Z"
        database.eventQueries.insertEvent(
            id = "event-1",
            organizerId = "organizer-1",
            title = "Test Event",
            description = "Description",
            status = "CONFIRMED",
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
}

private class StubPlatformCalendarService : PlatformCalendarService {
    override fun addEvent(event: EnhancedCalendarEvent): Result<Unit> = Result.success(Unit)
    override fun updateEvent(event: EnhancedCalendarEvent): Result<Unit> = Result.success(Unit)
    override fun deleteEvent(eventId: String): Result<Unit> = Result.success(Unit)
}
