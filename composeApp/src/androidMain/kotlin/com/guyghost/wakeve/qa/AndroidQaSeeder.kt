package com.guyghost.wakeve.qa

import android.content.Context
import android.util.Log
import com.guyghost.wakeve.database.WakeveDb
import com.guyghost.wakeve.invitationexperience.DirectInviteRecipientDigestPort
import com.guyghost.wakeve.invitationexperience.DirectInviteRecipientKeyOwner
import com.guyghost.wakeve.invitationexperience.DatabaseDirectInviteBatchRepository
import com.guyghost.wakeve.invitationexperience.DatabaseEventNotificationPreferenceRepository
import com.guyghost.wakeve.invitationexperience.EventNotificationPreference
import com.guyghost.wakeve.invitationexperience.InformationOperationAction
import com.guyghost.wakeve.invitationexperience.OperationKey
import com.guyghost.wakeve.invitationexperience.OperationSubject
import com.guyghost.wakeve.invitationexperience.OperationTarget
import com.guyghost.wakeve.models.Event
import com.guyghost.wakeve.models.EventPlanningMode
import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.models.EventType
import com.guyghost.wakeve.models.TimeOfDay
import com.guyghost.wakeve.models.TimeSlot
import com.guyghost.wakeve.repository.DatabaseEventRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Portage Android du seeder QA iOS (`InvitationExperienceQALaunchSupport`).
 *
 * DEBUG/QA uniquement : peuple le repository SQLDelight avec les 5 événements
 * du cycle (draft, polling, confirmed, finalized, past) + utilisateurs,
 * audiences, lieux, notification, invitations directes protégées.
 * Idempotent : relancer ne duplique rien (contract iOS
 * `…IdempotentAcrossRelaunch`).
 *
 * Ne jamais inclure dans les builds release : uniquement appelé depuis
 * `App.kt` derrière le garde `FLAG_DEBUGGABLE` + extra `wakeve.dev.auth`.
 */
class AndroidQaSeeder(
    private val context: Context,
    private val database: WakeveDb,
    private val eventRepository: DatabaseEventRepository,
    private val directInviteRepository: DatabaseDirectInviteBatchRepository,
    private val notificationRepository: DatabaseEventNotificationPreferenceRepository
) {
    object Seed {
        const val DRAFT = "qa-invitation-draft"
        const val POLLING = "qa-invitation-polling"
        const val CONFIRMED = "qa-invitation-confirmed"
        const val FINALIZED = "qa-invitation-finalized"
        const val PAST = "qa-invitation-past"
        const val PENDING_PARTICIPANT = "qa-invitation-guest-pending"
        const val DIRECT_INVITE_BATCH = "qa-invitation-direct-batch"
        const val DIRECT_INVITE_OPERATION = "qa-invitation-direct-operation"
        const val NOTIFICATION_OPERATION = "qa-invitation-notification-operation"
    }

    private data class QaSeedEvent(
        val event: Event,
        val confirmed: Boolean,
        val presetId: String,
        val focalX: Double,
        val focalY: Double
    )

    suspend fun seed(requestedViewerId: String): Boolean = withContext(Dispatchers.IO) {
        // Stabiliser le viewerId QA entre les sessions (le guest id régénère à
        // chaque lancement ; user.email est UNIQUE) — DEBUG only.
        val prefs = context.getSharedPreferences("wakeve-qa", Context.MODE_PRIVATE)
        val viewerId = prefs.getString("qaViewerId", null) ?: requestedViewerId.also {
            prefs.edit().putString("qaViewerId", it).apply()
        }
        val now = Date()
        val futureStart = Date(now.time + 30L * 24 * 60 * 60 * 1000)
        val futureEnd = Date(futureStart.time + 3L * 60 * 60 * 1000)
        val pastStart = Date(now.time - 30L * 24 * 60 * 60 * 1000)
        val pastEnd = Date(pastStart.time + 3L * 60 * 60 * 1000)

        if (!ensureQaUsers(viewerId)) {
            Log.d(TAG, "seed step failed: ensureQaUsers")
            return@withContext false
        }

        val seeds = listOf(
            makeEvent(
                id = Seed.DRAFT,
                title = "Escapade à Annecy",
                status = EventStatus.DRAFT,
                viewerId = viewerId,
                start = futureStart,
                end = futureEnd,
                confirmed = false,
                presetId = "wakeve-lake"
            ),
            makeEvent(
                id = Seed.POLLING,
                title = "Vote pour le week-end",
                status = EventStatus.POLLING,
                viewerId = viewerId,
                start = Date(futureStart.time + 7L * 24 * 60 * 60 * 1000),
                end = Date(futureEnd.time + 7L * 24 * 60 * 60 * 1000),
                confirmed = false,
                presetId = "wakeve-celebration"
            ),
            makeEvent(
                id = Seed.CONFIRMED,
                title = "Week-end confirmé",
                status = EventStatus.CONFIRMED,
                viewerId = viewerId,
                start = Date(futureStart.time + 14L * 24 * 60 * 60 * 1000),
                end = Date(futureEnd.time + 14L * 24 * 60 * 60 * 1000),
                confirmed = true,
                presetId = "wakeve-sunset"
            ),
            makeEvent(
                id = Seed.FINALIZED,
                title = "Séjour finalisé",
                status = EventStatus.FINALIZED,
                viewerId = viewerId,
                start = Date(futureStart.time + 21L * 24 * 60 * 60 * 1000),
                end = Date(futureEnd.time + 21L * 24 * 60 * 60 * 1000),
                confirmed = true,
                presetId = "wakeve-lake"
            ),
            makeEvent(
                id = Seed.PAST,
                title = "Souvenir du lac",
                status = EventStatus.CONFIRMED,
                viewerId = viewerId,
                start = pastStart,
                end = pastEnd,
                confirmed = true,
                presetId = "wakeve-celebration"
            )
        )

        for (seed in seeds) {
            if (!ensureEvent(seed)) {
                Log.d(TAG, "seed step failed: ensureEvent ${seed.event.id}")
                return@withContext false
            }
        }

        if (!ensureDraftAudience(viewerId)) {
            Log.d(TAG, "seed step failed: ensureDraftAudience")
            return@withContext false
        }
        if (!ensureDraftLocation()) {
            Log.d(TAG, "seed step failed: ensureDraftLocation")
            return@withContext false
        }
        if (!ensureConfirmedNotification(viewerId)) {
            Log.d(TAG, "seed step failed: ensureConfirmedNotification")
            return@withContext false
        }
        if (!ensureProtectedDirectInvite(viewerId)) {
            Log.d(TAG, "seed step failed: ensureProtectedDirectInvite")
            return@withContext false
        }
        Log.d(TAG, "seed OK for viewerId=$viewerId")
        true
    }

    private fun ensureQaUsers(viewerId: String): Boolean {
        val now = iso8601(Date())
        if (database.userQueries.selectUserById(id = viewerId).executeAsOneOrNull() == null) {
            database.userQueries.insertUser(
                id = viewerId,
                provider_id = "qa-provider-organizer",
                email = "organizer@qa.wakeve.invalid",
                name = "Léa Martin",
                avatar_url = null,
                provider = "qa",
                role = "ORGANIZER",
                created_at = now,
                updated_at = now
            )
        }
        if (database.userQueries.selectUserById(id = Seed.PENDING_PARTICIPANT)
            .executeAsOneOrNull() == null
        ) {
            database.userQueries.insertUser(
                id = Seed.PENDING_PARTICIPANT,
                provider_id = "qa-provider-guest",
                email = "guest@qa.wakeve.invalid",
                name = "Noé Bernard",
                avatar_url = null,
                provider = "qa",
                role = "USER",
                created_at = now,
                updated_at = now
            )
        }
        return database.userQueries.selectUserById(id = viewerId).executeAsOneOrNull() != null &&
            database.userQueries.selectUserById(id = Seed.PENDING_PARTICIPANT)
            .executeAsOneOrNull() != null
    }

    private suspend fun ensureEvent(seed: QaSeedEvent): Boolean {
        val existing = eventRepository.getEvent(id = seed.event.id)
        if (existing != null) {
            if (existing.organizerId != seed.event.organizerId ||
                existing.status != seed.event.status ||
                existing.proposedSlots.isEmpty()
            ) {
                return false
            }
            if (existing.description != seed.event.description) {
                eventRepository.updateEvent(event = seed.event)
            }
        } else {
            eventRepository.createEvent(event = seed.event)
                .onFailure {
                    Log.d(TAG, "createEvent failed: $it")
                    return false
                }
        }

        val now = iso8601(Date())
        val currentArtwork = database.invitationExperienceQueries
            .selectArtworkByEventId(event_id = seed.event.id)
            .executeAsOneOrNull()
        if (currentArtwork?.kind != "STRUCTURED" ||
            currentArtwork?.source_kind != "PRESET" ||
            currentArtwork?.preset_id != seed.presetId
        ) {
            database.invitationExperienceQueries.upsertEventArtwork(
                event_id = seed.event.id,
                kind = "STRUCTURED",
                structured_version = 1L,
                source_kind = "PRESET",
                preset_id = seed.presetId,
                server_asset_id = null,
                canonical_https_url = null,
                asset_revision = null,
                alt_kind = "DECORATIVE",
                alt_text = null,
                focal_x = seed.focalX,
                focal_y = seed.focalY,
                crop = "FILL",
                legacy_remote_url = null,
                updated_at = now
            )
        }

        if (seed.confirmed &&
            database.confirmedDateQueries.existsByEventId(eventId = seed.event.id)
            .executeAsOneOrNull() == null
        ) {
            // Le repository persiste les créneaux sous l'identité physique
            // namespacée (TimeSlotStorageIdentity.physicalId) — relire la ligne
            // persistée pour satisfaire la FK confirmedDate(timeSlotId).
            val persistedSlot = database.timeSlotQueries
                .selectByEventId(eventId = seed.event.id)
                .executeAsOneOrNull()
                ?: return false
            database.confirmedDateQueries.insertConfirmedDate(
                id = "qa-confirmed-${seed.event.id}",
                eventId = seed.event.id,
                timeslotId = persistedSlot.id,
                confirmedByOrganizerId = seed.event.organizerId,
                confirmedAt = now,
                updatedAt = now
            )
        }

        return eventRepository.getEvent(id = seed.event.id) != null
    }

    private suspend fun ensureDraftAudience(viewerId: String): Boolean {
        val records = eventRepository.getParticipantRecords(eventId = Seed.DRAFT).orEmpty()
        if (records.none { it.userId == Seed.PENDING_PARTICIPANT }) {
            eventRepository.addParticipant(
                eventId = Seed.DRAFT,
                participantId = Seed.PENDING_PARTICIPANT
            )
        }

        val organizer = database.participantQueries
            .selectByEventIdAndUserId(eventId = Seed.DRAFT, userId = viewerId)
            .executeAsOneOrNull()
            ?: return false
        if (organizer.hasValidatedDate != 1L) {
            database.participantQueries.updateValidation(
                hasValidatedDate = 1L,
                updatedAt = iso8601(Date()),
                id = organizer.id
            )
        }

        database.participantQueries.updateAccessAxes(
            rsvpState = "ACCEPTED",
            dateValidationState = "VALIDATED_RETAINED_DATE",
            updatedAt = iso8601(Date()),
            id = organizer.id
        )
        database.participantQueries
            .selectByEventIdAndUserId(eventId = Seed.DRAFT, userId = Seed.PENDING_PARTICIPANT)
            .executeAsOneOrNull()
            ?.let { pending ->
                database.participantQueries.updateAccessAxes(
                    rsvpState = "PENDING",
                    dateValidationState = "NOT_VALIDATED",
                    updatedAt = iso8601(Date()),
                    id = pending.id
                )
            }

        val updated = eventRepository.getParticipantRecords(eventId = Seed.DRAFT).orEmpty()
        return updated.size >= 2 &&
            updated.any { it.rsvp == "ACCEPTED" } &&
            updated.any { it.rsvp == "PENDING" }
    }

    private fun ensureDraftLocation(): Boolean {
        if (database.potentialLocationQueries
            .selectFirstLocationByEventId(eventId = Seed.DRAFT)
            .executeAsOneOrNull() == null
        ) {
            database.potentialLocationQueries.insertLocation(
                id = "qa-location-annecy",
                eventId = Seed.DRAFT,
                name = "Annecy",
                locationType = "CITY",
                address = null,
                coordinates = null,
                createdAt = iso8601(Date())
            )
        }
        return database.potentialLocationQueries
            .selectFirstLocationByEventId(eventId = Seed.DRAFT)
            .executeAsOneOrNull() != null
    }

    private suspend fun ensureConfirmedNotification(viewerId: String): Boolean {
        return try {
            if (notificationRepository.get(eventId = Seed.CONFIRMED, userId = viewerId) != null) {
                return true
            }
            notificationRepository.save(
                operationKey = com.guyghost.wakeve.invitationexperience.OperationKey(
                    subject = com.guyghost.wakeve.invitationexperience.OperationSubject.EventNotification(
                        eventId = Seed.CONFIRMED,
                        userId = viewerId
                    ),
                    action = com.guyghost.wakeve.invitationexperience.InformationOperationAction.SAVE_EVENT_PREFERENCE,
                    target = com.guyghost.wakeve.invitationexperience.OperationTarget.User(viewerId),
                    operationId = Seed.NOTIFICATION_OPERATION
                ),
                preference = com.guyghost.wakeve.invitationexperience.EventNotificationPreference.ALL_EVENT_UPDATES
            )
            notificationRepository.get(eventId = Seed.CONFIRMED, userId = viewerId) != null
        } catch (t: Throwable) {
            Log.d(TAG, "ensureConfirmedNotification failed: $t")
            false
        }
    }

    private suspend fun ensureProtectedDirectInvite(viewerId: String): Boolean {
        val existingBatches = database.invitationExperienceQueries
            .selectDirectInviteBatchesByEventId(event_id = Seed.DRAFT)
            .executeAsList()
        if (existingBatches.isNotEmpty()) {
            val outcomes = database.invitationExperienceQueries
                .selectDirectInviteRecipientOutcomes(batch_id = existingBatches.first().batch_id)
                .executeAsList()
            return existingBatches.size == 1 && outcomes.size >= 2
        }

        val event = eventRepository.getEvent(id = Seed.DRAFT)
            ?.takeIf { it.organizerId == viewerId && it.status == EventStatus.DRAFT }
            ?: return false
        val aggregate = database.eventQueries.selectById(id = Seed.DRAFT)
            .executeAsOneOrNull()
            ?.takeIf { it.aggregateSchemaVersion == 1L }
            ?: return false

        val keyOwner = DirectInviteRecipientKeyOwner(QaDigestPort, keyVersion = 1)
        val firstKey = keyOwner.protect(rawRecipientInput = "+33 6 12 34 56 70")
            ?: return false.also { Log.d(TAG, "directInvite key protection failed") }
        val secondKey = keyOwner.protect(rawRecipientInput = "+33 6 12 34 56 71")
            ?: return false.also { Log.d(TAG, "directInvite key protection failed") }

        // Écriture directe du lot protégé : le submit(command:) 1-arg du
        // repository est une frontière sous garde (enveloppes scellées du flux
        // audience requis) — le harnais QA reflète la transaction du repository
        // (mêmes tables, mêmes statuts, clés hmac, aucun destinataire brut).
        val timestamp = iso8601(Date())
        val retentionExpiry = iso8601(Date(Date().time + 29L * 24 * 60 * 60 * 1000))
        database.invitationExperienceQueries.insertDirectInviteBatch(
            batch_id = Seed.DIRECT_INVITE_BATCH,
            event_id = event.id,
            actor_id = viewerId,
            operation_id = Seed.DIRECT_INVITE_OPERATION,
            access_revision = aggregate.aggregateRevision,
            status = "PENDING_SYNC",
            created_at = timestamp,
            updated_at = timestamp,
            expires_at = retentionExpiry
        )
        for (key in listOf(firstKey, secondKey)) {
            database.invitationExperienceQueries.insertDirectInviteRecipientOutcome(
                batch_id = Seed.DIRECT_INVITE_BATCH,
                recipient_key = key.value,
                key_version = 1L,
                status = "QUEUED_LOCAL",
                invitation_id = null,
                reason_code = null,
                expires_at = retentionExpiry,
                updated_at = timestamp
            )
        }

        val batches = database.invitationExperienceQueries
            .selectDirectInviteBatchesByEventId(event_id = event.id)
            .executeAsList()
        if (batches.size != 1) return false
        val batch = batches.first()
        if (database.invitationExperienceQueries
            .selectDirectInviteRecipientOutcomes(batch_id = batch.batch_id)
            .executeAsList()
            .size < 2
        ) return false
        // Vérifier via le repository réel (même frontière que l'UI audience).
        return directInviteRepository.load(batchId = batch.batch_id) != null
    }

    private fun makeEvent(
        id: String,
        title: String,
        status: EventStatus,
        viewerId: String,
        start: Date,
        end: Date,
        confirmed: Boolean,
        presetId: String
    ): QaSeedEvent {
        val startValue = iso8601(start)
        val endValue = iso8601(end)
        val slot = TimeSlot(
            id = "qa-slot-$id",
            start = startValue,
            end = endValue,
            timezone = "Europe/Paris",
            timeOfDay = TimeOfDay.SPECIFIC
        )
        val now = iso8601(Date())
        return QaSeedEvent(
            event = Event(
                id = id,
                title = title,
                description = "Un week-end au bord du lac d’Annecy pour retrouver le groupe et profiter du château.",
                organizerId = viewerId,
                participants = listOf(viewerId),
                proposedSlots = listOf(slot),
                deadline = iso8601(Date(start.time - 7L * 24 * 60 * 60 * 1000)),
                status = status,
                finalDate = if (confirmed) startValue else null,
                createdAt = now,
                updatedAt = now,
                eventType = EventType.OTHER,
                eventTypeCustom = null,
                minParticipants = 2,
                maxParticipants = 8,
                expectedParticipants = 4,
                heroImageUrl = null,
                planningMode = EventPlanningMode.TIME_SLOT_POLL,
                aggregateRevision = 1L,
                aggregateSchemaVersion = 1L
            ),
            confirmed = confirmed,
            presetId = presetId,
            focalX = if (id == Seed.DRAFT) 0.72 else 0.5,
            focalY = if (id == Seed.DRAFT) 0.42 else 0.5
        )
    }

    private fun iso8601(date: Date): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        formatter.timeZone = TimeZone.getTimeZone("UTC")
        return formatter.format(date)
    }

    /**
     * Port digest QA : HMAC-SHA256 avec secret fixe debug (jamais en release).
     * Miroir JVM du Keychain port iOS — les clés n'ont besoin d'être que des
     * digests hex valides et stables pendant la session QA.
     */
    private object QaDigestPort : DirectInviteRecipientDigestPort {
        private val secret = ByteArray(32) { (it * 37 + 11).toByte() }

        override fun hmacSha256(normalizedRecipient: String): String? = try {
            val mac = Mac.getInstance("HmacSHA256")
            mac.init(SecretKeySpec(secret, "HmacSHA256"))
            mac.doFinal(normalizedRecipient.toByteArray(Charsets.UTF_8))
                .joinToString("") { "%02x".format(it) }
        } catch (t: Throwable) {
            Log.d(TAG, "hmac failed: $t")
            null
        }
    }

    private companion object {
        const val TAG = "QALaunch"

    }
}
