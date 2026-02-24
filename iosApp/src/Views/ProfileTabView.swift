import SwiftUI
#if canImport(UIKit)
import UIKit
#endif

// MARK: - Profile Tab View

struct ProfileTabView: View {
    let userId: String
    @EnvironmentObject var authStateManager: AuthStateManager

    @AppStorage("darkMode") private var darkMode = false
    @AppStorage("notificationsEnabled") private var notificationsEnabled = true

    @State private var showLeaderboard = false
    @State private var showDashboard = false

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 24) {
                    // Profile Header
                    ProfileHeaderSection()

                    // Organizer Dashboard Link
                    DashboardLinkSection(showDashboard: $showDashboard)

                    // Gamification: Points & Level
                    GamificationSummarySection()

                    // Gamification: Badges
                    ProfileTabBadgesSection()

                    // Leaderboard Link
                    LeaderboardLinkSection(showLeaderboard: $showLeaderboard)

                    // Preferences Section
                    PreferencesSection()

                    // Appearance Section
                    AppearanceSection()

                    // About Section
                    AboutSection()

                    // Sign Out Button
                    SignOutButton()
                }
                .padding()
            }
            .navigationTitle(String(localized: "profile.title"))
            .preferredColorScheme(darkMode ? .dark : .light)
            .sheet(isPresented: $showLeaderboard) {
                LeaderboardView()
            }
            .sheet(isPresented: $showDashboard) {
                NavigationStack {
                    OrganizerDashboardView()
                }
            }
        }
    }
}

// MARK: - Dashboard Link Section

struct DashboardLinkSection: View {
    @Binding var showDashboard: Bool

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(String(localized: "profile.organizer"))
                .font(.headline)
                .foregroundColor(.primary)

            ProfileCard {
                Button(action: {
                    showDashboard = true
                }) {
                    HStack(spacing: 16) {
                        Image(systemName: "chart.line.uptrend.xyaxis")
                            .font(.title2)
                            .foregroundColor(.purple)
                            .frame(width: 32, height: 32)

                        VStack(alignment: .leading, spacing: 2) {
                            Text(String(localized: "profile.dashboard"))
                                .font(.subheadline)
                                .fontWeight(.semibold)
                                .foregroundColor(.primary)

                            Text(String(localized: "profile.dashboard_subtitle"))
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }

                        Spacer()

                        Image(systemName: "chevron.right")
                            .font(.system(size: 14, weight: .semibold))
                            .foregroundColor(.secondary)
                    }
                    .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
            }
        }
    }
}

// MARK: - Profile Header Section

struct ProfileHeaderSection: View {
    var body: some View {
        VStack(spacing: 16) {
            // Avatar
            ZStack {
                Circle()
                    .fill(
                        LinearGradient(
                            gradient: Gradient(colors: [
                                Color(red: 0.15, green: 0.39, blue: 0.92), // Blue
                                Color(red: 0.49, green: 0.23, blue: 0.93)  // Purple
                            ]),
                            startPoint: .topLeading,
                            endPoint: .bottomTrailing
                        )
                    )
                    .frame(width: 80, height: 80)

                Image(systemName: "person.fill")
                    .font(.system(size: 40))
                    .foregroundColor(.white)
            }

            // User Info
            VStack(spacing: 4) {
                Text("John Doe")
                    .font(.title3)
                    .fontWeight(.bold)

                Text("john.doe@example.com")
                    .font(.subheadline)
                    .foregroundColor(.secondary)
            }
        }
        .padding(.vertical, 8)
    }
}

// MARK: - Preferences Section

struct PreferencesSection: View {
    @AppStorage("notificationsEnabled") private var notificationsEnabled = true

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(String(localized: "profile.preferences"))
                .font(.headline)
                .foregroundColor(.primary)

            ProfileCard {
                VStack(spacing: 0) {
                    // Notifications Toggle
                    PreferenceToggleRow(
                        icon: "bell.fill",
                        title: String(localized: "profile.push_notifications"),
                        description: String(localized: "profile.push_notifications_desc"),
                        isOn: $notificationsEnabled
                    )
                }
            }
        }
    }
}

// MARK: - Appearance Section

struct AppearanceSection: View {
    @AppStorage("darkMode") private var darkMode = false

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(String(localized: "profile.appearance"))
                .font(.headline)
                .foregroundColor(.primary)

            ProfileCard {
                VStack(spacing: 0) {
                    // Dark Mode Toggle
                    PreferenceToggleRow(
                        icon: darkMode ? "moon.fill" : "sun.max.fill",
                        title: darkMode ? String(localized: "profile.dark_mode") : String(localized: "profile.light_mode"),
                        description: darkMode ? String(localized: "profile.dark_mode_desc") : String(localized: "profile.light_mode_desc"),
                        isOn: $darkMode
                    )
                }
            }
        }
    }
}

// MARK: - About Section

struct AboutSection: View {
    @State private var showSettings = false
    
    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(String(localized: "profile.about"))
                .font(.headline)
                .foregroundColor(.primary)

            ProfileCard {
                VStack(spacing: 0) {
                    // Settings Link
                    AboutLinkRow(
                        icon: "gearshape.fill",
                        title: String(localized: "profile.settings"),
                        action: {
                            showSettings = true
                        }
                    )
                    
                    Divider()
                    
                    // Version
                    AboutRow(
                        icon: "info.circle.fill",
                        title: String(localized: "profile.version"),
                        value: "1.0.0"
                    )

                    Divider()

                    // Documentation Link
                    AboutLinkRow(
                        icon: "book.fill",
                        title: String(localized: "profile.documentation"),
                        action: {
                            // Open documentation URL
                            if let url = URL(string: "https://github.com/guyghost/wakeve") {
                                #if canImport(UIKit)
                                UIApplication.shared.open(url)
                                #endif
                            }
                        }
                    )

                    Divider()

                    // GitHub Link
                    AboutLinkRow(
                        icon: "link.circle.fill",
                        title: "GitHub",
                        action: {
                            // Open GitHub URL
                            if let url = URL(string: "https://github.com/guyghost/wakeve") {
                                #if canImport(UIKit)
                                UIApplication.shared.open(url)
                                #endif
                            }
                        }
                    )
                }
            }
        }
        .sheet(isPresented: $showSettings) {
            SettingsView()
        }
    }
}

// MARK: - Profile Card (Simplified Liquid Glass)

struct ProfileCard<Content: View>: View {
    let content: Content

    init(@ViewBuilder content: () -> Content) {
        self.content = content()
    }

    var body: some View {
        if #available(iOS 26.0, *) {
            content
                .padding()
                .glassEffect()
                .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
        } else {
            // Fallback for iOS < 26
            content
                .padding()
                .background(.regularMaterial)
                .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
                .shadow(color: .black.opacity(0.05), radius: 8, x: 0, y: 4)
        }
    }
}

// MARK: - Preference Toggle Row

struct PreferenceToggleRow: View {
    let icon: String
    let title: String
    let description: String
    @Binding var isOn: Bool

    var body: some View {
        HStack(spacing: 16) {
            // Icon
            Image(systemName: icon)
                .font(.title3)
                .foregroundColor(.blue)
                .frame(width: 32, height: 32)

            // Content
            VStack(alignment: .leading, spacing: 4) {
                Text(title)
                    .font(.subheadline)
                    .fontWeight(.semibold)
                    .foregroundColor(.primary)

                Text(description)
                    .font(.caption)
                    .foregroundColor(.secondary)
                    .lineLimit(2)
            }

            Spacer()

            // Toggle
            Toggle("", isOn: $isOn)
                .labelsHidden()
        }
        .padding(.vertical, 12)
        .contentShape(Rectangle())
    }
}

// MARK: - About Row

struct AboutRow: View {
    let icon: String
    let title: String
    let value: String

    var body: some View {
        HStack(spacing: 16) {
            // Icon
            Image(systemName: icon)
                .font(.title3)
                .foregroundColor(.purple)
                .frame(width: 32, height: 32)

            // Content
            VStack(alignment: .leading, spacing: 4) {
                Text(title)
                    .font(.caption)
                    .foregroundColor(.secondary)

                Text(value)
                    .font(.subheadline)
                    .fontWeight(.medium)
                    .foregroundColor(.primary)
            }

            Spacer()
        }
        .padding(.vertical, 12)
    }
}

// MARK: - About Link Row

struct AboutLinkRow: View {
    let icon: String
    let title: String
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 16) {
                // Icon
                Image(systemName: icon)
                    .font(.title3)
                    .foregroundColor(.green)
                    .frame(width: 32, height: 32)

                // Content
                Text(title)
                    .font(.subheadline)
                    .fontWeight(.medium)
                    .foregroundColor(.primary)

                Spacer()

                // Chevron
                Image(systemName: "chevron.right")
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundColor(.secondary)
            }
            .padding(.vertical, 12)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }
}

// MARK: - Sign Out Button

struct SignOutButton: View {
    @EnvironmentObject var authStateManager: AuthStateManager

    var body: some View {
        Button(action: {
            authStateManager.signOut()
        }) {
            HStack(spacing: 8) {
                Image(systemName: "rectangle.portrait.and.arrow.right")
                Text(String(localized: "auth.sign_out"))
            }
            .font(.headline)
            .foregroundColor(.white)
            .padding(.horizontal, 32)
            .padding(.vertical, 16)
            .background(Color(red: 0.86, green: 0.15, blue: 0.15)) // Error red
            .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
        }
    }
}

// MARK: - Gamification Summary Section

struct GamificationSummarySection: View {
    // Mock data - in production, this comes from GamificationService
    private let totalPoints = 1250
    private let level = 7
    private let levelName = "Champion"
    private let levelProgress: Double = 0.25 // 1250 of 1200-1800 range => (1250-1200)/(1800-1200) = 50/600
    private let pointsToNextLevel = 1800

    private let eventCreationPoints = 500
    private let votingPoints = 300
    private let commentPoints = 250
    private let participationPoints = 200

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(String(localized: "gamification.points_and_level"))
                .font(.headline)
                .foregroundColor(.primary)

            ProfileCard {
                VStack(spacing: 16) {
                    // Total Points + Level
                    HStack {
                        VStack(alignment: .leading, spacing: 4) {
                            Text(String(localized: "gamification.total_points"))
                                .font(.subheadline)
                                .foregroundColor(.secondary)
                            Text("\(totalPoints)")
                                .font(.system(size: 32, weight: .bold, design: .rounded))
                                .foregroundColor(.blue)
                        }

                        Spacer()

                        // Level badge
                        VStack(spacing: 2) {
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
                                    .font(.title2)
                                    .foregroundColor(.white)
                            }

                            Text(String(format: String(localized: "gamification.level_short"), level))
                                .font(.caption2)
                                .fontWeight(.bold)
                                .foregroundColor(.blue)
                        }
                    }

                    // Level name + progress bar
                    VStack(alignment: .leading, spacing: 6) {
                        HStack {
                            Text(levelName)
                                .font(.subheadline)
                                .fontWeight(.semibold)
                                .foregroundColor(.primary)

                            Spacer()

                            Text("\(totalPoints) / \(pointsToNextLevel) pts")
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
                                            gradient: Gradient(colors: [.blue, .purple]),
                                            startPoint: .leading,
                                            endPoint: .trailing
                                        )
                                    )
                                    .frame(width: geometry.size.width * levelProgress, height: 8)
                                    .animation(.easeInOut(duration: 0.8), value: levelProgress)
                            }
                        }
                        .frame(height: 8)
                    }

                    Divider()

                    // Points breakdown
                    PointsBreakdownRow(label: String(localized: "gamification.event_creation"), points: eventCreationPoints, color: .red.opacity(0.8))
                    PointsBreakdownRow(label: String(localized: "gamification.voting"), points: votingPoints, color: .teal)
                    PointsBreakdownRow(label: String(localized: "gamification.commenting"), points: commentPoints, color: .yellow.opacity(0.8))
                    PointsBreakdownRow(label: String(localized: "gamification.participation"), points: participationPoints, color: .green.opacity(0.7))
                }
            }
        }
    }
}

struct PointsBreakdownRow: View {
    let label: String
    let points: Int
    let color: Color

    var body: some View {
        HStack {
            Circle()
                .fill(color)
                .frame(width: 10, height: 10)

            Text(label)
                .font(.subheadline)
                .foregroundColor(.primary)

            Spacer()

            Text("\(points)")
                .font(.subheadline)
                .fontWeight(.semibold)
                .foregroundColor(color)
        }
    }
}

// MARK: - Badges Section

fileprivate struct ProfileTabBadgesSection: View {
    // Mock data - in production, this comes from GamificationService
    private var earnedBadges: [(id: String, name: String, icon: String, rarity: String)] {
        [
            ("badge-first-event", String(localized: "gamification.badge.first_event"), "\u{1F389}", "common"),
            ("badge-super-organizer", String(localized: "gamification.badge.super_organizer"), "\u{1F3C6}", "epic"),
            ("badge-early-bird", String(localized: "gamification.badge.early_bird"), "\u{1F426}", "rare"),
            ("badge-voting-master", String(localized: "gamification.badge.voting_master"), "\u{1F5F3}\u{FE0F}", "rare"),
            ("badge-active-participant", String(localized: "gamification.badge.active_participant"), "\u{1F64B}", "common"),
            ("badge-event-master", String(localized: "gamification.badge.event_master"), "\u{1F3AD}", "legendary"),
            ("badge-commentator", String(localized: "gamification.badge.commentator"), "\u{1F4AC}", "rare"),
            ("badge-dedicated", String(localized: "gamification.badge.dedicated"), "\u{2B50}", "rare")
        ]
    }

    private var lockedBadges: [(id: String, name: String, rarity: String)] {
        [
            ("badge-social-butterfly", String(localized: "gamification.badge.social_butterfly"), "epic"),
            ("badge-party-animal", String(localized: "gamification.badge.party_animal"), "legendary"),
            ("badge-century-club", String(localized: "gamification.badge.century_club"), "common"),
            ("badge-millenium-club", String(localized: "gamification.badge.millennium_club"), "epic")
        ]
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Image(systemName: "trophy.fill")
                    .foregroundColor(.orange)
                Text("Badges")
                    .font(.headline)
                    .foregroundColor(.primary)
                Spacer()
                Text("\(earnedBadges.count) / \(earnedBadges.count + lockedBadges.count)")
                    .font(.caption)
                    .foregroundColor(.secondary)
            }

            // Earned badges
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 12) {
                    ForEach(earnedBadges, id: \.id) { badge in
                        ProfileTabBadgeItemView(
                            name: badge.name,
                            icon: badge.icon,
                            rarity: badge.rarity,
                            isEarned: true
                        )
                    }

                    // Locked badges
                    ForEach(lockedBadges, id: \.id) { badge in
                        ProfileTabBadgeItemView(
                            name: badge.name,
                            icon: "\u{1F512}",
                            rarity: badge.rarity,
                            isEarned: false
                        )
                    }
                }
            }
        }
    }
}

fileprivate struct ProfileTabBadgeItemView: View {
    let name: String
    let icon: String
    let rarity: String
    let isEarned: Bool

    @State private var appeared = false

    private var rarityColor: Color {
        switch rarity {
        case "common": return .gray
        case "rare": return .blue
        case "epic": return .purple
        case "legendary": return .orange
        default: return .gray
        }
    }

    var body: some View {
        VStack(spacing: 8) {
            Text(icon)
                .font(.system(size: 32))
                .frame(width: 48, height: 48)

            Text(name)
                .font(.caption2)
                .fontWeight(.medium)
                .foregroundColor(isEarned ? .primary : .secondary)
                .multilineTextAlignment(.center)
                .lineLimit(2)

            Text(isEarned ? rarity.capitalized : String(localized: "gamification.locked"))
                .font(.caption2)
                .foregroundColor(isEarned ? rarityColor : .gray)
                .fontWeight(.medium)
        }
        .frame(width: 90, height: 110)
        .padding(8)
        .background(
            RoundedRectangle(cornerRadius: 12)
                .fill(isEarned ? rarityColor.opacity(0.1) : Color.gray.opacity(0.05))
        )
        .opacity(isEarned ? 1.0 : 0.5)
        .scaleEffect(appeared && isEarned ? 1.0 : 0.9)
        .animation(.spring(response: 0.4, dampingFraction: 0.7), value: appeared)
        .onAppear {
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.1) {
                appeared = true
            }
        }
    }
}

// MARK: - Leaderboard Link Section

struct LeaderboardLinkSection: View {
    @Binding var showLeaderboard: Bool

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(String(localized: "gamification.leaderboard"))
                .font(.headline)
                .foregroundColor(.primary)

            ProfileCard {
                Button(action: {
                    showLeaderboard = true
                }) {
                    HStack(spacing: 16) {
                        Image(systemName: "chart.bar.fill")
                            .font(.title2)
                            .foregroundColor(.blue)
                            .frame(width: 32, height: 32)

                        VStack(alignment: .leading, spacing: 2) {
                            Text(String(localized: "gamification.view_leaderboard"))
                                .font(.subheadline)
                                .fontWeight(.semibold)
                                .foregroundColor(.primary)

                            Text(String(localized: "gamification.leaderboard_subtitle"))
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }

                        Spacer()

                        Image(systemName: "chevron.right")
                            .font(.system(size: 14, weight: .semibold))
                            .foregroundColor(.secondary)
                    }
                    .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
            }
        }
    }
}

// MARK: - Preview

struct ProfileTabView_Previews: PreviewProvider {
    static var previews: some View {
        let authService = AuthenticationService()
        
        Group {
            ProfileTabView(userId: "user-1")
                .environmentObject(AuthStateManager(authService: authService))
                .previewDisplayName("Light Mode")
            
            ProfileTabView(userId: "user-1")
                .environmentObject(AuthStateManager(authService: authService))
                .preferredColorScheme(.dark)
                .previewDisplayName("Dark Mode")
        }
    }
}
