package com.guyghost.wakeve.routes

import com.guyghost.wakeve.auth.userId
import com.guyghost.wakeve.database.WakeveDb
import com.guyghost.wakeve.equipment.EquipmentManager
import com.guyghost.wakeve.equipment.EquipmentRepository
import com.guyghost.wakeve.models.AssignEquipmentItemRequest
import com.guyghost.wakeve.models.AutoGenerateEquipmentRequest
import com.guyghost.wakeve.models.CreateEquipmentItemRequest
import com.guyghost.wakeve.models.EquipmentCategory
import com.guyghost.wakeve.models.EquipmentItem
import com.guyghost.wakeve.models.ItemStatus
import com.guyghost.wakeve.models.UpdateEquipmentItemRequest
import com.guyghost.wakeve.models.UpdateEquipmentStatusRequest
import com.guyghost.wakeve.repository.EventRepositoryInterface
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route

/**
 * Equipment API Routes
 *
 * Provides RESTful endpoints for equipment checklist management including:
 * - Equipment item CRUD operations
 * - Category-based organization
 * - Item status tracking (NEEDED, ASSIGNED, CONFIRMED, PACKED, CANCELLED)
 * - Assignment to participants
 * - Auto-generation of checklists based on event type
 * - Cost calculations and statistics
 *
 * Authorization (QA BUG-B, API multi-user QA 2026-09-27):
 * - reads and item creation: event organizer or participants ([hasEventMemberAccess]);
 * - item edit, delete and checklist auto-generation: organizer only (as for meals);
 * - assignment: the organizer assigns any member; a participant may only take an
 *   unassigned item for themselves or release their own item;
 * - status updates: organizer or the current assignee;
 * - an item is only reachable through the event it belongs to.
 * Missing dependencies fail closed (403).
 */
fun io.ktor.server.routing.Route.equipmentRoutes(
    repository: EquipmentRepository,
    eventRepository: EventRepositoryInterface? = null,
    database: WakeveDb? = null,
    manager: EquipmentManager = EquipmentManager
) {
    /** Returns the caller's id if they are a member of [eventId]; null otherwise. */
    fun ApplicationCall.memberId(eventId: String): String? {
        val userId = principal<JWTPrincipal>()?.userId ?: return null
        if (eventRepository == null || database == null) return null
        return userId.takeIf { hasEventMemberAccess(eventRepository, database, eventId, userId) }
    }

    fun isOrganizer(eventId: String, userId: String): Boolean =
        eventRepository != null && isEventOrganizerOf(eventRepository, eventId, userId)

    fun isMember(eventId: String, userId: String): Boolean =
        eventRepository != null && database != null &&
            hasEventMemberAccess(eventRepository, database, eventId, userId)

    fun itemInEvent(eventId: String, itemId: String): EquipmentItem? =
        repository.getEquipmentItemById(itemId)?.takeIf { it.eventId == eventId }

    route("/events/{eventId}/equipment") {

        // GET /api/events/{eventId}/equipment - Get all equipment items for event
        get {
            try {
                val eventId = call.parameters["eventId"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Event ID required")
                )
                call.memberId(eventId) ?: return@get call.respond(
                    HttpStatusCode.Forbidden,
                    mapOf("error" to equipmentAccessDeniedMessage())
                )

                val items = repository.getEquipmentItemsByEventId(eventId)
                call.respond(HttpStatusCode.OK, items)
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to equipmentListFailureMessage())
                )
            }
        }

        // GET /api/events/{eventId}/equipment/category/{category} - Get items by category
        get("/category/{category}") {
            try {
                val eventId = call.parameters["eventId"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Event ID required")
                )
                call.memberId(eventId) ?: return@get call.respond(
                    HttpStatusCode.Forbidden,
                    mapOf("error" to equipmentAccessDeniedMessage())
                )

                val categoryStr = call.parameters["category"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Category required")
                )

                val category = try {
                    EquipmentCategory.valueOf(categoryStr.uppercase())
                } catch (e: IllegalArgumentException) {
                    return@get call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "Invalid category: $categoryStr")
                    )
                }

                val items = repository.getEquipmentItemsByCategory(eventId, category)
                call.respond(HttpStatusCode.OK, items)
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to equipmentCategoryListFailureMessage())
                )
            }
        }

        // GET /api/events/{eventId}/equipment/status/{status} - Get items by status
        get("/status/{status}") {
            try {
                val eventId = call.parameters["eventId"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Event ID required")
                )
                call.memberId(eventId) ?: return@get call.respond(
                    HttpStatusCode.Forbidden,
                    mapOf("error" to equipmentAccessDeniedMessage())
                )

                val statusStr = call.parameters["status"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Status required")
                )

                val status = try {
                    ItemStatus.valueOf(statusStr.uppercase())
                } catch (e: IllegalArgumentException) {
                    return@get call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "Invalid status: $statusStr")
                    )
                }

                val items = repository.getEquipmentItemsByStatus(eventId, status)
                call.respond(HttpStatusCode.OK, items)
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to equipmentStatusListFailureMessage())
                )
            }
        }

        // GET /api/events/{eventId}/equipment/participant/{participantId} - Get items assigned to participant
        get("/participant/{participantId}") {
            try {
                val eventId = call.parameters["eventId"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Event ID required")
                )
                call.memberId(eventId) ?: return@get call.respond(
                    HttpStatusCode.Forbidden,
                    mapOf("error" to equipmentAccessDeniedMessage())
                )

                val participantId = call.parameters["participantId"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Participant ID required")
                )

                val items = repository.getEquipmentItemsByAssignee(eventId, participantId)
                call.respond(HttpStatusCode.OK, items)
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to participantEquipmentListFailureMessage())
                )
            }
        }

        // GET /api/events/{eventId}/equipment/statistics - Get equipment statistics
        get("/statistics") {
            try {
                val eventId = call.parameters["eventId"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Event ID required")
                )
                call.memberId(eventId) ?: return@get call.respond(
                    HttpStatusCode.Forbidden,
                    mapOf("error" to equipmentAccessDeniedMessage())
                )

                val items = repository.getEquipmentItemsByEventId(eventId)
                val stats = manager.calculateEquipmentStats(items)
                call.respond(HttpStatusCode.OK, stats)
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to equipmentStatisticsFailureMessage())
                )
            }
        }

        // POST /api/events/{eventId}/equipment - Create an equipment item
        post {
            try {
                val eventId = call.parameters["eventId"] ?: return@post call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Event ID required")
                )
                val userId = call.memberId(eventId) ?: return@post call.respond(
                    HttpStatusCode.Forbidden,
                    mapOf("error" to equipmentAccessDeniedMessage())
                )

                val request = try {
                    call.receive<CreateEquipmentItemRequest>()
                } catch (e: Exception) {
                    return@post call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to equipmentInvalidPayloadMessage())
                    )
                }

                // Validate request
                if (request.name.isBlank()) {
                    return@post call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "Equipment name is required")
                    )
                }

                if (request.quantity <= 0) {
                    return@post call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "Quantity must be greater than 0")
                    )
                }

                val assignee = request.assignedTo?.trim()?.takeIf { it.isNotEmpty() }
                if (assignee != null) {
                    if (assignee != userId && !isOrganizer(eventId, userId)) {
                        return@post call.respond(
                            HttpStatusCode.Forbidden,
                            mapOf("error" to "Only the organizer can assign items to other participants")
                        )
                    }
                    if (!isMember(eventId, assignee)) {
                        return@post call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf("error" to "Assignee must be a participant of this event")
                        )
                    }
                }

                val item = repository.createEquipmentItem(
                    request.copy(assignedTo = assignee).toEquipmentItem(eventId)
                )
                call.respond(HttpStatusCode.Created, item)
            } catch (e: IllegalArgumentException) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to equipmentInvalidPayloadMessage())
                )
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to equipmentCreateFailureMessage())
                )
            }
        }

        // POST /api/events/{eventId}/equipment/auto-generate - Auto-generate equipment checklist
        post("/auto-generate") {
            try {
                val eventId = call.parameters["eventId"] ?: return@post call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Event ID required")
                )
                val userId = call.memberId(eventId)
                if (userId == null || !isOrganizer(eventId, userId)) {
                    return@post call.respond(
                        HttpStatusCode.Forbidden,
                        mapOf("error" to equipmentAccessDeniedMessage())
                    )
                }

                val request = try {
                    call.receive<AutoGenerateEquipmentRequest>()
                } catch (e: Exception) {
                    return@post call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to equipmentInvalidPayloadMessage())
                    )
                }

                if (request.participantCount <= 0) {
                    return@post call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "Participant count must be greater than 0")
                    )
                }

                val items = manager.autoGenerateChecklist(
                    eventId = eventId,
                    eventType = request.eventType,
                    participantCount = request.participantCount
                )

                // Save all generated items
                val savedItems = items.map { item ->
                    repository.createEquipmentItem(item)
                }

                call.respond(HttpStatusCode.Created, savedItems)
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to equipmentAutoGenerateFailureMessage())
                )
            }
        }

        // PUT /api/events/{eventId}/equipment/{itemId} - Update an equipment item (organizer only)
        put("/{itemId}") {
            try {
                val eventId = call.parameters["eventId"] ?: return@put call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Event ID required")
                )
                val userId = call.memberId(eventId)
                if (userId == null || !isOrganizer(eventId, userId)) {
                    return@put call.respond(
                        HttpStatusCode.Forbidden,
                        mapOf("error" to equipmentAccessDeniedMessage())
                    )
                }

                val itemId = call.parameters["itemId"] ?: return@put call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Item ID required")
                )

                val request = try {
                    call.receive<UpdateEquipmentItemRequest>()
                } catch (e: Exception) {
                    return@put call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to equipmentInvalidPayloadMessage())
                    )
                }
                val existingItem = itemInEvent(eventId, itemId) ?: return@put call.respond(
                    HttpStatusCode.NotFound,
                    mapOf("error" to "Equipment item not found")
                )

                // Validate request
                if (request.name != null && request.name.isBlank()) {
                    return@put call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "Equipment name cannot be empty")
                    )
                }

                if (request.quantity != null && request.quantity <= 0) {
                    return@put call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "Quantity must be greater than 0")
                    )
                }

                if (request.assignedTo != null && !isMember(eventId, request.assignedTo)) {
                    return@put call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "Assignee must be a participant of this event")
                    )
                }

                val updatedItem = repository.updateEquipmentItem(request.applyTo(existingItem))
                call.respond(HttpStatusCode.OK, updatedItem)
            } catch (e: IllegalArgumentException) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to equipmentInvalidPayloadMessage())
                )
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to equipmentUpdateFailureMessage())
                )
            }
        }

        // PUT /api/events/{eventId}/equipment/{itemId}/assign - Assign item to participant
        put("/{itemId}/assign") {
            try {
                val eventId = call.parameters["eventId"] ?: return@put call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Event ID required")
                )
                val userId = call.memberId(eventId) ?: return@put call.respond(
                    HttpStatusCode.Forbidden,
                    mapOf("error" to equipmentAccessDeniedMessage())
                )

                val itemId = call.parameters["itemId"] ?: return@put call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Item ID required")
                )

                val request = try {
                    call.receive<AssignEquipmentItemRequest>()
                } catch (e: Exception) {
                    return@put call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to equipmentInvalidPayloadMessage())
                    )
                }

                val item = itemInEvent(eventId, itemId) ?: return@put call.respond(
                    HttpStatusCode.NotFound,
                    mapOf("error" to "Equipment item not found")
                )

                val newAssignee = request.participantId?.trim()?.takeIf { it.isNotEmpty() }
                val allowed = isOrganizer(eventId, userId) || when (newAssignee) {
                    // Release: only your own item.
                    null -> item.assignedTo == userId
                    // Take: only for yourself, and only if nobody else already brings it.
                    else -> newAssignee == userId && (item.assignedTo == null || item.assignedTo == userId)
                }
                if (!allowed) {
                    return@put call.respond(
                        HttpStatusCode.Forbidden,
                        mapOf("error" to "You cannot change who brings this item")
                    )
                }
                if (newAssignee != null && !isMember(eventId, newAssignee)) {
                    return@put call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "Assignee must be a participant of this event")
                    )
                }

                val updatedItem = item.copy(
                    assignedTo = newAssignee,
                    status = if (newAssignee != null) ItemStatus.ASSIGNED else ItemStatus.NEEDED
                )

                repository.updateEquipmentItem(updatedItem)
                call.respond(HttpStatusCode.OK, updatedItem)
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to equipmentAssignFailureMessage())
                )
            }
        }

        // PUT /api/events/{eventId}/equipment/{itemId}/status - Update item status
        put("/{itemId}/status") {
            try {
                val eventId = call.parameters["eventId"] ?: return@put call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Event ID required")
                )
                val userId = call.memberId(eventId) ?: return@put call.respond(
                    HttpStatusCode.Forbidden,
                    mapOf("error" to equipmentAccessDeniedMessage())
                )

                val itemId = call.parameters["itemId"] ?: return@put call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Item ID required")
                )

                val request = try {
                    call.receive<UpdateEquipmentStatusRequest>()
                } catch (e: Exception) {
                    return@put call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to equipmentInvalidPayloadMessage())
                    )
                }

                val item = itemInEvent(eventId, itemId) ?: return@put call.respond(
                    HttpStatusCode.NotFound,
                    mapOf("error" to "Equipment item not found")
                )

                if (!isOrganizer(eventId, userId) && item.assignedTo != userId) {
                    return@put call.respond(
                        HttpStatusCode.Forbidden,
                        mapOf("error" to "Only the organizer or the assignee can update this item's status")
                    )
                }

                // Validate status transition
                if (!manager.isValidStatusTransition(item.status, request.newStatus)) {
                    return@put call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "Invalid status transition from ${item.status} to ${request.newStatus}")
                    )
                }

                val updatedItem = item.copy(status = request.newStatus)
                repository.updateEquipmentItem(updatedItem)
                call.respond(HttpStatusCode.OK, updatedItem)
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to equipmentStatusUpdateFailureMessage())
                )
            }
        }

        // DELETE /api/events/{eventId}/equipment/{itemId} - Delete an equipment item (organizer only)
        delete("/{itemId}") {
            try {
                val eventId = call.parameters["eventId"] ?: return@delete call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Event ID required")
                )
                val userId = call.memberId(eventId)
                if (userId == null || !isOrganizer(eventId, userId)) {
                    return@delete call.respond(
                        HttpStatusCode.Forbidden,
                        mapOf("error" to equipmentAccessDeniedMessage())
                    )
                }

                val itemId = call.parameters["itemId"] ?: return@delete call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Item ID required")
                )

                itemInEvent(eventId, itemId) ?: return@delete call.respond(
                    HttpStatusCode.NotFound,
                    mapOf("error" to "Equipment item not found")
                )

                repository.deleteEquipmentItem(itemId)
                call.respond(HttpStatusCode.NoContent)
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to equipmentDeleteFailureMessage())
                )
            }
        }
    }
}

internal fun equipmentAccessDeniedMessage(): String =
    "You do not have access to this event"

internal fun equipmentInvalidPayloadMessage(): String =
    "Invalid equipment request. Please check the fields and try again."

internal fun equipmentListFailureMessage(): String =
    "Failed to fetch equipment items. Please try again."

internal fun equipmentCategoryListFailureMessage(): String =
    "Failed to fetch equipment items for this category. Please try again."

internal fun equipmentStatusListFailureMessage(): String =
    "Failed to fetch equipment items for this status. Please try again."

internal fun participantEquipmentListFailureMessage(): String =
    "Failed to fetch participant equipment items. Please try again."

internal fun equipmentStatisticsFailureMessage(): String =
    "Failed to fetch equipment statistics. Please try again."

internal fun equipmentCreateFailureMessage(): String =
    "Failed to create the equipment item. Please try again."

internal fun equipmentAutoGenerateFailureMessage(): String =
    "Failed to generate the equipment checklist. Please try again."

internal fun equipmentUpdateFailureMessage(): String =
    "Failed to update the equipment item. Please try again."

internal fun equipmentAssignFailureMessage(): String =
    "Failed to assign the equipment item. Please try again."

internal fun equipmentStatusUpdateFailureMessage(): String =
    "Failed to update equipment status. Please try again."

internal fun equipmentDeleteFailureMessage(): String =
    "Failed to delete the equipment item. Please try again."
