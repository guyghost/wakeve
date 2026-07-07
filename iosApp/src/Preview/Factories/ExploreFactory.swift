import Foundation

#if DEBUG

enum ExploreFactory {
    static var previewData: ExplorePreviewData {
        ExplorePreviewData(
            selectedCategory: .all,
            trendingEvents: [
                event(
                    id: "explore-rooftop",
                    title: "Anniversaire rooftop",
                    description: "Soiree avec vote pour la date et options repas.",
                    eventType: "BIRTHDAY",
                    participantCount: 8,
                    maxParticipants: 12,
                    status: "POLLING",
                    locationName: "Marseille"
                ),
                event(
                    id: "explore-hike",
                    title: "Randonnee Calanques",
                    description: "Depart groupe, pique-nique et coucher de soleil.",
                    eventType: "OUTDOOR_ACTIVITY",
                    participantCount: 6,
                    maxParticipants: 10,
                    status: "DRAFT",
                    locationName: "Cassis"
                )
            ],
            nearbyEvents: [
                event(
                    id: "explore-dinner",
                    title: "Diner terrasse",
                    description: "Comparaison des restaurants et budget partage.",
                    eventType: "FOOD_TASTING",
                    participantCount: 5,
                    maxParticipants: 8,
                    status: "CONFIRMED",
                    locationName: "Vieux-Port"
                )
            ],
            recommendedEvents: [
                event(
                    id: "explore-team",
                    title: "Atelier produit",
                    description: "Workshop avec reunions et sondage de disponibilite.",
                    eventType: "WORKSHOP",
                    participantCount: 14,
                    maxParticipants: 20,
                    status: "POLLING",
                    locationName: "Aix-en-Provence"
                ),
                event(
                    id: "explore-yoga",
                    title: "Session bien-etre",
                    description: "Format calme pour aligner horaires et preferences.",
                    eventType: "WELLNESS_EVENT",
                    participantCount: 9,
                    maxParticipants: nil,
                    status: "DRAFT",
                    locationName: "Parc Borely"
                )
            ]
        )
    }

    private static func event(
        id: String,
        title: String,
        description: String,
        eventType: String,
        participantCount: Int,
        maxParticipants: Int?,
        status: String,
        locationName: String?
    ) -> ExploreEventItem {
        ExploreEventItem(
            id: id,
            title: title,
            description: description,
            eventType: eventType,
            participantCount: participantCount,
            maxParticipants: maxParticipants,
            status: status,
            locationName: locationName,
            deadline: "2026-06-15T18:00:00Z"
        )
    }
}

#endif
