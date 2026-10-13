import XCTest
@testable import Wakeve

/// Suggestions IA dans le hub (revue couche 9, #47) : ce que montrait le panneau IA de l'ancien détail.
@MainActor
final class EventHubAITests: XCTestCase {
    private let fr = Locale(identifier: "fr")

    private func facts(
        phase: EventHubFacts.Phase = .polling,
        isOrganizer: Bool = false,
        hasDetailsAccess: Bool = true,
        slotCount: Int = 3,
        finalDate: Date? = nil
    ) -> EventHubFacts {
        EventHubFacts(
            id: "e1", title: "Anniversaire de Léa", phase: phase,
            isOrganizer: isOrganizer, viewerAccepted: true,
            hasDetailsAccess: hasDetailsAccess || isOrganizer, isLocalGuest: false,
            pollOpen: true, userBallotComplete: false, ballotsKnown: true,
            votersWithCompleteBallot: 0, otherEligibleVoters: 0, otherVotersComplete: 0,
            slotCount: slotCount, leadingSlotStart: nil, finalDate: finalDate,
            confirmedCount: 2, pendingCount: 1, participantNames: ["Léa", "Tom"], summaries: [:]
        )
    }

    // MARK: - Visibilité (règle de l'ancien panneau)

    /// Ancien détail : organisateur ou participant éligible (accès aux détails), jamais une fois finalisé ;
    /// suggestions de sondage pour l'organisateur seul, tant que le sondage se prépare ou tourne.
    func testSectionsFollowTheLegacyPanelGating() {
        XCTAssertEqual(EventHubAI.sections(for: facts(phase: .draft, isOrganizer: true)), [.summary, .polls, .checklist, .invitation])
        XCTAssertEqual(EventHubAI.sections(for: facts(phase: .polling, isOrganizer: true)), [.summary, .polls, .checklist, .invitation])
        XCTAssertEqual(EventHubAI.sections(for: facts(phase: .polling)), [.summary, .checklist, .invitation])
        for phase in [EventHubFacts.Phase.confirmed, .comparing, .organizing] {
            XCTAssertEqual(EventHubAI.sections(for: facts(phase: phase, isOrganizer: true)), [.summary, .checklist, .invitation], "\(phase)")
        }
        XCTAssertEqual(EventHubAI.sections(for: facts(phase: .finalized, isOrganizer: true)), [], "Finalisé : plus de suggestions.")
        XCTAssertEqual(EventHubAI.sections(for: facts(phase: .organizing, hasDetailsAccess: false)), [], "Sans accès aux détails.")
    }

    /// Les suggestions de sondage sont des questions libres sans dates : rien à appliquer directement ;
    /// en brouillon, l'organisateur peut reprendre ses créneaux dans la création.
    func testOnlyADraftOrganizerGetsTheAddDatesShortcut() {
        XCTAssertTrue(EventHubAI.offersAddDates(for: facts(phase: .draft, isOrganizer: true)))
        XCTAssertFalse(EventHubAI.offersAddDates(for: facts(phase: .polling, isOrganizer: true)))
        XCTAssertFalse(EventHubAI.offersAddDates(for: facts(phase: .draft)))
    }

    // MARK: - Contexte

    func testContextUsesOnlyFactsTheViewerSees() {
        let day = ISO8601DateFormatter().date(from: "2026-10-17T10:00:00Z")!
        let polling = EventHubAI.context(for: facts(slotCount: 3), locale: fr)
        XCTAssertEqual(polling.title, "Anniversaire de Léa")
        XCTAssertEqual(polling.date, "3 créneaux")
        XCTAssertEqual(polling.participantNames, ["Léa", "Tom"])
        let confirmed = EventHubAI.context(for: facts(phase: .confirmed, finalDate: day), locale: fr)
        XCTAssertEqual(confirmed.date, HomeDateText.short(day, locale: fr))
        XCTAssertEqual(confirmed.knownFacts.participantNames, ["Léa", "Tom"])
    }

    // MARK: - Checklist

    func testChecklistSuggestionsSkipWhatIsAlreadyInTheChecklist() {
        let existing = EventChecklist.seedingTemplate(["Confirmer le lieu"], into: [])
        let generated = [
            ChecklistItem(title: "confirmer le lieu", category: .venue, priority: .high),
            ChecklistItem(title: "Prévoir le budget", category: .budget, priority: .medium)
        ]
        XCTAssertEqual(EventHubAI.isInChecklist(generated[0], existing: existing), true)
        XCTAssertEqual(EventHubAI.isInChecklist(generated[1], existing: existing), false)
    }

    func testInvitationVariantsReadTheirMessage() {
        let set = InvitationMessageSet(simple: "A", warm: "B", shortWhatsApp: "C")
        XCTAssertEqual(EventHubAI.InvitationVariant.allCases.map { $0.text(in: set) }, ["A", "B", "C"])
        XCTAssertEqual(EventHubAI.InvitationVariant.warm.title(locale: fr), "Chaleureux")
    }

    // MARK: - Génération

    private struct FakeClient: WakeveAIClientProtocol {
        var fails: Set<String> = []
        struct Boom: Error {}
        func generateEventDraft(prompt: WakeveAIPrompt, request: WakeveAIGenerationRequest) async throws -> EventDraft { throw Boom() }
        func generatePollSuggestions(prompt: WakeveAIPrompt, knownFacts: WakeveAIKnownFacts) async throws -> [PollSuggestion] {
            if fails.contains("polls") { throw Boom() }
            return [PollSuggestion(question: "Quel budget ?", options: ["Petit", "Moyen"], pollType: .budget)]
        }
        func generateChecklist(prompt: WakeveAIPrompt, knownFacts: WakeveAIKnownFacts) async throws -> [ChecklistItem] {
            if fails.contains("checklist") { throw Boom() }
            return [ChecklistItem(title: "Prévoir le gâteau", category: .food, priority: .high)]
        }
        func generateInvitationMessages(prompt: WakeveAIPrompt, knownFacts: WakeveAIKnownFacts) async throws -> InvitationMessageSet {
            if fails.contains("invitation") { throw Boom() }
            return InvitationMessageSet(simple: "Viens !", warm: "On t'attend avec plaisir.", shortWhatsApp: "Partant ?")
        }
        func generateEventSummary(prompt: WakeveAIPrompt, knownFacts: WakeveAIKnownFacts) async throws -> EventSummary {
            if fails.contains("summary") { throw Boom() }
            return EventSummary(decided: ["Le titre"], missing: ["La date"], recommendedNextAction: "Lancer le vote.")
        }
        func generateTransportSuggestions(prompt: WakeveAIPrompt, knownFacts: WakeveAIKnownFacts) async throws -> TransportCoordinationSuggestion { throw Boom() }
    }

    func testGenerationFillsOnlyTheVisibleSections() async {
        let model = EventHubAIModel(makeClient: { FakeClient() })
        XCTAssertEqual(model.state, .idle, "Rien n'est généré à l'ouverture du hub.")
        await model.generate(facts: facts(phase: .polling))
        guard case .loaded(let result) = model.state else { return XCTFail("\(model.state)") }
        XCTAssertEqual(result.summary?.recommendedNextAction, "Lancer le vote.")
        XCTAssertEqual(result.polls, [], "Participant : pas de suggestions de sondage.")
        XCTAssertEqual(result.checklist.map(\.title), ["Prévoir le gâteau"])
        XCTAssertEqual(result.invitation?.warm, "On t'attend avec plaisir.")
    }

    func testOrganizerGetsPollSuggestions() async {
        let model = EventHubAIModel(makeClient: { FakeClient() })
        await model.generate(facts: facts(phase: .draft, isOrganizer: true))
        guard case .loaded(let result) = model.state else { return XCTFail("\(model.state)") }
        XCTAssertEqual(result.polls.map(\.question), ["Quel budget ?"])
    }

    func testPartialFailuresKeepTheOtherSuggestions() async {
        let model = EventHubAIModel(makeClient: { FakeClient(fails: ["summary", "invitation"]) }, makeFallback: nil)
        await model.generate(facts: facts(phase: .organizing, isOrganizer: true))
        guard case .loaded(let result) = model.state else { return XCTFail("\(model.state)") }
        XCTAssertNil(result.summary)
        XCTAssertNil(result.invitation)
        XCTAssertEqual(result.checklist.count, 1)
    }

    func testEverythingFailingShowsTheFailure() async {
        let model = EventHubAIModel(makeClient: { FakeClient(fails: ["summary", "polls", "checklist", "invitation"]) }, makeFallback: nil)
        await model.generate(facts: facts(phase: .draft, isOrganizer: true))
        XCTAssertEqual(model.state, .failed)
    }

    /// Foundation Models annoncé disponible mais en échec (constaté au simulateur) : repli heuristique par partie.
    func testFailingPartsFallBackToTheHeuristicClient() async {
        let model = EventHubAIModel(makeClient: { FakeClient(fails: ["summary", "invitation"]) })
        await model.generate(facts: facts(phase: .organizing, isOrganizer: true))
        guard case .loaded(let result) = model.state else { return XCTFail("\(model.state)") }
        XCTAssertEqual(result.checklist.map(\.title), ["Prévoir le gâteau"], "Partie réussie : gardée.")
        XCTAssertNotNil(result.summary, "Partie en échec : client heuristique.")
        XCTAssertNotNil(result.invitation)
    }

    func testHeuristicClientProducesSuggestions() async {
        let model = EventHubAIModel(makeClient: { HeuristicWakeveAIClient() })
        await model.generate(facts: facts(phase: .polling, isOrganizer: true))
        guard case .loaded(let result) = model.state else { return XCTFail("\(model.state)") }
        XCTAssertFalse(result.isEmpty)
        XCTAssertNotNil(result.summary)
        XCTAssertFalse(result.polls.isEmpty)
    }

    // MARK: - Localisation

    static let keys = [
        "hub.ai.title", "hub.ai.subtitle", "hub.ai.generate", "hub.ai.regenerate", "hub.ai.error",
        "hub.ai.summary_title", "hub.ai.decided", "hub.ai.missing", "hub.ai.polls_title", "hub.ai.checklist_title",
        "hub.ai.add_to_checklist", "hub.ai.add_to_checklist_a11y_format", "hub.ai.in_checklist", "hub.ai.invitation_title",
        "hub.ai.invitation.simple", "hub.ai.invitation.warm", "hub.ai.invitation.short",
        "hub.ai.copy", "hub.ai.copied", "hub.ai.share", "ai.preparing", "common.retry", "hub.primary.add_dates",
        "hub.checklist.title", "hub.checklist.progress_format", "hub.checklist.local_note", "hub.checklist.toggle_hint"
    ]

    func testEveryKeyExistsInEveryLanguage() throws {
        let root = URL(fileURLWithPath: #filePath).deletingLastPathComponent().deletingLastPathComponent()
        for locale in ["en", "fr", "es", "it", "pt"] {
            let strings = try String(contentsOf: root.appendingPathComponent("src/Resources/\(locale).lproj/Localizable.strings"), encoding: .utf8)
            for key in Self.keys {
                XCTAssertTrue(strings.contains("\"\(key)\" ="), "\(key) manquante (\(locale))")
            }
        }
        XCTAssertEqual(WK.localizedFormat("hub.ai.subtitle", locale: fr), "Générées sur ton appareil. Relis-les avant de t'en servir.")
    }

    // MARK: - Branchement

    private func source(_ path: String) throws -> String {
        try String(
            contentsOf: URL(fileURLWithPath: #filePath).deletingLastPathComponent().deletingLastPathComponent()
                .appendingPathComponent(path),
            encoding: .utf8
        )
    }

    func testCardGeneratesOnDemandWithTheFactoryOffTheMainThread() throws {
        let model = try source("src/Models/Hub/EventHubAI.swift")
        XCTAssertTrue(model.contains("WakeveAIClientFactory.makeDefault(availability: WakeveAIAvailabilityService().currentAvailability())"))
        XCTAssertTrue(model.contains("Task.detached(priority: .userInitiated)"))
        let card = try source("src/Views/Hub/EventHubAICard.swift")
        XCTAssertTrue(card.contains("WKCard("))
        XCTAssertTrue(card.contains("WKChip("))
        XCTAssertFalse(card.contains(".task"), "Pas de génération automatique à l'ouverture du hub.")
        let supplements = try source("src/Views/Hub/EventHubSupplements.swift")
        XCTAssertTrue(supplements.contains("EventHubAI.sections(for: facts)"))
        XCTAssertTrue(supplements.contains("checklist.add("), "Une suggestion ajoutée rejoint la checklist du modèle.")
    }
}
