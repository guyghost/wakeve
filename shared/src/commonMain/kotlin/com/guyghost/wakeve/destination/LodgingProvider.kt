package com.guyghost.wakeve.destination

import com.guyghost.wakeve.models.Destination
import com.guyghost.wakeve.models.ExternalAccommodation
import com.guyghost.wakeve.models.SuggestionSeason

/**
 * Provider d'hébergement.
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

object NoConfiguredLodgingProvider : LodgingProvider {
    override suspend fun fetchDestinations(
        from: List<String>,
        season: SuggestionSeason,
        groupSize: Int,
        duration: Int
    ): List<Destination> {
        error("Lodging provider is not configured")
    }

    override suspend fun fetchAccommodations(
        destination: String,
        groupSize: Int,
        duration: Int
    ): List<ExternalAccommodation> {
        error("Lodging provider is not configured")
    }
}

@Deprecated(
    message = "Inject a configured lodging provider in production or a deterministic provider in tests.",
    replaceWith = ReplaceWith("NoConfiguredLodgingProvider")
)
class MockLodgingProvider : LodgingProvider {
    override suspend fun fetchDestinations(
        from: List<String>,
        season: SuggestionSeason,
        groupSize: Int,
        duration: Int
    ): List<Destination> {
        error("Lodging provider is not configured")
    }

    override suspend fun fetchAccommodations(
        destination: String,
        groupSize: Int,
        duration: Int
    ): List<ExternalAccommodation> {
        error("Lodging provider is not configured")
    }
}
