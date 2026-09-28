import SwiftUI

/// Barre flottante 3 zones : Événements · ＋ · Activité.
struct WKFloatingNavBar: View {
    @Binding var selection: AppZone
    var activityBadge: Int = 0
    let onCreate: () -> Void

    @Environment(\.accessibilityReduceTransparency) private var reduceTransparency
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    static func badgeText(for count: Int) -> String? {
        guard count > 0 else { return nil }
        return count > 99 ? "99+" : "\(count)"
    }

    /// Valeur d'accessibilité pluralisée du badge Activité pour la locale demandée,
    /// indépendante de la langue de l'app (même approche que WKAvatarStack.othersText).
    static func badgeAccessibilityValue(for count: Int, locale: Locale = .current) -> String {
        guard count > 0 else { return "" }
        let bundle = locale.language.languageCode
            .flatMap { Bundle.main.path(forResource: $0.identifier, ofType: "lproj") }
            .flatMap(Bundle.init(path:)) ?? .main
        let format = bundle.localizedString(forKey: "wk.nav.activity.badge_format", value: nil, table: nil)
        return String(format: format, locale: locale, count)
    }

    var body: some View {
        HStack(spacing: WK.Space.xs) {
            zoneButton(.events)
            Button(action: onCreate) {
                Image(systemName: "plus")
                    .font(WK.Typo.title)
                    .foregroundStyle(WK.Colors.textPrimary)
                    .frame(width: WK.Size.minTapTarget + WK.Space.md, height: WK.Size.minTapTarget)
                    .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            .accessibilityLabel(String(localized: "wk.nav.create"))
            zoneButton(.activity)
        }
        .padding(WK.Space.xxs + 2)  // 6 pt : anneau intérieur de la capsule
        .modifier(BarChrome(reduceTransparency: reduceTransparency))
        .padding(.horizontal, WK.Space.lg)
    }

    private func zoneButton(_ zone: AppZone) -> some View {
        let isSelected = selection == zone
        let badge = zone == .activity ? Self.badgeText(for: activityBadge) : nil
        return Button {
            withAnimation(WK.Motion.snappy(reduceMotion: reduceMotion)) { selection = zone }
        } label: {
            HStack(spacing: WK.Space.xxs) {
                Image(systemName: zone.systemImage)
                if isSelected { Text(zone.title) }
            }
            .font(WK.Typo.caption.weight(.semibold))
            .foregroundStyle(isSelected ? WK.Colors.onAccentFill : WK.Colors.textSecondary)
            .padding(.horizontal, WK.Space.sm)
            .frame(minWidth: WK.Size.minTapTarget, minHeight: WK.Size.minTapTarget)
            .background {
                if isSelected { Capsule().fill(WK.Colors.accentFill) }
            }
            .overlay(alignment: .topTrailing) {
                if let badge {
                    Text(badge)
                        .font(WK.Typo.micro.weight(.bold))
                        .foregroundStyle(WK.Status.actionNeeded.fill)
                        .padding(.horizontal, WK.Space.xxs + 1)
                        .frame(minHeight: WK.Space.md)
                        .background(WK.Status.actionNeeded.onFill, in: Capsule())
                        .offset(x: 4, y: 2)
                        .accessibilityHidden(true)
                }
            }
            .contentShape(Capsule())
        }
        .buttonStyle(.plain)
        .accessibilityLabel(zone.title)
        .accessibilityValue(badge.map { _ in Self.badgeAccessibilityValue(for: activityBadge) } ?? "")
        .accessibilityAddTraits(isSelected ? [.isSelected, .isButton] : .isButton)
    }

    private struct BarChrome: ViewModifier {
        let reduceTransparency: Bool

        func body(content: Content) -> some View {
            if #available(iOS 26.0, *), !reduceTransparency {
                content.glassEffect(.regular, in: Capsule())
            } else if reduceTransparency {
                content
                    .background(WK.Colors.card, in: Capsule())
                    .overlay(Capsule().stroke(WK.Colors.cardInset, lineWidth: 1))
            } else {
                content.background(.regularMaterial, in: Capsule())
            }
        }
    }
}
