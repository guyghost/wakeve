import Foundation
import XCTest
@testable import Wakeve

private final class APNsCallbackSinkSpy: RemoteNotificationRegistrationCallbackSink {
    private(set) var successes: [(token: Data, correlationID: String)] = []
    private(set) var failures: [(errorClass: IosNotificationRegistrationErrorClass, correlationID: String)] = []

    func remoteNotificationRegistrationDidSucceed(token: Data, correlationID: String) {
        successes.append((token, correlationID))
    }

    func remoteNotificationRegistrationDidFail(
        errorClass: IosNotificationRegistrationErrorClass,
        correlationID: String
    ) {
        failures.append((errorClass, correlationID))
    }
}

final class APNsUIKitCoordinatorContractRedTests: XCTestCase {
    func testUIKitCoordinatorCoalescesConcurrentRequestsAndCompletesBothWithTheirOriginalCorrelations() {
        var starts: [String] = []
        let coordinator = UIKitAPNsRegistrationCoordinator { correlationID in
            starts.append(correlationID)
        }
        let first = APNsCallbackSinkSpy()
        let second = APNsCallbackSinkSpy()

        coordinator.enqueue(correlationID: "c1", callbackSink: first)
        coordinator.enqueue(correlationID: "c2", callbackSink: second)
        XCTAssertEqual(starts, ["c1"])

        XCTAssertTrue(coordinator.completeCurrentSuccess(token: Data("shared-token".utf8)))
        XCTAssertEqual(first.successes.map { $0.correlationID }, ["c1"])
        XCTAssertEqual(second.successes.map { $0.correlationID }, ["c2"])
        XCTAssertEqual(starts, ["c1"], "A coalesced request must not open a second UIKit callback window.")

        XCTAssertFalse(coordinator.completeCurrentSuccess(token: Data("stale-token".utf8)))
        XCTAssertEqual(first.successes.count, 1)
        XCTAssertEqual(second.successes.count, 1)
    }

    func testUIKitCoordinatorRejectsDuplicateCurrentFailureAfterCoalescedCompletion() {
        var starts: [String] = []
        let coordinator = UIKitAPNsRegistrationCoordinator { correlationID in
            starts.append(correlationID)
        }
        let first = APNsCallbackSinkSpy()
        let second = APNsCallbackSinkSpy()

        coordinator.enqueue(correlationID: "c1", callbackSink: first)
        coordinator.enqueue(correlationID: "c2", callbackSink: second)

        XCTAssertTrue(coordinator.completeCurrentFailure(errorClass: .network))
        XCTAssertEqual(first.failures.map { $0.correlationID }, ["c1"])
        XCTAssertEqual(second.failures.map { $0.correlationID }, ["c2"])
        XCTAssertEqual(starts, ["c1"])

        XCTAssertFalse(coordinator.completeCurrentFailure(errorClass: .network))
        XCTAssertEqual(first.failures.count, 1)
        XCTAssertEqual(second.failures.count, 1)
    }

    func testBackendFailureClassifierKeepsTransportAndCapacityFailuresRetryable() {
        XCTAssertEqual(APNsBackendFailureClassifier.classify(statusCode: 429, networkFailure: false), .network)
        XCTAssertEqual(APNsBackendFailureClassifier.classify(statusCode: 503, networkFailure: false), .network)
        XCTAssertEqual(APNsBackendFailureClassifier.classify(statusCode: nil, networkFailure: true), .network)
    }

    func testBackendFailureClassifierFailsClosedForValidationAndAuthentication() {
        XCTAssertEqual(APNsBackendFailureClassifier.classify(statusCode: 400, networkFailure: false), .configuration)
        XCTAssertEqual(APNsBackendFailureClassifier.classify(statusCode: 401, networkFailure: false), .backend)
    }

    func testAuthorizationRegistrationWrapperCompletesExactlyOnceWithoutPromptWhenAlreadyAuthorized() {
        var promptCalls = 0
        var registrationCalls = 0
        var completions: [(Bool, Error?)] = []
        let wrapper = APNsAuthorizationRegistrationWrapper(
            status: { .authorized },
            requestAuthorization: { promptCalls += 1 },
            registerForRemoteNotifications: { registrationCalls += 1 }
        )

        wrapper.requestAuthorizationAndRegister { granted, error in
            completions.append((granted, error))
        }

        XCTAssertEqual(promptCalls, 0)
        XCTAssertEqual(registrationCalls, 1)
        XCTAssertEqual(completions.count, 1)
        XCTAssertEqual(completions[0].0, true)
        XCTAssertNil(completions[0].1)
    }

    func testAuthorizationRegistrationWrapperCompletesExactlyOnceWithoutPromptWhenDenied() {
        var promptCalls = 0
        var registrationCalls = 0
        var completions: [(Bool, Error?)] = []
        let wrapper = APNsAuthorizationRegistrationWrapper(
            status: { .denied },
            requestAuthorization: { promptCalls += 1 },
            registerForRemoteNotifications: { registrationCalls += 1 }
        )

        wrapper.requestAuthorizationAndRegister { granted, error in
            completions.append((granted, error))
        }

        XCTAssertEqual(promptCalls, 0)
        XCTAssertEqual(registrationCalls, 0)
        XCTAssertEqual(completions.count, 1)
        XCTAssertEqual(completions[0].0, false)
        XCTAssertNil(completions[0].1)
    }

    func testNotificationHandlingDoesNotLogPayloadAndPersistsSanitizedHistoryBeforeUICallbacks() throws {
        let source = try readProjectFile("iosApp/src/Services/APNsService.swift")
        let receive = slice(source, from: "public func didReceiveRemoteNotification", to: "private func showLocalNotification")
        let logLines = source.split(separator: "\n").filter { $0.contains("debugLog") }

        XCTAssertFalse(logLines.contains { $0.contains("userInfo") }, "APNs payloads must never be logged in full.")
        XCTAssertTrue(source.contains("NotificationHistoryPersistencePort"))
        XCTAssertTrue(source.contains("SanitizedNotificationHistoryRecord"))
        XCTAssertTrue(receive.contains("notificationHistory.persist("))
        XCTAssertOrder(receive, "notificationHistory.persist(", before: "onNotificationReceived?(")
        XCTAssertFalse(receive.contains("notificationHistory.persist(userInfo"), "History receives a sanitized record, never the complete payload.")
    }

    func testAPNsServiceIsMainActorConfinedAndLegacyWrapperNeverBlocksOnAuthorizationStatus() throws {
        let source = try readProjectFile("iosApp/src/Services/APNsService.swift")
        let wrapper = slice(source, from: "private lazy var authorizationRegistrationWrapper", to: "private lazy var ownedRegistrationAdapter")

        XCTAssertTrue(source.contains("@MainActor\n@objc public class APNsService"), "APNs service state and UIKit callbacks must be serialized on MainActor.")
        XCTAssertFalse(source.contains("DispatchSemaphore"), "APNs authorization reads must never block the main thread.")
        XCTAssertFalse(wrapper.contains("getAuthorizationStatus()"), "The legacy authorization wrapper must not call a synchronous status API.")
    }

    func testBackgroundNotificationHandlingNeverLogsRawPayloadOrIdentifier() throws {
        let source = try readProjectFile("iosApp/src/Services/APNsService.swift")
        let handler = slice(source, from: "private func handleNotification", to: "// MARK: - Badge Management")

        let backgroundLogLines = handler.split(separator: "\n").filter { $0.contains("debugLog") }

        XCTAssertFalse(backgroundLogLines.contains { $0.contains("userInfo") || $0.contains("notificationId") }, "Background notification handling must not log raw payloads or payload-derived identifiers.")
        XCTAssertTrue(backgroundLogLines.isEmpty, "Background notification handling must remain log-free so future payload fields cannot leak.")
    }

    private func readProjectFile(_ relativePath: String) throws -> String {
        let fileURL = URL(fileURLWithPath: #filePath)
        let projectRoot = fileURL
            .deletingLastPathComponent()
            .deletingLastPathComponent()
            .deletingLastPathComponent()
        return try String(contentsOf: projectRoot.appendingPathComponent(relativePath), encoding: .utf8)
    }

    private func slice(_ source: String, from startMarker: String, to endMarker: String) -> String {
        guard let start = source.range(of: startMarker)?.lowerBound else { return "" }
        let tail = source[start...]
        guard let end = tail.dropFirst(startMarker.count).range(of: endMarker)?.lowerBound else {
            return String(tail)
        }
        return String(tail[..<end])
    }

    private func XCTAssertOrder(
        _ source: String,
        _ first: String,
        before second: String,
        file: StaticString = #filePath,
        line: UInt = #line
    ) {
        guard let firstRange = source.range(of: first), let secondRange = source.range(of: second) else {
            XCTFail("Expected both \(first) and \(second).", file: file, line: line)
            return
        }
        XCTAssertLessThan(firstRange.lowerBound, secondRange.lowerBound, "Expected \(first) before \(second).", file: file, line: line)
    }
}
