package com.guyghost.wakeve

import com.guyghost.wakeve.models.CalendarEvent
import com.guyghost.wakeve.models.CalendarInvite

interface CalendarService {
    suspend fun addEventToCalendar(event: CalendarEvent): Result<String>
    suspend fun generateICSInvite(event: CalendarEvent): CalendarInvite
    suspend fun updateCalendarEvent(calendarEventId: String, event: CalendarEvent): Result<Unit>
    suspend fun removeCalendarEvent(calendarEventId: String): Result<Unit>
}

class DefaultCalendarService : CalendarService {

    override suspend fun addEventToCalendar(event: CalendarEvent): Result<String> {
        // Platform-specific implementation would go here
        // For now, return a mock calendar event ID
        return Result.success("calendar-event-${event.id}")
    }

    override suspend fun generateICSInvite(event: CalendarEvent): CalendarInvite {
        val uid = generateUID()
        val icsContent = buildICSContent(event, uid)
        return CalendarInvite(
            eventId = event.eventId,
            icsContent = icsContent,
            generatedAt = "2025-11-20T10:00:00Z" // Mock timestamp
        )
    }

    override suspend fun updateCalendarEvent(calendarEventId: String, event: CalendarEvent): Result<Unit> {
        // Platform-specific implementation
        return Result.success(Unit)
    }

    override suspend fun removeCalendarEvent(calendarEventId: String): Result<Unit> {
        // Platform-specific implementation
        return Result.success(Unit)
    }

    private fun buildICSContent(event: CalendarEvent, uid: String): String {
        val startUTC = formatToICSDateTime(event.startTime)
        val endUTC = formatToICSDateTime(event.endTime)

        val attendees = event.attendees.joinToString("\n") { attendee ->
            "ATTENDEE;RSVP=TRUE:mailto:$attendee"
        }

        return """
BEGIN:VCALENDAR
VERSION:2.0
PRODID:-//Wakeve//Event Organizer//EN
BEGIN:VEVENT
UID:$uid
DTSTAMP:20251120T100000Z
DTSTART:$startUTC
DTEND:$endUTC
SUMMARY:${event.title}
DESCRIPTION:${event.description}
LOCATION:${event.location ?: ""}
ORGANIZER:mailto:${event.organizer}
$attendees
END:VEVENT
END:VCALENDAR
        """.trimIndent()
    }

    private fun formatToICSDateTime(isoString: String): String {
        // Convert ISO 8601 to ICS format (YYYYMMDDTHHMMSSZ)
        // Assume input is already in UTC (ends with Z)
        val withoutZ = isoString.removeSuffix("Z")
        val withoutSeparators = withoutZ.replace("-", "").replace(":", "").replace(".", "")
        return withoutSeparators + "Z"
    }

    private fun generateUID(): String {
        return "wakeve-${kotlin.random.Random.nextLong()}@wakeve.com"
    }
}