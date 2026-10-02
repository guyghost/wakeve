import SwiftUI

/// Cartes du hub sous la grille de modules (revue couche 9, #47) : ce que l'ancien détail montrait
/// et que les tuiles ne portent pas — checklist de l'événement, suggestions IA.
struct EventHubSupplements: View {
    let facts: EventHubFacts
    @ObservedObject var checklist: EventChecklistModel
    @ObservedObject var ai: EventHubAIModel
    /// « Ajouter des dates » des idées de sondage (route `.addDates` du hub).
    let onAddDates: () -> Void

    var body: some View {
        if EventHubChecklistCard.isVisible(items: checklist.items, facts: facts) {
            EventHubChecklistCard(items: checklist.items, onToggle: checklist.toggle)
        }
        let sections = EventHubAI.sections(for: facts)
        if !sections.isEmpty {
            EventHubAICard(
                sections: sections,
                state: ai.state,
                checklistItems: checklist.items,
                offersAddDates: EventHubAI.offersAddDates(for: facts),
                onGenerate: { Task { await ai.generate(facts: facts) } },
                // Une suggestion ajoutée rejoint la checklist de l'événement (celle du modèle de création).
                onAddToChecklist: { title in checklist.add([title]) },
                onAddDates: onAddDates
            )
        }
    }
}
