import XCTest
@testable import Wakeve

final class PremiumCreateEventContractTests: XCTestCase {
    func testCreateEventUsesLightweightFiveStepFlow() throws {
        let source = try readProjectFile("iosApp/src/Views/Events/CreateEventSheet.swift")
        let content = slice(source, from: "struct CreateEventSheet", to: "@ViewBuilder\n    private var gradientBackground")

        XCTAssertTrue(content.contains("@State private var currentStep: CreateEventStep = .name"))
        XCTAssertTrue(content.contains("createStepProgress"))
        XCTAssertTrue(content.contains("createStepContent"))
        XCTAssertTrue(content.contains("case .name"))
        XCTAssertTrue(content.contains("case .date"))
        XCTAssertTrue(content.contains("case .place"))
        XCTAssertTrue(content.contains("case .invite"))
        XCTAssertTrue(content.contains("case .confirm"))
    }

    func testCreateEventUsesPremiumControlsAndBottomAction() throws {
        let source = try readProjectFile("iosApp/src/Views/Events/CreateEventSheet.swift")
        let content = slice(source, from: "struct CreateEventSheet", to: "@ViewBuilder\n    private var gradientBackground")

        XCTAssertTrue(content.contains("LiquidGlassToolbar(title: String(localized: \"create_event.title\")"))
        XCTAssertTrue(content.contains("LiquidGlassCard("))
        XCTAssertTrue(content.contains("CreateEventChoiceRow("))
        XCTAssertTrue(content.contains("createBottomAction"))
        XCTAssertTrue(content.contains("LiquidGlassButton("))
        XCTAssertTrue(content.contains("advanceCreateStep"))
    }

    func testCreateEventPreservesCreationViewModelAndDateSlotContract() throws {
        let source = try readProjectFile("iosApp/src/Views/Events/CreateEventSheet.swift")

        XCTAssertTrue(source.contains("@StateObject private var viewModel = CreateEventViewModel()"))
        XCTAssertTrue(source.contains("viewModel.createEvent("))
        XCTAssertTrue(source.contains("@State private var proposedSlotDrafts: [EventSlotDraft] = []"))
        XCTAssertTrue(source.contains("selectedSlots: selectedSlotInputs()"))
        XCTAssertTrue(source.contains("proposedSlotDrafts.map"))
        XCTAssertTrue(source.contains("expectedParticipants.map { Int32($0) }"))
        XCTAssertTrue(source.contains("eventTypeDisplayName(selectedEventType)"))
    }

    func testCreateEventDateStepSupportsMultipleSlots() throws {
        let source = try readProjectFile("iosApp/src/Views/Events/CreateEventSheet.swift")
        let content = slice(source, from: "private var dateStep", to: "private var placeStep")

        XCTAssertTrue(content.contains("String(localized: \"create_event.add_slot\")"))
        XCTAssertTrue(content.contains("ForEach(proposedSlotDrafts)"))
        XCTAssertTrue(content.contains("CreateEventSlotRow("))
        XCTAssertTrue(source.contains("appendOrUpdateCurrentSlotDraft()"))
        XCTAssertTrue(source.contains("editProposedSlot("))
        XCTAssertTrue(source.contains("removeProposedSlot("))
    }

    func testCreateEventFinalStepOpensPreviewBeforeCreation() throws {
        let source = try readProjectFile("iosApp/src/Views/Events/CreateEventSheet.swift")
        let advance = slice(source, from: "private func advanceCreateStep()", to: "private func moveToPreviousStep()")

        XCTAssertTrue(source.contains("currentStep == .confirm ? String(localized: \"create_event.preview_action\") : String(localized: \"common.continue\")"))
        XCTAssertTrue(source.contains("currentStep == .confirm ? \"eye.fill\" : \"arrow.right\""))
        XCTAssertTrue(advance.contains("openPreviewIfValid()"))
        XCTAssertFalse(
            advance.contains("createEvent()"),
            "The wizard confirm action must open EventPreviewSheet; persistence belongs to the preview confirmation."
        )
    }

    func testCreateEventRequiresOnlyTitleForSimpleEventsWithDescriptionFallback() throws {
        let source = try readProjectFile("iosApp/src/Views/Events/CreateEventSheet.swift")
        let canCreate = slice(source, from: "private var canCreate", to: "private func darkenColor")
        let canAdvance = slice(source, from: "private var canAdvanceStep", to: "private func advanceCreateStep")

        XCTAssertTrue(canCreate.contains("hasRequiredEventText"))
        XCTAssertFalse(canCreate.contains("!proposedSlotDrafts.isEmpty"))
        XCTAssertTrue(canAdvance.contains("case .date:"))
        XCTAssertTrue(canAdvance.contains("return true"))
        XCTAssertTrue(source.contains("eventDescriptionForPersistence"))
        XCTAssertTrue(source.contains("String(localized: \"create_event.default_description\")"))
        XCTAssertTrue(source.contains("String(localized: \"create_event.date_later\")"))
        XCTAssertTrue(source.contains("String(localized: \"create_event.validation.title_required\")"))
    }

    func testCreateEventCarriesTemplateAndPotentialLocationContext() throws {
        let source = try readProjectFile("iosApp/src/Views/Events/CreateEventSheet.swift")

        XCTAssertTrue(source.contains("let initialScenario: EventScenario?"))
        XCTAssertTrue(source.contains("applyInitialScenarioIfNeeded()"))
        XCTAssertTrue(source.contains("@State private var appliedTemplateChecklist: [String]"))
        XCTAssertTrue(source.contains("appliedTemplateChecklist = initialScenario.checklistItems"))
        XCTAssertTrue(source.contains("String(localized: \"create_event.smart_plan.explore\")"))
        XCTAssertTrue(source.contains("String(localized: \"create_event.smart_plan.to_validate\")"))
        XCTAssertTrue(source.contains("EventCreationContext("))
        XCTAssertTrue(source.contains("potentialLocationName: selectedLocation?.nilIfEmpty"))
    }

    func testCreateEventVisibleDateFormattingUsesUserLocale() throws {
        let source = try readProjectFile("iosApp/src/Views/Events/CreateEventSheet.swift")
        let eventSlotDraft = slice(source, from: "private struct EventSlotDraft", to: "// MARK: - Date Time Picker Popup")
        let datePickerPopup = slice(source, from: "struct DateTimePickerPopup", to: "private struct EventPreviewSheet")
        let previewSheet = slice(source, from: "private struct EventPreviewSheet", to: "// MARK: - Rounded Corner Shape")

        XCTAssertTrue(eventSlotDraft.contains("formatter.locale = .autoupdatingCurrent"))
        XCTAssertTrue(eventSlotDraft.contains("formatter.timeStyle = .short"))
        XCTAssertTrue(datePickerPopup.contains("formatter.locale = .autoupdatingCurrent"))
        XCTAssertTrue(source.contains("formatter.timeStyle = .short"))
        XCTAssertTrue(previewSheet.contains("dateFormatter.locale = .autoupdatingCurrent"))
        XCTAssertFalse(eventSlotDraft.contains("Locale(identifier: \"fr_FR\")"), "Visible event slot dates should respect the user's locale.")
        XCTAssertFalse(datePickerPopup.contains("Locale(identifier: \"fr_FR\")"), "Date picker summaries should respect the user's locale.")
        XCTAssertFalse(previewSheet.contains("Locale(identifier: \"fr_FR\")"), "Invitation preview dates should respect the user's locale.")
    }

    func testInvitationPreviewRemainingOptionsCopyIsLocalized() throws {
        let source = try readProjectFile("iosApp/src/Views/Events/CreateEventSheet.swift")
        let previewSheet = slice(source, from: "struct EventPreviewSheet", to: "private var itineraryPreviewCard")

        XCTAssertTrue(previewSheet.contains("remainingProposedSlotsText"))
        XCTAssertTrue(previewSheet.contains("create_event.preview.more_options_singular_format"))
        XCTAssertTrue(previewSheet.contains("create_event.preview.more_options_plural_format"))
        XCTAssertFalse(previewSheet.contains("autre\\(proposedSlots.count"))

        for locale in ["en", "fr", "es", "it", "pt"] {
            let strings = try readProjectFile("iosApp/src/Resources/\(locale).lproj/Localizable.strings")
            XCTAssertTrue(strings.contains("\"create_event.preview.more_options_singular_format\""), "Missing singular preview options key for \(locale).")
            XCTAssertTrue(strings.contains("\"create_event.preview.more_options_plural_format\""), "Missing plural preview options key for \(locale).")
        }
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
