import XCTest
@testable import Wakeve

final class EventWeatherMapCardContractTests: XCTestCase {
    func testWeatherCardPreviewsCoverRequiredStates() throws {
        let source = try readProjectFile("iosApp/src/Components/EventWeatherMapCard.swift")

        for previewName in [
            "Weather Loading",
            "Weather Available",
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
