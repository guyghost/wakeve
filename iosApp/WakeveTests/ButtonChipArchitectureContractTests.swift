import XCTest
@testable import Wakeve

/// Contract tests — Proposition Swarm DAO #26 : boutons et chips unifiés
/// (pilule primaire #2563EB, bouton icône circulaire secondaire, chips méta).
/// Pattern source-contract, cf. CardArchitectureContractTests.
final class ButtonChipArchitectureContractTests: XCTestCase {

    func testPrimaryPillUsesCanonicalBlueToken() throws {
        let components = try readProjectFile("iosApp/src/Components/DesignSystem/WakeveDesignSystemComponents.swift")
        let primarySlice = components.slice(from: "struct WakeveActionButton", to: "// MARK: - Circular Icon Button")

        XCTAssertTrue(
            primarySlice.contains("Color.wakevePrimary"),
            "Le CTA primaire doit utiliser le token bleu canonique #2563EB (#26)."
        )
        XCTAssertFalse(
            primarySlice.contains("permissionBlue"),
            "Le bleu dérivé permissionBlue (#3F8FF2) ne doit plus servir de CTA primaire (#26)."
        )
        XCTAssertTrue(
            primarySlice.contains("frame(height: 56)"),
            "La pilule primaire garde sa hauteur standard de 56pt."
        )
    }

    func testSecondaryIconButtonComponentExists() throws {
        let components = try readProjectFile("iosApp/src/Components/DesignSystem/WakeveDesignSystemComponents.swift")

        XCTAssertTrue(
            components.contains("struct WakeveSecondaryIconButton"),
            "Le bouton icône circulaire secondaire (fond bleu clair) doit exister (#26)."
        )
        XCTAssertTrue(
            components.contains("Color.wakevePrimary.opacity"),
            "Le fond secondaire doit dériver du bleu primaire (#D8E3FB sur blanc, #26)."
        )
    }

    func testMetaChipComponentExistsAndLegacyBadgeDelegates() throws {
        let components = try readProjectFile("iosApp/src/Components/DesignSystem/WakeveDesignSystemComponents.swift")
        XCTAssertTrue(
            components.contains("struct WakeveMetaChip"),
            "La chip de métadonnées standard doit exister dans le design system (#26)."
        )

        let voting = try readProjectFile("iosApp/src/Views/Polls/PollVotingView.swift")
        XCTAssertTrue(
            voting.contains("WakeveMetaChip"),
            "PollTimeZoneBadge doit déléguer à WakeveMetaChip (plus de style ad hoc, #26)."
        )
    }

    func testDecisionAnnouncementUsesSecondaryIconButton() throws {
        let results = try readProjectFile("iosApp/src/Views/Polls/PollResultsView.swift")

        XCTAssertTrue(
            results.contains("WakeveSecondaryIconButton"),
            "L'action copier de l'annonce doit utiliser le bouton secondaire standard (#26)."
        )
        XCTAssertFalse(
            results.contains(".buttonStyle(.bordered)"),
            "Les boutons bordés système ne doivent plus servir d'actions secondaires (#26)."
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
                let parent = URL(fileURLWithPath: candidateRoot.path).deletingLastPathComponent()
                if parent.path == candidateRoot.path || parent.path.isEmpty { break }
                candidateRoot = parent
            }
        }
        throw NSError(
            domain: "ButtonChipArchitectureContractTests",
            code: 1,
            userInfo: [NSLocalizedDescriptionKey: "Projet introuvable depuis le bundle de test : \(relativePath)"]
        )
    }
}

private extension String {
    func slice(from startMarker: String, to endMarker: String) -> String {
        guard let startRange = range(of: startMarker) else { return "" }
        let searchEnd = range(of: endMarker, range: startRange.upperBound..<endIndex) ?? endIndex..<endIndex
        return String(self[startRange.lowerBound..<searchEnd.lowerBound])
    }
}
