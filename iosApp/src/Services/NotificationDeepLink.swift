import Foundation

/// Convertit le `userInfo` d'un tap de notification (`NavigateToEvent`) en URL de deep link.
enum NotificationDeepLink {
    /// Un identifiant reste un seul segment de chemin : « / » est encodé (`%2F`),
    /// ce que `DeepLinkService` rejette ensuite plutôt que de naviguer ailleurs.
    private static let eventIdAllowed: CharacterSet = {
        var set = CharacterSet.urlPathAllowed
        set.remove("/")
        return set
    }()

    static func url(from userInfo: [AnyHashable: Any]) -> URL? {
        if let url = userInfo["deepLink"] as? URL { return url }
        if let raw = userInfo["deepLink"] as? String, !raw.isEmpty, let url = URL(string: raw) { return url }
        guard let eventId = userInfo["eventId"] as? String, !eventId.isEmpty,
              let encoded = eventId.addingPercentEncoding(withAllowedCharacters: eventIdAllowed) else {
            return nil
        }
        return URL(string: "wakeve://event/\(encoded)")
    }
}
