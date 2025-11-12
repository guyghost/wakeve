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
            VStack(alignment: .leading, spacing: LiquidGlassDesign.spacingL) {
                // Header
                VStack(alignment: .leading, spacing: LiquidGlassDesign.spacingS) {
                    Text("Résultats du vote")
                        .font(LiquidGlassDesign.titleL)
                    Text("Événement: \(event.title)")
                        .font(LiquidGlassDesign.bodySmall)
                        .foregroundColor(.secondary)
                }
                .padding(.horizontal, LiquidGlassDesign.spacingL)
                
                VStack(spacing: LiquidGlassDesign.spacingL) {
                    // Best Slot Recommendation
                    if let (bestSlot, score) = bestResult {
                        VStack(alignment: .leading, spacing: LiquidGlassDesign.spacingM) {
                            HStack(spacing: LiquidGlassDesign.spacingS) {
                                Image(systemName: "star.fill")
                                    .font(.system(size: 18))
                                    .foregroundColor(LiquidGlassDesign.warningOrange)
                                
                                Text("Créneau recommandé")
                                    .font(LiquidGlassDesign.titleS)
                            }
                            
                            VStack(alignment: .leading, spacing: LiquidGlassDesign.spacingXS) {
                                Text(bestSlot.start)
                                    .font(LiquidGlassDesign.bodySmall)
                                Text("à")
                                    .font(LiquidGlassDesign.caption)
                                    .foregroundColor(.secondary)
                                Text(bestSlot.end)
                                    .font(LiquidGlassDesign.bodySmall)
                            }
                            
                            // Score Breakdown
                            HStack(spacing: LiquidGlassDesign.spacingL) {
                                ScoreIndicator(
                                    label: "Oui",
                                    count: score.yesCount,
                                    color: LiquidGlassDesign.successGreen
                                )
                                
                                ScoreIndicator(
                                    label: "Peut-être",
                                    count: score.maybeCount,
                                    color: LiquidGlassDesign.warningOrange
                                )
                                
                                ScoreIndicator(
                                    label: "Non",
                                    count: score.noCount,
                                    color: LiquidGlassDesign.errorRed
                                )
                                
                                Spacer()
                                
                                VStack(alignment: .trailing, spacing: LiquidGlassDesign.spacingXS) {
                                    Text("Score")
                                        .font(LiquidGlassDesign.caption)
                                        .foregroundColor(.secondary)
                                    Text("\(Int(score.totalScore))")
                                        .font(LiquidGlassDesign.titleS)
                                        .foregroundColor(score.totalScore > 0 ? LiquidGlassDesign.successGreen : LiquidGlassDesign.errorRed)
                                }
                            }
                        }
                        .liquidGlassCard()
                    }
                    
                    // All Scores
                    VStack(alignment: .leading, spacing: LiquidGlassDesign.spacingM) {
                        Text("Tous les créneaux")
                            .font(LiquidGlassDesign.titleS)
                            .padding(.horizontal, LiquidGlassDesign.spacingL)
                        
                        VStack(spacing: LiquidGlassDesign.spacingS) {
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
                        .padding(.horizontal, LiquidGlassDesign.spacingL)
                    }
                    
                    // Confirmation Section
                    if !isConfirmed && selectedSlotId != nil {
                        VStack(alignment: .leading, spacing: LiquidGlassDesign.spacingS) {
                            HStack(spacing: LiquidGlassDesign.spacingS) {
                                Image(systemName: "checkmark.circle.fill")
                                    .font(.system(size: 18))
                                    .foregroundColor(LiquidGlassDesign.successGreen)
                                
                                Text("Confirmer le créneau?")
                                    .font(LiquidGlassDesign.titleS)
                            }
                            
                            Text("Une fois confirmé, les participants seront notifiés et l'événement sera finalisé.")
                                .font(LiquidGlassDesign.bodySmall)
                                .foregroundColor(.secondary)
                        }
                        .liquidGlassCard()
                    }
                    
                    // Action Button
                    Button(action: confirmDate) {
                        Text("Confirmer la date finale")
                            .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(LiquidGlassButtonStyle(
                        isEnabled: selectedSlotId != nil && !isConfirmed
                    ))
                    .disabled(selectedSlotId == nil || isConfirmed)
                    .padding(.horizontal, LiquidGlassDesign.spacingL)
                    
                    Spacer()
                        .frame(height: LiquidGlassDesign.spacingL)
                }
            }
            .padding(.vertical, LiquidGlassDesign.spacingL)
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
        
        if repository.isOrganizer(eventId: event.id, participantId: "organizer-1") {
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
        VStack(alignment: .center, spacing: LiquidGlassDesign.spacingXS) {
            Circle()
                .fill(color)
                .frame(width: 44, height: 44)
                .overlay {
                    Text("\(count)")
                        .font(LiquidGlassDesign.titleS)
                        .foregroundColor(.white)
                }
            
            Text(label)
                .font(LiquidGlassDesign.caption)
                .foregroundColor(.secondary)
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
            HStack(spacing: LiquidGlassDesign.spacingM) {
                VStack(alignment: .leading, spacing: LiquidGlassDesign.spacingXS) {
                    Text("De: \(slot.start)")
                        .font(LiquidGlassDesign.bodySmall)
                    Text("À: \(slot.end)")
                        .font(LiquidGlassDesign.bodySmall)
                }
                
                Spacer()
                
                VStack(alignment: .trailing, spacing: LiquidGlassDesign.spacingXS) {
                    Text("Score")
                        .font(LiquidGlassDesign.caption)
                        .foregroundColor(.secondary)
                    
                    Text("\(Int(score.totalScore))")
                        .font(LiquidGlassDesign.titleS)
                        .foregroundColor(
                            score.totalScore > 0
                                ? LiquidGlassDesign.successGreen
                                : LiquidGlassDesign.errorRed
                        )
                }
            }
            .liquidGlassCard(cornerRadius: LiquidGlassDesign.radiusL)
            .overlay(
                isSelected ? RoundedRectangle(cornerRadius: LiquidGlassDesign.radiusL)
                    .stroke(LiquidGlassDesign.accentBlue, lineWidth: 2)
                    : nil
            )
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
        status: EventStatus.CONFIRMED
    )
    
    PollResultsView(
        event: mockEvent,
        repository: mockRepository,
        onDateConfirmed: { _ in }
    )
}
