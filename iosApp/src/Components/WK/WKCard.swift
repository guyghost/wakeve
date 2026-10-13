import SwiftUI

struct WKCard<Content: View>: View {
    enum Style { case standard, inset, selected }

    var style: Style = .standard
    var padding: CGFloat = WK.Space.sm
    var radius: CGFloat = WK.Radius.md
    /// Teinte légère superposée au fond (hero d'un événement), à l'opacité `WK.Tint.surface`.
    var tint: Color? = nil
    @ViewBuilder let content: () -> Content

    var body: some View {
        // VStack explicite : un contenu à plusieurs enfants forme une seule carte.
        VStack(alignment: .leading, spacing: WK.Space.xs) {
            content()
        }
        .padding(padding)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background {
            // Teinte au-dessus du fond de carte, sous le contenu : le texte garde sa couleur.
            ZStack {
                WK.shape(radius).fill(style == .inset ? WK.Colors.cardInset : WK.Colors.card)
                if let tint {
                    WK.shape(radius).fill(tint.opacity(WK.Tint.surface))
                }
            }
        }
        .overlay {
            if style == .selected {
                WK.shape(radius).strokeBorder(WK.Colors.accent, lineWidth: WK.Stroke.emphasis)
            }
        }
    }
}
