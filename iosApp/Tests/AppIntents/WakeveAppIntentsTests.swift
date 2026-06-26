import AppIntentsTesting
import XCTest

@available(iOS 27.0, *)
final class WakeveAppIntentsTests: XCTestCase {
    private var support: WakeveIntentTestSupport!

    override func setUp() async throws {
        try await super.setUp()
        support = WakeveIntentTestSupport()
        do {
            try await support.resetFixtures()
        } catch {
            if WakeveIntentTestSupport.isCustomerBuildSecurityError(error) {
                if WakeveIntentTestSupport.requiresRuntimeExecution {
                    throw error
                }
                throw XCTSkip("AppIntentsTesting refused execution on this Xcode/iOS runtime customer build.")
            }
            throw error
        }
    }

    override func tearDown() async throws {
        support = nil
        try await super.tearDown()
    }

    func testIntentAndEntityDefinitionsAreDiscoverable() async throws {
        for intentIdentifier in [
            "CreateEventIntent",
            "UpdateEventIntent",
            "InviteParticipantsIntent",
            "CreatePollIntent",
            "VoteIntent",
            "ProposeTransportIntent",
            "OpenEventIntent",
            "SummarizeEventIntent",
            "ViewUpcomingEventsIntent",
            "SeedWakeveTestDataIntent",
            "ClearWakeveTestDataIntent",
            "OpenWakeveScreenForTestIntent",
            "DeleteEventForTestIntent",
            "FinalizeEventForTestIntent",
            "ClosePollForTestIntent"
        ] {
            XCTAssertEqual(support.definitions.intents[intentIdentifier].identifier, intentIdentifier)
        }

        _ = support.events
        _ = support.groups
        _ = support.participants
        _ = support.polls
        _ = support.transports
    }

    func testSystemSurfaceMetadataDefinitionsCanConstructSupportedIntents() async throws {
        let event = support.event("event-week-end-famille")
        let group = support.group("group-famille")
        let participant = support.participant("participant-bob")
        let poll = support.poll("poll-week-end-famille")

        _ = support.definitions.intents["CreateEventIntent"].makeIntent(
            title: "Metadata Smoke",
            date: Date(timeIntervalSince1970: 1_820_000_000),
            location: nil as String?,
            group: group,
            notes: nil as String?
        )
        _ = support.definitions.intents["UpdateEventIntent"].makeIntent(
            event: event,
            title: nil as String?,
            date: nil as Date?,
            location: nil as String?,
            notes: "Updated from metadata smoke"
        )
        _ = support.definitions.intents["InviteParticipantsIntent"].makeIntent(
            event: event,
            participants: [participant]
        )
        _ = support.definitions.intents["CreatePollIntent"].makeIntent(
            event: event,
            question: "Metadata question?",
            options: ["Yes", "No"]
        )
        _ = support.definitions.intents["VoteIntent"].makeIntent(
            poll: poll,
            option: "Samedi"
        )
        _ = support.definitions.intents["ProposeTransportIntent"].makeIntent(
            event: event,
            departure: "Paris",
            seats: 2,
            time: Date(timeIntervalSince1970: 1_820_003_600)
        )
        _ = support.definitions.intents["OpenEventIntent"].makeIntent(event: event)
        _ = support.definitions.intents["SummarizeEventIntent"].makeIntent(event: event)
        _ = support.definitions.intents["ViewUpcomingEventsIntent"].makeIntent()

        let eventSuggestions = try await support.events.suggestedEntities()
        XCTAssertGreaterThanOrEqual(eventSuggestions.count, 2)
    }

    func testCreateUpdateInvitePollVoteTransportOpenAndSummarizeIntents() async throws {
        try await support.createEvent(title: "Diner Marseille", location: "Marseille")

        let created = try await support.events.entities(matching: "diner marseille")
        XCTAssertEqual(created.count, 1)

        let event = try XCTUnwrap(created.first)
        XCTAssertEqual(try event.title, "Diner Marseille")
        XCTAssertEqual(try event.location, "Marseille")

        try await support.updateEvent(event, title: "Diner Marseille Vieux-Port", location: "Vieux-Port")
        let updated = try await support.events.entities(matching: "vieux-port")
        XCTAssertEqual(updated.count, 1)
        XCTAssertEqual(try updated.first?.title, "Diner Marseille Vieux-Port")

        let updatedEvent = try XCTUnwrap(updated.first)
        try await support.invite(["participant-emma", "participant-alice"], to: updatedEvent)
        let invited = try await support.events.entities(matching: "vieux-port")
        XCTAssertEqual(try invited.first?.participantsCount, 2)

        try await support.createPoll(
            event: updatedEvent,
            question: "Quel menu ?",
            options: ["Tapas", "Pizza"]
        )
        let polls = try await support.polls.entities(matching: "menu")
        XCTAssertEqual(polls.count, 1)

        let poll = try XCTUnwrap(polls.first)
        try await support.vote(poll: poll, option: "Tapas")
        let votedPolls = try await support.polls.entities(matching: "menu")
        XCTAssertEqual(try votedPolls.first?.status, "ACTIVE")

        try await support.proposeTransport(event: updatedEvent, departure: "Aix-en-Provence", seats: 4)
        let transports = try await support.transports.entities(matching: "aix")
        XCTAssertEqual(transports.count, 1)
        XCTAssertEqual(try transports.first?.seats, 4)

        try await support.openEvent(updatedEvent)
        try await support.summarize(updatedEvent)
    }

    func testIntentChainingWorkflowsUseReturnedEntitiesAsInputs() async throws {
        try await support.createEvent(title: "Week-end Lisbonne", location: "Lisbonne")
        let lisbonEvents = try await support.events.entities(matching: "lisbonne")
        let event = try XCTUnwrap(lisbonEvents.first)

        try await support.createPoll(
            event: event,
            question: "Quel quartier ?",
            options: ["Alfama", "Belem"]
        )
        let polls = try await support.polls.entities(matching: "quartier")
        let poll = try XCTUnwrap(polls.first)
        try await support.vote(poll: poll, option: "Alfama")

        try await support.invite(["participant-emma"], to: event)
        let updatedEvents = try await support.events.entities(matching: "lisbonne")
        let updatedEvent = try XCTUnwrap(updatedEvents.first)
        try await support.proposeTransport(event: updatedEvent, departure: "Aeroport", seats: 3)
        try await support.summarize(updatedEvent)

        try await support.updateEvent(updatedEvent, title: "Week-end Lisbonne Final", location: "Lisbonne")
        let renamedEvents = try await support.events.entities(matching: "final")
        let renamed = try XCTUnwrap(renamedEvents.first)
        try await support.openEvent(renamed)
    }

    func testEventEntityQueriesSupportIdentifierSearchSuggestionsAccentsPartialAndCaseInsensitiveTerms() async throws {
        let byIdentifier = try await support.events.entities(identifiers: ["event-anniversaire-emma"])
        XCTAssertEqual(byIdentifier.count, 1)
        XCTAssertEqual(try byIdentifier.first?.title, "Anniversaire Emma")

        let accentInsensitive = try await support.events.entities(matching: "anniversaire emma")
        XCTAssertEqual(accentInsensitive.count, 1)

        let partial = try await support.events.entities(matching: "week-end")
        XCTAssertEqual(partial.count, 1)
        XCTAssertEqual(try partial.first?.title, "Week-end Famille")

        let caseInsensitive = try await support.events.entities(matching: "FAMILLE")
        XCTAssertEqual(caseInsensitive.count, 1)

        let noResult = try await support.events.entities(matching: "transport")
        XCTAssertTrue(noResult.isEmpty)

        let suggestions = try await support.events.suggestedEntities()
        XCTAssertGreaterThanOrEqual(suggestions.count, 2)
    }

    func testGroupParticipantPollAndTransportQueries() async throws {
        let familyGroups = try await support.groups.entities(matching: "famille")
        XCTAssertEqual(familyGroups.count, 1)
        XCTAssertEqual(try familyGroups.first?.name, "Famille")

        let groupSuggestions = try await support.groups.suggestedEntities()
        XCTAssertGreaterThanOrEqual(groupSuggestions.count, 1)

        let participants = try await support.participants.entities(matching: "emma")
        XCTAssertEqual(participants.count, 1)
        XCTAssertEqual(try participants.first?.displayName, "Emma Martin")

        let scopedParticipants = try await support.participants.entities(matching: "group-famille")
        XCTAssertGreaterThanOrEqual(scopedParticipants.count, 2)

        let activePolls = try await support.polls.entities(matching: "event-week-end-famille")
        XCTAssertEqual(activePolls.count, 1)

        let pollSuggestions = try await support.polls.suggestedEntities()
        XCTAssertGreaterThanOrEqual(pollSuggestions.count, 1)

        let transports = try await support.transports.entities(matching: "event-week-end-famille")
        XCTAssertEqual(transports.count, 1)

        let transportSuggestions = try await support.transports.suggestedEntities()
        XCTAssertGreaterThanOrEqual(transportSuggestions.count, 1)
    }

    func testSpotlightQueryAndViewAnnotationsDoNotRegress() async throws {
        try await support.createEvent(title: "Week-end Lisbonne", location: "Lisbonne")
        let spotlightMatches = try await support.events.spotlightQuery("week-end")
        XCTAssertGreaterThanOrEqual(spotlightMatches.count, 1)

        let lisbonMatches = try await support.events.spotlightQuery("Lisbonne")
        XCTAssertEqual(lisbonMatches.count, 1)
        XCTAssertEqual(try lisbonMatches.first?.title, "Week-end Lisbonne")
        XCTAssertEqual(try lisbonMatches.first?.location, "Lisbonne")

        let spotlightMisses = try await support.events.spotlightQuery("transport")
        XCTAssertTrue(spotlightMisses.isEmpty)

        let event = try XCTUnwrap(lisbonMatches.first)
        try await support.updateEvent(event, title: "Week-end Porto", location: "Porto")
        let renamedMatches = try await support.events.spotlightQuery("Porto")
        XCTAssertEqual(renamedMatches.count, 1)
        XCTAssertEqual(try renamedMatches.first?.title, "Week-end Porto")
        let oldLisbonMatches = try await support.events.spotlightQuery("Lisbonne")
        XCTAssertTrue(oldLisbonMatches.isEmpty)

        let renamedEvent = try XCTUnwrap(renamedMatches.first)
        try await support.deleteEvent(renamedEvent)
        let deletedMatches = try await support.events.spotlightQuery("Porto")
        XCTAssertTrue(deletedMatches.isEmpty)
    }

    func testViewAnnotationsExposeVisibleEventPollGroupAndTransportEntities() async throws {
        let event = support.event("event-week-end-famille")
        let poll = support.poll("poll-week-end-famille")
        let group = support.group("group-famille")
        let transport = support.transports.makeReference(identifier: "transport-week-end-famille")

        try await support.openEventForViewAnnotationTest(event)
        let eventAnnotations = try await support.events.viewAnnotations()
        XCTAssertTrue(
            eventAnnotations.contains { $0.entity == event },
            "Expected EventDetailView to annotate the visible EventEntity"
        )

        try await support.openPollForViewAnnotationTest(poll)
        let pollAnnotations = try await support.polls.viewAnnotations()
        XCTAssertTrue(
            pollAnnotations.contains { $0.entity == poll },
            "Expected PollDetailView to annotate the visible PollEntity"
        )

        try await support.openGroupForViewAnnotationTest(group)
        let groupAnnotations = try await support.groups.viewAnnotations()
        XCTAssertTrue(
            groupAnnotations.contains { $0.entity == group },
            "Expected GroupDetailView to annotate the visible GroupEntity"
        )

        try await support.openTransportForViewAnnotationTest(transport)
        let transportAnnotations = try await support.transports.viewAnnotations()
        XCTAssertTrue(
            transportAnnotations.contains { $0.entity == transport },
            "Expected TransportDetailView to annotate the visible TransportEntity"
        )
    }

    func testInvalidIntentInputsReturnStableErrors() async throws {
        let event = support.event("event-week-end-famille")
        let confirmedEvent = support.event("event-anniversaire-emma")
        let poll = support.poll("poll-week-end-famille")
        let invitedParticipant = support.participant("participant-emma")

        try await assertAppIntentThrows("blank event title") {
            let intent = support.definitions.intents["CreateEventIntent"].makeIntent(
                title: "   ",
                date: Date(timeIntervalSince1970: 1_820_000_000),
                location: "Paris",
                group: support.group("group-famille"),
                notes: nil as String?
            )
            try await intent.run()
        }

        try await assertAppIntentThrows("poll without options") {
            let intent = support.definitions.intents["CreatePollIntent"].makeIntent(
                event: event,
                question: "Quel menu ?",
                options: [String]()
            )
            try await intent.run()
        }

        try await support.closePoll(poll)
        try await assertAppIntentThrows("closed poll vote") {
            let intent = support.definitions.intents["VoteIntent"].makeIntent(
                poll: poll,
                option: "Samedi"
            )
            try await intent.run()
        }

        try await assertAppIntentThrows("duplicate invite") {
            let intent = support.definitions.intents["InviteParticipantsIntent"].makeIntent(
                event: event,
                participants: [invitedParticipant]
            )
            try await intent.run()
        }

        try await support.finalizeEvent(event)
        try await assertAppIntentThrows("transport on finalized event") {
            let intent = support.definitions.intents["ProposeTransportIntent"].makeIntent(
                event: event,
                departure: "Paris",
                seats: 2,
                time: Date(timeIntervalSince1970: 1_820_003_600)
            )
            try await intent.run()
        }

        try await support.createEvent(title: "Deleted Event", location: "Nantes")
        let deletedEvents = try await support.events.entities(matching: "deleted event")
        let deletedEvent = try XCTUnwrap(deletedEvents.first)
        try await support.deleteEvent(deletedEvent)
        try await assertAppIntentThrows("update deleted event") {
            let intent = support.definitions.intents["UpdateEventIntent"].makeIntent(
                event: deletedEvent,
                title: "Should not update",
                date: nil as Date?,
                location: nil as String?,
                notes: nil as String?
            )
            try await intent.run()
        }

        try await assertAppIntentThrows("open unavailable event") {
            let intent = support.definitions.intents["OpenEventIntent"].makeIntent(event: deletedEvent)
            try await intent.run()
        }

        try await assertAppIntentThrows("invalid poll option") {
            let intent = support.definitions.intents["VoteIntent"].makeIntent(
                poll: poll,
                option: "Vendredi"
            )
            try await intent.run()
        }

        try await assertAppIntentThrows("invalid transport seats") {
            let intent = support.definitions.intents["ProposeTransportIntent"].makeIntent(
                event: confirmedEvent,
                departure: "Paris",
                seats: 0,
                time: Date(timeIntervalSince1970: 1_820_003_600)
            )
            try await intent.run()
        }
    }

    private func assertAppIntentThrows(
        _ context: String,
        operation: () async throws -> Void
    ) async throws {
        do {
            try await operation()
            XCTFail("Expected \(context) to throw")
        } catch {
            XCTAssertFalse(error.localizedDescription.isEmpty, "Expected \(context) to expose a user-readable error")
        }
    }
}
