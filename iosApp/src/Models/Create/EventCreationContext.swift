import Foundation

/// Contexte remis au lancement du flux de création (4 questions) : la checklist du modèle choisi,
/// gardée avec l'événement (`EventChecklistStoring`) et affichée dans le hub.
struct EventCreationContext: Equatable {
    let templateChecklist: [String]

    init(templateChecklist: [String]) {
        self.templateChecklist = templateChecklist
    }

    init(form: CreateEventForm) {
        self.init(templateChecklist: form.checklist)
    }
}
