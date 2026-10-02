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

    func testTextMutedMeetsWCAGAAOnCardsAndCanvas() {
        for surface in [("card", WK.Colors.card), ("cardInset", WK.Colors.cardInset), ("canvas", WK.Colors.canvas)] {
            for style in [UIUserInterfaceStyle.light, .dark] {
                let ratio = contrast(WK.Colors.textMuted, surface.1, style)
                XCTAssertGreaterThanOrEqual(ratio, 4.5, "textMuted vs \(surface.0) en \(style == .dark ? "sombre" : "clair") : \(ratio)")
            }
        }
    }

    /// `status.color` n'atteint que 3:1 : il ne sert qu'aux indices non textuels (pastille,
    /// icône) et ne doit JAMAIS être utilisé comme couleur de texte.
    func testStatusColorAsNonTextCueOnCardIsAtLeastThreeToOne() {
        for status in WK.Status.allCases {
            for style in [UIUserInterfaceStyle.light, .dark] {
                XCTAssertGreaterThanOrEqual(contrast(status.color, WK.Colors.card, style), 3, "\(status)")
            }
        }
    }

    func testAvatarTintsMatchSpecAndAreOpaque() {
        let expected: [(Int, Int, Int)] = [
            (0xDC, 0xD6, 0xF7), (0xBD, 0xE8, 0xCF), (0xF7, 0xC9, 0xC4),
            (0xFB, 0xE3, 0xB8), (0xCF, 0xE3, 0xF7), (0xF3, 0xD1, 0xE6)
        ]
        XCTAssertEqual(WK.Colors.avatarTints.count, expected.count)
        for (tint, value) in zip(WK.Colors.avatarTints, expected) {
            for style in [UIUserInterfaceStyle.light, .dark] {
                XCTAssertTrue(rgb(tint, style) == value)
            }
        }
    }

    func testOnAvatarMeetsWCAGAAOnEveryAvatarTint() {
        for style in [UIUserInterfaceStyle.light, .dark] {
            XCTAssertTrue(rgb(WK.Colors.onAvatar, style) == (0x1C, 0x1C, 0x1E))
            for (index, tint) in WK.Colors.avatarTints.enumerated() {
                let ratio = contrast(WK.Colors.onAvatar, tint, style)
                XCTAssertGreaterThanOrEqual(ratio, 4.5, "onAvatar vs avatarTints[\(index)] en \(style == .dark ? "sombre" : "clair") : \(ratio)")
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

    // MARK: - Mode immersif (couche 8)

    /// Couleur translucide posée sur un fond opaque (rendu réel d'une carte ou d'un contour).
    private func composite(_ top: Color, over bottom: Color) -> Color {
        let t = UIColor(top).resolvedColor(with: UITraitCollection(userInterfaceStyle: .dark))
        let b = UIColor(bottom).resolvedColor(with: UITraitCollection(userInterfaceStyle: .dark))
        var tr: CGFloat = 0, tg: CGFloat = 0, tb: CGFloat = 0, ta: CGFloat = 0
        var br: CGFloat = 0, bg: CGFloat = 0, bb: CGFloat = 0, ba: CGFloat = 0
        t.getRed(&tr, green: &tg, blue: &tb, alpha: &ta)
        b.getRed(&br, green: &bg, blue: &bb, alpha: &ba)
        func mix(_ x: CGFloat, _ y: CGFloat) -> CGFloat { x * ta + y * (1 - ta) }
        return Color(uiColor: UIColor(red: mix(tr, br), green: mix(tg, bg), blue: mix(tb, bb), alpha: 1))
    }

    /// Le fond n'est plus presque noir : la teinte de l'événement se voit (luminance relative ≥ 0,02).
    func testImmersiveMoodBackgroundIsVisiblyTinted() {
        for mood in EventMoodPalette.Mood.allCases {
            let m = WK.Mood(palette: .palette(for: mood))
            XCTAssertGreaterThanOrEqual(luminance(m.background, .dark), 0.02, "\(mood) : fond trop proche du noir")
        }
    }

    /// Deux ambiances différentes ont des fonds distincts (au moins un canal écarté de 12/255).
    func testImmersiveMoodsHaveDistinctBackgrounds() {
        let moods = EventMoodPalette.Mood.allCases
        for (index, a) in moods.enumerated() {
            for b in moods[(index + 1)...] {
                let ca = rgb(WK.Mood(palette: .palette(for: a)).background, .dark)
                let cb = rgb(WK.Mood(palette: .palette(for: b)).background, .dark)
                let gap = max(abs(ca.0 - cb.0), abs(ca.1 - cb.1), abs(ca.2 - cb.2))
                XCTAssertGreaterThanOrEqual(gap, 12, "\(a) et \(b) : fonds trop proches (\(gap))")
            }
        }
    }

    /// Texte sur une carte immersive (`surface` translucide sur le fond) : mêmes seuils que sur le fond.
    func testImmersiveMoodTextIsReadableOnItsSurface() {
        for mood in EventMoodPalette.Mood.allCases {
            let m = WK.Mood(palette: .palette(for: mood))
            let surface = composite(m.surface, over: m.background)
            XCTAssertGreaterThanOrEqual(contrast(m.textPrimary, surface, .dark), 7, "\(mood)")
            XCTAssertGreaterThanOrEqual(contrast(m.textSecondary, surface, .dark), 4.5, "\(mood)")
        }
    }

    /// Contour des pastilles : indice non textuel ≥ 3:1 sur le fond (WCAG 1.4.11).
    func testImmersivePillStrokeIsVisibleOnItsBackground() {
        for mood in EventMoodPalette.Mood.allCases {
            let m = WK.Mood(palette: .palette(for: mood))
            XCTAssertGreaterThanOrEqual(contrast(composite(m.pillStroke, over: m.background), m.background, .dark), 3, "\(mood)")
        }
    }
}
