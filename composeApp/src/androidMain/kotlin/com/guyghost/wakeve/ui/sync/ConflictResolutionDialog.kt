package com.guyghost.wakeve.ui.sync

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.guyghost.wakeve.sync.conflict.ConflictRecord
import com.guyghost.wakeve.sync.conflict.ConflictSummary
import com.guyghost.wakeve.sync.conflict.ResolutionDecision

/**
 * Material 3 bottom sheet for resolving offline sync conflicts.
 *
 * Presents each CRITICAL field conflict with:
 * - Field name label
 * - "Mine" (local) value
 * - "Theirs" (remote) value
 * - Per-field Keep Mine / Keep Theirs buttons
 *
 * Bulk "Keep All Mine" / "Keep All Theirs" shortcuts at the bottom.
 * Confirm is disabled until all critical conflicts have been resolved.
 *
 * ## Accessibility
 * - Each conflict row is semantically labelled for screen readers
 * - All interactive elements have ≥48dp touch target
 * - Color is never the sole differentiator between local and remote
 *
 * @param summary         The [ConflictSummary] to resolve (must have criticalConflicts)
 * @param onResolved      Called with a list of [ResolutionDecision]s when confirmed
 * @param onDismiss       Called if the user dismisses without resolving (sheet drag-down)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConflictResolutionDialog(
    summary: ConflictSummary,
    onResolved: (List<ResolutionDecision>) -> Unit,
    onDismiss: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    // Track the pending decision for each critical field (fieldName → decision)
    val pendingDecisions = remember {
        mutableStateMapOf<String, ResolutionDecision>()
    }

    val allResolved = summary.criticalConflicts.all { pendingDecisions.containsKey(it.fieldName) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            // ── Header ──────────────────────────────────────────────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "Sync conflict",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text(
                text = "${summary.criticalConflicts.size} field(s) were edited by multiple participants. Choose which version to keep.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp, bottom = 16.dp)
            )

            // ── Bulk actions ─────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        summary.criticalConflicts.forEach { conflict ->
                            pendingDecisions[conflict.fieldName] =
                                ResolutionDecision.KeepLocal(conflict.fieldName, conflict.localValue)
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Text("Keep all mine", maxLines = 1)
                }
                OutlinedButton(
                    onClick = {
                        summary.criticalConflicts.forEach { conflict ->
                            pendingDecisions[conflict.fieldName] =
                                ResolutionDecision.KeepRemote(conflict.fieldName, conflict.remoteValue)
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Text("Keep all theirs", maxLines = 1)
                }
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))

            // ── Conflict list ─────────────────────────────────────────────
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
            ) {
                items(summary.criticalConflicts) { conflict ->
                    val decision = pendingDecisions[conflict.fieldName]
                    ConflictRowItem(
                        conflict = conflict,
                        currentDecision = decision,
                        onKeepLocal = {
                            pendingDecisions[conflict.fieldName] =
                                ResolutionDecision.KeepLocal(conflict.fieldName, conflict.localValue)
                        },
                        onKeepRemote = {
                            pendingDecisions[conflict.fieldName] =
                                ResolutionDecision.KeepRemote(conflict.fieldName, conflict.remoteValue)
                        }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(12.dp))

            // ── Confirm button ────────────────────────────────────────────
            Button(
                onClick = { onResolved(pendingDecisions.values.toList()) },
                enabled = allResolved,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(26.dp)
            ) {
                Text(
                    text = if (allResolved) "Apply resolution" else "Resolve all conflicts to continue",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

/**
 * A single row in the conflict resolution list.
 *
 * Shows the field name, local value ("Mine"), remote value ("Theirs"),
 * and two action buttons. The selected option is visually highlighted.
 */
@Composable
private fun ConflictRowItem(
    conflict: ConflictRecord,
    currentDecision: ResolutionDecision?,
    onKeepLocal: () -> Unit,
    onKeepRemote: () -> Unit,
    modifier: Modifier = Modifier
) {
    val localSelected  = currentDecision is ResolutionDecision.KeepLocal
    val remoteSelected = currentDecision is ResolutionDecision.KeepRemote

    Card(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                contentDescription =
                    "Conflict in ${friendlyFieldName(conflict.fieldName)}. " +
                    "Mine: ${conflict.localValue}. Theirs: ${conflict.remoteValue}."
            },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Field label
            Text(
                text = friendlyFieldName(conflict.fieldName),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(10.dp))

            // Values row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Mine (local)
                ConflictValueChip(
                    label = "Mine",
                    value = conflict.localValue.truncated(),
                    selected = localSelected,
                    onClick = onKeepLocal,
                    modifier = Modifier.weight(1f)
                )
                // Theirs (remote)
                ConflictValueChip(
                    label = "Theirs",
                    value = conflict.remoteValue.truncated(),
                    selected = remoteSelected,
                    onClick = onKeepRemote,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ConflictValueChip(
    label: String,
    value: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor = if (selected)
        MaterialTheme.colorScheme.primaryContainer
    else
        MaterialTheme.colorScheme.surface

    val contentColor = if (selected)
        MaterialTheme.colorScheme.onPrimaryContainer
    else
        MaterialTheme.colorScheme.onSurface

    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .height(80.dp)
            .semantics { contentDescription = "Keep $label: $value" }
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = contentColor
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                color = contentColor,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Helpers

private fun friendlyFieldName(fieldName: String): String = when (fieldName) {
    "title"          -> "Event title"
    "description"    -> "Description"
    "status"         -> "Status"
    "finalDate"      -> "Confirmed date"
    "deadline"       -> "Voting deadline"
    "participants"   -> "Participants"
    "proposedSlots"  -> "Time slots"
    else             -> fieldName.replaceFirstChar { it.uppercase() }
}

private fun String.truncated(maxChars: Int = 60): String =
    if (length <= maxChars) this else take(maxChars - 1) + "…"
