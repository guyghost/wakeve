package com.guyghost.wakeve.notification

import com.guyghost.wakeve.repository.DatabaseEventRepository
import com.guyghost.wakeve.i18n.ServerLocalizer
import com.guyghost.wakeve.moderation.ModerationRepository
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
    private val eventRepository: DatabaseEventRepository,
    private val moderationRepository: ModerationRepository? = null
) {
    private val logger = LoggerFactory.getLogger("EventNotificationTrigger")
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // ==================== DEDUPLICATION ====================

    /**
     * Buffer de votes en attente de regroupement.
     * Cle : "userId:eventId" -> liste des noms de votants
     */
    private val pendingVoteNotifications = ConcurrentHashMap<String, MutableList<String>>()
    private val pendingVoteLocales = ConcurrentHashMap<String, String>()
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
                logger.info("Notification rate limit reached ({}/{}/h)", timestamps.size, maxNotificationsPerHour)
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
    fun onVoteAdded(eventId: String, voterId: String, voterName: String? = null, locale: String = "fr") {
        scope.launch {
            try {
                val event = eventRepository.getEvent(eventId) ?: return@launch
                val organizerId = event.organizerId

                // Ne pas notifier l'organisateur s'il vote lui-meme
                if (organizerId == voterId) return@launch

                val displayName = voterName ?: ServerLocalizer.t("notification.vote.default_voter", locale)
                val batchKey = "$organizerId:$eventId"

                val isFirstInBatch = voteFlushMutex.withLock {
                    val pending = pendingVoteNotifications.getOrPut(batchKey) { mutableListOf() }
                    pending.add(displayName)
                    pendingVoteLocales.putIfAbsent(batchKey, locale)
                    pending.size == 1 // Premier vote dans ce batch
                }

                // Si c'est le premier vote, planifier l'envoi apres 5 minutes
                if (isFirstInBatch) {
                    scope.launch {
                        delay(voteBatchWindowMs)
                        flushVoteNotification(batchKey, organizerId, eventId, event.title)
                    }
                }

                logger.info("Vote notification queued for batching")
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
            val voterNames: List<String>
            val locale: String
            voteFlushMutex.withLock {
                voterNames = pendingVoteNotifications.remove(batchKey) ?: return
                locale = pendingVoteLocales.remove(batchKey) ?: "fr"
            }

            if (voterNames.isEmpty()) return

            val (title, body) = if (voterNames.size == 1) {
                ServerLocalizer.t("notification.vote.title_single", locale) to
                    ServerLocalizer.t("notification.vote.body_single", locale, voterNames.first(), eventTitle)
            } else {
                ServerLocalizer.t("notification.vote.title_multiple", locale) to
                    ServerLocalizer.t("notification.vote.body_multiple", locale, voterNames.size, eventTitle)
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
                .onSuccess { logger.info("Vote notification batch sent (count={})", voterNames.size) }
                .onFailure { logger.warn("Failed to send batched vote notification") }
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
    fun onEventStatusChanged(eventId: String, newStatus: String, finalDate: String? = null, locale: String = "fr") {
        scope.launch {
            try {
                val event = eventRepository.getEvent(eventId) ?: return@launch
                val participants = eventRepository.getParticipants(eventId) ?: return@launch

                val (notificationType, title, body) = when (newStatus.uppercase()) {
                    "CONFIRMED", "FINALIZED" -> Triple(
                        NotificationType.DATE_CONFIRMED,
                        ServerLocalizer.t("notification.status.confirmed_title", locale),
                        if (finalDate != null) {
                            ServerLocalizer.t("notification.status.confirmed_body_with_date", locale, event.title, finalDate)
                        } else {
                            ServerLocalizer.t("notification.status.confirmed_body", locale, event.title)
                        }
                    )
                    "POLLING" -> Triple(
                        NotificationType.VOTE_REMINDER,
                        ServerLocalizer.t("notification.status.polling_title", locale),
                        ServerLocalizer.t("notification.status.polling_body", locale, event.title)
                    )
                    "CANCELLED" -> Triple(
                        NotificationType.EVENT_UPDATE,
                        ServerLocalizer.t("notification.status.cancelled_title", locale),
                        ServerLocalizer.t("notification.status.cancelled_body", locale, event.title)
                    )
                    else -> Triple(
                        NotificationType.EVENT_UPDATE,
                        ServerLocalizer.t("notification.status.updated_title", locale),
                        ServerLocalizer.t("notification.status.updated_body", locale, event.title)
                    )
                }

                // Notifier tous les participants (sauf l'organisateur qui a fait le changement)
                val deliveryResults = mutableListOf<Result<String>>()
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

                    val result = sendWithRateLimit(request)
                    deliveryResults += result
                    result
                        .onFailure { logger.warn("Failed to send status change notification") }
                }

                val summary = summarizeNotificationDelivery(deliveryResults)
                logger.info(
                    "Status change notifications processed (status={}, attempted={}, sent={}, failed={})",
                    newStatus,
                    summary.attempted,
                    summary.sent,
                    summary.failed
                )
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
    fun onNewComment(eventId: String, authorId: String, authorName: String, commentPreview: String, locale: String = "fr") {
        scope.launch {
            try {
                val event = eventRepository.getEvent(eventId) ?: return@launch
                val participants = eventRepository.getParticipants(eventId) ?: return@launch

                val truncatedPreview = if (commentPreview.length > 80) {
                    commentPreview.take(80) + "..."
                } else {
                    commentPreview
                }

                val deliveryResults = mutableListOf<Result<String>>()
                for (participantId in participants) {
                    // Ne pas notifier l'auteur du commentaire
                    if (participantId == authorId) continue
                    if (moderationRepository?.isBlocked(participantId, authorId) == true) continue

                    val request = NotificationRequest(
                        userId = participantId,
                        type = NotificationType.NEW_COMMENT,
                        title = ServerLocalizer.t("notification.comment.title", locale, authorName),
                        body = ServerLocalizer.t("notification.comment.body", locale, event.title, truncatedPreview),
                        eventId = eventId,
                        data = mapOf(
                            "eventId" to eventId,
                            "authorId" to authorId
                        )
                    )

                    val result = sendWithRateLimit(request)
                    deliveryResults += result
                    result
                        .onFailure { logger.warn("Failed to send comment notification") }
                }

                val summary = summarizeNotificationDelivery(deliveryResults)
                logger.info(
                    "Comment notifications processed (attempted={}, sent={}, failed={})",
                    summary.attempted,
                    summary.sent,
                    summary.failed
                )
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
    fun onDeadlineApproaching(eventId: String, hoursRemaining: Int, locale: String = "fr") {
        scope.launch {
            try {
                val event = eventRepository.getEvent(eventId) ?: return@launch
                val participants = eventRepository.getParticipants(eventId) ?: return@launch

                val timeText = when {
                    hoursRemaining <= 1 -> ServerLocalizer.t("notification.deadline.time_less_than_hour", locale)
                    hoursRemaining < 24 -> ServerLocalizer.t("notification.deadline.time_hours", locale, hoursRemaining)
                    else -> ServerLocalizer.t("notification.deadline.time_days", locale, hoursRemaining / 24)
                }

                val deliveryResults = mutableListOf<Result<String>>()
                for (participantId in participants) {
                    val request = NotificationRequest(
                        userId = participantId,
                        type = NotificationType.DEADLINE_REMINDER,
                        title = ServerLocalizer.t("notification.deadline.title", locale),
                        body = ServerLocalizer.t("notification.deadline.body", locale, timeText, event.title),
                        eventId = eventId,
                        data = mapOf(
                            "eventId" to eventId,
                            "hoursRemaining" to hoursRemaining.toString()
                        )
                    )

                    val result = sendWithRateLimit(request)
                    deliveryResults += result
                    result
                        .onFailure { logger.warn("Failed to send deadline reminder") }
                }

                val summary = summarizeNotificationDelivery(deliveryResults)
                logger.info(
                    "Deadline reminders processed (attempted={}, sent={}, failed={}, hoursRemaining={})",
                    summary.attempted,
                    summary.sent,
                    summary.failed,
                    hoursRemaining
                )
            } catch (e: Exception) {
                logger.error("Error triggering deadline notification", e)
            }
        }
    }
}

internal data class NotificationDeliverySummary(
    val attempted: Int,
    val sent: Int,
    val failed: Int
)

internal fun summarizeNotificationDelivery(results: List<Result<String>>): NotificationDeliverySummary {
    val sent = results.count { it.isSuccess }
    val failed = results.count { it.isFailure }
    return NotificationDeliverySummary(
        attempted = results.size,
        sent = sent,
        failed = failed
    )
}
