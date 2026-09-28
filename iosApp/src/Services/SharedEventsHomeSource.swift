import Foundation
import Shared

/// Source réelle de l'accueil (couche 3, #47) : lit les projections de la bibliothèque et le dépôt
/// d'événements du module Kotlin `Shared`. Tout l'accès Kotlin de l'accueil reste ici.
/// Seuls les appels suspendus `library(...)` restent sur le fil principal ; les lectures SQLite
/// synchrones (sondage, participants, noms) tournent hors du fil principal (modèle mémoire
/// Kotlin/Native moderne : objets partageables entre fils, pilote SQLDelight natif à pool).
/// Les règles pures sont testées unitairement ; l'accès base est vérifié sur simulateur.
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
        let cards = try await loadCards(viewerId: viewerId)
        let reader = self
        return await Task.detached(priority: .userInitiated) {
            reader.rawEvents(from: cards, viewerId: viewerId)
        }.value
    }

    /// Les fonctions suspendues Kotlin sont appelées depuis le fil principal.
    @MainActor
    private func loadCards(viewerId: String) async throws -> [LibraryCardProjection] {
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
        return cards
    }

    /// Lectures synchrones : une requête de sondage et une de participants par sondage,
    /// noms mis en cache pour tout le chargement.
    private func rawEvents(from cards: [LibraryCardProjection], viewerId: String) -> [HomeRawEvent] {
        var names: [String: String] = [:]
        return cards.map { raw(from: $0, viewerId: viewerId, names: &names) }
    }

    private func raw(
        from card: LibraryCardProjection,
        viewerId: String,
        names: inout [String: String]
    ) -> HomeRawEvent {
        let event = card.event
        let statusName = event.status.name
        let isOwner = event.organizerId == viewerId
        let isOrganizer = card.memberships.contains(.hosting) || isOwner
        let isTemporallyPast = card.temporalClass == .past
        let isActive = Self.keepsActive(
            statusName: statusName,
            isTemporallyPast: isTemporallyPast,
            hasStructuredEndBound: EventTemporalClassifier.shared.structuredEndBound(event: event) != nil
        )
        let ballots: HomeBallotStats
        if event.status == .polling {
            // Sondage illisible : bulletins inconnus (ni « à voter » ni « prêt »).
            let votes = repository.getPoll(eventId: event.id)?.votes
            let accepted = (repository.getParticipantRecords(eventId: event.id) ?? [])
                .map { ParticipantAccessMapper.shared.fromRepositoryRecord(record: $0) }
                .filter { $0.role == .member && $0.rsvp == .accepted }
                .map(\.userId)
            ballots = Self.ballotStats(
                slotIds: Set(event.proposedSlots.map(\.id)),
                ballots: votes?.mapValues { Set($0.keys) },
                organizerId: event.organizerId,
                acceptedParticipantIds: Set(accepted),
                viewerId: viewerId
            )
        } else {
            ballots = .none
        }
        return HomeRawEvent(
            id: event.id,
            title: event.title,
            statusName: statusName,
            isOrganizer: isOrganizer,
            isOwner: isOwner,
            isPast: !isActive,
            readOnly: Self.isReadOnly(statusName: statusName, keepsActive: isActive, temporallyPast: isTemporallyPast),
            // `ATTENDING` = membre actif avec RSVP accepté (projection de la bibliothèque).
            viewerAccepted: isOrganizer || card.memberships.contains(.attending),
            deadlineISO: event.deadline,
            finalDateISO: event.finalDate,
            earliestSlotStartISO: Self.earliestStartISO(event.proposedSlots.compactMap(\.start)),
            ballots: ballots,
            participantNames: event.participants.prefix(5).map { userId in
                if let cached = names[userId] { return cached }
                let name = displayName(userId)
                names[userId] = name
                return name
            },
            hasPendingSync: !(card.syncState is LibrarySyncStateSynced)
        )
    }

    // MARK: - Règles pures (testées unitairement)

    /// Un événement classé « passé » reste actif seulement s'il est brouillon, ou en sondage
    /// sans borne de fin structurée (aucun créneau exploitable). Un sondage dont tous les
    /// créneaux sont terminés rejoint « Passés ».
    static func keepsActive(statusName: String, isTemporallyPast: Bool, hasStructuredEndBound: Bool) -> Bool {
        guard isTemporallyPast else { return true }
        switch statusName {
        case "DRAFT": return true
        case "POLLING": return !hasStructuredEndBound
        default: return false
        }
    }

    /// Lecture seule : finalisé, ou passé sans être gardé actif. Contrairement à la politique
    /// d'interaction de la projection, un sondage sans créneau daté (classé « passé ») reste actionnable.
    static func isReadOnly(statusName: String, keepsActive: Bool, temporallyPast: Bool) -> Bool {
        statusName == "FINALIZED" || (temporallyPast && !keepsActive)
    }

    /// Bulletins complets des votants éligibles (participants acceptés + organisateur).
    /// `ballots` : identifiants de créneaux votés, par identifiant d'utilisateur ; `nil` si le
    /// sondage n'a pas pu être lu (bulletins inconnus).
    static func ballotStats(
        slotIds: Set<String>,
        ballots: [String: Set<String>]?,
        organizerId: String,
        acceptedParticipantIds: Set<String>,
        viewerId: String
    ) -> HomeBallotStats {
        guard let ballots else { return .unknown }
        func isComplete(_ userId: String) -> Bool {
            !slotIds.isEmpty && slotIds.isSubset(of: ballots[userId] ?? [])
        }
        let eligible = acceptedParticipantIds.union([organizerId])
        let others = eligible.subtracting([organizerId])
        return HomeBallotStats(
            userBallotComplete: isComplete(viewerId),
            votersWithCompleteBallot: eligible.filter(isComplete).count,
            eligibleVoters: eligible.count,
            otherVotersComplete: others.filter(isComplete).count,
            otherEligibleVoters: others.count
        )
    }

    /// Début de créneau le plus tôt (les chaînes illisibles sont ignorées).
    static func earliestStartISO(_ starts: [String]) -> String? {
        starts
            .compactMap { value in HomeDateText.parseISO(value).map { (value, $0) } }
            .min { $0.1 < $1.1 }?
            .0
    }

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
