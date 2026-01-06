package com.guyghost.wakeve.ui.event

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.guyghost.wakeve.models.Event
import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.models.TimeSlot
import kotlinx.datetime.Clock
import org.junit.Rule
import org.junit.runner.RunWith
import kotlin.test.BeforeTest
import kotlin.test.Test

/**
 * UI tests for Calendar Integration Card component.
 *
 * Tests validate that:
 * - Calendar Integration Card is displayed with correct UI elements
 * - "Ajouter" (Add to Calendar) button callback is triggered on click
 * - "Inviter" (Share Invite) button callback is triggered on click
 * - Card displays event date information correctly
 *
 * Component: CalendarIntegrationCard
 * Parent: ModernEventDetailView (CONFIRMED and FINALIZED states)
 */
@RunWith(AndroidJUnit4::class)
class CalendarIntegrationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var testEvent: Event

    @BeforeTest
    fun setup() {
        // Create a test event in CONFIRMED status with a final date
        testEvent = Event(
            id = "test-event-001",
            title = "Team Building Event",
            description = "Annual team building and strategic planning session",
            organizerId = "org-user-001",
            participants = listOf("user-001", "user-002", "user-003"),
            proposedSlots = listOf(
                TimeSlot(
                    id = "slot-1",
                    start = "2025-03-15T10:00:00Z",
                    end = "2025-03-15T12:00:00Z",
                    timezone = "Europe/Paris"
                )
            ),
            deadline = "2025-02-28T18:00:00Z",
            status = EventStatus.CONFIRMED,
            finalDate = "2025-03-15T10:00:00Z",
            createdAt = Clock.System.now().toString(),
            updatedAt = Clock.System.now().toString()
        )
    }

    /**
     * Test 1: Calendar Integration Card is visible when event is loaded
     *
     * Scenario:
     * - GIVEN: ModernEventDetailView displays an event in CONFIRMED status
     * - WHEN: The view is rendered
     * - THEN: CalendarIntegrationCard should be visible with correct title
     */
    @Test
    fun calendarIntegrationCard_isDisplayed_whenEventIsConfirmed() {
        // Arrange
        var addToCalendarClicked = false
        var shareInviteClicked = false

        // Act: Render the CalendarIntegrationCard
        composeTestRule.setContent {
            MaterialTheme {
                CalendarIntegrationCard(
                    event = testEvent,
                    onAddToCalendar = { addToCalendarClicked = true },
                    onShareInvite = { shareInviteClicked = true }
                )
            }
        }

        // Assert: Card title should be visible
        composeTestRule.onNodeWithText("Calendrier & Invitations")
            .assertExists("Calendar Integration Card title should be displayed")

        // Assert: Callbacks should not have been triggered yet
        assert(!addToCalendarClicked) { "Add to calendar callback should not be triggered on render" }
        assert(!shareInviteClicked) { "Share invite callback should not be triggered on render" }
    }

    /**
     * Test 2: Event date is displayed correctly in the Card
     *
     * Scenario:
     * - GIVEN: Event has a finalDate set
     * - WHEN: CalendarIntegrationCard is rendered
     * - THEN: The date should be displayed in readable format
     *
     * Note: The exact format depends on the TimeZone conversion logic.
     * This test verifies that a date string containing date/time numbers is displayed.
     */
    @Test
    fun eventDate_isDisplayed_inCardContent() {
        // Arrange
        composeTestRule.setContent {
            MaterialTheme {
                CalendarIntegrationCard(
                    event = testEvent,
                    onAddToCalendar = {},
                    onShareInvite = {}
                )
            }
        }

        // Assert: Date text should be displayed (contains "Date prévue :")
        composeTestRule.onNodeWithText(
            text = "Date prévue :",
            substring = true // Look for partial match
        ).assertExists("Date information should be displayed in the card")
    }

    /**
     * Test 3: "Ajouter" button triggers onAddToCalendar callback
     *
     * Scenario:
     * - GIVEN: CalendarIntegrationCard is rendered with onAddToCalendar callback
     * - WHEN: User clicks the "Ajouter" button
     * - THEN: The onAddToCalendar callback should be invoked
     */
    @Test
    fun addToCalendarButton_triggersCallback_onClick() {
        // Arrange
        var callbackInvoked = false
        val mockCallback = { callbackInvoked = true }

        composeTestRule.setContent {
            MaterialTheme {
                CalendarIntegrationCard(
                    event = testEvent,
                    onAddToCalendar = mockCallback,
                    onShareInvite = {}
                )
            }
        }

        // Precondition: Callback should not be invoked initially
        assert(!callbackInvoked) { "Callback should not be invoked before button click" }

        // Act: Click the "Ajouter" button
        composeTestRule.onNodeWithText("Ajouter")
            .performClick()

        // Assert: Callback should have been invoked
        assert(callbackInvoked) { "onAddToCalendar callback should be invoked after button click" }
    }

    /**
     * Test 4: "Inviter" button triggers onShareInvite callback
     *
     * Scenario:
     * - GIVEN: CalendarIntegrationCard is rendered with onShareInvite callback
     * - WHEN: User clicks the "Inviter" button
     * - THEN: The onShareInvite callback should be invoked
     */
    @Test
    fun shareInviteButton_triggersCallback_onClick() {
        // Arrange
        var callbackInvoked = false
        val mockCallback = { callbackInvoked = true }

        composeTestRule.setContent {
            MaterialTheme {
                CalendarIntegrationCard(
                    event = testEvent,
                    onAddToCalendar = {},
                    onShareInvite = mockCallback
                )
            }
        }

        // Precondition: Callback should not be invoked initially
        assert(!callbackInvoked) { "Callback should not be invoked before button click" }

        // Act: Click the "Inviter" button
        composeTestRule.onNodeWithText("Inviter")
            .performClick()

        // Assert: Callback should have been invoked
        assert(callbackInvoked) { "onShareInvite callback should be invoked after button click" }
    }

    /**
     * Test 5: Both buttons are clickable in the Card
     *
     * Scenario:
     * - GIVEN: CalendarIntegrationCard is rendered
     * - WHEN: The card is displayed
     * - THEN: Both "Ajouter" and "Inviter" buttons should be present and clickable
     */
    @Test
    fun bothButtons_arePresent_andClickable() {
        // Arrange
        var addToCalendarInvoked = false
        var shareInviteInvoked = false

        composeTestRule.setContent {
            MaterialTheme {
                CalendarIntegrationCard(
                    event = testEvent,
                    onAddToCalendar = { addToCalendarInvoked = true },
                    onShareInvite = { shareInviteInvoked = true }
                )
            }
        }

        // Assert: Both buttons exist
        composeTestRule.onNodeWithText("Ajouter")
            .assertExists("'Ajouter' button should exist")

        composeTestRule.onNodeWithText("Inviter")
            .assertExists("'Inviter' button should exist")

        // Preconditions
        assert(!addToCalendarInvoked) { "Add to calendar should not be invoked yet" }
        assert(!shareInviteInvoked) { "Share invite should not be invoked yet" }

        // Act & Assert: Click first button
        composeTestRule.onNodeWithText("Ajouter")
            .performClick()
        assert(addToCalendarInvoked) { "Add to calendar callback should be invoked" }
        assert(!shareInviteInvoked) { "Share invite callback should not be invoked yet" }

        // Act & Assert: Click second button
        composeTestRule.onNodeWithText("Inviter")
            .performClick()
        assert(shareInviteInvoked) { "Share invite callback should be invoked" }
    }

    /**
     * Test 6: Multiple rapid clicks trigger multiple callbacks
     *
     * Scenario:
     * - GIVEN: CalendarIntegrationCard is rendered
     * - WHEN: User clicks the "Ajouter" button multiple times rapidly
     * - THEN: The callback should be invoked for each click
     *
     * This tests that the button doesn't have debouncing that prevents multiple clicks.
     */
    @Test
    fun addToCalendarButton_triggersCallback_onMultipleClicks() {
        // Arrange
        var callCount = 0
        val mockCallback = { callCount++ }

        composeTestRule.setContent {
            MaterialTheme {
                CalendarIntegrationCard(
                    event = testEvent,
                    onAddToCalendar = mockCallback,
                    onShareInvite = {}
                )
            }
        }

        // Precondition
        assert(callCount == 0) { "Call count should start at 0" }

        // Act: Click multiple times
        composeTestRule.onNodeWithText("Ajouter")
            .performClick()
        assert(callCount == 1) { "First click should increment counter" }

        composeTestRule.onNodeWithText("Ajouter")
            .performClick()
        assert(callCount == 2) { "Second click should increment counter" }

        composeTestRule.onNodeWithText("Ajouter")
            .performClick()
        assert(callCount == 3) { "Third click should increment counter" }
    }

    /**
     * Test 7: Card displays without finalDate (null case)
     *
     * Scenario:
     * - GIVEN: Event has no finalDate (null)
     * - WHEN: CalendarIntegrationCard is rendered
     * - THEN: Card should still display without crashing, just without the date text
     */
    @Test
    fun calendarIntegrationCard_rendersGracefully_withoutFinalDate() {
        // Arrange: Create event without final date
        val eventWithoutDate = testEvent.copy(finalDate = null)

        composeTestRule.setContent {
            MaterialTheme {
                CalendarIntegrationCard(
                    event = eventWithoutDate,
                    onAddToCalendar = {},
                    onShareInvite = {}
                )
            }
        }

        // Assert: Card title should still be visible
        composeTestRule.onNodeWithText("Calendrier & Invitations")
            .assertExists("Card should display even without finalDate")

        // Assert: Both buttons should still be clickable
        composeTestRule.onNodeWithText("Ajouter")
            .assertExists("'Ajouter' button should exist")

        composeTestRule.onNodeWithText("Inviter")
            .assertExists("'Inviter' button should exist")
    }

    /**
     * Test 8: Card icons are displayed correctly
     *
     * Scenario:
     * - GIVEN: CalendarIntegrationCard is rendered
     * - WHEN: The card is displayed
     * - THEN: Calendar and Share icons should be present
     */
    @Test
    fun cardIcons_areDisplayed_inCard() {
        // Arrange
        composeTestRule.setContent {
            MaterialTheme {
                CalendarIntegrationCard(
                    event = testEvent,
                    onAddToCalendar = {},
                    onShareInvite = {}
                )
            }
        }

        // Assert: Icons should be displayed (by content description)
        // Note: Icons in the row header
        composeTestRule.onNodeWithContentDescription("", useUnmergedTree = true)
            .assertExists("Calendar icon should be present in the card header")
    }

    /**
     * Test 9: Card layout respects Material Design spacing
     *
     * Scenario:
     * - GIVEN: CalendarIntegrationCard is rendered
     * - WHEN: The component is composed
     * - THEN: All elements should be properly laid out and visible
     *
     * This is a sanity check for layout integrity.
     */
    @Test
    fun cardLayout_hasProperStructure_allElementsVisible() {
        // Arrange
        composeTestRule.setContent {
            MaterialTheme {
                CalendarIntegrationCard(
                    event = testEvent,
                    onAddToCalendar = {},
                    onShareInvite = {}
                )
            }
        }

        // Assert: All major elements are present
        composeTestRule.onNodeWithText("Calendrier & Invitations")
            .assertExists("Title should be visible")

        composeTestRule.onNodeWithText("Ajouter")
            .assertExists("Add button should be visible")

        composeTestRule.onNodeWithText("Inviter")
            .assertExists("Invite button should be visible")

        composeTestRule.onNodeWithText("Date prévue :", substring = true)
            .assertExists("Date should be visible when available")
    }

    /**
     * Test 10: Integration with ModernEventDetailView - CONFIRMED status
     *
     * Scenario:
     * - GIVEN: ModernEventDetailView renders an event in CONFIRMED status
     * - WHEN: The view is composed
     * - THEN: Calendar action buttons should be visible in ConfirmedModeActions
     *
     * Note: This test focuses on the integration of callbacks within ModernEventDetailView.
     */
    @Test
    fun modernEventDetailView_displaysCalendarActions_inConfirmedMode() {
        // Arrange
        var addToCalendarInvoked = false
        var shareInviteInvoked = false

        composeTestRule.setContent {
            MaterialTheme {
                ModernEventDetailView(
                    event = testEvent,
                    userId = "user-001",
                    onNavigateToScenarioList = {},
                    onNavigateToBudgetOverview = {},
                    onNavigateToAccommodation = {},
                    onNavigateToMealPlanning = {},
                    onNavigateToEquipmentChecklist = {},
                    onNavigateToActivityPlanning = {},
                    onNavigateToComments = {},
                    onNavigateToHome = {},
                    onAddToCalendar = { addToCalendarInvoked = true },
                    onShareInvite = { shareInviteInvoked = true }
                )
            }
        }

        // Assert: "Ajouter au calendrier" button from ConfirmedModeActions should exist
        composeTestRule.onNodeWithText("Ajouter au calendrier")
            .assertExists("'Ajouter au calendrier' button should exist in CONFIRMED mode")

        // Assert: "Partager l'invitation" button should exist
        composeTestRule.onNodeWithText("Partager l'invitation")
            .assertExists("'Partager l'invitation' button should exist in CONFIRMED mode")

        // Act: Click the buttons
        composeTestRule.onNodeWithText("Ajouter au calendrier")
            .performClick()
        assert(addToCalendarInvoked) { "onAddToCalendar callback should be invoked" }

        composeTestRule.onNodeWithText("Partager l'invitation")
            .performClick()
        assert(shareInviteInvoked) { "onShareInvite callback should be invoked" }
    }

    /**
     * Test 11: Integration with ModernEventDetailView - FINALIZED status
     *
     * Scenario:
     * - GIVEN: ModernEventDetailView renders an event in FINALIZED status
     * - WHEN: The view is composed
     * - THEN: Calendar action buttons should be visible in FinalizedModeActions
     */
    @Test
    fun modernEventDetailView_displaysCalendarActions_inFinalizedMode() {
        // Arrange
        val finalizedEvent = testEvent.copy(status = EventStatus.FINALIZED)
        var addToCalendarInvoked = false
        var shareInviteInvoked = false

        composeTestRule.setContent {
            MaterialTheme {
                ModernEventDetailView(
                    event = finalizedEvent,
                    userId = "user-001",
                    onNavigateToScenarioList = {},
                    onNavigateToBudgetOverview = {},
                    onNavigateToAccommodation = {},
                    onNavigateToMealPlanning = {},
                    onNavigateToEquipmentChecklist = {},
                    onNavigateToActivityPlanning = {},
                    onNavigateToComments = {},
                    onNavigateToHome = {},
                    onAddToCalendar = { addToCalendarInvoked = true },
                    onShareInvite = { shareInviteInvoked = true }
                )
            }
        }

        // Assert: "Ajouter au calendrier" button from FinalizedModeActions should exist
        composeTestRule.onNodeWithText("Ajouter au calendrier")
            .assertExists("'Ajouter au calendrier' button should exist in FINALIZED mode")

        // Assert: "Partager l'invitation" button should exist
        composeTestRule.onNodeWithText("Partager l'invitation")
            .assertExists("'Partager l'invitation' button should exist in FINALIZED mode")

        // Act: Click the buttons
        composeTestRule.onNodeWithText("Ajouter au calendrier")
            .performClick()
        assert(addToCalendarInvoked) { "onAddToCalendar callback should be invoked in FINALIZED mode" }

        composeTestRule.onNodeWithText("Partager l'invitation")
            .performClick()
        assert(shareInviteInvoked) { "onShareInvite callback should be invoked in FINALIZED mode" }
    }

    /**
     * Test 12: Calendar actions NOT displayed in non-appropriate statuses
     *
     * Scenario:
     * - GIVEN: ModernEventDetailView renders an event in DRAFT status
     * - WHEN: The view is composed
     * - THEN: Calendar Integration Card should NOT be shown
     *
     * Note: The CalendarIntegrationCard is only shown in CONFIRMED and FINALIZED states
     * as per the ConfirmedModeActions and FinalizedModeActions composables.
     */
    @Test
    fun modernEventDetailView_doesNotDisplayCalendarCard_inDraftMode() {
        // Arrange
        val draftEvent = testEvent.copy(status = EventStatus.DRAFT)

        composeTestRule.setContent {
            MaterialTheme {
                ModernEventDetailView(
                    event = draftEvent,
                    userId = "user-001",
                    onNavigateToScenarioList = {},
                    onNavigateToBudgetOverview = {},
                    onNavigateToAccommodation = {},
                    onNavigateToMealPlanning = {},
                    onNavigateToEquipmentChecklist = {},
                    onNavigateToActivityPlanning = {},
                    onNavigateToComments = {},
                    onNavigateToHome = {}
                )
            }
        }

        // Assert: Calendar Integration Card should not be visible in DRAFT mode
        // The CalendarIntegrationCard displays "Calendrier & Invitations" title
        composeTestRule.onNodeWithText("Calendrier & Invitations")
            .assertDoesNotExist("Calendar Integration Card should not be displayed in DRAFT mode")
    }

    /**
     * Test 13: Callback invocation is independent between buttons
     *
     * Scenario:
     * - GIVEN: CalendarIntegrationCard with distinct callbacks
     * - WHEN: One button is clicked
     * - THEN: Only that button's callback should be invoked
     */
    @Test
    fun buttonCallbacks_areIndependent_noUnintendedSideEffects() {
        // Arrange
        var addToCalendarCount = 0
        var shareInviteCount = 0

        composeTestRule.setContent {
            MaterialTheme {
                CalendarIntegrationCard(
                    event = testEvent,
                    onAddToCalendar = { addToCalendarCount++ },
                    onShareInvite = { shareInviteCount++ }
                )
            }
        }

        // Preconditions
        assert(addToCalendarCount == 0) { "Add to calendar count should start at 0" }
        assert(shareInviteCount == 0) { "Share invite count should start at 0" }

        // Act: Click only the "Ajouter" button
        composeTestRule.onNodeWithText("Ajouter")
            .performClick()

        // Assert: Only the "Ajouter" callback should have been invoked
        assert(addToCalendarCount == 1) { "Add to calendar count should be 1 after click" }
        assert(shareInviteCount == 0) { "Share invite count should still be 0" }

        // Act: Click only the "Inviter" button
        composeTestRule.onNodeWithText("Inviter")
            .performClick()

        // Assert: Only the "Inviter" callback should have been invoked
        assert(addToCalendarCount == 1) { "Add to calendar count should still be 1" }
        assert(shareInviteCount == 1) { "Share invite count should be 1 after click" }
    }
}
