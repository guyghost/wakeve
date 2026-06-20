package com.guyghost.wakeve.destination

import com.guyghost.wakeve.models.SuggestionSeason
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFailsWith

class LodgingProviderTest {
    @Test
    fun noConfiguredLodgingProviderDoesNotFabricateDestinations() = runTest {
        assertFailsWith<IllegalStateException> {
            NoConfiguredLodgingProvider.fetchDestinations(
                from = listOf("Paris"),
                season = SuggestionSeason.SUMMER,
                groupSize = 4,
                duration = 3
            )
        }
    }

    @Test
    fun legacyMockLodgingProviderDoesNotFabricateAccommodations() = runTest {
        assertFailsWith<IllegalStateException> {
            @Suppress("DEPRECATION")
            MockLodgingProvider().fetchAccommodations(
                destination = "Paris",
                groupSize = 4,
                duration = 3
            )
        }
    }
}
