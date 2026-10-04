import SwiftUI
import UIKit

/// Carte « Suggestions » sous la grille du hub (revue couche 9, #47) : générées à la demande, à relire.
/// Résumé, idées de sondage (organisateur, brouillon ou sondage), checklist (ajout à la checklist de
/// l'événement), messages d'invitation (copier, partager).
struct EventHubAICard: View {
    static let accessibilityID = "hub.ai"

    let sections: [EventHubAI.Section]
    let state: EventHubAIModel.State
    let checklistItems: [EventChecklistItem]
    let offersAddDates: Bool
    let onGenerate: () -> Void
    let onAddToChecklist: (String) -> Void
    let onAddDates: () -> Void

    @State private var variant: EventHubAI.InvitationVariant = .simple

    var body: some View {
        WKCard(padding: WK.Space.md, radius: WK.Radius.lg) {
            Label {
                Text(String(localized: "hub.ai.title"))
                    .font(WK.Typo.headline)
                    .foregroundStyle(WK.Colors.textPrimary)
            } icon: {
                Image(systemName: "sparkles")
                    .foregroundStyle(WK.Colors.accent)
                    .accessibilityHidden(true)
            }
            .accessibilityAddTraits(.isHeader)
            Text(String(localized: "hub.ai.subtitle"))
                .font(WK.Typo.caption)
                .foregroundStyle(WK.Colors.textSecondary)
                .fixedSize(horizontal: false, vertical: true)

            switch state {
            case .idle:
                generateButton(titleKey: "hub.ai.generate", style: .prominent)
            case .loading:
                HStack(spacing: WK.Space.xs) {
                    ProgressView()
                        .accessibilityHidden(true)
                    Text(String(localized: "ai.preparing"))
                        .font(WK.Typo.caption)
                        .foregroundStyle(WK.Colors.textSecondary)
                }
                .frame(minHeight: WK.Size.minTapTarget)
                .accessibilityElement(children: .combine)
            case .failed:
                Label {
                    Text(String(localized: "hub.ai.error"))
                        .foregroundStyle(WK.Colors.textPrimary)
                        .fixedSize(horizontal: false, vertical: true)
                } icon: {
                    Image(systemName: "exclamationmark.triangle.fill")
                        .foregroundStyle(WK.Status.actionNeeded.color)
                        .accessibilityHidden(true)
                }
                .font(WK.Typo.caption)
                generateButton(titleKey: "common.retry", style: .standard)
            case .loaded(let result):
                loaded(result)
                generateButton(titleKey: "hub.ai.regenerate", style: .standard)
            }
        }
        .wkAccessibilityID(Self.accessibilityID)
    }

    private func generateButton(titleKey: String, style: WKChip.Style) -> some View {
        WKChip(
            title: String(localized: String.LocalizationValue(titleKey)),
            systemImage: "sparkles",
            style: style,
            accessibilityID: "hub.ai.generate",
            action: onGenerate
        )
    }

    @ViewBuilder
    private func loaded(_ result: EventHubAIResult) -> some View {
        if sections.contains(.summary), let summary = result.summary {
            block(titleKey: "hub.ai.summary_title") {
                list(titleKey: "hub.ai.decided", values: summary.decided)
                list(titleKey: "hub.ai.missing", values: summary.missing)
                Text(summary.recommendedNextAction)
                    .font(WK.Typo.body)
                    .foregroundStyle(WK.Colors.textPrimary)
                    .fixedSize(horizontal: false, vertical: true)
            }
        }
        if sections.contains(.polls), !result.polls.isEmpty {
            block(titleKey: "hub.ai.polls_title") {
                ForEach(Array(result.polls.enumerated()), id: \.offset) { _, poll in
                    VStack(alignment: .leading, spacing: WK.Space.xxxs) {
                        Text(poll.question)
                            .font(WK.Typo.body)
                            .foregroundStyle(WK.Colors.textPrimary)
                        Text(poll.options.joined(separator: " · "))
                            .font(WK.Typo.caption)
                            .foregroundStyle(WK.Colors.textSecondary)
                    }
                    .fixedSize(horizontal: false, vertical: true)
                    .accessibilityElement(children: .combine)
                }
                if offersAddDates {
                    WKChip(
                        title: String(localized: "hub.primary.add_dates"),
                        systemImage: "calendar.badge.plus",
                        accessibilityID: "hub.ai.addDates",
                        action: onAddDates
                    )
                }
            }
        }
        if sections.contains(.checklist), !result.checklist.isEmpty {
            block(titleKey: "hub.ai.checklist_title") {
                ForEach(Array(result.checklist.enumerated()), id: \.offset) { _, item in
                    checklistRow(item)
                }
            }
        }
        if sections.contains(.invitation), let invitation = result.invitation {
            block(titleKey: "hub.ai.invitation_title") {
                invitationBlock(invitation)
            }
        }
    }

    private func block<Content: View>(titleKey: String, @ViewBuilder content: () -> Content) -> some View {
        let body = content()
        return WKCard(style: .inset, padding: WK.Space.sm) {
            Text(String(localized: String.LocalizationValue(titleKey)))
                .font(WK.Typo.caption.weight(.semibold))
                .foregroundStyle(WK.Colors.textSecondary)
                .accessibilityAddTraits(.isHeader)
            body
        }
    }

    @ViewBuilder
    private func list(titleKey: String, values: [String]) -> some View {
        if !values.isEmpty {
            Text(String(localized: String.LocalizationValue(titleKey)))
                .font(WK.Typo.micro)
                .foregroundStyle(WK.Colors.textSecondary)
            ForEach(values, id: \.self) { value in
                Label(value, systemImage: "circle.fill")
                    .labelStyle(EventHubAIBulletStyle())
                    .font(WK.Typo.caption)
                    .foregroundStyle(WK.Colors.textPrimary)
                    .fixedSize(horizontal: false, vertical: true)
            }
        }
    }

    private func checklistRow(_ item: ChecklistItem) -> some View {
        let added = EventHubAI.isInChecklist(item, existing: checklistItems)
        return ViewThatFits(in: .horizontal) {
            HStack(spacing: WK.Space.xs) {
                checklistTitle(item)
                Spacer(minLength: WK.Space.xs)
                checklistAction(item, added: added)
            }
            VStack(alignment: .leading, spacing: WK.Space.xxs) {
                checklistTitle(item)
                checklistAction(item, added: added)
            }
        }
    }

    private func checklistTitle(_ item: ChecklistItem) -> some View {
        Text(item.title)
            .font(WK.Typo.body)
            .foregroundStyle(WK.Colors.textPrimary)
            .fixedSize(horizontal: false, vertical: true)
    }

    @ViewBuilder
    private func checklistAction(_ item: ChecklistItem, added: Bool) -> some View {
        if added {
            Label(String(localized: "hub.ai.in_checklist"), systemImage: "checkmark.circle.fill")
                .font(WK.Typo.caption)
                .foregroundStyle(WK.Colors.textSecondary)
                .frame(minHeight: WK.Size.minTapTarget)
        } else {
            WKChip(
                title: String(localized: "hub.ai.add_to_checklist"),
                systemImage: "plus",
                accessibilityID: "hub.ai.addToChecklist",
                action: { onAddToChecklist(item.title) }
            )
            .accessibilityLabel(String(format: String(localized: "hub.ai.add_to_checklist_a11y_format"), item.title))
        }
    }

    private func invitationBlock(_ invitation: InvitationMessageSet) -> some View {
        let text = variant.text(in: invitation)
        return VStack(alignment: .leading, spacing: WK.Space.xs) {
            EventHubAIChipRow {
                ForEach(EventHubAI.InvitationVariant.allCases) { option in
                    WKChip(title: option.title(), isSelected: option == variant, action: { variant = option })
                }
            }
            Text(text)
                .font(WK.Typo.body)
                .foregroundStyle(WK.Colors.textPrimary)
                .fixedSize(horizontal: false, vertical: true)
                .textSelection(.enabled)
            EventHubAIChipRow {
                WKChip(
                    title: String(localized: "hub.ai.copy"),
                    systemImage: "doc.on.doc",
                    accessibilityID: "hub.ai.copy",
                    action: {
                        UIPasteboard.general.string = text
                        WakeveHaptics.success()
                        AccessibilityNotification.Announcement(String(localized: "hub.ai.copied")).post()
                    }
                )
                ShareLink(item: text) {
                    Label(String(localized: "hub.ai.share"), systemImage: "square.and.arrow.up")
                        .font(WK.Typo.caption.weight(.medium))
                        .foregroundStyle(WK.Colors.textPrimary)
                        .padding(.horizontal, WK.Space.sm)
                        .padding(.vertical, WK.Space.xs)
                        .frame(minHeight: WK.Size.minTapTarget)
                        .background(WK.Colors.cardInset, in: WK.pill)
                }
                .wkAccessibilityID("hub.ai.share")
            }
        }
    }
}

/// Pastilles en ligne, empilées aux tailles d'accessibilité.
private struct EventHubAIChipRow<Content: View>: View {
    @ViewBuilder let content: () -> Content
    @Environment(\.dynamicTypeSize) private var dynamicTypeSize

    var body: some View {
        let layout = dynamicTypeSize.isAccessibilitySize
            ? AnyLayout(VStackLayout(alignment: .leading, spacing: WK.Space.xs))
            : AnyLayout(HStackLayout(spacing: WK.Space.xs))
        layout { content() }
    }
}

/// Puce discrète devant un élément de liste.
private struct EventHubAIBulletStyle: LabelStyle {
    func makeBody(configuration: Configuration) -> some View {
        HStack(alignment: .firstTextBaseline, spacing: WK.Space.xs) {
            configuration.icon
                .font(WK.Typo.micro)
                .imageScale(.small)
                .foregroundStyle(WK.Colors.textTertiary)
                .accessibilityHidden(true)
            configuration.title
        }
    }
}
