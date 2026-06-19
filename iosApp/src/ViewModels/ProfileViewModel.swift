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
        case .creation: return String(localized: "gamification.event_creation")
        case .voting: return String(localized: "gamification.voting")
        case .participation: return String(localized: "gamification.participation")
        case .engagement: return String(localized: "gamification.badge_category.engagement")
        case .special: return String(localized: "gamification.badge_category.special")
        }
    }
}

enum BadgeRarity: String, CaseIterable, Codable {
    case common = "COMMON"
    case rare = "RARE"
    case epic = "EPIC"
    case legendary = "LEGENDARY"
    
    var displayName: String {
        switch self {
        case .common: return String(localized: "gamification.rarity.common")
        case .rare: return String(localized: "gamification.rarity.rare")
        case .epic: return String(localized: "gamification.rarity.epic")
        case .legendary: return String(localized: "gamification.rarity.legendary")
        }
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
        case .allTime: return String(localized: "leaderboard.filter.global")
        case .thisMonth: return String(localized: "leaderboard.filter.this_month")
        case .thisWeek: return String(localized: "leaderboard.filter.this_week")
        case .friends: return String(localized: "leaderboard.filter.friends")
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
        UserDefaults.standard.string(forKey: "wakeve_current_user_id")
            ?? UserDefaults.standard.string(forKey: "wakeve_guest_user_id")
            ?? "local-profile"
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
        let lastUpdated = ISO8601DateFormatter().string(from: Date())
        userPoints = UserPoints(
            userId: currentUserId,
            totalPoints: 0,
            eventCreationPoints: 0,
            votingPoints: 0,
            commentPoints: 0,
            participationPoints: 0,
            decayPoints: 0,
            lastUpdated: lastUpdated
        )
    }
    
    private func loadUserBadges() async throws {
        badges = []
    }
    
    private func loadLeaderboard() async throws {
        guard totalPoints > 0 || !badges.isEmpty else {
            leaderboard = []
            return
        }

        leaderboard = [
            LeaderboardEntry(
                id: currentUserId,
                userId: currentUserId,
                username: String(localized: "leaderboard.you"),
                totalPoints: totalPoints,
                badgesCount: badges.count,
                rank: 1,
                isCurrentUser: true,
                isFriend: false,
                legendaryCount: badges.filter { $0.rarity == .legendary }.count,
                epicCount: badges.filter { $0.rarity == .epic }.count
            )
        ]
    }
    
    // MARK: - Actions
    
    func selectTab(_ tab: LeaderboardType) {
        selectedTab = tab
        Task {
            do {
                try await loadLeaderboard()
            } catch {
                self.error = error.localizedDescription
            }
        }
    }
    
    func clearError() {
        error = nil
    }
}
