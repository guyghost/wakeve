import SwiftUI

/// Barre d'actions de sheet : une action principale + icônes secondaires groupées.
struct WKActionBar: View {
    struct Item: Identifiable {
        let systemImage: String
        let label: String
        /// Identifiant d'accessibilité stable, indépendant de la langue (spec §7).
        var accessibilityID: String? = nil
        let action: () -> Void

        /// Identité stable entre rendus et indépendante du libellé localisé.
        /// Deux items partageant la même icône DOIVENT recevoir des `accessibilityID` distincts.
        var id: String { accessibilityID ?? systemImage }
    }

    let primaryTitle: String
    let primaryAction: () -> Void
    var secondary: [Item] = []
    var primaryAccessibilityID: String? = nil

    var body: some View {
        HStack(spacing: WK.Space.xs) {
            Button(action: primaryAction) {
                Text(primaryTitle)
                    .font(WK.Typo.headline)
                    .multilineTextAlignment(.center)
                    .foregroundStyle(WK.Colors.onAccent)
                    .padding(.horizontal, WK.Space.md)
                    .padding(.vertical, WK.Space.xs)
                    .frame(minHeight: WK.Size.minTapTarget)
                    .background(WK.Colors.accent, in: WK.pill)
                    .contentShape(WK.pill)
            }
            .buttonStyle(.plain)
            .wkAccessibilityID(primaryAccessibilityID)

            if !secondary.isEmpty {
                HStack(spacing: 0) {
                    ForEach(secondary) { item in
                        Button(action: item.action) {
                            Image(systemName: item.systemImage)
                                .font(WK.Typo.body)
                                .foregroundStyle(WK.Colors.textPrimary)
                                .frame(minWidth: WK.Size.minTapTarget, minHeight: WK.Size.minTapTarget)
                                .contentShape(Rectangle())
                        }
                        .buttonStyle(.plain)
                        .dynamicTypeSize(...DynamicTypeSize.accessibility1)
                        .accessibilityLabel(item.label)
                        .accessibilityShowsLargeContentViewer {
                            Label(item.label, systemImage: item.systemImage)
                        }
                        .wkAccessibilityID(item.accessibilityID)
                    }
                }
                .padding(.horizontal, WK.Space.xxs)
                .background(WK.Colors.cardInset, in: Capsule(style: .continuous))
            }
        }
    }
}
