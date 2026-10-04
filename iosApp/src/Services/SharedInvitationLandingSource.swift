import Foundation
import Shared

/// Source réelle de l'invitation reçue (couche 8, #47) : faits du hub (`SharedEventHubSource`), puis
/// nom de l'organisateur et réponse du spectateur, lus hors du fil principal sur le schéma existant.
struct SharedInvitationLandingSource: InvitationLandingSource {
    private let hubSource: SharedEventHubSource
    private let repository: DatabaseEventRepository
    private let database: WakeveDb

    init(
        repository: DatabaseEventRepository = RepositoryProvider.shared.databaseRepository,
        database: WakeveDb = RepositoryProvider.shared.database
    ) {
        hubSource = SharedEventHubSource(repository: repository, database: database)
        self.repository = repository
        self.database = database
    }

    func loadLanding(eventId: String, viewerId: String, isLocalGuest: Bool) async throws -> InvitationLandingFacts {
        let hub = try await hubSource.loadFacts(eventId: eventId, viewerId: viewerId, isLocalGuest: isLocalGuest)
        // Mêmes garanties de fil que `SharedEventHubSource.loadFacts` (lectures synchrones, pilote sûr entre fils).
        let reader = self
        let work = Task.detached(priority: .userInitiated) {
            try reader.landing(hub: hub, viewerId: viewerId)
        }
        return try await withTaskCancellationHandler {
            try await work.value
        } onCancel: {
            work.cancel()
        }
    }

    /// Nom de l'organisateur, nil s'il est inconnu ou si le spectateur est l'organisateur lui-même.
    static func organizerName(organizerId: String?, viewerId: String, lookup: (String) -> String?) -> String? {
        guard let organizerId, organizerId != viewerId else { return nil }
        return lookup(organizerId)
    }

    private func landing(hub: EventHubFacts, viewerId: String) throws -> InvitationLandingFacts {
        try Task.checkCancellation()
        let viewer = (repository.getParticipantRecords(eventId: hub.id) ?? [])
            .map { ParticipantAccessMapper.shared.fromRepositoryRecord(record: $0) }
            .first { $0.userId == viewerId }
        let organizerName = Self.organizerName(organizerId: hub.organizerId, viewerId: viewerId) { id in
            database.userQueries.selectUserById(id: id).executeAsOneOrNull()?.name
        }
        return InvitationLandingFacts(
            hub: hub,
            organizerName: organizerName,
            response: InvitationLandingFacts.response(
                isOrganizer: hub.isOrganizer,
                accepted: hub.viewerAccepted,
                declined: viewer?.rsvp == .declined
            )
        )
    }
}
