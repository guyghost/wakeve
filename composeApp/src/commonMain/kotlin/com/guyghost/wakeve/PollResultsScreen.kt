package com.guyghost.wakeve

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun PollResultsScreen(
    event: Event,
    repository: EventRepository,
    onDateConfirmed: (String) -> Unit
) {
    val poll = repository.getPoll(event.id)
    val scores = remember {
        if (poll != null) {
            PollLogic.getSlotScores(poll, event.proposedSlots)
        } else {
            emptyList()
        }
    }
    val bestResult = remember {
        if (poll != null) {
            PollLogic.getBestSlotWithScore(poll, event.proposedSlots)
        } else {
            null
        }
    }

    var selectedSlotId by remember { mutableStateOf<String?>(bestResult?.first?.id) }
    var isConfirmed by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.background)
            .safeContentPadding()
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            "Poll Results",
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            "Event: ${event.title}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Best Slot Recommendation
        if (bestResult != null) {
            val (bestSlot, score) = bestResult
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Recommended Time Slot",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    ) {
                        Text(
                            bestSlot.start,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            "to",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            bestSlot.end,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    // Score Breakdown
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        ScoreIndicator("Yes", score.yesCount, 0xFF4CAF50.toInt()) // Green
                        ScoreIndicator("Maybe", score.maybeCount, 0xFFFFC107.toInt()) // Yellow
                        ScoreIndicator("No", score.noCount, 0xFFF44336.toInt()) // Red
                    }

                    Text(
                        "Score: ${score.totalScore}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
            }
        }

        // All Scores
        Text(
            "All Time Slots",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(scores) { score ->
                val slot = event.proposedSlots.find { it.id == score.slotId }
                if (slot != null) {
                    SlotResultCard(
                        slot = slot,
                        score = score,
                        isSelected = selectedSlotId == slot.id,
                        onSelect = { selectedSlotId = slot.id }
                    )
                }
            }
        }

        // Confirmation Section
        if (!isConfirmed && selectedSlotId != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Confirm selected time slot?",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        "Once confirmed, participants will be notified and the event will be finalized.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Action Button
        Button(
            onClick = {
                val selectedSlot = event.proposedSlots.find { it.id == selectedSlotId }
                if (selectedSlot != null) {
                    // Check if user is organizer
                    if (repository.isOrganizer(event.id, "organizer-1")) { // TODO: Get from auth
                        repository.updateEventStatus(
                            event.id,
                            EventStatus.CONFIRMED,
                            selectedSlot.start
                        )
                        isConfirmed = true
                        onDateConfirmed(event.id)
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            enabled = selectedSlotId != null && !isConfirmed
        ) {
            Text("Confirm Final Date", style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
fun RowScope.ScoreIndicator(label: String, count: Int, color: Int) {
    Column(
        modifier = Modifier.weight(1f),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier
                .size(40.dp),
            color = androidx.compose.ui.graphics.Color(color),
            shape = androidx.compose.foundation.shape.CircleShape
        ) {
            Text(
                count.toString(),
                modifier = Modifier.wrapContentSize(Alignment.Center),
                style = MaterialTheme.typography.labelMedium,
                color = androidx.compose.ui.graphics.Color.White
            )
        }
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
fun SlotResultCard(
    slot: TimeSlot,
    score: PollLogic.SlotScore,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isSelected) {
                    Modifier.border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = MaterialTheme.shapes.medium
                    )
                } else {
                    Modifier
                }
            ),
        onClick = onSelect
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        slot.start,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        "→",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        slot.end,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Text(
                    "Score: ${score.totalScore}",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (score.totalScore > 0) {
                        androidx.compose.ui.graphics.Color(0xFF4CAF50)
                    } else {
                        androidx.compose.ui.graphics.Color(0xFFF44336)
                    }
                )
            }

            // Vote breakdown
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                VoteChip("Yes", score.yesCount, 0xFF4CAF50.toInt())
                VoteChip("Maybe", score.maybeCount, 0xFFFFC107.toInt())
                VoteChip("No", score.noCount, 0xFFF44336.toInt())
            }
        }
    }
}

@Composable
fun VoteChip(label: String, count: Int, color: Int) {
    AssistChip(
        onClick = {},
        label = {
            Text("$label: $count", style = MaterialTheme.typography.labelSmall)
        },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = androidx.compose.ui.graphics.Color(color).copy(alpha = 0.2f)
        )
    )
}


