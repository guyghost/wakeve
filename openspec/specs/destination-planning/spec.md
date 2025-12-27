# Destination Planning Specification

## Version
**Version**: 1.0.0
**Status**: ✅ Implémenté
**Date de création**: 26 décembre 2025
**Auteur**: Équipe Wakeve

## Overview

Le système de Destination Planning de Wakeve fournit des suggestions de destinations et d'hébergements basées sur les préférences utilisateur, la saisonnalité, la taille du groupe et d'autres critères multi-facteurs.

## Domain Model

### Core Concepts

- **Destination Suggestion**: Recommandations de lieux/villes basées sur les préférences
- **Lodging Suggestion**: Recommandations d'hébergements basées sur la destination
- **Multi-Criteria Scoring**: Score composite basé sur coût, accessibilité, popularité, saisonnalité
- **Provider Integration**: Intégration avec APIs externes (mockées pour l'instant)

### Destination

```kotlin
@Serializable
data class Destination(
    val id: String,
    val name: String,
    val country: String,
    val region: String,
    val latitude: Double,
    val longitude: Double,
    val averageCostPerNight: Double,
    val currency: String,
    val bestSeasons: List<Season>,
    val accessibilityScore: Double,  // 0.0-1.0
    val popularityScore: Double,     // 0.0-1.0
    val imageUrl: String?,
    val description: String?,
    val tags: List<String>  // e.g., ["beach", "mountain", "city"]
)
```

### Lodging

```kotlin
@Serializable
data class Lodging(
    val id: String,
    val destinationId: String,
    val name: String,
    val type: LodgingType,
    val address: String,
    val pricePerNight: Double,
    val currency: String,
    val maxOccupancy: Int,
    val bedrooms: Int,
    val bathrooms: Int,
    val amenities: List<String>,
    val rating: Double,         // 0.0-5.0
    val reviewCount: Int,
    val imageUrl: String?,
    val provider: String,        // "mock", "airbnb", "booking.com", etc.
    val providerUrl: String?,
    val availability: AvailabilityStatus
)
```

### LodgingType

```kotlin
enum class LodgingType {
    HOTEL,
    APARTMENT,
    HOUSE,
    VILLA,
    HOSTEL,
    CAMPING,
    BED_AND_BREAKFAST,
    CHALET,
    OTHER
}
```

### AvailabilityStatus

```kotlin
enum class AvailabilityStatus {
    AVAILABLE,
    LIMITED,
    FULL,
    UNKNOWN
}
```

### Season

```kotlin
enum class Season {
    WINTER,
    SPRING,
    SUMMER,
    FALL,
    ALL_YEAR
}
```

### DestinationSuggestion

```kotlin
@Serializable
data class DestinationSuggestion(
    val destination: Destination,
    val overallScore: Double,        // 0.0-1.0
    val costScore: Double,           // 0.0-1.0 (1 = dans le budget)
    val accessibilityScore: Double,   // 0.0-1.0 (1 = très accessible)
    val seasonalityScore: Double,     // 0.0-1.0 (1 = bonne saison)
    val popularityScore: Double,      // 0.0-1.0 (1 = très populaire)
    val personalizationScore: Double, // 0.0-1.0 (1 = correspond parfaitement)
    val reasons: List<String>
)
```

### LodgingSuggestion

```kotlin
@Serializable
data class LodgingSuggestion(
    val lodging: Lodging,
    val overallScore: Double,        // 0.0-1.0
    val costScore: Double,           // 0.0-1.0
    val capacityScore: Double,        // 0.0-1.0 (1 = capacité adaptée)
    val amenityScore: Double,        // 0.0-1.0 (1 = beaucoup d'équipements)
    val ratingScore: Double,         // 0.0-1.0 (1 = excellentes avis)
    val reasons: List<String>
)
```

### DestinationSearchCriteria

```kotlin
@Serializable
data class DestinationSearchCriteria(
    val eventId: String,
    val participantCount: Int,
    val budgetRange: BudgetRange,
    val preferredSeasons: List<Season>,
    val preferredRegions: List<String>,
    val preferredActivities: List<String>,
    val maxGroupSize: Int,
    val preferredLodgingTypes: List<LodgingType>
)

@Serializable
data class BudgetRange(
    val min: Double,
    val max: Double,
    val currency: String,
    val isPerNight: Boolean = true
)
```

## DestinationService

### Responsibilities

**Génération de suggestions de destinations**:
- Basé sur les préférences utilisateur
- Multi-criteria scoring (coût, accessibilité, saisonnalité, popularité, personnalisation)
- Filtrage par région, saison, budget

**Génération de suggestions d'hébergements**:
- Basé sur la destination sélectionnée
- Capacité adaptée à la taille du groupe
- Score par équipements, avis, prix

**Intégration avec providers**:
- Mock providers pour l'instant
- Structure pour APIs externes (Google Places, Airbnb, Booking.com, etc.)

### API

```kotlin
class DestinationService(
    private val database: WakevDb,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val destinationProvider: DestinationProvider
) {

    /**
     * Suggère des destinations basées sur les critères
     */
    suspend fun suggestDestinations(
        criteria: DestinationSearchCriteria,
        limit: Int = 10
    ): Result<List<DestinationSuggestion>> {
        val destinations = destinationProvider.searchDestinations(
            regions = criteria.preferredRegions,
            seasons = criteria.preferredSeasons,
            activities = criteria.preferredActivities
        )

        val scoredDestinations = destinations.map { destination ->
            val scores = calculateDestinationScores(
                destination = destination,
                criteria = criteria
            )

            DestinationSuggestion(
                destination = destination,
                overallScore = scores.overall,
                costScore = scores.cost,
                accessibilityScore = scores.accessibility,
                seasonalityScore = scores.seasonality,
                popularityScore = scores.popularity,
                personalizationScore = scores.personalization,
                reasons = generateReasons(scores, destination)
            )
        }.sortedByDescending { it.overallScore }

        return Result.success(scoredDestinations.take(limit))
    }

    /**
     * Suggère des hébergements pour une destination
     */
    suspend fun suggestLodgings(
        destinationId: String,
        participantCount: Int,
        budgetRange: BudgetRange,
        preferredTypes: List<LodgingType>,
        limit: Int = 10
    ): Result<List<LodgingSuggestion>> {
        val lodgings = destinationProvider.searchLodgings(
            destinationId = destinationId,
            types = preferredTypes,
            maxOccupancy = participantCount
        )

        val scoredLodgings = lodgings.map { lodging ->
            val scores = calculateLodgingScores(
                lodging = lodging,
                participantCount = participantCount,
                budgetRange = budgetRange
            )

            LodgingSuggestion(
                lodging = lodging,
                overallScore = scores.overall,
                costScore = scores.cost,
                capacityScore = scores.capacity,
                amenityScore = scores.amenity,
                ratingScore = scores.rating,
                reasons = generateLodgingReasons(scores, lodging)
            )
        }.sortedByDescending { it.overallScore }

        return Result.success(scoredLodgings.take(limit))
    }

    /**
     * Obtient les détails d'une destination
     */
    suspend fun getDestination(destinationId: String): Destination? {
        return destinationProvider.getDestinationById(destinationId)
    }

    /**
     * Obtient les détails d'un hébergement
     */
    suspend fun getLodging(lodgingId: String): Lodging? {
        return destinationProvider.getLodgingById(lodgingId)
    }

    private fun calculateDestinationScores(
        destination: Destination,
        criteria: DestinationSearchCriteria
    ): DestinationScores {
        // Coût: Dans le budget ou proche
        val costScore = calculateCostScore(
            averageCost = destination.averageCostPerNight,
            budgetRange = criteria.budgetRange
        )

        // Accessibilité: Facilité d'accès (transport)
        val accessibilityScore = destination.accessibilityScore

        // Saisonnalité: Bonne saison pour la destination
        val seasonalityScore = calculateSeasonalityScore(
            destinationSeasons = destination.bestSeasons,
            preferredSeasons = criteria.preferredSeasons
        )

        // Popularité: Évaluations de la communauté
        val popularityScore = destination.popularityScore

        // Personnalisation: Correspondance activités/régions
        val personalizationScore = calculatePersonalizationScore(
            destinationTags = destination.tags,
            destinationRegion = destination.region,
            preferredActivities = criteria.preferredActivities,
            preferredRegions = criteria.preferredRegions
        )

        // Score composite
        val overallScore = (costScore * 0.30) +
                          (accessibilityScore * 0.20) +
                          (seasonalityScore * 0.15) +
                          (popularityScore * 0.10) +
                          (personalizationScore * 0.25)

        return DestinationScores(
            overall = overallScore,
            cost = costScore,
            accessibility = accessibilityScore,
            seasonality = seasonalityScore,
            popularity = popularityScore,
            personalization = personalizationScore
        )
    }

    private fun calculateCostScore(
        averageCost: Double,
        budgetRange: BudgetRange
    ): Double {
        return when {
            averageCost <= budgetRange.max -> 1.0
            averageCost <= budgetRange.max * 1.5 -> 0.5
            else -> 0.0
        }
    }

    private fun calculateSeasonalityScore(
        destinationSeasons: List<Season>,
        preferredSeasons: List<Season>
    ): Double {
        val intersection = destinationSeasons.intersect(preferredSeasons.toSet())

        return when {
            intersection.isNotEmpty() -> 1.0
            destinationSeasons.contains(Season.ALL_YEAR) -> 0.8
            else -> 0.3
        }
    }

    private fun calculatePersonalizationScore(
        destinationTags: List<String>,
        destinationRegion: String,
        preferredActivities: List<String>,
        preferredRegions: List<String>
    ): Double {
        val activityMatches = destinationTags.intersect(preferredActivities.toSet()).size
        val regionMatches = if (destinationRegion in preferredRegions) 1 else 0

        val totalCriteria = preferredActivities.size + preferredRegions.size
        val matches = activityMatches + regionMatches

        return if (totalCriteria > 0) {
            matches.toDouble() / totalCriteria.toDouble()
        } else {
            0.0
        }
    }

    private fun calculateLodgingScores(
        lodging: Lodging,
        participantCount: Int,
        budgetRange: BudgetRange
    ): LodgingScores {
        // Coût: Dans le budget par nuit
        val costScore = calculateLodgingCostScore(
            pricePerNight = lodging.pricePerNight,
            budgetRange = budgetRange
        )

        // Capacité: Adaptée à la taille du groupe
        val capacityScore = calculateCapacityScore(
            maxOccupancy = lodging.maxOccupancy,
            participantCount = participantCount
        )

        // Équipements: Nombre d'équipements disponibles
        val amenityScore = calculateAmenityScore(
            amenities = lodging.amenities
        )

        // Avis: Note des utilisateurs
        val ratingScore = (lodging.rating / 5.0).coerceIn(0.0, 1.0)

        // Score composite
        val overallScore = (costScore * 0.35) +
                          (capacityScore * 0.25) +
                          (amenityScore * 0.20) +
                          (ratingScore * 0.20)

        return LodgingScores(
            overall = overallScore,
            cost = costScore,
            capacity = capacityScore,
            amenity = amenityScore,
            rating = ratingScore
        )
    }

    private fun calculateLodgingCostScore(
        pricePerNight: Double,
        budgetRange: BudgetRange
    ): Double {
        return when {
            pricePerNight <= budgetRange.max -> 1.0
            pricePerNight <= budgetRange.max * 1.5 -> 0.5
            else -> 0.0
        }
    }

    private fun calculateCapacityScore(
        maxOccupancy: Int,
        participantCount: Int
    ): Double {
        return when {
            maxOccupancy >= participantCount -> 1.0
            maxOccupancy >= participantCount * 0.8 -> 0.7
            maxOccupancy >= participantCount * 0.5 -> 0.4
            else -> 0.0
        }
    }

    private fun calculateAmenityScore(
        amenities: List<String>
    ): Double {
        return (amenities.size.toDouble() / 20.0).coerceAtMost(1.0)
    }

    private fun generateReasons(
        scores: DestinationScores,
        destination: Destination
    ): List<String> {
        val reasons = mutableListOf<String>()

        if (scores.cost >= 0.8) {
            reasons.add("Dans votre budget")
        }
        if (scores.accessibility >= 0.8) {
            reasons.add("Très accessible")
        }
        if (scores.seasonality >= 0.8) {
            reasons.add("Bonne saison")
        }
        if (scores.popularity >= 0.7) {
            reasons.add("Populaire")
        }
        if (scores.personalization >= 0.7) {
            reasons.add("Correspond à vos préférences")
        }

        return reasons
    }

    private fun generateLodgingReasons(
        scores: LodgingScores,
        lodging: Lodging
    ): List<String> {
        val reasons = mutableListOf<String>()

        if (scores.cost >= 0.8) {
            reasons.add("Bon rapport qualité-prix")
        }
        if (scores.capacity >= 0.8) {
            reasons.add("Capacité adaptée à votre groupe")
        }
        if (scores.amenity >= 0.6) {
            reasons.add("Nombreux équipements")
        }
        if (scores.rating >= 0.7) {
            reasons.add("Excellents avis")
        }

        return reasons
    }

    private data class DestinationScores(
        val overall: Double,
        val cost: Double,
        val accessibility: Double,
        val seasonality: Double,
        val popularity: Double,
        val personalization: Double
    )

    private data class LodgingScores(
        val overall: Double,
        val cost: Double,
        val capacity: Double,
        val amenity: Double,
        val rating: Double
    )
}
```

## DestinationProvider

### Mock Implementation

```kotlin
class DestinationProvider : DestinationProvider {

    private val destinations = listOf(
        Destination(
            id = "dest-1",
            name = "Nice",
            country = "France",
            region = "Provence-Alpes-Côte d'Azur",
            latitude = 43.7102,
            longitude = 7.2620,
            averageCostPerNight = 150.0,
            currency = "EUR",
            bestSeasons = listOf(Season.SUMMER, Season.SPRING, Season.FALL),
            accessibilityScore = 0.9,
            popularityScore = 0.85,
            imageUrl = null,
            description = "Beautiful Mediterranean city with beaches",
            tags = listOf("beach", "city", "culture", "food")
        ),
        Destination(
            id = "dest-2",
            name = "Chamonix",
            country = "France",
            region = "Auvergne-Rhône-Alpes",
            latitude = 45.9237,
            longitude = 6.8694,
            averageCostPerNight = 200.0,
            currency = "EUR",
            bestSeasons = listOf(Season.WINTER, Season.SUMMER),
            accessibilityScore = 0.7,
            popularityScore = 0.75,
            imageUrl = null,
            description = "Mountain resort near Mont Blanc",
            tags = listOf("mountain", "ski", "nature", "adventure")
        ),
        Destination(
            id = "dest-3",
            name = "Bordeaux",
            country = "France",
            region = "Nouvelle-Aquitaine",
            latitude = 44.8378,
            longitude = -0.5792,
            averageCostPerNight = 130.0,
            currency = "EUR",
            bestSeasons = listOf(Season.ALL_YEAR),
            accessibilityScore = 0.95,
            popularityScore = 0.80,
            imageUrl = null,
            description = "Famous wine region with UNESCO heritage",
            tags = listOf("city", "wine", "culture", "food", "history")
        )
    )

    private val lodgings = listOf(
        Lodging(
            id = "lodging-1",
            destinationId = "dest-1",
            name = "Hotel Nice Etoile",
            type = LodgingType.HOTEL,
            address = "123 Promenade des Anglais, Nice",
            pricePerNight = 120.0,
            currency = "EUR",
            maxOccupancy = 2,
            bedrooms = 1,
            bathrooms = 1,
            amenities = listOf("wifi", "parking", "pool", "restaurant", "bar", "gym"),
            rating = 4.5,
            reviewCount = 120,
            imageUrl = null,
            provider = "mock",
            providerUrl = null,
            availability = AvailabilityStatus.AVAILABLE
        ),
        Lodging(
            id = "lodging-2",
            destinationId = "dest-1",
            name = "Nice Beach Apartment",
            type = LodgingType.APARTMENT,
            address = "45 Rue de France, Nice",
            pricePerNight = 180.0,
            currency = "EUR",
            maxOccupancy = 4,
            bedrooms = 2,
            bathrooms = 1,
            amenities = listOf("wifi", "kitchen", "washing-machine", "tv", "balcony"),
            rating = 4.2,
            reviewCount = 45,
            imageUrl = null,
            provider = "mock",
            providerUrl = null,
            availability = AvailabilityStatus.AVAILABLE
        )
    )

    override suspend fun searchDestinations(
        regions: List<String>,
        seasons: List<Season>,
        activities: List<String>
    ): List<Destination> {
        return destinations.filter { destination ->
            val regionMatch = regions.isEmpty() || destination.region in regions
            val seasonMatch = seasons.isEmpty() ||
                           destination.bestSeasons.intersect(seasons.toSet()).isNotEmpty()
            val activityMatch = activities.isEmpty() ||
                             destination.tags.intersect(activities.toSet()).isNotEmpty()

            regionMatch && seasonMatch && activityMatch
        }
    }

    override suspend fun searchLodgings(
        destinationId: String,
        types: List<LodgingType>,
        maxOccupancy: Int
    ): List<Lodging> {
        return lodgings.filter { lodging ->
            val destinationMatch = lodging.destinationId == destinationId
            val typeMatch = types.isEmpty() || lodging.type in types
            val capacityMatch = lodging.maxOccupancy >= maxOccupancy

            destinationMatch && typeMatch && capacityMatch
        }
    }

    override suspend fun getDestinationById(destinationId: String): Destination? {
        return destinations.find { it.id == destinationId }
    }

    override suspend fun getLodgingById(lodgingId: String): Lodging? {
        return lodgings.find { it.id == lodgingId }
    }
}
```

## API Endpoints

```
POST   /api/events/{id}/destinations/suggest        # Suggérer destinations
GET    /api/events/{id}/destinations/{destinationId}  # Détails destination
POST   /api/events/{id}/lodgings/suggest           # Suggérer hébergements
GET    /api/events/{id}/lodgings/{lodgingId}         # Détails hébergement
GET    /api/destinations/search                     # Recherche destinations
GET    /api/lodgings/search                         # Recherche hébergements
GET    /api/destinations/regions                     # Liste des régions
GET    /api/destinations/activities                 # Liste des activités
```

## Scenarios

### SCENARIO 1: Suggest Destinations

**GIVEN**: Utilisateur préférences été, plage, sud de la France, budget 100-200€/nuit
**WHEN**: Organisateur demande suggestions
**THEN**: Système retourne:
  - Nice (score: 0.85)
  - Bordeaux (score: 0.78)
  - Marseille (score: 0.75)
**AND**: Raisons explicites (ex: "Bonne saison", "Correspond à vos préférences")

```kotlin
val criteria = DestinationSearchCriteria(
    eventId = "event-1",
    participantCount = 4,
    budgetRange = BudgetRange(100.0, 200.0, "EUR"),
    preferredSeasons = listOf(Season.SUMMER),
    preferredRegions = listOf("Provence-Alpes-Côte d'Azur", "Nouvelle-Aquitaine"),
    preferredActivities = listOf("beach", "swimming"),
    maxGroupSize = 10,
    preferredLodgingTypes = emptyList()
)

val suggestions = destinationService.suggestDestinations(criteria)

assertTrue(suggestions.getOrThrow().isNotEmpty())
assertTrue(suggestions.getOrThrow()[0].seasonalityScore > 0.8)
```

### SCENARIO 2: Suggest Lodgings

**GIVEN**: Destination Nice sélectionnée, 4 participants, budget 100-200€/nuit
**WHEN**: Organisateur demande suggestions d'hébergements
**THEN**: Système retourne:
  - Hotel Nice Etoile (score: 0.82, capacité: 2)
  - Nice Beach Apartment (score: 0.88, capacité: 4)
**AND**: Priorité à capacité adaptée au groupe

```kotlin
val lodgings = destinationService.suggestLodgings(
    destinationId = "dest-1",
    participantCount = 4,
    budgetRange = BudgetRange(100.0, 200.0, "EUR"),
    preferredTypes = listOf(LodgingType.APARTMENT, LodgingType.HOTEL)
)

assertTrue(lodgings.getOrThrow().isNotEmpty())
assertTrue(lodgings.getOrThrow()[0].capacityScore > 0.7)
```

### SCENARIO 3: Filter by Budget

**GIVEN**: Budget strict de 100€/nuit
**WHEN**: Organisateur demande suggestions
**THEN**: Seuls les hébergements ≤ 100€/nuit suggérés
**AND**: Score coût = 1.0 pour tous

```kotlin
val lodgings = destinationService.suggestLodgings(
    destinationId = "dest-1",
    participantCount = 4,
    budgetRange = BudgetRange(0.0, 100.0, "EUR"),
    preferredTypes = emptyList()
)

val affordableLodgings = lodgings.getOrThrow().filter { it.lodging.pricePerNight <= 100.0 }
assertEquals(affordableLodgings, lodgings.getOrThrow())
```

### SCENARIO 4: Seasonality Matching

**GIVEN**: Préférence hiver, activités ski
**WHEN**: Organisateur demande suggestions
**THEN**: Destinations hivernales suggérées (Chamonix, etc.)
**AND**: Score saisonnalité = 1.0

```kotlin
val criteria = DestinationSearchCriteria(
    eventId = "event-1",
    participantCount = 4,
    budgetRange = BudgetRange(0.0, 300.0, "EUR"),
    preferredSeasons = listOf(Season.WINTER),
    preferredRegions = emptyList(),
    preferredActivities = listOf("ski", "mountain"),
    maxGroupSize = 10,
    preferredLodgingTypes = emptyList()
)

val suggestions = destinationService.suggestDestinations(criteria)

assertTrue(suggestions.getOrThrow().any {
    it.destination.name == "Chamonix" &&
    it.seasonalityScore == 1.0
})
```

### SCENARIO 5: Personalization

**GIVEN**: Préférences vin, culture, histoire
**WHEN**: Organisateur demande suggestions
**THEN**: Bordeaux suggéré avec score personnalisation élevé
**AND**: Raison: "Correspond à vos préférences"

```kotlin
val criteria = DestinationSearchCriteria(
    eventId = "event-1",
    participantCount = 4,
    budgetRange = BudgetRange(0.0, 200.0, "EUR"),
    preferredSeasons = emptyList(),
    preferredRegions = emptyList(),
    preferredActivities = listOf("wine", "culture", "history"),
    maxGroupSize = 10,
    preferredLodgingTypes = emptyList()
)

val suggestions = destinationService.suggestDestinations(criteria)

val bordeaux = suggestions.getOrThrow().find { it.destination.name == "Bordeaux" }
assertNotNull(bordeaux)
assertTrue(bordeaux!!.personalizationScore > 0.7)
assertTrue(bordeaux.reasons.contains("Correspond à vos préférences"))
```

## Testing

### Unit Tests

#### DestinationServiceTest

```kotlin
class DestinationServiceTest {
    @Test
    fun `suggestDestinations filters by region`() {
        // Given
        val criteria = DestinationSearchCriteria(
            eventId = "event-1",
            participantCount = 4,
            budgetRange = BudgetRange(0.0, 200.0, "EUR"),
            preferredRegions = listOf("Provence-Alpes-Côte d'Azur"),
            preferredSeasons = emptyList(),
            preferredActivities = emptyList(),
            maxGroupSize = 10,
            preferredLodgingTypes = emptyList()
        )

        // When
        val result = destinationService.suggestDestinations(criteria)

        // Then
        assertTrue(result.isSuccess)
        val suggestions = result.getOrThrow()
        assertTrue(suggestions.isNotEmpty())
        assertTrue(suggestions.all {
            it.destination.region == "Provence-Alpes-Côte d'Azur"
        })
    }

    @Test
    fun `suggestDestinations respects budget`() {
        // Given
        val criteria = DestinationSearchCriteria(
            eventId = "event-1",
            participantCount = 4,
            budgetRange = BudgetRange(0.0, 100.0, "EUR"),
            preferredRegions = emptyList(),
            preferredSeasons = emptyList(),
            preferredActivities = emptyList(),
            maxGroupSize = 10,
            preferredLodgingTypes = emptyList()
        )

        // When
        val result = destinationService.suggestDestinations(criteria)

        // Then
        assertTrue(result.isSuccess)
        val suggestions = result.getOrThrow()
        assertTrue(suggestions.all {
            it.destination.averageCostPerNight <= 100.0
        })
    }

    @Test
    fun `suggestDestinations scores correctly by season`() {
        // Given
        val criteria = DestinationSearchCriteria(
            eventId = "event-1",
            participantCount = 4,
            budgetRange = BudgetRange(0.0, 300.0, "EUR"),
            preferredRegions = emptyList(),
            preferredSeasons = listOf(Season.SUMMER),
            preferredActivities = emptyList(),
            maxGroupSize = 10,
            preferredLodgingTypes = emptyList()
        )

        // When
        val result = destinationService.suggestDestinations(criteria)

        // Then
        assertTrue(result.isSuccess)
        val suggestions = result.getOrThrow()

        val summerDestination = suggestions.find {
            it.destination.name == "Nice"
        }

        assertNotNull(summerDestination)
        assertTrue(summerDestination!!.seasonalityScore == 1.0)
    }

    @Test
    fun `suggestLodgings filters by type`() {
        // When
        val result = destinationService.suggestLodgings(
            destinationId = "dest-1",
            participantCount = 4,
            budgetRange = BudgetRange(0.0, 300.0, "EUR"),
            preferredTypes = listOf(LodgingType.APARTMENT)
        )

        // Then
        assertTrue(result.isSuccess)
        val suggestions = result.getOrThrow()
        assertTrue(suggestions.isNotEmpty())
        assertTrue(suggestions.all {
            it.lodging.type == LodgingType.APARTMENT
        })
    }

    @Test
    fun `suggestLodgings prioritizes capacity`() {
        // When
        val result = destinationService.suggestLodgings(
            destinationId = "dest-1",
            participantCount = 4,
            budgetRange = BudgetRange(0.0, 300.0, "EUR"),
            preferredTypes = emptyList()
        )

        // Then
        assertTrue(result.isSuccess)
        val suggestions = result.getOrThrow()
        assertTrue(suggestions.all {
            it.capacityScore > 0.7
        })
    }

    @Test
    fun `calculateCostScore respects budget range`() {
        val budget = BudgetRange(100.0, 200.0, "EUR")

        val exactBudgetScore = destinationService.calculateCostScore(150.0, budget)
        val overBudgetScore = destinationService.calculateCostScore(250.0, budget)

        assertTrue(exactBudgetScore == 1.0)
        assertTrue(overBudgetScore == 0.5)
    }

    @Test
    fun `calculateSeasonalityScore matches preferred seasons`() {
        val exactMatchScore = destinationService.calculateSeasonalityScore(
            destinationSeasons = listOf(Season.SUMMER),
            preferredSeasons = listOf(Season.SUMMER)
        )

        val noMatchScore = destinationService.calculateSeasonalityScore(
            destinationSeasons = listOf(Season.WINTER),
            preferredSeasons = listOf(Season.SUMMER)
        )

        assertTrue(exactMatchScore == 1.0)
        assertTrue(noMatchScore == 0.3)
    }

    @Test
    fun `generateReasons includes all applicable reasons`() {
        val scores = DestinationScores(
            overall = 0.8,
            cost = 0.9,
            accessibility = 0.8,
            seasonality = 0.8,
            popularity = 0.7,
            personalization = 0.7
        )

        val reasons = destinationService.generateReasons(
            scores,
            destinations[0]
        )

        assertTrue(reasons.contains("Dans votre budget"))
        assertTrue(reasons.contains("Très accessible"))
        assertTrue(reasons.contains("Bonne saison"))
        assertTrue(reasons.contains("Populaire"))
        assertTrue(reasons.contains("Correspond à vos préférences"))
    }
}
```

## Performance Considerations

### Database (No persistence needed for Phase 1)

Destination and lodging data is fetched from external providers, not stored locally.

### Caching

```kotlin
class DestinationCache(
    private val maxCacheSize: Int = 100,
    private val ttlSeconds: Long = 600 // 10 minutes
) {
    private val cache = mutableMapOf<String, CacheEntry>()

    data class CacheEntry(
        val suggestions: List<DestinationSuggestion>,
        val timestamp: Instant
    )

    fun get(criteria: DestinationSearchCriteria): List<DestinationSuggestion>? {
        val key = criteria.hashCode().toString()
        val entry = cache[key] ?: return null

        if (entry.timestamp.plusSeconds(ttlSeconds).isAfter(Instant.now())) {
            return entry.suggestions
        }

        cache.remove(key)
        return null
    }

    fun put(criteria: DestinationSearchCriteria, suggestions: List<DestinationSuggestion>) {
        val key = criteria.hashCode().toString()

        if (cache.size >= maxCacheSize) {
            // Remove oldest entry
            val oldestKey = cache.minByOrNull { it.value.timestamp }?.key
            oldestKey?.let { cache.remove(it) }
        }

        cache[key] = CacheEntry(suggestions, Instant.now())
    }
}
```

## Limitations

**Phase 1** (Current):
1. **Mock providers**: Toutes les données sont mockées
2. **Pas d'intégration temps réel**: Pas d'appels aux APIs externes
3. **Données statiques**: Destinations et hébergements fixes
4. **Pas de géocoding**: Pas de recherche par adresse
5. **Pas de reviews détaillées**: Note globale uniquement

**Phase 2** (Future):
1. **Intégration APIs réelles**:
   - Google Places API pour destinations
   - Airbnb API pour hébergements
   - Booking.com API pour hôtels
2. **Géocoding avancé**: Recherche par adresse, code postal
3. **Reviews détaillées**: Avis texte, photos
4. **Availability temps réel**: Vérification disponibilité
5. **Photos haute résolution**: Galerie d'images
6. **Cartes interactives**: Map avec visualisation

## Related Specs

- `event-organization/spec.md` - Main event management
- `suggestion-management/spec.md` - Recommendation engine
- `accommodation/spec.md` - Accommodation (hébergement interne)
- `budget-management/spec.md` - Budget tracking
- `calendar-management/spec.md` - Season selection
- `transport-optimization/spec.md` - Getting to destination

---

**Version**: 1.0.0
**Last Updated**: 26 décembre 2025
**Maintainer**: Équipe Wakeve
