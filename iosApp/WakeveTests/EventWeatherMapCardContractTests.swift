import XCTest
@testable import Wakeve

final class EventWeatherMapCardContractTests: XCTestCase {
    func testWeatherCardPreviewsCoverRequiredStates() throws {
        let source = try readProjectFile("iosApp/src/Components/EventWeatherMapCard.swift")

        for previewName in [
            "Weather Loading",
            "Weather Available",
            "Weather Available Dark",
            "Weather Available Dynamic Type",
            "Weather Reduce Transparency",
            "Weather Stale",
            "Weather Pending",
            "Weather Unavailable"
        ] {
            XCTAssertTrue(source.contains("#Preview(\"\(previewName)\")"), "Missing \(previewName) preview")
        }

        XCTAssertTrue(source.contains("EventWeatherMapCardPreviewFixtures.availableSummary"))
        XCTAssertTrue(source.contains("EventWeatherMapCardPreviewFixtures.staleSummary"))
        XCTAssertTrue(source.contains("EventWeatherMapCardPreviewSurface(state: .loading)"))
        XCTAssertTrue(source.contains("EventWeatherMapCardPreviewSurface(state: .pending(refreshDate:"))
        XCTAssertTrue(source.contains("EventWeatherMapCardPreviewSurface(state: .unavailable(message:"))
    }

    func testWeatherCardStaleStateIsExplicitlyVisible() throws {
        let source = try readProjectFile("iosApp/src/Components/EventWeatherMapCard.swift")

        XCTAssertTrue(source.contains("let isStale: Bool"))
        XCTAssertTrue(source.contains("isStale: Bool = false"))
        XCTAssertTrue(source.contains("if summary.isStale"))
        XCTAssertTrue(source.contains("weather.stale_badge"))
        XCTAssertTrue(source.contains("SemanticColor.warning(for: colorScheme)"))
    }

    func testWeatherCardPreviewLocalizationKeysExistInCoreLocales() throws {
        for locale in ["en", "fr", "es", "it", "pt"] {
            let strings = try readProjectFile("iosApp/src/Resources/\(locale).lproj/Localizable.strings")
            for key in [
                "weather.loading",
                "weather.stale_badge",
                "weather.pending_title",
                "weather.pending_body_format",
                "weather.unavailable_title",
                "weather.unavailable_message"
            ] {
                XCTAssertTrue(strings.contains("\"\(key)\""), "\(locale) missing \(key)")
            }
        }
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

    func testWeatherDesignValidationCoversA11yAndAppearanceModes() throws {
        let source = try readProjectFile("iosApp/src/Components/EventWeatherMapCard.swift")
        let scenarioView = try readProjectFile("iosApp/src/Views/Events/ScenarioOrganizationView.swift")
        let glassModifier = try readProjectFile("iosApp/src/Theme/LiquidGlassModifier.swift")
        let validation = try readProjectFile("docs/design/event-weather-ios-validation.md")

        XCTAssertTrue(source.contains(".preferredColorScheme(.dark)"))
        XCTAssertTrue(source.contains(".dynamicTypeSize(.accessibility3)"))
        XCTAssertTrue(source.contains("#Preview(\"Weather Reduce Transparency\")"))
        XCTAssertTrue(glassModifier.contains("accessibilityReduceTransparency"))
        XCTAssertTrue(glassModifier.contains("regularMaterial"))
        XCTAssertTrue(source.contains("weather.map_accessibility_format"))
        XCTAssertTrue(source.contains("WakeveTheme.Typography"))
        XCTAssertTrue(source.contains("WakeveTheme.ColorToken"))
        XCTAssertTrue(scenarioView.contains(".accessibilityElement(children: .combine)"))
        XCTAssertTrue(validation.contains("Liquid Glass / material hierarchy"))
        XCTAssertTrue(validation.contains("Dynamic Type"))
        XCTAssertTrue(validation.contains("Reduce Transparency"))
        XCTAssertTrue(validation.contains("Dark/light mode"))
    }

    func testWeatherPrivacyReviewGuardsProviderDataAndAccessControl() throws {
        let source = try readProjectFile("iosApp/src/Components/EventWeatherMapCard.swift")
        let provider = try readProjectFile("iosApp/src/Services/EventWeatherProvider.swift")
        let detail = try readProjectFile("iosApp/src/Views/App/ContentView.swift")
        let scenarioView = try readProjectFile("iosApp/src/Views/Events/ScenarioOrganizationView.swift")
        let review = try readProjectFile("docs/reviews/event-weather-privacy-review.md")

        XCTAssertTrue(provider.contains("CLLocation(latitude: place.coordinate.latitude, longitude: place.coordinate.longitude)"))
        XCTAssertTrue(provider.contains("weatherService.weather(for: location)"))
        XCTAssertFalse(provider.contains("participantId"))
        XCTAssertFalse(provider.contains("participants"))
        XCTAssertFalse(provider.contains("votes"))
        XCTAssertTrue(source.contains("func hide()"))
        XCTAssertTrue(detail.contains("if canShowWeatherContext"))
        XCTAssertTrue(detail.contains("guard canShowWeatherContext else"))
        XCTAssertTrue(detail.contains("canAccessOrganizationDetails"))
        XCTAssertTrue(scenarioView.contains("if isLocked"))
        XCTAssertTrue(scenarioView.contains("ScenarioWeatherComparisonContext(scenario: item.scenario)"))
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
