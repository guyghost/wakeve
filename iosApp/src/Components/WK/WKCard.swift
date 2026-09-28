import SwiftUI

struct WKCard<Content: View>: View {
    enum Style { case standard, inset, selected }

    var style: Style = .standard
    var padding: CGFloat = WK.Space.sm
    var radius: CGFloat = WK.Radius.md
    @ViewBuilder let content: () -> Content

    var body: some View {
        // VStack explicite : un contenu à plusieurs enfants forme une seule carte.
        VStack(alignment: .leading, spacing: WK.Space.xs) {
            content()
        }
        .padding(padding)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(style == .inset ? WK.Colors.cardInset : WK.Colors.card, in: WK.shape(radius))
        .overlay {
            if style == .selected {
                WK.shape(radius).strokeBorder(WK.Colors.accent, lineWidth: WK.Stroke.emphasis)
            }
        }
    }
}
