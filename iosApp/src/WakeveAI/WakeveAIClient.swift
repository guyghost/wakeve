import Foundation

#if canImport(FoundationModels)
import FoundationModels
#endif

protocol WakeveAIClientProtocol: Sendable {
    func generateEventDraft(prompt: WakeveAIPrompt, request: WakeveAIGenerationRequest) async throws -> EventDraft
    func generatePollSuggestions(prompt: WakeveAIPrompt, knownFacts: WakeveAIKnownFacts) async throws -> [PollSuggestion]
    func generateChecklist(prompt: WakeveAIPrompt, knownFacts: WakeveAIKnownFacts) async throws -> [ChecklistItem]
    func generateInvitationMessages(prompt: WakeveAIPrompt, knownFacts: WakeveAIKnownFacts) async throws -> InvitationMessageSet
    func generateEventSummary(prompt: WakeveAIPrompt, knownFacts: WakeveAIKnownFacts) async throws -> EventSummary
    func generateTransportSuggestions(prompt: WakeveAIPrompt, knownFacts: WakeveAIKnownFacts) async throws -> TransportCoordinationSuggestion
}

struct WakeveAIClientFactory {
    static func makeDefault(availability: WakeveAIAvailability) -> WakeveAIClientProtocol {
        #if canImport(FoundationModels)
        if #available(iOS 26.0, *), availability == .available {
            return FoundationModelsWakeveAIClient()
        }
        #endif
        return HeuristicWakeveAIClient()
    }
}

struct HeuristicWakeveAIClient: WakeveAIClientProtocol {
    func generateEventDraft(prompt: WakeveAIPrompt, request: WakeveAIGenerationRequest) async throws -> EventDraft {
        let input = request.userInput.trimmingCharacters(in: .whitespacesAndNewlines)
        let lower = input.lowercased()
        let title = Self.title(from: input)
        let destination = Self.destination(from: input)
        let participants = Self.participantHints(from: lower)

        return EventDraft(
            title: title,
            subtitle: destination.isEmpty ? "A organiser ensemble" : destination,
            description: input.isEmpty ? "Un moment a organiser avec le groupe." : input,
            destinationName: destination,
            locationHint: destination.isEmpty ? "Lieu a confirmer" : destination,
            dateOptions: Self.dateOptions(from: lower),
            participantHints: participants,
            suggestedPolls: Self.pollSuggestions(from: lower),
            checklist: Self.checklist(from: lower),
            transportHints: Self.transportHints(from: lower),
            rationale: "Brouillon local base sur la phrase, a valider avant application."
        )
    }

    func generatePollSuggestions(prompt: WakeveAIPrompt, knownFacts: WakeveAIKnownFacts) async throws -> [PollSuggestion] {
        [
            PollSuggestion(question: "Quelle date convient le mieux ?", options: ["Option 1", "Option 2", "Option 3"], pollType: .date),
            PollSuggestion(question: "Quel budget par personne ?", options: ["Economique", "Equilibre", "Confort"], pollType: .budget),
            PollSuggestion(question: "Qui vient en voiture ?", options: ["Je conduis", "Je cherche une place", "Pas besoin"], pollType: .transport)
        ]
    }

    func generateChecklist(prompt: WakeveAIPrompt, knownFacts: WakeveAIKnownFacts) async throws -> [ChecklistItem] {
        [
            ChecklistItem(title: "Confirmer le lieu", category: .venue, priority: .high),
            ChecklistItem(title: "Lister les invites", category: .guests, priority: .high),
            ChecklistItem(title: "Prevoir le budget", category: .budget, priority: .medium)
        ]
    }

    func generateInvitationMessages(prompt: WakeveAIPrompt, knownFacts: WakeveAIKnownFacts) async throws -> InvitationMessageSet {
        InvitationMessageSet(
            simple: "Je prepare un evenement Wakeve. Dites-moi si vous etes partants.",
            warm: "On organise un moment ensemble, et j'aimerais beaucoup vous avoir avec nous.",
            shortWhatsApp: "Partants pour cet evenement ? Je vous envoie les options."
        )
    }

    func generateEventSummary(prompt: WakeveAIPrompt, knownFacts: WakeveAIKnownFacts) async throws -> EventSummary {
        EventSummary(
            decided: ["Le brouillon d'evenement existe"],
            missing: ["Verifier les invites", "Confirmer les details pratiques"],
            recommendedNextAction: "Valider la prochaine action avec le groupe."
        )
    }

    func generateTransportSuggestions(prompt: WakeveAIPrompt, knownFacts: WakeveAIKnownFacts) async throws -> TransportCoordinationSuggestion {
        TransportCoordinationSuggestion(
            missingDetails: ["Demander les lieux de depart"],
            coordinationIdeas: ["Regrouper les personnes par ville de depart"],
            groupMessageDraft: "Vous pouvez ajouter votre ville de depart pour qu'on coordonne les trajets ?"
        )
    }

    private static func title(from input: String) -> String {
        let trimmed = input.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return "Nouvel evenement" }
        return String(trimmed.prefix(48)).capitalized
    }

    private static func destination(from input: String) -> String {
        let markers = [" a ", " à ", " au ", " aux "]
        for marker in markers {
            if let range = input.range(of: marker, options: [.caseInsensitive, .diacriticInsensitive]) {
                let tail = input[range.upperBound...]
                let stopWords = [" avec ", " en ", " debut ", " début ", " samedi ", " dimanche "]
                let destination = stopWords.reduce(String(tail)) { current, stop in
                    if let stopRange = current.range(of: stop, options: [.caseInsensitive, .diacriticInsensitive]) {
                        return String(current[..<stopRange.lowerBound])
                    }
                    return current
                }
                return destination.trimmingCharacters(in: .whitespacesAndNewlines).shortHint
            }
        }
        return ""
    }

    private static func participantHints(from input: String) -> [String] {
        if input.contains("famille") { return ["Famille"] }
        if input.contains("amis") || input.contains("ami") { return ["Amis"] }
        if input.contains("enfant") { return ["Enfants", "Parents"] }
        return ["Groupe a confirmer"]
    }

    private static func dateOptions(from input: String) -> [DateOption] {
        if input.contains("samedi") {
            return [DateOption(label: "Samedi", startDateHint: "samedi", endDateHint: "", confidence: 0.7, reason: "Jour mentionne par l'utilisateur")]
        }
        if input.contains("juillet") {
            return [DateOption(label: "Debut juillet", startDateHint: "debut juillet", endDateHint: "", confidence: 0.55, reason: "Periode vague a transformer en sondage")]
        }
        return [DateOption(label: "Date a proposer", startDateHint: "a confirmer", endDateHint: "", confidence: 0.35, reason: "La phrase ne donne pas de date precise")]
    }

    private static func pollSuggestions(from input: String) -> [PollSuggestion] {
        var polls = [
            PollSuggestion(question: "Quelle date convient le mieux ?", options: ["Option 1", "Option 2", "Option 3"], pollType: .date)
        ]
        if input.contains("barbecue") || input.contains("diner") || input.contains("dîner") {
            polls.append(PollSuggestion(question: "Que mange-t-on ?", options: ["Barbecue", "A partager", "Sur place"], pollType: .food))
        }
        polls.append(PollSuggestion(question: "Qui gere le transport ?", options: ["Je conduis", "Je cherche une place", "Transport public"], pollType: .transport))
        return Array(polls.prefix(3))
    }

    private static func checklist(from input: String) -> [ChecklistItem] {
        var items = [
            ChecklistItem(title: "Confirmer les invites", category: .guests, priority: .high),
            ChecklistItem(title: "Fixer un budget simple", category: .budget, priority: .medium)
        ]
        if input.contains("barbecue") || input.contains("plage") {
            items.insert(ChecklistItem(title: "Prevoir repas et materiel", category: .food, priority: .high), at: 0)
        }
        return Array(items.prefix(3))
    }

    private static func transportHints(from input: String) -> [TransportHint] {
        if input.contains("road trip") {
            return [TransportHint(type: .carpool, label: "Voitures", description: "Demander qui conduit et les places disponibles.")]
        }
        return [TransportHint(type: .missingInfo, label: "Departs", description: "Demander les villes de depart avant de coordonner.")]
    }
}

#if canImport(FoundationModels)
@available(iOS 26.0, *)
struct FoundationModelsWakeveAIClient: WakeveAIClientProtocol {
    func generateEventDraft(prompt: WakeveAIPrompt, request: WakeveAIGenerationRequest) async throws -> EventDraft {
        let session = LanguageModelSession(instructions: prompt.system)
        let response = try await session.respond(to: prompt.user, generating: FoundationEventDraft.self)
        return response.content.coreModel
    }

    func generatePollSuggestions(prompt: WakeveAIPrompt, knownFacts: WakeveAIKnownFacts) async throws -> [PollSuggestion] {
        let session = LanguageModelSession(instructions: prompt.system)
        let response = try await session.respond(to: prompt.user, generating: FoundationPollSuggestionBatch.self)
        return response.content.suggestions.map(\.coreModel)
    }

    func generateChecklist(prompt: WakeveAIPrompt, knownFacts: WakeveAIKnownFacts) async throws -> [ChecklistItem] {
        let session = LanguageModelSession(instructions: prompt.system)
        let response = try await session.respond(to: prompt.user, generating: FoundationChecklistBatch.self)
        return response.content.items.map(\.coreModel)
    }

    func generateInvitationMessages(prompt: WakeveAIPrompt, knownFacts: WakeveAIKnownFacts) async throws -> InvitationMessageSet {
        let session = LanguageModelSession(instructions: prompt.system)
        let response = try await session.respond(to: prompt.user, generating: FoundationInvitationMessageSet.self)
        return response.content.coreModel
    }

    func generateEventSummary(prompt: WakeveAIPrompt, knownFacts: WakeveAIKnownFacts) async throws -> EventSummary {
        let session = LanguageModelSession(instructions: prompt.system)
        let response = try await session.respond(to: prompt.user, generating: FoundationEventSummary.self)
        return response.content.coreModel
    }

    func generateTransportSuggestions(prompt: WakeveAIPrompt, knownFacts: WakeveAIKnownFacts) async throws -> TransportCoordinationSuggestion {
        let session = LanguageModelSession(instructions: prompt.system)
        let response = try await session.respond(to: prompt.user, generating: FoundationTransportCoordinationSuggestion.self)
        return response.content.coreModel
    }
}

@available(iOS 26.0, *)
@Generable
struct FoundationEventDraft: Sendable {
    var title: String
    var subtitle: String
    var description: String
    var destinationName: String
    var locationHint: String
    var dateOptions: [FoundationDateOption]
    var participantHints: [String]
    var suggestedPolls: [FoundationPollSuggestion]
    var checklist: [FoundationChecklistItem]
    var transportHints: [FoundationTransportHint]
    var rationale: String

    var coreModel: EventDraft {
        EventDraft(
            title: title,
            subtitle: subtitle,
            description: description,
            destinationName: destinationName,
            locationHint: locationHint,
            dateOptions: dateOptions.map(\.coreModel),
            participantHints: participantHints,
            suggestedPolls: suggestedPolls.map(\.coreModel),
            checklist: checklist.map(\.coreModel),
            transportHints: transportHints.map(\.coreModel),
            rationale: rationale
        )
    }
}

@available(iOS 26.0, *)
@Generable
struct FoundationDateOption: Sendable {
    var label: String
    var startDateHint: String
    var endDateHint: String
    var confidence: Double
    var reason: String

    var coreModel: DateOption {
        DateOption(
            label: label,
            startDateHint: startDateHint,
            endDateHint: endDateHint,
            confidence: confidence,
            reason: reason
        )
    }
}

@available(iOS 26.0, *)
@Generable
struct FoundationPollSuggestion: Sendable {
    var question: String
    var options: [String]
    var pollType: String

    var coreModel: PollSuggestion {
        PollSuggestion(
            question: question,
            options: options,
            pollType: PollSuggestionType(rawValue: pollType) ?? .generic
        )
    }
}

@available(iOS 26.0, *)
@Generable
struct FoundationChecklistItem: Sendable {
    var title: String
    var category: String
    var priority: String

    var coreModel: ChecklistItem {
        ChecklistItem(
            title: title,
            category: ChecklistCategory(rawValue: category) ?? .guests,
            priority: ChecklistPriority(rawValue: priority) ?? .medium
        )
    }
}

@available(iOS 26.0, *)
@Generable
struct FoundationTransportHint: Sendable {
    var type: String
    var label: String
    var description: String

    var coreModel: TransportHint {
        TransportHint(
            type: TransportHintType(rawValue: type) ?? .missingInfo,
            label: label,
            description: description
        )
    }
}

@available(iOS 26.0, *)
@Generable
struct FoundationPollSuggestionBatch: Sendable {
    var suggestions: [FoundationPollSuggestion]
}

@available(iOS 26.0, *)
@Generable
struct FoundationChecklistBatch: Sendable {
    var items: [FoundationChecklistItem]
}

@available(iOS 26.0, *)
@Generable
struct FoundationInvitationMessageSet: Sendable {
    var simple: String
    var warm: String
    var shortWhatsApp: String

    var coreModel: InvitationMessageSet {
        InvitationMessageSet(simple: simple, warm: warm, shortWhatsApp: shortWhatsApp)
    }
}

@available(iOS 26.0, *)
@Generable
struct FoundationEventSummary: Sendable {
    var decided: [String]
    var missing: [String]
    var recommendedNextAction: String

    var coreModel: EventSummary {
        EventSummary(decided: decided, missing: missing, recommendedNextAction: recommendedNextAction)
    }
}

@available(iOS 26.0, *)
@Generable
struct FoundationTransportCoordinationSuggestion: Sendable {
    var missingDetails: [String]
    var coordinationIdeas: [String]
    var groupMessageDraft: String

    var coreModel: TransportCoordinationSuggestion {
        TransportCoordinationSuggestion(
            missingDetails: missingDetails,
            coordinationIdeas: coordinationIdeas,
            groupMessageDraft: groupMessageDraft
        )
    }
}
#endif

private extension String {
    var shortHint: String {
        String(prefix(40)).trimmingCharacters(in: .whitespacesAndNewlines)
    }
}
