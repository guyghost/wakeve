import SwiftUI
import MapKit
import WeatherKit
import CoreLocation
import Shared

@MainActor
final class EventWeatherViewModel: ObservableObject {
    @Published private(set) var state: EventWeatherCardState = .idle

    private let weatherService = WeatherService.shared

    func load(event: Event) async {
        guard event.status == .confirmed || event.status == .organizing || event.status == .finalized else {
            state = .hidden
            return
        }

        guard let eventDate = event.weatherTargetDate else {
            state = .unavailable(message: "Date confirmée requise pour afficher la météo.")
            return
        }

        let daysUntilEvent = Calendar.current.dateComponents(
            [.day],
            from: Calendar.current.startOfDay(for: Date()),
            to: Calendar.current.startOfDay(for: eventDate)
        ).day ?? 0

        guard daysUntilEvent >= 0 else {
            state = .unavailable(message: "La météo n'est plus disponible pour cette date passée.")
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

            let location = CLLocation(latitude: place.coordinate.latitude, longitude: place.coordinate.longitude)
            let weather = try await weatherService.weather(for: location)
            let day = weather.dailyForecast.forecast.first {
                Calendar.current.isDate($0.date, inSameDayAs: eventDate)
            } ?? weather.dailyForecast.forecast.first

            guard let day else {
                state = .pending(refreshDate: Date())
                return
            }

            state = .available(
                EventWeatherSummary(
                    placeName: place.name,
                    coordinate: place.coordinate,
                    condition: day.condition.description,
                    symbolName: day.symbolName,
                    lowTemperature: day.lowTemperature.converted(to: .celsius).value,
                    highTemperature: day.highTemperature.converted(to: .celsius).value,
                    precipitationChance: day.precipitationChance,
                    windSpeedKph: day.wind.speed.converted(to: .kilometersPerHour).value,
                    fetchedAt: Date()
                )
            )
        } catch {
            state = .unavailable(message: "Météo indisponible. Vérifiez la capacité WeatherKit et la connexion.")
        }
    }

    private func resolvePlace(for event: Event) async throws -> EventWeatherPlace? {
        let locations = RepositoryProvider.shared.database.potentialLocationQueries
            .selectByEventId(eventId: event.id)
            .executeAsList()

        guard let location = locations.first else {
            return nil
        }

        if let coordinates = location.coordinates.flatMap(Self.parseCoordinates) {
            return EventWeatherPlace(
                name: location.name,
                coordinate: coordinates
            )
        }

        let request = MKLocalSearch.Request()
        request.naturalLanguageQuery = [location.name, location.address].compactMap { $0 }.joined(separator: ", ")

        let response = try await MKLocalSearch(request: request).start()
        guard let item = response.mapItems.first,
              let coordinate = item.placemark.location?.coordinate else {
            return nil
        }

        return EventWeatherPlace(
            name: item.name ?? location.name,
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
}

private struct EventWeatherPlace {
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
                    Text("Chargement météo")
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
                            Text("Météo")
                                .font(WakeveTheme.Typography.section)
                                .foregroundColor(primaryText)

                            Text("\(summary.placeName) · \(summary.condition)")
                                .font(WakeveTheme.Typography.callout)
                                .foregroundColor(secondaryText)
                                .lineLimit(2)
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
                    .accessibilityLabel("Carte du lieu météo \(summary.placeName)")
                }
            }
        case .pending(let refreshDate):
            stateMessage(
                icon: "calendar.badge.clock",
                title: "Météo bientôt disponible",
                body: "Les prévisions seront demandées à partir du \(refreshDate.formatted(date: .abbreviated, time: .omitted))."
            )
        case .missingLocation:
            stateMessage(
                icon: "mappin.slash",
                title: "Lieu à préciser",
                body: "Ajoutez un lieu ou une adresse pour afficher la météo de l'événement."
            )
        case .unavailable(let message):
            stateMessage(
                icon: "exclamationmark.triangle",
                title: "Météo indisponible",
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
