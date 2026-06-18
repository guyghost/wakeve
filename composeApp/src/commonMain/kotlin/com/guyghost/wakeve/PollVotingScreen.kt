package com.guyghost.wakeve

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.guyghost.wakeve.models.Event
import com.guyghost.wakeve.models.TimeSlot
import com.guyghost.wakeve.models.Vote
import com.guyghost.wakeve.ui.designsystem.WakeveButtonGroup
import com.guyghost.wakeve.ui.designsystem.WakeveCard
import com.guyghost.wakeve.ui.designsystem.WakeveProgressIndicator
import com.guyghost.wakeve.ui.designsystem.WakeveSize
import com.guyghost.wakeve.ui.designsystem.WakeveSpacing

data class PollVotingState(
    val eventId: String = "",
    val participantId: String = "",
    val votes: Map<String, Vote> = emptyMap(),
    val hasVoted: Boolean = false,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null
)

@Composable
fun PollVotingScreen(
    event: Event,
    state: PollVotingState,
    onVoteChange: (slotId: String, vote: Vote) -> Unit,
    onSubmitVotes: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.testTag("poll_voting_screen"),
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .fillMaxSize()
                .padding(paddingValues)
                .padding(WakeveSpacing.md)
        ) {
            Text(
                "Vote on Time Slots",
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.padding(bottom = WakeveSpacing.sm)
            )
            Text(
                "Event: ${event.title}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = WakeveSpacing.lg)
            )

            WakeveCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = WakeveSpacing.md)
            ) {
                Column {
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

            Text(
                "Proposed Time Slots",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = WakeveSpacing.sm)
            )

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(bottom = WakeveSpacing.md),
                verticalArrangement = Arrangement.spacedBy(WakeveSpacing.sm)
            ) {
                items(event.proposedSlots, key = { it.id }) { slot ->
                    TimeSlotVoteCard(
                        slot = slot,
                        currentVote = state.votes[slot.id],
                        onVoteChange = { vote -> onVoteChange(slot.id, vote) }
                    )
                }
            }

            if (state.errorMessage != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = WakeveSpacing.sm),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Text(
                        state.errorMessage,
                        modifier = Modifier.padding(WakeveSpacing.sm),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Button(
                onClick = onSubmitVotes,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = WakeveSize.minTouchTarget),
                enabled = state.votes.size == event.proposedSlots.size &&
                    !state.hasVoted &&
                    !state.isSubmitting
            ) {
                if (state.isSubmitting) {
                    Box(contentAlignment = Alignment.Center) {
                        WakeveProgressIndicator(modifier = Modifier.size(WakeveSize.progressIndicator))
                    }
                } else {
                    Text(
                        if (state.hasVoted) "Votes submitted" else "Submit Votes",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
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
        Column(modifier = Modifier.padding(WakeveSpacing.md)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = WakeveSpacing.sm),
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
            WakeveButtonGroup {
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
            .heightIn(min = WakeveSize.minTouchTarget)
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
