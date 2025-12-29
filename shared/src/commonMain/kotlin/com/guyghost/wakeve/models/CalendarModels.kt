package com.guyghost.wakeve.models

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

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

// Enhanced interface for the new calendar service
interface EnhancedCalendarService {
    suspend fun generateICSInvitation(eventId: String, invitees: List<String>): ICSDocument
    suspend fun addToNativeCalendar(eventId: String, participantId: String): Result<Unit>
    suspend fun updateNativeCalendarEvent(eventId: String, participantId: String): Result<Unit>
    suspend fun removeFromNativeCalendar(eventId: String, participantId: String): Result<Unit>
}
