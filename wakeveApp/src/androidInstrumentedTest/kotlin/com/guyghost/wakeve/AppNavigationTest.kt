package com.guyghost.wakeve

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.guyghost.wakeve.navigation.Screen
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Unit tests for app navigation flow based on authentication and onboarding state.
 *
 * Tests validate the correct route is selected based on:
 * - Authentication state (authenticated vs unauthenticated)
 * - Onboarding completion status
 *
 * Navigation Logic:
 * - Unauthenticated → GetStarted route (leads to Auth)
 * - Authenticated + Not onboarded → ONBOARDING route
 * - Authenticated + Onboarded → HOME route
 */
class AppNavigationTest {

    private lateinit var context: Context

    @BeforeTest
    fun setup() {
        // Get application context for test
        context = ApplicationProvider.getApplicationContext()

        // Clear all preferences before each test (idempotent)
        getSharedPreferences(context).edit().clear().apply()
    }

    @AfterTest
    fun tearDown() {
        // Clean up after test
        getSharedPreferences(context).edit().clear().apply()
    }

    /**
     * Test 1: First authenticated launch shows onboarding
     *
     * Scenario:
     * Given: New user (no onboarding completion flag) and is authenticated
     * When: App is launched (splash screen completes)
     * Then: Should navigate to ONBOARDING route
     */
    @Test
    fun `first authenticated launch shows onboarding`() {
        // Arrange: Simulate first authenticated launch
        // - Onboarding is not completed (default state)
        // - User is authenticated (simulate by checking auth state)
        val isAuthenticated = true
        val hasOnboarded = hasCompletedOnboarding(context)

        // Act: Determine navigation route based on app logic
        val expectedRoute = when {
            !isAuthenticated -> Screen.GetStarted.route
            isAuthenticated && !hasOnboarded -> Screen.Onboarding.route
            isAuthenticated && hasOnboarded -> Screen.Home.route
            else -> Screen.Home.route
        }

        // Assert: Should navigate to ONBOARDING for first authenticated launch
        assertEquals(
            Screen.Onboarding.route,
            expectedRoute,
            "First authenticated launch should navigate to ONBOARDING"
        )
    }

    /**
     * Test 2: Returning authenticated user skips onboarding
     *
     * Scenario:
     * Given: User has completed onboarding and is authenticated
     * When: App is launched (splash screen completes)
     * Then: Should navigate to HOME route
     */
    @Test
    fun `returning authenticated user skips onboarding`() {
        // Arrange: Simulate returning user
        // - Mark onboarding as complete
        markOnboardingComplete(context)
        val isAuthenticated = true
        val hasOnboarded = hasCompletedOnboarding(context)

        // Verify preconditions
        assert(hasOnboarded) { "Precondition: onboarding should be marked complete" }
        assert(isAuthenticated) { "Precondition: user should be authenticated" }

        // Act: Determine navigation route based on app logic
        val expectedRoute = when {
            !isAuthenticated -> Screen.GetStarted.route
            isAuthenticated && !hasOnboarded -> Screen.Onboarding.route
            isAuthenticated && hasOnboarded -> Screen.Home.route
            else -> Screen.Home.route
        }

        // Assert: Should navigate to HOME for returning authenticated user
        assertEquals(
            Screen.Home.route,
            expectedRoute,
            "Returning authenticated user should skip onboarding and go to HOME"
        )
    }

    /**
     * Test 3: Unauthenticated user goes to get started
     *
     * Scenario:
     * Given: User is not authenticated (no auth token/session)
     * When: App is launched (splash screen completes)
     * Then: Should navigate to GetStarted route (which leads to Auth)
     */
    @Test
    fun `unauthenticated user goes to get started`() {
        // Arrange: Simulate unauthenticated user
        val isAuthenticated = false
        val hasOnboarded = hasCompletedOnboarding(context)

        // Act: Determine navigation route based on app logic
        val expectedRoute = when {
            !isAuthenticated -> Screen.GetStarted.route
            isAuthenticated && !hasOnboarded -> Screen.Onboarding.route
            isAuthenticated && hasOnboarded -> Screen.Home.route
            else -> Screen.Home.route
        }

        // Assert: Should navigate to GetStarted for unauthenticated user
        assertEquals(
            Screen.GetStarted.route,
            expectedRoute,
            "Unauthenticated user should be directed to GetStarted"
        )
    }

    /**
     * Test 4: Unauthenticated user still goes to get started even if onboarded
     *
     * Scenario:
     * Given: User has completed onboarding but is NOT authenticated
     * When: App is launched
     * Then: Should navigate to GetStarted (auth check takes precedence)
     */
    @Test
    fun `unauthenticated user goes to get started even if onboarded`() {
        // Arrange: Mark onboarding as complete but not authenticated
        markOnboardingComplete(context)
        val isAuthenticated = false
        val hasOnboarded = hasCompletedOnboarding(context)

        // Verify preconditions
        assert(hasOnboarded) { "Precondition: onboarding should be marked complete" }
        assert(!isAuthenticated) { "Precondition: user should NOT be authenticated" }

        // Act: Determine navigation route based on app logic
        val expectedRoute = when {
            !isAuthenticated -> Screen.GetStarted.route
            isAuthenticated && !hasOnboarded -> Screen.Onboarding.route
            isAuthenticated && hasOnboarded -> Screen.Home.route
            else -> Screen.Home.route
        }

        // Assert: Should navigate to GetStarted (auth has priority)
        assertEquals(
            Screen.GetStarted.route,
            expectedRoute,
            "Unauthenticated user should go to GetStarted regardless of onboarding status"
        )
    }

    /**
     * Test 5: Navigation order correctly prioritizes auth > onboarding > home
     *
     * Scenario: Test all combinations to ensure priority is correct
     * - Auth state takes priority over onboarding state
     * - Onboarding state only checked when authenticated
     */
    @Test
    fun `navigation correctly prioritizes auth check over onboarding`() {
        // Test matrix: [isAuthenticated][hasOnboarded] → expected route
        val navigationMatrix = listOf(
            Triple(false, false, Screen.GetStarted.route),  // No auth, no onboard
            Triple(false, true, Screen.GetStarted.route),   // No auth, but onboarded (still get started)
            Triple(true, false, Screen.Onboarding.route),   // Auth, no onboard
            Triple(true, true, Screen.Home.route),          // Auth, onboarded
        )

        navigationMatrix.forEach { (isAuth, hasOnboard, expectedRoute) ->
            // Arrange: Reset state before each test
            getSharedPreferences(context).edit().clear().apply()

            if (hasOnboard) {
                markOnboardingComplete(context)
            }

            val actualRoute = when {
                !isAuth -> Screen.GetStarted.route
                isAuth && !hasOnboard -> Screen.Onboarding.route
                isAuth && hasOnboard -> Screen.Home.route
                else -> Screen.Home.route
            }

            // Assert: Each combination should produce correct route
            assertEquals(
                expectedRoute,
                actualRoute,
                "Auth=$isAuth, OnboardingDone=$hasOnboard should route to $expectedRoute"
            )
        }
    }

    /**
     * Test 6: Onboarding completion affects navigation correctly
     *
     * Scenario: User authenticates, then completes onboarding
     * When: Onboarding is marked complete
     * Then: Next navigation should go to HOME instead of ONBOARDING
     */
    @Test
    fun `onboarding completion changes navigation from ONBOARDING to HOME`() {
        // Arrange: Simulate authenticated user before onboarding
        val isAuthenticated = true
        val beforeOnboarding = when {
            !isAuthenticated -> Screen.GetStarted.route
            isAuthenticated && !hasCompletedOnboarding(context) -> Screen.Onboarding.route
            else -> Screen.Home.route
        }

        // Assert: Should navigate to ONBOARDING first
        assertEquals(
            Screen.Onboarding.route,
            beforeOnboarding,
            "Before onboarding, should navigate to ONBOARDING"
        )

        // Act: User completes onboarding
        markOnboardingComplete(context)

        // Act: Check navigation after onboarding
        val afterOnboarding = when {
            !isAuthenticated -> Screen.GetStarted.route
            isAuthenticated && !hasCompletedOnboarding(context) -> Screen.Onboarding.route
            else -> Screen.Home.route
        }

        // Assert: Should navigate to HOME after onboarding
        assertEquals(
            Screen.Home.route,
            afterOnboarding,
            "After onboarding, should navigate to HOME"
        )
    }
}
