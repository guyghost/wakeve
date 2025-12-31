import Foundation
import Shared

/// ViewModel for Meeting List View on iOS
///
/// Manages state and intents for displaying a list of virtual meetings.
/// Uses shared Kotlin state machine to handle all business logic.
///
/// ## Usage
///
/// ```swift
/// @StateObject private var viewModel = MeetingListViewModel(eventId: "event-1")
///
/// var body: some View {
///     if viewModel.isLoading {
///         ProgressView("Loading meetings...")
///     } else if viewModel.isEmpty {
///         EmptyStateView()
///     } else {
///         List(viewModel.meetings) { meeting in
///             MeetingRow(meeting: meeting)
///         }
///     }
///     .onAppear {
///         viewModel.loadMeetings()
///     }
/// }
/// ```
@MainActor
class MeetingListViewModel: ObservableObject {
    // MARK: - Published Properties

    /// Current state from state machine
    @Published var state: MeetingManagementContract.State

    /// Toast message to display (auto-clears after display)
    @Published var toastMessage: String?

    /// Navigation route to trigger (e.g., "detail/meeting-123")
    @Published var navigationRoute: String?

    /// Whether to pop back to previous screen
    @Published var shouldNavigateBack = false

    // MARK: - Private Properties

    /// The event ID this view is managing meetings for
    private let eventId: String

    /// The observable state machine wrapper
    private let stateMachineWrapper: ObservableStateMachine<
        MeetingManagementContract.State,
        MeetingManagementContractIntent,
        MeetingManagementContractSideEffect
    >

    // MARK: - Initialization

    init(eventId: String) {
        self.eventId = eventId

        // Get the shared database from RepositoryProvider
        let database = RepositoryProvider.shared.database

        // Create a state machine via iOS factory (IosFactory is an object/singleton)
        self.stateMachineWrapper = IosFactory.shared.createMeetingStateMachine(database: database)

        // Initialize state with current state from state machine
        self.state = self.stateMachineWrapper.currentState!

        // Observe state changes from the state machine
        self.stateMachineWrapper.onStateChange = { [weak self] newState in
            guard let self = self, let newState = newState else { return }

            DispatchQueue.main.async {
                self.state = newState
            }
        }

        // Observe side effects from the state machine
        self.stateMachineWrapper.onSideEffect = { [weak self] effect in
            guard let self = self, let effect = effect else { return }

            DispatchQueue.main.async {
                self.handleSideEffect(effect)
            }
        }
    }

    // MARK: - Public Methods

    /// Dispatch an intent to the state machine
    func dispatch(_ intent: MeetingManagementContractIntent) {
        stateMachineWrapper.dispatch(intent: intent)
    }

    /// Load meetings for the current event
    func loadMeetings() {
        dispatch(MeetingManagementContractIntentLoadMeetings(eventId: eventId))
    }

    /// Create a new meeting
    func createMeeting(
        platform: Shared.MeetingPlatform,
        title: String,
        description: String?,
        scheduledFor: Date,
        durationMinutes: Int64,
        requirePassword: Bool = true,
        waitingRoom: Bool = true
    ) {
        let request = CreateMeetingRequest(
            eventId: eventId,
            organizerId: getCurrentUserId(),
            platform: platform,
            title: title,
            description: description,
            scheduledFor: scheduledFor.toKotlinxInstant(),
            duration: durationMinutes.toKotlinDuration(),
            timezone: TimeZone.current.identifier,
            participantLimit: nil,
            requirePassword: requirePassword,
            waitingRoom: waitingRoom
        )

        dispatch(MeetingManagementContractIntentCreateMeeting(request: request))
    }

    /// Update an existing meeting
    func updateMeeting(
        meetingId: String,
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

    /// Cancel a meeting
    func cancelMeeting(meetingId: String) {
        dispatch(MeetingManagementContractIntentCancelMeeting(meetingId: meetingId))
    }

    /// Generate a meeting link for a specific platform
    func generateMeetingLink(meetingId: String, platform: Shared.MeetingPlatform) {
        dispatch(MeetingManagementContractIntentGenerateMeetingLink(meetingId: meetingId, platform: platform))
    }

    /// Select a meeting for viewing details
    func selectMeeting(meetingId: String) {
        dispatch(MeetingManagementContractIntentSelectMeeting(meetingId: meetingId))
    }

    /// Clear generated link
    func clearGeneratedLink() {
        dispatch(MeetingManagementContractIntentClearGeneratedLink.shared)
    }

    /// Clear any error state
    func clearError() {
        dispatch(MeetingManagementContractIntentClearError.shared)
    }

    // MARK: - Convenience Properties

    /// List of meetings
    var meetings: [VirtualMeeting] {
        state.meetings
    }

    /// Currently selected meeting
    var selectedMeeting: VirtualMeeting? {
        state.selectedMeeting
    }

    /// Generated meeting link
    var generatedLink: MeetingLinkResponse? {
        state.generatedLink
    }

    /// Whether meetings list is empty
    var isEmpty: Bool {
        meetings.isEmpty
    }

    /// Whether currently in loading state
    var isLoading: Bool {
        state.isLoading
    }

    /// Whether there is an error
    var hasError: Bool {
        state.hasError
    }

    /// Error message (if any)
    var errorMessage: String? {
        state.error
    }

    /// Whether there is a generated link
    var hasGeneratedLink: Bool {
        state.hasGeneratedLink
    }

    // MARK: - Private Methods

    /// Handle side effects emitted by state machine
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
        case let shareLink as MeetingManagementContractSideEffectShareMeetingLink:
            shareMeetingLink(url: shareLink.link)
        default:
            break
        }
    }

    /// Share a meeting link
    private func shareMeetingLink(url: String) {
        print("Share meeting link: \(url)")
    }

    /// Get the current user ID (placeholder)
    private func getCurrentUserId() -> String {
        return "user-placeholder"
    }

    // MARK: - Deinit

    deinit {
        stateMachineWrapper.dispose()
    }
}

// MARK: - Date Extensions for Kotlin Interop

extension Date {
    /// Convert Swift Date to Kotlinx Instant
    func toKotlinxInstant() -> Kotlinx_datetimeInstant {
        let epochSeconds = Int64(self.timeIntervalSince1970)
        let nanoseconds = Int32((self.timeIntervalSince1970.truncatingRemainder(dividingBy: 1)) * 1_000_000_000)
        return Kotlinx_datetimeInstant.companion.fromEpochSeconds(epochSeconds: epochSeconds, nanosecondAdjustment: nanoseconds)
    }
}

extension Int64 {
    /// Convert minutes to Kotlin Duration
    func toKotlinDuration() -> Int64 {
        return self * 60 * 1_000_000_000
    }
}
