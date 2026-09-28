import XCTest
import SwiftUI
import UIKit
@testable import Wakeve

@MainActor
func fittingSize<V: View>(_ view: V, width: CGFloat = 1000, dynamicType: DynamicTypeSize = .large) -> CGSize {
    let host = UIHostingController(rootView: view.environment(\.dynamicTypeSize, dynamicType))
    return host.sizeThatFits(in: CGSize(width: width, height: .greatestFiniteMagnitude))
}

/// Tests comportementaux : on mesure le rendu réel plutôt que de lire le source.
@MainActor
final class WKRenderingTests: XCTestCase {

    /// WKCard doit empiler ses enfants. Sans VStack interne, les modificateurs s'appliquent à chaque
    /// enfant du TupleView : dans un HStack on obtient deux cartes côte à côte. Comme les cartes ont
    /// `maxWidth: .infinity`, la largeur mesurée vaut toujours la largeur proposée et n'est pas
    /// discriminante ; la hauteur l'est : empilée, l'action ajoute au moins une cible tactile (44 pt)
    /// à la hauteur, alors que côte à côte la hauteur reste celle de la plus haute des deux cartes.
    func testCardStacksMultipleChildrenVertically() {
        let withAction = fittingSize(HStack(spacing: 0) {
            WKHeroMetric(caption: "A", value: "5", unit: "/8", subtitle: "votes", actionTitle: "Voter") {}
        })
        let withoutAction = fittingSize(HStack(spacing: 0) {
            WKHeroMetric(caption: "A", value: "5", unit: "/8", subtitle: "votes")
        })
        XCTAssertGreaterThanOrEqual(withAction.height, withoutAction.height + WK.Size.minTapTarget,
                                    "avec action \(withAction), sans action \(withoutAction)")
    }

    private let extremes: [DynamicTypeSize] = [.large, .accessibility5]

    private func assertMinTapTarget(_ size: CGSize, _ what: String, file: StaticString = #filePath, line: UInt = #line) {
        XCTAssertGreaterThanOrEqual(size.width, WK.Size.minTapTarget, "\(what) largeur \(size)", file: file, line: line)
        XCTAssertGreaterThanOrEqual(size.height, WK.Size.minTapTarget, "\(what) hauteur \(size)", file: file, line: line)
    }

    // MARK: - I3 barre flottante

    func testFloatingNavBarFitsA375ptScreenAtLargestAccessibilitySize() {
        let size = fittingSize(WKFloatingNavBar(selection: .constant(.events), activityBadge: 3) {},
                               width: 375, dynamicType: .accessibility5)
        XCTAssertLessThanOrEqual(size.width, 375, "\(size)")
        XCTAssertLessThanOrEqual(size.height, 120, "\(size)")
    }

    func testFloatingNavBarKeepsMinimumTapTargetAtDefaultSize() {
        let size = fittingSize(WKFloatingNavBar(selection: .constant(.events), activityBadge: 3) {}, width: 375)
        XCTAssertGreaterThanOrEqual(size.height, WK.Size.minTapTarget, "\(size)")
    }

    func testFloatingNavBarHidesSelectedTitleOnlyAtAccessibilitySizes() {
        XCTAssertTrue(WKFloatingNavBar.showsSelectedTitle(at: .large))
        XCTAssertTrue(WKFloatingNavBar.showsSelectedTitle(at: .xxxLarge))
        XCTAssertFalse(WKFloatingNavBar.showsSelectedTitle(at: .accessibility1))
        XCTAssertFalse(WKFloatingNavBar.showsSelectedTitle(at: .accessibility5))
        // Mesure : à AX1 (titre masqué) la barre est plus étroite qu'en xxxLarge (titre visible).
        let bar = WKFloatingNavBar(selection: .constant(.events)) {}
        let withTitle = fittingSize(bar, width: 375, dynamicType: .xxxLarge)
        let withoutTitle = fittingSize(bar, width: 375, dynamicType: .accessibility1)
        XCTAssertLessThan(withoutTitle.width, withTitle.width, "xxxLarge \(withTitle) / AX1 \(withoutTitle)")
    }

    func testFloatingNavBarExposesStableAccessibilityIdentifiers() {
        XCTAssertEqual(WKFloatingNavBar.accessibilityID(for: .events), "wk.nav.events")
        XCTAssertEqual(WKFloatingNavBar.accessibilityID(for: .activity), "wk.nav.activity")
        XCTAssertEqual(WKFloatingNavBar.createAccessibilityID, "wk.nav.create")
    }

    // MARK: - I4 cibles tactiles

    func testButtonsKeepMinimumTapTargetFromDefaultToAX5() {
        for size in extremes {
            assertMinTapTarget(fittingSize(WKCircleButton(systemImage: "gearshape", accessibilityLabel: "Réglages") {}, dynamicType: size), "WKCircleButton \(size)")
            assertMinTapTarget(fittingSize(WKChip(title: "Samedi") {}, dynamicType: size), "WKChip \(size)")
            assertMinTapTarget(fittingSize(WKChip(title: "Voter", style: .prominent) {}, dynamicType: size), "WKChip prominent \(size)")
            assertMinTapTarget(fittingSize(WKPrimaryButton(title: "Continuer") {}, width: 375, dynamicType: size), "WKPrimaryButton \(size)")
        }
    }

    func testActionBarSecondaryIconsKeepMinimumTapTargetFromDefaultToAX5() {
        let one = [WKActionBar.Item(systemImage: "square.and.arrow.up", label: "Partager") {}]
        let two = one + [WKActionBar.Item(systemImage: "trash", label: "Supprimer") {}]
        for size in extremes {
            let a = fittingSize(WKActionBar(primaryTitle: "OK", primaryAction: {}, secondary: one).fixedSize(), dynamicType: size)
            let b = fittingSize(WKActionBar(primaryTitle: "OK", primaryAction: {}, secondary: two).fixedSize(), dynamicType: size)
            XCTAssertGreaterThanOrEqual(a.height, WK.Size.minTapTarget, "\(size) \(a)")
            // Chaque icône secondaire ajoutée élargit la barre d'au moins une cible tactile
            // (tolérance d'un demi-point pour l'arrondi au pixel de la mise en page).
            XCTAssertGreaterThanOrEqual(b.width - a.width + 0.5, WK.Size.minTapTarget, "\(size) 1 icône \(a) / 2 icônes \(b)")
        }
    }

    // MARK: - M4 bouton principal

    func testPrimaryButtonWrapsLongTitleInsteadOfTruncating() {
        let title = "Confirmer la date et prévenir tout le groupe maintenant"
        let narrow = fittingSize(WKPrimaryButton(title: title) {}, width: 200, dynamicType: .accessibility3)
        let wide = fittingSize(WKPrimaryButton(title: title) {}, width: 2000, dynamicType: .accessibility3)
        XCTAssertGreaterThan(narrow.height, wide.height, "étroit \(narrow) / large \(wide)")
    }
}
