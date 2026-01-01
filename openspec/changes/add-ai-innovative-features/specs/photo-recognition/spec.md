# Specification: Photo Recognition & Auto-Tagging

> **Change ID**: `add-ai-innovative-features`
> **Capability**: `collaboration-management` (extension)
> **Type**: Enhancement with New Capability
> **Date**: 2026-01-01

## Summary
The Photo Recognition & Auto-Tagging system provides an intelligent way to organize event memories. It automatically detects faces (locally for privacy), tags visual content (food, decoration, location), and groups photos into smart albums. Users can search their photo library using natural language or visual similarity.

## Requirements

### Requirement: Automatic Face Detection
**ID**: `photo-101`

The system SHALL automatically detect faces in event photos.

**Business Rules:**
- Face detection runs locally on-device for privacy
- Multiple faces can be detected in a single photo
- Detection works on photos up to 50 participants
- Confidence score returned (0.0 - 1.0)
- No facial recognition (faces are anonymous blobs, not identified)

**Scenarios:**
- **Scenario:** Group photo with 15 people
  - **GIVEN** Event photo uploaded
  - **WHEN** PhotoRecognitionService.processPhoto invoked
  - **THEN** 15 faces detected with 95%+ confidence
  - **THEN** Bounding boxes returned for each face

- **Scenario:** Photo with no people
  - **GIVEN** Landscape photo of a venue
  - **WHEN** PhotoRecognitionService.processPhoto invoked
  - **THEN** 0 faces detected
  - **THEN** Confidence score = 0.0

### Requirement: Automatic Tagging
**ID**: `photo-102`

The system SHALL automatically tag photo content based on visual recognition.

**Business Rules:**
- Tags are created from: people, food, decoration, location type (indoor/outdoor)
- Each tag has: label (ex: "Pizza Party"), confidence (0.0-1.0), category
- Tags are editable by users
- Auto-tagging runs locally on-device (Cloud Vision for iOS, ML Kit Vision for Android)
- Top 3 tags with highest confidence suggested
- No user data sent to cloud for privacy

**Supported Categories:**
- **People:** "Wedding Couple", "Friends Group", "Family"
- **Food:** "Pizza Party", "Bar Scene", "Dinner Table"
- **Decoration:** "Balloons", "Flowers", "Lights", "Banner"
- **Location Type:** "Indoor Restaurant", "Outdoor Park", "Beach", "Home", "Venue Hall"

**Scenarios:**
- **Scenario:** Wedding photo with food and decoration
  - **GIVEN** Photo uploaded from wedding
  - **WHEN** PhotoRecognitionService.tagPhoto invoked
  - **THEN** Tags: ["Wedding Couple" (0.95), "Dinner Table" (0.90), "Flowers" (0.88)]
  - **THEN** User can add/edit/remove tags

- **Scenario:** Outdoor event photo
  - **GIVEN** Photo uploaded from beach party
  - **WHEN** PhotoRecognitionService.tagPhoto invoked
  - **THEN** Tags: ["Beach Party" (0.92), "Friends Group" (0.85), "Outdoor Park" (0.78)]
  - **THEN** Location type = "outdoor" detected

### Requirement: Smart Albums
**ID**: `photo-103`

The system SHALL automatically group photos into intelligent albums based on events and metadata.

**Business Rules:**
- Albums created per event automatically
- Albums can be named: "Mariage de Sophie", "Soirée chez Jean"
- Albums sorted by date (newest first)
- Albums display cover photo (first photo or user-selected)
- Users can create custom albums
- Auto-album suggestions: "Summer 2025", "Team Building Events"

**Scenarios:**
- **Scenario:** Event with 50 photos
  - **GIVEN** User uploads photos during event
  - **WHEN** All photos processed
  - **THEN** Auto-album created with name derived from event type + date
  - **THEN** Photos sorted chronologically
  - **THEN** User sees album in "Mes Albums" section

- **Scenario:** Custom album creation
  - **GIVEN** User wants to group specific photos
  - **WHEN** User creates "Week-end Ski Trip" album
  - **THEN** Album created with selected photos
  - **THEN** Album appears with other albums

### Requirement: Photo Search & Discovery
**ID**: `photo-104`

The system SHALL enable photo search based on tags and visual similarity.

**Business Rules:**
- Full-text search on all tags and captions
- Visual similarity search (find similar photos)
- Search filters: by event, by date range, by person (face detection)
- Search results sorted by relevance score
- Users can add custom captions to photos

**Scenarios:**
- **Scenario:** User searches for "pizza party" photos
  - **GIVEN** User has 500 photos across multiple events
  - **WHEN** User queries with "pizza party"
  - **THEN** Top 20 most relevant results returned
  - **THEN** Results include photos from events tagged "Pizza Party"

- **Scenario:** Visual similarity search
  - **GIVEN** User finds a great photo in an album
  - **WHEN** User clicks "Find Similar Photos"
  - **THEN** System finds photos with similar composition
  - **THEN** Results ranked by visual similarity score

### Requirement: Privacy & Data Governance
**ID:** `photo-105`

The system SHALL process all recognition locally on-device without uploading images to server.

**Business Rules:**
- Face detection models run locally on device (Core ML Kit Vision / TensorFlow Lite)
- Auto-tagging models run locally
- No user biometric data sent to any cloud service
- Users can opt-out of auto-tagging entirely
- Photos and tags persist only in local SQLite database
- Photos backed up to cloud (Google Photos / iCloud) but no recognition processing on server

**Scenarios:**
- **Scenario:** Privacy-conscious user
  - **GIVEN** User enables auto-tagging
  - **WHEN** User uploads photo of friends
  - **THEN** Face detection runs locally
  - **THEN** Tags suggested based on visual content
  - **THEN** No data leaves device
  - **THEN** User can review and approve/reject suggestions

- **Scenario:** User opts out of auto-tagging
  - **GIVEN** User disables auto-tagging in settings
  - **WHEN** Photo uploaded
  - **THEN** No auto-tags suggested
  - **THEN** Manual tagging only

## Data Models

### Photo
```kotlin
@Serializable
data class Photo(
    val id: String,
    val eventId: String,
    val url: String,                 // Cloud storage URL (Google Photos/iCloud)
    val localPath: String?,             // Local file path
    val thumbnailUrl: String?,
    val caption: String?,
    val uploadedAt: String,              // ISO 8601
    val tags: List<PhotoTag>,           // Auto and manual tags
    val faceDetections: List<FaceDetection>, // Face bounding boxes
    val albums: List<String>,             // Album IDs
    val isFavorite: Boolean = false
)

data class FaceDetection(
    val boundingBox: BoundingBox,
    val confidence: Double              // 0.0 - 1.0
)

data class BoundingBox(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int
)

data class PhotoTag(
    val tagId: String,
    val label: String,                // "Pizza Party", "Wedding Couple"
    val confidence: Double,               // 0.0 - 1.0
    val category: PhotoCategory,          // PEOPLE, FOOD, DECORATION, LOCATION
    val source: TagSource,             // AUTO or MANUAL
    val suggestedAt: String?,             // ISO timestamp
)

enum class PhotoCategory {
    PEOPLE,
    FOOD,
    DECORATION,
    LOCATION
}

enum class TagSource {
    AUTO,
    MANUAL
}
```

### Album
```kotlin
@Serializable
data class Album(
    val id: String,
    val eventId: String,
    val name: String,                 // "Mariage de Sophie"
    val coverPhotoId: String?,           // First photo in album
    val photoIds: List<String>,           // All photo IDs in album
    val createdAt: String,
    val isAutoGenerated: Boolean = false  // True if auto-created by system
)
```

## API Changes

### POST /api/events/{eventId}/photos
Upload a new photo to the event.

**Request:**
```json
{
  "eventId": "event-123",
  "photoData": "base64_encoded_image",
  "caption": "Photo from our beach party!"
}
```

**Response:**
```json
{
  "photoId": "photo-456",
  "url": "https://photos.app/...",
  "suggestedTags": [
    {
      "tagId": "tag-789",
      "label": "Beach Party",
      "confidence": 0.92,
      "category": "LOCATION",
      "source": "AUTO"
    }
  ]
}
```

### GET /api/events/{eventId}/photos
List all photos for an event.

**Response:**
```json
{
  "photos": [
    {
      "id": "photo-456",
      "eventId": "event-123",
      "url": "...",
      "thumbnailUrl": "...",
      "caption": "...",
      "tags": [
        {
          "tagId": "tag-789",
          "label": "Beach Party",
          "source": "AUTO"
        }
      ]
    }
  ]
}
```

### POST /api/events/{eventId}/photos/{photoId}/tags
Add or update tags on a photo.

**Request:**
```json
{
  "tags": [
    {"tagId": "tag-789"},
    {"tagId": "custom-tag-123", "label": "Custom Tag"}
  ]
}
```

**Response:**
```json
{
  "status": "UPDATED",
  "tags": [
    {
      "tagId": "tag-789",
      "label": "Beach Party",
      "confidence": 0.92,
      "source": "MANUAL"
    }
  ]
}
```

### POST /api/events/{eventId}/albums
Create a new album.

**Request:**
```json
{
  "eventId": "event-123",
  "name": "Summer Trip 2025",
  "photoIds": ["photo-1", "photo-2", "photo-3"]
}
```

### GET /api/events/{eventId}/albums
List all albums for an event.

**Response:**
```json
{
  "albums": [
    {
      "id": "album-123",
      "eventId": "event-123",
      "name": "Mariage de Sophie",
      "coverPhotoId": "photo-1",
      "photoCount": 45,
      "createdAt": "2025-06-15T10:00:00Z"
    }
  ]
}
```

### GET /api/photos/search
Search photos by query.

**Query Parameters:**
- `q`: Search query
- `eventId` (optional): Filter by event
- `startDate` (optional): Date range start
- `endDate` (optional): Date range end
- `minConfidence` (optional): Minimum confidence threshold

**Response:**
```json
{
  "results": [
    {
      "photoId": "photo-123",
      "url": "...",
      "thumbnailUrl": "...",
      "relevanceScore": 0.87,
      "matchedTags": ["Beach Party", "Friends Group"]
    }
  ]
}
```

## Testing Requirements

### Unit Tests (shared)
- PhotoRecognitionServiceTest: 5 tests (face detection, tagging)
- AlbumServiceTest: 3 tests (creation, photo management)
- PhotoSearchServiceTest: 4 tests (text search, similarity search)
- PrivacyTest: 2 tests (local-only processing)

### Integration Tests
- PhotoWorkflowTest: 4 tests (upload → auto-tag → album creation)
- VisualSearchTest: 2 tests (similarity search workflow)
- OfflineSyncTest: 2 tests (photos upload offline, sync when online)

### Performance Tests
- FaceDetectionTest: 2 tests (< 2s per photo on mobile)
- TaggingPerformanceTest: 1 test (< 1s per photo)
- SearchPerformanceTest: 1 test (< 500ms for search)

### Accessibility
- VoiceOver/TalkBack: All photo management UI accessible
- Tag editing interface accessible to keyboard users

## Implementation Notes

### Face Detection Platforms
- **iOS**: Core ML Kit Vision (VNRecognizeRectanglesRequest)
- **Android**: Google ML Kit (FaceDetection)
- **Benefits**: On-device, privacy-preserving, fast (< 2s)

### Auto-Tagging Models
- **iOS**: Core ML Vision for image classification
- **Android**: TensorFlow Lite (TFLite)
- **Fallback**: Heuristic rules if ML confidence < 70%

### Offline Storage
- Photos: Local SQLite cache of metadata (not full images)
- Full Images: Backed up to cloud (Google Photos API, iCloud Photos)
- Sync: Background sync of new photos

### Tag Categories
- Pre-trained models for: PEOPLE (couples, groups, families), FOOD (20 categories), DECORATION (15 categories), LOCATION (10 categories)
- Custom tags: Users can add any tag manually
- Suggestion algorithm: Weighted scoring of visual features

### Search Algorithm
- Text search: TF-IDF (Term Frequency-Inverse Document Frequency)
- Visual similarity: CNN embeddings comparison with cosine similarity
- Hybrid scoring: 70% text + 30% visual
