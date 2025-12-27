package com.guyghost.wakeve.models

import kotlinx.serialization.Serializable

/**
 * Platform for virtual meetings
 */
@Serializable
enum class MeetingPlatform {
    ZOOM,
    GOOGLE_MEET,
    FACETIME,
    TEAMS,
    WEBEX
}

/**
 * Status of a virtual meeting
 */
@Serializable
enum class MeetingStatus {
    SCHEDULED,
    STARTED,
    ENDED,
    CANCELLED
}

/**
 * Status of a meeting invitation
 */
@Serializable
enum class InvitationStatus {
    PENDING,
    ACCEPTED,
    DECLINED,
    TENTATIVE
}

/**
 * Timing for meeting reminders
 */
@Serializable
enum class MeetingReminderTiming {
    ONE_DAY_BEFORE,
    ONE_HOUR_BEFORE,
    FIFTEEN_MINUTES_BEFORE,
    FIVE_MINUTES_BEFORE
}

/**
 * Status of a reminder
 */
@Serializable
enum class ReminderStatus {
    SCHEDULED,
    SENT,
    FAILED
}

/**
 * Virtual meeting for event coordination
 */
@Serializable
data class VirtualMeeting(
    val id: String,
    val eventId: String,
    val organizerId: String,
    val platform: MeetingPlatform,
    val meetingId: String,
    val meetingPassword: String?,
    val meetingUrl: String,
    val dialInNumber: String?,
    val dialInPassword: String?,
    val title: String,
    val description: String?,
    val scheduledFor: kotlinx.datetime.Instant,
    val duration: kotlin.time.Duration,
    val timezone: String,
    val participantLimit: Int?,
    val requirePassword: Boolean,
    val waitingRoom: Boolean,
    val hostKey: String?,
    val createdAt: kotlinx.datetime.Instant,
    val status: MeetingStatus
)

/**
 * Meeting invitation for a participant
 */
@Serializable
data class MeetingInvitation(
    val id: String,
    val meetingId: String,
    val participantId: String,
    val status: InvitationStatus,
    val sentAt: kotlinx.datetime.Instant,
    val respondedAt: kotlinx.datetime.Instant?,
    val acceptedAt: kotlinx.datetime.Instant?
)

/**
 * Meeting reminder
 */
@Serializable
data class MeetingReminder(
    val id: String,
    val meetingId: String,
    val participantId: String?,
    val timing: MeetingReminderTiming,
    val scheduledFor: kotlinx.datetime.Instant,
    val sentAt: kotlinx.datetime.Instant?,
    val status: ReminderStatus
)

/**
 * Request to create a virtual meeting
 */
@Serializable
data class CreateMeetingRequest(
    val eventId: String,
    val organizerId: String,
    val platform: MeetingPlatform,
    val title: String,
    val description: String?,
    val scheduledFor: kotlinx.datetime.Instant,
    val duration: kotlin.time.Duration,
    val timezone: String,
    val participantLimit: Int? = null,
    val requirePassword: Boolean = true,
    val waitingRoom: Boolean = true
)

/**
 * Request to update a meeting
 */
@Serializable
data class UpdateMeetingRequest(
    val title: String?,
    val description: String?,
    val scheduledFor: kotlinx.datetime.Instant?,
    val duration: kotlin.time.Duration?
)

/**
 * Response with meeting link
 */
@Serializable
data class MeetingLinkResponse(
    val meetingId: String,
    val meetingUrl: String,
    val dialInNumber: String?,
    val password: String?
)
