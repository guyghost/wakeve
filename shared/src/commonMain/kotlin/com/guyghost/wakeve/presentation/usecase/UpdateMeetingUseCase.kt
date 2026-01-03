package com.guyghost.wakeve.presentation.usecase

import com.guyghost.wakeve.meeting.MeetingRepository
import com.guyghost.wakeve.meeting.MeetingUpdates
import com.guyghost.wakeve.models.UpdateMeetingRequest
import com.guyghost.wakeve.models.VirtualMeeting
import kotlinx.datetime.Clock

/**
 * Use case for updating an existing meeting
 *
 * This use case handles updating meeting details such as:
 * - Title
 * - Description
 * - Start time
 * - Duration
 * - Platform
 * - Meeting link
 *
 * ## Usage
 *
 * ```kotlin
 * val updateMeetingUseCase = UpdateMeetingUseCase(repository)
 *
 * updateMeetingUseCase(meetingId, request).fold(
 *     onSuccess = { meeting ->
 *         // Show success toast
 *     },
 *     onFailure = { error ->
 *         // Show error message
 *     }
 * )
 * ```
 *
 * @property repository The repository for meeting data access
 */
class UpdateMeetingUseCase(
    private val repository: MeetingRepository
) {
    /**
     * Execute use case to update a meeting
     *
     * @param meetingId The ID of the meeting to update
     * @param request The update request with fields to change
     * @return Result containing updated VirtualMeeting or error
     */
    suspend operator fun invoke(meetingId: String, request: UpdateMeetingRequest): Result<VirtualMeeting> {
        return try {
            val meeting = repository.getMeetingById(meetingId)
                ?: return Result.failure(Exception("Meeting not found: $meetingId"))

            // Create updates map
            val updates = MeetingUpdates(
                title = request.title,
                description = request.description,
                startTime = request.scheduledFor,
                duration = request.duration,
                platform = null, // Platform updates not allowed
                meetingLink = null // Link updates not allowed
            )

            // Update meeting in repository
            repository.updateMeeting(meetingId, updates).getOrThrow()

            // Get updated meeting
            val updatedMeeting = repository.getMeetingById(meetingId)!!

            // Convert to VirtualMeeting model
            val virtualMeeting = VirtualMeeting(
                id = updatedMeeting.id,
                eventId = updatedMeeting.eventId,
                organizerId = updatedMeeting.organizerId,
                platform = updatedMeeting.platform,
                meetingId = updatedMeeting.hostMeetingId,
                meetingPassword = updatedMeeting.password,
                meetingUrl = updatedMeeting.meetingLink,
                dialInNumber = null,
                dialInPassword = null,
                title = updatedMeeting.title,
                description = updatedMeeting.description,
                scheduledFor = updatedMeeting.startTime,
                duration = updatedMeeting.duration,
                timezone = "UTC",
                participantLimit = null,
                requirePassword = updatedMeeting.password.isNotEmpty(),
                waitingRoom = true,
                hostKey = null,
                createdAt = Clock.System.now(),
                status = com.guyghost.wakeve.models.MeetingStatus.valueOf(updatedMeeting.status.name)
            )

            Result.success(virtualMeeting)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
