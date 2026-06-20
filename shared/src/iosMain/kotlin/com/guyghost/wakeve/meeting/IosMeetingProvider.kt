package com.guyghost.wakeve.meeting

import com.guyghost.wakeve.models.MeetingLinkResponse
import com.guyghost.wakeve.models.MeetingPlatform

/**
 * iOS meeting provider placeholder.
 *
 * Fails explicitly until Zoom, Google Meet, FaceTime creation and launch bridges are wired.
 */
class IosMeetingProvider : MeetingProvider {

    override suspend fun createMeeting(
        platform: MeetingPlatform,
        title: String,
        description: String?,
        scheduledFor: kotlinx.datetime.Instant,
        duration: kotlin.time.Duration,
        timezone: String,
        participantLimit: Int?,
        requirePassword: Boolean,
        waitingRoom: Boolean
    ): Result<MeetingLinkResponse> =
        NoConfiguredMeetingProvider.createMeeting(
            platform = platform,
            title = title,
            description = description,
            scheduledFor = scheduledFor,
            duration = duration,
            timezone = timezone,
            participantLimit = participantLimit,
            requirePassword = requirePassword,
            waitingRoom = waitingRoom
        )

    override fun isPlatformAvailable(platform: MeetingPlatform): Boolean =
        NoConfiguredMeetingProvider.isPlatformAvailable(platform)

    override fun getAppUrl(platform: MeetingPlatform): String? =
        NoConfiguredMeetingProvider.getAppUrl(platform)

    override fun launchMeeting(meetingUrl: String): Result<Unit> =
        NoConfiguredMeetingProvider.launchMeeting(meetingUrl)
}
