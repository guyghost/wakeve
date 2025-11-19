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
    
    let repository: EventRepository
    let onEventCreated: (String) -> Void
    
    var body: some View {
        ZStack {
            // Background gradient
            LinearGradient(
                gradient: Gradient(colors: [
                    Color.blue.opacity(0.1),
                    Color.purple.opacity(0.1),
                    Color.pink.opacity(0.1)
                ]),
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
            .ignoresSafeArea()
            
            ScrollView {
                VStack(spacing: 24) {
                    // Header
                    VStack(spacing: 8) {
                        Text("Create New Event")
                            .font(.system(size: 32, weight: .bold, design: .rounded))
                            .foregroundColor(.white)
                        
                        Text("Plan your collaborative event")
                            .font(.system(size: 16, weight: .medium, design: .rounded))
                            .foregroundColor(.white.opacity(0.8))
                    }
                    .padding(.top, 40)
                    
                    // Event Details Card
                    VStack(spacing: 20) {
                        VStack(alignment: .leading, spacing: 8) {
                            Text("Event Title")
                                .font(.system(size: 16, weight: .semibold, design: .rounded))
                                .foregroundColor(.white)
                            
                            TextField("Enter event title", text: $eventTitle)
                                .padding()
                                .background(Color.white.opacity(0.1))
                                .cornerRadius(12)
                                .foregroundColor(.white)
                                .font(.system(size: 16, design: .rounded))
                        }
                        
                        VStack(alignment: .leading, spacing: 8) {
                            Text("Description")
                                .font(.system(size: 16, weight: .semibold, design: .rounded))
                                .foregroundColor(.white)
                            
                            TextEditor(text: $eventDescription)
                                .frame(height: 100)
                                .padding(12)
                                .background(Color.white.opacity(0.1))
                                .cornerRadius(12)
                                .foregroundColor(.white)
                                .font(.system(size: 16, design: .rounded))
                        }
                        
                        VStack(alignment: .leading, spacing: 8) {
                            Text("Voting Deadline")
                                .font(.system(size: 16, weight: .semibold, design: .rounded))
                                .foregroundColor(.white)
                            
                            DatePicker(
                                "Select deadline",
                                selection: $deadline,
                                in: Date()...,
                                displayedComponents: [.date, .hourAndMinute]
                            )
                            .datePickerStyle(.compact)
                            .padding()
                            .background(Color.white.opacity(0.1))
                            .cornerRadius(12)
                            .foregroundColor(.white)
                        }
                    }
                    .padding(24)
                    .liquidGlass(cornerRadius: 24, opacity: 0.9)
                    
                    // Time Slots Card
                    VStack(spacing: 20) {
                        HStack {
                            Text("Time Slots")
                                .font(.system(size: 20, weight: .bold, design: .rounded))
                                .foregroundColor(.white)
                            
                            Spacer()
                            
                            Button(action: addTimeSlot) {
                                Image(systemName: "plus.circle.fill")
                                    .font(.system(size: 24))
                                    .foregroundColor(.blue)
                            }
                        }
                        
                        if timeSlots.isEmpty {
                            VStack(spacing: 16) {
                                Image(systemName: "calendar.badge.plus")
                                    .font(.system(size: 48))
                                    .foregroundColor(.white.opacity(0.6))
                                
                                Text("Add your first time slot")
                                    .font(.system(size: 16, weight: .medium, design: .rounded))
                                    .foregroundColor(.white.opacity(0.8))
                                
                                Text("Participants will vote on these options")
                                    .font(.system(size: 14, design: .rounded))
                                    .foregroundColor(.white.opacity(0.6))
                                    .multilineTextAlignment(.center)
                            }
                            .padding(.vertical, 40)
                        } else {
                            ForEach(timeSlots.indices, id: \.self) { index in
                                TimeSlotRow(
                                    timeSlot: $timeSlots[index],
                                    onDelete: { removeTimeSlot(at: index) }
                                )
                            }
                        }
                    }
                    .padding(24)
                    .liquidGlass(cornerRadius: 24, opacity: 0.9)
                    
                    // Create Button
                    Button(action: createEvent) {
                        ZStack {
                            if isLoading {
                                ProgressView()
                                    .progressViewStyle(CircularProgressViewStyle(tint: .white))
                            } else {
                                Text("Create Event")
                                    .font(.system(size: 18, weight: .bold, design: .rounded))
                                    .foregroundColor(.white)
                            }
                        }
                        .frame(maxWidth: .infinity)
                        .frame(height: 56)
                        .background(
                            LinearGradient(
                                gradient: Gradient(colors: [
                                    Color.blue.opacity(0.8),
                                    Color.purple.opacity(0.8)
                                ]),
                                startPoint: .leading,
                                endPoint: .trailing
                            )
                        )
                        .cornerRadius(28)
                        .shadow(color: Color.blue.opacity(0.3), radius: 10, x: 0, y: 5)
                    }
                    .disabled(eventTitle.isEmpty || timeSlots.isEmpty || isLoading)
                    .opacity(eventTitle.isEmpty || timeSlots.isEmpty || isLoading ? 0.6 : 1.0)
                    .padding(.horizontal, 24)
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
    
    private func createEvent() {
        guard !eventTitle.isEmpty, !timeSlots.isEmpty else { return }
        
        isLoading = true
        
        do {
            let event = Event(
                id: UUID().uuidString,
                title: eventTitle,
                description: eventDescription,
                organizerId: "organizer-1", // TODO: Get from auth
                participants: [],
                proposedSlots: timeSlots,
                deadline: ISO8601DateFormatter().string(from: deadline),
                status: EventStatus.draft,
                finalDate: nil
            )
            
            let result = repository.createEvent(event: event)
            
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
            VStack(alignment: .leading, spacing: 8) {
                DatePicker("Start", selection: $startDate, displayedComponents: [.date, .hourAndMinute])
                    .datePickerStyle(.compact)
                    .labelsHidden()
                
                DatePicker("End", selection: $endDate, displayedComponents: [.date, .hourAndMinute])
                    .datePickerStyle(.compact)
                    .labelsHidden()
            }
            
            Button(action: onDelete) {
                Image(systemName: "trash.circle.fill")
                    .font(.system(size: 24))
                    .foregroundColor(.red.opacity(0.8))
            }
        }
        .padding(16)
        .background(Color.white.opacity(0.1))
        .cornerRadius(12)
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
            timeSlot = TimeSlot(
                id: timeSlot.id,
                start: ISO8601DateFormatter().string(from: newValue),
                end: timeSlot.end,
                timezone: timeSlot.timezone
            )
        }
        .onChange(of: endDate) { newValue in
            timeSlot = TimeSlot(
                id: timeSlot.id,
                start: timeSlot.start,
                end: ISO8601DateFormatter().string(from: newValue),
                timezone: timeSlot.timezone
            )
        }
    }
}

struct EventCreationView_Previews: PreviewProvider {
    static var previews: some View {
        EventCreationView(
            repository: EventRepository(),
            onEventCreated: { _ in }
        )
    }
}