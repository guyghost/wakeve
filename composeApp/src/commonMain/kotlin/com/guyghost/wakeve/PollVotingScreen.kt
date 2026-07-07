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
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

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
                "Choisir une date",
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.padding(bottom = WakeveSpacing.sm)
            )
            Text(
                event.title,
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
                        "Date limite du vote",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        formatInstantLabel(event.deadline),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Text(
                "Créneaux proposés",
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
                        if (state.hasVoted) "Votes envoyés" else "Envoyer mes votes",
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
    val slotLabel = formatTimeSlotLabel(slot)

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
                        slotLabel.date,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        slotLabel.time,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                AssistChip(
                    onClick = {},
                    label = { Text(slot.timezone.ifBlank { "UTC" }) }
                )
            }

            WakeveButtonGroup {
                VoteButton(
                    label = "Oui",
                    vote = Vote.YES,
                    isSelected = currentVote == Vote.YES,
                    onClick = { onVoteChange(Vote.YES) }
                )
                VoteButton(
                    label = "Peut-être",
                    vote = Vote.MAYBE,
                    isSelected = currentVote == Vote.MAYBE,
                    onClick = { onVoteChange(Vote.MAYBE) }
                )
                VoteButton(
                    label = "Non",
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

private data class TimeSlotLabel(
    val date: String,
    val time: String
)

private fun formatTimeSlotLabel(slot: TimeSlot): TimeSlotLabel {
    val timeZone = runCatching { TimeZone.of(slot.timezone.ifBlank { "UTC" }) }
        .getOrDefault(TimeZone.UTC)
    val start = slot.start?.toLocalDateTimeOrNull(timeZone)
    val end = slot.end?.toLocalDateTimeOrNull(timeZone)

    if (start == null && end == null) {
        return TimeSlotLabel(
            date = formatTimeOfDay(slot.timeOfDay),
            time = "Horaire flexible"
        )
    }

    val date = start ?: end
    val dateLabel = date?.let(::formatDate) ?: "Date à confirmer"
    val timeLabel = when {
        start != null && end != null && start.date == end.date -> {
            "${formatTime(start)} - ${formatTime(end)}"
        }
        start != null && end != null -> {
            "Du ${formatDateTime(start)} au ${formatDateTime(end)}"
        }
        start != null -> "À partir de ${formatTime(start)}"
        end != null -> "Jusqu'à ${formatTime(end)}"
        else -> "Horaire à confirmer"
    }

    return TimeSlotLabel(date = dateLabel, time = timeLabel)
}

private fun formatInstantLabel(value: String): String {
    return value.toLocalDateTimeOrNull(TimeZone.currentSystemDefault())
        ?.let(::formatDateTime)
        ?: value
}

private fun String.toLocalDateTimeOrNull(timeZone: TimeZone): LocalDateTime? {
    return runCatching { Instant.parse(this).toLocalDateTime(timeZone) }.getOrNull()
}

private fun formatDateTime(value: LocalDateTime): String = "${formatDate(value)} à ${formatTime(value)}"

private fun formatDate(value: LocalDateTime): String {
    val day = when (value.dayOfWeek) {
        DayOfWeek.MONDAY -> "lun."
        DayOfWeek.TUESDAY -> "mar."
        DayOfWeek.WEDNESDAY -> "mer."
        DayOfWeek.THURSDAY -> "jeu."
        DayOfWeek.FRIDAY -> "ven."
        DayOfWeek.SATURDAY -> "sam."
        DayOfWeek.SUNDAY -> "dim."
    }
    val month = when (value.monthNumber) {
        1 -> "janv."
        2 -> "févr."
        3 -> "mars"
        4 -> "avr."
        5 -> "mai"
        6 -> "juin"
        7 -> "juil."
        8 -> "août"
        9 -> "sept."
        10 -> "oct."
        11 -> "nov."
        12 -> "déc."
        else -> value.monthNumber.toString()
    }
    return "$day ${value.dayOfMonth} $month ${value.year}"
}

private fun formatTime(value: LocalDateTime): String {
    return "${value.hour.toString().padStart(2, '0')}:${value.minute.toString().padStart(2, '0')}"
}

private fun formatTimeOfDay(timeOfDay: com.guyghost.wakeve.models.TimeOfDay): String {
    return when (timeOfDay) {
        com.guyghost.wakeve.models.TimeOfDay.ALL_DAY -> "Toute la journée"
        com.guyghost.wakeve.models.TimeOfDay.MORNING -> "Matin"
        com.guyghost.wakeve.models.TimeOfDay.AFTERNOON -> "Après-midi"
        com.guyghost.wakeve.models.TimeOfDay.EVENING -> "Soirée"
        com.guyghost.wakeve.models.TimeOfDay.SPECIFIC -> "Horaire à préciser"
    }
}
