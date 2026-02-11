import SwiftUI
import LiquidGlass

/// Comment List View for Wakeve
///
/// Liquid Glass design with threaded comment support.
/// Supports @mentions, pinning, and soft delete.
struct CommentListView: View {
    let eventId: String
    let section: CommentSection
    let comments: [CommentThread]
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
                                        onUserClick: onUserClick
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
                        onSend: {
                            if !commentText.isEmpty {
                                onAddComment(commentText, mentionedUsers)
                                commentText = ""
                                mentionedUsers = []
                            }
                        }
                    )
                    .background(WakevColors.surface)
                }

                // Mention Autocomplete Overlay
                if showMentionAutocomplete {
                    MentionAutocompleteView(
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
                }
            }
        }
    }

    private var header: some View {
        HStack {
            Text(getSectionTitle(section))
                .font(.title3)
                .fontWeight(.bold)
                .foregroundColor(WakevColors.onSurface)

            Spacer()

            // Filter/Sort options (optional)
            Button(action: {}) {
                Image(systemName: "line.3.horizontal.decrease.circle")
                    .foregroundColor(WakevColors.primary)
            }
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
        .background(WakevColors.background)
    }

    private var emptyCommentsSection: some View {
        VStack(spacing: 16) {
            Image(systemName: "bubble.left.and.bubble.right")
                .font(.system(size: 60))
                .foregroundColor(WakevColors.onSurfaceVariant)

            Text("No comments yet")
                .font(.title2)
                .foregroundColor(WakevColors.onSurfaceVariant)

            Text("Be the first to share your thoughts!")
                .font(.body)
                .foregroundColor(WakevColors.onSurfaceVariant)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(WakevColors.background)
    }

    private func insertMention(_ username: String) {
        let mentionText = "@\(username) "
        commentText += mentionText
    }

    private func getSectionTitle(_ section: CommentSection) -> String {
        switch section {
        case .general: return "Comments"
        case .scenario: return "Scenario Comments"
        case .poll: return "Poll Comments"
        case .transport: return "Transport Comments"
        case .accommodation: return "Accommodation Comments"
        case .meal: return "Meal Comments"
        case .equipment: return "Equipment Comments"
        case .activity: return "Activity Comments"
        case .budget: return "Budget Comments"
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
                onUserClick: onUserClick
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
                            onUserClick: onUserClick
                        )
                    }
                }
                .padding(.leading, 56)
            }

            // Load more replies indicator
            if thread.hasMoreReplies {
                Button(action: {}) {
                    Text("Load more replies (\(thread.comment.replyCount))")
                        .font(.body)
                        .foregroundColor(WakevColors.primary)
                }
                .padding(.leading, 56)
                .padding(.top, 4)
            }
        }
    }
}
