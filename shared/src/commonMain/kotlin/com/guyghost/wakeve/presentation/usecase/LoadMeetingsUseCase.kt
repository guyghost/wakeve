package com.guyghost.wakeve.presentation.usecase

import com.guyghost.wakeve.meeting.MeetingRepository
import com.guyghost.wakeve.models.VirtualMeeting

/**
 * Use case for loading meetings for an event
 *
 * This use case is responsible for retrieving all virtual meetings
 * associated with a specific event from the repository.
 *
 * ## Usage
 *
 * ```kotlin
 * val loadMeetingsUseCase = LoadMeetingsUseCase(meetingRepository)
 *
 * loadMeetingsUseCase(eventId).fold(
 *     onSuccess = { meetings ->
 *         // Display meetings list
 *     },
 *     onFailure = { error ->
 *         // Handle error
 *     }
 * )
 * ```
 *
 * @property repository The repository for meeting data access
 */
class LoadMeetingsUseCase(
    private val repository: MeetingRepository
) {
    /**
     * Execute the use case to load meetings for an event
     *
     * @param eventId The ID of the event to load meetings for
     * @return Result containing list of VirtualMeeting or error
     */
    suspend operator fun invoke(eventId: String): Result<List<VirtualMeeting>> {
        return try {
            val meetings = repository.getMeetingsByEventId(eventId)

            // Convert to VirtualMeeting models
            val virtualMeetings = meetings.map { meeting ->
                VirtualMeeting(
                    id = meeting.id,
                    eventId = meeting.eventId,
                    organizerId = meeting.organizerId,
                    platform = meeting.platform,
                    meetingId = meeting.hostMeetingId,
                    meetingPassword = meeting.password,
                    meetingUrl = meeting.meetingLink,
                    dialInNumber = null,
                    dialInPassword = null,
                    title = meeting.title,
                    description = meeting.description,
                    scheduledFor = meeting.startTime,
                    duration = meeting.duration,
                    timezone = "UTC",
                    participantLimit = null,
                    requirePassword = meeting.password.isNotEmpty(),
                    waitingRoom = true,
                    hostKey = null,
                    createdAt = kotlinx.datetime.Clock.System.now(),
                    status = com.guyghost.wakeve.models.MeetingStatus.valueOf(meeting.status.name)
                )
            }

            Result.success(virtualMeetings)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
