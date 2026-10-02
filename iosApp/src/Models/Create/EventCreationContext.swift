import Foundation

/// Contexte auxiliaire remis à la fin de la création (flux 4 questions) : lieu potentiel
/// et modèle source.
struct EventCreationContext {
    let potentialLocationName: String?
    let sourceScenario: EventScenario?
}
