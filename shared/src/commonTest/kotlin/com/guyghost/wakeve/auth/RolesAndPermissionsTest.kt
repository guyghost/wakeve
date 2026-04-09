package com.guyghost.wakeve.auth

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for RBAC (Role-Based Access Control) system.
 *
 * Tests UserRole, Permission, and RolePermissions including:
 * - Role hierarchy and permission inheritance
 * - Permission parsing and validation
 * - Edge cases for permission checks
 */
class RolesAndPermissionsTest {

    // ========================================================================
    // UserRole Tests
    // ========================================================================

    @Test
    fun `UserRole has 4 roles`() {
        assertEquals(4, UserRole.entries.size)
    }

    @Test
    fun `default role is USER`() {
        assertEquals(UserRole.USER, UserRole.default())
    }

    @Test
    fun `fromString parses roles case-insensitively`() {
        assertEquals(UserRole.USER, UserRole.fromString("USER"))
        assertEquals(UserRole.USER, UserRole.fromString("user"))
        assertEquals(UserRole.USER, UserRole.fromString("User"))
        assertEquals(UserRole.ORGANIZER, UserRole.fromString("organizer"))
        assertEquals(UserRole.MODERATOR, UserRole.fromString("MODERATOR"))
        assertEquals(UserRole.ADMIN, UserRole.fromString("admin"))
    }

    @Test
    fun `fromString returns null for invalid role`() {
        assertNull(UserRole.fromString("UNKNOWN"))
        assertNull(UserRole.fromString(""))
        assertNull(UserRole.fromString("superuser"))
    }

    // ========================================================================
    // Permission Tests
    // ========================================================================

    @Test
    fun `Permission has expected count`() {
        assertTrue(Permission.entries.size >= 20, "Should have at least 20 permissions")
    }

    @Test
    fun `fromString parses permissions case-insensitively`() {
        assertEquals(Permission.EVENT_CREATE, Permission.fromString("EVENT_CREATE"))
        assertEquals(Permission.EVENT_CREATE, Permission.fromString("event_create"))
        assertEquals(Permission.SYSTEM_SETTINGS, Permission.fromString("system_settings"))
        assertNull(Permission.fromString("NONEXISTENT"))
    }

    @Test
    fun `core event permissions exist`() {
        val eventPerms = listOf(
            Permission.EVENT_CREATE,
            Permission.EVENT_READ,
            Permission.EVENT_UPDATE_OWN,
            Permission.EVENT_UPDATE_ANY,
            Permission.EVENT_DELETE_OWN,
            Permission.EVENT_DELETE_ANY
        )
        assertEquals(6, eventPerms.size)
        eventPerms.forEach { perm ->
            assertNotNull(Permission.fromString(perm.name))
        }
    }

    @Test
    fun `core vote permissions exist`() {
        val votePerms = listOf(
            Permission.VOTE_CREATE,
            Permission.VOTE_UPDATE_OWN,
            Permission.VOTE_UPDATE_ANY,
            Permission.VOTE_DELETE_OWN,
            Permission.VOTE_DELETE_ANY
        )
        assertEquals(5, votePerms.size)
    }

    @Test
    fun `session permissions exist`() {
        val sessionPerms = listOf(
            Permission.SESSION_READ_OWN,
            Permission.SESSION_READ_ANY,
            Permission.SESSION_REVOKE_OWN,
            Permission.SESSION_REVOKE_ANY
        )
        assertEquals(4, sessionPerms.size)
    }

    @Test
    fun `admin-only permissions exist`() {
        val adminPerms = listOf(
            Permission.SYSTEM_SETTINGS,
            Permission.SYSTEM_METRICS,
            Permission.SYSTEM_LOGS
        )
        assertEquals(3, adminPerms.size)
    }

    // ========================================================================
    // RolePermissions - Permission Inheritance Tests
    // ========================================================================

    @Test
    fun `USER has basic event permissions`() {
        val perms = RolePermissions.getPermissions(UserRole.USER)
        assertTrue(perms.contains(Permission.EVENT_CREATE))
        assertTrue(perms.contains(Permission.EVENT_READ))
        assertTrue(perms.contains(Permission.EVENT_UPDATE_OWN))
        assertTrue(perms.contains(Permission.EVENT_DELETE_OWN))
    }

    @Test
    fun `USER cannot update any event`() {
        val perms = RolePermissions.getPermissions(UserRole.USER)
        assertFalse(perms.contains(Permission.EVENT_UPDATE_ANY))
        assertFalse(perms.contains(Permission.EVENT_DELETE_ANY))
    }

    @Test
    fun `USER has vote permissions`() {
        val perms = RolePermissions.getPermissions(UserRole.USER)
        assertTrue(perms.contains(Permission.VOTE_CREATE))
        assertTrue(perms.contains(Permission.VOTE_UPDATE_OWN))
        assertTrue(perms.contains(Permission.VOTE_DELETE_OWN))
    }

    @Test
    fun `USER has own session permissions`() {
        val perms = RolePermissions.getPermissions(UserRole.USER)
        assertTrue(perms.contains(Permission.SESSION_READ_OWN))
        assertTrue(perms.contains(Permission.SESSION_REVOKE_OWN))
        assertFalse(perms.contains(Permission.SESSION_READ_ANY))
        assertFalse(perms.contains(Permission.SESSION_REVOKE_ANY))
    }

    @Test
    fun `ORGANIZER inherits USER permissions`() {
        val userPerms = RolePermissions.getPermissions(UserRole.USER)
        val orgPerms = RolePermissions.getPermissions(UserRole.ORGANIZER)
        assertTrue(orgPerms.containsAll(userPerms),
            "ORGANIZER should have all USER permissions")
    }

    @Test
    fun `ORGANIZER can update and remove any participant`() {
        val perms = RolePermissions.getPermissions(UserRole.ORGANIZER)
        assertTrue(perms.contains(Permission.EVENT_UPDATE_ANY))
        assertTrue(perms.contains(Permission.PARTICIPANT_REMOVE_ANY))
    }

    @Test
    fun `MODERATOR inherits ORGANIZER permissions`() {
        val orgPerms = RolePermissions.getPermissions(UserRole.ORGANIZER)
        val modPerms = RolePermissions.getPermissions(UserRole.MODERATOR)
        assertTrue(modPerms.containsAll(orgPerms),
            "MODERATOR should have all ORGANIZER permissions")
    }

    @Test
    fun `MODERATOR can delete any event and ban users`() {
        val perms = RolePermissions.getPermissions(UserRole.MODERATOR)
        assertTrue(perms.contains(Permission.EVENT_DELETE_ANY))
        assertTrue(perms.contains(Permission.USER_BAN))
        assertTrue(perms.contains(Permission.SESSION_READ_ANY))
        assertTrue(perms.contains(Permission.SESSION_REVOKE_ANY))
    }

    @Test
    fun `ADMIN has all permissions`() {
        val adminPerms = RolePermissions.getPermissions(UserRole.ADMIN)
        assertEquals(Permission.entries.toSet(), adminPerms,
            "ADMIN should have ALL permissions")
    }

    @Test
    fun `ADMIN has system permissions`() {
        val perms = RolePermissions.getPermissions(UserRole.ADMIN)
        assertTrue(perms.contains(Permission.SYSTEM_SETTINGS))
        assertTrue(perms.contains(Permission.SYSTEM_METRICS))
        assertTrue(perms.contains(Permission.SYSTEM_LOGS))
    }

    @Test
    fun `only ADMIN has system permissions`() {
        assertFalse(RolePermissions.hasPermission(UserRole.USER, Permission.SYSTEM_SETTINGS))
        assertFalse(RolePermissions.hasPermission(UserRole.ORGANIZER, Permission.SYSTEM_SETTINGS))
        assertFalse(RolePermissions.hasPermission(UserRole.MODERATOR, Permission.SYSTEM_SETTINGS))
        assertTrue(RolePermissions.hasPermission(UserRole.ADMIN, Permission.SYSTEM_SETTINGS))
    }

    // ========================================================================
    // RolePermissions - hasPermission Tests
    // ========================================================================

    @Test
    fun `hasPermission works for single role`() {
        assertTrue(RolePermissions.hasPermission(UserRole.USER, Permission.EVENT_CREATE))
        assertFalse(RolePermissions.hasPermission(UserRole.USER, Permission.EVENT_DELETE_ANY))
    }

    @Test
    fun `hasPermission with role set checks any role`() {
        val roles = setOf(UserRole.USER, UserRole.MODERATOR)
        assertTrue(RolePermissions.hasPermission(roles, Permission.EVENT_DELETE_ANY),
            "MODERATOR can delete any event")
        assertTrue(RolePermissions.hasPermission(roles, Permission.EVENT_CREATE),
            "USER can create events")
    }

    @Test
    fun `hasPermission with empty role set returns false`() {
        assertFalse(RolePermissions.hasPermission(emptySet(), Permission.EVENT_READ))
    }

    // ========================================================================
    // Permission Hierarchy Size Tests
    // ========================================================================

    @Test
    fun `permission count increases with role hierarchy`() {
        val userCount = RolePermissions.getPermissions(UserRole.USER).size
        val orgCount = RolePermissions.getPermissions(UserRole.ORGANIZER).size
        val modCount = RolePermissions.getPermissions(UserRole.MODERATOR).size
        val adminCount = RolePermissions.getPermissions(UserRole.ADMIN).size

        assertTrue(userCount > 0, "USER should have permissions")
        assertTrue(orgCount > userCount, "ORGANIZER should have more permissions than USER")
        assertTrue(modCount > orgCount, "MODERATOR should have more than ORGANIZER")
        assertEquals(Permission.entries.size, adminCount, "ADMIN has all permissions")
    }

    // ========================================================================
    // RBACClaims Tests
    // ========================================================================

    @Test
    fun `RBACClaims has correct constant values`() {
        assertEquals("roles", RBACClaims.ROLES)
        assertEquals("permissions", RBACClaims.PERMISSIONS)
    }
}
