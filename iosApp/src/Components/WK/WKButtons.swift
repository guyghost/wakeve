import SwiftUI

/// Action principale de l'écran. Une seule par écran.
struct WKPrimaryButton: View {
    let title: String
    var systemImage: String? = nil
    var accessibilityID: String? = nil
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Group {
                if let systemImage {
                    Label(title, systemImage: systemImage)
                        .labelStyle(.titleAndIcon)
                } else {
                    Text(title)
                }
            }
            .font(WK.Typo.headline)
            .multilineTextAlignment(.center)
            .foregroundStyle(WK.Colors.onPrimaryButton)
            .padding(.horizontal, WK.Space.md)
            .padding(.vertical, WK.Space.xs)
            .frame(maxWidth: .infinity, minHeight: max(WK.Size.primaryButtonHeight, WK.Size.minTapTarget))
            .background(WK.Colors.primaryButton, in: WK.pill)
            .contentShape(WK.pill)
        }
        .buttonStyle(.plain)
        .wkAccessibilityID(accessibilityID)
    }
}

/// Suggestion ou filtre.
struct WKChip: View {
    /// `prominent` = action mise en avant (sans sémantique de sélection).
    enum Style { case standard, prominent }

    let title: String
    var systemImage: String? = nil
    var style: Style = .standard
    var isSelected: Bool = false
    var accessibilityID: String? = nil
    let action: () -> Void

    private var isFilled: Bool { style == .prominent || isSelected }

    var body: some View {
        Button(action: action) {
            HStack(spacing: WK.Space.xxs) {
                if let systemImage { Image(systemName: systemImage) }
                Text(title)
            }
            .font(WK.Typo.caption.weight(.medium))
            .multilineTextAlignment(.center)
            .foregroundStyle(isFilled ? WK.Colors.onPrimaryButton : WK.Colors.textPrimary)
            .padding(.horizontal, WK.Space.sm)
            .padding(.vertical, WK.Space.xs)
            .frame(minHeight: WK.Size.minTapTarget)
            .background(isFilled ? WK.Colors.primaryButton : WK.Colors.cardInset, in: WK.pill)
            .contentShape(WK.pill)
        }
        .buttonStyle(.plain)
        .accessibilityAddTraits(isSelected ? .isSelected : [])
        .wkAccessibilityID(accessibilityID)
    }
}

/// Bouton rond de chrome (retour, réglages, menu). Seul usage du verre avec la barre flottante.
struct WKCircleButton: View {
    let systemImage: String
    let accessibilityLabel: String
    var accessibilityID: String? = nil
    let action: () -> Void

    @Environment(\.accessibilityReduceTransparency) private var reduceTransparency

    var body: some View {
        Button(action: action) {
            Image(systemName: systemImage)
                .font(WK.Typo.headline)
                .foregroundStyle(.primary)
                .frame(minWidth: WK.Size.minTapTarget, minHeight: WK.Size.minTapTarget)
                .modifier(CircleChrome(reduceTransparency: reduceTransparency))
                .contentShape(Circle())
        }
        .buttonStyle(.plain)
        // Glyphe dans un cercle : plafonné, le Large Content Viewer prend le relais au-delà.
        .dynamicTypeSize(...DynamicTypeSize.accessibility1)
        .accessibilityLabel(accessibilityLabel)
        .accessibilityShowsLargeContentViewer {
            Label(accessibilityLabel, systemImage: systemImage)
        }
        .wkAccessibilityID(accessibilityID)
    }

    private struct CircleChrome: ViewModifier {
        let reduceTransparency: Bool

        func body(content: Content) -> some View {
            if #available(iOS 26.0, *), !reduceTransparency {
                content.glassEffect(.regular.interactive(), in: Circle())
            } else if reduceTransparency {
                content.background(WK.Colors.card, in: Circle())
            } else {
                content.background(.regularMaterial, in: Circle())
            }
        }
    }
}
