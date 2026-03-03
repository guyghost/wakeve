import Foundation

#if DEBUG

enum UserFactory {

    // MARK: - Named Variants

    static var organizer: User {
        User(id: "user-organizer-001", name: "Marie Dupont", email: "marie@example.com", avatarUrl: nil)
    }

    static var participant: User {
        User(id: "user-participant-002", name: "Lucas Martin", email: "lucas@example.com", avatarUrl: nil)
    }

    static var guest: User {
        User(id: "user-guest-003", name: "Invit\u{00E9}", email: "guest@example.com", avatarUrl: nil)
    }

    static var withAvatar: User {
        User(id: "user-avatar-004", name: "Sophie Bernard", email: "sophie@example.com", avatarUrl: "https://i.pravatar.cc/150?u=sophie")
    }

    // MARK: - Builder

    static func make(
        id: String = "user-\(UUID().uuidString.prefix(8))",
        name: String = "Test User",
        email: String = "test@example.com",
        avatarUrl: String? = nil
    ) -> User {
        User(id: id, name: name, email: email, avatarUrl: avatarUrl)
    }

    // MARK: - Collections

    static func group(count: Int) -> [User] {
        let names = ["Marie Dupont", "Lucas Martin", "Sophie Bernard", "Thomas Leroy", "Emma Petit", "Hugo Moreau", "L\u{00E9}a Fournier", "Nathan Girard"]
        return (0..<min(count, names.count)).map { i in
            User(id: "user-group-\(i)", name: names[i], email: "\(names[i].lowercased().replacingOccurrences(of: " ", with: "."))@example.com", avatarUrl: nil)
        }
    }
}

#endif
