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
}

// MARK: - Native TabView with Liquid Glass (iOS 26+)

/// A native iOS TabView that automatically adopts Liquid Glass styling on iOS 26+
/// Falls back to standard TabView on earlier iOS versions
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

/// Wrapper to apply tabItem to content
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

// MARK: - Main TabBar Container using Native Components

/// Native iOS TabView container with Liquid Glass support
/// Uses native TabView which automatically adopts Liquid Glass on iOS 26+
struct WakevTabBarContainer<Home: View, Inbox: View, Explore: View, Profile: View>: View {
    @Binding var selectedTab: WakevTab
    
    let homeContent: Home
    let inboxContent: Inbox
    let exploreContent: Explore
    let profileContent: Profile
    
    init(
        selectedTab: Binding<WakevTab>,
        @ViewBuilder home: () -> Home,
        @ViewBuilder inbox: () -> Inbox,
        @ViewBuilder explore: () -> Explore,
        @ViewBuilder profile: () -> Profile
    ) {
        self._selectedTab = selectedTab
        self.homeContent = home()
        self.inboxContent = inbox()
        self.exploreContent = explore()
        self.profileContent = profile()
    }
    
    var body: some View {
        TabView(selection: $selectedTab) {
            homeContent
                .tabItem {
                    Label("Accueil", systemImage: "house")
                }
                .tag(WakevTab.home)
            
            inboxContent
                .tabItem {
                    Label("Inbox", systemImage: "tray.fill")
                }
                .tag(WakevTab.inbox)
            
            exploreContent
                .tabItem {
                    Label("Explorer", systemImage: "sparkles")
                }
                .tag(WakevTab.explore)
            
            profileContent
                .tabItem {
                    Label("Profil", systemImage: "person.crop.circle")
                }
                .tag(WakevTab.profile)
        }
        .tint(Color(red: 37/255, green: 99/255, blue: 235/255)) // wakevPrimary
    }
}

// MARK: - Simple Container (Single Content Switched)

/// Simplified container that switches content based on selected tab
struct WakevTabBarSimpleContainer<Content: View>: View {
    @Binding var selectedTab: WakevTab
    let content: (WakevTab) -> Content
    
    init(
        selectedTab: Binding<WakevTab>,
        @ViewBuilder content: @escaping (WakevTab) -> Content
    ) {
        self._selectedTab = selectedTab
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
            }
        }
        .tint(Color(red: 37/255, green: 99/255, blue: 235/255)) // wakevPrimary
    }
}

// MARK: - Glass Effect Extensions (iOS 26+)

@available(iOS 26.0, *)
extension View {
    /// Applies the native Liquid Glass effect
    func wakevGlass() -> some View {
        self.glassEffect()
    }
    
    /// Applies interactive Liquid Glass effect for buttons
    func wakevInteractiveGlass() -> some View {
        self.glassEffect(.regular.interactive())
    }
}

// MARK: - Liquid Glass Button (iOS 26+ Native)

/// A button using native Liquid Glass on iOS 26+
struct LiquidGlassButton: View {
    let title: String
    let icon: String?
    let action: () -> Void
    
    init(
        _ title: String,
        icon: String? = nil,
        action: @escaping () -> Void
    ) {
        self.title = title
        self.icon = icon
        self.action = action
    }
    
    var body: some View {
        Button(action: action) {
            HStack(spacing: 8) {
                if let icon = icon {
                    Image(systemName: icon)
                }
                Text(title)
            }
            .font(.headline)
            .foregroundColor(.primary)
            .padding(.horizontal, 20)
            .padding(.vertical, 12)
        }
        .liquidGlassButtonStyle()
    }
}

// MARK: - Button Style with Liquid Glass

struct LiquidGlassButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        if #available(iOS 26.0, *) {
            configuration.label
                .glassEffect(.regular.interactive())
                .scaleEffect(configuration.isPressed ? 0.97 : 1.0)
                .animation(.spring(response: 0.2, dampingFraction: 0.7), value: configuration.isPressed)
        } else {
            // Fallback for iOS < 26
            configuration.label
                .background(.regularMaterial)
                .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                .scaleEffect(configuration.isPressed ? 0.97 : 1.0)
                .animation(.spring(response: 0.2, dampingFraction: 0.7), value: configuration.isPressed)
        }
    }
}

extension View {
    func liquidGlassButtonStyle() -> some View {
        self.buttonStyle(LiquidGlassButtonStyle())
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

struct WakevTabBarPreview: View {
    @State private var selectedTab: WakevTab = .home
    
    var body: some View {
        WakevTabBarContainer(
            selectedTab: $selectedTab,
            home: {
                PreviewContent(
                    title: "Home",
                    icon: "house.fill",
                    color: .blue
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
                gradient: Gradient(colors: [.blue.opacity(0.3), .purple.opacity(0.3)]),
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
            .ignoresSafeArea()
            
            VStack(spacing: 24) {
                // Glass Card - Commented out due to import scope issue
                // LiquidGlassCard {
                //     VStack(alignment: .leading, spacing: 12) {
                //         Text("Liquid Glass Card")
                //             .font(.headline)
                //         Text("This card uses native Liquid Glass on iOS 26+")
                //             .font(.subheadline)
                //             .foregroundColor(.secondary)
                //     }
                //     .frame(maxWidth: .infinity, alignment: .leading)
                // }
                // .padding(.horizontal)
                
                // Glass Button
                LiquidGlassButton("Tap Me", icon: "hand.tap") {
                    print("Tapped!")
                }
            }
        }
    }
}
