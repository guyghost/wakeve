import XCTest

final class OrganizationPhase5ContractTests: XCTestCase {
    func testAppViewDefinesPhase5OrganizationDestinations() throws {
        let source = try readProjectFile("iosApp/src/Views/App/ContentView.swift")

        XCTAssertTrue(
            source.contains("case meetingList"),
            "iOS navigation must expose an event-scoped meetings destination."
        )
        XCTAssertTrue(
            source.contains("case budgetOverview") && source.contains("case budgetDetail"),
            "iOS navigation must expose event-scoped budget and expenses destinations."
        )
        XCTAssertTrue(
            source.contains("case paymentPot") || source.contains("case payment"),
            "iOS navigation must expose an event-scoped payment pot destination for ORGANIZING events."
        )
        XCTAssertTrue(
            source.contains("case tricount") || source.contains("case tricountHandoff"),
            "iOS navigation must expose an event-scoped Tricount handoff destination for ORGANIZING events."
        )
    }

    func testEventDetailOrganizingDashboardExposesPhase5EntriesForAuthorizedUsers() throws {
        let source = try readProjectFile("iosApp/src/Views/App/ContentView.swift")
        let eventDetail = slice(
            source,
            from: "struct EventDetailView: View",
            to: "struct OrganizerChip: View"
        )

        XCTAssertTrue(
            eventDetail.contains(".organizing"),
            "EventDetailView must explicitly expose Phase 5 organization actions from ORGANIZING workflow state."
        )
        XCTAssertTrue(
            containsAny(eventDetail, ["onOpenMeetings", "MeetingListView", "Reunions", "Meetings"]),
            "ORGANIZING event detail must expose a meetings entry point."
        )
        XCTAssertTrue(
            containsAny(eventDetail, ["onOpenBudget", "BudgetOverviewView", "BudgetDetailView", "Depenses", "Expenses"]),
            "ORGANIZING event detail must expose budget and expenses entry points."
        )
        XCTAssertTrue(
            containsAny(eventDetail, ["onOpenPayment", "PaymentPot", "Cagnotte", "Pot commun"]),
            "ORGANIZING event detail must expose a payment pot entry point."
        )
        XCTAssertTrue(
            containsAny(eventDetail, ["onOpenTricount", "Tricount"]),
            "ORGANIZING event detail must expose a Tricount handoff entry point."
        )
        XCTAssertTrue(
            containsAny(eventDetail, ["isParticipantConfirmed", "canAccessOrganizationDetails", "participantAccess"]),
            "EventDetailView must receive or compute confirmed-attendee access before showing Phase 5 details to non-organizers."
        )
    }

    func testContentViewGuardsPhase5RoutesWithConfirmedAttendanceBeforeRenderingDetails() throws {
        let source = try readProjectFile("iosApp/src/Views/App/ContentView.swift")
        let homeContent = slice(
            source,
            from: "private var homeTabContent",
            to: "// MARK: - Tab Content"
        )

        let sensitiveCases = [
            "case .meetingList",
            "case .budgetOverview",
            "case .budgetDetail",
            "case .paymentPot",
            "case .tricount"
        ]
        let existingSensitiveCases = sensitiveCases.filter { homeContent.contains($0) }

        XCTAssertEqual(
            existingSensitiveCases.count,
            sensitiveCases.count,
            "ContentView must include meetings, budget, expenses, payment pot, and Tricount route cases."
        )
        XCTAssertTrue(
            containsAny(homeContent, ["isParticipantConfirmed(for: event)", "canAccessOrganizationDetails", "AccessDenied"]),
            "Phase 5 route cases must check organizer or confirmed-attendee access before rendering sensitive details."
        )
        XCTAssertFalse(
            homeContent.contains("MeetingListView(eventId: event.id)") &&
                !homeContent.contains("isParticipantConfirmed(for: event)"),
            "MeetingListView must not render directly for declined, pending, or non-participant users."
        )
        XCTAssertFalse(
            homeContent.contains("BudgetOverviewView(eventId: event.id)") &&
                !homeContent.contains("isParticipantConfirmed(for: event)"),
            "BudgetOverviewView must not render directly for declined, pending, or non-participant users."
        )
    }

    func testPhase5ViewsAndViewModelsExposeOfflineAndPendingSyncState() throws {
        let phase5Sources = try [
            "iosApp/src/Views/App/ContentView.swift",
            "iosApp/src/Views/Meeting/MeetingListView.swift",
            "iosApp/src/Views/Meeting/MeetingDetailView.swift",
            "iosApp/src/ViewModels/MeetingListViewModel.swift",
            "iosApp/src/ViewModels/MeetingDetailViewModel.swift",
            "iosApp/src/Views/Budget/BudgetOverviewView.swift",
            "iosApp/src/Views/Budget/BudgetDetailView.swift",
            "iosApp/src/ViewModels/BudgetViewModel.swift"
        ].map { try readProjectFile($0) }.joined(separator: "\n")

        XCTAssertTrue(
            containsAny(phase5Sources, ["pendingSync", "PendingSync", "selectPending()", "queued"]),
            "iOS Phase 5 screens must show when meetings, expenses, payments, or Tricount changes are pending sync."
        )
        XCTAssertTrue(
            containsAny(phase5Sources, ["offline", "Offline", "hors ligne", "isOnline", "connectivity"]),
            "iOS Phase 5 screens must show offline state for local-first organization actions."
        )
    }

    func testTricountLinksRequireSafeMetadataBeforeOpening() throws {
        let phase5Sources = try [
            "iosApp/src/Views/App/ContentView.swift",
            "iosApp/src/Views/Budget/BudgetOverviewView.swift",
            "iosApp/src/Views/Budget/BudgetDetailView.swift",
            "iosApp/src/ViewModels/BudgetViewModel.swift"
        ].map { try readProjectFile($0) }.joined(separator: "\n")

        XCTAssertTrue(
            phase5Sources.contains("Tricount"),
            "iOS Phase 5 UI must include a Tricount handoff surface before exposing external settlement links."
        )
        XCTAssertTrue(
            containsAny(
                phase5Sources,
                ["SafeExternalLink", "verificationStatus", "isVerified", "sanitize", "validatedURL", "canOpenURL"]
            ),
            "Tricount links must be validated or sanitized through safe link metadata before SwiftUI opens them."
        )
        XCTAssertFalse(
            phase5Sources.range(
                of: #"Link\s*\(\s*destination:\s*URL\s*\(\s*string:\s*[^)]*tricount"#,
                options: [.regularExpression, .caseInsensitive]
            ) != nil,
            "iOS must not create raw Tricount Link destinations without validation/sanitization."
        )
    }

    func testMeetingCreationUsesCurrentOrganizerContextAndHidesCreateForParticipants() throws {
        let viewModel = try readProjectFile("iosApp/src/ViewModels/MeetingListViewModel.swift")
        let view = try readProjectFile("iosApp/src/Views/Meeting/MeetingListView.swift")

        XCTAssertFalse(
            viewModel.contains("user-placeholder"),
            "iOS meeting creation must use the authenticated current user/organizer context, not a placeholder actor."
        )
        XCTAssertTrue(
            containsAny(
                viewModel,
                [
                    "init(eventId: String, currentUserId:",
                    "init(eventId: String, userId:",
                    "private let currentUserId",
                    "authStateManager.currentUser",
                    "TokenStorage"
                ]
            ),
            "MeetingListViewModel must receive or resolve the current user id before building CreateMeetingRequest.organizerId."
        )
        XCTAssertTrue(
            containsAny(
                view,
                [
                    "let isOrganizer",
                    "canCreateMeetings",
                    "event.organizerId == userId",
                    "showCreateMeetingAction"
                ]
            ),
            "MeetingListView must condition the create action on organizer access."
        )
        XCTAssertFalse(
            view.contains("showCreateSheet = true") &&
                !containsAny(view, ["isOrganizer", "canCreateMeetings", "showCreateMeetingAction"]),
            "iOS meeting creation UI must not expose the create sheet to confirmed non-organizer participants."
        )
    }

    func testPaymentPotAndTricountViewsExposeLifecycleActions() throws {
        let phase5Sources = try [
            "iosApp/src/Views/App/ContentView.swift",
            "iosApp/src/Views/Budget/BudgetOverviewView.swift",
            "iosApp/src/Views/Budget/BudgetDetailView.swift",
            "iosApp/src/ViewModels/BudgetViewModel.swift"
        ].map { try readProjectFile($0) }.joined(separator: "\n")

        XCTAssertTrue(
            containsAny(
                phase5Sources,
                [
                    "createPaymentPot",
                    "createPot(",
                    "onCreatePaymentPot",
                    "CreatePaymentPot",
                    "Créer une cagnotte",
                    "Creer une cagnotte"
                ]
            ),
            "iOS payment pot dashboard must expose a create lifecycle action for organizers."
        )
        XCTAssertTrue(
            containsAny(phase5Sources, ["PaymentPotView", "onOpenPayment", "OpenPaymentPot", "Ouvrir la cagnotte"]),
            "iOS payment pot dashboard must expose an open/view lifecycle action."
        )
        XCTAssertTrue(
            containsAny(phase5Sources, ["closePaymentPot", "closePot(", "onClosePaymentPot", "ClosePaymentPot", "Fermer la cagnotte"]),
            "iOS payment pot dashboard must expose a close lifecycle action for organizers."
        )
        XCTAssertTrue(
            containsAny(phase5Sources, ["linkTricount", "linkHandoff(", "onLinkTricount", "LinkTricount", "Associer Tricount"]),
            "iOS Tricount dashboard must expose a link lifecycle action."
        )
        XCTAssertTrue(
            containsAny(
                phase5Sources,
                [
                    "markTricountNotNeeded",
                    "markNotNeeded(",
                    "onMarkTricountNotNeeded",
                    "MarkTricountNotNeeded",
                    "Tricount not needed",
                    "Tricount non requis"
                ]
            ),
            "iOS Tricount dashboard must expose an explicit not-needed lifecycle action."
        )
    }

    func testMeetingRouteInjectsCurrentUserAndOrganizerGateIntoMeetingListView() throws {
        let content = try readProjectFile("iosApp/src/Views/App/ContentView.swift")
        let meetingView = try readProjectFile("iosApp/src/Views/Meeting/MeetingListView.swift")
        let meetingCase = slice(content, from: "case .meetingList:", to: "case .meetingDetail:")

        XCTAssertTrue(
            containsAny(
                meetingView,
                [
                    "init(eventId: String, currentUserId:",
                    "init(eventId: String, userId:",
                    "let currentUserId",
                    "let isOrganizer",
                    "canCreateMeetings"
                ]
            ),
            "MeetingListView must accept the current actor and organizer/create permission as explicit UI contract inputs."
        )
        XCTAssertFalse(
            meetingCase.contains("MeetingListView(eventId: event.id)") &&
                !containsAny(meetingCase, ["currentUserId: userId", "userId: userId", "isOrganizer:", "canCreateMeetings:"]),
            "ContentView must not route to MeetingListView without injecting current user and organizer/create permission."
        )
        XCTAssertTrue(
            containsAny(
                meetingView,
                [
                    "if isOrganizer",
                    "if canCreateMeetings",
                    ".disabled(!isOrganizer)",
                    ".disabled(!canCreateMeetings)"
                ]
            ),
            "MeetingListView toolbar and empty-state create controls must be hidden or disabled for non-organizers."
        )
    }

    func testPaymentPotViewExposesCreateActivateAndCloseLifecycleActions() throws {
        let content = try readProjectFile("iosApp/src/Views/App/ContentView.swift")
        let paymentPotView = slice(content, from: "private struct PaymentPotView: View", to: "private struct TricountHandoffView: View")

        XCTAssertTrue(
            containsAny(
                paymentPotView,
                [
                    "onCreatePaymentPot",
                    "createPaymentPot",
                    "CreatePaymentPot",
                    "Create pot",
                    "Créer une cagnotte"
                ]
            ),
            "iOS PaymentPotView must expose a create-pot lifecycle action, not only static local-first copy."
        )
        XCTAssertTrue(
            containsAny(
                paymentPotView,
                [
                    "onActivatePaymentPot",
                    "activatePaymentPot",
                    "ActivatePaymentPot",
                    "Activate pot",
                    "Activer la cagnotte"
                ]
            ),
            "iOS PaymentPotView must expose an activate-pot lifecycle action for settlement readiness."
        )
        XCTAssertTrue(
            containsAny(
                paymentPotView,
                [
                    "onClosePaymentPot",
                    "closePaymentPot",
                    "ClosePaymentPot",
                    "Close pot",
                    "Clôturer la cagnotte"
                ]
            ),
            "iOS PaymentPotView must expose a close-pot lifecycle action."
        )
    }

    func testTricountViewExposesLinkUnlinkAndUsesSafeUrlOpener() throws {
        let content = try readProjectFile("iosApp/src/Views/App/ContentView.swift")
        let tricountView = slice(content, from: "private struct TricountHandoffView: View", to: "private struct SafeExternalLink")

        XCTAssertTrue(
            containsAny(
                tricountView,
                ["onLinkTricount", "linkTricount", "LinkTricount", "Associer Tricount", "Link Tricount"]
            ),
            "iOS TricountHandoffView must expose a link/create handoff action."
        )
        XCTAssertTrue(
            containsAny(
                tricountView,
                ["onUnlinkTricount", "unlinkTricount", "UnlinkTricount", "Dissocier Tricount", "Unlink Tricount"]
            ),
            "iOS TricountHandoffView must expose an unlink lifecycle action."
        )
        XCTAssertTrue(
            containsAny(
                tricountView,
                ["openSafeURL", "OpenSafeURLAction", "SafeURLOpener", "onOpenSafeURL", "safeURLDispatcher"]
            ),
            "iOS TricountHandoffView must delegate external opening to a safe-url abstraction."
        )
        XCTAssertFalse(
            containsAny(tricountView, ["UIApplication.shared.open", "UIApplication.shared.canOpenURL"]),
            "iOS TricountHandoffView must not call UIApplication.shared.open/canOpenURL directly from SwiftUI."
        )
    }

    private func readProjectFile(_ relativePath: String) throws -> String {
        var directory = URL(fileURLWithPath: #filePath)
            .deletingLastPathComponent()

        while true {
            let candidate = directory.appendingPathComponent(relativePath)
            if FileManager.default.fileExists(atPath: candidate.path) {
                return try String(contentsOf: candidate, encoding: .utf8)
            }

            let parent = directory.deletingLastPathComponent()
            if parent.path == directory.path {
                throw NSError(
                    domain: "OrganizationPhase5ContractTests",
                    code: 1,
                    userInfo: [NSLocalizedDescriptionKey: "Could not find project file: \(relativePath)"]
                )
            }
            directory = parent
        }
    }

    private func slice(_ source: String, from startMarker: String, to endMarker: String) -> String {
        guard let startRange = source.range(of: startMarker) else {
            return source
        }
        let tail = source[startRange.lowerBound...]
        guard let endRange = tail.range(of: endMarker) else {
            return String(tail)
        }
        return String(tail[..<endRange.lowerBound])
    }

    private func containsAny(_ source: String, _ candidates: [String]) -> Bool {
        candidates.contains { source.contains($0) }
    }
}
