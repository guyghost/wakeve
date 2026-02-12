//
//  APNsService.swift
//  iosApp
//
//  Created by Wakeve Team
//  Handles Apple Push Notification service integration
//

import Foundation
import UserNotifications
import UIKit

/**
 * Service for managing Apple Push Notifications.
 * Handles permission requests, token registration, and notification display.
 */
@objc public class APNsService: NSObject {

    public static let shared = APNsService()
    private override init() {}

    // Notification center delegate
    private let notificationCenter = UNUserNotificationCenter.current()

    // Callback for when notification is received in foreground
    public var onNotificationReceived: (([AnyHashable: Any]) -> Void)?

    /**
     * Request notification permission from user.
     * Call this on app launch.
     */
    public func requestAuthorization(completion: @escaping (Bool, Error?) -> Void) {
        let options: UNAuthorizationOptions = [.alert, .sound, .badge]
        notificationCenter.requestAuthorization(options: options) { granted, error in
            DispatchQueue.main.async {
                completion(granted, error)
            }
        }
    }

    /**
     * Check if notification permission is granted.
     */
    public func checkAuthorizationStatus(completion: @escaping (UNAuthorizationStatus) -> Void) {
        notificationCenter.getNotificationSettings { settings in
            DispatchQueue.main.async {
                completion(settings.authorizationStatus)
            }
        }
    }

    /**
     * Get current authorization status.
     */
    public func getAuthorizationStatus() -> UNAuthorizationStatus {
        var status: UNAuthorizationStatus?
        let semaphore = DispatchSemaphore(value: 0)

        notificationCenter.getNotificationSettings { settings in
            status = settings.authorizationStatus
            semaphore.signal()
        }

        semaphore.wait()
        return status ?? .notDetermined
    }

    /**
     * Register for remote notifications.
     * Call after permission is granted.
     */
    public func registerForRemoteNotifications() {
        DispatchQueue.main.async {
            UIApplication.shared.registerForRemoteNotifications()
        }
    }

    /**
     * Handle successful APNs token registration.
     * Call this from UIApplicationDelegate.
     */
    public func didRegisterForRemoteNotifications(withDeviceToken deviceToken: Data) {
        // Convert token to string
        let tokenParts = deviceToken.map { data in String(format: "%02.2hhx", data) }
        let token = tokenParts.joined()

        print("APNsService: Token registered: \(token.prefix(20))...")

        // Register with backend
        registerTokenWithBackend(token: token)
    }

    /**
     * Handle failed token registration.
     * Call this from UIApplicationDelegate.
     */
    public func didFailToRegisterForRemoteNotifications(error: Error) {
        print("APNsService: Failed to register token: \(error.localizedDescription)")
    }

    /**
     * Handle incoming remote notification (foreground).
     * Call this from UIApplicationDelegate.
     */
    public func didReceiveRemoteNotification(userInfo: [AnyHashable: Any]) {
        print("APNsService: Received notification: \(userInfo)")

        // Notify callback
        onNotificationReceived?(userInfo)

        // Handle notification content
        handleNotification(userInfo: userInfo)
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

        // Create request
        let request = UNNotificationRequest(
            identifier: UUID().uuidString,
            content: content,
            trigger: nil // Immediate
        )

        // Add notification request
        notificationCenter.add(request) { error in
            if let error = error {
                print("APNsService: Failed to add notification: \(error.localizedDescription)")
            }
        }
    }

    /**
     * Handle notification content.
     */
    private func handleNotification(userInfo: [AnyHashable: Any]) {
        guard let aps = userInfo["aps"] as? [String: Any] else { return }

        let title = aps["alert"] as? String ?? "Wakeve"
        let body = (aps["alert"] as? [String: String])?["body"] ?? "New notification"
        let notificationId = userInfo["notificationId"] as? String ?? UUID().uuidString
        let eventId = userInfo["eventId"] as? String

        // Check if app is in foreground
        if UIApplication.shared.applicationState == .active {
            // Show in-app notification (banner or snackbar)
            NotificationCenter.default.post(
                name: NSNotification.Name("DidReceiveForegroundNotification"),
                object: nil,
                userInfo: userInfo
            )
        } else {
            // System will handle background notification
            print("APNsService: Background notification received")
        }
    }

    /**
     * Register APNs token with backend.
     * TODO: Integrate with Kotlin/Native backend service.
     */
    private func registerTokenWithBackend(token: String) {
        print("APNsService: Registering token with backend...")
        print("Token (first 20 chars): \(String(token.prefix(20)))")

        // TODO: Call Kotlin/Native notification service
        // Example:
        // let notificationService = NotificationService()
        // notificationService.registerPushToken(
        //     userId: SessionManager.shared.currentUserId ?? "",
        //     platform: Platform.ios,
        //     token: token
        // )

        // Placeholder implementation
        DispatchQueue.main.asyncAfter(deadline: .now() + 2.0) {
            print("APNsService: Token registration complete (mock)")
        }
    }

    /**
     * Unregister APNs token from backend.
     */
    public func unregisterToken(completion: @escaping (Bool, Error?) -> Void) {
        // TODO: Implement backend unregistration
        completion(true, nil)
    }

    /**
     * Set notification badge count.
     */
    public func setBadgeCount(_ count: Int) {
        DispatchQueue.main.async {
            UIApplication.shared.applicationIconBadgeNumber = count
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

extension APNsService: UNUserNotificationCenterDelegate {

    /**
     * Called when a notification is delivered while app is in foreground.
     */
    public func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        let userInfo = notification.request.content.userInfo

        print("APNsService: Foreground notification received: \(userInfo)")

        // Show banner and play sound
        completionHandler([.banner, .sound, .badge])

        // Notify callback
        onNotificationReceived?(userInfo)
    }

    /**
     * Called when user taps on a notification.
     */
    public func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        didReceive response: UNNotificationResponse,
        withCompletionHandler completionHandler: @escaping () -> Void
    ) {
        let userInfo = response.notification.request.content.userInfo

        print("APNsService: User tapped notification: \(userInfo)")

        // Handle deep link
        if let eventId = userInfo["eventId"] as? String {
            handleDeepLink(eventId: eventId)
        }

        completionHandler()
    }

    /**
     * Handle deep link from notification tap.
     */
    private func handleDeepLink(eventId: String) {
        // Navigate to event detail screen
        // TODO: Integrate with SwiftUI navigation
        print("APNsService: Deep link to event: \(eventId)")

        NotificationCenter.default.post(
            name: NSNotification.Name("NavigateToEvent"),
            object: nil,
            userInfo: ["eventId": eventId]
        )
    }
}
