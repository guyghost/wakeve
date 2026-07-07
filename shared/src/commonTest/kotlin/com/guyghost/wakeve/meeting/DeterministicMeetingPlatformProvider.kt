package com.guyghost.wakeve.meeting

import com.guyghost.wakeve.models.MeetingPlatform
import kotlinx.datetime.Instant
import kotlin.time.Duration

class DeterministicMeetingPlatformProvider : MeetingPlatformProvider {
    override suspend fun generateMeetingLink(
        platform: MeetingPlatform,
        title: String,
        description: String?,
        startTime: Instant,
        duration: Duration
    ): String {
        return when (platform) {
            MeetingPlatform.ZOOM -> "https://zoom.us/j/1234567890"
            MeetingPlatform.GOOGLE_MEET -> "https://meet.google.com/abc-def-ghij"
            MeetingPlatform.FACETIME -> "facetime://deterministic"
            MeetingPlatform.TEAMS -> "https://teams.microsoft.com/l/meetup-join/deterministic"
            MeetingPlatform.WEBEX -> "https://webex.com/j/deterministic"
        }
    }

    override fun getHostMeetingId(meetingLink: String): String {
        return meetingLink.substringAfterLast("/")
    }

    override fun cancelMeeting(platform: MeetingPlatform, hostMeetingId: String) = Unit
}
