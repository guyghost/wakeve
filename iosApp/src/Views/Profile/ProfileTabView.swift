import SwiftUI
import UserNotifications
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

                        OrganizerUtilitySection()
                        ProfileReliabilitySection()
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
        }
    }
}

// MARK: - Organizer Utility Section

struct OrganizerUtilitySection: View {
    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(String(localized: "profile.organizer"))
                .font(.headline)
                .foregroundColor(.primary)

            ProfileCard {
                VStack(spacing: WakeveTheme.Spacing.sm) {
                    OrganizerUtilityRow(
                        icon: "chart.line.uptrend.xyaxis",
                        title: String(localized: "profile.organizer.dashboard_title"),
                        detail: String(localized: "profile.organizer.dashboard_detail")
                    )

                    Divider()

                    OrganizerUtilityRow(
                        icon: "person.2.fill",
                        title: String(localized: "profile.organizer.guest_status_title"),
                        detail: String(localized: "profile.organizer.guest_status_detail")
                    )
                }
            }
        }
        .accessibilityIdentifier("profileOrganizerUtilitySection")
    }
}

private struct OrganizerUtilityRow: View {
    let icon: String
    let title: String
    let detail: String

    var body: some View {
        HStack(alignment: .top, spacing: WakeveTheme.Spacing.sm) {
            Image(systemName: icon)
                .font(.system(size: 16, weight: .semibold))
                .foregroundColor(WakeveTheme.ColorToken.permissionBlue)
                .frame(width: 30, height: 30)

            VStack(alignment: .leading, spacing: 3) {
                Text(title)
                    .font(WakeveTheme.Typography.bodySemibold)
                    .foregroundColor(.primary)

                Text(detail)
                    .font(WakeveTheme.Typography.metadata)
                    .foregroundColor(.secondary)
                    .fixedSize(horizontal: false, vertical: true)
            }
        }
    }
}

// MARK: - Profile Reliability Section

struct ProfileReliabilitySection: View {
    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(String(localized: "profile.reliability_title"))
                .font(.headline)
                .foregroundColor(.primary)

            ProfileCard {
                VStack(alignment: .leading, spacing: WakeveTheme.Spacing.md) {
                    Text(String(localized: "profile.reliability_subtitle"))
                        .font(WakeveTheme.Typography.body)
                        .foregroundColor(.secondary)
                        .fixedSize(horizontal: false, vertical: true)

                    Divider()

                    ProfileReliabilityRow(
                        icon: "link",
                        title: String(localized: "profile.reliability.invite_title"),
                        detail: String(localized: "profile.reliability.invite_detail")
                    )

                    ProfileReliabilityRow(
                        icon: "checklist.checked",
                        title: String(localized: "profile.reliability.decisions_title"),
                        detail: String(localized: "profile.reliability.decisions_detail")
                    )

                    ProfileReliabilityRow(
                        icon: "lock.shield.fill",
                        title: String(localized: "profile.reliability.privacy_title"),
                        detail: String(localized: "profile.reliability.privacy_detail")
                    )
                }
            }
        }
        .accessibilityIdentifier("profileReliabilitySection")
    }
}

private struct ProfileReliabilityRow: View {
    let icon: String
    let title: String
    let detail: String

    var body: some View {
        HStack(alignment: .top, spacing: WakeveTheme.Spacing.sm) {
            Image(systemName: icon)
                .font(.system(size: 16, weight: .semibold))
                .foregroundColor(WakeveTheme.ColorToken.permissionBlue)
                .frame(width: 30, height: 30)

            VStack(alignment: .leading, spacing: 3) {
                Text(title)
                    .font(WakeveTheme.Typography.bodySemibold)
                    .foregroundColor(.primary)

                Text(detail)
                    .font(WakeveTheme.Typography.metadata)
                    .foregroundColor(.secondary)
                    .fixedSize(horizontal: false, vertical: true)
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
            WakeveAvatar(initials: initials, size: 112, badgeSystemImage: "checkmark.shield.fill")
                .shadow(color: .black.opacity(0.18), radius: 20, x: 0, y: 10)

            VStack(spacing: WakeveTheme.Spacing.xs) {
                Text(displayName)
                    .font(WakeveTheme.Typography.largeTitle)
                    .fontWeight(.bold)
                    .lineLimit(2)
                    .minimumScaleFactor(0.74)

                if let displayEmail {
                    Text(displayEmail)
                        .font(WakeveTheme.Typography.metadata)
                        .foregroundColor(.secondary)
                        .lineLimit(1)
                        .minimumScaleFactor(0.8)
                }
            }

            Label(String(localized: "profile.identity_ready"), systemImage: "checkmark.seal.fill")
                .font(WakeveTheme.Typography.bodySemibold)
                .foregroundColor(SemanticColor.primaryText(for: colorScheme))
                .padding(.horizontal, WakeveTheme.Spacing.lg)
                .frame(height: 44)
                .background(SemanticColor.badge(for: colorScheme))
                .clipShape(Capsule())
                .accessibilityIdentifier("profileIdentityReadyPill")
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
    @State private var notificationSystemPermission: UNAuthorizationStatus = .notDetermined

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
                            value: notificationPreferenceSummary
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
        .onAppear(perform: refreshNotificationPermission)
    }

    private var notificationPreferenceSummary: String {
        String(
            format: String(localized: "settings_sheet.notifications_summary_format"),
            notificationsEnabled ? String(localized: "settings_sheet.wakeve_enabled") : String(localized: "settings_sheet.wakeve_disabled"),
            notificationSystemPermissionLabel
        )
    }

    private var notificationSystemPermissionLabel: String {
        switch notificationSystemPermission {
        case .authorized, .provisional, .ephemeral:
            return String(localized: "settings_sheet.ios_allowed")
        case .denied:
            return String(localized: "settings_sheet.ios_denied")
        case .notDetermined:
            return String(localized: "settings_sheet.ios_not_requested")
        @unknown default:
            return String(localized: "settings_sheet.ios_unknown")
        }
    }

    private func refreshNotificationPermission() {
        APNsService.shared.checkAuthorizationStatus { status in
            notificationSystemPermission = status
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

                    AboutLinkRow(
                        icon: "questionmark.circle.fill",
                        title: String(localized: "settings_sheet.help"),
                        action: {
                            openExternalURL("https://wakeve.app/support")
                        }
                    )

                    Divider()

                    AboutLinkRow(
                        icon: "hand.raised.fill",
                        title: String(localized: "settings_sheet.privacy"),
                        action: {
                            openExternalURL("https://wakeve.app/privacy")
                        }
                    )

                    Divider()

                    AboutLinkRow(
                        icon: "doc.text.fill",
                        title: String(localized: "settings_sheet.terms"),
                        action: {
                            openExternalURL("https://wakeve.app/terms")
                        }
                    )

                    Divider()

                    AboutLinkRow(
                        icon: "doc.append.fill",
                        title: String(localized: "profile.third_party_notices"),
                        action: {
                            openExternalURL("https://wakeve.app/third-party-notices")
                        }
                    )
                }
            }
        }
    }

    private func openExternalURL(_ rawValue: String) {
        guard let url = URL(string: rawValue) else {
            return
        }

        #if canImport(UIKit)
        UIApplication.shared.open(url)
        #endif
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

// MARK: - Profile Card

struct ProfileCard<Content: View>: View {
    let content: Content

    init(@ViewBuilder content: () -> Content) {
        self.content = content()
    }

    var body: some View {
        WakeveContentCard(cornerRadius: WakeveTheme.Radius.xl) {
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
