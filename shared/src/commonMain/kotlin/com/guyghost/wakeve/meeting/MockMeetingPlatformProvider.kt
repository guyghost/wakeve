package com.guyghost.wakeve.meeting

import com.guyghost.wakeve.models.MeetingPlatform
import kotlinx.datetime.Instant
import kotlin.time.Duration

/**
 * Legacy provider kept for source compatibility. Production code must inject a real provider.
 */
@Deprecated(
    message = "Inject a configured provider in production or a deterministic provider in tests.",
    replaceWith = ReplaceWith("UnavailableMeetingPlatformProvider()")
)
class MockMeetingPlatformProvider : MeetingPlatformProvider {
    override suspend fun generateMeetingLink(
        platform: MeetingPlatform,
        title: String,
        description: String?,
        startTime: Instant,
        duration: Duration
    ): String {
        error("Meeting link provider is not configured for $platform")
    }
    
    override fun getHostMeetingId(meetingLink: String): String {
        error("Meeting link provider is not configured")
    }
    
    override fun cancelMeeting(platform: MeetingPlatform, hostMeetingId: String) {
        error("Meeting link provider is not configured for $platform")
    }
}

/**
 * Production-safe provider used when no real meeting platform integration is configured.
 */
class UnavailableMeetingPlatformProvider : MeetingPlatformProvider {
    override suspend fun generateMeetingLink(
        platform: MeetingPlatform,
        title: String,
        description: String?,
        startTime: Instant,
        duration: Duration
    ): String {
        error("Meeting link provider is not configured for $platform")
    }

    override fun getHostMeetingId(meetingLink: String): String {
        error("Meeting link provider is not configured")
    }

    override fun cancelMeeting(platform: MeetingPlatform, hostMeetingId: String) {
        error("Meeting link provider is not configured for $platform")
    }
}
