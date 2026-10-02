import SwiftUI

/// Cartes du hub sous la grille de modules (revue couche 9, #47) : ce que l'ancien détail montrait
/// et que les tuiles ne portent pas.
struct EventHubSupplements: View {
    let facts: EventHubFacts
    @ObservedObject var checklist: EventChecklistModel

    var body: some View {
        if EventHubChecklistCard.isVisible(items: checklist.items, facts: facts) {
            EventHubChecklistCard(items: checklist.items, onToggle: checklist.toggle)
        }
    }
}
