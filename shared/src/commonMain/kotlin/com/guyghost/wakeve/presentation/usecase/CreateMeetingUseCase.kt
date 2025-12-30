package com.guyghost.wakeve.presentation.usecase

import com.guyghost.wakeve.models.CreateMeetingRequest
import com.guyghost.wakeve.models.VirtualMeeting
import com.guyghost.wakeve.models.MeetingStatus
import com.guyghost.wakeve.meeting.MeetingService
import com.guyghost.wakeve.meeting.MeetingPlatformProvider
import com.guyghost.wakeve.meeting.MeetingRepository
import kotlinx.datetime.Clock

/**
 * Use case for creating a new virtual meeting
 *
 * This use case orchestrates the creation of a meeting by:
 * 1. Validating the event is in a valid state
 * 2. Getting validated participants
 * 3. Generating meeting details via platform provider
 * 4. Creating the meeting record in the repository
 *
 * ## Usage
 *
 * ```kotlin
 * val createMeetingUseCase = CreateMeetingUseCase(
 *     meetingService = MeetingService(...),
 *     meetingRepository = MeetingRepository(...)
 * )
 *
 * createMeetingUseCase(request).fold(
 *     onSuccess = { meeting ->
 *         // Navigate to meeting detail
 *     },
 *     onFailure = { error ->
 *         // Show error message
 *     }
 * )
 * ```
 *
 * @property meetingService The service for meeting operations
 * @property repository The repository for meeting data access
 */
class CreateMeetingUseCase(
    private val meetingService: MeetingService,
    private val repository: MeetingRepository
) {
    /**
     * Execute the use case to create a meeting
     *
     * @param request The meeting creation request
     * @return Result containing the created VirtualMeeting or error
     */
    suspend operator fun invoke(request: CreateMeetingRequest): Result<VirtualMeeting> {
        return try {
            // Create meeting using the existing service
            val virtualMeeting = meetingService.createMeeting(
                eventId = request.eventId,
                organizerId = request.organizerId,
                title = request.title,
                description = request.description,
                scheduledFor = request.scheduledFor,
                duration = request.duration,
                timezone = request.timezone,
                platform = request.platform,
                participantLimit = request.participantLimit,
                requirePassword = request.requirePassword,
                waitingRoom = request.waitingRoom
            ).getOrThrow()

            Result.success(virtualMeeting)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
