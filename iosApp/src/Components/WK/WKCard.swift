import SwiftUI

struct WKCard<Content: View>: View {
    enum Style { case standard, inset, selected }

    var style: Style = .standard
    var padding: CGFloat = WK.Space.sm
    var radius: CGFloat = WK.Radius.md
    @ViewBuilder let content: () -> Content

    var body: some View {
        content()
            .padding(padding)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(style == .inset ? WK.Colors.cardInset : WK.Colors.card, in: WK.shape(radius))
            .overlay {
                if style == .selected {
                    WK.shape(radius).strokeBorder(WK.Colors.accent, lineWidth: 1.5)
                }
            }
    }
}
