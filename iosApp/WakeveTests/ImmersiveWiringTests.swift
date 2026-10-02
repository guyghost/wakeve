import XCTest
import Shared
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
        XCTAssertTrue(detail.contains("if let event = selectedEvent {"))
        let redesign = slice(detail, from: "if let event = selectedEvent {", to: "} else if let event = selectedEvent")
        let flat = redesign.split(whereSeparator: \.isWhitespace).joined(separator: " ")
        XCTAssertTrue(flat.contains(
            "if InvitationLandingRoute.showsLanding( marker: invitationLandingEventId, eventId: event.id, organizerId: event.organizerId, viewerId: userId ) {"
        ), "Jamais à l'organisateur (règle pure testée).")
        XCTAssertTrue(redesign.contains("invitationLandingContent(for: event)"))
        XCTAssertTrue(redesign.contains("eventHubContent(for: event)"), "Sinon, le hub.")
        // Organisateur marqué : le hub efface le marqueur.
        XCTAssertTrue(redesign.contains(".task(id: invitationLandingEventId)"))
        XCTAssertTrue(flat.contains("if invitationLandingEventId == event.id { invitationLandingEventId = nil }"))
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

    /// Marqueur périmé (revue I1) : effacé quand la navigation quitte le détail sans l'afficher, ou ouvre
    /// un autre événement (accueil, Activité, jour J), sans toucher la résolution du lien.
    func testStaleLandingMarkerIsClearedOnNavigation() throws {
        let source = try contentView
        let squashed = source.split(whereSeparator: \.isWhitespace).joined(separator: " ")
        XCTAssertTrue(squashed.contains(
            ".onChange(of: currentView) { _, view in invitationLandingEventId = InvitationLandingRoute.marker(invitationLandingEventId, showingDetail: view == .eventDetail) }"
        ))
        let home = slice(source, from: "private func openEventFromHome(_ id: String)", to: "private func handleHomeNextStep(")
        XCTAssertTrue(home.contains("invitationLandingEventId = InvitationLandingRoute.marker(invitationLandingEventId, opening: id)"))
        let action = slice(source, from: "private func openHomeAction(_ action: HomeNextStep.Action, eventId: String)", to: "// MARK: - Activité de la refonte")
        XCTAssertTrue(action.contains("invitationLandingEventId = InvitationLandingRoute.marker(invitationLandingEventId, opening: eventId)"))
        let day = slice(source, from: "private func openEventDay(_ eventId: String)", to: "private func viewEventFromEventDay(")
        XCTAssertTrue(day.contains("invitationLandingEventId = InvitationLandingRoute.marker(invitationLandingEventId, opening: eventId)"))
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

    func testEventDayOpensFromTheShell() throws {
        let source = try contentView
        let open = slice(source, from: "private func openEventDay(_ eventId: String)", to: "private func viewEventFromEventDay(")
        XCTAssertFalse(open.contains("guard"), "Plus de flag de refonte à vérifier (couche 9).")
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

/// Argument de lancement QA de l'invitation reçue (couche 8) : DEBUG seulement.
final class InvitationLandingQALaunchTests: XCTestCase {
    func testLandingLaunchArgumentIsDebugOnly() throws {
        let source = try String(contentsOf: URL(fileURLWithPath: #filePath).deletingLastPathComponent()
            .deletingLastPathComponent().appendingPathComponent("src/Views/App/ContentView.swift"), encoding: .utf8)
        let argument = try XCTUnwrap(source.range(of: "\"--wakeve-qa-open-invitation-landing\""))
        let before = source[..<argument.lowerBound]
        let lastIf = try XCTUnwrap(before.range(of: "#if DEBUG", options: .backwards))
        XCTAssertNil(before[lastIf.upperBound...].range(of: "#endif"), "L'argument vit dans un bloc #if DEBUG.")
        let call = try XCTUnwrap(source.range(of: "openQAInvitationLandingIfRequested()\n"))
        let beforeCall = source[..<call.lowerBound]
        let callIf = try XCTUnwrap(beforeCall.range(of: "#if DEBUG", options: .backwards))
        XCTAssertNil(beforeCall[callIf.upperBound...].range(of: "#endif"))
        XCTAssertEqual(source.components(separatedBy: "\"--wakeve-qa-open-invitation-landing\"").count - 1, 1)
    }

#if DEBUG
    /// Seed QA (revue couche 8) : une invitation reçue de Noé Bernard, réponse du spectateur en attente,
    /// pour vérifier l'invitation d'un invité non organisateur (« Ta réponse est attendue »).
    @MainActor
    func testSeedAddsAnInvitationReceivedFromThePendingParticipant() async throws {
        let database = RepositoryProvider.shared.database
        let repository = RepositoryProvider.shared.databaseRepository
        let viewerId = "wakeve-debug-user"
        let support = InvitationExperienceQALaunchSupport(database: database, eventRepository: repository)
        let route = await support.prepare(
            arguments: [
                InvitationExperienceQALaunchSupport.seedArgument,
                InvitationExperienceQALaunchSupport.openRouteArgument,
                "library"
            ],
            viewerId: viewerId
        )
        XCTAssertEqual(route, .library, "Le seed existant reste complet.")
        let eventId = InvitationExperienceQALaunchSupport.receivedInvitationEventId
        let event = try XCTUnwrap(repository.getEvent(id: eventId))
        XCTAssertEqual(event.organizerId, "qa-invitation-guest-pending")
        XCTAssertEqual(event.status, .polling)
        let viewer = (repository.getParticipantRecords(eventId: eventId) ?? []).first { $0.userId == viewerId }
        XCTAssertEqual(viewer?.rsvp, "PENDING")

        let facts = try await SharedInvitationLandingSource().loadLanding(eventId: eventId, viewerId: viewerId, isLocalGuest: false)
        XCTAssertEqual(facts.organizerName, "Noé Bernard")
        XCTAssertEqual(facts.response, .pending)
        XCTAssertTrue(InvitationLandingRoute.showsLanding(
            marker: eventId, eventId: eventId, organizerId: event.organizerId, viewerId: viewerId
        ))
    }
#endif
}
