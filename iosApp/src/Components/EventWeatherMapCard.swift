import SwiftUI
import MapKit
import CoreLocation
import Shared

@MainActor
final class EventWeatherViewModel: ObservableObject {
    @Published private(set) var state: EventWeatherCardState = .idle

    private let weatherProvider: EventWeatherProviding

    init(weatherProvider: EventWeatherProviding = CachedEventWeatherProvider(upstream: WeatherKitEventForecastProvider())) {
        self.weatherProvider = weatherProvider
    }

    func hide() {
        state = .hidden
    }

    func load(event: Event) async {
        guard event.status == .confirmed || event.status == .organizing || event.status == .finalized else {
            state = .hidden
            return
        }

        guard let eventDate = event.weatherTargetDate else {
            state = .unavailable(message: String(localized: "weather.required_date_message"))
            return
        }

        let daysUntilEvent = Calendar.current.dateComponents(
            [.day],
            from: Calendar.current.startOfDay(for: Date()),
            to: Calendar.current.startOfDay(for: eventDate)
        ).day ?? 0

        guard daysUntilEvent >= 0 else {
            state = .unavailable(message: String(localized: "weather.past_date_message"))
            return
        }

        guard daysUntilEvent <= 10 else {
            let refreshDate = Calendar.current.date(byAdding: .day, value: -9, to: eventDate) ?? eventDate
            state = .pending(refreshDate: refreshDate)
            return
        }

        state = .loading

        do {
            guard let place = try await resolvePlace(for: event) else {
                state = .missingLocation
                return
            }

            let forecast = await weatherProvider.forecast(
                for: place,
                targetDate: eventDate,
                fetchedAt: Date()
            )

            switch forecast {
            case .available(let summary):
                state = .available(summary)
            case .pending(let refreshDate):
                state = .pending(refreshDate: refreshDate)
            case .permissionOrEntitlementRequired:
                state = .unavailable(message: String(localized: "weather.unavailable_message"))
            case .providerUnavailable:
                state = .unavailable(message: String(localized: "weather.unavailable_message"))
            }
        } catch {
            state = .unavailable(message: String(localized: "weather.unavailable_message"))
        }
    }

    private func resolvePlace(for event: Event) async throws -> EventWeatherPlace? {
        if let potentialLocationPlace = try await resolvePotentialLocationPlace(for: event) {
            return potentialLocationPlace
        }

        return try await resolveScenarioPlace(for: event)
    }

    private func resolvePotentialLocationPlace(for event: Event) async throws -> EventWeatherPlace? {
        let locations = RepositoryProvider.shared.database.potentialLocationQueries
            .selectByEventId(eventId: event.id)
            .executeAsList()

        for location in locations {
            if let coordinates = location.coordinates.flatMap(Self.parseCoordinates) {
                return EventWeatherPlace(
                    name: location.name,
                    coordinate: coordinates
                )
            }

            if let place = try await searchPlace(
                name: location.name,
                address: location.address
            ) {
                return place
            }
        }

        return nil
    }

    private func resolveScenarioPlace(for event: Event) async throws -> EventWeatherPlace? {
        let scenarioRepository = ScenarioRepository(db: RepositoryProvider.shared.database)
        let scenarios = scenarioPlaceCandidates(
            selected: scenarioRepository.getSelectedScenario(eventId: event.id),
            selectedFallbacks: scenarioRepository.getScenariosByEventIdAndStatus(eventId: event.id, status: ScenarioStatus.selected),
            allScenarios: scenarioRepository.getScenariosByEventId(eventId: event.id)
        )

        for scenario in scenarios {
            if let place = try await searchPlace(name: scenario.location, address: nil) {
                return place
            }
        }

        return nil
    }

    private func scenarioPlaceCandidates(
        selected: Scenario_?,
        selectedFallbacks: [Scenario_],
        allScenarios: [Scenario_]
    ) -> [Scenario_] {
        var seenIds = Set<String>()
        var candidates: [Scenario_] = []

        for scenario in ([selected].compactMap { $0 } + selectedFallbacks + allScenarios) {
            let location = scenario.location.trimmingCharacters(in: .whitespacesAndNewlines)
            guard !location.isEmpty, !seenIds.contains(scenario.id) else {
                continue
            }

            seenIds.insert(scenario.id)
            candidates.append(scenario)
        }

        return candidates
    }

    private func searchPlace(name: String, address: String?) async throws -> EventWeatherPlace? {
        let query = [name, address]
            .compactMap { $0?.trimmingCharacters(in: .whitespacesAndNewlines) }
            .filter { !$0.isEmpty }
            .joined(separator: ", ")
        guard !query.isEmpty else {
            return nil
        }

        let request = MKLocalSearch.Request()
        request.naturalLanguageQuery = query

        let response = try await MKLocalSearch(request: request).start()
        guard let item = response.mapItems.first,
              let coordinate = item.placemark.location?.coordinate else {
            return nil
        }

        return EventWeatherPlace(
            name: item.name ?? name,
            coordinate: coordinate
        )
    }

    private static func parseCoordinates(_ rawValue: String) -> CLLocationCoordinate2D? {
        guard let data = rawValue.data(using: .utf8),
              let object = try? JSONSerialization.jsonObject(with: data) as? [String: Double],
              let latitude = object["latitude"],
              let longitude = object["longitude"] else {
            return nil
        }

        return CLLocationCoordinate2D(latitude: latitude, longitude: longitude)
    }
}

enum EventWeatherCardState {
    case idle
    case hidden
    case loading
    case available(EventWeatherSummary)
    case pending(refreshDate: Date)
    case missingLocation
    case unavailable(message: String)
}

struct EventWeatherSummary {
    let placeName: String
    let coordinate: CLLocationCoordinate2D
    let condition: String
    let symbolName: String
    let lowTemperature: Double
    let highTemperature: Double
    let precipitationChance: Double
    let windSpeedKph: Double
    let fetchedAt: Date
    let isStale: Bool

    init(
        placeName: String,
        coordinate: CLLocationCoordinate2D,
        condition: String,
        symbolName: String,
        lowTemperature: Double,
        highTemperature: Double,
        precipitationChance: Double,
        windSpeedKph: Double,
        fetchedAt: Date,
        isStale: Bool = false
    ) {
        self.placeName = placeName
        self.coordinate = coordinate
        self.condition = condition
        self.symbolName = symbolName
        self.lowTemperature = lowTemperature
        self.highTemperature = highTemperature
        self.precipitationChance = precipitationChance
        self.windSpeedKph = windSpeedKph
        self.fetchedAt = fetchedAt
        self.isStale = isStale
    }
}

struct EventWeatherPlace {
    let name: String
    let coordinate: CLLocationCoordinate2D
}

struct EventWeatherMapCard: View {
    @Environment(\.colorScheme) private var colorScheme

    let state: EventWeatherCardState

    var body: some View {
        switch state {
        case .hidden:
            EmptyView()
        case .idle, .loading:
            shell {
                HStack(spacing: WakeveTheme.Spacing.sm) {
                    ProgressView()
                        .accessibilityLabel(String(localized: "weather.loading"))
                    Text(String(localized: "weather.loading"))
                        .font(WakeveTheme.Typography.callout)
                        .foregroundColor(secondaryText)
                }
            }
        case .available(let summary):
            shell {
                VStack(alignment: .leading, spacing: WakeveTheme.Spacing.md) {
                    HStack(alignment: .top, spacing: WakeveTheme.Spacing.md) {
                        Image(systemName: summary.symbolName)
                            .font(.title2.weight(.semibold))
                            .foregroundColor(WakeveTheme.ColorToken.eventHighlight(for: colorScheme))
                            .frame(width: 44, height: 44)
                            .background(WakeveTheme.ColorToken.controlFill(for: colorScheme))
                            .clipShape(RoundedRectangle(cornerRadius: WakeveTheme.Radius.md, style: .continuous))

                        VStack(alignment: .leading, spacing: WakeveTheme.Spacing.xxs) {
                            Text(String(localized: "weather.title"))
                                .font(WakeveTheme.Typography.section)
                                .foregroundColor(primaryText)

                            Text("\(summary.placeName) · \(summary.condition)")
                                .font(WakeveTheme.Typography.callout)
                                .foregroundColor(secondaryText)
                                .lineLimit(2)

                            if summary.isStale {
                                Text(String(localized: "weather.stale_badge"))
                                    .font(WakeveTheme.Typography.caption)
                                    .foregroundColor(SemanticColor.warning(for: colorScheme))
                            }
                        }

                        Spacer()
                    }

                    HStack(spacing: WakeveTheme.Spacing.sm) {
                        metric("thermometer.medium", "\(Int(summary.lowTemperature.rounded()))° / \(Int(summary.highTemperature.rounded()))°")
                        metric("cloud.rain", "\(Int((summary.precipitationChance * 100).rounded()))%")
                        metric("wind", "\(Int(summary.windSpeedKph.rounded())) km/h")
                    }

                    Map {
                        Marker(summary.placeName, coordinate: summary.coordinate)
                    }
                    .frame(height: 118)
                    .clipShape(RoundedRectangle(cornerRadius: WakeveTheme.Radius.md, style: .continuous))
                    .accessibilityLabel(String(format: String(localized: "weather.map_accessibility_format"), summary.placeName))
                }
            }
        case .pending(let refreshDate):
            stateMessage(
                icon: "calendar.badge.clock",
                title: String(localized: "weather.pending_title"),
                body: String(format: String(localized: "weather.pending_body_format"), refreshDate.formatted(date: .abbreviated, time: .omitted))
            )
        case .missingLocation:
            stateMessage(
                icon: "mappin.slash",
                title: String(localized: "weather.missing_location_title"),
                body: String(localized: "weather.missing_location_body")
            )
        case .unavailable(let message):
            stateMessage(
                icon: "exclamationmark.triangle",
                title: String(localized: "weather.unavailable_title"),
                body: message
            )
        }
    }

    private func shell<Content: View>(@ViewBuilder content: () -> Content) -> some View {
        WakeveGlassCard(prominence: .regular, cornerRadius: WakeveTheme.Radius.xl, padding: WakeveTheme.Spacing.md) {
            content()
        }
    }

    private func stateMessage(icon: String, title: String, body: String) -> some View {
        shell {
            HStack(alignment: .top, spacing: WakeveTheme.Spacing.md) {
                Image(systemName: icon)
                    .font(.title3.weight(.semibold))
                    .foregroundColor(WakeveTheme.ColorToken.accent(for: colorScheme))
                    .frame(width: 40, height: 40)
                    .background(WakeveTheme.ColorToken.controlFill(for: colorScheme))
                    .clipShape(Circle())

                VStack(alignment: .leading, spacing: WakeveTheme.Spacing.xxs) {
                    Text(title)
                        .font(WakeveTheme.Typography.section)
                        .foregroundColor(primaryText)
                    Text(body)
                        .font(WakeveTheme.Typography.callout)
                        .foregroundColor(secondaryText)
                        .lineLimit(3)
                }
            }
        }
    }

    private func metric(_ icon: String, _ value: String) -> some View {
        HStack(spacing: WakeveTheme.Spacing.xs) {
            Image(systemName: icon)
                .font(.caption.weight(.bold))
            Text(value)
                .font(WakeveTheme.Typography.caption)
                .lineLimit(1)
                .minimumScaleFactor(0.78)
        }
        .foregroundColor(primaryText)
        .frame(maxWidth: .infinity)
        .padding(.vertical, WakeveTheme.Spacing.xs)
        .background(WakeveTheme.ColorToken.controlFill(for: colorScheme))
        .clipShape(Capsule())
    }

    private var primaryText: Color {
        WakeveTheme.ColorToken.primaryText(for: colorScheme)
    }

    private var secondaryText: Color {
        WakeveTheme.ColorToken.secondaryText(for: colorScheme)
    }
}

private extension Event {
    var weatherTargetDate: Date? {
        let formatter = ISO8601DateFormatter()
        if let finalDate, let date = formatter.date(from: finalDate) {
            return date
        }

        return proposedSlots.compactMap { slot in
            slot.start.flatMap { formatter.date(from: $0) }
        }.first
    }
}

enum EventWeatherMapCardPreviewFixtures {
    static let fetchedAt = Date(timeIntervalSince1970: 1_784_640_000)
    static let refreshDate = Date(timeIntervalSince1970: 1_784_985_600)

    static let availableSummary = EventWeatherSummary(
        placeName: "Biarritz",
        coordinate: CLLocationCoordinate2D(latitude: 43.4832, longitude: -1.5586),
        condition: "Sunny",
        symbolName: "sun.max",
        lowTemperature: 20,
        highTemperature: 28,
        precipitationChance: 0.12,
        windSpeedKph: 16,
        fetchedAt: fetchedAt
    )

    static let staleSummary = EventWeatherSummary(
        placeName: "Biarritz",
        coordinate: CLLocationCoordinate2D(latitude: 43.4832, longitude: -1.5586),
        condition: "Cloudy",
        symbolName: "cloud.sun",
        lowTemperature: 17,
        highTemperature: 23,
        precipitationChance: 0.38,
        windSpeedKph: 22,
        fetchedAt: fetchedAt,
        isStale: true
    )
}

private struct EventWeatherMapCardPreviewSurface: View {
    let state: EventWeatherCardState

    var body: some View {
        EventWeatherMapCard(state: state)
            .padding()
            .background(Color(.systemGroupedBackground))
    }
}

#Preview("Weather Loading") {
    EventWeatherMapCardPreviewSurface(state: .loading)
}

#Preview("Weather Available") {
    EventWeatherMapCardPreviewSurface(state: .available(EventWeatherMapCardPreviewFixtures.availableSummary))
}

#Preview("Weather Available Dark") {
    EventWeatherMapCardPreviewSurface(state: .available(EventWeatherMapCardPreviewFixtures.availableSummary))
        .preferredColorScheme(.dark)
}

#Preview("Weather Available Dynamic Type") {
    EventWeatherMapCardPreviewSurface(state: .available(EventWeatherMapCardPreviewFixtures.availableSummary))
        .dynamicTypeSize(.accessibility3)
}

#Preview("Weather Reduce Transparency") {
    EventWeatherMapCardPreviewSurface(state: .available(EventWeatherMapCardPreviewFixtures.availableSummary))
}

#Preview("Weather Stale") {
    EventWeatherMapCardPreviewSurface(state: .available(EventWeatherMapCardPreviewFixtures.staleSummary))
}

#Preview("Weather Pending") {
    EventWeatherMapCardPreviewSurface(state: .pending(refreshDate: EventWeatherMapCardPreviewFixtures.refreshDate))
}

#Preview("Weather Unavailable") {
    EventWeatherMapCardPreviewSurface(state: .unavailable(message: String(localized: "weather.unavailable_message")))
}
