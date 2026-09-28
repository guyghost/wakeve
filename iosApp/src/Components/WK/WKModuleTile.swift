import SwiftUI

/// Tuile de module du hub d'événement (Date, Lieu, Transport…).
struct WKModuleTile: View {
    let systemImage: String
    let title: String
    let summary: String
    var status: WK.Status? = nil
    var isHighlighted: Bool = false
    let action: () -> Void

    static func accessibilityLabel(title: String, summary: String) -> String {
        "\(title), \(summary)"
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
                    Text(summary)
                        .font(WK.Typo.caption)
                        .foregroundStyle(status?.color ?? WK.Colors.textSecondary)
                        .lineLimit(2)
                }
                .frame(maxWidth: .infinity, minHeight: WK.Size.minTapTarget * 2, alignment: .topLeading)
            }
            .contentShape(WK.shape(WK.Radius.md))
        }
        .buttonStyle(.plain)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(Self.accessibilityLabel(title: title, summary: summary))
        .accessibilityAddTraits(.isButton)
        .accessibilityAddTraits(isHighlighted ? .isSelected : [])
    }
}
