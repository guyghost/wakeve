package com.guyghost.wakeve.presentation.state

import com.guyghost.wakeve.models.CreateMeetingRequest
import com.guyghost.wakeve.models.MeetingLinkResponse
import com.guyghost.wakeve.models.MeetingPlatform
import com.guyghost.wakeve.models.UpdateMeetingRequest
import com.guyghost.wakeve.models.VirtualMeeting

/**
 * Contract for Meeting Management State Machine
 *
 * Defines the state, intents, and side effects for managing virtual meetings.
 * This contract is shared across Android (Jetpack Compose) and iOS (SwiftUI).
 *
 * ## Architecture
 *
 * ```
 * User Action (Android/iOS)
 *   ↓
 * Intent
 *   ↓
 * MeetingServiceStateMachine
 *   ↓
 * Use Cases (LoadMeetings, CreateMeeting, GenerateMeetingLink, etc.)
 *   ↓
 * Repository (MeetingRepository)
 *   ↓
 * Update State + Emit Side Effect
 *   ↓
 * UI Re-render (Compose/SwiftUI)
 * ```
 *
 * ## Usage in Android (Jetpack Compose)
 *
 * ```kotlin
 * @Composable
 * fun MeetingListScreen(
 *     viewModel: MeetingManagementViewModel = koinViewModel()
 * ) {
 *     val state by viewModel.state.collectAsState()
 *
 *     LaunchedEffect(Unit) {
 *         viewModel.dispatch(MeetingManagementContract.Intent.LoadMeetings(eventId))
 *     }
 *
 *     LaunchedEffect(Unit) {
 *         viewModel.sideEffect.collect { effect ->
 *             when (effect) {
 *                 is SideEffect.NavigateTo -> navigate(effect.route)
 *                 is SideEffect.ShowToast -> showToast(effect.message)
 *                 // ...
 *             }
 *         }
 *     }
 *
 *     // Render UI with state
 *     MeetingListContent(
 *         state = state,
 *         onDispatch = { viewModel.dispatch(it) }
 *     )
 * }
 * ```
 *
 * ## Usage in iOS (SwiftUI)
 *
 * ```swift
 * class MeetingListViewModel: ObservableObject {
 *     @Published var state: MeetingManagementContract.State
 *     private let stateMachine: StateMachine<...>
 *
 *     init() {
 *         stateMachine = IosFactory().createMeetingStateMachine()
 *         state = stateMachine.currentState
 *
 *         stateMachine.onStateChange = { [weak self] in
 *             self?.state = $0
 *         }
 *
 *         stateMachine.onSideEffect = { [weak self] effect in
 *             self?.handleSideEffect(effect)
 *         }
 *     }
 *
 *     func dispatch(_ intent: MeetingManagementContract.Intent) {
 *         stateMachine.dispatch(intent: intent)
 *     }
 * }
 * ```
 *
 * @see MeetingServiceStateMachine
 * @see com.guyghost.wakeve.models.VirtualMeeting
 */
object MeetingManagementContract {

    /**
     * State for meeting management
     *
     * Contains all observable data needed to render the meeting management UI.
     */
    data class State(
        /**
         * Whether meetings are currently being loaded/created/updated
         */
        val isLoading: Boolean = false,

        /**
         * List of all meetings for the current event
         */
        val meetings: List<VirtualMeeting> = emptyList(),

        /**
         * Currently selected meeting (for detail view)
         */
        val selectedMeeting: VirtualMeeting? = null,

        /**
         * The ID of the current event
         */
        val eventId: String = "",

        /**
         * Generated meeting link (for meeting creation)
         */
        val generatedLink: MeetingLinkResponse? = null,

        /**
         * Error message if an operation failed
         */
        val error: String? = null
    ) {
        /**
         * Helper to check if there's an error
         */
        val hasError: Boolean
            get() = error != null

        /**
         * Helper to check if meetings list is empty
         */
        val isEmpty: Boolean
            get() = meetings.isEmpty()

        /**
         * Helper to check if there's a generated link
         */
        val hasGeneratedLink: Boolean
            get() = generatedLink != null
    }

    /**
     * Intents (user actions) that can be dispatched to the state machine
     *
     * Each intent represents a user action or system event that should
     * trigger a state update or side effect.
     */
    sealed interface Intent {
        /**
         * Load all meetings for a specific event
         */
        data class LoadMeetings(val eventId: String) : Intent

        /**
         * Create a new meeting
         */
        data class CreateMeeting(val request: CreateMeetingRequest) : Intent

        /**
         * Update an existing meeting
         */
        data class UpdateMeeting(val meetingId: String, val request: UpdateMeetingRequest) : Intent

        /**
         * Cancel a meeting
         */
        data class CancelMeeting(val meetingId: String) : Intent

        /**
         * Generate a meeting link for a specific platform
         */
        data class GenerateMeetingLink(val meetingId: String, val platform: MeetingPlatform) : Intent

        /**
         * Select a meeting for viewing details
         */
        data class SelectMeeting(val meetingId: String) : Intent

        /**
         * Clear the generated link
         */
        data object ClearGeneratedLink : Intent

        /**
         * Clear any error state
         */
        data object ClearError : Intent
    }

    /**
     * Side effects (one-shot events) emitted by the state machine
     *
     * Side effects are events that happen once and are not part of the state.
     * Examples: navigation, toasts, sharing, etc.
     */
    sealed interface SideEffect {
        /**
         * Show a toast message to the user
         */
        data class ShowToast(val message: String) : SideEffect

        /**
         * Show an error message to the user
         */
        data class ShowError(val message: String) : SideEffect

        /**
         * Navigate to a specific route/screen
         */
        data class NavigateTo(val route: String) : SideEffect

        /**
         * Navigate back to the previous screen
         */
        data object NavigateBack : SideEffect

        /**
         * Share a meeting link
         */
        data class ShareMeetingLink(val link: String) : SideEffect
    }
}
