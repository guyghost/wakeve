import Foundation

enum HomeDateText {
    /// Nombre de jours calendaires (≥ 0) entre deux instants, dans le calendrier donné.
    static func daysBetween(_ from: Date, _ to: Date, calendar: Calendar = .current) -> Int {
        let start = calendar.startOfDay(for: from)
        let end = calendar.startOfDay(for: to)
        return max(0, calendar.dateComponents([.day], from: start, to: end).day ?? 0)
    }
}
