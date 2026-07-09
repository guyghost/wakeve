import SwiftUI

// MARK: - Leaderboard View

/// Full leaderboard screen with filtering and pull-to-refresh.
/// Shows top participants ranked by points with avatar, name, points, and badge count.
struct LeaderboardView: View {
    @Environment(\.dismiss) private var dismiss
    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    @State private var selectedFilter: LeaderboardFilter = .global
    @State private var entries: [LeaderboardEntryData]

    init(entries: [LeaderboardEntryData] = []) {
        _entries = State(initialValue: entries)
    }

    var body: some View {
        NavigationStack {
            ZStack {
                WakeveScreenBackground(style: .grouped)

                VStack(spacing: 0) {
                    filterTabs

                    ScrollView {
                        LazyVStack(spacing: WakeveTheme.Spacing.md) {
                            if entries.isEmpty {
                                leaderboardEmptyState
                            } else {
                                if entries.count >= 3 {
                                    podiumView
                                        .padding(.vertical, WakeveTheme.Spacing.md)
                                }

                                let remainingEntries = entries.count > 3 ? Array(entries.dropFirst(3)) : entries
                                ForEach(remainingEntries) { entry in
                                    leaderboardRow(entry: entry)
                                }
                            }
                        }
                        .padding(WakeveTheme.Spacing.page)
                    }
                    .refreshable {
                        await refreshData()
                    }
                }
            }
            .navigationTitle(String(localized: "leaderboard.title"))
            .navigationBarTitleDisplayMode(.large)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button(String(localized: "leaderboard.close")) {
                        dismiss()
                    }
                }
            }
        }
    }

    private var leaderboardEmptyState: some View {
        WakeveContentCard(prominence: .prominent, cornerRadius: WakeveTheme.Radius.xl) {
            VStack(alignment: .leading, spacing: WakeveTheme.Spacing.lg) {
                HStack(alignment: .top, spacing: WakeveTheme.Spacing.md) {
                    Image(systemName: "chart.line.uptrend.xyaxis")
                        .font(.system(size: 24, weight: .semibold))
                        .foregroundColor(WakeveTheme.ColorToken.permissionBlue)
                        .frame(width: 48, height: 48)
                        .background(WakeveTheme.ColorToken.permissionBlue.opacity(0.12), in: Circle())

                    VStack(alignment: .leading, spacing: WakeveTheme.Spacing.xs) {
                        Text(String(localized: "leaderboard.empty.title"))
                            .font(WakeveTheme.Typography.title2)
                            .foregroundColor(WakeveTheme.ColorToken.primaryText(for: colorScheme))
                            .fixedSize(horizontal: false, vertical: true)

                        Text(String(localized: "leaderboard.empty.subtitle"))
                            .font(WakeveTheme.Typography.body)
                            .foregroundColor(WakeveTheme.ColorToken.secondaryText(for: colorScheme))
                            .fixedSize(horizontal: false, vertical: true)
                    }
                }

                VStack(spacing: WakeveTheme.Spacing.sm) {
                    LeaderboardEmptyMetric(
                        icon: "person.2.badge.gearshape.fill",
                        title: String(localized: "leaderboard.empty.activity_title"),
                        detail: String(localized: "leaderboard.empty.activity_detail")
                    )

                    LeaderboardEmptyMetric(
                        icon: "lock.shield.fill",
                        title: String(localized: "leaderboard.empty.privacy_title"),
                        detail: String(localized: "leaderboard.empty.privacy_detail")
                    )

                    LeaderboardEmptyMetric(
                        icon: "calendar.badge.plus",
                        title: String(localized: "leaderboard.empty.first_event_title"),
                        detail: String(localized: "leaderboard.empty.first_event_detail")
                    )
                }
            }
        }
        .accessibilityIdentifier("leaderboardEmptyState")
    }

    // MARK: - Filter Tabs

    private var filterTabs: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                ForEach(LeaderboardFilter.allCases) { filter in
                    Button(action: {
                        withAnimation(reduceMotion ? nil : .easeInOut(duration: 0.2)) {
                            selectedFilter = filter
                        }
                    }) {
                        Text(filter.displayName)
                            .font(.subheadline)
                            .fontWeight(selectedFilter == filter ? .semibold : .regular)
                            .padding(.horizontal, WakeveTheme.Spacing.md)
                            .padding(.vertical, 8)
                            .background(
                                selectedFilter == filter
                                    ? WakeveTheme.ColorToken.permissionBlue.opacity(0.15)
                                    : WakeveTheme.ColorToken.controlFill(for: colorScheme)
                            )
                            .foregroundColor(selectedFilter == filter ? WakeveTheme.ColorToken.permissionBlue : .secondary)
                            .clipShape(Capsule())
                    }
                }
            }
            .padding(.horizontal, WakeveTheme.Spacing.page)
            .padding(.vertical, WakeveTheme.Spacing.sm)
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

            Text(entry.isCurrentUser ? String(localized: "leaderboard.you") : entry.username.components(separatedBy: " ").first ?? "")
                .font(.caption)
                .fontWeight(entry.isCurrentUser ? .bold : .medium)
                .foregroundColor(.primary)
                .lineLimit(1)
                .minimumScaleFactor(0.78)

            Text(formatPoints(entry.totalPoints))
                .font(.caption)
                .fontWeight(.bold)
                .foregroundColor(WakeveTheme.ColorToken.permissionBlue)

            Text(String(format: String(localized: "leaderboard.badges_count"), entry.badgesCount))
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
                    Text(entry.isCurrentUser ? String(localized: "leaderboard.you") : entry.username)
                        .font(.subheadline)
                        .fontWeight(entry.isCurrentUser ? .bold : .medium)
                        .foregroundColor(.primary)

                    if entry.isFriend {
                        Image(systemName: "person.2.fill")
                            .font(.caption2)
                            .foregroundColor(WakeveTheme.ColorToken.permissionBlue)
                    }
                }

                Text(String(format: String(localized: "leaderboard.badges_count"), entry.badgesCount))
                    .font(.caption)
                    .foregroundColor(.secondary)
            }

            Spacer()

            // Points
            VStack(alignment: .trailing, spacing: 2) {
                Text(formatPoints(entry.totalPoints))
                    .font(.headline)
                    .fontWeight(.bold)
                    .foregroundColor(WakeveTheme.ColorToken.permissionBlue)

                Text(String(localized: "leaderboard.points"))
                    .font(.caption2)
                    .foregroundColor(.secondary)
            }
        }
        .padding(12)
        .background(
            RoundedRectangle(cornerRadius: 12)
                .fill(entry.isCurrentUser ? WakeveTheme.ColorToken.permissionBlue.opacity(0.08) : Color.clear)
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
        await Task.yield()
    }
}

private struct LeaderboardEmptyMetric: View {
    let icon: String
    let title: String
    let detail: String

    @Environment(\.colorScheme) private var colorScheme

    var body: some View {
        HStack(alignment: .top, spacing: WakeveTheme.Spacing.sm) {
            Image(systemName: icon)
                .font(.system(size: 16, weight: .semibold))
                .foregroundColor(WakeveTheme.ColorToken.permissionBlue)
                .frame(width: 30, height: 30)

            VStack(alignment: .leading, spacing: 3) {
                Text(title)
                    .font(WakeveTheme.Typography.bodySemibold)
                    .foregroundColor(WakeveTheme.ColorToken.primaryText(for: colorScheme))

                Text(detail)
                    .font(WakeveTheme.Typography.metadata)
                    .foregroundColor(WakeveTheme.ColorToken.secondaryText(for: colorScheme))
                    .fixedSize(horizontal: false, vertical: true)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
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
        case .global: return String(localized: "leaderboard.filter.global")
        case .thisMonth: return String(localized: "leaderboard.filter.this_month")
        case .thisWeek: return String(localized: "leaderboard.filter.this_week")
        case .friends: return String(localized: "leaderboard.filter.friends")
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
}

// MARK: - Previews

#Preview("Leaderboard Light") {
    LeaderboardView()
        .preferredColorScheme(.light)
}

#Preview("Leaderboard Dark") {
    LeaderboardView()
        .preferredColorScheme(.dark)
}
