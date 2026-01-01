import SwiftUI
import Shared

/// Time slot input with flexible time-of-day selection.
/// Mirrors Android's TimeSlotInput with Liquid Glass design.
///
/// Features:
/// - Menu selector for TimeOfDay (ALL_DAY, MORNING, AFTERNOON, EVENING, SPECIFIC)
/// - Conditional DatePicker for start/end times (shown only for SPECIFIC)
/// - Timezone picker with common timezones
/// - Context-sensitive help text
/// - Liquid Glass styling
/// - VoiceOver accessibility
///
/// Example:
/// ```swift
/// TimeSlotInput(
///     timeSlot: $timeSlot,
///     enabled: true
/// )
/// ```
struct TimeSlotInput: View {
    @Binding var timeSlot: Shared.TimeSlot?
    var enabled: Bool = true
    
    // Local state
    @State private var timeOfDay: Shared.TimeOfDay
    @State private var startDate: Date
    @State private var endDate: Date
    @State private var timezone: String
    
    // UI state
    @State private var showStartPicker: Bool = false
    @State private var showEndPicker: Bool = false
    
    // Common timezones
    private let commonTimezones = [
        "UTC",
        "Europe/Paris",
        "Europe/London",
        "America/New_York",
        "America/Los_Angeles",
        "Asia/Tokyo",
        "Australia/Sydney"
    ]
    
    // MARK: - Initialization
    
    init(timeSlot: Binding<Shared.TimeSlot?>, enabled: Bool = true) {
        self._timeSlot = timeSlot
        self.enabled = enabled
        
        // Initialize local state from binding
        let slot = timeSlot.wrappedValue
        _timeOfDay = State(initialValue: slot?.timeOfDay ?? .specific)
        _timezone = State(initialValue: slot?.timezone ?? TimeZone.current.identifier)
        
        // Parse dates from ISO8601 strings or use defaults
        let iso8601Formatter = ISO8601DateFormatter()
        if let startString = slot?.start, let parsedStart = iso8601Formatter.date(from: startString) {
            _startDate = State(initialValue: parsedStart)
        } else {
            _startDate = State(initialValue: Date())
        }
        
        if let endString = slot?.end, let parsedEnd = iso8601Formatter.date(from: endString) {
            _endDate = State(initialValue: parsedEnd)
        } else {
            _endDate = State(initialValue: Date().addingTimeInterval(3600)) // +1 hour
        }
    }
    
    // MARK: - Body
    
    var body: some View {
        LiquidGlassCard(style: .regular, padding: 20) {
            VStack(alignment: .leading, spacing: 16) {
                // Header
                HStack(spacing: 8) {
                    Image(systemName: "clock.fill")
                        .font(.system(size: 20, weight: .medium))
                        .foregroundColor(.blue)
                    
                    Text("Time Preference")
                        .font(.headline)
                        .foregroundColor(.primary)
                }
                .accessibilityElement(children: .combine)
                .accessibilityLabel("Time Preference")
                
                // TimeOfDay Menu
                VStack(alignment: .leading, spacing: 8) {
                    Text("When?")
                        .font(.subheadline.weight(.medium))
                        .foregroundColor(.primary)
                    
                    Menu {
                        ForEach([Shared.TimeOfDay.allDay, .morning, .afternoon, .evening, .specific], id: \.hashValue) { tod in
                            Button {
                                withAnimation(.easeInOut(duration: 0.2)) {
                                    timeOfDay = tod
                                    updateTimeSlot()
                                }
                            } label: {
                                VStack(alignment: .leading, spacing: 2) {
                                    HStack {
                                        Text(timeOfDayDisplayName(tod))
                                        if tod.hashValue == timeOfDay.hashValue {
                                            Spacer()
                                            Image(systemName: "checkmark")
                                                .foregroundColor(.blue)
                                        }
                                    }
                                    
                                    if let subtitle = timeOfDaySubtitle(tod) {
                                        Text(subtitle)
                                            .font(.caption)
                                            .foregroundColor(.secondary)
                                    }
                                }
                            }
                        }
                    } label: {
                        HStack {
                            Text(timeOfDayDisplayName(timeOfDay))
                                .font(.body)
                                .foregroundColor(.primary)
                            
                            Spacer()
                            
                            Image(systemName: "chevron.up.chevron.down")
                                .font(.system(size: 14, weight: .medium))
                                .foregroundColor(.secondary)
                        }
                        .padding(12)
                        .background(.ultraThinMaterial)
                        .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
                        .overlay(
                            RoundedRectangle(cornerRadius: 10, style: .continuous)
                                .stroke(Color.primary.opacity(0.1), lineWidth: 1)
                        )
                    }
                    .disabled(!enabled)
                    .accessibilityLabel("Time of day")
                    .accessibilityValue(timeOfDayDisplayName(timeOfDay))
                    .accessibilityHint("Select when the event should occur")
                }
                
                // Specific time pickers (conditional)
                if timeOfDay == .specific {
                    VStack(spacing: 12) {
                        // Start Time
                        VStack(alignment: .leading, spacing: 8) {
                            Text("Start Time")
                                .font(.subheadline.weight(.medium))
                                .foregroundColor(.primary)
                            
                            Button {
                                showStartPicker.toggle()
                            } label: {
                                HStack {
                                    Image(systemName: "calendar")
                                        .font(.system(size: 16, weight: .medium))
                                        .foregroundColor(.secondary)
                                        .frame(width: 20)
                                    
                                    Text(formattedDate(startDate))
                                        .font(.body)
                                        .foregroundColor(.primary)
                                    
                                    Spacer()
                                    
                                    Image(systemName: "chevron.right")
                                        .font(.system(size: 12, weight: .medium))
                                        .foregroundColor(.secondary)
                                }
                                .padding(12)
                                .background(.ultraThinMaterial)
                                .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
                                .overlay(
                                    RoundedRectangle(cornerRadius: 10, style: .continuous)
                                        .stroke(Color.primary.opacity(0.1), lineWidth: 1)
                                )
                            }
                            .disabled(!enabled)
                            .accessibilityLabel("Start Time")
                            .accessibilityValue(formattedDate(startDate))
                            .accessibilityHint("Tap to change start time")
                            
                            if showStartPicker {
                                DatePicker(
                                    "Start Time",
                                    selection: $startDate,
                                    displayedComponents: [.date, .hourAndMinute]
                                )
                                .datePickerStyle(.graphical)
                                .padding(.vertical, 8)
                                .onChange(of: startDate) { _ in
                                    updateTimeSlot()
                                }
                                .transition(.opacity.combined(with: .move(edge: .top)))
                            }
                        }
                        
                        // End Time
                        VStack(alignment: .leading, spacing: 8) {
                            Text("End Time")
                                .font(.subheadline.weight(.medium))
                                .foregroundColor(.primary)
                            
                            Button {
                                showEndPicker.toggle()
                            } label: {
                                HStack {
                                    Image(systemName: "calendar")
                                        .font(.system(size: 16, weight: .medium))
                                        .foregroundColor(.secondary)
                                        .frame(width: 20)
                                    
                                    Text(formattedDate(endDate))
                                        .font(.body)
                                        .foregroundColor(.primary)
                                    
                                    Spacer()
                                    
                                    Image(systemName: "chevron.right")
                                        .font(.system(size: 12, weight: .medium))
                                        .foregroundColor(.secondary)
                                }
                                .padding(12)
                                .background(.ultraThinMaterial)
                                .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
                                .overlay(
                                    RoundedRectangle(cornerRadius: 10, style: .continuous)
                                        .stroke(Color.primary.opacity(0.1), lineWidth: 1)
                                )
                            }
                            .disabled(!enabled)
                            .accessibilityLabel("End Time")
                            .accessibilityValue(formattedDate(endDate))
                            .accessibilityHint("Tap to change end time")
                            
                            if showEndPicker {
                                DatePicker(
                                    "End Time",
                                    selection: $endDate,
                                    displayedComponents: [.date, .hourAndMinute]
                                )
                                .datePickerStyle(.graphical)
                                .padding(.vertical, 8)
                                .onChange(of: endDate) { _ in
                                    updateTimeSlot()
                                }
                                .transition(.opacity.combined(with: .move(edge: .top)))
                            }
                        }
                    }
                    .transition(.opacity.combined(with: .move(edge: .top)))
                }
                
                // Timezone Selector
                VStack(alignment: .leading, spacing: 8) {
                    Text("Timezone")
                        .font(.subheadline.weight(.medium))
                        .foregroundColor(.primary)
                    
                    Menu {
                        ForEach(commonTimezones, id: \.self) { tz in
                            Button {
                                timezone = tz
                                updateTimeSlot()
                            } label: {
                                HStack {
                                    Text(tz)
                                    if tz == timezone {
                                        Spacer()
                                        Image(systemName: "checkmark")
                                            .foregroundColor(.blue)
                                    }
                                }
                            }
                        }
                    } label: {
                        HStack {
                            Text(timezone)
                                .font(.body)
                                .foregroundColor(.primary)
                            
                            Spacer()
                            
                            Image(systemName: "chevron.up.chevron.down")
                                .font(.system(size: 14, weight: .medium))
                                .foregroundColor(.secondary)
                        }
                        .padding(12)
                        .background(.ultraThinMaterial)
                        .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
                        .overlay(
                            RoundedRectangle(cornerRadius: 10, style: .continuous)
                                .stroke(Color.primary.opacity(0.1), lineWidth: 1)
                        )
                    }
                    .disabled(!enabled)
                    .accessibilityLabel("Timezone")
                    .accessibilityValue(timezone)
                    .accessibilityHint("Select timezone for this event")
                }
                
                // Helper Text
                HStack(alignment: .top, spacing: 8) {
                    Image(systemName: "lightbulb.fill")
                        .font(.system(size: 14))
                        .foregroundColor(.yellow)
                    
                    Text(helpText)
                        .font(.caption)
                        .foregroundColor(.secondary)
                        .fixedSize(horizontal: false, vertical: true)
                }
                .padding(12)
                .background(Color.blue.opacity(0.08))
                .clipShape(RoundedRectangle(cornerRadius: 8, style: .continuous))
            }
        }
        .animation(.easeInOut(duration: 0.25), value: timeOfDay.hashValue)
        .animation(.easeInOut(duration: 0.2), value: showStartPicker)
        .animation(.easeInOut(duration: 0.2), value: showEndPicker)
    }
    
    // MARK: - Helpers
    
    private func timeOfDayDisplayName(_ tod: Shared.TimeOfDay) -> String {
        switch tod {
        case Shared.TimeOfDay.allDay: return "All Day"
        case Shared.TimeOfDay.morning: return "Morning"
        case Shared.TimeOfDay.afternoon: return "Afternoon"
        case Shared.TimeOfDay.evening: return "Evening"
        case Shared.TimeOfDay.specific: return "Specific Time"
        default: return "Specific Time"
        }
    }
    
    private func timeOfDaySubtitle(_ tod: Shared.TimeOfDay) -> String? {
        switch tod {
        case Shared.TimeOfDay.morning: return "8am - 12pm"
        case Shared.TimeOfDay.afternoon: return "12pm - 6pm"
        case Shared.TimeOfDay.evening: return "6pm - 12am"
        default: return nil
        }
    }
    
    private var helpText: String {
        switch timeOfDay {
        case Shared.TimeOfDay.allDay:
            return "Flexible all-day event"
        case Shared.TimeOfDay.morning:
            return "Morning event (8am-12pm)"
        case Shared.TimeOfDay.afternoon:
            return "Afternoon event (12pm-6pm)"
        case Shared.TimeOfDay.evening:
            return "Evening event (6pm-12am)"
        case Shared.TimeOfDay.specific:
            return "Enter exact start and end times"
        default:
            return "Select a time preference above"
        }
    }
    
    private func formattedDate(_ date: Date) -> String {
        let formatter = DateFormatter()
        formatter.dateStyle = .medium
        formatter.timeStyle = .short
        return formatter.string(from: date)
    }
    
    private func updateTimeSlot() {
        let iso8601Formatter = ISO8601DateFormatter()
        
        let newSlot = Shared.TimeSlot(
            id: timeSlot?.id ?? "slot-\(Date().timeIntervalSince1970)",
            start: timeOfDay == .specific ? iso8601Formatter.string(from: startDate) : nil,
            end: timeOfDay == .specific ? iso8601Formatter.string(from: endDate) : nil,
            timezone: timezone,
            timeOfDay: timeOfDay
        )
        
        timeSlot = newSlot
    }
}

// MARK: - Previews

struct TimeSlotInput_Previews: PreviewProvider {
    static var previews: some View {
        ScrollView {
            VStack(spacing: 24) {
                // Preview 1: Specific time
                PreviewWrapper(
                    timeOfDay: .specific,
                    title: "Specific Time"
                )
                
                // Preview 2: All Day
                PreviewWrapper(
                    timeOfDay: .allDay,
                    title: "All Day"
                )
                
                // Preview 3: Morning
                PreviewWrapper(
                    timeOfDay: .morning,
                    title: "Morning"
                )
                
                // Preview 4: Afternoon
                PreviewWrapper(
                    timeOfDay: .afternoon,
                    title: "Afternoon"
                )
                
                // Preview 5: Evening
                PreviewWrapper(
                    timeOfDay: .evening,
                    title: "Evening"
                )
                
                // Preview 6: Disabled
                PreviewWrapper(
                    timeOfDay: .specific,
                    title: "Disabled State",
                    enabled: false
                )
            }
            .padding()
        }
        .background(Color(red: 0.95, green: 0.95, blue: 0.97))
    }
    
    /// Helper wrapper for previews with state
    private struct PreviewWrapper: View {
        @State var timeSlot: Shared.TimeSlot?
        let title: String
        var enabled: Bool = true
        
        init(timeOfDay: Shared.TimeOfDay, title: String, enabled: Bool = true) {
            let iso8601 = ISO8601DateFormatter()
            _timeSlot = State(initialValue: Shared.TimeSlot(
                id: UUID().uuidString,
                start: timeOfDay == .specific ? iso8601.string(from: Date()) : nil,
                end: timeOfDay == .specific ? iso8601.string(from: Date().addingTimeInterval(3600)) : nil,
                timezone: TimeZone.current.identifier,
                timeOfDay: timeOfDay
            ))
            self.title = title
            self.enabled = enabled
        }
        
        var body: some View {
            VStack(alignment: .leading, spacing: 8) {
                Text(title)
                    .font(.headline)
                    .foregroundColor(.secondary)
                    .padding(.horizontal, 4)
                
                TimeSlotInput(
                    timeSlot: $timeSlot,
                    enabled: enabled
                )
            }
        }
    }
}
