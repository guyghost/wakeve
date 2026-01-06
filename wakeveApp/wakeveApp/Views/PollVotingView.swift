import SwiftUI
import Shared

/// Poll voting screen - matches Android's PollVotingScreen
/// Features: Time slot voting, submit votes, validation
struct PollVotingView: View {
    let event: Event
    let participantId: String
    let repository: EventRepositoryInterface
    let onVoteSubmitted: () -> Void
    let onBack: () -> Void

    @State private var votes: [String: Vote] = [:]
    @State private var hasVoted = false
    @State private var isLoading = false
    @State private var showError = false
    @State private var errorMessage = ""

    var body: some View {
        NavigationStack {
            ZStack {
                Color(.systemBackground)
                    .ignoresSafeArea()

                ScrollView {
                    VStack(spacing: 20) {
                        // Header
                        VStack(spacing: 8) {
                            Text("Votez pour le créneau")
                                .font(.largeTitle.weight(.bold))
                                .foregroundColor(.primary)
                                .multilineTextAlignment(.center)

                            Text(event.title)
                                .font(.body)
                                .foregroundColor(.secondary)
                                .multilineTextAlignment(.center)
                        }
                        .padding(.top, 20)

                        // Deadline Info Card
                        LiquidGlassCard(cornerRadius: 16, padding: 16) {
                            VStack(alignment: .leading, spacing: 8) {
                                HStack(spacing: 8) {
                                    Image(systemName: "clock.badge.exclamationmark.fill")
                                        .font(.system(size: 16, weight: .medium))
                                        .foregroundColor(.wakevWarning)

                                    Text("Date limite de vote")
                                        .font(.subheadline.weight(.medium))
                                        .foregroundColor(.primary)
                                }

                                Text(formatDeadline(event.deadline))
                                    .font(.body)
                                    .foregroundColor(.secondary)
                            }
                        }

                        // Time Slots
                        Text("Créneaux proposés")
                            .font(.title2.weight(.semibold))
                            .foregroundColor(.primary)
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .padding(.top, 10)

                        if event.proposedSlots.isEmpty {
                            // Empty state
                            VStack(spacing: 16) {
                                Image(systemName: "calendar.badge.exclamationmark")
                                    .font(.system(size: 48))
                                    .foregroundColor(.secondary)

                                Text("Aucun créneau proposé")
                                    .font(.body)
                                    .foregroundColor(.secondary)
                            }
                            .padding(.vertical, 40)
                        } else {
                            // Time slot cards
                            VStack(spacing: 12) {
                                ForEach(event.proposedSlots, id: \.id) { slot in
                                    TimeSlotVoteCard(
                                        slot: slot,
                                        currentVote: votes[slot.id],
                                        onVoteChange: { vote in
                                            votes[slot.id] = vote
                                        }
                                    )
                                }
                            }
                        }

                        // Error message
                        if showError {
                            HStack(spacing: 8) {
                                Image(systemName: "exclamationmark.triangle.fill")
                                    .foregroundColor(.red)

                                Text(errorMessage)
                                    .font(.body)
                                    .foregroundColor(.red)
                            }
                            .padding()
                            .background(Color.red.opacity(0.1))
                            .cornerRadius(12)
                        }

                        // Submit Button
                        Button(action: submitVotes) {
                            HStack(spacing: 8) {
                                if isLoading {
                                    ProgressView()
                                        .progressViewStyle(CircularProgressViewStyle(tint: .white))
                                } else {
                                    Image(systemName: "checkmark.circle.fill")
                                        .font(.system(size: 20, weight: .semibold))
                                }

                                Text(hasVoted ? "Vote envoyé" : "Envoyer mon vote")
                                    .font(.headline.weight(.semibold))
                            }
                            .foregroundColor(.white)
                            .frame(maxWidth: .infinity)
                            .frame(height: 54)
                            .background(
                                LinearGradient(
                                    gradient: Gradient(colors: [
                                        Color.wakevPrimary,
                                        Color.wakevAccent
                                    ]),
                                    startPoint: .topLeading,
                                    endPoint: .bottomTrailing
                                )
                            )
                            .cornerRadius(14)
                        }
                        .disabled(
                            isLoading ||
                            hasVoted ||
                            votes.count != event.proposedSlots.count
                        )
                        .padding(.horizontal, 20)
                        .padding(.top, 10)
                        .padding(.bottom, 40)
                    }
                    .padding(.horizontal, 16)
                }
            }
            .navigationTitle("Sondage")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button(action: onBack) {
                        Image(systemName: "chevron.left")
                            .font(.system(size: 20, weight: .semibold))
                            .foregroundColor(.primary)
                    }
                    .accessibilityLabel("Retour")
                }
            }
        }
    }

    // MARK: - Helper Methods

    private func formatDeadline(_ dateString: String) -> String {
        if let date = ISO8601DateFormatter().date(from: dateString) {
            let formatter = DateFormatter()
            formatter.dateStyle = .medium
            formatter.timeStyle = .short
            return formatter.string(from: date)
        }
        return dateString
    }

    private func submitVotes() {
        // Validate all slots have votes
        if votes.count != event.proposedSlots.count {
            showError = true
            errorMessage = "Veuillez voter pour tous les créneaux"
            return
        }

        isLoading = true
        showError = false

        // Submit votes
        Task {
            do {
                for (slotId, vote) in votes {
                    _ = try await repository.addVote(
                        eventId: event.id,
                        participantId: participantId,
                        slotId: slotId,
                        vote: vote
                    )
                }

                // Success
                await MainActor.run {
                    isLoading = false
                    hasVoted = true
                    onVoteSubmitted()

                    // Haptic feedback
                    let generator = UINotificationFeedbackGenerator()
                    generator.notificationOccurred(.success)
                }
            } catch {
                await MainActor.run {
                    isLoading = false
                    showError = true
                    errorMessage = "Erreur lors de l'envoi du vote: \(error.localizedDescription)"

                    // Haptic feedback
                    let generator = UINotificationFeedbackGenerator()
                    generator.notificationOccurred(.error)
                }
            }
        }
    }
}

// MARK: - Time Slot Vote Card

/// Card displaying a time slot with voting options
struct TimeSlotVoteCard: View {
    let slot: TimeSlot
    let currentVote: Vote?
    let onVoteChange: (Vote) -> Void

    var body: some View {
        LiquidGlassCard(cornerRadius: 16, padding: 16) {
            VStack(spacing: 16) {
                // Slot info
                VStack(alignment: .leading, spacing: 4) {
                    HStack(spacing: 6) {
                        Image(systemName: slotIcon)
                            .font(.system(size: 14, weight: .medium))
                            .foregroundColor(.wakevPrimary)

                        Text(timeOfDayText)
                            .font(.subheadline.weight(.medium))
                            .foregroundColor(.primary)
                    }

                    if let start = slot.start, let end = slot.end {
                        Text("\(formatDate(start)) - \(formatDate(end))")
                            .font(.body)
                            .foregroundColor(.secondary)
                    }

                    Text(slot.timezone)
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
                .frame(maxWidth: .infinity, alignment: .leading)

                // Vote buttons
                HStack(spacing: 12) {
                    PollVoteButton(
                        label: "Oui",
                        icon: "checkmark.circle.fill",
                        vote: .yes,
                        isSelected: currentVote == .yes,
                        onTap: { onVoteChange(.yes) },
                        color: .wakevSuccess
                    )

                    PollVoteButton(
                        label: "Peut-être",
                        icon: "questionmark.circle.fill",
                        vote: .maybe,
                        isSelected: currentVote == .maybe,
                        onTap: { onVoteChange(.maybe) },
                        color: .wakevWarning
                    )

                    PollVoteButton(
                        label: "Non",
                        icon: "xmark.circle.fill",
                        vote: .no,
                        isSelected: currentVote == .no,
                        onTap: { onVoteChange(.no) },
                        color: .wakevError
                    )
                }
            }
        }
    }

    private var timeOfDayText: String {
        switch slot.timeOfDay {
        case Shared.TimeOfDay.allDay: return "Toute la journée"
        case Shared.TimeOfDay.morning: return "Matin"
        case Shared.TimeOfDay.afternoon: return "Après-midi"
        case Shared.TimeOfDay.evening: return "Soir"
        case Shared.TimeOfDay.specific: return "Horaire spécifique"
        default: return "Créneau"
        }
    }

    private var slotIcon: String {
        switch slot.timeOfDay {
        case Shared.TimeOfDay.allDay: return "sun.max.fill"
        case Shared.TimeOfDay.morning: return "sunrise.fill"
        case Shared.TimeOfDay.afternoon: return "sun.max.fill"
        case Shared.TimeOfDay.evening: return "moon.fill"
        case Shared.TimeOfDay.specific: return "clock.fill"
        default: return "calendar.badge.clock"
        }
    }

    private func formatDate(_ isoString: String) -> String {
        let formatter = ISO8601DateFormatter()
        guard let date = formatter.date(from: isoString) else { return isoString }

        let displayFormatter = DateFormatter()
        displayFormatter.dateStyle = .short
        displayFormatter.timeStyle = .short
        return displayFormatter.string(from: date)
    }
}

// MARK: - Poll Vote Button

/// Button for voting Yes/Maybe/No on a time slot
private struct PollVoteButton: View {
    let label: String
    let icon: String
    let vote: Vote
    let isSelected: Bool
    let onTap: () -> Void
    let color: Color

    var body: some View {
        Button(action: onTap) {
            HStack(spacing: 8) {
                Image(systemName: icon)
                    .font(.system(size: 16, weight: .semibold))

                Text(label)
                    .font(.subheadline.weight(.medium))
            }
            .foregroundColor(isSelected ? .white : color)
            .padding(.horizontal, 16)
            .padding(.vertical, 12)
            .frame(maxWidth: .infinity)
            .background(
                RoundedRectangle(cornerRadius: 10, style: .continuous)
                    .fill(isSelected ? color : color.opacity(0.1))
            )
            .overlay(
                RoundedRectangle(cornerRadius: 10, style: .continuous)
                    .stroke(color, lineWidth: isSelected ? 0 : 1)
            )
        }
        .accessibilityLabel("\(label) pour ce créneau")
        .accessibilityAddTraits(isSelected ? .isSelected : [])
    }
}

// MARK: - Preview
// Preview commented out due to API changes in shared module
// TODO: Update preview when Event model is stabilized
