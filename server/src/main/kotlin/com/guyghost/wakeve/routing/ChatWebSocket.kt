package com.guyghost.wakeve.routes

import com.guyghost.wakeve.models.ChatMessageType
import com.guyghost.wakeve.models.ChatWebSocketMessage
import com.guyghost.wakeve.models.ChatWebSocketResponse
import com.guyghost.wakeve.models.MessageData
import io.ktor.server.routing.Route
import io.ktor.server.websocket.DefaultWebSocketServerSession
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.close
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
    private val connections = ConcurrentHashMap<String, DefaultWebSocketServerSession>()

    /**
     * Ajoute une connexion pour un événement.
     */
    fun addConnection(eventId: String, session: DefaultWebSocketServerSession) {
        connections[eventId] = session
    }

    /**
     * Supprime une connexion d'un événement.
     */
    fun removeConnection(eventId: String) {
        connections.remove(eventId)
    }

    /**
     * Diffuse un message à la connexion d'un événement.
     */
    fun broadcast(eventId: String, message: ChatWebSocketResponse) {
        connections[eventId]?.let { session ->
            CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
                try {
                    val jsonText = json.encodeToString(message)
                    session.send(Frame.Text(jsonText))
                } catch (e: Exception) {
                    // La connexion est probablement fermée, on ignore
                }
            }
        }
    }

    /**
     * Retourne le nombre de connexions pour un événement.
     */
    fun getConnectionCount(eventId: String): Int {
        return if (connections.containsKey(eventId)) 1 else 0
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
fun Route.chatWebSocketRoute() {
    val connectionManager = eventChatConnections

    webSocket("/ws/events/{eventId}/chat") {
        val eventId: String = call.parameters["eventId"] ?: run {
            close()
            return@webSocket
        }

        // Ajouter la connexion au gestionnaire
        connectionManager.addConnection(eventId, this)

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
                                    connectionManager.broadcast(eventId, response)
                                }

                                ChatMessageType.TYPING -> {
                                    // Diffuser l'indicateur de frappe
                                    val response = ChatWebSocketResponse(
                                        type = ChatMessageType.TYPING,
                                        data = chatMessage.data
                                    )
                                    connectionManager.broadcast(eventId, response)
                                }

                                ChatMessageType.REACTION -> {
                                    // Diffuser la réaction
                                    val response = ChatWebSocketResponse(
                                        type = ChatMessageType.REACTION,
                                        data = chatMessage.data
                                    )
                                    connectionManager.broadcast(eventId, response)
                                }

                                ChatMessageType.READ_RECEIPT -> {
                                    // Diffuser le reçu de lecture
                                    val response = ChatWebSocketResponse(
                                        type = ChatMessageType.READ_RECEIPT,
                                        data = chatMessage.data
                                    )
                                    connectionManager.broadcast(eventId, response)
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
                                errorMessage = "Invalid message format: ${e.message}"
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
            connectionManager.removeConnection(eventId)
        }
    }
}
