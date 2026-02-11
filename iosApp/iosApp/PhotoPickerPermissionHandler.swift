import UIKit
import Photos
import PhotosUI

/**
 * Handler for requesting and managing photo library access permissions on iOS 14+.
 *
 * This class provides a clean API for:
 * - Requesting photo library authorization
 * - Checking current authorization status
 * - Handling limited access mode (iOS 14+)
 * - Presenting permission rationale dialogs
 *
 * ## Permission Levels
 *
 * | Status | Description |
 * |--------|-------------|
 * | `.notDetermined` | User hasn't been asked yet |
 * | `.authorized` | Full access granted |
 * | `.limited` | Only selected photos accessible (iOS 14+) |
 * | `.restricted` | Access restricted by parental controls |
 * | `.denied` | User explicitly denied access |
 *
 * ## Usage
 *
 * ```swift
 * // Check current status
 * if PhotoPickerPermissionHandler.isAuthorized() {
 *     // Show image picker
 * }
 *
 * // Request access
 * PhotoPickerPermissionHandler.requestPermission { granted in
 *     if granted {
 *         // Show image picker
 *     } else {
 *         // Show settings alert
 *     }
 * }
 * ```
 *
 * ## Requirements
 *
 * Add the following to your Info.plist:
 *
 * ```xml
 * <key>NSPhotoLibraryUsageDescription</key>
 * <string>We need access to your photo library to attach images to messages.</string>
 * ```
 */
final class PhotoPickerPermissionHandler {
    
    // MARK: - Constants
    
    /// The Info.plist key for photo library usage description
    private static let photoLibraryUsageDescriptionKey = "NSPhotoLibraryUsageDescription"
    
    // MARK: - Authorization Status
    
    /**
     * Checks the current photo library authorization status.
     *
     * - Returns: The current authorization status
     */
    static func authorizationStatus() -> PHAuthorizationStatus {
        return PHPhotoLibrary.authorizationStatus(for: .readWrite)
    }
    
    /**
     * Checks if photo library access is authorized (full or limited).
     *
     * - Returns: `true` if access is authorized or limited, `false` otherwise
     */
    static func isAuthorized() -> Bool {
        let status = authorizationStatus()
        return status == .authorized || status == .limited
    }
    
    /**
     * Checks if the user has explicitly denied access.
     *
     * - Returns: `true` if access was denied by the user
     */
    static func isDenied() -> Bool {
        return authorizationStatus() == .denied
    }
    
    /**
     * Checks if access is in the "not determined" state.
     *
     * - Returns: `true` if the user hasn't been asked yet
     */
    static func isNotDetermined() -> Bool {
        return authorizationStatus() == .notDetermined
    }
    
    // MARK: - Permission Requests
    
    /**
     * Requests photo library access permission.
     *
     * For iOS 14+, this triggers the system permission dialog.
     * On older iOS versions, this falls back to the standard `.authorized` request.
     *
     * - Parameter completion: Called with the result of the permission request
     */
    static func requestPermission(completion: @escaping (Bool) -> Void) {
        // Check if we're on iOS 14+ for limited access support
        if #available(iOS 14.0, *) {
            let status = PHPhotoLibrary.authorizationStatus(for: .readWrite)
            
            switch status {
            case .notDetermined:
                // First time request - will show system dialog
                PHPhotoLibrary.requestAuthorization(for: .readWrite) { newStatus in
                    DispatchQueue.main.async {
                        let granted = newStatus == .authorized || newStatus == .limited
                        completion(granted)
                    }
                }
                
            case .authorized, .limited:
                // Already authorized
                completion(true)
                
            case .denied, .restricted:
                // User previously denied or restricted
                completion(false)
                
            @unknown default:
                completion(false)
            }
        } else {
            // iOS 13 and below
            let status = PHPhotoLibrary.authorizationStatus()
            
            switch status {
            case .notDetermined:
                PHPhotoLibrary.requestAuthorization { newStatus in
                    DispatchQueue.main.async {
                        completion(newStatus == .authorized)
                    }
                }
                
            case .authorized, .limited:
                completion(true)
                
            case .denied, .restricted:
                completion(false)
                
            @unknown default:
                completion(false)
            }
        }
    }
    
    /**
     * Requests limited photo library access (iOS 14+).
     *
     * This method only works on iOS 14 and later. On older versions,
     * it falls back to requesting full access.
     *
     * - Parameter completion: Called with the result of the permission request
     */
    @available(iOS 14.0, *)
    static func requestLimitedAccess(completion: @escaping (Bool) -> Void) {
        let status = PHPhotoLibrary.authorizationStatus(for: .readWrite)
        
        switch status {
        case .notDetermined:
            // Request access - user will see the limited access dialog
            PHPhotoLibrary.requestAuthorization(for: .readWrite) { newStatus in
                DispatchQueue.main.async {
                    completion(newStatus == .authorized || newStatus == .limited)
                }
            }
            
        case .authorized:
            // Already have full access
            completion(true)
            
        case .limited:
            // Already have limited access
            completion(true)
            
        case .denied, .restricted:
            completion(false)
            
        @unknown default:
            completion(false)
        }
    }
    
    /**
     * Opens the app's settings page to let the user change permissions.
     *
     * Call this method when the user needs to manually enable
     * photo library access after a previous denial.
     *
     * - Parameter from viewController: The view controller to present the alert from
     */
    static func openSettingsToEnableAccess(from viewController: UIViewController) {
        let alert = UIAlertController(
            title: "Accès aux photos désactivé",
            message: "Pour permettre l'accès aux photos, allez dans Paramètres > Confidentialité > Photos et activez l'accès pour Wakeve.",
            preferredStyle: .alert
        )
        
        alert.addAction(UIAlertAction(title: "Annuler", style: .cancel))
        alert.addAction(UIAlertAction(title: "Paramètres", style: .default) { _ in
            if let url = URL(string: UIApplication.openSettingsURLString) {
                UIApplication.shared.open(url)
            }
        })
        
        viewController.present(alert, animated: true)
    }
    
    // MARK: - Info.plist Validation
    
    /**
     * Checks if the required Info.plist key is configured.
     *
     * The photo library usage description is required to display
     * the system permission dialog.
     *
     * - Returns: `true` if the key is configured, `false` otherwise
     */
    static func hasUsageDescriptionConfigured() -> Bool {
        guard let bundle = Bundle.main.infoDictionary else {
            return false
        }
        
        return bundle[photoLibraryUsageDescriptionKey] != nil
    }
    
    /**
     * Gets the configured usage description text.
     *
     * - Returns: The usage description string, or nil if not configured
     */
    static func usageDescriptionText() -> String? {
        guard let bundle = Bundle.main.infoDictionary else {
            return nil
        }
        
        return bundle[photoLibraryUsageDescriptionKey] as? String
    }
}

// MARK: - Presentation Helper

extension PhotoPickerPermissionHandler {
    
    /**
     * Presents an image picker after ensuring permission is granted.
     *
     * This is a convenience method that:
     * 1. Checks current permission status
     * 2. Requests permission if needed
     * 3. Presents the picker on success
     * 4. Shows a settings alert on denial
     *
     * - Parameters:
     *   - picker: The picker view controller to present
     *   - from viewController: The view controller to present from
     *   - onPick: Called when the user picks an image
     *   - onCancel: Called when the user cancels
     */
    static func presentImagePicker(
        _ picker: PHPickerViewController,
        from viewController: UIViewController,
        onPick: @escaping (PHPickerResult) -> Void,
        onCancel: @escaping () -> Void
    ) {
        // Check permission first
        if isAuthorized() {
            // Present picker directly
            presentPicker(picker, from: viewController, onPick: onPick, onCancel: onCancel)
        } else {
            // Request permission
            requestPermission { granted in
                if granted {
                    presentPicker(picker, from: viewController, onPick: onPick, onCancel: onCancel)
                } else {
                    // Show settings alert
                    openSettingsToEnableAccess(from: viewController)
                }
            }
        }
    }
    
    /**
     * Internal method to present the picker with delegate set up.
     */
    private static func presentPicker(
        _ picker: PHPickerViewController,
        from viewController: UIViewController,
        onPick: @escaping (PHPickerResult) -> Void,
        onCancel: @escaping () -> Void
    ) {
        // Set up the delegate
        picker.delegate = createPickerDelegate(onPick: onPick, onCancel: onCancel)
        
        // Present the picker
        viewController.present(picker, animated: true)
    }
    
    /**
     * Creates a delegate for the picker.
     */
    private static func createPickerDelegate(
        onPick: @escaping (PHPickerResult) -> Void,
        onCancel: @escaping () -> Void
    ) -> PHPickerViewControllerDelegate {
        return PhotoPickerDelegate(onPick: onPick, onCancel: onCancel)
    }
}

/**
 * PHPickerViewControllerDelegate implementation for handling picker results.
 */
private class PhotoPickerDelegate: NSObject, PHPickerViewControllerDelegate {
    private let onPick: (PHPickerResult) -> Void
    private let onCancel: () -> Void
    
    init(onPick: @escaping (PHPickerResult) -> Void, onCancel: @escaping () -> Void) {
        self.onPick = onPick
        self.onCancel = onCancel
        super.init()
    }
    
    func picker(
        _ picker: PHPickerViewController,
        didFinishPicking results: [PHPickerResult]
    ) {
        picker.dismiss(animated: true) {
            if let firstResult = results.first {
                self.onPick(firstResult)
            } else {
                self.onCancel()
            }
        }
    }
}
