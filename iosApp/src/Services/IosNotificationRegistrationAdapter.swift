import Combine
import CryptoKit
import Foundation

enum IosNotificationRegistrationEnvironment: String, Equatable, Sendable {
    case sandbox
    case production
}

struct IosNotificationRegistrationConfiguration: Equatable, Sendable {
    let topic: String
    let environment: IosNotificationRegistrationEnvironment

    init?(topic: String, environment: IosNotificationRegistrationEnvironment) {
        let normalizedTopic = topic.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !normalizedTopic.isEmpty else { return nil }
        self.topic = normalizedTopic
        self.environment = environment
    }
}

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
    case rawApnsTokenUnavailable(correlationID: String)
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
    var installationID: String
    var state: IosNotificationRegistrationState
    var authorizationStatus: IosNotificationAuthorizationStatus?
    var authenticationSessionID: String?
    var hasUsableCredential: Bool
    var tokenFingerprint: String?
    var backendRegistrationID: String?
    var attempt: Int
    var nextRetryAt: Date?
    var lastErrorClass: IosNotificationRegistrationErrorClass?
    var logoutRequested: Bool
    var activeCorrelationID: String?

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

protocol RawApnsTokenCustodyPort: AnyObject {
    func replace(_ token: Data)
    func withToken(_ operation: (Data) -> Void) -> Bool
    func clear()
}

struct BackendDeviceRegistrationRequest: Equatable, Sendable {
    let installationID: String
    let token: Data
    let topic: String
    let environment: String
    let authenticationSessionID: String
    let correlationID: String
}

enum IosBackendUnregistrationTarget: Equatable, Sendable {
    case registration(registrationID: String, installationID: String)
    case installation(installationID: String)
}

struct BackendDeviceUnregistrationRequest: Equatable, Sendable {
    let target: IosBackendUnregistrationTarget
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

protocol RegistrationRetryJitterSource: AnyObject {
    /// Returns a sample in the closed unit interval. The adapter still clamps
    /// the value so a custom source cannot schedule outside the modelled cap.
    func nextUnitInterval() -> Double
}

final class SystemRegistrationRetryJitterSource: RegistrationRetryJitterSource {
    private var generator = SystemRandomNumberGenerator()

    func nextUnitInterval() -> Double {
        Double.random(in: 0...1, using: &generator)
    }
}

enum RegistrationRetryBackoff {
    private static let maximumDelay: TimeInterval = 60

    static func fullJitterDelay(attempt: Int, unitInterval: Double) -> TimeInterval {
        let boundedSample = unitInterval.isFinite
            ? min(1.0, max(0.0, unitInterval))
            : 0.0
        let exponent = max(1, attempt) - 1

        // 2^6 already exceeds the reviewed 60-second ceiling. Saturating
        // before exponentiation also keeps extreme attempt values overflow-safe.
        let exponentialCap: TimeInterval = exponent >= 6
            ? maximumDelay
            : TimeInterval(1 << exponent)
        return min(maximumDelay, exponentialCap) * boundedSample
    }
}

final class IosNotificationRegistrationAdapter: ObservableObject, RemoteNotificationRegistrationCallbackSink {
    @Published private(set) var snapshot: IosNotificationRegistrationSnapshot

    private let permission: NotificationPermissionPort
    private let remoteRegistration: RemoteNotificationRegistrationPort
    private let backend: BackendDeviceRegistrationPort
    private let credentials: CredentialLifecyclePort
    private let clock: RegistrationClockPort
    private let retryScheduler: RegistrationRetryScheduler
    private let retryJitterSource: RegistrationRetryJitterSource
    private let configuration: IosNotificationRegistrationConfiguration
    private let tokenCustody: RawApnsTokenCustodyPort
    private var correlationSequence = 0
    private var resumeState: IosNotificationRegistrationState = .checkingPermission
    private let maxAttempts = 3
    private(set) var staleCallbackAuditCount = 0

    init(
        installationID: String,
        permission: NotificationPermissionPort,
        remoteRegistration: RemoteNotificationRegistrationPort,
        backend: BackendDeviceRegistrationPort,
        credentials: CredentialLifecyclePort,
        clock: RegistrationClockPort,
        retryScheduler: RegistrationRetryScheduler,
        retryJitterSource: RegistrationRetryJitterSource,
        configuration: IosNotificationRegistrationConfiguration,
        tokenCustody: RawApnsTokenCustodyPort
    ) {
        self.permission = permission
        self.remoteRegistration = remoteRegistration
        self.backend = backend
        self.credentials = credentials
        self.clock = clock
        self.retryScheduler = retryScheduler
        self.retryJitterSource = retryJitterSource
        self.configuration = configuration
        self.tokenCustody = tokenCustody

        var initialSnapshot = IosNotificationRegistrationSnapshot.initial(installationID: installationID)
        initialSnapshot.authenticationSessionID = credentials.authenticationSessionID
        initialSnapshot.hasUsableCredential = credentials.hasUsableCredential
        snapshot = initialSnapshot
    }

    func send(_ event: IosNotificationRegistrationEvent) {
        switch snapshot.state {
        case .checkingPermission:
            handleCheckingPermission(event)
        case .notDetermined:
            handleNotDetermined(event)
        case .requestingPermission:
            handleRequestingPermission(event)
        case .denied:
            handleDenied(event)
        case .registeringApns:
            handleRegisteringApns(event)
        case .awaitingAuthentication:
            handleAwaitingAuthentication(event)
        case .registeringBackend:
            handleRegisteringBackend(event)
        case .retry:
            handleRetry(event)
        case .registered:
            handleRegistered(event)
        case .unregistering:
            handleUnregistering(event)
        case .unregistered, .cancelled, .misconfigured:
            break
        }
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

    private func handleCheckingPermission(_ event: IosNotificationRegistrationEvent) {
        switch event {
        case .appBecameActive:
            transition(to: .checkingPermission)
        case let .permissionStatusResolved(status, correlationID):
            guard requireCurrentCorrelation(correlationID) else { return }
            if allowsRemoteRegistration(status) {
                transition(to: .registeringApns) { $0.authorizationStatus = status }
            } else if status == .notDetermined {
                transition(to: .notDetermined) { $0.authorizationStatus = status }
            } else if status == .denied {
                transition(to: .denied) { $0.authorizationStatus = status }
            }
        case let .permissionStatusFailed(errorClass, correlationID):
            guard requireCurrentCorrelation(correlationID) else { return }
            handleRecoverableFailure(errorClass, resumeAt: .checkingPermission)
        case .logoutRequested:
            startLogoutFlight()
        case .configurationInvalid:
            transitionToMisconfigured()
        default:
            break
        }
    }

    private func handleNotDetermined(_ event: IosNotificationRegistrationEvent) {
        switch event {
        case .userRequestedEnable:
            transition(to: .requestingPermission)
        case .userCancelled:
            transition(to: .cancelled)
        case .logoutRequested:
            startLogoutFlight()
        case .appBecameActive:
            transition(to: .checkingPermission)
        default:
            break
        }
    }

    private func handleRequestingPermission(_ event: IosNotificationRegistrationEvent) {
        switch event {
        case let .permissionGranted(correlationID):
            guard requireCurrentCorrelation(correlationID) else { return }
            transition(to: .registeringApns) { $0.authorizationStatus = .authorized }
        case let .permissionDenied(correlationID):
            guard requireCurrentCorrelation(correlationID) else { return }
            transition(to: .denied) { $0.authorizationStatus = .denied }
        case let .permissionRequestFailed(errorClass, correlationID):
            guard requireCurrentCorrelation(correlationID) else { return }
            handleRecoverableFailure(errorClass, resumeAt: .requestingPermission)
        case .logoutRequested:
            startLogoutFlight()
        case .configurationInvalid:
            transitionToMisconfigured()
        default:
            break
        }
    }

    private func handleDenied(_ event: IosNotificationRegistrationEvent) {
        switch event {
        case .userOpenedSettings:
            permission.openSettings()
        case .appBecameActive:
            transition(to: .checkingPermission)
        case .userCancelled:
            transition(to: .cancelled)
        case .logoutRequested:
            startLogoutFlight()
        default:
            break
        }
    }

    private func handleRegisteringApns(_ event: IosNotificationRegistrationEvent) {
        switch event {
        case let .apnsDidRegister(token, correlationID):
            guard requireCurrentCorrelation(correlationID) else { return }
            tokenCustody.replace(token)
            let fingerprint = tokenFingerprint(token)
            let destination: IosNotificationRegistrationState = hasUsableAuthentication
                ? .registeringBackend
                : .awaitingAuthentication
            transition(to: destination) {
                $0.tokenFingerprint = fingerprint
            }
        case let .apnsDidFail(errorClass, correlationID):
            guard requireCurrentCorrelation(correlationID) else { return }
            handleRecoverableFailure(errorClass, resumeAt: .registeringApns)
        case .logoutRequested:
            startLogoutFlight()
        case .configurationInvalid:
            transitionToMisconfigured()
        default:
            break
        }
    }

    private func handleAwaitingAuthentication(_ event: IosNotificationRegistrationEvent) {
        switch event {
        case let .authenticationBecameAvailable(sessionID):
            transition(to: .registeringBackend) {
                $0.authenticationSessionID = sessionID
                $0.hasUsableCredential = true
            }
        case let .apnsDidRegister(token, correlationID):
            guard requireCurrentCorrelation(correlationID) else { return }
            tokenCustody.replace(token)
            updateSnapshot {
                $0.tokenFingerprint = tokenFingerprint(token)
            }
        case .userCancelled:
            transition(to: .cancelled)
        case .logoutRequested:
            startLogoutFlight()
        default:
            break
        }
    }

    private func handleRegisteringBackend(_ event: IosNotificationRegistrationEvent) {
        switch event {
        case let .rawApnsTokenUnavailable(correlationID):
            guard requireCurrentCorrelation(correlationID) else { return }
            transitionToMisconfigured()
        case let .backendRegisterSucceeded(registrationID, correlationID):
            guard requireCurrentCorrelation(correlationID) else { return }
            tokenCustody.clear()
            transition(to: .registered) {
                $0.backendRegistrationID = registrationID
                $0.attempt = 0
                $0.nextRetryAt = nil
                $0.lastErrorClass = nil
            }
        case let .backendRegisterFailed(errorClass, correlationID):
            guard requireCurrentCorrelation(correlationID) else { return }
            if errorClass == .backend {
                transition(to: .awaitingAuthentication) {
                    $0.lastErrorClass = errorClass
                    $0.nextRetryAt = nil
                }
            } else {
                handleRecoverableFailure(errorClass, resumeAt: .registeringBackend)
            }
        case .authenticationBecameUnavailable:
            transition(to: .awaitingAuthentication) {
                $0.authenticationSessionID = nil
                $0.hasUsableCredential = false
                $0.lastErrorClass = .backend
                $0.nextRetryAt = nil
            }
        case .logoutRequested:
            startLogoutFlight()
        case .configurationInvalid:
            transitionToMisconfigured()
        default:
            break
        }
    }

    private func handleRetry(_ event: IosNotificationRegistrationEvent) {
        switch event {
        case .retryDue:
            if snapshot.logoutRequested,
               resumeState == .unregistering,
               !hasUsableAuthentication {
                updateSnapshot {
                    $0.lastErrorClass = .backend
                    $0.nextRetryAt = nil
                }
                return
            }
            retryScheduler.cancelRetry()
            if snapshot.attempt < maxAttempts, resumeState == .unregistering, hasUsableAuthentication {
                transition(to: .unregistering)
            } else if snapshot.attempt < maxAttempts, resumeState == .registeringBackend, hasUsableAuthentication {
                transition(to: .registeringBackend)
            } else if snapshot.attempt < maxAttempts, resumeState == .registeringApns {
                transition(to: .registeringApns)
            } else if snapshot.attempt < maxAttempts, resumeState == .requestingPermission {
                transition(to: .requestingPermission)
            } else if snapshot.attempt < maxAttempts {
                transition(to: .checkingPermission)
            } else {
                transitionToMisconfigured()
            }
        case let .authenticationBecameAvailable(sessionID):
            updateSnapshot {
                $0.authenticationSessionID = sessionID
                $0.hasUsableCredential = true
            }
            if snapshot.logoutRequested, resumeState == .unregistering {
                retryScheduler.cancelRetry()
                transition(to: .unregistering)
            }
        case .authenticationBecameUnavailable:
            updateSnapshot {
                $0.authenticationSessionID = nil
                $0.hasUsableCredential = false
                $0.lastErrorClass = .backend
            }
        case .appBecameActive where !snapshot.logoutRequested:
            retryScheduler.cancelRetry()
            transition(to: .checkingPermission)
        case .userCancelled where !snapshot.logoutRequested:
            retryScheduler.cancelRetry()
            transition(to: .cancelled)
        case .logoutRequested:
            guard !snapshot.logoutRequested else { return }
            startLogoutFlight()
        case .configurationInvalid:
            transitionToMisconfigured()
        default:
            break
        }
    }

    private func handleRegistered(_ event: IosNotificationRegistrationEvent) {
        switch event {
        case let .apnsDidRegister(token, correlationID):
            guard requireCurrentCorrelation(correlationID) else { return }
            tokenCustody.replace(token)
            let fingerprint = tokenFingerprint(token)
            transition(to: .registeringBackend) {
                $0.tokenFingerprint = fingerprint
            }
        case .authenticationBecameUnavailable:
            updateSnapshot {
                $0.authenticationSessionID = nil
                $0.hasUsableCredential = false
                $0.lastErrorClass = .backend
            }
        case .logoutRequested:
            startLogoutFlight()
        case .appBecameActive:
            transition(to: .checkingPermission)
        default:
            break
        }
    }

    private func handleUnregistering(_ event: IosNotificationRegistrationEvent) {
        switch event {
        case let .backendUnregisterSucceeded(_, correlationID):
            guard requireCurrentCorrelation(correlationID) else { return }
            transition(to: .unregistered) {
                $0.backendRegistrationID = nil
                $0.tokenFingerprint = nil
                $0.authenticationSessionID = nil
                $0.hasUsableCredential = false
                $0.attempt = 0
                $0.nextRetryAt = nil
                $0.lastErrorClass = nil
            }
        case let .backendUnregisterFailed(errorClass, correlationID):
            guard requireCurrentCorrelation(correlationID) else { return }
            if errorClass == .network || errorClass == .backend {
                resumeState = .unregistering
                enterRetry(errorClass: errorClass)
            } else {
                transitionToMisconfigured(errorClass: errorClass)
            }
        case .logoutRequested:
            break
        case .configurationInvalid:
            transitionToMisconfigured()
        default:
            break
        }
    }

    private func transition(
        to state: IosNotificationRegistrationState,
        update: (inout IosNotificationRegistrationSnapshot) -> Void = { _ in }
    ) {
        updateSnapshot {
            $0.state = state
            update(&$0)
        }
        performEntryEffects(for: state)
    }

    private func performEntryEffects(for state: IosNotificationRegistrationState) {
        switch state {
        case .checkingPermission:
            let correlationID = beginInvocation()
            permission.readStatus(correlationID: correlationID)
        case .requestingPermission:
            let correlationID = beginInvocation()
            permission.requestAuthorization(correlationID: correlationID)
        case .registeringApns:
            let correlationID = beginInvocation()
            remoteRegistration.register(correlationID: correlationID, callbackSink: self)
        case .registeringBackend:
            guard let authenticationSessionID = snapshot.authenticationSessionID,
                  snapshot.hasUsableCredential else {
                transition(to: .awaitingAuthentication)
                return
            }
            let correlationID = beginInvocation()
            let tokenWasAvailable = tokenCustody.withToken { [backend, configuration, installationID = snapshot.installationID] token in
                backend.register(BackendDeviceRegistrationRequest(
                    installationID: installationID,
                    token: token,
                    topic: configuration.topic,
                    environment: configuration.environment.rawValue,
                    authenticationSessionID: authenticationSessionID,
                    correlationID: correlationID
                ))
            }
            if !tokenWasAvailable {
                send(.rawApnsTokenUnavailable(correlationID: correlationID))
            }
        case .retry:
            if let nextRetryAt = snapshot.nextRetryAt {
                retryScheduler.scheduleRetry(at: nextRetryAt, event: .retryDue)
            }
        case .unregistering:
            guard let authenticationSessionID = snapshot.authenticationSessionID,
                  snapshot.hasUsableCredential else {
                resumeState = .unregistering
                enterRetry(errorClass: .backend, incrementAttempt: false, logoutRequested: true)
                return
            }
            let correlationID = beginInvocation()
            backend.unregister(BackendDeviceUnregistrationRequest(
                target: backendUnregistrationTarget,
                authenticationSessionID: authenticationSessionID,
                correlationID: correlationID
            ))
        case .unregistered:
            retryScheduler.cancelRetry()
            tokenCustody.clear()
            credentials.clearCredentialAfterPushUnregistered()
        case .cancelled, .misconfigured:
            retryScheduler.cancelRetry()
            tokenCustody.clear()
        case .notDetermined, .denied, .awaitingAuthentication, .registered:
            break
        }
    }

    private func handleRecoverableFailure(
        _ errorClass: IosNotificationRegistrationErrorClass,
        resumeAt state: IosNotificationRegistrationState
    ) {
        if errorClass == .network {
            resumeState = state
            enterRetry(errorClass: errorClass)
        } else if errorClass == .configuration || errorClass == .permission {
            transitionToMisconfigured(errorClass: errorClass)
        } else {
            auditStaleCallback()
        }
    }

    private func enterRetry(
        errorClass: IosNotificationRegistrationErrorClass,
        incrementAttempt: Bool = true,
        logoutRequested: Bool? = nil
    ) {
        let attempt = snapshot.attempt + (incrementAttempt ? 1 : 0)
        let backoffAttempt = max(1, attempt)
        let delay = RegistrationRetryBackoff.fullJitterDelay(
            attempt: backoffAttempt,
            unitInterval: retryJitterSource.nextUnitInterval()
        )
        let retryAt = clock.now.addingTimeInterval(delay)
        transition(to: .retry) {
            $0.attempt = attempt
            $0.lastErrorClass = errorClass
            $0.nextRetryAt = retryAt
            if let logoutRequested { $0.logoutRequested = logoutRequested }
        }
    }

    private func startLogoutFlight() {
        retryScheduler.cancelRetry()
        resumeState = .unregistering
        if hasUsableAuthentication {
            transition(to: .unregistering) {
                $0.logoutRequested = true
                $0.attempt = 0
                $0.lastErrorClass = nil
                $0.nextRetryAt = nil
            }
        } else {
            updateSnapshot {
                $0.attempt = 0
                $0.lastErrorClass = .backend
                $0.nextRetryAt = nil
            }
            enterRetry(errorClass: .backend, incrementAttempt: false, logoutRequested: true)
        }
    }

    private func transitionToMisconfigured(
        errorClass: IosNotificationRegistrationErrorClass = .configuration
    ) {
        transition(to: .misconfigured) {
            $0.lastErrorClass = errorClass
            $0.nextRetryAt = nil
        }
    }

    private func beginInvocation() -> String {
        let correlationID = nextCorrelationID()
        updateSnapshot { $0.activeCorrelationID = correlationID }
        return correlationID
    }

    private func requireCurrentCorrelation(_ correlationID: String) -> Bool {
        guard snapshot.activeCorrelationID == correlationID else {
            auditStaleCallback()
            return false
        }
        return true
    }

    private func auditStaleCallback() {
        staleCallbackAuditCount += 1
    }

    private var hasUsableAuthentication: Bool {
        snapshot.authenticationSessionID != nil && snapshot.hasUsableCredential
    }

    private var backendUnregistrationTarget: IosBackendUnregistrationTarget {
        if let registrationID = snapshot.backendRegistrationID {
            return .registration(registrationID: registrationID, installationID: snapshot.installationID)
        }
        return .installation(installationID: snapshot.installationID)
    }

    private func allowsRemoteRegistration(_ status: IosNotificationAuthorizationStatus) -> Bool {
        status == .authorized || status == .provisional || status == .ephemeral
    }

    private func tokenFingerprint(_ token: Data) -> String {
        SHA256.hash(data: token).map { String(format: "%02x", $0) }.joined()
    }

    private func updateSnapshot(_ update: (inout IosNotificationRegistrationSnapshot) -> Void) {
        var next = snapshot
        update(&next)
        snapshot = next
    }
}

struct IosNotificationRegistrationPresentation: Equatable {
    let statusAccessibilityIdentifier: String
    let statusAccessibilityLabelKey: String
    let statusDescriptionKey: String
    let statusAccessibilityHintKey: String
    let primaryActionTitleKey: String?
    let primaryActionAccessibilityIdentifier: String?
    let primaryActionAccessibilityLabelKey: String?
    let primaryActionAccessibilityHintKey: String?
    let primaryEvent: IosNotificationRegistrationEvent?
}

struct IosNotificationRegistrationControlSemantics: Equatable {
    let minimumHitTargetPoints: Int
    let supportsDynamicType: Bool
    let decorativeIconHiddenFromVoiceOver: Bool
    let accessibilityIdentifier: String?
    let accessibilityLabelKey: String?
    let accessibilityHintKey: String?
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
                statusDescriptionKey: "notifications.system_permission.not_determined",
                statusAccessibilityHintKey: "notifications.system_permission.accessibility.status_hint",
                primaryActionTitleKey: "notifications.system_permission.enable",
                primaryActionAccessibilityIdentifier: "notificationPermissionEnableButton",
                primaryActionAccessibilityLabelKey: "notifications.system_permission.accessibility.enable",
                primaryActionAccessibilityHintKey: "notifications.system_permission.accessibility.enable_hint",
                primaryEvent: .userRequestedEnable
            )
        case .denied:
            return .init(
                statusAccessibilityIdentifier: "notificationPermissionStatus",
                statusAccessibilityLabelKey: "notifications.system_permission.accessibility.status",
                statusDescriptionKey: "notifications.system_permission.denied",
                statusAccessibilityHintKey: "notifications.system_permission.accessibility.status_hint",
                primaryActionTitleKey: "notifications.system_permission.open_settings",
                primaryActionAccessibilityIdentifier: "notificationPermissionOpenSettingsButton",
                primaryActionAccessibilityLabelKey: "notifications.system_permission.accessibility.open_settings",
                primaryActionAccessibilityHintKey: "notifications.system_permission.accessibility.open_settings_hint",
                primaryEvent: .userOpenedSettings
            )
        case .registered:
            return .init(
                statusAccessibilityIdentifier: "notificationRegistrationRegisteredStatus",
                statusAccessibilityLabelKey: "notifications.system_permission.accessibility.registered",
                statusDescriptionKey: "notifications.system_permission.authorized",
                statusAccessibilityHintKey: "notifications.system_permission.accessibility.registered_hint",
                primaryActionTitleKey: nil,
                primaryActionAccessibilityIdentifier: nil,
                primaryActionAccessibilityLabelKey: nil,
                primaryActionAccessibilityHintKey: nil,
                primaryEvent: nil
            )
        case .retry:
            return .init(
                statusAccessibilityIdentifier: "notificationRegistrationRetryStatus",
                statusAccessibilityLabelKey: "notifications.system_permission.accessibility.retry",
                statusDescriptionKey: "notifications.system_permission.retry",
                statusAccessibilityHintKey: "notifications.system_permission.accessibility.retry_hint",
                primaryActionTitleKey: nil,
                primaryActionAccessibilityIdentifier: nil,
                primaryActionAccessibilityLabelKey: nil,
                primaryActionAccessibilityHintKey: nil,
                primaryEvent: nil
            )
        case .misconfigured:
            return .init(
                statusAccessibilityIdentifier: "notificationRegistrationMisconfiguredStatus",
                statusAccessibilityLabelKey: "notifications.system_permission.accessibility.misconfigured",
                statusDescriptionKey: "notifications.system_permission.misconfigured",
                statusAccessibilityHintKey: "notifications.system_permission.accessibility.misconfigured_hint",
                primaryActionTitleKey: nil,
                primaryActionAccessibilityIdentifier: nil,
                primaryActionAccessibilityLabelKey: nil,
                primaryActionAccessibilityHintKey: nil,
                primaryEvent: nil
            )
        case .unregistering:
            return .init(
                statusAccessibilityIdentifier: "notificationRegistrationUnregisteringStatus",
                statusAccessibilityLabelKey: "notifications.system_permission.accessibility.unregistering",
                statusDescriptionKey: "notifications.system_permission.unregistering",
                statusAccessibilityHintKey: "notifications.system_permission.accessibility.unregistering_hint",
                primaryActionTitleKey: nil,
                primaryActionAccessibilityIdentifier: nil,
                primaryActionAccessibilityLabelKey: nil,
                primaryActionAccessibilityHintKey: nil,
                primaryEvent: nil
            )
        case .registeringApns, .awaitingAuthentication, .registeringBackend:
            return .init(
                statusAccessibilityIdentifier: "notificationPermissionStatus",
                statusAccessibilityLabelKey: "notifications.system_permission.accessibility.status",
                statusDescriptionKey: "notifications.system_permission.authorized",
                statusAccessibilityHintKey: "notifications.system_permission.accessibility.status_hint",
                primaryActionTitleKey: nil,
                primaryActionAccessibilityIdentifier: nil,
                primaryActionAccessibilityLabelKey: nil,
                primaryActionAccessibilityHintKey: nil,
                primaryEvent: nil
            )
        case .checkingPermission, .requestingPermission, .unregistered, .cancelled:
            return .init(
                statusAccessibilityIdentifier: "notificationPermissionStatus",
                statusAccessibilityLabelKey: "notifications.system_permission.accessibility.status",
                statusDescriptionKey: "notifications.system_permission.not_determined",
                statusAccessibilityHintKey: "notifications.system_permission.accessibility.status_hint",
                primaryActionTitleKey: nil,
                primaryActionAccessibilityIdentifier: nil,
                primaryActionAccessibilityLabelKey: nil,
                primaryActionAccessibilityHintKey: nil,
                primaryEvent: nil
            )
        }
    }

    static func controlSemantics(
        for presentation: IosNotificationRegistrationPresentation
    ) -> IosNotificationRegistrationControlSemantics {
        IosNotificationRegistrationControlSemantics(
            minimumHitTargetPoints: 44,
            supportsDynamicType: true,
            decorativeIconHiddenFromVoiceOver: true,
            accessibilityIdentifier: presentation.primaryActionAccessibilityIdentifier,
            accessibilityLabelKey: presentation.primaryActionAccessibilityLabelKey,
            accessibilityHintKey: presentation.primaryActionAccessibilityHintKey
        )
    }
}
