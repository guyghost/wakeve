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

        XCTAssertTrue(shell.contains("PollBallotContract.CommitCompleteBallotCommand("))
        XCTAssertTrue(shell.contains("repository.commitCompleteBallot(command: command)"))
        XCTAssertEqual(
            shell.components(separatedBy: "repository.commitCompleteBallot(").count - 1,
            1,
            "PollVoting must submit one complete command through the atomic repository boundary."
        )
        XCTAssertFalse(
            shell.contains("repository.addVote("),
            "The shell must not loop over slot-level writes after the atomic ballot boundary exists."
        )
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

    func testLocalPendingBallotUsesDurableJournalAndDoesNotClaimServerDelivery() throws {
        let source = try readProjectFile("iosApp/src/Views/Polls/PollVotingView.swift")
        let shell = slice(source, from: "struct PollVotingView", to: "// MARK: - Poll Voting Content View")
        let submission = slice(source, from: "private func submitVotes()", to: "// MARK: - Poll Voting Content View")

        XCTAssertTrue(
            shell.contains("localPending") || shell.contains("LOCAL_PENDING"),
            "The UI must render a locally committed ballot as pending sync, not as sent."
        )
        XCTAssertTrue(
            shell.localizedCaseInsensitiveContains("journal") &&
                shell.localizedCaseInsensitiveContains("rehydrat"),
            "PollVoting must restore the exact DISPATCHED journal command after view recreation."
        )
        XCTAssertTrue(
            shell.localizedCaseInsensitiveContains("retry"),
            "Pending ballot sync needs a durable retry action using the same receipt and command."
        )
        XCTAssertFalse(
            submission.contains("hasVoted = true\n                WakeveHaptics.success()\n                showSuccess = true"),
            "LOCAL_PENDING is navigable local success, but must not display the server-sent success assertion."
        )
    }

    func testEveryBallotRetryRehydratesOneDurableDispatchedCommandBeforeRepositoryDispatch() throws {
        let source = try readProjectFile("iosApp/src/Views/Polls/PollVotingView.swift")
        let retry = slice(source, from: "private func retryPendingCommand()", to: "private func dispatchPersistedCommand")

        XCTAssertTrue(
            retry.contains("journal.loadDispatchableCommands") &&
                retry.contains("eventId: event.id") &&
                retry.contains("actorId: participantId"),
            "Retry must re-read the correlated durable journal status instead of trusting presentation memory."
        )
        XCTAssertFalse(
            retry.contains("dispatchPersistedCommand(pendingCommand)"),
            "A retained Swift command is not proof that the journal is still DISPATCHED."
        )
    }

    func testNonRetryableBallotFailureDoesNotRenderRetryAction() throws {
        let source = try readProjectFile("iosApp/src/Views/Polls/PollVotingView.swift")
        let alert = slice(source, from: ".alert(String(localized: \"common.error\")", to: ".alert(String(localized: \"common.success\")")
        let dispatch = slice(source, from: "private func dispatchPersistedCommand", to: "private func presentCommittedReceipt")

        XCTAssertTrue(
            source.contains("ballotRetryAvailable") || source.contains("canRetryBallotFailure"),
            "Retry visibility needs an explicit projection of the typed failure's retryable flag."
        )
        XCTAssertTrue(
            alert.contains("ballotRetryAvailable") || alert.contains("canRetryBallotFailure"),
            "A terminal failure must not expose Retry."
        )
        XCTAssertTrue(
            dispatch.contains("rejected.failure.retryable"),
            "The typed repository failure, not free-form error copy, owns retry eligibility."
        )
    }

    func testStageOrDispatchMarkFailureCannotReachAtomicRepositoryDispatch() throws {
        let source = try readProjectFile("iosApp/src/Views/Polls/PollVotingView.swift")
        let submit = slice(source, from: "private func submitVotes()", to: "private func rehydrateBallotJournal")

        let stageGuard = try XCTUnwrap(submit.range(of: "guard staged is BallotJournalResultStored"))
        let dispatchMarkGuard = try XCTUnwrap(submit.range(of: "guard dispatched is BallotJournalResultStored"))
        let repositoryDispatch = try XCTUnwrap(submit.range(of: "await dispatchPersistedCommand(command)"))
        XCTAssertLessThan(stageGuard.lowerBound, dispatchMarkGuard.lowerBound)
        XCTAssertLessThan(dispatchMarkGuard.lowerBound, repositoryDispatch.lowerBound)
        XCTAssertFalse(
            String(submit[submit.index(after: submit.firstIndex(of: "}") ?? submit.startIndex)...])
                .contains("repository.commitCompleteBallot"),
            "Failure handling must not bypass the DISPATCHED journal acknowledgement."
        )
    }

    func testBackDuringBallotDispatchMarksUnknownWithCorrelatedJournalCASBeforeDismissal() throws {
        let source = try readProjectFile("iosApp/src/Views/Polls/PollVotingView.swift")
        let shell = slice(source, from: "struct PollVotingView", to: "// MARK: - Poll Voting Content View")
        let back = slice(source, from: "private func handleBack", to: "private func submitVotes")

        XCTAssertTrue(
            shell.contains("onBack: { Task { await handleBack() } }") ||
                shell.contains("onBack: { Task { await handleBack") ,
            "Back must enter the ballot reducer instead of dismissing around an in-flight dispatch."
        )
        XCTAssertTrue(back.contains("markOutcomeUnknownIfDispatched"))
        XCTAssertTrue(
            back.contains("operationKey") && back.contains("ballotFingerprint"),
            "The unknown transition needs a compare-and-set on the exact durable command tuple."
        )
        XCTAssertTrue(
            back.contains("loadRehydrationProjections") && back.contains("onBack()"),
            "Dismissal follows correlated durable resolution evidence, never presentation memory."
        )
        XCTAssertFalse(
            shell.contains("onBack: onBack"),
            "Direct back navigation bypasses journal fencing while commit outcome is unknown."
        )
    }

    func testBackFromDispatchedBallotConsumesTerminalProofAndNeverClearsAfterFailedTombstoneCAS() throws {
        let source = try readProjectFile("iosApp/src/Views/Polls/PollVotingView.swift")
        let back = slice(source, from: "private func handleBack", to: "private func submitVotes")

        XCTAssertTrue(
            back.contains("terminalDestination"),
            "A rehydrated terminal destination is durable navigation evidence and must be consumed before dismissing."
        )
        XCTAssertTrue(
            back.localizedCaseInsensitiveContains("receipt") || back.localizedCaseInsensitiveContains("proof") || back.contains("tombstone"),
            "DISPATCHED back navigation must wait for a correlated receipt, proof, or cancellation tombstone."
        )
        XCTAssertFalse(
            back.contains("try? await journal.markOutcomeUnknownIfDispatched"),
            "A failed unknown-outcome CAS must remain visible and must never be treated as permission to clear or dismiss."
        )
        XCTAssertFalse(
            back.contains("guard marked is BallotJournalResultStored else { return }\n") && back.contains("onBack()"),
            "The CAS result must be rehydrated and terminalDestination consumed, not merely followed by a still-DISPATCHED row."
        )
    }

    func testRehydrationConsumesEveryTombstoneDestinationWithDistinctSemantics() throws {
        let source = try readProjectFile("iosApp/src/Views/Polls/PollVotingView.swift")
        let rehydrate = slice(source, from: "private func rehydrateBallotJournal", to: "private func retryPendingCommand")

        XCTAssertFalse(
            rehydrate.contains("case .cancelled, .dispatchCancellationTombstoned:"),
            "A plain cancelled stage and a DISPATCHED terminal tombstone are different durable outcomes."
        )
        XCTAssertTrue(
            rehydrate.contains("case .dispatchCancellationTombstoned:") &&
                rehydrate.contains("consumeTerminalDestination"),
            "Rehydration must consume the persisted destination instead of silently clearing the command."
        )
        for terminal in ["Cancelled", "Revised", "TerminalFailure"] {
            XCTAssertTrue(
                rehydrate.contains(terminal),
                "Rehydration is missing distinct handling for the (terminal) terminal destination."
            )
        }
    }

    func testBackConsumesOnlyCancelledTombstoneAndCallsOnBackExactlyOnce() throws {
        let source = try readProjectFile("iosApp/src/Views/Polls/PollVotingView.swift")
        let back = slice(source, from: "private func handleBack", to: "private func submitVotes")
        let tombstoned = slice(
            back,
            from: "case .dispatchCancellationTombstoned:",
            to: "default:"
        )
        let cancelled = slice(
            tombstoned,
            from: "case is PollBallotContractBallotTerminalDestinationCancelled:",
            to: "case is PollBallotContractBallotTerminalDestinationRevised:"
        )
        let revised = slice(
            tombstoned,
            from: "case is PollBallotContractBallotTerminalDestinationRevised:",
            to: "case is PollBallotContractBallotTerminalDestinationTerminalFailure:"
        )
        let terminalFailure = slice(
            tombstoned,
            from: "case is PollBallotContractBallotTerminalDestinationTerminalFailure:",
            to: "default:"
        )

        XCTAssertEqual(
            cancelled.components(separatedBy: "onBack()").count - 1,
            1,
            "A durable CANCELLED destination owns exactly one return effect."
        )
        XCTAssertEqual(
            revised.components(separatedBy: "onBack()").count - 1,
            0,
            "REVISED restores the revised ballot state and must not dismiss the poll."
        )
        XCTAssertEqual(
            terminalFailure.components(separatedBy: "onBack()").count - 1,
            0,
            "TERMINAL_FAILURE remains visible and must not dismiss the poll."
        )
    }

    func testRehydratedCancelledTombstoneReturnsExactlyOnceWhileRevisedAndFailureStayVisible() throws {
        let source = try readProjectFile("iosApp/src/Views/Polls/PollVotingView.swift")
        let rehydrate = slice(
            source,
            from: "private func rehydrateBallotJournal",
            to: "private func retryPendingCommand"
        )
        let tombstoned = slice(
            rehydrate,
            from: "case .dispatchCancellationTombstoned:",
            to: "default:"
        )
        let cancelled = slice(
            tombstoned,
            from: "case is PollBallotContractBallotTerminalDestinationCancelled:",
            to: "case is PollBallotContractBallotTerminalDestinationRevised:"
        )
        let revised = slice(
            tombstoned,
            from: "case is PollBallotContractBallotTerminalDestinationRevised:",
            to: "case is PollBallotContractBallotTerminalDestinationTerminalFailure:"
        )
        let terminalFailure = slice(
            tombstoned,
            from: "case is PollBallotContractBallotTerminalDestinationTerminalFailure:",
            to: "default:"
        )

        XCTAssertEqual(
            cancelled.components(separatedBy: "onBack()").count - 1,
            1,
            "Rehydrating durable CANCELLED consumes RETURN_TO_EVENT exactly once."
        )
        XCTAssertEqual(revised.components(separatedBy: "onBack()").count - 1, 0)
        XCTAssertEqual(terminalFailure.components(separatedBy: "onBack()").count - 1, 0)
    }

    func testUnknownDispatchedBallotBlocksNewUUIDAndRetriesThePersistedCommand() throws {
        let source = try readProjectFile("iosApp/src/Views/Polls/PollVotingView.swift")
        let submit = slice(source, from: "private func submitVotes", to: "private func rehydrateBallotJournal")
        let rehydrate = slice(source, from: "private func rehydrateBallotJournal", to: "private func retryPendingCommand")
        let retry = slice(source, from: "private func retryPendingCommand", to: "private func dispatchPersistedCommand")

        let durableRead = try XCTUnwrap(submit.range(of: "loadRehydrationProjections"))
        let uuid = try XCTUnwrap(submit.range(of: "UUID()"))
        XCTAssertLessThan(
            durableRead.lowerBound,
            uuid.lowerBound,
            "The durable journal gate must run before a new operation UUID can be allocated."
        )
        XCTAssertTrue(
            submit.contains(".dispatched") && submit.localizedCaseInsensitiveContains("unknown") && submit.contains("return"),
            "A DISPATCHED unknown outcome must reject a second submit instead of staging a second command."
        )
        XCTAssertTrue(
            rehydrate.contains("pendingCommand = command") &&
                rehydrate.contains("dispatchPersistedCommand(command)"),
            "Rehydration must retry the exact persisted command."
        )
        XCTAssertFalse(rehydrate.contains("UUID()"))
        XCTAssertTrue(
            retry.contains("loadDispatchableCommands") && retry.contains("dispatchPersistedCommand(command)"),
            "Manual retry must also use the one durable DISPATCHED command."
        )
    }

    func testClosedVotingChoicesAreSemanticallyDisabledForTouchAndVoiceOver() throws {
        let voting = try readProjectFile("iosApp/src/Views/Polls/PollVotingView.swift")
        let designSystem = try readProjectFile("iosApp/src/Components/DesignSystem/PremiumLiquidGlassComponents.swift")
        let options = slice(voting, from: "private var voteOptions", to: "private var selectedVoteFeedback")
        let card = slice(designSystem, from: "struct VoteOptionCard", to: "private var icon")

        XCTAssertEqual(
            options.components(separatedBy: "isDisabled: votingClosed").count - 1,
            3,
            "YES, MAYBE, and NO must all receive the modeled CLOSED_CHOICE affordance."
        )
        XCTAssertTrue(card.contains("let isDisabled: Bool"))
        XCTAssertTrue(card.contains(".disabled(isDisabled)"))
        XCTAssertTrue(
            card.contains("accessibility") && card.contains("isDisabled"),
            "VoiceOver must expose the same disabled semantic as touch."
        )
        XCTAssertFalse(
            options.contains(".allowsHitTesting(!votingClosed)"),
            "Hit-testing suppression alone leaves an enabled accessibility control."
        )
    }

    func testPollDecisionAnnouncementUsesInvitationAccessibilityIdentifier() throws {
        let source = try readProjectFile("iosApp/src/Views/Polls/PollResultsView.swift")
        let announcement = slice(source, from: "struct PollDecisionAnnouncementCard", to: "// MARK: - Slot Result Card")

        XCTAssertTrue(
            announcement.contains(".invitationAccessibilityIdentifier(\"pollDecisionAnnouncementCard\")"),
            "Invitation surfaces must use the shared identifier bridge so runtime probes work on every supported iOS."
        )
        XCTAssertFalse(
            announcement.contains(".accessibilityIdentifier(\"pollDecisionAnnouncementCard\")"),
            "The direct modifier bypasses the invitation compatibility bridge."
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
