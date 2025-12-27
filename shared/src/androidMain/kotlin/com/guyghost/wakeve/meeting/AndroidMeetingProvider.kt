package com.guyghost.wakeve.meeting

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.guyghost.wakeve.models.*

/**
 * Android implementation of MeetingProvider
 */
actual class AndroidMeetingProvider(
    private val context: Context
) : MeetingProvider {

    actual override suspend fun createMeeting(
        platform: MeetingPlatform,
        title: String,
        description: String?,
        scheduledFor: kotlinx.datetime.Instant,
        duration: kotlin.time.Duration,
        timezone: String,
        participantLimit: Int?,
        requirePassword: Boolean,
        waitingRoom: Boolean
    ): Result<MeetingLinkResponse> = kotlinx.coroutines.runCatching {
        when (platform) {
            MeetingPlatform.ZOOM -> createZoomMeeting(
                title,
                description,
                scheduledFor,
                duration,
                timezone,
                participantLimit,
                requirePassword,
                waitingRoom
            )
            MeetingPlatform.GOOGLE_MEET -> createGoogleMeetMeeting(
                title,
                description,
                scheduledFor,
                duration,
                timezone
            )
            MeetingPlatform.FACETIME -> createFaceTimeMeeting(
                title,
                description,
                scheduledFor,
                duration
            )
            else -> throw PlatformNotSupportedException(platform)
        }
    }

    actual override fun isPlatformAvailable(platform: MeetingPlatform): Boolean {
        return when (platform) {
            MeetingPlatform.ZOOM, MeetingPlatform.GOOGLE_MEET -> true // Web-based, always available
            MeetingPlatform.FACETIME -> false // Not available on Android
            MeetingPlatform.TEAMS, MeetingPlatform.WEBEX -> false // Not implemented yet
        }
    }

    actual override fun getAppUrl(platform: MeetingPlatform): String? {
        return when (platform) {
            MeetingPlatform.ZOOM -> "zoommtg://"
            MeetingPlatform.GOOGLE_MEET -> null // Web only
            MeetingPlatform.FACETIME -> null // iOS only
            MeetingPlatform.TEAMS -> "msteams://"
            MeetingPlatform.WEBEX -> "ciscospark://"
        }
    }

    actual override fun launchMeeting(meetingUrl: String): Result<Unit> = kotlinx.coroutines.runCatching {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(meetingUrl)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    private fun createZoomMeeting(
        title: String,
        description: String?,
        scheduledFor: kotlinx.datetime.Instant,
        duration: kotlin.time.Duration,
        timezone: String,
        participantLimit: Int?,
        requirePassword: Boolean,
        waitingRoom: Boolean
    ): MeetingLinkResponse {
        val meetingId = generateZoomMeetingId()
        val password = if (requirePassword) generateRandomPassword(6) else null

        val meetingUrl = if (password != null) {
            "https://zoom.us/j/$meetingId?pwd=$password"
        } else {
            "https://zoom.us/j/$meetingId"
        }

        return MeetingLinkResponse(
            meetingId = meetingId,
            meetingUrl = meetingUrl,
            dialInNumber = "+33 1 23 45 67 89",
            password = password
        )
    }

    private fun createGoogleMeetMeeting(
        title: String,
        description: String?,
        scheduledFor: kotlinx.datetime.Instant,
        duration: kotlin.time.Duration,
        timezone: String
    ): MeetingLinkResponse {
        val meetCode = generateMeetCode()
        val meetingUrl = "https://meet.google.com/$meetCode"

        return MeetingLinkResponse(
            meetingId = meetCode,
            meetingUrl = meetingUrl,
            dialInNumber = null,
            password = null
        )
    }

    private fun createFaceTimeMeeting(
        title: String,
        description: String?,
        scheduledFor: kotlinx.datetime.Instant,
        duration: kotlin.time.Duration
    ): MeetingLinkResponse {
        // FaceTime is not available on Android
        throw PlatformNotSupportedException(MeetingPlatform.FACETIME)
    }

    private fun generateZoomMeetingId(): String {
        return (1..10).map { kotlin.random.Random.nextInt(0, 10) }.joinToString("")
    }

    private fun generateRandomPassword(length: Int): String {
        val chars = ('a'..'z') + ('A'..'Z') + ('0'..'9')
        return (1..length).map { chars.random() }.joinToString("")
    }

    private fun generateMeetCode(): String {
        val letters = "abcdefghijklmnopqrstuvwxyz-"
        return (1..10).map { letters.random() }.joinToString("")
            .substring(0, 3) +
                "-" +
                (1..4).map { letters.random() }.joinToString("")
    }
}
