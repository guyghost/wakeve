import XCTest
@testable import Wakeve

final class PremiumEventDetailContractTests: XCTestCase {
    func testEventDetailUsesPremiumHierarchy() throws {
        let source = try readProjectFile("iosApp/src/Views/App/ContentView.swift")
        let content = slice(source, from: "struct EventDetailView", to: "private struct EventDetailHeroMetric")

        XCTAssertTrue(content.contains("EventHeroCard("))
        XCTAssertTrue(content.contains("metadataOverview"))
        XCTAssertTrue(content.contains("EventWeatherMapCard(state: eventWeatherViewModel.state)"))
        XCTAssertTrue(content.contains("urgentNextAction"))
        XCTAssertTrue(content.contains("participantsPreview"))
        XCTAssertTrue(content.contains("messagePreview"))
        XCTAssertTrue(content.contains("LiquidGlassToolbar(title: \"Événement\""))
        XCTAssertTrue(content.contains("LiquidGlassButton("))
    }

    func testEventDetailUsesProgressiveSectionsAndCompactMessages() throws {
        let source = try readProjectFile("iosApp/src/Views/App/ContentView.swift")
        let content = slice(source, from: "struct EventDetailView", to: "private struct EventDetailHeroMetric")

        XCTAssertTrue(content.contains("EventDetailSectionCard(title: \"Organisation\")"))
        XCTAssertTrue(content.contains("EventDetailActionRow("))
        XCTAssertTrue(content.contains("EventDetailParticipantsPreview("))
        XCTAssertTrue(content.contains("EventDetailMessagePreview("))
        XCTAssertFalse(
            content.contains("EventPreviewDetailRow("),
            "Event Detail should use the premium progressive rows in its main hierarchy."
        )
    }

    func testEventDetailPreservesBusinessLogicBoundary() throws {
        let source = try readProjectFile("iosApp/src/Views/App/ContentView.swift")
        let content = slice(source, from: "struct EventDetailView", to: "private struct EventDetailHeroMetric")

        XCTAssertTrue(content.contains("canAccessScenarioPlanning"))
        XCTAssertTrue(content.contains("canAccessTransportPlanning"))
        XCTAssertTrue(content.contains("canShowOrganizationDashboard"))
        XCTAssertTrue(content.contains("ParticipantAccessMapper.shared.fromRepositoryRecord"))
        XCTAssertFalse(
            content.contains("StateMachine("),
            "Event Detail should keep presentation local and not instantiate shared state machines."
        )
    }

    func testEventDetailWeatherCardUsesWeatherKitAndMapKit() throws {
        let source = try readProjectFile("iosApp/src/Components/EventWeatherMapCard.swift")

        XCTAssertTrue(source.contains("import WeatherKit"))
        XCTAssertTrue(source.contains("import MapKit"))
        XCTAssertTrue(source.contains("WeatherService.shared"))
        XCTAssertTrue(source.contains("MKLocalSearch"))
        XCTAssertTrue(source.contains("potentialLocationQueries"))
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
