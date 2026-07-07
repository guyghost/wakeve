package com.guyghost.wakeve.routes

import com.guyghost.wakeve.gamification.GamificationService
import com.guyghost.wakeve.gamification.LeaderboardType
import com.guyghost.wakeve.gamification.PointsAction
import com.guyghost.wakeve.gamification.UserLevel
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import kotlinx.serialization.Serializable

/**
 * Gamification API Routes
 *
 * Provides endpoints for:
 * - GET /api/leaderboard?limit=&type=&eventId= - Get leaderboard rankings
 * - GET /api/users/{id}/badges - Get user's earned badges
 * - GET /api/users/{id}/points - Get user's point total, breakdown, and level
 * - POST /api/users/{id}/points/award - Award points for an action
 */
fun io.ktor.server.routing.Route.gamificationRoutes(gamificationService: GamificationService) {

    // GET /api/leaderboard - Get top users by points
    route("/leaderboard") {
        get {
            try {
                val queryParameters = call.request.queryParameters
                val limit = queryParameters["limit"]?.toIntOrNull() ?: 20
                val typeParam = queryParameters["type"] ?: "ALL_TIME"

                val type = try {
                    LeaderboardType.valueOf(typeParam.uppercase())
                } catch (e: IllegalArgumentException) {
                    LeaderboardType.ALL_TIME
                }

                val leaderboard = gamificationService.getLeaderboard(
                    type = type,
                    limit = limit.coerceIn(1, 100)
                )

                call.respond(HttpStatusCode.OK, mapOf("leaderboard" to leaderboard))
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to leaderboardFetchFailureMessage())
                )
            }
        }
    }

    // User-specific gamification routes
    route("/users/{userId}") {

        // GET /api/users/{id}/badges - Get user's badges
        route("/badges") {
            get {
                try {
                    val userId = call.parameters["userId"] ?: return@get call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "User ID required")
                    )
                    val principal = call.principal<JWTPrincipal>() ?: return@get call.respond(
                        HttpStatusCode.Unauthorized,
                        mapOf("error" to "Not authenticated")
                    )
                    if (!principal.canReadGamificationProfile(userId)) {
                        return@get call.respond(
                            HttpStatusCode.Forbidden,
                            mapOf("error" to "You cannot read another user's gamification profile")
                        )
                    }

                    val badges = gamificationService.getUserBadges(userId)

                    call.respond(
                        HttpStatusCode.OK,
                        UserBadgesResponse(
                            userId = userId,
                            badges = badges,
                            count = badges.size
                        )
                    )
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        mapOf("error" to userBadgesFetchFailureMessage())
                    )
                }
            }
        }

        // GET /api/users/{id}/points - Get user's point total and level
        route("/points") {
            get {
                try {
                    val userId = call.parameters["userId"] ?: return@get call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "User ID required")
                    )
                    val principal = call.principal<JWTPrincipal>() ?: return@get call.respond(
                        HttpStatusCode.Unauthorized,
                        mapOf("error" to "Not authenticated")
                    )
                    if (!principal.canReadGamificationProfile(userId)) {
                        return@get call.respond(
                            HttpStatusCode.Forbidden,
                            mapOf("error" to "You cannot read another user's gamification profile")
                        )
                    }

                    val userPoints = gamificationService.getUserPoints(userId)
                    val userLevel = gamificationService.getUserLevel(userId)

                    if (userPoints == null) {
                        val defaultLevel = UserLevel.fromPoints(0)
                        call.respond(HttpStatusCode.OK, UserPointsResponse(
                            userId = userId,
                            totalPoints = 0,
                            eventCreationPoints = 0,
                            votingPoints = 0,
                            commentPoints = 0,
                            participationPoints = 0,
                            level = defaultLevel.level,
                            levelName = defaultLevel.name,
                            pointsForNextLevel = defaultLevel.pointsForNextLevel,
                            progressToNextLevel = defaultLevel.progressToNextLevel
                        ))
                    } else {
                        call.respond(HttpStatusCode.OK, UserPointsResponse(
                            userId = userId,
                            totalPoints = userPoints.totalPoints,
                            eventCreationPoints = userPoints.eventCreationPoints,
                            votingPoints = userPoints.votingPoints,
                            commentPoints = userPoints.commentPoints,
                            participationPoints = userPoints.participationPoints,
                            level = userLevel?.level ?: 1,
                            levelName = userLevel?.name ?: "Debutant",
                            pointsForNextLevel = userLevel?.pointsForNextLevel ?: 50,
                            progressToNextLevel = userLevel?.progressToNextLevel ?: 0f
                        ))
                    }
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        mapOf("error" to userPointsFetchFailureMessage())
                    )
                }
            }

            // POST /api/users/{id}/points/award - Award points for an action
            post("/award") {
                try {
                    val userId = call.parameters["userId"] ?: return@post call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "User ID required")
                    )
                    val principal = call.principal<JWTPrincipal>() ?: return@post call.respond(
                        HttpStatusCode.Unauthorized,
                        mapOf("error" to "Not authenticated")
                    )
                    if (!principal.canAwardGamificationPoints()) {
                        return@post call.respond(
                            HttpStatusCode.Forbidden,
                            mapOf("error" to "Only administrators can award points directly")
                        )
                    }

                    val request = call.receive<AwardPointsRequest>()

                    val action = try {
                        PointsAction.valueOf(request.action.uppercase())
                    } catch (e: IllegalArgumentException) {
                        return@post call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf("error" to "Invalid action: ${request.action}")
                        )
                    }

                    val result = gamificationService.awardPoints(
                        userId = userId,
                        action = action,
                        eventId = request.eventId
                    )

                    call.respond(
                        HttpStatusCode.OK,
                        AwardPointsResponse(
                            pointsEarned = result.pointsEarned,
                            newTotal = result.newTotal,
                            badgesUnlocked = result.badgesUnlocked,
                            newLevel = UserLevel.fromPoints(result.newTotal)
                        )
                    )
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to pointsAwardFailureMessage())
                    )
                }
            }
        }
    }
}

/**
 * Response model for user points endpoint.
 */
@Serializable
data class UserPointsResponse(
    val userId: String,
    val totalPoints: Int,
    val eventCreationPoints: Int,
    val votingPoints: Int,
    val commentPoints: Int,
    val participationPoints: Int,
    val level: Int,
    val levelName: String,
    val pointsForNextLevel: Int,
    val progressToNextLevel: Float
)

@Serializable
data class UserBadgesResponse(
    val userId: String,
    val badges: List<com.guyghost.wakeve.gamification.Badge>,
    val count: Int
)

/**
 * Request model for awarding points.
 */
@Serializable
data class AwardPointsRequest(
    val action: String,
    val eventId: String? = null
)

@Serializable
data class AwardPointsResponse(
    val pointsEarned: Int,
    val newTotal: Int,
    val badgesUnlocked: List<com.guyghost.wakeve.gamification.Badge>,
    val newLevel: UserLevel
)

private fun JWTPrincipal.canAwardGamificationPoints(): Boolean {
    val role = payload.getClaim("role")?.asString()
    val roles = payload.getClaim("roles")?.asList(String::class.java).orEmpty()
    return (roles + listOfNotNull(role)).any { it.equals("ADMIN", ignoreCase = true) }
}

private fun JWTPrincipal.canReadGamificationProfile(targetUserId: String): Boolean {
    val requesterId = payload.getClaim("userId")?.asString()
    if (requesterId == targetUserId) return true
    return canAwardGamificationPoints()
}

internal fun leaderboardFetchFailureMessage(): String =
    "Failed to fetch leaderboard. Please try again."

internal fun userBadgesFetchFailureMessage(): String =
    "Failed to fetch badges. Please try again."

internal fun userPointsFetchFailureMessage(): String =
    "Failed to fetch points. Please try again."

internal fun pointsAwardFailureMessage(): String =
    "Failed to award points. Please try again."
