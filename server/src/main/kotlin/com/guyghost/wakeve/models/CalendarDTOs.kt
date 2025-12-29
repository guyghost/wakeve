package com.guyghost.wakeve.models

import kotlinx.serialization.Serializable

@Serializable
data class ICSInvitationRequest(
    val invitees: List<String>
)

@Serializable
data class ICSInvitationResponse(
    val content: String,
    val filename: String
)

@Serializable
data class NativeCalendarRequest(
    val participantId: String
)

@Serializable
data class NativeCalendarResponse(
    val success: Boolean,
    val calendarEventId: String?
)

@Serializable
data class UpdateNativeCalendarRequest(
    val title: String? = null,
    val description: String? = null,
    val location: String? = null
)

@Serializable
data class CalendarReminderRequest(
    val timing: MeetingReminderTiming
)

@Serializable
data class CalendarReminderResponse(
    val success: Boolean,
    val remindersScheduled: Int
)
