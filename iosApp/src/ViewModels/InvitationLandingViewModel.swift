import Foundation

/// Source injectable de l'invitation reçue (couche 8, #47).
protocol InvitationLandingSource {
    func loadLanding(eventId: String, viewerId: String, isLocalGuest: Bool) async throws -> InvitationLandingFacts
}

@MainActor
final class InvitationLandingViewModel: ObservableObject {
    enum State: Equatable { case loading, loaded, failed }

    @Published private(set) var state: State = .loading
    @Published private(set) var model: InvitationLandingModel?

    private let eventId: String
    private let viewerId: String
    private let isLocalGuest: Bool
    private let source: InvitationLandingSource
    private var generation = 0

    init(eventId: String, viewerId: String, isLocalGuest: Bool, source: InvitationLandingSource) {
        self.eventId = eventId
        self.viewerId = viewerId
        self.isLocalGuest = isLocalGuest
        self.source = source
    }

    func reload() async {
        generation += 1
        let token = generation
        do {
            let facts = try await source.loadLanding(eventId: eventId, viewerId: viewerId, isLocalGuest: isLocalGuest)
            guard token == generation else { return }
            model = InvitationLandingModel(facts: facts)
            state = .loaded
        } catch {
            guard token == generation, !(error is CancellationError) else { return }
            state = model == nil ? .failed : .loaded
        }
    }
}
