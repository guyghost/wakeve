import XCTest
@testable import Wakeve

/// Le détail legacy (`EventDetailView`) est supprimé en couche 9 ; seul reste le contrat de `EventInfoSheet`.
final class PremiumEventDetailContractTests: XCTestCase {
    func testEventInfoSheetUsesLocalizationKeys() throws {
        let source = try readProjectFile("iosApp/src/Components/EventInfoSheet.swift")

        XCTAssertTrue(source.contains("event_info.title"))
        XCTAssertTrue(source.contains("event_info.description_title"))
        XCTAssertTrue(source.contains("event_info.character_limit_format"))
        XCTAssertTrue(source.contains("event_info.profile_help"))
        XCTAssertFalse(source.contains("Informations sur l'évènement"))
        XCTAssertFalse(source.contains("Limite de caractères"))
    }

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

                let parentURL = candidateRoot.deletingLastPathComponent()
                guard parentURL.path != candidateRoot.path else { break }
                candidateRoot = parentURL
            }
        }

        throw CocoaError(.fileNoSuchFile)
    }
}
