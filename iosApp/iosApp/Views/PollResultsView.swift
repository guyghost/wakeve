import SwiftUI
import Shared

/// PollResultsView - Displays poll results with Liquid Glass design
/// 
/// Features:
/// - Clean results visualization with Liquid Glass cards
/// - Progress indicators and clear winner highlighting
/// - Accessibility support with proper labels and hints
struct PollResultsView: View {
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
                headerView
                
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
                            pollResultsContent
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
    
    // MARK: - Header View
    
    private var headerView: some View {
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
                .accessibilityLabel("Close poll results")
                
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
    }
    
    // MARK: - Poll Results Content
    
    @ViewBuilder
    private var pollResultsContent: some View {
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
                        SlotResultCard(
                            slot: slot,
                            score: score,
                            isBest: score.slotId == bestSlot?.id
                        )
                    }
                }
            }
            
            // Confirm Button
            if repository.isOrganizer(eventId: event.id, userId: userId), bestSlot != nil {
                confirmButton
            }
        } else {
            // Empty State
            emptyStateView
        }
    }
    
    // MARK: - Confirm Button
    
    private var confirmButton: some View {
        LiquidGlassButton(
            title: isLoading ? "" : "Confirm This Date",
            style: .primary
        ) {
            Task {
                await confirmDate()
            }
        }
        .disabled(isLoading)
        .overlay {
            if isLoading {
                ProgressView()
                    .progressViewStyle(CircularProgressViewStyle(tint: .white))
            }
        }
        .padding(.top, 8)
    }
    
    // MARK: - Empty State
    
    private var emptyStateView: some View {
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
        .liquidGlass(cornerRadius: 20, opacity: 0.9, intensity: 1.0)
    }
    
    // MARK: - Logic
    
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

struct BestSlotCard: View {
    let slot: TimeSlot
    
    var body: some View {
        VStack(spacing: 16) {
            HStack {
                Image(systemName: "star.fill")
                    .font(.system(size: 20))
                    .foregroundColor(.wakevWarning)
                
                Text("Most Popular Time")
                    .font(.system(size: 17, weight: .semibold))
                    .foregroundColor(.primary)
                
                Spacer()
            }
            
            VStack(spacing: 8) {
                Text(formatDate(slot.start ?? ""))
                    .font(.system(size: 24, weight: .bold))
                    .foregroundColor(.primary)
                
                HStack(spacing: 6) {
                    Image(systemName: "clock")
                        .font(.system(size: 15))
                        .foregroundColor(.secondary)
                    
                    Text("\(formatTime(slot.start ?? "")) - \(formatTime(slot.end ?? ""))")
                        .font(.system(size: 17))
                        .foregroundColor(.secondary)
                }
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 12)
            .liquidGlass(cornerRadius: 12, opacity: 0.7, intensity: 0.8)
        }
        .padding(20)
        .liquidGlass(cornerRadius: 20, opacity: 0.9, intensity: 1.0)
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

// MARK: - Confirmed Date Card

struct ConfirmedDateCard: View {
    let event: Event
    let finalSlot: TimeSlot?
    
    var body: some View {
        VStack(spacing: 24) {
            // Success Icon
            ZStack {
                Circle()
                    .fill(Color.wakevSuccess.opacity(0.1))
                    .frame(width: 80, height: 80)
                
                Image(systemName: "checkmark")
                    .font(.system(size: 36, weight: .semibold))
                    .foregroundColor(.wakevSuccess)
            }
            .padding(.top, 20)
            
            // Title
            VStack(spacing: 8) {
                Text("Date Confirmed")
                    .font(.system(size: 28, weight: .bold))
                    .foregroundColor(.primary)
                
                Text("The event is all set!")
                    .font(.system(size: 17))
                    .foregroundColor(.secondary)
            }
            
            // Final Date Details
            if let slot = finalSlot {
                VStack(spacing: 12) {
                    LiquidGlassDivider(style: .subtle)
                    
                    VStack(spacing: 8) {
                        Text(formatDate(slot.start ?? ""))
                            .font(.system(size: 22, weight: .semibold))
                            .foregroundColor(.primary)
                        
                        HStack(spacing: 6) {
                            Image(systemName: "clock")
                                .font(.system(size: 15))
                                .foregroundColor(.secondary)
                            
                            Text("\(formatTime(slot.start ?? "")) - \(formatTime(slot.end ?? ""))")
                                .font(.system(size: 17))
                                .foregroundColor(.secondary)
                        }
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 16)
                    .liquidGlass(cornerRadius: 12, opacity: 0.7, intensity: 0.8)
                    
                    LiquidGlassDivider(style: .subtle)
                    
                    HStack(spacing: 8) {
                        Image(systemName: "bell.fill")
                            .font(.system(size: 14))
                            .foregroundColor(.secondary)
                        
                        Text("All participants have been notified")
                            .font(.system(size: 15))
                            .foregroundColor(.secondary)
                    }
                }
            }
        }
        .padding(24)
        .liquidGlass(cornerRadius: 20, opacity: 0.9, intensity: 1.0)
    }
    
    private func formatDate(_ dateString: String) -> String {
        if let date = ISO8601DateFormatter().date(from: dateString) {
            let formatter = DateFormatter()
            formatter.dateFormat = "EEEE, MMMM d"
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

// MARK: - Slot Result Card

struct SlotResultCard: View {
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
            
            // Vote Breakdown with Liquid Glass Badges
            HStack(spacing: 16) {
                VoteBadge(
                    label: "Available",
                    count: Int(score.yesCount),
                    color: .wakevSuccess
                )
                
                VoteBadge(
                    label: "Maybe",
                    count: Int(score.maybeCount),
                    color: .wakevWarning
                )
                
                VoteBadge(
                    label: "No",
                    count: Int(score.noCount),
                    color: .wakevError
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
                        .foregroundColor(isBest ? .wakevPrimary : .primary)
                }
            }
            
            // Best Indicator
            if isBest {
                HStack {
                    Image(systemName: "star.fill")
                        .font(.system(size: 12))
                        .foregroundColor(.wakevWarning)
                    
                    Text("Most Popular")
                        .font(.system(size: 13, weight: .medium))
                        .foregroundColor(.primary)
                    
                    Spacer()
                }
                .padding(.horizontal, 12)
                .padding(.vertical, 6)
                .background(Color.wakevWarning.opacity(0.15))
                .liquidGlass(cornerRadius: 8, opacity: 0.5, intensity: 0.5)
            }
        }
        .padding(16)
        .liquidGlass(cornerRadius: 20, opacity: 0.9, intensity: 1.0)
        .overlay(
            RoundedRectangle(cornerRadius: 20, style: .continuous)
                .stroke(isBest ? Color.wakevPrimary.opacity(0.3) : Color.clear, lineWidth: 2)
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

// MARK: - Vote Badge

struct VoteBadge: View {
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

// MARK: - Preview

#Preview("Poll Results View") {
    PollResultsView(
        event: Event(
            id: "preview-event-1",
            title: "Team Building Event",
            description: "Annual team building",
            organizerId: "user-1",
            status: .polling,
            proposedSlots: [
                TimeSlot(id: "slot-1", start: ISO8601DateFormatter().date(from: "2025-01-15T09:00:00Z")?.ISO8601String() ?? "", end: ISO8601DateFormatter().date(from: "2025-01-15T12:00:00Z")?.ISO8601String() ?? ""),
                TimeSlot(id: "slot-2", start: ISO8601DateFormatter().date(from: "2025-01-16T14:00:00Z")?.ISO8601String() ?? "", end: ISO8601DateFormatter().date(from: "2025-01-16T17:00:00Z")?.ISO8601String() ?? "")
            ],
            minParticipants: 5,
            maxParticipants: 20,
            expectedParticipants: 12
        ),
        repository: MockEventRepository(),
        userId: "user-1",
        onDateConfirmed: { _ in print("Date confirmed") },
        onBack: { print("Back") }
    )
}
