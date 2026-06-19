import XCTest
@testable import Wakeve

final class PremiumPollVotingContractTests: XCTestCase {
    func testPollVotingUsesOneQuestionFlow() throws {
        let source = try readProjectFile("iosApp/src/Views/Polls/PollVotingView.swift")
        let content = slice(source, from: "struct PollVotingContentView", to: "// MARK: - Vote Guide Row")

        XCTAssertTrue(content.contains("@State private var activeSlotIndex"))
        XCTAssertTrue(content.contains("String(localized: \"poll.voting.header_question\")"))
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
        XCTAssertTrue(content.contains("LiquidGlassToolbar("))
        XCTAssertTrue(content.contains("String(localized: \"poll.voting.title\")"))
        XCTAssertTrue(content.contains("poll.voting.responses_progress_format"))
        XCTAssertTrue(content.contains("LiquidGlassButton("))
        XCTAssertTrue(content.contains("nextActionTitle"))
        XCTAssertTrue(content.contains("private func selectVote(_ vote: PollVote)"))
        XCTAssertTrue(content.contains("WakeveHaptics.selection()"))
    }

    func testPollVotingContentCardsUseSemanticBrandLayer() throws {
        let source = try readProjectFile("iosApp/src/Views/Polls/PollVotingView.swift")
        let content = slice(source, from: "struct PollVotingContentView", to: "// MARK: - Vote Guide Row")

        XCTAssertTrue(content.contains("SemanticColor.appBackground"))
        XCTAssertTrue(content.contains("WakeveContentCard("))
        XCTAssertTrue(content.contains("SemanticColor.progress"))
        XCTAssertFalse(content.contains("LiquidGlassCard("), "Poll content cards should not use Liquid Glass.")
        XCTAssertFalse(content.contains("WakeveGlassCard("), "Poll content cards should use WakeveContentCard.")
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

    func testPollResultsExposeShareableDecisionAnnouncement() throws {
        let source = try readProjectFile("iosApp/src/Views/Polls/PollResultsView.swift")
        let content = slice(source, from: "struct PollResultsContentView", to: "// MARK: - Best Slot Card")
        let announcement = slice(source, from: "struct PollDecisionAnnouncementCard", to: "// MARK: - Slot Result Card")

        XCTAssertTrue(
            content.contains("PollDecisionAnnouncementCard("),
            "Poll results must turn the winning slot into an explicit next action, not only a score display."
        )
        XCTAssertTrue(
            announcement.contains("ShareLink(item: announcementMessage)"),
            "Poll decision announcement must use the native iOS share sheet for WhatsApp/iMessage handoff."
        )
        XCTAssertTrue(
            announcement.contains("copyAnnouncementMessage()")
                && announcement.contains("UIPasteboard.general.string = announcementMessage")
                && announcement.contains("WakeveHaptics.success()"),
            "Poll decision announcement must also support a reliable copy handoff with success feedback."
        )
        XCTAssertTrue(
            announcement.contains("pollDecisionAnnouncementShareLink")
                && announcement.contains("pollDecisionAnnouncementCopyButton")
                && announcement.contains("pollDecisionAnnouncementCopiedFeedback"),
            "Poll decision announcement share, copy, and copied states need stable identifiers for UI verification."
        )
        XCTAssertTrue(
            announcement.contains(".simultaneousGesture(TapGesture().onEnded")
                && announcement.contains("WakeveHaptics.selection()"),
            "Sharing the decision announcement should provide immediate tactile feedback before the native share sheet opens."
        )
        XCTAssertTrue(
            announcement.contains("poll.results.announcement.share_action")
                && announcement.contains("poll.results.announcement.pending_title")
                && announcement.contains("poll.results.announcement.pending_message_format"),
            "Poll decision announcement must provide localized organizer-facing copy ready to send."
        )
        XCTAssertFalse(
            announcement.contains("Partager l’annonce") || announcement.contains("Message prêt à envoyer"),
            "Poll decision announcement should not hardcode French copy."
        )

        for locale in ["en", "fr", "es", "it", "pt"] {
            let strings = try readProjectFile("iosApp/src/Resources/\(locale).lproj/Localizable.strings")
            XCTAssertTrue(strings.contains("\"poll.results.announcement.copy_action\""))
            XCTAssertTrue(strings.contains("\"poll.results.announcement.copied\""))
        }
    }

    func testPollResultsConfirmedStateShowsOperationalNextSteps() throws {
        let source = try readProjectFile("iosApp/src/Views/Polls/PollResultsView.swift")
        let content = slice(source, from: "struct PollResultsContentView", to: "// MARK: - Best Slot Card")
        let nextSteps = slice(source, from: "struct PollResolutionNextStepsCard", to: "// MARK: - Slot Result Card")

        XCTAssertTrue(
            content.contains("PollResolutionNextStepsCard()"),
            "A confirmed poll should show what happens next instead of ending at a success state."
        )
        XCTAssertTrue(nextSteps.contains("pollResolutionNextStepsCard"))
        XCTAssertTrue(nextSteps.contains("poll.results.next_steps.title"))
        XCTAssertTrue(nextSteps.contains("poll.results.next_steps.announce_title"))
        XCTAssertTrue(nextSteps.contains("poll.results.next_steps.calendar_title"))
        XCTAssertTrue(nextSteps.contains("poll.results.next_steps.plan_title"))
        XCTAssertTrue(nextSteps.contains("poll.results.next_steps.owners_title"))
        XCTAssertTrue(nextSteps.contains("megaphone.fill"))
        XCTAssertTrue(nextSteps.contains("calendar.badge.checkmark"))
        XCTAssertTrue(nextSteps.contains("map.fill"))
        XCTAssertTrue(nextSteps.contains("checklist.checked"))

        for locale in ["en", "fr", "es", "it", "pt"] {
            let strings = try readProjectFile("iosApp/src/Resources/\(locale).lproj/Localizable.strings")
            for key in [
                "poll.results.next_steps.title",
                "poll.results.next_steps.subtitle",
                "poll.results.next_steps.announce_title",
                "poll.results.next_steps.announce_detail",
                "poll.results.next_steps.calendar_title",
                "poll.results.next_steps.calendar_detail",
                "poll.results.next_steps.plan_title",
                "poll.results.next_steps.plan_detail",
                "poll.results.next_steps.owners_title",
                "poll.results.next_steps.owners_detail"
            ] {
                XCTAssertTrue(strings.contains("\"\(key)\""), "\(locale) is missing \(key)")
            }
        }
    }

    func testPollResultsDateFormattingUsesUserLocale() throws {
        let source = try readProjectFile("iosApp/src/Views/Polls/PollResultsView.swift")
        let content = slice(source, from: "private func formatDate", to: "// MARK: - Slot Result Card")

        XCTAssertTrue(content.contains("formatter.locale = .autoupdatingCurrent"))
        XCTAssertTrue(content.contains("formatter.timeZone = timeZone(for: timezone)"))
        XCTAssertTrue(content.contains("poll.results.announcement.time_at_format"))
        XCTAssertTrue(content.contains("poll.results.announcement.time_range_format"))
        XCTAssertFalse(content.contains("Locale(identifier: \"fr_FR\")"), "Poll result dates and times should respect the user's locale.")
        XCTAssertFalse(content.contains("Locale.current"), "Poll result dates and times should follow autoupdating locale changes.")
    }

    func testPollTimezoneIsVisibleAndSlotBased() throws {
        let votingSource = try readProjectFile("iosApp/src/Views/Polls/PollVotingView.swift")
        let votingContent = slice(votingSource, from: "struct PollVotingContentView", to: "// MARK: - Vote Guide Row")
        let timezoneBadge = slice(votingSource, from: "struct PollTimeZoneBadge", to: "// MARK: - Preview")

        XCTAssertTrue(votingContent.contains("PollTimeZoneBadge("))
        XCTAssertTrue(votingContent.contains("formatTimeZoneLabel(activeSlot.timezone, at: activeSlot.start)"))
        XCTAssertTrue(votingContent.contains("formatter.timeZone = timeZone(for: timezone)"))
        XCTAssertTrue(votingContent.contains("poll.timezone.label_format"))
        XCTAssertTrue(timezoneBadge.contains("Label(label, systemImage: \"globe\")"))

        let resultsSource = try readProjectFile("iosApp/src/Views/Polls/PollResultsView.swift")
        let resultsContent = slice(resultsSource, from: "struct BestSlotCard", to: "// MARK: - Vote Count Badge")
        let announcement = slice(resultsSource, from: "struct PollDecisionAnnouncementCard", to: "// MARK: - Slot Result Card")

        XCTAssertTrue(resultsContent.contains("PollTimeZoneBadge("))
        XCTAssertTrue(resultsContent.contains("formatter.timeZone = timeZone(for: timezone)"))
        XCTAssertTrue(announcement.contains("formatTimeRange(start: slot.start, end: slot.end, timezone: slot.timezone)"))
        XCTAssertTrue(announcement.contains("formatTimeZoneDisplay(timezone, at: start)"))
        XCTAssertFalse(
            resultsContent.contains("formatTime(slot.start ?? \"\")"),
            "Poll result cards should format times in the slot timezone, not the viewer device timezone."
        )

        for locale in ["en", "fr", "es", "it", "pt"] {
            let strings = try readProjectFile("iosApp/src/Resources/\(locale).lproj/Localizable.strings")
            XCTAssertTrue(strings.contains("\"poll.timezone.label_format\""))
        }
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
