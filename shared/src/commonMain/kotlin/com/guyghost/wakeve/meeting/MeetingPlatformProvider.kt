package com.guyghost.wakeve.meeting

import kotlinx.datetime.Instant
import kotlin.time.Duration

/**
 * Provider de plateforme de réunion
 */
interface MeetingPlatformProvider {
    suspend fun generateMeetingLink(
        platform: MeetingPlatform,
        title: String,
        description: String?,
        startTime: Instant,
        duration: Duration
    ): String
    
    fun getHostMeetingId(meetingLink: String): String
    
    fun cancelMeeting(platform: MeetingPlatform, hostMeetingId: String)
}