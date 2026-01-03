package com.guyghost.wakeve.presentation.statemachine

import com.guyghost.wakeve.presentation.state.MeetingManagementContract
import com.guyghost.wakeve.presentation.state.MeetingManagementContract.Intent
import com.guyghost.wakeve.presentation.state.MeetingManagementContract.SideEffect
import com.guyghost.wakeve.presentation.usecase.CancelMeetingUseCase
import com.guyghost.wakeve.presentation.usecase.CreateMeetingUseCase
import com.guyghost.wakeve.presentation.usecase.GenerateMeetingLinkUseCase
import com.guyghost.wakeve.presentation.usecase.LoadMeetingsUseCase
import com.guyghost.wakeve.presentation.usecase.UpdateMeetingUseCase
import kotlinx.coroutines.CoroutineScope

/**
 * State Machine for managing virtual meetings in event planning workflow
 *
 * This state machine handles all meeting-related operations:
 * - Loading meetings for an event
 * - Creating new meetings
 * - Updating and deleting meetings
 * - Generating meeting links (Zoom, Google Meet, FaceTime, etc.)
 * - Managing meeting states (SCHEDULED, IN_PROGRESS, COMPLETED, CANCELLED)
 *
 * ## Architecture
 *
 * ```
 * Intent (user action)
 *   ↓
 * handleIntent()
 *   ↓
 * updateState() ← Updates the UI state
 * emitSideEffect() ← Triggers navigation/toasts
 * ```
 *
 * ## Usage Example (Android)
 *
 * ```kotlin
 * @Composable
 * fun MeetingListScreen(
 *     viewModel: MeetingManagementViewModel = koinViewModel()
 * ) {
 *     val state by viewModel.state.collectAsState()
 *
 *     LaunchedEffect(Unit) {
 *         viewModel.dispatch(
 *             Intent.LoadMeetings(eventId = "event-1")
 *         )
 *     }
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
 * ## Usage Example (iOS)
 *
 * ```swift
 * class MeetingListViewModel: ObservableObject {
 *     @Published var state: MeetingManagementContract.State
 *     private let stateMachine: StateMachine<...>
 *
 *     init(eventId: String) {
 *         stateMachine = factory.createMeetingStateMachine()
 *         state = stateMachine.currentState
 *
 *         stateMachine.onStateChange = { [weak self] in
 *             self?.state = $0
 *         }
 *
 *         dispatch(.loadMeetings(eventId: eventId))
 *     }
 *
 *     func dispatch(_ intent: MeetingManagementContract.Intent) {
 *         stateMachine.dispatch(intent: intent)
 *     }
 * }
 * ```
 *
 * @property loadMeetingsUseCase Use case for loading meetings
 * @property createMeetingUseCase Use case for creating meetings
 * @property updateMeetingUseCase Use case for updating meetings
 * @property cancelMeetingUseCase Use case for canceling meetings
 * @property generateMeetingLinkUseCase Use case for generating meeting links
 * @property scope CoroutineScope for launching async work
 */
class MeetingServiceStateMachine(
    private val loadMeetingsUseCase: LoadMeetingsUseCase,
    private val createMeetingUseCase: CreateMeetingUseCase,
    private val updateMeetingUseCase: UpdateMeetingUseCase,
    private val cancelMeetingUseCase: CancelMeetingUseCase,
    private val generateMeetingLinkUseCase: GenerateMeetingLinkUseCase,
    scope: CoroutineScope
) : StateMachine<MeetingManagementContract.State, Intent, SideEffect>(
    initialState = MeetingManagementContract.State(),
    scope = scope
) {

    // ========================================================================
    // Intent Dispatcher
    // ========================================================================

    override suspend fun handleIntent(intent: Intent) {
        when (intent) {
            is Intent.LoadMeetings -> handleLoadMeetings(intent)
            is Intent.CreateMeeting -> handleCreateMeeting(intent)
            is Intent.UpdateMeeting -> handleUpdateMeeting(intent)
            is Intent.CancelMeeting -> handleCancelMeeting(intent)
            is Intent.GenerateMeetingLink -> handleGenerateMeetingLink(intent)
            is Intent.SelectMeeting -> handleSelectMeeting(intent)
            is Intent.ClearGeneratedLink -> handleClearGeneratedLink()
            is Intent.ClearError -> handleClearError()
        }
    }

    // ========================================================================
    // Intent Handlers - Load Operations
    // ========================================================================

    /**
     * Handle load meetings intent
     *
     * Loads all meetings for a specific event.
     *
     * Flow:
     * 1. Set isLoading = true, clear error, update eventId
     * 2. Call loadMeetingsUseCase(eventId)
     * 3. On success: update meetings list, set isLoading = false
     * 4. On failure: set error, emit ShowError, set isLoading = false
     *
     * @param intent Contains eventId to load meetings for
     */
    private suspend fun handleLoadMeetings(intent: Intent.LoadMeetings) {
        updateState {
            it.copy(
                isLoading = true,
                error = null,
                eventId = intent.eventId
            )
        }

        loadMeetingsUseCase(intent.eventId).fold(
            onSuccess = { meetings ->
                updateState {
                    it.copy(
                        isLoading = false,
                        meetings = meetings
                    )
                }
            },
            onFailure = { error ->
                val errorMsg = error.message ?: "Failed to load meetings"
                updateState {
                    it.copy(
                        isLoading = false,
                        error = errorMsg
                    )
                }
                emitSideEffect(SideEffect.ShowError(errorMsg))
            }
        )
    }

    // ========================================================================
    // Intent Handlers - Create/Update/Delete Operations
    // ========================================================================

    /**
     * Handle create meeting intent
     *
     * Creates a new meeting for an event.
     * Only organizers can create meetings.
     *
     * Flow:
     * 1. Set isLoading = true
     * 2. Call createMeetingUseCase(request)
     * 3. On success: reload meetings, emit ShowToast + NavigateTo
     * 4. On failure: set error, emit ShowError, set isLoading = false
     *
     * @param intent Contains the meeting creation request
     */
    private suspend fun handleCreateMeeting(intent: Intent.CreateMeeting) {
        updateState { it.copy(isLoading = true, error = null) }

        createMeetingUseCase(intent.request).fold(
            onSuccess = { meeting ->
                // Reload meetings
                reloadMeetings(currentState.eventId)
                emitSideEffect(SideEffect.ShowToast("Meeting created successfully"))
                emitSideEffect(SideEffect.NavigateTo("meeting/${meeting.id}"))
            },
            onFailure = { error ->
                val errorMsg = error.message ?: "Failed to create meeting"
                updateState {
                    it.copy(
                        isLoading = false,
                        error = errorMsg
                    )
                }
                emitSideEffect(SideEffect.ShowError(errorMsg))
            }
        )
    }

    /**
     * Handle update meeting intent
     *
     * Updates an existing meeting.
     * Only organizers can update meetings.
     *
     * Flow:
     * 1. Set isLoading = true
     * 2. Call updateMeetingUseCase(meetingId, request)
     * 3. On success: reload meetings, update selectedMeeting, emit ShowToast
     * 4. On failure: set error, emit ShowError, set isLoading = false
     *
     * @param intent Contains meetingId and update request
     */
    private suspend fun handleUpdateMeeting(intent: Intent.UpdateMeeting) {
        updateState { it.copy(isLoading = true, error = null) }

        updateMeetingUseCase(intent.meetingId, intent.request).fold(
            onSuccess = { meeting ->
                // Reload meetings
                reloadMeetings(currentState.eventId)
                updateState { it.copy(selectedMeeting = meeting) }
                emitSideEffect(SideEffect.ShowToast("Meeting updated successfully"))
            },
            onFailure = { error ->
                val errorMsg = error.message ?: "Failed to update meeting"
                updateState {
                    it.copy(
                        isLoading = false,
                        error = errorMsg
                    )
                }
                emitSideEffect(SideEffect.ShowError(errorMsg))
            }
        )
    }

    /**
     * Handle cancel meeting intent
     *
     * Cancels a meeting.
     * Only organizers can cancel meetings.
     *
     * Flow:
     * 1. Set isLoading = true
     * 2. Call cancelMeetingUseCase(meetingId, organizerId)
     * 3. On success: reload meetings, clear selectedMeeting, emit ShowToast + NavigateBack
     * 4. On failure: set error, emit ShowError, set isLoading = false
     *
     * @param intent Contains the ID of the meeting to cancel
     */
    private suspend fun handleCancelMeeting(intent: Intent.CancelMeeting) {
        updateState { it.copy(isLoading = true, error = null) }

        // Need organizer ID - get from selected meeting
        val organizerId = currentState.meetings.find { it.id == intent.meetingId }?.organizerId
            ?: run {
                emitSideEffect(SideEffect.ShowError("Cannot cancel meeting: not found"))
                updateState { it.copy(isLoading = false) }
                return
            }

        cancelMeetingUseCase(intent.meetingId, organizerId).fold(
            onSuccess = { _ ->
                // Reload meetings
                reloadMeetings(currentState.eventId)
                updateState { it.copy(selectedMeeting = null) }
                emitSideEffect(SideEffect.ShowToast("Meeting cancelled successfully"))
                emitSideEffect(SideEffect.NavigateBack)
            },
            onFailure = { error ->
                val errorMsg = error.message ?: "Failed to cancel meeting"
                updateState {
                    it.copy(
                        isLoading = false,
                        error = errorMsg
                    )
                }
                emitSideEffect(SideEffect.ShowError(errorMsg))
            }
        )
    }

    // ========================================================================
    // Intent Handlers - Link Generation
    // ========================================================================

    /**
     * Handle generate meeting link intent
     *
     * Generates a meeting link for a specific platform.
     *
     * Flow:
     * 1. Set isLoading = true
     * 2. Call generateMeetingLinkUseCase(meetingId, platform)
     * 3. On success: update generatedLink, emit ShareMeetingLink
     * 4. On failure: set error, emit ShowError, set isLoading = false
     *
     * @param intent Contains meetingId and platform to generate link for
     */
    private suspend fun handleGenerateMeetingLink(intent: Intent.GenerateMeetingLink) {
        updateState { it.copy(isLoading = true, error = null) }

        generateMeetingLinkUseCase(intent.meetingId, intent.platform).fold(
            onSuccess = { linkResponse ->
                updateState {
                    it.copy(
                        isLoading = false,
                        generatedLink = linkResponse
                    )
                }
                emitSideEffect(SideEffect.ShareMeetingLink(linkResponse.meetingUrl))
            },
            onFailure = { error ->
                val errorMsg = error.message ?: "Failed to generate meeting link"
                updateState {
                    it.copy(
                        isLoading = false,
                        error = errorMsg
                    )
                }
                emitSideEffect(SideEffect.ShowError(errorMsg))
            }
        )
    }

    // ========================================================================
    // Intent Handlers - Selection and Navigation
    // ========================================================================

    /**
     * Handle select meeting intent
     *
     * Selects a meeting for viewing details.
     *
     * Flow:
     * 1. Find meeting in meetings list by ID
     * 2. If found: update selectedMeeting, emit NavigateTo
     * 3. If not found: emit ShowError
     *
     * @param intent Contains the ID of the meeting to select
     */
    private suspend fun handleSelectMeeting(intent: Intent.SelectMeeting) {
        val meeting = currentState.meetings.find { it.id == intent.meetingId }

        if (meeting != null) {
            updateState { it.copy(selectedMeeting = meeting) }
            emitSideEffect(SideEffect.NavigateTo("meeting/${intent.meetingId}"))
        } else {
            emitSideEffect(SideEffect.ShowError("Meeting not found"))
        }
    }

    /**
     * Handle clear generated link intent
     *
     * Clears the generated link from state.
     *
     * Flow:
     * 1. Set generatedLink to null
     */
    private suspend fun handleClearGeneratedLink() {
        updateState { it.copy(generatedLink = null) }
    }

    // ========================================================================
    // Intent Handlers - Error Management
    // ========================================================================

    /**
     * Handle clear error intent
     *
     * Clears any error state to dismiss error messages in the UI.
     *
     * Flow:
     * 1. Set error to null
     */
    private suspend fun handleClearError() {
        updateState { it.copy(error = null) }
    }

    // ========================================================================
    // Private Helpers
    // ========================================================================

    /**
     * Helper to reload meetings from repository
     *
     * Used internally after operations that modify meetings
     * (create, update, delete).
     *
     * @param eventId The ID of the event to reload meetings for
     */
    private suspend fun reloadMeetings(eventId: String) {
        loadMeetingsUseCase(eventId).fold(
            onSuccess = { meetings ->
                updateState {
                    it.copy(
                        isLoading = false,
                        meetings = meetings
                    )
                }
            },
            onFailure = { error ->
                val errorMsg = error.message ?: "Failed to reload meetings"
                updateState {
                    it.copy(
                        isLoading = false,
                        error = errorMsg
                    )
                }
                emitSideEffect(SideEffect.ShowError(errorMsg))
            }
        )
    }
}
