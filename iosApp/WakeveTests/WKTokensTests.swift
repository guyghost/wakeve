import XCTest
import SwiftUI
import UIKit
@testable import Wakeve

final class WKTokensTests: XCTestCase {

    private func rgb(_ color: Color, _ style: UIUserInterfaceStyle) -> (Int, Int, Int) {
        let resolved = UIColor(color).resolvedColor(with: UITraitCollection(userInterfaceStyle: style))
        var r: CGFloat = 0, g: CGFloat = 0, b: CGFloat = 0, a: CGFloat = 0
        resolved.getRed(&r, green: &g, blue: &b, alpha: &a)
        XCTAssertEqual(a, 1, accuracy: 0.001, "Le contraste exige des couleurs opaques")
        return (Int(round(r * 255)), Int(round(g * 255)), Int(round(b * 255)))
    }

    private func luminance(_ color: Color, _ style: UIUserInterfaceStyle) -> Double {
        let (r, g, b) = rgb(color, style)
        func channel(_ v: Int) -> Double {
            let c = Double(v) / 255
            return c <= 0.03928 ? c / 12.92 : pow((c + 0.055) / 1.055, 2.4)
        }
        return 0.2126 * channel(r) + 0.7152 * channel(g) + 0.0722 * channel(b)
    }

    private func contrast(_ a: Color, _ b: Color, _ style: UIUserInterfaceStyle) -> Double {
        let la = luminance(a, style), lb = luminance(b, style)
        return (max(la, lb) + 0.05) / (min(la, lb) + 0.05)
    }

    func testSurfaceTokensMatchSpec() {
        XCTAssertTrue(rgb(WK.Colors.canvas, .light) == (0xF2, 0xF2, 0xF4))
        XCTAssertTrue(rgb(WK.Colors.canvas, .dark) == (0, 0, 0))
        XCTAssertTrue(rgb(WK.Colors.card, .light) == (0xFF, 0xFF, 0xFF))
        XCTAssertTrue(rgb(WK.Colors.card, .dark) == (0x1C, 0x1C, 0x1E))
        XCTAssertTrue(rgb(WK.Colors.cardInset, .light) == (0xF6, 0xF6, 0xF8))
    }

    func testAccentIsSingleIndigo() {
        XCTAssertTrue(rgb(WK.Colors.accent, .light) == (0x5B, 0x54, 0xD6))
        XCTAssertTrue(rgb(WK.Colors.accent, .dark) == (0x8B, 0x85, 0xF0))
    }

    func testStatusLightValuesMatchSpec() {
        XCTAssertTrue(rgb(WK.Status.confirmed.color, .light) == (0x2E, 0x9D, 0x5B))
        XCTAssertTrue(rgb(WK.Status.pending.fill, .light) == (0xFF, 0xF1, 0xD6))
        XCTAssertTrue(rgb(WK.Status.actionNeeded.onFill, .light) == (0xA1, 0x2E, 0x2E))
        XCTAssertTrue(rgb(WK.Status.draft.fill, .light) == (0xF2, 0xF2, 0xF4))
    }

    func testEveryStatusPillMeetsWCAGAAInBothModes() {
        for status in WK.Status.allCases {
            for style in [UIUserInterfaceStyle.light, .dark] {
                let ratio = contrast(status.onFill, status.fill, style)
                XCTAssertGreaterThanOrEqual(ratio, 4.5, "\(status) en \(style == .dark ? "sombre" : "clair") : \(ratio)")
            }
        }
    }

    func testPrimaryButtonMeetsWCAGAA() {
        for style in [UIUserInterfaceStyle.light, .dark] {
            XCTAssertGreaterThanOrEqual(contrast(WK.Colors.onPrimaryButton, WK.Colors.primaryButton, style), 4.5)
        }
    }

    func testTextOnAccentMeetsWCAGAA() {
        for style in [UIUserInterfaceStyle.light, .dark] {
            XCTAssertGreaterThanOrEqual(contrast(WK.Colors.onAccent, WK.Colors.accent, style), 4.5)
            XCTAssertGreaterThanOrEqual(contrast(WK.Colors.onAccentFill, WK.Colors.accentFill, style), 4.5)
        }
    }

    func testRadiiAndSpacingScale() {
        XCTAssertEqual(WK.Radius.sm, 12)
        XCTAssertEqual(WK.Radius.md, 18)
        XCTAssertEqual(WK.Radius.lg, 28)
        XCTAssertEqual([WK.Space.xxs, WK.Space.xs, WK.Space.sm, WK.Space.md, WK.Space.lg, WK.Space.xl], [4, 8, 12, 16, 24, 32])
        XCTAssertEqual(WK.Space.screen, 16)
        XCTAssertEqual(WK.Size.minTapTarget, 44)
    }

    func testMotionFallsBackToFadeUnderReduceMotion() {
        XCTAssertEqual(WK.Motion.snappy(reduceMotion: true), WK.Motion.reducedFade)
        XCTAssertEqual(WK.Motion.smooth(reduceMotion: true), WK.Motion.reducedFade)
        XCTAssertEqual(WK.Motion.bouncy(reduceMotion: true), WK.Motion.reducedFade)
        XCTAssertNotEqual(WK.Motion.bouncy(reduceMotion: false), WK.Motion.reducedFade)
    }

    func testTextOnCardInsetMeetsWCAGAA() {
        for style in [UIUserInterfaceStyle.light, .dark] {
            let ratio = contrast(WK.Colors.onCardInset, WK.Colors.cardInset, style)
            XCTAssertGreaterThanOrEqual(ratio, 4.5, "onCardInset vs cardInset en \(style == .dark ? "sombre" : "clair") : \(ratio)")
        }
    }

    func testStatusColorOnCardIsAtLeastThreeToOne() {
        for status in WK.Status.allCases {
            for style in [UIUserInterfaceStyle.light, .dark] {
                XCTAssertGreaterThanOrEqual(contrast(status.color, WK.Colors.card, style), 3, "\(status)")
            }
        }
    }

    func testImmersiveMoodTextIsReadableOnItsBackground() {
        for mood in EventMoodPalette.Mood.allCases {
            let m = WK.Mood(palette: .palette(for: mood))
            XCTAssertGreaterThanOrEqual(contrast(m.textPrimary, m.background, .dark), 7, "\(mood)")
            XCTAssertGreaterThanOrEqual(contrast(m.textSecondary, m.background, .dark), 4.5, "\(mood)")
        }
    }
}
