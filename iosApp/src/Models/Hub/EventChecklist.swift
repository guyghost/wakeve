import Foundation

/// Élément de la checklist d'un événement (revue couche 9, #47).
struct EventChecklistItem: Codable, Equatable, Identifiable {
    enum Source: String, Codable {
        /// Checklist du modèle choisi à la création (`EventScenario.checklistItems`).
        case template
        /// Ajouté depuis les suggestions de la carte IA du hub.
        case suggestion
    }

    let id: String
    let title: String
    var isDone: Bool
    let source: Source
}

/// Règles pures de la checklist : semis du modèle, ajouts sans doublon, cases cochées.
enum EventChecklist {
    struct Progress: Equatable {
        let done: Int
        let total: Int
    }

    /// Comparaison des titres : espaces, casse et accents ignorés.
    static func normalized(_ title: String) -> String {
        title.trimmingCharacters(in: .whitespacesAndNewlines)
            .folding(options: [.caseInsensitive, .diacriticInsensitive], locale: nil)
    }

    static func contains(_ title: String, in items: [EventChecklistItem]) -> Bool {
        let key = normalized(title)
        return items.contains { normalized($0.title) == key }
    }

    /// Les éléments du modèle deviennent exactement `titles` (dans cet ordre) ; un titre déjà présent garde
    /// son identifiant, son libellé et sa case ; les suggestions ajoutées suivent, inchangées.
    static func seedingTemplate(
        _ titles: [String],
        into items: [EventChecklistItem],
        makeID: () -> String = { UUID().uuidString }
    ) -> [EventChecklistItem] {
        let suggestions = items.filter { $0.source == .suggestion }
        var template: [EventChecklistItem] = []
        for raw in titles {
            let title = raw.trimmingCharacters(in: .whitespacesAndNewlines)
            guard !title.isEmpty, !contains(title, in: template) else { continue }
            if let existing = items.first(where: { $0.source == .template && normalized($0.title) == normalized(title) }) {
                template.append(existing)
            } else {
                template.append(EventChecklistItem(id: makeID(), title: title, isDone: false, source: .template))
            }
        }
        let kept = suggestions.filter { !contains($0.title, in: template) }
        return template + kept
    }

    /// Ajoute des suggestions à la fin, sans titre vide ni doublon.
    static func adding(
        _ titles: [String],
        into items: [EventChecklistItem],
        makeID: () -> String = { UUID().uuidString }
    ) -> [EventChecklistItem] {
        var result = items
        for raw in titles {
            let title = raw.trimmingCharacters(in: .whitespacesAndNewlines)
            guard !title.isEmpty, !contains(title, in: result) else { continue }
            result.append(EventChecklistItem(id: makeID(), title: title, isDone: false, source: .suggestion))
        }
        return result
    }

    static func toggling(_ id: String, in items: [EventChecklistItem]) -> [EventChecklistItem] {
        items.map { item in
            guard item.id == id else { return item }
            var toggled = item
            toggled.isDone.toggle()
            return toggled
        }
    }

    static func progress(_ items: [EventChecklistItem]) -> Progress {
        Progress(done: items.filter(\.isDone).count, total: items.count)
    }
}

/// Stockage de la checklist d'un événement.
protocol EventChecklistStoring {
    func items(eventId: String) -> [EventChecklistItem]
    func save(_ items: [EventChecklistItem], eventId: String)
}

extension EventChecklistStoring {
    func seedTemplate(eventId: String, titles: [String]) {
        let current = items(eventId: eventId)
        let seeded = EventChecklist.seedingTemplate(titles, into: current)
        guard seeded != current else { return }
        save(seeded, eventId: eventId)
    }

    func add(eventId: String, titles: [String]) {
        let current = items(eventId: eventId)
        let added = EventChecklist.adding(titles, into: current)
        guard added != current else { return }
        save(added, eventId: eventId)
    }

    func toggle(eventId: String, itemId: String) {
        save(EventChecklist.toggling(itemId, in: items(eventId: eventId)), eventId: eventId)
    }
}

/// Checklist gardée **sur cet appareil** (JSON dans `UserDefaults`, une clé par événement).
/// Le schéma partagé n'a pas de table de checklist (le matériel a la sienne, `EquipmentItem`) : ni synchronisée
/// ni visible des autres participants, comme l'était la checklist préparée de l'ancienne feuille (en mémoire).
struct UserDefaultsEventChecklistStore: EventChecklistStoring {
    let defaults: UserDefaults

    init(defaults: UserDefaults = .standard) {
        self.defaults = defaults
    }

    static func key(eventId: String) -> String { "wakeve.eventChecklist.\(eventId)" }

    func items(eventId: String) -> [EventChecklistItem] {
        guard let data = defaults.data(forKey: Self.key(eventId: eventId)),
              let items = try? JSONDecoder().decode([EventChecklistItem].self, from: data) else { return [] }
        return items
    }

    func save(_ items: [EventChecklistItem], eventId: String) {
        guard !items.isEmpty else {
            defaults.removeObject(forKey: Self.key(eventId: eventId))
            return
        }
        guard let data = try? JSONEncoder().encode(items) else { return }
        defaults.set(data, forKey: Self.key(eventId: eventId))
    }
}

/// Checklist publiée pour le hub ; lecture et écriture locales (rapides, sur le fil principal).
@MainActor
final class EventChecklistModel: ObservableObject {
    @Published private(set) var items: [EventChecklistItem] = []

    private let eventId: String
    private let store: EventChecklistStoring

    init(eventId: String, store: EventChecklistStoring = UserDefaultsEventChecklistStore()) {
        self.eventId = eventId
        self.store = store
    }

    func reload() {
        items = store.items(eventId: eventId)
    }

    func toggle(_ id: String) {
        store.toggle(eventId: eventId, itemId: id)
        reload()
    }

    func add(_ titles: [String]) {
        store.add(eventId: eventId, titles: titles)
        reload()
    }

    func contains(_ title: String) -> Bool {
        EventChecklist.contains(title, in: items)
    }
}
