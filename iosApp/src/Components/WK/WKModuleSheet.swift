import SwiftUI

/// Constantes du conteneur de sheet (hors du type générique, qui ne peut pas porter de propriétés statiques stockées).
enum WKModuleSheetChrome {
    /// Identifiant d'accessibilité stable du bouton fermer (spec §7).
    static let closeAccessibilityID = "wk.sheet.close"
    /// Libellé existant « Fermer ».
    static let closeLabelKey = "common.close"
    static let primaryAccessibilityID = "wk.sheet.primary"

    /// Moyen/grand ; aux tailles d'accessibilité, grand seulement (à mi-hauteur l'en-tête occuperait tout).
    static func detents(for dynamicTypeSize: DynamicTypeSize) -> Set<PresentationDetent> {
        dynamicTypeSize.isAccessibilitySize ? [.large] : [.medium, .large]
    }
}

/// Sheet d'un module du hub (couche 5a, #47) : en-tête (titre, pastille, fermer), cartes défilantes,
/// phrase « ce qui manque », barre d'actions en bas. Detents moyen/grand (grand seul aux tailles d'accessibilité).
struct WKModuleSheet<Content: View>: View {
    struct Status: Equatable {
        let text: String
        let status: WK.Status
    }

    let title: String
    var status: Status? = nil
    var missing: String? = nil
    let primary: (title: String, action: () -> Void)?
    var secondary: [WKActionBar.Item] = []
    let onClose: () -> Void
    @ViewBuilder let content: () -> Content

    @Environment(\.dynamicTypeSize) private var dynamicTypeSize

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: WK.Space.sm) {
                content()
                if let missing {
                    Text(missing)
                        .font(WK.Typo.body)
                        .foregroundStyle(WK.Colors.textMuted)
                        .fixedSize(horizontal: false, vertical: true)
                        .frame(maxWidth: .infinity, alignment: .leading)
                }
            }
            .padding(.horizontal, WK.Space.screen)
            .padding(.vertical, WK.Space.sm)
        }
        .safeAreaInset(edge: .top, spacing: 0) {
            WKModuleSheetHeader(
                title: title,
                status: status.map { .init(text: $0.text, status: $0.status) },
                onClose: onClose
            )
        }
        .safeAreaInset(edge: .bottom, spacing: 0) { bottomBar }
        .background(WK.Colors.canvas.ignoresSafeArea())
        .presentationDetents(WKModuleSheetChrome.detents(for: dynamicTypeSize))
        .presentationDragIndicator(.visible)
        .presentationBackground(WK.Colors.canvas)
    }

    @ViewBuilder
    private var bottomBar: some View {
        if let primary {
            WKActionBar(
                primaryTitle: primary.title,
                primaryAction: primary.action,
                secondary: secondary,
                primaryAccessibilityID: WKModuleSheetChrome.primaryAccessibilityID
            )
            .frame(maxWidth: .infinity, alignment: .leading)
            .modifier(WKModuleSheetBarChrome())
        } else if !secondary.isEmpty {
            WKModuleSheetSecondaryRow(items: secondary)
                .frame(maxWidth: .infinity, alignment: .leading)
                .modifier(WKModuleSheetBarChrome())
        }
    }
}

/// En-tête de la sheet : titre replié sans troncature, pastille à droite (dessous si la place manque), fermer.
struct WKModuleSheetHeader: View {
    struct Status: Equatable {
        let text: String
        let status: WK.Status
    }

    let title: String
    let status: Status?
    let onClose: () -> Void

    var body: some View {
        HStack(alignment: .top, spacing: WK.Space.xs) {
            ViewThatFits(in: .horizontal) {
                HStack(alignment: .firstTextBaseline, spacing: WK.Space.xs) {
                    titleText.fixedSize()
                    pill
                }
                VStack(alignment: .leading, spacing: WK.Space.xxs) {
                    titleText.fixedSize(horizontal: false, vertical: true)
                    pill
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.top, WK.Space.xs)
            WKCircleButton(
                systemImage: "xmark",
                accessibilityLabel: String(localized: String.LocalizationValue(WKModuleSheetChrome.closeLabelKey)),
                accessibilityID: WKModuleSheetChrome.closeAccessibilityID,
                action: onClose
            )
        }
        .padding(.horizontal, WK.Space.screen)
        .padding(.top, WK.Space.md)
        .padding(.bottom, WK.Space.xs)
        // Le contenu défile sous l'en-tête : fond opaque.
        .background(WK.Colors.canvas)
    }

    private var titleText: some View {
        Text(title)
            .font(WK.Typo.title)
            .foregroundStyle(WK.Colors.textPrimary)
            .accessibilityAddTraits(.isHeader)
    }

    @ViewBuilder
    private var pill: some View {
        if let status {
            WKStatusPill(text: status.text, status: status.status)
        }
    }
}

/// Actions secondaires sans action principale : puces libellées, empilées aux tailles d'accessibilité.
struct WKModuleSheetSecondaryRow: View {
    let items: [WKActionBar.Item]

    @Environment(\.dynamicTypeSize) private var dynamicTypeSize

    var body: some View {
        let layout = dynamicTypeSize.isAccessibilitySize
            ? AnyLayout(VStackLayout(alignment: .leading, spacing: WK.Space.xs))
            : AnyLayout(HStackLayout(spacing: WK.Space.xs))
        layout {
            ForEach(items) { item in
                WKChip(title: item.label, systemImage: item.systemImage, accessibilityID: item.accessibilityID, action: item.action)
            }
        }
    }
}

/// Barre du bas opaque, au-dessus du contenu qui défile.
private struct WKModuleSheetBarChrome: ViewModifier {
    func body(content: Content) -> some View {
        content
            .padding(.horizontal, WK.Space.screen)
            .padding(.vertical, WK.Space.xs)
            .background(WK.Colors.canvas.ignoresSafeArea(edges: .bottom))
    }
}
