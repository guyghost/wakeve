import XCTest
@testable import Wakeve

final class WakeveAIGeneratorTests: XCTestCase {
    func testEventDraftGeneratorStreamsExpectedSectionsFromFakeClient() async throws {
        let generator = EventDraftGenerator(
            availabilityProvider: FixedWakeveAIAvailabilityService(availability: .available),
            client: FakeWakeveAIClient(),
            timeoutSeconds: 2
        )

        var sections: [EventDraftSection] = []
        for try await section in generator.generate(request: WakeveAIGenerationRequest(userInput: "Week-end à Marrakech avec 8 amis début juillet")) {
            sections.append(section)
        }

        XCTAssertTrue(sections.contains { if case .title("Week-end Marrakech") = $0 { return true }; return false })
        XCTAssertTrue(sections.contains { if case .description = $0 { return true }; return false })
        XCTAssertTrue(sections.contains { if case .dateOptions = $0 { return true }; return false })
        XCTAssertTrue(sections.contains { if case .checklist = $0 { return true }; return false })
        XCTAssertTrue(sections.contains { if case .suggestedPolls = $0 { return true }; return false })
        XCTAssertTrue(sections.contains { if case .completed = $0 { return true }; return false })
    }

    func testEventDraftGeneratorFallsBackWhenAvailabilityIsUnsupported() async {
        let generator = EventDraftGenerator(
            availabilityProvider: FixedWakeveAIAvailabilityService(availability: .unsupportedDevice),
            client: nil,
            timeoutSeconds: 2
        )

        do {
            for try await _ in generator.generate(request: WakeveAIGenerationRequest(userInput: "Diner simple")) {}
            XCTFail("Expected unavailable error")
        } catch WakeveAIError.unavailable(let availability) {
            XCTAssertEqual(availability, .unsupportedDevice)
        } catch {
            XCTFail("Unexpected error: \(error)")
        }
    }

    func testPromptCatalogUsesSpecializedVersionedPrompts() {
        XCTAssertEqual(WakeveAIPromptCatalog.eventDraft(userInput: "Road trip", localeIdentifier: "fr_FR").id, "event_draft_v1")
        XCTAssertEqual(WakeveAIPromptCatalog.pollSuggestions(context: "Event", localeIdentifier: "fr_FR").id, "poll_suggestions_v1")
        XCTAssertEqual(WakeveAIPromptCatalog.checklist(context: "Event", localeIdentifier: "fr_FR").id, "checklist_v1")
        XCTAssertEqual(WakeveAIPromptCatalog.invitationMessage(context: "Event", localeIdentifier: "fr_FR").id, "invitation_message_v1")
        XCTAssertEqual(WakeveAIPromptCatalog.eventSummary(context: "Event", localeIdentifier: "fr_FR").id, "event_summary_v1")
        XCTAssertEqual(WakeveAIPromptCatalog.transportSuggestions(context: "Event", localeIdentifier: "fr_FR").id, "transport_suggestions_v1")
    }

    func testHeuristicClientHandlesVaguePartialAndMultilingualFixtures() async throws {
        let client = HeuristicWakeveAIClient()
        let fixtures = [
            "beach party with friends next month",
            "anniversaire enfant samedi après-midi",
            "friends weekend à Lyon début juillet",
            "voyage en famille au bord de mer",
            "dîner simple demain soir",
            "road trip entre amis"
        ]

        for fixture in fixtures {
            let draft = try await client.generateEventDraft(
                prompt: WakeveAIPromptCatalog.eventDraft(userInput: fixture, localeIdentifier: "fr_FR"),
                request: WakeveAIGenerationRequest(userInput: fixture, localeIdentifier: "fr_FR")
            )

            XCTAssertFalse(draft.title.isEmpty, "Missing title for \(fixture)")
            XCTAssertFalse(draft.description.isEmpty, "Missing description for \(fixture)")
            XCTAssertLessThanOrEqual(draft.dateOptions.count, 3)
            XCTAssertLessThanOrEqual(draft.suggestedPolls.count, 3)
            XCTAssertLessThanOrEqual(draft.checklist.count, 3)
        }
    }

    func testFakeClientGeneratorsReturnReviewableOutputs() async throws {
        let client = FakeWakeveAIClient()
        let contextProvider = StaticWakeveAIContextProvider()

        let polls = try await PollSuggestionGenerator(client: client).generate(
            context: contextProvider.eventContextValue.promptSummary,
            knownFacts: contextProvider.eventContextValue.knownFacts,
            localeIdentifier: "fr_FR"
        )
        let checklist = try await ChecklistGenerator(client: client).generate(
            context: contextProvider.eventContextValue.promptSummary,
            knownFacts: contextProvider.eventContextValue.knownFacts,
            localeIdentifier: "fr_FR"
        )
        let messages = try await InvitationMessageGenerator(client: client).generate(
            context: contextProvider.eventContextValue.promptSummary,
            knownFacts: contextProvider.eventContextValue.knownFacts,
            localeIdentifier: "fr_FR"
        )
        let summary = try await EventSummaryGenerator(
            client: client,
            contextProvider: contextProvider
        ).generate(eventId: "event-1", localeIdentifier: "fr_FR")
        let transport = try await TransportSuggestionGenerator(
            client: client,
            contextProvider: contextProvider
        ).generate(eventId: "event-1", localeIdentifier: "fr_FR")

        XCTAssertEqual(polls.count, 3)
        XCTAssertEqual(checklist.count, 3)
        XCTAssertFalse(messages.simple.isEmpty)
        XCTAssertFalse(messages.warm.isEmpty)
        XCTAssertFalse(messages.shortWhatsApp.isEmpty)
        XCTAssertEqual(summary.decided.count, 1)
        XCTAssertEqual(summary.missing.count, 2)
        XCTAssertFalse(summary.recommendedNextAction.isEmpty)
        XCTAssertEqual(transport.missingDetails.count, 1)
        XCTAssertEqual(transport.coordinationIdeas.count, 1)
        XCTAssertFalse(transport.groupMessageDraft.isEmpty)
    }

    func testEventDraftGeneratorReportsTimeout() async {
        let generator = EventDraftGenerator(
            availabilityProvider: FixedWakeveAIAvailabilityService(availability: .available),
            client: SlowWakeveAIClient(),
            timeoutSeconds: 0.01
        )

        do {
            for try await _ in generator.generate(request: WakeveAIGenerationRequest(userInput: "Road trip entre amis")) {}
            XCTFail("Expected timeout error")
        } catch WakeveAIError.timedOut {
            XCTAssertTrue(true)
        } catch {
            XCTFail("Unexpected error: \(error)")
        }
    }
}

private struct FakeWakeveAIClient: WakeveAIClientProtocol {
    func generateEventDraft(prompt: WakeveAIPrompt, request: WakeveAIGenerationRequest) async throws -> EventDraft {
        EventDraft(
            title: "Week-end Marrakech",
            subtitle: "Entre amis",
            description: "Un week-end simple à organiser.",
            destinationName: "Marrakech",
            locationHint: "Marrakech",
            dateOptions: [
                DateOption(label: "Début juillet", startDateHint: "début juillet", endDateHint: "", confidence: 0.6, reason: "Mentionné")
            ],
            participantHints: ["8 amis"],
            suggestedPolls: [
                PollSuggestion(question: "Quelle date convient le mieux ?", options: ["Option 1", "Option 2"], pollType: .date)
            ],
            checklist: [
                ChecklistItem(title: "Confirmer le budget", category: .budget, priority: .medium)
            ],
            transportHints: [
                TransportHint(type: .flight, label: "Vols", description: "Comparer les départs.")
            ],
            rationale: "Phrase utilisateur structurée."
        )
    }

    func generatePollSuggestions(prompt: WakeveAIPrompt, knownFacts: WakeveAIKnownFacts) async throws -> [PollSuggestion] {
        [
            PollSuggestion(question: "Quelle date convient le mieux ?", options: ["Samedi", "Dimanche"], pollType: .date),
            PollSuggestion(question: "Quel budget ?", options: ["Simple", "Équilibré"], pollType: .budget),
            PollSuggestion(question: "Qui conduit ?", options: ["Conducteur", "Passager"], pollType: .transport),
            PollSuggestion(question: "Suggestion en trop", options: ["A"], pollType: .generic)
        ]
    }

    func generateChecklist(prompt: WakeveAIPrompt, knownFacts: WakeveAIKnownFacts) async throws -> [ChecklistItem] {
        [
            ChecklistItem(title: "Confirmer le lieu", category: .venue, priority: .high),
            ChecklistItem(title: "Lister les invités", category: .guests, priority: .high),
            ChecklistItem(title: "Préparer le budget", category: .budget, priority: .medium)
        ]
    }

    func generateInvitationMessages(prompt: WakeveAIPrompt, knownFacts: WakeveAIKnownFacts) async throws -> InvitationMessageSet {
        InvitationMessageSet(
            simple: "On prépare l'événement, vous êtes partants ?",
            warm: "J'aimerais vous réunir pour ce moment.",
            shortWhatsApp: "Partants ? Je vous envoie les options."
        )
    }

    func generateEventSummary(prompt: WakeveAIPrompt, knownFacts: WakeveAIKnownFacts) async throws -> EventSummary {
        EventSummary(decided: ["Date pressentie"], missing: ["Lieu", "Budget"], recommendedNextAction: "Lancer le sondage.")
    }

    func generateTransportSuggestions(prompt: WakeveAIPrompt, knownFacts: WakeveAIKnownFacts) async throws -> TransportCoordinationSuggestion {
        TransportCoordinationSuggestion(
            missingDetails: ["Ville de départ"],
            coordinationIdeas: ["Regrouper par ville"],
            groupMessageDraft: "Ajoutez votre ville de départ pour coordonner les trajets."
        )
    }
}

private struct SlowWakeveAIClient: WakeveAIClientProtocol {
    func generateEventDraft(prompt: WakeveAIPrompt, request: WakeveAIGenerationRequest) async throws -> EventDraft {
        try await Task.sleep(nanoseconds: 200_000_000)
        return .empty
    }

    func generatePollSuggestions(prompt: WakeveAIPrompt, knownFacts: WakeveAIKnownFacts) async throws -> [PollSuggestion] { [] }
    func generateChecklist(prompt: WakeveAIPrompt, knownFacts: WakeveAIKnownFacts) async throws -> [ChecklistItem] { [] }
    func generateInvitationMessages(prompt: WakeveAIPrompt, knownFacts: WakeveAIKnownFacts) async throws -> InvitationMessageSet {
        InvitationMessageSet(simple: "a", warm: "b", shortWhatsApp: "c")
    }
    func generateEventSummary(prompt: WakeveAIPrompt, knownFacts: WakeveAIKnownFacts) async throws -> EventSummary {
        EventSummary(decided: [], missing: [], recommendedNextAction: "Action")
    }
    func generateTransportSuggestions(prompt: WakeveAIPrompt, knownFacts: WakeveAIKnownFacts) async throws -> TransportCoordinationSuggestion {
        TransportCoordinationSuggestion(missingDetails: [], coordinationIdeas: [], groupMessageDraft: "Message")
    }
}

private struct StaticWakeveAIContextProvider: WakeveAIContextProviding {
    let eventContextValue = WakeveAIEventContext(
        eventId: "event-1",
        title: "Week-end",
        date: "samedi",
        location: "Lyon",
        participantNames: ["Emma", "Noah"],
        voteSummaries: ["Samedi"],
        taskTitles: ["Confirmer le budget"],
        recentMessages: ["Tout le monde est partant"]
    )

    let transportContextValue = WakeveAITransportContext(
        proposedTrips: ["Train"],
        participantNames: ["Emma", "Noah"],
        schedules: ["samedi"],
        missingDepartureParticipants: ["Noah"]
    )

    func currentGroup() async -> WakeveAIGroupContext? {
        WakeveAIGroupContext(groupId: "event-1", memberDisplayNames: eventContextValue.participantNames)
    }

    func eventContext(eventId: String) async -> WakeveAIEventContext? {
        eventContextValue
    }

    func participantStatuses(eventId: String) async -> WakeveAIParticipantStatuses? {
        WakeveAIParticipantStatuses(accepted: ["Emma"], pending: ["Noah"], declined: [])
    }

    func voteResults(eventId: String) async -> WakeveAIVoteResults? {
        WakeveAIVoteResults(activePolls: ["Date"], results: ["Samedi"])
    }

    func transportContext(eventId: String) async -> WakeveAITransportContext? {
        transportContextValue
    }

    func userPreferences() async -> WakeveAIUserPreferences? {
        WakeveAIUserPreferences(languageCode: "fr", localPreferences: [])
    }
}
