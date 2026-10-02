import Foundation

/// Itinéraire vers le lieu du jour J : coordonnées connues, ou recherche par nom dans Plans.
enum EventDayDirections: Equatable {
    case coordinate(latitude: Double, longitude: Double, name: String)
    case search(query: String)

    /// Plans : itinéraire vers une adresse recherchée (`maps://?daddr=`).
    var searchURL: URL? {
        guard case .search(let query) = self else { return nil }
        var components = URLComponents()
        components.scheme = "maps"
        components.host = ""
        components.queryItems = [URLQueryItem(name: "daddr", value: query)]
        return components.url
    }

    init?(place: EventDayPlace?) {
        guard let place else { return nil }
        if let latitude = place.latitude, let longitude = place.longitude {
            self = .coordinate(latitude: latitude, longitude: longitude, name: place.name)
        } else {
            self = .search(query: [place.name, place.address].compactMap { $0 }.joined(separator: ", "))
        }
    }
}

/// Présentation pure du jour J (couche 8, #47), localisée pour `locale`, heures dans le fuseau du créneau.
struct EventDayModel: Equatable {
    struct Pill: Equatable {
        let text: String
        let systemImage: String
    }

    let title: String
    let caption: String
    /// Heure du rendez-vous (« 19:30 »), « Toute la journée », ou nil sans créneau daté.
    let time: String?
    /// « samedi 3 octobre ».
    let date: String?
    /// « jusqu'à 23:00 » quand la fin est le même jour.
    let until: String?
    let placeName: String
    let placeAddress: String?
    let coordinate: (latitude: Double, longitude: Double)?
    let pills: [Pill]
    let directions: EventDayDirections?
    let primaryTitle: String
    let secondaryTitle: String?
    let mapLabel: String?
    let palette: EventMoodPalette

    static func == (lhs: EventDayModel, rhs: EventDayModel) -> Bool {
        lhs.title == rhs.title && lhs.time == rhs.time && lhs.date == rhs.date && lhs.until == rhs.until
            && lhs.placeName == rhs.placeName && lhs.placeAddress == rhs.placeAddress && lhs.pills == rhs.pills
            && lhs.directions == rhs.directions && lhs.primaryTitle == rhs.primaryTitle
            && lhs.secondaryTitle == rhs.secondaryTitle && lhs.palette == rhs.palette
            && lhs.coordinate?.latitude == rhs.coordinate?.latitude && lhs.coordinate?.longitude == rhs.coordinate?.longitude
    }

    init(facts: EventDayFacts, locale: Locale = WK.appLocale) {
        func text(_ key: String) -> String { WK.localizedFormat(key, locale: locale) }
        title = facts.title
        caption = text("immersive.event_day.caption")
        palette = EventMoodPalette.palette(for: facts.eventTypeName)

        let zone = facts.slot?.timeZone ?? .current
        func formatter(_ template: String) -> DateFormatter {
            let formatter = DateFormatter()
            formatter.locale = locale
            formatter.timeZone = zone
            formatter.setLocalizedDateFormatFromTemplate(template)
            return formatter
        }
        let clock = formatter("jjmm")
        let start = facts.slot?.start
        if facts.slot?.isAllDay == true {
            time = text("immersive.event_day.all_day")
        } else {
            time = start.map(clock.string(from:))
        }
        date = start.map(formatter("EEEEdMMMM").string(from:))
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = zone
        if facts.slot?.isAllDay != true, let start, let end = facts.slot?.end, end > start, calendar.isDate(start, inSameDayAs: end) {
            until = String(format: text("immersive.event_day.until_format"), clock.string(from: end))
        } else {
            until = nil
        }

        placeName = facts.place?.name ?? text("immersive.event_day.no_place")
        placeAddress = facts.place?.address
        if let latitude = facts.place?.latitude, let longitude = facts.place?.longitude {
            coordinate = (latitude, longitude)
        } else {
            coordinate = nil
        }
        mapLabel = coordinate == nil ? nil : String(format: text("immersive.event_day.map_format"), placeName)

        var pills: [Pill] = []
        if let transport = facts.transport, let pill = HubModuleSheetData.transportPill(transport, locale: locale) {
            pills.append(Pill(text: pill.text, systemImage: "car"))
        }
        for meal in facts.meals {
            pills.append(Pill(
                text: String(format: text("hub.summary.format"), meal.name, meal.time),
                systemImage: "fork.knife"
            ))
        }
        if facts.confirmedCount + facts.pendingCount > 0 {
            pills.append(Pill(
                text: HubSummaryText.participants(confirmed: facts.confirmedCount, pending: facts.pendingCount, locale: locale),
                systemImage: "person.2"
            ))
        }
        self.pills = pills

        directions = EventDayDirections(place: facts.place)
        if directions != nil {
            primaryTitle = text("immersive.event_day.directions")
            secondaryTitle = text("immersive.view_event")
        } else {
            primaryTitle = text("immersive.view_event")
            secondaryTitle = nil
        }
    }
}

/// Jour J présenté en plein écran (`AuthenticatedView`).
struct EventDayPresentation: Identifiable, Equatable {
    let id: String
}
