import XCTest
import Shared
@testable import Wakeve

/// Persistance étape par étape du flux de création (couche 7, #47), avec la vraie machine d'état
/// et le vrai dépôt SQLDelight de l'app hôte (identifiants d'organisateur uniques par test).
@MainActor
final class EventDraftFlowControllerTests: XCTestCase {
    private let repository = RepositoryProvider.shared.repository
    private let database = RepositoryProvider.shared.database
    private var userId = ""

    override func setUp() async throws {
        userId = "flow-test-\(UUID().uuidString.prefix(8))"
    }

    private func makeController() -> EventDraftFlowController {
        EventDraftFlowController(userId: userId)
    }

    private func whatForm(title: String = "Brunch du dimanche") -> CreateEventForm {
        var form = CreateEventForm()
        form.title = "  \(title) "
        form.description = "Chez Léa, on apporte tous un truc"
        return form
    }

    private func slot(_ id: String, _ start: String, _ end: String, moment: CreateEventMoment = .specific) -> CreateEventSlot {
        CreateEventSlot(id: id, input: EventTimeSlotInput(
            start: start, end: end, timeOfDay: moment.timeOfDay
        ))
    }

    private func revision(_ eventId: String) -> Int64? {
        repository.getEvent(id: eventId)?.aggregateRevision
    }

    // MARK: - Étape 1 : création

    func testFirstValidSaveCreatesTheDraft() async throws {
        let controller = makeController()
        XCTAssertNil(controller.eventId)
        XCTAssertNil(controller.lastSavedAt)

        var form = whatForm()
        form.eventTypeName = "BIRTHDAY"
        let result = await controller.save(step: .what, form: form)

        XCTAssertEqual(result, .saved)
        let eventId = try XCTUnwrap(controller.eventId)
        let event = try XCTUnwrap(repository.getEvent(id: eventId))
        XCTAssertEqual(event.title, "Brunch du dimanche", "Titre enregistré sans espaces superflus.")
        XCTAssertEqual(event.description_, "Chez Léa, on apporte tous un truc")
        XCTAssertEqual(event.status, .draft)
        XCTAssertEqual(event.organizerId, userId)
        XCTAssertEqual(event.eventType, Shared.EventType.birthday)
        XCTAssertNotNil(controller.lastSavedAt)
        XCTAssertFalse(controller.isSaving)
        XCTAssertTrue(
            database.invitationExperienceQueries.selectOperationReceiptsByEventId(event_id: eventId).executeAsList().isEmpty,
            "Un brouillon du flux n'a aucun reçu d'invitation (critère de réouverture dans le flux)."
        )
    }

    func testStepSavesDoNotWaitForTheSyncTransport() async throws {
        let controller = makeController()
        let start = Date()
        _ = await controller.save(step: .what, form: whatForm())
        var form = whatForm(title: "Brunch modifié")
        _ = await controller.save(step: .what, form: form)
        form.expectedParticipants = 3
        _ = await controller.save(step: .who, form: form)
        XCTAssertNotNil(controller.eventId)
        XCTAssertLessThan(Date().timeIntervalSince(start), 5, "« Continuer » ne doit pas attendre les tentatives de synchro.")
    }

    func testInvalidStepIsNeverSavedAndCreatesNothing() async {
        let controller = makeController()
        let result = await controller.save(step: .what, form: CreateEventForm())
        guard case .failed = result else { return XCTFail("Une étape invalide doit échouer : \(result)") }
        XCTAssertNil(controller.eventId, "Rien de saisi : aucun brouillon créé.")
        XCTAssertNotNil(controller.errorMessage)
    }

    // MARK: - Mises à jour (révision relue)

    func testUpdatesRereadTheAggregateRevision() async throws {
        let controller = makeController()
        _ = await controller.save(step: .what, form: whatForm())
        let eventId = try XCTUnwrap(controller.eventId)
        let created = try XCTUnwrap(revision(eventId))

        let second = await controller.save(step: .what, form: whatForm(title: "Brunch de rentrée"))
        XCTAssertEqual(second, .saved)
        XCTAssertEqual(repository.getEvent(id: eventId)?.title, "Brunch de rentrée")
        let afterSecond = try XCTUnwrap(revision(eventId))
        XCTAssertGreaterThan(afterSecond, created)

        let third = await controller.save(step: .what, form: whatForm(title: "Brunch d'automne"))
        XCTAssertEqual(third, .saved, "Chaque mise à jour relit la révision : pas d'écriture périmée.")
        XCTAssertEqual(repository.getEvent(id: eventId)?.title, "Brunch d'automne")
    }

    func testUnchangedStepSkipsTheWrite() async throws {
        let controller = makeController()
        _ = await controller.save(step: .what, form: whatForm())
        let eventId = try XCTUnwrap(controller.eventId)
        let before = revision(eventId)
        let again = await controller.save(step: .what, form: whatForm())
        XCTAssertEqual(again, .saved)
        XCTAssertEqual(revision(eventId), before, "Aucune modification : aucune nouvelle révision.")
    }

    func testCustomTypeLabelIsClearedWhenSwitchingType() async throws {
        let controller = makeController()
        var form = whatForm()
        form.eventTypeName = CreateEventForm.customTypeName
        form.eventTypeCustom = "Crémaillère"
        _ = await controller.save(step: .what, form: form)
        let eventId = try XCTUnwrap(controller.eventId)
        XCTAssertEqual(repository.getEvent(id: eventId)?.eventType, Shared.EventType.custom)
        XCTAssertEqual(repository.getEvent(id: eventId)?.eventTypeCustom, "Crémaillère")

        form.eventTypeName = "PARTY"
        let result = await controller.save(step: .what, form: form)
        XCTAssertEqual(result, .saved)
        XCTAssertEqual(repository.getEvent(id: eventId)?.eventType, Shared.EventType.party)
        XCTAssertNil(repository.getEvent(id: eventId)?.eventTypeCustom)
    }

    // MARK: - Qui ?

    func testParticipantCountsAreSavedAndCanBeCleared() async throws {
        let controller = makeController()
        var form = whatForm()
        _ = await controller.save(step: .what, form: form)
        let eventId = try XCTUnwrap(controller.eventId)

        form.minParticipants = 4
        form.expectedParticipants = 8
        form.maxParticipants = 12
        let saved1 = await controller.save(step: .who, form: form)
        XCTAssertEqual(saved1, .saved)
        var event = try XCTUnwrap(repository.getEvent(id: eventId))
        XCTAssertEqual(event.minParticipants?.intValue, 4)
        XCTAssertEqual(event.expectedParticipants?.intValue, 8)
        XCTAssertEqual(event.maxParticipants?.intValue, 12)

        form.maxParticipants = nil
        form.expectedParticipants = 6
        let saved2 = await controller.save(step: .who, form: form)
        XCTAssertEqual(saved2, .saved)
        event = try XCTUnwrap(repository.getEvent(id: eventId))
        XCTAssertNil(event.maxParticipants, "Désactiver une valeur la remet à nil.")
        XCTAssertEqual(event.expectedParticipants?.intValue, 6)
        XCTAssertEqual(event.minParticipants?.intValue, 4)

        form.maxParticipants = 2
        guard case .failed = await controller.save(step: .who, form: form) else {
            return XCTFail("max < min doit être refusé")
        }
        XCTAssertNil(repository.getEvent(id: eventId)?.maxParticipants)
    }

    // MARK: - Où ?

    func testLocationsAreSyncedWithTheDraft() async throws {
        let controller = makeController()
        var form = whatForm()
        _ = await controller.save(step: .what, form: form)
        let eventId = try XCTUnwrap(controller.eventId)
        let names: () -> [String] = {
            self.database.potentialLocationQueries.selectByEventId(eventId: eventId).executeAsList().map(\.name)
        }

        form.locations = ["Annecy", "Lyon"]
        let saved3 = await controller.save(step: .place, form: form)
        XCTAssertEqual(saved3, .saved)
        XCTAssertEqual(names(), ["Annecy", "Lyon"])

        form.locations = ["Lyon", "Chamonix"]
        let saved4 = await controller.save(step: .place, form: form)
        XCTAssertEqual(saved4, .saved)
        XCTAssertEqual(Set(names()), ["Lyon", "Chamonix"])
        XCTAssertEqual(names().count, 2)

        form.locations = ["Lyon", "lyon"]
        guard case .failed = await controller.save(step: .place, form: form) else {
            return XCTFail("Doublon refusé")
        }
        XCTAssertEqual(names().count, 2)
    }

    // MARK: - Quand ?

    func testSlotsAreSavedWithStableIdsAndCanBeRemoved() async throws {
        let controller = makeController()
        var form = whatForm()
        _ = await controller.save(step: .what, form: form)
        let eventId = try XCTUnwrap(controller.eventId)

        form.slots = [
            slot("slot-one", "2026-10-10T19:00:00Z", "2026-10-10T23:00:00Z", moment: .evening),
            slot("slot-two", "2026-10-11T18:00:00Z", "2026-10-11T21:00:00Z")
        ]
        let saved5 = await controller.save(step: .time, form: form)
        XCTAssertEqual(saved5, .saved)
        var slots = try XCTUnwrap(repository.getEvent(id: eventId)).proposedSlots
        XCTAssertEqual(Set(slots.map(\.id)), ["slot-one", "slot-two"])
        XCTAssertEqual(slots.first { $0.id == "slot-one" }?.timeOfDay, Shared.TimeOfDay.evening)

        form.slots.removeFirst()
        let saved6 = await controller.save(step: .time, form: form)
        XCTAssertEqual(saved6, .saved)
        slots = try XCTUnwrap(repository.getEvent(id: eventId)).proposedSlots
        XCTAssertEqual(slots.map(\.id), ["slot-two"])
    }

    // MARK: - Reprise

    func testHydrateRebuildsTheFormOfAFlowDraft() async throws {
        let controller = makeController()
        var form = whatForm()
        form.eventTypeName = "PARTY"
        _ = await controller.save(step: .what, form: form)
        form.expectedParticipants = 5
        _ = await controller.save(step: .who, form: form)
        form.locations = ["Annecy"]
        _ = await controller.save(step: .place, form: form)
        let eventId = try XCTUnwrap(controller.eventId)

        let resumed = makeController()
        let hydrated = try XCTUnwrap(resumed.hydrate(eventId: eventId))
        XCTAssertEqual(resumed.eventId, eventId)
        XCTAssertEqual(hydrated.title, "Brunch du dimanche")
        XCTAssertEqual(hydrated.description, "Chez Léa, on apporte tous un truc")
        XCTAssertEqual(hydrated.eventTypeName, "PARTY")
        XCTAssertEqual(hydrated.expectedParticipants, 5)
        XCTAssertNil(hydrated.minParticipants)
        XCTAssertEqual(hydrated.locations, ["Annecy"])
        XCTAssertTrue(hydrated.slots.isEmpty)
        XCTAssertEqual(hydrated.firstInvalidStep, .time, "La reprise ouvre l'étape de la première validation en échec.")

        var next = hydrated
        next.slots = [slot("slot-r", "2026-10-12T10:00:00Z", "2026-10-12T12:00:00Z")]
        let saved7 = await resumed.save(step: .time, form: next)
        XCTAssertEqual(saved7, .saved, "Le contrôleur repris met à jour le même brouillon.")
        XCTAssertEqual(repository.getEvent(id: eventId)?.proposedSlots.count, 1)
        XCTAssertNil(makeController().hydrate(eventId: "missing-\(UUID().uuidString)"))
    }

    // MARK: - Lancement

    func testLaunchWithoutSlotIsRefusedAndKeepsTheDraft() async throws {
        let controller = makeController()
        _ = await controller.save(step: .what, form: whatForm())
        let eventId = try XCTUnwrap(controller.eventId)

        let result = await controller.launch()
        XCTAssertEqual(result, .failed(String(localized: "participants.start_poll.requires_slot")))
        XCTAssertEqual(repository.getEvent(id: eventId)?.status, .draft)
    }

    func testLaunchStartsThePoll() async throws {
        let controller = makeController()
        var form = whatForm()
        _ = await controller.save(step: .what, form: form)
        form.slots = [slot("slot-l", "2026-10-20T18:00:00Z", "2026-10-20T22:00:00Z")]
        _ = await controller.save(step: .time, form: form)
        let eventId = try XCTUnwrap(controller.eventId)

        let result = await controller.launch()
        XCTAssertEqual(result, .launched(eventId))
        XCTAssertEqual(repository.getEvent(id: eventId)?.status, .polling)
    }

    func testLaunchWithoutDraftFails() async {
        guard case .failed = await makeController().launch() else { return XCTFail("Aucun brouillon à lancer") }
    }
}
