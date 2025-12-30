import Foundation
import Shared

/// ViewModel for Meeting Detail View on iOS
///
/// Manages state and intents for displaying details of a single virtual meeting.
/// Uses shared Kotlin state machine to handle all business logic.
///
/// ## Usage
///
/// ```swift
/// @StateObject private var viewModel = MeetingDetailViewModel(meetingId: "meeting-123")
///
/// var body: some View {
///     if viewModel.isLoading {
///         ProgressView("Loading meeting...")
///     } else if let meeting = viewModel.meeting {
///         MeetingDetailViewContent(meeting: meeting)
///     } else {
///         ErrorView(errorMessage: viewModel.errorMessage)
///     }
/// }
/// ```
@MainActor
class MeetingDetailViewModel: ObservableObject {
    // MARK: - Published Properties

    /// Current state from state machine
    @Published var state: MeetingManagementContract.State

    /// The currently selected meeting
    @Published var meeting: VirtualMeeting?

    /// Toast message to display
    @Published var toastMessage: String?

    /// Navigation route to trigger
    @Published var navigationRoute: String?

    /// Whether to pop back to previous screen
    @Published var shouldNavigateBack = false

    /// Whether to show delete confirmation dialog
    @Published var showDeleteConfirm = false

    /// Whether view is in editing mode
    @Published var isEditing = false

    // MARK: - Private Properties

    /// The meeting ID passed to this view model
    private let meetingId: String

    /// The observable state machine wrapper
    private let stateMachineWrapper: ObservableStateMachine<
        MeetingManagementContract.State,
        MeetingManagementContractIntent,
        MeetingManagementContractSideEffect
    >

    // MARK: - Initialization

    init(meetingId: String) {
        self.meetingId = meetingId

        // Get the shared database from RepositoryProvider
        let database = RepositoryProvider.shared.database

        // Create a state machine via iOS factory
        self.stateMachineWrapper = IosFactory.shared.createMeetingStateMachine(database: database)

        // Initialize state with current state from state machine
        self.state = self.stateMachineWrapper.currentState!

        // Observe state changes
        self.stateMachineWrapper.onStateChange = { [weak self] newState in
            guard let self = self, let newState = newState else { return }

            DispatchQueue.main.async {
                self.state = newState
                self.updateSelectedMeeting()
            }
        }

        // Observe side effects
        self.stateMachineWrapper.onSideEffect = { [weak self] effect in
            guard let self = self, let effect = effect else { return }

            DispatchQueue.main.async {
                self.handleSideEffect(effect)
            }
        }

        // Load meetings
        loadMeetings()
    }

    // MARK: - Public Methods

    func dispatch(_ intent: MeetingManagementContractIntent) {
        stateMachineWrapper.dispatch(intent: intent)
    }

    /// Load meetings
    func loadMeetings() {
        dispatch(MeetingManagementContractIntentLoadMeetings(eventId: ""))
    }

    /// Update the meeting
    func updateMeeting(
        title: String,
        description: String?,
        scheduledFor: Date,
        durationMinutes: Int64
    ) {
        let request = UpdateMeetingRequest(
            title: title,
            description: description,
            scheduledFor: scheduledFor.toKotlinxInstant(),
            duration: durationMinutes.toKotlinDuration()
        )

        dispatch(MeetingManagementContractIntentUpdateMeeting(meetingId: meetingId, request: request))
    }

    /// Cancel the meeting
    func cancelMeeting() {
        dispatch(MeetingManagementContractIntentCancelMeeting(meetingId: meetingId))
        showDeleteConfirm = false
    }

    /// Generate a meeting link
    func generateMeetingLink(platform: MeetingPlatform) {
        dispatch(MeetingManagementContractIntentGenerateMeetingLink(meetingId: meetingId, platform: platform))
    }

    /// Start editing
    func startEditing() {
        isEditing = true
    }

    /// Cancel editing
    func cancelEditing() {
        isEditing = false
    }

    /// Clear error
    func clearError() {
        dispatch(MeetingManagementContractIntentClearError.shared)
    }

    /// Share meeting link
    func shareMeetingLink() {
        guard let link = meeting?.meetingUrl else { return }
        print("Share meeting link: \(link)")
    }

    // MARK: - Convenience Properties

    var isOrganizer: Bool { true }
    var isLoaded: Bool { meeting != nil }
    var isEmpty: Bool { state.meetings.isEmpty }
    var isLoading: Bool { state.isLoading }
    var hasError: Bool { state.hasError }
    var errorMessage: String? { state.error }

    // MARK: - Private Methods

    private func handleSideEffect(_ effect: MeetingManagementContractSideEffect) {
        switch effect {
        case let showToast as MeetingManagementContractSideEffectShowToast:
            toastMessage = showToast.message
        case let navigateTo as MeetingManagementContractSideEffectNavigateTo:
            navigationRoute = navigateTo.route
        case is MeetingManagementContractSideEffectNavigateBack:
            shouldNavigateBack = true
        case let showError as MeetingManagementContractSideEffectShowError:
            toastMessage = showError.message
        case is MeetingManagementContractSideEffectShareMeetingLink:
            shareMeetingLink()
        default:
            break
        }
    }

    private func updateSelectedMeeting() {
        meeting = state.meetings.first { $0.id == meetingId }
    }

    // MARK: - Deinit

    deinit {
        stateMachineWrapper.dispose()
    }
}
