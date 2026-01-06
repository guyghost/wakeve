import SwiftUI
import Shared

/// Modern poll results view inspired by Apple Invites
/// Features: Clean results visualization, progress indicators, clear winner highlighting
struct ModernPollResultsView: View {
    let event: Event
    let repository: EventRepositoryInterface
    let userId: String
    let onDateConfirmed: (String) -> Void
    let onBack: () -> Void

    @State private var poll: Poll?
    @State private var slotScores: [PollLogic.SlotScore] = []
    @State private var bestSlot: TimeSlot?
    @State private var isLoading = false
    @State private var errorMessage = ""
    @State private var showError = false
    @State private var showSuccess = false

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
                                .background(Color(.tertiarySystemFill))
                                .clipShape(Circle())
                        }

                        Spacer()
                    }
                    .padding(.horizontal, 20)
                    .padding(.top, 60)

                    VStack(spacing: 8) {
                        Text("Results")
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
                        if event.status == .confirmed, let finalDate = event.finalDate {
                            // Confirmed State
                            ConfirmedDateCard(
                                event: event,
                                finalSlot: event.proposedSlots.first { $0.id == finalDate }
                            )
                        } else {
                            // Polling State - Show Results
                            if !slotScores.isEmpty {
                                // Best Slot Highlight
                                if let best = bestSlot {
                                    BestSlotCard(slot: best)
                                }

                                // Results List
                                VStack(alignment: .leading, spacing: 12) {
                                    Text("All Options")
                                        .font(.system(size: 20, weight: .semibold))
                                        .foregroundColor(.primary)
                                        .padding(.horizontal, 4)

                                    ForEach(slotScores.sorted(by: { $0.totalScore > $1.totalScore }), id: \.slotId) { score in
                                        if let slot = event.proposedSlots.first(where: { $0.id == score.slotId }) {
                                            ModernSlotResultCard(
                                                slot: slot,
                                                score: score,
                                                isBest: score.slotId == bestSlot?.id
                                            )
                                        }
                                    }
                                }

                                // Confirm Button
                                if repository.isOrganizer(eventId: event.id, userId: userId), bestSlot != nil {
                                    Button {
                                        Task {
                                            await confirmDate()
                                        }
                                    } label: {
                                        HStack {
                                            if isLoading {
                                                ProgressView()
                                                    .progressViewStyle(CircularProgressViewStyle(tint: .white))
                                            } else {
                                                Text("Confirm This Date")
                                                    .font(.system(size: 17, weight: .semibold))
                                                    .foregroundColor(.white)
                                            }
                                        }
                                        .frame(maxWidth: .infinity)
                                        .frame(height: 50)
                                        .background(Color.blue)
                                        .continuousCornerRadius(14)
                                    }
                                    .disabled(isLoading)
                                    .padding(.top, 8)
                                }
                            } else {
                                // Empty State
                                VStack(spacing: 20) {
                                    Image(systemName: "chart.bar")
                                        .font(.system(size: 56))
                                        .foregroundColor(.secondary.opacity(0.5))
                                        .padding(.top, 40)

                                    VStack(spacing: 8) {
                                        Text("No Votes Yet")
                                            .font(.system(size: 24, weight: .bold))
                                            .foregroundColor(.primary)

                                        Text("Waiting for participants to vote")
                                            .font(.system(size: 17))
                                            .foregroundColor(.secondary)
                                            .multilineTextAlignment(.center)
                                    }
                                    .padding(.bottom, 40)
                                }
                                .frame(maxWidth: .infinity)
                                .glassCard(cornerRadius: 20, material: .regularMaterial)
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
            loadPollResults()
        }
        .alert("Error", isPresented: $showError) {
            Button("OK", role: .cancel) {}
        } message: {
            Text(errorMessage)
        }
        .alert("Success", isPresented: $showSuccess) {
            Button("OK", role: .cancel) {
                onDateConfirmed(event.id)
            }
        } message: {
            Text("Event date confirmed successfully!")
        }
    }

    private func loadPollResults() {
        poll = repository.getPoll(eventId: event.id)

        if let poll = poll {
            slotScores = PollLogic.shared.getSlotScores(poll: poll, slots: event.proposedSlots)

            if let bestResult = PollLogic.shared.getBestSlotWithScore(poll: poll, slots: event.proposedSlots) {
                bestSlot = bestResult.first
            }
        }
    }

    private func confirmDate() async {
        guard let bestSlot = bestSlot else { return }

        guard repository.isOrganizer(eventId: event.id, userId: userId) else {
            errorMessage = "Only the organizer can confirm the date"
            showError = true
            return
        }

        isLoading = true

        do {
            let result = try await repository.updateEventStatus(
                id: event.id,
                status: EventStatus.confirmed,
                finalDate: bestSlot.id
            )

            if let success = result as? Bool, success {
                isLoading = false
                showSuccess = true
            } else {
                isLoading = false
                errorMessage = "Failed to confirm date"
                showError = true
            }
        } catch {
            isLoading = false
            errorMessage = error.localizedDescription
            showError = true
        }
    }
}

// MARK: - Best Slot Card

// MARK: - Modern Slot Result Card

struct ModernSlotResultCard: View {
    let slot: TimeSlot
    let score: PollLogic.SlotScore
    let isBest: Bool

    var body: some View {
        VStack(spacing: 16) {
            // Date and Time
            VStack(spacing: 6) {
                Text(formatDate(slot.start ?? ""))
                    .font(.system(size: 17, weight: .semibold))
                    .foregroundColor(.primary)

                HStack(spacing: 6) {
                    Image(systemName: "clock")
                        .font(.system(size: 14))
                        .foregroundColor(.secondary)

                    Text("\(formatTime(slot.start ?? "")) - \(formatTime(slot.end ?? ""))")
                        .font(.system(size: 15))
                        .foregroundColor(.secondary)
                }
            }

            // Vote Breakdown
            HStack(spacing: 16) {
                VoteCountBadge(
                    label: "Available",
                    count: Int(score.yesCount),
                    color: .green
                )

                VoteCountBadge(
                    label: "Maybe",
                    count: Int(score.maybeCount),
                    color: .orange
                )

                VoteCountBadge(
                    label: "No",
                    count: Int(score.noCount),
                    color: .red
                )

                Spacer()

                // Total Score
                VStack(spacing: 4) {
                    Text("Score")
                        .font(.system(size: 11, weight: .medium))
                        .foregroundColor(.secondary)
                        .textCase(.uppercase)

                    Text("\(score.totalScore)")
                        .font(.system(size: 24, weight: .bold))
                        .foregroundColor(isBest ? .blue : .primary)
                }
            }

            // Best Indicator
            if isBest {
                HStack {
                    Image(systemName: "star.fill")
                        .font(.system(size: 12))
                        .foregroundColor(.yellow)

                    Text("Most Popular")
                        .font(.system(size: 13, weight: .medium))
                        .foregroundColor(.primary)

                    Spacer()
                }
                .padding(.horizontal, 12)
                .padding(.vertical, 6)
                .background(Color.yellow.opacity(0.15))
                .continuousCornerRadius(8)
            }
        }
        .padding(16)
        .glassCard(cornerRadius: 20, material: .regularMaterial)
        .overlay(
            RoundedRectangle(cornerRadius: 20, style: .continuous)
                .stroke(isBest ? Color.blue.opacity(0.3) : Color.clear, lineWidth: 2)
        )
    }

    private func formatDate(_ dateString: String) -> String {
        if let date = ISO8601DateFormatter().date(from: dateString) {
            let formatter = DateFormatter()
            formatter.dateFormat = "EEE, MMM d"
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

// MARK: - Vote Count Badge

struct VoteCountBadge: View {
    let label: String
    let count: Int
    let color: Color

    var body: some View {
        VStack(spacing: 4) {
            Text("\(count)")
                .font(.system(size: 18, weight: .semibold))
                .foregroundColor(color)

            Text(label)
                .font(.system(size: 10, weight: .medium))
                .foregroundColor(.secondary)
                .textCase(.uppercase)
        }
    }
}
