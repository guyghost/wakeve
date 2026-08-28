import SwiftUI
import Shared

private enum StudioArtworkSelection: String, CaseIterable, Identifiable {
    case keepExisting
    case none
    case preset

    var id: String { rawValue }
}

struct InvitationStudioPreviewSnapshot {
    let event: Event
    let artwork: any Artwork
    let hostDisplayName: String
    let locationDisplayName: String
}

@MainActor
final class EventCreationStudioViewModel: ObservableObject {
    @Published var title = ""
    @Published var eventDescription = ""
    @Published private(set) var persistedEvent: Event?
    @Published private(set) var studioState: any CreationStudioState
    @Published fileprivate var artworkSelection: StudioArtworkSelection = .none

    let stateMachine: CreationStudioStateMachine
    private let repository: DatabaseEventRepository
    private let database: WakeveDb
    private let aggregateOwner: DatabaseUpdateDraftAggregateUseCase
    private let syncOwner: any CreationStudioSyncOwner
    private let actorId: String
    private let eventId: String
    private let existingArtwork: (any Artwork)?
    private let deadline: String
    private var syncObservationTask: Task<Void, Never>?

    init(
        eventId: String? = nil,
        actorId: String = "",
        baseRevision: Int64? = nil,
        existingArtwork: (any Artwork)? = nil,
        repository: DatabaseEventRepository = RepositoryProvider.shared.databaseRepository,
        database: WakeveDb = RepositoryProvider.shared.database,
        aggregateOwner: DatabaseUpdateDraftAggregateUseCase = DatabaseUpdateDraftAggregateUseCase(
            database: RepositoryProvider.shared.database
        ),
        syncOwner: any CreationStudioSyncOwner = DatabaseCreationStudioSyncOwner(
            database: RepositoryProvider.shared.database,
            syncManager: RepositoryProvider.shared.syncManager
        ),
        stateMachine: CreationStudioStateMachine = CreationStudioStateMachine()
    ) {
        self.repository = repository
        self.database = database
        self.aggregateOwner = aggregateOwner
        self.syncOwner = syncOwner
        self.actorId = actorId
        self.eventId = eventId ?? "event-\(UUID().uuidString.lowercased())"
        self.existingArtwork = existingArtwork
        self.stateMachine = stateMachine
        let event = eventId.flatMap(repository.getEvent(id:))
        persistedEvent = event
        title = event?.title ?? ""
        eventDescription = event?.description_ ?? ""
        deadline = event?.deadline ?? ISO8601DateFormatter().string(
            from: Date().addingTimeInterval(30 * 24 * 60 * 60)
        )
        let fields = StudioEventFields(
            title: event?.title ?? "",
            description: event?.description_ ?? "",
            deadline: deadline,
            eventType: event?.eventType ?? .other,
            eventTypeCustom: event?.eventTypeCustom,
            minParticipants: event?.minParticipants,
            maxParticipants: event?.maxParticipants,
            expectedParticipants: event?.expectedParticipants,
            proposedSlots: event?.proposedSlots ?? [],
            planningMode: event?.planningMode ?? .timeSlotPoll
        )
        let isEditingExisting = event != nil && baseRevision != nil
        let draft = CreationDraft(
            draftRevision: 0,
            fields: fields,
            artworkChoice: isEditingExisting
                ? ArtworkChoiceKeepExisting.shared
                : ArtworkChoiceNone.shared
        )
        artworkSelection = isEditingExisting ? .keepExisting : .none
        studioState = CreationStudioStateEditing(
            mode: isEditingExisting
                ? StudioModeEditExisting(eventId: self.eventId, baseRevision: baseRevision ?? 0)
                : StudioModeNew.shared,
            baseRevision: isEditingExisting
                ? StudioBaseRevisionValue(revision: baseRevision ?? 0)
                : StudioBaseRevisionNotApplicable.shared,
            draft: draft
        )
        syncObservationTask = Task { [weak self] in
            await self?.restorePersistedSync()
        }
    }

    var currentDraftRevision: Int64 {
        currentDraft?.draftRevision ?? 0
    }

    var currentPreviewArtwork: any Artwork {
        if let state = studioState as? CreationStudioStatePreviewReady {
            return state.artwork
        }
        if let state = studioState as? CreationStudioStatePreviewing {
            return state.artwork
        }
        if let state = studioState as? CreationStudioStateCommitting {
            return state.artwork
        }
        if let state = studioState as? CreationStudioStateDetachedCommitting {
            return state.artwork
        }
        if let state = studioState as? CreationStudioStateDetachedResolving {
            return state.artwork
        }
        if let state = studioState as? CreationStudioStateFailedBeforeCommit {
            return state.artwork ?? resolvedArtwork(artworkSelection)
        }
        return resolvedArtwork(artworkSelection)
    }

    var currentPreviewEvent: Event {
        if let draft = currentDraft {
            return projectedEvent(fields: draft.fields)
        }
        if let persistedEvent {
            return persistedEvent
        }
        return projectedEvent(
            fields: StudioEventFields(
                title: title,
                description: eventDescription,
                deadline: deadline,
                eventType: .other,
                eventTypeCustom: nil,
                minParticipants: nil,
                maxParticipants: nil,
                expectedParticipants: nil,
                proposedSlots: [],
                planningMode: .timeSlotPoll
            )
        )
    }

    var currentPreviewDateDisplayName: String {
        let event = currentPreviewEvent
        let repositoryDate = event.finalDate ??
            event.proposedSlots.compactMap(\.start).first ??
            event.deadline
        return InvitationEventMetadataProjection.localizedDate(for: repositoryDate)
    }

    var currentPreviewLocationDisplayName: String {
        let location = database.potentialLocationQueries
            .selectFirstLocationByEventId(eventId: eventId)
            .executeAsOneOrNull()?
            .name
            .trimmingCharacters(in: .whitespacesAndNewlines)
        return location?.isEmpty == false
            ? location ?? String(localized: "invitation.state.unavailable")
            : String(localized: "invitation.state.unavailable")
    }

    var currentPreviewHostDisplayName: String {
        InvitationEventMetadataProjection.organizerDisplayName(
            for: currentPreviewEvent,
            database: database
        )
    }

    var isDraftValid: Bool {
        !title.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty &&
            !eventDescription.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
    }

    var retryAvailable: Bool {
        (studioState as? CreationStudioStateSyncFailed)?.retryable == true ||
            studioState is CreationStudioStateFailedBeforeCommit ||
            studioState is CreationStudioStateDetachedCommitting
    }

    var isPendingSync: Bool {
        studioState is CreationStudioStatePendingSync ||
            studioState is CreationStudioStateDetachedPendingSync
    }

    var primaryActionAvailable: Bool {
        if studioState is CreationStudioStateEditing {
            return isDraftValid
        }
        return studioState is CreationStudioStatePreviewReady ||
            studioState is CreationStudioStatePreviewing ||
            retryAvailable
    }

    var primaryActionTitle: String {
        if studioState is CreationStudioStatePreviewing {
            return String(localized: "common.done")
        }
        if retryAvailable {
            return String(localized: "common.retry")
        }
        return String(localized: "invitation.studio.preview")
    }

    private var currentDraft: CreationDraft? {
        if let state = studioState as? CreationStudioStateEditing { return state.draft }
        if let state = studioState as? CreationStudioStatePreviewReady { return state.draft }
        if let state = studioState as? CreationStudioStatePreviewing { return state.draft }
        if let state = studioState as? CreationStudioStateCommitting { return state.draft }
        if let state = studioState as? CreationStudioStateDetachedCommitting { return state.draft }
        if let state = studioState as? CreationStudioStateDetachedResolving { return state.draft }
        if let state = studioState as? CreationStudioStateFailedBeforeCommit { return state.draft }
        return nil
    }

    func updateFields() {
        guard let editing = studioState as? CreationStudioStateEditing else { return }
        let current = editing.draft.fields
        studioState = stateMachine.transition(
            state: studioState,
            event: CreationStudioEventUpdateFields(
                expectedDraftRevision: editing.draft.draftRevision,
                fields: StudioEventFields(
                    title: title,
                    description: eventDescription,
                    deadline: deadline,
                    eventType: current.eventType,
                    eventTypeCustom: current.eventTypeCustom,
                    minParticipants: current.minParticipants,
                    maxParticipants: current.maxParticipants,
                    expectedParticipants: current.expectedParticipants,
                    proposedSlots: current.proposedSlots,
                    planningMode: current.planningMode
                )
            )
        )
    }

    fileprivate func selectArtwork(_ selection: StudioArtworkSelection) {
        guard let editing = studioState as? CreationStudioStateEditing else { return }
        let choice: any ArtworkChoice = switch selection {
        case .keepExisting: ArtworkChoiceKeepExisting.shared
        case .none: ArtworkChoiceNone.shared
        case .preset: ArtworkChoicePreset(presetId: "wakeve-celebration")
        }
        let next = stateMachine.transition(
            state: studioState,
            event: CreationStudioEventUpdateArtwork(
                expectedDraftRevision: editing.draft.draftRevision,
                artworkChoice: choice,
                capability: ArtworkSelectionCapabilityHidden.shared
            )
        )
        guard next !== studioState else { return }
        artworkSelection = selection
        studioState = next
    }

    func performPrimaryAction(
        onPreview: (
            _ snapshot: InvitationStudioPreviewSnapshot,
            _ confirmCommit: @escaping () async -> Bool
        ) -> Void
    ) async {
        if let editing = studioState as? CreationStudioStateEditing {
            guard isDraftValid else { return }
            let resolving = stateMachine.transition(
                state: editing,
                event: CreationStudioEventRequestPreview(
                    expectedDraftRevision: editing.draft.draftRevision,
                    capability: ArtworkSelectionCapabilityHidden.shared
                )
            )
            guard resolving is CreationStudioStateResolvingArtwork else { return }
            let artwork = resolvedArtwork(artworkSelection)
            let ready = stateMachine.transition(
                state: resolving,
                event: CreationStudioEventArtworkResolved(
                    draftRevision: editing.draft.draftRevision,
                    artwork: artwork
                )
            )
            let opened = stateMachine.transition(
                state: ready,
                event: CreationStudioEventOpenPreview(
                    expectedDraftRevision: editing.draft.draftRevision
                )
            )
            guard let previewing = opened as? CreationStudioStatePreviewing else { return }
            studioState = previewing
            let snapshot = previewSnapshot(
                draft: previewing.draft,
                artwork: previewing.artwork
            )
            onPreview(snapshot) { [weak self] in
                guard let self else { return false }
                return await self.confirmPreviewCommit()
            }
            return
        }

        if studioState is CreationStudioStatePreviewing {
            _ = await confirmPreviewCommit()
            return
        }

        await retry()
    }

    func confirmPreviewCommit() async -> Bool {
        guard let previewing = studioState as? CreationStudioStatePreviewing else {
            return false
        }
        let operationId = "studio-\(UUID().uuidString.lowercased())"
        let command = makeCommitCommand(
            previewing: previewing,
            operationId: operationId
        )
        let envelope = StudioCommitEnvelopeFactory.shared.build(command: command)
        studioState = stateMachine.transition(
            state: previewing,
            event: CreationStudioEventConfirmCommit(
                expectedDraftRevision: previewing.draft.draftRevision,
                operationId: operationId,
                durableOperationRef: envelope.durableOperationRef,
                requestFingerprint: envelope.requestFingerprint,
                resolutionRetryBudget: envelope.maxResolutionAttempts,
                envelope: envelope
            )
        )
        return await executeCommit()
    }

    /// Closing is a modeled presentation event. An in-flight durable command is detached,
    /// never cancelled or rewritten by the sheet lifecycle.
    func close() {
        studioState = stateMachine.transition(
            state: studioState,
            event: CreationStudioEventClose.shared
        )
    }

    func retry() async {
        if let failed = studioState as? CreationStudioStateFailedBeforeCommit,
           let operationId = failed.operationId {
            studioState = stateMachine.transition(
                state: failed,
                event: CreationStudioEventRetryBeforeCommit(operationId: operationId)
            )
            _ = await executeCommit()
        } else if let failed = studioState as? CreationStudioStateSyncFailed {
            guard let binding = aggregateOwner.loadSyncBinding(
                eventId: failed.eventId,
                operationId: failed.operationId
            ) else {
                projectRepositoryInconsistentSyncFailure(
                    eventId: failed.eventId,
                    committedRevision: failed.committedRevision,
                    operationId: failed.operationId,
                    envelope: failed.envelope
                )
                return
            }
            do {
                let retried = try await syncOwner.retry(binding: binding)
                consumeSyncResult(retried, binding: binding)
                if retried is CreationStudioSyncResultPending {
                    syncObservationTask?.cancel()
                    syncObservationTask = Task { [weak self] in
                        await self?.observeSync(binding)
                    }
                }
            } catch {
                consumeSyncFailure(binding, error: .repositoryUnavailable)
            }
        } else if let detached = studioState as? CreationStudioStateDetachedCommitting {
            resolveUnknownCommit(detached)
        }
    }

    private func makeCommitCommand(
        previewing: CreationStudioStatePreviewing,
        operationId: String
    ) -> UpdateDraftAggregateCommand {
        let expectedBaseRevision: Int64
        if let edit = previewing.mode as? StudioModeEditExisting {
            expectedBaseRevision = edit.baseRevision
        } else {
            expectedBaseRevision = 0
        }
        let resolvedArtwork = previewing.artwork
        let expectedResultingArtwork = resolvedArtwork
        return UpdateDraftAggregateCommand(
            eventId: eventId,
            actorId: actorId,
            expectedBaseRevision: expectedBaseRevision,
            eventDraft: previewing.draft.fields,
            artwork: expectedResultingArtwork,
            operationId: operationId,
            artworkCapability: ArtworkSelectionCapabilityHidden.shared,
            draftRevision: previewing.draft.draftRevision
        )
    }

    private func executeCommit() async -> Bool {
        guard let committing = studioState as? CreationStudioStateCommitting else { return false }
        let expectedBaseRevision: Int64
        if let edit = committing.mode as? StudioModeEditExisting {
            expectedBaseRevision = edit.baseRevision
        } else {
            expectedBaseRevision = 0
        }
        let command = UpdateDraftAggregateCommand(
            eventId: eventId,
            actorId: actorId,
            expectedBaseRevision: expectedBaseRevision,
            eventDraft: committing.draft.fields,
            artwork: committing.artwork,
            operationId: committing.operationId,
            artworkCapability: ArtworkSelectionCapabilityHidden.shared,
            draftRevision: committing.draft.draftRevision
        )
        let envelope = StudioCommitEnvelopeFactory.shared.build(command: command)
        guard envelope.durableOperationRef == committing.durableOperationRef,
              envelope.requestFingerprint == committing.requestFingerprint else { return false }
        do {
            let result = try await aggregateOwner.execute(command: command)
            if let committed = result as? UpdateDraftAggregateResultCommitted {
                persistedEvent = repository.getEvent(id: committed.eventId)
                let commitEvent: any CreationStudioEvent
                if studioState is CreationStudioStateDetachedCommitting ||
                    studioState is CreationStudioStateDetachedResolving {
                    commitEvent = CreationStudioEventLateLocalCommit(
                        eventId: committed.eventId,
                        draftRevision: committing.draft.draftRevision,
                        committedRevision: committed.committedRevision,
                        operationId: committed.operationId,
                        pendingSync: committed.pendingSync
                    )
                } else {
                    commitEvent = CreationStudioEventLocalCommit(
                        eventId: committed.eventId,
                        draftRevision: committing.draft.draftRevision,
                        committedRevision: committed.committedRevision,
                        operationId: committed.operationId,
                        pendingSync: committed.pendingSync
                    )
                }
                studioState = stateMachine.transition(
                    state: studioState,
                    event: commitEvent
                )
                if committed.pendingSync {
                    guard let binding = aggregateOwner.loadSyncBinding(
                        eventId: committed.eventId,
                        operationId: committed.operationId
                    ) else {
                        projectRepositoryInconsistentSyncFailure(
                            eventId: committed.eventId,
                            committedRevision: committed.committedRevision,
                            operationId: committed.operationId,
                            envelope: envelope
                        )
                        return false
                    }
                    syncObservationTask?.cancel()
                    syncObservationTask = Task { [weak self] in
                        await self?.observeSync(binding)
                    }
                }
                return studioState is CreationStudioStatePendingSync ||
                    studioState is CreationStudioStateDetachedPendingSync ||
                    studioState is CreationStudioStateCompleted
            } else if let rejected = result as? UpdateDraftAggregateResultRejected {
                if studioState is CreationStudioStateDetachedCommitting ||
                    studioState is CreationStudioStateDetachedResolving {
                    if let detached = studioState as? CreationStudioStateDetachedCommitting {
                        resolveProvenNonCommit(detached)
                    }
                } else {
                    studioState = stateMachine.transition(
                        state: studioState,
                        event: CreationStudioEventFailBeforeLocalCommit(
                            draftRevision: committing.draft.draftRevision,
                            operationId: committing.operationId,
                            error: rejected.error
                        )
                    )
                }
            } else if result is UpdateDraftAggregateResultOutcomeUnknown {
                studioState = stateMachine.transition(
                    state: studioState,
                    event: CreationStudioEventOutcomeUnknown(
                        draftRevision: committing.draft.draftRevision,
                        operationId: committing.operationId,
                        retryable: true
                    )
                )
                if let detached = studioState as? CreationStudioStateDetachedCommitting {
                    resolveUnknownCommit(detached)
                }
            }
            return false
        } catch {
            studioState = stateMachine.transition(
                state: studioState,
                event: CreationStudioEventOutcomeUnknown(
                    draftRevision: committing.draft.draftRevision,
                    operationId: committing.operationId,
                    retryable: true
                )
            )
            if let detached = studioState as? CreationStudioStateDetachedCommitting {
                resolveUnknownCommit(detached)
            }
            return false
        }
    }

    private func beginResolutionAttempt(
        _ detached: CreationStudioStateDetachedCommitting
    ) -> (attemptId: String, fence: Int32)? {
        let attemptId = "studio-resolution-\(UUID().uuidString.lowercased())"
        let fence = detached.resolutionAttempt + 1
        studioState = stateMachine.transition(
            state: studioState,
            event: CreationStudioEventRetryResolution(
                draftRevision: detached.draft.draftRevision,
                operationId: detached.operationId,
                attemptId: attemptId,
                fence: fence
            )
        )
        guard let resolving = studioState as? CreationStudioStateDetachedResolving,
              resolving.attemptId == attemptId,
              resolving.fence == fence else { return nil }
        return (attemptId, fence)
    }

    private func resolveProvenNonCommit(_ detached: CreationStudioStateDetachedCommitting) {
        guard let attempt = beginResolutionAttempt(detached) else { return }
        studioState = stateMachine.transition(
            state: studioState,
            event: CreationStudioEventResolutionResult(
                draftRevision: detached.draft.draftRevision,
                operationId: detached.operationId,
                outcome: .provenNotCommitted,
                eventId: nil,
                committedRevision: nil,
                pendingSync: false,
                attemptId: attempt.attemptId,
                fence: attempt.fence
            )
        )
    }

    private func resolveUnknownCommit(_ detached: CreationStudioStateDetachedCommitting) {
        guard let attempt = beginResolutionAttempt(detached) else { return }
        let receipt = database.invitationExperienceQueries
            .selectOperationReceiptByOperationId(operation_id: detached.operationId)
            .executeAsOneOrNull()
        let commitEnvelopePayload = receipt?.commit_envelope
        let commitEnvelope: StudioCommitEnvelope? = receipt.flatMap {
            aggregateOwner.loadCommitEnvelope(eventId: $0.event_id, operationId: $0.operation_id)
        }
        let syncMetadata = database.syncMetadataQueries
            .selectById(id: "studio:\(detached.operationId)")
            .executeAsOneOrNull()
        let pendingSubject: StudioPendingSyncSubject? = receipt.flatMap {
            aggregateOwner.loadPendingSyncSubject(eventId: $0.event_id, operationId: $0.operation_id)
        }
        let isCorrelatedCommit = receipt?.event_id == eventId &&
            receipt?.actor_id == actorId &&
            receipt?.action == "UPDATE_DRAFT_AGGREGATE" &&
            (receipt?.status == "PENDING_SYNC" || receipt?.status == "COMMITTED") &&
            receipt?.durable_operation_ref == detached.durableOperationRef &&
            receipt?.request_fingerprint == detached.requestFingerprint &&
            commitEnvelopePayload?.isEmpty == false &&
            commitEnvelope?.identity.operationId == detached.operationId &&
            commitEnvelope?.requestPayload.subject.eventId == eventId &&
            commitEnvelope?.requestPayload.actorId == actorId &&
            commitEnvelope?.durableOperationRef == detached.durableOperationRef &&
            commitEnvelope?.requestFingerprint == detached.requestFingerprint &&
            syncMetadata?.entityType == "event" &&
            syncMetadata?.entityId == eventId &&
            syncMetadata?.payload.isEmpty == false &&
            pendingSubject?.eventId == eventId &&
            pendingSubject?.localReceiptId == detached.operationId &&
            pendingSubject?.committedRevision == receipt?.aggregate_revision &&
            pendingSubject?.envelope.durableOperationRef == detached.durableOperationRef &&
            pendingSubject?.envelope.requestFingerprint == detached.requestFingerprint
        if isCorrelatedCommit, let receipt {
            persistedEvent = repository.getEvent(id: receipt.event_id)
            studioState = stateMachine.transition(
                state: studioState,
                event: CreationStudioEventResolutionResult(
                    draftRevision: detached.draft.draftRevision,
                    operationId: detached.operationId,
                    outcome: .localCommitted,
                    eventId: receipt.event_id,
                    committedRevision: KotlinLong(value: receipt.aggregate_revision),
                    pendingSync: true,
                    attemptId: attempt.attemptId,
                    fence: attempt.fence
                )
            )
            if let pending = studioState as? CreationStudioStateDetachedPendingSync {
                syncObservationTask?.cancel()
                syncObservationTask = Task { [weak self] in
                    await self?.observeSync(pending.binding)
                }
            }
        } else {
            let repositoryInconsistent = PollBallotContract.FailureCode.repositoryInconsistent
            _ = repositoryInconsistent
            studioState = stateMachine.transition(
                state: studioState,
                event: CreationStudioEventRepositoryInconsistent(
                    draftRevision: detached.draft.draftRevision,
                    operationId: detached.operationId,
                    attemptId: attempt.attemptId,
                    fence: attempt.fence,
                    retryable: false
                )
            )
        }
    }

    private func observeSync(_ binding: CreationStudioSyncBinding) async {
        while !Task.isCancelled {
            do {
                let result = try await syncOwner.observe(binding: binding)
                consumeSyncResult(result, binding: binding)
                if result is CreationStudioSyncResultPending {
                    try await Task.sleep(nanoseconds: 1_000_000_000)
                    continue
                }
                return
            } catch is CancellationError {
                return
            } catch {
                consumeSyncFailure(binding, error: .repositoryUnavailable)
                return
            }
        }
    }

    private func restorePersistedSync() async {
        guard let persistedEvent else { return }
        func repositoryInconsistent(operationId: String, committedRevision: Int64) {
            let stableCode = PollBallotContract.FailureCode.repositoryInconsistent
            _ = stableCode
            let terminal = CreationStudioStateSyncFailed(
                eventId: eventId,
                committedRevision: committedRevision,
                operationId: operationId,
                error: .commitOutcomeUnknown,
                retryable: false,
                code: .repositoryInconsistent,
                commitOutcome: .unknown,
                envelope: nil
            )
            let reduced = stateMachine.transition(
                state: studioState,
                event: CreationStudioEventSyncFailed(
                    eventId: eventId,
                    committedRevision: committedRevision,
                    operationId: operationId,
                    error: .commitOutcomeUnknown,
                    retryable: false,
                    code: .repositoryInconsistent,
                    commitOutcome: .unknown
                )
            )
            studioState = reduced is CreationStudioStateSyncFailed ? reduced : terminal
        }
        let receipts = database.invitationExperienceQueries
            .selectOperationReceiptsByEventId(event_id: eventId)
            .executeAsList()
        guard let receipt = receipts.last(where: {
            $0.action == "UPDATE_DRAFT_AGGREGATE" &&
                $0.aggregate_revision == persistedEvent.aggregateRevision
        }) else {
            return
        }
        let sync = database.syncMetadataQueries
            .selectById(id: "studio:\(receipt.operation_id)")
            .executeAsOneOrNull()
        guard let sync else {
            repositoryInconsistent(
                operationId: receipt.operation_id,
                committedRevision: receipt.aggregate_revision
            )
            return
        }
        guard sync.synced == 0 else { return }

        guard let binding = aggregateOwner.loadSyncBinding(
            eventId: eventId,
            operationId: receipt.operation_id
        ), let envelope = aggregateOwner.loadCommitEnvelope(
            eventId: eventId,
            operationId: receipt.operation_id
        ) else {
            repositoryInconsistent(
                operationId: receipt.operation_id,
                committedRevision: receipt.aggregate_revision
            )
            return
        }
        studioState = CreationStudioStateDetachedPendingSync(
            eventId: binding.eventId,
            committedRevision: binding.aggregateRevision,
            operationId: binding.operationId,
            binding: binding,
            envelope: envelope
        )
        await observeSync(binding)
    }

    func cancelSyncObservation() {
        syncObservationTask?.cancel()
        syncObservationTask = nil
    }

    private func consumeSyncResult(
        _ result: any CreationStudioSyncResult,
        binding: CreationStudioSyncBinding
    ) {
        guard result.binding == binding else { return }
        if result is CreationStudioSyncResultCompleted {
            studioState = stateMachine.transition(
                state: studioState,
                event: CreationStudioEventSyncCompleted(
                    eventId: binding.eventId,
                    committedRevision: binding.aggregateRevision,
                    operationId: binding.operationId
                )
            )
        } else if let failed = result as? CreationStudioSyncResultFailed {
            consumeSyncFailure(
                binding,
                error: failed.error,
                retryable: failed.retryable,
                code: failed.code,
                commitOutcome: failed.commitOutcome
            )
        } else if result is CreationStudioSyncResultPending,
                  let failed = studioState as? CreationStudioStateSyncFailed {
            studioState = stateMachine.transition(
                state: failed,
                event: CreationStudioEventRetrySync(
                    eventId: binding.eventId,
                    committedRevision: binding.aggregateRevision,
                    operationId: binding.operationId
                )
            )
        }
    }

    private func consumeSyncFailure(
        _ binding: CreationStudioSyncBinding,
        error: InvitationExperienceError,
        retryable: Bool = true,
        code: PollBallotContract.FailureCode? = nil,
        commitOutcome: PollBallotContract.CommitOutcome? = nil
    ) {
        studioState = stateMachine.transition(
            state: studioState,
            event: CreationStudioEventSyncFailed(
                eventId: binding.eventId,
                committedRevision: binding.aggregateRevision,
                operationId: binding.operationId,
                error: error,
                retryable: retryable,
                code: code,
                commitOutcome: commitOutcome
            )
        )
    }

    private func projectRepositoryInconsistentSyncFailure(
        eventId: String,
        committedRevision: Int64,
        operationId: String,
        envelope: StudioCommitEnvelope?
    ) {
        let event = CreationStudioEventSyncFailed(
            eventId: eventId,
            committedRevision: committedRevision,
            operationId: operationId,
            error: .commitOutcomeUnknown,
            retryable: false,
            code: .repositoryInconsistent,
            commitOutcome: .unknown
        )
        let reduced = stateMachine.transition(state: studioState, event: event)
        studioState = reduced is CreationStudioStateSyncFailed
            ? reduced
            : CreationStudioStateSyncFailed(
                eventId: eventId,
                committedRevision: committedRevision,
                operationId: operationId,
                error: .commitOutcomeUnknown,
                retryable: false,
                code: .repositoryInconsistent,
                commitOutcome: .unknown,
                envelope: envelope
            )
    }

    private func resolvedArtwork(_ selection: StudioArtworkSelection) -> any Artwork {
        switch selection {
        case .keepExisting:
            existingArtwork ?? ArtworkNone.shared
        case .none:
            ArtworkNone.shared
        case .preset:
            ArtworkStructured(
                version: 1,
                ref: ArtworkRef(
                    source: ArtworkSourcePreset(presetId: "wakeve-celebration"),
                    alt: ArtworkAltDecorative.shared,
                    focalPoint: ArtworkFocalPoint(x: 0.5, y: 0.5),
                    crop: .fill
                )
            )
        }
    }

    private func previewSnapshot(
        draft: CreationDraft,
        artwork: any Artwork
    ) -> InvitationStudioPreviewSnapshot {
        let event = projectedEvent(fields: draft.fields)
        let location = database.potentialLocationQueries
            .selectFirstLocationByEventId(eventId: eventId)
            .executeAsOneOrNull()?
            .name
            .trimmingCharacters(in: .whitespacesAndNewlines)
        return InvitationStudioPreviewSnapshot(
            event: event,
            artwork: artwork,
            hostDisplayName: InvitationEventMetadataProjection.organizerDisplayName(
                for: event,
                database: database
            ),
            locationDisplayName: location?.isEmpty == false
                ? location ?? String(localized: "invitation.state.unavailable")
                : String(localized: "invitation.state.unavailable")
        )
    }

    private func projectedEvent(fields: StudioEventFields) -> Event {
        let stored = persistedEvent
        let now = ISO8601DateFormatter().string(from: Date())
        return Event(
            id: eventId,
            title: fields.title,
            description: fields.description_,
            organizerId: stored?.organizerId ?? actorId,
            participants: stored?.participants ?? [actorId],
            proposedSlots: fields.proposedSlots,
            deadline: fields.deadline,
            status: stored?.status ?? .draft,
            finalDate: stored?.finalDate,
            createdAt: stored?.createdAt ?? now,
            updatedAt: stored?.updatedAt ?? now,
            eventType: fields.eventType,
            eventTypeCustom: fields.eventTypeCustom,
            minParticipants: fields.minParticipants,
            maxParticipants: fields.maxParticipants,
            expectedParticipants: fields.expectedParticipants,
            heroImageUrl: stored?.heroImageUrl,
            planningMode: fields.planningMode,
            aggregateRevision: stored?.aggregateRevision ?? 1,
            aggregateSchemaVersion: stored?.aggregateSchemaVersion ?? 1
        )
    }
}

@MainActor
struct EventCreationStudioView: View {
    @StateObject private var viewModel: EventCreationStudioViewModel
    @Environment(\.verticalSizeClass) private var verticalSizeClass
    @Environment(\.dynamicTypeSize) private var dynamicTypeSize

    let previewAvailable: Bool
    let onCancel: () -> Void
    let onRequestPreview: (
        _ snapshot: InvitationStudioPreviewSnapshot,
        _ confirmCommit: @escaping () async -> Bool
    ) -> Void

    init(
        viewModel: EventCreationStudioViewModel? = nil,
        eventId: String? = nil,
        actorId: String = "",
        baseRevision: Int64? = nil,
        existingArtwork: (any Artwork)? = nil,
        previewAvailable: Bool,
        onCancel: @escaping () -> Void,
        onRequestPreview: @escaping (
            _ snapshot: InvitationStudioPreviewSnapshot,
            _ confirmCommit: @escaping () async -> Bool
        ) -> Void
    ) {
        _viewModel = StateObject(
            wrappedValue: viewModel ?? EventCreationStudioViewModel(
                eventId: eventId,
                actorId: actorId,
                baseRevision: baseRevision,
                existingArtwork: existingArtwork
            )
        )
        self.previewAvailable = previewAvailable
        self.onCancel = onCancel
        self.onRequestPreview = onRequestPreview
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: verticalSizeClass == .compact ? 14 : 22) {
                    VStack(alignment: .leading, spacing: 12) {
                        Label(String(localized: "invitation.studio.title"), systemImage: "photo.on.rectangle.angled")
                            .font(.headline)

                        InvitationArtworkView(
                            artwork: viewModel.currentPreviewArtwork,
                            event: viewModel.currentPreviewEvent
                        )
                        .aspectRatio(4.0 / 3.0, contentMode: .fit)
                        .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
                        .contentShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
                        .invitationAccessibilityIdentifier("eventStudioArtworkHero")

                        Text(viewModel.title)
                            .font(.title2.weight(.semibold))
                            .lineLimit(2)
                            .fixedSize(horizontal: false, vertical: true)
                            .invitationAccessibilityIdentifier("eventStudioHeroTitle")

                        LabeledContent(
                            String(localized: "events.start"),
                            value: viewModel.currentPreviewDateDisplayName
                        )
                        .invitationAccessibilityIdentifier("eventStudioHeroDate")

                        LabeledContent(
                            String(localized: "events.location"),
                            value: viewModel.currentPreviewLocationDisplayName
                        )
                        .invitationAccessibilityIdentifier("eventStudioHeroLocation")

                        LabeledContent(
                            String(localized: "invitation.information.organizer"),
                            value: viewModel.currentPreviewHostDisplayName
                        )
                        .invitationAccessibilityIdentifier("eventStudioHeroHost")

                        LazyVGrid(columns: artworkColumns, alignment: .leading, spacing: 10) {
                            if viewModel.artworkSelection == .keepExisting {
                                artworkButton(
                                    .keepExisting,
                                    title: String(localized: "invitation.studio.artwork.keep_existing"),
                                    systemImage: "photo",
                                    identifier: "eventStudioArtworkKeepExisting"
                                )
                            }
                            artworkButton(
                                .none,
                                title: String(localized: "invitation.studio.artwork.none"),
                                systemImage: "circle.slash",
                                identifier: "eventStudioArtworkNone"
                            )
                            artworkButton(
                                .preset,
                                title: String(localized: "invitation.studio.artwork.preset"),
                                systemImage: "sparkles.rectangle.stack",
                                identifier: "eventStudioArtworkPreset"
                            )
                        }

                        if viewModel.isPendingSync {
                            Label(String(localized: "invitation.studio.pending_sync"), systemImage: "icloud.and.arrow.up")
                                .font(.subheadline)
                                .foregroundStyle(.secondary)
                                .invitationAccessibilityIdentifier("eventStudioPendingSyncState")
                        }

                    }
                    .padding(16)
                    .background(Color(uiColor: .secondarySystemBackground))
                    .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
                    .accessibilitySortPriority(dynamicTypeSize.isAccessibilitySize ? 2 : 1)

                    VStack(alignment: .leading, spacing: 8) {
                        Text(String(localized: "create_event.title_label"))
                            .font(.headline)
                        TextField(String(localized: "create_event.title_placeholder"), text: $viewModel.title)
                            .textFieldStyle(.roundedBorder)
                            .frame(minHeight: 44)
                            .onChange(of: viewModel.title) { _, _ in viewModel.updateFields() }
                    }

                    DisclosureGroup(String(localized: "event_info.description_title")) {
                        TextEditor(text: $viewModel.eventDescription)
                            .frame(minHeight: verticalSizeClass == .compact ? 120 : 190)
                            .padding(10)
                            .background(Color(uiColor: .secondarySystemBackground))
                            .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
                            .onChange(of: viewModel.eventDescription) { _, _ in viewModel.updateFields() }
                    }
                    .invitationAccessibilityIdentifier("eventStudioDescriptionDisclosure")
                }
                .padding()
            }
            .navigationTitle(String(localized: "invitation.studio.title"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(String(localized: "common.cancel")) {
                        viewModel.close()
                        onCancel()
                    }
                }
            }
            .safeAreaInset(edge: .bottom) {
                Button {
                    Task {
                        await viewModel.performPrimaryAction(onPreview: onRequestPreview)
                    }
                } label: {
                    Text(viewModel.primaryActionTitle)
                        .frame(maxWidth: .infinity, minHeight: 44)
                        .fixedSize(horizontal: false, vertical: true)
                }
                .buttonStyle(.borderedProminent)
                .disabled(!previewAvailable || !viewModel.primaryActionAvailable)
                .invitationAccessibilityIdentifier(
                    "eventCreationStudioPrimaryAction",
                    isEnabled: previewAvailable && viewModel.primaryActionAvailable
                )
                .padding()
                .padding(.bottom, dynamicTypeSize.isAccessibilitySize ? 8 : 0)
                .background(.bar)
            }
        }
        .toolbar(.hidden, for: .tabBar)
        .onDisappear {
            viewModel.cancelSyncObservation()
        }
    }

    private func artworkButton(
        _ selection: StudioArtworkSelection,
        title: String,
        systemImage: String,
        identifier: String
    ) -> some View {
        Button {
            viewModel.selectArtwork(selection)
        } label: {
            HStack(spacing: 8) {
                Label(title, systemImage: systemImage)
                    .lineLimit(2)
                    .fixedSize(horizontal: false, vertical: true)
                Spacer(minLength: 0)
                if viewModel.artworkSelection == selection {
                    Image(systemName: "checkmark.circle.fill")
                        .foregroundStyle(.tint)
                        .accessibilityHidden(true)
                        .invitationAccessibilityIdentifier("eventStudioArtworkSelectionIndicator")
                }
            }
                .frame(maxWidth: .infinity, minHeight: 44)
                .fixedSize(horizontal: false, vertical: true)
        }
        .buttonStyle(.bordered)
        .frame(maxWidth: .infinity, alignment: .leading)
        .invitationAccessibilityIdentifier(identifier)
        .accessibilityAddTraits(viewModel.artworkSelection == selection ? .isSelected : [])
    }

    private var artworkColumns: [GridItem] {
        [GridItem(.flexible(), spacing: 10, alignment: .leading)]
    }
}
