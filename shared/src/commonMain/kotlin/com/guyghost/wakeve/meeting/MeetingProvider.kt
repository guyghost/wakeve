package com.guyghost.wakeve.meeting

import com.guyghost.wakeve.models.*

/**
 * Platform-specific meeting provider interface
 * Implemented for Android and iOS separately
 */
expect class MeetingProvider {

    /**
     * Create a meeting on the specified platform
     */
    suspend fun createMeeting(
        platform: MeetingPlatform,
        title: String,
        description: String?,
        scheduledFor: kotlinx.datetime.Instant,
        duration: kotlin.time.Duration,
        timezone: String,
        participantLimit: Int?,
        requirePassword: Boolean,
        waitingRoom: Boolean
    ): Result<MeetingLinkResponse>

    /**
     * Check if platform is available on this device
     */
    fun isPlatformAvailable(platform: MeetingPlatform): Boolean

    /**
     * Get the app URL for a platform (e.g., zoommtg://)
     */
    fun getAppUrl(platform: MeetingPlatform): String?

    /**
     * Launch a meeting in the native app
     */
    fun launchMeeting(meetingUrl: String): Result<Unit>
}

/**
 * Meeting provider exception
 */
class MeetingProviderException(message: String, cause: Throwable? = null) : Exception(message, cause)

class PlatformNotSupportedException(platform: MeetingPlatform) :
    MeetingProviderException("Platform not supported: $platform")

class MeetingCreationFailedException(platform: MeetingPlatform, reason: String) :
    MeetingProviderException("Failed to create meeting on $platform: $reason")
