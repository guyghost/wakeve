import XCTest
@testable import Wakeve

final class WakeveAIValidationTests: XCTestCase {
    func testInteractionMetadataValidationAndApplyPolicy() {
        let metadata = WakeveAIInteractionMetadata(
            useCase: .eventPlanDraft,
            routing: WakeveAIRoutingMetadata(route: .onDevice, providerName: "Foundation Models", modelName: "system", cloudUsed: false),
            sanitizedInputSummary: "Event draft phrase",
            sanitizedOutputSummary: "Structured event draft",
            confidence: 0.8,
            reasoningSummary: "Generated typed draft for user review.",
            latencyMilliseconds: 120,
            validation: .needsReview(),
            cost: .zeroOnDevice()
        )

        XCTAssertEqual(WakeveAIInteractionMetadataPolicy.validate(metadata).status, .needsReview)
        XCTAssertTrue(WakeveAIInteractionMetadataPolicy.canExposeApplyAction(metadata))

        let invalid = WakeveAIInteractionMetadata(
            useCase: .eventPlanDraft,
            routing: metadata.routing,
            sanitizedInputSummary: "",
            sanitizedOutputSummary: "",
            confidence: 2,
            reasoningSummary: "",
            latencyMilliseconds: -1,
            validation: .accepted(),
            cost: WakeveAICostEstimate(
                known: true,
                amount: -0.01,
                currencyCode: "USD",
                inputUnits: -1,
                outputUnits: -2
            )
        )

        let invalidValidation = WakeveAIInteractionMetadataPolicy.validate(invalid)
        XCTAssertEqual(invalidValidation.status, .rejected)
        XCTAssertTrue(invalidValidation.issues.contains("invalid_confidence"))
        XCTAssertTrue(invalidValidation.issues.contains("missing_input_summary"))
        XCTAssertTrue(invalidValidation.issues.contains("missing_output_summary"))
        XCTAssertTrue(invalidValidation.issues.contains("missing_reasoning_summary"))
        XCTAssertTrue(invalidValidation.issues.contains("invalid_latency"))
        XCTAssertTrue(invalidValidation.issues.contains("invalid_cost"))
        XCTAssertTrue(invalidValidation.issues.contains("invalid_input_units"))
        XCTAssertTrue(invalidValidation.issues.contains("invalid_output_units"))

        let rejected = WakeveAIInteractionMetadata(
            useCase: .eventPlanDraft,
            routing: metadata.routing,
            sanitizedInputSummary: "Event draft phrase",
            sanitizedOutputSummary: "Invented participant",
            confidence: 0.4,
            reasoningSummary: "Validation rejected output.",
            latencyMilliseconds: 10,
            validation: .rejected("invented_participant"),
            cost: .zeroOnDevice()
        )

        XCTAssertFalse(WakeveAIInteractionMetadataPolicy.canExposeApplyAction(rejected))
    }

    func testMetadataFromMetricsUsesSanitizedDisclosureAndLatency() {
        let started = Date()
        let completed = started.addingTimeInterval(0.25)
        let metrics = WakeveAIMetrics(
            promptId: "event_draft_v1",
            availability: .available,
            startedAt: started,
            completedAt: completed,
            timedOut: false,
            cancelled: false,
            validationIssues: []
        )

        let metadata = WakeveAIInteractionMetadata.fromMetrics(
            useCase: .eventPlanDraft,
            promptId: "event_draft_v1",
            availability: .available,
            sanitizedInputSummary: "Smart event draft phrase",
            sanitizedOutputSummary: "Structured event draft",
            reasoningSummary: "Generated typed event draft for explicit user review.",
            validation: .needsReview(),
            metrics: metrics
        )

        XCTAssertEqual(metadata.routing.route, .onDevice)
        XCTAssertEqual(metadata.latencyMilliseconds, 250)
        XCTAssertEqual(metadata.validation.status, .needsReview)
        XCTAssertFalse(metadata.sanitizedInputSummary.contains("Alice"))
        XCTAssertTrue(WakeveAIInteractionMetadataPolicy.canExposeApplyAction(metadata))
    }

    func testValidationCoversInitialWakeveAIOutputTypes() {
        let draft = EventDraft(
            title: "Fête plage",
            subtitle: "Famille",
            description: "Moment simple",
            destinationName: "Plage",
            locationHint: "Lieu à confirmer",
            dateOptions: [
                DateOption(label: "Samedi", startDateHint: "samedi", endDateHint: "", confidence: 0.7, reason: "Mentionné")
            ],
            participantHints: ["Famille"],
            suggestedPolls: [
                PollSuggestion(question: "Quelle date ?", options: ["Samedi", "Dimanche"], pollType: .date)
            ],
            checklist: [
                ChecklistItem(title: "Prévoir le repas", category: .food, priority: .high)
            ],
            transportHints: [
                TransportHint(type: .meetingPoint, label: "Départs", description: "Demander les villes de départ")
            ],
            rationale: "Phrase structurée"
        )
        let messages = InvitationMessageSet(
            simple: "On prépare l'événement, vous êtes partants ?",
            warm: "J'aimerais vous réunir pour ce moment.",
            shortWhatsApp: "Partants ? Je vous envoie les options."
        )
        let summary = EventSummary(
            decided: ["Titre choisi"],
            missing: ["Date à confirmer"],
            recommendedNextAction: "Lancer un sondage simple."
        )
        let transport = TransportCoordinationSuggestion(
            missingDetails: ["Ville de départ"],
            coordinationIdeas: ["Regrouper par ville"],
            groupMessageDraft: "Ajoutez votre ville de départ pour coordonner les trajets."
        )

        XCTAssertTrue(WakeveAIValidator.validate(draft).isEmpty)
        XCTAssertTrue(WakeveAIValidator.validate(messages).isEmpty)
        XCTAssertTrue(WakeveAIValidator.validate(summary).isEmpty)
        XCTAssertTrue(WakeveAIValidator.validate(transport).isEmpty)
    }

    func testEventDraftValidationCapsGeneratedCollections() {
        let draft = EventDraft(
            title: "Week-end plage",
            subtitle: "Dakar",
            description: "Barbecue et musique",
            destinationName: "Dakar",
            locationHint: "Plage",
            dateOptions: [
                DateOption(label: "A", startDateHint: "A", endDateHint: "", confidence: 0.5, reason: "A"),
                DateOption(label: "B", startDateHint: "B", endDateHint: "", confidence: 0.5, reason: "B"),
                DateOption(label: "C", startDateHint: "C", endDateHint: "", confidence: 0.5, reason: "C"),
                DateOption(label: "D", startDateHint: "D", endDateHint: "", confidence: 0.5, reason: "D")
            ],
            participantHints: ["A", "B", "C", "D"],
            suggestedPolls: [
                PollSuggestion(question: "Q1", options: ["A"], pollType: .generic),
                PollSuggestion(question: "Q2", options: ["A"], pollType: .generic),
                PollSuggestion(question: "Q3", options: ["A"], pollType: .generic),
                PollSuggestion(question: "Q4", options: ["A"], pollType: .generic)
            ],
            checklist: [],
            transportHints: [
                TransportHint(type: .local, label: "A", description: "A"),
                TransportHint(type: .local, label: "B", description: "B"),
                TransportHint(type: .local, label: "C", description: "C"),
                TransportHint(type: .local, label: "D", description: "D")
            ],
            rationale: "Test"
        )

        XCTAssertTrue(WakeveAIValidator.validate(draft).contains(.tooManyItems))

        let sanitized = WakeveAIValidator.sanitized(draft)
        XCTAssertEqual(sanitized.dateOptions.count, 3)
        XCTAssertEqual(sanitized.participantHints.count, 3)
        XCTAssertEqual(sanitized.suggestedPolls.count, 3)
        XCTAssertEqual(sanitized.transportHints.count, 3)
    }

    func testInvitationSummaryAndTransportValidationRejectsUnsafeOutput() {
        let messages = InvitationMessageSet(
            simple: "",
            warm: String(repeating: "a", count: 421),
            shortWhatsApp: "participant:Emma vient avec address:12 rue test"
        )
        let summary = EventSummary(
            decided: ["A", "B", "C", "D"],
            missing: [],
            recommendedNextAction: "price:60 déjà confirmé"
        )
        let transport = TransportCoordinationSuggestion(
            missingDetails: ["A", "B", "C", "D"],
            coordinationIdeas: ["transport:TGV 123"],
            groupMessageDraft: ""
        )

        XCTAssertTrue(WakeveAIValidator.validate(messages).contains(.emptyRequiredText))
        XCTAssertTrue(WakeveAIValidator.validate(messages).contains(.overlongText))
        XCTAssertTrue(WakeveAIValidator.validate(messages).contains(.inventedParticipant))
        XCTAssertTrue(WakeveAIValidator.validate(messages).contains(.inventedAddress))
        XCTAssertTrue(WakeveAIValidator.validate(summary).contains(.tooManyItems))
        XCTAssertTrue(WakeveAIValidator.validate(summary).contains(.inventedPrice))
        XCTAssertTrue(WakeveAIValidator.validate(transport).contains(.emptyRequiredText))
        XCTAssertTrue(WakeveAIValidator.validate(transport).contains(.tooManyItems))
        XCTAssertTrue(WakeveAIValidator.validate(transport).contains(.inventedTransportFact))
    }

    func testValidationRejectsInventedParticipantVoteAddressPriceAndTransportFacts() {
        let draft = EventDraft(
            title: "Resume",
            subtitle: "",
            description: "participant:Emma vote:Oui address:12 rue test price:50 transport:TGV 123",
            destinationName: "",
            locationHint: "",
            dateOptions: [],
            participantHints: [],
            suggestedPolls: [],
            checklist: [],
            transportHints: [],
            rationale: "Test"
        )

        let issues = Set(WakeveAIValidator.validate(draft, knownFacts: .empty))
        XCTAssertTrue(issues.contains(.inventedParticipant))
        XCTAssertTrue(issues.contains(.inventedVote))
        XCTAssertTrue(issues.contains(.inventedAddress))
        XCTAssertTrue(issues.contains(.inventedPrice))
        XCTAssertTrue(issues.contains(.inventedTransportFact))
    }

    func testKnownFactsAllowGroundedBusinessFacts() {
        let draft = EventDraft(
            title: "Resume",
            subtitle: "",
            description: "participant:Emma vote:Samedi address:Plage price:20 transport:Train",
            destinationName: "",
            locationHint: "",
            dateOptions: [],
            participantHints: [],
            suggestedPolls: [],
            checklist: [],
            transportHints: [],
            rationale: "Test"
        )

        let facts = WakeveAIKnownFacts(
            participantNames: ["Emma"],
            voteLabels: ["Samedi"],
            addresses: ["Plage"],
            priceLabels: ["20"],
            transportLabels: ["Train"]
        )

        let issues = WakeveAIValidator.validate(draft, knownFacts: facts)
        XCTAssertFalse(issues.contains(.inventedParticipant))
        XCTAssertFalse(issues.contains(.inventedVote))
        XCTAssertFalse(issues.contains(.inventedAddress))
        XCTAssertFalse(issues.contains(.inventedPrice))
        XCTAssertFalse(issues.contains(.inventedTransportFact))
    }

    func testAvailabilityMappingCoversKnownUnavailableStates() {
        XCTAssertEqual(FixedWakeveAIAvailabilityService(availability: .available).currentAvailability(), .available)
        XCTAssertEqual(WakeveAIAvailabilityService.mapUnavailableReason("appleIntelligenceNotEnabled"), .appleIntelligenceDisabled)
        XCTAssertEqual(WakeveAIAvailabilityService.mapUnavailableReason("model assets downloading"), .notReady)
        XCTAssertEqual(WakeveAIAvailabilityService.mapUnavailableReason("deviceNotSupported"), .unsupportedDevice)
        XCTAssertEqual(WakeveAIAvailabilityService.mapUnavailableReason("unexpected reason"), .unknownUnavailable("unexpected reason"))
    }

    func testDebugAvailabilityOverrideMapsSimulatorFallbackStates() {
        #if DEBUG
        XCTAssertEqual(
            WakeveAIAvailabilityService.debugAvailabilityOverride(environment: ["WAKEVE_AI_AVAILABILITY_OVERRIDE": "unsupported"]),
            .unsupportedDevice
        )
        XCTAssertEqual(
            WakeveAIAvailabilityService.debugAvailabilityOverride(arguments: ["Wakeve", "--wakeve-ai-availability=disabled"], environment: [:]),
            .appleIntelligenceDisabled
        )
        XCTAssertEqual(
            WakeveAIAvailabilityService.debugAvailabilityOverride(environment: ["WAKEVE_AI_AVAILABILITY_OVERRIDE": "not_ready"]),
            .notReady
        )
        XCTAssertEqual(
            WakeveAIAvailabilityService.debugAvailabilityOverride(environment: ["WAKEVE_AI_AVAILABILITY_OVERRIDE": "available"]),
            .available
        )
        XCTAssertEqual(
            WakeveAIAvailabilityService.debugAvailabilityOverride(environment: ["WAKEVE_AI_AVAILABILITY_OVERRIDE": "unknown"]),
            .unknownUnavailable("debug_override")
        )
        XCTAssertNil(WakeveAIAvailabilityService.debugAvailabilityOverride(environment: ["WAKEVE_AI_AVAILABILITY_OVERRIDE": "invalid"]))
        #endif
    }
}
