package com.guyghost.wakeve.models

import kotlinx.serialization.Serializable

@Serializable
data class CalendarEvent(
    val id: String,
    val title: String,
    val description: String,
    val startTime: String, // ISO 8601
    val endTime: String, // ISO 8601
    val timezone: String,
    val location: String? = null,
    val attendees: List<String> = emptyList(), // emails
    val organizer: String, // email
    val eventId: String // reference to our Event
)

@Serializable
data class CalendarInvite(
    val eventId: String,
    val icsContent: String, // Full ICS string
    val generatedAt: String // ISO 8601
)

interface CalendarService {
    suspend fun addEventToCalendar(event: CalendarEvent): Result<String> // Returns event ID in calendar
    suspend fun generateICSInvite(event: CalendarEvent): CalendarInvite
    suspend fun updateCalendarEvent(calendarEventId: String, event: CalendarEvent): Result<Unit>
    suspend fun removeCalendarEvent(calendarEventId: String): Result<Unit>
}