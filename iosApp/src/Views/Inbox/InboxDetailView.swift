import SwiftUI
import Shared
#if canImport(UIKit)
import UIKit
#endif

/// Detail view for inbox notifications.
/// Displays notification header, type-specific content, event status stepper, and conversations.
struct InboxDetailView: View {
    let item: InboxItemModel
    var conversationItems: [InboxItemModel] = []

    @Environment(\.dismiss) private var dismiss
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @Environment(\.colorScheme) private var colorScheme
    @State private var moderationTarget: ModerationActionTarget?
    @State private var showCopiedHandoffMessage = false

    var body: some View {
        ScrollView {
            VStack(spacing: 24) {
                // A. Notification Header
                notificationHeader

                // B. Type-specific content
                typeSpecificContent

                // C. Event Status Stepper
                eventStatusStepper

                // D. Decision snapshot
                decisionSnapshotCard

                // E. External group handoff
                groupHandoffCard

                // F. Timeline
                conversationsSection

                Spacer().frame(height: 40)
            }
            .padding(.horizontal, 20)
            .padding(.top, 16)
        }
        .background(WakeveScreenBackground(style: .grouped))
        .navigationTitle(item.eventName ?? item.title)
        .navigationBarTitleDisplayMode(.inline)
        .navigationBarBackButtonHidden(true)
        .sheet(item: $moderationTarget) { target in
            ModerationActionSheet(target: target)
        }
        .toolbar {
            ToolbarItem(placement: .navigationBarLeading) {
                Button {
                    dismiss()
                } label: {
                    HStack(spacing: 4) {
                        Image(systemName: "chevron.left")
                            .font(WakeveTheme.Typography.bodySemibold)
                        Text(String(localized: "common.back"))
                            .font(WakeveTheme.Typography.body)
                    }
                    .foregroundColor(.wakevePrimary)
                }
            }
        }
    }

    private var timelineItems: [InboxItemModel] {
        conversationItems.isEmpty ? [item] : conversationItems
    }

    private var eventDisplayName: String {
        item.eventName ?? item.title
    }

    private var groupHandoffMessage: String {
        let invitationCode = item.eventId.map { InvitationTokenCodec.invitationCode(forEventId: $0) }
        let codeLine = invitationCode.map { String(format: String(localized: "inbox.handoff.code_line_format"), $0) } ?? ""

        switch item.type {
        case .invitation:
            return handoffMessage("inbox.handoff.message.invitation_format", codeLine: codeLine)
        case .pollUpdate:
            return handoffMessage("inbox.handoff.message.poll_format", codeLine: codeLine)
        case .comment:
            return handoffMessage("inbox.handoff.message.comment_format", codeLine: codeLine)
        case .eventUpdate:
            return handoffMessage("inbox.handoff.message.event_update_format", codeLine: codeLine)
        }
    }

    private func handoffMessage(_ key: String.LocalizationValue, codeLine: String) -> String {
        String(format: String(localized: key), eventDisplayName, item.message, codeLine)
    }

    private var actionableItems: [InboxItemModel] {
        timelineItems.filter { !$0.isRead || $0.requiresAction }
    }

    private var pollSignalItems: [InboxItemModel] {
        let pollItems = timelineItems.filter { $0.type == .pollUpdate }
        if pollItems.isEmpty, item.type == .pollUpdate {
            return [item]
        }
        return pollItems
    }

    private var inferredCurrentStepKey: EventStepKey {
        let source = timelineItems.first ?? item
        let searchableText = "\(source.title) \(source.message)".localizedLowercase

        if searchableText.contains("final") || searchableText.contains("ready") {
            return .finalized
        }
        if searchableText.contains("organizing") || searchableText.contains("meeting") || searchableText.contains("transport") || searchableText.contains("budget") {
            return .organizing
        }
        if searchableText.contains("confirm") || searchableText.contains("date selected") {
            return .confirmed
        }
        if searchableText.contains("scenario") || searchableText.contains("compare") || searchableText.contains("option") {
            return .comparing
        }
        if searchableText.contains("poll") || searchableText.contains("vote") {
            return .polling
        }

        switch source.type {
        case .invitation:
            return .draft
        case .pollUpdate:
            return .polling
        case .comment:
            return .confirmed
        case .eventUpdate:
            return .organizing
        }
    }

    private var eventSteps: [EventStep] {
        buildEventSteps(current: inferredCurrentStepKey)
    }

    private var decisionSnapshotRows: [InboxDecisionSnapshotRow] {
        [
            InboxDecisionSnapshotRow(
                id: "latest",
                icon: "sparkle.magnifyingglass",
                title: String(localized: "inbox.decision.latest_signal"),
                value: timelineItems.first?.title ?? item.title
            ),
            InboxDecisionSnapshotRow(
                id: "open",
                icon: "checklist",
                title: String(localized: "inbox.decision.open_items"),
                value: String(
                    format: String(localized: "inbox.decision.open_items_format"),
                    actionableItems.count
                )
            ),
            InboxDecisionSnapshotRow(
                id: "next",
                icon: "arrow.turn.down.right",
                title: String(localized: "inbox.decision.next_action"),
                value: decisionNextAction
            )
        ]
    }

    private var decisionNextAction: String {
        if actionableItems.contains(where: { $0.type == .invitation }) {
            return String(localized: "inbox.decision.next.invitation")
        }

        if actionableItems.contains(where: { $0.type == .pollUpdate }) {
            return String(localized: "inbox.decision.next.poll")
        }

        if timelineItems.contains(where: { $0.type == .comment }) {
            return String(localized: "inbox.decision.next.comment")
        }

        return String(localized: "inbox.decision.next.updated")
    }

    // MARK: - A. Notification Header

    private var notificationHeader: some View {
        VStack(spacing: 12) {
            HStack(alignment: .top, spacing: 12) {
                // Type icon
                ZStack {
                    Circle()
                        .fill(item.iconColor.opacity(0.15))
                        .frame(width: 48, height: 48)

                    Image(systemName: item.icon)
                        .font(.system(size: 20, weight: .medium))
                        .foregroundColor(item.iconColor)
                }

                VStack(alignment: .leading, spacing: 4) {
                    Text(item.title)
                        .font(WakeveTheme.Typography.bodySemibold)
                        .foregroundColor(.primary)

                    Text(item.message)
                        .font(WakeveTheme.Typography.body)
                        .foregroundColor(.secondary)

                    Text(item.timeAgo)
                        .font(WakeveTheme.Typography.caption)
                        .foregroundColor(.secondary)
                }

                Spacer()
            }
        }
        .padding(16)
        .glassCard()
    }

    // MARK: - B. Type-Specific Content

    @ViewBuilder
    private var typeSpecificContent: some View {
        switch item.type {
        case .pollUpdate:
            pollTrendSection
        case .invitation:
            invitationCard
        case .comment:
            commentHighlight
        case .eventUpdate:
            eventUpdateSummary
        }
    }

    // MARK: Poll Trend Section

    private var pollTrendSection: some View {
        VStack(alignment: .leading, spacing: 16) {
            HStack {
                Image(systemName: "chart.bar.fill")
                    .font(WakeveTheme.Typography.bodySemibold)
                    .foregroundColor(.wakevePrimary)

                Text(String(localized: "inbox.detail.poll_trends"))
                    .font(WakeveTheme.Typography.section)
                    .foregroundColor(.primary)
            }

            ForEach(Array(pollSignalItems.prefix(4))) { signal in
                InboxPollSignalRow(item: signal)
            }
        }
        .padding(16)
        .glassCard()
        .accessibilityIdentifier("inboxPollSignalSection")
    }

    // MARK: Invitation Card

    private var invitationCard: some View {
        VStack(spacing: 16) {
            HStack {
                Image(systemName: "envelope.open.fill")
                    .font(WakeveTheme.Typography.bodySemibold)
                    .foregroundColor(.wakevePrimary)

                Text(String(localized: "inbox.detail.invited_title"))
                    .font(WakeveTheme.Typography.section)
                    .foregroundColor(.primary)

                Spacer()
            }

            VStack(alignment: .leading, spacing: 8) {
                HStack(spacing: 8) {
                    Image(systemName: "calendar")
                        .foregroundColor(.secondary)
                    Text(item.eventName ?? String(localized: "inbox.detail.event_fallback"))
                        .font(WakeveTheme.Typography.body)
                        .foregroundColor(.primary)
                }

                HStack(spacing: 8) {
                    Image(systemName: "person.2.fill")
                        .foregroundColor(.secondary)
                    Text(String(format: String(localized: "inbox.detail.participants_invited_format"), 6))
                        .font(WakeveTheme.Typography.body)
                        .foregroundColor(.secondary)
                }
            }

            // RSVP Buttons
            HStack(spacing: 12) {
                RSVPActionButton(title: String(localized: "events.rsvp.going"), color: .wakeveSuccess, icon: "checkmark") {}
                RSVPActionButton(title: String(localized: "events.rsvp.maybe"), color: .wakeveWarning, icon: "questionmark") {}
                RSVPActionButton(title: String(localized: "events.rsvp.not_going"), color: .wakeveError, icon: "xmark") {}
            }
        }
        .padding(16)
        .glassCard()
    }

    // MARK: Comment Highlight

    private var commentHighlight: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Image(systemName: "bubble.left.fill")
                    .font(WakeveTheme.Typography.bodySemibold)
                    .foregroundColor(.wakeveSuccess)

                Text(String(localized: "inbox.detail.new_comment"))
                    .font(WakeveTheme.Typography.section)
                    .foregroundColor(.primary)
            }

            // Comment bubble
            VStack(alignment: .leading, spacing: 8) {
                HStack(spacing: 8) {
                    Text("GR")
                        .font(WakeveTheme.Typography.caption.weight(.bold))
                        .foregroundColor(.white)
                        .frame(width: 32, height: 32)
                        .background(Circle().fill(Color.wakevePrimary))

                    VStack(alignment: .leading, spacing: 2) {
                        Text(String(localized: "inbox.author.group"))
                            .font(WakeveTheme.Typography.bodySemibold)
                            .foregroundColor(.primary)
                        Text(item.timeAgo)
                            .font(WakeveTheme.Typography.tiny)
                            .foregroundColor(.secondary)
                    }

                    Spacer()

                    chatMessageModerationMenu(
                        messageId: item.id,
                        authorId: "alice-martin",
                        authorName: "Alice Martin"
                    )
                }

                Text(item.message)
                    .font(WakeveTheme.Typography.body)
                    .foregroundColor(.primary)
                    .padding(12)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(Color(.tertiarySystemFill))
                    .continuousCornerRadius(12)
            }
        }
        .padding(16)
        .glassCard()
    }

    // MARK: Event Update Summary

    private var eventUpdateSummary: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Image(systemName: "calendar.badge.clock")
                    .font(WakeveTheme.Typography.bodySemibold)
                    .foregroundColor(.wakeveWarning)

                Text(String(localized: "inbox.detail.event_update"))
                    .font(WakeveTheme.Typography.section)
                    .foregroundColor(.primary)
            }

            VStack(alignment: .leading, spacing: 8) {
                UpdateRow(icon: "arrow.triangle.2.circlepath", text: String(localized: "inbox.detail.update.status_polling"))
                UpdateRow(icon: "person.badge.plus", text: String(localized: "inbox.detail.update.new_participants"))
                UpdateRow(icon: "calendar.badge.exclamationmark", text: String(localized: "inbox.detail.update.deadline_extended"))
            }
        }
        .padding(16)
        .glassCard()
    }

    // MARK: - C. Event Status Stepper

    private var eventStatusStepper: some View {
        VStack(alignment: .leading, spacing: 16) {
            HStack {
                Image(systemName: "list.bullet")
                    .font(WakeveTheme.Typography.bodySemibold)
                    .foregroundColor(.wakevePrimary)

                Text(String(localized: "inbox.detail.event_progress"))
                    .font(WakeveTheme.Typography.section)
                    .foregroundColor(.primary)
            }

            VStack(alignment: .leading, spacing: 0) {
                ForEach(Array(eventSteps.enumerated()), id: \.offset) { index, step in
                    HStack(alignment: .top, spacing: 16) {
                        // Step indicator
                        VStack(spacing: 0) {
                            ZStack {
                                Circle()
                                    .fill(step.state == .completed ? Color.wakevePrimary :
                                          step.state == .current ? Color.wakevePrimary :
                                          WakeveTheme.ColorToken.controlFill(for: colorScheme))
                                    .frame(width: 24, height: 24)

                                if step.state == .completed {
                                    Image(systemName: "checkmark")
                                        .font(WakeveTheme.Typography.tiny.weight(.bold))
                                        .foregroundColor(.white)
                                } else if step.state == .current {
                                    Circle()
                                        .fill(Color.white)
                                        .frame(width: 8, height: 8)
                                }
                            }

                            // Connecting line
                            if index < eventSteps.count - 1 {
                                Rectangle()
                                    .fill(step.state == .completed ? Color.wakevePrimary : WakeveTheme.ColorToken.separator(for: colorScheme))
                                    .frame(width: 2, height: 32)
                            }
                        }

                        // Step label
                        VStack(alignment: .leading, spacing: 2) {
                            Text(step.title)
                                .font(.system(size: 15, weight: step.state == .current ? .semibold : .regular))
                                .foregroundColor(step.state == .upcoming ? .secondary : .primary)

                            if step.state == .current {
                                Text(String(localized: "inbox.current_step"))
                                    .font(WakeveTheme.Typography.tiny)
                                    .foregroundColor(.wakevePrimary)
                            }
                        }
                        .padding(.top, 1)

                        Spacer()
                    }
                }
            }
        }
        .padding(16)
        .glassCard()
    }

    // MARK: - D. Decision Snapshot

    private var decisionSnapshotCard: some View {
        WakeveContentCard(prominence: .regular, cornerRadius: WakeveTheme.Radius.xl, padding: WakeveTheme.Spacing.md) {
            VStack(alignment: .leading, spacing: 14) {
                HStack(alignment: .top, spacing: 12) {
                    Image(systemName: "checkmark.seal.fill")
                        .font(WakeveTheme.Typography.bodySemibold)
                        .foregroundColor(.wakevePrimary)
                        .frame(width: 36, height: 36)
                        .background(Color.wakevePrimary.opacity(0.12))
                        .clipShape(Circle())

                    VStack(alignment: .leading, spacing: 4) {
                        Text(String(localized: "inbox.decision.title"))
                            .font(WakeveTheme.Typography.bodySemibold)
                            .foregroundColor(.primary)

                        Text(String(localized: "inbox.decision.subtitle"))
                            .font(WakeveTheme.Typography.callout)
                            .foregroundColor(.secondary)
                            .fixedSize(horizontal: false, vertical: true)
                    }
                }

                VStack(spacing: 10) {
                    ForEach(decisionSnapshotRows) { row in
                        InboxDecisionSnapshotRowView(row: row)
                    }
                }

                HStack(spacing: 8) {
                    ForEach(Array(timelineItems.prefix(3))) { signal in
                        Label(signal.type.shortLabel, systemImage: signal.icon)
                            .font(WakeveTheme.Typography.tiny)
                            .foregroundColor(signal.iconColor)
                            .lineLimit(1)
                            .minimumScaleFactor(0.74)
                            .padding(.horizontal, 10)
                            .padding(.vertical, 6)
                            .background(signal.iconColor.opacity(0.12))
                            .clipShape(Capsule())
                    }
                }
                .accessibilityIdentifier("inboxDecisionSnapshotSignals")
            }
        }
        .accessibilityIdentifier("inboxDecisionSnapshotCard")
    }

    // MARK: - E. External Group Handoff

    private var groupHandoffCard: some View {
        WakeveContentCard(prominence: .prominent, cornerRadius: WakeveTheme.Radius.xl, padding: WakeveTheme.Spacing.md) {
            VStack(alignment: .leading, spacing: 14) {
                HStack(alignment: .top, spacing: 12) {
                    Image(systemName: "paperplane.fill")
                        .font(WakeveTheme.Typography.bodySemibold)
                        .foregroundColor(.wakevePrimary)
                        .frame(width: 36, height: 36)
                        .background(Color.wakevePrimary.opacity(0.12))
                        .clipShape(Circle())

                    VStack(alignment: .leading, spacing: 4) {
                        Text(String(localized: "inbox.handoff.title"))
                            .font(WakeveTheme.Typography.bodySemibold)
                            .foregroundColor(.primary)

                        Text(String(localized: "inbox.handoff.subtitle"))
                            .font(WakeveTheme.Typography.callout)
                            .foregroundColor(.secondary)
                            .fixedSize(horizontal: false, vertical: true)
                    }
                }

                Text(groupHandoffMessage)
                    .font(WakeveTheme.Typography.callout)
                    .foregroundColor(.primary)
                    .padding(12)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(Color(.secondarySystemGroupedBackground))
                    .continuousCornerRadius(12)
                    .accessibilityIdentifier("groupHandoffMessagePreview")

                HStack(spacing: 10) {
                    ShareLink(item: groupHandoffMessage) {
                        Label(String(localized: "inbox.handoff.share_action"), systemImage: "square.and.arrow.up")
                            .font(WakeveTheme.Typography.bodySemibold)
                            .foregroundColor(.white)
                            .lineLimit(1)
                            .minimumScaleFactor(0.76)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 12)
                            .background(Color.wakevePrimary)
                            .continuousCornerRadius(14)
                    }
                    .frame(minHeight: 44)
                    .simultaneousGesture(TapGesture().onEnded {
                        WakeveHaptics.selection()
                    })
                    .accessibilityIdentifier("groupHandoffShareLink")

                    Button {
                        copyGroupHandoffMessage()
                    } label: {
                        Label(String(localized: "inbox.handoff.copy_action"), systemImage: "doc.on.doc.fill")
                            .font(WakeveTheme.Typography.bodySemibold)
                            .foregroundColor(.wakevePrimary)
                            .lineLimit(1)
                            .minimumScaleFactor(0.76)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 12)
                            .background(Color.wakevePrimary.opacity(0.12))
                            .continuousCornerRadius(14)
                    }
                    .buttonStyle(.plain)
                    .frame(minHeight: 44)
                    .accessibilityIdentifier("groupHandoffCopyButton")
                }

                if showCopiedHandoffMessage {
                    Label(String(localized: "inbox.handoff.copied"), systemImage: "checkmark.circle.fill")
                        .font(WakeveTheme.Typography.caption)
                        .foregroundColor(.wakeveSuccess)
                        .transition(.opacity.combined(with: .move(edge: .top)))
                }
            }
        }
    }

    private func copyGroupHandoffMessage() {
        #if canImport(UIKit)
        UIPasteboard.general.string = groupHandoffMessage
        #endif
        WakeveHaptics.success()
        withAnimation(reduceMotion ? nil : .easeInOut(duration: 0.18)) {
            showCopiedHandoffMessage = true
        }
    }

    // MARK: - F. Timeline

    private var conversationsSection: some View {
        VStack(alignment: .leading, spacing: 16) {
            HStack {
                Image(systemName: "bubble.left.and.bubble.right.fill")
                    .font(WakeveTheme.Typography.bodySemibold)
                    .foregroundColor(.wakevePrimary)

                Text(String(localized: "inbox.detail.conversations"))
                    .font(WakeveTheme.Typography.section)
                    .foregroundColor(.primary)

                Spacer()

                Text("\(timelineItems.count)")
                    .font(WakeveTheme.Typography.caption)
                    .foregroundColor(.secondary)
                    .padding(.horizontal, 8)
                    .padding(.vertical, 2)
                    .background(WakeveTheme.ColorToken.controlFill(for: colorScheme))
                    .continuousCornerRadius(10)
            }

            ForEach(Array(timelineItems.reversed())) { conversationItem in
                InboxTimelineMessageRow(
                    item: conversationItem,
                    onModerationTarget: { moderationTarget = $0 }
                )
            }
        }
        .padding(16)
        .glassCard()
    }

    private func chatMessageModerationMenu(messageId: String, authorId: String, authorName: String) -> some View {
        Menu {
            Button {
                moderationTarget = ModerationActionTarget(
                    type: .chatMessage,
                    targetId: messageId,
                    eventId: item.eventId,
                    authorId: authorId,
                    displayName: String(localized: "moderation.report_chat_context"),
                    allowsBlock: true
                )
            } label: {
                Label(String(localized: "moderation.report_content"), systemImage: "exclamationmark.bubble")
            }
            .accessibilityIdentifier("reportChatMessageAction")

            Button {
                moderationTarget = ModerationActionTarget(
                    type: .user,
                    targetId: authorId,
                    eventId: item.eventId,
                    authorId: authorId,
                    displayName: authorName,
                    allowsBlock: true
                )
            } label: {
                Label(String(localized: "moderation.report_user"), systemImage: "person.crop.circle.badge.exclamationmark")
            }
            .accessibilityIdentifier("reportChatAuthorAction")

            Button(role: .destructive) {
                moderationTarget = ModerationActionTarget(
                    type: .user,
                    targetId: authorId,
                    eventId: item.eventId,
                    authorId: authorId,
                    displayName: authorName,
                    allowsBlock: true
                )
            } label: {
                Label(String(localized: "moderation.block_user"), systemImage: "person.crop.circle.badge.xmark")
            }
            .accessibilityIdentifier("blockChatAuthorAction")
        } label: {
            Image(systemName: "ellipsis.circle")
                .foregroundColor(.secondary)
                .frame(minWidth: 44, minHeight: 44)
        }
    }
}

// MARK: - Vote Bar

private struct InboxDecisionSnapshotRow: Identifiable {
    let id: String
    let icon: String
    let title: String
    let value: String
}

private struct InboxDecisionSnapshotRowView: View {
    let row: InboxDecisionSnapshotRow

    var body: some View {
        HStack(spacing: 10) {
            Image(systemName: row.icon)
                .font(WakeveTheme.Typography.caption)
                .foregroundColor(.wakevePrimary)
                .frame(width: 30, height: 30)
                .background(Color.wakevePrimary.opacity(0.12))
                .clipShape(Circle())

            VStack(alignment: .leading, spacing: 2) {
                Text(row.title)
                    .font(WakeveTheme.Typography.tiny)
                    .foregroundColor(.secondary)
                    .textCase(.uppercase)

                Text(row.value)
                    .font(WakeveTheme.Typography.bodySemibold)
                    .foregroundColor(.primary)
                    .lineLimit(2)
                    .minimumScaleFactor(0.78)
            }

            Spacer(minLength: 4)
        }
        .padding(10)
        .background(Color(.secondarySystemGroupedBackground))
        .continuousCornerRadius(12)
    }
}

private extension InboxItemType {
    var shortLabel: String {
        switch self {
        case .invitation:
            return String(localized: "inbox.decision.signal.invite")
        case .pollUpdate:
            return String(localized: "inbox.decision.signal.poll")
        case .comment:
            return String(localized: "inbox.decision.signal.comment")
        case .eventUpdate:
            return String(localized: "inbox.decision.signal.update")
        }
    }
}

private struct VoteBar: View {
    let label: String
    let count: Int
    let total: Int
    let color: Color

    private var fraction: CGFloat {
        total > 0 ? CGFloat(count) / CGFloat(total) : 0
    }

    var body: some View {
        VStack(spacing: 4) {
            GeometryReader { geo in
                ZStack(alignment: .leading) {
                    RoundedRectangle(cornerRadius: 4)
                        .fill(color.opacity(0.15))
                        .frame(height: 8)

                    RoundedRectangle(cornerRadius: 4)
                        .fill(color)
                        .frame(width: geo.size.width * fraction, height: 8)
                }
            }
            .frame(height: 8)

            HStack(spacing: 2) {
                Text("\(count)")
                    .font(WakeveTheme.Typography.tiny)
                    .foregroundColor(color)
                Text(label)
                    .font(WakeveTheme.Typography.tiny)
                    .foregroundColor(.secondary)
            }
        }
    }
}

// MARK: - RSVP Action Button

private struct RSVPActionButton: View {
    let title: String
    let color: Color
    let icon: String
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            VStack(spacing: 6) {
                Image(systemName: icon)
                    .font(WakeveTheme.Typography.bodySemibold)
                Text(title)
                    .font(WakeveTheme.Typography.tiny)
            }
            .foregroundColor(color)
            .frame(maxWidth: .infinity)
            .padding(.vertical, 12)
            .background(color.opacity(0.1))
            .continuousCornerRadius(12)
        }
        .frame(minHeight: 44)
    }
}

// MARK: - Update Row

private struct UpdateRow: View {
    let icon: String
    let text: String

    var body: some View {
        HStack(spacing: 10) {
            Image(systemName: icon)
                .font(WakeveTheme.Typography.callout)
                .foregroundColor(.secondary)
                .frame(width: 20)

            Text(text)
                .font(WakeveTheme.Typography.body)
                .foregroundColor(.primary)
        }
    }
}

// MARK: - Inbox Timeline Message Row

private struct InboxTimelineMessageRow: View {
    let item: InboxItemModel
    let onModerationTarget: (ModerationActionTarget) -> Void

    var body: some View {
        HStack(alignment: .top, spacing: 10) {
            Text(authorInitials)
                .font(WakeveTheme.Typography.tiny.weight(.bold))
                .foregroundColor(.white)
                .frame(width: 32, height: 32)
                .background(Circle().fill(item.iconColor))

            VStack(alignment: .leading, spacing: 4) {
                HStack {
                    Text(authorName)
                        .font(WakeveTheme.Typography.caption)
                        .foregroundColor(.primary)

                    Spacer()

                    Text(item.timeAgo)
                        .font(WakeveTheme.Typography.tiny)
                        .foregroundColor(.secondary)

                    Menu {
                        Button {
                            onModerationTarget(
                                ModerationActionTarget(
                                    type: .chatMessage,
                                    targetId: item.id,
                                    eventId: item.eventId,
                                    authorId: "wakeve-system",
                                    displayName: String(localized: "moderation.report_chat_context"),
                                    allowsBlock: true
                                )
                            )
                        } label: {
                            Label(String(localized: "moderation.report_content"), systemImage: "exclamationmark.bubble")
                        }
                        .accessibilityIdentifier("reportConversationMessageAction")

                        Button(role: .destructive) {
                            onModerationTarget(
                                ModerationActionTarget(
                                    type: .user,
                                    targetId: "wakeve-system",
                                    eventId: item.eventId,
                                    authorId: "wakeve-system",
                                    displayName: authorName,
                                    allowsBlock: true
                                )
                            )
                        } label: {
                            Label(String(localized: "moderation.block_user"), systemImage: "person.crop.circle.badge.xmark")
                        }
                        .accessibilityIdentifier("blockConversationAuthorAction")
                    } label: {
                        Image(systemName: "ellipsis.circle")
                            .foregroundColor(.secondary)
                            .frame(minWidth: 44, minHeight: 44)
                    }
                }

                Text(item.title)
                    .font(WakeveTheme.Typography.caption)
                    .foregroundColor(.primary)
                    .fixedSize(horizontal: false, vertical: true)

                Text(item.message)
                    .font(WakeveTheme.Typography.callout)
                    .foregroundColor(.secondary)
                    .fixedSize(horizontal: false, vertical: true)
            }
        }
        .padding(.vertical, 4)
    }

    private var authorName: String {
        switch item.type {
        case .comment:
            return String(localized: "inbox.author.group")
        case .invitation:
            return String(localized: "inbox.author.invitation")
        case .pollUpdate:
            return String(localized: "inbox.author.poll")
        case .eventUpdate:
            return String(localized: "inbox.author.event_update")
        }
    }

    private var authorInitials: String {
        switch item.type {
        case .comment:
            return "GR"
        case .invitation:
            return "IN"
        case .pollUpdate:
            return "SO"
        case .eventUpdate:
            return "EV"
        }
    }
}

// MARK: - Event Step Model

private enum StepState {
    case completed, current, upcoming
}

private struct EventStep {
    let title: String
    let state: StepState
}

private enum EventStepKey: CaseIterable {
    case draft, polling, comparing, confirmed, organizing, finalized

    var title: String {
        switch self {
        case .draft:
            return String(localized: "inbox.step.draft")
        case .polling:
            return String(localized: "inbox.step.polling")
        case .comparing:
            return String(localized: "inbox.step.comparing")
        case .confirmed:
            return String(localized: "inbox.step.confirmed")
        case .organizing:
            return String(localized: "inbox.step.organizing")
        case .finalized:
            return String(localized: "inbox.step.finalized")
        }
    }
}

private func buildEventSteps(current currentKey: EventStepKey) -> [EventStep] {
    var passedCurrent = false
    return EventStepKey.allCases.map { key in
        if passedCurrent {
            return EventStep(title: key.title, state: .upcoming)
        } else if key == currentKey {
            passedCurrent = true
            return EventStep(title: key.title, state: .current)
        } else {
            return EventStep(title: key.title, state: .completed)
        }
    }
}

// MARK: - Poll Signal Row

private struct InboxPollSignalRow: View {
    let item: InboxItemModel

    var body: some View {
        HStack(alignment: .top, spacing: 12) {
            Image(systemName: item.icon)
                .font(WakeveTheme.Typography.caption)
                .foregroundColor(item.iconColor)
                .frame(width: 30, height: 30)
                .background(item.iconColor.opacity(0.12))
                .clipShape(Circle())

            VStack(alignment: .leading, spacing: 4) {
                HStack(alignment: .firstTextBaseline) {
                    Text(item.title)
                        .font(WakeveTheme.Typography.bodySemibold)
                        .foregroundColor(.primary)
                        .lineLimit(2)
                        .minimumScaleFactor(0.82)

                    Spacer(minLength: 8)

                    Text(item.timeAgo)
                        .font(WakeveTheme.Typography.tiny)
                        .foregroundColor(.secondary)
                }

                Text(item.message)
                    .font(WakeveTheme.Typography.callout)
                    .foregroundColor(.secondary)
                    .fixedSize(horizontal: false, vertical: true)
            }
        }
        .padding(12)
        .background(Color(.secondarySystemGroupedBackground))
        .continuousCornerRadius(12)
    }
}

// MARK: - Preview

#Preview("Inbox Detail - Poll Update Light") {
    NavigationStack {
        InboxDetailView(
            item: InboxItemModel(
                id: "preview",
                title: "Poll results updated",
                message: "3 new votes on Week-end ski 2024",
                timeAgo: "17m",
                type: .pollUpdate,
                isRead: false,
                commentCount: 3,
                isFocused: true,
                eventName: "Week-end ski 2024",
                eventId: "evt-preview"
            )
        )
    }
    .preferredColorScheme(.light)
}

#Preview("Inbox Detail - Poll Update Dark") {
    NavigationStack {
        InboxDetailView(
            item: InboxItemModel(
                id: "preview",
                title: "Poll results updated",
                message: "3 new votes on Week-end ski 2024",
                timeAgo: "17m",
                type: .pollUpdate,
                isRead: false,
                commentCount: 3,
                isFocused: true,
                eventName: "Week-end ski 2024",
                eventId: "evt-preview"
            )
        )
    }
    .preferredColorScheme(.dark)
}

#Preview("Inbox Detail - Invitation Light") {
    NavigationStack {
        InboxDetailView(
            item: InboxItemModel(
                id: "preview-inv",
                title: "You're invited to Réunion famille",
                message: "Join the family reunion planning",
                timeAgo: "3h",
                type: .invitation,
                isRead: false,
                commentCount: 0,
                isFocused: false,
                eventName: "Réunion famille",
                eventId: "evt-preview-2"
            )
        )
    }
    .preferredColorScheme(.light)
}

#Preview("Inbox Detail - Invitation Dark") {
    NavigationStack {
        InboxDetailView(
            item: InboxItemModel(
                id: "preview-inv",
                title: "You're invited to Réunion famille",
                message: "Join the family reunion planning",
                timeAgo: "3h",
                type: .invitation,
                isRead: false,
                commentCount: 0,
                isFocused: false,
                eventName: "Réunion famille",
                eventId: "evt-preview-2"
            )
        )
    }
    .preferredColorScheme(.dark)
}
