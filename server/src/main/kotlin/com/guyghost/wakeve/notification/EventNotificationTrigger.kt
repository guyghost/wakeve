package com.guyghost.wakeve.notification

import com.guyghost.wakeve.DatabaseEventRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

/**
 * Service for triggering push notifications on key event actions.
 *
 * This service is called from route handlers to send notifications
 * asynchronously when important events occur:
 * - New vote on an event poll
 * - Event status change (confirmed, cancelled, etc.)
 * - New comment posted
 * - Deadline approaching (via scheduled task)
 *
 * Inclut la deduplication intelligente et le rate limiting :
 * - Les votes multiples pour le meme evenement sont regroupes en 5 minutes
 * - Maximum 10 notifications par heure par utilisateur
 */
class EventNotificationTrigger(
    private val notificationService: NotificationService,
    private val eventRepository: DatabaseEventRepository
) {
    private val logger = LoggerFactory.getLogger("EventNotificationTrigger")
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // ==================== DEDUPLICATION ====================

    /**
     * Buffer de votes en attente de regroupement.
     * Cle : "userId:eventId" -> liste des noms de votants
     */
    private val pendingVoteNotifications = ConcurrentHashMap<String, MutableList<String>>()
    private val voteFlushMutex = Mutex()

    /**
     * Delai de regroupement des notifications de vote (5 minutes).
     */
    private val voteBatchWindowMs = 5.minutes

    // ==================== RATE LIMITING ====================

    /**
     * Compteur de notifications envoyees par utilisateur dans l'heure courante.
     * Cle : userId -> liste de timestamps des notifications envoyees.
     */
    private val rateLimitCounters = ConcurrentHashMap<String, MutableList<Long>>()
    private val rateLimitMutex = Mutex()

    /**
     * Limite maximale de notifications par heure par utilisateur.
     */
    private val maxNotificationsPerHour = 10

    /**
     * Verifie si l'utilisateur peut recevoir une notification (rate limiting).
     *
     * @return true si la notification peut etre envoyee
     */
    private suspend fun canSendNotification(userId: String): Boolean {
        rateLimitMutex.withLock {
            val now = Clock.System.now().toEpochMilliseconds()
            val oneHourAgo = now - 1.hours.inWholeMilliseconds

            val timestamps = rateLimitCounters.getOrPut(userId) { mutableListOf() }

            // Nettoyer les entrees de plus d'une heure
            timestamps.removeAll { it < oneHourAgo }

            if (timestamps.size >= maxNotificationsPerHour) {
                logger.info("Rate limit atteint pour $userId (${timestamps.size}/$maxNotificationsPerHour/h)")
                return false
            }

            timestamps.add(now)
            return true
        }
    }

    /**
     * Envoie une notification avec rate limiting.
     */
    private suspend fun sendWithRateLimit(request: NotificationRequest): Result<String> {
        if (!canSendNotification(request.userId)) {
            return Result.failure(Exception("Rate limit atteint pour ${request.userId}"))
        }
        return notificationService.sendNotification(request)
    }

    /**
     * Trigger notification when a vote is added to an event poll.
     *
     * Utilise la deduplication : si plusieurs votes arrivent pour le meme
     * evenement dans une fenetre de 5 minutes, ils sont regroupes en une
     * seule notification "X personnes ont vote".
     *
     * @param eventId ID de l'evenement
     * @param voterId ID du participant qui a vote
     * @param voterName Nom du participant (optionnel)
     */
    fun onVoteAdded(eventId: String, voterId: String, voterName: String? = null) {
        scope.launch {
            try {
                val event = eventRepository.getEvent(eventId) ?: return@launch
                val organizerId = event.organizerId

                // Ne pas notifier l'organisateur s'il vote lui-meme
                if (organizerId == voterId) return@launch

                val displayName = voterName ?: "Un participant"
                val batchKey = "$organizerId:$eventId"

                val isFirstInBatch = voteFlushMutex.withLock {
                    val pending = pendingVoteNotifications.getOrPut(batchKey) { mutableListOf() }
                    pending.add(displayName)
                    pending.size == 1 // Premier vote dans ce batch
                }

                // Si c'est le premier vote, planifier l'envoi apres 5 minutes
                if (isFirstInBatch) {
                    scope.launch {
                        delay(voteBatchWindowMs)
                        flushVoteNotification(batchKey, organizerId, eventId, event.title)
                    }
                }

                logger.info("Vote de $displayName pour event $eventId mis en file d'attente")
            } catch (e: Exception) {
                logger.error("Error triggering vote notification", e)
            }
        }
    }

    /**
     * Envoie la notification regroupee de votes.
     */
    private suspend fun flushVoteNotification(
        batchKey: String,
        organizerId: String,
        eventId: String,
        eventTitle: String
    ) {
        try {
            val voterNames = voteFlushMutex.withLock {
                pendingVoteNotifications.remove(batchKey) ?: return
            }

            if (voterNames.isEmpty()) return

            val (title, body) = if (voterNames.size == 1) {
                "Nouveau vote" to "${voterNames.first()} a vote pour \"$eventTitle\""
            } else {
                "Nouveaux votes" to "${voterNames.size} personnes ont vote pour \"$eventTitle\""
            }

            val request = NotificationRequest(
                userId = organizerId,
                type = NotificationType.VOTE_REMINDER,
                title = title,
                body = body,
                eventId = eventId,
                data = mapOf(
                    "eventId" to eventId,
                    "voteCount" to voterNames.size.toString(),
                    "batched" to "true"
                )
            )

            sendWithRateLimit(request)
                .onSuccess { logger.info("Vote notification (batch=${voterNames.size}) sent for event $eventId") }
                .onFailure { logger.warn("Failed to send batched vote notification: ${it.message}") }
        } catch (e: Exception) {
            logger.error("Error flushing vote notification batch", e)
        }
    }

    /**
     * Trigger notification when event status changes.
     *
     * Notifies all participants about the status change.
     *
     * @param eventId ID de l'evenement
     * @param newStatus Nouveau statut de l'evenement
     * @param finalDate Date finale confirmee (si applicable)
     */
    fun onEventStatusChanged(eventId: String, newStatus: String, finalDate: String? = null) {
        scope.launch {
            try {
                val event = eventRepository.getEvent(eventId) ?: return@launch
                val participants = eventRepository.getParticipants(eventId) ?: return@launch

                val (notificationType, title, body) = when (newStatus.uppercase()) {
                    "CONFIRMED", "FINALIZED" -> Triple(
                        NotificationType.DATE_CONFIRMED,
                        "Date confirmee !",
                        "La date de \"${event.title}\" est confirmee${finalDate?.let { " : $it" } ?: ""}"
                    )
                    "POLLING" -> Triple(
                        NotificationType.VOTE_REMINDER,
                        "Votez maintenant !",
                        "Le sondage pour \"${event.title}\" est ouvert. Votez pour vos dates preferees."
                    )
                    "CANCELLED" -> Triple(
                        NotificationType.EVENT_UPDATE,
                        "Evenement annule",
                        "\"${event.title}\" a ete annule par l'organisateur."
                    )
                    else -> Triple(
                        NotificationType.EVENT_UPDATE,
                        "Mise a jour",
                        "\"${event.title}\" a ete mis a jour."
                    )
                }

                // Notifier tous les participants (sauf l'organisateur qui a fait le changement)
                for (participantId in participants) {
                    if (participantId == event.organizerId) continue

                    val request = NotificationRequest(
                        userId = participantId,
                        type = notificationType,
                        title = title,
                        body = body,
                        eventId = eventId,
                        data = mapOf(
                            "eventId" to eventId,
                            "status" to newStatus,
                            "finalDate" to (finalDate ?: "")
                        )
                    )

                    sendWithRateLimit(request)
                        .onFailure { logger.warn("Failed to notify participant $participantId: ${it.message}") }
                }

                logger.info("Status change notifications sent for event $eventId -> $newStatus")
            } catch (e: Exception) {
                logger.error("Error triggering status change notification", e)
            }
        }
    }

    /**
     * Trigger notification when a new comment is posted.
     *
     * Notifies other participants in the event.
     *
     * @param eventId ID de l'evenement
     * @param authorId ID de l'auteur du commentaire
     * @param authorName Nom de l'auteur
     * @param commentPreview Apercu du contenu du commentaire
     */
    fun onNewComment(eventId: String, authorId: String, authorName: String, commentPreview: String) {
        scope.launch {
            try {
                val event = eventRepository.getEvent(eventId) ?: return@launch
                val participants = eventRepository.getParticipants(eventId) ?: return@launch

                val truncatedPreview = if (commentPreview.length > 80) {
                    commentPreview.take(80) + "..."
                } else {
                    commentPreview
                }

                for (participantId in participants) {
                    // Ne pas notifier l'auteur du commentaire
                    if (participantId == authorId) continue

                    val request = NotificationRequest(
                        userId = participantId,
                        type = NotificationType.NEW_COMMENT,
                        title = "$authorName a commente",
                        body = "\"${event.title}\" : $truncatedPreview",
                        eventId = eventId,
                        data = mapOf(
                            "eventId" to eventId,
                            "authorId" to authorId
                        )
                    )

                    sendWithRateLimit(request)
                        .onFailure { logger.warn("Failed to notify participant $participantId: ${it.message}") }
                }
            } catch (e: Exception) {
                logger.error("Error triggering comment notification", e)
            }
        }
    }

    /**
     * Trigger deadline reminder notifications.
     *
     * Called by a scheduled task to warn participants about approaching deadlines.
     *
     * @param eventId ID de l'evenement
     * @param hoursRemaining Heures restantes avant la deadline
     */
    fun onDeadlineApproaching(eventId: String, hoursRemaining: Int) {
        scope.launch {
            try {
                val event = eventRepository.getEvent(eventId) ?: return@launch
                val participants = eventRepository.getParticipants(eventId) ?: return@launch

                val timeText = when {
                    hoursRemaining <= 1 -> "moins d'une heure"
                    hoursRemaining < 24 -> "$hoursRemaining heures"
                    else -> "${hoursRemaining / 24} jour(s)"
                }

                for (participantId in participants) {
                    val request = NotificationRequest(
                        userId = participantId,
                        type = NotificationType.DEADLINE_REMINDER,
                        title = "Deadline approche",
                        body = "Il reste $timeText pour voter sur \"${event.title}\"",
                        eventId = eventId,
                        data = mapOf(
                            "eventId" to eventId,
                            "hoursRemaining" to hoursRemaining.toString()
                        )
                    )

                    sendWithRateLimit(request)
                        .onFailure { logger.warn("Failed to send deadline reminder to $participantId: ${it.message}") }
                }

                logger.info("Deadline reminders sent for event $eventId ($hoursRemaining hours remaining)")
            } catch (e: Exception) {
                logger.error("Error triggering deadline notification", e)
            }
        }
    }
}
