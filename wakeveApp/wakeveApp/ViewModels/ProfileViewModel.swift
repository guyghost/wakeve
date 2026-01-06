import Foundation
import Combine

// MARK: - Badge Model (matching shared model)

enum BadgeCategory: String, CaseIterable, Codable {
    case creation = "CREATION"
    case voting = "VOTING"
    case participation = "PARTICIPATION"
    case engagement = "ENGAGEMENT"
    case special = "SPECIAL"
    
    var displayName: String {
        switch self {
        case .creation: return "Création"
        case .voting: return "Votes"
        case .participation: return "Participation"
        case .engagement: return "Engagement"
        case .special: return "Spéciaux"
        }
    }
}

enum BadgeRarity: String, CaseIterable, Codable {
    case common = "COMMON"
    case rare = "RARE"
    case epic = "EPIC"
    case legendary = "LEGENDARY"
    
    var displayName: String {
        rawValue.lowercased().prefix(1).uppercased() + rawValue.lowercased().dropFirst()
    }
    
    var color: String {
        switch self {
        case .common: return "gray"
        case .rare: return "blue"
        case .epic: return "purple"
        case .legendary: return "orange"
        }
    }
}

struct ProfileBadge: Identifiable, Equatable {
    let id: String
    let name: String
    let description: String
    let icon: String
    let requirement: Int
    let pointsReward: Int
    let category: BadgeCategory
    let rarity: BadgeRarity
    let unlockedAt: String?
    
    static func == (lhs: ProfileBadge, rhs: ProfileBadge) -> Bool {
        lhs.id == rhs.id
    }
}

// MARK: - User Points Model

struct UserPoints {
    let userId: String
    let totalPoints: Int
    let eventCreationPoints: Int
    let votingPoints: Int
    let commentPoints: Int
    let participationPoints: Int
    let decayPoints: Int
    let lastUpdated: String
    
    var effectivePoints: Int {
        totalPoints + decayPoints
    }
}

// MARK: - Leaderboard Models

enum LeaderboardType: String, CaseIterable {
    case allTime = "ALL_TIME"
    case thisMonth = "THIS_MONTH"
    case thisWeek = "THIS_WEEK"
    case friends = "FRIENDS"
    
    var displayName: String {
        switch self {
        case .allTime: return "Tout temps"
        case .thisMonth: return "Ce mois"
        case .thisWeek: return "Cette semaine"
        case .friends: return "Amis"
        }
    }
}

struct LeaderboardEntry: Identifiable {
    let id: String
    let userId: String
    let username: String
    let totalPoints: Int
    let badgesCount: Int
    let rank: Int
    let isCurrentUser: Bool
    let isFriend: Bool
    let legendaryCount: Int
    let epicCount: Int
}

// MARK: - Profile ViewModel

@MainActor
class ProfileViewModel: ObservableObject {
    // MARK: - Published Properties
    
    @Published var isLoading: Bool = true
    @Published var error: String?
    @Published var userPoints: UserPoints?
    @Published var badges: [ProfileBadge] = []
    @Published var leaderboard: [LeaderboardEntry] = []
    @Published var selectedTab: LeaderboardType = .allTime
    
    // MARK: - Computed Properties
    
    var currentUserId: String {
        "user-current"
    }
    
    var totalPoints: Int {
        userPoints?.totalPoints ?? 0
    }
    
    var eventCreationPoints: Int {
        userPoints?.eventCreationPoints ?? 0
    }
    
    var votingPoints: Int {
        userPoints?.votingPoints ?? 0
    }
    
    var commentPoints: Int {
        userPoints?.commentPoints ?? 0
    }
    
    var participationPoints: Int {
        userPoints?.participationPoints ?? 0
    }
    
    var badgesByCategory: [BadgeCategory: [ProfileBadge]] {
        Dictionary(grouping: badges, by: { $0.category })
    }
    
    // MARK: - Initialization
    
    init() {
        loadProfileData()
    }
    
    // MARK: - Data Loading
    
    func loadProfileData() {
        isLoading = true
        error = nil
        
        Task {
            do {
                try await loadUserPoints()
                try await loadUserBadges()
                try await loadLeaderboard()
                isLoading = false
            } catch {
                self.error = error.localizedDescription
                isLoading = false
            }
        }
    }
    
    private func loadUserPoints() async throws {
        // Mock data for demonstration
        // In production, this would call the GamificationService
        userPoints = UserPoints(
            userId: currentUserId,
            totalPoints: 1250,
            eventCreationPoints: 500,
            votingPoints: 300,
            commentPoints: 250,
            participationPoints: 200,
            decayPoints: 0,
            lastUpdated: "2026-01-02T10:30:00Z"
        )
    }
    
    private func loadUserBadges() async throws {
        // Mock data for demonstration
        badges = [
            ProfileBadge(
                id: "badge-first-event",
                name: "Premier Événement",
                description: "A créé son premier événement",
                icon: "🎉",
                requirement: 1,
                pointsReward: 50,
                category: .creation,
                rarity: .common,
                unlockedAt: "2025-12-01T10:00:00Z"
            ),
            ProfileBadge(
                id: "badge-super-organizer",
                name: "Super Organisateur",
                description: "A créé 10 événements",
                icon: "🏆",
                requirement: 10,
                pointsReward: 100,
                category: .creation,
                rarity: .epic,
                unlockedAt: "2025-12-15T14:30:00Z"
            ),
            ProfileBadge(
                id: "badge-early-bird",
                name: "早起鸟 (Early Bird)",
                description: "A voté dans les 24h",
                icon: "🐦",
                requirement: 1,
                pointsReward: 25,
                category: .voting,
                rarity: .common,
                unlockedAt: "2025-12-20T08:00:00Z"
            ),
            ProfileBadge(
                id: "badge-voting-master",
                name: "Maître du Vote",
                description: "A voté 50 fois",
                icon: "🗳️",
                requirement: 50,
                pointsReward: 75,
                category: .voting,
                rarity: .rare,
                unlockedAt: "2025-12-28T16:00:00Z"
            ),
            ProfileBadge(
                id: "badge-active-participant",
                name: "Participant Actif",
                description: "A participé à 5 événements",
                icon: "🙋",
                requirement: 5,
                pointsReward: 50,
                category: .participation,
                rarity: .common,
                unlockedAt: "2025-12-10T12:00:00Z"
            ),
            ProfileBadge(
                id: "badge-event-master",
                name: "Maître des Événements",
                description: "A organisé 5 événements ce mois",
                icon: "🎭",
                requirement: 5,
                pointsReward: 150,
                category: .engagement,
                rarity: .legendary,
                unlockedAt: "2025-12-25T20:00:00Z"
            ),
            ProfileBadge(
                id: "badge-commentator",
                name: "Commentateur",
                description: "A commenté 10 scénarios",
                icon: "💬",
                requirement: 10,
                pointsReward: 30,
                category: .participation,
                rarity: .common,
                unlockedAt: "2025-12-18T11:00:00Z"
            ),
            ProfileBadge(
                id: "badge-dedicated",
                name: "Dévoué",
                description: "7 jours consécutifs de participation",
                icon: "⭐",
                requirement: 7,
                pointsReward: 100,
                category: .engagement,
                rarity: .rare,
                unlockedAt: "2025-12-30T09:00:00Z"
            )
        ]
    }
    
    private func loadLeaderboard() async throws {
        // Mock data for demonstration
        leaderboard = [
            LeaderboardEntry(
                id: "user-1",
                userId: "user-1",
                username: "Alice Martin",
                totalPoints: 2450,
                badgesCount: 12,
                rank: 1,
                isCurrentUser: false,
                isFriend: false,
                legendaryCount: 2,
                epicCount: 4
            ),
            LeaderboardEntry(
                id: "user-2",
                userId: "user-2",
                username: "Bob Dupont",
                totalPoints: 2100,
                badgesCount: 10,
                rank: 2,
                isCurrentUser: false,
                isFriend: true,
                legendaryCount: 1,
                epicCount: 3
            ),
            LeaderboardEntry(
                id: "user-3",
                userId: "user-3",
                username: "Claire Bernard",
                totalPoints: 1950,
                badgesCount: 9,
                rank: 3,
                isCurrentUser: false,
                isFriend: false,
                legendaryCount: 1,
                epicCount: 2
            ),
            LeaderboardEntry(
                id: currentUserId,
                userId: currentUserId,
                username: "Vous",
                totalPoints: 1250,
                badgesCount: 8,
                rank: 4,
                isCurrentUser: true,
                isFriend: false,
                legendaryCount: 1,
                epicCount: 1
            ),
            LeaderboardEntry(
                id: "user-5",
                userId: "user-5",
                username: "David Leroy",
                totalPoints: 1100,
                badgesCount: 7,
                rank: 5,
                isCurrentUser: false,
                isFriend: true,
                legendaryCount: 0,
                epicCount: 2
            ),
            LeaderboardEntry(
                id: "user-6",
                userId: "user-6",
                username: "Emma Moreau",
                totalPoints: 980,
                badgesCount: 6,
                rank: 6,
                isCurrentUser: false,
                isFriend: false,
                legendaryCount: 0,
                epicCount: 1
            ),
            LeaderboardEntry(
                id: "user-7",
                userId: "user-7",
                username: "Frank Rousseau",
                totalPoints: 850,
                badgesCount: 5,
                rank: 7,
                isCurrentUser: false,
                isFriend: false,
                legendaryCount: 0,
                epicCount: 1
            ),
            LeaderboardEntry(
                id: "user-8",
                userId: "user-8",
                username: "Grace Wang",
                totalPoints: 720,
                badgesCount: 4,
                rank: 8,
                isCurrentUser: false,
                isFriend: true,
                legendaryCount: 0,
                epicCount: 0
            ),
            LeaderboardEntry(
                id: "user-9",
                userId: "user-9",
                username: "Henry Chen",
                totalPoints: 650,
                badgesCount: 4,
                rank: 9,
                isCurrentUser: false,
                isFriend: false,
                legendaryCount: 0,
                epicCount: 0
            ),
            LeaderboardEntry(
                id: "user-10",
                userId: "user-10",
                username: "Isabelle Dubois",
                totalPoints: 580,
                badgesCount: 3,
                rank: 10,
                isCurrentUser: false,
                isFriend: false,
                legendaryCount: 0,
                epicCount: 0
            )
        ]
    }
    
    // MARK: - Actions
    
    func selectTab(_ tab: LeaderboardType) {
        selectedTab = tab
        Task {
            try await loadLeaderboard()
        }
    }
    
    func clearError() {
        error = nil
    }
}
