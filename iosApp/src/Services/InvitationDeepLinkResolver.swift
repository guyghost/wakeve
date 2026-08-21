import Foundation

private struct InvitationDeepLinkResolutionPayload: Decodable {
    let eventId: String
    let isValid: Bool
}

/// Resolves opaque, server-issued invitation tokens at the network boundary.
/// The app never derives an event identity from token bytes locally.
final class InvitationDeepLinkResolver {
    private let session: URLSession
    private let baseURL: URL

    init(
        session: URLSession = .shared,
        baseURL: URL = InvitationDeepLinkResolver.defaultBaseURL
    ) {
        self.session = session
        self.baseURL = baseURL
    }

    func resolve(token: String) async -> String? {
        let normalized = token.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !normalized.isEmpty, normalized.count <= 256 else { return nil }
        let url = baseURL
            .appendingPathComponent("api")
            .appendingPathComponent("invite")
            .appendingPathComponent(normalized)
        var request = URLRequest(url: url)
        request.httpMethod = "GET"
        request.setValue("application/json", forHTTPHeaderField: "Accept")

        guard let (data, response) = try? await session.data(for: request),
              (response as? HTTPURLResponse)?.statusCode == 200,
              let payload = try? JSONDecoder().decode(
                InvitationDeepLinkResolutionPayload.self,
                from: data
              ),
              payload.isValid,
              !payload.eventId.isEmpty
        else {
            return nil
        }
        return payload.eventId
    }

    nonisolated private static var defaultBaseURL: URL {
        #if DEBUG
        URL(string: "http://localhost:8080")!
        #else
        URL(string: "https://api.wakeve.app")!
        #endif
    }
}
