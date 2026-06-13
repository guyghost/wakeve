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
        XCTAssertTrue(source.contains("Modifier"))
        XCTAssertTrue(source.contains("String(localized: \"common.apply\")"))
        XCTAssertTrue(source.contains("Ignorer"))
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

    func testEventDetailExposesReviewableAISuggestions() throws {
        let source = try readProjectFile("iosApp/src/Views/App/ContentView.swift")
        let content = slice(source, from: "struct EventDetailView: View", to: "private struct EventDetailHeroMetric")

        XCTAssertTrue(content.contains("eventAISuggestionPanel"))
        XCTAssertTrue(content.contains("PollSuggestionGenerator"))
        XCTAssertTrue(content.contains("ChecklistGenerator"))
        XCTAssertTrue(content.contains("InvitationMessageGenerator"))
        XCTAssertTrue(content.contains("EventSummaryGenerator"))
        XCTAssertTrue(content.contains("Modifier"))
        XCTAssertTrue(content.contains("Appliquer"))
        XCTAssertTrue(content.contains("Ignorer"))
        XCTAssertFalse(content.contains("LanguageModelSession("), "Event detail views must not own Foundation Models sessions.")
    }

    func testTransportPlanningExposesReviewableTransportHelper() throws {
        let source = try readProjectFile("iosApp/src/Views/Events/TransportPlanningView.swift")
        let content = slice(source, from: "struct TransportPlanningView: View", to: "private struct TransportPlanningWakeveAIContextProvider")

        XCTAssertTrue(content.contains("transportHelperCard"))
        XCTAssertTrue(content.contains("TransportSuggestionGenerator"))
        XCTAssertTrue(content.contains("Message à envoyer"))
        XCTAssertTrue(content.contains("Modifier"))
        XCTAssertTrue(content.contains("Appliquer"))
        XCTAssertTrue(content.contains("Ignorer"))
        XCTAssertFalse(content.contains("LanguageModelSession("), "Transport planning views must not own Foundation Models sessions.")
    }

    func testAIBadgeCopyUsesWakeveTone() throws {
        let badgeSource = try readProjectFile("iosApp/src/Models/AISuggestionModels.swift")
        let viewSource = try readProjectFile("iosApp/src/Components/AIBadgeView.swift")

        XCTAssertTrue(badgeSource.contains("Suggestion"))
        XCTAssertTrue(badgeSource.contains("Proposition locale à relire"))
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
