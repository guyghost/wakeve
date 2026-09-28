import Foundation

enum HomeDateText {
    /// Nombre de jours calendaires (≥ 0) entre deux instants, dans le calendrier donné.
    static func daysBetween(_ from: Date, _ to: Date, calendar: Calendar = .current) -> Int {
        let start = calendar.startOfDay(for: from)
        let end = calendar.startOfDay(for: to)
        return max(0, calendar.dateComponents([.day], from: start, to: end).day ?? 0)
    }

    static func short(_ date: Date, locale: Locale = WK.appLocale, calendar: Calendar = .current) -> String {
        let formatter = DateFormatter()
        formatter.locale = locale
        formatter.calendar = calendar
        formatter.timeZone = calendar.timeZone
        formatter.setLocalizedDateFormatFromTemplate("EEE d MMM")
        return formatter.string(from: date)
    }

    static func parseISO(_ value: String?) -> Date? {
        guard let value, !value.isEmpty else { return nil }
        let withFraction = ISO8601DateFormatter()
        withFraction.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        return withFraction.date(from: value) ?? ISO8601DateFormatter().date(from: value)
    }

    /// « clôture dans N j » avec pluriel (stringsdict `home.v2.closes_in_days`).
    static func closesIn(days: Int, locale: Locale = WK.appLocale) -> String {
        String(format: WK.localizedFormat("home.v2.closes_in_days", locale: locale), locale: locale, days)
    }
}
