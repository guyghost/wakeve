import SwiftUI

/// Action principale de l'écran. Une seule par écran.
struct WKPrimaryButton: View {
    let title: String
    var systemImage: String? = nil
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Label {
                Text(title)
            } icon: {
                if let systemImage { Image(systemName: systemImage) }
            }
            .labelStyle(.titleAndIcon)
            .font(WK.Typo.headline)
            .foregroundStyle(WK.Colors.onPrimaryButton)
            .frame(maxWidth: .infinity, minHeight: max(WK.Size.primaryButtonHeight, WK.Size.minTapTarget))
            .background(WK.Colors.primaryButton, in: Capsule())
            .contentShape(Capsule())
        }
        .buttonStyle(.plain)
    }
}

/// Suggestion ou filtre.
struct WKChip: View {
    let title: String
    var systemImage: String? = nil
    var isSelected: Bool = false
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: WK.Space.xxs) {
                if let systemImage { Image(systemName: systemImage) }
                Text(title)
            }
            .font(WK.Typo.caption.weight(.medium))
            .foregroundStyle(isSelected ? WK.Colors.onPrimaryButton : WK.Colors.textPrimary)
            .padding(.horizontal, WK.Space.sm)
            .frame(minHeight: WK.Size.minTapTarget)
            .background(isSelected ? WK.Colors.primaryButton : WK.Colors.card, in: Capsule())
            .contentShape(Capsule())
        }
        .buttonStyle(.plain)
        .accessibilityAddTraits(isSelected ? .isSelected : [])
    }
}

/// Bouton rond de chrome (retour, réglages, menu). Seul usage du verre avec la barre flottante.
struct WKCircleButton: View {
    let systemImage: String
    let accessibilityLabel: String
    let action: () -> Void

    @Environment(\.accessibilityReduceTransparency) private var reduceTransparency

    var body: some View {
        Button(action: action) {
            Image(systemName: systemImage)
                .font(WK.Typo.headline)
                .foregroundStyle(WK.Colors.textPrimary)
                .frame(width: WK.Size.minTapTarget, height: WK.Size.minTapTarget)
                .modifier(CircleChrome(reduceTransparency: reduceTransparency))
                .contentShape(Circle())
        }
        .buttonStyle(.plain)
        .accessibilityLabel(accessibilityLabel)
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
