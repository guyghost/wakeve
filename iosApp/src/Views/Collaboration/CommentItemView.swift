import SwiftUI

/// Comment Item View for Wakeve
///
/// Liquid Glass design card with comment content, mentions, and actions.
struct CommentItemView: View {
    let comment: Comment
    let isPinned: Bool
    let currentUserId: String
    let isOrganizer: Bool
    let isParent: Bool

    var onReply: (String, String) -> Void = { _, _ in }
    var onEdit: (String, String) -> Void = { _, _ in }
    var onDelete: (String) -> Void = { _ in }
    var onPin: (String, Bool) -> Void = { _, _ in }
    var onUserClick: (String) -> Void = { _ in }

    @State private var showMenu: Bool = false

    var body: some View {
        GlassCard(
            isPinned: isPinned
        ) {
            VStack(alignment: .leading, spacing: 8) {
                // Header: Avatar + Name + Actions
                HStack(alignment: .center) {
                    // Avatar
                    Avatar(initials: getInitials(comment.authorName))
                        .onTapGesture {
                            onUserClick(comment.authorId)
                        }

                    Spacer()
                        .frame(width: 12)

                    // Name + Time
                    VStack(alignment: .leading, spacing: 2) {
                        Text(comment.authorName)
                            .font(.headline)
                            .fontWeight(.bold)
                            .foregroundColor(WakeveColors.onSurface)
                            .onTapGesture {
                                onUserClick(comment.authorId)
                            }

                        Text(formatTimestamp(comment.createdAt))
                            .font(.caption)
                            .foregroundColor(WakeveColors.onSurfaceVariant)
                    }

                    Spacer()

                    // Pin icon (if pinned)
                    if isPinned {
                        Image(systemName: "pin.fill")
                            .foregroundColor(WakeveColors.primary)
                            .font(.system(size: 16))

                        Spacer()
                            .frame(width: 8)
                    }

                    // More options menu
                    if comment.canEdit(currentUserId) || comment.canDelete(currentUserId, isOrganizer) {
                        Menu {
                            Button {
                                Label("Reply", systemImage: "arrow.uturn.backward")
                            } action: {
                                onReply(comment.id, comment.authorName)
                            }

                            if comment.canEdit(currentUserId) {
                                Button {
                                    Label("Edit", systemImage: "pencil")
                                } action: {
                                    onEdit(comment.id, comment.content)
                                }
                            }

                            if comment.canPin(currentUserId, isOrganizer) {
                                Button {
                                    Label(comment.isPinned ? "Unpin" : "Pin", systemImage: "pin")
                                } action: {
                                    onPin(comment.id, !comment.isPinned)
                                }
                            }

                            Divider()

                            if comment.canDelete(currentUserId, isOrganizer) {
                                Button(role: .destructive) {
                                    Label(comment.authorId == currentUserId ? "Delete" : "Remove",
                                          systemImage: "trash")
                                } action: {
                                    onDelete(comment.id)
                                }
                            }
                        } label: {
                            Image(systemName: "ellipsis.circle")
                                .foregroundColor(WakeveColors.onSurface)
                        }
                    }
                }

                // Content with highlighted mentions
                AttributedComment(
                    content: comment.content,
                    mentions: comment.mentions,
                    isDeleted: comment.isDeleted
                )

                // Reply button (for parent comments only)
                if isParent && !comment.isDeleted {
                    Button(action: { onReply(comment.id, comment.authorName) }) {
                        Text("Reply")
                            .font(.body)
                            .foregroundColor(WakeveColors.primary)
                    }
                    .buttonStyle(.borderless)
                }
            }
            .padding(12)
        }
    }
}

/// Glass card with optional pinned styling
struct GlassCard<Content: View>: View {
    let isPinned: Bool
    let content: Content

    var body: some View {
        content
            .background(
                isPinned ? WakeveColors.primary.opacity(0.2) : WakeveColors.surface,
                in: RoundedRectangle(cornerRadius: 16)
            )
            .overlay(
                RoundedRectangle(cornerRadius: 16)
                    .stroke(WakeveColors.outline, lineWidth: 1)
            )
    }
}

/// Avatar placeholder with initials
struct Avatar: View {
    let initials: String

    var body: some View {
        Text(initials)
            .font(.title3)
            .fontWeight(.bold)
            .foregroundColor(WakeveColors.onPrimaryContainer)
            .frame(width: 40, height: 40)
            .background(
                WakeveColors.primaryContainer,
                in: Circle()
            )
    }
}

/// Attributed text with highlighted mentions
struct AttributedComment: View {
    let content: String
    let mentions: [String]
    let isDeleted: Bool

    var body: some View {
        if isDeleted {
            Text("[Deleted]")
                .font(.body)
                .foregroundColor(WakeveColors.onSurfaceVariant)
                .italic()
        } else {
            attributedContent
        }
    }

    private var attributedContent: some View {
        if mentions.isEmpty {
            Text(content)
                .font(.body)
                .foregroundColor(WakeveColors.onSurface)
        } else {
            // Simple version - split and highlight mentions
            var text = content
            for mention in mentions {
                let mentionText = "@\(mention)"
                if let range = text.range(of: mentionText) {
                    let before = String(text.prefix(upTo: range.lowerBound))
                    let after = String(text.suffix(from: range.upperBound))
                    text = before // Simplified for brevity

                    // In production, use AttributedString with custom attributes
                }
            }

            Text(text)
                .font(.body)
                .foregroundColor(WakeveColors.onSurface)
        }
    }
}

/// Get initials from name
func getInitials(_ name: String) -> String {
    let parts = name.split(separator: " ")
        .prefix(2)
        .compactMap { $0.first }
        .map { String($0).uppercased() }
    return parts.joined()
}

/// Format timestamp to relative time
func formatTimestamp(_ timestamp: String) -> String {
    // Parse ISO 8601 timestamp and calculate relative time
    // Simplified for brevity
    let formatter = ISO8601DateFormatter()
    if let date = formatter.date(from: timestamp) {
        let now = Date()
        let interval = now.timeIntervalSince(date)

        if interval < 60 {
            return "Just now"
        } else if interval < 3600 {
            return "\(Int(interval / 60))m ago"
        } else if interval < 86400 {
            return "\(Int(interval / 3600))h ago"
        } else if interval < 604800 {
            return "\(Int(interval / 86400))d ago"
        } else {
            return "\(Int(interval / 604800))w ago"
        }
    }

    return "Unknown time"
}

/// ISO 8601 date formatter
private func ISO8601DateFormatter() -> DateFormatter {
    let formatter = DateFormatter()
    formatter.dateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'"
    formatter.timeZone = TimeZone(secondsFromGMT: 0)
    formatter.locale = Locale(identifier: "en_US_POSIX")
    return formatter
}
