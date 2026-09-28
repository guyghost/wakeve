import XCTest
@testable import Wakeve

final class WKComponentsContractTests: XCTestCase {

    static let requiredKeys = [
        "wk.avatars.others_format",
        "wk.nav.events",
        "wk.nav.create",
        "wk.nav.activity",
        "wk.nav.activity.badge_format"
    ]

    func testWKKeysAreLocalizedInSupportedLocales() throws {
        for locale in ["en", "fr", "es", "it", "pt"] {
            let strings = try readProjectFile("iosApp/src/Resources/\(locale).lproj/Localizable.strings")
            for key in Self.requiredKeys {
                XCTAssertTrue(strings.contains("\"\(key)\""), "Clé \(key) manquante pour \(locale).")
            }
        }
    }

    func testCardUsesContinuousTokenRadii() throws {
        let source = try readProjectFile("iosApp/src/Components/WK/WKCard.swift")
        XCTAssertTrue(source.contains("WK.shape("), "WKCard doit utiliser WK.shape (coins continus).")
        XCTAssertTrue(source.contains("case standard, inset, selected"))
    }

    func testStatusPillExposesTextToVoiceOver() throws {
        let source = try readProjectFile("iosApp/src/Components/WK/WKStatusPill.swift")
        XCTAssertTrue(source.contains(".accessibilityLabel(text)"), "Le statut ne doit jamais être porté par la couleur seule.")
    }

    func testButtonsGuaranteeMinimumTapTarget() throws {
        let source = try readProjectFile("iosApp/src/Components/WK/WKButtons.swift")
        XCTAssertGreaterThanOrEqual(
            source.components(separatedBy: "WK.Size.minTapTarget").count - 1, 3,
            "WKPrimaryButton, WKChip et WKCircleButton doivent chacun garantir 44 pt."
        )
    }

    func testCircleButtonRequiresAccessibilityLabelAndHonorsReduceTransparency() throws {
        let source = try readProjectFile("iosApp/src/Components/WK/WKButtons.swift")
        let circle = source.components(separatedBy: "struct WKCircleButton").last ?? ""
        XCTAssertTrue(circle.contains("let accessibilityLabel: String"))
        XCTAssertTrue(circle.contains("accessibilityReduceTransparency"))
        XCTAssertTrue(circle.contains("#available(iOS 26.0, *)"))
    }

    func testHeroMetricCombinesValueAndCaptionForVoiceOver() throws {
        let source = try readProjectFile("iosApp/src/Components/WK/WKHeroMetric.swift")
        XCTAssertTrue(source.contains("static func accessibilitySummary"))
        XCTAssertTrue(source.contains("WK.Typo.display"))
    }

    func testHeroMetricSummaryReadsNaturally() {
        XCTAssertEqual(
            WKHeroMetric.accessibilitySummary(caption: "Week-end Annecy", value: "5", unit: "/8", subtitle: "votes reçus"),
            "Week-end Annecy, 5/8, votes reçus"
        )
        XCTAssertEqual(
            WKHeroMetric.accessibilitySummary(caption: "Rendez-vous", value: "19:30", unit: nil, subtitle: nil),
            "Rendez-vous, 19:30"
        )
    }

    func readProjectFile(_ relativePath: String) throws -> String {
        let projectRoot = URL(fileURLWithPath: #filePath)
            .deletingLastPathComponent()   // WakeveTests
            .deletingLastPathComponent()   // iosApp
            .deletingLastPathComponent()   // racine
        return try String(contentsOf: projectRoot.appendingPathComponent(relativePath), encoding: .utf8)
    }
}
