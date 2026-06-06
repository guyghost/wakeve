import XCTest
@testable import Wakeve

final class PremiumParticipantsContractTests: XCTestCase {
    func testParticipantsUseGroupedPremiumHierarchy() throws {
        let source = try readProjectFile("iosApp/src/Views/Events/ParticipantManagementView.swift")
        let content = slice(source, from: "struct ParticipantManagementView", to: "private struct ParticipantPresentationRow")

        XCTAssertTrue(content.contains("EventHeroCard("))
        XCTAssertTrue(content.contains("LiquidGlassToolbar(title: \"Participants\""))
        XCTAssertTrue(content.contains("ParticipantGroupSection("))
        XCTAssertTrue(content.contains("title: \"Acceptés\""))
        XCTAssertTrue(content.contains("title: \"En attente\""))
        XCTAssertTrue(content.contains("title: \"Refusés\""))
    }

    func testParticipantsExposeContextualInviteAction() throws {
        let source = try readProjectFile("iosApp/src/Views/Events/ParticipantManagementView.swift")
        let content = slice(source, from: "struct ParticipantManagementView", to: "private struct ParticipantPresentationRow")

        XCTAssertTrue(content.contains("if event.status == .draft"))
        XCTAssertTrue(content.contains("addParticipantCard"))
        XCTAssertTrue(content.contains("Text(\"Inviter un participant\")"))
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
