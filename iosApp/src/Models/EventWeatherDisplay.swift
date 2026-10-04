import Foundation

/// Quand montrer la météo de l'événement (hub, jour J ; revue couche 9, #47). Règle de l'ancienne carte météo :
/// date retenue (confirmé, organisation, finalisé), accès aux détails, jour cible dans la fenêtre du fournisseur.
enum EventWeatherRule {
    /// Fenêtre des prévisions journalières (WeatherKit) : aujourd'hui et les 10 jours suivants.
    static let windowDays = 10

    static func applies(phase: EventHubFacts.Phase, hasAccess: Bool) -> Bool {
        hasAccess && [.confirmed, .organizing, .finalized].contains(phase)
    }

    /// Début du créneau retenu, sinon date retenue de l'événement.
    static func targetDate(slotStart: Date?, finalDate: Date?) -> Date? {
        slotStart ?? finalDate
    }

    /// Jour cible entre aujourd'hui (inclus, même si l'heure est passée) et aujourd'hui + 10 jours.
    static func isInWindow(target: Date, now: Date, calendar: Calendar = .current) -> Bool {
        guard let days = calendar.dateComponents(
            [.day], from: calendar.startOfDay(for: now), to: calendar.startOfDay(for: target)
        ).day else { return false }
        return (0...windowDays).contains(days)
    }

    /// Date à charger, ou nil quand rien ne doit s'afficher.
    static func loadTarget(
        phase: EventHubFacts.Phase,
        hasAccess: Bool,
        slotStart: Date?,
        finalDate: Date?,
        now: Date,
        calendar: Calendar = .current
    ) -> Date? {
        guard applies(phase: phase, hasAccess: hasAccess),
              let target = targetDate(slotStart: slotStart, finalDate: finalDate),
              isInWindow(target: target, now: now, calendar: calendar) else { return nil }
        return target
    }
}

/// Lieu de la prévision, comme l'ancien `EventWeatherViewModel` : coordonnées connues (lieu du jour J :
/// scénario retenu ou lieu potentiel), sinon lieu déjà géocodé de l'événement (`resolvedEventLocation`),
/// sinon recherche MapKit par nom et adresse.
enum EventWeatherPlaceChoice: Equatable {
    struct Resolved: Equatable {
        let label: String
        let latitude: Double
        let longitude: Double
    }

    case coordinate(name: String, latitude: Double, longitude: Double)
    case search(query: String)
    case none

    static func choose(place: EventDayPlace?, resolved: Resolved?) -> EventWeatherPlaceChoice {
        if let place, let latitude = place.latitude, let longitude = place.longitude {
            return .coordinate(name: place.name, latitude: latitude, longitude: longitude)
        }
        if let resolved {
            return .coordinate(name: resolved.label, latitude: resolved.latitude, longitude: resolved.longitude)
        }
        let query = [place?.name, place?.address]
            .compactMap { $0?.trimmingCharacters(in: .whitespacesAndNewlines) }
            .filter { !$0.isEmpty }
            .joined(separator: ", ")
        return query.isEmpty ? .none : .search(query: query)
    }
}

/// Prévision affichée.
struct EventWeatherDisplay: Equatable {
    let placeName: String
    let condition: String
    let symbolName: String
    /// °C.
    let low: Double
    let high: Double
    /// 0...1.
    let precipitationChance: Double
}

enum EventWeatherState: Equatable {
    case hidden, loading, available(EventWeatherDisplay), unavailable

    /// Hors fenêtre (`pending`) : rien ; fournisseur ou droits indisponibles : mention discrète.
    init(result: EventWeatherProviderResult) {
        switch result {
        case .available(let summary):
            self = .available(EventWeatherDisplay(
                placeName: summary.placeName,
                condition: summary.condition,
                symbolName: summary.symbolName,
                low: summary.lowTemperature,
                high: summary.highTemperature,
                precipitationChance: summary.precipitationChance
            ))
        case .pending:
            self = .hidden
        case .permissionOrEntitlementRequired, .providerUnavailable:
            self = .unavailable
        }
    }
}

enum EventWeatherText {
    /// « 12° / 21° » (unité de la langue : °F en anglais américain).
    static func temperatures(_ display: EventWeatherDisplay, locale: Locale = WK.appLocale) -> String {
        let formatter = MeasurementFormatter()
        formatter.locale = locale
        formatter.unitStyle = .short
        formatter.unitOptions = .temperatureWithoutUnit
        formatter.numberFormatter.maximumFractionDigits = 0
        func text(_ celsius: Double) -> String {
            formatter.string(from: Measurement(value: celsius, unit: UnitTemperature.celsius))
        }
        return "\(text(display.low)) / \(text(display.high))"
    }

    /// « Pluie : 30 % ».
    static func rain(_ display: EventWeatherDisplay, locale: Locale = WK.appLocale) -> String {
        let percent = Int((display.precipitationChance * 100).rounded())
        return String(format: WK.localizedFormat("hub.weather.rain_format", locale: locale), locale: locale, percent)
    }

    /// « Météo · Annecy ».
    static func title(_ display: EventWeatherDisplay, locale: Locale = WK.appLocale) -> String {
        String(format: WK.localizedFormat("hub.weather.title_format", locale: locale), display.placeName)
    }
}
