import XCTest
import Shared
@testable import Wakeve

/// Branchement du flux de création dans `AuthenticatedView` (couche 7, #47).
@MainActor
final class CreateEventFlowWiringTests: XCTestCase {

    // MARK: - Aiguillage des brouillons

    func testFlowDraftsReopenInTheFlowAndStudioDraftsKeepTheirPath() {
        XCTAssertEqual(CreateFlowEntry.draftRoute(status: .draft, planningMode: .timeSlotPoll, hasInvitationReceipt: false), .createFlow)
        XCTAssertEqual(CreateFlowEntry.draftRoute(status: .draft, planningMode: .timeSlotPoll, hasInvitationReceipt: true), .existing,
                       "Un brouillon du studio (reçu d'invitation) se rouvre dans le studio.")
        XCTAssertEqual(CreateFlowEntry.draftRoute(status: .polling, planningMode: .timeSlotPoll, hasInvitationReceipt: false), .existing,
                       "Seul un brouillon s'édite dans le flux.")
        XCTAssertEqual(CreateFlowEntry.draftRoute(status: nil, planningMode: nil, hasInvitationReceipt: false), .existing)
    }

    func testOnlyPollDraftsReopenInTheFlow() {
        XCTAssertEqual(CreateFlowEntry.draftRoute(status: .draft, planningMode: .scenarioMatrix, hasInvitationReceipt: false), .existing,
                       "Un brouillon matrice garde son chemin : le flux ne crée que des sondages de créneaux.")
        XCTAssertEqual(CreateFlowEntry.draftRoute(status: .draft, planningMode: nil, hasInvitationReceipt: false), .existing)
    }

    func testInvitationReceiptIsTheStudioMarker() async throws {
        let database = RepositoryProvider.shared.database
        let controller = EventDraftFlowController(userId: "flow-wiring-\(UUID().uuidString.prefix(8))")
        var form = CreateEventForm()
        form.title = "Apéro"
        form.description = "Sur le toit"
        _ = await controller.save(step: .what, form: form)
        let eventId = try XCTUnwrap(controller.eventId)
        XCTAssertFalse(CreateFlowEntry.hasInvitationReceipt(eventId: eventId, database: database))

        let now = ISO8601DateFormatter().string(from: Date())
        database.invitationExperienceQueries.insertEventOperationReceipt(
            operation_id: "op-\(UUID().uuidString)",
            event_id: eventId,
            actor_id: "studio",
            action: "UPDATE_DRAFT_AGGREGATE",
            aggregate_revision: 1,
            request_fingerprint: "",
            durable_operation_ref: "",
            commit_envelope: "",
            server_receipt_id: nil,
            status: "PENDING_SYNC",
            created_at: now,
            updated_at: now
        )
        XCTAssertTrue(CreateFlowEntry.hasInvitationReceipt(eventId: eventId, database: database))
    }

    // MARK: - AuthenticatedView

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

    func testFlowCoverIsAddedAfterTheNotificationPreferencesSheet() throws {
        let source = try contentView
        XCTAssertTrue(source.contains("@State private var showCreateEventFlow = false"))
        XCTAssertTrue(source.contains("@State private var createFlowDraftId: String?"))
        let notification = try XCTUnwrap(source.range(of: ".sheet(isPresented: $showNotificationPreferencesSheet)"))
        let cover = try XCTUnwrap(source.range(of: ".fullScreenCover(isPresented: $showCreateEventFlow)"))
        XCTAssertLessThan(notification.lowerBound, cover.lowerBound, "Tranches des tests d'ancrage intactes.")
        let block = slice(source, from: ".fullScreenCover(isPresented: $showCreateEventFlow)", to: ".sheet(item: $invitationStudioPreview)")
        XCTAssertTrue(block.contains("CreateEventFlow("))
        XCTAssertTrue(block.contains("draftEventId: createFlowDraftId"))
        XCTAssertTrue(block.contains("finishCreateEventFlow(event, context: context)"))
    }

    // MARK: - ＋ (nouvel événement)

    /// Décision du 2026-10-02 : le flux ne synchronise pas encore les événements (Swarm DAO #48) ;
    /// avec le rollout invitation, le studio (synchronisé) reste le point d'entrée.
    func testNewEventRouteFollowsTheInvitationRollout() {
        XCTAssertEqual(CreateFlowEntry.newEventRoute(redesign: true, invitationRollout: false), .createFlow)
        XCTAssertEqual(CreateFlowEntry.newEventRoute(redesign: true, invitationRollout: true), .studio,
                       "Rollout invitation allumé : le studio reste le point d'entrée (#48).")
        XCTAssertEqual(CreateFlowEntry.newEventRoute(redesign: false, invitationRollout: true), .studio)
        XCTAssertEqual(CreateFlowEntry.newEventRoute(redesign: false, invitationRollout: false), .legacySheet)
    }

    func testPlusFollowsTheNewEventRoute() throws {
        let source = try contentView
        let begin = slice(source, from: "private func beginRedesignEventCreation()", to: "private func openCreateEventFlow(")
        XCTAssertTrue(begin.contains("CreateFlowEntry.newEventRoute("))
        XCTAssertTrue(begin.contains("redesign: iosRedesign2026"))
        XCTAssertTrue(begin.contains("invitationRollout: invitationExperienceRolloutEnabled"))
        XCTAssertTrue(begin.contains("openCreateEventFlow(draftEventId: nil)"))
        XCTAssertTrue(begin.contains("currentView = .eventCreation"), "Rollout allumé : studio, comme avant la couche 7.")
        XCTAssertTrue(begin.contains("selectedCreationBaseRevision = nil"))
        XCTAssertTrue(begin.contains("showEventCreationSheet = true"))

        let deepLink = slice(source, from: "case .eventCreate:", to: "case .event(.detail")
        XCTAssertTrue(deepLink.contains("currentView = .eventCreation"), "Lien profond `.eventCreate` inchangé.")
        XCTAssertTrue(deepLink.contains("showEventCreationSheet = true"))
    }

    func testHomeEmptyStateCreateUsesThePlusRoute() throws {
        let source = try contentView
        let home = slice(source, from: "EventsHomeContainer(", to: "onEditDraft:")
        XCTAssertTrue(home.contains("onCreate: { beginRedesignEventCreation() }"))
    }

    func testEditDraftRoutesFlowDraftsToTheFlow() throws {
        let source = try contentView
        let edit = slice(source, from: "private func editDraftFromHome(_ id: String)", to: "// MARK: - Hub d'événement de la refonte")
        XCTAssertTrue(edit.contains("CreateFlowEntry.draftRoute("))
        XCTAssertTrue(edit.contains("planningMode: draft?.planningMode"), "Le mode de planification décide aussi.")
        XCTAssertTrue(edit.contains("CreateFlowEntry.hasInvitationReceipt("))
        XCTAssertTrue(edit.contains("openCreateEventFlow(draftEventId: id)"))
        XCTAssertTrue(edit.contains("InvitationExperienceRouteRequestCanvasAction(action: .editDraft)"),
                      "Les brouillons du studio gardent le chemin actuel.")
    }

    func testLaunchOpensTheHubAndKeepsTheTemplateChecklist() throws {
        let source = try contentView
        let finish = slice(source, from: "private func finishCreateEventFlow(", to: "private func closeCreateEventFlow(")
        XCTAssertTrue(finish.contains("persistCreationContext(context, for: event)"))
        XCTAssertTrue(finish.contains("selectedEvent = event"))
        XCTAssertTrue(finish.contains("currentView = .eventDetail"))
        XCTAssertTrue(finish.contains("showCreateEventFlow = false"))
        XCTAssertTrue(finish.contains("eventsHomeReloadToken += 1"))
        let close = slice(source, from: "private func closeCreateEventFlow(", to: "// MARK: - Accueil de la refonte")
        XCTAssertTrue(close.contains("eventsHomeReloadToken += 1"), "Le brouillon apparaît à l'accueil après fermeture.")
    }
}
