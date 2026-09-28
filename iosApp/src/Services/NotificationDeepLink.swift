import Foundation

/// Convertit le `userInfo` d'un tap de notification (`NavigateToEvent`) en URL de deep link.
enum NotificationDeepLink {
    static func url(from userInfo: [AnyHashable: Any]) -> URL? {
        if let url = userInfo["deepLink"] as? URL { return url }
        if let raw = userInfo["deepLink"] as? String, !raw.isEmpty, let url = URL(string: raw) { return url }
        guard let eventId = userInfo["eventId"] as? String, !eventId.isEmpty,
              let encoded = eventId.addingPercentEncoding(withAllowedCharacters: .urlPathAllowed) else {
            return nil
        }
        return URL(string: "wakeve://event/\(encoded)")
    }
}
