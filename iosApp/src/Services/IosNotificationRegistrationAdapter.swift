import Combine
import Foundation

enum IosNotificationAuthorizationStatus: Equatable, Sendable {
    case notDetermined
    case denied
    case authorized
    case provisional
    case ephemeral
}

enum IosNotificationRegistrationState: String, Equatable, Sendable {
    case checkingPermission
    case notDetermined
    case requestingPermission
    case denied
    case registeringApns
    case awaitingAuthentication
    case registeringBackend
    case retry
    case registered
    case unregistering
    case unregistered
    case cancelled
    case misconfigured
}

enum IosNotificationRegistrationErrorClass: Equatable, Sendable {
    case permission
    case network
    case backend
    case configuration
}

enum IosNotificationRegistrationEvent: Equatable, Sendable {
    case appBecameActive
    case permissionStatusResolved(IosNotificationAuthorizationStatus, correlationID: String)
    case permissionStatusFailed(IosNotificationRegistrationErrorClass, correlationID: String)
    case userRequestedEnable
    case userOpenedSettings
    case userCancelled
    case permissionGranted(correlationID: String)
    case permissionDenied(correlationID: String)
    case permissionRequestFailed(IosNotificationRegistrationErrorClass, correlationID: String)
    case apnsDidRegister(token: Data, correlationID: String)
    case apnsDidFail(IosNotificationRegistrationErrorClass, correlationID: String)
    case authenticationBecameAvailable(sessionID: String)
    case authenticationBecameUnavailable
    case backendRegisterSucceeded(registrationID: String, correlationID: String)
    case backendRegisterFailed(IosNotificationRegistrationErrorClass, correlationID: String)
    case retryDue
    case logoutRequested
    case backendUnregisterSucceeded(alreadyAbsent: Bool, correlationID: String)
    case backendUnregisterFailed(IosNotificationRegistrationErrorClass, correlationID: String)
    case configurationInvalid
}

/// Immutable, presentation-safe projection of one installation actor.
/// Raw APNs tokens and credentials deliberately never enter this value.
struct IosNotificationRegistrationSnapshot: Equatable, Sendable {
    let installationID: String
    let state: IosNotificationRegistrationState
    let authorizationStatus: IosNotificationAuthorizationStatus?
    let authenticationSessionID: String?
    let hasUsableCredential: Bool
    let tokenFingerprint: String?
    let backendRegistrationID: String?
    let attempt: Int
    let nextRetryAt: Date?
    let lastErrorClass: IosNotificationRegistrationErrorClass?
    let logoutRequested: Bool
    let activeCorrelationID: String?

    static func initial(installationID: String) -> Self {
        Self(
            installationID: installationID,
            state: .checkingPermission,
            authorizationStatus: nil,
            authenticationSessionID: nil,
            hasUsableCredential: false,
            tokenFingerprint: nil,
            backendRegistrationID: nil,
            attempt: 0,
            nextRetryAt: nil,
            lastErrorClass: nil,
            logoutRequested: false,
            activeCorrelationID: nil
        )
    }
}

protocol NotificationPermissionPort: AnyObject {
    func readStatus(correlationID: String)
    func requestAuthorization(correlationID: String)
    func openSettings()
}

protocol RemoteNotificationRegistrationCallbackSink: AnyObject {
    func remoteNotificationRegistrationDidSucceed(token: Data, correlationID: String)
    func remoteNotificationRegistrationDidFail(
        errorClass: IosNotificationRegistrationErrorClass,
        correlationID: String
    )
}

protocol RemoteNotificationRegistrationPort: AnyObject {
    func register(correlationID: String, callbackSink: RemoteNotificationRegistrationCallbackSink)
}

struct BackendDeviceRegistrationRequest: Equatable, Sendable {
    let installationID: String
    let token: Data
    let topic: String
    let environment: String
    let authenticationSessionID: String
    let correlationID: String
}

struct BackendDeviceUnregistrationRequest: Equatable, Sendable {
    let installationID: String
    let backendRegistrationID: String
    let authenticationSessionID: String
    let correlationID: String
}

protocol BackendDeviceRegistrationPort: AnyObject {
    func register(_ request: BackendDeviceRegistrationRequest)
    func unregister(_ request: BackendDeviceUnregistrationRequest)
}

/// The auth shell must call this only after the adapter reaches `unregistered`.
protocol CredentialLifecyclePort: AnyObject {
    var authenticationSessionID: String? { get }
    var hasUsableCredential: Bool { get }
    func clearCredentialAfterPushUnregistered()
}

protocol RegistrationClockPort: AnyObject {
    var now: Date { get }
}

protocol RegistrationRetryScheduler: AnyObject {
    func scheduleRetry(at date: Date, event: IosNotificationRegistrationEvent)
    func cancelRetry()
}

/// Production seam for the reviewed machine. Transition implementation is intentionally
/// fail-closed until the RED behavioral suite is made green in task 4.1.
final class IosNotificationRegistrationAdapter: ObservableObject, RemoteNotificationRegistrationCallbackSink {
    @Published private(set) var snapshot: IosNotificationRegistrationSnapshot

    private let permission: NotificationPermissionPort
    private let remoteRegistration: RemoteNotificationRegistrationPort
    private let backend: BackendDeviceRegistrationPort
    private let credentials: CredentialLifecyclePort
    private let clock: RegistrationClockPort
    private let retryScheduler: RegistrationRetryScheduler
    private var correlationSequence = 0

    init(
        installationID: String,
        permission: NotificationPermissionPort,
        remoteRegistration: RemoteNotificationRegistrationPort,
        backend: BackendDeviceRegistrationPort,
        credentials: CredentialLifecyclePort,
        clock: RegistrationClockPort,
        retryScheduler: RegistrationRetryScheduler
    ) {
        snapshot = .initial(installationID: installationID)
        self.permission = permission
        self.remoteRegistration = remoteRegistration
        self.backend = backend
        self.credentials = credentials
        self.clock = clock
        self.retryScheduler = retryScheduler
    }

    func send(_ event: IosNotificationRegistrationEvent) {
        // Scaffold behavior only. Reading permission is safe and cannot display the system prompt.
        guard event == .appBecameActive else { return }
        let correlationID = nextCorrelationID()
        snapshot = replacingSnapshot(state: .checkingPermission, correlationID: correlationID)
        permission.readStatus(correlationID: correlationID)
    }

    func remoteNotificationRegistrationDidSucceed(token: Data, correlationID: String) {
        send(.apnsDidRegister(token: token, correlationID: correlationID))
    }

    func remoteNotificationRegistrationDidFail(
        errorClass: IosNotificationRegistrationErrorClass,
        correlationID: String
    ) {
        send(.apnsDidFail(errorClass, correlationID: correlationID))
    }

    private func nextCorrelationID() -> String {
        correlationSequence += 1
        return "registration-\(snapshot.installationID)-\(correlationSequence)"
    }

    private func replacingSnapshot(
        state: IosNotificationRegistrationState,
        correlationID: String?
    ) -> IosNotificationRegistrationSnapshot {
        IosNotificationRegistrationSnapshot(
            installationID: snapshot.installationID,
            state: state,
            authorizationStatus: snapshot.authorizationStatus,
            authenticationSessionID: snapshot.authenticationSessionID,
            hasUsableCredential: snapshot.hasUsableCredential,
            tokenFingerprint: snapshot.tokenFingerprint,
            backendRegistrationID: snapshot.backendRegistrationID,
            attempt: snapshot.attempt,
            nextRetryAt: snapshot.nextRetryAt,
            lastErrorClass: snapshot.lastErrorClass,
            logoutRequested: snapshot.logoutRequested,
            activeCorrelationID: correlationID
        )
    }
}

struct IosNotificationRegistrationPresentation: Equatable {
    let statusAccessibilityIdentifier: String
    let statusAccessibilityLabelKey: String
    let primaryActionAccessibilityIdentifier: String?
    let primaryActionAccessibilityLabelKey: String?
    let primaryEvent: IosNotificationRegistrationEvent?
}

final class IosNotificationRegistrationViewModel: ObservableObject {
    @Published private(set) var presentation: IosNotificationRegistrationPresentation
    private let adapter: IosNotificationRegistrationAdapter
    private var observation: AnyCancellable?

    init(adapter: IosNotificationRegistrationAdapter) {
        self.adapter = adapter
        presentation = Self.project(adapter.snapshot)
        observation = adapter.$snapshot.sink { [weak self] snapshot in
            self?.presentation = Self.project(snapshot)
        }
    }

    func performPrimaryAction() {
        guard let event = presentation.primaryEvent else { return }
        adapter.send(event)
    }

    static func project(_ snapshot: IosNotificationRegistrationSnapshot) -> IosNotificationRegistrationPresentation {
        switch snapshot.state {
        case .notDetermined:
            return .init(
                statusAccessibilityIdentifier: "notificationPermissionStatus",
                statusAccessibilityLabelKey: "notifications.system_permission.accessibility.status",
                primaryActionAccessibilityIdentifier: "notificationPermissionEnableButton",
                primaryActionAccessibilityLabelKey: "notifications.system_permission.accessibility.enable",
                primaryEvent: .userRequestedEnable
            )
        case .denied:
            return .init(
                statusAccessibilityIdentifier: "notificationPermissionStatus",
                statusAccessibilityLabelKey: "notifications.system_permission.accessibility.status",
                primaryActionAccessibilityIdentifier: "notificationPermissionOpenSettingsButton",
                primaryActionAccessibilityLabelKey: "notifications.system_permission.accessibility.open_settings",
                primaryEvent: .userOpenedSettings
            )
        case .retry:
            return .init(
                statusAccessibilityIdentifier: "notificationPermissionStatus",
                statusAccessibilityLabelKey: "notifications.system_permission.accessibility.status",
                primaryActionAccessibilityIdentifier: "notificationRegistrationRetryButton",
                primaryActionAccessibilityLabelKey: "notifications.system_permission.accessibility.retry",
                primaryEvent: .retryDue
            )
        case .misconfigured:
            return .init(
                statusAccessibilityIdentifier: "notificationRegistrationMisconfiguredStatus",
                statusAccessibilityLabelKey: "notifications.system_permission.accessibility.misconfigured",
                primaryActionAccessibilityIdentifier: nil,
                primaryActionAccessibilityLabelKey: nil,
                primaryEvent: nil
            )
        default:
            return .init(
                statusAccessibilityIdentifier: "notificationPermissionStatus",
                statusAccessibilityLabelKey: "notifications.system_permission.accessibility.status",
                primaryActionAccessibilityIdentifier: nil,
                primaryActionAccessibilityLabelKey: nil,
                primaryEvent: nil
            )
        }
    }
}
