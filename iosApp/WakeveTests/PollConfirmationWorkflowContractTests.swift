import XCTest
@testable import Wakeve

final class PollConfirmationWorkflowContractTests: XCTestCase {
    private let viewPath = "iosApp/src/Views/Polls/PollResultsView.swift"
    private let viewModelPath = "iosApp/src/ViewModels/PollConfirmationViewModel.swift"
    private let actionButtonPath = "iosApp/src/Components/DesignSystem/WakeveDesignSystemComponents.swift"

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

    func testStartupRehydratesDurableConfirmationWithoutReplayingConfirmationEffects() throws {
        let source = readProjectFileIfPresent(viewModelPath)
        let initialization = slice(source, from: "init(", to: "deinit")
        let rehydration = slice(source, from: "private func rehydrateConfirmation()", to: "private func ")

        XCTAssertTrue(
            initialization.contains("rehydrateConfirmation()"),
            "Composition must load durable confirmation state when PollConfirmationViewModel starts."
        )
        XCTAssertTrue(
            rehydration.contains("repository.loadConfirmationProjection(eventId: event.id)"),
            "Startup must obtain the durable projection from the application-scoped repository."
        )
        XCTAssertTrue(
            rehydration.contains("EventManagementContractIntentRehydrateConfirmation"),
            "Startup must dispatch the typed RehydrateConfirmation intent."
        )

        for forbiddenEffect in [
            "confirmEventDate(",
            "confirmPollDate(",
            "submitConfirmation(",
            "WakeveHaptics.success()",
            "onDateConfirmed(event.id)",
            "navigate("
        ] {
            XCTAssertFalse(
                rehydration.contains(forbiddenEffect),
                "Rehydration must restore render state without replaying \(forbiddenEffect)."
            )
        }
    }

    func testRestoredReceiptRemainsHandledWhenSyncLaterCompletes() throws {
        let source = readProjectFileIfPresent(viewModelPath)
        let rehydration = slice(source, from: "private func rehydrateConfirmation()", to: "private func ")
        let confirmedState = slice(
            source,
            from: "else if phase == EventManagementContract.ConfirmationPhase.confirmedPendingSync",
            to: "private func handle"
        )

        XCTAssertTrue(
            rehydration.contains("navigatedReceiptId = confirmed.receiptId"),
            "A restored receipt must be marked handled before RehydrateConfirmation can emit later state updates."
        )
        XCTAssertTrue(
            confirmedState.contains("navigatedReceiptId != receiptId"),
            "A pending-to-synced update for the restored receipt must remain ineligible for haptic or navigation replay."
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

    func testSyncedStateHasVisibleVoiceOverCopyWithoutDeliveryClaims() throws {
        let view = readProjectFileIfPresent(viewPath)
        let synced = slice(view, from: "case .synced:", to: "case ")

        XCTAssertFalse(synced.contains("EmptyView()"), "Synced confirmation must remain visibly distinct from pending sync.")
        XCTAssertTrue(synced.contains("poll.results.confirmation.synced.title"))
        XCTAssertTrue(synced.contains("poll.results.confirmation.synced.message"))
        XCTAssertTrue(synced.contains("pollConfirmationSyncedStatus"))
        XCTAssertTrue(synced.contains("poll.results.confirmation.accessibility.synced"))

        for locale in ["en", "fr", "es", "it", "pt"] {
            let strings = readProjectFileIfPresent("iosApp/src/Resources/\(locale).lproj/Localizable.strings")
            let syncedLines = strings
                .split(separator: "\n")
                .filter { $0.contains("poll.results.confirmation.synced") }
                .joined(separator: " ")
                .lowercased()

            XCTAssertTrue(syncedLines.contains("synced.title"), "\(locale) is missing synced title copy.")
            XCTAssertTrue(syncedLines.contains("synced.message"), "\(locale) is missing synced detail copy.")
            XCTAssertTrue(syncedLines.contains("accessibility.synced"), "\(locale) is missing synced VoiceOver copy.")
            for forbiddenClaim in ["participant", "calendar", "notifi", "particip", "calend", "notific"] {
                XCTAssertFalse(
                    syncedLines.contains(forbiddenClaim),
                    "Synced copy may acknowledge only the decision sync, never participant notification or calendar delivery."
                )
            }
        }
    }

    func testConfirmationDecisionSurfaceUsesDynamicTypeCompatibleTypography() throws {
        let view = readProjectFileIfPresent(viewPath)
        let actionButton = readProjectFileIfPresent(actionButtonPath)
        let decisionHeader = slice(view, from: "// Header", to: "ScrollView")
        let bestSlot = slice(view, from: "struct BestSlotCard", to: "// MARK: - Confirmed Date Card")
        let confirmedDate = slice(view, from: "struct ConfirmedDateCard", to: "// MARK: - Poll Decision Announcement")
        let status = slice(view, from: "private var confirmationStatus", to: "// MARK: - Poll Results Content View")

        XCTAssertFalse(
            decisionHeader.contains(".font(.system(size:"),
            "The confirmation decision title must use Dynamic-Type-compatible typography."
        )
        XCTAssertFalse(
            bestSlot.contains(".font(.system(size:"),
            "The selected decision date must use Dynamic-Type-compatible typography."
        )
        XCTAssertFalse(
            confirmedDate.contains(".font(.system(size:"),
            "The confirmed decision date must use Dynamic-Type-compatible typography."
        )
        XCTAssertTrue(status.contains(".font(.callout") || status.contains(".font(.body"))
        XCTAssertTrue(
            actionButton.contains(".font(WakeveTheme.Typography.bodySemibold)"),
            "The confirm action must use the shared Dynamic-Type-compatible text style."
        )
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
