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

    func testCachedProviderReusesFreshResultForSamePlaceAndDate() async {
        var now = makeDate("2026-07-15T08:00:00Z")
        let upstream = StubEventWeatherProvider()
        let provider = CachedEventWeatherProvider(
            upstream: upstream,
            ttlSeconds: 600,
            now: { now }
        )
        let place = makePlace()
        let targetDate = makeDate("2026-07-18T10:00:00Z")

        _ = await provider.forecast(for: place, targetDate: targetDate, fetchedAt: now)
        _ = await provider.forecast(for: place, targetDate: targetDate, fetchedAt: now.addingTimeInterval(5))

        let callCountBeforeExpiry = await upstream.callCount
        XCTAssertEqual(callCountBeforeExpiry, 1)

        now = now.addingTimeInterval(601)
        _ = await provider.forecast(for: place, targetDate: targetDate, fetchedAt: now)

        let callCountAfterExpiry = await upstream.callCount
        XCTAssertEqual(callCountAfterExpiry, 2)
    }

    func testCachedProviderCoalescesConcurrentRequestsForSamePlaceAndDate() async {
        let upstream = StubEventWeatherProvider(delayNanoseconds: 50_000_000)
        let provider = CachedEventWeatherProvider(
            upstream: upstream,
            ttlSeconds: 600,
            now: { self.makeDate("2026-07-15T08:00:00Z") }
        )
        let place = makePlace()
        let targetDate = makeDate("2026-07-18T10:00:00Z")
        let fetchedAt = makeDate("2026-07-15T08:00:00Z")

        async let first = provider.forecast(for: place, targetDate: targetDate, fetchedAt: fetchedAt)
        async let second = provider.forecast(for: place, targetDate: targetDate, fetchedAt: fetchedAt)
        _ = await [first, second]

        let callCount = await upstream.callCount
        XCTAssertEqual(callCount, 1)
    }

    private func fakeError(_ message: String) -> NSError {
        NSError(domain: "WeatherKit", code: 1, userInfo: [NSLocalizedDescriptionKey: message])
    }

    private func makePlace() -> EventWeatherPlace {
        EventWeatherPlace(
            name: "Biarritz",
            coordinate: CLLocationCoordinate2D(latitude: 43.4832, longitude: -1.5586)
        )
    }

    private func makeDate(_ value: String) -> Date {
        ISO8601DateFormatter().date(from: value)!
    }

    private actor StubEventWeatherProvider: EventWeatherProviding {
        private let delayNanoseconds: UInt64
        private var calls = 0

        init(delayNanoseconds: UInt64 = 0) {
            self.delayNanoseconds = delayNanoseconds
        }

        var callCount: Int {
            calls
        }

        func forecast(
            for place: EventWeatherPlace,
            targetDate: Date,
            fetchedAt: Date
        ) async -> EventWeatherProviderResult {
            calls += 1

            if delayNanoseconds > 0 {
                try? await Task.sleep(nanoseconds: delayNanoseconds)
            }

            return .available(
                EventWeatherSummary(
                    placeName: place.name,
                    coordinate: place.coordinate,
                    condition: "Sunny",
                    symbolName: "sun.max",
                    lowTemperature: 20,
                    highTemperature: 28,
                    precipitationChance: 0.1,
                    windSpeedKph: 12,
                    fetchedAt: fetchedAt
                )
            )
        }
    }
}
