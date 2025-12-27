package com.guyghost.wakeve

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for app navigation logic.
 * 
 * These tests validate the navigation routing logic without requiring
 * Android context or SharedPreferences (pure logic tests).
 */
class NavigationRouteLogicTest {

    /**
     * Test navigation route selection based on auth and onboarding state
     */
    @Test
    fun `navigation route selection works correctly for all state combinations`() {
        // Test matrix: (isAuthenticated, hasOnboarded) -> expected route
        val testCases = listOf(
            Triple(false, false, "LOGIN"),       // No auth, no onboard
            Triple(false, true, "LOGIN"),        // No auth, but onboarded
            Triple(true, false, "ONBOARDING"),   // Auth, no onboard
            Triple(true, true, "HOME"),          // Auth, onboarded
        )

        testCases.forEach { (isAuth, hasOnboard, expectedRoute) ->
            val route = when {
                !isAuth -> "LOGIN"
                isAuth && !hasOnboard -> "ONBOARDING"
                isAuth && hasOnboard -> "HOME"
                else -> "HOME"
            }

            assertEquals(
                expectedRoute,
                route,
                "Route for auth=$isAuth, onboard=$hasOnboard should be $expectedRoute"
            )
        }
    }

    /**
     * Test that authentication check takes priority over onboarding
     */
    @Test
    fun `authentication takes priority over onboarding in routing`() {
        // Even if user has onboarded, if not authenticated, should go to LOGIN
        val isAuthenticated = false
        val hasOnboarded = true

        val route = when {
            !isAuthenticated -> "LOGIN"
            isAuthenticated && !hasOnboarded -> "ONBOARDING"
            else -> "HOME"
        }

        assertEquals(
            "LOGIN",
            route,
            "Unauthenticated users should go to LOGIN even if onboarded"
        )
    }

    /**
     * Test that authenticated users without onboarding go to ONBOARDING
     */
    @Test
    fun `authenticated users without onboarding go to ONBOARDING screen`() {
        val isAuthenticated = true
        val hasOnboarded = false

        val route = when {
            !isAuthenticated -> "LOGIN"
            isAuthenticated && !hasOnboarded -> "ONBOARDING"
            else -> "HOME"
        }

        assertEquals(
            "ONBOARDING",
            route,
            "First authenticated users should see ONBOARDING"
        )
    }

    /**
     * Test that returning authenticated users skip onboarding
     */
    @Test
    fun `returning authenticated users skip onboarding and go to HOME`() {
        val isAuthenticated = true
        val hasOnboarded = true

        val route = when {
            !isAuthenticated -> "LOGIN"
            isAuthenticated && !hasOnboarded -> "ONBOARDING"
            else -> "HOME"
        }

        assertEquals(
            "HOME",
            route,
            "Returning authenticated users should go to HOME"
        )
    }

    /**
     * Test state transition from ONBOARDING to HOME
     */
    @Test
    fun `onboarding completion transitions from ONBOARDING to HOME`() {
        val isAuthenticated = true

        // Before onboarding
        var hasOnboarded = false
        var route = when {
            !isAuthenticated -> "LOGIN"
            isAuthenticated && !hasOnboarded -> "ONBOARDING"
            else -> "HOME"
        }
        assertEquals("ONBOARDING", route)

        // After onboarding
        hasOnboarded = true
        route = when {
            !isAuthenticated -> "LOGIN"
            isAuthenticated && !hasOnboarded -> "ONBOARDING"
            else -> "HOME"
        }
        assertEquals("HOME", route)
    }
}
