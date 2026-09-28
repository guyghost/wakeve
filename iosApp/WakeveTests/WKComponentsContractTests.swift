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

    func readProjectFile(_ relativePath: String) throws -> String {
        let projectRoot = URL(fileURLWithPath: #filePath)
            .deletingLastPathComponent()   // WakeveTests
            .deletingLastPathComponent()   // iosApp
            .deletingLastPathComponent()   // racine
        return try String(contentsOf: projectRoot.appendingPathComponent(relativePath), encoding: .utf8)
    }
}
