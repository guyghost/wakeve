package com.guyghost.wakeve.presentation.usecase

import com.guyghost.wakeve.models.MeetingPlatform
import kotlinx.datetime.Instant
import kotlin.time.Duration

/**
 * Mock implementation of MeetingService for testing
 */
class MockMeetingService {
    
    companion object {
        private var idCounter = 0L
        
        fun generateUniqueId(): String {
            return "meeting-${++idCounter}-${System.currentTimeMillis()}"
        }
    }
    
    var shouldFailCreate = false
    var createdMeetings = mutableListOf<com.guyghost.wakeve.models.VirtualMeeting>()
    
    suspend fun createMeeting(
        eventId: String,
        organizerId: String,
        platform: MeetingPlatform,
        title: String,
        description: String?,
        scheduledFor: Instant,
        duration: Duration,
        timezone: String,
        participantLimit: Int? = null,
        requirePassword: Boolean = true,
        waitingRoom: Boolean = true
    ): Result<com.guyghost.wakeve.models.VirtualMeeting> {
        return if (shouldFailCreate) {
            Result.failure(Exception("Service error: Failed to create meeting"))
        } else {
            val meeting = com.guyghost.wakeve.models.VirtualMeeting(
                id = generateUniqueId(),
                eventId = eventId,
                organizerId = organizerId,
                platform = platform,
                meetingId = generateMeetingId(platform),
                meetingPassword = if (requirePassword) generatePassword() else null,
                meetingUrl = generateMeetingUrl(platform),
                dialInNumber = if (platform == MeetingPlatform.ZOOM) "+33 1 23 45 67 89" else null,
                dialInPassword = null,
                title = title,
                description = description,
                scheduledFor = scheduledFor,
                duration = duration,
                timezone = timezone,
                participantLimit = participantLimit,
                requirePassword = requirePassword,
                waitingRoom = waitingRoom,
                hostKey = if (platform == MeetingPlatform.ZOOM) "host-${generateUniqueId()}" else null,
                createdAt = kotlinx.datetime.Clock.System.now(),
                status = com.guyghost.wakeve.models.MeetingStatus.SCHEDULED
            )
            
            createdMeetings.add(meeting)
            Result.success(meeting)
        }
    }
    
    private fun generateMeetingId(platform: MeetingPlatform): String {
        return when (platform) {
            MeetingPlatform.ZOOM -> (1..10).map { kotlin.random.Random.nextInt(0, 10) }.joinToString("")
            MeetingPlatform.GOOGLE_MEET -> {
                val chars = "abcdefghijklmnopqrstuvwxyz"
                (1..3).map { chars.random() }.joinToString("") + "-" +
                (1..3).map { chars.random() }.joinToString("") + "-" +
                (1..4).map { chars.random() }.joinToString("")
            }
            MeetingPlatform.FACETIME -> "user-${generateUniqueId()}"
            else -> "meeting-${System.currentTimeMillis()}"
        }
    }
    
    private fun generatePassword(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..6).map { chars.random() }.joinToString("")
    }
    
    private fun generateMeetingUrl(platform: MeetingPlatform): String {
        return when (platform) {
            MeetingPlatform.ZOOM -> "https://zoom.us/j/${generateMeetingId(platform)}"
            MeetingPlatform.GOOGLE_MEET -> "https://meet.google.com/${generateMeetingId(platform)}"
            MeetingPlatform.FACETIME -> "facetime://"
            else -> "https://example.com/meeting/${generateMeetingId(platform)}"
        }
    }
}