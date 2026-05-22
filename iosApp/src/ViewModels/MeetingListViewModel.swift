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
class MeetingListViewModel: StateMachineViewModel<
    MeetingManagementContract.State,
    MeetingManagementContractIntent,
    MeetingManagementContractSideEffect
> {

    // MARK: - Private Properties

    /// The event ID this view is managing meetings for
    private let eventId: String
    private let currentUserId: String
    @Published var pendingSync: Bool
    @Published var isOnline: Bool

    // MARK: - Initialization

    init(eventId: String, currentUserId: String) {
        self.eventId = eventId
        self.currentUserId = currentUserId
        let initialPendingSync = Self.hasPendingSync(eventId: eventId)
        self.pendingSync = initialPendingSync
        self.isOnline = !initialPendingSync

        let database = RepositoryProvider.shared.database
        let wrapper = IosFactory.shared.createMeetingStateMachine(database: database)
        super.init(stateMachineWrapper: wrapper)
    }

    convenience init(eventId: String) {
        self.init(eventId: eventId, currentUserId: "anonymous-user")
    }

    // MARK: - Public Methods

    /// Load meetings for the current event
    func loadMeetings() {
        pendingSync = hasPendingSync()
        isOnline = !pendingSync
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
            organizerId: currentUserId,
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
    func cancelMeeting(
        meetingId: String,
        currentUserId: String? = nil,
        isOrganizer: Bool = false,
        isReadOnly: Bool = true
    ) {
        let actorId = currentUserId ?? self.currentUserId
        guard isOrganizer && !isReadOnly && !actorId.isEmpty else {
            return
        }
        dispatch(MeetingManagementContractIntentCancelMeeting(meetingId: meetingId, currentUserId: actorId))
    }

    /// Generate a meeting link for a specific platform
    func generateMeetingLink(
        meetingId: String,
        platform: Shared.MeetingPlatform,
        currentUserId: String? = nil,
        isOrganizer: Bool = false,
        isReadOnly: Bool = true
    ) {
        let actorId = currentUserId ?? self.currentUserId
        guard isOrganizer && !isReadOnly && !actorId.isEmpty else {
            return
        }
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
        case let shareLink as MeetingManagementContractSideEffectShareMeetingLink:
            shareMeetingLink(url: shareLink.link)
        default:
            break
        }
    }

    // MARK: - Private Methods

    /// Share a meeting link
    private func shareMeetingLink(url: String) {
        print("Share meeting link: \(url)")
    }

    private func hasPendingSync() -> Bool {
        Self.hasPendingSync(eventId: eventId)
    }

    private static func hasPendingSync(eventId: String) -> Bool {
        RepositoryProvider.shared.database.syncMetadataQueries.selectPending().executeAsList().contains { pending in
            pending.entityType == "meeting" &&
                (pending.entityId == eventId || pending.entityId.hasPrefix("\(eventId):") || pending.entityId.contains(eventId))
        }
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
