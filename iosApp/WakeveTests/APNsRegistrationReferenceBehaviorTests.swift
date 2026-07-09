import XCTest
@testable import Wakeve

private final class RegistrationSpies: NotificationPermissionPort, RemoteNotificationRegistrationPort,
    BackendDeviceRegistrationPort, CredentialLifecyclePort, RegistrationClockPort, RegistrationRetryScheduler {
    var authenticationSessionID: String?
    var hasUsableCredential: Bool { authenticationSessionID != nil }
    var now = Date(timeIntervalSince1970: 1_000)
    private(set) var effects: [String] = []
    private(set) weak var callbackSink: RemoteNotificationRegistrationCallbackSink?

    init(sessionID: String? = nil) { authenticationSessionID = sessionID }
    func readStatus(correlationID: String) { effects.append("permission.read:\(correlationID)") }
    func requestAuthorization(correlationID: String) { effects.append("permission.prompt:\(correlationID)") }
    func openSettings() { effects.append("settings.open") }
    func register(correlationID: String, callbackSink: RemoteNotificationRegistrationCallbackSink) {
        effects.append("apns.register:\(correlationID)"); self.callbackSink = callbackSink
    }
    func register(_ request: BackendDeviceRegistrationRequest) {
        effects.append("backend.register:\(request.installationID):\(request.correlationID)")
    }
    func unregister(_ request: BackendDeviceUnregistrationRequest) {
        effects.append("backend.unregister:\(request.installationID):\(request.correlationID)")
    }
    func clearCredentialAfterPushUnregistered() { effects.append("auth.clearJWT"); authenticationSessionID = nil }
    func scheduleRetry(at date: Date, event: IosNotificationRegistrationEvent) { effects.append("retry.schedule:\(date.timeIntervalSince1970)") }
    func cancelRetry() { effects.append("retry.cancel") }
}

final class APNsRegistrationReferenceBehaviorTests: XCTestCase {
    func testLaunchReadsStatusAndNeverPrompts() {
        let (adapter, spies) = makeAdapter()
        adapter.send(.appBecameActive)
        XCTAssertEqual(adapter.snapshot.state, .checkingPermission)
        XCTAssertEqual(spies.effects.filter { $0.hasPrefix("permission.read:") }.count, 1)
        XCTAssertFalse(spies.effects.contains { $0.hasPrefix("permission.prompt:") })
    }

    func testExplicitEnableIsTheOnlyPathToPermissionPrompt() {
        let (adapter, spies) = makeAdapter()
        adapter.send(.appBecameActive)
        adapter.send(.permissionStatusResolved(.notDetermined, correlationID: correlation(adapter)))
        XCTAssertEqual(adapter.snapshot.state, .notDetermined)
        adapter.send(.userRequestedEnable)
        XCTAssertEqual(adapter.snapshot.state, .requestingPermission)
        XCTAssertEqual(spies.effects.filter { $0.hasPrefix("permission.prompt:") }.count, 1)
    }

    func testDeniedOpensSettingsAndActiveRefreshesWithoutPrompt() {
        let (adapter, spies) = makeAdapter()
        adapter.send(.appBecameActive)
        adapter.send(.permissionStatusResolved(.denied, correlationID: correlation(adapter)))
        adapter.send(.userOpenedSettings)
        adapter.send(.appBecameActive)
        XCTAssertEqual(spies.effects.filter { $0 == "settings.open" }.count, 1)
        XCTAssertEqual(spies.effects.filter { $0.hasPrefix("permission.read:") }.count, 2)
        XCTAssertFalse(spies.effects.contains { $0.hasPrefix("permission.prompt:") })
    }

    func testStaleAPNsCallbackCannotTransitionOrReplaceToken() {
        let (adapter, _) = makeAdapter(sessionID: "session")
        adapter.send(.appBecameActive); let stale = correlation(adapter)
        adapter.send(.permissionStatusResolved(.authorized, correlationID: stale))
        adapter.send(.apnsDidRegister(token: Data("stale-token".utf8), correlationID: stale))
        XCTAssertEqual(adapter.snapshot.state, .registeringApns)
        XCTAssertNil(adapter.snapshot.tokenFingerprint)
    }

    func testTokenRotationRegistersSameInstallationAgain() {
        let (adapter, spies) = makeRegisteredAdapter(installationID: "a")
        adapter.send(.apnsDidRegister(token: Data("token-2".utf8), correlationID: correlation(adapter)))
        XCTAssertEqual(adapter.snapshot.state, .registered)
        XCTAssertEqual(spies.effects.filter { $0.hasPrefix("backend.register:a:") }.count, 2)
    }

    func testAuthenticationDeferredRegistrationResumesWhenSessionArrives() {
        let (adapter, spies) = makeAdapter()
        adapter.send(.appBecameActive)
        adapter.send(.permissionStatusResolved(.authorized, correlationID: correlation(adapter)))
        adapter.send(.apnsDidRegister(token: Data("token".utf8), correlationID: correlation(adapter)))
        XCTAssertEqual(adapter.snapshot.state, .awaitingAuthentication)
        XCTAssertFalse(spies.effects.contains { $0.hasPrefix("backend.register:") })
        adapter.send(.authenticationBecameAvailable(sessionID: "session"))
        XCTAssertEqual(adapter.snapshot.state, .registeringBackend)
    }

    func testBackendFailureUsesClockAndSchedulesRetry() {
        let (adapter, spies) = makeAdapter(sessionID: "session")
        adapter.send(.appBecameActive)
        adapter.send(.permissionStatusResolved(.authorized, correlationID: correlation(adapter)))
        adapter.send(.apnsDidRegister(token: Data("token".utf8), correlationID: correlation(adapter)))
        adapter.send(.backendRegisterFailed(.network, correlationID: correlation(adapter)))
        XCTAssertEqual(adapter.snapshot.state, .retry)
        XCTAssertEqual(adapter.snapshot.nextRetryAt, spies.now.addingTimeInterval(1))
        XCTAssertTrue(spies.effects.contains { $0.hasPrefix("retry.schedule:") })
    }

    func testLogoutClearsCredentialOnlyAfterUnregisterSuccess() {
        let (adapter, spies) = makeRegisteredAdapter(installationID: "a")
        adapter.send(.logoutRequested)
        XCTAssertEqual(adapter.snapshot.state, .unregistering)
        XCTAssertFalse(spies.effects.contains("auth.clearJWT"))
        adapter.send(.backendUnregisterSucceeded(alreadyAbsent: false, correlationID: correlation(adapter)))
        XCTAssertEqual(adapter.snapshot.state, .unregistered)
        XCTAssertLessThan(spies.effects.firstIndex { $0.hasPrefix("backend.unregister:a:") }!, spies.effects.firstIndex(of: "auth.clearJWT")!)
    }

    func testLogoutOfflineRetainsCredentialThenAlreadyAbsentCompletes() {
        let (adapter, spies) = makeRegisteredAdapter(installationID: "a")
        adapter.send(.logoutRequested)
        adapter.send(.backendUnregisterFailed(.network, correlationID: correlation(adapter)))
        XCTAssertEqual(adapter.snapshot.state, .retry); XCTAssertNotNil(spies.authenticationSessionID)
        adapter.send(.retryDue)
        adapter.send(.backendUnregisterSucceeded(alreadyAbsent: true, correlationID: correlation(adapter)))
        XCTAssertEqual(adapter.snapshot.state, .unregistered); XCTAssertNil(spies.authenticationSessionID)
    }

    func testLogoutTargetsOnlyOneOfTwoInstallations() {
        let (first, firstSpies) = makeRegisteredAdapter(installationID: "installation-a")
        let (second, secondSpies) = makeRegisteredAdapter(installationID: "installation-b")
        first.send(.logoutRequested)
        XCTAssertEqual(first.snapshot.state, .unregistering)
        XCTAssertEqual(second.snapshot.state, .registered)
        XCTAssertNotNil(secondSpies.authenticationSessionID)
        XCTAssertFalse(firstSpies.effects.contains { $0.contains("installation-b") })
    }

    func testAccessibilityProjectionUsesStableProductionActions() {
        let snapshot = IosNotificationRegistrationSnapshot.initial(installationID: "a")
        let retry = IosNotificationRegistrationSnapshot(
            installationID: snapshot.installationID, state: .retry, authorizationStatus: .authorized,
            authenticationSessionID: "session", hasUsableCredential: true, tokenFingerprint: "fingerprint",
            backendRegistrationID: nil, attempt: 1, nextRetryAt: Date(), lastErrorClass: .network,
            logoutRequested: false, activeCorrelationID: "correlation"
        )
        let presentation = IosNotificationRegistrationViewModel.project(retry)
        XCTAssertEqual(presentation.primaryActionAccessibilityIdentifier, "notificationRegistrationRetryButton")
        XCTAssertEqual(presentation.primaryActionAccessibilityLabelKey, "notifications.system_permission.accessibility.retry")
        XCTAssertEqual(presentation.primaryEvent, .retryDue)
    }

    private func makeAdapter(installationID: String = "a", sessionID: String? = nil) -> (IosNotificationRegistrationAdapter, RegistrationSpies) {
        let spies = RegistrationSpies(sessionID: sessionID)
        return (IosNotificationRegistrationAdapter(
            installationID: installationID, permission: spies, remoteRegistration: spies, backend: spies,
            credentials: spies, clock: spies, retryScheduler: spies
        ), spies)
    }

    private func makeRegisteredAdapter(installationID: String) -> (IosNotificationRegistrationAdapter, RegistrationSpies) {
        let pair = makeAdapter(installationID: installationID, sessionID: "session")
        pair.0.send(.appBecameActive)
        pair.0.send(.permissionStatusResolved(.authorized, correlationID: correlation(pair.0)))
        pair.0.send(.apnsDidRegister(token: Data("token".utf8), correlationID: correlation(pair.0)))
        pair.0.send(.backendRegisterSucceeded(registrationID: "registration-\(installationID)", correlationID: correlation(pair.0)))
        return pair
    }

    private func correlation(_ adapter: IosNotificationRegistrationAdapter) -> String {
        try! XCTUnwrap(adapter.snapshot.activeCorrelationID)
    }
}
