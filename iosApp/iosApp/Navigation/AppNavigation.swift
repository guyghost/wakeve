import SwiftUI
import Shared

/// Main tab navigation for iOS app
/// Matches Android's bottom tab navigation (Home, Explore, Messages, Profile)
///
/// ## Tabs Structure
/// - Home: Event list and creation
/// - Explore: Discover events and features
/// - Messages: Notifications and chat
/// - Profile: User settings and preferences
struct MainTabView: View {
    let userId: String
    let repository: EventRepositoryInterface
    @State private var selectedTab: AppTab = .home

    var body: some View {
        TabView(selection: $selectedTab) {
            HomeTabContent()
                .tabItem {
                    Label(AppTab.home.title, systemImage: AppTab.home.iconName)
                }
                .tag(AppTab.home)

            ExploreTabContent()
                .tabItem {
                    Label(AppTab.explore.title, systemImage: AppTab.explore.iconName)
                }
                .tag(AppTab.explore)

            MessagesTabContent()
                .tabItem {
                    Label(AppTab.messages.title, systemImage: AppTab.messages.iconName)
                }
                .tag(AppTab.messages)

            ProfileTabContent()
                .tabItem {
                    Label(AppTab.profile.title, systemImage: AppTab.profile.iconName)
                }
                .tag(AppTab.profile)
        }
        .tint(.wakevPrimary)
    }

    // MARK: - Tab Contents

    @ViewBuilder
    private func HomeTabContent() -> some View {
        ModernHomeView(
            userId: userId,
            repository: repository,
            onEventSelected: { event in
                // Navigate to event detail
            },
            onCreateEvent: {
                // Show event creation sheet
            }
        )
    }

    @ViewBuilder
    private func ExploreTabContent() -> some View {
        ExploreView(
            userId: userId,
            repository: repository
        )
    }

    @ViewBuilder
    private func MessagesTabContent() -> some View {
        MessagesView(
            userId: userId
        )
    }

    @ViewBuilder
    private func ProfileTabContent() -> some View {
        ProfileScreen()
    }
}

// MARK: - App Tab Enum

/// Available tabs in the app
/// Matches Android's navigation tabs
enum AppTab: String, CaseIterable {
    case home = "home"
    case explore = "explore"
    case messages = "messages"
    case profile = "profile"

    var title: String {
        switch self {
        case .home: return "Accueil"
        case .explore: return "Explorer"
        case .messages: return "Messages"
        case .profile: return "Profil"
        }
    }

    var iconName: String {
        switch self {
        case .home: return "house.fill"
        case .explore: return "safari.fill"
        case .messages: return "message.fill"
        case .profile: return "person.fill"
        }
    }
}

// MARK: - Preview

#Preview("Main Tab View") {
    MainTabView(
        userId: "user123",
        repository: MockEventRepository()
    )
}
