import CoreLocation
import Foundation

/// Résumé météo d'un lieu d'événement (fourni par `EventWeatherProvider`).
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
