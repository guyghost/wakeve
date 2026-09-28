import XCTest
import SwiftUI
@testable import Wakeve

@MainActor
final class EventsHomeViewTests: XCTestCase {
    private let now = ISO8601DateFormatter().date(from: "2026-10-01T10:00:00Z")!

    private func summary(voted: Bool) -> HomeEventSummary {
        HomeEventSummary(facts: HomeEventFacts(
            id: "e1", title: "Raclette", phase: .polling, role: .participant, isOwner: false,
            isPast: false, readOnly: false, pollOpen: true, viewerAccepted: true,
            ballots: HomeBallotStats(userBallotComplete: voted, votersWithCompleteBallot: 2, eligibleVoters: 4,
                                     otherVotersComplete: 1, otherEligibleVoters: 3),
            deadline: nil, eventDate: nil, participantNames: ["Léa", "Tom"]
        ))
    }

    func testCardAccessibilityLabelReadsTitleStatusAndPeople() {
        let label = HomeEventCard.accessibilityLabel(for: summary(voted: false), locale: Locale(identifier: "en"))
        XCTAssertTrue(label.hasPrefix("Raclette, "))
        XCTAssertTrue(label.contains(WK.Status.actionNeeded.localizedName) || label.contains(String(localized: "home.v2.status.vote_required")))
        XCTAssertTrue(label.contains("Léa"))
    }

    func testCardKeepsMinimumTapTargetAtAX5() {
        let host = UIHostingController(rootView: HomeEventCard(summary: summary(voted: true), onOpen: {})
            .environment(\.dynamicTypeSize, .accessibility5))
        let size = host.sizeThatFits(in: CGSize(width: 180, height: CGFloat.greatestFiniteMagnitude))
        XCTAssertGreaterThanOrEqual(size.height, WK.Size.minTapTarget)
    }

    func testHomeShowsEmptyStateAndHidesHeroWhenNoEvents() throws {
        let source = try String(contentsOf: URL(fileURLWithPath: #filePath)
            .deletingLastPathComponent().deletingLastPathComponent()
            .appendingPathComponent("src/Views/Home/EventsHomeView.swift"), encoding: .utf8)
        XCTAssertTrue(source.contains("case .empty"))
        XCTAssertTrue(source.contains("WKHeroMetric("))
        XCTAssertTrue(source.contains("if let step = viewModel.nextStep"))
        XCTAssertTrue(source.contains("DisclosureGroup"), "La section Passés est repliée par défaut.")
        XCTAssertTrue(source.contains(".contextMenu"))
        XCTAssertTrue(source.contains(".refreshable"))
    }

    func testDeletionRightComesFromTheSummaryNotAnExtraRepositoryRead() throws {
        let root = URL(fileURLWithPath: #filePath).deletingLastPathComponent().deletingLastPathComponent()
        let home = try String(contentsOf: root.appendingPathComponent("src/Views/Home/EventsHomeView.swift"), encoding: .utf8)
        XCTAssertFalse(home.contains("canDelete: (String) -> Bool"))
        XCTAssertTrue(home.contains("summary.canDelete"))
        let content = try String(contentsOf: root.appendingPathComponent("src/Views/App/ContentView.swift"), encoding: .utf8)
        XCTAssertFalse(content.contains("canDeleteFromHome"))
        XCTAssertTrue(content.contains("requestDeleteFromHome"), "Le chemin de suppression reste en place.")
    }

    func testGridUsesOneColumnAtAccessibilitySizes() {
        XCTAssertEqual(EventsHomeView.columnCount(for: .large), 2)
        XCTAssertEqual(EventsHomeView.columnCount(for: .accessibility1), 1)
    }

    func testNextStepSubtitleCombinesMissingVoteAndDeadline() {
        let step = HomeNextStep(eventId: "e", title: "Raclette", kind: .voteRequired, action: .vote,
                                metric: .votes(complete: 5, eligible: 8), daysLeft: 2)
        let text = EventsHomeView.subtitle(for: step, locale: Locale(identifier: "en"))
        XCTAssertTrue(text.contains("2"), text)
    }

    private func summary(phase: HomeEventFacts.Phase, role: HomeEventFacts.Role) -> HomeEventSummary {
        HomeEventSummary(facts: HomeEventFacts(
            id: "e2", title: "Brunch", phase: phase, role: role, isOwner: role == .organizer,
            isPast: false, readOnly: false, pollOpen: true, viewerAccepted: true, ballots: .none,
            deadline: nil, eventDate: nil, participantNames: []
        ))
    }

    func testHeroUnitAndVoiceOverValueAreLocalized() {
        let fr = Locale(identifier: "fr")
        let en = Locale(identifier: "en")
        let votes = HomeNextStep(eventId: "e", title: "Raclette", kind: .voteRequired, action: .vote,
                                 metric: .votes(complete: 5, eligible: 8), daysLeft: nil)
        XCTAssertEqual(EventsHomeView.heroUnit(for: votes, locale: fr), "/8")
        XCTAssertEqual(EventsHomeView.heroAccessibilityValue(for: votes, locale: fr), "5 votes sur 8")
        XCTAssertEqual(EventsHomeView.heroAccessibilityValue(for: votes, locale: en), "5 of 8 votes")
        let days = HomeNextStep(eventId: "e", title: "Raclette", kind: .organizing, action: .open,
                                metric: .days(5), daysLeft: 5)
        XCTAssertEqual(EventsHomeView.heroUnit(for: days, locale: fr), " jours")
        XCTAssertEqual(EventsHomeView.heroAccessibilityValue(for: days, locale: fr), "5 jours")
        XCTAssertEqual(EventsHomeView.heroUnit(for: days, locale: en), " days")
        let oneDay = HomeNextStep(eventId: "e", title: "Raclette", kind: .organizing, action: .open,
                                  metric: .days(1), daysLeft: 1)
        XCTAssertEqual(EventsHomeView.heroAccessibilityValue(for: oneDay, locale: en), "1 day")
    }

    func testOrganizingHeroSaysTodayOnTheEventDay() {
        let today = HomeNextStep(eventId: "e", title: "Raclette", kind: .organizing, action: .open,
                                 metric: .days(0), daysLeft: 0)
        let fr = Locale(identifier: "fr")
        let en = Locale(identifier: "en")
        XCTAssertEqual(EventsHomeView.heroValue(for: today, locale: fr), "aujourd'hui")
        XCTAssertEqual(EventsHomeView.heroValue(for: today, locale: en), "today")
        XCTAssertNil(EventsHomeView.heroUnit(for: today, locale: fr), "Pas de « 0 jours ».")
        XCTAssertEqual(EventsHomeView.heroAccessibilityValue(for: today, locale: fr), "aujourd'hui")
        XCTAssertEqual(EventsHomeView.heroAccessibilityValue(for: today, locale: en), "today")
        let later = HomeNextStep(eventId: "e", title: "Raclette", kind: .organizing, action: .open,
                                 metric: .days(3), daysLeft: 3)
        XCTAssertEqual(EventsHomeView.heroValue(for: later, locale: fr), "3")
        let votes = HomeNextStep(eventId: "e", title: "Raclette", kind: .voteRequired, action: .vote,
                                 metric: .votes(complete: 0, eligible: 4), daysLeft: 0)
        XCTAssertEqual(EventsHomeView.heroValue(for: votes, locale: fr), "0")
        let home = try? String(contentsOf: URL(fileURLWithPath: #filePath).deletingLastPathComponent()
            .deletingLastPathComponent().appendingPathComponent("src/Views/Home/EventsHomeView.swift"), encoding: .utf8)
        XCTAssertTrue(home?.contains("value: Self.heroValue(for: step)") == true)
    }

    func testHeroMetricPrefersExplicitVoiceOverValue() {
        XCTAssertEqual(
            WKHeroMetric.accessibilitySummary(caption: "Raclette", value: "5", unit: "/8", subtitle: "votes reçus",
                                              accessibilityValue: "5 votes sur 8"),
            "Raclette, 5 votes sur 8, votes reçus"
        )
        XCTAssertEqual(
            WKHeroMetric.accessibilitySummary(caption: "Raclette", value: "5", unit: "/8", subtitle: nil),
            "Raclette, 5/8"
        )
    }

    func testCardActionsMatchTheContextMenu() throws {
        XCTAssertEqual(EventsHomeView.cardActions(for: summary(phase: .draft, role: .organizer)), [.editDraft, .delete])
        XCTAssertEqual(EventsHomeView.cardActions(for: summary(phase: .confirmed, role: .organizer)), [.delete])
        XCTAssertEqual(EventsHomeView.cardActions(for: summary(phase: .polling, role: .participant)), [])
        let root = URL(fileURLWithPath: #filePath).deletingLastPathComponent().deletingLastPathComponent()
        let card = try String(contentsOf: root.appendingPathComponent("src/Views/Home/HomeEventCard.swift"), encoding: .utf8)
        XCTAssertTrue(card.contains(".accessibilityActions"), "Modifier / supprimer sont accessibles à VoiceOver.")
        let home = try String(contentsOf: root.appendingPathComponent("src/Views/Home/EventsHomeView.swift"), encoding: .utf8)
        XCTAssertTrue(home.contains("accessibilityValue: Self.heroAccessibilityValue(for: step)"))
        XCTAssertTrue(home.contains("viewModel.showsCreateCTA"))
    }
}
