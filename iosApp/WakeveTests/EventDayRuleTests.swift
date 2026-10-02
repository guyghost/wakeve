import XCTest
@testable import Wakeve

/// Règle du jour J et lecture pure de ses données (couche 8, #47).
final class EventDayRuleTests: XCTestCase {
    private func date(_ iso: String) -> Date { ISO8601DateFormatter().date(from: iso)! }
    private var utcCalendar: Calendar {
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = TimeZone(identifier: "UTC")!
        return calendar
    }

    private func isEventDay(
        phase: EventHubFacts.Phase = .organizing,
        invitationRollout: Bool = false,
        start: String? = "2026-10-03T17:30:00Z",
        end: String? = "2026-10-03T21:00:00Z",
        timezone: String? = "Europe/Paris",
        hasAccess: Bool = true,
        now: String
    ) -> Bool {
        EventDayRule.isEventDay(
            phase: phase,
            invitationRollout: invitationRollout,
            finalDate: start.map(date),
            slotStart: start.map(date),
            slotEnd: end.map(date),
            timezone: timezone,
            hasAccess: hasAccess,
            now: date(now),
            calendar: utcCalendar
        )
    }

    func testOrganizingEventWithAccessIsTheEventDayOnItsRetainedDay() {
        XCTAssertTrue(isEventDay(now: "2026-10-03T08:00:00Z"))
        XCTAssertTrue(isEventDay(now: "2026-10-03T21:30:00Z"), "Après la fin, le même jour : toujours le jour J.")
        XCTAssertFalse(isEventDay(now: "2026-10-02T12:00:00Z"))
        XCTAssertFalse(isEventDay(now: "2026-10-04T12:00:00Z"))
    }

    func testOnlyOrganizingOrFinalizedWithoutInvitationRollout() {
        for phase in [EventHubFacts.Phase.draft, .polling, .comparing, .confirmed] {
            XCTAssertFalse(isEventDay(phase: phase, now: "2026-10-03T08:00:00Z"), "\(phase)")
        }
        XCTAssertTrue(isEventDay(phase: .finalized, invitationRollout: false, now: "2026-10-03T08:00:00Z"))
        XCTAssertFalse(isEventDay(phase: .finalized, invitationRollout: true, now: "2026-10-03T08:00:00Z"),
                       "Rollout invitation : un événement finalisé part aux archives (conflit différé).")
        XCTAssertTrue(isEventDay(phase: .organizing, invitationRollout: true, now: "2026-10-03T08:00:00Z"))
    }

    func testRequiresDetailsAccess() {
        XCTAssertFalse(isEventDay(hasAccess: false, now: "2026-10-03T08:00:00Z"))
    }

    /// Le jour se compte dans le fuseau du créneau, pas dans celui de l'appareil.
    func testDayIsComputedInTheSlotTimeZone() {
        // 22:30 UTC le 3 = 00:30 le 4 à Paris.
        let start = "2026-10-03T22:30:00Z", end = "2026-10-04T01:00:00Z"
        XCTAssertTrue(isEventDay(start: start, end: end, now: "2026-10-04T09:00:00Z"))
        XCTAssertFalse(isEventDay(start: start, end: end, now: "2026-10-03T12:00:00Z"), "Le 3 à Paris : pas encore.")
        XCTAssertTrue(isEventDay(start: start, end: end, timezone: "America/New_York", now: "2026-10-03T12:00:00Z"),
                      "À New York, le créneau tombe le 3.")
    }

    func testMidnightBoundaryInTheSlotTimeZone() {
        // Minuit à Paris le 4 = 22:00 UTC le 3.
        let start = "2026-10-03T22:00:00Z", end = "2026-10-04T02:00:00Z"
        XCTAssertFalse(isEventDay(start: start, end: end, now: "2026-10-03T21:59:00Z"))
        XCTAssertTrue(isEventDay(start: start, end: end, now: "2026-10-03T22:00:00Z"))
    }

    func testSlotSpanningTwoDaysCountsWhileInProgress() {
        // Vendredi 22:00 → samedi 04:00 (Paris).
        let start = "2026-10-02T20:00:00Z", end = "2026-10-03T02:00:00Z"
        XCTAssertTrue(isEventDay(start: start, end: end, now: "2026-10-02T19:00:00Z"), "Vendredi : jour du début.")
        XCTAssertTrue(isEventDay(start: start, end: end, now: "2026-10-03T01:00:00Z"), "Samedi 03:00 : en cours.")
        XCTAssertFalse(isEventDay(start: start, end: end, now: "2026-10-03T10:00:00Z"), "Samedi matin : terminé.")
        // Week-end : samedi 10:00 → dimanche 18:00.
        XCTAssertTrue(isEventDay(start: "2026-10-03T08:00:00Z", end: "2026-10-04T16:00:00Z", now: "2026-10-04T10:00:00Z"))
    }

    /// Journée entière (`EventSlotInputBuilder`) : minuit local → minuit suivant, borne de fin exclue.
    func testAllDaySlot() {
        let start = "2026-10-02T22:00:00Z", end = "2026-10-03T22:00:00Z"
        XCTAssertTrue(isEventDay(start: start, end: end, now: "2026-10-03T21:30:00Z"))
        XCTAssertFalse(isEventDay(start: start, end: end, now: "2026-10-03T22:00:00Z"), "Minuit suivant : fini.")
    }

    func testUnknownTimeZoneFallsBackToTheCalendarAndMissingDatesNeverMatch() {
        XCTAssertTrue(isEventDay(timezone: "Not/AZone", now: "2026-10-03T08:00:00Z"))
        XCTAssertTrue(isEventDay(timezone: nil, now: "2026-10-03T08:00:00Z"))
        XCTAssertFalse(isEventDay(start: nil, end: nil, now: "2026-10-03T08:00:00Z"))
    }

    func testDayStringUsesTheSlotTimeZone() {
        let paris = TimeZone(identifier: "Europe/Paris")!
        XCTAssertEqual(EventDayRule.dayString(date("2026-10-03T22:30:00Z"), timeZone: paris), "2026-10-04")
        XCTAssertEqual(EventDayRule.dayString(date("2026-10-03T21:30:00Z"), timeZone: paris), "2026-10-03")
    }

    // MARK: - Lieu

    func testCoordinatesParseTheExistingJSONFormat() {
        let parsed = EventDayPlaceResolver.coordinates(from: #"{"latitude": 45.8992, "longitude": 6.1294}"#)
        XCTAssertEqual(parsed?.latitude, 45.8992)
        XCTAssertEqual(parsed?.longitude, 6.1294)
        XCTAssertEqual(EventDayPlaceResolver.coordinates(from: #"{"latitude": 45, "longitude": 6}"#)?.latitude, 45)
        XCTAssertNil(EventDayPlaceResolver.coordinates(from: nil))
        XCTAssertNil(EventDayPlaceResolver.coordinates(from: ""))
        XCTAssertNil(EventDayPlaceResolver.coordinates(from: "Annecy"))
        XCTAssertNil(EventDayPlaceResolver.coordinates(from: #"{"latitude": 95, "longitude": 6}"#), "Latitude hors bornes.")
        XCTAssertNil(EventDayPlaceResolver.coordinates(from: #"{"lat": 45, "lng": 6}"#))
    }

    private let annecy = EventDayPlaceResolver.Location(
        id: "l1", name: "Annecy", address: "Place de la Gare", coordinatesJSON: #"{"latitude": 45.9, "longitude": 6.12}"#
    )
    private let lyon = EventDayPlaceResolver.Location(id: "l2", name: "Lyon", address: nil, coordinatesJSON: nil)

    func testRetainedScenarioWinsAndBorrowsItsSourceLocationCoordinates() {
        let place = EventDayPlaceResolver.resolve(
            selectedScenario: .init(location: "Chalet du lac", sourcePotentialLocationId: "l1"),
            potentialLocations: [lyon, annecy]
        )
        XCTAssertEqual(place?.name, "Chalet du lac")
        XCTAssertEqual(place?.address, "Place de la Gare")
        XCTAssertEqual(place?.latitude, 45.9)
    }

    func testRetainedScenarioMatchesALocationByNameOtherwiseHasNoCoordinates() {
        let byName = EventDayPlaceResolver.resolve(
            selectedScenario: .init(location: " annecy ", sourcePotentialLocationId: nil),
            potentialLocations: [lyon, annecy]
        )
        XCTAssertEqual(byName?.name, "annecy")
        XCTAssertEqual(byName?.longitude, 6.12)
        let unknown = EventDayPlaceResolver.resolve(
            selectedScenario: .init(location: "Genève", sourcePotentialLocationId: nil),
            potentialLocations: [annecy]
        )
        XCTAssertEqual(unknown?.name, "Genève")
        XCTAssertNil(unknown?.latitude)
        XCTAssertFalse(unknown?.hasCoordinate ?? true)
    }

    func testWithoutRetainedScenarioTheFirstPotentialLocationIsUsed() {
        let place = EventDayPlaceResolver.resolve(selectedScenario: nil, potentialLocations: [lyon, annecy])
        XCTAssertEqual(place?.name, "Lyon")
        XCTAssertNil(place?.latitude)
        XCTAssertEqual(
            EventDayPlaceResolver.resolve(selectedScenario: .init(location: "  ", sourcePotentialLocationId: nil),
                                          potentialLocations: [annecy])?.name,
            "Annecy", "Scénario sans lieu : premier lieu potentiel."
        )
        XCTAssertNil(EventDayPlaceResolver.resolve(selectedScenario: nil, potentialLocations: []))
    }

    // MARK: - Faits

    func testFactsDelegateToTheRuleAndKeepTodayMealsInOrder() {
        let facts = EventDayFacts(
            eventId: "e1", title: "Week-end", eventTypeName: nil, phase: .organizing, hasAccess: true,
            slot: RetainedSlot(start: date("2026-10-03T17:30:00Z"), end: date("2026-10-03T21:00:00Z"),
                               timeZoneIdentifier: "Europe/Paris", timeOfDayName: "SPECIFIC"),
            place: nil, transport: .chosen,
            meals: [EventDayMeal(name: "Dîner", time: "20:00", statusName: "PLANNED")],
            confirmedCount: 4, pendingCount: 1
        )
        XCTAssertTrue(facts.isEventDay(now: date("2026-10-03T09:00:00Z"), invitationRollout: false))
        XCTAssertFalse(facts.isEventDay(now: date("2026-10-05T09:00:00Z"), invitationRollout: false))
        let flexible = EventDayFacts(
            eventId: "e1", title: "Week-end", eventTypeName: nil, phase: .organizing, hasAccess: true,
            slot: RetainedSlot(start: nil, end: nil, timeZoneIdentifier: "Europe/Paris", timeOfDayName: "MORNING"),
            place: nil, transport: nil, meals: [], confirmedCount: 0, pendingCount: 0,
            finalDate: date("2026-10-03T08:00:00Z")
        )
        XCTAssertTrue(flexible.isEventDay(now: date("2026-10-03T15:00:00Z"), invitationRollout: false),
                      "Créneau flexible sans début : la date retenue fait foi, comme au hub et à l'accueil.")
        XCTAssertEqual(
            EventDayMeal.today([
                EventDayMeal(name: "Brunch", time: "11:00", statusName: "CANCELLED"),
                EventDayMeal(name: "Dîner", time: "20:00", statusName: "PLANNED"),
                EventDayMeal(name: "Apéro", time: "18:30", statusName: "ASSIGNED")
            ]).map(\.name),
            ["Apéro", "Dîner"], "Repas annulés exclus, triés par heure."
        )
    }
}
