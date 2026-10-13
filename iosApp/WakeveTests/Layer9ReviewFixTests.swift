import XCTest
@testable import Wakeve

/// Corrections de la revue de la couche 9 (#47) branchées dans `AuthenticatedView`.
final class Layer9ReviewFixTests: XCTestCase {
    private func source(_ path: String) throws -> String {
        try String(
            contentsOf: URL(fileURLWithPath: #filePath).deletingLastPathComponent().deletingLastPathComponent()
                .appendingPathComponent(path),
            encoding: .utf8
        )
    }

    /// Le menu du hub ajoute l'événement au calendrier comme l'écran Infos (`CalendarService.addToNativeCalendar`).
    func testHubMenuAddsTheEventToTheCalendarLikeInformation() throws {
        let content = try source("src/Views/App/ContentView.swift")
        guard let hub = content.range(of: "private func eventHubContent(for event: Event) -> some View") else {
            return XCTFail("eventHubContent")
        }
        XCTAssertTrue(String(content[hub.lowerBound...].prefix(2600)).contains("onAddToCalendar: { addInformationEventToCalendar(event) }"))
        guard let add = content.range(of: "private func addInformationEventToCalendar(_ event: Event)") else {
            return XCTFail("addInformationEventToCalendar")
        }
        XCTAssertTrue(String(content[add.lowerBound...].prefix(600)).contains("addToNativeCalendar("))
        let hubView = try source("src/Views/Hub/EventHubView.swift")
        XCTAssertTrue(hubView.contains("Button(String(localized: \"hub.menu.add_to_calendar\"), action: onAddToCalendar)"))
    }

    /// Lancement QA amorcé sans route préparée : l'accueil ne reste plus bloqué sur l'indicateur d'amorçage.
    func testQASeedWaitEndsWhenNothingIsPrepared() throws {
        let content = try source("src/Views/App/ContentView.swift")
        guard let start = content.range(of: "private func prepareInvitationExperienceQALaunch() async") else {
            return XCTFail("prepareInvitationExperienceQALaunch")
        }
        let body = String(content[start.lowerBound...].prefix(1400))
        guard let nilPath = body.range(of: "debugLog(\"[QALaunch] prepare returned nil\")") else {
            return XCTFail("chemin nil")
        }
        let branch = String(body[nilPath.lowerBound...].prefix(200))
        XCTAssertTrue(branch.contains("invitationQALibraryIsSeedReady = true"), branch)
    }

    func testCalendarMenuKeyExistsInEveryLanguage() throws {
        let root = URL(fileURLWithPath: #filePath).deletingLastPathComponent().deletingLastPathComponent()
        for locale in ["en", "fr", "es", "it", "pt"] {
            let strings = try String(contentsOf: root.appendingPathComponent("src/Resources/\(locale).lproj/Localizable.strings"), encoding: .utf8)
            XCTAssertTrue(strings.contains("\"hub.menu.add_to_calendar\" ="), locale)
        }
        XCTAssertEqual(WK.localizedFormat("hub.menu.add_to_calendar", locale: Locale(identifier: "fr")), "Ajouter au calendrier")
    }
}
