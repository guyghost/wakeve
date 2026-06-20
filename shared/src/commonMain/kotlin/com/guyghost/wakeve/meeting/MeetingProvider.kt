package com.guyghost.wakeve.meeting

import com.guyghost.wakeve.models.MeetingLinkResponse
import com.guyghost.wakeve.models.MeetingPlatform

/**
 * Platform-specific meeting provider interface
 * Implemented for Android and iOS separately
 */
interface MeetingProvider {

    /**
     * Create a meeting on specified platform
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
     * Get app URL for a platform (e.g., zoommtg://)
     */
    fun getAppUrl(platform: MeetingPlatform): String?

    /**
     * Launch a meeting in the native app
     */
    fun launchMeeting(meetingUrl: String): Result<Unit>
}

/**
 * Meeting provider for builds where no external meeting platform integration has been wired.
 */
object NoConfiguredMeetingProvider : MeetingProvider {
    private fun notConfigured(platform: MeetingPlatform? = null): MeetingProviderException {
        val suffix = platform?.let { " for $it" }.orEmpty()
        return MeetingProviderException("Meeting provider is not configured$suffix")
    }

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
    ): Result<MeetingLinkResponse> = Result.failure(notConfigured(platform))

    override fun isPlatformAvailable(platform: MeetingPlatform): Boolean = false

    override fun getAppUrl(platform: MeetingPlatform): String? = null

    override fun launchMeeting(meetingUrl: String): Result<Unit> =
        Result.failure(notConfigured())
}

/**
 * Meeting provider exception
 */
open class MeetingProviderException(message: String, cause: Throwable? = null) : Exception(message, cause)

class PlatformNotSupportedException(platform: MeetingPlatform) :
    MeetingProviderException("Platform not supported: $platform")

class MeetingCreationFailedException(platform: MeetingPlatform, reason: String) :
    MeetingProviderException("Failed to create meeting on $platform: $reason")
