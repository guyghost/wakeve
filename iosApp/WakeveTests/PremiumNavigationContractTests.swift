import XCTest
@testable import Wakeve

final class PremiumNavigationContractTests: XCTestCase {
    func testWakeveTabsAreDestinationOnly() {
        XCTAssertEqual(WakeveTab.allCases, [.home, .groups, .messages, .profile])
        XCTAssertEqual(WakeveTab.allCases.map(\.title), ["Accueil", "Groupes", "Messages", "Profil"])
    }

    func testWakeveTabsDoNotExposeCreateEventAsDestination() {
        let rawValues = WakeveTab.allCases.map(\.rawValue)

        XCTAssertFalse(rawValues.contains("create"))
        XCTAssertFalse(rawValues.contains("eventCreation"))
        XCTAssertFalse(rawValues.contains("inbox"))
        XCTAssertFalse(rawValues.contains("explore"))
    }

    func testContentViewUsesProfileAsTabDestination() throws {
        let source = try readProjectFile("iosApp/src/Views/App/ContentView.swift")
        let tabView = slice(source, from: "TabView(selection: $selectedTab)", to: ".tint(.wakevePrimary)")
        let tabContent = slice(source, from: "private func tabContent(for tab: WakeveTab)", to: "private func isParticipantConfirmed")

        XCTAssertTrue(tabView.contains("tabContent(for: .profile)"))
        XCTAssertTrue(tabView.contains("WakeveTab.profile.title"))
        XCTAssertTrue(tabContent.contains("case .profile:"))
        XCTAssertTrue(tabContent.contains("ProfileTabView("))
        XCTAssertFalse(tabView.contains("tabContent(for: .inbox)"))
        XCTAssertFalse(tabView.contains("tabContent(for: .explore)"))
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
