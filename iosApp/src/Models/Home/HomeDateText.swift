import Foundation

enum HomeDateText {
    /// Nombre de jours calendaires (≥ 0) entre deux instants, dans le calendrier donné.
    static func daysBetween(_ from: Date, _ to: Date, calendar: Calendar = .current) -> Int {
        let start = calendar.startOfDay(for: from)
        let end = calendar.startOfDay(for: to)
        return max(0, calendar.dateComponents([.day], from: start, to: end).day ?? 0)
    }

    /// Formateurs réutilisés (création coûteuse) ; `NSCache` est sûr entre fils.
    private static let shortFormatters = NSCache<NSString, DateFormatter>()
    private static let isoWithFraction: ISO8601DateFormatter = {
        let formatter = ISO8601DateFormatter()
        formatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        return formatter
    }()
    private static let iso = ISO8601DateFormatter()

    static func short(_ date: Date, locale: Locale = WK.appLocale, calendar: Calendar = .current) -> String {
        let key = "\(locale.identifier)|\(calendar.identifier)|\(calendar.timeZone.identifier)" as NSString
        if let cached = shortFormatters.object(forKey: key) {
            return cached.string(from: date)
        }
        let formatter = DateFormatter()
        formatter.locale = locale
        formatter.calendar = calendar
        formatter.timeZone = calendar.timeZone
        formatter.setLocalizedDateFormatFromTemplate("EEE d MMM")
        shortFormatters.setObject(formatter, forKey: key)
        return formatter.string(from: date)
    }

    static func parseISO(_ value: String?) -> Date? {
        guard let value, !value.isEmpty else { return nil }
        return isoWithFraction.date(from: value) ?? iso.date(from: value)
    }

    /// « clôture dans N j » avec pluriel (stringsdict `home.v2.closes_in_days`).
    static func closesIn(days: Int, locale: Locale = WK.appLocale) -> String {
        String(format: WK.localizedFormat("home.v2.closes_in_days", locale: locale), locale: locale, days)
    }
}
