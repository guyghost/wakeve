import SwiftUI

/// Grand chiffre en tête d'écran (votes reçus, heure, budget/personne).
struct WKHeroMetric: View {
    let caption: String
    let value: String
    var unit: String? = nil
    var subtitle: String? = nil
    var actionTitle: String? = nil
    var action: (() -> Void)? = nil

    static func accessibilitySummary(caption: String, value: String, unit: String?, subtitle: String?) -> String {
        [caption, value + (unit ?? ""), subtitle].compactMap { $0 }.joined(separator: ", ")
    }

    var body: some View {
        WKCard(padding: WK.Space.md, radius: WK.Radius.lg) {
            VStack(spacing: WK.Space.xxs) {
                Text(caption)
                    .font(WK.Typo.caption)
                    .foregroundStyle(WK.Colors.textSecondary)
                HStack(alignment: .firstTextBaseline, spacing: 2) {
                    Text(value)
                        .font(WK.Typo.display)
                        .foregroundStyle(WK.Colors.textPrimary)
                    if let unit {
                        Text(unit)
                            .font(WK.Typo.title)
                            .foregroundStyle(WK.Colors.textTertiary)
                    }
                }
                .monospacedDigit()
                if let subtitle {
                    Text(subtitle)
                        .font(WK.Typo.caption)
                        .foregroundStyle(WK.Colors.textPrimary)
                }
            }
            .frame(maxWidth: .infinity)
            .accessibilityElement(children: .ignore)
            .accessibilityLabel(Self.accessibilitySummary(caption: caption, value: value, unit: unit, subtitle: subtitle))

            if let actionTitle, let action {
                WKChip(title: actionTitle, isSelected: true, action: action)
                    .frame(maxWidth: .infinity)
                    .padding(.top, WK.Space.xs)
            }
        }
    }
}
