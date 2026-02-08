package com.guyghost.wakeve.routes

import com.guyghost.wakeve.auth.userId
import com.guyghost.wakeve.budget.BudgetCalculator
import com.guyghost.wakeve.budget.BudgetRepository
import com.guyghost.wakeve.models.Budget
import com.guyghost.wakeve.models.BudgetCategory
import com.guyghost.wakeve.models.BudgetItem
import com.guyghost.wakeve.EventRepositoryInterface
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.principal
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import io.ktor.server.auth.authenticate
import kotlinx.serialization.Serializable

/**
 * Budget API Routes
 *
 * Provides RESTful endpoints for budget management including:
 * - Budget CRUD operations
 * - Budget item management
 * - Category filtering
 * - Payment status tracking
 * - Settlement suggestions
 *
 * **SECURITY**: All routes require JWT authentication via "auth-jwt"
 */
fun io.ktor.server.routing.Route.budgetRoutes(repository: BudgetRepository, eventRepository: EventRepositoryInterface) {
    authenticate("auth-jwt") {
        route("/events/{eventId}/budget") {
        
        // GET /api/events/{eventId}/budget - Get budget for event
        get {
            try {
                val principal = call.principal<JWTPrincipal>()
                    ?: return@get call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Not authenticated"))

                val eventId = call.parameters["eventId"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Event ID required")
                )

                val userId = principal.userId

                // Check if user has access to the event
                if (!hasEventAccess(eventRepository, eventId, userId)) {
                    return@get call.respond(
                        HttpStatusCode.Forbidden,
                        mapOf("error" to "You do not have access to this event")
                    )
                }

                val budget = repository.getBudgetByEventId(eventId)
                if (budget != null) {
                    call.respond(HttpStatusCode.OK, budget)
                } else {
                    call.respond(
                        HttpStatusCode.NotFound,
                        mapOf("error" to "Budget not found for event")
                    )
                }
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to e.message.orEmpty())
                )
            }
        }

        // PUT /api/events/{eventId}/budget - Update or create budget
        put {
            try {
                val principal = call.principal<JWTPrincipal>()
                    ?: return@put call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Not authenticated"))

                val eventId = call.parameters["eventId"] ?: return@put call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Event ID required")
                )

                val userId = principal.userId

                // Check if user has access to the event
                if (!hasEventAccess(eventRepository, eventId, userId)) {
                    return@put call.respond(
                        HttpStatusCode.Forbidden,
                        mapOf("error" to "You do not have access to this event")
                    )
                }

                val budget = call.receive<Budget>()

                // Ensure eventId matches
                if (budget.eventId != eventId) {
                    return@put call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "Event ID mismatch")
                    )
                }

                // Check if budget exists
                val existing = repository.getBudgetByEventId(eventId)

                val savedBudget = if (existing != null) {
                    // Update existing budget
                    val updated = budget.copy(
                        id = existing.id,
                        createdAt = existing.createdAt,
                        updatedAt = getCurrentIsoTimestamp()
                    )
                    repository.updateBudget(updated)
                } else {
                    // Create new budget
                    repository.createBudget(eventId)
                }

                call.respond(HttpStatusCode.OK, savedBudget)
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to e.message.orEmpty())
                )
            }
        }

        // GET /api/events/{eventId}/budget/items - Get all budget items (with optional filters)
        get("/items") {
            try {
                val principal = call.principal<JWTPrincipal>()
                    ?: return@get call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Not authenticated"))

                val eventId = call.parameters["eventId"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Event ID required")
                )

                val userId = principal.userId

                // Check if user has access to the event
                if (!hasEventAccess(eventRepository, eventId, userId)) {
                    return@get call.respond(
                        HttpStatusCode.Forbidden,
                        mapOf("error" to "You do not have access to this event")
                    )
                }

                // Get budget for event
                val budget = repository.getBudgetByEventId(eventId) ?: return@get call.respond(
                    HttpStatusCode.NotFound,
                    mapOf("error" to "Budget not found for event")
                )

                // Parse query parameters
                val categoryParam = call.request.queryParameters["category"]
                val paidParam = call.request.queryParameters["paid"]
                val participantId = call.request.queryParameters["participantId"]

                // Get items with filters
                val items = when {
                    categoryParam != null -> {
                        val category = try {
                            BudgetCategory.valueOf(categoryParam.uppercase())
                        } catch (e: IllegalArgumentException) {
                            return@get call.respond(
                                HttpStatusCode.BadRequest,
                                mapOf("error" to "Invalid category: $categoryParam")
                            )
                        }
                        repository.getBudgetItemsByCategory(budget.id, category)
                    }
                    paidParam != null -> {
                        val isPaid = paidParam.toBoolean()
                        if (isPaid) {
                            repository.getPaidItems(budget.id)
                        } else {
                            repository.getUnpaidItems(budget.id)
                        }
                    }
                    participantId != null -> {
                        repository.getItemsSharedByParticipant(budget.id, participantId)
                    }
                    else -> {
                        repository.getBudgetItems(budget.id)
                    }
                }

                call.respond(HttpStatusCode.OK, mapOf(
                    "items" to items,
                    "count" to items.size
                ))
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to e.message.orEmpty())
                )
            }
        }

        // POST /api/events/{eventId}/budget/items - Add new budget item
        post("/items") {
            try {
                val principal = call.principal<JWTPrincipal>()
                    ?: return@post call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Not authenticated"))

                val eventId = call.parameters["eventId"] ?: return@post call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Event ID required")
                )

                val userId = principal.userId

                // Check if user has access to the event
                if (!hasEventAccess(eventRepository, eventId, userId)) {
                    return@post call.respond(
                        HttpStatusCode.Forbidden,
                        mapOf("error" to "You do not have access to this event")
                    )
                }

                // Get budget for event
                val budget = repository.getBudgetByEventId(eventId) ?: return@post call.respond(
                    HttpStatusCode.NotFound,
                    mapOf("error" to "Budget not found for event")
                )

                @Serializable
                data class CreateItemRequest(
                    val name: String,
                    val description: String = "",
                    val category: String,
                    val estimatedCost: Double,
                    val sharedBy: List<String> = emptyList()
                )

                val request = call.receive<CreateItemRequest>()

                // Parse category
                val category = try {
                    BudgetCategory.valueOf(request.category.uppercase())
                } catch (e: IllegalArgumentException) {
                    return@post call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "Invalid category: ${request.category}")
                    )
                }

                // Validate estimated cost
                if (request.estimatedCost <= 0) {
                    return@post call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "Estimated cost must be greater than 0")
                    )
                }

                val item = repository.createBudgetItem(
                    budgetId = budget.id,
                    name = request.name,
                    description = request.description,
                    category = category,
                    estimatedCost = request.estimatedCost,
                    sharedBy = request.sharedBy.ifEmpty { listOf(userId) }
                )

                call.respond(HttpStatusCode.Created, item)
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to e.message.orEmpty())
                )
            }
        }

        // GET /api/events/{eventId}/budget/items/{itemId} - Get specific item
        get("/items/{itemId}") {
            try {
                val principal = call.principal<JWTPrincipal>()
                    ?: return@get call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Not authenticated"))

                val eventId = call.parameters["eventId"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Event ID required")
                )

                val userId = principal.userId

                // Check if user has access to the event
                if (!hasEventAccess(eventRepository, eventId, userId)) {
                    return@get call.respond(
                        HttpStatusCode.Forbidden,
                        mapOf("error" to "You do not have access to this event")
                    )
                }

                val itemId = call.parameters["itemId"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Item ID required")
                )

                // Verify budget exists
                val budget = repository.getBudgetByEventId(eventId) ?: return@get call.respond(
                    HttpStatusCode.NotFound,
                    mapOf("error" to "Budget not found for event")
                )

                val item = repository.getBudgetItemById(itemId)
                if (item != null && item.budgetId == budget.id) {
                    call.respond(HttpStatusCode.OK, item)
                } else {
                    call.respond(
                        HttpStatusCode.NotFound,
                        mapOf("error" to "Item not found")
                    )
                }
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to e.message.orEmpty())
                )
            }
        }

        // PUT /api/events/{eventId}/budget/items/{itemId} - Update budget item
        put("/items/{itemId}") {
            try {
                val principal = call.principal<JWTPrincipal>()
                    ?: return@put call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Not authenticated"))

                val eventId = call.parameters["eventId"] ?: return@put call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Event ID required")
                )

                val userId = principal.userId

                // Check if user has access to the event
                if (!hasEventAccess(eventRepository, eventId, userId)) {
                    return@put call.respond(
                        HttpStatusCode.Forbidden,
                        mapOf("error" to "You do not have access to this event")
                    )
                }

                val itemId = call.parameters["itemId"] ?: return@put call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Item ID required")
                )

                // Verify budget exists
                val budget = repository.getBudgetByEventId(eventId) ?: return@put call.respond(
                    HttpStatusCode.NotFound,
                    mapOf("error" to "Budget not found for event")
                )

                val existing = repository.getBudgetItemById(itemId) ?: return@put call.respond(
                    HttpStatusCode.NotFound,
                    mapOf("error" to "Item not found")
                )

                // Verify item belongs to this budget
                if (existing.budgetId != budget.id) {
                    return@put call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "Item does not belong to this budget")
                    )
                }

                val item = call.receive<BudgetItem>()

                // Validate item
                val validation = BudgetCalculator.validateBudgetItem(item)
                if (validation.isNotEmpty()) {
                    return@put call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("errors" to validation)
                    )
                }

                val updated = item.copy(
                    id = itemId,
                    budgetId = budget.id,
                    createdAt = existing.createdAt,
                    updatedAt = getCurrentIsoTimestamp()
                )

                val savedItem = repository.updateBudgetItem(updated)
                call.respond(HttpStatusCode.OK, savedItem)
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to e.message.orEmpty())
                )
            }
        }

        // DELETE /api/events/{eventId}/budget/items/{itemId} - Delete budget item
        delete("/items/{itemId}") {
            try {
                val principal = call.principal<JWTPrincipal>()
                    ?: return@delete call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Not authenticated"))

                val eventId = call.parameters["eventId"] ?: return@delete call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Event ID required")
                )

                val userId = principal.userId

                // Check if user has access to the event
                if (!hasEventAccess(eventRepository, eventId, userId)) {
                    return@delete call.respond(
                        HttpStatusCode.Forbidden,
                        mapOf("error" to "You do not have access to this event")
                    )
                }

                val itemId = call.parameters["itemId"] ?: return@delete call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Item ID required")
                )

                // Verify budget exists
                val budget = repository.getBudgetByEventId(eventId) ?: return@delete call.respond(
                    HttpStatusCode.NotFound,
                    mapOf("error" to "Budget not found for event")
                )

                val existing = repository.getBudgetItemById(itemId) ?: return@delete call.respond(
                    HttpStatusCode.NotFound,
                    mapOf("error" to "Item not found")
                )

                // Verify item belongs to this budget
                if (existing.budgetId != budget.id) {
                    return@delete call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "Item does not belong to this budget")
                    )
                }

                repository.deleteBudgetItem(itemId)
                call.respond(
                    HttpStatusCode.OK,
                    mapOf("message" to "Item deleted successfully")
                )
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to e.message.orEmpty())
                )
            }
        }

        // GET /api/events/{eventId}/budget/summary - Get budget summary with statistics
        get("/summary") {
            try {
                val principal = call.principal<JWTPrincipal>()
                    ?: return@get call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Not authenticated"))

                val eventId = call.parameters["eventId"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Event ID required")
                )

                val userId = principal.userId

                // Check if user has access to the event
                if (!hasEventAccess(eventRepository, eventId, userId)) {
                    return@get call.respond(
                        HttpStatusCode.Forbidden,
                        mapOf("error" to "You do not have access to this event")
                    )
                }

                val budget = repository.getBudgetByEventId(eventId) ?: return@get call.respond(
                    HttpStatusCode.NotFound,
                    mapOf("error" to "Budget not found for event")
                )

                // Get event to retrieve participant count
                val event = eventRepository.getEvent(eventId)
                if (event == null) {
                    return@get call.respond(
                        HttpStatusCode.NotFound,
                        mapOf("error" to "Event not found")
                    )
                }

                // Use actual participant count (prefer expectedParticipants if set, otherwise use participants list size)
                val participantCount = event.expectedParticipants ?: event.participants.size

                val items = repository.getBudgetItems(budget.id)
                val summary = BudgetCalculator.generateBudgetSummary(
                    budget = budget,
                    items = items,
                    participantCount = participantCount
                )

                call.respond(HttpStatusCode.OK, mapOf(
                    "budget" to budget,
                    "summary" to summary,
                    "itemCount" to items.size
                ))
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to e.message.orEmpty())
                )
            }
        }

        // GET /api/events/{eventId}/budget/settlements - Get settlement suggestions
        get("/settlements") {
            try {
                val principal = call.principal<JWTPrincipal>()
                    ?: return@get call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Not authenticated"))

                val eventId = call.parameters["eventId"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Event ID required")
                )

                val userId = principal.userId

                // Check if user has access to the event
                if (!hasEventAccess(eventRepository, eventId, userId)) {
                    return@get call.respond(
                        HttpStatusCode.Forbidden,
                        mapOf("error" to "You do not have access to this event")
                    )
                }

                val budget = repository.getBudgetByEventId(eventId) ?: return@get call.respond(
                    HttpStatusCode.NotFound,
                    mapOf("error" to "Budget not found for event")
                )

                val settlements = repository.getSettlements(budget.id)

                call.respond(HttpStatusCode.OK, mapOf(
                    "settlements" to settlements.map { (from, to, amount) ->
                        mapOf(
                            "from" to from,
                            "to" to to,
                            "amount" to amount
                        )
                    },
                    "count" to settlements.size
                ))
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to e.message.orEmpty())
                )
            }
        }

        // GET /api/events/{eventId}/budget/participants/{participantId} - Get participant's budget info
        get("/participants/{participantId}") {
            try {
                val principal = call.principal<JWTPrincipal>()
                    ?: return@get call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Not authenticated"))

                val eventId = call.parameters["eventId"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Event ID required")
                )

                val userId = principal.userId

                // Check if user has access to the event
                if (!hasEventAccess(eventRepository, eventId, userId)) {
                    return@get call.respond(
                        HttpStatusCode.Forbidden,
                        mapOf("error" to "You do not have access to this event")
                    )
                }

                val participantId = call.parameters["participantId"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Participant ID required")
                )

                val budget = repository.getBudgetByEventId(eventId) ?: return@get call.respond(
                    HttpStatusCode.NotFound,
                    mapOf("error" to "Budget not found for event")
                )

                val share = repository.getParticipantBudgetShare(budget.id, participantId)
                val balances = repository.getParticipantBalances(budget.id)
                val participantBalance = balances[participantId] ?: 0.0

                call.respond(HttpStatusCode.OK, mapOf(
                    "participantId" to participantId,
                    "share" to share,
                    "balance" to participantBalance,
                    "balanceDescription" to when {
                        participantBalance > 0 -> "Owed by others"
                        participantBalance < 0 -> "Owes to others"
                        else -> "Settled up"
                    }
                ))
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to e.message.orEmpty())
                )
            }
        }

        // GET /api/events/{eventId}/budget/statistics - Get budget statistics
        get("/statistics") {
            try {
                val principal = call.principal<JWTPrincipal>()
                    ?: return@get call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Not authenticated"))

                val eventId = call.parameters["eventId"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Event ID required")
                )

                val userId = principal.userId

                // Check if user has access to the event
                if (!hasEventAccess(eventRepository, eventId, userId)) {
                    return@get call.respond(
                        HttpStatusCode.Forbidden,
                        mapOf("error" to "You do not have access to this event")
                    )
                }

                val budget = repository.getBudgetByEventId(eventId) ?: return@get call.respond(
                    HttpStatusCode.NotFound,
                    mapOf("error" to "Budget not found for event")
                )

                val totalItems = repository.countItems(budget.id)
                val paidItems = repository.countPaidItems(budget.id)
                val unpaidItems = totalItems - paidItems

                val categoryStats = BudgetCategory.values().map { category ->
                    val categoryItems = repository.getBudgetItemsByCategory(budget.id, category)
                    mapOf(
                        "category" to category.name,
                        "count" to categoryItems.size,
                        "estimatedTotal" to categoryItems.sumOf { it.estimatedCost },
                        "actualTotal" to categoryItems.sumOf { it.actualCost }
                    )
                }.filter { (it["count"] as Int) > 0 }

                call.respond(HttpStatusCode.OK, mapOf(
                    "totalItems" to totalItems,
                    "paidItems" to paidItems,
                    "unpaidItems" to unpaidItems,
                    "categoryStatistics" to categoryStats
                ))
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to e.message.orEmpty())
                )
            }
        }
    }
    } // End authenticate("auth-jwt")
}

/**
 * Helper function to get current ISO 8601 timestamp
 */
private fun getCurrentIsoTimestamp(): String {
    return java.time.Instant.now().toString()
}

/**
 * Check if the authenticated user has access to the event.
 * User has access if they are the organizer or a participant.
 */
private fun hasEventAccess(
    eventRepository: EventRepositoryInterface,
    eventId: String,
    userId: String
): Boolean {
    val event = eventRepository.getEvent(eventId) ?: return false
    return event.organizerId == userId || event.participants.contains(userId)
}
