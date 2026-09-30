import Foundation
import Shared

/// Accès aux détails d'organisation : organisateur, ou participant ayant accepté avec la date
/// retenue validée (`ParticipantManagementPresentationMapper`). Règle unique partagée par
/// `AuthenticatedView.isParticipantConfirmed(for:)` et le hub (couche 4, #47).
enum OrganizationDetailsAccess {
    static func isGranted(organizerId: String, viewerId: String, records: [ParticipantRepositoryRecord]?) -> Bool {
        if organizerId == viewerId {
            return true
        }

        guard let records, !records.isEmpty else {
            return false
        }

        let participantAccessStates = records.map { record in
            ParticipantAccessMapper.shared.fromRepositoryRecord(record: record)
        }
        let rows = ParticipantManagementPresentationMapper.shared.map(participants: participantAccessStates)
        return rows.first { $0.userIdOrEmail == viewerId }?.canAccessOrganizationDetails ?? false
    }
}

/// Résumés d'une ligne des tuiles du hub, localisés pour `locale` (réutilise les formats existants).
enum HubSummaryText {
    /// Pluriel stringsdict (`hub.*_count`).
    static func plural(_ key: String, _ count: Int, locale: Locale) -> String {
        String(format: WK.localizedFormat(key, locale: locale), locale: locale, count)
    }

    static func slots(_ count: Int, locale: Locale) -> String {
        plural("hub.slots_count", count, locale: locale)
    }

    /// « %d option(s) » (formats existants du détail d'événement).
    static func options(_ count: Int, locale: Locale) -> String {
        let key = count == 1 ? "event.detail.slot_option_singular_format" : "event.detail.slot_options_plural_format"
        return String(format: WK.localizedFormat(key, locale: locale), locale: locale, count)
    }

    static func scenarios(_ count: Int, locale: Locale) -> String {
        plural("hub.scenarios_count", count, locale: locale)
    }

    /// « 1 confirmé », ou « 3 confirmés · 2 en attente ».
    static func participants(confirmed: Int, pending: Int, locale: Locale) -> String {
        let confirmedText = plural("hub.confirmed_count", confirmed, locale: locale)
        guard pending > 0 else { return confirmedText }
        let pendingText = plural("hub.pending_count", pending, locale: locale)
        return String(format: WK.localizedFormat("hub.summary.format", locale: locale), confirmedText, pendingText)
    }

    static func meals(completed: Int, total: Int, locale: Locale) -> String {
        String(format: WK.localizedFormat("hub.summary.meals_progress_format", locale: locale), locale: locale, completed, total)
    }

    /// Montant en euros (le budget est tenu en euros, comme `BudgetOverviewView`).
    static func euros(_ amount: Double, locale: Locale) -> String {
        currency(amount, code: "EUR", locale: locale)
    }

    /// Même formatage que `formatCurrencyAmount` (ContentView), avec la langue de l'app.
    static func currency(_ amount: Double, code: String, locale: Locale) -> String {
        let formatter = NumberFormatter()
        formatter.locale = locale
        formatter.numberStyle = .currency
        formatter.currencyCode = code
        formatter.maximumFractionDigits = amount.rounded() == amount ? 0 : 2
        return formatter.string(from: NSNumber(value: amount)) ?? "\(code) \(amount)"
    }

    /// Même logique que `paymentPotSummaryValue` (ContentView), sans le suffixe « détails finalisés »
    /// (le statut du hub le dit déjà).
    static func paymentPot(goalAmount: Double?, currency code: String?, locale: Locale) -> String {
        guard let goalAmount else {
            return WK.localizedFormat("event.detail.payment_pot.define_before_share", locale: locale)
        }
        guard goalAmount > 0 else {
            return WK.localizedFormat("event.detail.payment_pot.define_goal", locale: locale)
        }
        let amount = currency(goalAmount, code: code ?? "EUR", locale: locale)
        return String(format: WK.localizedFormat("event.detail.payment_pot.goal_format", locale: locale), amount)
    }
}

/// Source réelle du hub (couche 4, #47) : lit une fois l'événement et les données de ses modules
/// via les dépôts Kotlin `Shared`, hors du fil principal. Tout l'accès Kotlin du hub reste ici.
/// Les règles pures sont testées unitairement ; l'accès base est vérifié sur simulateur.
struct SharedEventHubSource: EventHubSource {
    struct EventNotFound: Error {}

    private let repository: DatabaseEventRepository
    private let database: WakeveDb

    init(
        repository: DatabaseEventRepository = RepositoryProvider.shared.databaseRepository,
        database: WakeveDb = RepositoryProvider.shared.database
    ) {
        self.repository = repository
        self.database = database
    }

    func loadFacts(eventId: String, viewerId: String, isLocalGuest: Bool) async throws -> EventHubFacts {
        // Envoyer ces objets Kotlin non `Sendable` vers un autre fil est sûr : Kotlin 2.2 utilise le
        // modèle mémoire moderne (objets partageables entre fils, pas de gel), les lectures ci-dessous
        // sont synchrones (aucun saut de dispatcher), et le pilote SQLDelight natif est sûr entre
        // fils — l'app lit déjà cette base depuis `Dispatchers.Default`.
        let reader = self
        let now = Date()
        let locale = WK.appLocale
        let work = Task.detached(priority: .userInitiated) {
            try reader.facts(eventId: eventId, viewerId: viewerId, isLocalGuest: isLocalGuest, now: now, locale: locale)
        }
        return try await withTaskCancellationHandler {
            try await work.value
        } onCancel: {
            work.cancel()
        }
    }

    // MARK: - Lecture (hors fil principal)

    private func facts(
        eventId: String,
        viewerId: String,
        isLocalGuest: Bool,
        now: Date,
        locale: Locale
    ) throws -> EventHubFacts {
        guard let event = repository.getEvent(id: eventId) else { throw EventNotFound() }
        try Task.checkCancellation()

        let phase = Self.phase(statusName: event.status.name)
        let isOrganizer = event.organizerId == viewerId
        let records = repository.getParticipantRecords(eventId: event.id)
        let states = (records ?? []).map { ParticipantAccessMapper.shared.fromRepositoryRecord(record: $0) }
        let rows = ParticipantManagementPresentationMapper.shared.map(participants: states)
        let viewerState = states.first { $0.userId == viewerId }
        let viewerAccepted = isOrganizer || (viewerState?.role == .member && viewerState?.rsvp == .accepted)
        let hasDetailsAccess = OrganizationDetailsAccess.isGranted(
            organizerId: event.organizerId, viewerId: viewerId, records: records
        )

        // Bulletins et créneau en tête : uniquement pendant le sondage (même règle que l'accueil).
        var ballots = HomeBallotStats.none
        var leadingSlotStart: Date?
        if event.status == .polling {
            let poll = repository.getPoll(eventId: event.id)
            let accepted = states
                .filter { $0.role == .member && $0.rsvp == .accepted }
                .map(\.userId)
            ballots = SharedEventsHomeSource.ballotStats(
                slotIds: Set(event.proposedSlots.map(\.id)),
                ballots: poll?.votes.mapValues { Set($0.keys) },
                organizerId: event.organizerId,
                acceptedParticipantIds: Set(accepted),
                viewerId: viewerId
            )
            if let poll {
                leadingSlotStart = Self.leadingStart(
                    hasVotes: !poll.votes.isEmpty,
                    bestSlotStartISO: PollLogic.shared.getBestSlotWithScore(poll: poll, slots: event.proposedSlots)?.first?.start
                )
            }
        }
        try Task.checkCancellation()

        // Invités : même règle que la toile d'invitation (refusés exclus, confirmés = accès aux détails).
        let guests = Self.guestCounts(zip(states, rows).map { state, row in
            (declined: state.rsvp == .declined, confirmed: row.canAccessOrganizationDetails)
        })

        let participantNames = event.participants.prefix(5).map(displayName)

        let pollOpen = HomeDateText.parseISO(event.deadline).map { $0 > now } ?? true
        let finalDate = HomeDateText.parseISO(event.finalDate)

        func make(summaries: [HubModule: String]) -> EventHubFacts {
            EventHubFacts(
                id: event.id,
                title: event.title,
                phase: phase,
                isOrganizer: isOrganizer,
                viewerAccepted: viewerAccepted,
                hasDetailsAccess: hasDetailsAccess,
                isLocalGuest: isLocalGuest,
                pollOpen: pollOpen,
                userBallotComplete: ballots.userBallotComplete,
                ballotsKnown: ballots.ballotsKnown,
                votersWithCompleteBallot: ballots.votersWithCompleteBallot,
                otherEligibleVoters: ballots.otherEligibleVoters,
                otherVotersComplete: ballots.otherVotersComplete,
                slotCount: event.proposedSlots.count,
                leadingSlotStart: leadingSlotStart,
                finalDate: finalDate,
                confirmedCount: guests.confirmed,
                pendingCount: guests.pending,
                participantNames: participantNames,
                summaries: summaries,
                eventTypeName: event.eventType.name,
                organizerId: event.organizerId
            )
        }

        // Résumés : seulement pour les tuiles visibles et ouvertes (`EventHubModel.summarizedModules`).
        let base = make(summaries: [:])
        var summaries: [HubModule: String] = [:]
        for module in EventHubModel.summarizedModules(for: base) {
            try Task.checkCancellation()
            summaries[module] = summary(for: module, event: event, facts: base, locale: locale)
        }
        return make(summaries: summaries)
    }

    /// Résumé d'une ligne ; nil (indice statique affiché par la vue) si le module est vide ou illisible.
    private func summary(for module: HubModule, event: Event, facts: EventHubFacts, locale: Locale) -> String? {
        switch module {
        case .date:
            if let finalDate = facts.finalDate, facts.phase != .draft, facts.phase != .polling {
                return HomeDateText.short(finalDate, locale: locale)
            }
            return facts.slotCount > 0 ? HubSummaryText.slots(facts.slotCount, locale: locale) : nil
        case .location:
            switch Self.locationSummary(for: facts) {
            case .potentialLocations:
                let count = database.potentialLocationQueries.selectByEventId(eventId: event.id).executeAsList().count
                return count > 0 ? HubSummaryText.options(count, locale: locale) : nil
            case .scenarios:
                return summary(for: .scenarios, event: event, facts: facts, locale: locale)
            case .none:
                return nil
            }
        case .participants:
            if facts.confirmedCount + facts.pendingCount > 0 {
                return HubSummaryText.participants(confirmed: facts.confirmedCount, pending: facts.pendingCount, locale: locale)
            }
            return event.participants.isEmpty ? nil : HubSummaryText.plural("hub.guests_count", event.participants.count, locale: locale)
        case .budget:
            // Lecture seule du dépôt : le chargement du modèle de vue du budget créerait un budget absent.
            guard let budget = BudgetRepository(db: database).getBudgetByEventId(eventId: event.id),
                  budget.totalEstimated > 0 else { return nil }
            return HubSummaryText.euros(budget.totalEstimated, locale: locale)
        case .scenarios:
            let count = ScenarioRepository(db: database).getScenariosByEventId(eventId: event.id).count
            return count > 0 ? HubSummaryText.scenarios(count, locale: locale) : nil
        case .transport:
            let transport = TransportRepositoryBridge(database: database)
            if transport.getSelectedPlanId(eventId: event.id) != nil {
                return WK.localizedFormat("transport.plan.selected", locale: locale)
            }
            let count = transport.getPlansByEvent(eventId: event.id).count
            return count > 0 ? HubSummaryText.options(count, locale: locale) : nil
        case .accommodation:
            let count = AccommodationRepository(db: database).getAccommodationsByEventId(eventId: event.id).count
            return count > 0 ? HubSummaryText.options(count, locale: locale) : nil
        case .meals:
            let meals = MealRepository(db: database).getMealPlanningSummary(eventId: event.id)
            guard meals.totalMeals > 0 else { return nil }
            return HubSummaryText.meals(completed: Int(meals.mealsCompleted), total: Int(meals.totalMeals), locale: locale)
        case .equipment:
            let count = EquipmentRepository(db: database).getEquipmentItemsByEventId(eventId: event.id).count
            return count > 0 ? HubSummaryText.plural("hub.items_count", count, locale: locale) : nil
        case .activities:
            let count = ActivityRepository(db: database).getActivitiesByEventId(eventId: event.id).count
            return count > 0 ? HubSummaryText.plural("hub.activities_count", count, locale: locale) : nil
        case .meetings:
            let count = database.meetingQueries
                .selectByEventId(eventId: event.id)
                .executeAsList()
                .filter { $0.status != "CANCELLED" }
                .count
            return count > 0 ? HubSummaryText.plural("hub.meetings_count", count, locale: locale) : nil
        case .payments:
            let pot = PaymentPotRepository(db: database).getActivePotForEvent(eventId: event.id)
            return HubSummaryText.paymentPot(goalAmount: pot?.goalAmount, currency: pot?.currency, locale: locale)
        case .recap, .photos:
            return nil
        }
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

    // MARK: - Règles pures (testées unitairement)

    /// `EventStatus.name` Kotlin → phase ; statut inconnu traité comme finalisé (comme l'accueil).
    static func phase(statusName: String) -> EventHubFacts.Phase {
        switch statusName {
        case "DRAFT": return .draft
        case "POLLING": return .polling
        case "COMPARING": return .comparing
        case "CONFIRMED": return .confirmed
        case "ORGANIZING": return .organizing
        default: return .finalized
        }
    }

    /// Refusés exclus ; confirmés = accès aux détails, les autres sont en attente.
    static func guestCounts(_ guests: [(declined: Bool, confirmed: Bool)]) -> (confirmed: Int, pending: Int) {
        let active = guests.filter { !$0.declined }
        let confirmed = active.filter(\.confirmed).count
        return (confirmed, active.count - confirmed)
    }

    enum LocationSummary: Equatable { case potentialLocations, scenarios, none }

    /// La tuile Lieu ouvre la liste des scénarios : son résumé décrit ce que cet écran montre.
    /// Avant la date, les lieux potentiels (visibles de l'organisateur seul) ; ensuite, les scénarios.
    static func locationSummary(for facts: EventHubFacts) -> LocationSummary {
        switch facts.phase {
        case .draft, .polling: return facts.isOrganizer ? .potentialLocations : .none
        case .confirmed, .comparing, .organizing, .finalized: return .scenarios
        }
    }

    /// Créneau en tête seulement s'il existe au moins un vote (sinon PollLogic renvoie un créneau arbitraire).
    static func leadingStart(hasVotes: Bool, bestSlotStartISO: String?) -> Date? {
        guard hasVotes else { return nil }
        return HomeDateText.parseISO(bestSlotStartISO)
    }
}
