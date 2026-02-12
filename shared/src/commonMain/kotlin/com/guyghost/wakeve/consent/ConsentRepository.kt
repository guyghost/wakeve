package com.guyghost.wakeve.consent

import com.guyghost.wakeve.database.WakeveDb
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.Clock

/**
 * Interface for consent management.
 *
 * Manages user consent for analytics and data collection
 * in compliance with RGPD regulations.
 */
interface ConsentRepository {
    /**
     * Check if user has granted consent
     *
     * @return true if consent granted, false otherwise
     */
    suspend fun hasGivenConsent(userId: String): Boolean

    /**
     * Grant consent for analytics and data collection
     *
     * @param userId The user ID granting consent
     */
    suspend fun grantConsent(userId: String)

    /**
     * Revoke consent and clear data
     *
     * @param userId The user ID revoking consent
     */
    suspend fun revokeConsent(userId: String)

    /**
     * Get the date when consent was granted
     *
     * @return Epoch milliseconds of consent date, or null if not granted
     */
    suspend fun getConsentDate(userId: String): Long?

    /**
     * Observe consent changes as a flow
     *
     * @return StateFlow emitting current consent status
     */
    fun observeConsent(): StateFlow<Boolean>
}

/**
 * Default implementation of ConsentRepository.
 *
 * Uses SQLDelight database for persistence.
 */
class ConsentRepositoryImpl(
    private val database: WakeveDb
) : ConsentRepository {

    private val _consentFlow = MutableStateFlow(false)

    override fun observeConsent(): StateFlow<Boolean> = _consentFlow.asStateFlow()

    override suspend fun hasGivenConsent(userId: String): Boolean {
        val hasConsent = database.consentQueries
            .hasGivenConsent(userId)
            .executeAsOneOrNull()

        // Update flow if value changed
        if (_consentFlow.value != (hasConsent == 1L)) {
            _consentFlow.value = hasConsent == 1L
        }

        return hasConsent == 1L
    }

    override suspend fun grantConsent(userId: String) {
        val now = Clock.System.now().toEpochMilliseconds()
        val timestamp = Clock.System.now().toString()

        database.consentQueries.insertConsent(
            user_id = userId,
            has_consent = 1L,
            consent_date = now,
            updated_at = timestamp
        )

        _consentFlow.value = true
    }

    override suspend fun revokeConsent(userId: String) {
        val timestamp = Clock.System.now().toString()

        database.consentQueries.updateConsent(
            has_consent = 0L,
            consent_date = null,
            updated_at = timestamp,
            user_id = userId
        )

        _consentFlow.value = false
    }

    override suspend fun getConsentDate(userId: String): Long? {
        val consent = database.consentQueries
            .selectConsentByUserId(userId)
            .executeAsOneOrNull()

        return consent?.consent_date
    }
}
