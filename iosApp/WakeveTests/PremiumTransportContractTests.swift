import XCTest
@testable import Wakeve

final class PremiumTransportContractTests: XCTestCase {
    func testTransportUsesPremiumRouteHierarchy() throws {
        let source = try readProjectFile("iosApp/src/Views/Events/TransportPlanningView.swift")
        let content = slice(source, from: "struct TransportPlanningView", to: "private struct TransportInfoPill")

        XCTAssertTrue(content.contains("EventHeroCard("))
        XCTAssertTrue(content.contains("moodPalette: .travel"))
        XCTAssertTrue(content.contains("routePreviewCard"))
        XCTAssertTrue(content.contains("TransportRoutePoint("))
        XCTAssertTrue(content.contains("TransportMetricTile("))
        XCTAssertTrue(content.contains("participantsCard"))
        XCTAssertTrue(content.contains("TransportParticipantRow("))
    }

    func testTransportUsesSinglePrimaryBottomAction() throws {
        let source = try readProjectFile("iosApp/src/Views/Events/TransportPlanningView.swift")
        let content = slice(source, from: "struct TransportPlanningView", to: "private struct TransportInfoPill")
        let optimizationCard = slice(source, from: "private var optimizationCard", to: "private var generatedPlanCard")

        XCTAssertTrue(content.contains("bottomPrimaryAction"))
        XCTAssertTrue(content.contains("LiquidGlassButton("))
        XCTAssertTrue(content.contains("primaryActionTitle"))
        XCTAssertTrue(content.contains("primaryActionDisabled"))
        XCTAssertFalse(
            optimizationCard.contains("\"Générer le plan\""),
            "The generate action should live in the single bottom primary action, not as another primary button inside the card."
        )
    }

    func testTransportContentCardsUseSemanticBrandLayer() throws {
        let source = try readProjectFile("iosApp/src/Views/Events/TransportPlanningView.swift")
        let content = slice(source, from: "struct TransportPlanningView", to: "private struct TransportInfoPill")

        XCTAssertTrue(content.contains("SemanticColor.appBackground"))
        XCTAssertTrue(content.contains("WakeveContentCard("))
        XCTAssertTrue(content.contains("SemanticColor.progress"))
        XCTAssertTrue(content.contains("SemanticColor.warning"))
        XCTAssertFalse(content.contains("WakeveGlassCard("), "Transport content cards should use WakeveContentCard.")
        XCTAssertFalse(content.contains("LiquidGlassCard("), "Transport content cards should not use Liquid Glass.")
    }

    func testTransportPreservesMutationGuardsAndDepartureWriter() throws {
        let source = try readProjectFile("iosApp/src/Views/Events/TransportPlanningView.swift")
        let content = slice(source, from: "struct TransportPlanningView", to: "private struct TransportInfoPill")

        XCTAssertTrue(content.contains("let isReadOnly: Bool"))
        XCTAssertTrue(content.contains("let eventStatus: EventStatus"))
        XCTAssertTrue(content.contains("let selectedDestination: TransportLocation?"))
        XCTAssertTrue(content.contains("workflowAllowsMutation(eventStatus)"))
        XCTAssertTrue(content.contains("TransportLocation("))
        XCTAssertTrue(content.contains("onSaveDepartureLocation(location)"))
    }

    func testTransportDestinationMissingPrimaryActionIsRoutable() throws {
        let source = try readProjectFile("iosApp/src/Views/Events/TransportPlanningView.swift")
        let disabled = slice(source, from: "private var primaryActionDisabled", to: "private func primaryAction()")
        let action = slice(source, from: "private func primaryAction()", to: "private func saveDeparture")

        XCTAssertTrue(source.contains("let onChooseDestination: () -> Void"))
        XCTAssertTrue(disabled.contains("if selectedDestination == nil"))
        XCTAssertTrue(action.contains("onChooseDestination()"))
    }

    func testTransportPlanningCopyUsesLocalizationKeys() throws {
        let source = try readProjectFile("iosApp/src/Views/Events/TransportPlanningView.swift")
        let content = slice(source, from: "struct TransportPlanningView", to: "private struct TransportInfoPill")

        XCTAssertTrue(content.contains("transport.destination_missing"))
        XCTAssertTrue(content.contains("transport.ai.title"))
        XCTAssertTrue(content.contains("transport.action.choose_destination"))
        XCTAssertTrue(content.contains("transport.plan.total_cost_format"))
        XCTAssertTrue(content.contains("transport.participants.title"))
        XCTAssertFalse(content.contains("\"Destination non sélectionnée\""))
        XCTAssertFalse(content.contains("\"Départs participants\""))
        XCTAssertFalse(content.contains("\"Sélectionner le plan final\""))
    }

    func testTransportAIUsesCurrentLanguagePreference() throws {
        let source = try readProjectFile("iosApp/src/Views/Events/TransportPlanningView.swift")
        let provider = slice(source, from: "private struct TransportPlanningWakeveAIContextProvider", to: "private struct TransportAIList")

        XCTAssertTrue(provider.contains("Locale.autoupdatingCurrent.language.languageCode?.identifier"))
        XCTAssertFalse(provider.contains("languageCode: \"fr\""), "Transport AI context should follow the user's current language.")
    }

    private func readProjectFile(_ relativePath: String) throws -> String {
        let fileURL = URL(fileURLWithPath: #filePath)
        let testsDir = fileURL.deletingLastPathComponent()
        let iosAppDir = testsDir.deletingLastPathComponent()
        let projectRoot = iosAppDir.deletingLastPathComponent()
        let targetURL = projectRoot.appendingPathComponent(relativePath)
        return try String(contentsOf: targetURL, encoding: .utf8)
    }

    private func slice(_ source: String, from startMarker: String, to endMarker: String) -> String {
        guard let start = source.range(of: startMarker)?.lowerBound else {
            return source
        }

        let tail = source[start...]
        guard let end = tail.range(of: endMarker)?.lowerBound else {
            return String(tail)
        }

        return String(tail[..<end])
    }
}
