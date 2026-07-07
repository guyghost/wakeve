import XCTest

final class OrganizationPhase7ContractTests: XCTestCase {
    func testOrganizationDashboardExposesEveryFinalizationReadinessSection() throws {
        let sources = try organizationSources()
        let expectedSections: [String: [String]] = [
            "participants": ["Participants", "participants", "attendance", "confirmed attendee"],
            "scenario": ["Scenario", "scenario"],
            "destination": ["Destination", "destination"],
            "lodging": ["Lodging", "Accommodation", "Hebergement", "lodging", "accommodation"],
            "transport": ["Transport", "transport"],
            "meetings": ["Meetings", "Meeting", "Reunions", "meeting"],
            "calendar": ["Calendar", "Calendrier", "calendar"],
            "notifications": ["Notifications", "Notification", "reminder", "Rappel"],
            "budget": ["Budget", "Expense", "Depenses", "budget"],
            "payment": ["Payment", "PaymentPot", "Cagnotte", "payment"],
            "tricount": ["Tricount", "tricount"],
            "sync": ["Sync", "pendingSync", "Queued", "offline", "sync"],
            "unsafe links": ["Unsafe", "SafeExternalLink", "safe link", "external link"],
            "access control": ["AccessDenied", "accessControl", "canAccessOrganizationDetails"]
        ]

        let missing = expectedSections
            .filter { _, candidates in !containsAny(sources, candidates) }
            .map { key, _ in key }
            .sorted()

        XCTAssertTrue(
            missing.isEmpty,
            "Phase 7 iOS organization dashboard must expose every finalization readiness section. Missing: \(missing)"
        )
    }

    func testOrganizationStatesAreVisiblyDistinctAcrossAccessSyncAndReadiness() throws {
        let sources = try organizationSources()
        let expectedStates: [String: [String]] = [
            "access denied": ["AccessDenied", "access denied", "Confirm your attendance"],
            "optional not needed": ["notNeeded", "NotNeeded", "not needed", "non requis"],
            "incomplete": ["Incomplete", "incomplete", "missing", "required"],
            "complete": ["Complete", "complete", "ready", "pret"],
            "pending sync": ["pendingSync", "PendingSync", "queued", "Pending"],
            "failed sync": ["failedSync", "FailedSync", "retry", "conflict", "ConflictDetected"],
            "read only finalized": ["readOnly", "ReadOnly", "finalized", "Finalized"]
        ]

        let missing = expectedStates
            .filter { _, candidates in !containsAny(sources, candidates) }
            .map { key, _ in key }
            .sorted()

        XCTAssertTrue(
            missing.isEmpty,
            "Phase 7 iOS UI must distinguish access, readiness, sync, and read-only states. Missing: \(missing)"
        )
        XCTAssertFalse(
            sources.range(of: #"pendingSync\s*:\s*false|pendingSync\s*=\s*false"#, options: .regularExpression) != nil,
            "Phase 7 iOS UI must derive pending-sync state from repositories instead of hard-coded false values."
        )
    }

    func testFinalizedOrganizationUxKeepsDetailsButDisablesMutationActions() throws {
        let sources = try organizationSources()
        let finalizedBlocks = regexMatches(
            in: sources,
            pattern: #"\.finalized[\s\S]{0,1600}|EventStatus\.FINALIZED[\s\S]{0,1600}"#
        ).joined(separator: "\n")

        XCTAssertFalse(
            finalizedBlocks.isEmpty,
            "iOS must have an explicit FINALIZED organization UI branch."
        )
        XCTAssertTrue(
            containsAny(
                finalizedBlocks,
                ["readOnly", "ReadOnly", ".disabled(true)", "disabled: true", "mutationsDisabled", "viewOnly"]
            ),
            "FINALIZED organization sections must be rendered read-only, not as active planning controls."
        )
        XCTAssertFalse(
            sources.range(
                of: #"\.finalized[\s\S]{0,900}(onCreate|onAdd|onLink|onClose|onMark|Create|Ajouter|Associer)"#,
                options: [.regularExpression, .caseInsensitive]
            ) != nil,
            "FINALIZED UI must not expose organization mutation actions."
        )
    }

    func testPreConfirmedAndUnauthorizedUsersDoNotSeeConfirmedOnlyOrganizationDetails() throws {
        let sources = try organizationSources()

        XCTAssertFalse(
            sources.range(
                of: #"\.(draft|polling)[\s\S]{0,900}(MeetingListView|BudgetOverviewView|PaymentPotView|TricountHandoffView|TransportPlanningView|OrganizationReadiness)"#,
                options: [.regularExpression, .caseInsensitive]
            ) != nil,
            "DRAFT and POLLING states must not expose confirmed-only organization dashboard details."
        )
        XCTAssertTrue(
            containsAny(
                sources,
                ["canAccessOrganizationDetails", "isParticipantConfirmed", "validatedRetainedDate", "VALIDATED_RETAINED_DATE"]
            ),
            "iOS organization surfaces must gate detail access through organizer-or-confirmed-attendee policy."
        )
        XCTAssertTrue(
            containsAny(
                sources,
                ["AccessDenied", "access denied", "Accès refusé", "Confirm your attendance", "confirmationRequired"]
            ),
            "iOS must render an access-denied or confirmation-required state for declined, pending, and non-participant users."
        )
    }

    func testConfirmedParticipantAndDeniedAccessStatesAreExplicitOnOrganizationRoutes() throws {
        let content = try readProjectFile("iosApp/src/Views/App/ContentView.swift")
        let homeContent = slice(content, from: "private var homeTabContent", to: "// MARK: - Tab Content")
        let sensitiveCases = [
            "case .transportPlanning",
            "case .meetingList",
            "case .budgetOverview",
            "case .paymentPot",
            "case .tricount"
        ]
        let missingCases = sensitiveCases.filter { !homeContent.contains($0) }

        XCTAssertTrue(
            missingCases.isEmpty,
            "iOS must define event-scoped organization route cases for all confirmed-participant details. Missing: \(missingCases)"
        )
        XCTAssertTrue(
            containsAny(
                homeContent,
                ["isParticipantConfirmed(for: event)", "canAccessOrganizationDetails", "validatedRetainedDate", "participantAccess"]
            ),
            "iOS organization route cases must admit confirmed participants, not only organizers."
        )
        XCTAssertTrue(
            containsAny(
                homeContent,
                ["declined", "pending", "notInvited", "AccessDenied", "confirmationRequired", "accessDeniedReason"]
            ),
            "iOS route gating must distinguish declined, pending, and non-participant access states instead of silently hiding details."
        )
    }

    func testScenarioEmptyStateIsActionableForManualAndMatrixPlanning() throws {
        let scenarioView = try readProjectFile("iosApp/src/Views/Events/ScenarioOrganizationView.swift")
        let contentView = try readProjectFile("iosApp/src/Views/App/ContentView.swift")
        let emptyState = slice(scenarioView, from: "private var emptyState", to: "@ViewBuilder\n    private var bottomComparisonBar")
        let createFlow = slice(contentView, from: ".fullScreenCover(isPresented: $showEventCreationSheet)", to: ".sheet(isPresented: $showNotificationPreferencesSheet)")

        XCTAssertTrue(emptyState.contains("showCreateScenarioSheet = true"))
        XCTAssertTrue(scenarioView.contains("CreateScenarioOptionSheet("))
        XCTAssertTrue(scenarioView.contains("viewModel.createScenario("))
        XCTAssertTrue(scenarioView.contains("viewModel.generateScenarioMatrix(eventId: event.id, userId: participantId)"))
        XCTAssertTrue(scenarioView.contains("viewModel.publishScenarioMatrix(eventId: event.id, userId: participantId)"))
        XCTAssertTrue(scenarioView.contains("canGenerateMatrixScenarios"))
        XCTAssertTrue(scenarioView.contains("canPublishMatrixScenarios"))
        XCTAssertTrue(emptyState.contains("scenario.create_option"))
        XCTAssertTrue(scenarioView.contains("scenario.generate_options"))
        XCTAssertTrue(scenarioView.contains("scenario.matrix_publish.ready_title"))
        XCTAssertTrue(createFlow.contains("event.planningMode == .scenarioMatrix ? .scenarioList : .participantManagement"))
    }

    func testScenarioDecisionSurfacesBudgetBeforeFinalSelection() throws {
        let scenarioView = try readProjectFile("iosApp/src/Views/Events/ScenarioOrganizationView.swift")
        let scenarioContent = slice(scenarioView, from: "private var scenarioContent", to: "private func comparisonSection")
        let budgetSection = slice(scenarioView, from: "private var budgetDecisionSection", to: "private var missingScenarioBudgetCount")
        let finalSelectionCard = slice(scenarioView, from: "private struct ScenarioOrganizationCard", to: "private func formatBudget")

        XCTAssertTrue(scenarioContent.contains("budgetDecisionSection"))
        XCTAssertLessThan(
            scenarioContent.range(of: "budgetDecisionSection")?.lowerBound ?? scenarioContent.endIndex,
            scenarioContent.range(of: "String(localized: \"scenario.ranking\")")?.lowerBound ?? scenarioContent.endIndex,
            "The scenario budget decision summary must appear before ranking/final-choice controls."
        )
        XCTAssertTrue(budgetSection.contains("scenario.budget_decision.title"))
        XCTAssertTrue(budgetSection.contains("scenario.budget_decision.per_person"))
        XCTAssertTrue(scenarioView.contains("viewModel.scenarios.map(\\.scenario.estimatedBudgetPerPerson)"))
        XCTAssertTrue(budgetSection.contains("missingScenarioBudgetCount"))
        XCTAssertTrue(budgetSection.contains("ScenarioBudgetInsightTile"))
        XCTAssertFalse(budgetSection.contains("Budget avant décision"))
        XCTAssertTrue(finalSelectionCard.contains("scenario.estimatedBudgetPerPerson"))
    }

    func testSelectedScenarioDecisionCreatesAnnouncementAndNextActions() throws {
        let scenarioView = try readProjectFile("iosApp/src/Views/Events/ScenarioOrganizationView.swift")
        let scenarioContent = slice(scenarioView, from: "private var scenarioContent", to: "private func comparisonSection")
        let resolutionCard = slice(scenarioView, from: "private struct ScenarioDecisionResolutionCard", to: "private struct ScenarioOrganizationCard")

        XCTAssertTrue(scenarioContent.contains("selectedScenarioDecisionSection(selectedScenario)"))
        XCTAssertTrue(scenarioView.contains("selectedScenarioWithVotes"))
        XCTAssertTrue(scenarioView.contains("$0.scenario.status == .selected"))
        XCTAssertTrue(scenarioView.contains("selectedScenarioAnnouncementMessage(for:"))
        XCTAssertTrue(scenarioView.contains("scenario.decision.message_format"))
        XCTAssertTrue(resolutionCard.contains("ShareLink(item: announcementMessage)"))
        XCTAssertTrue(resolutionCard.contains("UIPasteboard.general.string = announcementMessage"))
        XCTAssertTrue(resolutionCard.contains("WakeveHaptics.success()"))
        XCTAssertTrue(resolutionCard.contains("scenarioDecisionResolutionCard"))
        XCTAssertTrue(resolutionCard.contains("scenarioDecisionShareLink"))
        XCTAssertTrue(resolutionCard.contains("scenarioDecisionCopyButton"))
        XCTAssertTrue(resolutionCard.contains("scenarioDecisionCopiedFeedback"))
        XCTAssertTrue(resolutionCard.contains("scenario.decision.open_transport"))
        XCTAssertTrue(resolutionCard.contains("scenario.decision.open_meetings"))
        XCTAssertTrue(resolutionCard.contains("isDisabled: !canOpenTransport"))

        for locale in ["en", "fr", "es", "it", "pt"] {
            let strings = try readProjectFile("iosApp/src/Resources/\(locale).lproj/Localizable.strings")
            for key in [
                "scenario.decision.title",
                "scenario.decision.subtitle_format",
                "scenario.decision.message_format",
                "scenario.decision.budget_value_format",
                "scenario.decision.share_action",
                "scenario.decision.copy_action",
                "scenario.decision.copied",
                "scenario.decision.open_transport",
                "scenario.decision.open_meetings",
                "scenario.decision.transport_locked"
            ] {
                XCTAssertTrue(strings.contains("\"\(key)\""), "Missing localized selected-scenario decision key \(key) for \(locale).")
            }
        }
    }

    func testScenarioComparisonSurfacesOptionalWeatherContext() throws {
        let scenarioView = try readProjectFile("iosApp/src/Views/Events/ScenarioOrganizationView.swift")
        let comparisonSection = slice(scenarioView, from: "private func comparisonSection", to: "private var lockedState")
        let weatherContext = slice(scenarioView, from: "private enum ScenarioWeatherContextState", to: "private struct ScenarioOrganizationCard")

        XCTAssertTrue(scenarioView.contains("import MapKit"))
        XCTAssertTrue(comparisonSection.contains("ScenarioWeatherComparisonContext(scenario: item.scenario)"))
        XCTAssertTrue(weatherContext.contains("ScenarioWeatherContextViewModel"))
        XCTAssertTrue(weatherContext.contains("ScenarioWeatherDateParser.firstDate(in: scenario.dateOrPeriod)"))
        XCTAssertTrue(weatherContext.contains("MKLocalSearch.Request()"))
        XCTAssertTrue(weatherContext.contains("WeatherKitEventForecastProvider()"))
        XCTAssertTrue(weatherContext.contains("weatherProvider.forecast(for: place, targetDate: targetDate"))
        XCTAssertTrue(weatherContext.contains("weather.title"))
        XCTAssertTrue(weatherContext.contains("weather.pending_title"))
    }

    func testScenarioManualCreationAndMatrixCopyUseLocalizationKeys() throws {
        let scenarioView = try readProjectFile("iosApp/src/Views/Events/ScenarioOrganizationView.swift")
        let helpers = slice(scenarioView, from: "private var budgetDecisionSubtitle", to: "private func formatBudget")
        let createSheet = slice(scenarioView, from: "private struct CreateScenarioOptionSheet", to: "private struct ScenarioSheetValueRow")

        XCTAssertTrue(helpers.contains("scenario.matrix_readiness.missing_slots_and_destinations"))
        XCTAssertTrue(helpers.contains("scenario.destination_to_define"))
        XCTAssertTrue(helpers.contains("scenario.manual.default_description"))
        XCTAssertTrue(createSheet.contains("scenario.manual.title"))
        XCTAssertTrue(createSheet.contains("scenario.manual.budget_per_person"))
        XCTAssertTrue(createSheet.contains("scenario.manual.duration_day_plural_format"))
        XCTAssertTrue(scenarioView.contains("scenario.compare_selected_format"))
        XCTAssertFalse(helpers.contains("Destination à préciser"))
        XCTAssertFalse(createSheet.contains("Nouvelle option"))
        XCTAssertFalse(createSheet.contains("Option à comparer avec le groupe."))
        XCTAssertFalse(scenarioView.contains("\"Comparer \\(selectedComparisonIds.count) scenarios\""))
    }

    func testOfflinePendingAndFailedSyncStatesDoNotReadAsServerConfirmed() throws {
        let sources = try organizationSources()

        XCTAssertTrue(
            containsAny(sources, ["pendingSync", "PendingSync", "queued", "local write", "local-first"]),
            "iOS organization sections must expose pending local-first writes."
        )
        XCTAssertTrue(
            containsAny(
                sources,
                [
                    "not yet synced",
                    "not server confirmed",
                    "server-confirmed",
                    "queued for sync",
                    "Synchronisation en attente",
                    "pending server confirmation"
                ]
            ),
            "Pending-sync copy must make clear local writes are not server-confirmed yet."
        )
        XCTAssertTrue(
            containsAny(
                sources,
                ["failedSync", "FailedSync", "retry", "Retry", "conflict", "ConflictDetected", "resolveConflict"]
            ),
            "Failed retryable sync or conflict states must be visible/actionable before finalization."
        )
    }

    func testVisiblePendingSyncBannersUseLocalizedUserCopy() throws {
        let visibleCopy = try visibleOrganizationCopy()
        let sources = try organizationSources()
        let pendingCopy = visibleCopy.filter { copy in
            containsAny(
                copy,
                [
                    "pending sync",
                    "queued",
                    "server confirmed",
                    "Synchronisation en attente",
                    "Modifications locales en attente"
                ]
            )
        }
        let localizedPendingKeys = [
            "sync.pending_changes",
            "meetings.sync.pending"
        ]
        let forbiddenPhrases = ["queued", "not server confirmed", "server-confirmed", "pendingSync"]
        let exposedTechnicalPhrases = pendingCopy.filter { copy in
            forbiddenPhrases.contains { phrase in
                copy.range(of: phrase, options: [.caseInsensitive, .diacriticInsensitive]) != nil
            }
        }

        XCTAssertTrue(
            pendingCopy.contains { copy in
                containsAny(
                    copy,
                    [
                        "Synchronisation en attente",
                        "Modifications locales en attente d'envoi",
                        "sync.pending_changes"
                    ]
                ) || sources.contains("sync.pending_changes")
            } || localizedPendingKeys.allSatisfy { sources.contains($0) },
            "Pending-sync banners must use localized user copy such as Synchronisation en attente / Modifications locales en attente d'envoi. Found: \(pendingCopy)"
        )
        XCTAssertTrue(
            exposedTechnicalPhrases.isEmpty,
            "Pending-sync banners must not expose implementation phrases like queued or not server confirmed. Found: \(exposedTechnicalPhrases)"
        )
    }

    func testTransportPlanningPrimaryFlowIsLocalizedForFrenchUsers() throws {
        let visibleCopy = try extractVisibleStrings(from: readProjectFile("iosApp/src/Views/Events/TransportPlanningView.swift"))
        let englishPrimaryPhrases = [
            "Destination not selected",
            "Confirmed date:",
            "Date confirmed soon",
            "Offline pending sync",
            "Readiness complete",
            "Missing departure",
            "All confirmed participants have a departure point.",
            "Departure",
            "Departure point",
            "Save departure",
            "Optimization",
            "Generate plan",
            "Transport not needed",
            "Generated plan",
            "No route has been generated yet.",
            "Selected",
            "Total cost:",
            "Select final plan",
            "Transport locked",
            "Only organizers and confirmed participants"
        ]
        let exposedEnglishPhrases = englishPrimaryPhrases.filter { phrase in
            visibleCopy.contains { copy in
                copy.range(of: phrase, options: [.caseInsensitive, .diacriticInsensitive]) != nil
            }
        }
        let localizedPrimaryCopy = [
            "Date confirmée",
            "Destination",
            "Synchronisation en attente",
            "Départ",
            "Point de départ",
            "Enregistrer",
            "Optimisation",
            "Générer",
            "Plan",
            "Sélectionner",
            "Transport verrouillé"
        ]

        let transportSource = try readProjectFile("iosApp/src/Views/Events/TransportPlanningView.swift")
        let requiredLocalizedKeys = [
            "transport.destination_missing",
            "transport.date_pending",
            "transport.departure.title",
            "transport.departure.placeholder",
            "transport.optimization.title",
            "transport.generated_plan.title",
            "transport.action.prepare_plan",
            "transport.action.choose_final_plan",
            "transport.locked.title"
        ]

        XCTAssertTrue(
            visibleCopy.contains { copy in containsAny(copy, localizedPrimaryCopy) } ||
                requiredLocalizedKeys.allSatisfy { transportSource.contains("String(localized: \"\($0)\")") },
            "TransportPlanningView primary state copy must be localized for French users. Found: \(visibleCopy)"
        )
        XCTAssertTrue(
            exposedEnglishPhrases.isEmpty,
            "TransportPlanningView must not expose a mostly-English primary flow. English primary phrases still visible: \(exposedEnglishPhrases)"
        )
    }

    func testReadinessSectionsAndStateBadgesUseStableLocalizationKeys() throws {
        let sources = try organizationSources()
        let expectedStableKeys = [
            "organization.section.participants",
            "organization.section.scenario",
            "organization.section.destination",
            "organization.section.lodging",
            "organization.section.transport",
            "organization.section.meetings",
            "organization.section.calendar",
            "organization.section.notifications",
            "organization.section.budget",
            "organization.section.payment",
            "organization.section.tricount",
            "organization.section.sync",
            "organization.section.unsafe_links",
            "organization.section.access_control",
            "organization.state.empty",
            "organization.state.optional_not_needed",
            "organization.state.incomplete",
            "organization.state.complete",
            "organization.state.pending_sync",
            "organization.state.failed_sync",
            "organization.state.access_denied"
        ]
        let missing = expectedStableKeys.filter { key in
            let snakeKey = key.replacingOccurrences(of: ".", with: "_")
            return !sources.contains(key) &&
                !sources.contains(snakeKey) &&
                !sources.contains(snakeKey.uppercased())
        }

        XCTAssertTrue(
            missing.isEmpty,
            "iOS Phase 7 organization UX must use stable keys/labels matching Android for sections and state badges. Missing: \(missing)"
        )
    }

    func testVisibleOrganizationCopyDoesNotExposeTechnicalContractMarkers() throws {
        let bannedVisibleMarkers = [
            "AccessDenied",
            "readOnly",
            "ReadOnly",
            "viewOnly",
            "mutationsDisabled",
            "pendingSync",
            "PendingSync",
            "not server confirmed",
            "queued for sync",
            "pending server confirmation",
            "server-confirmed"
        ]
        let bannedVisiblePatterns = [
            #"(?i)\bqueued\b"#,
            #"(?i)\bComing Soon\b"#,
            #"(?i)organization\.(section|state)\."#
        ]

        let violations = try visibleOrganizationCopyLiterals().filter { literal in
            bannedVisibleMarkers.contains { marker in literal.text.contains(marker) } ||
                bannedVisiblePatterns.contains { pattern in
                    literal.text.range(of: pattern, options: .regularExpression) != nil
                }
        }

        XCTAssertTrue(
            violations.isEmpty,
            "Visible iOS organization copy must be product-ready and must not expose technical contract markers. Violations: \(violations.map { $0.display })"
        )
    }

    func testMeetingCancellationIsOrganizerAndCurrentActorGated() throws {
        let meetingView = try readProjectFile("iosApp/src/Views/Meeting/MeetingListView.swift")
        let meetingViewModel = try readProjectFile("iosApp/src/ViewModels/MeetingListViewModel.swift")
        let swipeCancellation = slice(meetingView, from: ".swipeActions", to: ".refreshable")
        let cancelFunction = slice(meetingViewModel, from: "func cancelMeeting", to: "/// Generate a meeting link")

        XCTAssertTrue(
            containsAny(
                swipeCancellation,
                [
                    "if isOrganizer",
                    "guard isOrganizer",
                    "canCancelMeetings",
                    "canMutateMeetings",
                    "canCreateMeetings"
                ]
            ),
            "iOS meeting destructive swipe actions must be hidden or guarded unless the current actor is the organizer."
        )
        XCTAssertTrue(
            containsAny(
                cancelFunction,
                [
                    "currentUserId",
                    "actorId:",
                    "organizerId:",
                    "cancelledBy",
                    "CancelMeeting(meetingId: meetingId, currentUserId:"
                ]
            ),
            "MeetingListViewModel.cancelMeeting must carry the current actor into the cancellation path."
        )
    }

    func testMeetingDetailMutationsAreCurrentActorAndReadOnlyGated() throws {
        let detailView = try readProjectFile("iosApp/src/Views/Meeting/MeetingDetailView.swift")
        let viewModel = try readProjectFile("iosApp/src/ViewModels/MeetingListViewModel.swift")
        let initializer = slice(detailView, from: "init(meetingId: String", to: "var body: some View")
        let toolbar = slice(detailView, from: ".toolbar", to: ".sheet")
        let generateSheet = slice(detailView, from: "MeetingGenerateLinkSheet", to: ".confirmationDialog")
        let cancelDialog = slice(detailView, from: ".confirmationDialog", to: ".onAppear")
        let cancelFunction = slice(viewModel, from: "func cancelMeeting", to: "/// Generate a meeting link")
        let generateFunction = slice(viewModel, from: "func generateMeetingLink", to: "/// Select a meeting")

        XCTAssertTrue(
            containsAny(initializer + "\n" + detailView, ["currentUserId:", "currentUserId: String"]),
            "MeetingDetailView must receive the current actor id instead of relying on anonymous/default state."
        )
        XCTAssertTrue(
            containsAny(initializer + "\n" + detailView, ["isOrganizer:", "isOrganizer: Bool", "canManageMeetings", "canMutateMeetings"]),
            "MeetingDetailView must receive or derive explicit organizer/mutation permission."
        )
        XCTAssertTrue(
            containsAny(initializer + "\n" + detailView, ["isReadOnly:", "isReadOnly: Bool", "event.status", "EventStatus", ".finalized"]),
            "MeetingDetailView must receive or derive read-only/finalized status before rendering mutation actions."
        )
        XCTAssertTrue(
            containsAny(toolbar + "\n" + generateSheet, ["if canMutate", "if canManage", "if isOrganizer && !isReadOnly", ".disabled(!can", ".disabled(isReadOnly", "guard isOrganizer", "guard can"]),
            "MeetingDetailView must hide or disable Generate Link for non-organizers and read-only/finalized events."
        )
        XCTAssertTrue(
            containsAny(toolbar + "\n" + cancelDialog, ["if canMutate", "if canManage", "if isOrganizer && !isReadOnly", ".disabled(!can", ".disabled(isReadOnly", "guard isOrganizer", "guard can"]),
            "MeetingDetailView must hide or disable Cancel Meeting for non-organizers and read-only/finalized events."
        )
        XCTAssertFalse(
            cancelDialog.range(of: #"viewModel\.cancelMeeting\(\s*\)"#, options: .regularExpression) != nil,
            "MeetingDetailView must not call cancelMeeting without passing the current actor and authorization context."
        )
        XCTAssertFalse(
            generateSheet.range(of: #"viewModel\.generateMeetingLink\(\s*platform:\s*platform\s*\)"#, options: .regularExpression) != nil,
            "MeetingDetailView must not call generateMeetingLink without passing current actor, organizer, and read-only authorization context."
        )
        XCTAssertTrue(
            containsAny(cancelFunction, ["currentUserId:", "actorId:", "cancelledBy:", "MeetingManagementContractIntentCancelMeeting(meetingId: meetingId, currentUserId:"]),
            "MeetingListViewModel.cancelMeeting must forward the current actor into the shared cancellation intent/path."
        )
        XCTAssertTrue(
            containsAny(generateFunction, ["currentUserId:", "actorId:", "generatedBy:", "isOrganizer", "canMutate", "canManage", "isReadOnly"]),
            "MeetingListViewModel.generateMeetingLink must enforce or forward current actor/read-only authorization context."
        )
    }

    func testMeetingDetailRoutesPropagateCurrentActorOrganizerAndReadOnlyPolicy() throws {
        let contentView = try readProjectFile("iosApp/src/Views/App/ContentView.swift")
        let meetingListView = try readProjectFile("iosApp/src/Views/Meeting/MeetingListView.swift")
        let detailView = try readProjectFile("iosApp/src/Views/Meeting/MeetingDetailView.swift")
        let routes = regexMatches(
            in: contentView + "\n" + meetingListView,
            pattern: #"MeetingDetailView\s*\([\s\S]{0,420}\)"#
        )

        XCTAssertFalse(routes.isEmpty, "iOS organization routes must still navigate to MeetingDetailView.")

        let routePolicyFailures = routes.enumerated().compactMap { index, route -> String? in
            let hasActor = containsAny(route, ["currentUserId:", "actorId:", "userId:"])
            let hasOrganizerPolicy = containsAny(route, ["isOrganizer:", "canManageMeetings:", "canMutateMeetings:"])
            let hasReadOnlyPolicy = containsAny(route, ["isReadOnly:", "readOnly:", "event.status == .finalized", "status == .finalized"])
            return hasActor && hasOrganizerPolicy && hasReadOnlyPolicy ? nil : "route \(index + 1): \(route)"
        }

        XCTAssertTrue(
            routePolicyFailures.isEmpty,
            "Every MeetingDetailView route must pass the current actor, organizer/mutation policy, and read-only/finalized policy. Failures: \(routePolicyFailures)"
        )
        XCTAssertFalse(
            detailView.range(
                of: #"Menu\s*\{[\s\S]{0,900}(Générer un lien|meetings\.generate_link)[\s\S]{0,900}(Annuler la réunion|meetings\.cancel)"#,
                options: [.regularExpression, .caseInsensitive]
            ) != nil && !containsAny(detailView, ["canMutateMeetings", "canManageMeetings", "isOrganizer && !isReadOnly"]),
            "MeetingDetailView must not render Generate Link and Cancel Meeting in one ungated menu for confirmed non-organizers or FINALIZED events."
        )
    }

    func testMeetingListDoesNotUseDebugPreviewMeetingInitializerForRealDetailNavigation() throws {
        let meetingListView = try readProjectFile("iosApp/src/Views/Meeting/MeetingListView.swift")
        let destination = slice(meetingListView, from: ".navigationDestination", to: ".sheet")

        XCTAssertTrue(
            destination.contains("MeetingDetailView("),
            "MeetingListView must keep a real MeetingDetailView navigation destination for selected meetings."
        )
        XCTAssertFalse(
            destination.contains("previewMeeting:"),
            "MeetingListView must not call the DEBUG-only MeetingDetailView previewMeeting initializer for real meeting rows."
        )
    }

    func testMeetingDetailRealPathPropagatesEventIdIntoViewModel() throws {
        let detailView = try readProjectFile("iosApp/src/Views/Meeting/MeetingDetailView.swift")
        let initializer = slice(detailView, from: "init(\n        meetingId: String", to: "init(meetingId: String, eventId: String)")
        let viewModelConstruction = slice(initializer, from: "MeetingDetailViewModel(", to: ")")

        XCTAssertTrue(
            viewModelConstruction.contains("meetingId: meetingId"),
            "MeetingDetailView must construct MeetingDetailViewModel from the selected meeting id."
        )
        XCTAssertTrue(
            viewModelConstruction.contains("eventId: eventId"),
            "MeetingDetailView must pass eventId into MeetingDetailViewModel so direct detail navigation loads the event-scoped meeting."
        )
    }

    func testMeetingDetailViewModelStoresEventIdAndNeverLoadsAllMeetingsWithEmptyEventId() throws {
        let viewModel = try readProjectFile("iosApp/src/ViewModels/MeetingDetailViewModel.swift")
        let properties = slice(viewModel, from: "// MARK: - Private Properties", to: "// MARK: - Initialization")
        let initializer = slice(viewModel, from: "init(meetingId: String", to: "convenience init")
        let loadMeetings = slice(viewModel, from: "func loadMeetings()", to: "/// Update the meeting")

        XCTAssertTrue(
            properties.contains("private let eventId: String"),
            "MeetingDetailViewModel must store the eventId used to scope meeting detail loads."
        )
        XCTAssertTrue(
            initializer.contains("eventId: String") && initializer.contains("self.eventId = eventId"),
            "MeetingDetailViewModel must initialize and retain eventId instead of relying on an empty fallback."
        )
        XCTAssertFalse(
            loadMeetings.contains(#"eventId: """#),
            "MeetingDetailViewModel.loadMeetings must not dispatch LoadMeetings(eventId: \"\"); it must use the stored eventId."
        )
        XCTAssertTrue(
            loadMeetings.contains("eventId: eventId") || loadMeetings.contains("eventId: self.eventId"),
            "MeetingDetailViewModel.loadMeetings must dispatch LoadMeetings with the stored eventId."
        )
    }

    func testMeetingDeepLinksResolveParentEventBeforeOpeningDetail() throws {
        let content = try readProjectFile("iosApp/src/Views/App/ContentView.swift")
        let deepLinkSwitch = slice(content, from: "private func handleDeepLinkNavigation", to: "private func navigateToEvent")
        let meetingNavigation = slice(content, from: "private func navigateToMeeting", to: "private func isParticipantConfirmed")

        XCTAssertTrue(
            deepLinkSwitch.contains(#"case ("meeting", let meetingId?, _):"#),
            "ContentView must consume DeepLinkService meeting routes instead of dropping them."
        )
        XCTAssertTrue(
            deepLinkSwitch.contains("navigateToMeeting(meetingId: meetingId)"),
            "Meeting deep links must route through a dedicated resolver."
        )
        XCTAssertTrue(
            meetingNavigation.contains("meetingQueries") &&
                meetingNavigation.contains(".selectById(id: meetingId)") &&
                meetingNavigation.contains("meeting.eventId"),
            "Meeting deep links must resolve the parent event before constructing MeetingDetailView."
        )
        XCTAssertTrue(
            meetingNavigation.contains("selectedMeetingId = meetingId") &&
                meetingNavigation.contains("currentView = .meetingDetail"),
            "Meeting deep links must select the meeting and open the real meeting detail route."
        )
    }

    func testFinalizedPaymentAndTricountRoutesPassReadOnlyStateToMutationViews() throws {
        let content = try readProjectFile("iosApp/src/Views/App/ContentView.swift")
        let paymentRoute = slice(content, from: "case .paymentPot:", to: "case .tricount:")
        let tricountRoute = slice(content, from: "case .tricount:", to: "case .transportPlanning:")
        let paymentView = slice(content, from: "private struct PaymentPotView", to: "private struct TricountHandoffView")
        let tricountView = slice(content, from: "private struct TricountHandoffView", to: "private enum SafeURLOpener")

        let routeFailures = [
            ("PaymentPotView", paymentRoute),
            ("TricountHandoffView", tricountRoute)
        ].filter { _, routeSource in
            !containsAny(
                routeSource,
                [
                    "isReadOnly:",
                    "readOnly:",
                    "canMutate:",
                    "canManage:",
                    "event.status == .finalized",
                    "isFinalizedOrganizationState(event)"
                ]
            )
        }.map { name, _ in name }

        XCTAssertTrue(
            routeFailures.isEmpty,
            "iOS payment and Tricount routes must pass finalized/read-only state into mutation views. Missing: \(routeFailures)"
        )

        let viewFailures = [
            ("PaymentPotView", paymentView),
            ("TricountHandoffView", tricountView)
        ].filter { _, viewSource in
            !containsAny(viewSource, ["isReadOnly", "readOnly", "canMutate", "canManage", "mutationsEnabled"])
        }.map { name, _ in name }

        XCTAssertTrue(
            viewFailures.isEmpty,
            "iOS payment and Tricount views must expose explicit read-only mutation guards for FINALIZED events. Missing: \(viewFailures)"
        )

        let organizerOnlyMutationButtons = regexMatches(
            in: paymentView + "\n" + tricountView,
            pattern: #"Button\("(Créer une cagnotte|Activer la cagnotte|Clôturer la cagnotte|Associer Tricount|Dissocier Tricount|Tricount non requis)"[\s\S]{0,180}\.disabled\(!isOrganizer\)"#
        )

        XCTAssertTrue(
            organizerOnlyMutationButtons.isEmpty,
            "FINALIZED iOS payment/Tricount mutations must not be disabled only for non-organizers. Buttons found: \(organizerOnlyMutationButtons)"
        )
    }

    func testFinalizedEventsKeepTransportPlanningReachableReadOnlyOnIOS() throws {
        let content = try readProjectFile("iosApp/src/Views/App/ContentView.swift")
        let transportAccess = slice(
            content,
            from: "private var canAccessTransportPlanning: Bool",
            to: "private var canShowOrganizationDashboard: Bool"
        )
        let transportRoute = slice(content, from: "case .transportPlanning:", to: "case .inbox:")

        XCTAssertTrue(
            transportAccess.contains("case .finalized"),
            "iOS finalized events must keep Transport reachable from dashboard/menu for read-only consultation."
        )
        XCTAssertTrue(
            containsAny(
                transportRoute,
                [
                    "isReadOnly: isFinalizedOrganizationState(event)",
                    "isReadOnly: event.status == .finalized",
                    "readOnly: isFinalizedOrganizationState(event)"
                ]
            ),
            "iOS transport route must pass read-only state when FINALIZED details are opened."
        )
    }

    func testIOSPaymentAndTricountRoutesRequirePhase5WorkflowStatus() throws {
        let content = try readProjectFile("iosApp/src/Views/App/ContentView.swift")
        let paymentRoute = slice(content, from: "case .paymentPot:", to: "case .tricount:")
        let tricountRoute = slice(content, from: "case .tricount:", to: "case .transportPlanning:")
        let routes = [
            ("paymentPot", paymentRoute),
            ("tricount", tricountRoute)
        ]

        let missingWorkflowGate = routes.filter { _, routeSource in
            !containsAny(
                routeSource,
                [
                    "canEnterOrganizationRoutes",
                    "canAccessPhase5Organization",
                    "canMutatePhase5Organization",
                    "organizationDashboardVisible",
                    "canShowOrganizationDashboard",
                    "event.status == .organizing",
                    "event.status == .finalized",
                    "case .organizing",
                    "case .finalized"
                ]
            )
        }.map { name, _ in name }
        let detailOnlyGates = routes.filter { _, routeSource in
            routeSource.range(
                of: #"if\s+canAccessOrganizationDetails\(for:\s*event\)\s*\{"#,
                options: .regularExpression
            ) != nil
        }.map { name, _ in name }

        XCTAssertTrue(
            missingWorkflowGate.isEmpty,
            "iOS payment/Tricount direct routes must require ORGANIZING mutation access and allow FINALIZED only as read-only. Missing workflow gate: \(missingWorkflowGate)"
        )
        XCTAssertTrue(
            detailOnlyGates.isEmpty,
            "iOS payment/Tricount direct routes must not rely only on confirmed-participant detail access before ORGANIZING. Detail-only gates: \(detailOnlyGates)"
        )
    }

    func testIOSPaymentAndTricountMutationGuardsRequireOrganizingAndFinalizedReadOnly() throws {
        let content = try readProjectFile("iosApp/src/Views/App/ContentView.swift")
        let paymentRoute = slice(content, from: "case .paymentPot:", to: "case .tricount:")
        let tricountRoute = slice(content, from: "case .tricount:", to: "case .transportPlanning:")
        let paymentView = slice(content, from: "private struct PaymentPotView", to: "private struct TricountHandoffView")
        let tricountView = slice(content, from: "private struct TricountHandoffView", to: "private enum SafeURLOpener")

        let routeMutationStateFailures = [
            ("PaymentPotView", paymentRoute),
            ("TricountHandoffView", tricountRoute)
        ].filter { _, routeSource in
            !containsAny(
                routeSource,
                [
                    "canManagePayment:",
                    "canManageTricount:",
                    "canMutate:",
                    "canManage:",
                    "event.status == .organizing",
                    "canEnterOrganizationRoutes",
                    "isReadOnly: isFinalizedOrganizationState(event)"
                ]
            )
        }.map { name, _ in name }

        let finalizedOnlyMutationGuards = [
            ("PaymentPotView", paymentView),
            ("TricountHandoffView", tricountView)
        ].filter { _, viewSource in
            viewSource.contains("self.canManagePayment = isOrganizer && !isReadOnly") ||
                viewSource.contains("self.canManageTricount = isOrganizer && !isReadOnly")
        }.map { name, _ in name }

        XCTAssertTrue(
            routeMutationStateFailures.isEmpty,
            "iOS payment/Tricount routes must pass explicit mutation state derived from ORGANIZING workflow access, not only isOrganizer/read-only. Missing: \(routeMutationStateFailures)"
        )
        XCTAssertTrue(
            finalizedOnlyMutationGuards.isEmpty,
            "iOS payment/Tricount views must not compute mutations from isOrganizer && !isReadOnly only; pre-ORGANIZING states must also be non-mutable. Offenders: \(finalizedOnlyMutationGuards)"
        )
    }

    func testEventDetailPaymentAndTricountRowsExposeRealReadinessState() throws {
        let content = try readProjectFile("iosApp/src/Views/App/ContentView.swift")
        let eventDetail = slice(content, from: "private struct EventDetailView", to: "private struct EventDetailHero")

        XCTAssertTrue(
            containsAny(eventDetail, ["paymentPotSummaryValue()", "getActivePotForEvent(eventId:", "event.detail.payment_pot.define_before_share"]),
            "Event detail payment row must summarize the actual payment-pot readiness instead of a generic shared-pot label."
        )
        XCTAssertTrue(
            containsAny(eventDetail, ["tricountSummaryValue()", "getPaymentReadiness(eventId:", "event.detail.tricount.decide_before_expenses"]),
            "Event detail Tricount row must summarize verified/not-needed/missing handoff state instead of generic secure-handoff copy."
        )
        XCTAssertFalse(
            eventDetail.contains(#"organizationDashboardValue("Cagnotte commune")"#),
            "Event detail must not advertise a common pot when no configured goal exists."
        )
        XCTAssertFalse(
            eventDetail.contains(#"organizationDashboardValue("Handoff sécurisé")"#),
            "Event detail must not advertise a secure handoff before the Tricount readiness state is known."
        )
    }

    private func organizationSources() throws -> String {
        let fileBoundary = "\n" + String(repeating: "#", count: 901) + "\n"
        return try [
            "iosApp/src/Views/App/ContentView.swift",
            "iosApp/src/Views/Events/ScenarioOrganizationView.swift",
            "iosApp/src/Views/Events/TransportPlanningView.swift",
            "iosApp/src/Views/Meeting/MeetingListView.swift",
            "iosApp/src/Views/Budget/BudgetOverviewView.swift",
            "iosApp/src/Views/Budget/BudgetDetailView.swift",
            "iosApp/src/ViewModels/MeetingListViewModel.swift",
            "iosApp/src/ViewModels/BudgetViewModel.swift",
            "iosApp/src/ViewModels/TransportPlanningViewModel.swift"
        ].map { try readProjectFile($0) }.joined(separator: fileBoundary)
    }

    private func visibleOrganizationCopy() throws -> [String] {
        return try [
            "iosApp/src/Views/App/ContentView.swift",
            "iosApp/src/Views/Events/ScenarioOrganizationView.swift",
            "iosApp/src/Views/Events/TransportPlanningView.swift",
            "iosApp/src/Views/Meeting/MeetingListView.swift",
            "iosApp/src/Views/Budget/BudgetOverviewView.swift",
            "iosApp/src/Views/Budget/BudgetDetailView.swift"
        ].flatMap { path in
            try extractVisibleStrings(from: readProjectFile(path))
        }
    }

    private func readProjectFile(_ relativePath: String) throws -> String {
        var directory = URL(fileURLWithPath: #filePath)
            .deletingLastPathComponent()

        while true {
            let candidate = directory.appendingPathComponent(relativePath)
            if FileManager.default.fileExists(atPath: candidate.path) {
                return try String(contentsOf: candidate, encoding: .utf8)
            }

            let parent = directory.deletingLastPathComponent()
            if parent.path == directory.path {
                throw NSError(
                    domain: "OrganizationPhase7ContractTests",
                    code: 1,
                    userInfo: [NSLocalizedDescriptionKey: "Could not find project file: \(relativePath)"]
                )
            }
            directory = parent
        }
    }

    private func slice(_ source: String, from startMarker: String, to endMarker: String) -> String {
        guard let startRange = source.range(of: startMarker) else {
            return source
        }
        let tail = source[startRange.lowerBound...]
        guard let endRange = tail.range(of: endMarker) else {
            return String(tail)
        }
        return String(tail[..<endRange.lowerBound])
    }

    private func containsAny(_ source: String, _ candidates: [String]) -> Bool {
        candidates.contains { candidate in
            source.range(of: candidate, options: [.caseInsensitive, .diacriticInsensitive]) != nil
        }
    }

    private func extractVisibleStrings(from source: String) throws -> [String] {
        let patterns = [
            #"\bText\s*\(\s*"((?:\\"|[^"])*)""#,
            #"\bLabel\s*\(\s*"((?:\\"|[^"])*)""#,
            #"\bButton\s*\(\s*"((?:\\"|[^"])*)""#,
            #"\bTextField\s*\(\s*"((?:\\"|[^"])*)""#,
            #"\bWakeveActionButton\s*\(\s*"((?:\\"|[^"])*)""#,
            #"\.alert\s*\(\s*"((?:\\"|[^"])*)""#,
            #"\baccessibilityLabel\s*:\s*"((?:\\"|[^"])*)""#
        ]
        return try patterns.flatMap { pattern -> [String] in
            try regexCaptureGroups(in: source, pattern: pattern)
        }.removingDuplicates()
    }

    private func regexCaptureGroups(in source: String, pattern: String) throws -> [String] {
        let regex = try NSRegularExpression(pattern: pattern)
        let range = NSRange(source.startIndex..<source.endIndex, in: source)
        return regex.matches(in: source, options: [], range: range).compactMap { match in
            guard match.numberOfRanges > 1,
                  let matchRange = Range(match.range(at: 1), in: source) else {
                return nil
            }
            return String(source[matchRange])
        }
    }

    private func regexMatches(in source: String, pattern: String) -> [String] {
        guard let regex = try? NSRegularExpression(pattern: pattern, options: [.caseInsensitive]) else {
            return []
        }
        let range = NSRange(source.startIndex..<source.endIndex, in: source)
        return regex.matches(in: source, options: [], range: range).compactMap { match in
            guard let matchRange = Range(match.range, in: source) else { return nil }
            return String(source[matchRange])
        }
    }

    private func visibleOrganizationCopyLiterals() throws -> [VisibleCopyLiteral] {
        let sourceFiles = [
            "iosApp/src/Views/App/ContentView.swift",
            "iosApp/src/Views/Events/TransportPlanningView.swift",
            "iosApp/src/Views/Meeting/MeetingListView.swift",
            "iosApp/src/Views/Budget/BudgetOverviewView.swift",
            "iosApp/src/ViewModels/TransportPlanningViewModel.swift",
            "iosApp/src/ViewModels/MeetingListViewModel.swift",
            "iosApp/src/ViewModels/BudgetViewModel.swift"
        ]
        let visibleApiPattern = #"\b(Text|Button|Label|Section|ErrorView|AccessDenied)\s*\("#
        let visibleStatePattern = #"\b(statusText|errorMessage|message)\s*="#

        return try sourceFiles.flatMap { relativePath in
            let source = try readProjectFile(relativePath)
            let lines = source.components(separatedBy: .newlines)
            var visibleRanges: [ClosedRange<Int>] = []

            for (index, line) in lines.enumerated() {
                if line.range(of: visibleApiPattern, options: .regularExpression) != nil ||
                    line.range(of: visibleStatePattern, options: .regularExpression) != nil {
                    visibleRanges.append(index...min(index + 10, lines.count - 1))
                }
            }

            return lines.enumerated().flatMap { index, line in
                guard visibleRanges.contains(where: { $0.contains(index) }) else {
                    return [VisibleCopyLiteral]()
                }

                return extractStringLiterals(from: line)
                    .filter { isLikelyVisibleCopy($0) }
                    .map { VisibleCopyLiteral(relativePath: relativePath, line: index + 1, text: $0) }
            }
        }
    }

    private func extractStringLiterals(from line: String) -> [String] {
        guard let regex = try? NSRegularExpression(pattern: #""([^"\\]*(?:\\.[^"\\]*)*)""#) else {
            return []
        }
        let range = NSRange(line.startIndex..<line.endIndex, in: line)
        return regex.matches(in: line, range: range).compactMap { match in
            guard match.numberOfRanges > 1,
                  let matchRange = Range(match.range(at: 1), in: line) else {
                return nil
            }
            return String(line[matchRange])
        }
    }

    private func isLikelyVisibleCopy(_ text: String) -> Bool {
        let trimmed = text.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else {
            return false
        }
        guard !trimmed.hasPrefix("http://"), !trimmed.hasPrefix("https://") else {
            return false
        }
        return trimmed.rangeOfCharacter(from: .letters) != nil
    }

    private struct VisibleCopyLiteral {
        let relativePath: String
        let line: Int
        let text: String

        var display: String {
            "\(relativePath):\(line) \"\(text)\""
        }
    }
}

private extension Array where Element == String {
    func removingDuplicates() -> [String] {
        var seen = Set<String>()
        return filter { seen.insert($0).inserted }
    }
}
