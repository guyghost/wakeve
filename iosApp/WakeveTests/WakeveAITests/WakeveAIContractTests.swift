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

    func testWakeveAIMetadataModelsMirrorSharedContract() throws {
        let source = try readProjectFile("iosApp/src/WakeveAI/WakeveAIModels.swift")

        XCTAssertTrue(source.contains("enum WakeveAIUseCase"))
        XCTAssertTrue(source.contains("struct WakeveAIInteractionMetadata"))
        XCTAssertTrue(source.contains("struct WakeveAIValidationResult"))
        XCTAssertTrue(source.contains("struct WakeveAICostEstimate"))
        XCTAssertTrue(source.contains("enum WakeveAIInteractionMetadataPolicy"))
        XCTAssertTrue(source.contains("sanitizedInputSummary"))
        XCTAssertTrue(source.contains("sanitizedOutputSummary"))
        XCTAssertTrue(source.contains("reasoningSummary"))
        XCTAssertTrue(source.contains("latencyMilliseconds"))
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
