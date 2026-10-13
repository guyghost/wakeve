import XCTest
import SwiftUI
@testable import Wakeve

/// Conteneur de sheet des modules du hub (couche 5a, #47) : rendu mesuré aux grandes tailles.
@MainActor
final class WKModuleSheetTests: XCTestCase {
    private let longTitle = "Hébergement pour tout le week-end à Annecy"

    private var twoSecondaries: [WKActionBar.Item] {
        [
            .init(systemImage: "arrow.up.left.and.arrow.down.right", label: "Plein écran", accessibilityID: "a") {},
            .init(systemImage: "bubble.left", label: "Commentaires", accessibilityID: "b") {}
        ]
    }

    private func sheet(primary: (title: String, action: () -> Void)?, secondary: [WKActionBar.Item]) -> some View {
        WKModuleSheet(
            title: longTitle,
            status: .init(text: "2/5 prêts", status: .pending),
            missing: "3 repas sans responsable",
            primary: primary,
            secondary: secondary,
            onClose: {}
        ) {
            WKCard(style: .inset) {
                Text("Barbecue du samedi soir").font(WK.Typo.body)
            }
        }
    }

    func testHeaderWrapsTheTitleAndFitsAPhoneAtAX5() {
        let header = WKModuleSheetHeader(title: longTitle, status: .init(text: "2/5 prêts", status: .pending), onClose: {})
        let large = fittingSize(header, width: 375, dynamicType: .large)
        let ax5 = fittingSize(header, width: 375, dynamicType: .accessibility5)
        let wide = fittingSize(header, width: 1000, dynamicType: .accessibility5)
        XCTAssertLessThanOrEqual(ax5.width, 375, "\(ax5)")
        // Le titre se replie (aucune troncature) : l'en-tête grandit nettement à AX5…
        XCTAssertGreaterThan(ax5.height, large.height * 2, "large \(large) / AX5 \(ax5)")
        // …et selon la largeur disponible : une ligne tronquée aurait la même hauteur partout.
        XCTAssertGreaterThan(ax5.height, wide.height, "375 \(ax5) / 1000 \(wide)")
    }

    func testCloseButtonKeepsTheMinimumTapTarget() {
        for size in [DynamicTypeSize.large, .accessibility5] {
            // Bouton mesuré seul (taille idéale) : la hauteur de l'en-tête ne dit rien de la cible tactile.
            let button = fittingSize(
                WKCircleButton(systemImage: "xmark", accessibilityLabel: "Fermer", action: {}),
                width: 1000, dynamicType: size
            )
            XCTAssertGreaterThanOrEqual(button.width, WK.Size.minTapTarget, "\(size) \(button)")
            XCTAssertGreaterThanOrEqual(button.height, WK.Size.minTapTarget, "\(size) \(button)")
            XCTAssertLessThan(button.width, 375 / 2, "Taille idéale, pas la largeur proposée : \(button)")
        }
        XCTAssertEqual(WKModuleSheetChrome.closeAccessibilityID, "wk.sheet.close")
        XCTAssertEqual(WKModuleSheetChrome.closeLabelKey, "common.close")
        XCTAssertEqual(WK.localizedFormat(WKModuleSheetChrome.closeLabelKey, locale: Locale(identifier: "fr")), "Fermer")
    }

    func testBottomBarWrapsItsPrimaryTitleInsteadOfOverflowingAtAX5() {
        let bar = WKActionBar(primaryTitle: "Ajouter un repas", primaryAction: {}, secondary: twoSecondaries)
        let available = 375 - 2 * WK.Space.screen
        let ideal = fittingSize(bar, width: 1000, dynamicType: .accessibility5)
        // Précondition qui rend le test discriminant : à AX5 la barre idéale ne tient pas sur un téléphone.
        XCTAssertGreaterThan(ideal.width, available, "idéale \(ideal)")
        let phone = fittingSize(bar, width: available, dynamicType: .accessibility5)
        XCTAssertLessThanOrEqual(phone.width, available, "\(phone)")
        XCTAssertGreaterThan(phone.height, ideal.height, "Le titre se replie : idéale \(ideal) / téléphone \(phone)")
    }

    func testDetentsAreLargeOnlyAtAccessibilitySizes() {
        XCTAssertEqual(WKModuleSheetChrome.detents(for: .large), [.medium, .large])
        XCTAssertEqual(WKModuleSheetChrome.detents(for: .xxxLarge), [.medium, .large])
        for size in [DynamicTypeSize.accessibility1, .accessibility3, .accessibility5] {
            XCTAssertEqual(WKModuleSheetChrome.detents(for: size), [.large], "\(size)")
        }
    }

    func testSecondaryOnlyRowStacksAtAX5() {
        let row = WKModuleSheetSecondaryRow(items: twoSecondaries)
        // Taille idéale (largeur proposée non contraignante) : la rangée ne compte pas sur la compression.
        let ideal = fittingSize(row, width: 1000, dynamicType: .accessibility5)
        XCTAssertLessThanOrEqual(ideal.width, 375, "idéale \(ideal)")
        XCTAssertGreaterThanOrEqual(ideal.height, 2 * WK.Size.minTapTarget, "empilée \(ideal)")
        // Aux tailles standard, une seule ligne.
        let large = fittingSize(row, width: 375, dynamicType: .large)
        XCTAssertLessThan(large.height, 2 * WK.Size.minTapTarget, "\(large)")
        XCTAssertGreaterThanOrEqual(large.height, WK.Size.minTapTarget, "\(large)")
    }

    func testWholeSheetFitsAPhoneAtAX5WithAndWithoutPrimary() {
        for primary: (title: String, action: () -> Void)? in [("Ajouter un repas", {}), nil] {
            for secondary in [twoSecondaries, []] {
                let size = fittingSize(sheet(primary: primary, secondary: secondary), width: 375, dynamicType: .accessibility5)
                XCTAssertLessThanOrEqual(size.width, 375, "\(primary?.title ?? "-") \(secondary.count) \(size)")
                XCTAssertGreaterThan(size.height, 0)
            }
        }
    }

    func testSheetUsesWKChromeAndDetents() throws {
        let source = try String(contentsOf: URL(fileURLWithPath: #filePath)
            .deletingLastPathComponent().deletingLastPathComponent()
            .appendingPathComponent("src/Components/WK/WKModuleSheet.swift"), encoding: .utf8)
        for anchor in [".presentationDetents(WKModuleSheetChrome.detents(for: dynamicTypeSize))",
                       "[.medium, .large]", ".presentationDragIndicator(.visible)", ".presentationBackground(WK.Colors.canvas)",
                       "WKActionBar(", "WKCircleButton(", "WKStatusPill(", "safeAreaInset(edge: .bottom",
                       "WK.Colors.textMuted", "WK.Colors.canvas"] {
            XCTAssertTrue(source.contains(anchor), anchor)
        }
        XCTAssertFalse(source.contains(".background(WK.Colors.canvas.ignoresSafeArea())"),
                       "Le fond de présentation suffit ; pas de second fond identique.")
        let gallery = try String(contentsOf: URL(fileURLWithPath: #filePath)
            .deletingLastPathComponent().deletingLastPathComponent()
            .appendingPathComponent("src/Components/WK/WKGallery.swift"), encoding: .utf8)
        XCTAssertTrue(gallery.contains("WKModuleSheet("), "La galerie montre le conteneur de sheet.")
    }
}
