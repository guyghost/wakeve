import SwiftUI

/// Tuile de module du hub d'événement (Date, Lieu, Transport…).
struct WKModuleTile: View {
    let systemImage: String
    let title: String
    let summary: String
    var status: WK.Status? = nil
    var isHighlighted: Bool = false
    let action: () -> Void

    @Environment(\.dynamicTypeSize) private var dynamicTypeSize

    /// "Transport, 2 sans place, En attente" — le statut est toujours nommé, jamais porté par la couleur seule.
    static func accessibilityLabel(title: String, summary: String, status: WK.Status?) -> String {
        [title, summary, status?.localizedName].compactMap { $0 }.joined(separator: ", ")
    }

    /// Le surlignage signale la prochaine étape ; ce n'est pas une sélection.
    static func accessibilityValue(isHighlighted: Bool) -> String {
        isHighlighted ? String(localized: "wk.module.highlight") : ""
    }

    var body: some View {
        Button(action: action) {
            WKCard(style: isHighlighted ? .selected : .standard) {
                VStack(alignment: .leading, spacing: WK.Space.xxs) {
                    Image(systemName: systemImage)
                        .font(WK.Typo.headline)
                        .foregroundStyle(WK.Colors.accent)
                    Text(title)
                        .font(WK.Typo.headline)
                        .foregroundStyle(WK.Colors.textPrimary)
                    HStack(alignment: .firstTextBaseline, spacing: WK.Space.xxs) {
                        if let status {
                            // Indice non textuel (3:1) ; le nom du statut est lu par VoiceOver.
                            Image(systemName: "circle.fill")
                                .font(WK.Typo.micro)
                                .imageScale(.small)
                                .foregroundStyle(status.color)
                                .accessibilityHidden(true)
                        }
                        Text(summary)
                            .font(WK.Typo.caption)
                            .foregroundStyle(WK.Colors.textMuted)
                            .lineLimit(dynamicTypeSize.isAccessibilitySize ? nil : 2)
                    }
                }
                .frame(maxWidth: .infinity, minHeight: WK.Size.minTapTarget * 2, alignment: .topLeading)
            }
            .contentShape(WK.shape(WK.Radius.md))
        }
        .buttonStyle(.plain)
        .accessibilityLabel(Self.accessibilityLabel(title: title, summary: summary, status: status))
        .accessibilityValue(Self.accessibilityValue(isHighlighted: isHighlighted))
    }
}
