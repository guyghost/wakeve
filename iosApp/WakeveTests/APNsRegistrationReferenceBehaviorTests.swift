import XCTest
import Combine
@testable import Wakeve

private enum TokenCustodyEffect: Equatable {
    case replaced
    case accessed
    case cleared
}

private final class DeterministicRegistrationRetryJitter: RegistrationRetryJitterSource {
    private var values: [Double]
    private(set) var consumedValues: [Double] = []

    init(values: [Double]) {
        self.values = values
    }

    func nextUnitInterval() -> Double {
        let value = values.removeFirst()
        consumedValues.append(value)
        return value
    }
}

private final class RegistrationSpies: NotificationPermissionPort, RemoteNotificationRegistrationPort,
    BackendDeviceRegistrationPort, CredentialLifecyclePort, RegistrationClockPort, RegistrationRetryScheduler,
    RawApnsTokenCustodyPort {
    var authenticationSessionID: String?
    var hasUsableCredential: Bool { authenticationSessionID != nil }
    var now = Date(timeIntervalSince1970: 1_000)
    private(set) var effects: [String] = []
    private(set) weak var callbackSink: RemoteNotificationRegistrationCallbackSink?
    private(set) var rawAPNsToken: Data?
    private(set) var tokenCustodyEffects: [TokenCustodyEffect] = []
    private(set) var unregistrationRequests: [BackendDeviceUnregistrationRequest] = []

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
        unregistrationRequests.append(request)
        switch request.target {
        case let .registration(_, installationID), let .installation(installationID):
            effects.append("backend.unregister:\(installationID):\(request.correlationID)")
        }
    }
    func clearCredentialAfterPushUnregistered() { effects.append("auth.clearJWT"); authenticationSessionID = nil }
    func scheduleRetry(at date: Date, event: IosNotificationRegistrationEvent) { effects.append("retry.schedule:\(date.timeIntervalSince1970)") }
    func cancelRetry() { effects.append("retry.cancel") }
    func replace(_ token: Data) { rawAPNsToken = token; tokenCustodyEffects.append(.replaced) }
    func withToken(_ operation: (Data) -> Void) -> Bool { guard let rawAPNsToken else { return false }; tokenCustodyEffects.append(.accessed); operation(rawAPNsToken); return true }
    func clear() { rawAPNsToken = nil; tokenCustodyEffects.append(.cleared) }
}

final class APNsRegistrationReferenceBehaviorTests: XCTestCase {
    func testRetryUsesInjectedFullJitterWithinOneTwoFourSecondExponentialCaps() {
        let spies = RegistrationSpies(sessionID: "session")
        let jitter = DeterministicRegistrationRetryJitter(values: [0, 1, 0.25])
        let configuration = try! XCTUnwrap(
            IosNotificationRegistrationConfiguration(topic: "com.guyghost.wakeve", environment: .production)
        )
        let adapter = IosNotificationRegistrationAdapter(
            installationID: "a",
            permission: spies,
            remoteRegistration: spies,
            backend: spies,
            credentials: spies,
            clock: spies,
            retryScheduler: spies,
            retryJitterSource: jitter,
            configuration: configuration,
            tokenCustody: spies
        )

        beginBackendRegistration(adapter, token: Data("jitter-token".utf8))
        adapter.send(.backendRegisterFailed(.network, correlationID: correlation(adapter)))
        XCTAssertEqual(adapter.snapshot.nextRetryAt, spies.now, "full jitter lower bound for cap 1 must be zero")

        adapter.send(.retryDue)
        adapter.send(.backendRegisterFailed(.network, correlationID: correlation(adapter)))
        XCTAssertEqual(
            adapter.snapshot.nextRetryAt,
            spies.now.addingTimeInterval(2),
            "a unit-jitter value of one must select the upper bound of cap 2"
        )

        adapter.send(.retryDue)
        adapter.send(.backendRegisterFailed(.network, correlationID: correlation(adapter)))
        XCTAssertEqual(
            adapter.snapshot.nextRetryAt,
            spies.now.addingTimeInterval(1),
            "a 0.25 jitter sample must produce one second within cap 4"
        )
        XCTAssertEqual(jitter.consumedValues, [0, 1, 0.25])
    }

    func testLogoutFromEveryActiveRegistrationPhaseWithAuthenticationStartsOneScopedUnregister() {
        let factories: [(String, (IosNotificationRegistrationAdapter, RegistrationSpies) -> Void)] = [
            ("checkingPermission", { _, _ in }),
            ("notDetermined", { adapter, _ in
                adapter.send(.appBecameActive)
                adapter.send(.permissionStatusResolved(.notDetermined, correlationID: self.correlation(adapter)))
            }),
            ("requestingPermission", { adapter, _ in
                adapter.send(.appBecameActive)
                adapter.send(.permissionStatusResolved(.notDetermined, correlationID: self.correlation(adapter)))
                adapter.send(.userRequestedEnable)
            }),
            ("denied", { adapter, _ in
                adapter.send(.appBecameActive)
                adapter.send(.permissionStatusResolved(.denied, correlationID: self.correlation(adapter)))
            }),
            ("registeringApns", { adapter, _ in
                adapter.send(.appBecameActive)
                adapter.send(.permissionStatusResolved(.authorized, correlationID: self.correlation(adapter)))
            }),
            ("registeringBackend", { adapter, _ in self.beginBackendRegistration(adapter, token: Data("token".utf8)) }),
            ("retry", { adapter, _ in
                self.beginBackendRegistration(adapter, token: Data("token".utf8))
                adapter.send(.backendRegisterFailed(.network, correlationID: self.correlation(adapter)))
            }),
            ("registered", { adapter, _ in
                let (registered, _) = self.makeRegisteredAdapter(installationID: "a")
                adapter.send(.appBecameActive)
                adapter.send(.permissionStatusResolved(.authorized, correlationID: self.correlation(adapter)))
                adapter.send(.apnsDidRegister(token: Data("token".utf8), correlationID: self.correlation(adapter)))
                adapter.send(.backendRegisterSucceeded(registrationID: registered.snapshot.backendRegistrationID!, correlationID: self.correlation(adapter)))
            })
        ]

        for (name, prepare) in factories {
            let (adapter, spies) = makeAdapter(sessionID: "session")
            prepare(adapter, spies)
            adapter.send(.logoutRequested)

            XCTAssertEqual(adapter.snapshot.state, .unregistering, "Logout from \(name) must not silently skip the backend cleanup.")
            XCTAssertEqual(spies.unregistrationRequests.count, 1, "Logout from \(name) must issue exactly one scoped request.")
            let target = spies.unregistrationRequests[0].target
            if name == "registered" {
                XCTAssertEqual(target, .registration(registrationID: "registration-a", installationID: "a"))
            } else {
                XCTAssertEqual(target, .installation(installationID: "a"))
            }
        }
    }

    func testLogoutWithoutAuthenticationWaitsInRetryThenUnregistersOnAuthenticationReturn() {
        let (adapter, spies) = makeAdapter()
        adapter.send(.appBecameActive)
        adapter.send(.permissionStatusResolved(.authorized, correlationID: correlation(adapter)))
        adapter.send(.apnsDidRegister(token: Data("awaiting-token".utf8), correlationID: correlation(adapter)))
        XCTAssertEqual(adapter.snapshot.state, .awaitingAuthentication)
        adapter.send(.logoutRequested)

        XCTAssertEqual(adapter.snapshot.state, .retry)
        XCTAssertTrue(spies.unregistrationRequests.isEmpty)
        XCTAssertFalse(spies.effects.contains("auth.clearJWT"))

        adapter.send(.authenticationBecameAvailable(sessionID: "replacement-session"))
        XCTAssertEqual(adapter.snapshot.state, .unregistering)
        XCTAssertEqual(spies.unregistrationRequests.map(\.target), [.installation(installationID: "a")])
    }

    func testStaleBackendFailureFromRetryC1CannotChangeC2OrStartAnotherRegistration() {
        let (adapter, spies) = makeAdapter(sessionID: "session")
        beginBackendRegistration(adapter, token: Data("token".utf8))
        let c1 = correlation(adapter)
        adapter.send(.backendRegisterFailed(.network, correlationID: c1))
        adapter.send(.retryDue)
        let c2 = correlation(adapter)
        let registrationsBeforeStale = spies.effects.filter { $0.hasPrefix("backend.register:") }.count

        adapter.send(.backendRegisterFailed(.network, correlationID: c1))
        XCTAssertEqual(adapter.snapshot.state, .registeringBackend)
        XCTAssertEqual(correlation(adapter), c2)
        XCTAssertEqual(spies.effects.filter { $0.hasPrefix("backend.register:") }.count, registrationsBeforeStale)
    }

    func testAuthenticationAndBudgetFailuresRemainBlockedWithoutCredentialClearOrUnregisterSuccess() {
        let (authAdapter, authSpies) = makeRegisteredAdapter(installationID: "a")
        authAdapter.send(.logoutRequested)
        authAdapter.send(.backendUnregisterFailed(.backend, correlationID: correlation(authAdapter)))
        XCTAssertEqual(authAdapter.snapshot.state, .retry, "401/403 must be classified as a recoverable authentication block.")
        XCTAssertNotNil(authSpies.authenticationSessionID)
        XCTAssertFalse(authSpies.effects.contains("auth.clearJWT"))

        let (budgetAdapter, budgetSpies) = makeRegisteredAdapter(installationID: "a")
        budgetAdapter.send(.logoutRequested)
        for _ in 0..<3 {
            budgetAdapter.send(.backendUnregisterFailed(.network, correlationID: correlation(budgetAdapter)))
            budgetAdapter.send(.retryDue)
        }
        XCTAssertEqual(budgetAdapter.snapshot.state, .misconfigured)
        XCTAssertNotNil(budgetSpies.authenticationSessionID)
        XCTAssertFalse(budgetSpies.effects.contains("auth.clearJWT"))
    }

    func testTwoSnapshotObserversReceiveOneIdenticalTerminalUnregistrationState() {
        let (adapter, _) = makeRegisteredAdapter(installationID: "a")
        var first: [IosNotificationRegistrationState] = []
        var second: [IosNotificationRegistrationState] = []
        let firstObserver = adapter.$snapshot.sink { first.append($0.state) }
        let secondObserver = adapter.$snapshot.sink { second.append($0.state) }

        adapter.send(.logoutRequested)
        adapter.send(.backendUnregisterSucceeded(alreadyAbsent: true, correlationID: correlation(adapter)))

        XCTAssertEqual(first.filter { $0 == .unregistered }.count, 1)
        XCTAssertEqual(second.filter { $0 == .unregistered }.count, 1)
        withExtendedLifetime((firstObserver, secondObserver)) {}
    }
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
        let (adapter, spies) = makeAdapter(sessionID: "session")
        adapter.send(.appBecameActive); let stale = correlation(adapter)
        adapter.send(.permissionStatusResolved(.authorized, correlationID: stale))
        adapter.send(.apnsDidRegister(token: Data("stale-token".utf8), correlationID: stale))
        XCTAssertEqual(adapter.snapshot.state, .registeringApns)
        XCTAssertNil(adapter.snapshot.tokenFingerprint)
        XCTAssertNil(spies.rawAPNsToken)
        XCTAssertEqual(spies.tokenCustodyEffects, [])
    }

    func testTokenRotationRegistersSameInstallationAgain() {
        let (adapter, spies) = makeRegisteredAdapter(installationID: "a")
        adapter.send(.apnsDidRegister(token: Data("token-2".utf8), correlationID: correlation(adapter)))
        XCTAssertEqual(adapter.snapshot.state, .registeringBackend)
        XCTAssertEqual(spies.effects.filter { $0.hasPrefix("backend.register:a:") }.count, 2)
        XCTAssertEqual(adapter.snapshot.backendRegistrationID, "registration-a", "rotation must retain the last acknowledged registration until its replacement is acknowledged")
        adapter.send(.backendRegisterSucceeded(registrationID: "registration-a-rotated", correlationID: correlation(adapter)))
        XCTAssertEqual(adapter.snapshot.state, .registered)
        XCTAssertEqual(adapter.snapshot.backendRegistrationID, "registration-a-rotated")
    }

    func testRotationFailureThenLogoutTargetsTheLastAcknowledgedRegistration() {
        let (adapter, spies) = makeRegisteredAdapter(installationID: "a")
        adapter.send(.apnsDidRegister(token: Data("rotated-token".utf8), correlationID: correlation(adapter)))
        adapter.send(.backendRegisterFailed(.network, correlationID: correlation(adapter)))
        XCTAssertEqual(adapter.snapshot.state, .retry)
        XCTAssertEqual(adapter.snapshot.backendRegistrationID, "registration-a")

        adapter.send(.logoutRequested)
        XCTAssertEqual(adapter.snapshot.state, .unregistering)
        XCTAssertEqual(spies.unregistrationRequests.count, 1)
        XCTAssertEqual(spies.unregistrationRequests[0].target, .registration(registrationID: "registration-a", installationID: "a"))
    }

    func testRegisteredRequiresCurrentCorrelatedBackendAcknowledgement() {
        let (adapter, spies) = makeAdapter(sessionID: "session")
        adapter.send(.appBecameActive)
        adapter.send(.permissionStatusResolved(.authorized, correlationID: correlation(adapter)))
        let token = Data("token".utf8)
        adapter.send(.apnsDidRegister(token: token, correlationID: correlation(adapter)))

        XCTAssertEqual(adapter.snapshot.state, .registeringBackend)
        XCTAssertEqual(spies.effects.filter { $0.hasPrefix("backend.register:a:") }.count, 1)
        XCTAssertEqual(spies.rawAPNsToken, token)
        let custodyBeforeStaleAcknowledgement = spies.tokenCustodyEffects
        let backendCorrelation = correlation(adapter)
        adapter.send(.backendRegisterSucceeded(registrationID: "stale", correlationID: "stale-correlation"))
        XCTAssertEqual(adapter.snapshot.state, .registeringBackend)
        XCTAssertNil(adapter.snapshot.backendRegistrationID)
        XCTAssertEqual(spies.rawAPNsToken, token)
        XCTAssertEqual(spies.tokenCustodyEffects, custodyBeforeStaleAcknowledgement)

        adapter.send(.backendRegisterSucceeded(registrationID: "registration-a", correlationID: backendCorrelation))
        XCTAssertEqual(adapter.snapshot.state, .registered)
        XCTAssertEqual(adapter.snapshot.backendRegistrationID, "registration-a")
        XCTAssertNil(spies.rawAPNsToken)
        XCTAssertEqual(spies.tokenCustodyEffects, custodyBeforeStaleAcknowledgement + [.cleared])
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

    func testMissingCustodyTokenWhenDeferredAuthenticationArrivesFailsClosedWithoutBackendRegistration() {
        let token = Data("deferred-token".utf8)
        let (adapter, spies) = makeAdapter()
        adapter.send(.appBecameActive)
        adapter.send(.permissionStatusResolved(.authorized, correlationID: correlation(adapter)))
        adapter.send(.apnsDidRegister(token: token, correlationID: correlation(adapter)))
        XCTAssertEqual(adapter.snapshot.state, .awaitingAuthentication)
        XCTAssertEqual(spies.rawAPNsToken, token)
        spies.clear()

        adapter.send(.authenticationBecameAvailable(sessionID: "session"))
        XCTAssertEqual(adapter.snapshot.state, .misconfigured)
        XCTAssertFalse(spies.effects.contains { $0.hasPrefix("backend.register:") })
        XCTAssertNil(spies.rawAPNsToken)
        XCTAssertEqual(spies.tokenCustodyEffects, [.replaced, .cleared, .cleared])
    }

    func testRawAPNsTokenIsRetainedWhileAuthenticationOrRetryStillNeedsBackendRegistration() {
        let awaitingToken = Data("awaiting-token".utf8)
        let (awaitingAdapter, awaitingSpies) = makeAdapter()
        awaitingAdapter.send(.appBecameActive)
        awaitingAdapter.send(.permissionStatusResolved(.authorized, correlationID: correlation(awaitingAdapter)))
        awaitingAdapter.send(.apnsDidRegister(token: awaitingToken, correlationID: correlation(awaitingAdapter)))
        XCTAssertEqual(awaitingAdapter.snapshot.state, .awaitingAuthentication)
        XCTAssertEqual(awaitingSpies.rawAPNsToken, awaitingToken)

        let retryToken = Data("retry-token".utf8)
        let (retryAdapter, retrySpies) = makeAdapter(sessionID: "session")
        retryAdapter.send(.appBecameActive)
        retryAdapter.send(.permissionStatusResolved(.authorized, correlationID: correlation(retryAdapter)))
        retryAdapter.send(.apnsDidRegister(token: retryToken, correlationID: correlation(retryAdapter)))
        retryAdapter.send(.backendRegisterFailed(.network, correlationID: correlation(retryAdapter)))
        XCTAssertEqual(retryAdapter.snapshot.state, .retry)
        XCTAssertEqual(retrySpies.rawAPNsToken, retryToken)
    }

    func testRawAPNsTokenIsPurgedAtBackendAcknowledgementCancellationUnregistrationAndMisconfiguration() {
        let backendAckToken = Data("backend-ack-token".utf8)
        let (backendAckAdapter, backendAckSpies) = makeAdapter(sessionID: "session")
        beginBackendRegistration(backendAckAdapter, token: backendAckToken)
        XCTAssertEqual(backendAckSpies.rawAPNsToken, backendAckToken)
        backendAckAdapter.send(.backendRegisterSucceeded(registrationID: "registration-a", correlationID: correlation(backendAckAdapter)))
        XCTAssertNil(backendAckSpies.rawAPNsToken)

        let cancellationToken = Data("cancel-token".utf8)
        let (cancellationAdapter, cancellationSpies) = makeAdapter()
        cancellationAdapter.send(.appBecameActive)
        cancellationAdapter.send(.permissionStatusResolved(.authorized, correlationID: correlation(cancellationAdapter)))
        cancellationAdapter.send(.apnsDidRegister(token: cancellationToken, correlationID: correlation(cancellationAdapter)))
        cancellationAdapter.send(.userCancelled)
        XCTAssertEqual(cancellationAdapter.snapshot.state, .cancelled)
        XCTAssertNil(cancellationSpies.rawAPNsToken)

        let (unregisterAdapter, unregisterSpies) = makeRegisteredAdapter(installationID: "a")
        let unregisterToken = Data("unregister-token".utf8)
        unregisterAdapter.send(.apnsDidRegister(token: unregisterToken, correlationID: correlation(unregisterAdapter)))
        XCTAssertEqual(unregisterSpies.rawAPNsToken, unregisterToken)
        unregisterAdapter.send(.logoutRequested)
        unregisterAdapter.send(.backendUnregisterSucceeded(alreadyAbsent: false, correlationID: correlation(unregisterAdapter)))
        XCTAssertEqual(unregisterAdapter.snapshot.state, .unregistered)
        XCTAssertNil(unregisterSpies.rawAPNsToken)

        let misconfiguredToken = Data("misconfigured-token".utf8)
        let (misconfiguredAdapter, misconfiguredSpies) = makeAdapter(sessionID: "session")
        beginBackendRegistration(misconfiguredAdapter, token: misconfiguredToken)
        XCTAssertEqual(misconfiguredSpies.rawAPNsToken, misconfiguredToken)
        misconfiguredAdapter.send(.configurationInvalid)
        XCTAssertEqual(misconfiguredAdapter.snapshot.state, .misconfigured)
        XCTAssertNil(misconfiguredSpies.rawAPNsToken)
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

    func testLogoutInterruptsRetryingAuthenticatedBackendRegistrationWithInstallationUnregister() {
        let token = Data("logout-retry-token".utf8)
        let (adapter, spies) = makeAdapter(sessionID: "session")
        beginBackendRegistration(adapter, token: token)
        adapter.send(.backendRegisterFailed(.network, correlationID: correlation(adapter)))
        XCTAssertEqual(adapter.snapshot.state, .retry)
        XCTAssertNil(adapter.snapshot.backendRegistrationID)
        XCTAssertEqual(spies.effects.filter { $0.hasPrefix("backend.register:a:") }.count, 1)
        XCTAssertEqual(spies.rawAPNsToken, token)

        adapter.send(.logoutRequested)
        XCTAssertEqual(adapter.snapshot.state, .unregistering)
        XCTAssertEqual(spies.effects.filter { $0.hasPrefix("backend.register:a:") }.count, 1)
        XCTAssertEqual(spies.effects.filter { $0.hasPrefix("backend.unregister:a:") }.count, 1)
        XCTAssertEqual(spies.unregistrationRequests.count, 1)
        let request = spies.unregistrationRequests[0]
        XCTAssertEqual(request.target, .installation(installationID: "a"))
        XCTAssertEqual(request.authenticationSessionID, "session")
        XCTAssertEqual(request.correlationID, correlation(adapter))
        XCTAssertEqual(spies.rawAPNsToken, token)
        XCTAssertNotNil(spies.authenticationSessionID)

        adapter.send(.backendUnregisterSucceeded(alreadyAbsent: true, correlationID: correlation(adapter)))
        XCTAssertEqual(adapter.snapshot.state, .unregistered)
        XCTAssertNil(spies.rawAPNsToken)
        XCTAssertNil(spies.authenticationSessionID)
        XCTAssertEqual(spies.tokenCustodyEffects, [.replaced, .accessed, .cleared])
    }

    func testLogoutClearsCredentialOnlyAfterUnregisterSuccess() {
        let (adapter, spies) = makeRegisteredAdapter(installationID: "a")
        adapter.send(.logoutRequested)
        XCTAssertEqual(adapter.snapshot.state, .unregistering)
        XCTAssertFalse(spies.effects.contains("auth.clearJWT"))
        XCTAssertEqual(spies.unregistrationRequests.count, 1)
        let request = spies.unregistrationRequests[0]
        XCTAssertEqual(request.target, .registration(registrationID: "registration-a", installationID: "a"))
        XCTAssertEqual(request.authenticationSessionID, "session")
        XCTAssertEqual(request.correlationID, correlation(adapter))
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
        XCTAssertEqual(firstSpies.unregistrationRequests.map(\.target), [.registration(registrationID: "registration-installation-a", installationID: "installation-a")])
        XCTAssertTrue(secondSpies.unregistrationRequests.isEmpty)
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
        XCTAssertEqual(presentation.statusAccessibilityIdentifier, "notificationRegistrationRetryStatus")
        XCTAssertEqual(presentation.statusAccessibilityLabelKey, "notifications.system_permission.accessibility.retry")
        XCTAssertNil(presentation.primaryActionAccessibilityIdentifier)
        XCTAssertNil(presentation.primaryActionAccessibilityLabelKey)
        XCTAssertNil(presentation.primaryEvent)
    }

    func testRegistrationConfigurationRequiresNonEmptyTopicAndClosedEnvironment() {
        let production = IosNotificationRegistrationConfiguration(
            topic: "com.guyghost.wakeve",
            environment: .production
        )
        XCTAssertEqual(production?.topic, "com.guyghost.wakeve")
        XCTAssertEqual(production?.environment, .production)
        XCTAssertNotNil(
            IosNotificationRegistrationConfiguration(topic: "com.guyghost.wakeve", environment: .sandbox)
        )
        XCTAssertNil(IosNotificationRegistrationConfiguration(topic: "   ", environment: .sandbox))
    }

    private func makeAdapter(installationID: String = "a", sessionID: String? = nil) -> (IosNotificationRegistrationAdapter, RegistrationSpies) {
        let spies = RegistrationSpies(sessionID: sessionID)
        let configuration = try! XCTUnwrap(
            IosNotificationRegistrationConfiguration(topic: "com.guyghost.wakeve", environment: .production)
        )
        return (IosNotificationRegistrationAdapter(
            installationID: installationID, permission: spies, remoteRegistration: spies, backend: spies,
            credentials: spies, clock: spies, retryScheduler: spies,
            retryJitterSource: DeterministicRegistrationRetryJitter(values: Array(repeating: 1, count: 4)),
            configuration: configuration,
            tokenCustody: spies
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

    private func beginBackendRegistration(_ adapter: IosNotificationRegistrationAdapter, token: Data) {
        adapter.send(.appBecameActive)
        adapter.send(.permissionStatusResolved(.authorized, correlationID: correlation(adapter)))
        adapter.send(.apnsDidRegister(token: token, correlationID: correlation(adapter)))
        XCTAssertEqual(adapter.snapshot.state, .registeringBackend)
    }
}
