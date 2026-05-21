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

    let event: Event
    @Binding var votes: [String: PollVote]
    let isLoading: Bool
    let hasVoted: Bool
    let onSubmitVotes: () -> Void
    let onBack: () -> Void

    var body: some View {
        ZStack {
            WakeveScreenBackground(style: .event)

            VStack(spacing: 0) {
                VStack(spacing: WakeveTheme.Spacing.md) {
                    HStack {
                        WakeveCircleButton(
                            systemImage: "chevron.left",
                            accessibilityLabel: "Retour",
                            variant: colorScheme == .dark ? .eventBack : .light,
                            size: 44,
                            action: onBack
                        )

                        Spacer()

                        if canSubmitVotes {
                            Button(action: onSubmitVotes) {
                                if isLoading {
                                    ProgressView()
                                        .progressViewStyle(CircularProgressViewStyle())
                                } else {
                                    Text("Suivant")
                                        .font(WakeveTheme.Typography.bodySemibold)
                                        .foregroundColor(nextButtonForeground)
                                        .padding(.horizontal, 22)
                                        .frame(height: 44)
                                        .background(nextButtonBackground)
                                        .clipShape(Capsule())
                                }
                            }
                            .disabled(isLoading)
                        }
                    }
                    .padding(.horizontal, WakeveTheme.Spacing.lg)
                    .padding(.top, WakeveTheme.Navigation.controlTopSpacing)

                    VStack(spacing: WakeveTheme.Spacing.xs) {
                        Text(event.title)
                            .font(WakeveTheme.Typography.display)
                            .foregroundColor(headerTextColor)
                            .multilineTextAlignment(.center)
                            .lineLimit(2)
                            .minimumScaleFactor(0.68)

                        Text("Choisissez vos disponibilités")
                            .font(WakeveTheme.Typography.rowTitle)
                            .foregroundColor(headerSecondaryTextColor)
                            .multilineTextAlignment(.center)
                    }
                    .padding(.horizontal, WakeveTheme.Spacing.lg)
                }

                ScrollView {
                    VStack(spacing: WakeveTheme.Spacing.md) {
                        if hasVoted {
                            WakeveGlassCard(cornerRadius: WakeveTheme.Radius.xl) {
                                VStack(spacing: WakeveTheme.Spacing.lg) {
                                ZStack {
                                    Circle()
                                        .fill(Color.green.opacity(0.1))
                                        .frame(width: 80, height: 80)

                                    Image(systemName: "checkmark")
                                        .font(.system(size: 36, weight: .semibold))
                                        .foregroundColor(.green)
                                }
                                .padding(.top, 40)

                                VStack(spacing: 8) {
                                    Text("Votes envoyés")
                                        .font(WakeveTheme.Typography.section)
                                        .foregroundColor(.primary)

                                    Text("Merci pour votre réponse")
                                        .font(WakeveTheme.Typography.body)
                                        .foregroundColor(.secondary)
                                }

                                Text("L’organisateur sera prévenu quand tout le monde aura voté.")
                                    .font(WakeveTheme.Typography.metadata)
                                    .foregroundColor(.secondary)
                                    .multilineTextAlignment(.center)
                                    .padding(.horizontal, WakeveTheme.Spacing.xl)
                            }
                                .frame(maxWidth: .infinity)
                            }
                        } else {
                            WakeveGlassCard(cornerRadius: WakeveTheme.Radius.xl) {
                                VStack(spacing: WakeveTheme.Spacing.md) {
                                HStack {
                                    Image(systemName: "info.circle.fill")
                                        .font(.system(size: 20))
                                        .foregroundColor(WakeveTheme.ColorToken.permissionBlue)

                                    Text("Comment voter")
                                        .font(WakeveTheme.Typography.bodySemibold)
                                        .foregroundColor(.primary)

                                    Spacer()
                                }

                                VStack(alignment: .leading, spacing: 12) {
                                    VoteGuideRow(
                                        icon: "checkmark",
                                        color: .green,
                                        title: "Oui",
                                        description: "Ce créneau me convient"
                                    )

                                    VoteGuideRow(
                                        icon: "questionmark",
                                        color: .orange,
                                        title: "Peut-être",
                                        description: "Possible si nécessaire"
                                    )

                                    VoteGuideRow(
                                        icon: "xmark",
                                        color: .red,
                                        title: "Non",
                                        description: "Je ne peux pas venir"
                                    )
                                }
                            }
                            }

                            HStack {
                                Text("\(votes.count) / \(event.proposedSlots.count) créneaux votés")
                                    .font(WakeveTheme.Typography.metadata)
                                    .foregroundColor(headerSecondaryTextColor)

                                Spacer()

                                if let deadline = formatDeadlineShort(event.deadline) {
                                    HStack(spacing: 4) {
                                        Image(systemName: "clock")
                                            .font(.system(size: 13))
                                        Text(deadline)
                                            .font(.system(size: 13))
                                    }
                                    .foregroundColor(headerSecondaryTextColor)
                                }
                            }
                            .padding(.horizontal, 4)

                            ForEach(event.proposedSlots.indices, id: \.self) { index in
                                let slot = event.proposedSlots[index]
                                TimeSlotVoteCard(
                                    timeSlot: slot,
                                    selectedVote: votes[slot.id],
                                    onVoteSelected: { vote in
                                        votes[slot.id] = vote
                                    }
                                )
                            }
                        }

                        Spacer()
                            .frame(height: 40)
                    }
                    .padding(.horizontal, 20)
                    .padding(.top, WakeveTheme.Spacing.md)
                }
            }
        }
    }

    private var canSubmitVotes: Bool {
        !hasVoted && !event.proposedSlots.isEmpty && votes.count == event.proposedSlots.count
    }

    private var headerTextColor: Color {
        WakeveTheme.ColorToken.primaryText(for: colorScheme)
    }

    private var headerSecondaryTextColor: Color {
        WakeveTheme.ColorToken.secondaryText(for: colorScheme)
    }

    private var nextButtonBackground: Color {
        colorScheme == .dark ? WakeveTheme.ColorToken.eventLilacAction : WakeveTheme.ColorToken.permissionBlue
    }

    private var nextButtonForeground: Color {
        colorScheme == .dark ? WakeveTheme.ColorToken.eventLilacText : .white
    }

    private func formatDeadlineShort(_ deadlineString: String) -> String? {
        if let date = ISO8601DateFormatter().date(from: deadlineString) {
            let formatter = DateFormatter()
            formatter.dateFormat = "MMM d"
            return "Avant le " + formatter.string(from: date)
        }
        return nil
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
