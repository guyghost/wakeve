import Foundation
import BackgroundTasks

/**
 * Background token refresh manager for iOS.
 *
 * Uses BGTaskScheduler to refresh authentication tokens in the background.
 * Registers and schedules background tasks to keep tokens fresh even when
 * the app is not running.
 *
 * Setup:
 * 1. Add "Background processing" capability in Xcode
 * 2. Add BGTaskScheduler identifier to Info.plist:
 *    <key>BGTaskSchedulerPermittedIdentifiers</key>
 *    <array>
 *        <string>com.guyghost.wakeve.tokenrefresh</string>
 *    </array>
 * 3. Call registerBackgroundTasks() in app delegate
 */
@available(iOS 13.0, *)
class BackgroundTokenRefreshManager {
    static let shared = BackgroundTokenRefreshManager()

    private let taskIdentifier = "com.guyghost.wakeve.tokenrefresh"

    /// AuthStateManager instance (set this after initialization)
    weak var authStateManager: AuthStateManager?

    private init() {}

    /**
     * Register background task handlers.
     *
     * Call this in application(_:didFinishLaunchingWithOptions:)
     */
    func registerBackgroundTasks() {
        BGTaskScheduler.shared.register(
            forTaskWithIdentifier: taskIdentifier,
            using: nil
        ) { [weak self] task in
            guard let bgTask = task as? BGProcessingTask else { return }
            self?.handleTokenRefresh(task: bgTask)
        }
    }

    /**
     * Schedule background token refresh.
     *
     * Call this when user logs in.
     */
    func scheduleTokenRefresh() {
        let request = BGProcessingTaskRequest(identifier: taskIdentifier)

        // Set requirements
        request.requiresNetworkConnectivity = true
        request.requiresExternalPower = false

        // Run earliest 15 minutes from now
        request.earliestBeginDate = Date(timeIntervalSinceNow: 15 * 60)

        do {
            try BGTaskScheduler.shared.submit(request)
            print("✅ Background token refresh scheduled")
        } catch {
            print("❌ Failed to schedule background token refresh: \\(error)")
        }
    }

    /**
     * Cancel background token refresh.
     *
     * Call this when user logs out.
     */
    func cancelTokenRefresh() {
        BGTaskScheduler.shared.cancel(taskRequestWithIdentifier: taskIdentifier)
        print("🚫 Background token refresh cancelled")
    }

    /**
     * Handle background token refresh task.
     */
    private func handleTokenRefresh(task: BGProcessingTask) {
        // Schedule the next refresh
        scheduleTokenRefresh()

        // Set expiration handler
        task.expirationHandler = {
            print("⚠️ Background token refresh task expired")
            task.setTaskCompleted(success: false)
        }

        // Perform the refresh
        Task {
            guard let authManager = authStateManager else {
                print("❌ AuthStateManager not available")
                task.setTaskCompleted(success: false)
                return
            }

            // Attempt token refresh
            await authManager.refreshTokenIfNeeded()
            
            // Refresh completed (errors handled internally by AuthStateManager)
            print("✅ Background token refresh completed")
            task.setTaskCompleted(success: true)
        }
    }
}

// MARK: - Simulating background refresh in Simulator

extension BackgroundTokenRefreshManager {
    /**
     * Simulate background refresh (for testing in simulator).
     *
     * In Xcode, use: e -l objc -- (void)[[BGTaskScheduler sharedScheduler] _simulateLaunchForTaskWithIdentifier:@"com.guyghost.wakeve.tokenrefresh"]
     */
    func simulateBackgroundRefresh() {
        print("🧪 Simulating background token refresh")

        Task {
            guard let authManager = authStateManager else {
                print("❌ AuthStateManager not available")
                return
            }

            await authManager.refreshTokenIfNeeded()
            print("✅ Simulated refresh completed")
        }
    }
}
