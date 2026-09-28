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
}
