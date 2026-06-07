import Foundation

#if canImport(FoundationModels)
import FoundationModels
#endif

enum WakeveAIAvailability: Equatable, Sendable {
    case available
    case appleIntelligenceDisabled
    case notReady
    case unsupportedDevice
    case unknownUnavailable(String)

    var isAvailable: Bool {
        self == .available
    }

    var discreetMessage: String {
        switch self {
        case .available:
            return "Suggestion disponible"
        case .appleIntelligenceDisabled:
            return "Apple Intelligence est desactive. Vous pouvez continuer sans suggestion."
        case .notReady:
            return "La suggestion n'est pas prete pour le moment."
        case .unsupportedDevice:
            return "La suggestion n'est pas disponible sur cet appareil."
        case .unknownUnavailable:
            return "La suggestion n'est pas disponible pour le moment."
        }
    }
}

enum WakeveAIError: Error, Equatable {
    case unavailable(WakeveAIAvailability)
    case validationFailed([WakeveAIValidationIssue])
    case timedOut
    case cancelled
    case generationFailed(String)
}

enum WakeveAIValidationIssue: String, Equatable, Hashable, Sendable {
    case emptyRequiredText
    case tooManyItems
    case overlongText
    case invalidConfidence
    case inventedParticipant
    case inventedVote
    case inventedAvailability
    case inventedAddress
    case inventedPrice
    case inventedTransportFact
}

struct EventDraft: Codable, Equatable, Hashable, Sendable {
    var title: String
    var subtitle: String
    var description: String
    var destinationName: String
    var locationHint: String
    var dateOptions: [DateOption]
    var participantHints: [String]
    var suggestedPolls: [PollSuggestion]
    var checklist: [ChecklistItem]
    var transportHints: [TransportHint]
    var rationale: String
}

struct DateOption: Codable, Equatable, Hashable, Sendable {
    var label: String
    var startDateHint: String
    var endDateHint: String
    var confidence: Double
    var reason: String
}

struct PollSuggestion: Codable, Equatable, Hashable, Sendable {
    var question: String
    var options: [String]
    var pollType: PollSuggestionType
}

enum PollSuggestionType: String, Codable, CaseIterable, Hashable, Sendable {
    case date
    case food
    case transport
    case budget
    case location
    case activity
    case generic
}

struct ChecklistItem: Codable, Equatable, Hashable, Sendable {
    var title: String
    var category: ChecklistCategory
    var priority: ChecklistPriority
}

enum ChecklistCategory: String, Codable, CaseIterable, Hashable, Sendable {
    case food
    case transport
    case venue
    case guests
    case equipment
    case budget
}

enum ChecklistPriority: String, Codable, CaseIterable, Hashable, Sendable {
    case high
    case medium
    case low
}

struct TransportHint: Codable, Equatable, Hashable, Sendable {
    var type: TransportHintType
    var label: String
    var description: String
}

enum TransportHintType: String, Codable, CaseIterable, Hashable, Sendable {
    case carpool
    case train
    case flight
    case meetingPoint
    case missingInfo
    case local
}

struct InvitationMessageSet: Codable, Equatable, Hashable, Sendable {
    var simple: String
    var warm: String
    var shortWhatsApp: String
}

struct EventSummary: Codable, Equatable, Hashable, Sendable {
    var decided: [String]
    var missing: [String]
    var recommendedNextAction: String
}

struct TransportCoordinationSuggestion: Codable, Equatable, Hashable, Sendable {
    var missingDetails: [String]
    var coordinationIdeas: [String]
    var groupMessageDraft: String
}

struct WakeveAIKnownFacts: Equatable, Sendable {
    var participantNames: Set<String> = []
    var voteLabels: Set<String> = []
    var availabilityLabels: Set<String> = []
    var addresses: Set<String> = []
    var priceLabels: Set<String> = []
    var transportLabels: Set<String> = []

    static let empty = WakeveAIKnownFacts()
}

struct WakeveAIGenerationRequest: Equatable, Sendable {
    var userInput: String
    var localeIdentifier: String
    var knownFacts: WakeveAIKnownFacts

    init(
        userInput: String,
        localeIdentifier: String = Locale.current.identifier,
        knownFacts: WakeveAIKnownFacts = .empty
    ) {
        self.userInput = userInput
        self.localeIdentifier = localeIdentifier
        self.knownFacts = knownFacts
    }
}

enum EventDraftSection: Equatable, Sendable {
    case title(String)
    case description(String)
    case dateOptions([DateOption])
    case checklist([ChecklistItem])
    case suggestedPolls([PollSuggestion])
    case completed(EventDraft)
    case failed(String)
}

struct SmartEventDraftState: Equatable {
    enum Phase: Equatable {
        case idle
        case checkingAvailability
        case unavailable(WakeveAIAvailability)
        case preparing
        case streaming
        case ready(EventDraft)
        case failed(String)
        case cancelled
    }

    var phrase: String = ""
    var phase: Phase = .idle
    var streamedTitle: String = ""
    var streamedDescription: String = ""
    var streamedDateOptions: [DateOption] = []
    var streamedChecklist: [ChecklistItem] = []
    var streamedPolls: [PollSuggestion] = []
    var metrics: WakeveAIMetrics?

    var canGenerate: Bool {
        phrase.trimmingCharacters(in: .whitespacesAndNewlines).count >= 6
    }
}

extension EventDraft {
    static let empty = EventDraft(
        title: "",
        subtitle: "",
        description: "",
        destinationName: "",
        locationHint: "",
        dateOptions: [],
        participantHints: [],
        suggestedPolls: [],
        checklist: [],
        transportHints: [],
        rationale: ""
    )
}
