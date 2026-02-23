package com.guyghost.wakeve.routes

import com.guyghost.wakeve.gamification.GamificationService
import com.guyghost.wakeve.gamification.LeaderboardType
import com.guyghost.wakeve.gamification.PointsAction
import com.guyghost.wakeve.gamification.UserLevel
import io.ktor.http.HttpStatusCode
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
                val limit = call.parameters["limit"]?.toIntOrNull() ?: 20
                val typeParam = call.parameters["type"] ?: "ALL_TIME"
                val eventId = call.parameters["eventId"]

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
                    mapOf("error" to (e.message ?: "Failed to fetch leaderboard"))
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

                    val badges = gamificationService.getUserBadges(userId)

                    call.respond(HttpStatusCode.OK, mapOf(
                        "userId" to userId,
                        "badges" to badges,
                        "count" to badges.size
                    ))
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        mapOf("error" to (e.message ?: "Failed to fetch badges"))
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

                    val userPoints = gamificationService.getUserPoints(userId)
                    val userLevel = gamificationService.getUserLevel(userId)

                    if (userPoints == null) {
                        call.respond(HttpStatusCode.OK, mapOf(
                            "userId" to userId,
                            "totalPoints" to 0,
                            "level" to UserLevel.fromPoints(0)
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
                        mapOf("error" to (e.message ?: "Failed to fetch points"))
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

                    call.respond(HttpStatusCode.OK, mapOf(
                        "pointsEarned" to result.pointsEarned,
                        "newTotal" to result.newTotal,
                        "badgesUnlocked" to result.badgesUnlocked,
                        "newLevel" to UserLevel.fromPoints(result.newTotal)
                    ))
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to (e.message ?: "Failed to award points"))
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

/**
 * Request model for awarding points.
 */
@Serializable
data class AwardPointsRequest(
    val action: String,
    val eventId: String? = null
)
