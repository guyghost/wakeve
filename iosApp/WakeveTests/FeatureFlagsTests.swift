import XCTest
@testable import Wakeve

final class FeatureFlagsTests: XCTestCase {
    private let suiteName = "FeatureFlagsTests"

    override func tearDown() {
        UserDefaults().removePersistentDomain(forName: suiteName)
        super.tearDown()
    }

    func testRedesignFlagIsOffByDefault() {
        let defaults = UserDefaults(suiteName: suiteName)!
        XCTAssertFalse(FeatureFlags.isRedesign2026Enabled(in: defaults))
    }

    func testRedesignFlagReadsItsKey() {
        let defaults = UserDefaults(suiteName: suiteName)!
        defaults.set(true, forKey: FeatureFlags.redesign2026Key)
        XCTAssertTrue(FeatureFlags.isRedesign2026Enabled(in: defaults))
    }

    func testRedesignKeyIsStable() {
        // Utilisée par @AppStorage et par l'argument de lancement `-iosRedesign2026 YES`.
        XCTAssertEqual(FeatureFlags.redesign2026Key, "iosRedesign2026")
    }
}
