import XCTest
import SwiftUI
@testable import Wakeve

/// Jour J en mode immersif (couche 8, #47) : présentation, itinéraire, entrées (hub, accueil) et textes.
@MainActor
final class EventDayViewTests: XCTestCase {
    private let fr = Locale(identifier: "fr_FR")
    private func date(_ iso: String) -> Date { ISO8601DateFormatter().date(from: iso)! }

    private func facts(
        slot: RetainedSlot? = RetainedSlot(start: ISO8601DateFormatter().date(from: "2026-10-03T17:30:00Z"),
                                           end: ISO8601DateFormatter().date(from: "2026-10-03T21:00:00Z"),
                                           timeZoneIdentifier: "Europe/Paris", timeOfDayName: "SPECIFIC"),
        place: EventDayPlace? = EventDayPlace(name: "Gare d'Annecy", address: "Place de la Gare", latitude: 45.9, longitude: 6.12),
        transport: HubModuleSheetData.TransportState? = .chosen,
        meals: [EventDayMeal] = [EventDayMeal(name: "Dîner", time: "20:00", statusName: "PLANNED")],
        confirmed: Int = 5,
        pending: Int = 1,
        finalDate: Date? = nil
    ) -> EventDayFacts {
        EventDayFacts(
            eventId: "e1", title: "Week-end Annecy", eventTypeName: "TRAVEL", phase: .organizing, hasAccess: true,
            slot: slot, place: place, transport: transport, meals: meals,
            confirmedCount: confirmed, pendingCount: pending, finalDate: finalDate
        )
    }

    private func model(_ facts: EventDayFacts) -> EventDayModel { EventDayModel(facts: facts, locale: fr) }

    // MARK: - Présentation

    func testTimeAndDateAreShownInTheSlotTimeZone() {
        let m = model(facts())
        XCTAssertEqual(m.caption, "C'est aujourd'hui")
        XCTAssertEqual(m.time, "19:30", "17:30 UTC = 19:30 à Paris.")
        XCTAssertEqual(m.date, "samedi 3 octobre")
        XCTAssertEqual(m.until, "jusqu'à 23:00")
    }

    func testAllDaySlotHasNoTimeOfMeeting() {
        let allDay = model(facts(slot: RetainedSlot(start: date("2026-10-02T22:00:00Z"), end: date("2026-10-03T22:00:00Z"),
                                                    timeZoneIdentifier: "Europe/Paris", timeOfDayName: "ALL_DAY")))
        XCTAssertEqual(allDay.time, "Toute la journée")
        XCTAssertEqual(allDay.date, "samedi 3 octobre")
        XCTAssertNil(allDay.until)
        XCTAssertNil(model(facts(slot: nil)).time)
    }

    /// Créneau flexible (matin, sans heure de début) : date retenue (`event.finalDate`) et moment de la journée,
    /// jamais une heure inventée.
    func testFlexibleSlotShowsTheRetainedDateAndTheTimeOfDay() {
        let morning = model(facts(
            slot: RetainedSlot(start: nil, end: nil, timeZoneIdentifier: "Europe/Paris", timeOfDayName: "MORNING"),
            finalDate: date("2026-10-03T08:00:00Z")
        ))
        XCTAssertEqual(morning.time, "Matin")
        XCTAssertEqual(morning.date, "samedi 3 octobre")
        XCTAssertNil(morning.until)
        let evening = model(facts(
            slot: RetainedSlot(start: nil, end: nil, timeZoneIdentifier: "Europe/Paris", timeOfDayName: "EVENING"),
            finalDate: date("2026-10-03T08:00:00Z")
        ))
        XCTAssertEqual(evening.time, "Soir")
        let afternoon = EventDayModel(facts: facts(
            slot: RetainedSlot(start: nil, end: nil, timeZoneIdentifier: "Europe/Paris", timeOfDayName: "AFTERNOON"),
            finalDate: date("2026-10-03T08:00:00Z")
        ), locale: Locale(identifier: "en_US"))
        XCTAssertEqual(afternoon.time, "Afternoon")
    }

    /// Date retenue héritée (`event.finalDate`) sans ligne `confirmedDate` : la date, sans heure.
    func testLegacyFinalDateWithoutConfirmedDateRowShowsTheDate() {
        let legacy = model(facts(slot: nil, finalDate: date("2026-10-03T12:00:00Z")))
        XCTAssertEqual(legacy.date, "samedi 3 octobre")
        XCTAssertNil(legacy.time)
        XCTAssertNil(legacy.until)
        XCTAssertNil(model(facts(slot: nil)).date)
    }

    func testPlaceFallsBackToAHint() {
        XCTAssertEqual(model(facts()).placeName, "Gare d'Annecy")
        XCTAssertEqual(model(facts()).placeAddress, "Place de la Gare")
        XCTAssertEqual(model(facts(place: nil)).placeName, "Lieu à préciser")
        XCTAssertNil(model(facts(place: nil)).placeAddress)
    }

    func testPillsShowTransportTodayMealsAndParticipants() {
        let pills = model(facts()).pills
        XCTAssertEqual(pills.map(\.text), [
            HubModuleSheetData.transportPill(.chosen, locale: fr)?.text,
            "Dîner · 20:00",
            HubSummaryText.participants(confirmed: 5, pending: 1, locale: fr)
        ].compactMap { $0 })
        XCTAssertEqual(pills.map(\.systemImage), ["car", "fork.knife", "person.2"])
        XCTAssertEqual(model(facts(transport: .noPlan, meals: [], confirmed: 0, pending: 0)).pills, [],
                       "Sans plan de transport, repas ni invité : aucune pastille.")
    }

    /// Deux repas identiques restent deux pastilles distinctes (identifiants uniques pour `ForEach`).
    func testPillIdentifiersAreUnique() {
        let twin = EventDayMeal(name: "Dîner", time: "20:00", statusName: "PLANNED")
        let pills = model(facts(meals: [twin, twin])).pills
        XCTAssertEqual(pills.count, 4)
        XCTAssertEqual(Set(pills.map(\.id)).count, pills.count, "\(pills.map(\.id))")
    }

    func testDirectionsUseCoordinatesOrSearchByName() {
        XCTAssertEqual(model(facts()).directions, .coordinate(latitude: 45.9, longitude: 6.12, name: "Gare d'Annecy"))
        let byName = model(facts(place: EventDayPlace(name: "Chalet du lac", address: "Talloires", latitude: nil, longitude: nil)))
        XCTAssertEqual(byName.directions, .search(query: "Chalet du lac, Talloires"))
        XCTAssertNil(model(facts(place: nil)).directions)
        XCTAssertEqual(
            EventDayDirections.search(query: "Chalet du lac, Talloires").searchURL?.absoluteString,
            "maps://?daddr=Chalet%20du%20lac,%20Talloires"
        )
    }

    func testActionsAreDirectionsThenTheEvent() {
        let m = model(facts())
        XCTAssertEqual(m.primaryTitle, "Itinéraire")
        XCTAssertEqual(m.secondaryTitle, "Voir l'événement")
        let noPlace = model(facts(place: nil))
        XCTAssertEqual(noPlace.primaryTitle, "Voir l'événement", "Sans lieu, une seule action.")
        XCTAssertNil(noPlace.secondaryTitle)
    }

    // MARK: - Rendu

    func testEventDayFitsA375ptScreenAtAX5() {
        for facts in [facts(), facts(place: EventDayPlace(name: "Chalet", address: nil, latitude: nil, longitude: nil))] {
            let view = EventDayView(model: model(facts), onDirections: {}, onViewEvent: {}, onClose: {})
            let size = fittingSize(view.frame(height: 812), width: 375, dynamicType: .accessibility5)
            XCTAssertLessThanOrEqual(size.width, 375, "\(size)")
        }
        XCTAssertEqual(EventDayView.accessibilityID, "immersive.eventDay")
        XCTAssertEqual(EventDayView.mapAccessibilityID, "immersive.eventDay.map")
    }

    // MARK: - Hub

    private func hubFacts(phase: EventHubFacts.Phase = .organizing, slot: RetainedSlot?) -> EventHubFacts {
        var facts = EventHubFacts(
            id: "e1", title: "Week-end Annecy", phase: phase, isOrganizer: true, viewerAccepted: true,
            hasDetailsAccess: true, isLocalGuest: false, pollOpen: false, userBallotComplete: true, ballotsKnown: true,
            votersWithCompleteBallot: 0, otherEligibleVoters: 0, otherVotersComplete: 0, slotCount: 1,
            leadingSlotStart: nil, finalDate: slot?.start, confirmedCount: 2, pendingCount: 0,
            participantNames: ["Léa"], summaries: [:]
        )
        facts.retainedSlot = slot
        return facts
    }

    func testHubFactsKnowTheEventDay() {
        let slot = RetainedSlot(start: date("2026-10-03T17:30:00Z"), end: date("2026-10-03T21:00:00Z"),
                                timeZoneIdentifier: "Europe/Paris", timeOfDayName: "SPECIFIC")
        XCTAssertTrue(hubFacts(slot: slot).isEventDay(now: date("2026-10-03T08:00:00Z"), invitationRollout: false))
        XCTAssertFalse(hubFacts(slot: slot).isEventDay(now: date("2026-10-04T08:00:00Z"), invitationRollout: false))
        XCTAssertFalse(hubFacts(slot: nil).isEventDay(now: date("2026-10-03T08:00:00Z"), invitationRollout: false))
        XCTAssertFalse(hubFacts(phase: .confirmed, slot: slot).isEventDay(now: date("2026-10-03T08:00:00Z"), invitationRollout: false))
    }

    /// La bannière suit l'heure : recalculée chaque minute (hub ouvert à minuit, retour au premier plan).
    func testHubRecomputesTheEventDayEveryMinute() throws {
        let source = try String(contentsOf: URL(fileURLWithPath: #filePath).deletingLastPathComponent()
            .deletingLastPathComponent().appendingPathComponent("src/Views/Hub/EventHubView.swift"), encoding: .utf8)
        XCTAssertTrue(source.contains("TimelineView(.everyMinute) { context in"))
        XCTAssertTrue(source.contains("isEventDay: facts.isEventDay(now: context.date, invitationRollout: invitationRollout)"))
        XCTAssertFalse(source.contains("isEventDay(now: Date()"), "Plus d'instant figé au chargement.")
    }

    func testHubHeroShowsTheTodayBannerOnlyOnTheEventDay() {
        let facts = hubFacts(slot: nil)
        let model = EventHubModel(facts: facts)
        let plain = fittingSize(EventHubContent(facts: facts, model: model, onOpenModule: { _ in }, onQuickVote: {}), width: 375)
        let banner = fittingSize(EventHubContent(facts: facts, model: model, onOpenModule: { _ in }, onQuickVote: {},
                                                 isEventDay: true, onOpenEventDay: {}), width: 375)
        XCTAssertGreaterThanOrEqual(banner.height, plain.height + WK.Size.minTapTarget, "sans \(plain) / avec \(banner)")
        XCTAssertEqual(EventHubContent.eventDayAccessibilityID, "hub.eventDay")
    }

    // MARK: - Accueil

    private let now = ISO8601DateFormatter().date(from: "2026-10-03T08:00:00Z")!

    private func homeFacts(id: String, phase: HomeEventFacts.Phase, role: HomeEventFacts.Role = .participant,
                           userBallotComplete: Bool = true, isEventDay: Bool = false) -> HomeEventFacts {
        var facts = HomeEventFacts(
            id: id, title: id, phase: phase, role: role, isOwner: role == .organizer, isPast: false, readOnly: false,
            pollOpen: true, viewerAccepted: true,
            ballots: HomeBallotStats(userBallotComplete: userBallotComplete, votersWithCompleteBallot: 1, eligibleVoters: 3,
                                     otherVotersComplete: 0, otherEligibleVoters: 2),
            deadline: nil, eventDate: nil, participantNames: []
        )
        facts.isEventDay = isEventDay
        return facts
    }

    func testNextStepEventDayComesRightAfterVotingAndConfirming() {
        let today = homeFacts(id: "today", phase: .organizing, isEventDay: true)
        let vote = homeFacts(id: "vote", phase: .polling, userBallotComplete: false)
        let polling = homeFacts(id: "poll", phase: .polling, role: .organizer)
        let organizing = homeFacts(id: "orga", phase: .organizing, role: .organizer)

        XCTAssertEqual(HomeNextStep.pick(from: [today, vote], now: now)?.eventId, "vote")
        let step = HomeNextStep.pick(from: [polling, organizing, today], now: now)
        XCTAssertEqual(step?.eventId, "today")
        XCTAssertEqual(step?.kind, .eventDay)
        XCTAssertEqual(step?.action, .eventDay)
        XCTAssertEqual(step?.metric, .days(0))
    }

    func testEventDayStepEvenOnceTheSlotIsOverToday() {
        var past = homeFacts(id: "today", phase: .organizing, isEventDay: true)
        past = HomeEventFacts(
            id: past.id, title: past.title, phase: past.phase, role: past.role, isOwner: past.isOwner, isPast: true,
            readOnly: false, pollOpen: true, viewerAccepted: true, ballots: past.ballots, deadline: nil, eventDate: nil,
            participantNames: [], isEventDay: true
        )
        XCTAssertEqual(HomeNextStep.pick(from: [past], now: now)?.kind, .eventDay)
    }

    func testHomeFactsComputeTheEventDayFromTheRetainedSlot() {
        var raw = HomeRawEvent(
            id: "e1", title: "e1", statusName: "ORGANIZING", isOrganizer: false, isOwner: false, isPast: false,
            readOnly: false, viewerAccepted: true, deadlineISO: "2026-09-01T00:00:00Z", finalDateISO: "2026-10-03T17:30:00Z",
            earliestSlotStartISO: nil, ballots: .none, participantNames: [], hasPendingSync: false
        )
        raw.retainedSlot = RetainedSlot(start: date("2026-10-03T17:30:00Z"), end: date("2026-10-03T21:00:00Z"),
                                        timeZoneIdentifier: "Europe/Paris", timeOfDayName: "SPECIFIC")
        raw.hasDetailsAccess = true
        XCTAssertTrue(EventsHomeViewModel.facts(from: raw, now: now).isEventDay)
        raw.hasDetailsAccess = false
        XCTAssertFalse(EventsHomeViewModel.facts(from: raw, now: now).isEventDay, "Participant sans accès : pas de jour J.")
        raw.hasDetailsAccess = true
        raw = HomeRawEvent(
            id: raw.id, title: raw.title, statusName: "FINALIZED", isOrganizer: false, isOwner: false, isPast: false,
            readOnly: true, viewerAccepted: true, deadlineISO: raw.deadlineISO, finalDateISO: raw.finalDateISO,
            earliestSlotStartISO: nil, ballots: .none, participantNames: [], hasPendingSync: false,
            retainedSlot: raw.retainedSlot, hasDetailsAccess: true
        )
        XCTAssertTrue(EventsHomeViewModel.facts(from: raw, now: now, invitationRollout: false).isEventDay)
        XCTAssertFalse(EventsHomeViewModel.facts(from: raw, now: now, invitationRollout: true).isEventDay)
    }

    func testHomeCardCopyForTheEventDay() {
        let step = HomeNextStep(eventId: "e1", title: "Week-end", kind: .eventDay, action: .eventDay, metric: .days(0), daysLeft: 0)
        XCTAssertEqual(EventsHomeView.subtitle(for: step, locale: fr), "C'est le jour J")
        XCTAssertEqual(EventsHomeView.heroValue(for: step, locale: fr), "aujourd'hui")
        XCTAssertEqual(WK.localizedFormat("home.v2.next_step.action.event_day", locale: fr), "Voir le jour J")
    }

    // MARK: - Textes

    static let keys = [
        "immersive.event_day.caption", "immersive.event_day.all_day", "immersive.event_day.until_format",
        "immersive.event_day.no_place", "immersive.event_day.directions", "immersive.event_day.map_format",
        "immersive.event_day.banner", "immersive.event_day.banner_hint", "immersive.view_event",
        "home.v2.next_step.event_day.subtitle", "home.v2.next_step.action.event_day"
    ]

    func testEventDayKeysExistInEveryLanguage() throws {
        let resources = URL(fileURLWithPath: #filePath).deletingLastPathComponent().deletingLastPathComponent()
            .appendingPathComponent("src/Resources")
        for locale in ["en", "fr", "es", "it", "pt"] {
            let strings = try String(contentsOf: resources.appendingPathComponent("\(locale).lproj/Localizable.strings"), encoding: .utf8)
            for key in Self.keys {
                XCTAssertTrue(strings.contains("\"\(key)\" ="), "\(key) manquante (\(locale))")
            }
        }
    }
}
