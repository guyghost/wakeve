package com.guyghost.wakeve.presentation.usecase

import com.guyghost.wakeve.meeting.MeetingRepository
import com.guyghost.wakeve.models.EventStatus

/**
 * Use case for canceling a meeting
 *
 * This use case handles:
 * - Validating the user is the organizer
 * - Canceling the meeting on the platform
 * - Updating the meeting status to CANCELLED
 *
 * ## Usage
 *
 * ```kotlin
 * val cancelMeetingUseCase = CancelMeetingUseCase(
 *     meetingService = MeetingService(...),
 *     repository = MeetingRepository(...)
 * )
 *
 * cancelMeetingUseCase(meetingId, currentUserId).fold(
 *     onSuccess = { ->
 *         // Navigate back with toast
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
class CancelMeetingUseCase(
    private val meetingService: com.guyghost.wakeve.meeting.MeetingService,
    private val repository: MeetingRepository
) {
    /**
     * Execute use case to cancel a meeting
     *
     * @param meetingId The ID of the meeting to cancel
     * @param organizerId The ID of the current actor (for authorization check)
     * @return Result indicating success or failure
     */
    suspend operator fun invoke(meetingId: String, organizerId: String): Result<Unit> {
        return try {
            val meeting = repository.getMeetingById(meetingId)
                ?: return Result.failure(Exception("Meeting $meetingId not found"))

            if (meeting.organizerId != organizerId) {
                return Result.failure(Exception("Unauthorized: only the meeting organizer can cancel this meeting"))
            }

            val eventStatus = repository.getEventStatusForMeeting(meetingId)
                ?: return Result.failure(Exception("Meeting event not found"))
            if (eventStatus != EventStatus.ORGANIZING) {
                return Result.failure(Exception("Cannot cancel meeting unless the event is ORGANIZING; current status is $eventStatus"))
            }

            // Cancel meeting using service
            meetingService.cancelMeeting(meetingId).getOrThrow()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
