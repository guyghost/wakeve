import Foundation

struct PollSuggestionGenerator: Sendable {
    var client: WakeveAIClientProtocol

    func generate(context: String, knownFacts: WakeveAIKnownFacts, localeIdentifier: String = Locale.current.identifier) async throws -> [PollSuggestion] {
        let prompt = WakeveAIPromptCatalog.pollSuggestions(context: context, localeIdentifier: localeIdentifier)
        let suggestions = try await client.generatePollSuggestions(prompt: prompt, knownFacts: knownFacts)
        return Array(suggestions.prefix(3))
    }
}

struct ChecklistGenerator: Sendable {
    var client: WakeveAIClientProtocol

    func generate(context: String, knownFacts: WakeveAIKnownFacts, localeIdentifier: String = Locale.current.identifier) async throws -> [ChecklistItem] {
        let prompt = WakeveAIPromptCatalog.checklist(context: context, localeIdentifier: localeIdentifier)
        let items = try await client.generateChecklist(prompt: prompt, knownFacts: knownFacts)
        return WakeveAIValidator.sanitized(EventDraft(
            title: "Checklist",
            subtitle: "",
            description: "Checklist",
            destinationName: "",
            locationHint: "",
            dateOptions: [],
            participantHints: [],
            suggestedPolls: [],
            checklist: items,
            transportHints: [],
            rationale: ""
        )).checklist
    }
}

struct InvitationMessageGenerator: Sendable {
    var client: WakeveAIClientProtocol

    func generate(context: String, knownFacts: WakeveAIKnownFacts, localeIdentifier: String = Locale.current.identifier) async throws -> InvitationMessageSet {
        let prompt = WakeveAIPromptCatalog.invitationMessage(context: context, localeIdentifier: localeIdentifier)
        let messages = try await client.generateInvitationMessages(prompt: prompt, knownFacts: knownFacts)
        let issues = WakeveAIValidator.validate(messages, knownFacts: knownFacts)
        if WakeveAIValidator.requiresFallback(issues) {
            throw WakeveAIError.validationFailed(issues)
        }
        return messages
    }
}

struct EventSummaryGenerator: Sendable {
    var client: WakeveAIClientProtocol
    var contextProvider: WakeveAIContextProviding

    func generate(eventId: String, localeIdentifier: String = Locale.current.identifier) async throws -> EventSummary {
        let context = await contextProvider.eventContext(eventId: eventId)
        let knownFacts = context?.knownFacts ?? .empty
        let prompt = WakeveAIPromptCatalog.eventSummary(
            context: context?.promptSummary ?? "No event context available.",
            localeIdentifier: localeIdentifier
        )
        let summary = try await client.generateEventSummary(prompt: prompt, knownFacts: knownFacts)
        let issues = WakeveAIValidator.validate(summary, knownFacts: knownFacts)
        if WakeveAIValidator.requiresFallback(issues) {
            throw WakeveAIError.validationFailed(issues)
        }
        return EventSummary(
            decided: Array(summary.decided.prefix(3)),
            missing: Array(summary.missing.prefix(3)),
            recommendedNextAction: summary.recommendedNextAction
        )
    }
}

struct TransportSuggestionGenerator: Sendable {
    var client: WakeveAIClientProtocol
    var contextProvider: WakeveAIContextProviding

    func generate(eventId: String, localeIdentifier: String = Locale.current.identifier) async throws -> TransportCoordinationSuggestion {
        let context = await contextProvider.transportContext(eventId: eventId)
        let knownFacts = context?.knownFacts ?? .empty
        let prompt = WakeveAIPromptCatalog.transportSuggestions(
            context: context?.promptSummary ?? "No transport context available.",
            localeIdentifier: localeIdentifier
        )
        let suggestion = try await client.generateTransportSuggestions(prompt: prompt, knownFacts: knownFacts)
        let issues = WakeveAIValidator.validate(suggestion, knownFacts: knownFacts)
        if WakeveAIValidator.requiresFallback(issues) {
            throw WakeveAIError.validationFailed(issues)
        }
        return TransportCoordinationSuggestion(
            missingDetails: Array(suggestion.missingDetails.prefix(3)),
            coordinationIdeas: Array(suggestion.coordinationIdeas.prefix(3)),
            groupMessageDraft: suggestion.groupMessageDraft
        )
    }
}
