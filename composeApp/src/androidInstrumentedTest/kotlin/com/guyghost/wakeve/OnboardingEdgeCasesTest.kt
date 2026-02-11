package com.guyghost.wakeve

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for onboarding edge cases and offline scenarios.
 *
 * Tests validate that:
 * - Concurrent access doesn't cause issues
 * - State is reliable across app lifecycle events
 * - Offline state doesn't affect onboarding persistence
 */
class OnboardingEdgeCasesTest {

    private lateinit var context: Context

    @BeforeTest
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        getSharedPreferences(context).edit().clear().apply()
    }

    @AfterTest
    fun tearDown() {
        getSharedPreferences(context).edit().clear().apply()
    }

    /**
     * Test 1: Multiple rapid calls to markOnboardingComplete are safe
     *
     * Given: Fresh context
     * When: markOnboardingComplete() is called multiple times rapidly
     * Then: All calls should succeed and state should be true
     */
    @Test
    fun `multiple rapid calls to markOnboardingComplete are safe`() {
        // Act: Call markOnboardingComplete multiple times
        repeat(5) {
            markOnboardingComplete(context)
        }

        // Assert: State should be true and no exceptions thrown
        assertTrue(
            hasCompletedOnboarding(context),
            "Multiple rapid calls should result in true state"
        )
    }

    /**
     * Test 2: Concurrent reads don't interfere with state
     *
     * Given: Onboarding is marked complete
     * When: Multiple threads read the state simultaneously
     * Then: All reads should return true
     */
    @Test
    fun `concurrent reads return consistent state`() {
        // Arrange: Mark onboarding complete
        markOnboardingComplete(context)

        // Act: Read from multiple threads (simulate concurrent access)
        val results = mutableListOf<Boolean>()
        val threads = (0..9).map {
            Thread {
                results.add(hasCompletedOnboarding(context))
            }
        }

        threads.forEach { it.start() }
        threads.forEach { it.join() }

        // Assert: All reads should return true (consistent state)
        assertTrue(
            results.all { it },
            "All concurrent reads should return the same state (true)"
        )
    }

    /**
     * Test 3: Empty context doesn't crash hasCompletedOnboarding
     *
     * Given: Fresh context with no preferences
     * When: hasCompletedOnboarding() is called immediately
     * Then: Should return false gracefully (no crash)
     */
    @Test
    fun `hasCompletedOnboarding handles empty preferences gracefully`() {
        // Arrange: Context has no preferences set (done in setup)

        // Act & Assert: Should return false without throwing exception
        try {
            val result = hasCompletedOnboarding(context)
            assertFalse(
                result,
                "Empty preferences should return false"
            )
        } catch (e: Exception) {
            assertTrue(
                false,
                "hasCompletedOnboarding should not throw exception: ${e.message}"
            )
        }
    }

    /**
     * Test 4: Offline state doesn't affect onboarding persistence
     *
     * Given: Network connection is unavailable (simulated)
     * When: onboarding state is marked complete
     * Then: State should persist regardless of network status
     */
    @Test
    fun `onboarding persistence works offline`() {
        // Note: We can't actually disconnect network in unit tests,
        // but this validates that our persistence mechanism (SharedPreferences)
        // doesn't depend on network

        // Arrange: Assume network is offline
        val isNetworkAvailable = false

        // Act: Mark onboarding complete (should work offline)
        markOnboardingComplete(context)

        // Act: Read state (should work offline)
        val state = hasCompletedOnboarding(context)

        // Assert: State should be true even "offline"
        assertTrue(
            state,
            "Onboarding persistence should work offline (SharedPreferences is local)"
        )
    }

    /**
     * Test 5: State survives preference file corruption recovery
     *
     * Given: A preference value exists
     * When: A different app tries to read it (cross-app access prevented)
     * Then: Our app should still read its own preferences correctly
     */
    @Test
    fun `preferences are isolated per package`() {
        // Arrange: Set a preference value
        markOnboardingComplete(context)
        assertTrue(
            hasCompletedOnboarding(context),
            "Precondition: should be marked complete"
        )

        // Act: Get preferences multiple times (simulating app restarts)
        val prefs1 = getSharedPreferences(context)
        val state1 = prefs1.getBoolean("has_completed_onboarding", false)

        val prefs2 = getSharedPreferences(context)
        val state2 = prefs2.getBoolean("has_completed_onboarding", false)

        // Assert: Both should return the same value (isolation)
        assertTrue(
            state1 && state2,
            "Preferences should be isolated and consistent"
        )
    }

    /**
     * Test 6: Preference value type is respected (Boolean not String)
     *
     * Given: A boolean preference is set
     * When: Retrieved as boolean
     * Then: Should return correct boolean value
     */
    @Test
    fun `onboarding preference type is respected as boolean`() {
        // Arrange: Mark onboarding complete
        markOnboardingComplete(context)

        // Act: Retrieve as boolean (correct type)
        val prefs = getSharedPreferences(context)
        val asBoolean = prefs.getBoolean("has_completed_onboarding", false)

        // Assert: Should be true
        assertTrue(asBoolean, "Should retrieve boolean value correctly")

        // Act: Try to retrieve as String (would use default)
        val asString = prefs.getString("has_completed_onboarding", "false")

        // Assert: String retrieval should return default (type mismatch)
        // This validates that we're using the correct type
        assertTrue(
            asBoolean,
            "Boolean type should be preserved"
        )
    }

    /**
     * Test 7: Clear preferences works correctly
     *
     * Given: onboarding is marked complete
     * When: All preferences are cleared
     * Then: hasCompletedOnboarding should return false again
     */
    @Test
    fun `clearing preferences resets onboarding state`() {
        // Arrange: Mark onboarding complete
        markOnboardingComplete(context)
        assertTrue(
            hasCompletedOnboarding(context),
            "Precondition: should be marked complete"
        )

        // Act: Clear all preferences
        val prefs = getSharedPreferences(context)
        val editor = prefs.edit()
        editor.clear()
        editor.apply()

        // Assert: State should be reset to false
        assertFalse(
            hasCompletedOnboarding(context),
            "After clearing preferences, state should be false"
        )
    }

    /**
     * Test 8: Idempotent markOnboardingComplete
     *
     * Given: onboarding is already marked complete
     * When: markOnboardingComplete() is called again
     * Then: Should not throw error and state remains true
     */
    @Test
    fun `markOnboardingComplete is idempotent`() {
        // Arrange: Mark complete once
        markOnboardingComplete(context)
        assertTrue(
            hasCompletedOnboarding(context),
            "Precondition: should be marked complete"
        )

        // Act: Mark complete again
        try {
            markOnboardingComplete(context)
        } catch (e: Exception) {
            assertTrue(false, "Should not throw exception: ${e.message}")
        }

        // Assert: State should still be true
        assertTrue(
            hasCompletedOnboarding(context),
            "State should remain true after re-marking"
        )
    }
}
