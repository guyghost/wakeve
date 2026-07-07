package com.guyghost.wakeve.routes

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.guyghost.wakeve.JvmDatabaseFactory
import com.guyghost.wakeve.database.DatabaseProvider
import com.guyghost.wakeve.gamification.BadgeEligibilityChecker
import com.guyghost.wakeve.gamification.GamificationService
import com.guyghost.wakeve.gamification.UserPoints
import com.guyghost.wakeve.gamification.repository.InMemoryUserBadgesRepository
import com.guyghost.wakeve.gamification.repository.UserPointsRepository
import com.guyghost.wakeve.module
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.testApplication
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GamificationRoutesValidationTest {
    private val jwtSecret = System.getenv("JWT_SECRET") ?: "default-secret-key-change-in-production"
    private val jwtIssuer = System.getenv("JWT_ISSUER") ?: "wakev-api"
    private val jwtAudience = System.getenv("JWT_AUDIENCE") ?: "wakev-client"
    private val json = Json { ignoreUnknownKeys = true }

    @BeforeTest
    fun setup() {
        DatabaseProvider.resetDatabase()
    }

    @AfterTest
    fun teardown() {
        DatabaseProvider.resetDatabase()
    }

    @Test
    fun `leaderboard reads query parameters for limit and type`() = testApplication {
        val database = DatabaseProvider.getDatabase(JvmDatabaseFactory(":memory:"))
        val pointsRepository = FixedUserPointsRepository(
            listOf(
                userPoints("stale-user", 300, lastUpdated = "2020-01-01T00:00:00Z"),
                userPoints("fresh-user", 200, lastUpdated = Clock.System.now().toString()),
                userPoints("other-fresh-user", 100, lastUpdated = Clock.System.now().toString())
            )
        )
        val badgesRepository = InMemoryUserBadgesRepository()
        val gamificationService = GamificationService(
            pointsRepository,
            badgesRepository,
            BadgeEligibilityChecker(pointsRepository, badgesRepository)
        )
        val token = createTestJwt("leaderboard-user")
        val client = createClient {
            install(ContentNegotiation) { json(json) }
        }

        application {
            module(
                database = database,
                gamificationService = gamificationService
            )
        }

        val limited = client.get("/api/leaderboard?limit=1") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        val limitedText = limited.bodyAsText()
        val limitedEntries = json.parseToJsonElement(limitedText)
            .jsonObject
            .getValue("leaderboard")
            .jsonArray

        assertEquals(HttpStatusCode.OK, limited.status, limitedText)
        assertEquals(1, limitedEntries.size)
        assertEquals("stale-user", limitedEntries[0].jsonObject.getValue("userId").jsonPrimitive.content)

        val thisWeek = client.get("/api/leaderboard?type=THIS_WEEK&limit=10") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        val thisWeekText = thisWeek.bodyAsText()
        val thisWeekEntries = json.parseToJsonElement(thisWeekText)
            .jsonObject
            .getValue("leaderboard")
            .jsonArray
        val thisWeekUserIds = thisWeekEntries.map {
            it.jsonObject.getValue("userId").jsonPrimitive.content
        }

        assertEquals(HttpStatusCode.OK, thisWeek.status, thisWeekText)
        assertEquals(2, thisWeekEntries.size)
        assertTrue("fresh-user" in thisWeekUserIds, thisWeekText)
        assertTrue("other-fresh-user" in thisWeekUserIds, thisWeekText)
        assertFalse("stale-user" in thisWeekUserIds, thisWeekText)
    }

    @Test
    fun `default gamification service unlocks points milestone badges`() = testApplication {
        val database = DatabaseProvider.getDatabase(JvmDatabaseFactory(":memory:"))
        val token = createTestJwt("admin-user", role = "ADMIN")
        val client = createClient {
            install(ContentNegotiation) { json(json) }
        }

        application { module(database = database) }

        client.post("/api/users/gamified-user/points/award") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody("""{"action":"CREATE_EVENT"}""")
        }
        val secondAward = client.post("/api/users/gamified-user/points/award") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody("""{"action":"CREATE_EVENT"}""")
        }
        val secondAwardText = secondAward.bodyAsText()

        assertEquals(HttpStatusCode.OK, secondAward.status, secondAwardText)

        val badgeIds = json.parseToJsonElement(secondAwardText)
            .jsonObject
            .getValue("badgesUnlocked")
            .jsonArray
            .map { it.jsonObject.getValue("id").jsonPrimitive.content }

        assertTrue("badge-century-club" in badgeIds, secondAwardText)
    }

    @Test
    fun `award points rejects another authenticated user target`() = testApplication {
        val database = DatabaseProvider.getDatabase(JvmDatabaseFactory(":memory:"))
        val token = createTestJwt("attacker-user")
        val client = createClient {
            install(ContentNegotiation) { json(json) }
        }

        application { module(database = database) }

        val response = client.post("/api/users/victim-user/points/award") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody("""{"action":"CREATE_EVENT"}""")
        }

        assertEquals(HttpStatusCode.Forbidden, response.status, response.bodyAsText())
    }

    @Test
    fun `award points rejects direct self service point farming`() = testApplication {
        val database = DatabaseProvider.getDatabase(JvmDatabaseFactory(":memory:"))
        val token = createTestJwt("regular-user")
        val client = createClient {
            install(ContentNegotiation) { json(json) }
        }

        application { module(database = database) }

        val response = client.post("/api/users/regular-user/points/award") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody("""{"action":"CREATE_EVENT"}""")
        }

        assertEquals(HttpStatusCode.Forbidden, response.status, response.bodyAsText())
    }

    @Test
    fun `user points and badges are readable by self but hidden from other users`() = testApplication {
        val database = DatabaseProvider.getDatabase(JvmDatabaseFactory(":memory:"))
        val token = createTestJwt("regular-user")
        val client = createClient {
            install(ContentNegotiation) { json(json) }
        }

        application { module(database = database) }

        val ownPoints = client.get("/api/users/regular-user/points") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        val ownBadges = client.get("/api/users/regular-user/badges") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        val otherPoints = client.get("/api/users/other-user/points") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        val otherBadges = client.get("/api/users/other-user/badges") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }

        assertEquals(HttpStatusCode.OK, ownPoints.status, ownPoints.bodyAsText())
        assertEquals(HttpStatusCode.OK, ownBadges.status, ownBadges.bodyAsText())
        assertEquals(HttpStatusCode.Forbidden, otherPoints.status, otherPoints.bodyAsText())
        assertEquals(HttpStatusCode.Forbidden, otherBadges.status, otherBadges.bodyAsText())
    }

    @Test
    fun `admin can read another user's gamification profile`() = testApplication {
        val database = DatabaseProvider.getDatabase(JvmDatabaseFactory(":memory:"))
        val token = createTestJwt("admin-user", role = "ADMIN")
        val client = createClient {
            install(ContentNegotiation) { json(json) }
        }

        application { module(database = database) }

        val points = client.get("/api/users/other-user/points") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        val badges = client.get("/api/users/other-user/badges") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }

        assertEquals(HttpStatusCode.OK, points.status, points.bodyAsText())
        assertEquals(HttpStatusCode.OK, badges.status, badges.bodyAsText())
    }

    private fun userPoints(userId: String, totalPoints: Int, lastUpdated: String): UserPoints =
        UserPoints(
            userId = userId,
            totalPoints = totalPoints,
            eventCreationPoints = totalPoints,
            lastUpdated = lastUpdated
        )

    private fun createTestJwt(userId: String, role: String = "USER"): String =
        JWT.create()
            .withIssuer(jwtIssuer)
            .withAudience(jwtAudience)
            .withClaim("userId", userId)
            .withClaim("role", role)
            .withClaim("sessionId", "test-session-$userId")
            .withClaim("permissions", listOf("READ", "WRITE"))
            .withExpiresAt(java.util.Date(System.currentTimeMillis() + 3_600_000))
            .sign(Algorithm.HMAC256(jwtSecret))
}

private class FixedUserPointsRepository(
    users: List<UserPoints>
) : UserPointsRepository {
    private val usersById = users.associateBy { it.userId }.toMutableMap()

    override suspend fun getUserPoints(userId: String): UserPoints? = usersById[userId]

    override suspend fun getUserPointsOrDefault(userId: String): UserPoints =
        usersById[userId] ?: createUserPoints(userId)

    override suspend fun createUserPoints(userId: String): UserPoints {
        val userPoints = UserPoints(userId = userId, lastUpdated = Clock.System.now().toString())
        usersById[userId] = userPoints
        return userPoints
    }

    override suspend fun incrementEventCreationPoints(userId: String, points: Int): UserPoints =
        increment(userId, points) { current, total ->
            current.copy(eventCreationPoints = current.eventCreationPoints + points, totalPoints = total)
        }

    override suspend fun incrementVotingPoints(userId: String, points: Int): UserPoints =
        increment(userId, points) { current, total ->
            current.copy(votingPoints = current.votingPoints + points, totalPoints = total)
        }

    override suspend fun incrementCommentPoints(userId: String, points: Int): UserPoints =
        increment(userId, points) { current, total ->
            current.copy(commentPoints = current.commentPoints + points, totalPoints = total)
        }

    override suspend fun incrementParticipationPoints(userId: String, points: Int): UserPoints =
        increment(userId, points) { current, total ->
            current.copy(participationPoints = current.participationPoints + points, totalPoints = total)
        }

    override suspend fun applyPointsDecay(userId: String): UserPoints? = usersById[userId]

    override suspend fun getTopPointEarners(limit: Int): List<UserPoints> =
        usersById.values.sortedByDescending { it.totalPoints }.take(limit)

    override suspend fun getPointsStatistics(): Map<String, Long> =
        mapOf(
            "userCount" to usersById.size.toLong(),
            "totalPoints" to usersById.values.sumOf { it.totalPoints.toLong() }
        )

    override suspend fun userHasMinimumPoints(userId: String, minimumPoints: Int): Boolean =
        (usersById[userId]?.totalPoints ?: 0) >= minimumPoints

    override suspend fun getTotalPoints(userId: String): Int =
        usersById[userId]?.totalPoints ?: 0

    override suspend fun deleteUserPoints(userId: String) {
        usersById.remove(userId)
    }

    private suspend fun increment(
        userId: String,
        points: Int,
        update: (UserPoints, Int) -> UserPoints
    ): UserPoints {
        val current = getUserPointsOrDefault(userId)
        val updated = update(current, current.totalPoints + points)
            .copy(lastUpdated = Clock.System.now().toString())
        usersById[userId] = updated
        return updated
    }
}
