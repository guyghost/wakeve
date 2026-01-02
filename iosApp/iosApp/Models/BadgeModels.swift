import SwiftUI

// MARK: - Badge Category Enum
///
/// Category classification for badges to organize achievements.
public enum BadgeCategory: String, Codable, CaseIterable {
    case CREATION = "CREATION"
    case VOTING = "VOTING"
    case PARTICIPATION = "PARTICIPATION"
    case ENGAGEMENT = "ENGAGEMENT"
    case SPECIAL = "SPECIAL"

    var displayName: String {
        switch self {
        case .CREATION:
            return "Création"
        case .VOTING:
            return "Vote"
        case .PARTICIPATION:
            return "Participation"
        case .ENGAGEMENT:
            return "Engagement"
        case .SPECIAL:
            return "Spécial"
        }
    }

    var icon: String {
        switch self {
        case .CREATION:
            return "🎨"
        case .VOTING:
            return "🗳️"
        case .PARTICIPATION:
            return "🎉"
        case .ENGAGEMENT:
            return "💬"
        case .SPECIAL:
            return "⭐"
        }
    }
}

// MARK: - Badge Rarity Enum
///
/// Rarity level affecting display appearance and prestige.
public enum BadgeRarity: String, Codable, CaseIterable {
    case COMMON = "COMMON"
    case RARE = "RARE"
    case EPIC = "EPIC"
    case LEGENDARY = "LEGENDARY"

    var displayName: String {
        switch self {
        case .COMMON:
            return "Commun"
        case .RARE:
            return "Rare"
        case .EPIC:
            return "Épique"
        case .LEGENDARY:
            return "Légendaire"
        }
    }

    var color: Color {
        switch self {
        case .COMMON:
            return .gray
        case .RARE:
            return .blue
        case .EPIC:
            return .purple
        case .LEGENDARY:
            return .orange
        }
    }

    var glowColor: Color {
        switch self {
        case .COMMON:
            return .gray.opacity(0.3)
        case .RARE:
            return .blue.opacity(0.4)
        case .EPIC:
            return .purple.opacity(0.5)
        case .LEGENDARY:
            return .orange.opacity(0.6)
        }
    }

    var pointsMultiplier: Int {
        switch self {
        case .COMMON:
            return 1
        case .RARE:
            return 2
        case .EPIC:
            return 3
        case .LEGENDARY:
            return 5
        }
    }
}

// MARK: - Badge Model
///
/// Represents a badge that can be earned by users for specific achievements.
///
/// - Properties:
///   - id: Unique identifier for the badge (e.g., "badge-super-organizer")
///   - name: Display name of the badge (e.g., "Super Organisateur")
///   - description: Description of how to earn the badge
///   - icon: Emoji or icon identifier for display
///   - requirement: Threshold needed to unlock (events count, points, etc.)
///   - pointsReward: Points awarded when badge is unlocked
///   - category: Category of the badge for organization
///   - rarity: Rarity level affecting display and prestige
///   - unlockedAt: ISO timestamp when the badge was unlocked (nil if locked)
public struct Badge: Identifiable, Codable, Equatable {
    public let id: String
    public let name: String
    public let description: String
    public let icon: String
    public let requirement: Int
    public let pointsReward: Int
    public let category: BadgeCategory
    public let rarity: BadgeRarity
    public let unlockedAt: String?

    public init(
        id: String,
        name: String,
        description: String,
        icon: String,
        requirement: Int,
        pointsReward: Int,
        category: BadgeCategory,
        rarity: BadgeRarity,
        unlockedAt: String? = nil
    ) {
        self.id = id
        self.name = name
        self.description = description
        self.icon = icon
        self.requirement = requirement
        self.pointsReward = pointsReward
        self.category = category
        self.rarity = rarity
        self.unlockedAt = unlockedAt
    }

    /// Whether the badge is currently unlocked
    public var isUnlocked: Bool {
        unlockedAt != nil
    }

    /// Progress towards unlocking (0.0 to 1.0)
    public func progress(currentValue: Int) -> Double {
        guard requirement > 0 else { return 1.0 }
        return min(1.0, Double(currentValue) / Double(requirement))
    }
}

// MARK: - User Badges Model
///
/// Contains all badges owned by a user with metadata.
public struct UserBadges: Codable {
    public let userId: String
    public let badges: [Badge]
    public let unlockedAt: [String: String]  // badgeId -> timestamp

    public init(userId: String, badges: [Badge], unlockedAt: [String: String] = [:]) {
        self.userId = userId
        self.badges = badges
        self.unlockedAt = unlockedAt
    }

    /// Check if user has a specific badge
    public func hasBadge(_ badgeId: String) -> Bool {
        badges.contains { $0.id == badgeId }
    }

    /// Count badges by rarity
    public func countByRarity(_ rarity: BadgeRarity) -> Int {
        badges.filter { $0.rarity == rarity }.count
    }

    /// Total points from all badges
    public func totalBadgePoints() -> Int {
        badges.reduce(0) { $0 + $1.pointsReward }
    }

    /// Get unlocked badge count
    public var unlockedCount: Int {
        badges.filter { $0.isUnlocked }.count
    }

    /// Get locked badge count
    public var lockedCount: Int {
        badges.filter { !$0.isUnlocked }.count
    }
}

// MARK: - Predefined Badges
///
/// Factory methods for creating standard badges.
extension Badge {
    /// Badge for creating first event
    public static func firstEvent() -> Badge {
        Badge(
            id: "badge-first-event",
            name: "Premier événement",
            description: "A créé son premier événement",
            icon: "🎉",
            requirement: 1,
            pointsReward: 50,
            category: .CREATION,
            rarity: .COMMON
        )
    }

    /// Badge for creating 5 events
    public static func dedicated() -> Badge {
        Badge(
            id: "badge-dedicated",
            name: "Organisateur dévoué",
            description: "A créé 5 événements",
            icon: "📅",
            requirement: 5,
            pointsReward: 75,
            category: .CREATION,
            rarity: .RARE
        )
    }

    /// Badge for creating 10 events
    public static func superOrganizer() -> Badge {
        Badge(
            id: "badge-super-organizer",
            name: "Super Organisateur",
            description: "A créé 10 événements",
            icon: "🏆",
            requirement: 10,
            pointsReward: 100,
            category: .CREATION,
            rarity: .EPIC
        )
    }

    /// Badge for creating 25 events
    public static func eventMaster() -> Badge {
        Badge(
            id: "badge-event-master",
            name: "Maître des événements",
            description: "A créé 25 événements",
            icon: "👑",
            requirement: 25,
            pointsReward: 250,
            category: .CREATION,
            rarity: .LEGENDARY
        )
    }

    /// Badge for voting 10 times
    public static func earlyBird() -> Badge {
        Badge(
            id: "badge-early-bird",
            name: "Coq matinal",
            description: "A vote sur 10 sondages",
            icon: "🐔",
            requirement: 10,
            pointsReward: 30,
            category: .VOTING,
            rarity: .COMMON
        )
    }

    /// Badge for voting 50 times
    public static func votingMachine() -> Badge {
        Badge(
            id: "badge-voting-machine",
            name: "Machine à voter",
            description: "A participé à 50 votes",
            icon: "🗳️",
            requirement: 50,
            pointsReward: 100,
            category: .VOTING,
            rarity: .RARE
        )
    }

    /// Badge for participating in 5 events
    public static func festivalGoer() -> Badge {
        Badge(
            id: "badge-festival-goer",
            name: "Fêteux",
            description: "A participé à 5 événements",
            icon: "🎊",
            requirement: 5,
            pointsReward: 50,
            category: .PARTICIPATION,
            rarity: .COMMON
        )
    }

    /// Badge for reaching 1000 points
    public static func millennium() -> Badge {
        Badge(
            id: "badge-millennium",
            name: "Millenium",
            description: "A atteint 1000 points",
            icon: "🌟",
            requirement: 1000,
            pointsReward: 200,
            category: .ENGAGEMENT,
            rarity: .RARE
        )
    }

    /// Badge for reaching 10000 points
    public static func eventLegend() -> Badge {
        Badge(
            id: "badge-legend",
            name: "Légende des événements",
            description: "A atteint 10000 points",
            icon: "💎",
            requirement: 10000,
            pointsReward: 500,
            category: .ENGAGEMENT,
            rarity: .LEGENDARY
        )
    }

    /// Get all predefined badges
    public static var allPredefined: [Badge] {
        [
            firstEvent(),
            dedicated(),
            superOrganizer(),
            eventMaster(),
            earlyBird(),
            votingMachine(),
            festivalGoer(),
            millennium(),
            eventLegend()
        ]
    }
}

// MARK: - Sample Data

extension Badge {
    public static var samples: [Badge] {
        allPredefined
    }

    public static var sampleUnlocked: Badge {
        var badge = superOrganizer()
        return Badge(
            id: badge.id,
            name: badge.name,
            description: badge.description,
            icon: badge.icon,
            requirement: badge.requirement,
            pointsReward: badge.pointsReward,
            category: badge.category,
            rarity: badge.rarity,
            unlockedAt: "2026-01-01T10:30:00Z"
        )
    }
}

extension UserBadges {
    public static var sample: UserBadges {
        UserBadges(
            userId: "user-123",
            badges: Badge.samples,
            unlockedAt: [
                "badge-first-event": "2025-12-01T10:00:00Z",
                "badge-dedicated": "2025-12-15T14:30:00Z"
            ]
        )
    }

    public static var empty: UserBadges {
        UserBadges(userId: "user-123", badges: [], unlockedAt: [:])
    }
}
