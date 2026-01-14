package com.guyghost.wakeve.auth.shell.services

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.cinterop.*
import platform.CoreFoundation.*
import platform.Foundation.*
import platform.Security.*
import kotlin.experimental.ExperimentalNativeApi

/**
 * iOS implementation of TokenStorage using iOS Keychain for secure storage.
 */
@OptIn(ExperimentalForeignApi::class, ExperimentalNativeApi::class)
class IosTokenStorage : TokenStorage {

    private val service = "com.guyghost.wakeve.auth"

    override suspend fun storeString(key: String, value: String) {
        withContext(Dispatchers.Default) {
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
                    val valueData = value.utf8ToCFData()
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
        return withContext(Dispatchers.Default) {
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

                    val result = alloc<CFTypeRefVar>()
                    val status = SecItemCopyMatching(query, result.ptr)

                    if (status == errSecSuccess && result.value != null) {
                        val data = result.value as CFDataRef
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
        withContext(Dispatchers.Default) {
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
        return withContext(Dispatchers.Default) {
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
        withContext(Dispatchers.Default) {
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

@OptIn(ExperimentalForeignApi::class)
private fun String.utf8ToCFData(): CFDataRef? {
    val bytes = this.encodeToByteArray()
    val uBytes = bytes.map { it.toUByte() }.toUByteArray()
    return uBytes.usePinned { pinned ->
        CFDataCreate(
            allocator = null,
            bytes = pinned.addressOf(0),
            length = uBytes.size.convert()
        )
    }
}
