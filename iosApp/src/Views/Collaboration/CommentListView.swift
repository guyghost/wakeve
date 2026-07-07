import SwiftUI
import Shared
#if canImport(LiquidGlass)
import LiquidGlass
#endif

/// Sections for organizing comments (Swift enum wrapping CommentSection_ values)
enum CommentSectionType: String, CaseIterable {
    case general
    case scenario
    case poll
    case transport
    case accommodation
    case meal
    case equipment
    case activity
    case budget
    
    /// Map to CommentSection_ if available, nil otherwise
    var sharedValue: CommentSection_? {
        switch self {
        case .general: return .general
        case .transport: return .transport
        case .accommodation: return .accommodation
        case .equipment: return .equipment
        case .activity: return .activity
        default: return nil
        }
    }
}

// CommentThread type is already defined in Shared framework

/// Comment List View for Wakeve
///
/// Liquid Glass design with threaded comment support.
/// Supports @mentions, pinning, and soft delete.
struct CommentListView: View {
    let eventId: String
    let section: CommentSectionType
    let comments: [CommentThread]
    var mentionableUsers: [String] = []
    let currentUserId: String
    let isOrganizer: Bool

    var onNavigateBack: () -> Void = {}
    var onAddComment: (String, [String]) -> Void = { _, _ in }
    var onReply: (String, String) -> Void = { _, _ in }
    var onEdit: (String, String) -> Void = { _, _ in }
    var onDelete: (String) -> Void = { _ in }
    var onPin: (String, Bool) -> Void = { _, _ in }
    var onUserClick: (String) -> Void = { _ in }

    @State private var commentText: String = ""
    @State private var mentionedUsers: [String] = []
    @State private var showMentionAutocomplete: Bool = false
    @State private var moderationTarget: ModerationActionTarget?

    var body: some View {
        NavigationView {
            ZStack {
                VStack(spacing: 0) {
                    // Header
                    header

                    // Comment List
                    if comments.isEmpty {
                        emptyCommentsSection
                    } else {
                        ScrollView {
                            LazyVStack(spacing: 12) {
                                ForEach(comments, id: \.comment.id) { thread in
                                    CommentThreadView(
                                        thread: thread,
                                        currentUserId: currentUserId,
                                        isOrganizer: isOrganizer,
                                        onReply: onReply,
                                        onEdit: onEdit,
                                        onDelete: onDelete,
                                        onPin: onPin,
                                        onUserClick: onUserClick,
                                        onReportComment: { comment in
                                            moderationTarget = ModerationActionTarget(
                                                type: .comment,
                                                targetId: comment.id,
                                                eventId: eventId,
                                                authorId: comment.authorId,
                                                displayName: String(localized: "moderation.report_comment_context"),
                                                allowsBlock: comment.authorId != currentUserId
                                            )
                                        },
                                        onReportUser: { userId, userName in
                                            moderationTarget = ModerationActionTarget(
                                                type: .user,
                                                targetId: userId,
                                                eventId: eventId,
                                                authorId: userId,
                                                displayName: userName,
                                                allowsBlock: userId != currentUserId
                                            )
                                        },
                                        onBlockUser: { userId, userName in
                                            moderationTarget = ModerationActionTarget(
                                                type: .user,
                                                targetId: userId,
                                                eventId: eventId,
                                                authorId: userId,
                                                displayName: userName,
                                                allowsBlock: true
                                            )
                                        }
                                    )
                                }
                            }
                            .padding(.horizontal, 16)
                            .padding(.vertical, 12)
                        }
                    }

                    // Comment Input
                    CommentInputView(
                        text: $commentText,
                        mentionedUsers: $mentionedUsers,
                        showMentionAutocomplete: $showMentionAutocomplete,
                        hasMentionableUsers: !mentionableUsers.isEmpty,
                        onSend: {
                            if !commentText.isEmpty {
                                onAddComment(commentText, mentionedUsers)
                                commentText = ""
                                mentionedUsers = []
                            }
                        }
                    )
                    .background(WakeveColors.surface)
                }

                // Mention Autocomplete Overlay
                if showMentionAutocomplete && !mentionableUsers.isEmpty {
                    MentionAutocompleteView(
                        users: mentionableUsers,
                        onUserSelected: { username in
                            insertMention(username)
                            showMentionAutocomplete = false
                        }
                    )
                    .padding(.bottom, 100)
                }
            }
            .navigationTitle(getSectionTitle(section))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button(action: onNavigateBack) {
                        Image(systemName: "chevron.left")
                    }
                    .accessibilityLabel(String(localized: "common.back"))
                }
            }
            .sheet(item: $moderationTarget) { target in
                ModerationActionSheet(target: target)
            }
        }
    }

    private var header: some View {
        HStack {
            Text(getSectionTitle(section))
                .font(.title3)
                .fontWeight(.bold)
                .foregroundColor(WakeveColors.onSurface)

            Spacer()

            // Filter/Sort options (optional)
            Button(action: {}) {
                Image(systemName: "line.3.horizontal.decrease.circle")
                    .foregroundColor(WakeveColors.primary)
            }
            .accessibilityLabel(String(localized: "home.filter_events_accessibility"))
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
        .background(WakeveColors.background)
    }

    private var emptyCommentsSection: some View {
        VStack(spacing: 16) {
            Image(systemName: "bubble.left.and.bubble.right")
                .font(.system(size: 60))
                .foregroundColor(WakeveColors.onSurfaceVariant)

            Text(String(localized: "comments.empty.title"))
                .font(.title2)
                .foregroundColor(WakeveColors.onSurfaceVariant)

            Text(String(localized: "comments.empty.subtitle"))
                .font(.body)
                .foregroundColor(WakeveColors.onSurfaceVariant)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(WakeveColors.background)
    }

    private func insertMention(_ username: String) {
        let mentionText = "@\(username) "
        commentText += mentionText
    }

    private func getSectionTitle(_ section: CommentSectionType) -> String {
        switch section {
        case .general: return String(localized: "comments.section.general")
        case .scenario: return String(localized: "comments.section.scenario")
        case .poll: return String(localized: "comments.section.poll")
        case .transport: return String(localized: "comments.section.transport")
        case .accommodation: return String(localized: "comments.section.accommodation")
        case .meal: return String(localized: "comments.section.meal")
        case .equipment: return String(localized: "comments.section.equipment")
        case .activity: return String(localized: "comments.section.activity")
        case .budget: return String(localized: "comments.section.budget")
        }
    }
}

/// Threaded comment view with replies
struct CommentThreadView: View {
    let thread: CommentThread
    let currentUserId: String
    let isOrganizer: Bool

    var onReply: (String, String) -> Void = { _, _ in }
    var onEdit: (String, String) -> Void = { _, _ in }
    var onDelete: (String) -> Void = { _ in }
    var onPin: (String, Bool) -> Void = { _, _ in }
    var onUserClick: (String) -> Void = { _ in }
    var onReportComment: (Comment_) -> Void = { _ in }
    var onReportUser: (String, String) -> Void = { _, _ in }
    var onBlockUser: (String, String) -> Void = { _, _ in }

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            // Parent comment
            CommentItemView(
                comment: thread.comment,
                isPinned: thread.comment.isPinned,
                currentUserId: currentUserId,
                isOrganizer: isOrganizer,
                isParent: true,
                onReply: onReply,
                onEdit: onEdit,
                onDelete: onDelete,
                onPin: onPin,
                onUserClick: onUserClick,
                onReportComment: onReportComment,
                onReportUser: onReportUser,
                onBlockUser: onBlockUser
            )

            // Replies (indented)
            if !thread.replies.isEmpty {
                VStack(alignment: .leading, spacing: 8) {
                    ForEach(thread.replies, id: \.id) { reply in
                        CommentItemView(
                            comment: reply,
                            isPinned: false,
                            currentUserId: currentUserId,
                            isOrganizer: isOrganizer,
                            isParent: false,
                            onReply: onReply,
                            onEdit: onEdit,
                            onDelete: onDelete,
                            onPin: onPin,
                            onUserClick: onUserClick,
                            onReportComment: onReportComment,
                            onReportUser: onReportUser,
                            onBlockUser: onBlockUser
                        )
                    }
                }
                .padding(.leading, 56)
            }

            // Load more replies indicator
            if thread.hasMoreReplies {
                Button(action: {}) {
                    Text(String.localizedStringWithFormat(
                        String(localized: "comments.load_more_replies"),
                        thread.comment.replyCount
                    ))
                        .font(.body)
                        .foregroundColor(WakeveColors.primary)
                }
                .padding(.leading, 56)
                .padding(.top, 4)
            }
        }
    }
}

/// Comment Input View
struct CommentInputView: View {
    @Binding var text: String
    @Binding var mentionedUsers: [String]
    @Binding var showMentionAutocomplete: Bool
    let hasMentionableUsers: Bool
    var onSend: () -> Void

    var body: some View {
        VStack(spacing: 0) {
            Divider()
            
            HStack(spacing: 12) {
                // Text field
                TextField(String(localized: "comments.add_placeholder"), text: $text, axis: .vertical)
                    .lineLimit(1...5)
                    .padding(.horizontal, 12)
                    .padding(.vertical, 8)
                    .background(WakeveColors.surface.opacity(0.5))
                    .cornerRadius(20)
                
                // Mention button
                Button(action: { showMentionAutocomplete.toggle() }) {
                    Image(systemName: "at")
                        .foregroundColor(showMentionAutocomplete ? WakeveColors.primary : WakeveColors.onSurfaceVariant)
                }
                .disabled(!hasMentionableUsers)
                .accessibilityLabel(String(localized: "comments.mention"))
                
                // Send button
                Button(action: onSend) {
                    Image(systemName: "arrow.up.circle.fill")
                        .font(.title2)
                        .foregroundColor(text.isEmpty ? WakeveColors.onSurfaceVariant : WakeveColors.primary)
                }
                .disabled(text.isEmpty)
                .accessibilityLabel(String(localized: "comments.send"))
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 12)
            .background(WakeveColors.surface)
        }
    }
}

/// Mention Autocomplete View
struct MentionAutocompleteView: View {
    let users: [String]
    var onUserSelected: (String) -> Void
    
    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            Text(String(localized: "comments.mention"))
                .font(.caption)
                .foregroundColor(WakeveColors.onSurfaceVariant)
                .padding(.horizontal, 16)
                .padding(.vertical, 8)
            
            ForEach(users, id: \.self) { user in
                Button(action: { onUserSelected(user) }) {
                    HStack {
                        Image(systemName: "person.circle")
                            .foregroundColor(WakeveColors.primary)
                        Text("@\(user)")
                            .foregroundColor(WakeveColors.onSurface)
                        Spacer()
                    }
                    .padding(.horizontal, 16)
                    .padding(.vertical, 12)
                }
                .background(WakeveColors.surface)
            }
        }
        .background(WakeveColors.surface)
        .cornerRadius(12)
        .shadow(radius: 4)
        .padding(.horizontal, 16)
    }
}

// MARK: - Previews

#if DEBUG
#Preview("Comments - Threaded Light") {
    CommentListView(
        eventId: EventFactory.polling.id,
        section: .general,
        comments: CommentFactory.threads,
        currentUserId: UserFactory.organizer.id,
        isOrganizer: true
    )
    .preferredColorScheme(.light)
}

#Preview("Comments - Threaded Dark") {
    CommentListView(
        eventId: EventFactory.polling.id,
        section: .general,
        comments: CommentFactory.threads,
        currentUserId: UserFactory.organizer.id,
        isOrganizer: true
    )
    .preferredColorScheme(.dark)
}

#Preview("Comments - Empty") {
    CommentListView(
        eventId: EventFactory.polling.id,
        section: .poll,
        comments: [],
        currentUserId: UserFactory.participant.id,
        isOrganizer: false
    )
}
#endif
