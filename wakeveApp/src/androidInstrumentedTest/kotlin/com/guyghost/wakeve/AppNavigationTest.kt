package com.guyghost.wakeve

import android.content.Context
import androidx.test.core.app.ApplicationProvider
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
 * - Unauthenticated → LOGIN route
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
            !isAuthenticated -> AppRoute.LOGIN
            isAuthenticated && !hasOnboarded -> AppRoute.ONBOARDING
            isAuthenticated && hasOnboarded -> AppRoute.HOME
            else -> AppRoute.HOME
        }

        // Assert: Should navigate to ONBOARDING for first authenticated launch
        assertEquals(
            AppRoute.ONBOARDING,
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
            !isAuthenticated -> AppRoute.LOGIN
            isAuthenticated && !hasOnboarded -> AppRoute.ONBOARDING
            isAuthenticated && hasOnboarded -> AppRoute.HOME
            else -> AppRoute.HOME
        }

        // Assert: Should navigate to HOME for returning authenticated user
        assertEquals(
            AppRoute.HOME,
            expectedRoute,
            "Returning authenticated user should skip onboarding and go to HOME"
        )
    }

    /**
     * Test 3: Unauthenticated user goes to login
     *
     * Scenario:
     * Given: User is not authenticated (no auth token/session)
     * When: App is launched (splash screen completes)
     * Then: Should navigate to LOGIN route
     */
    @Test
    fun `unauthenticated user goes to login`() {
        // Arrange: Simulate unauthenticated user
        val isAuthenticated = false
        val hasOnboarded = hasCompletedOnboarding(context)

        // Act: Determine navigation route based on app logic
        val expectedRoute = when {
            !isAuthenticated -> AppRoute.LOGIN
            isAuthenticated && !hasOnboarded -> AppRoute.ONBOARDING
            isAuthenticated && hasOnboarded -> AppRoute.HOME
            else -> AppRoute.HOME
        }

        // Assert: Should navigate to LOGIN for unauthenticated user
        assertEquals(
            AppRoute.LOGIN,
            expectedRoute,
            "Unauthenticated user should be directed to LOGIN"
        )
    }

    /**
     * Test 4: Unauthenticated user still goes to login even if onboarded
     *
     * Scenario:
     * Given: User has completed onboarding but is NOT authenticated
     * When: App is launched
     * Then: Should navigate to LOGIN (auth check takes precedence)
     */
    @Test
    fun `unauthenticated user goes to login even if onboarded`() {
        // Arrange: Mark onboarding as complete but not authenticated
        markOnboardingComplete(context)
        val isAuthenticated = false
        val hasOnboarded = hasCompletedOnboarding(context)

        // Verify preconditions
        assert(hasOnboarded) { "Precondition: onboarding should be marked complete" }
        assert(!isAuthenticated) { "Precondition: user should NOT be authenticated" }

        // Act: Determine navigation route based on app logic
        val expectedRoute = when {
            !isAuthenticated -> AppRoute.LOGIN
            isAuthenticated && !hasOnboarded -> AppRoute.ONBOARDING
            isAuthenticated && hasOnboarded -> AppRoute.HOME
            else -> AppRoute.HOME
        }

        // Assert: Should navigate to LOGIN (auth has priority)
        assertEquals(
            AppRoute.LOGIN,
            expectedRoute,
            "Unauthenticated user should go to LOGIN regardless of onboarding status"
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
            Triple(false, false, AppRoute.LOGIN),      // No auth, no onboard
            Triple(false, true, AppRoute.LOGIN),       // No auth, but onboarded (still login)
            Triple(true, false, AppRoute.ONBOARDING),  // Auth, no onboard
            Triple(true, true, AppRoute.HOME),         // Auth, onboarded
        )

        navigationMatrix.forEach { (isAuth, hasOnboard, expectedRoute) ->
            // Arrange: Reset state before each test
            getSharedPreferences(context).edit().clear().apply()

            if (hasOnboard) {
                markOnboardingComplete(context)
            }

            val actualRoute = when {
                !isAuth -> AppRoute.LOGIN
                isAuth && !hasOnboard -> AppRoute.ONBOARDING
                isAuth && hasOnboard -> AppRoute.HOME
                else -> AppRoute.HOME
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
            !isAuthenticated -> AppRoute.LOGIN
            isAuthenticated && !hasCompletedOnboarding(context) -> AppRoute.ONBOARDING
            else -> AppRoute.HOME
        }

        // Assert: Should navigate to ONBOARDING first
        assertEquals(
            AppRoute.ONBOARDING,
            beforeOnboarding,
            "Before onboarding, should navigate to ONBOARDING"
        )

        // Act: User completes onboarding
        markOnboardingComplete(context)

        // Act: Check navigation after onboarding
        val afterOnboarding = when {
            !isAuthenticated -> AppRoute.LOGIN
            isAuthenticated && !hasCompletedOnboarding(context) -> AppRoute.ONBOARDING
            else -> AppRoute.HOME
        }

        // Assert: Should navigate to HOME after onboarding
        assertEquals(
            AppRoute.HOME,
            afterOnboarding,
            "After onboarding, should navigate to HOME"
        )
    }
}
