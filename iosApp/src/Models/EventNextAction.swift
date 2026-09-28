import SwiftUI
import Shared

// MARK: - Event Next Action

/// Prochaine action utile pour un événement, dérivée de son statut.
/// Déplacé depuis HomeView.swift (couche 0 de la refonte iOS, proposition #47).
struct EventNextAction {
    let title: String
    let shortTitle: String
    let subtitle: String
    let blockedReason: String?
    let systemImage: String

    var isBlocked: Bool { blockedReason != nil }
    var displaySubtitle: String { blockedReason ?? subtitle }

    init(event: Event) {
        switch event.status {
        case .draft:
            title = String(localized: "events.next_action.draft.title")
            shortTitle = String(localized: "events.next_action.draft.short")
            systemImage = "paperplane.fill"

            let hasSlots = !event.proposedSlots.isEmpty
            if !hasSlots {
                blockedReason = String(localized: "events.next_action.draft.blocked.slots")
            } else {
                blockedReason = nil
            }
            subtitle = String(localized: "events.next_action.draft.subtitle")

        case .polling:
            title = String(localized: "events.next_action.polling.title")
            shortTitle = String(localized: "events.next_action.polling.short")
            subtitle = String(localized: "events.next_action.polling.subtitle")
            blockedReason = nil
            systemImage = "chart.bar.fill"

        case .confirmed, .comparing:
            title = String(localized: "events.next_action.confirmed.title")
            shortTitle = String(localized: "events.next_action.confirmed.short")
            subtitle = String(localized: "events.next_action.confirmed.subtitle")
            blockedReason = nil
            systemImage = "map.fill"

        case .organizing:
            title = String(localized: "events.next_action.organizing.title")
            shortTitle = String(localized: "events.next_action.organizing.short")
            subtitle = String(localized: "events.next_action.organizing.subtitle")
            blockedReason = nil
            systemImage = "checklist"

        case .finalized:
            title = String(localized: "events.next_action.finalized.title")
            shortTitle = String(localized: "events.next_action.finalized.short")
            subtitle = String(localized: "events.next_action.finalized.subtitle")
            blockedReason = nil
            systemImage = "checkmark.seal.fill"

        default:
            title = String(localized: "events.next_action.default.title")
            shortTitle = String(localized: "events.next_action.default.short")
            subtitle = String(localized: "events.next_action.default.subtitle")
            blockedReason = nil
            systemImage = "arrow.right"
        }
    }
}
// END EventNextAction
