import SwiftUI
import Shared

/// Modern poll voting view inspired by Apple Invites
/// Features: Clean design, card-based time slots, clear voting options
struct ModernPollVotingView: View {
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
        ZStack {
            // Clean background
            Color(.systemGroupedBackground)
                .ignoresSafeArea()

            VStack(spacing: 0) {
                // Header
                VStack(spacing: 16) {
                    HStack {
                        Button(action: onBack) {
                            Image(systemName: "xmark")
                                .font(.system(size: 16, weight: .semibold))
                                .foregroundColor(.secondary)
                                .frame(width: 36, height: 36)
                                .background(.thinMaterial)
                                .clipShape(Circle())
                                .shadow(color: .black.opacity(0.05), radius: 4, x: 0, y: 2)
                        }

                        Spacer()

                        if !hasVoted && votes.count == event.proposedSlots.count {
                            Button {
                                Task {
                                    await submitVotes()
                                }
                            } label: {
                                if isLoading {
                                    ProgressView()
                                        .progressViewStyle(CircularProgressViewStyle())
                                } else {
                                    Text("Submit")
                                        .font(.system(size: 17, weight: .semibold))
                                        .foregroundColor(.blue)
                                }
                            }
                            .disabled(isLoading)
                        }
                    }
                    .padding(.horizontal, 20)
                    .padding(.top, 60)

                    VStack(spacing: 8) {
                        Text("Vote on Times")
                            .font(.system(size: 34, weight: .bold))
                            .foregroundColor(.primary)
                            .frame(maxWidth: .infinity, alignment: .leading)

                        Text(event.title)
                            .font(.system(size: 20, weight: .medium))
                            .foregroundColor(.secondary)
                            .frame(maxWidth: .infinity, alignment: .leading)
                    }
                    .padding(.horizontal, 20)
                }

                ScrollView {
                    VStack(spacing: 16) {
                        if hasVoted {
                            // Success State
                            VStack(spacing: 20) {
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
                                    Text("Votes Submitted")
                                        .font(.system(size: 24, weight: .bold))
                                        .foregroundColor(.primary)

                                    Text("Thank you for your response")
                                        .font(.system(size: 17))
                                        .foregroundColor(.secondary)
                                }

                                Text("The organizer will be notified when everyone has voted.")
                                    .font(.system(size: 15))
                                    .foregroundColor(.secondary)
                                    .multilineTextAlignment(.center)
                                    .padding(.horizontal, 40)
                                    .padding(.bottom, 40)
                            }
                            .frame(maxWidth: .infinity)
                            .padding(20)
                            .glassCard(cornerRadius: 20, material: .regularMaterial)
                        } else {
                            // Voting Instructions Card
                            VStack(spacing: 16) {
                                HStack {
                                    Image(systemName: "info.circle.fill")
                                        .font(.system(size: 20))
                                        .foregroundColor(.blue)

                                    Text("How to Vote")
                                        .font(.system(size: 17, weight: .semibold))
                                        .foregroundColor(.primary)

                                    Spacer()
                                }

                                VStack(alignment: .leading, spacing: 12) {
                                    VoteGuideRow(
                                        icon: "checkmark",
                                        color: .green,
                                        title: "Available",
                                        description: "This time works for me"
                                    )

                                    VoteGuideRow(
                                        icon: "questionmark",
                                        color: .orange,
                                        title: "Maybe",
                                        description: "Could work if needed"
                                    )

                                    VoteGuideRow(
                                        icon: "xmark",
                                        color: .red,
                                        title: "Not Available",
                                        description: "Can't make this time"
                                    )
                                }
                            }
                            .padding(20)
                            .glassCard(cornerRadius: 20, material: .regularMaterial)

                            // Progress Indicator
                            HStack {
                                Text("\(votes.count) of \(event.proposedSlots.count) voted")
                                    .font(.system(size: 15, weight: .medium))
                                    .foregroundColor(.secondary)

                                Spacer()

                                if let deadline = formatDeadlineShort(event.deadline) {
                                    HStack(spacing: 4) {
                                        Image(systemName: "clock")
                                            .font(.system(size: 13))
                                        Text(deadline)
                                            .font(.system(size: 13))
                                    }
                                    .foregroundColor(.secondary)
                                }
                            }
                            .padding(.horizontal, 4)

                            // Time Slots
                            ForEach(event.proposedSlots.indices, id: \.self) { index in
                                let slot = event.proposedSlots[index]
                                ModernTimeSlotVoteCard(
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
                    .padding(.top, 16)
                }
            }
        }
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
            if let participantVoteMap = poll.votes[participantId] as? [String: PollVote] {
                hasVoted = !participantVoteMap.isEmpty

                if hasVoted {
                    votes = participantVoteMap
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
                // Convert PollVote to Shared.Vote
                let sharedVote: Shared.Vote = {
                    switch pollVote {
                    case .yes: return .yes
                    case .maybe: return .maybe
                    case .no: return .no
                    }
                }()
                
                let result = try await repository.addVote(
                    eventId: event.id,
                    participantId: participantId,
                    slotId: slotId,
                    vote: sharedVote
                )

                if let success = result as? Bool, success {
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

    private func formatDeadlineShort(_ deadlineString: String) -> String? {
        if let date = ISO8601DateFormatter().date(from: deadlineString) {
            let formatter = DateFormatter()
            formatter.dateFormat = "MMM d"
            return "Due " + formatter.string(from: date)
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

// MARK: - Modern Time Slot Vote Card

struct ModernTimeSlotVoteCard: View {
    let timeSlot: TimeSlot
    let selectedVote: PollVote?
    let onVoteSelected: (PollVote) -> Void

    var body: some View {
        VStack(spacing: 16) {
            // Time slot header
            VStack(spacing: 6) {
                Text(formatDate(timeSlot.start ?? ""))
                    .font(.system(size: 17, weight: .semibold))
                    .foregroundColor(.primary)

                HStack(spacing: 6) {
                    Image(systemName: "clock")
                        .font(.system(size: 14))
                        .foregroundColor(.secondary)

                    Text("\(formatTime(timeSlot.start ?? "")) - \(formatTime(timeSlot.end ?? ""))")
                        .font(.system(size: 15))
                        .foregroundColor(.secondary)
                }
            }

            // Vote buttons
            HStack(spacing: 12) {
                ModernVoteButton(
                    vote: .yes,
                    icon: "checkmark",
                    label: "Available",
                    color: .green,
                    isSelected: selectedVote == .yes,
                    action: { onVoteSelected(.yes) }
                )

                ModernVoteButton(
                    vote: .maybe,
                    icon: "questionmark",
                    label: "Maybe",
                    color: .orange,
                    isSelected: selectedVote == .maybe,
                    action: { onVoteSelected(.maybe) }
                )

                ModernVoteButton(
                    vote: .no,
                    icon: "xmark",
                    label: "Not Available",
                    color: .red,
                    isSelected: selectedVote == .no,
                    action: { onVoteSelected(.no) }
                )
            }
        }
        .padding(20)
        .glassCard(cornerRadius: 20, material: .regularMaterial)
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

// MARK: - Modern Vote Button

struct ModernVoteButton: View {
    let vote: PollVote
    let icon: String
    let label: String
    let color: Color
    let isSelected: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            VStack(spacing: 8) {
                ZStack {
                    // Background circle
                    if isSelected {
                        Circle()
                            .fill(color)
                            .frame(width: 44, height: 44)
                    } else {
                        // Non-selected state with ultraThinMaterial
                        Circle()
                            .frame(width: 44, height: 44)
                            .background(.ultraThinMaterial)
                            .clipShape(Circle())
                    }

                    Image(systemName: icon)
                        .font(.system(size: 18, weight: .semibold))
                        .foregroundColor(isSelected ? .white : .secondary)
                }

                Text(label)
                    .font(.system(size: 11, weight: isSelected ? .semibold : .regular))
                    .foregroundColor(isSelected ? color : .secondary)
                    .lineLimit(1)
                    .minimumScaleFactor(0.8)
            }
            .frame(maxWidth: .infinity)
        }
        .buttonStyle(ScaleButtonStyle())
    }
}
