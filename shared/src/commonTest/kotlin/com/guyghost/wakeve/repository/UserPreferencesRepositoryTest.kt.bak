package com.guyghost.wakeve.repository

import com.guyghost.wakeve.database.WakevDb
import com.guyghost.wakeve.models.*
import com.guyghost.wakeve.sqldelight.User_preferences
import com.guyghost.wakeve.sqldelight.Preference_interaction
import com.guyghost.wakeve.sqldelight.UserPreferencesQueries
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*

/**
 * Unit tests for UserPreferencesRepository.
 * Tests the preference learning and storage functionality.
 */
class UserPreferencesRepositoryTest {

    private lateinit var database: WakevDb
    private lateinit var userPreferencesQueries: UserPreferencesQueries
    private lateinit var repository: UserPreferencesRepository
    private val json = Json { prettyPrint = false; ignoreUnknownKeys = true }

    @Before
    fun setup() {
        database = mock()
        userPreferencesQueries = mock()

        whenever(database.userPreferencesQueries).thenReturn(userPreferencesQueries)

        repository = UserPreferencesRepository(database)
    }

    @Test
    fun `getUserPreferences returns null when no preferences exist`() = runTest {
        // Given
        whenever(userPreferencesQueries.selectPreferencesByUserId("user123").executeAsOneOrNull())
            .thenReturn(null)

        // When
        val result = repository.getUserPreferences("user123")

        // Then
        assertNull(result)
    }

    @Test
    fun `getUserPreferences returns preferences when found`() = runTest {
        // Given
        val row = User_preferences(
            user_id = "user123",
            preferred_days = "[MONDAY,FRIDAY]",
            preferred_time_of_day = "[MORNING,EVENING]",
            preferred_event_types = "[BIRTHDAY,PARTY]",
            preferred_locations = "[\"Paris\",\"Lyon\"]",
            avoid_events = 0L,
            score_weights = "{\"proximityWeight\":0.3,\"typeMatchWeight\":0.2,\"seasonalityWeight\":0.3,\"socialWeight\":0.2,\"totalWeight\":1.0}",
            last_updated = "2024-01-15T10:00:00Z"
        )

        whenever(userPreferencesQueries.selectPreferencesByUserId("user123").executeAsOneOrNull())
            .thenReturn(row)

        // When
        val result = repository.getUserPreferences("user123")

        // Then
        assertNotNull(result)
        assertEquals("user123", result?.userId)
        assertEquals(listOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY), result?.preferredDays)
        assertEquals(listOf(TimeOfDay.MORNING, TimeOfDay.EVENING), result?.preferredTimeOfDay)
        assertEquals(listOf(EventType.BIRTHDAY, EventType.PARTY), result?.preferredEventTypes)
        assertEquals(listOf("Paris", "Lyon"), result?.preferredLocations)
        assertFalse(result?.avoidEvents ?: true)
    }

    @Test
    fun `updateUserPreferences inserts new preferences`() = runTest {
        // Given
        val preferences = UserPreference(
            userId = "user123",
            preferredDays = listOf(DayOfWeek.SATURDAY),
            preferredTimeOfDay = listOf(TimeOfDay.EVENING),
            preferredEventTypes = listOf(EventType.WEDDING),
            preferredLocations = listOf("Nice"),
            avoidEvents = false,
            scoreWeights = ScoreWeights.DEFAULT,
            lastUpdated = "2024-01-15T10:00:00Z"
        )

        whenever(userPreferencesQueries.selectPreferencesByUserId("user123").executeAsOneOrNull())
            .thenReturn(null)

        // When
        repository.updateUserPreferences("user123", preferences)

        // Then
        verify(userPreferencesQueries).insertPreferences(
            user_id = eq("user123"),
            preferred_days = eq(json.encodeToString(listOf(DayOfWeek.SATURDAY))),
            preferred_time_of_day = eq(json.encodeToString(listOf(TimeOfDay.EVENING))),
            preferred_event_types = eq(json.encodeToString(listOf(EventType.WEDDING))),
            preferred_locations = eq(json.encodeToString(listOf("Nice"))),
            avoid_events = eq(0L),
            score_weights = any(),
            last_updated = any()
        )
    }

    @Test
    fun `recordVote creates interaction record`() = runTest {
        // When
        repository.recordVote(
            userId = "user123",
            eventId = "event456",
            voteType = VoteType.YES,
            timeOfDay = TimeOfDay.AFTERNOON,
            dayOfWeek = DayOfWeek.SATURDAY
        )

        // Then
        verify(userPreferencesQueries).insertInteraction(
            id = any(),
            user_id = eq("user123"),
            event_id = eq("event456"),
            interaction_type = eq("VOTE"),
            event_type = isNull(),
            time_of_day = eq("AFTERNOON"),
            vote_type = eq("YES"),
            day_of_week = eq("SATURDAY"),
            location = isNull(),
            timestamp = any(),
            synced = eq(0L)
        )
    }

    @Test
    fun `recordEventCreation creates interaction with event type`() = runTest {
        // When
        repository.recordEventCreation(
            userId = "user123",
            eventId = "event456",
            eventType = EventType.BIRTHDAY
        )

        // Then
        verify(userPreferencesQueries).insertInteraction(
            id = any(),
            user_id = eq("user123"),
            event_id = eq("event456"),
            interaction_type = eq("EVENT_CREATION"),
            event_type = eq("BIRTHDAY"),
            time_of_day = isNull(),
            vote_type = isNull(),
            day_of_week = isNull(),
            location = isNull(),
            timestamp = any(),
            synced = eq(0L)
        )
    }

    @Test
    fun `calculateImplicitPreferences returns empty for no interactions`() = runTest {
        // Given
        whenever(userPreferencesQueries.selectRecentInteractionsByUserId(any(), any()).executeAsList())
            .thenReturn(emptyList())

        // When
        val result = repository.calculateImplicitPreferences("user123", decayDays = 30)

        // Then
        assertNotNull(result)
        assertEquals("user123", result.userId)
        assertTrue(result.preferredDays.isEmpty())
        assertTrue(result.preferredTimeOfDay.isEmpty())
        assertTrue(result.preferredEventTypes.isEmpty())
        assertTrue(result.preferredLocations.isEmpty())
    }

    @Test
    fun `calculateImplicitPreferences learns from YES votes`() = runTest {
        // Given
        val yesterday = "2024-01-14T10:00:00Z"

        val interactions = listOf(
            createInteractionRow(
                id = "int1",
                user_id = "user123",
                event_id = "event1",
                interaction_type = "VOTE",
                time_of_day = "MORNING",
                vote_type = "YES",
                day_of_week = "MONDAY",
                timestamp = yesterday
            )
        )

        whenever(userPreferencesQueries.selectRecentInteractionsByUserId(any(), any()).executeAsList())
            .thenReturn(interactions)

        // When
        val result = repository.calculateImplicitPreferences("user123", decayDays = 30)

        // Then
        assertTrue(result.preferredDays.contains(DayOfWeek.MONDAY))
        assertTrue(result.preferredTimeOfDay.contains(TimeOfDay.MORNING))
    }

    @Test
    fun `calculateImplicitPreferences learns from event creation`() = runTest {
        // Given
        val yesterday = "2024-01-14T10:00:00Z"

        val interactions = listOf(
            createInteractionRow(
                id = "int1",
                user_id = "user123",
                event_id = "event1",
                interaction_type = "EVENT_CREATION",
                event_type = "TEAM_BUILDING",
                timestamp = yesterday
            )
        )

        whenever(userPreferencesQueries.selectRecentInteractionsByUserId(any(), any()).executeAsList())
            .thenReturn(interactions)

        // When
        val result = repository.calculateImplicitPreferences("user123", decayDays = 30)

        // Then
        assertTrue(result.preferredEventTypes.contains(EventType.TEAM_BUILDING))
    }

    @Test
    fun `calculateImplicitPreferences learns from participation with location`() = runTest {
        // Given
        val yesterday = "2024-01-14T10:00:00Z"

        val interactions = listOf(
            createInteractionRow(
                id = "int1",
                user_id = "user123",
                event_id = "event1",
                interaction_type = "PARTICIPATION",
                location = "Marseille",
                timestamp = yesterday
            )
        )

        whenever(userPreferencesQueries.selectRecentInteractionsByUserId(any(), any()).executeAsList())
            .thenReturn(interactions)

        // When
        val result = repository.calculateImplicitPreferences("user123", decayDays = 30)

        // Then
        assertTrue(result.preferredLocations.contains("Marseille"))
    }

    @Test
    fun `applyDecay deletes old interactions`() = runTest {
        // When
        repository.applyDecay("user123", decayDays = 30)

        // Then
        verify(userPreferencesQueries).deleteOldInteractions(any())
    }

    @Test
    fun `UserPreference empty factory creates empty preferences`() {
        // When
        val empty = UserPreference.empty("user123")

        // Then
        assertEquals("user123", empty.userId)
        assertTrue(empty.preferredDays.isEmpty())
        assertTrue(empty.preferredTimeOfDay.isEmpty())
        assertTrue(empty.preferredEventTypes.isEmpty())
        assertTrue(empty.preferredLocations.isEmpty())
        assertFalse(empty.avoidEvents)
        assertEquals(ScoreWeights.DEFAULT, empty.scoreWeights)
    }

    @Test
    fun `ScoreWeights isValid returns true for default weights`() {
        // When/Then
        assertTrue(ScoreWeights.DEFAULT.isValid())
    }

    @Test
    fun `ScoreWeights isValid returns false for invalid weights`() {
        // Given
        val invalid = ScoreWeights(
            proximityWeight = 0.5,
            typeMatchWeight = 0.5,
            seasonalityWeight = 0.5,
            socialWeight = 0.5,
            totalWeight = 1.0
        )

        // When/Then
        assertFalse(invalid.isValid())
    }

    @Test
    fun `getInteractionHistory returns mapped interactions`() = runTest {
        // Given
        val row = createInteractionRow(
            id = "int1",
            user_id = "user123",
            event_id = "event1",
            interaction_type = "VOTE",
            time_of_day = "MORNING",
            vote_type = "YES",
            day_of_week = "MONDAY",
            timestamp = "2024-01-14T10:00:00Z"
        )

        whenever(userPreferencesQueries.selectInteractionsByUserId("user123").executeAsList())
            .thenReturn(listOf(row))

        // When
        val result = repository.getInteractionHistory("user123")

        // Then
        assertEquals(1, result.size)
        assertEquals("int1", result[0].id)
        assertEquals("user123", result[0].userId)
        assertEquals(InteractionType.VOTE, result[0].interactionType)
        assertEquals(TimeOfDay.MORNING, result[0].timeOfDay)
        assertEquals(VoteType.YES, result[0].voteType)
        assertEquals(DayOfWeek.MONDAY, result[0].dayOfWeek)
    }

    @Test
    fun `deleteUserData removes all user data`() = runTest {
        // When
        repository.deleteUserData("user123")

        // Then
        verify(userPreferencesQueries).deletePreferences("user123")
        verify(userPreferencesQueries).deleteOldInteractions(eq("1970-01-01T00:00:00Z"))
    }

    @Test
    fun `mergeWithExplicit prefers explicit over implicit`() = runTest {
        // Given
        val explicitPreferences = UserPreference(
            userId = "user123",
            preferredDays = listOf(DayOfWeek.SUNDAY),  // Explicit preference
            preferredTimeOfDay = emptyList(),  // Will use implicit
            preferredEventTypes = listOf(EventType.WEDDING),
            preferredLocations = listOf("Paris"),
            avoidEvents = true,
            scoreWeights = ScoreWeights.DEFAULT,
            lastUpdated = "2024-01-15T10:00:00Z"
        )

        val yesterday = "2024-01-14T10:00:00Z"

        val interactions = listOf(
            createInteractionRow(
                id = "int1",
                user_id = "user123",
                event_id = "event1",
                interaction_type = "VOTE",
                time_of_day = "EVENING",
                vote_type = "YES",
                day_of_week = "SATURDAY",
                timestamp = yesterday
            )
        )

        whenever(userPreferencesQueries.selectRecentInteractionsByUserId(any(), any()).executeAsList())
            .thenReturn(interactions)

        // When
        val result = repository.mergeWithExplicit("user123", explicitPreferences)

        // Then
        assertEquals(listOf(DayOfWeek.SUNDAY), result.preferredDays)  // Explicit
        assertEquals(listOf(TimeOfDay.EVENING), result.preferredTimeOfDay)  // From implicit
        assertEquals(listOf(EventType.WEDDING), result.preferredEventTypes)  // Explicit
        assertEquals(listOf("Paris"), result.preferredLocations)  // Explicit
        assertTrue(result.avoidEvents)  // Explicit
    }

    @Test
    fun `NO votes decrease preference score`() = runTest {
        // Given
        val yesterday = "2024-01-14T10:00:00Z"

        val interactions = listOf(
            createInteractionRow(
                id = "int1",
                user_id = "user123",
                event_id = "event1",
                interaction_type = "VOTE",
                time_of_day = "MORNING",
                vote_type = "NO",
                day_of_week = "MONDAY",
                timestamp = yesterday
            )
        )

        whenever(userPreferencesQueries.selectRecentInteractionsByUserId(any(), any()).executeAsList())
            .thenReturn(interactions)

        // When
        val result = repository.calculateImplicitPreferences("user123", decayDays = 30)

        // Then
        // NO votes should result in negative weight, so the day should not be in preferredDays
        assertTrue(result.preferredDays.isEmpty() || !result.preferredDays.contains(DayOfWeek.MONDAY))
    }

    @Test
    fun `multiple YES votes for same day increase preference score`() = runTest {
        // Given
        val yesterday = "2024-01-14T10:00:00Z"
        val twoDaysAgo = "2024-01-13T10:00:00Z"

        val interactions = listOf(
            createInteractionRow(
                id = "int1",
                user_id = "user123",
                event_id = "event1",
                interaction_type = "VOTE",
                time_of_day = "MORNING",
                vote_type = "YES",
                day_of_week = "SATURDAY",
                timestamp = yesterday
            ),
            createInteractionRow(
                id = "int2",
                user_id = "user123",
                event_id = "event2",
                interaction_type = "VOTE",
                time_of_day = "AFTERNOON",
                vote_type = "YES",
                day_of_week = "SATURDAY",
                timestamp = twoDaysAgo
            )
        )

        whenever(userPreferencesQueries.selectRecentInteractionsByUserId(any(), any()).executeAsList())
            .thenReturn(interactions)

        // When
        val result = repository.calculateImplicitPreferences("user123", decayDays = 30)

        // Then
        assertTrue(result.preferredDays.contains(DayOfWeek.SATURDAY))
    }

    // Helper function for creating mock rows

    private fun createInteractionRow(
        id: String,
        user_id: String,
        event_id: String,
        interaction_type: String,
        event_type: String? = null,
        time_of_day: String? = null,
        vote_type: String? = null,
        day_of_week: String? = null,
        location: String? = null,
        timestamp: String
    ): Preference_interaction {
        return Preference_interaction(
            id = id,
            user_id = user_id,
            event_id = event_id,
            interaction_type = interaction_type,
            event_type = event_type,
            time_of_day = time_of_day,
            vote_type = vote_type,
            day_of_week = day_of_week,
            location = location,
            timestamp = timestamp,
            synced = 0L
        )
    }
}
