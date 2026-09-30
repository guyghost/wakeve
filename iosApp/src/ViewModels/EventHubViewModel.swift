import Foundation

/// Source injectable des faits du hub (couche 4, #47).
protocol EventHubSource {
    func loadFacts(eventId: String, viewerId: String, isLocalGuest: Bool) async throws -> EventHubFacts
}

@MainActor
final class EventHubViewModel: ObservableObject {
    enum State: Equatable { case loading, loaded, failed }

    @Published private(set) var state: State = .loading
    @Published private(set) var facts: EventHubFacts?
    @Published private(set) var model: EventHubModel?

    private let eventId: String
    private let viewerId: String
    private let isLocalGuest: Bool
    private let source: EventHubSource
    /// Seul le dernier chargement lancé publie son résultat.
    private var generation = 0

    init(eventId: String, viewerId: String, isLocalGuest: Bool, source: EventHubSource) {
        self.eventId = eventId
        self.viewerId = viewerId
        self.isLocalGuest = isLocalGuest
        self.source = source
    }

    func reload() async {
        generation += 1
        let token = generation
        do {
            let loaded = try await source.loadFacts(eventId: eventId, viewerId: viewerId, isLocalGuest: isLocalGuest)
            guard token == generation else { return }
            facts = loaded
            model = EventHubModel(facts: loaded)
            state = .loaded
        } catch {
            // Chargement annulé (vue quittée) : on garde l'état courant.
            guard token == generation, !(error is CancellationError) else { return }
            // Données déjà affichées : on les garde plutôt que d'afficher un échec.
            state = facts == nil ? .failed : .loaded
        }
    }
}
