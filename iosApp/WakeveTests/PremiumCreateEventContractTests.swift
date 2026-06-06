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

        XCTAssertTrue(content.contains("LiquidGlassToolbar(title: \"Nouvel événement\""))
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

        XCTAssertTrue(content.contains("Ajouter un créneau"))
        XCTAssertTrue(content.contains("ForEach(proposedSlotDrafts)"))
        XCTAssertTrue(content.contains("CreateEventSlotRow("))
        XCTAssertTrue(source.contains("appendOrUpdateCurrentSlotDraft()"))
        XCTAssertTrue(source.contains("editProposedSlot("))
        XCTAssertTrue(source.contains("removeProposedSlot("))
    }

    func testCreateEventFinalStepOpensPreviewBeforeCreation() throws {
        let source = try readProjectFile("iosApp/src/Views/Events/CreateEventSheet.swift")
        let advance = slice(source, from: "private func advanceCreateStep()", to: "private func moveToPreviousStep()")

        XCTAssertTrue(source.contains("currentStep == .confirm ? \"Voir l’aperçu\" : \"Continuer\""))
        XCTAssertTrue(source.contains("currentStep == .confirm ? \"eye.fill\" : \"arrow.right\""))
        XCTAssertTrue(advance.contains("openPreviewIfValid()"))
        XCTAssertFalse(
            advance.contains("createEvent()"),
            "The wizard confirm action must open EventPreviewSheet; persistence belongs to the preview confirmation."
        )
    }

    func testCreateEventRequiresDescriptionAndAtLeastOneSlot() throws {
        let source = try readProjectFile("iosApp/src/Views/Events/CreateEventSheet.swift")
        let canCreate = slice(source, from: "private var canCreate", to: "private func darkenColor")

        XCTAssertTrue(canCreate.contains("hasRequiredEventText"))
        XCTAssertTrue(canCreate.contains("!proposedSlotDrafts.isEmpty"))
        XCTAssertTrue(source.contains("Le titre et la description sont requis"))
        XCTAssertTrue(source.contains("Ajoutez au moins un créneau"))
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
