import XCTest
@testable import Wakeve

final class InvitationExperienceLocalizationRedTests: XCTestCase {
    private let locales = ["en", "fr", "es", "it", "pt"]
    private let requiredKeys = [
        "invitation.library.title",
        "invitation.library.filter.drafts",
        "invitation.library.filter.hosting",
        "invitation.library.filter.attending",
        "invitation.library.filter.upcoming",
        "invitation.library.filter.past",
        "invitation.studio.title",
        "invitation.studio.preview",
        "invitation.studio.pending_sync",
        "invitation.audience.title",
        "invitation.information.title",
        "invitation.archive.title",
        "invitation.archive.read_only",
        "invitation.state.pending_sync",
        "invitation.state.stale",
        "invitation.state.unavailable",
        "invitation.action.view_archive",
        "invitation.action.reload_projection",
        "common.retry"
    ]

    func testAllInvitationExperienceKeysExistInEverySupportedCatalog() throws {
        for locale in locales {
            let catalog = try readProjectFile("iosApp/src/Resources/\(locale).lproj/Localizable.strings")
            for key in requiredKeys {
                XCTAssertTrue(catalog.contains("\"\(key)\""), "\(locale) is missing \(key)")
            }
        }
    }

    func testPrimaryActionCopyCanGrowWithoutAOneLineTruncationContract() {
        let paths = [
            "iosApp/src/Views/Invitations/EventLibraryView.swift",
            "iosApp/src/Views/Invitations/EventCreationStudioView.swift",
            "iosApp/src/Views/Invitations/EventAudienceView.swift",
            "iosApp/src/Views/Invitations/EventInformationView.swift",
            "iosApp/src/Views/Invitations/EventArchiveView.swift"
        ]

        for path in paths {
            let source = readProjectFileIfPresent(path)
            XCTAssertTrue(source.contains("fixedSize(horizontal: false, vertical: true)"), "\(path) must allow long action/state copy to wrap.")
            XCTAssertFalse(source.contains("lineLimit(1)"), "\(path) must not truncate long localized state or action copy to one line.")
        }
    }

    func testStudioArtworkChoiceCopyCanWrapWithoutFixedHeightOrSingleLineClipping() throws {
        let studio = try readProjectFile(
            "iosApp/src/Views/Invitations/EventCreationStudioView.swift"
        )
        guard let start = studio.range(of: "private func artworkButton("),
              let end = studio.range(of: "\n    }\n}", range: start.upperBound..<studio.endIndex) else {
            return XCTFail("Missing Studio artwork button implementation.")
        }
        let artworkButton = String(studio[start.lowerBound..<end.upperBound])

        XCTAssertTrue(
            artworkButton.contains("fixedSize(horizontal: false, vertical: true)"),
            "Artwork labels must preserve every localized line instead of compressing inside circular buttons."
        )
        XCTAssertFalse(artworkButton.contains("lineLimit(1)"))
        XCTAssertFalse(artworkButton.contains(".frame(height:"))
    }

    private func readProjectFileIfPresent(_ relativePath: String) -> String {
        (try? readProjectFile(relativePath)) ?? ""
    }

    private func readProjectFile(_ relativePath: String) throws -> String {
        let fileURL = URL(fileURLWithPath: #filePath)
        let testsDirectory = fileURL.deletingLastPathComponent()
        let iosAppDirectory = testsDirectory.deletingLastPathComponent()
        let projectRoot = iosAppDirectory.deletingLastPathComponent()
        return try String(
            contentsOf: projectRoot.appendingPathComponent(relativePath),
            encoding: .utf8
        )
    }
}
