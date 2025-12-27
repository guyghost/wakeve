package com.guyghost.wakeve.models

import kotlinx.datetime.Instant
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

// New models for enhanced calendar service
@Serializable
data class ICSDocument(
    val content: String,
    val filename: String
)

@Serializable
data class EnhancedCalendarEvent(
    val id: String,
    val title: String,
    val description: String?,
    val location: String,
    val startDate: Instant,
    val endDate: Instant,
    val attendees: List<String>,
    val organizer: String
)

interface CalendarService {
    suspend fun addEventToCalendar(event: CalendarEvent): Result<String> // Returns event ID in calendar
    suspend fun generateICSInvite(event: CalendarEvent): CalendarInvite
    suspend fun updateCalendarEvent(calendarEventId: String, event: CalendarEvent): Result<Unit>
    suspend fun removeCalendarEvent(calendarEventId: String): Result<Unit>
}

// Enhanced interface for the new calendar service
interface EnhancedCalendarService {
    suspend fun generateICSInvitation(eventId: String, invitees: List<String>): ICSDocument
    suspend fun addToNativeCalendar(eventId: String, participantId: String): Result<Unit>
    suspend fun updateNativeCalendarEvent(eventId: String, participantId: String): Result<Unit>
    suspend fun removeFromNativeCalendar(eventId: String, participantId: String): Result<Unit>
}