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
        NavigationStack {
            ScrollView {
                VStack(alignment: .center, spacing: 16) {
                    Text("Create Event")
                        .font(.system(size: 32, weight: .bold))
                        .padding(.bottom, 8)
                    
                    // Title Input
                    TextField("Event Title", text: $title)
                        .textFieldStyle(.roundedBorder)
                        .padding(.horizontal)
                    
                    // Description Input
                    TextEditor(text: $description)
                        .frame(height: 100)
                        .border(Color.gray.opacity(0.3), width: 1)
                        .cornerRadius(6)
                        .padding(.horizontal)
                        .overlay(
                            VStack {
                                HStack {
                                    Text("Description")
                                        .font(.caption)
                                        .foregroundColor(.gray)
                                        .padding(8)
                                    Spacer()
                                }
                                Spacer()
                            }
                            .allowsHitTesting(false)
                        )
                    
                    // Deadline Input
                    TextField("Deadline (ISO 8601, e.g., 2025-12-25T18:00:00Z)", text: $deadline)
                        .textFieldStyle(.roundedBorder)
                        .padding(.horizontal)
                    
                    Divider()
                        .padding(.vertical, 8)
                    
                    // Time Slots Section
                    VStack(alignment: .leading, spacing: 12) {
                        Text("Proposed Time Slots")
                            .font(.system(size: 16, weight: .semibold))
                        
                        HStack(spacing: 8) {
                            TextField("Start", text: $slotStart)
                                .textFieldStyle(.roundedBorder)
                            
                            TextField("End", text: $slotEnd)
                                .textFieldStyle(.roundedBorder)
                            
                            Button(action: addSlot) {
                                Text("Add")
                                    .frame(maxWidth: .infinity)
                            }
                            .buttonStyle(.bordered)
                        }
                        
                        // Display Added Slots
                        if !slots.isEmpty {
                            Text("Added Slots (\(slots.count))")
                                .font(.system(size: 14, weight: .medium))
                                .padding(.top, 8)
                            
                            ForEach(slots, id: \.id) { slot in
                                SlotCard(
                                    slot: slot,
                                    onRemove: { removeSlot(slot) }
                                )
                            }
                        }
                    }
                    .padding(.horizontal)
                    
                    Divider()
                        .padding(.vertical, 8)
                    
                    // Error Display
                    if showError {
                        VStack(alignment: .leading) {
                            Text(errorMessage)
                                .font(.caption)
                                .foregroundColor(.red)
                        }
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(12)
                        .background(Color.red.opacity(0.1))
                        .cornerRadius(6)
                        .padding(.horizontal)
                    }
                    
                    // Create Button
                    Button(action: createEvent) {
                        Text("Create Event")
                            .frame(maxWidth: .infinity)
                            .padding(12)
                    }
                    .buttonStyle(.borderedProminent)
                    .disabled(slots.isEmpty)
                    .padding(.horizontal)
                }
                .padding(.vertical, 16)
            }
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
        // Validate inputs
        if title.isEmpty {
            errorMessage = "Event title is required"
            showError = true
            return
        }
        
        if deadline.isEmpty {
            errorMessage = "Deadline is required"
            showError = true
            return
        }
        
        if slots.isEmpty {
            errorMessage = "At least one time slot is required"
            showError = true
            return
        }
        
        let event = Event(
            id: "event-\(Int.random(in: 1000000...9999999))",
            title: title,
            description: description,
            organizerId: "organizer-1", // TODO: Get from auth
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
        HStack {
            VStack(alignment: .leading, spacing: 4) {
                Text(slot.start)
                    .font(.caption)
                Text("→")
                    .font(.caption)
                Text(slot.end)
                    .font(.caption)
            }
            
            Spacer()
            
            Button(action: onRemove) {
                Text("Remove")
                    .font(.caption)
            }
            .buttonStyle(.bordered)
        }
        .padding(12)
        .background(Color.gray.opacity(0.1))
        .cornerRadius(6)
    }
}

#Preview {
    EventCreationView(
        onEventCreated: { _ in },
        onNavigateToParticipants: { _ in }
    )
}
