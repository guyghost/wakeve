package com.guyghost.wakeve.auth

import org.slf4j.LoggerFactory
import java.security.SecureRandom
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

/**
 * Gestionnaire d'OTP (One-Time Password) pour l'authentification par email.
 *
 * Stocke les OTP en mémoire avec expiration automatique, rate limiting,
 * et compteur de tentatives pour la sécurité.
 *
 * Thread-safe via ConcurrentHashMap.
 */
class OtpManager(
    /** Durée de validité d'un OTP en secondes (défaut: 5 minutes) */
    private val otpTtlSeconds: Long = 300,
    /** Nombre maximum de demandes d'OTP par email sur la fenêtre de rate limiting */
    private val maxRequestsPerWindow: Int = 3,
    /** Fenêtre de rate limiting en secondes (défaut: 15 minutes) */
    private val rateLimitWindowSeconds: Long = 900,
    /** Nombre maximum de tentatives de vérification avant invalidation */
    private val maxAttempts: Int = 5
) {
    private val logger = LoggerFactory.getLogger("OtpManager")
    private val random = SecureRandom()

    /** Stockage des OTP actifs, clé = email */
    private val otpStore = ConcurrentHashMap<String, OtpEntry>()

    /** Historique des demandes pour le rate limiting, clé = email */
    private val requestHistory = ConcurrentHashMap<String, MutableList<Instant>>()

    /**
     * Entrée OTP stockée en mémoire.
     */
    private data class OtpEntry(
        val code: String,
        val email: String,
        val createdAt: Instant,
        val expiresAt: Instant,
        var attempts: Int = 0
    ) {
        fun isExpired(): Boolean = Instant.now().isAfter(expiresAt)
    }

    /**
     * Vérifie si un email est limité par le rate limiting.
     *
     * @param email L'adresse email à vérifier
     * @return true si l'email a dépassé le nombre maximum de demandes
     */
    fun isRateLimited(email: String): Boolean {
        val normalizedEmail = email.lowercase().trim()
        val now = Instant.now()
        val windowStart = now.minusSeconds(rateLimitWindowSeconds)

        val history = requestHistory[normalizedEmail] ?: return false

        // Nettoyer les entrées hors fenêtre
        history.removeAll { it.isBefore(windowStart) }

        return history.size >= maxRequestsPerWindow
    }

    /**
     * Génère un nouvel OTP pour l'email donné.
     *
     * @param email L'adresse email destinataire
     * @return Le code OTP à 6 chiffres, ou null si rate limited
     */
    fun generateOtp(email: String): String? {
        val normalizedEmail = email.lowercase().trim()

        if (isRateLimited(normalizedEmail)) {
            logger.warn("Rate limit atteint pour l'email: {}", normalizedEmail)
            return null
        }

        val code = String.format("%06d", random.nextInt(1_000_000))
        val now = Instant.now()

        otpStore[normalizedEmail] = OtpEntry(
            code = code,
            email = normalizedEmail,
            createdAt = now,
            expiresAt = now.plusSeconds(otpTtlSeconds)
        )

        // Enregistrer la demande dans l'historique
        requestHistory.computeIfAbsent(normalizedEmail) { mutableListOf() }.add(now)

        logger.info("OTP généré pour {}: {} (expire dans {}s)", normalizedEmail, code, otpTtlSeconds)

        return code
    }

    /**
     * Vérifie un OTP pour l'email donné.
     *
     * @param email L'adresse email
     * @param code Le code OTP à vérifier
     * @return true si le code est valide et non expiré
     */
    fun verifyOtp(email: String, code: String): Boolean {
        val normalizedEmail = email.lowercase().trim()
        val entry = otpStore[normalizedEmail]

        if (entry == null) {
            logger.debug("Aucun OTP trouvé pour: {}", normalizedEmail)
            return false
        }

        if (entry.isExpired()) {
            logger.debug("OTP expiré pour: {}", normalizedEmail)
            otpStore.remove(normalizedEmail)
            return false
        }

        entry.attempts++

        if (entry.attempts > maxAttempts) {
            logger.warn("Nombre maximum de tentatives atteint pour: {}", normalizedEmail)
            otpStore.remove(normalizedEmail)
            return false
        }

        if (entry.code != code) {
            logger.debug(
                "OTP invalide pour: {} (tentative {}/{})",
                normalizedEmail, entry.attempts, maxAttempts
            )
            return false
        }

        // OTP valide, le supprimer du cache
        otpStore.remove(normalizedEmail)
        logger.info("OTP vérifié avec succès pour: {}", normalizedEmail)
        return true
    }

    /**
     * Retourne le nombre de tentatives restantes pour un OTP donné.
     *
     * @param email L'adresse email
     * @return Le nombre de tentatives restantes, ou 0 si aucun OTP actif
     */
    fun remainingAttempts(email: String): Int {
        val normalizedEmail = email.lowercase().trim()
        val entry = otpStore[normalizedEmail] ?: return 0
        if (entry.isExpired()) {
            otpStore.remove(normalizedEmail)
            return 0
        }
        return (maxAttempts - entry.attempts).coerceAtLeast(0)
    }

    /**
     * Nettoie les OTP expirés et l'historique de rate limiting obsolète.
     * Appelé périodiquement pour libérer la mémoire.
     */
    fun cleanupExpired() {
        val now = Instant.now()
        val windowStart = now.minusSeconds(rateLimitWindowSeconds)

        // Nettoyer les OTP expirés
        val expiredEmails = otpStore.entries
            .filter { it.value.isExpired() }
            .map { it.key }
        expiredEmails.forEach { otpStore.remove(it) }

        // Nettoyer l'historique de rate limiting
        requestHistory.forEach { (email, history) ->
            history.removeAll { it.isBefore(windowStart) }
            if (history.isEmpty()) {
                requestHistory.remove(email)
            }
        }

        if (expiredEmails.isNotEmpty()) {
            logger.debug("Nettoyage: {} OTP expirés supprimés", expiredEmails.size)
        }
    }
}
