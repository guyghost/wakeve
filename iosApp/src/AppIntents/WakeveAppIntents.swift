import AppIntents
import Foundation

struct CreateEventIntent: AppIntent {
    static let title: LocalizedStringResource = "Create Event"
    static let description = IntentDescription("Create a Wakeve event without opening the app.")
    static let openAppWhenRun = false

    @Parameter(title: "Title")
    var title: String

    @Parameter(title: "Date")
    var date: Date

    @Parameter(title: "Location")
    var location: String?

    @Parameter(title: "Group")
    var group: GroupEntity?

    @Parameter(title: "Notes")
    var notes: String?

    func perform() async throws -> some IntentResult & ReturnsValue<EventEntity> & ProvidesDialog {
        let record = try await WakeveIntentStore.shared.createEvent(
            title: title,
            date: date,
            location: location,
            groupId: group?.id,
            notes: notes
        )
        let entity = WakeveIntentEntityMapping.event(record)
        return .result(value: entity, dialog: "Created \(entity.title).")
    }
}

struct UpdateEventIntent: AppIntent {
    static let title: LocalizedStringResource = "Update Event"
    static let description = IntentDescription("Update a Wakeve event title, date, location, or notes.")
    static let openAppWhenRun = false

    @Parameter(title: "Event")
    var event: EventEntity

    @Parameter(title: "Title")
    var title: String?

    @Parameter(title: "Date")
    var date: Date?

    @Parameter(title: "Location")
    var location: String?

    @Parameter(title: "Notes")
    var notes: String?

    func perform() async throws -> some IntentResult & ReturnsValue<EventEntity> & ProvidesDialog {
        let record = try await WakeveIntentStore.shared.updateEvent(
            id: event.id,
            title: title,
            date: date,
            location: location,
            notes: notes
        )
        let entity = WakeveIntentEntityMapping.event(record)
        return .result(value: entity, dialog: "Updated \(entity.title).")
    }
}

struct InviteParticipantsIntent: AppIntent {
    static let title: LocalizedStringResource = "Invite Participants"
    static let description = IntentDescription("Invite participants to a Wakeve event.")
    static let openAppWhenRun = false

    @Parameter(title: "Event")
    var event: EventEntity

    @Parameter(title: "Participants")
    var participants: [ParticipantEntity]

    func perform() async throws -> some IntentResult & ReturnsValue<EventEntity> & ProvidesDialog {
        let record = try await WakeveIntentStore.shared.invite(
            participantIds: participants.map(\.id),
            to: event.id
        )
        let entity = WakeveIntentEntityMapping.event(record)
        return .result(value: entity, dialog: "Invited \(participants.count) participants.")
    }
}

struct CreatePollIntent: AppIntent {
    static let title: LocalizedStringResource = "Create Poll"
    static let description = IntentDescription("Create a Wakeve poll for an event.")
    static let openAppWhenRun = false

    @Parameter(title: "Event")
    var event: EventEntity

    @Parameter(title: "Question")
    var question: String

    @Parameter(title: "Options")
    var options: [String]

    func perform() async throws -> some IntentResult & ReturnsValue<PollEntity> & ProvidesDialog {
        let record = try await WakeveIntentStore.shared.createPoll(
            eventId: event.id,
            question: question,
            options: options
        )
        let entity = WakeveIntentEntityMapping.poll(record)
        return .result(value: entity, dialog: "Created poll \(entity.question).")
    }
}

struct VoteIntent: AppIntent {
    static let title: LocalizedStringResource = "Vote"
    static let description = IntentDescription("Vote on a Wakeve poll option.")
    static let openAppWhenRun = false

    @Parameter(title: "Poll")
    var poll: PollEntity

    @Parameter(title: "Option")
    var option: String

    func perform() async throws -> some IntentResult & ReturnsValue<PollEntity> & ProvidesDialog {
        let record = try await WakeveIntentStore.shared.vote(pollId: poll.id, option: option)
        let entity = WakeveIntentEntityMapping.poll(record)
        return .result(value: entity, dialog: "Recorded vote for \(option).")
    }
}

struct ProposeTransportIntent: AppIntent {
    static let title: LocalizedStringResource = "Propose Transport"
    static let description = IntentDescription("Propose a transport option for a Wakeve event.")
    static let openAppWhenRun = false

    @Parameter(title: "Event")
    var event: EventEntity

    @Parameter(title: "Departure")
    var departure: String

    @Parameter(title: "Seats")
    var seats: Int

    @Parameter(title: "Time")
    var time: Date

    func perform() async throws -> some IntentResult & ReturnsValue<TransportEntity> & ProvidesDialog {
        let record = try await WakeveIntentStore.shared.proposeTransport(
            eventId: event.id,
            departure: departure,
            seats: seats,
            time: time
        )
        let entity = WakeveIntentEntityMapping.transport(record)
        return .result(value: entity, dialog: "Proposed transport from \(departure).")
    }
}

struct OpenEventIntent: AppIntent {
    static let title: LocalizedStringResource = "Open Event"
    static let description = IntentDescription("Open Wakeve on a specific event.")
    static let openAppWhenRun = true

    @Parameter(title: "Event")
    var event: EventEntity

    func perform() async throws -> some IntentResult & ProvidesDialog {
        let records = await WakeveIntentStore.shared.events(for: [event.id])
        guard !records.isEmpty else {
            throw WakeveIntentStoreError.notFound("Event not found or unavailable")
        }

        await WakeveIntentStore.shared.setOpenEvent(event.id)
        return .result(dialog: "Opening \(event.title).")
    }
}

struct SummarizeEventIntent: AppIntent {
    static let title: LocalizedStringResource = "Summarize Event"
    static let description = IntentDescription("Summarize the current state of a Wakeve event.")
    static let openAppWhenRun = false

    @Parameter(title: "Event")
    var event: EventEntity

    func perform() async throws -> some IntentResult & ReturnsValue<String> & ProvidesDialog {
        let polls = await WakeveIntentStore.shared.polls(matching: event.id)
        let transports = await WakeveIntentStore.shared.transports(matching: event.id)
        let pollText = polls.first.map { "Poll: \($0.question) (\($0.status))" } ?? "No active poll"
        let transportText = transports.first.map { "Transport: \($0.departure), \($0.seats) seats" } ?? "Transport not ready"
        let summary = "\(event.title) is \(event.status) in \(event.location) with \(event.participantsCount) participants. \(pollText). \(transportText)."
        return .result(value: summary, dialog: IntentDialog(stringLiteral: summary))
    }
}

struct ViewUpcomingEventsIntent: AppIntent {
    static let title: LocalizedStringResource = "View Upcoming Events"
    static let description = IntentDescription("Show a concise list of upcoming Wakeve events.")
    static let openAppWhenRun = false

    func perform() async throws -> some IntentResult & ReturnsValue<String> & ProvidesDialog {
        let events = await WakeveIntentStore.shared.allEvents()
            .sorted { $0.date < $1.date }
            .prefix(3)

        let summary = events.isEmpty
            ? "No upcoming Wakeve events."
            : events.map { "\($0.title) in \($0.location)" }.joined(separator: ". ")

        return .result(value: summary, dialog: IntentDialog(stringLiteral: summary))
    }
}

struct ResetWakeveIntentFixturesIntent: AppIntent {
    static let title: LocalizedStringResource = "Reset Wakeve Intent Fixtures"
    static let description = IntentDescription("Reset deterministic Wakeve App Intents fixtures for automated tests.")
    static let openAppWhenRun = false

    func perform() async throws -> some IntentResult & ProvidesDialog {
        await WakeveIntentStore.shared.resetToFixtures()
        return .result(dialog: "Wakeve App Intents fixtures reset.")
    }
}

#if DEBUG
struct SeedWakeveTestDataIntent: AppIntent {
    static let title: LocalizedStringResource = "Seed Wakeve Test Data"
    static let description = IntentDescription("Seed deterministic Wakeve App Intents fixtures for automated tests.")
    static let openAppWhenRun = false
    static let isDiscoverable = false

    func perform() async throws -> some IntentResult & ProvidesDialog {
        await WakeveIntentStore.shared.resetToFixtures()
        return .result(dialog: "Wakeve App Intents test data seeded.")
    }
}

struct ClearWakeveTestDataIntent: AppIntent {
    static let title: LocalizedStringResource = "Clear Wakeve Test Data"
    static let description = IntentDescription("Clear deterministic Wakeve App Intents fixtures for automated tests.")
    static let openAppWhenRun = false
    static let isDiscoverable = false

    func perform() async throws -> some IntentResult & ProvidesDialog {
        await WakeveIntentStore.shared.clearTestData()
        return .result(dialog: "Wakeve App Intents test data cleared.")
    }
}

struct OpenWakeveScreenForTestIntent: AppIntent {
    static let title: LocalizedStringResource = "Open Wakeve Screen For Test"
    static let description = IntentDescription("Open a Wakeve system-surface test screen for automated view annotation checks.")
    static let openAppWhenRun = true
    static let isDiscoverable = false

    @Parameter(title: "Screen")
    var screen: String?

    @Parameter(title: "Event")
    var event: EventEntity?

    @Parameter(title: "Poll")
    var poll: PollEntity?

    @Parameter(title: "Group")
    var group: GroupEntity?

    @Parameter(title: "Transport")
    var transport: TransportEntity?

    func perform() async throws -> some IntentResult & ProvidesDialog {
        let requestedScreen = screen?.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()

        switch requestedScreen {
        case "poll":
            guard let poll else {
                throw WakeveIntentStoreError.invalidInput("Poll is required")
            }
            await WakeveIntentStore.shared.setOpenTestScreen(kind: "poll", entityId: poll.id)
            return .result(dialog: "Opening \(poll.question) for App Intents testing.")
        case "group":
            guard let group else {
                throw WakeveIntentStoreError.invalidInput("Group is required")
            }
            await WakeveIntentStore.shared.setOpenTestScreen(kind: "group", entityId: group.id)
            return .result(dialog: "Opening \(group.name) for App Intents testing.")
        case "transport":
            guard let transport else {
                throw WakeveIntentStoreError.invalidInput("Transport is required")
            }
            await WakeveIntentStore.shared.setOpenTestScreen(kind: "transport", entityId: transport.id)
            return .result(dialog: "Opening \(transport.departure) for App Intents testing.")
        default:
            guard let event else {
                throw WakeveIntentStoreError.invalidInput("Event is required")
            }
            await WakeveIntentStore.shared.setOpenTestScreen(kind: "event", entityId: event.id)
            return .result(dialog: "Opening \(event.title) for App Intents testing.")
        }
    }
}

struct DeleteEventForTestIntent: AppIntent {
    static let title: LocalizedStringResource = "Delete Event For Test"
    static let description = IntentDescription("Delete a Wakeve event fixture for automated App Intents tests.")
    static let openAppWhenRun = false
    static let isDiscoverable = false

    @Parameter(title: "Event")
    var event: EventEntity

    func perform() async throws -> some IntentResult & ProvidesDialog {
        try await WakeveIntentStore.shared.deleteEvent(id: event.id)
        return .result(dialog: "Deleted \(event.title) for App Intents testing.")
    }
}

struct FinalizeEventForTestIntent: AppIntent {
    static let title: LocalizedStringResource = "Finalize Event For Test"
    static let description = IntentDescription("Finalize a Wakeve event fixture for automated App Intents tests.")
    static let openAppWhenRun = false
    static let isDiscoverable = false

    @Parameter(title: "Event")
    var event: EventEntity

    func perform() async throws -> some IntentResult & ReturnsValue<EventEntity> & ProvidesDialog {
        let record = try await WakeveIntentStore.shared.markEventFinalized(id: event.id)
        let entity = WakeveIntentEntityMapping.event(record)
        return .result(value: entity, dialog: "Finalized \(entity.title) for App Intents testing.")
    }
}

struct ClosePollForTestIntent: AppIntent {
    static let title: LocalizedStringResource = "Close Poll For Test"
    static let description = IntentDescription("Close a Wakeve poll fixture for automated App Intents tests.")
    static let openAppWhenRun = false
    static let isDiscoverable = false

    @Parameter(title: "Poll")
    var poll: PollEntity

    func perform() async throws -> some IntentResult & ReturnsValue<PollEntity> & ProvidesDialog {
        let record = try await WakeveIntentStore.shared.closePoll(id: poll.id)
        let entity = WakeveIntentEntityMapping.poll(record)
        return .result(value: entity, dialog: "Closed \(entity.question) for App Intents testing.")
    }
}
#endif

struct WakeveAppShortcuts: AppShortcutsProvider {
    static var shortcutTileColor: ShortcutTileColor = .blue

    static var appShortcuts: [AppShortcut] {
        AppShortcut(
            intent: CreateEventIntent(),
            phrases: [
                "Create event with \(.applicationName)",
                "Plan with \(.applicationName)"
            ],
            shortTitle: "Create Event",
            systemImageName: "calendar.badge.plus"
        )

        AppShortcut(
            intent: OpenEventIntent(),
            phrases: [
                "Open event in \(.applicationName)",
                "Show my event in \(.applicationName)"
            ],
            shortTitle: "Open Event",
            systemImageName: "calendar"
        )

        AppShortcut(
            intent: SummarizeEventIntent(),
            phrases: [
                "Summarize event in \(.applicationName)",
                "What is happening in \(.applicationName)"
            ],
            shortTitle: "Summarize Event",
            systemImageName: "text.bubble"
        )

        AppShortcut(
            intent: CreatePollIntent(),
            phrases: [
                "Create a poll in \(.applicationName)",
                "Ask a group question with \(.applicationName)"
            ],
            shortTitle: "Create Poll",
            systemImageName: "chart.bar.doc.horizontal"
        )

        AppShortcut(
            intent: InviteParticipantsIntent(),
            phrases: [
                "Invite participants with \(.applicationName)",
                "Add guests in \(.applicationName)"
            ],
            shortTitle: "Invite",
            systemImageName: "person.badge.plus"
        )

        AppShortcut(
            intent: VoteIntent(),
            phrases: [
                "Vote in \(.applicationName)",
                "Choose a poll option in \(.applicationName)"
            ],
            shortTitle: "Vote",
            systemImageName: "checkmark.circle"
        )

        AppShortcut(
            intent: ProposeTransportIntent(),
            phrases: [
                "Propose transport with \(.applicationName)",
                "Add a ride in \(.applicationName)"
            ],
            shortTitle: "Propose Transport",
            systemImageName: "car"
        )

        AppShortcut(
            intent: ViewUpcomingEventsIntent(),
            phrases: [
                "Show upcoming events in \(.applicationName)",
                "What events are coming up in \(.applicationName)"
            ],
            shortTitle: "Upcoming",
            systemImageName: "calendar.badge.clock"
        )
    }
}
