package com.guyghost.wakeve.presentation.usecase

import com.guyghost.wakeve.models.VirtualMeeting

/**
 * Interface for LoadMeetingsUseCase to enable testing
 */
interface ILoadMeetingsUseCase {
    suspend operator fun invoke(eventId: String): Result<List<VirtualMeeting>>
}