import SwiftUI
#if canImport(UIKit)
import UIKit
#endif

// MARK: - Tab Items

/// Defines the available tabs in the Wakeve app
enum WakevTab: String, CaseIterable, Identifiable {
    case home
    case inbox
    case explore
    case profile
    
    var id: String { rawValue }
    
    var title: String {
        switch self {
        case .home: return "Accueil"
        case .inbox: return "Inbox"
        case .explore: return "Explorer"
        case .profile: return "Profil"
        }
    }
    
    var icon: String {
        switch self {
        case .home: return "house"
        case .inbox: return "tray.fill"
        case .explore: return "sparkles"
        case .profile: return "person.crop.circle"
        }
    }
    
    var accessibilityLabel: String {
        switch self {
        case .home: return "Page d'accueil"
        case .inbox: return "Boîte de réception"
        case .explore: return "Explorer les événements"
        case .profile: return "Profil utilisateur"
        }
    }
    
    var accessibilityHint: String {
        switch self {
        case .home: return "Accéder à la page d'accueil"
        case .inbox: return "Voir vos messages et notifications"
        case .explore: return "Découvrir de nouveaux événements"
        case .profile: return "Gérer votre profil"
        }
    }
}

// MARK: - Tab Badge Configuration

/// Badge configuration for tabs
struct TabBadge: Equatable {
    let count: Int
    let style: BadgeStyle
    
    static let zero = TabBadge(count: 0, style: .neutral)
    
    var isVisible: Bool { count > 0 }
    
    enum BadgeStyle {
        case primary
        case accent
        case success
        case warning
        case error
        case neutral
    }
}

extension WakevTab {
    /// Default badge style for each tab
    var defaultBadgeStyle: TabBadge.BadgeStyle {
        switch self {
        case .home: return .primary
        case .inbox: return .error
        case .explore: return .accent
        case .profile: return .neutral
        }
    }
}

// MARK: - Custom Tab Button with Liquid Glass

/// Individual tab button with Liquid Glass styling
struct LiquidGlassTabButton: View {
    let tab: WakevTab
    let isSelected: Bool
    let badge: TabBadge
    let action: () -> Void
    
    @State private var isPressed = false
    
    var body: some View {
        Button(action: {
            withAnimation(.spring(response: 0.3, dampingFraction: 0.7)) {
                action()
            }
        }) {
            VStack(spacing: 4) {
                ZStack(alignment: .topTrailing) {
                    // Icon with glass effect
                    tabIconView
                        .font(.system(size: 22, weight: isSelected ? .semibold : .regular))
                        .foregroundColor(iconColor)
                        .padding(12)
                        .background(iconBackground)
                        .overlay(
                            RoundedRectangle(cornerRadius: 14, style: .continuous)
                                .stroke(iconBorder, lineWidth: 1)
                        )
                        .shadow(color: iconShadow, radius: 8, x: 0, y: 4)
                    
                    // Badge overlay
                    if badge.isVisible {
                        badgeView
                            .offset(x: 8, y: -4)
                            .transition(.scale.combined(with: .opacity))
                    }
                }
                
                // Tab title
                Text(tab.title)
                    .font(.system(size: 11, weight: isSelected ? .semibold : .medium))
                    .foregroundColor(titleColor)
                    .lineLimit(1)
            }
        }
        .buttonStyle(PlainButtonStyle())
        .accessibilityLabel(tab.accessibilityLabel)
        .accessibilityHint(tab.accessibilityHint)
        .accessibilityAddTraits(isSelected ? [.isSelected, .isButton] : [.isButton])
        .scaleEffect(isPressed ? 0.95 : 1.0)
        .simultaneousGesture(
            DragGesture(minimumDistance: 0)
                .onChanged { _ in isPressed = true }
                .onEnded { _ in isPressed = false }
        )
        .animation(.spring(response: 0.2, dampingFraction: 0.6), value: isPressed)
    }
    
    // MARK: - Computed Properties
    
    private var iconColor: Color {
        isSelected ? Color.wakevPrimary : Color.wakevTextSecondary
    }
    
    private var titleColor: Color {
        isSelected ? Color.wakevPrimary : Color.wakevTextSecondary
    }
    
    private var iconBackground: some View {
        RoundedRectangle(cornerRadius: 14, style: .continuous)
            .fill(isSelected ? Color.white.opacity(0.75) : Color.clear)
    }
    
    private var iconBorder: LinearGradient {
        LinearGradient(
            colors: [
                Color.white.opacity(isSelected ? 0.6 : 0.0),
                Color.white.opacity(isSelected ? 0.2 : 0.0)
            ],
            startPoint: .topLeading,
            endPoint: .bottomTrailing
        )
    }
    
    private var iconShadow: Color {
        isSelected ? Color.wakevPrimary.opacity(0.3) : .clear
    }
    
    private var tabIconView: some View {
        Image(systemName: tab.icon)
    }
    
    private var badgeView: some View {
        Text("\(badge.count)")
            .font(.system(size: 12, weight: .bold))
            .foregroundColor(badgeForegroundColor)
            .padding(.horizontal, 10)
            .padding(.vertical, 4)
            .background(badgeBackground)
            .clipShape(Capsule())
            .overlay(
                Capsule()
                    .stroke(badgeBorder, lineWidth: 1)
            )
            .shadow(color: badgeShadowColor, radius: 4, x: 0, y: 2)
    }
    
    private var badgeForegroundColor: Color {
        switch badge.style {
        case .primary: return Color.wakevPrimary
        case .accent: return Color.wakevAccent
        case .success: return Color.wakevSuccess
        case .warning: return Color.wakevWarning
        case .error: return Color.wakevError
        case .neutral: return Color.wakevTextPrimary
        }
    }
    
    private var badgeBackground: LinearGradient {
        switch badge.style {
        case .primary:
            return LinearGradient(
                colors: [Color.white.opacity(0.95), Color.white.opacity(0.85)],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
        case .accent:
            return LinearGradient(
                colors: [Color.wakevAccent.opacity(0.2), Color.wakevAccent.opacity(0.1)],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
        case .success:
            return LinearGradient(
                colors: [Color.wakevSuccess.opacity(0.2), Color.wakevSuccess.opacity(0.1)],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
        case .warning:
            return LinearGradient(
                colors: [Color.wakevWarning.opacity(0.2), Color.wakevWarning.opacity(0.1)],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
        case .error:
            return LinearGradient(
                colors: [Color.wakevError.opacity(0.2), Color.wakevError.opacity(0.1)],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
        case .neutral:
            return LinearGradient(
                colors: [Color.white.opacity(0.2), Color.white.opacity(0.1)],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
        }
    }
    
    private var badgeBorder: LinearGradient {
        LinearGradient(
            colors: [Color.white.opacity(0.5), Color.white.opacity(0.2)],
            startPoint: .topLeading,
            endPoint: .bottomTrailing
        )
    }
    
    private var badgeShadowColor: Color {
        switch badge.style {
        case .primary: return Color.wakevPrimary.opacity(0.25)
        case .accent: return Color.wakevAccent.opacity(0.25)
        case .success: return Color.wakevSuccess.opacity(0.25)
        case .warning: return Color.wakevWarning.opacity(0.25)
        case .error: return Color.wakevError.opacity(0.25)
        case .neutral: return .clear
        }
    }
}

// MARK: - Liquid Glass Tab Bar Container (Imperative Shell)

/// Custom tab bar container with Liquid Glass styling
struct WakevTabBarContainer<Home: View, Inbox: View, Explore: View, Profile: View>: View {
    @Binding var selectedTab: WakevTab
    
    /// Badge configurations for each tab
    var homeBadge: TabBadge
    var inboxBadge: TabBadge
    var exploreBadge: TabBadge
    var profileBadge: TabBadge
    
    let homeContent: Home
    let inboxContent: Inbox
    let exploreContent: Explore
    let profileContent: Profile
    
    init(
        selectedTab: Binding<WakevTab>,
        homeBadge: TabBadge = .zero,
        inboxBadge: TabBadge = .zero,
        exploreBadge: TabBadge = .zero,
        profileBadge: TabBadge = .zero,
        @ViewBuilder home: () -> Home,
        @ViewBuilder inbox: () -> Inbox,
        @ViewBuilder explore: () -> Explore,
        @ViewBuilder profile: () -> Profile
    ) {
        self._selectedTab = selectedTab
        self.homeBadge = homeBadge
        self.inboxBadge = inboxBadge
        self.exploreBadge = exploreBadge
        self.profileBadge = profileBadge
        self.homeContent = home()
        self.inboxContent = inbox()
        self.exploreContent = explore()
        self.profileContent = profile()
    }
    
    var body: some View {
        TabView(selection: $selectedTab) {
            homeContent
                .tabItem {
                    Label(WakevTab.home.title, systemImage: WakevTab.home.icon)
                }
                .tag(WakevTab.home)
                .badge(homeBadge.isVisible ? String(homeBadge.count) : nil)
            
            inboxContent
                .tabItem {
                    Label(WakevTab.inbox.title, systemImage: WakevTab.inbox.icon)
                }
                .tag(WakevTab.inbox)
                .badge {
                    if inboxBadge.isVisible {
                        badgeView(for: inboxBadge)
                    }
                }
            
            exploreContent
                .tabItem {
                    Label(WakevTab.explore.title, systemImage: WakevTab.explore.icon)
                }
                .tag(WakevTab.explore)
                .badge {
                    if exploreBadge.isVisible {
                        badgeView(for: exploreBadge)
                    }
                }
            
            profileContent
                .tabItem {
                    Label(WakevTab.profile.title, systemImage: WakevTab.profile.icon)
                }
                .tag(WakevTab.profile)
                .badge {
                    if profileBadge.isVisible {
                        badgeView(for: profileBadge)
                    }
                }
        }
        .tint(Color.wakevPrimary)
        .onAppear {
            configureTabBarAppearance()
        }
    }
    
    // MARK: - Badge View Builder
    
    @ViewBuilder
    private func badgeView(for badge: TabBadge) -> some View {
        Text("\(badge.count)")
            .font(.system(size: 12, weight: .bold))
            .foregroundColor(badgeForegroundColor(for: badge.style))
            .padding(.horizontal, 10)
            .padding(.vertical, 4)
            .background(badgeBackground(for: badge.style))
            .clipShape(Capsule())
            .overlay(
                Capsule()
                    .stroke(badgeBorder, lineWidth: 1)
            )
            .shadow(color: badgeShadowColor(for: badge.style), radius: 4, x: 0, y: 2)
    }
    
    private func badgeForegroundColor(for style: TabBadge.BadgeStyle) -> Color {
        switch style {
        case .primary: return Color.wakevPrimary
        case .accent: return Color.wakevAccent
        case .success: return Color.wakevSuccess
        case .warning: return Color.wakevWarning
        case .error: return Color.wakevError
        case .neutral: return Color.wakevTextPrimary
        }
    }
    
    private func badgeBackground(for style: TabBadge.BadgeStyle) -> LinearGradient {
        switch style {
        case .primary:
            return LinearGradient(
                colors: [Color.white.opacity(0.95), Color.white.opacity(0.85)],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
        case .accent:
            return LinearGradient(
                colors: [Color.wakevAccent.opacity(0.2), Color.wakevAccent.opacity(0.1)],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
        case .success:
            return LinearGradient(
                colors: [Color.wakevSuccess.opacity(0.2), Color.wakevSuccess.opacity(0.1)],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
        case .warning:
            return LinearGradient(
                colors: [Color.wakevWarning.opacity(0.2), Color.wakevWarning.opacity(0.1)],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
        case .error:
            return LinearGradient(
                colors: [Color.wakevError.opacity(0.2), Color.wakevError.opacity(0.1)],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
        case .neutral:
            return LinearGradient(
                colors: [Color.white.opacity(0.2), Color.white.opacity(0.1)],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
        }
    }
    
    private var badgeBorder: LinearGradient {
        LinearGradient(
            colors: [Color.white.opacity(0.5), Color.white.opacity(0.2)],
            startPoint: .topLeading,
            endPoint: .bottomTrailing
        )
    }
    
    private func badgeShadowColor(for style: TabBadge.BadgeStyle) -> Color {
        switch style {
        case .primary: return Color.wakevPrimary.opacity(0.25)
        case .accent: return Color.wakevAccent.opacity(0.25)
        case .success: return Color.wakevSuccess.opacity(0.25)
        case .warning: return Color.wakevWarning.opacity(0.25)
        case .error: return Color.wakevError.opacity(0.25)
        case .neutral: return .clear
        }
    }
    
    private func configureTabBarAppearance() {
        let appearance = UITabBarAppearance()
        appearance.configureWithDefaultBackground()
        appearance.backgroundColor = UIColor.systemBackground.withAlphaComponent(0.8)
        appearance.selectionIndicatorTintColor = UIColor(Color.wakevPrimary)
        UITabBar.appearance().standardAppearance = appearance
        UITabBar.appearance().scrollEdgeAppearance = appearance
    }
}

// MARK: - Simplified Container

/// Simplified container that switches content based on selected tab
struct WakevTabBarSimpleContainer<Content: View>: View {
    @Binding var selectedTab: WakevTab
    
    var homeBadge: TabBadge
    var inboxBadge: TabBadge
    var exploreBadge: TabBadge
    var profileBadge: TabBadge
    
    let content: (WakevTab) -> Content
    
    init(
        selectedTab: Binding<WakevTab>,
        homeBadge: TabBadge = .zero,
        inboxBadge: TabBadge = .zero,
        exploreBadge: TabBadge = .zero,
        profileBadge: TabBadge = .zero,
        @ViewBuilder content: @escaping (WakevTab) -> Content
    ) {
        self._selectedTab = selectedTab
        self.homeBadge = homeBadge
        self.inboxBadge = inboxBadge
        self.exploreBadge = exploreBadge
        self.profileBadge = profileBadge
        self.content = content
    }
    
    var body: some View {
        TabView(selection: $selectedTab) {
            ForEach(WakevTab.allCases) { tab in
                content(tab)
                    .tabItem {
                        Label(tab.title, systemImage: tab.icon)
                    }
                    .tag(tab)
                    .badge({
                        let badge = badgeForTab(tab)
                        return badge.isVisible ? String(badge.count) : nil
                    }())
            }
        }
        .tint(Color.wakevPrimary)
    }
    
    private func badgeForTab(_ tab: WakevTab) -> TabBadge {
        switch tab {
        case .home: return homeBadge
        case .inbox: return inboxBadge
        case .explore: return exploreBadge
        case .profile: return profileBadge
        }
    }
    
    @ViewBuilder
    private func badgeView(for badge: TabBadge) -> some View {
        Text("\(badge.count)")
            .font(.system(size: 12, weight: .bold))
            .foregroundColor(badgeForegroundColor(for: badge.style))
            .padding(.horizontal, 10)
            .padding(.vertical, 4)
            .background(badgeBackground(for: badge.style))
            .clipShape(Capsule())
            .overlay(
                Capsule()
                    .stroke(badgeBorder, lineWidth: 1)
            )
            .shadow(color: badgeShadowColor(for: badge.style), radius: 4, x: 0, y: 2)
    }
    
    private func badgeForegroundColor(for style: TabBadge.BadgeStyle) -> Color {
        switch style {
        case .primary: return Color.wakevPrimary
        case .accent: return Color.wakevAccent
        case .success: return Color.wakevSuccess
        case .warning: return Color.wakevWarning
        case .error: return Color.wakevError
        case .neutral: return Color.wakevTextPrimary
        }
    }
    
    private func badgeBackground(for style: TabBadge.BadgeStyle) -> LinearGradient {
        switch style {
        case .primary:
            return LinearGradient(
                colors: [Color.white.opacity(0.95), Color.white.opacity(0.85)],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
        case .accent:
            return LinearGradient(
                colors: [Color.wakevAccent.opacity(0.2), Color.wakevAccent.opacity(0.1)],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
        case .success:
            return LinearGradient(
                colors: [Color.wakevSuccess.opacity(0.2), Color.wakevSuccess.opacity(0.1)],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
        case .warning:
            return LinearGradient(
                colors: [Color.wakevWarning.opacity(0.2), Color.wakevWarning.opacity(0.1)],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
        case .error:
            return LinearGradient(
                colors: [Color.wakevError.opacity(0.2), Color.wakevError.opacity(0.1)],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
        case .neutral:
            return LinearGradient(
                colors: [Color.white.opacity(0.2), Color.white.opacity(0.1)],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
        }
    }
    
    private var badgeBorder: LinearGradient {
        LinearGradient(
            colors: [Color.white.opacity(0.5), Color.white.opacity(0.2)],
            startPoint: .topLeading,
            endPoint: .bottomTrailing
        )
    }
    
    private func badgeShadowColor(for style: TabBadge.BadgeStyle) -> Color {
        switch style {
        case .primary: return Color.wakevPrimary.opacity(0.25)
        case .accent: return Color.wakevAccent.opacity(0.25)
        case .success: return Color.wakevSuccess.opacity(0.25)
        case .warning: return Color.wakevWarning.opacity(0.25)
        case .error: return Color.wakevError.opacity(0.25)
        case .neutral: return .clear
        }
    }
}

// MARK: - Glass Effect Extensions (iOS 26+)

@available(iOS 26.0, *)
extension View {
    func wakevGlass() -> some View {
        self.glassEffect()
    }
    
    func wakevInteractiveGlass() -> some View {
        self.glassEffect(.regular.interactive())
    }
}

// MARK: - Native TabView with Liquid Glass (iOS 26+)

struct WakevNativeTabView<Content: View>: View {
    @Binding var selectedTab: WakevTab
    let content: Content
    
    init(
        selectedTab: Binding<WakevTab>,
        @ViewBuilder content: () -> Content
    ) {
        self._selectedTab = selectedTab
        self.content = content()
    }
    
    var body: some View {
        TabView(selection: $selectedTab) {
            content
        }
    }
}

// MARK: - Tab Content Wrapper

struct WakevTabContent<Content: View>: View {
    let tab: WakevTab
    let content: Content
    
    init(
        tab: WakevTab,
        @ViewBuilder content: () -> Content
    ) {
        self.tab = tab
        self.content = content()
    }
    
    var body: some View {
        content
            .tabItem {
                Label(tab.title, systemImage: tab.icon)
            }
            .tag(tab)
    }
}

// MARK: - Preview

#Preview("Native TabView - Light") {
    WakevTabBarPreview()
        .preferredColorScheme(.light)
}

#Preview("Native TabView - Dark") {
    WakevTabBarPreview()
        .preferredColorScheme(.dark)
}

#Preview("Glass Components") {
    GlassComponentsPreview()
}

#Preview("Liquid Glass Tab Buttons") {
    LiquidGlassTabButtonsPreview()
}

// MARK: - Preview Views

struct WakevTabBarPreview: View {
    @State private var selectedTab: WakevTab = .home
    @State private var inboxBadge = TabBadge(count: 5, style: .error)
    @State private var exploreBadge = TabBadge(count: 12, style: .accent)
    
    var body: some View {
        WakevTabBarContainer(
            selectedTab: $selectedTab,
            homeBadge: .zero,
            inboxBadge: inboxBadge,
            exploreBadge: exploreBadge,
            profileBadge: .zero,
            home: {
                PreviewContent(
                    title: "Home",
                    icon: "house.fill",
                    color: Color.wakevPrimary
                )
            },
            inbox: {
                PreviewContent(
                    title: "Inbox",
                    icon: "tray.fill",
                    color: .purple
                )
            },
            explore: {
                PreviewContent(
                    title: "Explore",
                    icon: "sparkles",
                    color: .orange
                )
            },
            profile: {
                PreviewContent(
                    title: "Profile",
                    icon: "person.crop.circle.fill",
                    color: .green
                )
            }
        )
    }
}

struct LiquidGlassTabButtonsPreview: View {
    @State private var selectedTab: WakevTab = .home
    @State private var inboxBadge = TabBadge(count: 3, style: .error)
    @State private var exploreBadge = TabBadge(count: 7, style: .accent)
    
    var body: some View {
        VStack {
            Spacer()
            
            VStack(spacing: 0) {
                Rectangle()
                    .fill(Color.clear)
                    .frame(height: 0.5)
                    .background(
                        LinearGradient(
                            gradient: Gradient(colors: [
                                Color.wakevBorder.opacity(0.15),
                                Color.wakevBorder.opacity(0)
                            ]),
                            startPoint: .leading,
                            endPoint: .trailing
                        )
                    )
                
                HStack(spacing: 0) {
                    LiquidGlassTabButton(
                        tab: .home,
                        isSelected: selectedTab == .home,
                        badge: .zero,
                        action: { selectedTab = .home }
                    )
                    
                    verticalDivider
                    
                    LiquidGlassTabButton(
                        tab: .inbox,
                        isSelected: selectedTab == .inbox,
                        badge: inboxBadge,
                        action: { selectedTab = .inbox }
                    )
                    
                    verticalDivider
                    
                    LiquidGlassTabButton(
                        tab: .explore,
                        isSelected: selectedTab == .explore,
                        badge: exploreBadge,
                        action: { selectedTab = .explore }
                    )
                    
                    verticalDivider
                    
                    LiquidGlassTabButton(
                        tab: .profile,
                        isSelected: selectedTab == .profile,
                        badge: .zero,
                        action: { selectedTab = .profile }
                    )
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 8)
                .background(
                    Rectangle()
                        .fill(.ultraThinMaterial)
                        .shadow(color: .black.opacity(0.1), radius: 10, x: 0, y: -5)
                )
            }
            .background(
                Rectangle()
                    .fill(Color(.systemBackground).opacity(0.9))
                    .blur(radius: 10)
            )
            
            Spacer()
            
            VStack(spacing: 12) {
                Text("Tabs avec Liquid Glass")
                    .font(.headline)
                
                HStack(spacing: 16) {
                    Button("Inbox: +1") {
                        inboxBadge = TabBadge(count: inboxBadge.count + 1, style: .error)
                    }
                    .buttonStyle(.bordered)
                    
                    Button("Explore: +1") {
                        exploreBadge = TabBadge(count: exploreBadge.count + 1, style: .accent)
                    }
                    .buttonStyle(.bordered)
                }
            }
            .padding()
        }
        .background(Color.wakevBackgroundDark.ignoresSafeArea())
    }
    
    private var verticalDivider: some View {
        Rectangle()
            .fill(Color.clear)
            .frame(width: 0.5)
            .frame(height: 24)
            .background(
                LinearGradient(
                    gradient: Gradient(colors: [
                        Color.wakevBorder.opacity(0.15),
                        Color.wakevBorder.opacity(0)
                    ]),
                    startPoint: .top,
                    endPoint: .bottom
                )
            )
    }
}

struct PreviewContent: View {
    let title: String
    let icon: String
    let color: Color
    
    var body: some View {
        ZStack {
            LinearGradient(
                gradient: Gradient(colors: [
                    color.opacity(0.1),
                    color.opacity(0.05)
                ]),
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
            .ignoresSafeArea()
            
            VStack(spacing: 20) {
                Image(systemName: icon)
                    .font(.system(size: 64))
                    .foregroundColor(color)
                
                Text(title)
                    .font(.largeTitle)
                    .fontWeight(.bold)
            }
        }
    }
}

struct GlassComponentsPreview: View {
    var body: some View {
        ZStack {
            LinearGradient(
                gradient: Gradient(colors: [Color.wakevPrimary.opacity(0.3), Color.wakevAccent.opacity(0.3)]),
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
            .ignoresSafeArea()
            
            VStack(spacing: 24) {
                VStack(alignment: .leading, spacing: 12) {
                    Text("Liquid Glass Card")
                        .font(.headline)
                    Text("This card uses native Liquid Glass on iOS 26+")
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding()
                .liquidGlass(cornerRadius: 16)
                .padding(.horizontal)
                
                LiquidGlassButton(title: "Tap Me", icon: "hand.tap") {
                    print("Tapped!")
                }
                
                HStack(spacing: 16) {
                    LiquidGlassBadge(count: 5, style: .primary)
                    LiquidGlassBadge(count: 12, style: .accent)
                    LiquidGlassBadge(count: 3, style: .success)
                }
            }
        }
    }
}
