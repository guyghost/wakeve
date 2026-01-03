package com.guyghost.wakeve.meeting

import com.guyghost.wakeve.models.MeetingPlatform
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlin.time.Duration

/**
 * Réunion
 */
@Serializable
data class Meeting(
    val id: String,
    val eventId: String,
    val organizerId: String,
    val title: String,
    val description: String?,
    val startTime: Instant,
    val duration: Duration,
    val platform: MeetingPlatform,
    val meetingLink: String,
    val hostMeetingId: String,
    val password: String,
    val invitedParticipants: List<String>,
    val status: MeetingStatus = MeetingStatus.SCHEDULED,
    val createdAt: String
)