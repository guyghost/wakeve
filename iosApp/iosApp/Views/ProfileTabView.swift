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

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 24) {
                    // Profile Header
                    ProfileHeaderSection()

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
            .navigationTitle("Mon Profil")
            .preferredColorScheme(darkMode ? .dark : .light)
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
            Text("Mes Préférences")
                .font(.headline)
                .foregroundColor(.primary)

            ProfileCard {
                VStack(spacing: 0) {
                    // Notifications Toggle
                    PreferenceToggleRow(
                        icon: "bell.fill",
                        title: "Notifications push",
                        description: "Recevoir des notifications sur l'appareil",
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
            Text("Apparence")
                .font(.headline)
                .foregroundColor(.primary)

            ProfileCard {
                VStack(spacing: 0) {
                    // Dark Mode Toggle
                    PreferenceToggleRow(
                        icon: darkMode ? "moon.fill" : "sun.max.fill",
                        title: darkMode ? "Mode sombre" : "Mode clair",
                        description: darkMode ? "Utiliser le thème sombre" : "Utiliser le thème clair",
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
            Text("À propos")
                .font(.headline)
                .foregroundColor(.primary)

            ProfileCard {
                VStack(spacing: 0) {
                    // Settings Link
                    AboutLinkRow(
                        icon: "gearshape.fill",
                        title: "Paramètres",
                        action: {
                            showSettings = true
                        }
                    )
                    
                    Divider()
                    
                    // Version
                    AboutRow(
                        icon: "info.circle.fill",
                        title: "Version",
                        value: "1.0.0"
                    )

                    Divider()

                    // Documentation Link
                    AboutLinkRow(
                        icon: "book.fill",
                        title: "Documentation",
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
                Text("Se déconnecter")
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
