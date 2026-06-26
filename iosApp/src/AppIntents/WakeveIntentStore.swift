import Foundation

struct WakeveIntentEventRecord: Codable, Equatable, Sendable {
    var id: String
    var title: String
    var date: Date
    var location: String
    var status: String
    var participantsCount: Int
    var invitedParticipantIds: [String]
    var groupId: String?
    var notes: String?
}

struct WakeveIntentGroupRecord: Codable, Equatable, Sendable {
    var id: String
    var name: String
    var membersCount: Int
}

struct WakeveIntentParticipantRecord: Codable, Equatable, Sendable {
    var id: String
    var displayName: String
    var status: String
    var groupId: String?
}

struct WakeveIntentPollRecord: Codable, Equatable, Sendable {
    var id: String
    var eventId: String
    var question: String
    var options: [String]
    var status: String
    var votes: [String: Int]
}

struct WakeveIntentTransportRecord: Codable, Equatable, Sendable {
    var id: String
    var eventId: String
    var driver: String
    var departure: String
    var seats: Int
    var time: Date
}

struct WakeveIntentState: Codable, Equatable, Sendable {
    var events: [WakeveIntentEventRecord]
    var groups: [WakeveIntentGroupRecord]
    var participants: [WakeveIntentParticipantRecord]
    var polls: [WakeveIntentPollRecord]
    var transports: [WakeveIntentTransportRecord]
}

struct WakeveIntentTestScreen: Codable, Equatable, Sendable {
    var kind: String
    var entityId: String
}

actor WakeveIntentStore {
    static let shared = WakeveIntentStore()

    private let defaults: UserDefaults
    private let stateKey = "wakeve.appIntents.state.v1"
    private let openEventKey = "wakeve.appIntents.openEventId"
    private let openTestScreenKey = "wakeve.appIntents.openTestScreen"

    init(defaults: UserDefaults = .standard) {
        self.defaults = defaults
    }

    func resetToFixtures() {
        save(Self.fixtureState)
        defaults.removeObject(forKey: openEventKey)
        defaults.removeObject(forKey: openTestScreenKey)
    }

    func clearTestData() {
        save(WakeveIntentState(events: [], groups: [], participants: [], polls: [], transports: []))
        defaults.removeObject(forKey: openEventKey)
        defaults.removeObject(forKey: openTestScreenKey)
    }

    func setOpenEvent(_ eventId: String) {
        defaults.set(eventId, forKey: openEventKey)
    }

    func consumeOpenEventId() -> String? {
        let eventId = defaults.string(forKey: openEventKey)
        defaults.removeObject(forKey: openEventKey)
        return eventId
    }

    func setOpenTestScreen(kind: String, entityId: String) {
        let screen = WakeveIntentTestScreen(kind: kind, entityId: entityId)
        guard let data = try? JSONEncoder.wakeveIntent.encode(screen) else { return }
        defaults.set(data, forKey: openTestScreenKey)
    }

    func consumeOpenTestScreen() -> WakeveIntentTestScreen? {
        guard let data = defaults.data(forKey: openTestScreenKey),
              let screen = try? JSONDecoder.wakeveIntent.decode(WakeveIntentTestScreen.self, from: data) else {
            defaults.removeObject(forKey: openTestScreenKey)
            return nil
        }

        defaults.removeObject(forKey: openTestScreenKey)
        return screen
    }

    func allEvents() -> [WakeveIntentEventRecord] {
        load().events
    }

    func events(for identifiers: [String]) -> [WakeveIntentEventRecord] {
        let wanted = Set(identifiers)
        return load().events.filter { wanted.contains($0.id) }
    }

    func events(matching query: String) -> [WakeveIntentEventRecord] {
        let normalizedQuery = query.normalizedForWakeveIntentSearch
        guard !normalizedQuery.isEmpty else { return allEvents() }

        return load().events.filter { event in
            [
                event.title,
                event.location,
                event.status,
                event.notes ?? ""
            ].contains { $0.normalizedForWakeveIntentSearch.contains(normalizedQuery) }
        }
    }

    func createEvent(
        title: String,
        date: Date,
        location: String?,
        groupId: String?,
        notes: String?
    ) throws -> WakeveIntentEventRecord {
        var state = load()
        let trimmedTitle = title.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmedTitle.isEmpty else {
            throw WakeveIntentStoreError.invalidInput("Event title is required")
        }

        let stableSlug = trimmedTitle.normalizedForWakeveIntentIdentifier
        let baseId = stableSlug.isEmpty ? "event" : "event-\(stableSlug)"
        let id = uniqueId(baseId, existing: Set(state.events.map(\.id)))
        let event = WakeveIntentEventRecord(
            id: id,
            title: trimmedTitle.isEmpty ? "Untitled event" : trimmedTitle,
            date: date,
            location: location?.trimmingCharacters(in: .whitespacesAndNewlines).nilIfEmpty ?? "To decide",
            status: "DRAFT",
            participantsCount: 0,
            invitedParticipantIds: [],
            groupId: groupId,
            notes: notes?.trimmingCharacters(in: .whitespacesAndNewlines).nilIfEmpty
        )
        state.events.append(event)
        save(state)
        return event
    }

    func updateEvent(
        id: String,
        title: String?,
        date: Date?,
        location: String?,
        notes: String?
    ) throws -> WakeveIntentEventRecord {
        var state = load()
        guard let index = state.events.firstIndex(where: { $0.id == id }) else {
            throw WakeveIntentStoreError.notFound("Event not found")
        }

        if let title, !title.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            state.events[index].title = title.trimmingCharacters(in: .whitespacesAndNewlines)
        }
        if let date {
            state.events[index].date = date
        }
        if let location, !location.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            state.events[index].location = location.trimmingCharacters(in: .whitespacesAndNewlines)
        }
        if let notes {
            state.events[index].notes = notes.trimmingCharacters(in: .whitespacesAndNewlines).nilIfEmpty
        }

        save(state)
        return state.events[index]
    }

    func deleteEvent(id: String) throws {
        var state = load()
        guard state.events.contains(where: { $0.id == id }) else {
            throw WakeveIntentStoreError.notFound("Event not found")
        }

        state.events.removeAll { $0.id == id }
        state.polls.removeAll { $0.eventId == id }
        state.transports.removeAll { $0.eventId == id }
        save(state)
    }

    func markEventFinalized(id: String) throws -> WakeveIntentEventRecord {
        var state = load()
        guard let index = state.events.firstIndex(where: { $0.id == id }) else {
            throw WakeveIntentStoreError.notFound("Event not found")
        }

        state.events[index].status = "FINALIZED"
        save(state)
        return state.events[index]
    }

    func invite(participantIds: [String], to eventId: String) throws -> WakeveIntentEventRecord {
        var state = load()
        guard let index = state.events.firstIndex(where: { $0.id == eventId }) else {
            throw WakeveIntentStoreError.notFound("Event not found")
        }

        let knownParticipants = Set(state.participants.map(\.id))
        let validParticipantIds = Array(Set(participantIds).intersection(knownParticipants)).sorted()
        guard !validParticipantIds.isEmpty else {
            throw WakeveIntentStoreError.invalidInput("Select at least one known participant")
        }

        let alreadyInvited = Set(state.events[index].invitedParticipantIds)
        let duplicates = validParticipantIds.filter { alreadyInvited.contains($0) }
        guard duplicates.isEmpty else {
            throw WakeveIntentStoreError.invalidInput("Participant already invited")
        }

        state.events[index].invitedParticipantIds.append(contentsOf: validParticipantIds)
        state.events[index].participantsCount = state.events[index].invitedParticipantIds.count
        save(state)
        return state.events[index]
    }

    func createPoll(eventId: String, question: String, options: [String]) throws -> WakeveIntentPollRecord {
        var state = load()
        guard state.events.contains(where: { $0.id == eventId }) else {
            throw WakeveIntentStoreError.notFound("Event not found")
        }

        let trimmedQuestion = question.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmedQuestion.isEmpty else {
            throw WakeveIntentStoreError.invalidInput("Poll question is required")
        }

        let trimmedOptions = options.map { $0.trimmingCharacters(in: .whitespacesAndNewlines) }.filter { !$0.isEmpty }
        guard !trimmedOptions.isEmpty else {
            throw WakeveIntentStoreError.invalidInput("Poll options are required")
        }

        let id = uniqueId("poll-\(eventId)", existing: Set(state.polls.map(\.id)))
        let poll = WakeveIntentPollRecord(
            id: id,
            eventId: eventId,
            question: trimmedQuestion,
            options: trimmedOptions,
            status: "ACTIVE",
            votes: [:]
        )
        state.polls.append(poll)
        save(state)
        return poll
    }

    func vote(pollId: String, option: String) throws -> WakeveIntentPollRecord {
        var state = load()
        guard let index = state.polls.firstIndex(where: { $0.id == pollId }) else {
            throw WakeveIntentStoreError.notFound("Poll not found")
        }
        guard state.polls[index].status == "ACTIVE" else {
            throw WakeveIntentStoreError.invalidInput("Poll is closed")
        }

        let selected = option.trimmingCharacters(in: .whitespacesAndNewlines)
        guard state.polls[index].options.contains(selected) else {
            throw WakeveIntentStoreError.invalidInput("Poll option not found")
        }

        state.polls[index].votes[selected, default: 0] += 1
        save(state)
        return state.polls[index]
    }

    func closePoll(id: String) throws -> WakeveIntentPollRecord {
        var state = load()
        guard let index = state.polls.firstIndex(where: { $0.id == id }) else {
            throw WakeveIntentStoreError.notFound("Poll not found")
        }

        state.polls[index].status = "CLOSED"
        save(state)
        return state.polls[index]
    }

    func proposeTransport(
        eventId: String,
        departure: String,
        seats: Int,
        time: Date
    ) throws -> WakeveIntentTransportRecord {
        var state = load()
        guard let event = state.events.first(where: { $0.id == eventId }) else {
            throw WakeveIntentStoreError.notFound("Event not found")
        }
        guard event.status != "FINALIZED" else {
            throw WakeveIntentStoreError.invalidInput("Cannot propose transport for a finalized event")
        }
        guard seats > 0 else {
            throw WakeveIntentStoreError.invalidInput("Transport seats must be greater than zero")
        }

        let id = uniqueId("transport-\(eventId)", existing: Set(state.transports.map(\.id)))
        let transport = WakeveIntentTransportRecord(
            id: id,
            eventId: eventId,
            driver: "Current user",
            departure: departure.trimmingCharacters(in: .whitespacesAndNewlines),
            seats: seats,
            time: time
        )
        state.transports.append(transport)
        save(state)
        return transport
    }

    func allGroups() -> [WakeveIntentGroupRecord] {
        load().groups
    }

    func groups(for identifiers: [String]) -> [WakeveIntentGroupRecord] {
        let wanted = Set(identifiers)
        return load().groups.filter { wanted.contains($0.id) }
    }

    func groups(matching query: String) -> [WakeveIntentGroupRecord] {
        filter(load().groups, query: query) { [$0.name] }
    }

    func allParticipants() -> [WakeveIntentParticipantRecord] {
        load().participants
    }

    func participants(for identifiers: [String]) -> [WakeveIntentParticipantRecord] {
        let wanted = Set(identifiers)
        return load().participants.filter { wanted.contains($0.id) }
    }

    func participants(matching query: String) -> [WakeveIntentParticipantRecord] {
        filter(load().participants, query: query) { [$0.displayName, $0.status, $0.groupId ?? ""] }
    }

    func allPolls() -> [WakeveIntentPollRecord] {
        load().polls
    }

    func polls(for identifiers: [String]) -> [WakeveIntentPollRecord] {
        let wanted = Set(identifiers)
        return load().polls.filter { wanted.contains($0.id) }
    }

    func polls(matching query: String) -> [WakeveIntentPollRecord] {
        filter(load().polls, query: query) { [$0.question, $0.status, $0.eventId] + $0.options }
    }

    func allTransports() -> [WakeveIntentTransportRecord] {
        load().transports
    }

    func transports(for identifiers: [String]) -> [WakeveIntentTransportRecord] {
        let wanted = Set(identifiers)
        return load().transports.filter { wanted.contains($0.id) }
    }

    func transports(matching query: String) -> [WakeveIntentTransportRecord] {
        filter(load().transports, query: query) { [$0.eventId, $0.driver, $0.departure, "\($0.seats)"] }
    }

    private func load() -> WakeveIntentState {
        guard let data = defaults.data(forKey: stateKey),
              let state = try? JSONDecoder.wakeveIntent.decode(WakeveIntentState.self, from: data) else {
            let fixtures = Self.fixtureState
            save(fixtures)
            return fixtures
        }
        return state
    }

    private func save(_ state: WakeveIntentState) {
        guard let data = try? JSONEncoder.wakeveIntent.encode(state) else { return }
        defaults.set(data, forKey: stateKey)
    }

    private func uniqueId(_ base: String, existing: Set<String>) -> String {
        if !existing.contains(base) { return base }

        var suffix = 2
        while existing.contains("\(base)-\(suffix)") {
            suffix += 1
        }
        return "\(base)-\(suffix)"
    }

    private func filter<Record>(
        _ records: [Record],
        query: String,
        searchableValues: (Record) -> [String]
    ) -> [Record] {
        let normalizedQuery = query.normalizedForWakeveIntentSearch
        guard !normalizedQuery.isEmpty else { return records }

        return records.filter { record in
            searchableValues(record).contains {
                $0.normalizedForWakeveIntentSearch.contains(normalizedQuery)
            }
        }
    }
}

enum WakeveIntentStoreError: LocalizedError {
    case notFound(String)
    case invalidInput(String)

    var errorDescription: String? {
        switch self {
        case .notFound(let message), .invalidInput(let message):
            return message
        }
    }
}

private extension WakeveIntentStore {
    static let fixtureState = WakeveIntentState(
        events: [
            WakeveIntentEventRecord(
                id: "event-anniversaire-emma",
                title: "Anniversaire Emma",
                date: Date(timeIntervalSince1970: 1_813_161_600),
                location: "Paris",
                status: "CONFIRMED",
                participantsCount: 8,
                invitedParticipantIds: [
                    "participant-emma",
                    "participant-alice"
                ],
                groupId: "group-famille",
                notes: "Prepare cake and invitations"
            ),
            WakeveIntentEventRecord(
                id: "event-week-end-famille",
                title: "Week-end Famille",
                date: Date(timeIntervalSince1970: 1_816_099_200),
                location: "Lyon",
                status: "POLLING",
                participantsCount: 5,
                invitedParticipantIds: [
                    "participant-emma",
                    "participant-alice"
                ],
                groupId: "group-famille",
                notes: "Compare dates and transport"
            )
        ],
        groups: [
            WakeveIntentGroupRecord(id: "group-famille", name: "Famille", membersCount: 5),
            WakeveIntentGroupRecord(id: "group-amis", name: "Amis proches", membersCount: 7)
        ],
        participants: [
            WakeveIntentParticipantRecord(id: "participant-emma", displayName: "Emma Martin", status: "CONFIRMED", groupId: "group-famille"),
            WakeveIntentParticipantRecord(id: "participant-alice", displayName: "Alice Dupont", status: "INVITED", groupId: "group-famille"),
            WakeveIntentParticipantRecord(id: "participant-bob", displayName: "Bob Bernard", status: "MAYBE", groupId: "group-amis")
        ],
        polls: [
            WakeveIntentPollRecord(
                id: "poll-week-end-famille",
                eventId: "event-week-end-famille",
                question: "Quel week-end convient le mieux ?",
                options: ["Samedi", "Dimanche"],
                status: "ACTIVE",
                votes: [:]
            )
        ],
        transports: [
            WakeveIntentTransportRecord(
                id: "transport-week-end-famille",
                eventId: "event-week-end-famille",
                driver: "Emma Martin",
                departure: "Paris",
                seats: 3,
                time: Date(timeIntervalSince1970: 1_816_056_000)
            )
        ]
    )
}

extension JSONEncoder {
    static var wakeveIntent: JSONEncoder {
        let encoder = JSONEncoder()
        encoder.dateEncodingStrategy = .iso8601
        return encoder
    }
}

extension JSONDecoder {
    static var wakeveIntent: JSONDecoder {
        let decoder = JSONDecoder()
        decoder.dateDecodingStrategy = .iso8601
        return decoder
    }
}

private extension String {
    var nilIfEmpty: String? {
        isEmpty ? nil : self
    }

    var normalizedForWakeveIntentSearch: String {
        folding(options: [.diacriticInsensitive, .caseInsensitive], locale: .current)
            .lowercased()
            .trimmingCharacters(in: .whitespacesAndNewlines)
    }

    var normalizedForWakeveIntentIdentifier: String {
        normalizedForWakeveIntentSearch
            .components(separatedBy: CharacterSet.alphanumerics.inverted)
            .filter { !$0.isEmpty }
            .joined(separator: "-")
    }
}
