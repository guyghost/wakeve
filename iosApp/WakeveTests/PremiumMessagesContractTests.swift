import XCTest
@testable import Wakeve

final class PremiumMessagesContractTests: XCTestCase {
    func testMessagesUseEventConversationHierarchy() throws {
        let source = try readProjectFile("iosApp/src/Views/Inbox/InboxView.swift")
        let content = slice(source, from: "struct InboxView", to: "// MARK: - Filter Tab Button")

        XCTAssertTrue(content.contains("Text(\"Messages\")"))
        XCTAssertTrue(content.contains("eventConversations"))
        XCTAssertTrue(content.contains("filteredConversations"))
        XCTAssertTrue(content.contains("EventConversationRow(conversation: conversation)"))
        XCTAssertTrue(content.contains("markConversationAsRead"))
    }

    func testMessagesExposeSearchUnreadAndEventContext() throws {
        let source = try readProjectFile("iosApp/src/Views/Inbox/InboxView.swift")
        let content = slice(source, from: "struct InboxView", to: "// MARK: - Filter Tab Button")
        let row = slice(source, from: "private struct EventConversationRow", to: "struct InboxRow")

        XCTAssertTrue(content.contains("@State private var searchText"))
        XCTAssertTrue(content.contains("WakeveSearchField("))
        XCTAssertTrue(content.contains("localizedCaseInsensitiveContains"))
        XCTAssertTrue(row.contains("conversation.unreadCount"))
        XCTAssertTrue(row.contains("conversation.eventName"))
        XCTAssertTrue(row.contains("conversation.requiresAction"))
    }

    func testMessagesContentRowsUseSemanticBrandLayer() throws {
        let source = try readProjectFile("iosApp/src/Views/Inbox/InboxView.swift")
        let content = slice(source, from: "struct InboxView", to: "// MARK: - Filter Tab Button")
        let row = slice(source, from: "private struct EventConversationRow", to: "struct InboxRow")

        XCTAssertTrue(content.contains("SemanticColor.appBackground"))
        XCTAssertTrue(row.contains("WakeveContentCard("))
        XCTAssertTrue(row.contains("SemanticColor.selectedState"))
        XCTAssertTrue(row.contains("SemanticColor.warning"))
        XCTAssertFalse(row.contains("LiquidGlassCard("), "Message conversation rows are content and should not use Liquid Glass.")
    }

    func testMessagesPreserveInboxModelAndViewModelBoundary() throws {
        let source = try readProjectFile("iosApp/src/Views/Inbox/InboxView.swift")

        XCTAssertTrue(source.contains("@StateObject private var viewModel: InboxViewModel"))
        XCTAssertTrue(source.contains("viewModel.loadNotifications()"))
        XCTAssertTrue(source.contains("viewModel.markAsRead"))
        XCTAssertTrue(source.contains("InboxDetailView(item: latest)"))
        XCTAssertFalse(
            source.contains("StateMachine("),
            "Messages should remain a SwiftUI presentation over InboxViewModel notifications."
        )
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
