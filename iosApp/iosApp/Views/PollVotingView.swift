import SwiftUI
import Shared

struct PollVotingView: View {
    let event: Event
    let repository: EventRepository
    
    @State private var votes: [String: Vote] = [:] // slotId -> Vote
    @State private var hasVoted = false
    @State private var isError = false
    @State private var errorMessage = ""
    @State private var showError = false
    
    let participantId = "participant-1" // TODO: Get from auth
    
    var onVoteSubmitted: (String) -> Void
    
    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                Text("Vote on Time Slots")
                    .font(.system(size: 32, weight: .bold))
                
                Text("Event: \(event.title)")
                    .font(.caption)
                    .foregroundColor(.gray)
                
                // Deadline Info
                VStack(alignment: .leading, spacing: 4) {
                    Text("Voting Deadline")
                        .font(.caption)
                        .foregroundColor(.gray)
                    Text(event.deadline)
                        .font(.callout)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(12)
                .background(Color.gray.opacity(0.05))
                .cornerRadius(6)
                
                // Time Slots Voting
                Text("Proposed Time Slots")
                    .font(.system(size: 16, weight: .semibold))
                
                VStack(spacing: 12) {
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
                
                Spacer()
                
                // Error Display
                if showError {
                    Text(errorMessage)
                        .font(.caption)
                        .foregroundColor(.red)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(12)
                        .background(Color.red.opacity(0.1))
                        .cornerRadius(6)
                }
                
                // Submit Button
                Button(action: submitVotes) {
                    Text("Submit Votes")
                        .frame(maxWidth: .infinity)
                        .padding(12)
                }
                .buttonStyle(.borderedProminent)
                .disabled(votes.count != event.proposedSlots.count || hasVoted)
            }
            .padding(16)
        }
    }
    
    private func submitVotes() {
        if votes.count != event.proposedSlots.count {
            errorMessage = "Please vote on all time slots"
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
                errorMessage = "Failed to submit vote"
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
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                VStack(alignment: .leading, spacing: 4) {
                    Text("From: \(slot.start)")
                        .font(.caption)
                    Text("To: \(slot.end)")
                        .font(.caption)
                }
                
                Spacer()
                
                Text("UTC")
                    .font(.caption2)
                    .padding(.horizontal, 8)
                    .padding(.vertical, 4)
                    .background(Color.blue.opacity(0.1))
                    .cornerRadius(4)
            }
            
            // Vote Options
            HStack(spacing: 8) {
                VoteButton(
                    label: "Yes",
                    vote: Vote.YES,
                    isSelected: currentVote == Vote.YES,
                    onClick: { onVoteChange(Vote.YES) }
                )
                
                VoteButton(
                    label: "Maybe",
                    vote: Vote.MAYBE,
                    isSelected: currentVote == Vote.MAYBE,
                    onClick: { onVoteChange(Vote.MAYBE) }
                )
                
                VoteButton(
                    label: "No",
                    vote: Vote.NO,
                    isSelected: currentVote == Vote.NO,
                    onClick: { onVoteChange(Vote.NO) }
                )
            }
        }
        .padding(12)
        .background(Color.gray.opacity(0.05))
        .cornerRadius(6)
    }
}

struct VoteButton: View {
    let label: String
    let vote: Vote
    let isSelected: Bool
    let onClick: () -> Void
    
    var body: some View {
        Button(action: onClick) {
            Text(label)
                .font(.caption)
                .frame(maxWidth: .infinity)
                .padding(8)
        }
        .buttonStyle(
            isSelected ? .borderedProminent : .bordered
        )
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
        status: EventStatus.polling
    )
    
    PollVotingView(
        event: mockEvent,
        repository: mockRepository,
        onVoteSubmitted: { _ in }
    )
}
