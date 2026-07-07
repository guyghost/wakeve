package com.guyghost.wakeve.meeting

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.guyghost.wakeve.models.MeetingLinkResponse
import com.guyghost.wakeve.models.MeetingPlatform

/**
 * Android implementation of MeetingProvider.
 *
 * Android can launch existing meeting URLs through intents. Creating meetings on
 * external platforms still requires configured provider APIs.
 */
class AndroidMeetingProvider(
    private val context: Context
) : MeetingProvider {

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

    override fun isPlatformAvailable(platform: MeetingPlatform): Boolean {
        return false
    }

    override fun getAppUrl(platform: MeetingPlatform): String? {
        return when (platform) {
            MeetingPlatform.ZOOM -> "zoommtg://"
            MeetingPlatform.GOOGLE_MEET -> null // Web only
            MeetingPlatform.FACETIME -> null // iOS only
            MeetingPlatform.TEAMS -> "msteams://"
            MeetingPlatform.WEBEX -> "ciscospark://"
        }
    }

    override fun launchMeeting(meetingUrl: String): Result<Unit> = runCatching {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(meetingUrl)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }
}
