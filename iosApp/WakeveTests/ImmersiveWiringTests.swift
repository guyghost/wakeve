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

/// Entrées du jour J (couche 8, #47).
final class EventDayWiringTests: XCTestCase {
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

    func testEventDayIsAFullScreenCoverAfterTheExistingPresentations() throws {
        let source = try contentView
        XCTAssertTrue(source.contains("@State private var eventDayPresentation: EventDayPresentation?"))
        let studio = try XCTUnwrap(source.range(of: ".sheet(item: $invitationStudioPreview)"))
        let cover = try XCTUnwrap(source.range(of: ".fullScreenCover(item: $eventDayPresentation)"))
        XCTAssertLessThan(studio.lowerBound, cover.lowerBound, "Tranches des tests d'ancrage intactes.")
        let block = slice(source, from: ".fullScreenCover(item: $eventDayPresentation)", to: ".confirmationDialog(")
        XCTAssertTrue(block.contains("EventDayContainer("))
        XCTAssertTrue(block.contains("viewEventFromEventDay("))
    }

    func testEventDayOpensOnlyUnderTheRedesign() throws {
        let source = try contentView
        let open = slice(source, from: "private func openEventDay(_ eventId: String)", to: "private func viewEventFromEventDay(")
        XCTAssertTrue(open.contains("guard iosRedesign2026 else"))
        XCTAssertTrue(open.contains("eventDayPresentation = EventDayPresentation(id: eventId)"))
        let view = slice(source, from: "private func viewEventFromEventDay(", to: "/// Sheet présentée")
        XCTAssertTrue(view.contains("eventDayPresentation = nil"))
        XCTAssertTrue(view.contains("openEventFromHome(eventId)"), "« Voir l'événement » → hub, même chemin que l'accueil.")
    }

    func testHubBannerAndHomeNextStepOpenTheEventDay() throws {
        let source = try contentView
        let hub = String(source[try XCTUnwrap(source.range(of: "private func eventHubContent(for event: Event)")).lowerBound...].prefix(2500))
        XCTAssertTrue(hub.contains("onOpenEventDay: { openEventDay(event.id) }"))
        let action = slice(source, from: "private func openHomeAction(_ action: HomeNextStep.Action, eventId: String)", to: "// MARK: - Activité de la refonte")
        XCTAssertEqual(action.components(separatedBy: "case .eventDay: openEventDay(eventId)").count - 1, 2,
                       "Avec et sans rollout invitation.")
        let home = slice(source, from: "EventsHomeContainer(", to: "} else if invitationExperienceRolloutEnabled")
        XCTAssertTrue(home.contains("invitationRollout: invitationExperienceRolloutEnabled"))
    }
}
