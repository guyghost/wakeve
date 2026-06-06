import XCTest
@testable import Wakeve

final class PremiumPollVotingContractTests: XCTestCase {
    func testPollVotingUsesOneQuestionFlow() throws {
        let source = try readProjectFile("iosApp/src/Views/Polls/PollVotingView.swift")
        let content = slice(source, from: "struct PollVotingContentView", to: "// MARK: - Vote Guide Row")

        XCTAssertTrue(content.contains("@State private var activeSlotIndex"))
        XCTAssertTrue(content.contains("Text(\"Une question à la fois: est-ce que ce créneau vous convient ?\")"))
        XCTAssertTrue(content.contains("activeSlotQuestionCard"))
        XCTAssertTrue(content.contains("progressCard"))
        XCTAssertFalse(
            content.contains("ForEach(event.proposedSlots.indices"),
            "The premium vote flow should focus on one active time slot instead of rendering every slot at once."
        )
    }

    func testPollVotingUsesPremiumVoteOptionsAndCapsuleAction() throws {
        let source = try readProjectFile("iosApp/src/Views/Polls/PollVotingView.swift")
        let content = slice(source, from: "struct PollVotingContentView", to: "// MARK: - Vote Guide Row")

        XCTAssertTrue(content.contains("VoteOptionCard("))
        XCTAssertTrue(content.contains("selectedVoteFeedback"))
        XCTAssertTrue(content.contains("LiquidGlassToolbar(title: \"Vote\""))
        XCTAssertTrue(content.contains("LiquidGlassButton("))
        XCTAssertTrue(content.contains("nextActionTitle"))
    }

    func testPollVotingPreservesRepositorySubmissionBoundary() throws {
        let source = try readProjectFile("iosApp/src/Views/Polls/PollVotingView.swift")
        let shell = slice(source, from: "struct PollVotingView", to: "// MARK: - Poll Voting Content View")
        let content = slice(source, from: "struct PollVotingContentView", to: "// MARK: - Vote Guide Row")

        XCTAssertTrue(shell.contains("repository.addVote("))
        XCTAssertTrue(shell.contains("submitVotes()"))
        XCTAssertTrue(content.contains("onSubmitVotes()"))
        XCTAssertFalse(
            content.contains("repository.addVote("),
            "PollVotingContentView should remain presentation-only and delegate submission to the shell view."
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
