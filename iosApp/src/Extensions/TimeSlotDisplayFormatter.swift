import Foundation
import Shared

/// Single source of truth for how a proposed time slot's hours are displayed.
///
/// An ALL_DAY slot has no meaningful hours: it is shown as its date plus the localized
/// "all day" label (`events.all_day`), never as "00:00 - 00:00" or a stray creation time.
enum TimeSlotDisplayFormatter {
    static var allDayLabel: String {
        String(localized: "events.all_day")
    }

    static func isAllDay(_ timeOfDay: Shared.TimeOfDay?) -> Bool {
        timeOfDay == .allDay
    }

    /// The hours line of a slot: the localized "all day" label for ALL_DAY slots,
    /// otherwise "start - end" formatted in the slot timezone.
    static func hoursText(
        start: String?,
        end: String?,
        timezone: String,
        timeOfDay: Shared.TimeOfDay?,
        locale: Locale = .autoupdatingCurrent
    ) -> String {
        if isAllDay(timeOfDay) {
            return allDayLabel
        }

        let startValue = time(start, timezone: timezone, locale: locale)
        let endValue = time(end, timezone: timezone, locale: locale)

        guard let startValue else { return start ?? "" }
        guard let endValue, endValue != startValue else { return startValue }
        return "\(startValue) - \(endValue)"
    }

    /// Short time ("19:00") of an ISO-8601 instant in the given timezone, or nil if unparsable.
    static func time(_ value: String?, timezone: String, locale: Locale = .autoupdatingCurrent) -> String? {
        guard let value, let date = ISO8601DateFormatter().date(from: value) else { return nil }
        let formatter = DateFormatter()
        formatter.locale = locale
        formatter.timeZone = timeZone(for: timezone)
        formatter.timeStyle = .short
        formatter.dateStyle = .none
        return formatter.string(from: date)
    }

    static func timeZone(for identifier: String) -> TimeZone {
        TimeZone(identifier: identifier.trimmingCharacters(in: .whitespacesAndNewlines)) ?? .current
    }
}
