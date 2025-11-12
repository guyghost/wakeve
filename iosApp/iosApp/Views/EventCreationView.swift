import SwiftUI
import Shared

struct EventCreationView: View {
    @State private var title = ""
    @State private var description = ""
    @State private var deadline = ""
    @State private var slots: [TimeSlot] = []
    @State private var slotStart = ""
    @State private var slotEnd = ""
    @State private var isError = false
    @State private var errorMessage = ""
    @State private var showError = false
    
    var onEventCreated: (Event) -> Void
    var onNavigateToParticipants: (String) -> Void = { _ in }
    
    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: LiquidGlassDesign.spacingL) {
                // Header
                VStack(alignment: .leading, spacing: LiquidGlassDesign.spacingS) {
                    Text("Créer un événement")
                        .font(LiquidGlassDesign.titleL)
                    Text("Définissez les détails et les créneaux disponibles")
                        .font(LiquidGlassDesign.bodySmall)
                        .foregroundColor(.secondary)
                }
                .padding(.horizontal, LiquidGlassDesign.spacingL)
                
                VStack(spacing: LiquidGlassDesign.spacingL) {
                    // Title Input
                    VStack(alignment: .leading, spacing: LiquidGlassDesign.spacingS) {
                        Text("Titre de l'événement")
                            .font(LiquidGlassDesign.titleS)
                        TextField("Ex: Réunion d'équipe", text: $title)
                            .textFieldStyle(LiquidGlassTextFieldStyle())
                    }
                    
                    // Description Input
                    VStack(alignment: .leading, spacing: LiquidGlassDesign.spacingS) {
                        Text("Description")
                            .font(LiquidGlassDesign.titleS)
                        TextEditor(text: $description)
                            .frame(height: 100)
                            .padding(LiquidGlassDesign.spacingM)
                            .background(
                                RoundedRectangle(cornerRadius: LiquidGlassDesign.radiusM)
                                    .fill(LiquidGlassDesign.glassColor)
                            )
                            .overlay(
                                RoundedRectangle(cornerRadius: LiquidGlassDesign.radiusM)
                                    .stroke(Color.white.opacity(0.2), lineWidth: 1)
                            )
                            .font(LiquidGlassDesign.bodyRegular)
                    }
                    
                    // Deadline Input
                    VStack(alignment: .leading, spacing: LiquidGlassDesign.spacingS) {
                        Text("Date limite de vote")
                            .font(LiquidGlassDesign.titleS)
                        TextField("ISO 8601 (2025-12-25T18:00:00Z)", text: $deadline)
                            .textFieldStyle(LiquidGlassTextFieldStyle())
                    }
                    
                    Divider()
                        .opacity(0.3)
                    
                    // Time Slots Section
                    VStack(alignment: .leading, spacing: LiquidGlassDesign.spacingM) {
                        Text("Créneaux proposés")
                            .font(LiquidGlassDesign.titleS)
                        
                        HStack(spacing: LiquidGlassDesign.spacingS) {
                            TextField("Début", text: $slotStart)
                                .textFieldStyle(LiquidGlassTextFieldStyle())
                            
                            TextField("Fin", text: $slotEnd)
                                .textFieldStyle(LiquidGlassTextFieldStyle())
                            
                            Button(action: addSlot) {
                                Image(systemName: "plus.circle.fill")
                                    .font(.system(size: 24))
                                    .foregroundColor(LiquidGlassDesign.accentBlue)
                            }
                            .padding(.top, LiquidGlassDesign.spacingM)
                        }
                        
                        // Display Added Slots
                        if !slots.isEmpty {
                            VStack(alignment: .leading, spacing: LiquidGlassDesign.spacingS) {
                                Text("Créneaux ajoutés (\(slots.count))")
                                    .font(LiquidGlassDesign.titleS)
                                
                                ForEach(slots, id: \.id) { slot in
                                    SlotCard(
                                        slot: slot,
                                        onRemove: { removeSlot(slot) }
                                    )
                                }
                            }
                            .padding(.top, LiquidGlassDesign.spacingS)
                        }
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
                    }
                    
                    // Create Button
                    Button(action: createEvent) {
                        Text("Créer l'événement")
                    }
                    .buttonStyle(LiquidGlassButtonStyle(isEnabled: !slots.isEmpty))
                    .disabled(slots.isEmpty)
                    
                    Spacer()
                        .frame(height: LiquidGlassDesign.spacingL)
                }
                .padding(.horizontal, LiquidGlassDesign.spacingL)
            }
            .padding(.vertical, LiquidGlassDesign.spacingL)
        }
    }
    
    private func addSlot() {
        guard !slotStart.isEmpty && !slotEnd.isEmpty else { return }
        
        let newSlot = TimeSlot(
            id: "slot-\(slots.count + 1)",
            start: slotStart,
            end: slotEnd,
            timezone: "UTC"
        )
        slots.append(newSlot)
        slotStart = ""
        slotEnd = ""
    }
    
    private func removeSlot(_ slot: TimeSlot) {
        slots.removeAll { $0.id == slot.id }
    }
    
    private func createEvent() {
        if title.isEmpty {
            errorMessage = "Le titre de l'événement est requis"
            showError = true
            return
        }
        
        if deadline.isEmpty {
            errorMessage = "La date limite est requise"
            showError = true
            return
        }
        
        if slots.isEmpty {
            errorMessage = "Au moins un créneau est requis"
            showError = true
            return
        }
        
        let event = Event(
            id: "event-\(Int.random(in: 1000000...9999999))",
            title: title,
            description: description,
            organizerId: "organizer-1",
            participants: [],
            proposedSlots: slots,
            deadline: deadline,
            status: EventStatus.DRAFT
        )
        
        onEventCreated(event)
        onNavigateToParticipants(event.id)
    }
}

struct SlotCard: View {
    let slot: TimeSlot
    let onRemove: () -> Void
    
    var body: some View {
        HStack(spacing: LiquidGlassDesign.spacingM) {
            VStack(alignment: .leading, spacing: LiquidGlassDesign.spacingXS) {
                Text("De: \(slot.start)")
                    .font(LiquidGlassDesign.bodySmall)
                Text("À: \(slot.end)")
                    .font(LiquidGlassDesign.bodySmall)
            }
            
            Spacer()
            
            Button(action: onRemove) {
                Image(systemName: "xmark.circle.fill")
                    .foregroundColor(LiquidGlassDesign.errorRed)
            }
        }
        .liquidGlassCard()
    }
}

#Preview {
    EventCreationView(
        onEventCreated: { _ in },
        onNavigateToParticipants: { _ in }
    )
}
