import SwiftUI
import Shared

struct PollVotingView: View {
    let event: Event
    let repository: EventRepositoryInterface
    let participantId: String
    let onVoteSubmitted: () -> Void
    
    @State private var votes: [String: Vote] = [:]
    @State private var isLoading = false
    @State private var errorMessage = ""
    @State private var showError = false
    @State private var showSuccess = false
    @State private var hasVoted = false
    
    var body: some View {
        ZStack {
            // Premium dark background
            Color.black
                .ignoresSafeArea()
            
            VStack(spacing: 0) {
                // Header - Large Typography
                VStack(spacing: 4) {
                    Text("CAST YOUR VOTE")
                        .font(.system(size: 13, weight: .medium, design: .rounded))
                        .foregroundColor(.white.opacity(0.5))
                        .textCase(.uppercase)
                        .tracking(1.2)
                    
                    Text(event.title)
                        .font(.system(size: 34, weight: .bold, design: .rounded))
                        .foregroundColor(.white)
                        .multilineTextAlignment(.center)
                    
                    Text("Choose your availability")
                        .font(.system(size: 15, design: .rounded))
                        .foregroundColor(.white.opacity(0.6))
                }
                .padding(.top, 60)
                .padding(.horizontal, 20)
                
                ScrollView {
                    VStack(spacing: 24) {
                        // Event Info Card
                        VStack(spacing: 16) {
                            HStack {
                                Image(systemName: "calendar")
                                    .font(.system(size: 20))
                                    .foregroundColor(.orange)
                                
                                Text("Event Details")
                                    .font(.system(size: 18, weight: .semibold, design: .rounded))
                                    .foregroundColor(.white)
                                
                                Spacer()
                            }
                            
                             if let description = event.description as? String, !description.isEmpty {
                                Text(description)
                                    .font(.system(size: 14, design: .rounded))
                                    .foregroundColor(.white.opacity(0.8))
                                    .multilineTextAlignment(.leading)
                            }
                            
                            HStack {
                                Image(systemName: "clock")
                                    .font(.system(size: 16))
                                    .foregroundColor(.white.opacity(0.6))
                                
                                Text("Deadline: \(formatDeadline(event.deadline))")
                                    .font(.system(size: 14, design: .rounded))
                                    .foregroundColor(.white.opacity(0.8))
                            }
                        }
                        .padding(24)
                        .liquidGlass(cornerRadius: 24, opacity: 0.9)
                        
                        // Voting Instructions
                        VStack(spacing: 12) {
                            Text("How to Vote")
                                .font(.system(size: 18, weight: .semibold, design: .rounded))
                                .foregroundColor(.white)
                            
                            VStack(alignment: .leading, spacing: 8) {
                                VoteOptionRow(option: .yes, description: "Perfect time for me")
                                VoteOptionRow(option: .maybe, description: "Could work if needed")
                                VoteOptionRow(option: .no, description: "Not available")
                            }
                        }
                        .padding(24)
                        .liquidGlass(cornerRadius: 24, opacity: 0.9)
                        
                        // Time Slots Voting
                        VStack(spacing: 20) {
                            HStack {
                                Text("Time Slots")
                                    .font(.system(size: 20, weight: .bold, design: .rounded))
                                    .foregroundColor(.white)
                                
                                Spacer()
                                
                                Text("\(votes.count)/\(event.proposedSlots.count) voted")
                                    .font(.system(size: 14, design: .rounded))
                                    .foregroundColor(.white.opacity(0.8))
                            }
                            
                            ForEach(event.proposedSlots.indices, id: \.self) { index in
                                let slot = event.proposedSlots[index]
                                TimeSlotVoteCard(
                                    timeSlot: slot,
                                    selectedVote: votes[slot.id],
                                    onVoteSelected: { vote in
                                        votes[slot.id] = vote
                                    }
                                )
                            }
                        }
                        .padding(24)
                        .liquidGlass(cornerRadius: 24, opacity: 0.9)
                        
                        // Submit Votes Button
                        if !hasVoted {
                            Button(action: {
                                Task {
                                    await submitVotes()
                                }
                            }) {
                                ZStack {
                                    if isLoading {
                                        ProgressView()
                                            .progressViewStyle(CircularProgressViewStyle(tint: .white))
                                    } else {
                                        Text("Submit Votes")
                                            .font(.system(size: 18, weight: .bold, design: .rounded))
                                            .foregroundColor(.white)
                                    }
                                }
                                .frame(maxWidth: .infinity)
                                .frame(height: 56)
                                .background(
                                    LinearGradient(
                                        gradient: Gradient(colors: [
                                            Color.orange.opacity(0.8),
                                            Color.red.opacity(0.8)
                                        ]),
                                        startPoint: .leading,
                                        endPoint: .trailing
                                    )
                                )
                                .cornerRadius(28)
                                .shadow(color: Color.orange.opacity(0.3), radius: 10, x: 0, y: 5)
                            }
                            .disabled(votes.count != event.proposedSlots.count || isLoading)
                            .opacity(votes.count != event.proposedSlots.count || isLoading ? 0.6 : 1.0)
                            .padding(.horizontal, 24)
                            .padding(.top, 8)
                        } else {
                            VStack(spacing: 16) {
                                Image(systemName: "checkmark.circle.fill")
                                    .font(.system(size: 48))
                                    .foregroundColor(.green)
                                
                                Text("Votes Submitted!")
                                    .font(.system(size: 18, weight: .semibold, design: .rounded))
                                    .foregroundColor(.white)
                                
                                Text("Thank you for participating")
                                    .font(.system(size: 14, design: .rounded))
                                    .foregroundColor(.white.opacity(0.8))
                            }
                            .padding(.vertical, 40)
                        }
                        
                        Spacer(minLength: 40)
                    }
                    .padding(.horizontal, 20)
                }
            }
        }
        .onAppear {
            checkExistingVotes()
        }
        .alert("Error", isPresented: $showError) {
            Button("OK", role: .cancel) {}
        } message: {
            Text(errorMessage)
        }
        .alert("Success", isPresented: $showSuccess) {
            Button("OK", role: .cancel) {
                onVoteSubmitted()
            }
        } message: {
            Text("Your votes have been submitted successfully!")
        }
    }
    
    private func checkExistingVotes() {
        if let poll = repository.getPoll(eventId: event.id) {
            // Check if this participant has already voted
            if let participantVoteMap = poll.votes[participantId] as? [String: Vote] {
                hasVoted = !participantVoteMap.isEmpty
                
                if hasVoted {
                    // Load existing votes
                    votes = participantVoteMap
                }
            }
        }
    }
    
    private func submitVotes() async {
        guard votes.count == event.proposedSlots.count else { return }
        
        isLoading = true
        
        // Submit votes for each slot
        var successCount = 0
        var lastError: Error?
        
        for (slotId, vote) in votes {
            do {
                let result = try await repository.addVote(
                    eventId: event.id,
                    participantId: participantId,
                    slotId: slotId,
                    vote: vote
                )
                
                if let success = result as? Bool, success {
                    successCount += 1
                }
            } catch {
                lastError = error
            }
        }
        
        isLoading = false
        
        if successCount == votes.count {
            hasVoted = true
            showSuccess = true
        } else {
            errorMessage = lastError?.localizedDescription ?? "Failed to submit some votes"
            showError = true
        }
    }
    
    private func formatDeadline(_ deadlineString: String) -> String {
        if let date = ISO8601DateFormatter().date(from: deadlineString) {
            let formatter = DateFormatter()
            formatter.dateStyle = .medium
            formatter.timeStyle = .short
            return formatter.string(from: date)
        }
        return deadlineString
    }
}

struct VoteOptionRow: View {
    let option: Vote
    let description: String
    
    var body: some View {
        HStack(spacing: 12) {
            ZStack {
                Circle()
                    .fill(voteColor(for: option))
                    .frame(width: 24, height: 24)
                
                Text(voteSymbol(for: option))
                    .font(.system(size: 14, weight: .bold, design: .rounded))
                    .foregroundColor(.white)
            }
            
            VStack(alignment: .leading, spacing: 2) {
                Text(voteLabel(for: option))
                    .font(.system(size: 14, weight: .semibold, design: .rounded))
                    .foregroundColor(.white)
                
                Text(description)
                    .font(.system(size: 12, design: .rounded))
                    .foregroundColor(.white.opacity(0.7))
            }
        }
    }
    
    private func voteColor(for vote: Vote) -> Color {
        switch vote {
        case .yes: return .green
        case .maybe: return .orange
        case .no: return .red
        default: return .gray
        }
    }
    
    private func voteSymbol(for vote: Vote) -> String {
        switch vote {
        case .yes: return "✓"
        case .maybe: return "~"
        case .no: return "✗"
        default: return "?"
        }
    }
    
    private func voteLabel(for vote: Vote) -> String {
        switch vote {
        case .yes: return "Yes"
        case .maybe: return "Maybe"
        case .no: return "No"
        default: return "Unknown"
        }
    }
}

struct TimeSlotVoteCard: View {
    let timeSlot: TimeSlot
    let selectedVote: Vote?
    let onVoteSelected: (Vote) -> Void
    
    var body: some View {
        VStack(spacing: 16) {
            // Time slot info
            VStack(spacing: 8) {
                Text(formatDate(timeSlot.start))
                    .font(.system(size: 16, weight: .semibold, design: .rounded))
                    .foregroundColor(.white)
                
                HStack(spacing: 8) {
                    Image(systemName: "clock")
                        .font(.system(size: 14))
                        .foregroundColor(.white.opacity(0.6))
                    
                    Text("\(formatTime(timeSlot.start)) - \(formatTime(timeSlot.end))")
                        .font(.system(size: 14, design: .rounded))
                        .foregroundColor(.white.opacity(0.8))
                }
            }
            
            // Vote buttons
            HStack(spacing: 16) {
                VoteButton(
                    vote: .yes,
                    isSelected: selectedVote == .yes,
                    action: { onVoteSelected(.yes) }
                )
                
                VoteButton(
                    vote: .maybe,
                    isSelected: selectedVote == .maybe,
                    action: { onVoteSelected(.maybe) }
                )
                
                VoteButton(
                    vote: .no,
                    isSelected: selectedVote == .no,
                    action: { onVoteSelected(.no) }
                )
            }
        }
        .padding(20)
        .background(Color.white.opacity(0.1))
        .cornerRadius(16)
    }
    
    private func formatDate(_ dateString: String) -> String {
        if let date = ISO8601DateFormatter().date(from: dateString) {
            let formatter = DateFormatter()
            formatter.dateStyle = .medium
            formatter.timeStyle = .none
            return formatter.string(from: date)
        }
        return dateString
    }
    
    private func formatTime(_ dateString: String) -> String {
        if let date = ISO8601DateFormatter().date(from: dateString) {
            let formatter = DateFormatter()
            formatter.dateStyle = .none
            formatter.timeStyle = .short
            return formatter.string(from: date)
        }
        return dateString
    }
}

struct PollVotingView_Previews: PreviewProvider {
    static var previews: some View {
        let sampleSlots = [
            TimeSlot(
                id: "slot-1",
                start: ISO8601DateFormatter().string(from: Date().addingTimeInterval(24 * 60 * 60)),
                end: ISO8601DateFormatter().string(from: Date().addingTimeInterval(26 * 60 * 60)),
                timezone: "America/New_York"
            ),
            TimeSlot(
                id: "slot-2", 
                start: ISO8601DateFormatter().string(from: Date().addingTimeInterval(48 * 60 * 60)),
                end: ISO8601DateFormatter().string(from: Date().addingTimeInterval(50 * 60 * 60)),
                timezone: "America/New_York"
            )
        ]
        
        let now = ISO8601DateFormatter().string(from: Date())
        let sampleEvent = Event(
            id: "sample-event",
            title: "Team Meeting",
            description: "Weekly sync meeting",
            organizerId: "organizer-1",
            participants: ["alice@example.com"],
            proposedSlots: sampleSlots,
            deadline: ISO8601DateFormatter().string(from: Date().addingTimeInterval(7 * 24 * 60 * 60)),
            status: EventStatus.polling,
            finalDate: nil,
            createdAt: now,
            updatedAt: now
        )
        
        PollVotingView(
            event: sampleEvent,
            repository: EventRepository(),
            participantId: "participant-1",
            onVoteSubmitted: {}
        )
    }
}