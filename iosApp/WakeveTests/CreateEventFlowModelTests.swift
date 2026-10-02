import XCTest
import Shared
@testable import Wakeve

/// Règles pures du flux de création en 4 questions (couche 7, #47) : validation par étape selon le
/// tableau DRAFT d'AGENTS.md, première étape invalide, progression, modèles et créneaux.
final class CreateEventFlowModelTests: XCTestCase {
    private let iso = ISO8601DateFormatter()

    private var utc: Calendar {
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = TimeZone(identifier: "UTC")!
        return calendar
    }

    private func date(_ value: String) -> Date { iso.date(from: value)! }

    private func validForm() -> CreateEventForm {
        var form = CreateEventForm()
        form.title = "Week-end à Annecy"
        form.description = "On loue un chalet"
        form.slots = [CreateEventSlotBuilder.slot(
            id: "slot-a", day: date("2026-10-10T00:00:00Z"), moment: .evening,
            start: date("2026-10-10T19:00:00Z"), end: date("2026-10-10T23:00:00Z"), calendar: utc
        )]
        return form
    }

    // MARK: - Étapes

    func testStepsAreTheFourQuestionsInOrder() {
        XCTAssertEqual(CreateEventFlowStep.allCases, [.what, .who, .place, .time])
        XCTAssertEqual(CreateEventFlowStep.what.next, .who)
        XCTAssertNil(CreateEventFlowStep.time.next)
        XCTAssertNil(CreateEventFlowStep.what.previous)
        XCTAssertEqual(CreateEventFlowStep.time.previous, .place)
        XCTAssertTrue(CreateEventFlowStep.time.isLast)
        XCTAssertFalse(CreateEventFlowStep.place.isLast)
        XCTAssertTrue(CreateEventFlowStep.place.isSkippable, "« Où ? » est optionnel.")
        XCTAssertFalse(CreateEventFlowStep.what.isSkippable)
    }

    func testProgressSegmentsMarkDoneCurrentAndUpcoming() {
        XCTAssertEqual(CreateEventFlowStep.segments(current: .what), [.current, .upcoming, .upcoming, .upcoming])
        XCTAssertEqual(CreateEventFlowStep.segments(current: .place), [.done, .done, .current, .upcoming])
        XCTAssertEqual(CreateEventFlowStep.segments(current: .time), [.done, .done, .done, .current])
    }

    // MARK: - Quoi ?

    func testWhatRequiresTrimmedTitleAndDescription() {
        var form = CreateEventForm()
        form.title = "   "
        form.description = "\n "
        let errors = form.errors(for: .what)
        XCTAssertEqual(errors[.title], "create_event.validation.title_required")
        XCTAssertEqual(errors[.description], "create_flow.error.description_required")

        form.title = "Brunch"
        form.description = "Chez Léa"
        XCTAssertTrue(form.errors(for: .what).isEmpty)
    }

    func testCustomTypeRequiresALabel() {
        var form = validForm()
        form.eventTypeName = CreateEventForm.customTypeName
        form.eventTypeCustom = "  "
        XCTAssertEqual(form.errors(for: .what)[.customType], "create_flow.error.custom_type_required")
        form.eventTypeCustom = "Pendaison de crémaillère"
        XCTAssertNil(form.errors(for: .what)[.customType])
        XCTAssertEqual(form.persistedEventTypeCustom, "Pendaison de crémaillère")

        form.eventTypeName = "BIRTHDAY"
        XCTAssertNil(form.errors(for: .what)[.customType], "Le libellé n'est exigé que pour « Autre ».")
        XCTAssertNil(form.persistedEventTypeCustom, "Le libellé n'est enregistré que pour un type personnalisé.")
    }

    func testEventTypeNamesMapToSharedEnum() {
        XCTAssertEqual(CreateEventForm.eventType(named: "BIRTHDAY"), Shared.EventType.birthday)
        XCTAssertEqual(CreateEventForm.eventType(named: "CUSTOM"), Shared.EventType.custom)
        XCTAssertEqual(CreateEventForm.eventType(named: "nope"), Shared.EventType.other)
        XCTAssertEqual(CreateEventForm().eventTypeName, "OTHER")
        XCTAssertFalse(CreateEventForm.commonEventTypeNames.contains("OTHER"))
        XCTAssertFalse(CreateEventForm.commonEventTypeNames.contains("CUSTOM"))
    }

    // MARK: - Qui ?

    func testWhoIsOptionalButCountsMustBePositive() {
        var form = validForm()
        XCTAssertTrue(form.errors(for: .who).isEmpty, "Les effectifs sont optionnels.")
        form.minParticipants = 0
        form.expectedParticipants = -2
        form.maxParticipants = 0
        let errors = form.errors(for: .who)
        XCTAssertEqual(errors[.minParticipants], "create_flow.error.participants_positive")
        XCTAssertEqual(errors[.expectedParticipants], "create_flow.error.participants_positive")
        XCTAssertEqual(errors[.maxParticipants], "create_flow.error.participants_positive")
    }

    func testMaxMustNotBeLessThanMin() {
        var form = validForm()
        form.minParticipants = 8
        form.maxParticipants = 5
        XCTAssertEqual(form.errors(for: .who)[.maxParticipants], "create_flow.error.max_less_than_min")
        form.maxParticipants = 8
        XCTAssertTrue(form.errors(for: .who).isEmpty, "max == min est valide.")
        form.minParticipants = nil
        form.maxParticipants = 3
        XCTAssertTrue(form.errors(for: .who).isEmpty, "Sans minimum, aucun ordre à vérifier.")
    }

    // MARK: - Où ?

    func testPlaceIsOptionalAndRejectsEmptyAndDuplicateNames() {
        var form = validForm()
        XCTAssertTrue(form.errors(for: .place).isEmpty, "Aucun lieu : étape valide.")
        form.locations = ["Annecy", "  ", "annecy ", "Lyon"]
        let errors = form.errors(for: .place)
        XCTAssertNil(errors[.location(0)])
        XCTAssertEqual(errors[.location(1)], "create_flow.error.location_empty")
        XCTAssertEqual(errors[.location(2)], "create_flow.error.location_duplicate", "Doublon insensible à la casse.")
        XCTAssertNil(errors[.location(3)])
    }

    func testAddingALocationIsCheckedBeforeInsertion() {
        var form = validForm()
        form.locations = ["Annecy"]
        XCTAssertEqual(form.locationAdditionError("  "), "create_flow.error.location_empty")
        XCTAssertEqual(form.locationAdditionError("ANNECY"), "create_flow.error.location_duplicate")
        XCTAssertNil(form.locationAdditionError("Chamonix"))
    }

    // MARK: - Quand ?

    func testTimeRequiresAtLeastOneSlot() {
        var form = validForm()
        form.slots = []
        XCTAssertEqual(form.errors(for: .time)[.slots], "create_event.validation.slot_required")
    }

    func testSpecificSlotRequiresStartBeforeEnd() {
        var form = validForm()
        let bad = CreateEventSlotBuilder.slot(
            id: "slot-b", day: date("2026-10-11T00:00:00Z"), moment: .specific,
            start: date("2026-10-11T20:00:00Z"), end: date("2026-10-11T19:00:00Z"), calendar: utc
        )
        let equal = CreateEventSlotBuilder.slot(
            id: "slot-c", day: date("2026-10-11T00:00:00Z"), moment: .specific,
            start: date("2026-10-11T20:00:00Z"), end: date("2026-10-11T20:00:00Z"), calendar: utc
        )
        form.slots.append(contentsOf: [bad, equal])
        let errors = form.errors(for: .time)
        XCTAssertEqual(errors[.slot("slot-b")], "create_flow.error.slot_end_before_start")
        XCTAssertEqual(errors[.slot("slot-c")], "create_flow.error.slot_end_before_start")
        XCTAssertNil(errors[.slot("slot-a")], "Un moment flou n'a pas d'heure de fin à vérifier.")
    }

    /// Créneau repris sans date : gardé s'il a un moment flou, signalé « heure à choisir » s'il est précis.
    func testDatelessSlotsAreKeptAndPreciseOnesNeedATime() {
        var form = validForm()
        form.slots = [
            CreateEventSlot(id: "flex", input: EventTimeSlotInput(start: "", end: nil, timeOfDay: .evening)),
            CreateEventSlot(id: "precise", input: EventTimeSlotInput(start: "", end: nil, timeOfDay: .specific))
        ]
        XCTAssertTrue(form.slots[0].isDateless)
        XCTAssertTrue(form.slots[1].isDateless)
        let errors = form.errors(for: .time)
        XCTAssertNil(errors[.slot("flex")], "Un moment flou sans date reste valide.")
        XCTAssertEqual(errors[.slot("precise")], "create_flow.error.slot_time_required")
        XCTAssertEqual(form.firstInvalidStep, .time)
    }

    func testMomentSlotsUseTheirTimeOfDayAndHours() {
        let day = date("2026-10-10T15:42:00Z")
        let allDay = CreateEventSlotBuilder.slot(id: "d", day: day, moment: .allDay, start: day, end: day, calendar: utc)
        XCTAssertEqual(allDay.input.timeOfDay, Shared.TimeOfDay.allDay)
        XCTAssertEqual(allDay.input.start, "2026-10-10T00:00:00Z")
        XCTAssertEqual(allDay.input.end, "2026-10-11T00:00:00Z")

        let morning = CreateEventSlotBuilder.slot(id: "m", day: day, moment: .morning, start: day, end: day, calendar: utc)
        XCTAssertEqual(morning.input.timeOfDay, Shared.TimeOfDay.morning)
        XCTAssertEqual(morning.input.start, "2026-10-10T09:00:00Z")
        XCTAssertEqual(morning.input.end, "2026-10-10T12:00:00Z")

        let afternoon = CreateEventSlotBuilder.slot(id: "a", day: day, moment: .afternoon, start: day, end: day, calendar: utc)
        XCTAssertEqual(afternoon.input.timeOfDay, Shared.TimeOfDay.afternoon)
        XCTAssertEqual(afternoon.input.start, "2026-10-10T14:00:00Z")

        let evening = CreateEventSlotBuilder.slot(id: "e", day: day, moment: .evening, start: day, end: day, calendar: utc)
        XCTAssertEqual(evening.input.timeOfDay, Shared.TimeOfDay.evening)
        XCTAssertEqual(evening.input.start, "2026-10-10T19:00:00Z")
        XCTAssertEqual(evening.input.end, "2026-10-10T23:00:00Z")

        let specific = CreateEventSlotBuilder.slot(
            id: "s", day: day, moment: .specific,
            start: date("2026-10-10T18:30:00Z"), end: date("2026-10-10T21:00:00Z"), calendar: utc
        )
        XCTAssertEqual(specific.input.timeOfDay, Shared.TimeOfDay.specific)
        XCTAssertEqual(specific.input.start, "2026-10-10T18:30:00Z")
        XCTAssertEqual(specific.input.end, "2026-10-10T21:00:00Z")
        XCTAssertEqual(specific.moment, .specific)
        XCTAssertEqual(CreateEventMoment(timeOfDay: .afternoon), .afternoon)
    }

    func testSlotIdsAreStableAndUnique() {
        let first = CreateEventSlotBuilder.newID()
        XCTAssertTrue(first.hasPrefix("slot-"))
        XCTAssertNotEqual(first, CreateEventSlotBuilder.newID())
    }

    // MARK: - Première étape invalide

    func testFirstInvalidStepFollowsTheQuestionOrder() {
        XCTAssertEqual(CreateEventForm().firstInvalidStep, .what)
        var form = validForm()
        XCTAssertNil(form.firstInvalidStep)
        form.maxParticipants = 2
        form.minParticipants = 4
        XCTAssertEqual(form.firstInvalidStep, .who)
        form.maxParticipants = nil
        form.slots = []
        XCTAssertEqual(form.firstInvalidStep, .time, "Où ? vide reste valide : la reprise saute à Quand ?.")
        form.locations = ["Paris", "paris"]
        XCTAssertEqual(form.firstInvalidStep, .place)
    }

    func testBlankFormCreatesNoDraft() {
        XCTAssertTrue(CreateEventForm().isBlank)
        var form = CreateEventForm()
        form.title = "  "
        XCTAssertTrue(form.isBlank)
        form.expectedParticipants = 4
        XCTAssertFalse(form.isBlank)
    }

    // MARK: - Modèles

    func testScenarioPrefillsWhatAndChecklist() throws {
        let scenario = try XCTUnwrap(EventScenario.allScenarios.first)
        var form = CreateEventForm()
        form.apply(scenario: scenario)
        XCTAssertEqual(form.title, scenario.suggestedTitle)
        XCTAssertEqual(form.description, scenario.description)
        XCTAssertEqual(form.eventTypeName, scenario.eventType)
        XCTAssertEqual(form.scenarioId, CreateEventForm.scenarioID(scenario))
        XCTAssertEqual(form.checklist, scenario.checklistItems)
        XCTAssertEqual(form.preparedChecklist.map(\.title), Array(scenario.checklistItems.prefix(5)))
        XCTAssertTrue(form.errors(for: .what).isEmpty)
    }

    func testSwitchingScenarioReplacesItsPrefillButKeepsTypedText() throws {
        let first = EventScenario.allScenarios[0]
        let second = EventScenario.allScenarios[1]
        var form = CreateEventForm()
        form.apply(scenario: first)
        form.apply(scenario: second)
        XCTAssertEqual(form.title, second.suggestedTitle, "Le préremplissage du modèle précédent est remplacé.")
        XCTAssertEqual(form.description, second.description)

        form.title = "Mon titre à moi"
        form.apply(scenario: first)
        XCTAssertEqual(form.title, "Mon titre à moi", "Un titre saisi n'est jamais écrasé.")
        XCTAssertEqual(form.description, first.description)

        form.clearScenario()
        XCTAssertNil(form.scenarioId)
        XCTAssertTrue(form.checklist.isEmpty)
        XCTAssertEqual(form.title, "Mon titre à moi")
    }

    func testScenarioClearsAPreviousCustomLabel() throws {
        var form = CreateEventForm()
        form.eventTypeName = CreateEventForm.customTypeName
        form.eventTypeCustom = "Crémaillère"
        form.apply(scenario: try XCTUnwrap(EventScenario.allScenarios.first))
        XCTAssertEqual(form.eventTypeCustom, "")
    }
}
