import Foundation

/// Suggestions IA du hub (revue couche 9, #47) : résumé, idées de sondage, checklist, messages d'invitation,
/// comme le panneau IA de l'ancien détail. Règles pures ici ; génération dans `EventHubAIModel`.
enum EventHubAI {
    enum Section: Equatable { case summary, polls, checklist, invitation }

    /// Règle de l'ancien panneau : organisateur ou participant éligible (accès aux détails), plus rien une fois
    /// l'événement finalisé ; idées de sondage pour l'organisateur seul, en brouillon ou pendant le sondage.
    static func sections(for facts: EventHubFacts) -> [Section] {
        guard facts.hasDetailsAccess, facts.phase != .finalized else { return [] }
        var sections: [Section] = [.summary]
        if facts.isOrganizer, [.draft, .polling].contains(facts.phase) {
            sections.append(.polls)
        }
        sections.append(contentsOf: [.checklist, .invitation])
        return sections
    }

    /// Les idées de sondage sont des questions libres sans dates : aucune API ne les transforme en créneaux.
    /// En brouillon, l'organisateur reprend ses créneaux dans la création (« Ajouter des dates », route du hub).
    static func offersAddDates(for facts: EventHubFacts) -> Bool {
        facts.isOrganizer && facts.phase == .draft
    }

    /// Contexte limité à ce que le hub montre déjà au spectateur (titre, date ou créneaux, invités).
    static func context(for facts: EventHubFacts, locale: Locale = WK.appLocale) -> WakeveAIEventContext {
        WakeveAIEventContext(
            eventId: facts.id,
            title: facts.title,
            date: dateText(for: facts, locale: locale),
            location: nil,
            participantNames: facts.participantNames,
            voteSummaries: [],
            taskTitles: [],
            recentMessages: []
        )
    }

    private static func dateText(for facts: EventHubFacts, locale: Locale) -> String? {
        if let finalDate = facts.finalDate, facts.phase != .draft, facts.phase != .polling {
            return HomeDateText.short(finalDate, locale: locale)
        }
        return facts.slotCount > 0 ? HubSummaryText.slots(facts.slotCount, locale: locale) : nil
    }

    static func isInChecklist(_ item: ChecklistItem, existing: [EventChecklistItem]) -> Bool {
        EventChecklist.contains(item.title, in: existing)
    }

    enum InvitationVariant: CaseIterable, Identifiable {
        case simple, warm, short

        var id: Self { self }

        func text(in set: InvitationMessageSet) -> String {
            switch self {
            case .simple: return set.simple
            case .warm: return set.warm
            case .short: return set.shortWhatsApp
            }
        }

        func title(locale: Locale = WK.appLocale) -> String {
            switch self {
            case .simple: return WK.localizedFormat("hub.ai.invitation.simple", locale: locale)
            case .warm: return WK.localizedFormat("hub.ai.invitation.warm", locale: locale)
            case .short: return WK.localizedFormat("hub.ai.invitation.short", locale: locale)
            }
        }
    }
}

/// Suggestions générées ; une partie peut manquer (génération ou validation en échec).
struct EventHubAIResult: Equatable {
    var summary: EventSummary?
    var polls: [PollSuggestion] = []
    var checklist: [ChecklistItem] = []
    var invitation: InvitationMessageSet?

    var isEmpty: Bool { summary == nil && polls.isEmpty && checklist.isEmpty && invitation == nil }
}

/// Fournisseur de contexte des générateurs, figé sur les faits du hub.
struct EventHubAIContextProvider: WakeveAIContextProviding {
    let context: WakeveAIEventContext

    func currentGroup() async -> WakeveAIGroupContext? {
        WakeveAIGroupContext(groupId: context.eventId, memberDisplayNames: context.participantNames)
    }

    func eventContext(eventId: String) async -> WakeveAIEventContext? { context }
    func participantStatuses(eventId: String) async -> WakeveAIParticipantStatuses? { nil }
    func voteResults(eventId: String) async -> WakeveAIVoteResults? { nil }
    func transportContext(eventId: String) async -> WakeveAITransportContext? { nil }
    func userPreferences() async -> WakeveAIUserPreferences? { nil }
}

/// Génère les suggestions **à la demande** (« Générer », jamais à l'ouverture du hub), hors du fil principal :
/// Foundation Models quand il est disponible, sinon le client heuristique local (`WakeveAIClientFactory`).
@MainActor
final class EventHubAIModel: ObservableObject {
    enum State: Equatable { case idle, loading, loaded(EventHubAIResult), failed }

    @Published private(set) var state: State = .idle

    private let makeClient: @Sendable () -> WakeveAIClientProtocol
    private var generation = 0

    init(makeClient: @escaping @Sendable () -> WakeveAIClientProtocol = {
        WakeveAIClientFactory.makeDefault(availability: WakeveAIAvailabilityService().currentAvailability())
    }) {
        self.makeClient = makeClient
    }

    var isLoading: Bool { state == .loading }

    func generate(facts: EventHubFacts) async {
        let sections = EventHubAI.sections(for: facts)
        guard !sections.isEmpty, state != .loading else { return }
        generation += 1
        let token = generation
        state = .loading
        let context = EventHubAI.context(for: facts)
        let localeIdentifier = WK.appLocale.identifier
        let makeClient = self.makeClient
        let work = Task.detached(priority: .userInitiated) {
            await Self.run(sections: sections, context: context, localeIdentifier: localeIdentifier, client: makeClient())
        }
        let result = await work.value
        guard token == generation else { return }
        state = result.isEmpty ? .failed : .loaded(result)
    }

    /// Chaque partie est indépendante : un échec n'efface pas les autres.
    nonisolated private static func run(
        sections: [EventHubAI.Section],
        context: WakeveAIEventContext,
        localeIdentifier: String,
        client: WakeveAIClientProtocol
    ) async -> EventHubAIResult {
        let prompt = context.promptSummary
        let knownFacts = context.knownFacts
        async let summary = attempt(sections.contains(.summary)) {
            try await EventSummaryGenerator(client: client, contextProvider: EventHubAIContextProvider(context: context))
                .generate(eventId: context.eventId, localeIdentifier: localeIdentifier)
        }
        async let polls = attempt(sections.contains(.polls)) {
            try await PollSuggestionGenerator(client: client)
                .generate(context: prompt, knownFacts: knownFacts, localeIdentifier: localeIdentifier)
        }
        async let checklist = attempt(sections.contains(.checklist)) {
            try await ChecklistGenerator(client: client)
                .generate(context: prompt, knownFacts: knownFacts, localeIdentifier: localeIdentifier)
        }
        async let invitation = attempt(sections.contains(.invitation)) {
            try await InvitationMessageGenerator(client: client)
                .generate(context: prompt, knownFacts: knownFacts, localeIdentifier: localeIdentifier)
        }
        return await EventHubAIResult(
            summary: summary,
            polls: polls ?? [],
            checklist: checklist ?? [],
            invitation: invitation
        )
    }

    /// nil si la partie n'est pas demandée ou si sa génération échoue.
    nonisolated private static func attempt<T: Sendable>(
        _ enabled: Bool,
        _ operation: @Sendable () async throws -> T
    ) async -> T? {
        guard enabled else { return nil }
        return try? await operation()
    }
}
