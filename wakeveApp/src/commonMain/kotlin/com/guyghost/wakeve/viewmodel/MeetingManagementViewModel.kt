package com.guyghost.wakeve.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guyghost.wakeve.auth.AuthStateManager
import com.guyghost.wakeve.models.CreateMeetingRequest
import com.guyghost.wakeve.models.MeetingLinkResponse
import com.guyghost.wakeve.models.MeetingPlatform
import com.guyghost.wakeve.models.UpdateMeetingRequest
import com.guyghost.wakeve.models.VirtualMeeting
import com.guyghost.wakeve.presentation.state.MeetingManagementContract
import com.guyghost.wakeve.presentation.statemachine.MeetingServiceStateMachine
import com.guyghost.wakeve.presentation.usecase.CancelMeetingUseCase
import com.guyghost.wakeve.presentation.usecase.CreateMeetingUseCase
import com.guyghost.wakeve.presentation.usecase.GenerateMeetingLinkUseCase
import com.guyghost.wakeve.presentation.usecase.LoadMeetingsUseCase
import com.guyghost.wakeve.presentation.usecase.UpdateMeetingUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for Meeting Management on Android
 *
 * Wraps MeetingServiceStateMachine and exposes StateFlow properties
 * for easy consumption by Jetpack Compose UI.
 *
 * ## Features
 *
 * - Load meetings for an event
 * - Create new meetings (organizer only)
 * - Update and delete meetings (organizer only)
 * - Generate meeting links (Zoom, Google Meet, FaceTime)
 * - Select meetings for viewing details
 * - Handle side effects (navigation, toasts, sharing)
 *
 * ## Usage in Compose
 *
 * ```kotlin
 * @Composable
 * fun MeetingListScreen(
 *     viewModel: MeetingManagementViewModel = koinViewModel()
 * ) {
 *     val state by viewModel.state.collectAsStateWithLifecycle()
 *
 *     LaunchedEffect(Unit) {
 *         viewModel.sideEffect.collect { effect ->
 *             when (effect) {
 *                 is SideEffect.NavigateTo -> navigate(effect.route)
 *                 is SideEffect.ShowToast -> showToast(effect.message)
 *                 is SideEffect.NavigateBack -> navController.popBackStack()
 *                 else -> {} // Handle other side effects
 *             }
 *         }
 *     }
 *
 *     MeetingListContent(
 *         state = state,
 *         onDispatch = { viewModel.dispatch(it) }
 *     )
 * }
 * ```
 *
 * @see MeetingServiceStateMachine
 * @see MeetingManagementContract
 */
class MeetingManagementViewModel(
    private val loadMeetingsUseCase: LoadMeetingsUseCase,
    private val createMeetingUseCase: CreateMeetingUseCase,
    private val updateMeetingUseCase: UpdateMeetingUseCase,
    private val cancelMeetingUseCase: CancelMeetingUseCase,
    private val generateMeetingLinkUseCase: GenerateMeetingLinkUseCase,
    private val stateMachine: MeetingServiceStateMachine
) : ViewModel() {

    // ========================================================================
    // Side Effects Channel
    // ========================================================================

    private val _sideEffect = MutableStateFlow<MeetingManagementContract.SideEffect?>(null)
    val sideEffect: StateFlow<MeetingManagementContract.SideEffect?> = _sideEffect.asStateFlow()

    // ========================================================================
    // State
    // ========================================================================

    private val _state = MutableStateFlow(MeetingManagementContract.State())
    val state: StateFlow<MeetingManagementContract.State> = _state.asStateFlow()

    // ========================================================================
    // Initialization
    // ========================================================================

    init {
        // Initialize state from state machine
        viewModelScope.launch {
            stateMachine.state.collect { newState ->
                _state.value = newState
            }
        }

        // Collect side effects from state machine
        viewModelScope.launch {
            stateMachine.sideEffect.collect { effect ->
                _sideEffect.value = effect
            }
        }
    }

    // ========================================================================
    // Dispatch Method
    // ========================================================================

    /**
     * Dispatch an intent to the state machine
     *
     * @param intent The intent to dispatch
     */
    fun dispatch(intent: MeetingManagementContract.Intent) {
        stateMachine.dispatch(intent)
    }

    // ========================================================================
    // Initialize with Event ID
    // ========================================================================

    /**
     * Initialize with an event ID
     *
     * @param eventId The ID of the event to load meetings for
     */
    fun initialize(eventId: String) {
        dispatch(MeetingManagementContract.Intent.LoadMeetings(eventId))
    }

    // ========================================================================
    // Create Meeting
    // ========================================================================

    /**
     * Create a new meeting
     *
     * @param platform The meeting platform (Zoom, Google Meet, FaceTime, etc.)
     * @param title The meeting title
     * @param description The meeting description
     * @param scheduledFor The scheduled start time
     * @param duration The meeting duration
     * @param requirePassword Whether a password is required
     * @param waitingRoom Whether a waiting room is enabled
     */
    fun createMeeting(
        platform: MeetingPlatform,
        title: String,
        description: String?,
        scheduledFor: kotlinx.datetime.Instant,
        duration: kotlin.time.Duration,
        timezone: String = "UTC",
        requirePassword: Boolean = true,
        waitingRoom: Boolean = true
    ) {
        val request = CreateMeetingRequest(
            eventId = _state.value.eventId,
            organizerId = getCurrentUserId(),
            platform = platform,
            title = title,
            description = description,
            scheduledFor = scheduledFor,
            duration = duration,
            timezone = timezone,
            requirePassword = requirePassword,
            waitingRoom = waitingRoom
        )

        dispatch(MeetingManagementContract.Intent.CreateMeeting(request))
    }

    // ========================================================================
    // Update Meeting
    // ========================================================================

    /**
     * Update an existing meeting
     *
     * @param meetingId The ID of the meeting to update
     * @param title The new title
     * @param description The new description
     * @param scheduledFor The new scheduled start time
     * @param duration The new duration
     */
    fun updateMeeting(
        meetingId: String,
        title: String,
        description: String?,
        scheduledFor: kotlinx.datetime.Instant,
        duration: kotlin.time.Duration
    ) {
        val request = UpdateMeetingRequest(
            title = title,
            description = description,
            scheduledFor = scheduledFor,
            duration = duration
        )

        dispatch(MeetingManagementContract.Intent.UpdateMeeting(meetingId, request))
    }

    // ========================================================================
    // Cancel Meeting
    // ========================================================================

    /**
     * Cancel a meeting
     *
     * @param meetingId The ID of the meeting to cancel
     */
    fun cancelMeeting(meetingId: String) {
        dispatch(MeetingManagementContract.Intent.CancelMeeting(meetingId))
    }

    // ========================================================================
    // Generate Meeting Link
    // ========================================================================

    /**
     * Generate a meeting link for a specific platform
     *
     * @param meetingId The ID of the meeting to generate link for
     * @param platform The platform to generate link for
     */
    fun generateMeetingLink(meetingId: String, platform: MeetingPlatform) {
        dispatch(MeetingManagementContract.Intent.GenerateMeetingLink(meetingId, platform))
    }

    // ========================================================================
    // Clear Generated Link
    // ========================================================================

    /**
     * Clear the generated meeting link
     */
    fun clearGeneratedLink() {
        dispatch(MeetingManagementContract.Intent.ClearGeneratedLink)
    }

    // ========================================================================
    // Clear Error
    // ========================================================================

    /**
     * Clear any error state
     */
    fun clearError() {
        dispatch(MeetingManagementContract.Intent.ClearError)
    }

    // ========================================================================
    // Convenience Properties
    // ========================================================================

    /**
     * List of meetings
     */
    val meetings: List<VirtualMeeting>
        get() = _state.value.meetings

    /**
     * Currently selected meeting
     */
    val selectedMeeting: VirtualMeeting?
        get() = _state.value.selectedMeeting

    /**
     * Generated meeting link
     */
    val generatedLink: MeetingLinkResponse?
        get() = _state.value.generatedLink

    /**
     * Whether currently loading
     */
    val isLoading: Boolean
        get() = _state.value.isLoading

    /**
     * Whether there is an error
     */
    val hasError: Boolean
        get() = _state.value.hasError

    /**
     * Error message (if any)
     */
    val error: String?
        get() = _state.value.error

    /**
     * Whether meetings list is empty
     */
    val isEmpty: Boolean
        get() = _state.value.isEmpty

    /**
     * Whether there is a generated link
     */
    val hasGeneratedLink: Boolean
        get() = _state.value.hasGeneratedLink

    // ========================================================================
    // Private Helpers
    // ========================================================================

    /**
     * Get the current user ID from AuthStateManager
     */
    private suspend fun getCurrentUserIdAsync(): String {
        val authStateManager = AuthStateManager.getInstance()
        return authStateManager.getCurrentUserId() ?: "anonymous"
    }
    
    /**
     * Synchronous version using the auth state
     */
    private fun getCurrentUserId(): String {
        val authStateManager = AuthStateManager.getInstance()
        val authState = authStateManager.authState.value
        return if (authState is com.guyghost.wakeve.auth.AuthState.Authenticated) {
            authState.userId
        } else {
            "anonymous"
        }
    }
}
