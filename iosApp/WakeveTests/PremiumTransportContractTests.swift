import XCTest
@testable import Wakeve

final class PremiumTransportContractTests: XCTestCase {
    func testTransportUsesPremiumRouteHierarchy() throws {
        let source = try readProjectFile("iosApp/src/Views/Events/TransportPlanningView.swift")
        let content = slice(source, from: "struct TransportPlanningView", to: "private struct TransportInfoPill")

        XCTAssertTrue(content.contains("EventHeroCard("))
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
