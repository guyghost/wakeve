import XCTest
@testable import Wakeve

/// Textes du fil d'activité (couche 6, #47) : présents dans les 5 langues, français au tutoiement.
final class ActivityLocalizationTests: XCTestCase {
    static let stringKeys = [
        "activity.feed.filter.todo_format", "activity.feed.filter.all",
        "activity.feed.vote_required", "activity.feed.ready_to_confirm", "activity.feed.rsvp_pending",
        "activity.feed.empty.todo", "activity.feed.empty.all",
        "activity.feed.a11y.to_do", "activity.feed.unread",
        "activity.feed.general", "activity.feed.filter.label"
    ]
    static let pluralKeys = ["activity.feed.messages_count"]

    private var resources: URL {
        URL(fileURLWithPath: #filePath).deletingLastPathComponent().deletingLastPathComponent()
            .appendingPathComponent("src/Resources")
    }

    func testActivityKeysExistInEveryLanguage() throws {
        for locale in ["en", "fr", "es", "it", "pt"] {
            let strings = try String(contentsOf: resources.appendingPathComponent("\(locale).lproj/Localizable.strings"), encoding: .utf8)
            for key in Self.stringKeys {
                XCTAssertTrue(strings.contains("\"\(key)\" ="), "\(key) manquante (\(locale))")
            }
            let dict = try String(contentsOf: resources.appendingPathComponent("\(locale).lproj/Localizable.stringsdict"), encoding: .utf8)
            for key in Self.pluralKeys {
                XCTAssertTrue(dict.contains("<key>\(key)</key>"), "pluriel \(key) manquant (\(locale))")
            }
        }
    }

    func testFrenchCopyUsesTutoiementAndPlurals() {
        let fr = Locale(identifier: "fr")
        XCTAssertEqual(WK.localizedFormat("activity.feed.vote_required", locale: fr), "Vote : il manque ton vote")
        XCTAssertEqual(WK.localizedFormat("activity.feed.ready_to_confirm", locale: fr), "Tout le monde a voté, choisis la date")
        XCTAssertEqual(WK.localizedFormat("activity.feed.rsvp_pending", locale: fr), "Réponds à l'invitation")
        XCTAssertEqual(HubSummaryText.plural("activity.feed.messages_count", 1, locale: fr), "1 nouveau message")
        XCTAssertEqual(HubSummaryText.plural("activity.feed.messages_count", 3, locale: fr), "3 nouveaux messages")
        XCTAssertEqual(HubSummaryText.plural("activity.feed.messages_count", 1, locale: Locale(identifier: "en")), "1 new message")
        XCTAssertEqual(HubSummaryText.plural("activity.feed.messages_count", 2, locale: Locale(identifier: "en")), "2 new messages")
        XCTAssertEqual(
            String(format: WK.localizedFormat("activity.feed.filter.todo_format", locale: fr), locale: fr, 2),
            "À traiter (2)"
        )
    }
}
