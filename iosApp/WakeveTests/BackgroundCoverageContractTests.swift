import XCTest
@testable import Wakeve

/// Contract tests — Proposition Swarm DAO #27 : couverture du fond ivoire.
/// Aucun écran de contenu ne doit rester sur un fond système (blanc/gris).
final class BackgroundCoverageContractTests: XCTestCase {

    func testIvoryBackgroundAppliedToSystemChromeScreens() throws {
        let screens: [(file: String, marker: String)] = [
            ("iosApp/src/Views/Invitations/EventLibraryView.swift", "EventLibraryView"),
            ("iosApp/src/Views/Invitations/EventInformationView.swift", "EventInformationView"),
            ("iosApp/src/Views/Invitations/EventArchiveView.swift", "EventArchiveView")
        ]

        for screen in screens {
            let source = try readProjectFile(screen.file)
            XCTAssertTrue(
                source.contains("WakeveScreenBackground(style: .app)"),
                "\(screen.marker) doit poser le fond ivoire standard (DAO #27) au lieu du fond système."
            )
        }
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
            domain: "BackgroundCoverageContractTests",
            code: 1,
            userInfo: [NSLocalizedDescriptionKey: "Projet introuvable depuis le bundle de test : \(relativePath)"]
        )
    }
}
