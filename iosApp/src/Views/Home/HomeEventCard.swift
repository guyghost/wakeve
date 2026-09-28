import SwiftUI

/// Carte d'événement de la grille d'accueil (couche 3, #47) : avatars, titre, statut coloré et libellé.
struct HomeEventCard: View {
    let summary: HomeEventSummary
    let onOpen: () -> Void
    /// Actions VoiceOver, présentes seulement quand elles sont permises.
    var onEditDraft: (() -> Void)? = nil
    var onDelete: (() -> Void)? = nil

    @Environment(\.dynamicTypeSize) private var dynamicTypeSize

    /// Libellé du statut dans la langue de l'app (clé localisée ou date courte).
    static func statusText(for summary: HomeEventSummary) -> String {
        switch summary.label {
        case .key(let key): return String(localized: String.LocalizationValue(key))
        case .date(let date): return HomeDateText.short(date)
        }
    }

    static func avatars(for summary: HomeEventSummary) -> [WKAvatar] {
        summary.facts.participantNames.enumerated().map { index, name in
            WKAvatar(id: "\(summary.id)-\(index)", name: name)
        }
    }

    /// « titre, statut, noms » pour VoiceOver ; `locale` règle la liste des noms.
    static func accessibilityLabel(for summary: HomeEventSummary, locale: Locale = WK.appLocale) -> String {
        let people = WKAvatarStack.accessibilityLabel(for: avatars(for: summary), locale: locale)
        return [summary.facts.title, statusText(for: summary), people]
            .filter { !$0.isEmpty }
            .joined(separator: ", ")
    }

    var body: some View {
        Button(action: onOpen) {
            WKCard {
                let avatars = Self.avatars(for: summary)
                if !avatars.isEmpty {
                    WKAvatarStack(avatars: avatars)
                }
                Text(summary.facts.title)
                    .font(WK.Typo.headline)
                    .foregroundStyle(WK.Colors.textPrimary)
                    .multilineTextAlignment(.leading)
                    .lineLimit(dynamicTypeSize.isAccessibilitySize ? nil : 2)
                    .frame(maxWidth: .infinity, alignment: .leading)
                WKStatusPill(text: Self.statusText(for: summary), status: summary.status)
            }
            .frame(minHeight: WK.Size.minTapTarget, alignment: .topLeading)
            .contentShape(WK.shape(WK.Radius.md))
        }
        .buttonStyle(.plain)
        .accessibilityLabel(Self.accessibilityLabel(for: summary))
        .accessibilityActions {
            if let onEditDraft {
                Button(String(localized: "home.v2.menu.edit"), action: onEditDraft)
            }
            if let onDelete {
                Button(String(localized: "home.v2.menu.delete"), action: onDelete)
            }
        }
        .wkAccessibilityID("home.card.\(summary.id)")
    }
}
