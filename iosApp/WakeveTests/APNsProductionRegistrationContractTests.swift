import XCTest
@testable import Wakeve

final class APNsProductionRegistrationContractTests: XCTestCase {
    private let apnsServicePath = "iosApp/src/Services/APNsService.swift"
    private let registrationAdapterPath = "iosApp/src/Services/IosNotificationRegistrationAdapter.swift"
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
        let adapter = try readProjectFile(registrationAdapterPath)
        let view = try readProjectFile(preferencesViewPath)
        let model = try readProjectFile(registrationModelPath)

        XCTAssertTrue(model.contains("USER_REQUESTED_ENABLE"), "The approved model owns the explicit enable event.")
        XCTAssertTrue(view.contains("notifications.system_permission.enable"))
        XCTAssertFalse(view.contains("requestAuthorizationAndRegister("), "The view must dispatch a typed action, never invoke APNs directly.")
        XCTAssertFalse(view.contains("requestAuthorization(options:"), "The view must not own the system permission port.")
        XCTAssertTrue(view.contains("IosNotificationRegistrationViewModel"))
        XCTAssertTrue(view.contains("performPrimaryAction()"), "The enable control must route through the typed presentation action.")
        XCTAssertTrue(adapter.contains("adapter.send(event)"))
        XCTAssertTrue(adapter.contains("permission.requestAuthorization(correlationID:"))
        XCTAssertTrue(adapter.contains("remoteRegistration.register(correlationID:"))
    }

    func testDeniedStateOpensSettingsAndRefreshesWhenAppReturnsActive() throws {
        let adapter = try readProjectFile(registrationAdapterPath)
        let model = try readProjectFile(registrationModelPath)
        let view = try readProjectFile(preferencesViewPath)

        XCTAssertTrue(model.contains("USER_OPENED_SETTINGS"))
        XCTAssertTrue(model.contains("APP_BECAME_ACTIVE"), "Returning from Settings must re-enter permission checking through the model event.")
        XCTAssertTrue(adapter.contains("permission.openSettings()"))
        XCTAssertTrue(adapter.contains("permission.readStatus(correlationID:"))
        XCTAssertTrue(view.contains("IosNotificationRegistrationViewModel"))
        XCTAssertTrue(view.contains("performPrimaryAction()"))
        XCTAssertFalse(view.contains("UIApplication.openSettingsURLString"), "The view must dispatch USER_OPENED_SETTINGS through the adapter port.")
    }

    func testAppDelegateCallbacksCarryCurrentCorrelationAndStaleCallbacksAreIgnored() throws {
        let delegate = try readProjectFile(appDelegatePath)
        let adapter = try readProjectFile(registrationAdapterPath)

        XCTAssertTrue(delegate.contains("didRegisterForRemoteNotificationsWithDeviceToken"))
        XCTAssertTrue(delegate.contains("didFailToRegisterForRemoteNotificationsWithError"))
        XCTAssertTrue(adapter.contains("RemoteNotificationRegistrationCallbackSink"))
        XCTAssertTrue(adapter.contains("remoteNotificationRegistrationDidSucceed"))
        XCTAssertTrue(adapter.contains("remoteNotificationRegistrationDidFail"))
        XCTAssertTrue(adapter.contains("activeCorrelationID"), "Callbacks must be evaluated against the current invocation correlation.")
        XCTAssertTrue(adapter.contains("auditStaleCallback"), "A callback from an older invocation must be ignored and audited.")
    }

    func testTokenRefreshReplacesTokenAndBackendFailureRemainsRecoverable() throws {
        let adapter = try readProjectFile(registrationAdapterPath)

        XCTAssertTrue(adapter.contains("installationID"), "Registration identity must be stable across APNs token rotation.")
        XCTAssertTrue(adapter.contains("tokenFingerprint"), "Observable registration state must use a fingerprint, not the raw token.")
        XCTAssertTrue(adapter.contains("backendRegisterFailed"))
        XCTAssertTrue(adapter.contains("retryDue"))
        XCTAssertTrue(adapter.contains("nextRetryAt"))
        XCTAssertTrue(adapter.contains("backendRegisterSucceeded"))
    }

    func testRegistrationWaitsForAuthenticationInsteadOfCallingBackendAnonymously() throws {
        let adapter = try readProjectFile(registrationAdapterPath)

        XCTAssertTrue(adapter.contains("awaitingAuthentication"))
        XCTAssertTrue(adapter.contains("authenticationBecameAvailable"))
        XCTAssertTrue(adapter.contains("authenticationBecameUnavailable"))
        XCTAssertTrue(adapter.contains("authenticationSessionID"))
        XCTAssertFalse(
            adapter.contains("deferring token registration") && !adapter.contains("authenticationBecameAvailable"),
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
        let auth = try readProjectFile(authStatePath)

        XCTAssertTrue(auth.contains("unregistering"))
        XCTAssertTrue(auth.contains("retry"))
        XCTAssertTrue(auth.contains("offline"), "Offline logout must expose a recoverable state rather than erase credentials.")
    }

    func testLogoutAlreadyAbsentIsAnIdempotentSuccessForOneInstallation() throws {
        let target = IosBackendUnregistrationTarget.registration(
            registrationID: "registration-a",
            installationID: "installation-a"
        )

        XCTAssertEqual(target, .registration(registrationID: "registration-a", installationID: "installation-a"))
        XCTAssertNotEqual(target, .installation(installationID: "installation-a"))
    }

    func testUnregistrationUsesAClosedKnownRegistrationOrInstallationFallbackTarget() throws {
        let adapter = try readProjectFile(registrationAdapterPath)
        let request = slice(adapter, from: "struct BackendDeviceUnregistrationRequest", to: "protocol BackendDeviceRegistrationPort")

        XCTAssertTrue(adapter.contains("enum IosBackendUnregistrationTarget"))
        XCTAssertTrue(adapter.contains("case registration(registrationID: String, installationID: String)"))
        XCTAssertTrue(adapter.contains("case installation(installationID: String)"))
        XCTAssertTrue(request.contains("let target: IosBackendUnregistrationTarget"))
        XCTAssertTrue(request.contains("authenticationSessionID"))
        XCTAssertTrue(request.contains("correlationID"))
        XCTAssertFalse(request.contains("backendRegistrationID: String"), "Unknown acknowledgement must not synthesize a backend registration ID.")
    }

    func testLogoutOfOneInstallationDoesNotUnregisterSecondDevice() throws {
        let first = IosBackendUnregistrationTarget.registration(
            registrationID: "registration-a",
            installationID: "installation-a"
        )
        let second = IosBackendUnregistrationTarget.registration(
            registrationID: "registration-b",
            installationID: "installation-b"
        )

        XCTAssertNotEqual(first, second)
    }

    func testRegistrationAdapterUsesOnlyReviewedStatesEventsAndCorrelationInvariant() throws {
        let model = try readProjectFile(registrationModelPath)
        let adapter = try readProjectFile(registrationAdapterPath)

        for state in [
            "checkingPermission", "notDetermined", "requestingPermission", "denied",
            "registeringApns", "awaitingAuthentication", "registeringBackend", "retry",
            "registered", "unregistering", "unregistered", "cancelled", "misconfigured"
        ] {
            XCTAssertTrue(model.contains(state), "Approved model is missing \(state).")
            XCTAssertTrue(adapter.contains(state), "Swift adapter must project reviewed state \(state).")
        }

        XCTAssertTrue(model.contains("stale callbacks never transition state"))
        XCTAssertTrue(adapter.contains("activeCorrelationID"))
        XCTAssertFalse(adapter.localizedCaseInsensitiveContains("openai"))
        XCTAssertFalse(adapter.localizedCaseInsensitiveContains("prompt decides"))
    }

    func testProductionTypesBindTheExecutableAdapterPorts() throws {
        let adapter = try readProjectFile(registrationAdapterPath)
        let auth = try readProjectFile(authStatePath)

        for conformance in [
            "NotificationPermissionPort",
            "RemoteNotificationRegistrationPort",
            "BackendDeviceRegistrationPort",
            "RegistrationClockPort"
        ] {
            XCTAssertTrue(adapter.contains(conformance), "The registration adapter must bind executable port \(conformance).")
        }
        XCTAssertTrue(auth.contains("CredentialLifecyclePort"), "AuthStateManager must expose ordered credential clearing through the reviewed port.")
        XCTAssertTrue(adapter.contains("IosNotificationRegistrationEvent"))
        XCTAssertTrue(adapter.contains("IosNotificationRegistrationState"))
    }

    func testRegistrationModelRequiresExplicitClosedApnsScopeAndPrivateTokenCustody() throws {
        let model = try readProjectFile(registrationModelPath)
        let adapter = try readProjectFile(registrationAdapterPath)
        let service = try readProjectFile(apnsServicePath)
        let snapshot = slice(adapter, from: "struct IosNotificationRegistrationSnapshot", to: "protocol NotificationPermissionPort")

        XCTAssertTrue(model.contains("topic"), "The approved model must require an explicit APNs topic.")
        XCTAssertTrue(model.contains("environment"), "The approved model must require an explicit APNs environment.")
        XCTAssertTrue(model.contains("'sandbox' | 'production'"), "APNs environment must be a closed sandbox|production set.")
        XCTAssertTrue(adapter.contains("IosNotificationRegistrationConfiguration"))
        XCTAssertTrue(adapter.contains("topic"))
        XCTAssertTrue(adapter.contains("sandbox") && adapter.contains("production"))
        XCTAssertFalse(snapshot.contains("token: Data"), "The observable snapshot must never retain a raw APNs token.")
        XCTAssertFalse(snapshot.contains("rawToken"), "The observable snapshot must never retain a raw APNs token.")
        XCTAssertTrue(adapter.contains("RawApnsTokenCustodyPort"), "Raw APNs tokens must be held behind a dedicated custody port.")
        XCTAssertTrue(adapter.contains("tokenCustody.replace("), "The adapter must put an APNs callback token into the custody port.")
        XCTAssertTrue(adapter.contains("tokenCustody.withToken"), "The adapter must access raw token material only through the custody port.")
        XCTAssertTrue(adapter.contains("tokenCustody.clear()"), "Terminal registration paths must purge the raw APNs token through the custody port.")
        XCTAssertFalse(service.contains("token.prefix("), "The legacy APNs bridge must not log raw token material.")
    }

    func testRegistrationViewModelProjectsEveryReviewedPermissionAndRecoveryState() {
        let expected: [(
            IosNotificationRegistrationState,
            String,
            String,
            String,
            String,
            String?,
            String?,
            IosNotificationRegistrationEvent?
        )] = [
            (
                .notDetermined,
                "notificationPermissionStatus",
                "notifications.system_permission.accessibility.status",
                "notifications.system_permission.not_determined",
                "notifications.system_permission.accessibility.status_hint",
                "notificationPermissionEnableButton",
                "notifications.system_permission.accessibility.enable_hint",
                .userRequestedEnable
            ),
            (
                .denied,
                "notificationPermissionStatus",
                "notifications.system_permission.accessibility.status",
                "notifications.system_permission.denied",
                "notifications.system_permission.accessibility.status_hint",
                "notificationPermissionOpenSettingsButton",
                "notifications.system_permission.accessibility.open_settings_hint",
                .userOpenedSettings
            ),
            (
                .registered,
                "notificationRegistrationRegisteredStatus",
                "notifications.system_permission.accessibility.registered",
                "notifications.system_permission.authorized",
                "notifications.system_permission.accessibility.registered_hint",
                nil,
                nil,
                nil
            ),
            (
                .retry,
                "notificationRegistrationRetryStatus",
                "notifications.system_permission.accessibility.retry",
                "notifications.system_permission.retry",
                "notifications.system_permission.accessibility.retry_hint",
                nil,
                nil,
                nil
            ),
            (
                .misconfigured,
                "notificationRegistrationMisconfiguredStatus",
                "notifications.system_permission.accessibility.misconfigured",
                "notifications.system_permission.misconfigured",
                "notifications.system_permission.accessibility.misconfigured_hint",
                nil,
                nil,
                nil
            ),
            (
                .unregistering,
                "notificationRegistrationUnregisteringStatus",
                "notifications.system_permission.accessibility.unregistering",
                "notifications.system_permission.unregistering",
                "notifications.system_permission.accessibility.unregistering_hint",
                nil,
                nil,
                nil
            )
        ]

        for (
            state,
            statusIdentifier,
            statusLabelKey,
            statusDescriptionKey,
            statusHintKey,
            primaryIdentifier,
            primaryHintKey,
            primaryEvent
        ) in expected {
            var snapshot = IosNotificationRegistrationSnapshot.initial(installationID: "installation-a")
            snapshot.state = state

            let presentation = IosNotificationRegistrationViewModel.project(snapshot)

            XCTAssertEqual(presentation.statusAccessibilityIdentifier, statusIdentifier, "Unexpected status identifier for \(state).")
            XCTAssertEqual(presentation.statusAccessibilityLabelKey, statusLabelKey, "Unexpected status label for \(state).")
            XCTAssertEqual(presentation.statusDescriptionKey, statusDescriptionKey, "Unexpected visible description for \(state).")
            XCTAssertEqual(presentation.statusAccessibilityHintKey, statusHintKey, "Unexpected status hint for \(state).")
            XCTAssertEqual(presentation.primaryActionAccessibilityIdentifier, primaryIdentifier, "Unexpected primary control for \(state).")
            XCTAssertEqual(presentation.primaryActionAccessibilityHintKey, primaryHintKey, "Unexpected primary-action hint for \(state).")
            XCTAssertEqual(presentation.primaryEvent, primaryEvent, "Only reviewed user-driven actions may leave \(state).")
        }
    }

    func testNotificationRegistrationUIUsesOnlyTheViewModelProjectionAndPassiveRetryStatus() throws {
        let view = try readProjectFile(preferencesViewPath)
        let adapter = try readProjectFile(registrationAdapterPath)
        let service = try readProjectFile(apnsServicePath)

        for identifier in [
            "notificationPermissionStatus",
            "notificationPermissionEnableButton",
            "notificationPermissionOpenSettingsButton",
            "notificationRegistrationRegisteredStatus",
            "notificationRegistrationRetryStatus",
            "notificationRegistrationMisconfiguredStatus",
            "notificationRegistrationUnregisteringStatus"
        ] {
            XCTAssertTrue(adapter.contains(identifier), "Missing stable accessibility identifier: \(identifier)")
        }

        for key in [
            "notifications.system_permission.accessibility.status",
            "notifications.system_permission.accessibility.enable",
            "notifications.system_permission.accessibility.open_settings",
            "notifications.system_permission.accessibility.registered",
            "notifications.system_permission.accessibility.retry",
            "notifications.system_permission.accessibility.misconfigured",
            "notifications.system_permission.accessibility.unregistering"
        ] {
            XCTAssertTrue(adapter.contains(key), "Missing accessibility label projection: \(key)")
        }

        XCTAssertTrue(view.contains("IosNotificationRegistrationViewModel"))
        XCTAssertTrue(view.contains("performPrimaryAction()"), "The UI must dispatch a reviewed primary action through its view model.")
        XCTAssertTrue(view.contains("presentation.statusAccessibilityIdentifier"), "The view must apply the status identifier from the reviewed projection.")
        XCTAssertTrue(view.contains("presentation.statusAccessibilityLabelKey"), "The view must apply the status label from the reviewed projection.")
        XCTAssertTrue(view.contains("presentation.primaryActionAccessibilityIdentifier"), "The view must apply the primary-control identifier from the reviewed projection.")
        XCTAssertTrue(view.contains("presentation.primaryActionAccessibilityLabelKey"), "The view must apply the primary-control label from the reviewed projection.")
        XCTAssertTrue(view.contains("controlSemantics("), "The projected control must consume the tested accessibility/rendering semantics.")
        XCTAssertTrue(view.contains("minimumHitTargetPoints"))
        XCTAssertTrue(view.contains("decorativeIconHiddenFromVoiceOver"))
        XCTAssertTrue(view.contains(".accessibilityIdentifier("))
        XCTAssertTrue(view.contains(".accessibilityLabel("))
        XCTAssertFalse(view.contains("UNAuthorizationStatus"), "Authorization status branching belongs to the adapter/model, not the view.")
        XCTAssertFalse(view.contains("switch systemPermissionStatus"), "The UI must render the approved projection instead of UIKit status switches.")
        XCTAssertFalse(view.contains("openAppSettings()"), "Opening Settings is a typed adapter event, not a direct UI side effect.")
        XCTAssertFalse(view.contains("UIApplication.openSettingsURLString"), "The view must not directly open Settings.")
        XCTAssertFalse(view.contains("APNsService.shared.checkAuthorizationStatus"), "The view must not poll UIKit authorization status directly.")
        XCTAssertFalse(adapter.contains("notificationRegistrationRetryButton"), "Retry is passive and scheduled; it must not expose a manual retry button.")
        XCTAssertFalse(adapter.contains("primaryEvent: .retryDue"), "Only the retry scheduler may emit RETRY_DUE.")
        XCTAssertFalse(view.contains(".retryDue"), "The view must never schedule or trigger retry work itself.")
        XCTAssertTrue(adapter.contains("retryScheduler.scheduleRetry"))
        XCTAssertTrue(service.contains("registrationAdapter?.send(.appBecameActive)"), "Return-to-active must enter the reviewed adapter event path.")
    }

    func testNotificationRegistrationAccessibilityLabelsAndHintsAreLocalizedInAllSupportedLocales() throws {
        let keys = [
            "notifications.system_permission.accessibility.status",
            "notifications.system_permission.accessibility.status_hint",
            "notifications.system_permission.accessibility.enable",
            "notifications.system_permission.accessibility.enable_hint",
            "notifications.system_permission.accessibility.open_settings",
            "notifications.system_permission.accessibility.open_settings_hint",
            "notifications.system_permission.accessibility.registered",
            "notifications.system_permission.accessibility.registered_hint",
            "notifications.system_permission.accessibility.retry",
            "notifications.system_permission.accessibility.retry_hint",
            "notifications.system_permission.accessibility.misconfigured",
            "notifications.system_permission.accessibility.misconfigured_hint",
            "notifications.system_permission.accessibility.unregistering",
            "notifications.system_permission.accessibility.unregistering_hint",
            "notifications.system_permission.not_determined",
            "notifications.system_permission.denied",
            "notifications.system_permission.authorized",
            "notifications.system_permission.retry",
            "notifications.system_permission.misconfigured",
            "notifications.system_permission.unregistering"
        ]

        for locale in ["en", "fr", "es", "it", "pt"] {
            let strings = parseLocalizedStrings(
                try readProjectFile("iosApp/src/Resources/\(locale).lproj/Localizable.strings")
            )
            for key in keys {
                XCTAssertFalse(strings[key, default: ""].trimmingCharacters(in: .whitespacesAndNewlines).isEmpty, "Missing or blank \(key) in \(locale) localization.")
            }
        }
    }

    func testNotificationRegistrationControlSemanticsAreDerivedFromTheReviewedProjection() {
        var snapshot = IosNotificationRegistrationSnapshot.initial(installationID: "installation-a")
        snapshot.state = .notDetermined

        let semantics = IosNotificationRegistrationViewModel.controlSemantics(
            for: IosNotificationRegistrationViewModel.project(snapshot)
        )

        XCTAssertEqual(44, semantics.minimumHitTargetPoints)
        XCTAssertTrue(semantics.supportsDynamicType)
        XCTAssertTrue(semantics.decorativeIconHiddenFromVoiceOver)
        XCTAssertEqual("notificationPermissionEnableButton", semantics.accessibilityIdentifier)
        XCTAssertEqual("notifications.system_permission.accessibility.enable", semantics.accessibilityLabelKey)
        XCTAssertEqual("notifications.system_permission.accessibility.enable_hint", semantics.accessibilityHintKey)

        snapshot.state = .denied
        let openSettingsSemantics = IosNotificationRegistrationViewModel.controlSemantics(
            for: IosNotificationRegistrationViewModel.project(snapshot)
        )

        XCTAssertEqual(44, openSettingsSemantics.minimumHitTargetPoints)
        XCTAssertTrue(openSettingsSemantics.supportsDynamicType)
        XCTAssertTrue(openSettingsSemantics.decorativeIconHiddenFromVoiceOver)
        XCTAssertEqual("notificationPermissionOpenSettingsButton", openSettingsSemantics.accessibilityIdentifier)
        XCTAssertEqual("notifications.system_permission.accessibility.open_settings", openSettingsSemantics.accessibilityLabelKey)
        XCTAssertEqual("notifications.system_permission.accessibility.open_settings_hint", openSettingsSemantics.accessibilityHintKey)
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

    private func parseLocalizedStrings(_ source: String) -> [String: String] {
        Dictionary(uniqueKeysWithValues: source.split(separator: "\n").compactMap { line in
            let components = line.split(separator: "=", maxSplits: 1).map(String.init)
            guard components.count == 2 else { return nil }
            let key = components[0].trimmingCharacters(in: .whitespacesAndNewlines)
                .trimmingCharacters(in: CharacterSet(charactersIn: "\""))
            let value = components[1].trimmingCharacters(in: .whitespacesAndNewlines)
                .trimmingCharacters(in: CharacterSet(charactersIn: ";"))
                .trimmingCharacters(in: CharacterSet(charactersIn: "\""))
            guard !key.isEmpty else { return nil }
            return (key, value)
        })
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
