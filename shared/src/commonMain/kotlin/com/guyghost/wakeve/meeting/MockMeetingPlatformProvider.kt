package com.guyghost.wakeve.meeting

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration

/**
 * Provider de plateforme de réunion (mocké pour l'instant)
 */
class MockMeetingPlatformProvider : MeetingPlatformProvider {
    override suspend fun generateMeetingLink(
        platform: MeetingPlatform,
        title: String,
        description: String?,
        startTime: Instant,
        duration: Duration
    ): String {
        // Génère des liens mockés pour chaque plateforme
        return when (platform) {
            MeetingPlatform.ZOOM -> "https://zoom.us/j/${generateMeetingId()}"
            MeetingPlatform.GOOGLE_MEET -> "https://meet.google.com/${generateMeetingId()}"
            MeetingPlatform.FACETIME -> "facetime://${generateMeetingId()}"
        }
    }
    
    override fun getHostMeetingId(meetingLink: String): String {
        // Extrait l'ID depuis le lien
        return meetingLink.substringAfterLast("/")
    }
    
    override fun cancelMeeting(platform: MeetingPlatform, hostMeetingId: String) {
        // Simule l'annulation
        println("Cancelling meeting $hostMeetingId on $platform")
    }
    
    private fun generateMeetingId(): String {
        return "${Clock.System.now().toEpochMilliseconds()}"
    }
}