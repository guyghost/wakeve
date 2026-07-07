package com.guyghost.wakeve.ui.event

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.guyghost.wakeve.PollResultsScreen
import com.guyghost.wakeve.preview.PreviewTheme

@Preview(name = "Poll results - phone", widthDp = 393, heightDp = 852, showBackground = true)
@Composable
private fun PollResultsPhonePreview() {
    PreviewTheme {
        PollResultsScreen(
            state = previewPollResultsState(),
            onSlotSelected = {},
            onConfirmFinalDate = {},
            onBack = {}
        )
    }
}

@Preview(name = "Poll results - tablet", widthDp = 1024, heightDp = 768, showBackground = true)
@Composable
private fun PollResultsTabletPreview() {
    PreviewTheme {
        PollResultsScreen(
            state = previewPollResultsState(),
            onSlotSelected = {},
            onConfirmFinalDate = {},
            onBack = {}
        )
    }
}

private fun previewPollResultsState(): PollResultsUiState {
    val slots = listOf(
        PollSlotResultUiState(
            slotId = "slot-1",
            startLabel = "2026-07-14T09:00:00Z",
            endLabel = "2026-07-14T18:00:00Z",
            totalScore = 5,
            yesCount = 2,
            maybeCount = 1,
            noCount = 0,
            isSelected = true
        ),
        PollSlotResultUiState(
            slotId = "slot-2",
            startLabel = "2026-07-15T09:00:00Z",
            endLabel = "2026-07-15T18:00:00Z",
            totalScore = 0,
            yesCount = 1,
            maybeCount = 0,
            noCount = 2,
            isSelected = false
        )
    )

    return PollResultsUiState(
        eventId = "preview-event",
        eventTitle = "Lisbon team retreat",
        isOrganizer = true,
        selectedSlotId = "slot-1",
        isConfirming = false,
        hasConfirmed = false,
        errorMessage = null,
        recommendedSlot = slots.first(),
        slots = slots
    )
}
