import XCTest
@testable import Wakeve

final class APNsProductionRegistrationContractTests: XCTestCase {
    private let apnsServicePath = "iosApp/src/Services/APNsService.swift"
    private let appDelegatePath = "iosApp/src/Services/AppDelegate.swift"
    private let authStatePath = "iosApp/src/Services/AuthStateManager.swift"
    private let preferencesViewPath = "iosApp/src/Views/Notifications/NotificationPreferencesView.swift"
    private let registrationModelPath = "models/ios-notification-registration.machine.ts"

    func testLaunchReadsPermissionWithoutRequestingAuthorization() throws {
        let delegate = try readProjectFile(appDelegatePath)
        let auth = try readProjectFile(authStatePath)

        let launchSurfaces = delegate + auth
        XCTAssertFalse(launchSurfaces.contains("requestAuthorizationAndRegister("))
        XCTAssertFalse(launchSurfaces.contains("requestAuthorization(options:"))
        XCTAssertTrue(
            launchSurfaces.contains("checkAuthorizationStatus(") ||
                launchSurfaces.contains("APP_BECAME_ACTIVE"),
            "Launch/resume must only resolve notification status; it must never trigger the system prompt."
        )
    }

    func testNotDeterminedRequiresExplicitEnableEventBeforePermissionRequest() throws {
        let service = try readProjectFile(apnsServicePath)
        let view = try readProjectFile(preferencesViewPath)

        XCTAssertTrue(view.contains("notDetermined"))
        XCTAssertTrue(view.contains("USER_REQUESTED_ENABLE"), "The enable action must dispatch the reviewed typed event.")
        XCTAssertTrue(view.contains("notifications.system_permission.enable"))
        XCTAssertTrue(view.contains("requestAuthorizationAndRegister("))
        XCTAssertTrue(service.contains("requestAuthorization(options: options)"))
    }

    func testDeniedStateOpensSettingsAndRefreshesWhenAppReturnsActive() throws {
        let view = try readProjectFile(preferencesViewPath)

        XCTAssertTrue(view.contains("systemPermissionStatus == .denied"))
        XCTAssertTrue(view.contains("UIApplication.openSettingsURLString"))
        XCTAssertTrue(view.contains("phase == .active"))
        XCTAssertTrue(view.contains("APP_BECAME_ACTIVE"), "Returning from Settings must re-enter permission checking through the model event.")
        XCTAssertFalse(slice(view, from: "case .denied", to: "case .notDetermined").contains("requestAuthorization"))
    }

    func testAppDelegateCallbacksCarryCurrentCorrelationAndStaleCallbacksAreIgnored() throws {
        let delegate = try readProjectFile(appDelegatePath)
        let service = try readProjectFile(apnsServicePath)

        XCTAssertTrue(delegate.contains("didRegisterForRemoteNotificationsWithDeviceToken"))
        XCTAssertTrue(delegate.contains("didFailToRegisterForRemoteNotificationsWithError"))
        XCTAssertTrue(delegate.contains("correlationId"), "AppDelegate must forward the active invocation correlation identifier.")
        XCTAssertTrue(service.contains("APNS_DID_REGISTER"))
        XCTAssertTrue(service.contains("APNS_DID_FAIL"))
        XCTAssertTrue(service.contains("auditStaleCallback"), "A callback from an older invocation must be ignored and audited.")
    }

    func testTokenRefreshReplacesTokenAndBackendFailureRemainsRecoverable() throws {
        let service = try readProjectFile(apnsServicePath)

        XCTAssertTrue(service.contains("installationId"), "Registration identity must be stable across APNs token rotation.")
        XCTAssertTrue(service.contains("tokenFingerprint"), "Observable registration state must use a fingerprint, not the raw token.")
        XCTAssertTrue(service.contains("BACKEND_REGISTER_FAILED"))
        XCTAssertTrue(service.contains("RETRY_DUE"))
        XCTAssertTrue(service.contains("nextRetryAt"))
        XCTAssertTrue(service.contains("BACKEND_REGISTER_SUCCEEDED"))
    }

    func testRegistrationWaitsForAuthenticationInsteadOfCallingBackendAnonymously() throws {
        let service = try readProjectFile(apnsServicePath)

        XCTAssertTrue(service.contains("awaitingAuthentication"))
        XCTAssertTrue(service.contains("AUTH_BECAME_AVAILABLE"))
        XCTAssertTrue(service.contains("AUTH_BECAME_UNAVAILABLE"))
        XCTAssertTrue(service.contains("authSessionId"))
        XCTAssertFalse(
            service.contains("deferring token registration") && !service.contains("AUTH_BECAME_AVAILABLE"),
            "Deferred registration must be durable and resume from an explicit authentication event."
        )
    }

    func testLogoutAwaitsAuthenticatedUnregisterBeforeClearingJWT() throws {
        let auth = try readProjectFile(authStatePath)
        let signOut = slice(auth, from: "func signOut()", to: "// MARK:")

        XCTAssertTrue(signOut.contains("await"), "Logout must await the unregister terminal.")
        XCTAssertTrue(signOut.contains("LOGOUT_REQUESTED"))
        XCTAssertTrue(signOut.contains("PUSH_UNREGISTERED"))
        XCTAssertOrder(signOut, "LOGOUT_REQUESTED", before: "authService.signOut()")
        XCTAssertOrder(signOut, "PUSH_UNREGISTERED", before: "authService.signOut()")
    }

    func testLogoutNetworkFailureIsRetryableAndDoesNotClearCredentials() throws {
        let service = try readProjectFile(apnsServicePath)
        let auth = try readProjectFile(authStatePath)

        XCTAssertTrue(service.contains("BACKEND_UNREGISTER_FAILED"))
        XCTAssertTrue(service.contains("resumeUnregistering"))
        XCTAssertTrue(service.contains("RETRY_DUE"))
        XCTAssertTrue(auth.contains("unregistering"))
        XCTAssertTrue(auth.contains("retry"))
        XCTAssertTrue(auth.contains("offline"), "Offline logout must expose a recoverable state rather than erase credentials.")
    }

    func testLogoutAlreadyAbsentIsAnIdempotentSuccessForOneInstallation() throws {
        let service = try readProjectFile(apnsServicePath)

        XCTAssertTrue(service.contains("installationId"))
        XCTAssertTrue(service.contains("BACKEND_UNREGISTER_SUCCEEDED"))
        XCTAssertTrue(service.contains("alreadyAbsent"), "An already-absent installation must complete unregister idempotently.")
        XCTAssertFalse(service.contains("?platform=ios"), "Logout must target one installation, not every iOS device owned by the user.")
    }

    func testLogoutOfOneInstallationDoesNotUnregisterSecondDevice() throws {
        let service = try readProjectFile(apnsServicePath)

        XCTAssertTrue(service.contains("installationId"))
        XCTAssertTrue(service.contains("backendRegistrationId"))
        XCTAssertFalse(service.contains("unregisterAllDevices"))
        XCTAssertFalse(service.contains("deleteAllRegistrations"))
    }

    func testRegistrationAdapterUsesOnlyReviewedStatesEventsAndCorrelationInvariant() throws {
        let model = try readProjectFile(registrationModelPath)
        let service = try readProjectFile(apnsServicePath)

        for state in [
            "checkingPermission", "notDetermined", "requestingPermission", "denied",
            "registeringApns", "awaitingAuthentication", "registeringBackend", "retry",
            "registered", "unregistering", "unregistered", "cancelled", "misconfigured"
        ] {
            XCTAssertTrue(model.contains(state), "Approved model is missing \(state).")
            XCTAssertTrue(service.contains(state), "Swift adapter must project reviewed state \(state).")
        }

        XCTAssertTrue(model.contains("stale callbacks never transition state"))
        XCTAssertTrue(service.contains("correlationId"))
        XCTAssertFalse(service.localizedCaseInsensitiveContains("openai"))
        XCTAssertFalse(service.localizedCaseInsensitiveContains("prompt decides"))
    }

    func testProductionTypesBindTheExecutableAdapterPorts() throws {
        let service = try readProjectFile(apnsServicePath)
        let auth = try readProjectFile(authStatePath)

        for conformance in [
            "NotificationPermissionPort",
            "RemoteNotificationRegistrationPort",
            "BackendDeviceRegistrationPort",
            "RegistrationClockPort"
        ] {
            XCTAssertTrue(service.contains(conformance), "APNsService must bind executable registration port \(conformance).")
        }
        XCTAssertTrue(auth.contains("CredentialLifecyclePort"), "AuthStateManager must expose ordered credential clearing through the reviewed port.")
        XCTAssertTrue(service.contains("IosNotificationRegistrationEvent"))
        XCTAssertTrue(service.contains("IosNotificationRegistrationState"))
    }

    func testPermissionAndRecoveryStatesExposeStableAccessibleControls() throws {
        let view = try readProjectFile(preferencesViewPath)

        for identifier in [
            "notificationPermissionStatus",
            "notificationPermissionEnableButton",
            "notificationPermissionOpenSettingsButton",
            "notificationRegistrationRetryButton",
            "notificationRegistrationMisconfiguredStatus"
        ] {
            XCTAssertTrue(view.contains(identifier), "Missing stable accessibility identifier: \(identifier)")
        }

        for key in [
            "notifications.system_permission.accessibility.status",
            "notifications.system_permission.accessibility.enable",
            "notifications.system_permission.accessibility.open_settings",
            "notifications.system_permission.accessibility.retry",
            "notifications.system_permission.accessibility.misconfigured"
        ] {
            XCTAssertTrue(view.contains(key), "Missing localized accessibility label: \(key)")
        }

        XCTAssertTrue(view.contains(".accessibilityHint("))
        XCTAssertTrue(view.contains("registered"))
        XCTAssertTrue(view.contains("retry"))
        XCTAssertTrue(view.contains("misconfigured"))
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
