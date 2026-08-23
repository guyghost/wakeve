//
//  APNsService.swift
//  iosApp
//
//  Created by Wakeve Team
//  Handles Apple Push Notification service integration
//

import Combine
import Foundation
import UserNotifications
import UIKit

private final class InMemoryRawApnsTokenCustody: RawApnsTokenCustodyPort {
    private let lock = NSLock()
    private var token: Data?

    func replace(_ token: Data) {
        lock.lock()
        self.token = token
        lock.unlock()
    }

    func withToken(_ operation: (Data) -> Void) -> Bool {
        lock.lock()
        let availableToken = token
        lock.unlock()
        guard let availableToken else { return false }
        operation(availableToken)
        return true
    }

    func clear() {
        lock.lock()
        token = nil
        lock.unlock()
    }
}

final class UIKitAPNsRegistrationCoordinator {
    private struct Invocation {
        let correlationID: String
        let callbackSink: RemoteNotificationRegistrationCallbackSink
    }

    private let lock = NSLock()
    private let startRegistration: (String) -> Void
    private var activeInvocations: [Invocation] = []

    init(startRegistration: @escaping (String) -> Void) {
        self.startRegistration = startRegistration
    }

    func enqueue(correlationID: String, callbackSink: RemoteNotificationRegistrationCallbackSink) {
        let invocation = Invocation(correlationID: correlationID, callbackSink: callbackSink)
        let correlationToStart: String?

        lock.lock()
        if activeInvocations.isEmpty {
            activeInvocations = [invocation]
            correlationToStart = correlationID
        } else {
            activeInvocations.append(invocation)
            correlationToStart = nil
        }
        lock.unlock()

        if let correlationToStart {
            startRegistration(correlationToStart)
        }
    }

    @discardableResult
    func completeSuccess(token: Data, correlationID: String) -> Bool {
        complete(correlationID: correlationID) { invocation in
            invocation.callbackSink.remoteNotificationRegistrationDidSucceed(
                token: token,
                correlationID: invocation.correlationID
            )
        }
    }

    @discardableResult
    func completeFailure(
        errorClass: IosNotificationRegistrationErrorClass,
        correlationID: String
    ) -> Bool {
        complete(correlationID: correlationID) { invocation in
            invocation.callbackSink.remoteNotificationRegistrationDidFail(
                errorClass: errorClass,
                correlationID: invocation.correlationID
            )
        }
    }

    @discardableResult
    func completeCurrentSuccess(token: Data) -> Bool {
        guard let correlationID = currentCorrelationID else { return false }
        return completeSuccess(token: token, correlationID: correlationID)
    }

    @discardableResult
    func completeCurrentFailure(errorClass: IosNotificationRegistrationErrorClass) -> Bool {
        guard let correlationID = currentCorrelationID else { return false }
        return completeFailure(errorClass: errorClass, correlationID: correlationID)
    }

    private var currentCorrelationID: String? {
        lock.lock()
        defer { lock.unlock() }
        return activeInvocations.first?.correlationID
    }

    private func complete(
        correlationID: String,
        delivery: (Invocation) -> Void
    ) -> Bool {
        let completed: [Invocation]

        lock.lock()
        guard activeInvocations.first?.correlationID == correlationID else {
            lock.unlock()
            return false
        }
        completed = activeInvocations
        activeInvocations.removeAll(keepingCapacity: true)
        lock.unlock()

        completed.forEach(delivery)
        return true
    }
}

enum APNsBackendFailureClassifier {
    static func classify(
        statusCode: Int?,
        networkFailure: Bool
    ) -> IosNotificationRegistrationErrorClass {
        if networkFailure { return .network }
        guard let statusCode else { return .configuration }
        if statusCode == 429 || statusCode == 408 || (500..<600).contains(statusCode) {
            return .network
        }
        if statusCode == 401 || statusCode == 403 {
            return .backend
        }
        return .configuration
    }
}

struct BackendDeviceRegistrationHttpRequest: Equatable, Sendable {
    let method: String
    let path: String
    let body: [String: String]?
}

enum BackendDeviceUnregistrationHttpOutcome: Equatable, Sendable {
    case succeeded(alreadyAbsent: Bool)
    case authenticationFailure
    case transientFailure
    case configurationFailure
    case notOwnedOrMissing
}

enum BackendDeviceRegistrationHttpContract {
    static func unregistrationRequest(
        for target: IosBackendUnregistrationTarget
    ) -> BackendDeviceRegistrationHttpRequest {
        switch target {
        case let .registration(registrationID, _):
            var componentAllowed = CharacterSet.alphanumerics
            componentAllowed.formUnion(CharacterSet(charactersIn: "-._~"))
            let encodedRegistrationID = registrationID.addingPercentEncoding(
                withAllowedCharacters: componentAllowed
            ) ?? ""
            return BackendDeviceRegistrationHttpRequest(
                method: "DELETE",
                path: "/notifications/registrations/\(encodedRegistrationID)",
                body: nil
            )
        case let .installation(installationID):
            return BackendDeviceRegistrationHttpRequest(
                method: "DELETE",
                path: "/notifications/unregister",
                body: ["installationId": installationID]
            )
        }
    }

    static func classifyUnregistrationResponse(
        statusCode: Int,
        alreadyAbsent: Bool
    ) -> BackendDeviceUnregistrationHttpOutcome {
        if (200..<300).contains(statusCode) {
            return .succeeded(alreadyAbsent: alreadyAbsent)
        }
        if statusCode == 401 || statusCode == 403 {
            return .authenticationFailure
        }
        if statusCode == 404 {
            return .notOwnedOrMissing
        }
        if statusCode == 408 || statusCode == 429 || (500..<600).contains(statusCode) {
            return .transientFailure
        }
        return .configurationFailure
    }
}

final class APNsAuthorizationRegistrationWrapper {
    private let status: () -> IosNotificationAuthorizationStatus
    private let requestAuthorization: () -> Void
    private let registerForRemoteNotifications: () -> Void

    init(
        status: @escaping () -> IosNotificationAuthorizationStatus,
        requestAuthorization: @escaping () -> Void,
        registerForRemoteNotifications: @escaping () -> Void
    ) {
        self.status = status
        self.requestAuthorization = requestAuthorization
        self.registerForRemoteNotifications = registerForRemoteNotifications
    }

    func requestAuthorizationAndRegister(completion: @escaping (Bool, Error?) -> Void) {
        switch status() {
        case .notDetermined:
            requestAuthorization()
        case .authorized, .provisional, .ephemeral:
            registerForRemoteNotifications()
            completion(true, nil)
        case .denied:
            completion(false, nil)
        }
    }
}

struct SanitizedNotificationHistoryRecord: Codable, Equatable, Sendable {
    let notificationID: String
    let eventID: String?
    let receivedAtEpochMilliseconds: Int64
}

protocol NotificationHistoryPersistencePort: AnyObject {
    func persist(_ record: SanitizedNotificationHistoryRecord)
}

private final class LocalSanitizedNotificationHistoryPersistence: NotificationHistoryPersistencePort {
    private let lock = NSLock()
    private let defaults: UserDefaults
    private let storageKey = "wakeve.notification.history.sanitized.v1"
    private let maximumRecordCount = 100

    init(defaults: UserDefaults = .standard) {
        self.defaults = defaults
    }

    func persist(_ record: SanitizedNotificationHistoryRecord) {
        lock.lock()
        defer { lock.unlock() }

        let decoder = JSONDecoder()
        var records = defaults.data(forKey: storageKey)
            .flatMap { try? decoder.decode([SanitizedNotificationHistoryRecord].self, from: $0) }
            ?? []
        records.removeAll { $0.notificationID == record.notificationID }
        records.append(record)
        if records.count > maximumRecordCount {
            records.removeFirst(records.count - maximumRecordCount)
        }
        if let encoded = try? JSONEncoder().encode(records) {
            defaults.set(encoded, forKey: storageKey)
        }
    }
}

final class CredentialLifecycleObserverRegistry {
    private final class WeakObserver {
        weak var value: CredentialLifecyclePort?

        init(_ value: CredentialLifecyclePort) {
            self.value = value
        }
    }

    private var observers: [WeakObserver] = []

    func bind(_ observer: CredentialLifecyclePort) {
        observers.removeAll { $0.value == nil }
        guard !observers.contains(where: { entry in
            guard let value = entry.value else { return false }
            return value === observer
        }) else { return }
        observers.append(WeakObserver(observer))
    }

    func notifyPushUnregistered() {
        let liveObservers = observers.compactMap(\.value)
        observers = liveObservers.map(WeakObserver.init)
        liveObservers.forEach { $0.clearCredentialAfterPushUnregistered() }
    }
}

/**
 * Service for managing Apple Push Notifications.
 * Handles permission requests, token registration with the backend, and notification display.
 *
 * Token registration flow:
 * 1. A user-facing settings/onboarding action requests notification permission
 * 2. System calls didRegisterForRemoteNotifications with device token
 * 3. The correlated adapter places the raw token in private in-memory custody
 * 4. The adapter sends a scoped registration through the authenticated backend port
 * 5. Logout unregisters one reviewed registration/installation target before credentials clear
 */
@MainActor
@objc public class APNsService: NSObject, @preconcurrency NotificationPermissionPort,
    @preconcurrency RemoteNotificationRegistrationPort, @preconcurrency BackendDeviceRegistrationPort,
    @preconcurrency CredentialLifecyclePort, @preconcurrency RegistrationClockPort,
    @preconcurrency RegistrationRetryScheduler, @preconcurrency PushUnregistrationPort {

    public static let shared = APNsService()
    private override init() {
        super.init()
    }

    // Notification center delegate
    private let notificationCenter = UNUserNotificationCenter.current()

    /// Compatibility projection. It exposes only the reviewed fingerprint, never raw token material.
    var currentDeviceToken: String? {
        registrationAdapter?.snapshot.tokenFingerprint
    }

    /// Callback for when notification is received in foreground
    public var onNotificationReceived: (([AnyHashable: Any]) -> Void)?

    /// Callback for when user taps a notification (deep link data)
    public var onNotificationTapped: (([AnyHashable: Any]) -> Void)?

    // MARK: - API Configuration

    private var baseUrl: String {
        #if DEBUG
        return "http://localhost:8080/api"
        #else
        return "https://api.wakeve.app/api"
        #endif
    }

    private let session: URLSession = {
        let config = URLSessionConfiguration.default
        config.timeoutIntervalForRequest = 15
        return URLSession(configuration: config)
    }()

    private let tokenCustody = InMemoryRawApnsTokenCustody()
    private let notificationHistory: NotificationHistoryPersistencePort =
        LocalSanitizedNotificationHistoryPersistence()
    private var boundRegistrationAdapter: IosNotificationRegistrationAdapter?
    private var activeAccessToken: String?
    private var activeAuthenticationSessionID: String?
    private var authenticationSequence = 0
    private var retryWorkItem: DispatchWorkItem?
    private var cachedAuthorizationStatus: IosNotificationAuthorizationStatus = .notDetermined
    private var authorizationCompletions: [(Bool, Error?) -> Void] = []
    private var unregistrationCompletions: [UUID: (Bool, Error?) -> Void] = [:]
    private var unregistrationBootstrapInFlight = false
    private var registrationObservation: AnyCancellable?
    private let credentialLifecycleObservers = CredentialLifecycleObserverRegistry()

    private lazy var uikitRegistrationCoordinator = UIKitAPNsRegistrationCoordinator { _ in
        Task { @MainActor in
            UIApplication.shared.registerForRemoteNotifications()
        }
    }

    private lazy var authorizationRegistrationWrapper = APNsAuthorizationRegistrationWrapper(
        status: { [weak self] in
            self?.cachedAuthorizationStatus ?? .denied
        },
        requestAuthorization: { [weak self] in
            self?.registrationAdapter?.send(.userRequestedEnable)
        },
        registerForRemoteNotifications: { [weak self] in
            self?.applicationBecameActive()
        }
    )

    private lazy var ownedRegistrationAdapter: IosNotificationRegistrationAdapter? = {
        guard let topic = Bundle.main.bundleIdentifier,
              let configuration = IosNotificationRegistrationConfiguration(
                  topic: topic,
                  environment: Self.apnsEnvironment
              ) else {
            return nil
        }
        let adapter = IosNotificationRegistrationAdapter(
            installationID: Self.stableInstallationID(),
            permission: self,
            remoteRegistration: self,
            backend: self,
            credentials: self,
            clock: self,
            retryScheduler: self,
            retryJitterSource: SystemRegistrationRetryJitterSource(),
            configuration: configuration,
            tokenCustody: tokenCustody
        )
        observeRegistrationAdapter(adapter)
        return adapter
    }()

    var registrationAdapter: IosNotificationRegistrationAdapter? {
        boundRegistrationAdapter ?? ownedRegistrationAdapter
    }

    var authenticationSessionID: String? { activeAuthenticationSessionID }
    var hasUsableCredential: Bool {
        activeAccessToken != nil && activeAuthenticationSessionID != nil
    }
    var registrationState: IosNotificationRegistrationState? {
        registrationAdapter?.snapshot.state
    }
    var now: Date { Date() }

    private static var apnsEnvironment: IosNotificationRegistrationEnvironment {
        #if DEBUG
        return .sandbox
        #else
        return .production
        #endif
    }

    private static func stableInstallationID() -> String {
        let key = "wakeve.installation.id"
        if let existing = UserDefaults.standard.string(forKey: key), !existing.isEmpty {
            return existing
        }
        let created = UUID().uuidString
        UserDefaults.standard.set(created, forKey: key)
        return created
    }

    func bindRegistrationAdapter(_ adapter: IosNotificationRegistrationAdapter) {
        boundRegistrationAdapter = adapter
        observeRegistrationAdapter(adapter)
    }

    func bindCredentialLifecyclePort(_ port: CredentialLifecyclePort) {
        credentialLifecycleObservers.bind(port)
    }

    // MARK: - Permission & Registration

    public func requestAuthorization(completion: @escaping (Bool, Error?) -> Void) {
        authorizationCompletions.append(completion)
        notificationCenter.getNotificationSettings { [weak self] settings in
            Task { @MainActor [weak self] in
                guard let self else { return }
                guard self.registrationAdapter != nil else {
                    self.finishAuthorization(
                        granted: false,
                        error: self.serviceError(statusCode: 0)
                    )
                    return
                }
                self.cachedAuthorizationStatus = self.registrationStatus(settings.authorizationStatus)
                self.authorizationRegistrationWrapper.requestAuthorizationAndRegister { [weak self] granted, error in
                    self?.finishAuthorization(granted: granted, error: error)
                }
            }
        }
    }

    public func requestAuthorizationAndRegister(completion: ((Bool, Error?) -> Void)? = nil) {
        requestAuthorization { granted, error in completion?(granted, error) }
    }

    public func checkAuthorizationStatus(completion: @escaping (UNAuthorizationStatus) -> Void) {
        notificationCenter.getNotificationSettings { settings in
            Task { @MainActor in
                completion(settings.authorizationStatus)
            }
        }
    }

    /// Compatibility wrapper: the machine re-checks permission before invoking the APNs port.
    public func registerForRemoteNotifications() {
        applicationBecameActive()
    }

    /// APP_BECAME_ACTIVE is the only launch/resume registration event.
    func applicationBecameActive() {
        registrationAdapter?.send(.appBecameActive)
    }

    func readStatus(correlationID: String) {
        notificationCenter.getNotificationSettings { [weak self] settings in
            Task { @MainActor [weak self] in
                guard let self else { return }
                let status = self.registrationStatus(settings.authorizationStatus)
                self.cachedAuthorizationStatus = status
                self.registrationAdapter?.send(.permissionStatusResolved(status, correlationID: correlationID))
            }
        }
    }

    func requestAuthorization(correlationID: String) {
        notificationCenter.requestAuthorization(options: [.alert, .sound, .badge]) { [weak self] granted, error in
            Task { @MainActor [weak self] in
                guard let self else { return }
                if let error {
                    self.registrationAdapter?.send(.permissionRequestFailed(.permission, correlationID: correlationID))
                    self.finishAuthorization(granted: false, error: error)
                } else if granted {
                    self.cachedAuthorizationStatus = .authorized
                    self.registrationAdapter?.send(.permissionGranted(correlationID: correlationID))
                    self.finishAuthorization(granted: true, error: nil)
                } else {
                    self.cachedAuthorizationStatus = .denied
                    self.registrationAdapter?.send(.permissionDenied(correlationID: correlationID))
                    self.finishAuthorization(granted: false, error: nil)
                }
            }
        }
    }

    func openSettings() {
        guard let url = URL(string: UIApplication.openSettingsURLString) else { return }
        UIApplication.shared.open(url)
    }

    func register(correlationID: String, callbackSink: RemoteNotificationRegistrationCallbackSink) {
        uikitRegistrationCoordinator.enqueue(
            correlationID: correlationID,
            callbackSink: callbackSink
        )
    }

    // MARK: - Correlated APNs callbacks

    public func didRegisterForRemoteNotifications(withDeviceToken deviceToken: Data) {
        uikitRegistrationCoordinator.completeCurrentSuccess(token: deviceToken)
    }

    public func didFailToRegisterForRemoteNotifications(error: Error) {
        uikitRegistrationCoordinator.completeCurrentFailure(errorClass: .network)
    }

    // MARK: - Authentication binding and compatibility wrappers

    public func registerTokenWithBackendIfAuthenticated() {
        let tokenStorage = SecureTokenStorage()
        Task { [weak self] in
            let accessToken = await tokenStorage.getAccessToken()
            guard let service = self else { return }
            await MainActor.run {
                guard let accessToken else {
                    service.registrationAdapter?.send(.authenticationBecameUnavailable)
                    return
                }
                let sessionID = service.bindCredential(accessToken)
                service.registrationAdapter?.send(.authenticationBecameAvailable(sessionID: sessionID))
            }
        }
    }

    public func unregisterToken(completion: @escaping (Bool, Error?) -> Void) {
        unregistrationCompletions[UUID()] = completion
        if registrationAdapter?.snapshot.state == .unregistered {
            finishUnregistration(success: true, error: nil, terminal: true)
            return
        }
        if registrationAdapter?.snapshot.state == .misconfigured {
            finishUnregistration(
                success: false,
                error: serviceError(statusCode: 400),
                terminal: true
            )
            return
        }
        guard !unregistrationBootstrapInFlight else { return }
        if registrationAdapter?.snapshot.logoutRequested == true { return }

        unregistrationBootstrapInFlight = true
        let tokenStorage = SecureTokenStorage()
        Task { [weak self] in
            let accessToken = await tokenStorage.getAccessToken()
            guard let service = self else { return }
            await MainActor.run {
                service.unregistrationBootstrapInFlight = false
                guard let adapter = service.registrationAdapter else {
                    service.finishUnregistration(
                        success: false,
                        error: service.serviceError(statusCode: 0),
                        terminal: true
                    )
                    return
                }
                guard let accessToken else {
                    adapter.send(.authenticationBecameUnavailable)
                    adapter.send(.logoutRequested)
                    service.finishUnregistration(
                        success: false,
                        error: service.serviceError(statusCode: 401)
                    )
                    return
                }
                let sessionID = service.bindCredential(accessToken)
                adapter.send(.authenticationBecameAvailable(sessionID: sessionID))
                adapter.send(.logoutRequested)
            }
        }
    }

    func clearCredentialAfterPushUnregistered() {
        activeAccessToken = nil
        activeAuthenticationSessionID = nil
        credentialLifecycleObservers.notifyPushUnregistered()
    }

    // MARK: - Backend registration port

    func register(_ registration: BackendDeviceRegistrationRequest) {
        guard let accessToken = credential(for: registration.authenticationSessionID),
              let url = URL(string: "\(baseUrl)/notifications/register") else {
            registrationAdapter?.send(.backendRegisterFailed(.backend, correlationID: registration.correlationID))
            return
        }

        var urlRequest = URLRequest(url: url)
        urlRequest.httpMethod = "POST"
        urlRequest.setValue("application/json", forHTTPHeaderField: "Content-Type")
        urlRequest.setValue("Bearer \(accessToken)", forHTTPHeaderField: "Authorization")
        let token = registration.token.map { String(format: "%02x", $0) }.joined()
        let body: [String: String] = [
            "installationId": registration.installationID,
            "topic": registration.topic,
            "environment": registration.environment,
            "platform": "ios",
            "token": token,
        ]

        do {
            urlRequest.httpBody = try JSONSerialization.data(withJSONObject: body)
        } catch {
            registrationAdapter?.send(.backendRegisterFailed(.configuration, correlationID: registration.correlationID))
            return
        }

        Task { [weak self] in
            guard let self else { return }
            do {
                let (data, response) = try await session.data(for: urlRequest)
                guard let httpResponse = response as? HTTPURLResponse else {
                    await sendRegistrationFailure(.network, correlationID: registration.correlationID)
                    return
                }
                guard (200..<300).contains(httpResponse.statusCode),
                      let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
                      let registrationId = json["registrationId"] as? String,
                      !registrationId.isEmpty else {
                    let errorClass = APNsBackendFailureClassifier.classify(
                        statusCode: httpResponse.statusCode,
                        networkFailure: false
                    )
                    await sendRegistrationFailure(errorClass, correlationID: registration.correlationID)
                    return
                }
                await MainActor.run {
                    self.registrationAdapter?.send(.backendRegisterSucceeded(
                        registrationID: registrationId,
                        correlationID: registration.correlationID
                    ))
                }
            } catch {
                await sendRegistrationFailure(
                    APNsBackendFailureClassifier.classify(statusCode: nil, networkFailure: true),
                    correlationID: registration.correlationID
                )
            }
        }
    }

    func unregister(_ unregistration: BackendDeviceUnregistrationRequest) {
        let httpContract = BackendDeviceRegistrationHttpContract.unregistrationRequest(
            for: unregistration.target
        )
        guard let accessToken = credential(for: unregistration.authenticationSessionID),
              let url = URL(string: baseUrl + httpContract.path) else {
            registrationAdapter?.send(.backendUnregisterFailed(.backend, correlationID: unregistration.correlationID))
            return
        }

        var urlRequest = URLRequest(url: url)
        urlRequest.httpMethod = httpContract.method
        urlRequest.setValue("Bearer \(accessToken)", forHTTPHeaderField: "Authorization")
        if let body = httpContract.body {
            urlRequest.setValue("application/json", forHTTPHeaderField: "Content-Type")
            do {
                urlRequest.httpBody = try JSONSerialization.data(withJSONObject: body)
            } catch {
                registrationAdapter?.send(.backendUnregisterFailed(.configuration, correlationID: unregistration.correlationID))
                finishUnregistration(success: false, error: error, terminal: true)
                return
            }
        }

        Task { [weak self] in
            guard let self else { return }
            do {
                let (data, response) = try await session.data(for: urlRequest)
                guard let httpResponse = response as? HTTPURLResponse else {
                    await sendUnregistrationFailure(.network, correlationID: unregistration.correlationID, error: nil)
                    return
                }
                let receipt = try? JSONSerialization.jsonObject(with: data) as? [String: Any]
                let receiptAlreadyAbsent = receipt?["alreadyAbsent"] as? Bool ?? false
                let outcome = BackendDeviceRegistrationHttpContract.classifyUnregistrationResponse(
                    statusCode: httpResponse.statusCode,
                    alreadyAbsent: receiptAlreadyAbsent
                )
                switch outcome {
                case let .succeeded(alreadyAbsent):
                    await MainActor.run {
                        self.registrationAdapter?.send(.backendUnregisterSucceeded(
                            alreadyAbsent: alreadyAbsent,
                            correlationID: unregistration.correlationID
                        ))
                        self.finishUnregistration(success: true, error: nil, terminal: true)
                    }
                case .authenticationFailure:
                    await sendUnregistrationFailure(
                        .backend,
                        correlationID: unregistration.correlationID,
                        error: serviceError(statusCode: httpResponse.statusCode)
                    )
                case .transientFailure:
                    await sendUnregistrationFailure(
                        .network,
                        correlationID: unregistration.correlationID,
                        error: serviceError(statusCode: httpResponse.statusCode)
                    )
                case .configurationFailure, .notOwnedOrMissing:
                    await sendUnregistrationFailure(
                        .configuration,
                        correlationID: unregistration.correlationID,
                        error: serviceError(statusCode: httpResponse.statusCode),
                        terminal: true
                    )
                }
            } catch {
                await sendUnregistrationFailure(
                    APNsBackendFailureClassifier.classify(statusCode: nil, networkFailure: true),
                    correlationID: unregistration.correlationID,
                    error: error
                )
            }
        }
    }

    // BACKEND_UNREGISTER_FAILED enters retry with resumeUnregistering until RETRY_DUE.
    private func sendUnregistrationFailure(
        _ errorClass: IosNotificationRegistrationErrorClass,
        correlationID: String,
        error: Error?,
        terminal: Bool = false
    ) async {
        await MainActor.run {
            registrationAdapter?.send(.backendUnregisterFailed(errorClass, correlationID: correlationID))
            finishUnregistration(success: false, error: error, terminal: terminal)
        }
    }

    private func sendRegistrationFailure(
        _ errorClass: IosNotificationRegistrationErrorClass,
        correlationID: String
    ) async {
        await MainActor.run {
            registrationAdapter?.send(.backendRegisterFailed(errorClass, correlationID: correlationID))
        }
    }

    func scheduleRetry(at date: Date, event: IosNotificationRegistrationEvent) {
        cancelRetry()
        let workItem = DispatchWorkItem { [weak self] in
            self?.registrationAdapter?.send(event)
        }
        retryWorkItem = workItem
        DispatchQueue.main.asyncAfter(deadline: .now() + max(0, date.timeIntervalSinceNow), execute: workItem)
    }

    func cancelRetry() {
        retryWorkItem?.cancel()
        retryWorkItem = nil
    }

    private func registrationStatus(_ status: UNAuthorizationStatus) -> IosNotificationAuthorizationStatus {
        switch status {
        case .notDetermined: return .notDetermined
        case .denied: return .denied
        case .authorized: return .authorized
        case .provisional: return .provisional
        case .ephemeral: return .ephemeral
        @unknown default: return .denied
        }
    }

    private func bindCredential(_ accessToken: String) -> String {
        if activeAccessToken != accessToken || activeAuthenticationSessionID == nil {
            authenticationSequence += 1
            activeAuthenticationSessionID = "notification-auth-session-\(authenticationSequence)"
        }
        activeAccessToken = accessToken
        return activeAuthenticationSessionID!
    }

    private func credential(for sessionID: String) -> String? {
        guard sessionID == activeAuthenticationSessionID else { return nil }
        return activeAccessToken
    }

    private func finishAuthorization(granted: Bool, error: Error?) {
        let completions = authorizationCompletions
        authorizationCompletions.removeAll(keepingCapacity: true)
        completions.forEach { $0(granted, error) }
    }

    private func finishUnregistration(
        success: Bool,
        error: Error?,
        terminal: Bool = false
    ) {
        let completions = Array(unregistrationCompletions.values)
        if success || terminal {
            unregistrationCompletions.removeAll(keepingCapacity: true)
        }
        completions.forEach { $0(success, error) }
    }

    private func observeRegistrationAdapter(_ adapter: IosNotificationRegistrationAdapter) {
        registrationObservation = adapter.$snapshot.sink { [weak self] snapshot in
            Task { @MainActor [weak self] in
                guard let self, snapshot.logoutRequested else { return }
                switch snapshot.state {
                case .unregistered:
                    self.finishUnregistration(success: true, error: nil, terminal: true)
                case .misconfigured:
                    self.finishUnregistration(
                        success: false,
                        error: self.serviceError(statusCode: 400),
                        terminal: true
                    )
                default:
                    break
                }
            }
        }
    }

    private func serviceError(statusCode: Int) -> Error {
        NSError(domain: "Wakeve.APNsService", code: statusCode)
    }

    // MARK: - Notification Handling

    /**
     * Handle incoming remote notification (foreground/background).
     * Call this from UIApplicationDelegate.
     */
    public func didReceiveRemoteNotification(userInfo: [AnyHashable: Any]) {
        let historyRecord = sanitizedHistoryRecord(from: userInfo)
        notificationHistory.persist(historyRecord)

        // Notify callback
        onNotificationReceived?(userInfo)

        // Handle notification content
        handleNotification(userInfo: userInfo)
    }

    private func sanitizedHistoryRecord(
        from userInfo: [AnyHashable: Any]
    ) -> SanitizedNotificationHistoryRecord {
        SanitizedNotificationHistoryRecord(
            notificationID: sanitizedIdentifier(userInfo["notificationId"]) ?? UUID().uuidString,
            eventID: sanitizedIdentifier(userInfo["eventId"]),
            receivedAtEpochMilliseconds: Int64(Date().timeIntervalSince1970 * 1_000)
        )
    }

    private func sanitizedIdentifier(_ value: Any?) -> String? {
        guard let rawValue = value as? String else { return nil }
        let allowed = CharacterSet.alphanumerics.union(CharacterSet(charactersIn: "-_."))
        let normalized = rawValue.unicodeScalars.filter { allowed.contains($0) }
        guard !normalized.isEmpty else { return nil }
        return String(String.UnicodeScalarView(normalized).prefix(128))
    }

    /**
     * Show local notification (for foreground messages).
     */
    private func showLocalNotification(title: String, body: String, userInfo: [AnyHashable: Any]) {
        let content = UNMutableNotificationContent()
        content.title = title
        content.body = body
        content.sound = .default
        content.userInfo = userInfo

        let request = UNNotificationRequest(
            identifier: UUID().uuidString,
            content: content,
            trigger: nil // Immediate
        )

        notificationCenter.add(request) { error in
            if error != nil {
                debugLog("APNS_LOCAL_NOTIFICATION_ADD_FAILED")
            }
        }
    }

    /**
     * Handle notification content and route to appropriate handler.
     */
    private func handleNotification(userInfo: [AnyHashable: Any]) {
        guard userInfo["aps"] is [String: Any] else { return }

        if UIApplication.shared.applicationState == .active {
            NotificationCenter.default.post(
                name: NSNotification.Name("DidReceiveForegroundNotification"),
                object: nil,
                userInfo: userInfo
            )
        }
    }

    // MARK: - Badge Management

    /**
     * Set notification badge count.
     */
    public func setBadgeCount(_ count: Int) {
        UNUserNotificationCenter.current().setBadgeCount(count) { error in
            if error != nil {
                debugLog("APNS_BADGE_COUNT_SET_FAILED")
            }
        }
    }

    /**
     * Clear notification badge.
     */
    public func clearBadge() {
        setBadgeCount(0)
    }

    /**
     * Get all delivered notifications.
     */
    public func getDeliveredNotifications(completion: @escaping ([UNNotification]) -> Void) {
        notificationCenter.getDeliveredNotifications { notifications in
            completion(notifications)
        }
    }

    /**
     * Remove all delivered notifications.
     */
    public func removeAllDeliveredNotifications() {
        notificationCenter.removeAllDeliveredNotifications()
    }

    /**
     * Remove specific delivered notifications.
     */
    public func removeDeliveredNotifications(withIdentifiers identifiers: [String]) {
        notificationCenter.removeDeliveredNotifications(withIdentifiers: identifiers)
    }
}

// MARK: - UNUserNotificationCenterDelegate

extension APNsService: @preconcurrency UNUserNotificationCenterDelegate {

    /**
     * Called when a notification is delivered while app is in foreground.
     * Shows banner + sound so the user sees the notification.
     */
    public func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        let userInfo = notification.request.content.userInfo

        // Show banner and play sound even in foreground
        completionHandler([.banner, .sound, .badge])

        didReceiveRemoteNotification(userInfo: userInfo)
    }

    /**
     * Called when user taps on a notification.
     * Handles deep linking to the relevant screen.
     */
    public func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        didReceive response: UNNotificationResponse,
        withCompletionHandler completionHandler: @escaping () -> Void
    ) {
        let userInfo = response.notification.request.content.userInfo

        notificationHistory.persist(sanitizedHistoryRecord(from: userInfo))

        // Notify tap callback
        onNotificationTapped?(userInfo)

        // Handle deep link navigation
        if let deepLinkUri = userInfo["deepLink"] as? String,
           URL(string: deepLinkUri) != nil {
            // Use the deep link URI from notification payload
            DispatchQueue.main.async {
                NotificationCenter.default.post(
                    name: NSNotification.Name("NavigateToEvent"),
                    object: nil,
                    userInfo: ["deepLink": deepLinkUri]
                )
            }
        } else if let eventId = userInfo["eventId"] as? String {
            // Fallback: navigate to event detail
            handleDeepLink(eventId: eventId)
        }

        completionHandler()
    }

    /**
     * Handle deep link from notification tap.
     * Posts a notification for SwiftUI navigation handling.
     */
    private func handleDeepLink(eventId: String) {
        debugLog("[APNsService] Event deep-link handoff requested")

        NotificationCenter.default.post(
            name: NSNotification.Name("NavigateToEvent"),
            object: nil,
            userInfo: ["eventId": eventId]
        )
    }
}
