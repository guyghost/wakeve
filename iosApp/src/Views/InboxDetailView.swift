import SwiftUI
import Shared

/// Detail view for inbox notifications.
/// Displays notification header, type-specific content, event status stepper, and conversations.
struct InboxDetailView: View {
    let item: InboxItemModel

    @Environment(\.dismiss) private var dismiss

    var body: some View {
        ScrollView {
            VStack(spacing: 24) {
                // A. Notification Header
                notificationHeader

                // B. Type-specific content
                typeSpecificContent

                // C. Event Status Stepper
                eventStatusStepper

                // D. Conversations
                conversationsSection

                Spacer().frame(height: 40)
            }
            .padding(.horizontal, 20)
            .padding(.top, 16)
        }
        .background(Color(.systemGroupedBackground).ignoresSafeArea())
        .navigationTitle(item.eventName ?? item.title)
        .navigationBarTitleDisplayMode(.inline)
        .navigationBarBackButtonHidden(true)
        .toolbar {
            ToolbarItem(placement: .navigationBarLeading) {
                Button {
                    dismiss()
                } label: {
                    HStack(spacing: 4) {
                        Image(systemName: "chevron.left")
                            .font(.system(size: 16, weight: .semibold))
                        Text("Back")
                            .font(.system(size: 17))
                    }
                    .foregroundColor(.wakevePrimary)
                }
            }
        }
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
                        .font(.system(size: 17, weight: .semibold))
                        .foregroundColor(.primary)

                    Text(item.message)
                        .font(.system(size: 15))
                        .foregroundColor(.secondary)

                    Text(item.timeAgo)
                        .font(.system(size: 13))
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
                    .font(.system(size: 16, weight: .semibold))
                    .foregroundColor(.wakevePrimary)

                Text("Poll Trends")
                    .font(.system(size: 20, weight: .semibold))
                    .foregroundColor(.primary)
            }

            ForEach(samplePollSlots, id: \.label) { slot in
                VStack(alignment: .leading, spacing: 8) {
                    Text(slot.label)
                        .font(.system(size: 15, weight: .medium))
                        .foregroundColor(.primary)

                    // Vote bars
                    HStack(spacing: 8) {
                        VoteBar(label: "Yes", count: slot.yes, total: slot.total, color: .green)
                        VoteBar(label: "Maybe", count: slot.maybe, total: slot.total, color: .orange)
                        VoteBar(label: "No", count: slot.no, total: slot.total, color: .red)
                    }
                }
            }
        }
        .padding(16)
        .glassCard()
    }

    // MARK: Invitation Card

    private var invitationCard: some View {
        VStack(spacing: 16) {
            HStack {
                Image(systemName: "envelope.open.fill")
                    .font(.system(size: 16, weight: .semibold))
                    .foregroundColor(.wakevePrimary)

                Text("You're Invited")
                    .font(.system(size: 20, weight: .semibold))
                    .foregroundColor(.primary)

                Spacer()
            }

            VStack(alignment: .leading, spacing: 8) {
                HStack(spacing: 8) {
                    Image(systemName: "calendar")
                        .foregroundColor(.secondary)
                    Text(item.eventName ?? "Event")
                        .font(.system(size: 15))
                        .foregroundColor(.primary)
                }

                HStack(spacing: 8) {
                    Image(systemName: "person.2.fill")
                        .foregroundColor(.secondary)
                    Text("6 participants invited")
                        .font(.system(size: 15))
                        .foregroundColor(.secondary)
                }
            }

            // RSVP Buttons
            HStack(spacing: 12) {
                RSVPActionButton(title: "Accept", color: .wakeveSuccess, icon: "checkmark") {}
                RSVPActionButton(title: "Maybe", color: .wakeveWarning, icon: "questionmark") {}
                RSVPActionButton(title: "Decline", color: .wakeveError, icon: "xmark") {}
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
                    .font(.system(size: 16, weight: .semibold))
                    .foregroundColor(.wakeveSuccess)

                Text("New Comment")
                    .font(.system(size: 20, weight: .semibold))
                    .foregroundColor(.primary)
            }

            // Comment bubble
            VStack(alignment: .leading, spacing: 8) {
                HStack(spacing: 8) {
                    Text("AM")
                        .font(.system(size: 13, weight: .bold))
                        .foregroundColor(.white)
                        .frame(width: 32, height: 32)
                        .background(Circle().fill(Color.wakevePrimary))

                    VStack(alignment: .leading, spacing: 2) {
                        Text("Alice Martin")
                            .font(.system(size: 15, weight: .semibold))
                            .foregroundColor(.primary)
                        Text("2h ago")
                            .font(.system(size: 12))
                            .foregroundColor(.secondary)
                    }
                }

                Text(item.message)
                    .font(.system(size: 15))
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
                    .font(.system(size: 16, weight: .semibold))
                    .foregroundColor(.wakeveWarning)

                Text("Event Update")
                    .font(.system(size: 20, weight: .semibold))
                    .foregroundColor(.primary)
            }

            VStack(alignment: .leading, spacing: 8) {
                UpdateRow(icon: "arrow.triangle.2.circlepath", text: "Status changed to Polling")
                UpdateRow(icon: "person.badge.plus", text: "2 new participants joined")
                UpdateRow(icon: "calendar.badge.exclamationmark", text: "Deadline extended to next Friday")
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
                    .font(.system(size: 16, weight: .semibold))
                    .foregroundColor(.wakevePrimary)

                Text("Event Progress")
                    .font(.system(size: 20, weight: .semibold))
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
                                          Color(.systemGray4))
                                    .frame(width: 24, height: 24)

                                if step.state == .completed {
                                    Image(systemName: "checkmark")
                                        .font(.system(size: 12, weight: .bold))
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
                                    .fill(step.state == .completed ? Color.wakevePrimary : Color(.systemGray4))
                                    .frame(width: 2, height: 32)
                            }
                        }

                        // Step label
                        VStack(alignment: .leading, spacing: 2) {
                            Text(step.title)
                                .font(.system(size: 15, weight: step.state == .current ? .semibold : .regular))
                                .foregroundColor(step.state == .upcoming ? .secondary : .primary)

                            if step.state == .current {
                                Text("Current step")
                                    .font(.system(size: 12))
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

    // MARK: - D. Conversations

    private var conversationsSection: some View {
        VStack(alignment: .leading, spacing: 16) {
            HStack {
                Image(systemName: "bubble.left.and.bubble.right.fill")
                    .font(.system(size: 16, weight: .semibold))
                    .foregroundColor(.wakevePrimary)

                Text("Conversations")
                    .font(.system(size: 20, weight: .semibold))
                    .foregroundColor(.primary)

                Spacer()

                Text("\(sampleComments.count)")
                    .font(.system(size: 13, weight: .medium))
                    .foregroundColor(.secondary)
                    .padding(.horizontal, 8)
                    .padding(.vertical, 2)
                    .background(Color(.systemGray5))
                    .continuousCornerRadius(10)
            }

            ForEach(sampleComments) { comment in
                InboxCommentRow(comment: comment)
            }
        }
        .padding(16)
        .glassCard()
    }
}

// MARK: - Vote Bar

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
                    .font(.system(size: 12, weight: .semibold))
                    .foregroundColor(color)
                Text(label)
                    .font(.system(size: 10))
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
                    .font(.system(size: 16, weight: .semibold))
                Text(title)
                    .font(.system(size: 12, weight: .medium))
            }
            .foregroundColor(color)
            .frame(maxWidth: .infinity)
            .padding(.vertical, 12)
            .background(color.opacity(0.1))
            .continuousCornerRadius(12)
        }
    }
}

// MARK: - Update Row

private struct UpdateRow: View {
    let icon: String
    let text: String

    var body: some View {
        HStack(spacing: 10) {
            Image(systemName: icon)
                .font(.system(size: 14))
                .foregroundColor(.secondary)
                .frame(width: 20)

            Text(text)
                .font(.system(size: 15))
                .foregroundColor(.primary)
        }
    }
}

// MARK: - Inbox Comment Row (simplified, without importing CommentItemView)

private struct InboxCommentRow: View {
    let comment: SampleComment

    var body: some View {
        HStack(alignment: .top, spacing: 10) {
            // Avatar
            Text(comment.initials)
                .font(.system(size: 12, weight: .bold))
                .foregroundColor(.white)
                .frame(width: 32, height: 32)
                .background(Circle().fill(comment.avatarColor))

            VStack(alignment: .leading, spacing: 4) {
                HStack {
                    Text(comment.authorName)
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundColor(.primary)

                    Spacer()

                    Text(comment.timeAgo)
                        .font(.system(size: 12))
                        .foregroundColor(.secondary)
                }

                Text(comment.content)
                    .font(.system(size: 14))
                    .foregroundColor(.primary)
                    .fixedSize(horizontal: false, vertical: true)
            }
        }
        .padding(.vertical, 4)
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

/// Compute event steps based on a mock status (polling for sample).
/// Uses EventStatus enum values: draft, polling, comparing, confirmed, organizing, finalized
private let eventSteps: [EventStep] = {
    // Mock current status — change to test different states
    let currentStatus: String = "polling"

    let allSteps = [
        ("Draft", "draft"),
        ("Polling", "polling"),
        ("Comparing", "comparing"),
        ("Confirmed", "confirmed"),
        ("Organizing", "organizing"),
        ("Finalized", "finalized")
    ]

    var passedCurrent = false
    return allSteps.map { (title, key) in
        if passedCurrent {
            return EventStep(title: title, state: .upcoming)
        } else if key == currentStatus {
            passedCurrent = true
            return EventStep(title: title, state: .current)
        } else {
            return EventStep(title: title, state: .completed)
        }
    }
}()

// MARK: - Sample Poll Data

private struct SamplePollSlot {
    let label: String
    let yes: Int
    let maybe: Int
    let no: Int
    var total: Int { yes + maybe + no }
}

private let samplePollSlots: [SamplePollSlot] = [
    SamplePollSlot(label: "Sat, Mar 8 - Morning", yes: 4, maybe: 1, no: 1),
    SamplePollSlot(label: "Sat, Mar 8 - Afternoon", yes: 3, maybe: 2, no: 1),
    SamplePollSlot(label: "Sun, Mar 9 - Morning", yes: 2, maybe: 3, no: 1),
    SamplePollSlot(label: "Sun, Mar 9 - Afternoon", yes: 1, maybe: 1, no: 4)
]

// MARK: - Sample Comments Data

struct SampleComment: Identifiable {
    let id: String
    let authorName: String
    let initials: String
    let content: String
    let timeAgo: String
    let avatarColor: Color
}

private let sampleComments: [SampleComment] = [
    SampleComment(
        id: "c1",
        authorName: "Alice Martin",
        initials: "AM",
        content: "I'd prefer the Saturday morning slot. Anyone else?",
        timeAgo: "2h",
        avatarColor: .wakevePrimary
    ),
    SampleComment(
        id: "c2",
        authorName: "Bob Chen",
        initials: "BC",
        content: "Saturday works for me too! Let's finalize the location.",
        timeAgo: "1h",
        avatarColor: .wakeveAccent
    ),
    SampleComment(
        id: "c3",
        authorName: "Clara Dupont",
        initials: "CD",
        content: "Can we also discuss transport arrangements?",
        timeAgo: "45m",
        avatarColor: .wakeveSuccess
    )
]

// MARK: - Preview

#Preview("InboxDetailView - Poll Update") {
    NavigationStack {
        InboxDetailView(
            item: InboxItemModel(
                id: "preview",
                title: "Poll results updated",
                message: "3 new votes on Week-end ski 2024",
                timeAgo: "17m",
                type: .pollUpdate,
                isRead: false,
                repository: "wakeve",
                number: 42,
                commentCount: 3,
                status: .open,
                isFocused: true,
                eventName: "Week-end ski 2024",
                eventId: "evt-preview"
            )
        )
    }
}

#Preview("InboxDetailView - Invitation") {
    NavigationStack {
        InboxDetailView(
            item: InboxItemModel(
                id: "preview-inv",
                title: "You're invited to Réunion famille",
                message: "Join the family reunion planning",
                timeAgo: "3h",
                type: .invitation,
                isRead: false,
                repository: "wakeve",
                number: 10,
                commentCount: 0,
                status: .open,
                isFocused: false,
                eventName: "Réunion famille",
                eventId: "evt-preview-2"
            )
        )
    }
}
