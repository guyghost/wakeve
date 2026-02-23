import SwiftUI
import Foundation

/**
 * Deep link types for Wakeve iOS app.
 *
 * Represents the different types of deep links that can be handled.
 */
enum DeepLinkType {
    case eventDetail(eventId: String)
    case pollVoting(eventId: String)
    case meetingDetail(meetingId: String)
    case invite(token: String)
}

/**
 * Deep link service for Wakeve iOS app.
 *
 * Handles deep link navigation for various Wakeve features:
 * - wakeve://event/{id} - Navigate to event details
 * - wakeve://poll/{eventId} - Navigate to poll voting
 * - wakeve://meeting/{meetingId} - Navigate to meeting details
 * - wakeve://invite/{token} - Handle invite tokens
 *
 * Also supports Universal Links for https://wakeve.app URLs (future enhancement).
 *
 * Pattern: Functional Core (parseDeepLink) + Imperative Shell (DeepLinkService)
 */
@MainActor
class DeepLinkService: ObservableObject {

    // MARK: - Properties

    /// Current pending deep link to handle
    @Published var pendingDeepLink: DeepLinkType? = nil

    /// Navigation path for deep link (used in SwiftUI Navigation)
    @Published var navigationPath: [String] = []

    // MARK: - Initialization

    init() {
        Log.debug("DeepLinkService initialized")
    }

    // MARK: - Deep Link Parsing

    /**
     * Parse a deep link URL into a DeepLinkType.
     *
     * This is a pure function (no side effects) that extracts parameters from URL.
     *
     * - Parameter url: The deep link URL to parse
     * - Returns: Parsed DeepLinkType or nil if invalid
     */
    func parseDeepLink(_ url: URL) -> DeepLinkType? {
        Log.debug("Parsing deep link: \(url.absoluteString)")

        guard let scheme = url.scheme else {
            Log.error("Deep link missing scheme: \(url.absoluteString)")
            return nil
        }

        // Validate scheme
        guard scheme == "wakeve" else {
            // Check for Universal Links (https://wakeve.app)
            if scheme == "https" && url.host == "wakeve.app" {
                return parseUniversalLink(url)
            }

            Log.error("Invalid deep link scheme: \(scheme)")
            return nil
        }

        guard let host = url.host else {
            Log.error("Deep link missing host: \(url.absoluteString)")
            return nil
        }

        let pathComponents = url.pathComponents.filter { !$0.isEmpty }

        switch host {
        case "event":
            // wakeve://event/{id}
            if let eventId = pathComponents.first {
                return .eventDetail(eventId: eventId)
            } else {
                Log.error("Deep link missing event ID")
                return nil
            }

        case "poll":
            // wakeve://poll/{eventId}
            if let eventId = pathComponents.first {
                return .pollVoting(eventId: eventId)
            } else {
                Log.error("Deep link missing event ID in poll")
                return nil
            }

        case "meeting":
            // wakeve://meeting/{meetingId}
            if let meetingId = pathComponents.first {
                return .meetingDetail(meetingId: meetingId)
            } else {
                Log.error("Deep link missing meeting ID")
                return nil
            }

        case "invite":
            // wakeve://invite/{token}
            if let token = pathComponents.first {
                return .invite(token: token)
            } else {
                Log.error("Deep link missing invite token")
                return nil
            }

        default:
            Log.error("Unknown deep link host: \(host)")
            return nil
        }
    }

    /**
     * Parse a Universal Link URL.
     *
     * Universal Links use https://wakeve.app instead of wakeve:// scheme.
     * This allows users to open links from browsers without the app installed.
     *
     * - Parameter url: The Universal Link URL to parse
     * - Returns: Parsed DeepLinkType or nil if invalid
     */
    private func parseUniversalLink(_ url: URL) -> DeepLinkType? {
        Log.debug("Parsing Universal Link: \(url.absoluteString)")

        // Remove leading "/" from path
        let path = url.pathComponents.filter { !$0.isEmpty }

        guard !path.isEmpty else {
            return nil
        }

        let resourceType = path[0]

        switch resourceType {
        case "event":
            // https://wakeve.app/event/{id}
            if path.count > 1 {
                let eventId = path[1]
                return .eventDetail(eventId: eventId)
            }

        case "poll":
            // https://wakeve.app/poll/{eventId}
            if path.count > 1 {
                let eventId = path[1]
                return .pollVoting(eventId: eventId)
            }

        case "meeting":
            // https://wakeve.app/meeting/{meetingId}
            if path.count > 1 {
                let meetingId = path[1]
                return .meetingDetail(meetingId: meetingId)
            }

        case "invite":
            // https://wakeve.app/invite/{token}
            if path.count > 1 {
                let token = path[1]
                return .invite(token: token)
            }

        default:
            Log.error("Unknown Universal Link resource: \(resourceType)")
            return nil
        }

        return nil
    }

    // MARK: - Deep Link Handling

    /**
     * Handle a deep link.
     *
     * This method parses the URL and updates the navigation path to navigate
     * to the appropriate screen. It handles edge cases such as:
     * - Invalid URLs
     * - Unauthenticated users (may need to show auth screen first)
     * - Non-existent events/meetings
     *
     * - Parameter url: The deep link URL to handle
     * - Parameter isAuthenticated: Whether the user is authenticated
     * - Returns: true if deep link was handled successfully, false otherwise
     */
    @discardableResult
    func handleDeepLink(_ url: URL, isAuthenticated: Bool = true) -> Bool {
        Log.debug("Handling deep link: \(url.absoluteString)")

        // Parse deep link
        guard let deepLink = parseDeepLink(url) else {
            Log.warning("Failed to parse deep link: \(url.absoluteString)")
            return false
        }

        // Store as pending deep link
        pendingDeepLink = deepLink

        // Handle deep link based on type
        switch deepLink {
        case .eventDetail(let eventId):
            return handleEventDetail(eventId: eventId, isAuthenticated: isAuthenticated)

        case .pollVoting(let eventId):
            return handlePollVoting(eventId: eventId, isAuthenticated: isAuthenticated)

        case .meetingDetail(let meetingId):
            return handleMeetingDetail(meetingId: meetingId, isAuthenticated: isAuthenticated)

        case .invite(let token):
            return handleInvite(token: token, isAuthenticated: isAuthenticated)
        }
    }

    /**
     * Handle deep link to event details.
     *
     * - Parameter eventId: The event ID from deep link
     * - Parameter isAuthenticated: Whether the user is authenticated
     * - Returns: true if navigation was successful
     */
    private func handleEventDetail(eventId: String, isAuthenticated: Bool) -> Bool {
        Log.debug("Navigating to event detail: \(eventId)")

        // TODO: Check if event exists (optional validation)
        // For now, navigate directly

        // Update navigation path for SwiftUI
        navigationPath = ["event", eventId]

        return true
    }

    /**
     * Handle deep link to poll voting.
     *
     * - Parameter eventId: The event ID from deep link
     * - Parameter isAuthenticated: Whether the user is authenticated
     * - Returns: true if navigation was successful
     */
    private func handlePollVoting(eventId: String, isAuthenticated: Bool) -> Bool {
        Log.debug("Navigating to poll voting: \(eventId)")

        // Update navigation path for SwiftUI
        navigationPath = ["event", eventId, "poll"]

        return true
    }

    /**
     * Handle deep link to meeting details.
     *
     * - Parameter meetingId: The meeting ID from deep link
     * - Parameter isAuthenticated: Whether the user is authenticated
     * - Returns: true if navigation was successful
     */
    private func handleMeetingDetail(meetingId: String, isAuthenticated: Bool) -> Bool {
        Log.debug("Navigating to meeting detail: \(meetingId)")

        // Update navigation path for SwiftUI
        navigationPath = ["meeting", meetingId]

        return true
    }

    /**
     * Handle deep link to invite.
     *
     * Resolves the invitation code and navigates to the event.
     * If the user is authenticated, automatically accepts the invitation.
     * If not, stores the pending invite for processing after authentication.
     *
     * - Parameter token: The invite token/code from deep link
     * - Parameter isAuthenticated: Whether the user is authenticated
     * - Returns: true if navigation was successful
     */
    private func handleInvite(token: String, isAuthenticated: Bool) -> Bool {
        Log.debug("Handling invite with token: \(token)")

        // Store the pending invite code
        pendingInviteCode = token

        // Update navigation path to trigger invite handling in the UI
        navigationPath = ["invite", token]

        return true
    }

    /// Pending invitation code waiting to be processed
    @Published var pendingInviteCode: String? = nil

    /// Clear the pending invite code after processing
    func clearPendingInvite() {
        pendingInviteCode = nil
        Log.debug("Cleared pending invite code")
    }

    /**
     * Clear the pending deep link.
     *
     * Call this after the deep link has been handled to clear the state.
     */
    func clearPendingDeepLink() {
        pendingDeepLink = nil
        Log.debug("Cleared pending deep link")
    }

    /**
     * Reset navigation path.
     *
     * Call this to clear the navigation path and return to root.
     */
    func resetNavigation() {
        navigationPath = []
        Log.debug("Reset navigation path")
    }
}

// MARK: - Logger

private enum Log {
    static func debug(_ message: String) {
        #if DEBUG
        print("[DeepLinkService] DEBUG: \(message)")
        #endif
    }

    static func warning(_ message: String) {
        print("[DeepLinkService] WARNING: \(message)")
    }

    static func error(_ message: String) {
        print("[DeepLinkService] ERROR: \(message)")
    }
}
