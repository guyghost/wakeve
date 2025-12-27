# Suggestion Management Specification

## Version
**Version**: 1.0.0
**Status**: ✅ Implémenté
**Date de création**: 26 décembre 2025
**Auteur**: Équipe Wakeve

## Overview

Le système de Suggestions de Wakeve fournit des recommandations personnalisées aux utilisateurs basées sur:
- Leurs préférences (budget, activités, saisons, localisation)
- L'historique de leurs interactions avec le système
- Les préférences d'autres utilisateurs similaires (collaborative filtering)
- Les statistiques globales (popularité des destinations/activités)

## Domain Model

### RecommendationEngine

#### Responsibilities

**Calcul de scores multi-critères**: Évalue les scénarios, destinations, activités et restaurants selon 5 critères pondérés :
1. **Coût** (30%) : Plus c'est proche du budget, mieux c'est
2. **Personnalisation** (25%) : Correspond aux préférences utilisateur
3. **Accessibilité** (20%) : Facilité d'accès (transport, accessibilité)
4. **Saisonnalité** (15%) : Bonne saison pour la destination/activité
5. **Popularité** (10%) : Évaluations de la communauté

#### Scoring Algorithm

```kotlin
score = (costScore * 0.30) + 
        (personalizationScore * 0.25) + 
        (accessibilityScore * 0.20) +
        (seasonalityScore * 0.15) +
        (popularityScore * 0.10)
```

**Règles de scoring**:

**Coût**:
- `<= budgetMax` : score = 1.0
- `<= budgetMax * 1.5` : score = 0.5
- `> budgetMax * 1.5` : score = 0.0

**Personnalisation**:
- Correspondance préférences utilisateur (activités, saisons, durée)
- Durée dans `preferredDurationRange` : score += 1.0
- Group size <= `maxGroupSize` : score += 1.0
- Type d'activité dans `preferredActivities` : score += 1.0
- Chaque critère correspondant ajoute `score / nombreDeCritères`

**Accessibilité**:
- Distance depuis villes préférées : 
  - `< 50km` : score = 1.0
  - `50-100km` : score = 0.7
  - `> 100km` : score = 0.3

**Saisonnalité**:
- Saison actuelle dans `preferredSeasons` : score = 1.0
- Saison adjacente dans `preferredSeasons` : score = 0.5
- Autre saison : score = 0.0

**Popularité**:
- Basé sur les statistiques globales (mocké pour l'instant)
- Score normalisé 0.0-1.0

#### Types de Recommandation

```kotlin
enum class RecommendationType {
    SCENARIO,        // Scénarios de voyage
    DESTINATION,      // Destinations potentielles
    ACTIVITY,         // Activités à faire
    RESTAURANT,       // Restaurants pour les repas
    TRANSPORT,        // Options de transport
    LODGING           // Hébergements
}
```

### UserPreferences

Les préférences utilisateur sont stockées et utilisées pour personnaliser les recommandations.

#### Modèle

```kotlin
@Serializable
data class UserPreferences(
    val userId: String,
    val budgetRange: BudgetRange,
    val preferredDurationRange: ClosedRange<Int>,
    val preferredSeasons: List<Season>,
    val preferredActivities: List<String>,
    val maxGroupSize: Int,
    val locationPreferences: LocationPreferences,
    val accessibilityNeeds: List<String>
)

@Serializable
data class BudgetRange(
    val min: Double,
    val max: Double,
    val currency: String
)

@Serializable
data class LocationPreferences(
    val preferredRegions: List<String>,
    val maxDistanceFromCity: Int, // km
    val nearbyCities: List<String>
)

enum class Season {
    WINTER, SPRING, SUMMER, FALL, ALL_YEAR
}
```

#### Méthodes de Recommandation

**1. Content-Based** - Filtrage par préférences:
```kotlin
suspend fun recommendContentBased(
    userId: String,
    context: RecommendationContext,
    limit: Int = 10
): List<RecommendationResult>
```

Retourne les éléments qui correspondent le mieux aux préférences de l'utilisateur.

**2. Collaborative** - Filtrage basé sur utilisateurs similaires:
```kotlin
suspend fun recommendCollaborative(
    userId: String,
    similarUsers: List<String>,
    context: RecommendationContext,
    limit: Int = 10
): List<RecommendationResult>
```
```kotlin
suspend fun calculateUserSimilarity(
    user1: UserPreferences,
    user2: UserPreferences
): Double
```

Retourne un score de similarité 0.0-1.0 basé sur:
- Intersection des activités préférées
- Intersection des saisons préférées
- Intersection des régions préférées
- Similitude de budget

**3. Hybride** - Combinaison des deux approches:
```kotlin
suspend fun recommendHybrid(
    userId: String,
    similarUsers: List<String>,
    context: RecommendationContext,
    weights: ScoringWeights = ScoringWeights(),
    limit: Int = 10
): List<RecommendationResult>
```

Pondère les deux approches et fusionne les résultats.

### SuggestionService

#### Responsibilities

**Génération de suggestions** pour les événements:
- Scénarios (basés sur les scénarios créés)
- Activités (basées sur les préférences et destination)
- Restaurants (basés sur la localisation)
- Transport (basés sur les lieux de départ)
- Logement (basés sur les hébergements disponibles)
- Destinations (basées sur les saisons et budget)

#### Intégration

**Avec Phase 2 - Budget**:
```kotlin
suspend fun suggestBudgetOptimizations(
    eventId: String,
    userId: String
): SuggestionResult
```

Suggère des ajustements au budget basé sur les dépenses estimées.

**Avec Phase 3 - Logistique**:
```kotlin
suspend fun suggestLogisticsOptimizations(
    eventId: String,
    userId: String
): SuggestionResult
```

Suggère des optimisations pour la logistique (logement, repas, équipements).

**Avec Phase 4 - Collaboration**:
```kotlin
suspend fun suggestCollaborationActions(
    eventId: String,
    userId: String
): SuggestionResult
```

Suggère des actions pour améliorer la collaboration.

### A/B Testing

Les recommandations sont trackées pour optimiser l'algorithme.

#### Tracking des Interactions

```kotlin
enum class SuggestionInteractionType {
    VIEWED,      // L'utilisateur a vu la suggestion
    CLICKED,     // L'utilisateur a cliqué
    DISMISSED,    // L'utilisateur a rejeté la suggestion
    ACCEPTED      // L'utilisateur a accepté la suggestion
}

suspend fun trackSuggestionInteraction(
    userId: String,
    suggestionId: String,
    interactionType: SuggestionInteractionType
)
```

Les interactions sont stockées pour ajuster les scores et améliorer le système de recommandation.

### Scenarios d'Utilisation

#### Scénario 1: Recommandation de Scénarios

**GIVEN**: L'organisateur crée un événement
**WHEN**: L'utilisateur ouvre l'écran de scénarios
**THEN**: 
1. Le système génère 3 suggestions de scénarios basées sur:
   - Le budget approximatif
   - La durée préférée
   - Les saisons préférées
2. Les scénarios sont affichés avec:
   - Score global (ex: "92/100")
   - Raisons explicites (ex: "Dans votre budget", "Bonne saison")
   - Bouton "Utiliser ce scénario"

**SUCCESS**: Le scénario est appliqué à l'événement

#### Scénario 2: Recommandation d'Activités

**GIVEN**: L'événement est confirmé et une destination est choisie
**WHEN**: L'utilisateur ouvre l'écran de planification d'activités
**THEN**:
1. Le système génère des suggestions d'activités basées sur:
   - Les préférences utilisateur
   - La destination choisie
   - La durée de l'événement
2. Les activités sont classées par score
3. L'utilisateur peut les ajouter à l'événement

**SUCCESS**: L'utilisateur sélectionne les activités suggérées

#### Scénario 3: Recommandation de Restaurants

**GIVEN**: L'utilisateur planifie les repas pour un événement
**WHEN**: L'utilisateur est dans la section "Repas" de l'événement
**THEN**:
1. Le système suggère des restaurants basés sur:
   - La localisation
   - Les préférences alimentaires des participants
   - Le budget moyen par personne
2. Les restaurants sont notés avec:
   - Cuisine
   - Gamme de prix
   - Note moyenne
3. L'utilisateur peut voir les menus et réserver

**SUCCESS**: L'utilisateur sélectionne un restaurant

#### Scénario 4: Feedback

**GIVEN**: Un utilisateur accepte une suggestion
**WHEN**: L'interaction est trackée
**THEN**:
1. Le score de l'élément recommandé augmente
2. L'algorithme apprend et améliore
3. Les futures recommandations sont plus pertinentes

## API

### Endpoints

```kotlin
// SuggestionService
POST /api/events/{id}/suggestions/generate
GET  /api/events/{id}/suggestions/{suggestionId}
POST /api/events/{id}/suggestions/{suggestionId}/interaction

// UserPreferences
GET /api/users/{userId}/preferences
PUT /api/users/{userId}/preferences
GET /api/users/{userId}/preferences/budget
PUT /api/users/{userId}/preferences/activities
GET /api/users/{userId}/preferences/seasons
```

## Data Models

### RecommendationScore

```kotlin
@Serializable
data class RecommendationScore(
    val itemId: String,
    val userId: String,
    val overallScore: Double,        // 0.0 à 1.0
    val costScore: Double,         // 0.0 à 1.0 (1 = dans le budget)
    val accessibilityScore: Double, // 0.0 à 1.0 (1 = très accessible)
    val popularityScore: Double,    // 0.0 à 1.0 (1 = très populaire)
    val seasonalityScore: Double,   // 0.0 à 1.0 (1 = bonne saison)
    val personalizationScore: Double // 0.0 à 1.0 (1 = correspond parfaitement)
)
)
```

### ScoringWeights

```kotlin
@Serializable
data class ScoringWeights(
    val cost: Double = 0.3,          // 30%
    val accessibility: Double = 0.2,    // 20%
    val popularity: Double = 0.1,      // 10%
    val seasonality: Double = 0.15,     // 15%
    val personalization: Double = 0.25   // 25%
)
```

### RecommendationResult

```kotlin
@Serializable
data class RecommendationResult(
    val itemId: String,
    val item: RecommendationItem,
    val overallScore: Double,
    val costScore: Double,
    val contentScore: Double,
    val reasons: List<String>
)

@Serializable
data class RecommendationItem(
    val id: String,
    val type: RecommendationType,
    name: String,
    val description: String,
    val imageUrl: String?,
    val metadata: Map<String, Any> = emptyMap()
)
```

### UserPreferences

```kotlin
@Serializable
data class UserPreferences(
    val userId: String,
    val budgetRange: BudgetRange,
    val preferredDurationRange: ClosedRange<Int>,
    val preferredSeasons: List<Season>,
    val preferredActivities: List<String>,
    val maxGroupSize: Int,
    val locationPreferences: LocationPreferences,
    val accessibilityNeeds: List<String>
)

@Serializable
data class BudgetRange(
    val min: Double,
    val max: Double,
    val currency: String
)

@Serializable
data class LocationPreferences(
    val preferredRegions: List<String>,
    val maxDistanceFromCity: Int,
    val nearbyCities: List<String>
)

enum class Season {
    WINTER, SPRING, SUMMER, FALL, ALL_YEAR
}
```

### RecommendationContext

```kotlin
@Serializable
data class RecommendationContext(
    val eventId: String,
    val userId: String,
    val userPreferences: UserPreferences,
    val participantCount: Int,
    val season: Season,
    val dateRange: ClosedRange<Instant>? = null
)
```

### SuggestionABTest

```kotlin
@Serializable
data class SuggestionABTest(
    val id: String,
    val name: String,
    val variantA: String,
    val variantB: String,
    val metric: String,
    val createdAt: String,
    val status: ABTestStatus,
    val winner: String? = null
)

enum class ABTestStatus {
    RUNNING, COMPLETED, CANCELLED
}
```

## Testing

### Unit Tests

#### RecommendationEngineTest

```kotlin
class RecommendationEngineTest {
    @Test
    fun `testCalculateScenarioScore respects user budget`()
    
    @Test
    fun `testCalculateCostScore with exact budget match`()
    
    @Test
    fun `testCalculateCostScore with over budget`()
    
    @Test
    fun `testCalculatePersonalizationScore matches all preferences`()
    
    @Test
    fun `testCalculateSeasonalityScore for preferred season`()
    
    @Test
    fun `testCalculateUserSimilarity with identical preferences`()
    
    @Test
    fun `testRecommendCollaborative filters by similarity threshold`()
    
    @Test
    fun `testRecommendHybrid combines scores correctly`()
}
```

#### SuggestionServiceTest

```kotlin
class SuggestionServiceTest {
    @Test
    fun `testGenerateScenarioSuggestions uses RecommendationEngine`()
    
    @Test
    fun `testGenerateActivitySuggestions filters by preferences`()
    
    @Test
    fun `testTrackSuggestionInteraction updates ABTest`()
    
    @Test
    fun `testGenerateRestaurantSuggestions respects dietary restrictions`()
}
```

## Performance

### Indexes de Base de Données

Les tables sont indexées pour optimiser les requêtes fréquentes:

```sql
-- user_preferences
CREATE INDEX idx_user_prefs_activities ON user_preferences(userId, preferredActivities);
CREATE INDEX idx_user_prefs_seasons ON user_preferences(userId, preferredSeasons);

-- AB Tests
CREATE INDEX idx_ab_test_status ON suggestion_ab_tests(status);

-- Interactions tracking
CREATE INDEX idx_suggestion_interactions ON suggestion_interactions(userId, interactionType, createdAt);
```

### Caching

Les résultats de recommandation sont mis en cache pour améliorer les performances:

- **TTL**: 5 minutes
- **Size**: 100 entrées maximum
- **LRU**: Least Recently Used eviction

```kotlin
class SuggestionCache(
    private val maxCacheSize: Int = 100,
    private val ttlSeconds: Long = 300
)
```

## Limitations

Pour cette phase (Phase 5.1), les limitations suivantes s'appliquent:

1. **Providers mockés**: Les providers de données externes (Google Maps, restaurants, etc.) sont mockés
2. **Scoring simplifié**: L'algorithme de similarité utilise une approche basée (cosinus simple)
3. **A/B Testing**: Le framework est prêt mais les expériences sont limitées
4. **Pas de Machine Learning**: Les recommandations sont basées sur des règles explicites (pas encore de ML)

## Future Enhancements

**Phase 6+** (non implémenté):
1. Machine Learning pour scoring avancé
2. Intégration avec Google Maps API pour géocoding
3. Intégration avec Yelp/Google Places pour restaurants
4. Collaborative filtering réel avec données d'interaction
5. Algorithmes de clustering pour destinations
6. Recommandations en temps réel basées sur le contexte

---

**Version**: 1.0.0  
**Last Updated**: 26 décembre 2025  
**Maintainer**: Équipe Wakeve