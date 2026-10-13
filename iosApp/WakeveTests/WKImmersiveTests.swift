import XCTest
import SwiftUI
@testable import Wakeve

/// Composants du mode immersif (couche 8, #47) : mesures réelles, plutôt que lecture du source.
@MainActor
final class WKImmersiveTests: XCTestCase {
    private let mood = WK.Mood(palette: .palette(for: .evening))
    private let extremes: [DynamicTypeSize] = [.large, .accessibility5]

    func testImmersiveButtonsKeepMinimumTapTargetFromDefaultToAX5() {
        for size in extremes {
            for style in [WKImmersiveButton.Style.primary, .secondary] {
                let button = fittingSize(WKImmersiveButton(title: "Itinéraire", style: style, mood: mood) {}, width: 375, dynamicType: size)
                XCTAssertGreaterThanOrEqual(button.height, WK.Size.minTapTarget, "\(style) \(size) \(button)")
                XCTAssertGreaterThanOrEqual(button.height, WK.Size.primaryButtonHeight, "\(style) \(size) \(button)")
            }
        }
    }

    func testImmersiveButtonWrapsLongTitleInsteadOfTruncating() {
        let title = "Voir l'événement et prévenir tout le groupe maintenant"
        let narrow = fittingSize(WKImmersiveButton(title: title, mood: mood) {}, width: 200, dynamicType: .accessibility3)
        let wide = fittingSize(WKImmersiveButton(title: title, mood: mood) {}, width: 2000, dynamicType: .accessibility3)
        XCTAssertGreaterThan(narrow.height, wide.height, "étroit \(narrow) / large \(wide)")
    }

    func testImmersivePillWrapsWithinA375ptScreenAtAX5() {
        let pill = WKImmersivePill(text: "Covoiturage : 2 places libres depuis la gare", systemImage: "car", mood: mood)
        let size = fittingSize(pill, width: 375, dynamicType: .accessibility5)
        XCTAssertLessThanOrEqual(size.width, 375, "\(size)")
        let single = fittingSize(WKImmersivePill(text: "Dîner", mood: mood).fixedSize(), dynamicType: .large)
        XCTAssertGreaterThan(size.height, single.height, "AX5 \(size) / défaut \(single)")
    }

    func testImmersiveCardStacksItsChildren() {
        let one = fittingSize(WKImmersiveCard(mood: mood) { Text("A") }, width: 375)
        let two = fittingSize(WKImmersiveCard(mood: mood) { Text("A"); Text("B") }, width: 375)
        XCTAssertGreaterThan(two.height, one.height, "un \(one) / deux \(two)")
    }

    func testScaffoldFitsA375ptScreenAtAX5() {
        let mood = self.mood
        let scaffold = WKImmersiveScaffold(
            mood: mood,
            primary: .init(title: "Itinéraire", systemImage: "arrow.triangle.turn.up.right.diamond") {},
            secondary: .init(title: "Voir l'événement") {},
            onClose: {}
        ) {
            Text("19:30").font(WK.Typo.display)
            WKImmersivePill(text: "Dîner · 20:00", mood: mood)
        }
        let size = fittingSize(scaffold.frame(height: 812), width: 375, dynamicType: .accessibility5)
        XCTAssertLessThanOrEqual(size.width, 375, "\(size)")
    }

    /// AX5 : les deux actions du bas restent sous un quart d'un écran de 812 pt (titre plafonné, cf. couche 7).
    func testScaffoldActionsStayCompactAtAX5() {
        let limit = WKImmersiveScaffold<EmptyView>.actionsDynamicTypeLimit
        XCTAssertTrue(limit.isAccessibilitySize, "Le texte grandit encore aux tailles d'accessibilité.")
        let actions = VStack(spacing: WK.Space.xs) {
            WKImmersiveButton(title: "Itinéraire", mood: mood) {}
            WKImmersiveButton(title: "Voir l'événement", style: .secondary, mood: mood) {}
        }
        .dynamicTypeSize(...limit)
        let size = fittingSize(actions, width: 375 - 2 * WK.Space.screen, dynamicType: .accessibility5)
        XCTAssertLessThanOrEqual(size.height, 812 / 4, "\(size)")
    }

    /// Reduce Transparency : la croix du mode immersif prend le fond opaque de l'ambiance (`mood.closeFill`),
    /// pas la carte neutre des écrans clairs ; les autres `WKCircleButton` gardent `WK.Colors.card`.
    func testScaffoldCloseButtonUsesTheMoodFillUnderReduceTransparency() throws {
        let root = URL(fileURLWithPath: #filePath).deletingLastPathComponent().deletingLastPathComponent()
        let immersive = try String(contentsOf: root.appendingPathComponent("src/Components/WK/WKImmersive.swift"), encoding: .utf8)
        XCTAssertTrue(immersive.contains("reducedTransparencyFill: mood.closeFill"))
        let buttons = try String(contentsOf: root.appendingPathComponent("src/Components/WK/WKButtons.swift"), encoding: .utf8)
        XCTAssertTrue(buttons.contains("var reducedTransparencyFill: Color? = nil"))
        XCTAssertTrue(buttons.contains("content.background(fill ?? WK.Colors.card, in: Circle())"))
    }

    func testScaffoldExposesStableAccessibilityIdentifiers() {
        XCTAssertEqual(WKImmersiveScaffold<EmptyView>.closeAccessibilityID, "immersive.close")
        XCTAssertEqual(WKImmersiveScaffold<EmptyView>.primaryAccessibilityID, "immersive.primary")
        XCTAssertEqual(WKImmersiveScaffold<EmptyView>.secondaryAccessibilityID, "immersive.secondary")
    }

    /// Galerie DEBUG : une preview par ambiance et les variantes d'accessibilité (spec §10).
    func testGalleryPreviewsTheImmersiveScaffold() throws {
        let gallery = try String(
            contentsOf: URL(fileURLWithPath: #filePath).deletingLastPathComponent().deletingLastPathComponent()
                .appendingPathComponent("src/Components/WK/WKGallery.swift"),
            encoding: .utf8
        )
        XCTAssertTrue(gallery.contains("struct WKImmersiveGallery: View"))
        XCTAssertTrue(gallery.contains("EventMoodPalette.Mood.allCases"), "Une preview par ambiance.")
        for preview in ["#Preview(\"Immersif\")", "#Preview(\"Immersif AX5\")",
                        "#Preview(\"Immersif Reduce Transparency\")", "#Preview(\"Immersif Increase Contrast\")"] {
            XCTAssertTrue(gallery.contains(preview), preview)
        }
    }
}
