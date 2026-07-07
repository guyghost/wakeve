package com.guyghost.wakeve.meeting

import com.guyghost.wakeve.models.MeetingPlatform
import kotlinx.datetime.Instant
import kotlin.time.Duration

/**
 * Zoom provider placeholder.
 *
 * A real Zoom integration must call Zoom APIs and return provider-created meeting metadata.
 */
class ZoomMeetingPlatformProvider : MeetingPlatformProvider {

    override suspend fun generateMeetingLink(
        platform: MeetingPlatform,
        title: String,
        description: String?,
        startTime: Instant,
        duration: Duration
    ): String {
        error("Zoom meeting provider is not configured")
    }

    override fun getHostMeetingId(meetingLink: String): String {
        return meetingLink
            .substringAfter("/j/")
            .substringBefore("?")
    }

    override fun cancelMeeting(
        platform: MeetingPlatform,
        hostMeetingId: String
    ) {
        error("Zoom meeting provider is not configured")
    }

    fun getDialInNumber(): String {
        error("Zoom meeting provider is not configured")
    }

    fun getDialInPassword(meetingId: String): String {
        error("Zoom meeting provider is not configured")
    }

    fun generateHostKey(): String {
        error("Zoom meeting provider is not configured")
    }
}
