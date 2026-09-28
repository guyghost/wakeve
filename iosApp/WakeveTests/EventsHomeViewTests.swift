import XCTest
import SwiftUI
@testable import Wakeve

@MainActor
final class EventsHomeViewTests: XCTestCase {
    private let now = ISO8601DateFormatter().date(from: "2026-10-01T10:00:00Z")!

    private func summary(voted: Bool) -> HomeEventSummary {
        HomeEventSummary(facts: HomeEventFacts(
            id: "e1", title: "Raclette", phase: .polling, role: .participant, isPast: false,
            userBallotComplete: voted, votersWithCompleteBallot: 2, eligibleVoters: 4,
            deadline: nil, eventDate: nil, participantNames: ["Léa", "Tom"]
        ), now: now)
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
}
