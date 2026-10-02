import XCTest
import Shared
@testable import Wakeve

/// Regression tests for the P0 blockers found during the iOS QA tour of 2026-09-27
/// (docs/qa/IOS_QA_TOUR_2026-09-27.md).
@MainActor
final class QABlockersRegressionTests: XCTestCase {

    // MARK: - IOS-3: event creation must never leave the wizard stuck

    private let oneSlot = [
        EventTimeSlotInput(start: "2026-10-17T08:00:00Z", end: "2026-10-17T20:00:00Z", timeOfDay: .allDay)
    ]

    private func waitUntilCreationSettles(_ viewModel: CreateEventViewModel, file: StaticString = #filePath, line: UInt = #line) async {
        let deadline = Date().addingTimeInterval(5)
        while viewModel.isCreating && Date() < deadline {
            try? await Task.sleep(nanoseconds: 50_000_000)
        }
        XCTAssertFalse(viewModel.isCreating, "Creation must settle instead of spinning forever", file: file, line: line)
    }

    func testDraftWithoutDatesIsCreated() async {
        // "Date à décider avec le groupe": a DRAFT may exist before any date is known.
        let viewModel = CreateEventViewModel()
        var created: WakeveEvent?
        viewModel.onEventCreated = { created = $0 }

        viewModel.createEvent(title: "QA watch party sans date", description: "", userId: "qa-organizer")

        await waitUntilCreationSettles(viewModel)
        XCTAssertNil(viewModel.creationErrorMessage)
        XCTAssertEqual(created?.status, .draft)
        XCTAssertEqual(created?.proposedSlots.isEmpty, true)
    }

    func testRepeatedCreationFailureAlwaysSettles() async {
        let viewModel = CreateEventViewModel()

        // A blank organizer is rejected by the shared CreateEventUseCase.
        for attempt in 1...2 {
            viewModel.createEvent(title: "Watch party \(attempt)", description: "", userId: "", selectedSlots: oneSlot)
            await waitUntilCreationSettles(viewModel)
            XCTAssertNotNil(viewModel.creationErrorMessage, "Attempt \(attempt) must surface its failure")
        }
    }

    func testWizardKeepsDatesOptionalAndSurfacesCreationFailures() throws {
        let sheet = readProjectFileIfPresent("iosApp/src/Views/Events/CreateEventSheet.swift")
        let canAdvance = slice(sheet, from: "private var canAdvanceStep", to: "private func advanceCreateStep")
        let dateCase = slice(canAdvance, from: "case .date:", to: "case .place")

        XCTAssertTrue(dateCase.contains("return true"), "Dates stay optional in the wizard (DRAFT without dates)")
        XCTAssertTrue(sheet.contains("creationErrorMessage"), "The sheet must surface creation failures")
    }

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
