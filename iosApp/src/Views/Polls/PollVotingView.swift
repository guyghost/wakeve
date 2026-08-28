import SwiftUI
import Shared

/// Poll voting view inspired by Apple Invites
/// Features: Clean design, card-based time slots, clear voting options
struct PollVotingView: View {
    let event: Event
    let repository: EventRepositoryInterface
    let journal: DatabaseBallotCommandJournal
    let participantId: String
    let onVoteSubmitted: () -> Void
    let onBack: () -> Void

    @State private var votes: [String: PollVote] = [:]
    @State private var isLoading = false
    @State private var errorMessage = ""
    @State private var showError = false
    @State private var showSuccess = false
    @State private var hasVoted = false
    @State private var votingClosed = false
    @State private var localPending = false
    @State private var pendingCommand: PollBallotContract.CommitCompleteBallotCommand?
    @State private var pendingReceipt: PollBallotContract.Receipt?
    @State private var ballotRetryAvailable = false

    var body: some View {
        PollVotingContentView(
            event: event,
            votes: $votes,
            isLoading: isLoading,
            hasVoted: hasVoted,
            votingClosed: votingClosed,
            onSubmitVotes: {
                Task { await submitVotes() }
            },
            onBack: { Task { await handleBack() } }
        )
        .onAppear {
            votingClosed = repository.isDeadlinePassed(deadline: event.deadline)
            checkExistingVotes()
            Task { await rehydrateBallotJournal() }
        }
        .alert(String(localized: "common.error"), isPresented: $showError) {
            if ballotRetryAvailable {
                Button(String(localized: "common.retry")) {
                    Task { await retryPendingCommand() }
                }
            }
            Button(String(localized: "common.ok"), role: .cancel) {}
        } message: {
            Text(errorMessage)
        }
        .alert(String(localized: "common.success"), isPresented: $showSuccess) {
            Button(String(localized: "common.ok"), role: .cancel) {
                onVoteSubmitted()
            }
        } message: {
            Text(String(localized: "poll.voting.success_message"))
        }
        .alert(
            String(localized: "poll.results.confirmation.pending_sync.title"),
            isPresented: $localPending
        ) {
            Button(String(localized: "common.retry")) {
                Task { await retryLocalPendingSync() }
            }
            Button(String(localized: "common.continue")) {
                onVoteSubmitted()
            }
        } message: {
            Text(String(localized: "poll.results.confirmation.pending_sync.message"))
        }
    }

    private func checkExistingVotes() {
        if let poll = repository.getPoll(eventId: event.id) {
            if let participantVoteMap = poll.votes[participantId] {
                hasVoted = repository.hasCompleteBallot(
                    eventId: event.id,
                    participantId: participantId
                )

                votes = participantVoteMap.reduce(into: [String: PollVote]()) { result, entry in
                    switch entry.value {
                    case .yes:
                        result[entry.key] = .yes
                    case .maybe:
                        result[entry.key] = .maybe
                    case .no:
                        result[entry.key] = .no
                    default:
                        break
                    }
                }
            }
        }
    }

    private func handleBack() async {
        let projections = journal.loadRehydrationProjections(
            eventId: event.id,
            actorId: participantId
        )
        guard !projections.isEmpty else {
            onBack()
            return
        }
        guard projections.allSatisfy({ $0 is BallotJournalProjectionValid }) else { return }
        guard let valid = projections.first as? BallotJournalProjectionValid else { return }
        let command = PollBallotContract.shared.command(envelope: valid.record.command)
        let operationKey = valid.record.operationKey
        let ballotFingerprint = valid.record.command.ballotFingerprint
        switch valid.record.status {
        case .dispatched:
            let marked: any BallotJournalResult
            do {
                marked = try await journal.markOutcomeUnknownIfDispatched(
                    operationKey: operationKey,
                    ballotFingerprint: ballotFingerprint
                )
            } catch {
                return
            }
            if !(marked is BallotJournalResultStored) { return }
            do {
                let proof = try await repository.resolveCompleteBallotOutcome(command: command)
                if let committed = proof as? PollBallotContractResolutionResultCommitted {
                    presentCommittedReceipt(committed.receipt)
                    return
                }
                guard let notCommitted = proof as? PollBallotContractResolutionResultProvenNotCommitted,
                      notCommitted.operationKey == operationKey,
                      notCommitted.ballotFingerprint == ballotFingerprint else { return }
                let tombstone = try await journal.tombstoneDispatchedCommand(
                    command: command,
                    destination: PollBallotContractBallotTerminalDestinationCancelled.shared
                )
                guard tombstone is BallotJournalResultStored else { return }
                let resolved = journal.loadRehydrationProjections(
                    eventId: event.id,
                    actorId: participantId
                )
                guard let durable = resolved.first as? BallotJournalProjectionValid,
                      durable.record.operationKey == operationKey,
                      durable.record.command.ballotFingerprint == ballotFingerprint,
                      durable.record.status == .dispatchCancellationTombstoned,
                      consumeTerminalDestination(durable.record.terminalDestination) else { return }
                pendingCommand = nil
                ballotRetryAvailable = false
                onBack()
            } catch {
                return
            }
        case .stagedNotDispatched:
            let cancelled: any BallotJournalResult
            do {
                cancelled = try await journal.cancelCommand(command: command)
            } catch {
                return
            }
            guard cancelled is BallotJournalResultStored else { return }
            onBack()
        case .cancelled:
            onBack()
        case .dispatchCancellationTombstoned:
            switch valid.record.terminalDestination {
            case is PollBallotContractBallotTerminalDestinationCancelled:
                guard consumeTerminalDestination(valid.record.terminalDestination) else { return }
                pendingCommand = nil
                ballotRetryAvailable = false
                onBack()
            case is PollBallotContractBallotTerminalDestinationRevised:
                guard consumeTerminalDestination(valid.record.terminalDestination) else { return }
                pendingCommand = nil
                ballotRetryAvailable = false
            case is PollBallotContractBallotTerminalDestinationTerminalFailure:
                _ = consumeTerminalDestination(valid.record.terminalDestination)
            default:
                return
            }
        default:
            return
        }
    }

    private func consumeTerminalDestination(
        _ terminalDestination: (any PollBallotContractBallotTerminalDestination)?
    ) -> Bool {
        switch terminalDestination {
        case is PollBallotContractBallotTerminalDestinationCancelled,
             is PollBallotContractBallotTerminalDestinationRevised:
            return true
        case let failure as PollBallotContractBallotTerminalDestinationTerminalFailure:
            ballotRetryAvailable = false
            errorMessage = String(localized: "poll.voting.error.submit_failed")
            showError = true
            _ = failure.failureCode
            return false
        default:
            return false
        }
    }

    private func submitVotes() async {
        votingClosed = repository.isDeadlinePassed(deadline: event.deadline)
        guard !votingClosed, !isLoading else { return }

        // A durable DISPATCHED command is an outcome-unknown fence. It must be retried with
        // its exact persisted identity before a later submission may allocate another UUID.
        let durableProjections = journal.loadRehydrationProjections(
            eventId: event.id,
            actorId: participantId
        )
        if durableProjections.contains(where: { $0 is BallotJournalProjectionInconsistent }) {
            pendingCommand = nil
            ballotRetryAvailable = false
            errorMessage = String(localized: "poll.voting.error.submit_failed")
            showError = true
            return
        }
        if let dispatched = durableProjections
            .compactMap({ $0 as? BallotJournalProjectionValid })
            .first(where: { $0.record.status == .dispatched }) {
            pendingCommand = PollBallotContract.shared.command(envelope: dispatched.record.command)
            ballotRetryAvailable = true
            errorMessage = String(localized: "poll.voting.error.submit_failed")
            showError = true
            return
        }

        let entries: [PollBallotContract.BallotEntry] = event.proposedSlots.compactMap { slot in
            guard let pollVote = votes[slot.id] else { return nil }
            let sharedVote: Vote_ = switch pollVote {
            case .yes: .yes
            case .maybe: .maybe
            case .no: .no
            }
            return PollBallotContract.BallotEntry(slotId: slot.id, vote: sharedVote)
        }
        guard entries.count == event.proposedSlots.count else { return }

        let command = PollBallotContract.CommitCompleteBallotCommand(
            eventId: event.id,
            actorId: participantId,
            pollRevision: event.aggregateRevision,
            entries: entries,
            operationId: "poll-ballot-\(UUID().uuidString.lowercased())",
            authoritativeDeadlineIso: event.deadline
        )
        pendingCommand = command
        isLoading = true

        do {
            let staged = try await journal.stageCommand(command: command)
            guard staged is BallotJournalResultStored else {
                pendingCommand = nil
                ballotRetryAvailable = false
                throw BallotJournalFailure.stage
            }
            let dispatched = try await journal.markCommandDispatched(command: command)
            guard dispatched is BallotJournalResultStored else {
                do {
                    _ = try await journal.cancelCommand(command: command)
                } catch {
                    // The durable STAGED record remains visible for fail-closed rehydration.
                }
                pendingCommand = nil
                ballotRetryAvailable = false
                throw BallotJournalFailure.dispatch
            }
            await dispatchPersistedCommand(command)
        } catch {
            isLoading = false
            WakeveHaptics.warning()
            errorMessage = error.localizedDescription
            showError = true
        }
    }

    /// Only durable DISPATCHED records are eligible for unknown-outcome rehydration.
    private func rehydrateBallotJournal() async {
        let projections = journal.loadRehydrationProjections(
            eventId: event.id,
            actorId: participantId
        )
        if let inconsistency = projections.first(where: {
            $0 is BallotJournalProjectionInconsistent
        }) as? BallotJournalProjectionInconsistent {
            pendingCommand = nil
            ballotRetryAvailable = inconsistency.retryable
            errorMessage = String(localized: "poll.voting.error.submit_failed")
            showError = true
            return
        }
        guard let valid = projections.first as? BallotJournalProjectionValid else { return }
        let command = PollBallotContract.shared.command(envelope: valid.record.command)
        switch valid.record.status {
        case .dispatched:
            pendingCommand = command
            await dispatchPersistedCommand(command)
        case .stagedNotDispatched:
            do {
                let cancelled = try await journal.cancelCommand(command: command)
                if cancelled is BallotJournalResultStored {
                    pendingCommand = nil
                    ballotRetryAvailable = false
                }
            } catch {
                return
            }
        case .cancelled:
            pendingCommand = nil
            ballotRetryAvailable = false
        case .dispatchCancellationTombstoned:
            switch valid.record.terminalDestination {
            case is PollBallotContractBallotTerminalDestinationCancelled:
                guard consumeTerminalDestination(valid.record.terminalDestination) else { return }
                pendingCommand = nil
                ballotRetryAvailable = false
                onBack()
            case is PollBallotContractBallotTerminalDestinationRevised:
                guard consumeTerminalDestination(valid.record.terminalDestination) else { return }
                pendingCommand = nil
                ballotRetryAvailable = false
            case is PollBallotContractBallotTerminalDestinationTerminalFailure:
                _ = consumeTerminalDestination(valid.record.terminalDestination)
                pendingCommand = nil
                ballotRetryAvailable = false
            default:
                pendingCommand = nil
                ballotRetryAvailable = false
                errorMessage = String(localized: "poll.voting.error.submit_failed")
                showError = true
            }
        default:
            pendingCommand = nil
            ballotRetryAvailable = false
        }
    }

    private func retryPendingCommand() async {
        let durableCommands = journal.loadDispatchableCommands(
            eventId: event.id,
            actorId: participantId
        )
        guard durableCommands.count == 1, let command = durableCommands.first else {
            ballotRetryAvailable = false
            pendingCommand = nil
            errorMessage = String(localized: "poll.voting.error.submit_failed")
            showError = true
            return
        }
        pendingCommand = command
        await dispatchPersistedCommand(command)
    }

    private func dispatchPersistedCommand(
        _ command: PollBallotContract.CommitCompleteBallotCommand
    ) async {
        isLoading = true
        do {
            let result = try await repository.commitCompleteBallot(command: command)
            isLoading = false
            if let committed = result as? PollBallotContractCommitResultCommitted {
                presentCommittedReceipt(committed.receipt)
            } else if let replay = result as? PollBallotContractCommitResultAlreadyCommitted {
                presentCommittedReceipt(replay.receipt)
            } else if let rejected = result as? PollBallotContractCommitResultRejected {
                if rejected.failure.code == PollBallotContract.FailureCode.deadlineReached {
                    votingClosed = true
                }
                ballotRetryAvailable = rejected.failure.retryable
                if !rejected.failure.retryable {
                    do {
                        let tombstone = try await journal.tombstoneDispatchedCommand(
                            command: command,
                            destination: PollBallotContractBallotTerminalDestinationTerminalFailure(
                                failureCode: rejected.failure.code,
                                commitOutcome: rejected.failure.commitOutcome
                            )
                        )
                        if tombstone is BallotJournalResultStored {
                            let durable = journal.loadRehydrationProjections(
                                eventId: event.id,
                                actorId: participantId
                            )
                            if let record = durable.first as? BallotJournalProjectionValid,
                               record.record.status == .dispatchCancellationTombstoned,
                               record.record.terminalDestination is PollBallotContractBallotTerminalDestinationTerminalFailure {
                                pendingCommand = nil
                            }
                        }
                    } catch {
                        // Keep the DISPATCHED command available; the durable CAS did not complete.
                    }
                }
                WakeveHaptics.warning()
                errorMessage = String(localized: "poll.voting.error.submit_failed")
                showError = true
            }
        } catch {
            isLoading = false
            ballotRetryAvailable = true
            WakeveHaptics.warning()
            errorMessage = error.localizedDescription
            showError = true
        }
    }

    private func presentCommittedReceipt(_ receipt: PollBallotContract.Receipt) {
        hasVoted = true
        ballotRetryAvailable = false
        pendingReceipt = receipt
        WakeveHaptics.success()
        if receipt.syncStatus == PollBallotContract.SyncStatus.localPending {
            localPending = true
        } else {
            showSuccess = true
        }
    }

    /// Sync retry replays the durable BallotSyncPayload; it never creates a second ballot.
    private func retryLocalPendingSync() async {
        guard let receipt = pendingReceipt else { return }
        isLoading = true
        do {
            let acknowledged = try await RepositoryProvider.shared.syncManager
                .retryPendingBallotSync(localReceiptId: receipt.receiptId)
            isLoading = false
            guard acknowledged.boolValue else {
                errorMessage = String(localized: "poll.voting.error.submit_failed")
                showError = true
                return
            }
            localPending = false
            showSuccess = true
        } catch {
            isLoading = false
            errorMessage = error.localizedDescription
            showError = true
        }
    }
}

private enum BallotJournalFailure: LocalizedError {
    case stage
    case dispatch

    var errorDescription: String? {
        String(localized: "poll.voting.error.submit_failed")
    }
}

// MARK: - Poll Voting Content View

struct PollVotingContentView: View {
    @Environment(\.colorScheme) private var colorScheme
    @State private var activeSlotIndex = 0

    let event: Event
    @Binding var votes: [String: PollVote]
    let isLoading: Bool
    let hasVoted: Bool
    var votingClosed: Bool = false
    let onSubmitVotes: () -> Void
    let onBack: () -> Void

    var body: some View {
        ZStack {
            SemanticColor.appBackground(for: colorScheme)
                .ignoresSafeArea()

            ScrollView {
                VStack(alignment: .leading, spacing: WakeveTheme.Spacing.lg) {
                    headerQuestion

                    if hasVoted {
                        voteSubmittedCard
                    } else if event.proposedSlots.isEmpty {
                        EmptyState(
                            systemImage: "calendar.badge.exclamationmark",
                            title: String(localized: "poll.voting.empty_title"),
                            subtitle: String(localized: "poll.voting.empty_subtitle")
                        )
                    } else {
                        progressCard
                        activeSlotQuestionCard
                        voteOptions
                        selectedVoteFeedback
                    }
                }
                .padding(.horizontal, WakeveTheme.Spacing.page)
                .padding(.top, 92)
                .padding(.bottom, hasVoted ? 36 : 118)
            }
        }
        .toolbar(.hidden, for: .tabBar)
        .safeAreaInset(edge: .top, spacing: 0) {
            toolbar
        }
        .safeAreaInset(edge: .bottom, spacing: 0) {
            if !hasVoted && !event.proposedSlots.isEmpty {
                bottomNextAction
            }
        }
        .onChange(of: event.proposedSlots.count) { _, newCount in
            activeSlotIndex = min(activeSlotIndex, max(0, newCount - 1))
        }
    }

    private var toolbar: some View {
        LiquidGlassToolbar(
            title: String(localized: "poll.voting.title"),
            subtitle: String(format: String(localized: "poll.voting.responses_progress_format"), completedCount, event.proposedSlots.count)
        ) {
            WakeveCircleButton(
                systemImage: "chevron.left",
                accessibilityLabel: String(localized: "common.back"),
                variant: .glass,
                size: 40,
                action: onBack
            )
        } trailing: {
            if let deadline = formatDeadlineShort(event.deadline) {
                Text(deadline)
                    .font(WakeveTheme.Typography.tiny)
                    .foregroundColor(SemanticColor.secondaryText(for: colorScheme))
                    .lineLimit(1)
                    .minimumScaleFactor(0.78)
                    .padding(.horizontal, WakeveTheme.Spacing.sm)
                    .frame(height: 34)
                    .background(SemanticColor.badge(for: colorScheme))
                    .clipShape(Capsule())
            }
        }
        .padding(.horizontal, WakeveTheme.Spacing.page)
        .padding(.top, WakeveTheme.Spacing.sm)
        .padding(.bottom, WakeveTheme.Spacing.xs)
        .background(
            LinearGradient(
                colors: [
                    SemanticColor.appBackground(for: colorScheme),
                    SemanticColor.appBackground(for: colorScheme).opacity(0)
                ],
                startPoint: .top,
                endPoint: .bottom
            )
            .ignoresSafeArea(edges: .top)
        )
    }

    private var headerQuestion: some View {
        VStack(alignment: .leading, spacing: WakeveTheme.Spacing.sm) {
            Text(event.title)
                .font(TypographyTokens.screenTitle)
                .foregroundColor(SemanticColor.primaryText(for: colorScheme))
                .lineLimit(2)
                .minimumScaleFactor(0.76)

            Text(String(localized: "poll.voting.header_question"))
                .font(TypographyTokens.callout)
                .foregroundColor(SemanticColor.secondaryText(for: colorScheme))
                .lineLimit(3)
        }
    }

    private var voteSubmittedCard: some View {
        WakeveContentCard(prominence: .prominent, cornerRadius: WakeveTheme.Radius.xl, padding: WakeveTheme.Spacing.xl) {
            VStack(spacing: WakeveTheme.Spacing.lg) {
                Image(systemName: "checkmark.circle.fill")
                    .font(.system(size: 58, weight: .semibold))
                    .foregroundColor(SemanticColor.confirmation(for: colorScheme))

                VStack(spacing: WakeveTheme.Spacing.xs) {
                    Text(String(localized: "poll.voting.submitted_title"))
                        .font(TypographyTokens.cardTitle)
                        .foregroundColor(SemanticColor.primaryText(for: colorScheme))

                    Text(String(localized: "poll.voting.submitted_subtitle"))
                        .font(TypographyTokens.callout)
                        .foregroundColor(SemanticColor.secondaryText(for: colorScheme))
                        .multilineTextAlignment(.center)
                }
            }
            .frame(maxWidth: .infinity)
        }
    }

    private var progressCard: some View {
        WakeveContentCard(prominence: .subtle, cornerRadius: WakeveTheme.Radius.xl, padding: WakeveTheme.Spacing.md) {
            VStack(alignment: .leading, spacing: WakeveTheme.Spacing.sm) {
                HStack {
                    Text(String(localized: "poll.voting.progress"))
                        .font(TypographyTokens.caption)
                        .foregroundColor(SemanticColor.secondaryText(for: colorScheme))
                    Spacer()
                    Text("\(activeSlotIndex + 1) / \(event.proposedSlots.count)")
                        .font(TypographyTokens.caption)
                        .foregroundColor(SemanticColor.primaryText(for: colorScheme))
                }

                GeometryReader { proxy in
                    Capsule()
                        .fill(SemanticColor.badge(for: colorScheme))
                        .overlay(alignment: .leading) {
                            Capsule()
                                .fill(SemanticColor.progress(for: colorScheme))
                                .frame(width: proxy.size.width * progressValue)
                        }
                }
                .frame(height: 8)
            }
        }
    }

    private var activeSlotQuestionCard: some View {
        WakeveContentCard(prominence: .regular, cornerRadius: WakeveTheme.Radius.xl, padding: WakeveTheme.Spacing.xl) {
            VStack(alignment: .leading, spacing: WakeveTheme.Spacing.md) {
                Text(String(localized: "poll.voting.slot_question"))
                    .font(TypographyTokens.cardTitle)
                    .foregroundColor(SemanticColor.primaryText(for: colorScheme))

                Text(formatDate(activeSlot.start ?? "", timezone: activeSlot.timezone))
                    .font(WakeveTheme.Typography.title2)
                    .foregroundColor(SemanticColor.primaryText(for: colorScheme))
                    .lineLimit(2)

                HStack(spacing: WakeveTheme.Spacing.xs) {
                    Image(systemName: "clock")
                        .font(.caption.weight(.bold))
                    Text("\(formatTime(activeSlot.start ?? "", timezone: activeSlot.timezone)) - \(formatTime(activeSlot.end ?? "", timezone: activeSlot.timezone))")
                        .font(TypographyTokens.metadata)
                }
                .foregroundColor(SemanticColor.secondaryText(for: colorScheme))

                PollTimeZoneBadge(
                    label: formatTimeZoneLabel(activeSlot.timezone, at: activeSlot.start),
                    foregroundColor: SemanticColor.secondaryText(for: colorScheme),
                    backgroundColor: SemanticColor.badge(for: colorScheme)
                )
            }
            .frame(maxWidth: .infinity, alignment: .leading)
        }
    }

    private var voteOptions: some View {
        VStack(spacing: WakeveTheme.Spacing.sm) {
            VoteOptionCard(
                vote: .yes,
                title: String(localized: "poll.results.vote.yes"),
                subtitle: String(localized: "poll.voting.option.yes.subtitle"),
                isSelected: activeVote == .yes,
                isDisabled: votingClosed
            ) {
                selectVote(.yes)
            }

            VoteOptionCard(
                vote: .maybe,
                title: String(localized: "poll.results.vote.maybe"),
                subtitle: String(localized: "poll.voting.option.maybe.subtitle"),
                isSelected: activeVote == .maybe,
                isDisabled: votingClosed
            ) {
                selectVote(.maybe)
            }

            VoteOptionCard(
                vote: .no,
                title: String(localized: "poll.results.vote.no"),
                subtitle: String(localized: "poll.voting.option.no.subtitle"),
                isSelected: activeVote == .no,
                isDisabled: votingClosed
            ) {
                selectVote(.no)
            }
        }
        .opacity(votingClosed ? 0.55 : 1)
    }

    @ViewBuilder
    private var selectedVoteFeedback: some View {
        if let activeVote {
            HStack(spacing: WakeveTheme.Spacing.sm) {
                Image(systemName: "checkmark.circle.fill")
                    .foregroundColor(SemanticColor.confirmation(for: colorScheme))
                Text(feedbackText(for: activeVote))
                    .font(TypographyTokens.callout)
                    .foregroundColor(SemanticColor.secondaryText(for: colorScheme))
            }
            .padding(.horizontal, WakeveTheme.Spacing.md)
            .padding(.vertical, WakeveTheme.Spacing.sm)
            .background(SemanticColor.confirmation(for: colorScheme).opacity(0.12))
            .clipShape(Capsule())
        }
    }

    private var bottomNextAction: some View {
        VStack(spacing: 0) {
            LinearGradient(
                colors: [
                    SemanticColor.appBackground(for: colorScheme).opacity(0),
                    SemanticColor.appBackground(for: colorScheme)
                ],
                startPoint: .top,
                endPoint: .bottom
            )
            .frame(height: 32)
            .allowsHitTesting(false)

            HStack(spacing: WakeveTheme.Spacing.sm) {
                if activeSlotIndex > 0 {
                    Button {
                        activeSlotIndex -= 1
                    } label: {
                        Image(systemName: "chevron.left")
                            .font(.headline.weight(.bold))
                            .foregroundColor(SemanticColor.primaryText(for: colorScheme))
                            .frame(width: 52, height: 52)
                            .background(SemanticColor.badge(for: colorScheme))
                            .clipShape(Circle())
                            .liquidGlass(cornerRadius: WakeveTheme.Radius.full)
                    }
                    .buttonStyle(.plain)
                }

                LiquidGlassButton(
                    nextActionTitle,
                    systemImage: isLastSlot ? "paperplane.fill" : "arrow.right",
                    variant: .primary,
                    isDisabled: votingClosed || activeVote == nil || isLoading || (isLastSlot && !canSubmitVotes),
                    isLoading: isLoading,
                    action: advanceOrSubmit
                )
            }
            .padding(.horizontal, WakeveTheme.Spacing.page)
            .padding(.bottom, WakeveTheme.Spacing.sm)
            .background(SemanticColor.appBackground(for: colorScheme))
        }
    }

    private var activeSlot: TimeSlot {
        event.proposedSlots[safeActiveIndex]
    }

    private var safeActiveIndex: Int {
        min(max(activeSlotIndex, 0), max(event.proposedSlots.count - 1, 0))
    }

    private var activeVote: PollVote? {
        votes[activeSlot.id]
    }

    private var completedCount: Int {
        event.proposedSlots.filter { votes[$0.id] != nil }.count
    }

    private var progressValue: CGFloat {
        guard !event.proposedSlots.isEmpty else { return 0 }
        return CGFloat(activeSlotIndex + 1) / CGFloat(event.proposedSlots.count)
    }

    private var isLastSlot: Bool {
        activeSlotIndex >= event.proposedSlots.count - 1
    }

    private var canSubmitVotes: Bool {
        !hasVoted && !event.proposedSlots.isEmpty && votes.count == event.proposedSlots.count
    }

    private var nextActionTitle: String {
        isLastSlot ? String(localized: "poll.voting.submit_votes") : String(localized: "poll.voting.next_slot")
    }

    private func advanceOrSubmit() {
        guard !votingClosed, activeVote != nil else { return }
        if isLastSlot {
            onSubmitVotes()
        } else {
            WakeveHaptics.selection()
            activeSlotIndex = min(activeSlotIndex + 1, event.proposedSlots.count - 1)
        }
    }

    private func selectVote(_ vote: PollVote) {
        WakeveHaptics.selection()
        votes[activeSlot.id] = vote
    }

    private func feedbackText(for vote: PollVote) -> String {
        switch vote {
        case .yes: return String(localized: "poll.voting.feedback.yes")
        case .maybe: return String(localized: "poll.voting.feedback.maybe")
        case .no: return String(localized: "poll.voting.feedback.no")
        }
    }

    private func formatDeadlineShort(_ deadlineString: String) -> String? {
        if let date = ISO8601DateFormatter().date(from: deadlineString) {
            let formatter = DateFormatter()
            formatter.locale = .autoupdatingCurrent
            formatter.dateFormat = "MMM d"
            return String(format: String(localized: "poll.voting.deadline_before_format"), formatter.string(from: date))
        }
        return nil
    }

    private func formatDate(_ dateString: String, timezone: String) -> String {
        if let date = ISO8601DateFormatter().date(from: dateString) {
            let formatter = DateFormatter()
            formatter.locale = .autoupdatingCurrent
            formatter.timeZone = timeZone(for: timezone)
            formatter.dateFormat = "EEEE d MMM"
            return formatter.string(from: date)
        }
        return dateString
    }

    private func formatTime(_ dateString: String, timezone: String) -> String {
        if let date = ISO8601DateFormatter().date(from: dateString) {
            let formatter = DateFormatter()
            formatter.locale = .autoupdatingCurrent
            formatter.timeZone = timeZone(for: timezone)
            formatter.timeStyle = .short
            return formatter.string(from: date)
        }
        return dateString
    }

    private func timeZone(for identifier: String) -> TimeZone {
        TimeZone(identifier: identifier.trimmingCharacters(in: .whitespacesAndNewlines)) ?? .current
    }

    private func formatTimeZoneLabel(_ identifier: String, at dateString: String?) -> String {
        String(
            format: String(localized: "poll.timezone.label_format"),
            formatTimeZoneDisplay(identifier, at: dateString)
        )
    }

    private func formatTimeZoneDisplay(_ identifier: String, at dateString: String?) -> String {
        let trimmed = identifier.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return TimeZone.current.identifier }
        guard let timeZone = TimeZone(identifier: trimmed) else {
            return trimmed.replacingOccurrences(of: "_", with: " ")
        }

        let date = dateString.flatMap { ISO8601DateFormatter().date(from: $0) }
        let abbreviation = date.flatMap { timeZone.abbreviation(for: $0) } ?? timeZone.abbreviation() ?? trimmed
        let city = trimmed.split(separator: "/").last.map { String($0).replacingOccurrences(of: "_", with: " ") } ?? trimmed

        return city == abbreviation ? abbreviation : "\(abbreviation) · \(city)"
    }
}

// MARK: - Vote Guide Row

struct VoteGuideRow: View {
    let icon: String
    let color: Color
    let title: String
    let description: String

    var body: some View {
        HStack(spacing: 12) {
            ZStack {
                Circle()
                    .fill(color.opacity(0.15))
                    .frame(width: 32, height: 32)

                Image(systemName: icon)
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundColor(color)
            }

            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                    .font(WakeveTheme.Typography.metadata)
                    .foregroundColor(.primary)

                Text(description)
                    .font(WakeveTheme.Typography.caption)
                    .foregroundColor(.secondary)
            }

            Spacer()
        }
    }
}

// MARK: - Time Slot Vote Card

struct TimeSlotVoteCard: View {
    let timeSlot: TimeSlot
    let selectedVote: PollVote?
    let onVoteSelected: (PollVote) -> Void

    var body: some View {
        WakeveContentCard(cornerRadius: WakeveTheme.Radius.xl) {
            VStack(spacing: WakeveTheme.Spacing.md) {
                VStack(spacing: 6) {
                Text(formatDate(timeSlot.start ?? "", timezone: timeSlot.timezone))
                    .font(WakeveTheme.Typography.bodySemibold)
                    .foregroundColor(.primary)

                HStack(spacing: 6) {
                    Image(systemName: "clock")
                        .font(.system(size: 14))
                        .foregroundColor(.secondary)

                    Text("\(formatTime(timeSlot.start ?? "", timezone: timeSlot.timezone)) - \(formatTime(timeSlot.end ?? "", timezone: timeSlot.timezone))")
                        .font(WakeveTheme.Typography.metadata)
                        .foregroundColor(.secondary)
                }

                PollTimeZoneBadge(
                    label: formatTimeZoneLabel(timeSlot.timezone, at: timeSlot.start),
                    foregroundColor: .secondary,
                    backgroundColor: Color(.tertiarySystemFill)
                )
            }

                WakeveSegmentedVoteControl(
                    selectedVote: selectedVote,
                    onVoteSelected: onVoteSelected
                )
            }
        }
    }

    private func formatDate(_ dateString: String, timezone: String) -> String {
        if let date = ISO8601DateFormatter().date(from: dateString) {
            let formatter = DateFormatter()
            formatter.locale = .autoupdatingCurrent
            formatter.timeZone = timeZone(for: timezone)
            formatter.dateFormat = "EEEE, MMM d"
            return formatter.string(from: date)
        }
        return dateString
    }

    private func formatTime(_ dateString: String, timezone: String) -> String {
        if let date = ISO8601DateFormatter().date(from: dateString) {
            let formatter = DateFormatter()
            formatter.locale = .current
            formatter.timeZone = timeZone(for: timezone)
            formatter.timeStyle = .short
            return formatter.string(from: date)
        }
        return dateString
    }

    private func timeZone(for identifier: String) -> TimeZone {
        TimeZone(identifier: identifier.trimmingCharacters(in: .whitespacesAndNewlines)) ?? .current
    }

    private func formatTimeZoneLabel(_ identifier: String, at dateString: String?) -> String {
        String(
            format: String(localized: "poll.timezone.label_format"),
            formatTimeZoneDisplay(identifier, at: dateString)
        )
    }

    private func formatTimeZoneDisplay(_ identifier: String, at dateString: String?) -> String {
        let trimmed = identifier.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return TimeZone.current.identifier }
        guard let timeZone = TimeZone(identifier: trimmed) else {
            return trimmed.replacingOccurrences(of: "_", with: " ")
        }

        let date = dateString.flatMap { ISO8601DateFormatter().date(from: $0) }
        let abbreviation = date.flatMap { timeZone.abbreviation(for: $0) } ?? timeZone.abbreviation() ?? trimmed
        let city = trimmed.split(separator: "/").last.map { String($0).replacingOccurrences(of: "_", with: " ") } ?? trimmed

        return city == abbreviation ? abbreviation : "\(abbreviation) · \(city)"
    }
}

struct PollTimeZoneBadge: View {
    let label: String
    let foregroundColor: Color
    let backgroundColor: Color

    var body: some View {
        Label(label, systemImage: "globe")
            .font(TypographyTokens.caption)
            .foregroundColor(foregroundColor)
            .lineLimit(1)
            .minimumScaleFactor(0.78)
            .padding(.horizontal, WakeveTheme.Spacing.sm)
            .frame(height: 30)
            .background(backgroundColor)
            .clipShape(Capsule())
    }
}

// MARK: - Preview

#if DEBUG
#Preview("Poll Voting - Empty Votes Light") {
    PollVotingPreviewHarness(
        event: EventFactory.polling,
        initialVotes: [:],
        hasVoted: false
    )
    .preferredColorScheme(.light)
}

#Preview("Poll Voting - Empty Votes Dark") {
    PollVotingPreviewHarness(
        event: EventFactory.polling,
        initialVotes: [:],
        hasVoted: false
    )
    .preferredColorScheme(.dark)
}

#Preview("Poll Voting - Ready To Submit") {
    PollVotingPreviewHarness(
        event: EventFactory.polling,
        initialVotes: EventFactory.pollingPreviewVotes,
        hasVoted: false
    )
}

#Preview("Poll Voting - Already Voted") {
    PollVotingPreviewHarness(
        event: EventFactory.polling,
        initialVotes: EventFactory.pollingPreviewVotes,
        hasVoted: true
    )
}

private struct PollVotingPreviewHarness: View {
    let event: Event
    @State private var votes: [String: PollVote]
    let hasVoted: Bool

    init(event: Event, initialVotes: [String: PollVote], hasVoted: Bool) {
        self.event = event
        self._votes = State(initialValue: initialVotes)
        self.hasVoted = hasVoted
    }

    var body: some View {
        PollVotingContentView(
            event: event,
            votes: $votes,
            isLoading: false,
            hasVoted: hasVoted,
            onSubmitVotes: {},
            onBack: {}
        )
    }
}
#endif
