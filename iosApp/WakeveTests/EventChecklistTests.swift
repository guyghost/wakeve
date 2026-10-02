import XCTest
@testable import Wakeve

/// Checklist du modèle choisi à la création, gardée par événement et affichée dans le hub (revue couche 9, #47).
final class EventChecklistTests: XCTestCase {
    private var counter = 0
    private func makeID() -> String {
        counter += 1
        return "item-\(counter)"
    }

    private func defaults() -> UserDefaults {
        let name = "EventChecklistTests-\(UUID().uuidString)"
        let defaults = UserDefaults(suiteName: name)!
        addTeardownBlock { defaults.removePersistentDomain(forName: name) }
        return defaults
    }

    // MARK: - Règles pures

    func testSeedingATemplateAddsUncheckedItemsInOrder() {
        let items = EventChecklist.seedingTemplate(["Réserver le lieu", "Gâteau"], into: [], makeID: makeID)
        XCTAssertEqual(items.map(\.title), ["Réserver le lieu", "Gâteau"])
        XCTAssertTrue(items.allSatisfy { !$0.isDone && $0.source == .template })
        XCTAssertEqual(Set(items.map(\.id)).count, 2)
    }

    func testReseedingKeepsCheckedStateAndSuggestionsButDropsRemovedTemplateItems() {
        var items = EventChecklist.seedingTemplate(["Réserver le lieu", "Gâteau"], into: [], makeID: makeID)
        items = EventChecklist.toggling(items[0].id, in: items)
        items = EventChecklist.adding(["Prévoir la musique"], into: items, makeID: makeID)
        let reseeded = EventChecklist.seedingTemplate(["  réserver le LIEU ", "Bougies"], into: items, makeID: makeID)
        XCTAssertEqual(reseeded.map(\.title), ["Réserver le lieu", "Bougies", "Prévoir la musique"],
                       "Modèle d'abord (titre déjà enregistré conservé), suggestions ajoutées ensuite.")
        XCTAssertEqual(reseeded.map(\.isDone), [true, false, false])
        XCTAssertEqual(reseeded[0].id, items[0].id, "Un élément conservé garde son identifiant.")
    }

    func testSeedingNoTemplateRemovesOnlyTemplateItems() {
        var items = EventChecklist.seedingTemplate(["Gâteau"], into: [], makeID: makeID)
        items = EventChecklist.adding(["Prévoir la musique"], into: items, makeID: makeID)
        XCTAssertEqual(EventChecklist.seedingTemplate([], into: items, makeID: makeID).map(\.title), ["Prévoir la musique"])
    }

    func testAddingSkipsBlankAndDuplicateTitles() {
        let items = EventChecklist.seedingTemplate(["Gâteau"], into: [], makeID: makeID)
        let added = EventChecklist.adding(["gateau", "  ", "Musique", "musique "], into: items, makeID: makeID)
        XCTAssertEqual(added.map(\.title), ["Gâteau", "Musique"])
        XCTAssertEqual(added.last?.source, .suggestion)
        XCTAssertTrue(EventChecklist.contains("GÂTEAU", in: added))
        XCTAssertFalse(EventChecklist.contains("Bougies", in: added))
    }

    func testTogglingFlipsOnlyTheTargetedItem() {
        let items = EventChecklist.seedingTemplate(["A", "B"], into: [], makeID: makeID)
        let toggled = EventChecklist.toggling(items[1].id, in: items)
        XCTAssertEqual(toggled.map(\.isDone), [false, true])
        XCTAssertEqual(EventChecklist.toggling(items[1].id, in: toggled).map(\.isDone), [false, false])
    }

    func testProgressCountsCheckedItems() {
        var items = EventChecklist.seedingTemplate(["A", "B", "C"], into: [], makeID: makeID)
        items = EventChecklist.toggling(items[0].id, in: items)
        XCTAssertEqual(EventChecklist.progress(items), EventChecklist.Progress(done: 1, total: 3))
        XCTAssertEqual(EventHubChecklistCard.progressText(EventChecklist.progress(items), locale: Locale(identifier: "fr")), "1/3 faits")
        XCTAssertEqual(EventHubChecklistCard.progressText(EventChecklist.progress(items), locale: Locale(identifier: "en")), "1/3 done")
    }

    // MARK: - Stockage local

    func testStoreRoundTripsPerEvent() {
        let store = UserDefaultsEventChecklistStore(defaults: defaults())
        XCTAssertEqual(store.items(eventId: "e1"), [])
        store.seedTemplate(eventId: "e1", titles: ["Gâteau", "Bougies"])
        store.add(eventId: "e1", titles: ["Musique"])
        let gateau = store.items(eventId: "e1")[0]
        store.toggle(eventId: "e1", itemId: gateau.id)
        let items = store.items(eventId: "e1")
        XCTAssertEqual(items.map(\.title), ["Gâteau", "Bougies", "Musique"])
        XCTAssertEqual(items.map(\.isDone), [true, false, false])
        XCTAssertEqual(store.items(eventId: "e2"), [], "Une checklist par événement.")
    }

    func testStoreIgnoresUnreadableData() {
        let defaults = defaults()
        defaults.set(Data("not json".utf8), forKey: UserDefaultsEventChecklistStore.key(eventId: "e1"))
        XCTAssertEqual(UserDefaultsEventChecklistStore(defaults: defaults).items(eventId: "e1"), [])
    }

    func testSeedingAnEmptyTemplateOnAnEmptyChecklistWritesNothing() {
        let defaults = defaults()
        UserDefaultsEventChecklistStore(defaults: defaults).seedTemplate(eventId: "e1", titles: [])
        XCTAssertNil(defaults.data(forKey: UserDefaultsEventChecklistStore.key(eventId: "e1")))
    }

    // MARK: - Création

    func testCreationContextCarriesTheTemplateChecklist() throws {
        var form = CreateEventForm()
        XCTAssertEqual(EventCreationContext(form: form).templateChecklist, [])
        let scenario = try XCTUnwrap(EventScenario.allScenarios.first)
        form.apply(scenario: scenario)
        XCTAssertEqual(EventCreationContext(form: form).templateChecklist, scenario.checklistItems)
        form.clearScenario()
        XCTAssertEqual(EventCreationContext(form: form).templateChecklist, [])
    }

    // MARK: - Hub

    private func facts(hasDetailsAccess: Bool) -> EventHubFacts {
        EventHubFacts(
            id: "e1", title: "Anniversaire", phase: .polling,
            isOrganizer: false, viewerAccepted: true,
            hasDetailsAccess: hasDetailsAccess, isLocalGuest: false,
            pollOpen: true, userBallotComplete: false, ballotsKnown: true,
            votersWithCompleteBallot: 0, otherEligibleVoters: 0, otherVotersComplete: 0,
            slotCount: 1, leadingSlotStart: nil, finalDate: nil,
            confirmedCount: 1, pendingCount: 0, participantNames: [], summaries: [:]
        )
    }

    func testHubShowsTheChecklistOnlyWhenItHasItemsAndTheViewerHasAccess() {
        let items = EventChecklist.seedingTemplate(["Gâteau"], into: [], makeID: makeID)
        XCTAssertTrue(EventHubChecklistCard.isVisible(items: items, facts: facts(hasDetailsAccess: true)))
        XCTAssertFalse(EventHubChecklistCard.isVisible(items: [], facts: facts(hasDetailsAccess: true)))
        XCTAssertFalse(EventHubChecklistCard.isVisible(items: items, facts: facts(hasDetailsAccess: false)))
    }

    // MARK: - Branchement

    private func source(_ path: String) throws -> String {
        try String(
            contentsOf: URL(fileURLWithPath: #filePath).deletingLastPathComponent().deletingLastPathComponent()
                .appendingPathComponent(path),
            encoding: .utf8
        )
    }

    func testFlowKeepsTheTemplateChecklistWithTheDraftAndOnLaunch() throws {
        let flow = try source("src/Views/Create/CreateEventFlow.swift")
        XCTAssertTrue(flow.contains("checklistStore.seedTemplate(eventId: eventId, titles: form.checklist)"),
                      "Chaque étape enregistrée garde la checklist du modèle avec le brouillon.")
        XCTAssertTrue(flow.contains("hydrated.checklist = checklistStore.items(eventId: draftEventId)"),
                      "Un brouillon repris retrouve sa checklist.")
        XCTAssertTrue(flow.contains("onLaunched(event, EventCreationContext(form: form))"))
        let content = try source("src/Views/App/ContentView.swift")
        guard let start = content.range(of: "private func persistCreationContext(_ context: EventCreationContext, for event: Event)") else {
            return XCTFail("persistCreationContext")
        }
        XCTAssertTrue(String(content[start.lowerBound...].prefix(500))
            .contains("seedTemplate(eventId: event.id, titles: context.templateChecklist)"))
        XCTAssertFalse(content.contains("potentialLocationName"), "État mort retiré.")
        guard let delete = content.range(of: "private func deleteInformationEventThroughOwner(_ event: Event)") else {
            return XCTFail("deleteInformationEventThroughOwner")
        }
        XCTAssertTrue(String(content[delete.lowerBound...].prefix(700))
            .contains("UserDefaultsEventChecklistStore().save([], eventId: event.id)"), "La checklist locale part avec l'événement.")
    }

    func testHubOwnsTheChecklistAndRendersItWithWKComponents() throws {
        let hub = try source("src/Views/Hub/EventHubView.swift")
        XCTAssertTrue(hub.contains("@StateObject private var checklist: EventChecklistModel"))
        XCTAssertTrue(hub.contains("AnyView(EventHubSupplements("))
        XCTAssertTrue(hub.contains("checklist: checklist,"))
        let supplements = try source("src/Views/Hub/EventHubSupplements.swift")
        XCTAssertTrue(supplements.contains("EventHubChecklistCard.isVisible(items: checklist.items, facts: facts)"))
        XCTAssertTrue(supplements.contains("EventHubChecklistCard("))
        let card = try source("src/Views/Hub/EventHubChecklistCard.swift")
        XCTAssertTrue(card.contains("WKCard("))
        XCTAssertTrue(card.contains(".accessibilityAddTraits(item.isDone ? .isSelected : [])"))
    }
}
