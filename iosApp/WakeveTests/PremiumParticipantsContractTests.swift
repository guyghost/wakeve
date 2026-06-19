import XCTest
@testable import Wakeve

final class PremiumParticipantsContractTests: XCTestCase {
    func testParticipantsUseGroupedPremiumHierarchy() throws {
        let source = try readProjectFile("iosApp/src/Views/Events/ParticipantManagementView.swift")
        let content = slice(source, from: "struct ParticipantManagementView", to: "private struct ParticipantPresentationRow")

        XCTAssertTrue(content.contains("EventHeroCard("))
        XCTAssertTrue(content.contains("LiquidGlassToolbar(title: String(localized: \"participants.title\")"))
        XCTAssertTrue(content.contains("ParticipantGroupSection("))
        XCTAssertTrue(content.contains("title: String(localized: \"participants.accepted\")"))
        XCTAssertTrue(content.contains("title: String(localized: \"participants.pending\")"))
        XCTAssertTrue(content.contains("title: String(localized: \"participants.declined\")"))
    }

    func testParticipantGroupsUseBrandContentCards() throws {
        let source = try readProjectFile("iosApp/src/Views/Events/ParticipantManagementView.swift")
        let section = slice(source, from: "private struct ParticipantGroupSection", to: "private struct ParticipantRowView")

        XCTAssertTrue(section.contains("WakeveGroupCard("))
        XCTAssertTrue(section.contains("SemanticColor.secondaryText"))
        XCTAssertTrue(section.contains("SemanticColor.badge"))
        XCTAssertFalse(section.contains("WakeveGlassCard("), "Participant groups are content surfaces and should not use glass cards.")
    }

    func testParticipantsExposeContextualInviteAction() throws {
        let source = try readProjectFile("iosApp/src/Views/Events/ParticipantManagementView.swift")
        let content = slice(source, from: "struct ParticipantManagementView", to: "private struct ParticipantPresentationRow")

        XCTAssertTrue(content.contains("if event.status == .draft"))
        XCTAssertTrue(content.contains("invitationShareCard"))
        XCTAssertTrue(content.contains("InvitationShareSheet("))
        XCTAssertTrue(content.contains("InvitationTokenCodec.invitationCode(forEventId: event.id)"))
        XCTAssertTrue(content.contains("WakeveInvitationPreviewCard("))
        XCTAssertTrue(content.contains("ShareLink(item: invitationShareMessage)"))
        XCTAssertTrue(content.contains("invitationDeliveryStateText"))
        XCTAssertTrue(content.contains("invitation.preview_poster"))
        XCTAssertTrue(content.contains("invitation.share_whatsapp_messages"))
        XCTAssertTrue(content.contains("addParticipantCard"))
        XCTAssertTrue(content.contains("WakeveContentCard(prominence: .regular"))
        XCTAssertTrue(content.contains("String(localized: \"participants.invite_one\")"))
        XCTAssertTrue(content.contains("LiquidGlassButton("))
        XCTAssertTrue(content.contains("canStartPoll"))
    }

    func testParticipantsExposeGroupPresencePulse() throws {
        let source = try readProjectFile("iosApp/src/Views/Events/ParticipantManagementView.swift")
        let content = slice(source, from: "struct ParticipantManagementView", to: "private struct ParticipantPresentationRow")

        XCTAssertTrue(content.contains("groupPresenceCard"))
        XCTAssertTrue(content.contains("ParticipantPresenceCard("))
        XCTAssertTrue(content.contains("ParticipantFacePile("))
        XCTAssertTrue(content.contains("participants.presence.title"))
        XCTAssertTrue(content.contains("participants.presence.subtitle.active"))
        XCTAssertTrue(content.contains("participants.presence.next_action.nudge"))
        XCTAssertTrue(content.contains("groupPresenceProgress"))
        XCTAssertTrue(content.contains("participants.count.singular_format"))
        XCTAssertTrue(content.contains("participants.count.plural_format"))
    }

    func testParticipantsPresenceIsLocalizedInCoreLocales() throws {
        let localeFiles = [
            "iosApp/src/Resources/en.lproj/Localizable.strings",
            "iosApp/src/Resources/fr.lproj/Localizable.strings",
            "iosApp/src/Resources/es.lproj/Localizable.strings",
            "iosApp/src/Resources/it.lproj/Localizable.strings",
            "iosApp/src/Resources/pt.lproj/Localizable.strings"
        ]
        let requiredKeys = [
            "participants.count.singular_format",
            "participants.count.plural_format",
            "participants.presence.title",
            "participants.presence.subtitle.empty",
            "participants.presence.subtitle.pending_only",
            "participants.presence.subtitle.active",
            "participants.presence.metric.going",
            "participants.presence.metric.waiting",
            "participants.presence.metric.unavailable",
            "participants.presence.next_action.label",
            "participants.presence.next_action.share",
            "participants.presence.next_action.nudge",
            "participants.presence.next_action.poll",
            "participants.presence.next_action.ready"
        ]

        for localeFile in localeFiles {
            let content = try readProjectFile(localeFile)
            for key in requiredKeys {
                XCTAssertTrue(content.contains("\"\(key)\""), "\(localeFile) is missing \(key)")
            }
        }
    }

    func testParticipantsErrorCopyUsesLocalizationKeys() throws {
        let source = try readProjectFile("iosApp/src/Views/Events/ParticipantManagementView.swift")
        let requiredKeys = [
            "participants.error.invalid_email",
            "participants.error.add_failed",
            "participants.error.add_selected_failed_format",
            "participants.error.start_poll_failed"
        ]

        for key in requiredKeys {
            XCTAssertTrue(source.contains("String(localized: \"\(key)\")"), "Participant management must use localized key \(key).")
        }

        XCTAssertFalse(source.contains("Saisissez une adresse email valide"))
        XCTAssertFalse(source.contains("Impossible d’ajouter ce participant"))
        XCTAssertFalse(source.contains("Impossible d'ajouter \\(failedEmails.count) participant(s)"))
        XCTAssertFalse(source.contains("Impossible de lancer le sondage"))

        for localeFile in [
            "iosApp/src/Resources/en.lproj/Localizable.strings",
            "iosApp/src/Resources/fr.lproj/Localizable.strings",
            "iosApp/src/Resources/es.lproj/Localizable.strings",
            "iosApp/src/Resources/it.lproj/Localizable.strings",
            "iosApp/src/Resources/pt.lproj/Localizable.strings"
        ] {
            let content = try readProjectFile(localeFile)
            for key in requiredKeys {
                XCTAssertTrue(content.contains("\"\(key)\""), "\(localeFile) is missing \(key)")
            }
        }
    }

    func testParticipantsPreserveRepositoryBoundary() throws {
        let source = try readProjectFile("iosApp/src/Views/Events/ParticipantManagementView.swift")
        let content = slice(source, from: "struct ParticipantManagementView", to: "private struct ParticipantPresentationRow")

        XCTAssertTrue(content.contains("repository?.getParticipantRecords"))
        XCTAssertTrue(content.contains("repository.addParticipant"))
        XCTAssertTrue(content.contains("repository.updateEventStatus"))
        XCTAssertTrue(content.contains("!event.proposedSlots.isEmpty"))
        XCTAssertFalse(
            content.contains("StateMachine("),
            "Participants should remain a SwiftUI presentation shell around repository calls."
        )
    }

    func testParticipantPollStartUsesDecisionHaptics() throws {
        let source = try readProjectFile("iosApp/src/Views/Events/ParticipantManagementView.swift")
        let confirmation = slice(source, from: "private func presentStartPollConfirmation", to: "private var invitationDeliveryStateText")
        let startPoll = slice(source, from: "private func startPoll() async", to: "private struct ParticipantPresentationRow")

        XCTAssertTrue(confirmation.contains("WakeveHaptics.selection()"))
        XCTAssertTrue(confirmation.contains("showStartPollConfirmation = true"))
        XCTAssertTrue(source.contains("presentStartPollConfirmation()"))
        XCTAssertTrue(startPoll.contains("WakeveHaptics.success()"))
        XCTAssertTrue(startPoll.contains("WakeveHaptics.warning()"))
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
