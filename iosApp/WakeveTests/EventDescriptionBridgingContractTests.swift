import XCTest

/// Regression guard for event cards showing a raw Kotlin dump
/// ("Event(id=…, title=…") instead of the event description.
///
/// Kotlin's `description` property is bridged to Swift as `description_`; `event.description`
/// resolves to NSObject's `description`, i.e. the data class `toString()`.
final class EventDescriptionBridgingContractTests: XCTestCase {

    func testViewsNeverReadNSObjectDescriptionOfAKotlinEvent() throws {
        let viewsRoot = try XCTUnwrap(projectDirectory("iosApp/src/Views"))
        let enumerator = try XCTUnwrap(FileManager.default.enumerator(at: viewsRoot, includingPropertiesForKeys: nil))
        // `event.description` (not `description_`) is the Kotlin `toString()` dump.
        let forbidden = try NSRegularExpression(pattern: #"\bevent\??\.description\b(?!_)"#)
        var scanned = 0

        for case let url as URL in enumerator where url.pathExtension == "swift" {
            let source = try String(contentsOf: url, encoding: .utf8)
            scanned += 1
            let range = NSRange(source.startIndex..., in: source)
            XCTAssertNil(
                forbidden.firstMatch(in: source, range: range),
                "\(url.lastPathComponent) reads event.description; use event.description_"
            )
            XCTAssertFalse(
                source.contains("hasPrefix(\"Event(\")"),
                "\(url.lastPathComponent) must not parse the Kotlin toString() dump"
            )
        }

        XCTAssertGreaterThan(scanned, 0)
    }

    // MARK: - Helpers

    private func projectDirectory(_ relativePath: String) -> URL? {
        let fileURL = URL(fileURLWithPath: #filePath)
        let runtimeURL = URL(fileURLWithPath: FileManager.default.currentDirectoryPath)
        for startURL in [fileURL.deletingLastPathComponent(), runtimeURL] {
            var candidateRoot = startURL
            for _ in 0..<8 {
                let targetURL = candidateRoot.appendingPathComponent(relativePath)
                var isDirectory: ObjCBool = false
                if FileManager.default.fileExists(atPath: targetURL.path, isDirectory: &isDirectory), isDirectory.boolValue {
                    return targetURL
                }
                let parentURL = candidateRoot.deletingLastPathComponent()
                guard parentURL.path != candidateRoot.path else { break }
                candidateRoot = parentURL
            }
        }
        return nil
    }
}
