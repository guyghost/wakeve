package com.guyghost.wakeve.presentation.usecase

import com.guyghost.wakeve.meeting.MeetingPlatformProvider
import com.guyghost.wakeve.meeting.MeetingRepository
import com.guyghost.wakeve.models.MeetingLinkResponse
import com.guyghost.wakeve.models.MeetingPlatform

/**
 * Use case for generating a meeting link for a specific platform
 *
 * This use case handles generating a meeting link using a platform provider
 * for platforms like Zoom, Google Meet, FaceTime, etc.
 *
 * ## Usage
 *
 * ```kotlin
 * val generateMeetingLinkUseCase = GenerateMeetingLinkUseCase(
 *     platformProvider = ZoomMeetingPlatformProvider(),
 *     repository = MeetingRepository(...)
 * )
 *
 * generateMeetingLinkUseCase(meetingId, platform).fold(
 *     onSuccess = { linkResponse ->
 *         // Show link or share it
 *     },
 *     onFailure = { error ->
 *         // Show error message
 *     }
 * )
 * ```
 *
 * @property platformProvider The provider for meeting platform operations
 * @property repository The repository for meeting data access
 */
class GenerateMeetingLinkUseCase(
    private val platformProvider: MeetingPlatformProvider,
    private val repository: MeetingRepository
) {
    /**
     * Execute use case to generate a meeting link
     *
     * @param meetingId The ID of the meeting to generate link for
     * @param platform The platform to generate link for
     * @return Result containing MeetingLinkResponse or error
     */
    suspend operator fun invoke(meetingId: String, platform: MeetingPlatform): Result<MeetingLinkResponse> {
        return try {
            // Get meeting from repository
            val meeting = repository.getMeetingById(meetingId)
                ?: return Result.failure(Exception("Meeting not found: $meetingId"))

            // Generate meeting link using platform provider
            val meetingLink = platformProvider.generateMeetingLink(
                platform = platform,
                title = meeting.title,
                description = meeting.description,
                startTime = meeting.startTime,
                duration = meeting.duration
            )

            // Get host meeting ID
            val hostMeetingId = platformProvider.getHostMeetingId(meetingLink)

            // Create response
            val linkResponse = MeetingLinkResponse(
                meetingId = hostMeetingId,
                meetingUrl = meetingLink,
                dialInNumber = null,
                password = meeting.password
            )

            Result.success(linkResponse)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
