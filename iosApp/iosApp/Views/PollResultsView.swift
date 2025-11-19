import SwiftUI
import Shared

struct PollResultsView: View {
    let event: Event
    let repository: EventRepository
    let onDateConfirmed: (String) -> Void
    
    @State private var poll: Poll?
    @State private var slotScores: [PollLogic.SlotScore] = []
    @State private var bestSlot: TimeSlot?
    @State private var isLoading = false
    @State private var errorMessage = ""
    @State private var showError = false
    @State private var showSuccess = false
    
    var body: some View {
        ZStack {
            // Background gradient
            LinearGradient(
                gradient: Gradient(colors: [
                    Color.purple.opacity(0.1),
                    Color.blue.opacity(0.1),
                    Color.teal.opacity(0.1)
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
                    
                    Text("Poll Results")
                        .font(.system(size: 16, weight: .medium, design: .rounded))
                        .foregroundColor(.white.opacity(0.8))
                }
                .padding(.top, 60)
                .padding(.horizontal, 20)
                
                ScrollView {
                    VStack(spacing: 24) {
                        // Event Status Card
                        VStack(spacing: 16) {
                            HStack {
                                Image(systemName: statusIcon)
                                    .font(.system(size: 20))
                                    .foregroundColor(statusColor)
                                
                                Text("Status: \(statusText)")
                                    .font(.system(size: 18, weight: .semibold, design: .rounded))
                                    .foregroundColor(.white)
                                
                                Spacer()
                                
                                if event.status == EventStatus.polling {
                                    Text("Active")
                                        .font(.system(size: 12, weight: .medium, design: .rounded))
                                        .foregroundColor(.green)
                                        .padding(.horizontal, 8)
                                        .padding(.vertical, 4)
                                        .background(Color.green.opacity(0.2))
                                        .cornerRadius(8)
                                } else if event.status == EventStatus.confirmed {
                                    Text("Confirmed")
                                        .font(.system(size: 12, weight: .medium, design: .rounded))
                                        .foregroundColor(.blue)
                                        .padding(.horizontal, 8)
                                        .padding(.vertical, 4)
                                        .background(Color.blue.opacity(0.2))
                                        .cornerRadius(8)
                                }
                            }
                            
                             if let description = event.description as? String, !description.isEmpty {
                                Text(description)
                                    .font(.system(size: 14, design: .rounded))
                                    .foregroundColor(.white.opacity(0.8))
                                    .multilineTextAlignment(.leading)
                            }
                            
                            HStack {
                                Image(systemName: "person.2")
                                    .font(.system(size: 16))
                                    .foregroundColor(.white.opacity(0.6))
                                
                                Text("\(event.participants.count) participants")
                                    .font(.system(size: 14, design: .rounded))
                                    .foregroundColor(.white.opacity(0.8))
                                
                                Spacer()
                                
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
                        
                        if event.status == EventStatus.polling {
                            // Poll Results Card
                            VStack(spacing: 20) {
                                HStack {
                                    Text("Voting Results")
                                        .font(.system(size: 20, weight: .bold, design: .rounded))
                                        .foregroundColor(.white)
                                    
                                    Spacer()
                                    
                                    if let best = bestSlot {
                                        Text("Best: \(formatTime(best.start))")
                                            .font(.system(size: 14, design: .rounded))
                                            .foregroundColor(.green)
                                    }
                                }
                                
                                if slotScores.isEmpty {
                                    VStack(spacing: 16) {
                                        Image(systemName: "chart.bar.xaxis")
                                            .font(.system(size: 48))
                                            .foregroundColor(.white.opacity(0.6))
                                        
                                        Text("No votes yet")
                                            .font(.system(size: 16, weight: .medium, design: .rounded))
                                            .foregroundColor(.white.opacity(0.8))
                                        
                                        Text("Waiting for participants to vote")
                                            .font(.system(size: 14, design: .rounded))
                                            .foregroundColor(.white.opacity(0.6))
                                            .multilineTextAlignment(.center)
                                    }
                                    .padding(.vertical, 40)
                                } else {
                                    VStack(spacing: 16) {
                                        ForEach(slotScores.sorted(by: { $0.totalScore > $1.totalScore }), id: \.slotId) { score in
                                            SlotResultRow(
                                                slot: event.proposedSlots.first { $0.id == score.slotId },
                                                score: score,
                                                isBest: score.slotId == bestSlot?.id
                                            )
                                        }
                                    }
                                }
                            }
                            .padding(24)
                            .liquidGlass(cornerRadius: 24, opacity: 0.9)
                            
                            // Confirm Date Button (only for organizer)
                             if let _ = bestSlot, !slotScores.isEmpty {
                                VStack(spacing: 16) {
                                    Text("Ready to confirm the best time slot?")
                                        .font(.system(size: 16, weight: .medium, design: .rounded))
                                        .foregroundColor(.white)
                                        .multilineTextAlignment(.center)
                                    
                                    Button(action: confirmDate) {
                                        ZStack {
                                            if isLoading {
                                                ProgressView()
                                                    .progressViewStyle(CircularProgressViewStyle(tint: .white))
                                            } else {
                                                Text("Confirm Final Date")
                                                    .font(.system(size: 18, weight: .bold, design: .rounded))
                                                    .foregroundColor(.white)
                                            }
                                        }
                                        .frame(maxWidth: .infinity)
                                        .frame(height: 56)
                                        .background(
                                            LinearGradient(
                                                gradient: Gradient(colors: [
                                                    Color.purple.opacity(0.8),
                                                    Color.blue.opacity(0.8)
                                                ]),
                                                startPoint: .leading,
                                                endPoint: .trailing
                                            )
                                        )
                                        .cornerRadius(28)
                                        .shadow(color: Color.purple.opacity(0.3), radius: 10, x: 0, y: 5)
                                    }
                                    .disabled(isLoading)
                                    .opacity(isLoading ? 0.6 : 1.0)
                                }
                                .padding(.horizontal, 24)
                            }
                        } else if event.status == EventStatus.confirmed, let finalDate = event.finalDate {
                            // Confirmed Date Card
                            VStack(spacing: 20) {
                                HStack {
                                    Image(systemName: "checkmark.circle.fill")
                                        .font(.system(size: 24))
                                        .foregroundColor(.green)
                                    
                                    Text("Event Confirmed!")
                                        .font(.system(size: 20, weight: .bold, design: .rounded))
                                        .foregroundColor(.white)
                                }
                                
                                VStack(spacing: 12) {
                                    Text("Final Date & Time")
                                        .font(.system(size: 16, weight: .semibold, design: .rounded))
                                        .foregroundColor(.white)
                                    
                                    if let slot = event.proposedSlots.first(where: { $0.id == finalDate }) {
                                        VStack(spacing: 8) {
                                            Text(formatDate(slot.start))
                                                .font(.system(size: 18, weight: .bold, design: .rounded))
                                                .foregroundColor(.green)
                                            
                                            HStack(spacing: 8) {
                                                Image(systemName: "clock")
                                                    .font(.system(size: 16))
                                                    .foregroundColor(.green.opacity(0.8))
                                                
                                                Text("\(formatTime(slot.start)) - \(formatTime(slot.end))")
                                                    .font(.system(size: 16, design: .rounded))
                                                    .foregroundColor(.green.opacity(0.8))
                                            }
                                        }
                                        .padding(20)
                                        .background(Color.green.opacity(0.1))
                                        .cornerRadius(16)
                                        .overlay(
                                            RoundedRectangle(cornerRadius: 16)
                                                .stroke(Color.green.opacity(0.3), lineWidth: 1)
                                        )
                                    }
                                }
                                
                                Text("All participants have been notified")
                                    .font(.system(size: 14, design: .rounded))
                                    .foregroundColor(.white.opacity(0.8))
                                    .multilineTextAlignment(.center)
                            }
                            .padding(24)
                            .liquidGlass(cornerRadius: 24, opacity: 0.9)
                        }
                        
                        Spacer(minLength: 40)
                    }
                    .padding(.horizontal, 20)
                }
            }
        }
        .onAppear {
            loadPollResults()
        }
        .alert("Error", isPresented: $showError) {
            Button("OK", role: .cancel) {}
        } message: {
            Text(errorMessage)
        }
        .alert("Success", isPresented: $showSuccess) {
            Button("OK", role: .cancel) {
                onDateConfirmed(event.id)
            }
        } message: {
            Text("Event date confirmed successfully!")
        }
    }
    
    private var statusIcon: String {
        switch event.status {
        case .draft: return "doc"
        case .polling: return "chart.bar"
        case .confirmed: return "checkmark.circle"
        default: return "questionmark.circle"
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
        case .draft: return "Draft"
        case .polling: return "Polling"
        case .confirmed: return "Confirmed"
        default: return "Unknown"
        }
    }
    
    private func loadPollResults() {
        poll = repository.getPoll(eventId: event.id)
        
        if let poll = poll {
            slotScores = PollLogic.shared.getSlotScores(poll: poll, slots: event.proposedSlots)
            
            if let bestResult = PollLogic.shared.getBestSlotWithScore(poll: poll, slots: event.proposedSlots) {
                bestSlot = bestResult.first
            }
        }
    }
    
    private func confirmDate() {
        guard let bestSlot = bestSlot else { return }
        
        isLoading = true
        
        let result = repository.updateEventStatus(
            id: event.id,
            status: EventStatus.confirmed,
            finalDate: bestSlot.id
        )
        
        if let success = result as? Bool, success {
            isLoading = false
            showSuccess = true
        } else {
            isLoading = false
            errorMessage = "Failed to confirm date"
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

struct SlotResultRow: View {
    let slot: TimeSlot?
    let score: PollLogic.SlotScore
    let isBest: Bool
    
    var body: some View {
        VStack(spacing: 12) {
            if let slot = slot {
                // Time slot info
                VStack(spacing: 6) {
                    Text(formatDate(slot.start))
                        .font(.system(size: 16, weight: .semibold, design: .rounded))
                        .foregroundColor(.white)
                    
                    HStack(spacing: 8) {
                        Image(systemName: "clock")
                            .font(.system(size: 14))
                            .foregroundColor(.white.opacity(0.6))
                        
                        Text("\(formatTime(slot.start)) - \(formatTime(slot.end))")
                            .font(.system(size: 14, design: .rounded))
                            .foregroundColor(.white.opacity(0.8))
                    }
                }
            }
            
            // Score breakdown
            HStack(spacing: 16) {
                 VoteCountView(label: "Yes", count: Int(score.yesCount), color: .green)
                 VoteCountView(label: "Maybe", count: Int(score.maybeCount), color: .orange)
                 VoteCountView(label: "No", count: Int(score.noCount), color: .red)
                
                Spacer()
                
                VStack(spacing: 2) {
                    Text("Score")
                        .font(.system(size: 12, design: .rounded))
                        .foregroundColor(.white.opacity(0.6))
                    
                    Text("\(score.totalScore)")
                        .font(.system(size: 18, weight: .bold, design: .rounded))
                        .foregroundColor(isBest ? .green : .white)
                }
            }
        }
        .padding(16)
        .background(
            ZStack {
                RoundedRectangle(cornerRadius: 12)
                    .fill(isBest ? Color.green.opacity(0.1) : Color.white.opacity(0.1))
                RoundedRectangle(cornerRadius: 12)
                    .stroke(isBest ? Color.green.opacity(0.3) : Color.white.opacity(0.2), lineWidth: 1)
            }
        )
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

struct VoteCountView: View {
    let label: String
    let count: Int
    let color: Color
    
    var body: some View {
        VStack(spacing: 4) {
            ZStack {
                Circle()
                    .fill(color.opacity(0.2))
                    .frame(width: 32, height: 32)
                
                Text("\(count)")
                    .font(.system(size: 14, weight: .bold, design: .rounded))
                    .foregroundColor(color)
            }
            
            Text(label)
                .font(.system(size: 10, design: .rounded))
                .foregroundColor(.white.opacity(0.8))
        }
    }
}

struct PollResultsView_Previews: PreviewProvider {
    static var previews: some View {
        let sampleSlots = [
            TimeSlot(
                id: "slot-1",
                start: ISO8601DateFormatter().string(from: Date().addingTimeInterval(24 * 60 * 60)),
                end: ISO8601DateFormatter().string(from: Date().addingTimeInterval(26 * 60 * 60)),
                timezone: "America/New_York"
            )
        ]
        
        let sampleEvent = Event(
            id: "sample-event",
            title: "Team Meeting",
            description: "Weekly sync meeting",
            organizerId: "organizer-1",
            participants: ["alice@example.com", "bob@example.com"],
            proposedSlots: sampleSlots,
            deadline: ISO8601DateFormatter().string(from: Date().addingTimeInterval(7 * 24 * 60 * 60)),
            status: EventStatus.polling,
            finalDate: nil
        )
        
        PollResultsView(
            event: sampleEvent,
            repository: EventRepository(),
            onDateConfirmed: { _ in }
        )
    }
}