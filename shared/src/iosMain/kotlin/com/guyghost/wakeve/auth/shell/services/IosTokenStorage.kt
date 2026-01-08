package com.guyghost.wakeve.auth.shell.services

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.Foundation.CFDataCreate
import platform.Foundation.CFDataGetBytePtr
import platform.Foundation.CFDataGetLength
import platform.Foundation.CFStringCreateWithCString
import platform.Security.*

/**
 * iOS implementation of TokenStorage using iOS Keychain for secure storage.
 */
class IosTokenStorage : TokenStorage {

    private val service = "com.guyghost.wakeve.auth"

    override suspend fun storeString(key: String, value: String) {
        withContext(Dispatchers.IO) {
            try {
                memScoped {
                    val query = CFDictionaryCreateMutable(null, 0, null, null)
                    CFDictionarySetValue(query, kSecClass, kSecClassGenericPassword)
                    CFDictionarySetValue(
                        query,
                        kSecAttrService,
                        CFStringCreateWithCString(null, service, kCFStringEncodingUTF8)
                    )
                    CFDictionarySetValue(
                        query,
                        kSecAttrAccount,
                        CFStringCreateWithCString(null, key, kCFStringEncodingUTF8)
                    )

                    // Delete existing entry first
                    SecItemDelete(query)

                    // Add new entry
                    val valueData = value.encodeToByteArray().toCFData()
                    CFDictionarySetValue(query, kSecValueData, valueData)
                    CFDictionarySetValue(query, kSecAttrAccessible, kSecAttrAccessibleAfterFirstUnlock)

                    SecItemAdd(query, null)
                }
            } catch (e: Exception) {
                // Log error in production, but don't throw to maintain interface contract
            }
        }
    }

    override suspend fun getString(key: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                memScoped {
                    val query = CFDictionaryCreateMutable(null, 0, null, null)
                    CFDictionarySetValue(query, kSecClass, kSecClassGenericPassword)
                    CFDictionarySetValue(
                        query,
                        kSecAttrService,
                        CFStringCreateWithCString(null, service, kCFStringEncodingUTF8)
                    )
                    CFDictionarySetValue(
                        query,
                        kSecAttrAccount,
                        CFStringCreateWithCString(null, key, kCFStringEncodingUTF8)
                    )
                    CFDictionarySetValue(query, kSecReturnData, kCFBooleanTrue)
                    CFDictionarySetValue(query, kSecMatchLimit, kSecMatchLimitOne)

                    var result: CFTypeRef? = null
                    val status = SecItemCopyMatching(query, cValueOf(&result))

                    if (status == errSecSuccess && result != null) {
                        val data = result as CFDataRef
                        val bytes = CFDataGetBytePtr(data)
                        val length = CFDataGetLength(data)
                        val value = ByteArray(length.toInt()) { bytes!![it].toByte() }
                        value.decodeToString()
                    } else {
                        null
                    }
                }
            } catch (e: Exception) {
                // Log error in production, but don't throw to maintain interface contract
                null
            }
        }
    }

    override suspend fun remove(key: String) {
        withContext(Dispatchers.IO) {
            try {
                memScoped {
                    val query = CFDictionaryCreateMutable(null, 0, null, null)
                    CFDictionarySetValue(query, kSecClass, kSecClassGenericPassword)
                    CFDictionarySetValue(
                        query,
                        kSecAttrService,
                        CFStringCreateWithCString(null, service, kCFStringEncodingUTF8)
                    )
                    CFDictionarySetValue(
                        query,
                        kSecAttrAccount,
                        CFStringCreateWithCString(null, key, kCFStringEncodingUTF8)
                    )

                    SecItemDelete(query)
                }
            } catch (e: Exception) {
                // Log error in production, but don't throw to maintain interface contract
            }
        }
    }

    override suspend fun contains(key: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                memScoped {
                    val query = CFDictionaryCreateMutable(null, 0, null, null)
                    CFDictionarySetValue(query, kSecClass, kSecClassGenericPassword)
                    CFDictionarySetValue(
                        query,
                        kSecAttrService,
                        CFStringCreateWithCString(null, service, kCFStringEncodingUTF8)
                    )
                    CFDictionarySetValue(
                        query,
                        kSecAttrAccount,
                        CFStringCreateWithCString(null, key, kCFStringEncodingUTF8)
                    )
                    CFDictionarySetValue(query, kSecReturnData, kCFBooleanFalse)
                    CFDictionarySetValue(query, kSecMatchLimit, kSecMatchLimitOne)

                    val status = SecItemCopyMatching(query, null)
                    status == errSecSuccess
                }
            } catch (e: Exception) {
                // Log error in production, but don't throw to maintain interface contract
                false
            }
        }
    }

    override suspend fun clearAll() {
        withContext(Dispatchers.IO) {
            try {
                memScoped {
                    val query = CFDictionaryCreateMutable(null, 0, null, null)
                    CFDictionarySetValue(query, kSecClass, kSecClassGenericPassword)
                    CFDictionarySetValue(
                        query,
                        kSecAttrService,
                        CFStringCreateWithCString(null, service, kCFStringEncodingUTF8)
                    )

                    SecItemDelete(query)
                }
            } catch (e: Exception) {
                // Log error in production, but don't throw to maintain interface contract
            }
        }
    }
}
