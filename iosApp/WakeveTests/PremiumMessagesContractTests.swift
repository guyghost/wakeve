import XCTest
@testable import Wakeve

final class PremiumMessagesContractTests: XCTestCase {
    func testCollaborationCommentsUseLocalizedActionsAndTimestamps() throws {
        let source = try readProjectFile("iosApp/src/Views/Collaboration/CommentItemView.swift")

        for key in [
            "comment.action.reply",
            "comment.action.edit",
            "comment.action.pin",
            "comment.action.unpin",
            "comment.deleted",
            "comment.time.just_now",
            "comment.time.minutes_ago_format",
            "comment.time.hours_ago_format",
            "comment.time.days_ago_format",
            "comment.time.weeks_ago_format",
            "comment.time.unknown",
            "common.delete",
            "common.remove"
        ] {
            XCTAssertTrue(source.contains("\"\(key)\""), "Comment item should use localized key \(key).")
        }

        for hardcodedCopy in [
            "Label(\"Reply\"",
            "Label(\"Edit\"",
            "\"Unpin\" : \"Pin\"",
            "\"Delete\" : \"Remove\"",
            "Text(\"Reply\")",
            "Text(\"[Deleted]\")",
            "return \"Just now\"",
            "return \"Unknown time\"",
            "m ago\"",
            "h ago\"",
            "d ago\"",
            "w ago\""
        ] {
            XCTAssertFalse(source.contains(hardcodedCopy), "Comment item should not hardcode visible copy: \(hardcodedCopy).")
        }

        for locale in ["en", "fr", "es", "it", "pt"] {
            let strings = try readProjectFile("iosApp/src/Resources/\(locale).lproj/Localizable.strings")
            for key in [
                "comment.action.reply",
                "comment.action.edit",
                "comment.action.pin",
                "comment.action.unpin",
                "comment.deleted",
                "comment.time.just_now",
                "comment.time.minutes_ago_format",
                "comment.time.hours_ago_format",
                "comment.time.days_ago_format",
                "comment.time.weeks_ago_format",
                "comment.time.unknown",
                "common.remove"
            ] {
                XCTAssertTrue(strings.contains("\"\(key)\""), "Missing localized comment key \(key) for \(locale).")
            }
        }
    }

    private func readProjectFile(_ relativePath: String) throws -> String {
        let fileURL = URL(fileURLWithPath: #filePath)
        let testsDir = fileURL.deletingLastPathComponent()
        let iosAppDir = testsDir.deletingLastPathComponent()
        let projectRoot = iosAppDir.deletingLastPathComponent()
        let targetURL = projectRoot.appendingPathComponent(relativePath)
        return try String(contentsOf: targetURL, encoding: .utf8)
    }

    private func slice(_ source: String, from startMarker: String, to endMarker: String) -> String {
        guard let start = source.range(of: startMarker)?.lowerBound else {
            return source
        }

        let tail = source[start...]
        guard let end = tail.range(of: endMarker)?.lowerBound else {
            return String(tail)
        }

        return String(tail[..<end])
    }
}
