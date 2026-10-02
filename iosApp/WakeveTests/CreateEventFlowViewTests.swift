import XCTest
import SwiftUI
import Shared
@testable import Wakeve

/// Vues du flux de création en 4 questions (couche 7, #47) : rendu par étape, erreurs visibles et
/// annoncées, AX5 sans débordement, cibles ≥ 44 pt.
@MainActor
final class CreateEventFlowViewTests: XCTestCase {
    private let fr = Locale(identifier: "fr")
    private let iso = ISO8601DateFormatter()

    private func filledForm() -> CreateEventForm {
        var form = CreateEventForm()
        form.title = "Week-end raclette à Chamonix avec toute la bande"
        form.description = "On loue un grand chalet, chacun apporte quelque chose"
        form.eventTypeName = CreateEventForm.customTypeName
        form.eventTypeCustom = "Crémaillère"
        form.minParticipants = 4
        form.expectedParticipants = 8
        form.locations = ["Chamonix-Mont-Blanc", "Annecy"]
        form.slots = [
            CreateEventSlot(id: "a", input: EventTimeSlotInput(start: "2026-10-10T17:00:00Z", end: "2026-10-10T21:00:00Z", timeOfDay: .evening)),
            CreateEventSlot(id: "b", input: EventTimeSlotInput(start: "2026-10-11T18:00:00Z", end: "2026-10-11T17:00:00Z", timeOfDay: .specific))
        ]
        return form
    }

    private func screen(step: CreateEventFlowStep, form: CreateEventForm, showErrors: Bool = false, banner: String? = nil) -> some View {
        CreateEventFlowScreen(
            form: .constant(form),
            step: step,
            showErrors: showErrors,
            isDraftSaved: true,
            isSaving: false,
            bannerMessage: banner,
            actions: CreateEventFlowScreen.Actions()
        )
    }

    private func render<V: View>(_ view: V, size: DynamicTypeSize, width: CGFloat = 390) -> CGSize {
        let host = UIHostingController(rootView: view.environment(\.dynamicTypeSize, size))
        return host.sizeThatFits(in: CGSize(width: width, height: CGFloat.greatestFiniteMagnitude))
    }

    // MARK: - Rendu par étape

    func testEveryStepRendersWithinTheWidthAtAX5() {
        for step in CreateEventFlowStep.allCases {
            let regular = render(screen(step: step, form: filledForm()), size: .large)
            let ax5 = render(screen(step: step, form: filledForm()), size: .accessibility5)
            XCTAssertGreaterThan(regular.height, 0, "\(step)")
            XCTAssertLessThanOrEqual(ax5.width, 390, "Pas de débordement horizontal en AX5 (\(step)).")
            XCTAssertGreaterThan(ax5.height, regular.height, "Le texte grandit au lieu d'être rogné (\(step)).")
        }
    }

    func testEmptyStepsRender() {
        for step in CreateEventFlowStep.allCases {
            XCTAssertGreaterThan(render(screen(step: step, form: CreateEventForm()), size: .large).height, 0)
        }
    }

    // MARK: - Erreurs

    func testErrorsAreShownOnlyAfterAContinueAttempt() {
        for step in CreateEventFlowStep.allCases {
            var form = filledForm()
            switch step {
            case .what: form.title = ""
            case .who: form.maxParticipants = 2
            case .place: form.locations = ["Annecy", "annecy"]
            case .time: break // créneau « b » : fin avant le début
            }
            let hidden = render(screen(step: step, form: form, showErrors: false), size: .large).height
            let shown = render(screen(step: step, form: form, showErrors: true), size: .large).height
            XCTAssertGreaterThan(shown, hidden, "L'erreur s'affiche sous le champ (\(step)).")
        }
    }

    func testBannerIsRendered() {
        let without = render(screen(step: .time, form: filledForm()), size: .large).height
        let with = render(screen(step: .time, form: filledForm(), banner: "Le brouillon n'a pas pu être enregistré."), size: .large).height
        XCTAssertGreaterThan(with, without)
    }

    func testErrorTextIsReadWithItsPrefix() {
        XCTAssertEqual(CreateFlowFieldError.accessibilityText("Ajoute un titre", locale: fr), "Erreur : Ajoute un titre")
        XCTAssertEqual(CreateFlowFieldError.accessibilityText("Add a title", locale: Locale(identifier: "en")), "Error: Add a title")
    }

    func testAnnouncementListsTheVisibleErrorsInFieldOrder() {
        var form = CreateEventForm()
        form.eventTypeName = CreateEventForm.customTypeName
        let text = CreateEventFlowScreen.announcement(for: form.errors(for: .what), locale: fr)
        XCTAssertEqual(text, [
            WK.localizedFormat("create_event.validation.title_required", locale: fr),
            WK.localizedFormat("create_flow.error.description_required", locale: fr),
            WK.localizedFormat("create_flow.error.custom_type_required", locale: fr)
        ].joined(separator: " "))
    }

    // MARK: - Textes

    func testQuestionsAndPrimaryTitles() {
        XCTAssertEqual(CreateEventFlowScreen.question(for: .what, locale: fr), "C'est quoi, ton événement ?")
        XCTAssertEqual(CreateEventFlowScreen.question(for: .time, locale: fr), "Quand est-ce que ça pourrait se passer ?")
        XCTAssertEqual(CreateEventFlowScreen.primaryTitle(for: .who, locale: fr), "Continuer")
        XCTAssertEqual(CreateEventFlowScreen.primaryTitle(for: .time, locale: fr), "Lancer le sondage")
        XCTAssertEqual(CreateEventFlowScreen.progressLabel(for: .place, locale: fr), "Étape 3 sur 4")
    }

    func testSlotTextsUseTheMomentOrTheHours() {
        let evening = CreateEventSlot(id: "e", input: EventTimeSlotInput(start: "2026-10-10T17:00:00Z", end: "2026-10-10T21:00:00Z", timeOfDay: .evening))
        XCTAssertEqual(CreateFlowSlotRow.detail(for: evening, locale: fr), "Soir")
        let allDay = CreateEventSlot(id: "d", input: EventTimeSlotInput(start: "2026-10-10T00:00:00Z", end: "2026-10-11T00:00:00Z", timeOfDay: .allDay))
        XCTAssertEqual(CreateFlowSlotRow.detail(for: allDay, locale: fr), "Journée")
        let specific = CreateEventSlot(id: "s", input: EventTimeSlotInput(start: "2026-10-10T17:00:00Z", end: "2026-10-10T21:00:00Z", timeOfDay: .specific))
        XCTAssertTrue(CreateFlowSlotRow.detail(for: specific, locale: fr).contains(" - "), "Heure précise : début - fin")
        XCTAssertFalse(CreateFlowSlotRow.title(for: evening, locale: fr).isEmpty)
    }

    func testDatelessSlotSaysTheDateIsUnset() {
        let flex = CreateEventSlot(id: "f", input: EventTimeSlotInput(start: "", end: nil, timeOfDay: .evening))
        XCTAssertEqual(CreateFlowSlotRow.title(for: flex, locale: fr), "Date à définir")
        XCTAssertEqual(CreateFlowSlotRow.detail(for: flex, locale: fr), "Soir")
    }

    // MARK: - Cibles tactiles

    func testRemoveButtonsKeepMinimumTapTarget() {
        let button = CreateFlowRemoveButton(accessibilityLabel: "Retirer", accessibilityID: "x", action: {})
        XCTAssertGreaterThanOrEqual(render(button, size: .xSmall, width: 200).height, WK.Size.minTapTarget)
        XCTAssertGreaterThanOrEqual(render(button, size: .xSmall, width: 200).width, WK.Size.minTapTarget)
        XCTAssertGreaterThanOrEqual(render(button, size: .accessibility5, width: 200).height, WK.Size.minTapTarget)
    }

    func testRowsKeepMinimumTapTargetAtAX5() {
        let slot = filledForm().slots[0]
        let row = CreateFlowSlotRow(slot: slot, index: 0, error: nil, onRemove: {})
        XCTAssertGreaterThanOrEqual(render(row, size: .xSmall, width: 358).height, WK.Size.minTapTarget)
        XCTAssertLessThanOrEqual(render(row, size: .accessibility5, width: 358).width, 358)
        let location = CreateFlowLocationRow(name: "Annecy", index: 0, error: "Doublon", onRemove: {})
        XCTAssertGreaterThanOrEqual(render(location, size: .xSmall, width: 358).height, WK.Size.minTapTarget)
    }

    // MARK: - AX5 au simulateur (correctifs)

    func testAccessibilitySizesKeepRoomForTheQuestion() throws {
        let flow = try source("src/Views/Create/CreateEventFlow.swift")
        XCTAssertTrue(flow.contains(".dynamicTypeSize(...DynamicTypeSize.accessibility1)"),
                      "« Brouillon enregistré » plafonné en AX5.")
        XCTAssertTrue(flow.contains("!dynamicTypeSize.isAccessibilitySize ? \"paperplane.fill\""),
                      "Pas d'icône dans « Lancer le sondage » aux tailles d'accessibilité.")
        XCTAssertTrue(flow.contains(".background(WK.Colors.canvas)\n    }\n\n    private var footer"),
                      "L'en-tête est opaque : le contenu ne défile pas sous la progression.")
        let steps = try source("src/Views/Create/CreateEventFlowSteps.swift")
        XCTAssertFalse(steps.contains(".prominent"), "Une seule action principale par écran (WKPrimaryButton).")
    }

    // MARK: - Robustesse (revue)

    func testCloseAndBackAreDisabledWhileBusyAndLaunchIsIgnoredAfterClose() throws {
        let flow = try source("src/Views/Create/CreateEventFlow.swift")
        XCTAssertGreaterThanOrEqual(flow.components(separatedBy: ".disabled(isSaving)").count - 1, 2,
                                    "Fermer et Retour inactifs pendant un enregistrement ou un lancement.")
        XCTAssertTrue(flow.contains("guard !isBusy"), "Les actions ignorent un appui pendant le travail en cours.")
        XCTAssertTrue(flow.contains("guard !didClose"), "Un lancement terminé après la fermeture n'ouvre rien.")
        XCTAssertTrue(flow.contains("form.savesOnClose(step: step, hasDraft: controller.eventId != nil)"))
        XCTAssertTrue(flow.contains("isDraftSaved: controller.isDraftSaved"),
                      "« Brouillon enregistré » suit le dernier enregistrement.")
    }

    // MARK: - Contrat de la vue

    private func source(_ path: String) throws -> String {
        try String(contentsOf: URL(fileURLWithPath: #filePath).deletingLastPathComponent()
            .deletingLastPathComponent().appendingPathComponent(path), encoding: .utf8)
    }

    func testViewUsesWKComponentsAndAccessibilityContract() throws {
        let flow = try source("src/Views/Create/CreateEventFlow.swift")
        XCTAssertTrue(flow.contains("struct CreateEventFlow: View"))
        XCTAssertTrue(flow.contains("userId: String"))
        XCTAssertTrue(flow.contains("draftEventId: String?"))
        XCTAssertTrue(flow.contains("initialScenario: EventScenario?"))
        XCTAssertTrue(flow.contains("onClose: () -> Void"))
        XCTAssertTrue(flow.contains("onLaunched: (Event, EventCreationContext) -> Void"))
        XCTAssertTrue(flow.contains("EventDraftFlowController"))
        XCTAssertTrue(flow.contains("WKCircleButton("))
        XCTAssertTrue(flow.contains("WKPrimaryButton("))
        XCTAssertTrue(flow.contains("WK.Typo.title"))
        XCTAssertTrue(flow.contains(".accessibilityAddTraits(.isHeader)"))
        XCTAssertTrue(flow.contains("AccessibilityNotification.Announcement"), "Les erreurs sont annoncées à VoiceOver.")
        XCTAssertTrue(flow.contains("\"create_flow.continue\""))
        XCTAssertTrue(flow.contains("\"create_flow.close\""))

        let steps = try source("src/Views/Create/CreateEventFlowSteps.swift")
        XCTAssertTrue(steps.contains("WKChip("))
        XCTAssertTrue(steps.contains("EventScenario.allScenarios"))
        XCTAssertTrue(steps.contains("LocationSelectionSheet("))
        XCTAssertTrue(steps.contains("Stepper("))
        for text in [flow, steps] {
            XCTAssertFalse(text.contains("Color(hex:"))
            XCTAssertFalse(text.contains(".font(.system(size:"))
            XCTAssertFalse(text.contains("WakeveTheme"), "Tokens WK uniquement.")
        }
    }
}
