import XCTest
import SwiftUI
@testable import Wakeve

/// Contract tests — Proposition Swarm DAO #27 : fond ivoire chaud #F6F1EA
/// standardisé (réf. qa-screenshots/cycle-2026-09-04/28-poll-results.png).
final class DesignTokensContractTests: XCTestCase {

    private struct RGB: Equatable {
        let r: Int, g: Int, b: Int
    }

    private func components(_ color: Color) -> RGB {
        let ui = UIColor(color)
        var r: CGFloat = 0, g: CGFloat = 0, b: CGFloat = 0, a: CGFloat = 0
        ui.getRed(&r, green: &g, blue: &b, alpha: &a)
        return RGB(r: Int(round(r * 255)), g: Int(round(g * 255)), b: Int(round(b * 255)))
    }

    func testWarmIvoryTokenMatchesReferenceScreenshot() {
        let c = components(Color.wakeveWarmIvory)
        XCTAssertEqual(c.r, 246)
        XCTAssertEqual(c.g, 241)
        XCTAssertEqual(c.b, 234)
    }

    func testWarmIvoryDarkMatchesMidnight() {
        let c = components(Color.wakeveWarmIvoryDark)
        XCTAssertEqual(c.r, 7)
        XCTAssertEqual(c.g, 20)
        XCTAssertEqual(c.b, 33)
    }

    func testSemanticAppBackgroundLightUsesWarmIvory() {
        XCTAssertEqual(
            components(SemanticColor.appBackground(for: .light)),
            components(Color.wakeveWarmIvory)
        )
    }

    func testSemanticAppBackgroundDarkUnchanged() {
        XCTAssertEqual(
            components(SemanticColor.appBackground(for: .dark)),
            components(BrandColor.midnightBlue)
        )
    }

    func testColorTokenSoftIvoryAlignedWithWarmIvory() {
        XCTAssertEqual(
            components(WakeveTheme.ColorToken.softIvory),
            components(Color.wakeveWarmIvory)
        )
    }

    func testPageBackgroundLightUsesWarmIvory() {
        XCTAssertEqual(
            components(WakeveTheme.ColorToken.pageBackground(for: .light)),
            components(Color.wakeveWarmIvory)
        )
    }
}
