import Foundation

/// Créneau retenu d'un événement (`confirmedDate` joint à `timeSlot`).
struct RetainedSlot: Equatable {
    let start: Date?
    let end: Date?
    /// Fuseau du créneau (`timeSlot.timezone`, ex. « Europe/Paris »).
    let timeZoneIdentifier: String
    /// `TimeOfDay.name` : ALL_DAY, MORNING, AFTERNOON, EVENING, SPECIFIC.
    let timeOfDayName: String?

    var timeZone: TimeZone? { TimeZone(identifier: timeZoneIdentifier) }
    var isAllDay: Bool { timeOfDayName == "ALL_DAY" }
}

/// Règle du jour J (couche 8, #47).
enum EventDayRule {
    /// Jour J = phase d'organisation (ou finalisé **sans** rollout invitation : avec lui, un événement finalisé
    /// part aux archives), accès aux détails accordé, et aujourd'hui est le jour de la date retenue dans le
    /// fuseau du créneau, ou maintenant ∈ [début, fin[ (créneau sur deux jours).
    static func isEventDay(
        phase: EventHubFacts.Phase,
        invitationRollout: Bool,
        finalDate: Date?,
        slotStart: Date?,
        slotEnd: Date?,
        timezone: String?,
        hasAccess: Bool,
        now: Date,
        calendar: Calendar = Calendar(identifier: .gregorian)
    ) -> Bool {
        let phaseAllowed = phase == .organizing || (phase == .finalized && !invitationRollout)
        guard phaseAllowed, hasAccess else { return false }
        var local = calendar
        if let timezone, let zone = TimeZone(identifier: timezone) {
            local.timeZone = zone
        }
        if let anchor = slotStart ?? finalDate, local.isDate(anchor, inSameDayAs: now) {
            return true
        }
        if let slotStart, let slotEnd, slotStart <= now, now < slotEnd {
            return true
        }
        return false
    }

    /// `yyyy-MM-dd` du jour de `date` dans `timeZone` (format de `meal.date`).
    static func dayString(_ date: Date, timeZone: TimeZone) -> String {
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = timeZone
        let parts = calendar.dateComponents([.year, .month, .day], from: date)
        return String(format: "%04d-%02d-%02d", parts.year ?? 0, parts.month ?? 0, parts.day ?? 0)
    }
}

/// Lieu du jour J : nom, adresse et coordonnées si connues.
struct EventDayPlace: Equatable {
    let name: String
    let address: String?
    let latitude: Double?
    let longitude: Double?

    var hasCoordinate: Bool { latitude != nil && longitude != nil }
}

/// Résolution pure du lieu du jour J (lecture seule, sans recherche réseau) : scénario retenu, sinon premier
/// lieu potentiel. Les coordonnées viennent du JSON des lieux potentiels (même format que la carte météo).
enum EventDayPlaceResolver {
    struct Scenario: Equatable {
        let location: String
        let sourcePotentialLocationId: String?
    }

    struct Location: Equatable {
        let id: String
        let name: String
        let address: String?
        /// `potentialLocation.coordinates` : `{"latitude": 48.8566, "longitude": 2.3522}`.
        let coordinatesJSON: String?
    }

    static func coordinates(from json: String?) -> (latitude: Double, longitude: Double)? {
        guard let data = json?.data(using: .utf8),
              let object = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
              let latitude = (object["latitude"] as? NSNumber)?.doubleValue,
              let longitude = (object["longitude"] as? NSNumber)?.doubleValue,
              (-90...90).contains(latitude), (-180...180).contains(longitude) else {
            return nil
        }
        return (latitude, longitude)
    }

    /// `potentialLocations` dans l'ordre de création (`selectByEventId`).
    static func resolve(selectedScenario: Scenario?, potentialLocations: [Location]) -> EventDayPlace? {
        if let scenario = selectedScenario {
            let name = scenario.location.trimmingCharacters(in: .whitespacesAndNewlines)
            if !name.isEmpty {
                let source = potentialLocations.first { $0.id == scenario.sourcePotentialLocationId }
                    ?? potentialLocations.first {
                        $0.name.trimmingCharacters(in: .whitespacesAndNewlines).localizedCaseInsensitiveCompare(name) == .orderedSame
                    }
                return place(name: name, location: source)
            }
        }
        guard let first = potentialLocations.first(where: {
            !$0.name.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
        }) else { return nil }
        return place(name: first.name.trimmingCharacters(in: .whitespacesAndNewlines), location: first)
    }

    private static func place(name: String, location: Location?) -> EventDayPlace {
        let coordinate = coordinates(from: location?.coordinatesJSON)
        let address = location?.address?.trimmingCharacters(in: .whitespacesAndNewlines)
        return EventDayPlace(
            name: name,
            address: address?.isEmpty == false ? address : nil,
            latitude: coordinate?.latitude,
            longitude: coordinate?.longitude
        )
    }
}

/// Repas du jour (`meal.getMealsByDate`).
struct EventDayMeal: Equatable {
    let name: String
    /// `HH:mm`.
    let time: String
    /// `MealStatus.name`.
    let statusName: String

    /// Repas non annulés, par heure.
    static func today(_ meals: [EventDayMeal]) -> [EventDayMeal] {
        meals.filter { $0.statusName != "CANCELLED" }.sorted { $0.time < $1.time }
    }
}

/// Faits du jour J, vus par l'utilisateur courant, lus par `SharedEventDaySource`.
struct EventDayFacts: Equatable {
    let eventId: String
    let title: String
    let eventTypeName: String?
    let phase: EventHubFacts.Phase
    let hasAccess: Bool
    let slot: RetainedSlot?
    let place: EventDayPlace?
    /// État du transport (même règle que la sheet Transport), nil s'il n'est pas lisible.
    let transport: HubModuleSheetData.TransportState?
    /// Repas du jour, non annulés, par heure.
    let meals: [EventDayMeal]
    let confirmedCount: Int
    let pendingCount: Int
    /// Date retenue de l'événement (`event.finalDate`) : repli quand le créneau n'a pas de début (créneau
    /// flexible) ou qu'aucune ligne `confirmedDate` n'existe (date héritée).
    var finalDate: Date? = nil

    func isEventDay(now: Date, invitationRollout: Bool) -> Bool {
        EventDayRule.isEventDay(
            phase: phase,
            invitationRollout: invitationRollout,
            finalDate: finalDate,
            slotStart: slot?.start,
            slotEnd: slot?.end,
            timezone: slot?.timeZoneIdentifier,
            hasAccess: hasAccess,
            now: now
        )
    }
}
