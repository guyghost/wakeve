import SwiftUI
import Shared

private struct WakeveAccessibilityReduceTransparencyOverrideKey: EnvironmentKey {
    static let defaultValue = false
}

extension EnvironmentValues {
    var wakeveAccessibilityReduceTransparencyOverride: Bool {
        get { self[WakeveAccessibilityReduceTransparencyOverrideKey.self] }
        set { self[WakeveAccessibilityReduceTransparencyOverrideKey.self] = newValue }
    }
}

enum EventDetailInvitationCanvasMode: Equatable {
    case draftOrganizerBlocked
    case draftOrganizerReady
    case draftParticipantReadOnly
    case draftRestricted
    case pollingOrganizer
    case pollingResponseDue
    case pollingResponseSubmitted
    case pollingParticipantNeutral
    case pollingRestricted
    case confirmedOrganizer
    case confirmedParticipant
    case confirmedRestricted
    case comparingOrganizer
    case comparingParticipant
    case comparingRestricted
    case organizingOrganizer
    case organizingParticipant
    case organizingRestricted
    case finalizedReadOnly
    case finalizedRestrictedReadOnly
    case unsupported
}

enum EventDetailInvitationCanvasAccess: Equatable {
    case organizer
    case eligibleParticipant
    case restrictedParticipant
    case nonParticipant
}

enum EventDetailInvitationCanvasVoteState: Equatable {
    case required
    case submitted
    case notApplicable
    case unavailable
}

enum EventDetailInvitationCanvasResponsibility: Equatable {
    case currentUser
    case organizer
    case group
    case none
    case unavailable
}

enum EventDetailInvitationCanvasParticipantStatus: Equatable {
    case confirmed
    case pending
}

struct EventDetailInvitationCanvasParticipantIdentity: Equatable, Identifiable {
    let id: String
    let initials: String
    let accessibilityName: String
    let status: EventDetailInvitationCanvasParticipantStatus
}

struct EventDetailInvitationCanvasParticipantSnapshot: Equatable {
    let confirmedCount: Int
    let pendingCount: Int
    let identities: [EventDetailInvitationCanvasParticipantIdentity]

    init(
        confirmedCount: Int,
        pendingCount: Int,
        identities: [EventDetailInvitationCanvasParticipantIdentity]
    ) {
        self.identities = identities
        // Identities are the single snapshot source. Caller-provided aggregates
        // are accepted for source compatibility, then reconciled deterministically.
        self.confirmedCount = identities.filter { $0.status == .confirmed }.count
        self.pendingCount = identities.filter { $0.status == .pending }.count
    }
}

enum EventDetailInvitationCanvasParticipantData: Equatable {
    case loading
    case available(EventDetailInvitationCanvasParticipantSnapshot)
    case unavailable
    case failed
}

struct EventDetailInvitationCanvasReadinessItem: Equatable, Identifiable {
    let id: String
    let isComplete: Bool
    let destination: EventDetailInvitationCanvasAction
}

enum EventDetailInvitationCanvasReadinessData: Equatable {
    case loading
    case available(items: [EventDetailInvitationCanvasReadinessItem])
    case unavailable
    case failed
}

enum EventDetailInvitationCanvasSyncSubject: Equatable {
    case event(id: String)
    case participantResponse(id: String)
    case planningItem(id: String)
}

enum EventDetailInvitationCanvasRelevantSync: Equatable {
    case none
    case pending(subject: EventDetailInvitationCanvasSyncSubject)
    case synced(subject: EventDetailInvitationCanvasSyncSubject)
    case unavailable
}

enum EventDetailInvitationCanvasSyncDecoration: Equatable {
    case none
    case pending(subject: EventDetailInvitationCanvasSyncSubject)
    case synced(subject: EventDetailInvitationCanvasSyncSubject)
}

enum EventDetailInvitationCanvasHeroImageState: Equatable {
    case loading
    case available
    case missing
    case failed
}

enum EventDetailInvitationCanvasHeroTreatment: Equatable {
    case remoteImage
    case loadingMoodFallback
    case moodFallback
}

enum EventDetailInvitationCanvasAuxiliaryFreshness: Equatable {
    case current
    case stale
    case unavailable
}

enum EventDetailInvitationCanvasSecondarySection: Hashable {
    case invitationLanding
    case metadataOverview
    case weather
    case anticipation
    case aiReview
    case readiness
    case participants
    case organizationDetails
    case messages
}

enum EventDetailInvitationCanvasAction: Hashable {
    case editDraft
    case submitVote
    case viewPollResults
    case compareOptions
    case continueOrganization
    case viewFinalDetails
    case showAccessState
    case showDetails
}

enum EventDetailInvitationCanvasActionPlacement: Equatable {
    case inCanvas
    case persistentSafeArea
}

enum EventDetailInvitationCanvasInteractionPolicy: Equatable {
    case interactive
    case readOnly
}

struct EventDetailInvitationCanvasPendingSyncMetadata: Equatable {
    let entityType: String
    let entityID: String
}

struct EventDetailInvitationCanvasContextMapper {
    struct Input {
        let eventStatus: EventStatus
        let eventID: String
        let organizerID: String
        let currentUserID: String
        let participantIDs: Set<String>
        let participantRecords: [ParticipantRepositoryRecord]
        let hasPollData: Bool
        let proposedSlotIDs: Set<String>
        let votedSlotIDs: Set<String>
        let pendingSyncMetadata: [EventDetailInvitationCanvasPendingSyncMetadata]
        let hasPendingPhase5AggregateSync: Bool
        let routableActions: Set<EventDetailInvitationCanvasAction>
    }

    struct Output {
        let currentUserAccess: EventDetailInvitationCanvasAccess
        let currentUserVote: EventDetailInvitationCanvasVoteState
        let participantData: EventDetailInvitationCanvasParticipantData
        let responsibility: EventDetailInvitationCanvasResponsibility
        let availableActions: Set<EventDetailInvitationCanvasAction>
        let relevantSync: EventDetailInvitationCanvasRelevantSync
        let interactionPolicy: EventDetailInvitationCanvasInteractionPolicy
    }

    func map(_ input: Input) -> Output {
        let currentUserAccess = access(for: input)
        let currentUserVote = voteState(for: input, access: currentUserAccess)

        return Output(
            currentUserAccess: currentUserAccess,
            currentUserVote: currentUserVote,
            participantData: participantData(from: input.participantRecords),
            responsibility: responsibility(
                status: input.eventStatus,
                access: currentUserAccess,
                voteState: currentUserVote
            ),
            availableActions: availableActions(
                status: input.eventStatus,
                access: currentUserAccess,
                voteState: currentUserVote,
                routableActions: input.routableActions
            ),
            relevantSync: relevantSync(for: input),
            interactionPolicy: interactionPolicy(for: input.eventStatus)
        )
    }

    private func interactionPolicy(
        for status: EventStatus
    ) -> EventDetailInvitationCanvasInteractionPolicy {
        status == .finalized ? .readOnly : .interactive
    }

    private func access(for input: Input) -> EventDetailInvitationCanvasAccess {
        if input.organizerID == input.currentUserID {
            return .organizer
        }

        guard let currentRecord = input.participantRecords.first(where: {
            $0.userId == input.currentUserID
        }) else {
            return input.participantIDs.contains(input.currentUserID)
                ? .restrictedParticipant
                : .nonParticipant
        }

        let accessState = ParticipantAccessMapper.shared.fromRepositoryRecord(record: currentRecord)
        let accessRow = ParticipantManagementPresentationMapper.shared
            .map(participants: [accessState])
            .first

        switch input.eventStatus {
        case .draft, .polling:
            return accessState.rsvp == .declined
                ? .restrictedParticipant
                : .eligibleParticipant
        case .confirmed, .comparing, .organizing, .finalized:
            return accessRow?.canAccessOrganizationDetails == true
                ? .eligibleParticipant
                : .restrictedParticipant
        default:
            return .restrictedParticipant
        }
    }

    private func voteState(
        for input: Input,
        access: EventDetailInvitationCanvasAccess
    ) -> EventDetailInvitationCanvasVoteState {
        guard input.eventStatus == .polling else { return .notApplicable }
        guard access == .eligibleParticipant else { return .notApplicable }
        guard input.hasPollData, !input.proposedSlotIDs.isEmpty else { return .unavailable }

        let hasCompleteVote = input.proposedSlotIDs.allSatisfy(input.votedSlotIDs.contains)
        return hasCompleteVote ? .submitted : .required
    }

    private func participantData(
        from participantRecords: [ParticipantRepositoryRecord]
    ) -> EventDetailInvitationCanvasParticipantData {
        guard !participantRecords.isEmpty else { return .unavailable }

        let accessStates = participantRecords.map {
            ParticipantAccessMapper.shared.fromRepositoryRecord(record: $0)
        }
        let accessRows = ParticipantManagementPresentationMapper.shared.map(participants: accessStates)
        let accessRowsByID = Dictionary(uniqueKeysWithValues: accessRows.map {
            ($0.userIdOrEmail, $0)
        })

        let identities: [EventDetailInvitationCanvasParticipantIdentity] = zip(
            participantRecords,
            accessStates
        ).compactMap { pair -> EventDetailInvitationCanvasParticipantIdentity? in
            let (record, accessState) = pair
            guard accessState.rsvp != ParticipantRsvp.declined else { return nil }
            let displayName = displayName(from: record.userId)
            let canAccessDetails = accessRowsByID[record.userId]?.canAccessOrganizationDetails == true
            return EventDetailInvitationCanvasParticipantIdentity(
                id: record.id,
                initials: initials(from: displayName),
                accessibilityName: displayName,
                status: canAccessDetails ? .confirmed : .pending
            )
        }

        return .available(
            EventDetailInvitationCanvasParticipantSnapshot(
                confirmedCount: identities.filter {
                    $0.status == EventDetailInvitationCanvasParticipantStatus.confirmed
                }.count,
                pendingCount: identities.filter {
                    $0.status == EventDetailInvitationCanvasParticipantStatus.pending
                }.count,
                identities: identities
            )
        )
    }

    private func responsibility(
        status: EventStatus,
        access: EventDetailInvitationCanvasAccess,
        voteState: EventDetailInvitationCanvasVoteState
    ) -> EventDetailInvitationCanvasResponsibility {
        switch (status, access, voteState) {
        case (.draft, .organizer, _):
            return .currentUser
        case (.polling, .eligibleParticipant, .required):
            return .currentUser
        case (.polling, .organizer, _):
            return .group
        case (.confirmed, .organizer, _),
             (.comparing, .organizer, _),
             (.organizing, .organizer, _):
            return .currentUser
        case (.confirmed, .eligibleParticipant, _),
             (.comparing, .eligibleParticipant, _),
             (.organizing, .eligibleParticipant, _):
            return .organizer
        case (.finalized, _, _):
            return .none
        default:
            return .unavailable
        }
    }

    private func availableActions(
        status: EventStatus,
        access: EventDetailInvitationCanvasAccess,
        voteState: EventDetailInvitationCanvasVoteState,
        routableActions: Set<EventDetailInvitationCanvasAction>
    ) -> Set<EventDetailInvitationCanvasAction> {
        var candidates: Set<EventDetailInvitationCanvasAction> = [.showDetails]

        switch (status, access) {
        case (.draft, .organizer):
            candidates.insert(.editDraft)
        case (.polling, .organizer):
            candidates.insert(.viewPollResults)
        case (.polling, .eligibleParticipant):
            candidates.insert(voteState == .required ? .submitVote : .viewPollResults)
        case (.confirmed, .organizer), (.confirmed, .eligibleParticipant):
            candidates.formUnion([.compareOptions, .continueOrganization])
        case (.comparing, .organizer), (.comparing, .eligibleParticipant):
            candidates.insert(.compareOptions)
        case (.organizing, .organizer), (.organizing, .eligibleParticipant):
            candidates.insert(.continueOrganization)
        case (.finalized, .organizer), (.finalized, .eligibleParticipant):
            candidates.insert(.viewFinalDetails)
        case (_, .restrictedParticipant), (_, .nonParticipant), (.draft, .eligibleParticipant):
            candidates.insert(.showAccessState)
        default:
            break
        }

        return candidates.intersection(routableActions).union([.showDetails])
    }

    private func relevantSync(for input: Input) -> EventDetailInvitationCanvasRelevantSync {
        let hasPendingVote = input.pendingSyncMetadata.contains { metadata in
            metadata.entityType == "vote" && input.proposedSlotIDs.contains { slotID in
                metadata.entityID == "vote_\(slotID)_\(input.currentUserID)"
            }
        }
        if hasPendingVote {
            return .pending(subject: .participantResponse(id: input.currentUserID))
        }

        let hasPendingEvent = input.hasPendingPhase5AggregateSync ||
            input.pendingSyncMetadata.contains {
                $0.entityType == "event" && $0.entityID == input.eventID
            }
        if hasPendingEvent {
            return .pending(subject: .event(id: input.eventID))
        }

        return .none
    }

    private func displayName(from rawValue: String) -> String {
        let localPart = rawValue.split(separator: "@", maxSplits: 1).first.map(String.init) ?? rawValue
        let words = localPart
            .replacingOccurrences(of: ".", with: " ")
            .replacingOccurrences(of: "_", with: " ")
            .split(separator: " ")
            .map { String($0).capitalized }
        return words.isEmpty ? rawValue : words.joined(separator: " ")
    }

    private func initials(from displayName: String) -> String {
        let words = displayName.split(separator: " ")
        if let first = words.first?.first, let second = words.dropFirst().first?.first {
            return "\(first)\(second)".uppercased()
        }
        return String(displayName.prefix(2)).uppercased()
    }
}

struct EventDetailInvitationShareBinding: Equatable, Hashable, Sendable {
    let eventId: String
    let actorId: String
    let accessRevision: Int64
    let capabilityId: String
}

struct EventDetailInvitationServerIssuedSharePayload: Equatable, Hashable, Sendable {
    /// Opaque reference issued by the secure invitation flow. The canvas never
    /// interprets it as a token or constructs a URL from it.
    let opaqueValue: String
    let binding: EventDetailInvitationShareBinding

    init(
        opaqueValue: String,
        binding: EventDetailInvitationShareBinding
    ) {
        self.opaqueValue = opaqueValue
        self.binding = binding
    }

    #if DEBUG
    /// Compatibility for isolated mapper tests and previews. Production READY
    /// construction always requires an explicit server-issued binding.
    init(opaqueValue: String) {
        self.init(
            opaqueValue: opaqueValue,
            binding: EventDetailInvitationShareBinding(
                eventId: "debug-unbound-event",
                actorId: "debug-unbound-actor",
                accessRevision: -1,
                capabilityId: "debug-unbound-capability"
            )
        )
    }
    #endif
}

enum EventDetailInvitationShareCapability: Equatable {
    typealias Binding = EventDetailInvitationShareBinding

    case hidden
    case requesting
    case ready(serverIssuedPayload: EventDetailInvitationServerIssuedSharePayload)
    case unavailable(reason: String)
    case failed(reason: String)
}

enum EventDetailInvitationCanvasLifecycleTone: Equatable {
    case draft
    case polling
    case confirmed
    case comparing
    case organizing
    case finalized
    case unavailable
}

struct EventDetailInvitationCanvasPresentation: Equatable {
    let canvasMode: EventDetailInvitationCanvasMode
    let lifecycleLabelKey: String
    let lifecycleTone: EventDetailInvitationCanvasLifecycleTone
    let participantData: EventDetailInvitationCanvasParticipantData
    let responsibleActor: EventDetailInvitationCanvasResponsibility
    let primaryAction: EventDetailInvitationCanvasAction
    let primaryActionTitleKey: String
    let primaryActionSubtitleKey: String
    let primaryActionSystemImage: String
    let primaryActionPlacement: EventDetailInvitationCanvasActionPlacement
    let shareCapability: EventDetailInvitationShareCapability
    let syncDecoration: EventDetailInvitationCanvasSyncDecoration
    let heroTreatment: EventDetailInvitationCanvasHeroTreatment
    let auxiliaryFreshness: EventDetailInvitationCanvasAuxiliaryFreshness
    let visibleSecondarySections: Set<EventDetailInvitationCanvasSecondarySection>
    let interactionPolicy: EventDetailInvitationCanvasInteractionPolicy
}

struct EventDetailInvitationCanvasMapper {
    struct Input {
        let eventStatus: EventStatus
        let hasRequiredSlots: Bool
        let currentUserAccess: EventDetailInvitationCanvasAccess
        let currentUserVote: EventDetailInvitationCanvasVoteState
        let participantData: EventDetailInvitationCanvasParticipantData
        let responsibility: EventDetailInvitationCanvasResponsibility
        let readinessData: EventDetailInvitationCanvasReadinessData
        let availableActions: Set<EventDetailInvitationCanvasAction>
        let relevantSync: EventDetailInvitationCanvasRelevantSync
        let shareCapability: EventDetailInvitationShareCapability
        let heroImageState: EventDetailInvitationCanvasHeroImageState
        let auxiliaryFreshness: EventDetailInvitationCanvasAuxiliaryFreshness
        let requestedSecondarySections: Set<EventDetailInvitationCanvasSecondarySection>
        let primaryActionPlacement: EventDetailInvitationCanvasActionPlacement
        let shareValidationContext: EventDetailInvitationShareBinding?

        init(
            eventStatus: EventStatus,
            hasRequiredSlots: Bool,
            currentUserAccess: EventDetailInvitationCanvasAccess,
            currentUserVote: EventDetailInvitationCanvasVoteState,
            participantData: EventDetailInvitationCanvasParticipantData,
            responsibility: EventDetailInvitationCanvasResponsibility,
            readinessData: EventDetailInvitationCanvasReadinessData,
            availableActions: Set<EventDetailInvitationCanvasAction>,
            relevantSync: EventDetailInvitationCanvasRelevantSync,
            shareCapability: EventDetailInvitationShareCapability,
            heroImageState: EventDetailInvitationCanvasHeroImageState,
            auxiliaryFreshness: EventDetailInvitationCanvasAuxiliaryFreshness,
            requestedSecondarySections: Set<EventDetailInvitationCanvasSecondarySection>,
            primaryActionPlacement: EventDetailInvitationCanvasActionPlacement,
            shareValidationContext: EventDetailInvitationShareBinding? = nil
        ) {
            self.eventStatus = eventStatus
            self.hasRequiredSlots = hasRequiredSlots
            self.currentUserAccess = currentUserAccess
            self.currentUserVote = currentUserVote
            self.participantData = participantData
            self.responsibility = responsibility
            self.readinessData = readinessData
            self.availableActions = availableActions
            self.relevantSync = relevantSync
            self.shareCapability = shareCapability
            self.heroImageState = heroImageState
            self.auxiliaryFreshness = auxiliaryFreshness
            self.requestedSecondarySections = requestedSecondarySections
            self.primaryActionPlacement = primaryActionPlacement
            self.shareValidationContext = shareValidationContext
        }
    }

    static let finalizedAllowedActions: Set<EventDetailInvitationCanvasAction> = [
        .viewFinalDetails,
        .showAccessState,
        .showDetails
    ]

    func map(_ input: Input) -> EventDetailInvitationCanvasPresentation {
        let mode: EventDetailInvitationCanvasMode
        let primaryAction: EventDetailInvitationCanvasAction

        switch (input.eventStatus, input.currentUserAccess) {
        case (.draft, .organizer):
            mode = input.hasRequiredSlots ? .draftOrganizerReady : .draftOrganizerBlocked
            primaryAction = available(.editDraft, in: input.availableActions) ?? .showDetails
        case (.draft, .eligibleParticipant):
            mode = .draftParticipantReadOnly
            primaryAction = available(.showAccessState, in: input.availableActions) ?? .showDetails
        case (.draft, .restrictedParticipant):
            mode = .draftRestricted
            primaryAction = available(.showAccessState, in: input.availableActions) ?? .showDetails
        case (.draft, .nonParticipant):
            mode = .draftRestricted
            primaryAction = available(.showAccessState, in: input.availableActions) ?? .showDetails

        case (.polling, .organizer):
            mode = .pollingOrganizer
            primaryAction = available(.viewPollResults, in: input.availableActions) ?? .showDetails
        case (.polling, .eligibleParticipant):
            switch input.currentUserVote {
            case .required:
                mode = .pollingResponseDue
                primaryAction = available(.submitVote, in: input.availableActions) ?? .showDetails
            case .submitted:
                mode = .pollingResponseSubmitted
                primaryAction = available(.viewPollResults, in: input.availableActions) ?? .showDetails
            case .notApplicable, .unavailable:
                mode = .pollingParticipantNeutral
                primaryAction = available(.viewPollResults, in: input.availableActions) ?? .showDetails
            }
        case (.polling, .restrictedParticipant):
            mode = .pollingRestricted
            primaryAction = available(.showAccessState, in: input.availableActions) ?? .showDetails
        case (.polling, .nonParticipant):
            mode = .pollingRestricted
            primaryAction = available(.showAccessState, in: input.availableActions) ?? .showDetails

        case (.confirmed, .organizer):
            mode = .confirmedOrganizer
            primaryAction = firstAvailable(
                [.compareOptions, .continueOrganization],
                in: input.availableActions
            ) ?? .showDetails
        case (.confirmed, .eligibleParticipant):
            mode = .confirmedParticipant
            primaryAction = firstAvailable(
                [.compareOptions, .continueOrganization],
                in: input.availableActions
            ) ?? .showDetails
        case (.confirmed, .restrictedParticipant):
            mode = .confirmedRestricted
            primaryAction = available(.showAccessState, in: input.availableActions) ?? .showDetails
        case (.confirmed, .nonParticipant):
            mode = .confirmedRestricted
            primaryAction = available(.showAccessState, in: input.availableActions) ?? .showDetails

        case (.comparing, .organizer):
            mode = .comparingOrganizer
            primaryAction = available(.compareOptions, in: input.availableActions) ?? .showDetails
        case (.comparing, .eligibleParticipant):
            mode = .comparingParticipant
            primaryAction = available(.compareOptions, in: input.availableActions) ?? .showDetails
        case (.comparing, .restrictedParticipant):
            mode = .comparingRestricted
            primaryAction = available(.showAccessState, in: input.availableActions) ?? .showDetails
        case (.comparing, .nonParticipant):
            mode = .comparingRestricted
            primaryAction = available(.showAccessState, in: input.availableActions) ?? .showDetails

        case (.organizing, .organizer):
            mode = .organizingOrganizer
            primaryAction = organizingAction(for: input) ?? .showDetails
        case (.organizing, .eligibleParticipant):
            mode = .organizingParticipant
            primaryAction = organizingAction(for: input) ?? .showDetails
        case (.organizing, .restrictedParticipant):
            mode = .organizingRestricted
            primaryAction = available(.showAccessState, in: input.availableActions) ?? .showDetails
        case (.organizing, .nonParticipant):
            mode = .organizingRestricted
            primaryAction = available(.showAccessState, in: input.availableActions) ?? .showDetails

        case (.finalized, .organizer):
            mode = .finalizedReadOnly
            primaryAction = finalizedAction(
                preferred: .viewFinalDetails,
                availableActions: input.availableActions
            )
        case (.finalized, .eligibleParticipant):
            mode = .finalizedReadOnly
            primaryAction = finalizedAction(
                preferred: .viewFinalDetails,
                availableActions: input.availableActions
            )
        case (.finalized, .restrictedParticipant):
            mode = .finalizedRestrictedReadOnly
            primaryAction = finalizedAction(
                preferred: .showAccessState,
                availableActions: input.availableActions
            )
        case (.finalized, .nonParticipant):
            mode = .finalizedRestrictedReadOnly
            primaryAction = finalizedAction(
                preferred: .showAccessState,
                availableActions: input.availableActions
            )
        default:
            mode = .unsupported
            primaryAction = .showDetails
        }

        return EventDetailInvitationCanvasPresentation(
            canvasMode: mode,
            lifecycleLabelKey: lifecycleLabelKey(for: input.eventStatus),
            lifecycleTone: lifecycleTone(for: input.eventStatus),
            participantData: filteredParticipantData(for: input),
            responsibleActor: filteredResponsibility(for: input),
            primaryAction: primaryAction,
            primaryActionTitleKey: actionTitleKey(for: primaryAction),
            primaryActionSubtitleKey: actionSubtitleKey(for: mode),
            primaryActionSystemImage: actionSystemImage(for: primaryAction),
            primaryActionPlacement: input.primaryActionPlacement,
            shareCapability: filteredShareCapability(for: input),
            syncDecoration: syncDecoration(for: input.relevantSync),
            heroTreatment: heroTreatment(for: input.heroImageState),
            auxiliaryFreshness: input.auxiliaryFreshness,
            visibleSecondarySections: visibleSecondarySections(for: input),
            interactionPolicy: interactionPolicy(for: input.eventStatus)
        )
    }

    private func interactionPolicy(
        for status: EventStatus
    ) -> EventDetailInvitationCanvasInteractionPolicy {
        status == .finalized ? .readOnly : .interactive
    }

    private func available(
        _ action: EventDetailInvitationCanvasAction,
        in availableActions: Set<EventDetailInvitationCanvasAction>
    ) -> EventDetailInvitationCanvasAction? {
        availableActions.contains(action) ? action : nil
    }

    private func firstAvailable(
        _ orderedActions: [EventDetailInvitationCanvasAction],
        in availableActions: Set<EventDetailInvitationCanvasAction>
    ) -> EventDetailInvitationCanvasAction? {
        orderedActions.first(where: availableActions.contains)
    }

    private func finalizedAction(
        preferred: EventDetailInvitationCanvasAction,
        availableActions: Set<EventDetailInvitationCanvasAction>
    ) -> EventDetailInvitationCanvasAction {
        let safeActions = availableActions.intersection(Self.finalizedAllowedActions)
        if safeActions.contains(preferred) {
            return preferred
        }
        if safeActions.contains(.showDetails) {
            return .showDetails
        }
        return .showDetails
    }

    private func organizingAction(for input: Input) -> EventDetailInvitationCanvasAction? {
        guard case .available(let items) = input.readinessData,
              let nextIncompleteItem = items.first(where: { !$0.isComplete }),
              nextIncompleteItem.destination == .continueOrganization
        else {
            return nil
        }
        return available(nextIncompleteItem.destination, in: input.availableActions)
    }

    private func filteredParticipantData(
        for input: Input
    ) -> EventDetailInvitationCanvasParticipantData {
        switch input.currentUserAccess {
        case .organizer, .eligibleParticipant:
            return input.participantData
        case .restrictedParticipant, .nonParticipant:
            return .unavailable
        }
    }

    private func filteredResponsibility(
        for input: Input
    ) -> EventDetailInvitationCanvasResponsibility {
        switch input.currentUserAccess {
        case .organizer, .eligibleParticipant:
            return input.responsibility
        case .restrictedParticipant, .nonParticipant:
            return .none
        }
    }

    private func filteredShareCapability(for input: Input) -> EventDetailInvitationShareCapability {
        guard input.currentUserAccess == .organizer else { return .hidden }
        guard case .ready(let serverIssuedPayload) = input.shareCapability else {
            return input.shareCapability
        }
        #if DEBUG
        if input.shareValidationContext == nil {
            return input.shareCapability
        }
        #endif
        guard let current = input.shareValidationContext else { return .hidden }
        let binding = serverIssuedPayload.binding
        guard binding.eventId == current.eventId,
              binding.actorId == current.actorId,
              binding.accessRevision == current.accessRevision,
              binding.capabilityId == current.capabilityId,
              !binding.capabilityId.isEmpty
        else { return .hidden }
        return input.shareCapability
    }

    private func syncDecoration(
        for relevantSync: EventDetailInvitationCanvasRelevantSync
    ) -> EventDetailInvitationCanvasSyncDecoration {
        switch relevantSync {
        case .none, .unavailable:
            return .none
        case .pending(let subject):
            return .pending(subject: subject)
        case .synced(let subject):
            return .synced(subject: subject)
        }
    }

    private func heroTreatment(
        for imageState: EventDetailInvitationCanvasHeroImageState
    ) -> EventDetailInvitationCanvasHeroTreatment {
        switch imageState {
        case .available:
            return .remoteImage
        case .loading:
            return .loadingMoodFallback
        case .missing, .failed:
            return .moodFallback
        }
    }

    private func visibleSecondarySections(
        for input: Input
    ) -> Set<EventDetailInvitationCanvasSecondarySection> {
        let accessFilteredSections: Set<EventDetailInvitationCanvasSecondarySection>
        switch input.currentUserAccess {
        case .organizer, .eligibleParticipant:
            accessFilteredSections = input.requestedSecondarySections
        case .restrictedParticipant, .nonParticipant:
            let failClosedSections: Set<EventDetailInvitationCanvasSecondarySection> = [
                .invitationLanding,
                .metadataOverview,
                .anticipation,
                .messages
            ]
            accessFilteredSections = input.requestedSecondarySections.intersection(failClosedSections)
        }

        guard input.eventStatus == .finalized else {
            return accessFilteredSections
        }
        return accessFilteredSections.subtracting([.aiReview])
    }

    private func lifecycleLabelKey(for status: EventStatus) -> String {
        switch status {
        case .draft: return "event.detail.canvas.lifecycle.draft"
        case .polling: return "event.detail.canvas.lifecycle.polling"
        case .confirmed: return "event.detail.canvas.lifecycle.confirmed"
        case .comparing: return "event.detail.canvas.lifecycle.comparing"
        case .organizing: return "event.detail.canvas.lifecycle.organizing"
        case .finalized: return "event.detail.canvas.lifecycle.finalized"
        default: return "event.detail.canvas.lifecycle.unavailable"
        }
    }

    private func lifecycleTone(for status: EventStatus) -> EventDetailInvitationCanvasLifecycleTone {
        switch status {
        case .draft: return .draft
        case .polling: return .polling
        case .confirmed: return .confirmed
        case .comparing: return .comparing
        case .organizing: return .organizing
        case .finalized: return .finalized
        default: return .unavailable
        }
    }

    private func actionTitleKey(for action: EventDetailInvitationCanvasAction) -> String {
        switch action {
        case .editDraft: return "event.detail.canvas.action.edit_draft"
        case .submitVote: return "event.detail.canvas.action.submit_vote"
        case .viewPollResults: return "event.detail.canvas.action.view_results"
        case .compareOptions: return "event.detail.canvas.action.compare_options"
        case .continueOrganization: return "event.detail.canvas.action.continue_organization"
        case .viewFinalDetails: return "event.detail.canvas.action.view_final_details"
        case .showAccessState: return "event.detail.canvas.action.show_access_state"
        case .showDetails: return "event.detail.canvas.action.show_details"
        }
    }

    private func actionSubtitleKey(for mode: EventDetailInvitationCanvasMode) -> String {
        switch mode {
        case .draftOrganizerBlocked: return "event.detail.canvas.next.draft_blocked"
        case .draftOrganizerReady: return "event.detail.canvas.next.draft_ready"
        case .draftParticipantReadOnly, .draftRestricted: return "event.detail.canvas.next.draft_read_only"
        case .pollingOrganizer: return "event.detail.canvas.next.polling_organizer"
        case .pollingResponseDue: return "event.detail.canvas.next.polling_response_due"
        case .pollingResponseSubmitted: return "event.detail.canvas.next.polling_response_submitted"
        case .pollingParticipantNeutral: return "event.detail.canvas.next.polling_neutral"
        case .pollingRestricted: return "event.detail.canvas.next.polling_restricted"
        case .confirmedOrganizer, .confirmedParticipant: return "event.detail.canvas.next.confirmed"
        case .confirmedRestricted: return "event.detail.canvas.next.confirmed_restricted"
        case .comparingOrganizer, .comparingParticipant: return "event.detail.canvas.next.comparing"
        case .comparingRestricted: return "event.detail.canvas.next.comparing_restricted"
        case .organizingOrganizer, .organizingParticipant: return "event.detail.canvas.next.organizing"
        case .organizingRestricted: return "event.detail.canvas.next.organizing_restricted"
        case .finalizedReadOnly: return "event.detail.canvas.next.finalized"
        case .finalizedRestrictedReadOnly: return "event.detail.canvas.next.finalized_restricted"
        case .unsupported: return "event.detail.canvas.next.unavailable"
        }
    }

    private func actionSystemImage(for action: EventDetailInvitationCanvasAction) -> String {
        switch action {
        case .editDraft: return "pencil"
        case .submitVote: return "checklist.checked"
        case .viewPollResults: return "chart.bar.fill"
        case .compareOptions: return "map.fill"
        case .continueOrganization: return "checklist"
        case .viewFinalDetails: return "checkmark.seal.fill"
        case .showAccessState: return "lock.fill"
        case .showDetails: return "list.bullet.rectangle"
        }
    }
}

struct EventDetailInvitationCanvasMenuAction: Identifiable {
    let id: String
    let title: String
    let systemImage: String
    let action: () -> Void
}

enum EventDetailInvitationCanvasHeroCropPolicy: Equatable {
    case center
    case trailing
    case topTrailing

    var alignment: Alignment {
        switch self {
        case .center: return .center
        case .trailing: return .trailing
        case .topTrailing: return .topTrailing
        }
    }
}

struct EventDetailInvitationCanvas: View {
    @Environment(\.accessibilityReduceTransparency) private var systemReduceTransparency
    @Environment(\.wakeveAccessibilityReduceTransparencyOverride) private var reduceTransparencyOverride
    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.colorSchemeContrast) private var colorSchemeContrast
    @Environment(\.dynamicTypeSize) private var dynamicTypeSize
    @ScaledMetric(relativeTo: .largeTitle) private var editorialTitleSize: CGFloat = 52

    let event: Event
    let artwork: any Artwork
    let dateText: String
    let organizerName: String?
    let presentation: EventDetailInvitationCanvasPresentation
    let safeAreaTop: CGFloat
    let viewportWidth: CGFloat
    let menuActions: [EventDetailInvitationCanvasMenuAction]
    let onBack: () -> Void
    let onShare: (EventDetailInvitationServerIssuedSharePayload) -> Void
    let onPrimaryAction: () -> Void

    private var reduceTransparency: Bool {
        systemReduceTransparency || reduceTransparencyOverride
    }

    var body: some View {
        ZStack(alignment: .top) {
            ZStack {
                heroBackground
                readableScrim
            }
            .ignoresSafeArea(edges: .top)

            VStack(alignment: .leading, spacing: WakeveTheme.Spacing.lg) {
                Spacer(minLength: dynamicTypeSize.isAccessibilitySize ? 160 : 300)

                titleAndState
                organizerRow
                participantContext
                nextActionSurface
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.horizontal, WakeveTheme.Spacing.lg)
            .padding(.bottom, WakeveTheme.Spacing.xl)

            chromeControls
                .padding(.horizontal, WakeveTheme.Spacing.lg)
                .padding(.top, max(WakeveTheme.Spacing.sm, safeAreaTop + WakeveTheme.Spacing.sm))
        }
        .frame(width: viewportWidth, alignment: .leading)
        .frame(minHeight: dynamicTypeSize.isAccessibilitySize ? 720 : 790)
        .clipped()
        .background(EventMoodPalette.palette(for: event.eventType.name).primary(for: colorScheme))
        .accessibilityElement(children: .contain)
    }

    private var heroBackground: some View {
        productionHeroBackground
    }

    private var productionHeroBackground: some View {
        GeometryReader { geometry in
            InvitationArtworkView(
                artwork: artwork,
                event: event
            )
            .frame(width: geometry.size.width, height: geometry.size.height)
            .clipped()
        }
    }

    private var readableScrim: some View {
        let opaqueLayerOpacity: Double = reduceTransparency ? 0.46 :
            (colorSchemeContrast == .increased ? 0.72 : 0.20)

        return ZStack {
            Color.black.opacity(opaqueLayerOpacity)

            LinearGradient(
                colors: [
                    .black.opacity(colorSchemeContrast == .increased ? 0.54 : 0.18),
                    .clear,
                    .black.opacity(colorSchemeContrast == .increased ? 0.94 : 0.82)
                ],
                startPoint: .top,
                endPoint: .bottom
            )
            LinearGradient(
                colors: [.clear, EventMoodPalette.palette(for: event.eventType.name).primary(for: .dark).opacity(0.74)],
                startPoint: .center,
                endPoint: .bottom
            )
        }
        .accessibilityHidden(true)
    }

    private var titleAndState: some View {
        VStack(alignment: .leading, spacing: WakeveTheme.Spacing.xs) {
            Text(event.title)
                .font(.system(size: editorialTitleSize, weight: .medium, design: .serif))
                .foregroundStyle(.white)
                .lineLimit(dynamicTypeSize.isAccessibilitySize ? nil : 3)
                .minimumScaleFactor(dynamicTypeSize.isAccessibilitySize ? 1 : 0.82)
                .fixedSize(horizontal: false, vertical: true)
                .accessibilitySortPriority(60)

            ViewThatFits(in: .horizontal) {
                HStack(alignment: .firstTextBaseline, spacing: WakeveTheme.Spacing.xs) {
                    Text(dateText)
                        .font(WakeveTheme.Typography.bodySemibold)
                        .foregroundStyle(.white)

                    Text("·")
                        .foregroundStyle(.white.opacity(0.62))
                        .accessibilityHidden(true)

                    Text(String(localized: localizedKey(presentation.lifecycleLabelKey)))
                        .font(WakeveTheme.Typography.bodySemibold)
                        .foregroundStyle(lifecycleColor)
                }

                VStack(alignment: .leading, spacing: WakeveTheme.Spacing.xxs) {
                    Text(dateText)
                        .font(WakeveTheme.Typography.bodySemibold)
                        .foregroundStyle(.white)

                    Text(String(localized: localizedKey(presentation.lifecycleLabelKey)))
                        .font(WakeveTheme.Typography.bodySemibold)
                        .foregroundStyle(lifecycleColor)
                }
            }
            .padding(.horizontal, WakeveTheme.Spacing.sm)
            .padding(.vertical, WakeveTheme.Spacing.xs)
            .background(
                Color.black.opacity(colorSchemeContrast == .increased ? 0.92 : 0.58),
                in: Capsule()
            )
            .fixedSize(horizontal: false, vertical: true)
            .accessibilityElement(children: .combine)
            .accessibilitySortPriority(70)

            syncAndFreshnessDecoration
        }
        .padding(WakeveTheme.Spacing.md)
        .background(
            WakeveTheme.ColorToken.midnightElevated,
            in: RoundedRectangle(cornerRadius: WakeveTheme.Radius.panel, style: .continuous)
        )
        .invitationAccessibilityIdentifier("eventDetailInvitationTitlePlate")
    }

    @ViewBuilder
    private var syncAndFreshnessDecoration: some View {
        switch presentation.syncDecoration {
        case .pending:
            Label(String(localized: "event.detail.canvas.sync_pending"), systemImage: "arrow.triangle.2.circlepath")
                .font(WakeveTheme.Typography.caption)
                .foregroundStyle(.white)
                .padding(.horizontal, WakeveTheme.Spacing.sm)
                .padding(.vertical, WakeveTheme.Spacing.xs)
                .background(WakeveTheme.ColorToken.midnightElevated, in: Capsule())
                .accessibilityElement(children: .combine)
                .accessibilitySortPriority(65)
        case .synced:
            Label(String(localized: "event.detail.canvas.sync_confirmed"), systemImage: "checkmark.icloud.fill")
                .font(WakeveTheme.Typography.caption)
                .foregroundStyle(.white)
                .padding(.horizontal, WakeveTheme.Spacing.sm)
                .padding(.vertical, WakeveTheme.Spacing.xs)
                .background(WakeveTheme.ColorToken.midnightElevated, in: Capsule())
                .accessibilityElement(children: .combine)
                .accessibilitySortPriority(65)
        case .none:
            EmptyView()
        }

        if presentation.auxiliaryFreshness == .stale {
            Label(String(localized: "event.detail.canvas.last_known"), systemImage: "clock.arrow.circlepath")
                .font(WakeveTheme.Typography.caption)
                .foregroundStyle(.white.opacity(0.70))
                .accessibilitySortPriority(64)
        }
    }

    @ViewBuilder
    private var organizerRow: some View {
        if let organizerName, !organizerName.isEmpty {
            HStack(spacing: WakeveTheme.Spacing.sm) {
                EventDetailInvitationInitialsAvatar(
                    initials: initials(for: organizerName),
                    size: 42,
                    status: nil
                )

                Text(String(localized: "event.detail.canvas.organized_by"))
                    .foregroundStyle(.white.opacity(0.66))
                + Text(" \(organizerName)")
                    .foregroundStyle(.white)
                    .fontWeight(.semibold)
            }
            .font(WakeveTheme.Typography.callout)
            .accessibilityElement(children: .combine)
            .accessibilitySortPriority(50)
        }
    }

    @ViewBuilder
    private var participantContext: some View {
        switch presentation.participantData {
        case .available(let snapshot):
            VStack(alignment: .leading, spacing: WakeveTheme.Spacing.sm) {
                EventDetailInvitationParticipantStrip(snapshot: snapshot)

                HStack(spacing: WakeveTheme.Spacing.xs) {
                    Text(
                        String(
                            format: String(localized: "event.detail.canvas.participants.confirmed_format"),
                            snapshot.confirmedCount
                        )
                    )
                    .foregroundStyle(WakeveTheme.ColorToken.confirmationBase)

                    if snapshot.pendingCount > 0 {
                        Text("·")
                            .foregroundStyle(.white.opacity(0.56))
                            .accessibilityHidden(true)
                        Text(
                            String(
                                format: String(localized: "event.detail.canvas.participants.pending_format"),
                                snapshot.pendingCount
                            )
                        )
                        .foregroundStyle(WakeveTheme.ColorToken.warmAmber)
                    }
                }
                .font(WakeveTheme.Typography.callout)
                .accessibilityElement(children: .combine)
            }
            .accessibilitySortPriority(40)
        case .loading:
            Label(String(localized: "event.detail.canvas.participants.loading"), systemImage: "person.2.fill")
                .font(WakeveTheme.Typography.callout)
                .foregroundStyle(.white.opacity(0.72))
                .accessibilitySortPriority(40)
        case .unavailable, .failed:
            Label(String(localized: "event.detail.canvas.participants.unavailable"), systemImage: "person.2.fill")
                .font(WakeveTheme.Typography.callout)
                .foregroundStyle(.white.opacity(0.72))
                .accessibilitySortPriority(40)
        }
    }

    @ViewBuilder
    private var nextActionSurface: some View {
        switch presentation.primaryActionPlacement {
        case .inCanvas:
            EventDetailInvitationNextActionSurface(
                presentation: presentation,
                showsPrimaryAction: true,
                onPrimaryAction: onPrimaryAction
            )
        case .persistentSafeArea:
            EventDetailInvitationNextActionSurface(
                presentation: presentation,
                showsPrimaryAction: false,
                onPrimaryAction: onPrimaryAction
            )
        }
    }

    @ViewBuilder
    private var chromeControls: some View {
        if #available(iOS 26.0, *), !reduceTransparency, colorSchemeContrast != .increased {
            GlassEffectContainer(spacing: WakeveTheme.Spacing.md) {
                chromeControlRow(usesNativeGlass: true)
            }
        } else {
            chromeControlRow(usesNativeGlass: false)
        }
    }

    private func chromeControlRow(usesNativeGlass: Bool) -> some View {
        HStack(spacing: WakeveTheme.Spacing.sm) {
            EventDetailInvitationCircleButton(
                systemImage: "chevron.left",
                accessibilityLabel: String(localized: "common.back"),
                usesNativeGlass: usesNativeGlass,
                action: onBack
            )

            Spacer(minLength: WakeveTheme.Spacing.md)

            switch presentation.shareCapability {
            case .hidden:
                EmptyView()
            case .requesting:
                EventDetailInvitationCircleProgress(usesNativeGlass: usesNativeGlass)
            case .ready(let serverIssuedPayload):
                EventDetailInvitationCircleButton(
                    systemImage: "square.and.arrow.up",
                    accessibilityLabel: String(localized: "event.detail.canvas.share"),
                    usesNativeGlass: usesNativeGlass
                ) {
                    onShare(serverIssuedPayload)
                }
            case .unavailable(let reason), .failed(let reason):
                EventDetailInvitationCircleButton(
                    systemImage: "square.and.arrow.up",
                    accessibilityLabel: String(localized: "event.detail.canvas.share_unavailable"),
                    accessibilityHint: reason,
                    isDisabled: true,
                    usesNativeGlass: usesNativeGlass,
                    action: {}
                )
            }

            Menu {
                ForEach(menuActions) { menuAction in
                    Button(action: menuAction.action) {
                        Label(menuAction.title, systemImage: menuAction.systemImage)
                    }
                    .invitationAccessibilityIdentifier(menuAction.id)
                }
            } label: {
                EventDetailInvitationCircleLabel(
                    systemImage: "ellipsis",
                    usesNativeGlass: usesNativeGlass
                )
            }
            .accessibilityLabel(String(localized: "event.detail.canvas.menu"))
            .frame(minWidth: 44, minHeight: 44)
        }
    }

    private var lifecycleColor: Color {
        switch presentation.lifecycleTone {
        case .draft: return WakeveTheme.ColorToken.warmAmber
        case .polling: return WakeveTheme.ColorToken.paleBlue
        case .confirmed, .finalized: return WakeveTheme.ColorToken.confirmationBase
        case .comparing: return WakeveTheme.ColorToken.mutedLavender
        case .organizing: return WakeveTheme.ColorToken.paleBlue
        case .unavailable: return .white.opacity(0.68)
        }
    }

    private func initials(for value: String) -> String {
        let components = value
            .replacingOccurrences(of: "@", with: " ")
            .replacingOccurrences(of: ".", with: " ")
            .split(separator: " ")

        if let first = components.first?.first,
           let second = components.dropFirst().first?.first {
            return "\(first)\(second)".uppercased()
        }
        return String(value.prefix(2)).uppercased()
    }

    private func localizedKey(_ value: String) -> String.LocalizationValue {
        String.LocalizationValue(value)
    }
}

struct EventDetailInvitationCanvasPersistentAction: View {
    @Environment(\.accessibilityReduceTransparency) private var systemReduceTransparency
    @Environment(\.wakeveAccessibilityReduceTransparencyOverride) private var reduceTransparencyOverride
    @Environment(\.colorSchemeContrast) private var colorSchemeContrast

    let presentation: EventDetailInvitationCanvasPresentation
    let action: () -> Void

    private var reduceTransparency: Bool {
        systemReduceTransparency || reduceTransparencyOverride
    }

    @ViewBuilder
    var body: some View {
        if reduceTransparency || colorSchemeContrast == .increased {
            actionContent
                .background(WakeveTheme.ColorToken.midnightElevated)
        } else if #available(iOS 26.0, *) {
            actionContent
                .glassEffect(.regular, in: .rect(cornerRadius: WakeveTheme.Radius.panel))
        } else {
            actionContent
                .background(.regularMaterial)
        }
    }

    private var actionContent: some View {
        EventDetailInvitationPrimaryActionButton(
            titleKey: presentation.primaryActionTitleKey,
            systemImage: presentation.primaryActionSystemImage,
            action: action
        )
        .padding(.horizontal, WakeveTheme.Spacing.page)
        .padding(.vertical, WakeveTheme.Spacing.sm)
        .accessibilityIdentifier("eventDetailInvitationCanvasPersistentAction")
    }
}

private struct EventDetailInvitationNextActionSurface: View {
    let presentation: EventDetailInvitationCanvasPresentation
    let showsPrimaryAction: Bool
    let onPrimaryAction: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: WakeveTheme.Spacing.md) {
            HStack(alignment: .top, spacing: WakeveTheme.Spacing.md) {
                Image(systemName: presentation.primaryActionSystemImage)
                    .font(.system(size: 22, weight: .semibold))
                    .foregroundStyle(.white)
                    .frame(width: 52, height: 52)
                    .background(.white.opacity(0.16), in: Circle())
                    .accessibilityHidden(true)

                VStack(alignment: .leading, spacing: WakeveTheme.Spacing.xxs) {
                    Text(String(localized: "event.detail.canvas.next_step"))
                        .font(WakeveTheme.Typography.caption)
                        .foregroundStyle(.white.opacity(0.76))
                        .textCase(.uppercase)

                    Text(String(localized: localizedKey(presentation.primaryActionSubtitleKey)))
                        .font(WakeveTheme.Typography.section)
                        .foregroundStyle(.white)
                        .fixedSize(horizontal: false, vertical: true)

                    if let responsibilityText {
                        Label(responsibilityText, systemImage: responsibilitySymbol)
                            .font(WakeveTheme.Typography.callout)
                            .foregroundStyle(.white.opacity(0.84))
                            .fixedSize(horizontal: false, vertical: true)
                    }
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .layoutPriority(1)
            }
            .frame(maxWidth: .infinity, alignment: .leading)

            if showsPrimaryAction {
                Divider()
                    .overlay(.white.opacity(0.16))

                EventDetailInvitationPrimaryActionButton(
                    titleKey: presentation.primaryActionTitleKey,
                    systemImage: nil,
                    action: onPrimaryAction
                )
                .accessibilityIdentifier("eventDetailInvitationCanvasInCanvasAction")
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(WakeveTheme.Spacing.lg)
        .background(
            WakeveTheme.ColorToken.midnightElevated,
            in: RoundedRectangle(cornerRadius: WakeveTheme.Radius.panel, style: .continuous)
        )
        .accessibilitySortPriority(30)
    }

    private var responsibilityText: String? {
        switch presentation.responsibleActor {
        case .currentUser: return String(localized: "event.detail.canvas.responsibility.current_user")
        case .organizer: return String(localized: "event.detail.canvas.responsibility.organizer")
        case .group: return String(localized: "event.detail.canvas.responsibility.group")
        case .none, .unavailable: return nil
        }
    }

    private var responsibilitySymbol: String {
        presentation.responsibleActor == .currentUser ? "person.fill" : "person.2.fill"
    }

    private func localizedKey(_ value: String) -> String.LocalizationValue {
        String.LocalizationValue(value)
    }
}

private struct EventDetailInvitationPrimaryActionButton: View {
    @Environment(\.accessibilityReduceTransparency) private var systemReduceTransparency
    @Environment(\.wakeveAccessibilityReduceTransparencyOverride) private var reduceTransparencyOverride
    @Environment(\.colorSchemeContrast) private var colorSchemeContrast
    @Environment(\.dynamicTypeSize) private var dynamicTypeSize

    let titleKey: String
    let systemImage: String?
    let action: () -> Void

    private var reduceTransparency: Bool {
        systemReduceTransparency || reduceTransparencyOverride
    }

    @ViewBuilder
    var body: some View {
        if #available(iOS 26.0, *), !reduceTransparency, colorSchemeContrast != .increased {
            buttonLabel
                .buttonStyle(.glassProminent)
                .tint(WakeveTheme.ColorToken.paleBlue)
        } else {
            buttonLabel
                .buttonStyle(.plain)
                .background(fallbackBackground, in: Capsule())
                .overlay(Capsule().stroke(.white.opacity(0.28), lineWidth: 1))
        }
    }

    private var buttonLabel: some View {
        Button(action: action) {
            HStack(spacing: WakeveTheme.Spacing.xs) {
                if let systemImage {
                    Image(systemName: systemImage)
                        .font(.body.weight(.semibold))
                }
                Text(String(localized: String.LocalizationValue(titleKey)))
                    .font(WakeveTheme.Typography.bodySemibold)
                    .multilineTextAlignment(.center)
            }
            .foregroundStyle(WakeveTheme.ColorToken.midnight)
            .frame(maxWidth: .infinity)
            .frame(minHeight: dynamicTypeSize.isAccessibilitySize ? 62 : 54)
            .padding(.horizontal, WakeveTheme.Spacing.md)
            .contentShape(Capsule())
        }
        .frame(minHeight: 44)
    }

    private var fallbackBackground: LinearGradient {
        LinearGradient(
            colors: [WakeveTheme.ColorToken.paleBlue, WakeveTheme.ColorToken.mutedLavender],
            startPoint: .leading,
            endPoint: .trailing
        )
    }
}

private struct EventDetailInvitationGlassSurface: ViewModifier {
    @Environment(\.accessibilityReduceTransparency) private var systemReduceTransparency
    @Environment(\.wakeveAccessibilityReduceTransparencyOverride) private var reduceTransparencyOverride
    @Environment(\.colorSchemeContrast) private var colorSchemeContrast

    let cornerRadius: CGFloat

    private var reduceTransparency: Bool {
        systemReduceTransparency || reduceTransparencyOverride
    }

    @ViewBuilder
    func body(content: Content) -> some View {
        if #available(iOS 26.0, *), !reduceTransparency, colorSchemeContrast != .increased {
            content
                .background(.black.opacity(0.14))
                .glassEffect(.regular, in: .rect(cornerRadius: cornerRadius))
        } else if reduceTransparency || colorSchemeContrast == .increased {
            content
                .background(WakeveTheme.ColorToken.midnightElevated)
                .overlay(
                    RoundedRectangle(cornerRadius: cornerRadius, style: .continuous)
                        .stroke(.white.opacity(0.32), lineWidth: 1)
                )
                .clipShape(RoundedRectangle(cornerRadius: cornerRadius, style: .continuous))
        } else {
            content
                .background(.ultraThinMaterial)
                .overlay(
                    RoundedRectangle(cornerRadius: cornerRadius, style: .continuous)
                        .stroke(.white.opacity(0.22), lineWidth: 1)
                )
                .clipShape(RoundedRectangle(cornerRadius: cornerRadius, style: .continuous))
        }
    }
}

private struct EventDetailInvitationCircleButton: View {
    let systemImage: String
    let accessibilityLabel: String
    var accessibilityHint: String = ""
    var isDisabled = false
    let usesNativeGlass: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            EventDetailInvitationCircleLabel(
                systemImage: systemImage,
                usesNativeGlass: usesNativeGlass
            )
        }
        .buttonStyle(.plain)
        .disabled(isDisabled)
        .opacity(isDisabled ? 0.62 : 1)
        .accessibilityLabel(accessibilityLabel)
        .accessibilityHint(accessibilityHint)
        .frame(minWidth: 44, minHeight: 44)
    }
}

private struct EventDetailInvitationCircleLabel: View {
    @Environment(\.accessibilityReduceTransparency) private var systemReduceTransparency
    @Environment(\.wakeveAccessibilityReduceTransparencyOverride) private var reduceTransparencyOverride
    @Environment(\.colorSchemeContrast) private var colorSchemeContrast

    let systemImage: String
    let usesNativeGlass: Bool

    private var reduceTransparency: Bool {
        systemReduceTransparency || reduceTransparencyOverride
    }

    @ViewBuilder
    var body: some View {
        if #available(iOS 26.0, *), usesNativeGlass, !reduceTransparency, colorSchemeContrast != .increased {
            label
                .glassEffect(.regular.interactive(), in: .circle)
        } else {
            label
                .background(fallbackBackground, in: Circle())
                .overlay(Circle().stroke(.white.opacity(0.20), lineWidth: 1))
        }
    }

    private var label: some View {
        Image(systemName: systemImage)
            .font(.system(size: 18, weight: .semibold))
            .foregroundStyle(.white)
            .frame(width: 48, height: 48)
            .contentShape(Circle())
    }

    private var fallbackBackground: AnyShapeStyle {
        if reduceTransparency || colorSchemeContrast == .increased {
            return AnyShapeStyle(WakeveTheme.ColorToken.midnightElevated)
        }
        return AnyShapeStyle(.ultraThinMaterial)
    }
}

private struct EventDetailInvitationCircleProgress: View {
    @Environment(\.accessibilityReduceTransparency) private var systemReduceTransparency
    @Environment(\.wakeveAccessibilityReduceTransparencyOverride) private var reduceTransparencyOverride
    @Environment(\.colorSchemeContrast) private var colorSchemeContrast

    let usesNativeGlass: Bool

    private var reduceTransparency: Bool {
        systemReduceTransparency || reduceTransparencyOverride
    }

    @ViewBuilder
    var body: some View {
        if reduceTransparency || colorSchemeContrast == .increased {
            progress
                .background(WakeveTheme.ColorToken.midnightElevated, in: Circle())
                .overlay(Circle().stroke(.white.opacity(0.28), lineWidth: 1))
        } else if #available(iOS 26.0, *), usesNativeGlass {
            progress
                .glassEffect(.regular, in: .circle)
        } else {
            progress
                .background(.regularMaterial, in: Circle())
                .overlay(Circle().stroke(.white.opacity(0.20), lineWidth: 1))
        }
    }

    private var progress: some View {
        ProgressView()
            .tint(.white)
            .frame(width: 48, height: 48)
            .accessibilityLabel(String(localized: "event.detail.canvas.share_requesting"))
            .frame(minWidth: 44, minHeight: 44)
    }
}

private struct EventDetailInvitationParticipantStrip: View {
    @Environment(\.dynamicTypeSize) private var dynamicTypeSize

    let snapshot: EventDetailInvitationCanvasParticipantSnapshot

    var body: some View {
        HStack(spacing: -10) {
            ForEach(Array(snapshot.identities.prefix(maxVisible))) { identity in
                EventDetailInvitationInitialsAvatar(
                    initials: identity.initials,
                    size: 44,
                    status: identity.status
                )
                .accessibilityLabel(identity.accessibilityName)
            }

            if snapshot.identities.count > maxVisible {
                Text("+\(snapshot.identities.count - maxVisible)")
                    .font(WakeveTheme.Typography.callout.weight(.semibold))
                    .foregroundStyle(.white.opacity(0.82))
                    .frame(width: 44, height: 44)
                    .background(WakeveTheme.ColorToken.mutedLavender.opacity(0.42), in: Circle())
                    .overlay(Circle().stroke(.white.opacity(0.28), lineWidth: 1))
                    .accessibilityLabel(
                        String(
                            format: String(localized: "event.detail.canvas.participants.additional_format"),
                            snapshot.identities.count - maxVisible
                        )
                    )
            }
        }
    }

    private var maxVisible: Int {
        dynamicTypeSize.isAccessibilitySize ? 4 : 7
    }
}

private struct EventDetailInvitationInitialsAvatar: View {
    let initials: String
    let size: CGFloat
    let status: EventDetailInvitationCanvasParticipantStatus?

    var body: some View {
        Text(initials.isEmpty ? "?" : initials)
            .font(.system(size: size * 0.32, weight: .bold, design: .rounded))
            .foregroundStyle(.white)
            .frame(width: size, height: size)
            .background(
                LinearGradient(
                    colors: [WakeveTheme.ColorToken.mutedLavender, WakeveTheme.ColorToken.permissionBlue],
                    startPoint: .topLeading,
                    endPoint: .bottomTrailing
                ),
                in: Circle()
            )
            .overlay(Circle().stroke(.white.opacity(0.68), lineWidth: 1.5))
            .overlay(alignment: .bottomTrailing) {
                if let status {
                    statusBadge(status)
                }
            }
    }

    @ViewBuilder
    private func statusBadge(
        _ status: EventDetailInvitationCanvasParticipantStatus
    ) -> some View {
        switch status {
        case .confirmed:
            Image(systemName: "checkmark")
                .font(.system(size: size * 0.14, weight: .black))
                .foregroundStyle(WakeveTheme.ColorToken.midnight)
                .frame(width: size * 0.34, height: size * 0.34)
                .background(WakeveTheme.ColorToken.confirmationBase, in: Circle())
                .overlay(Circle().stroke(WakeveTheme.ColorToken.midnight, lineWidth: 2))
                .accessibilityHidden(true)
        case .pending:
            Image(systemName: "clock.fill")
                .font(.system(size: size * 0.14, weight: .bold))
                .foregroundStyle(WakeveTheme.ColorToken.midnight)
                .frame(width: size * 0.34, height: size * 0.34)
                .background(WakeveTheme.ColorToken.warmAmber, in: Circle())
                .overlay(Circle().stroke(WakeveTheme.ColorToken.midnight, lineWidth: 2))
                .accessibilityHidden(true)
        }
    }
}

#if DEBUG
private enum EventDetailInvitationCanvasPreviewFixture {
    static let assetName = "AnnecyEventHero"
    static let dateText = "18–20 sept. 2026"
    static let artwork: any Artwork = ArtworkStructured(
        version: 1,
        ref: ArtworkRef(
            source: ArtworkSourcePreset(presetId: "wakeve-lake"),
            alt: ArtworkAltDecorative.shared,
            focalPoint: ArtworkFocalPoint(x: 0.82, y: 0.42),
            crop: .fill
        )
    )
    static let menuActions = [
        EventDetailInvitationCanvasMenuAction(
            id: "preview-menu",
            title: "Gérer l’événement",
            systemImage: "slider.horizontal.3",
            action: {}
        )
    ]

    static let event = EventFactory.make(
        id: "preview-annecy-invitation-canvas",
        title: "Un week-end à Annecy",
        description: "Une invitation confirmée entre lac et montagne.",
        organizerId: "lea.martin@wakeve.preview",
        participants: [
            "lea.martin@wakeve.preview",
            "alex.bernard@wakeve.preview",
            "ines.dupont@wakeve.preview",
            "sam.roy@wakeve.preview",
            "noa.moreau@wakeve.preview",
            "lina.petit@wakeve.preview"
        ],
        status: .confirmed,
        finalDate: "2026-09-18T16:00:00Z",
        eventType: .outdoorActivity,
        expectedParticipants: 6,
        heroImageUrl: nil
    )

    static let participantData = EventDetailInvitationCanvasParticipantData.available(
        EventDetailInvitationCanvasParticipantSnapshot(
            confirmedCount: 4,
            pendingCount: 2,
            identities: [
                .init(id: "lea", initials: "LM", accessibilityName: "Léa Martin", status: .confirmed),
                .init(id: "alex", initials: "AB", accessibilityName: "Alex Bernard", status: .confirmed),
                .init(id: "ines", initials: "ID", accessibilityName: "Inès Dupont", status: .confirmed),
                .init(id: "sam", initials: "SR", accessibilityName: "Sam Roy", status: .confirmed),
                .init(id: "noa", initials: "NM", accessibilityName: "Noa Moreau", status: .pending),
                .init(id: "lina", initials: "LP", accessibilityName: "Lina Petit", status: .pending)
            ]
        )
    )

    static let shareBinding = EventDetailInvitationShareBinding(
        eventId: event.id,
        actorId: event.organizerId,
        accessRevision: 1,
        capabilityId: "preview-annecy-share-capability"
    )

    static let presentation = EventDetailInvitationCanvasMapper().map(
        EventDetailInvitationCanvasMapper.Input(
            eventStatus: .confirmed,
            hasRequiredSlots: true,
            currentUserAccess: .organizer,
            currentUserVote: .notApplicable,
            participantData: participantData,
            responsibility: .currentUser,
            readinessData: .unavailable,
            availableActions: [.compareOptions],
            relevantSync: .synced(subject: .event(id: event.id)),
            shareCapability: .ready(
                serverIssuedPayload: .init(
                    opaqueValue: "preview-server-issued-share-reference",
                    binding: shareBinding
                )
            ),
            heroImageState: .available,
            auxiliaryFreshness: .current,
            requestedSecondarySections: [],
            primaryActionPlacement: .inCanvas,
            shareValidationContext: shareBinding
        )
    )
}

struct EventDetailInvitationCanvasQAView: View {
    var body: some View {
        GeometryReader { geometry in
            ScrollView(.vertical) {
                EventDetailInvitationCanvas(
                    event: EventDetailInvitationCanvasPreviewFixture.event,
                    artwork: EventDetailInvitationCanvasPreviewFixture.artwork,
                    dateText: EventDetailInvitationCanvasPreviewFixture.dateText,
                    organizerName: "Léa Martin",
                    presentation: EventDetailInvitationCanvasPreviewFixture.presentation,
                    safeAreaTop: geometry.safeAreaInsets.top,
                    viewportWidth: geometry.size.width,
                    menuActions: EventDetailInvitationCanvasPreviewFixture.menuActions,
                    onBack: {},
                    onShare: { _ in },
                    onPrimaryAction: {}
                )
            }
            .scrollIndicators(.hidden)
            .accessibilityIdentifier(EventDetailInvitationCanvasPreviewFixture.assetName)
            .ignoresSafeArea()
        }
        .preferredColorScheme(.dark)
    }
}

#Preview("Invitation Canvas — Annecy confirmed") {
    EventDetailInvitationCanvasQAView()
}
#endif
