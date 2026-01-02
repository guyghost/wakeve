import SwiftUI

// MARK: - Profile Screen

struct ProfileScreen: View {
    @StateObject private var viewModel = ProfileViewModel()
    
    var body: some View {
        NavigationStack {
            Group {
                if viewModel.isLoading {
                    ProgressView("Chargement...")
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
            .navigationTitle("Profil & Succès")
            .alert("Erreur", isPresented: .constant(viewModel.error != nil)) {
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
        VStack(alignment: .leading, spacing: 16) {
            // Header with total points
            HStack {
                VStack(alignment: .leading, spacing: 4) {
                    Text("Points Totaux")
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
                                gradient: Gradient(colors: [.blue, .purple]),
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
            
            Divider()
                .opacity(0.5)
            
            // Points breakdown
            PointBreakdownRow(label: "Création d'événements", points: eventCreationPoints, color: Color(hex: "FF6B6B"))
            PointBreakdownRow(label: "Votes", points: votingPoints, color: Color(hex: "4ECDC4"))
            PointBreakdownRow(label: "Commentaires", points: commentPoints, color: Color(hex: "FFE66D"))
            PointBreakdownRow(label: "Participation", points: participationPoints, color: Color(hex: "95E1D3"))
        }
        .padding(16)
        .background(
            RoundedRectangle(cornerRadius: 16, style: .continuous)
                .fill(.regularMaterial)
                .shadow(color: .black.opacity(0.05), radius: 8, x: 0, y: 4)
        )
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
    let badges: [Badge]
    
    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            // Section header
            HStack {
                Image(systemName: "trophy.fill")
                    .foregroundColor(.yellow)
                
                Text("Succès")
                    .font(.title2)
                    .fontWeight(.bold)
                    .foregroundColor(.primary)
                
                Spacer()
                
                Text("\(badges.count) badges")
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
    let badges: [Badge]
    
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
    let badge: Badge
    
    var body: some View {
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
            
            // Rarity
            Text(badge.rarity.displayName)
                .font(.caption2)
                .fontWeight(.medium)
                .foregroundColor(rarityColor)
        }
        .frame(width: 110, height: 130)
        .background(
            RoundedRectangle(cornerRadius: 12, style: .continuous)
                .fill(rarityColor.opacity(0.15))
        )
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
            return .blue
        case .epic:
            return .purple
        case .legendary:
            return .orange
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
                    .foregroundColor(.blue)
                
                Text("Classement")
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
                    TabButton(
                        title: tab.displayName,
                        isSelected: selectedTab == tab
                    ) {
                        onTabSelected(tab)
                    }
                }
            }
            .padding(.horizontal)
        }
    }
}

struct TabButton: View {
    let title: String
    let isSelected: Bool
    let action: () -> Void
    
    var body: some View {
        Button(action: action) {
            Text(title)
                .font(.subheadline)
                .fontWeight(.medium)
                .padding(.horizontal, 16)
                .padding(.vertical, 8)
                .background(
                    RoundedRectangle(cornerRadius: 20)
                        .fill(isSelected ? Color.blue.opacity(0.2) : Color.clear)
                )
                .overlay(
                    RoundedRectangle(cornerRadius: 20)
                        .stroke(isSelected ? Color.blue : Color.gray.opacity(0.3), lineWidth: 1)
                )
                .foregroundColor(isSelected ? .blue : .secondary)
        }
        .buttonStyle(.plain)
    }
}

struct LeaderboardItemView: View {
    let entry: LeaderboardEntry
    let isCurrentUser: Bool
    
    var body: some View {
        HStack(spacing: 12) {
            // Rank
            Text("#\(entry.rank)")
                .font(.headline)
                .fontWeight(.bold)
                .foregroundColor(rankColor)
                .frame(width: 40, alignment: .leading)
            
            // User info
            VStack(alignment: .leading, spacing: 2) {
                HStack(spacing: 4) {
                    Text(entry.username)
                        .font(.subheadline)
                        .fontWeight(isCurrentUser ? .bold : .medium)
                        .foregroundColor(.primary)
                    
                    if entry.isFriend {
                        Text("👥")
                            .font(.caption)
                    }
                }
                
                Text("\(entry.badgesCount) badges")
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
            
            Spacer()
            
            // Points
            VStack(alignment: .trailing, spacing: 2) {
                Text(formatPoints(entry.totalPoints))
                    .font(.headline)
                    .fontWeight(.bold)
                    .foregroundColor(.blue)
                
                Text("points")
                    .font(.caption2)
                    .foregroundColor(.secondary)
            }
        }
        .padding(12)
        .background(
            RoundedRectangle(cornerRadius: 12, style: .continuous)
                .fill(isCurrentUser ? Color.blue.opacity(0.1) : Color(.systemBackground).opacity(0.8))
        )
        .overlay(
            RoundedRectangle(cornerRadius: 12, style: .continuous)
                .stroke(isCurrentUser ? Color.blue.opacity(0.3) : Color.gray.opacity(0.2), lineWidth: 1)
        )
        .shadow(color: isCurrentUser ? Color.blue.opacity(0.1) : .clear, radius: 4, x: 0, y: 2)
    }
    
    private var rankColor: Color {
        switch entry.rank {
        case 1:
            return .yellow
        case 2:
            return .gray
        case 3:
            return .orange
        default:
            return .primary
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

// MARK: - Color Extension

extension Color {
    init(hex: String) {
        let scanner = Scanner(string: hex)
        var rgbValue: UInt64 = 0
        scanner.scanHexInt64(&rgbValue)
        
        let r = Double((rgbValue & 0xFF0000) >> 16) / 255.0
        let g = Double((rgbValue & 0x00FF00) >> 8) / 255.0
        let b = Double(rgbValue & 0x0000FF) / 255.0
        
        self.init(red: r, green: g, blue: b)
    }
}

// MARK: - Preview

#Preview {
    ProfileScreen()
}
