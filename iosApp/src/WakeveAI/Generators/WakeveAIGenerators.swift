import Foundation

struct EventDraftGenerator: Sendable {
    var availabilityProvider: WakeveAIAvailabilityProviding
    var client: WakeveAIClientProtocol?
    var timeoutSeconds: TimeInterval

    init(
        availabilityProvider: WakeveAIAvailabilityProviding = WakeveAIAvailabilityService(),
        client: WakeveAIClientProtocol? = nil,
        timeoutSeconds: TimeInterval = 12
    ) {
        self.availabilityProvider = availabilityProvider
        self.client = client
        self.timeoutSeconds = timeoutSeconds
    }

    func generate(request: WakeveAIGenerationRequest) -> AsyncThrowingStream<EventDraftSection, Error> {
        AsyncThrowingStream { continuation in
            let task = Task {
                let availability = availabilityProvider.currentAvailability()
                let startedAt = Date()
                let prompt = WakeveAIPromptCatalog.eventDraft(
                    userInput: request.userInput,
                    localeIdentifier: request.localeIdentifier
                )

                guard availability.isAvailable || client != nil else {
                    continuation.finish(throwing: WakeveAIError.unavailable(availability))
                    return
                }

                do {
                    let selectedClient = client ?? WakeveAIClientFactory.makeDefault(availability: availability)
                    let draft = try await withTimeout(seconds: timeoutSeconds) {
                        try await selectedClient.generateEventDraft(prompt: prompt, request: request)
                    }
                    let sanitized = WakeveAIValidator.sanitized(draft, knownFacts: request.knownFacts)
                    let issues = WakeveAIValidator.validate(sanitized, knownFacts: request.knownFacts)
                    WakeveAIMetricsRecorder.shared.record(WakeveAIMetrics(
                        promptId: prompt.id,
                        availability: availability,
                        startedAt: startedAt,
                        completedAt: Date(),
                        timedOut: false,
                        cancelled: false,
                        validationIssues: issues
                    ))

                    if WakeveAIValidator.requiresFallback(issues) {
                        continuation.finish(throwing: WakeveAIError.validationFailed(issues))
                        return
                    }

                    continuation.yield(.title(sanitized.title))
                    continuation.yield(.description(sanitized.description))
                    continuation.yield(.dateOptions(sanitized.dateOptions))
                    continuation.yield(.checklist(sanitized.checklist))
                    continuation.yield(.suggestedPolls(sanitized.suggestedPolls))
                    continuation.yield(.completed(sanitized))
                    continuation.finish()
                } catch is CancellationError {
                    WakeveAIMetricsRecorder.shared.record(WakeveAIMetrics(
                        promptId: prompt.id,
                        availability: availability,
                        startedAt: startedAt,
                        completedAt: Date(),
                        timedOut: false,
                        cancelled: true,
                        validationIssues: []
                    ))
                    continuation.finish(throwing: WakeveAIError.cancelled)
                } catch WakeveAIError.timedOut {
                    WakeveAIMetricsRecorder.shared.record(WakeveAIMetrics(
                        promptId: prompt.id,
                        availability: availability,
                        startedAt: startedAt,
                        completedAt: Date(),
                        timedOut: true,
                        cancelled: false,
                        validationIssues: []
                    ))
                    continuation.finish(throwing: WakeveAIError.timedOut)
                } catch {
                    continuation.finish(throwing: error)
                }
            }

            continuation.onTermination = { _ in
                task.cancel()
            }
        }
    }
}

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

private func withTimeout<T: Sendable>(
    seconds: TimeInterval,
    operation: @escaping @Sendable () async throws -> T
) async throws -> T {
    try await withThrowingTaskGroup(of: T.self) { group in
        group.addTask {
            try await operation()
        }
        group.addTask {
            try await Task.sleep(nanoseconds: UInt64(seconds * 1_000_000_000))
            throw WakeveAIError.timedOut
        }

        guard let result = try await group.next() else {
            throw WakeveAIError.generationFailed("No generation result")
        }
        group.cancelAll()
        return result
    }
}
