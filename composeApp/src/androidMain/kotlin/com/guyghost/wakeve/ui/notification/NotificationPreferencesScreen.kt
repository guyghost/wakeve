package com.guyghost.wakeve.ui.notification

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.HowToVote
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.guyghost.wakeve.R

// MARK: - Preference Item Data

data class PreferenceToggleItem(
    val id: String,
    val label: String,
    val description: String,
    val icon: ImageVector,
    val isEnabled: Boolean
)

// MARK: - Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationPreferencesScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val votesLabel = stringResource(R.string.votes)
    val votesDesc = stringResource(R.string.prefs_votes_desc)
    val commentsLabel = stringResource(R.string.comments)
    val commentsDesc = stringResource(R.string.prefs_comments_desc)
    val statusLabel = stringResource(R.string.prefs_status_changes)
    val statusDesc = stringResource(R.string.prefs_status_changes_desc)
    val remindersLabel = stringResource(R.string.prefs_reminders)
    val remindersDesc = stringResource(R.string.prefs_reminders_desc)
    val deadlinesLabel = stringResource(R.string.prefs_deadlines)
    val deadlinesDesc = stringResource(R.string.prefs_deadlines_desc)
    val weeklyLabel = stringResource(R.string.prefs_weekly_digest)
    val weeklyDesc = stringResource(R.string.prefs_weekly_digest_desc)

    var preferences by remember {
        mutableStateOf(
            listOf(
                PreferenceToggleItem(
                    id = "votes",
                    label = votesLabel,
                    description = votesDesc,
                    icon = Icons.Filled.HowToVote,
                    isEnabled = true
                ),
                PreferenceToggleItem(
                    id = "comments",
                    label = commentsLabel,
                    description = commentsDesc,
                    icon = Icons.Filled.ChatBubble,
                    isEnabled = true
                ),
                PreferenceToggleItem(
                    id = "status_changes",
                    label = statusLabel,
                    description = statusDesc,
                    icon = Icons.Filled.SwapHoriz,
                    isEnabled = true
                ),
                PreferenceToggleItem(
                    id = "reminders",
                    label = remindersLabel,
                    description = remindersDesc,
                    icon = Icons.Filled.Notifications,
                    isEnabled = true
                ),
                PreferenceToggleItem(
                    id = "deadlines",
                    label = deadlinesLabel,
                    description = deadlinesDesc,
                    icon = Icons.Filled.AccessTime,
                    isEnabled = true
                ),
                PreferenceToggleItem(
                    id = "weekly_digest",
                    label = weeklyLabel,
                    description = weeklyDesc,
                    icon = Icons.Filled.Newspaper,
                    isEnabled = false
                )
            )
        )
    }

    var soundEnabled by remember { mutableStateOf(true) }
    var quietHoursEnabled by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.prefs_title),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                actions = {
                    TextButton(onClick = {
                        // TODO: Appeler l'API PUT /api/notifications/preferences
                    }) {
                        Text(
                            text = stringResource(R.string.prefs_save),
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Types de notifications
            Text(
                text = stringResource(R.string.prefs_notification_types),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            ) {
                Column {
                    preferences.forEachIndexed { index, item ->
                        PreferenceRow(
                            item = item,
                            onToggle = { enabled ->
                                preferences = preferences.toMutableList().also {
                                    it[index] = item.copy(isEnabled = enabled)
                                }
                            }
                        )

                        if (index < preferences.size - 1) {
                            HorizontalDivider(
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant,
                                modifier = Modifier.padding(start = 56.dp)
                            )
                        }
                    }
                }
            }

            // General
            Text(
                text = stringResource(R.string.prefs_general),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.VolumeUp,
                        contentDescription = null,
                        tint = if (soundEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    Text(
                        text = stringResource(R.string.prefs_sound),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f)
                    )

                    Switch(
                        checked = soundEnabled,
                        onCheckedChange = { soundEnabled = it }
                    )
                }
            }

            // Ne pas deranger
            Text(
                text = stringResource(R.string.prefs_do_not_disturb),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.NightsStay,
                            contentDescription = null,
                            tint = if (quietHoursEnabled) Color(0xFF9333EA) else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.prefs_quiet_hours),
                                style = MaterialTheme.typography.bodyLarge
                            )
                            if (quietHoursEnabled) {
                                Text(
                                    text = "22:00 - 07:00",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Switch(
                            checked = quietHoursEnabled,
                            onCheckedChange = { quietHoursEnabled = it }
                        )
                    }

                    if (quietHoursEnabled) {
                        HorizontalDivider(
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant,
                            modifier = Modifier.padding(start = 56.dp)
                        )

                        Text(
                            text = stringResource(R.string.prefs_quiet_hours_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 56.dp, end = 16.dp, top = 8.dp, bottom = 12.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// MARK: - Preference Row

@Composable
private fun PreferenceRow(
    item: PreferenceToggleItem,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = null,
            tint = if (item.isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.label,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = item.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Switch(
            checked = item.isEnabled,
            onCheckedChange = onToggle
        )
    }
}
