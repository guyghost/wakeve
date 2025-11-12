import SwiftUI
import Shared

struct PollVotingView: View {
    let event: Event
    let repository: EventRepository
    
    @State private var votes: [String: Vote] = [:]
    @State private var hasVoted = false
    @State private var isError = false
    @State private var errorMessage = ""
    @State private var showError = false
    
    let participantId = "participant-1"
    
    var onVoteSubmitted: (String) -> Void
    
    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: LiquidGlassDesign.spacingL) {
                // Header
                VStack(alignment: .leading, spacing: LiquidGlassDesign.spacingS) {
                    Text("Voter sur les créneaux")
                        .font(LiquidGlassDesign.titleL)
                    Text("Événement: \(event.title)")
                        .font(LiquidGlassDesign.bodySmall)
                        .foregroundColor(.secondary)
                }
                .padding(.horizontal, LiquidGlassDesign.spacingL)
                
                VStack(spacing: LiquidGlassDesign.spacingL) {
                    // Deadline Info
                    HStack(spacing: LiquidGlassDesign.spacingM) {
                        Image(systemName: "calendar")
                            .font(.system(size: 18))
                            .foregroundColor(LiquidGlassDesign.accentBlue)
                        
                        VStack(alignment: .leading, spacing: LiquidGlassDesign.spacingXS) {
                            Text("Date limite du vote")
                                .font(LiquidGlassDesign.caption)
                                .foregroundColor(.secondary)
                            Text(event.deadline)
                                .font(LiquidGlassDesign.bodySmall)
                        }
                        
                        Spacer()
                    }
                    .liquidGlassCard()
                    
                    // Progress Indicator
                    HStack(spacing: LiquidGlassDesign.spacingS) {
                        Text("Progression: \(votes.count)/\(event.proposedSlots.count)")
                            .font(LiquidGlassDesign.bodySmall)
                            .foregroundColor(.secondary)
                        
                        Spacer()
                        
                        ProgressView(value: Double(votes.count), total: Double(event.proposedSlots.count))
                            .frame(width: 100)
                    }
                    .padding(.horizontal, LiquidGlassDesign.spacingL)
                    
                    // Time Slots Voting
                    VStack(alignment: .leading, spacing: LiquidGlassDesign.spacingS) {
                        Text("Créneaux proposés")
                            .font(LiquidGlassDesign.titleS)
                            .padding(.horizontal, LiquidGlassDesign.spacingL)
                        
                        VStack(spacing: LiquidGlassDesign.spacingM) {
                            ForEach(event.proposedSlots, id: \.id) { slot in
                                TimeSlotVoteCard(
                                    slot: slot,
                                    currentVote: votes[slot.id],
                                    onVoteChange: { vote in
                                        votes[slot.id] = vote
                                        showError = false
                                    }
                                )
                            }
                        }
                        .padding(.horizontal, LiquidGlassDesign.spacingL)
                    }
                    
                    // Error Display
                    if showError {
                        HStack(spacing: LiquidGlassDesign.spacingM) {
                            Image(systemName: "exclamationmark.circle.fill")
                                .foregroundColor(LiquidGlassDesign.errorRed)
                            
                            Text(errorMessage)
                                .font(LiquidGlassDesign.bodySmall)
                                .foregroundColor(LiquidGlassDesign.errorRed)
                            
                            Spacer()
                        }
                        .padding(LiquidGlassDesign.spacingM)
                        .background(
                            RoundedRectangle(cornerRadius: LiquidGlassDesign.radiusM)
                                .fill(LiquidGlassDesign.errorRed.opacity(0.1))
                        )
                        .padding(.horizontal, LiquidGlassDesign.spacingL)
                    }
                    
                    // Submit Button
                    Button(action: submitVotes) {
                        Text("Soumettre les votes")
                            .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(LiquidGlassButtonStyle(
                        isEnabled: votes.count == event.proposedSlots.count && !hasVoted
                    ))
                    .disabled(votes.count != event.proposedSlots.count || hasVoted)
                    .padding(.horizontal, LiquidGlassDesign.spacingL)
                    
                    Spacer()
                        .frame(height: LiquidGlassDesign.spacingL)
                }
            }
            .padding(.vertical, LiquidGlassDesign.spacingL)
        }
    }
    
    private func submitVotes() {
        if votes.count != event.proposedSlots.count {
            errorMessage = "Veuillez voter sur tous les créneaux"
            showError = true
            return
        }
        
        var allSuccess = true
        for (slotId, vote) in votes {
            let result = repository.addVote(
                eventId: event.id,
                participantId: participantId,
                timeSlotId: slotId,
                vote: vote
            )
            
            if !result.isSuccess {
                allSuccess = false
                errorMessage = "Erreur lors de la soumission des votes"
                showError = true
                break
            }
        }
        
        if allSuccess {
            hasVoted = true
            onVoteSubmitted(event.id)
        }
    }
}

struct TimeSlotVoteCard: View {
    let slot: TimeSlot
    let currentVote: Vote?
    let onVoteChange: (Vote) -> Void
    
    var body: some View {
        VStack(alignment: .leading, spacing: LiquidGlassDesign.spacingM) {
            HStack {
                VStack(alignment: .leading, spacing: LiquidGlassDesign.spacingXS) {
                    Text("De: \(slot.start)")
                        .font(LiquidGlassDesign.bodySmall)
                    Text("À: \(slot.end)")
                        .font(LiquidGlassDesign.bodySmall)
                }
                
                Spacer()
                
                Text(slot.timezone)
                    .font(LiquidGlassDesign.caption)
                    .foregroundColor(.secondary)
                    .padding(.horizontal, LiquidGlassDesign.spacingS)
                    .padding(.vertical, LiquidGlassDesign.spacingXS)
                    .background(LiquidGlassDesign.glassColor)
                    .cornerRadius(LiquidGlassDesign.radiusS)
            }
            
            // Vote Options
            HStack(spacing: LiquidGlassDesign.spacingS) {
                VoteButton(
                    label: "Oui",
                    vote: Vote.YES,
                    isSelected: currentVote == Vote.YES,
                    color: LiquidGlassDesign.successGreen,
                    onClick: { onVoteChange(Vote.YES) }
                )
                
                VoteButton(
                    label: "Peut-être",
                    vote: Vote.MAYBE,
                    isSelected: currentVote == Vote.MAYBE,
                    color: LiquidGlassDesign.warningOrange,
                    onClick: { onVoteChange(Vote.MAYBE) }
                )
                
                VoteButton(
                    label: "Non",
                    vote: Vote.NO,
                    isSelected: currentVote == Vote.NO,
                    color: LiquidGlassDesign.errorRed,
                    onClick: { onVoteChange(Vote.NO) }
                )
            }
        }
        .liquidGlassCard()
    }
}

struct VoteButton: View {
    let label: String
    let vote: Vote
    let isSelected: Bool
    let color: Color
    let onClick: () -> Void
    
    var body: some View {
        Button(action: onClick) {
            VStack(spacing: LiquidGlassDesign.spacingXS) {
                Image(systemName: voteIcon(vote))
                    .font(.system(size: 16, weight: .semibold))
                
                Text(label)
                    .font(LiquidGlassDesign.caption)
            }
            .frame(maxWidth: .infinity)
            .padding(LiquidGlassDesign.spacingM)
            .foregroundColor(isSelected ? .white : color)
            .background(
                RoundedRectangle(cornerRadius: LiquidGlassDesign.radiusM)
                    .fill(isSelected ? color : color.opacity(0.1))
            )
        }
    }
    
    private func voteIcon(_ vote: Vote) -> String {
        switch vote {
        case .YES:
            return "checkmark.circle"
        case .MAYBE:
            return "questionmark.circle"
        case .NO:
            return "xmark.circle"
        @unknown default:
            return "circle"
        }
    }
}

#Preview {
    let mockRepository = EventRepository()
    let mockEvent = Event(
        id: "event-1",
        title: "Team Outing",
        description: "Fun team gathering",
        organizerId: "org-1",
        participants: ["john@example.com"],
        proposedSlots: [
            TimeSlot(id: "slot-1", start: "2025-12-25T10:00:00Z", end: "2025-12-25T12:00:00Z", timezone: "UTC"),
            TimeSlot(id: "slot-2", start: "2025-12-26T14:00:00Z", end: "2025-12-26T16:00:00Z", timezone: "UTC")
        ],
        deadline: "2025-12-25T18:00:00Z",
        status: EventStatus.POLLING
    )
    
    PollVotingView(
        event: mockEvent,
        repository: mockRepository,
        onVoteSubmitted: { _ in }
    )
}
