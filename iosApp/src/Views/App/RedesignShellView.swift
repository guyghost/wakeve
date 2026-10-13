import SwiftUI

/// Shell de la refonte (couche 2, proposition #47) : deux zones gardées montées
/// (état préservé, comme un TabView), en-tête profil/réglages à la racine
/// d'Événements, barre flottante à la racine de chaque zone.
struct RedesignShellView<Events: View, Activity: View>: View {
    @Bindable var router: AppRouter
    let eventsAtRoot: Bool
    /// Activité à sa racine (aucun détail poussé, pas de mode sélection).
    let activityAtRoot: Bool
    let activityBadge: Int
    let userId: String
    let userName: String?
    let onCreate: () -> Void
    @ViewBuilder let events: () -> Events
    @ViewBuilder let activity: () -> Activity

    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    static func showsNavBar(zone: AppZone, eventsAtRoot: Bool, activityAtRoot: Bool) -> Bool {
        switch zone {
        case .events: return eventsAtRoot
        case .activity: return activityAtRoot
        }
    }

    static func showsHeader(zone: AppZone, eventsAtRoot: Bool) -> Bool {
        zone == .events && eventsAtRoot
    }

    var body: some View {
        ZStack {
            events()
                .opacity(router.zone == .events ? 1 : 0)
                .allowsHitTesting(router.zone == .events)
                .accessibilityHidden(router.zone != .events)
                .safeAreaInset(edge: .top, spacing: 0) {
                    if Self.showsHeader(zone: router.zone, eventsAtRoot: eventsAtRoot) {
                        header
                    }
                }
            activity()
                .opacity(router.zone == .activity ? 1 : 0)
                .allowsHitTesting(router.zone == .activity)
                .accessibilityHidden(router.zone != .activity)
        }
        .safeAreaInset(edge: .bottom, spacing: 0) {
            // L'animation est portée par ce conteneur : seule la barre anime
            // son apparition, le basculement de zone reste instantané.
            VStack(spacing: 0) {
                if isNavBarVisible {
                    WKFloatingNavBar(selection: $router.zone, activityBadge: activityBadge, onCreate: onCreate)
                        .padding(.bottom, WK.Space.xs)
                        .transition(.move(edge: .bottom).combined(with: .opacity))
                }
            }
            .animation(WK.Motion.smooth(reduceMotion: reduceMotion), value: isNavBarVisible)
        }
    }

    private var isNavBarVisible: Bool {
        Self.showsNavBar(zone: router.zone, eventsAtRoot: eventsAtRoot, activityAtRoot: activityAtRoot)
    }

    private var header: some View {
        HStack {
            Button {
                router.apply(.presentProfile)
            } label: {
                // Le libellé du bouton (« Profil ») remplace celui de la pile d'avatars.
                WKAvatarStack(avatars: [WKAvatar(id: userId, name: userName ?? "")])
                    .accessibilityHidden(true)
                    .frame(minWidth: WK.Size.minTapTarget, minHeight: WK.Size.minTapTarget)
                    .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            .accessibilityLabel(String(localized: "wk.nav.profile"))
            .wkAccessibilityID("wk.nav.profile")

            Spacer()

            Text(String(localized: "wk.wordmark"))
                .font(WK.Typo.headline)
                .foregroundStyle(WK.Colors.textTertiary)
                .accessibilityHidden(true)

            Spacer()

            WKCircleButton(
                systemImage: "gearshape",
                accessibilityLabel: String(localized: "wk.nav.settings"),
                accessibilityID: "wk.nav.settings"
            ) {
                router.apply(.presentSettings)
            }
        }
        .padding(.horizontal, WK.Space.screen)
        .padding(.vertical, WK.Space.xxs)
        // Le contenu défile sous l'en-tête (safeAreaInset) : un fond opaque évite la superposition.
        .background(WK.Colors.canvas.ignoresSafeArea(edges: .top))
    }
}
