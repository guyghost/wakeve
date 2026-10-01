import XCTest
import Shared
@testable import Wakeve

/// Commentaires de la route `.comments` (couche 5c, #47) : écritures branchées sur le dépôt Kotlin.
final class EventCommentsRouteViewTests: XCTestCase {

    // MARK: - Sections

    func testEverySectionReadsAndWritesItsOwnRepositorySection() {
        let expected: [CommentSectionType: CommentSection_] = [
            .general: .general, .scenario: .scenario, .poll: .poll, .transport: .transport,
            .accommodation: .accommodation, .meal: .meal, .equipment: .equipment,
            .activity: .activity, .budget: .budget
        ]
        XCTAssertEqual(expected.count, CommentSectionType.allCases.count)
        for section in CommentSectionType.allCases {
            XCTAssertEqual(EventCommentsRouteView.repositorySection(for: section), expected[section], "\(section)")
        }
    }

    // MARK: - Contenu

    func testContentIsTrimmedAndBoundedLikeTheKotlinRequest() {
        XCTAssertEqual(EventCommentsRouteView.checkContent("  On part à 9 h \n"), .valid("On part à 9 h"))
        XCTAssertEqual(EventCommentsRouteView.checkContent(" \n "), .empty)
        XCTAssertEqual(EventCommentsRouteView.checkContent(String(repeating: "a", count: 2_000)),
                       .valid(String(repeating: "a", count: 2_000)))
        XCTAssertEqual(EventCommentsRouteView.checkContent(String(repeating: "a", count: 2_001)), .tooLong)
    }

    // MARK: - Droits (mêmes règles que le menu de `CommentItemView`)

    func testOnlyTheAuthorEditsTheAuthorOrOrganizerDeletesAndOnlyTheOrganizerPins() {
        func permits(_ action: EventCommentsRouteView.WriteAction, author: String, organizer: Bool) -> Bool {
            EventCommentsRouteView.permits(action, authorId: author, currentUserId: "me", isOrganizer: organizer)
        }
        XCTAssertTrue(permits(.reply, author: "other", organizer: false))
        XCTAssertTrue(permits(.edit, author: "me", organizer: false))
        XCTAssertFalse(permits(.edit, author: "other", organizer: true), "L'organisateur ne réécrit pas les autres.")
        XCTAssertTrue(permits(.delete, author: "me", organizer: false))
        XCTAssertTrue(permits(.delete, author: "other", organizer: true))
        XCTAssertFalse(permits(.delete, author: "other", organizer: false))
        XCTAssertTrue(permits(.pin, author: "other", organizer: true))
        XCTAssertFalse(permits(.pin, author: "me", organizer: false))
    }

    // MARK: - Erreurs

    func testErrorsAreLocalized() {
        let fr = Locale(identifier: "fr")
        XCTAssertEqual(EventCommentsRouteView.errorKey(isModerationRejection: true), "comments.error.rejected")
        XCTAssertEqual(EventCommentsRouteView.errorKey(isModerationRejection: false), "common.error_generic")
        for key in ["comments.error.rejected", "comments.error.too_long", "comments.edit.title",
                    "comments.reply.title_format", "comments.reply.placeholder", "comments.edit.placeholder",
                    "comments.delete.title", "comments.delete.message"] {
            XCTAssertNotEqual(WK.localizedFormat(key, locale: fr), key, key)
        }
        XCTAssertEqual(String(format: WK.localizedFormat("comments.reply.title_format", locale: fr), "Léa"), "Répondre à Léa")
    }

    // MARK: - Source

    /// L'épinglage écrit en base doit s'afficher : l'extension Swift ne masque plus `Comment_.isPinned`.
    func testCommentItemReadsThePinnedFlagFromTheSharedModel() throws {
        let item = try String(contentsOf: URL(fileURLWithPath: #filePath)
            .deletingLastPathComponent().deletingLastPathComponent()
            .appendingPathComponent("src/Views/Collaboration/CommentItemView.swift"), encoding: .utf8)
        XCTAssertFalse(item.contains("var isPinned: Bool { false }"))
        // `getCommentThread` charge toutes les réponses : un `replyCount` non décrémenté par la suppression
        // douce afficherait un « Charger plus de réponses » sans effet.
        XCTAssertFalse(item.contains("var hasMoreReplies: Bool"))
    }

    func testRouteInjectsEveryWriteCallbackIntoTheCommentList() throws {
        let file = try String(contentsOf: URL(fileURLWithPath: #filePath)
            .deletingLastPathComponent().deletingLastPathComponent()
            .appendingPathComponent("src/Views/Events/EventSecondaryRouteViews.swift"), encoding: .utf8)
        let start = try XCTUnwrap(file.range(of: "struct EventCommentsRouteView: View {"))
        let end = try XCTUnwrap(file.range(of: "struct EventPhotosFollowUpRouteView", range: start.upperBound..<file.endIndex))
        let route = String(file[start.lowerBound..<end.lowerBound])
        for callback in ["onAddComment:", "onReply:", "onEdit:", "onDelete:", "onPin:"] {
            XCTAssertTrue(route.contains(callback), "Callback manquant : \(callback)")
        }
        for call in ["createComment(", "updateComment(", "softDeleteComment(", "pinComment(", "unpinComment("] {
            XCTAssertTrue(route.contains(call), "Écriture manquante : \(call)")
        }
        XCTAssertFalse(route.contains("section.sharedValue"), "Toutes les sections passent par `repositorySection`.")
    }
}
