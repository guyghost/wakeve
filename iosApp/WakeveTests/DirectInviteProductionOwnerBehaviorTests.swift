import CryptoKit
import Foundation
import Shared
import XCTest

@testable import Wakeve

@MainActor
final class DirectInviteProductionOwnerBehaviorTests: XCTestCase {
    override func tearDown() {
        DirectInviteURLProtocolStub.handler = nil
        super.tearDown()
    }

    func testCapabilitySealAndDispatchUseAuthenticatedProtectedPayloadAndExactAcknowledgement() async throws {
        let providerKey = Curve25519.KeyAgreement.PrivateKey()
        let recorder = DirectInviteRequestRecorder()
        DirectInviteURLProtocolStub.handler = { request in
            recorder.append(request)
            switch (request.httpMethod, request.url?.path) {
            case ("GET", "/api/events/event-1/direct-invites/capability"):
                return Self.jsonResponse(
                    statusCode: 200,
                    object: [
                        "state": "READY",
                        "eventId": "event-1",
                        "actorId": "actor-1",
                        "accessRevision": 7,
                        "sealingPublicKey": providerKey.publicKey.rawRepresentation
                            .base64EncodedString(),
                        "keyVersion": 3
                    ]
                )
            case ("POST", "/api/events/event-1/direct-invites/batches"):
                let body = try Self.requestBody(request)
                let payload = try XCTUnwrap(
                    JSONSerialization.jsonObject(with: body) as? [String: Any]
                )
                let envelopes = try XCTUnwrap(payload["envelopes"] as? [[String: Any]])
                let recipientKey = try XCTUnwrap(envelopes.first?["recipientKey"] as? String)
                return Self.jsonResponse(
                    statusCode: 200,
                    object: [
                        "batchId": "batch-1",
                        "operationId": "operation-1",
                        "status": "ACKNOWLEDGED",
                        "outcomes": [[
                            "recipientKey": recipientKey,
                            "status": "SERVER_ACCEPTED",
                            "invitationId": "invitation-1"
                        ]]
                    ]
                )
            default:
                XCTFail("Unexpected production request: \(request.httpMethod ?? "nil") \(request.url?.path ?? "nil")")
                return Self.jsonResponse(statusCode: 404, object: [:])
            }
        }

        let owner = DirectInviteProductionOwner(
            session: makeSession(),
            baseURL: URL(string: "https://wakeve.test")!,
            tokenReader: { "access-token" }
        )
        XCTAssertNotNil(
            KeychainDirectInviteRecipientDigestPort(),
            "The production owner cannot become ready if its device-protected digest port is unavailable."
        )
        XCTAssertNotNil(
            DirectInviteAEADSealer(
                publicKeyBase64: providerKey.publicKey.rawRepresentation.base64EncodedString(),
                keyVersion: 3
            ),
            "The provider X25519 key advertised by the capability fixture must be accepted."
        )
        await owner.refresh(eventId: "event-1", actorId: "actor-1")

        let context = owner.context(eventId: "event-1", actorId: "actor-1")
        XCTAssertTrue(
            context.isReady,
            "Exact READY capability should install all production ports; requests=\(recorder.requests)."
        )
        let capability = try XCTUnwrap(context.capability as? DirectInviteCapabilityReady)
        XCTAssertEqual(capability.eventId, "event-1")
        XCTAssertEqual(capability.actorId, "actor-1")
        XCTAssertEqual(capability.accessRevision, 7)

        let binding = DirectInviteDeliveryBinding(
            eventId: "event-1",
            actorId: "actor-1",
            accessRevision: capability.accessRevision,
            batchId: "batch-1",
            operationId: "operation-1"
        )
        let rawRecipient = "Lea.Example@Example.com"
        let protectedRecipient = try XCTUnwrap(
            context.recipientKeyOwner?.protectAndSeal(
                rawRecipientInput: rawRecipient,
                binding: binding,
                expiresAt: "2026-09-01T12:00:00Z",
                sealer: try XCTUnwrap(context.deliverySealer)
            )
        )
        XCTAssertFalse(protectedRecipient.envelope.ciphertext.contains("lea.example@example.com"))
        XCTAssertFalse(protectedRecipient.envelope.ciphertext.contains(rawRecipient))
        XCTAssertTrue(protectedRecipient.envelope.ciphertext.hasPrefix("x25519-chacha20poly1305-v3:"))

        let result = try await XCTUnwrap(context.deliveryTransport).dispatch(
            request: DirectInviteDeliveryRequest(
                binding: binding,
                envelopes: Set([protectedRecipient.envelope])
            )
        )
        let acknowledgement = try XCTUnwrap(
            result as? DirectInviteDeliveryResultAcknowledged
        )
        XCTAssertEqual(acknowledgement.batchId, "batch-1")
        XCTAssertEqual(acknowledgement.operationId, "operation-1")
        XCTAssertEqual(acknowledgement.outcomesByRecipientKey.count, 1)

        let requests = recorder.requests
        XCTAssertEqual(requests.count, 2)
        XCTAssertTrue(requests.allSatisfy {
            $0.value(forHTTPHeaderField: "Authorization") == "Bearer access-token"
        })
        let postBody = try Self.requestBody(try XCTUnwrap(requests.last))
        let serializedPost = String(decoding: postBody, as: UTF8.self)
        XCTAssertFalse(serializedPost.localizedCaseInsensitiveContains("lea.example@example.com"))
        XCTAssertFalse(serializedPost.localizedCaseInsensitiveContains("rawRecipient"))
        let postPayload = try XCTUnwrap(
            JSONSerialization.jsonObject(with: postBody) as? [String: Any]
        )
        let postedEnvelopes = try XCTUnwrap(postPayload["envelopes"] as? [[String: Any]])
        XCTAssertEqual(
            postedEnvelopes.first?["recipientKey"] as? String,
            protectedRecipient.recipientKey.value
        )
        XCTAssertEqual(
            postedEnvelopes.first?["ciphertext"] as? String,
            protectedRecipient.envelope.ciphertext
        )
    }

    func testMismatchedCapabilityRemainsFailClosedAndDoesNotInstallDeliveryPorts() async {
        let providerKey = Curve25519.KeyAgreement.PrivateKey()
        let recorder = DirectInviteRequestRecorder()
        DirectInviteURLProtocolStub.handler = { request in
            recorder.append(request)
            return Self.jsonResponse(
                statusCode: 200,
                object: [
                    "state": "READY",
                    "eventId": "event-1",
                    "actorId": "different-actor",
                    "accessRevision": 7,
                    "sealingPublicKey": providerKey.publicKey.rawRepresentation
                        .base64EncodedString(),
                    "keyVersion": 3
                ]
            )
        }

        let owner = DirectInviteProductionOwner(
            session: makeSession(),
            baseURL: URL(string: "https://wakeve.test")!,
            tokenReader: { "access-token" }
        )
        await owner.refresh(eventId: "event-1", actorId: "actor-1")

        let context = owner.context(eventId: "event-1", actorId: "actor-1")
        XCTAssertFalse(context.isReady)
        XCTAssertNil(context.recipientKeyOwner)
        XCTAssertNil(context.deliverySealer)
        XCTAssertNil(context.deliveryTransport)
        XCTAssertTrue(context.capability is DirectInviteCapabilityUnavailable)
        XCTAssertEqual(recorder.requests.count, 1)
    }

    private func makeSession() -> URLSession {
        let configuration = URLSessionConfiguration.ephemeral
        configuration.protocolClasses = [DirectInviteURLProtocolStub.self]
        return URLSession(configuration: configuration)
    }

    nonisolated private static func jsonResponse(
        statusCode: Int,
        object: [String: Any]
    ) -> DirectInviteURLProtocolStub.Response {
        DirectInviteURLProtocolStub.Response(
            statusCode: statusCode,
            headers: ["Content-Type": "application/json"],
            data: try! JSONSerialization.data(withJSONObject: object)
        )
    }

    nonisolated private static func requestBody(_ request: URLRequest) throws -> Data {
        if let body = request.httpBody {
            return body
        }
        let stream = try XCTUnwrap(
            request.httpBodyStream,
            "URLSession must preserve the encoded batch as data or a body stream."
        )
        stream.open()
        defer { stream.close() }
        var body = Data()
        var buffer = [UInt8](repeating: 0, count: 4_096)
        while true {
            let count = stream.read(&buffer, maxLength: buffer.count)
            if count > 0 {
                body.append(buffer, count: count)
            } else if count == 0 {
                return body
            } else {
                throw stream.streamError ?? URLError(.cannotDecodeContentData)
            }
        }
    }
}

private final class DirectInviteRequestRecorder: @unchecked Sendable {
    private let lock = NSLock()
    private var storage: [URLRequest] = []

    var requests: [URLRequest] {
        lock.withLock { storage }
    }

    func append(_ request: URLRequest) {
        lock.withLock { storage.append(request) }
    }
}

private final class DirectInviteURLProtocolStub: URLProtocol, @unchecked Sendable {
    struct Response {
        let statusCode: Int
        let headers: [String: String]
        let data: Data
    }

    nonisolated(unsafe) static var handler: ((URLRequest) throws -> Response)?

    override class func canInit(with request: URLRequest) -> Bool { true }

    override class func canonicalRequest(for request: URLRequest) -> URLRequest { request }

    override func startLoading() {
        guard let handler = Self.handler else {
            client?.urlProtocol(self, didFailWithError: URLError(.badServerResponse))
            return
        }
        do {
            let response = try handler(request)
            let httpResponse = HTTPURLResponse(
                url: request.url!,
                statusCode: response.statusCode,
                httpVersion: "HTTP/1.1",
                headerFields: response.headers
            )!
            client?.urlProtocol(
                self,
                didReceive: httpResponse,
                cacheStoragePolicy: .notAllowed
            )
            client?.urlProtocol(self, didLoad: response.data)
            client?.urlProtocolDidFinishLoading(self)
        } catch {
            client?.urlProtocol(self, didFailWithError: error)
        }
    }

    override func stopLoading() {}
}
