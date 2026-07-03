package com.guyghost.wakeve.meeting

import com.guyghost.wakeve.models.MeetingLinkResponse
import com.guyghost.wakeve.models.MeetingPlatform
import platform.Foundation.NSURL
import platform.UIKit.UIApplication

/**
 * iOS meeting provider.
 *
 * Creation still fails explicitly until provider credentials are configured, but stored safe
 * meeting URLs can be opened through native iOS URL handling.
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
        getAppUrl(platform)?.let { url ->
            NSURL.URLWithString(url)?.let { UIApplication.sharedApplication.canOpenURL(it) } == true
        } ?: false

    override fun getAppUrl(platform: MeetingPlatform): String? = when (platform) {
        MeetingPlatform.ZOOM -> "zoommtg://"
        MeetingPlatform.GOOGLE_MEET -> "googlemeet://"
        MeetingPlatform.FACETIME -> "facetime://"
        MeetingPlatform.TEAMS -> "msteams://"
        MeetingPlatform.WEBEX -> "wbx://"
    }

    override fun launchMeeting(meetingUrl: String): Result<Unit> = runCatching {
        val url = NSURL.URLWithString(meetingUrl)
            ?: throw MeetingProviderException("Invalid meeting URL")

        if (!isSafeMeetingUrl(url)) {
            throw MeetingProviderException("Unsafe meeting URL blocked")
        }

        val canOpen = UIApplication.sharedApplication.canOpenURL(url)
        if (!canOpen) {
            throw MeetingProviderException("No installed iOS handler for meeting URL")
        }

        val opened = UIApplication.sharedApplication.openURL(url)
        if (!opened) {
            throw MeetingProviderException("Unable to open meeting URL")
        }
    }

    private fun isSafeMeetingUrl(url: NSURL): Boolean {
        val scheme = url.scheme?.lowercase() ?: return false
        if (url.user != null || url.password != null || url.fragment != null) {
            return false
        }

        return when (scheme) {
            "https" -> true
            "zoommtg", "zoomus", "googlemeet", "facetime", "facetime-audio", "msteams", "wbx" -> true
            else -> false
        }
    }
}
