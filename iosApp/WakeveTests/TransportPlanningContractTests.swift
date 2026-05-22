import XCTest

final class TransportPlanningContractTests: XCTestCase {
    func testTransportAccessUsesConfirmedParticipantStateForNonOrganizer() throws {
        let source = try readProjectFile("iosApp/src/Views/App/ContentView.swift")

        XCTAssertFalse(
            source.contains("isParticipantConfirmed: event.organizerId == userId ? true : nil"),
            "Transport access must not lock every non-organizer behind nil participant confirmation."
        )
        XCTAssertTrue(
            source.contains("canAccessOrganizationDetails") ||
                source.contains("DateValidationState") ||
                source.contains("participantAccess"),
            "Transport navigation must derive non-organizer access from confirmed participant state."
        )
    }

    func testTransportPlanningViewDoesNotUseHardcodedLocalPlanningState() throws {
        let source = try readProjectFile("iosApp/src/Views/Events/TransportPlanningView.swift")

        XCTAssertFalse(
            source.contains("@State private var missingDeparture = [\"Participant 1\", \"Participant 2\"]"),
            "Missing departures must come from shared/repository state, not hardcoded sample names."
        )
        XCTAssertFalse(
            source.contains("generatedPlan = \"\\(selectedOptimization.rawValue)-plan\""),
            "Plan generation must call shared/repository state, not synthesize local plan IDs."
        )
        XCTAssertTrue(
            source.contains("TransportReadiness") ||
                source.contains("TransportRepository") ||
                source.contains("TransportPlanningViewModel"),
            "The iOS transport screen must consume real readiness and plan state."
        )
    }

    func testContentViewPassesRepositoryBackedTransportStateAndCallbacks() throws {
        let source = try readProjectFile("iosApp/src/Views/App/ContentView.swift")
        let transportCase = slice(
            source,
            from: "case .transportPlanning:",
            to: "case .inbox:"
        )
        let presentationFactory = slice(
            source,
            from: "private func makeTransportPlanningState",
            to: "private func transportDestination"
        )

        XCTAssertFalse(
            transportCase.contains("readiness: nil"),
            "ContentView must pass repository/shared TransportReadiness instead of readiness: nil."
        )
        XCTAssertFalse(
            presentationFactory.contains("plans: []"),
            "ContentView must pass persisted/generated transport plans instead of hardcoded empty plans."
        )
        XCTAssertFalse(
            presentationFactory.contains("pendingSync: false"),
            "ContentView must derive pendingSync from transport sync metadata instead of a false constant."
        )
        XCTAssertFalse(
            transportCase.contains("print(\"Generate transport plan requested") ||
                transportCase.contains("print(\"Final transport plan selected") ||
                transportCase.contains("print(\"Transport not needed requested"),
            "Transport callbacks must mutate repository/shared state and reload presentation state; print-only callbacks are stubs."
        )
        XCTAssertTrue(
            transportCase.contains("TransportRepository") ||
                transportCase.contains("TransportPlanningViewModel") ||
                transportCase.contains("sharedTransport") ||
                transportCase.contains("repository.generateTransport") ||
                transportCase.contains("repository.selectTransport") ||
                transportCase.contains("repository.markTransport"),
            "ContentView must wire TransportPlanningView actions to real transport persistence."
        )
    }

    func testTransportPlanningViewRequiresReadOnlyWorkflowStatusAndSelectedDestinationContracts() throws {
        let source = try readProjectFile("iosApp/src/Views/Events/TransportPlanningView.swift")
        let signature = slice(
            source,
            from: "struct TransportPlanningView: View",
            to: "@State private var selectedOptimization"
        )

        XCTAssertTrue(
            signature.contains("let isReadOnly: Bool") ||
                signature.contains("let readOnly: Bool"),
            "TransportPlanningView must receive explicit read-only mode for FINALIZED events."
        )
        XCTAssertTrue(
            signature.contains("let eventStatus: EventStatus") ||
                signature.contains("let workflowStatus: EventStatus"),
            "TransportPlanningView must receive workflow status instead of deriving mutability from organizer/readiness alone."
        )
        XCTAssertTrue(
            signature.contains("let selectedDestination: TransportLocation?") ||
                signature.contains("let destination: TransportLocation?"),
            "TransportPlanningView must receive a nullable selected destination object, not a non-null display string."
        )
        XCTAssertFalse(
            signature.contains("let destination: String"),
            "A destination label string cannot prove a selected scenario exists."
        )
    }

    func testTransportPlanningViewDisablesAllMutationsWhenReadOnlyFinalizedOrDestinationMissing() throws {
        let source = try readProjectFile("iosApp/src/Views/Events/TransportPlanningView.swift")
        let optimizationCard = slice(
            source,
            from: "private var optimizationCard",
            to: "private var generatedPlanCard"
        )
        let generatedPlanCard = slice(
            source,
            from: "private var generatedPlanCard",
            to: "private var lockedState"
        )

        XCTAssertTrue(
            optimizationCard.contains("isReadOnly") ||
                optimizationCard.contains("readOnly"),
            "Generate and mark-not-needed buttons must include read-only mode in their disabled policy."
        )
        XCTAssertTrue(
            generatedPlanCard.contains("isReadOnly") ||
                generatedPlanCard.contains("readOnly"),
            "Select-final buttons must include read-only mode in their disabled policy."
        )
        XCTAssertTrue(
            optimizationCard.contains("selectedDestination != nil") ||
                optimizationCard.contains("destination != nil"),
            "Generate and mark-not-needed buttons must be disabled until a selected destination exists."
        )
        XCTAssertTrue(
            generatedPlanCard.contains("selectedDestination != nil") ||
                generatedPlanCard.contains("destination != nil"),
            "Select-final buttons must be disabled until a selected destination exists."
        )
        XCTAssertTrue(
            optimizationCard.contains(".FINALIZED") ||
                generatedPlanCard.contains(".FINALIZED") ||
                optimizationCard.contains("eventStatus") ||
                generatedPlanCard.contains("eventStatus") ||
                optimizationCard.contains("workflowStatus") ||
                generatedPlanCard.contains("workflowStatus"),
            "TransportPlanningView must disable mutations in FINALIZED/read-only workflow state."
        )
    }

    func testTransportDestinationIsLoadedFromSelectedScenarioRepositoryState() throws {
        let contentView = try readProjectFile("iosApp/src/Views/App/ContentView.swift")
        let viewModel = try readProjectFile("iosApp/src/ViewModels/TransportPlanningViewModel.swift")
        let transportCase = slice(
            contentView,
            from: "case .transportPlanning:",
            to: "case .inbox:"
        )
        let destinationFactory = slice(
            contentView,
            from: "private func transportDestination",
            to: "// MARK: - Explore Tab View"
        )

        let contentLoadsSelectedScenario =
            containsAny(destinationFactory, [
                "getScenariosByEventIdAndStatus",
                "getSelectedScenario",
                "selectedScenarioRepository",
                "loadSelectedScenario"
            ]) &&
            containsAny(destinationFactory, [
                "ScenarioStatus.selected",
                ".selected",
                "SELECTED"
            ])
        let viewModelLoadsSelectedScenario =
            containsAny(viewModel, [
                "getScenariosByEventIdAndStatus",
                "getSelectedScenario",
                "selectedScenarioRepository",
                "loadSelectedScenario"
            ]) &&
            containsAny(viewModel, [
                "ScenarioStatus.selected",
                ".selected",
                "SELECTED"
            ])

        XCTAssertTrue(
            contentLoadsSelectedScenario || viewModelLoadsSelectedScenario,
            "Transport selectedDestination must be loaded from a repository/local scenario whose status is SELECTED, not from an unassigned selectedScenario state variable."
        )
        XCTAssertTrue(
            transportCase.contains("selectedDestination: selectedDestination") &&
                transportCase.contains("transportPlanningViewModel.load"),
            "The transport screen must reload presentation state with the selected scenario destination before rendering transport readiness and actions."
        )
    }

    func testTransportPlanningViewModelExposesDepartureLocationWriterContract() throws {
        let source = try readProjectFile("iosApp/src/ViewModels/TransportPlanningViewModel.swift")
        let exposesWriter = source.range(
            of: #"func\s+(save|set|update)[A-Za-z]*Departure[A-Za-z]*\s*\("#,
            options: .regularExpression
        ) != nil

        XCTAssertTrue(
            exposesWriter,
            "TransportPlanningViewModel must expose a public writer for the user's departure point."
        )
        XCTAssertTrue(
            source.contains("participantId") &&
                source.contains("TransportLocation") &&
                containsAny(source, ["saveDepartureLocation", "setDepartureLocation", "updateDepartureLocation"]) &&
                source.contains("state = makeState") &&
                source.contains("selectedDestination"),
            "The departure writer contract must persist a participant TransportLocation, mark pending sync, and refresh readiness using the selected destination."
        )
    }

    func testTransportPlanningViewModelPersistsThroughSharedRepositoryNotUserDefaultsOnlyStore() throws {
        let source = try readProjectFile("iosApp/src/ViewModels/TransportPlanningViewModel.swift")

        XCTAssertFalse(
            source.contains("UserDefaults.standard") ||
                source.contains("TransportPlanningLocalStore.shared") ||
                source.contains("private final class TransportPlanningLocalStore"),
            "TransportPlanningViewModel must not persist transport plans, departures, selected plan, or not-needed state in an iOS-only UserDefaults store."
        )
        XCTAssertTrue(
            containsAny(source, [
                "TransportRepository",
                "SharedTransportRepository",
                "TransportOfflineRepository",
                "RepositoryProvider.shared.transport",
                "saveDepartureLocation(",
                "selectFinalPlan(",
                "markTransportNotNeeded("
            ]),
            "TransportPlanningViewModel must depend on the shared transport repository/sync path used by Kotlin Multiplatform persistence."
        )
        XCTAssertTrue(
            source.contains("repository:") &&
                containsAny(source, [
                    "transportRepository",
                    "TransportRepository",
                    "SharedTransportRepository"
                ]),
            "The transport repository dependency must be injectable/testable instead of hidden behind a singleton local store."
        )
    }

    func testTransportPlanningPendingSyncIsReplayableOperationNotVisualBooleanFlag() throws {
        let source = try readProjectFile("iosApp/src/ViewModels/TransportPlanningViewModel.swift")

        XCTAssertFalse(
            source.contains("func hasPendingSync(eventId: String) -> Bool") ||
                source.contains("pendingSyncKey(") ||
                source.contains("defaults.set(true, forKey: pendingSyncKey(eventId))"),
            "Pending sync for iOS transport must not be a non-replayable visual boolean flag in UserDefaults."
        )
        XCTAssertTrue(
            containsAny(source, [
                "PendingSyncOperation",
                "replayPendingSync",
                "syncMetadata",
                "queueSync",
                "SyncOperationType"
            ]),
            "TransportPlanningViewModel must expose or consume replayable pending sync operations for offline transport mutations."
        )
        XCTAssertTrue(
            containsAny(source, [
                "transport_departure_location",
                "transport_plan_selection",
                "transport_event_status",
                "transport_plan"
            ]),
            "Replayable transport sync must preserve operation identity, not only a screen-level pendingSync flag."
        )
    }

    func testTransportPlanningViewModelPendingSyncExcludesAuditOnlyConflictResolutionRows() throws {
        let source = try readProjectFile("iosApp/src/ViewModels/TransportPlanningViewModel.swift")
        let pendingSyncFunction = slice(
            source,
            from: "private func hasReplayablePendingSync",
            to: "\n    }\n}"
        )

        XCTAssertFalse(
            pendingSyncFunction.contains("selectPending()") &&
                !pendingSyncFunction.contains("CONFLICT_RESOLVED") &&
                !pendingSyncFunction.contains("hasPendingTransportSync"),
            "iOS TransportPlanningViewModel.hasReplayablePendingSync must not count raw selectPending() rows without excluding audit-only CONFLICT_RESOLVED metadata or delegating to the shared repository pending API."
        )
        XCTAssertTrue(
            containsAny(pendingSyncFunction, [
                "operation.operation != \"CONFLICT_RESOLVED\"",
                "operation.operation == \"CREATE\"",
                "operation.operation == \"UPDATE\"",
                "operation.operation == \"DELETE\"",
                "hasPendingTransportSync"
            ]),
            "iOS transport pending sync must expose only operations that replayPendingSync can actually send."
        )
    }

    func testTransportPlanningViewBindsDepartureInputActionToViewModelWriter() throws {
        let view = try readProjectFile("iosApp/src/Views/Events/TransportPlanningView.swift")
        let contentView = try readProjectFile("iosApp/src/Views/App/ContentView.swift")
        let signature = slice(
            view,
            from: "struct TransportPlanningView: View",
            to: "@State private var selectedOptimization"
        )
        let transportCase = slice(
            contentView,
            from: "case .transportPlanning:",
            to: "case .inbox:"
        )

        XCTAssertTrue(
            containsAny(signature, [
                "onSaveDepartureLocation",
                "onSetDepartureLocation",
                "onUpdateDepartureLocation"
            ]),
            "TransportPlanningView must receive a departure save/update callback from ContentView or its ViewModel."
        )
        XCTAssertTrue(
            view.contains("TextField(") &&
                containsAny(view, ["departure", "Departure", "point de depart", "depart"]) &&
                view.contains("TransportLocation("),
            "TransportPlanningView must provide an editable departure input that creates a TransportLocation from user-entered data."
        )
        XCTAssertTrue(
            containsAny(transportCase, [
                "onSaveDepartureLocation",
                "onSetDepartureLocation",
                "onUpdateDepartureLocation"
            ]) &&
                containsAny(transportCase, [
                    "transportPlanningAdapter.save",
                    "transportPlanningAdapter.set",
                    "transportPlanningAdapter.update"
                ]),
            "ContentView must connect the departure input action to TransportPlanningViewModel instead of leaving departure data read-only."
        )
    }

    private func readProjectFile(_ relativePath: String) throws -> String {
        var directory = URL(fileURLWithPath: #filePath)
            .deletingLastPathComponent()

        for _ in 0..<8 {
            let candidate = directory.appendingPathComponent(relativePath)
            if FileManager.default.fileExists(atPath: candidate.path) {
                return try String(contentsOf: candidate, encoding: .utf8)
            }
            directory.deleteLastPathComponent()
        }

        XCTFail("Could not locate project file \(relativePath)")
        return ""
    }

    private func slice(_ source: String, from startMarker: String, to endMarker: String) -> String {
        guard let start = source.range(of: startMarker) else {
            XCTFail("Missing source marker \(startMarker)")
            return ""
        }
        guard let end = source[start.lowerBound...].range(of: endMarker) else {
            XCTFail("Missing source marker \(endMarker)")
            return String(source[start.lowerBound...])
        }
        return String(source[start.lowerBound..<end.lowerBound])
    }

    private func containsAny(_ source: String, _ candidates: [String]) -> Bool {
        candidates.contains { source.contains($0) }
    }
}
