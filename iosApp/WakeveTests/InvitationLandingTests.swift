import XCTest
import SwiftUI
@testable import Wakeve

/// Invitation reçue en mode immersif (couche 8, #47) : présentation pure, textes, rendu et chargement.
@MainActor
final class InvitationLandingTests: XCTestCase {
    private let fr = Locale(identifier: "fr_FR")

    private func hub(
        phase: EventHubFacts.Phase = .polling,
        isOrganizer: Bool = false,
        viewerAccepted: Bool = true,
        userBallotComplete: Bool = false,
        slotCount: Int = 3,
        finalDate: Date? = nil,
        confirmed: Int = 3,
        pending: Int = 2,
        eventTypeName: String? = "BIRTHDAY"
    ) -> EventHubFacts {
        EventHubFacts(
            id: "e1", title: "Anniversaire de Léa", phase: phase,
            isOrganizer: isOrganizer, viewerAccepted: viewerAccepted || isOrganizer,
            hasDetailsAccess: isOrganizer, isLocalGuest: false,
            pollOpen: true, userBallotComplete: userBallotComplete, ballotsKnown: true,
            votersWithCompleteBallot: 1, otherEligibleVoters: 4, otherVotersComplete: 1,
            slotCount: slotCount, leadingSlotStart: nil, finalDate: finalDate,
            confirmedCount: confirmed, pendingCount: pending, participantNames: ["Léa"], summaries: [:],
            eventTypeName: eventTypeName, organizerId: "org"
        )
    }

    private func model(_ hub: EventHubFacts, organizer: String? = "Léa", response: InvitationLandingFacts.Response = .accepted) -> InvitationLandingModel {
        InvitationLandingModel(facts: InvitationLandingFacts(hub: hub, organizerName: organizer, response: response), locale: fr)
    }

    // MARK: - Présentation

    func testCaptionNamesTheOrganizerWithoutGenderedWording() {
        XCTAssertEqual(model(hub()).caption, "Invitation de Léa")
        XCTAssertEqual(model(hub(), organizer: nil).caption, "Invitation")
        XCTAssertEqual(model(hub(), organizer: "  ").caption, "Invitation")
    }

    func testWhenShowsTheVoteInProgressOrTheRetainedDate() {
        XCTAssertEqual(model(hub(phase: .polling, slotCount: 3)).when, "Vote en cours · 3 créneaux")
        XCTAssertNil(model(hub(phase: .polling, slotCount: 0)).when)
        let date = ISO8601DateFormatter().date(from: "2026-10-17T18:00:00Z")!
        XCTAssertEqual(model(hub(phase: .confirmed, finalDate: date)).when, HomeDateText.short(date, locale: fr))
        XCTAssertNil(model(hub(phase: .draft, slotCount: 2)).when)
    }

    func testParticipantsUseTheHubCounts() {
        XCTAssertEqual(model(hub(confirmed: 3, pending: 2)).participants,
                       HubSummaryText.participants(confirmed: 3, pending: 2, locale: fr))
        XCTAssertNil(model(hub(confirmed: 0, pending: 0)).participants)
    }

    func testResponseStateIsOnlyDisplayed() {
        XCTAssertEqual(model(hub(), response: .accepted).response, "Tu as accepté")
        XCTAssertEqual(model(hub(), response: .pending).response, "Ta réponse est attendue")
        XCTAssertEqual(model(hub(), response: .declined).response, "Tu as décliné")
        XCTAssertEqual(model(hub(isOrganizer: true), response: .organizer).response, "Tu organises")
    }

    func testResponseFollowsTheViewerRole() {
        XCTAssertEqual(InvitationLandingFacts.response(isOrganizer: true, accepted: false, declined: true), .organizer)
        XCTAssertEqual(InvitationLandingFacts.response(isOrganizer: false, accepted: true, declined: false), .accepted)
        XCTAssertEqual(InvitationLandingFacts.response(isOrganizer: false, accepted: false, declined: true), .declined)
        XCTAssertEqual(InvitationLandingFacts.response(isOrganizer: false, accepted: false, declined: false), .pending)
    }

    /// Mêmes règles que le hub : « Voter » quand le vote est attendu, sinon « Voir l'événement ».
    func testPrimaryFollowsTheHubRules() {
        let vote = model(hub(phase: .polling, viewerAccepted: true, userBallotComplete: false))
        XCTAssertEqual(EventHubModel(facts: hub(phase: .polling)).primary, .vote)
        XCTAssertEqual(vote.primary, .vote)
        XCTAssertEqual(vote.primaryTitle, "Voter")
        let voted = model(hub(phase: .polling, userBallotComplete: true))
        XCTAssertEqual(voted.primary, .viewEvent)
        XCTAssertEqual(voted.primaryTitle, "Voir l'événement")
        XCTAssertEqual(model(hub(phase: .polling, viewerAccepted: false), response: .pending).primary, .viewEvent,
                       "Invitation non acceptée : aucun vote (pas d'Accepter/Décliner côté client).")
        XCTAssertEqual(model(hub(phase: .organizing)).primary, .viewEvent)
    }

    func testMoodFollowsTheEventType() {
        XCTAssertEqual(model(hub(eventTypeName: "BIRTHDAY")).palette, .palette(for: "BIRTHDAY"))
        XCTAssertEqual(model(hub(eventTypeName: nil)).palette, .weekend)
    }

    // MARK: - Textes

    static let keys = [
        "immersive.invitation.caption", "immersive.invitation.caption_format", "immersive.invitation.polling_format",
        "immersive.invitation.response.organizer", "immersive.invitation.response.accepted",
        "immersive.invitation.response.pending", "immersive.invitation.response.declined",
        "immersive.view_event", "hub.primary.vote", "common.close", "common.error_generic"
    ]

    private var resources: URL {
        URL(fileURLWithPath: #filePath).deletingLastPathComponent().deletingLastPathComponent()
            .appendingPathComponent("src/Resources")
    }

    func testInvitationKeysExistInEveryLanguage() throws {
        for locale in ["en", "fr", "es", "it", "pt"] {
            let strings = try String(contentsOf: resources.appendingPathComponent("\(locale).lproj/Localizable.strings"), encoding: .utf8)
            for key in Self.keys {
                XCTAssertTrue(strings.contains("\"\(key)\" ="), "\(key) manquante (\(locale))")
            }
        }
    }

    func testFrenchImmersiveCopyUsesTutoiement() throws {
        let strings = try String(contentsOf: resources.appendingPathComponent("fr.lproj/Localizable.strings"), encoding: .utf8)
        let lines = strings.split(separator: "\n").filter { $0.hasPrefix("\"immersive.") }
        XCTAssertFalse(lines.isEmpty)
        for line in lines {
            let value = line.split(separator: "=", maxSplits: 1).last.map(String.init) ?? ""
            for word in [" vous ", "Vous ", " votre ", "Votre ", " vos ", "Vos "] {
                XCTAssertFalse(value.contains(word), "Tutoiement attendu : \(line)")
            }
        }
    }

    // MARK: - Rendu

    func testLandingFitsA375ptScreenAtAX5() {
        let view = InvitationLandingView(model: model(hub()), artwork: nil, onPrimary: {}, onClose: {})
        let size = fittingSize(view.frame(height: 812), width: 375, dynamicType: .accessibility5)
        XCTAssertLessThanOrEqual(size.width, 375, "\(size)")
    }

    func testLandingExposesStableAccessibilityIdentifiers() {
        XCTAssertEqual(InvitationLandingView.accessibilityID, "immersive.invitation")
        XCTAssertEqual(InvitationLandingView.titleAccessibilityID, "immersive.invitation.title")
    }

    // MARK: - Chargement

    private struct FakeSource: InvitationLandingSource {
        let result: Result<InvitationLandingFacts, Error>
        func loadLanding(eventId: String, viewerId: String, isLocalGuest: Bool) async throws -> InvitationLandingFacts {
            try result.get()
        }
    }

    func testViewModelPublishesTheModelOrAFailure() async {
        let facts = InvitationLandingFacts(hub: hub(), organizerName: "Léa", response: .accepted)
        let loaded = InvitationLandingViewModel(eventId: "e1", viewerId: "v", isLocalGuest: false, source: FakeSource(result: .success(facts)))
        XCTAssertEqual(loaded.state, .loading)
        await loaded.reload()
        XCTAssertEqual(loaded.state, .loaded)
        XCTAssertEqual(loaded.model?.title, "Anniversaire de Léa")

        struct Boom: Error {}
        let failed = InvitationLandingViewModel(eventId: "e1", viewerId: "v", isLocalGuest: false, source: FakeSource(result: .failure(Boom())))
        await failed.reload()
        XCTAssertEqual(failed.state, .failed)
        XCTAssertNil(failed.model)
    }
}
