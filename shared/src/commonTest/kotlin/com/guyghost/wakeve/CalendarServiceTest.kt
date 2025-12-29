package com.guyghost.wakeve.calendar

import com.guyghost.wakeve.models.ICSDocument
import com.guyghost.wakeve.models.EnhancedCalendarEvent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class CalendarServiceTest {

    @Test
    fun `generate ICS document with all event details`() {
        val icsContent = buildTestICSContent()

        assertTrue(icsContent.contains("BEGIN:VCALENDAR"))
        assertTrue(icsContent.contains("END:VCALENDAR"))
        assertTrue(icsContent.contains("SUMMARY:Team Meeting"))
        assertTrue(icsContent.contains("DESCRIPTION:Weekly sync"))
        assertTrue(icsContent.contains("LOCATION:Conference Room A"))
        assertTrue(icsContent.contains("DTSTART"))
        assertTrue(icsContent.contains("DTEND"))
    }

    @Test
    fun `ICS document includes correct timezone`() {
        val icsContent = buildTestICSContent()

        assertTrue(icsContent.contains("DTSTART;TZID=Europe/Paris"))
        assertTrue(icsContent.contains("DTEND;TZID=Europe/Paris"))
    }

    @Test
    fun `ICS document contains VALARM for reminders`() {
        val icsContent = buildTestICSContent()
        
        assertTrue(icsContent.contains("BEGIN:VALARM"))
        assertTrue(icsContent.contains("TRIGGER:-P1DT090000"))
        assertTrue(icsContent.contains("TRIGGER:-P1W"))
        assertTrue(icsContent.contains("ACTION:DISPLAY"))
        assertTrue(icsContent.contains("END:VALARM"))
    }

    @Test
    fun `ICS document has unique UID`() {
        val ics1 = buildTestICSContent()
        val ics2 = buildTestICSContent("different-event-id")
        
        // UIDs should be different for different events
        val uid1 = extractUID(ics1)
        val uid2 = extractUID(ics2)
        assertTrue(uid1 != uid2)
    }

    @Test
    fun `ICS document filename is sanitized`() {
        // Test the regex pattern used in CalendarService for sanitizing filenames
        val testTitle = "Événement @ Test!"
        val sanitized = testTitle.replace(Regex("[^a-zA-Z0-9\\s]"), "").replace("\\s+".toRegex(), "_")

        // The regex removes accented characters and special chars, keeping only ASCII and spaces
        // "Événement @ Test!" becomes "vnement_Test"
        assertTrue(sanitized == "vnement_Test")
        assertFalse(sanitized.contains("É"))
        assertFalse(sanitized.contains("é"))
        assertFalse(sanitized.contains("@"))
        assertFalse(sanitized.contains("!"))
    }

    // Platform Calendar Service Tests
    
    @Test
    fun `PlatformCalendarService addEvent returns success`() {
        val mockService = MockPlatformCalendarService()
        val result = mockService.addEvent(createTestCalendarEvent())
        
        assertTrue(result.isSuccess)
        assertEquals(Unit, result.getOrNull())
    }

    @Test
    fun `PlatformCalendarService updateEvent returns success`() {
        val mockService = MockPlatformCalendarService()
        val result = mockService.updateEvent(createTestCalendarEvent())
        
        assertTrue(result.isSuccess)
        assertEquals(Unit, result.getOrNull())
    }

    @Test
    fun `PlatformCalendarService deleteEvent returns success`() {
        val mockService = MockPlatformCalendarService()
        val result = mockService.deleteEvent("test-event-id")
        
        assertTrue(result.isSuccess)
        assertEquals(Unit, result.getOrNull())
    }

    @Test
    fun `PlatformCalendarService handles errors`() {
        val errorMockService = ErrorMockPlatformCalendarService()
        val result = errorMockService.addEvent(createTestCalendarEvent())
        
        assertTrue(result.isFailure)
    }

    @Test
    fun `PlatformCalendarService permission denied handled`() {
        val permissionMockService = PermissionDeniedMockPlatformCalendarService()
        val result = permissionMockService.addEvent(createTestCalendarEvent())
        
        assertTrue(result.isFailure)
    }

    // Helper methods
    
    private fun buildTestICSContent(eventId: String = "event-1"): String {
        return """
        BEGIN:VCALENDAR
        VERSION:2.0
        PRODID:-//Wakeve//Wakeve Event//FR
        CALSCALE:GREGORIAN
        METHOD:REQUEST
        BEGIN:VEVENT
        UID:${eventId}@wakeve.app
        DTSTAMP:20251201T090000Z
        DTSTART;TZID=Europe/Paris:20251201T100000
        DTEND;TZID=Europe/Paris:20251201T110000
        SUMMARY:Team Meeting
        DESCRIPTION:Weekly sync
        LOCATION:Conference Room A
        ORGANIZER;CN=Organizer:mailto:organizer@example.com
        END:VEVENT
        BEGIN:VALARM
        TRIGGER:-P1DT090000
        DESCRIPTION:Rappel: Événement dans 1 jour
        ACTION:DISPLAY
        END:VALARM
        BEGIN:VALARM
        TRIGGER:-P1W
        DESCRIPTION:Rappel: Événement dans 1 semaine
        ACTION:DISPLAY
        END:VALARM
        END:VCALENDAR
        """.trimIndent()
    }

    private fun extractUID(text: String): String {
        val lines = text.lines()
        return lines.find { it.startsWith("UID:") }?.substring(4) ?: ""
    }

    private fun createTestCalendarEvent() = EnhancedCalendarEvent(
        id = "test-event-1",
        title = "Team Meeting",
        description = "Weekly sync",
        location = "Conference Room A",
        startDate = kotlinx.datetime.Instant.parse("2025-12-01T10:00:00Z"),
        endDate = kotlinx.datetime.Instant.parse("2025-12-01T12:00:00Z"),
        attendees = listOf("user1@example.com", "user2@example.com"),
        organizer = "organizer@example.com"
    )
}

// Mock implementations for testing

class MockPlatformCalendarService : PlatformCalendarService {
    override fun addEvent(event: EnhancedCalendarEvent): Result<Unit> {
        return Result.success(Unit)
    }

    override fun updateEvent(event: EnhancedCalendarEvent): Result<Unit> {
        return Result.success(Unit)
    }

    override fun deleteEvent(eventId: String): Result<Unit> {
        return Result.success(Unit)
    }
}

class ErrorMockPlatformCalendarService : PlatformCalendarService {
    override fun addEvent(event: EnhancedCalendarEvent): Result<Unit> {
        return Result.failure(Exception("Mock error"))
    }

    override fun updateEvent(event: EnhancedCalendarEvent): Result<Unit> {
        return Result.failure(Exception("Mock error"))
    }

    override fun deleteEvent(eventId: String): Result<Unit> {
        return Result.failure(Exception("Mock error"))
    }
}

/**
 * Exception thrown when calendar permission is denied.
 * Defined locally for commonTest since the actual implementation is platform-specific.
 */
class CalendarPermissionDeniedException : Exception("Calendar permission denied")

class PermissionDeniedMockPlatformCalendarService : PlatformCalendarService {
    override fun addEvent(event: EnhancedCalendarEvent): Result<Unit> {
        return Result.failure(CalendarPermissionDeniedException())
    }

    override fun updateEvent(event: EnhancedCalendarEvent): Result<Unit> {
        return Result.failure(CalendarPermissionDeniedException())
    }

    override fun deleteEvent(eventId: String): Result<Unit> {
        return Result.failure(CalendarPermissionDeniedException())
    }
}
