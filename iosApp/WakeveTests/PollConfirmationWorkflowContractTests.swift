import XCTest
@testable import Wakeve

final class PollConfirmationWorkflowContractTests: XCTestCase {
    private let viewPath = "iosApp/src/Views/Polls/PollResultsView.swift"
    private let viewModelPath = "iosApp/src/ViewModels/PollConfirmationViewModel.swift"

    func testViewModelExposesEveryReviewedRenderState() throws {
        let source = readProjectFileIfPresent(viewModelPath)

        for state in [
            "reviewingResults",
            "confirmPrompt",
            "confirming",
            "failed",
            "pendingSync",
            "synced"
        ] {
            XCTAssertTrue(
                source.contains(state),
                "PollConfirmationViewModel must expose the reviewed \(state) render state."
            )
        }
    }

    func testViewUsesNativePromptAndCancelDispatchesNoConfirmationCommand() throws {
        let view = readProjectFileIfPresent(viewPath)
        let viewModel = readProjectFileIfPresent(viewModelPath)

        XCTAssertTrue(view.contains(".confirmationDialog("), "Confirmation must use the native iOS prompt.")
        XCTAssertTrue(view.contains("cancelConfirmation"), "The native cancel action must map to a typed UI callback.")
        XCTAssertTrue(viewModel.contains("case .cancelConfirmation"))
        XCTAssertTrue(viewModel.contains("state = .reviewingResults"))

        let cancelBranch = slice(viewModel, from: "case .cancelConfirmation", to: "case ")
        for forbiddenEffect in ["confirmPollDate(", "submitConfirmation(", "dispatchConfirmation(", "navigate(", "WakeveHaptics.success"] {
            XCTAssertFalse(
                cancelBranch.contains(forbiddenEffect),
                "Cancel must have zero command, navigation, or success-feedback dispatch."
            )
        }
    }

    func testConfirmingCoalescesDuplicateSubmit() throws {
        let source = readProjectFileIfPresent(viewModelPath)

        XCTAssertTrue(source.contains("operationInFlight"), "The adapter must track the single in-flight operation.")
        XCTAssertTrue(source.contains("guard !operationInFlight"), "A second submit must be coalesced before dispatch.")
        XCTAssertTrue(source.contains("isConfirmActionDisabled"), "The render projection must disable submit while confirming.")
        XCTAssertTrue(source.contains("state = .confirming"))
    }

    func testRetryReusesStableOperationIdAndSelectedSlot() throws {
        let source = readProjectFileIfPresent(viewModelPath)
        let retry = slice(source, from: "case .retryConfirmation", to: "case ")

        XCTAssertTrue(source.contains("operationId"))
        XCTAssertTrue(source.contains("selectedSlotId"))
        XCTAssertTrue(retry.contains("operationId"), "Retry must reuse the existing operation identifier.")
        XCTAssertTrue(retry.contains("selectedSlotId"), "Retry must reuse the staged slot.")
        XCTAssertFalse(
            retry.contains("UUID()"),
            "Retry must not generate a new operation identifier."
        )
    }

    func testFailureNeverNavigatesAndOnlyCommittedReceiptCanNavigate() throws {
        let source = readProjectFileIfPresent(viewModelPath)
        let failed = slice(source, from: "case .confirmationFailed", to: "case ")
        let committed = slice(source, from: "case .confirmationCommitted", to: "case ")

        XCTAssertTrue(source.contains("confirmationReceiptId"), "Navigation eligibility must derive from a durable receipt.")
        XCTAssertFalse(failed.contains("navigate"), "A failed or rolled-back confirmation must never navigate.")
        XCTAssertFalse(failed.contains("onDateConfirmed"), "Failure must not invoke the success route callback.")
        XCTAssertTrue(committed.contains("confirmationReceiptId"))
        XCTAssertTrue(
            committed.contains("navigate") || committed.contains("onDateConfirmed"),
            "Only the committed receipt branch may emit navigation."
        )
    }

    func testPendingSyncCopyDoesNotClaimRemoteSuccessOrParticipantDelivery() throws {
        let view = readProjectFileIfPresent(viewPath)

        XCTAssertTrue(view.contains("poll.results.confirmation.pending_sync.title"))
        XCTAssertTrue(view.contains("poll.results.confirmation.pending_sync.message"))

        for locale in ["en", "fr", "es", "it", "pt"] {
            let strings = readProjectFileIfPresent("iosApp/src/Resources/\(locale).lproj/Localizable.strings")
            let pendingLines = strings
                .split(separator: "\n")
                .filter { $0.contains("poll.results.confirmation.pending_sync") }
                .joined(separator: " ")
                .lowercased()

            XCTAssertTrue(pendingLines.contains("pending_sync.title"), "\(locale) is missing pending-sync title copy.")
            XCTAssertTrue(pendingLines.contains("pending_sync.message"), "\(locale) is missing pending-sync detail copy.")
            for forbiddenClaim in [
                "server confirmed", "participants notified", "participants received",
                "serveur confirmé", "participants notifiés", "participants ont reçu"
            ] {
                XCTAssertFalse(
                    pendingLines.contains(forbiddenClaim),
                    "Pending copy must describe a local save, not remote acknowledgement or participant delivery."
                )
            }
        }
    }

    func testEveryInteractiveAndStatusStateHasAnAccessibilityLabel() throws {
        let source = readProjectFileIfPresent(viewPath)

        for identifier in [
            "pollConfirmationConfirmButton",
            "pollConfirmationCancelButton",
            "pollConfirmationProgress",
            "pollConfirmationError",
            "pollConfirmationRetryButton",
            "pollConfirmationPendingSyncStatus"
        ] {
            XCTAssertTrue(source.contains(identifier), "Missing stable accessibility identifier: \(identifier)")
        }

        for key in [
            "poll.results.confirmation.accessibility.confirm",
            "poll.results.confirmation.accessibility.cancel",
            "poll.results.confirmation.accessibility.progress",
            "poll.results.confirmation.accessibility.error",
            "poll.results.confirmation.accessibility.retry",
            "poll.results.confirmation.accessibility.pending_sync"
        ] {
            XCTAssertTrue(source.contains(key), "Missing localized accessibility label: \(key)")
        }
    }

    private func readProjectFileIfPresent(_ relativePath: String) -> String {
        let fileURL = URL(fileURLWithPath: #filePath)
        let testsDir = fileURL.deletingLastPathComponent()
        let iosAppDir = testsDir.deletingLastPathComponent()
        let projectRoot = iosAppDir.deletingLastPathComponent()
        let targetURL = projectRoot.appendingPathComponent(relativePath)
        return (try? String(contentsOf: targetURL, encoding: .utf8)) ?? ""
    }

    private func slice(_ source: String, from startMarker: String, to endMarker: String) -> String {
        guard let start = source.range(of: startMarker)?.lowerBound else {
            return ""
        }

        let tail = source[start...]
        guard let end = tail.dropFirst(startMarker.count).range(of: endMarker)?.lowerBound else {
            return String(tail)
        }

        return String(tail[..<end])
    }
}
