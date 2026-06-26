import AppIntentsTesting
import Foundation
import XCTest

@available(iOS 27.0, *)
final class WakeveIntentTestSupport {
    static let bundleIdentifier = "com.guyghost.wakeve"

    let definitions = IntentDefinitions(bundleIdentifier: bundleIdentifier)

    static func isCustomerBuildSecurityError(_ error: Error) -> Bool {
        let nsError = error as NSError
        return nsError.domain == "AppIntentsServicesSecurityErrorDomain" && nsError.code == 803
    }

    static var requiresRuntimeExecution: Bool {
        #if WAKEVE_APP_INTENTS_REQUIRE_RUNTIME_EXECUTION
        true
        #else
        ProcessInfo.processInfo.environment["WAKEVE_APP_INTENTS_REQUIRE_RUNTIME_EXECUTION"] == "1"
        #endif
    }

    var events: AppEntityDefinition {
        definitions.entities["EventEntity"]
    }

    var groups: AppEntityDefinition {
        definitions.entities["GroupEntity"]
    }

    var participants: AppEntityDefinition {
        definitions.entities["ParticipantEntity"]
    }

    var polls: AppEntityDefinition {
        definitions.entities["PollEntity"]
    }

    var transports: AppEntityDefinition {
        definitions.entities["TransportEntity"]
    }

    func resetFixtures() async throws {
        let intent = definitions.intents["SeedWakeveTestDataIntent"].makeIntent()
        try await intent.run()
    }

    func clearFixtures() async throws {
        let intent = definitions.intents["ClearWakeveTestDataIntent"].makeIntent()
        try await intent.run()
    }

    func event(_ id: String) -> AnyAppEntity {
        events.makeReference(identifier: id)
    }

    func group(_ id: String) -> AnyAppEntity {
        groups.makeReference(identifier: id)
    }

    func participant(_ id: String) -> AnyAppEntity {
        participants.makeReference(identifier: id)
    }

    func poll(_ id: String) -> AnyAppEntity {
        polls.makeReference(identifier: id)
    }

    func createEvent(
        title: String,
        date: Date = Date(timeIntervalSince1970: 1_820_000_000),
        location: String = "Marseille",
        notes: String = "Created from AppIntentsTesting"
    ) async throws {
        let intent = definitions.intents["CreateEventIntent"].makeIntent(
            title: title,
            date: date,
            location: location,
            group: group("group-famille"),
            notes: notes
        )
        try await intent.run()
    }

    func updateEvent(
        _ event: AnyAppEntity,
        title: String,
        location: String
    ) async throws {
        let intent = definitions.intents["UpdateEventIntent"].makeIntent(
            event: event,
            title: title,
            location: location
        )
        try await intent.run()
    }

    func invite(_ participantIds: [String], to event: AnyAppEntity) async throws {
        let selectedParticipants = participantIds.map(participant)
        let intent = definitions.intents["InviteParticipantsIntent"].makeIntent(
            event: event,
            participants: selectedParticipants
        )
        try await intent.run()
    }

    func createPoll(event: AnyAppEntity, question: String, options: [String]) async throws {
        let intent = definitions.intents["CreatePollIntent"].makeIntent(
            event: event,
            question: question,
            options: options
        )
        try await intent.run()
    }

    func vote(poll: AnyAppEntity, option: String) async throws {
        let intent = definitions.intents["VoteIntent"].makeIntent(
            poll: poll,
            option: option
        )
        try await intent.run()
    }

    func proposeTransport(
        event: AnyAppEntity,
        departure: String,
        seats: Int,
        time: Date = Date(timeIntervalSince1970: 1_820_003_600)
    ) async throws {
        let intent = definitions.intents["ProposeTransportIntent"].makeIntent(
            event: event,
            departure: departure,
            seats: seats,
            time: time
        )
        try await intent.run()
    }

    func openEvent(_ event: AnyAppEntity) async throws {
        let intent = definitions.intents["OpenEventIntent"].makeIntent(event: event)
        try await intent.run()
    }

    func openEventForViewAnnotationTest(_ event: AnyAppEntity) async throws {
        let intent = definitions.intents["OpenWakeveScreenForTestIntent"].makeIntent(
            screen: "event",
            event: event
        )
        try await intent.run()
    }

    func openPollForViewAnnotationTest(_ poll: AnyAppEntity) async throws {
        let intent = definitions.intents["OpenWakeveScreenForTestIntent"].makeIntent(
            screen: "poll",
            poll: poll
        )
        try await intent.run()
    }

    func openGroupForViewAnnotationTest(_ group: AnyAppEntity) async throws {
        let intent = definitions.intents["OpenWakeveScreenForTestIntent"].makeIntent(
            screen: "group",
            group: group
        )
        try await intent.run()
    }

    func openTransportForViewAnnotationTest(_ transport: AnyAppEntity) async throws {
        let intent = definitions.intents["OpenWakeveScreenForTestIntent"].makeIntent(
            screen: "transport",
            transport: transport
        )
        try await intent.run()
    }

    func deleteEvent(_ event: AnyAppEntity) async throws {
        let intent = definitions.intents["DeleteEventForTestIntent"].makeIntent(event: event)
        try await intent.run()
    }

    func finalizeEvent(_ event: AnyAppEntity) async throws {
        let intent = definitions.intents["FinalizeEventForTestIntent"].makeIntent(event: event)
        try await intent.run()
    }

    func closePoll(_ poll: AnyAppEntity) async throws {
        let intent = definitions.intents["ClosePollForTestIntent"].makeIntent(poll: poll)
        try await intent.run()
    }

    func summarize(_ event: AnyAppEntity) async throws {
        let intent = definitions.intents["SummarizeEventIntent"].makeIntent(event: event)
        try await intent.run()
    }
}
