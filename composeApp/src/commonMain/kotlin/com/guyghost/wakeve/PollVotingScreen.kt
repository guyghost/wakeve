package com.guyghost.wakeve

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

data class PollVotingState(
    val eventId: String = "",
    val participantId: String = "participant-1", // TODO: Get from auth
    val votes: Map<String, Vote> = emptyMap(),
    val hasVoted: Boolean = false,
    val isError: Boolean = false,
    val errorMessage: String = ""
)

@Composable
fun PollVotingScreen(
    event: Event,
    repository: EventRepository,
    onVoteSubmitted: (String) -> Unit
) {
    var state by remember {
        mutableStateOf(PollVotingState(eventId = event.id))
    }

    Column(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.background)
            .safeContentPadding()
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            "Vote on Time Slots",
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            "Event: ${event.title}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Deadline Info
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    "Voting Deadline",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    event.deadline,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        // Time Slots Voting
        Text(
            "Proposed Time Slots",
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
            items(event.proposedSlots) { slot ->
                TimeSlotVoteCard(
                    slot = slot,
                    currentVote = state.votes[slot.id],
                    onVoteChange = { vote ->
                        state = state.copy(
                            votes = state.votes + (slot.id to vote),
                            isError = false
                        )
                    }
                )
            }
        }

        // Error Display
        if (state.isError) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Text(
                    state.errorMessage,
                    modifier = Modifier.padding(12.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        // Submit Button
        Button(
            onClick = {
                // Validate all slots have votes
                if (state.votes.size != event.proposedSlots.size) {
                    state = state.copy(
                        isError = true,
                        errorMessage = "Please vote on all time slots"
                    )
                } else {
                    // Submit votes
                    var allSuccess = true
                    state.votes.forEach { (slotId, vote) ->
                        val result = repository.addVote(
                            event.id,
                            state.participantId,
                            slotId,
                            vote
                        )
                        if (result.isFailure) {
                            allSuccess = false
                            state = state.copy(
                                isError = true,
                                errorMessage = result.exceptionOrNull()?.message ?: "Failed to submit vote"
                            )
                        }
                    }
                    if (allSuccess) {
                        state = state.copy(hasVoted = true)
                        onVoteSubmitted(event.id)
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            enabled = state.votes.size == event.proposedSlots.size && !state.hasVoted
        ) {
            Text("Submit Votes", style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
fun TimeSlotVoteCard(
    slot: TimeSlot,
    currentVote: Vote?,
    onVoteChange: (Vote) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        "From: ${slot.start}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        "To: ${slot.end}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                AssistChip(
                    onClick = {},
                    label = { Text("UTC") }
                )
            }

            // Vote Options
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                VoteButton(
                    label = "Yes",
                    vote = Vote.YES,
                    isSelected = currentVote == Vote.YES,
                    onClick = { onVoteChange(Vote.YES) }
                )
                VoteButton(
                    label = "Maybe",
                    vote = Vote.MAYBE,
                    isSelected = currentVote == Vote.MAYBE,
                    onClick = { onVoteChange(Vote.MAYBE) }
                )
                VoteButton(
                    label = "No",
                    vote = Vote.NO,
                    isSelected = currentVote == Vote.NO,
                    onClick = { onVoteChange(Vote.NO) }
                )
            }
        }
    }
}

@Composable
fun RowScope.VoteButton(
    label: String,
    vote: Vote,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .height(40.dp)
            .weight(1f),
        colors = if (isSelected) {
            ButtonDefaults.buttonColors()
        } else {
            ButtonDefaults.outlinedButtonColors()
        }
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}
