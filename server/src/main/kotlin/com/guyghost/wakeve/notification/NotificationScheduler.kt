package com.guyghost.wakeve.notification

import com.guyghost.wakeve.DatabaseEventRepository
import com.guyghost.wakeve.i18n.ServerLocalizer
import kotlinx.coroutines.*
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.slf4j.LoggerFactory
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

/**
 * Planificateur de notifications intelligentes.
 *
 * Gere les rappels automatiques :
 * - Rappel de deadline (24h et 1h avant la date limite du sondage)
 * - Rappel du jour de l'evenement (matin du jour confirme)
 * - Digest hebdomadaire (resume des notifications non lues)
 *
 * Utilise des coroutines avec delay() pour la planification.
 */
class NotificationScheduler(
    private val notificationService: NotificationService,
    private val eventNotificationTrigger: EventNotificationTrigger,
    private val eventRepository: DatabaseEventRepository
) {
    private val logger = LoggerFactory.getLogger("NotificationScheduler")
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Jobs actifs pour le suivi et l'annulation
    private val scheduledJobs = mutableMapOf<String, Job>()

    /**
     * Demarre le planificateur.
     * Lance les taches periodiques de verification.
     */
    fun start() {
        logger.info("Demarrage du NotificationScheduler")

        // Verification periodique des deadlines (toutes les 15 minutes)
        scope.launch {
            while (isActive) {
                try {
                    checkDeadlineReminders()
                } catch (e: Exception) {
                    logger.error("Erreur lors de la verification des deadlines", e)
                }
                delay(15.minutes)
            }
        }

        // Verification periodique des evenements du jour (toutes les heures)
        scope.launch {
            while (isActive) {
                try {
                    checkEventDayReminders()
                } catch (e: Exception) {
                    logger.error("Erreur lors de la verification des evenements du jour", e)
                }
                delay(1.hours)
            }
        }

        // Digest hebdomadaire (toutes les 24h, verifie si c'est lundi matin)
        scope.launch {
            while (isActive) {
                try {
                    checkWeeklyDigest()
                } catch (e: Exception) {
                    logger.error("Erreur lors de la verification du digest hebdomadaire", e)
                }
                delay(24.hours)
            }
        }
    }

    /**
     * Arrete le planificateur et annule tous les jobs.
     */
    fun stop() {
        logger.info("Arret du NotificationScheduler")
        scope.cancel()
        scheduledJobs.clear()
    }

    /**
     * Planifie un rappel de deadline pour un evenement specifique.
     *
     * @param eventId ID de l'evenement
     * @param deadline Instant de la deadline
     */
    fun scheduleDeadlineReminder(eventId: String, deadline: Instant) {
        val now = Clock.System.now()

        // Rappel 24h avant
        val reminder24h = deadline - 24.hours
        if (reminder24h > now) {
            val delay24h = reminder24h - now
            val jobKey = "deadline-24h-$eventId"

            scheduledJobs[jobKey]?.cancel()
            scheduledJobs[jobKey] = scope.launch {
                delay(delay24h)
                eventNotificationTrigger.onDeadlineApproaching(eventId, 24)
                scheduledJobs.remove(jobKey)
            }
            logger.info("Rappel 24h planifie pour l'evenement $eventId dans ${delay24h.inWholeMinutes} minutes")
        }

        // Rappel 1h avant
        val reminder1h = deadline - 1.hours
        if (reminder1h > now) {
            val delay1h = reminder1h - now
            val jobKey = "deadline-1h-$eventId"

            scheduledJobs[jobKey]?.cancel()
            scheduledJobs[jobKey] = scope.launch {
                delay(delay1h)
                eventNotificationTrigger.onDeadlineApproaching(eventId, 1)
                scheduledJobs.remove(jobKey)
            }
            logger.info("Rappel 1h planifie pour l'evenement $eventId dans ${delay1h.inWholeMinutes} minutes")
        }
    }

    /**
     * Annule les rappels planifies pour un evenement.
     */
    fun cancelReminders(eventId: String) {
        scheduledJobs.keys
            .filter { it.contains(eventId) }
            .forEach { key ->
                scheduledJobs[key]?.cancel()
                scheduledJobs.remove(key)
                logger.info("Rappel annule: $key")
            }
    }

    /**
     * Verifie les deadlines approchantes dans la base de donnees.
     * Envoie des rappels pour les evenements en cours de sondage
     * dont la deadline est dans 24h ou 1h.
     */
    private suspend fun checkDeadlineReminders() {
        val now = Clock.System.now()
        val events = eventRepository.getAllPollingEvents()

        for (event in events) {
            val deadline = runCatching {
                Instant.parse(event.deadline)
            }.getOrNull() ?: continue

            val hoursRemaining = (deadline - now).inWholeHours.toInt()

            // Envoyer un rappel si on est dans la fenetre de 24h ou 1h
            when {
                hoursRemaining in 23..25 -> {
                    val jobKey = "deadline-check-24h-${event.id}"
                    if (jobKey !in scheduledJobs) {
                        scheduledJobs[jobKey] = scope.launch {
                            eventNotificationTrigger.onDeadlineApproaching(event.id, 24)
                        }
                    }
                }
                hoursRemaining in 0..1 -> {
                    val jobKey = "deadline-check-1h-${event.id}"
                    if (jobKey !in scheduledJobs) {
                        scheduledJobs[jobKey] = scope.launch {
                            eventNotificationTrigger.onDeadlineApproaching(event.id, 1)
                        }
                    }
                }
            }
        }
    }

    /**
     * Verifie les evenements confirmes du jour et envoie des rappels matinaux.
     */
    private suspend fun checkEventDayReminders() {
        val now = Clock.System.now()
        val todayMs = now.toEpochMilliseconds()

        // On verifie les evenements confirmes dont la date finale est aujourd'hui
        val events = eventRepository.getConfirmedEventsForToday(todayMs)

        for (event in events) {
            val jobKey = "event-day-${event.id}-${todayMs / 86_400_000}"
            if (jobKey in scheduledJobs) continue

            val participants = eventRepository.getParticipants(event.id) ?: continue

            for (participantId in participants) {
                // TODO: resolve user's preferred locale from profile/preferences
                val locale = "fr"

                val request = NotificationRequest(
                    userId = participantId,
                    type = NotificationType.MEETING_REMINDER,
                    title = ServerLocalizer.t("notification.event_day.title", locale),
                    body = ServerLocalizer.t("notification.event_day.body", locale, event.title),
                    eventId = event.id,
                    data = mapOf("eventId" to event.id)
                )

                notificationService.sendNotification(request)
                    .onFailure {
                        logger.warn("Echec du rappel jour-J pour $participantId: ${it.message}")
                    }
            }

            scheduledJobs[jobKey] = Job() // Marquer comme traite
            logger.info("Rappels jour-J envoyes pour l'evenement ${event.id}")
        }
    }

    /**
     * Verifie s'il faut envoyer le digest hebdomadaire.
     * Envoye le lundi a 9h (approximation basee sur l'heure UTC).
     */
    private suspend fun checkWeeklyDigest() {
        val now = Clock.System.now()
        val dayOfWeek = (now.toEpochMilliseconds() / 86_400_000 + 4) % 7 // 0=dimanche, 1=lundi...
        val hourOfDay = (now.toEpochMilliseconds() / 3_600_000) % 24

        // Envoyer le digest le lundi entre 8h et 10h UTC
        if (dayOfWeek != 1L || hourOfDay !in 8..9) return

        val weekKey = "weekly-digest-${now.toEpochMilliseconds() / 604_800_000}"
        if (weekKey in scheduledJobs) return

        logger.info("Envoi du digest hebdomadaire")

        // Recuperer tous les utilisateurs avec des tokens enregistres
        val userIds = eventRepository.getAllUserIds()

        for (userId in userIds) {
            val unreadNotifications = notificationService.getUnreadNotifications(userId)
            if (unreadNotifications.isEmpty()) continue

            // TODO: resolve user's preferred locale from profile/preferences
            val locale = "fr"

            val count = unreadNotifications.size
            val summary = buildString {
                if (count == 1) {
                    append(ServerLocalizer.t("notification.digest.unread_single", locale))
                } else {
                    append(ServerLocalizer.t("notification.digest.unread_multiple", locale, count))
                }
                val types = unreadNotifications.groupBy { it.type }
                val parts = mutableListOf<String>()
                types.forEach { (type, items) ->
                    val label = when (type.name) {
                        "VOTE_SUBMITTED", "VOTE_CLOSE_REMINDER" ->
                            ServerLocalizer.t("notification.digest.votes", locale, items.size)
                        "COMMENT_POSTED", "COMMENT_REPLY" ->
                            ServerLocalizer.t("notification.digest.comments", locale, items.size)
                        "EVENT_UPDATE", "EVENT_CONFIRMED" ->
                            ServerLocalizer.t("notification.digest.updates", locale, items.size)
                        "DEADLINE_REMINDER" ->
                            ServerLocalizer.t("notification.digest.reminders", locale, items.size)
                        else ->
                            ServerLocalizer.t("notification.digest.notifications", locale, items.size)
                    }
                    parts.add(label)
                }
                append(parts.joinToString(", "))
            }

            val request = NotificationRequest(
                userId = userId,
                type = NotificationType.EVENT_UPDATE,
                title = ServerLocalizer.t("notification.digest.title", locale),
                body = summary,
                data = mapOf("digest" to "true")
            )

            notificationService.sendNotification(request)
                .onFailure {
                    logger.warn("Echec du digest pour $userId: ${it.message}")
                }
        }

        scheduledJobs[weekKey] = Job()
    }
}
