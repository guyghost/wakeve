import Foundation
import Shared

struct EventTimeSlotInput: Equatable {
    let start: String
    let end: String?
    let timeOfDay: Shared.TimeOfDay

    init(
        start: String,
        end: String? = nil,
        timeOfDay: Shared.TimeOfDay = .specific
    ) {
        self.start = start
        self.end = end
        self.timeOfDay = timeOfDay
    }
}

/// Builds the slot input saved by the creation wizard from its date/time pickers.
///
/// An all-day slot spans the whole local day (start of day -> start of next day),
/// exactly like `DraftDatesSheet`; it never keeps the time at which it was created.
enum EventSlotInputBuilder {
    /// Default start time of a new specific-time slot (an evening meetup), instead of "now".
    static let defaultStartHour = 19

    static func defaultStartTime(on day: Date, calendar: Calendar = .current) -> Date {
        calendar.date(bySettingHour: defaultStartHour, minute: 0, second: 0, of: day) ?? day
    }

    static func input(
        startDate: Date,
        startTime: Date,
        isAllDay: Bool,
        hasEndTime: Bool,
        endTime: Date,
        formatter: ISO8601DateFormatter,
        calendar: Calendar = .current
    ) -> EventTimeSlotInput {
        let start = startDateTime(startDate: startDate, startTime: startTime, isAllDay: isAllDay, calendar: calendar)
        let end = endDateTime(
            startDate: startDate,
            start: start,
            isAllDay: isAllDay,
            hasEndTime: hasEndTime,
            endTime: endTime,
            calendar: calendar
        )
        return EventTimeSlotInput(
            start: formatter.string(from: start),
            end: formatter.string(from: end),
            timeOfDay: isAllDay ? .allDay : .specific
        )
    }

    static func startDateTime(startDate: Date, startTime: Date, isAllDay: Bool, calendar: Calendar = .current) -> Date {
        if isAllDay {
            return calendar.startOfDay(for: startDate)
        }
        return combinedDateTime(date: startDate, time: startTime, calendar: calendar)
    }

    static func endDateTime(
        startDate: Date,
        start: Date,
        isAllDay: Bool,
        hasEndTime: Bool,
        endTime: Date,
        calendar: Calendar = .current
    ) -> Date {
        if isAllDay {
            return calendar.date(byAdding: .day, value: 1, to: start) ?? start.addingTimeInterval(24 * 3600)
        }

        if hasEndTime {
            let combinedEnd = combinedDateTime(date: startDate, time: endTime, calendar: calendar)
            if combinedEnd <= start {
                return calendar.date(byAdding: .day, value: 1, to: combinedEnd) ?? combinedEnd
            }
            return combinedEnd
        }

        return calendar.date(byAdding: .hour, value: 1, to: start) ?? start.addingTimeInterval(3600)
    }

    private static func combinedDateTime(date: Date, time: Date, calendar: Calendar) -> Date {
        let dateComponents = calendar.dateComponents([.year, .month, .day], from: date)
        let timeComponents = calendar.dateComponents([.hour, .minute], from: time)

        var components = DateComponents()
        components.year = dateComponents.year
        components.month = dateComponents.month
        components.day = dateComponents.day
        components.hour = timeComponents.hour
        components.minute = timeComponents.minute

        return calendar.date(from: components) ?? date
    }
}

enum EventTimeSlotFactory {
    static func proposedSlots(from selectedDate: String?) -> [TimeSlot] {
        guard let selectedDate, !selectedDate.isEmpty else {
            return []
        }

        return proposedSlots(from: [
            EventTimeSlotInput(start: selectedDate)
        ])
    }

    static func proposedSlots(from selectedSlotStarts: [String]) -> [TimeSlot] {
        proposedSlots(from: selectedSlotStarts.map { EventTimeSlotInput(start: $0) })
    }

    static func proposedSlots(from selectedSlots: [EventTimeSlotInput]) -> [TimeSlot] {
        selectedSlots
            .filter { !$0.start.isEmpty }
            .map { slot in
                TimeSlot(
                    id: "slot-\(UUID().uuidString.prefix(8))",
                    start: slot.start,
                    end: slot.end ?? defaultEndDate(for: slot.start),
                    timezone: TimeZone.current.identifier,
                    timeOfDay: slot.timeOfDay
                )
            }
    }

    private static func defaultEndDate(for start: String) -> String? {
        let formatter = ISO8601DateFormatter()
        guard let startDate = formatter.date(from: start),
              let endDate = Calendar.current.date(byAdding: .hour, value: 1, to: startDate) else {
            return nil
        }

        return formatter.string(from: endDate)
    }
}
