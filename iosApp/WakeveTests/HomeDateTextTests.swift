import XCTest
@testable import Wakeve

final class HomeDateTextTests: XCTestCase {
    private var utc: Calendar {
        var c = Calendar(identifier: .gregorian)
        c.timeZone = TimeZone(identifier: "UTC")!
        return c
    }

    func testDaysBetweenCountsCalendarDays() {
        let f = ISO8601DateFormatter()
        XCTAssertEqual(HomeDateText.daysBetween(f.date(from: "2026-10-01T23:00:00Z")!, f.date(from: "2026-10-02T01:00:00Z")!, calendar: utc), 1)
        XCTAssertEqual(HomeDateText.daysBetween(f.date(from: "2026-10-03T00:00:00Z")!, f.date(from: "2026-10-01T00:00:00Z")!, calendar: utc), 0)
    }

    func testShortDateIsLocalized() {
        let date = ISO8601DateFormatter().date(from: "2026-10-10T12:00:00Z")!
        let fr = HomeDateText.short(date, locale: Locale(identifier: "fr_FR"), calendar: utc)
        XCTAssertTrue(fr.lowercased().contains("10"), fr)
        XCTAssertTrue(fr.lowercased().contains("oct"), fr)
    }

    func testCachedShortFormatterStillHonoursEachLocale() {
        let date = ISO8601DateFormatter().date(from: "2026-10-10T12:00:00Z")!
        let fr = HomeDateText.short(date, locale: Locale(identifier: "fr_FR"), calendar: utc)
        let en = HomeDateText.short(date, locale: Locale(identifier: "en_US"), calendar: utc)
        XCTAssertNotEqual(fr, en)
        XCTAssertEqual(fr, HomeDateText.short(date, locale: Locale(identifier: "fr_FR"), calendar: utc))
    }

    func testParsesKotlinIsoStrings() {
        XCTAssertNotNil(HomeDateText.parseISO("2026-10-10T12:00:00Z"))
        XCTAssertNotNil(HomeDateText.parseISO("2026-10-10T12:00:00.123Z"))
        XCTAssertNil(HomeDateText.parseISO(""))
    }

    func testHomeKeysExistInEveryLocale() throws {
        let keys = [
            "home.v2.status.draft", "home.v2.status.vote_required", "home.v2.status.ready_to_confirm",
            "home.v2.status.polling", "home.v2.status.organizing", "home.v2.status.confirmed", "home.v2.status.past",
            "home.v2.next_step.caption_format", "home.v2.next_step.vote.subtitle", "home.v2.next_step.ready.subtitle",
            "home.v2.next_step.polling.subtitle", "home.v2.next_step.organizing.subtitle",
            "home.v2.next_step.action.vote", "home.v2.next_step.action.results", "home.v2.next_step.action.organize",
            "home.v2.section.past", "home.v2.empty.title", "home.v2.empty.body", "home.v2.empty.action",
            "home.v2.menu.open", "home.v2.menu.edit", "home.v2.menu.delete", "home.v2.sync.pending", "wk.wordmark"
        ]
        let root = URL(fileURLWithPath: #filePath).deletingLastPathComponent().deletingLastPathComponent()
        for locale in ["en", "fr", "es", "it", "pt"] {
            let strings = try String(contentsOf: root.appendingPathComponent("src/Resources/\(locale).lproj/Localizable.strings"), encoding: .utf8)
            for key in keys {
                XCTAssertTrue(strings.contains("\"\(key)\""), "\(key) manquante (\(locale))")
            }
            let dict = try String(contentsOf: root.appendingPathComponent("src/Resources/\(locale).lproj/Localizable.stringsdict"), encoding: .utf8)
            XCTAssertTrue(dict.contains("<key>home.v2.closes_in_days</key>"), "pluriel manquant (\(locale))")
        }
    }
}
