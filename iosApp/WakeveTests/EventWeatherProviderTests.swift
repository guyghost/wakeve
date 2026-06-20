import XCTest
import CoreLocation
@testable import Wakeve

final class EventWeatherProviderTests: XCTestCase {
    private let calendar = Calendar(identifier: .gregorian)

    func testMapperReturnsAvailableSummaryForMatchingWeatherKitDay() {
        let targetDate = makeDate("2026-07-18T10:00:00Z")
        let fetchedAt = makeDate("2026-07-15T08:00:00Z")
        let place = EventWeatherPlace(
            name: "Biarritz",
            coordinate: CLLocationCoordinate2D(latitude: 43.4832, longitude: -1.5586)
        )

        let result = EventWeatherProviderMapper.mapSuccess(
            days: [
                EventWeatherForecastDay(
                    date: makeDate("2026-07-17T10:00:00Z"),
                    condition: "Cloudy",
                    symbolName: "cloud",
                    lowTemperatureCelsius: 16,
                    highTemperatureCelsius: 21,
                    precipitationChance: 0.4,
                    windSpeedKph: 18
                ),
                EventWeatherForecastDay(
                    date: targetDate,
                    condition: "Sunny",
                    symbolName: "sun.max",
                    lowTemperatureCelsius: 20,
                    highTemperatureCelsius: 28,
                    precipitationChance: 0.1,
                    windSpeedKph: 12
                )
            ],
            place: place,
            targetDate: targetDate,
            fetchedAt: fetchedAt,
            calendar: calendar
        )

        guard case .available(let summary) = result else {
            return XCTFail("Expected available summary")
        }
        XCTAssertEqual(summary.placeName, "Biarritz")
        XCTAssertEqual(summary.condition, "Sunny")
        XCTAssertEqual(summary.symbolName, "sun.max")
        XCTAssertEqual(summary.lowTemperature, 20)
        XCTAssertEqual(summary.highTemperature, 28)
        XCTAssertEqual(summary.precipitationChance, 0.1)
        XCTAssertEqual(summary.windSpeedKph, 12)
        XCTAssertEqual(summary.fetchedAt, fetchedAt)
        XCTAssertEqual(summary.coordinate.latitude, 43.4832, accuracy: 0.0001)
        XCTAssertEqual(summary.coordinate.longitude, -1.5586, accuracy: 0.0001)
    }

    func testMapperReturnsPendingWhenWeatherKitHasNoDailyForecast() {
        let fetchedAt = makeDate("2026-07-15T08:00:00Z")
        let result = EventWeatherProviderMapper.mapSuccess(
            days: [],
            place: EventWeatherPlace(
                name: "Biarritz",
                coordinate: CLLocationCoordinate2D(latitude: 43.4832, longitude: -1.5586)
            ),
            targetDate: makeDate("2026-07-18T10:00:00Z"),
            fetchedAt: fetchedAt,
            calendar: calendar
        )

        guard case .pending(let refreshDate) = result else {
            return XCTFail("Expected pending state")
        }
        XCTAssertEqual(refreshDate, fetchedAt)
    }

    func testFailureMapperPreservesEntitlementFailure() {
        let result = EventWeatherProviderMapper.mapFailure(
            .permissionOrEntitlementRequired,
            fallbackRefreshDate: makeDate("2026-07-15T08:00:00Z")
        )

        guard case .permissionOrEntitlementRequired = result else {
            return XCTFail("Expected entitlement state")
        }
    }

    func testFailureMapperPreservesProviderOutage() {
        let result = EventWeatherProviderMapper.mapFailure(
            .providerUnavailable,
            fallbackRefreshDate: makeDate("2026-07-15T08:00:00Z")
        )

        guard case .providerUnavailable = result else {
            return XCTFail("Expected provider unavailable state")
        }
    }

    func testFailureMapperPreservesUnsupportedDateRefreshDate() {
        let refreshDate = makeDate("2026-07-09T00:00:00Z")
        let result = EventWeatherProviderMapper.mapFailure(
            .unsupportedDate(refreshDate: refreshDate),
            fallbackRefreshDate: makeDate("2026-07-15T08:00:00Z")
        )

        guard case .pending(let mappedRefreshDate) = result else {
            return XCTFail("Expected pending state for unsupported date")
        }
        XCTAssertEqual(mappedRefreshDate, refreshDate)
    }

    func testWeatherKitErrorClassifierMapsEntitlementProviderAndUnsupportedDateFailures() {
        XCTAssertEqual(
            WeatherKitEventForecastProvider.mapError(fakeError("Missing WeatherKit entitlement")),
            .permissionOrEntitlementRequired
        )
        XCTAssertEqual(
            WeatherKitEventForecastProvider.mapError(fakeError("Forecast date outside supported date range")),
            .unsupportedDate(refreshDate: nil)
        )
        XCTAssertEqual(
            WeatherKitEventForecastProvider.mapError(fakeError("Weather service temporarily unavailable")),
            .providerUnavailable
        )
    }

    private func fakeError(_ message: String) -> NSError {
        NSError(domain: "WeatherKit", code: 1, userInfo: [NSLocalizedDescriptionKey: message])
    }

    private func makeDate(_ value: String) -> Date {
        ISO8601DateFormatter().date(from: value)!
    }
}
