import XCTest
@testable import Wakeve

final class DeepLinkLoggingRedTests: XCTestCase {
    func testAPNsFailureLogsUseOnlyStaticNonSensitiveCodes() throws {
        let apns = try readProjectFile("iosApp/src/Services/APNsService.swift")
        let localNotification = slice(
            apns,
            from: "private func showLocalNotification",
            to: "private func handleNotification"
        )
        let badgeCount = slice(
            apns,
            from: "public func setBadgeCount",
            to: "public func clearBadge"
        )

        assertStaticFailureCode(
            in: localNotification,
            expectedCode: "APNS_LOCAL_NOTIFICATION_ADD_FAILED",
            scope: "local-notification add"
        )
        assertStaticFailureCode(
            in: badgeCount,
            expectedCode: "APNS_BADGE_COUNT_SET_FAILED",
            scope: "badge-count set"
        )
    }

    func testDeepLinkHandoffsNeverLogRawURLPayloadOrEventID() throws {
        let app = try readProjectFile("iosApp/src/iOSApp.swift")
        let service = try readProjectFile("iosApp/src/Services/DeepLinkService.swift")
        let apns = try readProjectFile("iosApp/src/Services/APNsService.swift")

        let appHandoff = slice(app, from: "private func handleDeepLink(_ url: URL)", to: "\n}")
        let notificationFallback = slice(apns, from: "private func handleDeepLink(eventId: String)", to: "\n}")

        assertNoRawDeepLinkLogging(appHandoff, scope: "iOSApp.handleDeepLink")
        assertNoRawDeepLinkLogging(service, scope: "DeepLinkService parse/handle path")
        assertNoRawDeepLinkLogging(notificationFallback, scope: "APNsService.handleDeepLink(eventId:)")
    }

    private func assertNoRawDeepLinkLogging(_ source: String, scope: String, file: StaticString = #filePath, line: UInt = #line) {
        let loggingLines = loggingLines(in: source)

        XCTAssertFalse(loggingLines.contains("eventId"), "\(scope) must not log a raw event identifier.", file: file, line: line)
        XCTAssertFalse(loggingLines.contains("userInfo"), "\(scope) must not log notification payload data.", file: file, line: line)
        XCTAssertFalse(loggingLines.contains("absoluteString"), "\(scope) must not log a raw deep-link URL or payload-bearing query.", file: file, line: line)
        XCTAssertFalse(loggingLines.contains("payload"), "\(scope) must not log raw payload data.", file: file, line: line)
    }

    private func assertStaticFailureCode(
        in source: String,
        expectedCode: String,
        scope: String,
        file: StaticString = #filePath,
        line: UInt = #line
    ) {
        let lines = loggingLines(in: source)
        XCTAssertFalse(lines.contains("localizedDescription"), "\(scope) must not log an OS-provided error description.", file: file, line: line)
        XCTAssertFalse(lines.contains("\\("), "\(scope) logs must not interpolate error data.", file: file, line: line)
        XCTAssertTrue(lines.contains(expectedCode), "\(scope) must emit the reviewed static failure code.", file: file, line: line)
    }

    private func loggingLines(in source: String) -> String {
        source
            .split(separator: "\n", omittingEmptySubsequences: false)
            .filter { line in
                line.contains("debugLog(") || line.contains("Log.debug(") ||
                    line.contains("Log.warning(") || line.contains("Log.error(") ||
                    line.contains("print(") || line.contains("NSLog(") || line.contains("os_log(")
            }
            .joined(separator: "\n")
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
}
