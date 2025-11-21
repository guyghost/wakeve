package com.guyghost.wakeve

import com.guyghost.wakeve.models.CalendarEvent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CalendarServiceTest {

    private val service = DefaultCalendarService()

    private val sampleEvent = CalendarEvent(
        id = "cal-1",
        title = "Team Meeting",
        description = "Weekly sync meeting",
        startTime = "2025-12-01T10:00:00Z",
        endTime = "2025-12-01T11:00:00Z",
        timezone = "UTC",
        location = "Conference Room A",
        attendees = listOf("user1@example.com", "user2@example.com"),
        organizer = "organizer@example.com",
        eventId = "event-1"
    )

    @Test
    fun generateICSInviteCreatesValidICS() {
        val invite = service.generateICSInvite(sampleEvent)

        assertEquals(sampleEvent.eventId, invite.eventId)
        assertTrue(invite.icsContent.isNotEmpty())
        assertTrue(invite.icsContent.startsWith("BEGIN:VCALENDAR"))
        assertTrue(invite.icsContent.contains("END:VCALENDAR"))
        assertTrue(invite.icsContent.contains("BEGIN:VEVENT"))
        assertTrue(invite.icsContent.contains("END:VEVENT"))
        assertTrue(invite.icsContent.contains("SUMMARY:${sampleEvent.title}"))
        assertTrue(invite.icsContent.contains("DESCRIPTION:${sampleEvent.description}"))
        assertTrue(invite.icsContent.contains("LOCATION:${sampleEvent.location}"))
        assertTrue(invite.icsContent.contains("ORGANIZER:mailto:${sampleEvent.organizer}"))
        sampleEvent.attendees.forEach { attendee ->
            assertTrue(invite.icsContent.contains("ATTENDEE:mailto:$attendee"))
        }
    }

    @Test
    fun generateICSInviteIncludesCorrectTimestamps() {
        val invite = service.generateICSInvite(sampleEvent)

        // Should contain DTSTART and DTEND in UTC
        assertTrue(invite.icsContent.contains("DTSTART:20251201T100000Z"))
        assertTrue(invite.icsContent.contains("DTEND:20251201T110000Z"))
    }

    @Test
    fun generateICSInviteHasUniqueUID() {
        val invite1 = service.generateICSInvite(sampleEvent)
        val invite2 = service.generateICSInvite(sampleEvent)

        // UIDs should be different
        val uid1 = extractUID(invite1.icsContent)
        val uid2 = extractUID(invite2.icsContent)
        assertTrue(uid1 != uid2)
    }

    private fun extractUID(ics: String): String {
        val lines = ics.lines()
        return lines.find { it.startsWith("UID:") }?.substring(4) ?: ""
    }
}