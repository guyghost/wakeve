import Foundation
import CoreLocation
import WeatherKit

struct EventWeatherForecastDay: Equatable {
    let date: Date
    let condition: String
    let symbolName: String
    let lowTemperatureCelsius: Double
    let highTemperatureCelsius: Double
    let precipitationChance: Double
    let windSpeedKph: Double
}

enum EventWeatherProviderFailure: Error, Equatable {
    case permissionOrEntitlementRequired
    case providerUnavailable
    case unsupportedDate(refreshDate: Date?)
}

enum EventWeatherProviderResult {
    case available(EventWeatherSummary)
    case pending(refreshDate: Date)
    case permissionOrEntitlementRequired
    case providerUnavailable
}

protocol EventWeatherProviding {
    func forecast(
        for place: EventWeatherPlace,
        targetDate: Date,
        fetchedAt: Date
    ) async -> EventWeatherProviderResult
}

enum EventWeatherProviderMapper {
    static func mapSuccess(
        days: [EventWeatherForecastDay],
        place: EventWeatherPlace,
        targetDate: Date,
        fetchedAt: Date,
        calendar: Calendar = .current
    ) -> EventWeatherProviderResult {
        guard let day = days.first(where: { calendar.isDate($0.date, inSameDayAs: targetDate) }) ?? days.first else {
            return .pending(refreshDate: fetchedAt)
        }

        return .available(
            EventWeatherSummary(
                placeName: place.name,
                coordinate: place.coordinate,
                condition: day.condition,
                symbolName: day.symbolName,
                lowTemperature: day.lowTemperatureCelsius,
                highTemperature: day.highTemperatureCelsius,
                precipitationChance: day.precipitationChance,
                windSpeedKph: day.windSpeedKph,
                fetchedAt: fetchedAt
            )
        )
    }

    static func mapFailure(
        _ failure: EventWeatherProviderFailure,
        fallbackRefreshDate: Date
    ) -> EventWeatherProviderResult {
        switch failure {
        case .permissionOrEntitlementRequired:
            return .permissionOrEntitlementRequired
        case .providerUnavailable:
            return .providerUnavailable
        case .unsupportedDate(let refreshDate):
            return .pending(refreshDate: refreshDate ?? fallbackRefreshDate)
        }
    }
}

final class WeatherKitEventForecastProvider: EventWeatherProviding {
    private let weatherService: WeatherService

    init(weatherService: WeatherService = .shared) {
        self.weatherService = weatherService
    }

    func forecast(
        for place: EventWeatherPlace,
        targetDate: Date,
        fetchedAt: Date = Date()
    ) async -> EventWeatherProviderResult {
        do {
            let location = CLLocation(latitude: place.coordinate.latitude, longitude: place.coordinate.longitude)
            let weather = try await weatherService.weather(for: location)
            let days = weather.dailyForecast.forecast.map { day in
                EventWeatherForecastDay(
                    date: day.date,
                    condition: day.condition.description,
                    symbolName: day.symbolName,
                    lowTemperatureCelsius: day.lowTemperature.converted(to: .celsius).value,
                    highTemperatureCelsius: day.highTemperature.converted(to: .celsius).value,
                    precipitationChance: day.precipitationChance,
                    windSpeedKph: day.wind.speed.converted(to: .kilometersPerHour).value
                )
            }
            return EventWeatherProviderMapper.mapSuccess(
                days: days,
                place: place,
                targetDate: targetDate,
                fetchedAt: fetchedAt
            )
        } catch {
            return EventWeatherProviderMapper.mapFailure(
                WeatherKitEventForecastProvider.mapError(error),
                fallbackRefreshDate: fetchedAt
            )
        }
    }

    static func mapError(_ error: Error) -> EventWeatherProviderFailure {
        let description = String(describing: error).lowercased()
        let localized = error.localizedDescription.lowercased()
        let combined = "\(description) \(localized)"

        if combined.contains("entitlement") || combined.contains("not authorized") || combined.contains("permission") {
            return .permissionOrEntitlementRequired
        }
        if combined.contains("unsupported") || combined.contains("outside") || combined.contains("date range") {
            return .unsupportedDate(refreshDate: nil)
        }
        return .providerUnavailable
    }
}
