package com.guyghost.wakeve.auth.shell.services

/**
 * In-memory implementation of TokenStorage for testing.
 * Stores all tokens in a simple map, suitable for unit and integration tests.
 */
class InMemoryTokenStorage : TokenStorage {
    private val storage = mutableMapOf<String, String>()

    override suspend fun storeString(key: String, value: String) {
        storage[key] = value
    }

    override suspend fun getString(key: String): String? {
        return storage[key]
    }

    override suspend fun remove(key: String) {
        storage.remove(key)
    }

    override suspend fun contains(key: String): Boolean {
        return storage.containsKey(key)
    }

    override suspend fun clearAll() {
        storage.clear()
    }
}
