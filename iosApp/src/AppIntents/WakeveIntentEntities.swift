import AppIntents
import CoreSpotlight
import Foundation
import UniformTypeIdentifiers

struct EventEntity: AppEntity, IndexedEntity, Identifiable {
    let id: String
    let title: String
    let date: Date
    let location: String
    let status: String
    let participantsCount: Int

    static let typeDisplayRepresentation: TypeDisplayRepresentation = "Event"
    static let defaultQuery = EventEntityQuery()

    var displayRepresentation: DisplayRepresentation {
        DisplayRepresentation(
            title: "\(title)",
            subtitle: "\(location) - \(status)"
        )
    }

    var attributeSet: CSSearchableItemAttributeSet {
        let attributes = CSSearchableItemAttributeSet(contentType: .text)
        attributes.title = title
        attributes.contentDescription = "\(location) \(status) \(participantsCount) participants"
        return attributes
    }
}

struct GroupEntity: AppEntity, Identifiable {
    let id: String
    let name: String
    let membersCount: Int

    static let typeDisplayRepresentation: TypeDisplayRepresentation = "Group"
    static let defaultQuery = GroupEntityQuery()

    var displayRepresentation: DisplayRepresentation {
        DisplayRepresentation(
            title: "\(name)",
            subtitle: "\(membersCount) members"
        )
    }
}

struct ParticipantEntity: AppEntity, Identifiable {
    let id: String
    let displayName: String
    let status: String
    let groupId: String?

    static let typeDisplayRepresentation: TypeDisplayRepresentation = "Participant"
    static let defaultQuery = ParticipantEntityQuery()

    var displayRepresentation: DisplayRepresentation {
        DisplayRepresentation(
            title: "\(displayName)",
            subtitle: "\(status)"
        )
    }
}

struct PollEntity: AppEntity, Identifiable {
    let id: String
    let eventId: String
    let question: String
    let options: [String]
    let status: String

    static let typeDisplayRepresentation: TypeDisplayRepresentation = "Poll"
    static let defaultQuery = PollEntityQuery()

    var displayRepresentation: DisplayRepresentation {
        DisplayRepresentation(
            title: "\(question)",
            subtitle: "\(status) - \(options.count) options"
        )
    }
}

struct TransportEntity: AppEntity, Identifiable {
    let id: String
    let eventId: String
    let driver: String
    let departure: String
    let seats: Int
    let time: Date

    static let typeDisplayRepresentation: TypeDisplayRepresentation = "Transport"
    static let defaultQuery = TransportEntityQuery()

    var displayRepresentation: DisplayRepresentation {
        DisplayRepresentation(
            title: "\(departure)",
            subtitle: "\(driver) - \(seats) seats"
        )
    }
}

enum WakeveIntentEntityMapping {
    static func event(_ record: WakeveIntentEventRecord) -> EventEntity {
        EventEntity(
            id: record.id,
            title: record.title,
            date: record.date,
            location: record.location,
            status: record.status,
            participantsCount: record.participantsCount
        )
    }

    static func group(_ record: WakeveIntentGroupRecord) -> GroupEntity {
        GroupEntity(id: record.id, name: record.name, membersCount: record.membersCount)
    }

    static func participant(_ record: WakeveIntentParticipantRecord) -> ParticipantEntity {
        ParticipantEntity(
            id: record.id,
            displayName: record.displayName,
            status: record.status,
            groupId: record.groupId
        )
    }

    static func poll(_ record: WakeveIntentPollRecord) -> PollEntity {
        PollEntity(
            id: record.id,
            eventId: record.eventId,
            question: record.question,
            options: record.options,
            status: record.status
        )
    }

    static func transport(_ record: WakeveIntentTransportRecord) -> TransportEntity {
        TransportEntity(
            id: record.id,
            eventId: record.eventId,
            driver: record.driver,
            departure: record.departure,
            seats: record.seats,
            time: record.time
        )
    }
}

struct EventEntityQuery: EntityStringQuery, EnumerableEntityQuery {
    init() {}

    func entities(for identifiers: [EventEntity.ID]) async throws -> [EventEntity] {
        await WakeveIntentStore.shared.events(for: identifiers).map(WakeveIntentEntityMapping.event)
    }

    func entities(matching string: String) async throws -> [EventEntity] {
        await WakeveIntentStore.shared.events(matching: string).map(WakeveIntentEntityMapping.event)
    }

    func suggestedEntities() async throws -> [EventEntity] {
        await WakeveIntentStore.shared.allEvents().map(WakeveIntentEntityMapping.event)
    }

    func allEntities() async throws -> [EventEntity] {
        try await suggestedEntities()
    }
}

struct GroupEntityQuery: EntityStringQuery, EnumerableEntityQuery {
    init() {}

    func entities(for identifiers: [GroupEntity.ID]) async throws -> [GroupEntity] {
        await WakeveIntentStore.shared.groups(for: identifiers).map(WakeveIntentEntityMapping.group)
    }

    func entities(matching string: String) async throws -> [GroupEntity] {
        await WakeveIntentStore.shared.groups(matching: string).map(WakeveIntentEntityMapping.group)
    }

    func suggestedEntities() async throws -> [GroupEntity] {
        await WakeveIntentStore.shared.allGroups().map(WakeveIntentEntityMapping.group)
    }

    func allEntities() async throws -> [GroupEntity] {
        try await suggestedEntities()
    }
}

struct ParticipantEntityQuery: EntityStringQuery, EnumerableEntityQuery {
    init() {}

    func entities(for identifiers: [ParticipantEntity.ID]) async throws -> [ParticipantEntity] {
        await WakeveIntentStore.shared.participants(for: identifiers).map(WakeveIntentEntityMapping.participant)
    }

    func entities(matching string: String) async throws -> [ParticipantEntity] {
        await WakeveIntentStore.shared.participants(matching: string).map(WakeveIntentEntityMapping.participant)
    }

    func suggestedEntities() async throws -> [ParticipantEntity] {
        await WakeveIntentStore.shared.allParticipants().map(WakeveIntentEntityMapping.participant)
    }

    func allEntities() async throws -> [ParticipantEntity] {
        try await suggestedEntities()
    }
}

struct PollEntityQuery: EntityStringQuery, EnumerableEntityQuery {
    init() {}

    func entities(for identifiers: [PollEntity.ID]) async throws -> [PollEntity] {
        await WakeveIntentStore.shared.polls(for: identifiers).map(WakeveIntentEntityMapping.poll)
    }

    func entities(matching string: String) async throws -> [PollEntity] {
        await WakeveIntentStore.shared.polls(matching: string).map(WakeveIntentEntityMapping.poll)
    }

    func suggestedEntities() async throws -> [PollEntity] {
        await WakeveIntentStore.shared.allPolls().map(WakeveIntentEntityMapping.poll)
    }

    func allEntities() async throws -> [PollEntity] {
        try await suggestedEntities()
    }
}

struct TransportEntityQuery: EntityStringQuery, EnumerableEntityQuery {
    init() {}

    func entities(for identifiers: [TransportEntity.ID]) async throws -> [TransportEntity] {
        await WakeveIntentStore.shared.transports(for: identifiers).map(WakeveIntentEntityMapping.transport)
    }

    func entities(matching string: String) async throws -> [TransportEntity] {
        await WakeveIntentStore.shared.transports(matching: string).map(WakeveIntentEntityMapping.transport)
    }

    func suggestedEntities() async throws -> [TransportEntity] {
        await WakeveIntentStore.shared.allTransports().map(WakeveIntentEntityMapping.transport)
    }

    func allEntities() async throws -> [TransportEntity] {
        try await suggestedEntities()
    }
}
