import XCTest
import Shared
@testable import Wakeve

/// Regression tests for IOS-4 (all-day slots) and the local-guest finalization dead end
/// found during the iOS QA tour of 2026-09-27 (docs/qa/IOS_QA_TOUR_2026-09-27.md).
final class AllDaySlotAndGuestFinalizeTests: XCTestCase {

    private let paris = "Europe/Paris"
    private let frenchLocale = Locale(identifier: "fr_FR")

    // MARK: - IOS-4: display

    func testAllDaySlotShowsAllDayLabelInsteadOfHours() {
        let text = TimeSlotDisplayFormatter.hoursText(
            start: "2026-10-16T22:00:00Z",
            end: "2026-10-17T22:00:00Z",
            timezone: paris,
            timeOfDay: .allDay,
            locale: frenchLocale
        )

        XCTAssertEqual(text, String(localized: "events.all_day"))
        XCTAssertFalse(text.contains("00:00"))
    }

    func testAllDaySlotCreatedWithStrayTimeStillHidesHours() {
        // Slots saved by the old wizard started at the creation time (e.g. 23:05).
        let text = TimeSlotDisplayFormatter.hoursText(
            start: "2026-10-17T21:05:00Z",
            end: "2026-10-17T22:05:00Z",
            timezone: paris,
            timeOfDay: .allDay,
            locale: frenchLocale
        )

        XCTAssertEqual(text, String(localized: "events.all_day"))
        XCTAssertFalse(text.contains("23:05"))
    }

    func testSpecificSlotShowsHoursInSlotTimezone() {
        let text = TimeSlotDisplayFormatter.hoursText(
            start: "2026-10-17T17:00:00Z",
            end: "2026-10-17T19:00:00Z",
            timezone: paris,
            timeOfDay: .specific,
            locale: frenchLocale
        )

        XCTAssertEqual(text, "19:00 - 21:00")
    }

    func testPollScreensAndAnnouncementUseTheSharedAllDayFormatting() {
        let voting = readProjectFileIfPresent("iosApp/src/Views/Polls/PollVotingView.swift")
        let results = readProjectFileIfPresent("iosApp/src/Views/Polls/PollResultsView.swift")
        let announcement = slice(results, from: "struct PollDecisionAnnouncementCard", to: "// MARK: - Resolution Next Steps Card")
        let contentView = readProjectFileIfPresent("iosApp/src/Views/App/ContentView.swift")
        let formatSlot = slice(contentView, from: "private func formatSlot(_ slot: TimeSlot)", to: "private func formatEventDate")

        for (name, source) in [("PollVotingView", voting), ("PollResultsView", results)] {
            XCTAssertFalse(
                source.contains("\\(formatTime(") ,
                "\(name) must not build a raw 'start - end' hours line that ignores ALL_DAY"
            )
            XCTAssertTrue(source.contains("TimeSlotDisplayFormatter.hoursText("), "\(name) must use the shared slot hours formatter")
        }
        XCTAssertTrue(announcement.contains("TimeSlotDisplayFormatter.isAllDay(slot.timeOfDay)"), "The shareable message must not invent hours for an all-day slot")
        XCTAssertTrue(formatSlot.contains("TimeSlotDisplayFormatter.isAllDay(slot.timeOfDay)"), "The event detail slot label must honour ALL_DAY")
    }

    // MARK: - IOS-4: creation wizard

    func testAllDaySlotSpansTheWholeLocalDay() throws {
        let calendar = Calendar.current
        let day = try XCTUnwrap(calendar.date(from: DateComponents(year: 2026, month: 10, day: 17, hour: 23, minute: 5)))
        let formatter = ISO8601DateFormatter()

        let input = EventSlotInputBuilder.input(
            startDate: day,
            startTime: day,
            isAllDay: true,
            hasEndTime: false,
            endTime: day,
            formatter: formatter,
            calendar: calendar
        )

        let start = try XCTUnwrap(formatter.date(from: input.start))
        let end = try XCTUnwrap(input.end.flatMap(formatter.date(from:)))
        XCTAssertEqual(input.timeOfDay, .allDay)
        XCTAssertEqual(start, calendar.startOfDay(for: day), "An all-day slot starts at the start of the local day, not at the creation time")
        XCTAssertEqual(end, calendar.date(byAdding: .day, value: 1, to: calendar.startOfDay(for: day)))
    }

    func testSpecificSlotKeepsItsTimeAndDefaultsToOneHour() throws {
        let calendar = Calendar.current
        let day = try XCTUnwrap(calendar.date(from: DateComponents(year: 2026, month: 10, day: 17)))
        let time = try XCTUnwrap(calendar.date(from: DateComponents(year: 2026, month: 1, day: 1, hour: 19, minute: 30)))
        let formatter = ISO8601DateFormatter()

        let input = EventSlotInputBuilder.input(
            startDate: day,
            startTime: time,
            isAllDay: false,
            hasEndTime: false,
            endTime: time,
            formatter: formatter,
            calendar: calendar
        )

        let start = try XCTUnwrap(formatter.date(from: input.start))
        let end = try XCTUnwrap(input.end.flatMap(formatter.date(from:)))
        XCTAssertEqual(input.timeOfDay, .specific)
        XCTAssertEqual(calendar.component(.hour, from: start), 19)
        XCTAssertEqual(calendar.component(.minute, from: start), 30)
        XCTAssertEqual(calendar.component(.day, from: start), 17)
        XCTAssertEqual(end.timeIntervalSince(start), 3600)
    }

    func testNewSpecificSlotDefaultsToAnEveningTimeInsteadOfNow() throws {
        let calendar = Calendar.current
        let now = try XCTUnwrap(calendar.date(from: DateComponents(year: 2026, month: 10, day: 17, hour: 23, minute: 5)))

        let defaultTime = EventSlotInputBuilder.defaultStartTime(on: now, calendar: calendar)

        XCTAssertEqual(calendar.component(.hour, from: defaultTime), 19)
        XCTAssertEqual(calendar.component(.minute, from: defaultTime), 0)
    }

    func testWizardUsesTheSlotBuilderAndATappableAllDayToggle() {
        let sheet = readProjectFileIfPresent("iosApp/src/Views/Events/CreateEventSheet.swift")
        let draft = slice(sheet, from: "private struct EventSlotDraft", to: "// MARK: - Date Time Picker Popup")
        let newSlot = slice(sheet, from: "private func prepareNewSlotDraft()", to: "private func editProposedSlot")
        let popup = slice(sheet, from: "struct DateTimePickerPopup", to: "private struct EventPreviewSheet")

        XCTAssertTrue(draft.contains("EventSlotInputBuilder.input("))
        XCTAssertTrue(newSlot.contains("EventSlotInputBuilder.defaultStartTime("))
        XCTAssertFalse(newSlot.contains("startTime = now"), "A new slot must not default to the current time")
        XCTAssertFalse(popup.contains(".frame(width: 48, height: 28)"), "The all-day switch must keep its full tap target")
    }

    // MARK: - Local guest cannot finalize: propose signing in

    func testGuestOrganizerIsInvitedToSignInBeforeFinalizing() {
        let contentView = readProjectFileIfPresent("iosApp/src/Views/App/ContentView.swift")
        let lifecycleCard = slice(contentView, from: "private var lifecycleCard: some View", to: "private var lifecycleBodyText")

        XCTAssertTrue(contentView.contains("isLocalGuestSession: authStateManager.isCurrentSessionGuest"))
        XCTAssertTrue(lifecycleCard.contains("showsGuestSignInPrompt"))
        XCTAssertTrue(contentView.contains("event.lifecycle.guest.body"))
        XCTAssertTrue(contentView.contains("onRequestSignIn"))
        XCTAssertTrue(contentView.contains("eventLifecycleGuestSignInAction"))
    }

    func testGuestLifecycleCopyIsLocalizedInEveryLanguage() {
        let keys = [
            "event.lifecycle.guest.title",
            "event.lifecycle.guest.body",
            "event.lifecycle.guest.action",
            "event.lifecycle.guest.confirm_message"
        ]
        for locale in ["fr", "en", "es", "it", "pt"] {
            let strings = readProjectFileIfPresent("iosApp/src/Resources/\(locale).lproj/Localizable.strings")
            for key in keys {
                XCTAssertTrue(strings.contains("\"\(key)\""), "\(locale) is missing \(key)")
            }
        }
    }

    // MARK: - Helpers

    private func readProjectFileIfPresent(_ relativePath: String) -> String {
        let fileURL = URL(fileURLWithPath: #filePath)
        let runtimeURL = URL(fileURLWithPath: FileManager.default.currentDirectoryPath)
        for startURL in [fileURL.deletingLastPathComponent(), runtimeURL] {
            var candidateRoot = startURL
            for _ in 0..<8 {
                let targetURL = candidateRoot.appendingPathComponent(relativePath)
                if let source = try? String(contentsOf: targetURL, encoding: .utf8) {
                    return source
                }
                let parentURL = candidateRoot.deletingLastPathComponent()
                guard parentURL.path != candidateRoot.path else { break }
                candidateRoot = parentURL
            }
        }
        return ""
    }

    private func slice(_ source: String, from start: String, to end: String) -> String {
        guard let startRange = source.range(of: start) else { return "" }
        let tail = source[startRange.upperBound...]
        guard let endRange = tail.range(of: end) else { return String(tail) }
        return String(tail[..<endRange.lowerBound])
    }
}
