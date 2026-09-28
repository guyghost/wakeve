import SwiftUI

/// Barre flottante 3 zones : Événements · ＋ · Activité.
struct WKFloatingNavBar: View {
    @Binding var selection: AppZone
    var activityBadge: Int = 0
    let onCreate: () -> Void

    @Environment(\.accessibilityReduceTransparency) private var reduceTransparency
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    /// Lu sur la vue englobante, donc avant le plafond `...accessibility1` appliqué dans `body`.
    @Environment(\.dynamicTypeSize) private var dynamicTypeSize

    static let createAccessibilityID = "wk.nav.create"

    static func accessibilityID(for zone: AppZone) -> String {
        "wk.nav.\(zone.rawValue)"
    }

    /// Aux tailles d'accessibilité, la zone sélectionnée n'affiche que son icône
    /// (le Large Content Viewer et VoiceOver donnent le titre) pour tenir sur 375 pt.
    static func showsSelectedTitle(at size: DynamicTypeSize) -> Bool {
        !size.isAccessibilitySize
    }

    static func badgeText(for count: Int) -> String? {
        guard count > 0 else { return nil }
        return count > 99 ? "99+" : "\(count)"
    }

    /// Valeur d'accessibilité pluralisée du badge Activité pour la locale demandée.
    static func badgeAccessibilityValue(for count: Int, locale: Locale = WK.appLocale) -> String {
        guard count > 0 else { return "" }
        return String(format: WK.localizedFormat("wk.nav.activity.badge_format", locale: locale), locale: locale, count)
    }

    var body: some View {
        HStack(spacing: WK.Space.xs) {
            zoneButton(.events)
            Button(action: onCreate) {
                Image(systemName: "plus")
                    .font(WK.Typo.title)
                    .foregroundStyle(.primary)
                    .frame(minWidth: WK.Size.minTapTarget + WK.Space.md, minHeight: WK.Size.minTapTarget)
                    .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            .accessibilityLabel(String(localized: "wk.nav.create"))
            .accessibilityShowsLargeContentViewer {
                Label(String(localized: "wk.nav.create"), systemImage: "plus")
            }
            .accessibilityIdentifier(Self.createAccessibilityID)
            zoneButton(.activity)
        }
        .padding(WK.Space.xxs + WK.Space.xxxs)  // anneau intérieur de la capsule
        .modifier(BarChrome(reduceTransparency: reduceTransparency))
        .padding(.horizontal, WK.Space.lg)
        .dynamicTypeSize(...DynamicTypeSize.accessibility1)
    }

    private func zoneButton(_ zone: AppZone) -> some View {
        let isSelected = selection == zone
        let badge = zone == .activity ? Self.badgeText(for: activityBadge) : nil
        return Button {
            withAnimation(WK.Motion.snappy(reduceMotion: reduceMotion)) { selection = zone }
        } label: {
            HStack(spacing: WK.Space.xxs) {
                Image(systemName: zone.systemImage)
                if isSelected && Self.showsSelectedTitle(at: dynamicTypeSize) { Text(zone.title) }
            }
            .font(WK.Typo.caption.weight(.semibold))
            .foregroundStyle(isSelected ? AnyShapeStyle(WK.Colors.onAccentFill) : AnyShapeStyle(.secondary))
            .padding(.horizontal, WK.Space.sm)
            .frame(minWidth: WK.Size.minTapTarget, minHeight: WK.Size.minTapTarget)
            .background {
                if isSelected { Capsule().fill(WK.Colors.accentFill) }
            }
            .overlay(alignment: .topTrailing) {
                if let badge {
                    Text(badge)
                        .font(WK.Typo.micro.weight(.bold))
                        // Pastille décorative (valeur lue par VoiceOver) : jamais tronquée ni masquant l'icône.
                        .fixedSize()
                        .dynamicTypeSize(...DynamicTypeSize.xxxLarge)
                        .foregroundStyle(WK.Status.actionNeeded.fill)
                        .padding(.horizontal, WK.Space.xxs)
                        .frame(minHeight: WK.Space.md)
                        .background(WK.Status.actionNeeded.onFill, in: Capsule())
                        .offset(x: WK.Space.xxs, y: WK.Space.xxxs)
                        .accessibilityHidden(true)
                }
            }
            .contentShape(Capsule())
        }
        .buttonStyle(.plain)
        .accessibilityLabel(zone.title)
        .accessibilityValue(badge.map { _ in Self.badgeAccessibilityValue(for: activityBadge) } ?? "")
        .accessibilityAddTraits(isSelected ? [.isSelected, .isButton] : .isButton)
        .accessibilityShowsLargeContentViewer {
            Label(zone.title, systemImage: zone.systemImage)
        }
        .accessibilityIdentifier(Self.accessibilityID(for: zone))
    }

    private struct BarChrome: ViewModifier {
        let reduceTransparency: Bool

        func body(content: Content) -> some View {
            if #available(iOS 26.0, *), !reduceTransparency {
                content.glassEffect(.regular, in: Capsule())
            } else if reduceTransparency {
                // Contour sous le contenu : la pastille du badge reste au-dessus.
                content.background {
                    Capsule()
                        .fill(WK.Colors.card)
                        .overlay(Capsule().stroke(Color(uiColor: .opaqueSeparator), lineWidth: WK.Stroke.hairline))
                }
            } else {
                content.background(.regularMaterial, in: Capsule())
            }
        }
    }
}
