import CoreLocation
import XCTest
@testable import Wakeve

/// Météo de l'événement dans le hub et le jour J (revue couche 9, #47), à la place de la carte météo de
/// l'ancien détail (`EventWeatherMapCard` / `EventWeatherViewModel`).
final class EventWeatherDisplayTests: XCTestCase {
    private let fr = Locale(identifier: "fr_FR")
    private let en = Locale(identifier: "en_US")
    private var paris: Calendar {
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = TimeZone(identifier: "Europe/Paris")!
        return calendar
    }
    private let now = ISO8601DateFormatter().date(from: "2026-10-02T15:00:00Z")!

    // MARK: - Fenêtre du fournisseur (0-10 jours)

    func testForecastWindowIsTodayToTenDaysAhead() {
        func day(_ offset: Int, hour: Int = 10) -> Date {
            let start = paris.startOfDay(for: now)
            return paris.date(byAdding: DateComponents(day: offset, hour: hour), to: start)!
        }
        XCTAssertTrue(EventWeatherRule.isInWindow(target: day(0, hour: 1), now: now, calendar: paris), "Aujourd'hui, même passé.")
        XCTAssertTrue(EventWeatherRule.isInWindow(target: day(10, hour: 23), now: now, calendar: paris))
        XCTAssertFalse(EventWeatherRule.isInWindow(target: day(11, hour: 0), now: now, calendar: paris))
        XCTAssertFalse(EventWeatherRule.isInWindow(target: day(-1), now: now, calendar: paris), "Date passée.")
    }

    /// Ancienne règle : date retenue (confirmé, organisation, finalisé) et accès aux détails.
    func testWeatherNeedsARetainedDateAndDetailAccess() {
        for phase in [EventHubFacts.Phase.confirmed, .organizing, .finalized] {
            XCTAssertTrue(EventWeatherRule.applies(phase: phase, hasAccess: true), "\(phase)")
            XCTAssertFalse(EventWeatherRule.applies(phase: phase, hasAccess: false), "\(phase)")
        }
        for phase in [EventHubFacts.Phase.draft, .polling, .comparing] {
            XCTAssertFalse(EventWeatherRule.applies(phase: phase, hasAccess: true), "\(phase)")
        }
    }

    func testTargetDateIsTheRetainedSlotStartThenTheFinalDate() {
        let slot = now.addingTimeInterval(3_600)
        let final = now.addingTimeInterval(7_200)
        XCTAssertEqual(EventWeatherRule.targetDate(slotStart: slot, finalDate: final), slot)
        XCTAssertEqual(EventWeatherRule.targetDate(slotStart: nil, finalDate: final), final)
        XCTAssertNil(EventWeatherRule.targetDate(slotStart: nil, finalDate: nil))
    }

    func testHubLoadsOnlyWhenTheRuleAndWindowAllowIt() {
        let soon = now.addingTimeInterval(3 * 86_400)
        let far = now.addingTimeInterval(20 * 86_400)
        XCTAssertEqual(EventWeatherRule.loadTarget(phase: .organizing, hasAccess: true, slotStart: soon, finalDate: nil, now: now, calendar: paris), soon)
        XCTAssertNil(EventWeatherRule.loadTarget(phase: .organizing, hasAccess: true, slotStart: far, finalDate: nil, now: now, calendar: paris))
        XCTAssertNil(EventWeatherRule.loadTarget(phase: .polling, hasAccess: true, slotStart: soon, finalDate: nil, now: now, calendar: paris))
        XCTAssertNil(EventWeatherRule.loadTarget(phase: .organizing, hasAccess: false, slotStart: soon, finalDate: nil, now: now, calendar: paris))
        XCTAssertNil(EventWeatherRule.loadTarget(phase: .organizing, hasAccess: true, slotStart: nil, finalDate: nil, now: now, calendar: paris))
    }

    // MARK: - Lieu

    func testPlaceUsesKnownCoordinatesFirstThenTheResolvedLocationThenASearchByName() {
        let withCoordinates = EventDayPlace(name: "Chalet", address: "Annecy", latitude: 45.9, longitude: 6.1)
        let nameOnly = EventDayPlace(name: "Chalet du lac", address: "Annecy", latitude: nil, longitude: nil)
        let resolved = EventWeatherPlaceChoice.Resolved(label: "Annecy", latitude: 45.89, longitude: 6.12)
        XCTAssertEqual(EventWeatherPlaceChoice.choose(place: withCoordinates, resolved: resolved),
                       .coordinate(name: "Chalet", latitude: 45.9, longitude: 6.1))
        XCTAssertEqual(EventWeatherPlaceChoice.choose(place: nameOnly, resolved: resolved),
                       .coordinate(name: "Annecy", latitude: 45.89, longitude: 6.12))
        XCTAssertEqual(EventWeatherPlaceChoice.choose(place: nameOnly, resolved: nil), .search(query: "Chalet du lac, Annecy"))
        XCTAssertEqual(EventWeatherPlaceChoice.choose(
            place: EventDayPlace(name: "Lyon", address: nil, latitude: nil, longitude: nil), resolved: nil
        ), .search(query: "Lyon"))
        XCTAssertEqual(EventWeatherPlaceChoice.choose(place: nil, resolved: nil), .none)
    }

    // MARK: - Résultat du fournisseur

    private func summary() -> EventWeatherSummary {
        EventWeatherSummary(
            placeName: "Annecy", coordinate: CLLocationCoordinate2D(latitude: 45.9, longitude: 6.1),
            condition: "Ensoleillé", symbolName: "sun.max", lowTemperature: 11.6, highTemperature: 21.2,
            precipitationChance: 0.3, windSpeedKph: 12, fetchedAt: now
        )
    }

    func testProviderResultsMapToWhatTheScreensShow() {
        XCTAssertEqual(EventWeatherState(result: .available(summary())), .available(EventWeatherDisplay(
            placeName: "Annecy", condition: "Ensoleillé", symbolName: "sun.max",
            low: 11.6, high: 21.2, precipitationChance: 0.3
        )))
        XCTAssertEqual(EventWeatherState(result: .pending(refreshDate: now)), .hidden, "Hors fenêtre : rien à montrer.")
        XCTAssertEqual(EventWeatherState(result: .providerUnavailable), .unavailable)
        XCTAssertEqual(EventWeatherState(result: .permissionOrEntitlementRequired), .unavailable)
    }

    // MARK: - Textes

    func testTemperaturesAndRainAreLocalized() {
        let display = EventWeatherDisplay(placeName: "Annecy", condition: "Ensoleillé", symbolName: "sun.max",
                                          low: 11.6, high: 21.2, precipitationChance: 0.3)
        XCTAssertEqual(EventWeatherText.temperatures(display, locale: fr), "12° / 21°")
        XCTAssertEqual(EventWeatherText.rain(display, locale: fr), "Pluie : 30 %")
        XCTAssertEqual(EventWeatherText.rain(display, locale: en), "Rain: 30%")
        XCTAssertEqual(EventWeatherText.title(display, locale: fr), "Météo · Annecy")
    }

    func testEveryWeatherKeyExistsInEveryLanguage() throws {
        let root = URL(fileURLWithPath: #filePath).deletingLastPathComponent().deletingLastPathComponent()
        for locale in ["en", "fr", "es", "it", "pt"] {
            let strings = try String(contentsOf: root.appendingPathComponent("src/Resources/\(locale).lproj/Localizable.strings"), encoding: .utf8)
            for key in ["hub.weather.title_format", "hub.weather.rain_format", "hub.weather.unavailable", "weather.loading"] {
                XCTAssertTrue(strings.contains("\"\(key)\" ="), "\(key) manquante (\(locale))")
            }
        }
    }

    // MARK: - Branchement

    private func source(_ path: String) throws -> String {
        try String(
            contentsOf: URL(fileURLWithPath: #filePath).deletingLastPathComponent().deletingLastPathComponent()
                .appendingPathComponent(path),
            encoding: .utf8
        )
    }

    func testLoaderReusesTheProviderAndSearchesOffTheMainThread() throws {
        let loader = try source("src/Services/EventWeatherLoader.swift")
        XCTAssertTrue(loader.contains("CachedEventWeatherProvider(upstream: WeatherKitEventForecastProvider())"))
        XCTAssertTrue(loader.contains("Task.detached(priority: .userInitiated)"))
        XCTAssertTrue(loader.contains("MKLocalSearch"))
        XCTAssertTrue(loader.contains("EventDayPlaceResolver.resolve("))
        XCTAssertTrue(loader.contains("selectResolvedLocationByEvent"))
    }

    func testHubAndEventDayShowTheWeather() throws {
        let supplements = try source("src/Views/Hub/EventHubSupplements.swift")
        XCTAssertTrue(supplements.contains("EventHubWeatherCard("))
        let hub = try source("src/Views/Hub/EventHubView.swift")
        XCTAssertTrue(hub.contains("@StateObject private var weather = EventWeatherModel()"))
        XCTAssertTrue(hub.contains("EventWeatherRule.loadTarget("))
        let day = try source("src/Views/Immersive/EventDayView.swift")
        XCTAssertTrue(day.contains("EventDayWeatherCard("))
        XCTAssertTrue(day.contains("@StateObject private var weather = EventWeatherModel()"))
    }
}
