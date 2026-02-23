//
//  AppDelegate.swift
//  iosApp
//
//  Handles UIApplication lifecycle events for push notifications.
//  Bridges APNs token registration and notification handling to the SwiftUI app.
//

import UIKit
import UserNotifications

/**
 * App delegate for handling push notifications and APNs token registration.
 *
 * This class is the bridge between the iOS system notification callbacks
 * and the APNsService that manages token registration with the backend.
 */
class AppDelegate: NSObject, UIApplicationDelegate {

    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        // Set APNsService as the notification center delegate
        let apnsService = APNsService.shared
        UNUserNotificationCenter.current().delegate = apnsService

        // Request notification authorization and register for remote notifications
        apnsService.requestAuthorization { granted, error in
            if granted {
                print("[AppDelegate] Notification permission granted")
                apnsService.registerForRemoteNotifications()
            } else {
                print("[AppDelegate] Notification permission denied: \(error?.localizedDescription ?? "unknown")")
            }
        }

        // Check if app was launched from a notification
        if let remoteNotification = launchOptions?[.remoteNotification] as? [AnyHashable: Any] {
            print("[AppDelegate] App launched from notification")
            apnsService.didReceiveRemoteNotification(userInfo: remoteNotification)
        }

        return true
    }

    // MARK: - Remote Notification Registration

    /**
     * Called when APNs successfully registers a device token.
     */
    func application(
        _ application: UIApplication,
        didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data
    ) {
        APNsService.shared.didRegisterForRemoteNotifications(withDeviceToken: deviceToken)
    }

    /**
     * Called when APNs registration fails.
     */
    func application(
        _ application: UIApplication,
        didFailToRegisterForRemoteNotificationsWithError error: Error
    ) {
        APNsService.shared.didFailToRegisterForRemoteNotifications(error: error)
    }

    /**
     * Called when a remote notification arrives while the app is in the foreground or background.
     */
    func application(
        _ application: UIApplication,
        didReceiveRemoteNotification userInfo: [AnyHashable: Any],
        fetchCompletionHandler completionHandler: @escaping (UIBackgroundFetchResult) -> Void
    ) {
        APNsService.shared.didReceiveRemoteNotification(userInfo: userInfo)
        completionHandler(.newData)
    }
}
