package com.guyghost.wakeve.ui.components

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.guyghost.wakeve.models.LocationType
import com.guyghost.wakeve.models.PotentialLocation
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Compose tests for PotentialLocationsList component.
 */
class PotentialLocationsListTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @Test
    fun locationsList_showsEmptyStateWhenNoLocations() {
        // When
        composeTestRule.setContent {
            PotentialLocationsList(
                locations = emptyList(),
                onAddLocation = {},
                onRemoveLocation = {}
            )
        }
        
        // Then
        composeTestRule
            .onNodeWithText("No locations yet")
            .assertExists()
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("Add potential venues, cities, or regions")
            .assertExists()
    }
    
    @Test
    fun locationsList_displaysTitle() {
        // When
        composeTestRule.setContent {
            PotentialLocationsList(
                locations = emptyList(),
                onAddLocation = {},
                onRemoveLocation = {}
            )
        }
        
        // Then
        composeTestRule
            .onNodeWithText("Potential Locations")
            .assertExists()
            .assertIsDisplayed()
    }
    
    @Test
    fun locationsList_showsAddButton() {
        // When
        composeTestRule.setContent {
            PotentialLocationsList(
                locations = emptyList(),
                onAddLocation = {},
                onRemoveLocation = {}
            )
        }
        
        // Then
        composeTestRule
            .onNodeWithText("Add")
            .assertExists()
            .assertIsDisplayed()
    }
    
    @Test
    fun locationsList_callsAddCallbackWhenButtonClicked() {
        // Given
        var addCalled = false
        
        // When
        composeTestRule.setContent {
            PotentialLocationsList(
                locations = emptyList(),
                onAddLocation = { addCalled = true },
                onRemoveLocation = {}
            )
        }
        
        composeTestRule
            .onNodeWithText("Add")
            .performClick()
        
        // Then
        composeTestRule.waitForIdle()
        assertTrue(addCalled)
    }
    
    @Test
    fun locationsList_displaysLocationWhenProvided() {
        // Given
        val location = PotentialLocation(
            id = "loc-1",
            eventId = "event-1",
            name = "Paris",
            locationType = LocationType.CITY,
            address = null,
            coordinates = null,
            createdAt = "2025-12-31T10:00:00Z"
        )
        
        // When
        composeTestRule.setContent {
            PotentialLocationsList(
                locations = listOf(location),
                onAddLocation = {},
                onRemoveLocation = {}
            )
        }
        
        // Then
        composeTestRule
            .onNodeWithText("Paris")
            .assertExists()
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("City")
            .assertExists()
    }
    
    @Test
    fun locationsList_displaysMultipleLocations() {
        // Given
        val locations = listOf(
            PotentialLocation(
                id = "loc-1",
                eventId = "event-1",
                name = "Paris",
                locationType = LocationType.CITY,
                address = null,
                coordinates = null,
                createdAt = "2025-12-31T10:00:00Z"
            ),
            PotentialLocation(
                id = "loc-2",
                eventId = "event-1",
                name = "Hotel Royal",
                locationType = LocationType.SPECIFIC_VENUE,
                address = "123 Main St",
                coordinates = null,
                createdAt = "2025-12-31T10:01:00Z"
            ),
            PotentialLocation(
                id = "loc-3",
                eventId = "event-1",
                name = "Zoom Meeting",
                locationType = LocationType.ONLINE,
                address = null,
                coordinates = null,
                createdAt = "2025-12-31T10:02:00Z"
            )
        )
        
        // When
        composeTestRule.setContent {
            PotentialLocationsList(
                locations = locations,
                onAddLocation = {},
                onRemoveLocation = {}
            )
        }
        
        // Then
        composeTestRule.onNodeWithText("Paris").assertExists()
        composeTestRule.onNodeWithText("Hotel Royal").assertExists()
        composeTestRule.onNodeWithText("Zoom Meeting").assertExists()
        composeTestRule.onNodeWithText("City").assertExists()
        composeTestRule.onNodeWithText("Venue").assertExists()
        composeTestRule.onNodeWithText("Online").assertExists()
    }
    
    @Test
    fun locationsList_showsCountBadgeWhenLocationsExist() {
        // Given
        val locations = listOf(
            PotentialLocation(
                id = "loc-1",
                eventId = "event-1",
                name = "Paris",
                locationType = LocationType.CITY,
                address = null,
                coordinates = null,
                createdAt = "2025-12-31T10:00:00Z"
            ),
            PotentialLocation(
                id = "loc-2",
                eventId = "event-1",
                name = "London",
                locationType = LocationType.CITY,
                address = null,
                coordinates = null,
                createdAt = "2025-12-31T10:01:00Z"
            )
        )
        
        // When
        composeTestRule.setContent {
            PotentialLocationsList(
                locations = locations,
                onAddLocation = {},
                onRemoveLocation = {}
            )
        }
        
        // Then - badge should show "2"
        composeTestRule
            .onNodeWithText("2")
            .assertExists()
            .assertIsDisplayed()
    }
    
    @Test
    fun locationsList_displaysAddressWhenProvided() {
        // Given
        val location = PotentialLocation(
            id = "loc-1",
            eventId = "event-1",
            name = "Hotel Royal",
            locationType = LocationType.SPECIFIC_VENUE,
            address = "123 Main St, Paris",
            coordinates = null,
            createdAt = "2025-12-31T10:00:00Z"
        )
        
        // When
        composeTestRule.setContent {
            PotentialLocationsList(
                locations = listOf(location),
                onAddLocation = {},
                onRemoveLocation = {}
            )
        }
        
        // Then
        composeTestRule
            .onNodeWithText("123 Main St, Paris")
            .assertExists()
    }
    
    @Test
    fun locationsList_callsRemoveCallbackWhenDeleteClicked() {
        // Given
        var removedId: String? = null
        val location = PotentialLocation(
            id = "loc-1",
            eventId = "event-1",
            name = "Paris",
            locationType = LocationType.CITY,
            address = null,
            coordinates = null,
            createdAt = "2025-12-31T10:00:00Z"
        )
        
        // When
        composeTestRule.setContent {
            PotentialLocationsList(
                locations = listOf(location),
                onAddLocation = {},
                onRemoveLocation = { removedId = it }
            )
        }
        
        // Click delete button
        composeTestRule
            .onNodeWithContentDescription("Remove location")
            .performClick()
        
        // Then
        composeTestRule.waitForIdle()
        assertEquals("loc-1", removedId)
    }
    
    @Test
    fun locationsList_disabledWhenEnabledFalse() {
        // When
        composeTestRule.setContent {
            PotentialLocationsList(
                locations = emptyList(),
                onAddLocation = {},
                onRemoveLocation = {},
                enabled = false
            )
        }
        
        // Then
        composeTestRule
            .onNodeWithText("Add")
            .assertIsNotEnabled()
    }
    
    @Test
    fun locationsList_showsCorrectIconForEachLocationType() {
        // Given
        val locations = listOf(
            PotentialLocation(
                id = "loc-1",
                eventId = "event-1",
                name = "Paris",
                locationType = LocationType.CITY,
                address = null,
                coordinates = null,
                createdAt = "2025-12-31T10:00:00Z"
            ),
            PotentialLocation(
                id = "loc-2",
                eventId = "event-1",
                name = "South of France",
                locationType = LocationType.REGION,
                address = null,
                coordinates = null,
                createdAt = "2025-12-31T10:01:00Z"
            )
        )
        
        // When
        composeTestRule.setContent {
            PotentialLocationsList(
                locations = locations,
                onAddLocation = {},
                onRemoveLocation = {}
            )
        }
        
        // Then - type labels should be displayed
        composeTestRule.onNodeWithText("City").assertExists()
        composeTestRule.onNodeWithText("Region").assertExists()
    }
}
