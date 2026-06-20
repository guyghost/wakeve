package com.guyghost.wakeve.routes

import com.guyghost.wakeve.database.WakeveDb
import com.guyghost.wakeve.models.ChatMessageType
import com.guyghost.wakeve.models.ChatWebSocketMessage
import com.guyghost.wakeve.models.ChatWebSocketResponse
import com.guyghost.wakeve.models.MessageData
import com.guyghost.wakeve.moderation.ModerationRepository
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.routing.Route
import io.ktor.server.websocket.DefaultWebSocketServerSession
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.CloseReason
import io.ktor.websocket.readText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap

private val json = Json { ignoreUnknownKeys = true }

/**
 * Gestionnaire de connexions WebSocket par événement.
 *
 * Gère les connexions multiples pour chaque événement et permet
 * la diffusion de messages à tous les participants d'un événement.
 */
class EventChatConnections {
    private data class EventChatConnection(
        val userId: String,
        val session: DefaultWebSocketServerSession
    )

    private val connections = ConcurrentHashMap<String, ConcurrentHashMap<String, EventChatConnection>>()

    /**
     * Ajoute une connexion pour un événement.
     */
    fun addConnection(eventId: String, userId: String, session: DefaultWebSocketServerSession): String {
        val connectionId = "$userId-${System.nanoTime()}"
        connections
            .computeIfAbsent(eventId) { ConcurrentHashMap() }[connectionId] = EventChatConnection(userId, session)
        return connectionId
    }

    /**
     * Supprime une connexion d'un événement.
     */
    fun removeConnection(eventId: String, connectionId: String) {
        connections[eventId]?.let { eventConnections ->
            eventConnections.remove(connectionId)
            if (eventConnections.isEmpty()) {
                connections.remove(eventId, eventConnections)
            }
        }
    }

    /**
     * Diffuse un message à la connexion d'un événement.
     */
    fun broadcast(
        eventId: String,
        message: ChatWebSocketResponse,
        moderationRepository: ModerationRepository? = null
    ) {
        val eventConnections = connections[eventId] ?: return
        CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            val senderId = message.data.userId
            val jsonText = json.encodeToString(message)
            eventConnections.forEach { (_, connection) ->
                if (senderId.isNotBlank() &&
                    senderId != connection.userId &&
                    moderationRepository?.isBlockedForEvent(connection.userId, senderId, eventId) == true
                ) {
                    return@forEach
                }

                try {
                    connection.session.send(Frame.Text(jsonText))
                } catch (e: Exception) {
                    // La connexion est probablement fermée, on ignore l'envoi.
                }
            }
        }
    }

    /**
     * Retourne le nombre de connexions pour un événement.
     */
    fun getConnectionCount(eventId: String): Int {
        return connections[eventId]?.size ?: 0
    }
}

// Instance partagée du gestionnaire de connexions
val eventChatConnections = EventChatConnections()

/**
 * Route WebSocket pour le chat en temps réel.
 *
 * Endpoint: /ws/events/{eventId}/chat
 *
 * Gère les connexions multiples par événement et diffuse les messages
 * à tous les participants connectés.
 */
fun Route.chatWebSocketRoute(
    database: WakeveDb,
    moderationRepository: ModerationRepository? = null
) {
    val connectionManager = eventChatConnections

    webSocket("/ws/events/{eventId}/chat") {
        val eventId: String = call.parameters["eventId"] ?: run {
            close()
            return@webSocket
        }
        val userId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asString()
            ?: run {
                close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Authentication required"))
                return@webSocket
            }
        if (!hasChatWebSocketAccess(database, eventId, userId)) {
            close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Event access required"))
            return@webSocket
        }

        // Ajouter la connexion au gestionnaire
        val connectionId = connectionManager.addConnection(eventId, userId, this)

        try {
            // Boucle de réception des messages
            for (frame in incoming) {
                when (frame) {
                    is Frame.Text -> {
                        val text = frame.readText()
                        try {
                            // Désérialiser le message entrant
                            val chatMessage = json.decodeFromString<ChatWebSocketMessage>(text)

                            // Traiter le message selon son type
                            when (chatMessage.type) {
                                ChatMessageType.MESSAGE -> {
                                    // Créer une réponse de type MESSAGE
                                    val response = ChatWebSocketResponse(
                                        type = ChatMessageType.MESSAGE,
                                        data = chatMessage.data.copy(
                                            messageId = chatMessage.data.messageId ?: "msg_${System.currentTimeMillis()}"
                                        )
                                    )
                                    // Diffuser à tous les participants de l'événement
                                    connectionManager.broadcast(eventId, response, moderationRepository)
                                }

                                ChatMessageType.TYPING -> {
                                    // Diffuser l'indicateur de frappe
                                    val response = ChatWebSocketResponse(
                                        type = ChatMessageType.TYPING,
                                        data = chatMessage.data
                                    )
                                    connectionManager.broadcast(eventId, response, moderationRepository)
                                }

                                ChatMessageType.REACTION -> {
                                    // Diffuser la réaction
                                    val response = ChatWebSocketResponse(
                                        type = ChatMessageType.REACTION,
                                        data = chatMessage.data
                                    )
                                    connectionManager.broadcast(eventId, response, moderationRepository)
                                }

                                ChatMessageType.READ_RECEIPT -> {
                                    // Diffuser le reçu de lecture
                                    val response = ChatWebSocketResponse(
                                        type = ChatMessageType.READ_RECEIPT,
                                        data = chatMessage.data
                                    )
                                    connectionManager.broadcast(eventId, response, moderationRepository)
                                }
                            }
                        } catch (e: Exception) {
                            // Envoyer un message d'erreur au client
                            val errorResponse = ChatWebSocketResponse(
                                type = ChatMessageType.MESSAGE,
                                data = MessageData(
                                    eventId = eventId,
                                    userId = "",
                                    userName = "",
                                    content = null
                                ),
                                success = false,
                                errorMessage = chatWebSocketInvalidMessageFailureMessage()
                            )
                            val errorJson = json.encodeToString(errorResponse)
                            send(Frame.Text(errorJson))
                        }
                    }
                    else -> {
                        // Ignorer les autres types de frames
                    }
                }
            }
        } finally {
            // Supprimer la connexion lors de la déconnexion
            connectionManager.removeConnection(eventId, connectionId)
        }
    }
}

internal fun hasChatWebSocketAccess(database: WakeveDb, eventId: String, userId: String): Boolean {
    val event = database.eventQueries.selectById(eventId).executeAsOneOrNull() ?: return false
    if (event.organizerId == userId) return true
    return database.participantQueries
        .selectByEventIdAndUserId(eventId, userId)
        .executeAsOneOrNull() != null
}

internal fun chatWebSocketInvalidMessageFailureMessage(): String =
    "Invalid chat message format."
