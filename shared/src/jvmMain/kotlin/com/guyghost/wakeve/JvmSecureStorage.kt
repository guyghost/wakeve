package com.guyghost.wakeve

import com.guyghost.wakeve.models.UserSession
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.nio.file.Files
import java.nio.file.Paths
import java.io.File
import java.util.*
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

/**
 * Implémentation JVM du stockage sécurisé
 * Utilise AES pour chiffrer les données sensibles
 */
class JvmSecureStorage(private val storagePath: String = ".wakeve/auth") : SecureStorage {
    
    private val json = Json { ignoreUnknownKeys = true }
    private val sessionFile = File(storagePath, "session.enc")
    private val keyFile = File(storagePath, "key.enc")
    private var secretKey: SecretKey? = null
    
    init {
        val dir = File(storagePath)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        loadOrCreateKey()
    }
    
    override suspend fun saveSession(session: UserSession) {
        try {
            val json = Json.encodeToString(session)
            val encrypted = encryptData(json.toByteArray(Charsets.UTF_8))
            
            sessionFile.writeBytes(encrypted)
        } catch (e: Exception) {
            throw Exception("Erreur lors de la sauvegarde de la session: ${e.message}")
        }
    }
    
    override suspend fun getSession(): UserSession? {
        return try {
            if (!sessionFile.exists()) return null
            
            val encrypted = sessionFile.readBytes()
            val decrypted = decryptData(encrypted)
            val json = String(decrypted, Charsets.UTF_8)
            
            Json.decodeFromString(json)
        } catch (e: Exception) {
            null
        }
    }
    
    override fun clearSession() {
        if (sessionFile.exists()) {
            sessionFile.delete()
        }
    }
    
    // MARK: - Private Methods
    
    private fun loadOrCreateKey() {
        secretKey = if (keyFile.exists()) {
            loadKey()
        } else {
            createAndSaveKey()
        }
    }
    
    private fun createAndSaveKey(): SecretKey {
        val keyGen = KeyGenerator.getInstance("AES")
        keyGen.init(256)
        val key = keyGen.generateKey()
        
        // Sauvegarder la clé (en production, utiliser un KeyStore)
        keyFile.writeBytes(Base64.getEncoder().encode(key.encoded))
        
        return key
    }
    
    private fun loadKey(): SecretKey {
        val encoded = Base64.getDecoder().decode(keyFile.readBytes())
        return SecretKeySpec(encoded, 0, encoded.size, "AES", null)
    }
    
    private fun encryptData(data: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        return cipher.doFinal(data)
    }
    
    private fun decryptData(data: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES")
        cipher.init(Cipher.DECRYPT_MODE, secretKey)
        return cipher.doFinal(data)
    }
}
