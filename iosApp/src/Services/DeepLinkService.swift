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
    case eventInformation(eventId: String)
    case eventArchive(eventId: String)
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

    init(eventRoute: IosEventRoute) {
        switch eventRoute {
        case .detail(let eventId): self = .eventDetail(eventId: eventId)
        case .participants(let eventId): self = .eventParticipants(eventId: eventId)
        case .information(let eventId): self = .eventInformation(eventId: eventId)
        case .archive(let eventId): self = .eventArchive(eventId: eventId)
        case .pollVoting(let eventId): self = .pollVoting(eventId: eventId)
        case .pollResults(let eventId): self = .pollResults(eventId: eventId)
        case .scenarioList(let eventId): self = .scenarioList(eventId: eventId)
        case .scenarioComparison(let eventId): self = .scenarioComparison(eventId: eventId)
        case .scenarioManagement(let eventId): self = .scenarioManagement(eventId: eventId)
        case .scenarioDetail(let eventId, let scenarioId):
            self = .scenarioDetail(eventId: eventId, scenarioId: scenarioId)
        case .budgetOverview(let eventId): self = .budgetOverview(eventId: eventId)
        case .budgetDetail(let eventId, let budgetItemId):
            self = .budgetDetail(eventId: eventId, budgetItemId: budgetItemId)
        case .meetingList(let eventId): self = .meetingList(eventId: eventId)
        case .comments(let eventId): self = .eventComments(eventId: eventId)
        case .invitationShare(let eventId): self = .invitationShare(eventId: eventId)
        case .transport(let eventId): self = .transport(eventId: eventId)
        case .accommodation(let eventId): self = .accommodation(eventId: eventId)
        case .meals(let eventId): self = .meals(eventId: eventId)
        case .equipment(let eventId): self = .equipment(eventId: eventId)
        case .activities(let eventId): self = .activities(eventId: eventId)
        case .payment(let eventId): self = .payment(eventId: eventId)
        case .tricount(let eventId): self = .tricount(eventId: eventId)
        case .photos(let eventId): self = .eventPhotos(eventId: eventId)
        }
    }

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
        case .eventInformation(let eventId):
            return .event(.information(eventId: eventId))
        case .eventArchive(let eventId):
            return .event(.archive(eventId: eventId))
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

    /// Typed navigation intent consumed by the authenticated root.
    @Published private(set) var navigationRoute: IosRoute?

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
        Log.debug("parse_started")

        guard isSafeWakeveURL(url) else {
            Log.error("parse_rejected_unsafe")
            return nil
        }

        guard let scheme = url.scheme else {
            Log.error("parse_rejected_missing_scheme")
            return nil
        }

        let components = URLComponents(url: url, resolvingAgainstBaseURL: false)
        let query = queryItems(from: components)

        if scheme == "https" && url.host == "wakeve.app" {
            return parseUniversalLink(url, query: query)
        }

        guard scheme == "wakeve" else {
            Log.error("parse_rejected_invalid_scheme")
            return nil
        }

        guard let host = url.host else {
            Log.error("parse_rejected_missing_host")
            return nil
        }

        let path = normalizedPathComponents(url)
        return parseResource(host, path: path, query: query)
    }

    private func parseUniversalLink(_ url: URL, query: [String: String]) -> DeepLinkType? {
        Log.debug("parse_universal_link")

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
            Log.error("parse_rejected_unknown_resource")
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

        guard let route = IosEventRoute.parse(
            eventId: first,
            components: path.dropFirst(),
            detailsTab: query["tab"]?.trimmedLowercased
        ) else { return nil }
        return DeepLinkType(eventRoute: route)
    }

    // MARK: - Deep Link Handling

    /**
     * Handle a deep link by parsing and publishing a route path.
     */
    @discardableResult
    func handleDeepLink(_ url: URL, isAuthenticated: Bool = true) -> Bool {
        Log.debug("handle_started")

        guard let deepLink = parseDeepLink(url) else {
            Log.warning("handle_rejected_parse_failure")
            return false
        }

        pendingDeepLink = deepLink

        if case .invite(let token) = deepLink {
            pendingInviteCode = token
        }

        navigationRoute = deepLink.route
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
        navigationRoute = nil
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
