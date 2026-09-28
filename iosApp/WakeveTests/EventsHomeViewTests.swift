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
                                value: "5", unit: "/8", daysLeft: 2)
        let text = EventsHomeView.subtitle(for: step, locale: Locale(identifier: "en"))
        XCTAssertTrue(text.contains("2"), text)
    }
}
