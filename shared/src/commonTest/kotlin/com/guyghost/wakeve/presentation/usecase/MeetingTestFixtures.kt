package com.guyghost.wakeve.presentation.usecase

import com.guyghost.wakeve.meeting.Meeting
import com.guyghost.wakeve.meeting.MeetingStatus
import com.guyghost.wakeve.models.CreateMeetingRequest
import com.guyghost.wakeve.models.MeetingPlatform
import com.guyghost.wakeve.models.UpdateMeetingRequest
import com.guyghost.wakeve.models.VirtualMeeting
import kotlinx.datetime.Clock
import kotlin.time.Duration.Companion.minutes

/**
 * Test fixtures for meeting-related tests
 */
object MeetingTestFixtures {
    
    fun createSampleMeeting(
        id: String = "meeting-1",
        eventId: String = "event-1",
        organizerId: String = "user-1",
        title: String = "Test Meeting",
        description: String? = "Test Description",
        startTime: kotlinx.datetime.Instant = Clock.System.now(),
        duration: kotlin.time.Duration = 60.minutes,
        platform: MeetingPlatform = MeetingPlatform.ZOOM,
        meetingLink: String = "https://zoom.us/j/123456789",
        hostMeetingId: String = "123456789",
        password: String = "abc123",
        invitedParticipants: List<String> = listOf("user-1", "user-2"),
        status: MeetingStatus = MeetingStatus.SCHEDULED,
        createdAt: String = Clock.System.now().toString()
    ) = Meeting(
        id = id,
        eventId = eventId,
        organizerId = organizerId,
        title = title,
        description = description,
        startTime = startTime,
        duration = duration,
        platform = platform,
        meetingLink = meetingLink,
        hostMeetingId = hostMeetingId,
        password = password,
        invitedParticipants = invitedParticipants,
        status = status,
        createdAt = createdAt
    )
    
    fun createSampleMeetings(count: Int = 3): List<Meeting> {
        return (1..count).map { index ->
            createSampleMeeting(
                id = "meeting-$index",
                eventId = "event-${index % 2 + 1}", // Alternate between event-1 and event-2
                title = "Test Meeting $index"
            )
        }
    }
    
    fun createSampleVirtualMeeting(
        id: String = "meeting-1",
        eventId: String = "event-1",
        organizerId: String = "user-1",
        platform: MeetingPlatform = MeetingPlatform.ZOOM,
        meetingId: String = "123456789",
        meetingPassword: String? = "abc123",
        meetingUrl: String = "https://zoom.us/j/123456789",
        dialInNumber: String? = "+33 1 23 45 67 89",
        dialInPassword: String? = null,
        title: String = "Test Meeting",
        description: String? = "Test Description",
        scheduledFor: kotlinx.datetime.Instant = Clock.System.now(),
        duration: kotlin.time.Duration = 60.minutes,
        timezone: String = "UTC",
        participantLimit: Int? = null,
        requirePassword: Boolean = true,
        waitingRoom: Boolean = true,
        hostKey: String? = null,
        createdAt: kotlinx.datetime.Instant = Clock.System.now(),
        status: com.guyghost.wakeve.models.MeetingStatus = com.guyghost.wakeve.models.MeetingStatus.SCHEDULED
    ) = VirtualMeeting(
        id = id,
        eventId = eventId,
        organizerId = organizerId,
        platform = platform,
        meetingId = meetingId,
        meetingPassword = meetingPassword,
        meetingUrl = meetingUrl,
        dialInNumber = dialInNumber,
        dialInPassword = dialInPassword,
        title = title,
        description = description,
        scheduledFor = scheduledFor,
        duration = duration,
        timezone = timezone,
        participantLimit = participantLimit,
        requirePassword = requirePassword,
        waitingRoom = waitingRoom,
        hostKey = hostKey,
        createdAt = createdAt,
        status = status
    )
    
    fun createSampleCreateMeetingRequest(
        eventId: String = "event-1",
        organizerId: String = "user-1",
        platform: MeetingPlatform = MeetingPlatform.ZOOM,
        title: String = "Test Meeting",
        description: String? = "Test Description",
        scheduledFor: kotlinx.datetime.Instant = Clock.System.now(),
        duration: kotlin.time.Duration = 60.minutes,
        timezone: String = "UTC",
        participantLimit: Int? = null,
        requirePassword: Boolean = true,
        waitingRoom: Boolean = true
    ) = CreateMeetingRequest(
        eventId = eventId,
        organizerId = organizerId,
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
    
    fun createSampleUpdateMeetingRequest(
        title: String? = "Updated Meeting",
        description: String? = "Updated Description",
        scheduledFor: kotlinx.datetime.Instant? = Clock.System.now().plus(60.minutes),
        duration: kotlin.time.Duration? = 90.minutes
    ) = UpdateMeetingRequest(
        title = title,
        description = description,
        scheduledFor = scheduledFor,
        duration = duration
    )
}