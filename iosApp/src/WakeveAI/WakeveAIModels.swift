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

enum WakeveAIUseCase: String, Codable, Equatable, Sendable {
    case eventPlanDraft
    case eventSummary
    case organizerMessage
    case planningAgent
    case pollSuggestion
    case checklist
    case invitationMessage
    case transportSuggestion
    case suggestionRationale
}

enum WakeveAIInferenceRoute: String, Codable, Equatable, Sendable {
    case onDevice
    case cloud
    case localFallback
}

struct WakeveAIRoutingMetadata: Codable, Equatable, Sendable {
    var route: WakeveAIInferenceRoute
    var providerName: String
    var modelName: String?
    var cloudUsed: Bool
    var reason: String?

    init(
        route: WakeveAIInferenceRoute,
        providerName: String,
        modelName: String? = nil,
        cloudUsed: Bool = false,
        reason: String? = nil
    ) {
        self.route = route
        self.providerName = providerName
        self.modelName = modelName
        self.cloudUsed = cloudUsed
        self.reason = reason
    }
}

enum WakeveAIValidationStatus: String, Codable, Equatable, Sendable {
    case accepted
    case needsReview
    case rejected
}

struct WakeveAIValidationResult: Codable, Equatable, Sendable {
    var status: WakeveAIValidationStatus
    var issues: [String]

    static func accepted() -> WakeveAIValidationResult {
        WakeveAIValidationResult(status: .accepted, issues: [])
    }

    static func needsReview(_ issues: [String] = []) -> WakeveAIValidationResult {
        WakeveAIValidationResult(status: .needsReview, issues: issues)
    }

    static func rejected(_ issues: String...) -> WakeveAIValidationResult {
        WakeveAIValidationResult(status: .rejected, issues: issues)
    }
}

struct WakeveAICostEstimate: Codable, Equatable, Sendable {
    var known: Bool
    var amount: Double?
    var currencyCode: String?
    var inputUnits: Int?
    var outputUnits: Int?

    static func unknown() -> WakeveAICostEstimate {
        WakeveAICostEstimate(known: false, amount: nil, currencyCode: nil, inputUnits: nil, outputUnits: nil)
    }

    static func zeroOnDevice() -> WakeveAICostEstimate {
        WakeveAICostEstimate(known: true, amount: 0, currencyCode: "USD", inputUnits: nil, outputUnits: nil)
    }
}

struct WakeveAIInteractionMetadata: Codable, Equatable, Sendable {
    var useCase: WakeveAIUseCase
    var routing: WakeveAIRoutingMetadata
    var sanitizedInputSummary: String
    var sanitizedOutputSummary: String
    var confidence: Double
    var reasoningSummary: String
    var latencyMilliseconds: Int?
    var validation: WakeveAIValidationResult
    var cost: WakeveAICostEstimate

    static func fromMetrics(
        useCase: WakeveAIUseCase,
        promptId: String,
        availability: WakeveAIAvailability,
        sanitizedInputSummary: String,
        sanitizedOutputSummary: String,
        reasoningSummary: String,
        validation: WakeveAIValidationResult,
        metrics: WakeveAIMetrics?
    ) -> WakeveAIInteractionMetadata {
        let route: WakeveAIInferenceRoute = availability == .available ? .onDevice : .localFallback
        let routing = WakeveAIRoutingMetadata(
            route: route,
            providerName: route == .onDevice ? "Apple Foundation Models" : "Wakeve local fallback",
            modelName: route == .onDevice ? "SystemLanguageModel.default" : nil,
            cloudUsed: false,
            reason: availability.isAvailable ? nil : availability.discreetMessage
        )

        return WakeveAIInteractionMetadata(
            useCase: useCase,
            routing: routing,
            sanitizedInputSummary: sanitizedInputSummary.sanitizedMetadataText(fallback: "Input summary unavailable"),
            sanitizedOutputSummary: sanitizedOutputSummary.sanitizedMetadataText(fallback: "Output summary unavailable"),
            confidence: route == .onDevice ? 0.8 : 1.0,
            reasoningSummary: reasoningSummary.sanitizedMetadataText(fallback: "Reasoning summary unavailable"),
            latencyMilliseconds: metrics?.durationMilliseconds,
            validation: validation,
            cost: route == .onDevice ? .zeroOnDevice() : .zeroOnDevice()
        )
    }
}

enum WakeveAIInteractionMetadataPolicy {
    static func validate(_ metadata: WakeveAIInteractionMetadata?) -> WakeveAIValidationResult {
        guard let metadata else {
            return .rejected("missing_metadata")
        }

        var issues: [String] = []
        if !(0...1).contains(metadata.confidence) { issues.append("invalid_confidence") }
        if metadata.sanitizedInputSummary.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            issues.append("missing_input_summary")
        }
        if metadata.sanitizedOutputSummary.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            issues.append("missing_output_summary")
        }
        if metadata.reasoningSummary.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            issues.append("missing_reasoning_summary")
        }
        if let latency = metadata.latencyMilliseconds, latency < 0 {
            issues.append("invalid_latency")
        }
        if let amount = metadata.cost.amount, amount < 0 {
            issues.append("invalid_cost")
        }
        if let inputUnits = metadata.cost.inputUnits, inputUnits < 0 {
            issues.append("invalid_input_units")
        }
        if let outputUnits = metadata.cost.outputUnits, outputUnits < 0 {
            issues.append("invalid_output_units")
        }
        if metadata.validation.status == .rejected {
            issues.append("output_rejected")
        }
        issues.append(contentsOf: metadata.validation.issues)

        let uniqueIssues = Array(Set(issues)).sorted()
        if !uniqueIssues.isEmpty {
            return WakeveAIValidationResult(status: .rejected, issues: uniqueIssues)
        }
        return metadata.validation.status == .accepted ? .accepted() : .needsReview()
    }

    static func canExposeApplyAction(_ metadata: WakeveAIInteractionMetadata?) -> Bool {
        validate(metadata).status != .rejected
    }
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
    var metadata: WakeveAIInteractionMetadata?

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

private extension String {
    func sanitizedMetadataText(fallback: String) -> String {
        let trimmed = trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return fallback }
        return String(trimmed.prefix(240))
    }
}
