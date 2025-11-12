import SwiftUI
import Shared

struct PollResultsView: View {
    let event: Event
    let repository: EventRepository
    
    @State private var selectedSlotId: String?
    @State private var isConfirmed = false
    
    var onDateConfirmed: (String) -> Void
    
    var body: some View {
        let poll = repository.getPoll(eventId: event.id)
        let scores = poll != nil ? PollLogic.getSlotScores(poll!, slots: event.proposedSlots) : []
        let bestResult = poll != nil ? PollLogic.getBestSlotWithScore(poll!, slots: event.proposedSlots) : nil
        
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                Text("Poll Results")
                    .font(.system(size: 32, weight: .bold))
                
                Text("Event: \(event.title)")
                    .font(.caption)
                    .foregroundColor(.gray)
                
                // Best Slot Recommendation
                if let (bestSlot, score) = bestResult {
                    VStack(alignment: .leading, spacing: 12) {
                        Text("Recommended Time Slot")
                            .font(.system(size: 14, weight: .semibold))
                        
                        VStack(alignment: .leading, spacing: 4) {
                            Text(bestSlot.start)
                                .font(.callout)
                            Text("to")
                                .font(.caption)
                                .foregroundColor(.gray)
                            Text(bestSlot.end)
                                .font(.callout)
                        }
                        
                        // Score Breakdown
                        HStack(spacing: 16) {
                            ScoreIndicator(
                                label: "Yes",
                                count: score.yesCount,
                                color: Color(red: 0.3, green: 0.8, blue: 0.3)
                            )
                            
                            ScoreIndicator(
                                label: "Maybe",
                                count: score.maybeCount,
                                color: Color(red: 1, green: 0.8, blue: 0.2)
                            )
                            
                            ScoreIndicator(
                                label: "No",
                                count: score.noCount,
                                color: Color(red: 1, green: 0.3, blue: 0.3)
                            )
                        }
                        
                        Text("Score: \(Int(score.totalScore))")
                            .font(.callout)
                            .padding(.top, 8)
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(16)
                    .background(Color.blue.opacity(0.1))
                    .cornerRadius(8)
                }
                
                // All Scores
                Text("All Time Slots")
                    .font(.system(size: 16, weight: .semibold))
                
                VStack(spacing: 12) {
                    ForEach(scores, id: \.slotId) { score in
                        if let slot = event.proposedSlots.first(where: { $0.id == score.slotId }) {
                            SlotResultCard(
                                slot: slot,
                                score: score,
                                isSelected: selectedSlotId == slot.id,
                                onSelect: { selectedSlotId = slot.id }
                            )
                        }
                    }
                }
                
                Spacer()
                
                // Confirmation Section
                if !isConfirmed && selectedSlotId != nil {
                    VStack(alignment: .leading, spacing: 8) {
                        Text("Confirm selected time slot?")
                            .font(.callout)
                        Text("Once confirmed, participants will be notified and the event will be finalized.")
                            .font(.caption)
                            .foregroundColor(.gray)
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(16)
                    .background(Color.gray.opacity(0.05))
                    .cornerRadius(8)
                }
                
                // Action Button
                Button(action: confirmDate) {
                    Text("Confirm Final Date")
                        .frame(maxWidth: .infinity)
                        .padding(12)
                }
                .buttonStyle(.borderedProminent)
                .disabled(selectedSlotId == nil || isConfirmed)
            }
            .padding(16)
        }
        .onAppear {
            if let bestResult = bestResult {
                selectedSlotId = bestResult.0.id
            }
        }
    }
    
    private func confirmDate() {
        guard let selectedSlotId = selectedSlotId else { return }
        guard let selectedSlot = event.proposedSlots.first(where: { $0.id == selectedSlotId }) else { return }
        
        // Check if user is organizer
        if repository.isOrganizer(eventId: event.id, participantId: "organizer-1") { // TODO: Get from auth
            repository.updateEventStatus(
                eventId: event.id,
                status: EventStatus.CONFIRMED,
                confirmedDate: selectedSlot.start
            )
            isConfirmed = true
            onDateConfirmed(event.id)
        }
    }
}

struct ScoreIndicator: View {
    let label: String
    let count: Int
    let color: Color
    
    var body: some View {
        VStack(alignment: .center, spacing: 4) {
            Circle()
                .fill(color)
                .frame(width: 40, height: 40)
                .overlay {
                    Text("\(count)")
                        .font(.caption)
                        .foregroundColor(.white)
                }
            
            Text(label)
                .font(.caption2)
        }
    }
}

struct SlotResultCard: View {
    let slot: TimeSlot
    let score: PollLogic.SlotScore
    let isSelected: Bool
    let onSelect: () -> Void
    
    var body: some View {
        Button(action: onSelect) {
            HStack {
                VStack(alignment: .leading, spacing: 4) {
                    Text(slot.start)
                        .font(.caption)
                    Text("→")
                        .font(.caption)
                    Text(slot.end)
                        .font(.caption)
                }
                
                Spacer()
                
                Text("Score: \(Int(score.totalScore))")
                    .font(.caption)
                    .foregroundColor(score.totalScore > 0 ? Color(red: 0.3, green: 0.8, blue: 0.3) : Color(red: 1, green: 0.3, blue: 0.3))
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(12)
            .background(Color.gray.opacity(0.05))
            .overlay {
                if isSelected {
                    RoundedRectangle(cornerRadius: 6)
                        .stroke(Color.blue, lineWidth: 2)
                }
            }
            .cornerRadius(6)
        }
        .buttonStyle(.plain)
    }
}

#Preview {
    let mockRepository = EventRepository()
    let mockEvent = Event(
        id: "event-1",
        title: "Team Outing",
        description: "Fun team gathering",
        organizerId: "org-1",
        participants: ["john@example.com", "jane@example.com"],
        proposedSlots: [
            TimeSlot(id: "slot-1", start: "2025-12-25T10:00:00Z", end: "2025-12-25T12:00:00Z", timezone: "UTC"),
            TimeSlot(id: "slot-2", start: "2025-12-26T14:00:00Z", end: "2025-12-26T16:00:00Z", timezone: "UTC")
        ],
        deadline: "2025-12-25T18:00:00Z",
        status: EventStatus.polling
    )
    
    PollResultsView(
        event: mockEvent,
        repository: mockRepository,
        onDateConfirmed: { _ in }
    )
}
