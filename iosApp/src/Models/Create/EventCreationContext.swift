import Foundation

/// Contexte auxiliaire remis à la fin de la création (flux 4 questions) : lieu potentiel,
/// modèle source et checklist préparée.
struct EventCreationContext {
    let potentialLocationName: String?
    let sourceScenario: EventScenario?
    let preparedChecklist: [ChecklistItem]
}
