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
            VStack(alignment: .leading, spacing: 16) {
                Text("Manage Participants")
                    .font(.system(size: 32, weight: .bold))
                
                Text("Event: \(event.title)")
                    .font(.caption)
                    .foregroundColor(.gray)
                
                // Add Participant Section
                VStack(alignment: .leading, spacing: 12) {
                    Text("Add Participant")
                        .font(.system(size: 16, weight: .semibold))
                    
                    HStack(spacing: 8) {
                        TextField("Email", text: $newParticipantEmail)
                            .textFieldStyle(.roundedBorder)
                            .keyboardType(.emailAddress)
                        
                        Button(action: addParticipant) {
                            Text("Add")
                                .frame(maxWidth: .infinity)
                        }
                        .buttonStyle(.bordered)
                    }
                    
                    if showError {
                        Text(errorMessage)
                            .font(.caption)
                            .foregroundColor(.red)
                            .padding(.top, 4)
                    }
                }
                .padding(16)
                .background(Color.gray.opacity(0.05))
                .cornerRadius(8)
                
                // Participants List
                if !participants.isEmpty {
                    VStack(alignment: .leading, spacing: 12) {
                        Text("Participants (\(participants.count))")
                            .font(.system(size: 16, weight: .semibold))
                        
                        ForEach(participants, id: \.self) { email in
                            ParticipantRow(
                                email: email,
                                onRemove: { removeParticipant(email) }
                            )
                        }
                    }
                } else {
                    VStack {
                        Text("No participants added yet")
                            .font(.caption)
                            .foregroundColor(.gray)
                    }
                    .frame(maxWidth: .infinity)
                    .padding(32)
                }
                
                Spacer()
                
                // Action Buttons
                HStack(spacing: 12) {
                    Button(action: { onParticipantsAdded(event.id) }) {
                        Text("Back")
                            .frame(maxWidth: .infinity)
                            .padding(12)
                    }
                    .buttonStyle(.bordered)
                    
                    Button(action: startPoll) {
                        Text("Start Poll")
                            .frame(maxWidth: .infinity)
                            .padding(12)
                    }
                    .buttonStyle(.borderedProminent)
                    .disabled(participants.isEmpty)
                }
            }
            .padding(16)
        }
        .onAppear {
            participants = event.participants
        }
    }
    
    private func addParticipant() {
        let email = newParticipantEmail.trimmingCharacters(in: .whitespaces)
        
        guard !email.isEmpty else {
            errorMessage = "Email is required"
            showError = true
            return
        }
        
        guard isValidEmail(email) else {
            errorMessage = "Invalid email format"
            showError = true
            return
        }
        
        guard !participants.contains(email) else {
            errorMessage = "Participant already added"
            showError = true
            return
        }
        
        let result = repository.addParticipant(eventId: event.id, participantEmail: email)
        
        if result.isSuccess {
            participants.append(email)
            newParticipantEmail = ""
            showError = false
        } else {
            errorMessage = "Failed to add participant"
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
        HStack {
            VStack(alignment: .leading, spacing: 4) {
                Text(email)
                    .font(.callout)
                Text("Not yet voted")
                    .font(.caption)
                    .foregroundColor(.gray)
            }
            
            Spacer()
            
            Button(action: onRemove) {
                Text("Remove")
                    .font(.caption)
            }
            .buttonStyle(.bordered)
        }
        .padding(12)
        .background(Color.gray.opacity(0.05))
        .cornerRadius(6)
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
        status: EventStatus.draft
    )
    
    ParticipantManagementView(
        event: mockEvent,
        repository: mockRepository,
        onParticipantsAdded: { _ in },
        onNavigateToPoll: { _ in }
    )
}
