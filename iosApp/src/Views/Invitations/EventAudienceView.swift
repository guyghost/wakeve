import SwiftUI
import Shared

enum EventAudienceSubmissionError: Equatable {
    case securityUnavailable
    case invalidRecipient
    case submissionFailed

    var localizedMessage: String {
        switch self {
        case .securityUnavailable:
            String(localized: "invitation.audience.error.security_unavailable")
        case .invalidRecipient:
            String(localized: "invitation.audience.error.invalid_recipient")
        case .submissionFailed:
            String(localized: "invitation.audience.error.submit_failed")
        }
    }
}

@MainActor
final class EventAudienceViewModel: ObservableObject {
    @Published private(set) var projection: AudienceProjection
    @Published private(set) var inviteEnabled = false
    @Published private(set) var displayNamesByIdentityKey: [String: String] = [:]
    @Published private(set) var persistedRecipientOutcomes: [any DirectInviteRecipientOutcome] = []
    @Published private(set) var actionableBatch: (any DirectInviteOperation)?
    @Published private(set) var isSubmitting = false
    @Published private(set) var submissionError: EventAudienceSubmissionError?

    let projector: AudienceProjector
    private let eventId: String
    private let repository: DatabaseEventRepository
    private let database: WakeveDb
    private let directInviteRepository: DatabaseDirectInviteBatchRepository
    private let recipientKeyOwner: DirectInviteRecipientKeyOwner?
    private let deliverySealer: (any DirectInviteDeliverySealer)?
    private let deliveryTransport: (any DirectInviteDeliveryTransport)?
    private let directInviteCapability: any DirectInviteCapability

    init(
        eventId: String,
        repository: DatabaseEventRepository = RepositoryProvider.shared.databaseRepository,
        database: WakeveDb = RepositoryProvider.shared.database,
        projector: AudienceProjector = AudienceProjector(),
        directInviteCapability: any DirectInviteCapability = DirectInviteCapabilityHidden.shared,
        recipientKeyOwner: DirectInviteRecipientKeyOwner? = nil,
        deliverySealer: (any DirectInviteDeliverySealer)? = nil,
        deliveryTransport: (any DirectInviteDeliveryTransport)? = nil
    ) {
        self.eventId = eventId
        self.repository = repository
        self.database = database
        self.directInviteRepository = DatabaseDirectInviteBatchRepository(
            database: database,
            deliveryTransport: deliveryTransport
        )
        self.recipientKeyOwner = recipientKeyOwner
        self.deliverySealer = deliverySealer
        self.deliveryTransport = deliveryTransport
        self.projector = projector
        self.directInviteCapability = directInviteCapability
        self.projection = projector.project(
            identities: [],
            freshness: FreshnessUnavailable.shared
        )
    }

    func reload() async {
        let records = repository.getParticipantRecords(eventId: eventId) ?? []
        displayNamesByIdentityKey = Dictionary(uniqueKeysWithValues: records.compactMap { record in
            guard let name = database.userQueries
                .selectUserById(id: record.userId)
                .executeAsOneOrNull()?
                .name
                .trimmingCharacters(in: .whitespacesAndNewlines),
                  !name.isEmpty
            else { return nil }
            return (record.id, name)
        })
        var identities = records.map { record in
            let access = ParticipantAccessMapper.shared.fromRepositoryRecord(record: record)
            let rsvp: RsvpState = switch access.rsvp {
            case .accepted: .accepted
            case .declined: .declined
            case .pending: .pending
            case .notApplicable: .notApplicable
            case .unavailable: .unavailable
            default: .unavailable
            }
            let membership: any MembershipState = switch access.role {
            case .organizer, .member: MembershipStateActiveMember(memberId: record.id)
            case .nonMember: MembershipStateNonMember.shared
            default: MembershipStateNonMember.shared
            }
            let dateValidation: DateValidationState_ = switch access.dateValidation {
            case .validatedRetainedDate: .validatedRetainedDate
            case .notValidated: .notValidated
            case .notApplicable: .notApplicable
            case .unavailable: .unavailable
            default: .unavailable
            }
            return AudienceIdentityAxes(
                identityKey: record.id,
                delivery: InviteDeliveryStateNone.shared,
                approval: ApprovalStateNotApplicable.shared,
                membership: membership,
                rsvp: rsvp,
                dateValidation: dateValidation
            )
        }
        var outcomes: [any DirectInviteRecipientOutcome] = []
        var loadedOperations: [any DirectInviteOperation] = []
        let batches = database.invitationExperienceQueries
            .selectDirectInviteBatchesByEventId(event_id: eventId)
            .executeAsList()
        for batch in batches {
            // Loading through the owner applies expiry/retention checks before the
            // protected recipient snapshot can be projected by the UI.
            guard let loaded = try? await directInviteRepository.load(batchId: batch.batch_id) else {
                continue
            }
            loadedOperations.append(loaded)
            for row in database.invitationExperienceQueries
                .selectDirectInviteRecipientOutcomes(batch_id: batch.batch_id)
                .executeAsList() {
                let projected = persistedOutcome(
                    status: row.status,
                    invitationId: row.invitation_id,
                    reason: row.reason_code,
                    operationId: batch.operation_id
                )
                identities.append(
                    AudienceIdentityAxes(
                        identityKey: row.recipient_key,
                        delivery: projected.delivery,
                        approval: ApprovalStateNotApplicable.shared,
                        membership: MembershipStateNonMember.shared,
                        rsvp: .notApplicable,
                        dateValidation: .notApplicable
                    )
                )
                if let outcome = projected.outcome { outcomes.append(outcome) }
            }
        }
        persistedRecipientOutcomes = outcomes
        actionableBatch = loadedOperations.reversed().first(where: { operation in
            operation is DirectInviteOperationFailed ||
                operation is DirectInviteOperationPendingSync ||
                operation is DirectInviteOperationSubmitting
        })
        projection = projector.project(
            identities: identities,
            freshness: FreshnessCurrent.shared
        )
        guard let event = repository.getEvent(id: eventId),
              let capability = directInviteCapability as? DirectInviteCapabilityReady,
              let aggregateRevision = database.eventQueries
                .selectById(id: eventId)
                .executeAsOneOrNull()?
                .aggregateRevision
        else {
            inviteEnabled = false
            return
        }
        inviteEnabled = event.status == .draft &&
            event.id == capability.eventId &&
            event.organizerId == capability.actorId &&
            aggregateRevision == capability.accessRevision &&
            capability.allowedEventStatuses.contains(.draft) &&
            recipientKeyOwner != nil &&
            deliverySealer != nil &&
            deliveryTransport != nil
    }

    private func persistedOutcome(
        status: String,
        invitationId: String?,
        reason: String?,
        operationId: String
    ) -> (delivery: any InviteDeliveryState, outcome: (any DirectInviteRecipientOutcome)?) {
        switch status {
        case "SERVER_ACCEPTED", "DELIVERED":
            guard let invitationId, !invitationId.isEmpty else {
                let failed = DirectInviteRecipientOutcomeFailed(error: .permanentFailure)
                return (
                    InviteDeliveryStateFailedBeforeServer(
                        operationId: operationId,
                        error: .permanentFailure
                    ),
                    failed
                )
            }
            let delivery: any InviteDeliveryState = status == "DELIVERED"
                ? InviteDeliveryStateDelivered(invitationId: invitationId)
                : InviteDeliveryStateServerAccepted(invitationId: invitationId)
            return (
                delivery,
                DirectInviteRecipientOutcomeServerAccepted(invitationId: invitationId)
            )
        case "INVALID":
            return (
                InviteDeliveryStateFailedBeforeServer(operationId: operationId, error: .validation),
                DirectInviteRecipientOutcomeInvalid(reason: reason ?? "VALIDATION")
            )
        case "CANCELLED":
            return (InviteDeliveryStateNone.shared, DirectInviteRecipientOutcomeCancelled.shared)
        case "QUEUED_LOCAL":
            return (InviteDeliveryStateQueuedLocal(operationId: operationId), nil)
        default:
            let failed = DirectInviteRecipientOutcomeFailed(error: .networkUnavailable)
            return (
                InviteDeliveryStateFailedBeforeServer(
                    operationId: operationId,
                    error: .networkUnavailable
                ),
                failed
            )
        }
    }

    func displayName(for identityKey: String) -> String {
        displayNamesByIdentityKey[identityKey] ??
            String(localized: "invitation.audience.identity.invitation")
    }

    var inviteDisabledReason: String? {
        guard !inviteEnabled else { return nil }
        if recipientKeyOwner == nil ||
            directInviteCapability is DirectInviteCapabilityHidden ||
            directInviteCapability is DirectInviteCapabilityUnavailable {
            return String(localized: "invitation.audience.error.security_unavailable")
        }
        return String(localized: "invitation.state.unavailable")
    }

    func submitRecipient(_ rawRecipientInput: String) async -> Bool {
        submissionError = nil
        guard !isSubmitting else { return false }
        guard inviteEnabled, let event = repository.getEvent(id: eventId) else {
            submissionError = recipientKeyOwner == nil
                ? .securityUnavailable
                : .submissionFailed
            return false
        }
        guard let recipientKeyOwner, let deliverySealer, deliveryTransport != nil else {
            submissionError = .securityUnavailable
            return false
        }

        isSubmitting = true
        defer { isSubmitting = false }
        let operationId = UUID().uuidString.lowercased()
        let batchId = "direct-invite-\(UUID().uuidString.lowercased())"
        guard let capability = directInviteCapability as? DirectInviteCapabilityReady else {
            submissionError = .securityUnavailable
            return false
        }
        let binding = DirectInviteDeliveryBinding(
            eventId: event.id,
            actorId: event.organizerId,
            accessRevision: capability.accessRevision,
            batchId: batchId,
            operationId: operationId
        )
        let expiresAt = ISO8601DateFormatter().string(
            from: Date().addingTimeInterval(29 * 24 * 60 * 60)
        )
        guard let protectedRecipient = recipientKeyOwner.protectAndSeal(
            rawRecipientInput: rawRecipientInput,
            binding: binding,
            expiresAt: expiresAt,
            sealer: deliverySealer
        ) else {
            submissionError = .invalidRecipient
            return false
        }
        let deliveryEnvelopes = Set([protectedRecipient.envelope])
        let result = try? await directInviteRepository.submit(
            command: SubmitDirectInviteBatchCommand(
                eventId: event.id,
                actorId: event.organizerId,
                eventStatus: event.status,
                batchId: batchId,
                operationId: operationId,
                recipientKeys: Set([protectedRecipient.recipientKey]),
                capability: directInviteCapability
            ),
            deliveryEnvelopes: deliveryEnvelopes
        )
        await reload()
        let succeeded = result is DirectInviteOperationPendingSync ||
            result is DirectInviteOperationSubmitting ||
            result is DirectInviteOperationCompleted
        if !succeeded {
            submissionError = .submissionFailed
        }
        return succeeded
    }

    var canRetryDirectInviteBatch: Bool {
        actionableBatch is DirectInviteOperationFailed && inviteEnabled
    }

    var canCancelDirectInviteBatch: Bool {
        guard inviteEnabled else { return false }
        return actionableBatch is DirectInviteOperationFailed ||
            actionableBatch is DirectInviteOperationPendingSync ||
            actionableBatch is DirectInviteOperationSubmitting
    }

    func retryDirectInviteBatch() async {
        guard let operation = actionableBatch as? DirectInviteOperationFailed,
              let capability = directInviteCapability as? DirectInviteCapabilityReady,
              let event = repository.getEvent(id: eventId),
              let accessRevision = database.eventQueries
                .selectById(id: eventId)
                .executeAsOneOrNull()?
                .aggregateRevision,
              event.status == .draft,
              event.id == capability.eventId,
              event.organizerId == capability.actorId,
              accessRevision == capability.accessRevision
        else {
            await reload()
            return
        }
        _ = try? await directInviteRepository.retry(
            command: RetryDirectInviteBatchCommand(
                operation: operation,
                capability: capability
            )
        )
        await reload()
    }

    func cancelDirectInviteBatch() async {
        guard let operation = actionableBatch,
              let capability = directInviteCapability as? DirectInviteCapabilityReady,
              let event = repository.getEvent(id: eventId),
              let accessRevision = database.eventQueries
                .selectById(id: eventId)
                .executeAsOneOrNull()?
                .aggregateRevision,
              event.status == .draft,
              event.id == capability.eventId,
              event.organizerId == capability.actorId,
              accessRevision == capability.accessRevision
        else {
            await reload()
            return
        }
        _ = try? await directInviteRepository.cancel(
            command: CancelDirectInviteBatchCommand(
                operation: operation,
                capability: capability
            )
        )
        await reload()
    }
}

struct EventAudienceView: View {
    @StateObject private var viewModel: EventAudienceViewModel
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @State private var recipientInput = ""

    let directInviteAvailable: Bool
    let directInviteCapability: any DirectInviteCapability
    let onInvite: () -> Void

    init(
        eventId: String,
        directInviteAvailable: Bool,
        repository: DatabaseEventRepository = RepositoryProvider.shared.databaseRepository,
        database: WakeveDb = RepositoryProvider.shared.database,
        directInviteCapability: any DirectInviteCapability = DirectInviteCapabilityHidden.shared,
        recipientKeyOwner: DirectInviteRecipientKeyOwner? = nil,
        deliverySealer: (any DirectInviteDeliverySealer)? = nil,
        deliveryTransport: (any DirectInviteDeliveryTransport)? = nil,
        onInvite: @escaping () -> Void
    ) {
        _viewModel = StateObject(
            wrappedValue: EventAudienceViewModel(
                eventId: eventId,
                repository: repository,
                database: database,
                directInviteCapability: directInviteCapability,
                recipientKeyOwner: recipientKeyOwner,
                deliverySealer: deliverySealer,
                deliveryTransport: deliveryTransport
            )
        )
        self.directInviteAvailable = directInviteAvailable
        self.directInviteCapability = directInviteCapability
        self.onInvite = onInvite
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    HStack {
                        Label(String(localized: "invitation.audience.title"), systemImage: "person.2.fill")
                            .font(.headline)
                        Spacer()
                        Text(viewModel.projection.counts.totalIdentities, format: .number)
                            .font(.headline.monospacedDigit())
                            .accessibilityLabel(
                                String(
                                    format: String(localized: "participants.count_format"),
                                    viewModel.projection.counts.totalIdentities
                                )
                            )
                    }
                    .padding(16)
                    .background(Color(uiColor: .secondarySystemBackground))
                    .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))

                    ForEach(viewModel.projection.identities, id: \.identityKey) { identity in
                        HStack(spacing: 12) {
                            Image(systemName: "person.crop.circle.fill")
                                .font(.title2)
                                .foregroundStyle(.secondary)
                            VStack(alignment: .leading, spacing: 4) {
                                Text(viewModel.displayName(for: identity.identityKey))
                                    .font(.body)
                                    .textSelection(.enabled)
                                    .fixedSize(horizontal: false, vertical: true)
                                HStack(spacing: 8) {
                                    Text(identityGroupTitle(identity))
                                        .invitationAccessibilityIdentifier("eventAudienceIdentityGroup")
                                    Text(identityStatusTitle(identity))
                                        .invitationAccessibilityIdentifier("eventAudienceIdentityStatus")
                                }
                                .font(.caption)
                                .foregroundStyle(.secondary)
                                .fixedSize(horizontal: false, vertical: true)
                            }
                            Spacer(minLength: 8)
                        }
                        .padding(14)
                        .background(Color(uiColor: .secondarySystemBackground))
                        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
                    }

                    recipientComposer
                    audienceAxes

                    if viewModel.canCancelDirectInviteBatch {
                        HStack(spacing: 12) {
                            Button(String(localized: "common.cancel"), role: .cancel) {
                                Task { await viewModel.cancelDirectInviteBatch() }
                            }
                            .buttonStyle(.bordered)
                            .invitationAccessibilityIdentifier("eventAudienceCancelBatchAction")
                        }
                        .frame(maxWidth: .infinity, alignment: .trailing)
                    }
                }
                .padding()
                .animation(reduceMotion ? nil : .default, value: viewModel.projection.counts.totalIdentities)
            }
            .navigationTitle(String(localized: "invitation.audience.title"))
        }
        .toolbar(.hidden, for: .tabBar)
        .task { await viewModel.reload() }
    }

    private var recipientComposer: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(spacing: 10) {
                Image(systemName: "envelope.fill")
                    .foregroundStyle(.secondary)
                    .accessibilityHidden(true)

                TextField(
                    String(localized: "invitation.audience.recipient.placeholder"),
                    text: $recipientInput
                )
                .textContentType(.emailAddress)
                .textInputAutocapitalization(.never)
                .autocorrectionDisabled()
                .disabled(!viewModel.inviteEnabled || viewModel.isSubmitting)
                .frame(minHeight: 44)
                .invitationAccessibilityIdentifier("eventAudienceRecipientInput")
            }
            .padding(.horizontal, 12)
            .background(Color(uiColor: .systemBackground))
            .overlay {
                RoundedRectangle(cornerRadius: 12, style: .continuous)
                    .stroke(Color.secondary.opacity(0.42), lineWidth: 1)
            }
            .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))

            if let inviteDisabledReason = viewModel.inviteDisabledReason {
                Label(inviteDisabledReason, systemImage: "lock.fill")
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
                    .fixedSize(horizontal: false, vertical: true)
                    .invitationAccessibilityIdentifier("eventAudienceDisabledReason")
            }

            if let submissionError = viewModel.submissionError {
                Label(submissionError.localizedMessage, systemImage: "exclamationmark.triangle.fill")
                    .font(.subheadline)
                    .foregroundStyle(.red)
                    .fixedSize(horizontal: false, vertical: true)
                    .invitationAccessibilityIdentifier("eventAudienceSubmitError")
            }

            Button {
                Task {
                    if viewModel.canRetryDirectInviteBatch {
                        await viewModel.retryDirectInviteBatch()
                    } else if await viewModel.submitRecipient(recipientInput) {
                        recipientInput = ""
                        onInvite()
                    }
                }
            } label: {
                Label(
                    viewModel.canRetryDirectInviteBatch
                        ? String(localized: "common.retry")
                        : String(localized: "event.detail.menu.add_participants"),
                    systemImage: viewModel.canRetryDirectInviteBatch
                        ? "arrow.clockwise"
                        : "person.badge.plus"
                )
                    .frame(maxWidth: .infinity, minHeight: 44)
                    .fixedSize(horizontal: false, vertical: true)
            }
            .buttonStyle(.borderedProminent)
            .disabled(
                viewModel.isSubmitting ||
                    (!viewModel.canRetryDirectInviteBatch && (
                        !viewModel.inviteEnabled ||
                            recipientInput.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
                    ))
            )
            .invitationAccessibilityIdentifier(
                "eventAudiencePrimaryAction",
                isEnabled: viewModel.canRetryDirectInviteBatch || viewModel.inviteEnabled
            )
        }
        .padding(16)
        .background(Color(uiColor: .secondarySystemBackground))
        .overlay {
            RoundedRectangle(cornerRadius: 18, style: .continuous)
                .stroke(Color.secondary.opacity(0.18), lineWidth: 1)
        }
        .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
        .accessibilityElement(children: .contain)
        .invitationAccessibilityIdentifier("eventAudienceRecipientComposer")
    }

    private var audienceAxes: some View {
        VStack(alignment: .leading, spacing: 10) {
            audienceAxis(
                String(localized: "invitation.audience.axis.delivery"),
                value: viewModel.projection.counts.deliveredInvitations,
                symbol: "paperplane",
                identifier: "eventAudienceDeliveryAxis"
            )
            audienceAxis(
                String(localized: "invitation.audience.axis.approval"),
                value: viewModel.projection.counts.pendingApprovals,
                symbol: "person.badge.clock",
                identifier: "eventAudienceApprovalAxis"
            )
            audienceAxis(
                String(localized: "invitation.audience.axis.membership"),
                value: viewModel.projection.counts.activeMembers,
                symbol: "person.2",
                identifier: "eventAudienceMembershipAxis"
            )
            audienceAxis(
                String(localized: "invitation.audience.axis.rsvp"),
                value: viewModel.projection.counts.acceptedRsvps,
                symbol: "checkmark.circle",
                identifier: "eventAudienceRsvpAxis"
            )
            audienceAxis(
                String(localized: "invitation.audience.axis.date_validation"),
                value: viewModel.projection.counts.validatedDates,
                symbol: "calendar.badge.checkmark",
                identifier: "eventAudienceDateValidationAxis"
            )
            if viewModel.persistedRecipientOutcomes.isEmpty {
                Label(String(localized: "invitation.state.unavailable"), systemImage: "envelope.badge")
                    .invitationAccessibilityIdentifier("eventAudienceRecipientOutcome")
            } else {
                ForEach(Array(viewModel.persistedRecipientOutcomes.enumerated()), id: \.offset) { _, outcome in
                    Label(recipientOutcomeTitle(outcome), systemImage: "envelope.badge")
                        .invitationAccessibilityIdentifier("eventAudienceRecipientOutcome")
                }
            }
        }
        .font(.subheadline)
        .foregroundStyle(.secondary)
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color(uiColor: .secondarySystemBackground))
        .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
    }

    private func audienceAxis(
        _ label: String,
        value: Int32,
        symbol: String,
        identifier: String
    ) -> some View {
        LabeledContent {
            Text(value, format: .number)
        } label: {
            Label(label, systemImage: symbol)
        }
        .invitationAccessibilityIdentifier(identifier)
    }

    private func recipientOutcomeTitle(_ outcome: any DirectInviteRecipientOutcome) -> String {
        switch outcome {
        case is DirectInviteRecipientOutcomeServerAccepted:
            String(localized: "invitation.audience.outcome.accepted")
        case is DirectInviteRecipientOutcomeInvalid:
            String(localized: "invitation.audience.outcome.invalid")
        case is DirectInviteRecipientOutcomeCancelled:
            String(localized: "invitation.audience.outcome.cancelled")
        default:
            String(localized: "invitation.audience.outcome.failed")
        }
    }

    private func identityGroupTitle(_ identity: AudienceIdentityAxes) -> String {
        identity.membership is MembershipStateActiveMember
            ? String(localized: "invitation.audience.identity.group.member")
            : String(localized: "invitation.audience.identity.group.invitation")
    }

    private func identityStatusTitle(_ identity: AudienceIdentityAxes) -> String {
        switch identity.rsvp {
        case .accepted:
            return String(localized: "invitation.audience.identity.status.accepted")
        case .pending:
            return String(localized: "invitation.audience.identity.status.pending")
        case .declined:
            return String(localized: "invitation.audience.identity.status.declined")
        default:
            break
        }

        switch identity.delivery {
        case is InviteDeliveryStateDelivered,
             is InviteDeliveryStateServerAccepted:
            return String(localized: "invitation.audience.identity.status.delivered")
        case is InviteDeliveryStateQueuedLocal:
            return String(localized: "invitation.audience.identity.status.pending")
        default:
            return String(localized: "invitation.state.unavailable")
        }
    }
}
