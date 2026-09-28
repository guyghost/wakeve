import SwiftUI

/// Barre d'actions de sheet : une action principale + icônes secondaires groupées.
struct WKActionBar: View {
    struct Item: Identifiable {
        let systemImage: String
        let label: String
        let action: () -> Void

        /// Identité stable entre rendus ; sert aussi d'identifiant d'accessibilité.
        var id: String { systemImage + "|" + label }
    }

    let primaryTitle: String
    let primaryAction: () -> Void
    var secondary: [Item] = []

    var body: some View {
        HStack(spacing: WK.Space.xs) {
            Button(action: primaryAction) {
                Text(primaryTitle)
                    .font(WK.Typo.headline)
                    .foregroundStyle(WK.Colors.onAccent)
                    .padding(.horizontal, WK.Space.md)
                    .padding(.vertical, WK.Space.xs)
                    .frame(minHeight: WK.Size.minTapTarget)
                    .background(WK.Colors.accent, in: WK.pill)
                    .contentShape(WK.pill)
            }
            .buttonStyle(.plain)

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
                        .accessibilityIdentifier(item.id)
                    }
                }
                .padding(.horizontal, WK.Space.xxs)
                .background(WK.Colors.cardInset, in: Capsule())
            }
        }
    }
}
