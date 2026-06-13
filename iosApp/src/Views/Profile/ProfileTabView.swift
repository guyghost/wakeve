import SwiftUI
#if canImport(UIKit)
import UIKit
#endif

// MARK: - Profile Tab View

struct ProfileTabView: View {
    let userId: String
    let userName: String?
    let userEmail: String?
    var onDismiss: (() -> Void)?
    var onSignOut: (() -> Void)?

    @AppStorage("darkMode") private var darkMode = false

    @State private var showLeaderboard = false
    @State private var showDashboard = false

    init(
        userId: String,
        userName: String? = nil,
        userEmail: String? = nil,
        onDismiss: (() -> Void)? = nil,
        onSignOut: (() -> Void)? = nil
    ) {
        self.userId = userId
        self.userName = userName
        self.userEmail = userEmail
        self.onDismiss = onDismiss
        self.onSignOut = onSignOut
    }

    var body: some View {
        NavigationStack {
            ZStack {
                WakeveScreenBackground(style: darkMode ? .profile : .grouped)

                ScrollView {
                    VStack(spacing: WakeveTheme.Spacing.xl) {
                        ProfileHeaderSection(userName: userName, userEmail: userEmail)

                        DashboardLinkSection(showDashboard: $showDashboard)
                        GamificationSummarySection()
                        ProfileTabBadgesSection()
                        LeaderboardLinkSection(showLeaderboard: $showLeaderboard)
                        PreferencesSection(userId: userId)
                        AppearanceSection()
                        AboutSection()
                        SignOutButton(onDismiss: onDismiss, onSignOut: onSignOut)
                    }
                    .padding(WakeveTheme.Spacing.page)
                    .padding(.top, WakeveTheme.Spacing.sm)
                }
            }
            .navigationTitle(String(localized: "profile.title"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbarBackground(.hidden, for: .navigationBar)
            .toolbar {
                if let onDismiss {
                    ToolbarItem(placement: .topBarTrailing) {
                        Button(action: onDismiss) {
                            Text(String(localized: "common.done"))
                                .font(WakeveTheme.Typography.bodySemibold)
                        }
                    }
                }
            }
            .sheet(isPresented: $showLeaderboard) {
                LeaderboardView()
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
    @Environment(\.colorScheme) private var colorScheme

    let userName: String?
    let userEmail: String?

    private var displayName: String {
        let trimmedName = userName?.trimmingCharacters(in: .whitespacesAndNewlines)
        return trimmedName?.isEmpty == false ? trimmedName! : String(localized: "profile.user")
    }

    private var displayEmail: String? {
        let trimmedEmail = userEmail?.trimmingCharacters(in: .whitespacesAndNewlines)
        return trimmedEmail?.isEmpty == false ? trimmedEmail : nil
    }

    private var initials: String {
        let words = displayName
            .split(separator: " ")
            .prefix(2)
            .compactMap { $0.first }

        if words.isEmpty {
            return "U"
        }

        return words.map { String($0) }.joined().uppercased()
    }

    var body: some View {
        VStack(spacing: WakeveTheme.Spacing.md) {
            WakeveAvatar(initials: initials, size: 112, badgeSystemImage: "sparkles")
                .shadow(color: .black.opacity(0.18), radius: 20, x: 0, y: 10)

            VStack(spacing: WakeveTheme.Spacing.xs) {
                Text(displayName)
                    .font(WakeveTheme.Typography.largeTitle)
                    .fontWeight(.bold)
                    .lineLimit(1)
                    .minimumScaleFactor(0.75)

                if let displayEmail {
                    Text(displayEmail)
                        .font(WakeveTheme.Typography.metadata)
                        .foregroundColor(.secondary)
                        .lineLimit(1)
                        .minimumScaleFactor(0.8)
                }
            }

            Text(String(localized: "profile.edit"))
                .font(WakeveTheme.Typography.bodySemibold)
                .foregroundColor(WakeveTheme.ColorToken.primaryText(for: colorScheme))
                .padding(.horizontal, WakeveTheme.Spacing.lg)
                .frame(height: 44)
                .background(WakeveTheme.ColorToken.controlFill(for: colorScheme))
                .clipShape(Capsule())
        }
        .padding(.vertical, WakeveTheme.Spacing.lg)
    }
}

// MARK: - Preferences Section

struct PreferencesSection: View {
    let userId: String

    @AppStorage("notificationsEnabled") private var notificationsEnabled = true
    @AppStorage("calendarSyncEnabled") private var calendarSyncEnabled = false
    @AppStorage("emailNotificationsEnabled") private var emailNotificationsEnabled = false

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

                    Divider()

                    NavigationLink {
                        NotificationPreferencesView(userId: userId)
                    } label: {
                        ProfileNavigationRow(
                            icon: "bell.badge.fill",
                            title: String(localized: "settings_sheet.notifications"),
                            value: notificationsEnabled ? String(localized: "settings_sheet.enabled") : String(localized: "settings_sheet.disabled")
                        )
                    }
                    .buttonStyle(.plain)

                    Divider()

                    PreferenceToggleRow(
                        icon: "calendar",
                        title: String(localized: "settings_sheet.calendar_sync"),
                        description: calendarSyncEnabled ? String(localized: "settings_sheet.enabled") : String(localized: "settings_sheet.disabled"),
                        isOn: $calendarSyncEnabled
                    )

                    Divider()

                    PreferenceToggleRow(
                        icon: "envelope.fill",
                        title: String(localized: "settings_sheet.email_notifications"),
                        description: emailNotificationsEnabled ? String(localized: "settings_sheet.enabled") : String(localized: "settings_sheet.disabled"),
                        isOn: $emailNotificationsEnabled
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
    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(String(localized: "profile.about"))
                .font(.headline)
                .foregroundColor(.primary)

            ProfileCard {
                VStack(spacing: 0) {
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

                    Divider()

                    NavigationLink {
                        DataManagementView()
                    } label: {
                        ProfileNavigationRow(
                            icon: "hand.raised.fill",
                            title: String(localized: "settings_sheet.data_management"),
                            value: String(localized: "settings_sheet.data_management_value")
                        )
                    }
                    .buttonStyle(.plain)

                    Divider()

                    ProfilePlainLinkRow(
                        icon: "questionmark.circle.fill",
                        title: String(localized: "settings_sheet.help")
                    )

                    Divider()

                    ProfilePlainLinkRow(
                        icon: "doc.text.fill",
                        title: String(localized: "settings_sheet.terms")
                    )
                }
            }
        }
    }
}

// MARK: - Data Management

struct DataManagementView: View {
    @EnvironmentObject private var authStateManager: AuthStateManager
    @Environment(\.colorScheme) private var colorScheme

    @State private var isDeleting = false
    @State private var showDeletionConfirmation = false
    @State private var deletionError: String?
    @State private var completionMessage: String?

    private var isGuest: Bool {
        authStateManager.isCurrentSessionGuest
    }

    private var destructiveTitle: String {
        isGuest
            ? String(localized: "data_management.delete_guest_data")
            : String(localized: "data_management.delete_account")
    }

    var body: some View {
        ZStack {
            WakeveScreenBackground(style: .grouped)

            ScrollView {
                VStack(alignment: .leading, spacing: WakeveTheme.Spacing.lg) {
                    VStack(alignment: .leading, spacing: WakeveTheme.Spacing.sm) {
                        Text(String(localized: "data_management.title"))
                            .font(WakeveTheme.Typography.title2)
                            .foregroundColor(.primary)

                        Text(String(localized: "data_management.subtitle"))
                            .font(WakeveTheme.Typography.body)
                            .foregroundColor(.secondary)
                    }

                    ProfileCard {
                        VStack(alignment: .leading, spacing: WakeveTheme.Spacing.md) {
                            Label(
                                String(localized: "data_management.scope_title"),
                                systemImage: "lock.shield.fill"
                            )
                            .font(WakeveTheme.Typography.bodySemibold)
                            .foregroundColor(.primary)

                            Text(isGuest
                                 ? String(localized: "data_management.guest_scope")
                                 : String(localized: "data_management.account_scope"))
                                .font(WakeveTheme.Typography.body)
                                .foregroundColor(.secondary)
                                .fixedSize(horizontal: false, vertical: true)
                        }
                        .padding(.vertical, WakeveTheme.Spacing.sm)
                    }

                    if let deletionError {
                        Text(deletionError)
                            .font(WakeveTheme.Typography.body)
                            .foregroundColor(WakeveColors.error)
                            .padding(WakeveTheme.Spacing.md)
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .background(WakeveColors.error.opacity(0.12))
                            .clipShape(RoundedRectangle(cornerRadius: WakeveTheme.Radius.md))
                    }

                    if let completionMessage {
                        Text(completionMessage)
                            .font(WakeveTheme.Typography.body)
                            .foregroundColor(WakeveColors.success)
                            .padding(WakeveTheme.Spacing.md)
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .background(WakeveColors.success.opacity(0.12))
                            .clipShape(RoundedRectangle(cornerRadius: WakeveTheme.Radius.md))
                    }

                    WakeveActionButton(
                        isDeleting ? String(localized: "common.loading") : destructiveTitle,
                        systemImage: isGuest ? "trash.fill" : "person.crop.circle.badge.xmark",
                        variant: .destructive
                    ) {
                        showDeletionConfirmation = true
                    }
                    .disabled(isDeleting)
                }
                .padding(WakeveTheme.Spacing.page)
            }
        }
        .navigationTitle(String(localized: "data_management.title"))
        .navigationBarTitleDisplayMode(.inline)
        .confirmationDialog(
            destructiveTitle,
            isPresented: $showDeletionConfirmation,
            titleVisibility: .visible
        ) {
            Button(destructiveTitle, role: .destructive) {
                Task {
                    await performDeletion()
                }
            }
            Button(String(localized: "common.cancel"), role: .cancel) {}
        } message: {
            Text(isGuest
                 ? String(localized: "data_management.confirm_guest_message")
                 : String(localized: "data_management.confirm_account_message"))
        }
    }

    private func performDeletion() async {
        isDeleting = true
        deletionError = nil
        completionMessage = nil
        defer {
            isDeleting = false
        }

        do {
            if isGuest {
                await authStateManager.deleteGuestData()
                completionMessage = String(localized: "data_management.guest_deleted")
            } else {
                try await authStateManager.deleteCurrentAccount()
                completionMessage = String(localized: "data_management.account_deleted")
            }
        } catch {
            deletionError = String(localized: "data_management.delete_error")
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
        WakeveGlassCard(cornerRadius: WakeveTheme.Radius.xl) {
            content
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
                .foregroundColor(WakeveTheme.ColorToken.permissionBlue)
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
                .tint(WakeveTheme.ColorToken.permissionBlue)
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
                .foregroundColor(WakeveTheme.ColorToken.permissionBlue)
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
                    .foregroundColor(WakeveColors.success)
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

// MARK: - Profile Navigation Row

struct ProfileNavigationRow: View {
    let icon: String
    let title: String
    let value: String

    var body: some View {
        HStack(spacing: 16) {
            Image(systemName: icon)
                .font(.title3)
                .foregroundColor(WakeveTheme.ColorToken.permissionBlue)
                .frame(width: 32, height: 32)

            Text(title)
                .font(.subheadline)
                .fontWeight(.semibold)
                .foregroundColor(.primary)

            Spacer(minLength: 12)

            Text(value)
                .font(.caption)
                .foregroundColor(.secondary)

            Image(systemName: "chevron.right")
                .font(.system(size: 14, weight: .semibold))
                .foregroundColor(.secondary)
        }
        .padding(.vertical, 12)
        .contentShape(Rectangle())
    }
}

// MARK: - Profile Plain Link Row

struct ProfilePlainLinkRow: View {
    let icon: String
    let title: String

    var body: some View {
        HStack(spacing: 16) {
            Image(systemName: icon)
                .font(.title3)
                .foregroundColor(WakeveColors.success)
                .frame(width: 32, height: 32)

            Text(title)
                .font(.subheadline)
                .fontWeight(.medium)
                .foregroundColor(.primary)
                .multilineTextAlignment(.leading)

            Spacer()
        }
        .padding(.vertical, 12)
        .contentShape(Rectangle())
    }
}

// MARK: - Sign Out Button

struct SignOutButton: View {
    var onDismiss: (() -> Void)?
    var onSignOut: (() -> Void)?

    @EnvironmentObject var authStateManager: AuthStateManager

    var body: some View {
        WakeveActionButton(
            String(localized: "auth.sign_out"),
            systemImage: "rectangle.portrait.and.arrow.right",
            variant: .destructive
        ) {
            onDismiss?()

            if let onSignOut {
                onSignOut()
            } else {
                authStateManager.signOut()
            }
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
                .preferredColorScheme(.light)
            
            ProfileTabView(userId: "user-1")
                .environmentObject(AuthStateManager(authService: authService))
                .preferredColorScheme(.dark)
        }
    }
}
