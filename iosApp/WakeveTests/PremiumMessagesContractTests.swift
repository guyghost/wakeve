import XCTest
@testable import Wakeve

final class PremiumMessagesContractTests: XCTestCase {
    func testMessagesUseEventConversationHierarchy() throws {
        let source = try readProjectFile("iosApp/src/Views/Inbox/InboxView.swift")
        let content = slice(source, from: "struct InboxView", to: "// MARK: - Filter Tab Button")

        XCTAssertTrue(content.contains("String(localized: \"inbox.title\")"))
        XCTAssertTrue(content.contains("eventConversationCountText"))
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

    func testMessagesExposeCoordinationOverviewFilters() throws {
        let source = try readProjectFile("iosApp/src/Views/Inbox/InboxView.swift")
        let content = slice(source, from: "struct InboxView", to: "// MARK: - Filter Tab Button")
        let overview = slice(source, from: "private struct InboxCoordinationOverviewCard", to: "// MARK: - Filter Tab Button")

        XCTAssertTrue(content.contains("coordinationOverviewCard"))
        XCTAssertTrue(content.contains("unreadMessageCount"))
        XCTAssertTrue(content.contains("actionRequiredCount"))
        XCTAssertTrue(content.contains("viewModel.items.filter { $0.isFocused || $0.requiresAction }"))
        XCTAssertTrue(content.contains("selectedFilter = .unread"))
        XCTAssertTrue(content.contains("selectedFilter = .focused"))
        XCTAssertTrue(content.contains("selectedFilter = .inbox"))
        XCTAssertTrue(content.contains("WakeveHaptics.selection()"))
        XCTAssertTrue(overview.contains("WakeveContentCard(prominence: .prominent"))
        XCTAssertTrue(overview.contains("InboxCoordinationMetricButton("))
        XCTAssertTrue(overview.contains("inboxCoordinationOverviewCard"))
        XCTAssertTrue(overview.contains("inbox.coordination.title"))
        XCTAssertTrue(overview.contains("inbox.coordination.unread"))
        XCTAssertTrue(overview.contains("inbox.coordination.action_required"))
        XCTAssertTrue(overview.contains("inbox.coordination.events"))
        XCTAssertFalse(overview.contains("LiquidGlassCard("), "Inbox coordination summary is content and should use WakeveContentCard.")

        for locale in ["en", "fr", "es", "it", "pt"] {
            let strings = try readProjectFile("iosApp/src/Resources/\(locale).lproj/Localizable.strings")
            for key in [
                "inbox.coordination.title",
                "inbox.coordination.subtitle",
                "inbox.coordination.unread",
                "inbox.coordination.action_required",
                "inbox.coordination.events"
            ] {
                XCTAssertTrue(strings.contains("\"\(key)\""), "Missing localized inbox coordination key \(key) for \(locale).")
            }
        }
    }

    func testMessagesContentRowsUseSemanticBrandLayer() throws {
        let source = try readProjectFile("iosApp/src/Views/Inbox/InboxView.swift")
        let content = slice(source, from: "struct InboxView", to: "// MARK: - Filter Tab Button")
        let row = slice(source, from: "private struct EventConversationRow", to: "struct InboxRow")
        let eventFilterSheet = slice(source, from: "struct EventFilterSheet", to: "// MARK: - Inbox Row")

        XCTAssertTrue(content.contains("SemanticColor.appBackground"))
        XCTAssertTrue(row.contains("WakeveContentCard("))
        XCTAssertTrue(row.contains("SemanticColor.selectedState"))
        XCTAssertTrue(row.contains("SemanticColor.warning"))
        XCTAssertTrue(eventFilterSheet.contains("NavigationStack"))
        XCTAssertTrue(eventFilterSheet.contains("ScrollView(showsIndicators: false)"))
        XCTAssertTrue(eventFilterSheet.contains("WakeveContentCard("))
        XCTAssertTrue(eventFilterSheet.contains("EventFilterOptionRow"))
        XCTAssertTrue(eventFilterSheet.contains("inbox.filter_all_subtitle"))
        XCTAssertFalse(eventFilterSheet.contains("List {"), "Message event filter should use branded content cards, not native List chrome.")
        XCTAssertFalse(eventFilterSheet.contains("NavigationView"), "Message event filter should use modern NavigationStack.")
        XCTAssertFalse(row.contains("LiquidGlassCard("), "Message conversation rows are content and should not use Liquid Glass.")
    }

    func testMessagesPreserveInboxModelAndViewModelBoundary() throws {
        let source = try readProjectFile("iosApp/src/Views/Inbox/InboxView.swift")
        let viewModel = try readProjectFile("iosApp/src/ViewModels/InboxViewModel.swift")

        XCTAssertTrue(source.contains("@StateObject private var viewModel: InboxViewModel"))
        XCTAssertTrue(source.contains("viewModel.loadNotifications()"))
        XCTAssertTrue(source.contains("viewModel.markAsRead"))
        XCTAssertTrue(source.contains("InboxDetailView(item: latest, conversationItems: conversation.items)"))
        XCTAssertTrue(viewModel.contains("notifications.time.just_now"))
        XCTAssertTrue(viewModel.contains("notifications.time.minutes_ago"))
        XCTAssertFalse(viewModel.contains("À l'instant"), "Inbox timestamps should use localized notification time keys.")
        XCTAssertFalse(
            source.contains("StateMachine("),
            "Messages should remain a SwiftUI presentation over InboxViewModel notifications."
        )
    }

    func testMessageDetailUsesRealConversationAndExternalGroupHandoff() throws {
        let source = try readProjectFile("iosApp/src/Views/Inbox/InboxDetailView.swift")
        let content = slice(source, from: "struct InboxDetailView", to: "// MARK: - Vote Bar")

        XCTAssertTrue(content.contains("var conversationItems: [InboxItemModel] = []"))
        XCTAssertTrue(content.contains("private var timelineItems: [InboxItemModel]"))
        XCTAssertTrue(content.contains("groupHandoffCard"))
        XCTAssertTrue(content.contains("groupHandoffMessage"))
        XCTAssertTrue(content.contains("ShareLink(item: groupHandoffMessage)"))
        XCTAssertTrue(content.contains("copyGroupHandoffMessage()"))
        XCTAssertTrue(content.contains("UIPasteboard.general.string = groupHandoffMessage"))
        XCTAssertTrue(content.contains("WakeveHaptics.success()"))
        XCTAssertTrue(content.contains("inbox.handoff.copy_action"))
        XCTAssertTrue(content.contains("inbox.handoff.copied"))
        XCTAssertTrue(content.contains("inbox.handoff.subtitle"))
        XCTAssertTrue(content.contains("inbox.handoff.message.invitation_format"))
        XCTAssertTrue(content.contains("ForEach(Array(timelineItems.reversed()))"))
        XCTAssertTrue(content.contains("InboxTimelineMessageRow("))
        XCTAssertFalse(content.contains("ForEach(sampleComments)"), "Message detail should render the actual event conversation, not fixed sample comments.")
        XCTAssertFalse(content.contains("Relancer le groupe"))
        XCTAssertFalse(content.contains("Envoyez un message prêt à coller"))
        XCTAssertFalse(content.contains("Poll Trends"))
        XCTAssertFalse(content.contains("You're Invited"))
        XCTAssertFalse(content.contains("Event Progress"))
        XCTAssertFalse(content.contains("Conversations"))
    }

    func testMessageDetailSurfacesDecisionSnapshotFromConversationItems() throws {
        let source = try readProjectFile("iosApp/src/Views/Inbox/InboxDetailView.swift")
        let content = slice(source, from: "struct InboxDetailView", to: "// MARK: - Vote Bar")

        XCTAssertTrue(content.contains("decisionSnapshotCard"))
        XCTAssertTrue(content.contains("inboxDecisionSnapshotCard"))
        XCTAssertTrue(content.contains("private var actionableItems: [InboxItemModel]"))
        XCTAssertTrue(content.contains("timelineItems.filter { !$0.isRead || $0.requiresAction }"))
        XCTAssertTrue(content.contains("decisionSnapshotRows"))
        XCTAssertTrue(content.contains("InboxDecisionSnapshotRowView(row: row)"))
        XCTAssertTrue(content.contains("ForEach(Array(timelineItems.prefix(3)))"))
        XCTAssertTrue(content.contains("signal.type.shortLabel"))
        XCTAssertTrue(content.contains("inbox.decision.next.poll"))
        XCTAssertTrue(content.contains("inbox.decision.next.invitation"))

        for locale in ["en", "fr", "es", "it", "pt"] {
            let strings = try readProjectFile("iosApp/src/Resources/\(locale).lproj/Localizable.strings")
            for key in [
                "inbox.decision.title",
                "inbox.decision.subtitle",
                "inbox.decision.latest_signal",
                "inbox.decision.open_items",
                "inbox.decision.open_items_format",
                "inbox.decision.next_action",
                "inbox.decision.next.invitation",
                "inbox.decision.next.poll",
                "inbox.decision.next.comment",
                "inbox.decision.next.updated",
                "inbox.decision.signal.invite",
                "inbox.decision.signal.poll",
                "inbox.decision.signal.comment",
                "inbox.decision.signal.update"
            ] {
                XCTAssertTrue(strings.contains("\"\(key)\""), "Missing localized inbox decision key \(key) for \(locale).")
            }
        }
    }

    func testMessageDetailDoesNotRenderMockPollOrStatusState() throws {
        let source = try readProjectFile("iosApp/src/Views/Inbox/InboxDetailView.swift")

        XCTAssertTrue(source.contains("private var pollSignalItems: [InboxItemModel]"))
        XCTAssertTrue(source.contains("ForEach(Array(pollSignalItems.prefix(4)))"))
        XCTAssertTrue(source.contains("InboxPollSignalRow(item: signal)"))
        XCTAssertTrue(source.contains("private var inferredCurrentStepKey: EventStepKey"))
        XCTAssertTrue(source.contains("private var eventSteps: [EventStep]"))
        XCTAssertTrue(source.contains("buildEventSteps(current: inferredCurrentStepKey)"))
        XCTAssertTrue(source.contains("String(localized: \"inbox.step.polling\")"))
        XCTAssertTrue(source.contains("String(localized: \"inbox.step.organizing\")"))

        for mockArtifact in [
            "samplePollSlots",
            "SamplePollSlot",
            "Sat, Mar 8 - Morning",
            "Sat, Mar 8 - Afternoon",
            "Sun, Mar 9 - Morning",
            "Sun, Mar 9 - Afternoon",
            "Mock current status",
            "let currentStatus: String = \"polling\"",
            "(\"Draft\", \"draft\")",
            "(\"Polling\", \"polling\")"
        ] {
            XCTAssertFalse(source.contains(mockArtifact), "Inbox detail should not render mock poll or status state: \(mockArtifact).")
        }

        for locale in ["en", "fr", "es", "it", "pt"] {
            let strings = try readProjectFile("iosApp/src/Resources/\(locale).lproj/Localizable.strings")
            for key in [
                "inbox.step.draft",
                "inbox.step.polling",
                "inbox.step.comparing",
                "inbox.step.confirmed",
                "inbox.step.organizing",
                "inbox.step.finalized"
            ] {
                XCTAssertTrue(strings.contains("\"\(key)\""), "Missing localized inbox step key \(key) for \(locale).")
            }
        }
    }

    func testInboxSelectionToolbarUsesLocalizedCopy() throws {
        let source = try readProjectFile("iosApp/src/Views/Inbox/InboxView.swift")
        let toolbar = slice(source, from: ".toolbar {", to: ".toolbar(showActionBar")

        XCTAssertTrue(toolbar.contains("String(localized: \"common.cancel\")"))
        XCTAssertTrue(toolbar.contains("String(localized: \"inbox.selection.select\")"))
        XCTAssertTrue(toolbar.contains("String(localized: \"inbox.selection.select_all\")"))
        XCTAssertFalse(toolbar.contains("Button(\"Cancel\")"))
        XCTAssertFalse(toolbar.contains("Button(\"Select\")"))
        XCTAssertFalse(toolbar.contains("Button(\"Select All\")"))

        for locale in ["en", "fr", "es", "it", "pt"] {
            let strings = try readProjectFile("iosApp/src/Resources/\(locale).lproj/Localizable.strings")
            XCTAssertTrue(strings.contains("\"inbox.selection.select\""), "Missing inbox selection key for \(locale).")
            XCTAssertTrue(strings.contains("\"inbox.selection.select_all\""), "Missing inbox select-all key for \(locale).")
        }
    }

    func testCollaborationCommentsUseLocalizedActionsAndTimestamps() throws {
        let source = try readProjectFile("iosApp/src/Views/Collaboration/CommentItemView.swift")

        for key in [
            "comment.action.reply",
            "comment.action.edit",
            "comment.action.pin",
            "comment.action.unpin",
            "comment.deleted",
            "comment.time.just_now",
            "comment.time.minutes_ago_format",
            "comment.time.hours_ago_format",
            "comment.time.days_ago_format",
            "comment.time.weeks_ago_format",
            "comment.time.unknown",
            "common.delete",
            "common.remove"
        ] {
            XCTAssertTrue(source.contains("\"\(key)\""), "Comment item should use localized key \(key).")
        }

        for hardcodedCopy in [
            "Label(\"Reply\"",
            "Label(\"Edit\"",
            "\"Unpin\" : \"Pin\"",
            "\"Delete\" : \"Remove\"",
            "Text(\"Reply\")",
            "Text(\"[Deleted]\")",
            "return \"Just now\"",
            "return \"Unknown time\"",
            "m ago\"",
            "h ago\"",
            "d ago\"",
            "w ago\""
        ] {
            XCTAssertFalse(source.contains(hardcodedCopy), "Comment item should not hardcode visible copy: \(hardcodedCopy).")
        }

        for locale in ["en", "fr", "es", "it", "pt"] {
            let strings = try readProjectFile("iosApp/src/Resources/\(locale).lproj/Localizable.strings")
            for key in [
                "comment.action.reply",
                "comment.action.edit",
                "comment.action.pin",
                "comment.action.unpin",
                "comment.deleted",
                "comment.time.just_now",
                "comment.time.minutes_ago_format",
                "comment.time.hours_ago_format",
                "comment.time.days_ago_format",
                "comment.time.weeks_ago_format",
                "comment.time.unknown",
                "common.remove"
            ] {
                XCTAssertTrue(strings.contains("\"\(key)\""), "Missing localized comment key \(key) for \(locale).")
            }
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
