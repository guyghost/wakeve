# Specification: [Feature Name]

> **Capability**: `[capability-name]`
> **Version**: [1.0.0]
> **Status**: [Draft | Proposed | Active | Deprecated]
> **Last Updated**: [YYYY-MM-DD]

## Overview

[Provide a concise description of the capability and its purpose within the Wakeve ecosystem. Explain what problem it solves and who benefits from it.]

**Version**: [1.0.0]
**Status**: [Draft | Proposed | Active | Deprecated]
**Created**: [YYYY-MM-DD]
**Maintainer**: [Team or individual responsible]

### Core Concepts

**[Primary Entity]**: [Definition and purpose]

**[Secondary Entity]**: [Definition and purpose]

**[Key Concept]**: [Definition and purpose]

### Key Features

- **[Feature 1]**: [Description]
- **[Feature 2]**: [Description]
- **[Feature 3]**: [Description]

### Dependencies

| Dependency | Type | Description |
|------------|------|-------------|
| `[spec-name]` | Spec | [Description of dependency] |
| `[service-name]` | Service | [Description of dependency] |

## Purpose

[Describe the high-level purpose of this capability. What user need does it address? What value does it provide?]

### Use Cases

- **[Use Case 1]**: [Description]
- **[Use Case 2]**: [Description]
- **[Use Case 3]**: [Description]

## Requirements

### Requirement: [Brief requirement description]
**ID**: `[feature-abbreviation]-XXX`

[Detailed requirement description using SHALL/MUST/MAY keywords as appropriate.]

#### Scenario: [Scenario title]
- **GIVEN** [Precondition]
- **WHEN** [Action or event occurs]
- **THEN** [Expected outcome]
- **AND** [Additional outcomes, if applicable]

#### Scenario: [Alternative scenario title]
- **GIVEN** [Precondition]
- **WHEN** [Action or event occurs]
- **THEN** [Expected outcome]

### Requirement: [Brief requirement description with business rules]
**ID**: `[feature-abbreviation]-XXX`

[Detailed requirement description]

**Business Rules:**
- [Rule 1]
- [Rule 2]
- [Rule 3]

#### Scenario: [Scenario title]
- **GIVEN** [Precondition]
- **WHEN** [Action or event occurs]
- **THEN** [Expected outcome]

## Data Models

> All models are defined in language-agnostic format (JSON/Kotlin) to support multiplatform implementation.

### [Primary Entity]

| Field | Type | Description | Constraints |
|-------|------|-------------|-------------|
| `id` | string (UUID) | Unique identifier | Required, auto-generated |
| `field1` | type | Description | Optional/Required |
| `field2` | type | Description | Optional/Required |

```kotlin
@Serializable
data class PrimaryEntity(
    val id: String,
    val field1: String,
    val field2: Int?,
    val createdAt: Instant,
    val updatedAt: Instant?
)
```

**Constraints**:
- [Constraint 1]
- [Constraint 2]

### [Enum Type]

```kotlin
enum class EnumType {
    VALUE1,
    VALUE2,
    VALUE3
}
```

### [Helper Model]

```kotlin
data class HelperModel(
    val field1: String,
    val field2: Double
)
```

## API / Interface

### REST API Endpoints

> Base path: `/api/[resource]`

| Endpoint | Method | Description | Auth Required |
|----------|--------|-------------|---------------|
| GET /api/[resource] | GET | List all items | Yes |
| POST /api/[resource] | POST | Create new item | Yes |
| GET /api/[resource]/[id] | GET | Get item details | Yes |
| PUT /api/[resource]/[id] | PUT | Update item | Yes |
| DELETE /api/[resource]/[id] | DELETE | Delete item | Yes |

### [Endpoint Name]

**Endpoint**: `[METHOD] /api/[resource]`

**Description**: [What this endpoint does]

**Authentication**: Required | Optional

**Request Parameters**:
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `param1` | string | Yes | Description |
| `param2` | int | No | Description |

**Request Body**:
```json
{
  "field1": "value1",
  "field2": "value2"
}
```

**Response 200 OK**:
```json
{
  "id": "uuid-123",
  "field1": "value1",
  "createdAt": "2025-01-01T10:00:00Z"
}
```

**Error Responses**:
| Code | Description |
|------|-------------|
| 400 Bad Request | [Description of when this occurs] |
| 401 Unauthorized | [Description of when this occurs] |
| 403 Forbidden | [Description of when this occurs] |
| 404 Not Found | [Description of when this occurs] |

### Kotlin Interface (for shared layer)

```kotlin
interface RepositoryInterface {
    /**
     * Brief description of the method
     *
     * @param param1 Description
     * @return Result<[ReturnType]> success or failure
     */
    suspend fun methodName(param1: String): Result<ReturnType>
}
```

## Security

### Authentication Requirements

- [All/Pacific endpoints] require authentication via [Bearer token / API key]
- Unauthenticated access is [allowed / denied] for [specific operations]

### Authorization Requirements

| Role | Create | Read | Update | Delete | Special Operations |
|------|--------|------|--------|--------|-------------------|
| Organizer | ✅ | ✅ | ✅ | ✅ | [Description] |
| Participant | ❌ | ✅ | ❌ | ❌ | [Description] |
| Guest | ❌ | Limited | ❌ | ❌ | [Description] |

### Data Protection

- [Sensitive fields] must be encrypted at rest
- [Sensitive operations] must be logged for audit
- [Personal data handling according to RGPD/GDPR]

### Validation Rules

- Input validation requirements
- Rate limiting (if applicable)
- CSRF protection (if applicable)

## State Machine Integration

### Intents

```kotlin
// Feature-specific intents
sealed interface FeatureIntent : Intent {
    data class CreateFeature(val data: FeatureData) : FeatureIntent
    data class UpdateFeature(val id: String, val data: FeatureData) : FeatureIntent
    data class DeleteFeature(val id: String) : FeatureIntent
}
```

### State

```kotlin
data class FeatureState(
    val items: List<FeatureItem> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null
)
```

### Side Effects

| Intent | Side Effect | Description |
|--------|-------------|-------------|
| CreateFeature | NavigateTo("feature-detail") | After successful creation |
| CreateFeature | ShowToast("Feature created") | Success notification |
| DeleteFeature | ShowError("Failed to delete") | On error |

## Database Schema

```sql
CREATE TABLE feature_table (
    id TEXT PRIMARY KEY,
    field1 TEXT NOT NULL,
    field2 INTEGER NOT NULL,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL,
    FOREIGN KEY (related_id) REFERENCES related_table(id) ON DELETE CASCADE
);

CREATE INDEX idx_feature_field1 ON feature_table(field1);
```

## Testing Requirements

### Unit Tests

- [Test category 1] (X tests)
  - [Specific test requirement]
  - [Specific test requirement]
- [Test category 2] (X tests)
  - [Specific test requirement]

**Coverage Target**: [X]%

### Integration Tests

- [Test scenario 1]: [Description]
- [Test scenario 2]: [Description]
- [Test scenario 3]: [Description]

### Test Commands

```bash
# Run unit tests
./gradlew [module]:[testTask]

# Run integration tests
./gradlew [module]:[integrationTestTask]
```

### Test Scenarios

#### Scenario: [Test name]

```kotlin
@Test
fun `test description`() {
    // Given
    val input = setupTestData()

    // When
    val result = systemUnderTest.action(input)

    // Then
    assertTrue(result.isSuccess)
    assertEquals(expected, result.getOrThrow())
}
```

## Platform-Specific Implementation

### Shared Layer

- `shared/src/commonMain/kotlin/com/guyghost/wakeve/feature/FeatureService.kt`
- `shared/src/commonMain/kotlin/com/guyghost/wakeve/feature/FeatureRepository.kt`

### Android

- `wakeveApp/src/androidMain/kotlin/com/guyghost/wakeve/ui/feature/FeatureScreen.kt`
- Platform-specific dependencies:
  ```kotlin
  implementation("dependency:name:version")
  ```

### iOS

- `wakeveApp/wakeveApp/Views/FeatureView.swift`
- Platform-specific considerations:
  - [iOS-specific notes]

## Migration / Compatibility

### Database Migration (if applicable)

```sql
-- Version X: Add new table
CREATE TABLE new_table (
    id TEXT PRIMARY KEY,
    field1 TEXT NOT NULL,
    created_at INTEGER NOT NULL
);

-- Migrate existing data
INSERT INTO new_table (id, field1, created_at)
SELECT id, field1, created_at FROM old_table;
```

### API Versioning

- [Backward compatible / Breaking change]
- [Deprecation timeline]
- [Migration path for clients]

### Data Migration Strategy

1. **Phase 1**: [Description of first migration step]
2. **Phase 2**: [Description of second migration step]
3. **Phase 3**: [Description of final migration step]

## Implementation Files

### Contracts
- `shared/src/commonMain/kotlin/com/guyghost/wakeve/presentation/state/[Feature]Contract.kt`

### State Machines
- `shared/src/commonMain/kotlin/com/guyghost/wakeve/presentation/statemachine/[Feature]StateMachine.kt`

### Models
- `shared/src/commonMain/kotlin/com/guyghost/wakeve/models/[Feature]Models.kt`

### Repository
- `shared/src/commonMain/kotlin/com/guyghost/wakeve/repository/[Feature]Repository.kt`

### Routes (Server)
- `server/src/main/kotlin/com/guyghost/wakeve/routes/[Feature]Routes.kt`

### Tests
- `shared/src/commonTest/kotlin/com/guyghost/wakeve/[Feature]/[Feature]Test.kt`
- `shared/src/jvmTest/kotlin/com/guyghost/wakeve/[Feature]/[Feature]IntegrationTest.kt`

### Android UI
- `wakeveApp/src/androidMain/kotlin/com/guyghost/wakeve/ui/[feature]/[Feature]Screen.kt`

### iOS UI
- `wakeveApp/wakeveApp/Views/[Feature]View.swift`

## Related Specifications

- `[capability-1]`: [Relationship description]
- `[capability-2]`: [Relationship description]
- `[capability-3]`: [Relationship description]

## Internationalization

### User-Facing Strings

| Key | English | French | Context |
|-----|---------|--------|---------|
| `feature.title` | Feature Title | Titre de la fonctionnalité | Screen title |
| `feature.create` | Create | Créer | Button label |

## Performance Considerations

- [Caching strategy]
- [Database indexing]
- [API rate limits]
- [Offline behavior]

## Change History

| Version | Date | Changes |
|---------|------|---------|
| 1.0.0 | YYYY-MM-DD | Initial version |

## Acceptance Criteria

- [ ] [Criterion 1]
- [ ] [Criterion 2]
- [ ] [Criterion 3]
- [ ] [Criterion 4]

## Success Metrics

- [Metric 1]: [Target value]
- [Metric 2]: [Target value]
- [Metric 3]: [Target value]

---

**Spec Version**: [1.0.0]
**Last Updated**: [YYYY-MM-DD]
**Status**: [Draft | Proposed | Active | Deprecated]
**Maintainer**: [Team or individual responsible]
