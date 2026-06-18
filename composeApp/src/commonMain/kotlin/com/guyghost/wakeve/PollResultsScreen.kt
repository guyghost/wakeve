package com.guyghost.wakeve

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.guyghost.wakeve.ui.designsystem.WakeveButtonGroup
import com.guyghost.wakeve.ui.designsystem.WakeveCard
import com.guyghost.wakeve.ui.designsystem.WakeveProgressIndicator
import com.guyghost.wakeve.ui.designsystem.WakeveScaffold
import com.guyghost.wakeve.ui.designsystem.WakeveSize
import com.guyghost.wakeve.ui.designsystem.WakeveSpacing
import com.guyghost.wakeve.ui.designsystem.WakeveStateMessage
import com.guyghost.wakeve.ui.event.PollResultsUiState
import com.guyghost.wakeve.ui.event.PollSlotResultUiState

@Composable
fun PollResultsScreen(
    state: PollResultsUiState,
    onSlotSelected: (String) -> Unit,
    onConfirmFinalDate: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    WakeveScaffold(
        title = "Résultats du sondage",
        onNavigateBack = onBack,
        modifier = modifier.testTag("poll_results_screen")
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(WakeveSpacing.md),
            verticalArrangement = Arrangement.spacedBy(WakeveSpacing.md)
        ) {
            Text(
                text = state.eventTitle,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = if (state.isOrganizer) {
                    "Choisissez le créneau final à partir des votes."
                } else {
                    "Consultez les résultats. Seul l'organisateur peut confirmer la date finale."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            state.recommendedSlot?.let { recommended ->
                RecommendedSlotCard(
                    slot = recommended,
                    onSelect = { onSlotSelected(recommended.slotId) }
                )
            }

            Text(
                text = "Tous les créneaux",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (state.slots.isEmpty()) {
                WakeveStateMessage(
                    title = "Aucun vote disponible",
                    body = "Les résultats apparaîtront quand les participants auront voté.",
                    icon = Icons.Default.CalendarMonth,
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(bottom = WakeveSpacing.sm),
                    verticalArrangement = Arrangement.spacedBy(WakeveSpacing.sm)
                ) {
                    items(state.slots, key = { it.slotId }) { slot ->
                        SlotResultCard(
                            slot = slot,
                            onSelect = { onSlotSelected(slot.slotId) }
                        )
                    }
                }
            }

            state.errorMessage?.let { message ->
                WakeveStateMessage(
                    title = "Impossible de confirmer",
                    body = message
                )
            }

            if (state.selectedSlotId != null && !state.hasConfirmed) {
                WakeveCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Une fois confirmée, cette date débloque les étapes d'organisation pour les participants retenus.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Button(
                onClick = onConfirmFinalDate,
                enabled = state.canConfirm,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = WakeveSize.minTouchTarget)
            ) {
                if (state.isConfirming) {
                    WakeveProgressIndicator(modifier = Modifier.size(WakeveSize.minTouchTarget))
                } else {
                    Text("Confirmer la date finale", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun RecommendedSlotCard(
    slot: PollSlotResultUiState,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    WakeveCard(
        selected = true,
        onClick = onSelect,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(WakeveSpacing.md)) {
            Text(
                text = "Créneau recommandé",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            SlotTimeRange(slot = slot)
            ScoreBreakdown(slot = slot)
            Text(
                text = "Score ${slot.totalScore}",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun SlotResultCard(
    slot: PollSlotResultUiState,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    WakeveCard(
        selected = slot.isSelected,
        onClick = onSelect,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(WakeveSpacing.md)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SlotTimeRange(slot = slot, modifier = Modifier.weight(1f))
                AssistChip(
                    onClick = onSelect,
                    label = { Text("Score ${slot.totalScore}") }
                )
            }
            ScoreBreakdown(slot = slot)
        }
    }
}

@Composable
private fun SlotTimeRange(
    slot: PollSlotResultUiState,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(WakeveSpacing.xs)) {
        Text(slot.startLabel, style = MaterialTheme.typography.bodyMedium)
        Text(
            text = "jusqu'à",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(slot.endLabel, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun ScoreBreakdown(slot: PollSlotResultUiState) {
    WakeveButtonGroup {
        ScoreIndicator(label = "Oui", count = slot.yesCount, type = ScoreType.Yes, modifier = Modifier.weight(1f))
        ScoreIndicator(label = "Peut-être", count = slot.maybeCount, type = ScoreType.Maybe, modifier = Modifier.weight(1f))
        ScoreIndicator(label = "Non", count = slot.noCount, type = ScoreType.No, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun ScoreIndicator(
    label: String,
    count: Int,
    type: ScoreType,
    modifier: Modifier = Modifier
) {
    val icon = when (type) {
        ScoreType.Yes -> Icons.Default.Check
        ScoreType.Maybe -> Icons.Default.QuestionMark
        ScoreType.No -> Icons.Default.Close
    }
    val containerColor = when (type) {
        ScoreType.Yes -> MaterialTheme.colorScheme.primaryContainer
        ScoreType.Maybe -> MaterialTheme.colorScheme.tertiaryContainer
        ScoreType.No -> MaterialTheme.colorScheme.errorContainer
    }
    val contentColor = when (type) {
        ScoreType.Yes -> MaterialTheme.colorScheme.onPrimaryContainer
        ScoreType.Maybe -> MaterialTheme.colorScheme.onTertiaryContainer
        ScoreType.No -> MaterialTheme.colorScheme.onErrorContainer
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(WakeveSpacing.xs)
    ) {
        Surface(
            modifier = Modifier.size(WakeveSize.minTouchTarget),
            color = containerColor,
            shape = MaterialTheme.shapes.large
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor
                )
                Text(
                    count.toString(),
                    modifier = Modifier.wrapContentSize(Alignment.Center),
                    style = MaterialTheme.typography.labelSmall,
                    color = contentColor
                )
            }
        }
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

private enum class ScoreType {
    Yes,
    Maybe,
    No
}
