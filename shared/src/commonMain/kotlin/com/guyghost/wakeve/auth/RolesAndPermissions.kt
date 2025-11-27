package com.guyghost.wakeve.auth

/**
 * System roles for Role-Based Access Control (RBAC).
 *
 * Roles define broad categories of users with predefined sets of permissions.
 */
enum class UserRole {
    /**
     * Regular user - can create and manage their own events.
     */
    USER,

    /**
     * Event organizer - additional permissions for event management.
     */
    ORGANIZER,

    /**
     * Moderator - can moderate content and manage users.
     */
    MODERATOR,

    /**
     * Administrator - full system access.
     */
    ADMIN;

    companion object {
        /**
         * Get default role for new users.
         */
        fun default() = USER

        /**
         * Parse role from string (case-insensitive).
         */
        fun fromString(role: String): UserRole? {
            return entries.find { it.name.equals(role, ignoreCase = true) }
        }
    }
}

/**
 * Fine-grained permissions for specific actions.
 *
 * Permissions are checked against user's roles to determine authorization.
 */
enum class Permission {
    // Event permissions
    EVENT_CREATE,
    EVENT_READ,
    EVENT_UPDATE_OWN,
    EVENT_UPDATE_ANY,
    EVENT_DELETE_OWN,
    EVENT_DELETE_ANY,

    // Participant permissions
    PARTICIPANT_INVITE,
    PARTICIPANT_REMOVE_OWN,
    PARTICIPANT_REMOVE_ANY,

    // Vote permissions
    VOTE_CREATE,
    VOTE_UPDATE_OWN,
    VOTE_UPDATE_ANY,
    VOTE_DELETE_OWN,
    VOTE_DELETE_ANY,

    // User management permissions
    USER_READ,
    USER_UPDATE_OWN,
    USER_UPDATE_ANY,
    USER_DELETE_OWN,
    USER_DELETE_ANY,
    USER_BAN,

    // Session management
    SESSION_READ_OWN,
    SESSION_READ_ANY,
    SESSION_REVOKE_OWN,
    SESSION_REVOKE_ANY,

    // Admin permissions
    SYSTEM_SETTINGS,
    SYSTEM_METRICS,
    SYSTEM_LOGS;

    companion object {
        /**
         * Parse permission from string (case-insensitive).
         */
        fun fromString(permission: String): Permission? {
            return entries.find { it.name.equals(permission, ignoreCase = true) }
        }
    }
}

/**
 * Role-based permission mappings.
 *
 * Defines which permissions are granted to each role.
 */
object RolePermissions {
    /**
     * Get all permissions for a given role.
     */
    fun getPermissions(role: UserRole): Set<Permission> {
        return when (role) {
            UserRole.USER -> userPermissions
            UserRole.ORGANIZER -> organizerPermissions
            UserRole.MODERATOR -> moderatorPermissions
            UserRole.ADMIN -> adminPermissions
        }
    }

    /**
     * Check if a role has a specific permission.
     */
    fun hasPermission(role: UserRole, permission: Permission): Boolean {
        return getPermissions(role).contains(permission)
    }

    /**
     * Check if any of the given roles has a specific permission.
     */
    fun hasPermission(roles: Set<UserRole>, permission: Permission): Boolean {
        return roles.any { hasPermission(it, permission) }
    }

    /**
     * Permissions for regular users.
     */
    private val userPermissions = setOf(
        // Events
        Permission.EVENT_CREATE,
        Permission.EVENT_READ,
        Permission.EVENT_UPDATE_OWN,
        Permission.EVENT_DELETE_OWN,

        // Participants
        Permission.PARTICIPANT_INVITE,
        Permission.PARTICIPANT_REMOVE_OWN,

        // Votes
        Permission.VOTE_CREATE,
        Permission.VOTE_UPDATE_OWN,
        Permission.VOTE_DELETE_OWN,

        // Own user management
        Permission.USER_READ,
        Permission.USER_UPDATE_OWN,
        Permission.USER_DELETE_OWN,

        // Own sessions
        Permission.SESSION_READ_OWN,
        Permission.SESSION_REVOKE_OWN
    )

    /**
     * Permissions for organizers (includes all user permissions).
     */
    private val organizerPermissions = userPermissions + setOf(
        Permission.EVENT_UPDATE_ANY,  // Can update any event they organize
        Permission.PARTICIPANT_REMOVE_ANY  // Can remove participants from events they organize
    )

    /**
     * Permissions for moderators (includes all organizer permissions).
     */
    private val moderatorPermissions = organizerPermissions + setOf(
        Permission.EVENT_DELETE_ANY,
        Permission.VOTE_UPDATE_ANY,
        Permission.VOTE_DELETE_ANY,
        Permission.USER_READ,
        Permission.USER_BAN,
        Permission.SESSION_READ_ANY,
        Permission.SESSION_REVOKE_ANY
    )

    /**
     * Permissions for administrators (full access).
     */
    private val adminPermissions = Permission.entries.toSet()
}

/**
 * JWT claims for roles and permissions.
 */
object RBACClaims {
    const val ROLES = "roles"
    const val PERMISSIONS = "permissions"
}
