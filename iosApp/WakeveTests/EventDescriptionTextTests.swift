import XCTest
import Shared
@testable import Wakeve

/// Regression tests for the legacy home event card showing a raw Kotlin dump
/// ("Event(id=…, title=…") instead of the event description.
///
/// Kotlin's `description` property is bridged to Swift as `description_`; `event.description`
/// resolves to NSObject's `description`, i.e. the data class `toString()`.
final class EventDescriptionTextTests: XCTestCase {

    func testDisplayTextIsTheEventDescriptionNotTheKotlinDump() {
        let event = makeEvent(description: "Week-end entre amis, départ vendredi soir")

        let text = EventDescriptionText.displayText(for: event)

        XCTAssertEqual(text, "Week-end entre amis, départ vendredi soir")
        XCTAssertFalse(text?.hasPrefix("Event(") ?? false)
    }

    func testDisplayTextKeepsDescriptionsContainingKotlinLikeSeparators() {
        let event = makeEvent(description: "Budget serré, organizerId=moi, on verra")

        XCTAssertEqual(EventDescriptionText.displayText(for: event), "Budget serré, organizerId=moi, on verra")
    }

    func testDisplayTextIsNilForEmptyOrBlankDescription() {
        XCTAssertNil(EventDescriptionText.displayText(for: makeEvent(description: "")))
        XCTAssertNil(EventDescriptionText.displayText(for: makeEvent(description: "  \n ")))
    }

    func testEventViewsNeverReadNSObjectDescriptionOfAKotlinEvent() {
        let contentView = readProjectFileIfPresent("iosApp/src/Views/App/ContentView.swift")
        XCTAssertFalse(contentView.isEmpty)

        let eventCard = slice(contentView, from: "struct EventCard: View", to: "struct EventDetailView")
        XCTAssertTrue(eventCard.contains("EventDescriptionText.displayText(for: event)"))
        XCTAssertFalse(eventCard.contains("event.description."))
        XCTAssertFalse(eventCard.contains("Text(event.description)"))

        XCTAssertFalse(contentView.contains("raw.hasPrefix(\"Event(\")"), "Do not parse the Kotlin toString() dump")
        XCTAssertFalse(contentView.contains("event.description.trimmingCharacters"))
    }

    // MARK: - Helpers

    private func makeEvent(description: String) -> WakeveEvent {
        WakeveEvent(
            id: "event-1790543722343",
            title: "Road trip Lisbonne",
            description: description,
            organizerId: "organizer-1",
            participants: [],
            proposedSlots: [],
            deadline: "2026-10-20T10:00:00Z",
            status: .draft,
            finalDate: nil,
            createdAt: "2026-10-01T10:00:00Z",
            updatedAt: "2026-10-01T10:00:00Z",
            eventType: .other,
            eventTypeCustom: nil,
            minParticipants: nil,
            maxParticipants: nil,
            expectedParticipants: nil,
            heroImageUrl: nil,
            planningMode: .timeSlotPoll,
            aggregateRevision: 1,
            aggregateSchemaVersion: 1
        )
    }

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
