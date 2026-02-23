import SwiftUI
import Shared

// MARK: - Profile Settings Sheet

/// Settings bottom sheet style iOS that slides up from the bottom
/// Similar to iOS system Settings app with card-based layout
struct ProfileSettingsSheet: View {
    let userId: String
    let userName: String?
    let userEmail: String?
    
    var onDismiss: () -> Void
    var onSignOut: () -> Void
    
    @Environment(\.colorScheme) var colorScheme
    @State private var notificationsEnabled = false
    @State private var calendarSyncEnabled = false
    @State private var emailNotificationsEnabled = false
    
    var body: some View {
        NavigationView {
            ScrollView {
                VStack(spacing: 24) {
                    // Profile Card
                    profileCard
                        .padding(.horizontal, 16)
                    
                    // Settings Groups - Each item in separate card with spacing
                    VStack(spacing: 16) {
                        // Notifications
                        settingsGroup {
                            SettingsRow(
                                icon: "bell.fill",
                                title: String(localized: "settings_sheet.notifications"),
                                value: notificationsEnabled ? String(localized: "settings_sheet.enabled") : String(localized: "settings_sheet.disabled")
                            )
                        }
                        
                        // Calendar Sync
                        settingsGroup {
                            SettingsRow(
                                icon: "calendar",
                                title: String(localized: "settings_sheet.calendar_sync"),
                                value: calendarSyncEnabled ? String(localized: "common.yes") : String(localized: "common.no")
                            )
                        }
                        
                        // Email Notifications
                        settingsGroup {
                            SettingsRow(
                                icon: "envelope.fill",
                                title: String(localized: "settings_sheet.email_notifications"),
                                value: emailNotificationsEnabled ? String(localized: "settings_sheet.enabled") : String(localized: "settings_sheet.disabled")
                            )
                        }
                        
                        // Privacy Section
                        privacySection
                    }
                    .padding(.horizontal, 16)
                    
                    // Sign Out Button
                    Button(action: {
                        onDismiss()
                        onSignOut()
                    }) {
                        Text(String(localized: "auth.sign_out"))
                            .font(.system(size: 17, weight: .medium))
                            .foregroundColor(.red)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 16)
                            .background(Color(.secondarySystemGroupedBackground))
                            .cornerRadius(12)
                    }
                    .padding(.horizontal, 16)
                    
                    Spacer(minLength: 40)
                }
                .padding(.vertical, 16)
            }
            .background(Color(.systemGroupedBackground))
            .navigationTitle(String(localized: "settings_sheet.title"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(action: onDismiss) {
                        Image(systemName: "checkmark")
                            .font(.system(size: 16, weight: .semibold))
                            .foregroundColor(.primary)
                            .frame(width: 36, height: 36)
                            .background(Color(.systemGray5))
                            .clipShape(Circle())
                    }
                }
            }
        }
    }
    
    // MARK: - Profile Card
    
    private var profileCard: some View {
        HStack(spacing: 16) {
            // Avatar
            ZStack {
                Circle()
                    .fill(Color(hex: "F97316")) // Orange color like screenshot
                    .frame(width: 56, height: 56)
                
                Image(systemName: "person.fill")
                    .font(.system(size: 24))
                    .foregroundColor(.white)
            }
            
            VStack(alignment: .leading, spacing: 4) {
                Text(userName ?? String(localized: "profile.user"))
                    .font(.system(size: 18, weight: .semibold))
                    .foregroundColor(.primary)
                
                Text(userEmail ?? "")
                    .font(.system(size: 14))
                    .foregroundColor(.secondary)
            }
            
            Spacer()
        }
        .padding(16)
        .background(Color(.secondarySystemGroupedBackground))
        .cornerRadius(16)
    }
    
    // MARK: - Privacy Section
    
    private var privacySection: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(String(localized: "settings_sheet.privacy"))
                .font(.system(size: 13, weight: .regular))
                .foregroundColor(.secondary)
                .padding(.horizontal, 16)
                .padding(.top, 8)
            
            settingsGroup {
                VStack(spacing: 0) {
                    LinkRow(title: String(localized: "settings_sheet.data_management"))
                    
                    Divider()
                        .padding(.leading, 16)
                    
                    LinkRow(title: String(localized: "settings_sheet.help"))
                    
                    Divider()
                        .padding(.leading, 16)
                    
                    LinkRow(title: String(localized: "settings_sheet.terms"))
                }
            }
        }
    }
    
    // MARK: - Settings Group Container
    
    private func settingsGroup<Content: View>(@ViewBuilder content: () -> Content) -> some View {
        VStack(spacing: 0) {
            content()
        }
        .background(Color(.secondarySystemGroupedBackground))
        .cornerRadius(12)
    }
}

// MARK: - Settings Row

struct SettingsRow: View {
    let icon: String
    let title: String
    let value: String
    
    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: icon)
                .font(.system(size: 20))
                .foregroundColor(.primary)
                .frame(width: 28)
            
            Text(title)
                .font(.system(size: 17))
                .foregroundColor(.primary)
            
            Spacer()
            
            HStack(spacing: 4) {
                Text(value)
                    .font(.system(size: 17))
                    .foregroundColor(.secondary)
                
                Image(systemName: "chevron.right")
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundColor(.gray.opacity(0.6))
            }
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
        .contentShape(Rectangle())
    }
}

// MARK: - Link Row

struct LinkRow: View {
    let title: String
    
    var body: some View {
        HStack {
            Text(title)
                .font(.system(size: 17))
                .foregroundColor(Color(hex: "007AFF"))
            
            Spacer()
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
        .contentShape(Rectangle())
    }
}

// MARK: - Preview

struct ProfileSettingsSheet_Previews: PreviewProvider {
    static var previews: some View {
        ProfileSettingsSheet(
            userId: "user-123",
            userName: "Guy MANDINA NZEZA",
            userEmail: "guyghost@gmail.com",
            onDismiss: {},
            onSignOut: {}
        )
    }
}
