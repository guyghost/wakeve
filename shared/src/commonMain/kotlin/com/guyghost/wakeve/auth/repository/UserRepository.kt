package com.guyghost.wakeve.auth.repository

import com.guyghost.wakeve.auth.core.models.AuthMethod
import com.guyghost.wakeve.auth.core.models.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository interface for User data persistence.
 *
 * This interface defines the contract for user data operations,
 * supporting both authenticated users and guest sessions.
 *
 * GDPR Compliance:
 * - All operations respect data minimization principles
 * - User data is only stored with explicit consent
 * - Delete operations remove all personal data
 */
interface UserRepository {

    /**
     * Retrieves a user by their unique ID.
     *
     * @param id The user's unique identifier
     * @return The user if found, null otherwise
     */
    suspend fun getUserById(id: String): User?

    /**
     * Retrieves a user by their email address.
     *
     * @param email The user's email address
     * @return The user if found, null otherwise
     */
    suspend fun getUserByEmail(email: String): User?

    /**
     * Inserts a new user into the database.
     *
     * @param user The user to insert
     */
    suspend fun insertUser(user: User)

    /**
     * Updates an existing user's information.
     *
     * @param user The user with updated information
     */
    suspend fun updateUser(user: User)

    /**
     * Deletes a user and all associated data.
     *
     * GDPR Requirement: Implements right to be forgotten.
     *
     * @param id The user's unique identifier
     */
    suspend fun deleteUser(id: String)

    /**
     * Retrieves all users (admin only).
     *
     * @return List of all users in the database
     */
    suspend fun getAllUsers(): List<User>

    /**
     * Searches for users by name (partial match).
     *
     * @param namePart Partial name to search for
     * @return List of matching users
     */
    suspend fun searchUsersByName(namePart: String): List<User>
}

/**
 * In-memory implementation of UserRepository for testing and development.
 *
 * This implementation stores users in a mutable map,
 * suitable for unit tests, development, and offline support.
 */
class InMemoryUserRepository : UserRepository {

    private val users = mutableMapOf<String, User>()

    override suspend fun getUserById(id: String): User? = withContext(Dispatchers.Default) {
        users[id]
    }

    override suspend fun getUserByEmail(email: String): User? = withContext(Dispatchers.Default) {
        users.values.find { it.email == email }
    }

    override suspend fun insertUser(user: User) {
        withContext(Dispatchers.Default) {
            users[user.id] = user
        }
    }

    override suspend fun updateUser(user: User) {
        withContext(Dispatchers.Default) {
            users[user.id] = user
        }
    }

    override suspend fun deleteUser(id: String) {
        withContext(Dispatchers.Default) {
            users.remove(id)
        }
    }

    override suspend fun getAllUsers(): List<User> = withContext(Dispatchers.Default) {
        users.values.toList()
    }

    override suspend fun searchUsersByName(namePart: String): List<User> = withContext(Dispatchers.Default) {
        users.values
            .filter { it.name?.contains(namePart, ignoreCase = true) == true }
            .toList()
    }
}
