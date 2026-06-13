import Foundation

enum ModerationTargetType: String, Codable, CaseIterable, Identifiable {
    case comment = "COMMENT"
    case chatMessage = "CHAT_MESSAGE"
    case event = "EVENT"
    case user = "USER"

    var id: String { rawValue }
}

enum ModerationReportReason: String, Codable, CaseIterable, Identifiable {
    case harassment = "HARASSMENT"
    case hateOrAbuse = "HATE_OR_ABUSE"
    case sexualContent = "SEXUAL_CONTENT"
    case violenceOrThreat = "VIOLENCE_OR_THREAT"
    case spamOrScam = "SPAM_OR_SCAM"
    case privateInformation = "PRIVATE_INFORMATION"
    case other = "OTHER"

    var id: String { rawValue }

    var displayName: String {
        switch self {
        case .harassment: return String(localized: "moderation.reason.harassment")
        case .hateOrAbuse: return String(localized: "moderation.reason.hate_or_abuse")
        case .sexualContent: return String(localized: "moderation.reason.sexual_content")
        case .violenceOrThreat: return String(localized: "moderation.reason.violence_or_threat")
        case .spamOrScam: return String(localized: "moderation.reason.spam_or_scam")
        case .privateInformation: return String(localized: "moderation.reason.private_information")
        case .other: return String(localized: "moderation.reason.other")
        }
    }
}

struct ModerationActionTarget: Identifiable, Equatable {
    let id = UUID()
    let type: ModerationTargetType
    let targetId: String
    let eventId: String?
    let authorId: String?
    let displayName: String
    let allowsBlock: Bool
}

struct ModerationReportResponse: Decodable {
    let id: String
    let targetType: String
    let targetId: String
    let status: String
}

struct ModerationBlockResponse: Decodable {
    let id: String
    let blockerUserId: String
    let blockedUserId: String
    let eventId: String?
}

final class ModerationService {
    private let baseUrl: String = {
        #if DEBUG
        return "http://localhost:8080/api"
        #else
        return "https://api.wakeve.app/api"
        #endif
    }()

    private let tokenStorage: SecureTokenStorageProtocol
    private let session: URLSession

    init(
        tokenStorage: SecureTokenStorageProtocol = SecureTokenStorage(),
        session: URLSession = .shared
    ) {
        self.tokenStorage = tokenStorage
        self.session = session
    }

    func report(
        target: ModerationActionTarget,
        reason: ModerationReportReason,
        details: String?
    ) async throws -> ModerationReportResponse {
        let body = CreateModerationReportBody(
            targetType: target.type,
            targetId: target.targetId,
            eventId: target.eventId,
            reason: reason,
            details: details?.nilIfBlank
        )
        var request = try await authorizedRequest(path: "/moderation/reports", method: "POST")
        request.httpBody = try JSONEncoder().encode(body)

        let (data, response) = try await session.data(for: request)
        try validate(response: response, data: data)
        return try JSONDecoder().decode(ModerationReportResponse.self, from: data)
    }

    func blockUser(
        userId: String,
        eventId: String?,
        reason: ModerationReportReason?
    ) async throws -> ModerationBlockResponse {
        let body = CreateUserBlockBody(
            blockedUserId: userId,
            eventId: eventId,
            reason: reason
        )
        var request = try await authorizedRequest(path: "/moderation/blocks", method: "POST")
        request.httpBody = try JSONEncoder().encode(body)

        let (data, response) = try await session.data(for: request)
        try validate(response: response, data: data)
        return try JSONDecoder().decode(ModerationBlockResponse.self, from: data)
    }

    func unblockUser(
        userId: String,
        eventId: String?
    ) async throws {
        let encodedUserId = userId.addingPercentEncoding(withAllowedCharacters: .urlPathAllowed) ?? userId
        var path = "/moderation/blocks/\(encodedUserId)"
        if let eventId, !eventId.isEmpty {
            let encodedEventId = eventId.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) ?? eventId
            path += "?eventId=\(encodedEventId)"
        }
        let request = try await authorizedRequest(path: path, method: "DELETE")

        let (data, response) = try await session.data(for: request)
        try validate(response: response, data: data)
    }

    private func authorizedRequest(path: String, method: String) async throws -> URLRequest {
        guard let token = await tokenStorage.getAccessToken(), !token.isEmpty else {
            throw AuthError.authenticationFailed("Authentication required")
        }
        let url = URL(string: "\(baseUrl)\(path)")!
        var request = URLRequest(url: url)
        request.httpMethod = method
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        return request
    }

    private func validate(response: URLResponse, data: Data) throws {
        guard let httpResponse = response as? HTTPURLResponse else {
            throw AuthError.networkError
        }
        guard (200..<300).contains(httpResponse.statusCode) else {
            let error = try? JSONDecoder().decode(ModerationErrorResponse.self, from: data)
            throw AuthError.serverError(error?.message ?? "Moderation request failed")
        }
    }
}

private struct ModerationErrorResponse: Decodable {
    let message: String
}

private struct CreateModerationReportBody: Encodable {
    let targetType: ModerationTargetType
    let targetId: String
    let eventId: String?
    let reason: ModerationReportReason
    let details: String?
}

private struct CreateUserBlockBody: Encodable {
    let blockedUserId: String
    let eventId: String?
    let reason: ModerationReportReason?
}

private extension String {
    var nilIfBlank: String? {
        let trimmed = trimmingCharacters(in: .whitespacesAndNewlines)
        return trimmed.isEmpty ? nil : trimmed
    }
}
