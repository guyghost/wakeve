import XCTest
import Shared
@testable import Wakeve

/// Regression tests for the P0 blockers found during the iOS QA tour of 2026-09-27
/// (docs/qa/IOS_QA_TOUR_2026-09-27.md).
@MainActor
final class QABlockersRegressionTests: XCTestCase {

    // MARK: - IOS-3: event creation must never leave the wizard stuck

    // Couche 9 : les tests de `CreateEventViewModel` et de l'ancienne feuille sont supprimés avec elles.

    func testDraftWithoutDatesOffersToAddDatesBeforeThePoll() throws {
        let participants = readProjectFileIfPresent("iosApp/src/Views/Events/ParticipantManagementView.swift")
        let controller = readProjectFileIfPresent("iosApp/src/ViewModels/EventDraftDatesController.swift")
        let pollStart = readProjectFileIfPresent("iosApp/src/ViewModels/EventPollStartController.swift")

        XCTAssertTrue(participants.contains("needsDatesBeforePoll"))
        XCTAssertTrue(participants.contains("DraftDatesSheet("))
        XCTAssertTrue(controller.contains("EventManagementContractIntentUpdateEvent"), "Dates are saved through the shared state machine")
        XCTAssertTrue(pollStart.contains("POLL_REQUIRES_TIME_SLOT_MESSAGE"), "The missing-date failure is explained, not shown raw")
    }

    // MARK: - IOS-2: organizers can reach ORGANIZING and FINALIZED

    func testEventHubDispatchesLifecycleTransitions() throws {
        let controller = readProjectFileIfPresent("iosApp/src/ViewModels/EventLifecycleTransitionController.swift")
        let contentView = readProjectFileIfPresent("iosApp/src/Views/App/ContentView.swift")

        XCTAssertTrue(controller.contains("EventManagementContractIntentTransitionToOrganizing"))
        XCTAssertTrue(controller.contains("EventManagementContractIntentMarkAsFinalized"))
        // Réancré sur le hub (couche 9) : le détail legacy est supprimé.
        let hub = readProjectFileIfPresent("iosApp/src/Views/Hub/EventHubView.swift")
        XCTAssertTrue(hub.contains("EventLifecycleTransitionController("))
        XCTAssertTrue(hub.contains("onLifecycleChanged()"), "The hub must notify its parent after a transition")
        XCTAssertTrue(contentView.contains("onLifecycleChanged:"), "The parent must reload the event after a transition")
    }

    func testFinalizationBlockersAreExplainedInPlainLanguage() {
        let message = EventLifecycleBlockerFormatter.message(
            for: "Finalization blocked by MEETING_REQUIRED,LODGING_REQUIRED"
        )

        XCTAssertTrue(message.contains(String(localized: "event.lifecycle.blocker.meeting_required")))
        XCTAssertTrue(message.contains(String(localized: "event.lifecycle.blocker.lodging_required")))
        XCTAssertFalse(message.contains("MEETING_REQUIRED"), "Raw blocker codes must never reach the user")
    }

    func testUnknownLifecycleFailureFallsBackToALocalizedMessage() {
        XCTAssertEqual(
            EventLifecycleBlockerFormatter.message(for: "Failed to finalize event"),
            String(localized: "event.lifecycle.error.generic")
        )
    }

    // MARK: - Helpers

    private func readProjectFileIfPresent(_ relativePath: String) -> String {
        let fileURL = URL(fileURLWithPath: #filePath)
        let runtimeURL = URL(fileURLWithPath: FileManager.default.currentDirectoryPath)
        for startURL in [fileURL.deletingLastPathComponent(), runtimeURL] {
            var candidateRoot = startURL
            for _ in 0..<8 {
                let targetURL = candidateRoot.appendingPathComponent(relativePath)
                if let source = try? String(contentsOf: targetURL, encoding: .utf8) {
                    return source
                }
                let parentURL = candidateRoot.deletingLastPathComponent()
                guard parentURL.path != candidateRoot.path else { break }
                candidateRoot = parentURL
            }
        }
        return ""
    }

    private func slice(_ source: String, from start: String, to end: String) -> String {
        guard let startRange = source.range(of: start) else { return "" }
        let tail = source[startRange.upperBound...]
        guard let endRange = tail.range(of: end) else { return String(tail) }
        return String(tail[..<endRange.lowerBound])
    }
}
