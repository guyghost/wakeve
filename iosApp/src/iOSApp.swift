import SwiftUI

// Color extensions are defined in Theme/WakeveColors.swift

@main
struct iOSApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) var appDelegate
    @StateObject private var authStateManager: AuthStateManager
    @StateObject private var authService: AuthenticationService
    @StateObject private var deepLinkService: DeepLinkService
    #if DEBUG
    @State private var appIntentTestScreen: WakeveIntentTestScreen?
    #endif

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
            Group {
                #if DEBUG
                if let appIntentTestScreen {
                    WakeveAppIntentAnnotationTestSurface(
                        screen: appIntentTestScreen,
                        onDismiss: {
                            self.appIntentTestScreen = nil
                        }
                    )
                } else {
                    appContent
                }
                #else
                appContent
                #endif
            }
        }
    }

    private var appContent: some View {
        ContentView()
            .environmentObject(authStateManager)
            .environmentObject(authService)
            .environmentObject(deepLinkService)
            .task {
                #if DEBUG
                if await authStateManager.authenticateForDevelopmentLaunchIfRequested() {
                    await routePendingAppIntentHandoff()
                    return
                }
                #endif

                authStateManager.checkAuthStatus()
                await routePendingAppIntentHandoff()
            }
            .onOpenURL { url in
                // Handle incoming deep links
                handleDeepLink(url)
            }
            .onReceive(NotificationCenter.default.publisher(for: NSNotification.Name("NavigateToEvent"))) { notification in
                // Handle notification tap deep link
                if let eventId = notification.userInfo?["eventId"] as? String {
                    let url = URL(string: "wakeve://event/\(eventId)")!
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
        debugLog("[iOSApp] Deep link received: \(url.absoluteString)")

        // Check authentication status before handling deep link
        let isAuthenticated = authStateManager.isAuthenticated

        // Handle the deep link
        _ = deepLinkService.handleDeepLink(url, isAuthenticated: isAuthenticated)

        // Note: ContentView should observe deepLinkService.navigationPath
        // and navigate accordingly
    }

    private func routePendingAppIntentHandoff() async {
        #if DEBUG
        if let testScreen = await WakeveIntentStore.shared.consumeOpenTestScreen() {
            await MainActor.run {
                appIntentTestScreen = testScreen
            }
            return
        }
        #endif

        guard let eventId = await WakeveIntentStore.shared.consumeOpenEventId(),
              let url = URL(string: "wakeve://event/\(eventId)") else {
            return
        }

        await MainActor.run {
            handleDeepLink(url)
        }
    }
}
