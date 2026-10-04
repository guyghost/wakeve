import CoreLocation
import Foundation
import MapKit
import Shared

/// Charge la prévision du jour cible pour le lieu de l'événement (revue couche 9, #47), comme l'ancien
/// `EventWeatherViewModel` : lieu lu en base hors du fil principal (`EventDayPlaceResolver`, lieu géocodé
/// `resolvedEventLocation`), recherche MapKit par nom si aucune coordonnée, puis `EventWeatherProvider`.
struct EventWeatherLoader {
    /// Un seul cache de prévisions (10 min) pour le hub et le jour J.
    static let sharedProvider: EventWeatherProviding = CachedEventWeatherProvider(upstream: WeatherKitEventForecastProvider())

    private let provider: EventWeatherProviding
    private let database: WakeveDb

    init(provider: EventWeatherProviding = EventWeatherLoader.sharedProvider, database: WakeveDb = RepositoryProvider.shared.database) {
        self.provider = provider
        self.database = database
    }

    func load(eventId: String, targetDate: Date) async -> EventWeatherState {
        // Lectures synchrones de la base Kotlin hors du fil principal (mêmes garanties que `SharedEventDaySource`).
        let reader = self
        let choice = await Task.detached(priority: .userInitiated) {
            reader.placeChoice(eventId: eventId)
        }.value
        let place: EventWeatherPlace?
        switch choice {
        case .coordinate(let name, let latitude, let longitude):
            place = EventWeatherPlace(name: name, coordinate: CLLocationCoordinate2D(latitude: latitude, longitude: longitude))
        case .search(let query):
            place = await Self.searchPlace(query)
        case .none:
            place = nil
        }
        guard let place, !Task.isCancelled else { return .hidden }
        return EventWeatherState(result: await provider.forecast(for: place, targetDate: targetDate, fetchedAt: Date()))
    }

    private func placeChoice(eventId: String) -> EventWeatherPlaceChoice {
        let scenario = ScenarioRepository(db: database).getSelectedScenario(eventId: eventId)
        let locations = database.potentialLocationQueries.selectByEventId(eventId: eventId).executeAsList()
        let place = EventDayPlaceResolver.resolve(
            selectedScenario: scenario.map {
                EventDayPlaceResolver.Scenario(location: $0.location, sourcePotentialLocationId: $0.sourcePotentialLocationId)
            },
            potentialLocations: locations.map {
                EventDayPlaceResolver.Location(id: $0.id, name: $0.name, address: $0.address, coordinatesJSON: $0.coordinates)
            }
        )
        let resolved = database.eventWeatherQueries.selectResolvedLocationByEvent(eventId: eventId).executeAsOneOrNull()
        return EventWeatherPlaceChoice.choose(
            place: place,
            resolved: resolved.map { EventWeatherPlaceChoice.Resolved(label: $0.label, latitude: $0.latitude, longitude: $0.longitude) }
        )
    }

    /// Premier résultat MapKit pour « nom, adresse » (hors du fil principal).
    private static func searchPlace(_ query: String) async -> EventWeatherPlace? {
        let request = MKLocalSearch.Request()
        request.naturalLanguageQuery = query
        guard let item = try? await MKLocalSearch(request: request).start().mapItems.first else { return nil }
        let coordinate: CLLocationCoordinate2D
        if #available(iOS 26.0, *) {
            coordinate = item.location.coordinate
        } else {
            coordinate = item.placemark.coordinate
        }
        return EventWeatherPlace(name: item.name ?? query, coordinate: coordinate)
    }
}

/// Météo publiée pour une vue (hub, jour J).
@MainActor
final class EventWeatherModel: ObservableObject {
    @Published private(set) var state: EventWeatherState = .hidden

    private let load: (String, Date) async -> EventWeatherState
    private var generation = 0

    init(load: @escaping (String, Date) async -> EventWeatherState = { eventId, date in
        await EventWeatherLoader().load(eventId: eventId, targetDate: date)
    }) {
        self.load = load
    }

    /// nil : rien à montrer (règle ou fenêtre non satisfaite).
    func load(eventId: String, targetDate: Date?) async {
        generation += 1
        let token = generation
        guard let targetDate else {
            state = .hidden
            return
        }
        // Une prévision déjà affichée reste visible pendant le rechargement.
        if case .available = state {} else { state = .loading }
        let result = await load(eventId, targetDate)
        guard token == generation else { return }
        state = result
    }
}
