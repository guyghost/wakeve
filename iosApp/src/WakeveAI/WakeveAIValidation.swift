import Foundation

enum WakeveAIValidator {
    private static let maxItems = 3
    private static let maxShortTextLength = 180
    private static let maxMessageLength = 420

    static func validate(_ draft: EventDraft, knownFacts: WakeveAIKnownFacts = .empty) -> [WakeveAIValidationIssue] {
        var issues: [WakeveAIValidationIssue] = []

        if draft.title.trimmed.isEmpty || draft.description.trimmed.isEmpty {
            issues.append(.emptyRequiredText)
        }

        let boundedCollections = [
            draft.dateOptions.count,
            draft.participantHints.count,
            draft.suggestedPolls.count,
            draft.transportHints.count
        ]
        if boundedCollections.contains(where: { $0 > maxItems }) {
            issues.append(.tooManyItems)
        }

        if hasOverlongText(in: [
            draft.title,
            draft.subtitle,
            draft.description,
            draft.destinationName,
            draft.locationHint,
            draft.rationale
        ]) {
            issues.append(.overlongText)
        }

        if draft.dateOptions.contains(where: { $0.confidence < 0 || $0.confidence > 1 }) {
            issues.append(.invalidConfidence)
        }

        issues.append(contentsOf: factIssues(in: draft.allGeneratedText, knownFacts: knownFacts))
        return Array(Set(issues)).sorted { $0.rawValue < $1.rawValue }
    }

    static func sanitized(_ draft: EventDraft, knownFacts: WakeveAIKnownFacts = .empty) -> EventDraft {
        EventDraft(
            title: draft.title.shortened(to: maxShortTextLength),
            subtitle: draft.subtitle.shortened(to: maxShortTextLength),
            description: draft.description.shortened(to: maxShortTextLength),
            destinationName: draft.destinationName.shortened(to: maxShortTextLength),
            locationHint: draft.locationHint.shortened(to: maxShortTextLength),
            dateOptions: Array(draft.dateOptions.prefix(maxItems)).map(sanitized),
            participantHints: Array(draft.participantHints.prefix(maxItems)).map { $0.shortened(to: maxShortTextLength) },
            suggestedPolls: Array(draft.suggestedPolls.prefix(maxItems)).map(sanitized),
            checklist: sanitizedChecklist(draft.checklist),
            transportHints: Array(draft.transportHints.prefix(maxItems)).map(sanitized),
            rationale: draft.rationale.shortened(to: maxShortTextLength)
        )
    }

    static func validate(_ messages: InvitationMessageSet, knownFacts: WakeveAIKnownFacts = .empty) -> [WakeveAIValidationIssue] {
        var issues: [WakeveAIValidationIssue] = []
        let values = [messages.simple, messages.warm, messages.shortWhatsApp]
        if values.contains(where: { $0.trimmed.isEmpty }) {
            issues.append(.emptyRequiredText)
        }
        if values.contains(where: { $0.count > maxMessageLength }) {
            issues.append(.overlongText)
        }
        issues.append(contentsOf: factIssues(in: values, knownFacts: knownFacts))
        return Array(Set(issues)).sorted { $0.rawValue < $1.rawValue }
    }

    static func validate(_ summary: EventSummary, knownFacts: WakeveAIKnownFacts = .empty) -> [WakeveAIValidationIssue] {
        var issues: [WakeveAIValidationIssue] = []
        if summary.recommendedNextAction.trimmed.isEmpty {
            issues.append(.emptyRequiredText)
        }
        if summary.decided.count > maxItems || summary.missing.count > maxItems {
            issues.append(.tooManyItems)
        }
        issues.append(contentsOf: factIssues(in: summary.decided + summary.missing + [summary.recommendedNextAction], knownFacts: knownFacts))
        return Array(Set(issues)).sorted { $0.rawValue < $1.rawValue }
    }

    static func validate(_ transport: TransportCoordinationSuggestion, knownFacts: WakeveAIKnownFacts = .empty) -> [WakeveAIValidationIssue] {
        var issues: [WakeveAIValidationIssue] = []
        if transport.groupMessageDraft.trimmed.isEmpty {
            issues.append(.emptyRequiredText)
        }
        if transport.missingDetails.count > maxItems || transport.coordinationIdeas.count > maxItems {
            issues.append(.tooManyItems)
        }
        issues.append(contentsOf: factIssues(
            in: transport.missingDetails + transport.coordinationIdeas + [transport.groupMessageDraft],
            knownFacts: knownFacts
        ))
        return Array(Set(issues)).sorted { $0.rawValue < $1.rawValue }
    }

    static func requiresFallback(_ issues: [WakeveAIValidationIssue]) -> Bool {
        let blocking: Set<WakeveAIValidationIssue> = [
            .emptyRequiredText,
            .inventedParticipant,
            .inventedVote,
            .inventedAvailability,
            .inventedAddress,
            .inventedPrice,
            .inventedTransportFact
        ]
        return issues.contains { blocking.contains($0) }
    }

    private static func sanitized(_ option: DateOption) -> DateOption {
        DateOption(
            label: option.label.shortened(to: maxShortTextLength),
            startDateHint: option.startDateHint.shortened(to: maxShortTextLength),
            endDateHint: option.endDateHint.shortened(to: maxShortTextLength),
            confidence: min(max(option.confidence, 0), 1),
            reason: option.reason.shortened(to: maxShortTextLength)
        )
    }

    private static func sanitized(_ poll: PollSuggestion) -> PollSuggestion {
        PollSuggestion(
            question: poll.question.shortened(to: maxShortTextLength),
            options: Array(poll.options.prefix(maxItems)).map { $0.shortened(to: maxShortTextLength) },
            pollType: poll.pollType
        )
    }

    private static func sanitized(_ hint: TransportHint) -> TransportHint {
        TransportHint(
            type: hint.type,
            label: hint.label.shortened(to: maxShortTextLength),
            description: hint.description.shortened(to: maxShortTextLength)
        )
    }

    private static func sanitizedChecklist(_ items: [ChecklistItem]) -> [ChecklistItem] {
        var counts: [ChecklistCategory: Int] = [:]
        var result: [ChecklistItem] = []

        for item in items {
            let current = counts[item.category, default: 0]
            guard current < maxItems else { continue }
            counts[item.category] = current + 1
            result.append(ChecklistItem(
                title: item.title.shortened(to: maxShortTextLength),
                category: item.category,
                priority: item.priority
            ))
        }

        return result
    }

    private static func hasOverlongText(in values: [String]) -> Bool {
        values.contains { $0.count > maxShortTextLength }
    }

    private static func factIssues(in values: [String], knownFacts: WakeveAIKnownFacts) -> [WakeveAIValidationIssue] {
        let combined = values.joined(separator: " ")
        var issues: [WakeveAIValidationIssue] = []

        if containsTaggedFact("participant:", in: combined, allowedValues: knownFacts.participantNames) {
            issues.append(.inventedParticipant)
        }
        if containsTaggedFact("vote:", in: combined, allowedValues: knownFacts.voteLabels) {
            issues.append(.inventedVote)
        }
        if containsTaggedFact("availability:", in: combined, allowedValues: knownFacts.availabilityLabels) {
            issues.append(.inventedAvailability)
        }
        if containsTaggedFact("address:", in: combined, allowedValues: knownFacts.addresses) {
            issues.append(.inventedAddress)
        }
        if containsTaggedFact("price:", in: combined, allowedValues: knownFacts.priceLabels) {
            issues.append(.inventedPrice)
        }
        if containsTaggedFact("transport:", in: combined, allowedValues: knownFacts.transportLabels) {
            issues.append(.inventedTransportFact)
        }

        return issues
    }

    private static func containsTaggedFact(_ tag: String, in text: String, allowedValues: Set<String>) -> Bool {
        let lowercased = text.lowercased()
        guard lowercased.contains(tag) else { return false }
        return !allowedValues.contains { lowercased.contains("\(tag)\($0.lowercased())") }
    }
}

private extension EventDraft {
    var allGeneratedText: [String] {
        [title, subtitle, description, destinationName, locationHint, rationale] +
        participantHints +
        dateOptions.flatMap { [$0.label, $0.startDateHint, $0.endDateHint, $0.reason] } +
        suggestedPolls.flatMap { [$0.question] + $0.options } +
        checklist.map(\.title) +
        transportHints.flatMap { [$0.label, $0.description] }
    }
}

private extension String {
    var trimmed: String {
        trimmingCharacters(in: .whitespacesAndNewlines)
    }

    func shortened(to limit: Int) -> String {
        guard count > limit else { return self }
        return String(prefix(limit)).trimmingCharacters(in: .whitespacesAndNewlines)
    }
}
