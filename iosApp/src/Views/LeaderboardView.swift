import SwiftUI

// MARK: - Leaderboard View

/// Full leaderboard screen with filtering and pull-to-refresh.
/// Shows top participants ranked by points with avatar, name, points, and badge count.
struct LeaderboardView: View {
    @Environment(\.dismiss) private var dismiss

    @State private var selectedFilter: LeaderboardFilter = .global
    @State private var isRefreshing = false

    // Mock data - in production, this comes from GamificationService / server API
    @State private var entries: [LeaderboardEntryData] = LeaderboardEntryData.mockData

    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                // Filter tabs
                filterTabs

                // Content
                ScrollView {
                    LazyVStack(spacing: 8) {
                        // Podium for top 3
                        if entries.count >= 3 {
                            podiumView
                                .padding(.vertical, 16)
                        }

                        // Remaining entries
                        let remainingEntries = entries.count > 3 ? Array(entries.dropFirst(3)) : entries
                        ForEach(remainingEntries) { entry in
                            leaderboardRow(entry: entry)
                        }
                    }
                    .padding(.horizontal, 16)
                    .padding(.bottom, 16)
                }
                .refreshable {
                    await refreshData()
                }
            }
            .navigationTitle("Classement")
            .navigationBarTitleDisplayMode(.large)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button("Fermer") {
                        dismiss()
                    }
                }
            }
        }
    }

    // MARK: - Filter Tabs

    private var filterTabs: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                ForEach(LeaderboardFilter.allCases) { filter in
                    Button(action: {
                        withAnimation(.easeInOut(duration: 0.2)) {
                            selectedFilter = filter
                        }
                    }) {
                        Text(filter.displayName)
                            .font(.subheadline)
                            .fontWeight(selectedFilter == filter ? .semibold : .regular)
                            .padding(.horizontal, 16)
                            .padding(.vertical, 8)
                            .background(
                                selectedFilter == filter
                                    ? Color.blue.opacity(0.15)
                                    : Color.gray.opacity(0.1)
                            )
                            .foregroundColor(selectedFilter == filter ? .blue : .secondary)
                            .clipShape(Capsule())
                    }
                }
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 8)
        }
    }

    // MARK: - Podium View

    private var podiumView: some View {
        HStack(alignment: .bottom, spacing: 8) {
            // 2nd place
            if entries.count > 1 {
                podiumEntry(entry: entries[1], medal: "\u{1F948}", height: 80)
            }

            // 1st place
            if entries.count > 0 {
                podiumEntry(entry: entries[0], medal: "\u{1F947}", height: 100)
            }

            // 3rd place
            if entries.count > 2 {
                podiumEntry(entry: entries[2], medal: "\u{1F949}", height: 60)
            }
        }
    }

    private func podiumEntry(entry: LeaderboardEntryData, medal: String, height: CGFloat) -> some View {
        VStack(spacing: 4) {
            Text(medal)
                .font(.title)

            // Avatar
            ZStack {
                Circle()
                    .fill(
                        LinearGradient(
                            gradient: Gradient(colors: medalColors(for: entry.rank)),
                            startPoint: .topLeading,
                            endPoint: .bottomTrailing
                        )
                    )
                    .frame(width: 44, height: 44)

                Text(String(entry.username.prefix(1)).uppercased())
                    .font(.headline)
                    .fontWeight(.bold)
                    .foregroundColor(.white)
            }

            Text(entry.isCurrentUser ? "Vous" : entry.username.components(separatedBy: " ").first ?? "")
                .font(.caption)
                .fontWeight(entry.isCurrentUser ? .bold : .medium)
                .foregroundColor(.primary)
                .lineLimit(1)

            Text(formatPoints(entry.totalPoints))
                .font(.caption)
                .fontWeight(.bold)
                .foregroundColor(.blue)

            Text("\(entry.badgesCount) badges")
                .font(.caption2)
                .foregroundColor(.secondary)

            RoundedRectangle(cornerRadius: 8)
                .fill(medalColors(for: entry.rank).first!.opacity(0.2))
                .frame(height: height)
        }
        .frame(maxWidth: .infinity)
    }

    // MARK: - Leaderboard Row

    private func leaderboardRow(entry: LeaderboardEntryData) -> some View {
        HStack(spacing: 16) {
            // Rank
            Text("#\(entry.rank)")
                .font(.headline)
                .fontWeight(.bold)
                .foregroundColor(.secondary)
                .frame(width: 36)

            // Avatar
            ZStack {
                Circle()
                    .fill(
                        LinearGradient(
                            gradient: Gradient(colors: [.blue.opacity(0.5), .purple.opacity(0.5)]),
                            startPoint: .topLeading,
                            endPoint: .bottomTrailing
                        )
                    )
                    .frame(width: 40, height: 40)

                Text(String(entry.username.prefix(1)).uppercased())
                    .font(.subheadline)
                    .fontWeight(.bold)
                    .foregroundColor(.white)
            }

            // User info
            VStack(alignment: .leading, spacing: 2) {
                HStack(spacing: 4) {
                    Text(entry.isCurrentUser ? "Vous" : entry.username)
                        .font(.subheadline)
                        .fontWeight(entry.isCurrentUser ? .bold : .medium)
                        .foregroundColor(.primary)

                    if entry.isFriend {
                        Image(systemName: "person.2.fill")
                            .font(.caption2)
                            .foregroundColor(.blue)
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
            RoundedRectangle(cornerRadius: 12)
                .fill(entry.isCurrentUser ? Color.blue.opacity(0.08) : Color.clear)
        )
    }

    // MARK: - Helpers

    private func medalColors(for rank: Int) -> [Color] {
        switch rank {
        case 1: return [Color(red: 1, green: 0.84, blue: 0), Color(red: 0.85, green: 0.65, blue: 0)]
        case 2: return [Color(red: 0.75, green: 0.75, blue: 0.75), Color(red: 0.6, green: 0.6, blue: 0.6)]
        case 3: return [Color(red: 0.8, green: 0.5, blue: 0.2), Color(red: 0.6, green: 0.4, blue: 0.15)]
        default: return [.blue, .purple]
        }
    }

    private func formatPoints(_ points: Int) -> String {
        if points >= 1_000_000 { return "\(points / 1_000_000)M" }
        if points >= 1_000 { return "\(points / 1_000)k" }
        return "\(points)"
    }

    private func refreshData() async {
        // Simulate network refresh
        try? await Task.sleep(nanoseconds: 500_000_000)
        // In production, re-fetch from GamificationService
    }
}

// MARK: - Leaderboard Filter

enum LeaderboardFilter: String, CaseIterable, Identifiable {
    case global
    case thisMonth
    case thisWeek
    case friends

    var id: String { rawValue }

    var displayName: String {
        switch self {
        case .global: return "Global"
        case .thisMonth: return "Ce mois"
        case .thisWeek: return "Cette semaine"
        case .friends: return "Amis"
        }
    }
}

// MARK: - Leaderboard Entry Data

struct LeaderboardEntryData: Identifiable {
    let id: String
    let username: String
    let totalPoints: Int
    let badgesCount: Int
    let rank: Int
    let isCurrentUser: Bool
    let isFriend: Bool

    static let mockData: [LeaderboardEntryData] = [
        LeaderboardEntryData(id: "user-1", username: "Alice Martin", totalPoints: 2450, badgesCount: 12, rank: 1, isCurrentUser: false, isFriend: false),
        LeaderboardEntryData(id: "user-2", username: "Bob Dupont", totalPoints: 2100, badgesCount: 10, rank: 2, isCurrentUser: false, isFriend: true),
        LeaderboardEntryData(id: "user-3", username: "Claire Bernard", totalPoints: 1950, badgesCount: 9, rank: 3, isCurrentUser: false, isFriend: false),
        LeaderboardEntryData(id: "user-current", username: "Vous", totalPoints: 1250, badgesCount: 8, rank: 4, isCurrentUser: true, isFriend: false),
        LeaderboardEntryData(id: "user-5", username: "David Leroy", totalPoints: 1100, badgesCount: 7, rank: 5, isCurrentUser: false, isFriend: true),
        LeaderboardEntryData(id: "user-6", username: "Emma Moreau", totalPoints: 980, badgesCount: 6, rank: 6, isCurrentUser: false, isFriend: false),
        LeaderboardEntryData(id: "user-7", username: "Frank Rousseau", totalPoints: 850, badgesCount: 5, rank: 7, isCurrentUser: false, isFriend: false),
        LeaderboardEntryData(id: "user-8", username: "Grace Wang", totalPoints: 720, badgesCount: 4, rank: 8, isCurrentUser: false, isFriend: true),
        LeaderboardEntryData(id: "user-9", username: "Henry Chen", totalPoints: 650, badgesCount: 4, rank: 9, isCurrentUser: false, isFriend: false),
        LeaderboardEntryData(id: "user-10", username: "Isabelle Dubois", totalPoints: 580, badgesCount: 3, rank: 10, isCurrentUser: false, isFriend: false)
    ]
}

// MARK: - Previews

#Preview("Leaderboard") {
    LeaderboardView()
}

#Preview("Leaderboard Dark") {
    LeaderboardView()
        .preferredColorScheme(.dark)
}
