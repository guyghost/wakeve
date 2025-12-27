import SwiftUI

// TODO: Replace with actual types from Shared module
enum CommentSection: String, CaseIterable {
    case GENERAL, SCENARIO, POLL, TRANSPORT, ACCOMMODATION, MEAL, EQUIPMENT, ACTIVITY, BUDGET
}

struct Comment {
    let id: String
    let eventId: String
    let section: CommentSection
    let sectionItemId: String?
    let authorId: String
    let authorName: String
    let content: String
    let parentCommentId: String?
    let createdAt: String
    let updatedAt: String?
    let isEdited: Bool
    let replyCount: Int
}

struct CommentThread {
    let comment: Comment
    let replies: [Comment]
    let hasMoreReplies: Bool
}

struct CommentQueryFilters {
    let section: CommentSection?
    let sectionItemId: String?
    let authorId: String?
    let parentCommentId: String?
    let limit: Int
    let offset: Int

    init(
        section: CommentSection? = nil,
        sectionItemId: String? = nil,
        authorId: String? = nil,
        parentCommentId: String? = nil,
        limit: Int = 50,
        offset: Int = 0
    ) {
        self.section = section
        self.sectionItemId = sectionItemId
        self.authorId = authorId
        self.parentCommentId = parentCommentId
        self.limit = limit
        self.offset = offset
    }
}

struct CommentRequest {
    let section: CommentSection
    let sectionItemId: String?
    let content: String
    let parentCommentId: String?

    init(
        section: CommentSection,
        sectionItemId: String? = nil,
        content: String,
        parentCommentId: String? = nil
    ) {
        self.section = section
        self.sectionItemId = sectionItemId
        self.content = content
        self.parentCommentId = parentCommentId
    }
}

class CommentRepository {
    // TODO: Replace with actual repository implementation
    func getCommentsByEvent(eventId: String) -> [Comment] {
        // Mock data for preview/testing
        return [
            Comment(
                id: "comment-1",
                eventId: eventId,
                section: .GENERAL,
                sectionItemId: nil,
                authorId: "user-1",
                authorName: "Alice",
                content: "I'm excited for this event! Can't wait to see everyone.",
                parentCommentId: nil,
                createdAt: "2025-12-25T10:00:00Z",
                updatedAt: nil,
                isEdited: false,
                replyCount: 1
            ),
            Comment(
                id: "comment-2",
                eventId: eventId,
                section: .GENERAL,
                sectionItemId: nil,
                authorId: "user-2",
                authorName: "Bob",
                content: "Me too! What time should we meet?",
                parentCommentId: "comment-1",
                createdAt: "2025-12-25T10:15:00Z",
                updatedAt: nil,
                isEdited: false,
                replyCount: 0
            )
        ]
    }

    func createComment(eventId: String, authorId: String, authorName: String, request: CommentRequest) -> Comment {
        // Mock implementation
        return Comment(
            id: UUID().uuidString,
            eventId: eventId,
            section: request.section,
            sectionItemId: request.sectionItemId,
            authorId: authorId,
            authorName: authorName,
            content: request.content,
            parentCommentId: request.parentCommentId,
            createdAt: ISO8601DateFormatter().string(from: Date()),
            updatedAt: nil,
            isEdited: false,
            replyCount: 0
        )
    }
}

/// Comments View - iOS
///
/// Displays event comments with threading support.
/// Uses Liquid Glass design system with Material backgrounds.
struct CommentsView: View {
    let eventId: String
    let section: CommentSection?
    let sectionItemId: String?
    let currentUserId: String
    let currentUserName: String

    // TODO: Inject repository via dependency injection
    // For now, using placeholder - in real implementation, get from DI container
    private var repository: CommentRepository {
        // TODO: Get from shared container
        fatalError("CommentRepository must be injected")
    }

    @State private var commentThreads: [CommentThread] = []
    @State private var isLoading = false
    @State private var errorMessage: String?
    @State private var selectedSection: CommentSection?
    @State private var showFilterSheet = false
    @State private var showAddCommentSheet = false
    @State private var replyToComment: Comment?
    @State private var newCommentText = ""

    var body: some View {
        ZStack {
            Color.gray.opacity(0.05)
                .ignoresSafeArea()

            VStack(spacing: 0) {
                // Header
                headerView

                // Filter Bar
                if section == nil {
                    filterBar
                }

                if isLoading {
                    loadingView
                } else if commentThreads.isEmpty {
                    emptyStateView
                } else {
                    // Comments List
                    commentsList
                }
            }

            // FAB - Add Comment Button
            VStack {
                Spacer()
                HStack {
                    Spacer()
                    Button(action: {
                        replyToComment = nil
                        newCommentText = ""
                        showAddCommentSheet = true
                    }) {
                        Image(systemName: "plus.bubble.fill")
                            .font(.system(size: 20, weight: .semibold))
                            .foregroundColor(.white)
                            .frame(width: 56, height: 56)
                            .background(Color.blue)
                            .clipShape(Circle())
                            .shadow(color: Color.black.opacity(0.2), radius: 8, x: 0, y: 4)
                    }
                    .padding(.trailing, 20)
                    .padding(.bottom, 20)
                }
            }
        }
        .onAppear {
            selectedSection = section
            loadComments()
        }
        .sheet(isPresented: $showAddCommentSheet) {
            addCommentSheet
        }
        .sheet(isPresented: $showFilterSheet) {
            filterSheet
        }
        .alert("Error", isPresented: .constant(errorMessage != nil)) {
            Button("Retry", role: .none) {
                loadComments()
                errorMessage = nil
            }
            Button("OK", role: .cancel) {
                errorMessage = nil
            }
        } message: {
            Text(errorMessage ?? "Unknown error")
        }
        .refreshable {
            await loadCommentsAsync()
        }
    }

    // MARK: - Header View

    private var headerView: some View {
        HStack {
            Button(action: {
                // TODO: Implement navigation back
                // onBack()
            }) {
                Image(systemName: "arrow.left")
                    .font(.system(size: 16, weight: .semibold))
                    .foregroundColor(.secondary)
                    .frame(width: 36, height: 36)
                    .background(Color(.tertiarySystemFill))
                    .clipShape(Circle())
            }

            VStack(alignment: .leading, spacing: 2) {
                Text(titleText)
                    .font(.system(size: 18, weight: .semibold))
                    .foregroundColor(.primary)

                if let section = section {
                    Text(sectionDisplayName(section))
                        .font(.system(size: 14))
                        .foregroundColor(.secondary)
                }
            }

            Spacer()

            if section == nil {
                Button(action: { showFilterSheet = true }) {
                    Image(systemName: "line.3.horizontal.decrease.circle")
                        .font(.system(size: 20))
                        .foregroundColor(.secondary)
                        .frame(width: 36, height: 36)
                }
            }
        }
        .padding(.horizontal, 20)
        .padding(.vertical, 16)
        .background(            Color.gray.opacity(0.05))
    }

    // MARK: - Filter Bar

    private var filterBar: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                // All Sections
                CommentFilterChip(
                    title: "All",
                    isSelected: selectedSection == nil,
                    action: {
                        selectedSection = nil
                        loadComments()
                    }
                )

                // Section Chips
                ForEach(CommentSection.allCases, id: \.self) { section in
                    CommentFilterChip(
                        title: sectionShortName(section),
                        icon: sectionIcon(section),
                        isSelected: selectedSection == section,
                        action: {
                            selectedSection = section
                            loadComments()
                        }
                    )
                }
            }
            .padding(.horizontal, 20)
            .padding(.vertical, 8)
        }
    }

    // MARK: - Comments List

    private var commentsList: some View {
        ScrollView {
            LazyVStack(spacing: 16) {
                ForEach(commentThreads, id: \.comment.id) { thread in
                    CommentThreadView(
                        thread: thread,
                        currentUserId: currentUserId,
                        onReply: { comment in
                            replyToComment = comment
                            newCommentText = ""
                            showAddCommentSheet = true
                        },
                        onEdit: { comment in
                            // TODO: Implement edit
                        },
                        onDelete: { comment in
                            Task { await deleteComment(comment) }
                        },
                        onLoadMoreReplies: { comment in
                            Task { await loadMoreReplies(for: comment) }
                        }
                    )
                }

                // Bottom padding for FAB
                Spacer()
                    .frame(height: 80)
            }
            .padding(.horizontal, 20)
            .padding(.top, 8)
        }
    }

    // MARK: - Loading View

    private var loadingView: some View {
        VStack(spacing: 20) {
            ProgressView()
                .progressViewStyle(CircularProgressViewStyle())

            Text("Loading comments...")
                .font(.system(size: 17))
                .foregroundColor(.secondary)
        }
        .frame(maxHeight: .infinity)
    }

    // MARK: - Empty State

    private var emptyStateView: some View {
        VStack(spacing: 24) {
            Image(systemName: "bubble.left.and.bubble.right")
                .font(.system(size: 50))
                .foregroundColor(.secondary)

            VStack(spacing: 8) {
                Text("No Comments Yet")
                    .font(.system(size: 20, weight: .semibold))
                    .foregroundColor(.primary)

                Text("Be the first to start the discussion")
                    .font(.system(size: 15))
                    .foregroundColor(.secondary)
                    .multilineTextAlignment(.center)
            }
        }
        .frame(maxHeight: .infinity)
        .padding(.top, 60)
    }

    // MARK: - Add Comment Sheet

    private var addCommentSheet: some View {
        NavigationView {
            VStack(spacing: 0) {
                // Reply indicator if replying
                if let replyTo = replyToComment {
                    HStack(spacing: 12) {
                        Rectangle()
                            .fill(Color.blue)
                            .frame(width: 3)
                            .clipShape(RoundedRectangle(cornerRadius: 1.5, style: .continuous))

                        VStack(alignment: .leading, spacing: 4) {
                            Text("Replying to \(replyTo.authorName)")
                                .font(.system(size: 14, weight: .semibold))
                                .foregroundColor(.primary)

                            Text(replyTo.content)
                                .font(.system(size: 14))
                                .foregroundColor(.secondary)
                                .lineLimit(2)
                        }

                        Spacer()
                    }
                    .padding(16)
        .background(Color(.tertiarySystemFill))
                    .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                    .padding(.horizontal, 20)
                    .padding(.top, 16)
                }

                // Text input
                VStack(spacing: 16) {
                    ZStack(alignment: .topLeading) {
                        TextEditor(text: $newCommentText)
                            .font(.system(size: 16))
                            .frame(minHeight: 120, maxHeight: 200)
                            .padding(12)
                            .background(Color(.tertiarySystemFill))
                            .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                            .overlay(
                                RoundedRectangle(cornerRadius: 12, style: .continuous)
                                    .stroke(Color.gray, lineWidth: 1)
                            )

                        if newCommentText.isEmpty {
                            Text(replyToComment != nil ? "Write a reply..." : "Write a comment...")
                                .font(.system(size: 16))
                                .foregroundColor(.secondary)
                                .padding(16)
                                .allowsHitTesting(false)
                        }
                    }

                    // Character count
                    HStack {
                        Spacer()
                        Text("\(newCommentText.count)/2000")
                            .font(.system(size: 12))
                            .foregroundColor(newCommentText.count > 1800 ? .orange : .secondary)
                    }
                }
                .padding(.horizontal, 20)
                .padding(.vertical, 16)

                Spacer()
            }
            .navigationTitle(replyToComment != nil ? "Reply" : "New Comment")
            // iOS only: .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") {
                        showAddCommentSheet = false
                        replyToComment = nil
                        newCommentText = ""
                    }
                }

                ToolbarItem(placement: .confirmationAction) {
                    Button(replyToComment != nil ? "Reply" : "Post") {
                        Task { await postComment() }
                    }
                    .disabled(newCommentText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                }
            }
        }
    }

    // MARK: - Filter Sheet

    private var filterSheet: some View {
        NavigationView {
            List {
                Section {
                    Button(action: {
                        selectedSection = nil
                        showFilterSheet = false
                        loadComments()
                    }) {
                        HStack {
                            Text("All Sections")
                            Spacer()
                            if selectedSection == nil {
                                Image(systemName: "checkmark")
                                    .foregroundColor(.blue)
                            }
                        }
                    }
                }

                Section("Sections") {
                    ForEach(CommentSection.allCases, id: \.self) { section in
                        Button(action: {
                            selectedSection = section
                            showFilterSheet = false
                            loadComments()
                        }) {
                            HStack {
                                Image(systemName: sectionIcon(section))
                                    .foregroundColor(sectionColor(section))
                                    .frame(width: 24, height: 24)

                                Text(sectionDisplayName(section))
                                Spacer()

                                if selectedSection == section {
                                    Image(systemName: "checkmark")
                                        .foregroundColor(.blue)
                                }
                            }
                        }
                    }
                }
            }
            .navigationTitle("Filter Comments")
            // iOS only: .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("Done") {
                        showFilterSheet = false
                    }
                }
            }
        }
    }

    // MARK: - Helper Properties

    private var titleText: String {
        if let section = section {
            return "Comments - \(sectionShortName(section))"
        }
        return "Comments"
    }

    // MARK: - Helper Functions

    private func loadComments() {
        Task {
            await loadCommentsAsync()
        }
    }

    private func loadCommentsAsync() async {
        isLoading = true
        errorMessage = nil

        do {
            let filters = CommentQueryFilters(
                section: selectedSection,
                sectionItemId: sectionItemId,
                parentCommentId: nil, // Only top-level comments
                limit: 50,
                offset: 0
            )

            // TODO: Implement getCommentThreads in repository
            // For now, simulate with individual calls
            let comments = repository.getCommentsByEvent(eventId: eventId)
            let filteredComments = comments.filter { comment in
                if let section = selectedSection {
                    return comment.section == section
                }
                return comment.parentCommentId == nil // Only top-level
            }

            // Build threads
            var threads: [CommentThread] = []
            for comment in filteredComments {
                if comment.parentCommentId == nil { // Top-level comment
                    let replies = comments.filter { $0.parentCommentId == comment.id }
                    let thread = CommentThread(
                        comment: comment,
                        replies: replies,
                        hasMoreReplies: false // TODO: Implement pagination
                    )
                    threads.append(thread)
                }
            }

            // Sort by creation date (newest first)
            commentThreads = threads.sorted { $0.comment.createdAt > $1.comment.createdAt }

            isLoading = false
        } catch {
            errorMessage = error.localizedDescription
            isLoading = false
        }
    }

    private func postComment() async {
        guard !newCommentText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else { return }

        do {
            let request = CommentRequest(
                section: selectedSection ?? section ?? .GENERAL,
                sectionItemId: sectionItemId,
                content: newCommentText.trimmingCharacters(in: .whitespacesAndNewlines),
                parentCommentId: replyToComment?.id
            )

            let _ = repository.createComment(
                eventId: eventId,
                authorId: currentUserId,
                authorName: currentUserName,
                request: request
            )

            showAddCommentSheet = false
            replyToComment = nil
            newCommentText = ""

            // Reload comments
            await loadCommentsAsync()

        } catch {
            errorMessage = "Failed to post comment: \(error.localizedDescription)"
        }
    }

    private func deleteComment(_ comment: Comment) async {
        do {
            // TODO: Implement deleteComment in repository
            // repository.deleteComment(commentId: comment.id)

            // Reload comments
            await loadCommentsAsync()
        } catch {
            errorMessage = "Failed to delete comment: \(error.localizedDescription)"
        }
    }

    private func loadMoreReplies(for comment: Comment) async {
        // TODO: Implement pagination for replies
    }

    private func sectionDisplayName(_ section: CommentSection) -> String {
        switch section {
        case .GENERAL: return "General Discussion"
        case .SCENARIO: return "Scenario Comparison"
        case .POLL: return "Date Polling"
        case .TRANSPORT: return "Transportation"
        case .ACCOMMODATION: return "Accommodation"
        case .MEAL: return "Meal Planning"
        case .EQUIPMENT: return "Equipment"
        case .ACTIVITY: return "Activities"
        case .BUDGET: return "Budget"
        }
    }

    private func sectionShortName(_ section: CommentSection) -> String {
        switch section {
        case .GENERAL: return "General"
        case .SCENARIO: return "Scenarios"
        case .POLL: return "Poll"
        case .TRANSPORT: return "Transport"
        case .ACCOMMODATION: return "Stay"
        case .MEAL: return "Meals"
        case .EQUIPMENT: return "Equipment"
        case .ACTIVITY: return "Activities"
        case .BUDGET: return "Budget"
        }
    }

    private func sectionIcon(_ section: CommentSection) -> String {
        switch section {
        case .GENERAL: return "bubble.left.and.bubble.right"
        case .SCENARIO: return "list.bullet"
        case .POLL: return "calendar.badge.clock"
        case .TRANSPORT: return "car.fill"
        case .ACCOMMODATION: return "house.fill"
        case .MEAL: return "fork.knife"
        case .EQUIPMENT: return "bag.fill"
        case .ACTIVITY: return "figure.walk"
        case .BUDGET: return "dollarsign.circle.fill"
        }
    }

    private func sectionColor(_ section: CommentSection) -> Color {
        switch section {
        case .GENERAL: return .blue
        case .SCENARIO: return .purple
        case .POLL: return .green
        case .TRANSPORT: return .orange
        case .ACCOMMODATION: return .red
        case .MEAL: return .yellow
        case .EQUIPMENT: return .pink
        case .ACTIVITY: return .cyan
        case .BUDGET: return .mint
        }
    }
}

// MARK: - Comment Thread View

private struct CommentThreadView: View {
    let thread: CommentThread
    let currentUserId: String
    let onReply: (Comment) -> Void
    let onEdit: (Comment) -> Void
    let onDelete: (Comment) -> Void
    let onLoadMoreReplies: (Comment) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            // Main comment
            CommentCell(
                comment: thread.comment,
                isCurrentUser: thread.comment.authorId == currentUserId,
                onReply: { onReply(thread.comment) },
                onEdit: { onEdit(thread.comment) },
                onDelete: { onDelete(thread.comment) }
            )

            // Replies
            if !thread.replies.isEmpty {
                VStack(alignment: .leading, spacing: 8) {
                    ForEach(thread.replies, id: \.id) { reply in
                        ReplyCommentCell(
                            comment: reply,
                            isCurrentUser: reply.authorId == currentUserId,
                            onReply: { onReply(reply) },
                            onEdit: { onEdit(reply) },
                            onDelete: { onDelete(reply) }
                        )
                    }

                    if thread.hasMoreReplies {
                        Button(action: { onLoadMoreReplies(thread.comment) }) {
                            Text("Load more replies")
                                .font(.system(size: 14))
                                .foregroundColor(.blue)
                        }
                        .padding(.leading, 32)
                    }
                }
            }
        }
        .padding(.vertical, 8)
    }
}

// MARK: - Comment Cell

private struct CommentCell: View {
    let comment: Comment
    let isCurrentUser: Bool
    let onReply: () -> Void
    let onEdit: () -> Void
    let onDelete: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            // Header
            HStack(spacing: 12) {
                // Avatar
                ZStack {
                    Circle()
                        .fill(Color.blue.opacity(0.1))
                        .frame(width: 36, height: 36)

                    Text(comment.authorName.prefix(1).uppercased())
                        .font(.system(size: 16, weight: .semibold))
                        .foregroundColor(.blue)
                }

                VStack(alignment: .leading, spacing: 2) {
                    HStack(spacing: 8) {
                        Text(comment.authorName)
                            .font(.system(size: 16, weight: .semibold))
                            .foregroundColor(.primary)

                        if comment.isEdited {
                            Text("(edited)")
                                .font(.system(size: 12))
                                .foregroundColor(.secondary)
                        }
                    }

                    Text(formatTimestamp(comment.createdAt))
                        .font(.system(size: 12))
                        .foregroundColor(.secondary)
                }

                Spacer()

                // Actions Menu
                Menu {
                    Button(action: onReply) {
                        Label("Reply", systemImage: "arrowshape.turn.up.left")
                    }

                    if isCurrentUser {
                        Button(action: onEdit) {
                            Label("Edit", systemImage: "pencil")
                        }

                        Button(role: .destructive, action: onDelete) {
                            Label("Delete", systemImage: "trash")
                        }
                    }
                } label: {
                    Image(systemName: "ellipsis")
                        .font(.system(size: 14))
                        .foregroundColor(.secondary)
                        .frame(width: 24, height: 24)
                }
            }

            // Content
            Text(comment.content)
                .font(.system(size: 16))
                .foregroundColor(.primary)
                .lineSpacing(4)
                .fixedSize(horizontal: false, vertical: true)

            // Reply count
            if comment.replyCount > 0 {
                HStack(spacing: 4) {
                    Image(systemName: "bubble.left.and.bubble.right")
                        .font(.system(size: 12))
                        .foregroundColor(.secondary)

                    Text("\(comment.replyCount) repl\(comment.replyCount == 1 ? "y" : "ies")")
                        .font(.system(size: 12))
                        .foregroundColor(.secondary)
                }
            }
        }
        .padding(16)
        .background(Color.white.opacity(0.8))
        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
        .shadow(color: Color.black.opacity(0.05), radius: 8, x: 0, y: 4)
        .contextMenu {
            Button(action: onReply) {
                Label("Reply", systemImage: "arrowshape.turn.up.left")
            }

            if isCurrentUser {
                Button(action: onEdit) {
                    Label("Edit", systemImage: "pencil")
                }

                Button(role: .destructive, action: onDelete) {
                    Label("Delete", systemImage: "trash")
                }
            }
        }
    }

    private func formatTimestamp(_ isoString: String) -> String {
        // TODO: Implement proper date formatting
        // For now, return a placeholder
        return "2h ago"
    }
}

// MARK: - Reply Comment Cell

private struct ReplyCommentCell: View {
    let comment: Comment
    let isCurrentUser: Bool
    let onReply: () -> Void
    let onEdit: () -> Void
    let onDelete: () -> Void

    var body: some View {
        HStack(alignment: .top, spacing: 12) {
            // Indentation line
            Rectangle()
                .fill(Color.gray)
                .frame(width: 2)
                .clipShape(RoundedRectangle(cornerRadius: 1, style: .continuous))

            VStack(alignment: .leading, spacing: 8) {
                // Header
                HStack(spacing: 8) {
                    ZStack {
                        Circle()
                            .fill(Color.blue.opacity(0.1))
                            .frame(width: 24, height: 24)

                        Text(comment.authorName.prefix(1).uppercased())
                            .font(.system(size: 12, weight: .semibold))
                            .foregroundColor(.blue)
                    }

                    Text(comment.authorName)
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundColor(.primary)

                    if comment.isEdited {
                        Text("(edited)")
                            .font(.system(size: 12))
                            .foregroundColor(.secondary)
                    }

                    Spacer()

                    Text(formatTimestamp(comment.createdAt))
                        .font(.system(size: 12))
                        .foregroundColor(.secondary)
                }

                // Content
                Text(comment.content)
                    .font(.system(size: 14))
                    .foregroundColor(.primary)
                    .lineSpacing(3)
                    .fixedSize(horizontal: false, vertical: true)

                // Actions
                HStack(spacing: 16) {
                    Button(action: onReply) {
                        Text("Reply")
                            .font(.system(size: 12))
                            .foregroundColor(.blue)
                    }

                    if isCurrentUser {
                        Button(action: onEdit) {
                            Text("Edit")
                                .font(.system(size: 12))
                                .foregroundColor(.secondary)
                        }

                        Button(action: onDelete) {
                            Text("Delete")
                                .font(.system(size: 12))
                                .foregroundColor(.red)
                        }
                    }
                }
            }
        }
        .padding(.leading, 8)
    }

    private func formatTimestamp(_ isoString: String) -> String {
        // TODO: Implement proper date formatting
        return "1h ago"
    }
}

// MARK: - Filter Chip

private struct CommentFilterChip: View {
    let title: String
    let icon: String?
    let isSelected: Bool
    let action: () -> Void

    init(title: String, isSelected: Bool, action: @escaping () -> Void) {
        self.title = title
        self.icon = nil
        self.isSelected = isSelected
        self.action = action
    }

    init(title: String, icon: String, isSelected: Bool, action: @escaping () -> Void) {
        self.title = title
        self.icon = icon
        self.isSelected = isSelected
        self.action = action
    }

    var body: some View {
        Button(action: action) {
            HStack(spacing: 6) {
                if let icon = icon {
                    Image(systemName: icon)
                        .font(.system(size: 12))
                }

                Text(title)
                    .font(.system(size: 14, weight: .medium))
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 8)
            .background(
                isSelected
                    ? Color.blue.opacity(0.1)
                    : Color.gray.opacity(0.1)
            )
            .foregroundColor(isSelected ? .blue : .secondary)
            .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: 16, style: .continuous)
                    .stroke(isSelected ? Color.blue.opacity(0.3) : Color.clear, lineWidth: 1)
            )
        }
    }
}

// MARK: - Preview

struct CommentsView_Previews: PreviewProvider {
    static var previews: some View {
        // Note: Preview uses placeholder data
        // Real implementation requires proper database initialization
        CommentsView(
            eventId: "event-1",
            section: nil,
            sectionItemId: nil,
            currentUserId: "user-1",
            currentUserName: "John Doe"
        )
    }
}