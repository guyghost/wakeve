# Specification: Gamification of Participation

> **Change ID**: `add-ai-innovative-features`
> **Capability**: `gamification`
> **Type**: New Feature
> **Date**: 2026-01-01

## Summary
Système de gamification complet conçu pour motiver l'engagement des utilisateurs au sein de l'application Wakeve. Le système récompense les actions positives (création d'événements, votes rapides, participation active) par des points, des badges et des classements sociaux, transformant la planification logistique en une expérience ludique et gratifiante.

## ADDED Requirements

### Requirement: Points System
**ID**: `gamification-101`

The system SHALL implement a points system that rewards user participation in event activities.

**Business Rules:**
- Points are earned for multiple actions (creating events, voting, commenting, etc.)
- Points decay over time to encourage ongoing participation
- Points are not transferable between users
- Leaderboard ranks users by total points

**Scenarios:**
- Given user creates an event
- Then User earns +50 "Creator" points
- Given user votes on a poll slot
- Then User earns +5 points per vote
- Given user comments on a scenario
- Then User earns +10 points per comment

### Requirement: Badges
**ID**: `gamification-102`

The system SHALL award badges for specific achievements.

**Business Rules:**
- Badges are non-transferable
- Each badge can be earned only once per user
- Badges are permanent (no expiration)
- Badges display on user profile

**Scenarios:**
- Given user has created 10 events
- Then User earns "Super Organizer" badge
- Given user votes on 24 hours straight
- Then User earns "Early Bird" badge
- Given user organizes 5 events in a month
- Then User earns "Event Master" badge

### Requirement: Leaderboards
**ID**: `gamification-103`

The system SHALL display rankings of users based on points and badges.

**Business Rules:**
- Leaderboard can be filtered by: all time, this month, this week
- Leaderboard displays top 20 users
- Anonymous option available for privacy
- Friends leaderboard shows only user's friends

**Scenarios:**
- Given user clicks on leaderboard
- Then System displays top 20 users ranked by points
- Given user selects "Friends only"
- Then System shows ranking of user's friends only
- Given user enables "Anonymous mode"
- Then User is not shown on leaderboard

## Data Models

### UserPoints
```kotlin
@Serializable
data class UserPoints(
    val userId: String,
    val totalPoints: Int,
    val eventCreationPoints: Int,
    val votingPoints: Int,
    val commentPoints: Int,
    val participationPoints: Int,
    val decayPoints: Int,  // Points lost over time
    val lastUpdated: String
)
```

### UserBadges
```kotlin
@Serializable
data class UserBadges(
    val userId: String,
    val badges: List<Badge>,
    val unlockedAt: Map<String, String>  // badgeId -> timestamp
)

@Serializable
data class Badge(
    val id: String,
    val name: String,           // "Super Organizer"
    val description: String,      // "Created 10 events"
    val icon: String,               // Emoji or icon name
    val requirement: Int,          // Minimum points to unlock
    val pointsReward: Int,          // Points earned when unlocked
    val category: BadgeCategory,     // CREATION, VOTING, PARTICIPATION, ENGAGEMENT
    val rarity: BadgeRarity       // COMMON, RARE, EPIC, LEGENDARY
)

enum class BadgeCategory {
    CREATION,
    VOTING,
    PARTICIPATION,
    ENGAGEMENT
}

enum class BadgeRarity {
    COMMON,
    RARE,
    EPIC,
    LEGENDARY
}
```

### LeaderboardEntry
```kotlin
@Serializable
data class LeaderboardEntry(
    val userId: String,
    val username: String,
    val totalPoints: Int,
    val badgesCount: Int,
    val rank: Int,
    val isCurrentUser: Boolean = false,
    val isFriend: Boolean = false
)
```

### Points Rules
```kotlin
@Serializable
data class PointsRule(
    val action: String,           // "create_event", "vote", "comment"
    val points: Int,              // Points earned
    val maxPerDay: Int?,           // Optional daily limit
    val maxPerUser: Int?,           // Optional lifetime limit
    val decayRate: Double?          // Points lost per day (optional)
)
```

## API Changes

### GET /api/gamification/points/{userId}
Get current points for a user.

**Response:**
```json
{
  "totalPoints": 1250,
  "eventCreationPoints": 500,
  "votingPoints": 300,
  "commentPoints": 250,
  "participationPoints": 200,
  "decayedPoints": 0
}
```

### GET /api/gamification/badges/{userId}
Get all badges for a user.

**Response:**
```json
{
  "badges": [
    {
      "id": "badge-super-organizer",
      "name": "Super Organisateur",
      "description": "A créé 10 événements",
      "icon": "🏆",
      "category": "CREATION",
      "rarity": "EPIC",
      "requirement": 500,
      "pointsReward": 100,
      "unlockedAt": "2026-01-01T10:30:00Z"
    }
  ]
}
```

### GET /api/gamification/leaderboard
Get the leaderboard.

**Query Parameters:**
- `type`: "all" | "month" | "week" | "friends"
- `limit`: default 20, max 100

**Response:**
```json
{
  "entries": [
    {
      "userId": "user-123",
      "username": "Jean Dupont",
      "totalPoints": 2450,
      "badgesCount": 12,
      "rank": 3,
      "isCurrentUser": false,
      "isFriend": true
    }
  ]
}
```

### POST /api/gamification/events/{eventId}/complete
Award points when user completes an event participation.

**Request:**
```json
{
  "userId": "user-456"
}
```

**Response:**
```json
{
  "pointsEarned": 50,
  "newTotal": 1300,
  "badgeUnlocked": "badge-first-event",
  "message": "Félicitations! Vous avez gagné 50 points et le badge 'Premier événement'!"
}
```

### POST /api/gamification/achievements/{userId}
Manually award points/badges (admin only).

**Request:**
```json
{
  "points": 100,
  "badgeId": "badge-custom",
  "reason": "Bonus participation campagne"
}
```

## Testing Requirements

### Unit Tests (shared)
- GamificationServiceTest: 8 tests
  - Test points calculation
  - Test badge unlocking logic
  - Test leaderboard ranking
  - Test point decay

### Integration Tests
- GamificationWorkflowTest: 3 tests
  - Complete event participation workflow
  - Points awarding on multiple actions
- Leaderboard updates

### Performance Tests
- GamificationPerformanceTest: 3 tests
- Leaderboard query < 100ms
- Points calculation < 50ms
- Badge check < 10ms

## Implementation Notes

### Phase 1: Core Gamification (Sprint 1-2)
- UserPointsRepository
- GamificationService (logic)
- Badge system with predefined badges
- Basic leaderboard

### Phase 2: Enhanced Gamification (Sprint 3-4)
- Dynamic badges based on events
- Leaderboard filters and privacy
- Achievements system
- Notifications for badges

### Phase 3: Social Features (Sprint 5-6)
- Friend leaderboards
- Badge sharing
- Points transfer between organizers and participants
- Challenge system

### Dependencies
- Requires: EventRepository (exists)
- Requires: CommentRepository (from Phase 4 collaboration)
- Requires: PollService (exists)

### Backward Compatibility
- Users without points/badges start at 0
- Existing events retroactively awarded points

## Success Criteria

### Functional
- ✅ Users can earn points for all actions
- ✅ Badges unlock correctly at thresholds
- ✅ Leaderboard displays correct rankings
- ✅ Anonymous mode respects privacy
- ✅ Friend filtering works

### Non-Functional
- ✅ Performance: Leaderboard < 100ms
- ✅ Tests: 8 unit + 3 integration = 11 tests
- ✅ Documentation: Specs + guides
