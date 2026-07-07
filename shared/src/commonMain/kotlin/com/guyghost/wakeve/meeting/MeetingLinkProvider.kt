package com.guyghost.wakeve.meeting

import com.guyghost.wakeve.models.MeetingLinkResponse
import com.guyghost.wakeve.models.MeetingPlatform
import kotlinx.datetime.Instant
import kotlin.time.Duration

data class MeetingDetails(
    val url: String,
    val hostMeetingId: String,
    val password: String?,
    val dialInNumber: String?,
    val participantLimit: Int?,
    val requirePassword: Boolean,
    val waitingRoom: Boolean,
    val hostKey: String?
)

interface MeetingLinkProvider {
    suspend fun createMeeting(
        platform: MeetingPlatform,
        title: String,
        description: String?,
        scheduledFor: Instant,
        duration: Duration,
        timezone: String,
        participantLimit: Int?,
        requirePassword: Boolean,
        waitingRoom: Boolean
    ): MeetingDetails

    suspend fun generateMeetingLink(
        platform: MeetingPlatform,
        title: String,
        description: String?,
        scheduledFor: Instant,
        duration: Duration
    ): MeetingLinkResponse

    fun cancelMeeting(platform: MeetingPlatform, hostMeetingId: String): Result<Unit>
}

object NoConfiguredMeetingLinkProvider : MeetingLinkProvider {
    override suspend fun createMeeting(
        platform: MeetingPlatform,
        title: String,
        description: String?,
        scheduledFor: Instant,
        duration: Duration,
        timezone: String,
        participantLimit: Int?,
        requirePassword: Boolean,
        waitingRoom: Boolean
    ): MeetingDetails {
        error("Meeting link provider is not configured for $platform")
    }

    override suspend fun generateMeetingLink(
        platform: MeetingPlatform,
        title: String,
        description: String?,
        scheduledFor: Instant,
        duration: Duration
    ): MeetingLinkResponse {
        error("Meeting link provider is not configured for $platform")
    }

    override fun cancelMeeting(platform: MeetingPlatform, hostMeetingId: String): Result<Unit> =
        Result.failure(IllegalStateException("Meeting link provider is not configured for $platform"))
}
