package com.guyghost.wakeve.meeting

import com.guyghost.wakeve.models.MeetingPlatform
import kotlinx.datetime.Instant
import kotlin.time.Duration

/**
 * Google Meet provider placeholder.
 *
 * A real integration must create the meeting through Google Calendar/Meet APIs.
 */
class GoogleMeetPlatformProvider : MeetingPlatformProvider {

    override suspend fun generateMeetingLink(
        platform: MeetingPlatform,
        title: String,
        description: String?,
        startTime: Instant,
        duration: Duration
    ): String {
        error("Google Meet provider is not configured")
    }

    override fun getHostMeetingId(meetingLink: String): String {
        return meetingLink.substringAfterLast("/")
    }

    override fun cancelMeeting(
        platform: MeetingPlatform,
        hostMeetingId: String
    ) {
        error("Google Meet provider is not configured")
    }
}
