import SwiftUI
import Shared

struct PollResultsView: View {
    let event: Event
    let repository: EventRepositoryInterface
    let userId: String
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
            // Premium dark background
            Color.black
                .ignoresSafeArea()
            
            VStack(spacing: 0) {
                // Header - Large Typography
                VStack(spacing: 4) {
                    Text("Poll Results")
                        .font(.system(size: 13, weight: .medium, design: .rounded))
                        .foregroundColor(.white.opacity(0.5))
                        .textCase(.uppercase)
                        .tracking(1.2)
                    
                    Text(event.title)
                        .font(.system(size: 34, weight: .bold, design: .rounded))
                        .foregroundColor(.white)
                        .multilineTextAlignment(.center)
                }
                .padding(.top, 60)
                .padding(.horizontal, 20)
                
                ScrollView {
                    VStack(spacing: 24) {
                        // Event Status Card - Premium Monochrome
                        VStack(spacing: 20) {
                            HStack(alignment: .center, spacing: 12) {
                                Image(systemName: statusIcon)
                                    .font(.system(size: 28, weight: .medium))
                                    .foregroundColor(statusColor)
                                
                                VStack(alignment: .leading, spacing: 2) {
                                    Text("Status")
                                        .font(.system(size: 13, weight: .medium))
                                        .foregroundColor(.white.opacity(0.5))
                                        .textCase(.uppercase)
                                        .tracking(0.5)
                                    
                                    Text(statusText)
                                        .font(.system(size: 22, weight: .semibold, design: .rounded))
                                        .foregroundColor(.white)
                                }
                                
                                Spacer()
                                
                                // Status Badge
                                if event.status == EventStatus.polling {
                                    HStack(spacing: 4) {
                                        Circle()
                                            .fill(Color.red)
                                            .frame(width: 6, height: 6)
                                        
                                        Text("Active")
                                            .font(.system(size: 12, weight: .medium, design: .rounded))
                                            .foregroundColor(.white)
                                    }
                                    .padding(.horizontal, 12)
                                    .padding(.vertical, 6)
                                    .background(Color.white.opacity(0.1))
                                    .cornerRadius(20)
                                } else if event.status == EventStatus.confirmed {
                                    HStack(spacing: 4) {
                                        Circle()
                                            .fill(Color.red)
                                            .frame(width: 6, height: 6)
                                        
                                        Text("Confirmed")
                                            .font(.system(size: 12, weight: .medium, design: .rounded))
                                            .foregroundColor(.white)
                                    }
                                    .padding(.horizontal, 12)
                                    .padding(.vertical, 6)
                                    .background(Color.white.opacity(0.1))
                                    .cornerRadius(20)
                                }
                            }
                            
                            if let description = event.description as? String, !description.isEmpty {
                                Divider()
                                    .background(Color.white.opacity(0.1))
                                
                                Text(description)
                                    .font(.system(size: 15, design: .rounded))
                                    .foregroundColor(.white.opacity(0.7))
                                    .multilineTextAlignment(.leading)
                                    .frame(maxWidth: .infinity, alignment: .leading)
                            }
                            
                            Divider()
                                .background(Color.white.opacity(0.1))
                            
                            HStack(spacing: 24) {
                                VStack(alignment: .leading, spacing: 4) {
                                    Text("PARTICIPANTS")
                                        .font(.system(size: 11, weight: .medium))
                                        .foregroundColor(.white.opacity(0.5))
                                        .tracking(0.5)
                                    
                                    HStack(spacing: 6) {
                                        Image(systemName: "person.2.fill")
                                            .font(.system(size: 14))
                                            .foregroundColor(.white.opacity(0.9))
                                        
                                        Text("\(event.participants.count)")
                                            .font(.system(size: 17, weight: .semibold, design: .rounded))
                                            .foregroundColor(.white)
                                    }
                                }
                                
                                Spacer()
                                
                                VStack(alignment: .trailing, spacing: 4) {
                                    Text("DEADLINE")
                                        .font(.system(size: 11, weight: .medium))
                                        .foregroundColor(.white.opacity(0.5))
                                        .tracking(0.5)
                                    
                                    HStack(spacing: 6) {
                                        Text(formatDeadline(event.deadline))
                                            .font(.system(size: 17, weight: .semibold, design: .rounded))
                                            .foregroundColor(.white)
                                        
                                        Image(systemName: "clock.fill")
                                            .font(.system(size: 14))
                                            .foregroundColor(.white.opacity(0.9))
                                    }
                                }
                            }
                        }
                        .padding(28)
                        .liquidGlass(cornerRadius: 28, opacity: 0.95)
                        
                        if event.status == EventStatus.polling {
                            // Poll Results Card - Premium Design
                            VStack(spacing: 24) {
                                HStack(alignment: .center) {
                                    VStack(alignment: .leading, spacing: 2) {
                                        Text("VOTING RESULTS")
                                            .font(.system(size: 11, weight: .medium))
                                            .foregroundColor(.white.opacity(0.5))
                                            .tracking(0.5)
                                        
                                        Text("Time Preferences")
                                            .font(.system(size: 28, weight: .bold, design: .rounded))
                                            .foregroundColor(.white)
                                    }
                                    
                                    Spacer()
                                    
                                    if let best = bestSlot {
                                        VStack(alignment: .trailing, spacing: 2) {
                                            Text("BEST SLOT")
                                                .font(.system(size: 11, weight: .medium))
                                                .foregroundColor(.red.opacity(0.9))
                                                .tracking(0.5)
                                            
                                            Text(formatTime(best.start))
                                                .font(.system(size: 17, weight: .semibold, design: .rounded))
                                                .foregroundColor(.red)
                                        }
                                    }
                                }
                                
                                if slotScores.isEmpty {
                                    VStack(spacing: 20) {
                                        Image(systemName: "chart.bar.xaxis")
                                            .font(.system(size: 56, weight: .thin))
                                            .foregroundColor(.white.opacity(0.2))
                                            .padding(.top, 20)
                                        
                                        VStack(spacing: 8) {
                                            Text("No votes yet")
                                                .font(.system(size: 20, weight: .semibold, design: .rounded))
                                                .foregroundColor(.white.opacity(0.9))
                                            
                                            Text("Waiting for participants to vote")
                                                .font(.system(size: 15, design: .rounded))
                                                .foregroundColor(.white.opacity(0.5))
                                                .multilineTextAlignment(.center)
                                        }
                                    }
                                    .frame(maxWidth: .infinity)
                                    .padding(.vertical, 40)
                                } else {
                                    VStack(spacing: 12) {
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
                            .padding(28)
                            .liquidGlass(cornerRadius: 28, opacity: 0.95)
                            
                            // Confirm Date Button - Premium Red Accent
                             if let _ = bestSlot, !slotScores.isEmpty {
                                VStack(spacing: 16) {
                                    Text("Ready to finalize the event?")
                                        .font(.system(size: 15, weight: .medium, design: .rounded))
                                        .foregroundColor(.white.opacity(0.7))
                                        .multilineTextAlignment(.center)
                                    
                                    Button(action: {
                                        Task {
                                            await confirmDate()
                                        }
                                    }) {
                                        HStack(spacing: 8) {
                                            if isLoading {
                                                ProgressView()
                                                    .progressViewStyle(CircularProgressViewStyle(tint: .white))
                                            } else {
                                                Text("Confirm Final Date")
                                                    .font(.system(size: 17, weight: .semibold, design: .rounded))
                                                
                                                Image(systemName: "checkmark.circle.fill")
                                                    .font(.system(size: 17, weight: .semibold))
                                            }
                                        }
                                        .foregroundColor(.white)
                                        .frame(maxWidth: .infinity)
                                        .frame(height: 56)
                                        .background(Color.red)
                                        .cornerRadius(16)
                                        .shadow(color: Color.red.opacity(0.3), radius: 20, x: 0, y: 10)
                                    }
                                    .disabled(isLoading)
                                    .opacity(isLoading ? 0.6 : 1.0)
                                }
                                .padding(.horizontal, 20)
                            }
                        } else if event.status == EventStatus.confirmed, let finalDate = event.finalDate {
                            // Confirmed Date Card - Premium Success State
                            VStack(spacing: 28) {
                                HStack(spacing: 12) {
                                    Image(systemName: "checkmark.circle.fill")
                                        .font(.system(size: 32, weight: .medium))
                                        .foregroundColor(.red)
                                    
                                    VStack(alignment: .leading, spacing: 2) {
                                        Text("CONFIRMED")
                                            .font(.system(size: 11, weight: .medium))
                                            .foregroundColor(.red.opacity(0.9))
                                            .tracking(0.5)
                                        
                                        Text("Event Finalized")
                                            .font(.system(size: 24, weight: .bold, design: .rounded))
                                            .foregroundColor(.white)
                                    }
                                    
                                    Spacer()
                                }
                                
                                if let slot = event.proposedSlots.first(where: { $0.id == finalDate }) {
                                    VStack(spacing: 16) {
                                        Divider()
                                            .background(Color.white.opacity(0.1))
                                        
                                        VStack(spacing: 8) {
                                            Text("FINAL DATE & TIME")
                                                .font(.system(size: 11, weight: .medium))
                                                .foregroundColor(.white.opacity(0.5))
                                                .tracking(0.5)
                                            
                                            Text(formatDate(slot.start))
                                                .font(.system(size: 28, weight: .bold, design: .rounded))
                                                .foregroundColor(.white)
                                            
                                            HStack(spacing: 8) {
                                                Image(systemName: "clock.fill")
                                                    .font(.system(size: 15))
                                                    .foregroundColor(.white.opacity(0.7))
                                                
                                                Text("\(formatTime(slot.start)) – \(formatTime(slot.end))")
                                                    .font(.system(size: 17, weight: .medium, design: .rounded))
                                                    .foregroundColor(.white.opacity(0.9))
                                            }
                                        }
                                        .frame(maxWidth: .infinity)
                                        .padding(.vertical, 20)
                                        .background(Color.white.opacity(0.05))
                                        .cornerRadius(16)
                                        
                                        Divider()
                                            .background(Color.white.opacity(0.1))
                                        
                                        HStack(spacing: 8) {
                                            Image(systemName: "envelope.fill")
                                                .font(.system(size: 14))
                                                .foregroundColor(.white.opacity(0.5))
                                            
                                            Text("All participants have been notified")
                                                .font(.system(size: 14, design: .rounded))
                                                .foregroundColor(.white.opacity(0.6))
                                        }
                                    }
                                }
                            }
                            .padding(28)
                            .liquidGlass(cornerRadius: 28, opacity: 0.95)
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
        case .draft: return .white.opacity(0.6)
        case .polling: return .red
        case .confirmed: return .red
        default: return .white.opacity(0.4)
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
    
    private func confirmDate() async {
        guard let bestSlot = bestSlot else { return }

        // Check if user is organizer
        guard repository.isOrganizer(eventId: event.id, userId: userId) else {
            errorMessage = "Only the organizer can confirm the date"
            showError = true
            return
        }

        isLoading = true

        do {
            let result = try await repository.updateEventStatus(
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
        } catch {
            isLoading = false
            errorMessage = error.localizedDescription
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
        VStack(spacing: 16) {
            if let slot = slot {
                // Time slot info - Large Typography
                VStack(spacing: 6) {
                    Text(formatDate(slot.start))
                        .font(.system(size: 20, weight: .bold, design: .rounded))
                        .foregroundColor(.white)
                    
                    HStack(spacing: 6) {
                        Image(systemName: "clock.fill")
                            .font(.system(size: 13))
                            .foregroundColor(.white.opacity(0.5))
                        
                        Text("\(formatTime(slot.start)) – \(formatTime(slot.end))")
                            .font(.system(size: 15, weight: .medium, design: .rounded))
                            .foregroundColor(.white.opacity(0.7))
                    }
                }
            }
            
            Divider()
                .background(Color.white.opacity(0.1))
            
            // Score breakdown - Premium Layout
            HStack(spacing: 20) {
                VoteCountView(label: "Yes", count: Int(score.yesCount), color: .white)
                VoteCountView(label: "Maybe", count: Int(score.maybeCount), color: .white.opacity(0.6))
                VoteCountView(label: "No", count: Int(score.noCount), color: .white.opacity(0.3))
                
                Spacer()
                
                VStack(alignment: .trailing, spacing: 4) {
                    Text("SCORE")
                        .font(.system(size: 10, weight: .medium))
                        .foregroundColor(.white.opacity(0.5))
                        .tracking(0.5)
                    
                    Text("\(score.totalScore)")
                        .font(.system(size: 26, weight: .bold, design: .rounded))
                        .foregroundColor(isBest ? .red : .white)
                }
            }
            
            // Best indicator
            if isBest {
                HStack(spacing: 6) {
                    Image(systemName: "star.fill")
                        .font(.system(size: 12))
                        .foregroundColor(.red)
                    
                    Text("Best Time Slot")
                        .font(.system(size: 13, weight: .medium, design: .rounded))
                        .foregroundColor(.red)
                }
                .padding(.horizontal, 12)
                .padding(.vertical, 6)
                .background(Color.red.opacity(0.1))
                .cornerRadius(12)
            }
        }
        .padding(20)
        .background(
            ZStack {
                RoundedRectangle(cornerRadius: 16)
                    .fill(isBest ? Color.white.opacity(0.08) : Color.white.opacity(0.03))
                RoundedRectangle(cornerRadius: 16)
                    .stroke(isBest ? Color.red.opacity(0.3) : Color.white.opacity(0.1), lineWidth: isBest ? 2 : 1)
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
        VStack(spacing: 6) {
            Text("\(count)")
                .font(.system(size: 20, weight: .bold, design: .rounded))
                .foregroundColor(color)
            
            Text(label)
                .font(.system(size: 11, weight: .medium))
                .foregroundColor(.white.opacity(0.5))
                .textCase(.uppercase)
                .tracking(0.3)
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
        
        let now = ISO8601DateFormatter().string(from: Date())
        let sampleEvent = Event(
            id: "sample-event",
            title: "Team Meeting",
            description: "Weekly sync meeting",
            organizerId: "organizer-1",
            participants: ["alice@example.com", "bob@example.com"],
            proposedSlots: sampleSlots,
            deadline: ISO8601DateFormatter().string(from: Date().addingTimeInterval(7 * 24 * 60 * 60)),
            status: EventStatus.polling,
            finalDate: nil,
            createdAt: now,
            updatedAt: now
        )
        
        PollResultsView(
            event: sampleEvent,
            repository: EventRepository(),
            userId: "preview-user",
            onDateConfirmed: { _ in }
        )
    }
}