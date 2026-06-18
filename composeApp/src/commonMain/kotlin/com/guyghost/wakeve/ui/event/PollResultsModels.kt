package com.guyghost.wakeve.ui.event

import com.guyghost.wakeve.models.Event
import com.guyghost.wakeve.models.Poll
import com.guyghost.wakeve.models.TimeSlot
import com.guyghost.wakeve.poll.PollLogic

data class PollResultsUiState(
    val eventId: String,
    val eventTitle: String,
    val isOrganizer: Boolean,
    val selectedSlotId: String?,
    val isConfirming: Boolean,
    val hasConfirmed: Boolean,
    val errorMessage: String?,
    val recommendedSlot: PollSlotResultUiState?,
    val slots: List<PollSlotResultUiState>
) {
    val canConfirm: Boolean
        get() = isOrganizer && selectedSlotId != null && !isConfirming && !hasConfirmed
}

data class PollSlotResultUiState(
    val slotId: String,
    val startLabel: String,
    val endLabel: String,
    val totalScore: Int,
    val yesCount: Int,
    val maybeCount: Int,
    val noCount: Int,
    val isSelected: Boolean
)

fun Event.toPollResultsUiState(
    poll: Poll?,
    isOrganizer: Boolean,
    selectedSlotId: String?,
    isConfirming: Boolean = false,
    hasConfirmed: Boolean = false,
    errorMessage: String? = null
): PollResultsUiState {
    val scores = poll?.let { PollLogic.getSlotScores(it, proposedSlots) }.orEmpty()
    val bestSlotId = poll?.let { PollLogic.getBestSlotWithScore(it, proposedSlots)?.first?.id }
    val slotResults = scores.mapNotNull { score ->
        val slot = proposedSlots.firstOrNull { it.id == score.slotId } ?: return@mapNotNull null
        slot.toPollSlotResultUiState(score, selectedSlotId)
    }

    return PollResultsUiState(
        eventId = id,
        eventTitle = title,
        isOrganizer = isOrganizer,
        selectedSlotId = selectedSlotId,
        isConfirming = isConfirming,
        hasConfirmed = hasConfirmed,
        errorMessage = errorMessage,
        recommendedSlot = slotResults.firstOrNull { it.slotId == bestSlotId },
        slots = slotResults
    )
}

private fun TimeSlot.toPollSlotResultUiState(
    score: PollLogic.SlotScore,
    selectedSlotId: String?
): PollSlotResultUiState =
    PollSlotResultUiState(
        slotId = id,
        startLabel = start ?: "Start time not set",
        endLabel = end ?: "End time not set",
        totalScore = score.totalScore,
        yesCount = score.yesCount,
        maybeCount = score.maybeCount,
        noCount = score.noCount,
        isSelected = id == selectedSlotId
    )
