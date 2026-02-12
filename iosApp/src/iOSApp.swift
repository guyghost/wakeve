import SwiftUI

// Color extensions are defined in Theme/WakeveColors.swift

@main
struct iOSApp: App {
    @StateObject private var authStateManager: AuthStateManager
    @StateObject private var authService: AuthenticationService
    @StateObject private var deepLinkService: DeepLinkService

    init() {
        let authSvc = AuthenticationService()
        _authService = StateObject(wrappedValue: authSvc)

        // Feature flag: Enable OAuth (set to false to disable)
        let enableOAuth = false // TODO: Move to build config
        _authStateManager = StateObject(wrappedValue: AuthStateManager(
            authService: authSvc,
            enableOAuth: enableOAuth
        ))

        // Initialize DeepLinkService
        _deepLinkService = StateObject(wrappedValue: DeepLinkService())
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environmentObject(authStateManager)
                .environmentObject(authService)
                .environmentObject(deepLinkService)
                .task {
                    authStateManager.checkAuthStatus()
                }
        }
        .onOpenURL { url in
            // Handle incoming deep links
            handleDeepLink(url)
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
        print("[iOSApp] Deep link received: \(url.absoluteString)")

        // Check authentication status before handling deep link
        let isAuthenticated = authStateManager.isAuthenticated

        // Handle the deep link
        _ = deepLinkService.handleDeepLink(url, isAuthenticated: isAuthenticated)

        // Note: ContentView should observe deepLinkService.navigationPath
        // and navigate accordingly
    }
}

