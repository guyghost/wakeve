package com.guyghost.wakeve.meeting

import com.guyghost.wakeve.models.MeetingLinkResponse
import com.guyghost.wakeve.models.MeetingPlatform
import kotlinx.datetime.Instant
import kotlin.time.Duration

class DeterministicMeetingLinkProvider : MeetingLinkProvider {
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
        val response = generateMeetingLink(platform, title, description, scheduledFor, duration)
        return MeetingDetails(
            url = response.meetingUrl,
            hostMeetingId = response.meetingId,
            password = response.password,
            dialInNumber = response.dialInNumber,
            participantLimit = participantLimit,
            requirePassword = requirePassword,
            waitingRoom = waitingRoom,
            hostKey = if (platform == MeetingPlatform.ZOOM) "654321" else null
        )
    }

    override suspend fun generateMeetingLink(
        platform: MeetingPlatform,
        title: String,
        description: String?,
        scheduledFor: Instant,
        duration: Duration
    ): MeetingLinkResponse {
        return when (platform) {
            MeetingPlatform.ZOOM -> MeetingLinkResponse(
                meetingId = "1234567890",
                meetingUrl = "https://zoom.us/j/1234567890?pwd=ABC123",
                dialInNumber = "+33 1 23 45 67 89",
                password = "ABC123"
            )
            MeetingPlatform.GOOGLE_MEET -> MeetingLinkResponse(
                meetingId = "abc-def-ghij",
                meetingUrl = "https://meet.google.com/abc-def-ghij",
                dialInNumber = null,
                password = null
            )
            MeetingPlatform.FACETIME -> MeetingLinkResponse(
                meetingId = "facetime",
                meetingUrl = "facetime://",
                dialInNumber = null,
                password = null
            )
            MeetingPlatform.TEAMS,
            MeetingPlatform.WEBEX -> error("Platform $platform is not supported by the deterministic test provider")
        }
    }

    override fun cancelMeeting(platform: MeetingPlatform, hostMeetingId: String): Result<Unit> =
        Result.success(Unit)
}
