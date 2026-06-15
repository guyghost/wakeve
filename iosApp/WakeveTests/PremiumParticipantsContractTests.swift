import XCTest
@testable import Wakeve

final class PremiumParticipantsContractTests: XCTestCase {
    func testParticipantsUseGroupedPremiumHierarchy() throws {
        let source = try readProjectFile("iosApp/src/Views/Events/ParticipantManagementView.swift")
        let content = slice(source, from: "struct ParticipantManagementView", to: "private struct ParticipantPresentationRow")

        XCTAssertTrue(content.contains("EventHeroCard("))
        XCTAssertTrue(content.contains("LiquidGlassToolbar(title: String(localized: \"participants.title\")"))
        XCTAssertTrue(content.contains("ParticipantGroupSection("))
        XCTAssertTrue(content.contains("title: String(localized: \"participants.accepted\")"))
        XCTAssertTrue(content.contains("title: String(localized: \"participants.pending\")"))
        XCTAssertTrue(content.contains("title: String(localized: \"participants.declined\")"))
    }

    func testParticipantGroupsUseBrandContentCards() throws {
        let source = try readProjectFile("iosApp/src/Views/Events/ParticipantManagementView.swift")
        let section = slice(source, from: "private struct ParticipantGroupSection", to: "private struct ParticipantRowView")

        XCTAssertTrue(section.contains("WakeveGroupCard("))
        XCTAssertTrue(section.contains("SemanticColor.secondaryText"))
        XCTAssertTrue(section.contains("SemanticColor.badge"))
        XCTAssertFalse(section.contains("WakeveGlassCard("), "Participant groups are content surfaces and should not use glass cards.")
    }

    func testParticipantsExposeContextualInviteAction() throws {
        let source = try readProjectFile("iosApp/src/Views/Events/ParticipantManagementView.swift")
        let content = slice(source, from: "struct ParticipantManagementView", to: "private struct ParticipantPresentationRow")

        XCTAssertTrue(content.contains("if event.status == .draft"))
        XCTAssertTrue(content.contains("addParticipantCard"))
        XCTAssertTrue(content.contains("WakeveContentCard(prominence: .regular"))
        XCTAssertTrue(content.contains("String(localized: \"participants.invite_one\")"))
        XCTAssertTrue(content.contains("LiquidGlassButton("))
        XCTAssertTrue(content.contains("canStartPoll"))
    }

    func testParticipantsPreserveRepositoryBoundary() throws {
        let source = try readProjectFile("iosApp/src/Views/Events/ParticipantManagementView.swift")
        let content = slice(source, from: "struct ParticipantManagementView", to: "private struct ParticipantPresentationRow")

        XCTAssertTrue(content.contains("repository?.getParticipantRecords"))
        XCTAssertTrue(content.contains("repository.addParticipant"))
        XCTAssertTrue(content.contains("repository.updateEventStatus"))
        XCTAssertTrue(content.contains("!event.proposedSlots.isEmpty"))
        XCTAssertFalse(
            content.contains("StateMachine("),
            "Participants should remain a SwiftUI presentation shell around repository calls."
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
