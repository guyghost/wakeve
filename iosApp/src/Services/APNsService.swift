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
 * Handles permission requests, token registration with the backend, and notification display.
 *
 * Token registration flow:
 * 1. App requests notification permission on launch (via AppDelegate)
 * 2. System calls didRegisterForRemoteNotifications with device token
 * 3. APNsService converts token to hex string and stores it locally
 * 4. When user is authenticated, token is sent to POST /api/notifications/register
 * 5. On logout, token is unregistered via DELETE /api/notifications/unregister
 */
@objc public class APNsService: NSObject {

    public static let shared = APNsService()
    private override init() {}

    // Notification center delegate
    private let notificationCenter = UNUserNotificationCenter.current()

    /// Current APNs device token (hex string)
    private(set) var currentDeviceToken: String?

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

    // MARK: - Permission & Registration

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
     * Get current authorization status (synchronous, use with caution).
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

    // MARK: - Token Handling

    /**
     * Handle successful APNs token registration.
     * Call this from UIApplicationDelegate.
     */
    public func didRegisterForRemoteNotifications(withDeviceToken deviceToken: Data) {
        // Convert token to hex string
        let tokenParts = deviceToken.map { data in String(format: "%02.2hhx", data) }
        let token = tokenParts.joined()

        print("[APNsService] Device token received: \(token.prefix(20))...")

        // Store token locally
        currentDeviceToken = token
        UserDefaults.standard.set(token, forKey: "apns_device_token")

        // Register with backend if user is authenticated
        registerTokenWithBackendIfAuthenticated()
    }

    /**
     * Handle failed token registration.
     * Call this from UIApplicationDelegate.
     */
    public func didFailToRegisterForRemoteNotifications(error: Error) {
        print("[APNsService] Failed to register for remote notifications: \(error.localizedDescription)")
    }

    // MARK: - Backend Token Registration

    /**
     * Register APNs token with backend server.
     *
     * Sends the device token to POST /api/notifications/register
     * with the JWT access token for authentication.
     */
    public func registerTokenWithBackendIfAuthenticated() {
        guard let token = currentDeviceToken ?? UserDefaults.standard.string(forKey: "apns_device_token") else {
            print("[APNsService] No device token available for registration")
            return
        }

        // Get access token from secure storage
        let tokenStorage = SecureTokenStorage()
        Task {
            guard let accessToken = await tokenStorage.getAccessToken() else {
                print("[APNsService] User not authenticated, deferring token registration")
                return
            }

            await registerToken(deviceToken: token, accessToken: accessToken)
        }
    }

    /**
     * Register device token with the backend API.
     */
    private func registerToken(deviceToken: String, accessToken: String) async {
        guard let url = URL(string: "\(baseUrl)/notifications/register") else { return }

        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.setValue("Bearer \(accessToken)", forHTTPHeaderField: "Authorization")

        let body: [String: String] = [
            "token": deviceToken,
            "platform": "ios"
        ]

        do {
            request.httpBody = try JSONSerialization.data(withJSONObject: body)
            let (_, response) = try await session.data(for: request)

            if let httpResponse = response as? HTTPURLResponse {
                if httpResponse.statusCode == 200 {
                    print("[APNsService] Token registered with backend successfully")
                } else {
                    print("[APNsService] Token registration failed: HTTP \(httpResponse.statusCode)")
                }
            }
        } catch {
            print("[APNsService] Token registration error: \(error.localizedDescription)")
        }
    }

    /**
     * Unregister APNs token from backend on logout.
     *
     * Calls DELETE /api/notifications/unregister?platform=ios
     */
    public func unregisterToken(completion: @escaping (Bool, Error?) -> Void) {
        let tokenStorage = SecureTokenStorage()
        Task {
            guard let accessToken = await tokenStorage.getAccessToken() else {
                completion(false, nil)
                return
            }

            guard let url = URL(string: "\(baseUrl)/notifications/unregister?platform=ios") else {
                completion(false, nil)
                return
            }

            var request = URLRequest(url: url)
            request.httpMethod = "DELETE"
            request.setValue("Bearer \(accessToken)", forHTTPHeaderField: "Authorization")

            do {
                let (_, response) = try await session.data(for: request)
                if let httpResponse = response as? HTTPURLResponse, httpResponse.statusCode == 200 {
                    print("[APNsService] Token unregistered from backend")
                    UserDefaults.standard.removeObject(forKey: "apns_device_token")
                    currentDeviceToken = nil
                    completion(true, nil)
                } else {
                    completion(false, nil)
                }
            } catch {
                print("[APNsService] Token unregistration error: \(error.localizedDescription)")
                completion(false, error)
            }
        }
    }

    // MARK: - Notification Handling

    /**
     * Handle incoming remote notification (foreground/background).
     * Call this from UIApplicationDelegate.
     */
    public func didReceiveRemoteNotification(userInfo: [AnyHashable: Any]) {
        print("[APNsService] Received notification: \(userInfo)")

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

        let request = UNNotificationRequest(
            identifier: UUID().uuidString,
            content: content,
            trigger: nil // Immediate
        )

        notificationCenter.add(request) { error in
            if let error = error {
                print("[APNsService] Failed to add local notification: \(error.localizedDescription)")
            }
        }
    }

    /**
     * Handle notification content and route to appropriate handler.
     */
    private func handleNotification(userInfo: [AnyHashable: Any]) {
        guard let aps = userInfo["aps"] as? [String: Any] else { return }

        // Extract notification data
        let eventId = userInfo["eventId"] as? String
        let notificationId = userInfo["notificationId"] as? String ?? UUID().uuidString
        let deepLinkUri = userInfo["deepLink"] as? String

        // Check if app is in foreground
        DispatchQueue.main.async {
            if UIApplication.shared.applicationState == .active {
                // Post notification for in-app banner display
                NotificationCenter.default.post(
                    name: NSNotification.Name("DidReceiveForegroundNotification"),
                    object: nil,
                    userInfo: userInfo
                )
            } else {
                print("[APNsService] Background notification received: \(notificationId)")
            }
        }
    }

    // MARK: - Badge Management

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
     * Shows banner + sound so the user sees the notification.
     */
    public func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        let userInfo = notification.request.content.userInfo

        print("[APNsService] Foreground notification: \(userInfo)")

        // Show banner and play sound even in foreground
        completionHandler([.banner, .sound, .badge])

        // Notify callback for in-app handling
        onNotificationReceived?(userInfo)
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

        print("[APNsService] Notification tapped: \(userInfo)")

        // Notify tap callback
        onNotificationTapped?(userInfo)

        // Handle deep link navigation
        if let deepLinkUri = userInfo["deepLink"] as? String,
           let url = URL(string: deepLinkUri) {
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
        print("[APNsService] Deep link to event: \(eventId)")

        NotificationCenter.default.post(
            name: NSNotification.Name("NavigateToEvent"),
            object: nil,
            userInfo: ["eventId": eventId]
        )
    }
}
