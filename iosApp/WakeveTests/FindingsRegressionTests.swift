import XCTest
import Shared
@testable import Wakeve

@MainActor
final class FindingsRegressionTests: XCTestCase {
    func testDevelopmentAuthStoresAccessTokenForSyncConsumers() async throws {
        let authService = AuthenticationService(tokenStorage: InMemorySecureTokenStorage())
        await authService.signOut()
        let manager = AuthStateManager(authService: authService)

        await manager.setAuthStateForDevelopment(userId: "dev-user-test", accessToken: "dev-token-test")

        let token = await authService.getAccessToken()
        let isAuthenticated = await authService.isAuthenticated()
        XCTAssertEqual(token, "dev-token-test")
        XCTAssertTrue(isAuthenticated)
    }

    func testDevelopmentLaunchAuthRequiresExplicitFlag() {
        XCTAssertFalse(
            AuthStateManager.shouldUseDevelopmentLaunchAuthentication(
                arguments: ["Wakeve"],
                environment: [:]
            )
        )
        XCTAssertTrue(
            AuthStateManager.shouldUseDevelopmentLaunchAuthentication(
                arguments: ["Wakeve", "--wakeve-debug-authenticated"],
                environment: [:]
            )
        )
        XCTAssertTrue(
            AuthStateManager.shouldUseDevelopmentLaunchAuthentication(
                arguments: ["Wakeve"],
                environment: ["WAKEVE_DEBUG_AUTHENTICATED": "1"]
            )
        )
    }

    func testGuestDataDeletionClearsLocalGuestSession() async throws {
        let authService = AuthenticationService(tokenStorage: InMemorySecureTokenStorage())
        let manager = AuthStateManager(authService: authService)

        await manager.continueAsGuest()
        XCTAssertTrue(manager.isAuthenticated)
        XCTAssertTrue(manager.isCurrentSessionGuest)
        XCTAssertNotNil(UserDefaults.standard.string(forKey: "wakeve_guest_user_id"))

        await manager.deleteGuestData()

        XCTAssertFalse(manager.isAuthenticated)
        XCTAssertNil(manager.currentUser)
        XCTAssertNil(UserDefaults.standard.string(forKey: "wakeve_guest_user_id"))
        XCTAssertNil(UserDefaults.standard.string(forKey: "wakeve_guest_user_name"))
    }

    func testProfileDataManagementExposesAccountDeletionFlow() throws {
        let profile = try readProjectFile("iosApp/src/Views/Profile/ProfileTabView.swift")
        let authService = try readProjectFile("iosApp/src/Services/AuthenticationService.swift")
        let authState = try readProjectFile("iosApp/src/Services/AuthStateManager.swift")

        XCTAssertTrue(profile.contains("NavigationLink"))
        XCTAssertTrue(profile.contains("DataManagementView"))
        XCTAssertTrue(profile.contains("data_management.delete_account"))
        XCTAssertTrue(profile.contains("data_management.delete_guest_data"))
        XCTAssertTrue(profile.contains("confirmationDialog"))
        XCTAssertTrue(authService.contains("func deleteAccount() async throws -> AccountDeletionResponse"))
        XCTAssertTrue(authService.contains("\"\\(baseUrl)/user/delete\""))
        XCTAssertTrue(authState.contains("func deleteCurrentAccount() async throws"))
        XCTAssertTrue(authState.contains("try await authService.deleteAccount()"))
        XCTAssertTrue(authState.contains("await completeLocalAccountDeletion()"))
    }

    func testProfileExposesRequiredLegalAndNoticeLinks() throws {
        let profile = try readProjectFile("iosApp/src/Views/Profile/ProfileTabView.swift")
        let english = try readProjectFile("iosApp/src/Resources/en.lproj/Localizable.strings")
        let french = try readProjectFile("iosApp/src/Resources/fr.lproj/Localizable.strings")

        XCTAssertTrue(profile.contains("https://wakeve.app/support"))
        XCTAssertTrue(profile.contains("https://wakeve.app/privacy"))
        XCTAssertTrue(profile.contains("https://wakeve.app/terms"))
        XCTAssertTrue(profile.contains("https://wakeve.app/third-party-notices"))
        XCTAssertTrue(profile.contains("profile.third_party_notices"))
        XCTAssertTrue(english.contains("\"profile.third_party_notices\" = \"Third-party notices\""))
        XCTAssertTrue(french.contains("\"profile.third_party_notices\" = \"Notices tierces\""))
    }

    func testAuthAndOnboardingProveWakeveValueBeforeSignIn() throws {
        let login = try readProjectFile("iosApp/src/Views/Auth/LoginView.swift")
        let onboarding = try readProjectFile("iosApp/src/Views/Auth/OnboardingView.swift")
        let english = try readProjectFile("iosApp/src/Resources/en.lproj/Localizable.strings")
        let french = try readProjectFile("iosApp/src/Resources/fr.lproj/Localizable.strings")

        XCTAssertTrue(login.contains("loginValueProofCard"))
        XCTAssertTrue(login.contains("auth.value_proof_title"))
        XCTAssertTrue(login.contains("auth.value_proof_relaunch"))
        XCTAssertTrue(login.contains("LoginProofRow"))
        XCTAssertTrue(onboarding.contains("OnboardingProofCard"))
        XCTAssertTrue(onboarding.contains("proofTitle: String(localized: \"onboarding.proof.create_title\")"))
        XCTAssertTrue(onboarding.contains("proofTitle: String(localized: \"onboarding.proof.enjoy_title\")"))
        XCTAssertTrue(english.contains("Why Wakeve beats the thread"))
        XCTAssertTrue(english.contains("WhatsApp or Messages"))
        XCTAssertTrue(french.contains("Pourquoi Wakeve bat le groupe"))
        XCTAssertTrue(french.contains("WhatsApp ou Messages"))
    }

    func testLoginAppleSignInErrorsUseLocalizedTrustCopy() throws {
        let login = try readProjectFile("iosApp/src/Views/Auth/LoginView.swift")
        let requiredKeys = [
            "auth.error.sign_in_failed_retry",
            "auth.error.invalid_response",
            "auth.error.not_handled",
            "auth.error.unknown",
            "auth.error.sign_in_failed_format",
            "auth.error.authentication_failed_format"
        ]

        for key in requiredKeys {
            XCTAssertTrue(login.contains("String(localized: \"\(key)\")"), "LoginView must use localized key \(key).")
        }

        XCTAssertFalse(login.contains("Sign-in failed. Please try again."))
        XCTAssertFalse(login.contains("Invalid response from Apple. Please try again."))
        XCTAssertFalse(login.contains("Sign-in request not handled."))
        XCTAssertFalse(login.contains("An unknown error occurred."))
        XCTAssertFalse(login.contains("Sign-in failed: \\("))
        XCTAssertFalse(login.contains("Authentication failed: \\("))

        for locale in ["en", "fr", "es", "it", "pt"] {
            let strings = try readProjectFile("iosApp/src/Resources/\(locale).lproj/Localizable.strings")
            for key in requiredKeys {
                XCTAssertTrue(strings.contains("\"\(key)\""), "Missing localized auth error key \(key) for \(locale).")
            }
        }
    }

    func testProfilePrioritizesOrganizerTrustOverGamification() throws {
        let profile = try readProjectFile("iosApp/src/Views/Profile/ProfileTabView.swift")
        let profileViewModel = try readProjectFile("iosApp/src/ViewModels/ProfileViewModel.swift")
        let hierarchy = slice(profile, from: "VStack(spacing: WakeveTheme.Spacing.xl)", to: "PreferencesSection(userId: userId)")
        let english = try readProjectFile("iosApp/src/Resources/en.lproj/Localizable.strings")
        let french = try readProjectFile("iosApp/src/Resources/fr.lproj/Localizable.strings")

        XCTAssertTrue(hierarchy.contains("ProfileReliabilitySection()"))
        XCTAssertFalse(hierarchy.contains("GamificationSummarySection()"))
        XCTAssertFalse(hierarchy.contains("ProfileTabBadgesSection()"))
        XCTAssertFalse(hierarchy.contains("LeaderboardLinkSection("))
        XCTAssertFalse(profile.contains("struct GamificationSummarySection"))
        XCTAssertFalse(profile.contains("ProfileTabBadgeItemView"))
        XCTAssertFalse(profile.contains("badgeSystemImage: \"sparkles\""))
        XCTAssertFalse(profile.contains("showDashboard"))
        XCTAssertTrue(profile.contains("OrganizerUtilitySection()"))
        XCTAssertTrue(profile.contains("profileOrganizerUtilitySection"))
        XCTAssertTrue(profile.contains("profile.organizer.dashboard_title"))
        XCTAssertTrue(profile.contains("profileReliabilitySection"))
        XCTAssertTrue(profile.contains("profileIdentityReadyPill"))
        XCTAssertTrue(profile.contains("profile.identity_ready"))
        XCTAssertTrue(profile.contains("profile.reliability.invite_title"))
        XCTAssertTrue(profile.contains("profile.reliability.privacy_detail"))
        XCTAssertTrue(english.contains("Organizer confidence"))
        XCTAssertTrue(english.contains("Identity ready for invites"))
        XCTAssertTrue(english.contains("Event dashboards live inside events"))
        XCTAssertTrue(french.contains("Confiance organisateur"))
        XCTAssertTrue(french.contains("Identité prête pour les invitations"))
        XCTAssertTrue(french.contains("Les tableaux de bord vivent dans les événements"))
        XCTAssertTrue(profileViewModel.contains("leaderboard.you"))
        XCTAssertTrue(profileViewModel.contains("badges = []"))
        XCTAssertTrue(profileViewModel.contains("leaderboard = []"))
        XCTAssertTrue(profileViewModel.contains("wakeve_guest_user_id"))
        XCTAssertFalse(profileViewModel.contains("Mock data for demonstration"))
        XCTAssertFalse(profileViewModel.contains("Alice Martin"))
        XCTAssertFalse(profileViewModel.contains("totalPoints: 1250"))
        XCTAssertFalse(profileViewModel.contains("Premier Événement"))
        XCTAssertFalse(profileViewModel.contains("Maître du Vote"))
        XCTAssertFalse(profileViewModel.contains("username: \"Vous\""))
    }

    func testLeaderboardDoesNotShowFakeSocialProof() throws {
        let leaderboard = try readProjectFile("iosApp/src/Views/Explore/LeaderboardView.swift")
        let english = try readProjectFile("iosApp/src/Resources/en.lproj/Localizable.strings")
        let french = try readProjectFile("iosApp/src/Resources/fr.lproj/Localizable.strings")

        XCTAssertTrue(leaderboard.contains("WakeveScreenBackground(style: .grouped)"))
        XCTAssertTrue(leaderboard.contains("WakeveContentCard(prominence: .prominent"))
        XCTAssertTrue(leaderboard.contains("leaderboardEmptyState"))
        XCTAssertTrue(leaderboard.contains("leaderboard.empty.title"))
        XCTAssertTrue(leaderboard.contains("init(entries: [LeaderboardEntryData] = [])"))
        XCTAssertFalse(leaderboard.contains("mockData"))
        XCTAssertFalse(leaderboard.contains("Mock data"))
        XCTAssertFalse(leaderboard.contains("Simulate network refresh"))
        XCTAssertFalse(leaderboard.contains("Alice Martin"))
        XCTAssertFalse(leaderboard.contains("Bob Dupont"))
        XCTAssertFalse(leaderboard.contains("totalPoints: 1250"))
        XCTAssertTrue(english.contains("\"leaderboard.empty.title\" = \"No trusted ranking yet\""))
        XCTAssertTrue(french.contains("\"leaderboard.empty.title\" = \"Aucun classement fiable pour l’instant\""))
    }

    func testOperationalLogisticsCopyIsLocalizedAcrossCoreSurfaces() throws {
        let photoPermissions = try readProjectFile("iosApp/src/PhotoPickerPermissionHandler.swift")
        let notifications = try readProjectFile("iosApp/src/ViewModels/NotificationPreferencesViewModel.swift")
        let meals = try readProjectFile("iosApp/src/Views/Events/MealPlanningSheets.swift")
        let english = try readProjectFile("iosApp/src/Resources/en.lproj/Localizable.strings")
        let french = try readProjectFile("iosApp/src/Resources/fr.lproj/Localizable.strings")

        XCTAssertTrue(photoPermissions.contains("photos.permission.disabled_title"))
        XCTAssertTrue(photoPermissions.contains("photos.permission.disabled_message"))
        XCTAssertFalse(photoPermissions.contains("Accès aux photos désactivé"))
        XCTAssertTrue(notifications.contains("notifications.type.event_invite"))
        XCTAssertTrue(notifications.contains("notifications.type.payment_due"))
        XCTAssertFalse(notifications.contains("Invitation événement"))
        XCTAssertTrue(meals.contains("meal.form.name"))
        XCTAssertTrue(meals.contains("meal.auto.estimated_count_format"))
        XCTAssertTrue(meals.contains("meal.dietary.constraint_type"))
        XCTAssertFalse(meals.contains("Nom du repas"))
        XCTAssertFalse(meals.contains("Génération automatique"))

        for key in [
            "photos.permission.disabled_title",
            "budget.item.shared_cost_format",
            "notifications.type.payment_due",
            "meal.validation.start_before_end",
            "meal.dietary.lactose_intolerant"
        ] {
            XCTAssertTrue(english.contains("\"\(key)\""), "Missing English localized key: \(key)")
            XCTAssertTrue(french.contains("\"\(key)\""), "Missing French localized key: \(key)")
        }
    }

    func testUgcModerationReportBlockControlsAreReviewerVisible() throws {
        let commentItem = try readProjectFile("iosApp/src/Views/Collaboration/CommentItemView.swift")
        let commentList = try readProjectFile("iosApp/src/Views/Collaboration/CommentListView.swift")
        let eventDetail = try readProjectFile("iosApp/src/Views/App/ContentView.swift")
        let inboxDetail = try readProjectFile("iosApp/src/Views/Inbox/InboxDetailView.swift")
        let participantManagement = try readProjectFile("iosApp/src/Views/Events/ParticipantManagementView.swift")
        let moderationSheet = try readProjectFile("iosApp/src/Views/Moderation/ModerationActionSheet.swift")
        let moderationService = try readProjectFile("iosApp/src/Services/ModerationService.swift")
        let english = try readProjectFile("iosApp/src/Resources/en.lproj/Localizable.strings")
        let french = try readProjectFile("iosApp/src/Resources/fr.lproj/Localizable.strings")

        XCTAssertTrue(commentItem.contains("reportCommentAction"), "Comment menu must expose report content.")
        XCTAssertTrue(commentItem.contains("reportUserAction"), "Comment menu must expose report user.")
        XCTAssertTrue(commentItem.contains("blockUserAction"), "Comment menu must expose block user.")
        XCTAssertTrue(commentList.contains(".sheet(item: $moderationTarget)"), "Comment list must present the moderation sheet.")
        XCTAssertTrue(eventDetail.contains("reportEventAction"), "Event detail menu must expose report event.")
        XCTAssertTrue(eventDetail.contains("type: .event"), "Event detail report target must use the event moderation type.")
        XCTAssertTrue(inboxDetail.contains("reportChatMessageAction"), "Inbox detail must expose report action for chat/message content.")
        XCTAssertTrue(inboxDetail.contains("blockChatAuthorAction"), "Inbox detail must expose block action for chat/message authors.")
        XCTAssertTrue(inboxDetail.contains("type: .chatMessage"), "Inbox detail report target must use the chat message moderation type.")
        XCTAssertTrue(inboxDetail.contains(".sheet(item: $moderationTarget)"), "Inbox detail must present the moderation sheet.")
        XCTAssertTrue(participantManagement.contains("reportParticipantUserAction"), "Participant rows must expose report user.")
        XCTAssertTrue(participantManagement.contains("blockParticipantUserAction"), "Participant rows must expose block user.")
        XCTAssertTrue(participantManagement.contains("type: .user"), "Participant actions must use the user moderation type.")
        XCTAssertTrue(commentItem.contains("moderatedCommentNotice"), "Rejected or hidden comments must show a moderated-content notice.")
        XCTAssertTrue(commentItem.contains("isModerationHidden"), "Comment rendering must distinguish hidden/rejected moderation states.")
        XCTAssertTrue(moderationSheet.contains("ModerationActionSheet"), "Moderation action sheet must be present.")
        XCTAssertTrue(moderationSheet.contains("support@wakeve.app"), "Moderation sheet must expose human support.")
        XCTAssertTrue(moderationSheet.contains("moderationStatusBadge"), "Moderation sheet must include status badge accessibility marker.")
        XCTAssertTrue(moderationSheet.contains("moderation.unblock_user"), "Moderation sheet must expose unblock user.")
        XCTAssertTrue(moderationService.contains("/moderation/reports"), "Moderation service must submit reports.")
        XCTAssertTrue(moderationService.contains("/moderation/blocks"), "Moderation service must submit blocks.")
        XCTAssertTrue(moderationService.contains("func unblockUser"), "Moderation service must submit unblock requests.")
        XCTAssertTrue(moderationService.contains("method: \"DELETE\""), "Moderation unblock must use the backend DELETE contract.")
        XCTAssertTrue(moderationService.contains("Authorization"), "Moderation service must send authenticated requests.")
        XCTAssertTrue(english.contains("\"moderation.report_content\" = \"Report Content\""), "English moderation report copy must be localized.")
        XCTAssertTrue(english.contains("\"moderation.block_user\" = \"Block User\""), "English block copy must be localized.")
        XCTAssertTrue(english.contains("\"moderation.unblock_user\" = \"Unblock User\""), "English unblock copy must be localized.")
        XCTAssertTrue(english.contains("\"moderation.report_chat_context\""), "English chat report context must be localized.")
        XCTAssertTrue(english.contains("\"moderation.hidden_content_notice\""), "English hidden-content notice must be localized.")
        XCTAssertTrue(french.contains("\"moderation.report_content\" = \"Signaler le contenu\""), "French moderation report copy must be localized.")
        XCTAssertTrue(french.contains("\"moderation.block_user\" = \"Bloquer l'utilisateur\""), "French block copy must be localized.")
        XCTAssertTrue(french.contains("\"moderation.unblock_user\""), "French unblock copy must be localized.")
        XCTAssertTrue(french.contains("\"moderation.report_chat_context\""), "French chat report context must be localized.")
        XCTAssertTrue(french.contains("\"moderation.hidden_content_notice\""), "French hidden-content notice must be localized.")
    }

    func testParticipantManagementRequiresAtLeastOneProposedSlotBeforeStartingPoll() throws {
        let source = try readProjectFile("iosApp/src/Views/Events/ParticipantManagementView.swift")
        let canStartPoll = slice(source, from: "private var canStartPoll", to: "private var hasTimezoneCoordinationContext")

        XCTAssertTrue(
            canStartPoll.contains("!event.proposedSlots.isEmpty"),
            "A draft poll must not be startable until at least one time slot exists."
        )
        XCTAssertFalse(
            canStartPoll.contains("!participantRows.isEmpty"),
            "Invitation-first sharing must allow starting the poll before manually adding email/contact participants."
        )
        XCTAssertTrue(
            source.contains("participants.draft_status.link_ready"),
            "Draft status should explain that the share link can carry guests into the poll."
        )
    }

    func testParticipantManagementSurfacesTimezoneCoordinationForInternationalGuests() throws {
        let source = try readProjectFile("iosApp/src/Views/Events/ParticipantManagementView.swift")
        let timezoneCard = slice(source, from: "private var timezoneCoordinationCard", to: "@ViewBuilder")

        XCTAssertTrue(source.contains("participantTimezoneCoordinationCard"))
        XCTAssertTrue(source.contains("hasTimezoneCoordinationContext"))
        XCTAssertTrue(source.contains("!event.proposedSlots.isEmpty"))
        XCTAssertTrue(source.contains("event.proposedSlots"))
        XCTAssertTrue(source.contains("map(\\.timezone)"))
        XCTAssertTrue(source.contains("TimeZone.current.identifier"))
        XCTAssertTrue(source.contains("timezoneDisplayName"))
        XCTAssertTrue(timezoneCard.contains("ParticipantTimezoneCoordinationCard"))
        XCTAssertTrue(timezoneCard.contains("participants.timezone.footnote"))

        for locale in ["en", "fr", "es", "it", "pt"] {
            let strings = try readProjectFile("iosApp/src/Resources/\(locale).lproj/Localizable.strings")
            for key in [
                "participants.timezone.title",
                "participants.timezone.single_subtitle_format",
                "participants.timezone.multi_subtitle_format",
                "participants.timezone.event_label",
                "participants.timezone.local_label",
                "participants.timezone.options_label",
                "participants.timezone.options_format",
                "participants.timezone.same_badge",
                "participants.timezone.local_badge",
                "participants.timezone.footnote"
            ] {
                XCTAssertTrue(strings.contains("\"\(key)\""), "Missing localized participant timezone key \(key) for \(locale).")
            }
        }
    }

    func testEventNextActionDoesNotBlockInvitationFirstPollsOnParticipants() throws {
        let source = try readProjectFile("iosApp/src/Views/Events/HomeView.swift")
        let nextAction = slice(source, from: "struct EventNextAction", to: "// MARK: - Event Theme")

        XCTAssertTrue(
            nextAction.contains("let hasSlots = !event.proposedSlots.isEmpty"),
            "Draft next action should stay gated by concrete poll options."
        )
        XCTAssertFalse(
            nextAction.contains("let hasParticipants = !event.participants.isEmpty"),
            "Draft next action must not require manual participants because share links can collect guests."
        )
        XCTAssertFalse(
            nextAction.contains("events.next_action.draft.blocked.participants"),
            "The primary action should not block an invitation-first poll when only participants are missing."
        )
    }

    func testCreateEventTurnsSelectedDateIntoProposedSlot() {
        let viewModel = CreateEventViewModel()
        var proposedSlotStarts: [String?] = []
        viewModel.onEventCreated = { event in
            proposedSlotStarts = event.proposedSlots.map(\.start)
        }

        viewModel.createEvent(
            title: "Week-end Lyon",
            description: "Pique-nique",
            userId: "dev-user-test",
            selectedDate: "2026-06-12T18:00:00Z"
        )

        XCTAssertEqual(proposedSlotStarts.count, 1)
        XCTAssertEqual(proposedSlotStarts.first ?? nil, "2026-06-12T18:00:00Z")
    }

    func testCreateEventTurnsMultipleSelectedSlotsIntoProposedSlots() {
        let viewModel = CreateEventViewModel()
        var proposedSlotStarts: [String?] = []
        var proposedSlotEnds: [String?] = []
        var proposedSlotTimesOfDay: [Shared.TimeOfDay] = []
        viewModel.onEventCreated = { event in
            proposedSlotStarts = event.proposedSlots.map(\.start)
            proposedSlotEnds = event.proposedSlots.map(\.end)
            proposedSlotTimesOfDay = event.proposedSlots.map(\.timeOfDay)
        }

        viewModel.createEvent(
            title: "Week-end Lyon",
            description: "Pique-nique",
            userId: "dev-user-test",
            selectedSlots: [
                EventTimeSlotInput(
                    start: "2026-06-12T18:00:00Z",
                    end: "2026-06-12T20:00:00Z",
                    timeOfDay: .specific
                ),
                EventTimeSlotInput(
                    start: "2026-06-13T09:00:00Z",
                    end: nil,
                    timeOfDay: .allDay
                )
            ]
        )

        XCTAssertEqual(proposedSlotStarts, [
            "2026-06-12T18:00:00Z",
            "2026-06-13T09:00:00Z"
        ])
        XCTAssertEqual(proposedSlotEnds.first ?? nil, "2026-06-12T20:00:00Z")
        XCTAssertEqual(proposedSlotTimesOfDay, [.specific, .allDay])
    }

    func testAppLaunchDoesNotRequestNotificationAuthorizationImmediately() throws {
        let source = try readProjectFile("iosApp/src/Services/AppDelegate.swift")
        let didFinishLaunching = slice(
            source,
            from: "func application(",
            to: "// MARK: - Remote Notification Registration"
        )

        XCTAssertFalse(
            didFinishLaunching.contains("requestAuthorization"),
            "Notification permission must be requested from an explicit user action, not during app launch/onboarding."
        )
    }

    func testInvitationPreviewReservesHeaderSpaceForTitle() throws {
        let source = try readProjectFile("iosApp/src/Views/Events/CreateEventSheet.swift")
        let preview = slice(source, from: "struct EventPreviewSheet", to: "// MARK: - Preview Cards")

        XCTAssertTrue(
            preview.contains("previewHeaderReservedHeight"),
            "EventPreviewSheet must reserve enough vertical space so the title cannot sit under the back/next buttons."
        )
    }

    func testVisibleFindingStringsAreLocalized() throws {
        let french = try readProjectFile("iosApp/src/Resources/fr.lproj/Localizable.strings")
        let requiredFrenchKeys = [
            "\"profile.edit\" = \"Modifier\"",
            "\"poll.results.title\" = \"Résultats\"",
            "\"poll.results.no_slots_title\" = \"Aucun créneau proposé\"",
            "\"poll.results.no_votes_title\" = \"Aucun vote pour le moment\"",
            "\"inbox.filter.inbox\" = \"Inbox\"",
            "\"inbox.filter.focused\" = \"Prioritaires\"",
            "\"inbox.filter.new\" = \"Nouveau\"",
            "\"inbox.filter.unread\" = \"Non lus\"",
            "\"inbox.filter.event\" = \"Événement\"",
            "\"settings_sheet.data_management\" = \"Gestion des données\"",
            "\"data_management.delete_account\" = \"Supprimer le compte\"",
            "\"data_management.delete_guest_data\" = \"Supprimer les données invité\""
        ]

        for key in requiredFrenchKeys {
            XCTAssertTrue(french.contains(key), "Missing localized French string: \(key)")
        }
    }

    func testEventTypeDisplayUsesLocalizedHelper() throws {
        let helpers = try readProjectFile("iosApp/src/Extensions/ViewExtensions.swift")
        let createSheet = try readProjectFile("iosApp/src/Views/Events/CreateEventSheet.swift")

        XCTAssertTrue(helpers.contains("func eventTypeDisplayName"))
        XCTAssertTrue(
            createSheet.contains("eventTypeDisplayName(selectedEventType)") &&
            createSheet.contains("eventTypeDisplayName(eventType)"),
            "Event type labels must use localized display names instead of Kotlin displayName."
        )
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

private final class InMemorySecureTokenStorage: SecureTokenStorageProtocol {
    private var accessToken: String?
    private var refreshToken: String?
    private var userId: String?
    private var tokenExpiry: Int64?

    func storeAccessToken(_ token: String) async throws {
        accessToken = token
    }

    func storeRefreshToken(_ token: String) async throws {
        refreshToken = token
    }

    func storeUserId(_ userId: String) async throws {
        self.userId = userId
    }

    func storeTokenExpiry(_ expiryTimestamp: Int64) async throws {
        tokenExpiry = expiryTimestamp
    }

    func getAccessToken() async -> String? {
        accessToken
    }

    func getRefreshToken() async -> String? {
        refreshToken
    }

    func getUserId() async -> String? {
        userId
    }

    func getTokenExpiry() async -> Int64? {
        tokenExpiry
    }

    func clearAllTokens() async throws {
        accessToken = nil
        refreshToken = nil
        userId = nil
        tokenExpiry = nil
    }

    func isTokenExpired() async -> Bool {
        guard let tokenExpiry else {
            return true
        }

        return Date().timeIntervalSince1970 * 1000 >= Double(tokenExpiry)
    }

    func hasValidToken() async -> Bool {
        guard let accessToken, !accessToken.isEmpty else {
            return false
        }

        return !(await isTokenExpired())
    }
}
