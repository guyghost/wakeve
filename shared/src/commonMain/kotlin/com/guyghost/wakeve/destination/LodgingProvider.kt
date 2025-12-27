package com.guyghost.wakeve.destination

import com.guyghost.wakeve.models.*

/**
 * Provider d'hébergement (mocké pour l'instant)
 */
interface LodgingProvider {
    suspend fun fetchDestinations(
        from: List<String>,
        season: SuggestionSeason,
        groupSize: Int,
        duration: Int
    ): List<Destination>

    suspend fun fetchAccommodations(
        destination: String,
        groupSize: Int,
        duration: Int
    ): List<ExternalAccommodation>
}

class MockLodgingProvider : LodgingProvider {
    override suspend fun fetchDestinations(
        from: List<String>,
        season: SuggestionSeason,
        groupSize: Int,
        duration: Int
    ): List<Destination> {
        return listOf(
            Destination(
                id = "dest-1",
                name = "Paris",
                city = "Paris",
                country = "France",
                region = "Île-de-France",
                averageCostPerPerson = 150.0,
                popularityScore = 0.9,
                nearbyAttractions = 50,
                imageUrl = "https://example.com/paris.jpg",
                metadata = mapOf(
                    "season" to season.name
                )
            ),
            Destination(
                id = "dest-2",
                name = "Nice",
                city = "Nice",
                country = "France",
                region = "PACA",
                averageCostPerPerson = 200.0,
                popularityScore = 0.7,
                nearbyAttractions = 30,
                imageUrl = "https://example.com/nice.jpg",
                metadata = mapOf(
                    "season" to season.name
                )
            )
        )
    }

    override suspend fun fetchAccommodations(
        destination: String,
        groupSize: Int,
        duration: Int
    ): List<ExternalAccommodation> {
        return listOf(
            ExternalAccommodation(
                id = "acc-1",
                name = "Hôtel Centre",
                type = AccommodationType.HOTEL,
                location = Location(
                    address = "1 Rue de la République",
                    city = destination,
                    country = "France",
                    region = "",
                    latitude = 48.8566,
                    longitude = 2.3522,
                    nearbyAttractions = 20
                ),
                costPerNight = 120.0,
                rating = 4.5,
                amenities = listOf("wifi", "parking", "restaurant", "gym"),
                maxCapacity = 4,
                imageUrl = "https://example.com/hotel.jpg",
                provider = "Booking.com"
            ),
            ExternalAccommodation(
                id = "acc-2",
                name = "Appartement Centre",
                type = AccommodationType.AIRBNB,
                location = Location(
                    address = "2 Rue de la Paix",
                    city = destination,
                    country = "France",
                    region = "",
                    latitude = 48.8600,
                    longitude = 2.3600,
                    nearbyAttractions = 15
                ),
                costPerNight = 80.0,
                rating = 4.2,
                amenities = listOf("wifi", "kitchen"),
                maxCapacity = 6,
                imageUrl = "https://example.com/airbnb.jpg",
                provider = "Airbnb"
            )
        )
    }
}