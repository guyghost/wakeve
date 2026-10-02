import XCTest
@testable import Wakeve

/// Branchement du mode immersif dans `AuthenticatedView` (couche 8, #47).
final class ImmersiveWiringTests: XCTestCase {
    private var contentView: String {
        get throws {
            try String(contentsOf: URL(fileURLWithPath: #filePath).deletingLastPathComponent()
                .deletingLastPathComponent().appendingPathComponent("src/Views/App/ContentView.swift"), encoding: .utf8)
        }
    }

    private func slice(_ source: String, from start: String, to end: String) -> String {
        guard let lower = source.range(of: start),
              let upper = source.range(of: end, range: lower.upperBound..<source.endIndex) else { return "" }
        return String(source[lower.lowerBound..<upper.lowerBound])
    }

    // MARK: - Invitation reçue

    func testRedesignShowsTheLandingInsteadOfTheHubForTheInvitedEvent() throws {
        let source = try contentView
        let detail = slice(source, from: "case .eventDetail:", to: "case .eventAudience:")
        XCTAssertTrue(detail.contains("if iosRedesign2026, let event = selectedEvent"))
        let redesign = slice(detail, from: "if iosRedesign2026, let event = selectedEvent", to: "} else if let event = selectedEvent")
        XCTAssertTrue(redesign.contains("if invitationLandingEventId == event.id"))
        XCTAssertTrue(redesign.contains("invitationLandingContent(for: event)"))
        XCTAssertTrue(redesign.contains("eventHubContent(for: event)"), "Sinon, le hub.")
        XCTAssertTrue(detail.contains("isInvitationLanding: invitationLandingEventId == event.id"), "Détail legacy inchangé.")
    }

    func testLandingActionsClearTheLandingThroughTheirOwnPath() throws {
        let source = try contentView
        let landing = slice(source, from: "private func invitationLandingContent(for event: Event)", to: "/// Sheet présentée")
        XCTAssertTrue(landing.contains("InvitationLandingContainer("))
        XCTAssertTrue(landing.contains("onViewEvent: { invitationLandingEventId = nil }"), "« Voir l'événement » → hub.")
        XCTAssertTrue(landing.contains("handleHubPrimary(.vote, for: event)"), "« Voter » : même route que le hub.")
        XCTAssertTrue(landing.contains("currentView = .eventList"), "Fermer : retour à la liste.")
        XCTAssertTrue(landing.contains("invitationExperienceProjectionRepository.artwork(eventId: event.id)"))
        // Le retour du hub garde son texte exact (ancré par HubModuleSheetViewTests).
        let squashed = source.split(whereSeparator: \.isWhitespace).joined(separator: " ")
        XCTAssertTrue(squashed.contains("onBack: { dismissHubModuleSheet() invitationLandingEventId = nil currentView = .eventList }"))
    }

    func testDeepLinkResolutionIsUntouched() throws {
        let source = try contentView
        let resolve = slice(source, from: "private func resolveInvitationDeepLink(token: String) async", to: "private func navigateToEvent(")
        XCTAssertTrue(resolve.contains("invitationLandingEventId = eventId"))
        XCTAssertTrue(resolve.contains("navigateInvitationDeepLink(eventId: eventId, action: .showDetails)"))
        XCTAssertFalse(resolve.contains("InvitationLanding"))
    }
}
