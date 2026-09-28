import Foundation
import Shared

/// Source réelle de l'accueil (couche 3, #47) : lit les projections de la bibliothèque et le dépôt
/// d'événements du module Kotlin `Shared`. Tout l'accès Kotlin de l'accueil reste ici.
/// Non testée unitairement (dépend de la base) ; vérifiée sur simulateur.
struct SharedEventsHomeSource: EventsHomeSource {
    struct LoadFailed: Error {}

    private let projectionRepository: DatabaseInvitationExperienceProjectionRepository
    private let repository: DatabaseEventRepository
    private let database: WakeveDb

    init(
        projectionRepository: DatabaseInvitationExperienceProjectionRepository =
            DatabaseInvitationExperienceProjectionRepository(database: RepositoryProvider.shared.database),
        repository: DatabaseEventRepository = RepositoryProvider.shared.databaseRepository,
        database: WakeveDb = RepositoryProvider.shared.database
    ) {
        self.projectionRepository = projectionRepository
        self.repository = repository
        self.database = database
    }

    func loadEvents(viewerId: String) async throws -> [HomeRawEvent] {
        try await load(viewerId: viewerId)
    }

    /// Les fonctions suspendues Kotlin sont appelées depuis le fil principal.
    @MainActor
    private func load(viewerId: String) async throws -> [HomeRawEvent] {
        let now = Kotlinx_datetimeInstant.companion.fromEpochMilliseconds(
            epochMilliseconds: Int64(Date().timeIntervalSince1970 * 1_000)
        )
        var seen = Set<String>()
        var cards: [LibraryCardProjection] = []
        var failures = 0
        let projections: [LibraryProjection] = [.upcoming, .drafts, .past]
        for projection in projections {
            let state = try await projectionRepository.library(viewerId: viewerId, projection: projection, now: now)
            if let ready = state as? LibraryLoadStateReady<NSArray>,
               let projected = ready.snapshot as? [LibraryCardProjection] {
                for card in projected where seen.insert(card.event.id).inserted {
                    cards.append(card)
                }
            } else if state is LibraryLoadStateFailed<NSArray> {
                failures += 1
            }
        }
        if failures == projections.count { throw LoadFailed() }
        return cards.map { raw(from: $0, viewerId: viewerId) }
    }

    @MainActor
    private func raw(from card: LibraryCardProjection, viewerId: String) -> HomeRawEvent {
        let event = card.event
        let statusName = event.status.name
        let keepsActive = event.status == .polling || event.status == .draft
        let slotIds = Set(event.proposedSlots.map(\.id))
        let votes = repository.getPoll(eventId: event.id)?.votes ?? [:]
        let completeVoters = slotIds.isEmpty ? 0 : votes.values.filter { ballot in
            slotIds.isSubset(of: Set(ballot.keys))
        }.count
        return HomeRawEvent(
            id: event.id,
            title: event.title,
            statusName: statusName,
            isOrganizer: card.memberships.contains(.hosting) || event.organizerId == viewerId,
            isPast: card.temporalClass == .past && !keepsActive,
            deadlineISO: event.deadline,
            finalDateISO: event.finalDate,
            firstSlotStartISO: event.proposedSlots.first?.start,
            userBallotComplete: repository.hasCompleteBallot(eventId: event.id, participantId: viewerId),
            votersWithCompleteBallot: completeVoters,
            eligibleVoters: max(1, event.participants.count),
            participantNames: event.participants.prefix(5).map(displayName),
            hasPendingSync: !(card.syncState is LibrarySyncStateSynced)
        )
    }

    @MainActor
    private func displayName(_ userId: String) -> String {
        let name = database.userQueries
            .selectUserById(id: userId)
            .executeAsOneOrNull()?
            .name
            .trimmingCharacters(in: .whitespacesAndNewlines)
        guard let name, !name.isEmpty else { return userId }
        return name
    }
}
