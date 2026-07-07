import SwiftUI
import Foundation

/**
 * Deep link types for Wakeve iOS app.
 *
 * The cases intentionally mirror Android's product-scoped route inventory while
 * keeping iOS navigation native. Private event routes are parsed here and later
 * routed through repository/access checks in `AuthenticatedView`.
 */
enum DeepLinkType: Equatable {
    case home
    case profile
    case settings(category: String?)
    case notifications(filter: String?)
    case notificationPreferences
    case leaderboard
    case organizerDashboard
    case eventCreate
    case eventDetail(eventId: String)
    case eventParticipants(eventId: String)
    case pollVoting(eventId: String)
    case pollResults(eventId: String)
    case scenarioList(eventId: String)
    case scenarioComparison(eventId: String)
    case scenarioManagement(eventId: String)
    case scenarioDetail(eventId: String, scenarioId: String)
    case budgetOverview(eventId: String)
    case budgetDetail(eventId: String, budgetItemId: String)
    case meetingList(eventId: String)
    case meetingDetail(meetingId: String)
    case eventComments(eventId: String)
    case invitationShare(eventId: String)
    case transport(eventId: String)
    case accommodation(eventId: String)
    case meals(eventId: String)
    case equipment(eventId: String)
    case activities(eventId: String)
    case payment(eventId: String)
    case tricount(eventId: String)
    case eventPhotos(eventId: String)
    case invite(token: String)

    var route: IosRoute {
        switch self {
        case .home:
            return .topLevel(.home)
        case .profile:
            return .topLevel(.profile)
        case .settings(let category):
            return category == "notifications" ? .topLevel(.notificationPreferences) : .topLevel(.settings)
        case .notifications(let filter):
            return .topLevel(.notifications(filter: filter))
        case .notificationPreferences:
            return .topLevel(.notificationPreferences)
        case .leaderboard:
            return .topLevel(.leaderboard)
        case .organizerDashboard:
            return .topLevel(.organizerDashboard)
        case .eventCreate:
            return .eventCreate
        case .eventDetail(let eventId):
            return .event(.detail(eventId: eventId))
        case .eventParticipants(let eventId):
            return .event(.participants(eventId: eventId))
        case .pollVoting(let eventId):
            return .event(.pollVoting(eventId: eventId))
        case .pollResults(let eventId):
            return .event(.pollResults(eventId: eventId))
        case .scenarioList(let eventId):
            return .event(.scenarioList(eventId: eventId))
        case .scenarioComparison(let eventId):
            return .event(.scenarioComparison(eventId: eventId))
        case .scenarioManagement(let eventId):
            return .event(.scenarioManagement(eventId: eventId))
        case .scenarioDetail(let eventId, let scenarioId):
            return .event(.scenarioDetail(eventId: eventId, scenarioId: scenarioId))
        case .budgetOverview(let eventId):
            return .event(.budgetOverview(eventId: eventId))
        case .budgetDetail(let eventId, let budgetItemId):
            return .event(.budgetDetail(eventId: eventId, budgetItemId: budgetItemId))
        case .meetingList(let eventId):
            return .event(.meetingList(eventId: eventId))
        case .meetingDetail(let meetingId):
            return .meetingDetail(meetingId: meetingId)
        case .eventComments(let eventId):
            return .event(.comments(eventId: eventId))
        case .invitationShare(let eventId):
            return .event(.invitationShare(eventId: eventId))
        case .transport(let eventId):
            return .event(.transport(eventId: eventId))
        case .accommodation(let eventId):
            return .event(.accommodation(eventId: eventId))
        case .meals(let eventId):
            return .event(.meals(eventId: eventId))
        case .equipment(let eventId):
            return .event(.equipment(eventId: eventId))
        case .activities(let eventId):
            return .event(.activities(eventId: eventId))
        case .payment(let eventId):
            return .event(.payment(eventId: eventId))
        case .tricount(let eventId):
            return .event(.tricount(eventId: eventId))
        case .eventPhotos(let eventId):
            return .event(.photos(eventId: eventId))
        case .invite(let token):
            return .invite(token: token)
        }
    }

    var navigationPath: [String] {
        route.navigationPath
    }
}

enum InvitationTokenCodec {
    private static let eventPrefix = "event-"

    static func invitationCode(forEventId eventId: String) -> String {
        let encoded = Data(eventId.utf8)
            .base64EncodedString()
            .replacingOccurrences(of: "+", with: "-")
            .replacingOccurrences(of: "/", with: "_")
            .replacingOccurrences(of: "=", with: "")
        return "\(eventPrefix)\(encoded)"
    }

    static func eventId(fromInvitationCode code: String) -> String? {
        guard code.hasPrefix(eventPrefix) else { return nil }
        var payload = String(code.dropFirst(eventPrefix.count))
            .replacingOccurrences(of: "-", with: "+")
            .replacingOccurrences(of: "_", with: "/")

        let padding = payload.count % 4
        if padding > 0 {
            payload.append(String(repeating: "=", count: 4 - padding))
        }

        guard let data = Data(base64Encoded: payload) else { return nil }
        return String(data: data, encoding: .utf8)
    }
}

/**
 * Deep link service for Wakeve iOS app.
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

    /// Pending invitation code waiting to be processed
    @Published var pendingInviteCode: String? = nil

    // MARK: - Initialization

    init() {
        Log.debug("DeepLinkService initialized")
    }

    // MARK: - Deep Link Parsing

    /**
     * Parse a Wakeve deep link URL into a route intent.
     *
     * Supported forms include wakeve://... custom scheme links and
     * https://wakeve.app/... universal links.
     */
    func parseDeepLink(_ url: URL) -> DeepLinkType? {
        Log.debug("Parsing deep link: \(url.absoluteString)")

        guard isSafeWakeveURL(url) else {
            Log.error("Unsafe deep link rejected: \(url.absoluteString)")
            return nil
        }

        guard let scheme = url.scheme else {
            Log.error("Deep link missing scheme: \(url.absoluteString)")
            return nil
        }

        let components = URLComponents(url: url, resolvingAgainstBaseURL: false)
        let query = queryItems(from: components)

        if scheme == "https" && url.host == "wakeve.app" {
            return parseUniversalLink(url, query: query)
        }

        guard scheme == "wakeve" else {
            Log.error("Invalid deep link scheme: \(scheme)")
            return nil
        }

        guard let host = url.host else {
            Log.error("Deep link missing host: \(url.absoluteString)")
            return nil
        }

        let path = normalizedPathComponents(url)
        return parseResource(host, path: path, query: query)
    }

    private func parseUniversalLink(_ url: URL, query: [String: String]) -> DeepLinkType? {
        Log.debug("Parsing Universal Link: \(url.absoluteString)")

        let path = normalizedPathComponents(url)
        guard let resourceType = path.first else {
            return nil
        }

        return parseResource(resourceType, path: Array(path.dropFirst()), query: query)
    }

    private func parseResource(_ resource: String, path: [String], query: [String: String]) -> DeepLinkType? {
        switch resource {
        case "home":
            return path.isEmpty ? .home : nil

        case "profile":
            return path.isEmpty ? .profile : nil

        case "settings":
            guard path.isEmpty else { return nil }
            let category = query["category"]?.trimmedLowercased
            return category == "notifications" ? .notificationPreferences : .settings(category: category)

        case "notifications":
            if path == ["preferences"] {
                return .notificationPreferences
            }
            guard path.isEmpty else { return nil }
            let filter = query["filter"]?.trimmedLowercased
            return .notifications(filter: filter == "unread" ? "unread" : nil)

        case "leaderboard":
            return path.isEmpty ? .leaderboard : nil

        case "organizer_dashboard":
            return path.isEmpty ? .organizerDashboard : nil

        case "event":
            return parseEventRoute(path: path, query: query)

        case "poll":
            guard let eventId = path.first, path.count == 1 else { return nil }
            return .pollVoting(eventId: eventId)

        case "meeting":
            guard let meetingId = path.first, path.count == 1 else { return nil }
            return .meetingDetail(meetingId: meetingId)

        case "invite":
            guard let token = path.first, path.count == 1 else { return nil }
            return .invite(token: token)

        default:
            Log.error("Unknown deep link resource: \(resource)")
            return nil
        }
    }

    private func parseEventRoute(path: [String], query: [String: String]) -> DeepLinkType? {
        guard let first = path.first else {
            Log.error("Deep link missing event path")
            return nil
        }

        if first == "create" && path.count == 1 {
            return .eventCreate
        }

        let eventId = first
        guard path.count > 1 else {
            return .eventDetail(eventId: eventId)
        }

        switch path[1] {
        case "details":
            guard path.count == 2 else { return nil }
            switch query["tab"]?.trimmedLowercased {
            case "comments":
                return .eventComments(eventId: eventId)
            case "budget":
                return .budgetOverview(eventId: eventId)
            case "participants":
                return .eventParticipants(eventId: eventId)
            default:
                return .eventDetail(eventId: eventId)
            }

        case "participants":
            return path.count == 2 ? .eventParticipants(eventId: eventId) : nil

        case "poll":
            guard path.count <= 3 else { return nil }
            if path.count == 3 && path[2] == "results" {
                return .pollResults(eventId: eventId)
            }
            return .pollVoting(eventId: eventId)

        case "scenarios":
            guard path.count <= 3 else { return nil }
            if path.count == 3 && path[2] == "compare" {
                return .scenarioComparison(eventId: eventId)
            }
            if path.count == 3 && path[2] == "manage" {
                return .scenarioManagement(eventId: eventId)
            }
            return path.count == 2 ? .scenarioList(eventId: eventId) : nil

        case "scenario":
            guard path.count == 3 else { return nil }
            return .scenarioDetail(eventId: eventId, scenarioId: path[2])

        case "budget":
            guard path.count <= 3 else { return nil }
            if path.count == 3 {
                return .budgetDetail(eventId: eventId, budgetItemId: path[2])
            }
            return .budgetOverview(eventId: eventId)

        case "meetings":
            return path.count == 2 ? .meetingList(eventId: eventId) : nil

        case "comments":
            return path.count == 2 ? .eventComments(eventId: eventId) : nil

        case "invite":
            return path.count == 2 ? .invitationShare(eventId: eventId) : nil

        case "transport":
            return path.count == 2 ? .transport(eventId: eventId) : nil

        case "accommodation":
            return path.count == 2 ? .accommodation(eventId: eventId) : nil

        case "meals":
            return path.count == 2 ? .meals(eventId: eventId) : nil

        case "equipment":
            return path.count == 2 ? .equipment(eventId: eventId) : nil

        case "activities":
            return path.count == 2 ? .activities(eventId: eventId) : nil

        case "payment":
            return path.count == 2 ? .payment(eventId: eventId) : nil

        case "tricount":
            return path.count == 2 ? .tricount(eventId: eventId) : nil

        case "photos":
            return path.count == 2 ? .eventPhotos(eventId: eventId) : nil

        default:
            return nil
        }
    }

    // MARK: - Deep Link Handling

    /**
     * Handle a deep link by parsing and publishing a route path.
     */
    @discardableResult
    func handleDeepLink(_ url: URL, isAuthenticated: Bool = true) -> Bool {
        Log.debug("Handling deep link: \(url.absoluteString)")

        guard let deepLink = parseDeepLink(url) else {
            Log.warning("Failed to parse deep link: \(url.absoluteString)")
            return false
        }

        pendingDeepLink = deepLink

        if case .invite(let token) = deepLink {
            pendingInviteCode = token
        }

        navigationPath = deepLink.navigationPath
        return true
    }

    /// Clear the pending invite code after processing.
    func clearPendingInvite() {
        pendingInviteCode = nil
        Log.debug("Cleared pending invite code")
    }

    /**
     * Clear the pending deep link.
     */
    func clearPendingDeepLink() {
        pendingDeepLink = nil
        Log.debug("Cleared pending deep link")
    }

    /**
     * Reset navigation path.
     */
    func resetNavigation() {
        navigationPath = []
        Log.debug("Reset navigation path")
    }

    // MARK: - Safety

    private func isSafeWakeveURL(_ url: URL) -> Bool {
        guard url.user == nil, url.password == nil, url.fragment == nil else {
            return false
        }

        if let port = url.port {
            if url.scheme == "https" && port != 443 {
                return false
            }
            if url.scheme == "wakeve" {
                return false
            }
        }

        let encodedPath = (URLComponents(url: url, resolvingAgainstBaseURL: false)?.percentEncodedPath ?? url.path)
            .lowercased()
        guard !encodedPath.contains("%2f"), !encodedPath.contains("%5c") else {
            return false
        }

        return normalizedPathComponents(url).allSatisfy { component in
            component != "."
                && component != ".."
                && !component.contains("/")
                && !component.contains("\\")
                && component.count <= 160
        }
    }

    private func normalizedPathComponents(_ url: URL) -> [String] {
        url.pathComponents
            .filter { $0 != "/" && !$0.isEmpty }
            .map { $0.removingPercentEncoding ?? $0 }
    }

    private func queryItems(from components: URLComponents?) -> [String: String] {
        components?.queryItems?.reduce(into: [String: String]()) { result, item in
            result[item.name] = item.value
        } ?? [:]
    }
}

private extension String {
    var trimmedLowercased: String {
        trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
    }
}

// MARK: - Logger

private enum Log {
    static func debug(_ message: String) {
        #if DEBUG
        debugLog("[DeepLinkService] DEBUG: \(message)")
        #endif
    }

    static func warning(_ message: String) {
        debugLog("[DeepLinkService] WARNING: \(message)")
    }

    static func error(_ message: String) {
        debugLog("[DeepLinkService] ERROR: \(message)")
    }
}
