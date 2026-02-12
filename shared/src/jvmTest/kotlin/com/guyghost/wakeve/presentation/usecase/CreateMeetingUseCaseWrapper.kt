package com.guyghost.wakeve.presentation.usecase

import com.guyghost.wakeve.models.CreateMeetingRequest
import com.guyghost.wakeve.models.VirtualMeeting

/**
 * Interface for CreateMeetingUseCase to enable testing
 */
interface ICreateMeetingUseCase {
    suspend operator fun invoke(request: CreateMeetingRequest): Result<VirtualMeeting>
}

/**
 * Wrapper class to adapt CreateMeetingUseCase for testing
 */
class CreateMeetingUseCaseWrapper(
    private val mockService: MockMeetingService,
    private val mockRepo: com.guyghost.wakeve.repository.MockMeetingRepository
) : ICreateMeetingUseCase {
    
    override suspend operator fun invoke(request: CreateMeetingRequest): Result<VirtualMeeting> {
        return try {
            // Create meeting using the mock service
            val virtualMeeting = mockService.createMeeting(
                eventId = request.eventId,
                organizerId = request.organizerId,
                platform = request.platform,
                title = request.title,
                description = request.description,
                scheduledFor = request.scheduledFor,
                duration = request.duration,
                timezone = request.timezone,
                participantLimit = request.participantLimit,
                requirePassword = request.requirePassword,
                waitingRoom = request.waitingRoom
            ).getOrThrow()

            // Store meeting in mock repository
            val meeting = com.guyghost.wakeve.meeting.Meeting(
                id = virtualMeeting.id,
                eventId = virtualMeeting.eventId,
                organizerId = virtualMeeting.organizerId,
                title = virtualMeeting.title,
                description = virtualMeeting.description,
                startTime = virtualMeeting.scheduledFor,
                duration = virtualMeeting.duration,
                platform = virtualMeeting.platform,
                meetingLink = virtualMeeting.meetingUrl,
                hostMeetingId = virtualMeeting.meetingId,
                password = virtualMeeting.meetingPassword ?: "",
                invitedParticipants = emptyList(), // Simplified for testing
                status = com.guyghost.wakeve.meeting.MeetingStatus.valueOf(virtualMeeting.status.name),
                createdAt = virtualMeeting.createdAt.toString()
            )
            
            mockRepo.createMeeting(meeting)
            Result.success(virtualMeeting)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}