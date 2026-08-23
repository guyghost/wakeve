import XCTest
@testable import Wakeve

final class APNsBackendUnregistrationHttpContractRedTests: XCTestCase {
    func testKnownRegistrationBuildsCanonicalDeleteWithoutFallbackPayload() {
        let request = BackendDeviceRegistrationHttpContract.unregistrationRequest(
            for: .registration(registrationID: "registration-a", installationID: "installation-a")
        )

        XCTAssertEqual(request.method, "DELETE")
        XCTAssertEqual(request.path, "/notifications/registrations/registration-a")
        XCTAssertNil(request.body)
    }

    func testUnknownRegistrationBuildsOnlyBoundedInstallationAlias() {
        let request = BackendDeviceRegistrationHttpContract.unregistrationRequest(
            for: .installation(installationID: "installation-a")
        )

        XCTAssertEqual(request.method, "DELETE")
        XCTAssertEqual(request.path, "/notifications/unregister")
        XCTAssertEqual(request.body, ["installationId": "installation-a"])
    }

    func testOnlyTwoXXCanSucceedAndAlreadyAbsentRequiresExplicitTwoXXReceipt() {
        XCTAssertEqual(
            BackendDeviceRegistrationHttpContract.classifyUnregistrationResponse(statusCode: 204, alreadyAbsent: false),
            .succeeded(alreadyAbsent: false)
        )
        XCTAssertEqual(
            BackendDeviceRegistrationHttpContract.classifyUnregistrationResponse(statusCode: 200, alreadyAbsent: true),
            .succeeded(alreadyAbsent: true)
        )
        XCTAssertEqual(
            BackendDeviceRegistrationHttpContract.classifyUnregistrationResponse(statusCode: 404, alreadyAbsent: true),
            .notOwnedOrMissing
        )
        XCTAssertEqual(
            BackendDeviceRegistrationHttpContract.classifyUnregistrationResponse(statusCode: 401, alreadyAbsent: false),
            .authenticationFailure
        )
    }

    func testAPNsServiceNeverTreatsHTTP404AsUnregisterSuccess() throws {
        let source = try readProjectFile("iosApp/src/Services/APNsService.swift")
        let unregister = slice(source, from: "func unregister(_ unregistration", to: "private func sendRegistrationFailure")

        XCTAssertFalse(unregister.contains("httpResponse.statusCode == 404"))
        XCTAssertFalse(unregister.contains("|| alreadyAbsent"))
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
