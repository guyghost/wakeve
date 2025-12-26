import SwiftUI

@main
struct iOSApp: App {
    @StateObject private var authStateManager: AuthStateManager
    @StateObject private var authService: AuthenticationService

    init() {
        let authSvc = AuthenticationService()
        _authService = StateObject(wrappedValue: authSvc)

        // Feature flag: Enable OAuth (set to false to disable)
        let enableOAuth = false // TODO: Move to build config
        _authStateManager = StateObject(wrappedValue: AuthStateManager(
            authService: authSvc,
            enableOAuth: enableOAuth
        ))
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environmentObject(authStateManager)
                .environmentObject(authService)
                .task {
                    authStateManager.checkAuthStatus()
                }
        }
    }
}