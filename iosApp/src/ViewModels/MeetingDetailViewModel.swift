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
/// @StateObject private var viewModel = MeetingDetailViewModel(meetingId: "meeting-123", eventId: "event-123")
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
class MeetingDetailViewModel: StateMachineViewModel<
    MeetingManagementContract.State,
    MeetingManagementContractIntent,
    MeetingManagementContractSideEffect
>{

    // MARK: - Published Properties

    /// The currently selected meeting
    @Published var meeting: VirtualMeeting?

    /// Whether to show delete confirmation dialog
    @Published var showDeleteConfirm = false

    /// Whether view is in editing mode
    @Published var isEditing = false

    // MARK: - Private Properties

    /// The meeting ID passed to this view model
    private let meetingId: String
    private let eventId: String
    private let currentUserId: String
    private let canMutateMeetings: Bool

    // MARK: - Initialization

    init(meetingId: String, eventId: String, currentUserId: String, canMutateMeetings: Bool) {
        self.meetingId = meetingId
        self.eventId = eventId
        self.currentUserId = currentUserId
        self.canMutateMeetings = canMutateMeetings

        let database = RepositoryProvider.shared.database
        let wrapper = IosFactory.shared.createMeetingStateMachine(database: database)
        super.init(stateMachineWrapper: wrapper)

        // Load meetings
        loadMeetings()
    }

    convenience init(meetingId: String) {
        self.init(meetingId: meetingId, eventId: meetingId, currentUserId: "anonymous-user", canMutateMeetings: false)
    }

    convenience init(meetingId: String, eventId: String) {
        self.init(meetingId: meetingId, eventId: eventId, currentUserId: "anonymous-user", canMutateMeetings: false)
    }

    // MARK: - Public Methods

    /// Load meetings
    func loadMeetings() {
        dispatch(MeetingManagementContractIntentLoadMeetings(eventId: eventId))
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
    func cancelMeeting(currentUserId: String? = nil) {
        guard canMutateMeetings else { return }
        let actorId = currentUserId ?? self.currentUserId
        guard !actorId.isEmpty else { return }
        dispatch(MeetingManagementContractIntentCancelMeeting(meetingId: meetingId, currentUserId: actorId))
        showDeleteConfirm = false
    }

    /// Generate a meeting link
    func generateMeetingLink(
        platform: Shared.MeetingPlatform,
        currentUserId: String? = nil,
        isOrganizer: Bool,
        isReadOnly: Bool
    ) {
        let actorId = currentUserId ?? self.currentUserId
        guard canMutateMeetings && isOrganizer && !isReadOnly && !actorId.isEmpty else { return }
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

    var isOrganizer: Bool { canMutateMeetings }
    var isLoaded: Bool { meeting != nil }
    var isEmpty: Bool { state.meetings.isEmpty }
    var isLoading: Bool { state.isLoading }
    var hasError: Bool { state.hasError }
    var errorMessage: String? { state.error }

    // MARK: - State Change Hook

    override func onStateDidChange() {
        updateSelectedMeeting()
    }

    // MARK: - Private Methods

    private func updateSelectedMeeting() {
        meeting = state.meetings.first { $0.id == meetingId }
    }

    // MARK: - Side Effect Mapping

    override func mapSideEffect(_ effect: MeetingManagementContractSideEffect) -> MappedSideEffect {
        switch effect {
        case let showToast as MeetingManagementContractSideEffectShowToast:
            return .toast(showToast.message)
        case let navigateTo as MeetingManagementContractSideEffectNavigateTo:
            return .navigate(navigateTo.route)
        case is MeetingManagementContractSideEffectNavigateBack:
            return .back
        case let showError as MeetingManagementContractSideEffectShowError:
            return .toast(showError.message)
        default:
            return .unhandled(effect)
        }
    }

    // MARK: - Additional Side Effect Handling

    override func handleAdditionalSideEffect(_ effect: MeetingManagementContractSideEffect) {
        switch effect {
        case is MeetingManagementContractSideEffectShareMeetingLink:
            shareMeetingLink()
        default:
            break
        }
    }
}
