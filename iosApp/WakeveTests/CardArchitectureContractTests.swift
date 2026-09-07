import XCTest
@testable import Wakeve

/// Contract tests — Proposition Swarm DAO #25 : cartes standardisées +
/// carte héro « info confirmée » à bordure dorée (modèle Meilleur créneau).
/// Pattern source-contract, cf. ScreenHeaderArchitectureContractTests.
final class CardArchitectureContractTests: XCTestCase {

    func testHeroCardComponentExists() throws {
        let components = try readProjectFile("iosApp/src/Components/DesignSystem/WakeveDesignSystemComponents.swift")

        XCTAssertTrue(
            components.contains("struct WakeveHeroCard"),
            "La carte héro partagée (#25) doit exister dans le design system."
        )
        XCTAssertTrue(
            components.contains("SemanticColor.warning(for: colorScheme).opacity(0.3)"),
            "La carte héro doit porter la bordure dorée standard (modèle Meilleur créneau)."
        )
    }

    func testPollResultsBestSlotConsumesSharedHeroCard() throws {
        let source = try readProjectFile("iosApp/src/Views/Polls/PollResultsView.swift")

        XCTAssertTrue(
            source.contains("WakeveHeroCard"),
            "Le meilleur créneau (référence du pattern) doit consommer la carte héro partagée."
        )
        XCTAssertFalse(
            source.contains(".stroke(SemanticColor.warning(for: colorScheme).opacity(0.3), lineWidth: 2)"),
            "La bordure dorée ne doit plus être codée inline dans les écrans (#25)."
        )
    }

    func testConfirmedDateCardUsesHeroTreatment() throws {
        let source = try readProjectFile("iosApp/src/Views/Polls/PollResultsView.swift")

        // La date confirmée est une information clé confirmée : traitement héro.
        let confirmedSlice = source.slice(from: "struct ConfirmedDateCard", to: "// MARK: - Poll Decision Announcement")
        XCTAssertFalse(confirmedSlice.isEmpty, "ConfirmedDateCard doit rester identifiable dans la source.")
        XCTAssertTrue(
            confirmedSlice.contains("WakeveHeroCard"),
            "La carte date confirmée doit recevoir le traitement héro (#25)."
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
            domain: "CardArchitectureContractTests",
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
