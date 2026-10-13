import XCTest

/// Réancré depuis `EventWeatherMapCardContractTests` et `PremiumEventDetailContractTests` (couche 9) :
/// la carte météo du détail legacy est supprimée ; la météo reste servie par `EventWeatherProvider`
/// dans la comparaison de scénarios (`ScenarioOrganizationView`).
final class EventWeatherContractTests: XCTestCase {
    func testScenarioWeatherUsesWeatherKitAndMapKit() throws {
        let provider = try readProjectFile("iosApp/src/Services/EventWeatherProvider.swift")
        let scenarioView = try readProjectFile("iosApp/src/Views/Events/ScenarioOrganizationView.swift")

        XCTAssertTrue(provider.contains("import WeatherKit"))
        XCTAssertTrue(provider.contains("final class WeatherKitEventForecastProvider"))
        XCTAssertTrue(provider.contains("init(weatherService: WeatherService = .shared)"))
        XCTAssertTrue(scenarioView.contains("EventWeatherProviding"))
        XCTAssertTrue(scenarioView.contains("MKLocalSearch"))
    }

    func testWeatherKitEntitlementIsWiredToAppTarget() throws {
        let entitlements = try readProjectFile("iosApp/src/Wakeve.entitlements")
        let project = try readProjectFile("iosApp/iosApp.xcodeproj/project.pbxproj")
        let provider = try readProjectFile("iosApp/src/Services/EventWeatherProvider.swift")

        XCTAssertTrue(provider.contains("import WeatherKit"))
        XCTAssertTrue(entitlements.contains("<key>com.apple.developer.weatherkit</key>"))
        XCTAssertTrue(entitlements.contains("<true/>"))
        XCTAssertTrue(project.contains("CODE_SIGN_ENTITLEMENTS = src/Wakeve.entitlements;"))
        XCTAssertTrue(project.contains("PRODUCT_BUNDLE_IDENTIFIER = com.guyghost.wakeve;"))
        XCTAssertTrue(project.contains("DEVELOPMENT_TEAM = \"${TEAM_ID}\";"))
    }

    func testWeatherPrivacyReviewGuardsProviderDataAndAccessControl() throws {
        let provider = try readProjectFile("iosApp/src/Services/EventWeatherProvider.swift")
        let scenarioView = try readProjectFile("iosApp/src/Views/Events/ScenarioOrganizationView.swift")
        let review = try readProjectFile("docs/reviews/event-weather-privacy-review.md")

        XCTAssertTrue(provider.contains("CLLocation(latitude: place.coordinate.latitude, longitude: place.coordinate.longitude)"))
        XCTAssertTrue(provider.contains("weatherService.weather(for: location)"))
        XCTAssertFalse(provider.contains("participantId"))
        XCTAssertFalse(provider.contains("participants"))
        XCTAssertFalse(provider.contains("votes"))
        XCTAssertTrue(scenarioView.contains("if isLocked"))
        XCTAssertTrue(scenarioView.contains("ScenarioWeatherComparisonContext(scenario: item.scenario)"))
        XCTAssertTrue(scenarioView.contains(".accessibilityElement(children: .combine)"))
        XCTAssertTrue(review.contains("No blocking local privacy, access-control, or fallback issues remain"))
        XCTAssertTrue(review.contains("Physical-device WeatherKit validation remains required"))
    }

    private func readProjectFile(_ relativePath: String) throws -> String {
        let fileURL = URL(fileURLWithPath: #filePath)
        let runtimeURL = URL(fileURLWithPath: FileManager.default.currentDirectoryPath)

        for startURL in [fileURL.deletingLastPathComponent(), runtimeURL] {
            var candidateRoot = startURL
            for _ in 0..<8 {
                let targetURL = candidateRoot.appendingPathComponent(relativePath)
                if FileManager.default.fileExists(atPath: targetURL.path) {
                    return try String(contentsOf: targetURL, encoding: .utf8)
                }

                let parentURL = candidateRoot.deletingLastPathComponent()
                guard parentURL.path != candidateRoot.path else { break }
                candidateRoot = parentURL
            }
        }

        throw CocoaError(.fileNoSuchFile)
    }
}
