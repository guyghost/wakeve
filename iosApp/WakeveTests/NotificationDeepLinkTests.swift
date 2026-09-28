import XCTest
@testable import Wakeve

final class NotificationDeepLinkTests: XCTestCase {
    func testPrefersExplicitDeepLink() {
        let url = NotificationDeepLink.url(from: ["deepLink": "wakeve://event/e1/poll", "eventId": "e1"])
        XCTAssertEqual(url?.absoluteString, "wakeve://event/e1/poll")
    }

    func testFallsBackToEventId() {
        XCTAssertEqual(NotificationDeepLink.url(from: ["eventId": "e1"])?.absoluteString, "wakeve://event/e1")
    }

    func testAcceptsURLValues() {
        let url = URL(string: "https://wakeve.app/event/e1")!
        XCTAssertEqual(NotificationDeepLink.url(from: ["deepLink": url]), url)
    }

    func testRejectsEmptyOrMissingValues() {
        XCTAssertNil(NotificationDeepLink.url(from: [:]))
        XCTAssertNil(NotificationDeepLink.url(from: ["deepLink": ""]))
        XCTAssertNil(NotificationDeepLink.url(from: ["eventId": ""]))
    }

    func testPercentEncodesEventIds() {
        XCTAssertEqual(NotificationDeepLink.url(from: ["eventId": "a b"])?.absoluteString, "wakeve://event/a%20b")
    }

    func testEncodesSlashesInEventIdsSoTheyCannotAddPathSegments() {
        XCTAssertEqual(
            NotificationDeepLink.url(from: ["eventId": "e1/poll"])?.absoluteString,
            "wakeve://event/e1%2Fpoll"
        )
    }
}
