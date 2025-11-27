import SwiftUI
import Shared

/// Modern participant management view inspired by Apple Invites
/// Features: Clean list design, easy participant management, clear status indicators
struct ModernParticipantManagementView: View {
    let event: Event
    let repository: EventRepository
    let onParticipantsUpdated: () -> Void
    let onBack: () -> Void

    @State private var newParticipantEmail = ""
    @State private var participants: [String] = []
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

                        Spacer()

                        // Start Poll Button (if draft and has participants)
                        if event.status == .draft && !participants.isEmpty {
                            Button {
                                Task {
                                    await startPoll()
                                }
                            } label: {
                                if isLoading {
                                    ProgressView()
                                        .progressViewStyle(CircularProgressViewStyle())
                                } else {
                                    Text("Start Poll")
                                        .font(.system(size: 17, weight: .semibold))
                                        .foregroundColor(.blue)
                                }
                            }
                            .disabled(isLoading)
                        }
                    }
                    .padding(.horizontal, 20)
                    .padding(.top, 60)

                    VStack(spacing: 8) {
                        Text("Participants")
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

                ScrollView {
                    VStack(spacing: 16) {
                        // Add Participant Card (only in draft)
                        if event.status == .draft {
                            VStack(spacing: 16) {
                                HStack {
                                    Text("Add Participant")
                                        .font(.system(size: 17, weight: .semibold))
                                        .foregroundColor(.primary)

                                    Spacer()
                                }

                                HStack(spacing: 12) {
                                    TextField("Email address", text: $newParticipantEmail)
                                        .font(.system(size: 17))
                                        .textFieldStyle(.plain)
                                        .padding(14)
                                        .background(Color(.tertiarySystemFill))
                                        .cornerRadius(12)
                                        .keyboardType(.emailAddress)
                                        .autocapitalization(.none)
                                        .textContentType(.emailAddress)

                                    Button {
                                        Task {
                                            await addParticipant()
                                        }
                                    } label: {
                                        if isLoading {
                                            ProgressView()
                                                .progressViewStyle(CircularProgressViewStyle())
                                                .frame(width: 44, height: 44)
                                        } else {
                                            Image(systemName: "plus.circle.fill")
                                                .font(.system(size: 32))
                                                .foregroundColor(.blue)
                                        }
                                    }
                                    .disabled(newParticipantEmail.isEmpty || isLoading)
                                }
                            }
                            .padding(20)
                            .background(Color(.systemBackground))
                            .cornerRadius(16)
                        }

                        // Status Banner
                        HStack {
                            VStack(alignment: .leading, spacing: 4) {
                                Text("\(participants.count) Participant\(participants.count == 1 ? "" : "s")")
                                    .font(.system(size: 20, weight: .semibold))
                                    .foregroundColor(.primary)

                                HStack(spacing: 6) {
                                    Circle()
                                        .fill(statusColor)
                                        .frame(width: 8, height: 8)

                                    Text(statusText)
                                        .font(.system(size: 15))
                                        .foregroundColor(.secondary)
                                }
                            }

                            Spacer()
                        }
                        .padding(.horizontal, 4)

                        // Participants List
                        if participants.isEmpty {
                            VStack(spacing: 20) {
                                Image(systemName: "person.2")
                                    .font(.system(size: 56))
                                    .foregroundColor(.secondary.opacity(0.5))
                                    .padding(.top, 40)

                                VStack(spacing: 8) {
                                    Text("No Participants Yet")
                                        .font(.system(size: 24, weight: .bold))
                                        .foregroundColor(.primary)

                                    Text(event.status == .draft ? "Add participants to start the event" : "No one has joined yet")
                                        .font(.system(size: 17))
                                        .foregroundColor(.secondary)
                                        .multilineTextAlignment(.center)
                                        .padding(.horizontal, 40)
                                }
                                .padding(.bottom, 40)
                            }
                            .frame(maxWidth: .infinity)
                            .background(Color(.systemBackground))
                            .cornerRadius(16)
                        } else {
                            VStack(spacing: 0) {
                                ForEach(Array(participants.enumerated()), id: \.element) { index, participant in
                                    ModernParticipantRowView(email: participant)

                                    if index < participants.count - 1 {
                                        Divider()
                                            .padding(.leading, 60)
                                    }
                                }
                            }
                            .background(Color(.systemBackground))
                            .cornerRadius(16)
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
            loadParticipants()
        }
        .alert("Error", isPresented: $showError) {
            Button("OK", role: .cancel) {}
        } message: {
            Text(errorMessage)
        }
        .alert("Success", isPresented: $showSuccess) {
            Button("OK", role: .cancel) {
                onBack()
            }
        } message: {
            Text("Poll started successfully!")
        }
    }

    private var statusColor: Color {
        switch event.status {
        case .draft: return .orange
        case .polling: return .blue
        case .confirmed: return .green
        default: return .gray
        }
    }

    private var statusText: String {
        switch event.status {
        case .draft: return "Draft - Add participants to begin"
        case .polling: return "Poll Active"
        case .confirmed: return "Event Confirmed"
        default: return "Unknown Status"
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

// MARK: - Modern Participant Row

struct ModernParticipantRowView: View {
    let email: String

    private var initials: String {
        let components = email.components(separatedBy: "@")
        if let name = components.first, !name.isEmpty {
            return String(name.prefix(1).uppercased())
        }
        return String(email.prefix(1).uppercased())
    }

    private var avatarColor: Color {
        let colors: [Color] = [.red, .orange, .yellow, .green, .blue, .purple, .pink]
        let hash = abs(email.hashValue)
        return colors[hash % colors.count]
    }

    var body: some View {
        HStack(spacing: 16) {
            // Avatar
            Circle()
                .fill(avatarColor)
                .frame(width: 44, height: 44)
                .overlay(
                    Text(initials)
                        .font(.system(size: 18, weight: .semibold))
                        .foregroundColor(.white)
                )

            // Email
            VStack(alignment: .leading, spacing: 2) {
                Text(email)
                    .font(.system(size: 17))
                    .foregroundColor(.primary)

                Text("Participant")
                    .font(.system(size: 13))
                    .foregroundColor(.secondary)
            }

            Spacer()

            // Status indicator
            Image(systemName: "checkmark.circle.fill")
                .font(.system(size: 20))
                .foregroundColor(.green)
        }
        .padding(16)
    }
}
