package com.guyghost.wakeve.presentation.usecase

import com.guyghost.wakeve.models.UpdateMeetingRequest
import com.guyghost.wakeve.models.VirtualMeeting

/**
 * Interface for UpdateMeetingUseCase to enable testing
 */
interface IUpdateMeetingUseCase {
    suspend operator fun invoke(meetingId: String, request: UpdateMeetingRequest): Result<VirtualMeeting>
}