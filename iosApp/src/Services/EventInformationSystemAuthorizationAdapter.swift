import Shared
import UserNotifications

typealias EventInformationSystemAuthorizationReader = @MainActor () async -> SystemNotificationAuthorization

/// Read-only adapter for the OS notification axis shown by Event Information.
/// Permission changes remain owned by the dedicated notification onboarding flow.
struct EventInformationSystemAuthorizationAdapter {
    private let center: UNUserNotificationCenter

    init(center: UNUserNotificationCenter = .current()) {
        self.center = center
    }

    func read() async -> SystemNotificationAuthorization {
        let settings = await withCheckedContinuation { continuation in
            center.getNotificationSettings { settings in
                continuation.resume(returning: settings)
            }
        }
        switch settings.authorizationStatus {
        case .authorized:
            return .authorized
        case .denied:
            return .denied
        case .notDetermined:
            return .notDetermined
        case .provisional:
            return .provisional
        case .ephemeral:
            return .ephemeral
        @unknown default:
            return .restricted
        }
    }
}
