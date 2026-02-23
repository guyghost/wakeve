import SwiftUI

// MARK: - Profile Screen

struct ProfileScreen: View {
    @StateObject private var viewModel = ProfileViewModel()
    
    var body: some View {
        NavigationStack {
            Group {
                if viewModel.isLoading {
                    ProgressView(String(localized: "common.loading"))
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                } else {
                    ScrollView {
                        VStack(spacing: 20) {
                            // Points Summary Card
                            PointsSummaryCard(
                                totalPoints: viewModel.totalPoints,
                                eventCreationPoints: viewModel.eventCreationPoints,
                                votingPoints: viewModel.votingPoints,
                                commentPoints: viewModel.commentPoints,
                                participationPoints: viewModel.participationPoints
                            )
                            .padding(.horizontal)

                            // Level Progress Section
                            LevelProgressSection(totalPoints: viewModel.totalPoints)
                                .padding(.horizontal)

                            // Badges Section
                            BadgesSection(badges: viewModel.badges)
                            
                            // Leaderboard Section
                            LeaderboardSection(
                                leaderboard: viewModel.leaderboard,
                                selectedTab: viewModel.selectedTab,
                                currentUserId: viewModel.currentUserId,
                                onTabSelected: { viewModel.selectTab($0) }
                            )
                        }
                        .padding(.vertical)
                    }
                }
            }
            .navigationTitle(String(localized: "profile.achievements"))
            .alert(String(localized: "common.error"), isPresented: .constant(viewModel.error != nil)) {
                Button("OK") {
                    viewModel.clearError()
                }
            } message: {
                if let error = viewModel.error {
                    Text(error)
                }
            }
        }
    }
}

// MARK: - Points Summary Card

struct PointsSummaryCard: View {
    let totalPoints: Int
    let eventCreationPoints: Int
    let votingPoints: Int
    let commentPoints: Int
    let participationPoints: Int
    
    var body: some View {
        LiquidGlassCard {
            VStack(alignment: .leading, spacing: 16) {
                // Header with total points
                HStack {
                    VStack(alignment: .leading, spacing: 4) {
                        Text(String(localized: "gamification.total_points"))
                            .font(.subheadline)
                            .foregroundColor(.secondary)
                        
                        Text(formatPoints(totalPoints))
                            .font(.system(size: 36, weight: .bold))
                            .foregroundColor(.primary)
                    }
                    
                    Spacer()
                    
                    // Star icon
                    ZStack {
                        Circle()
                            .fill(
                                LinearGradient(
                                    colors: [.wakevePrimary, .wakeveAccent],
                                    startPoint: .topLeading,
                                    endPoint: .bottomTrailing
                                )
                            )
                            .frame(width: 56, height: 56)
                        
                        Image(systemName: "star.fill")
                            .font(.system(size: 24))
                            .foregroundColor(.white)
                    }
                }
                
                LiquidGlassDivider(style: .subtle)
                    .padding(.vertical, 4)
                
                // Points breakdown
                PointBreakdownRow(label: String(localized: "gamification.event_creation"), points: eventCreationPoints, color: .wakeveWarning)
                PointBreakdownRow(label: String(localized: "gamification.voting"), points: votingPoints, color: .wakeveSuccess)
                PointBreakdownRow(label: String(localized: "gamification.commenting"), points: commentPoints, color: .wakeveAccent)
                PointBreakdownRow(label: String(localized: "gamification.participation"), points: participationPoints, color: .wakevePrimary)
            }
        }
    }
}

struct PointBreakdownRow: View {
    let label: String
    let points: Int
    let color: Color
    
    var body: some View {
        HStack {
            Circle()
                .fill(color)
                .frame(width: 12, height: 12)
                .accessibilityHidden(true)  // Color indicator, label is provided by Text
            
            Text(label)
                .font(.subheadline)
                .foregroundColor(.primary)
            
            Spacer()
            
            Text(formatPoints(points))
                .font(.headline)
                .fontWeight(.semibold)
                .foregroundColor(color)
        }
    }
}

// MARK: - Badges Section

struct BadgesSection: View {
    let badges: [ProfileBadge]
    
    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            // Section header
            HStack {
                Image(systemName: "trophy.fill")
                    .foregroundColor(.wakeveWarning)
                
                Text(String(localized: "gamification.achievements"))
                    .font(.title2)
                    .fontWeight(.bold)
                    .foregroundColor(.primary)
                
                Spacer()
                
                Text(String(format: String(localized: "gamification.badges_count"), badges.count))
                    .font(.subheadline)
                    .foregroundColor(.secondary)
            }
            .padding(.horizontal)
            
            // Badges by category
            ForEach(BadgeCategory.allCases, id: \.self) { category in
                let categoryBadges = badges.filter { $0.category == category }
                if !categoryBadges.isEmpty {
                    BadgeCategorySection(category: category, badges: categoryBadges)
                }
            }
        }
    }
}

struct BadgeCategorySection: View {
    let category: BadgeCategory
    let badges: [ProfileBadge]
    
    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(category.displayName)
                .font(.headline)
                .foregroundColor(.secondary)
                .padding(.horizontal)
            
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 12) {
                    ForEach(badges) { badge in
                        BadgeItemView(badge: badge)
                    }
                }
                .padding(.horizontal)
            }
        }
    }
}

struct BadgeItemView: View {
    let badge: ProfileBadge
    
    var body: some View {
        LiquidGlassCard(
            cornerRadius: 12,
            padding: 12
        ) {
            VStack(spacing: 8) {
                // Badge icon
                Text(badge.icon)
                    .font(.system(size: 40))
                
                // Badge name
                Text(badge.name)
                    .font(.caption)
                    .fontWeight(.medium)
                    .multilineTextAlignment(.center)
                    .lineLimit(2)
                    .foregroundColor(.primary)
                    .frame(width: 100)
                
                // Rarity badge
                LiquidGlassBadge(
                    text: badge.rarity.displayName,
                    style: rarityBadgeStyle
                )
            }
            .frame(width: 110, height: 130)
        }
        .overlay(
            RoundedRectangle(cornerRadius: 12, style: .continuous)
                .stroke(rarityColor.opacity(0.5), lineWidth: 1)
        )
    }
    
    private var rarityColor: Color {
        switch badge.rarity {
        case .common:
            return .gray
        case .rare:
            return .wakevePrimary
        case .epic:
            return .wakeveAccent
        case .legendary:
            return .wakeveWarning
        }
    }
    
    private var rarityBadgeStyle: LiquidGlassBadgeStyle {
        switch badge.rarity {
        case .common:
            return .default
        case .rare:
            return .default
        case .epic:
            return .accent
        case .legendary:
            return .warning
        }
    }
}

// MARK: - Leaderboard Section

struct LeaderboardSection: View {
    let leaderboard: [LeaderboardEntry]
    let selectedTab: LeaderboardType
    let currentUserId: String
    let onTabSelected: (LeaderboardType) -> Void
    
    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            // Section header
            HStack {
                Image(systemName: "chart.bar.fill")
                    .foregroundColor(.wakevePrimary)
                
                Text(String(localized: "gamification.leaderboard"))
                    .font(.title2)
                    .fontWeight(.bold)
                    .foregroundColor(.primary)
            }
            .padding(.horizontal)
            
            // Tabs
            LeaderboardTabs(selectedTab: selectedTab, onTabSelected: onTabSelected)
            
            // Leaderboard list
            LazyVStack(spacing: 8) {
                ForEach(leaderboard) { entry in
                    LeaderboardItemView(
                        entry: entry,
                        isCurrentUser: entry.userId == currentUserId
                    )
                }
            }
            .padding(.horizontal)
        }
    }
}

struct LeaderboardTabs: View {
    let selectedTab: LeaderboardType
    let onTabSelected: (LeaderboardType) -> Void
    
    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                ForEach(LeaderboardType.allCases, id: \.self) { tab in
                    LiquidGlassButton(
                        title: tab.displayName,
                        style: selectedTab == tab ? .primary : .text,
                        size: .small,
                        action: { onTabSelected(tab) }
                    )
                }
            }
            .padding(.horizontal)
        }
    }
}

struct LeaderboardItemView: View {
    let entry: LeaderboardEntry
    let isCurrentUser: Bool
    
    var body: some View {
        LiquidGlassCard(
            cornerRadius: 12,
            padding: 12
        ) {
            HStack(spacing: 12) {
                // Rank badge
                LiquidGlassBadge(
                    text: "#\(entry.rank)",
                    style: rankBadgeStyle
                )
                .frame(width: 50, alignment: .leading)
                
                // User info
                VStack(alignment: .leading, spacing: 2) {
                    HStack(spacing: 4) {
                        Text(entry.username)
                            .font(.subheadline)
                            .fontWeight(isCurrentUser ? .bold : .medium)
                            .foregroundColor(.primary)
                        
                        if entry.isFriend {
                            LiquidGlassBadge(
                                text: String(localized: "leaderboard.friend"),
                                style: .accent
                            )
                        }
                    }
                    
                    Text(String(format: String(localized: "gamification.badges_count"), entry.badgesCount))
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
                
                Spacer()
                
                // Points
                VStack(alignment: .trailing, spacing: 2) {
                    Text(formatPoints(entry.totalPoints))
                        .font(.headline)
                        .fontWeight(.bold)
                        .foregroundColor(.wakevePrimary)
                    
                    Text("points")
                        .font(.caption2)
                        .foregroundColor(.secondary)
                }
            }
        }
        .accessibilityElement(children: .combine)
        .accessibilityLabel(String(format: String(localized: "events.accessibility.rank"), entry.username, entry.rank, entry.totalPoints))
        .accessibilityHint(isCurrentUser ? String(localized: "events.accessibility.its_you") : "")
    }
    
    private var rankBadgeStyle: LiquidGlassBadgeStyle {
        switch entry.rank {
        case 1:
            return .warning
        case 2:
            return .default
        case 3:
            return .accent
        default:
            return .default
        }
    }
}

// MARK: - Level Progress Section

struct LevelProgressSection: View {
    let totalPoints: Int

    // Level thresholds matching the shared Kotlin model
    private var levelThresholds: [(points: Int, name: String)] {
        [
            (0, String(localized: "gamification.level.beginner")),
            (50, String(localized: "gamification.level.explorer")),
            (150, String(localized: "gamification.level.contributor")),
            (300, String(localized: "gamification.level.organizer")),
            (500, String(localized: "gamification.level.expert")),
            (800, String(localized: "gamification.level.master")),
            (1200, String(localized: "gamification.level.champion")),
            (1800, String(localized: "gamification.level.legend")),
            (2500, String(localized: "gamification.level.mythic")),
            (3500, String(localized: "gamification.level.transcendent"))
        ]
    }

    private var currentLevelIndex: Int {
        var idx = 0
        for i in levelThresholds.indices {
            if totalPoints >= levelThresholds[i].points {
                idx = i
            } else {
                break
            }
        }
        return idx
    }

    private var currentThreshold: Int { levelThresholds[currentLevelIndex].points }
    private var nextThreshold: Int {
        currentLevelIndex + 1 < levelThresholds.count
            ? levelThresholds[currentLevelIndex + 1].points
            : currentThreshold + 1000
    }
    private var levelName: String { levelThresholds[currentLevelIndex].name }
    private var level: Int { currentLevelIndex + 1 }
    private var progress: Double {
        let needed = Double(nextThreshold - currentThreshold)
        if needed <= 0 { return 1.0 }
        return Double(totalPoints - currentThreshold) / needed
    }

    var body: some View {
        LiquidGlassCard {
            VStack(alignment: .leading, spacing: 12) {
                HStack {
                    VStack(alignment: .leading, spacing: 2) {
                        Text(String(format: String(localized: "gamification.level_format"), level))
                            .font(.headline)
                            .fontWeight(.bold)
                            .foregroundColor(.wakevePrimary)

                        Text(levelName)
                            .font(.subheadline)
                            .foregroundColor(.secondary)
                    }

                    Spacer()

                    Text("\(totalPoints) / \(nextThreshold) pts")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }

                // Progress bar
                GeometryReader { geometry in
                    ZStack(alignment: .leading) {
                        RoundedRectangle(cornerRadius: 4)
                            .fill(Color.gray.opacity(0.2))
                            .frame(height: 8)

                        RoundedRectangle(cornerRadius: 4)
                            .fill(
                                LinearGradient(
                                    gradient: Gradient(colors: [.wakevePrimary, .wakeveAccent]),
                                    startPoint: .leading,
                                    endPoint: .trailing
                                )
                            )
                            .frame(width: geometry.size.width * CGFloat(progress), height: 8)
                            .animation(.easeInOut(duration: 0.8), value: progress)
                    }
                }
                .frame(height: 8)
            }
        }
    }
}

// MARK: - Helper Functions

private func formatPoints(_ points: Int) -> String {
    if points >= 1_000_000 {
        return String(format: "%.1fM", Double(points) / 1_000_000)
    } else if points >= 1_000 {
        return String(format: "%.1fk", Double(points) / 1_000)
    } else {
        return "\(points)"
    }
}

// MARK: - Preview

#Preview {
    ProfileScreen()
}
