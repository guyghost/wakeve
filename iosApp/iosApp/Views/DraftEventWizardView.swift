import SwiftUI
import Shared

/// Multi-step wizard for creating an event in DRAFT phase.
/// Mirrors Android's DraftEventWizard with native iOS navigation.
///
/// Steps:
/// 1. Basic Info (title, description, event type)
/// 2. Participants Estimation (min/max/expected)
/// 3. Potential Locations
/// 4. Time Slots
///
/// Features:
/// - Auto-save on each step transition
/// - Step-by-step validation
/// - Progress indicator
/// - Previous/Next navigation
/// - Liquid Glass design
///
/// Example:
/// ```swift
/// DraftEventWizardView(
///     initialEvent: nil,
///     onSaveStep: { event in
///         repository.saveEvent(event)
///     },
///     onComplete: { event in
///         repository.createEvent(event)
///         dismiss()
///     },
///     onCancel: {
///         dismiss()
///     }
/// )
/// ```
struct DraftEventWizardView: View {
    var initialEvent: Shared.Event?
    var onSaveStep: (Shared.Event) -> Void
    var onComplete: (Shared.Event) -> Void
    var onCancel: () -> Void
    
    // Current step (0-3)
    @State private var currentStep: Int = 0
    
    // Step 1: Basic Info
    @State private var title: String
    @State private var description: String
    @State private var eventType: Shared.EventType
    @State private var eventTypeCustom: String
    
    // Step 2: Participants
    @State private var minParticipants: Int?
    @State private var maxParticipants: Int?
    @State private var expectedParticipants: Int?
    
    // Step 3: Locations
    @State private var locations: [Shared.PotentialLocation_] = []
    @State private var showLocationSheet: Bool = false
    
    // Step 4: Time Slots
    @State private var timeSlots: [Shared.TimeSlot] = []
    @State private var editingTimeSlot: Shared.TimeSlot?
    @State private var showTimeSlotSheet: Bool = false
    
    private let steps = ["Basic Info", "Participants", "Locations", "Time Slots"]
    
    // MARK: - Initialization
    
    init(
        initialEvent: Shared.Event?,
        onSaveStep: @escaping (Shared.Event) -> Void,
        onComplete: @escaping (Shared.Event) -> Void,
        onCancel: @escaping () -> Void
    ) {
        self.initialEvent = initialEvent
        self.onSaveStep = onSaveStep
        self.onComplete = onComplete
        self.onCancel = onCancel
        
        // Initialize state from initialEvent
        _title = State(initialValue: initialEvent?.title ?? "")
        _description = State(initialValue: initialEvent?.description_ ?? "")
        _eventType = State(initialValue: initialEvent?.eventType ?? .other)
        _eventTypeCustom = State(initialValue: initialEvent?.eventTypeCustom ?? "")
        _minParticipants = State(initialValue: initialEvent?.minParticipants?.intValue)
        _maxParticipants = State(initialValue: initialEvent?.maxParticipants?.intValue)
        _expectedParticipants = State(initialValue: initialEvent?.expectedParticipants?.intValue)
        _timeSlots = State(initialValue: initialEvent?.proposedSlots ?? [])
    }
    
    // MARK: - Body
    
    var body: some View {
        NavigationView {
            VStack(spacing: 0) {
                // Progress Bar
                ProgressView(value: Double(currentStep + 1), total: Double(steps.count))
                    .progressViewStyle(.linear)
                    .tint(.blue)
                
                // Step Indicator
                HStack {
                    Text("Step \(currentStep + 1) of \(steps.count): \(steps[currentStep])")
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                    
                    Spacer()
                }
                .padding(.horizontal)
                .padding(.vertical, 12)
                .background(Color(.systemBackground))
                
                Divider()
                
                // Step Content
                TabView(selection: $currentStep) {
                    step1BasicInfo.tag(0)
                    step2Participants.tag(1)
                    step3Locations.tag(2)
                    step4TimeSlots.tag(3)
                }
                .tabViewStyle(.page(indexDisplayMode: .never))
                .animation(.easeInOut, value: currentStep)
                
                Divider()
                
                // Navigation Buttons
                HStack(spacing: 12) {
                    if currentStep > 0 {
                        Button {
                            withAnimation {
                                currentStep -= 1
                            }
                        } label: {
                            HStack {
                                Image(systemName: "chevron.left")
                                    .font(.system(size: 14, weight: .semibold))
                                Text("Previous")
                            }
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 14)
                            .background(Color.secondary.opacity(0.1))
                            .foregroundColor(.primary)
                            .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                        }
                    }
                    
                    if currentStep < steps.count - 1 {
                        Button {
                            if isStepValid(currentStep) {
                                onSaveStep(buildEvent())
                                withAnimation {
                                    currentStep += 1
                                }
                            }
                        } label: {
                            HStack {
                                Text("Next")
                                Image(systemName: "chevron.right")
                                    .font(.system(size: 14, weight: .semibold))
                            }
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 14)
                            .background(isStepValid(currentStep) ? Color.blue : Color.gray)
                            .foregroundColor(.white)
                            .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                        }
                        .disabled(!isStepValid(currentStep))
                    } else {
                        Button {
                            if isStepValid(currentStep) {
                                onComplete(buildEvent())
                            }
                        } label: {
                            HStack {
                                Image(systemName: "checkmark")
                                    .font(.system(size: 14, weight: .semibold))
                                Text("Create Event")
                            }
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 14)
                            .background(isStepValid(currentStep) ? Color.green : Color.gray)
                            .foregroundColor(.white)
                            .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                        }
                        .disabled(!isStepValid(currentStep))
                    }
                }
                .padding()
                .background(Color(.systemBackground))
            }
            .navigationTitle("Create Event")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Cancel") {
                        onCancel()
                    }
                }
            }
        }
    }
    
    // MARK: - Step 1: Basic Info
    
    private var step1BasicInfo: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 20) {
                Text("Tell us about your event")
                    .font(.title2.weight(.bold))
                    .padding(.horizontal)
                
                VStack(spacing: 16) {
                    // Title
                    VStack(alignment: .leading, spacing: 8) {
                        Text("Event Title")
                            .font(.subheadline.weight(.medium))
                        
                        TextField("e.g., Team Building Weekend", text: $title)
                            .padding(12)
                            .background(.ultraThinMaterial)
                            .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
                            .overlay(
                                RoundedRectangle(cornerRadius: 10, style: .continuous)
                                    .stroke(title.isEmpty ? Color.red.opacity(0.3) : Color.clear, lineWidth: 1)
                            )
                    }
                    
                    // Description
                    VStack(alignment: .leading, spacing: 8) {
                        Text("Description")
                            .font(.subheadline.weight(.medium))
                        
                        TextField("Describe your event...", text: $description, axis: .vertical)
                            .lineLimit(3...5)
                            .padding(12)
                            .background(.ultraThinMaterial)
                            .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
                            .overlay(
                                RoundedRectangle(cornerRadius: 10, style: .continuous)
                                    .stroke(description.isEmpty ? Color.red.opacity(0.3) : Color.clear, lineWidth: 1)
                            )
                    }
                    
                    // Event Type
                    EventTypePicker(
                        selectedType: $eventType,
                        customTypeValue: $eventTypeCustom,
                        enabled: true
                    )
                }
                .padding(.horizontal)
            }
            .padding(.vertical)
        }
    }
    
    // MARK: - Step 2: Participants
    
    private var step2Participants: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 20) {
                Text("How many people?")
                    .font(.title2.weight(.bold))
                    .padding(.horizontal)
                
                ParticipantsEstimationCard(
                    minParticipants: $minParticipants,
                    maxParticipants: $maxParticipants,
                    expectedParticipants: $expectedParticipants,
                    enabled: true
                )
                .padding(.horizontal)
            }
            .padding(.vertical)
        }
    }
    
    // MARK: - Step 3: Locations
    
    private var step3Locations: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 20) {
                Text("Where could it be?")
                    .font(.title2.weight(.bold))
                    .padding(.horizontal)
                
                Text("Add potential locations (optional)")
                    .font(.subheadline)
                    .foregroundColor(.secondary)
                    .padding(.horizontal)
                
                PotentialLocationsList(
                    locations: $locations,
                    onAddLocation: {
                        showLocationSheet = true
                    },
                    onRemoveLocation: { id in
                        locations.removeAll { $0.id == id }
                    },
                    enabled: true
                )
                .padding(.horizontal)
            }
            .padding(.vertical)
        }
        .sheet(isPresented: $showLocationSheet) {
            LocationInputSheet(
                eventId: initialEvent?.id ?? "temp-event",
                onDismiss: {
                    showLocationSheet = false
                },
                onConfirm: { location in
                    locations.append(location)
                    showLocationSheet = false
                }
            )
        }
    }
    
    // MARK: - Step 4: Time Slots
    
    private var step4TimeSlots: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 20) {
                Text("When should it happen?")
                    .font(.title2.weight(.bold))
                    .padding(.horizontal)
                
                Text("Add at least one time slot")
                    .font(.subheadline)
                    .foregroundColor(.secondary)
                    .padding(.horizontal)
                
                // Time Slots List
                if timeSlots.isEmpty {
                    VStack(spacing: 12) {
                        Image(systemName: "clock.badge.plus")
                            .font(.system(size: 48, weight: .light))
                            .foregroundColor(.secondary.opacity(0.4))
                        
                        Text("No time slots yet")
                            .font(.body.weight(.medium))
                            .foregroundColor(.secondary)
                        
                        Button {
                            editingTimeSlot = nil
                            showTimeSlotSheet = true
                        } label: {
                            HStack {
                                Image(systemName: "plus")
                                Text("Add Time Slot")
                            }
                            .frame(maxWidth: .infinity)
                            .padding()
                            .background(Color.blue)
                            .foregroundColor(.white)
                            .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                        }
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 40)
                    .padding(.horizontal)
                } else {
                    VStack(spacing: 12) {
                        ForEach(Array(timeSlots.enumerated()), id: \.element.id) { index, slot in
                            TimeSlotRow(slot: slot) {
                                timeSlots.remove(at: index)
                            }
                        }
                        
                        Button {
                            editingTimeSlot = nil
                            showTimeSlotSheet = true
                        } label: {
                            HStack {
                                Image(systemName: "plus")
                                Text("Add Another Time Slot")
                            }
                            .frame(maxWidth: .infinity)
                            .padding()
                            .background(Color.blue.opacity(0.1))
                            .foregroundColor(.blue)
                            .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                        }
                    }
                    .padding(.horizontal)
                }
            }
            .padding(.vertical)
        }
        .sheet(isPresented: $showTimeSlotSheet) {
            TimeSlotSheet(
                timeSlot: $editingTimeSlot,
                onDismiss: {
                    showTimeSlotSheet = false
                },
                onConfirm: { slot in
                    timeSlots.append(slot)
                    showTimeSlotSheet = false
                }
            )
        }
    }
    
    // MARK: - Helpers
    
    private func buildEvent() -> Shared.Event {
        let iso8601 = ISO8601DateFormatter()
        let now = iso8601.string(from: Date())
        
        return Shared.Event(
            id: initialEvent?.id ?? "event-\(Date().timeIntervalSince1970)",
            title: title,
            description: description,
            organizerId: initialEvent?.organizerId ?? "current-user",
            participants: initialEvent?.participants ?? [],
            proposedSlots: timeSlots,
            deadline: initialEvent?.deadline ?? now,
            status: .draft,
            finalDate: nil,
            createdAt: initialEvent?.createdAt ?? now,
            updatedAt: now,
            eventType: eventType,
            eventTypeCustom: eventType == .custom ? eventTypeCustom : nil,
            minParticipants: minParticipants.map { KotlinInt(value: Int32($0)) },
            maxParticipants: maxParticipants.map { KotlinInt(value: Int32($0)) },
            expectedParticipants: expectedParticipants.map { KotlinInt(value: Int32($0)) }
        )
    }
    
    private func isStepValid(_ step: Int) -> Bool {
        switch step {
        case 0: // Basic Info
            return !title.isEmpty && !description.isEmpty &&
                   (eventType != .custom || !eventTypeCustom.isEmpty)
        case 1: // Participants
            let minOk = minParticipants == nil || minParticipants! > 0
            let maxOk = maxParticipants == nil || maxParticipants! > 0
            let rangeOk = minParticipants == nil || maxParticipants == nil || maxParticipants! >= minParticipants!
            let expectedOk = expectedParticipants == nil || expectedParticipants! > 0
            return minOk && maxOk && rangeOk && expectedOk
        case 2: // Locations (optional)
            return true
        case 3: // Time Slots
            return !timeSlots.isEmpty
        default:
            return false
        }
    }
}

// MARK: - Time Slot Row

private struct TimeSlotRow: View {
    let slot: Shared.TimeSlot
    let onRemove: () -> Void
    
    var body: some View {
        HStack {
            VStack(alignment: .leading, spacing: 4) {
                Text(timeOfDayText)
                    .font(.body.weight(.medium))
                
                if let start = slot.start, let end = slot.end {
                    Text("\(formatDate(start)) - \(formatDate(end))")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
                
                Text(slot.timezone)
                    .font(.caption2)
                    .foregroundColor(.secondary)
            }
            
            Spacer()
            
            Button {
                onRemove()
            } label: {
                Image(systemName: "trash.fill")
                    .foregroundColor(.red)
            }
        }
        .padding()
        .background(.ultraThinMaterial)
        .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
    }
    
    private var timeOfDayText: String {
        switch slot.timeOfDay {
        case Shared.TimeOfDay.allDay: return "All Day"
        case Shared.TimeOfDay.morning: return "Morning (8am-12pm)"
        case Shared.TimeOfDay.afternoon: return "Afternoon (12pm-6pm)"
        case Shared.TimeOfDay.evening: return "Evening (6pm-12am)"
        case Shared.TimeOfDay.specific: return "Specific Time"
        default: return "Time Slot"
        }
    }
    
    private func formatDate(_ isoString: String) -> String {
        let formatter = ISO8601DateFormatter()
        guard let date = formatter.date(from: isoString) else { return isoString }
        
        let displayFormatter = DateFormatter()
        displayFormatter.dateStyle = .short
        displayFormatter.timeStyle = .short
        return displayFormatter.string(from: date)
    }
}

// MARK: - Time Slot Sheet

private struct TimeSlotSheet: View {
    @Binding var timeSlot: Shared.TimeSlot?
    var onDismiss: () -> Void
    var onConfirm: (Shared.TimeSlot) -> Void
    
    var body: some View {
        NavigationView {
            TimeSlotInput(
                timeSlot: $timeSlot,
                enabled: true
            )
            .padding()
            .navigationTitle("Add Time Slot")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Cancel") {
                        onDismiss()
                    }
                }
                
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Add") {
                        if let slot = timeSlot {
                            onConfirm(slot)
                        }
                    }
                    .disabled(timeSlot == nil)
                }
            }
        }
    }
}

// MARK: - Previews

struct DraftEventWizardView_Previews: PreviewProvider {
    static var previews: some View {
        DraftEventWizardView(
            initialEvent: nil,
            onSaveStep: { event in
                print("Step saved: \(event.title)")
            },
            onComplete: { event in
                print("Event created: \(event.title)")
            },
            onCancel: {
                print("Cancelled")
            }
        )
    }
}
