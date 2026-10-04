import XCTest
@testable import Wakeve

final class WKAvatarStackTests: XCTestCase {

    private func avatars(_ names: [String]) -> [WKAvatar] {
        names.enumerated().map { WKAvatar(id: "\($0.offset)", name: $0.element) }
    }

    func testShowsAllWhenWithinLimit() {
        let layout = WKAvatarStack.layout(count: 3, maxVisible: 4)
        XCTAssertEqual(layout.visible, 3)
        XCTAssertEqual(layout.overflow, 0)
    }

    func testCollapsesOverflowIntoCounter() {
        let layout = WKAvatarStack.layout(count: 9, maxVisible: 4)
        XCTAssertEqual(layout.visible, 4)
        XCTAssertEqual(layout.overflow, 5)
    }

    func testEmptyStack() {
        let layout = WKAvatarStack.layout(count: 0, maxVisible: 4)
        XCTAssertEqual(layout.visible, 0)
        XCTAssertEqual(layout.overflow, 0)
    }

    func testInitialsUseFirstLettersOfFirstTwoWords() {
        XCTAssertEqual(WKAvatar(id: "1", name: "léa martin").initials, "LM")
        XCTAssertEqual(WKAvatar(id: "2", name: "Tom").initials, "T")
        XCTAssertEqual(WKAvatar(id: "3", name: "  ").initials, "?")
    }

    func testAccessibilityLabelNamesFirstTwoThenCountsOthers() {
        let label = WKAvatarStack.accessibilityLabel(for: avatars(["Léa", "Tom", "Max", "Sam", "Zoé", "Ana"]), locale: Locale(identifier: "en"))
        XCTAssertTrue(label.contains("Léa"))
        XCTAssertTrue(label.contains("Tom"))
        XCTAssertFalse(label.contains("Max"))
        XCTAssertTrue(label.contains("4"))
    }

    func testAccessibilityLabelListsEveryoneWhenThreeOrLess() {
        let label = WKAvatarStack.accessibilityLabel(for: avatars(["Léa", "Tom", "Max"]), locale: Locale(identifier: "en"))
        XCTAssertEqual(label, "Léa, Tom, and Max")
    }

    func testAccessibilityLabelIsFullyFrenchForFrenchLocale() {
        let label = WKAvatarStack.accessibilityLabel(for: avatars(["Léa", "Tom", "Max", "Sam", "Zoé", "Ana"]), locale: Locale(identifier: "fr"))
        XCTAssertEqual(label, "Léa, Tom et 4 autres")
    }

    func testAccessibilityLabelSkipsBlankNames() {
        let label = WKAvatarStack.accessibilityLabel(for: avatars(["Léa", "  ", "Tom"]), locale: Locale(identifier: "en"))
        XCTAssertEqual(label, "Léa and Tom")
    }

    func testTintIndexIsDeterministicAndInRange() {
        let ids = ["", "a", "uuid-123", String(repeating: "x", count: 500)]
        for id in ids {
            let first = WKAvatar.tintIndex(for: id, paletteCount: 6)
            let second = WKAvatar.tintIndex(for: id, paletteCount: 6)
            XCTAssertEqual(first, second, "id=\(id)")
            XCTAssertTrue((0..<6).contains(first), "id=\(id) index=\(first)")
        }
    }
}
