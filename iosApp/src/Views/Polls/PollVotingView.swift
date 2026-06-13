import SwiftUI
import Shared

/// Poll voting view inspired by Apple Invites
/// Features: Clean design, card-based time slots, clear voting options
struct PollVotingView: View {
    let event: Event
    let repository: EventRepositoryInterface
    let participantId: String
    let onVoteSubmitted: () -> Void
    let onBack: () -> Void

    @State private var votes: [String: PollVote] = [:]
    @State private var isLoading = false
    @State private var errorMessage = ""
    @State private var showError = false
    @State private var showSuccess = false
    @State private var hasVoted = false

    var body: some View {
        PollVotingContentView(
            event: event,
            votes: $votes,
            isLoading: isLoading,
            hasVoted: hasVoted,
            onSubmitVotes: {
                Task { await submitVotes() }
            },
            onBack: onBack
        )
        .onAppear {
            checkExistingVotes()
        }
        .alert("Error", isPresented: $showError) {
            Button("OK", role: .cancel) {}
        } message: {
            Text(errorMessage)
        }
        .alert("Success", isPresented: $showSuccess) {
            Button("OK", role: .cancel) {
                onVoteSubmitted()
            }
        } message: {
            Text("Your votes have been submitted successfully!")
        }
    }

    private func checkExistingVotes() {
        if let poll = repository.getPoll(eventId: event.id) {
            if let participantVoteMap = poll.votes[participantId] {
                hasVoted = !participantVoteMap.isEmpty

                if hasVoted {
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
    }

    private func submitVotes() async {
        guard votes.count == event.proposedSlots.count else { return }

        isLoading = true

        var successCount = 0
        var lastError: Error?

        for (slotId, pollVote) in votes {
            do {
                let sharedVote: Vote_ = {
                    switch pollVote {
                    case .yes: return .yes
                    case .maybe: return .maybe
                    case .no: return .no
                    }
                }()

                _ = try await repository.addVote(
                    eventId: event.id,
                    participantId: participantId,
                    slotId: slotId,
                    vote: sharedVote
                )

                let submittedVotes = repository.getPoll(eventId: event.id)?.votes[participantId] as? [String: Vote_]
                if submittedVotes?[slotId] != nil {
                    successCount += 1
                }
            } catch {
                lastError = error
            }
        }

        isLoading = false

        if successCount == votes.count {
            hasVoted = true
            showSuccess = true
        } else {
            errorMessage = lastError?.localizedDescription ?? "Failed to submit some votes"
            showError = true
        }
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
    let onSubmitVotes: () -> Void
    let onBack: () -> Void

    var body: some View {
        ZStack {
            WakeveTheme.ColorToken.pageBackground(for: colorScheme)
                .ignoresSafeArea()

            ScrollView {
                VStack(alignment: .leading, spacing: WakeveTheme.Spacing.lg) {
                    headerQuestion

                    if hasVoted {
                        voteSubmittedCard
                    } else if event.proposedSlots.isEmpty {
                        EmptyState(
                            systemImage: "calendar.badge.exclamationmark",
                            title: "Aucun créneau",
                            subtitle: "L’organisateur doit ajouter au moins une option avant le vote."
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
        LiquidGlassToolbar(title: "Vote", subtitle: "\(completedCount) / \(event.proposedSlots.count) réponses") {
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
                    .foregroundColor(WakeveTheme.ColorToken.secondaryText(for: colorScheme))
                    .lineLimit(1)
                    .minimumScaleFactor(0.78)
                    .padding(.horizontal, WakeveTheme.Spacing.sm)
                    .frame(height: 34)
                    .background(WakeveTheme.ColorToken.controlFill(for: colorScheme))
                    .clipShape(Capsule())
            }
        }
        .padding(.horizontal, WakeveTheme.Spacing.page)
        .padding(.top, WakeveTheme.Spacing.sm)
        .padding(.bottom, WakeveTheme.Spacing.xs)
        .background(
            LinearGradient(
                colors: [
                    WakeveTheme.ColorToken.pageBackground(for: colorScheme),
                    WakeveTheme.ColorToken.pageBackground(for: colorScheme).opacity(0)
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
                .font(WakeveTheme.Typography.largeTitle)
                .foregroundColor(WakeveTheme.ColorToken.primaryText(for: colorScheme))
                .lineLimit(2)
                .minimumScaleFactor(0.76)

            Text("Une question à la fois: est-ce que ce créneau vous convient ?")
                .font(WakeveTheme.Typography.callout)
                .foregroundColor(WakeveTheme.ColorToken.secondaryText(for: colorScheme))
                .lineLimit(3)
        }
    }

    private var voteSubmittedCard: some View {
        LiquidGlassCard(prominence: .prominent, cornerRadius: WakeveTheme.Radius.xl, padding: WakeveTheme.Spacing.xl) {
            VStack(spacing: WakeveTheme.Spacing.lg) {
                Image(systemName: "checkmark.circle.fill")
                    .font(.system(size: 58, weight: .semibold))
                    .foregroundColor(WakeveTheme.ColorToken.confirmation(for: colorScheme))

                VStack(spacing: WakeveTheme.Spacing.xs) {
                    Text("Votes envoyés")
                        .font(WakeveTheme.Typography.section)
                        .foregroundColor(WakeveTheme.ColorToken.primaryText(for: colorScheme))

                    Text("Merci pour votre réponse. L’organisateur sera prévenu quand tout le monde aura voté.")
                        .font(WakeveTheme.Typography.callout)
                        .foregroundColor(WakeveTheme.ColorToken.secondaryText(for: colorScheme))
                        .multilineTextAlignment(.center)
                }
            }
            .frame(maxWidth: .infinity)
        }
    }

    private var progressCard: some View {
        LiquidGlassCard(prominence: .subtle, cornerRadius: WakeveTheme.Radius.xl, padding: WakeveTheme.Spacing.md) {
            VStack(alignment: .leading, spacing: WakeveTheme.Spacing.sm) {
                HStack {
                    Text("Progression")
                        .font(WakeveTheme.Typography.caption)
                        .foregroundColor(WakeveTheme.ColorToken.secondaryText(for: colorScheme))
                    Spacer()
                    Text("\(activeSlotIndex + 1) / \(event.proposedSlots.count)")
                        .font(WakeveTheme.Typography.caption)
                        .foregroundColor(WakeveTheme.ColorToken.primaryText(for: colorScheme))
                }

                GeometryReader { proxy in
                    Capsule()
                        .fill(WakeveTheme.ColorToken.controlFill(for: colorScheme))
                        .overlay(alignment: .leading) {
                            Capsule()
                                .fill(WakeveTheme.ColorToken.progress(for: colorScheme))
                                .frame(width: proxy.size.width * progressValue)
                        }
                }
                .frame(height: 8)
            }
        }
    }

    private var activeSlotQuestionCard: some View {
        LiquidGlassCard(prominence: .regular, cornerRadius: WakeveTheme.Radius.xl, padding: WakeveTheme.Spacing.xl) {
            VStack(alignment: .leading, spacing: WakeveTheme.Spacing.md) {
                Text("Ce créneau vous convient ?")
                    .font(WakeveTheme.Typography.section)
                    .foregroundColor(WakeveTheme.ColorToken.primaryText(for: colorScheme))

                Text(formatDate(activeSlot.start ?? ""))
                    .font(WakeveTheme.Typography.title2)
                    .foregroundColor(WakeveTheme.ColorToken.primaryText(for: colorScheme))
                    .lineLimit(2)

                HStack(spacing: WakeveTheme.Spacing.xs) {
                    Image(systemName: "clock")
                        .font(.caption.weight(.bold))
                    Text("\(formatTime(activeSlot.start ?? "")) - \(formatTime(activeSlot.end ?? ""))")
                        .font(WakeveTheme.Typography.metadata)
                }
                .foregroundColor(WakeveTheme.ColorToken.secondaryText(for: colorScheme))
            }
            .frame(maxWidth: .infinity, alignment: .leading)
        }
    }

    private var voteOptions: some View {
        VStack(spacing: WakeveTheme.Spacing.sm) {
            VoteOptionCard(
                vote: .yes,
                title: "Oui",
                subtitle: "Ce créneau me convient.",
                isSelected: activeVote == .yes
            ) {
                votes[activeSlot.id] = .yes
            }

            VoteOptionCard(
                vote: .maybe,
                title: "Peut-être",
                subtitle: "Possible si nécessaire.",
                isSelected: activeVote == .maybe
            ) {
                votes[activeSlot.id] = .maybe
            }

            VoteOptionCard(
                vote: .no,
                title: "Non",
                subtitle: "Je ne peux pas venir.",
                isSelected: activeVote == .no
            ) {
                votes[activeSlot.id] = .no
            }
        }
    }

    @ViewBuilder
    private var selectedVoteFeedback: some View {
        if let activeVote {
            HStack(spacing: WakeveTheme.Spacing.sm) {
                Image(systemName: "checkmark.circle.fill")
                    .foregroundColor(WakeveTheme.ColorToken.confirmation(for: colorScheme))
                Text(feedbackText(for: activeVote))
                    .font(WakeveTheme.Typography.callout)
                    .foregroundColor(WakeveTheme.ColorToken.secondaryText(for: colorScheme))
            }
            .padding(.horizontal, WakeveTheme.Spacing.md)
            .padding(.vertical, WakeveTheme.Spacing.sm)
            .background(WakeveTheme.ColorToken.confirmation(for: colorScheme).opacity(0.12))
            .clipShape(Capsule())
        }
    }

    private var bottomNextAction: some View {
        VStack(spacing: 0) {
            LinearGradient(
                colors: [
                    WakeveTheme.ColorToken.pageBackground(for: colorScheme).opacity(0),
                    WakeveTheme.ColorToken.pageBackground(for: colorScheme)
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
                            .foregroundColor(WakeveTheme.ColorToken.primaryText(for: colorScheme))
                            .frame(width: 52, height: 52)
                            .background(WakeveTheme.ColorToken.controlFill(for: colorScheme))
                            .clipShape(Circle())
                            .liquidGlass(cornerRadius: WakeveTheme.Radius.full)
                    }
                    .buttonStyle(.plain)
                }

                LiquidGlassButton(
                    nextActionTitle,
                    systemImage: isLastSlot ? "paperplane.fill" : "arrow.right",
                    variant: .primary,
                    isDisabled: activeVote == nil || isLoading || (isLastSlot && !canSubmitVotes),
                    isLoading: isLoading,
                    action: advanceOrSubmit
                )
            }
            .padding(.horizontal, WakeveTheme.Spacing.page)
            .padding(.bottom, WakeveTheme.Spacing.sm)
            .background(WakeveTheme.ColorToken.pageBackground(for: colorScheme))
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
        isLastSlot ? "Envoyer mes votes" : "Créneau suivant"
    }

    private func advanceOrSubmit() {
        guard activeVote != nil else { return }
        if isLastSlot {
            onSubmitVotes()
        } else {
            activeSlotIndex = min(activeSlotIndex + 1, event.proposedSlots.count - 1)
        }
    }

    private func feedbackText(for vote: PollVote) -> String {
        switch vote {
        case .yes: return "Réponse enregistrée: oui."
        case .maybe: return "Réponse enregistrée: peut-être."
        case .no: return "Réponse enregistrée: non."
        }
    }

    private func formatDeadlineShort(_ deadlineString: String) -> String? {
        if let date = ISO8601DateFormatter().date(from: deadlineString) {
            let formatter = DateFormatter()
            formatter.locale = Locale(identifier: "fr_FR")
            formatter.dateFormat = "MMM d"
            return "Avant le " + formatter.string(from: date)
        }
        return nil
    }

    private func formatDate(_ dateString: String) -> String {
        if let date = ISO8601DateFormatter().date(from: dateString) {
            let formatter = DateFormatter()
            formatter.locale = Locale(identifier: "fr_FR")
            formatter.dateFormat = "EEEE d MMM"
            return formatter.string(from: date)
        }
        return dateString
    }

    private func formatTime(_ dateString: String) -> String {
        if let date = ISO8601DateFormatter().date(from: dateString) {
            let formatter = DateFormatter()
            formatter.locale = Locale(identifier: "fr_FR")
            formatter.timeStyle = .short
            return formatter.string(from: date)
        }
        return dateString
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
                    .font(.system(size: 15, weight: .medium))
                    .foregroundColor(.primary)

                Text(description)
                    .font(.system(size: 13))
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
        WakeveGlassCard(cornerRadius: WakeveTheme.Radius.xl) {
            VStack(spacing: WakeveTheme.Spacing.md) {
                VStack(spacing: 6) {
                Text(formatDate(timeSlot.start ?? ""))
                    .font(WakeveTheme.Typography.bodySemibold)
                    .foregroundColor(.primary)

                HStack(spacing: 6) {
                    Image(systemName: "clock")
                        .font(.system(size: 14))
                        .foregroundColor(.secondary)

                    Text("\(formatTime(timeSlot.start ?? "")) - \(formatTime(timeSlot.end ?? ""))")
                        .font(WakeveTheme.Typography.metadata)
                        .foregroundColor(.secondary)
                }
            }

                WakeveSegmentedVoteControl(
                    selectedVote: selectedVote,
                    onVoteSelected: onVoteSelected
                )
            }
        }
    }

    private func formatDate(_ dateString: String) -> String {
        if let date = ISO8601DateFormatter().date(from: dateString) {
            let formatter = DateFormatter()
            formatter.dateFormat = "EEEE, MMM d"
            return formatter.string(from: date)
        }
        return dateString
    }

    private func formatTime(_ dateString: String) -> String {
        if let date = ISO8601DateFormatter().date(from: dateString) {
            let formatter = DateFormatter()
            formatter.timeStyle = .short
            return formatter.string(from: date)
        }
        return dateString
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
