# Specification: AI Predictive Recommendations

> **Change ID**: `add-ai-innovative-features`
> **Capability**: `suggestion-management` (extension)
> **Type**: Enhancement with AI
> **Date**: 2026-01-01

## Summary
This specification defines the integration of Machine Learning (ML) to provide predictive and personalized recommendations for event planning. It covers user preference learning, availability prediction, and an A/B testing framework to optimize recommendation quality over time.

## ADDED Requirements

### Requirement: ML-Based Recommendations
**ID**: `suggestion-101`

The system SHALL use Machine Learning to provide personalized recommendations for events.

**Business Rules:**
- Training Data: Historical votes, event outcomes, user preferences
- Features: Date, location, event type, participants, season, day of week
- Models: Regression for optimal date, Classification for location type
- Retraining: Monthly with new data
- Fallback: Use heuristic rules if ML model confidence < 70%

**Scenarios:**
- Given event with 100 historical votes
- When RecommendationService invoked
- Then Top 3 dates with 80%+ predicted participation
- Given user prefers cultural events on weekends
- When creating weekend event
- Then Recommendations prioritize museums, theaters over parks

### Requirement: User Preference Learning
**ID**: `suggestion-102`

The system SHALL learn from user behavior to personalize recommendations.

**Business Rules:**
- Implicit preferences: Learn from votes, events created, participant count
- Explicit preferences: User can set preferences in settings
- Decay: Older interactions have less weight (exponential decay)
- Categories: Preferred days, time of day, event types, locations

**Scenarios:**
- Given user creates 5 weekend events in a row
- When RecommendationService invoked
- Then Weekends prioritized in future recommendations
- Given user accepts "afternoon" time slots 80% of the time
- Then Afternoon weighted higher in scoring

### Requirement: Predictive Availability
**ID**: `suggestion-103`

The system SHALL predict participant availability based on historical patterns.

**Business Rules:**
- Pattern: "Jean usually attends weekend parties, not corporate events"
- Seasonality: High attendance in summer, low in winter
- Day of Week: Fridays 60% higher attendance than Mondays
- Confidence Score: Each prediction has a confidence level (0-100%)

**Scenarios:**
- Given event on Friday evening
- And historical data shows 80% attendance on Fridays
- Then Confidence score = 80% for date
- Given user "Marie" who always attends
- Then Confidence score = 95% regardless of day

### Requirement: A/B Testing Framework
**ID**: `suggestion-104`

The system SHALL support A/B testing for ML models.

**Business Rules:**
- Metrics: Participation rate, user satisfaction, time to decision
- Variants: Model A (heuristic only), Model B (ML), Model C (ML + heuristic)
- Traffic split: 10% of users get Model B, 90% get Model A initially
- Rollout: Gradual if Model B outperforms

**Scenarios:**
- Given ML model deployed
- When A/B test runs for 30 days
- Then System collects metrics and selects best model
- Given Model B has 15% higher participation
- Then Model B rolled out to 100% of users

## Data Models

### UserPreference
```kotlin
@Serializable
data class UserPreference(
    val userId: String,
    val preferredDays: List<DayOfWeek>,        // MONDAY, TUESDAY, etc.
    val preferredTimeOfDay: List<TimeOfDay>, // MORNING, AFTERNOON, EVENING
    val preferredEventTypes: List<EventType>, // BIRTHDAY, WEDDING, etc.
    val preferredLocations: List<String>,      // City names
    val avoidEvents: Boolean = false,         // Skip weddings, funerals, etc.
    val scoreWeights: ScoreWeights,
    val lastUpdated: String
)

@Serializable
data class ScoreWeights(
    val proximityWeight: Double = 0.3,     // Near preferred dates
    val typeMatchWeight: Double = 0.2,      // Match preferred types
    val seasonalityWeight: Double = 0.3,       // Favor high season
    val socialWeight: Double = 0.2,          // Friends attending
    val totalWeight: Double = 1.0
)
```

### MLPrediction
```kotlin
@Serializable
data class MLPrediction(
    val date: String,                    // ISO date
    val confidenceScore: Double,          // 0.0 - 1.0
    val predictedAttendance: Double,      // 0.0 - 1.0
    val features: Map<String, Any>,       // Features used for prediction
    val modelVersion: String,            // For A/B testing
)
```

### TrainingData
```kotlin
@Serializable
data class TrainingData(
    val eventId: String,
    val eventDate: String,
    val eventDayOfWeek: String,
    val eventType: String,
    val participantCount: Int,
    val actualAttendance: Int,
    val weather: WeatherData?,           // Optional weather at time
    val season: String                   // SUMMER, AUTUMN, WINTER, SPRING
)
```

## API Changes

### POST /api/recommendations/train-model
Trigger retraining of the ML model with new data.

**Request:**
```json
{
  "modelVersion": "v1.0"
}
```

**Response:**
```json
{
  "status": "TRAINING_IN_PROGRESS",
  "estimatedDuration": "45 minutes",
  "newModelVersion": "v1.1"
}
```

### GET /api/recommendations/predict
Get personalized recommendations for an event.

**Request:**
```json
{
  "eventId": "event-123",
  "numRecommendations": 5
}
```

**Response:**
```json
{
  "recommendations": [
    {
      "date": "2026-06-15",
      "timeOfDay": "AFTERNOON",
      "confidenceScore": 0.85,
      "predictedAttendance": 0.78,
      "reasoning": "Based on your preference for weekend afternoon events and 80% historical attendance"
    }
  ]
}
```

### POST /api/recommendations/feedback
Submit feedback when user accepts/rejects a recommendation (for retraining).

**Request:**
```json
{
  "eventId": "event-123",
  "date": "2026-06-15",
  "accepted": true,
  "userRating": 5
}
```

**Response:**
```json
{
  "status": "FEEDBACK_RECORDED"
}
```

## Testing Requirements

### Unit Tests (shared)
- RecommendationEngineTest: 8 tests
  - Test prediction with historical data
  - Test preference learning
  - Test confidence scoring
  - Test A/B testing logic
- UserPreferenceRepositoryTest: 5 tests
  - CRUD operations for preferences
  - Default preferences for new users
- MLPredictionSerializationTest: 3 tests
  - JSON serialization/deserialization

### Integration Tests
- RecommendationServiceIntegrationTest: 5 tests
  - Full workflow: preferences → ML prediction → feedback → retraining
  - Fallback to heuristics when ML confidence < 70%
  - A/B test variant assignment
- Data CollectionTest: 3 tests
  - Verify training data collection from events

### Performance Tests
- MLPredictionLatencyTest: 3 tests
  - Ensure < 200ms for prediction
  - Ensure < 5s for model retraining
- ModelAccuracyTest: 2 tests
  - Verify > 70% accuracy on validation set

## Implementation Notes

### ML Framework
- **Mobile:** TensorFlow Lite for on-device inference
- **Backend:** TensorFlow / scikit-learn for model training
- **Model Types:**
  - Date prediction: Linear Regression
  - Location classification: Random Forest
  - Attendance prediction: Logistic Regression

### Training Pipeline
- **Data Collection:** Automatic from EventRepository
- **Preprocessing:** Feature engineering, normalization
- **Training:** Daily batch training with new data
- **Validation:** Split data 80/20 train/validation
- **Deployment:** Export model to TensorFlow Lite format

### On-Device Inference
- **Reasoning:** Faster response, privacy (no server needed for predictions)
- **Offline Support:** ML predictions work offline
- **Fallback:** Heuristics if model fails or confidence too low

### Monitoring
- **Metrics:** Prediction accuracy, user satisfaction, A/B test results
- **Alerts:** Model performance degradation
- **Retraining:** Automatic if accuracy drops below 70%
