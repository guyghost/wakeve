package com.guyghost.wakeve.routes

import com.guyghost.wakeve.equipment.EquipmentManager
import com.guyghost.wakeve.equipment.EquipmentRepository
import com.guyghost.wakeve.models.*
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.*

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
 */
fun io.ktor.server.routing.Route.equipmentRoutes(
    repository: EquipmentRepository,
    manager: EquipmentManager = EquipmentManager()
) {
    route("/events/{eventId}/equipment") {
        
        // GET /api/events/{eventId}/equipment - Get all equipment items for event
        get {
            try {
                val eventId = call.parameters["eventId"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Event ID required")
                )

                val items = repository.getEquipmentByEventId(eventId)
                call.respond(HttpStatusCode.OK, items)
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to e.message.orEmpty())
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

                val items = repository.getEquipmentByCategory(eventId, category)
                call.respond(HttpStatusCode.OK, items)
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to e.message.orEmpty())
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

                val items = repository.getEquipmentByStatus(eventId, status)
                call.respond(HttpStatusCode.OK, items)
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to e.message.orEmpty())
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
                
                val participantId = call.parameters["participantId"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Participant ID required")
                )

                val items = repository.getEquipmentByAssignedTo(eventId, participantId)
                call.respond(HttpStatusCode.OK, items)
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to e.message.orEmpty())
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

                val items = repository.getEquipmentByEventId(eventId)
                val stats = manager.calculateEquipmentStats(items)
                call.respond(HttpStatusCode.OK, stats)
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to e.message.orEmpty())
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

                val request = call.receive<CreateEquipmentItemRequest>()
                
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

                val item = repository.createEquipmentItem(request.toEquipmentItem(eventId))
                call.respond(HttpStatusCode.Created, item)
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to e.message.orEmpty())
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

                val request = call.receive<AutoGenerateEquipmentRequest>()
                
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
                    mapOf("error" to e.message.orEmpty())
                )
            }
        }
        
        // PUT /api/events/{eventId}/equipment/{itemId} - Update an equipment item
        put("/{itemId}") {
            try {
                val eventId = call.parameters["eventId"] ?: return@put call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Event ID required")
                )
                
                val itemId = call.parameters["itemId"] ?: return@put call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Item ID required")
                )

                val request = call.receive<UpdateEquipmentItemRequest>()
                
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

                val updatedItem = repository.updateEquipmentItem(request.applyTo(itemId, eventId))
                call.respond(HttpStatusCode.OK, updatedItem)
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to e.message.orEmpty())
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
                
                val itemId = call.parameters["itemId"] ?: return@put call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Item ID required")
                )

                val request = call.receive<AssignEquipmentItemRequest>()
                
                val item = repository.getEquipmentById(itemId)
                if (item == null) {
                    return@put call.respond(
                        HttpStatusCode.NotFound,
                        mapOf("error" to "Equipment item not found")
                    )
                }
                
                val updatedItem = item.copy(
                    assignedTo = request.participantId,
                    status = if (request.participantId != null) ItemStatus.ASSIGNED else ItemStatus.NEEDED
                )
                
                repository.updateEquipmentItem(updatedItem)
                call.respond(HttpStatusCode.OK, updatedItem)
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to e.message.orEmpty())
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
                
                val itemId = call.parameters["itemId"] ?: return@put call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Item ID required")
                )

                val request = call.receive<UpdateEquipmentStatusRequest>()
                
                val item = repository.getEquipmentById(itemId)
                if (item == null) {
                    return@put call.respond(
                        HttpStatusCode.NotFound,
                        mapOf("error" to "Equipment item not found")
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
                    mapOf("error" to e.message.orEmpty())
                )
            }
        }
        
        // DELETE /api/events/{eventId}/equipment/{itemId} - Delete an equipment item
        delete("/{itemId}") {
            try {
                val eventId = call.parameters["eventId"] ?: return@delete call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Event ID required")
                )
                
                val itemId = call.parameters["itemId"] ?: return@delete call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Item ID required")
                )

                repository.deleteEquipmentItem(itemId)
                call.respond(HttpStatusCode.NoContent)
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to e.message.orEmpty())
                )
            }
        }
    }
}
