import XCTest
@testable import Wakeve

/// Contract tests — Proposition Swarm DAO #24 : en-têtes d'écran unifiés
/// (grand titre display + sous-titre contexte événement + bouton circulaire 44pt).
/// Pattern source-contract, cf. InvitationExperienceArchitectureReviewRedTests.
final class ScreenHeaderArchitectureContractTests: XCTestCase {

    func testSharedHeaderComponentExistsWithReferencePattern() throws {
        let components = try readProjectFile("iosApp/src/Components/DesignSystem/WakeveDesignSystemComponents.swift")

        XCTAssertTrue(
            components.contains("struct WakeveScreenHeader"),
            "Le composant d'en-tête partagé (#24) doit exister dans le design system."
        )
        XCTAssertTrue(
            components.contains("WakeveTheme.Typography.display"),
            "L'en-tête standard doit utiliser le grand titre display (modèle écran Résultats)."
        )
        XCTAssertTrue(
            components.contains("WakeveCircleButton("),
            "L'en-tête standard doit réutiliser le bouton circulaire 44pt."
        )
    }

    func testPollResultsConsumesSharedHeaderInsteadOfInlinePattern() throws {
        let source = try readProjectFile("iosApp/src/Views/Polls/PollResultsView.swift")

        XCTAssertTrue(
            source.contains("WakeveScreenHeader("),
            "L'écran Résultats (référence du pattern) doit consommer le composant partagé."
        )
    }

    func testScenarioOrganizationDropsGradientHeroForStandardHeader() throws {
        let source = try readProjectFile("iosApp/src/Views/Events/ScenarioOrganizationView.swift")

        XCTAssertTrue(
            source.contains("WakeveScreenHeader("),
            "L'écran Scénarios doit utiliser l'en-tête standard (fond ivoire, #24)."
        )
        XCTAssertFalse(
            source.contains("heroColors"),
            "Le dégradé bleu du héros Scénarios doit disparaître au profit du traitement standard (#24)."
        )
        XCTAssertFalse(
            source.contains("map.fill"),
            "Le filigrane du héros dégradé doit disparaître avec le héros (#24)."
        )
        XCTAssertTrue(
            source.contains("phaseBadge"),
            "Le badge de statut doit être conservé, restylé en chip (#24)."
        )
    }

    // MARK: - Helpers

    private func readProjectFile(_ relativePath: String) throws -> String {
        let fileURL = URL(fileURLWithPath: #filePath)
        let runtimeURL = URL(fileURLWithPath: FileManager.default.currentDirectoryPath)
        for startURL in [fileURL.deletingLastPathComponent(), runtimeURL] {
            var candidateRoot = startURL
            for _ in 0..<8 {
                let targetURL = candidateRoot.appendingPathComponent(relativePath)
                if FileManager.default.fileExists(atPath: targetURL.path) {
                    return try String(contentsOf: targetURL, encoding: .utf8)
                }
                guard let parent = candidateRoot.parentURL() else { break }
                candidateRoot = parent
            }
        }
        throw NSError(
            domain: "ScreenHeaderArchitectureContractTests",
            code: 1,
            userInfo: [NSLocalizedDescriptionKey: "Projet introuvable depuis le bundle de test : \(relativePath)"]
        )
    }
}

private extension URL {
    func parentURL() -> URL? {
        let parent = deletingLastPathComponent()
        return parent.path.isEmpty || parent == self ? nil : URL(fileURLWithPath: parent.path)
    }
}
