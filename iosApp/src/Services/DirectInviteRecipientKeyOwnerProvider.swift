import CryptoKit
import Foundation
import Security
import Shared

/// Platform digest boundary for direct-invite recipient protection.
final class KeychainDirectInviteRecipientDigestPort: NSObject, DirectInviteRecipientDigestPort {
    private enum SecretReadResult {
        case success(Data)
        case failure(OSStatus)
    }

    private static let secretByteCount = 32

    private let signingKey: SymmetricKey

    init?(
        service: String = "com.guyghost.wakeve.direct-invite-recipient-key",
        account: String = "recipient-hmac-v1"
    ) {
        guard !service.isEmpty,
              !account.isEmpty,
              let secret = Self.loadOrCreateSecret(service: service, account: account),
              secret.count == Self.secretByteCount
        else {
            return nil
        }

        signingKey = SymmetricKey(data: secret)
        super.init()
    }

    func hmacSha256(normalizedRecipient: String) -> String? {
        guard !normalizedRecipient.isEmpty,
              let recipientData = normalizedRecipient.data(using: .utf8)
        else {
            return nil
        }

        return HMAC<SHA256>
            .authenticationCode(for: recipientData, using: signingKey)
            .map { String(format: "%02x", $0) }
            .joined()
    }

    private static func loadOrCreateSecret(service: String, account: String) -> Data? {
        switch readSecret(service: service, account: account) {
        case .success(let secret):
            return secret.count == secretByteCount ? secret : nil
        case .failure(let status) where status == errSecItemNotFound:
            break
        case .failure:
            return nil
        }

        var bytes = [UInt8](repeating: 0, count: secretByteCount)
        let randomStatus = bytes.withUnsafeMutableBytes { buffer in
            guard let baseAddress = buffer.baseAddress else { return errSecParam }
            return SecRandomCopyBytes(kSecRandomDefault, buffer.count, baseAddress)
        }
        guard randomStatus == errSecSuccess else { return nil }

        let secret = Data(bytes)
        var item = baseQuery(service: service, account: account)
        item[kSecValueData as String] = secret
        item[kSecAttrAccessible as String] = kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly

        switch SecItemAdd(item as CFDictionary, nil) {
        case errSecSuccess:
            return secret
        case errSecDuplicateItem:
            guard case .success(let persistedSecret) = readSecret(
                service: service,
                account: account
            ) else {
                return nil
            }
            return persistedSecret.count == secretByteCount ? persistedSecret : nil
        default:
            return nil
        }
    }

    private static func readSecret(
        service: String,
        account: String
    ) -> SecretReadResult {
        var query = baseQuery(service: service, account: account)
        query[kSecReturnData as String] = true
        query[kSecMatchLimit as String] = kSecMatchLimitOne

        var item: CFTypeRef?
        let status = SecItemCopyMatching(query as CFDictionary, &item)
        guard status == errSecSuccess else { return .failure(status) }
        guard let secret = item as? Data else { return .failure(errSecDecode) }
        return .success(secret)
    }

    private static func baseQuery(service: String, account: String) -> [String: Any] {
        [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: account
        ]
    }
}

struct DirectInviteRecipientContext {
    let capability: any DirectInviteCapability
    let recipientKeyOwner: DirectInviteRecipientKeyOwner?
    let deliverySealer: (any DirectInviteDeliverySealer)?
    let deliveryTransport: (any DirectInviteDeliveryTransport)?
    let isReady: Bool
    let generation: Int

    static func unavailable(_ reason: String, generation: Int = 0) -> Self {
        Self(
            capability: DirectInviteCapabilityUnavailable(reason: reason),
            recipientKeyOwner: nil,
            deliverySealer: nil,
            deliveryTransport: nil,
            isReady: false,
            generation: generation
        )
    }
}

typealias DirectInviteAccessTokenReader = () async -> String?

@MainActor
protocol DirectInviteContextProvider: AnyObject {
    func context(eventId: String, actorId: String) -> DirectInviteRecipientContext
    func refresh(eventId: String, actorId: String) async
}

/// Production composition owner. It obtains authorization from the backend,
/// then installs the platform HMAC, AEAD sealing and authenticated transport
/// ports as one indivisible context. A missing dimension remains fail-closed.
@MainActor
final class DirectInviteProductionOwner: ObservableObject, DirectInviteContextProvider {
    @Published private(set) var revision = 0

    private let session: URLSession
    private let baseURL: URL
    private let tokenReader: DirectInviteAccessTokenReader
    private var contexts: [String: DirectInviteRecipientContext] = [:]

    init(
        session: URLSession = .shared,
        baseURL: URL = DirectInviteProductionOwner.defaultBaseURL,
        tokenReader: @escaping DirectInviteAccessTokenReader = {
            await SecureTokenStorage().getAccessToken()
        }
    ) {
        self.session = session
        self.baseURL = baseURL
        self.tokenReader = tokenReader
    }

    func context(eventId: String, actorId: String) -> DirectInviteRecipientContext {
        contexts[cacheKey(eventId: eventId, actorId: actorId)] ??
            .unavailable("CAPABILITY_NOT_LOADED", generation: revision)
    }

    func refresh(eventId: String, actorId: String) async {
        let key = cacheKey(eventId: eventId, actorId: actorId)
        guard let token = await tokenReader(), !token.isEmpty,
              let capabilityResponse = await loadCapability(
                eventId: eventId,
                actorId: actorId,
                token: token
              ),
              let digestPort = KeychainDirectInviteRecipientDigestPort(),
              let sealer = DirectInviteAEADSealer(
                publicKeyBase64: capabilityResponse.sealingPublicKey,
                keyVersion: capabilityResponse.keyVersion
              )
        else {
            install(.unavailable("SECURE_DELIVERY_UNAVAILABLE"), for: key)
            return
        }

        let transport = DirectInviteAuthenticatedTransport(
            session: session,
            baseURL: baseURL,
            tokenReader: tokenReader
        )
        install(
            DirectInviteRecipientContext(
                capability: DirectInviteCapabilityReady(
                    eventId: capabilityResponse.eventId,
                    actorId: capabilityResponse.actorId,
                    accessRevision: capabilityResponse.accessRevision,
                    allowedEventStatuses: Set([EventStatus.draft])
                ),
                recipientKeyOwner: DirectInviteRecipientKeyOwner(
                    digestPort: digestPort,
                    keyVersion: capabilityResponse.keyVersion
                ),
                deliverySealer: sealer,
                deliveryTransport: transport,
                isReady: true,
                generation: revision + 1
            ),
            for: key
        )
    }

    private func install(_ context: DirectInviteRecipientContext, for key: String) {
        revision += 1
        contexts[key] = DirectInviteRecipientContext(
            capability: context.capability,
            recipientKeyOwner: context.recipientKeyOwner,
            deliverySealer: context.deliverySealer,
            deliveryTransport: context.deliveryTransport,
            isReady: context.isReady,
            generation: revision
        )
    }

    private func loadCapability(
        eventId: String,
        actorId: String,
        token: String
    ) async -> DirectInviteCapabilityPayload? {
        guard let url = directInviteURL(eventId: eventId, suffix: "capability") else {
            return nil
        }
        var request = URLRequest(url: url)
        request.httpMethod = "GET"
        request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        request.setValue("application/json", forHTTPHeaderField: "Accept")

        guard let (data, response) = try? await session.data(for: request),
              (response as? HTTPURLResponse)?.statusCode == 200,
              let payload = try? JSONDecoder().decode(
                DirectInviteCapabilityPayload.self,
                from: data
              ),
              payload.state == "READY",
              payload.eventId == eventId,
              payload.actorId == actorId,
              payload.accessRevision > 0,
              payload.keyVersion > 0,
              Data(base64Encoded: payload.sealingPublicKey)?.count == 32
        else {
            return nil
        }
        return payload
    }

    private func directInviteURL(eventId: String, suffix: String) -> URL? {
        guard !eventId.isEmpty else { return nil }
        return baseURL
            .appendingPathComponent("api")
            .appendingPathComponent("events")
            .appendingPathComponent(eventId)
            .appendingPathComponent("direct-invites")
            .appendingPathComponent(suffix)
    }

    private func cacheKey(eventId: String, actorId: String) -> String {
        "\(eventId)|\(actorId)"
    }

    nonisolated private static var defaultBaseURL: URL {
        #if DEBUG
        URL(string: "http://localhost:8080")!
        #else
        URL(string: "https://api.wakeve.app")!
        #endif
    }
}

private struct DirectInviteCapabilityPayload: Decodable {
    let state: String
    let eventId: String
    let actorId: String
    let accessRevision: Int64
    let sealingPublicKey: String
    let keyVersion: Int32
}

/// AEAD envelope using an ephemeral X25519 key. The delivery provider owns the
/// matching private key; the app never receives it and stores no plaintext.
final class DirectInviteAEADSealer: NSObject, DirectInviteDeliverySealer {
    private let providerPublicKey: Curve25519.KeyAgreement.PublicKey
    private let keyVersion: Int32

    init?(publicKeyBase64: String, keyVersion: Int32) {
        guard keyVersion > 0,
              let data = Data(base64Encoded: publicKeyBase64),
              let publicKey = try? Curve25519.KeyAgreement.PublicKey(rawRepresentation: data)
        else {
            return nil
        }
        self.providerPublicKey = publicKey
        self.keyVersion = keyVersion
        super.init()
    }

    func seal(
        binding: DirectInviteDeliveryBinding,
        recipientKey: RecipientKey,
        normalizedRecipient: String,
        expiresAt: String
    ) -> DirectInviteDeliveryEnvelope? {
        guard !normalizedRecipient.isEmpty,
              let plaintext = normalizedRecipient.data(using: .utf8)
        else {
            return nil
        }
        return try? autoreleasepool {
            let ephemeralKey = Curve25519.KeyAgreement.PrivateKey()
            let sharedSecret = try ephemeralKey.sharedSecretFromKeyAgreement(
                with: providerPublicKey
            )
            let bindingString =
                "\(binding.eventId)|\(binding.actorId)|\(binding.accessRevision)|" +
                "\(binding.batchId)|\(binding.operationId)|\(recipientKey.value)"
            let bindingData = Data(bindingString.utf8)
            let symmetricKey = sharedSecret.hkdfDerivedSymmetricKey(
                using: SHA256.self,
                salt: Data(SHA256.hash(data: bindingData)),
                sharedInfo: bindingData,
                outputByteCount: 32
            )
            let sealed = try ChaChaPoly.seal(
                plaintext,
                using: symmetricKey,
                authenticating: bindingData
            )
            let ciphertext = [
                "x25519-chacha20poly1305-v\(keyVersion)",
                ephemeralKey.publicKey.rawRepresentation.base64EncodedString(),
                sealed.combined.base64EncodedString()
            ].joined(separator: ":")
            return DirectInviteDeliveryEnvelope(
                binding: binding,
                recipientKey: recipientKey,
                ciphertext: ciphertext,
                keyVersion: keyVersion,
                expiresAt: expiresAt
            )
        }
    }
}

/// Authenticated client transport for the backend batch endpoint.
final class DirectInviteAuthenticatedTransport: NSObject, DirectInviteDeliveryTransport {
    private let session: URLSession
    private let baseURL: URL
    private let tokenReader: DirectInviteAccessTokenReader

    init(
        session: URLSession,
        baseURL: URL,
        tokenReader: @escaping DirectInviteAccessTokenReader
    ) {
        self.session = session
        self.baseURL = baseURL
        self.tokenReader = tokenReader
        super.init()
    }

    func dispatch(
        request: DirectInviteDeliveryRequest
    ) async throws -> any DirectInviteDeliveryResult {
        guard let token = await tokenReader(), !token.isEmpty else {
            return DirectInviteDeliveryResultDeferred(error: .networkUnavailable)
        }
        let binding = request.binding
        let endpoint = baseURL
            .appendingPathComponent("api")
            .appendingPathComponent("events")
            .appendingPathComponent(binding.eventId)
            .appendingPathComponent("direct-invites")
            .appendingPathComponent("batches")
        let envelopes = request.envelopes.sorted {
            $0.recipientKey.value < $1.recipientKey.value
        }
        let payload = DirectInviteBatchPayload(
            accessRevision: binding.accessRevision,
            batchId: binding.batchId,
            operationId: binding.operationId,
            envelopes: envelopes.map {
                DirectInviteEnvelopePayload(
                    recipientKey: $0.recipientKey.value,
                    ciphertext: $0.ciphertext,
                    keyVersion: $0.keyVersion,
                    expiresAt: $0.expiresAt
                )
            }
        )
        var urlRequest = URLRequest(url: endpoint)
        urlRequest.httpMethod = "POST"
        urlRequest.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        urlRequest.setValue("application/json", forHTTPHeaderField: "Content-Type")
        urlRequest.httpBody = try JSONEncoder().encode(payload)

        let (data, response) = try await session.data(for: urlRequest)
        guard let http = response as? HTTPURLResponse else {
            return DirectInviteDeliveryResultDeferred(error: .networkUnavailable)
        }
        if http.statusCode == 409 {
            return DirectInviteDeliveryResultRejected(error: .conflict)
        }
        guard http.statusCode == 200,
              let acknowledgement = try? JSONDecoder().decode(
                DirectInviteBatchAcknowledgement.self,
                from: data
              ),
              acknowledgement.status == "ACKNOWLEDGED",
              acknowledgement.batchId == binding.batchId,
              acknowledgement.operationId == binding.operationId
        else {
            return DirectInviteDeliveryResultDeferred(error: .serverUnavailable)
        }

        var outcomes: [RecipientKey: any DirectInviteRecipientOutcome] = [:]
        for result in acknowledgement.outcomes {
            guard let key = envelopes.first(where: {
                $0.recipientKey.value == result.recipientKey
            })?.recipientKey else {
                return DirectInviteDeliveryResultRejected(error: .conflict)
            }
            switch result.status {
            case "SERVER_ACCEPTED":
                guard let invitationId = result.invitationId, !invitationId.isEmpty else {
                    return DirectInviteDeliveryResultRejected(error: .permanentFailure)
                }
                outcomes[key] = DirectInviteRecipientOutcomeServerAccepted(
                    invitationId: invitationId
                )
            case "INVALID":
                outcomes[key] = DirectInviteRecipientOutcomeInvalid(
                    reason: result.reasonCode ?? "VALIDATION"
                )
            default:
                return DirectInviteDeliveryResultRejected(error: .permanentFailure)
            }
        }
        guard outcomes.count == envelopes.count else {
            return DirectInviteDeliveryResultRejected(error: .conflict)
        }
        return DirectInviteDeliveryResultAcknowledged(
            batchId: binding.batchId,
            operationId: binding.operationId,
            outcomesByRecipientKey: outcomes
        )
    }
}

private struct DirectInviteBatchPayload: Encodable {
    let accessRevision: Int64
    let batchId: String
    let operationId: String
    let envelopes: [DirectInviteEnvelopePayload]
}

private struct DirectInviteEnvelopePayload: Encodable {
    let recipientKey: String
    let ciphertext: String
    let keyVersion: Int32
    let expiresAt: String
}

private struct DirectInviteBatchAcknowledgement: Decodable {
    let batchId: String
    let operationId: String
    let status: String
    let outcomes: [DirectInviteOutcomePayload]
}

private struct DirectInviteOutcomePayload: Decodable {
    let recipientKey: String
    let status: String
    let invitationId: String?
    let reasonCode: String?
}
