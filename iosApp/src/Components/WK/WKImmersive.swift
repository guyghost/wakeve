import SwiftUI

/// Action d'un écran immersif (CTA blanc ou secondaire à contour).
struct WKImmersiveAction {
    let title: String
    var systemImage: String? = nil
    let action: () -> Void
}

/// Conteneur du mode immersif (spec §5.6, couche 8) : fond de l'ambiance plein écran, contenu défilant,
/// bouton fermer, CTA blanc en bas (et une action secondaire à contour). Toujours sombre : le contenu
/// est rendu en `colorScheme` sombre quel que soit le réglage de l'appareil.
struct WKImmersiveScaffold<Content: View>: View {
    static var closeAccessibilityID: String { "immersive.close" }
    static var primaryAccessibilityID: String { "immersive.primary" }
    static var secondaryAccessibilityID: String { "immersive.secondary" }
    /// Plafond Dynamic Type des actions du bas : en AX5, deux boutons occupaient un tiers de l'écran
    /// au-dessus d'un contenu défilant (même correctif que le flux de création, couche 7).
    static var actionsDynamicTypeLimit: DynamicTypeSize { .accessibility2 }

    let mood: WK.Mood
    var primary: WKImmersiveAction? = nil
    var secondary: WKImmersiveAction? = nil
    let onClose: () -> Void
    @ViewBuilder let content: () -> Content

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: WK.Space.md) {
                content()
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.horizontal, WK.Space.screen)
            .padding(.vertical, WK.Space.md)
        }
        .foregroundStyle(mood.textPrimary)
        .background(mood.background.ignoresSafeArea())
        .safeAreaInset(edge: .top, spacing: 0) { topBar }
        .safeAreaInset(edge: .bottom, spacing: 0) { actions }
        .environment(\.colorScheme, .dark)
    }

    private var topBar: some View {
        HStack {
            Spacer()
            WKCircleButton(
                systemImage: "xmark",
                accessibilityLabel: String(localized: "common.close"),
                accessibilityID: Self.closeAccessibilityID,
                action: onClose
            )
        }
        .padding(.horizontal, WK.Space.screen)
        .padding(.vertical, WK.Space.xxs)
        // Le contenu défile sous la barre : fond opaque de l'ambiance.
        .background(mood.background.ignoresSafeArea(edges: .top))
    }

    @ViewBuilder
    private var actions: some View {
        if primary != nil || secondary != nil {
            VStack(spacing: WK.Space.xs) {
                if let primary {
                    WKImmersiveButton(
                        title: primary.title,
                        systemImage: primary.systemImage,
                        mood: mood,
                        accessibilityID: Self.primaryAccessibilityID,
                        action: primary.action
                    )
                }
                if let secondary {
                    WKImmersiveButton(
                        title: secondary.title,
                        systemImage: secondary.systemImage,
                        style: .secondary,
                        mood: mood,
                        accessibilityID: Self.secondaryAccessibilityID,
                        action: secondary.action
                    )
                }
            }
            .dynamicTypeSize(...Self.actionsDynamicTypeLimit)
            .padding(.horizontal, WK.Space.screen)
            .padding(.vertical, WK.Space.xs)
            .background(mood.background.ignoresSafeArea(edges: .bottom))
        }
    }
}

/// Bouton du mode immersif : CTA blanc (`primary`) ou bouton à contour fin (`secondary`).
struct WKImmersiveButton: View {
    enum Style { case primary, secondary }

    let title: String
    var systemImage: String? = nil
    var style: Style = .primary
    let mood: WK.Mood
    var accessibilityID: String? = nil
    let action: () -> Void

    @Environment(\.dynamicTypeSize) private var dynamicTypeSize

    var body: some View {
        Button(action: action) {
            Group {
                // Aux tailles d'accessibilité, le titre seul : l'icône prendrait une ligne entière.
                if let systemImage, !dynamicTypeSize.isAccessibilitySize {
                    Label(title, systemImage: systemImage)
                        .labelStyle(.titleAndIcon)
                } else {
                    Text(title)
                }
            }
            .font(WK.Typo.headline)
            .multilineTextAlignment(.center)
            .foregroundStyle(style == .primary ? mood.onCta : mood.textPrimary)
            .padding(.horizontal, WK.Space.md)
            .padding(.vertical, WK.Space.xs)
            .frame(maxWidth: .infinity, minHeight: max(WK.Size.primaryButtonHeight, WK.Size.minTapTarget))
            .background {
                if style == .primary {
                    WK.pill.fill(mood.cta)
                } else {
                    WK.pill.strokeBorder(mood.pillStroke, lineWidth: WK.Stroke.hairline)
                }
            }
            .contentShape(WK.pill)
        }
        .buttonStyle(.plain)
        .accessibilityLabel(title)
        .wkAccessibilityID(accessibilityID)
    }
}

/// Pastille logistique du mode immersif : contour fin, texte clair, sans fond.
struct WKImmersivePill: View {
    let text: String
    var systemImage: String? = nil
    let mood: WK.Mood

    var body: some View {
        HStack(alignment: .firstTextBaseline, spacing: WK.Space.xxs) {
            if let systemImage {
                Image(systemName: systemImage)
                    .accessibilityHidden(true)
            }
            Text(text)
                .fixedSize(horizontal: false, vertical: true)
        }
        .font(WK.Typo.caption.weight(.medium))
        .foregroundStyle(mood.textPrimary)
        .padding(.horizontal, WK.Space.sm)
        .padding(.vertical, WK.Space.xs)
        .overlay(WK.pill.strokeBorder(mood.pillStroke, lineWidth: WK.Stroke.hairline))
        .accessibilityElement(children: .combine)
    }
}

/// Carte posée sur le fond immersif (`mood.surface`), contour ajouté sous Increase Contrast.
struct WKImmersiveCard<Content: View>: View {
    let mood: WK.Mood
    @ViewBuilder let content: () -> Content

    @Environment(\.colorSchemeContrast) private var contrast

    var body: some View {
        // VStack explicite : un contenu à plusieurs enfants forme une seule carte.
        VStack(alignment: .leading, spacing: WK.Space.xs) {
            content()
        }
        .padding(WK.Space.md)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(WK.shape(WK.Radius.lg).fill(mood.surface))
        .overlay {
            if contrast == .increased {
                WK.shape(WK.Radius.lg).strokeBorder(mood.pillStroke, lineWidth: WK.Stroke.hairline)
            }
        }
    }
}
