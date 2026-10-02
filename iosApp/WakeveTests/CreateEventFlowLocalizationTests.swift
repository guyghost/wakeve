import XCTest
@testable import Wakeve

/// Textes du flux de création (couche 7, #47) : présents dans les 5 langues, français au tutoiement.
final class CreateEventFlowLocalizationTests: XCTestCase {
    static let stringKeys = [
        "create_flow.title",
        "create_flow.question.what", "create_flow.question.who",
        "create_flow.question.place", "create_flow.question.time",
        "create_flow.subtitle.what", "create_flow.subtitle.who",
        "create_flow.subtitle.place", "create_flow.subtitle.time",
        "create_flow.field.title", "create_flow.field.title_placeholder",
        "create_flow.field.description", "create_flow.field.description_placeholder",
        "create_flow.field.type", "create_flow.field.custom_type", "create_flow.field.templates",
        "create_flow.field.min", "create_flow.field.expected", "create_flow.field.max",
        "create_flow.field.count_unset", "create_flow.field.location_placeholder",
        "create_flow.type.other", "create_flow.invite_hint",
        "create_flow.draft_saved", "create_flow.continue", "create_flow.skip",
        "create_flow.add_location", "create_flow.search_location", "create_flow.add_slot",
        "create_flow.slot.editor_title", "create_flow.slot.day", "create_flow.slot.start", "create_flow.slot.end",
        "create_flow.moment.all_day", "create_flow.moment.morning", "create_flow.moment.afternoon",
        "create_flow.moment.evening", "create_flow.moment.specific",
        "create_flow.empty.locations", "create_flow.empty.slots",
        "create_flow.error.description_required", "create_flow.error.custom_type_required",
        "create_flow.error.participants_positive", "create_flow.error.max_less_than_min",
        "create_flow.error.location_empty", "create_flow.error.location_duplicate",
        "create_flow.error.slot_end_before_start", "create_flow.error.slot_time_required",
        "create_flow.error.save_failed", "create_flow.slot.date_unset",
        "create_flow.a11y.progress_format", "create_flow.a11y.remove_location_format",
        "create_flow.a11y.remove_slot", "create_flow.a11y.error_format"
    ]

    /// Clés existantes réutilisées par le flux.
    static let reusedKeys = [
        "create_event.validation.title_required", "create_event.validation.slot_required",
        "participants.start_poll.action", "participants.start_poll.requires_slot",
        "events.all_day", "common.close", "common.back", "common.cancel", "common.add"
    ]

    private var resources: URL {
        URL(fileURLWithPath: #filePath).deletingLastPathComponent().deletingLastPathComponent()
            .appendingPathComponent("src/Resources")
    }

    func testCreateFlowKeysExistInEveryLanguage() throws {
        for locale in ["en", "fr", "es", "it", "pt"] {
            let strings = try String(contentsOf: resources.appendingPathComponent("\(locale).lproj/Localizable.strings"), encoding: .utf8)
            for key in Self.stringKeys + Self.reusedKeys {
                XCTAssertTrue(strings.contains("\"\(key)\" ="), "\(key) manquante (\(locale))")
            }
        }
    }

    func testModelErrorKeysAreAllTranslated() throws {
        let strings = try String(contentsOf: resources.appendingPathComponent("fr.lproj/Localizable.strings"), encoding: .utf8)
        let keys = ["create_flow.error.description_required", "create_flow.error.custom_type_required",
                    "create_flow.error.participants_positive", "create_flow.error.max_less_than_min",
                    "create_flow.error.location_empty", "create_flow.error.location_duplicate",
                    "create_flow.error.slot_end_before_start", "create_flow.error.slot_time_required"]
            + CreateEventMoment.allCases.map(\.titleKey)
        for key in keys {
            XCTAssertTrue(strings.contains("\"\(key)\" ="), key)
        }
    }

    func testFrenchCopyAsksTheFourQuestionsWithTutoiement() {
        let fr = Locale(identifier: "fr")
        XCTAssertEqual(WK.localizedFormat("create_flow.question.what", locale: fr), "C'est quoi, ton événement ?")
        XCTAssertEqual(WK.localizedFormat("create_flow.question.who", locale: fr), "Vous serez combien ?")
        XCTAssertEqual(WK.localizedFormat("create_flow.question.place", locale: fr), "Où ça pourrait se passer ?")
        XCTAssertEqual(WK.localizedFormat("create_flow.question.time", locale: fr), "Quand est-ce que ça pourrait se passer ?")
        XCTAssertEqual(WK.localizedFormat("create_flow.draft_saved", locale: fr), "Brouillon enregistré")
        XCTAssertEqual(WK.localizedFormat("create_flow.invite_hint", locale: fr), "Tu inviteras ton groupe juste après le lancement.")
        XCTAssertEqual(WK.localizedFormat("create_flow.moment.all_day", locale: fr), "Journée")
        XCTAssertEqual(WK.localizedFormat("create_flow.moment.specific", locale: fr), "Heure précise")
        XCTAssertEqual(
            String(format: WK.localizedFormat("create_flow.a11y.progress_format", locale: fr), locale: fr, 2, 4),
            "Étape 2 sur 4"
        )
        XCTAssertEqual(WK.localizedFormat("create_flow.question.what", locale: Locale(identifier: "en")), "What's your event?")
    }

    func testFrenchCreateFlowCopyNeverUsesVous() throws {
        let strings = try String(contentsOf: resources.appendingPathComponent("fr.lproj/Localizable.strings"), encoding: .utf8)
        for line in strings.split(separator: "\n") where line.hasPrefix("\"create_flow.") {
            // « Vous serez combien ? » parle du groupe (dont l'organisateur), pas d'une formule de politesse.
            if line.hasPrefix("\"create_flow.question.who\"") { continue }
            let value = line.split(separator: "=", maxSplits: 1).last.map(String.init) ?? ""
            for word in [" vous ", "Vous ", " votre ", "Votre ", " vos ", "Vos "] {
                XCTAssertFalse(value.contains(word), "Tutoiement attendu : \(line)")
            }
        }
    }
}
