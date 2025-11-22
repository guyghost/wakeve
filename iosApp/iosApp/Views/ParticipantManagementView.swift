import SwiftUI
import Shared

struct ParticipantManagementView: View {
    let event: Event
    let repository: EventRepository
    let onParticipantsUpdated: () -> Void
    
    @State private var newParticipantEmail = ""
    @State private var participants: [String] = []
    @State private var isLoading = false
    @State private var errorMessage = ""
    @State private var showError = false
    @State private var showSuccess = false
    
    var body: some View {
        ZStack {
            // Background gradient
            LinearGradient(
                gradient: Gradient(colors: [
                    Color.green.opacity(0.1),
                    Color.blue.opacity(0.1),
                    Color.purple.opacity(0.1)
                ]),
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
            .ignoresSafeArea()
            
            VStack(spacing: 0) {
                // Header
                VStack(spacing: 8) {
                    Text(event.title)
                        .font(.system(size: 24, weight: .bold, design: .rounded))
                        .foregroundColor(.white)
                        .multilineTextAlignment(.center)
                    
                    Text("Manage Participants")
                        .font(.system(size: 16, weight: .medium, design: .rounded))
                        .foregroundColor(.white.opacity(0.8))
                }
                .padding(.top, 60)
                .padding(.horizontal, 20)
                
                ScrollView {
                    VStack(spacing: 24) {
                        // Add Participant Card
                        if event.status == EventStatus.draft {
                            VStack(spacing: 20) {
                                VStack(alignment: .leading, spacing: 8) {
                                    Text("Add Participant")
                                        .font(.system(size: 18, weight: .semibold, design: .rounded))
                                        .foregroundColor(.white)
                                    
                                    HStack(spacing: 12) {
                                        TextField("Enter email address", text: $newParticipantEmail)
                                            .padding()
                                            .background(Color.white.opacity(0.1))
                                            .cornerRadius(12)
                                            .foregroundColor(.white)
                                            .font(.system(size: 16, design: .rounded))
                                            .keyboardType(.emailAddress)
                                            .autocapitalization(.none)
                                        
                                        Button {
                                            Task {
                                                await addParticipant()
                                            }
                                        } label: {
                                            ZStack {
                                                if isLoading {
                                                    ProgressView()
                                                        .progressViewStyle(CircularProgressViewStyle(tint: .white))
                                                        .scaleEffect(0.8)
                                                } else {
                                                    Image(systemName: "plus.circle.fill")
                                                        .font(.system(size: 24))
                                                        .foregroundColor(.green)
                                                }
                                            }
                                            .frame(width: 44, height: 44)
                                            .background(Color.white.opacity(0.1))
                                            .cornerRadius(22)
                                        }
                                        .disabled(newParticipantEmail.isEmpty || isLoading)
                                    }
                                }
                            }
                            .padding(24)
                            .liquidGlass(cornerRadius: 24, opacity: 0.9)
                        }
                        
                        // Participants List Card
                        VStack(spacing: 20) {
                            HStack {
                                Text("Participants (\(participants.count))")
                                    .font(.system(size: 20, weight: .bold, design: .rounded))
                                    .foregroundColor(.white)
                                
                                Spacer()
                                
                                if event.status == EventStatus.draft {
                                    Text("Draft")
                                        .font(.system(size: 12, weight: .medium, design: .rounded))
                                        .foregroundColor(.orange)
                                        .padding(.horizontal, 8)
                                        .padding(.vertical, 4)
                                        .background(Color.orange.opacity(0.2))
                                        .cornerRadius(8)
                                } else {
                                    Text("Poll Active")
                                        .font(.system(size: 12, weight: .medium, design: .rounded))
                                        .foregroundColor(.green)
                                        .padding(.horizontal, 8)
                                        .padding(.vertical, 4)
                                        .background(Color.green.opacity(0.2))
                                        .cornerRadius(8)
                                }
                            }
                            
                            if participants.isEmpty {
                                VStack(spacing: 16) {
                                    Image(systemName: "person.2.circle")
                                        .font(.system(size: 48))
                                        .foregroundColor(.white.opacity(0.6))
                                    
                                    Text("No participants yet")
                                        .font(.system(size: 16, weight: .medium, design: .rounded))
                                        .foregroundColor(.white.opacity(0.8))
                                    
                                    if event.status == EventStatus.draft {
                                        Text("Add participants to start the event")
                                            .font(.system(size: 14, design: .rounded))
                                            .foregroundColor(.white.opacity(0.6))
                                            .multilineTextAlignment(.center)
                                    }
                                }
                                .padding(.vertical, 40)
                            } else {
                                VStack(spacing: 12) {
                                    ForEach(participants, id: \.self) { participant in
                                        ParticipantRow(email: participant)
                                    }
                                }
                            }
                        }
                        .padding(24)
                        .liquidGlass(cornerRadius: 24, opacity: 0.9)
                        
                        // Start Poll Button (only for organizer in draft status)
                        if event.status == EventStatus.draft && !participants.isEmpty {
                            Button {
                                Task {
                                    await startPoll()
                                }
                            } label: {
                                ZStack {
                                    if isLoading {
                                        ProgressView()
                                            .progressViewStyle(CircularProgressViewStyle(tint: .white))
                                    } else {
                                        Text("Start Poll")
                                            .font(.system(size: 18, weight: .bold, design: .rounded))
                                            .foregroundColor(.white)
                                    }
                                }
                                .frame(maxWidth: .infinity)
                                .frame(height: 56)
                                .background(
                                    LinearGradient(
                                        gradient: Gradient(colors: [
                                            Color.green.opacity(0.8),
                                            Color.blue.opacity(0.8)
                                        ]),
                                        startPoint: .leading,
                                        endPoint: .trailing
                                    )
                                )
                                .cornerRadius(28)
                                .shadow(color: Color.green.opacity(0.3), radius: 10, x: 0, y: 5)
                            }
                            .padding(.horizontal, 24)
                            .padding(.top, 8)
                        }
                        
                        Spacer(minLength: 40)
                    }
                    .padding(.horizontal, 20)
                }
            }
        }
        .onAppear {
            loadParticipants()
        }
        .alert("Error", isPresented: $showError) {
            Button("OK", role: .cancel) {}
        } message: {
            Text(errorMessage)
        }
        .alert("Success", isPresented: $showSuccess) {
            Button("OK", role: .cancel) {}
        } message: {
            Text("Poll started successfully!")
        }
    }
    
    private func loadParticipants() {
        participants = repository.getParticipants(eventId: event.id) ?? []
    }
    
    private func addParticipant() async {
        guard !newParticipantEmail.isEmpty else { return }
        
        // Basic email validation
        let emailRegex = "[A-Z0-9a-z._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,64}"
        let emailPredicate = NSPredicate(format:"SELF MATCHES %@", emailRegex)
        
        guard emailPredicate.evaluate(with: newParticipantEmail) else {
            errorMessage = "Please enter a valid email address"
            showError = true
            return
        }
        
        isLoading = true
        
        do {
            let result = try await repository.addParticipant(eventId: event.id, participantId: newParticipantEmail)
            
            if let success = result as? Bool, success {
                isLoading = false
                newParticipantEmail = ""
                loadParticipants()
                onParticipantsUpdated()
            } else {
                isLoading = false
                errorMessage = "Failed to add participant"
                showError = true
            }
        } catch {
            isLoading = false
            errorMessage = error.localizedDescription
            showError = true
        }
    }
    
    private func startPoll() async {
        isLoading = true
        
        do {
            let result = try await repository.updateEventStatus(
                id: event.id,
                status: EventStatus.polling,
                finalDate: nil
            )
            
            if let success = result as? Bool, success {
                isLoading = false
                showSuccess = true
                onParticipantsUpdated()
            } else {
                isLoading = false
                errorMessage = "Failed to start poll"
                showError = true
            }
        } catch {
            isLoading = false
            errorMessage = error.localizedDescription
            showError = true
        }
    }
}

struct ParticipantRow: View {
    let email: String
    
    var body: some View {
        HStack(spacing: 16) {
            Image(systemName: "person.circle.fill")
                .font(.system(size: 32))
                .foregroundColor(.blue.opacity(0.8))
            
            VStack(alignment: .leading, spacing: 2) {
                Text(email)
                    .font(.system(size: 16, weight: .medium, design: .rounded))
                    .foregroundColor(.white)
                
                Text("Participant")
                    .font(.system(size: 12, design: .rounded))
                    .foregroundColor(.white.opacity(0.6))
            }
            
            Spacer()
            
            Image(systemName: "checkmark.circle.fill")
                .font(.system(size: 20))
                .foregroundColor(.green.opacity(0.8))
        }
        .padding(16)
        .background(Color.white.opacity(0.1))
        .cornerRadius(12)
    }
}

struct ParticipantManagementView_Previews: PreviewProvider {
    static var previews: some View {
        let now = ISO8601DateFormatter().string(from: Date())
        let sampleEvent = Event(
            id: "sample-event",
            title: "Team Retreat 2025",
            description: "Annual team building event",
            organizerId: "organizer-1",
            participants: ["alice@example.com", "bob@example.com"],
            proposedSlots: [],
            deadline: ISO8601DateFormatter().string(from: Date().addingTimeInterval(7 * 24 * 60 * 60)),
            status: EventStatus.draft,
            finalDate: nil,
            createdAt: now,
            updatedAt: now
        )
        
        ParticipantManagementView(
            event: sampleEvent,
            repository: EventRepository(),
            onParticipantsUpdated: {}
        )
    }
}