package com.guyghost.wakeve.meeting

import com.guyghost.wakeve.models.MeetingPlatform
import kotlinx.datetime.Instant
import kotlin.time.Duration

/**
 * FaceTime provider placeholder.
 *
 * A real integration must be supplied by the native iOS layer.
 * Note: FaceTime is platform-specific (iOS only for group calls)
 */
class FaceTimePlatformProvider : MeetingPlatformProvider {

    companion object {
        private const val FACETIME_SCHEME = "facetime://"
    }

    override suspend fun generateMeetingLink(
        platform: MeetingPlatform,
        title: String,
        description: String?,
        startTime: Instant,
        duration: Duration
    ): String {
        error("FaceTime provider is not configured")
    }

    override fun getHostMeetingId(meetingLink: String): String {
        // For FaceTime, the meeting ID is the organizer's Apple ID
        // This will be provided by the iOS platform
        return meetingLink.removePrefix(FACETIME_SCHEME)
    }

    override fun cancelMeeting(
        platform: MeetingPlatform,
        hostMeetingId: String
    ) {
        error("FaceTime provider is not configured")
    }

    /**
     * Validate if an Apple ID is properly formatted
     */
    fun validateAppleId(appleId: String): Boolean {
        // Apple IDs should be an email address or phone number
        val emailRegex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
        val phoneRegex = Regex("^\\+?[0-9\\-\\s]+$")
        return emailRegex.matches(appleId) || phoneRegex.matches(appleId)
    }

    /**
     * Get FaceTime link for a specific Apple ID
     * Used for 1:1 FaceTime calls
     */
    fun getFaceTimeLink(appleId: String): String {
        return "$FACETIME_SCHEME$appleId"
    }
}
