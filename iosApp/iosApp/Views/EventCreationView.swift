import SwiftUI
import Shared

struct EventCreationView: View {
    @State private var eventTitle = ""
    @State private var eventDescription = ""
    @State private var timeSlots: [TimeSlot] = []
    @State private var deadline = Date().addingTimeInterval(7 * 24 * 60 * 60) // 1 week from now
    @State private var isLoading = false
    @State private var errorMessage = ""
    @State private var showError = false

    let userId: String
    let repository: EventRepository
    let onEventCreated: (String) -> Void
    
    var body: some View {
        ZStack {
            // Premium dark background
            Color.black
                .ignoresSafeArea()
            
            ScrollView {
                VStack(spacing: 24) {
                    // Header - Large Typography
                    VStack(spacing: 4) {
                        Text("CREATE EVENT")
                            .font(.system(size: 13, weight: .medium, design: .rounded))
                            .foregroundColor(.white.opacity(0.5))
                            .textCase(.uppercase)
                            .tracking(1.2)
                        
                        Text("New Event")
                            .font(.system(size: 34, weight: .bold, design: .rounded))
                            .foregroundColor(.white)
                        
                        Text("Plan your collaborative event")
                            .font(.system(size: 15, design: .rounded))
                            .foregroundColor(.white.opacity(0.6))
                    }
                    .padding(.top, 60)
                    
                    // Event Details Card - Premium Design
                    VStack(spacing: 24) {
                        VStack(alignment: .leading, spacing: 8) {
                            Text("EVENT TITLE")
                                .font(.system(size: 11, weight: .medium))
                                .foregroundColor(.white.opacity(0.5))
                                .tracking(0.5)
                            
                            TextField("Enter event title", text: $eventTitle)
                                .padding(16)
                                .background(Color.white.opacity(0.05))
                                .cornerRadius(12)
                                .foregroundColor(.white)
                                .font(.system(size: 17, design: .rounded))
                        }
                        
                        VStack(alignment: .leading, spacing: 8) {
                            Text("DESCRIPTION")
                                .font(.system(size: 11, weight: .medium))
                                .foregroundColor(.white.opacity(0.5))
                                .tracking(0.5)
                            
                            TextEditor(text: $eventDescription)
                                .frame(height: 100)
                                .padding(12)
                                .background(Color.white.opacity(0.05))
                                .cornerRadius(12)
                                .foregroundColor(.white)
                                .font(.system(size: 16, design: .rounded))
                                .scrollContentBackground(.hidden)
                        }
                        
                        VStack(alignment: .leading, spacing: 8) {
                            Text("VOTING DEADLINE")
                                .font(.system(size: 11, weight: .medium))
                                .foregroundColor(.white.opacity(0.5))
                                .tracking(0.5)
                            
                            DatePicker(
                                "Select deadline",
                                selection: $deadline,
                                in: Date()...,
                                displayedComponents: [.date, .hourAndMinute]
                            )
                            .datePickerStyle(.compact)
                            .labelsHidden()
                            .padding(16)
                            .background(Color.white.opacity(0.05))
                            .cornerRadius(12)
                            .foregroundColor(.white)
                        }
                    }
                    .padding(28)
                    .liquidGlass(cornerRadius: 28, opacity: 0.95)
                    
                    // Time Slots Card - Premium Design
                    VStack(spacing: 24) {
                        HStack(alignment: .center) {
                            VStack(alignment: .leading, spacing: 2) {
                                Text("TIME SLOTS")
                                    .font(.system(size: 11, weight: .medium))
                                    .foregroundColor(.white.opacity(0.5))
                                    .tracking(0.5)
                                
                                Text("Proposed Times")
                                    .font(.system(size: 24, weight: .bold, design: .rounded))
                                    .foregroundColor(.white)
                            }
                            
                            Spacer()
                            
                            Button(action: addTimeSlot) {
                                HStack(spacing: 6) {
                                    Image(systemName: "plus")
                                        .font(.system(size: 14, weight: .semibold))
                                    Text("Add")
                                        .font(.system(size: 15, weight: .semibold, design: .rounded))
                                }
                                .foregroundColor(.white)
                                .padding(.horizontal, 16)
                                .padding(.vertical, 10)
                                .background(Color.red)
                                .cornerRadius(20)
                            }
                        }
                        
                        if timeSlots.isEmpty {
                            VStack(spacing: 20) {
                                Image(systemName: "calendar.badge.clock")
                                    .font(.system(size: 56, weight: .thin))
                                    .foregroundColor(.white.opacity(0.2))
                                    .padding(.top, 20)
                                
                                VStack(spacing: 8) {
                                    Text("No time slots yet")
                                        .font(.system(size: 20, weight: .semibold, design: .rounded))
                                        .foregroundColor(.white.opacity(0.9))
                                    
                                    Text("Add time options for participants to vote on")
                                        .font(.system(size: 15, design: .rounded))
                                        .foregroundColor(.white.opacity(0.5))
                                        .multilineTextAlignment(.center)
                                }
                            }
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 40)
                        } else {
                            VStack(spacing: 12) {
                                ForEach(timeSlots.indices, id: \.self) { index in
                                    TimeSlotRow(
                                        timeSlot: $timeSlots[index],
                                        onDelete: { removeTimeSlot(at: index) }
                                    )
                                }
                            }
                        }
                    }
                    .padding(28)
                    .liquidGlass(cornerRadius: 28, opacity: 0.95)
                    
                    // Create Button - Premium Red
                    Button {
                        Task {
                            await createEvent()
                        }
                    } label: {
                        HStack(spacing: 8) {
                            if isLoading {
                                ProgressView()
                                    .progressViewStyle(CircularProgressViewStyle(tint: .white))
                            } else {
                                Text("Create Event")
                                    .font(.system(size: 17, weight: .semibold, design: .rounded))
                                
                                Image(systemName: "arrow.right")
                                    .font(.system(size: 16, weight: .semibold))
                            }
                        }
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity)
                        .frame(height: 56)
                        .background(Color.red)
                        .cornerRadius(16)
                        .shadow(color: Color.red.opacity(0.3), radius: 20, x: 0, y: 10)
                    }
                    .disabled(eventTitle.isEmpty || timeSlots.isEmpty || isLoading)
                    .opacity(eventTitle.isEmpty || timeSlots.isEmpty || isLoading ? 0.4 : 1.0)
                    .padding(.horizontal, 20)
                    .padding(.bottom, 40)
                }
                .padding(.horizontal, 20)
            }
        }
        .alert("Error", isPresented: $showError) {
            Button("OK", role: .cancel) {}
        } message: {
            Text(errorMessage)
        }
    }
    
    private func addTimeSlot() {
        let newSlot = TimeSlot(
            id: UUID().uuidString,
            start: ISO8601DateFormatter().string(from: Date()),
            end: ISO8601DateFormatter().string(from: Date().addingTimeInterval(2 * 60 * 60)),
            timezone: TimeZone.current.identifier
        )
        timeSlots.append(newSlot)
    }
    
    private func removeTimeSlot(at index: Int) {
        timeSlots.remove(at: index)
    }
    
    private func createEvent() async {
        guard !eventTitle.isEmpty, !timeSlots.isEmpty else { return }
        
        isLoading = true
        
        do {
            let now = ISO8601DateFormatter().string(from: Date())
            let event = Event(
                id: UUID().uuidString,
                title: eventTitle,
                description: eventDescription,
                organizerId: userId,  // Authenticated user ID
                participants: [],
                proposedSlots: timeSlots,
                deadline: ISO8601DateFormatter().string(from: deadline),
                status: EventStatus.draft,
                finalDate: nil,
                createdAt: now,
                updatedAt: now
            )
            
            let result = try await repository.createEvent(event: event)
            
            if let createdEvent = result as? Event {
                isLoading = false
                onEventCreated(createdEvent.id)
            } else {
                isLoading = false
                errorMessage = "Failed to create event"
                showError = true
            }
        } catch {
            isLoading = false
            errorMessage = error.localizedDescription
            showError = true
        }
    }
}

struct TimeSlotRow: View {
    @Binding var timeSlot: TimeSlot
    let onDelete: () -> Void
    
    @State private var startDate = Date()
    @State private var endDate = Date().addingTimeInterval(2 * 60 * 60)
    
    var body: some View {
        HStack(spacing: 16) {
            VStack(alignment: .leading, spacing: 12) {
                VStack(alignment: .leading, spacing: 4) {
                    Text("START TIME")
                        .font(.system(size: 10, weight: .medium))
                        .foregroundColor(.white.opacity(0.5))
                        .tracking(0.3)
                    
                    DatePicker("Start", selection: $startDate, displayedComponents: [.date, .hourAndMinute])
                        .datePickerStyle(.compact)
                        .labelsHidden()
                }
                
                VStack(alignment: .leading, spacing: 4) {
                    Text("END TIME")
                        .font(.system(size: 10, weight: .medium))
                        .foregroundColor(.white.opacity(0.5))
                        .tracking(0.3)
                    
                    DatePicker("End", selection: $endDate, displayedComponents: [.date, .hourAndMinute])
                        .datePickerStyle(.compact)
                        .labelsHidden()
                }
            }
            
            Spacer()
            
            Button(action: onDelete) {
                Image(systemName: "xmark.circle.fill")
                    .font(.system(size: 24))
                    .foregroundColor(.white.opacity(0.3))
            }
        }
        .padding(16)
        .background(Color.white.opacity(0.03))
        .cornerRadius(12)
        .overlay(
            RoundedRectangle(cornerRadius: 12)
                .stroke(Color.white.opacity(0.1), lineWidth: 1)
        )
        .onAppear {
            // Initialize dates from timeSlot if available
            if let start = ISO8601DateFormatter().date(from: timeSlot.start) {
                startDate = start
            }
            if let end = ISO8601DateFormatter().date(from: timeSlot.end) {
                endDate = end
            }
        }
        .onChange(of: startDate) { newValue in
            Task { @MainActor in
                timeSlot = TimeSlot(
                    id: timeSlot.id,
                    start: ISO8601DateFormatter().string(from: newValue),
                    end: timeSlot.end,
                    timezone: timeSlot.timezone
                )
            }
        }
        .onChange(of: endDate) { newValue in
            Task { @MainActor in
                timeSlot = TimeSlot(
                    id: timeSlot.id,
                    start: timeSlot.start,
                    end: ISO8601DateFormatter().string(from: newValue),
                    timezone: timeSlot.timezone
                )
            }
        }
    }
}

struct EventCreationView_Previews: PreviewProvider {
    static var previews: some View {
        EventCreationView(
            userId: "preview-user-id",
            repository: EventRepository(),
            onEventCreated: { _ in }
        )
    }
}