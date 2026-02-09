# Specification: Suggestion Engine

> **Capability**: `suggestion-engine`
> **Version**: 1.0.0
> **Status**: Active
> **Last Updated**: 2026-02-08

## Overview

This specification defines the AI-powered suggestion engine that provides personalized recommendations for destinations, accommodations, and transport options. It uses a multi-criteria weighted scoring algorithm combined with user preference learning to deliver relevant suggestions.

**Version**: 1.0.0
**Status**: Active
**Created**: 2026-02-08
**Maintainer**: AI/ML Team

### Core Concepts

**Multi-Criteria Scoring**: Weighted scoring algorithm evaluating options across 5 dimensions (cost, personalization, accessibility, seasonality, popularity).

**User Profile**: Aggregated data about user preferences, history, and social graph used for personalization.

**Suggestion Context**: The event-specific parameters (budget, dates, participant count, location) that influence recommendations.

**Cold Start Problem**: Handling new users with no history by using popular defaults and progressive profiling.

### Key Features

- **Destination Recommendations**: AI-ranked destinations based on user preferences and event context
- **Accommodation Suggestions**: Filtered and scored lodging options
- **Transport Optimization**: Route suggestions with cost/time trade-offs
- **Progressive Profiling**: System learns from user interactions
- **A/B Testing Framework**: Experimental evaluation of suggestion quality

### Dependencies

| Dependency | Type | Description |
|------------|------|-------------|
| `destination-planning` | Spec | Consumes destination suggestions |
| `transport-optimization` | Spec | Consumes transport suggestions |
| `event-organization` | Spec | Provides event context for suggestions |
| `user-auth` | Spec | Provides user profile data |

## Purpose

The Suggestion Engine enhances event planning by reducing decision paralysis and surfacing relevant options that match the user's preferences and event constraints.

### Use Cases

- **Destination Discovery**: User doesn't know where to go → system shows ranked destinations
- **Accommodation Selection**: User needs lodging → system filters and ranks options
- **Transport Coordination**: Users need to meet → system suggests optimal meeting points
- **Budget Optimization**: User has budget constraints → system finds best value options

## Requirements

### Requirement: Multi-Criteria Scoring Algorithm
**ID**: `SUGG-001`

The system SHALL score all suggestions using a weighted 5-criteria algorithm.

**Scoring Formula**:
```
finalScore = (costScore × 0.30) + (personalizationScore × 0.25) +
            (accessibilityScore × 0.20) + (seasonalityScore × 0.15) +
            (popularityScore × 0.10)
```

**Criteria Definitions**:

| Criterion | Weight | Description | Source |
|-----------|--------|-------------|--------|
| Cost | 30% | Affordability relative to budget | User input, API data |
| Personalization | 25% | Match with user preferences | User profile, history |
| Accessibility | 20% | Ease of access (transport, visa) | Geo APIs |
| Seasonality | 15% | Weather, peak/off-peak | Historical data |
| Popularity | 10% | General popularity | Booking data, reviews |

**Score Normalization**: Each criterion is normalized to 0.0-1.0 range before weighting.

#### Scenario: Score a destination suggestion
- **GIVEN** a user has budget €500, prefers beach destinations, and event is in July
- **WHEN** system scores "Nice, France" for this event
- **THEN** the system SHALL:
  - Calculate costScore based on €500 budget fit
  - Calculate personalizationScore based on beach preference match
  - Calculate accessibilityScore based on flight availability
  - Calculate seasonalityScore based on July weather in Nice
  - Calculate popularityScore based on booking data
  - Return final weighted score

### Requirement: User Profile Learning
**ID**: `SUGG-002`

The system SHALL maintain and update user profiles based on interactions.

**Profile Components**:
- **Preferences**: Explicit user likes/dislikes (beach vs mountain, luxury vs budget)
- **History**: Past events and chosen options
- **Social Graph**: Friends' preferences (social proof)
- **Context**: Typical budget, group size, travel style

#### Scenario: Update profile from user action
- **GIVEN** a user views "Mountain cabin" suggestions
- **WHEN** user clicks "Not interested" on a suggestion
- **THEN** the system SHALL:
  - Decrease mountain preference score
  - Record negative feedback for future filtering
  - Adjust personalizationScore for future queries

#### Scenario: Cold start for new user
- **GIVEN** a new user with no history
- **WHEN** user requests suggestions
- **THEN** the system SHALL:
  - Use popular destinations as defaults
  - Ask progressive profiling questions
  - Personalize based on first interactions

### Requirement: Destination Suggestions
**ID**: `SUGG-003`

The system SHALL provide destination recommendations based on event context.

**Input Parameters**:
- Budget range (min/max per person)
- Departure location(s) of participants
- Event dates
- Group size
- Preferred activities (optional)

#### Scenario: Get destination suggestions
- **GIVEN** a user has €300 budget, departing from Paris, 5 participants, July
- **WHEN** user requests destination suggestions
- **THEN** the system SHALL:
  - Filter destinations by cost (near €300)
  - Score by accessibility from Paris (flights, train)
  - Score by seasonality (July weather)
  - Return ranked list of 10 destinations

### Requirement: Accommodation Suggestions
**ID**: `SUGG-004`

The system SHALL provide accommodation recommendations based on destination and user profile.

**Input Parameters**:
- Destination ID
- Participant count (capacity requirements)
- Budget range
- Preferred accommodation types (hotel, apartment, house, etc.)
- Required amenities
- Check-in/check-out dates

**Accommodation Scoring**:
```
finalScore = (capacityScore × 0.25) + (amenityScore × 0.20) +
            (priceScore × 0.25) + (ratingScore × 0.15) +
            (personalizationScore × 0.15)
```

#### Scenario: Get accommodation suggestions
- **GIVEN** destination is Nice, 4 participants, budget €150-250/night
- **WHEN** user requests accommodation suggestions
- **THEN** the system SHALL:
  - Filter accommodations with capacity >= 4
  - Score by price fit within budget
  - Score by amenity match (pool, wifi, parking, etc.)
  - Return ranked list with reasons

#### Scenario: Accommodation with specific amenities
- **GIVEN** user requires wheelchair accessibility and parking
- **WHEN** user requests accommodation suggestions
- **THEN** the system SHALL:
  - Prioritize accessible accommodations
  - Boost score for parking availability
  - Include accessibility notes in reasons

### Requirement: Transport Suggestions
**ID**: `SUGG-005`

The system SHALL suggest optimal meeting points and transport options.

**Input Parameters**:
- Participant locations
- Event location (if already chosen)
- Budget constraints
- Time constraints

#### Scenario: Suggest meeting point
- **GIVEN** participants are in Paris, Lyon, and Marseille
- **WHEN** organizer requests meeting point suggestion
- **THEN** the system SHALL:
  - Calculate centroid or weighted center
  - Consider train travel time to each location
  - Suggest central location with good rail connections
  - Provide cost and time estimates for each participant

### Requirement: A/B Testing Framework
**ID**: `SUGG-006`

The system SHALL support A/B testing of suggestion algorithms.

**Testable Variants**:
- Scoring weights (e.g., 30/25/20/15/10 vs 25/30/20/15/10)
- Personalization algorithms (collaborative filtering vs content-based)
- Ranking diversity (similar vs diverse results)

#### Scenario: A/B test scoring weights
- **GIVEN** two variants of scoring weights (A and B)
- **WHEN** user is assigned to variant B
- **THEN** the system SHALL:
  - Apply variant B weights for all suggestions
  - Track user engagement metrics
  - Log variant assignment for analysis

## Data Models

### SuggestionRequest

```kotlin
@Serializable
data class SuggestionRequest(
    val type: SuggestionType,
    val context: SuggestionContext,
    val preferences: UserPreferences? = null
)
```

### SuggestionType

```kotlin
enum class SuggestionType {
    DESTINATION,
    ACCOMMODATION,
    TRANSPORT,
    ACTIVITY
}
```

### SuggestionContext

```kotlin
@Serializable
data class SuggestionContext(
    val eventId: String?,
    val budgetMin: Double? = null,
    val budgetMax: Double? = null,
    val departureLocations: List<String> = emptyList(),
    val participantCount: Int,
    val startDate: Instant,
    val endDate: Instant,
    val preferredActivities: List<String> = emptyList()
)
```

### UserPreferences

```kotlin
@Serializable
data class UserPreferences(
    val userId: String,
    // Explicit preferences
    val likedCategories: Set<String>,        // beach, mountain, city, countryside
    val dislikedCategories: Set<String>,
    val preferredBudgetLevel: BudgetLevel,   // budget, mid-range, luxury
    val preferredAtmosphere: Set<String>,     // quiet, lively, family-friendly
    val accessibilityNeeds: Set<String>,      // wheelchair, public_transport, parking

    // Learned weights (updated by system)
    val categoryWeights: Map<String, Double> = emptyMap(),  // beach: 0.8, mountain: 0.2

    // Social graph
    val friendsPreferences: Map<String, UserPreferences> = emptyMap()
)
```

### ScoredSuggestion

```kotlin
@Serializable
data class ScoredSuggestion(
    val id: String,
    val type: SuggestionType,
    val suggestion: Map<String, Any>,  // Actual suggestion data
    val scores: ScoreBreakdown,
    val finalScore: Double,
    val rank: Int
)
```

### ScoreBreakdown

```kotlin
@Serializable
data class ScoreBreakdown(
    val costScore: Double,          // 0.0 - 1.0
    val personalizationScore: Double, // 0.0 - 1.0
    val accessibilityScore: Double,   // 0.0 - 1.0
    val seasonalityScore: Double,     // 0.0 - 1.0
    val popularityScore: Double,      // 0.0 - 1.0
    val finalScore: Double            // Weighted sum
)
```

### AccommodationScoreBreakdown

```kotlin
@Serializable
data class AccommodationScoreBreakdown(
    val capacityScore: Double,       // 0.0 - 1.0 (fits group size)
    val amenityScore: Double,        // 0.0 - 1.0 (amenities match)
    val priceScore: Double,          // 0.0 - 1.0 (budget fit)
    val ratingScore: Double,         // 0.0 - 1.0 (reviews)
    val personalizationScore: Double, // 0.0 - 1.0 (ML affinity)
    val finalScore: Double            // Weighted sum
)
```

### BudgetLevel

```kotlin
enum class BudgetLevel {
    BUDGET,      // < €100/night
    MID_RANGE,   // €100-250/night
    LUXURY       // > €250/night
}
```

### FeedbackSignal

```kotlin
@Serializable
data class FeedbackSignal(
    val id: String,
    val userId: String,
    val eventType: FeedbackEventType,
    val suggestionId: String,
    val suggestionType: SuggestionType,
    val itemId: String,
    val metadata: Map<String, String> = emptyMap(),
    val timestamp: Instant
)

enum class FeedbackEventType {
    // Explicit feedback
    VIEWED,
    CLICKED,
    BOOKED,
    DISMISSED,
    FAVORITED,
    SHARED,

    // Implicit feedback
    HOVERED,
    EXPANDED_DETAILS,
    COMPARED,
    VIEWED_SIMILAR,

    // Negative signals
    HIDDEN,
    REPORTED_IRRELEVANT
}
```

## API / Interface

### REST API Endpoints

> Base path: `/api/suggestions`

| Endpoint | Method | Description | Auth Required |
|----------|--------|-------------|---------------|
| POST /api/suggestions/destinations | POST | Get destination recommendations | Yes |
| POST /api/suggestions/accommodations | POST | Get accommodation recommendations | Yes |
| POST /api/suggestions/transport | POST | Get transport/meeting point suggestions | Yes |
| POST /api/suggestions/feedback | POST | Submit feedback signal | Yes |
| GET /api/suggestions/profile | GET | Get user personalization profile | Yes |
| PUT /api/suggestions/profile | PUT | Update user preferences | Yes |

### POST /api/suggestions/destinations

**Description**: Get destination recommendations for an event

**Authentication**: Required

**Request Body**:
```json
{
  "budgetMin": 200,
  "budgetMax": 500,
  "departureLocations": ["Paris", "Lyon", "Marseille"],
  "participantCount": 5,
  "startDate": "2026-07-15T10:00:00Z",
  "endDate": "2026-07-20T10:00:00Z",
  "preferredActivities": ["beach", "water_sports"],
  "limit": 10
}
```

**Response 200 OK**:
```json
{
  "suggestions": [
    {
      "id": "dest-001",
      "name": "Nice",
      "country": "France",
      "region": "Provence-Alpes-Côte d'Azur",
      "scores": {
        "costScore": 0.8,
        "personalizationScore": 0.9,
        "accessibilityScore": 0.7,
        "seasonalityScore": 0.85,
        "popularityScore": 0.75,
        "finalScore": 0.80
      },
      "estimatedCostPerPerson": 350,
      "flightTime": { "paris": "1h30", "lyon": "0h50", "marseille": "0h30" },
      "weather": { "july": { "temp": 27, "sunshine": "high" } }
    }
  ]
}
```

### POST /api/suggestions/accommodations

**Description**: Get accommodation recommendations for a destination

**Authentication**: Required

**Request Body**:
```json
{
  "destinationId": "dest-001",
  "participantCount": 4,
  "budgetMin": 150,
  "budgetMax": 250,
  "preferredTypes": ["APARTMENT", "HOUSE"],
  "requiredAmenities": ["wifi", "parking", "kitchen"],
  "checkIn": "2026-07-15T14:00:00Z",
  "checkOut": "2026-07-20T10:00:00Z",
  "limit": 10
}
```

**Response 200 OK**:
```json
{
  "suggestions": [
    {
      "id": "lodging-001",
      "name": "Beach Apartment Nice",
      "type": "APARTMENT",
      "address": "45 Rue de France, Nice",
      "pricePerNight": 180,
      "maxOccupancy": 4,
      "amenities": ["wifi", "parking", "kitchen", "balcony", "washing_machine"],
      "rating": 4.5,
      "scores": {
        "capacityScore": 1.0,
        "amenityScore": 0.85,
        "priceScore": 0.95,
        "ratingScore": 0.9,
        "personalizationScore": 0.75,
        "finalScore": 0.88
      },
      "reasons": [
        "Perfect capacity for your group",
        "Has all required amenities",
        "Excellent value for money",
        "Highly rated by guests"
      ],
      "totalPrice": 900,
      "availability": "AVAILABLE"
    }
  ]
}
```

### POST /api/suggestions/transport-meeting-point

**Description**: Get optimal meeting point for participants

**Request Body**:
```json
{
  "participantLocations": [
    { "userId": "user-1", "city": "Paris", "coordinates": [48.8566, 2.3522] },
    { "userId": "user-2", "city": "Lyon", "coordinates": [45.7640, 4.8357] },
    { "userId": "user-3", "city": "Marseille", "coordinates": [43.2965, 5.3698] }
  ],
  "eventLocation": null,
  "budgetMaxPerPerson": 50
}
```

**Response 200 OK**:
```json
{
  "suggestions": [
    {
      "name": "Valence",
      "coordinates": [44.9333, 4.8333],
      "travelTimes": {
        "user-1": "2h15 by train",
        "user-2": "1h00 by train",
        "user-3": "1h30 by train"
      },
      "costPerPerson": { "user-1": 45, "user-2": 25, "user-3": 30 },
      "totalCost": 100,
      "accessibility": "high"
    }
  ]
}
```

## Security

### Authentication Requirements

- All suggestion endpoints require authentication
- User profile data is isolated per user
- No cross-user profile leakage

### Privacy Considerations

- User preferences stored securely
- Social graph data respects friends' privacy
- Explicit consent for accessing location data

## State Machine Integration

### Suggestion Intents

```kotlin
sealed interface SuggestionIntent : Intent {
    data class GetDestinations(val context: SuggestionContext) : SuggestionIntent
    data class GetMeetingPoints(val participantLocations: List<Location>) : SuggestionIntent
    data class RecordFeedback(val suggestionId: String, val feedback: FeedbackType) : SuggestionIntent
    data class UpdatePreferences(val updates: PreferenceUpdates) : SuggestionIntent
}
```

### Feedback Types

```kotlin
enum class FeedbackType {
    LIKE,
    DISLIKE,
    VIEWED,
    BOOKED,
    HIDDEN
}
```

## Database Schema

```sql
-- User Personalization Profile
CREATE TABLE user_personalization_profile (
    user_id TEXT PRIMARY KEY,
    version INTEGER NOT NULL DEFAULT 1,
    price_tier TEXT NOT NULL DEFAULT 'MID_RANGE',
    budget_flexibility REAL NOT NULL DEFAULT 0.3,
    adventure_level TEXT NOT NULL DEFAULT 'MODERATE',
    planning_style TEXT NOT NULL DEFAULT 'STRUCTURED',
    group_size_preference TEXT NOT NULL DEFAULT 'SMALL_GROUP',
    sample_size INTEGER NOT NULL DEFAULT 0,
    profile_confidence REAL NOT NULL DEFAULT 0.0,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL,
    last_retrained_at INTEGER
);

CREATE INDEX idx_user_profile_updated ON user_personalization_profile(updated_at);
CREATE INDEX idx_user_profile_confidence ON user_personalization_profile(profile_confidence);

-- Category Affinities
CREATE TABLE category_affinity (
    id TEXT PRIMARY KEY,
    user_id TEXT NOT NULL,
    category TEXT NOT NULL,
    affinity REAL NOT NULL,  -- 0.0-1.0
    confidence REAL NOT NULL DEFAULT 0.5,
    updated_at INTEGER NOT NULL,
    FOREIGN KEY (user_id) REFERENCES user_personalization_profile(user_id) ON DELETE CASCADE,
    UNIQUE(user_id, category)
);

CREATE INDEX idx_category_affinity_user ON category_affinity(user_id);
CREATE INDEX idx_category_affinity_score ON category_affinity(affinity DESC);

-- Destination Affinities
CREATE TABLE destination_affinity (
    id TEXT PRIMARY KEY,
    user_id TEXT NOT NULL,
    destination_id TEXT NOT NULL,
    affinity REAL NOT NULL,  -- 0.0-1.0
    interaction_count INTEGER NOT NULL DEFAULT 1,
    last_interaction_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL,
    FOREIGN KEY (user_id) REFERENCES user_personalization_profile(user_id) ON DELETE CASCADE,
    UNIQUE(user_id, destination_id)
);

CREATE INDEX idx_destination_affinity_user ON destination_affinity(user_id);
CREATE INDEX idx_destination_affinity_score ON destination_affinity(affinity DESC);

-- Seasonal Affinities
CREATE TABLE seasonal_affinity (
    id TEXT PRIMARY KEY,
    user_id TEXT NOT NULL,
    season TEXT NOT NULL,
    affinity REAL NOT NULL,  -- 0.0-1.0
    updated_at INTEGER NOT NULL,
    FOREIGN KEY (user_id) REFERENCES user_personalization_profile(user_id) ON DELETE CASCADE,
    UNIQUE(user_id, season)
);

CREATE INDEX idx_seasonal_affinity_user ON seasonal_affinity(user_id);

-- Feedback Signals (Enhanced)
CREATE TABLE suggestion_feedback (
    id TEXT PRIMARY KEY,
    user_id TEXT NOT NULL,
    event_type TEXT NOT NULL,
    suggestion_id TEXT,
    suggestion_type TEXT,
    item_id TEXT NOT NULL,
    metadata TEXT,  -- JSON
    created_at INTEGER NOT NULL DEFAULT (strftime('%s', 'now')),
    FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE
);

CREATE INDEX idx_feedback_user_time ON suggestion_feedback(user_id, created_at DESC);
CREATE INDEX idx_feedback_item ON suggestion_feedback(item_id);
CREATE INDEX idx_feedback_type ON suggestion_feedback(event_type);

-- A/B Test Assignments (Enhanced)
CREATE TABLE ab_test_assignment (
    id TEXT PRIMARY KEY,
    user_id TEXT NOT NULL,
    test_id TEXT NOT NULL,
    variant_id TEXT NOT NULL,
    assigned_at INTEGER NOT NULL DEFAULT (strftime('%s', 'now')),
    FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE,
    UNIQUE(user_id, test_id)
);

CREATE INDEX idx_abtest_user ON ab_test_assignment(user_id);
CREATE INDEX idx_abtest_test ON ab_test_assignment(test_id);

-- A/B Test Metrics
CREATE TABLE ab_test_metrics (
    id TEXT PRIMARY KEY,
    test_id TEXT NOT NULL,
    variant_id TEXT NOT NULL,
    metric_type TEXT NOT NULL,
    value REAL NOT NULL,
    sample_size INTEGER NOT NULL,
    recorded_at INTEGER NOT NULL DEFAULT (strftime('%s', 'now')),
    FOREIGN KEY (test_id, variant_id) REFERENCES ab_test_assignment(test_id, variant_id)
);

CREATE INDEX idx_abtest_metrics_test_variant ON ab_test_metrics(test_id, variant_id);

-- Accommodation Suggestions Cache
CREATE TABLE accommodation_suggestion_cache (
    id TEXT PRIMARY KEY,
    cache_key TEXT NOT NULL UNIQUE,
    destination_id TEXT NOT NULL,
    suggestions TEXT NOT NULL,  -- JSON array
    created_at INTEGER NOT NULL,
    expires_at INTEGER NOT NULL
);

CREATE INDEX idx_accom_cache_key ON accommodation_suggestion_cache(cache_key);
CREATE INDEX idx_accom_cache_expires ON accommodation_suggestion_cache(expires_at);
```

## Testing Requirements

**Coverage Target**: 80%

### Unit Tests

- **ScoringAlgorithmTest**: Verify correct weight application
- **NormalizationTest**: Verify 0-1 range normalization
- **ProfileUpdateTest**: Verify preference updates

### Integration Tests

- **EndToEndSuggestionTest**: Full suggestion flow
- **ABTestFrameworkTest**: Verify variant assignment

### Test Scenarios

#### Scenario: Calculate final score

```kotlin
@Test
fun `calculateScore applies correct weights`() {
    // Given
    val breakdown = ScoreBreakdown(
        costScore = 0.8,
        personalizationScore = 0.9,
        accessibilityScore = 0.7,
        seasonalityScore = 0.85,
        popularityScore = 0.75
    )

    // When
    val result = scoringAlgorithm.calculateFinalScore(breakdown)

    // Then
    val expected = (0.8 * 0.30) + (0.9 * 0.25) +
                   (0.7 * 0.20) + (0.85 * 0.15) +
                   (0.75 * 0.10)
    assertEquals(expected, result, 0.001)
}
```

## Platform-Specific Implementation

### Shared Layer

- `shared/src/commonMain/kotlin/com/guyghost/wakeve/suggestion/ScoringEngine.kt`
- `shared/src/commonMain/kotlin/com/guyghost/wakeve/suggestion/UserProfileManager.kt`
- `shared/src/commonMain/kotlin/com/guyghost/wakeve/suggestion/ABTestFramework.kt`

### Backend

- `server/src/main/kotlin/com/guyghost/wakeve/routes/SuggestionRoutes.kt`
- `server/src/main/kotlin/com/guyghost/wakeve/service/DestinationSuggestionService.kt`

## Implementation Files

### Core
- `shared/src/commonMain/kotlin/com/guyghost/wakeve/suggestion/ScoringAlgorithm.kt`
- `shared/src/commonMain/kotlin/com/guyghost/wakeve/suggestion/UserPreferences.kt`

### Services
- `shared/src/commonMain/kotlin/com/guyghost/wakeve/suggestion/DestinationSuggestionService.kt`
- `shared/src/commonMain/kotlin/com/guyghost/wakeve/suggestion/TransportSuggestionService.kt`

### Models
- `shared/src/commonMain/kotlin/com/guyghost/wakeve/models/SuggestionModels.kt`

## Related Specifications

- `destination-planning`: Consumes destination suggestions
- `transport-optimization`: Consumes transport suggestions
- `event-organization`: Provides suggestion context

## Internationalization

| Key | English | French | Context |
|-----|---------|--------|---------|
| `suggestion.budget.budget` | Budget | Économique | Budget level |
| `suggestion.budget.mid_range` | Mid-range | Moyenne gamme | Budget level |
| `suggestion.budget.luxury` | Luxury | Luxe | Budget level |
| `suggestion.activity.beach` | Beach | Plage | Activity |
| `suggestion.activity.mountain` | Mountain | Montagne | Activity |

## Performance Considerations

- **Caching**: Cache scored suggestions for 5 minutes
- **Pre-computation**: Pre-score popular destinations weekly
- **Async Scoring**: Score calculations run in background
- **Pagination**: Return 10-20 suggestions per page

## ML Model Interfaces

### Model Contract

```kotlin
interface SuggestionModel {
    /**
     * Generate suggestions based on context and preferences
     */
    suspend fun generate(
        context: SuggestionContext,
        preferences: UserPreferences
    ): List<ScoredSuggestion>

    /**
     * Update model based on user feedback
     */
    suspend fun recordFeedback(
        userId: String,
        suggestionId: String,
        feedback: FeedbackType
    )
}
```

### Collaborative Filtering Model

```kotlin
interface CollaborativeFilteringModel {
    /**
     * Train model on user-item interaction matrix
     */
    suspend fun train(interactions: List<UserItemInteraction>): TrainingResult

    /**
     * Predict user affinity for items
     */
    suspend fun predict(userId: String, itemIds: List<String>): Map<String, Double>

    /**
     * Get similar items based on collaborative signals
     */
    suspend fun getSimilarItems(itemId: String, limit: Int): List<SimilarItem>

    /**
     * Get similar users for social recommendations
     */
    suspend fun getSimilarUsers(userId: String, limit: Int): List<String>
}

data class UserItemInteraction(
    val userId: String,
    val itemId: String,
    val interactionType: InteractionType,
    val timestamp: Instant,
    val rating: Double? = null  // Implicit or explicit rating
)

enum class InteractionType {
    VIEW,
    CLICK,
    BOOK,
    DISMISS,
    FAVORITE,
    SHARE
}

data class TrainingResult(
    val modelVersion: Int,
    val accuracy: Double,
    val sampleSize: Int,
    val trainedAt: Instant
)

data class SimilarItem(
    val itemId: String,
    val similarity: Double,
    val reason: String
)
```

### Content-Based Model

```kotlin
interface ContentBasedModel {
    /**
     * Build user profile from item features and interactions
     */
    suspend fun buildProfile(
        userId: String,
        interactions: List<UserItemInteraction>
    ): UserVectorProfile

    /**
     * Score items based on content similarity to user profile
     */
    suspend fun scoreItems(
        profile: UserVectorProfile,
        items: List<Item>
    ): Map<String, Double>

    /**
     * Extract feature vectors from items
     */
    suspend fun extractFeatures(item: Item): ItemFeatures
}

data class UserVectorProfile(
    val userId: String,
    val featureVector: Map<String, Double>,  // Feature name -> weight
    val version: Int,
    val lastUpdated: Instant
)

data class ItemFeatures(
    val itemId: String,
    val categoryVector: Map<String, Double>,
    val priceLevel: Double,
    val seasonalityVector: Map<Season, Double>,
    val activityVector: Map<String, Double>,
    val accessibilityScore: Double
)
```

### Hybrid Model

```kotlin
interface HybridModel {
    /**
     * Combine collaborative and content-based predictions
     */
    suspend fun predict(
        userId: String,
        itemIds: List<String>,
        collaborativeWeight: Double = 0.5
    ): Map<String, Double>

    /**
     * Explain prediction contribution
     */
    suspend fun explain(
        userId: String,
        itemId: String
    ): PredictionExplanation

    /**
     * Dynamically adjust weights based on data availability
     */
    suspend fun getOptimalWeights(userId: String): ModelWeights
}

data class PredictionExplanation(
    val itemId: String,
    val predictedScore: Double,
    val collaborativeContribution: Double,
    val contentContribution: Double,
    val topFactors: List<PredictionFactor>
)

data class PredictionFactor(
    val factorName: String,
    val contribution: Double,
    val description: String
)

data class ModelWeights(
    val collaborative: Double,
    val contentBased: Double,
    val popularity: Double,
    val reason: String  // Why these weights were chosen
)
```

### Neural Collaborative Filtering Interface

```kotlin
interface NeuralCollaborativeFilteringModel : SuggestionModel {
    /**
     * Configure neural network architecture
     */
    suspend fun configure(config: NCFConfig)

    /**
     * Get embeddings for visualization or similarity
     */
    suspend fun getEmbeddings(
        itemType: EmbeddingType,
        ids: List<String>
    ): Map<String, FloatArray>
}

enum class EmbeddingType {
    USER,
    ITEM,
    CATEGORY
}

data class NCFConfig(
    val embeddingDim: Int = 32,
    val hiddenLayers: List<Int> = listOf(64, 32, 16),
    val dropoutRate: Double = 0.2,
    val learningRate: Double = 0.001,
    val batchSize: Int = 256,
    val epochs: Int = 10
)
```

### Personalization Model Details

```kotlin
/**
 * Extended personalization model with multi-dimensional affinity
 */
interface PersonalizationModel {
    /**
     * Get or create user profile
     */
    suspend fun getProfile(userId: String): UserPersonalizationProfile

    /**
     * Update profile with explicit preference
     */
    suspend fun updatePreference(
        userId: String,
        category: String,
        affinity: Double
    )

    /**
     * Update profile from interaction (implicit feedback)
     */
    suspend fun recordInteraction(
        userId: String,
        interaction: UserItemInteraction
    )

    /**
     * Get affinity score for specific criteria
     */
    suspend fun getAffinity(
        userId: String,
        category: String
    ): Double

    /**
     * Handle cold start - initialize profile from onboarding
     */
    suspend fun initializeFromOnboarding(
        userId: String,
        onboardingData: OnboardingData
    ): UserPersonalizationProfile
}

data class OnboardingData(
    val userId: String,
    val selectedCategories: List<String>,
    val budgetRange: ClosedRange<Double>,
    val preferredSeasons: List<Season>,
    val groupStyle: GroupStyle,
    val adventureLevel: AdventureLevel,
    val planningStyle: PlanningStyle
)

enum class GroupStyle {
    SOLO,
    COUPLE,
    FAMILY,
    FRIENDS
}

/**
 * Comprehensive user profile for personalization
 */
@Serializable
data class UserPersonalizationProfile(
    val userId: String,
    val version: Int,
    val createdAt: Instant,
    val updatedAt: Instant,

    // Category affinities (0.0-1.0)
    val categoryAffinities: Map<String, Double>,
    val destinationAffinities: Map<String, Double>,  // destinationId -> affinity
    val activityAffinities: Map<String, Double>,     // activity -> affinity

    // Price sensitivity
    val priceTier: PriceTier,
    val budgetFlexibility: Double,  // 0.0-1.0, how much user will exceed budget

    // Travel preferences
    val adventureLevel: AdventureLevel,
    val planningStyle: PlanningStyle,
    val groupSizePreference: GroupSizePreference,

    // Seasonality
    val seasonalAffinities: Map<Season, Double>,

    // Model metadata
    val sampleSize: Int,              // Number of data points
    val lastRetrainedAt: Instant?,

    // Confidence scores
    val profileConfidence: Double,    // 0.0-1.0, how much we trust this profile
    val categoryConfidence: Map<String, Double>
)

enum class PriceTier {
    BUDGET,
    MID_RANGE,
    LUXURY,
    FLEXIBLE
}

enum class AdventureLevel {
    RELAXED,
    MODERATE,
    ADVENTUROUS,
    EXTREME
}

enum class PlanningStyle {
    SPONTANEOUS,    // Last-minute bookings
    FLEXIBLE,       // Some planning
    STRUCTURED      // Well in advance
}

enum class GroupSizePreference {
    SOLO,
    SMALL_GROUP,    // 2-4
    MEDIUM_GROUP,   // 5-10
    LARGE_GROUP     // 11+
}
```

### Accommodation Suggestion Interface

```kotlin
/**
 * Accommodation-specific suggestion model
 */
interface AccommodationSuggestionModel {
    /**
     * Suggest accommodations based on destination and user profile
     */
    suspend fun suggestAccommodations(
        destinationId: String,
        context: SuggestionContext,
        profile: UserPersonalizationProfile
    ): List<ScoredSuggestion>

    /**
     * Score accommodation for specific user
     */
    suspend fun scoreAccommodation(
        userId: String,
        accommodationId: String,
        context: SuggestionContext
    ): SuggestionScore

    /**
     * Get price-adjusted suggestions (within budget optimization)
     */
    suspend fun suggestWithinBudget(
        destinationId: String,
        budgetRange: ClosedRange<Double>,
        participantCount: Int,
        profile: UserPersonalizationProfile
    ): List<ScoredSuggestion>
}

data class SuggestionScore(
    val overall: Double,           // 0.0-1.0 composite score
    val relevance: Double,         // 0.0-1.0 preference match
    val affordability: Double,     // 0.0-1.0 budget fit
    val availability: Double,      // 0.0-1.0 availability likelihood
    val quality: Double,           // 0.0-1.0 ratings/popularity
    val personalization: Double,   // 0.0-1.0 ML affinity
    val confidence: Double         // 0.0-1.0 score certainty
)
```

### A/B Testing Model Interface

```kotlin
/**
 * Model for A/B testing suggestion algorithms
 */
interface ABTestModel {
    /**
     * Assign user to test variant
     */
    suspend fun assignVariant(
        userId: String,
        testId: String
    ): ABTestVariant

    /**
     * Record conversion for metric tracking
     */
    suspend fun recordConversion(
        userId: String,
        testId: String,
        metricType: MetricType,
        value: Double
    )

    /**
     * Get test results with statistical analysis
     */
    suspend fun getTestResults(testId: String): ABTestResults

    /**
     * Determine winner with statistical significance
     */
    suspend fun determineWinner(
        testId: String,
        confidenceLevel: Double = 0.95
    ): TestWinner
}

data class ABTestVariant(
    val id: String,
    val testId: String,
    val name: String,
    val description: String,
    val config: Map<String, Any>,
    val trafficPercentage: Double,
    val isControl: Boolean = false
)

data class ABTestResults(
    val testId: String,
    val status: TestStatus,
    val variants: Map<String, VariantMetrics>,
    val winner: String?,
    val statisticalSignificance: Double,
    val recommendedAction: String
)

enum class TestStatus {
    RUNNING,
    COMPLETED,
    INCONCLUSIVE,
    STOPPED_EARLY
}

data class VariantMetrics(
    val variantId: String,
    val sampleSize: Int,
    val metrics: Map<String, MetricValue>,
    val confidenceIntervals: Map<String, ConfidenceInterval>
)

data class MetricValue(
    val name: String,
    val value: Double,
    val changeFromControl: Double? = null,
    val isSignificant: Boolean = false
)

data class ConfidenceInterval(
    val lower: Double,
    val upper: Double,
    val confidenceLevel: Double
)

data class TestWinner(
    val variantId: String,
    val confidence: Double,
    val improvement: Double,
    val reason: String
)

enum class MetricType {
    CLICK_THROUGH_RATE,
    CONVERSION_RATE,
    BOOKING_RATE,
    TIME_TO_DECISION,
    USER_SATISFACTION,
    SUGGESTION_ACCEPTANCE_RATE
}
```

## Change History

| Version | Date | Changes |
|---------|------|---------|
| 1.0.0 | 2026-02-08 | Initial version with scoring algorithm |

## Acceptance Criteria

- [x] Multi-criteria scoring algorithm defined
- [x] User profile learning model specified
- [x] Destination suggestion API defined
- [x] Transport suggestion API defined
- [x] A/B testing framework specified
- [ ] ML model implementation (Phase 2)
- [ ] Real-time personalization (Phase 2)

## Success Metrics

- Suggestion engagement rate: >20%
- User satisfaction with suggestions: >4.0/5
- A/B test statistical significance: 95% confidence
- Scoring latency: <500ms p95

---

**Spec Version**: 1.0.0
**Last Updated**: 2026-02-08
**Status**: Active
**Maintainer**: AI/ML Team
