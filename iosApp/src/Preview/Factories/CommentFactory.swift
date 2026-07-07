import Foundation
import Shared

#if DEBUG

enum CommentFactory {
    private static let now = "2026-05-21T10:30:00.000000Z"

    static var organizerComment: Comment_ {
        make(
            id: "comment-organizer-1",
            authorId: UserFactory.organizer.id,
            authorName: UserFactory.organizer.name,
            content: "J'ai ajoute deux options de date. Dites-moi vite celle qui vous arrange le plus.",
            replyCount: 2
        )
    }

    static var participantReply: Comment_ {
        make(
            id: "comment-reply-1",
            authorId: UserFactory.participant.id,
            authorName: UserFactory.participant.name,
            content: "Le samedi soir marche mieux pour moi. @Marie je peux aussi aider pour les courses.",
            parentCommentId: organizerComment.id
        )
    }

    static var editedComment: Comment_ {
        make(
            id: "comment-edited-1",
            authorId: "user-sophie",
            authorName: "Sophie Bernard",
            content: "Je confirme pour deux personnes, sans gluten si possible.",
            updatedAt: now,
            isEdited: true
        )
    }

    static var deletedComment: Comment_ {
        make(
            id: "comment-deleted-1",
            authorId: "user-removed",
            authorName: "Ancien participant",
            content: "[Deleted]"
        )
    }

    static var threads: [CommentThread] {
        [
            CommentThread(
                comment: organizerComment,
                replies: [participantReply, editedComment],
                hasMoreReplies: false
            ),
            CommentThread(
                comment: make(
                    id: "comment-budget-1",
                    authorId: "user-hugo",
                    authorName: "Hugo Moreau",
                    content: "Pour le budget, je propose de bloquer 45 euros par personne pour le repas.",
                    replyCount: 0
                ),
                replies: [],
                hasMoreReplies: false
            )
        ]
    }

    static func make(
        id: String = "comment-preview",
        eventId: String = EventFactory.polling.id,
        section: CommentSection_ = .general,
        sectionItemId: String? = nil,
        authorId: String = UserFactory.organizer.id,
        authorName: String = UserFactory.organizer.name,
        content: String = "Commentaire de preview",
        parentCommentId: String? = nil,
        mentions: [String] = [],
        isDeleted: Bool = false,
        isPinned: Bool = false,
        createdAt: String = now,
        updatedAt: String? = nil,
        isEdited: Bool = false,
        replyCount: Int32 = 0,
        moderationStatus: ModerationStatus = .approved
    ) -> Comment_ {
        Comment_(
            id: id,
            eventId: eventId,
            section: section,
            sectionItemId: sectionItemId,
            authorId: authorId,
            authorName: authorName,
            content: content,
            parentCommentId: parentCommentId,
            mentions: mentions,
            isDeleted: isDeleted,
            isPinned: isPinned,
            createdAt: createdAt,
            updatedAt: updatedAt,
            isEdited: isEdited,
            replyCount: replyCount,
            moderationStatus: moderationStatus
        )
    }
}

#endif
