import SwiftUI

// Color extensions are defined in Theme/WakeveColors.swift

@main
struct iOSApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) var appDelegate
    @StateObject private var authStateManager: AuthStateManager
    @StateObject private var authService: AuthenticationService
    @StateObject private var deepLinkService: DeepLinkService

    init() {
        let authSvc = AuthenticationService()
        _authService = StateObject(wrappedValue: authSvc)

        // Feature flag: Enable OAuth authentication
        let enableOAuth = true
        _authStateManager = StateObject(wrappedValue: AuthStateManager(
            authService: authSvc,
            enableOAuth: enableOAuth
        ))

        // Initialize DeepLinkService
        _deepLinkService = StateObject(wrappedValue: DeepLinkService())
    }

    var body: some Scene {
        WindowGroup {
            appRoot
        }
    }

    @ViewBuilder
    private var appRoot: some View {
        normalAppRoot
    }

    private var normalAppRoot: some View {
        ContentView()
            .environmentObject(authStateManager)
            .environmentObject(authService)
            .environmentObject(deepLinkService)
            .task {
                #if DEBUG
                if await authStateManager.authenticateForDevelopmentLaunchIfRequested() {
                    return
                }
                #endif

                authStateManager.checkAuthStatus()
            }
            .onOpenURL { url in
                // Handle incoming deep links
                handleDeepLink(url)
            }
            .onReceive(NotificationCenter.default.publisher(for: NSNotification.Name("NavigateToEvent"))) { notification in
                // Handle notification tap deep link
                if let url = NotificationDeepLink.url(from: notification.userInfo ?? [:]) {
                    handleDeepLink(url)
                }
            }
    }

    // MARK: - Deep Link Handling

    /**
     * Handle deep link URL.
     *
     * This method is called when the app is opened via a deep link.
     * It passes the URL to DeepLinkService for processing and navigation.
     *
     * - Parameter url: The deep link URL to handle
     */
    private func handleDeepLink(_ url: URL) {
        debugLog("[iOSApp] Deep link handoff received")

        // Check authentication status before handling deep link
        let isAuthenticated = authStateManager.isAuthenticated

        // Handle the deep link
        _ = deepLinkService.handleDeepLink(url, isAuthenticated: isAuthenticated)

        // ContentView observes the typed deepLinkService.navigationRoute.
        // and navigate accordingly
    }
}
