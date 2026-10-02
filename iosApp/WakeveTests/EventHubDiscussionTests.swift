import XCTest
@testable import Wakeve

/// Entrée « Discussion » du hub (revue couche 9, #47) : le fil général de l'événement,
/// perdu avec l'aperçu des messages de l'ancien détail.
final class EventHubDiscussionTests: XCTestCase {
    private let fr = Locale(identifier: "fr")
    private let en = Locale(identifier: "en")

    private func facts(phase: EventHubFacts.Phase, hasDetailsAccess: Bool, isOrganizer: Bool = false) -> EventHubFacts {
        EventHubFacts(
            id: "e1", title: "Week-end", phase: phase,
            isOrganizer: isOrganizer, viewerAccepted: true,
            hasDetailsAccess: hasDetailsAccess || isOrganizer, isLocalGuest: false,
            pollOpen: true, userBallotComplete: false, ballotsKnown: true,
            votersWithCompleteBallot: 0, otherEligibleVoters: 0, otherVotersComplete: 0,
            slotCount: 1, leadingSlotStart: nil, finalDate: nil,
            confirmedCount: 1, pendingCount: 0, participantNames: [], summaries: [:]
        )
    }

    /// Le fil (`case .comments`) est gardé par `canAccessDetailedPlanning` (confirmé et après, avec l'accès) :
    /// avant la date retenue, une tuile serait verrouillée pour tout le monde, donc elle n'apparaît pas.
    func testDiscussionTileAppearsOnceTheDateIsRetained() {
        XCTAssertFalse(EventHubModel.modules(for: .draft).contains(.discussion))
        XCTAssertFalse(EventHubModel.modules(for: .polling).contains(.discussion))
        for phase in [EventHubFacts.Phase.confirmed, .comparing, .organizing, .finalized] {
            XCTAssertEqual(EventHubModel.modules(for: phase).last, .discussion, "\(phase)")
        }
    }

    func testDiscussionTileFollowsTheCommentsScreenGuard() {
        for phase in [EventHubFacts.Phase.confirmed, .comparing, .organizing, .finalized] {
            XCTAssertFalse(EventHubModel.isLocked(.discussion, facts: facts(phase: phase, hasDetailsAccess: true)), "\(phase)")
            XCTAssertTrue(EventHubModel.isLocked(.discussion, facts: facts(phase: phase, hasDetailsAccess: false)), "\(phase)")
        }
        for phase in [EventHubFacts.Phase.draft, .polling] {
            XCTAssertTrue(EventHubModel.isLocked(.discussion, facts: facts(phase: phase, hasDetailsAccess: true, isOrganizer: true)),
                          "\(phase) : l'écran des commentaires refuse l'accès avant la date retenue.")
        }
        let locked = EventHubModel(facts: facts(phase: .confirmed, hasDetailsAccess: false))
        XCTAssertEqual(locked.tiles.first { $0.module == .discussion }?.isLocked, true)
    }

    func testDiscussionRoutesToTheGeneralThreadWithAndWithoutTheRollout() {
        for rollout in [true, false] {
            for phase in [EventHubFacts.Phase.confirmed, .organizing, .finalized] {
                XCTAssertEqual(EventHubRouting.route(for: .discussion, phase: phase, invitationRollout: rollout), .discussion)
            }
        }
        XCTAssertFalse(EventHubRouting.sheetModules.contains(.discussion), "Le fil reste un écran plein.")
        XCTAssertNil(EventHubRouting.fullScreenFallback(for: .discussion))
    }

    func testDiscussionTileHasItsOwnHint() {
        let tile = EventHubModel.Tile(module: .discussion, isLocked: false, isHighlighted: false, status: nil)
        let f = facts(phase: .organizing, hasDetailsAccess: true)
        XCTAssertEqual(EventHubView.hintKey(for: .discussion), "hub.tile.discussion_hint")
        XCTAssertEqual(EventHubView.tileSummary(tile, facts: f, locale: fr), "Échange avec le groupe")
        XCTAssertEqual(EventHubView.tileSummary(tile, facts: f, locale: en), "Chat with the group")
        XCTAssertEqual(EventHubView.moduleTitle(.discussion, locale: fr), "Discussion")
    }

    /// Même route (et mêmes gardes) que la ligne « messages » de l'Activité et le lien profond `.event(.comments)`.
    func testHubRouteOpensTheGeneralCommentSection() throws {
        let source = try String(
            contentsOf: URL(fileURLWithPath: #filePath)
                .deletingLastPathComponent().deletingLastPathComponent()
                .appendingPathComponent("src/Views/App/ContentView.swift"),
            encoding: .utf8
        )
        guard let start = source.range(of: "private func performHubRoute(_ route: EventHubRoute, for event: Event)") else {
            return XCTFail("performHubRoute introuvable")
        }
        let body = String(source[start.lowerBound...].prefix(2600))
        guard let discussion = body.range(of: "case .discussion:") else { return XCTFail("case .discussion absent") }
        let branch = String(body[discussion.lowerBound...].prefix(300))
        XCTAssertTrue(branch.contains("selectedCommentSection = .general"))
        XCTAssertTrue(branch.contains("navigateInvitationDeepLink(eventId: event.id, destination: .comments)"))
    }
}
