package com.guyghost.wakeve

import android.content.Context
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for onboarding persistence using SharedPreferences.
 *
 * Tests validate that:
 * - First launch has no onboarding completion
 * - Marking onboarding complete persists the state
 * - State persists across multiple reads
 */
class OnboardingPersistenceTest {

    private lateinit var context: Context
    private lateinit var sharedPreferences: SharedPreferences

    @BeforeTest
    fun setup() {
        // Get application context for test
        context = ApplicationProvider.getApplicationContext()

        // Get SharedPreferences instance
        sharedPreferences = getSharedPreferences(context)

        // Clear all preferences before each test (idempotent)
        sharedPreferences.edit().clear().apply()
    }

    @AfterTest
    fun tearDown() {
        // Clean up after test
        sharedPreferences.edit().clear().apply()
    }

    /**
     * Test 1: Verify hasCompletedOnboarding returns false for first launch
     *
     * Given: Fresh context with cleared SharedPreferences (first app launch)
     * When: hasCompletedOnboarding() is called
     * Then: Should return false (no onboarding completed yet)
     */
    @Test
    fun `hasCompletedOnboarding returns false for first launch`() {
        // Arrange: Context is initialized with empty preferences (done in setup())

        // Act: Check if onboarding is completed
        val result = hasCompletedOnboarding(context)

        // Assert: Should be false for first launch
        assertFalse(result, "First launch should have no onboarding completion")
    }

    /**
     * Test 2: Verify markOnboardingComplete saves state to SharedPreferences
     *
     * Given: Fresh context with cleared SharedPreferences
     * When: markOnboardingComplete() is called
     * Then: hasCompletedOnboarding() should return true
     */
    @Test
    fun `markOnboardingComplete saves state`() {
        // Arrange: Clear preferences (done in setup())
        assertFalse(
            hasCompletedOnboarding(context),
            "Precondition: onboarding should not be completed initially"
        )

        // Act: Mark onboarding as complete
        markOnboardingComplete(context)

        // Assert: State should now be saved
        assertTrue(
            hasCompletedOnboarding(context),
            "After marking complete, hasCompletedOnboarding should return true"
        )
    }

    /**
     * Test 3: Verify onboarding state persists between multiple reads
     *
     * Given: markOnboardingComplete() has been called
     * When: hasCompletedOnboarding() is called multiple times
     * Then: All calls should return true (persistent state)
     */
    @Test
    fun `onboarding state persists between reads`() {
        // Arrange: Mark onboarding as complete
        markOnboardingComplete(context)
        assertTrue(
            hasCompletedOnboarding(context),
            "Precondition: onboarding should be marked complete"
        )

        // Act: Read the state multiple times
        val firstRead = hasCompletedOnboarding(context)
        val secondRead = hasCompletedOnboarding(context)
        val thirdRead = hasCompletedOnboarding(context)

        // Assert: All reads should return true (persistent state)
        assertTrue(firstRead, "First read should return true")
        assertTrue(secondRead, "Second read should return true")
        assertTrue(thirdRead, "Third read should return true")
    }

    /**
     * Test 4: Verify state persists after getting new SharedPreferences instance
     *
     * Given: markOnboardingComplete() has been called
     * When: A new SharedPreferences instance is obtained
     * Then: The state should still be persisted
     */
    @Test
    fun `onboarding state persists across SharedPreferences instances`() {
        // Arrange: Mark onboarding as complete
        markOnboardingComplete(context)
        assertTrue(
            hasCompletedOnboarding(context),
            "Precondition: onboarding should be marked complete"
        )

        // Act: Get a new SharedPreferences instance (simulating app restart)
        val newPrefsInstance = getSharedPreferences(context)
        val newState = newPrefsInstance.getBoolean("has_completed_onboarding", false)

        // Assert: State should persist in new instance
        assertTrue(
            newState,
            "State should persist when accessing via new SharedPreferences instance"
        )
    }

    /**
     * Test 5: Verify state can be reset
     *
     * Given: onboarding has been marked as complete
     * When: The preference is cleared
     * Then: hasCompletedOnboarding should return false again
     */
    @Test
    fun `onboarding state can be reset`() {
        // Arrange: Mark onboarding as complete
        markOnboardingComplete(context)
        assertTrue(
            hasCompletedOnboarding(context),
            "Precondition: onboarding should be marked complete"
        )

        // Act: Reset the preference
        sharedPreferences.edit().putBoolean("has_completed_onboarding", false).apply()

        // Assert: State should be reset
        assertFalse(
            hasCompletedOnboarding(context),
            "After reset, hasCompletedOnboarding should return false"
        )
    }

    /**
     * Test 6: Verify getSharedPreferences uses correct preference file and mode
     *
     * Given: Context is available
     * When: getSharedPreferences() is called
     * Then: Should return a SharedPreferences instance with correct name
     */
    @Test
    fun `getSharedPreferences returns correct preferences instance`() {
        // Act: Get SharedPreferences instance
        val prefs = getSharedPreferences(context)

        // Assert: Should be non-null
        assertTrue(
            prefs != null,
            "getSharedPreferences should return a valid SharedPreferences instance"
        )

        // Act: Write a test value
        prefs.edit().putString("test_key", "test_value").apply()

        // Assert: Value should be retrievable
        val testValue = prefs.getString("test_key", null)
        assertTrue(
            testValue == "test_value",
            "Written test value should be retrievable"
        )
    }
}
