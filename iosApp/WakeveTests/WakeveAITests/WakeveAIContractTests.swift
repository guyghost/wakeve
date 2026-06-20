import XCTest
@testable import Wakeve

final class WakeveAIContractTests: XCTestCase {
    func testWakeveAIModuleContainsRequiredBoundaries() throws {
        let root = try projectRoot()
        let required = [
            "iosApp/src/WakeveAI/WakeveAIAvailabilityService.swift",
            "iosApp/src/WakeveAI/WakeveAIClient.swift",
            "iosApp/src/WakeveAI/WakeveAIModels.swift",
            "iosApp/src/WakeveAI/WakeveAIValidation.swift",
            "iosApp/src/WakeveAI/Generators/WakeveAIGenerators.swift",
            "iosApp/src/WakeveAI/Tools/WakeveAITools.swift",
            "iosApp/src/WakeveAI/Prompts/WakeveAIPromptCatalog.swift",
            "iosApp/src/WakeveAI/Instrumentation/WakeveAIMetrics.swift"
        ]

        for path in required {
            XCTAssertTrue(FileManager.default.fileExists(atPath: root.appendingPathComponent(path).path), "Missing \(path)")
        }
    }

    func testCreateEventSmartDraftUsesViewModelAndExplicitActions() throws {
        let source = try readProjectFile("iosApp/src/Views/Events/CreateEventSheet.swift")

        XCTAssertTrue(source.contains("viewModel.generateSmartEventDraft()"))
        XCTAssertTrue(source.contains("viewModel.cancelSmartEventDraft()"))
        XCTAssertTrue(source.contains("viewModel.ignoreSmartEventDraft()"))
        XCTAssertTrue(source.contains("String(localized: \"common.edit\")"))
        XCTAssertTrue(source.contains("String(localized: \"common.apply\")"))
        XCTAssertTrue(source.contains("String(localized: \"create_event.ai.ignore\")"))
        XCTAssertTrue(source.contains("applySmartEventDraft"))
        XCTAssertFalse(source.contains("LanguageModelSession("), "SwiftUI views must not own Foundation Models sessions.")
    }

    func testCreateEventViewModelOwnsSmartDraftGeneration() throws {
        let source = try readProjectFile("iosApp/src/ViewModels/CreateEventViewModel.swift")

        XCTAssertTrue(source.contains("@Published var smartEventDraftState"))
        XCTAssertTrue(source.contains("EventDraftGenerator"))
        XCTAssertTrue(source.contains("generateSmartEventDraft()"))
        XCTAssertTrue(source.contains("cancelSmartEventDraft()"))
    }

    func testCreateEventViewModelHandlesSmartDraftFallbackTimeoutCancellationAndStreaming() throws {
        let source = try readProjectFile("iosApp/src/ViewModels/CreateEventViewModel.swift")

        XCTAssertTrue(source.contains("catch WakeveAIError.unavailable"))
        XCTAssertTrue(source.contains(".unavailable(availability)"))
        XCTAssertTrue(source.contains("catch WakeveAIError.timedOut"))
        XCTAssertTrue(source.contains("La suggestion prend trop de temps"))
        XCTAssertTrue(source.contains("catch WakeveAIError.cancelled"))
        XCTAssertTrue(source.contains("catch is CancellationError"))
        XCTAssertTrue(source.contains(".cancelled"))
        XCTAssertTrue(source.contains("case .title"))
        XCTAssertTrue(source.contains("case .description"))
        XCTAssertTrue(source.contains("case .dateOptions"))
        XCTAssertTrue(source.contains("case .checklist"))
        XCTAssertTrue(source.contains("case .suggestedPolls"))
        XCTAssertTrue(source.contains("case .completed"))
    }

    func testWakeveAIClientWrapsFoundationModelsWithProductionGuards() throws {
        let source = try readProjectFile("iosApp/src/WakeveAI/WakeveAIClient.swift")
        let foundationClient = slice(source, from: "struct FoundationModelsWakeveAIClient", to: "@available(iOS 26.0, *)\n@Generable")

        XCTAssertTrue(foundationClient.contains("LanguageModelSession(instructions: prompt.system)"))
        XCTAssertTrue(foundationClient.contains("generating: FoundationEventDraft.self"))
        XCTAssertTrue(foundationClient.contains("withClientTimeout"))
        XCTAssertTrue(foundationClient.contains("Task.checkCancellation()"))
        XCTAssertTrue(foundationClient.contains("WakeveAIMetrics"))
        XCTAssertTrue(foundationClient.contains("WakeveAILogger.debug"))
        XCTAssertTrue(foundationClient.contains("knownFacts.nonEmptyCategoryCount"))
        XCTAssertFalse(foundationClient.contains("debugPersonalContext"), "Production client must not log personal prompt context by default.")
    }

    func testCreateEventSheetKeepsSmartDraftApplyAndIgnoreExplicit() throws {
        let source = try readProjectFile("iosApp/src/Views/Events/CreateEventSheet.swift")
        let applyBlock = slice(source, from: "private func applySmartEventDraft", to: "private func darkenColor")

        XCTAssertTrue(applyBlock.contains("title = draft.title"))
        XCTAssertTrue(applyBlock.contains("description = draft.description"))
        XCTAssertTrue(applyBlock.contains("selectedLocation = location"))
        XCTAssertTrue(applyBlock.contains("expectedParticipants"))
        XCTAssertTrue(applyBlock.contains("viewModel.ignoreSmartEventDraft()"))
        XCTAssertFalse(applyBlock.contains("viewModel.createEvent("), "Applying a draft must not create the event automatically.")
    }

    func testCreateEventSmartDraftMaterializesDatesAndChecklistIntoWizardState() throws {
        let source = try readProjectFile("iosApp/src/Views/Events/CreateEventSheet.swift")
        let applyBlock = slice(source, from: "private func applySmartEventDraft", to: "private func applyInitialScenarioIfNeeded")
        let confirmStep = slice(source, from: "private var confirmStep", to: "private var createBottomAction")

        XCTAssertTrue(
            applyBlock.contains("materializeSmartDateOptions(draft.dateOptions)") &&
            applyBlock.contains("mergeSmartSlotDrafts(smartSlots)"),
            "Applying a smart event draft must turn AI date options into editable proposed slots."
        )
        XCTAssertTrue(
            applyBlock.contains("appliedSmartChecklist = Array(draft.checklist.prefix(3))"),
            "Applying a smart event draft must carry checklist items into the creation flow instead of only previewing text."
        )
        XCTAssertTrue(
            source.contains("preparedCreationChecklist") &&
            source.contains("preparedChecklist: preparedCreationChecklist"),
            "Creating an event from an AI/template plan must carry prepared checklist items into the post-creation context."
        )
        XCTAssertTrue(
            confirmStep.contains("smartAppliedPlanSummary"),
            "The confirmation step must surface AI-created dates/checklist before previewing the invitation."
        )
    }

    func testCreateEventSmartDraftDateParsingUsesCurrentUserLocale() throws {
        let source = try readProjectFile("iosApp/src/Views/Events/CreateEventSheet.swift")
        let parser = slice(source, from: "private func parsedSmartDate", to: "private func fallbackSmartDateOption")

        XCTAssertTrue(parser.contains("formatter.locale = .autoupdatingCurrent"))
        XCTAssertFalse(parser.contains("Locale(identifier: \"fr_FR\")"), "Smart draft date parsing must respect the user's locale instead of forcing French.")
    }

    func testCreatedEventDetailSeedsPreparedChecklistFromCreationContext() throws {
        let source = try readProjectFile("iosApp/src/Views/App/ContentView.swift")
        let creationCover = slice(source, from: ".fullScreenCover(isPresented: $showEventCreationSheet)", to: "private var tabBarVisibility")
        let detail = slice(source, from: "struct EventDetailView: View", to: "private var topControls")

        XCTAssertTrue(source.contains("@State private var preparedCreationChecklists: [String: [ChecklistItem]]"))
        XCTAssertTrue(creationCover.contains("persistCreationContext(context, for: event)"))
        XCTAssertTrue(source.contains("preparedCreationChecklists[event.id] = context.preparedChecklist"))
        XCTAssertTrue(source.contains("preparedCreationChecklist: preparedCreationChecklists[event.id] ?? []"))
        XCTAssertTrue(detail.contains("let preparedCreationChecklist: [ChecklistItem]"))
        XCTAssertTrue(detail.contains("seedPreparedCreationChecklistIfNeeded()"))
        XCTAssertTrue(source.contains("eventAIChecklist = preparedCreationChecklist"))
    }

    func testEventDetailExposesReviewableAISuggestions() throws {
        let source = try readProjectFile("iosApp/src/Views/App/ContentView.swift")
        let content = slice(source, from: "struct EventDetailView: View", to: "private struct EventDetailHeroMetric")

        XCTAssertTrue(content.contains("eventAISuggestionPanel"))
        XCTAssertTrue(content.contains("PollSuggestionGenerator"))
        XCTAssertTrue(content.contains("ChecklistGenerator"))
        XCTAssertTrue(content.contains("InvitationMessageGenerator"))
        XCTAssertTrue(content.contains("EventSummaryGenerator"))
        XCTAssertTrue(source.contains("event.detail.ai.review_subtitle"))
        XCTAssertTrue(source.contains("event.detail.ai.polls_title"))
        XCTAssertTrue(source.contains("event.detail.ai.summary_title"))
        XCTAssertTrue(source.contains("event.detail.ai.decided_title"))
        XCTAssertTrue(source.contains("event.detail.ai.ignore_action"))
        XCTAssertTrue(source.contains("event.detail.ai.error_unavailable"))
        XCTAssertTrue(source.contains("event.detail.ai.priority.high"))
        XCTAssertTrue(source.contains("event.detail.ai.priority.medium"))
        XCTAssertTrue(source.contains("event.detail.ai.priority.low"))
        XCTAssertTrue(source.contains("common.edit"))
        XCTAssertTrue(source.contains("common.apply"))
        XCTAssertFalse(source.contains("Préparez les prochaines actions"))
        XCTAssertFalse(source.contains("Sondages proposés"))
        XCTAssertFalse(source.contains("La suggestion n'est pas disponible pour le moment."))
        XCTAssertFalse(source.contains("Text(\"\\(item.category.rawValue) · \\(item.priority.rawValue)\")"))
        XCTAssertFalse(source.contains("EventAIReviewBox(title: \"Résumé\""))
        XCTAssertFalse(source.contains("Label(\"Appliqué\""))
        XCTAssertFalse(source.contains("Label(\"Ignoré\""))
        XCTAssertFalse(content.contains("LanguageModelSession("), "Event detail views must not own Foundation Models sessions.")
    }

    func testEventDetailAIUsesCurrentUserLocale() throws {
        let source = try readProjectFile("iosApp/src/Views/App/ContentView.swift")
        let generation = slice(source, from: "private func generateEventAISuggestions()", to: "private func updateInvitationDraft")

        XCTAssertTrue(generation.contains("let localeIdentifier = Locale.autoupdatingCurrent.identifier"))
        XCTAssertTrue(generation.contains("localeIdentifier: localeIdentifier"))
        XCTAssertFalse(generation.contains("localeIdentifier: \"fr_FR\""), "Event detail AI should not force French output.")
    }

    func testTransportPlanningExposesReviewableTransportHelper() throws {
        let source = try readProjectFile("iosApp/src/Views/Events/TransportPlanningView.swift")
        let content = slice(source, from: "struct TransportPlanningView: View", to: "private struct TransportPlanningWakeveAIContextProvider")

        XCTAssertTrue(content.contains("transportHelperCard"))
        XCTAssertTrue(content.contains("TransportSuggestionGenerator"))
        XCTAssertTrue(content.contains("String(localized: \"transport.ai.message_to_send\")"))
        XCTAssertTrue(content.contains("String(localized: \"common.edit\")"))
        XCTAssertTrue(content.contains("String(localized: \"common.apply\")"))
        XCTAssertTrue(content.contains("String(localized: \"transport.ai.ignore_action\")"))
        XCTAssertFalse(content.contains("LanguageModelSession("), "Transport planning views must not own Foundation Models sessions.")
    }

    func testTransportAIUsesCurrentUserLocale() throws {
        let source = try readProjectFile("iosApp/src/Views/Events/TransportPlanningView.swift")
        let content = slice(source, from: "private func generateTransportSuggestion()", to: "private var primaryText")

        XCTAssertTrue(content.contains("localeIdentifier: Locale.autoupdatingCurrent.identifier"))
        XCTAssertFalse(content.contains("localeIdentifier: \"fr_FR\""), "Transport AI should not force French output.")
    }

    func testAIBadgeCopyUsesWakeveTone() throws {
        let badgeSource = try readProjectFile("iosApp/src/Models/AISuggestionModels.swift")
        let viewSource = try readProjectFile("iosApp/src/Components/AIBadgeView.swift")

        XCTAssertTrue(badgeSource.contains("ai.badge.suggestion"))
        XCTAssertTrue(badgeSource.contains("ai.badge.suggestion.tooltip"))
        XCTAssertTrue(badgeSource.contains("ai.badge.medium_confidence"))
        XCTAssertTrue(viewSource.contains("ai.badge_sheet.review_title"))
        XCTAssertTrue(viewSource.contains("ai.badge_sheet.validation_hint"))
        XCTAssertTrue(viewSource.contains("common.close"))
        XCTAssertFalse(badgeSource.contains("Proposition locale à relire"))
        XCTAssertFalse(badgeSource.contains("À vérifier"))
        XCTAssertFalse(viewSource.contains("À relire"))
        XCTAssertFalse(viewSource.contains("Aucune action n'est appliquée sans validation."))
        XCTAssertFalse(badgeSource.contains("AI Suggestion"))
        XCTAssertFalse(badgeSource.contains("AI generated suggestion"))
        XCTAssertFalse(badgeSource.contains("🤖"))
        XCTAssertFalse(viewSource.contains("Confidence Details"))
        XCTAssertFalse(viewSource.contains("Close"))
    }

    private func slice(_ source: String, from start: String, to end: String) -> String {
        guard let startRange = source.range(of: start) else { return "" }
        let suffix = source[startRange.lowerBound...]
        guard let endRange = suffix.range(of: end) else { return String(suffix) }
        return String(suffix[..<endRange.lowerBound])
    }

    private func readProjectFile(_ relativePath: String) throws -> String {
        try String(contentsOf: projectRoot().appendingPathComponent(relativePath), encoding: .utf8)
    }

    private func projectRoot() throws -> URL {
        let fileURL = URL(fileURLWithPath: #filePath)
        let testsDir = fileURL.deletingLastPathComponent()
        let wakeveAITestsDir = testsDir.deletingLastPathComponent()
        let iosAppDir = wakeveAITestsDir.deletingLastPathComponent()
        return iosAppDir.deletingLastPathComponent()
    }
}
