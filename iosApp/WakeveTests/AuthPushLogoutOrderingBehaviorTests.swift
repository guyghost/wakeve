import Foundation
import XCTest
@testable import Wakeve

@MainActor
final class AuthPushLogoutOrderingBehaviorTests: XCTestCase {
    func testCredentialLifecycleRegistryNotifiesEveryManagerOnceWithoutRetainingDuplicates() {
        let registry = CredentialLifecycleObserverRegistry()
        let firstManager = CredentialLifecycleObserverSpy()
        let secondManager = CredentialLifecycleObserverSpy()

        registry.bind(firstManager)
        registry.bind(firstManager)
        registry.bind(secondManager)
        registry.notifyPushUnregistered()

        XCTAssertEqual(firstManager.clearCount, 1)
        XCTAssertEqual(secondManager.clearCount, 1)
    }

    func testOfflineUnregistrationRetainsCredentialsUntilTerminalPushUnregistered() async throws {
        let tokenStorage = OrderedLogoutTokenStorage()
        try await tokenStorage.storeAccessToken("access-token")
        try await tokenStorage.storeRefreshToken("refresh-token")
        try await tokenStorage.storeUserId("user-a")
        try await tokenStorage.storeTokenExpiry(Int64.max)

        let pushRegistration = ControllablePushUnregistrationPort()
        let authService = AuthenticationService(tokenStorage: tokenStorage)
        let manager = AuthStateManager(
            authService: authService,
            pushRegistration: pushRegistration
        )
        manager.isAuthenticated = true

        manager.signOut()
        let didRequestUnregistration = await eventually { pushRegistration.requestCount == 1 }
        XCTAssertTrue(didRequestUnregistration)
        XCTAssertEqual(manager.pushLogoutState, .unregistering)

        pushRegistration.failOffline()
        let didExposeOfflineState = await eventually { manager.pushLogoutState == .offline }
        XCTAssertTrue(didExposeOfflineState)
        XCTAssertTrue(manager.isAuthenticated)
        let retainedAccessToken = await authService.getAccessToken()
        XCTAssertEqual(retainedAccessToken, "access-token")
        XCTAssertEqual(tokenStorage.clearCount, 0)

        pushRegistration.succeedAsAlreadyUnregistered()
        let didCompleteSignOut = await eventually { !manager.isAuthenticated }
        XCTAssertTrue(didCompleteSignOut)
        XCTAssertEqual(manager.pushLogoutState, .completed)
        let clearedAccessToken = await authService.getAccessToken()
        XCTAssertNil(clearedAccessToken)
        XCTAssertEqual(tokenStorage.clearCount, 1)
        XCTAssertEqual(pushRegistration.effects, ["LOGOUT_REQUESTED", "retry", "PUSH_UNREGISTERED"])
    }

    private func eventually(
        _ condition: @escaping @MainActor () async -> Bool
    ) async -> Bool {
        for _ in 0..<200 {
            if await condition() { return true }
            await Task.yield()
        }
        return false
    }
}

private final class CredentialLifecycleObserverSpy: CredentialLifecyclePort {
    let authenticationSessionID: String? = "auth-session"
    let hasUsableCredential = true
    private(set) var clearCount = 0

    func clearCredentialAfterPushUnregistered() {
        clearCount += 1
    }
}

private final class ControllablePushUnregistrationPort: PushUnregistrationPort {
    private(set) var authenticationSessionID: String? = "auth-session"
    private(set) var hasUsableCredential = true
    private(set) var registrationState: IosNotificationRegistrationState? = .registered
    private(set) var requestCount = 0
    private(set) var effects: [String] = []

    private weak var credentialLifecyclePort: CredentialLifecyclePort?
    private var completion: ((Bool, Error?) -> Void)?

    func bindCredentialLifecyclePort(_ port: CredentialLifecyclePort) {
        credentialLifecyclePort = port
    }

    func unregisterToken(completion: @escaping (Bool, Error?) -> Void) {
        requestCount += 1
        effects.append("LOGOUT_REQUESTED")
        registrationState = .unregistering
        self.completion = completion
    }

    func failOffline() {
        registrationState = .retry
        effects.append("retry")
        completion?(false, URLError(.notConnectedToInternet))
    }

    func succeedAsAlreadyUnregistered() {
        registrationState = .unregistered
        effects.append("PUSH_UNREGISTERED")
        credentialLifecyclePort?.clearCredentialAfterPushUnregistered()
        completion?(true, nil)
        completion = nil
        authenticationSessionID = nil
        hasUsableCredential = false
    }
}

private final class OrderedLogoutTokenStorage: SecureTokenStorageProtocol {
    private var accessToken: String?
    private var refreshToken: String?
    private var userID: String?
    private var tokenExpiry: Int64?
    private(set) var clearCount = 0

    func storeAccessToken(_ token: String) async throws { accessToken = token }
    func storeRefreshToken(_ token: String) async throws { refreshToken = token }
    func storeUserId(_ userId: String) async throws { userID = userId }
    func storeTokenExpiry(_ expiryTimestamp: Int64) async throws { tokenExpiry = expiryTimestamp }
    func getAccessToken() async -> String? { accessToken }
    func getRefreshToken() async -> String? { refreshToken }
    func getUserId() async -> String? { userID }
    func getTokenExpiry() async -> Int64? { tokenExpiry }

    func clearAllTokens() async throws {
        clearCount += 1
        accessToken = nil
        refreshToken = nil
        userID = nil
        tokenExpiry = nil
    }

    func isTokenExpired() async -> Bool {
        guard let tokenExpiry else { return true }
        return Date().timeIntervalSince1970 * 1_000 >= Double(tokenExpiry)
    }

    func hasValidToken() async -> Bool {
        guard let accessToken, !accessToken.isEmpty else { return false }
        return !(await isTokenExpired())
    }
}
