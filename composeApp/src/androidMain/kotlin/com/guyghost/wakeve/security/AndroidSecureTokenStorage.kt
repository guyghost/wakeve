package com.guyghost.wakeve.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Secure token storage for Android using KeyStore and EncryptedSharedPreferences
 */
class AndroidSecureTokenStorage(private val context: Context) : SecureTokenStorage {

    private val keyStore = KeyStore.getInstance("AndroidKeyStore").apply {
        load(null)
    }

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val encryptedPrefs = EncryptedSharedPreferences.create(
        context,
        "wakev_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private val accessTokenKey = "access_token"
    private val refreshTokenKey = "refresh_token"
    private val userIdKey = "user_id"
    private val tokenExpiryKey = "token_expiry"

    override suspend fun storeAccessToken(token: String): Result<Unit> = runCatching {
        encryptedPrefs.edit()
            .putString(accessTokenKey, token)
            .apply()
    }

    override suspend fun storeRefreshToken(token: String): Result<Unit> = runCatching {
        encryptedPrefs.edit()
            .putString(refreshTokenKey, token)
            .apply()
    }

    override suspend fun storeUserId(userId: String): Result<Unit> = runCatching {
        encryptedPrefs.edit()
            .putString(userIdKey, userId)
            .apply()
    }

    override suspend fun storeTokenExpiry(expiryTimestamp: Long): Result<Unit> = runCatching {
        encryptedPrefs.edit()
            .putLong(tokenExpiryKey, expiryTimestamp)
            .apply()
    }

    override suspend fun getAccessToken(): String? {
        return encryptedPrefs.getString(accessTokenKey, null)
    }

    override suspend fun getRefreshToken(): String? {
        return encryptedPrefs.getString(refreshTokenKey, null)
    }

    override suspend fun getUserId(): String? {
        return encryptedPrefs.getString(userIdKey, null)
    }

    override suspend fun getTokenExpiry(): Long? {
        val expiry = encryptedPrefs.getLong(tokenExpiryKey, 0L)
        return if (expiry == 0L) null else expiry
    }

    override suspend fun clearAllTokens(): Result<Unit> = runCatching {
        encryptedPrefs.edit()
            .remove(accessTokenKey)
            .remove(refreshTokenKey)
            .remove(userIdKey)
            .remove(tokenExpiryKey)
            .apply()
    }

    override suspend fun isTokenExpired(): Boolean {
        val expiry = getTokenExpiry()
        return expiry != null && System.currentTimeMillis() >= expiry
    }

    override suspend fun hasValidToken(): Boolean {
        val token = getAccessToken()
        return token != null && !isTokenExpired()
    }

    /**
     * Generate or retrieve encryption key from KeyStore
     */
    private fun getOrCreateSecretKey(): SecretKey {
        val keyAlias = "wakev_token_key"

        // Try to get existing key
        keyStore.getKey(keyAlias, null)?.let { return it as SecretKey }

        // Generate new key
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            "AndroidKeyStore"
        )

        val keyGenParameterSpec = KeyGenParameterSpec.Builder(
            keyAlias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()

        keyGenerator.init(keyGenParameterSpec)
        return keyGenerator.generateKey()
    }

    /**
     * Encrypt data using KeyStore key
     */
    private fun encryptData(data: String): String {
        val secretKey = getOrCreateSecretKey()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)

        val encryptedBytes = cipher.doFinal(data.toByteArray(Charsets.UTF_8))
        val iv = cipher.iv

        // Combine IV and encrypted data
        val combined = ByteArray(iv.size + encryptedBytes.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(encryptedBytes, 0, combined, iv.size, encryptedBytes.size)

        return Base64.encodeToString(combined, Base64.DEFAULT)
    }

    /**
     * Decrypt data using KeyStore key
     */
    private fun decryptData(encryptedData: String): String {
        val secretKey = getOrCreateSecretKey()
        val combined = Base64.decode(encryptedData, Base64.DEFAULT)

        // Extract IV and encrypted data
        val iv = combined.copyOfRange(0, 12) // GCM IV is 12 bytes
        val encryptedBytes = combined.copyOfRange(12, combined.size)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)

        val decryptedBytes = cipher.doFinal(encryptedBytes)
        return String(decryptedBytes, Charsets.UTF_8)
    }
}