package com.guyghost.wakeve.presentation.usecase

import com.guyghost.wakeve.models.Event

/**
 * Result type for participant estimation.
 *
 * Represents calculated participant count estimates based on min/max/expected values.
 *
 * @property min Minimum participants (from minParticipants or calculated)
 * @property max Maximum participants (from maxParticipants or calculated)
 * @property expected Expected participants (from expectedParticipants or midpoint of min/max)
 */
data class ParticipantsEstimation(
    val min: Int,
    val max: Int,
    val expected: Int
)

/**
 * Use case for estimating participant counts.
 *
 * Calculates min/max/expected participant counts based on available data:
 * - If expectedParticipants is provided, use it (min = max = expected)
 * - If minParticipants and maxParticipants provided, calculate midpoint for expected
 * - If only minParticipants: expected = min
 * - If only maxParticipants: expected = max
 * - If none provided: expected = 0
 *
 * ## Usage
 *
 * ```kotlin
 * val estimateParticipants = EstimateParticipantsUseCase()
 * val estimation = estimateParticipants(event)
 * println("Expected: ${estimation.expected} (${estimation.min}-${estimation.max})")
 * ```
 */
class EstimateParticipantsUseCase {
    /**
     * Estimate participant counts for an event.
     *
     * @param event The event to estimate participants for
     * @return ParticipantsEstimation with min, max, and expected values
     */
    operator fun invoke(event: Event): ParticipantsEstimation {
        // Case 1: expectedParticipants is explicitly provided
        if (event.expectedParticipants != null) {
            return ParticipantsEstimation(
                min = event.expectedParticipants,
                max = event.expectedParticipants,
                expected = event.expectedParticipants
            )
        }

        // Case 2: minParticipants and maxParticipants are provided
        if (event.minParticipants != null && event.maxParticipants != null) {
            val midpoint = (event.minParticipants + event.maxParticipants) / 2
            return ParticipantsEstimation(
                min = event.minParticipants,
                max = event.maxParticipants,
                expected = midpoint
            )
        }

        // Case 3: Only minParticipants
        if (event.minParticipants != null) {
            return ParticipantsEstimation(
                min = event.minParticipants,
                max = event.minParticipants,
                expected = event.minParticipants
            )
        }

        // Case 4: Only maxParticipants
        if (event.maxParticipants != null) {
            return ParticipantsEstimation(
                min = event.maxParticipants,
                max = event.maxParticipants,
                expected = event.maxParticipants
            )
        }

        // Case 5: No participant data provided (default)
        return ParticipantsEstimation(
            min = 0,
            max = 0,
            expected = 0
        )
    }
}
