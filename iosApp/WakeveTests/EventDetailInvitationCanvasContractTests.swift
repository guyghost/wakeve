import XCTest
@testable import Wakeve
import Shared

final class EventDetailInvitationCanvasContractTests: XCTestCase {
    func testMapperProjectsEveryLifecycleAccessPairToOneExpectedModeAndAction() {
        let allActions: Set<EventDetailInvitationCanvasAction> = [
            .editDraft,
            .submitVote,
            .viewPollResults,
            .compareOptions,
            .continueOrganization,
            .viewFinalDetails,
            .showAccessState,
            .showDetails
        ]
        let scenarios: [(
            status: EventStatus,
            access: EventDetailInvitationCanvasAccess,
            mode: EventDetailInvitationCanvasMode,
            action: EventDetailInvitationCanvasAction
        )] = [
            (.draft, .organizer, .draftOrganizerReady, .editDraft),
            (.draft, .eligibleParticipant, .draftParticipantReadOnly, .showAccessState),
            (.draft, .restrictedParticipant, .draftRestricted, .showAccessState),
            (.draft, .nonParticipant, .draftRestricted, .showAccessState),
            (.polling, .organizer, .pollingOrganizer, .viewPollResults),
            (.polling, .eligibleParticipant, .pollingResponseDue, .submitVote),
            (.polling, .restrictedParticipant, .pollingRestricted, .showAccessState),
            (.polling, .nonParticipant, .pollingRestricted, .showAccessState),
            (.confirmed, .organizer, .confirmedOrganizer, .compareOptions),
            (.confirmed, .eligibleParticipant, .confirmedParticipant, .compareOptions),
            (.confirmed, .restrictedParticipant, .confirmedRestricted, .showAccessState),
            (.confirmed, .nonParticipant, .confirmedRestricted, .showAccessState),
            (.comparing, .organizer, .comparingOrganizer, .compareOptions),
            (.comparing, .eligibleParticipant, .comparingParticipant, .compareOptions),
            (.comparing, .restrictedParticipant, .comparingRestricted, .showAccessState),
            (.comparing, .nonParticipant, .comparingRestricted, .showAccessState),
            (.organizing, .organizer, .organizingOrganizer, .continueOrganization),
            (.organizing, .eligibleParticipant, .organizingParticipant, .continueOrganization),
            (.organizing, .restrictedParticipant, .organizingRestricted, .showAccessState),
            (.organizing, .nonParticipant, .organizingRestricted, .showAccessState),
            (.finalized, .organizer, .finalizedReadOnly, .viewFinalDetails),
            (.finalized, .eligibleParticipant, .finalizedReadOnly, .viewFinalDetails),
            (.finalized, .restrictedParticipant, .finalizedRestrictedReadOnly, .showAccessState),
            (.finalized, .nonParticipant, .finalizedRestrictedReadOnly, .showAccessState)
        ]

        XCTAssertEqual(scenarios.count, 24)

        for scenario in scenarios {
            let presentation = EventDetailInvitationCanvasMapper().map(
                makeInput(
                    eventStatus: scenario.status,
                    access: scenario.access,
                    availableActions: allActions
                )
            )

            XCTAssertEqual(
                presentation.canvasMode,
                scenario.mode,
                "Unexpected mode for \(scenario.status) × \(scenario.access)."
            )
            XCTAssertEqual(
                presentation.primaryAction,
                scenario.action,
                "Unexpected primary action for \(scenario.status) × \(scenario.access)."
            )
        }
    }

    func testMapperProjectsDraftAndPollingSubconditionsDeterministically() {
        let mapper = EventDetailInvitationCanvasMapper()

        let blockedDraft = mapper.map(
            makeInput(
                eventStatus: .draft,
                hasRequiredSlots: false,
                access: .organizer,
                availableActions: [.editDraft]
            )
        )
        XCTAssertEqual(blockedDraft.canvasMode, .draftOrganizerBlocked)
        XCTAssertEqual(blockedDraft.primaryAction, .editDraft)

        let submittedVote = mapper.map(
            makeInput(
                eventStatus: .polling,
                access: .eligibleParticipant,
                vote: .submitted,
                availableActions: [.viewPollResults]
            )
        )
        XCTAssertEqual(submittedVote.canvasMode, .pollingResponseSubmitted)
        XCTAssertEqual(submittedVote.primaryAction, .viewPollResults)

        for vote in [
            EventDetailInvitationCanvasVoteState.notApplicable,
            .unavailable
        ] {
            let neutralVote = mapper.map(
                makeInput(
                    eventStatus: .polling,
                    access: .eligibleParticipant,
                    vote: vote,
                    availableActions: [.viewPollResults]
                )
            )
            XCTAssertEqual(neutralVote.canvasMode, .pollingParticipantNeutral)
            XCTAssertEqual(neutralVote.primaryAction, .viewPollResults)
        }
    }

    func testMapperFallsBackToShowDetailsWhenAuthorizedActionIsUnavailable() {
        let scenarios: [(EventStatus, EventDetailInvitationCanvasAccess)] = [
            (.draft, .organizer),
            (.polling, .eligibleParticipant),
            (.confirmed, .organizer),
            (.comparing, .eligibleParticipant),
            (.organizing, .organizer),
            (.finalized, .eligibleParticipant)
        ]

        for (status, access) in scenarios {
            let presentation = EventDetailInvitationCanvasMapper().map(
                makeInput(
                    eventStatus: status,
                    access: access,
                    availableActions: []
                )
            )

            XCTAssertEqual(
                presentation.primaryAction,
                .showDetails,
                "Missing authorized action must fail closed for \(status) × \(access)."
            )
        }
    }

    func testFinalizedMapperRejectsEveryMutatingAction() {
        let mutatingActions: Set<EventDetailInvitationCanvasAction> = [
            .editDraft,
            .submitVote,
            .compareOptions,
            .continueOrganization
        ]

        for access in [
            EventDetailInvitationCanvasAccess.organizer,
            .eligibleParticipant,
            .restrictedParticipant,
            .nonParticipant
        ] {
            let presentation = EventDetailInvitationCanvasMapper().map(
                makeInput(
                    eventStatus: .finalized,
                    access: access,
                    availableActions: mutatingActions
                )
            )

            XCTAssertEqual(presentation.primaryAction, .showDetails)
            XCTAssertTrue(
                EventDetailInvitationCanvasMapper.finalizedAllowedActions.contains(
                    presentation.primaryAction
                )
            )
        }
    }

    func testOrganizingActionFailsClosedWithoutAvailableReadinessData() {
        let mapper = EventDetailInvitationCanvasMapper()

        for readiness in [
            EventDetailInvitationCanvasReadinessData.loading,
            .unavailable,
            .failed
        ] {
            let presentation = mapper.map(
                makeInput(
                    eventStatus: .organizing,
                    access: .organizer,
                    readinessData: readiness,
                    availableActions: [.continueOrganization]
                )
            )

            XCTAssertEqual(
                presentation.primaryAction,
                .showDetails,
                "An organizing action requires available structured readiness data."
            )
        }

        let availableReadiness = mapper.map(
            makeInput(
                eventStatus: .organizing,
                access: .organizer,
                readinessData: .available(
                    items: [
                        EventDetailInvitationCanvasReadinessItem(
                            id: "transport",
                            isComplete: false,
                            destination: .continueOrganization
                        )
                    ]
                ),
                availableActions: [.continueOrganization]
            )
        )
        XCTAssertEqual(availableReadiness.primaryAction, .continueOrganization)
    }

    func testMapperFiltersPrivateParticipantResponsibilityAndShareDataByAccess() {
        let identity = EventDetailInvitationCanvasParticipantIdentity(
            id: "participant-1",
            initials: "AL",
            accessibilityName: "Alex",
            status: .confirmed
        )
        let participantData = EventDetailInvitationCanvasParticipantData.available(
            EventDetailInvitationCanvasParticipantSnapshot(
                confirmedCount: 1,
                pendingCount: 0,
                identities: [identity]
            )
        )
        let sharePayload = EventDetailInvitationServerIssuedSharePayload(
            opaqueValue: "server-owned-reference"
        )

        let organizer = EventDetailInvitationCanvasMapper().map(
            makeInput(
                eventStatus: .confirmed,
                access: .organizer,
                participantData: participantData,
                responsibility: .currentUser,
                availableActions: [.compareOptions],
                shareCapability: .ready(serverIssuedPayload: sharePayload)
            )
        )
        XCTAssertEqual(organizer.participantData, participantData)
        XCTAssertEqual(organizer.responsibleActor, .currentUser)
        XCTAssertEqual(
            organizer.shareCapability,
            .ready(serverIssuedPayload: sharePayload)
        )

        for access in [
            EventDetailInvitationCanvasAccess.restrictedParticipant,
            .nonParticipant
        ] {
            let restricted = EventDetailInvitationCanvasMapper().map(
                makeInput(
                    eventStatus: .confirmed,
                    access: access,
                    participantData: participantData,
                    responsibility: .currentUser,
                    availableActions: [.showAccessState],
                    shareCapability: .ready(serverIssuedPayload: sharePayload)
                )
            )
            XCTAssertEqual(restricted.participantData, .unavailable)
            XCTAssertEqual(restricted.responsibleActor, .none)
            XCTAssertEqual(restricted.shareCapability, .hidden)
        }
    }

    func testParticipantSnapshotReconcilesCountsWithTheSameIdentitySnapshot() {
        let identities = [
            EventDetailInvitationCanvasParticipantIdentity(
                id: "confirmed",
                initials: "CO",
                accessibilityName: "Confirmed",
                status: .confirmed
            ),
            EventDetailInvitationCanvasParticipantIdentity(
                id: "pending",
                initials: "PE",
                accessibilityName: "Pending",
                status: .pending
            )
        ]

        let snapshot = EventDetailInvitationCanvasParticipantSnapshot(
            confirmedCount: 99,
            pendingCount: 42,
            identities: identities
        )

        XCTAssertEqual(snapshot.confirmedCount, 1)
        XCTAssertEqual(snapshot.pendingCount, 1)
        XCTAssertEqual(snapshot.identities, identities)
    }

    func testPendingSyncNeverProjectsAsSyncedAndPreservesItsTypedSubject() {
        let participantSubject = EventDetailInvitationCanvasSyncSubject.participantResponse(
            id: "participant-vote"
        )
        let pendingVote = EventDetailInvitationCanvasMapper().map(
            makeInput(
                eventStatus: .polling,
                access: .eligibleParticipant,
                availableActions: [.submitVote],
                relevantSync: .pending(subject: participantSubject)
            )
        )
        XCTAssertEqual(
            pendingVote.syncDecoration,
            .pending(subject: participantSubject)
        )
        XCTAssertNotEqual(
            pendingVote.syncDecoration,
            .synced(subject: participantSubject)
        )

        let eventSubject = EventDetailInvitationCanvasSyncSubject.event(id: "event-1")
        let pendingEvent = EventDetailInvitationCanvasMapper().map(
            makeInput(
                eventStatus: .confirmed,
                access: .organizer,
                availableActions: [.showDetails],
                relevantSync: .pending(subject: eventSubject)
            )
        )
        XCTAssertEqual(pendingEvent.syncDecoration, .pending(subject: eventSubject))
    }

    func testFinalizedPresentationHidesAIAndKeepsReadableDetails() throws {
        let requestedSections: Set<EventDetailInvitationCanvasSecondarySection> = [
            .aiReview,
            .participants,
            .organizationDetails
        ]
        let presentation = EventDetailInvitationCanvasMapper().map(
            makeInput(
                eventStatus: .finalized,
                access: .organizer,
                availableActions: [.viewFinalDetails],
                requestedSecondarySections: requestedSections
            )
        )

        XCTAssertFalse(
            presentation.visibleSecondarySections.contains(.aiReview),
            "FINALIZED must not expose mutable AI suggestion review."
        )
        XCTAssertTrue(presentation.visibleSecondarySections.contains(.participants))
        XCTAssertTrue(presentation.visibleSecondarySections.contains(.organizationDetails))

        let source = try readProjectFile(
            "iosApp/src/Views/Events/EventDetailInvitationCanvas.swift"
        )
        XCTAssertTrue(source.contains("enum EventDetailInvitationCanvasInteractionPolicy"))
        XCTAssertTrue(source.contains("case interactive"))
        XCTAssertTrue(source.contains("case readOnly"))
        XCTAssertTrue(
            source.contains("interactionPolicy: EventDetailInvitationCanvasInteractionPolicy")
        )
    }

    func testFinalizedReadableSectionsConsumeExplicitReadOnlyInteractionPolicy() throws {
        let detailSource = try eventDetailSource()
        let contentSource = try readProjectFile("iosApp/src/Views/App/ContentView.swift")
        let participantsComponent = slice(
            contentSource,
            from: "private struct EventDetailParticipantsPreview",
            to: "private struct EventDetailSectionCard"
        )
        let actionRowComponent = slice(
            contentSource,
            from: "private struct EventDetailActionRow",
            to: "private struct EventDetailMessagePreview"
        )

        XCTAssertTrue(
            detailSource.contains(
                "participantsPreview(interactionPolicy: presentation.interactionPolicy)"
            )
        )
        XCTAssertTrue(
            detailSource.contains(
                "detailRows(interactionPolicy: presentation.interactionPolicy)"
            )
        )

        for component in [participantsComponent, actionRowComponent] {
            XCTAssertTrue(
                component.contains(
                    "let interactionPolicy: EventDetailInvitationCanvasInteractionPolicy"
                )
            )
            XCTAssertTrue(
                component.contains(".disabled(interactionPolicy == .readOnly)") ||
                    component.contains("if interactionPolicy == .interactive") ||
                    component.contains(".allowsHitTesting(interactionPolicy == .interactive)"),
                "Readable FINALIZED content must explicitly neutralize its interaction."
            )
        }
    }

    func testFinalizedMenuUsesReadOnlyPolicyAndKeepsOnlyReportAndSupport() throws {
        let detailSource = try eventDetailSource()
        let menuFactory = slice(
            detailSource,
            from: "private func canvasMenuActions",
            to: "private func performCanvasAction"
        )
        let readOnlyBranch = slice(
            menuFactory,
            from: "interactionPolicy == .readOnly",
            to: "return actions"
        )

        XCTAssertTrue(
            detailSource.contains("interactionPolicy: presentation.interactionPolicy")
        )
        XCTAssertTrue(
            menuFactory.contains(
                "interactionPolicy: EventDetailInvitationCanvasInteractionPolicy"
            )
        )
        XCTAssertTrue(menuFactory.contains("interactionPolicy == .readOnly"))
        XCTAssertTrue(readOnlyBranch.contains("id: \"reportEventAction\""))
        XCTAssertTrue(readOnlyBranch.contains("id: \"support\""))

        for forbidden in [
            "onManageParticipants",
            "onVote",
            "onViewResults",
            "onOrganize",
            "onOpenTransport",
            "onOpenMeetings",
            "onOpenBudget",
            "onOpenPayment",
            "onOpenTricount",
            "onOpenComments",
            "onOpenPhotos"
        ] {
            XCTAssertFalse(
                readOnlyBranch.contains(forbidden),
                "FINALIZED menu must not retain business callback \(forbidden)."
            )
        }
    }

    func testFinalizedDetailsActionsScrollLocallyWithoutOpeningMutableRoutes() throws {
        let detailSource = try eventDetailSource()
        let actionHandler = slice(
            detailSource,
            from: "private func performCanvasAction",
            to: "private func scrollToProgressiveDetails"
        )
        let localDetailsBranch = slice(
            actionHandler,
            from: "case .viewFinalDetails, .showDetails:",
            to: "}"
        )

        XCTAssertTrue(localDetailsBranch.contains("scrollToProgressiveDetails(scrollProxy)"))
        for mutatingRoute in [
            "onManageParticipants",
            "onVote",
            "onViewResults",
            "onOrganize",
            "onOpenBudget",
            "onOpenComments"
        ] {
            XCTAssertFalse(localDetailsBranch.contains(mutatingRoute))
        }
    }

    func testPresentationProjectionCoversEveryLifecycleAccessPairAndExclusiveMode() throws {
        let source = try readIOSProductionSources()
        let compactSource = source.filter { !$0.isWhitespace }

        XCTAssertTrue(source.contains("enum EventDetailInvitationCanvasMode"))
        XCTAssertTrue(source.contains("enum EventDetailInvitationCanvasAccess"))
        XCTAssertTrue(source.contains("struct EventDetailInvitationCanvasPresentation"))
        XCTAssertTrue(source.contains("EventDetailInvitationCanvasMapper"))

        let lifecycleCases = ["draft", "polling", "confirmed", "comparing", "organizing", "finalized"]
        let accessCases = ["organizer", "eligibleParticipant", "restrictedParticipant", "nonParticipant"]

        for lifecycle in lifecycleCases {
            for access in accessCases {
                XCTAssertTrue(
                    compactSource.contains("(.\(lifecycle),.\(access))"),
                    "The total mapper must explicitly cover \(lifecycle) × \(access)."
                )
            }
        }

        let expectedModes = [
            "draftOrganizerBlocked",
            "draftOrganizerReady",
            "draftParticipantReadOnly",
            "draftRestricted",
            "pollingOrganizer",
            "pollingResponseDue",
            "pollingResponseSubmitted",
            "pollingParticipantNeutral",
            "pollingRestricted",
            "confirmedOrganizer",
            "confirmedParticipant",
            "confirmedRestricted",
            "comparingOrganizer",
            "comparingParticipant",
            "comparingRestricted",
            "organizingOrganizer",
            "organizingParticipant",
            "organizingRestricted",
            "finalizedReadOnly",
            "finalizedRestrictedReadOnly"
        ]

        for mode in expectedModes {
            XCTAssertTrue(source.contains("case \(mode)"), "Missing exclusive canvas mode: \(mode).")
        }
    }

    func testCanvasActionsAreTypedNavigationRequestsWithShowDetailsFallback() throws {
        let source = try readIOSProductionSources()

        XCTAssertTrue(source.contains("enum EventDetailInvitationCanvasAction"))

        for action in [
            "editDraft",
            "submitVote",
            "viewPollResults",
            "compareOptions",
            "continueOrganization",
            "viewFinalDetails",
            "showAccessState",
            "showDetails"
        ] {
            XCTAssertTrue(source.contains("case \(action)"), "Missing typed canvas action: \(action).")
        }

        XCTAssertTrue(
            source.contains("primaryAction: EventDetailInvitationCanvasAction"),
            "The presentation must expose exactly one typed navigation/details action."
        )
        XCTAssertTrue(
            source.contains("?? .showDetails") || source.contains("default: .showDetails"),
            "SHOW_DETAILS must be the deterministic, non-mutating fallback."
        )
    }

    func testFinalizedActionPolicyIsExplicitlyReadOnly() throws {
        let source = try readIOSProductionSources()
        let finalizedPolicy = bracketedDeclaration(named: "finalizedAllowedActions", in: source)

        XCTAssertFalse(finalizedPolicy.isEmpty, "The mapper must declare its FINALIZED action allow-list.")
        XCTAssertTrue(finalizedPolicy.contains(".viewFinalDetails"))
        XCTAssertTrue(finalizedPolicy.contains(".showAccessState"))
        XCTAssertTrue(finalizedPolicy.contains(".showDetails"))

        for forbiddenAction in [".editDraft", ".submitVote", ".compareOptions", ".continueOrganization"] {
            XCTAssertFalse(
                finalizedPolicy.contains(forbiddenAction),
                "FINALIZED must never expose mutating action \(forbiddenAction)."
            )
        }
    }

    func testInvitationCanvasShareConsumesOnlyTypedServerIssuedCapability() throws {
        let source = try readIOSProductionSources()
        let detailSource = try eventDetailSource()
        let canvasImplementation = sources(containing: "struct EventDetailInvitationCanvas: View", in: source)

        XCTAssertTrue(source.contains("enum EventDetailInvitationShareCapability"))
        for state in ["hidden", "requesting", "ready", "unavailable", "failed"] {
            XCTAssertTrue(source.contains("case \(state)"), "Missing secure-share capability state: \(state).")
        }
        XCTAssertTrue(source.contains("serverIssuedPayload"))
        XCTAssertTrue(canvasImplementation.contains("onShare"))
        XCTAssertFalse(canvasImplementation.contains("InvitationTokenCodec"))
        XCTAssertFalse(detailSource.contains("InvitationTokenCodec"))
        XCTAssertFalse(detailSource.contains("https://wakeve.app/invite/"))
    }

    func testInvitationCanvasLeadsEventDetailAndExistingSectionsRemainProgressive() throws {
        let detailSource = try eventDetailSource()
        let expectedOrder = [
            "EventDetailInvitationCanvas(",
            "invitationLandingCard",
            "metadataOverview",
            "EventWeatherMapCard(state: eventWeatherViewModel.state)",
            "anticipationPanel",
            "eventAISuggestionPanel",
            "groupReadinessPanel",
            "participantsPreview",
            "detailRows",
            "messagePreview"
        ]

        assertAppearInOrder(expectedOrder, in: detailSource)

        for preservedDestination in [
            "onOpenTransport",
            "onOpenMeetings",
            "onOpenBudget",
            "onOpenPayment",
            "onOpenTricount",
            "onOpenAccommodation",
            "onOpenMeals",
            "onOpenEquipment",
            "onOpenActivities",
            "onOpenComments",
            "onOpenPhotos",
            "onOpenInvitationShare"
        ] {
            XCTAssertTrue(
                detailSource.contains(preservedDestination),
                "The invitation canvas refactor must preserve \(preservedDestination)."
            )
        }
    }

    func testInvitationLandingIsInformationalAndDoesNotCompeteWithCanvasAction() throws {
        let detailSource = try eventDetailSource()
        let invitationLanding = slice(
            detailSource,
            from: "private var invitationLandingCard",
            to: "private var anticipationPanel"
        )

        XCTAssertFalse(invitationLanding.contains("WakeveActionButton("))
        XCTAssertFalse(invitationLanding.contains("onVote()"))
        XCTAssertFalse(invitationLanding.contains("onManageParticipants()"))
        XCTAssertFalse(invitationLanding.contains("eventInvitationLandingPrimaryAction"))
    }

    func testEventDetailConsumesPureContextProjectionInsteadOfDecidingAccessAndActions() throws {
        let source = try readIOSProductionSources()
        let detailSource = try eventDetailSource()
        let contextMapper = sources(
            containing: "struct EventDetailInvitationCanvasContextMapper",
            in: source
        )

        XCTAssertFalse(
            contextMapper.isEmpty,
            "A pure context mapper must own access, vote, responsibility, actions, and sync projection."
        )
        for output in [
            "currentUserAccess",
            "currentUserVote",
            "participantData",
            "responsibility",
            "availableActions",
            "relevantSync"
        ] {
            XCTAssertTrue(contextMapper.contains(output), "Context mapper is missing \(output).")
        }
        XCTAssertTrue(contextMapper.contains("ParticipantAccessMapper.shared.fromRepositoryRecord"))
        XCTAssertTrue(contextMapper.contains("canAccessOrganizationDetails"))
        XCTAssertFalse(contextMapper.contains("LanguageModelSession("))

        for viewOwnedDecision in [
            "private var canvasAccess",
            "private var canvasVoteState",
            "private var canvasResponsibility",
            "private var availableCanvasActions",
            "private var canvasRelevantSync"
        ] {
            XCTAssertFalse(
                detailSource.contains(viewOwnedDecision),
                "EventDetailView must consume the pure context projection, not own \(viewOwnedDecision)."
            )
        }
        XCTAssertFalse(detailSource.contains("rsvp == \"DECLINED\""))
        XCTAssertTrue(detailSource.contains("EventDetailInvitationCanvasContextMapper"))
    }

    func testVoteSubmissionRequiresVotesForEveryProposedSlot() throws {
        let source = try readIOSProductionSources()
        let detailSource = try eventDetailSource()
        let contextMapper = sources(
            containing: "struct EventDetailInvitationCanvasContextMapper",
            in: source
        )

        XCTAssertTrue(contextMapper.contains("proposedSlotIDs"))
        XCTAssertTrue(contextMapper.contains("votedSlotIDs"))
        XCTAssertTrue(
            contextMapper.contains("allSatisfy"),
            "A non-empty partial vote must remain REQUIRED until every proposed slot has a vote."
        )
        XCTAssertFalse(
            detailSource.contains("poll.votes[userId]?.isEmpty == false"),
            "Any non-empty vote dictionary is insufficient to claim SUBMITTED."
        )
    }

    func testProductionShareCallbackConsumesOpaquePayloadWithoutLegacyInvitationRouting() throws {
        let detailSource = try eventDetailSource()
        let canvasCall = slice(
            detailSource,
            from: "EventDetailInvitationCanvas(",
            to: "if isInvitationLanding"
        )
        let shareHandler = slice(
            detailSource,
            from: "private func shareServerIssuedInvitation",
            to: "static let disconnectedSecureShareConfiguration"
        )
        let disconnectedConfiguration = slice(
            detailSource,
            from: "static let disconnectedSecureShareConfiguration",
            to: "private var canvasContext"
        )

        XCTAssertTrue(
            detailSource.contains(
                "let onShareServerIssuedInvitation: (EventDetailInvitationServerIssuedSharePayload) -> Void"
            ),
            "Event Detail must accept the opaque server-issued payload as its share callback input."
        )
        XCTAssertTrue(canvasCall.contains("onShare: shareServerIssuedInvitation"))
        XCTAssertFalse(canvasCall.contains("onOpenInvitationShare"))
        XCTAssertFalse(canvasCall.contains("InvitationTokenCodec"))
        XCTAssertTrue(shareHandler.contains("onShareServerIssuedInvitation(serverIssuedPayload)"))
        XCTAssertTrue(shareHandler.contains("capabilityId"))
        XCTAssertFalse(shareHandler.contains("InvitationTokenCodec"))
        XCTAssertTrue(
            disconnectedConfiguration.contains("shareCapability: .hidden"),
            "Production share must remain hidden until the secure capability is connected."
        )
    }

    func testProductionDoesNotAdvertiseEditDraftWithoutARealEditRoute() throws {
        let detailSource = try eventDetailSource()
        let actionHandler = slice(
            detailSource,
            from: "private func performCanvasAction",
            to: "private func scrollToProgressiveDetails"
        )

        XCTAssertFalse(detailSource.contains("actions.insert(.editDraft)"))
        XCTAssertFalse(
            actionHandler.contains("case .editDraft:\n            onManageParticipants()"),
            "EDIT_DRAFT must not be silently routed to participant management."
        )
    }

    func testOneActionIdentityUsesMutuallyExclusiveAdaptivePlacement() throws {
        let source = try readIOSProductionSources()
        let detailSource = try eventDetailSource()
        let canvasImplementation = sources(containing: "struct EventDetailInvitationCanvas: View", in: source)

        XCTAssertTrue(source.contains("enum EventDetailInvitationCanvasActionPlacement"))
        XCTAssertTrue(source.contains("case inCanvas"))
        XCTAssertTrue(source.contains("case persistentSafeArea"))
        XCTAssertTrue(source.contains("primaryActionPlacement: EventDetailInvitationCanvasActionPlacement"))
        XCTAssertTrue(canvasImplementation.contains("switch presentation.primaryActionPlacement"))
        XCTAssertEqual(
            occurrences(of: "EventDetailInvitationCanvas(", in: detailSource),
            1,
            "Event Detail must render one invitation canvas, not duplicate its primary action surface."
        )
        XCTAssertFalse(detailSource.contains("urgentNextAction"))
        XCTAssertFalse(detailSource.contains("bottomPrimaryAction"))
    }

    func testCanvasHonorsAccessibilityAndKeepsControlsAtNativeHitTargets() throws {
        let source = try readProjectFile(
            "iosApp/src/Views/Events/EventDetailInvitationCanvas.swift"
        )
        let canvas = slice(
            source,
            from: "struct EventDetailInvitationCanvas: View",
            to: "struct EventDetailInvitationCanvasPersistentAction: View"
        )

        for environment in [
            "accessibilityReduceTransparency",
            "colorSchemeContrast",
            "dynamicTypeSize",
            "@ScaledMetric"
        ] {
            XCTAssertTrue(
                canvas.contains(environment),
                "Invitation canvas must adapt to \(environment)."
            )
        }

        let motionProducingAPIs = [
            "withAnimation(",
            ".animation(",
            ".transition(",
            ".symbolEffect(",
            ".contentTransition("
        ]
        let canvasProducesMotion = motionProducingAPIs.contains { canvas.contains($0) }
        XCTAssertTrue(
            canvas.contains("accessibilityReduceMotion") || !canvasProducesMotion,
            "A canvas that produces motion must adapt it to accessibilityReduceMotion."
        )

        XCTAssertTrue(canvas.contains(".accessibilitySortPriority("))
        XCTAssertTrue(source.contains(".frame(minWidth: 44, minHeight: 44)"))
        XCTAssertTrue(source.contains("if #available(iOS 26.0, *)"))
        XCTAssertTrue(source.contains("GlassEffectContainer("))
    }

    func testPersistentActionAndShareLoaderUseOpaqueAccessibilityFallbacks() throws {
        let source = try readProjectFile(
            "iosApp/src/Views/Events/EventDetailInvitationCanvas.swift"
        )
        let persistentAction = slice(
            source,
            from: "struct EventDetailInvitationCanvasPersistentAction: View",
            to: "private struct EventDetailInvitationNextActionSurface"
        )
        let shareLoader = slice(
            source,
            from: "private struct EventDetailInvitationCircleProgress: View",
            to: "private struct EventDetailInvitationParticipantStrip"
        )

        for component in [persistentAction, shareLoader] {
            XCTAssertTrue(component.contains("accessibilityReduceTransparency"))
            XCTAssertTrue(component.contains("colorSchemeContrast"))
            XCTAssertTrue(
                component.contains("midnightElevated"),
                "Reduce Transparency and increased contrast require an opaque surface."
            )
        }
        XCTAssertFalse(
            persistentAction.contains(".background(.ultraThinMaterial)"),
            "The persistent action cannot always force translucent material."
        )
        XCTAssertFalse(
            shareLoader.contains(".background(.ultraThinMaterial, in: Circle())"),
            "The share loader cannot always force translucent material."
        )
    }

    func testContextSyncUsesHonestEventAndParticipantResponseSubjects() throws {
        let source = try readIOSProductionSources()
        let detailSource = try eventDetailSource()
        let contextMapper = sources(
            containing: "struct EventDetailInvitationCanvasContextMapper",
            in: source
        )

        XCTAssertTrue(contextMapper.contains(".event(id:"))
        XCTAssertTrue(contextMapper.contains(".participantResponse(id:"))
        XCTAssertTrue(contextMapper.contains(".pending(subject:"))
        XCTAssertFalse(
            detailSource.contains(".planningItem(id: event.id)"),
            "An event or vote pending state must not be mislabeled as a planning-item sync."
        )
    }

    func testCanvasConstrainsNarrowContentAndAdaptsDateLifecycleLayout() throws {
        let source = try readProjectFile(
            "iosApp/src/Views/Events/EventDetailInvitationCanvas.swift"
        )
        let canvas = slice(
            source,
            from: "struct EventDetailInvitationCanvas: View",
            to: "struct EventDetailInvitationCanvasPersistentAction: View"
        )
        let canvasBody = slice(
            canvas,
            from: "var body: some View",
            to: "private var heroBackground"
        )
        let titleAndState = slice(
            canvas,
            from: "private var titleAndState",
            to: "private var syncAndFreshnessDecoration"
        )
        let nextAction = slice(
            source,
            from: "private struct EventDetailInvitationNextActionSurface",
            to: "private struct EventDetailInvitationPrimaryActionButton"
        )

        XCTAssertTrue(
            canvasBody.contains(".frame(maxWidth: .infinity, alignment: .leading)"),
            "The padded canvas content must accept the viewport width and stay leading-aligned."
        )
        XCTAssertTrue(
            nextAction.contains(".frame(maxWidth: .infinity, alignment: .leading)"),
            "The next-action surface must not size itself from an unbounded text ideal width."
        )
        XCTAssertTrue(
            occurrences(of: ".frame(maxWidth: .infinity, alignment: .leading)", in: nextAction) >= 2
                || nextAction.contains(".layoutPriority(1)"),
            "The next-action text column needs an explicit compressible-width or layout-priority contract."
        )

        let hasStructuredNarrowFallback = titleAndState.contains("ViewThatFits")
            || titleAndState.contains("EventDetailInvitationDateLifecycleLayout")
            || (titleAndState.contains("horizontalSizeClass") && titleAndState.contains("VStack"))
            || titleAndState.contains("AnyLayout")
        XCTAssertTrue(
            hasStructuredNarrowFallback,
            "Date and lifecycle need a structured vertical fallback when their horizontal row does not fit."
        )
        XCTAssertTrue(titleAndState.contains("Text(dateText)"))
        XCTAssertTrue(titleAndState.contains("presentation.lifecycleLabelKey"))
    }

    func testHeroCropAndFocalPointComeFromTheTotalArtworkAggregate() throws {
        let canvasSource = try readProjectFile(
            "iosApp/src/Views/Events/EventDetailInvitationCanvas.swift"
        )
        let artworkSource = try readProjectFile(
            "iosApp/src/Views/Invitations/EventLibraryView.swift"
        )
        let canvas = slice(
            canvasSource,
            from: "struct EventDetailInvitationCanvas: View",
            to: "struct EventDetailInvitationCanvasPersistentAction: View"
        )
        let canvasInputs = slice(canvas, from: "let event: Event", to: "var body: some View")
        let productionHero = slice(
            canvas,
            from: "private var productionHeroBackground",
            to: "private var readableScrim"
        )
        let sharedArtwork = slice(
            artworkSource,
            from: "struct InvitationArtworkView: View",
            to: "private struct LibraryFilterChip"
        )
        let qaHost = slice(
            canvasSource,
            from: "struct EventDetailInvitationCanvasQAView: View",
            to: "#Preview(\"Invitation Canvas — Annecy confirmed\")"
        )

        XCTAssertTrue(canvasInputs.contains("let artwork: any Artwork"))
        XCTAssertFalse(canvasInputs.contains("heroCropPolicy"))
        XCTAssertTrue(productionHero.contains("InvitationArtworkView("))
        XCTAssertTrue(productionHero.contains("artwork: artwork"))
        XCTAssertTrue(sharedArtwork.contains("structured.ref.crop"))
        XCTAssertTrue(sharedArtwork.contains("structured.ref.focalPoint"))
        XCTAssertTrue(sharedArtwork.contains("focalAlignment(focalPoint)"))
        XCTAssertTrue(
            qaHost.contains("artwork: EventDetailInvitationCanvasPreviewFixture.artwork"),
            "The Annecy QA host must inject modeled PRESET artwork through the production renderer."
        )
        XCTAssertFalse(qaHost.contains("previewHeroImageName"))
        XCTAssertFalse(qaHost.contains("heroCropPolicy"))
    }

    func testSharedArtworkRendererBoundsEveryFillCropAndHonorsFocalAlignment() throws {
        let canvasSource = try readProjectFile(
            "iosApp/src/Views/Events/EventDetailInvitationCanvas.swift"
        )
        let artworkSource = try readProjectFile(
            "iosApp/src/Views/Invitations/EventLibraryView.swift"
        )
        let canvas = slice(
            canvasSource,
            from: "struct EventDetailInvitationCanvas: View",
            to: "struct EventDetailInvitationCanvasPersistentAction: View"
        )
        let productionHero = slice(
            canvas,
            from: "private var productionHeroBackground",
            to: "private var readableScrim"
        )
        let sharedArtwork = slice(
            artworkSource,
            from: "struct InvitationArtworkView: View",
            to: "private struct LibraryFilterChip"
        )

        XCTAssertTrue(
            productionHero.contains("InvitationArtworkView("),
            "Canvas geometry must contain the single shared artwork renderer."
        )
        XCTAssertTrue(sharedArtwork.contains(".scaledToFill()"))
        XCTAssertTrue(sharedArtwork.contains(".clipped()"))
        XCTAssertTrue(sharedArtwork.contains("focalAlignment(focalPoint)"))
        XCTAssertTrue(sharedArtwork.contains("crop == .fit"))
        XCTAssertFalse(
            productionHero.contains("AsyncImage("),
            "Detail must not retain a second remote-only crop implementation beside InvitationArtworkView."
        )
    }

    func testCanvasReceivesFiniteViewportWidthWithoutFixingDynamicTypeHeight() throws {
        let canvasSource = try readProjectFile(
            "iosApp/src/Views/Events/EventDetailInvitationCanvas.swift"
        )
        let contentSource = try readProjectFile("iosApp/src/Views/App/ContentView.swift")
        let canvas = slice(
            canvasSource,
            from: "struct EventDetailInvitationCanvas: View",
            to: "struct EventDetailInvitationCanvasPersistentAction: View"
        )
        let canvasInputs = slice(canvas, from: "let event: Event", to: "var body: some View")
        let canvasBody = slice(canvas, from: "var body: some View", to: "private var heroBackground")
        let eventDetail = slice(
            contentSource,
            from: "struct EventDetailView: View",
            to: "private struct EventDetailHeroMetric"
        )
        let productionCall = slice(
            eventDetail,
            from: "EventDetailInvitationCanvas(",
            to: "if isInvitationLanding"
        )
        let qaHost = slice(
            canvasSource,
            from: "struct EventDetailInvitationCanvasQAView: View",
            to: "#Preview(\"Invitation Canvas — Annecy confirmed\")"
        )

        XCTAssertTrue(
            canvasInputs.contains("let viewportWidth: CGFloat"),
            "The canvas must receive a finite width from its measured parent."
        )
        assertAppearInOrder(
            [
                ".frame(width: viewportWidth, alignment: .leading)",
                ".frame(minHeight:",
                ".clipped()"
            ],
            in: canvasBody
        )
        XCTAssertTrue(productionCall.contains("viewportWidth: viewport.size.width"))
        XCTAssertTrue(qaHost.contains("viewportWidth: geometry.size.width"))
        XCTAssertFalse(
            canvasInputs.contains("viewportHeight"),
            "Only width is constrained; height must remain intrinsic for Dynamic Type."
        )
        XCTAssertFalse(
            canvasBody.contains(".frame(height:"),
            "The canvas root may use a minimum height but must not fix its height."
        )
        XCTAssertFalse(
            canvasBody.contains(".frame(width: viewportWidth, height:"),
            "The finite viewport contract must not smuggle in a fixed height."
        )
    }

    func testConfirmedParticipantBadgeIncludesACheckmarkAndPendingRemainsDistinct() throws {
        let source = try readProjectFile(
            "iosApp/src/Views/Events/EventDetailInvitationCanvas.swift"
        )
        let avatar = slice(
            source,
            from: "private struct EventDetailInvitationInitialsAvatar: View",
            to: "private enum EventDetailInvitationCanvasPreviewFixture"
        )

        XCTAssertTrue(avatar.contains("case .confirmed"))
        XCTAssertTrue(
            avatar.contains("checkmark"),
            "Confirmed participation cannot be communicated by green color alone."
        )
        XCTAssertTrue(avatar.contains("case .pending"))
        XCTAssertTrue(
            avatar.contains("warmAmber"),
            "Pending participation must retain a distinct visual treatment."
        )
        XCTAssertFalse(
            avatar.contains("status == .confirmed ?"),
            "A color-only confirmed/pending ternary cannot express the semantic badge distinction."
        )
    }

    func testAnnecyQAFixtureExercisesMenuChromeWithAnAbbreviatedDate() throws {
        let source = try readProjectFile(
            "iosApp/src/Views/Events/EventDetailInvitationCanvas.swift"
        )
        let debugFixture = slice(
            source,
            from: "private enum EventDetailInvitationCanvasPreviewFixture",
            to: "#endif"
        )
        let qaHost = slice(
            debugFixture,
            from: "struct EventDetailInvitationCanvasQAView: View",
            to: "#Preview(\"Invitation Canvas — Annecy confirmed\")"
        )

        XCTAssertTrue(debugFixture.contains("static let dateText = \"18–20 sept. 2026\""))
        XCTAssertTrue(qaHost.contains("dateText: EventDetailInvitationCanvasPreviewFixture.dateText"))
        XCTAssertTrue(debugFixture.contains("static let menuActions"))
        XCTAssertTrue(debugFixture.contains("EventDetailInvitationCanvasMenuAction("))
        XCTAssertTrue(qaHost.contains("menuActions: EventDetailInvitationCanvasPreviewFixture.menuActions"))
        XCTAssertFalse(
            qaHost.contains("menuActions: []"),
            "The narrow-width QA launch must exercise the trailing menu control."
        )
    }

    func testAnnecyQAHostScrollsVerticallyWithoutFixingAccessibilityHeight() throws {
        let source = try readProjectFile(
            "iosApp/src/Views/Events/EventDetailInvitationCanvas.swift"
        )
        let qaHost = slice(
            source,
            from: "struct EventDetailInvitationCanvasQAView: View",
            to: "#Preview(\"Invitation Canvas — Annecy confirmed\")"
        )
        let hasVerticalScroll = qaHost.contains("ScrollView {")
            || qaHost.contains("ScrollView(.vertical")

        XCTAssertTrue(
            hasVerticalScroll,
            "The QA host must scroll when accessibility Dynamic Type expands the canvas beyond the viewport."
        )
        assertAppearInOrder(
            [
                "GeometryReader { geometry in",
                "ScrollView",
                "EventDetailInvitationCanvas("
            ],
            in: qaHost
        )
        XCTAssertTrue(qaHost.contains("viewportWidth: geometry.size.width"))
        XCTAssertFalse(qaHost.contains(".frame(height:"))
        XCTAssertFalse(qaHost.contains(".frame(maxHeight:"))
        XCTAssertFalse(
            qaHost.contains("height: geometry.size.height"),
            "The viewport height must not be imposed on vertically expanding QA content."
        )
    }

    func testAnnecyQAScrollContainerOwnsSafeAreaCoverage() throws {
        let source = try readProjectFile(
            "iosApp/src/Views/Events/EventDetailInvitationCanvas.swift"
        )
        let qaHost = slice(
            source,
            from: "struct EventDetailInvitationCanvasQAView: View",
            to: "#Preview(\"Invitation Canvas — Annecy confirmed\")"
        )

        XCTAssertTrue(qaHost.contains("ScrollView(.vertical"))
        XCTAssertTrue(qaHost.contains("safeAreaTop: geometry.safeAreaInsets.top"))

        guard let scrollRange = qaHost.range(of: "ScrollView"),
              let openBrace = qaHost[scrollRange.upperBound...].firstIndex(of: "{")
        else {
            XCTFail("The QA host requires a structured vertical ScrollView.")
            return
        }

        var depth = 0
        var closeBrace: String.Index?
        for index in qaHost[openBrace...].indices {
            switch qaHost[index] {
            case "{":
                depth += 1
            case "}":
                depth -= 1
                if depth == 0 {
                    closeBrace = index
                }
            default:
                break
            }
            if closeBrace != nil { break }
        }

        guard let closeBrace else {
            XCTFail("Unable to resolve the QA ScrollView boundary.")
            return
        }

        let scrollBodyStart = qaHost.index(after: openBrace)
        let scrollBody = String(qaHost[scrollBodyStart..<closeBrace])
        let modifierStart = qaHost.index(after: closeBrace)
        let modifierTail = qaHost[modifierStart...]
        let modifierEnd = modifierTail.firstIndex(of: "}") ?? qaHost.endIndex
        let scrollModifiers = String(qaHost[modifierStart..<modifierEnd])

        XCTAssertTrue(scrollBody.contains("EventDetailInvitationCanvas("))
        XCTAssertFalse(
            scrollBody.contains(".ignoresSafeArea()"),
            "Safe-area coverage on the child leaves the ScrollView's own top inset black."
        )
        XCTAssertTrue(
            scrollModifiers.contains(".ignoresSafeArea()"),
            "The scroll container must paint through the safe area while preserving the measured top inset for controls."
        )
    }

    func testExplicitDebugLaunchArgumentDisplaysExactAnnecyCanvasFixtureAndPreservesContentView() throws {
        let appSource = try readProjectFile("iosApp/src/iOSApp.swift")
        let canvasSource = try readProjectFile(
            "iosApp/src/Views/Events/EventDetailInvitationCanvas.swift"
        )
        let launchArgument = "--wakeve-qa-invitation-canvas"
        let qaView = "EventDetailInvitationCanvasQAView()"
        let qaDeclaration = "struct EventDetailInvitationCanvasQAView: View"

        XCTAssertTrue(
            appSource.contains(launchArgument),
            "The deterministic canvas QA screen requires an explicit launch argument."
        )
        XCTAssertTrue(
            appSource.contains(qaView),
            "The app launch gate must select the dedicated invitation-canvas QA view."
        )

        if let argumentRange = appSource.range(of: launchArgument),
           let debugStart = appSource[..<argumentRange.lowerBound]
            .range(of: "#if DEBUG", options: .backwards),
           let debugEnd = appSource[argumentRange.upperBound...].range(of: "#endif") {
            let debugGate = String(appSource[debugStart.lowerBound..<debugEnd.upperBound])
            let releaseAndNormalSource = String(appSource[..<debugStart.lowerBound])
                + String(appSource[debugEnd.upperBound...])

            XCTAssertTrue(debugGate.contains(qaView))
            XCTAssertFalse(
                releaseAndNormalSource.contains(launchArgument),
                "The QA argument must not be compiled into the Release launch path."
            )
            XCTAssertFalse(releaseAndNormalSource.contains(qaView))
            XCTAssertEqual(
                occurrences(of: "ContentView()", in: releaseAndNormalSource),
                1,
                "Normal and Release launches must keep the existing ContentView root."
            )
            for existingModifier in [
                ".environmentObject(authStateManager)",
                ".environmentObject(authService)",
                ".environmentObject(deepLinkService)",
                ".onOpenURL",
                ".onReceive"
            ] {
                XCTAssertTrue(
                    releaseAndNormalSource.contains(existingModifier),
                    "The QA gate must not replace ContentView's existing \(existingModifier) wiring."
                )
            }
        } else {
            XCTFail("The QA launch argument must be enclosed by an explicit #if DEBUG gate.")
        }

        XCTAssertTrue(
            canvasSource.contains(qaDeclaration),
            "The Annecy fixture needs a launchable SwiftUI QA host."
        )

        if let declarationRange = canvasSource.range(of: qaDeclaration),
           let debugStart = canvasSource[..<declarationRange.lowerBound]
            .range(of: "#if DEBUG", options: .backwards),
           let debugEnd = canvasSource[declarationRange.upperBound...].range(of: "#endif") {
            let debugFixture = String(canvasSource[debugStart.lowerBound..<debugEnd.upperBound])
            let qaHost = slice(
                debugFixture,
                from: qaDeclaration,
                to: "#Preview(\"Invitation Canvas — Annecy confirmed\")"
            )

            XCTAssertEqual(occurrences(of: "EventDetailInvitationCanvas(", in: qaHost), 1)
            XCTAssertTrue(qaHost.contains("EventDetailInvitationCanvasPreviewFixture.event"))
            XCTAssertTrue(qaHost.contains("EventDetailInvitationCanvasPreviewFixture.presentation"))
            XCTAssertTrue(qaHost.contains("EventDetailInvitationCanvasPreviewFixture.artwork"))
            XCTAssertFalse(qaHost.contains("assetName:"))
            XCTAssertFalse(qaHost.contains("previewHeroImageName:"))
            XCTAssertTrue(qaHost.contains("dateText: EventDetailInvitationCanvasPreviewFixture.dateText"))
            XCTAssertTrue(qaHost.contains("organizerName: \"Léa Martin\""))
            XCTAssertFalse(
                qaHost.contains("EventFactory.make("),
                "The QA host must reuse the approved Annecy fixture instead of cloning it."
            )
        } else {
            XCTFail("The launchable Annecy QA host must itself be compiled only under #if DEBUG.")
        }
    }

    func testEveryCanvasLocalizationKeyExistsInEachSupportedLocale() throws {
        let source = try readProjectFile(
            "iosApp/src/Views/Events/EventDetailInvitationCanvas.swift"
        )
        let expression = try NSRegularExpression(
            pattern: #"\"(event\.detail\.canvas\.[^\"]+)\""#
        )
        let range = NSRange(source.startIndex..<source.endIndex, in: source)
        let keys: Set<String> = Set(expression.matches(in: source, range: range).compactMap { match -> String? in
            guard let keyRange = Range(match.range(at: 1), in: source) else { return nil }
            return String(source[keyRange])
        })

        XCTAssertFalse(keys.isEmpty, "The canvas must expose localized user-facing copy.")

        for locale in ["en", "fr", "es", "it", "pt"] {
            let strings = try readProjectFile(
                "iosApp/src/Resources/\(locale).lproj/Localizable.strings"
            )
            for key in keys {
                XCTAssertTrue(
                    strings.contains("\"\(key)\""),
                    "Missing canvas localization key \(key) for \(locale)."
                )
            }
        }
    }

    private func eventDetailSource() throws -> String {
        let source = try readProjectFile("iosApp/src/Views/App/ContentView.swift")
        return slice(source, from: "struct EventDetailView", to: "private struct EventDetailHeroMetric")
    }

    private func makeInput(
        eventStatus: EventStatus,
        hasRequiredSlots: Bool = true,
        access: EventDetailInvitationCanvasAccess,
        vote: EventDetailInvitationCanvasVoteState = .required,
        participantData: EventDetailInvitationCanvasParticipantData = .unavailable,
        responsibility: EventDetailInvitationCanvasResponsibility = .unavailable,
        readinessData: EventDetailInvitationCanvasReadinessData = .available(
            items: [
                EventDetailInvitationCanvasReadinessItem(
                    id: "next-organization-item",
                    isComplete: false,
                    destination: .continueOrganization
                )
            ]
        ),
        availableActions: Set<EventDetailInvitationCanvasAction>,
        relevantSync: EventDetailInvitationCanvasRelevantSync = .none,
        shareCapability: EventDetailInvitationShareCapability = .hidden,
        requestedSecondarySections: Set<EventDetailInvitationCanvasSecondarySection> = [],
        primaryActionPlacement: EventDetailInvitationCanvasActionPlacement = .inCanvas
    ) -> EventDetailInvitationCanvasMapper.Input {
        EventDetailInvitationCanvasMapper.Input(
            eventStatus: eventStatus,
            hasRequiredSlots: hasRequiredSlots,
            currentUserAccess: access,
            currentUserVote: vote,
            participantData: participantData,
            responsibility: responsibility,
            readinessData: readinessData,
            availableActions: availableActions,
            relevantSync: relevantSync,
            shareCapability: shareCapability,
            heroImageState: .missing,
            auxiliaryFreshness: .current,
            requestedSecondarySections: requestedSecondarySections,
            primaryActionPlacement: primaryActionPlacement
        )
    }

    private func readIOSProductionSources() throws -> String {
        let root = try projectRoot().appendingPathComponent("iosApp/src", isDirectory: true)
        guard let enumerator = FileManager.default.enumerator(
            at: root,
            includingPropertiesForKeys: [.isRegularFileKey],
            options: [.skipsHiddenFiles]
        ) else {
            throw CocoaError(.fileReadUnknown)
        }

        let swiftFiles = enumerator
            .compactMap { $0 as? URL }
            .filter { $0.pathExtension == "swift" }
            .sorted { $0.path < $1.path }

        return try swiftFiles
            .map { "// FILE: \($0.path)\n" + (try String(contentsOf: $0, encoding: .utf8)) }
            .joined(separator: "\n")
    }

    private func readProjectFile(_ relativePath: String) throws -> String {
        let target = try projectRoot().appendingPathComponent(relativePath)
        return try String(contentsOf: target, encoding: .utf8)
    }

    private func projectRoot() throws -> URL {
        let fileURL = URL(fileURLWithPath: #filePath)
        let runtimeURL = URL(fileURLWithPath: FileManager.default.currentDirectoryPath)

        for startURL in [fileURL.deletingLastPathComponent(), runtimeURL] {
            var candidate = startURL
            for _ in 0..<8 {
                if FileManager.default.fileExists(atPath: candidate.appendingPathComponent("iosApp").path) {
                    return candidate
                }
                let parent = candidate.deletingLastPathComponent()
                guard parent.path != candidate.path else { break }
                candidate = parent
            }
        }

        throw CocoaError(.fileNoSuchFile)
    }

    private func sources(containing marker: String, in combinedSource: String) -> String {
        combinedSource
            .components(separatedBy: "// FILE: ")
            .filter { $0.contains(marker) }
            .joined(separator: "\n")
    }

    private func bracketedDeclaration(named name: String, in source: String) -> String {
        guard let nameRange = source.range(of: name),
              let openBracket = source[nameRange.lowerBound...].firstIndex(of: "[")
        else {
            return ""
        }

        let tail = source[openBracket...]
        guard let closeBracket = tail.firstIndex(of: "]") else { return "" }
        return String(tail[...closeBracket])
    }

    private func assertAppearInOrder(
        _ markers: [String],
        in source: String,
        file: StaticString = #filePath,
        line: UInt = #line
    ) {
        var searchStart = source.startIndex

        for marker in markers {
            guard let range = source.range(of: marker, range: searchStart..<source.endIndex) else {
                XCTFail("Missing or out-of-order Event Detail section: \(marker).", file: file, line: line)
                return
            }
            searchStart = range.upperBound
        }
    }

    private func occurrences(of needle: String, in source: String) -> Int {
        guard !needle.isEmpty else { return 0 }
        return source.components(separatedBy: needle).count - 1
    }

    private func slice(_ source: String, from startMarker: String, to endMarker: String) -> String {
        guard let start = source.range(of: startMarker)?.lowerBound else { return source }
        let tail = source[start...]
        guard let end = tail.range(of: endMarker)?.lowerBound else { return String(tail) }
        return String(tail[..<end])
    }
}
