package com.guyghost.wakeve.presentation.usecase

import com.guyghost.wakeve.database.WakevDb
import com.guyghost.wakeve.models.*
import com.guyghost.wakeve.meeting.MeetingPlatformProvider
import com.guyghost.wakeve.meeting.MeetingService
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format.DateTimeFormat
import kotlin.time.Duration

/**
 * Helper to create a test database instance
 */
fun createTestDatabase(): WakevDb {
    // In real implementation, use SQLDelight in-memory database
    // For now, return a placeholder
    error("Test database creation not implemented")
}

/**
 * Helper to create a mock meeting platform provider
 */
fun createMockMeetingPlatformProvider(): MeetingPlatformProvider {
    return object : MeetingPlatformProvider {
        override suspend fun generateMeetingLink(
            platform: MeetingPlatform,
            title: String,
            description: String?,
            startTime: Instant,
            duration: Duration
        ): String {
            return when (platform) {
                MeetingPlatform.ZOOM -> "https://zoom.us/j/mock-${Clock.System.now().toEpochMilliseconds()}"
                MeetingPlatform.GOOGLE_MEET -> "https://meet.google.com/mock-${Clock.System.now().toEpochMilliseconds()}"
                MeetingPlatform.FACETIME -> "facetime://mock-${Clock.System.now().toEpochMilliseconds()}"
                else -> "https://example.com/meet/${Clock.System.now().toEpochMilliseconds()}"
            }
        }

        override fun getHostMeetingId(meetingLink: String): String {
            return meetingLink.substringAfterLast("/")
        }

        override fun cancelMeeting(platform: MeetingPlatform, hostMeetingId: String) {
            // Mock implementation - do nothing
        }
    }
}

/**
 * Helper to create a mock meeting service
 */
fun createMockMeetingService(
    database: WakevDb,
    repository: com.guyghost.wakeve.meeting.MeetingRepository
): MeetingService {
    val mockCalendarService = object : com.guyghost.wakeve.calendar.PlatformCalendarService {
        override fun addEvent(event: com.guyghost.wakeve.models.EnhancedCalendarEvent): Result<Unit> = Result.success(Unit)
        override fun updateEvent(event: com.guyghost.wakeve.models.EnhancedCalendarEvent): Result<Unit> = Result.success(Unit)
        override fun deleteEvent(eventId: String): Result<Unit> = Result.success(Unit)
    }
    val calendarService = com.guyghost.wakeve.calendar.CalendarService(database, mockCalendarService)
    val notificationService = com.guyghost.wakeve.DefaultNotificationService()
    
    return MeetingService(
        database = database,
        calendarService = calendarService,
        notificationService = notificationService
    )
}
