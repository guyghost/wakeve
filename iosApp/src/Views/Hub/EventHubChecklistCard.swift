import SwiftUI

/// Checklist de l'événement sous la grille du hub (revue couche 9, #47) : checklist du modèle choisi à la
/// création et suggestions ajoutées depuis la carte IA, cases cochées gardées sur cet appareil.
struct EventHubChecklistCard: View {
    static let accessibilityID = "hub.checklist"

    let items: [EventChecklistItem]
    let onToggle: (String) -> Void

    /// Visible avec au moins un élément et l'accès aux détails (règle de la carte IA).
    static func isVisible(items: [EventChecklistItem], facts: EventHubFacts) -> Bool {
        !items.isEmpty && facts.hasDetailsAccess
    }

    /// « 1/3 faits ».
    static func progressText(_ progress: EventChecklist.Progress, locale: Locale = WK.appLocale) -> String {
        String(format: WK.localizedFormat("hub.checklist.progress_format", locale: locale), locale: locale, progress.done, progress.total)
    }

    var body: some View {
        WKCard(padding: WK.Space.md, radius: WK.Radius.lg) {
            ViewThatFits(in: .horizontal) {
                HStack(alignment: .firstTextBaseline) {
                    title
                    Spacer(minLength: WK.Space.xs)
                    progress
                }
                VStack(alignment: .leading, spacing: WK.Space.xxs) {
                    title
                    progress
                }
            }
            Text(String(localized: "hub.checklist.local_note"))
                .font(WK.Typo.micro)
                .foregroundStyle(WK.Colors.textSecondary)
                .fixedSize(horizontal: false, vertical: true)
            ForEach(items) { item in
                row(item)
            }
        }
        .wkAccessibilityID(Self.accessibilityID)
    }

    private var title: some View {
        Text(String(localized: "hub.checklist.title"))
            .font(WK.Typo.headline)
            .foregroundStyle(WK.Colors.textPrimary)
            .accessibilityAddTraits(.isHeader)
    }

    private var progress: some View {
        Text(Self.progressText(EventChecklist.progress(items)))
            .font(WK.Typo.caption)
            .foregroundStyle(WK.Colors.textSecondary)
    }

    private func row(_ item: EventChecklistItem) -> some View {
        Button { onToggle(item.id) } label: {
            Label {
                Text(item.title)
                    .strikethrough(item.isDone)
                    .foregroundStyle(item.isDone ? WK.Colors.textSecondary : WK.Colors.textPrimary)
                    .fixedSize(horizontal: false, vertical: true)
            } icon: {
                Image(systemName: item.isDone ? "checkmark.circle.fill" : "circle")
                    .foregroundStyle(item.isDone ? WK.Status.confirmed.color : WK.Colors.textSecondary)
                    .accessibilityHidden(true)
            }
            .font(WK.Typo.body)
            .frame(maxWidth: .infinity, minHeight: WK.Size.minTapTarget, alignment: .leading)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        // Case à cocher : l'état est lu (« sélectionné »), pas seulement montré par la couleur et le trait.
        .accessibilityAddTraits(item.isDone ? .isSelected : [])
        .accessibilityHint(String(localized: "hub.checklist.toggle_hint"))
        .wkAccessibilityID("hub.checklist.item")
    }
}
