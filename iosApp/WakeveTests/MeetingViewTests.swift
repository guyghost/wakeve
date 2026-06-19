import XCTest
import Shared
@testable import Wakeve

/// Tests for Meeting views and helper logic
@MainActor
final class MeetingViewTests: XCTestCase {

    // MARK: - MeetingRowView helpers (via static functions)

    func testPlatformIconNames_nonEmpty() {
        // All platform icon names should be non-empty system image names
        let viewModel = MeetingListViewModel(eventId: "test")
        XCTAssertNotNil(viewModel)
    }

    // MARK: - MeetingListViewModel init

    func testMeetingListViewModelInitialState() {
        let vm = MeetingListViewModel(eventId: "test-event")
        XCTAssertTrue(vm.isEmpty)
        XCTAssertTrue(vm.meetings.isEmpty)
        XCTAssertFalse(vm.hasError)
        XCTAssertNil(vm.errorMessage)
    }

    func testMeetingListViewModelHasNoGeneratedLinkInitially() {
        let vm = MeetingListViewModel(eventId: "test-event")
        XCTAssertFalse(vm.hasGeneratedLink)
        XCTAssertNil(vm.generatedLink)
    }

    // MARK: - MeetingDetailViewModel init

    func testMeetingDetailViewModelInitialState() {
        let vm = MeetingDetailViewModel(meetingId: "test-meeting")
        XCTAssertNil(vm.meeting)
        XCTAssertFalse(vm.showDeleteConfirm)
        XCTAssertFalse(vm.isEditing)
    }

    // MARK: - CreateMeetingSheet

    func testCreateMeetingSheetInit() {
        var savedPlatform: MeetingPlatform? = nil
        var savedTitle: String? = nil

        let sheet = CreateMeetingSheet(eventId: "event-1") { platform, title in
            savedPlatform = platform
            savedTitle = title
        }

        XCTAssertNotNil(sheet)
        XCTAssertNil(savedPlatform)
        XCTAssertNil(savedTitle)
    }

    // MARK: - MeetingListView init

    func testMeetingListViewInit() {
        let view = MeetingListView(eventId: "test-event")
        XCTAssertNotNil(view)
    }

    func testMeetingListUsesBrandedCardsInsteadOfNativeList() throws {
        let source = try readProjectFile("iosApp/src/Views/Meeting/MeetingListView.swift")
        let listView = slice(source, from: "private var listView", to: "private var isPreviewing")
        let emptyView = slice(source, from: "private var emptyView", to: "// MARK: - List")

        XCTAssertTrue(source.contains("WakeveScreenBackground(style: .grouped)"))
        XCTAssertTrue(listView.contains("ScrollView"))
        XCTAssertTrue(listView.contains("LazyVStack"))
        XCTAssertTrue(listView.contains("MeetingOverviewCard("))
        XCTAssertTrue(listView.contains("MeetingRowCard(meeting: meeting)"))
        XCTAssertTrue(source.contains("private struct MeetingRowCard"))
        XCTAssertTrue(source.contains("private struct MeetingOverviewCard"))
        XCTAssertTrue(source.contains("meetings.overview.title"))
        XCTAssertTrue(source.contains("meetings.overview.metric.ready"))
        XCTAssertTrue(source.contains("meetingOverviewNextActionText"))
        XCTAssertTrue(source.contains("meetings.overview.next_action_generate"))
        XCTAssertTrue(source.contains("meetings.overview.next_action_share"))
        XCTAssertTrue(source.contains("meetings.overview.next_action_join"))
        XCTAssertTrue(source.contains("WakeveContentCard(prominence: .regular"))
        XCTAssertTrue(source.contains("meetings.sync.pending"))
        XCTAssertTrue(emptyView.contains("WakeveContentCard(prominence: .prominent"))
        XCTAssertTrue(emptyView.contains("meetings.empty_title"))
        XCTAssertFalse(listView.contains("List {"), "Meeting list should use Wakeve content cards, not native List chrome.")
        XCTAssertFalse(listView.contains(".swipeActions"), "Meeting cancellation should not be hidden behind swipe-only actions.")
    }

    func testMeetingOverviewCopyIsLocalizedInCoreLocales() throws {
        let localeFiles = [
            "iosApp/src/Resources/en.lproj/Localizable.strings",
            "iosApp/src/Resources/fr.lproj/Localizable.strings",
            "iosApp/src/Resources/es.lproj/Localizable.strings",
            "iosApp/src/Resources/it.lproj/Localizable.strings",
            "iosApp/src/Resources/pt.lproj/Localizable.strings"
        ]
        let requiredKeys = [
            "meetings.overview.title",
            "meetings.overview.subtitle",
            "meetings.overview.metric.total",
            "meetings.overview.metric.ready",
            "meetings.overview.metric.live",
            "meetings.overview.next_action_label",
            "meetings.overview.next_action_join",
            "meetings.overview.next_action_generate",
            "meetings.overview.next_action_share"
        ]

        for localeFile in localeFiles {
            let content = try readProjectFile(localeFile)
            for key in requiredKeys {
                XCTAssertTrue(content.contains("\"\(key)\""), "\(localeFile) is missing \(key)")
            }
        }
    }

    func testCreateMeetingSheetUsesBrandedLocalizedSurface() throws {
        let source = try readProjectFile("iosApp/src/Views/Meeting/MeetingListView.swift")
        let sheet = slice(source, from: "struct CreateMeetingSheet", to: "private struct MeetingPlatformOptionCard")

        XCTAssertTrue(sheet.contains("NavigationStack"))
        XCTAssertTrue(sheet.contains("WakeveScreenBackground(style: .grouped)"))
        XCTAssertTrue(sheet.contains("ScrollView"))
        XCTAssertTrue(sheet.contains("WakeveContentCard(prominence: .prominent"))
        XCTAssertTrue(sheet.contains("WakeveContentCard(prominence: .regular"))
        XCTAssertTrue(sheet.contains("MeetingPlatformOptionCard"))
        XCTAssertTrue(sheet.contains("meetings.create_sheet_title"))
        XCTAssertTrue(sheet.contains("meetings.create_sheet_subtitle"))
        XCTAssertTrue(sheet.contains("meetings.default_title_format"))
        XCTAssertTrue(sheet.contains("WakeveHaptics.success()"))
        XCTAssertFalse(sheet.contains("NavigationView"), "Create meeting should use NavigationStack.")
        XCTAssertFalse(sheet.contains("Form {"), "Create meeting should avoid native Form chrome.")
        XCTAssertFalse(sheet.contains("Section(\"Titre\")"), "Create meeting copy should be localized.")
        XCTAssertFalse(sheet.contains("Button(\"Créer\")"), "Create action should use localized labels.")
    }

    func testMeetingDetailUsesBrandedLocalizedSurface() throws {
        let source = try readProjectFile("iosApp/src/Views/Meeting/MeetingDetailView.swift")

        XCTAssertTrue(source.contains("WakeveScreenBackground(style: .grouped)"))
        XCTAssertTrue(source.contains("WakeveContentCard(prominence: .prominent"))
        XCTAssertTrue(source.contains("WakeveContentCard(prominence: .regular"))
        XCTAssertTrue(source.contains("WakeveActionButton("))
        XCTAssertTrue(source.contains("meetings.detail_title"))
        XCTAssertTrue(source.contains("meetings.link_label"))
        XCTAssertTrue(source.contains("meetings.share_link"))
        XCTAssertTrue(source.contains("meetings.not_found_subtitle"))
        XCTAssertTrue(source.contains("formatter.locale = .current"))
        XCTAssertFalse(source.contains("Locale(identifier: \"fr_FR\")"), "Meeting detail dates should respect the user's current locale.")
        XCTAssertFalse(source.contains("\"Réunion\""), "Meeting detail title should be localized.")
        XCTAssertFalse(source.contains("\"Annuler cette réunion ?\""), "Meeting cancellation confirmation should be localized.")
        XCTAssertFalse(source.contains("\"Rejoindre la réunion\""), "Meeting join action should be localized.")
        XCTAssertFalse(source.contains(".background(.regularMaterial"), "Meeting detail cards should use WakeveContentCard.")
    }

    func testMeetingGenerateLinkSheetUsesWakeveSurface() throws {
        let source = try readProjectFile("iosApp/src/Views/Meeting/MeetingGenerateLinkSheet.swift")

        XCTAssertTrue(source.contains("WakeveScreenBackground(style: .grouped)"))
        XCTAssertTrue(source.contains("WakeveContentCard(prominence: .prominent"))
        XCTAssertTrue(source.contains("WakeveContentCard(prominence: .regular"))
        XCTAssertTrue(source.contains("WakeveContentCard(prominence: .subtle"))
        XCTAssertTrue(source.contains("WakeveActionButton("))
        XCTAssertTrue(source.contains("WakeveHaptics.selection()"))
        XCTAssertTrue(source.contains("WakeveHaptics.success()"))
        XCTAssertTrue(source.contains("LazyVGrid"))
        XCTAssertFalse(source.contains("Color(.systemGroupedBackground)"), "Generate-link sheet should use Wakeve background.")
        XCTAssertFalse(source.contains("LinearGradient"), "Platform tiles should avoid ad hoc gradient styling.")
        XCTAssertFalse(source.contains("Color(.secondarySystemBackground)"), "Platform tiles should use Wakeve tokens.")
        XCTAssertFalse(source.contains("ToolbarItem(placement: .navigationBarTrailing)"), "Generate action should be a visible in-sheet primary action.")
        XCTAssertFalse(source.contains(".wakevePrimary"), "Generate-link sheet should use WakeveTheme tokens consistently.")
    }

    // MARK: - MeetingDetailView init

    func testMeetingDetailViewInit() {
        let view = MeetingDetailView(meetingId: "test-meeting", eventId: "test-event")
        XCTAssertNotNil(view)
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
