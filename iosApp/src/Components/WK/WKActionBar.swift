import SwiftUI

/// Barre d'actions de sheet : une action principale + icônes secondaires groupées.
struct WKActionBar: View {
    struct Item: Identifiable {
        let id = UUID()
        let systemImage: String
        let label: String
        let action: () -> Void
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
                    .frame(minHeight: WK.Size.minTapTarget)
                    .background(WK.Colors.accent, in: Capsule())
                    .contentShape(Capsule())
            }
            .buttonStyle(.plain)

            if !secondary.isEmpty {
                HStack(spacing: 0) {
                    ForEach(secondary) { item in
                        Button(action: item.action) {
                            Image(systemName: item.systemImage)
                                .font(WK.Typo.body)
                                .foregroundStyle(WK.Colors.textPrimary)
                                .frame(width: WK.Size.minTapTarget, height: WK.Size.minTapTarget)
                                .contentShape(Rectangle())
                        }
                        .buttonStyle(.plain)
                        .accessibilityLabel(item.label)
                    }
                }
                .padding(.horizontal, WK.Space.xxs)
                .background(WK.Colors.cardInset, in: Capsule())
            }
        }
    }
}
