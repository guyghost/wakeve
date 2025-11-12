import SwiftUI
import Shared

struct ParticipantManagementView: View {
    let event: Event
    let repository: EventRepository
    
    @State private var newParticipantEmail = ""
    @State private var participants: [String] = []
    @State private var isError = false
    @State private var errorMessage = ""
    @State private var showError = false
    
    var onParticipantsAdded: (String) -> Void
    var onNavigateToPoll: (String) -> Void
    
    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: LiquidGlassDesign.spacingL) {
                // Header
                VStack(alignment: .leading, spacing: LiquidGlassDesign.spacingS) {
                    Text("Inviter les participants")
                        .font(LiquidGlassDesign.titleL)
                    Text("Événement: \(event.title)")
                        .font(LiquidGlassDesign.bodySmall)
                        .foregroundColor(.secondary)
                }
                .padding(.horizontal, LiquidGlassDesign.spacingL)
                
                VStack(spacing: LiquidGlassDesign.spacingL) {
                    // Add Participant Section
                    VStack(alignment: .leading, spacing: LiquidGlassDesign.spacingM) {
                        Text("Ajouter un participant")
                            .font(LiquidGlassDesign.titleS)
                        
                        HStack(spacing: LiquidGlassDesign.spacingS) {
                            TextField("email@exemple.com", text: $newParticipantEmail)
                                .textFieldStyle(LiquidGlassTextFieldStyle())
                                .keyboardType(.emailAddress)
                                .autocorrectionDisabled()
                            
                            Button(action: addParticipant) {
                                Image(systemName: "plus.circle.fill")
                                    .font(.system(size: 24))
                                    .foregroundColor(LiquidGlassDesign.accentBlue)
                            }
                            .padding(.top, LiquidGlassDesign.spacingM)
                        }
                        
                        if showError {
                            HStack(spacing: LiquidGlassDesign.spacingS) {
                                Image(systemName: "exclamationmark.circle.fill")
                                    .foregroundColor(LiquidGlassDesign.errorRed)
                                
                                Text(errorMessage)
                                    .font(LiquidGlassDesign.bodySmall)
                                    .foregroundColor(LiquidGlassDesign.errorRed)
                            }
                            .padding(LiquidGlassDesign.spacingM)
                            .background(
                                RoundedRectangle(cornerRadius: LiquidGlassDesign.radiusM)
                                    .fill(LiquidGlassDesign.errorRed.opacity(0.1))
                            )
                        }
                    }
                    .liquidGlassCard()
                    
                    // Participants List
                    if !participants.isEmpty {
                        VStack(alignment: .leading, spacing: LiquidGlassDesign.spacingM) {
                            HStack {
                                Text("Participants")
                                    .font(LiquidGlassDesign.titleS)
                                
                                Text("\(participants.count)")
                                    .font(LiquidGlassDesign.titleS)
                                    .foregroundColor(LiquidGlassDesign.accentBlue)
                            }
                            
                            VStack(spacing: LiquidGlassDesign.spacingS) {
                                ForEach(participants, id: \.self) { email in
                                    ParticipantRow(
                                        email: email,
                                        onRemove: { removeParticipant(email) }
                                    )
                                }
                            }
                        }
                        .padding(.horizontal, LiquidGlassDesign.spacingL)
                    } else {
                        VStack(spacing: LiquidGlassDesign.spacingM) {
                            Image(systemName: "person.2.slash")
                                .font(.system(size: 48))
                                .foregroundColor(.secondary.opacity(0.5))
                            
                            Text("Aucun participant")
                                .font(LiquidGlassDesign.titleS)
                            
                            Text("Ajoutez des participants pour pouvoir créer un sondage")
                                .font(LiquidGlassDesign.bodySmall)
                                .foregroundColor(.secondary)
                                .multilineTextAlignment(.center)
                        }
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, LiquidGlassDesign.spacingXXL)
                        .padding(.horizontal, LiquidGlassDesign.spacingL)
                    }
                    
                    Spacer()
                    
                    // Action Buttons
                    HStack(spacing: LiquidGlassDesign.spacingM) {
                        Button(action: { onParticipantsAdded(event.id) }) {
                            Text("Retour")
                                .frame(maxWidth: .infinity)
                        }
                        .buttonStyle(LiquidGlassSecondaryButtonStyle())
                        
                        Button(action: startPoll) {
                            Text("Lancer le vote")
                                .frame(maxWidth: .infinity)
                        }
                        .buttonStyle(LiquidGlassButtonStyle(isEnabled: !participants.isEmpty))
                        .disabled(participants.isEmpty)
                    }
                    .padding(.horizontal, LiquidGlassDesign.spacingL)
                }
            }
            .padding(.vertical, LiquidGlassDesign.spacingL)
        }
        .onAppear {
            participants = event.participants
        }
    }
    
    private func addParticipant() {
        let email = newParticipantEmail.trimmingCharacters(in: .whitespaces)
        
        guard !email.isEmpty else {
            errorMessage = "L'email est requis"
            showError = true
            return
        }
        
        guard isValidEmail(email) else {
            errorMessage = "Format d'email invalide"
            showError = true
            return
        }
        
        guard !participants.contains(email) else {
            errorMessage = "Ce participant a déjà été ajouté"
            showError = true
            return
        }
        
        let result = repository.addParticipant(eventId: event.id, participantEmail: email)
        
        if result.isSuccess {
            participants.append(email)
            newParticipantEmail = ""
            showError = false
        } else {
            errorMessage = "Erreur lors de l'ajout du participant"
            showError = true
        }
    }
    
    private func removeParticipant(_ email: String) {
        participants.removeAll { $0 == email }
    }
    
    private func startPoll() {
        repository.updateEventStatus(eventId: event.id, status: EventStatus.POLLING)
        onNavigateToPoll(event.id)
    }
    
    private func isValidEmail(_ email: String) -> Bool {
        return email.contains("@") && email.contains(".")
    }
}

struct ParticipantRow: View {
    let email: String
    let onRemove: () -> Void
    
    var body: some View {
        HStack(spacing: LiquidGlassDesign.spacingM) {
            HStack(spacing: LiquidGlassDesign.spacingS) {
                Image(systemName: "person.circle.fill")
                    .font(.system(size: 20))
                    .foregroundColor(LiquidGlassDesign.accentBlue.opacity(0.6))
                
                VStack(alignment: .leading, spacing: LiquidGlassDesign.spacingXS) {
                    Text(email)
                        .font(LiquidGlassDesign.bodyRegular)
                        .lineLimit(1)
                    Text("En attente de vote")
                        .font(LiquidGlassDesign.caption)
                        .foregroundColor(.secondary)
                }
            }
            
            Spacer()
            
            Button(action: onRemove) {
                Image(systemName: "xmark.circle.fill")
                    .foregroundColor(LiquidGlassDesign.errorRed.opacity(0.6))
            }
        }
        .liquidGlassCard()
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
        proposedSlots: [],
        deadline: "2025-12-25T18:00:00Z",
        status: EventStatus.DRAFT
    )
    
    ParticipantManagementView(
        event: mockEvent,
        repository: mockRepository,
        onParticipantsAdded: { _ in },
        onNavigateToPoll: { _ in }
    )
}
